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

package sun.security.provider;

import java.io.*;
import java.lang.reflect.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import javax.security.auth.Subject;
import javax.security.auth.x500.X500Principal;
import java.net.SocketPermission;
import java.net.NetPermission;
import java.util.concurrent.ConcurrentHashMap;
import jdk.internal.access.JavaSecurityAccess;
import jdk.internal.access.SharedSecrets;
import jdk.internal.util.StaticProperty;
import sun.nio.fs.DefaultFileSystemProvider;
import sun.security.util.*;
import sun.net.www.ParseUtil;

import static java.nio.charset.StandardCharsets.UTF_8;
import static jdk.internal.access.JavaSecurityAccess.ProtectionDomainCache;

/**
 * This class represents a default Policy implementation for the
 * "JavaPolicy" type.
 *
 * <p> This object stores the policy for the entire Java runtime,
 * and is the amalgamation of multiple static policy
 * configurations that resides in files.
 * The algorithm for locating the policy file(s) and reading their
 * information into this <code>Policy</code> object is:
 *
 * <ol>
 * <li>
 *   Read in and load the default policy file named
 *   &lt;JAVA_HOME&gt;/lib/security/default.policy. &lt;JAVA_HOME&gt; refers
 *   to the value of the java.home system property, and specifies the directory
 *   where the JRE is installed. This policy file grants permissions to the
 *   modules loaded by the platform class loader. If the default policy file
 *   cannot be loaded, a fatal InternalError is thrown as these permissions
 *   are needed in order for the runtime to operate correctly.
 * <li>
 *   Loop through the <code>java.security.Security</code> properties,
 *   and <i>policy.url.1</i>, <i>policy.url.2</i>, ...,
 *   <i>policy.url.X</i>".  These properties are set
 *   in the Java security properties file, which is located in the file named
 *   &lt;JAVA_HOME&gt;/conf/security/java.security.
 *   Each property value specifies a <code>URL</code> pointing to a
 *   policy file to be loaded.  Read in and load each policy.
 *
 *   If none of these could be loaded, use a builtin static policy
 *   equivalent to the conf/security/java.policy file.
 *
 * <li>
 *   The <code>java.lang.System</code> property <i>java.security.policy</i>
 *   may also be set to a <code>URL</code> pointing to another policy file
 *   (which is the case when a user uses the -D switch at runtime).
 *   If this property is defined, and its use is allowed by the
 *   security property file (the Security property,
 *   <i>policy.allowSystemProperty</i> is set to <i>true</i>),
 *   also load that policy.
 *
 *   If the <i>java.security.policy</i> property is defined using
 *   "==" (rather than "="), then load the specified policy file and ignore
 *   all other configured policies. Note, that the default.policy file is
 *   also loaded, as specified in the first step of the algorithm above.
 *   If the specified policy file cannot be loaded, use a builtin static policy
 *   equivalent to the default conf/security/java.policy file.
 * </ol>
 *
 * Each policy file consists of one or more grant entries, each of
 * which consists of a number of permission entries.
 *
 * <pre>
 *   grant signedBy "<b>alias</b>", codeBase "<b>URL</b>",
 *         principal <b>principalClass</b> "<b>principalName</b>",
 *         principal <b>principalClass</b> "<b>principalName</b>",
 *         ... {
 *
 *     permission <b>Type</b> "<b>name</b> "<b>action</b>",
 *         signedBy "<b>alias</b>";
 *     permission <b>Type</b> "<b>name</b> "<b>action</b>",
 *         signedBy "<b>alias</b>";
 *     ....
 *   };
 * </pre>
 *
 * All non-bold items above must appear as is (although case
 * doesn't matter and some are optional, as noted below).
 * principal entries are optional and need not be present.
 * Italicized items represent variable values.
 *
 * <p> A grant entry must begin with the word <code>grant</code>.
 * The <code>signedBy</code>,<code>codeBase</code> and <code>principal</code>
 * name/value pairs are optional.
 * If they are not present, then any signer (including unsigned code)
 * will match, and any codeBase will match.
 * Note that the <i>principalClass</i>
 * may be set to the wildcard value, *, which allows it to match
 * any <code>Principal</code> class.  In addition, the <i>principalName</i>
 * may also be set to the wildcard value, *, allowing it to match
 * any <code>Principal</code> name.  When setting the <i>principalName</i>
 * to the *, do not surround the * with quotes.
 *
 * <p> A permission entry must begin with the word <code>permission</code>.
 * The word <code><i>Type</i></code> in the template above is
 * a specific permission type, such as <code>java.io.FilePermission</code>
 * or <code>java.lang.RuntimePermission</code>.
 *
 * <p> The "<i>action</i>" is required for
 * many permission types, such as <code>java.io.FilePermission</code>
 * (where it specifies what type of file access that is permitted).
 * It is not required for categories such as
 * <code>java.lang.RuntimePermission</code>
 * where it is not necessary - you either have the
 * permission specified by the <code>"<i>name</i>"</code>
 * value following the type name or you don't.
 *
 * <p> The <code>signedBy</code> name/value pair for a permission entry
 * is optional. If present, it indicates a signed permission. That is,
 * the permission class itself must be signed by the given alias in
 * order for it to be granted. For example,
 * suppose you have the following grant entry:
 *
 * <pre>
 *   grant principal foo.com.Principal "Duke" {
 *     permission Foo "foobar", signedBy "FooSoft";
 *   }
 * </pre>
 *
 * <p> Then this permission of type <i>Foo</i> is granted if the
 * <code>Foo.class</code> permission has been signed by the
 * "FooSoft" alias, or if XXX <code>Foo.class</code> is a
 * system class (i.e., is found on the CLASSPATH).
 *
 * <p> Items that appear in an entry must appear in the specified order
 * (<code>permission</code>, <i>Type</i>, "<i>name</i>", and
 * "<i>action</i>"). An entry is terminated with a semicolon.
 *
 * <p> Case is unimportant for the identifiers (<code>permission</code>,
 * <code>signedBy</code>, <code>codeBase</code>, etc.) but is
 * significant for the <i>Type</i>
 * or for any string that is passed in as a value.
 *
 * <p> An example of two entries in a policy configuration file is
 * <pre>
 *   // if the code is comes from "foo.com" and is running as "Duke",
 *   // grant it read/write to all files in /tmp.
 *
 *   grant codeBase "foo.com", principal foo.com.Principal "Duke" {
 *              permission java.io.FilePermission "/tmp/*", "read,write";
 *   };
 *
 *   // grant any code running as "Duke" permission to read
 *   // the "java.vendor" Property.
 *
 *   grant principal foo.com.Principal "Duke" {
 *         permission java.util.PropertyPermission "java.vendor";
 *
 *
 * </pre>
 *  This Policy implementation supports special handling of any
 *  permission that contains the string, "<b>${{self}}</b>", as part of
 *  its target name.  When such a permission is evaluated
 *  (such as during a security check), <b>${{self}}</b> is replaced
 *  with one or more Principal class/name pairs.  The exact
 *  replacement performed depends upon the contents of the
 *  grant clause to which the permission belongs.
 * <p>
 *
 *  If the grant clause does not contain any principal information,
 *  the permission will be ignored (permissions containing
 *  <b>${{self}}</b> in their target names are only valid in the context
 *  of a principal-based grant clause).  For example, BarPermission
 *  will always be ignored in the following grant clause:
 *
 * <pre>
 *    grant codebase "www.foo.com", signedby "duke" {
 *      permission BarPermission "... ${{self}} ...";
 *    };
 * </pre>
 *
 *  If the grant clause contains principal information, <b>${{self}}</b>
 *  will be replaced with that same principal information.
 *  For example, <b>${{self}}</b> in BarPermission will be replaced by
 *  <b>javax.security.auth.x500.X500Principal "cn=Duke"</b>
 *  in the following grant clause:
 *
 *  <pre>
 *    grant principal javax.security.auth.x500.X500Principal "cn=Duke" {
 *      permission BarPermission "... ${{self}} ...";
 *    };
 *  </pre>
 *
 *  If there is a comma-separated list of principals in the grant
 *  clause, then <b>${{self}}</b> will be replaced by the same
 *  comma-separated list or principals.
 *  In the case where both the principal class and name are
 *  wildcarded in the grant clause, <b>${{self}}</b> is replaced
 *  with all the principals associated with the <code>Subject</code>
 *  in the current <code>AccessControlContext</code>.
 *
 * <p> For PrivateCredentialPermissions, you can also use "<b>self</b>"
 * instead of "<b>${{self}}</b>". However the use of "<b>self</b>" is
 * deprecated in favour of "<b>${{self}}</b>".
 *
 * @see java.security.CodeSource
 * @see java.security.Permissions
 * @see java.security.ProtectionDomain
 */
