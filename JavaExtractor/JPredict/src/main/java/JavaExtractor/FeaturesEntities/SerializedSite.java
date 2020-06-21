package JavaExtractor.FeaturesEntities;

import JavaExtractor.Common.CommandLineValues;
import JavaExtractor.Common.Common;
import JavaExtractor.NodesPath;
import JavaExtractor.Visitors.FinderVisitor;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.comments.Comment;

import java.util.*;
import java.util.stream.Collectors;

public class SerializedSite {
    int num_targets;
    int num_nodes;
    ArrayList<String> targets = new ArrayList<>();
    ArrayList<Integer> is_token = new ArrayList<>();
    ArrayList<Integer> target_child_id = new ArrayList<>();
    List<List<SerializedPath>> internal_paths = new ArrayList<>();
    ArrayList<SerializedPath> relative_paths = new ArrayList<>();
    List<SerializedPath> head_paths; // = new ArrayList<>();
    SerializedPath head_root_path;
    int head_child_id;
    String linearized_tree;
    String filepath;
    String left_context;
    String right_context;
    String target_seq;
    int line;

    private transient CommandLineValues commandLineValues;

    public SerializedSite(CommandLineValues commandLineValues, CompletionSite completionSite) {
        this.commandLineValues = commandLineValues;
        filepath = completionSite.getFilePath().toUri().toString();
        line = completionSite.getHeadNode().getBegin().line;
        HashMap<Property, ArrayList<NodesPath>> pathsForNode = completionSite.getInternalPathsForNode();
        initializeHeadPaths(completionSite);
        initializeSequentialContext(completionSite);

        List<String> head_compound_tokens = head_paths.stream().filter(p -> p.getSources().size() > 0)
                .map(p -> p.token).collect(Collectors.toList());

        for (Node node : completionSite.getNodes()) {
            /*if (!Common.isValidNode(node)) {
                continue;
            }*/
            Property property = node.getUserData(Common.PropertyKey);
            targets.add(property.getType(true));
            is_token.add(0); // not token
            target_child_id.add(property.getChildId());
            //NodesPath rootPath = completionSite.getRootToHeadPath(property);
            //root_paths.add(new SerializedPath(rootPath, completionSite.getNodes(), false, Common.emptyArrayList, false));

            relative_paths.add(new SerializedPath(completionSite.getRelativePathForNode(property),
                    completionSite.getNodes(), false, Common.emptyArrayList, false));
            ArrayList<NodesPath> pathsForThisNode;
            if (pathsForNode.containsKey(property)) {
                pathsForThisNode = pathsForNode.get(property);
            } else {
                pathsForThisNode = new ArrayList<>();
            }
            List<SerializedPath> currentNodeSerializedPaths = getSerializedPathsForNodes(pathsForThisNode, completionSite.getNodes(), false, Common.emptyArrayList);
            internal_paths.add(currentNodeSerializedPaths);


            if (property.isLeaf() && !property.getRawType().equals(Common.EOS)) {
                ArrayList<String> nameParts = property.getName();
                String joinedNameParts = String.join(",", nameParts);
                if (nameParts.size() > 1 && head_compound_tokens.contains(joinedNameParts)) {
                    targets.add(joinedNameParts);
                    is_token.add(1);
                    target_child_id.add(0);

                    relative_paths.add(new SerializedPath(completionSite.getRelativePathForNode(property),
                            completionSite.getNodes(), true, new ArrayList<>(), false));
                    currentNodeSerializedPaths = getSerializedPathsForNodes(pathsForThisNode, completionSite.getNodes(), true, new ArrayList<>());
                    internal_paths.add(currentNodeSerializedPaths);
                }

                else {
                    for (int i = 0; i <= nameParts.size(); i++) {
                        String subtok = i < property.getName().size() ? property.getName().get(i) : Common.EOS;
                        targets.add(subtok);
                        is_token.add(1); // token
                        target_child_id.add(i);

                        List<String> prevSubtokens = property.getName().subList(0, i);
                        //root_paths.add(new SerializedPath(rootPath, completionSite.getNodes(), true, prevSubtokens, false));
                        relative_paths.add(new SerializedPath(completionSite.getRelativePathForNode(property),
                                completionSite.getNodes(), true, prevSubtokens, false));

                        currentNodeSerializedPaths = getSerializedPathsForNodes(pathsForThisNode, completionSite.getNodes(), true, prevSubtokens);
                        internal_paths.add(currentNodeSerializedPaths);
                    }
                }
            }
        }
        num_targets = targets.size();
        num_nodes = completionSite.getNodes().stream().filter(node -> !(node instanceof FakeNode)).collect(Collectors.reducing(0, e -> 1, Integer::sum));
        linearized_tree = linearizeCompletionSite(completionSite);
    }

