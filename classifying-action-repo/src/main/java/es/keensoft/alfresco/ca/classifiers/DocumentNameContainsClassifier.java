package es.keensoft.alfresco.ca.classifiers;

import java.util.Arrays;
import java.util.List;

import org.alfresco.model.ContentModel;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class DocumentNameContainsClassifier implements ClassifierInterface {
	private static final Log log = LogFactory.getLog(DocumentNameContainsClassifier.class);

	private ServiceRegistry serviceRegistry;

	@Override
	public String[] getValuesRow(NodeRef nodeRef, List<String[]> classifiersValues, int keyIndex) {
		String fileName = (String) serviceRegistry.getNodeService().getProperty(nodeRef, ContentModel.PROP_NAME);
		for (String[] row : classifiersValues) {
			String key = row[keyIndex];
			if (fileName.contains(key)) {
				if (log.isTraceEnabled())
					log.trace("'" + key + "' -> '" + Arrays.toString(row) + "'");
				return row;
			}
		}
		throw new RuntimeException("Value not found for name '" + fileName + "' and key index '" + keyIndex + "'.");
	}

	public void setServiceRegistry(ServiceRegistry serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
	}

}
