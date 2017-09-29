package es.keensoft.alfresco.ca.actions;

import java.util.HashMap;
import java.util.Map;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.TemplateService;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

public class SendPasswordByMailAction implements ClassifyingActionInterface {
	private static final String TEMPLATE_PASSWORD = "password";
	private static final String PARAM_TEMPLATE = "template";
	private static final String PARAM_SUBJECT = "subject";
	private JavaMailSender mailSender;
	private TemplateService templateService;

	@Override
	public void execute(NodeRef nodeRef, String[] classifierValuesRow, Map<String, String> extraParams) {
		String email = classifierValuesRow[3];
		String password = classifierValuesRow[2];
		String mailContent = generateMailContent(extraParams.get(PARAM_TEMPLATE), prepareTemplateData(password));
		MimeMessage message = composeMessage(email, extraParams.get(PARAM_SUBJECT), mailContent);
		mailSender.send(message);
	}

	private HashMap<String, String> prepareTemplateData(String password) {
		HashMap<String, String> templateData = new HashMap<String, String>();
		templateData.put(TEMPLATE_PASSWORD, password);
		return templateData;
	}

	private String generateMailContent(String templateNodeRef, Map<String, String> templateData) {
		return templateService.processTemplate(templateNodeRef, templateData);
	}

	private MimeMessage composeMessage(String email, String subject, String mailContent) {
		try {
			MimeMessage message = mailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
			helper.setTo(email);
			helper.setSubject(subject);
			helper.setText(mailContent, true);
			return message;
		} catch (MessagingException e) {
			throw new RuntimeException(e);
		}
	}

	public void setMailSender(JavaMailSender mailSender) {
		this.mailSender = mailSender;
	}

	public void setTemplateService(TemplateService templateService) {
		this.templateService = templateService;
	}

}
