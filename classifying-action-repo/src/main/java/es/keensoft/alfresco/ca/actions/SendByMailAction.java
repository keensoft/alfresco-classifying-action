package es.keensoft.alfresco.ca.actions;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.alfresco.model.ContentModel;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.ContentIOException;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.TemplateService;
import org.apache.commons.io.IOUtils;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

public class SendByMailAction implements ClassifyingActionInterface {
	private static final String PARAM_TEMPLATE = "template";
	private static final String PARAM_SUBJECT = "subject";
	private ServiceRegistry serviceRegistry;
	private JavaMailSender mailSender;
	private TemplateService templateService;

	@Override
	public void execute(NodeRef nodeRef, String[] classifierValuesRow, Map<String, String> extraParams) {
		String email = classifierValuesRow[3];
		String documentName = (String) serviceRegistry.getNodeService().getProperty(nodeRef, ContentModel.PROP_NAME);
		ContentReader reader = serviceRegistry.getContentService().getReader(nodeRef, ContentModel.PROP_CONTENT);

		String mailContent = generateMailContent(extraParams.get(PARAM_TEMPLATE), new HashMap<String, String>());
		MimeMessage message = composeMessage(email, documentName, getDocumentContentAsResource(reader), reader.getMimetype(), extraParams.get(PARAM_SUBJECT), mailContent);
		mailSender.send(message);
	}

	/**
	 * MimeMessageHelper requires InputStream to be readable twice
	 * 
	 * @param reader
	 * @return
	 * @throws ContentIOException
	 * @throws IOException
	 */
	private ByteArrayResource getDocumentContentAsResource(ContentReader reader) {
		try {
			byte[] documentContent = IOUtils.toByteArray(reader.getContentInputStream());
			return new ByteArrayResource(documentContent);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private String generateMailContent(String templateNodeRef, Map<String, String> templateData) {
		return templateService.processTemplate(templateNodeRef, templateData);
	}

	private MimeMessage composeMessage(String classifierValue, String documentName, ByteArrayResource documentContent, String documentMimetype, String subject, String mailContent) {
		try {
			MimeMessage message = mailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");			
			helper.setTo(classifierValue);
			helper.setSubject(subject);
			helper.setText(mailContent, true);
			helper.addAttachment(documentName, documentContent, documentMimetype);
			return message;
		} catch (MessagingException e) {
			throw new RuntimeException(e);
		}
	}

	public void setServiceRegistry(ServiceRegistry serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
	}

	public void setMailSender(JavaMailSender mailSender) {
		this.mailSender = mailSender;
	}

	public void setTemplateService(TemplateService templateService) {
		this.templateService = templateService;
	}

}
