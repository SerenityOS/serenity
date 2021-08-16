/*
 * Copyright (c) 2008, 2018, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.security.auth.module.Krb5LoginModule;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.kerberos.KerberosKey;
import javax.security.auth.kerberos.KerberosTicket;
import javax.security.auth.login.LoginContext;
import org.ietf.jgss.GSSContext;
import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSException;
import org.ietf.jgss.GSSManager;
import org.ietf.jgss.GSSName;
import org.ietf.jgss.MessageProp;
import org.ietf.jgss.Oid;
import sun.security.jgss.krb5.Krb5Util;
import sun.security.krb5.Credentials;
import sun.security.krb5.internal.ccache.CredentialsCache;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.security.Principal;
import java.util.Set;

/**
 * Context of a JGSS subject, encapsulating Subject and GSSContext.
 *
 * Three "constructors", which acquire the (private) credentials and fill
 * it into the Subject:
 *
 * 1. static fromJAAS(): Creates a Context using a JAAS login config entry
 * 2. static fromUserPass(): Creates a Context using a username and a password
 * 3. delegated(): A new context which uses the delegated credentials from a
 *    previously established acceptor Context
 *
 * Two context initiators, which create the GSSContext object inside:
 *
 * 1. startAsClient()
 * 2. startAsServer()
 *
 * Privileged action:
 *    doAs(): Performs an action in the name of the Subject
 *
 * Handshake process:
 *    static handShake(initiator, acceptor)
 *
 * A four-phase typical data communication which includes all four GSS
 * actions (wrap, unwrap, getMic and veryfyMiC):
 *    static transmit(message, from, to)
 */
public class Context {

    private Subject s;
    private GSSContext x;
    private String name;
    private GSSCredential cred;     // see static method delegated().

    static boolean usingStream = false;

    private Context() {}

    /**
     * Using the delegated credentials from a previous acceptor
     */
    public Context delegated() throws Exception {
        Context out = new Context();
        out.s = s;
        try {
            out.cred = Subject.doAs(s, new PrivilegedExceptionAction<GSSCredential>() {
                @Override
                public GSSCredential run() throws Exception {
                    GSSCredential cred = x.getDelegCred();
                    if (cred == null && x.getCredDelegState() ||
                            cred != null && !x.getCredDelegState()) {
                        throw new Exception("getCredDelegState not match");
                    }
                    return cred;
                }
            });
        } catch (PrivilegedActionException pae) {
            throw pae.getException();
        }
        out.name = name + " as " + out.cred.getName().toString();
        return out;
    }

    /**
     * No JAAS login at all, can be used to test JGSS without JAAS
     */
    public static Context fromThinAir() throws Exception {
        Context out = new Context();
        out.s = new Subject();
        return out;
    }

    /**
     * Logins with a JAAS login config entry name
     */
    public static Context fromJAAS(final String name) throws Exception {
        Context out = new Context();
        out.name = name;
        LoginContext lc = new LoginContext(name);
        lc.login();
        out.s = lc.getSubject();
        return out;
    }

    /**
     * Logins with username/password as a new Subject
     */
    public static Context fromUserPass(
            String user, char[] pass, boolean storeKey) throws Exception {
        return fromUserPass(new Subject(), user, pass, storeKey);
    }

    /**
     * Logins with username/password as an existing Subject. The
     * same subject can be used multiple times to simulate multiple logins.
     */
    public static Context fromUserPass(Subject s,
            String user, char[] pass, boolean storeKey) throws Exception {
        Context out = new Context();
        out.name = user;
        out.s = s;
        Krb5LoginModule krb5 = new Krb5LoginModule();
        Map<String, String> map = new HashMap<>();
        Map<String, Object> shared = new HashMap<>();

        if (storeKey) {
            map.put("storeKey", "true");
        }

        if (pass != null) {
            krb5.initialize(out.s, new CallbackHandler() {
                @Override
                public void handle(Callback[] callbacks)
                        throws IOException, UnsupportedCallbackException {
                    for (Callback cb: callbacks) {
                        if (cb instanceof NameCallback) {
                            ((NameCallback)cb).setName(user);
                        } else if (cb instanceof PasswordCallback) {
                            ((PasswordCallback)cb).setPassword(pass);
                        }
                    }
                }
            }, shared, map);
        } else {
            map.put("doNotPrompt", "true");
            map.put("useTicketCache", "true");
            if (user != null) {
                map.put("principal", user);
            }
            krb5.initialize(out.s, null, shared, map);
        }

        krb5.login();
        krb5.commit();

        return out;
    }

