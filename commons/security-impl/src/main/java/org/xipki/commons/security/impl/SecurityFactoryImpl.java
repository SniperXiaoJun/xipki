/*
 *
 * This file is part of the XiPKI project.
 * Copyright (c) 2013 - 2016 Lijun Liao
 * Author: Lijun Liao
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License version 3
 * as published by the Free Software Foundation with the addition of the
 * following permission added to Section 15 as permitted in Section 7(a):
 *
 * FOR ANY PART OF THE COVERED WORK IN WHICH THE COPYRIGHT IS OWNED BY
 * THE AUTHOR LIJUN LIAO. LIJUN LIAO DISCLAIMS THE WARRANTY OF NON INFRINGEMENT
 * OF THIRD PARTY RIGHTS.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * The interactive user interfaces in modified source and object code versions
 * of this program must display Appropriate Legal Notices, as required under
 * Section 5 of the GNU Affero General Public License.
 *
 * You can be released from the requirements of the license by purchasing
 * a commercial license. Buying such a license is mandatory as soon as you
 * develop commercial activities involving the XiPKI software without
 * disclosing the source code of your own applications.
 *
 * For more information, please contact Lijun Liao at this
 * address: lijun.liao@gmail.com
 */

package org.xipki.commons.security.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import javax.crypto.NoSuchPaddingException;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.bouncycastle.asn1.pkcs.CertificationRequest;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.crypto.RuntimeCryptoException;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentVerifierProvider;
import org.bouncycastle.operator.DefaultDigestAlgorithmIdentifierFinder;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.bc.BcContentVerifierProviderBuilder;
import org.bouncycastle.operator.bc.BcDSAContentVerifierProviderBuilder;
import org.bouncycastle.operator.bc.BcECContentVerifierProviderBuilder;
import org.bouncycastle.operator.bc.BcRSAContentVerifierProviderBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.PKCSException;
import org.bouncycastle.util.encoders.Base64;
import org.bouncycastle.util.encoders.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xipki.commons.common.ConfPairs;
import org.xipki.commons.common.InvalidConfException;
import org.xipki.commons.common.util.CollectionUtil;
import org.xipki.commons.common.util.IoUtil;
import org.xipki.commons.common.util.LogUtil;
import org.xipki.commons.common.util.ParamUtil;
import org.xipki.commons.common.util.StringUtil;
import org.xipki.commons.password.api.PasswordResolver;
import org.xipki.commons.password.api.PasswordResolverException;
import org.xipki.commons.security.api.AbstractSecurityFactory;
import org.xipki.commons.security.api.ConcurrentContentSigner;
import org.xipki.commons.security.api.KeyCertPair;
import org.xipki.commons.security.api.NoIdleSignerException;
import org.xipki.commons.security.api.SecurityException;
import org.xipki.commons.security.api.SecurityFactory;
import org.xipki.commons.security.api.SignatureAlgoControl;
import org.xipki.commons.security.api.p11.P11Constants;
import org.xipki.commons.security.api.p11.P11Control;
import org.xipki.commons.security.api.p11.P11CryptService;
import org.xipki.commons.security.api.p11.P11CryptServiceFactory;
import org.xipki.commons.security.api.p11.P11EntityIdentifier;
import org.xipki.commons.security.api.p11.P11KeyIdentifier;
import org.xipki.commons.security.api.p11.P11MechanismFilter;
import org.xipki.commons.security.api.p11.P11Module;
import org.xipki.commons.security.api.p11.P11ModuleConf;
import org.xipki.commons.security.api.p11.P11NullPasswordRetriever;
import org.xipki.commons.security.api.p11.P11PasswordRetriever;
import org.xipki.commons.security.api.p11.P11PermitAllMechanimFilter;
import org.xipki.commons.security.api.p11.P11Slot;
import org.xipki.commons.security.api.p11.P11SlotIdentifier;
import org.xipki.commons.security.api.p11.P11TokenException;
import org.xipki.commons.security.api.p11.P11UnknownEntityException;
import org.xipki.commons.security.api.util.AlgorithmUtil;
import org.xipki.commons.security.api.util.KeyUtil;
import org.xipki.commons.security.api.util.X509Util;
import org.xipki.commons.security.impl.p11.P11ContentSignerBuilder;
import org.xipki.commons.security.impl.p11.P11MechanismFilterImpl;
import org.xipki.commons.security.impl.p11.P11PasswordRetrieverImpl;
import org.xipki.commons.security.impl.p11.iaik.IaikP11CryptServiceFactory;
import org.xipki.commons.security.impl.p11.keystore.KeystoreP11CryptServiceFactory;
import org.xipki.commons.security.impl.p11.proxy.RemoteP11CryptServiceFactory;
import org.xipki.commons.security.impl.p12.SoftTokenContentSignerBuilder;
import org.xipki.commons.security.p11.conf.jaxb.MechanismsType;
import org.xipki.commons.security.p11.conf.jaxb.ModuleType;
import org.xipki.commons.security.p11.conf.jaxb.ModulesType;
import org.xipki.commons.security.p11.conf.jaxb.NativeLibraryType;
import org.xipki.commons.security.p11.conf.jaxb.ObjectFactory;
import org.xipki.commons.security.p11.conf.jaxb.PKCS11ConfType;
import org.xipki.commons.security.p11.conf.jaxb.PasswordType;
import org.xipki.commons.security.p11.conf.jaxb.PasswordsType;
import org.xipki.commons.security.p11.conf.jaxb.PermittedMechanismsType;
import org.xipki.commons.security.p11.conf.jaxb.SlotType;
import org.xipki.commons.security.p11.conf.jaxb.SlotsType;
import org.xml.sax.SAXException;

