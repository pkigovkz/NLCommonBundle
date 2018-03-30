package kz.gov.pki.knca.gui.dialog;

import kz.gov.pki.knca.gui.GUiConstants;
import kz.gov.pki.knca.gui.ProgramSettings;
import kz.gov.pki.knca.types.ClientException;
import kz.gov.pki.knca.types.StorageInfo;
import kz.gov.pki.provider.exception.ProviderUtilException;
import kz.gov.pki.provider.exception.ProviderUtilExceptionCode;
import kz.gov.pki.provider.utils.KeyStoreUtil;
import kz.gov.pki.reference.KNCACertificateType;
import kz.gov.pki.reference.KeyStoreEntry;
import org.osgi.service.log.LogService;
import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.*;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import static kz.gov.pki.knca.BundleProvider.KALKAN;
import static kz.gov.pki.knca.BundleLog.LOG;

public class SignerDialog extends JDialog {

    protected JPanel contentPanel;
    private JPanel keyInfoPanel, keyListPanel;
    private JLabel mesLabel;
    private JButton showPwdBtn;
    private KeyListEntry selectedItem;
    private StorageInfo storageInfo;
    private KNCACertificateType keyType;
    private JButton refreshKeyListButton, canButton,
            signButton, cancelButton;
    private JComboBox containerComboBox;
    private JPasswordField passwordField;
    private JLabel refreshLoadingJLabel;
    private JLabel signLoadingJLabel;
    private JComboBox keyListComboBox;
    private ImageIcon loadingImage;
    private JLabel keyOwnerText, periodText, serialNumberText, issuerNameText, algNameText;
    private boolean doSign;
    private Map<String, String> titles;
    private final int windowWidth = 660, borderGap = 15,
            eleGap = 5, mesPanelH = 60, headerH = 60;

    /**
     * Создает диалог. Конструктор класса <code>SignerDialog</code>
     *
     * @param sInfo информация о хранилище
     * @param inKeyType переменная для определения использования диалога
     */
    public SignerDialog(StorageInfo sInfo, KNCACertificateType inKeyType, Map<String, String> titles) {
        this.storageInfo = sInfo;
        this.titles = titles;
        this.keyType = inKeyType;

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(initHeader(), BorderLayout.NORTH);
        getContentPane().add(fillContentPane(), BorderLayout.CENTER);
        getContentPane().add(initFooter(), BorderLayout.SOUTH);

        setDialogProperties();
    }

