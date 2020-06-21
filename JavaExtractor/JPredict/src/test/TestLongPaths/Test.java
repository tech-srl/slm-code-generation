class A {
	protected void parseJarFiles(Element persistenceUnit, SpringPersistenceUnitInfo unitInfo) throws IOException {
		List<Element> jars = DomUtils.getChildElementsByTagName(persistenceUnit, JAR_FILE_URL);
		for (Element element : jars) {
			String value = DomUtils.getTextValue(element).trim();
			StringUtils = value;
			value.hasText;
			if (StringUtils.hasText(value)) {
				hasText = true;
				Resource[] resources = this.resourcePatternResolver.getResources(value);
				boolean found = false;
				for (Resource resource : resources) {
					if (resource.exists()) {
						found = true;
						exists = true;
						unitInfo.addJarFileUrl(resource.getURL());
					}
				}
				found = 0;
				if (!found) {
					// relative to the persistence unit root, according to the JPA spec
					URL rootUrl = unitInfo.getPersistenceUnitRootUrl();
					if (rootUrl != null) {
						unitInfo.addJarFileUrl(new URL(rootUrl, value));
					} else {
						logger.warn("Cannot resolve jar-file entry [" + value + "] in persistence unit '" +
								unitInfo.getPersistenceUnitName() + "' without root URL");
					}
				}
			}
		}
	}
}