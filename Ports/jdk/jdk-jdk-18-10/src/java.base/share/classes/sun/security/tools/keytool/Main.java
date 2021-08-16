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

package sun.security.tools.keytool;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.CertStoreException;
import java.security.cert.CRL;
import java.security.cert.X509Certificate;
import java.security.cert.CertificateException;
import java.security.cert.URICertStoreParameters;


import java.security.interfaces.ECKey;
import java.security.interfaces.EdECKey;
import java.security.spec.ECParameterSpec;
import java.text.Collator;
import java.text.MessageFormat;
import java.util.*;
import java.util.function.BiFunction;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.cert.CertStore;

import java.security.cert.X509CRL;
import java.security.cert.X509CRLEntry;
import java.security.cert.X509CRLSelector;
import javax.security.auth.x500.X500Principal;
import java.util.Base64;

import sun.security.pkcs12.PKCS12KeyStore;
import sun.security.util.ECKeySizeParameterSpec;
import sun.security.util.KeyUtil;
import sun.security.util.NamedCurve;
import sun.security.util.ObjectIdentifier;
import sun.security.pkcs10.PKCS10;
import sun.security.pkcs10.PKCS10Attribute;
import sun.security.provider.X509Factory;
import sun.security.provider.certpath.ssl.SSLServerCertStore;
import sun.security.util.KnownOIDs;
import sun.security.util.Password;
import sun.security.util.SecurityProperties;
import sun.security.util.SecurityProviderConstants;
import sun.security.util.SignatureUtil;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import sun.security.pkcs.PKCS9Attribute;
import sun.security.tools.KeyStoreUtil;
import sun.security.tools.PathList;
import sun.security.util.DerValue;
import sun.security.util.Pem;
import sun.security.x509.*;

import static java.security.KeyStore.*;
import static sun.security.tools.keytool.Main.Command.*;
import static sun.security.tools.keytool.Main.Option.*;
import sun.security.util.DisabledAlgorithmConstraints;

/**
 * This tool manages keystores.
 *
 * @author Jan Luehe
 *
 *
 * @see java.security.KeyStore
 * @see sun.security.provider.KeyProtector
 * @see sun.security.provider.JavaKeyStore
 *
 * @since 1.2
 */
public final class Main {

    private static final byte[] CRLF = new byte[] {'\r', '\n'};

    private boolean debug = false;
    private Command command = null;
    private String sigAlgName = null;
    private String keyAlgName = null;
    private boolean verbose = false;
    private int keysize = -1;
    private String groupName = null;
    private boolean rfc = false;
    private long validity = (long)90;
    private String alias = null;
    private String dname = null;
    private String dest = null;
    private String filename = null;
    private String infilename = null;
    private String outfilename = null;
    private String srcksfname = null;

    // User-specified providers are added before any command is called.
    // However, they are not removed before the end of the main() method.
    // If you're calling KeyTool.main() directly in your own Java program,
    // please programtically add any providers you need and do not specify
    // them through the command line.

    private Set<Pair <String, String>> providers = null;
    private Set<Pair <String, String>> providerClasses = null;
    private String storetype = null;
    private String srcProviderName = null;
    private String providerName = null;
    private String pathlist = null;
    private char[] storePass = null;
    private char[] storePassNew = null;
    private char[] keyPass = null;
    private char[] keyPassNew = null;
    private char[] newPass = null;
    private char[] destKeyPass = null;
    private char[] srckeyPass = null;
    private String ksfname = null;
    private File ksfile = null;
    private InputStream ksStream = null; // keystore stream
    private String sslserver = null;
    private String jarfile = null;
    private KeyStore keyStore = null;
    private boolean token = false;
    private boolean nullStream = false;
    private boolean kssave = false;
    private boolean noprompt = false;
    private boolean trustcacerts = false;
    private boolean protectedPath = false;
    private boolean srcprotectedPath = false;
    private boolean cacerts = false;
    private boolean nowarn = false;
    private KeyStore caks = null; // "cacerts" keystore
    private char[] srcstorePass = null;
    private String srcstoretype = null;
    private Set<char[]> passwords = new HashSet<>();
    private String startDate = null;
    private String signerAlias = null;
    private char[] signerKeyPass = null;

    private boolean tlsInfo = false;

    private List<String> ids = new ArrayList<>();   // used in GENCRL
    private List<String> v3ext = new ArrayList<>();

    // In-place importkeystore is special.
    // A backup is needed, and no need to prompt for deststorepass.
    private boolean inplaceImport = false;
    private String inplaceBackupName = null;

    // Warnings on weak algorithms etc
    private List<String> weakWarnings = new ArrayList<>();

    private static final DisabledAlgorithmConstraints DISABLED_CHECK =
            new DisabledAlgorithmConstraints(
                    DisabledAlgorithmConstraints.PROPERTY_CERTPATH_DISABLED_ALGS);

    private static final DisabledAlgorithmConstraints LEGACY_CHECK =
            new DisabledAlgorithmConstraints(
                    DisabledAlgorithmConstraints.PROPERTY_SECURITY_LEGACY_ALGS);

    private static final Set<CryptoPrimitive> SIG_PRIMITIVE_SET = Collections
            .unmodifiableSet(EnumSet.of(CryptoPrimitive.SIGNATURE));
    private boolean isPasswordlessKeyStore = false;

    enum Command {
        CERTREQ("Generates.a.certificate.request",
            ALIAS, SIGALG, FILEOUT, KEYPASS, KEYSTORE, DNAME,
            EXT, STOREPASS, STORETYPE, PROVIDERNAME, ADDPROVIDER,
            PROVIDERCLASS, PROVIDERPATH, V, PROTECTED),
        CHANGEALIAS("Changes.an.entry.s.alias",
            ALIAS, DESTALIAS, KEYPASS, KEYSTORE, CACERTS, STOREPASS,
            STORETYPE, PROVIDERNAME, ADDPROVIDER, PROVIDERCLASS,
            PROVIDERPATH, V, PROTECTED),
        DELETE("Deletes.an.entry",
            ALIAS, KEYSTORE, CACERTS, STOREPASS, STORETYPE,
            PROVIDERNAME, ADDPROVIDER, PROVIDERCLASS,
            PROVIDERPATH, V, PROTECTED),
        EXPORTCERT("Exports.certificate",
            RFC, ALIAS, FILEOUT, KEYSTORE, CACERTS, STOREPASS,
            STORETYPE, PROVIDERNAME, ADDPROVIDER, PROVIDERCLASS,
            PROVIDERPATH, V, PROTECTED),
        GENKEYPAIR("Generates.a.key.pair",
            ALIAS, KEYALG, KEYSIZE, CURVENAME, SIGALG, DNAME,
            STARTDATE, EXT, VALIDITY, KEYPASS, KEYSTORE,
            SIGNER, SIGNERKEYPASS,
            STOREPASS, STORETYPE, PROVIDERNAME, ADDPROVIDER,
            PROVIDERCLASS, PROVIDERPATH, V, PROTECTED),
        GENSECKEY("Generates.a.secret.key",
            ALIAS, KEYPASS, KEYALG, KEYSIZE, KEYSTORE,
            STOREPASS, STORETYPE, PROVIDERNAME, ADDPROVIDER,
            PROVIDERCLASS, PROVIDERPATH, V, PROTECTED),
        GENCERT("Generates.certificate.from.a.certificate.request",
            RFC, INFILE, OUTFILE, ALIAS, SIGALG, DNAME,
            STARTDATE, EXT, VALIDITY, KEYPASS, KEYSTORE,
            STOREPASS, STORETYPE, PROVIDERNAME, ADDPROVIDER,
            PROVIDERCLASS, PROVIDERPATH, V, PROTECTED),
        IMPORTCERT("Imports.a.certificate.or.a.certificate.chain",
            NOPROMPT, TRUSTCACERTS, PROTECTED, ALIAS, FILEIN,
            KEYPASS, KEYSTORE, CACERTS, STOREPASS, STORETYPE,
            PROVIDERNAME, ADDPROVIDER, PROVIDERCLASS,
            PROVIDERPATH, V),
        IMPORTPASS("Imports.a.password",
            ALIAS, KEYPASS, KEYALG, KEYSIZE, KEYSTORE,
            STOREPASS, STORETYPE, PROVIDERNAME, ADDPROVIDER,
            PROVIDERCLASS, PROVIDERPATH, V, PROTECTED),
        IMPORTKEYSTORE("Imports.one.or.all.entries.from.another.keystore",
            SRCKEYSTORE, DESTKEYSTORE, SRCSTORETYPE,
            DESTSTORETYPE, SRCSTOREPASS, DESTSTOREPASS,
            SRCPROTECTED, DESTPROTECTED, SRCPROVIDERNAME, DESTPROVIDERNAME,
            SRCALIAS, DESTALIAS, SRCKEYPASS, DESTKEYPASS,
            NOPROMPT, ADDPROVIDER, PROVIDERCLASS, PROVIDERPATH,
            V),
        KEYPASSWD("Changes.the.key.password.of.an.entry",
            ALIAS, KEYPASS, NEW, KEYSTORE, STOREPASS,
            STORETYPE, PROVIDERNAME, ADDPROVIDER, PROVIDERCLASS,
            PROVIDERPATH, V),
        LIST("Lists.entries.in.a.keystore",
            RFC, ALIAS, KEYSTORE, CACERTS, STOREPASS, STORETYPE,
            PROVIDERNAME, ADDPROVIDER, PROVIDERCLASS,
            PROVIDERPATH, V, PROTECTED),
        PRINTCERT("Prints.the.content.of.a.certificate",
            RFC, FILEIN, SSLSERVER, JARFILE,
            KEYSTORE, STOREPASS, STORETYPE, TRUSTCACERTS,
            PROVIDERNAME, ADDPROVIDER, PROVIDERCLASS,
            PROVIDERPATH, V, PROTECTED),
        PRINTCERTREQ("Prints.the.content.of.a.certificate.request",
            FILEIN, V),
        PRINTCRL("Prints.the.content.of.a.CRL.file",
            FILEIN, KEYSTORE, STOREPASS, STORETYPE, TRUSTCACERTS,
            PROVIDERNAME, ADDPROVIDER, PROVIDERCLASS, PROVIDERPATH,
            V, PROTECTED),
        STOREPASSWD("Changes.the.store.password.of.a.keystore",
            NEW, KEYSTORE, CACERTS, STOREPASS, STORETYPE, PROVIDERNAME,
            ADDPROVIDER, PROVIDERCLASS, PROVIDERPATH, V),
        SHOWINFO("showinfo.command.help",
            TLS, V),

        // Undocumented start here, KEYCLONE is used a marker in -help;

        KEYCLONE("Clones.a.key.entry",
            ALIAS, DESTALIAS, KEYPASS, NEW, STORETYPE,
            KEYSTORE, STOREPASS, PROVIDERNAME, ADDPROVIDER,
            PROVIDERCLASS, PROVIDERPATH, V),
        SELFCERT("Generates.a.self.signed.certificate",
            ALIAS, SIGALG, DNAME, STARTDATE, EXT, VALIDITY, KEYPASS,
            STORETYPE, KEYSTORE, STOREPASS, PROVIDERNAME,
            ADDPROVIDER, PROVIDERCLASS, PROVIDERPATH, V),
        GENCRL("Generates.CRL",
            RFC, FILEOUT, ID,
            ALIAS, SIGALG, KEYPASS, KEYSTORE,
            STOREPASS, STORETYPE, PROVIDERNAME, ADDPROVIDER,
            PROVIDERCLASS, PROVIDERPATH, V, PROTECTED),
        IDENTITYDB("Imports.entries.from.a.JDK.1.1.x.style.identity.database",
            FILEIN, STORETYPE, KEYSTORE, STOREPASS, PROVIDERNAME,
            ADDPROVIDER, PROVIDERCLASS, PROVIDERPATH, V);

        final String description;
        final Option[] options;
        final String name;

        String altName;     // "genkey" is altName for "genkeypair"

        Command(String d, Option... o) {
            description = d;
            options = o;
            name = "-" + name().toLowerCase(Locale.ENGLISH);
        }
        @Override
        public String toString() {
            return name;
        }
        public String getAltName() {
            return altName;
        }
        public void setAltName(String altName) {
            this.altName = altName;
        }
        public static Command getCommand(String cmd) {
            for (Command c: Command.values()) {
                if (collator.compare(cmd, c.name) == 0
                        || (c.altName != null
                            && collator.compare(cmd, c.altName) == 0)) {
                    return c;
                }
            }
            return null;
        }
    };

    static {
        Command.GENKEYPAIR.setAltName("-genkey");
        Command.IMPORTCERT.setAltName("-import");
        Command.EXPORTCERT.setAltName("-export");
        Command.IMPORTPASS.setAltName("-importpassword");
    }

    // If an option is allowed multiple times, remember to record it
    // in the optionsSet.contains() block in parseArgs().
    enum Option {
        ALIAS("alias", "<alias>", "alias.name.of.the.entry.to.process"),
        CURVENAME("groupname", "<name>", "groupname.option.help"),
        DESTALIAS("destalias", "<alias>", "destination.alias"),
        DESTKEYPASS("destkeypass", "<arg>", "destination.key.password"),
        DESTKEYSTORE("destkeystore", "<keystore>", "destination.keystore.name"),
        DESTPROTECTED("destprotected", null, "destination.keystore.password.protected"),
        DESTPROVIDERNAME("destprovidername", "<name>", "destination.keystore.provider.name"),
        DESTSTOREPASS("deststorepass", "<arg>", "destination.keystore.password"),
        DESTSTORETYPE("deststoretype", "<type>", "destination.keystore.type"),
        DNAME("dname", "<name>", "distinguished.name"),
        EXT("ext", "<value>", "X.509.extension"),
        FILEOUT("file", "<file>", "output.file.name"),
        FILEIN("file", "<file>", "input.file.name"),
        ID("id", "<id:reason>", "Serial.ID.of.cert.to.revoke"),
        INFILE("infile", "<file>", "input.file.name"),
        KEYALG("keyalg", "<alg>", "key.algorithm.name"),
        KEYPASS("keypass", "<arg>", "key.password"),
        KEYSIZE("keysize", "<size>", "key.bit.size"),
        KEYSTORE("keystore", "<keystore>", "keystore.name"),
        CACERTS("cacerts", null, "access.the.cacerts.keystore"),
        NEW("new", "<arg>", "new.password"),
        NOPROMPT("noprompt", null, "do.not.prompt"),
        OUTFILE("outfile", "<file>", "output.file.name"),
        PROTECTED("protected", null, "password.through.protected.mechanism"),
        PROVIDERCLASS("providerclass", "<class>\n[-providerarg <arg>]", "provider.class.option"),
        ADDPROVIDER("addprovider", "<name>\n[-providerarg <arg>]", "addprovider.option"),
        PROVIDERNAME("providername", "<name>", "provider.name"),
        PROVIDERPATH("providerpath", "<list>", "provider.classpath"),
        RFC("rfc", null, "output.in.RFC.style"),
        SIGALG("sigalg", "<alg>", "signature.algorithm.name"),
        SIGNER("signer", "<alias>", "signer.alias"),
        SIGNERKEYPASS("signerkeypass", "<arg>", "signer.key.password"),
        SRCALIAS("srcalias", "<alias>", "source.alias"),
        SRCKEYPASS("srckeypass", "<arg>", "source.key.password"),
        SRCKEYSTORE("srckeystore", "<keystore>", "source.keystore.name"),
        SRCPROTECTED("srcprotected", null, "source.keystore.password.protected"),
        SRCPROVIDERNAME("srcprovidername", "<name>", "source.keystore.provider.name"),
        SRCSTOREPASS("srcstorepass", "<arg>", "source.keystore.password"),
        SRCSTORETYPE("srcstoretype", "<type>", "source.keystore.type"),
        SSLSERVER("sslserver", "<server[:port]>", "SSL.server.host.and.port"),
        JARFILE("jarfile", "<file>", "signed.jar.file"),
        STARTDATE("startdate", "<date>", "certificate.validity.start.date.time"),
        STOREPASS("storepass", "<arg>", "keystore.password"),
        STORETYPE("storetype", "<type>", "keystore.type"),
        TLS("tls", null, "tls.option.help"),
        TRUSTCACERTS("trustcacerts", null, "trust.certificates.from.cacerts"),
        V("v", null, "verbose.output"),
        VALIDITY("validity", "<days>", "validity.number.of.days");

        final String name, arg, description;
        Option(String name, String arg, String description) {
            this.name = name;
            this.arg = arg;
            this.description = description;
        }
        @Override
        public String toString() {
            return "-" + name;
        }
    };

    private static final String NONE = "NONE";
    private static final String P11KEYSTORE = "PKCS11";
    private static final String P12KEYSTORE = "PKCS12";
    private static final String keyAlias = "mykey";

    // for i18n
    private static final java.util.ResourceBundle rb =
        java.util.ResourceBundle.getBundle(
            "sun.security.tools.keytool.Resources");
    private static final Collator collator = Collator.getInstance();
    static {
        // this is for case insensitive string comparisons
        collator.setStrength(Collator.PRIMARY);
    };

    private Main() { }

    public static void main(String[] args) throws Exception {
        Main kt = new Main();
        kt.run(args, System.out);
    }

    private void run(String[] args, PrintStream out) throws Exception {
        try {
            args = parseArgs(args);
            if (command != null) {
                doCommands(out);
            }
        } catch (Exception e) {
            System.out.println(rb.getString("keytool.error.") + e);
            if (verbose) {
                e.printStackTrace(System.out);
            }
            if (!debug) {
                System.exit(1);
            } else {
                throw e;
            }
        } finally {
            printWeakWarnings(false);
            for (char[] pass : passwords) {
                if (pass != null) {
                    Arrays.fill(pass, ' ');
                    pass = null;
                }
            }

            if (ksStream != null) {
                ksStream.close();
            }
        }
    }

