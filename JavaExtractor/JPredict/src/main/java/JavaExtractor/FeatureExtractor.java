package JavaExtractor;

import JavaExtractor.Common.CommandLineValues;
import JavaExtractor.Common.Common;
import JavaExtractor.Common.MethodContent;
import JavaExtractor.FeaturesEntities.CompletionSite;
import JavaExtractor.FeaturesEntities.ProgramFeatures;
import JavaExtractor.FeaturesEntities.Property;
import JavaExtractor.Visitors.CompletionSiteVisitor;
import JavaExtractor.Visitors.FunctionVisitor;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseProblemException;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings("StringEquality")
public class FeatureExtractor {
    private CommandLineValues commandLineValues;
    private static Set<String> s_ParentTypeToAddChildId = Stream
            .of("AssignExpr", "ArrayAccessExpr", "FieldAccessExpr", "MethodCallExpr")
            .collect(Collectors.toCollection(HashSet::new));

    final static String upSymbol = "|";
    final static String downSymbol = "|";
    private HashMap<Node, ArrayList<Node>> stackCache = new HashMap<>();
    Path filePath;

    public FeatureExtractor(CommandLineValues commandLineValues, Path filePath) {
        this.commandLineValues = commandLineValues;
        this.filePath = filePath;
    }

    public ArrayList<CompletionSite> extractFeatures(String code) throws ParseProblemException {
        CompilationUnit m_CompilationUnit = parseFileWithRetries(code);
        FunctionVisitor functionVisitor = new FunctionVisitor(this.commandLineValues);

        functionVisitor.visitDepthFirst(m_CompilationUnit);

        ArrayList<MethodContent> methods = functionVisitor.getMethodContents();

        ArrayList<CompletionSite> completionSites = generatePathFeatures(methods);

        return completionSites;
    }

    private CompilationUnit parseFileWithRetries(String code) throws ParseProblemException {
        final String classPrefix = "public class Test {";
        final String classSuffix = "}";
        final String methodPrefix = "SomeUnknownReturnType f() {";
        final String methodSuffix = "return noSuchReturnValue; }";

        String originalContent = code;
        String content = originalContent;
        CompilationUnit parsed = null;
        try {
            parsed = JavaParser.parse(content);
        } catch (ParseProblemException e1) {
            // Wrap with a class and method
            try {
                content = classPrefix + methodPrefix + originalContent + methodSuffix + classSuffix;
                parsed = JavaParser.parse(content);
            } catch (ParseProblemException e2) {
                // Wrap with a class only
                content = classPrefix + originalContent + classSuffix;
                parsed = JavaParser.parse(content);
            }
        }

        return parsed;
    }

    public ArrayList<CompletionSite> generatePathFeatures(ArrayList<MethodContent> methods) {
        ArrayList<CompletionSite> completionSitesForFile = new ArrayList<>();
        for (MethodContent content : methods) {
            if (content.getLength() < commandLineValues.MinCodeLength
                    || content.getLength() > commandLineValues.MaxCodeLength)
                continue;
            CompletionSiteVisitor completionSiteVisitor = new CompletionSiteVisitor(commandLineValues, this.filePath, content.getRoot());
            completionSiteVisitor.visitDepthFirst(content.getRoot());

            ArrayList<CompletionSite> completionSites = completionSiteVisitor.getCompletionSites();
            if (completionSites.isEmpty()) {
                continue;
            }

            for (CompletionSite site: completionSites) {
                ProgramFeatures singleSiteFeatures = generatePathFeaturesForFunction(content, site);
                if (!singleSiteFeatures.isEmpty()) {
                    site.setPathsForNode(singleSiteFeatures, this);
                    if (site.getPathsForHeadNode().size() > 0 && site.getNodes().size() > 0) {
                        completionSitesForFile.add(site);
                    }
                }
            }
        }
        return completionSitesForFile;
    }

    private ProgramFeatures generatePathFeaturesForFunction(MethodContent methodContent, CompletionSite site) {
        ArrayList<Node> functionLeaves = methodContent.getLeaves();
        ProgramFeatures programFeatures = new ProgramFeatures(methodContent.getName());

        for (Node contextNode: functionLeaves) {
            Node headNode = site.getHeadNode();
            if (site.getNodes().contains(contextNode)) {
                continue;
            }

            NodesPath path = generatePath(contextNode, headNode);
            if (path == null || path.isBottomUp()) {
                continue;
            }

            Property source = contextNode.getUserData(Common.PropertyKey);
            Property target = headNode.getUserData(Common.PropertyKey);
            programFeatures.addFeature(source, path, target);

        }
        Node headNode = site.getHeadNode();
        Property headNodeProperty = headNode.getUserData(Common.PropertyKey);
        for (Node siteNode: site.getNodes()) {
            if (headNode.getUserData(Common.PropertyKey) == siteNode.getUserData(Common.PropertyKey)) {
                continue;
            }

            NodesPath path = generatePath(headNode, siteNode);
            if (path != null) {
                path.setOtherEndNodeProperty(headNodeProperty);
                Property source = headNode.getUserData(Common.PropertyKey);
                Property target = siteNode.getUserData(Common.PropertyKey);
                programFeatures.addFeature(source, path, target);
            }
        }
        for (Node internalSourceNode: site.getNodes()) {
            if (!internalSourceNode.getUserData(Common.PropertyKey).isLeaf()) {
                continue;
            }
            if (internalSourceNode.getUserData(Common.PropertyKey).getRawType().equals(Common.EOS)) {
                continue;
            }
            for (Node internalTargetNode : site.getNodes()) {
                if (internalSourceNode == internalTargetNode) {
                    continue;
                }

                NodesPath path = generatePath(internalSourceNode, internalTargetNode);
                if (path == null || path.isBottomUp()) {
                    continue;
                }
                if (site.inSite(path.first()) && site.inSite(path.last()) && path.isRightToLeft()) {
                    // If this path is internal to the completion site, we take only paths from nodes to the left
                    continue;
                }

                Property source = internalSourceNode.getUserData(Common.PropertyKey);
                Property target = internalTargetNode.getUserData(Common.PropertyKey);
                programFeatures.addFeature(source, path, target);
            }
        }

        return programFeatures;
    }

