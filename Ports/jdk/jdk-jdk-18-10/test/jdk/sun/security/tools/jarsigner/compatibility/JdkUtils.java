/*
 * Copyright (c) 2017, 2019, Oracle and/or its affiliates. All rights reserved.
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

import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import static java.util.Arrays.asList;

/*
 * This class is used for returning some specific JDK information.
 */
public class JdkUtils {

    private enum Alg {
        KEY, SIG, DIGEST;
    }

    static final String M_JAVA_VERSION = "javaVersion";
    static final String M_JAVA_RUNTIME_VERSION = "javaRuntimeVersion";
    static final String M_IS_SUPPORTED_KEYALG = "isSupportedKeyalg";
    static final String M_IS_SUPPORTED_SIGALG = "isSupportedSigalg";
    static final String M_IS_SUPPORTED_DIGESTALG = "isSupportedDigestalg";

    // Returns the JDK build version.
    static String javaVersion() {
        return System.getProperty("java.version");
    }

    // Returns the JDK build runtime version.
    static String javaRuntimeVersion() {
        return System.getProperty("java.runtime.version");
    }

    // Checks if the specified algorithm is supported by the JDK.
    static boolean isSupportedAlg(Alg algType, String algName) {
        try {
            switch (algType) {
            case KEY:
                return KeyPairGenerator.getInstance(algName) != null;
            case SIG:
                return Signature.getInstance(algName) != null;
            case DIGEST:
                return MessageDigest.getInstance(algName) != null;
            }
        } catch (NoSuchAlgorithmException e) { }
        System.out.println(algName + " is not supported yet.");
        return false;
    }

    public static void main(String[] args) {
        if (M_JAVA_VERSION.equals(args[0])) {
            System.out.print(javaVersion());
        } else if (M_JAVA_RUNTIME_VERSION.equals(args[0])) {
            System.out.print(javaRuntimeVersion());
        } else if (M_IS_SUPPORTED_KEYALG.equals(args[0])) {
            System.out.print(isSupportedAlg(Alg.KEY, args[1]));
        } else if (M_IS_SUPPORTED_SIGALG.equals(args[0])) {
            System.out.print(isSupportedAlg(Alg.SIG, args[1]));
        } else if (M_IS_SUPPORTED_DIGESTALG.equals(args[0])) {
            System.out.print(isSupportedAlg(Alg.DIGEST, args[1]));
        } else {
            throw new IllegalArgumentException("invalid: " + asList(args));
        }
    }

}
