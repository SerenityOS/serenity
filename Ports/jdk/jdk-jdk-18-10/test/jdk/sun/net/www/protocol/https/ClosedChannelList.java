/*
 * Copyright (c) 2002, Oracle and/or its affiliates. All rights reserved.
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

import java.io.*;
import java.nio.channels.*;
import java.util.*;

class ClosedChannelList {

    static final long TIMEOUT = 10 * 1000; /* 10 sec */

    static class Element {
        long expiry;
        SelectionKey key;
        Element (long l, SelectionKey key) {
            expiry = l;
            this.key = key;
        }
    }

    LinkedList list;

    public ClosedChannelList () {
        list = new LinkedList ();
    }

    /* close chan after TIMEOUT milliseconds */

    public synchronized void add (SelectionKey key) {
        long exp = System.currentTimeMillis () + TIMEOUT;
        list.add (new Element (exp, key));
    }

    public synchronized void check () {
        check (false);
    }

    public synchronized void terminate () {
        check (true);
    }

    public synchronized void check (boolean forceClose) {
        Iterator iter = list.iterator ();
        long now = System.currentTimeMillis();
        while (iter.hasNext ()) {
            Element elm = (Element)iter.next();
            if (forceClose || elm.expiry <= now) {
                SelectionKey k = elm.key;
                try {
                    k.channel().close ();
                } catch (IOException e) {}
                k.cancel();
                iter.remove();
            }
        }
    }
}
