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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/*
 * A JDK server process.
 */
public class JdkProcServer extends AbstractServer {

    public static final Path PORT_LOG = Paths.get("port.log");

    private final Jdk jdk;
    private final Map<String, String> props = new HashMap<>();

    private Process process;

    public JdkProcServer(Builder builder) throws Exception {
        jdk = builder.getJdk();

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

        props.put(JdkProcUtils.PROP_CLIENT_AUTH,
                String.valueOf(builder.getClientAuth()));

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

    public static class Builder extends AbstractServer.Builder {

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
        public JdkProcServer build() throws Exception {
            return new JdkProcServer(this);
        }
    }

    @Override
    public Product getProduct() {
        return jdk;
    }

    @Override
    public int getPort() throws IOException {
        System.out.println("Waiting for port log...");
        if (!Utilities.waitFor(server -> server.isAlive() && readPort() > 0, this)) {
            throw new RuntimeException("Server doesn't start in time.");
        }

        return readPort();
    }

    @Override
    public boolean isAlive() {
        return Utilities.isAliveProcess(process);
    }

    @Override
    public void accept() throws IOException {
        process = JdkProcUtils.java(getProduct().getPath(), getClass(), props,
                getLogPath());
        try {
            process.waitFor();
        } catch (InterruptedException e) {
            throw new RuntimeException("Server was interrupted!", e);
        }

        if (process.exitValue() != 0) {
            throw new SSLTestException("Server exited abnormally!");
        }
    }

    @Override
    public void signalStop() {
        if (isAlive()) {
            Utilities.destroyProcess(process);
        }
    }

    @Override
    public void close() throws IOException {
        printLog();
        deletePort();
        deleteLog();
    }

    private static int readPort() {
        try {
            return Integer.valueOf(new String(Files.readAllBytes(PORT_LOG)));
        } catch (Exception e) {
            return 0;
        }
    }

    private static void deletePort() throws IOException {
        Utilities.deleteFile(PORT_LOG);
    }

    private static void savePort(int port) throws IOException {
        Files.write(PORT_LOG, String.valueOf(port).getBytes(Utilities.CHARSET));
    }

    public static void main(String[] args) throws Exception {
        String trustedCertsStr = System.getProperty(JdkProcUtils.PROP_TRUSTED_CERTS);
        String eeCertsStr = System.getProperty(JdkProcUtils.PROP_EE_CERTS);

        String protocolsStr = System.getProperty(JdkProcUtils.PROP_PROTOCOLS);
        String cipherSuitesStr = System.getProperty(JdkProcUtils.PROP_CIPHER_SUITES);

        boolean clientAuth = Boolean.getBoolean(JdkProcUtils.PROP_CLIENT_AUTH);
        String serverNamesStr = System.getProperty(JdkProcUtils.PROP_SERVER_NAMES);
        String appProtocolsStr = System.getProperty(JdkProcUtils.PROP_APP_PROTOCOLS);

        JdkServer.Builder builder = new JdkServer.Builder();
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
        builder.setClientAuth(clientAuth);
        if (!Utilities.isEmpty(serverNamesStr)) {
            builder.setServerNames(Utilities.split(serverNamesStr));
        }
        if (!Utilities.isEmpty(appProtocolsStr)) {
            builder.setAppProtocols(Utilities.split(appProtocolsStr));
        }

        try (JdkServer server = builder.build()) {
            int port = server.getPort();
            System.out.println("port=" + port);
            savePort(port);
            server.accept();
        }
    }
}