@SuppressWarnings("removal")
public class PolicyFile extends java.security.Policy {

    private static final Debug debug = Debug.getInstance("policy");

    private static final String SELF = "${{self}}";
    private static final String X500PRINCIPAL =
                        "javax.security.auth.x500.X500Principal";
    private static final String POLICY = "java.security.policy";
    private static final String POLICY_URL = "policy.url.";

    private static final int DEFAULT_CACHE_SIZE = 1;

    // contains the policy grant entries, PD cache, and alias mapping
    // can be updated if refresh() is called
    private volatile PolicyInfo policyInfo;

    private boolean expandProperties = true;
    private boolean allowSystemProperties = true;
    private boolean notUtf8 = false;
    private URL url;

    // for use with the reflection API
    private static final Class<?>[] PARAMS0 = { };
    private static final Class<?>[] PARAMS1 = { String.class };
    private static final Class<?>[] PARAMS2 = { String.class, String.class };

    /**
     * When a policy file has a syntax error, the exception code may generate
     * another permission check and this can cause the policy file to be parsed
     * repeatedly, leading to a StackOverflowError or ClassCircularityError.
     * To avoid this, this set is populated with policy files that have been
     * previously parsed and have syntax errors, so that they can be
     * subsequently ignored.
     */
    private static Set<URL> badPolicyURLs =
        Collections.newSetFromMap(new ConcurrentHashMap<URL,Boolean>());

    /**
     * Use the platform's default file system to avoid recursive initialization
     * issues when the VM is configured to use a custom file system provider.
     */
    private static final java.nio.file.FileSystem builtInFS =
        DefaultFileSystemProvider.theFileSystem();

    /**
     * Initializes the Policy object and reads the default policy
     * configuration file(s) into the Policy object.
     */
    public PolicyFile() {
        init((URL)null);
    }

    /**
     * Initializes the Policy object and reads the default policy
     * from the specified URL only.
     */
    public PolicyFile(URL url) {
        this.url = url;
        init(url);
    }

    /**
     * Initializes the Policy object and reads the default policy
     * configuration file(s) into the Policy object.
     *
     * See the class description for details on the algorithm used to
     * initialize the Policy object.
     */
    private void init(URL url) {
        // Properties are set once for each init(); ignore changes
        // between diff invocations of initPolicyFile(policy, url, info).
        String numCacheStr =
          AccessController.doPrivileged(new PrivilegedAction<>() {
            @Override
            public String run() {
                expandProperties = "true".equalsIgnoreCase
                    (Security.getProperty("policy.expandProperties"));
                allowSystemProperties = "true".equalsIgnoreCase
                    (Security.getProperty("policy.allowSystemProperty"));
                notUtf8 = "false".equalsIgnoreCase
                    (System.getProperty("sun.security.policy.utf8"));
                return System.getProperty("sun.security.policy.numcaches");
            }});

        int numCaches;
        if (numCacheStr != null) {
            try {
                numCaches = Integer.parseInt(numCacheStr);
            } catch (NumberFormatException e) {
                numCaches = DEFAULT_CACHE_SIZE;
            }
        } else {
            numCaches = DEFAULT_CACHE_SIZE;
        }
        PolicyInfo newInfo = new PolicyInfo(numCaches);
        initPolicyFile(newInfo, url);
        policyInfo = newInfo;
    }

    private void initPolicyFile(final PolicyInfo newInfo, final URL url) {

        // always load default.policy
        AccessController.doPrivileged(new PrivilegedAction<>() {
            @Override
            public Void run() {
                initDefaultPolicy(newInfo);
                return null;
            }
        });

        if (url != null) {

            /**
             * If the caller specified a URL via Policy.getInstance,
             * we only read from default.policy and that URL.
             */

            if (debug != null) {
                debug.println("reading " + url);
            }
            AccessController.doPrivileged(new PrivilegedAction<>() {
                @Override
                public Void run() {
                    if (init(url, newInfo) == false) {
                        // use static policy if all else fails
                        initStaticPolicy(newInfo);
                    }
                    return null;
                }
            });

        } else {

            /**
             * Caller did not specify URL via Policy.getInstance.
             * Read from URLs listed in the java.security properties file.
             */

            boolean loaded_one = initPolicyFile(POLICY, POLICY_URL, newInfo);
            // To maintain strict backward compatibility
            // we load the static policy only if POLICY load failed
            if (!loaded_one) {
                // use static policy if all else fails
                initStaticPolicy(newInfo);
            }
        }
    }

    private boolean initPolicyFile(final String propname, final String urlname,
                                   final PolicyInfo newInfo) {
        boolean loadedPolicy =
            AccessController.doPrivileged(new PrivilegedAction<>() {
            @Override
            public Boolean run() {
                boolean loaded_policy = false;

                if (allowSystemProperties) {
                    String extra_policy = System.getProperty(propname);
                    if (extra_policy != null) {
                        boolean overrideAll = false;
                        if (extra_policy.startsWith("=")) {
                            overrideAll = true;
                            extra_policy = extra_policy.substring(1);
                        }
                        try {
                            extra_policy =
                                PropertyExpander.expand(extra_policy);
                            URL policyURL;

                            File policyFile = new File(extra_policy);
                            if (policyFile.exists()) {
                                policyURL = ParseUtil.fileToEncodedURL
                                    (new File(policyFile.getCanonicalPath()));
                            } else {
                                policyURL = new URL(extra_policy);
                            }
                            if (debug != null) {
                                debug.println("reading "+policyURL);
                            }
                            if (init(policyURL, newInfo)) {
                                loaded_policy = true;
                            }
                        } catch (Exception e) {
                            // ignore.
                            if (debug != null) {
                                debug.println("caught exception: "+e);
                            }
                        }
                        if (overrideAll) {
                            if (debug != null) {
                                debug.println("overriding other policies!");
                            }
                            return Boolean.valueOf(loaded_policy);
                        }
                    }
                }

                int n = 1;
                String policy_uri;

                while ((policy_uri = Security.getProperty(urlname+n)) != null) {
                    try {
                        URL policy_url = null;
                        String expanded_uri = PropertyExpander.expand
                                (policy_uri).replace(File.separatorChar, '/');

                        if (policy_uri.startsWith("file:${java.home}/") ||
                            policy_uri.startsWith("file:${user.home}/")) {

                            // this special case accommodates
                            // the situation java.home/user.home
                            // expand to a single slash, resulting in
                            // a file://foo URI
                            policy_url = new File
                                (expanded_uri.substring(5)).toURI().toURL();
                        } else {
                            policy_url = new URI(expanded_uri).toURL();
                        }

                        if (debug != null) {
                            debug.println("reading " + policy_url);
                        }
                        if (init(policy_url, newInfo)) {
                            loaded_policy = true;
                        }
                    } catch (Exception e) {
                        if (debug != null) {
                            debug.println(
                                "Debug info only. Error reading policy " +e);
                            e.printStackTrace();
                        }
                        // ignore that policy
                    }
                    n++;
                }
                return Boolean.valueOf(loaded_policy);
            }
        });

        return loadedPolicy;
    }

    private void initDefaultPolicy(PolicyInfo newInfo) {
        Path defaultPolicy = builtInFS.getPath(StaticProperty.javaHome(),
                                     "lib",
                                     "security",
                                     "default.policy");
        if (debug != null) {
            debug.println("reading " + defaultPolicy);
        }
        try (BufferedReader br = Files.newBufferedReader(defaultPolicy)) {

            PolicyParser pp = new PolicyParser(expandProperties);
            pp.read(br);

            Enumeration<PolicyParser.GrantEntry> enum_ = pp.grantElements();
            while (enum_.hasMoreElements()) {
                PolicyParser.GrantEntry ge = enum_.nextElement();
                addGrantEntry(ge, null, newInfo);
            }
        } catch (Exception e) {
            throw new InternalError("Failed to load default.policy", e);
        }
    }

