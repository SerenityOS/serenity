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

import java.io.InputStream;
import java.security.KeyStore;
import java.security.Provider;
import java.security.Security;
import java.security.cert.CRL;
import java.security.cert.CRLException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactorySpi;
import java.util.Collection;
import java.util.Enumeration;

/*
 * @test
 * @bug 8139436
 * @summary This test validates an iteration over the Windows-ROOT certificate store
 *          and retrieving all certificates.
 *          Bug 8139436 reports an issue when 3rd party JCE providers would throw exceptions
 *          upon creating Certificate objects.
 *          This would for instance happen when using IAIK 3.15 and Elliptic Curve certificates
 *          are contained in the Windows-ROOT certificate store.
 *          The test uses a simple dummy provider which just throws Exceptions in its CertificateFactory.
 *          To test an external provider, you can use property sun.security.mscapi.testprovider and
 *          set it to the provider class name which has to be constructible by a constructor without
 *          arguments. The provider jar has to be added to the classpath.
 *          E.g. run jtreg with -javaoption:-Dsun.security.mscapi.testprovider=iaik.security.provider.IAIK and
 *          -cpa:<path to iaik_jce.jar>
 *
 * @requires os.family == "windows"
 * @author Christoph Langer
 * @run main IterateWindowsRootStore
 */
public class IterateWindowsRootStore {
    public static class TestFactory extends CertificateFactorySpi {
        @Override
        public Certificate engineGenerateCertificate(InputStream inStream) throws CertificateException {
            throw new CertificateException("unimplemented");
        }

        @Override
        public Collection<? extends Certificate> engineGenerateCertificates(InputStream inStream) throws CertificateException {
            throw new CertificateException("unimplemented");
        }

        @Override
        public CRL engineGenerateCRL(InputStream inStream) throws CRLException {
            throw new CRLException("unimplemented");
        }

        @Override
        public Collection<? extends CRL> engineGenerateCRLs(InputStream inStream) throws CRLException {
            throw new CRLException("unimplemented");
        }
    }

    public static class TestProvider extends Provider {
        private static final long serialVersionUID = 1L;

        public TestProvider() {
            super("TestProvider", 0.1, "Test provider for IterateWindowsRootStore");

            /*
             * Certificates
             */
            this.put("CertificateFactory.X.509", "IterateWindowsRootStore$TestFactory");
            this.put("Alg.Alias.CertificateFactory.X509", "X.509");
        }
    }

    public static void main(String[] args) throws Exception {
        // Try to register a JCE provider from property sun.security.mscapi.testprovider in the first slot
        // otherwise register a dummy provider which would provoke the issue of bug 8139436
        boolean providerPrepended = false;
        String testprovider = System.getProperty("sun.security.mscapi.testprovider");
        if (testprovider != null && !testprovider.isEmpty()) {
            try {
                System.out.println("Trying to prepend external JCE provider " + testprovider);
                Class<?> providerclass = Class.forName(testprovider);
                Object provider = providerclass.newInstance();
                Security.insertProviderAt((Provider)provider, 1);
            } catch (Exception e) {
                System.out.println("Could not load JCE provider " + testprovider +". Exception is:");
                e.printStackTrace(System.out);
            }
            providerPrepended = true;
            System.out.println("Sucessfully prepended JCE provider " + testprovider);
        }
        if (!providerPrepended) {
            System.out.println("Trying to prepend dummy JCE provider");
            Security.insertProviderAt(new TestProvider(), 1);
            System.out.println("Sucessfully prepended dummy JCE provider");
        }

        // load Windows-ROOT KeyStore
        KeyStore keyStore = KeyStore.getInstance("Windows-ROOT", "SunMSCAPI");
        keyStore.load(null, null);

        // iterate KeyStore
        Enumeration<String> aliases = keyStore.aliases();
        while (aliases.hasMoreElements()) {
            String alias = aliases.nextElement();
            System.out.print("Reading certificate for alias: " + alias + "...");
            keyStore.getCertificate(alias);
            System.out.println(" done.");
        }
    }
}
