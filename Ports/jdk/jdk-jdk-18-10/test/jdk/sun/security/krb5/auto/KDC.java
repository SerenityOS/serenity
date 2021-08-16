/*
 * Copyright (c) 2008, 2020, Oracle and/or its affiliates. All rights reserved.
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

import jdk.test.lib.Platform;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.*;
import java.io.*;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import sun.security.krb5.*;
import sun.security.krb5.internal.*;
import sun.security.krb5.internal.ccache.CredentialsCache;
import sun.security.krb5.internal.crypto.EType;
import sun.security.krb5.internal.crypto.KeyUsage;
import sun.security.krb5.internal.ktab.KeyTab;
import sun.security.util.DerInputStream;
import sun.security.util.DerOutputStream;
import sun.security.util.DerValue;

/**
 * A KDC server.
 *
 * Note: By setting the system property native.kdc.path to a native
 * krb5 installation, this class starts a native KDC with the
 * given realm and host. It can also add new principals and save keytabs.
 * Other features might not be available.
 * <p>
 * Features:
 * <ol>
 * <li> Supports TCP and UDP
 * <li> Supports AS-REQ and TGS-REQ
 * <li> Principal db and other settings hard coded in application
 * <li> Options, say, request preauth or not
 * </ol>
 * Side effects:
 * <ol>
 * <li> The Sun-internal class <code>sun.security.krb5.Config</code> is a
 * singleton and initialized according to Kerberos settings (krb5.conf and
 * java.security.krb5.* system properties). This means once it's initialized
 * it will not automatically notice any changes to these settings (or file
 * changes of krb5.conf). The KDC class normally does not touch these
 * settings (except for the <code>writeKtab()</code> method). However, to make
 * sure nothing ever goes wrong, if you want to make any changes to these
 * settings after calling a KDC method, call <code>Config.refresh()</code> to
 * make sure your changes are reflected in the <code>Config</code> object.
 * </ol>
 * System properties recognized:
 * <ul>
 * <li>test.kdc.save.ccache
 * </ul>
 * Issues and TODOs:
 * <ol>
 * <li> Generates krb5.conf to be used on another machine, currently the kdc is
 * always localhost
 * <li> More options to KDC, say, error output, say, response nonce !=
 * request nonce
 * </ol>
 * Note: This program uses internal krb5 classes (including reflection to
 * access private fields and methods).
 * <p>
 * Usages:
 * <p>
 * 1. Init and start the KDC:
 * <pre>
 * KDC kdc = KDC.create("REALM.NAME", port, isDaemon);
 * KDC kdc = KDC.create("REALM.NAME");
 * </pre>
 * Here, <code>port</code> is the UDP and TCP port number the KDC server
 * listens on. If zero, a random port is chosen, which you can use getPort()
 * later to retrieve the value.
 * <p>
 * If <code>isDaemon</code> is true, the KDC worker threads will be daemons.
 * <p>
 * The shortcut <code>KDC.create("REALM.NAME")</code> has port=0 and
 * isDaemon=false, and is commonly used in an embedded KDC.
 * <p>
 * 2. Adding users:
 * <pre>
 * kdc.addPrincipal(String principal_name, char[] password);
 * kdc.addPrincipalRandKey(String principal_name);
 * </pre>
 * A service principal's name should look like "host/f.q.d.n". The second form
 * generates a random key. To expose this key, call <code>writeKtab()</code> to
 * save the keys into a keytab file.
 * <p>
 * Note that you need to add the principal name krbtgt/REALM.NAME yourself.
 * <p>
 * Note that you can safely add a principal at any time after the KDC is
 * started and before a user requests info on this principal.
 * <p>
 * 3. Other public methods:
 * <ul>
 * <li> <code>getPort</code>: Returns the port number the KDC uses
 * <li> <code>getRealm</code>: Returns the realm name
 * <li> <code>writeKtab</code>: Writes all principals' keys into a keytab file
 * <li> <code>saveConfig</code>: Saves a krb5.conf file to access this KDC
 * <li> <code>setOption</code>: Sets various options
 * </ul>
 * Read the javadoc for details. Lazy developer can use <code>OneKDC</code>
 * directly.
 */
public class KDC {

    public static final int DEFAULT_LIFETIME = 39600;
    public static final int DEFAULT_RENEWTIME = 86400;

    public static final String NOT_EXISTING_HOST = "not.existing.host";

    // What etypes the KDC supports. Comma-separated strings. Null for all.
    // Please note native KDCs might use different names.
    private static final String SUPPORTED_ETYPES
            = System.getProperty("kdc.supported.enctypes");

    // The native KDC
    private final NativeKdc nativeKdc;

    // The native KDC process
    private Process kdcProc = null;

    // Under the hood.

    // Principal db. principal -> pass. A case-insensitive TreeMap is used
    // so that even if the client provides a name with different case, the KDC
    // can still locate the principal and give back correct salt.
    private TreeMap<String,char[]> passwords = new TreeMap<>
            (String.CASE_INSENSITIVE_ORDER);

    // Non default salts. Precisely, there should be different salts for
    // different etypes, pretend they are the same at the moment.
    private TreeMap<String,String> salts = new TreeMap<>
            (String.CASE_INSENSITIVE_ORDER);

    // Non default s2kparams for newer etypes. Precisely, there should be
    // different s2kparams for different etypes, pretend they are the same
    // at the moment.
    private TreeMap<String,byte[]> s2kparamses = new TreeMap<>
            (String.CASE_INSENSITIVE_ORDER);

    // Alias for referrals.
    private TreeMap<String,KDC> aliasReferrals = new TreeMap<>
            (String.CASE_INSENSITIVE_ORDER);

    // Alias for local resolution.
    private TreeMap<String,PrincipalName> alias2Principals = new TreeMap<>
            (String.CASE_INSENSITIVE_ORDER);

    // Realm name
    private String realm;
    // KDC
    private String kdc;
    // Service port number
    private int port;
    // The request/response job queue
    private BlockingQueue<Job> q = new ArrayBlockingQueue<>(100);
    // Options
    private Map<Option,Object> options = new HashMap<>();
    // Realm-specific krb5.conf settings
    private List<String> conf = new ArrayList<>();

    private Thread thread1, thread2, thread3;
    private volatile boolean udpConsumerReady = false;
    private volatile boolean tcpConsumerReady = false;
    private volatile boolean dispatcherReady = false;
    DatagramSocket u1 = null;
    ServerSocket t1 = null;

    public static enum KtabMode { APPEND, EXISTING };

    /**
     * Option names, to be expanded forever.
     */
    public static enum Option {
        /**
         * Whether pre-authentication is required. Default Boolean.TRUE
         */
        PREAUTH_REQUIRED,
        /**
         * Only issue TGT in RC4
         */
        ONLY_RC4_TGT,
        /**
         * Use RC4 as the first in preauth
         */
        RC4_FIRST_PREAUTH,
        /**
         * Use only one preauth, so that some keys are not easy to generate
         */
        ONLY_ONE_PREAUTH,
        /**
         * Set all name-type to a value in response
         */
        RESP_NT,
        /**
         * Multiple ETYPE-INFO-ENTRY with same etype but different salt
         */
        DUP_ETYPE,
        /**
         * What backend server can be delegated to
         */
        OK_AS_DELEGATE,
        /**
         * Allow S4U2self, List<String> of middle servers.
         * If not set, means KDC does not understand S4U2self at all, therefore
         * would ignore any PA-FOR-USER request and send a ticket using the
         * cname of teh requestor. If set, it returns FORWARDABLE tickets to
         * a server with its name in the list
         */
        ALLOW_S4U2SELF,
        /**
         * Allow S4U2proxy, Map<String,List<String>> of middle servers to
         * backends. If not set or a backend not in a server's list,
         * Krb5.KDC_ERR_POLICY will be send for S4U2proxy request.
         */
        ALLOW_S4U2PROXY,
        /**
         * Sensitive accounts can never be delegated.
         */
        SENSITIVE_ACCOUNTS,
        /**
         * If true, will check if TGS-REQ contains a non-null addresses field.
         */
        CHECK_ADDRESSES,
    };

