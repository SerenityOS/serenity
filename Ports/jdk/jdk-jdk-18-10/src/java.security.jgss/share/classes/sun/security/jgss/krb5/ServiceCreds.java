/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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

package sun.security.jgss.krb5;

import javax.security.auth.kerberos.KerberosTicket;
import javax.security.auth.kerberos.KerberosKey;
import javax.security.auth.kerberos.KerberosPrincipal;
import javax.security.auth.kerberos.KeyTab;
import javax.security.auth.Subject;

import sun.security.krb5.Credentials;
import sun.security.krb5.EncryptionKey;
import sun.security.krb5.KrbException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import sun.security.krb5.*;
import sun.security.krb5.internal.Krb5;

/**
 * Credentials of a kerberos acceptor. A KerberosPrincipal object (kp) is
 * the principal. It can be specified as the serverPrincipal argument
 * in the getInstance() method, or uses only KerberosPrincipal in the subject.
 * Otherwise, the creds object is unbound and kp is null.
 *
 * The class also encapsulates various secrets, which can be:
 *
 *   1. Some KerberosKeys (generated from password)
 *   2. Some KeyTabs (for a typical service based on keytabs)
 *   3. A TGT (for S4U2proxy extension or user2user)
 *
 * Note that some secrets can coexist. For example, a user2user service
 * can use its keytab (or keys) if the client can successfully obtain a
 * normal service ticket, or it can use the TGT (actually, the session key
 * of the TGT) if the client can only acquire a service ticket
 * of ENC-TKT-IN-SKEY style.
 *
 * @since 1.8
 */
public final class ServiceCreds {
    // The principal, or null if unbound
    private KerberosPrincipal kp;

    // All principals in the subject's princ set
    private Set<KerberosPrincipal> allPrincs;

    // All private credentials that can be used
    private List<KeyTab> ktabs;
    private List<KerberosKey> kk;
    private KerberosTicket tgt;

    private boolean destroyed;

    private ServiceCreds() {
        // Make sure this class cannot be instantiated externally.
    }

    /**
     * Creates a ServiceCreds object based on info in a Subject for
     * a given principal name (if specified).
     * @return the object, or null if there is no private creds for it
     */
    public static ServiceCreds getInstance(
            Subject subj, String serverPrincipal) {

        ServiceCreds sc = new ServiceCreds();

        sc.allPrincs =
                subj.getPrincipals(KerberosPrincipal.class);

        // Compatibility. A key implies its own principal
        for (KerberosKey key: SubjectComber.findMany(
                subj, serverPrincipal, null, KerberosKey.class)) {
            sc.allPrincs.add(key.getPrincipal());
        }

        if (serverPrincipal != null) {      // A named principal
            sc.kp = new KerberosPrincipal(serverPrincipal);
        } else {
            // For compatibility reason, we set the name of default principal
            // to the "only possible" name it can take, which means there is
            // only one KerberosPrincipal and there is no unbound keytabs
            if (sc.allPrincs.size() == 1) {
                boolean hasUnbound = false;
                for (KeyTab ktab: SubjectComber.findMany(
                        subj, null, null, KeyTab.class)) {
                    if (!ktab.isBound()) {
                        hasUnbound = true;
                        break;
                    }
                }
                if (!hasUnbound) {
                    sc.kp = sc.allPrincs.iterator().next();
                    serverPrincipal = sc.kp.getName();
                }
            }
        }

        sc.ktabs = SubjectComber.findMany(
                    subj, serverPrincipal, null, KeyTab.class);
        sc.kk = SubjectComber.findMany(
                    subj, serverPrincipal, null, KerberosKey.class);
        sc.tgt = SubjectComber.find(
                subj, null, serverPrincipal, KerberosTicket.class);
        if (sc.ktabs.isEmpty() && sc.kk.isEmpty() && sc.tgt == null) {
            return null;
        }

        sc.destroyed = false;

        return sc;
    }