/**
 * @author Lijun Liao
 * @since 2.0.0
 */

public class SecurityFactoryImpl extends AbstractSecurityFactory {

    private static final Logger LOG = LoggerFactory.getLogger(SecurityFactoryImpl.class);

    private static final DefaultDigestAlgorithmIdentifierFinder DFLT_DIGESTALG_IDENTIFIER_FINDER =
            new DefaultDigestAlgorithmIdentifierFinder();

    private static final Map<String, BcContentVerifierProviderBuilder> VERIFIER_PROVIDER_BUILDER
        = new HashMap<>();

    private String pkcs11Provider;

    private int defaultParallelism = 32;

    private P11Control p11Control;

    private P11CryptServiceFactory p11CryptServiceFactory;

    private boolean p11CryptServiciceFactoryInitialized;

    private PasswordResolver passwordResolver;

    private String pkcs11ConfFile;

    private boolean strongRandom4KeyEnabled = true;

    private boolean strongRandom4SignEnabled;

    private final Map<String, String> signerTypeMapping = new HashMap<>();

    public SecurityFactoryImpl() {
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    public boolean isStrongRandom4KeyEnabled() {
        return strongRandom4KeyEnabled;
    }

    public void setStrongRandom4KeyEnabled(
            final boolean strongRandom4KeyEnabled) {
        this.strongRandom4KeyEnabled = strongRandom4KeyEnabled;
    }

    public boolean isStrongRandom4SignEnabled() {
        return strongRandom4SignEnabled;
    }

    public void setStrongRandom4SignEnabled(
            final boolean strongRandom4SignEnabled) {
        this.strongRandom4SignEnabled = strongRandom4SignEnabled;
    }

    @Override
    public ConcurrentContentSigner createSigner(
            final String type,
            final String confWithoutAlgo,
            final String hashAlgo,
            final SignatureAlgoControl sigAlgoControl,
            final X509Certificate[] certs)
    throws SecurityException {
        ConcurrentContentSigner signer = doCreateSigner(type, confWithoutAlgo, hashAlgo,
                sigAlgoControl, certs);
        validateSigner(signer, type, confWithoutAlgo);
        return signer;
    }

    @Override
    public ConcurrentContentSigner createSigner(
            final String type,
            final String conf,
            final X509Certificate[] certificateChain)
    throws SecurityException {
        ConcurrentContentSigner signer = doCreateSigner(type, conf, null, null,
                certificateChain);
        validateSigner(signer, type, conf);
        return signer;
    }

    /*
     * sigAlgoControl will be considered only if hashAlgo is not set
     *
     */
    private ConcurrentContentSigner doCreateSigner(
            final String type,
            final String conf,
            final String hashAlgo,
            final SignatureAlgoControl sigAlgoControl,
            final X509Certificate[] certificateChain)
    throws SecurityException {
        String tmpType = type;
        if (signerTypeMapping.containsKey(tmpType)) {
            tmpType = signerTypeMapping.get(tmpType);
        }

        if ("PKCS11".equalsIgnoreCase(tmpType)
                || "PKCS12".equalsIgnoreCase(tmpType)
                || "JKS".equalsIgnoreCase(tmpType)) {
            ConfPairs keyValues = new ConfPairs(conf);

            String str = keyValues.getValue("parallelism");
            int parallelism = defaultParallelism;
            if (str != null) {
                try {
                    parallelism = Integer.parseInt(str);
                } catch (NumberFormatException ex) {
                    throw new SecurityException("invalid parallelism " + str);
                }

                if (parallelism < 1) {
                    throw new SecurityException("invalid parallelism " + str);
                }
            }

            if ("PKCS11".equalsIgnoreCase(tmpType)) {
                String moduleName = keyValues.getValue("module");
                if (moduleName == null) {
                    moduleName = DEFAULT_P11MODULE_NAME;
                }

                str = keyValues.getValue("slot");
                Integer slotIndex = (str == null)
                        ? null
                        : Integer.parseInt(str);

                str = keyValues.getValue("slot-id");
                Long slotId = (str == null)
                        ? null
                        : Long.parseLong(str);

                if ((slotIndex == null && slotId == null)
                        || (slotIndex != null && slotId != null)) {
                    throw new SecurityException(
                            "exactly one of slot (index) and slot-id must be specified");
                }

                String keyLabel = keyValues.getValue("key-label");
                str = keyValues.getValue("key-id");
                byte[] keyId = null;
                if (str != null) {
                    keyId = Hex.decode(str);
                }

                if ((keyId == null && keyLabel == null)
                        || (keyId != null && keyLabel != null)) {
                    throw new SecurityException(
                            "exactly one of key-id and key-label must be specified");
                }

                P11CryptService p11Service;
                P11Slot slot;
                try {
                    p11Service = getP11CryptService(moduleName);
                    P11Module module = p11Service.getModule();
                    P11SlotIdentifier p11SlotId = (slotId != null)
                            ? module.getSlotIdForId(slotId)
                            : module.getSlotIdForIndex(slotIndex);
                    slot = module.getSlot(p11SlotId);
                } catch (P11TokenException ex) {
                    throw new SecurityException("P11TokenException: " + ex.getMessage(), ex);
                }

                P11KeyIdentifier p11KeyId;
                try {
                    p11KeyId = (keyId != null)
                        ? slot.getKeyIdForId(keyId)
                        : slot.getKeyIdForLabel(keyLabel);
                } catch (P11UnknownEntityException ex) {
                    throw new SecurityException("unknown PKCS#11 entity: " + ex.getMessage(), ex);
                }

                P11EntityIdentifier entityId = new P11EntityIdentifier(slot.getSlotId(), p11KeyId);

                try {
                    AlgorithmIdentifier signatureAlgId;
                    if (hashAlgo == null) {
                        signatureAlgId = getSignatureAlgoId(conf);
                    } else {
                        PublicKey pubKey = slot.getIdentity(p11KeyId).getPublicKey();
                        signatureAlgId = AlgorithmUtil.getSignatureAlgoId(pubKey, hashAlgo,
                                sigAlgoControl);
                    }

                    P11ContentSignerBuilder signerBuilder = new P11ContentSignerBuilder(
                            p11Service, (SecurityFactory) this, entityId, certificateChain);
                    return signerBuilder.createSigner(signatureAlgId, parallelism);
                } catch (P11TokenException | NoSuchAlgorithmException ex) {
                    throw new SecurityException(ex.getMessage(), ex);
                }
            } else {
                String passwordHint = keyValues.getValue("password");
                char[] password;
                if (passwordHint == null) {
                    password = null;
                } else {
                    if (passwordResolver == null) {
                        password = passwordHint.toCharArray();
                    } else {
                        try {
                            password = passwordResolver.resolvePassword(passwordHint);
                        } catch (PasswordResolverException ex) {
                            throw new SecurityException(
                                    "could not resolve password. Message: " + ex.getMessage());
                        }
                    }
                }

                str = keyValues.getValue("keystore");
                String keyLabel = keyValues.getValue("key-label");

                InputStream keystoreStream;
                if (StringUtil.startsWithIgnoreCase(str, "base64:")) {
                    keystoreStream = new ByteArrayInputStream(
                            Base64.decode(str.substring("base64:".length())));
                } else if (StringUtil.startsWithIgnoreCase(str, "file:")) {
                    String fn = str.substring("file:".length());
                    try {
                        keystoreStream = new FileInputStream(IoUtil.expandFilepath(fn));
                    } catch (FileNotFoundException ex) {
                        throw new SecurityException("file not found: " + fn);
                    }
                } else {
                    throw new SecurityException("unknown keystore content format");
                }

                SoftTokenContentSignerBuilder signerBuilder = new SoftTokenContentSignerBuilder(
                        tmpType, keystoreStream, password, keyLabel, password, certificateChain);

                try {
                    AlgorithmIdentifier signatureAlgId;
                    if (hashAlgo == null) {
                        signatureAlgId = getSignatureAlgoId(conf);
                    } else {
                        PublicKey pubKey = signerBuilder.getCert().getPublicKey();
                        signatureAlgId = AlgorithmUtil.getSignatureAlgoId(pubKey, hashAlgo,
                                sigAlgoControl);
                    }

                    return signerBuilder.createSigner(
                            signatureAlgId, parallelism, getRandom4Sign());
                } catch (OperatorCreationException | NoSuchPaddingException
                        | NoSuchAlgorithmException ex) {
                    throw new SecurityException(String.format("%s: %s",
                            ex.getClass().getName(), ex.getMessage()));
                }
            }
        } else if (StringUtil.startsWithIgnoreCase(tmpType, "java:")) {
            if (hashAlgo == null) {
                ConcurrentContentSigner contentSigner;
                String classname = tmpType.substring("java:".length());
                try {
                    Class<?> clazz = Class.forName(classname);
                    contentSigner = (ConcurrentContentSigner) clazz.newInstance();
                } catch (Exception ex) {
                    throw new SecurityException(ex.getMessage(), ex);
                }
                contentSigner.initialize(conf, passwordResolver);

                if (certificateChain != null) {
                    contentSigner.setCertificateChain(certificateChain);
                }

                return contentSigner;
            } else {
                throw new SecurityException("unknwon type: " + tmpType);
            }
        } else {
            throw new SecurityException("unknwon type: " + tmpType);
        }
    } // method doCreateSigner

    private AlgorithmIdentifier getSignatureAlgoId(
            final String signerConf)
    throws SecurityException {
        ConfPairs keyValues = new ConfPairs(signerConf);
        String algoS = keyValues.getValue("algo");
        if (algoS == null) {
            throw new SecurityException("algo is not specified");
        }
        try {
            return AlgorithmUtil.getSignatureAlgoId(algoS);
        } catch (NoSuchAlgorithmException ex) {
            throw new SecurityException(ex.getMessage(), ex);
        }
    }

    @Override
    public ContentVerifierProvider getContentVerifierProvider(
            final PublicKey publicKey)
    throws InvalidKeyException {
        ParamUtil.requireNonNull("publicKey", publicKey);

        String keyAlg = publicKey.getAlgorithm().toUpperCase();
        if ("EC".equals(keyAlg)) {
            keyAlg = "ECDSA";
        }

        BcContentVerifierProviderBuilder builder = VERIFIER_PROVIDER_BUILDER.get(keyAlg);
        if (builder == null) {
            if ("RSA".equals(keyAlg)) {
                builder = new BcRSAContentVerifierProviderBuilder(DFLT_DIGESTALG_IDENTIFIER_FINDER);
            } else if ("DSA".equals(keyAlg)) {
                builder = new BcDSAContentVerifierProviderBuilder(DFLT_DIGESTALG_IDENTIFIER_FINDER);
            } else if ("ECDSA".equals(keyAlg)) {
                builder = new BcECContentVerifierProviderBuilder(DFLT_DIGESTALG_IDENTIFIER_FINDER);
            } else {
                throw new InvalidKeyException("unknown key algorithm of the public key "
                        + keyAlg);
            }
            VERIFIER_PROVIDER_BUILDER.put(keyAlg, builder);
        }

        AsymmetricKeyParameter keyParam = KeyUtil.generatePublicKeyParameter(publicKey);
        try {
            return builder.build(keyParam);
        } catch (OperatorCreationException ex) {
            throw new InvalidKeyException("could not build ContentVerifierProvider: "
                    + ex.getMessage(), ex);
        }
    }

    @Override
    public PublicKey generatePublicKey(
            final SubjectPublicKeyInfo subjectPublicKeyInfo)
    throws InvalidKeyException {
        try {
            return KeyUtil.generatePublicKey(subjectPublicKeyInfo);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException ex) {
            throw new InvalidKeyException(ex.getMessage(), ex);
        }
    }

    @Override
    public boolean verifyPopo(
            final CertificationRequest p10Req) {
        return verifyPopo(new PKCS10CertificationRequest(p10Req));
    }

    @Override
    public boolean verifyPopo(
            final PKCS10CertificationRequest p10Request) {
        try {
            SubjectPublicKeyInfo pkInfo = p10Request.getSubjectPublicKeyInfo();
            PublicKey pk = KeyUtil.generatePublicKey(pkInfo);

            ContentVerifierProvider cvp = getContentVerifierProvider(pk);
            return p10Request.isSignatureValid(cvp);
        } catch (InvalidKeyException | PKCSException | NoSuchAlgorithmException
                | InvalidKeySpecException ex) {
            String message = "could not validate POPO of PKCS#10 request";
            LOG.error(LogUtil.buildExceptionLogFormat(message), ex.getClass().getName(),
                    ex.getMessage());
            LOG.error(message, ex);
            return false;
        }
    }

    public void setPkcs11Provider(
            final String pkcs11Provider) {
        this.pkcs11Provider = pkcs11Provider;
    }

    public void setDefaultParallelism(
            final int defaultParallelism) {
        if (defaultParallelism > 0) {
            this.defaultParallelism = defaultParallelism;
        }
    }

    @Override
    public P11CryptService getP11CryptService(
            final String moduleName)
    throws SecurityException, P11TokenException {
        initP11CryptServiceFactory();
        return p11CryptServiceFactory.getP11CryptService(getRealPkcs11ModuleName(moduleName));
    }

    @Override
    public Set<String> getP11ModuleNames() {
        initPkcs11ModuleConf();
        return (p11Control == null)
                ? null
                : p11Control.getModuleNames();
    }

    private synchronized void initP11CryptServiceFactory()
    throws SecurityException {
        if (p11CryptServiceFactory != null) {
            return;
        }

        if (p11CryptServiciceFactoryInitialized) {
            throw new SecurityException("initialization of P11CryptServiceFactory has been"
                    + " processed and failed, no retry");
        }

        try {
            initPkcs11ModuleConf();

            Object p11Provider;

            if (IaikP11CryptServiceFactory.class.getName().equals(pkcs11Provider)) {
                p11Provider = new IaikP11CryptServiceFactory();
            } else if (KeystoreP11CryptServiceFactory.class.getName().equals(pkcs11Provider)) {
                p11Provider = new KeystoreP11CryptServiceFactory();
            } else if (RemoteP11CryptServiceFactory.class.getName().equals(pkcs11Provider)) {
                p11Provider = new RemoteP11CryptServiceFactory();
            } else {
                try {
                    Class<?> clazz = Class.forName(pkcs11Provider);
                    p11Provider = clazz.newInstance();
                } catch (Exception ex) {
                    throw new SecurityException(ex.getMessage(), ex);
                }
            }

            if (p11Provider instanceof P11CryptServiceFactory) {
                P11CryptServiceFactory p11CryptServiceFact =
                        (P11CryptServiceFactory) p11Provider;
                p11CryptServiceFact.init(p11Control);
                this.p11CryptServiceFactory = p11CryptServiceFact;
            } else {
                throw new SecurityException(pkcs11Provider + " is not instanceof "
                        + P11CryptServiceFactory.class.getName());
            }
        } finally {
            p11CryptServiciceFactoryInitialized = true;
        }
    } // method initP11CryptServiceFactory

    private void initPkcs11ModuleConf() {
        if (p11Control != null) {
            return;
        }

        if (StringUtil.isBlank(pkcs11ConfFile)) {
            throw new IllegalStateException("pkcs11ConfFile is not set");
        }

        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(ObjectFactory.class);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            SchemaFactory schemaFact = SchemaFactory.newInstance(
                    javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI);
            Schema schema = schemaFact.newSchema(getClass().getResource(
                    "/xsd/pkcs11-conf.xsd"));
            unmarshaller.setSchema(schema);
            @SuppressWarnings("unchecked")
            JAXBElement<PKCS11ConfType> rootElement = (JAXBElement<PKCS11ConfType>)
                    unmarshaller.unmarshal(new File(pkcs11ConfFile));
            PKCS11ConfType pkcs11Conf = rootElement.getValue();
            ModulesType modulesType = pkcs11Conf.getModules();

            Map<String, P11ModuleConf> confs = new HashMap<>();
            for (ModuleType moduleType : modulesType.getModule()) {
                String name = moduleType.getName();
                if (DEFAULT_P11MODULE_NAME.equals(name)) {
                    throw new InvalidConfException("invald module name "
                            + DEFAULT_P11MODULE_NAME + ", it is reserved");
                }

                if (confs.containsKey(name)) {
                    throw new InvalidConfException(
                            "multiple modules with the same module name is not permitted");
                }

                // Mechanism filter
                P11MechanismFilter mechFilter;
                PermittedMechanismsType mechsType = moduleType.getPermittedMechanisms();
                if (mechsType == null || CollectionUtil.isEmpty(mechsType.getMechanisms())) {
                    mechFilter = P11PermitAllMechanimFilter.INSTANCE;
                } else {
                    mechFilter = new P11MechanismFilterImpl();
                    for (MechanismsType mechType : mechsType.getMechanisms()) {
                        Set<P11SlotIdentifier> slots = getSlots(mechType.getSlots());
                        Set<Long> mechanisms = new HashSet<>();
                        for (String mechStr : mechType.getMechanism()) {
                            Long mech = null;
                            if (mechStr.startsWith("CKM_")) {
                                mech = P11Constants.getMechanism(mechStr);
                            } else {
                                int radix = 10;
                                String value = mechStr.toLowerCase();
                                if (value.startsWith("0x")) {
                                    radix = 16;
                                    value = value.substring(2);
                                }

                                if (value.endsWith("l")) {
                                    value = value.substring(0, value.length() - 1);
                                }

                                try {
                                    mech = Long.parseLong(value, radix);
                                } catch (NumberFormatException ex) {// CHECKSTYLE:SKIP
                                }
                            }

                            if (mech == null) {
                                LOG.warn("skipped unknown mechanism '" + mechStr + "'");
                            } else {
                                mechanisms.add(mech);
                            }
                        }
                        ((P11MechanismFilterImpl) mechFilter).addEntry(slots, mechanisms);
                    }
                }

                // Password retriever
                P11PasswordRetriever pwdRetriever;

                PasswordsType passwordsType = moduleType.getPasswords();
                if (passwordsType == null || CollectionUtil.isEmpty(passwordsType.getPassword())) {
                    pwdRetriever = P11NullPasswordRetriever.INSTANCE;
                } else {
                    pwdRetriever = new P11PasswordRetrieverImpl();
                    ((P11PasswordRetrieverImpl) pwdRetriever).setPasswordResolver(
                            passwordResolver);

                    for (PasswordType passwordType : passwordsType.getPassword()) {
                        Set<P11SlotIdentifier> slots = getSlots(passwordType.getSlots());
                        ((P11PasswordRetrieverImpl) pwdRetriever).addPasswordEntry(
                                slots, new ArrayList<>(passwordType.getSinglePassword()));
                    }
                }

                Set<P11SlotIdentifier> includeSlots = getSlots(moduleType.getIncludeSlots());
                Set<P11SlotIdentifier> excludeSlots = getSlots(moduleType.getExcludeSlots());

                final String osName = System.getProperty("os.name").toLowerCase();
                String nativeLibraryPath = null;
                for (NativeLibraryType library
                        : moduleType.getNativeLibraries().getNativeLibrary()) {
                    List<String> osNames = library.getOs();
                    if (CollectionUtil.isEmpty(osNames)) {
                        nativeLibraryPath = library.getPath();
                    } else {
                        for (String entry : osNames) {
                            if (osName.contains(entry.toLowerCase())) {
                                nativeLibraryPath = library.getPath();
                                break;
                            }
                        }
                    }

                    if (nativeLibraryPath != null) {
                        break;
                    }
                } // end for (NativeLibraryType library)

                if (nativeLibraryPath == null) {
                    throw new InvalidConfException("could not find PKCS#11 library for OS "
                            + osName);
                }

                long userType = moduleType.getUser().longValue();
                int maxMsgSize = moduleType.getMaxMessageSize().intValue();
                P11ModuleConf conf = new P11ModuleConf(name, nativeLibraryPath, userType,
                        maxMsgSize, pwdRetriever, mechFilter, includeSlots, excludeSlots,
                        (SecurityFactory) this);
                confs.put(name, conf);
            } // end for (ModuleType moduleType

            if (!confs.containsKey(DEFAULT_P11MODULE_NAME)) {
                throw new InvalidConfException("module '" + DEFAULT_P11MODULE_NAME
                        + "' is not defined");
            }

            this.p11Control = new P11Control(new HashSet<>(confs.values()));
        } catch (JAXBException | SAXException | InvalidConfException ex) {
            final String message = "invalid configuration file " + pkcs11ConfFile;
            if (LOG.isErrorEnabled()) {
                final String exceptionMessage;
                if (ex instanceof JAXBException) {
                    exceptionMessage = getMessage((JAXBException) ex);
                } else {
                    exceptionMessage = ex.getMessage();
                }
                LOG.error(LogUtil.buildExceptionLogFormat(message), ex.getClass().getName(),
                        exceptionMessage);
            }
            LOG.debug(message, ex);

            throw new RuntimeException(message);
        }
    } // method initPkcs11ModuleConf

