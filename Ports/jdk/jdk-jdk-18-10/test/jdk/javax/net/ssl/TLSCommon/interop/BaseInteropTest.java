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

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/*
 * The base interop test on SSL/TLS communication.
 */
public abstract class BaseInteropTest<U extends UseCase> {

    protected final Product serverProduct;
    protected final Product clientProduct;
    private static final int MAX_SERVER_RETRIES = 3;

    public BaseInteropTest(Product serverProduct, Product clientProduct) {
        this.serverProduct = serverProduct;
        this.clientProduct = clientProduct;
    }

    public boolean isJdkClient() {
        return Jdk.DEFAULT.equals(clientProduct);
    }

    /*
     * This main entrance of the test execution.
     */
    protected void execute() throws Exception {
        System.out.printf("Server: %s%nClient: %s%n",
                serverProduct, clientProduct);

        if (skipExecute()) {
            System.out.println("This execution was skipped.");
            return;
        }

        List<TestCase<U>> testCases = null;

        Path logPath = getLogPath();
        if (logPath != null) {
            System.out.println("Log: " + logPath);

            PrintStream origStdOut = System.out;
            PrintStream origStdErr = System.err;
            try (PrintStream printStream = new PrintStream(
                    new FileOutputStream(logPath.toFile()))) {
                System.setOut(printStream);
                System.setErr(printStream);

                testCases = runTest();
            } finally {
                System.setOut(origStdOut);
                System.setErr(origStdErr);
            }
        } else {
            testCases = runTest();
        }

        boolean fail = false;
        System.out.println("########## Failed Cases Start ##########");
        for (TestCase<U> testCase : testCases) {
            if (testCase.getStatus() == Status.FAIL) {
                System.out.println("--------------------");
                System.out.println(testCase);
                System.out.println("--------------------");
                fail = true;
            }
        }
        System.out.println("########## Failed Cases End ##########");

        if (fail) {
            throw new RuntimeException(
                    "At least one case failed! Please check log for details.");
        } else {
            System.out.println("This test passed!");
        }
    }

    /*
     * If either of server and client products is unavailable,
     * just skip this test execution.
     */
    protected boolean skipExecute() {
        return serverProduct.getPath() == null || clientProduct.getPath() == null;
    }

    /*
     * Returns the log path.
     * If null, no output will be redirected to local file.
     */
    protected Path getLogPath() {
        return Utilities.LOG_PATH == null
                ? null : Paths.get(Utilities.LOG_PATH);
    }

    /*
     * Provides a default set of test cases for testing.
     */
    protected abstract List<TestCase<U>> getTestCases();

    /*
     * Checks if test case should be ignored.
     */
    protected boolean ignoreTestCase(TestCase<U> testCase) {
        return false;
    }

    /*
     * Runs all test cases with the specified products as server and client
     * respectively.
     */
    protected List<TestCase<U>> runTest() throws Exception {
        List<TestCase<U>> executedTestCases = new ArrayList<>();

        List<TestCase<U>> testCases = getTestCases();
        for (TestCase<U> testCase : testCases) {
            System.out.println("========== Case Start ==========");
            System.out.println(testCase);

            if (!ignoreTestCase(testCase)) {
                Status status = runTestCase(testCase);
                testCase.setStatus(status);

                executedTestCases.add(testCase);
            } else {
                System.out.println("Ignored");
            }

            System.out.println("========== Case End ==========");
        }

        return executedTestCases;
    }

    /*
     * Runs a specific test case.
     */
    protected Status runTestCase(TestCase<U> testCase) throws Exception {
        Status serverStatus = Status.UNSTARTED;
        Status clientStatus = Status.UNSTARTED;

        ExecutorService executor = Executors.newFixedThreadPool(1);
        AbstractServer server = null;
        try {
            server = startAndGetServer(testCase.serverCase, executor);
            int port = server.getPort();
            System.out.println("Server is listening " + port);
            serverStatus = Status.PASS;

            try (AbstractClient client = createClient(testCase.clientCase)) {
                client.connect("localhost", port);
                clientStatus = Status.PASS;

                if (testCase.clientCase instanceof ExtUseCase) {
                    ExtUseCase serverCase = (ExtUseCase) testCase.serverCase;
                    ExtUseCase clientCase = (ExtUseCase) testCase.clientCase;

                    String[] clientAppProtocols = clientCase.getAppProtocols();
                        if (clientAppProtocols != null && clientAppProtocols.length > 0) {
                        String expectedNegoAppProtocol = Utilities.expectedNegoAppProtocol(
                                serverCase.getAppProtocols(),
                                clientAppProtocols);
                        System.out.println("Expected negotiated app protocol: "
                                + expectedNegoAppProtocol);
                        String negoAppProtocol = getNegoAppProtocol(server, client);
                        System.out.println(
                                "Actual negotiated app protocol: " + negoAppProtocol);
                        if (!Utilities.trimStr(negoAppProtocol).equals(
                                Utilities.trimStr(expectedNegoAppProtocol))) {
                            System.out.println(
                                    "Negotiated app protocol is unexpected");
                            clientStatus = Status.FAIL;
                        }
                    }
                }
            } catch (Exception exception) {
                clientStatus = handleClientException(exception);
            }
        } catch (Exception exception) {
            serverStatus = handleServerException(exception);
        } finally {
            if (server != null) {
                server.signalStop();
                server.close();
            }

            executor.shutdown();
        }

        Status caseStatus
                = serverStatus == Status.PASS && clientStatus == Status.PASS
                  ? Status.PASS
                  : Status.FAIL;
        System.out.printf(
                "ServerStatus=%s, ClientStatus=%s, CaseStatus=%s%n",
                serverStatus, clientStatus, caseStatus);
        return caseStatus;
    }

