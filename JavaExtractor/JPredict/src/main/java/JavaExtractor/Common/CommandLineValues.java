package JavaExtractor.Common;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import java.io.File;
import java.util.Map;

import static java.util.Map.entry;

/**
 * This class handles the programs arguments.
 */
public class CommandLineValues {
	@Option(name = "--file", required = false)
	public File File = null;

	@Option(name = "--dir", required = false, forbids = "--file")
	public String Dir = null;

	@Option(name = "--max_path_length", required = true)
	public int MaxPathLength;

	@Option(name = "--max_path_width", required = true)
	public int MaxPathWidth;

	@Option(name = "--num_threads", required = false)
	public int NumThreads = 32;

	@Option(name = "--min_code_len", required = false)
	public int MinCodeLength = 1;

	@Option(name = "--max_code_len", required = false)
	public int MaxCodeLength = 10000;

	@Option(name = "--pretty_print", required = false)
	public boolean PrettyPrint = false;

	@Option(name = "--max_child_id", required = false)
	public int MaxChildId = 3;

	@Option(name = "--print_last_eos", required = false)
	public boolean PrintLastEos = false;

	@Option(name = "--json_output", required = false)
	public boolean JsonOutput = false;

	@Option(name = "--max_contexts", required = false)
	public int MaxContexts = 200;

	@Option(name = "--max_internal_paths", required = false)
	public int MaxInternalPaths = 20;

	@Option(name = "--max_nodes", required = false)
	public int MaxNodesInSite = 10;

	@Option(name = "--no_subtokenization", required = false)
	public boolean NoSubtokenization = false;

    @Option(name = "--exp", required = false)
	public boolean ExtractAllExpressions = false;

    public Map<String, Integer> typesToGen = Map.ofEntries(
	        entry("IfStmt", 0));
    //entry(Common.MethodDeclaration, 2));

	public CommandLineValues(String... args) throws CmdLineException {
		CmdLineParser parser = new CmdLineParser(this);
		try {
			parser.parseArgument(args);
		} catch (CmdLineException e) {
			System.err.println(e.getMessage());
			parser.printUsage(System.err);
			throw e;
		}
	}

	public CommandLineValues() {

	}
}