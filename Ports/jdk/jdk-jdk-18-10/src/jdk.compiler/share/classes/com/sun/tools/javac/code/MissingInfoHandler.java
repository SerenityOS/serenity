/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.javac.code;

import com.sun.source.util.ParameterNameProvider;
import com.sun.tools.javac.code.Symbol.ParamSymbol;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;

/**
 * A Context class, that can return additional useful information for Symbols, currently
 * parameter names. It does so by calling user-supplied {@link ParameterNameProvider}.
 *
 * <p><b>This is NOT part of any supported API.
 * If you write code that depends on this, you do so at your own risk.
 * This code and its internal interfaces are subject to change or
 * deletion without notice.</b>
 */
public class MissingInfoHandler {
    protected static final Context.Key<MissingInfoHandler> missingInfoHandlerWrapperKey = new Context.Key<>();

    public static MissingInfoHandler instance(Context context) {
        MissingInfoHandler instance = context.get(missingInfoHandlerWrapperKey);
        if (instance == null)
            instance = new MissingInfoHandler(context);
        return instance;
    }

    private final Names names;
    private ParameterNameProvider parameterNameProvider;

    protected MissingInfoHandler(Context context) {
        context.put(missingInfoHandlerWrapperKey, this);
        names = Names.instance(context);
    }

    public Name getParameterName(ParamSymbol parameter) {
        if (parameterNameProvider != null) {
            CharSequence name = parameterNameProvider.getParameterName(parameter);
            if (name != null) {
                return names.fromString(name.toString());
            }
        }

        return null;
    }

    public void setDelegate(ParameterNameProvider delegate) {
        this.parameterNameProvider = delegate;
    }
}
