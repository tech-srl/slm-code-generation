package JavaExtractor.FeaturesEntities;

import com.github.javaparser.Position;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.visitor.GenericVisitor;
import com.github.javaparser.ast.visitor.VoidVisitor;

import java.util.Comparator;
import java.util.Optional;

public class FakeNode extends Node {
    Node parent;
    Optional<Position> position;

    public FakeNode(Node parent) {
        this.parent = parent;
        this.position = parent.getChildrenNodes().stream().map(child -> child.getBegin())
                .max(Comparator.naturalOrder()).map(pos -> pos.withColumn(pos.column + 1));
    }

    @Override
    public <R, A> R accept(GenericVisitor<R, A> v, A arg) {
        return null;
    }

    @Override
    public boolean equals(final Object obj) {
        return this == obj;
    }

    @Override
    public <A> void accept(VoidVisitor<A> v, A arg) {
    }

    @Override
    public Node getParentNode() {
        return parent;
    }

    @Override
    public Position getBegin() {
        return position.get();
    }
};
