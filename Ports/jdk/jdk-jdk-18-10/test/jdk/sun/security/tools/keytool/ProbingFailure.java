/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8214100
 * @summary use of keystore probing results in unnecessary exception thrown
 * @library /test/lib
 * @compile -XDignore.symbol.file ProbingFailure.java
 * @run main ProbingFailure
 */

import jdk.test.lib.SecurityTools;
import jdk.test.lib.process.OutputAnalyzer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.KeyStoreSpi;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.Date;
import java.util.Enumeration;

public class ProbingFailure {

    public static void main(String[] args) throws Exception {

        // genkeypair
        kt("-genkeypair -keystore mks -alias a -dname CN=A -keyalg DSA -storetype MYKS")
                .shouldHaveExitValue(0);

        // list
        kt("-list -keystore mks -storetype MYKS")
                .shouldHaveExitValue(0);

        kt("-list -keystore mks")
                .shouldHaveExitValue(1)
                .shouldContain("Unrecognized keystore format");

        // importkeystore
        kt("-importkeystore -srckeystore mks -srcstoretype MYKS -destkeystore p12")
                .shouldHaveExitValue(0);

        kt("-importkeystore -srckeystore mks -destkeystore p12a")
                .shouldHaveExitValue(1)
                .shouldContain("Unrecognized keystore format");

        // in-place importkeystore
        kt("-importkeystore -srckeystore mks -srcstoretype MYKS -destkeystore mks -deststoretype myks")
                .shouldContain("The original keystore \"mks\" is backed up")
                .shouldHaveExitValue(0);

        kt("-importkeystore -srckeystore mks -srcstoretype MYKS -destkeystore mks")
                .shouldContain("Migrated \"mks\" to PKCS12")
                .shouldHaveExitValue(0);

        kt("-importkeystore -srckeystore p12 -destkeystore p12 -deststoretype MYKS")
                .shouldContain("Migrated \"p12\" to MYKS")
                .shouldHaveExitValue(0);
    }

    static OutputAnalyzer kt(String cmd) throws Exception {
        return SecurityTools.keytool(
                "-storepass changeit -keypass changeit -debug "
                + "-srcstorepass changeit -deststorepass changeit "
                + "-providerclass ProbingFailure$MyProvider "
                + "-providerpath " + System.getProperty("test.classes")
                + " " + cmd);
    }

    public static class MyProvider extends Provider {
        public MyProvider() {
            super("MP", "1.0", "My Provider");
            put("KeyStore.MYKS", "ProbingFailure$MyKS");
        }
    }

    // The MYKS keystore prepends a zero byte before a PKCS12 file
    // and does not support probing.
    public static class MyKS extends KeyStoreSpi {

        KeyStore ks;

        public MyKS() {
            try {
                ks = KeyStore.getInstance("PKCS12");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public Key engineGetKey(String alias, char[] password)
                throws NoSuchAlgorithmException, UnrecoverableKeyException {
            try {
                return ks.getKey(alias, password);
            } catch (KeyStoreException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public Certificate[] engineGetCertificateChain(String alias) {
            try {
                return ks.getCertificateChain(alias);
            } catch (KeyStoreException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public Certificate engineGetCertificate(String alias) {
            try {
                return ks.getCertificate(alias);
            } catch (KeyStoreException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public Date engineGetCreationDate(String alias) {
            try {
                return ks.getCreationDate(alias);
            } catch (KeyStoreException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void engineSetKeyEntry(String alias, Key key, char[] password,
                Certificate[] chain) throws KeyStoreException {
            ks.setKeyEntry(alias, key, password, chain);
        }

        @Override
        public void engineSetKeyEntry(String alias, byte[] key,
                Certificate[] chain) throws KeyStoreException {
            ks.setKeyEntry(alias, key, chain);
        }

        @Override
        public void engineSetCertificateEntry(String alias, Certificate cert)
                throws KeyStoreException {
            ks.setCertificateEntry(alias, cert);
        }

        @Override
        public void engineDeleteEntry(String alias) throws KeyStoreException {
            ks.deleteEntry(alias);
        }

        @Override
        public Enumeration<String> engineAliases() {
            try {
                return ks.aliases();
            } catch (KeyStoreException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public boolean engineContainsAlias(String alias) {
            try {
                return ks.containsAlias(alias);
            } catch (KeyStoreException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public int engineSize() {
            try {
                return ks.size();
            } catch (KeyStoreException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public boolean engineIsKeyEntry(String alias) {
            try {
                return ks.isKeyEntry(alias);
            } catch (KeyStoreException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public boolean engineIsCertificateEntry(String alias) {
            try {
                return ks.isCertificateEntry(alias);
            } catch (KeyStoreException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public String engineGetCertificateAlias(Certificate cert) {
            try {
                return ks.getCertificateAlias(cert);
            } catch (KeyStoreException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void engineStore(OutputStream stream, char[] password)
                throws IOException, NoSuchAlgorithmException, CertificateException {
            stream.write(0);
            try {
                ks.store(stream, password);
            } catch (KeyStoreException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void engineLoad(InputStream stream, char[] password)
                throws IOException, NoSuchAlgorithmException, CertificateException {
            if (stream != null) {
                stream.read();
            }
            ks.load(stream, password);
        }

        @Override
        public boolean engineProbe(InputStream stream) {
            return false;
        }
    }
}