    /**
     * Logins with username/keytab as an existing Subject. The
     * same subject can be used multiple times to simulate multiple logins.
     */
    public static Context fromUserKtab(
            String user, String ktab, boolean storeKey) throws Exception {
        return fromUserKtab(new Subject(), user, ktab, storeKey);
    }

    /**
     * Logins with username/keytab as a new subject,
     */
    public static Context fromUserKtab(Subject s,
            String user, String ktab, boolean storeKey) throws Exception {
        Context out = new Context();
        out.name = user;
        out.s = s;
        Krb5LoginModule krb5 = new Krb5LoginModule();
        Map<String, String> map = new HashMap<>();

        map.put("isInitiator", "false");
        map.put("doNotPrompt", "true");
        map.put("useTicketCache", "false");
        map.put("useKeyTab", "true");
        map.put("keyTab", ktab);
        map.put("principal", user);
        if (storeKey) {
            map.put("storeKey", "true");
        }

        krb5.initialize(out.s, null, null, map);
        krb5.login();
        krb5.commit();
        return out;
    }

    /**
     * Starts as a client
     * @param target communication peer
     * @param mech GSS mech
     * @throws java.lang.Exception
     */
    public void startAsClient(final String target, final Oid mech) throws Exception {
        doAs(new Action() {
            @Override
            public byte[] run(Context me, byte[] dummy) throws Exception {
                GSSManager m = GSSManager.getInstance();
                me.x = m.createContext(
                          target.indexOf('@') < 0 ?
                            m.createName(target, null) :
                            m.createName(target, GSSName.NT_HOSTBASED_SERVICE),
                        mech,
                        cred,
                        GSSContext.DEFAULT_LIFETIME);
                return null;
            }
        }, null);
    }

    /**
     * Starts as a server
     * @param mech GSS mech
     * @throws java.lang.Exception
     */
    public void startAsServer(final Oid mech) throws Exception {
        startAsServer(null, mech, false);
    }

    public void startAsServer(final String name, final Oid mech) throws Exception {
        startAsServer(name, mech, false);
    }
    /**
     * Starts as a server with the specified service name
     * @param name the service name
     * @param mech GSS mech
     * @throws java.lang.Exception
     */
    public void startAsServer(final String name, final Oid mech, final boolean asInitiator) throws Exception {
        doAs(new Action() {
            @Override
            public byte[] run(Context me, byte[] dummy) throws Exception {
                GSSManager m = GSSManager.getInstance();
                me.cred = m.createCredential(
                        name == null ? null :
                          (name.indexOf('@') < 0 ?
                            m.createName(name, null) :
                            m.createName(name, GSSName.NT_HOSTBASED_SERVICE)),
                        GSSCredential.INDEFINITE_LIFETIME,
                        mech,
                        asInitiator?
                                GSSCredential.INITIATE_AND_ACCEPT:
                                GSSCredential.ACCEPT_ONLY);
                me.x = m.createContext(me.cred);
                return null;
            }
        }, null);
    }

    /**
     * Accesses the internal GSSContext object. Currently it's used for --
     *
     * 1. calling requestXXX() before handshake
     * 2. accessing source name
     *
     * Note: If the application needs to do any privileged call on this
     * object, please use doAs(). Otherwise, it can be done directly. The
     * methods listed above are all non-privileged calls.
     *
     * @return the GSSContext object
     */
    public GSSContext x() {
        return x;
    }

    /**
     * Accesses the internal subject.
     * @return the subject
     */
    public Subject s() {
        return s;
    }

    /**
     * Returns the cred inside, if there is one
     */
    public GSSCredential cred() {
        return cred;
    }

    /**
     * Disposes the GSSContext within
     * @throws org.ietf.jgss.GSSException
     */
    public void dispose() throws GSSException {
        x.dispose();
    }