    /*
     * Return a server once it is properly started to avoid client connection issues.
     * Retry operation if needed, server may fail to bind a port
     */
    protected AbstractServer startAndGetServer(U useCase, ExecutorService executor)
            throws Exception {
        int maxRetries = getServerMaxRetries();
        boolean serverAlive;
        AbstractServer server;

        do {
            server = createServer(useCase, executor);
            serverAlive = Utilities.waitFor(Server::isAlive, server);
            if (!serverAlive) {
                server.signalStop();
            }

            maxRetries--;
        } while (!serverAlive && maxRetries > 0);

        if (!serverAlive) {
            throw new RuntimeException("Server failed to start");
        }

        return server;
    }

    /*
     * Handles server side exception, and determines the status.
     */
    protected Status handleServerException(Exception exception) {
        return handleException(exception);
    }

    /*
     * Handles client side exception, and determines the status.
     */
    protected Status handleClientException(Exception exception) {
        return handleException(exception);
    }

    private Status handleException(Exception exception) {
        if (exception == null) {
            return Status.PASS;
        }

        exception.printStackTrace(System.out);
        return Status.FAIL;
    }

    /*
     * Creates server.
     */
    protected AbstractServer createServer(U useCase, ExecutorService executor) throws Exception {
        AbstractServer server = createServerBuilder(useCase).build();
        executor.submit(new ServerTask(server));
        return server;
    }

    protected AbstractServer.Builder createServerBuilder(U useCase)
            throws Exception {
        return (JdkServer.Builder) ((JdkServer.Builder) new JdkServer.Builder()
                .setProtocols(useCase.getProtocols())
                .setCipherSuites(useCase.getCipherSuites())
                .setCertTuple(useCase.getCertTuple()))
                .setClientAuth(useCase.isClientAuth());
    }

    /*
     * Creates client.
     */
    protected AbstractClient createClient(U useCase) throws Exception {
        return createClientBuilder(useCase).build();
    }

    protected AbstractClient.Builder createClientBuilder(U useCase)
            throws Exception {
        return (JdkClient.Builder) new JdkClient.Builder()
                .setProtocols(useCase.getProtocols())
                .setCipherSuites(useCase.getCipherSuites())
                .setCertTuple(useCase.getCertTuple());
    }

    /*
     * Returns the maximum number of attempts to start a server.
     */
    protected int getServerMaxRetries() {
        return MAX_SERVER_RETRIES;
    }

    /*
     * Determines the negotiated application protocol.
     * Generally, using JDK client to get this value.
     */
    protected String getNegoAppProtocol(AbstractServer server,
            AbstractClient client) throws SSLTestException {
        return isJdkClient() ? client.getNegoAppProtocol()
                             : server.getNegoAppProtocol();
    }

    protected static class ServerTask implements Callable<Void> {

        private final AbstractServer server;

        protected ServerTask(AbstractServer server) {
            this.server = server;
        }

        @Override
        public Void call() throws IOException {
            server.accept();
            return null;
        }
    }

    protected static class ClientTask implements Callable<Exception> {

        private final AbstractClient client;

        private final String host;
        private final int port;

        protected ClientTask(AbstractClient client, String host, int port) {
            this.client = client;

            this.host = host;
            this.port = port;
        }

        protected ClientTask(AbstractClient client, int port) {
            this(client, "localhost", port);
        }

        @Override
        public Exception call() {
            try (AbstractClient c = client) {
                c.connect(host, port);
            } catch (Exception exception) {
                return exception;
            }

            return null;
        }
    }
}
