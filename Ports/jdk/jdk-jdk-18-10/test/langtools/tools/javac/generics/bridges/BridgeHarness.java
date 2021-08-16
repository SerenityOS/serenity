/*
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8013789
 * @summary Compiler should emit bridges in interfaces
 * @library /tools/javac/lib
 * @modules jdk.jdeps/com.sun.tools.classfile
 *          jdk.compiler/com.sun.tools.javac.code
 *          jdk.compiler/com.sun.tools.javac.util
 * @build JavacTestingAbstractProcessor BridgeHarness
 * @run main BridgeHarness
 */

import com.sun.source.util.JavacTask;
import com.sun.tools.classfile.AccessFlags;
import com.sun.tools.classfile.ClassFile;
import com.sun.tools.classfile.ConstantPool;
import com.sun.tools.classfile.ConstantPoolException;
import com.sun.tools.classfile.Method;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.util.List;

import java.io.File;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import static javax.tools.StandardLocation.*;

public class BridgeHarness {

    /** number of errors found (must be zero for the test to pass) */
    static int nerrors = 0;

    /** the (shared) Java compiler used for compiling the tests */
    static final JavaCompiler comp = ToolProvider.getSystemJavaCompiler();

    /** the (shared) file manager used by the compiler */
    static final StandardJavaFileManager fm = comp.getStandardFileManager(null, null, null);

    public static void main(String[] args) throws Exception {
        try {
            //set sourcepath
            fm.setLocation(SOURCE_PATH,
                    Arrays.asList(new File(System.getProperty("test.src"), "tests")));
            //set output (-d)
            fm.setLocation(javax.tools.StandardLocation.CLASS_OUTPUT,
                    Arrays.asList(new File(System.getProperty("user.dir"))));
            for (JavaFileObject jfo : fm.list(SOURCE_PATH, "", Collections.singleton(JavaFileObject.Kind.SOURCE), true)) {
                //for each source, compile and check against annotations
                new BridgeHarness(jfo).compileAndCheck();
            }
            //if there were errors, fail
            if (nerrors > 0) {
                throw new AssertionError("Errors were found");
            }
        } finally {
            fm.close();
        }
    }

    /* utility methods */

    /**
     * Remove an element from a list
     */
    static <Z> List<Z> drop(List<Z> lz, Z z) {
        if (lz.head == z) {
            return drop(lz.tail, z);
        } else if (lz.isEmpty()) {
            return lz;
        } else {
            return drop(lz.tail, z).prepend(lz.head);
        }
    }

    /**
     * return a string representation of a bytecode method
     */
    static String descriptor(Method m, ConstantPool cp) throws ConstantPoolException {
        return m.getName(cp) + m.descriptor.getValue(cp);
    }

    /* test harness */

    /** Test file to be compiled */
    JavaFileObject jfo;

    /** Mapping between class name and list of bridges in class with that name */
    Map<String, List<Bridge>> bridgesMap = new HashMap<String, List<Bridge>>();

    protected BridgeHarness(JavaFileObject jfo) {
        this.jfo = jfo;
    }

    /**
     * Compile a test using a custom annotation processor and check the generated
     * bytecode against discovered annotations.
     */
    protected void compileAndCheck() throws Exception {
        JavacTask ct = (JavacTask)comp.getTask(null, fm, null, null, null, Arrays.asList(jfo));
        ct.setProcessors(Collections.singleton(new BridgeFinder()));

        for (JavaFileObject jfo : ct.generate()) {
            checkBridges(jfo);
        }
    }

    /**
     * Check that every bridge in the generated classfile has a matching bridge
     * annotation in the bridge map
     */
    protected void checkBridges(JavaFileObject jfo) {
        try (InputStream is = jfo.openInputStream()) {
            ClassFile cf = ClassFile.read(is);
            System.err.println("checking: " + cf.getName());

            List<Bridge> bridgeList = bridgesMap.get(cf.getName());
            if (bridgeList == null) {
                //no bridges - nothing to check;
                bridgeList = List.nil();
            }

            for (Method m : cf.methods) {
                if (m.access_flags.is(AccessFlags.ACC_SYNTHETIC | AccessFlags.ACC_BRIDGE)) {
                    //this is a bridge - see if there's a match in the bridge list
                    Bridge match = null;
                    for (Bridge b : bridgeList) {
                        if (b.value().equals(descriptor(m, cf.constant_pool))) {
                            match = b;
                            break;
                        }
                    }
                    if (match == null) {
                        error("No annotation for bridge method: " + descriptor(m, cf.constant_pool));
                    } else {
                        bridgeList = drop(bridgeList, match);
                    }
                }
            }
            if (bridgeList.nonEmpty()) {
                error("Redundant bridge annotation found: " + bridgeList.head.value());
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new Error("error reading " + jfo.toUri() +": " + e);
        }
    }

    /**
     * Log an error
     */
    protected void error(String msg) {
        nerrors++;
        System.err.printf("Error occurred while checking file: %s\nreason: %s\n", jfo.getName(), msg);
    }

    /**
     * This annotation processor is used to populate the bridge map with the
     * contents of the annotations that are found on the tests being compiled
     */
    @SupportedAnnotationTypes({"Bridges","Bridge"})
    class BridgeFinder extends JavacTestingAbstractProcessor {
        @Override
        public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
            if (roundEnv.processingOver())
                return true;

            TypeElement bridgeAnno = elements.getTypeElement("Bridge");
            TypeElement bridgesAnno = elements.getTypeElement("Bridges");

            //see if there are repeated annos
            for (Element elem: roundEnv.getElementsAnnotatedWith(bridgesAnno)) {
                List<Bridge> bridgeList = List.nil();
                Bridges bridges = elem.getAnnotation(Bridges.class);
                for (Bridge bridge : bridges.value()) {
                    bridgeList = bridgeList.prepend(bridge);
                }
                bridgesMap.put(((ClassSymbol)elem).flatname.toString(), bridgeList);
            }

            //see if there are non-repeated annos
            for (Element elem: roundEnv.getElementsAnnotatedWith(bridgeAnno)) {
                Bridge bridge = elem.getAnnotation(Bridge.class);
                bridgesMap.put(((ClassSymbol)elem).flatname.toString(),
                        List.of(bridge));
            }

            return true;
        }
    }
}
