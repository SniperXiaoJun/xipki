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

package org.xipki.ca.api.internal;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedDeque;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xipki.ca.api.profile.Certprofile;
import org.xipki.ca.api.profile.CertprofileFactory;
import org.xipki.ca.api.profile.CertprofileFactoryRegister;
import org.xipki.util.ObjectCreationException;
import org.xipki.util.ParamUtil;

/**
 * TODO.
 * @author Lijun Liao
 * @since 2.0.0
 */

public class CertprofileFactoryRegisterImpl implements CertprofileFactoryRegister {

  private static final Logger LOG = LoggerFactory.getLogger(CertprofileFactoryRegisterImpl.class);

  private ConcurrentLinkedDeque<CertprofileFactory> services = new ConcurrentLinkedDeque<>();

  @Override
  public Set<String> getSupportedTypes() {
    Set<String> types = new HashSet<>();
    for (CertprofileFactory service : services) {
      types.addAll(service.getSupportedTypes());
    }
    return Collections.unmodifiableSet(types);
  }

  @Override
  public boolean canCreateProfile(String type) {
    for (CertprofileFactory service : services) {
      if (service.canCreateProfile(type)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public Certprofile newCertprofile(String type) throws ObjectCreationException {
    ParamUtil.requireNonBlank("type", type);

    for (CertprofileFactory service : services) {
      if (service.canCreateProfile(type)) {
        return service.newCertprofile(type);
      }
    }

    throw new ObjectCreationException(
        "could not find factory to create Certprofile of type '" + type + "'");
  }

  public void bindService(CertprofileFactory service) {
    //might be null if dependency is optional
    if (service == null) {
      LOG.info("bindService invoked with null.");
      return;
    }

    boolean replaced = services.remove(service);
    services.add(service);

    String action = replaced ? "replaced" : "added";
    LOG.info("{} CertprofileFactory binding for {}", action, service);
  }

  public void unbindService(CertprofileFactory service) {
    //might be null if dependency is optional
    if (service == null) {
      LOG.debug("unbindService invoked with null.");
      return;
    }

    if (services.remove(service)) {
      LOG.info("removed CertprofileFactory binding for {}", service);
    } else {
      LOG.info("no CertprofileFactory binding found to remove for '{}'", service);
    }
  }

}