    /**
     * Reads a policy configuration into the Policy object using a
     * Reader object.
     */
    private boolean init(URL policy, PolicyInfo newInfo) {

        // skip parsing policy file if it has been previously parsed and
        // has syntax errors
        if (badPolicyURLs.contains(policy)) {
            if (debug != null) {
                debug.println("skipping bad policy file: " + policy);
            }
            return false;
        }

        try (InputStreamReader isr =
                 getInputStreamReader(PolicyUtil.getInputStream(policy))) {

            PolicyParser pp = new PolicyParser(expandProperties);
            pp.read(isr);

            KeyStore keyStore = null;
            try {
                keyStore = PolicyUtil.getKeyStore
                                (policy,
                                pp.getKeyStoreUrl(),
                                pp.getKeyStoreType(),
                                pp.getKeyStoreProvider(),
                                pp.getStorePassURL(),
                                debug);
            } catch (Exception e) {
                // ignore, treat it like we have no keystore
                if (debug != null) {
                    debug.println("Debug info only. Ignoring exception.");
                    e.printStackTrace();
                }
            }

            Enumeration<PolicyParser.GrantEntry> enum_ = pp.grantElements();
            while (enum_.hasMoreElements()) {
                PolicyParser.GrantEntry ge = enum_.nextElement();
                addGrantEntry(ge, keyStore, newInfo);
            }
            return true;
        } catch (PolicyParser.ParsingException pe) {
            // record bad policy file to avoid later reparsing it
            badPolicyURLs.add(policy);
            Object[] source = {policy, pe.getNonlocalizedMessage()};
            System.err.println(LocalizedMessage.getNonlocalized
                (POLICY + ".error.parsing.policy.message", source));
            if (debug != null) {
                pe.printStackTrace();
            }
        } catch (Exception e) {
            if (debug != null) {
                debug.println("error parsing "+policy);
                debug.println(e.toString());
                e.printStackTrace();
            }
        }

        return false;
    }

    private InputStreamReader getInputStreamReader(InputStream is) {
        /*
         * Read in policy using UTF-8 by default.
         *
         * Check non-standard system property to see if the default encoding
         * should be used instead.
         */
        return (notUtf8)
            ? new InputStreamReader(is)
            : new InputStreamReader(is, UTF_8);
    }

    private void initStaticPolicy(final PolicyInfo newInfo) {
        if (debug != null) {
            debug.println("Initializing with static permissions");
        }
        AccessController.doPrivileged(new PrivilegedAction<>() {
            @Override
            public Void run() {
                PolicyEntry pe = new PolicyEntry(new CodeSource(null,
                    (Certificate[]) null));
                pe.add(SecurityConstants.LOCAL_LISTEN_PERMISSION);
                pe.add(new PropertyPermission("java.version",
                    SecurityConstants.PROPERTY_READ_ACTION));
                pe.add(new PropertyPermission("java.vendor",
                    SecurityConstants.PROPERTY_READ_ACTION));
                pe.add(new PropertyPermission("java.vendor.url",
                    SecurityConstants.PROPERTY_READ_ACTION));
                pe.add(new PropertyPermission("java.class.version",
                    SecurityConstants.PROPERTY_READ_ACTION));
                pe.add(new PropertyPermission("os.name",
                    SecurityConstants.PROPERTY_READ_ACTION));
                pe.add(new PropertyPermission("os.version",
                    SecurityConstants.PROPERTY_READ_ACTION));
                pe.add(new PropertyPermission("os.arch",
                    SecurityConstants.PROPERTY_READ_ACTION));
                pe.add(new PropertyPermission("file.separator",
                    SecurityConstants.PROPERTY_READ_ACTION));
                pe.add(new PropertyPermission("path.separator",
                    SecurityConstants.PROPERTY_READ_ACTION));
                pe.add(new PropertyPermission("line.separator",
                    SecurityConstants.PROPERTY_READ_ACTION));
                pe.add(new PropertyPermission
                                ("java.specification.version",
                                    SecurityConstants.PROPERTY_READ_ACTION));
                pe.add(new PropertyPermission
                                ("java.specification.vendor",
                                    SecurityConstants.PROPERTY_READ_ACTION));
                pe.add(new PropertyPermission
                                ("java.specification.name",
                                    SecurityConstants.PROPERTY_READ_ACTION));
                pe.add(new PropertyPermission
                                ("java.vm.specification.version",
                                    SecurityConstants.PROPERTY_READ_ACTION));
                pe.add(new PropertyPermission
                                ("java.vm.specification.vendor",
                                    SecurityConstants.PROPERTY_READ_ACTION));
                pe.add(new PropertyPermission
                                ("java.vm.specification.name",
                                    SecurityConstants.PROPERTY_READ_ACTION));
                pe.add(new PropertyPermission("java.vm.version",
                    SecurityConstants.PROPERTY_READ_ACTION));
                pe.add(new PropertyPermission("java.vm.vendor",
                    SecurityConstants.PROPERTY_READ_ACTION));
                pe.add(new PropertyPermission("java.vm.name",
                    SecurityConstants.PROPERTY_READ_ACTION));

                // No need to sync because noone has access to newInfo yet
                newInfo.policyEntries.add(pe);

                return null;
            }
        });
    }

    /**
     * Given a GrantEntry, create a codeSource.
     *
     * @return null if signedBy alias is not recognized
     */
    private CodeSource getCodeSource(PolicyParser.GrantEntry ge, KeyStore keyStore,
        PolicyInfo newInfo) throws java.net.MalformedURLException
    {
        Certificate[] certs = null;
        if (ge.signedBy != null) {
            certs = getCertificates(keyStore, ge.signedBy, newInfo);
            if (certs == null) {
                // we don't have a key for this alias,
                // just return
                if (debug != null) {
                    debug.println("  -- No certs for alias '" +
                                       ge.signedBy + "' - ignoring entry");
                }
                return null;
            }
        }

        URL location;

        if (ge.codeBase != null)
            location = new URL(ge.codeBase);
        else
            location = null;

        return (canonicalizeCodebase(new CodeSource(location, certs),false));
    }

    /**
     * Add one policy entry to the list.
     */
    private void addGrantEntry(PolicyParser.GrantEntry ge,
                               KeyStore keyStore, PolicyInfo newInfo) {

        if (debug != null) {
            debug.println("Adding policy entry: ");
            debug.println("  signedBy " + ge.signedBy);
            debug.println("  codeBase " + ge.codeBase);
            if (ge.principals != null) {
                for (PolicyParser.PrincipalEntry pppe : ge.principals) {
                    debug.println("  " + pppe.toString());
                }
            }
        }

        try {
            CodeSource codesource = getCodeSource(ge, keyStore, newInfo);
            // skip if signedBy alias was unknown...
            if (codesource == null) return;

            // perform keystore alias principal replacement.
            // for example, if alias resolves to X509 certificate,
            // replace principal with:  <X500Principal class>  <SubjectDN>
            // -- skip if alias is unknown
            if (replacePrincipals(ge.principals, keyStore) == false)
                return;
            PolicyEntry entry = new PolicyEntry(codesource, ge.principals);
            Enumeration<PolicyParser.PermissionEntry> enum_ =
                                                ge.permissionElements();
            while (enum_.hasMoreElements()) {
                PolicyParser.PermissionEntry pe = enum_.nextElement();

                try {
                    // perform ${{ ... }} expansions within permission name
                    expandPermissionName(pe, keyStore);

                    // XXX special case PrivateCredentialPermission-SELF
                    Permission perm;
                    if (pe.permission.equals
                        ("javax.security.auth.PrivateCredentialPermission") &&
                        pe.name.endsWith(" self")) {
                        pe.name = pe.name.substring(0, pe.name.indexOf("self"))
                                + SELF;
                    }
                    // check for self
                    if (pe.name != null && pe.name.indexOf(SELF) != -1) {
                        // Create a "SelfPermission" , it could be an
                        // an unresolved permission which will be resolved
                        // when implies is called
                        // Add it to entry
                        Certificate[] certs;
                        if (pe.signedBy != null) {
                            certs = getCertificates(keyStore,
                                                    pe.signedBy,
                                                    newInfo);
                        } else {
                            certs = null;
                        }
                        perm = new SelfPermission(pe.permission,
                                                  pe.name,
                                                  pe.action,
                                                  certs);
                    } else {
                        perm = getInstance(pe.permission,
                                           pe.name,
                                           pe.action);
                    }
                    entry.add(perm);
                    if (debug != null) {
                        debug.println("  "+perm);
                    }
                } catch (ClassNotFoundException cnfe) {
                    Certificate[] certs;
                    if (pe.signedBy != null) {
                        certs = getCertificates(keyStore,
                                                pe.signedBy,
                                                newInfo);
                    } else {
                        certs = null;
                    }

                    // only add if we had no signer or we had
                    // a signer and found the keys for it.
                    if (certs != null || pe.signedBy == null) {
                        Permission perm = new UnresolvedPermission(
                                                  pe.permission,
                                                  pe.name,
                                                  pe.action,
                                                  certs);
                        entry.add(perm);
                        if (debug != null) {
                            debug.println("  "+perm);
                        }
                    }
                } catch (java.lang.reflect.InvocationTargetException ite) {
                    Object[] source = {pe.permission,
                                       ite.getCause().toString()};
                    System.err.println(
                        LocalizedMessage.getNonlocalized(
                            POLICY + ".error.adding.Permission.perm.message",
                            source));
                } catch (Exception e) {
                    Object[] source = {pe.permission,
                                       e.toString()};
                    System.err.println(
                        LocalizedMessage.getNonlocalized(
                            POLICY + ".error.adding.Permission.perm.message",
                            source));
                }
            }

            // No need to sync because noone has access to newInfo yet
            newInfo.policyEntries.add(entry);
        } catch (Exception e) {
            Object[] source = {e.toString()};
            System.err.println(
                LocalizedMessage.getNonlocalized(
                    POLICY + ".error.adding.Entry.message",
                    source));
        }
        if (debug != null)
            debug.println();
    }

