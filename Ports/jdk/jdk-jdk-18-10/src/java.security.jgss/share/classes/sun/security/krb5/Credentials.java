/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates. All rights reserved.
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

/*
 *
 *  (C) Copyright IBM Corp. 1999 All Rights Reserved.
 *  Copyright 1997 The Open Group Research Institute.  All rights reserved.
 */

package sun.security.krb5;

import sun.security.action.GetPropertyAction;
import sun.security.krb5.internal.*;
import sun.security.krb5.internal.ccache.CredentialsCache;
import sun.security.krb5.internal.crypto.EType;
import java.io.IOException;
import java.util.Date;
import java.util.Locale;
import java.net.InetAddress;

/**
 * This class encapsulates the concept of a Kerberos service
 * credential. That includes a Kerberos ticket and an associated
 * session key.
 */
public class Credentials {

    Ticket ticket;
    PrincipalName client;
    PrincipalName clientAlias;
    PrincipalName server;
    PrincipalName serverAlias;
    EncryptionKey key;
    TicketFlags flags;
    KerberosTime authTime;
    KerberosTime startTime;
    KerberosTime endTime;
    KerberosTime renewTill;
    HostAddresses cAddr;
    AuthorizationData authzData;
    private static boolean DEBUG = Krb5.DEBUG;
    private static CredentialsCache cache;
    static boolean alreadyLoaded = false;
    private static boolean alreadyTried = false;

    private Credentials proxy = null;

    public Credentials getProxy() {
        return proxy;
    }

    public Credentials setProxy(Credentials proxy) {
        this.proxy = proxy;
        return this;
    }

    // Read native ticket with session key type in the given list
    private static native Credentials acquireDefaultNativeCreds(int[] eTypes);

    public Credentials(Ticket new_ticket,
                       PrincipalName new_client,
                       PrincipalName new_client_alias,
                       PrincipalName new_server,
                       PrincipalName new_server_alias,
                       EncryptionKey new_key,
                       TicketFlags new_flags,
                       KerberosTime authTime,
                       KerberosTime new_startTime,
                       KerberosTime new_endTime,
                       KerberosTime renewTill,
                       HostAddresses cAddr,
                       AuthorizationData authzData) {
        this(new_ticket, new_client, new_client_alias, new_server,
                new_server_alias, new_key, new_flags, authTime,
                new_startTime, new_endTime, renewTill, cAddr);
        this.authzData = authzData;
    }

    // Warning: called by NativeCreds.c and nativeccache.c
    public Credentials(Ticket new_ticket,
                       PrincipalName new_client,
                       PrincipalName new_client_alias,
                       PrincipalName new_server,
                       PrincipalName new_server_alias,
                       EncryptionKey new_key,
                       TicketFlags new_flags,
                       KerberosTime authTime,
                       KerberosTime new_startTime,
                       KerberosTime new_endTime,
                       KerberosTime renewTill,
                       HostAddresses cAddr) {
        ticket = new_ticket;
        client = new_client;
        clientAlias = new_client_alias;
        server = new_server;
        serverAlias = new_server_alias;
        key = new_key;
        flags = new_flags;
        this.authTime = authTime;
        startTime = new_startTime;
        endTime = new_endTime;
        this.renewTill = renewTill;
        this.cAddr = cAddr;
    }

    public Credentials(byte[] encoding,
                       String client,
                       String clientAlias,
                       String server,
                       String serverAlias,
                       byte[] keyBytes,
                       int keyType,
                       boolean[] flags,
                       Date authTime,
                       Date startTime,
                       Date endTime,
                       Date renewTill,
                       InetAddress[] cAddrs) throws KrbException, IOException {
        this(new Ticket(encoding),
             new PrincipalName(client, PrincipalName.KRB_NT_PRINCIPAL),
             (clientAlias == null? null : new PrincipalName(clientAlias,
                     PrincipalName.KRB_NT_PRINCIPAL)),
             new PrincipalName(server, PrincipalName.KRB_NT_SRV_INST),
             (serverAlias == null? null : new PrincipalName(serverAlias,
                     PrincipalName.KRB_NT_SRV_INST)),
             new EncryptionKey(keyType, keyBytes),
             (flags == null? null: new TicketFlags(flags)),
             (authTime == null? null: new KerberosTime(authTime)),
             (startTime == null? null: new KerberosTime(startTime)),
             (endTime == null? null: new KerberosTime(endTime)),
             (renewTill == null? null: new KerberosTime(renewTill)),
             null); // caddrs are in the encoding at this point
    }

