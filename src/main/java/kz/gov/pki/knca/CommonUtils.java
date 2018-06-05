package kz.gov.pki.knca;

import kz.gov.pki.kalkan.Storage;
import kz.gov.pki.kalkan.exception.KalkanException;
import kz.gov.pki.kalkan.jce.provider.cms.CMSSignedData;
import kz.gov.pki.knca.gui.fileChooser.FileChooserDialog;
import kz.gov.pki.knca.gui.dialog.SignerDialog;
import kz.gov.pki.knca.types.ClientException;
import kz.gov.pki.knca.types.KeyInfo;
import kz.gov.pki.knca.types.ResponseMessage;
import kz.gov.pki.knca.types.StorageInfo;
import kz.gov.pki.provider.exception.ProviderUtilException;
import kz.gov.pki.provider.utils.CMSUtil;
import kz.gov.pki.provider.utils.KeyStoreUtil;
import kz.gov.pki.provider.utils.XMLUtil;
import kz.gov.pki.reference.*;
import org.json.JSONObject;
import org.osgi.service.log.LogService;

import javax.smartcardio.CardException;
import javax.swing.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.util.*;

import static kz.gov.pki.knca.BundleLog.LOG;
import static kz.gov.pki.knca.BundleProvider.KALKAN;

/**
 * Created by Yerlan.Yesmukhanov
 */
public class CommonUtils {

