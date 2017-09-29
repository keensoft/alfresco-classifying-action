package es.keensoft.alfresco.ca.actions;

import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;

public class RenameAction implements ClassifyingActionInterface {
	private ServiceRegistry serviceRegistry;

	@Override
	public void execute(NodeRef nodeRef, String[] classifierValuesRow, Map<String, String> extraParams) {
		String oldNameValue = classifierValuesRow[0];
		String newNameValue = classifierValuesRow[1];
		String currentName = (String) serviceRegistry.getNodeService().getProperty(nodeRef, ContentModel.PROP_NAME);
		String newName = composeNewName(oldNameValue, newNameValue, currentName);
		serviceRegistry.getNodeService().setProperty(nodeRef, ContentModel.PROP_NAME, newName);
	}

	private String composeNewName(String oldNameValue, String newNameValue, String currentName) {
		return currentName.replace(oldNameValue, newNameValue);
	}

	public void setServiceRegistry(ServiceRegistry serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
	}

}
