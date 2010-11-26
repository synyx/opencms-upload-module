
package org.synyx.opencms.deployment;

/**
 *
 * @author Florian Hopf, Synyx GmbH & Co. KG, hopf@synyx.de
 */
public class UploadInfo {

    private String fileName;
    private int fileType;
    private String path;
    private boolean publish;
    private String username;
    private String password;
    private byte [] contents;

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public int getFileType() {
        return fileType;
    }

    public void setFileType(int fileType) {
        this.fileType = fileType;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public boolean isPublish() {
        return publish;
    }

    public void setPublish(boolean publish) {
        this.publish = publish;
    }

    public byte[] getContents() {
        return contents;
    }

    public void setContents(byte[] contents) {
        this.contents = contents;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getResourcePath() {
        if (path.endsWith("/")) {
            return (new StringBuilder(path)).append(fileName).toString();
        } else {
            return (new StringBuilder(path)).append("/").append(fileName).toString();
        }
    }
    

}
