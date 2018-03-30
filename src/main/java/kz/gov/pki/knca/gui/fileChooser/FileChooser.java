package kz.gov.pki.knca.gui.fileChooser;

import kz.gov.pki.knca.gui.ProgramSettings;

import javax.swing.*;

/**
 *
 * @author Zhanbolat.Seitkulov
 */
public final class FileChooser extends JFileChooser {
    
    private FileType currentFileType;
    private FileChooserFilter keyFilter = new FileChooserFilter();
    
    /**
     * Конструктор класса <code>FileChooser</code>
     *
     * @param fileType тип файла для выбора
     */
    public FileChooser(FileChooser.FileType fileType) {
        setFileType(fileType);
        setFileFilter(keyFilter);
        if (String.valueOf(fileType).equalsIgnoreCase("ALL")) {
            removeChoosableFileFilter(getFileFilter());
        }
        setAcceptAllFileFilterUsed(false);
        setMultiSelectionEnabled(false);
        updateLanguage();
    }

    /**
     * Конструктор класса <code>FileChooser</code>
     *
     * @param fileType тип файла для выбора
     */
    public FileChooser(String fileType) {
        if (fileType == null || fileType.isEmpty()) {
            setFileType(FileChooser.FileType.DIRECTORY);
        } else {
            setFileType(FileType.valueOf(fileType));
        }
        setFileFilter(keyFilter);
        if (fileType.equalsIgnoreCase("ALL")) {
            removeChoosableFileFilter(getFileFilter());
        }
        setAcceptAllFileFilterUsed(false);
        setMultiSelectionEnabled(false);
        updateLanguage();
    }

    /**
     * Устанавливет тип файла для отоброжения файлов в <code>FileChooser</code>
     *
     * @param fileType тип файла для выбора
     */
    public void setFileType(FileChooser.FileType fileType) {
        currentFileType = fileType;
        keyFilter.clearExtension();

        switch (fileType) {
            case DIRECTORY:
                keyFilter.addExtension("");
                keyFilter.setDescription(ProgramSettings.getInstance().getDictionary("fileChooser.description.directory"));
                setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                break;
            case CER:
                keyFilter.addExtension("der");
                keyFilter.addExtension("cer");
                keyFilter.setDescription(ProgramSettings.getInstance().getDictionary("fileChooser.description.cer"));
                setFileSelectionMode(JFileChooser.FILES_ONLY);
                break;
            case CRL:
                keyFilter.addExtension("crl");
                keyFilter.setDescription(ProgramSettings.getInstance().getDictionary("fileChooser.description.crl"));
                setFileSelectionMode(JFileChooser.FILES_ONLY);
                break;
            case P12:
                keyFilter.addExtension("p12");
                keyFilter.setDescription(ProgramSettings.getInstance().getDictionary("fileChooser.description.key"));
                setFileSelectionMode(JFileChooser.FILES_ONLY);
                break;
            case JKS:
                keyFilter.addExtension("jks");
                keyFilter.setDescription(ProgramSettings.getInstance().getDictionary("fileChooser.description.key"));
                setFileSelectionMode(JFileChooser.FILES_ONLY);
                break;
            case KEYSTORE:
                keyFilter.addExtension("pfx");
                keyFilter.addExtension("p12");
                keyFilter.addExtension("bin");
                keyFilter.addExtension("jks");
                keyFilter.setDescription(ProgramSettings.getInstance().getDictionary("fileChooser.description.key"));
                setFileSelectionMode(JFileChooser.FILES_ONLY);
                break;   
            case ALL: 
                keyFilter.addExtension("*");
                keyFilter.setDescription(ProgramSettings.getInstance().getDictionary("fileChooser.description.file"));
                setFileSelectionMode(JFileChooser.FILES_ONLY);
                break;
        }
    }
    
