package com.gametools.keytool;

import org.spongycastle.jce.provider.BouncyCastleProvider;
import org.spongycastle.x509.X509V3CertificateGenerator;
import java.io.FileOutputStream;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.Date;
import javax.security.auth.x500.X500Principal;

public class KeyGeneratorUtil {

    // Статический блок инициализации для регистрации крипто-провайдера
    static {
        // Добавляем Spongy Castle в систему под коротким именем "SC"
        Security.addProvider(new BouncyCastleProvider());
    }

    /**
     * Метод генерирует пару ключей RSA, создает X.509 сертификат 
     * и сохраняет файлы .ks (BKS) и .cer в указанную папку.
     */
    public static void createKeysAndCert(String folderPath, String password, String alias) throws Exception {
        String ksPath = folderPath + "/keystore.ks";
        String cerPath = folderPath + "/certificate.cer";
        char[] pwdArray = password.toCharArray();

        // 1. Генерация пары ключей RSA (1024 бит — требование старых Sony Ericsson)
        KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("RSA", "SC");
        keyPairGen.initialize(1024, new SecureRandom());
        KeyPair keyPair = keyPairGen.generateKeyPair();

        // 2. Создание структуры X.509 сертификата (Self-Signed)
        X509V3CertificateGenerator certGen = new X509V3CertificateGenerator();
        X500Principal dnName = new X500Principal("CN=SonyEricssonDev, O=RetroModding, C=RU");

        certGen.setSerialNumber(BigInteger.valueOf(System.currentTimeMillis()));
        certGen.setSubjectDN(dnName);
        certGen.setIssuerDN(dnName); // Издатель совпадает с владельцем (самоподписанный)
        
        // Срок действия: от вчерашнего дня на 10 лет вперед
        certGen.setNotBefore(new Date(System.currentTimeMillis() - 1000L * 60 * 60 * 24));
        certGen.setNotAfter(new Date(System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 365 * 10));
        
        certGen.setPublicKey(keyPair.getPublic());
        certGen.setSignatureAlgorithm("SHA1withRSA"); // Алгоритм хэша, совместимый со старыми телефонами

        // Генерируем сам сертификат, подписывая его приватным ключом
        X509Certificate cert = certGen.generate(keyPair.getPrivate(), "SC");

        // 3. Бинарная запись публичного СЕРТИФИКАТА (.cer) для переноса в PhoneFS
        FileOutputStream cerOut = null;
        try {
            cerOut = new FileOutputStream(cerPath);
            cerOut.write(cert.getEncoded());
        } finally {
            if (cerOut != null) {
                cerOut.close();
            }
        }

        // 4. Создание ХРАНИЛИЩА КЛЮЧЕЙ (.ks) в стандартном формате Java — JKS (BKS)
        KeyStore keyStore = KeyStore.getInstance("BKS", "SC");
        keyStore.load(null, pwdArray); // Инициализация чистого хранилища в памяти

        // Создаем цепочку сертификатов (из одного нашего элемента)
        java.security.cert.Certificate[] chain = new java.security.cert.Certificate[]{cert};
        
        // Помещаем приватный ключ в хранилище под указанным алиасом
        keyStore.setKeyEntry(alias, keyPair.getPrivate(), pwdArray, chain);

        // Записываем готовый файл .ks на флешку / в память
        FileOutputStream ksOut = null;
        try {
            ksOut = new FileOutputStream(ksPath);
            keyStore.store(ksOut, pwdArray);
        } finally {
            if (ksOut != null) {
                ksOut.close();
            }
        }
    }
}
