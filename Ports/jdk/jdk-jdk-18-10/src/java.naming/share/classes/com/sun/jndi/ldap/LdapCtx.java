/*
 * Copyright (c) 1999, 2021, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.jndi.ldap;

import javax.naming.*;
import javax.naming.directory.*;
import javax.naming.spi.*;
import javax.naming.event.*;
import javax.naming.ldap.*;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;
import java.util.Vector;
import java.util.Hashtable;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Enumeration;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import java.io.IOException;
import java.io.OutputStream;

import com.sun.jndi.toolkit.ctx.*;
import com.sun.jndi.toolkit.dir.HierMemDirCtx;
import com.sun.jndi.toolkit.dir.SearchFilter;
import com.sun.jndi.ldap.ext.StartTlsResponseImpl;

/**
 * The LDAP context implementation.
 *
 * Implementation is not thread-safe. Caller must sync as per JNDI spec.
 * Members that are used directly or indirectly by internal worker threads
 * (Connection, EventQueue, NamingEventNotifier) must be thread-safe.
 * Connection - calls LdapClient.processUnsolicited(), which in turn calls
 *   LdapCtx.convertControls() and LdapCtx.fireUnsolicited().
 *   convertControls() - no sync; reads envprops and 'this'
 *   fireUnsolicited() - sync on eventSupport for all references to 'unsolicited'
 *      (even those in other methods);  don't sync on LdapCtx in case caller
 *      is already sync'ing on it - this would prevent Unsol events from firing
 *      and the Connection thread to block (thus preventing any other data
 *      from being read from the connection)
 *      References to 'eventSupport' need not be sync'ed because these
 *      methods can only be called after eventSupport has been set first
 *      (via addNamingListener()).
 * EventQueue - no direct or indirect calls to LdapCtx
 * NamingEventNotifier - calls newInstance() to get instance for run() to use;
 *      no sync needed for methods invoked on new instance;
 *
 * LdapAttribute links to LdapCtx in order to process getAttributeDefinition()
 * and getAttributeSyntaxDefinition() calls. It invokes LdapCtx.getSchema(),
 * which uses schemaTrees (a Hashtable - already sync). Potential conflict
 * of duplicating construction of tree for same subschemasubentry
 * but no inconsistency problems.
 *
 * NamingEnumerations link to LdapCtx for the following:
 * 1. increment/decrement enum count so that ctx doesn't close the
 *    underlying connection
 * 2. LdapClient handle to get next batch of results
 * 3. Sets LdapCtx's response controls
 * 4. Process return code
 * 5. For narrowing response controls (using ctx's factories)
 * Since processing of NamingEnumeration by client is treated the same as method
 * invocation on LdapCtx, caller is responsible for locking.
 *
 * @author Vincent Ryan
 * @author Rosanna Lee
 */

