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

package org.xipki.ca.qa.shell;

import java.rmi.UnexpectedException;
import java.util.Collections;
import java.util.Set;

import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.xipki.ca.server.mgmt.api.CaHasRequestorEntry;
import org.xipki.ca.server.mgmt.api.CaManager;
import org.xipki.ca.server.mgmt.shell.CaAction;
import org.xipki.ca.server.mgmt.shell.ShellUtil;
import org.xipki.ca.server.mgmt.shell.completer.CaNameCompleter;
import org.xipki.ca.server.mgmt.shell.completer.PermissionCompleter;
import org.xipki.ca.server.mgmt.shell.completer.ProfileNameAndAllCompleter;
import org.xipki.ca.server.mgmt.shell.completer.RequestorNameCompleter;
import org.xipki.shell.CmdFailure;
import org.xipki.shell.completer.YesNoCompleter;

/**
 * TODO.
 * @author Lijun Liao
 * @since 2.0.0
 */

@Command(scope = "caqa", name = "careq-check",
    description = "check information of requestors in CA (QA)")
@Service
public class CaRequestorCheckAction extends CaAction {

  @Option(name = "--ca", required = true, description = "CA name")
  @Completion(CaNameCompleter.class)
  private String caName;

  @Option(name = "--requestor", required = true, description = "requestor name")
  @Completion(RequestorNameCompleter.class)
  private String requestorName;

  @Option(name = "--ra", description = "whether as RA")
  @Completion(YesNoCompleter.class)
  private String raS = "no";

  @Option(name = "--permission", multiValued = true, description = "permission")
  @Completion(PermissionCompleter.class)
  private Set<String> permissions;

  @Option(name = "--profile", multiValued = true,
      description = "profile name or 'all' for all profiles, and 'null' for no profiles")
  @Completion(ProfileNameAndAllCompleter.class)
  private Set<String> profiles;

  @Override
  protected Object execute0() throws Exception {
    println("checking CA requestor CA='" + caName + "', requestor='" + requestorName + "'");

    if (caManager.getCa(caName) == null) {
      throw new UnexpectedException("could not find CA '" + caName + "'");
    }

    Set<CaHasRequestorEntry> entries = caManager.getRequestorsForCa(caName);
    CaHasRequestorEntry entry = null;
    String upRequestorName = requestorName.toLowerCase();
    for (CaHasRequestorEntry m : entries) {
      if (m.getRequestorIdent().getName().equals(upRequestorName)) {
        entry = m;
        break;
      }
    }

    if (entry == null) {
      throw new CmdFailure("CA is not associated with requestor '" + requestorName + "'");
    }

    boolean ra = isEnabled(raS, false, "ra");
    boolean bo = entry.isRa();
    if (ra != bo) {
      throw new CmdFailure("ra: is '" + bo + "', expected '" + ra + "'");
    }

    if (permissions != null) {
      int intPermission = ShellUtil.getPermission(permissions);

      if (intPermission != entry.getPermission()) {
        throw new CmdFailure("permissions: is '" + entry.getPermission()
            + "', but expected '" + intPermission + "'");
      }
    }

    if (profiles != null) {
      if (profiles.size() == 1) {
        if (CaManager.NULL.equalsIgnoreCase(profiles.iterator().next())) {
          profiles = Collections.emptySet();
        }
      }

      if (!profiles.equals(entry.getProfiles())) {
        throw new CmdFailure("profiles: is '" + entry.getProfiles()
            + "', but expected '" + profiles + "'");
      }
    }

    println(" checked CA requestor CA='" + caName + "', requestor='" + requestorName + "'");
    return null;
  } // method execute0

}
