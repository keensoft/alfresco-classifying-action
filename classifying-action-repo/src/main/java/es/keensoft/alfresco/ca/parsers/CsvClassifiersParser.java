package es.keensoft.alfresco.ca.parsers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.alfresco.model.ContentModel;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class CsvClassifiersParser implements ClassifiersParserInterface {
	private static final Log log = LogFactory.getLog(CsvClassifiersParser.class);

	private ServiceRegistry serviceRegistry;

	@Override
	public List<String[]> parseClassifiers(NodeRef nodeRef) {
		try {
			ArrayList<String[]> classifiersValues = new ArrayList<String[]>();
			InputStream documentIs = readDocument(nodeRef);
			BufferedReader br = new BufferedReader(new InputStreamReader(documentIs, StandardCharsets.UTF_8));
			String line;
			while ((line = br.readLine()) != null) {
				classifiersValues.add(line.split("\\|"));
			}
			log.debug("Loaded '" + classifiersValues.size() + "' classifier row(s).");

			if (log.isTraceEnabled()) {
				log.trace("Loaded classifiers:");
				for (String[] classifiersArray : classifiersValues) {
					log.trace(Arrays.toString(classifiersArray));
				}
			}
			return classifiersValues;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private InputStream readDocument(NodeRef nodeRef) {
		ContentReader reader = serviceRegistry.getContentService().getReader(nodeRef, ContentModel.PROP_CONTENT);
		return reader.getContentInputStream();
	}

	public void setServiceRegistry(ServiceRegistry serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
	}
}
