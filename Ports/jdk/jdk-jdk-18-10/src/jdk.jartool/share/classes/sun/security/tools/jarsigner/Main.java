/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
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

package sun.security.tools.jarsigner;

import java.io.*;
import java.net.UnknownHostException;
import java.security.cert.CertPathValidatorException;
import java.security.cert.PKIXBuilderParameters;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.*;
import java.util.jar.*;
import java.net.URI;
import java.text.Collator;
import java.text.MessageFormat;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.security.cert.CertificateException;
import java.security.*;

import java.net.SocketTimeoutException;
import java.net.URL;
import java.security.cert.CertPath;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.TrustAnchor;
import java.util.Map.Entry;

import jdk.internal.access.JavaUtilZipFileAccess;
import jdk.internal.access.SharedSecrets;
import jdk.security.jarsigner.JarSigner;
import jdk.security.jarsigner.JarSignerException;
import sun.security.pkcs.PKCS7;
import sun.security.pkcs.SignerInfo;
import sun.security.timestamp.TimestampToken;
import sun.security.tools.KeyStoreUtil;
import sun.security.validator.Validator;
import sun.security.validator.ValidatorException;
import sun.security.x509.*;
import sun.security.util.*;


/**
 * <p>The jarsigner utility.
 *
 * The exit codes for the main method are:
 *
 * 0: success
 * 1: any error that the jar cannot be signed or verified, including:
 *      keystore loading error
 *      TSP communication error
 *      jarsigner command line error...
 * otherwise: error codes from -strict
 *
 * @author Roland Schemers
 * @author Jan Luehe
 */
public class Main {

    // for i18n
    private static final java.util.ResourceBundle rb =
        java.util.ResourceBundle.getBundle
        ("sun.security.tools.jarsigner.Resources");
    private static final Collator collator = Collator.getInstance();
    static {
        // this is for case insensitive string comparisions
        collator.setStrength(Collator.PRIMARY);
    }

    private static final String NONE = "NONE";
    private static final String P11KEYSTORE = "PKCS11";

    private static final long SIX_MONTHS = 180*24*60*60*1000L; //milliseconds
    private static final long ONE_YEAR = 366*24*60*60*1000L;

    private static final DisabledAlgorithmConstraints JAR_DISABLED_CHECK =
            new DisabledAlgorithmConstraints(
                    DisabledAlgorithmConstraints.PROPERTY_JAR_DISABLED_ALGS);

    private static final DisabledAlgorithmConstraints CERTPATH_DISABLED_CHECK =
            new DisabledAlgorithmConstraints(
                    DisabledAlgorithmConstraints.PROPERTY_CERTPATH_DISABLED_ALGS);

    private static final DisabledAlgorithmConstraints LEGACY_CHECK =
            new DisabledAlgorithmConstraints(
                    DisabledAlgorithmConstraints.PROPERTY_SECURITY_LEGACY_ALGS);

    private static final Set<CryptoPrimitive> DIGEST_PRIMITIVE_SET = Collections
            .unmodifiableSet(EnumSet.of(CryptoPrimitive.MESSAGE_DIGEST));
    private static final Set<CryptoPrimitive> SIG_PRIMITIVE_SET = Collections
            .unmodifiableSet(EnumSet.of(CryptoPrimitive.SIGNATURE));

    private static boolean extraAttrsDetected;

    static final String VERSION = "1.0";

    static final int IN_KEYSTORE = 0x01;        // signer is in keystore
    static final int NOT_ALIAS = 0x04;          // alias list is NOT empty and
    // signer is not in alias list
    static final int SIGNED_BY_ALIAS = 0x08;    // signer is in alias list

    static final JavaUtilZipFileAccess JUZFA = SharedSecrets.getJavaUtilZipFileAccess();

    // Attention:
    // This is the entry that get launched by the security tool jarsigner.
    public static void main(String args[]) throws Exception {
        Main js = new Main();
        js.run(args);
    }

    X509Certificate[] certChain;    // signer's cert chain (when composing)
    PrivateKey privateKey;          // private key
    KeyStore store;                 // the keystore specified by -keystore
                                    // or the default keystore, never null

    String keystore; // key store file
    boolean nullStream = false; // null keystore input stream (NONE)
    boolean token = false; // token-based keystore
    String jarfile;  // jar files to sign or verify
    String alias;    // alias to sign jar with
    List<String> ckaliases = new ArrayList<>(); // aliases in -verify
    char[] storepass; // keystore password
    boolean protectedPath; // protected authentication path
    String storetype; // keystore type
    String providerName; // provider name
    List<String> providers = null; // list of provider names
    List<String> providerClasses = null; // list of provider classes
    // arguments for provider constructors
    HashMap<String,String> providerArgs = new HashMap<>();
    char[] keypass; // private key password
    String sigfile; // name of .SF file
    String sigalg; // name of signature algorithm
    String digestalg; // name of digest algorithm
    String signedjar; // output filename
    String tsaUrl; // location of the Timestamping Authority
    String tsaAlias; // alias for the Timestamping Authority's certificate
    String altCertChain; // file to read alternative cert chain from
    String tSAPolicyID;
    String tSADigestAlg;
    boolean verify = false; // verify the jar
    String verbose = null; // verbose output when signing/verifying
    boolean showcerts = false; // show certs when verifying
    boolean debug = false; // debug
    boolean signManifest = true; // "sign" the whole manifest
    boolean externalSF = true; // leave the .SF out of the PKCS7 block
    boolean strict = false;  // treat warnings as error
    boolean revocationCheck = false; // Revocation check flag

    // read zip entry raw bytes
    private String altSignerClass = null;
    private String altSignerClasspath = null;
    private ZipFile zipFile = null;

    // Informational warnings
    private boolean hasExpiringCert = false;
    private boolean hasExpiringTsaCert = false;
    private boolean noTimestamp = true;

    // Expiration date. The value could be null if signed by a trusted cert.
    private Date expireDate = null;
    private Date tsaExpireDate = null;

    // If there is a time stamp block inside the PKCS7 block file
    boolean hasTimestampBlock = false;

    private PublicKey weakPublicKey = null;
    private boolean disabledAlgFound = false;
    private String legacyDigestAlg = null;
    private String legacyTsaDigestAlg = null;
    private String legacySigAlg = null;

    // Severe warnings.

    // jarsigner used to check signer cert chain validity and key usages
    // itself and set various warnings. Later CertPath validation is
    // added but chainNotValidated is only flagged when no other existing
    // warnings are set. TSA cert chain check is added separately and
    // only tsaChainNotValidated is set, i.e. has no affect on hasExpiredCert,
    // notYetValidCert, or any badXyzUsage.

    private int legacyAlg = 0; // 1. digestalg, 2. sigalg, 4. tsadigestalg, 8. key
    private int disabledAlg = 0; // 1. digestalg, 2. sigalg, 4. tsadigestalg, 8. key
    private boolean hasExpiredCert = false;
    private boolean hasExpiredTsaCert = false;
    private boolean notYetValidCert = false;
    private boolean chainNotValidated = false;
    private boolean tsaChainNotValidated = false;
    private boolean notSignedByAlias = false;
    private boolean aliasNotInStore = false;
    private boolean hasUnsignedEntry = false;
    private boolean badKeyUsage = false;
    private boolean badExtendedKeyUsage = false;
    private boolean badNetscapeCertType = false;
    private boolean signerSelfSigned = false;

    private Throwable chainNotValidatedReason = null;
    private Throwable tsaChainNotValidatedReason = null;

    PKIXBuilderParameters pkixParameters;
    Set<X509Certificate> trustedCerts = new HashSet<>();

    public void run(String args[]) {
        try {
            args = parseArgs(args);

            // Try to load and install the specified providers
            if (providers != null) {
                for (String provName: providers) {
                    try {
                        KeyStoreUtil.loadProviderByName(provName,
                                providerArgs.get(provName));
                        if (debug) {
                            System.out.println("loadProviderByName: " + provName);
                        }
                    } catch (IllegalArgumentException e) {
                        throw new Exception(String.format(rb.getString(
                                "provider.name.not.found"), provName));
                    }
                }
            }

            if (providerClasses != null) {
                ClassLoader cl = ClassLoader.getSystemClassLoader();
                for (String provClass: providerClasses) {
                    try {
                        KeyStoreUtil.loadProviderByClass(provClass,
                                providerArgs.get(provClass), cl);
                        if (debug) {
                            System.out.println("loadProviderByClass: " + provClass);
                        }
                    } catch (ClassCastException cce) {
                        throw new Exception(String.format(rb.getString(
                                "provclass.not.a.provider"), provClass));
                    } catch (IllegalArgumentException e) {
                        throw new Exception(String.format(rb.getString(
                                "provider.class.not.found"), provClass), e.getCause());
                    }
                }
            }

            if (verify) {
                try {
                    loadKeyStore(keystore, false);
                } catch (Exception e) {
                    if ((keystore != null) || (storepass != null)) {
                        System.out.println(rb.getString("jarsigner.error.") +
                                        e.getMessage());
                        if (debug) {
                            e.printStackTrace();
                        }
                        System.exit(1);
                    }
                }
                /*              if (debug) {
                    SignatureFileVerifier.setDebug(true);
                    ManifestEntryVerifier.setDebug(true);
                }
                */
                verifyJar(jarfile);
            } else {
                loadKeyStore(keystore, true);
                getAliasInfo(alias);

                signJar(jarfile, alias);
            }
        } catch (Exception e) {
            System.out.println(rb.getString("jarsigner.error.") + e);
            if (debug) {
                e.printStackTrace();
            }
            System.exit(1);
        } finally {
            // zero-out private key password
            if (keypass != null) {
                Arrays.fill(keypass, ' ');
                keypass = null;
            }
            // zero-out keystore password
            if (storepass != null) {
                Arrays.fill(storepass, ' ');
                storepass = null;
            }
            Event.clearReportListener(Event.ReporterCategory.CRLCHECK);
        }

        if (strict) {
            int exitCode = 0;
            if (disabledAlg != 0 || chainNotValidated || hasExpiredCert
                    || hasExpiredTsaCert || notYetValidCert || signerSelfSigned) {
                exitCode |= 4;
            }
            if (badKeyUsage || badExtendedKeyUsage || badNetscapeCertType) {
                exitCode |= 8;
            }
            if (hasUnsignedEntry) {
                exitCode |= 16;
            }
            if (notSignedByAlias || aliasNotInStore) {
                exitCode |= 32;
            }
            if (tsaChainNotValidated) {
                exitCode |= 64;
            }
            if (exitCode != 0) {
                System.exit(exitCode);
            }
        }
    }

