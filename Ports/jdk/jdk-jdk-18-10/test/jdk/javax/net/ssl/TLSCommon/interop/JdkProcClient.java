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
import java.nio.file.Paths;
import java.security.Security;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 * A JDK client process.
 */
public class JdkProcClient extends AbstractClient {

    private final Jdk jdk;
    private final Map<String, String> props = new HashMap<>();

    private Process process;

    public JdkProcClient(Builder builder) {
        this.jdk = builder.getJdk();

        if (builder.getSecPropsFile() != null) {
            props.put(JdkProcUtils.PROP_SEC_PROPS_FILE,
                    builder.getSecPropsFile().toString());
        }

        if (builder.getCertTuple() != null) {
            props.put(JdkProcUtils.PROP_TRUSTED_CERTS,
                    JdkProcUtils.certsToStr(builder.getCertTuple().trustedCerts));
            props.put(JdkProcUtils.PROP_EE_CERTS,
                    JdkProcUtils.certsToStr(builder.getCertTuple().endEntityCerts));
        }

        if (builder.getProtocols() != null) {
            props.put(JdkProcUtils.PROP_PROTOCOLS,
                    Utilities.join(Utilities.enumsToStrs(builder.getProtocols())));
        }

        if (builder.getCipherSuites() != null) {
            props.put(JdkProcUtils.PROP_CIPHER_SUITES,
                    Utilities.join(Utilities.enumsToStrs(builder.getCipherSuites())));
        }

        if (builder.getServerNames() != null) {
            props.put(JdkProcUtils.PROP_SERVER_NAMES,
                    Utilities.join(builder.getServerNames()));
        }

        if (builder.getAppProtocols() != null) {
            props.put(JdkProcUtils.PROP_APP_PROTOCOLS,
                    Utilities.join(builder.getAppProtocols()));
        }

        if (builder.getNamedGroups() != null) {
            props.put(JdkProcUtils.PROP_NAMED_GROUPS,
                    Utilities.join(Utilities.namedGroupsToStrs(
                            builder.getNamedGroups())));
        }

        props.put("test.src", Utilities.TEST_SRC);
        if (Utilities.DEBUG) {
            props.put("javax.net.debug", "all");
        }
    }

    public static class Builder extends AbstractClient.Builder {

        private Jdk jdk;

        private Path secPropsFile;

        public Jdk getJdk() {
            return jdk;
        }

        public Builder setJdk(Jdk jdk) {
            this.jdk = jdk;
            return this;
        }

        public Path getSecPropsFile() {
            return secPropsFile;
        }

        public Builder setSecPropsFile(Path secPropsFile) {
            this.secPropsFile = secPropsFile;
            return this;
        }

        @Override
        public JdkProcClient build() {
            return new JdkProcClient(this);
        }
    }

    @Override
    public Jdk getProduct() {
        return jdk;
    }

    @Override
    public void connect(String host, int port) throws IOException {
        props.put(JdkProcUtils.PROP_HOST, host);
        props.put(JdkProcUtils.PROP_PORT, port + "");

        process = JdkProcUtils.java(getProduct().getPath(), getClass(), props,
                getLogPath());
        try {
            process.waitFor();
        } catch (InterruptedException e) {
            throw new RuntimeException("Client was interrupted!", e);
        }

        if (process.exitValue() != 0) {
            throw new SSLTestException("Client exited abnormally!");
        }
    }

    @Override
    protected Path getLogPath() {
        return Paths.get("client.log");
    }

    @Override
    public void close() throws IOException {
        printLog();
        deleteLog();
    }

    public static void main(String[] args) throws Exception {
        String trustedCertsStr = System.getProperty(JdkProcUtils.PROP_TRUSTED_CERTS);
        String eeCertsStr = System.getProperty(JdkProcUtils.PROP_EE_CERTS);

        String protocolsStr = System.getProperty(JdkProcUtils.PROP_PROTOCOLS);
        String cipherSuitesStr = System.getProperty(JdkProcUtils.PROP_CIPHER_SUITES);

        String serverNamesStr = System.getProperty(JdkProcUtils.PROP_SERVER_NAMES);
        String appProtocolsStr = System.getProperty(JdkProcUtils.PROP_APP_PROTOCOLS);

        // Re-enable TLSv1 and TLSv1.1 since client depends on them
        removeFromDisabledTlsAlgs("TLSv1", "TLSv1.1");

        JdkClient.Builder builder = new JdkClient.Builder();
        builder.setCertTuple(JdkProcUtils.createCertTuple(
                trustedCertsStr, eeCertsStr));
        if (!Utilities.isEmpty(protocolsStr)) {
            builder.setProtocols(Utilities.strToEnums(
                    Protocol.class, protocolsStr));
        }
        if (!Utilities.isEmpty(cipherSuitesStr)) {
            builder.setCipherSuites(Utilities.strToEnums(
                    CipherSuite.class, cipherSuitesStr));
        }
        if (!Utilities.isEmpty(serverNamesStr)) {
            builder.setServerNames(Utilities.split(serverNamesStr));
        }
        if (!Utilities.isEmpty(appProtocolsStr)) {
            builder.setAppProtocols(Utilities.split(appProtocolsStr));
        }

        String host = System.getProperty(JdkProcUtils.PROP_HOST);
        int port = Integer.getInteger(JdkProcUtils.PROP_PORT);

        try(JdkClient client = builder.build()) {
            client.connect(host, port);
        }
    }

    /**
     * Removes the specified protocols from the jdk.tls.disabledAlgorithms
     * security property.
     */
    private static void removeFromDisabledTlsAlgs(String... algs) {
        List<String> algList = Arrays.asList(algs);
        String value = Security.getProperty("jdk.tls.disabledAlgorithms");
        StringBuilder newValue = new StringBuilder();
        for (String constraint : value.split(",")) {
            String tmp = constraint.trim();
            if (!algList.contains(tmp)) {
                newValue.append(tmp);
                newValue.append(",");
            }
        }
        Security.setProperty("jdk.tls.disabledAlgorithms", newValue.toString());
    }
}