    private void initializeSequentialContext(CompletionSite completionSite) {
        target_seq = completionSite.getHeadNode().toStringWithoutComments();
        Node root = completionSite.getRootToHeadPath().first();
        Comment methodComment = root.getComment();
        boolean hasMethodComment = false;
        if (methodComment != null) {
            methodComment.setContent(Common.EmptyString);
            hasMethodComment = true;
        }
        List<Comment> inlineComments = root.getAllContainedComments();
        for (Comment c : inlineComments) {
            c.setContent(Common.EmptyString);
        }
        String context = root.toString();
        if (hasMethodComment) {
            context = context.substring(6);
        }

        int indexOfTarget = getIndexOfTargetSeq(target_seq, completionSite, root, context);

        left_context = context.substring(0, indexOfTarget);
        right_context = context.substring(indexOfTarget + target_seq.length());

        //FinderVisitor visitor = new FinderVisitor(completionSite.getHeadNode());
        //visitor.visitDepthFirst(rootClone);
        //Node headClone = visitor.getFoundNode();
    }

    private int getIndexOfTargetSeq(String target_seq, CompletionSite completionSite, Node root, String context) {
        // If the same completion site appears several times in the same method, find the index of ours
        int indexOfTarget = context.indexOf(target_seq);
        String remainingContext = context.substring(indexOfTarget + target_seq.length());
        if (remainingContext.indexOf(target_seq) > -1) {
            // target_seq appears more than once
            int occurrenceIndex = getOccurrenceIndex(root, completionSite);
            for (int i = 0; i < occurrenceIndex ; i++) {
                int indexOfTargetInRemaining = remainingContext.indexOf(target_seq);
                indexOfTarget += target_seq.length() + indexOfTargetInRemaining;
                remainingContext = remainingContext.substring(indexOfTargetInRemaining + target_seq.length());
            }
            return indexOfTarget;
        } else {
            return indexOfTarget;
        }
    }

    private int getOccurrenceIndex(Node root, CompletionSite completionSite) {
        FinderVisitor finderVisitor = new FinderVisitor(completionSite);
        finderVisitor.visitDepthFirst(root);
        return finderVisitor.getFoundIndex();
    }

    private void linearizeRec(Node node, StringBuilder builder) {
        //if (!Common.isValidNode(node)) {
        //    return false;
        //}
        Property property = node.getUserData(Common.PropertyKey);

        builder.append(property.getType(true));
        builder.append("(");
        List<Node> children = Property.reorderedChildren(node).stream()
                .filter(n -> !n.getUserData(Common.PropertyKey).getRawType().equals(Common.EOS))
                .filter(n -> !(n instanceof Comment))
                .collect(Collectors.toList());
        if (children.size() > 0) {
            for (int i = 0; i < children.size(); i++) {
                linearizeRec(children.get(i), builder);
                if (i < children.size() - 1) {
                    builder.append(",");
                }
            }
        } else {
            ArrayList<String> nameParts = property.getName();
            //builder.append("(");
            builder.append(String.join(",", nameParts));
            //builder.append(")");
        }
        builder.append(")");
    }

    private String linearizeCompletionSite(CompletionSite completionSite) {
        Node headNode = completionSite.getHeadNode();
        StringBuilder builder = new StringBuilder();
        linearizeRec(headNode, builder);
        return builder.toString();
    }

    private void initializeHeadPaths(CompletionSite completionSite) {
        Node headNode = completionSite.getHeadNode();
        Property headNodeProperty = headNode.getUserData(Common.PropertyKey);
        ArrayList<NodesPath> pathsForHeadNode = new ArrayList<>(completionSite.getPathsForHeadNode());
        Collections.shuffle(pathsForHeadNode);
        if (pathsForHeadNode.size() > this.commandLineValues.MaxContexts){
            pathsForHeadNode = new ArrayList<>(pathsForHeadNode.subList(0, this.commandLineValues.MaxContexts));
        }

        head_paths = pathsForHeadNode.stream().map(p -> new SerializedPath(p, completionSite.getNodes(), false, Common.emptyArrayList, true)).collect(Collectors.toList());
        head_child_id = headNodeProperty.getChildId();
        NodesPath headRootPath = completionSite.getRootToHeadPath();
        head_root_path = new SerializedPath(headRootPath, completionSite.getNodes(), false, Common.emptyArrayList, false);
    }

    private List<SerializedPath> getSerializedPathsForNodes(ArrayList<NodesPath> pathsForCurrentNode, Set<Node> nodesInPath, boolean includeLastNode, List<String> prevSubtokens) {
        ArrayList<NodesPath> currentNodePaths = pathsForCurrentNode;
        Collections.shuffle(currentNodePaths);

        if (currentNodePaths.size() > commandLineValues.MaxInternalPaths){
            currentNodePaths = new ArrayList<>(currentNodePaths.subList(0, commandLineValues.MaxInternalPaths));
        }
        return currentNodePaths
                .stream().map(p -> new SerializedPath(p, nodesInPath, includeLastNode, prevSubtokens, true)).collect(Collectors.toList());
    }


    public int getNum_targets() {
        return num_targets;
    }

    public ArrayList<String> getTargets() {
        return targets;
    }

    public ArrayList<Integer> getIs_token() {
        return is_token;
    }

    public ArrayList<Integer> getTarget_child_id() {
        return target_child_id;
    }

    public List<List<SerializedPath>> getInternal_paths() {
        return internal_paths;
    }
}
