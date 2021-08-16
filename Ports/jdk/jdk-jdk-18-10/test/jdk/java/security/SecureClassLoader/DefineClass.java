/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.security.CodeSource;
import java.security.Key;
import java.security.KeyStoreException;
import java.security.KeyStoreSpi;
import java.security.NoSuchAlgorithmException;
import java.security.Permission;
import java.security.Policy;
import java.security.ProtectionDomain;
import java.security.Provider;
import java.security.SecureClassLoader;
import java.security.Security;
import java.security.UnrecoverableKeyException;
import java.security.URIParameter;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.PropertyPermission;

/*
 * @test
 * @bug 6826789 8131486 8130181
 * @summary Make sure equivalent ProtectionDomains are granted the same
 *          permissions when the CodeSource URLs are different but resolve
 *          to the same ip address after name service resolution.
 * @run main/othervm -Djava.security.manager=allow DefineClass
 */

public class DefineClass {

    // permissions that are expected to be granted by the policy file
    private final static Permission[] GRANTED_PERMS = new Permission[] {
        new PropertyPermission("user.home", "read"),
        new PropertyPermission("user.name", "read")
    };

    // Base64 encoded bytes of simple class: "package foo; public class Foo {}"
    private final static String FOO_CLASS =
        "yv66vgAAADMADQoAAwAKBwALBwAMAQAGPGluaXQ+AQADKClWAQAEQ29kZQEA" +
        "D0xpbmVOdW1iZXJUYWJsZQEAClNvdXJjZUZpbGUBAAhGb28uamF2YQwABAAF" +
        "AQAHZm9vL0ZvbwEAEGphdmEvbGFuZy9PYmplY3QAIQACAAMAAAAAAAEAAQAE" +
        "AAUAAQAGAAAAHQABAAEAAAAFKrcAAbEAAAABAAcAAAAGAAEAAAABAAEACAAA" +
        "AAIACQ==";

    // Base64 encoded bytes of simple class: "package bar; public class Bar {}"
    private final static String BAR_CLASS =
        "yv66vgAAADMADQoAAwAKBwALBwAMAQAGPGluaXQ+AQADKClWAQAEQ29kZQEA" +
        "D0xpbmVOdW1iZXJUYWJsZQEAClNvdXJjZUZpbGUBAAhCYXIuamF2YQwABAAF" +
        "AQAHYmFyL0JhcgEAEGphdmEvbGFuZy9PYmplY3QAIQACAAMAAAAAAAEAAQAE" +
        "AAUAAQAGAAAAHQABAAEAAAAFKrcAAbEAAAABAAcAAAAGAAEAAAABAAEACAAA" +
        "AAIACQ==";

    // Base64 encoded bytes of simple class: "package baz; public class Baz {}"
    private final static String BAZ_CLASS =
        "yv66vgAAADQADQoAAwAKBwALBwAMAQAGPGluaXQ+AQADKClWAQAEQ29kZQEA" +
        "D0xpbmVOdW1iZXJUYWJsZQEAClNvdXJjZUZpbGUBAAhCYXouamF2YQwABAAF" +
        "AQAHYmF6L0JhegEAEGphdmEvbGFuZy9PYmplY3QAIQACAAMAAAAAAAEAAQAE" +
        "AAUAAQAGAAAAHQABAAEAAAAFKrcAAbEAAAABAAcAAAAGAAEAAAABAAEACAAA" +
        "AAIACQ==";

