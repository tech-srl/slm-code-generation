package JavaExtractor.Common;

import java.util.ArrayList;
import com.github.javaparser.ast.Node;

public class MethodContent {
	private Node root;
	private ArrayList<Node> leaves;
	private String name;
	private long length;

	public MethodContent(Node root, ArrayList<Node> leaves, String name, long length) {
		this.root = root;
		this.leaves = leaves;
		this.name = name;
		this.length = length;
	}

	public ArrayList<Node> getLeaves() {
		return leaves;
	}

	public String getName() {
		return name;
	}

	public long getLength() {
		return length;
	}

	public Node getRoot() {
		return root;
	}
}