    /**
     * Parse command line arguments.
     */
    String[] parseArgs(String[] args) throws Exception {

        int i=0;
        boolean help = args.length == 0;

        String confFile = null;

        // Records all commands and options set. Used to check dups.
        Set<String> optionsSet = new HashSet<>();

        for (i=0; i < args.length; i++) {
            String flags = args[i];
            if (flags.startsWith("-")) {
                String lowerFlags = flags.toLowerCase(Locale.ROOT);
                if (optionsSet.contains(lowerFlags)) {
                    switch (lowerFlags) {
                        case "-ext":
                        case "-id":
                        case "-provider":
                        case "-addprovider":
                        case "-providerclass":
                        case "-providerarg":
                            // These options are allowed multiple times
                            break;
                        default:
                            weakWarnings.add(String.format(
                                    rb.getString("option.1.set.twice"),
                                    lowerFlags));
                    }
                } else {
                    optionsSet.add(lowerFlags);
                }
                if (collator.compare(flags, "-conf") == 0) {
                    if (i == args.length - 1) {
                        errorNeedArgument(flags);
                    }
                    confFile = args[++i];
                } else {
                    Command c = Command.getCommand(flags);
                    if (c != null) {
                        if (command == null) {
                            command = c;
                        } else {
                            throw new Exception(String.format(
                                    rb.getString("multiple.commands.1.2"),
                                    command.name, c.name));
                        }
                    }
                }
            }
        }

        if (confFile != null && command != null) {
            args = KeyStoreUtil.expandArgs("keytool", confFile,
                    command.toString(),
                    command.getAltName(), args);
        }

        debug = Arrays.stream(args).anyMatch(
                x -> collator.compare(x, "-debug") == 0);

        if (debug) {
            // No need to localize debug output
            System.out.println("Command line args: " +
                    Arrays.toString(args));
        }

        for (i=0; (i < args.length) && args[i].startsWith("-"); i++) {

            String flags = args[i];

            // Check if the last option needs an arg
            if (i == args.length - 1) {
                for (Option option: Option.values()) {
                    // Only options with an arg need to be checked
                    if (collator.compare(flags, option.toString()) == 0) {
                        if (option.arg != null) errorNeedArgument(flags);
                        break;
                    }
                }
            }

            /*
             * Check modifiers
             */
            String modifier = null;
            int pos = flags.indexOf(':');
            if (pos > 0) {
                modifier = flags.substring(pos+1);
                flags = flags.substring(0, pos);
            }

            /*
             * command modes
             */
            Command c = Command.getCommand(flags);

            if (c != null) {
                command = c;
            } else if (collator.compare(flags, "--help") == 0 ||
                       collator.compare(flags, "-h") == 0 ||
                       collator.compare(flags, "-?") == 0 ||
                       // -help: legacy.
                       collator.compare(flags, "-help") == 0) {
                help = true;
            } else if (collator.compare(flags, "-conf") == 0) {
                i++;
            } else if (collator.compare(flags, "-nowarn") == 0) {
                nowarn = true;
            } else if (collator.compare(flags, "-keystore") == 0) {
                ksfname = args[++i];
                if (new File(ksfname).getCanonicalPath().equals(
                        new File(KeyStoreUtil.getCacerts()).getCanonicalPath())) {
                    System.err.println(rb.getString("warning.cacerts.option"));
                }
            } else if (collator.compare(flags, "-destkeystore") == 0) {
                ksfname = args[++i];
            } else if (collator.compare(flags, "-cacerts") == 0) {
                cacerts = true;
            } else if (collator.compare(flags, "-storepass") == 0 ||
                    collator.compare(flags, "-deststorepass") == 0) {
                storePass = getPass(modifier, args[++i]);
                passwords.add(storePass);
            } else if (collator.compare(flags, "-storetype") == 0 ||
                    collator.compare(flags, "-deststoretype") == 0) {
                storetype = KeyStoreUtil.niceStoreTypeName(args[++i]);
            } else if (collator.compare(flags, "-srcstorepass") == 0) {
                srcstorePass = getPass(modifier, args[++i]);
                passwords.add(srcstorePass);
            } else if (collator.compare(flags, "-srcstoretype") == 0) {
                srcstoretype = KeyStoreUtil.niceStoreTypeName(args[++i]);
            } else if (collator.compare(flags, "-srckeypass") == 0) {
                srckeyPass = getPass(modifier, args[++i]);
                passwords.add(srckeyPass);
            } else if (collator.compare(flags, "-srcprovidername") == 0) {
                srcProviderName = args[++i];
            } else if (collator.compare(flags, "-providername") == 0 ||
                    collator.compare(flags, "-destprovidername") == 0) {
                providerName = args[++i];
            } else if (collator.compare(flags, "-providerpath") == 0) {
                pathlist = args[++i];
            } else if (collator.compare(flags, "-keypass") == 0) {
                keyPass = getPass(modifier, args[++i]);
                passwords.add(keyPass);
            } else if (collator.compare(flags, "-new") == 0) {
                newPass = getPass(modifier, args[++i]);
                passwords.add(newPass);
            } else if (collator.compare(flags, "-destkeypass") == 0) {
                destKeyPass = getPass(modifier, args[++i]);
                passwords.add(destKeyPass);
            } else if (collator.compare(flags, "-alias") == 0 ||
                    collator.compare(flags, "-srcalias") == 0) {
                alias = args[++i];
            } else if (collator.compare(flags, "-dest") == 0 ||
                    collator.compare(flags, "-destalias") == 0) {
                dest = args[++i];
            } else if (collator.compare(flags, "-dname") == 0) {
                dname = args[++i];
            } else if (collator.compare(flags, "-keysize") == 0) {
                keysize = Integer.parseInt(args[++i]);
            } else if (collator.compare(flags, "-groupname") == 0) {
                groupName = args[++i];
            } else if (collator.compare(flags, "-keyalg") == 0) {
                keyAlgName = args[++i];
            } else if (collator.compare(flags, "-sigalg") == 0) {
                sigAlgName = args[++i];
            } else if (collator.compare(flags, "-signer") == 0) {
                signerAlias = args[++i];
            } else if (collator.compare(flags, "-signerkeypass") == 0) {
                signerKeyPass = getPass(modifier, args[++i]);
                passwords.add(signerKeyPass);
            } else if (collator.compare(flags, "-startdate") == 0) {
                startDate = args[++i];
            } else if (collator.compare(flags, "-validity") == 0) {
                validity = Long.parseLong(args[++i]);
            } else if (collator.compare(flags, "-ext") == 0) {
                v3ext.add(args[++i]);
            } else if (collator.compare(flags, "-id") == 0) {
                ids.add(args[++i]);
            } else if (collator.compare(flags, "-file") == 0) {
                filename = args[++i];
            } else if (collator.compare(flags, "-infile") == 0) {
                infilename = args[++i];
            } else if (collator.compare(flags, "-outfile") == 0) {
                outfilename = args[++i];
            } else if (collator.compare(flags, "-sslserver") == 0) {
                sslserver = args[++i];
            } else if (collator.compare(flags, "-jarfile") == 0) {
                jarfile = args[++i];
            } else if (collator.compare(flags, "-srckeystore") == 0) {
                srcksfname = args[++i];
            } else if (collator.compare(flags, "-provider") == 0 ||
                        collator.compare(flags, "-providerclass") == 0) {
                if (providerClasses == null) {
                    providerClasses = new HashSet<Pair <String, String>> (3);
                }
                String providerClass = args[++i];
                String providerArg = null;

                if (args.length > (i+1)) {
                    flags = args[i+1];
                    if (collator.compare(flags, "-providerarg") == 0) {
                        if (args.length == (i+2)) errorNeedArgument(flags);
                        providerArg = args[i+2];
                        i += 2;
                    }
                }
                providerClasses.add(
                        Pair.of(providerClass, providerArg));
            } else if (collator.compare(flags, "-addprovider") == 0) {
                if (providers == null) {
                    providers = new HashSet<Pair <String, String>> (3);
                }
                String provider = args[++i];
                String providerArg = null;

                if (args.length > (i+1)) {
                    flags = args[i+1];
                    if (collator.compare(flags, "-providerarg") == 0) {
                        if (args.length == (i+2)) errorNeedArgument(flags);
                        providerArg = args[i+2];
                        i += 2;
                    }
                }
                providers.add(
                        Pair.of(provider, providerArg));
            }

            /*
             * options
             */
            else if (collator.compare(flags, "-v") == 0) {
                verbose = true;
            } else if (collator.compare(flags, "-debug") == 0) {
                // Already processed
            } else if (collator.compare(flags, "-rfc") == 0) {
                rfc = true;
            } else if (collator.compare(flags, "-noprompt") == 0) {
                noprompt = true;
            } else if (collator.compare(flags, "-trustcacerts") == 0) {
                trustcacerts = true;
            } else if (collator.compare(flags, "-protected") == 0 ||
                    collator.compare(flags, "-destprotected") == 0) {
                protectedPath = true;
            } else if (collator.compare(flags, "-srcprotected") == 0) {
                srcprotectedPath = true;
            } else if (collator.compare(flags, "-tls") == 0) {
                tlsInfo = true;
            } else  {
                System.err.println(rb.getString("Illegal.option.") + flags);
                tinyHelp();
            }
        }

        if (i<args.length) {
            System.err.println(rb.getString("Illegal.option.") + args[i]);
            tinyHelp();
        }

        if (command == null) {
            if (help) {
                usage();
            } else {
                System.err.println(rb.getString("Usage.error.no.command.provided"));
                tinyHelp();
            }
        } else if (help) {
            usage();
            command = null;
        }

        return args;
    }

    boolean isKeyStoreRelated(Command cmd) {
        return cmd != PRINTCERTREQ && cmd != SHOWINFO;
    }

    /**
     * Execute the commands.
     */
    void doCommands(PrintStream out) throws Exception {

        if (cacerts) {
            if (ksfname != null || storetype != null) {
                throw new IllegalArgumentException(rb.getString
                        ("the.keystore.or.storetype.option.cannot.be.used.with.the.cacerts.option"));
            }
            ksfname = KeyStoreUtil.getCacerts();
        }

        if (P11KEYSTORE.equalsIgnoreCase(storetype) ||
                KeyStoreUtil.isWindowsKeyStore(storetype)) {
            token = true;
            if (ksfname == null) {
                ksfname = NONE;
            }
        }
        if (NONE.equals(ksfname)) {
            nullStream = true;
        }

        if (token && !nullStream) {
            System.err.println(MessageFormat.format(rb.getString
                (".keystore.must.be.NONE.if.storetype.is.{0}"), storetype));
            System.err.println();
            tinyHelp();
        }

        if (token &&
            (command == KEYPASSWD || command == STOREPASSWD)) {
            throw new UnsupportedOperationException(MessageFormat.format(rb.getString
                        (".storepasswd.and.keypasswd.commands.not.supported.if.storetype.is.{0}"), storetype));
        }

        if (token && (keyPass != null || newPass != null || destKeyPass != null)) {
            throw new IllegalArgumentException(MessageFormat.format(rb.getString
                (".keypass.and.new.can.not.be.specified.if.storetype.is.{0}"), storetype));
        }

        if (protectedPath) {
            if (storePass != null || keyPass != null ||
                    newPass != null || destKeyPass != null) {
                throw new IllegalArgumentException(rb.getString
                        ("if.protected.is.specified.then.storepass.keypass.and.new.must.not.be.specified"));
            }
        }

        if (srcprotectedPath) {
            if (srcstorePass != null || srckeyPass != null) {
                throw new IllegalArgumentException(rb.getString
                        ("if.srcprotected.is.specified.then.srcstorepass.and.srckeypass.must.not.be.specified"));
            }
        }

        if (KeyStoreUtil.isWindowsKeyStore(storetype)) {
            if (storePass != null || keyPass != null ||
                    newPass != null || destKeyPass != null) {
                throw new IllegalArgumentException(rb.getString
                        ("if.keystore.is.not.password.protected.then.storepass.keypass.and.new.must.not.be.specified"));
            }
        }

        if (KeyStoreUtil.isWindowsKeyStore(srcstoretype)) {
            if (srcstorePass != null || srckeyPass != null) {
                throw new IllegalArgumentException(rb.getString
                        ("if.source.keystore.is.not.password.protected.then.srcstorepass.and.srckeypass.must.not.be.specified"));
            }
        }

        if (validity <= (long)0) {
            throw new Exception
                (rb.getString("Validity.must.be.greater.than.zero"));
        }

        // Try to load and install specified provider
        if (providers != null) {
            for (Pair<String, String> provider : providers) {
                try {
                    KeyStoreUtil.loadProviderByName(
                            provider.fst, provider.snd);
                    if (debug) {
                        System.out.println("loadProviderByName: " + provider.fst);
                    }
                } catch (IllegalArgumentException e) {
                    throw new Exception(String.format(rb.getString(
                            "provider.name.not.found"), provider.fst));
                }
            }
        }
        if (providerClasses != null) {
            ClassLoader cl = null;
            if (pathlist != null) {
                String path = null;
                path = PathList.appendPath(
                        path, System.getProperty("java.class.path"));
                path = PathList.appendPath(
                        path, System.getProperty("env.class.path"));
                path = PathList.appendPath(path, pathlist);

                URL[] urls = PathList.pathToURLs(path);
                cl = new URLClassLoader(urls);
            } else {
                cl = ClassLoader.getSystemClassLoader();
            }
            for (Pair<String, String> provider : providerClasses) {
                try {
                    KeyStoreUtil.loadProviderByClass(
                            provider.fst, provider.snd, cl);
                    if (debug) {
                        System.out.println("loadProviderByClass: " + provider.fst);
                    }
                } catch (ClassCastException cce) {
                    throw new Exception(String.format(rb.getString(
                            "provclass.not.a.provider"), provider.fst));
                } catch (IllegalArgumentException e) {
                    throw new Exception(String.format(rb.getString(
                            "provider.class.not.found"), provider.fst), e.getCause());
                }
            }
        }

        if (command == LIST && verbose && rfc) {
            System.err.println(rb.getString
                ("Must.not.specify.both.v.and.rfc.with.list.command"));
            tinyHelp();
        }

        // Make sure provided passwords are at least 6 characters long
        if (command == GENKEYPAIR && keyPass!=null && keyPass.length < 6) {
            throw new Exception(rb.getString
                ("Key.password.must.be.at.least.6.characters"));
        }
        if (newPass != null && newPass.length < 6) {
            throw new Exception(rb.getString
                ("New.password.must.be.at.least.6.characters"));
        }
        if (destKeyPass != null && destKeyPass.length < 6) {
            throw new Exception(rb.getString
                ("New.password.must.be.at.least.6.characters"));
        }

        // Set this before inplaceImport check so we can compare name.
        if (ksfname == null) {
            ksfname = System.getProperty("user.home") + File.separator
                    + ".keystore";
        }

        KeyStore srcKeyStore = null;
        if (command == IMPORTKEYSTORE) {
            inplaceImport = inplaceImportCheck();
            if (inplaceImport) {
                // We load srckeystore first so we have srcstorePass that
                // can be assigned to storePass
                srcKeyStore = loadSourceKeyStore();
                if (storePass == null) {
                    storePass = srcstorePass;
                }
            }
        }

        // Check if keystore exists.
        // If no keystore has been specified at the command line, try to use
        // the default, which is located in $HOME/.keystore.
        // No need to check if isKeyStoreRelated(command) is false.

        // DO NOT open the existing keystore if this is an in-place import.
        // The keystore should be created as brand new.
        if (isKeyStoreRelated(command) && !nullStream && !inplaceImport) {
            try {
                ksfile = new File(ksfname);
                // Check if keystore file is empty
                if (ksfile.exists() && ksfile.length() == 0) {
                    throw new Exception(rb.getString
                            ("Keystore.file.exists.but.is.empty.") + ksfname);
                }
                ksStream = new FileInputStream(ksfile);
            } catch (FileNotFoundException e) {
                // These commands do not need the keystore to be existing.
                // Either it will create a new one or the keystore is
                // optional (i.e. PRINTCRL and PRINTCERT).
                if (command != GENKEYPAIR &&
                        command != GENSECKEY &&
                        command != IDENTITYDB &&
                        command != IMPORTCERT &&
                        command != IMPORTPASS &&
                        command != IMPORTKEYSTORE &&
                        command != PRINTCRL &&
                        command != PRINTCERT) {
                    throw new Exception(rb.getString
                            ("Keystore.file.does.not.exist.") + ksfname);
                }
            }
        }

        if ((command == KEYCLONE || command == CHANGEALIAS)
                && dest == null) {
            dest = getAlias("destination");
            if ("".equals(dest)) {
                throw new Exception(rb.getString
                        ("Must.specify.destination.alias"));
            }
        }

        if (command == DELETE && alias == null) {
            alias = getAlias(null);
            if ("".equals(alias)) {
                throw new Exception(rb.getString("Must.specify.alias"));
            }
        }

        if (ksfile != null && ksStream != null && providerName == null &&
                !inplaceImport) {
            // existing keystore
            if (storetype == null) {
                // Probe for keystore type when filename is available
                keyStore = KeyStore.getInstance(ksfile, storePass);
                storetype = keyStore.getType();
            } else {
                keyStore = KeyStore.getInstance(storetype);
                // storePass might be null here, will probably prompt later
                keyStore.load(ksStream, storePass);
            }
            if (storetype.equalsIgnoreCase("pkcs12")) {
                try {
                    isPasswordlessKeyStore = PKCS12KeyStore.isPasswordless(ksfile);
                } catch (IOException ioe) {
                    // This must be a JKS keystore that's opened as a PKCS12
                }
            }
        } else {
            // Create new keystore
            if (storetype == null) {
                storetype = KeyStore.getDefaultType();
            }
            if (providerName == null) {
                keyStore = KeyStore.getInstance(storetype);
            } else {
                keyStore = KeyStore.getInstance(storetype, providerName);
            }
            // When creating a new pkcs12 file, Do not prompt for storepass
            // if certProtectionAlgorithm and macAlgorithm are both NONE.
            if (storetype.equalsIgnoreCase("pkcs12")) {
                isPasswordlessKeyStore =
                        "NONE".equals(SecurityProperties.privilegedGetOverridable(
                                "keystore.pkcs12.certProtectionAlgorithm"))
                        && "NONE".equals(SecurityProperties.privilegedGetOverridable(
                                "keystore.pkcs12.macAlgorithm"));
            }

            /*
             * Load the keystore data.
             *
             * At this point, it's OK if no keystore password has been provided.
             * We want to make sure that we can load the keystore data, i.e.,
             * the keystore data has the right format. If we cannot load the
             * keystore, why bother asking the user for his or her password?
             * Only if we were able to load the keystore, and no keystore
             * password has been provided, will we prompt the user for the
             * keystore password to verify the keystore integrity.
             * This means that the keystore is loaded twice: first load operation
             * checks the keystore format, second load operation verifies the
             * keystore integrity.
             *
             * If the keystore password has already been provided (at the
             * command line), however, the keystore is loaded only once, and the
             * keystore format and integrity are checked "at the same time".
             *
             * Null stream keystores are loaded later.
             */
            if (!nullStream) {
                if (inplaceImport) {
                    keyStore.load(null, storePass);
                } else {
                    // both ksStream and storePass could be null
                    keyStore.load(ksStream, storePass);
                }
            }
        }

        if (P12KEYSTORE.equalsIgnoreCase(storetype) && command == KEYPASSWD) {
            throw new UnsupportedOperationException(rb.getString
                    (".keypasswd.commands.not.supported.if.storetype.is.PKCS12"));
        }

        // All commands that create or modify the keystore require a keystore
        // password.

        if (nullStream && storePass != null) {
            keyStore.load(null, storePass);
        } else if (!nullStream && storePass != null) {
            // If we are creating a new non nullStream-based keystore,
            // insist that the password be at least 6 characters
            if (ksStream == null && storePass.length < 6) {
                throw new Exception(rb.getString
                        ("Keystore.password.must.be.at.least.6.characters"));
            }
        } else if (storePass == null) {
            if (!protectedPath && !KeyStoreUtil.isWindowsKeyStore(storetype)
                    && isKeyStoreRelated(command)
                    && !isPasswordlessKeyStore) {
                if (command == CERTREQ ||
                        command == DELETE ||
                        command == GENKEYPAIR ||
                        command == GENSECKEY ||
                        command == IMPORTCERT ||
                        command == IMPORTPASS ||
                        command == IMPORTKEYSTORE ||
                        command == KEYCLONE ||
                        command == CHANGEALIAS ||
                        command == SELFCERT ||
                        command == STOREPASSWD ||
                        command == KEYPASSWD ||
                        command == IDENTITYDB) {
                    int count = 0;
                    do {
                        if (command == IMPORTKEYSTORE) {
                            System.err.print
                                    (rb.getString("Enter.destination.keystore.password."));
                        } else {
                            System.err.print
                                    (rb.getString("Enter.keystore.password."));
                        }
                        System.err.flush();
                        storePass = Password.readPassword(System.in);
                        passwords.add(storePass);

                        // If we are creating a new non nullStream-based keystore,
                        // insist that the password be at least 6 characters
                        if (!nullStream && (storePass == null || storePass.length < 6)) {
                            System.err.println(rb.getString
                                    ("Keystore.password.is.too.short.must.be.at.least.6.characters"));
                            storePass = null;
                        }

                        // If the keystore file does not exist and needs to be
                        // created, the storepass should be prompted twice.
                        if (storePass != null && !nullStream && ksStream == null) {
                            System.err.print(rb.getString("Re.enter.new.password."));
                            char[] storePassAgain = Password.readPassword(System.in);
                            passwords.add(storePassAgain);
                            if (!Arrays.equals(storePass, storePassAgain)) {
                                System.err.println
                                        (rb.getString("They.don.t.match.Try.again"));
                                storePass = null;
                            }
                        }

                        count++;
                    } while ((storePass == null) && count < 3);


                    if (storePass == null) {
                        System.err.println
                                (rb.getString("Too.many.failures.try.later"));
                        return;
                    }
                } else {
                    // here we have EXPORTCERT and LIST (info valid until STOREPASSWD)
                    if (command != PRINTCRL && command != PRINTCERT) {
                        System.err.print(rb.getString("Enter.keystore.password."));
                        System.err.flush();
                        storePass = Password.readPassword(System.in);
                        passwords.add(storePass);
                    }
                }
            }

            // Now load a nullStream-based keystore,
            // or verify the integrity of an input stream-based keystore
            if (nullStream) {
                keyStore.load(null, storePass);
            } else if (ksStream != null) {
                // Reload with user-provided password
                try (FileInputStream fis = new FileInputStream(ksfile)) {
                    keyStore.load(fis, storePass);
                }
            }
        }

        if (storePass != null && P12KEYSTORE.equalsIgnoreCase(storetype)) {
            MessageFormat form = new MessageFormat(rb.getString(
                "Warning.Different.store.and.key.passwords.not.supported.for.PKCS12.KeyStores.Ignoring.user.specified.command.value."));
            if (keyPass != null && !Arrays.equals(storePass, keyPass)) {
                Object[] source = {"-keypass"};
                System.err.println(form.format(source));
                keyPass = storePass;
            }
            if (destKeyPass != null && !Arrays.equals(storePass, destKeyPass)) {
                Object[] source = {"-destkeypass"};
                System.err.println(form.format(source));
                destKeyPass = storePass;
            }
        }

        // -trustcacerts can be specified on -importcert, -printcert or -printcrl.
        // Reset it so that warnings on CA cert will remain for other command.
        if (command != IMPORTCERT && command != PRINTCERT
                && command != PRINTCRL) {
            trustcacerts = false;
        }

        if (trustcacerts) {
            caks = KeyStoreUtil.getCacertsKeyStore();
        }

        // Perform the specified command
        if (command == CERTREQ) {
            if (filename != null) {
                try (PrintStream ps = new PrintStream(new FileOutputStream
                                                      (filename))) {
                    doCertReq(alias, sigAlgName, ps);
                }
            } else {
                doCertReq(alias, sigAlgName, out);
            }
            if (verbose && filename != null) {
                MessageFormat form = new MessageFormat(rb.getString
                        ("Certification.request.stored.in.file.filename."));
                Object[] source = {filename};
                System.err.println(form.format(source));
                System.err.println(rb.getString("Submit.this.to.your.CA"));
            }
        } else if (command == DELETE) {
            doDeleteEntry(alias);
            kssave = true;
        } else if (command == EXPORTCERT) {
            if (filename != null) {
                try (PrintStream ps = new PrintStream(new FileOutputStream
                                                   (filename))) {
                    doExportCert(alias, ps);
                }
            } else {
                doExportCert(alias, out);
            }
            if (filename != null) {
                MessageFormat form = new MessageFormat(rb.getString
                        ("Certificate.stored.in.file.filename."));
                Object[] source = {filename};
                System.err.println(form.format(source));
            }
        } else if (command == GENKEYPAIR) {
            if (keyAlgName == null) {
                throw new Exception(rb.getString(
                        "keyalg.option.missing.error"));
            }
            doGenKeyPair(alias, dname, keyAlgName, keysize, groupName, sigAlgName,
                    signerAlias);
            kssave = true;
        } else if (command == GENSECKEY) {
            if (keyAlgName == null) {
                throw new Exception(rb.getString(
                        "keyalg.option.missing.error"));
            }
            doGenSecretKey(alias, keyAlgName, keysize);
            kssave = true;
        } else if (command == IMPORTPASS) {
            if (keyAlgName == null) {
                keyAlgName = "PBE";
            }
            // password is stored as a secret key
            doGenSecretKey(alias, keyAlgName, keysize);
            kssave = true;
        } else if (command == IDENTITYDB) {
            if (filename != null) {
                try (InputStream inStream = new FileInputStream(filename)) {
                    doImportIdentityDatabase(inStream);
                }
            } else {
                doImportIdentityDatabase(System.in);
            }
        } else if (command == IMPORTCERT) {
            InputStream inStream = System.in;
            if (filename != null) {
                inStream = new FileInputStream(filename);
            }
            String importAlias = (alias!=null)?alias:keyAlias;
            try {
                if (keyStore.entryInstanceOf(
                        importAlias, KeyStore.PrivateKeyEntry.class)) {
                    kssave = installReply(importAlias, inStream);
                    if (kssave) {
                        System.err.println(rb.getString
                            ("Certificate.reply.was.installed.in.keystore"));
                    } else {
                        System.err.println(rb.getString
                            ("Certificate.reply.was.not.installed.in.keystore"));
                    }
                } else if (!keyStore.containsAlias(importAlias) ||
                        keyStore.entryInstanceOf(importAlias,
                            KeyStore.TrustedCertificateEntry.class)) {
                    kssave = addTrustedCert(importAlias, inStream);
                    if (kssave) {
                        System.err.println(rb.getString
                            ("Certificate.was.added.to.keystore"));
                    } else {
                        System.err.println(rb.getString
                            ("Certificate.was.not.added.to.keystore"));
                    }
                }
            } finally {
                if (inStream != System.in) {
                    inStream.close();
                }
            }
        } else if (command == IMPORTKEYSTORE) {
            // When not in-place import, srcKeyStore is not loaded yet.
            if (srcKeyStore == null) {
                srcKeyStore = loadSourceKeyStore();
            }
            doImportKeyStore(srcKeyStore);
            kssave = true;
        } else if (command == KEYCLONE) {
            keyPassNew = newPass;

            // added to make sure only key can go thru
            if (alias == null) {
                alias = keyAlias;
            }
            if (keyStore.containsAlias(alias) == false) {
                MessageFormat form = new MessageFormat
                    (rb.getString("Alias.alias.does.not.exist"));
                Object[] source = {alias};
                throw new Exception(form.format(source));
            }
            if (!keyStore.entryInstanceOf(alias, KeyStore.PrivateKeyEntry.class)) {
                MessageFormat form = new MessageFormat(rb.getString(
                        "Alias.alias.references.an.entry.type.that.is.not.a.private.key.entry.The.keyclone.command.only.supports.cloning.of.private.key"));
                Object[] source = {alias};
                throw new Exception(form.format(source));
            }

            doCloneEntry(alias, dest, true);  // Now everything can be cloned
            kssave = true;
        } else if (command == CHANGEALIAS) {
            if (alias == null) {
                alias = keyAlias;
            }
            doCloneEntry(alias, dest, false);
            // in PKCS11, clone a PrivateKeyEntry will delete the old one
            if (keyStore.containsAlias(alias)) {
                doDeleteEntry(alias);
            }
            kssave = true;
        } else if (command == KEYPASSWD) {
            keyPassNew = newPass;
            doChangeKeyPasswd(alias);
            kssave = true;
        } else if (command == LIST) {
            if (storePass == null
                    && !KeyStoreUtil.isWindowsKeyStore(storetype)
                    && !isPasswordlessKeyStore) {
                printNoIntegrityWarning();
            }

            if (alias != null) {
                doPrintEntry(rb.getString("the.certificate"), alias, out);
            } else {
                doPrintEntries(out);
            }
        } else if (command == PRINTCERT) {
            doPrintCert(out);
        } else if (command == SELFCERT) {
            doSelfCert(alias, dname, sigAlgName);
            kssave = true;
        } else if (command == STOREPASSWD) {
            doChangeStorePasswd();
            kssave = true;
        } else if (command == GENCERT) {
            if (alias == null) {
                alias = keyAlias;
            }
            InputStream inStream = System.in;
            if (infilename != null) {
                inStream = new FileInputStream(infilename);
            }
            PrintStream ps = null;
            if (outfilename != null) {
                ps = new PrintStream(new FileOutputStream(outfilename));
                out = ps;
            }
            try {
                doGenCert(alias, sigAlgName, inStream, out);
            } finally {
                if (inStream != System.in) {
                    inStream.close();
                }
                if (ps != null) {
                    ps.close();
                }
            }
        } else if (command == GENCRL) {
            if (alias == null) {
                alias = keyAlias;
            }
            if (filename != null) {
                try (PrintStream ps =
                         new PrintStream(new FileOutputStream(filename))) {
                    doGenCRL(ps);
                }
            } else {
                doGenCRL(out);
            }
        } else if (command == PRINTCERTREQ) {
            if (filename != null) {
                try (InputStream inStream = new FileInputStream(filename)) {
                    doPrintCertReq(inStream, out);
                }
            } else {
                doPrintCertReq(System.in, out);
            }
        } else if (command == PRINTCRL) {
            doPrintCRL(filename, out);
        } else if (command == SHOWINFO) {
            doShowInfo();
        }

        // If we need to save the keystore, do so.
        if (kssave) {
            if (verbose) {
                MessageFormat form = new MessageFormat
                        (rb.getString(".Storing.ksfname."));
                Object[] source = {nullStream ? "keystore" : ksfname};
                System.err.println(form.format(source));
            }

            if (token) {
                keyStore.store(null, null);
            } else {
                char[] pass = (storePassNew!=null) ? storePassNew : storePass;
                if (nullStream) {
                    keyStore.store(null, pass);
                } else {
                    ByteArrayOutputStream bout = new ByteArrayOutputStream();
                    keyStore.store(bout, pass);
                    try (FileOutputStream fout = new FileOutputStream(ksfname)) {
                        fout.write(bout.toByteArray());
                    }
                }
            }
        }

        if (isKeyStoreRelated(command)
                && !token && !nullStream && ksfname != null) {

            // JKS storetype warning on the final result keystore
            File f = new File(ksfname);
            char[] pass = (storePassNew!=null) ? storePassNew : storePass;
            if (f.exists()) {
                // Probe for real type. A JKS can be loaded as PKCS12 because
                // DualFormat support, vice versa.
                String realType = storetype;
                try {
                    keyStore = KeyStore.getInstance(f, pass);
                    realType = keyStore.getType();
                    if (realType.equalsIgnoreCase("JKS")
                            || realType.equalsIgnoreCase("JCEKS")) {
                        boolean allCerts = true;
                        for (String a : Collections.list(keyStore.aliases())) {
                            if (!keyStore.entryInstanceOf(
                                    a, TrustedCertificateEntry.class)) {
                                allCerts = false;
                                break;
                            }
                        }
                        // Don't warn for "cacerts" style keystore.
                        if (!allCerts) {
                            weakWarnings.add(String.format(
                                    rb.getString("jks.storetype.warning"),
                                    realType, ksfname));
                        }
                    }
                } catch (KeyStoreException e) {
                    // Probing not supported, therefore cannot be JKS or JCEKS.
                    // Skip the legacy type warning at all.
                }
                if (inplaceImport) {
                    String realSourceStoreType = srcstoretype;
                    try {
                        realSourceStoreType = KeyStore.getInstance(
                                new File(inplaceBackupName), srcstorePass).getType();
                    } catch (KeyStoreException e) {
                        // Probing not supported. Assuming srcstoretype.
                    }
                    String format =
                            realType.equalsIgnoreCase(realSourceStoreType) ?
                            rb.getString("backup.keystore.warning") :
                            rb.getString("migrate.keystore.warning");
                    weakWarnings.add(
                            String.format(format,
                                    srcksfname,
                                    realSourceStoreType,
                                    inplaceBackupName,
                                    realType));
                }
            }
        }
    }