    // can be null
    public String getName() {
        if (destroyed) {
            throw new IllegalStateException("This object is destroyed");
        }
        return kp == null ? null : kp.getName();
    }

    /**
     * Gets keys for "someone". Used in 2 cases:
     * 1. By TLS because it needs to get keys before client comes in.
     * 2. As a fallback in getEKeys() below.
     * This method can still return an empty array.
     */
    public KerberosKey[] getKKeys() {
        if (destroyed) {
            throw new IllegalStateException("This object is destroyed");
        }
        KerberosPrincipal one = kp;                 // named principal
        if (one == null && !allPrincs.isEmpty()) {  // or, a known principal
            one = allPrincs.iterator().next();
        }
        if (one == null) {                          // Or, some random one
            for (KeyTab ktab: ktabs) {
                // Must be unbound keytab, otherwise, allPrincs is not empty
                PrincipalName pn =
                        Krb5Util.snapshotFromJavaxKeyTab(ktab).getOneName();
                if (pn != null) {
                    one = new KerberosPrincipal(pn.getName());
                    break;
                }
            }
        }
        if (one != null) {
            return getKKeys(one);
        } else {
            return new KerberosKey[0];
        }
    }

    /**
     * Get kkeys for a principal,
     * @param princ the target name initiator requests. Not null.
     * @return keys for the princ, never null, might be empty
     */
    public KerberosKey[] getKKeys(KerberosPrincipal princ) {
        if (destroyed) {
            throw new IllegalStateException("This object is destroyed");
        }
        ArrayList<KerberosKey> keys = new ArrayList<>();
        if (kp != null && !princ.equals(kp)) {      // named principal
            return new KerberosKey[0];
        }
        for (KerberosKey k: kk) {
            if (k.getPrincipal().equals(princ)) {
                keys.add(k);
            }
        }
        for (KeyTab ktab: ktabs) {
            if (ktab.getPrincipal() == null && ktab.isBound()) {
                // legacy bound keytab. although we don't know who
                // the bound principal is, it must be in allPrincs
                if (!allPrincs.contains(princ)) {
                    continue;   // skip this legacy bound keytab
                }
            }
            for (KerberosKey k: ktab.getKeys(princ)) {
                keys.add(k);
            }
        }
        return keys.toArray(new KerberosKey[keys.size()]);
    }

    /**
     * Gets EKeys for a principal.
     * @param princ the target name initiator requests. Not null.
     * @return keys for the princ, never null, might be empty
     */
    public EncryptionKey[] getEKeys(PrincipalName princ) {
        if (destroyed) {
            throw new IllegalStateException("This object is destroyed");
        }
        KerberosKey[] kkeys = getKKeys(new KerberosPrincipal(princ.getName()));
        if (kkeys.length == 0) {
            // Fallback: old JDK does not perform real name checking. If the
            // acceptor has host.sun.com but initiator requests for host,
            // as long as their keys match (i.e. keys for one can decrypt
            // the other's service ticket), the authentication is OK.
            // There are real customers depending on this to use different
            // names for a single service.
            kkeys = getKKeys();
        }
        EncryptionKey[] ekeys = new EncryptionKey[kkeys.length];
        for (int i=0; i<ekeys.length; i++) {
            ekeys[i] =  new EncryptionKey(
                        kkeys[i].getEncoded(), kkeys[i].getKeyType(),
                        kkeys[i].getVersionNumber());
        }
        return ekeys;
    }

    public Credentials getInitCred() {
        if (destroyed) {
            throw new IllegalStateException("This object is destroyed");
        }
        if (tgt == null) {
            return null;
        }
        try {
            return Krb5Util.ticketToCreds(tgt);
        } catch (KrbException | IOException e) {
            return null;
        }
    }

    public void destroy() {
        // Do not wipe out real keys because they are references to the
        // priv creds in subject. Just make it useless.
        destroyed = true;
        kp = null;
        ktabs.clear();
        kk.clear();
        tgt = null;
    }
}
