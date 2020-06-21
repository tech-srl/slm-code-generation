package JavaExtractor.Visitors;

import JavaExtractor.Common.Common;
import JavaExtractor.FeaturesEntities.CompletionSite;
import JavaExtractor.FeaturesEntities.Property;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.visitor.TreeVisitor;

public class FinderVisitor extends TreeVisitor {
    private Node toFind;
    private int counter = 0;
    private int foundIndex = 0;

    public FinderVisitor(CompletionSite completionSite) {
        toFind = completionSite.getHeadNode();
    }

    @Override
    public void process(Node node) {
        try {
            Property nodeProperty = node.getUserData(Common.PropertyKey);
            if (nodeProperty != null && toFind.getUserData(Common.PropertyKey).toString().equals(nodeProperty.toString())) {
                if (nodeProperty == toFind.getUserData(Common.PropertyKey)) {
                    foundIndex = counter;
                }
                counter++;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public int getFoundIndex() {
        return foundIndex;
    }
}