    /*
     * Parse command line arguments.
     */
    String[] parseArgs(String args[]) throws Exception {
        /* parse flags */
        int n = 0;

        if (args.length == 0) fullusage();

        String confFile = null;
        String command = "-sign";
        for (n=0; n < args.length; n++) {
            if (collator.compare(args[n], "-verify") == 0) {
                command = "-verify";
            } else if (collator.compare(args[n], "-conf") == 0) {
                if (n == args.length - 1) {
                    usageNoArg();
                }
                confFile = args[++n];
            }
        }

        if (confFile != null) {
            args = KeyStoreUtil.expandArgs(
                    "jarsigner", confFile, command, null, args);
        }

        debug = Arrays.stream(args).anyMatch(
                x -> collator.compare(x, "-debug") == 0);

        if (debug) {
            // No need to localize debug output
            System.out.println("Command line args: " +
                    Arrays.toString(args));
        }

        for (n=0; n < args.length; n++) {

            String flags = args[n];
            String modifier = null;

            if (flags.startsWith("-")) {
                int pos = flags.indexOf(':');
                if (pos > 0) {
                    modifier = flags.substring(pos+1);
                    flags = flags.substring(0, pos);
                }
            }

            if (!flags.startsWith("-")) {
                if (jarfile == null) {
                    jarfile = flags;
                } else {
                    alias = flags;
                    ckaliases.add(alias);
                }
            } else if (collator.compare(flags, "-conf") == 0) {
                if (++n == args.length) usageNoArg();
            } else if (collator.compare(flags, "-keystore") == 0) {
                if (++n == args.length) usageNoArg();
                keystore = args[n];
            } else if (collator.compare(flags, "-storepass") ==0) {
                if (++n == args.length) usageNoArg();
                storepass = getPass(modifier, args[n]);
            } else if (collator.compare(flags, "-storetype") ==0) {
                if (++n == args.length) usageNoArg();
                storetype = args[n];
            } else if (collator.compare(flags, "-providerName") ==0) {
                if (++n == args.length) usageNoArg();
                providerName = args[n];
            } else if (collator.compare(flags, "-provider") == 0 ||
                        collator.compare(flags, "-providerClass") == 0) {
                if (++n == args.length) usageNoArg();
                if (providerClasses == null) {
                    providerClasses = new ArrayList<>(3);
                }
                providerClasses.add(args[n]);

                if (args.length > (n+1)) {
                    flags = args[n+1];
                    if (collator.compare(flags, "-providerArg") == 0) {
                        if (args.length == (n+2)) usageNoArg();
                        providerArgs.put(args[n], args[n+2]);
                        n += 2;
                    }
                }
            } else if (collator.compare(flags, "-addprovider") == 0) {
                if (++n == args.length) usageNoArg();
                if (providers == null) {
                    providers = new ArrayList<>(3);
                }
                providers.add(args[n]);

                if (args.length > (n+1)) {
                    flags = args[n+1];
                    if (collator.compare(flags, "-providerArg") == 0) {
                        if (args.length == (n+2)) usageNoArg();
                        providerArgs.put(args[n], args[n+2]);
                        n += 2;
                    }
                }
            } else if (collator.compare(flags, "-protected") ==0) {
                protectedPath = true;
            } else if (collator.compare(flags, "-certchain") ==0) {
                if (++n == args.length) usageNoArg();
                altCertChain = args[n];
            } else if (collator.compare(flags, "-tsapolicyid") ==0) {
                if (++n == args.length) usageNoArg();
                tSAPolicyID = args[n];
            } else if (collator.compare(flags, "-tsadigestalg") ==0) {
                if (++n == args.length) usageNoArg();
                tSADigestAlg = args[n];
            } else if (collator.compare(flags, "-debug") ==0) {
                // Already processed
            } else if (collator.compare(flags, "-keypass") ==0) {
                if (++n == args.length) usageNoArg();
                keypass = getPass(modifier, args[n]);
            } else if (collator.compare(flags, "-sigfile") ==0) {
                if (++n == args.length) usageNoArg();
                sigfile = args[n];
            } else if (collator.compare(flags, "-signedjar") ==0) {
                if (++n == args.length) usageNoArg();
                signedjar = args[n];
            } else if (collator.compare(flags, "-tsa") ==0) {
                if (++n == args.length) usageNoArg();
                tsaUrl = args[n];
            } else if (collator.compare(flags, "-tsacert") ==0) {
                if (++n == args.length) usageNoArg();
                tsaAlias = args[n];
            } else if (collator.compare(flags, "-altsigner") ==0) {
                if (++n == args.length) usageNoArg();
                altSignerClass = args[n];
                System.err.println(
                        rb.getString("This.option.is.forremoval") +
                                "-altsigner");
            } else if (collator.compare(flags, "-altsignerpath") ==0) {
                if (++n == args.length) usageNoArg();
                altSignerClasspath = args[n];
                System.err.println(
                        rb.getString("This.option.is.forremoval") +
                                "-altsignerpath");
            } else if (collator.compare(flags, "-sectionsonly") ==0) {
                signManifest = false;
            } else if (collator.compare(flags, "-internalsf") ==0) {
                externalSF = false;
            } else if (collator.compare(flags, "-verify") ==0) {
                verify = true;
            } else if (collator.compare(flags, "-verbose") ==0) {
                verbose = (modifier != null) ? modifier : "all";
            } else if (collator.compare(flags, "-sigalg") ==0) {
                if (++n == args.length) usageNoArg();
                sigalg = args[n];
            } else if (collator.compare(flags, "-digestalg") ==0) {
                if (++n == args.length) usageNoArg();
                digestalg = args[n];
            } else if (collator.compare(flags, "-certs") ==0) {
                showcerts = true;
            } else if (collator.compare(flags, "-strict") ==0) {
                strict = true;
            } else if (collator.compare(flags, "-?") == 0 ||
                       collator.compare(flags, "-h") == 0 ||
                       collator.compare(flags, "--help") == 0 ||
                       // -help: legacy.
                       collator.compare(flags, "-help") == 0) {
                fullusage();
            } else if (collator.compare(flags, "-revCheck") == 0) {
                revocationCheck = true;
            } else {
                System.err.println(
                        rb.getString("Illegal.option.") + flags);
                usage();
            }
        }

        // -certs must always be specified with -verbose
        if (verbose == null) showcerts = false;

        if (jarfile == null) {
            System.err.println(rb.getString("Please.specify.jarfile.name"));
            usage();
        }
        if (!verify && alias == null) {
            System.err.println(rb.getString("Please.specify.alias.name"));
            usage();
        }
        if (!verify && ckaliases.size() > 1) {
            System.err.println(rb.getString("Only.one.alias.can.be.specified"));
            usage();
        }

        if (storetype == null) {
            storetype = KeyStore.getDefaultType();
        }
        storetype = KeyStoreUtil.niceStoreTypeName(storetype);

        try {
            if (signedjar != null && new File(signedjar).getCanonicalPath().equals(
                    new File(jarfile).getCanonicalPath())) {
                signedjar = null;
            }
        } catch (IOException ioe) {
            // File system error?
            // Just ignore it.
        }

        if (P11KEYSTORE.equalsIgnoreCase(storetype) ||
                KeyStoreUtil.isWindowsKeyStore(storetype)) {
            token = true;
            if (keystore == null) {
                keystore = NONE;
            }
        }

        if (NONE.equals(keystore)) {
            nullStream = true;
        }

        if (token && !nullStream) {
            System.err.println(MessageFormat.format(rb.getString
                (".keystore.must.be.NONE.if.storetype.is.{0}"), storetype));
            usage();
        }

        if (token && keypass != null) {
            System.err.println(MessageFormat.format(rb.getString
                (".keypass.can.not.be.specified.if.storetype.is.{0}"), storetype));
            usage();
        }

        if (protectedPath) {
            if (storepass != null || keypass != null) {
                System.err.println(rb.getString
                        ("If.protected.is.specified.then.storepass.and.keypass.must.not.be.specified"));
                usage();
            }
        }
        if (KeyStoreUtil.isWindowsKeyStore(storetype)) {
            if (storepass != null || keypass != null) {
                System.err.println(rb.getString
                        ("If.keystore.is.not.password.protected.then.storepass.and.keypass.must.not.be.specified"));
                usage();
            }
        }
        return args;
    }

    static char[] getPass(String modifier, String arg) {
        char[] output =
            KeyStoreUtil.getPassWithModifier(modifier, arg, rb, collator);
        if (output != null) return output;
        usage();
        return null;    // Useless, usage() already exit
    }

    static void usageNoArg() {
        System.out.println(rb.getString("Option.lacks.argument"));
        usage();
    }

    static void usage() {
        System.out.println();
        System.out.println(rb.getString("Please.type.jarsigner.help.for.usage"));
        System.exit(1);
    }