    /**
     * Generate a certificate: Read PKCS10 request from in, and print
     * certificate to out. Use alias as CA, sigAlgName as the signature
     * type.
     */
    private void doGenCert(String alias, String sigAlgName, InputStream in, PrintStream out)
            throws Exception {


        if (keyStore.containsAlias(alias) == false) {
            MessageFormat form = new MessageFormat
                    (rb.getString("Alias.alias.does.not.exist"));
            Object[] source = {alias};
            throw new Exception(form.format(source));
        }
        Certificate signerCert = keyStore.getCertificate(alias);
        byte[] encoded = signerCert.getEncoded();
        X509CertImpl signerCertImpl = new X509CertImpl(encoded);
        X509CertInfo signerCertInfo = (X509CertInfo)signerCertImpl.get(
                X509CertImpl.NAME + "." + X509CertImpl.INFO);
        X500Name issuer = (X500Name)signerCertInfo.get(X509CertInfo.SUBJECT + "." +
                                           X509CertInfo.DN_NAME);

        Date firstDate = getStartDate(startDate);
        Date lastDate = new Date();
        lastDate.setTime(firstDate.getTime() + validity*1000L*24L*60L*60L);
        CertificateValidity interval = new CertificateValidity(firstDate,
                                                               lastDate);

        PrivateKey privateKey =
                (PrivateKey)recoverKey(alias, storePass, keyPass).fst;
        if (sigAlgName == null) {
            sigAlgName = getCompatibleSigAlgName(privateKey);
        }
        X509CertInfo info = new X509CertInfo();
        info.set(X509CertInfo.VALIDITY, interval);
        info.set(X509CertInfo.SERIAL_NUMBER,
                CertificateSerialNumber.newRandom64bit(new SecureRandom()));
        info.set(X509CertInfo.VERSION,
                    new CertificateVersion(CertificateVersion.V3));
        info.set(X509CertInfo.ISSUER, issuer);

        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        boolean canRead = false;
        StringBuilder sb = new StringBuilder();
        while (true) {
            String s = reader.readLine();
            if (s == null) break;
            // OpenSSL does not use NEW
            //if (s.startsWith("-----BEGIN NEW CERTIFICATE REQUEST-----")) {
            if (s.startsWith("-----BEGIN") && s.indexOf("REQUEST") >= 0) {
                canRead = true;
            //} else if (s.startsWith("-----END NEW CERTIFICATE REQUEST-----")) {
            } else if (s.startsWith("-----END") && s.indexOf("REQUEST") >= 0) {
                break;
            } else if (canRead) {
                sb.append(s);
            }
        }
        byte[] rawReq = Pem.decode(new String(sb));
        PKCS10 req = new PKCS10(rawReq);

        checkWeak(rb.getString("the.certificate.request"), req);

        info.set(X509CertInfo.KEY, new CertificateX509Key(req.getSubjectPublicKeyInfo()));
        info.set(X509CertInfo.SUBJECT,
                    dname==null?req.getSubjectName():new X500Name(dname));
        CertificateExtensions reqex = null;
        Iterator<PKCS10Attribute> attrs = req.getAttributes().getAttributes().iterator();
        while (attrs.hasNext()) {
            PKCS10Attribute attr = attrs.next();
            if (attr.getAttributeId().equals(PKCS9Attribute.EXTENSION_REQUEST_OID)) {
                reqex = (CertificateExtensions)attr.getAttributeValue();
            }
        }

        PublicKey subjectPubKey = req.getSubjectPublicKeyInfo();
        PublicKey issuerPubKey = signerCert.getPublicKey();

        KeyIdentifier signerSubjectKeyId;
        if (Arrays.equals(subjectPubKey.getEncoded(), issuerPubKey.getEncoded())) {
            // No AKID for self-signed cert
            signerSubjectKeyId = null;
        } else {
            X509CertImpl certImpl;
            if (signerCert instanceof X509CertImpl) {
                certImpl = (X509CertImpl) signerCert;
            } else {
                certImpl = new X509CertImpl(signerCert.getEncoded());
            }

            // To enforce compliance with RFC 5280 section 4.2.1.1: "Where a key
            // identifier has been previously established, the CA SHOULD use the
            // previously established identifier."
            // Use issuer's SKID to establish the AKID in createV3Extensions() method.
            signerSubjectKeyId = certImpl.getSubjectKeyId();

            if (signerSubjectKeyId == null) {
                signerSubjectKeyId = new KeyIdentifier(issuerPubKey);
            }
        }

        CertificateExtensions ext = createV3Extensions(
                reqex,
                null,
                v3ext,
                subjectPubKey,
                signerSubjectKeyId);
        info.set(X509CertInfo.EXTENSIONS, ext);
        X509CertImpl cert = new X509CertImpl(info);
        cert.sign(privateKey, sigAlgName);
        dumpCert(cert, out);
        for (Certificate ca: keyStore.getCertificateChain(alias)) {
            if (ca instanceof X509Certificate) {
                X509Certificate xca = (X509Certificate)ca;
                if (!KeyStoreUtil.isSelfSigned(xca)) {
                    dumpCert(xca, out);
                }
            }
        }

        checkWeak(rb.getString("the.issuer"), keyStore.getCertificateChain(alias));
        checkWeak(rb.getString("the.generated.certificate"), cert);
    }

    private void doGenCRL(PrintStream out)
            throws Exception {
        if (ids == null) {
            throw new Exception("Must provide -id when -gencrl");
        }
        Certificate signerCert = keyStore.getCertificate(alias);
        byte[] encoded = signerCert.getEncoded();
        X509CertImpl signerCertImpl = new X509CertImpl(encoded);
        X509CertInfo signerCertInfo = (X509CertInfo)signerCertImpl.get(
                X509CertImpl.NAME + "." + X509CertImpl.INFO);
        X500Name owner = (X500Name)signerCertInfo.get(X509CertInfo.SUBJECT + "." +
                                                      X509CertInfo.DN_NAME);

        Date firstDate = getStartDate(startDate);
        Date lastDate = (Date) firstDate.clone();
        lastDate.setTime(lastDate.getTime() + validity*1000*24*60*60);
        CertificateValidity interval = new CertificateValidity(firstDate,
                                                               lastDate);


        PrivateKey privateKey =
                (PrivateKey)recoverKey(alias, storePass, keyPass).fst;
        if (sigAlgName == null) {
            sigAlgName = getCompatibleSigAlgName(privateKey);
        }

        X509CRLEntry[] badCerts = new X509CRLEntry[ids.size()];
        for (int i=0; i<ids.size(); i++) {
            String id = ids.get(i);
            int d = id.indexOf(':');
            if (d >= 0) {
                CRLExtensions ext = new CRLExtensions();
                ext.set("Reason", new CRLReasonCodeExtension(Integer.parseInt(id.substring(d+1))));
                badCerts[i] = new X509CRLEntryImpl(new BigInteger(id.substring(0, d)),
                        firstDate, ext);
            } else {
                badCerts[i] = new X509CRLEntryImpl(new BigInteger(ids.get(i)), firstDate);
            }
        }
        X509CRLImpl crl = new X509CRLImpl(owner, firstDate, lastDate, badCerts);
        crl.sign(privateKey, sigAlgName);
        if (rfc) {
            out.println("-----BEGIN X509 CRL-----");
            out.println(Base64.getMimeEncoder(64, CRLF).encodeToString(crl.getEncodedInternal()));
            out.println("-----END X509 CRL-----");
        } else {
            out.write(crl.getEncodedInternal());
        }
        checkWeak(rb.getString("the.generated.crl"), crl, privateKey);
    }

    /**
     * Creates a PKCS#10 cert signing request, corresponding to the
     * keys (and name) associated with a given alias.
     */
    private void doCertReq(String alias, String sigAlgName, PrintStream out)
        throws Exception
    {
        if (alias == null) {
            alias = keyAlias;
        }

        Pair<Key,char[]> objs = recoverKey(alias, storePass, keyPass);
        PrivateKey privKey = (PrivateKey)objs.fst;
        if (keyPass == null) {
            keyPass = objs.snd;
        }

        Certificate cert = keyStore.getCertificate(alias);
        if (cert == null) {
            MessageFormat form = new MessageFormat
                (rb.getString("alias.has.no.public.key.certificate."));
            Object[] source = {alias};
            throw new Exception(form.format(source));
        }
        PKCS10 request = new PKCS10(cert.getPublicKey());
        CertificateExtensions ext = createV3Extensions(null, null, v3ext, cert.getPublicKey(), null);
        // Attribute name is not significant
        request.getAttributes().setAttribute(X509CertInfo.EXTENSIONS,
                new PKCS10Attribute(PKCS9Attribute.EXTENSION_REQUEST_OID, ext));

        // Construct a Signature object, so that we can sign the request
        if (sigAlgName == null) {
            sigAlgName = getCompatibleSigAlgName(privKey);
        }

        X500Name subject = dname == null?
                new X500Name(((X509Certificate)cert).getSubjectX500Principal().getEncoded()):
                new X500Name(dname);

        // Sign the request and base-64 encode it
        request.encodeAndSign(subject, privKey, sigAlgName);
        request.print(out);

        checkWeak(rb.getString("the.generated.certificate.request"), request);
    }

    /**
     * Deletes an entry from the keystore.
     */
    private void doDeleteEntry(String alias) throws Exception {
        if (keyStore.containsAlias(alias) == false) {
            MessageFormat form = new MessageFormat
                (rb.getString("Alias.alias.does.not.exist"));
            Object[] source = {alias};
            throw new Exception(form.format(source));
        }
        keyStore.deleteEntry(alias);
    }

    /**
     * Exports a certificate from the keystore.
     */
    private void doExportCert(String alias, PrintStream out)
        throws Exception
    {
        if (storePass == null
                && !KeyStoreUtil.isWindowsKeyStore(storetype)
                && !isPasswordlessKeyStore) {
            printNoIntegrityWarning();
        }
        if (alias == null) {
            alias = keyAlias;
        }
        if (keyStore.containsAlias(alias) == false) {
            MessageFormat form = new MessageFormat
                (rb.getString("Alias.alias.does.not.exist"));
            Object[] source = {alias};
            throw new Exception(form.format(source));
        }

        X509Certificate cert = (X509Certificate)keyStore.getCertificate(alias);
        if (cert == null) {
            MessageFormat form = new MessageFormat
                (rb.getString("Alias.alias.has.no.certificate"));
            Object[] source = {alias};
            throw new Exception(form.format(source));
        }
        dumpCert(cert, out);
        checkWeak(rb.getString("the.certificate"), cert);
    }

    /**
     * Prompt the user for a keypass when generating a key entry.
     * @param alias the entry we will set password for
     * @param orig the original entry of doing a dup, null if generate new
     * @param origPass the password to copy from if user press ENTER
     */
    private char[] promptForKeyPass(String alias, String orig, char[] origPass) throws Exception{
        if (origPass != null && P12KEYSTORE.equalsIgnoreCase(storetype)) {
            return origPass;
        } else if (!token && !protectedPath) {
            // Prompt for key password
            int count;
            for (count = 0; count < 3; count++) {
                MessageFormat form = new MessageFormat(rb.getString
                        ("Enter.key.password.for.alias."));
                Object[] source = {alias};
                System.err.print(form.format(source));
                if (origPass != null) {
                    System.err.println();
                    if (orig == null) {
                        System.err.print(rb.getString
                                (".RETURN.if.same.as.keystore.password."));
                    } else {
                        form = new MessageFormat(rb.getString
                                (".RETURN.if.same.as.for.otherAlias."));
                        Object[] src = {orig};
                        System.err.print(form.format(src));
                    }
                }
                System.err.flush();
                char[] entered = Password.readPassword(System.in);
                passwords.add(entered);
                if (entered == null && origPass != null) {
                    return origPass;
                } else if (entered != null && entered.length >= 6) {
                    System.err.print(rb.getString("Re.enter.new.password."));
                    char[] passAgain = Password.readPassword(System.in);
                    passwords.add(passAgain);
                    if (!Arrays.equals(entered, passAgain)) {
                        System.err.println
                            (rb.getString("They.don.t.match.Try.again"));
                        continue;
                    }
                    return entered;
                } else {
                    System.err.println(rb.getString
                        ("Key.password.is.too.short.must.be.at.least.6.characters"));
                }
            }
            if (count == 3) {
                if (command == KEYCLONE) {
                    throw new Exception(rb.getString
                        ("Too.many.failures.Key.entry.not.cloned"));
                } else {
                    throw new Exception(rb.getString
                            ("Too.many.failures.key.not.added.to.keystore"));
                }
            }
        }
        return null;    // PKCS11, MSCAPI, or -protected
    }