    public CommonUtils() {
        String osname = System.getProperty("os.name").toLowerCase();
        try {
            if (osname.contains("mac os")) {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            } else {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            }
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
    }

    /**
     *  Возвращает список активных хранилищ
     */
    public String getActiveTokens() {
        ResponseMessage<List<String>> res = new ResponseMessage("200");
        res.setResponseObject(new ArrayList<>());
        for (Storage storage : Storage.values()){
            try {
                if(!KeyStoreUtil.loadSlotList(storage).isEmpty()){
                    res.getResponseObject().add(storage.getName());
                }
            } catch (Exception e) {}
        }
        return getJson(res);
    }

    /**
     * Возвращает информацию о ключе. Открывает диалоговое окно для выбора ключа.
     *
     * @param storageName Имя хранилища ключа
     * @return Объект {@link ResponseMessage} в json формате, содержащий объект {@link KeyInfo}
     */
    public String getKeyInfo(String storageName) {
        ResponseMessage<KeyInfo> res = new ResponseMessage("500");
        try {
            StorageInfo storageInfo = new StorageInfo(storageName);

            Map<String, String> titles = new HashMap<>();
            titles.put("title", "label.signerDialog.keyInfoTitle");
            titles.put("actionBtn", "button.signerDialog.keyInfo");
            titles.put("header", "label.signerDialog.infoTitle");

            SignerDialog signerDialog = new SignerDialog(storageInfo, null, titles);
            signerDialog.setVisible(true);

            if (signerDialog.getDoSign()) {
                KeyStoreEntry keyStoreEntry = signerDialog.getSelectedItem();

                res.setCode("200");
                res.setResponseObject(new KeyInfo(keyStoreEntry, storageInfo.getAlias()));
            } else {
                res.setMessage("action.canceled");
            }
        } catch (Exception e) {
            LOG.log(LogService.LOG_ERROR, e.getMessage(), e);
            res.setMessage(e.getMessage());
        }
        return getJson(res);
    }

    /**
     * Функция для подписи xml. Открывает диалоговое окно для выбора ключа.
     *
     * @param storageName                 тип хранилища
     * @param keyTypeValue                тип использования ключа
     * @param xml                         Исходный XML для подписи заданный в виде строки.
     * @param tbsElementXPath             XPath выражение ссылающееся на элемент XML файла
     *                                    предназначенного для подписи.
     * @param signatureParentElementXPath XPath выражение ссылающееся на
     *                                    родительский элемент, куда будет записана подпись.
     * @return {@link ResponseMessage} в json формате, содержащий подписанный xml
     */
    public String signXml(String storageName, String keyTypeValue, String xml, String tbsElementXPath, String signatureParentElementXPath) {
        ResponseMessage<String> responseMessage = new ResponseMessage("500");
        try {
            StorageInfo storageInfo = new StorageInfo(storageName);

            KNCACertificateType keyType = null;
            try {
                keyType = KNCACertificateType.valueOf(keyTypeValue);
            } catch (Exception e) {
            }

            Map<String, String> titles = new HashMap<>();
            titles.put("title", "label.signerDialog.xmlTitle");

            SignerDialog signerDialog = new SignerDialog(storageInfo, keyType, titles);
            signerDialog.setVisible(true);

            if (signerDialog.getDoSign()) {
                KeyStore keyStore = KeyStoreUtil.getKeyStore(storageInfo.getStorage(), storageInfo.getContainer(), storageInfo.getPassword(), KALKAN.getProvider());
                responseMessage.setResponseObject(XMLUtil.createXmlSignature(xml, keyStore, storageInfo.getAlias(), storageInfo.getPassword(), tbsElementXPath, signatureParentElementXPath, KALKAN.getProvider()));
                responseMessage.setCode("200");
            } else {
                responseMessage.setMessage("action.canceled");
            }
        } catch (Exception e) {
            LOG.log(LogService.LOG_ERROR, e.getMessage(), e);
            responseMessage.setMessage(e.getMessage());
        }
        return getJson(responseMessage);
    }

    /**
     * Возвращает CMS подпись, которая формируется по расширенному стандарту
     * подписи CAdES (формат CAdES-BES). Открывает диалоговое окно для выбора ключа.
     *
     * @param storageName  тип хранилища.
     * @param keyTypeValue тип использования ключа.
     * @param base64_data  данные для подписи, либо DER-encoded байтовый массив CMS для добавления нового подписанта. В формате base64
     * @param attached     Флаг указывающий нужно ли присоединить подписываемые данные к подписи.
     * @return {@link ResponseMessage} в json формате, содержащий CMS подпись в формате base64
     */
    public String createCMSSignatureFromBase64(String storageName, String keyTypeValue, String base64_data, boolean attached) {
        ResponseMessage responseMessage = new ResponseMessage("500");
        try {
            StorageInfo storageInfo = new StorageInfo(storageName);

            KNCACertificateType keyType = null;
            try {
                keyType = KNCACertificateType.valueOf(keyTypeValue);
            } catch (Exception e) {
            }

            Map<String, String> titles = new HashMap<>();
            titles.put("title", "label.signerDialog.cmsTitle");

            SignerDialog signerDialog = new SignerDialog(storageInfo, keyType, titles);
            signerDialog.setVisible(true);

            if (signerDialog.getDoSign()) {
                KeyStore keyStore = KeyStoreUtil.getKeyStore(storageInfo.getStorage(), storageInfo.getContainer(), storageInfo.getPassword(), KALKAN.getProvider());
                CMSSignedData cmsSignedData = CMSUtil.createCAdES(keyStore, storageInfo.getAlias(), storageInfo.getPassword(), Base64.getDecoder().decode(base64_data), attached, KalkanHashAlgorithm.HASH_SHA256, TSAPolicy.TSA_RSA, KNCAServiceRequestMethod.GET, KALKAN.getProvider());
                responseMessage.setResponseObject(Base64.getEncoder().encodeToString(cmsSignedData.getEncoded()));
                responseMessage.setCode("200");
            } else {
                responseMessage.setMessage("action.canceled");
            }
        } catch (Exception e) {
            LOG.log(LogService.LOG_ERROR, e.getMessage(), e);
            responseMessage.setMessage(e.getMessage());
        }
        return getJson(responseMessage);
    }

    /**
     * Возвращает CMS подпись, которая формируется по расширенному стандарту
     * подписи CAdES (формат CAdES-BES). Открывает диалоговое окно для выбора ключа.
     *
     * @param storageName  тип хранилища.
     * @param keyTypeValue тип использования ключа.
     * @param filePath     Путь к файлу, который необходимо подписать
     * @param attached     Флаг указывающий нужно ли присоединить подписываемые данные к подписи.
     * @return {@link ResponseMessage} в json формате, содержащий CMS подпись в формате base64
     */
    public String createCMSSignatureFromFile(String storageName, String keyTypeValue, String filePath, boolean attached) {
        try {
            return createCMSSignatureFromBase64(storageName, keyTypeValue, getBase64FromFile(filePath), attached);
        } catch (ClientException e) {
            ResponseMessage responseMessage = new ResponseMessage("500");
            responseMessage.setMessage(e.getMessage());
            return getJson(responseMessage);
        }
    }

    /**
     * Открывает диалоговое окно для выбора файла
     *
     * @param fileExtension    Расширение для выбора файла
     * @param currentDirectory Путь выбираемого файла
     * @return {@link ResponseMessage} в json формате, содержащий полный путь к файлу
     */
    public String showFileChooser(String fileExtension, String currentDirectory) {
        ResponseMessage<String> responseMessage = new ResponseMessage("500");
        try {
            responseMessage.setResponseObject(chooseFile(fileExtension, currentDirectory));
            responseMessage.setCode("200");
        } catch (Exception e) {
            responseMessage.setMessage(e.getMessage());
        }
        return getJson(responseMessage);
    }

    private String getBase64FromFile(String file_path) throws ClientException {
        try {
            Path path = Paths.get(file_path);
            byte[] fileBytes = Files.readAllBytes(path);
            return Base64.getEncoder().encodeToString(fileBytes);
        } catch (IOException e) {
            throw new ClientException("file.not_found");
        }
    }

    private String chooseFile(String fileExt, String currentDirectory) throws ClientException {
        FileChooserDialog fileChooserDialog = new FileChooserDialog(new JFrame(), fileExt, currentDirectory);
        if (fileChooserDialog.getSelectedFilePath() != null) {
            return fileChooserDialog.getSelectedFilePath();
        } else {
            throw new ClientException("action.canceled");
        }
    }

    private String getJson(Object o) {
        return ((JSONObject) JSONObject.wrap(o)).toString();
    }
}