    private final static String BAZ_CERT =
        "-----BEGIN CERTIFICATE-----\n" +
        "MIIEFzCCA8OgAwIBAgIESpPf8TANBglghkgBZQMEAwIFADAOMQwwCgYDVQQDEwNG\n" +
        "b28wHhcNMTUwNzE1MTY1ODM5WhcNMTUxMDEzMTY1ODM5WjAOMQwwCgYDVQQDEwNG\n" +
        "b28wggNCMIICNQYHKoZIzjgEATCCAigCggEBAI95Ndm5qum/q+2Ies9JUbbzLsWe\n" +
        "O683GOjqxJYfPv02BudDUanEGDM5uAnnwq4cU5unR1uF0BGtuLR5h3VJhGlcrA6P\n" +
        "FLM2CCiiL/onEQo9YqmTRTQJoP5pbEZY+EvdIIGcNwmgEFexla3NACM9ulSEtikf\n" +
        "nWSO+INEhneXnOwEtDSmrC516Zhd4j2wKS/BEYyf+p2BgeczjbeStzDXueNJWS9o\n" +
        "CZhyFTkV6j1ri0ZTxjNFj4A7MqTC4PJykCVuTj+KOwg4ocRQ5OGMGimjfd9eoUPe\n" +
        "S2b/BJA+1c8WI+FY1IfGCOl/IRzYHcojy244B2X4IuNCvkhMBXY5OWAc1mcCHQC6\n" +
        "9pamhXj3397n+mfJd8eF7zKyM7rlgMC81WldAoIBABamXFggSFBwTnUCo5dXBA00\n" +
        "2jo0eMFU1OSlwC0kLuBPluYeS9CQSr2sjzfuseCfMYLSPJBDy2QviABBYO35ygmz\n" +
        "IHannDKmJ/JHPpGHm6LE50S9IIFUTLVbgCw2jR+oPtSJ6U4PoGiOMkKKXHjEeMaN\n" +
        "BSe3HJo6uwsL4SxEaJY559POdNsQGmWqK4f2TGgm2z7HL0tVmYNLtO2wL3yQ6aSW\n" +
        "06VdU1vr/EXU9hn2Pz3tu4c5JcLyJOB3MSltqIfsHkdI+H77X963VIQxayIy3uVT\n" +
        "3a8CESsNHwLaMJcyJP4nrtqLnUspItm6i+Oe2eEDpjxSgQvGiLfi7UMW4e8X294D\n" +
        "ggEFAAKCAQBsGeU8/STExzQsJ8kFM9xarA/2VAFMzyUpd3IQ2UGHQC5rEnGh/RiU\n" +
        "T20y7a2hCpQ1f/qgLnY8hku9GRVY3z8WamBzWLzCAEAx67EsS58mf4o8R3sUbkH5\n" +
        "/mRaZoNVSPUy+tXoLmTzIetU4W+JT8Rq4OcXXU9uo9TreeBehhVexS3vpVgQeUIn\n" +
        "MmMma8WHpovIJQQlp4cyjalX7Beda/tqX/HPLkAS4TRqQAz7hFr3FqFrVMKFSGo4\n" +
        "fTS06GGdQ4tw9c6NQLuQ9WF9BxYSwSk9yENQvKDZaBNarqPMnsh1Gi/QcKMRBVhM\n" +
        "RT/9vb4QUi/pOowhhKCDBLgjY60QgX3HoyEwHzAdBgNVHQ4EFgQUa787CE+3ZNAb\n" +
        "g1ql9yJVVrRCdx0wDQYJYIZIAWUDBAMCBQADPwAwPAIcCUkZIRrBlKdTzhKYBEOm\n" +
        "E1i45MMum1RuHc28agIcfHQkkjBA4FfH5UMRgKpIyRR8V/dVboxDj4hKOA==\n" +
        "-----END CERTIFICATE-----";

    public static void main(String[] args) throws Exception {

        Security.addProvider(new TestProvider());

        MySecureClassLoader scl = new MySecureClassLoader();

        File policyFile = new File(System.getProperty("test.src", "."),
                                   "DefineClass.policy");
        Policy p = Policy.getInstance("JavaPolicy",
                                      new URIParameter(policyFile.toURI()));
        Policy.setPolicy(p);

        System.setSecurityManager(new SecurityManager());
        ArrayList<Permission> perms1 = getPermissions(scl, p,
                                                      "http://localhost/",
                                                      "foo.Foo", FOO_CLASS,
                                                      null);
        checkPerms(perms1, GRANTED_PERMS);
        ArrayList<Permission> perms2 = getPermissions(scl, p,
                                                      "http://127.0.0.1/",
                                                      "bar.Bar", BAR_CLASS,
                                                      null);
        checkPerms(perms2, GRANTED_PERMS);
        assert(perms1.equals(perms2));

        // check that class signed by baz is granted an additional permission
        Certificate[] chain = new Certificate[] {getCert(BAZ_CERT)};
        ArrayList<Permission> perms3 = getPermissions(scl, p,
                                                      "http://localhost/",
                                                      "baz.Baz", BAZ_CLASS,
                                                      chain);
        List<Permission> perms = new ArrayList<>(Arrays.asList(GRANTED_PERMS));
        perms.add(new PropertyPermission("user.dir", "read"));
        checkPerms(perms3, perms.toArray(new Permission[0]));
    }

