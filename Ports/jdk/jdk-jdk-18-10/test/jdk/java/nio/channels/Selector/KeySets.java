/*
 * Copyright (c) 2003, 2017, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 * @bug 4776783 4778091 4778099
 * @summary Check various properties of key and selected-key sets
 *
 * @run main KeySets
 */

import java.io.*;
import java.nio.channels.*;
import java.util.*;

public class KeySets {

    static abstract class Catch {
        abstract void go() throws Exception;
        Catch(Class xc) throws Exception {
            try {
                go();
            } catch (Exception x) {
                if (xc.isInstance(x))
                    return;
                throw new Exception("Wrong exception", x);
            }
            throw new Exception("Not thrown as expected: " + xc.getName());
        }
    }

    // 4776783: Closing a selector should make key sets inaccessible
    static void testClose() throws Exception {

        final Selector sel = Selector.open();
        sel.keys();
        sel.selectedKeys();
        sel.close();

        new Catch(ClosedSelectorException.class) {
                void go() throws Exception {
                    sel.keys();
                }};

        new Catch(ClosedSelectorException.class) {
                void go() throws Exception {
                    sel.selectedKeys();
                }};
    }

    static void testNoAddition(final Set s) throws Exception {
        new Catch(UnsupportedOperationException.class) {
                void go() throws Exception {
                    s.add(new Object());
                }};
        new Catch(UnsupportedOperationException.class) {
                void go() throws Exception {
                    ArrayList al = new ArrayList();
                    al.add(new Object());
                    s.addAll(al);
                }};
    }

    static interface Adder {
        void add() throws IOException;
    }

    static void testNoRemoval(final Set s, final Adder adder)
        throws Exception
    {
        new Catch(UnsupportedOperationException.class) {
                void go() throws Exception {
                    adder.add();
                    s.clear();
                }};
        new Catch(UnsupportedOperationException.class) {
                void go() throws Exception {
                    adder.add();
                    Iterator i = s.iterator();
                    i.next();
                    i.remove();
                }};
        new Catch(UnsupportedOperationException.class) {
                void go() throws Exception {
                    adder.add();
                    s.remove(s.iterator().next());
                }};
        new Catch(UnsupportedOperationException.class) {
                void go() throws Exception {
                    adder.add();
                    HashSet hs = new HashSet();
                    hs.addAll(s);
                    s.removeAll(hs);
                }};
        new Catch(UnsupportedOperationException.class) {
                void go() throws Exception {
                    adder.add();
                    s.retainAll(Collections.EMPTY_SET);
                }};
    }

    static SelectionKey reg(Selector sel) throws IOException {
        DatagramChannel dc = DatagramChannel.open();
        dc.configureBlocking(false);
        return dc.register(sel, SelectionKey.OP_WRITE);
    }

    static void testMutability() throws Exception {

        final Selector sel = Selector.open();

        // 4778091: Selector.keys() should be immutable

        testNoRemoval(sel.keys(), new Adder() {
                public void add() throws IOException {
                    reg(sel);
                }
            });
        testNoAddition(sel.keys());

        // 4778099: Selector.selectedKeys() should allow removal but not addition

        sel.select();
        testNoAddition(sel.selectedKeys());
        SelectionKey sk = reg(sel);
        sel.select();
        int n = sel.selectedKeys().size();
        sel.selectedKeys().remove(sk);
        if (sel.selectedKeys().size() != n - 1)
            throw new Exception("remove failed");

        HashSet hs = new HashSet();
        hs.add(reg(sel));
        sel.select();
        sel.selectedKeys().retainAll(hs);
        if (sel.selectedKeys().isEmpty())
            throw new Exception("retainAll failed");
        sel.selectedKeys().removeAll(hs);
        if (!sel.selectedKeys().isEmpty())
            throw new Exception("removeAll failed");

        hs.clear();
        hs.add(reg(sel));
        sel.select();
        sel.selectedKeys().clear();
        if (!sel.selectedKeys().isEmpty())
            throw new Exception("clear failed");
    }

    public static void main(String[] args) throws Exception {
        testClose();
        testMutability();
    }
}
