/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

/*
 * The extended SSL/TLS communication parameters.
 */
public class ExtUseCase extends UseCase {

    // The server-acceptable SNI server name patterns;
    // Or the client-desired SNI server names.
    private String[] serverNames = null;

    // The supported application protocols.
    private String[] appProtocols = null;

    // The supported named groups.
    private NamedGroup[] namedGroups = null;

    public static ExtUseCase newInstance() {
        return new ExtUseCase();
    }

    public String[] getServerNames() {
        return serverNames;
    }

    public String getServerName() {
        return serverNames != null && serverNames.length > 0
                ? serverNames[0]
                : null;
    }

    public ExtUseCase setServerNames(String... serverNames) {
        this.serverNames = serverNames;
        return this;
    }

    public String[] getAppProtocols() {
        return appProtocols;
    }

    public String getAppProtocol() {
        return appProtocols != null && appProtocols.length > 0
                ? appProtocols[0]
                : null;
    }

    public ExtUseCase setAppProtocols(String... appProtocols) {
        this.appProtocols = appProtocols;
        return this;
    }

    public NamedGroup[] getNamedGroups() {
        return namedGroups;
    }

    public String getNamedGroup() {
        return namedGroups != null && namedGroups.length > 0
                ? appProtocols[0]
                : null;
    }

    public ExtUseCase setNamedGroups(NamedGroup... namedGroups) {
        this.namedGroups = namedGroups;
        return this;
    }

    @Override
    public String toString() {
        return Utilities.join(Utilities.PARAM_DELIMITER,
                super.toString(),
                Utilities.joinNameValue(
                        "serverNames", Utilities.join(serverNames)),
                Utilities.joinNameValue(
                        "appProtocols", Utilities.join(appProtocols)),
                Utilities.joinNameValue(
                        "NamedGroups", Utilities.join(namedGroups)));
    }
}
