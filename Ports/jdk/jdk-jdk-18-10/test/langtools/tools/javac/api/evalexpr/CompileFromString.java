/*
 * Copyright (c) 2005, 2006, Oracle and/or its affiliates. All rights reserved.
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

package evalexpr;

import java.lang.reflect.Method;
import java.util.*;
import javax.swing.JOptionPane;
import javax.tools.*;
import static javax.tools.StandardLocation.CLASS_OUTPUT;

/**
 * JSR 199 Demo application: compile from a String.
 *
 * <p><b>This is NOT part of any supported API.
 * If you write code that depends on this, you do so at your own
 * risk.  This code and its internal interfaces are subject to change
 * or deletion without notice.</b></p>
 *
 * @author Peter von der Ah&eacute;
 */
public class CompileFromString {

    /**
     * The name of the class used to evaluate expressions.
     */
    private final static String CLASS_NAME = "EvalExpression";

    /**
     * Object used to signal errors from evalExpression.
     */
    public final static Object ERROR = new Object() {
        public String toString() { return "error"; }
    };

    /**
     * Compile and evaluate the specified expression using the
     * given compiler.
     * @param compiler a JSR 199 compiler tool used to compile the given expression
     * @param expression a Java Programming Language expression
     * @return the value of the expression; ERROR if any errors occured during compilation
     * @throws java.lang.Exception exceptions are ignored for brevity
     */
    public static Object evalExpression(JavaCompiler compiler,
                                        DiagnosticListener<JavaFileObject> listener,
                                        List<String> flags,
                                        String expression)
        throws Exception
    {
        // Use a customized file manager
        MemoryFileManager mfm =
            new MemoryFileManager(compiler.getStandardFileManager(listener, null, null));

        // Create a file object from a string
        JavaFileObject fileObject = mfm.makeSource(CLASS_NAME,
            "public class " + CLASS_NAME + " {\n" +
            "    public static Object eval() throws Throwable {\n" +
            "        return " + expression + ";\n" +
            "    }\n}\n");

        JavaCompiler.CompilationTask task =
            compiler.getTask(null, mfm, listener, flags, null, Arrays.asList(fileObject));
        if (task.call()) {
            // Obtain a class loader for the compiled classes
            ClassLoader cl = mfm.getClassLoader(CLASS_OUTPUT);
            // Load the compiled class
            Class compiledClass = cl.loadClass(CLASS_NAME);
            // Find the eval method
            Method eval = compiledClass.getMethod("eval");
            // Invoke it
            return eval.invoke(null);
        } else {
            // Report that an error occured
            return ERROR;
        }
    }

    /**
     * Main entry point for program; ask user for expressions,
     * compile, evaluate, and print them.
     *
     * @param args ignored
     * @throws java.lang.Exception exceptions are ignored for brevity
     */
    public static void main(String... args) throws Exception {
        // Get a compiler tool
        final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        final List<String> compilerFlags = new ArrayList();
        compilerFlags.add("-Xlint:all"); // report all warnings
        compilerFlags.add("-g:none"); // don't generate debug info
        String expression = "System.getProperty(\"java.vendor\")";
        while (true) {
            expression = JOptionPane.showInputDialog("Please enter a Java expression",
                                                     expression);
            if (expression == null)
                return; // end program on "cancel"
            long time = System.currentTimeMillis();
            Object result = evalExpression(compiler, null, compilerFlags, expression);
            time = System.currentTimeMillis() - time;
            System.out.format("Elapsed time %dms %n", time);
            if (result == ERROR)
                System.out.format("Error compiling \"%s\"%n", expression);
            else
                System.out.format("%s => %s%n", expression, result);
        }
    }
}
