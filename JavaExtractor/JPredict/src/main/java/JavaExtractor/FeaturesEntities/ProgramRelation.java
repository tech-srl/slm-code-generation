package JavaExtractor.FeaturesEntities;

import java.util.ArrayList;
import java.util.function.Function;

import JavaExtractor.NodesPath;

public class ProgramRelation {
	private Property m_Source;
	private Property m_Target;
	private NodesPath m_Path;

	public ProgramRelation(Property sourceName, Property targetName, NodesPath path) {
		m_Source = sourceName;
		m_Target = targetName;
		m_Path = path;
	}

	public String toString() {
		return String.format("%s,%s,%s", m_Source.getName(), m_Path,
				m_Target.getName());
	}

	public NodesPath getPath() {
		return m_Path;
	}

	public Property getSource() {
		return m_Source;
	}

	public Property getTarget() {
		return m_Target;
	}
}
