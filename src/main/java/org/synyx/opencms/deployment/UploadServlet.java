// Decompiled by Jad v1.5.8e. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.geocities.com/kpdus/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   UploadServlet.java

package org.synyx.opencms.deployment;

import java.io.*;
import java.util.Iterator;
import java.util.List;
import javax.servlet.http.*;
import org.apache.commons.fileupload.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.opencms.db.CmsUserSettings;
import org.opencms.file.*;
import org.opencms.file.types.*;
import org.opencms.main.*;

public class UploadServlet extends HttpServlet
{

    public UploadServlet()
    {
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
    {
        log = LogFactory.getLog(getClass().getName());
        List properties = null;
        String fileName = null;
        String fileType = null;
        String user = null;
        String password = null;
        String path = null;
        String dir = null;
        String isDir = "";
        String resource = "";
        String resourceFolder = "";
        String redir = null;
        String exceptionDetail = null;
        DefaultFileItemFactory fac = new DefaultFileItemFactory();
        fac.setSizeThreshold(0x186a0);
        fac.setRepository(new File(System.getProperty("java.io.tmpdir")));
        DiskFileUpload fileUpload = new DiskFileUpload(fac);
        List fileItems = getFileItems(fileUpload, request);
        Iterator requestIterator = fileItems.iterator();
        do
        {
            if(!requestIterator.hasNext())
                break;
            FileItem fieldIterator = (FileItem)requestIterator.next();
            if(fieldIterator.getFieldName().equals("openCmsPath"))
                path = fieldIterator.getString();
            if(fieldIterator.getFieldName().equals("publish"))
                publish = fieldIterator.getString();
            if(fieldIterator.getFieldName().equals("fileType"))
                fileType = fieldIterator.getString();
            if(fieldIterator.getFieldName().equals("password"))
                password = fieldIterator.getString();
            if(fieldIterator.getFieldName().equals("username"))
                user = fieldIterator.getString();
            if(fieldIterator.getFieldName().equals("cmsDirectory"))
                dir = fieldIterator.getString();
            if(fieldIterator.getFieldName().equals("isDirectory"))
                isDir = fieldIterator.getString();
            if(fieldIterator.getFieldName().equals("redir"))
                redir = fieldIterator.getString();
        } while(true);
        if(getInitParameter("opencmsUsername") != null && getInitParameter("opencmsPassword") != null)
        {
            user = getInitParameter("opencmsUsername");
            password = getInitParameter("opencmsPassword");
        }
        int type = CmsResourceTypePlain.getStaticTypeId();
        type = resolveCmsFileType(fileType, type);
        CmsObject cmsObject = null;
        try
        {
            cmsObject = OpenCms.initCmsObject("Guest");
        }
        catch(CmsException ex)
        {
            log.error(ex);
        }
        try
        {
            cmsObject.loginUser(user, password);
        }
        catch(CmsException ex)
        {
            exceptionDetail = handleException(ex);
        }
        CmsUserSettings settings = new CmsUserSettings(cmsObject);
        Iterator fileIterator = fileItems.iterator();
        do
        {
            if(!fileIterator.hasNext())
                break;
            FileItem fileItem = (FileItem)fileIterator.next();
            if(!fileItem.isFormField())
            {
                log.debug((new StringBuilder()).append("Upload file \nNAME: ").append(fileItem.getName()).append(" SIZE: ").append(fileItem.getSize()).toString());
                fileName = fileItem.getName();
                try
                {
                    if(isDir.equalsIgnoreCase("false"))
                    {
                        byte content[] = read(fileItem);
                        if(path.endsWith("/"))
                        {
                            if(dir.equalsIgnoreCase("none"))
                                resource = (new StringBuilder()).append(path).append(fileName).toString();
                            else
                                resource = (new StringBuilder()).append(path).append(dir).append(fileName).toString();
                        } else
                        if(dir.equalsIgnoreCase("none"))
                            resource = (new StringBuilder()).append(path).append("/").append(fileName).toString();
                        else
                            resource = (new StringBuilder()).append(path).append("/").append(dir).append(fileName).toString();
                        path = (new StringBuilder()).append(settings.getStartSite()).append(path).toString();
                        CmsRequestContext requestContext = cmsObject.getRequestContext();
                        CmsProject cmsProject = requestContext.currentProject();
                        String projectName = cmsProject.getName();
                        log.info((new StringBuilder()).append("Current project is").append(projectName).toString());
                        requestContext.setCurrentProject(cmsObject.readProject("Offline"));
                        if(cmsObject.existsResource(resource))
                        {
                            cmsObject.lockResource(resource);
                            try
                            {
                                cmsObject.replaceResource(resource, type, content, properties);
                            }
                            catch(Exception ex)
                            {
                                handleException(ex);
                            }
                        } else
                        {
                            try
                            {
                                cmsObject.createResource(resource, type, content, properties);
                            }
                            catch(CmsIllegalArgumentException ex)
                            {
                                exceptionDetail = handleException(ex);
                            }
                            catch(CmsException ex)
                            {
                                exceptionDetail = handleException(ex);
                            }
                        }
                        if(publish.equals("true"))
                        {
                            cmsObject.unlockResource(resource);
                            if(cmsObject.hasPublishPermissions(resource))
                                cmsObject.publishResource(resource);
                            else
                                log.info("User has no permissions to publish.");
                        }
                        log.info("Fileupload was successful");
                        cmsProject = cmsObject.readProject(projectName);
                        requestContext.setCurrentProject(cmsProject);
                    } else
                    {
                        resourceFolder = (new StringBuilder()).append(path).append("/").append(dir).toString();
                        if(!cmsObject.existsResource(resourceFolder))
                        {
                            log.info((new StringBuilder()).append("Folder ").append(path).append(dir).append(" does not exist").toString());
                            int typeFolder = CmsResourceTypeFolder.getStaticTypeId();
                            cmsObject.createResource(resourceFolder, typeFolder, null, properties);
                            cmsObject.unlockResource(resourceFolder);
                            if(cmsObject.hasPublishPermissions(resourceFolder))
                                cmsObject.publishResource(resourceFolder);
                            else
                                log.info("User has no permissions to publish.");
                        }
                    }
                }
                catch(Exception ex)
                {
                    exceptionDetail = handleException(ex);
                }
            }
        } while(true);
        sendResponse(resourceFolder, fileName, isDir, result, publish, response, exceptionDetail, resource, redir);
    }

    protected List getFileItems(DiskFileUpload fileUpload, HttpServletRequest request)
    {
        List fileItems = null;
        try
        {
            fileItems = fileUpload.parseRequest(request);
        }
        catch(FileUploadException ex)
        {
            handleException(ex);
        }
        return fileItems;
    }

    protected int resolveCmsFileType(String fileType, int type)
    {
        if(fileType.equalsIgnoreCase("jsp"))
            type = CmsResourceTypeJsp.getStaticTypeId();
        if(fileType.equalsIgnoreCase("img"))
            type = CmsResourceTypeImage.getStaticTypeId();
        if(fileType.equalsIgnoreCase("bin"))
            type = CmsResourceTypeBinary.getStaticTypeId();
        if(fileType.equalsIgnoreCase("xml"))
            type = CmsResourceTypeXmlPage.getStaticTypeId();
        if(fileType.equalsIgnoreCase("folder"))
            type = CmsResourceTypeFolder.getStaticTypeId();
        if(fileType.equalsIgnoreCase("plain"))
            type = CmsResourceTypePlain.getStaticTypeId();
        return type;
    }

    protected String handleException(Exception ex)
    {
        result = "incorrect";
        log.error("Caught Exception", ex);
        return (new StringBuilder()).append(ex).append(ex.getMessage()).toString();
    }

    protected void sendResponse(String resourceFolder, String fileName, String isDir, String result, String publish, HttpServletResponse response, String exceptionDetail, 
            String resource, String redir)
    {
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = null;
        if(exceptionDetail == null && redir != null)
        {
            try
            {
                response.sendRedirect(redir);
            }
            catch(IOException ex)
            {
                log.error("Couldn't redirect", ex);
            }
        } else
        {
            try
            {
                out = response.getWriter();
            }
            catch(IOException ex)
            {
                log.error(ex);
            }
            if(publish.equals("true") && isDir.equalsIgnoreCase("false"))
                out.println((new StringBuilder()).append("File ").append(fileName).append(" was in ").append(resource.substring(0, resource.indexOf(fileName) - 1)).append(" ").append(result).append(" uploaded and published.").toString());
            else
                out.println((new StringBuilder()).append("File ").append(fileName).append(" was in ").append(resource.substring(0, resource.indexOf(fileName) - 1)).append(" ").append(result).append(" uploaded.").toString());
            if(isDir.equalsIgnoreCase("true"))
            {
                resourceFolder = resourceFolder.substring(0, resourceFolder.lastIndexOf("/") - 1);
                out.println((new StringBuilder()).append("  Directory ").append(resourceFolder.substring(resourceFolder.lastIndexOf("/") + 1, resourceFolder.length() - 1)).append(" was in ").append(resourceFolder.substring(0, resourceFolder.lastIndexOf("/") - 1)).append(" ").append(result).append(" uploaded and published.").toString());
            }
            out.println(exceptionDetail);
            out.close();
        }
    }

    private byte[] read(FileItem fileItem)
        throws Exception
    {
        InputStream in = fileItem.getInputStream();
        byte data[] = new byte[in.available()];
        in.read(data);
        return data;
    }

    private static final int sizeThreshold = 0x186a0;
    private static Log log = null;
    private final String CONTENT_TYPE = "text/html;charset=UTF-8";
    private final String INCORRECT = "incorrect";
    private static String result = "successfully";
    private static String publish = "false";
    private static final String CMS_XML = "xml";
    private static final String CMS_BIN = "bin";
    private static final String CMS_IMG = "img";
    private static final String CMS_JSP = "jsp";
    private static final String NONE = "none";
    private static final String CMS_PLAIN = "plain";
    private static final String CMS_FOLDER = "folder";
    private static final String OFFLINE_PROJECT = "Offline";
    private static final String IS_DIRECTORY = "isDirectory";
    private static final String CMS_DIRECTORY = "cmsDirectory";
    private static final String USERNAME = "username";
    private static final String PASSWORD = "password";
    private static final String FILETYPE = "fileType";
    private static final String PUBLISH = "publish";
    private static final String OPENCMS_PATH = "openCmsPath";
    private static final String opencmsPassword = "opencmsPassword";
    private static final String opencmsUsername = "opencmsUsername";
    private static final String REDIRECT_PATH = "redir";
    private static final String JAVA_IO_TMPDIR = "java.io.tmpdir";

}
