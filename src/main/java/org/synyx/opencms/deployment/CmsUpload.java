package org.synyx.opencms.deployment;

import java.util.Collections;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.opencms.file.CmsObject;
import org.opencms.file.CmsProject;
import org.opencms.file.CmsRequestContext;
import org.opencms.main.CmsException;
import org.opencms.main.OpenCms;

/**
 *
 * @author Florian Hopf, Synyx GmbH & Co. KG, hopf@synyx.de
 */
public class CmsUpload {

    private CmsObject cms;
    private Log log = LogFactory.getLog(CmsUpload.class);

    private void login(String username, String password) {
        try {
            cms = OpenCms.initCmsObject("Guest");
            cms.loginUser(username, password);
        } catch (CmsException ex) {
            throw new IllegalStateException(ex);
        }
    }

    public void upload(UploadInfo uploadInfo) throws CmsException, Exception {

        login(uploadInfo.getUsername(), uploadInfo.getPassword());

        CmsRequestContext requestContext = cms.getRequestContext();

        CmsProject cmsProject = requestContext.currentProject();

        requestContext.setCurrentProject(cms.readProject("Offline"));

        String resource = uploadInfo.getResourcePath();

        if (cms.existsResource(resource)) {
            cms.lockResource(resource);
            cms.replaceResource(resource, uploadInfo.getFileType(), uploadInfo.getContents(), Collections.emptyList());
        } else {
            cms.createResource(resource, uploadInfo.getFileType(), uploadInfo.getContents(), Collections.emptyList());
        }
        if (uploadInfo.isPublish()) {
            cms.unlockResource(resource);
            OpenCms.getPublishManager().publishResource(cms, resource);
        }
        log.info("Fileupload was successful");
        // TODO not really necessary
        requestContext.setCurrentProject(cmsProject);

    }
}