    public void setPkcs11ConfFile(
            final String confFile) {
        if (StringUtil.isBlank(confFile)) {
            this.pkcs11ConfFile = null;
        } else {
            this.pkcs11ConfFile = confFile;
        }
    }

    private String getRealPkcs11ModuleName(
            final String moduleName) {
        return (moduleName == null)
                ? DEFAULT_P11MODULE_NAME
                : moduleName;
    }

    public void setPasswordResolver(
            final PasswordResolver passwordResolver) {
        this.passwordResolver = passwordResolver;
    }

    @Override
    public PasswordResolver getPasswordResolver() {
        return passwordResolver;
    }

    public void setSignerTypeMap(
            final String signerTypeMap) {
        if (signerTypeMap == null) {
            LOG.debug("signerTypeMap is null");
            return;
        }

        String tmpSignerTypeMap = signerTypeMap.trim();
        if (StringUtil.isBlank(tmpSignerTypeMap)) {
            LOG.debug("signerTypeMap is empty");
            return;
        }

        StringTokenizer st = new StringTokenizer(tmpSignerTypeMap, " \t");
        while (st.hasMoreTokens()) {
            String token = st.nextToken();
            StringTokenizer st2 = new StringTokenizer(token, "=");
            if (st2.countTokens() != 2) {
                LOG.warn("invalid signerTypeMap entry '" + token + "'");
                continue;
            }

            String alias = st2.nextToken();
            if (signerTypeMapping.containsKey(alias)) {
                LOG.warn("signerType alias '{}' already defined, ignore map '{}'", alias, token);
                continue;
            }
            String signerType = st2.nextToken();
            signerTypeMapping.put(alias, signerType);
            LOG.info("add alias '{}' for signerType '{}'", alias, signerType);
        }
    }

