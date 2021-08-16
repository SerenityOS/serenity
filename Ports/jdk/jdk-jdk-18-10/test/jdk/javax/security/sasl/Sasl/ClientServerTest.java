/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

import java.io.Closeable;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.sasl.AuthorizeCallback;
import javax.security.sasl.RealmCallback;
import javax.security.sasl.RealmChoiceCallback;
import javax.security.sasl.Sasl;
import javax.security.sasl.SaslClient;
import javax.security.sasl.SaslException;
import javax.security.sasl.SaslServer;

/*
 * @test
 * @bug 8049814
 * @summary JAVA SASL server and client tests with CRAM-MD5 and
 *          DIGEST-MD5 mechanisms. The tests try different QOP values on
 *          client and server side.
 * @modules java.security.sasl/javax.security.sasl
 */
public class ClientServerTest {

    private static final int DELAY = 100;
    private static final String LOCALHOST = "localhost";
    private static final String DIGEST_MD5 = "DIGEST-MD5";
    private static final String CRAM_MD5 = "CRAM-MD5";
    private static final String PROTOCOL = "saslservice";
    private static final String USER_ID = "sasltester";
    private static final String PASSWD = "password";
    private static final String QOP_AUTH = "auth";
    private static final String QOP_AUTH_CONF = "auth-conf";
    private static final String QOP_AUTH_INT = "auth-int";
    private static final String AUTHID_SASL_TESTER = "sasl_tester";
    private static final ArrayList<String> SUPPORT_MECHS = new ArrayList<>();

    static {
        SUPPORT_MECHS.add(DIGEST_MD5);
        SUPPORT_MECHS.add(CRAM_MD5);
    }

    public static void main(String[] args) throws Exception {
        String[] allQops = { QOP_AUTH_CONF, QOP_AUTH_INT, QOP_AUTH };
        String[] twoQops = { QOP_AUTH_INT, QOP_AUTH };
        String[] authQop = { QOP_AUTH };
        String[] authIntQop = { QOP_AUTH_INT };
        String[] authConfQop = { QOP_AUTH_CONF };
        String[] emptyQop = {};

        boolean success = true;

        success &= runTest("", CRAM_MD5, new String[] { QOP_AUTH },
                new String[] { QOP_AUTH }, false);
        success &= runTest("", DIGEST_MD5, new String[] { QOP_AUTH },
                new String[] { QOP_AUTH }, false);
        success &= runTest(AUTHID_SASL_TESTER, DIGEST_MD5,
                new String[] { QOP_AUTH }, new String[] { QOP_AUTH }, false);
        success &= runTest("", DIGEST_MD5, allQops, authQop, false);
        success &= runTest("", DIGEST_MD5, allQops, authIntQop, false);
        success &= runTest("", DIGEST_MD5, allQops, authConfQop, false);
        success &= runTest("", DIGEST_MD5, twoQops, authQop, false);
        success &= runTest("", DIGEST_MD5, twoQops, authIntQop, false);
        success &= runTest("", DIGEST_MD5, twoQops, authConfQop, true);
        success &= runTest("", DIGEST_MD5, authIntQop, authQop, true);
        success &= runTest("", DIGEST_MD5, authConfQop, authQop, true);
        success &= runTest("", DIGEST_MD5, authConfQop, emptyQop, true);
        success &= runTest("", DIGEST_MD5, authIntQop, emptyQop, true);
        success &= runTest("", DIGEST_MD5, authQop, emptyQop, true);

        if (!success) {
            throw new RuntimeException("At least one test case failed");
        }

        System.out.println("Test passed");
    }

    private static boolean runTest(String authId, String mech,
            String[] clientQops, String[] serverQops, boolean expectException)
            throws Exception {

        System.out.println("AuthId:" + authId
                + " mechanism:" + mech
                + " clientQops: " + Arrays.toString(clientQops)
                + " serverQops: " + Arrays.toString(serverQops)
                + " expect exception:" + expectException);

        try (Server server = Server.start(LOCALHOST, authId, serverQops)) {
            new Client(LOCALHOST, server.getPort(), mech, authId, clientQops)
                    .run();
            if (expectException) {
                System.out.println("Expected exception not thrown");
                return false;
            }
        } catch (SaslException e) {
            if (!expectException) {
                System.out.println("Unexpected exception: " + e);
                return false;
            }
            System.out.println("Expected exception: " + e);
        }

        return true;
    }

    static enum SaslStatus {
        SUCCESS, FAILURE, CONTINUE
    }

    static class Message implements Serializable {

