package es.keensoft.alfresco.ca.actions;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.pdfbox.exceptions.COSVisitorException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.BadSecurityHandlerException;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;

public class PdfProtectAction implements ClassifyingActionInterface {
	private static final String OWNER_PASSWORD = "83}Mv&TZ3X\\$ZMcy";
	private static final String APPLICATION_PDF = "application/pdf";
	private final int KEY_LENGTH = 128;

	private ServiceRegistry serviceRegistry;

	@Override
	public void execute(NodeRef nodeRef, String[] classifierValuesRow, Map<String, String> extraParams) {
		String password = classifierValuesRow[2];
		ContentReader reader = serviceRegistry.getContentService().getReader(nodeRef, ContentModel.PROP_CONTENT);
		String documentMimetype = reader.getMimetype();
		if (documentMimetype.equals(APPLICATION_PDF)) {
			InputStream documentIs = reader.getContentInputStream();
			ContentWriter writer = serviceRegistry.getContentService().getWriter(nodeRef, ContentModel.PROP_CONTENT, true);
			OutputStream contentOutputStream = writer.getContentOutputStream();

			try {
				pdfProtect(password, documentIs, contentOutputStream);
			} catch (COSVisitorException | IOException | BadSecurityHandlerException e) {
				throw new RuntimeException("Error protecting '" + nodeRef + "': ", e);
			}
		} else {
			throw new RuntimeException("'" + nodeRef + "' should be '" + APPLICATION_PDF + "' but is '" + documentMimetype + "'.");
		}
	}

	private void pdfProtect(String password, InputStream documentIs, OutputStream resultOs) throws IOException, BadSecurityHandlerException, COSVisitorException {
		PDDocument pdf = PDDocument.load(documentIs);
		pdf.protect(getStandardProtectionPolicy(password));
		pdf.save(resultOs);
		pdf.close();
	}

	private StandardProtectionPolicy getStandardProtectionPolicy(String classifierValue) {
		AccessPermission ap = getAccessPermisssion();
		StandardProtectionPolicy spp = new StandardProtectionPolicy(OWNER_PASSWORD, classifierValue, ap);
		spp.setEncryptionKeyLength(KEY_LENGTH);
		return spp;
	}

	private AccessPermission getAccessPermisssion() {
		AccessPermission ap = new AccessPermission();
		ap.setCanAssembleDocument(false);
		ap.setCanExtractContent(false);
		ap.setCanExtractForAccessibility(false);
		ap.setCanFillInForm(false);
		ap.setCanModify(false);
		ap.setCanModifyAnnotations(false);
		ap.setCanPrint(true);
		ap.setCanPrintDegraded(false);
		return ap;
	}

	public void setServiceRegistry(ServiceRegistry serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
	}

}