    /**
     * Does something using the Subject inside
     * @param action the action
     * @param in the input byte
     * @return the output byte
     * @throws java.lang.Exception
     */
    public byte[] doAs(final Action action, final byte[] in) throws Exception {
        try {
            return Subject.doAs(s, new PrivilegedExceptionAction<byte[]>() {

                @Override
                public byte[] run() throws Exception {
                    return action.run(Context.this, in);
                }
            });
        } catch (PrivilegedActionException pae) {
            throw pae.getException();
        }
    }

    /**
     * Prints status of GSSContext and Subject
     * @throws java.lang.Exception
     */
    public void status() throws Exception {
        System.out.println("STATUS OF " + name.toUpperCase());
        if (x != null) {
            StringBuffer sb = new StringBuffer();
            if (x.getAnonymityState()) {
                sb.append("anon, ");
            }
            if (x.getConfState()) {
                sb.append("conf, ");
            }
            if (x.getCredDelegState()) {
                sb.append("deleg, ");
            }
            if (x.getIntegState()) {
                sb.append("integ, ");
            }
            if (x.getMutualAuthState()) {
                sb.append("mutual, ");
            }
            if (x.getReplayDetState()) {
                sb.append("rep det, ");
            }
            if (x.getSequenceDetState()) {
                sb.append("seq det, ");
            }
            System.out.println("   Context status of " + name + ": " + sb.toString());
            if (x.isProtReady() || x.isEstablished()) {
                System.out.println("   " + x.getSrcName() + " -> " + x.getTargName());
            }
        }
        xstatus();
        if (s != null) {
            System.out.println("====== START SUBJECT CONTENT =====");
            for (Principal p : s.getPrincipals()) {
                System.out.println("    Principal: " + p);
            }
            for (Object o : s.getPublicCredentials()) {
                System.out.println("    " + o.getClass());
                System.out.println("        " + o);
            }
            System.out.println("====== Private Credentials Set ======");
            for (Object o : s.getPrivateCredentials()) {
                System.out.println("    " + o.getClass());
                if (o instanceof KerberosTicket) {
                    KerberosTicket kt = (KerberosTicket) o;
                    System.out.println("        " + kt.getServer() + " for " + kt.getClient());
                } else if (o instanceof KerberosKey) {
                    KerberosKey kk = (KerberosKey) o;
                    System.out.print("        " + kk.getKeyType() + " " + kk.getVersionNumber() + " " + kk.getAlgorithm() + " ");
                    for (byte b : kk.getEncoded()) {
                        System.out.printf("%02X", b & 0xff);
                    }
                    System.out.println();
                } else if (o instanceof Map) {
                    Map map = (Map) o;
                    for (Object k : map.keySet()) {
                        System.out.println("        " + k + ": " + map.get(k));
                    }
                } else {
                    System.out.println("        " + o);
                }
            }
            System.out.println("====== END SUBJECT CONTENT =====");
        }
    }

    public void xstatus() throws Exception {
        System.out.println("   Extended context status:");
        if (x != null) {
            try {
                Class<?> clazz = Class.forName("com.sun.security.jgss.ExtendedGSSContext");
                if (clazz.isAssignableFrom(x.getClass())) {
                    if (clazz.getMethod("getDelegPolicyState").invoke(x) == Boolean.TRUE) {
                        System.out.println("   deleg policy");
                    }
                    if (x.isEstablished()) {
                        Class<?> inqType = Class.forName("com.sun.security.jgss.InquireType");
                        Method inqMethod = clazz.getMethod("inquireSecContext", inqType);
                        for (Object o : inqType.getEnumConstants()) {
                            System.out.println("   " + o + ":");
                            try {
                                System.out.println("      " + inqMethod.invoke(x, o));
                            } catch (Exception e) {
                                System.out.println(e.getCause());
                            }
                        }
                    }
                }
            } catch (ClassNotFoundException cnfe) {
                System.out.println("   -- ExtendedGSSContext not available");
            }
        }
        if (cred != null) {
            try {
                Class<?> clazz2 = Class.forName("com.sun.security.jgss.ExtendedGSSCredential");
                if (!clazz2.isAssignableFrom(cred.getClass())) {
                    throw new Exception("cred is not extended");
                }
            } catch (ClassNotFoundException cnfe) {
                System.out.println("   -- ExtendedGSSCredential not available");
            }
        }
    }

