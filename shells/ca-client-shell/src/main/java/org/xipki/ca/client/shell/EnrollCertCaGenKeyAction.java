/*
 *
 * Copyright (c) 2013 - 2018 Lijun Liao
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.xipki.ca.client.shell;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.support.completers.FileCompleter;
import org.apache.karaf.shell.support.completers.StringsCompleter;
import org.bouncycastle.asn1.crmf.CertRequest;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.xipki.ca.client.api.CertifiedKeyPairOrError;
import org.xipki.ca.client.api.EnrollCertResult;
import org.xipki.ca.client.api.dto.EnrollCertRequest;
import org.xipki.ca.client.api.dto.EnrollCertRequest.Type;
import org.xipki.ca.client.api.dto.EnrollCertRequestEntry;
import org.xipki.shell.CmdFailure;
import org.xipki.shell.IllegalCmdParamException;
import org.xipki.shell.completer.DerPemCompleter;
import org.xipki.util.StringUtil;

/**
 * TODO.
 * @author Lijun Liao
 * @since 2.0.0
 */

@Command(scope = "xi", name = "cmp-enroll-cagenkey",
    description = "enroll certificate (keypair will be generated by the CA)")
@Service
public class EnrollCertCaGenKeyAction extends EnrollAction {

  @Option(name = "--cmpreq-type",
      description = "CMP request type (ir for Initialization Request,\n"
          + "and cr for Certification Request)")
  @Completion(value = StringsCompleter.class, values = {"ir", "cr"})
  private String cmpreqType = "cr";

  @Option(name = "--cert-outform", description = "output format of the certificate")
  @Completion(DerPemCompleter.class)
  private String certOutform = "der";

  @Option(name = "--cert-out", description = "where to save the certificate")
  @Completion(FileCompleter.class)
  private String certOutputFile;

  @Option(name = "--p12-out", required = true, description = "where to save the PKCS#12 keystore")
  @Completion(FileCompleter.class)
  private String p12OutputFile;

  @Option(name = "--password", description = "password of the PKCS#12 file")
  private String password;

  @Override
  protected SubjectPublicKeyInfo getPublicKey() throws Exception {
    return null;
  }

  @Override
  protected EnrollCertRequestEntry buildEnrollCertRequestEntry(String id, String profile,
      CertRequest certRequest) throws Exception {
    final boolean caGenKeypair = true;
    final boolean kup = false;
    return new EnrollCertRequestEntry("id-1", profile, certRequest, null, caGenKeypair, kup);
  }

  @Override
  protected Object execute0() throws Exception {
    EnrollCertResult result = enroll();

    X509Certificate cert = null;
    PrivateKeyInfo privateKeyInfo = null;
    if (result != null) {
      String id = result.getAllIds().iterator().next();
      CertifiedKeyPairOrError certOrError = result.getCertOrError(id);
      cert = (X509Certificate) certOrError.getCertificate();
      privateKeyInfo = certOrError.getPrivateKeyInfo();
    }

    if (cert == null) {
      throw new CmdFailure("no certificate received from the server");
    }

    if (privateKeyInfo == null) {
      throw new CmdFailure("no private key received from the server");
    }

    if (StringUtil.isNotBlank(certOutputFile)) {
      saveVerbose("saved certificate to file", certOutputFile,
          encodeCert(cert.getEncoded(), certOutform));
    }

    PrivateKey privateKey = BouncyCastleProvider.getPrivateKey(privateKeyInfo);

    KeyStore ks = KeyStore.getInstance("PKCS12");
    char[] pwd = getPassword();
    ks.load(null, pwd);
    ks.setKeyEntry("main", privateKey, pwd, new Certificate[] {cert});
    ByteArrayOutputStream bout = new ByteArrayOutputStream();
    ks.store(bout, pwd);
    saveVerbose("saved key to file", p12OutputFile, bout.toByteArray());

    return null;
  } // method execute0

  @Override
  protected Type getCmpReqType() throws Exception {
    if ("cr".equalsIgnoreCase(cmpreqType)) {
      return EnrollCertRequest.Type.CERT_REQ;
    } else if ("ir".equalsIgnoreCase(cmpreqType)) {
      return EnrollCertRequest.Type.INIT_REQ;
    } else {
      throw new IllegalCmdParamException("invalid cmpreq-type " + cmpreqType);
    }
  }

  private char[] getPassword() throws IOException {
    char[] pwdInChar = readPasswordIfNotSet(password);
    if (pwdInChar != null) {
      password = new String(pwdInChar);
    }
    return pwdInChar;
  }

}