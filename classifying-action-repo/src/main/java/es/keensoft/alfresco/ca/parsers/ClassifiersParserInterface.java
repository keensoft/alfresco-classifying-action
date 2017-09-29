package es.keensoft.alfresco.ca.parsers;

import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;

public interface ClassifiersParserInterface {
	/**
	 * Parses the document that contains classifiers. For each entity there is 
	 * an ordered array of values.
	 * @param nodeRef NodeRef of document
	 * @return List of classifiers arrays
	 */
	List<String[]> parseClassifiers(NodeRef nodeRef);
}
