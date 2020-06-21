package JavaExtractor.Common;

import java.util.ArrayList;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import JavaExtractor.FeaturesEntities.FakeNode;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.UserDataKey;

import JavaExtractor.FeaturesEntities.ProgramNode;
import JavaExtractor.FeaturesEntities.Property;
import com.github.javaparser.ast.expr.NullLiteralExpr;

public final class Common {
	public static final UserDataKey<Property> PropertyKey = new UserDataKey<Property>() {
	};
	public static final UserDataKey<ProgramNode> ProgramNodeKey = new UserDataKey<ProgramNode>() {
	};
	public static final UserDataKey<Integer> ChildId = new UserDataKey<Integer>() {
	};
	public static final String EmptyString = "";
	public static final String UTF8 = "UTF-8";
	public static final String EvaluateTempDir = "EvalTemp";

	public static final String FieldAccessExpr = "FieldAccessExpr";
	public static final String ClassOrInterfaceType = "ClassOrInterfaceType";
	public static final String MethodDeclaration = "MethodDeclaration";
	public static final String NameExpr = "NameExpr";
	public static final String MethodCallExpr = "MethodCallExpr";
	public static final String GenericClass = "GenericClass";
	public static final String DummyNode = "DummyNode";
	public static final String BlankWord = "BLANK";
	public static final String EOS = "EOS";

	public static final int c_MaxLabelLength = 50;
	public static final String methodName = "METHOD_NAME";
	public static final String internalSeparator = "|";

	public static final ArrayList<String> emptyArrayList = new ArrayList<>();

	public static String normalizeName(String original, String defaultString) {
		original = original.toLowerCase().replaceAll("\\\\n", "") // escaped new
																	// lines
				.replaceAll("//s+", "") // whitespaces
				.replaceAll("[\"',]", "") // quotes, apostrophies, commas
				.replaceAll("\\P{Print}", ""); // unicode weird characters
		String stripped = original.replaceAll("[^A-Za-z]", "");
		if (stripped.length() == 0) {
			String carefulStripped = original.replaceAll(" ", "_");
			if (carefulStripped.length() == 0) {
				return defaultString;
			} else {
				return carefulStripped;
			}
		} else {
			return stripped;
		}
	}

	public static boolean isMethod(Node node) {
		String type = node.getUserData(Common.PropertyKey).getType();

		return isMethod(node, type);
	}

	public static String escapeStars(String name) {
		return name.replace("*", "STAR");
	}

	public static String maybeFlipNode(String nodeType, boolean isRightToLeft, boolean insideSite) {
		if (isRightToLeft && !insideSite) {
			return nodeType + "_INV";
		} else {
			return nodeType;
		}
	}

	public static boolean isMethod(Node node, String type) {
		Property parentProperty = node.getParentNode().getUserData(Common.PropertyKey);
		if (parentProperty == null) {
			return false;
		}

		String parentType = parentProperty.getType();
		return Common.NameExpr.equals(type) && Common.MethodDeclaration.equals(parentType);
	}

	public static ArrayList<String> splitToSubtokens(String str1) {
		String str2 = str1.trim();
		// [^a-zA-Z]
		return Stream.of(str2.split("(?<=[a-z])(?=[A-Z])|_|[0-9]|(?<=[A-Z])(?=[A-Z][a-z])|\\s+"))
				.filter(s -> s.length() > 0).map(s -> Common.normalizeName(s, Common.EmptyString))
				.filter(s -> s.length() > 0).collect(Collectors.toCollection(ArrayList::new));
	}

	public static boolean isInsideSite(Node n, Set<Node> nodesInSite) {
		return nodesInSite.contains(n);
	}

	public static boolean isInsideSite(Property p, Set<Node> nodesInSite) {
		return nodesInSite.contains(p.getNode());
	}

	public static boolean isValidNode(Node node) {
		Property property = node.getUserData(Common.PropertyKey);
		String nodeString = null;
		if (property == null) {
			nodeString = node.toString();
		} else {
			nodeString = property.toString();
		}
		return isValidNode(node, nodeString);
	}

	public static boolean isValidNode(Node node, String nodeToString) {
		if (node instanceof FakeNode) {
			return true;
		}
		return !nodeToString.isEmpty() && (!"null".equals(nodeToString) || (node instanceof NullLiteralExpr));
	}
}
