/*
 * Copyright (c) 2003, 2021, Oracle and/or its affiliates. All rights reserved.
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

package sun.security.pkcs11;

import java.io.*;
import static java.io.StreamTokenizer.*;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.*;

import java.security.*;

import sun.security.util.PropertyExpander;

import sun.security.pkcs11.wrapper.*;
import static sun.security.pkcs11.wrapper.PKCS11Constants.*;
import static sun.security.pkcs11.wrapper.CK_ATTRIBUTE.*;

import static sun.security.pkcs11.TemplateManager.*;

/**
 * Configuration container and file parsing.
 *
 * @author  Andreas Sterbenz
 * @since   1.5
 */
final class Config {

    static final int ERR_HALT       = 1;
    static final int ERR_IGNORE_ALL = 2;
    static final int ERR_IGNORE_LIB = 3;

    // same as allowSingleThreadedModules but controlled via a system property
    // and applied to all providers. if set to false, no SunPKCS11 instances
    // will accept single threaded modules regardless of the setting in their
    // config files.
    private static final boolean staticAllowSingleThreadedModules;
    private static final String osName;
    private static final String osArch;

    static {
        @SuppressWarnings("removal")
        List<String> props = AccessController.doPrivileged(
            new PrivilegedAction<>() {
                @Override
                public List<String> run() {
                    return List.of(
                        System.getProperty(
                            "sun.security.pkcs11.allowSingleThreadedModules",
                            "true"),
                        System.getProperty("os.name"),
                        System.getProperty("os.arch"));
                }
            }
        );
        if ("false".equalsIgnoreCase(props.get(0))) {
            staticAllowSingleThreadedModules = false;
        } else {
            staticAllowSingleThreadedModules = true;
        }
        osName = props.get(1);
        osArch = props.get(2);
    }

    private static final boolean DEBUG = false;

    private static void debug(Object o) {
        if (DEBUG) {
            System.out.println(o);
        }
    }

    // file name containing this configuration
    private String filename;

    // Reader and StringTokenizer used during parsing
    private Reader reader;

    private StreamTokenizer st;

    private Set<String> parsedKeywords;

    // name suffix of the provider
    private String name;

    // name of the PKCS#11 library
    private String library;

    // description to pass to the provider class
    private String description;

    // slotID of the slot to use
    private int slotID = -1;

    // slot to use, specified as index in the slotlist
    private int slotListIndex = -1;

    // set of enabled mechanisms (or null to use default)
    private Set<Long> enabledMechanisms;

    // set of disabled mechanisms
    private Set<Long> disabledMechanisms;

    // whether to print debug info during startup
    private boolean showInfo = false;

    // template manager, initialized from parsed attributes
    private TemplateManager templateManager;

    // how to handle error during startup, one of ERR_
    private int handleStartupErrors = ERR_HALT;

    // flag indicating whether the P11KeyStore should
    // be more tolerant of input parameters
    private boolean keyStoreCompatibilityMode = true;

    // flag indicating whether we need to explicitly cancel operations
    // see Token
    private boolean explicitCancel = true;

    // how often to test for token insertion, if no token is present
    private int insertionCheckInterval = 2000;

    // short ms value to indicate how often native cleaner thread is called
    private int resourceCleanerShortInterval = 2_000;
    // long ms value to indicate how often native cleaner thread is called
    private int resourceCleanerLongInterval = 60_000;

    // should Token be destroyed after logout()
    private boolean destroyTokenAfterLogout;

    // flag indicating whether to omit the call to C_Initialize()
    // should be used only if we are running within a process that
    // has already called it (e.g. Plugin inside of Mozilla/NSS)
    private boolean omitInitialize = false;

    // whether to allow modules that only support single threaded access.
    // they cannot be used safely from multiple PKCS#11 consumers in the
    // same process, for example NSS and SunPKCS11
    private boolean allowSingleThreadedModules = true;

    // name of the C function that returns the PKCS#11 functionlist
    // This option primarily exists for the deprecated
    // Secmod.Module.getProvider() method.
    private String functionList = "C_GetFunctionList";

    // whether to use NSS secmod mode. Implicitly set if nssLibraryDirectory,
    // nssSecmodDirectory, or nssModule is specified.
    private boolean nssUseSecmod;

