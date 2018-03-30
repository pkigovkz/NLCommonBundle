package kz.gov.pki.knca.types;

import kz.gov.pki.kalkan.util.encoders.Hex;
import kz.gov.pki.provider.exception.ProviderUtilException;
import kz.gov.pki.provider.utils.X509Util;
import kz.gov.pki.reference.KeyStoreEntry;
import lombok.Data;
import org.osgi.service.log.LogService;
import java.io.IOException;
import java.security.cert.X509Certificate;

import static kz.gov.pki.knca.BundleLog.LOG;

@Data
public class KeyInfo {
    private String alias;
    private String keyId;
    private String algorithm;
    private String subjectCn;
    private String subjectDn;
    private String issuerCn;
    private String issuerDn;
    private String serialNumber;
    private String certNotAfter;
    private String certNotBefore;
    private String authorityKeyIdentifier;
    private String pem;

    public KeyInfo(KeyStoreEntry keyStoreEntry, String alias) {
        X509Certificate certificate = keyStoreEntry.getX509Certificate();
        this.alias = alias;
        subjectCn = keyStoreEntry.getSubjectCn();
        algorithm = keyStoreEntry.getAlgorithm();
        issuerCn = keyStoreEntry.getIssuerCn();
        serialNumber = keyStoreEntry.getSerialNumber();
        certNotAfter = String.valueOf(certificate.getNotAfter().getTime());
        certNotBefore = String.valueOf(certificate.getNotBefore().getTime());
        subjectDn = X509Util.getSubjectDN(certificate).toString();
        issuerDn = X509Util.getIssuerDN(certificate).toString();
        try {
            keyId = X509Util.getKeyId(certificate);
            authorityKeyIdentifier = Hex.encodeStr(X509Util.getAuthorityKeyIdentifier(certificate));
        } catch (Exception e) {
            LOG.log(LogService.LOG_ERROR, e.getMessage(), e);
        }
        try {
            pem = X509Util.getPem(keyStoreEntry.getX509Certificate());
        } catch (IOException e) {
            LOG.log(LogService.LOG_ERROR, e.getMessage(), e);
        }
    }
}
