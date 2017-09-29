package es.keensoft.alfresco.ca.classifiers;

import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;

public interface ClassifierInterface {
	/**
	 * Determines the array of values, matching by node and the provided index
	 * @param nodeRef Document to get values for
	 * @param classifiersValues All the values arrays parsed
	 * @param keyIndex Index for the value to use as key in the matching with document
	 * @return Values array for document
	 */
	public String[] getValuesRow(NodeRef nodeRef, List<String[]> classifiersValues, int keyIndex);
}