    private JPanel initHeader() {
        String titleKey;
        if(titles.containsKey("header")){
            titleKey = titles.get("header");
        }else if (keyType != null && keyType.equals(KNCACertificateType.AUTHENTICATION)) {
            titleKey = "label.signerDialog.authTitle";
        } else {
            titleKey = "label.signerDialog.signTitle";
        }

        JPanel header = new JPanel();
        header.setPreferredSize(new Dimension(windowWidth, headerH));
        header.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));

        JLabel titleLabel = new JLabel("<html><p>" + ProgramSettings.getInstance().getDictionary(titleKey) + "</p></html>");
        titleLabel.setPreferredSize(new Dimension(windowWidth - 2 * borderGap, headerH - borderGap));
        titleLabel.setForeground(Color.BLACK);
        titleLabel.setFont(new Font(GUiConstants.LABEL_FONT_NAME, Font.BOLD, GUiConstants.LABEL_FONT_SIZE));
        header.add(titleLabel);
        return header;
    }

    private JPanel fillContentPane(){
        contentPanel = new JPanel();
        contentPanel.setPreferredSize(new Dimension(windowWidth, 144));
        contentPanel.setLayout(null);

        //Loaging gif
        contentPanel.add(getRefreshLoadingJLabel());

        //Storage Type label & text
        addStorageTypeForm();

        //Container Type label & text
        addContainerTypeForm();

        //Label for warning/error messages
        JLabel warningText = getWarningText();
        contentPanel.add(warningText);

        KeyListener capsLockListener = getCapsLockListener(warningText);

        //Password label & text
        addPasswordForm(capsLockListener);

        //Show(eye) password button
        contentPanel.add(getShowPwdBtn(capsLockListener));

        contentPanel.add(initKeyListPanel());

        //Refresh button
        addRefreshBtn(capsLockListener);

        //Cancel button
        addCancelBtn(capsLockListener);

        return contentPanel;
    }

    private void addRefreshBtn(KeyListener capsLockListener) {
        refreshKeyListButton = addButton("button.signerDialog.refreshKeyList", GUiConstants.BUTTON_BACKGROUND_COLOR,
                new Rectangle(226, 100, 207, 35), contentPanel);
        refreshKeyListButton.addActionListener((a)->{
            if (passwordField.getPassword() != null && passwordField.getPassword().length != 0) {
                blockRefreshPart();
                ExecutorService executorService = Executors.newSingleThreadExecutor();
                executorService.execute(()->{
                    try {
                        storageInfo.setPassword(passwordField.getPassword());
                        List<KeyListEntry> keys = refreshKeyList();
                        if (keys == null || keys.isEmpty()) {
                            if (keyType != null && keyType.equals(KNCACertificateType.AUTHENTICATION)) {
                                setMessage(ProgramSettings.getInstance().getDictionary("label.errorMessage.emptyStorage." + "auth"));
                            } else if (keyType != null && keyType.equals(KNCACertificateType.SIGNATURE)) {
                                setMessage(ProgramSettings.getInstance().getDictionary("label.errorMessage.emptyStorage." + "sign"));
                            } else {
                                setMessage(ProgramSettings.getInstance().getDictionary("label.errorMessage.emptyStorage"));
                            }
                            keyListPanel.setVisible(false);
                            contentPanel.setPreferredSize(new Dimension(contentPanel.getPreferredSize().width, 144));
                            unblockRefreshPart();
                            pack();
                        } else {
                            contentPanel.setPreferredSize(new Dimension(contentPanel.getPreferredSize().width, 401));
                            updateKeyList(keys);
                            pack();
                            refreshLoadingJLabel.setVisible(false);
                            keyListPanel.setVisible(true);
                            getRootPane().setDefaultButton(signButton);
                        }
                    }catch (Exception e){
                        LOG.log(LogService.LOG_ERROR, e.getMessage(), e);
                        unblockRefreshPart();
                        setMessage(e.getMessage());
                    }
                });
                executorService.shutdown();
            } else {
                setMessage(ProgramSettings.getInstance().getDictionary("label.errorMessage.emptyField"));
            }
        });
        refreshKeyListButton.addKeyListener(capsLockListener);
        getRootPane().setDefaultButton(refreshKeyListButton);
    }

    private void addCancelBtn(KeyListener capsLockListener) {
        canButton = addButton("button.cancel", GUiConstants.BUTTON_BACKGROUND_COLOR, new Rectangle(438, 100, 207, 35), contentPanel);
        canButton.addActionListener((a)->{
            setVisible(false);
            dispose();
        });
        canButton.addKeyListener(capsLockListener);
    }

    private JButton getShowPwdBtn(KeyListener capsLockListener) {
        showPwdBtn = new JButton(new ImageIcon(SignerDialog.class.getClassLoader().getResource("eye_16.png")));
        showPwdBtn.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                if(showPwdBtn.isEnabled()) {
                    passwordField.setEchoChar((char) 0);
                }
            }

            public void mouseReleased(MouseEvent e) {
                if(showPwdBtn.isEnabled()) {
                    passwordField.setEchoChar('*');
                    passwordField.requestFocusInWindow();
                }
            }
        });
        showPwdBtn.setForeground(GUiConstants.BUTTON_FOREGROUND_COLOR);
        showPwdBtn.setFont(new Font(GUiConstants.LABEL_FONT_NAME, 0, GUiConstants.LABEL_FONT_SIZE));
        showPwdBtn.setContentAreaFilled(false);
        showPwdBtn.setOpaque(true);
        showPwdBtn.setBackground(GUiConstants.BUTTON_BACKGROUND_COLOR);
        showPwdBtn.setBounds(600, 59, 45, 20);
        showPwdBtn.addKeyListener(capsLockListener);
        return showPwdBtn;
    }

    private KeyListener getCapsLockListener(JLabel warningText){
        return new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {
            }

            @Override
            public void keyPressed(KeyEvent e) {
                try {
                    warningText.setText("");
                    if (getToolkit().getLockingKeyState(KeyEvent.VK_CAPS_LOCK)) {
                        warningText.setText(ProgramSettings.getInstance().getDictionary("label.warningMessage.capsLock"));
                    }
                    if ((e.getKeyCode() < KeyEvent.VK_0 || e.getKeyCode() > KeyEvent.VK_9) && !getInputContext().getLocale().getLanguage().equals("en")) {
                        if (!warningText.getText().equals("")) {
                            warningText.setText(warningText.getText() + "; ");
                        }
                        warningText.setText(warningText.getText() + ProgramSettings.getInstance().getDictionary("label.warningMessage.language"));
                    }
                } catch (Exception ex) {
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
            }
        };
    }

    private JLabel getWarningText() {
        JLabel warningText = new JLabel("");
        warningText.setFont(new Font(GUiConstants.LABEL_FONT_NAME, Font.PLAIN, GUiConstants.SIGNER_DIALOG_FONT_SIZE));
        warningText.setForeground(Color.RED);
        warningText.setBounds(280, 80, 350, 14);
        return warningText;
    }

    private void addPasswordForm(KeyListener capsLockListener) {
        addLabel("label.enterPass", GUiConstants.LABEL_FOREGROUND_COLOR,
                new Rectangle(borderGap, 61, 250, 14), contentPanel);

        passwordField = new JPasswordField();
        passwordField.enableInputMethods(true);
        passwordField.setFont(new Font(GUiConstants.LABEL_FONT_NAME, Font.PLAIN, GUiConstants.SIGNER_DIALOG_FONT_SIZE));
        passwordField.setBounds(280, 59, 320, 20);
        contentPanel.add(passwordField);
        passwordField.setColumns(borderGap);
        passwordField.requestFocusInWindow();

        passwordField.addKeyListener(capsLockListener);
    }

    private void addContainerTypeForm() {
        addLabel("label.signerDialog.containerType", GUiConstants.LABEL_FOREGROUND_COLOR,
                new Rectangle(borderGap, 31, 250, 20), contentPanel);
        if (storageInfo.getStorage().isToken() && storageInfo.getContainers().size() > 1) {
            contentPanel.add(getContainerComboBox());
        } else {
            JLabel containerTypeText = new JLabel(storageInfo.getContainer());
            containerTypeText.setFont(new Font(GUiConstants.LABEL_FONT_NAME, Font.PLAIN, GUiConstants.SIGNER_DIALOG_FONT_SIZE));
            containerTypeText.setBounds(280, 31, 350, 20);
            contentPanel.add(containerTypeText);
        }
    }

    private JComboBox getContainerComboBox() {
        containerComboBox = new JComboBox();
        containerComboBox.setBounds(280, 31, 320, 20);
        for (String container : storageInfo.getContainers()) {
            containerComboBox.addItem(container);
        }
        containerComboBox.addItemListener((ItemEvent e) -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                storageInfo.setContainer((String) containerComboBox.getSelectedItem());
            }
        });
        return containerComboBox;
    }

    private void addStorageTypeForm(){
        addLabel("label.signerDialog.storageType", GUiConstants.LABEL_FOREGROUND_COLOR,
                new Rectangle(borderGap, 11, 250, 14), contentPanel);

        JLabel storageTypeText = new JLabel(ProgramSettings.getInstance().getDictionary("label.signerDialog.storage." + storageInfo.getStorage().getName()));
        storageTypeText.setFont(new Font(GUiConstants.LABEL_FONT_NAME, Font.PLAIN, GUiConstants.SIGNER_DIALOG_FONT_SIZE));
        storageTypeText.setBounds(280, 11, 350, 14);
        contentPanel.add(storageTypeText);
    }

    private void setDialogProperties(){
        setAlwaysOnTop(true);
        setModal(true);
        setTitle(ProgramSettings.getInstance().getDictionary(titles.containsKey("title") ? titles.get("title") : "label.signerDialog"));

        setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent evt) {
                setVisible(false);
                dispose();
            }
        });

        setFrameToScreenCenter();
        setResizable(false);
        pack();
    }

    public boolean getDoSign(){
        return doSign;
    }

    public KeyListEntry getSelectedItem(){
        return selectedItem;
    }

    private List<KeyListEntry> refreshKeyList() throws Exception {
        List<KeyListEntry> keys = null;
        try {
            KeyStore ks = KeyStoreUtil.getKeyStore(storageInfo.getStorage(), storageInfo.getContainer(), storageInfo.getPassword(), KALKAN.getProvider());

            Map<String, KeyStoreEntry> entries = null;
            KNCACertificateType kncaCertificateType;
            if (keyType == null) {
                kncaCertificateType = KNCACertificateType.SIGNATURE;
                entries = kz.gov.pki.provider.utils.KeyStoreUtil.getKeyStoreEntries(ks, storageInfo.getPassword(), kncaCertificateType);
                kncaCertificateType = KNCACertificateType.AUTHENTICATION;
            } else {
                kncaCertificateType = keyType;
            }
            if (entries != null) {
                entries.putAll(kz.gov.pki.provider.utils.KeyStoreUtil.getKeyStoreEntries(ks, storageInfo.getPassword(), kncaCertificateType));
            } else {
                entries = kz.gov.pki.provider.utils.KeyStoreUtil.getKeyStoreEntries(ks, storageInfo.getPassword(), kncaCertificateType);
            }
            if (entries != null && !entries.isEmpty()) {
                keys = new ArrayList();
                for (KeyStoreEntry kseKey : entries.values()) {
                    keys.add(new KeyListEntry(kseKey.getKeyId(), kseKey.getAlgorithm(), kseKey.getX509Certificate()));
                }
                Collections.sort(keys, new KeyListEntryComparator());
            }
        } catch (ProviderUtilException ex) {
            if(ex.getCode().equals(ProviderUtilExceptionCode.WRONG_KEYSTORE_PASSWORD)) {
                if (ex.getTryCount() > -1) {
                    throw new ClientException(ProgramSettings.getInstance().getDictionary("label.errorMessage.incorrectPassWithTryCount") + ex.getTryCount());
                } else {
                    throw new ClientException(ProgramSettings.getInstance().getDictionary("label.errorMessage.incorrectPass"));
                }
            }else if(ex.getCode().equals(ProviderUtilExceptionCode.BLOCKED_KEYSTORE_PASSWORD)) {
                throw new ClientException(ProgramSettings.getInstance().getDictionary("label.errorMessage.blockedPass"));
            }else{
                throw new ClientException(ex.getMessage());
            }
        }
        return keys;
    }

    private JPanel initFooter() {
        JPanel footer = new JPanel();
        footer.setPreferredSize(new Dimension(windowWidth, mesPanelH + borderGap));
        footer.setLayout(null);

        JPanel mesPanel = new JPanel();
        mesPanel.setBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null));
        mesPanel.setBounds(borderGap, 0, windowWidth - borderGap - borderGap, mesPanelH);
        mesPanel.setLayout(null);
        footer.add(mesPanel);
        {
            mesLabel = new JLabel();
            mesLabel.setBounds(eleGap, 0, windowWidth - 2 * borderGap - 2 * eleGap, mesPanelH);
            mesLabel.setForeground(Color.RED);
            mesLabel.setFont(new Font(GUiConstants.LABEL_FONT_NAME, Font.PLAIN, GUiConstants.LABEL_FONT_SIZE));
            mesPanel.add(mesLabel);
        }
        return footer;
    }

    private JPanel initKeyListPanel() {
        String labelKey;
        if (keyType != null && keyType.equals(KNCACertificateType.AUTHENTICATION)) {
            labelKey = "label.signerDialog.chooseKeyAuth";
        } else if (keyType != null && keyType.equals(KNCACertificateType.SIGNATURE)) {
            labelKey = "label.signerDialog.chooseKeySign";
        } else {
            labelKey = "label.signerDialog.chooseKey";
        }
        keyListPanel = new JPanel();
        keyListPanel.setBounds(15, 144, 630, 252);
        keyListPanel.setLayout(null);

        addLabel(labelKey, GUiConstants.LABEL_FOREGROUND_COLOR,
                new Rectangle(0, 11, 224, 14), keyListPanel);

        keyInfoPanel = new JPanel();
        keyInfoPanel.setBorder(new TitledBorder(new LineBorder(new Color(120, 120, 120), 1), ProgramSettings.getInstance().getDictionary("label.signerDialog.keyInfo.title"),
                TitledBorder.LEADING, TitledBorder.TOP, new Font(GUiConstants.LABEL_FONT_NAME, Font.BOLD, GUiConstants.SIGNER_DIALOG_FONT_SIZE), new Color(0, 0, 0)));
        keyInfoPanel.setBounds(0, 62, 630, 147);
        keyInfoPanel.setLayout(null);
        keyListPanel.add(keyInfoPanel);

        addLabel("label.signerDialog.keyInfo.algorithm", GUiConstants.LABEL_FOREGROUND_COLOR, new Rectangle(15, 122, 174, 14), keyInfoPanel);
        algNameText = addDynamicLabel(new Rectangle(185, 123, 430, 14), keyInfoPanel);

        addLabel("label.signerDialog.keyInfo.keyOwner", GUiConstants.LABEL_FOREGROUND_COLOR, new Rectangle(15, 22, 174, 14), keyInfoPanel);
        keyOwnerText = addDynamicLabel(new Rectangle(185, 23, 430, 14), keyInfoPanel);

        addLabel("label.signerDialog.keyInfo.serialNum", GUiConstants.LABEL_FOREGROUND_COLOR, new Rectangle(15, 72, 174, 14), keyInfoPanel);
        serialNumberText = addDynamicLabel(new Rectangle(185, 73, 440, 14), keyInfoPanel);

        addLabel("label.signerDialog.keyInfo.period", GUiConstants.LABEL_FOREGROUND_COLOR, new Rectangle(15, 47, 174, 14), keyInfoPanel);
        periodText = addDynamicLabel(new Rectangle(185, 48, 440, 14), keyInfoPanel);

        addLabel("label.signerDialog.keyInfo.issuer", GUiConstants.LABEL_FOREGROUND_COLOR, new Rectangle(15, 97, 174, 14), keyInfoPanel);
        issuerNameText = addDynamicLabel(new Rectangle(185, 98, 430, 14), keyInfoPanel);

        signLoadingJLabel = new JLabel();
        signLoadingJLabel.setIcon(loadingImage);
        signLoadingJLabel.setBounds(174, 217, 32, 32);
        keyListPanel.add(signLoadingJLabel);
        signLoadingJLabel.setVisible(false);

        keyListComboBox = new JComboBox();
        keyListComboBox.setBounds(0, 29, 630, 20);
        keyListComboBox.addItemListener((ItemEvent e) -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                selectedItem = (KeyListEntry) keyListComboBox.getItemAt(keyListComboBox.getSelectedIndex());
                fillSelectedKey();
            }
        });

        keyListPanel.add(keyListComboBox);

        signButton = addButton(titles.containsKey("actionBtn") ? titles.get("actionBtn") : "button.signerDialog.sign", GUiConstants.BUTTON_BACKGROUND_COLOR,
                new Rectangle(211, 215, 207, 35), keyListPanel);

        signButton.addActionListener((a)->{
                doSign = true;
                setVisible(false);
                dispose();
        });

        cancelButton = addButton("button.cancel", GUiConstants.BUTTON_BACKGROUND_COLOR,
                new Rectangle(423, 215, 207, 35), keyListPanel);

        cancelButton.addActionListener((a)->{
            setVisible(false);
            dispose();
        });
        keyListPanel.setVisible(false);
        return keyInfoPanel;
    }

    private JLabel getRefreshLoadingJLabel(){
        refreshLoadingJLabel = new JLabel();
        loadingImage = new ImageIcon(SignerDialog.class.getClassLoader().getResource("loading.gif"));
        refreshLoadingJLabel.setIcon(loadingImage);
        refreshLoadingJLabel.setBounds(189, 88, 32, 32);
        refreshLoadingJLabel.setVisible(false);
        return refreshLoadingJLabel;
    }

    private void addLabel(String key, Color foregroundColor, Rectangle rectangle, JPanel paretnt) {
        JLabel label;
        label = (key == null) ? new JLabel() : new JLabel(ProgramSettings.getInstance().getDictionary(key));
        label.setFont(new Font(GUiConstants.LABEL_FONT_NAME, Font.PLAIN, GUiConstants.SIGNER_DIALOG_FONT_SIZE));
        label.setForeground(foregroundColor);
        label.setBounds(rectangle);
        paretnt.add(label);
    }

    private JLabel addDynamicLabel(Rectangle rectangle, JPanel paretnt) {
        JLabel label = new JLabel();
        label.setFont(new Font(GUiConstants.LABEL_FONT_NAME, Font.PLAIN, GUiConstants.SIGNER_DIALOG_FONT_SIZE));
        label.setForeground(GUiConstants.BLACK_FOREGROUND_COLOR);
        label.setBounds(rectangle);
        paretnt.add(label);
        return label;
    }

    private JButton addButton(String key, Color backgroundColor, Rectangle rectangle, JPanel paretnt) {
        JButton button = new JButton(ProgramSettings.getInstance().getDictionary(key));
        button.setContentAreaFilled(false);
        button.setOpaque(true);
        button.setForeground(GUiConstants.BUTTON_FOREGROUND_COLOR);
        button.setFont(new Font(GUiConstants.LABEL_FONT_NAME, Font.PLAIN, GUiConstants.LABEL_FONT_SIZE));
        button.setBackground(backgroundColor);
        button.setBounds(rectangle);
        button.setBorderPainted(false);
        paretnt.add(button);
        return button;
    }

    private void blockRefreshPart() {
        refreshLoadingJLabel.setVisible(true);
        passwordField.setEnabled(false);
        showPwdBtn.setEnabled(false);
        if(containerComboBox != null){
            containerComboBox.setEnabled(false);
        }
        refreshKeyListButton.setEnabled(false);
        refreshKeyListButton.setBackground(new Color(180, 180, 180));
        canButton.setEnabled(false);
        canButton.setBackground(new Color(180, 180, 180));
        contentPanel.repaint();
        contentPanel.revalidate();
    }

    private void unblockRefreshPart() {
        refreshLoadingJLabel.setVisible(false);
        passwordField.setEnabled(true);
        showPwdBtn.setEnabled(true);
        if(containerComboBox != null){
            containerComboBox.setEnabled(true);
        }
        refreshKeyListButton.setEnabled(true);
        refreshKeyListButton.setBackground(GUiConstants.BUTTON_BACKGROUND_COLOR);
        canButton.setEnabled(true);
        canButton.setBackground(GUiConstants.BUTTON_BACKGROUND_COLOR);
    }

    private void updateKeyList(java.util.List<KeyListEntry> keys) {
        keyListComboBox.removeAllItems();
        for (KeyListEntry key : keys) {
            keyListComboBox.addItem(key);
        }
        selectedItem = (KeyListEntry) keyListComboBox.getItemAt(0);
        fillSelectedKey();
    }

    private void fillSelectedKey() {
        algNameText.setText(selectedItem.getAlgorithm());
        storageInfo.setAlias(selectedItem.getKeyId());

        if (selectedItem.getX509Certificate() != null) {
            keyOwnerText.setText(selectedItem.getSubjectCn());
            periodText.setText(selectedItem.getPeriod());
            serialNumberText.setText(selectedItem.getSerialNumber());
            issuerNameText.setText(selectedItem.getIssuerCn());
            signButton.setVisible(true);
            if (selectedItem.expired) {
                setMessage(ProgramSettings.getInstance().getDictionary("label.errorMessage.expired"));
            } else {
                setMessage("");
            }
        } else {
            keyOwnerText.setText("—");
            periodText.setText("—");
            serialNumberText.setText("—");
            issuerNameText.setText("—");
            signButton.setVisible(false);
            setMessage(ProgramSettings.getInstance().getDictionary("label.message.certificateNotFound"));
        }
    }

    /**
     * Выводит сообщение ошибки пользователю
     *
     * @param message сообщение для уведомления
     */
    protected void setMessage(String message) {
        mesLabel.setText("<html><p>" + message + "</p></html>");
    }

    /**
     * Размещает фрейм (окно) по центру экрана
     */
    private void setFrameToScreenCenter() {
        Dimension d = getToolkit().getScreenSize();
        setLocation(d.width / 2 - getPreferredSize().width / 2, d.height / 2 - (getPreferredSize().height + 232) / 2);
    }

    private class KeyListEntry extends KeyStoreEntry {

        private String period;
        private boolean expired = false;

        public KeyListEntry(String keyId, String algorithm, X509Certificate x509Certificate) throws CertificateException {
            super(keyId, algorithm, x509Certificate);
            if (x509Certificate != null) {
                period = parseDateToString(x509Certificate.getNotBefore()) + " - " + parseDateToString(x509Certificate.getNotAfter());
                try {
                    x509Certificate.checkValidity();
                } catch (CertificateExpiredException | CertificateNotYetValidException e) {
                    expired = true;
                }
            }
        }

        public String getPeriod() {
            return period;
        }

        public void setPeriod(String period) {
            this.period = period;
        }

        public boolean isExpired() {
            return expired;
        }

        public void setExpired(boolean expired) {
            this.expired = expired;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(getAlgorithm()).append("<@>");
            if (getX509Certificate() == null) {
                sb.append("—").append("<@>");
                sb.append(getKeyId());
            } else {
                sb.append(getSubjectCn()).append("<@>");
                sb.append(period).append("<@>");
                sb.append(getKeyId()).append("<@>");
                sb.append(getSerialNumber()).append("<@>");
                sb.append(getIssuerCn()).append("<@>");
            }
            return sb.toString();
        }

        private String parseDateToString(Date date) {
            if (date == null) {
                return null;
            }
            String pattern = "dd.MM.yyyy (HH:mm)";
            DateFormat df = new SimpleDateFormat(pattern);
            df.setTimeZone(TimeZone.getTimeZone("Asia/Almaty"));
            return df.format(date);
        }
    }

    private class KeyListEntryComparator implements Comparator<KeyStoreEntry> {

        @Override
        public int compare(KeyStoreEntry o1, KeyStoreEntry o2) {
            if (o1.getX509Certificate() == null && o2.getX509Certificate() == null) {
                return 0;
            }
            if (o1.getX509Certificate() != null && o2.getX509Certificate() == null) {
                return -1;
            }
            if (o1.getX509Certificate() == null && o2.getX509Certificate() != null) {
                return 1;
            }
            return o1.getX509Certificate().getNotBefore().compareTo(o2.getX509Certificate().getNotBefore());

        }

    }
}