    @Override
    public KeyCertPair createPrivateKeyAndCert(
            final String type,
            final String conf,
            final X509Certificate cert)
    throws SecurityException {
        if (!"PKCS11".equalsIgnoreCase(type) && !"PKCS12".equalsIgnoreCase(type)) {
            throw new SecurityException("unsupported SCEP responder type '" + type + "'");
        }

        ConfPairs keyValues = new ConfPairs(conf);

        String passwordHint = keyValues.getValue("password");
        char[] password;
        if (passwordHint == null) {
            password = null;
        } else {
            if (passwordResolver == null) {
                password = passwordHint.toCharArray();
            } else {
                try {
                    password = passwordResolver.resolvePassword(passwordHint);
                } catch (PasswordResolverException ex) {
                    throw new SecurityException("could not resolve password. Message: "
                            + ex.getMessage());
                }
            }
        }

        String str = keyValues.getValue("keystore");
        String keyLabel = keyValues.getValue("key-label");

        InputStream keystoreStream;
        if (StringUtil.startsWithIgnoreCase(str, "base64:")) {
            keystoreStream = new ByteArrayInputStream(
                    Base64.decode(str.substring("base64:".length())));
        } else if (StringUtil.startsWithIgnoreCase(str, "file:")) {
            String fn = str.substring("file:".length());
            try {
                keystoreStream = new FileInputStream(IoUtil.expandFilepath(fn));
            } catch (FileNotFoundException ex) {
                throw new SecurityException("file not found: " + fn);
            }
        } else {
            throw new SecurityException("unknown keystore content format");
        }

        X509Certificate[] certs = (cert == null)
                ? null
                : new X509Certificate[]{cert};
        SoftTokenContentSignerBuilder signerBuilder = new SoftTokenContentSignerBuilder(
                type, keystoreStream, password, keyLabel, password,
                certs);

        KeyCertPair keycertPair = new KeyCertPair(
                signerBuilder.getKey(), signerBuilder.getCert());
        return keycertPair;
    } // method createPrivateKeyAndCert