    static void fullusage() {
        System.out.println(rb.getString
                ("Usage.jarsigner.options.jar.file.alias"));
        System.out.println(rb.getString
                (".jarsigner.verify.options.jar.file.alias."));
        System.out.println();
        System.out.println(rb.getString
                (".keystore.url.keystore.location"));
        System.out.println();
        System.out.println(rb.getString
                (".storepass.password.password.for.keystore.integrity"));
        System.out.println();
        System.out.println(rb.getString
                (".storetype.type.keystore.type"));
        System.out.println();
        System.out.println(rb.getString
                (".keypass.password.password.for.private.key.if.different."));
        System.out.println();
        System.out.println(rb.getString
                (".certchain.file.name.of.alternative.certchain.file"));
        System.out.println();
        System.out.println(rb.getString
                (".sigfile.file.name.of.SF.DSA.file"));
        System.out.println();
        System.out.println(rb.getString
                (".signedjar.file.name.of.signed.JAR.file"));
        System.out.println();
        System.out.println(rb.getString
                (".digestalg.algorithm.name.of.digest.algorithm"));
        System.out.println();
        System.out.println(rb.getString
                (".sigalg.algorithm.name.of.signature.algorithm"));
        System.out.println();
        System.out.println(rb.getString
                (".verify.verify.a.signed.JAR.file"));
        System.out.println();
        System.out.println(rb.getString
                (".verbose.suboptions.verbose.output.when.signing.verifying."));
        System.out.println(rb.getString
                (".suboptions.can.be.all.grouped.or.summary"));
        System.out.println();
        System.out.println(rb.getString
                (".certs.display.certificates.when.verbose.and.verifying"));
        System.out.println();
        System.out.println(rb.getString
                (".certs.revocation.check"));
        System.out.println();
        System.out.println(rb.getString
                (".tsa.url.location.of.the.Timestamping.Authority"));
        System.out.println();
        System.out.println(rb.getString
                (".tsacert.alias.public.key.certificate.for.Timestamping.Authority"));
        System.out.println();
        System.out.println(rb.getString
                (".tsapolicyid.tsapolicyid.for.Timestamping.Authority"));
        System.out.println();
        System.out.println(rb.getString
                (".tsadigestalg.algorithm.of.digest.data.in.timestamping.request"));
        System.out.println();
        System.out.println(rb.getString
                (".altsigner.class.class.name.of.an.alternative.signing.mechanism"));
        System.out.println();
        System.out.println(rb.getString
                (".altsignerpath.pathlist.location.of.an.alternative.signing.mechanism"));
        System.out.println();
        System.out.println(rb.getString
                (".internalsf.include.the.SF.file.inside.the.signature.block"));
        System.out.println();
        System.out.println(rb.getString
                (".sectionsonly.don.t.compute.hash.of.entire.manifest"));
        System.out.println();
        System.out.println(rb.getString
                (".protected.keystore.has.protected.authentication.path"));
        System.out.println();
        System.out.println(rb.getString
                (".providerName.name.provider.name"));
        System.out.println();
        System.out.println(rb.getString
                (".add.provider.option"));
        System.out.println(rb.getString
                (".providerArg.option.1"));
        System.out.println();
        System.out.println(rb.getString
                (".providerClass.option"));
        System.out.println(rb.getString
                (".providerArg.option.2"));
        System.out.println();
        System.out.println(rb.getString
                (".strict.treat.warnings.as.errors"));
        System.out.println();
        System.out.println(rb.getString
                (".conf.url.specify.a.pre.configured.options.file"));
        System.out.println();
        System.out.println(rb.getString
                (".print.this.help.message"));
        System.out.println();

        System.exit(0);
    }

    void verifyJar(String jarName)
        throws Exception
    {
        boolean anySigned = false;  // if there exists entry inside jar signed
        JarFile jf = null;
        Map<String,String> digestMap = new HashMap<>();
        Map<String,PKCS7> sigMap = new HashMap<>();
        Map<String,String> sigNameMap = new HashMap<>();
        Map<String,String> unparsableSignatures = new HashMap<>();

        try {
            jf = new JarFile(jarName, true);
            Vector<JarEntry> entriesVec = new Vector<>();
            byte[] buffer = new byte[8192];

            String suffix1 = "-Digest-Manifest";
            String suffix2 = "-Digest-" + ManifestDigester.MF_MAIN_ATTRS;

            int suffixLength1 = suffix1.length();
            int suffixLength2 = suffix2.length();

            Enumeration<JarEntry> entries = jf.entries();
            while (entries.hasMoreElements()) {
                JarEntry je = entries.nextElement();
                entriesVec.addElement(je);
                try (InputStream is = jf.getInputStream(je)) {
                    String name = je.getName();
                    if (signatureRelated(name)
                            && SignatureFileVerifier.isBlockOrSF(name)) {
                        String alias = name.substring(name.lastIndexOf('/') + 1,
                                name.lastIndexOf('.'));
                        try {
                            if (name.endsWith(".SF")) {
                                Manifest sf = new Manifest(is);
                                boolean found = false;
                                for (Object obj : sf.getMainAttributes().keySet()) {
                                    String key = obj.toString();
                                    if (key.endsWith(suffix1)) {
                                        digestMap.put(alias, key.substring(
                                                0, key.length() - suffixLength1));
                                        found = true;
                                        break;
                                    } else if (key.endsWith(suffix2)) {
                                        digestMap.put(alias, key.substring(
                                                0, key.length() - suffixLength2));
                                        found = true;
                                        break;
                                    }
                                }
                                if (!found) {
                                    unparsableSignatures.putIfAbsent(alias,
                                        String.format(
                                            rb.getString("history.unparsable"),
                                            name));
                                }
                            } else {
                                sigNameMap.put(alias, name);
                                sigMap.put(alias, new PKCS7(is));
                            }
                        } catch (IOException ioe) {
                            unparsableSignatures.putIfAbsent(alias, String.format(
                                    rb.getString("history.unparsable"), name));
                        }
                    } else {
                        while (is.read(buffer, 0, buffer.length) != -1) {
                            // we just read. this will throw a SecurityException
                            // if  a signature/digest check fails.
                        }
                    }
                }
            }

            Manifest man = jf.getManifest();
            boolean hasSignature = false;

            // The map to record display info, only used when -verbose provided
            //      key: signer info string
            //      value: the list of files with common key
            Map<String,List<String>> output = new LinkedHashMap<>();

            if (man != null) {
                if (verbose != null) System.out.println();
                Enumeration<JarEntry> e = entriesVec.elements();

                String tab = rb.getString("6SPACE");

                while (e.hasMoreElements()) {
                    JarEntry je = e.nextElement();
                    String name = je.getName();

                    if (!extraAttrsDetected && JUZFA.getExtraAttributes(je) != -1) {
                        extraAttrsDetected = true;
                    }
                    hasSignature = hasSignature
                            || SignatureFileVerifier.isBlockOrSF(name);

                    CodeSigner[] signers = je.getCodeSigners();
                    boolean isSigned = (signers != null);
                    anySigned |= isSigned;

                    boolean unsignedEntry = !isSigned
                            && ((!je.isDirectory() && !signatureRelated(name))
                            // a directory entry but with a suspicious size
                            || (je.isDirectory() && je.getSize() > 0));
                    hasUnsignedEntry |= unsignedEntry;

                    int inStoreWithAlias = inKeyStore(signers);

                    boolean inStore = (inStoreWithAlias & IN_KEYSTORE) != 0;

                    notSignedByAlias |= (inStoreWithAlias & NOT_ALIAS) != 0;
                    if (keystore != null) {
                        aliasNotInStore |= isSigned && !inStore;
                    }

                    // Only used when -verbose provided
                    StringBuffer sb = null;
                    if (verbose != null) {
                        sb = new StringBuffer();
                        boolean inManifest =
                            ((man.getAttributes(name) != null) ||
                             (man.getAttributes("./"+name) != null) ||
                             (man.getAttributes("/"+name) != null));
                        sb.append(isSigned ? rb.getString("s") : rb.getString("SPACE"))
                                .append(inManifest ? rb.getString("m") : rb.getString("SPACE"))
                                .append(inStore ? rb.getString("k") : rb.getString("SPACE"))
                                .append((inStoreWithAlias & NOT_ALIAS) != 0 ?
                                 rb.getString("X") : rb.getString("SPACE"))
                                .append(unsignedEntry ? rb.getString("q") : rb.getString("SPACE"))
                                .append(rb.getString("SPACE"));
                        sb.append('|');
                    }

                    // When -certs provided, display info has extra empty
                    // lines at the beginning and end.
                    if (isSigned) {
                        if (showcerts) sb.append('\n');
                        for (CodeSigner signer: signers) {
                            // signerInfo() must be called even if -verbose
                            // not provided. The method updates various
                            // warning flags.
                            String si = signerInfo(signer, tab);
                            if (showcerts) {
                                sb.append(si);
                                sb.append('\n');
                            }
                        }
                    } else if (showcerts && !verbose.equals("all")) {
                        // Print no info for unsigned entries when -verbose:all,
                        // to be consistent with old behavior.
                        if (signatureRelated(name)) {
                            sb.append('\n')
                                    .append(tab)
                                    .append(rb
                                            .getString(".Signature.related.entries."))
                                    .append("\n\n");
                        } else if (unsignedEntry) {
                            sb.append('\n').append(tab)
                                    .append(rb.getString(".Unsigned.entries."))
                                    .append("\n\n");
                        } else {
                            sb.append('\n').append(tab)
                                    .append(rb.getString(".Directory.entries."))
                                    .append("\n\n");
                        }
                    }

                    if (verbose != null) {
                        String label = sb.toString();
                        if (signatureRelated(name)) {
                            // Entries inside META-INF and other unsigned
                            // entries are grouped separately.
                            label = "-" + label;
                        }

                        // The label finally contains 2 parts separated by '|':
                        // The legend displayed before the entry names, and
                        // the cert info (if -certs specified).

                        if (!output.containsKey(label)) {
                            output.put(label, new ArrayList<String>());
                        }

                        StringBuilder fb = new StringBuilder();
                        String s = Long.toString(je.getSize());
                        for (int i = 6 - s.length(); i > 0; --i) {
                            fb.append(' ');
                        }
                        fb.append(s).append(' ').
                                append(new Date(je.getTime()).toString());
                        fb.append(' ').append(name);

                        output.get(label).add(fb.toString());
                    }
                }
            }
            if (verbose != null) {
                for (Entry<String,List<String>> s: output.entrySet()) {
                    List<String> files = s.getValue();
                    String key = s.getKey();
                    if (key.charAt(0) == '-') { // the signature-related group
                        key = key.substring(1);
                    }
                    int pipe = key.indexOf('|');
                    if (verbose.equals("all")) {
                        for (String f: files) {
                            System.out.println(key.substring(0, pipe) + f);
                            System.out.printf(key.substring(pipe+1));
                        }
                    } else {
                        if (verbose.equals("grouped")) {
                            for (String f: files) {
                                System.out.println(key.substring(0, pipe) + f);
                            }
                        } else if (verbose.equals("summary")) {
                            System.out.print(key.substring(0, pipe));
                            if (files.size() > 1) {
                                System.out.println(files.get(0) + " " +
                                        String.format(rb.getString(
                                        ".and.d.more."), files.size()-1));
                            } else {
                                System.out.println(files.get(0));
                            }
                        }
                        System.out.printf(key.substring(pipe+1));
                    }
                }
                System.out.println();
                System.out.println(rb.getString(
                    ".s.signature.was.verified."));
                System.out.println(rb.getString(
                    ".m.entry.is.listed.in.manifest"));
                System.out.println(rb.getString(
                    ".k.at.least.one.certificate.was.found.in.keystore"));
                if (ckaliases.size() > 0) {
                    System.out.println(rb.getString(
                        ".X.not.signed.by.specified.alias.es."));
                }

                if (hasUnsignedEntry) {
                    System.out.println(rb.getString(
                            ".q.unsigned.entry"));
                }
            }
            if (man == null) {
                System.out.println();
                System.out.println(rb.getString("no.manifest."));
            }

            // If signer is a trusted cert or private entry in user's own
            // keystore, it can be self-signed. Please note aliasNotInStore
            // is always false when ~/.keystore is used.
            if (!aliasNotInStore && keystore != null) {
                signerSelfSigned = false;
            }

            // Even if the verbose option is not specified, all out strings
            // must be generated so disabledAlgFound can be updated.
            if (!digestMap.isEmpty()
                    || !sigMap.isEmpty()
                    || !unparsableSignatures.isEmpty()) {
                if (verbose != null) {
                    System.out.println();
                }
                for (String s : sigMap.keySet()) {
                    if (!digestMap.containsKey(s)) {
                        unparsableSignatures.putIfAbsent(s, String.format(
                                rb.getString("history.nosf"), s));
                    }
                }
                for (String s : digestMap.keySet()) {
                    PKCS7 p7 = sigMap.get(s);
                    if (p7 != null) {
                        String history;
                        try {
                            SignerInfo si = p7.getSignerInfos()[0];
                            X509Certificate signer = si.getCertificate(p7);
                            String digestAlg = digestMap.get(s);
                            String sigAlg = SignerInfo.makeSigAlg(
                                    si.getDigestAlgorithmId(),
                                    si.getDigestEncryptionAlgorithmId(),
                                    si.getAuthenticatedAttributes() == null);
                            PublicKey key = signer.getPublicKey();
                            PKCS7 tsToken = si.getTsToken();
                            if (tsToken != null) {
                                hasTimestampBlock = true;
                                SignerInfo tsSi = tsToken.getSignerInfos()[0];
                                X509Certificate tsSigner = tsSi.getCertificate(tsToken);
                                byte[] encTsTokenInfo = tsToken.getContentInfo().getData();
                                TimestampToken tsTokenInfo = new TimestampToken(encTsTokenInfo);
                                PublicKey tsKey = tsSigner.getPublicKey();
                                String tsDigestAlg = tsTokenInfo.getHashAlgorithm().getName();
                                String tsSigAlg = SignerInfo.makeSigAlg(
                                        tsSi.getDigestAlgorithmId(),
                                        tsSi.getDigestEncryptionAlgorithmId(),
                                        tsSi.getAuthenticatedAttributes() == null);
                                Calendar c = Calendar.getInstance(
                                        TimeZone.getTimeZone("UTC"),
                                        Locale.getDefault(Locale.Category.FORMAT));
                                c.setTime(tsTokenInfo.getDate());
                                history = String.format(
                                        rb.getString("history.with.ts"),
                                        signer.getSubjectX500Principal(),
                                        verifyWithWeak(digestAlg, DIGEST_PRIMITIVE_SET, false),
                                        verifyWithWeak(sigAlg, SIG_PRIMITIVE_SET, false),
                                        verifyWithWeak(key),
                                        c,
                                        tsSigner.getSubjectX500Principal(),
                                        verifyWithWeak(tsDigestAlg, DIGEST_PRIMITIVE_SET, true),
                                        verifyWithWeak(tsSigAlg, SIG_PRIMITIVE_SET, true),
                                        verifyWithWeak(tsKey));
                            } else {
                                history = String.format(
                                        rb.getString("history.without.ts"),
                                        signer.getSubjectX500Principal(),
                                        verifyWithWeak(digestAlg, DIGEST_PRIMITIVE_SET, false),
                                        verifyWithWeak(sigAlg, SIG_PRIMITIVE_SET, false),
                                        verifyWithWeak(key));
                            }
                        } catch (Exception e) {
                            // The only usage of sigNameMap, remember the name
                            // of the block file if it's invalid.
                            history = String.format(
                                    rb.getString("history.unparsable"),
                                    sigNameMap.get(s));
                        }
                        if (verbose != null) {
                            System.out.println(history);
                        }
                    } else {
                        unparsableSignatures.putIfAbsent(s, String.format(
                                rb.getString("history.nobk"), s));
                    }
                }
                if (verbose != null) {
                    for (String s : unparsableSignatures.keySet()) {
                        System.out.println(unparsableSignatures.get(s));
                    }
                }
            }
            System.out.println();

            if (!anySigned) {
                if (disabledAlgFound) {
                    if (verbose != null) {
                        System.out.println(rb.getString("jar.treated.unsigned.see.weak.verbose"));
                        System.out.println("\n  " +
                                DisabledAlgorithmConstraints.PROPERTY_JAR_DISABLED_ALGS +
                                "=" + Security.getProperty(DisabledAlgorithmConstraints.PROPERTY_JAR_DISABLED_ALGS));
                    } else {
                        System.out.println(rb.getString("jar.treated.unsigned.see.weak"));
                    }
                } else if (hasSignature) {
                    System.out.println(rb.getString("jar.treated.unsigned"));
                } else {
                    System.out.println(rb.getString("jar.is.unsigned"));
                }
            } else {
                displayMessagesAndResult(false);
            }
            return;
        } catch (Exception e) {
            System.out.println(rb.getString("jarsigner.") + e);
            if (debug) {
                e.printStackTrace();
            }
        } finally { // close the resource
            if (jf != null) {
                jf.close();
            }
        }

        System.exit(1);
    }

