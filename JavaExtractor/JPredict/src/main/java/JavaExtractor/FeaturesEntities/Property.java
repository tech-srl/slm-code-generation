package JavaExtractor.FeaturesEntities;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import JavaExtractor.Common.CommandLineValues;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.type.ClassOrInterfaceType;

import JavaExtractor.Common.Common;

public class Property {
	private String RawType;
	private String Type;
	private String Name;
	private ArrayList<String> SplitName;
	private String Operator;
	private Boolean isLeaf;
	private Integer childId;
	private Node node;
	private List<Node> ReorderedChildren = null;
	private String nodeString;

	CommandLineValues commandLineValues;
	//public static final HashSet<String> NumericalKeepValues = Stream.of("0", "1", "32", "64")
	//		.collect(Collectors.toCollection(HashSet::new));

	public Property(CommandLineValues commandLineValues, Node node, boolean isLeaf, boolean isGenericParent, int id) {
		this.isLeaf = isLeaf;
		this.node = node;
		this.commandLineValues = commandLineValues;
		Class<?> nodeClass = node.getClass();
		RawType = Type = nodeClass.getSimpleName();
		//if (node instanceof ClassOrInterfaceType && ((ClassOrInterfaceType) node).isBoxedType()) {
	    //		Type = "PrimitiveType";
		//}
        /*if (node instanceof VariableDeclarationExpr) {
            Type = Type + ":" + ((VariableDeclarationExpr)node).getElementType();
        }
        if (node instanceof Parameter) {
            Type = Type + ":" + ((Parameter)node).getElementType();
        }*/
		Operator = "";
		if (node instanceof BinaryExpr) {
			Operator = ((BinaryExpr) node).getOperator().toString();
		} else if (node instanceof UnaryExpr) {
			Operator = ((UnaryExpr) node).getOperator().toString();
		} else if (node instanceof AssignExpr) {
			Operator = ((AssignExpr) node).getOperator().toString();
		}
		if (Operator.length() > 0) {
			Type += ":" + Operator;
		}
		if (node instanceof MethodCallExpr && !Common.isValidNode(node.getChildrenNodes().get(0))) {
			Type = "StaticCall";
		}

		nodeString = node.toString();
		String nameToSplit = nodeString;
		if (isGenericParent) {
			nameToSplit = ((ClassOrInterfaceType) node).getName();
			//if (isLeaf) {
				// if it is a generic parent which counts as a leaf, then when
				// it is participating in a path
				// as a parent, it should be GenericClass and not a simple
				// ClassOrInterfaceType.
			Type = Common.GenericClass ; //+ ":" + nameToSplit;
			//}
		}
		if (commandLineValues.NoSubtokenization) {
			SplitName = new ArrayList<>(List.of(nameToSplit));
		} else {
			SplitName = Common.splitToSubtokens(nameToSplit);
		}
		//SplitName = splitNameParts.stream().collect(Collectors.joining(Common.internalSeparator));

		Name = Common.normalizeName(nodeString, Common.BlankWord);
		if (Name.length() > Common.c_MaxLabelLength) {
			Name = Name.substring(0, Common.c_MaxLabelLength);
		} else if (node instanceof ClassOrInterfaceType && ((ClassOrInterfaceType) node).isBoxedType()) {
			Name = ((ClassOrInterfaceType) node).toUnboxedType().toString();
		}

		/*if (Common.isMethod(node, Type)) {
			Name = SplitName = Common.methodName;
		}*/

		if (SplitName.stream().collect(Collectors.joining(Common.internalSeparator)).length() == 0) {
			SplitName = new ArrayList<>();
			SplitName.add(Name);
			/*if (node instanceof IntegerLiteralExpr && !NumericalKeepValues.contains(SplitName)) {
				// This is a numeric literal, but not in our white list
				SplitName = "<NUM>";
			}*/
		}

		this.childId = computeChildId();
		if (Common.MethodDeclaration.equals(RawType)) {
			this.childId = 0;
		}
	}