        private final SaslStatus status;
        private final byte[] data;

        public Message(SaslStatus status, byte[] data) {
            this.status = status;
            this.data = data;
        }

        public SaslStatus getStatus() {
            return status;
        }

        public byte[] getData() {
            return data;
        }
    }

    static class SaslPeer {

        final String host;
        final String mechanism;
        final String qop;
        final CallbackHandler callback;

        SaslPeer(String host, String authId, String... qops) {
            this(host, null, authId, qops);
        }

        SaslPeer(String host, String mechanism, String authId, String... qops) {
            this.host = host;
            this.mechanism = mechanism;

            StringJoiner sj = new StringJoiner(",");
            for (String q : qops) {
                sj.add(q);
            }
            qop = sj.toString();

            callback = new TestCallbackHandler(USER_ID, PASSWD, host, authId);
        }

        Message getMessage(Object ob) {
            if (!(ob instanceof Message)) {
                throw new RuntimeException("Expected an instance of Message");
            }
            return (Message) ob;
        }
    }

    static class Server extends SaslPeer implements Runnable, Closeable {

        private volatile boolean ready = false;
        private volatile ServerSocket ssocket;

        static Server start(String host, String authId, String[] serverQops)
                throws UnknownHostException {
            Server server = new Server(host, authId, serverQops);
            Thread thread = new Thread(server);
            thread.setDaemon(true);
            thread.start();

            while (!server.ready) {
                try {
                    Thread.sleep(DELAY);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

            return server;
        }

        Server(String host, String authId, String... qops) {
            super(host, authId, qops);
        }

        int getPort() {
            return ssocket.getLocalPort();
        }

        private void processConnection(SaslEndpoint endpoint)
                throws SaslException, IOException, ClassNotFoundException {
            System.out.println("process connection");
            endpoint.send(SUPPORT_MECHS);
            Object o = endpoint.receive();
            if (!(o instanceof String)) {
                throw new RuntimeException("Received unexpected object: " + o);
            }
            String mech = (String) o;
            SaslServer saslServer = createSaslServer(mech);
            Message msg = getMessage(endpoint.receive());
            while (!saslServer.isComplete()) {
                byte[] data = processData(msg.getData(), endpoint,
                        saslServer);
                if (saslServer.isComplete()) {
                    System.out.println("server is complete");
                    endpoint.send(new Message(SaslStatus.SUCCESS, data));
                } else {
                    System.out.println("server continues");
                    endpoint.send(new Message(SaslStatus.CONTINUE, data));
                    msg = getMessage(endpoint.receive());
                }
            }
        }

        private byte[] processData(byte[] data, SaslEndpoint endpoint,
                SaslServer server) throws SaslException, IOException {
            try {
                return server.evaluateResponse(data);
            } catch (SaslException e) {
                endpoint.send(new Message(SaslStatus.FAILURE, null));
                System.out.println("Error while processing data");
                throw e;
            }
        }

        private SaslServer createSaslServer(String mechanism)
                throws SaslException {
            Map<String, String> props = new HashMap<>();
            props.put(Sasl.QOP, qop);
            return Sasl.createSaslServer(mechanism, PROTOCOL, host, props,
                    callback);
        }

        @Override
        public void run() {
            try (ServerSocket ss = new ServerSocket(0)) {
                ssocket = ss;
                System.out.println("server started on port " + getPort());
                ready = true;
                Socket socket = ss.accept();
                try (SaslEndpoint endpoint = new SaslEndpoint(socket)) {
                    System.out.println("server accepted connection");
                    processConnection(endpoint);
                }
            } catch (Exception e) {
                // ignore it for now, client will throw an exception
            }
        }

        @Override
        public void close() throws IOException {
            if (!ssocket.isClosed()) {
                ssocket.close();
            }
        }
    }

    static class Client extends SaslPeer {

        private final int port;

        Client(String host, int port, String mech, String authId,
                String... qops) {
            super(host, mech, authId, qops);
            this.port = port;
        }

        public void run() throws Exception {
            System.out.println("Host:" + host + " port: "
                    + port);
            try (SaslEndpoint endpoint = SaslEndpoint.create(host, port)) {
                negotiateMechanism(endpoint);
                SaslClient client = createSaslClient();
                byte[] data = new byte[0];
                if (client.hasInitialResponse()) {
                    data = client.evaluateChallenge(data);
                }
                endpoint.send(new Message(SaslStatus.CONTINUE, data));
                Message msg = getMessage(endpoint.receive());
                while (!client.isComplete()
                        && msg.getStatus() != SaslStatus.FAILURE) {
                    switch (msg.getStatus()) {
                        case CONTINUE:
                            System.out.println("client continues");
                            data = client.evaluateChallenge(msg.getData());
                            endpoint.send(new Message(SaslStatus.CONTINUE,
                                    data));
                            msg = getMessage(endpoint.receive());
                            break;
                        case SUCCESS:
                            System.out.println("client succeeded");
                            data = client.evaluateChallenge(msg.getData());
                            if (data != null) {
                                throw new SaslException("data should be null");
                            }
                            break;
                        default:
                            throw new RuntimeException("Wrong status:"
                                    + msg.getStatus());
                    }
                }

                if (msg.getStatus() == SaslStatus.FAILURE) {
                    throw new RuntimeException("Status is FAILURE");
                }
            }

            System.out.println("Done");
        }

        private SaslClient createSaslClient() throws SaslException {
            Map<String, String> props = new HashMap<>();
            props.put(Sasl.QOP, qop);
            return Sasl.createSaslClient(new String[] {mechanism}, USER_ID,
                    PROTOCOL, host, props, callback);
        }

        private void negotiateMechanism(SaslEndpoint endpoint)
                throws ClassNotFoundException, IOException {
            Object o = endpoint.receive();
            if (o instanceof ArrayList) {
                ArrayList list = (ArrayList) o;
                if (!list.contains(mechanism)) {
                    throw new RuntimeException(
                            "Server does not support specified mechanism:"
                                    + mechanism);
                }
            } else {
                throw new RuntimeException(
                        "Expected an instance of ArrayList, but received " + o);
            }

            endpoint.send(mechanism);
        }

    }

    static class SaslEndpoint implements AutoCloseable {

        private final Socket socket;
        private ObjectInputStream input;
        private ObjectOutputStream output;

        static SaslEndpoint create(String host, int port) throws IOException {
            return new SaslEndpoint(new Socket(host, port));
        }

        SaslEndpoint(Socket socket) throws IOException {
            this.socket = socket;
        }

        private ObjectInputStream getInput() throws IOException {
            if (input == null && socket != null) {
                input = new ObjectInputStream(socket.getInputStream());
            }
            return input;
        }

        private ObjectOutputStream getOutput() throws IOException {
            if (output == null && socket != null) {
                output = new ObjectOutputStream(socket.getOutputStream());
            }
            return output;
        }

        public Object receive() throws IOException, ClassNotFoundException {
            return getInput().readObject();
        }

        public void send(Object obj) throws IOException {
            getOutput().writeObject(obj);
            getOutput().flush();
        }

        @Override
        public void close() throws IOException {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        }

    }

    static class TestCallbackHandler implements CallbackHandler {

        private final String userId;
        private final char[] passwd;
        private final String realm;
        private String authId;

        TestCallbackHandler(String userId, String passwd, String realm,
                String authId) {
            this.userId = userId;
            this.passwd = passwd.toCharArray();
            this.realm = realm;
            this.authId = authId;
        }

        @Override
        public void handle(Callback[] callbacks) throws IOException,
                UnsupportedCallbackException {
            for (Callback callback : callbacks) {
                if (callback instanceof NameCallback) {
                    System.out.println("NameCallback");
                    ((NameCallback) callback).setName(userId);
                } else if (callback instanceof PasswordCallback) {
                    System.out.println("PasswordCallback");
                    ((PasswordCallback) callback).setPassword(passwd);
                } else if (callback instanceof RealmCallback) {
                    System.out.println("RealmCallback");
                    ((RealmCallback) callback).setText(realm);
                } else if (callback instanceof RealmChoiceCallback) {
                    System.out.println("RealmChoiceCallback");
                    RealmChoiceCallback choice = (RealmChoiceCallback) callback;
                    if (realm == null) {
                        choice.setSelectedIndex(choice.getDefaultChoice());
                    } else {
                        String[] choices = choice.getChoices();
                        for (int j = 0; j < choices.length; j++) {
                            if (realm.equals(choices[j])) {
                                choice.setSelectedIndex(j);
                                break;
                            }
                        }
                    }
                } else if (callback instanceof AuthorizeCallback) {
                    System.out.println("AuthorizeCallback");
                    ((AuthorizeCallback) callback).setAuthorized(true);
                    if (authId == null || authId.trim().length() == 0) {
                        authId = userId;
                    }
                    ((AuthorizeCallback) callback).setAuthorizedID(authId);
                } else {
                    throw new UnsupportedCallbackException(callback);
                }
            }
        }
    }

}
