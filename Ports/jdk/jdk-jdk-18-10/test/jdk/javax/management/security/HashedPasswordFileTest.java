/*
 * Copyright (c) 2017, 2018, Oracle and/or its affiliates. All rights reserved.
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

 /* @test
 * @bug 5016517 8204661
 * @summary Test Hashed passwords
 * @library /test/lib
 * @modules java.management
 *          jdk.management.agent/jdk.internal.agent
 * @build HashedPasswordFileTest
 * @run testng/othervm  HashedPasswordFileTest
 *
 */

import jdk.internal.agent.ConnectorAddressLink;
import jdk.test.lib.Utils;
import jdk.test.lib.process.ProcessTools;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import javax.management.MBeanServer;
import javax.management.remote.*;
import java.io.*;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

@Test
public class HashedPasswordFileTest {

    private final String[] randomWords = {"accost", "savoie", "bogart", "merest",
            "azuela", "hoodie", "bursal", "lingua", "wincey", "trilby", "egesta",
            "wester", "gilgai", "weinek", "ochone", "sanest", "gainst", "defang",
            "ranket", "mayhem", "tagger", "timber", "eggcup", "mhren", "colloq",
            "dreamy", "hattie", "rootle", "bloody", "helyne", "beater", "cosine",
            "enmity", "outbox", "issuer", "lumina", "dekker", "vetoed", "dennis",
            "strove", "gurnet", "talkie", "bennie", "behove", "coates", "shiloh",
            "yemeni", "boleyn", "coaxal", "irne"};

    private final String[] hashAlgs = {
            "MD2",
            "MD5",
            "SHA-1",
            "SHA-224",
            "SHA-256",
            "SHA-384",
            "SHA-512/224",
            "SHA-512/256",
            "SHA3-224",
            "SHA3-256",
            "SHA3-384",
            "SHA3-512"
    };

    private final Random random = Utils.getRandomInstance();

    private JMXConnectorServer cs;

    private String randomWord() {
        int idx = random.nextInt(randomWords.length);
        return randomWords[idx];
    }

    private String[] getHash(String algorithm, String password) {
        try {
            byte[] salt = new byte[64];
            random.nextBytes(salt);

            MessageDigest digest = MessageDigest.getInstance(algorithm);
            digest.reset();
            digest.update(salt);
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));

            String saltStr = Base64.getEncoder().encodeToString(salt);
            String hashStr = Base64.getEncoder().encodeToString(hash);

