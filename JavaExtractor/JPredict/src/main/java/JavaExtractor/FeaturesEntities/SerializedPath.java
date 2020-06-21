package JavaExtractor.FeaturesEntities;

import JavaExtractor.Common.Common;
import JavaExtractor.NodesPath;
import com.github.javaparser.ast.Node;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;


public class SerializedPath {
    ArrayList<String> sources;
    transient String token;
    List<SerializedNode> nodes;

    public SerializedPath(NodesPath path, Set<Node> nodesInSite, boolean includeLastNode, List<String> prevSubtokens, boolean includeOtherEndNode) {
        this.sources = includeOtherEndNode ? path.getOtherEndNodeProperty().getName() : Common.emptyArrayList;
        this.token = String.join(",", this.sources);

        Stream<SerializedNode> pathNodes = path.getNodes(includeLastNode).stream()
                .map(n -> new SerializedNode(n, path.isRightToLeft(), Common.isInsideSite(n, nodesInSite)));
        //Stream<SerializedNode> prevSubtokenNodes = prevSubtokens.stream().map(sub -> new SerializedNode(sub));
        Stream<SerializedNode> prevSubtokenNodes = IntStream.range(0, prevSubtokens.size())
                .mapToObj(i -> new SerializedNode(i, prevSubtokens.get(i)));
        this.nodes = Stream.concat(pathNodes, prevSubtokenNodes).collect(Collectors.toList());
    }

    public ArrayList<String> getSources() {
        return sources;
    }

    public List<SerializedNode> getNodes() {
        return nodes;
    }

    public String getToken() {
        return token;
    }
}
