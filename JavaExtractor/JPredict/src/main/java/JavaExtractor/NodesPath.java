package JavaExtractor;

import JavaExtractor.Common.CommandLineValues;
import JavaExtractor.Common.Common;
import JavaExtractor.FeaturesEntities.Property;
import com.github.javaparser.ast.Node;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static JavaExtractor.Common.Common.BlankWord;

public class NodesPath {
    ArrayList<Node> upNodes = new ArrayList<>();
    Node midNode = null;
    ArrayList<Node> downNodes = new ArrayList<>();
    CommandLineValues commandLineValues;
    Property otherEndNodeProperty = null;

    public NodesPath(CommandLineValues commandLineValues) {
        this.commandLineValues = commandLineValues;
    }

    public void appendUpNode(Node node) {
        if (upNodes.isEmpty()) {
            this.otherEndNodeProperty = node.getUserData(Common.PropertyKey);
        }
        upNodes.add(node);

    }

    public void setMidNode(Node node) {
        midNode = node;
    }

    public void appendDownNode(Node node) {
        downNodes.add(node);
    }

    public Node first() {
        if (upNodes.size() > 0) {
            return upNodes.get(0);
        }
        if (midNode != null) {
            return midNode;
        }
        return downNodes.get(0);
    }

    public Node last() {
        if (downNodes.size() > 0) {
            return downNodes.get(downNodes.size() - 1);
        }
        if (midNode != null) {
            return midNode;
        }
        return upNodes.get(upNodes.size() - 1);
    }

    public ArrayList<Node> getUpNodes() {
        return upNodes;
    }

    public ArrayList<Node> getDownNodes() {
        return downNodes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NodesPath)) return false;
        NodesPath nodesPath = (NodesPath) o;
        return Objects.equals(upNodes, nodesPath.upNodes) &&
                Objects.equals(midNode, nodesPath.midNode) &&
                Objects.equals(downNodes, nodesPath.downNodes) &&
                Objects.equals(commandLineValues, nodesPath.commandLineValues) &&
                Objects.equals(otherEndNodeProperty, nodesPath.otherEndNodeProperty);
    }

    @Override
    public int hashCode() {
        return Objects.hash(upNodes, midNode, downNodes, commandLineValues, otherEndNodeProperty);
    }

    public int getLength() {
        int length = this.upNodes.size() + this.downNodes.size();
        if (this.midNode != null) {
            length++;
        }
        return length;
    }

    public NodesPath takeSuffixOfLength(int maxPathLength) {
        // We keep also the node to be predicted
        int totalMaxLen = maxPathLength + 1;
        NodesPath newPath = new NodesPath(this.commandLineValues);

        int startIndex = this.downNodes.size() - totalMaxLen;
        if (startIndex < 0) {
            startIndex = 0;
        }
        newPath.downNodes = new ArrayList<>(this.downNodes.subList(startIndex, this.downNodes.size()));
        if (newPath.getLength() < totalMaxLen) {
            newPath.midNode = this.midNode;
        }
        if (newPath.getLength() < totalMaxLen) {
            startIndex = this.upNodes.size() - (totalMaxLen - newPath.getLength());
            newPath.upNodes = new ArrayList<>(this.upNodes.subList(startIndex, this.upNodes.size()));
        }

        if (newPath.getLength() != totalMaxLen) {
            throw new RuntimeException();
        }
        newPath.otherEndNodeProperty = this.otherEndNodeProperty;
        return newPath;
    }

    public String toString(Set<Node> nodesInSite) {
        return toString(false, new ArrayList<>(), true, nodesInSite);
    }

    public String toString(boolean includeLastNode, List<String> prevSubtokens, boolean includeOtherEndNode, Set<Node> nodesInSite) {
        String otherEndNodeString = null;
        if (includeOtherEndNode) {
            Stream<String> otherEndNodeStream = otherEndNodeProperty.getName().stream();
            if (this.commandLineValues.PrintLastEos) {
                otherEndNodeStream = Stream.concat(otherEndNodeStream, Stream.of(Common.EOS));
            }
            otherEndNodeString = otherEndNodeStream.collect(Collectors.joining(Common.internalSeparator));
        } else {
            otherEndNodeString = BlankWord;
        }

        ArrayList<Node> concatenatedNodes = getNodes(includeLastNode);
        List<String> pathParts = nodesToStrings(concatenatedNodes, true, nodesInSite);

        pathParts.addAll(prevSubtokens.stream().map(sub -> sub + "*NO_CHILD").collect(Collectors.toList()));

        //int toSkip = pathParts.size() - this.commandLineValues.MaxPathLength;
        //if (toSkip < 0) {
        //    toSkip = 0;
        //}
        String pathString = pathParts.stream() // .skip(toSkip)
                .collect(Collectors.joining(Common.internalSeparator));

        return String.format("%s,%s", otherEndNodeString, pathString);
    }

    public List<String> nodesToStrings(ArrayList<Node> nodesList, boolean addChildId, Set<Node> nodesInSite) {
        return nodesList.stream().map(node -> node.getUserData(Common.PropertyKey))
                    .map(p -> Common.maybeFlipNode(
                                    p.getType(true), isRightToLeft(), Common.isInsideSite(p, nodesInSite))
                                    + (addChildId ? "*" + p.getChildId(): Common.EmptyString))
                    .collect(Collectors.toList());
    }

    public ArrayList<Node> getNodes(boolean includeLastNode) {
        ArrayList<Node> concatenatedNodes = new ArrayList<>();
        concatenatedNodes.addAll(upNodes);
        if (midNode != null) {
            concatenatedNodes.add(midNode);
        }

        concatenatedNodes.addAll(downNodes.subList(0, downNodes.size() - 1));

        if (includeLastNode) {
            concatenatedNodes.add(downNodes.get(downNodes.size() - 1));
        }
        return concatenatedNodes;
    }

    public boolean isRightToLeft() {
        return first().getBegin().isAfter(last().getBegin());
    }

    public boolean isBottomUp() {
        return downNodes.size() == 0;
    }

    public Property getOtherEndNodeProperty() {
        return otherEndNodeProperty;
    }

    public void setOtherEndNodeProperty(Property otherEndNodeProperty) {
        this.otherEndNodeProperty = otherEndNodeProperty;
    }

}
