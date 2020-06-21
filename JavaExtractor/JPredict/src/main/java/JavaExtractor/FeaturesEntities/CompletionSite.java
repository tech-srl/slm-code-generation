package JavaExtractor.FeaturesEntities;

import JavaExtractor.Common.CommandLineValues;
import JavaExtractor.Common.Common;
import JavaExtractor.FeatureExtractor;
import JavaExtractor.NodesPath;
import com.github.javaparser.ast.Node;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class CompletionSite {
    private Node head;
    private Set<Node> nodes = Collections.newSetFromMap( new IdentityHashMap<>());

    public Set<NodesPath> getPathsForHeadNode() {
        return pathsForHeadNode;
    }

    private Set<NodesPath> pathsForHeadNode = Collections.newSetFromMap( new IdentityHashMap<>());

    public NodesPath getRelativePathForNode(Property property) {
        return relativePathForNode.get(property);
    }

    private HashMap<Property, NodesPath> relativePathForNode = new HashMap<>();
    private HashMap<Property, ArrayList<NodesPath>> internalPathsForNode = new HashMap<>();
    private NodesPath rootToHeadPath;
    private CommandLineValues commandLineValues;
    private Node root;

    public Path getFilePath() {
        return filePath;
    }

    private Path filePath;

    public CompletionSite(CommandLineValues commandLineValues, Node head, Path filePath, Node root) {
        this.commandLineValues = commandLineValues;
        this.filePath = filePath;
        this.head = head;
        this.root = root;
    }

    public void setNodes(ArrayList<Node> nodes) {
        this.nodes.addAll(nodes);
    }

    public boolean inSite(Node node) {
        return this.nodes.contains(node);
    }

    public void setPathsForNode(ProgramFeatures singleMethodFeatures, FeatureExtractor featureExtractor) {
        NodesPath headRootPath = featureExtractor.getRootToNodePath(this.root, this.head);
        //if (headRootPath.getLength() > commandLineValues.MaxPathLength + 1) {
        //    headRootPath = headRootPath.takeSuffixOfLength(commandLineValues.MaxPathLength);
        //}
        Property headProperty = head.getUserData(Common.PropertyKey);
        rootToHeadPath = headRootPath;

        NodesPath headNodesPath = new NodesPath(commandLineValues);
        headNodesPath.appendDownNode(head);
        this.relativePathForNode.put(headProperty, headNodesPath);

        for (ProgramRelation relation : singleMethodFeatures.getFeatures()) {
            NodesPath path = relation.getPath();

            Property target = relation.getTarget();
            Property source = relation.getSource();

            if (source.getNode() == head) {
                this.relativePathForNode.put(target, path);
                continue;
            }

            if (target.getNode() == head) {
                this.pathsForHeadNode.add(path);
                continue;
            }

            if (!internalPathsForNode.containsKey(target)) {
                internalPathsForNode.put(target, new ArrayList<>());
            }
            //if (path.getLength() > this.commandLineValues.MaxPathLength + 1) {
            //    path = path.takeSuffixOfLength(this.commandLineValues.MaxPathLength);
            //}

            internalPathsForNode.get(target).add(path);

        }
    }

    public Set<Node> getNodes() {
        return nodes;
    }

    public NodesPath getRootToHeadPath() {
        return this.rootToHeadPath;
    }


    public HashMap<Property, ArrayList<NodesPath>> getInternalPathsForNode() {
        return this.internalPathsForNode;
    }

    @Override
    public String toString() {
        if (this.commandLineValues.JsonOutput) {
            Gson gson = null;
            if (this.commandLineValues.PrettyPrint) {
                gson = new GsonBuilder().setPrettyPrinting().create();
            }
            else {
                gson = new Gson();
            }
            SerializedSite serializedSite = new SerializedSite(commandLineValues, this);
            String json = gson.toJson(serializedSite);
            return json;
        } else {
            return toCsvString();
        }
    }

    private String toCsvString() {
        ArrayList<String> rows = new ArrayList<>();
        for (Node node : this.nodes) {
            Property property = node.getUserData(Common.PropertyKey);
            StringBuilder rowBuilder = new StringBuilder();
            rowBuilder.append(property.getType(true));
            rowBuilder.append(" 0"); // not name
            rowBuilder.append(" " + property.getChildId()); // childId

            if (node == this.head) {
                rowBuilder.append(" HEAD_PATHS:");
                for (NodesPath path: this.pathsForHeadNode) {
                    rowBuilder.append(" " + path.toString(false, new ArrayList<>(), true, this.getNodes()));
                }
            }
            NodesPath rootPath = this.rootToHeadPath;
            rowBuilder.append(" ROOT_PATH:");
            rowBuilder.append(" " + rootPath.toString(false, new ArrayList<>(), false, this.getNodes()));
            NodesPath relativePath = this.relativePathForNode.get(property);
            rowBuilder.append(" RELATIVE_PATH:");
            rowBuilder.append(" " + relativePath.toString(false, new ArrayList<>(), false, this.getNodes()));


            if (!internalPathsForNode.containsKey(property)) {
                internalPathsForNode.put(property, new ArrayList<>());
            }
            ArrayList<NodesPath> internalPathsForThisNode = this.internalPathsForNode.get(property);


            try {
                /*if (!this.internalPathsForNode.containsKey(property)) {
                    System.err.println("ERROR in file: " + this.filePath + ", internal_paths for node does not contain node: " +
                            node.toString());
                    continue;
                }*/
                rowBuilder.append(" INTERNAL:");
                for (NodesPath path : internalPathsForThisNode) {
                    rowBuilder.append(" ");
                    rowBuilder.append(path.toString(this.getNodes()));
                }
            } catch (NullPointerException ex) {
                ex.printStackTrace();
            }

            rows.add(rowBuilder.toString());

            if (property.isLeaf() && !property.getRawType().equals(Common.EOS)) {
                for (int i = 0; i <= property.getName().size(); i++) {
                    String subtok = i < property.getName().size() ? property.getName().get(i) : Common.EOS;
                    List<String> prevSubtokens = property.getName().subList(0, i);
                    StringBuilder subtokRowBuilder = new StringBuilder();
                    subtokRowBuilder.append(subtok);
                    subtokRowBuilder.append(" 1"); // name
                    subtokRowBuilder.append(" " + i); // the current subtoken index
                    subtokRowBuilder.append(" ROOT_PATH:");
                    subtokRowBuilder.append(" " + rootPath.toString(false, new ArrayList<>(), false, this.getNodes()));
                    subtokRowBuilder.append(" RELATIVE_PATH:");
                    subtokRowBuilder.append(" " + relativePath.toString(true, prevSubtokens, false, this.getNodes()));
                    subtokRowBuilder.append(" INTERNAL:");
                    for (NodesPath path : internalPathsForThisNode) {
                        subtokRowBuilder.append(" ");
                        subtokRowBuilder.append(path.toString(true, prevSubtokens, true, this.getNodes()));
                    }
                    rows.add(subtokRowBuilder.toString());
                }
            }
        }
        // stringBuilder.append(features.stream().map(ProgramRelation::toString).collect(Collectors.joining(" ")));

        String toPrint = rows.stream().collect(Collectors.joining("\n"));

        if (commandLineValues.PrettyPrint) {
            toPrint = toPrint.replace(" ", "\n\t");
        }
        return toPrint;
    }

    public Node getHeadNode() {
        return this.head;
    }
}
