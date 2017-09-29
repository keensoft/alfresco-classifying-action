package es.keensoft.alfresco.ca;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.action.ParameterDefinitionImpl;
import org.alfresco.repo.action.executer.ActionExecuterAbstractBase;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.repo.version.VersionModel;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ParameterDefinition;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.version.Version;
import org.alfresco.service.cmr.version.VersionType;
import org.alfresco.service.namespace.QName;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;

import es.keensoft.alfresco.ca.actions.ClassifyingActionInterface;
import es.keensoft.alfresco.ca.classifiers.ClassifierInterface;
import es.keensoft.alfresco.ca.parsers.ClassifiersParserInterface;

public class ClassifyingActionExecuter extends ActionExecuterAbstractBase implements BeanFactoryAware {
	private static final Log log = LogFactory.getLog(ClassifyingActionExecuter.class);

	private static final String PARAM_CONFIGURATION = "configuration";

	private final Map<String, ClassifyingActionDefinition> configuredActions = new HashMap<String, ClassifyingActionDefinition>();

	private List<String[]> classifiersValues = null;
	private String classifiersDocumentNodeRef = null;
	private Date classifiersDocumentModificationDate = null;

	private ClassifiersParserInterface classifiersParser;
	private BeanFactory beanFactory;
	private ServiceRegistry serviceRegistry;
	private Properties properties;

	@Override
	public void init() {
		super.init();
		loadConfiguredActionsInProperties();
	}

	private void loadConfiguredActionsInProperties() {
		String propActions = properties.getProperty("es.keensoft.alfresco.classifying-action.actions");
		log.debug("Recovering configuration for: " + propActions);
		String[] actionsArray = propActions.split(",");

		for (String action : actionsArray) {
			ClassifyingActionDefinition actionDefinition = new ClassifyingActionDefinition();
			actionDefinition.setName(action);

			String actionBeanName = properties.getProperty("es.keensoft.alfresco.classifying-action." + action + ".action");
			ClassifyingActionInterface actionBean = (ClassifyingActionInterface) beanFactory.getBean(actionBeanName);
			actionDefinition.setAction(actionBean);

			String classifierBeanName = properties.getProperty("es.keensoft.alfresco.classifying-action." + action + ".classifier");
			ClassifierInterface classifierBean = (ClassifierInterface) beanFactory.getBean(classifierBeanName);
			actionDefinition.setClassifier(classifierBean);

			String classifierKeyIndex = properties.getProperty("es.keensoft.alfresco.classifying-action." + action + ".classifier.key-index");
			actionDefinition.setClassifierKeyIndex(Integer.parseInt(classifierKeyIndex));

			String extraParams = properties.getProperty("es.keensoft.alfresco.classifying-action." + action + ".extra-params");
			actionDefinition.setExtraParams(parseExtraParams(extraParams));

			configuredActions.put(action, actionDefinition);
		}
		log.debug("Loaded '" + configuredActions.size() + "' action(s) from properties.");
	}

	private Map<String, String> parseExtraParams(String params) {
		Map<String, String> paramsMap = new HashMap<String, String>();

		String[] paramsArray = params.split("\\|");
		for (String param : paramsArray) {
			String[] paramParts = param.split("\\=");
			if (paramParts.length == 2)
				paramsMap.put(paramParts[0], paramParts[1]);
		}

		return paramsMap;
	}

	@Override
	protected void executeImpl(Action action, NodeRef actionedUponNodeRef) {
		updateClassifiersIfNeeded();
		ClassifyingActionDefinition classifyingAction = searchClassifyingAction(action);

		boolean error = false;
		Map<NodeRef, String> workResults = new HashMap<NodeRef, String>();
		if (isFolder(actionedUponNodeRef)) {
			List<ChildAssociationRef> childAssocs = getChildrenNodes(actionedUponNodeRef);
			for (ChildAssociationRef childAssociationRef : childAssocs) {
				NodeRef childRef = childAssociationRef.getChildRef();
				String workResult = doWork(classifyingAction, childRef);
				workResults.put(childRef, workResult);
				error = workResult != null ? true : error;
			}
		} else {
			String workResult = doWork(classifyingAction, actionedUponNodeRef);
			workResults.put(actionedUponNodeRef, workResult);
			error = workResult != null ? true : error;
		}

		if (error) {
			generateErrorDocument(classifyingAction, workResults, actionedUponNodeRef);
			throw new RuntimeException("Error ocurred while processing. Review previous logs.");
		}
	}

