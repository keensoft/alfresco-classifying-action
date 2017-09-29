package es.keensoft.alfresco.ca.actions;

import java.util.Map;

import org.alfresco.service.cmr.repository.NodeRef;

public interface ClassifyingActionInterface {
	/**
	 * Executes the action upon the document
	 * @param nodeRef NodeRef of the document
	 * @param classifierValuesRow Classifiers values matched for this document 
	 * @param extraParams Extra parameters configured for the action, in form of key-value
	 */
	public void execute(NodeRef nodeRef, String[] classifierValuesRow, Map<String, String> extraParams);
}