    private void displayMessagesAndResult(boolean isSigning) {
        String result;
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        List<String> info = new ArrayList<>();

        boolean signerNotExpired = expireDate == null
                || expireDate.after(new Date());

        if (badKeyUsage) {
            errors.add(isSigning
                    ? rb.getString("The.signer.certificate.s.KeyUsage.extension.doesn.t.allow.code.signing.")
                    : rb.getString("This.jar.contains.entries.whose.signer.certificate.s.KeyUsage.extension.doesn.t.allow.code.signing."));
        }

        if (badExtendedKeyUsage) {
            errors.add(isSigning
                    ? rb.getString("The.signer.certificate.s.ExtendedKeyUsage.extension.doesn.t.allow.code.signing.")
                    : rb.getString("This.jar.contains.entries.whose.signer.certificate.s.ExtendedKeyUsage.extension.doesn.t.allow.code.signing."));
        }

        if (badNetscapeCertType) {
            errors.add(isSigning
                    ? rb.getString("The.signer.certificate.s.NetscapeCertType.extension.doesn.t.allow.code.signing.")
                    : rb.getString("This.jar.contains.entries.whose.signer.certificate.s.NetscapeCertType.extension.doesn.t.allow.code.signing."));
        }

        // only in verifying
        if (hasUnsignedEntry) {
            errors.add(rb.getString(
                    "This.jar.contains.unsigned.entries.which.have.not.been.integrity.checked."));
        }

        if (hasExpiredCert) {
            errors.add(isSigning
                    ? rb.getString("The.signer.certificate.has.expired.")
                    : rb.getString("This.jar.contains.entries.whose.signer.certificate.has.expired."));
        }

        if (notYetValidCert) {
            errors.add(isSigning
                    ? rb.getString("The.signer.certificate.is.not.yet.valid.")
                    : rb.getString("This.jar.contains.entries.whose.signer.certificate.is.not.yet.valid."));
        }

        if (chainNotValidated) {
            errors.add(String.format(isSigning
                            ? rb.getString("The.signer.s.certificate.chain.is.invalid.reason.1")
                            : rb.getString("This.jar.contains.entries.whose.certificate.chain.is.invalid.reason.1"),
                    chainNotValidatedReason.getLocalizedMessage()));
        }

        if (tsaChainNotValidated) {
            errors.add(String.format(isSigning
                            ? rb.getString("The.tsa.certificate.chain.is.invalid.reason.1")
                            : rb.getString("This.jar.contains.entries.whose.tsa.certificate.chain.is.invalid.reason.1"),
                    tsaChainNotValidatedReason.getLocalizedMessage()));
        }

        // only in verifying
        if (notSignedByAlias) {
            errors.add(
                    rb.getString("This.jar.contains.signed.entries.which.is.not.signed.by.the.specified.alias.es."));
        }

        // only in verifying
        if (aliasNotInStore) {
            errors.add(rb.getString("This.jar.contains.signed.entries.that.s.not.signed.by.alias.in.this.keystore."));
        }

        if (signerSelfSigned) {
            errors.add(isSigning
                    ? rb.getString("The.signer.s.certificate.is.self.signed.")
                    : rb.getString("This.jar.contains.entries.whose.signer.certificate.is.self.signed."));
        }

        if (isSigning) {
            if ((legacyAlg & 1) == 1) {
                warnings.add(String.format(
                        rb.getString("The.1.algorithm.specified.for.the.2.option.is.considered.a.security.risk..This.algorithm.will.be.disabled.in.a.future.update."),
                        digestalg, "-digestalg"));
            }

            if ((disabledAlg & 1) == 1) {
                errors.add(String.format(
                        rb.getString("The.1.algorithm.specified.for.the.2.option.is.considered.a.security.risk.and.is.disabled."),
                        digestalg, "-digestalg"));
            }

            if ((legacyAlg & 2) == 2) {
                warnings.add(String.format(
                        rb.getString("The.1.algorithm.specified.for.the.2.option.is.considered.a.security.risk..This.algorithm.will.be.disabled.in.a.future.update."),
                        sigalg, "-sigalg"));
            }

            if ((disabledAlg & 2) == 2) {
                errors.add(String.format(
                        rb.getString("The.1.algorithm.specified.for.the.2.option.is.considered.a.security.risk.and.is.disabled."),
                        sigalg, "-sigalg"));
            }

            if ((legacyAlg & 4) == 4) {
                warnings.add(String.format(
                        rb.getString("The.1.algorithm.specified.for.the.2.option.is.considered.a.security.risk..This.algorithm.will.be.disabled.in.a.future.update."),
                        tSADigestAlg, "-tsadigestalg"));
            }

            if ((disabledAlg & 4) == 4) {
                errors.add(String.format(
                        rb.getString("The.1.algorithm.specified.for.the.2.option.is.considered.a.security.risk.and.is.disabled."),
                        tSADigestAlg, "-tsadigestalg"));
            }

            if ((legacyAlg & 8) == 8) {
                warnings.add(String.format(
                        rb.getString("The.1.signing.key.has.a.keysize.of.2.which.is.considered.a.security.risk..This.key.size.will.be.disabled.in.a.future.update."),
                        privateKey.getAlgorithm(), KeyUtil.getKeySize(privateKey)));
            }

            if ((disabledAlg & 8) == 8) {
                errors.add(String.format(
                        rb.getString("The.1.signing.key.has.a.keysize.of.2.which.is.considered.a.security.risk.and.is.disabled."),
                        privateKey.getAlgorithm(), KeyUtil.getKeySize(privateKey)));
            }
        } else {
            if ((legacyAlg & 1) != 0) {
                warnings.add(String.format(
                        rb.getString("The.digest.algorithm.1.is.considered.a.security.risk..This.algorithm.will.be.disabled.in.a.future.update."),
                        legacyDigestAlg));
            }

            if ((legacyAlg & 2) == 2) {
                warnings.add(String.format(
                        rb.getString("The.signature.algorithm.1.is.considered.a.security.risk..This.algorithm.will.be.disabled.in.a.future.update."),
                        legacySigAlg));
            }

            if ((legacyAlg & 4) != 0) {
                warnings.add(String.format(
                        rb.getString("The.timestamp.digest.algorithm.1.is.considered.a.security.risk..This.algorithm.will.be.disabled.in.a.future.update."),
                        legacyTsaDigestAlg));
            }

            if ((legacyAlg & 8) == 8) {
                warnings.add(String.format(
                        rb.getString("The.1.signing.key.has.a.keysize.of.2.which.is.considered.a.security.risk..This.key.size.will.be.disabled.in.a.future.update."),
                        weakPublicKey.getAlgorithm(), KeyUtil.getKeySize(weakPublicKey)));
            }
        }

        // This check must be placed after all other "errors.add()" calls were done.
        if (hasExpiredTsaCert) {
            // No need to warn about expiring if already expired
            hasExpiringTsaCert = false;
            // If there are already another errors, we just say it expired.
            if (!signerNotExpired || !errors.isEmpty()) {
                errors.add(rb.getString("The.timestamp.has.expired."));
            } else if (signerNotExpired) {
                if (expireDate != null) {
                    warnings.add(String.format(
                            rb.getString("The.timestamp.expired.1.but.usable.2"),
                            tsaExpireDate,
                            expireDate));
                }
                // Reset the flag so exit code is 0
                hasExpiredTsaCert = false;
            }
        }

        if (hasExpiringCert) {
            warnings.add(isSigning
                    ? rb.getString("The.signer.certificate.will.expire.within.six.months.")
                    : rb.getString("This.jar.contains.entries.whose.signer.certificate.will.expire.within.six.months."));
        }

        if (hasExpiringTsaCert && expireDate != null) {
            if (expireDate.after(tsaExpireDate)) {
                warnings.add(String.format(rb.getString(
                        "The.timestamp.will.expire.within.one.year.on.1.but.2"), tsaExpireDate, expireDate));
            } else {
                warnings.add(String.format(rb.getString(
                        "The.timestamp.will.expire.within.one.year.on.1"), tsaExpireDate));
            }
        }

        if (noTimestamp && expireDate != null) {
            if (hasTimestampBlock) {
                warnings.add(String.format(isSigning
                        ? rb.getString("invalid.timestamp.signing")
                        : rb.getString("bad.timestamp.verifying"), expireDate));
            } else {
                warnings.add(String.format(isSigning
                        ? rb.getString("no.timestamp.signing")
                        : rb.getString("no.timestamp.verifying"), expireDate));
            }
        }

        if (extraAttrsDetected) {
            warnings.add(rb.getString("extra.attributes.detected"));
        }

        if ((strict) && (!errors.isEmpty())) {
            result = isSigning
                    ? rb.getString("jar.signed.with.signer.errors.")
                    : rb.getString("jar.verified.with.signer.errors.");
        } else {
            result = isSigning
                    ? rb.getString("jar.signed.")
                    : rb.getString("jar.verified.");
        }
        System.out.println(result);

        if (strict) {
            if (!errors.isEmpty()) {
                System.out.println();
                System.out.println(rb.getString("Error."));
                errors.forEach(System.out::println);
            }
            if (!warnings.isEmpty()) {
                System.out.println();
                System.out.println(rb.getString("Warning."));
                warnings.forEach(System.out::println);
            }
        } else {
            if (!errors.isEmpty() || !warnings.isEmpty()) {
                System.out.println();
                System.out.println(rb.getString("Warning."));
                errors.forEach(System.out::println);
                warnings.forEach(System.out::println);
            }
        }

        if (!isSigning && (!errors.isEmpty() || !warnings.isEmpty())) {
            if (! (verbose != null && showcerts)) {
                System.out.println();
                System.out.println(rb.getString(
                        "Re.run.with.the.verbose.and.certs.options.for.more.details."));
            }
        }

        if (isSigning || verbose != null) {
            // Always print out expireDate, unless expired or expiring.
            if (!hasExpiringCert && !hasExpiredCert
                    && expireDate != null && signerNotExpired) {
                info.add(String.format(rb.getString(
                        "The.signer.certificate.will.expire.on.1."), expireDate));
            }
            if (!noTimestamp) {
                if (!hasExpiringTsaCert && !hasExpiredTsaCert && tsaExpireDate != null) {
                    if (signerNotExpired) {
                        info.add(String.format(rb.getString(
                                "The.timestamp.will.expire.on.1."), tsaExpireDate));
                    } else {
                        info.add(String.format(rb.getString(
                                "signer.cert.expired.1.but.timestamp.good.2."),
                                expireDate,
                                tsaExpireDate));
                    }
                }
            }
        }

        if (!info.isEmpty()) {
            System.out.println();
            info.forEach(System.out::println);
        }
    }

