/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

package org.banana;

import java.io.Reader;
import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;

public class BananaScript implements ScriptEngine {

    @Override
    public Object eval(String script, ScriptContext context) {
        throw new RuntimeException();
    }

    @Override
    public Object eval(Reader reader , ScriptContext context) {
        throw new RuntimeException();
    }

    @Override
    public Object eval(String script) {
        throw new RuntimeException();
    }

    @Override
    public Object eval(Reader reader) {
        throw new RuntimeException();
    }

    @Override
    public Object eval(String script, Bindings n) {
        throw new RuntimeException();
    }

    @Override
    public Object eval(Reader reader , Bindings n) {
        throw new RuntimeException();
    }
    @Override
    public void put(String key, Object value) {
        throw new RuntimeException();
    }

    @Override
    public Object get(String key) {
        throw new RuntimeException();
    }

    @Override
    public Bindings getBindings(int scope) {
        throw new RuntimeException();
    }

    @Override
    public void setBindings(Bindings bindings, int scope) {
        throw new RuntimeException();
    }

    @Override
    public Bindings createBindings() {
        throw new RuntimeException();
    }

    @Override
    public ScriptContext getContext() {
        throw new RuntimeException();
    }

    @Override
    public void setContext(ScriptContext context) {
        throw new RuntimeException();
    }

    @Override
    public ScriptEngineFactory getFactory() {
        throw new RuntimeException();
    }
}

