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

package com.sun.tools.javac.model;

import java.util.AbstractList;
import java.util.Iterator;
import com.sun.tools.javac.code.Scope;
import com.sun.tools.javac.code.Symbol;

import static com.sun.tools.javac.code.Flags.*;
import static com.sun.tools.javac.code.Scope.LookupKind.NON_RECURSIVE;

/**
 * Utility to construct a view of a symbol's members,
 * filtering out unwanted elements such as synthetic ones.
 * This view is most efficiently accessed through its iterator() method.
 *
 * <p><b>This is NOT part of any supported API.
 * If you write code that depends on this, you do so at your own risk.
 * This code and its internal interfaces are subject to change or
 * deletion without notice.</b>
 */
public class FilteredMemberList extends AbstractList<Symbol> {

    private final Scope scope;

    public FilteredMemberList(Scope scope) {
        this.scope = scope;
    }

    public int size() {
        int cnt = 0;
        for (Symbol sym : scope.getSymbols(NON_RECURSIVE)) {
            if (!unwanted(sym))
                cnt++;
        }
        return cnt;
    }

    public Symbol get(int index) {
        for (Symbol sym : scope.getSymbols(NON_RECURSIVE)) {
            if (!unwanted(sym) && (index-- == 0))
                return sym;
        }
        throw new IndexOutOfBoundsException();
    }

    // A more efficient implementation than AbstractList's.
    public Iterator<Symbol> iterator() {
        return scope.getSymbols(t -> !unwanted(t), NON_RECURSIVE).iterator();
    }

    /**
     * Tests whether this is a symbol that should never be seen by
     * clients, such as a synthetic class.  Returns true for null.
     */
    private static boolean unwanted(Symbol s) {
        return s == null  ||  (s.flags() & SYNTHETIC) != 0;
    }
}
