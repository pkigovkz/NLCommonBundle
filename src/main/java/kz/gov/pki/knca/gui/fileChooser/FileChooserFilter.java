package kz.gov.pki.knca.gui.fileChooser;

import javax.swing.filechooser.FileFilter;
import java.io.File;
import java.util.Enumeration;
import java.util.Hashtable;

/**
 *
 * @author Zhanbolat.Seitkulov
 */
public class FileChooserFilter extends FileFilter {
    
    private Hashtable<Object,Object> filters = null;
    private String description = null;
    private String fullDescription = null;
    private boolean useExtensionsInDescription = true;

    /**
     * Return <code>true</code> if this file should be shown in the directory pane,
     * <code>false</code> if it shouldn't.
     *
     * Files that begin with "." are ignored.
     *
     * @param f
     * @return 
     * @see #getExtension
     * @see FileFilter accepts
     */
    public boolean accept(File f) {
        String extension = null;
        if (f != null) {
            if (f.isDirectory()) {
                return true;
            }
            extension = getExtension(f);
            if (extension != null && filters.get(getExtension(f)) != null) {
                return true;
            }
        }    
        return false;
    }

    /**
     * Returns the human readable description of this filter. For
     * example: "JPEG and GIF Image Files (*.jpg, *.gif)"
     *
     * @return 
     * @see #setDescription
     * @see #setExtensionListInDescription
     * @see #isExtensionListInDescription
     * @see FileFilter#getDescription
     */
    public String getDescription() {
        if (fullDescription == null) {
            if (description == null || isExtensionListInDescription()) {
                fullDescription = description == null ? "(" : description + " (";
                
                // build the description from the extension list
                Enumeration extensions = filters.keys();
                
                if (extensions != null) {
                    fullDescription += "*." + (String) extensions.nextElement();
                    while (extensions.hasMoreElements()) {
                        fullDescription += ", ." + (String) extensions.nextElement();
                    }
                }
                fullDescription += ")";
            } else {
                fullDescription = description;
            }
        }        
        return fullDescription;
    }
    
    /**
     * Sets the human readable description of this filter. For
     * example: filter.setDescription("Gif and JPG Images");
     *
     * @param description
     * @see #setDescription
     * @see #setExtensionListInDescription
     * @see #isExtensionListInDescription
     */
    public void setDescription(String description) {
        this.description = description;
        fullDescription = null;
    }
    
    /**
     * Adds a filetype "dot" extension to filter against.
     *
     * For example: the following code will create a filter that filters
     * out all files except those that end in ".jpg" and ".tif":
     *
     *   ExampleFileFilter filter = new ExampleFileFilter();
     *   filter.addExtension("jpg");
     *   filter.addExtension("tif");
     *
     * Note that the "." before the extension is not needed and will be ignored.
     * @param extension
     */
    public void addExtension(String extension) {
        if (filters == null) {
            filters = new Hashtable<Object,Object>(5);
        }
        filters.put(extension.toLowerCase(), this);
        fullDescription = null;
    }
    
    /**
     * Clears all filetypes in filter.
     */
    public void clearExtension() {
        filters = new Hashtable<Object,Object>();
        description = null;
        fullDescription = null;
    }
    
    /**
     * Return the extension portion of the file's name .
     *
     * @param f
     * @return 
     * @see #getExtension
     * @see FileFilter#accept
     */
    public String getExtension(File f) {
        if (f != null) {
            String filename = f.getName();
            int i = filename.lastIndexOf('.');
            if (i > 0 && i < filename.length() - 1) {
                return filename.substring(i + 1).toLowerCase();
            }
        }    
        return null;
    }    
    
    /**
     * Returns whether the extension list (.jpg, .gif, etc) should
     * show up in the human readable description.
     *
     * Only relevent if a description was provided in the constructor
     * or using setDescription();
     *
     * @return 
     * @see #getDescription
     * @see #setDescription
     * @see #setExtensionListInDescription
     */
    public boolean isExtensionListInDescription() {
        return useExtensionsInDescription;
    }    
}