    public Property(CommandLineValues commandLineValues, Node fakeNode) {
	    // Only for creating a FakeNode
        this.isLeaf = true;
        this.commandLineValues = commandLineValues;
        RawType = Type = Common.EOS;
		this.node = fakeNode;

        SplitName = new ArrayList<>();

        Name = Common.EOS;

        this.childId = saturateChildId(reorderedChildren(fakeNode.getParentNode()).size());
    }

    public Property(CommandLineValues commandLineValues, Node fakeNode, String name) {
        // Only for creating a generic type
        this.isLeaf = true;
        this.commandLineValues = commandLineValues;
        RawType = Type = "NameExpr";
        this.node = fakeNode;

        SplitName = Common.splitToSubtokens(name);

        Name = name;

        this.childId = saturateChildId(reorderedChildren(fakeNode.getParentNode()).size());
    }

	public String getRawType() {
		return RawType;
	}
	
	public String getType() {
		return Type;
	}

	public String getType(boolean shorten) {
		if (shorten) {
			return shortTypes.getOrDefault(Type, Type);
		} else {
			return Type;
		}
	}

	public ArrayList<String> getName() {
		return SplitName;
	}

	public boolean isLeaf() {
		return isLeaf;
	}

	public int getChildId() {
		return childId;
	}

	public Node getNode() {
		return this.node;
	}

	private int computeChildId() {
		Node parent = node.getParentNode();
        List<Node> parentsChildren = reorderedChildren(parent);
        int childId = 0;
		for (Node child: parentsChildren) {
			/*if (!Common.isValidNode(child)) {
				continue;
			}*/
			if (child.getRange().equals(node.getRange())) {
				return saturateChildId(childId);
			}
			childId++;
		}
		return saturateChildId(childId);
	}

	@Override
	public String toString() {
		return nodeString;
	}

	public static List<Node> reorderedChildren(Node parent) {
		Property parentProperty = parent.getUserData(Common.PropertyKey);
		if (parentProperty != null && parentProperty.ReorderedChildren != null) {
			return parentProperty.ReorderedChildren;
		}
		List<Node> children = parent.getChildrenNodes();
		Stream<Node> childrenStream = children.stream();
        if (parentProperty != null && Common.MethodCallExpr.equals(parentProperty.getRawType())) {
			// Parent is CallExpression.
			//if (Common.isValidNode(children.get(0))) {
				// There is an object, thus we don't need the null node
			childrenStream = childrenStream.filter(Common::isValidNode);
			//}
			childrenStream = childrenStream.sorted(Comparator.comparing(n -> n.getBegin()));
        } else {
        	childrenStream = childrenStream.filter(Common::isValidNode);
		}
        List<Node> result = childrenStream.collect(Collectors.toList());
        if (parentProperty != null) {
			parentProperty.ReorderedChildren = result;
		}
        return result;
    }

    private int saturateChildId(int childId) {
		return Math.min(childId, commandLineValues.MaxChildId);
	}
	
