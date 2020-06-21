package JavaExtractor.Visitors;

import JavaExtractor.Common.CommandLineValues;
import JavaExtractor.Common.Common;
import JavaExtractor.FeaturesEntities.CompletionSite;
import JavaExtractor.FeaturesEntities.Property;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.visitor.TreeVisitor;

import java.nio.file.Path;
import java.util.ArrayList;

public class CompletionSiteVisitor extends TreeVisitor {
    private ArrayList<CompletionSite> completionSites = new ArrayList<>();
    private CommandLineValues commandLineValues;
    private Path filePath;
    private Node root;

    public CompletionSiteVisitor(CommandLineValues commandLineValues, Path filePath, Node root) {
        this.commandLineValues = commandLineValues;
        this.filePath = filePath;
        this.root = root;
    }

    @Override
    public void process(Node node) {
        Property property = node.getUserData(Common.PropertyKey);
        if (Common.MethodDeclaration.equals(property.getRawType())) {
            return;
        }
        Property parentProperty = node.getParentNode().getUserData(Common.PropertyKey);
        String parentRawType = parentProperty.getRawType();
        int childId = property.getChildId();

        if (commandLineValues.ExtractAllExpressions) {
            // Check if the node matches the criteria for extraction expressions (ultimate goal)
            boolean isExpression = node instanceof Expression               // node is an expression
                    && !(node instanceof AnnotationExpr)                    // but not like "Override"
                    && (!property.toString().concat(";")
                        .equals(parentProperty.toString())) // which is smaller than a full statement
                    && node.getChildrenNodes().size() > 0;                     // but larger than a single node
            if (!isExpression) {
                return;
            }
        } else {
            if (!commandLineValues.typesToGen.containsKey(parentRawType) || commandLineValues.typesToGen.get(parentRawType) != childId) {
                return;
            }
        }
        CompletionSite site = new CompletionSite(commandLineValues, node, filePath, root);
        AllNodesCollectorVisitor collectorVisitor = new AllNodesCollectorVisitor(commandLineValues);
        collectorVisitor.visitBreadthFirst(node);
        ArrayList<Node> siteNodes = collectorVisitor.getNodes();

        // for simplicity, filter out generics and WildCard expressions, they parse weirdly
        if (siteNodes.stream().map(n -> n.getUserData(Common.PropertyKey))
                .anyMatch(p -> p.getType(false).startsWith(Common.GenericClass) || p.getType(false).startsWith("Wild"))) {
            return;
        }

        if (siteNodes.size() <= commandLineValues.MaxNodesInSite) {
            site.setNodes(siteNodes);
            this.completionSites.add(site);
        }
    }

    public ArrayList<CompletionSite> getCompletionSites() {
        return completionSites;
    }
}