    @Override
    public SecureRandom getRandom4Key() {
        return getSecureRandom(strongRandom4KeyEnabled);
    }

    @Override
    public SecureRandom getRandom4Sign() {
        return getSecureRandom(strongRandom4SignEnabled);
    }

    @Override
    public byte[] extractMinimalKeyStore(
            final String keystoreType,
            final byte[] keystoreBytes,
            final String keyname,
            final char[] password,
            final X509Certificate[] newCertChain)
    throws KeyStoreException {
        ParamUtil.requireNonBlank("keystoreType", keystoreType);
        ParamUtil.requireNonNull("keystoreBytes", keystoreBytes);

        try {
            KeyStore ks;
            if ("JKS".equalsIgnoreCase(keystoreType)) {
                ks = KeyStore.getInstance(keystoreType);
            } else {
                ks = KeyStore.getInstance(keystoreType, "BC");
            }
            ks.load(new ByteArrayInputStream(keystoreBytes), password);

            String tmpKeyname = keyname;
            if (tmpKeyname == null) {
                Enumeration<String> aliases = ks.aliases();
                while (aliases.hasMoreElements()) {
                    String alias = aliases.nextElement();
                    if (ks.isKeyEntry(alias)) {
                        tmpKeyname = alias;
                        break;
                    }
                }
            } else {
                if (!ks.isKeyEntry(tmpKeyname)) {
                    throw new KeyStoreException("unknown key named " + tmpKeyname);
                }
            }

            Enumeration<String> aliases = ks.aliases();
            int numAliases = 0;
            while (aliases.hasMoreElements()) {
                aliases.nextElement();
                numAliases++;
            }

            Certificate[] certs;
            if (newCertChain == null || newCertChain.length < 1) {
                if (numAliases == 1) {
                    return keystoreBytes;
                }
                certs = ks.getCertificateChain(tmpKeyname);
            } else {
                certs = newCertChain;
            }

            ks = null;

            if ("JKS".equalsIgnoreCase(keystoreType)) {
                ks = KeyStore.getInstance(keystoreType);
            } else {
                ks = KeyStore.getInstance(keystoreType, "BC");
            }
            ks.load(null, password);

            PrivateKey key = (PrivateKey) ks.getKey(tmpKeyname, password);
            ks.setKeyEntry(tmpKeyname, key, password, certs);
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            ks.store(bout, password);
            byte[] bytes = bout.toByteArray();
            bout.close();
            return bytes;
        } catch (Exception ex) {
            if (ex instanceof KeyStoreException) {
                throw (KeyStoreException) ex;
            } else {
                throw new KeyStoreException(ex.getMessage(), ex);
            }
        }
    } // method extractMinimalKeyStore