	private static Map<String, String> shortTypes = Collections.unmodifiableMap(new HashMap<String, String>() {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		{
			put("ArrayAccessExpr", "ArAc");
			put("ArrayBracketPair", "ArBr");
			put("ArrayCreationExpr", "ArCr");
			put("ArrayCreationLevel", "ArCrLvl");
			put("ArrayInitializerExpr", "ArIn");
			put("ArrayType", "ArTy");
			put("AssertStmt", "Asrt");
			put("AssignExpr:and", "AsAn");
			put("AssignExpr:assign", "As");
			put("AssignExpr:lShift", "AsLS");
			put("AssignExpr:minus", "AsMi");
			put("AssignExpr:or", "AsOr");
			put("AssignExpr:plus", "AsP");
			put("AssignExpr:rem", "AsRe");
			put("AssignExpr:rSignedShift", "AsRSS");
			put("AssignExpr:rUnsignedShift", "AsRUS");
			put("AssignExpr:slash", "AsSl");
			put("AssignExpr:star", "AsSt");
			put("AssignExpr:xor", "AsX");
			put("BinaryExpr:and", "And");
			put("BinaryExpr:binAnd", "BinAnd");
			put("BinaryExpr:binOr", "BinOr");
			put("BinaryExpr:divide", "Div");
			put("BinaryExpr:equals", "Eq");
			put("BinaryExpr:greater", "Gt");
			put("BinaryExpr:greaterEquals", "Geq");
			put("BinaryExpr:less", "Ls");
			put("BinaryExpr:lessEquals", "Leq");
			put("BinaryExpr:lShift", "LS");
			put("BinaryExpr:minus", "Minus");
			put("BinaryExpr:notEquals", "Neq");
			put("BinaryExpr:or", "Or");
			put("BinaryExpr:plus", "Plus");
			put("BinaryExpr:remainder", "Mod");
			put("BinaryExpr:rSignedShift", "RSS");
			put("BinaryExpr:rUnsignedShift", "RUS");
			put("BinaryExpr:times", "Mul");
			put("BinaryExpr:xor", "Xor");
			put("BlockStmt", "Bk");
			put("BooleanLiteralExpr", "BoolEx");
			put("CastExpr", "Cast");
			put("CatchClause", "Catch");
			put("CharLiteralExpr", "CharEx");
			put("ClassExpr", "ClsEx");
			put("ClassOrInterfaceDeclaration", "ClsD");
			put("ClassOrInterfaceType", "Cls");
			put("ConditionalExpr", "Cond");
			put("ConstructorDeclaration", "Ctor");
			put("DoStmt", "Do");
			put("DoubleLiteralExpr", "Dbl");
			put("EmptyMemberDeclaration", "Emp");
			put("EnclosedExpr", "Enc");
			put("ExplicitConstructorInvocationStmt", "ExpCtor");
			put("ExpressionStmt", "Ex");
			put("FieldAccessExpr", "Fld");
			put("FieldDeclaration", "FldDec");
			put("ForeachStmt", "Foreach");
			put("ForStmt", "For");
			put("IfStmt", "If");
			put("InitializerDeclaration", "Init");
			put("InstanceOfExpr", "InstanceOf");
			put("IntegerLiteralExpr", "IntEx");
			put("IntegerLiteralMinValueExpr", "IntMinEx");
			put("LabeledStmt", "Labeled");
			put("LambdaExpr", "Lambda");
			put("LongLiteralExpr", "LongEx");
			put("MarkerAnnotationExpr", "MarkerExpr");
			put("MemberValuePair", "Mvp");
			put("MethodCallExpr", "Cal");
			put("MethodDeclaration", "Mth");
			put("MethodReferenceExpr", "MethRef");
			put("NameExpr", "Nm");
			put("NormalAnnotationExpr", "NormEx");
			put("NullLiteralExpr", "Null");
			put("ObjectCreationExpr", "ObjEx");
			put("Parameter", "Prm");
			put("PrimitiveType", "Prim");
			put("QualifiedNameExpr", "Qua");
			put("ReturnStmt", "Ret");
			put("SingleMemberAnnotationExpr", "SMEx");
			put("StringLiteralExpr", "StrEx");
			put("SuperExpr", "SupEx");
			put("SwitchEntryStmt", "SwiEnt");
			put("SwitchStmt", "Switch");
			put("SynchronizedStmt", "Sync");
			put("ThisExpr", "This");
			put("ThrowStmt", "Thro");
			put("TryStmt", "Try");
			put("TypeDeclarationStmt", "TypeDec");
			put("TypeExpr", "Type");
			put("TypeParameter", "TypePar");
			put("UnaryExpr:inverse", "Inverse");
			put("UnaryExpr:negative", "Neg");
			put("UnaryExpr:not", "Not");
			put("UnaryExpr:posDecrement", "PosDec");
			put("UnaryExpr:posIncrement", "PosInc");
			put("UnaryExpr:positive", "Pos");
			put("UnaryExpr:preDecrement", "PreDec");
			put("UnaryExpr:preIncrement", "PreInc");
			put("UnionType", "Unio");
			put("VariableDeclarationExpr", "VDE");
			put("VariableDeclarator", "VD");
			put("VariableDeclaratorId", "VDID");
			put("VoidType", "Void");
			put("WhileStmt", "While");
			put("WildcardType", "Wild");
		}
	});
}