    /**
     * Acquires a service ticket for the specified service
     * principal. If the service ticket is not already available, it
     * obtains a new one from the KDC.
     */
    /*
    public Credentials(Credentials tgt, PrincipalName service)
        throws KrbException {
    }
    */

    public final PrincipalName getClient() {
        return client;
    }

    public final PrincipalName getClientAlias() {
        return clientAlias;
    }

    public final PrincipalName getServer() {
        return server;
    }

    public final PrincipalName getServerAlias() {
        return serverAlias;
    }

    public final EncryptionKey getSessionKey() {
        return key;
    }

    public final Date getAuthTime() {
        if (authTime != null) {
            return authTime.toDate();
        } else {
            return null;
        }
    }

    public final Date getStartTime() {
        if (startTime != null)
            {
                return startTime.toDate();
            }
        return null;
    }

    public final Date getEndTime() {
        if (endTime != null)
            {
                return endTime.toDate();
            }
        return null;
    }

    public final Date getRenewTill() {
        if (renewTill != null)
            {
                return renewTill.toDate();
            }
        return null;
    }

    public final boolean[] getFlags() {
        if (flags == null) // Can be in a KRB-CRED
        return null;
        return flags.toBooleanArray();
    }

    public final InetAddress[] getClientAddresses() {

        if (cAddr == null)
        return null;

        return cAddr.getInetAddresses();
    }

    public final byte[] getEncoded() {
        byte[] retVal = null;
        try {
            retVal = ticket.asn1Encode();
        } catch (Asn1Exception e) {
            if (DEBUG) {
                System.out.println(e);
            }
        } catch (IOException ioe) {
            if (DEBUG) {
                System.out.println(ioe);
            }
        }
        return retVal;
    }

    public boolean isForwardable() {
        return flags.get(Krb5.TKT_OPTS_FORWARDABLE);
    }

    public boolean isRenewable() {
        return flags.get(Krb5.TKT_OPTS_RENEWABLE);
    }

    public Ticket getTicket() {
        return ticket;
    }

    public TicketFlags getTicketFlags() {
        return flags;
    }

    public AuthorizationData getAuthzData() {
        return authzData;
    }
    /**
     * Checks if the service ticket returned by the KDC has the OK-AS-DELEGATE
     * flag set
     * @return true if OK-AS_DELEGATE flag is set, otherwise, return false.
     */
    public boolean checkDelegate() {
        return flags.get(Krb5.TKT_OPTS_DELEGATE);
    }

    /**
     * Reset TKT_OPTS_DELEGATE to false, called at credentials acquirement
     * when one of the cross-realm TGTs does not have the OK-AS-DELEGATE
     * flag set. This info must be preservable and restorable through
     * the Krb5Util.credsToTicket/ticketToCreds() methods so that even if
     * the service ticket is cached it still remembers the cross-realm
     * authentication result.
     */
    public void resetDelegate() {
        flags.set(Krb5.TKT_OPTS_DELEGATE, false);
    }

    public Credentials renew() throws KrbException, IOException {
        KDCOptions options = new KDCOptions();
        options.set(KDCOptions.RENEW, true);
        /*
         * Added here to pass KrbKdcRep.check:73
         */
        options.set(KDCOptions.RENEWABLE, true);

        return new KrbTgsReq(options,
                             this,
                             server,
                             serverAlias,
                             null, // from
                             null, // till
                             null, // rtime
                             null, // eTypes
                             cAddr,
                             null,
                             null,
                             null).sendAndGetCreds();
    }

