package kz.gov.pki.knca.types;

import kz.gov.pki.kalkan.Storage;
import kz.gov.pki.kalkan.exception.KalkanException;
import kz.gov.pki.knca.BundleLog;
import kz.gov.pki.knca.gui.fileChooser.FileChooserDialog;
import kz.gov.pki.provider.exception.ProviderUtilException;
import kz.gov.pki.provider.utils.KeyStoreUtil;
import lombok.Data;

import javax.smartcardio.CardException;
import javax.swing.*;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Yerlan.Yesmukhanov on 20.06.2016.
 */
@Data
public class StorageInfo {
    private String alias;
    private String container;
    private char[] password;
    private KeyStore keyStore;
    private Storage storage;
    private List<String> containers;

    public StorageInfo(String storageName) throws ClientException {
        if (storageName == null || storageName.trim().isEmpty()) {
            throw new ClientException("storageName.param.empty");
        }
        storage = Storage.get(storageName);
        if (storage == null) {
            throw new ClientException("storage.unknown");
        }

        if(storage.isToken() && getContainers() == null){
            throw new ClientException("storage.empty");
        }

        if (!storage.isToken()){
            container = chooseFile();
        }else{
            container = getContainers().get(0);
        }
    }

    public List<String> getContainers() {
        if (containers == null && storage.isToken()) {
            try {
                for (String tName : KeyStoreUtil.loadSlotList(storage)) {
                    if(containers == null) {
                        containers = new ArrayList<>();
                    }
                    containers.add(tName);
                }
            } catch (CardException | ProviderUtilException | KalkanException e) {
                BundleLog.LOG.error(e.getMessage(), e);
            }
        }
        return containers;
    }

    private String chooseFile() throws ClientException {
        FileChooserDialog fileChooserDialog = new FileChooserDialog(new JFrame(), storage.equals(Storage.PKCS12) ? "P12" : "ALL", "");
        if (fileChooserDialog.getSelectedFilePath() != null) {
            return fileChooserDialog.getSelectedFilePath();
        } else {
            throw new ClientException("action.canceled");
        }
    }
}