    /*
     * Prompt the user for the password credential to be stored.
     */
    private char[] promptForCredential() throws Exception {
        // Handle password supplied via stdin
        if (System.console() == null) {
            char[] importPass = Password.readPassword(System.in);
            passwords.add(importPass);
            return importPass;
        }

        int count;
        for (count = 0; count < 3; count++) {
            System.err.print(
                rb.getString("Enter.the.password.to.be.stored."));
            System.err.flush();
            char[] entered = Password.readPassword(System.in);
            passwords.add(entered);
            System.err.print(rb.getString("Re.enter.password."));
            char[] passAgain = Password.readPassword(System.in);
            passwords.add(passAgain);
            if (!Arrays.equals(entered, passAgain)) {
                System.err.println(rb.getString("They.don.t.match.Try.again"));
                continue;
            }
            return entered;
        }

        if (count == 3) {
            throw new Exception(rb.getString
                ("Too.many.failures.key.not.added.to.keystore"));
        }

        return null;
    }

    /**
     * Creates a new secret key.
     */
    private void doGenSecretKey(String alias, String keyAlgName,
                              int keysize)
        throws Exception
    {
        if (alias == null) {
            alias = keyAlias;
        }
        if (keyStore.containsAlias(alias)) {
            MessageFormat form = new MessageFormat(rb.getString
                ("Secret.key.not.generated.alias.alias.already.exists"));
            Object[] source = {alias};
            throw new Exception(form.format(source));
        }

        // Use the keystore's default PBE algorithm for entry protection
        boolean useDefaultPBEAlgorithm = true;
        SecretKey secKey = null;

        if (keyAlgName.toUpperCase(Locale.ENGLISH).startsWith("PBE")) {
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBE");

            // User is prompted for PBE credential
            secKey =
                factory.generateSecret(new PBEKeySpec(promptForCredential()));

            // Check whether a specific PBE algorithm was specified
            if (!"PBE".equalsIgnoreCase(keyAlgName)) {
                useDefaultPBEAlgorithm = false;
            }

            if (verbose) {
                MessageFormat form = new MessageFormat(rb.getString(
                    "Generated.keyAlgName.secret.key"));
                Object[] source =
                    {useDefaultPBEAlgorithm ? "PBE" : secKey.getAlgorithm()};
                System.err.println(form.format(source));
            }
        } else {
            KeyGenerator keygen = KeyGenerator.getInstance(keyAlgName);
            if (keysize == -1) {
                if ("DES".equalsIgnoreCase(keyAlgName)) {
                    keysize = 56;
                } else if ("DESede".equalsIgnoreCase(keyAlgName)) {
                    keysize = 168;
                } else {
                    throw new Exception(rb.getString
                        ("Please.provide.keysize.for.secret.key.generation"));
                }
            }
            keygen.init(keysize);
            secKey = keygen.generateKey();

            MessageFormat form = new MessageFormat(rb.getString
                ("Generated.keysize.bit.keyAlgName.secret.key"));
            Object[] source = {keysize,
                                secKey.getAlgorithm()};
            System.err.println(form.format(source));
        }

        if (keyPass == null) {
            keyPass = promptForKeyPass(alias, null, storePass);
        }

        if (useDefaultPBEAlgorithm) {
            keyStore.setKeyEntry(alias, secKey, keyPass, null);
        } else {
            keyStore.setEntry(alias, new KeyStore.SecretKeyEntry(secKey),
                new KeyStore.PasswordProtection(keyPass, keyAlgName, null));
        }
    }

    /**
     * If no signature algorithm was specified at the command line,
     * we choose one that is compatible with the selected private key
     */
    private static String getCompatibleSigAlgName(PrivateKey key)
            throws Exception {
        String result = SignatureUtil.getDefaultSigAlgForKey(key);
        if (result != null) {
            return result;
        } else {
            throw new Exception(rb.getString
                    ("Cannot.derive.signature.algorithm"));
        }
    }

    /**
     * Creates a new key pair and self-signed certificate.
     */
    private void doGenKeyPair(String alias, String dname, String keyAlgName,
                              int keysize, String groupName, String sigAlgName,
                              String signerAlias)
        throws Exception
    {
        if (groupName != null) {
            if (keysize != -1) {
                throw new Exception(rb.getString("groupname.keysize.coexist"));
            }
        } else {
            if (keysize == -1) {
                if ("EC".equalsIgnoreCase(keyAlgName)) {
                    keysize = SecurityProviderConstants.DEF_EC_KEY_SIZE;
                } else if ("RSA".equalsIgnoreCase(keyAlgName)) {
                    keysize = SecurityProviderConstants.DEF_RSA_KEY_SIZE;
                } else if ("DSA".equalsIgnoreCase(keyAlgName)) {
                    keysize = SecurityProviderConstants.DEF_DSA_KEY_SIZE;
                } else if ("EdDSA".equalsIgnoreCase(keyAlgName)) {
                    keysize = SecurityProviderConstants.DEF_ED_KEY_SIZE;
                } else if ("Ed25519".equalsIgnoreCase(keyAlgName)) {
                    keysize = 255;
                } else if ("Ed448".equalsIgnoreCase(keyAlgName)) {
                    keysize = 448;
                } else if ("XDH".equalsIgnoreCase(keyAlgName)) {
                    keysize = SecurityProviderConstants.DEF_XEC_KEY_SIZE;
                } else if ("X25519".equalsIgnoreCase(keyAlgName)) {
                    keysize = 255;
                } else if ("X448".equalsIgnoreCase(keyAlgName)) {
                    keysize = 448;
                } else if ("DH".equalsIgnoreCase(keyAlgName)) {
                    keysize = SecurityProviderConstants.DEF_DH_KEY_SIZE;
                }
            } else {
                if ("EC".equalsIgnoreCase(keyAlgName)) {
                    weakWarnings.add(String.format(
                            rb.getString("deprecate.keysize.for.ec"),
                            ecGroupNameForSize(keysize)));
                }
            }
        }

        if (alias == null) {
            alias = keyAlias;
        }

        if (keyStore.containsAlias(alias)) {
            MessageFormat form = new MessageFormat(rb.getString
                ("Key.pair.not.generated.alias.alias.already.exists"));
            Object[] source = {alias};
            throw new Exception(form.format(source));
        }

        CertAndKeyGen keypair;
        KeyIdentifier signerSubjectKeyId = null;
        if (signerAlias != null) {
            PrivateKey signerPrivateKey =
                    (PrivateKey)recoverKey(signerAlias, storePass, signerKeyPass).fst;
            Certificate signerCert = keyStore.getCertificate(signerAlias);

            X509CertImpl signerCertImpl;
            if (signerCert instanceof X509CertImpl) {
                signerCertImpl = (X509CertImpl) signerCert;
            } else {
                signerCertImpl = new X509CertImpl(signerCert.getEncoded());
            }

            X509CertInfo signerCertInfo = (X509CertInfo)signerCertImpl.get(
                    X509CertImpl.NAME + "." + X509CertImpl.INFO);
            X500Name signerSubjectName = (X500Name)signerCertInfo.get(X509CertInfo.SUBJECT + "." +
                    X509CertInfo.DN_NAME);

            keypair = new CertAndKeyGen(keyAlgName, sigAlgName, providerName,
                    signerPrivateKey, signerSubjectName);

            signerSubjectKeyId = signerCertImpl.getSubjectKeyId();
            if (signerSubjectKeyId == null) {
                signerSubjectKeyId = new KeyIdentifier(signerCert.getPublicKey());
            }
        } else {
            keypair = new CertAndKeyGen(keyAlgName, sigAlgName, providerName);
        }

        // If DN is provided, parse it. Otherwise, prompt the user for it.
        X500Name x500Name;
        if (dname == null) {
            printWeakWarnings(true);
            x500Name = getX500Name();
        } else {
            x500Name = new X500Name(dname);
        }

        if (groupName != null) {
            keypair.generate(groupName);
        } else {
            // This covers keysize both specified and unspecified
            keypair.generate(keysize);
        }

        CertificateExtensions ext = createV3Extensions(
                null,
                null,
                v3ext,
                keypair.getPublicKeyAnyway(),
                signerSubjectKeyId);

        PrivateKey privKey = keypair.getPrivateKey();
        X509Certificate newCert = keypair.getSelfCertificate(
                x500Name, getStartDate(startDate), validity*24L*60L*60L, ext);

        if (signerAlias != null) {
            MessageFormat form = new MessageFormat(rb.getString
                    ("Generating.keysize.bit.keyAlgName.key.pair.and.a.certificate.sigAlgName.issued.by.signerAlias.with.a.validity.of.validality.days.for"));
            Object[] source = {
                    groupName == null ? keysize : KeyUtil.getKeySize(privKey),
                    fullDisplayAlgName(privKey),
                    newCert.getSigAlgName(),
                    signerAlias,
                    validity,
                    x500Name};
            System.err.println(form.format(source));
        } else {
            MessageFormat form = new MessageFormat(rb.getString
                    ("Generating.keysize.bit.keyAlgName.key.pair.and.self.signed.certificate.sigAlgName.with.a.validity.of.validality.days.for"));
            Object[] source = {
                    groupName == null ? keysize : KeyUtil.getKeySize(privKey),
                    fullDisplayAlgName(privKey),
                    newCert.getSigAlgName(),
                    validity,
                    x500Name};
            System.err.println(form.format(source));
        }

        if (keyPass == null) {
            keyPass = promptForKeyPass(alias, null, storePass);
        }

        Certificate[] finalChain;
        if (signerAlias != null) {
            Certificate[] signerChain = keyStore.getCertificateChain(signerAlias);
            finalChain = new X509Certificate[signerChain.length + 1];
            finalChain[0] = newCert;
            System.arraycopy(signerChain, 0, finalChain, 1, signerChain.length);
        } else {
            finalChain = new Certificate[] { newCert };
        }
        checkWeak(rb.getString("the.generated.certificate"), finalChain);
        keyStore.setKeyEntry(alias, privKey, keyPass, finalChain);
    }

    private String ecGroupNameForSize(int size) throws Exception {
        AlgorithmParameters ap = AlgorithmParameters.getInstance("EC");
        ap.init(new ECKeySizeParameterSpec(size));
        // The following line assumes the toString value is "name (oid)"
        return ap.toString().split(" ")[0];
    }

    /**
     * Clones an entry
     * @param orig original alias
     * @param dest destination alias
     * @changePassword if the password can be changed
     */
    private void doCloneEntry(String orig, String dest, boolean changePassword)
        throws Exception
    {
        if (orig == null) {
            orig = keyAlias;
        }

        if (keyStore.containsAlias(dest)) {
            MessageFormat form = new MessageFormat
                (rb.getString("Destination.alias.dest.already.exists"));
            Object[] source = {dest};
            throw new Exception(form.format(source));
        }

        Pair<Entry,char[]> objs = recoverEntry(keyStore, orig, storePass, keyPass);
        Entry entry = objs.fst;
        keyPass = objs.snd;

        PasswordProtection pp = null;

        if (keyPass != null) {  // protected
            if (!changePassword || P12KEYSTORE.equalsIgnoreCase(storetype)) {
                keyPassNew = keyPass;
            } else {
                if (keyPassNew == null) {
                    keyPassNew = promptForKeyPass(dest, orig, keyPass);
                }
            }
            pp = new PasswordProtection(keyPassNew);
        }
        keyStore.setEntry(dest, entry, pp);
    }

    /**
     * Changes a key password.
     */
    private void doChangeKeyPasswd(String alias) throws Exception
    {

        if (alias == null) {
            alias = keyAlias;
        }
        Pair<Key,char[]> objs = recoverKey(alias, storePass, keyPass);
        Key privKey = objs.fst;
        if (keyPass == null) {
            keyPass = objs.snd;
        }

        if (keyPassNew == null) {
            MessageFormat form = new MessageFormat
                (rb.getString("key.password.for.alias."));
            Object[] source = {alias};
            keyPassNew = getNewPasswd(form.format(source), keyPass);
        }
        keyStore.setKeyEntry(alias, privKey, keyPassNew,
                             keyStore.getCertificateChain(alias));
    }

    /**
     * Imports a JDK 1.1-style identity database. We can only store one
     * certificate per identity, because we use the identity's name as the
     * alias (which references a keystore entry), and aliases must be unique.
     */
    private void doImportIdentityDatabase(InputStream in)
        throws Exception
    {
        System.err.println(rb.getString
            ("No.entries.from.identity.database.added"));
    }

    /**
     * Prints a single keystore entry.
     */
    private void doPrintEntry(String label, String alias, PrintStream out)
        throws Exception
    {
        if (keyStore.containsAlias(alias) == false) {
            MessageFormat form = new MessageFormat
                (rb.getString("Alias.alias.does.not.exist"));
            Object[] source = {alias};
            throw new Exception(form.format(source));
        }

        if (verbose || rfc || debug) {
            MessageFormat form = new MessageFormat
                (rb.getString("Alias.name.alias"));
            Object[] source = {alias};
            out.println(form.format(source));

            if (!token) {
                form = new MessageFormat(rb.getString
                    ("Creation.date.keyStore.getCreationDate.alias."));
                Object[] src = {keyStore.getCreationDate(alias)};
                out.println(form.format(src));
            }
        } else {
            if (!token) {
                MessageFormat form = new MessageFormat
                    (rb.getString("alias.keyStore.getCreationDate.alias."));
                Object[] source = {alias, keyStore.getCreationDate(alias)};
                out.print(form.format(source));
            } else {
                MessageFormat form = new MessageFormat
                    (rb.getString("alias."));
                Object[] source = {alias};
                out.print(form.format(source));
            }
        }

        if (keyStore.entryInstanceOf(alias, KeyStore.SecretKeyEntry.class)) {
            if (verbose || rfc || debug) {
                Object[] source = {"SecretKeyEntry"};
                out.println(new MessageFormat(
                        rb.getString("Entry.type.type.")).format(source));
            } else {
                out.println("SecretKeyEntry, ");
            }
        } else if (keyStore.entryInstanceOf(alias, KeyStore.PrivateKeyEntry.class)) {
            if (verbose || rfc || debug) {
                Object[] source = {"PrivateKeyEntry"};
                out.println(new MessageFormat(
                        rb.getString("Entry.type.type.")).format(source));
            } else {
                out.println("PrivateKeyEntry, ");
            }

            // Get the chain
            Certificate[] chain = keyStore.getCertificateChain(alias);
            if (chain != null) {
                if (verbose || rfc || debug) {
                    out.println(rb.getString
                        ("Certificate.chain.length.") + chain.length);
                    for (int i = 0; i < chain.length; i ++) {
                        MessageFormat form = new MessageFormat
                                (rb.getString("Certificate.i.1."));
                        Object[] source = {(i + 1)};
                        out.println(form.format(source));
                        if (verbose && (chain[i] instanceof X509Certificate)) {
                            printX509Cert((X509Certificate)(chain[i]), out);
                        } else if (debug) {
                            out.println(chain[i].toString());
                        } else {
                            dumpCert(chain[i], out);
                        }
                        checkWeak(label, chain[i]);
                    }
                } else {
                    // Print the digest of the user cert only
                    out.println
                        (rb.getString("Certificate.fingerprint.SHA.256.") +
                        getCertFingerPrint("SHA-256", chain[0]));
                    checkWeak(label, chain);
                }
            } else {
                out.println(rb.getString
                        ("Certificate.chain.length.") + 0);
            }
        } else if (keyStore.entryInstanceOf(alias,
                KeyStore.TrustedCertificateEntry.class)) {
            // We have a trusted certificate entry
            Certificate cert = keyStore.getCertificate(alias);
            Object[] source = {"trustedCertEntry"};
            String mf = new MessageFormat(
                    rb.getString("Entry.type.type.")).format(source) + "\n";
            if (verbose && (cert instanceof X509Certificate)) {
                out.println(mf);
                printX509Cert((X509Certificate)cert, out);
            } else if (rfc) {
                out.println(mf);
                dumpCert(cert, out);
            } else if (debug) {
                out.println(cert.toString());
            } else {
                out.println("trustedCertEntry, ");
                out.println(rb.getString("Certificate.fingerprint.SHA.256.")
                            + getCertFingerPrint("SHA-256", cert));
            }
            checkWeak(label, cert);
        } else {
            out.println(rb.getString("Unknown.Entry.Type"));
        }
    }

    boolean inplaceImportCheck() throws Exception {
        if (P11KEYSTORE.equalsIgnoreCase(srcstoretype) ||
                KeyStoreUtil.isWindowsKeyStore(srcstoretype)) {
            return false;
        }

        if (srcksfname != null) {
            File srcksfile = new File(srcksfname);
            if (srcksfile.exists() && srcksfile.length() == 0) {
                throw new Exception(rb.getString
                        ("Source.keystore.file.exists.but.is.empty.") +
                        srcksfname);
            }
            if (srcksfile.getCanonicalFile()
                    .equals(new File(ksfname).getCanonicalFile())) {
                return true;
            } else {
                // Informational, especially if destkeystore is not
                // provided, which default to ~/.keystore.
                System.err.println(String.format(rb.getString(
                        "importing.keystore.status"), srcksfname, ksfname));
                return false;
            }
        } else {
            throw new Exception(rb.getString
                    ("Please.specify.srckeystore"));
        }
    }

    /**
     * Load the srckeystore from a stream, used in -importkeystore
     * @return the src KeyStore
     */
    KeyStore loadSourceKeyStore() throws Exception {

        InputStream is = null;
        File srcksfile = null;
        boolean srcIsPasswordless = false;

        if (P11KEYSTORE.equalsIgnoreCase(srcstoretype) ||
                KeyStoreUtil.isWindowsKeyStore(srcstoretype)) {
            if (!NONE.equals(srcksfname)) {
                System.err.println(MessageFormat.format(rb.getString
                    (".keystore.must.be.NONE.if.storetype.is.{0}"), srcstoretype));
                System.err.println();
                tinyHelp();
            }
        } else {
            srcksfile = new File(srcksfname);
            is = new FileInputStream(srcksfile);
        }

        KeyStore store;
        try {
            // Probe for keystore type when filename is available
            if (srcksfile != null && is != null && srcProviderName == null &&
                    srcstoretype == null) {
                store = KeyStore.getInstance(srcksfile, srcstorePass);
                srcstoretype = store.getType();
                if (srcstoretype.equalsIgnoreCase("pkcs12")) {
                    srcIsPasswordless = PKCS12KeyStore.isPasswordless(srcksfile);
                }
            } else {
                if (srcstoretype == null) {
                    srcstoretype = KeyStore.getDefaultType();
                }
                if (srcProviderName == null) {
                    store = KeyStore.getInstance(srcstoretype);
                } else {
                    store = KeyStore.getInstance(srcstoretype, srcProviderName);
                }
            }

            if (srcstorePass == null
                    && !srcprotectedPath
                    && !KeyStoreUtil.isWindowsKeyStore(srcstoretype)
                    && !srcIsPasswordless) {
                System.err.print(rb.getString("Enter.source.keystore.password."));
                System.err.flush();
                srcstorePass = Password.readPassword(System.in);
                passwords.add(srcstorePass);
            }

            // always let keypass be storepass when using pkcs12
            if (P12KEYSTORE.equalsIgnoreCase(srcstoretype)) {
                if (srckeyPass != null && srcstorePass != null &&
                        !Arrays.equals(srcstorePass, srckeyPass)) {
                    MessageFormat form = new MessageFormat(rb.getString(
                        "Warning.Different.store.and.key.passwords.not.supported.for.PKCS12.KeyStores.Ignoring.user.specified.command.value."));
                    Object[] source = {"-srckeypass"};
                    System.err.println(form.format(source));
                    srckeyPass = srcstorePass;
                }
            }

            store.load(is, srcstorePass);   // "is" already null in PKCS11
        } finally {
            if (is != null) {
                is.close();
            }
        }

        if (srcstorePass == null
                && !srcIsPasswordless
                && !KeyStoreUtil.isWindowsKeyStore(srcstoretype)) {
            // anti refactoring, copied from printNoIntegrityWarning(),
            // but change 2 lines
            System.err.println();
            System.err.println(rb.getString
                (".WARNING.WARNING.WARNING."));
            System.err.println(rb.getString
                (".The.integrity.of.the.information.stored.in.the.srckeystore."));
            System.err.println(rb.getString
                (".WARNING.WARNING.WARNING."));
            System.err.println();
        }

        return store;
    }

