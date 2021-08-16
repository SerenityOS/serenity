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

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/*
 * Abstract peer implementation.
 */
public abstract class AbstractPeer implements Peer {

    /*
     * Peer's log path. The path could be null.
     */
    protected Path getLogPath() {
        return null;
    }

    /*
     * Prints the content of log file to stdout.
     */
    protected void printLog() throws IOException {
        Path logPath = getLogPath();
        Objects.requireNonNull(logPath, "Please specify log path.");
        System.out.println(Utilities.readFile(logPath).orElse(""));
    }

    /*
     * Deletes log file if exists.
     */
    protected void deleteLog() throws IOException {
        Utilities.deleteFile(getLogPath());
    }

    /*
     * The negotiated application protocol.
     */
    public String getNegoAppProtocol() throws SSLTestException {
        throw new UnsupportedOperationException("getNegoAppProtocol");
    }

    @Override
    public void close() throws IOException { }

    public static abstract class Builder implements Peer.Builder {

        // The supported SSL/TLS protocols.
        private Protocol[] protocols;

        // The supported cipher suites.
        private CipherSuite[] cipherSuites;

        // The trusted CAs and certificates.
        private CertTuple certTuple;

        // The server-acceptable SNI server name patterns;
        // Or the client-desired SNI server names.
        private String[] serverNames;

        // The supported application protocols.
        private String[] appProtocols;

        // The supported named groups.
        private NamedGroup[] namedGroups;

        private String message = "M";

        private int timeout = Utilities.TIMEOUT;

        // System properties
        private final Map<String, String> props = new HashMap<>();

        public Protocol[] getProtocols() {
            return protocols;
        }

        public Protocol getProtocol() {
            return protocols != null && protocols.length > 0
                    ? protocols[0]
                    : null;
        }

        public Builder setProtocols(Protocol... protocols) {
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

        public Builder setCipherSuites(CipherSuite... cipherSuites) {
            this.cipherSuites = cipherSuites;
            return this;
        }

        public CertTuple getCertTuple() {
            return certTuple;
        }

        public Builder setCertTuple(CertTuple certTuple) {
            this.certTuple = certTuple;
            return this;
        }

        public String[] getServerNames() {
            return serverNames;
        }

        public String getServerName() {
            return serverNames != null && serverNames.length > 0
                    ? serverNames[0]
                    : null;
        }

        public Builder setServerNames(String... serverNames) {
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

        public Builder setAppProtocols(String... appProtocols) {
            this.appProtocols = appProtocols;
            return this;
        }

        public NamedGroup[] getNamedGroups() {
            return namedGroups;
        }

        public Builder setNamedGroups(NamedGroup... namedGroups) {
            this.namedGroups = namedGroups;
            return this;
        }

        public String getMessage() {
            return message;
        }

        public Builder setMessage(String message) {
            this.message = message;
            return this;
        }

        public int getTimeout() {
            return timeout;
        }

        public Builder setTimeout(int timeout) {
            this.timeout = timeout;
            return this;
        }

        public Builder addProp(String prop, String value) {
            props.put(prop, value);
            return this;
        }

        public String getProp(String prop) {
            return props.get(prop);
        }

        public Builder addAllProps(Map<String, String> props) {
            this.props.putAll(props);
            return this;
        }

        public Map<String, String> getAllProps() {
            return Collections.unmodifiableMap(props);
        }

        public abstract AbstractPeer build() throws Exception;
    }
}
