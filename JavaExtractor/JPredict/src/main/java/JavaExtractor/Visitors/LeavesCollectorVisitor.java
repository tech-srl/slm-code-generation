package JavaExtractor.Visitors;

import java.util.ArrayList;
import java.util.List;

import JavaExtractor.Common.CommandLineValues;
import JavaExtractor.FeaturesEntities.FakeNode;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.visitor.TreeVisitor;

import JavaExtractor.Common.Common;
import JavaExtractor.FeaturesEntities.Property;

public class LeavesCollectorVisitor extends TreeVisitor {
	ArrayList<Node> m_Leaves = new ArrayList<>();
	ArrayList<Node> genericParents = new ArrayList<>();
	CommandLineValues commandLineValues;
	private int currentId = 1;

	public LeavesCollectorVisitor(CommandLineValues commandLineValues) {
		this.commandLineValues = commandLineValues;
	}

	@Override
	public void process(Node node) {
		if (node instanceof Comment) {
			Property property = new Property(commandLineValues, node, true, false, currentId++);
			node.setUserData(Common.PropertyKey, property);
			return;
		}
		boolean isLeaf = false;
		boolean isGenericParent = isGenericParent(node);
		if (isGenericParent) {
			genericParents.add(node);
		}
		boolean isValidNode = Property.reorderedChildren(node.getParentNode()).contains(node);
		if (hasNoChildren(node)) {// && isNotComment(node)) {
			if (isValidNode) {
				isLeaf = true;
				m_Leaves.add(node);
			}
		}
		
		Property property = new Property(commandLineValues, node, isLeaf, isGenericParent, currentId++);
		node.setUserData(Common.PropertyKey, property);
	}

	private boolean isGenericParent(Node node) {
		return (node instanceof ClassOrInterfaceType) 
				&& ((ClassOrInterfaceType)node).getTypeArguments() != null 
				&& ((ClassOrInterfaceType)node).getTypeArguments().size() > 0;
	}

	private boolean hasNoChildren(Node node) {
		return node.getChildrenNodes().size() == 0;
	}

	
	public ArrayList<Node> getLeaves() {
		ArrayList<Node> nodesWithFakes = new ArrayList<Node>(m_Leaves);

		for (Node genericParent : genericParents) {
			Node fakeNode = new FakeNode(genericParent);
			Property fakeNodeProperty = new Property(commandLineValues, fakeNode, ((ClassOrInterfaceType)genericParent).getName());
			fakeNode.setUserData(Common.PropertyKey, fakeNodeProperty);

			genericParent.getChildrenNodes().add(fakeNode);
			nodesWithFakes.add(fakeNode);
		}

		return nodesWithFakes;
	}

}