    /**
     * A standalone KDC server.
     */
    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 0;
        KDC kdc = create("RABBIT.HOLE", "kdc.rabbit.hole", port, false);
        kdc.addPrincipal("dummy", "bogus".toCharArray());
        kdc.addPrincipal("foo", "bar".toCharArray());
        kdc.addPrincipalRandKey("krbtgt/RABBIT.HOLE");
        kdc.addPrincipalRandKey("server/host.rabbit.hole");
        kdc.addPrincipalRandKey("backend/host.rabbit.hole");
        KDC.saveConfig("krb5.conf", kdc, "forwardable = true");
    }

    /**
     * Creates and starts a KDC running as a daemon on a random port.
     * @param realm the realm name
     * @return the running KDC instance
     * @throws java.io.IOException for any socket creation error
     */
    public static KDC create(String realm) throws IOException {
        return create(realm, "kdc." + realm.toLowerCase(Locale.US), 0, true);
    }

    public static KDC existing(String realm, String kdc, int port) {
        KDC k = new KDC(realm, kdc);
        k.port = port;
        return k;
    }

    /**
     * Creates and starts a KDC server.
     * @param realm the realm name
     * @param port the TCP and UDP port to listen to. A random port will to
     *        chosen if zero.
     * @param asDaemon if true, KDC threads will be daemons. Otherwise, not.
     * @return the running KDC instance
     * @throws java.io.IOException for any socket creation error
     */
    public static KDC create(String realm, String kdc, int port,
                             boolean asDaemon) throws IOException {
        return new KDC(realm, kdc, port, asDaemon);
    }

    /**
     * Sets an option
     * @param key the option name
     * @param value the value
     */
    public void setOption(Option key, Object value) {
        if (value == null) {
            options.remove(key);
        } else {
            options.put(key, value);
        }
    }

    /**
     * Writes or appends keys into a keytab.
     * <p>
     * Attention: This is the most basic one of a series of methods below on
     * keytab creation or modification. All these methods reference krb5.conf
     * settings. If you need to modify krb5.conf or switch to another krb5.conf
     * later, please call <code>Config.refresh()</code> again. For example:
     * <pre>
     * kdc.writeKtab("/etc/kdc/ktab", true);  // Config is initialized,
     * System.setProperty("java.security.krb5.conf", "/home/mykrb5.conf");
     * Config.refresh();
     * </pre>
     * Inside this method there are 2 places krb5.conf is used:
     * <ol>
     * <li> (Fatal) Generating keys: EncryptionKey.acquireSecretKeys
     * <li> (Has workaround) Creating PrincipalName
     * </ol>
     * @param tab the keytab file name
     * @param append true if append, otherwise, overwrite.
     * @param names the names to write into, write all if names is empty
     */
    public void writeKtab(String tab, boolean append, String... names)
            throws IOException, KrbException {
        KeyTab ktab = null;
        if (nativeKdc == null) {
            ktab = append ? KeyTab.getInstance(tab) : KeyTab.create(tab);
        }
        Iterable<String> entries =
                (names.length != 0) ? Arrays.asList(names): passwords.keySet();
        for (String name : entries) {
            if (name.indexOf('@') < 0) {
                name = name + "@" + realm;
            }
            if (nativeKdc == null) {
                char[] pass = passwords.get(name);
                int kvno = 0;
                if (Character.isDigit(pass[pass.length - 1])) {
                    kvno = pass[pass.length - 1] - '0';
                }
                PrincipalName pn = new PrincipalName(name,
                        name.indexOf('/') < 0 ?
                                PrincipalName.KRB_NT_UNKNOWN :
                                PrincipalName.KRB_NT_SRV_HST);
                ktab.addEntry(pn,
                        getSalt(pn),
                        pass,
                        kvno,
                        true);
            } else {
                nativeKdc.ktadd(name, tab);
            }
        }
        if (nativeKdc == null) {
            ktab.save();
        }
    }

    /**
     * Writes all principals' keys from multiple KDCs into one keytab file.
     * @throws java.io.IOException for any file output error
     * @throws sun.security.krb5.KrbException for any realm and/or principal
     *         name error.
     */
    public static void writeMultiKtab(String tab, KDC... kdcs)
            throws IOException, KrbException {
        KeyTab.create(tab).save();      // Empty the old keytab
        appendMultiKtab(tab, kdcs);
    }

    /**
     * Appends all principals' keys from multiple KDCs to one keytab file.
     */
    public static void appendMultiKtab(String tab, KDC... kdcs)
            throws IOException, KrbException {
        for (KDC kdc: kdcs) {
            kdc.writeKtab(tab, true);
        }
    }

    /**
     * Write a ktab for this KDC.
     */
    public void writeKtab(String tab) throws IOException, KrbException {
        writeKtab(tab, false);
    }

    /**
     * Appends keys in this KDC to a ktab.
     */
    public void appendKtab(String tab) throws IOException, KrbException {
        writeKtab(tab, true);
    }

    /**
     * Adds a new principal to this realm with a given password.
     * @param user the principal's name. For a service principal, use the
     *        form of host/f.q.d.n
     * @param pass the password for the principal
     */
    public void addPrincipal(String user, char[] pass) {
        addPrincipal(user, pass, null, null);
    }

    /**
     * Adds a new principal to this realm with a given password.
     * @param user the principal's name. For a service principal, use the
     *        form of host/f.q.d.n
     * @param pass the password for the principal
     * @param salt the salt, or null if a default value will be used
     * @param s2kparams the s2kparams, or null if a default value will be used
     */
    public void addPrincipal(
            String user, char[] pass, String salt, byte[] s2kparams) {
        if (user.indexOf('@') < 0) {
            user = user + "@" + realm;
        }
        if (nativeKdc != null) {
            if (!user.equals("krbtgt/" + realm)) {
                nativeKdc.addPrincipal(user, new String(pass));
            }
            passwords.put(user, new char[0]);
        } else {
            passwords.put(user, pass);
            if (salt != null) {
                salts.put(user, salt);
            }
            if (s2kparams != null) {
                s2kparamses.put(user, s2kparams);
            }
        }
    }

    /**
     * Adds a new principal to this realm with a random password
     * @param user the principal's name. For a service principal, use the
     *        form of host/f.q.d.n
     */
    public void addPrincipalRandKey(String user) {
        addPrincipal(user, randomPassword());
    }

    /**
     * Returns the name of this realm
     * @return the name of this realm
     */
    public String getRealm() {
        return realm;
    }

    /**
     * Returns the name of kdc
     * @return the name of kdc
     */
    public String getKDC() {
        return kdc;
    }

    /**
     * Add realm-specific krb5.conf setting
     */
    public void addConf(String s) {
        conf.add(s);
    }

    /**
     * Writes a krb5.conf for one or more KDC that includes KDC locations for
     * each realm and the default realm name. You can also add extra strings
     * into the file. The method should be called like:
     * <pre>
     *   KDC.saveConfig("krb5.conf", kdc1, kdc2, ..., line1, line2, ...);
     * </pre>
     * Here you can provide one or more kdc# and zero or more line# arguments.
     * The line# will be put after [libdefaults] and before [realms]. Therefore
     * you can append new lines into [libdefaults] and/or create your new
     * stanzas as well. Note that a newline character will be appended to
     * each line# argument.
     * <p>
     * For example:
     * <pre>
     * KDC.saveConfig("krb5.conf", this);
     * </pre>
     * generates:
     * <pre>
     * [libdefaults]
     * default_realm = REALM.NAME
     *
     * [realms]
     *   REALM.NAME = {
     *     kdc = host:port_number
     *     # realm-specific settings
     *   }
     * </pre>
     *
     * Another example:
     * <pre>
     * KDC.saveConfig("krb5.conf", kdc1, kdc2, "forwardable = true", "",
     *         "[domain_realm]",
     *         ".kdc1.com = KDC1.NAME");
     * </pre>
     * generates:
     * <pre>
     * [libdefaults]
     * default_realm = KDC1.NAME
     * forwardable = true
     *
     * [domain_realm]
     * .kdc1.com = KDC1.NAME
     *
     * [realms]
     *   KDC1.NAME = {
     *     kdc = host:port1
     *   }
     *   KDC2.NAME = {
     *     kdc = host:port2
     *   }
     * </pre>
     * @param file the name of the file to write into
     * @param kdc the first (and default) KDC
     * @param more more KDCs or extra lines (in their appearing order) to
     * insert into the krb5.conf file. This method reads each argument's type
     * to determine what it's for. This argument can be empty.
     * @throws java.io.IOException for any file output error
     */
    public static void saveConfig(String file, KDC kdc, Object... more)
            throws IOException {
        StringBuffer sb = new StringBuffer();
        sb.append("[libdefaults]\ndefault_realm = ");
        sb.append(kdc.realm);
        sb.append("\n");
        for (Object o : more) {
            if (o instanceof String) {
                sb.append(o);
                sb.append("\n");
            }
        }
        sb.append("\n[realms]\n");
        sb.append(kdc.realmLine());
        for (Object o : more) {
            if (o instanceof KDC) {
                sb.append(((KDC) o).realmLine());
            }
        }
        Files.write(Paths.get(file), sb.toString().getBytes());
    }

    /**
     * Returns the service port of the KDC server.
     * @return the KDC service port
     */
    public int getPort() {
        return port;
    }

    /**
     * Register an alias name to be referred to a different KDC for
     * resolution, according to RFC 6806.
     * @param alias Alias name (i.e. user@REALM.COM).
     * @param referredKDC KDC to which the alias is referred for resolution.
     */
    public void registerAlias(String alias, KDC referredKDC) {
        aliasReferrals.remove(alias);
        aliasReferrals.put(alias, referredKDC);
    }

    /**
     * Register an alias to be resolved to a Principal Name locally,
     * according to RFC 6806.
     * @param alias Alias name (i.e. user@REALM.COM).
     * @param user Principal Name to which the alias is resolved.
     */
    public void registerAlias(String alias, String user)
            throws RealmException {
        alias2Principals.remove(alias);
        alias2Principals.put(alias, new PrincipalName(user));
    }

    // Private helper methods

    /**
     * Private constructor, cannot be called outside.
     * @param realm
     */
    private KDC(String realm, String kdc) {
        this.realm = realm;
        this.kdc = kdc;
        this.nativeKdc = null;
    }

    /**
     * A constructor that starts the KDC service also.
     */
    protected KDC(String realm, String kdc, int port, boolean asDaemon)
            throws IOException {
        this.realm = realm;
        this.kdc = kdc;
        this.nativeKdc = NativeKdc.get(this);
        startServer(port, asDaemon);
    }
    /**
     * Generates a 32-char random password
     * @return the password
     */
    private static char[] randomPassword() {
        char[] pass = new char[32];
        Random r = new Random();
        for (int i=0; i<31; i++)
            pass[i] = (char)('a' + r.nextInt(26));
        // The last char cannot be a number, otherwise, keyForUser()
        // believes it's a sign of kvno
        pass[31] = 'Z';
        return pass;
    }

    /**
     * Generates a random key for the given encryption type.
     * @param eType the encryption type
     * @return the generated key
     * @throws sun.security.krb5.KrbException for unknown/unsupported etype
     */
    private static EncryptionKey generateRandomKey(int eType)
            throws KrbException  {
        return genKey0(randomPassword(), "NOTHING", null, eType, null);
    }

    /**
     * Returns the password for a given principal
     * @param p principal
     * @return the password
     * @throws sun.security.krb5.KrbException when the principal is not inside
     *         the database.
     */
    private char[] getPassword(PrincipalName p, boolean server)
            throws KrbException {
        String pn = p.toString();
        if (p.getRealmString() == null) {
            pn = pn + "@" + getRealm();
        }
        char[] pass = passwords.get(pn);
        if (pass == null) {
            throw new KrbException(server?
                Krb5.KDC_ERR_S_PRINCIPAL_UNKNOWN:
                Krb5.KDC_ERR_C_PRINCIPAL_UNKNOWN, pn.toString());
        }
        return pass;
    }

    /**
     * Returns the salt string for the principal.
     * @param p principal
     * @return the salt
     */
    protected String getSalt(PrincipalName p) {
        String pn = p.toString();
        if (p.getRealmString() == null) {
            pn = pn + "@" + getRealm();
        }
        if (salts.containsKey(pn)) {
            return salts.get(pn);
        }
        if (passwords.containsKey(pn)) {
            try {
                // Find the principal name with correct case.
                p = new PrincipalName(passwords.ceilingEntry(pn).getKey());
            } catch (RealmException re) {
                // Won't happen
            }
        }
        String s = p.getRealmString();
        if (s == null) s = getRealm();
        for (String n: p.getNameStrings()) {
            s += n;
        }
        return s;
    }

    /**
     * Returns the s2kparams for the principal given the etype.
     * @param p principal
     * @param etype encryption type
     * @return the s2kparams, might be null
     */
    protected byte[] getParams(PrincipalName p, int etype) {
        switch (etype) {
            case EncryptedData.ETYPE_AES128_CTS_HMAC_SHA1_96:
            case EncryptedData.ETYPE_AES256_CTS_HMAC_SHA1_96:
            case EncryptedData.ETYPE_AES128_CTS_HMAC_SHA256_128:
            case EncryptedData.ETYPE_AES256_CTS_HMAC_SHA384_192:
                String pn = p.toString();
                if (p.getRealmString() == null) {
                    pn = pn + "@" + getRealm();
                }
                if (s2kparamses.containsKey(pn)) {
                    return s2kparamses.get(pn);
                }
                if (etype < EncryptedData.ETYPE_AES128_CTS_HMAC_SHA256_128) {
                    return new byte[]{0, 0, 0x10, 0};
                } else {
                    return new byte[]{0, 0, (byte) 0x80, 0};
                }
            default:
                return null;
        }
    }

    /**
     * Returns the key for a given principal of the given encryption type
     * @param p the principal
     * @param etype the encryption type
     * @param server looking for a server principal?
     * @return the key
     * @throws sun.security.krb5.KrbException for unknown/unsupported etype
     */
    EncryptionKey keyForUser(PrincipalName p, int etype, boolean server)
            throws KrbException {
        try {
            // Do not call EncryptionKey.acquireSecretKeys(), otherwise
            // the krb5.conf config file would be loaded.
            Integer kvno = null;
            // For service whose password ending with a number, use it as kvno.
            // Kvno must be postive.
            if (p.toString().indexOf('/') > 0) {
                char[] pass = getPassword(p, server);
                if (Character.isDigit(pass[pass.length-1])) {
                    kvno = pass[pass.length-1] - '0';
                }
            }
            return genKey0(getPassword(p, server), getSalt(p),
                    getParams(p, etype), etype, kvno);
        } catch (KrbException ke) {
            throw ke;
        } catch (Exception e) {
            throw new RuntimeException(e);  // should not happen
        }
    }

    /**
     * Returns a KerberosTime.
     *
     * @param offset offset from NOW in seconds
     */
    private static KerberosTime timeAfter(int offset) {
        return new KerberosTime(new Date().getTime() + offset * 1000L);
    }

    /**
     * Generates key from password.
     */
    private static EncryptionKey genKey0(
            char[] pass, String salt, byte[] s2kparams,
            int etype, Integer kvno) throws KrbException {
        return new EncryptionKey(EncryptionKeyDotStringToKey(
                pass, salt, s2kparams, etype),
                etype, kvno);
    }

    /**
     * Processes an incoming request and generates a response.
     * @param in the request
     * @return the response
     * @throws java.lang.Exception for various errors
     */
    protected byte[] processMessage(byte[] in) throws Exception {
        if ((in[0] & 0x1f) == Krb5.KRB_AS_REQ)
            return processAsReq(in);
        else
            return processTgsReq(in);
    }

    /**
     * Processes a TGS_REQ and generates a TGS_REP (or KRB_ERROR)
     * @param in the request
     * @return the response
     * @throws java.lang.Exception for various errors
     */
    protected byte[] processTgsReq(byte[] in) throws Exception {
        TGSReq tgsReq = new TGSReq(in);
        PrincipalName service = tgsReq.reqBody.sname;
        if (options.containsKey(KDC.Option.RESP_NT)) {
            service = new PrincipalName((int)options.get(KDC.Option.RESP_NT),
                    service.getNameStrings(), service.getRealm());
        }
        try {
            System.out.println(realm + "> " + tgsReq.reqBody.cname +
                    " sends TGS-REQ for " +
                    service + ", " + tgsReq.reqBody.kdcOptions);
            KDCReqBody body = tgsReq.reqBody;
            int[] eTypes = filterSupported(KDCReqBodyDotEType(body));
            if (eTypes.length == 0) {
                throw new KrbException(Krb5.KDC_ERR_ETYPE_NOSUPP);
            }
            int e2 = eTypes[0];     // etype for outgoing session key
            int e3 = eTypes[0];     // etype for outgoing ticket

            PAData[] pas = tgsReq.pAData;

            Ticket tkt = null;
            EncTicketPart etp = null;

            PrincipalName cname = null;
            boolean allowForwardable = true;
            boolean isReferral = false;
            if (body.kdcOptions.get(KDCOptions.CANONICALIZE)) {
                System.out.println(realm + "> verifying referral for " +
                        body.sname.getNameString());
                KDC referral = aliasReferrals.get(body.sname.getNameString());
                if (referral != null) {
                    service = new PrincipalName(
                            PrincipalName.TGS_DEFAULT_SRV_NAME +
                            PrincipalName.NAME_COMPONENT_SEPARATOR_STR +
                            referral.getRealm(), PrincipalName.KRB_NT_SRV_INST,
                            this.getRealm());
                    System.out.println(realm + "> referral to " +
                            referral.getRealm());
                    isReferral = true;
                }
            }

            if (pas == null || pas.length == 0) {
                throw new KrbException(Krb5.KDC_ERR_PADATA_TYPE_NOSUPP);
            } else {
                PrincipalName forUserCName = null;
                for (PAData pa: pas) {
                    if (pa.getType() == Krb5.PA_TGS_REQ) {
                        APReq apReq = new APReq(pa.getValue());
                        tkt = apReq.ticket;
                        int te = tkt.encPart.getEType();
                        EncryptionKey kkey = keyForUser(tkt.sname, te, true);
                        byte[] bb = tkt.encPart.decrypt(kkey, KeyUsage.KU_TICKET);
                        DerInputStream derIn = new DerInputStream(bb);
                        DerValue der = derIn.getDerValue();
                        etp = new EncTicketPart(der.toByteArray());
                        // Finally, cname will be overwritten by PA-FOR-USER
                        // if it exists.
                        cname = etp.cname;
                        System.out.println(realm + "> presenting a ticket of "
                                + etp.cname + " to " + tkt.sname);
                    } else if (pa.getType() == Krb5.PA_FOR_USER) {
                        if (options.containsKey(Option.ALLOW_S4U2SELF)) {
                            PAForUserEnc p4u = new PAForUserEnc(
                                    new DerValue(pa.getValue()), null);
                            forUserCName = p4u.name;
                            System.out.println(realm + "> See PA_FOR_USER "
                                    + " in the name of " + p4u.name);
                        }
                    }
                }
                if (forUserCName != null) {
                    List<String> names = (List<String>)
                            options.get(Option.ALLOW_S4U2SELF);
                    if (!names.contains(cname.toString())) {
                        // Mimic the normal KDC behavior. When a server is not
                        // allowed to send S4U2self, do not send an error.
                        // Instead, send a ticket which is useless later.
                        allowForwardable = false;
                    }
                    cname = forUserCName;
                }
                if (tkt == null) {
                    throw new KrbException(Krb5.KDC_ERR_PADATA_TYPE_NOSUPP);
                }
            }

            // Session key for original ticket, TGT
            EncryptionKey ckey = etp.key;

            // Session key for session with the service
            EncryptionKey key = generateRandomKey(e2);

            // Check time, TODO
            KerberosTime from = body.from;
            KerberosTime till = body.till;
            if (from == null || from.isZero()) {
                from = timeAfter(0);
            }
            if (till == null) {
                throw new KrbException(Krb5.KDC_ERR_NEVER_VALID); // TODO
            } else if (till.isZero()) {
                till = timeAfter(DEFAULT_LIFETIME);
            }

            boolean[] bFlags = new boolean[Krb5.TKT_OPTS_MAX+1];
            if (body.kdcOptions.get(KDCOptions.FORWARDABLE)
                    && allowForwardable) {
                List<String> sensitives = (List<String>)
                        options.get(Option.SENSITIVE_ACCOUNTS);
                if (sensitives != null && sensitives.contains(cname.toString())) {
                    // Cannot make FORWARDABLE
                } else {
                    bFlags[Krb5.TKT_OPTS_FORWARDABLE] = true;
                }
            }
            // We do not request for addresses for FORWARDED tickets
            if (options.containsKey(Option.CHECK_ADDRESSES)
                    && body.kdcOptions.get(KDCOptions.FORWARDED)
                    && body.addresses != null) {
                throw new KrbException(Krb5.KDC_ERR_BADOPTION);
            }
            if (body.kdcOptions.get(KDCOptions.FORWARDED) ||
                    etp.flags.get(Krb5.TKT_OPTS_FORWARDED)) {
                bFlags[Krb5.TKT_OPTS_FORWARDED] = true;
            }
            if (body.kdcOptions.get(KDCOptions.RENEWABLE)) {
                bFlags[Krb5.TKT_OPTS_RENEWABLE] = true;
                //renew = timeAfter(3600 * 24 * 7);
            }
            if (body.kdcOptions.get(KDCOptions.PROXIABLE)) {
                bFlags[Krb5.TKT_OPTS_PROXIABLE] = true;
            }
            if (body.kdcOptions.get(KDCOptions.POSTDATED)) {
                bFlags[Krb5.TKT_OPTS_POSTDATED] = true;
            }
            if (body.kdcOptions.get(KDCOptions.ALLOW_POSTDATE)) {
                bFlags[Krb5.TKT_OPTS_MAY_POSTDATE] = true;
            }
            if (body.kdcOptions.get(KDCOptions.CNAME_IN_ADDL_TKT)) {
                if (!options.containsKey(Option.ALLOW_S4U2PROXY)) {
                    // Don't understand CNAME_IN_ADDL_TKT
                    throw new KrbException(Krb5.KDC_ERR_BADOPTION);
                } else {
                    Map<String,List<String>> map = (Map<String,List<String>>)
                            options.get(Option.ALLOW_S4U2PROXY);
                    Ticket second = KDCReqBodyDotFirstAdditionalTicket(body);
                    EncryptionKey key2 = keyForUser(
                            second.sname, second.encPart.getEType(), true);
                    byte[] bb = second.encPart.decrypt(key2, KeyUsage.KU_TICKET);
                    DerInputStream derIn = new DerInputStream(bb);
                    DerValue der = derIn.getDerValue();
                    EncTicketPart tktEncPart = new EncTicketPart(der.toByteArray());
                    if (!tktEncPart.flags.get(Krb5.TKT_OPTS_FORWARDABLE)) {
                        //throw new KrbException(Krb5.KDC_ERR_BADOPTION);
                    }
                    PrincipalName client = tktEncPart.cname;
                    System.out.println(realm + "> and an additional ticket of "
                            + client + " to " + second.sname);
                    if (map.containsKey(cname.toString())) {
                        if (map.get(cname.toString()).contains(service.toString())) {
                            System.out.println(realm + "> S4U2proxy OK");
                        } else {
                            throw new KrbException(Krb5.KDC_ERR_BADOPTION);
                        }
                    } else {
                        throw new KrbException(Krb5.KDC_ERR_BADOPTION);
                    }
                    cname = client;
                }
            }

            String okAsDelegate = (String)options.get(Option.OK_AS_DELEGATE);
            if (okAsDelegate != null && (
                    okAsDelegate.isEmpty() ||
                    okAsDelegate.contains(service.getNameString()))) {
                bFlags[Krb5.TKT_OPTS_DELEGATE] = true;
            }
            bFlags[Krb5.TKT_OPTS_INITIAL] = true;

            KerberosTime renewTill = etp.renewTill;
            if (renewTill != null && body.kdcOptions.get(KDCOptions.RENEW)) {
                // till should never pass renewTill
                if (till.greaterThan(renewTill)) {
                    till = renewTill;
                }
                if (System.getProperty("test.set.null.renew") != null) {
                    // Testing 8186576, see NullRenewUntil.java.
                    renewTill = null;
                }
            }

            TicketFlags tFlags = new TicketFlags(bFlags);
            EncTicketPart enc = new EncTicketPart(
                    tFlags,
                    key,
                    cname,
                    new TransitedEncoding(1, new byte[0]),  // TODO
                    timeAfter(0),
                    from,
                    till, renewTill,
                    body.addresses != null ? body.addresses
                            : etp.caddr,
                    null);
            EncryptionKey skey = keyForUser(service, e3, true);
            if (skey == null) {
                throw new KrbException(Krb5.KDC_ERR_SUMTYPE_NOSUPP); // TODO
            }
            Ticket t = new Ticket(
                    System.getProperty("test.kdc.diff.sname") != null ?
                        new PrincipalName("xx" + service.toString()) :
                        service,
                    new EncryptedData(skey, enc.asn1Encode(), KeyUsage.KU_TICKET)
            );
            EncTGSRepPart enc_part = new EncTGSRepPart(
                    key,
                    new LastReq(new LastReqEntry[] {
                        new LastReqEntry(0, timeAfter(-10))
                    }),
                    body.getNonce(),    // TODO: detect replay
                    timeAfter(3600 * 24),
                    // Next 5 and last MUST be same with ticket
                    tFlags,
                    timeAfter(0),
                    from,
                    till, renewTill,
                    service,
                    body.addresses,
                    null
                    );
            EncryptedData edata = new EncryptedData(ckey, enc_part.asn1Encode(),
                    KeyUsage.KU_ENC_TGS_REP_PART_SESSKEY);
            TGSRep tgsRep = new TGSRep(null,
                    cname,
                    t,
                    edata);
            System.out.println("     Return " + tgsRep.cname
                    + " ticket for " + tgsRep.ticket.sname + ", flags "
                    + tFlags);

            DerOutputStream out = new DerOutputStream();
            out.write(DerValue.createTag(DerValue.TAG_APPLICATION,
                    true, (byte)Krb5.KRB_TGS_REP), tgsRep.asn1Encode());
            return out.toByteArray();
        } catch (KrbException ke) {
            ke.printStackTrace(System.out);
            KRBError kerr = ke.getError();
            KDCReqBody body = tgsReq.reqBody;
            System.out.println("     Error " + ke.returnCode()
                    + " " +ke.returnCodeMessage());
            if (kerr == null) {
                kerr = new KRBError(null, null, null,
                        timeAfter(0),
                        0,
                        ke.returnCode(),
                        body.cname,
                        service,
                        KrbException.errorMessage(ke.returnCode()),
                        null);
            }
            return kerr.asn1Encode();
        }
    }

    /**
     * Processes a AS_REQ and generates a AS_REP (or KRB_ERROR)
     * @param in the request
     * @return the response
     * @throws java.lang.Exception for various errors
     */
    protected byte[] processAsReq(byte[] in) throws Exception {
        ASReq asReq = new ASReq(in);
        byte[] asReqbytes = asReq.asn1Encode();
        int[] eTypes = null;
        List<PAData> outPAs = new ArrayList<>();

        PrincipalName service = asReq.reqBody.sname;
        if (options.containsKey(KDC.Option.RESP_NT)) {
            service = new PrincipalName((int)options.get(KDC.Option.RESP_NT),
                    service.getNameStrings(),
                    Realm.getDefault());
        }
        try {
            System.out.println(realm + "> " + asReq.reqBody.cname +
                    " sends AS-REQ for " +
                    service + ", " + asReq.reqBody.kdcOptions);

            KDCReqBody body = asReq.reqBody;

            eTypes = filterSupported(KDCReqBodyDotEType(body));
            if (eTypes.length == 0) {
                throw new KrbException(Krb5.KDC_ERR_ETYPE_NOSUPP);
            }
            int eType = eTypes[0];

            if (body.kdcOptions.get(KDCOptions.CANONICALIZE)) {
                PrincipalName principal = alias2Principals.get(
                        body.cname.getNameString());
                if (principal != null) {
                    body.cname = principal;
                } else {
                    KDC referral = aliasReferrals.get(body.cname.getNameString());
                    if (referral != null) {
                        body.cname = new PrincipalName(
                                PrincipalName.TGS_DEFAULT_SRV_NAME,
                                PrincipalName.KRB_NT_SRV_INST,
                                referral.getRealm());
                        throw new KrbException(Krb5.KRB_ERR_WRONG_REALM);
                    }
                }
            }

            EncryptionKey ckey = keyForUser(body.cname, eType, false);
            EncryptionKey skey = keyForUser(service, eType, true);

            if (options.containsKey(KDC.Option.ONLY_RC4_TGT)) {
                int tgtEType = EncryptedData.ETYPE_ARCFOUR_HMAC;
                boolean found = false;
                for (int i=0; i<eTypes.length; i++) {
                    if (eTypes[i] == tgtEType) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    throw new KrbException(Krb5.KDC_ERR_ETYPE_NOSUPP);
                }
                skey = keyForUser(service, tgtEType, true);
            }
            if (ckey == null) {
                throw new KrbException(Krb5.KDC_ERR_ETYPE_NOSUPP);
            }
            if (skey == null) {
                throw new KrbException(Krb5.KDC_ERR_SUMTYPE_NOSUPP); // TODO
            }

            // Session key
            EncryptionKey key = generateRandomKey(eType);
            // Check time, TODO
            KerberosTime from = body.from;
            KerberosTime till = body.till;
            KerberosTime rtime = body.rtime;
            if (from == null || from.isZero()) {
                from = timeAfter(0);
            }
            if (till == null) {
                throw new KrbException(Krb5.KDC_ERR_NEVER_VALID); // TODO
            } else if (till.isZero()) {
                till = timeAfter(DEFAULT_LIFETIME);
            } else if (till.greaterThan(timeAfter(24 * 3600))
                     && System.getProperty("test.kdc.force.till") == null) {
                // If till is more than 1 day later, make it renewable
                till = timeAfter(DEFAULT_LIFETIME);
                body.kdcOptions.set(KDCOptions.RENEWABLE, true);
                if (rtime == null) rtime = till;
            }
            if (rtime == null && body.kdcOptions.get(KDCOptions.RENEWABLE)) {
                rtime = timeAfter(DEFAULT_RENEWTIME);
            }
            //body.from
            boolean[] bFlags = new boolean[Krb5.TKT_OPTS_MAX+1];
            if (body.kdcOptions.get(KDCOptions.FORWARDABLE)) {
                List<String> sensitives = (List<String>)
                        options.get(Option.SENSITIVE_ACCOUNTS);
                if (sensitives != null
                        && sensitives.contains(body.cname.toString())) {
                    // Cannot make FORWARDABLE
                } else {
                    bFlags[Krb5.TKT_OPTS_FORWARDABLE] = true;
                }
            }
            if (body.kdcOptions.get(KDCOptions.RENEWABLE)) {
                bFlags[Krb5.TKT_OPTS_RENEWABLE] = true;
                //renew = timeAfter(3600 * 24 * 7);
            }
            if (body.kdcOptions.get(KDCOptions.PROXIABLE)) {
                bFlags[Krb5.TKT_OPTS_PROXIABLE] = true;
            }
            if (body.kdcOptions.get(KDCOptions.POSTDATED)) {
                bFlags[Krb5.TKT_OPTS_POSTDATED] = true;
            }
            if (body.kdcOptions.get(KDCOptions.ALLOW_POSTDATE)) {
                bFlags[Krb5.TKT_OPTS_MAY_POSTDATE] = true;
            }
            bFlags[Krb5.TKT_OPTS_INITIAL] = true;
            if (System.getProperty("test.kdc.always.enc.pa.rep") != null) {
                bFlags[Krb5.TKT_OPTS_ENC_PA_REP] = true;
            }

            // Creating PA-DATA
            DerValue[] pas2 = null, pas = null;
            if (options.containsKey(KDC.Option.DUP_ETYPE)) {
                int n = (Integer)options.get(KDC.Option.DUP_ETYPE);
                switch (n) {
                    case 1:     // customer's case in 7067974
                        pas2 = new DerValue[] {
                            new DerValue(new ETypeInfo2(1, null, null).asn1Encode()),
                            new DerValue(new ETypeInfo2(1, "", null).asn1Encode()),
                            new DerValue(new ETypeInfo2(
                                    1, realm, new byte[]{1}).asn1Encode()),
                        };
                        pas = new DerValue[] {
                            new DerValue(new ETypeInfo(1, null).asn1Encode()),
                            new DerValue(new ETypeInfo(1, "").asn1Encode()),
                            new DerValue(new ETypeInfo(1, realm).asn1Encode()),
                        };
                        break;
                    case 2:     // we still reject non-null s2kparams and prefer E2 over E
                        pas2 = new DerValue[] {
                            new DerValue(new ETypeInfo2(
                                    1, realm, new byte[]{1}).asn1Encode()),
                            new DerValue(new ETypeInfo2(1, null, null).asn1Encode()),
                            new DerValue(new ETypeInfo2(1, "", null).asn1Encode()),
                        };
                        pas = new DerValue[] {
                            new DerValue(new ETypeInfo(1, realm).asn1Encode()),
                            new DerValue(new ETypeInfo(1, null).asn1Encode()),
                            new DerValue(new ETypeInfo(1, "").asn1Encode()),
                        };
                        break;
                    case 3:     // but only E is wrong
                        pas = new DerValue[] {
                            new DerValue(new ETypeInfo(1, realm).asn1Encode()),
                            new DerValue(new ETypeInfo(1, null).asn1Encode()),
                            new DerValue(new ETypeInfo(1, "").asn1Encode()),
                        };
                        break;
                    case 4:     // we also ignore rc4-hmac
                        pas = new DerValue[] {
                            new DerValue(new ETypeInfo(23, "ANYTHING").asn1Encode()),
                            new DerValue(new ETypeInfo(1, null).asn1Encode()),
                            new DerValue(new ETypeInfo(1, "").asn1Encode()),
                        };
                        break;
                    case 5:     // "" should be wrong, but we accept it now
                                // See s.s.k.internal.PAData$SaltAndParams
                        pas = new DerValue[] {
                            new DerValue(new ETypeInfo(1, "").asn1Encode()),
                            new DerValue(new ETypeInfo(1, null).asn1Encode()),
                        };
                        break;
                }
            } else {
                int[] epas = eTypes;
                if (options.containsKey(KDC.Option.RC4_FIRST_PREAUTH)) {
                    for (int i=1; i<epas.length; i++) {
                        if (epas[i] == EncryptedData.ETYPE_ARCFOUR_HMAC) {
                            epas[i] = epas[0];
                            epas[0] = EncryptedData.ETYPE_ARCFOUR_HMAC;
                            break;
                        }
                    };
                } else if (options.containsKey(KDC.Option.ONLY_ONE_PREAUTH)) {
                    epas = new int[] { eTypes[0] };
                }
                pas2 = new DerValue[epas.length];
                for (int i=0; i<epas.length; i++) {
                    pas2[i] = new DerValue(new ETypeInfo2(
                            epas[i],
                            epas[i] == EncryptedData.ETYPE_ARCFOUR_HMAC ?
                                null : getSalt(body.cname),
                            getParams(body.cname, epas[i])).asn1Encode());
                }
                boolean allOld = true;
                for (int i: eTypes) {
                    if (i >= EncryptedData.ETYPE_AES128_CTS_HMAC_SHA1_96 &&
                            i != EncryptedData.ETYPE_ARCFOUR_HMAC) {
                        allOld = false;
                        break;
                    }
                }
                if (allOld) {
                    pas = new DerValue[epas.length];
                    for (int i=0; i<epas.length; i++) {
                        pas[i] = new DerValue(new ETypeInfo(
                                epas[i],
                                epas[i] == EncryptedData.ETYPE_ARCFOUR_HMAC ?
                                    null : getSalt(body.cname)
                                ).asn1Encode());
                    }
                }
            }

            DerOutputStream eid;
            if (pas2 != null) {
                eid = new DerOutputStream();
                eid.putSequence(pas2);
                outPAs.add(new PAData(Krb5.PA_ETYPE_INFO2, eid.toByteArray()));
            }
            if (pas != null) {
                eid = new DerOutputStream();
                eid.putSequence(pas);
                outPAs.add(new PAData(Krb5.PA_ETYPE_INFO, eid.toByteArray()));
            }

            PAData[] inPAs = asReq.pAData;
            List<PAData> enc_outPAs = new ArrayList<>();

            byte[] paEncTimestamp = null;
            if (inPAs != null) {
                for (PAData inPA : inPAs) {
                    if (inPA.getType() == Krb5.PA_ENC_TIMESTAMP) {
                        paEncTimestamp = inPA.getValue();
                    }
                }
            }

            if (paEncTimestamp == null) {
                Object preauth = options.get(Option.PREAUTH_REQUIRED);
                if (preauth == null || preauth.equals(Boolean.TRUE)) {
                    throw new KrbException(Krb5.KDC_ERR_PREAUTH_REQUIRED);
                }
            } else {
                EncryptionKey pakey = null;
                try {
                    EncryptedData data = newEncryptedData(
                            new DerValue(paEncTimestamp));
                    pakey = keyForUser(body.cname, data.getEType(), false);
                    data.decrypt(pakey, KeyUsage.KU_PA_ENC_TS);
                } catch (Exception e) {
                    KrbException ke = new KrbException(Krb5.KDC_ERR_PREAUTH_FAILED);
                    ke.initCause(e);
                    throw ke;
                }
                bFlags[Krb5.TKT_OPTS_PRE_AUTHENT] = true;
                for (PAData pa : inPAs) {
                    if (pa.getType() == Krb5.PA_REQ_ENC_PA_REP) {
                        Checksum ckSum = new Checksum(
                                Checksum.CKSUMTYPE_HMAC_SHA1_96_AES128,
                                asReqbytes, ckey, KeyUsage.KU_AS_REQ);
                        enc_outPAs.add(new PAData(Krb5.PA_REQ_ENC_PA_REP,
                                ckSum.asn1Encode()));
                        bFlags[Krb5.TKT_OPTS_ENC_PA_REP] = true;
                        break;
                    }
                }
            }

            TicketFlags tFlags = new TicketFlags(bFlags);
            EncTicketPart enc = new EncTicketPart(
                    tFlags,
                    key,
                    body.cname,
                    new TransitedEncoding(1, new byte[0]),
                    timeAfter(0),
                    from,
                    till, rtime,
                    body.addresses,
                    null);
            Ticket t = new Ticket(
                    service,
                    new EncryptedData(skey, enc.asn1Encode(), KeyUsage.KU_TICKET)
            );
            EncASRepPart enc_part = new EncASRepPart(
                    key,
                    new LastReq(new LastReqEntry[]{
                        new LastReqEntry(0, timeAfter(-10))
                    }),
                    body.getNonce(),    // TODO: detect replay?
                    timeAfter(3600 * 24),
                    // Next 5 and last MUST be same with ticket
                    tFlags,
                    timeAfter(0),
                    from,
                    till, rtime,
                    service,
                    body.addresses,
                    enc_outPAs.toArray(new PAData[enc_outPAs.size()])
                    );
            EncryptedData edata = new EncryptedData(ckey, enc_part.asn1Encode(),
                    KeyUsage.KU_ENC_AS_REP_PART);
            ASRep asRep = new ASRep(
                    outPAs.toArray(new PAData[outPAs.size()]),
                    body.cname,
                    t,
                    edata);

            System.out.println("     Return " + asRep.cname
                    + " ticket for " + asRep.ticket.sname + ", flags "
                    + tFlags);

            DerOutputStream out = new DerOutputStream();
            out.write(DerValue.createTag(DerValue.TAG_APPLICATION,
                    true, (byte)Krb5.KRB_AS_REP), asRep.asn1Encode());
            byte[] result = out.toByteArray();

            // Added feature:
            // Write the current issuing TGT into a ccache file specified
            // by the system property below.
            String ccache = System.getProperty("test.kdc.save.ccache");
            if (ccache != null) {
                asRep.encKDCRepPart = enc_part;
                sun.security.krb5.internal.ccache.Credentials credentials =
                    new sun.security.krb5.internal.ccache.Credentials(asRep);
                CredentialsCache cache =
                    CredentialsCache.create(asReq.reqBody.cname, ccache);
                if (cache == null) {
                   throw new IOException("Unable to create the cache file " +
                                         ccache);
                }
                cache.update(credentials);
                cache.save();
            }

            return result;
        } catch (KrbException ke) {
            ke.printStackTrace(System.out);
            KRBError kerr = ke.getError();
            KDCReqBody body = asReq.reqBody;
            System.out.println("     Error " + ke.returnCode()
                    + " " +ke.returnCodeMessage());
            byte[] eData = null;
            if (kerr == null) {
                if (ke.returnCode() == Krb5.KDC_ERR_PREAUTH_REQUIRED ||
                        ke.returnCode() == Krb5.KDC_ERR_PREAUTH_FAILED) {
                    outPAs.add(new PAData(Krb5.PA_ENC_TIMESTAMP, new byte[0]));
                }
                if (outPAs.size() > 0) {
                    DerOutputStream bytes = new DerOutputStream();
                    for (PAData p: outPAs) {
                        bytes.write(p.asn1Encode());
                    }
                    DerOutputStream temp = new DerOutputStream();
                    temp.write(DerValue.tag_Sequence, bytes);
                    eData = temp.toByteArray();
                }
                kerr = new KRBError(null, null, null,
                        timeAfter(0),
                        0,
                        ke.returnCode(),
                        body.cname,
                        service,
                        KrbException.errorMessage(ke.returnCode()),
                        eData);
            }
            return kerr.asn1Encode();
        }
    }

    private int[] filterSupported(int[] input) {
        int count = 0;
        for (int i = 0; i < input.length; i++) {
            if (!EType.isSupported(input[i])) {
                continue;
            }
            if (SUPPORTED_ETYPES != null) {
                boolean supported = false;
                for (String se : SUPPORTED_ETYPES.split(",")) {
                    if (Config.getType(se) == input[i]) {
                        supported = true;
                        break;
                    }
                }
                if (!supported) {
                    continue;
                }
            }
            if (count != i) {
                input[count] = input[i];
            }
            count++;
        }
        if (count != input.length) {
            input = Arrays.copyOf(input, count);
        }
        return input;
    }

    /**
     * Generates a line for a KDC to put inside [realms] of krb5.conf
     * @return REALM.NAME = { kdc = host:port etc }
     */
    private String realmLine() {
        StringBuilder sb = new StringBuilder();
        sb.append(realm).append(" = {\n    kdc = ")
                .append(kdc).append(':').append(port).append('\n');
        for (String s: conf) {
            sb.append("    ").append(s).append('\n');
        }
        return sb.append("}\n").toString();
    }

    /**
     * Start the KDC service. This server listens on both UDP and TCP using
     * the same port number. It uses three threads to deal with requests.
     * They can be set to daemon threads if requested.
     * @param port the port number to listen to. If zero, a random available
     *  port no less than 8000 will be chosen and used.
     * @param asDaemon true if the KDC threads should be daemons
     * @throws java.io.IOException for any communication error
     */
    protected void startServer(int port, boolean asDaemon) throws IOException {
        if (nativeKdc != null) {
            startNativeServer(port, asDaemon);
        } else {
            startJavaServer(port, asDaemon);
        }
    }

    private void startNativeServer(int port, boolean asDaemon) throws IOException {
        nativeKdc.prepare();
        nativeKdc.init();
        kdcProc = nativeKdc.kdc();
    }

    private void startJavaServer(int port, boolean asDaemon) throws IOException {
        if (port > 0) {
            u1 = new DatagramSocket(port, InetAddress.getByName("127.0.0.1"));
            t1 = new ServerSocket(port);
        } else {
            while (true) {
                // Try to find a port number that's both TCP and UDP free
                try {
                    port = 8000 + new java.util.Random().nextInt(10000);
                    u1 = null;
                    u1 = new DatagramSocket(port, InetAddress.getByName("127.0.0.1"));
                    t1 = new ServerSocket(port);
                    break;
                } catch (Exception e) {
                    if (u1 != null) u1.close();
                }
            }
        }
        final DatagramSocket udp = u1;
        final ServerSocket tcp = t1;
        System.out.println("Start KDC on " + port);

        this.port = port;

        // The UDP consumer
        thread1 = new Thread() {
            public void run() {
                udpConsumerReady = true;
                while (true) {
                    try {
                        byte[] inbuf = new byte[8192];
                        DatagramPacket p = new DatagramPacket(inbuf, inbuf.length);
                        udp.receive(p);
                        System.out.println("-----------------------------------------------");
                        System.out.println(">>>>> UDP packet received");
                        q.put(new Job(processMessage(Arrays.copyOf(inbuf, p.getLength())), udp, p));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        thread1.setDaemon(asDaemon);
        thread1.start();

        // The TCP consumer
        thread2 = new Thread() {
            public void run() {
                tcpConsumerReady = true;
                while (true) {
                    try {
                        Socket socket = tcp.accept();
                        System.out.println("-----------------------------------------------");
                        System.out.println(">>>>> TCP connection established");
                        DataInputStream in = new DataInputStream(socket.getInputStream());
                        DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                        int len = in.readInt();
                        if (len > 65535) {
                            throw new Exception("Huge request not supported");
                        }
                        byte[] token = new byte[len];
                        in.readFully(token);
                        q.put(new Job(processMessage(token), socket, out));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        thread2.setDaemon(asDaemon);
        thread2.start();

        // The dispatcher
        thread3 = new Thread() {
            public void run() {
                dispatcherReady = true;
                while (true) {
                    try {
                        q.take().send();
                    } catch (Exception e) {
                    }
                }
            }
        };
        thread3.setDaemon(true);
        thread3.start();

        // wait for the KDC is ready
        try {
            while (!isReady()) {
                Thread.sleep(100);
            }
        } catch(InterruptedException e) {
            throw new IOException(e);
        }
    }

    public void kinit(String user, String ccache) throws Exception {
        if (user.indexOf('@') < 0) {
            user = user + "@" + realm;
        }
        if (nativeKdc != null) {
            nativeKdc.kinit(user, ccache);
        } else {
            Context.fromUserPass(user, passwords.get(user), false)
                    .ccache(ccache);
        }
    }

    boolean isReady() {
        return udpConsumerReady && tcpConsumerReady && dispatcherReady;
    }

    public void terminate() {
        if (nativeKdc != null) {
            System.out.println("Killing kdc...");
            kdcProc.destroyForcibly();
            System.out.println("Done");
        } else {
            try {
                thread1.stop();
                thread2.stop();
                thread3.stop();
                u1.close();
                t1.close();
            } catch (Exception e) {
                // OK
            }
        }
    }

    public static KDC startKDC(final String host, final String krbConfFileName,
            final String realm, final Map<String, String> principals,
            final String ktab, final KtabMode mode) {

        KDC kdc;
        try {
            kdc = KDC.create(realm, host, 0, true);
            kdc.setOption(KDC.Option.PREAUTH_REQUIRED, Boolean.FALSE);
            if (krbConfFileName != null) {
                KDC.saveConfig(krbConfFileName, kdc);
            }

            // Add principals
            if (principals != null) {
                principals.forEach((name, password) -> {
                    if (password == null || password.isEmpty()) {
                        System.out.println(String.format(
                                "KDC:add a principal '%s' with a random " +
                                        "password", name));
                        kdc.addPrincipalRandKey(name);
                    } else {
                        System.out.println(String.format(
                                "KDC:add a principal '%s' with '%s' password",
                                name, password));
                        kdc.addPrincipal(name, password.toCharArray());
                    }
                });
            }

            // Create or append keys to existing keytab file
            if (ktab != null) {
                File ktabFile = new File(ktab);
                switch(mode) {
                    case APPEND:
                        if (ktabFile.exists()) {
                            System.out.println(String.format(
                                    "KDC:append keys to an exising keytab "
                                    + "file %s", ktab));
                            kdc.appendKtab(ktab);
                        } else {
                            System.out.println(String.format(
                                    "KDC:create a new keytab file %s", ktab));
                            kdc.writeKtab(ktab);
                        }
                        break;
                    case EXISTING:
                        System.out.println(String.format(
                                "KDC:use an existing keytab file %s", ktab));
                        break;
                    default:
                        throw new RuntimeException(String.format(
                                "KDC:unsupported keytab mode: %s", mode));
                }
            }

            System.out.println(String.format(
                    "KDC: started on %s:%s with '%s' realm",
                    host, kdc.getPort(), realm));
        } catch (Exception e) {
            throw new RuntimeException("KDC: unexpected exception", e);
        }

        return kdc;
    }

    /**
     * Helper class to encapsulate a job in a KDC.
     */
    private static class Job {
        byte[] token;           // The received request at creation time and
                                // the response at send time
        Socket s;               // The TCP socket from where the request comes
        DataOutputStream out;   // The OutputStream of the TCP socket
        DatagramSocket s2;      // The UDP socket from where the request comes
        DatagramPacket dp;      // The incoming UDP datagram packet
        boolean useTCP;         // Whether TCP or UDP is used

        // Creates a job object for TCP
        Job(byte[] token, Socket s, DataOutputStream out) {
            useTCP = true;
            this.token = token;
            this.s = s;
            this.out = out;
        }

        // Creates a job object for UDP
        Job(byte[] token, DatagramSocket s2, DatagramPacket dp) {
            useTCP = false;
            this.token = token;
            this.s2 = s2;
            this.dp = dp;
        }

        // Sends the output back to the client
        void send() {
            try {
                if (useTCP) {
                    System.out.println(">>>>> TCP request honored");
                    out.writeInt(token.length);
                    out.write(token);
                    s.close();
                } else {
                    System.out.println(">>>>> UDP request honored");
                    s2.send(new DatagramPacket(token, token.length, dp.getAddress(), dp.getPort()));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * A native KDC using the binaries in nativePath. Attention:
     * this is using binaries, not an existing KDC instance.
     * An implementation of this takes care of configuration,
     * principal db managing and KDC startup.
     */
    static abstract class NativeKdc {

        protected Map<String,String> env;
        protected String nativePath;
        protected String base;
        protected String realm;
        protected int port;

        NativeKdc(String nativePath, KDC kdc) {
            if (kdc.port == 0) {
                kdc.port = 8000 + new java.util.Random().nextInt(10000);
            }
            this.nativePath = nativePath;
            this.realm = kdc.realm;
            this.port = kdc.port;
            this.base = Paths.get("" + port).toAbsolutePath().toString();
        }

        // Add a new principal
        abstract void addPrincipal(String user, String pass);
        // Add a keytab entry
        abstract void ktadd(String user, String ktab);
        // Initialize KDC
        abstract void init();
        // Start kdc
        abstract Process kdc();
        // Configuration
        abstract void prepare();
        // Fill ccache
        abstract void kinit(String user, String ccache);

        static NativeKdc get(KDC kdc) {
            String prop = System.getProperty("native.kdc.path");
            if (prop == null) {
                return null;
            } else if (Files.exists(Paths.get(prop, "sbin/krb5kdc"))) {
                return new MIT(true, prop, kdc);
            } else if (Files.exists(Paths.get(prop, "kdc/krb5kdc"))) {
                return new MIT(false, prop, kdc);
            } else if (Files.exists(Paths.get(prop, "libexec/kdc"))) {
                return new Heimdal(prop, kdc);
            } else {
                throw new IllegalArgumentException("Strange " + prop);
            }
        }

        Process run(boolean wait, String... cmd) {
            try {
                System.out.println("Running " + cmd2str(env, cmd));
                ProcessBuilder pb = new ProcessBuilder();
                pb.inheritIO();
                pb.environment().putAll(env);
                Process p = pb.command(cmd).start();
                if (wait) {
                    if (p.waitFor() < 0) {
                        throw new RuntimeException("exit code is not null");
                    }
                    return null;
                } else {
                    return p;
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        private String cmd2str(Map<String,String> env, String... cmd) {
            return env.entrySet().stream().map(e -> e.getKey()+"="+e.getValue())
                    .collect(Collectors.joining(" ")) + " " +
                    Stream.of(cmd).collect(Collectors.joining(" "));
        }
    }

    // Heimdal KDC. Build your own and run "make install" to nativePath.
    static class Heimdal extends NativeKdc {

        Heimdal(String nativePath, KDC kdc) {
            super(nativePath, kdc);
            this.env = Map.of(
                    "KRB5_CONFIG", base + "/krb5.conf",
                    "KRB5_TRACE", "/dev/stderr",
                    Platform.sharedLibraryPathVariableName(), nativePath + "/lib");
        }

        @Override
        public void addPrincipal(String user, String pass) {
            run(true, nativePath + "/bin/kadmin", "-l", "-r", realm,
                    "add", "-p", pass, "--use-defaults", user);
        }

        @Override
        public void ktadd(String user, String ktab) {
            run(true, nativePath + "/bin/kadmin", "-l", "-r", realm,
                    "ext_keytab", "-k", ktab, user);
        }

        @Override
        public void init() {
            run(true, nativePath + "/bin/kadmin",  "-l",  "-r", realm,
                    "init", "--realm-max-ticket-life=1day",
                    "--realm-max-renewable-life=1month", realm);
        }

        @Override
        public Process kdc() {
            return run(false, nativePath + "/libexec/kdc",
                    "--addresses=127.0.0.1", "-P", "" + port);
        }

        @Override
        public void prepare() {
            try {
                Files.createDirectory(Paths.get(base));
                Files.write(Paths.get(base + "/krb5.conf"), Arrays.asList(
                        "[libdefaults]",
                        "default_realm = " + realm,
                        "default_keytab_name = FILE:" + base + "/krb5.keytab",
                        "forwardable = true",
                        "dns_lookup_kdc = no",
                        "dns_lookup_realm = no",
                        "dns_canonicalize_hostname = false",
                        "\n[realms]",
                        realm + " = {",
                        "  kdc = localhost:" + port,
                        "}",
                        "\n[kdc]",
                        "db-dir = " + base,
                        "database = {",
                        "    label = {",
                        "        dbname = " + base + "/current-db",
                        "        realm = " + realm,
                        "        mkey_file = " + base + "/mkey.file",
                        "        acl_file = " + base + "/heimdal.acl",
                        "        log_file = " + base + "/current.log",
                        "    }",
                        "}",
                        SUPPORTED_ETYPES == null ? ""
                                : ("\n[kadmin]\ndefault_keys = "
                                + (SUPPORTED_ETYPES + ",")
                                        .replaceAll(",", ":pw-salt ")),
                        "\n[logging]",
                        "kdc = 0-/FILE:" + base + "/messages.log",
                        "krb5 = 0-/FILE:" + base + "/messages.log",
                        "default = 0-/FILE:" + base + "/messages.log"
                ));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        @Override
        void kinit(String user, String ccache) {
            String tmpName = base + "/" + user + "." +
                    System.identityHashCode(this) + ".keytab";
            ktadd(user, tmpName);
            run(true, nativePath + "/bin/kinit",
                    "-f", "-t", tmpName, "-c", ccache, user);
        }
    }

    // MIT krb5 KDC. Make your own exploded (install == false), or
    // "make install" into nativePath (install == true).
    static class MIT extends NativeKdc {

        private boolean install; // "make install" or "make"

        MIT(boolean install, String nativePath, KDC kdc) {
            super(nativePath, kdc);
            this.install = install;
            this.env = Map.of(
                    "KRB5_KDC_PROFILE", base + "/kdc.conf",
                    "KRB5_CONFIG", base + "/krb5.conf",
                    "KRB5_TRACE", "/dev/stderr",
                    Platform.sharedLibraryPathVariableName(), nativePath + "/lib");
        }

        @Override
        public void addPrincipal(String user, String pass) {
            run(true, nativePath +
                    (install ? "/sbin/" : "/kadmin/cli/") + "kadmin.local",
                    "-q", "addprinc -pw " + pass + " " + user);
        }

        @Override
        public void ktadd(String user, String ktab) {
            run(true, nativePath +
                    (install ? "/sbin/" : "/kadmin/cli/") + "kadmin.local",
                    "-q", "ktadd -k " + ktab + " -norandkey " + user);
        }

        @Override
        public void init() {
            run(true, nativePath +
                    (install ? "/sbin/" : "/kadmin/dbutil/") + "kdb5_util",
                    "create", "-s", "-W", "-P", "olala");
        }

        @Override
        public Process kdc() {
            return run(false, nativePath +
                    (install ? "/sbin/" : "/kdc/") + "krb5kdc",
                    "-n");
        }

        @Override
        public void prepare() {
            try {
                Files.createDirectory(Paths.get(base));
                Files.write(Paths.get(base + "/kdc.conf"), Arrays.asList(
                        "[kdcdefaults]",
                        "\n[realms]",
                        realm + "= {",
                        "  kdc_listen = " + this.port,
                        "  kdc_tcp_listen = " + this.port,
                        "  database_name = " + base + "/principal",
                        "  key_stash_file = " + base + "/.k5.ATHENA.MIT.EDU",
                        SUPPORTED_ETYPES == null ? ""
                                : ("  supported_enctypes = "
                                + (SUPPORTED_ETYPES + ",")
                                        .replaceAll(",", ":normal ")),
                        "}"
                ));
                Files.write(Paths.get(base + "/krb5.conf"), Arrays.asList(
                        "[libdefaults]",
                        "default_realm = " + realm,
                        "default_keytab_name = FILE:" + base + "/krb5.keytab",
                        "forwardable = true",
                        "dns_lookup_kdc = no",
                        "dns_lookup_realm = no",
                        "dns_canonicalize_hostname = false",
                        "\n[realms]",
                        realm + " = {",
                        "  kdc = localhost:" + port,
                        "}",
                        "\n[logging]",
                        "kdc = FILE:" + base + "/krb5kdc.log"
                ));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        @Override
        void kinit(String user, String ccache) {
            String tmpName = base + "/" + user + "." +
                    System.identityHashCode(this) + ".keytab";
            ktadd(user, tmpName);
            run(true, nativePath +
                    (install ? "/bin/" : "/clients/kinit/") + "kinit",
                    "-f", "-t", tmpName, "-c", ccache, user);
        }
    }

    // Calling private methods thru reflections
    private static final Field getEType;
    private static final Constructor<EncryptedData> ctorEncryptedData;
    private static final Method stringToKey;
    private static final Field getAddlTkt;

    static {
        try {
            ctorEncryptedData = EncryptedData.class.getDeclaredConstructor(DerValue.class);
            ctorEncryptedData.setAccessible(true);
            getEType = KDCReqBody.class.getDeclaredField("eType");
            getEType.setAccessible(true);
            stringToKey = EncryptionKey.class.getDeclaredMethod(
                    "stringToKey",
                    char[].class, String.class, byte[].class, Integer.TYPE);
            stringToKey.setAccessible(true);
            getAddlTkt = KDCReqBody.class.getDeclaredField("additionalTickets");
            getAddlTkt.setAccessible(true);
        } catch (NoSuchFieldException nsfe) {
            throw new AssertionError(nsfe);
        } catch (NoSuchMethodException nsme) {
            throw new AssertionError(nsme);
        }
    }
    private EncryptedData newEncryptedData(DerValue der) {
        try {
            return ctorEncryptedData.newInstance(der);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }
    private static int[] KDCReqBodyDotEType(KDCReqBody body) {
        try {
            return (int[]) getEType.get(body);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }
    private static byte[] EncryptionKeyDotStringToKey(char[] password, String salt,
            byte[] s2kparams, int keyType) throws KrbCryptoException {
        try {
            return (byte[])stringToKey.invoke(
                    null, password, salt, s2kparams, keyType);
        } catch (InvocationTargetException ex) {
            throw (KrbCryptoException)ex.getCause();
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }
    private static Ticket KDCReqBodyDotFirstAdditionalTicket(KDCReqBody body) {
        try {
            return ((Ticket[])getAddlTkt.get(body))[0];
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }
}
