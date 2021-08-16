/*
 * Copyright (c) 2005, 2021, Oracle and/or its affiliates. All rights reserved.
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

package sun.net.httpserver;

import java.util.*;

class ContextList {

    private final LinkedList<HttpContextImpl> list = new LinkedList<>();

    public synchronized void add (HttpContextImpl ctx) {
        assert ctx.getPath() != null;
        if (contains(ctx)) {
            throw new IllegalArgumentException ("cannot add context to list");
        }
        list.add (ctx);
    }

    boolean contains(HttpContextImpl ctx) {
        return findContext(ctx.getProtocol(), ctx.getPath(), true) != null;
    }

    public synchronized int size () {
        return list.size();
    }

   /* initially contexts are located only by protocol:path.
    * Context with longest prefix matches (currently case-sensitive)
    */
    synchronized HttpContextImpl findContext (String protocol, String path) {
        return findContext (protocol, path, false);
    }

    synchronized HttpContextImpl findContext (String protocol, String path, boolean exact) {
        protocol = protocol.toLowerCase();
        String longest = "";
        HttpContextImpl lc = null;
        for (HttpContextImpl ctx: list) {
            if (!ctx.getProtocol().equals(protocol)) {
                continue;
            }
            String cpath = ctx.getPath();
            if (exact && !cpath.equals (path)) {
                continue;
            } else if (!exact && !path.startsWith(cpath)) {
                continue;
            }
            if (cpath.length() > longest.length()) {
                longest = cpath;
                lc = ctx;
            }
        }
        return lc;
    }

    public synchronized void remove (String protocol, String path)
        throws IllegalArgumentException
    {
        HttpContextImpl ctx = findContext (protocol, path, true);
        if (ctx == null) {
            throw new IllegalArgumentException ("cannot remove element from list");
        }
        list.remove (ctx);
    }

    public synchronized void remove (HttpContextImpl context)
        throws IllegalArgumentException
    {
        for (HttpContextImpl ctx: list) {
            if (ctx.equals (context)) {
                list.remove (ctx);
                return;
            }
        }
        throw new IllegalArgumentException ("no such context in list");
    }
}