    /**
     * Returns a new Permission object of the given Type. The Permission is
     * created by getting the
     * Class object using the <code>Class.forName</code> method, and using
     * the reflection API to invoke the (String name, String actions)
     * constructor on the
     * object.
     *
     * @param type the type of Permission being created.
     * @param name the name of the Permission being created.
     * @param actions the actions of the Permission being created.
     *
     * @exception  ClassNotFoundException  if the particular Permission
     *             class could not be found.
     *
     * @exception  IllegalAccessException  if the class or initializer is
     *               not accessible.
     *
     * @exception  InstantiationException  if getInstance tries to
     *               instantiate an abstract class or an interface, or if the
     *               instantiation fails for some other reason.
     *
     * @exception  NoSuchMethodException if the (String, String) constructor
     *               is not found.
     *
     * @exception  InvocationTargetException if the underlying Permission
     *               constructor throws an exception.
     *
     */

    private static final Permission getInstance(String type,
                                    String name,
                                    String actions)
        throws ClassNotFoundException,
               InstantiationException,
               IllegalAccessException,
               NoSuchMethodException,
               InvocationTargetException
    {
        Class<?> pc = Class.forName(type, false, null);
        Permission answer = getKnownPermission(pc, name, actions);
        if (answer != null) {
            return answer;
        }
        if (!Permission.class.isAssignableFrom(pc)) {
            // not the right subtype
            throw new ClassCastException(type + " is not a Permission");
        }

        if (name == null && actions == null) {
            try {
                Constructor<?> c = pc.getConstructor(PARAMS0);
                return (Permission) c.newInstance(new Object[] {});
            } catch (NoSuchMethodException ne) {
                try {
                    Constructor<?> c = pc.getConstructor(PARAMS1);
                    return (Permission) c.newInstance(
                              new Object[] { name});
                } catch (NoSuchMethodException ne1 ) {
                    Constructor<?> c = pc.getConstructor(PARAMS2);
                    return (Permission) c.newInstance(
                        new Object[] { name, actions });
                }
            }
        } else {
            if (name != null && actions == null) {
                try {
                    Constructor<?> c = pc.getConstructor(PARAMS1);
                    return (Permission) c.newInstance(new Object[] { name});
                } catch (NoSuchMethodException ne) {
                    Constructor<?> c = pc.getConstructor(PARAMS2);
                    return (Permission) c.newInstance(
                          new Object[] { name, actions });
                }
            } else {
                Constructor<?> c = pc.getConstructor(PARAMS2);
                return (Permission) c.newInstance(
                      new Object[] { name, actions });
             }
        }
    }

    /**
     * Creates one of the well-known permissions in the java.base module
     * directly instead of via reflection. Keep list short to not penalize
     * permissions from other modules.
     */
    private static Permission getKnownPermission(Class<?> claz, String name,
                                                 String actions) {
        if (claz.equals(FilePermission.class)) {
            return new FilePermission(name, actions);
        } else if (claz.equals(SocketPermission.class)) {
            return new SocketPermission(name, actions);
        } else if (claz.equals(RuntimePermission.class)) {
            return new RuntimePermission(name, actions);
        } else if (claz.equals(PropertyPermission.class)) {
            return new PropertyPermission(name, actions);
        } else if (claz.equals(NetPermission.class)) {
            return new NetPermission(name, actions);
        } else if (claz.equals(AllPermission.class)) {
            return SecurityConstants.ALL_PERMISSION;
        } else if (claz.equals(SecurityPermission.class)) {
            return new SecurityPermission(name, actions);
        } else {
            return null;
        }
    }

    /**
     * Creates one of the well-known principals in the java.base module
     * directly instead of via reflection. Keep list short to not penalize
     * principals from other modules.
     */
    private static Principal getKnownPrincipal(Class<?> claz, String name) {
        if (claz.equals(X500Principal.class)) {
            return new X500Principal(name);
        } else {
            return null;
        }
    }

    /**
     * Fetch all certs associated with this alias.
     */
    private Certificate[] getCertificates
                (KeyStore keyStore, String aliases, PolicyInfo newInfo) {

        List<Certificate> vcerts = null;

        StringTokenizer st = new StringTokenizer(aliases, ",");
        int n = 0;

        while (st.hasMoreTokens()) {
            String alias = st.nextToken().trim();
            n++;
            Certificate cert = null;
            // See if this alias's cert has already been cached
            synchronized (newInfo.aliasMapping) {
                cert = (Certificate)newInfo.aliasMapping.get(alias);

                if (cert == null && keyStore != null) {

                    try {
                        cert = keyStore.getCertificate(alias);
                    } catch (KeyStoreException kse) {
                        // never happens, because keystore has already been loaded
                        // when we call this
                    }
                    if (cert != null) {
                        newInfo.aliasMapping.put(alias, cert);
                        newInfo.aliasMapping.put(cert, alias);
                    }
                }
            }

            if (cert != null) {
                if (vcerts == null)
                    vcerts = new ArrayList<>();
                vcerts.add(cert);
            }
        }

        // make sure n == vcerts.size, since we are doing a logical *and*
        if (vcerts != null && n == vcerts.size()) {
            Certificate[] certs = new Certificate[vcerts.size()];
            vcerts.toArray(certs);
            return certs;
        } else {
            return null;
        }
    }

    /**
     * Refreshes the policy object by re-reading all the policy files.
     */
    @Override public void refresh() {
        init(url);
    }

    /**
     * Evaluates the global policy for the permissions granted to
     * the ProtectionDomain and tests whether the permission is
     * granted.
     *
     * @param pd the ProtectionDomain to test
     * @param p the Permission object to be tested for implication.
     *
     * @return true if "permission" is a proper subset of a permission
     * granted to this ProtectionDomain.
     *
     * @see java.security.ProtectionDomain
     */
    @Override
    public boolean implies(ProtectionDomain pd, Permission p) {
        ProtectionDomainCache pdMap = policyInfo.getPdMapping();
        PermissionCollection pc = pdMap.get(pd);

        if (pc != null) {
            return pc.implies(p);
        }

        pc = getPermissions(pd);
        if (pc == null) {
            return false;
        }

        // cache mapping of protection domain to its PermissionCollection
        pdMap.put(pd, pc);
        return pc.implies(p);
    }