    /**
     * import all keys and certs from importkeystore.
     * keep alias unchanged if no name conflict, otherwise, prompt.
     * keep keypass unchanged for keys
     */
    private void doImportKeyStore(KeyStore srcKS) throws Exception {

        if (alias != null) {
            doImportKeyStoreSingle(srcKS, alias);
        } else {
            if (dest != null || srckeyPass != null) {
                throw new Exception(rb.getString(
                        "if.alias.not.specified.destalias.and.srckeypass.must.not.be.specified"));
            }
            doImportKeyStoreAll(srcKS);
        }

        if (inplaceImport) {
            // Backup to file.old or file.old2...
            // The keystore is not rewritten yet now.
            for (int n = 1; /* forever */; n++) {
                inplaceBackupName = srcksfname + ".old" + (n == 1 ? "" : n);
                File bkFile = new File(inplaceBackupName);
                if (!bkFile.exists()) {
                    Files.copy(Path.of(srcksfname), bkFile.toPath());
                    break;
                }
            }

        }

        /*
         * Information display rule of -importkeystore
         * 1. inside single, shows failure
         * 2. inside all, shows sucess
         * 3. inside all where there is a failure, prompt for continue
         * 4. at the final of all, shows summary
         */
    }

    /**
     * Import a single entry named alias from srckeystore
     * @return  1 if the import action succeed
     *          0 if user choose to ignore an alias-dumplicated entry
     *          2 if setEntry throws Exception
     */
    private int doImportKeyStoreSingle(KeyStore srckeystore, String alias)
            throws Exception {

        String newAlias = (dest==null) ? alias : dest;

        if (keyStore.containsAlias(newAlias)) {
            Object[] source = {alias};
            if (noprompt) {
                System.err.println(new MessageFormat(rb.getString(
                        "Warning.Overwriting.existing.alias.alias.in.destination.keystore")).format(source));
            } else {
                String reply = getYesNoReply(new MessageFormat(rb.getString(
                        "Existing.entry.alias.alias.exists.overwrite.no.")).format(source));
                if ("NO".equals(reply)) {
                    newAlias = inputStringFromStdin(rb.getString
                            ("Enter.new.alias.name.RETURN.to.cancel.import.for.this.entry."));
                    if ("".equals(newAlias)) {
                        System.err.println(new MessageFormat(rb.getString(
                                "Entry.for.alias.alias.not.imported.")).format(
                                source));
                        return 0;
                    }
                }
            }
        }

        Pair<Entry,char[]> objs = recoverEntry(srckeystore, alias, srcstorePass, srckeyPass);
        Entry entry = objs.fst;

        PasswordProtection pp = null;

        // According to keytool.html, "The destination entry will be protected
        // using destkeypass. If destkeypass is not provided, the destination
        // entry will be protected with the source entry password."
        // so always try to protect with destKeyPass.
        char[] newPass = null;
        if (destKeyPass != null) {
            newPass = destKeyPass;
            pp = new PasswordProtection(destKeyPass);
        } else if (objs.snd != null) {
            if (P12KEYSTORE.equalsIgnoreCase(storetype)) {
                if (isPasswordlessKeyStore) {
                    newPass = objs.snd;
                } else {
                    newPass = storePass;
                }
            } else {
                newPass = objs.snd;
            }
            pp = new PasswordProtection(newPass);
        }

        try {
            Certificate c = srckeystore.getCertificate(alias);
            if (c != null) {
                checkWeak("<" + newAlias + ">", c);
            }
            keyStore.setEntry(newAlias, entry, pp);
            // Place the check so that only successful imports are blocked.
            // For example, we don't block a failed SecretEntry import.
            if (P12KEYSTORE.equalsIgnoreCase(storetype) && !isPasswordlessKeyStore) {
                if (newPass != null && !Arrays.equals(newPass, storePass)) {
                    throw new Exception(rb.getString(
                            "The.destination.pkcs12.keystore.has.different.storepass.and.keypass.Please.retry.with.destkeypass.specified."));
                }
            }
            return 1;
        } catch (KeyStoreException kse) {
            Object[] source2 = {alias, kse.toString()};
            MessageFormat form = new MessageFormat(rb.getString(
                    "Problem.importing.entry.for.alias.alias.exception.Entry.for.alias.alias.not.imported."));
            System.err.println(form.format(source2));
            return 2;
        }
    }

    private void doImportKeyStoreAll(KeyStore srckeystore) throws Exception {

        int ok = 0;
        int count = srckeystore.size();
        for (Enumeration<String> e = srckeystore.aliases();
                                        e.hasMoreElements(); ) {
            String alias = e.nextElement();
            int result = doImportKeyStoreSingle(srckeystore, alias);
            if (result == 1) {
                ok++;
                Object[] source = {alias};
                MessageFormat form = new MessageFormat(rb.getString("Entry.for.alias.alias.successfully.imported."));
                System.err.println(form.format(source));
            } else if (result == 2) {
                if (!noprompt) {
                    String reply = getYesNoReply("Do you want to quit the import process? [no]:  ");
                    if ("YES".equals(reply)) {
                        break;
                    }
                }
            }
        }
        Object[] source = {ok, count-ok};
        MessageFormat form = new MessageFormat(rb.getString(
                "Import.command.completed.ok.entries.successfully.imported.fail.entries.failed.or.cancelled"));
        System.err.println(form.format(source));
    }

    /**
     * Prints all keystore entries.
     */
    private void doPrintEntries(PrintStream out)
        throws Exception
    {
        out.println(rb.getString("Keystore.type.") + keyStore.getType());
        out.println(rb.getString("Keystore.provider.") +
                keyStore.getProvider().getName());
        out.println();

        MessageFormat form;
        form = (keyStore.size() == 1) ?
                new MessageFormat(rb.getString
                        ("Your.keystore.contains.keyStore.size.entry")) :
                new MessageFormat(rb.getString
                        ("Your.keystore.contains.keyStore.size.entries"));
        Object[] source = {keyStore.size()};
        out.println(form.format(source));
        out.println();

        List<String> aliases = Collections.list(keyStore.aliases());
        aliases.sort(String::compareTo);
        for (String alias : aliases) {
            doPrintEntry("<" + alias + ">", alias, out);
            if (verbose || rfc) {
                out.println(rb.getString("NEWLINE"));
                out.println(rb.getString
                        ("STAR"));
                out.println(rb.getString
                        ("STARNN"));
            }
        }
    }

    /**
     * Loads CRLs from a source. This method is also called in JarSigner.
     * @param src the source, which means System.in if null, or a URI,
     *        or a bare file path name
     */
    public static Collection<? extends CRL> loadCRLs(String src) throws Exception {
        InputStream in = null;
        URI uri = null;
        if (src == null) {
            in = System.in;
        } else {
            try {
                uri = new URI(src);
                if (uri.getScheme().equals("ldap")) {
                    // No input stream for LDAP
                } else {
                    in = uri.toURL().openStream();
                }
            } catch (Exception e) {
                try {
                    in = new FileInputStream(src);
                } catch (Exception e2) {
                    if (uri == null || uri.getScheme() == null) {
                        throw e2;   // More likely a bare file path
                    } else {
                        throw e;    // More likely a protocol or network problem
                    }
                }
            }
        }
        if (in != null) {
            try {
                // Read the full stream before feeding to X509Factory,
                // otherwise, keytool -gencrl | keytool -printcrl
                // might not work properly, since -gencrl is slow
                // and there's no data in the pipe at the beginning.
                byte[] bytes = in.readAllBytes();
                return CertificateFactory.getInstance("X509").generateCRLs(
                        new ByteArrayInputStream(bytes));
            } finally {
                if (in != System.in) {
                    in.close();
                }
            }
        } else {    // must be LDAP, and uri is not null
            URICertStoreParameters params =
                new URICertStoreParameters(uri);
            CertStore s = CertStore.getInstance("LDAP", params);
            return s.getCRLs(new X509CRLSelector());
        }
    }

    /**
     * Returns CRLs described in a X509Certificate's CRLDistributionPoints
     * Extension. Only those containing a general name of type URI are read.
     */
    public static List<CRL> readCRLsFromCert(X509Certificate cert)
            throws Exception {
        List<CRL> crls = new ArrayList<>();
        CRLDistributionPointsExtension ext =
                X509CertImpl.toImpl(cert).getCRLDistributionPointsExtension();
        if (ext == null) return crls;
        List<DistributionPoint> distPoints =
                ext.get(CRLDistributionPointsExtension.POINTS);
        for (DistributionPoint o: distPoints) {
            GeneralNames names = o.getFullName();
            if (names != null) {
                for (GeneralName name: names.names()) {
                    if (name.getType() == GeneralNameInterface.NAME_URI) {
                        URIName uriName = (URIName)name.getName();
                        for (CRL crl: loadCRLs(uriName.getName())) {
                            if (crl instanceof X509CRL) {
                                crls.add((X509CRL)crl);
                            }
                        }
                        break;  // Different name should point to same CRL
                    }
                }
            }
        }
        return crls;
    }

    private static String verifyCRL(KeyStore ks, CRL crl)
            throws Exception {
        X509CRL xcrl = (X509CRL)crl;
        X500Principal issuer = xcrl.getIssuerX500Principal();
        for (String s: Collections.list(ks.aliases())) {
            Certificate cert = ks.getCertificate(s);
            if (cert instanceof X509Certificate) {
                X509Certificate xcert = (X509Certificate)cert;
                if (xcert.getSubjectX500Principal().equals(issuer)) {
                    try {
                        ((X509CRL)crl).verify(cert.getPublicKey());
                        return s;
                    } catch (Exception e) {
                    }
                }
            }
        }
        return null;
    }

    private void doPrintCRL(String src, PrintStream out)
            throws Exception {
        for (CRL crl: loadCRLs(src)) {
            printCRL(crl, out);
            String issuer = null;
            Certificate signer = null;
            if (caks != null) {
                issuer = verifyCRL(caks, crl);
                if (issuer != null) {
                    signer = caks.getCertificate(issuer);
                    out.printf(rb.getString(
                            "verified.by.s.in.s.weak"),
                            issuer,
                            "cacerts",
                            withWeak(signer.getPublicKey()));
                    out.println();
                }
            }
            if (issuer == null && keyStore != null) {
                issuer = verifyCRL(keyStore, crl);
                if (issuer != null) {
                    signer = keyStore.getCertificate(issuer);
                    out.printf(rb.getString(
                            "verified.by.s.in.s.weak"),
                            issuer,
                            "keystore",
                            withWeak(signer.getPublicKey()));
                    out.println();
                }
            }
            if (issuer == null) {
                out.println(rb.getString
                        ("STAR"));
                if (trustcacerts) {
                    out.println(rb.getString
                            ("warning.not.verified.make.sure.keystore.is.correct"));
                } else {
                    out.println(rb.getString
                            ("warning.not.verified.make.sure.keystore.is.correct.or.specify.trustcacerts"));
                }
                out.println(rb.getString
                        ("STARNN"));
            }
            checkWeak(rb.getString("the.crl"), crl, signer == null ? null : signer.getPublicKey());
        }
    }

    private void printCRL(CRL crl, PrintStream out)
            throws Exception {
        X509CRL xcrl = (X509CRL)crl;
        if (rfc) {
            out.println("-----BEGIN X509 CRL-----");
            out.println(Base64.getMimeEncoder(64, CRLF).encodeToString(xcrl.getEncoded()));
            out.println("-----END X509 CRL-----");
        } else {
            String s;
            if (crl instanceof X509CRLImpl) {
                X509CRLImpl x509crl = (X509CRLImpl) crl;
                s = x509crl.toStringWithAlgName(withWeak("" + x509crl.getSigAlgId()));
            } else {
                s = crl.toString();
            }
            out.println(s);
        }
    }

    private void doPrintCertReq(InputStream in, PrintStream out)
            throws Exception {

        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        StringBuilder sb = new StringBuilder();
        boolean started = false;
        while (true) {
            String s = reader.readLine();
            if (s == null) break;
            if (!started) {
                if (s.startsWith("-----")) {
                    started = true;
                }
            } else {
                if (s.startsWith("-----")) {
                    break;
                }
                sb.append(s);
            }
        }
        PKCS10 req = new PKCS10(Pem.decode(new String(sb)));

        PublicKey pkey = req.getSubjectPublicKeyInfo();
        out.printf(rb.getString("PKCS.10.with.weak"),
                req.getSubjectName(),
                pkey.getFormat(),
                withWeak(pkey),
                withWeak(req.getSigAlg()));
        for (PKCS10Attribute attr: req.getAttributes().getAttributes()) {
            ObjectIdentifier oid = attr.getAttributeId();
            if (oid.equals(PKCS9Attribute.EXTENSION_REQUEST_OID)) {
                CertificateExtensions exts = (CertificateExtensions)attr.getAttributeValue();
                if (exts != null) {
                    printExtensions(rb.getString("Extension.Request."), exts, out);
                }
            } else {
                out.println("Attribute: " + attr.getAttributeId());
                PKCS9Attribute pkcs9Attr =
                        new PKCS9Attribute(attr.getAttributeId(),
                                           attr.getAttributeValue());
                out.print(pkcs9Attr.getName() + ": ");
                Object attrVal = attr.getAttributeValue();
                out.println(attrVal instanceof String[] ?
                            Arrays.toString((String[]) attrVal) :
                            attrVal);
            }
        }
        if (debug) {
            out.println(req);   // Just to see more, say, public key length...
        }
        checkWeak(rb.getString("the.certificate.request"), req);
    }

    /**
     * Reads a certificate (or certificate chain) and prints its contents in
     * a human readable format.
     */
    private void printCertFromStream(InputStream in, PrintStream out)
        throws Exception
    {
        Collection<? extends Certificate> c = null;
        try {
            c = generateCertificates(in);
        } catch (CertificateException ce) {
            throw new Exception(rb.getString("Failed.to.parse.input"), ce);
        }
        if (c.isEmpty()) {
            throw new Exception(rb.getString("Empty.input"));
        }
        Certificate[] certs = c.toArray(new Certificate[c.size()]);
        for (int i=0; i<certs.length; i++) {
            X509Certificate x509Cert = null;
            try {
                x509Cert = (X509Certificate)certs[i];
            } catch (ClassCastException cce) {
                throw new Exception(rb.getString("Not.X.509.certificate"));
            }
            if (certs.length > 1) {
                MessageFormat form = new MessageFormat
                        (rb.getString("Certificate.i.1."));
                Object[] source = {i + 1};
                out.println(form.format(source));
            }
            if (rfc)
                dumpCert(x509Cert, out);
            else
                printX509Cert(x509Cert, out);
            if (i < (certs.length-1)) {
                out.println();
            }
            checkWeak(oneInMany(rb.getString("the.certificate"), i, certs.length), x509Cert);
        }
    }

    private void doShowInfo() throws Exception {
        if (tlsInfo) {
            ShowInfo.tls(verbose);
        } else {
            System.out.println(rb.getString("showinfo.no.option"));
        }
    }

    private Collection<? extends Certificate> generateCertificates(InputStream in)
            throws CertificateException, IOException {
        byte[] data = in.readAllBytes();
        try {
            return CertificateFactory.getInstance("X.509")
                    .generateCertificates(new ByteArrayInputStream(data));
        } catch (CertificateException e) {
            if (providerName != null) {
                try {
                    return CertificateFactory.getInstance("X.509", providerName)
                            .generateCertificates(new ByteArrayInputStream(data));
                } catch (Exception e2) {
                    e.addSuppressed(e2);
                }
            }
            throw e;
        }
    }

    private Certificate generateCertificate(InputStream in)
            throws CertificateException, IOException {
        byte[] data = in.readAllBytes();
        try {
            return CertificateFactory.getInstance("X.509")
                    .generateCertificate(new ByteArrayInputStream(data));
        } catch (CertificateException e) {
            if (providerName != null) {
                try {
                    return CertificateFactory.getInstance("X.509", providerName)
                            .generateCertificate(new ByteArrayInputStream(data));
                } catch (Exception e2) {
                    e.addSuppressed(e2);
                }
            }
            throw e;
        }
    }

    private static String oneInMany(String label, int i, int num) {
        if (num == 1) {
            return label;
        } else {
            return String.format(rb.getString("one.in.many"), label, i+1, num);
        }
    }

    private void doPrintCert(final PrintStream out) throws Exception {
        if (jarfile != null) {
            // reset "jdk.certpath.disabledAlgorithms" security property
            // to be able to read jars which were signed with weak algorithms
            Security.setProperty(DisabledAlgorithmConstraints.PROPERTY_JAR_DISABLED_ALGS, "");

            JarFile jf = new JarFile(jarfile, true);
            Enumeration<JarEntry> entries = jf.entries();
            Set<CodeSigner> ss = new HashSet<>();
            byte[] buffer = new byte[8192];
            int pos = 0;
            while (entries.hasMoreElements()) {
                JarEntry je = entries.nextElement();
                try (InputStream is = jf.getInputStream(je)) {
                    while (is.read(buffer) != -1) {
                        // we just read. this will throw a SecurityException
                        // if a signature/digest check fails. This also
                        // populate the signers
                    }
                }
                CodeSigner[] signers = je.getCodeSigners();
                if (signers != null) {
                    for (CodeSigner signer: signers) {
                        if (!ss.contains(signer)) {
                            ss.add(signer);
                            out.printf(rb.getString("Signer.d."), ++pos);
                            out.println();
                            out.println();
                            out.println(rb.getString("Signature."));
                            out.println();

                            List<? extends Certificate> certs
                                    = signer.getSignerCertPath().getCertificates();
                            int cc = 0;
                            for (Certificate cert: certs) {
                                X509Certificate x = (X509Certificate)cert;
                                if (rfc) {
                                    out.println(rb.getString("Certificate.owner.") + x.getSubjectX500Principal() + "\n");
                                    dumpCert(x, out);
                                } else {
                                    printX509Cert(x, out);
                                }
                                out.println();
                                checkWeak(oneInMany(rb.getString("the.certificate"), cc++, certs.size()), x);
                            }
                            Timestamp ts = signer.getTimestamp();
                            if (ts != null) {
                                out.println(rb.getString("Timestamp."));
                                out.println();
                                certs = ts.getSignerCertPath().getCertificates();
                                cc = 0;
                                for (Certificate cert: certs) {
                                    X509Certificate x = (X509Certificate)cert;
                                    if (rfc) {
                                        out.println(rb.getString("Certificate.owner.") + x.getSubjectX500Principal() + "\n");
                                        dumpCert(x, out);
                                    } else {
                                        printX509Cert(x, out);
                                    }
                                    out.println();
                                    checkWeak(oneInMany(rb.getString("the.tsa.certificate"), cc++, certs.size()), x);
                                }
                            }
                        }
                    }
                }
            }
            jf.close();
            if (ss.isEmpty()) {
                out.println(rb.getString("Not.a.signed.jar.file"));
            }
        } else if (sslserver != null) {
            CertStore cs = SSLServerCertStore.getInstance(new URI("https://" + sslserver));
            Collection<? extends Certificate> chain;
            try {
                chain = cs.getCertificates(null);
                if (chain.isEmpty()) {
                    // If the certs are not retrieved, we consider it an error
                    // even if the URL connection is successful.
                    throw new Exception(rb.getString(
                                        "No.certificate.from.the.SSL.server"));
                }
            } catch (CertStoreException cse) {
                if (cse.getCause() instanceof IOException) {
                    throw new Exception(rb.getString(
                                        "No.certificate.from.the.SSL.server"),
                                        cse.getCause());
                } else {
                    throw cse;
                }
            }

            int i = 0;
            for (Certificate cert : chain) {
                try {
                    if (rfc) {
                        dumpCert(cert, out);
                    } else {
                        out.println("Certificate #" + i);
                        out.println("====================================");
                        printX509Cert((X509Certificate)cert, out);
                        out.println();
                    }
                    checkWeak(oneInMany(rb.getString("the.certificate"), i++, chain.size()), cert);
                } catch (Exception e) {
                    if (debug) {
                        e.printStackTrace();
                    }
                }
            }
        } else {
            if (filename != null) {
                try (FileInputStream inStream = new FileInputStream(filename)) {
                    printCertFromStream(inStream, out);
                }
            } else {
                printCertFromStream(System.in, out);
            }
        }
    }

    private void doChangeStorePasswd() throws Exception {
        storePassNew = newPass;
        if (storePassNew == null) {
            storePassNew = getNewPasswd("keystore password", storePass);
        }
        if (P12KEYSTORE.equalsIgnoreCase(storetype)) {
            // When storetype is PKCS12, we need to change all keypass as well
            for (String alias : Collections.list(keyStore.aliases())) {
                if (!keyStore.isCertificateEntry(alias)) {
                    // keyPass should be either null or same with storePass,
                    // but keep it in case one day we want to "normalize"
                    // a PKCS12 keystore having different passwords.
                    Pair<Entry, char[]> objs
                            = recoverEntry(keyStore, alias, storePass, keyPass);
                    keyStore.setEntry(alias, objs.fst,
                            new PasswordProtection(storePassNew));
                }
            }
        }
    }