    private String verifyWithWeak(String alg, Set<CryptoPrimitive> primitiveSet, boolean tsa) {
        if (JAR_DISABLED_CHECK.permits(primitiveSet, alg, null)) {
            if (LEGACY_CHECK.permits(primitiveSet, alg, null)) {
                return alg;
            } else {
                if (primitiveSet == SIG_PRIMITIVE_SET) {
                   legacyAlg |= 2;
                   legacySigAlg = alg;
                } else {
                    if (tsa) {
                        legacyAlg |= 4;
                        legacyTsaDigestAlg = alg;
                    } else {
                        legacyAlg |= 1;
                        legacyDigestAlg = alg;
                    }
                }
                return String.format(rb.getString("with.weak"), alg);
            }
        } else {
            disabledAlgFound = true;
            return String.format(rb.getString("with.disabled"), alg);
        }
    }

    private String verifyWithWeak(PublicKey key) {
        int kLen = KeyUtil.getKeySize(key);
        if (JAR_DISABLED_CHECK.permits(SIG_PRIMITIVE_SET, key)) {
            if (LEGACY_CHECK.permits(SIG_PRIMITIVE_SET, key)) {
                if (kLen >= 0) {
                    return String.format(rb.getString("key.bit"), kLen);
                } else {
                    return rb.getString("unknown.size");
                }
            } else {
                weakPublicKey = key;
                legacyAlg |= 8;
                return String.format(rb.getString("key.bit.weak"), kLen);
            }
        } else {
           disabledAlgFound = true;
           return String.format(rb.getString("key.bit.disabled"), kLen);
        }
    }

    private void checkWeakSign(String alg, Set<CryptoPrimitive> primitiveSet, boolean tsa) {
        if (JAR_DISABLED_CHECK.permits(primitiveSet, alg, null)) {
            if (!LEGACY_CHECK.permits(primitiveSet, alg, null)) {
                if (primitiveSet == SIG_PRIMITIVE_SET) {
                   legacyAlg |= 2;
                } else {
                    if (tsa) {
                        legacyAlg |= 4;
                    } else {
                        legacyAlg |= 1;
                    }
                }
            }
        } else {
           if (primitiveSet == SIG_PRIMITIVE_SET) {
               disabledAlg |= 2;
           } else {
               if (tsa) {
                   disabledAlg |= 4;
               } else {
                   disabledAlg |= 1;
               }
           }
        }
    }

    private void checkWeakSign(PrivateKey key) {
        if (JAR_DISABLED_CHECK.permits(SIG_PRIMITIVE_SET, key)) {
            if (!LEGACY_CHECK.permits(SIG_PRIMITIVE_SET, key)) {
                legacyAlg |= 8;
            }
        } else {
            disabledAlg |= 8;
        }
    }

    private static String checkWeakKey(PublicKey key) {
        int kLen = KeyUtil.getKeySize(key);
        if (CERTPATH_DISABLED_CHECK.permits(SIG_PRIMITIVE_SET, key)) {
            if (LEGACY_CHECK.permits(SIG_PRIMITIVE_SET, key)) {
                if (kLen >= 0) {
                    return String.format(rb.getString("key.bit"), kLen);
                } else {
                    return rb.getString("unknown.size");
                }
            } else {
                return String.format(rb.getString("key.bit.weak"), kLen);
            }
        } else {
           return String.format(rb.getString("key.bit.disabled"), kLen);
        }
    }

    private static String checkWeakAlg(String alg) {
        if (CERTPATH_DISABLED_CHECK.permits(SIG_PRIMITIVE_SET, alg, null)) {
            if (LEGACY_CHECK.permits(SIG_PRIMITIVE_SET, alg, null)) {
                return alg;
            } else {
                return String.format(rb.getString("with.weak"), alg);
            }
        } else {
            return String.format(rb.getString("with.disabled"), alg);
        }
    }

    private static MessageFormat validityTimeForm = null;
    private static MessageFormat notYetTimeForm = null;
    private static MessageFormat expiredTimeForm = null;
    private static MessageFormat expiringTimeForm = null;

