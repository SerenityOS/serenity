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
 * @test
 * @summary This is an interop compatibility test on ALPN.
 *
 * @library /test/lib
 *          ../TLSCommon
 *          ../TLSCommon/interop
 * @compile -source 1.8 -target 1.8
 *          JdkInfoUtils.java
 *          ../TLSCommon/interop/JdkProcServer.java
 *          ../TLSCommon/interop/JdkProcClient.java
 * @run main/manual AlpnTest true
 * @run main/manual AlpnTest false
 */

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class AlpnTest extends ExtInteropTest {

    private JdkInfo serverJdkInfo;
    private JdkInfo clientJdkInfo;

    public AlpnTest(JdkInfo serverJdkInfo, JdkInfo clientJdkInfo) {
        super(new Jdk(serverJdkInfo.version, serverJdkInfo.javaPath),
              new Jdk(clientJdkInfo.version, clientJdkInfo.javaPath));

        this.serverJdkInfo = serverJdkInfo;
        this.clientJdkInfo = clientJdkInfo;
    }

    @Override
    protected boolean skipExecute() {
        return super.skipExecute() || !supportsALPN();
    }

    private boolean supportsALPN() {
        boolean supported = true;

        if (!serverJdkInfo.supportsALPN) {
            System.out.println("The server doesn't support ALPN.");
            supported = false;
        }

        if (!clientJdkInfo.supportsALPN) {
            System.out.println("The client doesn't support ALPN.");
            supported = false;
        }

        return supported;
    }

    @Override
    protected List<TestCase<ExtUseCase>> getTestCases() {
        List<TestCase<ExtUseCase>> testCases = new ArrayList<>();

        for (Protocol protocol : new Protocol[] {
                Protocol.TLSV1_2, Protocol.TLSV1_3 }) {
            for (CipherSuite cipherSuite : Utilities.ALL_CIPHER_SUITES) {
                if (!cipherSuite.supportedByProtocol(protocol)) {
                    continue;
                }

                Cert cert = Utils.getCert(cipherSuite.keyExAlgorithm);
                CertTuple certTuple = new CertTuple(cert, cert);

                ExtUseCase serverCase = ExtUseCase.newInstance();
                serverCase.setCertTuple(certTuple);
                serverCase.setAppProtocols("http/1.1", "h2");

                ExtUseCase clientCase = ExtUseCase.newInstance();
                clientCase.setCertTuple(certTuple);
                clientCase.setProtocols(protocol);
                clientCase.setCipherSuites(cipherSuite);
                clientCase.setAppProtocols("h2");

                testCases.add(
                        new TestCase<ExtUseCase>(serverCase, clientCase));
            }
        }

        return testCases;
    }

    @Override
    protected boolean ignoreTestCase(TestCase<ExtUseCase> testCase) {
        CipherSuite cipherSuite = testCase.clientCase.getCipherSuite();
        return !serverJdkInfo.enablesCipherSuite(cipherSuite)
                || !clientJdkInfo.supportsCipherSuite(cipherSuite);
    }

    @Override
    protected AbstractServer.Builder createServerBuilder(ExtUseCase useCase)
            throws Exception {
        return serverJdkInfo == JdkInfo.DEFAULT
               ? createJdkServerBuilder(useCase)
               : createAltJdkServerBuilder(useCase);
    }

    private JdkServer.Builder createJdkServerBuilder(ExtUseCase useCase) {
        JdkServer.Builder builder = new JdkServer.Builder();
        builder.setCertTuple(useCase.getCertTuple());
        builder.setProtocols(useCase.getProtocols());
        builder.setCipherSuites(useCase.getCipherSuites());
        builder.setAppProtocols(useCase.getAppProtocols());
        return builder;
    }

    private JdkProcServer.Builder createAltJdkServerBuilder(ExtUseCase useCase) {
        JdkProcServer.Builder builder = new JdkProcServer.Builder();
        builder.setJdk((Jdk) serverProduct);
        builder.setCertTuple(useCase.getCertTuple());
        builder.setProtocols(useCase.getProtocols());
        builder.setCipherSuites(useCase.getCipherSuites());
        builder.setAppProtocols(useCase.getAppProtocols());
        return builder;
    }

    @Override
    protected AbstractClient.Builder createClientBuilder(ExtUseCase useCase)
            throws Exception {
        return clientJdkInfo == JdkInfo.DEFAULT
               ? createJdkClientBuilder(useCase)
               : createAltJdkClientBuilder(useCase);
    }

    private JdkClient.Builder createJdkClientBuilder(ExtUseCase useCase) {
        JdkClient.Builder builder = new JdkClient.Builder();
        builder.setCertTuple(useCase.getCertTuple());
        builder.setProtocols(useCase.getProtocols());
        builder.setCipherSuites(useCase.getCipherSuites());
        builder.setAppProtocols(useCase.getAppProtocols());
        return builder;
    }

    private JdkProcClient.Builder createAltJdkClientBuilder(ExtUseCase useCase) {
        JdkProcClient.Builder builder = new JdkProcClient.Builder();
        builder.setJdk((Jdk) clientProduct);
        builder.setCertTuple(useCase.getCertTuple());
        builder.setProtocols(useCase.getProtocols());
        builder.setCipherSuites(useCase.getCipherSuites());
        builder.setAppProtocols(useCase.getAppProtocols());
        return builder;
    }

    public static void main(String[] args) throws Exception {
        Boolean defaultJdkAsServer = Boolean.valueOf(args[0]);

        Set<JdkInfo> jdkInfos = Utils.jdkInfoList();
        for (JdkInfo jdkInfo : jdkInfos) {
            AlpnTest test = new AlpnTest(
                    defaultJdkAsServer ? JdkInfo.DEFAULT : jdkInfo,
                    defaultJdkAsServer ? jdkInfo : JdkInfo.DEFAULT);
            test.execute();
        }
    }
}