    /**
     * Examines this <code>Policy</code> and returns the permissions granted
     * to the specified <code>ProtectionDomain</code>.  This includes
     * the permissions currently associated with the domain as well
     * as the policy permissions granted to the domain's
     * CodeSource, ClassLoader, and Principals.
     *
     * <p> Note that this <code>Policy</code> implementation has
     * special handling for PrivateCredentialPermissions.
     * When this method encounters a <code>PrivateCredentialPermission</code>
     * which specifies "self" as the <code>Principal</code> class and name,
     * it does not add that <code>Permission</code> to the returned
     * <code>PermissionCollection</code>.  Instead, it builds
     * a new <code>PrivateCredentialPermission</code>
     * for each <code>Principal</code> associated with the provided
     * <code>Subject</code>.  Each new <code>PrivateCredentialPermission</code>
     * contains the same Credential class as specified in the
     * originally granted permission, as well as the Class and name
     * for the respective <code>Principal</code>.
     *
     * @param domain the Permissions granted to this
     *          <code>ProtectionDomain</code> are returned.
     *
     * @return the Permissions granted to the provided
     *          <code>ProtectionDomain</code>.
     */
    @Override
    public PermissionCollection getPermissions(ProtectionDomain domain) {
        Permissions perms = new Permissions();

        if (domain == null)
           return perms;

        // first get policy perms
        getPermissions(perms, domain);

        // add static perms
        //      - adding static perms after policy perms is necessary
        //        to avoid a regression for 4301064
        PermissionCollection pc = domain.getPermissions();
        if (pc != null) {
            synchronized (pc) {
                Enumeration<Permission> e = pc.elements();
                while (e.hasMoreElements()) {
                    perms.add(FilePermCompat.newPermPlusAltPath(e.nextElement()));
                }
            }
        }

        return perms;
    }

    /**
     * Examines this Policy and creates a PermissionCollection object with
     * the set of permissions for the specified CodeSource.
     *
     * @param codesource the CodeSource associated with the caller.
     * This encapsulates the original location of the code (where the code
     * came from) and the public key(s) of its signer.
     *
     * @return the set of permissions according to the policy.
     */
    @Override
    public PermissionCollection getPermissions(CodeSource codesource) {
        return getPermissions(new Permissions(), codesource);
    }

    /**
     * Examines the global policy and returns the provided Permissions
     * object with additional permissions granted to the specified
     * ProtectionDomain.
     *
     * @param perms the Permissions to populate
     * @param pd the ProtectionDomain associated with the caller.
     *
     * @return the set of Permissions according to the policy.
     */
    private PermissionCollection getPermissions(Permissions perms,
                                        ProtectionDomain pd ) {
        if (debug != null) {
            debug.println("getPermissions:\n\t" + printPD(pd));
        }

        final CodeSource cs = pd.getCodeSource();
        if (cs == null)
            return perms;

        CodeSource canonCodeSource = AccessController.doPrivileged(
            new java.security.PrivilegedAction<>(){
                @Override
                public CodeSource run() {
                    return canonicalizeCodebase(cs, true);
                }
            });
        return getPermissions(perms, canonCodeSource, pd.getPrincipals());
    }

    /**
     * Examines the global policy and returns the provided Permissions
     * object with additional permissions granted to the specified
     * CodeSource.
     *
     * @param perms the permissions to populate
     * @param cs the codesource associated with the caller.
     * This encapsulates the original location of the code (where the code
     * came from) and the public key(s) of its signer.
     *
     * @return the set of permissions according to the policy.
     */
    private PermissionCollection getPermissions(Permissions perms,
                                                final CodeSource cs) {

        if (cs == null)
            return perms;

        CodeSource canonCodeSource = AccessController.doPrivileged(
            new PrivilegedAction<>(){
                @Override
                public CodeSource run() {
                    return canonicalizeCodebase(cs, true);
                }
            });

        return getPermissions(perms, canonCodeSource, null);
    }

    private Permissions getPermissions(Permissions perms,
                                       final CodeSource cs,
                                       Principal[] principals) {
        for (PolicyEntry entry : policyInfo.policyEntries) {
            addPermissions(perms, cs, principals, entry);
        }

        return perms;
    }

    private void addPermissions(Permissions perms,
        final CodeSource cs,
        Principal[] principals,
        final PolicyEntry entry) {

        if (debug != null) {
            debug.println("evaluate codesources:\n" +
                "\tPolicy CodeSource: " + entry.getCodeSource() + "\n" +
                "\tActive CodeSource: " + cs);
        }

        // check to see if the CodeSource implies
        Boolean imp = AccessController.doPrivileged
            (new PrivilegedAction<>() {
            @Override
            public Boolean run() {
                return entry.getCodeSource().implies(cs);
            }
        });
        if (!imp.booleanValue()) {
            if (debug != null) {
                debug.println("evaluation (codesource) failed");
            }

            // CodeSource does not imply - return and try next policy entry
            return;
        }

        // check to see if the Principals imply

        List<PolicyParser.PrincipalEntry> entryPs = entry.getPrincipals();
        if (debug != null) {
            List<PolicyParser.PrincipalEntry> accPs = new ArrayList<>();
            if (principals != null) {
                for (int i = 0; i < principals.length; i++) {
                    accPs.add(new PolicyParser.PrincipalEntry
                                        (principals[i].getClass().getName(),
                                        principals[i].getName()));
                }
            }
            debug.println("evaluate principals:\n" +
                "\tPolicy Principals: " + entryPs + "\n" +
                "\tActive Principals: " + accPs);
        }

        if (entryPs == null || entryPs.isEmpty()) {

            // policy entry has no principals -
            // add perms regardless of principals in current ACC

            addPerms(perms, principals, entry);
            if (debug != null) {
                debug.println("evaluation (codesource/principals) passed");
            }
            return;

        } else if (principals == null || principals.length == 0) {

            // current thread has no principals but this policy entry
            // has principals - perms are not added

            if (debug != null) {
                debug.println("evaluation (principals) failed");
            }
            return;
        }

        // current thread has principals and this policy entry
        // has principals.  see if policy entry principals match
        // principals in current ACC

        for (PolicyParser.PrincipalEntry pppe : entryPs) {

            // Check for wildcards
            if (pppe.isWildcardClass()) {
                // a wildcard class matches all principals in current ACC
                continue;
            }

            if (pppe.isWildcardName()) {
                // a wildcard name matches any principal with the same class
                if (wildcardPrincipalNameImplies(pppe.principalClass,
                                                 principals)) {
                    continue;
                }
                if (debug != null) {
                    debug.println("evaluation (principal name wildcard) failed");
                }
                // policy entry principal not in current ACC -
                // immediately return and go to next policy entry
                return;
            }

            Set<Principal> pSet = new HashSet<>(Arrays.asList(principals));
            Subject subject = new Subject(true, pSet,
                                          Collections.EMPTY_SET,
                                          Collections.EMPTY_SET);
            try {
                ClassLoader cl = Thread.currentThread().getContextClassLoader();
                Class<?> pClass = Class.forName(pppe.principalClass, false, cl);
                Principal p = getKnownPrincipal(pClass, pppe.principalName);
                if (p == null) {
                    if (!Principal.class.isAssignableFrom(pClass)) {
                        // not the right subtype
                        throw new ClassCastException(pppe.principalClass +
                                                     " is not a Principal");
                    }

                    Constructor<?> c = pClass.getConstructor(PARAMS1);
                    p = (Principal)c.newInstance(new Object[] {
                                                 pppe.principalName });

                }

                if (debug != null) {
                    debug.println("found Principal " + p.getClass().getName());
                }

                // check if the Principal implies the current
                // thread's principals
                if (!p.implies(subject)) {
                    if (debug != null) {
                        debug.println("evaluation (principal implies) failed");
                    }

                    // policy principal does not imply the current Subject -
                    // immediately return and go to next policy entry
                    return;
                }
            } catch (Exception e) {
                // fall back to default principal comparison.
                // see if policy entry principal is in current ACC

                if (debug != null) {
                    e.printStackTrace();
                }

                if (!pppe.implies(subject)) {
                    if (debug != null) {
                        debug.println("evaluation (default principal implies) failed");
                    }

                    // policy entry principal not in current ACC -
                    // immediately return and go to next policy entry
                    return;
                }
            }

            // either the principal information matched,
            // or the Principal.implies succeeded.
            // continue loop and test the next policy principal
        }

        // all policy entry principals were found in the current ACC -
        // grant the policy permissions

        if (debug != null) {
            debug.println("evaluation (codesource/principals) passed");
        }
        addPerms(perms, principals, entry);
    }

    /**
     * Returns true if the array of principals contains at least one
     * principal of the specified class.
     */
    private static boolean wildcardPrincipalNameImplies(String principalClass,
                                                        Principal[] principals)
    {
        for (Principal p : principals) {
            if (principalClass.equals(p.getClass().getName())) {
                return true;
            }
        }
        return false;
    }

    private void addPerms(Permissions perms,
                        Principal[] accPs,
                        PolicyEntry entry) {
        for (int i = 0; i < entry.permissions.size(); i++) {
            Permission p = entry.permissions.get(i);
            if (debug != null) {
                debug.println("  granting " + p);
            }

            if (p instanceof SelfPermission) {
                // handle "SELF" permissions
                expandSelf((SelfPermission)p,
                        entry.getPrincipals(),
                        accPs,
                        perms);
            } else {
                perms.add(FilePermCompat.newPermPlusAltPath(p));
            }
        }
    }

