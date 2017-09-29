package es.keensoft.alfresco.ca;

import java.util.Map;

import es.keensoft.alfresco.ca.actions.ClassifyingActionInterface;
import es.keensoft.alfresco.ca.classifiers.ClassifierInterface;

public class ClassifyingActionDefinition {
	private String name;

	private int classifierKeyIndex;

	private Map<String, String> extraParams;

	private ClassifierInterface classifier;
	private ClassifyingActionInterface action;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getClassifierKeyIndex() {
		return classifierKeyIndex;
	}

	public void setClassifierKeyIndex(int classifierKeyIndex) {
		this.classifierKeyIndex = classifierKeyIndex;
	}

	public ClassifierInterface getClassifier() {
		return classifier;
	}

	public void setClassifier(ClassifierInterface classifier) {
		this.classifier = classifier;
	}

	public ClassifyingActionInterface getAction() {
		return action;
	}

	public void setAction(ClassifyingActionInterface action) {
		this.action = action;
	}

	public Map<String, String> getExtraParams() {
		return extraParams;
	}

	public void setExtraParams(Map<String, String> extraParams) {
		this.extraParams = extraParams;
	}

}
