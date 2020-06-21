package JavaExtractor.FeaturesEntities;

import JavaExtractor.Common.Common;
import com.github.javaparser.ast.Node;

import java.util.Arrays;
import java.util.List;

public class SerializedNode {
    private List<Object> node;
    //String val;
    //Integer child_id;

    public SerializedNode(Node node, boolean isRightToLeft, boolean insideSite) {
        Property nodeProperty = node.getUserData(Common.PropertyKey);
        String val = Common.maybeFlipNode(nodeProperty.getType(true), isRightToLeft, insideSite);
        Integer child_id = nodeProperty.getChildId();
        this.node = Arrays.asList(val, child_id);
    }

    public SerializedNode(Integer childId, String subtoken) {
        String val = subtoken;
        Integer child_id = childId;
        node = Arrays.asList(val, child_id);
    }

    public List<Object> getNode() {
        return node;
    }
}
