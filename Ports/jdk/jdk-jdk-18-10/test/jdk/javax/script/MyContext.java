/*
 * Copyright (c) 2006, Oracle and/or its affiliates. All rights reserved.
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

/*
 * This is pluggable context used by test for 6398614
 */

import javax.script.*;
import java.util.*;
import java.io.*;

public class MyContext  implements ScriptContext {

    public static final int APP_SCOPE = 125;

    protected Writer writer;

    protected Writer errorWriter;

    protected Reader reader;


    protected Bindings appScope;
    protected Bindings engineScope;
    protected Bindings globalScope;


    public MyContext() {
        appScope = new SimpleBindings();
        engineScope = new SimpleBindings();
        globalScope = null;
        reader = new InputStreamReader(System.in);
        writer = new PrintWriter(System.out , true);
        errorWriter = new PrintWriter(System.err, true);
    }

    public void setBindings(Bindings bindings, int scope) {

        switch (scope) {
            case APP_SCOPE:
                if (bindings == null) {
                    throw new NullPointerException("App scope cannot be null.");
                }
                appScope = bindings;
                break;

            case ENGINE_SCOPE:
                if (bindings == null) {
                    throw new NullPointerException("Engine scope cannot be null.");
                }
                engineScope = bindings;
                break;
            case GLOBAL_SCOPE:
                globalScope = bindings;
                break;
            default:
                throw new IllegalArgumentException("Invalid scope value.");
        }
    }

    public Object getAttribute(String name) {
        if (engineScope.containsKey(name)) {
            return getAttribute(name, ENGINE_SCOPE);
        } else if (appScope.containsKey(name)) {
            return getAttribute(name, APP_SCOPE);
        } else if (globalScope != null && globalScope.containsKey(name)) {
            return getAttribute(name, GLOBAL_SCOPE);
        }

        return null;
    }

    public Object getAttribute(String name, int scope) {

        switch (scope) {
            case APP_SCOPE:
                return appScope.get(name);

            case ENGINE_SCOPE:
                return engineScope.get(name);

            case GLOBAL_SCOPE:
                if (globalScope != null) {
                    return globalScope.get(name);
                }
                return null;

            default:
                throw new IllegalArgumentException("Illegal scope value.");
        }
    }

    public Object removeAttribute(String name, int scope) {

        switch (scope) {
            case APP_SCOPE:
                if (getBindings(APP_SCOPE) != null) {
                    return getBindings(APP_SCOPE).remove(name);
                }
                return null;


            case ENGINE_SCOPE:
                if (getBindings(ENGINE_SCOPE) != null) {
                    return getBindings(ENGINE_SCOPE).remove(name);
                }
                return null;

            case GLOBAL_SCOPE:
                if (getBindings(GLOBAL_SCOPE) != null) {
                    return getBindings(GLOBAL_SCOPE).remove(name);
                }
                return null;

            default:
                throw new IllegalArgumentException("Illegal scope value.");
        }
    }

    public void setAttribute(String name, Object value, int scope) {

        switch (scope) {
            case APP_SCOPE:
                appScope.put(name, value);
                return;

            case ENGINE_SCOPE:
                engineScope.put(name, value);
                return;

            case GLOBAL_SCOPE:
                if (globalScope != null) {
                    globalScope.put(name, value);
                }
                return;

            default:
                throw new IllegalArgumentException("Illegal scope value.");
        }
    }

    public Writer getWriter() {
        return writer;
    }

    public Reader getReader() {
        return reader;
    }

    public void setReader(Reader reader) {
        this.reader = reader;
    }

    public void setWriter(Writer writer) {
        this.writer = writer;
    }

    public Writer getErrorWriter() {
        return errorWriter;
    }

    public void setErrorWriter(Writer writer) {
        this.errorWriter = writer;
    }

    public int getAttributesScope(String name) {
        if (engineScope.containsKey(name)) {
            return ENGINE_SCOPE;
        } else if (appScope.containsKey(name)) {
            return APP_SCOPE;
        } else if (globalScope != null && globalScope.containsKey(name)) {
            return GLOBAL_SCOPE;
        } else {
            return -1;
        }
    }

    public Bindings getBindings(int scope) {
        if (scope == ENGINE_SCOPE) {
            return engineScope;
        } else if (scope == APP_SCOPE) {
            return appScope;
        } else if (scope == GLOBAL_SCOPE) {
            return globalScope;
        } else {
            throw new IllegalArgumentException("Illegal scope value.");
        }
    }

    public List<Integer> getScopes() {
        return scopes;
    }

    private static List<Integer> scopes;
    static {
        scopes = new ArrayList<Integer>(3);
        scopes.add(ENGINE_SCOPE);
        scopes.add(APP_SCOPE);
        scopes.add(GLOBAL_SCOPE);
        scopes = Collections.unmodifiableList(scopes);
    }
}