    /**
     * Returns a string about a certificate:
     *
     * [<tab>] <cert-type> [", " <subject-DN>] [" (" <keystore-entry-alias> ")"]
     * [<validity-period> | <expiry-warning>]
     * [<key-usage-warning>]
     *
     * Note: no newline character at the end.
     *
     * This method sets global flags like hasExpiringCert, hasExpiredCert,
     * notYetValidCert, badKeyUsage, badExtendedKeyUsage, badNetscapeCertType,
     * hasExpiringTsaCert, hasExpiredTsaCert.
     *
     * @param isTsCert true if c is in the TSA cert chain, false otherwise.
     * @param checkUsage true to check code signer keyUsage
     */
    String printCert(boolean isTsCert, String tab, Certificate c,
        Date timestamp, boolean checkUsage) throws Exception {

        StringBuilder certStr = new StringBuilder();
        String space = rb.getString("SPACE");
        X509Certificate x509Cert = null;

        if (c instanceof X509Certificate) {
            x509Cert = (X509Certificate) c;
            certStr.append(tab).append(x509Cert.getType())
                .append(rb.getString("COMMA"))
                .append(x509Cert.getSubjectX500Principal().toString());
        } else {
            certStr.append(tab).append(c.getType());
        }

        String alias = storeHash.get(c);
        if (alias != null) {
            certStr.append(space).append("(").append(alias).append(")");
        }

        if (x509Cert != null) {
            PublicKey key = x509Cert.getPublicKey();
            String sigalg = x509Cert.getSigAlgName();

            // Process the certificate in the signer's cert chain to see if
            // weak algorithms are used, and provide warnings as needed.
            if (trustedCerts.contains(x509Cert)) {
                // If the cert is trusted, only check its key size, but not its
                // signature algorithm.
                certStr.append("\n").append(tab)
                        .append("Signature algorithm: ")
                        .append(sigalg)
                        .append(rb.getString("COMMA"))
                        .append(checkWeakKey(key));

                certStr.append("\n").append(tab).append("[");
                certStr.append(rb.getString("trusted.certificate"));
            } else {
                certStr.append("\n").append(tab)
                        .append("Signature algorithm: ")
                        .append(checkWeakAlg(sigalg))
                        .append(rb.getString("COMMA"))
                        .append(checkWeakKey(key));

                certStr.append("\n").append(tab).append("[");

                Date notAfter = x509Cert.getNotAfter();
                try {
                    boolean printValidity = true;
                    if (isTsCert) {
                        if (tsaExpireDate == null || tsaExpireDate.after(notAfter)) {
                            tsaExpireDate = notAfter;
                        }
                    } else {
                        if (expireDate == null || expireDate.after(notAfter)) {
                            expireDate = notAfter;
                        }
                    }
                    if (timestamp == null) {
                        x509Cert.checkValidity();
                        // test if cert will expire within six months (or one year for tsa)
                        long age = isTsCert ? ONE_YEAR : SIX_MONTHS;
                        if (notAfter.getTime() < System.currentTimeMillis() + age) {
                            if (isTsCert) {
                                hasExpiringTsaCert = true;
                            } else {
                                hasExpiringCert = true;
                            }
                            if (expiringTimeForm == null) {
                                expiringTimeForm = new MessageFormat(
                                        rb.getString("certificate.will.expire.on"));
                            }
                            Object[] source = {notAfter};
                            certStr.append(expiringTimeForm.format(source));
                            printValidity = false;
                        }
                    } else {
                        x509Cert.checkValidity(timestamp);
                    }
                    if (printValidity) {
                        if (validityTimeForm == null) {
                            validityTimeForm = new MessageFormat(
                                    rb.getString("certificate.is.valid.from"));
                        }
                        Object[] source = {x509Cert.getNotBefore(), notAfter};
                        certStr.append(validityTimeForm.format(source));
                    }
                } catch (CertificateExpiredException cee) {
                    if (isTsCert) {
                        hasExpiredTsaCert = true;
                    } else {
                        hasExpiredCert = true;
                    }

                    if (expiredTimeForm == null) {
                        expiredTimeForm = new MessageFormat(
                                rb.getString("certificate.expired.on"));
                    }
                    Object[] source = {notAfter};
                    certStr.append(expiredTimeForm.format(source));

                } catch (CertificateNotYetValidException cnyve) {
                    if (!isTsCert) notYetValidCert = true;

                    if (notYetTimeForm == null) {
                        notYetTimeForm = new MessageFormat(
                                rb.getString("certificate.is.not.valid.until"));
                    }
                    Object[] source = {x509Cert.getNotBefore()};
                    certStr.append(notYetTimeForm.format(source));
                }
            }
            certStr.append("]");

            if (checkUsage) {
                boolean[] bad = new boolean[3];
                checkCertUsage(x509Cert, bad);
                if (bad[0] || bad[1] || bad[2]) {
                    String x = "";
                    if (bad[0]) {
                        x ="KeyUsage";
                    }
                    if (bad[1]) {
                        if (x.length() > 0) x = x + ", ";
                        x = x + "ExtendedKeyUsage";
                    }
                    if (bad[2]) {
                        if (x.length() > 0) x = x + ", ";
                        x = x + "NetscapeCertType";
                    }
                    certStr.append("\n").append(tab)
                        .append(MessageFormat.format(rb.getString(
                        ".{0}.extension.does.not.support.code.signing."), x));
                }
            }
        }
        return certStr.toString();
    }

    private static MessageFormat signTimeForm = null;

    private String printTimestamp(String tab, Timestamp timestamp) {

        if (signTimeForm == null) {
            signTimeForm =
                new MessageFormat(rb.getString("entry.was.signed.on"));
        }
        Object[] source = { timestamp.getTimestamp() };

        return new StringBuilder().append(tab).append("[")
            .append(signTimeForm.format(source)).append("]").toString();
    }

    private Map<CodeSigner,Integer> cacheForInKS = new IdentityHashMap<>();

    private int inKeyStoreForOneSigner(CodeSigner signer) {
        if (cacheForInKS.containsKey(signer)) {
            return cacheForInKS.get(signer);
        }

        int result = 0;
        if (store != null) {
            try {
                List<? extends Certificate> certs =
                        signer.getSignerCertPath().getCertificates();
                for (Certificate c : certs) {
                    String alias = storeHash.get(c);
                    if (alias == null) {
                        alias = store.getCertificateAlias(c);
                        if (alias != null) {
                            storeHash.put(c, alias);
                        }
                    }
                    if (alias != null) {
                        result |= IN_KEYSTORE;
                    }
                    for (String ckalias : ckaliases) {
                        if (c.equals(store.getCertificate(ckalias))) {
                            result |= SIGNED_BY_ALIAS;
                            // must continue with next certificate c and cannot
                            // return or break outer loop because has to fill
                            // storeHash for printCert
                            break;
                        }
                    }
                }
            } catch (KeyStoreException kse) {
                // never happens, because keystore has been loaded
            }
        }
        cacheForInKS.put(signer, result);
        return result;
    }

    /**
     * Maps certificates (as keys) to alias names associated in the keystore
     * {@link #keystore} (as values).
     */
    Hashtable<Certificate, String> storeHash = new Hashtable<>();

    int inKeyStore(CodeSigner[] signers) {

        if (signers == null)
            return 0;

        int output = 0;

        for (CodeSigner signer: signers) {
            int result = inKeyStoreForOneSigner(signer);
            output |= result;
        }
        if (ckaliases.size() > 0 && (output & SIGNED_BY_ALIAS) == 0) {
            output |= NOT_ALIAS;
        }
        return output;
    }

    void signJar(String jarName, String alias)
            throws Exception {

        if (digestalg == null) {
            digestalg = JarSigner.Builder.getDefaultDigestAlgorithm();
        }
        checkWeakSign(digestalg, DIGEST_PRIMITIVE_SET, false);

        if (tSADigestAlg == null) {
            tSADigestAlg = JarSigner.Builder.getDefaultDigestAlgorithm();
        }
        checkWeakSign(tSADigestAlg, DIGEST_PRIMITIVE_SET, true);

        if (sigalg == null) {
            sigalg = JarSigner.Builder.getDefaultSignatureAlgorithm(privateKey);
        }
        checkWeakSign(sigalg, SIG_PRIMITIVE_SET, false);

        checkWeakSign(privateKey);

        boolean aliasUsed = false;
        X509Certificate tsaCert = null;

        if (sigfile == null) {
            sigfile = alias;
            aliasUsed = true;
        }

        if (sigfile.length() > 8) {
            sigfile = sigfile.substring(0, 8).toUpperCase(Locale.ENGLISH);
        } else {
            sigfile = sigfile.toUpperCase(Locale.ENGLISH);
        }

        StringBuilder tmpSigFile = new StringBuilder(sigfile.length());
        for (int j = 0; j < sigfile.length(); j++) {
            char c = sigfile.charAt(j);
            if (!
                    ((c>= 'A' && c<= 'Z') ||
                            (c>= '0' && c<= '9') ||
                            (c == '-') ||
                            (c == '_'))) {
                if (aliasUsed) {
                    // convert illegal characters from the alias to be _'s
                    c = '_';
                } else {
                    throw new
                            RuntimeException(rb.getString
                            ("signature.filename.must.consist.of.the.following.characters.A.Z.0.9.or."));
                }
            }
            tmpSigFile.append(c);
        }

        sigfile = tmpSigFile.toString();

        String tmpJarName;
        if (signedjar == null) tmpJarName = jarName+".sig";
        else tmpJarName = signedjar;

        File jarFile = new File(jarName);
        File signedJarFile = new File(tmpJarName);

        // Open the jar (zip) file
        try {
            zipFile = new ZipFile(jarName);
        } catch (IOException ioe) {
            error(rb.getString("unable.to.open.jar.file.")+jarName, ioe);
        }

        CertPath cp = CertificateFactory.getInstance("X.509")
                .generateCertPath(Arrays.asList(certChain));
        JarSigner.Builder builder = new JarSigner.Builder(privateKey, cp);

        if (verbose != null) {
            builder.eventHandler((action, file) -> {
                switch (action) {
                    case "signing":
                        System.out.println(rb.getString(".signing.") + file);
                        break;
                    case "adding":
                        System.out.println(rb.getString(".adding.") + file);
                        break;
                    case "updating":
                        System.out.println(rb.getString(".updating.") + file);
                        break;
                    default:
                        throw new IllegalArgumentException("unknown action: "
                                + action);
                }
            });
        }

        if (digestalg != null) {
            builder.digestAlgorithm(digestalg);
        }
        if (sigalg != null) {
            builder.signatureAlgorithm(sigalg);
        }

        URI tsaURI = null;

        if (tsaUrl != null) {
            tsaURI = new URI(tsaUrl);
        } else if (tsaAlias != null) {
            tsaCert = getTsaCert(tsaAlias);
            tsaURI = PKCS7.getTimestampingURI(tsaCert);
        }

        if (tsaURI != null) {
            if (verbose != null) {
                System.out.println(
                        rb.getString("requesting.a.signature.timestamp"));
                if (tsaUrl != null) {
                    System.out.println(rb.getString("TSA.location.") + tsaUrl);
                } else if (tsaCert != null) {
                    System.out.println(rb.getString("TSA.certificate.") +
                            printCert(true, "", tsaCert, null, false));
                }
            }
            builder.tsa(tsaURI);
            if (tSADigestAlg != null) {
                builder.setProperty("tsaDigestAlg", tSADigestAlg);
            }

            if (tSAPolicyID != null) {
                builder.setProperty("tsaPolicyId", tSAPolicyID);
            }
        }

        if (altSignerClass != null) {
            builder.setProperty("altSigner", altSignerClass);
            if (verbose != null) {
                System.out.println(
                        rb.getString("using.an.alternative.signing.mechanism"));
            }
        }

        if (altSignerClasspath != null) {
            builder.setProperty("altSignerPath", altSignerClasspath);
        }

        builder.signerName(sigfile);

        builder.setProperty("sectionsOnly", Boolean.toString(!signManifest));
        builder.setProperty("internalSF", Boolean.toString(!externalSF));

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(signedJarFile);
        } catch (IOException ioe) {
            error(rb.getString("unable.to.create.")+tmpJarName, ioe);
        }

