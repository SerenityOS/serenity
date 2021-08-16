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

package org.pear;

import java.util.Arrays;
import java.util.List;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;

public class PearScriptEngineFactory implements ScriptEngineFactory {

    public PearScriptEngineFactory() { }

    public static PearScriptEngineFactory provider() {
        throw new RuntimeException("Should not be called");
    }

    @Override
    public String getEngineName() {
        return "PearScriptEngine";
    }

    @Override
    public String getEngineVersion() {
        return "1.0";
    }

    @Override
    public List<String> getExtensions() {
        return Arrays.asList("pear");
    }

    @Override
    public List<String> getMimeTypes() {
        return Arrays.asList("application/x-pearscript");
    }

    @Override
    public List<String> getNames() {
        return Arrays.asList("PearScript");
    }

    @Override
    public String getLanguageName() {
        return "PearScript";
    }

    @Override
    public String getLanguageVersion() {
        return "1.0";
    }

    @Override
    public Object getParameter(String key) {
        return null;
    }

    @Override
    public String getMethodCallSyntax(String obj, String m, String... args) {
        throw new RuntimeException();
    }

    @Override
    public String getOutputStatement(String toDisplay) {
        throw new RuntimeException();
    }

    @Override
    public String getProgram(String... statements) {
        throw new RuntimeException();
    }

    @Override
    public ScriptEngine getScriptEngine() {
        return new PearScript();
    }
}
