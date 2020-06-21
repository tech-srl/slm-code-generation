package JavaExtractor.Visitors;

import JavaExtractor.Common.CommandLineValues;
import JavaExtractor.Common.Common;
import JavaExtractor.FeaturesEntities.FakeNode;
import JavaExtractor.FeaturesEntities.Property;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.visitor.TreeVisitor;

import java.util.ArrayList;

public class AllNodesCollectorVisitor extends TreeVisitor {
    ArrayList<Node> nodes = new ArrayList<>();
    ArrayList<Node> nonterminals = new ArrayList<>();
    private int currentId = 1;
    CommandLineValues commandLineValues = null;

    public AllNodesCollectorVisitor(CommandLineValues commandLineValues) {
        this.commandLineValues = commandLineValues;
    }

    @Override
    public void process(Node node) {
        boolean isValidNode = Property.reorderedChildren(node.getParentNode()).contains(node);
        if (!isValidNode) {
            return;
        }
        if (node instanceof Comment) {
            return;
        }
        nodes.add(node);
        if (!node.getUserData(Common.PropertyKey).isLeaf()) {
            nonterminals.add(node);
        }
    }

    public ArrayList<Node> getNodes() {
        ArrayList<Node> nodesWithFakes = new ArrayList<Node>(nodes);
        for (Node nonterminal : nonterminals) {
            Node fakeNode = new FakeNode(nonterminal);
            Property fakeNodeProperty = new Property(commandLineValues, fakeNode);
            fakeNode.setUserData(Common.PropertyKey, fakeNodeProperty);

            nonterminal.getChildrenNodes().add(fakeNode);
            nodesWithFakes.add(fakeNode);
        }
        return nodesWithFakes;
    }

}
