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

import com.sun.jdi.Bootstrap;
import com.sun.jdi.VMDisconnectedException;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.connect.LaunchingConnector;
import com.sun.jdi.event.Event;
import com.sun.jdi.event.EventSet;
import com.sun.jdi.event.MethodEntryEvent;
import com.sun.jdi.request.MethodEntryRequest;

import java.security.AlgorithmParameters;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;

/*
 * @test
 * @bug 8237218 8239928
 * @modules jdk.crypto.ec
 *          jdk.jdi
 * @requires os.family != "windows"
 * @run main ECDSAJavaVerify debug
 * @summary Support NIST Curves verification in java implementation.
 *  This test does not run stable on Windows. VMDisconnectedException
 *  might not be thrown at all.
 */

// ATTENTION: This test depends on method names inside the non-exported
// class sun.security.ec.ECDSASignature.
public class ECDSAJavaVerify {

    static final String[] ALL_ALGS = new String[] {
            "SHA1withECDSA", "SHA256withECDSA", "SHA384withECDSA", "SHA512withECDSA"};

    static final String[] ALL_CURVES = new String[] {
            "secp256r1", "secp384r1", "secp521r1"};

    public static void main(String[] args) throws Exception {
        if (args.length == 1) {
            // Debugging a new process with no arg
            debug();
        } else if (args.length == 3) {
            // If one test case fail, re-run it with first 3 columns
            new Test().run(Integer.parseInt(args[0]), args[1], args[2]);
        } else {
            // Run all test cases
            Test t = new Test();
            Random r = new Random();

            for (String sigAlg : ALL_ALGS) {
                for (String curve : ALL_CURVES) {
                    t.run(r.nextInt(1000000), sigAlg, curve);
                }
            }
        }
    }

    static void debug() throws Exception {

        LaunchingConnector launchingConnector = Bootstrap
                .virtualMachineManager().defaultConnector();

        Map<String, Connector.Argument> arguments
                = launchingConnector.defaultArguments();
        arguments.get("main").setValue(ECDSAJavaVerify.class.getName());
        arguments.get("options").setValue(
                "-cp " + System.getProperty("test.classes"));
        VirtualMachine vm = launchingConnector.launch(arguments);

        MethodEntryRequest req = vm.eventRequestManager()
                .createMethodEntryRequest();
        req.addClassFilter("sun.security.ec.ECDSASignature");
        req.enable();

        int numberOfTests = ALL_ALGS.length * ALL_CURVES.length * 2;

        // Expected methods to call. 'J' for java impl, 'N' for native impl
        char[] expected = new char[numberOfTests];

        int pos = 0;
        for (String dummy : ALL_ALGS) {
            for (String curve : ALL_CURVES) {
                char caller = 'J';
                // For each case, Signature::verify is called twice
                expected[pos++] = caller;
                expected[pos++] = caller;
            }
        }

        // Test result
        // '.': not run yet
        // '-': enter engineVerify
        // 'v': expected impl called
        // 'x': unexpected impl called
        // Note: some error cases fail before any impl called. Ex: if there
        // is a DER encoding error.
        char[] result = new char[numberOfTests];
        Arrays.fill(result, '.');

        String stdout, stderr;

        try {
            EventSet eventSet;
            pos = -1; // will become 0 when entering 'engineVerify'
            while ((eventSet = vm.eventQueue().remove()) != null) {
                for (Event event : eventSet) {
                    if (event instanceof MethodEntryEvent) {
                        MethodEntryEvent e = (MethodEntryEvent)event;
                        switch (e.method().name()) {
                            case "engineVerify":
                                result[++pos] = '-';
                                break;
                            case "verifySignedDigestImpl": // the java impl
                                result[pos] = expected[pos] != 'J' ? 'x' : 'v';
                                break;
                        }
                    }
                    vm.resume();
                }
            }
        } catch (VMDisconnectedException e) {
            System.out.println("Virtual Machine is disconnected.");
        } finally {
            stderr = new String(vm.process().getErrorStream().readAllBytes());
            stdout = new String(vm.process().getInputStream().readAllBytes());
        }

        int exitCode = vm.process().waitFor();
        System.out.println("  exit: " + exitCode);
        System.out.println("stderr:\n" + stderr);
        System.out.println("stdout:\n" + stdout);

        String sResult = new String(result);

        System.out.println(" Cases: " + new String(expected));
        System.out.println("Result: " + sResult);

        if (pos != numberOfTests - 1 || sResult.contains("x")
                || sResult.contains(".")) {
            throw new Exception("Unexpected result");
        }

        if (stdout.contains("fail") || exitCode != 0) {
            throw new Exception("Test failed");
        }
    }

    static class Test {

        public boolean run(int seed, String sigAlg, String curve)
                throws Exception {

            // A determined SecureRandom based on seed. If there is anything
            // wrong, we can reproduce the problem using the seed.
            Random r = new Random(seed);
            SecureRandom rand = new SecureRandom() {
                @Override
                public void nextBytes(byte[] bytes) {
                    r.nextBytes(bytes);
                }
            };

            AlgorithmParameters ap = AlgorithmParameters.getInstance("EC", "SunEC");
            ap.init(new ECGenParameterSpec(curve));
            ECParameterSpec spec = ap.getParameterSpec(ECParameterSpec.class);

            KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC", "SunEC");
            kpg.initialize(spec, rand);
            KeyPair kp = kpg.generateKeyPair();
            ECPrivateKey ecPrivateKey = (ECPrivateKey) kp.getPrivate();
            ECPublicKey ecPublicKey = (ECPublicKey) kp.getPublic();

            Signature s1 = Signature.getInstance(sigAlg, "SunEC");
            s1.initSign(ecPrivateKey, rand);
            byte[] msg = new byte[1234];
            rand.nextBytes(msg);
            s1.update(msg);
            byte[] sig = s1.sign();

            Signature s2 = Signature.getInstance(sigAlg, "SunEC");
            s2.initVerify(ecPublicKey);
            s2.update(msg);

            boolean result1 = s2.verify(sig);

            s2.initVerify(ecPublicKey);
            // modify the signature in some random manner
            if (rand.nextInt(10) < 8) {
                sig[rand.nextInt(10000) % sig.length]
                        = (byte) rand.nextInt(10000);
            } else {
                int newLength = rand.nextInt(100);
                if (newLength == sig.length) {
                    newLength += 1 + rand.nextInt(2);
                }
                sig = Arrays.copyOf(sig, newLength);
            }

            boolean result2;
            try {
                result2 = s2.verify(sig);
            } catch (SignatureException se) {
                result2 = false;
            }

            boolean finalResult = result1 && !result2;
            System.out.printf("%10d %20s %20s -- %5s %5s -- %s\n",
                    seed, sigAlg, curve, result1, result2,
                    finalResult ? "succeed" : "fail");

            return finalResult;
        }
    }
}