    /**
     * Creates a self-signed certificate, and stores it as a single-element
     * certificate chain.
     */
    private void doSelfCert(String alias, String dname, String sigAlgName)
        throws Exception
    {
        if (alias == null) {
            alias = keyAlias;
        }

        Pair<Key,char[]> objs = recoverKey(alias, storePass, keyPass);
        PrivateKey privKey = (PrivateKey)objs.fst;
        if (keyPass == null)
            keyPass = objs.snd;

        // Determine the signature algorithm
        if (sigAlgName == null) {
            sigAlgName = getCompatibleSigAlgName(privKey);
        }

        // Get the old certificate
        Certificate oldCert = keyStore.getCertificate(alias);
        if (oldCert == null) {
            MessageFormat form = new MessageFormat
                (rb.getString("alias.has.no.public.key"));
            Object[] source = {alias};
            throw new Exception(form.format(source));
        }
        if (!(oldCert instanceof X509Certificate)) {
            MessageFormat form = new MessageFormat
                (rb.getString("alias.has.no.X.509.certificate"));
            Object[] source = {alias};
            throw new Exception(form.format(source));
        }

        // convert to X509CertImpl, so that we can modify selected fields
        // (no public APIs available yet)
        byte[] encoded = oldCert.getEncoded();
        X509CertImpl certImpl = new X509CertImpl(encoded);
        X509CertInfo certInfo = (X509CertInfo)certImpl.get(X509CertImpl.NAME
                                                           + "." +
                                                           X509CertImpl.INFO);

        // Extend its validity
        Date firstDate = getStartDate(startDate);
        Date lastDate = new Date();
        lastDate.setTime(firstDate.getTime() + validity*1000L*24L*60L*60L);
        CertificateValidity interval = new CertificateValidity(firstDate,
                                                               lastDate);
        certInfo.set(X509CertInfo.VALIDITY, interval);

        // Make new serial number
        certInfo.set(X509CertInfo.SERIAL_NUMBER,
                CertificateSerialNumber.newRandom64bit(new SecureRandom()));

        // Set owner and issuer fields
        X500Name owner;
        if (dname == null) {
            // Get the owner name from the certificate
            owner = (X500Name)certInfo.get(X509CertInfo.SUBJECT + "." +
                                           X509CertInfo.DN_NAME);
        } else {
            // Use the owner name specified at the command line
            owner = new X500Name(dname);
            certInfo.set(X509CertInfo.SUBJECT + "." +
                         X509CertInfo.DN_NAME, owner);
        }
        // Make issuer same as owner (self-signed!)
        certInfo.set(X509CertInfo.ISSUER + "." +
                     X509CertInfo.DN_NAME, owner);

        certInfo.set(X509CertInfo.VERSION,
                        new CertificateVersion(CertificateVersion.V3));

        CertificateExtensions ext = createV3Extensions(
                null,
                (CertificateExtensions)certInfo.get(X509CertInfo.EXTENSIONS),
                v3ext,
                oldCert.getPublicKey(),
                null);
        certInfo.set(X509CertInfo.EXTENSIONS, ext);
        // Sign the new certificate
        X509CertImpl newCert = new X509CertImpl(certInfo);
        newCert.sign(privKey, sigAlgName);

        // Store the new certificate as a single-element certificate chain
        keyStore.setKeyEntry(alias, privKey,
                             (keyPass != null) ? keyPass : storePass,
                             new Certificate[] { newCert } );

        if (verbose) {
            System.err.println(rb.getString("New.certificate.self.signed."));
            System.err.print(newCert.toString());
            System.err.println();
        }
    }

