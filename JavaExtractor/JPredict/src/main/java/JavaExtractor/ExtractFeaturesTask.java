package JavaExtractor;

import JavaExtractor.Common.CommandLineValues;
import JavaExtractor.Common.Common;
import JavaExtractor.FeaturesEntities.CompletionSite;
import com.github.javaparser.ParseProblemException;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

public class ExtractFeaturesTask implements Callable<Void> {
	CommandLineValues m_CommandLineValues;
	Path filePath;

	public ExtractFeaturesTask(CommandLineValues commandLineValues, Path path) {
		m_CommandLineValues = commandLineValues;
		this.filePath = path;
	}

	@Override
	public Void call() throws Exception {
		//System.err.println("Extracting file: " + filePath);
		processFile();
		//System.err.println("Done with file: " + filePath);
		return null;
	}

	public void processFile() {
		ArrayList<CompletionSite> completionSites;
		try {
			completionSites = extractSingleFile();
		} catch (ParseProblemException e) {
			//e.printStackTrace();
			return;
		}
		if (completionSites.isEmpty()) {
			return;
		}

		List<String> lines = serializeCompletionSites(completionSites);
		for (String line: lines) {
			System.out.println(line);
		}
	}

	public ArrayList<CompletionSite> extractSingleFile() throws ParseProblemException {
		String code = null;
		try {
			code = new String(Files.readAllBytes(this.filePath));
		} catch (IOException e) {
			e.printStackTrace();
			code = Common.EmptyString;
		}
		FeatureExtractor featureExtractor = new FeatureExtractor(m_CommandLineValues, this.filePath);

		ArrayList<CompletionSite> features = featureExtractor.extractFeatures(code);

		return features;
	}

	public List<String> serializeCompletionSites(ArrayList<CompletionSite> completionSites) {
		if (completionSites == null || completionSites.isEmpty()) {
			return new ArrayList<>();
		}

		List<String> methodsOutputs = new ArrayList<>();

		for (CompletionSite site : completionSites) {
			
			String toPrint = Common.EmptyString;
			try {
				toPrint = site.toString();
			} catch (Exception ex) {
				continue;
			}

			if (toPrint.length() > 0) {
				methodsOutputs.add(toPrint);
			}
		}
		return methodsOutputs;
	}
}