    // location of the NSS library files (libnss3.so, etc.)
    private String nssLibraryDirectory;

    // location of secmod.db
    private String nssSecmodDirectory;

    // which NSS module to use
    private String nssModule;

    private Secmod.DbMode nssDbMode = Secmod.DbMode.READ_WRITE;

    // Whether the P11KeyStore should specify the CKA_NETSCAPE_DB attribute
    // when creating private keys. Only valid if nssUseSecmod is true.
    private boolean nssNetscapeDbWorkaround = true;

    // Special init argument string for the NSS softtoken.
    // This is used when using the NSS softtoken directly without secmod mode.
    private String nssArgs;

    // whether to use NSS trust attributes for the KeyStore of this provider
    // this option is for internal use by the SunPKCS11 code only and
    // works only for NSS providers created via the Secmod API
    private boolean nssUseSecmodTrust = false;

    // Flag to indicate whether the X9.63 encoding for EC points shall be used
    // (true) or whether that encoding shall be wrapped in an ASN.1 OctetString
    // (false).
    private boolean useEcX963Encoding = false;

    // Flag to indicate whether NSS should favour performance (false) or
    // memory footprint (true).
    private boolean nssOptimizeSpace = false;

    Config(String fn) throws IOException {
        this.filename = fn;
        if (filename.startsWith("--")) {
            // inline config
            String config = filename.substring(2).replace("\\n", "\n");
            reader = new StringReader(config);
        } else {
            reader = new BufferedReader(new InputStreamReader
                (new FileInputStream(expand(filename)),
                    StandardCharsets.ISO_8859_1));
        }
        parsedKeywords = new HashSet<String>();
        st = new StreamTokenizer(reader);
        setupTokenizer();
        parse();
    }

    String getFileName() {
        return filename;
    }

    String getName() {
        return name;
    }

    String getLibrary() {
        return library;
    }

    String getDescription() {
        if (description != null) {
            return description;
        }
        return "SunPKCS11-" + name + " using library " + library;
    }

    int getSlotID() {
        return slotID;
    }

    int getSlotListIndex() {
        if ((slotID == -1) && (slotListIndex == -1)) {
            // if neither is set, default to first slot
            return 0;
        } else {
            return slotListIndex;
        }
    }

    boolean getShowInfo() {
        return (SunPKCS11.debug != null) || showInfo;
    }

    TemplateManager getTemplateManager() {
        if (templateManager == null) {
            templateManager = new TemplateManager();
        }
        return templateManager;
    }

    boolean isEnabled(long m) {
        if (enabledMechanisms != null) {
            return enabledMechanisms.contains(Long.valueOf(m));
        }
        if (disabledMechanisms != null) {
            return !disabledMechanisms.contains(Long.valueOf(m));
        }
        return true;
    }

    int getHandleStartupErrors() {
        return handleStartupErrors;
    }

    boolean getKeyStoreCompatibilityMode() {
        return keyStoreCompatibilityMode;
    }

    boolean getExplicitCancel() {
        return explicitCancel;
    }

    boolean getDestroyTokenAfterLogout() {
        return destroyTokenAfterLogout;
    }

    int getResourceCleanerShortInterval() {
        return resourceCleanerShortInterval;
    }

    int getResourceCleanerLongInterval() {
        return resourceCleanerLongInterval;
    }

    int getInsertionCheckInterval() {
        return insertionCheckInterval;
    }

    boolean getOmitInitialize() {
        return omitInitialize;
    }

    boolean getAllowSingleThreadedModules() {
        return staticAllowSingleThreadedModules && allowSingleThreadedModules;
    }

    String getFunctionList() {
        return functionList;
    }

    boolean getNssUseSecmod() {
        return nssUseSecmod;
    }

    String getNssLibraryDirectory() {
        return nssLibraryDirectory;
    }

    String getNssSecmodDirectory() {
        return nssSecmodDirectory;
    }

    String getNssModule() {
        return nssModule;
    }

    Secmod.DbMode getNssDbMode() {
        return nssDbMode;
    }

    public boolean getNssNetscapeDbWorkaround() {
        return nssUseSecmod && nssNetscapeDbWorkaround;
    }

    String getNssArgs() {
        return nssArgs;
    }

    boolean getNssUseSecmodTrust() {
        return nssUseSecmodTrust;
    }

