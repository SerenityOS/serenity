/*
 * Copyright (c) 1998, 2015, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.hotspot.igv.filter;

import com.sun.hotspot.igv.graph.Diagram;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.script.*;
import org.openide.cookies.OpenCookie;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Exceptions;

/**
 *
 * @author Thomas Wuerthinger
 */
public class CustomFilter extends AbstractFilter {

    public static final String JAVASCRIPT_HELPER_ID = "JavaScriptHelper";
    private String code;
    private String name;

    public CustomFilter(String name, String code) {
        this.name = name;
        this.code = code;
        getProperties().setProperty("name", name);
    }

    @Override
    public String getName() {
        return name;
    }

    public String getCode() {
        return code;
    }

    public void setName(String s) {
        name = s;
        fireChangedEvent();
    }

    public void setCode(String s) {
        code = s;
        fireChangedEvent();
    }

    @Override
    public OpenCookie getEditor() {
        return new OpenCookie() {

            @Override
            public void open() {
                openInEditor();
            }
        };
    }

    public boolean openInEditor() {
        EditFilterDialog dialog = new EditFilterDialog(CustomFilter.this);
        dialog.setVisible(true);
        boolean result = dialog.wasAccepted();
        this.getChangedEvent().fire();
        return result;
    }

    @Override
    public String toString() {
        return getName();
    }

    private static String getJsHelperText() {
        InputStream is = null;
        StringBuilder sb = new StringBuilder("if (typeof importPackage === 'undefined') { try { load('nashorn:mozilla_compat.js'); } catch (e) {} }"
                + "importPackage(Packages.com.sun.hotspot.igv.filter);"
                + "importPackage(Packages.com.sun.hotspot.igv.graph);"
                + "importPackage(Packages.com.sun.hotspot.igv.data);"
                + "importPackage(Packages.com.sun.hotspot.igv.util);"
                + "importPackage(java.awt);");
        try {
            FileObject fo = FileUtil.getConfigRoot().getFileObject(JAVASCRIPT_HELPER_ID);
            is = fo.getInputStream();
            BufferedReader r = new BufferedReader(new InputStreamReader(is));
            String s;
            while ((s = r.readLine()) != null) {
                sb.append(s);
                sb.append("\n");
            }

        } catch (IOException ex) {
            Logger.getLogger("global").log(Level.SEVERE, null, ex);
        } finally {
            try {
                is.close();
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
        return sb.toString();
    }

    @Override
    public void apply(Diagram d) {
        try {
            ScriptEngineManager sem = new ScriptEngineManager();
            ScriptEngine e = sem.getEngineByName("ECMAScript");
            e.eval(getJsHelperText());
            Bindings b = e.getContext().getBindings(ScriptContext.ENGINE_SCOPE);
            b.put("graph", d);
            b.put("IO", System.out);
            e.eval(code, b);
        } catch (ScriptException ex) {
            Exceptions.printStackTrace(ex);
        }
    }
}