public final class LdapCtx extends ComponentDirContext
    implements EventDirContext, LdapContext {

    /*
     * Used to store arguments to the search method.
     */
    static final class SearchArgs {
        Name name;
        String filter;
        SearchControls cons;
        String[] reqAttrs; // those attributes originally requested

        SearchArgs(Name name, String filter, SearchControls cons, String[] ra) {
            this.name = name;
            this.filter = filter;
            this.cons = cons;
            this.reqAttrs = ra;
        }
    }

    private static final boolean debug = false;

    private static final boolean HARD_CLOSE = true;
    private static final boolean SOFT_CLOSE = false;

    // -----------------  Constants  -----------------

    public static final int DEFAULT_PORT = 389;
    public static final int DEFAULT_SSL_PORT = 636;
    public static final String DEFAULT_HOST = "localhost";

    private static final boolean DEFAULT_DELETE_RDN = true;
    private static final boolean DEFAULT_TYPES_ONLY = false;
    private static final int DEFAULT_DEREF_ALIASES = 3; // always deref
    private static final int DEFAULT_LDAP_VERSION = LdapClient.LDAP_VERSION3_VERSION2;
    private static final int DEFAULT_BATCH_SIZE = 1;
    private static final int DEFAULT_REFERRAL_MODE = LdapClient.LDAP_REF_IGNORE;
    private static final char DEFAULT_REF_SEPARATOR = '#';

        // Used by LdapPoolManager
    static final String DEFAULT_SSL_FACTORY =
        "javax.net.ssl.SSLSocketFactory";       // use Sun's SSL
    private static final int DEFAULT_REFERRAL_LIMIT = 10;
    private static final String STARTTLS_REQ_OID = "1.3.6.1.4.1.1466.20037";

    // schema operational and user attributes
    private static final String[] SCHEMA_ATTRIBUTES =
        { "objectClasses", "attributeTypes", "matchingRules", "ldapSyntaxes" };

    // --------------- Environment property names ----------

    // LDAP protocol version: "2", "3"
    private static final String VERSION = "java.naming.ldap.version";

    // Binary-valued attributes. Space separated string of attribute names.
    private static final String BINARY_ATTRIBUTES =
                                        "java.naming.ldap.attributes.binary";

    // Delete old RDN during modifyDN: "true", "false"
    private static final String DELETE_RDN = "java.naming.ldap.deleteRDN";

    // De-reference aliases: "never", "searching", "finding", "always"
    private static final String DEREF_ALIASES = "java.naming.ldap.derefAliases";

    // Return only attribute types (no values)
    private static final String TYPES_ONLY = "java.naming.ldap.typesOnly";

    // Separator character for encoding Reference's RefAddrs; default is '#'
    private static final String REF_SEPARATOR = "java.naming.ldap.ref.separator";

    // Socket factory
    private static final String SOCKET_FACTORY = "java.naming.ldap.factory.socket";

    // Bind Controls (used by LdapReferralException)
    static final String BIND_CONTROLS = "java.naming.ldap.control.connect";

    private static final String REFERRAL_LIMIT =
        "java.naming.ldap.referral.limit";

    // trace BER (java.io.OutputStream)
    private static final String TRACE_BER = "com.sun.jndi.ldap.trace.ber";

    // Get around Netscape Schema Bugs
    private static final String NETSCAPE_SCHEMA_BUG =
        "com.sun.jndi.ldap.netscape.schemaBugs";
    // deprecated
    private static final String OLD_NETSCAPE_SCHEMA_BUG =
        "com.sun.naming.netscape.schemaBugs";   // for backward compatibility

    // Timeout for socket connect
    private static final String CONNECT_TIMEOUT =
        "com.sun.jndi.ldap.connect.timeout";

     // Timeout for reading responses
    private static final String READ_TIMEOUT =
        "com.sun.jndi.ldap.read.timeout";

    // Environment property for connection pooling
    private static final String ENABLE_POOL = "com.sun.jndi.ldap.connect.pool";

    // Environment property for the domain name (derived from this context's DN)
    private static final String DOMAIN_NAME = "com.sun.jndi.ldap.domainname";

    // Block until the first search reply is received
    private static final String WAIT_FOR_REPLY =
        "com.sun.jndi.ldap.search.waitForReply";

    // Size of the queue of unprocessed search replies
    private static final String REPLY_QUEUE_SIZE =
        "com.sun.jndi.ldap.search.replyQueueSize";

    // System and environment property name to control allowed list of
    // authentication mechanisms: "all" or "" or "mech1,mech2,...,mechN"
    //  "all": allow all mechanisms,
    //  "": allow none
    //  or comma separated list of allowed authentication mechanisms
    // Note: "none" or "anonymous" are always allowed.
    private static final String ALLOWED_MECHS_SP =
            "jdk.jndi.ldap.mechsAllowedToSendCredentials";

    // System property value
    private static final String ALLOWED_MECHS_SP_VALUE =
            getMechsAllowedToSendCredentials();

    // Set of authentication mechanisms allowed by the system property
    private static final Set<String> MECHS_ALLOWED_BY_SP =
            getMechsFromPropertyValue(ALLOWED_MECHS_SP_VALUE);

    // The message to use in NamingException if the transmission of plain credentials are not allowed
    private static final String UNSECURED_CRED_TRANSMIT_MSG =
                "Transmission of credentials over unsecured connection is not allowed";

    // ----------------- Fields that don't change -----------------------
    private static final NameParser parser = new LdapNameParser();

    // controls that Provider needs
    private static final ControlFactory myResponseControlFactory =
        new DefaultResponseControlFactory();
    private static final Control manageReferralControl =
        new ManageReferralControl(false);

    private static final HierMemDirCtx EMPTY_SCHEMA = new HierMemDirCtx();
    static {
        EMPTY_SCHEMA.setReadOnly(
            new SchemaViolationException("Cannot update schema object"));
    }

    // ------------ Package private instance variables ----------------
    // Cannot be private; used by enums

        // ------- Inherited by derived context instances

    int port_number;                    // port number of server
    String hostname = null;             // host name of server (no brackets
                                        //   for IPv6 literals)
    LdapClient clnt = null;             // connection handle
    Hashtable<String, java.lang.Object> envprops = null; // environment properties of context
    int handleReferrals = DEFAULT_REFERRAL_MODE; // how referral is handled
    boolean hasLdapsScheme = false;     // true if the context was created
                                        //  using an LDAPS URL.

        // ------- Not inherited by derived context instances

    String currentDN;                   // DN of this context
    Name currentParsedDN;               // DN of this context
    Vector<Control> respCtls = null;    // Response controls read
    Control[] reqCtls = null;           // Controls to be sent with each request
    // Used to track if context was seen to be secured with STARTTLS extended operation
    volatile boolean contextSeenStartTlsEnabled;

    // ------------- Private instance variables ------------------------

        // ------- Inherited by derived context instances

    private OutputStream trace = null;  // output stream for BER debug output
    private boolean netscapeSchemaBug = false;       // workaround
    private Control[] bindCtls = null;  // Controls to be sent with LDAP "bind"
    private int referralHopLimit = DEFAULT_REFERRAL_LIMIT;  // max referral
    private Hashtable<String, DirContext> schemaTrees = null; // schema root of this context
    private int batchSize = DEFAULT_BATCH_SIZE;      // batch size for search results
    private boolean deleteRDN = DEFAULT_DELETE_RDN;  // delete the old RDN when modifying DN
    private boolean typesOnly = DEFAULT_TYPES_ONLY;  // return attribute types (no values)
    private int derefAliases = DEFAULT_DEREF_ALIASES;// de-reference alias entries during searching
    private char addrEncodingSeparator = DEFAULT_REF_SEPARATOR;  // encoding RefAddr

    private Hashtable<String, Boolean> binaryAttrs = null; // attr values returned as byte[]
    private int connectTimeout = -1;         // no timeout value
    private int readTimeout = -1;            // no timeout value
    private boolean waitForReply = true;     // wait for search response
    private int replyQueueSize  = -1;        // unlimited queue size
    private boolean useSsl = false;          // true if SSL protocol is active
    private boolean useDefaultPortNumber = false; // no port number was supplied

        // ------- Not inherited by derived context instances

    // True if this context was created by another LdapCtx.
    private boolean parentIsLdapCtx = false; // see composeName()

    private int hopCount = 1;                // current referral hop count
    private String url = null;               // URL of context; see getURL()
    private EventSupport eventSupport;       // Event support helper for this ctx
    private boolean unsolicited = false;     // if there unsolicited listeners
    private boolean sharable = true;         // can share connection with other ctx

    // -------------- Constructors  -----------------------------------

    @SuppressWarnings("unchecked")
    public LdapCtx(String dn, String host, int port_number,
            Hashtable<?,?> props,
            boolean useSsl) throws NamingException {

        this.useSsl = this.hasLdapsScheme = useSsl;

        if (props != null) {
            envprops = (Hashtable<String, java.lang.Object>) props.clone();

            // SSL env prop overrides the useSsl argument
            if ("ssl".equals(envprops.get(Context.SECURITY_PROTOCOL))) {
                this.useSsl = true;
            }

            // %%% These are only examined when the context is created
            // %%% because they are only for debugging or workaround purposes.
            trace = (OutputStream)envprops.get(TRACE_BER);

            if (props.get(NETSCAPE_SCHEMA_BUG) != null ||
                props.get(OLD_NETSCAPE_SCHEMA_BUG) != null) {
                netscapeSchemaBug = true;
            }
        }

        currentDN = (dn != null) ? dn : "";
        currentParsedDN = parser.parse(currentDN);

        hostname = (host != null && host.length() > 0) ? host : DEFAULT_HOST;
        if (hostname.charAt(0) == '[') {
            hostname = hostname.substring(1, hostname.length() - 1);
        }

        if (port_number > 0) {
            this.port_number = port_number;
        } else {
            this.port_number = this.useSsl ? DEFAULT_SSL_PORT : DEFAULT_PORT;
            this.useDefaultPortNumber = true;
        }

        schemaTrees = new Hashtable<>(11, 0.75f);
        initEnv();
        try {
            connect(false);
        } catch (NamingException e) {
            try {
                close();
            } catch (Exception e2) {
                // Nothing
            }
            throw e;
        }
    }

    LdapCtx(LdapCtx existing, String newDN) throws NamingException {
        useSsl = existing.useSsl;
        hasLdapsScheme = existing.hasLdapsScheme;
        useDefaultPortNumber = existing.useDefaultPortNumber;

        hostname = existing.hostname;
        port_number = existing.port_number;
        currentDN = newDN;
        if (existing.currentDN == currentDN) {
            currentParsedDN = existing.currentParsedDN;
        } else {
            currentParsedDN = parser.parse(currentDN);
        }

        envprops = existing.envprops;
        schemaTrees = existing.schemaTrees;

        clnt = existing.clnt;
        clnt.incRefCount();

        parentIsLdapCtx = ((newDN == null || newDN.equals(existing.currentDN))
                           ? existing.parentIsLdapCtx
                           : true);

        // inherit these debugging/workaround flags
        trace = existing.trace;
        netscapeSchemaBug = existing.netscapeSchemaBug;

        initEnv();
    }

    public LdapContext newInstance(Control[] reqCtls) throws NamingException {

        LdapContext clone = new LdapCtx(this, currentDN);

        // Connection controls are inherited from environment

        // Set clone's request controls
        // setRequestControls() will clone reqCtls
        clone.setRequestControls(reqCtls);
        return clone;
    }

    // --------------- Namespace Updates ---------------------
    // -- bind/rebind/unbind
    // -- rename
    // -- createSubcontext/destroySubcontext

    protected void c_bind(Name name, Object obj, Continuation cont)
            throws NamingException {
        c_bind(name, obj, null, cont);
    }

    /*
     * attrs == null
     *      if obj is DirContext, attrs = obj.getAttributes()
     * if attrs == null && obj == null
     *      disallow (cannot determine objectclass to use)
     * if obj == null
     *      just create entry using attrs
     * else
     *      objAttrs = create attributes for representing obj
     *      attrs += objAttrs
     *      create entry using attrs
     */
    protected void c_bind(Name name, Object obj, Attributes attrs,
                          Continuation cont)
            throws NamingException {

        cont.setError(this, name);

        Attributes inputAttrs = attrs; // Attributes supplied by caller
        try {
            ensureOpen();

            if (obj == null) {
                if (attrs == null) {
                    throw new IllegalArgumentException(
                        "cannot bind null object with no attributes");
                }
            } else {
                attrs = Obj.determineBindAttrs(addrEncodingSeparator, obj, attrs,
                    false, name, this, envprops); // not cloned
            }

            String newDN = fullyQualifiedName(name);
            attrs = addRdnAttributes(newDN, attrs, inputAttrs != attrs);
            LdapEntry entry = new LdapEntry(newDN, attrs);

            LdapResult answer = clnt.add(entry, reqCtls);
            respCtls = answer.resControls; // retrieve response controls

            if (answer.status != LdapClient.LDAP_SUCCESS) {
                processReturnCode(answer, name);
            }

        } catch (LdapReferralException e) {
            if (handleReferrals == LdapClient.LDAP_REF_THROW)
                throw cont.fillInException(e);

            // process the referrals sequentially
            while (true) {

                LdapReferralContext refCtx =
                    (LdapReferralContext)e.getReferralContext(envprops, bindCtls);

                // repeat the original operation at the new context
                try {

                    refCtx.bind(name, obj, inputAttrs);
                    return;

                } catch (LdapReferralException re) {
                    e = re;
                    continue;

                } finally {
                    // Make sure we close referral context
                    refCtx.close();
                }
            }

        } catch (IOException e) {
            NamingException e2 = new CommunicationException(e.getMessage());
            e2.setRootCause(e);
            throw cont.fillInException(e2);

        } catch (NamingException e) {
            throw cont.fillInException(e);
        }
    }

    protected void c_rebind(Name name, Object obj, Continuation cont)
            throws NamingException {
        c_rebind(name, obj, null, cont);
    }


    /*
     * attrs == null
     *    if obj is DirContext, attrs = obj.getAttributes().
     * if attrs == null
     *    leave any existing attributes alone
     *    (set attrs = {objectclass=top} if object doesn't exist)
     * else
     *    replace all existing attributes with attrs
     * if obj == null
     *      just create entry using attrs
     * else
     *      objAttrs = create attributes for representing obj
     *      attrs += objAttrs
     *      create entry using attrs
     */
    protected void c_rebind(Name name, Object obj, Attributes attrs,
        Continuation cont) throws NamingException {

        cont.setError(this, name);

        Attributes inputAttrs = attrs;

        try {
            Attributes origAttrs = null;

            // Check if name is bound
            try {
                origAttrs = c_getAttributes(name, null, cont);
            } catch (NameNotFoundException e) {}

            // Name not bound, just add it
            if (origAttrs == null) {
                c_bind(name, obj, attrs, cont);
                return;
            }

            // there's an object there already, need to figure out
            // what to do about its attributes

            if (attrs == null && obj instanceof DirContext) {
                attrs = ((DirContext)obj).getAttributes("");
            }
            Attributes keepAttrs = (Attributes)origAttrs.clone();

            if (attrs == null) {
                // we're not changing any attrs, leave old attributes alone

                // Remove Java-related object classes from objectclass attribute
                Attribute origObjectClass =
                    origAttrs.get(Obj.JAVA_ATTRIBUTES[Obj.OBJECT_CLASS]);

                if (origObjectClass != null) {
                    // clone so that keepAttrs is not affected
                    origObjectClass = (Attribute)origObjectClass.clone();
                    for (int i = 0; i < Obj.JAVA_OBJECT_CLASSES.length; i++) {
                        origObjectClass.remove(Obj.JAVA_OBJECT_CLASSES_LOWER[i]);
                        origObjectClass.remove(Obj.JAVA_OBJECT_CLASSES[i]);
                    }
                    // update;
                    origAttrs.put(origObjectClass);
                }

                // remove all Java-related attributes except objectclass
                for (int i = 1; i < Obj.JAVA_ATTRIBUTES.length; i++) {
                    origAttrs.remove(Obj.JAVA_ATTRIBUTES[i]);
                }

                attrs = origAttrs;
            }
            if (obj != null) {
                attrs =
                    Obj.determineBindAttrs(addrEncodingSeparator, obj, attrs,
                        inputAttrs != attrs, name, this, envprops);
            }

            String newDN = fullyQualifiedName(name);
            // remove entry
            LdapResult answer = clnt.delete(newDN, reqCtls);
            respCtls = answer.resControls; // retrieve response controls

            if (answer.status != LdapClient.LDAP_SUCCESS) {
                processReturnCode(answer, name);
                return;
            }

            Exception addEx = null;
            try {
                attrs = addRdnAttributes(newDN, attrs, inputAttrs != attrs);

                // add it back using updated attrs
                LdapEntry entry = new LdapEntry(newDN, attrs);
                answer = clnt.add(entry, reqCtls);
                if (answer.resControls != null) {
                    respCtls = appendVector(respCtls, answer.resControls);
                }
            } catch (NamingException | IOException ae) {
                addEx = ae;
            }

            if ((addEx != null && !(addEx instanceof LdapReferralException)) ||
                answer.status != LdapClient.LDAP_SUCCESS) {
                // Attempt to restore old entry
                LdapResult answer2 =
                    clnt.add(new LdapEntry(newDN, keepAttrs), reqCtls);
                if (answer2.resControls != null) {
                    respCtls = appendVector(respCtls, answer2.resControls);
                }

                if (addEx == null) {
                    processReturnCode(answer, name);
                }
            }

            // Rethrow exception
            if (addEx instanceof NamingException) {
                throw (NamingException)addEx;
            } else if (addEx instanceof IOException) {
                throw (IOException)addEx;
            }

        } catch (LdapReferralException e) {
            if (handleReferrals == LdapClient.LDAP_REF_THROW)
                throw cont.fillInException(e);

            // process the referrals sequentially
            while (true) {

                LdapReferralContext refCtx =
                    (LdapReferralContext)e.getReferralContext(envprops, bindCtls);

                // repeat the original operation at the new context
                try {

                    refCtx.rebind(name, obj, inputAttrs);
                    return;

                } catch (LdapReferralException re) {
                    e = re;
                    continue;

                } finally {
                    // Make sure we close referral context
                    refCtx.close();
                }
            }

        } catch (IOException e) {
            NamingException e2 = new CommunicationException(e.getMessage());
            e2.setRootCause(e);
            throw cont.fillInException(e2);

        } catch (NamingException e) {
            throw cont.fillInException(e);
        }
    }

    protected void c_unbind(Name name, Continuation cont)
            throws NamingException {
        cont.setError(this, name);

        try {
            ensureOpen();

            String fname = fullyQualifiedName(name);
            LdapResult answer = clnt.delete(fname, reqCtls);
            respCtls = answer.resControls; // retrieve response controls

            adjustDeleteStatus(fname, answer);

            if (answer.status != LdapClient.LDAP_SUCCESS) {
                processReturnCode(answer, name);
            }

        } catch (LdapReferralException e) {
            if (handleReferrals == LdapClient.LDAP_REF_THROW)
                throw cont.fillInException(e);

            // process the referrals sequentially
            while (true) {

                LdapReferralContext refCtx =
                    (LdapReferralContext)e.getReferralContext(envprops, bindCtls);

                // repeat the original operation at the new context
                try {

                    refCtx.unbind(name);
                    return;

                } catch (LdapReferralException re) {
                    e = re;
                    continue;

                } finally {
                    // Make sure we close referral context
                    refCtx.close();
                }
            }

        } catch (IOException e) {
            NamingException e2 = new CommunicationException(e.getMessage());
            e2.setRootCause(e);
            throw cont.fillInException(e2);

        } catch (NamingException e) {
            throw cont.fillInException(e);
        }
    }

    protected void c_rename(Name oldName, Name newName, Continuation cont)
            throws NamingException
    {
        Name oldParsed, newParsed;
        Name oldParent, newParent;
        String newRDN = null;
        String newSuperior = null;

        // assert (oldName instanceOf CompositeName);

        cont.setError(this, oldName);

        try {
            ensureOpen();

            // permit oldName to be empty (for processing referral contexts)
            if (oldName.isEmpty()) {
                oldParent = parser.parse("");
            } else {
                oldParsed = parser.parse(oldName.get(0)); // extract DN & parse
                oldParent = oldParsed.getPrefix(oldParsed.size() - 1);
            }

            if (newName instanceof CompositeName) {
                newParsed = parser.parse(newName.get(0)); // extract DN & parse
            } else {
                newParsed = newName; // CompoundName/LdapName is already parsed
            }
            newParent = newParsed.getPrefix(newParsed.size() - 1);

            if(!oldParent.equals(newParent)) {
                if (!clnt.isLdapv3) {
                    throw new InvalidNameException(
                                  "LDAPv2 doesn't support changing " +
                                  "the parent as a result of a rename");
                } else {
                    newSuperior = fullyQualifiedName(newParent.toString());
                }
            }

            newRDN = newParsed.get(newParsed.size() - 1);

            LdapResult answer = clnt.moddn(fullyQualifiedName(oldName),
                                    newRDN,
                                    deleteRDN,
                                    newSuperior,
                                    reqCtls);
            respCtls = answer.resControls; // retrieve response controls

            if (answer.status != LdapClient.LDAP_SUCCESS) {
                processReturnCode(answer, oldName);
            }

        } catch (LdapReferralException e) {

            // Record the new RDN (for use after the referral is followed).
            e.setNewRdn(newRDN);

            // Cannot continue when a referral has been received and a
            // newSuperior name was supplied (because the newSuperior is
            // relative to a naming context BEFORE the referral is followed).
            if (newSuperior != null) {
                PartialResultException pre = new PartialResultException(
                    "Cannot continue referral processing when newSuperior is " +
                    "nonempty: " + newSuperior);
                pre.setRootCause(cont.fillInException(e));
                throw cont.fillInException(pre);
            }

            if (handleReferrals == LdapClient.LDAP_REF_THROW)
                throw cont.fillInException(e);

            // process the referrals sequentially
            while (true) {

                LdapReferralContext refCtx =
                    (LdapReferralContext)e.getReferralContext(envprops, bindCtls);

                // repeat the original operation at the new context
                try {

                    refCtx.rename(oldName, newName);
                    return;

                } catch (LdapReferralException re) {
                    e = re;
                    continue;

                } finally {
                    // Make sure we close referral context
                    refCtx.close();
                }
            }

        } catch (IOException e) {
            NamingException e2 = new CommunicationException(e.getMessage());
            e2.setRootCause(e);
            throw cont.fillInException(e2);

        } catch (NamingException e) {
            throw cont.fillInException(e);
        }
    }

    protected Context c_createSubcontext(Name name, Continuation cont)
            throws NamingException {
        return c_createSubcontext(name, null, cont);
    }

    protected DirContext c_createSubcontext(Name name, Attributes attrs,
                                            Continuation cont)
            throws NamingException {
        cont.setError(this, name);

        Attributes inputAttrs = attrs;
        try {
            ensureOpen();
            if (attrs == null) {
                  // add structural objectclass; name needs to have "cn"
                  Attribute oc = new BasicAttribute(
                      Obj.JAVA_ATTRIBUTES[Obj.OBJECT_CLASS],
                      Obj.JAVA_OBJECT_CLASSES[Obj.STRUCTURAL]);
                  oc.add("top");
                  attrs = new BasicAttributes(true); // case ignore
                  attrs.put(oc);
            }
            String newDN = fullyQualifiedName(name);
            attrs = addRdnAttributes(newDN, attrs, inputAttrs != attrs);

            LdapEntry entry = new LdapEntry(newDN, attrs);

            LdapResult answer = clnt.add(entry, reqCtls);
            respCtls = answer.resControls; // retrieve response controls

            if (answer.status != LdapClient.LDAP_SUCCESS) {
                processReturnCode(answer, name);
                return null;
            }

            // creation successful, get back live object
            return new LdapCtx(this, newDN);

        } catch (LdapReferralException e) {
            if (handleReferrals == LdapClient.LDAP_REF_THROW)
                throw cont.fillInException(e);

            // process the referrals sequentially
            while (true) {

                LdapReferralContext refCtx =
                    (LdapReferralContext)e.getReferralContext(envprops, bindCtls);

                // repeat the original operation at the new context
                try {

                    return refCtx.createSubcontext(name, inputAttrs);

                } catch (LdapReferralException re) {
                    e = re;
                    continue;

                } finally {
                    // Make sure we close referral context
                    refCtx.close();
                }
            }

        } catch (IOException e) {
            NamingException e2 = new CommunicationException(e.getMessage());
            e2.setRootCause(e);
            throw cont.fillInException(e2);

        } catch (NamingException e) {
            throw cont.fillInException(e);
        }
    }

    protected void c_destroySubcontext(Name name, Continuation cont)
        throws NamingException {
        cont.setError(this, name);

        try {
            ensureOpen();

            String fname = fullyQualifiedName(name);
            LdapResult answer = clnt.delete(fname, reqCtls);
            respCtls = answer.resControls; // retrieve response controls

            adjustDeleteStatus(fname, answer);

            if (answer.status != LdapClient.LDAP_SUCCESS) {
                processReturnCode(answer, name);
            }

        } catch (LdapReferralException e) {
            if (handleReferrals == LdapClient.LDAP_REF_THROW)
                throw cont.fillInException(e);

            // process the referrals sequentially
            while (true) {

                LdapReferralContext refCtx =
                    (LdapReferralContext)e.getReferralContext(envprops, bindCtls);

                // repeat the original operation at the new context
                try {

                    refCtx.destroySubcontext(name);
                    return;
                } catch (LdapReferralException re) {
                    e = re;
                    continue;
                } finally {
                    // Make sure we close referral context
                    refCtx.close();
                }
            }
        } catch (IOException e) {
            NamingException e2 = new CommunicationException(e.getMessage());
            e2.setRootCause(e);
            throw cont.fillInException(e2);
        } catch (NamingException e) {
            throw cont.fillInException(e);
        }
    }

    /**
     * Adds attributes from RDN to attrs if not already present.
     * Note that if attrs already contains an attribute by the same name,
     * or if the distinguished name is empty, then leave attrs unchanged.
     *
     * @param dn The non-null DN of the entry to add
     * @param attrs The non-null attributes of entry to add
     * @param directUpdate Whether attrs can be updated directly
     * @return Non-null attributes with attributes from the RDN added
     */
    private static Attributes addRdnAttributes(String dn, Attributes attrs,
        boolean directUpdate) throws NamingException {

            // Handle the empty name
            if (dn.isEmpty()) {
                return attrs;
            }

            // Parse string name into list of RDNs
            List<Rdn> rdnList = (new LdapName(dn)).getRdns();

            // Get leaf RDN
            Rdn rdn = rdnList.get(rdnList.size() - 1);
            Attributes nameAttrs = rdn.toAttributes();

            // Add attributes of RDN to attrs if not already there
            NamingEnumeration<? extends Attribute> enum_ = nameAttrs.getAll();
            Attribute nameAttr;
            while (enum_.hasMore()) {
                nameAttr = enum_.next();

                // If attrs already has the attribute, don't change or add to it
                if (attrs.get(nameAttr.getID()) ==  null) {

                    /**
                     * When attrs.isCaseIgnored() is false, attrs.get() will
                     * return null when the case mis-matches for otherwise
                     * equal attrIDs.
                     * As the attrIDs' case is irrelevant for LDAP, ignore
                     * the case of attrIDs even when attrs.isCaseIgnored() is
                     * false. This is done by explicitly comparing the elements in
                     * the enumeration of IDs with their case ignored.
                     */
                    if (!attrs.isCaseIgnored() &&
                            containsIgnoreCase(attrs.getIDs(), nameAttr.getID())) {
                        continue;
                    }

                    if (!directUpdate) {
                        attrs = (Attributes)attrs.clone();
                        directUpdate = true;
                    }
                    attrs.put(nameAttr);
                }
            }

            return attrs;
    }


    private static boolean containsIgnoreCase(NamingEnumeration<String> enumStr,
                                String str) throws NamingException {
        String strEntry;

        while (enumStr.hasMore()) {
             strEntry = enumStr.next();
             if (strEntry.equalsIgnoreCase(str)) {
                return true;
             }
        }
        return false;
    }


    private void adjustDeleteStatus(String fname, LdapResult answer) {
        if (answer.status == LdapClient.LDAP_NO_SUCH_OBJECT &&
            answer.matchedDN != null) {
            try {
                // %%% RL: are there any implications for referrals?

                Name orig = parser.parse(fname);
                Name matched = parser.parse(answer.matchedDN);
                if ((orig.size() - matched.size()) == 1)
                    answer.status = LdapClient.LDAP_SUCCESS;
            } catch (NamingException e) {}
        }
    }

    /*
     * Append the second Vector onto the first Vector
     * (v2 must be non-null)
     */
    private static <T> Vector<T> appendVector(Vector<T> v1, Vector<T> v2) {
        if (v1 == null) {
            v1 = v2;
        } else {
            for (int i = 0; i < v2.size(); i++) {
                v1.addElement(v2.elementAt(i));
            }
        }
        return v1;
    }

    // ------------- Lookups and Browsing -------------------------
    // lookup/lookupLink
    // list/listBindings

    protected Object c_lookupLink(Name name, Continuation cont)
            throws NamingException {
        return c_lookup(name, cont);
    }

    protected Object c_lookup(Name name, Continuation cont)
            throws NamingException {
        cont.setError(this, name);
        Object obj = null;
        Attributes attrs;

        try {
            SearchControls cons = new SearchControls();
            cons.setSearchScope(SearchControls.OBJECT_SCOPE);
            cons.setReturningAttributes(null); // ask for all attributes
            cons.setReturningObjFlag(true); // need values to construct obj

            LdapResult answer = doSearchOnce(name, "(objectClass=*)", cons, true);
            respCtls = answer.resControls; // retrieve response controls

            // should get back 1 SearchResponse and 1 SearchResult

            if (answer.status != LdapClient.LDAP_SUCCESS) {
                processReturnCode(answer, name);
            }

            if (answer.entries == null || answer.entries.size() != 1) {
                // found it but got no attributes
                attrs = new BasicAttributes(LdapClient.caseIgnore);
            } else {
                LdapEntry entry = answer.entries.elementAt(0);
                attrs = entry.attributes;

                Vector<Control> entryCtls = entry.respCtls; // retrieve entry controls
                if (entryCtls != null) {
                    appendVector(respCtls, entryCtls); // concatenate controls
                }
            }

            if (attrs.get(Obj.JAVA_ATTRIBUTES[Obj.CLASSNAME]) != null) {
                // serialized object or object reference
                obj = Obj.decodeObject(attrs);
            }
            if (obj == null) {
                obj = new LdapCtx(this, fullyQualifiedName(name));
            }
        } catch (LdapReferralException e) {
            if (handleReferrals == LdapClient.LDAP_REF_THROW)
                throw cont.fillInException(e);

            // process the referrals sequentially
            while (true) {

                LdapReferralContext refCtx =
                    (LdapReferralContext)e.getReferralContext(envprops, bindCtls);
                // repeat the original operation at the new context
                try {

                    return refCtx.lookup(name);

                } catch (LdapReferralException re) {
                    e = re;
                    continue;

                } finally {
                    // Make sure we close referral context
                    refCtx.close();
                }
            }

        } catch (NamingException e) {
            throw cont.fillInException(e);
        }

        try {
            return DirectoryManager.getObjectInstance(obj, name,
                this, envprops, attrs);

        } catch (NamingException e) {
            throw cont.fillInException(e);

        } catch (Exception e) {
            NamingException e2 = new NamingException(
                    "problem generating object using object factory");
            e2.setRootCause(e);
            throw cont.fillInException(e2);
        }
    }

    protected NamingEnumeration<NameClassPair> c_list(Name name, Continuation cont)
            throws NamingException {
        SearchControls cons = new SearchControls();
        String[] classAttrs = new String[2];

        classAttrs[0] = Obj.JAVA_ATTRIBUTES[Obj.OBJECT_CLASS];
        classAttrs[1] = Obj.JAVA_ATTRIBUTES[Obj.CLASSNAME];
        cons.setReturningAttributes(classAttrs);

        // set this flag to override the typesOnly flag
        cons.setReturningObjFlag(true);

        cont.setError(this, name);

        LdapResult answer = null;

        try {
            answer = doSearch(name, "(objectClass=*)", cons, true, true);

            // list result may contain continuation references
            if ((answer.status != LdapClient.LDAP_SUCCESS) ||
                (answer.referrals != null)) {
                processReturnCode(answer, name);
            }

            return new LdapNamingEnumeration(this, answer, name, cont);

        } catch (LdapReferralException e) {
            if (handleReferrals == LdapClient.LDAP_REF_THROW)
                throw cont.fillInException(e);

            // process the referrals sequentially
            while (true) {

                LdapReferralContext refCtx =
                    (LdapReferralContext)e.getReferralContext(envprops, bindCtls);

                // repeat the original operation at the new context
                try {

                    return refCtx.list(name);

                } catch (LdapReferralException re) {
                    e = re;
                    continue;

                } finally {
                    // Make sure we close referral context
                    refCtx.close();
                }
            }

        } catch (LimitExceededException e) {
            LdapNamingEnumeration res =
                new LdapNamingEnumeration(this, answer, name, cont);

            res.setNamingException(
                    (LimitExceededException)cont.fillInException(e));
            return res;

        } catch (PartialResultException e) {
            LdapNamingEnumeration res =
                new LdapNamingEnumeration(this, answer, name, cont);

            res.setNamingException(
                    (PartialResultException)cont.fillInException(e));
            return res;

        } catch (NamingException e) {
            throw cont.fillInException(e);
        }
    }

    protected NamingEnumeration<Binding> c_listBindings(Name name, Continuation cont)
            throws NamingException {

        SearchControls cons = new SearchControls();
        cons.setReturningAttributes(null); // ask for all attributes
        cons.setReturningObjFlag(true); // need values to construct obj

        cont.setError(this, name);

        LdapResult answer = null;

        try {
            answer = doSearch(name, "(objectClass=*)", cons, true, true);

            // listBindings result may contain continuation references
            if ((answer.status != LdapClient.LDAP_SUCCESS) ||
                (answer.referrals != null)) {
                processReturnCode(answer, name);
            }

            return new LdapBindingEnumeration(this, answer, name, cont);

        } catch (LdapReferralException e) {
            if (handleReferrals == LdapClient.LDAP_REF_THROW)
                throw cont.fillInException(e);

            // process the referrals sequentially
            while (true) {
                @SuppressWarnings("unchecked")
                LdapReferralContext refCtx =
                    (LdapReferralContext)e.getReferralContext(envprops, bindCtls);

                // repeat the original operation at the new context
                try {

                    return refCtx.listBindings(name);

                } catch (LdapReferralException re) {
                    e = re;
                    continue;

                } finally {
                    // Make sure we close referral context
                    refCtx.close();
                }
            }
        } catch (LimitExceededException e) {
            LdapBindingEnumeration res =
                new LdapBindingEnumeration(this, answer, name, cont);

            res.setNamingException(cont.fillInException(e));
            return res;

        } catch (PartialResultException e) {
            LdapBindingEnumeration res =
                new LdapBindingEnumeration(this, answer, name, cont);

            res.setNamingException(cont.fillInException(e));
            return res;

        } catch (NamingException e) {
            throw cont.fillInException(e);
        }
    }

    // --------------- Name-related Methods -----------------------
    // -- getNameParser/getNameInNamespace/composeName

    protected NameParser c_getNameParser(Name name, Continuation cont)
            throws NamingException
    {
        // ignore name, always return same parser
        cont.setSuccess();
        return parser;
    }

    public String getNameInNamespace() {
        return currentDN;
    }

    public Name composeName(Name name, Name prefix)
        throws NamingException
    {
        Name result;

        // Handle compound names.  A pair of LdapNames is an easy case.
        if ((name instanceof LdapName) && (prefix instanceof LdapName)) {
            result = (Name)(prefix.clone());
            result.addAll(name);
            return new CompositeName().add(result.toString());
        }
        if (!(name instanceof CompositeName)) {
            name = new CompositeName().add(name.toString());
        }
        if (!(prefix instanceof CompositeName)) {
            prefix = new CompositeName().add(prefix.toString());
        }

        int prefixLast = prefix.size() - 1;

        if (name.isEmpty() || prefix.isEmpty() ||
                name.get(0).isEmpty() || prefix.get(prefixLast).isEmpty()) {
            return super.composeName(name, prefix);
        }

        result = (Name)(prefix.clone());
        result.addAll(name);

        if (parentIsLdapCtx) {
            String ldapComp = concatNames(result.get(prefixLast + 1),
                                          result.get(prefixLast));
            result.remove(prefixLast + 1);
            result.remove(prefixLast);
            result.add(prefixLast, ldapComp);
        }
        return result;
    }

    private String fullyQualifiedName(Name rel) {
        return rel.isEmpty()
                ? currentDN
                : fullyQualifiedName(rel.get(0));
    }

    private String fullyQualifiedName(String rel) {
        return (concatNames(rel, currentDN));
    }

    // used by LdapSearchEnumeration
    private static String concatNames(String lesser, String greater) {
        if (lesser == null || lesser.isEmpty()) {
            return greater;
        } else if (greater == null || greater.isEmpty()) {
            return lesser;
        } else {
            return (lesser + "," + greater);
        }
    }

   // --------------- Reading and Updating Attributes
   // getAttributes/modifyAttributes

    protected Attributes c_getAttributes(Name name, String[] attrIds,
                                      Continuation cont)
            throws NamingException {
        cont.setError(this, name);

        SearchControls cons = new SearchControls();
        cons.setSearchScope(SearchControls.OBJECT_SCOPE);
        cons.setReturningAttributes(attrIds);

        try {
            LdapResult answer =
                doSearchOnce(name, "(objectClass=*)", cons, true);
            respCtls = answer.resControls; // retrieve response controls

            if (answer.status != LdapClient.LDAP_SUCCESS) {
                processReturnCode(answer, name);
            }

            if (answer.entries == null || answer.entries.size() != 1) {
                return new BasicAttributes(LdapClient.caseIgnore);
            }

            // get attributes from result
            LdapEntry entry = answer.entries.elementAt(0);

            Vector<Control> entryCtls = entry.respCtls; // retrieve entry controls
            if (entryCtls != null) {
                appendVector(respCtls, entryCtls); // concatenate controls
            }

            // do this so attributes can find their schema
            setParents(entry.attributes, (Name) name.clone());

            return (entry.attributes);

        } catch (LdapReferralException e) {
            if (handleReferrals == LdapClient.LDAP_REF_THROW)
                throw cont.fillInException(e);

            // process the referrals sequentially
            while (true) {

                LdapReferralContext refCtx =
                    (LdapReferralContext)e.getReferralContext(envprops, bindCtls);

                // repeat the original operation at the new context
                try {

                    return refCtx.getAttributes(name, attrIds);

                } catch (LdapReferralException re) {
                    e = re;
                    continue;

                } finally {
                    // Make sure we close referral context
                    refCtx.close();
                }
            }

        } catch (NamingException e) {
            throw cont.fillInException(e);
        }
    }

    protected void c_modifyAttributes(Name name, int mod_op, Attributes attrs,
                                      Continuation cont)
            throws NamingException {

        cont.setError(this, name);

        try {
            ensureOpen();

            if (attrs == null || attrs.size() == 0) {
                return; // nothing to do
            }
            String newDN = fullyQualifiedName(name);
            int jmod_op = convertToLdapModCode(mod_op);

            // construct mod list
            int[] jmods = new int[attrs.size()];
            Attribute[] jattrs = new Attribute[attrs.size()];

            NamingEnumeration<? extends Attribute> ae = attrs.getAll();
            for(int i = 0; i < jmods.length && ae.hasMore(); i++) {
                jmods[i] = jmod_op;
                jattrs[i] = ae.next();
            }

            LdapResult answer = clnt.modify(newDN, jmods, jattrs, reqCtls);
            respCtls = answer.resControls; // retrieve response controls

            if (answer.status != LdapClient.LDAP_SUCCESS) {
                processReturnCode(answer, name);
                return;
            }

        } catch (LdapReferralException e) {
            if (handleReferrals == LdapClient.LDAP_REF_THROW)
                throw cont.fillInException(e);

            // process the referrals sequentially
            while (true) {

                LdapReferralContext refCtx =
                    (LdapReferralContext)e.getReferralContext(envprops, bindCtls);

                // repeat the original operation at the new context
                try {

                    refCtx.modifyAttributes(name, mod_op, attrs);
                    return;

                } catch (LdapReferralException re) {
                    e = re;
                    continue;

                } finally {
                    // Make sure we close referral context
                    refCtx.close();
                }
            }

        } catch (IOException e) {
            NamingException e2 = new CommunicationException(e.getMessage());
            e2.setRootCause(e);
            throw cont.fillInException(e2);

        } catch (NamingException e) {
            throw cont.fillInException(e);
        }
    }

    protected void c_modifyAttributes(Name name, ModificationItem[] mods,
                                      Continuation cont)
            throws NamingException {
        cont.setError(this, name);

        try {
            ensureOpen();

            if (mods == null || mods.length == 0) {
                return; // nothing to do
            }
            String newDN = fullyQualifiedName(name);

            // construct mod list
            int[] jmods = new int[mods.length];
            Attribute[] jattrs = new Attribute[mods.length];
            ModificationItem mod;
            for (int i = 0; i < jmods.length; i++) {
                mod = mods[i];
                jmods[i] = convertToLdapModCode(mod.getModificationOp());
                jattrs[i] = mod.getAttribute();
            }

            LdapResult answer = clnt.modify(newDN, jmods, jattrs, reqCtls);
            respCtls = answer.resControls; // retrieve response controls

            if (answer.status != LdapClient.LDAP_SUCCESS) {
                processReturnCode(answer, name);
            }

        } catch (LdapReferralException e) {
            if (handleReferrals == LdapClient.LDAP_REF_THROW)
                throw cont.fillInException(e);

            // process the referrals sequentially
            while (true) {

                LdapReferralContext refCtx =
                    (LdapReferralContext)e.getReferralContext(envprops, bindCtls);

                // repeat the original operation at the new context
                try {

                    refCtx.modifyAttributes(name, mods);
                    return;

                } catch (LdapReferralException re) {
                    e = re;
                    continue;

                } finally {
                    // Make sure we close referral context
                    refCtx.close();
                }
            }

        } catch (IOException e) {
            NamingException e2 = new CommunicationException(e.getMessage());
            e2.setRootCause(e);
            throw cont.fillInException(e2);

        } catch (NamingException e) {
            throw cont.fillInException(e);
        }
    }

    private static int convertToLdapModCode(int mod_op) {
        switch (mod_op) {
        case DirContext.ADD_ATTRIBUTE:
            return(LdapClient.ADD);

        case DirContext.REPLACE_ATTRIBUTE:
            return (LdapClient.REPLACE);

        case DirContext.REMOVE_ATTRIBUTE:
            return (LdapClient.DELETE);

        default:
            throw new IllegalArgumentException("Invalid modification code");
        }
    }

   // ------------------- Schema -----------------------

    protected DirContext c_getSchema(Name name, Continuation cont)
            throws NamingException {
        cont.setError(this, name);
        try {
            return getSchemaTree(name);

        } catch (NamingException e) {
            throw cont.fillInException(e);
        }
    }

    protected DirContext c_getSchemaClassDefinition(Name name,
                                                    Continuation cont)
            throws NamingException {
        cont.setError(this, name);

        try {
            // retrieve the objectClass attribute from LDAP
            Attribute objectClassAttr = c_getAttributes(name,
                new String[]{"objectclass"}, cont).get("objectclass");
            if (objectClassAttr == null || objectClassAttr.size() == 0) {
                return EMPTY_SCHEMA;
            }

            // retrieve the root of the ObjectClass schema tree
            Context ocSchema = (Context) c_getSchema(name, cont).lookup(
                LdapSchemaParser.OBJECTCLASS_DEFINITION_NAME);

            // create a context to hold the schema objects representing the object
            // classes
            HierMemDirCtx objectClassCtx = new HierMemDirCtx();
            DirContext objectClassDef;
            String objectClassName;
            for (Enumeration<?> objectClasses = objectClassAttr.getAll();
                objectClasses.hasMoreElements(); ) {
                objectClassName = (String)objectClasses.nextElement();
                // %%% Should we fail if not found, or just continue?
                objectClassDef = (DirContext)ocSchema.lookup(objectClassName);
                objectClassCtx.bind(objectClassName, objectClassDef);
            }

            // Make context read-only
            objectClassCtx.setReadOnly(
                new SchemaViolationException("Cannot update schema object"));
            return (DirContext)objectClassCtx;

        } catch (NamingException e) {
            throw cont.fillInException(e);
        }
    }

    /*
     * getSchemaTree first looks to see if we have already built a
     * schema tree for the given entry. If not, it builds a new one and
     * stores it in our private hash table
     */
    private DirContext getSchemaTree(Name name) throws NamingException {
        String subschemasubentry = getSchemaEntry(name, true);

        DirContext schemaTree = schemaTrees.get(subschemasubentry);

        if(schemaTree==null) {
            if(debug){System.err.println("LdapCtx: building new schema tree " + this);}
            schemaTree = buildSchemaTree(subschemasubentry);
            schemaTrees.put(subschemasubentry, schemaTree);
        }

        return schemaTree;
    }

    /*
     * buildSchemaTree builds the schema tree corresponding to the
     * given subschemasubentree
     */
    private DirContext buildSchemaTree(String subschemasubentry)
        throws NamingException {

        // get the schema entry itself
        // DO ask for return object here because we need it to
        // create context. Since asking for all attrs, we won't
        // be transmitting any specific attrIDs (like Java-specific ones).
        SearchControls constraints = new
            SearchControls(SearchControls.OBJECT_SCOPE,
                0, 0, /* count and time limits */
                SCHEMA_ATTRIBUTES /* return schema attrs */,
                true /* return obj */,
                false /*deref link */ );

        Name sse = (new CompositeName()).add(subschemasubentry);
        NamingEnumeration<SearchResult> results =
            searchAux(sse, "(objectClass=subschema)", constraints,
            false, true, new Continuation());

        if(!results.hasMore()) {
            throw new OperationNotSupportedException(
                "Cannot get read subschemasubentry: " + subschemasubentry);
        }
        SearchResult result = results.next();
        results.close();

        Object obj = result.getObject();
        if(!(obj instanceof LdapCtx)) {
            throw new NamingException(
                "Cannot get schema object as DirContext: " + subschemasubentry);
        }

        return LdapSchemaCtx.createSchemaTree(envprops, subschemasubentry,
            (LdapCtx)obj /* schema entry */,
            result.getAttributes() /* schema attributes */,
            netscapeSchemaBug);
   }

    /*
     * getSchemaEntree returns the DN of the subschemasubentree for the
     * given entree. It first looks to see if the given entry has
     * a subschema different from that of the root DIT (by looking for
     * a "subschemasubentry" attribute). If it doesn't find one, it returns
     * the one for the root of the DIT (by looking for the root's
     * "subschemasubentry" attribute).
     *
     * This function is called regardless of the server's version, since
     * an administrator may have setup the server to support client schema
     * queries. If this function tries a search on a v2 server that
     * doesn't support schema, one of these two things will happen:
     * 1) It will get an exception when querying the root DSE
     * 2) It will not find a subschemasubentry on the root DSE
     * If either of these things occur and the server is not v3, we
     * throw OperationNotSupported.
     *
     * the relative flag tells whether the given name is relative to this
     * context.
     */
    private String getSchemaEntry(Name name, boolean relative)
        throws NamingException {

        // Asks for operational attribute "subschemasubentry"
        SearchControls constraints = new SearchControls(SearchControls.OBJECT_SCOPE,
            0, 0, /* count and time limits */
            new String[]{"subschemasubentry"} /* attr to return */,
            false /* returning obj */,
            false /* deref link */);

        NamingEnumeration<SearchResult> results;
        try {
            results = searchAux(name, "objectclass=*", constraints, relative,
                true, new Continuation());

        } catch (NamingException ne) {
            if (!clnt.isLdapv3 && currentDN.length() == 0 && name.isEmpty()) {
                // we got an error looking for a root entry on an ldapv2
                // server. The server must not support schema.
                throw new OperationNotSupportedException(
                    "Cannot get schema information from server");
            } else {
                throw ne;
            }
        }

        if (!results.hasMoreElements()) {
            throw new ConfigurationException(
                "Requesting schema of nonexistent entry: " + name);
        }

        SearchResult result = results.next();
        results.close();

        Attribute schemaEntryAttr =
            result.getAttributes().get("subschemasubentry");
        //System.err.println("schema entry attrs: " + schemaEntryAttr);

        if (schemaEntryAttr == null || schemaEntryAttr.size() < 0) {
            if (currentDN.length() == 0 && name.isEmpty()) {
                // the server doesn't have a subschemasubentry in its root DSE.
                // therefore, it doesn't support schema.
                throw new OperationNotSupportedException(
                    "Cannot read subschemasubentry of root DSE");
            } else {
                return getSchemaEntry(new CompositeName(), false);
            }
        }

        return (String)(schemaEntryAttr.get()); // return schema entry name
    }

    // package-private; used by search enum.
    // Set attributes to point to this context in case some one
    // asked for their schema
    void setParents(Attributes attrs, Name name) throws NamingException {
        NamingEnumeration<? extends Attribute> ae = attrs.getAll();
        while(ae.hasMore()) {
            ((LdapAttribute) ae.next()).setParent(this, name);
        }
    }

    /*
     * Returns the URL associated with this context; used by LdapAttribute
     * after deserialization to get pointer to this context.
     */
    String getURL() {
        if (url == null) {
            url = LdapURL.toUrlString(hostname, port_number, currentDN,
                hasLdapsScheme);
        }

        return url;
    }

   // --------------------- Searches -----------------------------
    protected NamingEnumeration<SearchResult> c_search(Name name,
                                         Attributes matchingAttributes,
                                         Continuation cont)
            throws NamingException {
        return c_search(name, matchingAttributes, null, cont);
    }

    protected NamingEnumeration<SearchResult> c_search(Name name,
                                         Attributes matchingAttributes,
                                         String[] attributesToReturn,
                                         Continuation cont)
            throws NamingException {
        SearchControls cons = new SearchControls();
        cons.setReturningAttributes(attributesToReturn);
        String filter;
        try {
            filter = SearchFilter.format(matchingAttributes);
        } catch (NamingException e) {
            cont.setError(this, name);
            throw cont.fillInException(e);
        }
        return c_search(name, filter, cons, cont);
    }

    protected NamingEnumeration<SearchResult> c_search(Name name,
                                         String filter,
                                         SearchControls cons,
                                         Continuation cont)
            throws NamingException {
        return searchAux(name, filter, cloneSearchControls(cons), true,
                 waitForReply, cont);
    }

    protected NamingEnumeration<SearchResult> c_search(Name name,
                                         String filterExpr,
                                         Object[] filterArgs,
                                         SearchControls cons,
                                         Continuation cont)
            throws NamingException {
        String strfilter;
        try {
            strfilter = SearchFilter.format(filterExpr, filterArgs);
        } catch (NamingException e) {
            cont.setError(this, name);
            throw cont.fillInException(e);
        }
        return c_search(name, strfilter, cons, cont);
    }

        // Used by NamingNotifier
    NamingEnumeration<SearchResult> searchAux(Name name,
        String filter,
        SearchControls cons,
        boolean relative,
        boolean waitForReply, Continuation cont) throws NamingException {

        LdapResult answer = null;
        String[] tokens = new String[2];    // stores ldap compare op. values
        String[] reqAttrs;                  // remember what was asked

        if (cons == null) {
            cons = new SearchControls();
        }
        reqAttrs = cons.getReturningAttributes();

        // if objects are requested then request the Java attributes too
        // so that the objects can be constructed
        if (cons.getReturningObjFlag()) {
            if (reqAttrs != null) {

                // check for presence of "*" (user attributes wildcard)
                boolean hasWildcard = false;
                for (int i = reqAttrs.length - 1; i >= 0; i--) {
                    if (reqAttrs[i].equals("*")) {
                        hasWildcard = true;
                        break;
                    }
                }
                if (! hasWildcard) {
                    String[] totalAttrs =
                        new String[reqAttrs.length +Obj.JAVA_ATTRIBUTES.length];
                    System.arraycopy(reqAttrs, 0, totalAttrs, 0,
                        reqAttrs.length);
                    System.arraycopy(Obj.JAVA_ATTRIBUTES, 0, totalAttrs,
                        reqAttrs.length, Obj.JAVA_ATTRIBUTES.length);

                    cons.setReturningAttributes(totalAttrs);
                }
            }
        }

        LdapCtx.SearchArgs args =
            new LdapCtx.SearchArgs(name, filter, cons, reqAttrs);

        cont.setError(this, name);
        try {
            // see if this can be done as a compare, otherwise do a search
            if (searchToCompare(filter, cons, tokens)){
                //System.err.println("compare triggered");
                answer = compare(name, tokens[0], tokens[1]);
                if (! (answer.compareToSearchResult(fullyQualifiedName(name)))){
                    processReturnCode(answer, name);
                }
            } else {
                answer = doSearch(name, filter, cons, relative, waitForReply);
                // search result may contain referrals
                processReturnCode(answer, name);
            }
            return new LdapSearchEnumeration(this, answer,
                                             fullyQualifiedName(name),
                                             args, cont);

        } catch (LdapReferralException e) {
            if (handleReferrals == LdapClient.LDAP_REF_THROW)
                throw cont.fillInException(e);

            // process the referrals sequentially
            while (true) {

                @SuppressWarnings("unchecked")
                LdapReferralContext refCtx = (LdapReferralContext)
                        e.getReferralContext(envprops, bindCtls);

                // repeat the original operation at the new context
                try {

                    return refCtx.search(name, filter, cons);

                } catch (LdapReferralException re) {
                    e = re;
                    continue;

                } finally {
                    // Make sure we close referral context
                    refCtx.close();
                }
            }

        } catch (LimitExceededException e) {
            LdapSearchEnumeration res =
                new LdapSearchEnumeration(this, answer, fullyQualifiedName(name),
                                          args, cont);
            res.setNamingException(e);
            return res;

        } catch (PartialResultException e) {
            LdapSearchEnumeration res =
                new LdapSearchEnumeration(this, answer, fullyQualifiedName(name),
                                          args, cont);

            res.setNamingException(e);
            return res;

        } catch (IOException e) {
            NamingException e2 = new CommunicationException(e.getMessage());
            e2.setRootCause(e);
            throw cont.fillInException(e2);

        } catch (NamingException e) {
            throw cont.fillInException(e);
        }
    }


    LdapResult getSearchReply(LdapClient eClnt, LdapResult res)
            throws NamingException {
        // ensureOpen() won't work here because
        // session was associated with previous connection

        // %%% RL: we can actually allow the enumeration to continue
        // using the old handle but other weird things might happen
        // when we hit a referral
        if (clnt != eClnt) {
            throw new CommunicationException(
                "Context's connection changed; unable to continue enumeration");
        }

        try {
            return eClnt.getSearchReply(batchSize, res, binaryAttrs);
        } catch (IOException e) {
            NamingException e2 = new CommunicationException(e.getMessage());
            e2.setRootCause(e);
            throw e2;
        }
    }

    // Perform a search. Expect 1 SearchResultEntry and the SearchResultDone.
    private LdapResult doSearchOnce(Name name, String filter,
        SearchControls cons, boolean relative) throws NamingException {

        int savedBatchSize = batchSize;
        batchSize = 2; // 2 protocol elements

        LdapResult answer = doSearch(name, filter, cons, relative, true);

        batchSize = savedBatchSize;
        return answer;
    }

    private LdapResult doSearch(Name name, String filter, SearchControls cons,
        boolean relative, boolean waitForReply) throws NamingException {
            ensureOpen();
            try {
                int scope;

                switch (cons.getSearchScope()) {
                case SearchControls.OBJECT_SCOPE:
                    scope = LdapClient.SCOPE_BASE_OBJECT;
                    break;
                default:
                case SearchControls.ONELEVEL_SCOPE:
                    scope = LdapClient.SCOPE_ONE_LEVEL;
                    break;
                case SearchControls.SUBTREE_SCOPE:
                    scope = LdapClient.SCOPE_SUBTREE;
                    break;
                }

                // If cons.getReturningObjFlag() then caller should already
                // have make sure to request the appropriate attrs

                String[] retattrs = cons.getReturningAttributes();
                if (retattrs != null && retattrs.length == 0) {
                    // Ldap treats null and empty array the same
                    // need to replace with single element array
                    retattrs = new String[1];
                    retattrs[0] = "1.1";
                }

                String nm = (relative
                             ? fullyQualifiedName(name)
                             : (name.isEmpty()
                                ? ""
                                : name.get(0)));

                // JNDI unit is milliseconds, LDAP unit is seconds.
                // Zero means no limit.
                int msecLimit = cons.getTimeLimit();
                int secLimit = 0;

                if (msecLimit > 0) {
                    secLimit = (msecLimit / 1000) + 1;
                }

                LdapResult answer =
                    clnt.search(nm,
                        scope,
                        derefAliases,
                        (int)cons.getCountLimit(),
                        secLimit,
                        cons.getReturningObjFlag() ? false : typesOnly,
                        retattrs,
                        filter,
                        batchSize,
                        reqCtls,
                        binaryAttrs,
                        waitForReply,
                        replyQueueSize);
                respCtls = answer.resControls; // retrieve response controls
                return answer;

            } catch (IOException e) {
                NamingException e2 = new CommunicationException(e.getMessage());
                e2.setRootCause(e);
                throw e2;
            }
    }


    /*
     * Certain simple JNDI searches are automatically converted to
     * LDAP compare operations by the LDAP service provider. A search
     * is converted to a compare iff:
     *
     *    - the scope is set to OBJECT_SCOPE
     *    - the filter string contains a simple assertion: "<type>=<value>"
     *    - the returning attributes list is present but empty
     */

    // returns true if a search can be carried out as a compare, and sets
    // tokens[0] and tokens[1] to the type and value respectively.
    // e.g. filter "cn=Jon Ruiz" becomes, type "cn" and value "Jon Ruiz"
    // This function uses the documents JNDI Compare example as a model
    // for when to turn a search into a compare.

    private static boolean searchToCompare(
                                    String filter,
                                    SearchControls cons,
                                    String tokens[]) {

        // if scope is not object-scope, it's really a search
        if (cons.getSearchScope() != SearchControls.OBJECT_SCOPE) {
            return false;
        }

        // if attributes are to be returned, it's really a search
        String[] attrs = cons.getReturningAttributes();
        if (attrs == null || attrs.length != 0) {
            return false;
        }

        // if the filter not a simple assertion, it's really a search
        if (! filterToAssertion(filter, tokens)) {
            return false;
        }

        // it can be converted to a compare
        return true;
    }

    // If the supplied filter is a simple assertion i.e. "<type>=<value>"
    // (enclosing parentheses are permitted) then
    // filterToAssertion will return true and pass the type and value as
    // the first and second elements of tokens respectively.
    // precondition: tokens[] must be initialized and be at least of size 2.

    private static boolean filterToAssertion(String filter, String tokens[]) {

        // find the left and right half of the assertion
        StringTokenizer assertionTokenizer = new StringTokenizer(filter, "=");

        if (assertionTokenizer.countTokens() != 2) {
            return false;
        }

        tokens[0] = assertionTokenizer.nextToken();
        tokens[1] = assertionTokenizer.nextToken();

        // make sure the value does not contain a wildcard
        if (tokens[1].indexOf('*') != -1) {
            return false;
        }

        // test for enclosing parenthesis
        boolean hasParens = false;
        int len = tokens[1].length();

        if ((tokens[0].charAt(0) == '(') &&
            (tokens[1].charAt(len - 1) == ')')) {
            hasParens = true;

        } else if ((tokens[0].charAt(0) == '(') ||
            (tokens[1].charAt(len - 1) == ')')) {
            return false; // unbalanced
        }

        // make sure the left and right half are not expressions themselves
        StringTokenizer illegalCharsTokenizer =
            new StringTokenizer(tokens[0], "()&|!=~><*", true);

        if (illegalCharsTokenizer.countTokens() != (hasParens ? 2 : 1)) {
            return false;
        }

        illegalCharsTokenizer =
            new StringTokenizer(tokens[1], "()&|!=~><*", true);

        if (illegalCharsTokenizer.countTokens() != (hasParens ? 2 : 1)) {
            return false;
        }

        // strip off enclosing parenthesis, if present
        if (hasParens) {
            tokens[0] = tokens[0].substring(1);
            tokens[1] = tokens[1].substring(0, len - 1);
        }

        return true;
    }

    private LdapResult compare(Name name, String type, String value)
        throws IOException, NamingException {

        ensureOpen();
        String nm = fullyQualifiedName(name);

        LdapResult answer = clnt.compare(nm, type, value, reqCtls);
        respCtls = answer.resControls; // retrieve response controls

        return answer;
    }

    private static SearchControls cloneSearchControls(SearchControls cons) {
        if (cons == null) {
            return null;
        }
        String[] retAttrs = cons.getReturningAttributes();
        if (retAttrs != null) {
            String[] attrs = new String[retAttrs.length];
            System.arraycopy(retAttrs, 0, attrs, 0, retAttrs.length);
            retAttrs = attrs;
        }
        return new SearchControls(cons.getSearchScope(),
                                  cons.getCountLimit(),
                                  cons.getTimeLimit(),
                                  retAttrs,
                                  cons.getReturningObjFlag(),
                                  cons.getDerefLinkFlag());
    }

   // -------------- Environment Properties ------------------

    /**
     * Override with noncloning version.
     */
    protected Hashtable<String, Object> p_getEnvironment() {
        return envprops;
    }

    @SuppressWarnings("unchecked") // clone()
    public Hashtable<String, Object> getEnvironment() throws NamingException {
        return (envprops == null
                ? new Hashtable<String, Object>(5, 0.75f)
                : (Hashtable<String, Object>)envprops.clone());
    }

    @SuppressWarnings("unchecked") // clone()
    public Object removeFromEnvironment(String propName)
        throws NamingException {

        // not there; just return
        if (envprops == null || envprops.get(propName) == null) {
            return null;
        }
        switch (propName) {
            case REF_SEPARATOR:
                addrEncodingSeparator = DEFAULT_REF_SEPARATOR;
                break;
            case TYPES_ONLY:
                typesOnly = DEFAULT_TYPES_ONLY;
                break;
            case DELETE_RDN:
                deleteRDN = DEFAULT_DELETE_RDN;
                break;
            case DEREF_ALIASES:
                derefAliases = DEFAULT_DEREF_ALIASES;
                break;
            case Context.BATCHSIZE:
                batchSize = DEFAULT_BATCH_SIZE;
                break;
            case REFERRAL_LIMIT:
                referralHopLimit = DEFAULT_REFERRAL_LIMIT;
                break;
            case Context.REFERRAL:
                setReferralMode(null, true);
                break;
            case BINARY_ATTRIBUTES:
                setBinaryAttributes(null);
                break;
            case CONNECT_TIMEOUT:
                connectTimeout = -1;
                break;
            case READ_TIMEOUT:
                readTimeout = -1;
                break;
            case WAIT_FOR_REPLY:
                waitForReply = true;
                break;
            case REPLY_QUEUE_SIZE:
                replyQueueSize = -1;
                break;

            // The following properties affect the connection

            case Context.SECURITY_PROTOCOL:
                closeConnection(SOFT_CLOSE);
                // De-activate SSL and reset the context's url and port number
                if (useSsl && !hasLdapsScheme) {
                    useSsl = false;
                    url = null;
                    if (useDefaultPortNumber) {
                        port_number = DEFAULT_PORT;
                    }
                }
                break;
            case VERSION:
            case SOCKET_FACTORY:
                closeConnection(SOFT_CLOSE);
                break;
            case Context.SECURITY_AUTHENTICATION:
            case Context.SECURITY_PRINCIPAL:
            case Context.SECURITY_CREDENTIALS:
                sharable = false;
                break;
        }

        // Update environment; reconnection will use new props
        envprops = (Hashtable<String, Object>)envprops.clone();
        return envprops.remove(propName);
    }

    @SuppressWarnings("unchecked") // clone()
    public Object addToEnvironment(String propName, Object propVal)
        throws NamingException {

            // If adding null, call remove
            if (propVal == null) {
                return removeFromEnvironment(propName);
            }
            switch (propName) {
                case REF_SEPARATOR:
                    setRefSeparator((String)propVal);
                    break;
                case TYPES_ONLY:
                    setTypesOnly((String)propVal);
                    break;
                case DELETE_RDN:
                    setDeleteRDN((String)propVal);
                    break;
                case DEREF_ALIASES:
                    setDerefAliases((String)propVal);
                    break;
                case Context.BATCHSIZE:
                    setBatchSize((String)propVal);
                    break;
                case REFERRAL_LIMIT:
                    setReferralLimit((String)propVal);
                    break;
                case Context.REFERRAL:
                    setReferralMode((String)propVal, true);
                    break;
                case BINARY_ATTRIBUTES:
                    setBinaryAttributes((String)propVal);
                    break;
                case CONNECT_TIMEOUT:
                    setConnectTimeout((String)propVal);
                    break;
                case READ_TIMEOUT:
                    setReadTimeout((String)propVal);
                    break;
                case WAIT_FOR_REPLY:
                    setWaitForReply((String)propVal);
                    break;
                case REPLY_QUEUE_SIZE:
                    setReplyQueueSize((String)propVal);
                    break;

            // The following properties affect the connection

                case Context.SECURITY_PROTOCOL:
                    closeConnection(SOFT_CLOSE);
                    // Activate SSL and reset the context's url and port number
                    if ("ssl".equals(propVal)) {
                        useSsl = true;
                        url = null;
                        if (useDefaultPortNumber) {
                            port_number = DEFAULT_SSL_PORT;
                        }
                    }
                    break;
                case VERSION:
                case SOCKET_FACTORY:
                    closeConnection(SOFT_CLOSE);
                    break;
                case Context.SECURITY_AUTHENTICATION:
                case Context.SECURITY_PRINCIPAL:
                case Context.SECURITY_CREDENTIALS:
                    sharable = false;
                    break;
            }

            // Update environment; reconnection will use new props
            envprops = (envprops == null
                ? new Hashtable<String, Object>(5, 0.75f)
                : (Hashtable<String, Object>)envprops.clone());
            return envprops.put(propName, propVal);
    }

    /**
     * Sets the URL that created the context in the java.naming.provider.url
     * property.
     */
    void setProviderUrl(String providerUrl) { // called by LdapCtxFactory
        if (envprops != null) {
            envprops.put(Context.PROVIDER_URL, providerUrl);
        }
    }

    /**
     * Sets the domain name for the context in the com.sun.jndi.ldap.domainname
     * property.
     * Used for hostname verification by Start TLS
     */
    void setDomainName(String domainName) { // called by LdapCtxFactory
        if (envprops != null) {
            envprops.put(DOMAIN_NAME, domainName);
        }
    }

    private void initEnv() throws NamingException {
        if (envprops == null) {
            // Make sure that referrals are to their default
            setReferralMode(null, false);
            return;
        }

        // Set batch size
        setBatchSize((String)envprops.get(Context.BATCHSIZE));

        // Set separator used for encoding RefAddr
        setRefSeparator((String)envprops.get(REF_SEPARATOR));

        // Set whether RDN is removed when renaming object
        setDeleteRDN((String)envprops.get(DELETE_RDN));

        // Set whether types are returned only
        setTypesOnly((String)envprops.get(TYPES_ONLY));

        // Set how aliases are dereferenced
        setDerefAliases((String)envprops.get(DEREF_ALIASES));

        // Set the limit on referral chains
        setReferralLimit((String)envprops.get(REFERRAL_LIMIT));

        setBinaryAttributes((String)envprops.get(BINARY_ATTRIBUTES));

        bindCtls = cloneControls((Control[]) envprops.get(BIND_CONTROLS));

        // set referral handling
        setReferralMode((String)envprops.get(Context.REFERRAL), false);

        // Set the connect timeout
        setConnectTimeout((String)envprops.get(CONNECT_TIMEOUT));

        // Set the read timeout
        setReadTimeout((String)envprops.get(READ_TIMEOUT));

        // Set the flag that controls whether to block until the first reply
        // is received
        setWaitForReply((String)envprops.get(WAIT_FOR_REPLY));

        // Set the size of the queue of unprocessed search replies
        setReplyQueueSize((String)envprops.get(REPLY_QUEUE_SIZE));

        // When connection is created, it will use these and other
        // properties from the environment
    }

    private void setDeleteRDN(String deleteRDNProp) {
        if ((deleteRDNProp != null) &&
            (deleteRDNProp.equalsIgnoreCase("false"))) {
            deleteRDN = false;
        } else {
            deleteRDN = DEFAULT_DELETE_RDN;
        }
    }

    private void setTypesOnly(String typesOnlyProp) {
        if ((typesOnlyProp != null) &&
            (typesOnlyProp.equalsIgnoreCase("true"))) {
            typesOnly = true;
        } else {
            typesOnly = DEFAULT_TYPES_ONLY;
        }
    }

    /**
     * Sets the batch size of this context;
     */
    private void setBatchSize(String batchSizeProp) {
        // set batchsize
        if (batchSizeProp != null) {
            batchSize = Integer.parseInt(batchSizeProp);
        } else {
            batchSize = DEFAULT_BATCH_SIZE;
        }
    }

    /**
     * Sets the referral mode of this context to 'follow', 'throw' or 'ignore'.
     * If referral mode is 'ignore' then activate the manageReferral control.
     */
    private void setReferralMode(String ref, boolean update) {
        // First determine the referral mode
        if (ref != null) {
            switch (ref) {
                case "follow-scheme":
                    handleReferrals = LdapClient.LDAP_REF_FOLLOW_SCHEME;
                    break;
                case "follow":
                    handleReferrals = LdapClient.LDAP_REF_FOLLOW;
                    break;
                case "throw":
                    handleReferrals = LdapClient.LDAP_REF_THROW;
                    break;
                case "ignore":
                    handleReferrals = LdapClient.LDAP_REF_IGNORE;
                    break;
                default:
                    throw new IllegalArgumentException(
                        "Illegal value for " + Context.REFERRAL + " property.");
            }
        } else {
            handleReferrals = DEFAULT_REFERRAL_MODE;
        }

        if (handleReferrals == LdapClient.LDAP_REF_IGNORE) {
            // If ignoring referrals, add manageReferralControl
            reqCtls = addControl(reqCtls, manageReferralControl);

        } else if (update) {

            // If we're update an existing context, remove the control
            reqCtls = removeControl(reqCtls, manageReferralControl);

        } // else, leave alone; need not update
    }

    /**
     * Set whether aliases are dereferenced during resolution and searches.
     */
    private void setDerefAliases(String deref) {
        if (deref != null) {
            switch (deref) {
                case "never":
                    derefAliases = 0; // never de-reference aliases
                    break;
                case "searching":
                    derefAliases = 1; // de-reference aliases during searching
                    break;
                case "finding":
                    derefAliases = 2; // de-reference during name resolution
                    break;
                case "always":
                    derefAliases = 3; // always de-reference aliases
                    break;
                default:
                    throw new IllegalArgumentException("Illegal value for " +
                        DEREF_ALIASES + " property.");
            }
        } else {
            derefAliases = DEFAULT_DEREF_ALIASES;
        }
    }

    private void setRefSeparator(String sepStr) throws NamingException {
        if (sepStr != null && sepStr.length() > 0) {
            addrEncodingSeparator = sepStr.charAt(0);
        } else {
            addrEncodingSeparator = DEFAULT_REF_SEPARATOR;
        }
    }

    /**
     * Sets the limit on referral chains
     */
    private void setReferralLimit(String referralLimitProp) {
        // set referral limit
        if (referralLimitProp != null) {
            referralHopLimit = Integer.parseInt(referralLimitProp);

            // a zero setting indicates no limit
            if (referralHopLimit == 0)
                referralHopLimit = Integer.MAX_VALUE;
        } else {
            referralHopLimit = DEFAULT_REFERRAL_LIMIT;
        }
    }

    // For counting referral hops
    void setHopCount(int hopCount) {
        this.hopCount = hopCount;
    }

    /**
     * Sets the connect timeout value
     */
    private void setConnectTimeout(String connectTimeoutProp) {
        if (connectTimeoutProp != null) {
            connectTimeout = Integer.parseInt(connectTimeoutProp);
        } else {
            connectTimeout = -1;
        }
    }

    /**
     * Sets the size of the queue of unprocessed search replies
     */
    private void setReplyQueueSize(String replyQueueSizeProp) {
        if (replyQueueSizeProp != null) {
           replyQueueSize = Integer.parseInt(replyQueueSizeProp);
            // disallow an empty queue
            if (replyQueueSize <= 0) {
                replyQueueSize = -1;    // unlimited
            }
        } else {
            replyQueueSize = -1;        // unlimited
        }
    }

    /**
     * Sets the flag that controls whether to block until the first search
     * reply is received
     */
    private void setWaitForReply(String waitForReplyProp) {
        if (waitForReplyProp != null &&
            (waitForReplyProp.equalsIgnoreCase("false"))) {
            waitForReply = false;
        } else {
            waitForReply = true;
        }
    }

    /**
     * Sets the read timeout value
     */
    private void setReadTimeout(String readTimeoutProp) {
        if (readTimeoutProp != null) {
           readTimeout = Integer.parseInt(readTimeoutProp);
        } else {
            readTimeout = -1;
        }
    }

    /*
     * Extract URLs from a string. The format of the string is:
     *
     *     <urlstring > ::= "Referral:" <ldapurls>
     *     <ldapurls>   ::= <separator> <ldapurl> | <ldapurls>
     *     <separator>  ::= ASCII linefeed character (0x0a)
     *     <ldapurl>    ::= LDAP URL format (RFC 1959)
     *
     * Returns a Vector of single-String Vectors.
     */
    private static Vector<Vector<String>> extractURLs(String refString) {

        int separator = 0;
        int urlCount = 0;

        // count the number of URLs
        while ((separator = refString.indexOf('\n', separator)) >= 0) {
            separator++;
            urlCount++;
        }

        Vector<Vector<String>> referrals = new Vector<>(urlCount);
        int iURL;
        int i = 0;

        separator = refString.indexOf('\n');
        iURL = separator + 1;
        while ((separator = refString.indexOf('\n', iURL)) >= 0) {
            Vector<String> referral = new Vector<>(1);
            referral.addElement(refString.substring(iURL, separator));
            referrals.addElement(referral);
            iURL = separator + 1;
        }
        Vector<String> referral = new Vector<>(1);
        referral.addElement(refString.substring(iURL));
        referrals.addElement(referral);

        return referrals;
    }

    /*
     * Argument is a space-separated list of attribute IDs
     * Converts attribute IDs to lowercase before adding to built-in list.
     */
    private void setBinaryAttributes(String attrIds) {
        if (attrIds == null) {
            binaryAttrs = null;
        } else {
            binaryAttrs = new Hashtable<>(11, 0.75f);
            StringTokenizer tokens =
                new StringTokenizer(attrIds.toLowerCase(Locale.ENGLISH), " ");

            while (tokens.hasMoreTokens()) {
                binaryAttrs.put(tokens.nextToken(), Boolean.TRUE);
            }
        }
    }

   // ----------------- Connection  ---------------------

    @SuppressWarnings("deprecation")
    protected void finalize() {
        try {
            close();
        } catch (NamingException e) {
            // ignore failures
        }
    }

    synchronized public void close() throws NamingException {
        if (debug) {
            System.err.println("LdapCtx: close() called " + this);
            (new Throwable()).printStackTrace();
        }

        // Event (normal and unsolicited)
        if (eventSupport != null) {
            eventSupport.cleanup(); // idempotent
            removeUnsolicited();
        }

        // Enumerations that are keeping the connection alive
        if (enumCount > 0) {
            if (debug)
                System.err.println("LdapCtx: close deferred");
            closeRequested = true;
            return;
        }
        closeConnection(SOFT_CLOSE);

// %%%: RL: There is no need to set these to null, as they're just
// variables whose contents and references will automatically
// be cleaned up when they're no longer referenced.
// Also, setting these to null creates problems for the attribute
// schema-related methods, which need these to work.
/*
        schemaTrees = null;
        envprops = null;
*/
    }

    @SuppressWarnings("unchecked") // clone()
    public void reconnect(Control[] connCtls) throws NamingException {
        // Update environment
        envprops = (envprops == null
                ? new Hashtable<String, Object>(5, 0.75f)
                : (Hashtable<String, Object>)envprops.clone());

        if (connCtls == null) {
            envprops.remove(BIND_CONTROLS);
            bindCtls = null;
        } else {
            envprops.put(BIND_CONTROLS, bindCtls = cloneControls(connCtls));
        }

        sharable = false;  // can't share with existing contexts
        ensureOpen();      // open or reauthenticated
    }

    // Load 'mechsAllowedToSendCredentials' system property value
    @SuppressWarnings("removal")
    private static String getMechsAllowedToSendCredentials() {
        PrivilegedAction<String> pa = () -> System.getProperty(ALLOWED_MECHS_SP);
        return System.getSecurityManager() == null ? pa.run() : AccessController.doPrivileged(pa);
    }

    // Get set of allowed authentication mechanism names from the property value
    private static Set<String> getMechsFromPropertyValue(String propValue) {
        if (propValue == null || propValue.isBlank()) {
            return Collections.emptySet();
        }
        return Arrays.stream(propValue.split(","))
                .map(String::trim)
                .filter(Predicate.not(String::isBlank))
                .collect(Collectors.toUnmodifiableSet());
    }

    // Returns true if TLS connection opened using "ldaps" scheme, or using "ldap" and then upgraded with
    // startTLS extended operation, and startTLS is still active.
    private boolean isConnectionEncrypted() {
        return hasLdapsScheme || clnt.isUpgradedToStartTls();
    }

    // Ensure connection and context are in a safe state to transmit credentials
    private void ensureCanTransmitCredentials(String authMechanism) throws NamingException {

        // "none" and "anonumous" authentication mechanisms are allowed unconditionally
        if ("none".equalsIgnoreCase(authMechanism) || "anonymous".equalsIgnoreCase(authMechanism)) {
            return;
        }

        // Check environment first
        String allowedMechanismsOrTrue = (String) envprops.get(ALLOWED_MECHS_SP);
        boolean useSpMechsCache = false;
        boolean anyPropertyIsSet = ALLOWED_MECHS_SP_VALUE != null || allowedMechanismsOrTrue != null;

        // If current connection is not encrypted, and context seen to be secured with STARTTLS
        // or 'mechsAllowedToSendCredentials' is set to any value via system/context environment properties
        if (!isConnectionEncrypted() && (contextSeenStartTlsEnabled || anyPropertyIsSet)) {
            // First, check if security principal is provided in context environment for "simple"
            // authentication mechanism. There is no check for other SASL mechanisms since the credentials
            // can be specified via other properties
            if ("simple".equalsIgnoreCase(authMechanism) && !envprops.containsKey(SECURITY_PRINCIPAL)) {
                return;
            }

            // If null - will use mechanism name cached from system property
            if (allowedMechanismsOrTrue == null) {
                useSpMechsCache = true;
                allowedMechanismsOrTrue = ALLOWED_MECHS_SP_VALUE;
            }

            // If the property value (system or environment) is 'all':
            // any kind of authentication is allowed unconditionally - no check is needed
            if ("all".equalsIgnoreCase(allowedMechanismsOrTrue)) {
                return;
            }

            // Get the set with allowed authentication mechanisms and check current mechanism
            Set<String> allowedAuthMechs = useSpMechsCache ?
                    MECHS_ALLOWED_BY_SP : getMechsFromPropertyValue(allowedMechanismsOrTrue);
            if (!allowedAuthMechs.contains(authMechanism)) {
                throw new NamingException(UNSECURED_CRED_TRANSMIT_MSG);
            }
        }
    }

    private void ensureOpen() throws NamingException {
        ensureOpen(false);
    }

    private void ensureOpen(boolean startTLS) throws NamingException {

        try {
            if (clnt == null) {
                if (debug) {
                    System.err.println("LdapCtx: Reconnecting " + this);
                }

                // reset the cache before a new connection is established
                schemaTrees = new Hashtable<>(11, 0.75f);
                connect(startTLS);

            } else if (!sharable || startTLS) {

                synchronized (clnt) {
                    if (!clnt.isLdapv3
                        || clnt.referenceCount > 1
                        || clnt.usingSaslStreams()
                        || !clnt.conn.useable) {
                        closeConnection(SOFT_CLOSE);
                    }
                }
                // reset the cache before a new connection is established
                schemaTrees = new Hashtable<>(11, 0.75f);
                connect(startTLS);
            }

        } finally {
            sharable = true;   // connection is now either new or single-use
                               // OK for others to start sharing again
        }
    }

    private void connect(boolean startTLS) throws NamingException {
        if (debug) { System.err.println("LdapCtx: Connecting " + this); }

        String user = null;             // authenticating user
        Object passwd = null;           // password for authenticating user
        String secProtocol = null;      // security protocol (e.g. "ssl")
        String socketFactory = null;    // socket factory
        String authMechanism = null;    // authentication mechanism
        String ver = null;
        int ldapVersion;                // LDAP protocol version
        boolean usePool = false;        // enable connection pooling

        if (envprops != null) {
            user = (String)envprops.get(Context.SECURITY_PRINCIPAL);
            passwd = envprops.get(Context.SECURITY_CREDENTIALS);
            ver = (String)envprops.get(VERSION);
            secProtocol =
               useSsl ? "ssl" : (String)envprops.get(Context.SECURITY_PROTOCOL);
            socketFactory = (String)envprops.get(SOCKET_FACTORY);
            authMechanism =
                (String)envprops.get(Context.SECURITY_AUTHENTICATION);

            usePool = "true".equalsIgnoreCase((String)envprops.get(ENABLE_POOL));
        }

        if (socketFactory == null) {
            socketFactory =
                "ssl".equals(secProtocol) ? DEFAULT_SSL_FACTORY : null;
        }

        if (authMechanism == null) {
            authMechanism = (user == null) ? "none" : "simple";
        }

        try {
            boolean initial = (clnt == null);

            if (initial) {
                ldapVersion = (ver != null) ? Integer.parseInt(ver) :
                    DEFAULT_LDAP_VERSION;

                clnt = LdapClient.getInstance(
                    usePool, // Whether to use connection pooling

                    // Required for LdapClient constructor
                    hostname,
                    port_number,
                    socketFactory,
                    connectTimeout,
                    readTimeout,
                    trace,

                    // Required for basic client identity
                    ldapVersion,
                    authMechanism,
                    bindCtls,
                    secProtocol,

                    // Required for simple client identity
                    user,
                    passwd,

                    // Required for SASL client identity
                    envprops);

                // Mark current context as secure if the connection is acquired
                // from the pool and it is secure.
                contextSeenStartTlsEnabled |= clnt.isUpgradedToStartTls();

                /**
                 * Pooled connections are preauthenticated;
                 * newly created ones are not.
                 */
                if (clnt.authenticateCalled()) {
                    return;
                }

            } else if (sharable && startTLS) {
                return; // no authentication required

            } else {
                // reauthenticating over existing connection;
                // only v3 supports this
                ldapVersion = LdapClient.LDAP_VERSION3;
            }

            LdapResult answer;
            synchronized (clnt.conn.startTlsLock) {
                ensureCanTransmitCredentials(authMechanism);
                answer = clnt.authenticate(initial, user, passwd, ldapVersion,
                        authMechanism, bindCtls, envprops);
            }

            respCtls = answer.resControls; // retrieve (bind) response controls

            if (answer.status != LdapClient.LDAP_SUCCESS) {
                if (initial) {
                    closeConnection(HARD_CLOSE);  // hard close
                }
                processReturnCode(answer);
            }

        } catch (LdapReferralException e) {
            if (handleReferrals == LdapClient.LDAP_REF_THROW)
                throw e;

            String referral;
            LdapURL url;
            NamingException saved_ex = null;

            // Process the referrals sequentially (top level) and
            // recursively (per referral)
            while (true) {

                if ((referral = e.getNextReferral()) == null) {
                    // No more referrals to follow

                    if (saved_ex != null) {
                        throw (NamingException)(saved_ex.fillInStackTrace());
                    } else {
                        // No saved exception, something must have gone wrong
                        throw new NamingException(
                        "Internal error processing referral during connection");
                    }
                }

                // Use host/port number from referral
                url = new LdapURL(referral);
                hostname = url.getHost();
                if ((hostname != null) && (hostname.charAt(0) == '[')) {
                    hostname = hostname.substring(1, hostname.length() - 1);
                }
                port_number = url.getPort();

                // Try to connect again using new host/port number
                try {
                    connect(startTLS);
                    break;

                } catch (NamingException ne) {
                    saved_ex = ne;
                    continue; // follow another referral
                }
            }
        }
    }

    private void closeConnection(boolean hardclose) {
        removeUnsolicited();            // idempotent

        if (clnt != null) {
            if (debug) {
                System.err.println("LdapCtx: calling clnt.close() " + this);
            }
            clnt.close(reqCtls, hardclose);
            clnt = null;
        }
    }

    // Used by Enum classes to track whether it still needs context
    private int enumCount = 0;
    private boolean closeRequested = false;

    synchronized void incEnumCount() {
        ++enumCount;
        if (debug) System.err.println("LdapCtx: " + this + " enum inc: " + enumCount);
    }

    synchronized void decEnumCount() {
        --enumCount;
        if (debug) System.err.println("LdapCtx: " + this + " enum dec: " + enumCount);

        if (enumCount == 0 && closeRequested) {
            try {
                close();
            } catch (NamingException e) {
                // ignore failures
            }
        }
    }


   // ------------ Return code and Error messages  -----------------------

    protected void processReturnCode(LdapResult answer) throws NamingException {
        processReturnCode(answer, null, this, null, envprops, null);
    }

    void processReturnCode(LdapResult answer, Name remainName)
    throws NamingException {
        processReturnCode(answer,
                          (new CompositeName()).add(currentDN),
                          this,
                          remainName,
                          envprops,
                          fullyQualifiedName(remainName));
    }

    protected void processReturnCode(LdapResult res, Name resolvedName,
        Object resolvedObj, Name remainName, Hashtable<?,?> envprops, String fullDN)
    throws NamingException {

        String msg = LdapClient.getErrorMessage(res.status, res.errorMessage);
        NamingException e;
        LdapReferralException r = null;

        switch (res.status) {

        case LdapClient.LDAP_SUCCESS:

            // handle Search continuation references
            if (res.referrals != null) {

                msg = "Unprocessed Continuation Reference(s)";

                if (handleReferrals == LdapClient.LDAP_REF_IGNORE) {
                    e = new PartialResultException(msg);
                    break;
                }

                // handle multiple sets of URLs
                int contRefCount = res.referrals.size();
                LdapReferralException head = null;
                LdapReferralException ptr = null;

                msg = "Continuation Reference";

                // make a chain of LdapReferralExceptions
                for (int i = 0; i < contRefCount; i++) {

                    r = new LdapReferralException(resolvedName, resolvedObj,
                        remainName, msg, envprops, fullDN, handleReferrals,
                        reqCtls);
                    r.setReferralInfo(res.referrals.elementAt(i), true);

                    if (hopCount > 1) {
                        r.setHopCount(hopCount);
                    }

                    if (head == null) {
                        head = ptr = r;
                    } else {
                        ptr.nextReferralEx = r; // append ex. to end of chain
                        ptr = r;
                    }
                }
                res.referrals = null;  // reset

                if (res.refEx == null) {
                    res.refEx = head;

                } else {
                    ptr = res.refEx;

                    while (ptr.nextReferralEx != null) {
                        ptr = ptr.nextReferralEx;
                    }
                    ptr.nextReferralEx = head;
                }

                // check the hop limit
                if (hopCount > referralHopLimit) {
                    NamingException lee =
                        new LimitExceededException("Referral limit exceeded");
                    lee.setRootCause(r);
                    throw lee;
                }
            }
            return;

        case LdapClient.LDAP_REFERRAL:

            if (handleReferrals == LdapClient.LDAP_REF_IGNORE) {
                e = new PartialResultException(msg);
                break;
            }

            r = new LdapReferralException(resolvedName, resolvedObj, remainName,
                msg, envprops, fullDN, handleReferrals, reqCtls);
            // only one set of URLs is present
            Vector<String> refs;
            if (res.referrals == null) {
                refs = null;
            } else if (handleReferrals == LdapClient.LDAP_REF_FOLLOW_SCHEME) {
                refs = new Vector<>();
                for (String s : res.referrals.elementAt(0)) {
                    if (s.startsWith("ldap:")) {
                        refs.add(s);
                    }
                }
                if (refs.isEmpty()) {
                    refs = null;
                }
            } else {
                refs = res.referrals.elementAt(0);
            }
            r.setReferralInfo(refs, false);

            if (hopCount > 1) {
                r.setHopCount(hopCount);
            }

            // check the hop limit
            if (hopCount > referralHopLimit) {
                NamingException lee =
                    new LimitExceededException("Referral limit exceeded");
                lee.setRootCause(r);
                e = lee;

            } else {
                e = r;
            }
            break;

        /*
         * Handle SLAPD-style referrals.
         *
         * Referrals received during name resolution should be followed
         * until one succeeds - the target entry is located. An exception
         * is thrown now to handle these.
         *
         * Referrals received during a search operation point to unexplored
         * parts of the directory and each should be followed. An exception
         * is thrown later (during results enumeration) to handle these.
         */

        case LdapClient.LDAP_PARTIAL_RESULTS:

            if (handleReferrals == LdapClient.LDAP_REF_IGNORE) {
                e = new PartialResultException(msg);
                break;
            }

            // extract SLAPD-style referrals from errorMessage
            if ((res.errorMessage != null) && (!res.errorMessage.isEmpty())) {
                res.referrals = extractURLs(res.errorMessage);
            } else {
                e = new PartialResultException(msg);
                break;
            }

            // build exception
            r = new LdapReferralException(resolvedName,
                resolvedObj,
                remainName,
                msg,
                envprops,
                fullDN,
                handleReferrals,
                reqCtls);

            if (hopCount > 1) {
                r.setHopCount(hopCount);
            }
            /*
             * %%%
             * SLAPD-style referrals received during name resolution
             * cannot be distinguished from those received during a
             * search operation. Since both must be handled differently
             * the following rule is applied:
             *
             *     If 1 referral and 0 entries is received then
             *     assume name resolution has not yet completed.
             */
            if (((res.entries == null) || (res.entries.isEmpty())) &&
                ((res.referrals != null) && (res.referrals.size() == 1))) {

                r.setReferralInfo(res.referrals, false);

                // check the hop limit
                if (hopCount > referralHopLimit) {
                    NamingException lee =
                        new LimitExceededException("Referral limit exceeded");
                    lee.setRootCause(r);
                    e = lee;

                } else {
                    e = r;
                }

            } else {
                r.setReferralInfo(res.referrals, true);
                res.refEx = r;
                return;
            }
            break;

        case LdapClient.LDAP_INVALID_DN_SYNTAX:
        case LdapClient.LDAP_NAMING_VIOLATION:

            if (remainName != null) {
                e = new
                    InvalidNameException(remainName.toString() + ": " + msg);
            } else {
                e = new InvalidNameException(msg);
            }
            break;

        default:
            e = mapErrorCode(res.status, res.errorMessage);
            break;
        }
        e.setResolvedName(resolvedName);
        e.setResolvedObj(resolvedObj);
        e.setRemainingName(remainName);
        throw e;
    }

    /**
     * Maps an LDAP error code to an appropriate NamingException.
     * %%% public; used by controls
     *
     * @param errorCode numeric LDAP error code
     * @param errorMessage textual description of the LDAP error. May be null.
     *
     * @return A NamingException or null if the error code indicates success.
     */
    public static NamingException mapErrorCode(int errorCode,
        String errorMessage) {

        if (errorCode == LdapClient.LDAP_SUCCESS)
            return null;

        NamingException e = null;
        String message = LdapClient.getErrorMessage(errorCode, errorMessage);

        switch (errorCode) {

        case LdapClient.LDAP_ALIAS_DEREFERENCING_PROBLEM:
            e = new NamingException(message);
            break;

        case LdapClient.LDAP_ALIAS_PROBLEM:
            e = new NamingException(message);
            break;

        case LdapClient.LDAP_ATTRIBUTE_OR_VALUE_EXISTS:
            e = new AttributeInUseException(message);
            break;

        case LdapClient.LDAP_AUTH_METHOD_NOT_SUPPORTED:
        case LdapClient.LDAP_CONFIDENTIALITY_REQUIRED:
        case LdapClient.LDAP_STRONG_AUTH_REQUIRED:
        case LdapClient.LDAP_INAPPROPRIATE_AUTHENTICATION:
            e = new AuthenticationNotSupportedException(message);
            break;

        case LdapClient.LDAP_ENTRY_ALREADY_EXISTS:
            e = new NameAlreadyBoundException(message);
            break;

        case LdapClient.LDAP_INVALID_CREDENTIALS:
        case LdapClient.LDAP_SASL_BIND_IN_PROGRESS:
            e = new AuthenticationException(message);
            break;

        case LdapClient.LDAP_INAPPROPRIATE_MATCHING:
            e = new InvalidSearchFilterException(message);
            break;

        case LdapClient.LDAP_INSUFFICIENT_ACCESS_RIGHTS:
            e = new NoPermissionException(message);
            break;

        case LdapClient.LDAP_INVALID_ATTRIBUTE_SYNTAX:
        case LdapClient.LDAP_CONSTRAINT_VIOLATION:
            e =  new InvalidAttributeValueException(message);
            break;

        case LdapClient.LDAP_LOOP_DETECT:
            e = new NamingException(message);
            break;

        case LdapClient.LDAP_NO_SUCH_ATTRIBUTE:
            e = new NoSuchAttributeException(message);
            break;

        case LdapClient.LDAP_NO_SUCH_OBJECT:
            e = new NameNotFoundException(message);
            break;

        case LdapClient.LDAP_OBJECT_CLASS_MODS_PROHIBITED:
        case LdapClient.LDAP_OBJECT_CLASS_VIOLATION:
        case LdapClient.LDAP_NOT_ALLOWED_ON_RDN:
            e = new SchemaViolationException(message);
            break;

        case LdapClient.LDAP_NOT_ALLOWED_ON_NON_LEAF:
            e = new ContextNotEmptyException(message);
            break;

        case LdapClient.LDAP_OPERATIONS_ERROR:
            // %%% need new exception ?
            e = new NamingException(message);
            break;

        case LdapClient.LDAP_OTHER:
            e = new NamingException(message);
            break;

        case LdapClient.LDAP_PROTOCOL_ERROR:
            e = new CommunicationException(message);
            break;

        case LdapClient.LDAP_SIZE_LIMIT_EXCEEDED:
            e = new SizeLimitExceededException(message);
            break;

        case LdapClient.LDAP_TIME_LIMIT_EXCEEDED:
            e = new TimeLimitExceededException(message);
            break;

        case LdapClient.LDAP_UNAVAILABLE_CRITICAL_EXTENSION:
            e = new OperationNotSupportedException(message);
            break;

        case LdapClient.LDAP_UNAVAILABLE:
        case LdapClient.LDAP_BUSY:
            e = new ServiceUnavailableException(message);
            break;

        case LdapClient.LDAP_UNDEFINED_ATTRIBUTE_TYPE:
            e = new InvalidAttributeIdentifierException(message);
            break;

        case LdapClient.LDAP_UNWILLING_TO_PERFORM:
            e = new OperationNotSupportedException(message);
            break;

        case LdapClient.LDAP_COMPARE_FALSE:
        case LdapClient.LDAP_COMPARE_TRUE:
        case LdapClient.LDAP_IS_LEAF:
            // these are really not exceptions and this code probably
            // never gets executed
            e = new NamingException(message);
            break;

        case LdapClient.LDAP_ADMIN_LIMIT_EXCEEDED:
            e = new LimitExceededException(message);
            break;

        case LdapClient.LDAP_REFERRAL:
            e = new NamingException(message);
            break;

        case LdapClient.LDAP_PARTIAL_RESULTS:
            e = new NamingException(message);
            break;

        case LdapClient.LDAP_INVALID_DN_SYNTAX:
        case LdapClient.LDAP_NAMING_VIOLATION:
            e = new InvalidNameException(message);
            break;

        default:
            e = new NamingException(message);
            break;
        }

        return e;
    }

    // ----------------- Extensions and Controls -------------------

    public ExtendedResponse extendedOperation(ExtendedRequest request)
        throws NamingException {

        boolean startTLS = (request.getID().equals(STARTTLS_REQ_OID));
        ensureOpen(startTLS);

        try {

            LdapResult answer =
                clnt.extendedOp(request.getID(), request.getEncodedValue(),
                                reqCtls, startTLS);
            respCtls = answer.resControls; // retrieve response controls

            if (answer.status != LdapClient.LDAP_SUCCESS) {
                processReturnCode(answer, new CompositeName());
            }
            // %%% verify request.getID() == answer.extensionId

            int len = (answer.extensionValue == null) ?
                        0 :
                        answer.extensionValue.length;

            ExtendedResponse er =
                request.createExtendedResponse(answer.extensionId,
                    answer.extensionValue, 0, len);

            if (er instanceof StartTlsResponseImpl) {
                // Pass the connection handle to StartTlsResponseImpl
                String domainName = (String)
                    (envprops != null ? envprops.get(DOMAIN_NAME) : null);
                ((StartTlsResponseImpl)er).setConnection(clnt.conn, domainName);
                contextSeenStartTlsEnabled |= startTLS;
            }
            return er;

        } catch (LdapReferralException e) {

            if (handleReferrals == LdapClient.LDAP_REF_THROW)
                throw e;

            // process the referrals sequentially
            while (true) {

                LdapReferralContext refCtx =
                    (LdapReferralContext)e.getReferralContext(envprops, bindCtls);

                // repeat the original operation at the new context
                try {

                    return refCtx.extendedOperation(request);

                } catch (LdapReferralException re) {
                    e = re;
                    continue;

                } finally {
                    // Make sure we close referral context
                    refCtx.close();
                }
            }

        } catch (IOException e) {
            NamingException e2 = new CommunicationException(e.getMessage());
            e2.setRootCause(e);
            throw e2;
        }
    }

    public void setRequestControls(Control[] reqCtls) throws NamingException {
        if (handleReferrals == LdapClient.LDAP_REF_IGNORE) {
            this.reqCtls = addControl(reqCtls, manageReferralControl);
        } else {
            this.reqCtls = cloneControls(reqCtls);
        }
    }

    public Control[] getRequestControls() throws NamingException {
        return cloneControls(reqCtls);
    }

    public Control[] getConnectControls() throws NamingException {
        return cloneControls(bindCtls);
    }

    public Control[] getResponseControls() throws NamingException {
        return (respCtls != null)? convertControls(respCtls) : null;
    }

    /**
     * Narrow controls using own default factory and ControlFactory.
     * @param ctls A non-null Vector<Control>
     */
    Control[] convertControls(Vector<Control> ctls) throws NamingException {
        int count = ctls.size();

        if (count == 0) {
            return null;
        }

        Control[] controls = new Control[count];

        for (int i = 0; i < count; i++) {
            // Try own factory first
            controls[i] = myResponseControlFactory.getControlInstance(
                ctls.elementAt(i));

            // Try assigned factories if own produced null
            if (controls[i] == null) {
                controls[i] = ControlFactory.getControlInstance(
                ctls.elementAt(i), this, envprops);
            }
        }
        return controls;
    }

    private static Control[] addControl(Control[] prevCtls, Control addition) {
        if (prevCtls == null) {
            return new Control[]{addition};
        }

        // Find it
        int found = findControl(prevCtls, addition);
        if (found != -1) {
            return prevCtls;  // no need to do it again
        }

        Control[] newCtls = new Control[prevCtls.length+1];
        System.arraycopy(prevCtls, 0, newCtls, 0, prevCtls.length);
        newCtls[prevCtls.length] = addition;
        return newCtls;
    }

    private static int findControl(Control[] ctls, Control target) {
        for (int i = 0; i < ctls.length; i++) {
            if (ctls[i] == target) {
                return i;
            }
        }
        return -1;
    }

    private static Control[] removeControl(Control[] prevCtls, Control target) {
        if (prevCtls == null) {
            return null;
        }

        // Find it
        int found = findControl(prevCtls, target);
        if (found == -1) {
            return prevCtls;  // not there
        }

        // Remove it
        Control[] newCtls = new Control[prevCtls.length-1];
        System.arraycopy(prevCtls, 0, newCtls, 0, found);
        System.arraycopy(prevCtls, found+1, newCtls, found,
            prevCtls.length-found-1);
        return newCtls;
    }

    private static Control[] cloneControls(Control[] ctls) {
        if (ctls == null) {
            return null;
        }
        Control[] copiedCtls = new Control[ctls.length];
        System.arraycopy(ctls, 0, copiedCtls, 0, ctls.length);
        return copiedCtls;
    }

    // -------------------- Events ------------------------
    /*
     * Access to eventSupport need not be synchronized even though the
     * Connection thread can access it asynchronously. It is
     * impossible for a race condition to occur because
     * eventSupport.addNamingListener() must have been called before
     * the Connection thread can call back to this ctx.
     */
    public void addNamingListener(Name nm, int scope, NamingListener l)
        throws NamingException {
            addNamingListener(getTargetName(nm), scope, l);
    }

    public void addNamingListener(String nm, int scope, NamingListener l)
        throws NamingException {
            if (eventSupport == null)
                eventSupport = new EventSupport(this);
            eventSupport.addNamingListener(getTargetName(new CompositeName(nm)),
                scope, l);

            // If first time asking for unsol
            if (l instanceof UnsolicitedNotificationListener && !unsolicited) {
                addUnsolicited();
            }
    }

    public void removeNamingListener(NamingListener l) throws NamingException {
        if (eventSupport == null)
            return; // no activity before, so just return

        eventSupport.removeNamingListener(l);

        // If removing an Unsol listener and it is the last one, let clnt know
        if (l instanceof UnsolicitedNotificationListener &&
            !eventSupport.hasUnsolicited()) {
            removeUnsolicited();
        }
    }

    public void addNamingListener(String nm, String filter, SearchControls ctls,
        NamingListener l) throws NamingException {
            if (eventSupport == null)
                eventSupport = new EventSupport(this);
            eventSupport.addNamingListener(getTargetName(new CompositeName(nm)),
                filter, cloneSearchControls(ctls), l);

            // If first time asking for unsol
            if (l instanceof UnsolicitedNotificationListener && !unsolicited) {
                addUnsolicited();
            }
    }

    public void addNamingListener(Name nm, String filter, SearchControls ctls,
        NamingListener l) throws NamingException {
            addNamingListener(getTargetName(nm), filter, ctls, l);
    }

    public void addNamingListener(Name nm, String filter, Object[] filterArgs,
        SearchControls ctls, NamingListener l) throws NamingException {
            addNamingListener(getTargetName(nm), filter, filterArgs, ctls, l);
    }

    public void addNamingListener(String nm, String filterExpr, Object[] filterArgs,
        SearchControls ctls, NamingListener l) throws NamingException {
        String strfilter = SearchFilter.format(filterExpr, filterArgs);
        addNamingListener(getTargetName(new CompositeName(nm)), strfilter, ctls, l);
    }

    public boolean targetMustExist() {
        return true;
    }

    /**
     * Retrieves the target name for which the listener is registering.
     * If nm is a CompositeName, use its first and only component. It
     * cannot have more than one components because a target be outside of
     * this namespace. If nm is not a CompositeName, then treat it as a
     * compound name.
     * @param nm The non-null target name.
     */
    private static String getTargetName(Name nm) throws NamingException {
        if (nm instanceof CompositeName) {
            if (nm.size() > 1) {
                throw new InvalidNameException(
                    "Target cannot span multiple namespaces: " + nm);
            } else if (nm.isEmpty()) {
                return "";
            } else {
                return nm.get(0);
            }
        } else {
            // treat as compound name
            return nm.toString();
        }
    }

    // ------------------ Unsolicited Notification ---------------
    // package private methods for handling unsolicited notification

    /**
     * Registers this context with the underlying LdapClient.
     * When the underlying LdapClient receives an unsolicited notification,
     * it will invoke LdapCtx.fireUnsolicited() so that this context
     * can (using EventSupport) notified any registered listeners.
     * This method is called by EventSupport when an unsolicited listener
     * first registers with this context (should be called just once).
     * @see #removeUnsolicited
     * @see #fireUnsolicited
     */
    private void addUnsolicited() throws NamingException {
        if (debug) {
            System.out.println("LdapCtx.addUnsolicited: " + this);
        }

        // addNamingListener must have created EventSupport already
        ensureOpen();
        synchronized (eventSupport) {
            clnt.addUnsolicited(this);
            unsolicited = true;
        }
    }

    /**
     * Removes this context from registering interest in unsolicited
     * notifications from the underlying LdapClient. This method is called
     * under any one of the following conditions:
     * <ul>
     * <li>All unsolicited listeners have been removed. (see removingNamingListener)
     * <li>This context is closed.
     * <li>This context's underlying LdapClient changes.
     *</ul>
     * After this method has been called, this context will not pass
     * on any events related to unsolicited notifications to EventSupport and
     * and its listeners.
     */

    private void removeUnsolicited() {
        if (debug) {
            System.out.println("LdapCtx.removeUnsolicited: " + unsolicited);
        }
        if (eventSupport == null) {
            return;
        }

        // addNamingListener must have created EventSupport already
        synchronized(eventSupport) {
            if (unsolicited && clnt != null) {
                clnt.removeUnsolicited(this);
            }
            unsolicited = false;
        }
    }

    /**
     * Uses EventSupport to fire an event related to an unsolicited notification.
     * Called by LdapClient when LdapClient receives an unsolicited notification.
     */
    void fireUnsolicited(Object obj) {
        if (debug) {
            System.out.println("LdapCtx.fireUnsolicited: " + obj);
        }
        // addNamingListener must have created EventSupport already
        synchronized(eventSupport) {
            if (unsolicited) {
                eventSupport.fireUnsolicited(obj);

                if (obj instanceof NamingException) {
                    unsolicited = false;
                    // No need to notify clnt because clnt is the
                    // only one that can fire a NamingException to
                    // unsol listeners and it will handle its own cleanup
                }
            }
        }
    }
}
