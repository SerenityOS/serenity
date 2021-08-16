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

/*
 * @test
 * @bug 8006259
 * @summary Test several modes of operation using vectors from SP 800-38A
 * @run main CheckExampleVectors
 */

import java.io.*;
import java.security.*;
import java.util.*;
import java.util.function.*;
import javax.crypto.*;
import javax.crypto.spec.*;

public class CheckExampleVectors {

    private enum Mode {
        ECB,
        CBC,
        CFB1,
        CFB8,
        CFB128,
        OFB,
        CTR
    }

    private enum Operation {
        Encrypt,
        Decrypt
    }

    private static class Block {
        private byte[] input;
        private byte[] output;

        public Block() {

        }
        public Block(String settings) {
            String[] settingsParts = settings.split(",");
            input = stringToBytes(settingsParts[0]);
            output = stringToBytes(settingsParts[1]);
        }
        public byte[] getInput() {
            return input;
        }
        public byte[] getOutput() {
            return output;
        }
    }

    private static class TestVector {
        private Mode mode;
        private Operation operation;
        private byte[] key;
        private byte[] iv;
        private List<Block> blocks = new ArrayList<Block>();

        public TestVector(String settings) {
            String[] settingsParts = settings.split(",");
            mode = Mode.valueOf(settingsParts[0]);
            operation = Operation.valueOf(settingsParts[1]);
            key = stringToBytes(settingsParts[2]);
            if (settingsParts.length > 3) {
                iv = stringToBytes(settingsParts[3]);
            }
        }

        public Mode getMode() {
            return mode;
        }
        public Operation getOperation() {
            return operation;
        }
        public byte[] getKey() {
            return key;
        }
        public byte[] getIv() {
            return iv;
        }
        public void addBlock (Block b) {
            blocks.add(b);
        }
        public Iterable<Block> getBlocks() {
            return blocks;
        }
    }

    private static final String VECTOR_FILE_NAME = "NIST_800_38A_vectors.txt";
    private static final Mode[] REQUIRED_MODES = {Mode.ECB, Mode.CBC, Mode.CTR};
    private static Set<Mode> supportedModes = new HashSet<Mode>();

    public static void main(String[] args) throws Exception {
        checkAllProviders();
        checkSupportedModes();
    }

    private static byte[] stringToBytes(String v) {
        if (v.equals("")) {
            return null;
        }
        return Base64.getDecoder().decode(v);
    }

    private static String toModeString(Mode mode) {
        return mode.toString();
    }

    private static int toCipherOperation(Operation op) {
        switch (op) {
            case Encrypt:
                return Cipher.ENCRYPT_MODE;
            case Decrypt:
                return Cipher.DECRYPT_MODE;
        }

        throw new RuntimeException("Unknown operation: " + op);
    }

    private static void log(String str) {
        System.out.println(str);
    }

    private static void checkVector(String providerName, TestVector test) {

        String modeString = toModeString(test.getMode());
        String cipherString = "AES" + "/" + modeString + "/" + "NoPadding";
        log("checking: " + cipherString + " on " + providerName);
        try {
            Cipher cipher = Cipher.getInstance(cipherString, providerName);
            SecretKeySpec key = new SecretKeySpec(test.getKey(), "AES");
            if (test.getIv() != null) {
                IvParameterSpec iv = new IvParameterSpec(test.getIv());
                cipher.init(toCipherOperation(test.getOperation()), key, iv);
            }
            else {
                cipher.init(toCipherOperation(test.getOperation()), key);
            }
            int blockIndex = 0;
            for (Block curBlock : test.getBlocks()) {
                byte[] blockOutput = cipher.update(curBlock.getInput());
                byte[] expectedBlockOutput = curBlock.getOutput();
                if (!Arrays.equals(blockOutput, expectedBlockOutput)) {
                    throw new RuntimeException("Blocks do not match at index "
                        + blockIndex);
                }
                blockIndex++;
            }
            log("success");
            supportedModes.add(test.getMode());
        } catch (NoSuchAlgorithmException ex) {
            log("algorithm not supported");
        } catch (NoSuchProviderException | NoSuchPaddingException
            | InvalidKeyException | InvalidAlgorithmParameterException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static boolean isComment(String line) {
        return (line != null) && line.startsWith("//");
    }

    private static TestVector readVector(BufferedReader in) throws IOException {
        String line;
        while (isComment(line = in.readLine())) {
            // skip comment lines
        }
        if (line == null || line.isEmpty()) {
            return null;
        }

        TestVector newVector = new TestVector(line);
        String numBlocksStr = in.readLine();
        int numBlocks = Integer.parseInt(numBlocksStr);
        for (int i = 0; i < numBlocks; i++) {
            Block newBlock = new Block(in.readLine());
            newVector.addBlock(newBlock);
        }

        return newVector;
    }

    private static void checkAllProviders() throws IOException {
        File dataFile = new File(System.getProperty("test.src", "."),
                                 VECTOR_FILE_NAME);
        BufferedReader in = new BufferedReader(new FileReader(dataFile));
        List<TestVector> allTests = new ArrayList<>();
        TestVector newTest;
        while ((newTest = readVector(in)) != null) {
            allTests.add(newTest);
        }

        for (Provider provider : Security.getProviders()) {
            checkProvider(provider.getName(), allTests);
        }
    }

    private static void checkProvider(String providerName,
                                      List<TestVector> allVectors)
        throws IOException {

        for (TestVector curVector : allVectors) {
            checkVector(providerName, curVector);
        }
    }

    /*
     *  This method helps ensure that the test is working properly by
     *  verifying that the test was able to check the test vectors for
     *  some of the modes of operation.
     */
    private static void checkSupportedModes() {
        for (Mode curMode : REQUIRED_MODES) {
            if (!supportedModes.contains(curMode)) {
                throw new RuntimeException(
                    "Mode not supported by any provider: " + curMode);
            }
        }

    }

}