	private NodeRef getParentFolder(NodeRef actionedUponNodeRef) {
		List<ChildAssociationRef> parentAssocs = serviceRegistry.getNodeService().getParentAssocs(actionedUponNodeRef);
		return parentAssocs.get(0).getParentRef();
	}

	private void generateErrorDocument(ClassifyingActionDefinition classifyingAction, Map<NodeRef, String> workResults, NodeRef actionedUponNodeRef) {
		log.debug("Generating error document...");
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd-HHmmss");
		String targetName = (String) serviceRegistry.getNodeService().getProperty(actionedUponNodeRef, ContentModel.PROP_NAME);
		String actionName = classifyingAction.getName();
		String content = generateErrorDocumentContent(workResults, targetName);
		String documentName = actionName + " (error report) - " + sdf.format(new Date()) + ".txt";

		RetryingTransactionHelper txHelper = serviceRegistry.getRetryingTransactionHelper();
		RetryingTransactionCallback<Void> createErrorNodeTransaction = new CreateErrorNodeTransaction(actionedUponNodeRef, documentName, content);
		txHelper.doInTransaction(createErrorNodeTransaction, false, true);
	}

	private String generateErrorDocumentContent(Map<NodeRef, String> workResults, String targetName) {
		StringBuffer content = new StringBuffer();
		content.append("Document or folder: " + targetName + "\n");
		content.append("Results: \n");
		for (Entry<NodeRef, String> workResult : workResults.entrySet()) {
			String documentName = (String) serviceRegistry.getNodeService().getProperty(workResult.getKey(), ContentModel.PROP_NAME);
			String result = workResult.getValue();
			content.append("\t" + documentName + " -> " + (result == null ? "OK" : result) + "\n");
		}
		return content.toString();
	}

	private void updateClassifiersIfNeeded() {
		if (classifiersDocumentNodeRef == null)
			classifiersDocumentNodeRef = properties.getProperty("es.keensoft.alfresco.classifying-action.classifier-document");

		Date modificationDate = getModificationDate();
		if (classifiersDocumentModificationDate == null || !classifiersDocumentModificationDate.equals(modificationDate)) {
			log.debug("Old classifiers values detected. Loading new from document '" + classifiersDocumentNodeRef + "'...");
			classifiersValues = classifiersParser.parseClassifiers(new NodeRef(classifiersDocumentNodeRef));
			classifiersDocumentModificationDate = modificationDate;
		}
	}

	private Date getModificationDate() {
		NodeRef nodeRef = new NodeRef(classifiersDocumentNodeRef);
		Date modifiedDate = (Date) serviceRegistry.getNodeService().getProperty(nodeRef, ContentModel.PROP_MODIFIED);
		return modifiedDate;
	}

	private ClassifyingActionDefinition searchClassifyingAction(Action action) {
		if (configuredActions.isEmpty())
			throw new RuntimeException("There are no actions configured for the module.");
		if (classifiersValues.isEmpty())
			throw new RuntimeException("There are no classifiers values loaded.");

		String configuration = (String) action.getParameterValue(PARAM_CONFIGURATION);
		if (StringUtils.isEmpty(configuration))
			throw new RuntimeException("Configuration parameters received empty.");

		ClassifyingActionDefinition classifyingActionDefinition = configuredActions.get(configuration);
		if (classifyingActionDefinition == null)
			throw new RuntimeException("There is no configured action with key '" + configuration + "'.");

		return classifyingActionDefinition;
	}

	private List<ChildAssociationRef> getChildrenNodes(NodeRef nodeRef) {
		List<ChildAssociationRef> childAssocs = serviceRegistry.getNodeService().getChildAssocs(nodeRef);
		return childAssocs;
	}

