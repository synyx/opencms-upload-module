package org.synyx.opencms.deployment;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.opencms.file.types.CmsResourceTypeBinary;
import org.opencms.file.types.CmsResourceTypeFolder;
import org.opencms.file.types.CmsResourceTypeImage;
import org.opencms.file.types.CmsResourceTypeJsp;
import org.opencms.file.types.CmsResourceTypePlain;
import org.opencms.file.types.CmsResourceTypeXmlPage;

/**
 * Simple bean to be used for uploading from a JSP.
 * @author Florian Hopf, Synyx GmbH & Co. KG, hopf@synyx.de
 */
public class UploadBean {

    private Log log = LogFactory.getLog(getClass().getName());
    private static final int SIZE_THRESHOLD = 0x186a0;
    private static final String CONTENT_TYPE = "text/html;charset=UTF-8";
    private static final String CMS_XML = "xml";
    private static final String CMS_BIN = "bin";
    private static final String CMS_IMG = "img";
    private static final String CMS_JSP = "jsp";
    private static final String CMS_PLAIN = "plain";
    private static final String CMS_FOLDER = "folder";
    private static final String USERNAME = "username";
    private static final String PASSWORD = "password";
    private static final String FILETYPE = "fileType";
    private static final String PUBLISH = "publish";
    private static final String OPENCMS_PATH = "openCmsPath";

    public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws IOException {

        String message = "";

        try {
            UploadInfo uploadInfo = extractUploadInfo(request);
            CmsUpload upload = new CmsUpload();
            upload.upload(uploadInfo);
            if (uploadInfo.isPublish()) {
                message = String.format("File %s was successfully uploaded and published.", uploadInfo.getResourcePath());
            } else {
                message = String.format("File %s was successfully uploaded.", uploadInfo.getResourcePath());
            }
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
            message = ex.getMessage();
        }

        sendResponse(response, message);
    }

    protected int resolveCmsFileType(String fileType) {
        if (fileType.equalsIgnoreCase(CMS_JSP)) {
            return CmsResourceTypeJsp.getStaticTypeId();
        }
        if (fileType.equalsIgnoreCase(CMS_IMG)) {
            return CmsResourceTypeImage.getStaticTypeId();
        }
        if (fileType.equalsIgnoreCase(CMS_BIN)) {
            return CmsResourceTypeBinary.getStaticTypeId();
        }
        if (fileType.equalsIgnoreCase(CMS_XML)) {
            return CmsResourceTypeXmlPage.getStaticTypeId();
        }
        if (fileType.equalsIgnoreCase(CMS_FOLDER)) {
            return CmsResourceTypeFolder.getStaticTypeId();
        }
        if (fileType.equalsIgnoreCase(CMS_PLAIN)) {
            return CmsResourceTypePlain.getStaticTypeId();
        }
        return CmsResourceTypePlain.getStaticTypeId();
    }

    protected void sendResponse(HttpServletResponse response, String message) throws IOException {
        response.setContentType(CONTENT_TYPE);
        PrintWriter out = null;
        out = response.getWriter();
        out.println(message);
        out.close();
    }

    private byte[] read(FileItem fileItem) throws IOException {
        InputStream in = fileItem.getInputStream();
        byte data[] = new byte[in.available()];
        in.read(data);
        return data;
    }

    private UploadInfo extractUploadInfo(HttpServletRequest request) throws IOException, FileUploadException {
        UploadInfo uploadInfo = new UploadInfo();

        DiskFileItemFactory fac = new DiskFileItemFactory();
        fac.setSizeThreshold(SIZE_THRESHOLD);
        fac.setRepository(new File(System.getProperty("java.io.tmpdir")));
        ServletFileUpload fileUpload = new ServletFileUpload(fac);
        List fileItems = fileUpload.parseRequest(request);
        Iterator fileItemIterator = fileItems.iterator();
        while (fileItemIterator.hasNext()) {
            FileItem item = (FileItem) fileItemIterator.next();
            if (item.getFieldName().equals(OPENCMS_PATH)) {
                uploadInfo.setPath(item.getString());
            } else if (item.getFieldName().equals(PUBLISH)) {
                uploadInfo.setPublish(Boolean.valueOf(item.getString()));
            } else if (item.getFieldName().equals(FILETYPE)) {
                uploadInfo.setFileType(resolveCmsFileType(item.getString()));
            } else if (item.getFieldName().equals(PASSWORD)) {
                uploadInfo.setPassword(item.getString());
            } else if (item.getFieldName().equals(USERNAME)) {
                uploadInfo.setUsername(item.getString());
            } else if (!item.isFormField()) {
                log.debug(String.format("Upload file \nNAME: %s SIZE: %d", item.getName(), item.getSize()));
                uploadInfo.setFileName(item.getName());
                uploadInfo.setContents(read(item));
            }
        }
        return uploadInfo;
    }
}