    private ArrayList<Node> getTreeStack(Node node) {
        ArrayList<Node> upStack = new ArrayList<>();
        Node current = node;
        while (current != null) {
            upStack.add(current);
            current = current.getParentNode();
        }
        return upStack;
    }

    public NodesPath getRootToNodePath(Node root, Node target) {

        return generatePath(root, target);
    }

    private NodesPath generatePath(Node source, Node target) {
        String down = downSymbol;
        String up = upSymbol;

        //StringJoiner stringBuilder = new StringJoiner(separator);
        ArrayList<Node> sourceStack = getTreeStack(source);
        ArrayList<Node> targetStack = getTreeStack(target);

        int commonPrefix = 0;
        int currentSourceAncestorIndex = sourceStack.size() - 1;
        int currentTargetAncestorIndex = targetStack.size() - 1;
        while (currentSourceAncestorIndex >= 0 && currentTargetAncestorIndex >= 0
                && sourceStack.get(currentSourceAncestorIndex) == targetStack.get(currentTargetAncestorIndex)) {
            commonPrefix++;
            currentSourceAncestorIndex--;
            currentTargetAncestorIndex--;
        }

        Node commonNode = sourceStack.get(sourceStack.size() - commonPrefix);

        if (currentSourceAncestorIndex >= 0 && currentTargetAncestorIndex >= 0) {
            int pathWidth = targetStack.get(currentTargetAncestorIndex).getUserData(Common.PropertyKey).getChildId()
                    - sourceStack.get(currentSourceAncestorIndex).getUserData(Common.PropertyKey).getChildId();
            if (pathWidth > commandLineValues.MaxPathWidth &&
                    !commonNode.getUserData(Common.PropertyKey).getRawType().equals(Common.MethodDeclaration)) {
                return null;
            }
        }

        NodesPath path = new NodesPath(commandLineValues);

        for (int i = 0; i < sourceStack.size() - commonPrefix; i++) {
            Node currentNode = sourceStack.get(i);
            path.appendUpNode(currentNode);
            /*String childId = Common.EmptyString;
            String parentRawType = currentNode.getParentNode().getUserData(Common.PropertyKey).getRawType();
            if (i == 0 || s_ParentTypeToAddChildId.contains(parentRawType)) {
                childId = saturateChildId(currentNode.getUserData(Common.ChildId))
                        .toString();
            }
            //stringBuilder.add(String.format("%s%s%s",
            //        currentNode.getUserData(Common.PropertyKey).getType(true), childId, up));
            */
        }

        path.setMidNode(commonNode);
        /*String commonNodeChildId = Common.EmptyString;
        Property parentNodeProperty = commonNode.getParentNode().getUserData(Common.PropertyKey);
        String commonNodeParentRawType = Common.EmptyString;
        if (parentNodeProperty != null) {
            commonNodeParentRawType = parentNodeProperty.getRawType();
        }
        if (s_ParentTypeToAddChildId.contains(commonNodeParentRawType)) {
            commonNodeChildId = saturateChildId(commonNode.getUserData(Common.ChildId))
                    .toString();
        }
        stringBuilder.add(String.format("%s%s",
                commonNode.getUserData(Common.PropertyKey).getType(true), commonNodeChildId));
		*/
        for (int i = targetStack.size() - commonPrefix - 1; i >= 0; i--) {
            Node currentNode = targetStack.get(i);
            path.appendDownNode(currentNode);
            /*String childId = Common.EmptyString;
            if (i == 0 || s_ParentTypeToAddChildId.contains(currentNode.getUserData(Common.PropertyKey).getRawType())) {
                childId = saturateChildId(currentNode.getUserData(Common.ChildId))
                        .toString();
            }
            stringBuilder.add(String.format("%s%s%s", down,
                    currentNode.getUserData(Common.PropertyKey).getType(true), childId));
			*/
        }

        //return stringBuilder.toString();
		return path;
    }
}