    /**
     * Метод для обновления языка интерфейса <code>FileChooser</code>
     *
     */
    public void updateLanguage() {
        switch (currentFileType) {
            case DIRECTORY:
                setDialogTitle(" " + ProgramSettings.getInstance().getDictionary("fileChooser.title.directory"));
                break;
            default:
                setDialogTitle(" " + ProgramSettings.getInstance().getDictionary("fileChooser.title.file"));
        }
        
        UIManager.put("FileChooser.openButtonText", ProgramSettings.getInstance().getDictionary("fileChooser.openButtonText"));
        UIManager.put("FileChooser.saveButtonText", ProgramSettings.getInstance().getDictionary("fileChooser.saveButtonText"));
        UIManager.put("FileChooser.cancelButtonText", ProgramSettings.getInstance().getDictionary("fileChooser.cancelButtonText"));
        UIManager.put("FileChooser.openButtonToolTipText", ProgramSettings.getInstance().getDictionary("fileChooser.openButtonToolTipText"));
        UIManager.put("FileChooser.saveButtonToolTipText", ProgramSettings.getInstance().getDictionary("fileChooser.saveButtonToolTipText"));
        UIManager.put("FileChooser.cancelButtonToolTipText", ProgramSettings.getInstance().getDictionary("fileChooser.cancelButtonToolTipText"));
        UIManager.put("FileChooser.lookInLabelText", ProgramSettings.getInstance().getDictionary("fileChooser.lookInLabelText"));
        UIManager.put("FileChooser.saveInLabelText", ProgramSettings.getInstance().getDictionary("fileChooser.saveInLabelText"));
        UIManager.put("FileChooser.fileNameLabelText", ProgramSettings.getInstance().getDictionary("fileChooser.fileNameLabelText"));
        UIManager.put("FileChooser.filesOfTypeLabelText", ProgramSettings.getInstance().getDictionary("fileChooser.filesOfTypeLabelText"));
        UIManager.put("FileChooser.upFolderToolTipText", ProgramSettings.getInstance().getDictionary("fileChooser.upFolderToolTipText"));
        UIManager.put("FileChooser.homeFolderToolTipText", ProgramSettings.getInstance().getDictionary("fileChooser.homeFolderToolTipText"));
        UIManager.put("FileChooser.newFolderToolTipText", ProgramSettings.getInstance().getDictionary("fileChooser.newFolderToolTipText"));
        UIManager.put("FileChooser.listViewButtonToolTipText", ProgramSettings.getInstance().getDictionary("fileChooser.listViewButtonToolTipText"));
        UIManager.put("FileChooser.detailsViewButtonToolTipText", ProgramSettings.getInstance().getDictionary("fileChooser.detailsViewButtonToolTipText"));
        UIManager.put("FileChooser.directoryOpenButtonText", ProgramSettings.getInstance().getDictionary("fileChooser.directoryOpenButtonText"));
        UIManager.put("FileChooser.directoryOpenButtonToolTipText", ProgramSettings.getInstance().getDictionary("fileChooser.directoryOpenButtonToolTipText"));
        updateDescription();
        updateUI();
    }
    
    /**
     * Метод для обновления описания типов файла в интерфейсе
     *
     */
    public void updateDescription() {
        switch (currentFileType) {
            case DIRECTORY:
                keyFilter.setDescription(ProgramSettings.getInstance().getDictionary("fileChooser.description.directory"));
                break;
            case CRL:
                keyFilter.setDescription(ProgramSettings.getInstance().getDictionary("fileChooser.description.crl"));
                break;
            case CER:
                keyFilter.setDescription(ProgramSettings.getInstance().getDictionary("fileChooser.description.cer"));
                break;
            case P12:
                keyFilter.setDescription(ProgramSettings.getInstance().getDictionary("fileChooser.description.key"));
                break;
            case JKS:
                keyFilter.setDescription(ProgramSettings.getInstance().getDictionary("fileChooser.description.key"));
                break;
            case KEYSTORE:
                keyFilter.setDescription(ProgramSettings.getInstance().getDictionary("fileChooser.description.key"));
                break;            
            default:
                keyFilter.setDescription(ProgramSettings.getInstance().getDictionary("fileChooser.description.file"));
        }
    }
    
    /**
     * Типы файлов
     *
     */
    public static enum FileType {
        DIRECTORY, CRL, CER, P12, KEYSTORE, JKS, ALL;
    }
}