            return new String[]{saltStr, hashStr};
        } catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException(ex);
        }
    }

    private String getPasswordFilePath() {
        String testDir = System.getProperty("test.src");
        String testFileName = "jmxremote.password";
        return testDir + File.separator + testFileName;
    }

    private File createNewPasswordFile() throws IOException {
        File file = new File(getPasswordFilePath());
        if (file.exists()) {
            file.delete();
        }
        file.createNewFile();
        return file;
    }

    private Map<String, String> generateClearTextPasswordFile() throws IOException {
        File file = createNewPasswordFile();
        Map<String, String> props = new HashMap<>();
        BufferedWriter br;
        try (FileWriter fw = new FileWriter(file)) {
            br = new BufferedWriter(fw);
            int numentries = random.nextInt(5) + 3;
            for (int i = 0; i < numentries; i++) {
                String username;
                do {
                    username = randomWord();
                } while (props.get(username) != null);
                String password = randomWord();
                props.put(username, password);
                br.write(username + " " + password + "\n");
            }
            br.flush();
        }
        br.close();
        return props;
    }

    private boolean isPasswordFileHashed() throws IOException {
        BufferedReader br;
        boolean result;
        try (FileReader fr = new FileReader(getPasswordFilePath())) {
            br = new BufferedReader(fr);
            result = br.lines().anyMatch(line -> {
                if (line.startsWith("#")) {
                    return false;
                }
                String[] tokens = line.split("\\s+");
                return tokens.length == 3 || tokens.length == 4;
            });
        }
        br.close();
        return result;
    }

    private Map<String, String> generateHashedPasswordFile() throws IOException {
        File file = createNewPasswordFile();
        Map<String, String> props = new HashMap<>();
        BufferedWriter br;
        try (FileWriter fw = new FileWriter(file)) {
            br = new BufferedWriter(fw);
            int numentries = random.nextInt(5) + 3;
            for (int i = 0; i < numentries; i++) {
                String username;
                do {
                    username = randomWord();
                } while (props.get(username) != null);
                String password = randomWord();
                String alg = hashAlgs[random.nextInt(hashAlgs.length)];
                String[] b64str = getHash(alg, password);
                br.write(username + " " + b64str[0] + " " + b64str[1] + " " + alg + "\n");
                props.put(username, password);
            }
            br.flush();
        }
        br.close();
        return props;
    }

    private JMXServiceURL createServerSide(boolean useHash)
            throws IOException {
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        JMXServiceURL url = new JMXServiceURL("rmi", null, 0);

        HashMap<String, Object> env = new HashMap<>();
        env.put("jmx.remote.x.password.file", getPasswordFilePath());
        env.put("jmx.remote.x.password.toHashes", useHash ? "true" : "false");
        cs = JMXConnectorServerFactory.newJMXConnectorServer(url, env, mbs);
        cs.start();
        return cs.getAddress();
    }

    @Test
    public void testClearTextPasswordFile() throws IOException {
        Boolean[] bvals = new Boolean[]{true, false};
        for (boolean bval : bvals) {
            try {
                Map<String, String> credentials = generateClearTextPasswordFile();
                JMXServiceURL serverUrl = createServerSide(bval);
                for (Map.Entry<String, String> entry : credentials.entrySet()) {
                    HashMap<String, Object> env = new HashMap<>();
                    env.put("jmx.remote.credentials",
                            new String[]{entry.getKey(), entry.getValue()});
                    try (JMXConnector cc = JMXConnectorFactory.connect(serverUrl, env)) {
                        cc.getMBeanServerConnection();
                    }
                }
                Assert.assertEquals(isPasswordFileHashed(), bval);
            } finally {
                cs.stop();
            }
        }
    }

    @Test
    public void testReadOnlyPasswordFile() throws IOException {
        Boolean[] bvals = new Boolean[]{true, false};
        for (boolean bval : bvals) {
            try {
                Map<String, String> credentials = generateClearTextPasswordFile();
                File file = new File(getPasswordFilePath());
                file.setReadOnly();
                JMXServiceURL serverUrl = createServerSide(bval);
                for (Map.Entry<String, String> entry : credentials.entrySet()) {
                    HashMap<String, Object> env = new HashMap<>();
                    env.put("jmx.remote.credentials",
                            new String[]{entry.getKey(), entry.getValue()});
                    try (JMXConnector cc = JMXConnectorFactory.connect(serverUrl, env)) {
                        cc.getMBeanServerConnection();
                    }
                }
                Assert.assertEquals(isPasswordFileHashed(), false);
            } finally {
                cs.stop();
            }
        }
    }

    @Test
    public void testHashedPasswordFile() throws IOException {
        Boolean[] bvals = new Boolean[]{true, false};
        for (boolean bval : bvals) {
            try {
                Map<String, String> credentials = generateHashedPasswordFile();
                JMXServiceURL serverUrl = createServerSide(bval);
                Assert.assertEquals(isPasswordFileHashed(), true);
                for (Map.Entry<String, String> entry : credentials.entrySet()) {
                    HashMap<String, Object> env = new HashMap<>();
                    env.put("jmx.remote.credentials",
                            new String[]{entry.getKey(), entry.getValue()});
                    try (JMXConnector cc = JMXConnectorFactory.connect(serverUrl, env)) {
                        cc.getMBeanServerConnection();
                    }
                }
            } finally {
                cs.stop();
            }
        }
    }

    private static class SimpleJMXClient implements Callable {
        private final JMXServiceURL url;
        private final Map<String, String> credentials;

        public SimpleJMXClient(JMXServiceURL url, Map<String, String> credentials) {
            this.url = url;
            this.credentials = credentials;
        }

        @Override
        public Object call() throws Exception {
            for (Map.Entry<String, String> entry : credentials.entrySet()) {
                HashMap<String, Object> env = new HashMap<>();
                env.put("jmx.remote.credentials",
                        new String[]{entry.getKey(), entry.getValue()});
                try (JMXConnector cc = JMXConnectorFactory.connect(url, env)) {
                    cc.getMBeanServerConnection();
                }
            }
            return null;
        }
    }

    @Test
    public void testMultipleClients() throws Throwable {
        Map<String, String> credentials = generateClearTextPasswordFile();
        JMXServiceURL serverUrl = createServerSide(true);
        Assert.assertEquals(isPasswordFileHashed(), false);
        // create random number of clients
        int numClients = random.nextInt(20) + 10;
        List<Future> futures = new ArrayList<>();
        ExecutorService executor = Executors.newFixedThreadPool(numClients);
        for (int i = 0; i < numClients; i++) {
            Future future = executor.submit(new SimpleJMXClient(serverUrl, credentials));
            futures.add(future);
        }
        try {
            for (Future future : futures) {
                future.get();
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException ex) {
            throw ex.getCause();
        } finally {
            executor.shutdown();
        }

        Assert.assertEquals(isPasswordFileHashed(), true);
    }

    @Test
    public void testPasswordChange() throws IOException {
        try {
            Map<String, String> credentials = generateClearTextPasswordFile();
            JMXServiceURL serverUrl = createServerSide(true);
            Assert.assertEquals(isPasswordFileHashed(), false);

            for (Map.Entry<String, String> entry : credentials.entrySet()) {
                HashMap<String, Object> env = new HashMap<>();
                env.put("jmx.remote.credentials",
                        new String[]{entry.getKey(), entry.getValue()});
                try (JMXConnector cc = JMXConnectorFactory.connect(serverUrl, env)) {
                    cc.getMBeanServerConnection();
                }
            }
            Assert.assertEquals(isPasswordFileHashed(), true);

            // Read the file back. Add new entries. Change passwords for few
            BufferedReader br = new BufferedReader(new FileReader(getPasswordFilePath()));
            String line;
            StringBuilder sbuild = new StringBuilder();
            while ((line = br.readLine()) != null) {
                if (line.trim().startsWith("#")) {
                    sbuild.append(line).append("\n");
                    continue;
                }

                // Change password for random entries
                if (random.nextBoolean()) {
                    String[] tokens = line.split("\\s+");
                    if ((tokens.length == 4 || tokens.length == 3)) {
                        String password = randomWord();
                        credentials.put(tokens[0], password);
                        sbuild.append(tokens[0]).append(" ").append(password).append("\n");
                    }
                } else {
                    sbuild.append(line).append("\n");
                }
            }

            // Add new entries in clear
            int newentries = random.nextInt(2) + 3;
            for (int i = 0; i < newentries; i++) {
                String username;
                do {
                    username = randomWord();
                } while (credentials.get(username) != null);
                String password = randomWord();
                credentials.put(username, password);
                sbuild.append(username).append(" ").append(password).append("\n");
            }

            // Add new entries as a hash
            int numentries = random.nextInt(2) + 3;
            for (int i = 0; i < numentries; i++) {
                String username;
                do {
                    username = randomWord();
                } while (credentials.get(username) != null);
                String password = randomWord();
                String alg = hashAlgs[random.nextInt(hashAlgs.length)];
                String[] b64str = getHash(alg, password);
                credentials.put(username, password);
                sbuild.append(username).append(" ").append(b64str[0])
                        .append(" ").append(b64str[1]).append(" ")
                        .append(alg).append("\n");
            }

            try (BufferedWriter bw = new BufferedWriter(new FileWriter(getPasswordFilePath()))) {
                bw.write(sbuild.toString());
            }

            for (Map.Entry<String, String> entry : credentials.entrySet()) {
                HashMap<String, Object> env = new HashMap<>();
                env.put("jmx.remote.credentials",
                        new String[]{entry.getKey(), entry.getValue()});
                try (JMXConnector cc = JMXConnectorFactory.connect(serverUrl, env)) {
                    cc.getMBeanServerConnection();
                }
            }
        } finally {
            cs.stop();
        }
    }

    @Test
    public void testDefaultAgent() throws Exception {
        List<String> pbArgs = new ArrayList<>();
        generateClearTextPasswordFile();

        // This will run only on a POSIX compliant system
        if (!FileSystems.getDefault().supportedFileAttributeViews().contains("posix")) {
            return;
        }

        // Make sure only owner is able to read/write the file or else
        // default agent will fail to start
        File file = new File(getPasswordFilePath());
        Set<PosixFilePermission> perms = new HashSet<>();
        perms.add(PosixFilePermission.OWNER_READ);
        perms.add(PosixFilePermission.OWNER_WRITE);
        Files.setPosixFilePermissions(file.toPath(), perms);

        pbArgs.add("-cp");
        pbArgs.add(System.getProperty("test.class.path"));

        pbArgs.add("-Dcom.sun.management.jmxremote.port=0");
        pbArgs.add("-Dcom.sun.management.jmxremote.authenticate=true");
        pbArgs.add("-Dcom.sun.management.jmxremote.password.file=" + file.getAbsolutePath());
        pbArgs.add("-Dcom.sun.management.jmxremote.ssl=false");
        pbArgs.add("--add-exports");
        pbArgs.add("jdk.management.agent/jdk.internal.agent=ALL-UNNAMED");
        pbArgs.add(TestApp.class.getSimpleName());

        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder(
                pbArgs.toArray(new String[0]));
        Process process = ProcessTools.startProcess(
                TestApp.class.getSimpleName(),
                pb);

        if (process.waitFor() != 0) {
            throw new RuntimeException("Test Failed : Error starting default agent");
        }
        Assert.assertEquals(isPasswordFileHashed(), true);
    }

    @Test
    public void testDefaultAgentNoHash() throws Exception {
        List<String> pbArgs = new ArrayList<>();
        generateClearTextPasswordFile();

        // This will run only on a POSIX compliant system
        if (!FileSystems.getDefault().supportedFileAttributeViews().contains("posix")) {
            return;
        }

        // Make sure only owner is able to read/write the file or else
        // default agent will fail to start
        File file = new File(getPasswordFilePath());
        Set<PosixFilePermission> perms = new HashSet<>();
        perms.add(PosixFilePermission.OWNER_READ);
        perms.add(PosixFilePermission.OWNER_WRITE);
        Files.setPosixFilePermissions(file.toPath(), perms);

        pbArgs.add("-cp");
        pbArgs.add(System.getProperty("test.class.path"));

        pbArgs.add("-Dcom.sun.management.jmxremote.port=0");
        pbArgs.add("-Dcom.sun.management.jmxremote.authenticate=true");
        pbArgs.add("-Dcom.sun.management.jmxremote.password.file=" + file.getAbsolutePath());
        pbArgs.add("-Dcom.sun.management.jmxremote.password.toHashes=false");
        pbArgs.add("-Dcom.sun.management.jmxremote.ssl=false");
        pbArgs.add("--add-exports");
        pbArgs.add("jdk.management.agent/jdk.internal.agent=ALL-UNNAMED");
        pbArgs.add(TestApp.class.getSimpleName());

        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder(
                pbArgs.toArray(new String[0]));
        Process process = ProcessTools.startProcess(
                TestApp.class.getSimpleName(),
                pb);

        if (process.waitFor() != 0) {
            throw new RuntimeException("Test Failed : Error starting default agent");
        }
        Assert.assertEquals(isPasswordFileHashed(), false);
    }

    @AfterClass
    public void cleanUp() {
        File file = new File(getPasswordFilePath());
        if (file.exists()) {
            file.delete();
        }
    }
}

class TestApp {

    public static void main(String[] args) throws IOException {
        try {
            Map<String, String> propsMap = ConnectorAddressLink.importRemoteFrom(0);
            String jmxServiceUrl = propsMap.get("sun.management.JMXConnectorServer.0.remoteAddress");
            Map<String, Object> env = new HashMap<>(1);
            // any dummy credentials will do. We just have to trigger password hashing
            env.put("jmx.remote.credentials", new String[]{"a", "a"});
            try (JMXConnector cc = JMXConnectorFactory.connect(new JMXServiceURL(jmxServiceUrl), env)) {
                cc.getMBeanServerConnection();
            }
        } catch (SecurityException ex) {
            // Catch authentication failure here
        }
    }
}