        Throwable failedCause = null;
        String failedMessage = null;

        try {
            Event.setReportListener(Event.ReporterCategory.ZIPFILEATTRS,
                    (t, o) -> extraAttrsDetected = true);
            builder.build().sign(zipFile, fos);
        } catch (JarSignerException e) {
            failedCause = e.getCause();
            if (failedCause instanceof SocketTimeoutException
                    || failedCause instanceof UnknownHostException) {
                // Provide a helpful message when TSA is beyond a firewall
                failedMessage = rb.getString("unable.to.sign.jar.") +
                        rb.getString("no.response.from.the.Timestamping.Authority.") +
                        "\n  -J-Dhttp.proxyHost=<hostname>" +
                        "\n  -J-Dhttp.proxyPort=<portnumber>\n" +
                        rb.getString("or") +
                        "\n  -J-Dhttps.proxyHost=<hostname> " +
                        "\n  -J-Dhttps.proxyPort=<portnumber> ";
            } else {
                // JarSignerException might have a null cause
                if (failedCause == null) {
                    failedCause = e;
                }
                failedMessage = rb.getString("unable.to.sign.jar.") + failedCause;
            }
        } catch (Exception e) {
            failedCause = e;
            failedMessage = rb.getString("unable.to.sign.jar.") + failedCause;
        } finally {
            // close the resources
            if (zipFile != null) {
                zipFile.close();
                zipFile = null;
            }

            if (fos != null) {
                fos.close();
            }

            Event.clearReportListener(Event.ReporterCategory.ZIPFILEATTRS);
        }

        if (failedCause != null) {
            signedJarFile.delete();
            error(failedMessage, failedCause);
        }

        if (verbose != null) {
            System.out.println();
        }

        // The JarSigner API always accepts the timestamp received.
        // We need to extract the certs from the signed jar to
        // validate it.
        try (JarFile check = new JarFile(signedJarFile)) {
            PKCS7 p7 = new PKCS7(check.getInputStream(check.getEntry(
                    "META-INF/" + sigfile + "."
                            + SignatureFileVerifier.getBlockExtension(privateKey))));
            Timestamp ts = null;
            try {
                SignerInfo si = p7.getSignerInfos()[0];
                if (si.getTsToken() != null) {
                    hasTimestampBlock = true;
                }
                ts = si.getTimestamp();
            } catch (Exception e) {
                tsaChainNotValidated = true;
                tsaChainNotValidatedReason = e;
            }
            // Spaces before the ">>> Signer" and other lines are different
            String result = certsAndTSInfo("", "    ", Arrays.asList(certChain), ts);
            if (verbose != null) {
                System.out.println(result);
            }
        } catch (Exception e) {
            if (debug) {
                e.printStackTrace();
            }
        }