    public byte[] wrap(byte[] t, final boolean privacy)
            throws Exception {
        return doAs(new Action() {
            @Override
            public byte[] run(Context me, byte[] input) throws Exception {
                System.out.printf("wrap %s privacy from %s: ", privacy?"with":"without", me.name);
                MessageProp p1 = new MessageProp(0, privacy);
                byte[] out;
                if (usingStream) {
                    ByteArrayOutputStream os = new ByteArrayOutputStream();
                    me.x.wrap(new ByteArrayInputStream(input), os, p1);
                    out = os.toByteArray();
                } else {
                    out = me.x.wrap(input, 0, input.length, p1);
                }
                System.out.println(printProp(p1));
                if ((x.getConfState() && privacy) != p1.getPrivacy()) {
                    throw new Exception("unexpected privacy status");
                }
                return out;
            }
        }, t);
    }

    public byte[] unwrap(byte[] t, final boolean privacyExpected)
            throws Exception {
        return doAs(new Action() {
            @Override
            public byte[] run(Context me, byte[] input) throws Exception {
                System.out.printf("unwrap from %s", me.name);
                MessageProp p1 = new MessageProp(0, true);
                byte[] bytes;
                if (usingStream) {
                    ByteArrayOutputStream os = new ByteArrayOutputStream();
                    me.x.unwrap(new ByteArrayInputStream(input), os, p1);
                    bytes = os.toByteArray();
                } else {
                    bytes = me.x.unwrap(input, 0, input.length, p1);
                }
                System.out.println(printProp(p1));
                if (p1.getPrivacy() != privacyExpected) {
                    throw new Exception("Unexpected privacy: " + p1.getPrivacy());
                }
                return bytes;
            }
        }, t);
    }

    public byte[] getMic(byte[] t) throws Exception {
        return doAs(new Action() {
            @Override
            public byte[] run(Context me, byte[] input) throws Exception {
                MessageProp p1 = new MessageProp(0, true);
                byte[] bytes;
                p1 = new MessageProp(0, true);
                System.out.printf("getMic from %s: ", me.name);
                if (usingStream) {
                    ByteArrayOutputStream os = new ByteArrayOutputStream();
                    me.x.getMIC(new ByteArrayInputStream(input), os, p1);
                    bytes = os.toByteArray();
                } else {
                    bytes = me.x.getMIC(input, 0, input.length, p1);
                }
                System.out.println(printProp(p1));
                return bytes;
            }
        }, t);
    }

    public void verifyMic(byte[] t, final byte[] msg) throws Exception {
        doAs(new Action() {
            @Override
            public byte[] run(Context me, byte[] input) throws Exception {
                MessageProp p1 = new MessageProp(0, true);
                System.out.printf("verifyMic from %s: ", me.name);
                if (usingStream) {
                    me.x.verifyMIC(new ByteArrayInputStream(input),
                            new ByteArrayInputStream(msg), p1);
                } else {
                    me.x.verifyMIC(input, 0, input.length,
                            msg, 0, msg.length,
                            p1);
                }
                System.out.println(printProp(p1));
                if (p1.isUnseqToken() || p1.isOldToken()
                        || p1.isDuplicateToken() || p1.isGapToken()) {
                    throw new Exception("Wrong sequence number detected");
                }
                return null;
            }
        }, t);
    }

    /**
     * Transmits a message from one Context to another. The sender wraps the
     * message and sends it to the receiver. The receiver unwraps it, creates
     * a MIC of the clear text and sends it back to the sender. The sender
     * verifies the MIC against the message sent earlier.
     * @param message the message
     * @param s1 the sender
     * @param s2 the receiver
     * @throws java.lang.Exception If anything goes wrong
     */
    static public void transmit(String message, final Context s1,
                                final Context s2) throws Exception {
        transmit(message.getBytes(), s1, s2);
    }

    /**
     * Transmits a message from one Context to another. The sender wraps the
     * message and sends it to the receiver. The receiver unwraps it, creates
     * a MIC of the clear text and sends it back to the sender. The sender
     * verifies the MIC against the message sent earlier.
     * @param messageBytes the message
     * @param s1 the sender
     * @param s2 the receiver
     * @throws java.lang.Exception If anything goes wrong
     */
    static public void transmit(byte[] messageBytes, final Context s1,
            final Context s2) throws Exception {
        System.out.printf("-------------------- TRANSMIT from %s to %s------------------------\n",
                s1.name, s2.name);
        byte[] wrapped = s1.wrap(messageBytes, true);
        byte[] unwrapped = s2.unwrap(wrapped, s2.x.getConfState());
        if (!Arrays.equals(messageBytes, unwrapped)) {
            throw new Exception("wrap/unwrap mismatch");
        }
        byte[] mic = s2.getMic(unwrapped);
        s1.verifyMic(mic, messageBytes);
    }

