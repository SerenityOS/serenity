/*
 * Copyright (c) 2000, 2018, Oracle and/or its affiliates. All rights reserved.
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

package sun.security.krb5.internal.rcache;

import sun.security.krb5.internal.Krb5;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;
import sun.security.krb5.internal.KerberosTime;
import sun.security.krb5.internal.KrbApErrException;

/**
 * This class provides an efficient caching mechanism to store AuthTimeWithHash
 * from client authenticators. The cache minimizes the memory usage by doing
 * self-cleanup of expired items in the cache.
 *
 * AuthTimeWithHash objects inside a cache are always sorted from big (new) to
 * small (old) as determined by {@link AuthTimeWithHash#compareTo}. In the most
 * common case a newcomer should be newer than the first element.
 *
 * @author Yanni Zhang
 */
public class AuthList {

    private final LinkedList<AuthTimeWithHash> entries;
    private final int lifespan;

    // entries.getLast().ctime, updated after each cleanup.
    private volatile int oldestTime = Integer.MIN_VALUE;

    /**
     * Constructs a AuthList.
     */
    public AuthList(int lifespan) {
        this.lifespan = lifespan;
        entries = new LinkedList<>();
    }

    /**
     * Puts the authenticator timestamp into the cache in descending order,
     * and throw an exception if it's already there.
     */
    public synchronized void put(AuthTimeWithHash t, KerberosTime currentTime)
            throws KrbApErrException {

        if (entries.isEmpty()) {
            entries.addFirst(t);
            oldestTime = t.ctime;
            return;
        } else {
            AuthTimeWithHash temp = entries.getFirst();
            int cmp = temp.compareTo(t);
            if (cmp < 0) {
                // This is the most common case, newly received authenticator
                // has larger timestamp.
                entries.addFirst(t);
            } else if (cmp == 0) {
                throw new KrbApErrException(Krb5.KRB_AP_ERR_REPEAT);
            } else {
                //unless client clock being re-adjusted.
                ListIterator<AuthTimeWithHash> it = entries.listIterator(1);
                boolean found = false;
                while (it.hasNext()) {
                    temp = it.next();
                    cmp = temp.compareTo(t);
                    if (cmp < 0) {
                        // Find an older one, put in front of it
                        entries.add(entries.indexOf(temp), t);
                        found = true;
                        break;
                    } else if (cmp == 0) {
                        throw new KrbApErrException(Krb5.KRB_AP_ERR_REPEAT);
                    }
                }
                if (!found) {
                    // All is newer than the newcomer. Sigh.
                    entries.addLast(t);
                }
            }
        }

        // let us cleanup while we are here
        long timeLimit = currentTime.getSeconds() - lifespan;

        // Only trigger a cleanup when the earliest entry is
        // lifespan + 5 sec ago. This ensures a cleanup is done
        // at most every 5 seconds so that we don't always
        // addLast(removeLast).
        if (oldestTime > timeLimit - 5) {
            return;
        }

        // and we remove the *enough* old ones (1 lifetime ago)
        while (!entries.isEmpty()) {
            AuthTimeWithHash removed = entries.removeLast();
            if (removed.ctime >= timeLimit) {
                entries.addLast(removed);
                oldestTime = removed.ctime;
                return;
            }
        }

        oldestTime = Integer.MIN_VALUE;
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        Iterator<AuthTimeWithHash> iter = entries.descendingIterator();
        int pos = entries.size();
        while (iter.hasNext()) {
            AuthTimeWithHash at = iter.next();
            sb.append('#').append(pos--).append(": ")
                    .append(at.toString()).append('\n');
        }
        return sb.toString();
    }
}