        if (signedjar == null) {
            // attempt an atomic rename. If that fails,
            // rename the original jar file, then the signed
            // one, then delete the original.
            if (!signedJarFile.renameTo(jarFile)) {
                File origJar = new File(jarName+".orig");

                if (jarFile.renameTo(origJar)) {
                    if (signedJarFile.renameTo(jarFile)) {
                        origJar.delete();
                    } else {
                        MessageFormat form = new MessageFormat(rb.getString
                    ("attempt.to.rename.signedJarFile.to.jarFile.failed"));
                        Object[] source = {signedJarFile, jarFile};
                        error(form.format(source));
                    }
                } else {
                    MessageFormat form = new MessageFormat(rb.getString
                        ("attempt.to.rename.jarFile.to.origJar.failed"));
                    Object[] source = {jarFile, origJar};
                    error(form.format(source));
                }
            }
        }
        displayMessagesAndResult(true);
    }

    /**
     * signature-related files include:
     * . META-INF/MANIFEST.MF
     * . META-INF/SIG-*
     * . META-INF/*.SF
     * . META-INF/*.DSA
     * . META-INF/*.RSA
     * . META-INF/*.EC
     */
    private boolean signatureRelated(String name) {
        return SignatureFileVerifier.isSigningRelated(name);
    }

    Map<CodeSigner,String> cacheForSignerInfo = new IdentityHashMap<>();

    /**
     * Returns a string of signer info, with a newline at the end.
     * Called by verifyJar().
     */
    private String signerInfo(CodeSigner signer, String tab) throws Exception {
        if (cacheForSignerInfo.containsKey(signer)) {
            return cacheForSignerInfo.get(signer);
        }
        List<? extends Certificate> certs = signer.getSignerCertPath().getCertificates();
        // signing time is only displayed on verification
        Timestamp ts = signer.getTimestamp();
        String tsLine = "";
        if (ts != null) {
            tsLine = printTimestamp(tab, ts) + "\n";
        }
        // Spaces before the ">>> Signer" and other lines are the same.

        String result = certsAndTSInfo(tab, tab, certs, ts);
        cacheForSignerInfo.put(signer, tsLine + result);
        return result;
    }

    /**
     * Fills info on certs and timestamp into a StringBuilder, sets
     * warning flags (through printCert) and validates cert chains.
     *
     * @param tab1 spaces before the ">>> Signer" line
     * @param tab2 spaces before the other lines
     * @param certs the signer cert
     * @param ts the timestamp, can be null
     * @return the info as a string
     */
    private String certsAndTSInfo(
            String tab1,
            String tab2,
            List<? extends Certificate> certs, Timestamp ts)
            throws Exception {

        Date timestamp;
        if (ts != null) {
            timestamp = ts.getTimestamp();
            noTimestamp = false;
        } else {
            timestamp = null;
        }
        // display the certificate(sb). The first one is end-entity cert and
        // its KeyUsage should be checked.
        boolean first = true;
        StringBuilder sb = new StringBuilder();
        sb.append(tab1).append(rb.getString("...Signer")).append('\n');
        for (Certificate c : certs) {
            sb.append(printCert(false, tab2, c, timestamp, first));
            sb.append('\n');
            first = false;
        }
        try {
            validateCertChain(Validator.VAR_CODE_SIGNING, certs, ts);
        } catch (Exception e) {
            chainNotValidated = true;
            chainNotValidatedReason = e;
            sb.append(tab2).append(rb.getString(".Invalid.certificate.chain."))
                    .append(e.getLocalizedMessage()).append("]\n");
        }
        if (ts != null) {
            sb.append(tab1).append(rb.getString("...TSA")).append('\n');
            for (Certificate c : ts.getSignerCertPath().getCertificates()) {
                sb.append(printCert(true, tab2, c, null, false));
                sb.append('\n');
            }
            try {
                validateCertChain(Validator.VAR_TSA_SERVER,
                        ts.getSignerCertPath().getCertificates(), null);
            } catch (Exception e) {
                tsaChainNotValidated = true;
                tsaChainNotValidatedReason = e;
                sb.append(tab2).append(rb.getString(".Invalid.TSA.certificate.chain."))
                        .append(e.getLocalizedMessage()).append("]\n");
            }
        }
        if (certs.size() == 1
                && KeyStoreUtil.isSelfSigned((X509Certificate)certs.get(0))) {
            signerSelfSigned = true;
        }

        return sb.toString();
    }

    void loadKeyStore(String keyStoreName, boolean prompt) {

        if (!nullStream && keyStoreName == null) {
            keyStoreName = System.getProperty("user.home") + File.separator
                + ".keystore";
        }

        try {
            try {
                KeyStore caks = KeyStoreUtil.getCacertsKeyStore();
                if (caks != null) {
                    Enumeration<String> aliases = caks.aliases();
                    while (aliases.hasMoreElements()) {
                        String a = aliases.nextElement();
                        try {
                            trustedCerts.add((X509Certificate)caks.getCertificate(a));
                        } catch (Exception e2) {
                            // ignore, when a SecretkeyEntry does not include a cert
                        }
                    }
                }
            } catch (Exception e) {
                // Ignore, if cacerts cannot be loaded
            }

            if (providerName == null) {
                store = KeyStore.getInstance(storetype);
            } else {
                store = KeyStore.getInstance(storetype, providerName);
            }

            // Get pass phrase
            // XXX need to disable echo; on UNIX, call getpass(char *prompt)Z
            // and on NT call ??
            if (token && storepass == null && !protectedPath
                    && !KeyStoreUtil.isWindowsKeyStore(storetype)) {
                storepass = getPass
                        (rb.getString("Enter.Passphrase.for.keystore."));
            } else if (!token && storepass == null && prompt) {
                storepass = getPass
                        (rb.getString("Enter.Passphrase.for.keystore."));
            }

            try {
                if (nullStream) {
                    store.load(null, storepass);
                } else {
                    keyStoreName = keyStoreName.replace(File.separatorChar, '/');
                    URL url = null;
                    try {
                        url = new URL(keyStoreName);
                    } catch (java.net.MalformedURLException e) {
                        // try as file
                        url = new File(keyStoreName).toURI().toURL();
                    }
                    InputStream is = null;
                    try {
                        is = url.openStream();
                        store.load(is, storepass);
                    } finally {
                        if (is != null) {
                            is.close();
                        }
                    }
                }
                Enumeration<String> aliases = store.aliases();
                while (aliases.hasMoreElements()) {
                    String a = aliases.nextElement();
                    try {
                        X509Certificate c = (X509Certificate)store.getCertificate(a);
                        // Only add TrustedCertificateEntry and self-signed
                        // PrivateKeyEntry
                        if (store.isCertificateEntry(a) ||
                                c.getSubjectX500Principal().equals(c.getIssuerX500Principal())) {
                            trustedCerts.add(c);
                        }
                    } catch (Exception e2) {
                        // ignore, when a SecretkeyEntry does not include a cert
                    }
                }
            } finally {
                try {
                    pkixParameters = new PKIXBuilderParameters(
                            trustedCerts.stream()
                                    .map(c -> new TrustAnchor(c, null))
                                    .collect(Collectors.toSet()),
                            null);

                    if (revocationCheck) {
                        Security.setProperty("ocsp.enable", "true");
                        System.setProperty("com.sun.security.enableCRLDP", "true");
                        Event.setReportListener(Event.ReporterCategory.CRLCHECK,
                                (t, o) -> System.out.println(String.format(rb.getString(t), o)));
                    }
                    pkixParameters.setRevocationEnabled(revocationCheck);
                } catch (InvalidAlgorithmParameterException ex) {
                    // Only if tas is empty
                }
            }
        } catch (IOException ioe) {
            throw new RuntimeException(rb.getString("keystore.load.") +
                                        ioe.getMessage());
        } catch (java.security.cert.CertificateException ce) {
            throw new RuntimeException(rb.getString("certificate.exception.") +
                                        ce.getMessage());
        } catch (NoSuchProviderException pe) {
            throw new RuntimeException(rb.getString("keystore.load.") +
                                        pe.getMessage());
        } catch (NoSuchAlgorithmException nsae) {
            throw new RuntimeException(rb.getString("keystore.load.") +
                                        nsae.getMessage());
        } catch (KeyStoreException kse) {
            throw new RuntimeException
                (rb.getString("unable.to.instantiate.keystore.class.") +
                kse.getMessage());
        }
    }

    X509Certificate getTsaCert(String alias) {

        java.security.cert.Certificate cs = null;

        try {
            cs = store.getCertificate(alias);
        } catch (KeyStoreException kse) {
            // this never happens, because keystore has been loaded
        }
        if (cs == null || (!(cs instanceof X509Certificate))) {
            MessageFormat form = new MessageFormat(rb.getString
                ("Certificate.not.found.for.alias.alias.must.reference.a.valid.KeyStore.entry.containing.an.X.509.public.key.certificate.for.the"));
            Object[] source = {alias, alias};
            error(form.format(source));
        }
        return (X509Certificate) cs;
    }

    /**
     * Check if userCert is designed to be a code signer
     * @param userCert the certificate to be examined
     * @param bad 3 booleans to show if the KeyUsage, ExtendedKeyUsage,
     *            NetscapeCertType has codeSigning flag turned on.
     *            If null, the class field badKeyUsage, badExtendedKeyUsage,
     *            badNetscapeCertType will be set.
     */
    void checkCertUsage(X509Certificate userCert, boolean[] bad) {

        // Can act as a signer?
        // 1. if KeyUsage, then [0:digitalSignature] or
        //    [1:nonRepudiation] should be true
        // 2. if ExtendedKeyUsage, then should contains ANY or CODE_SIGNING
        // 3. if NetscapeCertType, then should contains OBJECT_SIGNING
        // 1,2,3 must be true

        if (bad != null) {
            bad[0] = bad[1] = bad[2] = false;
        }

        boolean[] keyUsage = userCert.getKeyUsage();
        if (keyUsage != null) {
            keyUsage = Arrays.copyOf(keyUsage, 9);
            if (!keyUsage[0] && !keyUsage[1]) {
                if (bad != null) {
                    bad[0] = true;
                    badKeyUsage = true;
                }
            }
        }

        try {
            List<String> xKeyUsage = userCert.getExtendedKeyUsage();
            if (xKeyUsage != null) {
                if (!xKeyUsage.contains("2.5.29.37.0") // anyExtendedKeyUsage
                        && !xKeyUsage.contains("1.3.6.1.5.5.7.3.3")) {  // codeSigning
                    if (bad != null) {
                        bad[1] = true;
                        badExtendedKeyUsage = true;
                    }
                }
            }
        } catch (java.security.cert.CertificateParsingException e) {
            // shouldn't happen
        }

        try {
            // OID_NETSCAPE_CERT_TYPE
            byte[] netscapeEx = userCert.getExtensionValue
                    ("2.16.840.1.113730.1.1");
            if (netscapeEx != null) {
                DerInputStream in = new DerInputStream(netscapeEx);
                byte[] encoded = in.getOctetString();
                encoded = new DerValue(encoded).getUnalignedBitString()
                        .toByteArray();

                NetscapeCertTypeExtension extn =
                        new NetscapeCertTypeExtension(encoded);

                Boolean val = extn.get(NetscapeCertTypeExtension.OBJECT_SIGNING);
                if (!val) {
                    if (bad != null) {
                        bad[2] = true;
                        badNetscapeCertType = true;
                    }
                }
            }
        } catch (IOException e) {
            //
        }
    }

    // Called by signJar().
    void getAliasInfo(String alias) throws Exception {

        Key key = null;

        try {
            java.security.cert.Certificate[] cs = null;
            if (altCertChain != null) {
                try (FileInputStream fis = new FileInputStream(altCertChain)) {
                    cs = CertificateFactory.getInstance("X.509").
                            generateCertificates(fis).
                            toArray(new Certificate[0]);
                } catch (FileNotFoundException ex) {
                    error(rb.getString("File.specified.by.certchain.does.not.exist"));
                } catch (CertificateException | IOException ex) {
                    error(rb.getString("Cannot.restore.certchain.from.file.specified"));
                }
            } else {
                try {
                    cs = store.getCertificateChain(alias);
                } catch (KeyStoreException kse) {
                    // this never happens, because keystore has been loaded
                }
            }
            if (cs == null || cs.length == 0) {
                if (altCertChain != null) {
                    error(rb.getString
                            ("Certificate.chain.not.found.in.the.file.specified."));
                } else {
                    MessageFormat form = new MessageFormat(rb.getString
                        ("Certificate.chain.not.found.for.alias.alias.must.reference.a.valid.KeyStore.key.entry.containing.a.private.key.and"));
                    Object[] source = {alias, alias};
                    error(form.format(source));
                }
            }

            certChain = new X509Certificate[cs.length];
            for (int i=0; i<cs.length; i++) {
                if (!(cs[i] instanceof X509Certificate)) {
                    error(rb.getString
                        ("found.non.X.509.certificate.in.signer.s.chain"));
                }
                certChain[i] = (X509Certificate)cs[i];
            }

            try {
                if (!token && keypass == null)
                    key = store.getKey(alias, storepass);
                else
                    key = store.getKey(alias, keypass);
            } catch (UnrecoverableKeyException e) {
                if (token) {
                    throw e;
                } else if (keypass == null) {
                    // Did not work out, so prompt user for key password
                    MessageFormat form = new MessageFormat(rb.getString
                        ("Enter.key.password.for.alias."));
                    Object[] source = {alias};
                    keypass = getPass(form.format(source));
                    key = store.getKey(alias, keypass);
                }
            }
        } catch (NoSuchAlgorithmException e) {
            error(e.getMessage());
        } catch (UnrecoverableKeyException e) {
            error(rb.getString("unable.to.recover.key.from.keystore"));
        } catch (KeyStoreException kse) {
            // this never happens, because keystore has been loaded
        }

        if (!(key instanceof PrivateKey)) {
            MessageFormat form = new MessageFormat(rb.getString
                ("key.associated.with.alias.not.a.private.key"));
            Object[] source = {alias};
            error(form.format(source));
        } else {
            privateKey = (PrivateKey)key;
        }
    }

    void error(String message) {
        System.out.println(rb.getString("jarsigner.")+message);
        System.exit(1);
    }


    void error(String message, Throwable e) {
        System.out.println(rb.getString("jarsigner.")+message);
        if (debug) {
            e.printStackTrace();
        }
        System.exit(1);
    }

    /**
     * Validates a cert chain.
     *
     * @param parameter this might be a timestamp
     */
    void validateCertChain(String variant, List<? extends Certificate> certs,
                           Timestamp parameter)
            throws Exception {
        try {
            Validator.getInstance(Validator.TYPE_PKIX,
                    variant,
                    pkixParameters)
                    .validate(certs.toArray(new X509Certificate[certs.size()]),
                            null, parameter);
        } catch (Exception e) {
            if (debug) {
                e.printStackTrace();
            }

            // Exception might be dismissed if another warning flag
            // is already set by printCert.

            if (variant.equals(Validator.VAR_TSA_SERVER) &&
                    e instanceof ValidatorException) {
                // Throw cause if it's CertPathValidatorException,
                if (e.getCause() != null &&
                        e.getCause() instanceof CertPathValidatorException) {
                    e = (Exception) e.getCause();
                    Throwable t = e.getCause();
                    if ((t instanceof CertificateExpiredException &&
                            hasExpiredTsaCert)) {
                        // we already have hasExpiredTsaCert
                        return;
                    }
                }
            }

            if (variant.equals(Validator.VAR_CODE_SIGNING) &&
                    e instanceof ValidatorException) {
                // Throw cause if it's CertPathValidatorException,
                if (e.getCause() != null &&
                        e.getCause() instanceof CertPathValidatorException) {
                    e = (Exception) e.getCause();
                    Throwable t = e.getCause();
                    if ((t instanceof CertificateExpiredException &&
                                hasExpiredCert) ||
                            (t instanceof CertificateNotYetValidException &&
                                    notYetValidCert)) {
                        // we already have hasExpiredCert and notYetValidCert
                        return;
                    }
                }
                if (e instanceof ValidatorException) {
                    ValidatorException ve = (ValidatorException)e;
                    if (ve.getErrorType() == ValidatorException.T_EE_EXTENSIONS &&
                            (badKeyUsage || badExtendedKeyUsage || badNetscapeCertType)) {
                        // We already have badKeyUsage, badExtendedKeyUsage
                        // and badNetscapeCertType
                        return;
                    }
                }
            }
            throw e;
        }
    }

    char[] getPass(String prompt) {
        System.err.print(prompt);
        System.err.flush();
        try {
            char[] pass = Password.readPassword(System.in);

            if (pass == null) {
                error(rb.getString("you.must.enter.key.password"));
            } else {
                return pass;
            }
        } catch (IOException ioe) {
            error(rb.getString("unable.to.read.password.")+ioe.getMessage());
        }
        // this shouldn't happen
        return null;
    }
}
