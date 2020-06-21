package JavaExtractor;

import JavaExtractor.Common.CommandLineValues;
import JavaExtractor.Common.Common;
import JavaExtractor.FeaturesEntities.CompletionSite;
import JavaExtractor.FeaturesEntities.SerializedPath;
import JavaExtractor.FeaturesEntities.SerializedSite;
import com.github.javaparser.ParseException;
import com.google.gson.Gson;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Map.entry;
import static org.junit.Assert.*;
/*
public class ExtractFeaturesTaskTest {

    void parseOutput(String output, HashMap<String, String> expectedIsToken,
                     HashMap<String, String> expectedTargetChildId,
                     HashMap<String, ArrayList<String>> expectedContexts) {
        for (String line : output.split("\n")) {
            String[] parts = line.split(" ");
            String target = null;
            for (int i = 0; i < parts.length; i++) {
                String currentPart = parts[i];
                if (i == 0) {
                    target = currentPart;
                    continue;
                }
                if (i == 1) {
                    expectedIsToken.put(target, currentPart);
                    continue;
                }
                if (i == 2) {
                    expectedTargetChildId.put(target, currentPart);
                    continue;
                }
                if (i == 3) {
                    expectedContexts.put(target, new ArrayList<>());
                }
                expectedContexts.get(target).add(currentPart);
            }
        }
    }

    @org.junit.Test
    public void processFile() throws IOException, ParseException {
        CommandLineValues commandLineValues = new CommandLineValues();
        commandLineValues.MaxPathLength = 8;
        commandLineValues.MaxPathWidth = 2;
        commandLineValues.PrintLastEos = true;
        commandLineValues.typesToGen = Map.ofEntries(
                entry("IfStmt", 0));
        ExtractFeaturesTask task = new ExtractFeaturesTask(commandLineValues, Paths.get("src", "test", "Test.java"));
        ArrayList<CompletionSite> features = task.extractSingleFile();
        String toPrint = task.serializeCompletionSites(features);
        HashMap<String, String> actualIsToken = new HashMap<>();
        HashMap<String, String> actualTargetChildId = new HashMap<>();
        HashMap<String, ArrayList<String>> actualContexts = new HashMap<>();

        parseOutput(toPrint, actualIsToken, actualTargetChildId, actualContexts);

        HashMap<String, String> expectedIsToken = new HashMap<>();
        HashMap<String, String> expectedTargetChildId = new HashMap<>();
        HashMap<String, ArrayList<String>> expectedContexts = new HashMap<>();

        final String expected =
                        "Gt 0 0 BLANK,Mth*0|Bk*2|If*0 get|value|<EOS>,Nm_INV*2|Cal_INV*0|Ex_INV*0|Bk_INV*1|If_INV*0 void|<EOS>,Void*0|Mth*0|Bk*2|If*0 hello|world|<EOS>,StrEx_INV*1|Cal_INV*0|Ex_INV*0|Bk_INV*1|If_INV*0 foo|bar|<EOS>,Nm*1|Mth*0|Bk*2|If*0\n" +
                        "Nm 0 0 BLANK,Mth*0|Bk*2|If*0|Gt*0 hello|world|<EOS>,StrEx_INV*1|Cal_INV*0|Ex_INV*0|Bk_INV*1|If_INV*0|Gt_INV*0 get|value|<EOS>,Nm_INV*2|Cal_INV*0|Ex_INV*0|Bk_INV*1|If_INV*0|Gt_INV*0 void|<EOS>,Void*0|Mth*0|Bk*2|If*0|Gt*0 foo|bar|<EOS>,Nm*1|Mth*0|Bk*2|If*0|Gt*0\n" +
                        "xxx 1 0 BLANK,Mth*0|Bk*2|If*0|Gt*0 hello|world|<EOS>,StrEx_INV*1|Cal_INV*0|Ex_INV*0|Bk_INV*1|If_INV*0|Gt_INV*0|Nm_INV*0 get|value|<EOS>,Nm_INV*2|Cal_INV*0|Ex_INV*0|Bk_INV*1|If_INV*0|Gt_INV*0|Nm_INV*0 void|<EOS>,Void*0|Mth*0|Bk*2|If*0|Gt*0|Nm*0 foo|bar|<EOS>,Nm*1|Mth*0|Bk*2|If*0|Gt*0|Nm*0\n" +
                        "yyy 1 1 BLANK,Mth*0|Bk*2|If*0|Gt*0|xxx*NO_CHILD hello|world|<EOS>,StrEx_INV*1|Cal_INV*0|Ex_INV*0|Bk_INV*1|If_INV*0|Gt_INV*0|Nm_INV*0|xxx*NO_CHILD get|value|<EOS>,Nm_INV*2|Cal_INV*0|Ex_INV*0|Bk_INV*1|If_INV*0|Gt_INV*0|Nm_INV*0|xxx*NO_CHILD void|<EOS>,Void*0|Mth*0|Bk*2|If*0|Gt*0|Nm*0|xxx*NO_CHILD foo|bar|<EOS>,Nm*1|Mth*0|Bk*2|If*0|Gt*0|Nm*0|xxx*NO_CHILD\n" +
                        "<EOS> 1 2 BLANK,Mth*0|Bk*2|If*0|Gt*0|xxx*NO_CHILD|yyy*NO_CHILD hello|world|<EOS>,StrEx_INV*1|Cal_INV*0|Ex_INV*0|Bk_INV*1|If_INV*0|Gt_INV*0|Nm_INV*0|xxx*NO_CHILD|yyy*NO_CHILD get|value|<EOS>,Nm_INV*2|Cal_INV*0|Ex_INV*0|Bk_INV*1|If_INV*0|Gt_INV*0|Nm_INV*0|xxx*NO_CHILD|yyy*NO_CHILD void|<EOS>,Void*0|Mth*0|Bk*2|If*0|Gt*0|Nm*0|xxx*NO_CHILD|yyy*NO_CHILD foo|bar|<EOS>,Nm*1|Mth*0|Bk*2|If*0|Gt*0|Nm*0|xxx*NO_CHILD|yyy*NO_CHILD\n" +
                        "IntEx 0 1 BLANK,Mth*0|Bk*2|If*0|Gt*0 hello|world|<EOS>,StrEx_INV*1|Cal_INV*0|Ex_INV*0|Bk_INV*1|If_INV*0|Gt_INV*0 xxx|yyy|<EOS>,Nm*0|Gt*0 get|value|<EOS>,Nm_INV*2|Cal_INV*0|Ex_INV*0|Bk_INV*1|If_INV*0|Gt_INV*0 void|<EOS>,Void*0|Mth*0|Bk*2|If*0|Gt*0 foo|bar|<EOS>,Nm*1|Mth*0|Bk*2|If*0|Gt*0\n" +
                        "1 1 0 BLANK,Mth*0|Bk*2|If*0|Gt*0 hello|world|<EOS>,StrEx_INV*1|Cal_INV*0|Ex_INV*0|Bk_INV*1|If_INV*0|Gt_INV*0|IntEx_INV*1 xxx|yyy|<EOS>,Nm*0|Gt*0|IntEx*1 get|value|<EOS>,Nm_INV*2|Cal_INV*0|Ex_INV*0|Bk_INV*1|If_INV*0|Gt_INV*0|IntEx_INV*1 void|<EOS>,Void*0|Mth*0|Bk*2|If*0|Gt*0|IntEx*1 foo|bar|<EOS>,Nm*1|Mth*0|Bk*2|If*0|Gt*0|IntEx*1\n" +
                        "<EOS> 1 1 BLANK,Mth*0|Bk*2|If*0|Gt*0|1*NO_CHILD hello|world|<EOS>,StrEx_INV*1|Cal_INV*0|Ex_INV*0|Bk_INV*1|If_INV*0|Gt_INV*0|IntEx_INV*1|1*NO_CHILD xxx|yyy|<EOS>,Nm*0|Gt*0|IntEx*1|1*NO_CHILD get|value|<EOS>,Nm_INV*2|Cal_INV*0|Ex_INV*0|Bk_INV*1|If_INV*0|Gt_INV*0|IntEx_INV*1|1*NO_CHILD void|<EOS>,Void*0|Mth*0|Bk*2|If*0|Gt*0|IntEx*1|1*NO_CHILD foo|bar|<EOS>,Nm*1|Mth*0|Bk*2|If*0|Gt*0|IntEx*1|1*NO_CHILD";

        parseOutput(expected, expectedIsToken, expectedTargetChildId, expectedContexts);
        assertEquals(expectedIsToken, actualIsToken);
        assertEquals(expectedTargetChildId, actualTargetChildId);
        for (String key : expectedContexts.keySet()) {
            ArrayList<String> expectedContextsForKey = expectedContexts.get(key);
            ArrayList<String> actualContextsForKey = actualContexts.get(key);
            assertEquals(expectedContextsForKey.size(), actualContextsForKey.size());
            assertTrue(expectedContextsForKey.containsAll(actualContextsForKey));
            assertTrue(actualContextsForKey.containsAll(expectedContextsForKey));
        }
    }

    @org.junit.Test
    public void processJson() throws IOException, ParseException {
        CommandLineValues commandLineValues = new CommandLineValues();
        commandLineValues.MaxPathLength = 8;
        commandLineValues.MaxPathWidth = 2;
        commandLineValues.PrintLastEos = false;
        commandLineValues.JsonOutput = true;
        commandLineValues.MaxContexts = 10;
        commandLineValues.typesToGen = Map.ofEntries(
                entry("IfStmt", 0));
        ExtractFeaturesTask task = new ExtractFeaturesTask(commandLineValues, Paths.get("src", "test", "Test.java"));
        ArrayList<CompletionSite> features = task.extractSingleFile();
        String toPrint = task.serializeCompletionSites(features);

        Gson gson = new Gson();
        SerializedSite site = gson.fromJson(toPrint, SerializedSite.class);
        int max_contexts;
        ArrayList<String> targets = new ArrayList<>();
        ArrayList<Integer> is_token = new ArrayList<>();
        ArrayList<Integer> target_child_id = new ArrayList<>();
        ArrayList<String> root_paths = new ArrayList<>();
        //ArrayList<String> root_paths_child_ids = new ArrayList<>();
        ArrayList<String> paths = new ArrayList<>();

        ArrayList<String> expectedTargets = new ArrayList<>(Arrays.asList("Gt", "Nm", "xxx", "yyy", "<EOS>", "IntEx", "1", "<EOS>"));
        ArrayList<Integer> expectedIsToken = new ArrayList<>(Arrays.asList(0, 0, 1, 1, 1, 0, 1, 1));
        ArrayList<Integer> expectedTargetChildId = new ArrayList<>(Arrays.asList(0, 0, 0, 1, 2, 1, 0, 1));
        List<List<String>> expectedRootNodes = Arrays.asList(
                Arrays.asList("Mth","Bk","If"), Arrays.asList("Mth","Bk","If","Gt"), Arrays.asList("Mth","Bk","If","Gt"),
                Arrays.asList("Mth","Bk","If","Gt","xxx"), Arrays.asList("Mth","Bk","If","Gt","xxx","yyy"),
                Arrays.asList("Mth","Bk","If","Gt"), Arrays.asList("Mth","Bk","If","Gt"), Arrays.asList("Mth","Bk","If","Gt", "1")
        );
        List<List<Integer>> expectedRootChildIds = Arrays.asList(
                Arrays.asList(0,2,0), Arrays.asList(0,2,0,0), Arrays.asList(0,2,0,0),
                Arrays.asList(0,2,0,0,-1), Arrays.asList(0,2,0,0,-1,-1),
                Arrays.asList(0,2,0,0), Arrays.asList(0,2,0,0), Arrays.asList(0,2,0,0,-1)
        );
        ArrayList<String> expectedPathStrings = new ArrayList<>();
        expectedPathStrings.add("get|value|<EOS>,Nm_INV*2|Cal_INV*0|Ex_INV*0|Bk_INV*1|If_INV*0 void|<EOS>,Void*0|Mth*0|Bk*2|If*0 hello|world|<EOS>,StrEx_INV*1|Cal_INV*0|Ex_INV*0|Bk_INV*1|If_INV*0 foo|bar|<EOS>,Nm*1|Mth*0|Bk*2|If*0");
        expectedPathStrings.add("hello|world|<EOS>,StrEx_INV*1|Cal_INV*0|Ex_INV*0|Bk_INV*1|If_INV*0|Gt_INV*0 get|value|<EOS>,Nm_INV*2|Cal_INV*0|Ex_INV*0|Bk_INV*1|If_INV*0|Gt_INV*0 void|<EOS>,Void*0|Mth*0|Bk*2|If*0|Gt*0 foo|bar|<EOS>,Nm*1|Mth*0|Bk*2|If*0|Gt*0");
        expectedPathStrings.add("hello|world|<EOS>,StrEx_INV*1|Cal_INV*0|Ex_INV*0|Bk_INV*1|If_INV*0|Gt_INV*0|Nm_INV*0 get|value|<EOS>,Nm_INV*2|Cal_INV*0|Ex_INV*0|Bk_INV*1|If_INV*0|Gt_INV*0|Nm_INV*0 void|<EOS>,Void*0|Mth*0|Bk*2|If*0|Gt*0|Nm*0 foo|bar|<EOS>,Nm*1|Mth*0|Bk*2|If*0|Gt*0|Nm*0");
        expectedPathStrings.add("hello|world|<EOS>,StrEx_INV*1|Cal_INV*0|Ex_INV*0|Bk_INV*1|If_INV*0|Gt_INV*0|Nm_INV*0|xxx*NO_CHILD get|value|<EOS>,Nm_INV*2|Cal_INV*0|Ex_INV*0|Bk_INV*1|If_INV*0|Gt_INV*0|Nm_INV*0|xxx*NO_CHILD void|<EOS>,Void*0|Mth*0|Bk*2|If*0|Gt*0|Nm*0|xxx*NO_CHILD foo|bar|<EOS>,Nm*1|Mth*0|Bk*2|If*0|Gt*0|Nm*0|xxx*NO_CHILD");
        expectedPathStrings.add("hello|world|<EOS>,StrEx_INV*1|Cal_INV*0|Ex_INV*0|Bk_INV*1|If_INV*0|Gt_INV*0|Nm_INV*0|xxx*NO_CHILD|yyy*NO_CHILD get|value|<EOS>,Nm_INV*2|Cal_INV*0|Ex_INV*0|Bk_INV*1|If_INV*0|Gt_INV*0|Nm_INV*0|xxx*NO_CHILD|yyy*NO_CHILD void|<EOS>,Void*0|Mth*0|Bk*2|If*0|Gt*0|Nm*0|xxx*NO_CHILD|yyy*NO_CHILD foo|bar|<EOS>,Nm*1|Mth*0|Bk*2|If*0|Gt*0|Nm*0|xxx*NO_CHILD|yyy*NO_CHILD");
        expectedPathStrings.add("hello|world|<EOS>,StrEx_INV*1|Cal_INV*0|Ex_INV*0|Bk_INV*1|If_INV*0|Gt_INV*0 xxx|yyy|<EOS>,Nm*0|Gt*0 get|value|<EOS>,Nm_INV*2|Cal_INV*0|Ex_INV*0|Bk_INV*1|If_INV*0|Gt_INV*0 void|<EOS>,Void*0|Mth*0|Bk*2|If*0|Gt*0 foo|bar|<EOS>,Nm*1|Mth*0|Bk*2|If*0|Gt*0");
        expectedPathStrings.add("hello|world|<EOS>,StrEx_INV*1|Cal_INV*0|Ex_INV*0|Bk_INV*1|If_INV*0|Gt_INV*0|IntEx_INV*1 xxx|yyy|<EOS>,Nm*0|Gt*0|IntEx*1 get|value|<EOS>,Nm_INV*2|Cal_INV*0|Ex_INV*0|Bk_INV*1|If_INV*0|Gt_INV*0|IntEx_INV*1 void|<EOS>,Void*0|Mth*0|Bk*2|If*0|Gt*0|IntEx*1 foo|bar|<EOS>,Nm*1|Mth*0|Bk*2|If*0|Gt*0|IntEx*1");
        expectedPathStrings.add("hello|world|<EOS>,StrEx_INV*1|Cal_INV*0|Ex_INV*0|Bk_INV*1|If_INV*0|Gt_INV*0|IntEx_INV*1|1*NO_CHILD xxx|yyy|<EOS>,Nm*0|Gt*0|IntEx*1|1*NO_CHILD get|value|<EOS>,Nm_INV*2|Cal_INV*0|Ex_INV*0|Bk_INV*1|If_INV*0|Gt_INV*0|IntEx_INV*1|1*NO_CHILD void|<EOS>,Void*0|Mth*0|Bk*2|If*0|Gt*0|IntEx*1|1*NO_CHILD foo|bar|<EOS>,Nm*1|Mth*0|Bk*2|If*0|Gt*0|IntEx*1|1*NO_CHILD");
        List<List<String>> expectedPaths = expectedPathStrings.stream().map(s -> s.split(" "))
                .map(l -> Stream.concat(Arrays.stream(l), Collections.nCopies(10 - l.length, Common.BlankWord).stream()))
                .map(l -> l.collect(Collectors.toList()))
                .collect(Collectors.toList());

        assertEquals(8, site.getNum_targets());
        assertEquals(expectedTargets, site.getTargets());
        assertEquals(expectedIsToken, site.getIs_token());
        assertEquals(expectedTargetChildId, site.getTarget_child_id());

        List<List<SerializedPath>> batchedActual = site.getInternal_paths();
        assertEquals(8, batchedActual.size());
        for (int i = 0 ; i < expectedPaths.size() ; i++) {
            assertEquals(10, batchedActual.get(i).size());
            assertEquals(new HashSet<>(expectedPaths.get(i)), new HashSet<>(batchedActual.get(i)));
        }
    }

    List<List<String>> batch(List<String> list, int batchSize) {
        List<List<String>> result = new ArrayList<>();
        for (int i = 0 ; i * batchSize < list.size() ; i++) {
            result.add(list.subList(i * batchSize, (i + 1) * batchSize));
        }
        return result;
    }

    @org.junit.Test
    public void processFileTwoOccurrences() throws IOException, ParseException {
        CommandLineValues commandLineValues = new CommandLineValues();
        commandLineValues.MaxPathLength = 8;
        commandLineValues.MaxPathWidth = 2;
        commandLineValues.PrintLastEos = true;
        commandLineValues.typesToGen = Map.ofEntries(
                entry("IfStmt", 0));
        ExtractFeaturesTask task = new ExtractFeaturesTask(commandLineValues, Paths.get("src", "test", "TestTwoOccurrences.java"));
        ArrayList<CompletionSite> features = task.extractSingleFile();
        String toPrint = task.serializeCompletionSites(features);
        HashMap<String, String> actualIsToken = new HashMap<>();
        HashMap<String, String> actualTargetChildId = new HashMap<>();
        HashMap<String, ArrayList<String>> actualContexts = new HashMap<>();

        parseOutput(toPrint, actualIsToken, actualTargetChildId, actualContexts);

        HashMap<String, String> expectedIsToken = new HashMap<>();
        HashMap<String, String> expectedTargetChildId = new HashMap<>();
        HashMap<String, ArrayList<String>> expectedContexts = new HashMap<>();

        final String expected =
                "Gt 0 0 BLANK,Mth*0|Bk*2|If*0 get|value|<EOS>,Nm_INV*2|Cal_INV*0|Ex_INV*0|Bk_INV*1|If_INV*0 void|<EOS>,Void*0|Mth*0|Bk*2|If*0 hello|world|<EOS>,StrEx_INV*1|Cal_INV*0|Ex_INV*0|Bk_INV*1|If_INV*0 foo|bar|<EOS>,Nm*1|Mth*0|Bk*2|If*0 xxx|yyy|<EOS>,Nm_INV*0|Ret_INV*1|Bk_INV*2|If_INV*0\n" +
                        "Nm 0 0 BLANK,Mth*0|Bk*2|If*0|Gt*0 hello|world|<EOS>,StrEx_INV*1|Cal_INV*0|Ex_INV*0|Bk_INV*1|If_INV*0|Gt_INV*0 get|value|<EOS>,Nm_INV*2|Cal_INV*0|Ex_INV*0|Bk_INV*1|If_INV*0|Gt_INV*0 void|<EOS>,Void*0|Mth*0|Bk*2|If*0|Gt*0 foo|bar|<EOS>,Nm*1|Mth*0|Bk*2|If*0|Gt*0 xxx|yyy|<EOS>,Nm_INV*0|Ret_INV*1|Bk_INV*2|If_INV*0|Gt_INV*0\n" +
                        "xxx 1 0 BLANK,Mth*0|Bk*2|If*0|Gt*0 hello|world|<EOS>,StrEx_INV*1|Cal_INV*0|Ex_INV*0|Bk_INV*1|If_INV*0|Gt_INV*0|Nm_INV*0 get|value|<EOS>,Nm_INV*2|Cal_INV*0|Ex_INV*0|Bk_INV*1|If_INV*0|Gt_INV*0|Nm_INV*0 void|<EOS>,Void*0|Mth*0|Bk*2|If*0|Gt*0|Nm*0 foo|bar|<EOS>,Nm*1|Mth*0|Bk*2|If*0|Gt*0|Nm*0 xxx|yyy|<EOS>,Nm_INV*0|Ret_INV*1|Bk_INV*2|If_INV*0|Gt_INV*0|Nm_INV*0\n" +
                        "yyy 1 1 BLANK,Mth*0|Bk*2|If*0|Gt*0|xxx*NO_CHILD hello|world|<EOS>,StrEx_INV*1|Cal_INV*0|Ex_INV*0|Bk_INV*1|If_INV*0|Gt_INV*0|Nm_INV*0|xxx*NO_CHILD get|value|<EOS>,Nm_INV*2|Cal_INV*0|Ex_INV*0|Bk_INV*1|If_INV*0|Gt_INV*0|Nm_INV*0|xxx*NO_CHILD void|<EOS>,Void*0|Mth*0|Bk*2|If*0|Gt*0|Nm*0|xxx*NO_CHILD foo|bar|<EOS>,Nm*1|Mth*0|Bk*2|If*0|Gt*0|Nm*0|xxx*NO_CHILD xxx|yyy|<EOS>,Nm_INV*0|Ret_INV*1|Bk_INV*2|If_INV*0|Gt_INV*0|Nm_INV*0|xxx*NO_CHILD\n" +
                        "<EOS> 1 2 BLANK,Mth*0|Bk*2|If*0|Gt*0|xxx*NO_CHILD|yyy*NO_CHILD hello|world|<EOS>,StrEx_INV*1|Cal_INV*0|Ex_INV*0|Bk_INV*1|If_INV*0|Gt_INV*0|Nm_INV*0|xxx*NO_CHILD|yyy*NO_CHILD get|value|<EOS>,Nm_INV*2|Cal_INV*0|Ex_INV*0|Bk_INV*1|If_INV*0|Gt_INV*0|Nm_INV*0|xxx*NO_CHILD|yyy*NO_CHILD void|<EOS>,Void*0|Mth*0|Bk*2|If*0|Gt*0|Nm*0|xxx*NO_CHILD|yyy*NO_CHILD foo|bar|<EOS>,Nm*1|Mth*0|Bk*2|If*0|Gt*0|Nm*0|xxx*NO_CHILD|yyy*NO_CHILD xxx|yyy|<EOS>,Nm_INV*0|Ret_INV*1|Bk_INV*2|If_INV*0|Gt_INV*0|Nm_INV*0|xxx*NO_CHILD|yyy*NO_CHILD\n" +
                        "IntEx 0 1 BLANK,Mth*0|Bk*2|If*0|Gt*0 hello|world|<EOS>,StrEx_INV*1|Cal_INV*0|Ex_INV*0|Bk_INV*1|If_INV*0|Gt_INV*0 xxx|yyy|<EOS>,Nm*0|Gt*0 get|value|<EOS>,Nm_INV*2|Cal_INV*0|Ex_INV*0|Bk_INV*1|If_INV*0|Gt_INV*0 void|<EOS>,Void*0|Mth*0|Bk*2|If*0|Gt*0 foo|bar|<EOS>,Nm*1|Mth*0|Bk*2|If*0|Gt*0 xxx|yyy|<EOS>,Nm_INV*0|Ret_INV*1|Bk_INV*2|If_INV*0|Gt_INV*0\n" +
                        "1 1 0 BLANK,Mth*0|Bk*2|If*0|Gt*0 hello|world|<EOS>,StrEx_INV*1|Cal_INV*0|Ex_INV*0|Bk_INV*1|If_INV*0|Gt_INV*0|IntEx_INV*1 xxx|yyy|<EOS>,Nm*0|Gt*0|IntEx*1 get|value|<EOS>,Nm_INV*2|Cal_INV*0|Ex_INV*0|Bk_INV*1|If_INV*0|Gt_INV*0|IntEx_INV*1 void|<EOS>,Void*0|Mth*0|Bk*2|If*0|Gt*0|IntEx*1 foo|bar|<EOS>,Nm*1|Mth*0|Bk*2|If*0|Gt*0|IntEx*1 xxx|yyy|<EOS>,Nm_INV*0|Ret_INV*1|Bk_INV*2|If_INV*0|Gt_INV*0|IntEx_INV*1\n" +
                        "<EOS> 1 1 BLANK,Mth*0|Bk*2|If*0|Gt*0|1*NO_CHILD hello|world|<EOS>,StrEx_INV*1|Cal_INV*0|Ex_INV*0|Bk_INV*1|If_INV*0|Gt_INV*0|IntEx_INV*1|1*NO_CHILD xxx|yyy|<EOS>,Nm*0|Gt*0|IntEx*1|1*NO_CHILD get|value|<EOS>,Nm_INV*2|Cal_INV*0|Ex_INV*0|Bk_INV*1|If_INV*0|Gt_INV*0|IntEx_INV*1|1*NO_CHILD void|<EOS>,Void*0|Mth*0|Bk*2|If*0|Gt*0|IntEx*1|1*NO_CHILD foo|bar|<EOS>,Nm*1|Mth*0|Bk*2|If*0|Gt*0|IntEx*1|1*NO_CHILD xxx|yyy|<EOS>,Nm_INV*0|Ret_INV*1|Bk_INV*2|If_INV*0|Gt_INV*0|IntEx_INV*1|1*NO_CHILD";

        parseOutput(expected, expectedIsToken, expectedTargetChildId, expectedContexts);
        assertEquals(expectedIsToken, actualIsToken);
        assertEquals(expectedTargetChildId, actualTargetChildId);
        for (String key : expectedContexts.keySet()) {
            ArrayList<String> expectedContextsForKey = expectedContexts.get(key);
            ArrayList<String> actualContextsForKey = actualContexts.get(key);
            assertEquals(expectedContextsForKey.size(), actualContextsForKey.size());
            assertTrue(expectedContextsForKey.containsAll(actualContextsForKey));
            assertTrue(actualContextsForKey.containsAll(expectedContextsForKey));
        }
    }

    @org.junit.Test
    public void processFileWithLongPaths() throws IOException, ParseException {
        CommandLineValues commandLineValues = new CommandLineValues();
        commandLineValues.MaxPathLength = 8;
        commandLineValues.MaxPathWidth = 2;
        commandLineValues.PrintLastEos = false;
        commandLineValues.typesToGen = Map.ofEntries(
                entry("IfStmt", 0));
        ExtractFeaturesTask task = new ExtractFeaturesTask(commandLineValues, Paths.get("src", "test", "TestLongPaths.java"));
        ArrayList<CompletionSite> features = task.extractSingleFile();
        String toPrint = task.serializeCompletionSites(features);
        HashMap<String, String> actualIsToken = new HashMap<>();
        HashMap<String, String> actualTargetChildId = new HashMap<>();
        HashMap<String, ArrayList<String>> actualContexts = new HashMap<>();

        parseOutput(toPrint, actualIsToken, actualTargetChildId, actualContexts);

        HashMap<String, String> expectedIsToken = new HashMap<>();
        HashMap<String, String> expectedTargetChildId = new HashMap<>();
        HashMap<String, ArrayList<String>> expectedContexts = new HashMap<>();

        final String expected =
                "Gt 0 0 BLANK,Do*0|Bk*0|Do*0|Bk*0|Do*0|Bk*0|If*0 void,Do*0|Bk*0|Do*0|Bk*0|Do*0|Bk*0|If*0 hello|world,StrEx_INV*1|Cal_INV*0|Ex_INV*0|Bk_INV*1|If_INV*0 true,BoolEx_INV*1|Do_INV*0|Bk_INV*0|Do_INV*0|Bk_INV*0|If_INV*0 true,Do*0|Bk*0|Do*0|Bk*0|Do*0|Bk*0|If*0 true,BoolEx_INV*1|Do_INV*0|Bk_INV*0|If_INV*0 get|value,Nm_INV*2|Cal_INV*0|Ex_INV*0|Bk_INV*1|If_INV*0\n" +
                "Nm 0 0 BLANK,Bk*0|Do*0|Bk*0|Do*0|Bk*0|If*0|Gt*0 get|value,Nm_INV*2|Cal_INV*0|Ex_INV*0|Bk_INV*1|If_INV*0|Gt_INV*0 true,BoolEx_INV*1|Do_INV*0|Bk_INV*0|If_INV*0|Gt_INV*0 true,BoolEx_INV*1|Do_INV*0|Bk_INV*0|Do_INV*0|Bk_INV*0|If_INV*0|Gt_INV*0 void,Bk*0|Do*0|Bk*0|Do*0|Bk*0|If*0|Gt*0 hello|world,StrEx_INV*1|Cal_INV*0|Ex_INV*0|Bk_INV*1|If_INV*0|Gt_INV*0\n" +
                "xxx 1 0 BLANK,Bk*0|Do*0|Bk*0|Do*0|Bk*0|If*0|Gt*0 get|value,Nm_INV*2|Cal_INV*0|Ex_INV*0|Bk_INV*1|If_INV*0|Gt_INV*0|Nm_INV*0 true,BoolEx_INV*1|Do_INV*0|Bk_INV*0|If_INV*0|Gt_INV*0|Nm_INV*0 true,BoolEx_INV*1|Do_INV*0|Bk_INV*0|Do_INV*0|Bk_INV*0|If_INV*0|Gt_INV*0|Nm_INV*0 void,Bk*0|Do*0|Bk*0|Do*0|Bk*0|If*0|Gt*0|Nm*0 hello|world,StrEx_INV*1|Cal_INV*0|Ex_INV*0|Bk_INV*1|If_INV*0|Gt_INV*0|Nm_INV*0\n" +
                "yyy 1 1 BLANK,Bk*0|Do*0|Bk*0|Do*0|Bk*0|If*0|Gt*0|xxx*NO_CHILD get|value,Nm_INV*2|Cal_INV*0|Ex_INV*0|Bk_INV*1|If_INV*0|Gt_INV*0|Nm_INV*0|xxx*NO_CHILD true,BoolEx_INV*1|Do_INV*0|Bk_INV*0|If_INV*0|Gt_INV*0|Nm_INV*0|xxx*NO_CHILD true,BoolEx_INV*1|Do_INV*0|Bk_INV*0|Do_INV*0|Bk_INV*0|If_INV*0|Gt_INV*0|Nm_INV*0|xxx*NO_CHILD void,Bk*0|Do*0|Bk*0|Do*0|Bk*0|If*0|Gt*0|Nm*0|xxx*NO_CHILD hello|world,StrEx_INV*1|Cal_INV*0|Ex_INV*0|Bk_INV*1|If_INV*0|Gt_INV*0|Nm_INV*0|xxx*NO_CHILD\n" +
                "<EOS> 1 2 BLANK,Bk*0|Do*0|Bk*0|Do*0|Bk*0|If*0|Gt*0|xxx*NO_CHILD|yyy*NO_CHILD get|value,Nm_INV*2|Cal_INV*0|Ex_INV*0|Bk_INV*1|If_INV*0|Gt_INV*0|Nm_INV*0|xxx*NO_CHILD|yyy*NO_CHILD true,BoolEx_INV*1|Do_INV*0|Bk_INV*0|If_INV*0|Gt_INV*0|Nm_INV*0|xxx*NO_CHILD|yyy*NO_CHILD true,BoolEx_INV*1|Do_INV*0|Bk_INV*0|Do_INV*0|Bk_INV*0|If_INV*0|Gt_INV*0|Nm_INV*0|xxx*NO_CHILD|yyy*NO_CHILD void,Bk*0|Do*0|Bk*0|Do*0|Bk*0|If*0|Gt*0|Nm*0|xxx*NO_CHILD|yyy*NO_CHILD hello|world,StrEx_INV*1|Cal_INV*0|Ex_INV*0|Bk_INV*1|If_INV*0|Gt_INV*0|Nm_INV*0|xxx*NO_CHILD|yyy*NO_CHILD\n" +
                "IntEx 0 1 BLANK,Bk*0|Do*0|Bk*0|Do*0|Bk*0|If*0|Gt*0 void,Bk*0|Do*0|Bk*0|Do*0|Bk*0|If*0|Gt*0 hello|world,StrEx_INV*1|Cal_INV*0|Ex_INV*0|Bk_INV*1|If_INV*0|Gt_INV*0 xxx|yyy,Nm*0|Gt*0 get|value,Nm_INV*2|Cal_INV*0|Ex_INV*0|Bk_INV*1|If_INV*0|Gt_INV*0 true,BoolEx_INV*1|Do_INV*0|Bk_INV*0|If_INV*0|Gt_INV*0 true,BoolEx_INV*1|Do_INV*0|Bk_INV*0|Do_INV*0|Bk_INV*0|If_INV*0|Gt_INV*0\n" +
                "1 1 0 BLANK,Bk*0|Do*0|Bk*0|Do*0|Bk*0|If*0|Gt*0 void,Bk*0|Do*0|Bk*0|Do*0|Bk*0|If*0|Gt*0|IntEx*1 hello|world,StrEx_INV*1|Cal_INV*0|Ex_INV*0|Bk_INV*1|If_INV*0|Gt_INV*0|IntEx_INV*1 xxx|yyy,Nm*0|Gt*0|IntEx*1 get|value,Nm_INV*2|Cal_INV*0|Ex_INV*0|Bk_INV*1|If_INV*0|Gt_INV*0|IntEx_INV*1 true,BoolEx_INV*1|Do_INV*0|Bk_INV*0|If_INV*0|Gt_INV*0|IntEx_INV*1 true,BoolEx_INV*1|Do_INV*0|Bk_INV*0|Do_INV*0|Bk_INV*0|If_INV*0|Gt_INV*0|IntEx_INV*1\n" +
                "<EOS> 1 1 BLANK,Bk*0|Do*0|Bk*0|Do*0|Bk*0|If*0|Gt*0|1*NO_CHILD void,Bk*0|Do*0|Bk*0|Do*0|Bk*0|If*0|Gt*0|IntEx*1|1*NO_CHILD hello|world,StrEx_INV*1|Cal_INV*0|Ex_INV*0|Bk_INV*1|If_INV*0|Gt_INV*0|IntEx_INV*1|1*NO_CHILD xxx|yyy,Nm*0|Gt*0|IntEx*1|1*NO_CHILD get|value,Nm_INV*2|Cal_INV*0|Ex_INV*0|Bk_INV*1|If_INV*0|Gt_INV*0|IntEx_INV*1|1*NO_CHILD true,BoolEx_INV*1|Do_INV*0|Bk_INV*0|If_INV*0|Gt_INV*0|IntEx_INV*1|1*NO_CHILD true,BoolEx_INV*1|Do_INV*0|Bk_INV*0|Do_INV*0|Bk_INV*0|If_INV*0|Gt_INV*0|IntEx_INV*1|1*NO_CHILD";

        parseOutput(expected, expectedIsToken, expectedTargetChildId, expectedContexts);
        assertEquals(expectedIsToken, actualIsToken);
        assertEquals(expectedTargetChildId, actualTargetChildId);
        for (String key : expectedContexts.keySet()) {
            ArrayList<String> expectedContextsForKey = expectedContexts.get(key);
            ArrayList<String> actualContextsForKey = actualContexts.get(key);
            assertEquals(expectedContextsForKey.size(), actualContextsForKey.size());
            assertTrue(expectedContextsForKey.containsAll(actualContextsForKey));
            assertTrue(actualContextsForKey.containsAll(expectedContextsForKey));
        }
    }
} */