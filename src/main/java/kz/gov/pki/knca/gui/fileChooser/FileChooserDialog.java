package kz.gov.pki.knca.gui.fileChooser;

import kz.gov.pki.knca.gui.ProgramSettings;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;

/**
 *
 * @author Zhanbolat.Seitkulov
 */
public class FileChooserDialog extends JDialog {

    private String selectedFilePath = null;
    private FileChooser fileChooser = null;

    /**
     * Конструктор класса <code>FileChooserDialog</code>
     *
     * @param owner родительский <code>Frame</code> диалогового окна
     * @param fileExtension тип файлов которые необходимо выбрать
     * @param currentDirectory каталог в котором находятся необходимые файлы
     */
    public FileChooserDialog(final JFrame owner, String fileExtension, String currentDirectory) {
//        super(new JFrame());
        super(owner, ProgramSettings.getInstance().getDictionary("fileChooser.title.file"), true);
        setAlwaysOnTop(true);
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int height = screenSize.height;
        int width = screenSize.width;
        setSize(width / 2, height / 2);
        setResizable(true);
        setLocationByPlatform(true);
        setLocationRelativeTo(null);
        fileChooser = new FileChooser(fileExtension);
        if (currentDirectory != null && !currentDirectory.isEmpty()) {
            fileChooser.setCurrentDirectory(new File(currentDirectory));
        }
        fileChooser.setDialogType(JFileChooser.OPEN_DIALOG);
        add(fileChooser);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent we) {
                dispose();
            }
        });
        pack();
        fileChooser.revalidate();
        fileChooser.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JFileChooser choser = (JFileChooser) e.getSource();
                String str = e.getActionCommand();
                if (str.equals(JFileChooser.APPROVE_SELECTION)) {
                    File selectedFile = choser.getSelectedFile();

                    if (selectedFile.exists()) {
                        setSelectedFilePath(selectedFile.getPath());
                    }
                } else if (str.equals(JFileChooser.CANCEL_SELECTION)) {
                    setSelectedFilePath(null);
                }
                setVisible(false);
                owner.dispose();
                dispose();
            }
        });
        setVisible(true);
    }
    /**
     * Устанавливает полный путь выбранного файла
     *
     * @param selectedFilePath путь выбранного файла
     */
    public void setSelectedFilePath(String selectedFilePath) {
        this.selectedFilePath = selectedFilePath;
    }

    /**
     * Возвращает полный путь выбранного файла
     *
     * @return путь выбранного файла
     */
    public String getSelectedFilePath() {
        return selectedFilePath;
    }
}