    /**
     * @param sp the SelfPermission that needs to be expanded.
     *
     * @param entryPs list of principals for the Policy entry.
     *
     * @param pdp Principal array from the current ProtectionDomain.
     *
     * @param perms the PermissionCollection where the individual
     *                  Permissions will be added after expansion.
     */

    private void expandSelf(SelfPermission sp,
                            List<PolicyParser.PrincipalEntry> entryPs,
                            Principal[] pdp,
                            Permissions perms) {

        if (entryPs == null || entryPs.isEmpty()) {
            // No principals in the grant to substitute
            if (debug != null) {
                debug.println("Ignoring permission "
                                + sp.getSelfType()
                                + " with target name ("
                                + sp.getSelfName() + ").  "
                                + "No Principal(s) specified "
                                + "in the grant clause.  "
                                + "SELF-based target names are "
                                + "only valid in the context "
                                + "of a Principal-based grant entry."
                             );
            }
            return;
        }
        int startIndex = 0;
        int v;
        StringBuilder sb = new StringBuilder();
        while ((v = sp.getSelfName().indexOf(SELF, startIndex)) != -1) {

            // add non-SELF string
            sb.append(sp.getSelfName().substring(startIndex, v));

            // expand SELF
            Iterator<PolicyParser.PrincipalEntry> pli = entryPs.iterator();
            while (pli.hasNext()) {
                PolicyParser.PrincipalEntry pppe = pli.next();
                String[][] principalInfo = getPrincipalInfo(pppe,pdp);
                for (int i = 0; i < principalInfo.length; i++) {
                    if (i != 0) {
                        sb.append(", ");
                    }
                    sb.append(principalInfo[i][0] + " " +
                        "\"" + principalInfo[i][1] + "\"");
                }
                if (pli.hasNext()) {
                    sb.append(", ");
                }
            }
            startIndex = v + SELF.length();
        }
        // add remaining string (might be the entire string)
        sb.append(sp.getSelfName().substring(startIndex));

        if (debug != null) {
            debug.println("  expanded:\n\t" + sp.getSelfName()
                        + "\n  into:\n\t" + sb.toString());
        }
        try {
            // first try to instantiate the permission
            perms.add(FilePermCompat.newPermPlusAltPath(getInstance(sp.getSelfType(),
                                  sb.toString(),
                                  sp.getSelfActions())));
        } catch (ClassNotFoundException cnfe) {
            // ok, the permission is not in the bootclasspath.
            // before we add an UnresolvedPermission, check to see
            // whether this perm already belongs to the collection.
            // if so, use that perm's ClassLoader to create a new
            // one.
            Class<?> pc = null;
            synchronized (perms) {
                Enumeration<Permission> e = perms.elements();
                while (e.hasMoreElements()) {
                    Permission pElement = e.nextElement();
                    if (pElement.getClass().getName().equals(sp.getSelfType())) {
                        pc = pElement.getClass();
                        break;
                    }
                }
            }
            if (pc == null) {
                // create an UnresolvedPermission
                perms.add(new UnresolvedPermission(sp.getSelfType(),
                                                        sb.toString(),
                                                        sp.getSelfActions(),
                                                        sp.getCerts()));
            } else {
                try {
                    // we found an instantiated permission.
                    // use its class loader to instantiate a new permission.
                    Constructor<?> c;
                    // name parameter can not be null
                    if (sp.getSelfActions() == null) {
                        try {
                            c = pc.getConstructor(PARAMS1);
                            perms.add((Permission)c.newInstance
                                 (new Object[] {sb.toString()}));
                        } catch (NoSuchMethodException ne) {
                            c = pc.getConstructor(PARAMS2);
                            perms.add((Permission)c.newInstance
                                 (new Object[] {sb.toString(),
                                                sp.getSelfActions() }));
                        }
                    } else {
                        c = pc.getConstructor(PARAMS2);
                        perms.add((Permission)c.newInstance
                           (new Object[] {sb.toString(),
                                          sp.getSelfActions()}));
                    }
                } catch (Exception nme) {
                    if (debug != null) {
                        debug.println("self entry expansion " +
                        " instantiation failed: "
                        +  nme.toString());
                    }
                }
            }
        } catch (Exception e) {
            if (debug != null) {
                debug.println(e.toString());
            }
        }
    }

    /**
     * return the principal class/name pair in the 2D array.
     * array[x][y]:     x corresponds to the array length.
     *                  if (y == 0), it's the principal class.
     *                  if (y == 1), it's the principal name.
     */
    private String[][] getPrincipalInfo
        (PolicyParser.PrincipalEntry pe, Principal[] pdp) {

        // there are 3 possibilities:
        // 1) the entry's Principal class and name are not wildcarded
        // 2) the entry's Principal name is wildcarded only
        // 3) the entry's Principal class and name are wildcarded

        if (!pe.isWildcardClass() && !pe.isWildcardName()) {

            // build an info array for the principal
            // from the Policy entry
            String[][] info = new String[1][2];
            info[0][0] = pe.principalClass;
            info[0][1] = pe.principalName;
            return info;

        } else if (!pe.isWildcardClass() && pe.isWildcardName()) {

            // build an info array for every principal
            // in the current domain which has a principal class
            // that is equal to policy entry principal class name
            List<Principal> plist = new ArrayList<>();
            for (int i = 0; i < pdp.length; i++) {
                if (pe.principalClass.equals(pdp[i].getClass().getName()))
                    plist.add(pdp[i]);
            }
            String[][] info = new String[plist.size()][2];
            int i = 0;
            for (Principal p : plist) {
                info[i][0] = p.getClass().getName();
                info[i][1] = p.getName();
                i++;
            }
            return info;

        } else {

            // build an info array for every
            // one of the current Domain's principals

            String[][] info = new String[pdp.length][2];

            for (int i = 0; i < pdp.length; i++) {
                info[i][0] = pdp[i].getClass().getName();
                info[i][1] = pdp[i].getName();
            }
            return info;
        }
    }

    /*
     * Returns the signer certificates from the list of certificates
     * associated with the given code source.
     *
     * The signer certificates are those certificates that were used
     * to verify signed code originating from the codesource location.
     *
     * This method assumes that in the given code source, each signer
     * certificate is followed by its supporting certificate chain
     * (which may be empty), and that the signer certificate and its
     * supporting certificate chain are ordered bottom-to-top
     * (i.e., with the signer certificate first and the (root) certificate
     * authority last).
     */
    protected Certificate[] getSignerCertificates(CodeSource cs) {
        Certificate[] certs = null;
        if ((certs = cs.getCertificates()) == null)
            return null;
        for (int i=0; i<certs.length; i++) {
            if (!(certs[i] instanceof X509Certificate))
                return cs.getCertificates();
        }

        // Do we have to do anything?
        int i = 0;
        int count = 0;
        while (i < certs.length) {
            count++;
            while (((i+1) < certs.length)
                   && ((X509Certificate)certs[i]).getIssuerX500Principal().equals(
                           ((X509Certificate)certs[i+1]).getSubjectX500Principal())) {
                i++;
            }
            i++;
        }
        if (count == certs.length)
            // Done
            return certs;

        List<Certificate> userCertList = new ArrayList<>();
        i = 0;
        while (i < certs.length) {
            userCertList.add(certs[i]);
            while (((i+1) < certs.length)
                   && ((X509Certificate)certs[i]).getIssuerX500Principal().equals(
                           ((X509Certificate)certs[i+1]).getSubjectX500Principal())) {
                i++;
            }
            i++;
        }
        Certificate[] userCerts = new Certificate[userCertList.size()];
        userCertList.toArray(userCerts);
        return userCerts;
    }