	/**
	 * Executes work in new transaction and returns error or null
	 * 
	 * @param classifyingAction
	 *            Classifying action definition
	 * @param nodeRef
	 *            Action will execute upon this NodeRef
	 * @return error message in case of error during execution
	 */
	private String doWork(final ClassifyingActionDefinition classifyingAction, final NodeRef nodeRef) {
		ClassifierInterface classifier = classifyingAction.getClassifier();
		ClassifyingActionInterface action = classifyingAction.getAction();
		Map<String, String> extraParams = classifyingAction.getExtraParams();

		String[] classifierValuesRow;
		try {
			classifierValuesRow = classifier.getValuesRow(nodeRef, classifiersValues, classifyingAction.getClassifierKeyIndex());
		} catch (Exception e) {
			log.error("Error obtaining values row from classifier for node '" + nodeRef + "' with index '': " + classifyingAction.getClassifierKeyIndex());
			return e.getMessage();
		}

		RetryingTransactionHelper txHelper = serviceRegistry.getRetryingTransactionHelper();
		RetryingTransactionCallback<Void> cb = new WorkTransaction(classifierValuesRow, extraParams, action, classifyingAction, nodeRef);

		try {
			txHelper.doInTransaction(cb, false, true);
		} catch (Exception e) {
			log.error("Error executing action '" + classifyingAction.getName() + "' with node '" + nodeRef + "' and classifier values '" + Arrays.toString(classifierValuesRow) + "': ", e);
			return e.getMessage();
		}

		return null;
	}

	private boolean isFolder(NodeRef nodeRef) {
		FileInfo fileInfo = serviceRegistry.getFileFolderService().getFileInfo(nodeRef);
		return fileInfo.isFolder();
	}

	@Override
	protected void addParameterDefinitions(List<ParameterDefinition> paramList) {
		paramList.add(new ParameterDefinitionImpl(PARAM_CONFIGURATION, DataTypeDefinition.TEXT, true, getParamDisplayLabel(PARAM_CONFIGURATION)));
	}

	public void setServiceRegistry(ServiceRegistry serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
	}

	public void setProperties(Properties properties) {
		this.properties = properties;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

	public void setClassifiersParser(ClassifiersParserInterface classifiersParser) {
		this.classifiersParser = classifiersParser;
	}

	private final class WorkTransaction implements RetryingTransactionCallback<Void> {
		private final String[] classifierValuesRow;
		private final Map<String, String> extraParams;
		private final ClassifyingActionInterface action;
		private final ClassifyingActionDefinition classifyingAction;
		private final NodeRef nodeRef;

		private WorkTransaction(String[] classifierValuesRow, Map<String, String> extraParams, ClassifyingActionInterface action, ClassifyingActionDefinition classifyingAction, NodeRef nodeRef) {
			this.classifierValuesRow = classifierValuesRow;
			this.extraParams = extraParams;
			this.action = action;
			this.classifyingAction = classifyingAction;
			this.nodeRef = nodeRef;
		}

		public Void execute() throws Throwable {
			action.execute(nodeRef, classifierValuesRow, extraParams);

			Map<String, Serializable> versionProperties = new HashMap<String, Serializable>();
			versionProperties.put(Version.PROP_DESCRIPTION, classifyingAction.getName() + " (classifying)");
			versionProperties.put(VersionModel.PROP_VERSION_TYPE, VersionType.MINOR);
			serviceRegistry.getVersionService().createVersion(nodeRef, versionProperties);
			return null;
		}
	}

	private final class CreateErrorNodeTransaction implements RetryingTransactionCallback<Void> {
		private NodeRef actionedUponNodeRef;
		private String documentName;
		private String content;

		public CreateErrorNodeTransaction(NodeRef actionedUponNodeRef, String documentName, String content) {
			super();
			this.actionedUponNodeRef = actionedUponNodeRef;
			this.documentName = documentName;
			this.content = content;
		}

		public Void execute() throws Throwable {
			NodeRef resultDocumentFolder = isFolder(actionedUponNodeRef) ? actionedUponNodeRef : getParentFolder(actionedUponNodeRef);
			Map<QName, Serializable> properties = new HashMap<QName, Serializable>();
			properties.put(ContentModel.PROP_NAME, documentName);
			ChildAssociationRef childAssocRef = serviceRegistry.getNodeService().createNode(resultDocumentFolder, ContentModel.ASSOC_CONTAINS, QName.createQName(documentName), ContentModel.TYPE_CONTENT, properties);
			ContentWriter writer = serviceRegistry.getContentService().getWriter(childAssocRef.getChildRef(), ContentModel.PROP_CONTENT, true);
			writer.setMimetype("text/plain");
			writer.putContent(content);
			
			return null;
		}
	}
}
