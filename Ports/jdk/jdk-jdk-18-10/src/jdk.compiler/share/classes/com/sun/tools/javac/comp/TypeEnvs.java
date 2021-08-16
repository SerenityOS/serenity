/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.javac.comp;

import java.util.Collection;
import java.util.HashMap;
import com.sun.tools.javac.code.Symbol.TypeSymbol;
import com.sun.tools.javac.util.Context;

/** This class contains the type environments used by Enter, MemberEnter,
 *  Attr, DeferredAttr, and Lower.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
class TypeEnvs {
    private static final long serialVersionUID = 571524752489954631L;

    protected static final Context.Key<TypeEnvs> typeEnvsKey = new Context.Key<>();
    public static TypeEnvs instance(Context context) {
        TypeEnvs instance = context.get(typeEnvsKey);
        if (instance == null)
            instance = new TypeEnvs(context);
        return instance;
    }

    private HashMap<TypeSymbol,Env<AttrContext>> map;
    protected TypeEnvs(Context context) {
        map = new HashMap<>();
        context.put(typeEnvsKey, this);
    }

    Env<AttrContext> get(TypeSymbol sym) { return map.get(sym); }
    Env<AttrContext> put(TypeSymbol sym, Env<AttrContext> env) { return map.put(sym, env); }
    Env<AttrContext> remove(TypeSymbol sym) { return map.remove(sym); }
    Collection<Env<AttrContext>> values() { return map.values(); }
    void clear() { map.clear(); }
}
