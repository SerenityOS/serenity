/*
 * Copyright (c) 2013, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import sun.security.krb5.internal.KerberosTime;
import sun.security.krb5.internal.KrbApErrException;
import sun.security.krb5.internal.ReplayCache;

/**
 * This class stores replay caches. AuthTimeWithHash objects are categorized
 * into AuthLists keyed by the names of client and server.
 *
 * @author Yanni Zhang
 */
public class MemoryCache extends ReplayCache {

    // TODO: One day we'll need to read dynamic krb5.conf.
    private static final int lifespan = KerberosTime.getDefaultSkew();
    private static final boolean DEBUG = sun.security.krb5.internal.Krb5.DEBUG;

    private final Map<String,AuthList> content = new ConcurrentHashMap<>();

    @Override
    public synchronized void checkAndStore(KerberosTime currTime, AuthTimeWithHash time)
            throws KrbApErrException {
        String key = time.client + "|" + time.server;
        content.computeIfAbsent(key, k -> new AuthList(lifespan))
                .put(time, currTime);
        if (DEBUG) {
            System.out.println("MemoryCache: add " + time + " to " + key);
        }
        // TODO: clean up AuthList entries with only expired AuthTimeWithHash objects.
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (AuthList rc: content.values()) {
            sb.append(rc.toString());
        }
        return sb.toString();
    }
}
