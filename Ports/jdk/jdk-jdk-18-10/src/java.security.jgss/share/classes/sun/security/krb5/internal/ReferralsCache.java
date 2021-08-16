/*
 * Copyright (c) 2019, Red Hat, Inc.
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

package sun.security.krb5.internal;

import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import sun.security.krb5.Credentials;
import sun.security.krb5.PrincipalName;

/*
 * ReferralsCache class implements a cache scheme for referral TGTs as
 * described in RFC 6806 - 10. Caching Information. The goal is to optimize
 * resources (such as network traffic) when a client requests credentials for a
 * service principal to a given KDC. If a referral TGT was previously received,
 * cached information is used instead of issuing a new query. Once a referral
 * TGT expires, the corresponding referral entry in the cache is removed.
 */
final class ReferralsCache {

    private static Map<ReferralCacheKey, Map<String, ReferralCacheEntry>>
            referralsMap = new HashMap<>();

    private static final class ReferralCacheKey {
        private PrincipalName cname;
        private PrincipalName sname;
        ReferralCacheKey (PrincipalName cname, PrincipalName sname) {
            this.cname = cname;
            this.sname = sname;
        }
        public boolean equals(Object other) {
            if (!(other instanceof ReferralCacheKey))
                return false;
            ReferralCacheKey that = (ReferralCacheKey)other;
            return cname.equals(that.cname) &&
                    sname.equals(that.sname);
        }
        public int hashCode() {
            return cname.hashCode() + sname.hashCode();
        }
    }

    static final class ReferralCacheEntry {
        private final Credentials creds;
        private final String toRealm;
        ReferralCacheEntry(Credentials creds, String toRealm) {
            this.creds = creds;
            this.toRealm = toRealm;
        }
        Credentials getCreds() {
            return creds;
        }
        String getToRealm() {
            return toRealm;
        }
    }

    /*
     * Add a new referral entry to the cache, including: client principal,
     * service principal, source KDC realm, destination KDC realm and
     * referral TGT.
     *
     * If a loop is generated when adding the new referral, the first hop is
     * automatically removed. For example, let's assume that adding a
     * REALM-3.COM -> REALM-1.COM referral generates the following loop:
     * REALM-1.COM -> REALM-2.COM -> REALM-3.COM -> REALM-1.COM. Then,
     * REALM-1.COM -> REALM-2.COM referral entry is removed from the cache.
     */
    static synchronized void put(PrincipalName cname, PrincipalName service,
            String fromRealm, String toRealm, Credentials creds) {
        ReferralCacheKey k = new ReferralCacheKey(cname, service);
        pruneExpired(k);
        if (creds.getEndTime().before(new Date())) {
            return;
        }
        Map<String, ReferralCacheEntry> entries = referralsMap.get(k);
        if (entries == null) {
            entries = new HashMap<String, ReferralCacheEntry>();
            referralsMap.put(k, entries);
        }
        entries.remove(fromRealm);
        ReferralCacheEntry newEntry = new ReferralCacheEntry(creds, toRealm);
        entries.put(fromRealm, newEntry);

        // Remove loops within the cache
        ReferralCacheEntry current = newEntry;
        List<ReferralCacheEntry> seen = new LinkedList<>();
        while (current != null) {
            if (seen.contains(current)) {
                // Loop found. Remove the first referral to cut the loop.
                entries.remove(newEntry.getToRealm());
                break;
            }
            seen.add(current);
            current = entries.get(current.getToRealm());
        }
    }

    /*
     * Obtain a referral entry from the cache given a client principal,
     * service principal and a source KDC realm.
     */
    static synchronized ReferralCacheEntry get(PrincipalName cname,
            PrincipalName service, String fromRealm) {
        ReferralCacheKey k = new ReferralCacheKey(cname, service);
        pruneExpired(k);
        Map<String, ReferralCacheEntry> entries = referralsMap.get(k);
        if (entries != null) {
            ReferralCacheEntry toRef = entries.get(fromRealm);
            if (toRef != null) {
                return toRef;
            }
        }
        return null;
    }

    /*
     * Remove referral entries from the cache when referral TGTs expire.
     */
    private static void pruneExpired(ReferralCacheKey k) {
        Date now = new Date();
        Map<String, ReferralCacheEntry> entries = referralsMap.get(k);
        if (entries != null) {
            for (Entry<String, ReferralCacheEntry> mapEntry :
                    entries.entrySet()) {
                if (mapEntry.getValue().getCreds().getEndTime().before(now)) {
                    entries.remove(mapEntry.getKey());
                }
            }
        }
    }
}