    /**
     * Returns a TGT for the given client principal from a ticket cache.
     *
     * @param princ the client principal. A value of null means that the
     * default principal name in the credentials cache will be used.
     * @param ticketCache the path to the tickets file. A value
     * of null will be accepted to indicate that the default
     * path should be searched
     * @return the TGT credentials or null if none were found. If the tgt
     * expired, it is the responsibility of the caller to determine this.
     */
    public static Credentials acquireTGTFromCache(PrincipalName princ,
                                                  String ticketCache)
        throws KrbException, IOException {

        if (ticketCache == null) {
            // The default ticket cache on Windows and Mac is not a file.
            String os = GetPropertyAction.privilegedGetProperty("os.name");
            if (os.toUpperCase(Locale.ENGLISH).startsWith("WINDOWS") ||
                    os.toUpperCase(Locale.ENGLISH).contains("OS X")) {
                Credentials creds = acquireDefaultCreds();
                if (creds == null) {
                    if (DEBUG) {
                        System.out.println(">>> Found no TGT's in native ccache");
                    }
                    return null;
                }
                if (princ != null) {
                    if (creds.getClient().equals(princ)) {
                        if (DEBUG) {
                            System.out.println(">>> Obtained TGT from native ccache: "
                                               + creds);
                        }
                        return creds;
                    } else {
                        if (DEBUG) {
                            System.out.println(">>> native ccache contains TGT for "
                                               + creds.getClient()
                                               + " not "
                                               + princ);
                        }
                        return null;
                    }
                } else {
                    if (DEBUG) {
                        System.out.println(">>> Obtained TGT from native ccache: "
                                           + creds);
                    }
                    return creds;
                }
            }
        }

        /*
         * Returns the appropriate cache. If ticketCache is null, it is the
         * default cache otherwise it is the cache filename contained in it.
         */
        CredentialsCache ccache =
            CredentialsCache.getInstance(princ, ticketCache);

        if (ccache == null) {
            return null;
        }

        Credentials tgtCred = ccache.getInitialCreds();

        if (tgtCred == null) {
            return null;
        }

        if (EType.isSupported(tgtCred.key.getEType())) {
            return tgtCred;
        } else {
            if (DEBUG) {
                System.out.println(
                    ">>> unsupported key type found the default TGT: " +
                    tgtCred.key.getEType());
            }
            return null;
        }
    }

    /**
     * Acquires default credentials.
     * <br>The possible locations for default credentials cache is searched in
     * the following order:
     * <ol>
     * <li> The directory and cache file name specified by "KRB5CCNAME" system.
     * property.
     * <li> The directory and cache file name specified by "KRB5CCNAME"
     * environment variable.
     * <li> A cache file named krb5cc_{user.name} at {user.home} directory.
     * </ol>
     * @return a <code>KrbCreds</code> object if the credential is found,
     * otherwise return null.
     */

    // this method is intentionally changed to not check if the caller's
    // principal name matches cache file's principal name.
    // It assumes that the GSS call has
    // the privilege to access the default cache file.

    // This method is only called on Windows and Mac OS X, the native
    // acquireDefaultNativeCreds is also available on these platforms.
    public static synchronized Credentials acquireDefaultCreds() {
        Credentials result = null;

        if (cache == null) {
            cache = CredentialsCache.getInstance();
        }
        if (cache != null) {
            Credentials temp = cache.getInitialCreds();
            if (temp != null) {
                if (DEBUG) {
                    System.out.println(">>> KrbCreds found the default ticket"
                            + " granting ticket in credential cache.");
                }
                if (EType.isSupported(temp.key.getEType())) {
                    result = temp;
                } else {
                    if (DEBUG) {
                        System.out.println(
                            ">>> unsupported key type found the default TGT: " +
                            temp.key.getEType());
                    }
                }
            }
        }
        if (result == null) {
            // Doesn't seem to be a default cache on this system or
            // TGT has unsupported encryption type

            if (!alreadyTried) {
                // See if there's any native code to load
                try {
                    ensureLoaded();
                } catch (Exception e) {
                    if (DEBUG) {
                        System.out.println("Can not load native ccache library");
                        e.printStackTrace();
                    }
                    alreadyTried = true;
                }
            }
            if (alreadyLoaded) {
                // There is some native code
                if (DEBUG) {
                    System.out.println(">> Acquire default native Credentials");
                }
                try {
                    result = acquireDefaultNativeCreds(
                            EType.getDefaults("default_tkt_enctypes"));
                } catch (KrbException ke) {
                    // when there is no default_tkt_enctypes.
                }
            }
        }
        return result;
    }