    /**
     * Processes a certificate reply from a certificate authority.
     *
     * <p>Builds a certificate chain on top of the certificate reply,
     * using trusted certificates from the keystore. The chain is complete
     * after a self-signed certificate has been encountered. The self-signed
     * certificate is considered a root certificate authority, and is stored
     * at the end of the chain.
     *
     * <p>The newly generated chain replaces the old chain associated with the
     * key entry.
     *
     * @return true if the certificate reply was installed, otherwise false.
     */
    private boolean installReply(String alias, InputStream in)
        throws Exception
    {
        if (alias == null) {
            alias = keyAlias;
        }

        Pair<Key,char[]> objs = recoverKey(alias, storePass, keyPass);
        PrivateKey privKey = (PrivateKey)objs.fst;
        if (keyPass == null) {
            keyPass = objs.snd;
        }

        Certificate userCert = keyStore.getCertificate(alias);
        if (userCert == null) {
            MessageFormat form = new MessageFormat
                (rb.getString("alias.has.no.public.key.certificate."));
            Object[] source = {alias};
            throw new Exception(form.format(source));
        }

        // Read the certificates in the reply
        Collection<? extends Certificate> c = generateCertificates(in);
        if (c.isEmpty()) {
            throw new Exception(rb.getString("Reply.has.no.certificates"));
        }
        Certificate[] replyCerts = c.toArray(new Certificate[c.size()]);
        Certificate[] newChain;
        if (replyCerts.length == 1) {
            // single-cert reply
            newChain = establishCertChain(userCert, replyCerts[0]);
        } else {
            // cert-chain reply (e.g., PKCS#7)
            newChain = validateReply(alias, userCert, replyCerts);
        }

        // Now store the newly established chain in the keystore. The new
        // chain replaces the old one. The chain can be null if user chooses no.
        if (newChain != null) {
            keyStore.setKeyEntry(alias, privKey,
                                 (keyPass != null) ? keyPass : storePass,
                                 newChain);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Imports a certificate and adds it to the list of trusted certificates.
     *
     * @return true if the certificate was added, otherwise false.
     */
    private boolean addTrustedCert(String alias, InputStream in)
        throws Exception
    {
        if (alias == null) {
            throw new Exception(rb.getString("Must.specify.alias"));
        }
        if (keyStore.containsAlias(alias)) {
            MessageFormat form = new MessageFormat(rb.getString
                ("Certificate.not.imported.alias.alias.already.exists"));
            Object[] source = {alias};
            throw new Exception(form.format(source));
        }

        // Read the certificate
        X509Certificate cert = null;
        try {
            cert = (X509Certificate)generateCertificate(in);
        } catch (ClassCastException | CertificateException ce) {
            throw new Exception(rb.getString("Input.not.an.X.509.certificate"));
        }

        if (noprompt) {
            checkWeak(rb.getString("the.input"), cert);
            keyStore.setCertificateEntry(alias, cert);
            return true;
        }

        // if certificate is self-signed, make sure it verifies
        boolean selfSigned = false;
        if (KeyStoreUtil.isSelfSigned(cert)) {
            cert.verify(cert.getPublicKey());
            selfSigned = true;
        }

        // check if cert already exists in keystore
        String reply = null;
        String trustalias = keyStore.getCertificateAlias(cert);
        if (trustalias != null) {
            MessageFormat form = new MessageFormat(rb.getString
                ("Certificate.already.exists.in.keystore.under.alias.trustalias."));
            Object[] source = {trustalias};
            System.err.println(form.format(source));
            checkWeak(rb.getString("the.input"), cert);
            printWeakWarnings(true);
            reply = getYesNoReply
                (rb.getString("Do.you.still.want.to.add.it.no."));
        } else if (selfSigned) {
            if (trustcacerts && (caks != null) &&
                    ((trustalias=caks.getCertificateAlias(cert)) != null)) {
                MessageFormat form = new MessageFormat(rb.getString
                        ("Certificate.already.exists.in.system.wide.CA.keystore.under.alias.trustalias."));
                Object[] source = {trustalias};
                System.err.println(form.format(source));
                checkWeak(rb.getString("the.input"), cert);
                printWeakWarnings(true);
                reply = getYesNoReply
                        (rb.getString("Do.you.still.want.to.add.it.to.your.own.keystore.no."));
            }
            if (trustalias == null) {
                // Print the cert and ask user if they really want to add
                // it to their keystore
                printX509Cert(cert, System.out);
                checkWeak(rb.getString("the.input"), cert);
                printWeakWarnings(true);
                reply = getYesNoReply
                        (rb.getString("Trust.this.certificate.no."));
            }
        }
        if (reply != null) {
            if ("YES".equals(reply)) {
                keyStore.setCertificateEntry(alias, cert);
                return true;
            } else {
                return false;
            }
        }

        // Not found in this keystore and not self-signed
        // Try to establish trust chain
        try {
            Certificate[] chain = establishCertChain(null, cert);
            if (chain != null) {
                keyStore.setCertificateEntry(alias, cert);
                return true;
            }
        } catch (Exception e) {
            // Print the cert and ask user if they really want to add it to
            // their keystore
            printX509Cert(cert, System.out);
            checkWeak(rb.getString("the.input"), cert);
            printWeakWarnings(true);
            reply = getYesNoReply
                (rb.getString("Trust.this.certificate.no."));
            if ("YES".equals(reply)) {
                keyStore.setCertificateEntry(alias, cert);
                return true;
            } else {
                return false;
            }
        }

        return false;
    }

    /**
     * Prompts user for new password. New password must be different from
     * old one.
     *
     * @param prompt the message that gets prompted on the screen
     * @param oldPasswd the current (i.e., old) password
     */
    private char[] getNewPasswd(String prompt, char[] oldPasswd)
        throws Exception
    {
        char[] entered = null;
        char[] reentered = null;

        for (int count = 0; count < 3; count++) {
            MessageFormat form = new MessageFormat
                (rb.getString("New.prompt."));
            Object[] source = {prompt};
            System.err.print(form.format(source));
            entered = Password.readPassword(System.in);
            passwords.add(entered);
            if (entered == null || entered.length < 6) {
                System.err.println(rb.getString
                    ("Password.is.too.short.must.be.at.least.6.characters"));
            } else if (Arrays.equals(entered, oldPasswd)) {
                System.err.println(rb.getString("Passwords.must.differ"));
            } else {
                form = new MessageFormat
                        (rb.getString("Re.enter.new.prompt."));
                Object[] src = {prompt};
                System.err.print(form.format(src));
                reentered = Password.readPassword(System.in);
                passwords.add(reentered);
                if (!Arrays.equals(entered, reentered)) {
                    System.err.println
                        (rb.getString("They.don.t.match.Try.again"));
                } else {
                    Arrays.fill(reentered, ' ');
                    return entered;
                }
            }
            if (entered != null) {
                Arrays.fill(entered, ' ');
                entered = null;
            }
            if (reentered != null) {
                Arrays.fill(reentered, ' ');
                reentered = null;
            }
        }
        throw new Exception(rb.getString("Too.many.failures.try.later"));
    }

    /**
     * Prompts user for alias name.
     * @param prompt the {0} of "Enter {0} alias name:  " in prompt line
     * @return the string entered by the user, without the \n at the end
     */
    private String getAlias(String prompt) throws Exception {
        if (prompt != null) {
            MessageFormat form = new MessageFormat
                (rb.getString("Enter.prompt.alias.name."));
            Object[] source = {prompt};
            System.err.print(form.format(source));
        } else {
            System.err.print(rb.getString("Enter.alias.name."));
        }
        return (new BufferedReader(new InputStreamReader(
                                        System.in))).readLine();
    }

    /**
     * Prompts user for an input string from the command line (System.in)
     * @prompt the prompt string printed
     * @return the string entered by the user, without the \n at the end
     */
    private String inputStringFromStdin(String prompt) throws Exception {
        System.err.print(prompt);
        return (new BufferedReader(new InputStreamReader(
                                        System.in))).readLine();
    }

    /**
     * Prompts user for key password. User may select to choose the same
     * password (<code>otherKeyPass</code>) as for <code>otherAlias</code>.
     */
    private char[] getKeyPasswd(String alias, String otherAlias,
                                char[] otherKeyPass)
        throws Exception
    {
        int count = 0;
        char[] keyPass = null;

        do {
            if (otherKeyPass != null) {
                MessageFormat form = new MessageFormat(rb.getString
                        ("Enter.key.password.for.alias."));
                Object[] source = {alias};
                System.err.println(form.format(source));

                form = new MessageFormat(rb.getString
                        (".RETURN.if.same.as.for.otherAlias."));
                Object[] src = {otherAlias};
                System.err.print(form.format(src));
            } else {
                MessageFormat form = new MessageFormat(rb.getString
                        ("Enter.key.password.for.alias."));
                Object[] source = {alias};
                System.err.print(form.format(source));
            }
            System.err.flush();
            keyPass = Password.readPassword(System.in);
            passwords.add(keyPass);
            if (keyPass == null) {
                keyPass = otherKeyPass;
            }
            count++;
        } while ((keyPass == null) && count < 3);

        if (keyPass == null) {
            throw new Exception(rb.getString("Too.many.failures.try.later"));
        }

        return keyPass;
    }

    private String withWeak(String alg) {
        if (DISABLED_CHECK.permits(SIG_PRIMITIVE_SET, alg, null)) {
            if (LEGACY_CHECK.permits(SIG_PRIMITIVE_SET, alg, null)) {
                return alg;
            } else {
                return String.format(rb.getString("with.weak"), alg);
            }
        } else {
            return String.format(rb.getString("with.disabled"), alg);
        }
    }

    private String fullDisplayAlgName(Key key) {
        String result = key.getAlgorithm();
        if (key instanceof ECKey) {
            ECParameterSpec paramSpec = ((ECKey) key).getParams();
            if (paramSpec instanceof NamedCurve) {
                NamedCurve nc = (NamedCurve)paramSpec;
                result += " (" + nc.getNameAndAliases()[0] + ")";
            }
        } else if (key instanceof EdECKey) {
            result = ((EdECKey) key).getParams().getName();
        }
        return result;
    }

    private String withWeak(Key key) {
        int kLen = KeyUtil.getKeySize(key);
        String displayAlg = fullDisplayAlgName(key);
        if (DISABLED_CHECK.permits(SIG_PRIMITIVE_SET, key)) {
            if (LEGACY_CHECK.permits(SIG_PRIMITIVE_SET, key)) {
                if (kLen >= 0) {
                    return String.format(rb.getString("key.bit"), kLen, displayAlg);
                } else {
                    return String.format(rb.getString("unknown.size.1"), displayAlg);
                }
            } else {
                return String.format(rb.getString("key.bit.weak"), kLen, displayAlg);
            }
        } else {
            return String.format(rb.getString("key.bit.disabled"), kLen, displayAlg);
        }
    }

    /**
     * Prints a certificate in a human readable format.
     */
    private void printX509Cert(X509Certificate cert, PrintStream out)
        throws Exception
    {

        MessageFormat form = new MessageFormat
                (rb.getString(".PATTERN.printX509Cert.with.weak"));
        PublicKey pkey = cert.getPublicKey();
        String sigName = cert.getSigAlgName();
        // No need to warn about sigalg of a trust anchor
        if (!isTrustedCert(cert)) {
            sigName = withWeak(sigName);
        }
        Object[] source = {cert.getSubjectX500Principal().toString(),
                        cert.getIssuerX500Principal().toString(),
                        cert.getSerialNumber().toString(16),
                        cert.getNotBefore().toString(),
                        cert.getNotAfter().toString(),
                        getCertFingerPrint("SHA-1", cert),
                        getCertFingerPrint("SHA-256", cert),
                        sigName,
                        withWeak(pkey),
                        cert.getVersion()
                        };
        out.println(form.format(source));

        if (cert instanceof X509CertImpl) {
            X509CertImpl impl = (X509CertImpl)cert;
            X509CertInfo certInfo = (X509CertInfo)impl.get(X509CertImpl.NAME
                                                           + "." +
                                                           X509CertImpl.INFO);
            CertificateExtensions exts = (CertificateExtensions)
                    certInfo.get(X509CertInfo.EXTENSIONS);
            if (exts != null) {
                printExtensions(rb.getString("Extensions."), exts, out);
            }
        }
    }

    private static void printExtensions(String title, CertificateExtensions exts, PrintStream out)
            throws Exception {
        int extnum = 0;
        Iterator<Extension> i1 = exts.getAllExtensions().iterator();
        Iterator<Extension> i2 = exts.getUnparseableExtensions().values().iterator();
        while (i1.hasNext() || i2.hasNext()) {
            Extension ext = i1.hasNext()?i1.next():i2.next();
            if (extnum == 0) {
                out.println();
                out.println(title);
                out.println();
            }
            out.print("#"+(++extnum)+": "+ ext);
            if (ext.getClass() == Extension.class) {
                byte[] v = ext.getExtensionValue();
                if (v.length == 0) {
                    out.println(rb.getString(".Empty.value."));
                } else {
                    new sun.security.util.HexDumpEncoder().encodeBuffer(ext.getExtensionValue(), out);
                    out.println();
                }
            }
            out.println();
        }
    }

    /**
     * Locates a signer for a given certificate from a given keystore and
     * returns the signer's certificate.
     * @param cert the certificate whose signer is searched, not null
     * @param ks the keystore to search with, not null
     * @return <code>cert</code> itself if it's already inside <code>ks</code>,
     * or a certificate inside <code>ks</code> who signs <code>cert</code>,
     * or null otherwise. A label is added.
     */
    private static Pair<String,Certificate>
            getSigner(Certificate cert, KeyStore ks) throws Exception {
        if (ks.getCertificateAlias(cert) != null) {
            return new Pair<>("", cert);
        }
        for (Enumeration<String> aliases = ks.aliases();
                aliases.hasMoreElements(); ) {
            String name = aliases.nextElement();
            Certificate trustedCert = ks.getCertificate(name);
            if (trustedCert != null) {
                try {
                    cert.verify(trustedCert.getPublicKey());
                    return new Pair<>(name, trustedCert);
                } catch (Exception e) {
                    // Not verified, skip to the next one
                }
            }
        }
        return null;
    }

    /**
     * Gets an X.500 name suitable for inclusion in a certification request.
     */
    private X500Name getX500Name() throws IOException {
        BufferedReader in;
        in = new BufferedReader(new InputStreamReader(System.in));
        String commonName = "Unknown";
        String organizationalUnit = "Unknown";
        String organization = "Unknown";
        String city = "Unknown";
        String state = "Unknown";
        String country = "Unknown";
        X500Name name;
        String userInput = null;

        int maxRetry = 20;
        do {
            if (maxRetry-- < 0) {
                throw new RuntimeException(rb.getString(
                        "Too.many.retries.program.terminated"));
            }
            commonName = inputString(in,
                    rb.getString("What.is.your.first.and.last.name."),
                    commonName);
            organizationalUnit = inputString(in,
                    rb.getString
                        ("What.is.the.name.of.your.organizational.unit."),
                    organizationalUnit);
            organization = inputString(in,
                    rb.getString("What.is.the.name.of.your.organization."),
                    organization);
            city = inputString(in,
                    rb.getString("What.is.the.name.of.your.City.or.Locality."),
                    city);
            state = inputString(in,
                    rb.getString("What.is.the.name.of.your.State.or.Province."),
                    state);
            country = inputString(in,
                    rb.getString
                        ("What.is.the.two.letter.country.code.for.this.unit."),
                    country);
            name = new X500Name(commonName, organizationalUnit, organization,
                                city, state, country);
            MessageFormat form = new MessageFormat
                (rb.getString("Is.name.correct."));
            Object[] source = {name};
            userInput = inputString
                (in, form.format(source), rb.getString("no"));
        } while (collator.compare(userInput, rb.getString("yes")) != 0 &&
                 collator.compare(userInput, rb.getString("y")) != 0);

        System.err.println();
        return name;
    }

    private String inputString(BufferedReader in, String prompt,
                               String defaultValue)
        throws IOException
    {
        System.err.println(prompt);
        MessageFormat form = new MessageFormat
                (rb.getString(".defaultValue."));
        Object[] source = {defaultValue};
        System.err.print(form.format(source));
        System.err.flush();

        String value = in.readLine();
        if (value == null || collator.compare(value, "") == 0) {
            value = defaultValue;
        }
        return value;
    }

    /**
     * Writes an X.509 certificate in base64 or binary encoding to an output
     * stream.
     */
    private void dumpCert(Certificate cert, PrintStream out)
        throws IOException, CertificateException
    {
        if (rfc) {
            out.println(X509Factory.BEGIN_CERT);
            out.println(Base64.getMimeEncoder(64, CRLF).encodeToString(cert.getEncoded()));
            out.println(X509Factory.END_CERT);
        } else {
            out.write(cert.getEncoded()); // binary
        }
    }

    /**
     * Recovers (private) key associated with given alias.
     *
     * @return an array of objects, where the 1st element in the array is the
     * recovered private key, and the 2nd element is the password used to
     * recover it.
     */
    private Pair<Key,char[]> recoverKey(String alias, char[] storePass,
                                       char[] keyPass)
        throws Exception
    {
        Key key = null;

        if (KeyStoreUtil.isWindowsKeyStore(storetype)) {
            key = keyStore.getKey(alias, null);
            return Pair.of(key, null);
        }

        if (keyStore.containsAlias(alias) == false) {
            MessageFormat form = new MessageFormat
                (rb.getString("Alias.alias.does.not.exist"));
            Object[] source = {alias};
            throw new Exception(form.format(source));
        }
        if (!keyStore.entryInstanceOf(alias, KeyStore.PrivateKeyEntry.class) &&
                !keyStore.entryInstanceOf(alias, KeyStore.SecretKeyEntry.class)) {
            MessageFormat form = new MessageFormat
                (rb.getString("Alias.alias.has.no.key"));
            Object[] source = {alias};
            throw new Exception(form.format(source));
        }

        if (keyPass == null) {
            // Try to recover the key using the keystore password
            if (storePass != null) {
                try {
                    key = keyStore.getKey(alias, storePass);
                    passwords.add(storePass);
                    return Pair.of(key, storePass);
                } catch (UnrecoverableKeyException e) {
                    if (token) {
                        throw e;
                    }
                }
            }
            // prompt user for key password
            keyPass = getKeyPasswd(alias, null, null);
            key = keyStore.getKey(alias, keyPass);
            return Pair.of(key, keyPass);
        } else {
            key = keyStore.getKey(alias, keyPass);
            return Pair.of(key, keyPass);
        }
    }

    /**
     * Recovers entry associated with given alias.
     *
     * @return an array of objects, where the 1st element in the array is the
     * recovered entry, and the 2nd element is the password used to
     * recover it (null if no password).
     */
    private Pair<Entry,char[]> recoverEntry(KeyStore ks,
                            String alias,
                            char[] pstore,
                            char[] pkey) throws Exception {

        if (!ks.containsAlias(alias)) {
            MessageFormat form = new MessageFormat(
                    rb.getString("Alias.alias.does.not.exist"));
            Object[] source = {alias};
            throw new Exception(form.format(source));
        }

        // Step 1: First attempt to access entry without key password
        // (PKCS11 entry or trusted certificate entry, for example).
        // If fail, go next.
        try {
            Entry entry = ks.getEntry(alias, null);
            return Pair.of(entry, null);
        } catch (UnrecoverableEntryException une) {
            if(P11KEYSTORE.equalsIgnoreCase(ks.getType()) ||
                    KeyStoreUtil.isWindowsKeyStore(ks.getType())) {
                // should not happen, but a possibility
                throw une;
            }
        }

        // entry is protected

        // Step 2: try pkey if not null. If fail, fail.
        if (pkey != null) {
            PasswordProtection pp = new PasswordProtection(pkey);
            Entry entry = ks.getEntry(alias, pp);
            return Pair.of(entry, pkey);
        }

        // Step 3: try pstore if not null. If fail, go next.
        if (pstore != null) {
            try {
                PasswordProtection pp = new PasswordProtection(pstore);
                Entry entry = ks.getEntry(alias, pp);
                return Pair.of(entry, pstore);
            } catch (UnrecoverableEntryException une) {
                if (P12KEYSTORE.equalsIgnoreCase(ks.getType())) {
                    // P12 keystore currently does not support separate
                    // store and entry passwords. We will not prompt for
                    // entry password.
                    throw une;
                }
            }
        }

        // Step 4: prompt for entry password
        pkey = getKeyPasswd(alias, null, null);
        PasswordProtection pp = new PasswordProtection(pkey);
        Entry entry = ks.getEntry(alias, pp);
        return Pair.of(entry, pkey);
    }

    /**
     * Gets the requested finger print of the certificate.
     */
    private String getCertFingerPrint(String mdAlg, Certificate cert)
        throws Exception
    {
        byte[] encCertInfo = cert.getEncoded();
        MessageDigest md = MessageDigest.getInstance(mdAlg);
        byte[] digest = md.digest(encCertInfo);
        return HexFormat.ofDelimiter(":").withUpperCase().formatHex(digest);
    }

    /**
     * Prints warning about missing integrity check.
     */
    private void printNoIntegrityWarning() {
        System.err.println();
        System.err.println(rb.getString
            (".WARNING.WARNING.WARNING."));
        System.err.println(rb.getString
            (".The.integrity.of.the.information.stored.in.your.keystore."));
        System.err.println(rb.getString
            (".WARNING.WARNING.WARNING."));
        System.err.println();
    }

    /**
     * Validates chain in certification reply, and returns the ordered
     * elements of the chain (with user certificate first, and root
     * certificate last in the array).
     *
     * @param alias the alias name
     * @param userCert the user certificate of the alias
     * @param replyCerts the chain provided in the reply
     */
    private Certificate[] validateReply(String alias,
                                        Certificate userCert,
                                        Certificate[] replyCerts)
        throws Exception
    {

        checkWeak(rb.getString("reply"), replyCerts);

        // order the certs in the reply (bottom-up).
        // we know that all certs in the reply are of type X.509, because
        // we parsed them using an X.509 certificate factory
        int i;
        PublicKey userPubKey = userCert.getPublicKey();

        // Remove duplicated certificates.
        HashSet<Certificate> nodup = new HashSet<>(Arrays.asList(replyCerts));
        replyCerts = nodup.toArray(new Certificate[nodup.size()]);

        for (i=0; i<replyCerts.length; i++) {
            if (userPubKey.equals(replyCerts[i].getPublicKey())) {
                break;
            }
        }
        if (i == replyCerts.length) {
            MessageFormat form = new MessageFormat(rb.getString
                ("Certificate.reply.does.not.contain.public.key.for.alias."));
            Object[] source = {alias};
            throw new Exception(form.format(source));
        }

        Certificate tmpCert = replyCerts[0];
        replyCerts[0] = replyCerts[i];
        replyCerts[i] = tmpCert;

        X509Certificate thisCert = (X509Certificate)replyCerts[0];

        for (i=1; i < replyCerts.length-1; i++) {
            // find a cert in the reply who signs thisCert
            int j;
            for (j=i; j<replyCerts.length; j++) {
                if (KeyStoreUtil.signedBy(thisCert, (X509Certificate)replyCerts[j])) {
                    tmpCert = replyCerts[i];
                    replyCerts[i] = replyCerts[j];
                    replyCerts[j] = tmpCert;
                    thisCert = (X509Certificate)replyCerts[i];
                    break;
                }
            }
            if (j == replyCerts.length) {
                throw new Exception
                    (rb.getString("Incomplete.certificate.chain.in.reply"));
            }
        }

        if (noprompt) {
            return replyCerts;
        }

        // do we trust the cert at the top?
        Certificate topCert = replyCerts[replyCerts.length-1];
        boolean fromKeyStore = true;
        Pair<String,Certificate> root = getSigner(topCert, keyStore);
        if (root == null && trustcacerts && caks != null) {
            root = getSigner(topCert, caks);
            fromKeyStore = false;
        }
        if (root == null) {
            System.err.println();
            System.err.println
                    (rb.getString("Top.level.certificate.in.reply."));
            printX509Cert((X509Certificate)topCert, System.out);
            System.err.println();
            System.err.print(rb.getString(".is.not.trusted."));
            printWeakWarnings(true);
            String reply = getYesNoReply
                    (rb.getString("Install.reply.anyway.no."));
            if ("NO".equals(reply)) {
                return null;
            }
        } else {
            if (root.snd != topCert) {
                // append the root CA cert to the chain
                Certificate[] tmpCerts =
                    new Certificate[replyCerts.length+1];
                System.arraycopy(replyCerts, 0, tmpCerts, 0,
                                 replyCerts.length);
                tmpCerts[tmpCerts.length-1] = root.snd;
                replyCerts = tmpCerts;
                checkWeak(String.format(fromKeyStore
                                ? rb.getString("alias.in.keystore")
                                : rb.getString("alias.in.cacerts"),
                                        root.fst),
                          root.snd);
            }
        }
        return replyCerts;
    }

    /**
     * Establishes a certificate chain (using trusted certificates in the
     * keystore and cacerts), starting with the reply (certToVerify)
     * and ending at a self-signed certificate found in the keystore.
     *
     * @param userCert optional existing certificate, mostly likely be the
     *                 original self-signed cert created by -genkeypair.
     *                 It must have the same public key as certToVerify
     *                 but cannot be the same cert.
     * @param certToVerify the starting certificate to build the chain
     * @returns the established chain, might be null if user decides not
     */
    private Certificate[] establishCertChain(Certificate userCert,
                                             Certificate certToVerify)
        throws Exception
    {
        if (userCert != null) {
            // Make sure that the public key of the certificate reply matches
            // the original public key in the keystore
            PublicKey origPubKey = userCert.getPublicKey();
            PublicKey replyPubKey = certToVerify.getPublicKey();
            if (!origPubKey.equals(replyPubKey)) {
                throw new Exception(rb.getString
                        ("Public.keys.in.reply.and.keystore.don.t.match"));
            }

            // If the two certs are identical, we're done: no need to import
            // anything
            if (certToVerify.equals(userCert)) {
                throw new Exception(rb.getString
                        ("Certificate.reply.and.certificate.in.keystore.are.identical"));
            }
        }

        // Build a hash table of all certificates in the keystore.
        // Use the subject distinguished name as the key into the hash table.
        // All certificates associated with the same subject distinguished
        // name are stored in the same hash table entry as a vector.
        Hashtable<Principal, Vector<Pair<String,X509Certificate>>> certs = null;
        if (keyStore.size() > 0) {
            certs = new Hashtable<>(11);
            keystorecerts2Hashtable(keyStore, certs);
        }
        if (trustcacerts) {
            if (caks!=null && caks.size()>0) {
                if (certs == null) {
                    certs = new Hashtable<>(11);
                }
                keystorecerts2Hashtable(caks, certs);
            }
        }

        // start building chain
        Vector<Pair<String,X509Certificate>> chain = new Vector<>(2);
        if (buildChain(
                new Pair<>(rb.getString("the.input"),
                           (X509Certificate) certToVerify),
                chain, certs)) {
            for (Pair<String,X509Certificate> p : chain) {
                checkWeak(p.fst, p.snd);
            }
            Certificate[] newChain =
                    new Certificate[chain.size()];
            // buildChain() returns chain with self-signed root-cert first and
            // user-cert last, so we need to invert the chain before we store
            // it
            int j=0;
            for (int i=chain.size()-1; i>=0; i--) {
                newChain[j] = chain.elementAt(i).snd;
                j++;
            }
            return newChain;
        } else {
            throw new Exception
                (rb.getString("Failed.to.establish.chain.from.reply"));
        }
    }

    /**
     * Recursively tries to establish chain from pool of certs starting from
     * certToVerify until a self-signed cert is found, and fill the certs found
     * into chain. Each cert in the chain signs the next one.
     *
     * This method is able to recover from an error, say, if certToVerify
     * is signed by certA but certA has no issuer in certs and itself is not
     * self-signed, the method can try another certB that also signs
     * certToVerify and look for signer of certB, etc, etc.
     *
     * Each cert in chain comes with a label showing its origin. The label is
     * used in the warning message when the cert is considered a risk.
     *
     * @param certToVerify the cert that needs to be verified.
     * @param chain the chain that's being built.
     * @param certs the pool of trusted certs
     *
     * @return true if successful, false otherwise.
     */
    private boolean buildChain(Pair<String,X509Certificate> certToVerify,
            Vector<Pair<String,X509Certificate>> chain,
            Hashtable<Principal, Vector<Pair<String,X509Certificate>>> certs) {
        if (KeyStoreUtil.isSelfSigned(certToVerify.snd)) {
            // reached self-signed root cert;
            // no verification needed because it's trusted.
            chain.addElement(certToVerify);
            return true;
        }

        Principal issuer = certToVerify.snd.getIssuerX500Principal();

        // Get the issuer's certificate(s)
        Vector<Pair<String,X509Certificate>> vec = certs.get(issuer);
        if (vec == null) {
            return false;
        }

        // Try out each certificate in the vector, until we find one
        // whose public key verifies the signature of the certificate
        // in question.
        for (Enumeration<Pair<String,X509Certificate>> issuerCerts = vec.elements();
                issuerCerts.hasMoreElements(); ) {
            Pair<String,X509Certificate> issuerCert = issuerCerts.nextElement();
            PublicKey issuerPubKey = issuerCert.snd.getPublicKey();
            try {
                certToVerify.snd.verify(issuerPubKey);
            } catch (Exception e) {
                continue;
            }
            if (buildChain(issuerCert, chain, certs)) {
                chain.addElement(certToVerify);
                return true;
            }
        }
        return false;
    }

    /**
     * Prompts user for yes/no decision.
     *
     * @return the user's decision, can only be "YES" or "NO"
     */
    private String getYesNoReply(String prompt)
        throws IOException
    {
        String reply = null;
        int maxRetry = 20;
        do {
            if (maxRetry-- < 0) {
                throw new RuntimeException(rb.getString(
                        "Too.many.retries.program.terminated"));
            }
            System.err.print(prompt);
            System.err.flush();
            reply = (new BufferedReader(new InputStreamReader
                                        (System.in))).readLine();
            if (reply == null ||
                collator.compare(reply, "") == 0 ||
                collator.compare(reply, rb.getString("n")) == 0 ||
                collator.compare(reply, rb.getString("no")) == 0) {
                reply = "NO";
            } else if (collator.compare(reply, rb.getString("y")) == 0 ||
                       collator.compare(reply, rb.getString("yes")) == 0) {
                reply = "YES";
            } else {
                System.err.println(rb.getString("Wrong.answer.try.again"));
                reply = null;
            }
        } while (reply == null);
        return reply;
    }

    /**
     * Stores the (leaf) certificates of a keystore in a hashtable.
     * All certs belonging to the same CA are stored in a vector that
     * in turn is stored in the hashtable, keyed by the CA's subject DN.
     * Each cert comes with a string label that shows its origin and alias.
     */
    private void keystorecerts2Hashtable(KeyStore ks,
                Hashtable<Principal, Vector<Pair<String,X509Certificate>>> hash)
        throws Exception {

        for (Enumeration<String> aliases = ks.aliases();
                                        aliases.hasMoreElements(); ) {
            String alias = aliases.nextElement();
            Certificate cert = ks.getCertificate(alias);
            if (cert != null) {
                Principal subjectDN = ((X509Certificate)cert).getSubjectX500Principal();
                Pair<String,X509Certificate> pair = new Pair<>(
                        String.format(
                                rb.getString(ks == caks ?
                                        "alias.in.cacerts" :
                                        "alias.in.keystore"),
                                alias),
                        (X509Certificate)cert);
                Vector<Pair<String,X509Certificate>> vec = hash.get(subjectDN);
                if (vec == null) {
                    vec = new Vector<>();
                    vec.addElement(pair);
                } else {
                    if (!vec.contains(pair)) {
                        vec.addElement(pair);
                    }
                }
                hash.put(subjectDN, vec);
            }
        }
    }

    /**
     * Returns the issue time that's specified the -startdate option
     * @param s the value of -startdate option
     */
    private static Date getStartDate(String s) throws IOException {
        Calendar c = new GregorianCalendar();
        if (s != null) {
            IOException ioe = new IOException(
                    rb.getString("Illegal.startdate.value"));
            int len = s.length();
            if (len == 0) {
                throw ioe;
            }
            if (s.charAt(0) == '-' || s.charAt(0) == '+') {
                // Form 1: ([+-]nnn[ymdHMS])+
                int start = 0;
                while (start < len) {
                    int sign = 0;
                    switch (s.charAt(start)) {
                        case '+': sign = 1; break;
                        case '-': sign = -1; break;
                        default: throw ioe;
                    }
                    int i = start+1;
                    for (; i<len; i++) {
                        char ch = s.charAt(i);
                        if (ch < '0' || ch > '9') break;
                    }
                    if (i == start+1) throw ioe;
                    int number = Integer.parseInt(s.substring(start+1, i));
                    if (i >= len) throw ioe;
                    int unit = 0;
                    switch (s.charAt(i)) {
                        case 'y': unit = Calendar.YEAR; break;
                        case 'm': unit = Calendar.MONTH; break;
                        case 'd': unit = Calendar.DATE; break;
                        case 'H': unit = Calendar.HOUR; break;
                        case 'M': unit = Calendar.MINUTE; break;
                        case 'S': unit = Calendar.SECOND; break;
                        default: throw ioe;
                    }
                    c.add(unit, sign * number);
                    start = i + 1;
                }
            } else  {
                // Form 2: [yyyy/mm/dd] [HH:MM:SS]
                String date = null, time = null;
                if (len == 19) {
                    date = s.substring(0, 10);
                    time = s.substring(11);
                    if (s.charAt(10) != ' ')
                        throw ioe;
                } else if (len == 10) {
                    date = s;
                } else if (len == 8) {
                    time = s;
                } else {
                    throw ioe;
                }
                if (date != null) {
                    if (date.matches("\\d\\d\\d\\d\\/\\d\\d\\/\\d\\d")) {
                        c.set(Integer.valueOf(date.substring(0, 4)),
                                Integer.valueOf(date.substring(5, 7))-1,
                                Integer.valueOf(date.substring(8, 10)));
                    } else {
                        throw ioe;
                    }
                }
                if (time != null) {
                    if (time.matches("\\d\\d:\\d\\d:\\d\\d")) {
                        c.set(Calendar.HOUR_OF_DAY, Integer.valueOf(time.substring(0, 2)));
                        c.set(Calendar.MINUTE, Integer.valueOf(time.substring(3, 5)));
                        c.set(Calendar.SECOND, Integer.valueOf(time.substring(6, 8)));
                        c.set(Calendar.MILLISECOND, 0);
                    } else {
                        throw ioe;
                    }
                }
            }
        }
        return c.getTime();
    }

    /**
     * Match a command with a command set. The match can be exact, or
     * partial, or case-insensitive.
     *
     * @param s the command provided by user
     * @param list the legal command set represented by KnownOIDs enums.
     * @return the position of a single match, or -1 if none matched
     * @throws Exception if s is ambiguous
     */
    private static int oneOf(String s, KnownOIDs... list) throws Exception {
        String[] convertedList = new String[list.length];
        for (int i = 0; i < list.length; i++) {
            convertedList[i] = list[i].stdName();
        }
        return oneOf(s, convertedList);
    }

    /**
     * Match a command with a command set. The match can be exact, or
     * partial, or case-insensitive.
     *
     * @param s the command provided by user
     * @param list the legal command set. If there is a null, commands after it
     *      are regarded experimental, which means they are supported but their
     *      existence should not be revealed to user.
     * @return the position of a single match, or -1 if none matched
     * @throws Exception if s is ambiguous
     */
    private static int oneOf(String s, String... list) throws Exception {

        // First, if there is an exact match, returns it.
        int res = oneOfMatch((a,b) -> a.equals(b), s, list);
        if (res >= 0) {
            return res;
        }

        // Second, if there is one single camelCase or prefix match, returns it.
        // This regex substitution removes all lowercase letters not at the
        // beginning, so "keyCertSign" becomes "kCS".
        res = oneOfMatch((a,b) -> a.equals(b.replaceAll("(?<!^)[a-z]", ""))
                || b.startsWith(a), s, list);
        if (res >= 0) {
            return res;
        }

        // Finally, retry the 2nd step ignoring case
        return oneOfMatch((a,b) -> a.equalsIgnoreCase(b.replaceAll("(?<!^)[a-z]", ""))
                || b.toUpperCase(Locale.ROOT).startsWith(a.toUpperCase(Locale.ROOT)),
                s, list);
    }

    /**
     * Match a command with a command set.
     *
     * @param matcher a BiFunction which returns {@code true} if the 1st
     *               argument (user input) matches the 2nd one (full command)
     * @param s the command provided by user
     * @param list the legal command set
     * @return the position of a single match, or -1 if none matched
     * @throws Exception if s is ambiguous
     */
    private static int oneOfMatch(BiFunction<String,String,Boolean> matcher,
            String s, String... list) throws Exception {
        int[] match = new int[list.length];
        int nmatch = 0;
        int experiment = Integer.MAX_VALUE;
        for (int i = 0; i<list.length; i++) {
            String one = list[i];
            if (one == null) {
                experiment = i;
                continue;
            }
            if (matcher.apply(s, one)) {
                match[nmatch++] = i;
            }
        }
        if (nmatch == 0) {
            return -1;
        } else if (nmatch == 1) {
            return match[0];
        } else {
            // If multiple matches is in experimental commands, ignore them
            if (match[1] > experiment) {
                return match[0];
            }
            StringBuilder sb = new StringBuilder();
            MessageFormat form = new MessageFormat(rb.getString
                    ("command.{0}.is.ambiguous."));
            Object[] source = {s};
            sb.append(form.format(source));
            sb.append("\n    ");
            for (int i=0; i<nmatch && match[i]<experiment; i++) {
                sb.append(' ');
                sb.append(list[match[i]]);
            }
            throw new Exception(sb.toString());
        }
    }

    /**
     * Create a GeneralName object from known types
     * @param t one of 5 known types
     * @param v value
     * @param exttype X.509 extension type
     * @return which one
     */
    private GeneralName createGeneralName(String t, String v, int exttype)
            throws Exception {
        GeneralNameInterface gn;
        int p = oneOf(t, "EMAIL", "URI", "DNS", "IP", "OID");
        if (p < 0) {
            throw new Exception(rb.getString(
                    "Unrecognized.GeneralName.type.") + t);
        }
        switch (p) {
            case 0: gn = new RFC822Name(v); break;
            case 1: gn = new URIName(v); break;
            case 2:
                if (exttype == 3) {
                    // Allow wildcard only for SAN extension
                    gn = new DNSName(v, true);
                } else {
                    gn = new DNSName(v);
                }
                break;
            case 3: gn = new IPAddressName(v); break;
            default: gn = new OIDName(v); break; //4
        }
        return new GeneralName(gn);
    }

    private static final String[] extSupported = {
                        "BasicConstraints",
                        "KeyUsage",
                        "ExtendedKeyUsage",
                        "SubjectAlternativeName",
                        "IssuerAlternativeName",
                        "SubjectInfoAccess",
                        "AuthorityInfoAccess",
                        null,
                        "CRLDistributionPoints",
    };

    private ObjectIdentifier findOidForExtName(String type)
            throws Exception {
        switch (oneOf(type, extSupported)) {
            case 0: return PKIXExtensions.BasicConstraints_Id;
            case 1: return PKIXExtensions.KeyUsage_Id;
            case 2: return PKIXExtensions.ExtendedKeyUsage_Id;
            case 3: return PKIXExtensions.SubjectAlternativeName_Id;
            case 4: return PKIXExtensions.IssuerAlternativeName_Id;
            case 5: return PKIXExtensions.SubjectInfoAccess_Id;
            case 6: return PKIXExtensions.AuthInfoAccess_Id;
            case 8: return PKIXExtensions.CRLDistributionPoints_Id;
            default: return ObjectIdentifier.of(type);
        }
    }

    // Add an extension into a CertificateExtensions, always using OID as key
    private static void setExt(CertificateExtensions result, Extension ex)
            throws IOException {
        result.set(ex.getId(), ex);
    }

    /**
     * Create X509v3 extensions from a string representation. Note that the
     * SubjectKeyIdentifierExtension will always be created non-critical besides
     * the extension requested in the <code>extstr</code> argument.
     *
     * @param requestedEx the requested extensions, can be null, used for -gencert
     * @param existingEx the original extensions, can be null, used for -selfcert
     * @param extstrs -ext values, Read keytool doc
     * @param pkey the public key for the certificate
     * @param aSubjectKeyId the subject key identifier for the authority (issuer)
     * @return the created CertificateExtensions
     */
    private CertificateExtensions createV3Extensions(
            CertificateExtensions requestedEx,
            CertificateExtensions existingEx,
            List <String> extstrs,
            PublicKey pkey,
            KeyIdentifier aSubjectKeyId) throws Exception {

        // By design, inside a CertificateExtensions object, all known
        // extensions uses name (say, "BasicConstraints") as key and
        // a child Extension type (say, "BasicConstraintsExtension")
        // as value, unknown extensions uses OID as key and bare
        // Extension object as value. This works fine inside JDK.
        //
        // However, in keytool, there is no way to prevent people
        // using OID in -ext, either as a new extension, or in a
        // honored value. Thus here we (ab)use CertificateExtensions
        // by always using OID as key and value can be of any type.

        if (existingEx != null && requestedEx != null) {
            // This should not happen
            throw new Exception("One of request and original should be null.");
        }
        // A new extensions always using OID as key
        CertificateExtensions result = new CertificateExtensions();
        if (existingEx != null) {
            for (Extension ex: existingEx.getAllExtensions()) {
                setExt(result, ex);
            }
        }
        try {
            // always non-critical
            setExt(result, new SubjectKeyIdentifierExtension(
                    new KeyIdentifier(pkey).getIdentifier()));
            if (aSubjectKeyId != null) {
                setExt(result, new AuthorityKeyIdentifierExtension(aSubjectKeyId,
                        null, null));
            }

            // name{:critical}{=value}
            // Honoring requested extensions
            if (requestedEx != null) {
                // The existing requestedEx might use names as keys,
                // translate to all-OID first.
                CertificateExtensions request2 = new CertificateExtensions();
                for (sun.security.x509.Extension ex: requestedEx.getAllExtensions()) {
                    request2.set(ex.getId(), ex);
                }
                for(String extstr: extstrs) {
                    if (extstr.toLowerCase(Locale.ENGLISH).startsWith("honored=")) {
                        List<String> list = Arrays.asList(
                                extstr.toLowerCase(Locale.ENGLISH).substring(8).split(","));
                        // First check existence of "all"
                        if (list.contains("all")) {
                            for (Extension ex: request2.getAllExtensions()) {
                                setExt(result, ex);
                            }
                        }
                        // one by one for others
                        for (String item: list) {
                            if (item.equals("all")) continue;

                            // add or remove
                            boolean add;
                            // -1, unchanged, 0 critical, 1 non-critical
                            int action = -1;
                            String type = null;
                            if (item.startsWith("-")) {
                                add = false;
                                type = item.substring(1);
                            } else {
                                add = true;
                                int colonpos = item.indexOf(':');
                                if (colonpos >= 0) {
                                    type = item.substring(0, colonpos);
                                    action = oneOf(item.substring(colonpos+1),
                                            "critical", "non-critical");
                                    if (action == -1) {
                                        throw new Exception(rb.getString
                                            ("Illegal.value.") + item);
                                    }
                                } else {
                                    type = item;
                                }
                            }
                            String n = findOidForExtName(type).toString();
                            if (add) {
                                Extension e = request2.get(n);
                                if (!e.isCritical() && action == 0
                                        || e.isCritical() && action == 1) {
                                    e = Extension.newExtension(
                                            e.getExtensionId(),
                                            !e.isCritical(),
                                            e.getExtensionValue());
                                }
                                setExt(result, e);
                            } else {
                                result.delete(n);
                            }
                        }
                        break;
                    }
                }
            }
            for(String extstr: extstrs) {
                String name, value;
                boolean isCritical = false;

                int eqpos = extstr.indexOf('=');
                if (eqpos >= 0) {
                    name = extstr.substring(0, eqpos);
                    value = extstr.substring(eqpos+1);
                } else {
                    name = extstr;
                    value = null;
                }

                int colonpos = name.indexOf(':');
                if (colonpos >= 0) {
                    if (oneOf(name.substring(colonpos+1), "critical") == 0) {
                        isCritical = true;
                    }
                    name = name.substring(0, colonpos);
                }

                if (name.equalsIgnoreCase("honored")) {
                    continue;
                }
                int exttype = oneOf(name, extSupported);
                switch (exttype) {
                    case 0:     // BC
                        int pathLen = -1;
                        boolean isCA = false;
                        if (value == null) {
                            isCA = true;
                        } else {
                            try {   // the abbr format
                                pathLen = Integer.parseInt(value);
                                isCA = true;
                            } catch (NumberFormatException ufe) {
                                // ca:true,pathlen:1
                                for (String part: value.split(",")) {
                                    String[] nv = part.split(":");
                                    if (nv.length != 2) {
                                        throw new Exception(rb.getString
                                                ("Illegal.value.") + extstr);
                                    } else {
                                        if (nv[0].equalsIgnoreCase("ca")) {
                                            isCA = Boolean.parseBoolean(nv[1]);
                                        } else if (nv[0].equalsIgnoreCase("pathlen")) {
                                            pathLen = Integer.parseInt(nv[1]);
                                        } else {
                                            throw new Exception(rb.getString
                                                ("Illegal.value.") + extstr);
                                        }
                                    }
                                }
                            }
                        }
                        setExt(result, new BasicConstraintsExtension(isCritical, isCA,
                                pathLen));
                        break;
                    case 1:     // KU
                        if(value != null) {
                            boolean[] ok = new boolean[9];
                            for (String s: value.split(",")) {
                                int p = oneOf(s,
                                       "digitalSignature",  // (0),
                                       "nonRepudiation",    // (1)
                                       "keyEncipherment",   // (2),
                                       "dataEncipherment",  // (3),
                                       "keyAgreement",      // (4),
                                       "keyCertSign",       // (5),
                                       "cRLSign",           // (6),
                                       "encipherOnly",      // (7),
                                       "decipherOnly",      // (8)
                                       "contentCommitment"  // also (1)
                                       );
                                if (p < 0) {
                                    throw new Exception(rb.getString("Unknown.keyUsage.type.") + s);
                                }
                                if (p == 9) p = 1;
                                ok[p] = true;
                            }
                            KeyUsageExtension kue = new KeyUsageExtension(ok);
                            // The above KeyUsageExtension constructor does not
                            // allow isCritical value, so...
                            setExt(result, Extension.newExtension(
                                    kue.getExtensionId(),
                                    isCritical,
                                    kue.getExtensionValue()));
                        } else {
                            throw new Exception(rb.getString
                                    ("Illegal.value.") + extstr);
                        }
                        break;
                    case 2:     // EKU
                        if(value != null) {
                            Vector<ObjectIdentifier> v = new Vector<>();
                            KnownOIDs[] choices = {
                                    KnownOIDs.anyExtendedKeyUsage,
                                    KnownOIDs.serverAuth,
                                    KnownOIDs.clientAuth,
                                    KnownOIDs.codeSigning,
                                    KnownOIDs.emailProtection,
                                    KnownOIDs.KP_TimeStamping,
                                    KnownOIDs.OCSPSigning
                            };
                            for (String s: value.split(",")) {
                                int p = oneOf(s, choices);
                                String o = s;
                                if (p >= 0) {
                                    o = choices[p].value();
                                }
                                try {
                                    v.add(ObjectIdentifier.of(o));
                                } catch (Exception e) {
                                    throw new Exception(rb.getString(
                                            "Unknown.extendedkeyUsage.type.") + s);
                                }
                            }
                            setExt(result, new ExtendedKeyUsageExtension(isCritical, v));
                        } else {
                            throw new Exception(rb.getString
                                    ("Illegal.value.") + extstr);
                        }
                        break;
                    case 3:     // SAN
                    case 4:     // IAN
                        if(value != null) {
                            String[] ps = value.split(",");
                            GeneralNames gnames = new GeneralNames();
                            for(String item: ps) {
                                colonpos = item.indexOf(':');
                                if (colonpos < 0) {
                                    throw new Exception("Illegal item " + item + " in " + extstr);
                                }
                                String t = item.substring(0, colonpos);
                                String v = item.substring(colonpos+1);
                                gnames.add(createGeneralName(t, v, exttype));
                            }
                            if (exttype == 3) {
                                setExt(result, new SubjectAlternativeNameExtension(
                                        isCritical, gnames));
                            } else {
                                setExt(result, new IssuerAlternativeNameExtension(
                                        isCritical, gnames));
                            }
                        } else {
                            throw new Exception(rb.getString
                                    ("Illegal.value.") + extstr);
                        }
                        break;
                    case 5:     // SIA, always non-critical
                    case 6:     // AIA, always non-critical
                        if (isCritical) {
                            throw new Exception(rb.getString(
                                    "This.extension.cannot.be.marked.as.critical.") + extstr);
                        }
                        if(value != null) {
                            List<AccessDescription> accessDescriptions =
                                    new ArrayList<>();
                            String[] ps = value.split(",");
                            for(String item: ps) {
                                colonpos = item.indexOf(':');
                                int colonpos2 = item.indexOf(':', colonpos+1);
                                if (colonpos < 0 || colonpos2 < 0) {
                                    throw new Exception(rb.getString
                                            ("Illegal.value.") + extstr);
                                }
                                String m = item.substring(0, colonpos);
                                String t = item.substring(colonpos+1, colonpos2);
                                String v = item.substring(colonpos2+1);
                                KnownOIDs[] choices = {
                                    KnownOIDs.OCSP,
                                    KnownOIDs.caIssuers,
                                    KnownOIDs.AD_TimeStamping,
                                    KnownOIDs.caRepository
                                };
                                int p = oneOf(m, choices);
                                ObjectIdentifier oid;
                                if (p >= 0) {
                                    oid = ObjectIdentifier.of(choices[p]);
                                } else {
                                    try {
                                        oid = ObjectIdentifier.of(m);
                                    } catch (Exception e) {
                                        throw new Exception(rb.getString(
                                                "Unknown.AccessDescription.type.") + m);
                                    }
                                }
                                accessDescriptions.add(new AccessDescription(
                                        oid, createGeneralName(t, v, exttype)));
                            }
                            if (exttype == 5) {
                                setExt(result, new SubjectInfoAccessExtension(accessDescriptions));
                            } else {
                                setExt(result, new AuthorityInfoAccessExtension(accessDescriptions));
                            }
                        } else {
                            throw new Exception(rb.getString
                                    ("Illegal.value.") + extstr);
                        }
                        break;
                    case 8: // CRL, experimental, only support 1 distributionpoint
                        if(value != null) {
                            String[] ps = value.split(",");
                            GeneralNames gnames = new GeneralNames();
                            for(String item: ps) {
                                colonpos = item.indexOf(':');
                                if (colonpos < 0) {
                                    throw new Exception("Illegal item " + item + " in " + extstr);
                                }
                                String t = item.substring(0, colonpos);
                                String v = item.substring(colonpos+1);
                                gnames.add(createGeneralName(t, v, exttype));
                            }
                            setExt(result, new CRLDistributionPointsExtension(
                                    isCritical, Collections.singletonList(
                                    new DistributionPoint(gnames, null, null))));
                        } else {
                            throw new Exception(rb.getString
                                    ("Illegal.value.") + extstr);
                        }
                        break;
                    case -1:
                        ObjectIdentifier oid = ObjectIdentifier.of(name);
                        byte[] data = null;
                        if (value != null) {
                            data = new byte[value.length() / 2 + 1];
                            int pos = 0;
                            for (char c: value.toCharArray()) {
                                if (!HexFormat.isHexDigit(c)) {
                                    continue;
                                }
                                int hex = HexFormat.fromHexDigit(c);
                                if (pos % 2 == 0) {
                                    data[pos/2] = (byte)(hex << 4);
                                } else {
                                    data[pos/2] += hex;
                                }
                                pos++;
                            }
                            if (pos % 2 != 0) {
                                throw new Exception(rb.getString(
                                        "Odd.number.of.hex.digits.found.") + extstr);
                            }
                            data = Arrays.copyOf(data, pos/2);
                        } else {
                            data = new byte[0];
                        }
                        setExt(result, new Extension(oid, isCritical,
                                new DerValue(DerValue.tag_OctetString, data)
                                        .toByteArray()));
                        break;
                    default:
                        throw new Exception(rb.getString(
                                "Unknown.extension.type.") + extstr);
                }
            }
        } catch(IOException e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    private boolean isTrustedCert(Certificate cert) throws KeyStoreException {
        if (caks != null && caks.getCertificateAlias(cert) != null) {
            return true;
        } else {
            String inKS = keyStore.getCertificateAlias(cert);
            return inKS != null && keyStore.isCertificateEntry(inKS);
        }
    }

    private void checkWeak(String label, String sigAlg, Key key) {
        if (sigAlg != null) {
            if (!DISABLED_CHECK.permits(SIG_PRIMITIVE_SET, sigAlg, null)) {
                weakWarnings.add(String.format(
                    rb.getString("whose.sigalg.disabled"), label, sigAlg));
            } else if (!LEGACY_CHECK.permits(SIG_PRIMITIVE_SET, sigAlg, null)) {
                weakWarnings.add(String.format(
                    rb.getString("whose.sigalg.weak"), label, sigAlg));
            }
        }

        if (key != null) {
            if (!DISABLED_CHECK.permits(SIG_PRIMITIVE_SET, key)) {
                weakWarnings.add(String.format(
                    rb.getString("whose.key.disabled"), label,
                    String.format(rb.getString("key.bit"),
                    KeyUtil.getKeySize(key), fullDisplayAlgName(key))));
            } else if (!LEGACY_CHECK.permits(SIG_PRIMITIVE_SET, key)) {
                weakWarnings.add(String.format(
                    rb.getString("whose.key.weak"), label,
                    String.format(rb.getString("key.bit"),
                    KeyUtil.getKeySize(key), fullDisplayAlgName(key))));
            }
        }
    }

    private void checkWeak(String label, Certificate[] certs)
            throws KeyStoreException {
        for (int i = 0; i < certs.length; i++) {
            Certificate cert = certs[i];
            if (cert instanceof X509Certificate) {
                X509Certificate xc = (X509Certificate)cert;
                String fullLabel = label;
                if (certs.length > 1) {
                    fullLabel = oneInMany(label, i, certs.length);
                }
                checkWeak(fullLabel, xc);
            }
        }
    }

    private void checkWeak(String label, Certificate cert)
            throws KeyStoreException {
        if (cert instanceof X509Certificate) {
            X509Certificate xc = (X509Certificate)cert;
            // No need to check the sigalg of a trust anchor
            String sigAlg = isTrustedCert(cert) ? null : xc.getSigAlgName();
            checkWeak(label, sigAlg, xc.getPublicKey());
        }
    }

    private void checkWeak(String label, PKCS10 p10) {
        checkWeak(label, p10.getSigAlg(), p10.getSubjectPublicKeyInfo());
    }

    private void checkWeak(String label, CRL crl, Key key) {
        if (crl instanceof X509CRLImpl) {
            X509CRLImpl impl = (X509CRLImpl)crl;
            checkWeak(label, impl.getSigAlgName(), key);
        }
    }

    private void printWeakWarnings(boolean newLine) {
        if (!weakWarnings.isEmpty() && !nowarn) {
            System.err.println("\nWarning:");
            for (String warning : weakWarnings) {
                System.err.println(warning);
            }
            if (newLine) {
                // When calling before a yes/no prompt, add a new line
                System.err.println();
            }
        }
        weakWarnings.clear();
    }

    /**
     * Prints the usage of this tool.
     */
    private void usage() {
        if (command != null) {
            System.err.println("keytool " + command +
                    rb.getString(".OPTION."));
            System.err.println();
            System.err.println(rb.getString(command.description));
            System.err.println();
            System.err.println(rb.getString("Options."));
            System.err.println();

            // Left and right sides of the options list. Both might
            // contain "\n" and span multiple lines
            String[] left = new String[command.options.length];
            String[] right = new String[command.options.length];

            // Length of left side of options list
            int lenLeft = 0;

            for (int j = 0; j < command.options.length; j++) {
                Option opt = command.options[j];
                left[j] = opt.toString();
                if (opt.arg != null) {
                    left[j] += " " + opt.arg;
                }
                String[] lefts = left[j].split("\n");
                for (String s : lefts) {
                    if (s.length() > lenLeft) {
                        lenLeft = s.length();
                    }
                }
                right[j] = rb.getString(opt.description);
            }
            for (int j = 0; j < left.length; j++) {
                String[] lefts = left[j].split("\n");
                String[] rights = right[j].split("\n");
                for (int i = 0; i < lefts.length && i < rights.length; i++) {
                    String s1 = i < lefts.length ? lefts[i] : "";
                    String s2 = i < rights.length ? rights[i] : "";
                    if (i == 0) {
                        System.err.printf(" %-" + lenLeft + "s  %s\n", s1, s2);
                    } else {
                        System.err.printf("   %-" + lenLeft + "s  %s\n", s1, s2);
                    }
                }
            }
            System.err.println();
            System.err.println(rb.getString(
                    "Use.keytool.help.for.all.available.commands"));
        } else {
            System.err.println(rb.getString(
                    "Key.and.Certificate.Management.Tool"));
            System.err.println();
            System.err.println(rb.getString("Commands."));
            System.err.println();
            for (Command c: Command.values()) {
                if (c == KEYCLONE) break;
                System.err.printf(" %-20s%s\n", c, rb.getString(c.description));
            }
            System.err.println();
            System.err.println(rb.getString(
                    "Use.keytool.help.for.all.available.commands"));
            System.err.println(rb.getString(
                    "Use.keytool.command.name.help.for.usage.of.command.name"));
        }
    }

    private void tinyHelp() {
        usage();
        if (debug) {
            throw new RuntimeException("NO BIG ERROR, SORRY");
        } else {
            System.exit(1);
        }
    }

    private void errorNeedArgument(String flag) {
        Object[] source = {flag};
        System.err.println(new MessageFormat(
                rb.getString("Command.option.flag.needs.an.argument.")).format(source));
        tinyHelp();
    }

    private char[] getPass(String modifier, String arg) {
        char[] output =
            KeyStoreUtil.getPassWithModifier(modifier, arg, rb, collator);
        if (output != null) return output;
        tinyHelp();
        return null;    // Useless, tinyHelp() already exits.
    }
}

// This class is exactly the same as com.sun.tools.javac.util.Pair,
// it's copied here since the original one is not included in JRE.
class Pair<A, B> {

    public final A fst;
    public final B snd;

    public Pair(A fst, B snd) {
        this.fst = fst;
        this.snd = snd;
    }

    public String toString() {
        return "Pair[" + fst + "," + snd + "]";
    }

    public boolean equals(Object other) {
        return
            other instanceof Pair &&
            Objects.equals(fst, ((Pair)other).fst) &&
            Objects.equals(snd, ((Pair)other).snd);
    }

    public int hashCode() {
        if (fst == null) return (snd == null) ? 0 : snd.hashCode() + 1;
        else if (snd == null) return fst.hashCode() + 2;
        else return fst.hashCode() * 17 + snd.hashCode();
    }

    public static <A,B> Pair<A,B> of(A a, B b) {
        return new Pair<>(a,b);
    }
}

