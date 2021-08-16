/*
 * Copyright (c) 2005, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6249843
 * @summary Test exposing a Java object to script
 */

import java.io.File;
import java.io.FileReader;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

public class Test2 {
        public static class Testobj {
                private String val;
                public Testobj(String s) {
                        val = s;
                }
                public void setVal(String v) {
                        val = v;
                }
                public String getVal() {
                        return val;
                }
                public String toString() {
                        return "Testobj containing " + val;
                }
        }

        public static void main(String[] args) throws Exception {
            System.out.println("\nTest2\n");
            ScriptEngineManager m = new ScriptEngineManager();
            ScriptEngine eng = Helper.getJsEngine(m);
            if (eng == null) {
                     System.out.println("Warning: No js engine found; test vacuously passes.");
                     return;
            }
            eng.put("Testobj", new Testobj("Hello World"));
            eng.eval(new FileReader(
                    new File(System.getProperty("test.src", "."), "Test2.js")));
        }
}