    private CodeSource canonicalizeCodebase(CodeSource cs,
                                            boolean extractSignerCerts) {

        String path = null;

        CodeSource canonCs = cs;
        URL u = cs.getLocation();
        if (u != null) {
            if (u.getProtocol().equals("jar")) {
                // unwrap url embedded inside jar url
                String spec = u.getFile();
                int separator = spec.indexOf("!/");
                if (separator != -1) {
                    try {
                        u = new URL(spec.substring(0, separator));
                    } catch (MalformedURLException e) {
                        // Fail silently. In this case, url stays what
                        // it was above
                    }
                }
            }
            if (u.getProtocol().equals("file")) {
                boolean isLocalFile = false;
                String host = u.getHost();
                isLocalFile = (host == null || host.isEmpty() ||
                    host.equals("~") || host.equalsIgnoreCase("localhost"));

                if (isLocalFile) {
                    path = u.getFile().replace('/', File.separatorChar);
                    path = ParseUtil.decode(path);
                }
            }
        }

        if (path != null) {
            try {
                URL csUrl = null;
                path = canonPath(path);
                csUrl = ParseUtil.fileToEncodedURL(new File(path));

                if (extractSignerCerts) {
                    canonCs = new CodeSource(csUrl,
                                             getSignerCertificates(cs));
                } else {
                    canonCs = new CodeSource(csUrl,
                                             cs.getCertificates());
                }
            } catch (IOException ioe) {
                // leave codesource as it is, unless we have to extract its
                // signer certificates
                if (extractSignerCerts) {
                    canonCs = new CodeSource(cs.getLocation(),
                                             getSignerCertificates(cs));
                }
            }
        } else {
            if (extractSignerCerts) {
                canonCs = new CodeSource(cs.getLocation(),
                                         getSignerCertificates(cs));
            }
        }
        return canonCs;
    }

    // Wrapper to return a canonical path that avoids calling getCanonicalPath()
    // with paths that are intended to match all entries in the directory
    private static String canonPath(String path) throws IOException {
        if (path.endsWith("*")) {
            path = path.substring(0, path.length()-1) + "-";
            path = new File(path).getCanonicalPath();
            return path.substring(0, path.length()-1) + "*";
        } else {
            return new File(path).getCanonicalPath();
        }
    }

    private String printPD(ProtectionDomain pd) {
        Principal[] principals = pd.getPrincipals();
        String pals = "<no principals>";
        if (principals != null && principals.length > 0) {
            StringBuilder palBuf = new StringBuilder("(principals ");
            for (int i = 0; i < principals.length; i++) {
                palBuf.append(principals[i].getClass().getName() +
                              " \"" + principals[i].getName() +
                              "\"");
                if (i < principals.length-1)
                    palBuf.append(", ");
                else
                    palBuf.append(")");
            }
            pals = palBuf.toString();
        }
        return "PD CodeSource: "
                + pd.getCodeSource()
                +"\n\t" + "PD ClassLoader: "
                + pd.getClassLoader()
                +"\n\t" + "PD Principals: "
                + pals;
    }

    /**
     * return true if no replacement was performed,
     * or if replacement succeeded.
     */
    private boolean replacePrincipals(
        List<PolicyParser.PrincipalEntry> principals, KeyStore keystore) {

        if (principals == null || principals.isEmpty() || keystore == null)
            return true;

        for (PolicyParser.PrincipalEntry pppe : principals) {
            if (pppe.isReplaceName()) {

                // perform replacement
                // (only X509 replacement is possible now)
                String name;
                if ((name = getDN(pppe.principalName, keystore)) == null) {
                    return false;
                }

                if (debug != null) {
                    debug.println("  Replacing \"" +
                        pppe.principalName +
                        "\" with " +
                        X500PRINCIPAL + "/\"" +
                        name +
                        "\"");
                }

                pppe.principalClass = X500PRINCIPAL;
                pppe.principalName = name;
            }
        }
        // return true if no replacement was performed,
        // or if replacement succeeded
        return true;
    }

    private void expandPermissionName(PolicyParser.PermissionEntry pe,
                                        KeyStore keystore) throws Exception {
        // short cut the common case
        if (pe.name == null || pe.name.indexOf("${{", 0) == -1) {
            return;
        }

        int startIndex = 0;
        int b, e;
        StringBuilder sb = new StringBuilder();
        while ((b = pe.name.indexOf("${{", startIndex)) != -1) {
            e = pe.name.indexOf("}}", b);
            if (e < 1) {
                break;
            }
            sb.append(pe.name.substring(startIndex, b));

            // get the value in ${{...}}
            String value = pe.name.substring(b+3, e);

            // parse up to the first ':'
            int colonIndex;
            String prefix = value;
            String suffix;
            if ((colonIndex = value.indexOf(':')) != -1) {
                prefix = value.substring(0, colonIndex);
            }

            // handle different prefix possibilities
            if (prefix.equalsIgnoreCase("self")) {
                // do nothing - handled later
                sb.append(pe.name.substring(b, e+2));
                startIndex = e+2;
                continue;
            } else if (prefix.equalsIgnoreCase("alias")) {
                // get the suffix and perform keystore alias replacement
                if (colonIndex == -1) {
                    Object[] source = {pe.name};
                    throw new Exception(
                        LocalizedMessage.getNonlocalized(
                            "alias.name.not.provided.pe.name.",
                            source));
                }
                suffix = value.substring(colonIndex+1);
                if ((suffix = getDN(suffix, keystore)) == null) {
                    Object[] source = {value.substring(colonIndex+1)};
                    throw new Exception(
                        LocalizedMessage.getNonlocalized(
                            "unable.to.perform.substitution.on.alias.suffix",
                            source));
                }

                sb.append(X500PRINCIPAL + " \"" + suffix + "\"");
                startIndex = e+2;
            } else {
                Object[] source = {prefix};
                throw new Exception(
                    LocalizedMessage.getNonlocalized(
                        "substitution.value.prefix.unsupported",
                        source));
            }
        }

        // copy the rest of the value
        sb.append(pe.name.substring(startIndex));

        // replace the name with expanded value
        if (debug != null) {
            debug.println("  Permission name expanded from:\n\t" +
                        pe.name + "\nto\n\t" + sb.toString());
        }
        pe.name = sb.toString();
    }

    private String getDN(String alias, KeyStore keystore) {
        Certificate cert = null;
        try {
            cert = keystore.getCertificate(alias);
        } catch (Exception e) {
            if (debug != null) {
                debug.println("  Error retrieving certificate for '" +
                                alias +
                                "': " +
                                e.toString());
            }
            return null;
        }

        if (cert == null || !(cert instanceof X509Certificate)) {
            if (debug != null) {
                debug.println("  -- No certificate for '" +
                                alias +
                                "' - ignoring entry");
            }
            return null;
        } else {
            X509Certificate x509Cert = (X509Certificate)cert;

            // 4702543:  X500 names with an EmailAddress
            // were encoded incorrectly.  create new
            // X500Principal name with correct encoding

            X500Principal p = new X500Principal
                (x509Cert.getSubjectX500Principal().toString());
            return p.getName();
        }
    }

    /**
     * Each entry in the policy configuration file is represented by a
     * PolicyEntry object.  <p>
     *
     * A PolicyEntry is a (CodeSource,Permission) pair.  The
     * CodeSource contains the (URL, PublicKey) that together identify
     * where the Java bytecodes come from and who (if anyone) signed
     * them.  The URL could refer to localhost.  The URL could also be
     * null, meaning that this policy entry is given to all comers, as
     * long as they match the signer field.  The signer could be null,
     * meaning the code is not signed. <p>
     *
     * The Permission contains the (Type, Name, Action) triplet. <p>
     *
     * For now, the Policy object retrieves the public key from the
     * X.509 certificate on disk that corresponds to the signedBy
     * alias specified in the Policy config file.  For reasons of
     * efficiency, the Policy object keeps a hashtable of certs already
     * read in.  This could be replaced by a secure internal key
     * store.
     *
     * <p>
     * For example, the entry
     * <pre>
     *          permission java.io.File "/tmp", "read,write",
     *          signedBy "Duke";
     * </pre>
     * is represented internally
     * <pre>
     *
     * FilePermission f = new FilePermission("/tmp", "read,write");
     * PublicKey p = publickeys.get("Duke");
     * URL u = InetAddress.getLocalHost();
     * CodeBase c = new CodeBase( p, u );
     * pe = new PolicyEntry(f, c);
     * </pre>
     *
     * @author Marianne Mueller
     * @author Roland Schemers
     * @see java.security.CodeSource
     * @see java.security.Policy
     * @see java.security.Permissions
     * @see java.security.ProtectionDomain
     */
    private static class PolicyEntry {

        private final CodeSource codesource;
        final List<Permission> permissions;
        private final List<PolicyParser.PrincipalEntry> principals;

