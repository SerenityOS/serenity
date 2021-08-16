/*
 * Copyright (c) 2008, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @test
 * @bug 6397609
 * @summary Tests autocleaning
 * @author Sergey Malenkov
 * @modules java.compiler
 *          java.desktop
 *          jdk.compiler
 */

import java.beans.PropertyEditorManager;
import java.lang.ref.WeakReference;

public class Test6397609 {
    public static void main(String[] args) throws Exception {
        Class<?> targetClass = Object.class;
        Class<?> editorClass = new MemoryClassLoader().compile("Editor",
                "public class Editor extends java.beans.PropertyEditorSupport {}");
        PropertyEditorManager.registerEditor(targetClass, editorClass);

        // trigger a gc
        Object object = new Object();
        var r = new WeakReference<Object>(object);
        object = null;
        while (r.get() != null) {
            System.gc();
            Thread.sleep(100);
        }

        if (PropertyEditorManager.findEditor(targetClass) == null) {
            throw new Error("the editor is lost");
        }

        // allow, and wait for, Editor class to be unloaded
        var ref = new WeakReference<Class<?>>(editorClass);
        editorClass = null;
        while (ref.get() != null) {
            System.gc();
            Thread.sleep(100);
        }

        if (PropertyEditorManager.findEditor(targetClass) != null) {
            throw new Error("unexpected editor is found");
        }
    }
}
