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

package org.xipki.security.speed.pkcs11;

import org.xipki.security.pkcs11.P11IdentityId;
import org.xipki.security.pkcs11.P11Slot;
import org.xipki.util.ParamUtil;

/**
 * TODO.
 * @author Lijun Liao
 * @since 2.0.0
 */
// CHECKSTYLE:SKIP
public class P11ECKeyGenSpeed extends P11KeyGenSpeed {

  private final String curveNameOrOid;

  public P11ECKeyGenSpeed(P11Slot slot, byte[] id, String curveNameOrOid) throws Exception {
    super(slot, id, "PKCS#11 EC key generation\ncurve: " + curveNameOrOid);
    this.curveNameOrOid = ParamUtil.requireNonNull("curveNameOrOid", curveNameOrOid);
  }

  @Override
  protected void genKeypair() throws Exception {
    P11IdentityId objId = slot.generateECKeypair(curveNameOrOid, getControl());
    slot.removeIdentity(objId);
  }

}