        /**
         * Given a Permission and a CodeSource, create a policy entry.
         *
         * XXX Decide if/how to add validity fields and "purpose" fields to
         * XXX policy entries
         *
         * @param cs the CodeSource, which encapsulates the URL and the
         *        public key
         *        attributes from the policy config file. Validity checks
         *        are performed on the public key before PolicyEntry is
         *        called.
         *
         */
        PolicyEntry(CodeSource cs, List<PolicyParser.PrincipalEntry> principals)
        {
            this.codesource = cs;
            this.permissions = new ArrayList<Permission>();
            this.principals = principals; // can be null
        }

        PolicyEntry(CodeSource cs)
        {
            this(cs, null);
        }

        List<PolicyParser.PrincipalEntry> getPrincipals() {
            return principals; // can be null
        }

        /**
         * add a Permission object to this entry.
         * No need to sync add op because perms are added to entry only
         * while entry is being initialized
         */
        void add(Permission p) {
            permissions.add(p);
        }

        /**
         * Return the CodeSource for this policy entry
         */
        CodeSource getCodeSource() {
            return codesource;
        }

        @Override public String toString(){
            StringBuilder sb = new StringBuilder();
            sb.append(ResourcesMgr.getString("LPARAM"));
            sb.append(getCodeSource());
            sb.append("\n");
            for (int j = 0; j < permissions.size(); j++) {
                Permission p = permissions.get(j);
                sb.append(ResourcesMgr.getString("SPACE"));
                sb.append(ResourcesMgr.getString("SPACE"));
                sb.append(p);
                sb.append(ResourcesMgr.getString("NEWLINE"));
            }
            sb.append(ResourcesMgr.getString("RPARAM"));
            sb.append(ResourcesMgr.getString("NEWLINE"));
            return sb.toString();
        }
    }

    private static class SelfPermission extends Permission {

        @java.io.Serial
        private static final long serialVersionUID = -8315562579967246806L;

        /**
         * The class name of the Permission class that will be
         * created when this self permission is expanded .
         *
         * @serial
         */
        private String type;

        /**
         * The permission name.
         *
         * @serial
         */
        private String name;

        /**
         * The actions of the permission.
         *
         * @serial
         */
        private String actions;

        /**
         * The certs of the permission.
         *
         * @serial
         */
        private Certificate[] certs;

        /**
         * Creates a new SelfPermission containing the permission
         * information needed later to expand the self
         * @param type the class name of the Permission class that will be
         * created when this permission is expanded and if necessary resolved.
         * @param name the name of the permission.
         * @param actions the actions of the permission.
         * @param certs the certificates the permission's class was signed with.
         * This is a list of certificate chains, where each chain is composed of
         * a signer certificate and optionally its supporting certificate chain.
         * Each chain is ordered bottom-to-top (i.e., with the signer
         * certificate first and the (root) certificate authority last).
         */
        public SelfPermission(String type, String name, String actions,
                              Certificate[] certs)
        {
            super(type);
            if (type == null) {
                throw new NullPointerException
                    (LocalizedMessage.getNonlocalized("type.can.t.be.null"));
            }
            this.type = type;
            this.name = name;
            this.actions = actions;
            if (certs != null) {
                // Extract the signer certs from the list of certificates.
                for (int i=0; i<certs.length; i++) {
                    if (!(certs[i] instanceof X509Certificate)) {
                        // there is no concept of signer certs, so we store the
                        // entire cert array
                        this.certs = certs.clone();
                        break;
                    }
                }

                if (this.certs == null) {
                    // Go through the list of certs and see if all the certs are
                    // signer certs.
                    int i = 0;
                    int count = 0;
                    while (i < certs.length) {
                        count++;
                        while (((i+1) < certs.length) &&
                            ((X509Certificate)certs[i]).getIssuerX500Principal().equals(
                            ((X509Certificate)certs[i+1]).getSubjectX500Principal())) {
                            i++;
                        }
                        i++;
                    }
                    if (count == certs.length) {
                        // All the certs are signer certs, so we store the
                        // entire array
                        this.certs = certs.clone();
                    }

                    if (this.certs == null) {
                        // extract the signer certs
                        List<Certificate> signerCerts = new ArrayList<>();
                        i = 0;
                        while (i < certs.length) {
                            signerCerts.add(certs[i]);
                            while (((i+1) < certs.length) &&
                                ((X509Certificate)certs[i]).getIssuerX500Principal().equals(
                                ((X509Certificate)certs[i+1]).getSubjectX500Principal())) {
                                i++;
                            }
                            i++;
                        }
                        this.certs = new Certificate[signerCerts.size()];
                        signerCerts.toArray(this.certs);
                    }
                }
            }
        }

        /**
         * This method always returns false for SelfPermission permissions.
         * That is, an SelfPermission never considered to
         * imply another permission.
         *
         * @param p the permission to check against.
         *
         * @return false.
         */
        @Override public boolean implies(Permission p) {
            return false;
        }

        /**
         * Checks two SelfPermission objects for equality.
         *
         * Checks that <i>obj</i> is an SelfPermission, and has
         * the same type (class) name, permission name, actions, and
         * certificates as this object.
         *
         * @param obj the object we are testing for equality with this object.
         *
         * @return true if obj is an SelfPermission, and has the same
         * type (class) name, permission name, actions, and
         * certificates as this object.
         */
        @Override public boolean equals(Object obj) {
            if (obj == this)
                return true;

            if (! (obj instanceof SelfPermission))
                return false;
            SelfPermission that = (SelfPermission) obj;

            if (!(this.type.equals(that.type) &&
                this.name.equals(that.name) &&
                this.actions.equals(that.actions)))
                return false;

            if (this.certs.length != that.certs.length)
                return false;

            int i,j;
            boolean match;

            for (i = 0; i < this.certs.length; i++) {
                match = false;
                for (j = 0; j < that.certs.length; j++) {
                    if (this.certs[i].equals(that.certs[j])) {
                        match = true;
                        break;
                    }
                }
                if (!match) return false;
            }

            for (i = 0; i < that.certs.length; i++) {
                match = false;
                for (j = 0; j < this.certs.length; j++) {
                    if (that.certs[i].equals(this.certs[j])) {
                        match = true;
                        break;
                    }
                }
                if (!match) return false;
            }
            return true;
        }

        /**
         * Returns the hash code value for this object.
         *
         * @return a hash code value for this object.
         */
        @Override public int hashCode() {
            int hash = type.hashCode();
            if (name != null)
                hash ^= name.hashCode();
            if (actions != null)
                hash ^= actions.hashCode();
            return hash;
        }

        /**
         * Returns the canonical string representation of the actions,
         * which currently is the empty string "", since there are no actions
         * for an SelfPermission. That is, the actions for the
         * permission that will be created when this SelfPermission
         * is resolved may be non-null, but an SelfPermission
         * itself is never considered to have any actions.
         *
         * @return the empty string "".
         */
        @Override public String getActions() {
            return "";
        }

        public String getSelfType() {
            return type;
        }

        public String getSelfName() {
            return name;
        }

        public String getSelfActions() {
            return actions;
        }

        public Certificate[] getCerts() {
            return certs;
        }

        /**
         * Returns a string describing this SelfPermission.  The convention
         * is to specify the class name, the permission name, and the actions,
         * in the following format: '(unresolved "ClassName" "name" "actions")'.
         *
         * @return information about this SelfPermission.
         */
        @Override public String toString() {
            return "(SelfPermission " + type + " " + name + " " + actions + ")";
        }
    }

    /**
     * holds policy information that we need to synch on
     */
    private static class PolicyInfo {
        private static final boolean verbose = false;

        // Stores grant entries in the policy
        final List<PolicyEntry> policyEntries;

        // Maps aliases to certs
        final Map<Object, Object> aliasMapping;

        // Maps ProtectionDomain to PermissionCollection
        private final ProtectionDomainCache[] pdMapping;
        private java.util.Random random;

        PolicyInfo(int numCaches) {
            policyEntries = new ArrayList<>();
            aliasMapping = Collections.synchronizedMap(new HashMap<>(11));

            pdMapping = new ProtectionDomainCache[numCaches];
            JavaSecurityAccess jspda
                = SharedSecrets.getJavaSecurityAccess();
            for (int i = 0; i < numCaches; i++) {
                pdMapping[i] = jspda.getProtectionDomainCache();
            }
            if (numCaches > 1) {
                random = new java.util.Random();
            }
        }
        ProtectionDomainCache getPdMapping() {
            if (pdMapping.length == 1) {
                return pdMapping[0];
            } else {
                int i = java.lang.Math.abs(random.nextInt() % pdMapping.length);
                return pdMapping[i];
            }
        }
    }
}
