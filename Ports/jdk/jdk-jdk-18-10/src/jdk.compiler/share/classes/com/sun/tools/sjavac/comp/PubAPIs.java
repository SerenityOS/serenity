/*
 * Copyright (c) 1999, 2015, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.sjavac.comp;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.lang.model.element.Element;
import javax.tools.JavaFileObject;

import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Log;
import com.sun.tools.sjavac.pubapi.PubApi;

/**
 * Utility class containing public API information.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class PubAPIs {
    protected static final Context.Key<PubAPIs> pubApisKey = new Context.Key<>();

    // The log to be used for error reporting.
    protected Log log;

    // Map from a class name to its public api.
    // Will the Name encode the module in the future?
    // If not, this will have to change to map from Module+Name to public api.
    protected Map<ClassSymbol, PubApi> publicApiPerClass = new HashMap<>();

    public static PubAPIs instance(Context context) {
        PubAPIs instance = context.get(pubApisKey);
        if (instance == null)
            instance = new PubAPIs(context);
        return instance;
    }

    private PubAPIs(Context context) {
        context.put(pubApisKey, this);
        log = Log.instance(context);
    }

    /**
     * Convert the map from class names to their pubapi to a map
     * from package names to their pubapi.
     */
    public Map<String, PubApi> getPubapis(Collection<JavaFileObject> explicitJFOs, boolean explicits) {

        // Maps ":java.lang" to a package level pub api (with only types on top level)
        Map<String, PubApi> result = new HashMap<>();
        for (ClassSymbol cs : publicApiPerClass.keySet()) {

            boolean amongExplicits = explicitJFOs.contains(cs.sourcefile);
            if (explicits != amongExplicits)
                continue;

            String pkg = ":" + cs.packge().fullname;
            PubApi currentPubApi = result.getOrDefault(pkg, new PubApi());
            result.put(pkg, PubApi.mergeTypes(currentPubApi, publicApiPerClass.get(cs)));
        }

        return result;
    }

    /**
     * Visit the api of a class and construct a pubapi and
     * store it into the pubapi_perclass map.
     */
    @SuppressWarnings("deprecation")
    public void visitPubapi(Element e) {

        // Skip anonymous classes for now
        if (e == null)
            return;

        PubapiVisitor v = new PubapiVisitor();
        v.visit(e);
        publicApiPerClass.put((ClassSymbol) e, v.getCollectedPubApi());
    }
}