    /**
     * Acquires credentials for a specified service using initial credential.
     * When the service has a different realm
     * from the initial credential, we do cross-realm authentication
     * - first, we use the current credential to get
     * a cross-realm credential from the local KDC, then use that
     * cross-realm credential to request service credential
     * from the foreigh KDC.
     *
     * @param service the name of service principal using format
     * components@realm
     * @param ccreds client's initial credential.
     * @exception IOException if an error occurs in reading the credentials
     * cache
     * @exception KrbException if an error occurs specific to Kerberos
     * @return a <code>Credentials</code> object.
     */

    public static Credentials acquireServiceCreds(String service,
                                                  Credentials ccreds)
        throws KrbException, IOException {
        return CredentialsUtil.acquireServiceCreds(service, ccreds);
    }

    public static Credentials acquireS4U2selfCreds(PrincipalName user,
            Credentials ccreds) throws KrbException, IOException {
        return CredentialsUtil.acquireS4U2selfCreds(user, ccreds);
    }

    public static Credentials acquireS4U2proxyCreds(String service,
            Ticket second, PrincipalName client, Credentials ccreds)
        throws KrbException, IOException {
        return CredentialsUtil.acquireS4U2proxyCreds(
                service, second, client, ccreds);
    }

    public CredentialsCache getCache() {
        return cache;
    }

    /*
     * Prints out debug info.
     */
    public static void printDebug(Credentials c) {
        System.out.println(">>> DEBUG: ----Credentials----");
        System.out.println("\tclient: " + c.client.toString());
        if (c.clientAlias != null)
            System.out.println("\tclient alias: " + c.clientAlias.toString());
        System.out.println("\tserver: " + c.server.toString());
        if (c.serverAlias != null)
            System.out.println("\tserver alias: " + c.serverAlias.toString());
        System.out.println("\tticket: sname: " + c.ticket.sname.toString());
        if (c.startTime != null) {
            System.out.println("\tstartTime: " + c.startTime.getTime());
        }
        System.out.println("\tendTime: " + c.endTime.getTime());
        System.out.println("        ----Credentials end----");
    }


    @SuppressWarnings("removal")
    static void ensureLoaded() {
        java.security.AccessController.doPrivileged(
                new java.security.PrivilegedAction<Void> () {
                        public Void run() {
                                if (System.getProperty("os.name").contains("OS X")) {
                                    System.loadLibrary("osxkrb5");
                                } else {
                                    System.loadLibrary("w2k_lsa_auth");
                                }
                                return null;
                        }
                });
        alreadyLoaded = true;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("Credentials:");
        sb.append(    "\n      client=").append(client);
        if (clientAlias != null)
            sb.append(    "\n      clientAlias=").append(clientAlias);
        sb.append(    "\n      server=").append(server);
        if (serverAlias != null)
            sb.append(    "\n      serverAlias=").append(serverAlias);
        if (authTime != null) {
            sb.append("\n    authTime=").append(authTime);
        }
        if (startTime != null) {
            sb.append("\n   startTime=").append(startTime);
        }
        sb.append(    "\n     endTime=").append(endTime);
        sb.append(    "\n   renewTill=").append(renewTill);
        sb.append(    "\n       flags=").append(flags);
        sb.append(    "\nEType (skey)=").append(key.getEType());
        sb.append(    "\n   (tkt key)=").append(ticket.encPart.eType);
        return sb.toString();
    }

    public sun.security.krb5.internal.ccache.Credentials toCCacheCreds() {
        return new sun.security.krb5.internal.ccache.Credentials(
                getClient(), getServer(),
                getSessionKey(),
                date2kt(getAuthTime()),
                date2kt(getStartTime()),
                date2kt(getEndTime()),
                date2kt(getRenewTill()),
                false,
                flags,
                new HostAddresses(getClientAddresses()),
                getAuthzData(),
                getTicket(),
                null);
    }

    private static KerberosTime date2kt(Date d) {
        return d == null ? null : new KerberosTime(d);
    }
}