    /**
     * Returns a string description of a MessageProp object
     * @param prop the object
     * @return the description
     */
    static public String printProp(MessageProp prop) {
        StringBuffer sb = new StringBuffer();
        sb.append("MessagePop: ");
        sb.append("QOP="+ prop.getQOP() + ", ");
        sb.append(prop.getPrivacy()?"privacy, ":"");
        sb.append(prop.isDuplicateToken()?"dup, ":"");
        sb.append(prop.isGapToken()?"gap, ":"");
        sb.append(prop.isOldToken()?"old, ":"");
        sb.append(prop.isUnseqToken()?"unseq, ":"");
        if (prop.getMinorStatus() != 0) {
            sb.append(prop.getMinorString()+ "(" + prop.getMinorStatus()+")");
        }
        return sb.toString();
    }

    public Context impersonate(final String someone) throws Exception {
        try {
            GSSCredential creds = Subject.doAs(s, new PrivilegedExceptionAction<GSSCredential>() {
                @Override
                public GSSCredential run() throws Exception {
                    GSSManager m = GSSManager.getInstance();
                    GSSName other = m.createName(someone, GSSName.NT_USER_NAME);
                    if (Context.this.cred == null) {
                        Context.this.cred = m.createCredential(GSSCredential.INITIATE_ONLY);
                    }
                    return (GSSCredential)
                            Class.forName("com.sun.security.jgss.ExtendedGSSCredential")
                            .getMethod("impersonate", GSSName.class)
                            .invoke(Context.this.cred, other);
                }
            });
            Context out = new Context();
            out.s = s;
            out.cred = creds;
            out.name = name + " as " + out.cred.getName().toString();
            return out;
        } catch (PrivilegedActionException pae) {
            Exception e = pae.getException();
            if (e instanceof InvocationTargetException) {
                throw (Exception)((InvocationTargetException) e).getTargetException();
            } else {
                throw e;
            }
        }
    }

    public byte[] take(final byte[] in) throws Exception {
        return doAs(new Action() {
            @Override
            public byte[] run(Context me, byte[] input) throws Exception {
                if (me.x.isEstablished()) {
                    System.out.println(name + " side established");
                    if (input != null) {
                        throw new Exception("Context established but " +
                                "still receive token at " + name);
                    }
                    return null;
                } else {
                    if (me.x.isInitiator()) {
                        System.out.println(name + " call initSecContext");
                        return me.x.initSecContext(input, 0, input.length);
                    } else {
                        System.out.println(name + " call acceptSecContext");
                        return me.x.acceptSecContext(input, 0, input.length);
                    }
                }
            }
        }, in);
    }

    /**
     * Saves the tickets to a ccache file.
     *
     * @param file pathname of the ccache file
     * @return true if created, false otherwise.
     */
    public boolean ccache(String file) throws Exception {
        Set<KerberosTicket> tickets
                = s.getPrivateCredentials(KerberosTicket.class);
        if (tickets != null && !tickets.isEmpty()) {
            CredentialsCache cc = null;
            for (KerberosTicket t : tickets) {
                Credentials cred = Krb5Util.ticketToCreds(t);
                if (cc == null) {
                    cc = CredentialsCache.create(cred.getClient(), file);
                }
                cc.update(cred.toCCacheCreds());
            }
            if (cc != null) {
                cc.save();
                return true;
            }
        }
        return false;
    }

    /**
     * Handshake (security context establishment process) between two Contexts
     * @param c the initiator
     * @param s the acceptor
     * @throws java.lang.Exception
     */
    static public void handshake(final Context c, final Context s) throws Exception {
        byte[] t = new byte[0];
        while (true) {
            if (t != null || !c.x.isEstablished()) t = c.take(t);
            if (t != null || !s.x.isEstablished()) t = s.take(t);
            if (c.x.isEstablished() && s.x.isEstablished()) break;
        }
    }
}
