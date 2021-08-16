/*
 * Copyright (c) 2012, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.List;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.JDKToolFinder;
import static java.lang.System.out;
import java.util.ArrayList;

/**
 * Helper class for creating keystore and executing keytool commands
 */
public class Utils {
    public enum KeyStoreType {
        jks, pkcs12;
    }
    public static final String DEFAULT_DNAME
            = "CN=TestKey, T=FuncTestCertKey, O=Oracle, OU=JDKSQE, C=US";
    public static final String DEFAULT_PASSWD = "passwd";
    public static final String RSA = "rsa";
    public static final String JAVA_HOME = System.getProperty("java.home");
    public static final String KEYTOOL = "keytool";
    private static final int SUCCESS_EXIT_CODE = 0;

    public static OutputAnalyzer executeKeytoolCommand(String[] command) {
        return executeKeytoolCommand(command, SUCCESS_EXIT_CODE);
    }

    public static OutputAnalyzer executeKeytoolCommand(String[] command,
            int exitCode) {
        String[] keytoolCmd = new String[command.length + 1];
        OutputAnalyzer output = null;
        try {
            keytoolCmd[0] = JDKToolFinder.getJDKTool(KEYTOOL);
            System.arraycopy(command, 0, keytoolCmd, 1, command.length);
            output = ProcessTools.executeCommand(keytoolCmd);
            output.shouldHaveExitValue(exitCode);
            out.println("Executed keytool command sucessfully:"
                    + Arrays.toString(keytoolCmd));
        } catch (Throwable e) {
            e.printStackTrace(System.err);
            throw new RuntimeException("Keytool Command execution failed : "
                    + Arrays.toString(keytoolCmd), e);
        }
        return output;
    }

    public static void createKeyStore(KeyStoreType type, String name,
            String alias) {
        createKeyStore(DEFAULT_DNAME, type, name, alias, RSA);
    }

    public static void createKeyStore(String dName, KeyStoreType type,
            String name, String alias, String algorithm,
            String... optionalArgs) {
        createKeyStore(dName, type, name, alias, algorithm, optionalArgs,
                SUCCESS_EXIT_CODE);
    }

    public static void createKeyStore(String dName, KeyStoreType type,
            String name, String alias, String algorithm,
            String[] optionalArgs, int exitCode) {
        String[] command = new String[]{"-debug", "-genkeypair", "-alias",
            alias, "-keystore", name, "-dname", dName, "-storepass",
            DEFAULT_PASSWD, "-keypass", DEFAULT_PASSWD, "-validity", "7300",
            "-keyalg", algorithm, "-storetype", type.name()};
        if (optionalArgs != null && optionalArgs.length > 0) {
            List<String> commandArgs = new ArrayList<>(Arrays.asList(command));
            List<String> temp = Arrays.asList(optionalArgs);
            commandArgs.addAll(temp);
            if (!commandArgs.contains(("-keysize"))) {
                commandArgs.add("-keysize");
                commandArgs.add("1024");
            }
            command = commandArgs.toArray(new String[commandArgs.size()]);
        }
        executeKeytoolCommand(command, exitCode);
    }

    public static void exportCert(KeyStoreType type, String name,
            String alias, String cert) {
        String[] command = {"-debug", "-exportcert", "-keystore", name,
            "-storetype", type.name(), "-storepass", DEFAULT_PASSWD, "-alias",
            alias,"-file",cert,"-noprompt"};
        executeKeytoolCommand(command);
    }

    public static KeyStore loadKeyStore(String file, KeyStoreType type,
            char[] passwd)
            throws IOException, KeyStoreException,
            NoSuchAlgorithmException, CertificateException {
        KeyStore ks = KeyStore.getInstance(type.name());
        try (FileInputStream fin = new FileInputStream(file)) {
            ks.load(fin, passwd);
        }
        return ks;
    }

    public static void saveKeyStore(KeyStore ks, String file, char[] passwd)
            throws IOException, KeyStoreException, NoSuchAlgorithmException,
            CertificateException {
        try (FileOutputStream fout = new FileOutputStream(file)) {
            ks.store(fout, passwd);
        }
    }
}