    // returns the permissions granted to the codebase URL
    private static ArrayList<Permission> getPermissions(MySecureClassLoader scl,
                                                        Policy p, String url,
                                                        String className,
                                                        String classBytes,
                                                        Certificate[] chain)
                                                        throws IOException {
        CodeSource cs = new CodeSource(new URL(url), chain);
        Base64.Decoder bd = Base64.getDecoder();
        byte[] bytes = bd.decode(classBytes);
        Class<?> c = scl.defineMyClass(className, bytes, cs);
        ProtectionDomain pd = c.getProtectionDomain();
        return Collections.list(p.getPermissions(pd).elements());
    }

    private static void checkPerms(List<Permission> perms,
                                   Permission... grantedPerms)
        throws Exception
    {
        if (!perms.containsAll(Arrays.asList(grantedPerms))) {
            throw new Exception("Granted permissions not correct");
        }
    }

    private static Certificate getCert(String base64Cert) throws Exception {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        InputStream is = new ByteArrayInputStream(base64Cert.getBytes("UTF-8"));
        return cf.generateCertificate(is);
    }

    // A SecureClassLoader that allows the test to define its own classes
    private static class MySecureClassLoader extends SecureClassLoader {
        Class<?> defineMyClass(String name, byte[] b, CodeSource cs) {
            return super.defineClass(name, b, 0, b.length, cs);
        }
    }

    private static class TestProvider extends Provider {
        TestProvider() {
            super("Test8131486", "0.0", "For testing only");
            putService(new Provider.Service(this, "KeyStore", "Test8131486",
                       "DefineClass$TestKeyStore", null, null));
        }
    }

    /**
     * A KeyStore containing a single certificate entry named "baz".
     */
    public static class TestKeyStore extends KeyStoreSpi {
        private final String baz = "baz";
        private final List<String> aliases = Collections.singletonList(baz);
        private final Certificate bazCert;

        public TestKeyStore() {
            try {
                this.bazCert = getCert(BAZ_CERT);
            } catch (Exception e) {
                throw new Error();
            }
        }

        @Override
        public Enumeration<String> engineAliases() {
            return Collections.enumeration(aliases);
        }

        @Override
        public boolean engineContainsAlias(String alias) {
            return alias.equals(baz);
        }

        @Override
        public void engineDeleteEntry(String alias) throws KeyStoreException {
            throw new KeyStoreException();
        }

        @Override
        public Certificate engineGetCertificate(String alias) {
            return alias.equals(baz) ? bazCert : null;
        }

        @Override
        public String engineGetCertificateAlias(Certificate cert) {
            return cert.equals(bazCert) ? baz : null;
        }

        @Override
        public Certificate[] engineGetCertificateChain(String alias) {
            return alias.equals(baz) ? new Certificate[] {bazCert} : null;
        }

        @Override
        public Date engineGetCreationDate(String alias) {
            return alias.equals(baz) ? new Date() : null;
        }

        @Override
        public Key engineGetKey(String alias, char[] password)
            throws NoSuchAlgorithmException, UnrecoverableKeyException {
            return null;
        }

        @Override
        public boolean engineIsCertificateEntry(String alias) {
            return alias.equals(baz);
        }

        @Override
        public boolean engineIsKeyEntry(String alias) {
            return false;
        }

        @Override
        public void engineLoad(InputStream stream, char[] password)
            throws IOException, NoSuchAlgorithmException, CertificateException {
        }

        @Override
        public void engineSetCertificateEntry(String alias, Certificate cert)
            throws KeyStoreException {
            throw new KeyStoreException();
        }

        @Override
        public void engineSetKeyEntry(String alias, byte[] key,
                                      Certificate[] chain)
            throws KeyStoreException {
            throw new KeyStoreException();
        }

        @Override
        public void engineSetKeyEntry(String alias, Key key, char[] password,
                                      Certificate[] chain)
            throws KeyStoreException {
            throw new KeyStoreException();
        }

        @Override
        public int engineSize() { return 1; }

        @Override
        public void engineStore(OutputStream stream, char[] password)
            throws IOException, NoSuchAlgorithmException, CertificateException {
        }
    }
}