    public void shutdown() {
        if (p11CryptServiceFactory == null) {
            return;
        }

        try {
            p11CryptServiceFactory.shutdown();
        } catch (Throwable th) {
            LOG.error("could not shutdown KeyStoreP11ModulePool: " + th.getMessage(), th);
        }
    }

    private static SecureRandom getSecureRandom(
            final boolean strong) {
        if (!strong) {
            return new SecureRandom();
        }

        try {
            return SecureRandom.getInstanceStrong();
        } catch (NoSuchAlgorithmException ex) {
            throw new RuntimeCryptoException(
                    "could not get strong SecureRandom: " + ex.getMessage());
        }
    }

    private static void validateSigner(
            final ConcurrentContentSigner signer,
            final String signerType,
            final String signerConf)
    throws SecurityException {
        if (signer.getPublicKey() == null) {
            return;
        }

        String signatureAlgoName;
        try {
            signatureAlgoName = AlgorithmUtil.getSignatureAlgoName(
                    signer.getAlgorithmIdentifier());
        } catch (NoSuchAlgorithmException ex) {
            throw new SecurityException(ex.getMessage(), ex);
        }

        try {
            byte[] dummyContent = new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
            Signature verifier = Signature.getInstance(signatureAlgoName, "BC");

            byte[] signatureValue = signer.sign(dummyContent);

            verifier.initVerify(signer.getPublicKey());
            verifier.update(dummyContent);
            boolean valid = verifier.verify(signatureValue);
            if (!valid) {
                StringBuilder sb = new StringBuilder();
                sb.append("private key and public key does not match, ");
                sb.append("key type='").append(signerType).append("'; ");
                ConfPairs keyValues = new ConfPairs(signerConf);
                String pwd = keyValues.getValue("password");
                if (pwd != null) {
                    keyValues.putPair("password", "****");
                }
                keyValues.putPair("algo", signatureAlgoName);
                sb.append("conf='").append(keyValues.getEncoded());
                X509Certificate cert = signer.getCertificate();
                if (cert != null) {
                    String subject = X509Util.getRfc4519Name(cert.getSubjectX500Principal());
                    sb.append("', certificate subject='").append(subject).append("'");
                }

                throw new SecurityException(sb.toString());
            }
        } catch (IOException | NoSuchAlgorithmException | InvalidKeyException
                | SignatureException | NoSuchProviderException | NoIdleSignerException ex) {
            throw new SecurityException(ex.getMessage(), ex);
        }
    } // method validateSigner

    private static Set<P11SlotIdentifier> getSlots(
            final SlotsType type)
    throws InvalidConfException {
        if (type == null || CollectionUtil.isEmpty(type.getSlot())) {
            return null;
        }

        Set<P11SlotIdentifier> slots = new HashSet<>();
        for (SlotType slotType : type.getSlot()) {
            Long slotId = null;
            if (slotType.getId() != null) {
                String str = slotType.getId().trim();
                try {
                    if (StringUtil.startsWithIgnoreCase(str, "0X")) {
                        slotId = Long.parseLong(str.substring(2), 16);
                    } else {
                        slotId = Long.parseLong(str);
                    }
                } catch (NumberFormatException ex) {
                    String message = "invalid slotId '" + str + "'";
                    LOG.error(message);
                    throw new InvalidConfException(message);
                }
            }
            slots.add(new P11SlotIdentifier(slotType.getIndex(), slotId));
        }

        return slots;
    }

    private static String getMessage(
            final JAXBException ex) {
        String ret = ex.getMessage();
        if (ret == null && ex.getLinkedException() != null) {
            ret = ex.getLinkedException().getMessage();
        }
        return ret;
    }

}
