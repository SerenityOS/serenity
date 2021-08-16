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
 * @summary This is an interop compatibility test on TLSv1.3 hello retry request.
 *
 * @library /test/lib
 *          ../TLSCommon
 *          ../TLSCommon/interop
 * @compile -source 1.8 -target 1.8
 *          JdkInfoUtils.java
 *          ../TLSCommon/interop/JdkProcServer.java
 *          ../TLSCommon/interop/JdkProcClient.java
 * @run main/manual HrrTest true
 * @run main/manual HrrTest false
 */

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import jdk.test.lib.security.CertUtils;

public class HrrTest extends ExtInteropTest {

    private JdkInfo serverJdkInfo;
    private JdkInfo clientJdkInfo;

    public HrrTest(JdkInfo serverJdkInfo, JdkInfo clientJdkInfo) {
        super(new Jdk(serverJdkInfo.version, serverJdkInfo.javaPath),
              new Jdk(clientJdkInfo.version, clientJdkInfo.javaPath));

        this.serverJdkInfo = serverJdkInfo;
        this.clientJdkInfo = clientJdkInfo;
    }

    @Override
    protected boolean skipExecute() {
        return super.skipExecute() || !supportsTLSv1_3();
    }

    private boolean supportsTLSv1_3() {
        boolean supported = true;

        if (!serverJdkInfo.enablesProtocol(Protocol.TLSV1_3)) {
            System.out.println("The server doesn't support TLSv1.3.");
            supported = false;
        }

        if (!clientJdkInfo.enablesProtocol(Protocol.TLSV1_3)) {
            System.out.println("The client doesn't support TLSv1.3.");
            supported = false;
        }

        return supported;
    }

    /*
     * It takes the server to support secp384r1 only, and the client to support
     * secp256r1 and secp384r1 in order, the server should respond hello retry
     * request message.
     * Please note that it has to specify the supported groups via property
     * jdk.tls.namedGroups for JSSE peers.
     */
    @Override
    protected List<TestCase<ExtUseCase>> getTestCases() {
        List<TestCase<ExtUseCase>> testCases = new ArrayList<>();
        for (CipherSuite cipherSuite : new CipherSuite[] {
                CipherSuite.TLS_AES_128_GCM_SHA256,
                CipherSuite.TLS_AES_256_GCM_SHA384,
                CipherSuite.TLS_CHACHA20_POLY1305_SHA256}) {
            testCases.add(createTestCase(cipherSuite));
        }
        return testCases;
    }

    private TestCase<ExtUseCase> createTestCase(CipherSuite cipherSuite) {
        Cert cert = new Cert(KeyAlgorithm.RSA, SignatureAlgorithm.RSA,
                HashAlgorithm.SHA256, CertUtils.RSA_CERT, CertUtils.RSA_KEY);
        CertTuple certTuple = new CertTuple(cert, cert);

        ExtUseCase serverCase = ExtUseCase.newInstance();
        serverCase.setCertTuple(certTuple);
        serverCase.setNamedGroups(NamedGroup.SECP384R1);

        ExtUseCase clientCase = ExtUseCase.newInstance();
        clientCase.setCertTuple(certTuple);
        clientCase.setProtocols(Protocol.TLSV1_3);
        clientCase.setCipherSuites(cipherSuite);
        clientCase.setNamedGroups(NamedGroup.SECP256R1, NamedGroup.SECP384R1);

        return new TestCase<ExtUseCase>(serverCase, clientCase);
    }

    @Override
    protected boolean ignoreTestCase(TestCase<ExtUseCase> testCase) {
        CipherSuite cipherSuite = testCase.clientCase.getCipherSuite();
        return !serverJdkInfo.enablesCipherSuite(cipherSuite)
                || !clientJdkInfo.supportsCipherSuite(cipherSuite);
    }

    @Override
    protected JdkProcServer.Builder createServerBuilder(ExtUseCase useCase)
            throws Exception {
        JdkProcServer.Builder builder = new JdkProcServer.Builder();
        builder.setJdk((Jdk) serverProduct);
        builder.setCertTuple(useCase.getCertTuple());
        builder.setProtocols(useCase.getProtocols());
        builder.setCipherSuites(useCase.getCipherSuites());
        builder.setClientAuth(useCase.isClientAuth());
        builder.setServerNames(useCase.getServerNames());
        builder.setAppProtocols(useCase.getAppProtocols());
        builder.setNamedGroups(useCase.getNamedGroups());
        return builder;
    }

    @Override
    protected JdkProcClient.Builder createClientBuilder(ExtUseCase useCase)
            throws Exception {
        JdkProcClient.Builder builder = new JdkProcClient.Builder();
        builder.setJdk((Jdk) clientProduct);
        builder.setCertTuple(useCase.getCertTuple());
        builder.setProtocols(useCase.getProtocols());
        builder.setCipherSuites(useCase.getCipherSuites());
        builder.setServerNames(useCase.getServerNames());
        builder.setAppProtocols(useCase.getAppProtocols());
        builder.setNamedGroups(useCase.getNamedGroups());
        return builder;
    }

    public static void main(String[] args) throws Exception {
        Boolean defaultJdkAsServer = Boolean.valueOf(args[0]);

        Set<JdkInfo> jdkInfos = Utils.jdkInfoList();
        for (JdkInfo jdkInfo : jdkInfos) {
            HrrTest test = new HrrTest(
                    defaultJdkAsServer ? JdkInfo.DEFAULT : jdkInfo,
                    defaultJdkAsServer ? jdkInfo : JdkInfo.DEFAULT);
            test.execute();
        }
    }
}
