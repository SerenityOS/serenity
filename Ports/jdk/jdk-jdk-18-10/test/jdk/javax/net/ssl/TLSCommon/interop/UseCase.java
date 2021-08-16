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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/*
 * A set of specific SSL/TLS communication parameters on a peer.
 */
public class UseCase {

 // The tuple for trusted CAs and certificates.
    private CertTuple certTuple;

    // The supported SSL/TLS protocols.
    private Protocol[] protocols;

    // The supported cipher suites.
    private CipherSuite[] cipherSuites;

    // If require client authentication.
    private boolean clientAuth;

    public static UseCase newInstance() {
        return new UseCase();
    }

    public CertTuple getCertTuple() {
        return certTuple;
    }

    public UseCase setCertTuple(CertTuple certTuple) {
        this.certTuple = certTuple;
        return this;
    }

    public Protocol[] getProtocols() {
        return protocols;
    }

    public Protocol getProtocol() {
        return protocols != null && protocols.length > 0
                ? protocols[0]
                : null;
    }

    public UseCase setProtocols(Protocol... protocols) {
        this.protocols = protocols;
        return this;
    }

    public CipherSuite[] getCipherSuites() {
        return cipherSuites;
    }

    public CipherSuite getCipherSuite() {
        return cipherSuites != null && cipherSuites.length > 0
                ? cipherSuites[0]
                : null;
    }

    public UseCase setCipherSuites(CipherSuite... cipherSuites) {
        this.cipherSuites = cipherSuites;
        return this;
    }

    public boolean isClientAuth() {
        return clientAuth;
    }

    public UseCase setClientAuth(boolean clientAuth) {
        this.clientAuth = clientAuth;
        return this;
    }

    // The system properties used by a JDK peer.
    private final Map<String, String> props = new HashMap<>();

    public UseCase addProp(String prop, String value) {
        props.put(prop, value);
        return this;
    }

    public String getProp(String prop) {
        return props.get(prop);
    }

    public UseCase addAllProps(Map<String, String> props) {
        this.props.putAll(props);
        return this;
    }

    public Map<String, String> getAllProps() {
        return Collections.unmodifiableMap(props);
    }

    public UseCase removeProp(String prop) {
        props.remove(prop);
        return this;
    }

    public UseCase removeAllProps() {
        props.clear();
        return this;
    }

    @Override
    public String toString() {
        return Utilities.join(Utilities.PARAM_DELIMITER,
                "certTuple=[" + certTuple + "]",
                Utilities.joinNameValue("protocols", Utilities.join(protocols)),
                Utilities.joinNameValue("cipherSuites", Utilities.join(cipherSuites)),
                Utilities.joinNameValue("clientAuth", clientAuth ? "true" : ""));
    }
}