    boolean getUseEcX963Encoding() {
        return useEcX963Encoding;
    }

    boolean getNssOptimizeSpace() {
        return nssOptimizeSpace;
    }

    private static String expand(final String s) throws IOException {
        try {
            return PropertyExpander.expand(s);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    private void setupTokenizer() {
        st.resetSyntax();
        st.wordChars('a', 'z');
        st.wordChars('A', 'Z');
        st.wordChars('0', '9');
        st.wordChars(':', ':');
        st.wordChars('.', '.');
        st.wordChars('_', '_');
        st.wordChars('-', '-');
        st.wordChars('/', '/');
        st.wordChars('\\', '\\');
        st.wordChars('$', '$');
        st.wordChars('{', '{'); // need {} for property subst
        st.wordChars('}', '}');
        st.wordChars('*', '*');
        st.wordChars('+', '+');
        st.wordChars('~', '~');
        // XXX check ASCII table and add all other characters except special

        // special: #="(),
        st.whitespaceChars(0, ' ');
        st.commentChar('#');
        st.eolIsSignificant(true);
        st.quoteChar('\"');
    }

    private ConfigurationException excToken(String msg) {
        return new ConfigurationException(msg + " " + st);
    }

    private ConfigurationException excLine(String msg) {
        return new ConfigurationException(msg + ", line " + st.lineno());
    }

    private void parse() throws IOException {
        while (true) {
            int token = nextToken();
            if (token == TT_EOF) {
                break;
            }
            if (token == TT_EOL) {
                continue;
            }
            if (token != TT_WORD) {
                throw excToken("Unexpected token:");
            }
            String word = st.sval;
            if (word.equals("name")) {
                name = parseStringEntry(word);
            } else if (word.equals("library")) {
                library = parseLibrary(word);
            } else if (word.equals("description")) {
                parseDescription(word);
            } else if (word.equals("slot")) {
                parseSlotID(word);
            } else if (word.equals("slotListIndex")) {
                parseSlotListIndex(word);
            } else if (word.equals("enabledMechanisms")) {
                parseEnabledMechanisms(word);
            } else if (word.equals("disabledMechanisms")) {
                parseDisabledMechanisms(word);
            } else if (word.equals("attributes")) {
                parseAttributes(word);
            } else if (word.equals("handleStartupErrors")) {
                parseHandleStartupErrors(word);
            } else if (word.endsWith("insertionCheckInterval")) {
                insertionCheckInterval = parseIntegerEntry(word);
                if (insertionCheckInterval < 100) {
                    throw excLine(word + " must be at least 100 ms");
                }
            } else if (word.equals("cleaner.shortInterval")) {
                resourceCleanerShortInterval = parseIntegerEntry(word);
                if (resourceCleanerShortInterval < 1_000) {
                    throw excLine(word + " must be at least 1000 ms");
                }
            } else if (word.equals("cleaner.longInterval")) {
                resourceCleanerLongInterval = parseIntegerEntry(word);
                if (resourceCleanerLongInterval < 1_000) {
                    throw excLine(word + " must be at least 1000 ms");
                }
            } else if (word.equals("destroyTokenAfterLogout")) {
                destroyTokenAfterLogout = parseBooleanEntry(word);
            } else if (word.equals("showInfo")) {
                showInfo = parseBooleanEntry(word);
            } else if (word.equals("keyStoreCompatibilityMode")) {
                keyStoreCompatibilityMode = parseBooleanEntry(word);
            } else if (word.equals("explicitCancel")) {
                explicitCancel = parseBooleanEntry(word);
            } else if (word.equals("omitInitialize")) {
                omitInitialize = parseBooleanEntry(word);
            } else if (word.equals("allowSingleThreadedModules")) {
                allowSingleThreadedModules = parseBooleanEntry(word);
            } else if (word.equals("functionList")) {
                functionList = parseStringEntry(word);
            } else if (word.equals("nssUseSecmod")) {
                nssUseSecmod = parseBooleanEntry(word);
            } else if (word.equals("nssLibraryDirectory")) {
                nssLibraryDirectory = parseLibrary(word);
                nssUseSecmod = true;
            } else if (word.equals("nssSecmodDirectory")) {
                nssSecmodDirectory = expand(parseStringEntry(word));
                nssUseSecmod = true;
            } else if (word.equals("nssModule")) {
                nssModule = parseStringEntry(word);
                nssUseSecmod = true;
            } else if (word.equals("nssDbMode")) {
                String mode = parseStringEntry(word);
                if (mode.equals("readWrite")) {
                    nssDbMode = Secmod.DbMode.READ_WRITE;
                } else if (mode.equals("readOnly")) {
                    nssDbMode = Secmod.DbMode.READ_ONLY;
                } else if (mode.equals("noDb")) {
                    nssDbMode = Secmod.DbMode.NO_DB;
                } else {
                    throw excToken("nssDbMode must be one of readWrite, readOnly, and noDb:");
                }
                nssUseSecmod = true;
            } else if (word.equals("nssNetscapeDbWorkaround")) {
                nssNetscapeDbWorkaround = parseBooleanEntry(word);
                nssUseSecmod = true;
            } else if (word.equals("nssArgs")) {
                parseNSSArgs(word);
            } else if (word.equals("nssUseSecmodTrust")) {
                nssUseSecmodTrust = parseBooleanEntry(word);
            } else if (word.equals("useEcX963Encoding")) {
                useEcX963Encoding = parseBooleanEntry(word);
            } else if (word.equals("nssOptimizeSpace")) {
                nssOptimizeSpace = parseBooleanEntry(word);
            } else {
                throw new ConfigurationException
                        ("Unknown keyword '" + word + "', line " + st.lineno());
            }
            parsedKeywords.add(word);
        }
        reader.close();
        reader = null;
        st = null;
        parsedKeywords = null;
        if (name == null) {
            throw new ConfigurationException("name must be specified");
        }
        if (nssUseSecmod == false) {
            if (library == null) {
                throw new ConfigurationException("library must be specified");
            }
        } else {
            if (library != null) {
                throw new ConfigurationException
                    ("library must not be specified in NSS mode");
            }
            if ((slotID != -1) || (slotListIndex != -1)) {
                throw new ConfigurationException
                    ("slot and slotListIndex must not be specified in NSS mode");
            }
            if (nssArgs != null) {
                throw new ConfigurationException
                    ("nssArgs must not be specified in NSS mode");
            }
            if (nssUseSecmodTrust != false) {
                throw new ConfigurationException("nssUseSecmodTrust is an "
                    + "internal option and must not be specified in NSS mode");
            }
        }
    }

    //
    // Parsing helper methods
    //

    private int nextToken() throws IOException {
        int token = st.nextToken();
        debug(st);
        return token;
    }

    private void parseEquals() throws IOException {
        int token = nextToken();
        if (token != '=') {
            throw excToken("Expected '=', read");
        }
    }

    private void parseOpenBraces() throws IOException {
        while (true) {
            int token = nextToken();
            if (token == TT_EOL) {
                continue;
            }
            if ((token == TT_WORD) && st.sval.equals("{")) {
                return;
            }
            throw excToken("Expected '{', read");
        }
    }

    private boolean isCloseBraces(int token) {
        return (token == TT_WORD) && st.sval.equals("}");
    }

    private String parseWord() throws IOException {
        int token = nextToken();
        if (token != TT_WORD) {
            throw excToken("Unexpected value:");
        }
        return st.sval;
    }

    private String parseStringEntry(String keyword) throws IOException {
        checkDup(keyword);
        parseEquals();

        int token = nextToken();
        if (token != TT_WORD && token != '\"') {
            // not a word token nor a string enclosed by double quotes
            throw excToken("Unexpected value:");
        }
        String value = st.sval;

        debug(keyword + ": " + value);
        return value;
    }

    private boolean parseBooleanEntry(String keyword) throws IOException {
        checkDup(keyword);
        parseEquals();
        boolean value = parseBoolean();
        debug(keyword + ": " + value);
        return value;
    }

    private int parseIntegerEntry(String keyword) throws IOException {
        checkDup(keyword);
        parseEquals();
        int value = decodeNumber(parseWord());
        debug(keyword + ": " + value);
        return value;
    }

    private boolean parseBoolean() throws IOException {
        String val = parseWord();
        switch (val) {
            case "true":
                return true;
            case "false":
                return false;
            default:
                throw excToken("Expected boolean value, read:");
        }
    }

    private String parseLine() throws IOException {
        // allow quoted string as part of line
        String s = null;
        while (true) {
            int token = nextToken();
            if ((token == TT_EOL) || (token == TT_EOF)) {
                break;
            }
            if (token != TT_WORD && token != '\"') {
                throw excToken("Unexpected value");
            }
            if (s == null) {
                s = st.sval;
            } else {
                s = s + " " + st.sval;
            }
        }
        if (s == null) {
            throw excToken("Unexpected empty line");
        }
        return s;
    }

    private int decodeNumber(String str) throws IOException {
        try {
            if (str.startsWith("0x") || str.startsWith("0X")) {
                return Integer.parseInt(str.substring(2), 16);
            } else {
                return Integer.parseInt(str);
            }
        } catch (NumberFormatException e) {
            throw excToken("Expected number, read");
        }
    }

    private static boolean isNumber(String s) {
        if (s.length() == 0) {
            return false;
        }
        char ch = s.charAt(0);
        return ((ch >= '0') && (ch <= '9'));
    }

    private void parseComma() throws IOException {
        int token = nextToken();
        if (token != ',') {
            throw excToken("Expected ',', read");
        }
    }

    private static boolean isByteArray(String val) {
        return val.startsWith("0h");
    }

    private byte[] decodeByteArray(String str) throws IOException {
        if (str.startsWith("0h") == false) {
            throw excToken("Expected byte array value, read");
        }
        str = str.substring(2);
        // XXX proper hex parsing
        try {
            return new BigInteger(str, 16).toByteArray();
        } catch (NumberFormatException e) {
            throw excToken("Expected byte array value, read");
        }
    }

    private void checkDup(String keyword) throws IOException {
        if (parsedKeywords.contains(keyword)) {
            throw excLine(keyword + " must only be specified once");
        }
    }

    //
    // individual entry parsing methods
    //

    private String parseLibrary(String keyword) throws IOException {
        checkDup(keyword);
        parseEquals();
        String lib = parseLine();
        lib = expand(lib);
        int i = lib.indexOf("/$ISA/");
        if (i != -1) {
            // replace "/$ISA/" with "/"
            String prefix = lib.substring(0, i);
            String suffix = lib.substring(i + 5);
            lib = prefix + suffix;
        }
        debug(keyword + ": " + lib);

        // Check to see if full path is specified to prevent the DLL
        // preloading attack
        if (!(new File(lib)).isAbsolute()) {
            throw new ConfigurationException(
                "Absolute path required for library value: " + lib);
        }
        return lib;
    }

    private void parseDescription(String keyword) throws IOException {
        checkDup(keyword);
        parseEquals();
        description = parseLine();
        debug("description: " + description);
    }

    private void parseSlotID(String keyword) throws IOException {
        if (slotID >= 0) {
            throw excLine("Duplicate slot definition");
        }
        if (slotListIndex >= 0) {
            throw excLine
                ("Only one of slot and slotListIndex must be specified");
        }
        parseEquals();
        String slotString = parseWord();
        slotID = decodeNumber(slotString);
        debug("slot: " + slotID);
    }

    private void parseSlotListIndex(String keyword) throws IOException {
        if (slotListIndex >= 0) {
            throw excLine("Duplicate slotListIndex definition");
        }
        if (slotID >= 0) {
            throw excLine
                ("Only one of slot and slotListIndex must be specified");
        }
        parseEquals();
        String slotString = parseWord();
        slotListIndex = decodeNumber(slotString);
        debug("slotListIndex: " + slotListIndex);
    }

    private void parseEnabledMechanisms(String keyword) throws IOException {
        enabledMechanisms = parseMechanisms(keyword);
    }

    private void parseDisabledMechanisms(String keyword) throws IOException {
        disabledMechanisms = parseMechanisms(keyword);
    }

    private Set<Long> parseMechanisms(String keyword) throws IOException {
        checkDup(keyword);
        Set<Long> mechs = new HashSet<Long>();
        parseEquals();
        parseOpenBraces();
        while (true) {
            int token = nextToken();
            if (isCloseBraces(token)) {
                break;
            }
            if (token == TT_EOL) {
                continue;
            }
            if (token != TT_WORD) {
                throw excToken("Expected mechanism, read");
            }
            long mech = parseMechanism(st.sval);
            mechs.add(Long.valueOf(mech));
        }
        if (DEBUG) {
            System.out.print("mechanisms: [");
            for (Long mech : mechs) {
                System.out.print(Functions.getMechanismName(mech));
                System.out.print(", ");
            }
            System.out.println("]");
        }
        return mechs;
    }

    private long parseMechanism(String mech) throws IOException {
        if (isNumber(mech)) {
            return decodeNumber(mech);
        } else {
            try {
                return Functions.getMechanismId(mech);
            } catch (IllegalArgumentException e) {
                throw excLine("Unknown mechanism: " + mech);
            }
        }
    }

    private void parseAttributes(String keyword) throws IOException {
        if (templateManager == null) {
            templateManager = new TemplateManager();
        }
        int token = nextToken();
        if (token == '=') {
            String s = parseWord();
            if (s.equals("compatibility") == false) {
                throw excLine("Expected 'compatibility', read " + s);
            }
            setCompatibilityAttributes();
            return;
        }
        if (token != '(') {
            throw excToken("Expected '(' or '=', read");
        }
        String op = parseOperation();
        parseComma();
        long objectClass = parseObjectClass();
        parseComma();
        long keyAlg = parseKeyAlgorithm();
        token = nextToken();
        if (token != ')') {
            throw excToken("Expected ')', read");
        }
        parseEquals();
        parseOpenBraces();
        List<CK_ATTRIBUTE> attributes = new ArrayList<CK_ATTRIBUTE>();
        while (true) {
            token = nextToken();
            if (isCloseBraces(token)) {
                break;
            }
            if (token == TT_EOL) {
                continue;
            }
            if (token != TT_WORD) {
                throw excToken("Expected mechanism, read");
            }
            String attributeName = st.sval;
            long attributeId = decodeAttributeName(attributeName);
            parseEquals();
            String attributeValue = parseWord();
            attributes.add(decodeAttributeValue(attributeId, attributeValue));
        }
        templateManager.addTemplate
                (op, objectClass, keyAlg, attributes.toArray(CK_A0));
    }

    private void setCompatibilityAttributes() {
        // all secret keys
        templateManager.addTemplate(O_ANY, CKO_SECRET_KEY, PCKK_ANY,
        new CK_ATTRIBUTE[] {
            TOKEN_FALSE,
            SENSITIVE_FALSE,
            EXTRACTABLE_TRUE,
            ENCRYPT_TRUE,
            DECRYPT_TRUE,
            WRAP_TRUE,
            UNWRAP_TRUE,
        });

        // generic secret keys are special
        // They are used as MAC keys plus for the SSL/TLS (pre)master secrets
        templateManager.addTemplate(O_ANY, CKO_SECRET_KEY, CKK_GENERIC_SECRET,
        new CK_ATTRIBUTE[] {
            SIGN_TRUE,
            VERIFY_TRUE,
            ENCRYPT_NULL,
            DECRYPT_NULL,
            WRAP_NULL,
            UNWRAP_NULL,
            DERIVE_TRUE,
        });

        // all private and public keys
        templateManager.addTemplate(O_ANY, CKO_PRIVATE_KEY, PCKK_ANY,
        new CK_ATTRIBUTE[] {
            TOKEN_FALSE,
            SENSITIVE_FALSE,
            EXTRACTABLE_TRUE,
        });
        templateManager.addTemplate(O_ANY, CKO_PUBLIC_KEY, PCKK_ANY,
        new CK_ATTRIBUTE[] {
            TOKEN_FALSE,
        });

        // additional attributes for RSA private keys
        templateManager.addTemplate(O_ANY, CKO_PRIVATE_KEY, CKK_RSA,
        new CK_ATTRIBUTE[] {
            DECRYPT_TRUE,
            SIGN_TRUE,
            SIGN_RECOVER_TRUE,
            UNWRAP_TRUE,
        });
        // additional attributes for RSA public keys
        templateManager.addTemplate(O_ANY, CKO_PUBLIC_KEY, CKK_RSA,
        new CK_ATTRIBUTE[] {
            ENCRYPT_TRUE,
            VERIFY_TRUE,
            VERIFY_RECOVER_TRUE,
            WRAP_TRUE,
        });

        // additional attributes for DSA private keys
        templateManager.addTemplate(O_ANY, CKO_PRIVATE_KEY, CKK_DSA,
        new CK_ATTRIBUTE[] {
            SIGN_TRUE,
        });
        // additional attributes for DSA public keys
        templateManager.addTemplate(O_ANY, CKO_PUBLIC_KEY, CKK_DSA,
        new CK_ATTRIBUTE[] {
            VERIFY_TRUE,
        });

        // additional attributes for DH private keys
        templateManager.addTemplate(O_ANY, CKO_PRIVATE_KEY, CKK_DH,
        new CK_ATTRIBUTE[] {
            DERIVE_TRUE,
        });

        // additional attributes for EC private keys
        templateManager.addTemplate(O_ANY, CKO_PRIVATE_KEY, CKK_EC,
        new CK_ATTRIBUTE[] {
            SIGN_TRUE,
            DERIVE_TRUE,
        });
        // additional attributes for EC public keys
        templateManager.addTemplate(O_ANY, CKO_PUBLIC_KEY, CKK_EC,
        new CK_ATTRIBUTE[] {
            VERIFY_TRUE,
        });
    }

    private static final CK_ATTRIBUTE[] CK_A0 = new CK_ATTRIBUTE[0];

    private String parseOperation() throws IOException {
        String op = parseWord();
        switch (op) {
            case "*":
                return TemplateManager.O_ANY;
            case "generate":
                return TemplateManager.O_GENERATE;
            case "import":
                return TemplateManager.O_IMPORT;
            default:
                throw excLine("Unknown operation " + op);
        }
    }

    private long parseObjectClass() throws IOException {
        String name = parseWord();
        try {
            return Functions.getObjectClassId(name);
        } catch (IllegalArgumentException e) {
            throw excLine("Unknown object class " + name);
        }
    }

    private long parseKeyAlgorithm() throws IOException {
        String name = parseWord();
        if (isNumber(name)) {
            return decodeNumber(name);
        } else {
            try {
                return Functions.getKeyId(name);
            } catch (IllegalArgumentException e) {
                throw excLine("Unknown key algorithm " + name);
            }
        }
    }

    private long decodeAttributeName(String name) throws IOException {
        if (isNumber(name)) {
            return decodeNumber(name);
        } else {
            try {
                return Functions.getAttributeId(name);
            } catch (IllegalArgumentException e) {
                throw excLine("Unknown attribute name " + name);
            }
        }
    }

    private CK_ATTRIBUTE decodeAttributeValue(long id, String value)
            throws IOException {
        if (value.equals("null")) {
            return new CK_ATTRIBUTE(id);
        } else if (value.equals("true")) {
            return new CK_ATTRIBUTE(id, true);
        } else if (value.equals("false")) {
            return new CK_ATTRIBUTE(id, false);
        } else if (isByteArray(value)) {
            return new CK_ATTRIBUTE(id, decodeByteArray(value));
        } else if (isNumber(value)) {
            return new CK_ATTRIBUTE(id, Integer.valueOf(decodeNumber(value)));
        } else {
            throw excLine("Unknown attribute value " + value);
        }
    }

    private void parseNSSArgs(String keyword) throws IOException {
        checkDup(keyword);
        parseEquals();
        int token = nextToken();
        if (token != '"') {
            throw excToken("Expected quoted string");
        }
        nssArgs = expand(st.sval);
        debug("nssArgs: " + nssArgs);
    }

    private void parseHandleStartupErrors(String keyword) throws IOException {
        checkDup(keyword);
        parseEquals();
        String val = parseWord();
        if (val.equals("ignoreAll")) {
            handleStartupErrors = ERR_IGNORE_ALL;
        } else if (val.equals("ignoreMissingLibrary")) {
            handleStartupErrors = ERR_IGNORE_LIB;
        } else if (val.equals("halt")) {
            handleStartupErrors = ERR_HALT;
        } else {
            throw excToken("Invalid value for handleStartupErrors:");
        }
        debug("handleStartupErrors: " + handleStartupErrors);
    }

}

class ConfigurationException extends IOException {
    private static final long serialVersionUID = 254492758807673194L;
    ConfigurationException(String msg) {
        super(msg);
    }
}
