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
 * @bug 7047734 8027660 8037937 8047719 8058708 8064857
 * @summary The LVT is not generated correctly during some try/catch scenarios
 *          javac crash while creating LVT entry for a local variable defined in
 *          an inner block
 * @library /tools/javac/lib
 * @modules jdk.jdeps/com.sun.tools.classfile
 * @build JavacTestingAbstractProcessor LVTHarness
 * @run main LVTHarness
 */

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.Set;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import com.sun.source.util.JavacTask;
import com.sun.tools.classfile.Attribute;
import com.sun.tools.classfile.ClassFile;
import com.sun.tools.classfile.ConstantPool;
import com.sun.tools.classfile.ConstantPoolException;
import com.sun.tools.classfile.Code_attribute;
import com.sun.tools.classfile.ConstantPool.InvalidIndex;
import com.sun.tools.classfile.ConstantPool.UnexpectedEntry;
import com.sun.tools.classfile.Descriptor.InvalidDescriptor;
import com.sun.tools.classfile.LocalVariableTable_attribute;
import com.sun.tools.classfile.Method;

import static javax.tools.StandardLocation.*;
import static com.sun.tools.classfile.LocalVariableTable_attribute.Entry;
import static javax.tools.JavaFileObject.Kind.SOURCE;

public class LVTHarness {

    static int nerrors = 0;

    static final JavaCompiler comp = ToolProvider.getSystemJavaCompiler();
    static final StandardJavaFileManager fm = comp.getStandardFileManager(null, null, null);

    public static void main(String[] args) throws Exception {
        try {
            String testDir = System.getProperty("test.src");
            fm.setLocation(SOURCE_PATH, Arrays.asList(new File(testDir, "tests")));

            // Make sure classes are written to scratch dir.
            fm.setLocation(CLASS_OUTPUT, Arrays.asList(new File(".")));

            for (JavaFileObject jfo : fm.list(SOURCE_PATH, "", Collections.singleton(SOURCE), true)) {
                new LVTHarness(jfo).check();
            }
            if (nerrors > 0) {
                throw new AssertionError("Errors were found");
            }
        } finally {
            fm.close();
        }
    }


    JavaFileObject jfo;
    Map<ElementKey, AliveRanges> aliveRangeMap = new HashMap<>();
    Set<String> declaredKeys = new HashSet<>();
    List<ElementKey> seenAliveRanges = new ArrayList<>();

    protected LVTHarness(JavaFileObject jfo) {
        this.jfo = jfo;
    }

    protected void check() throws Exception {

        JavacTask ct = (JavacTask) comp.getTask(null, fm, null, Arrays.asList("-g"),
                                                null, Arrays.asList(jfo));
        System.err.println("compiling code " + jfo);
        ct.setProcessors(Collections.singleton(new AliveRangeFinder()));
        if (!ct.call()) {
            throw new AssertionError("Error during compilation");
        }


        File javaFile = new File(jfo.getName());
        File classFile = new File(javaFile.getName().replace(".java", ".class"));
        checkClassFile(classFile);

        //check all candidates have been used up
        for (Map.Entry<ElementKey, AliveRanges> entry : aliveRangeMap.entrySet()) {
            if (!seenAliveRanges.contains(entry.getKey())) {
                error("Redundant @AliveRanges annotation on method " +
                        entry.getKey().elem + " with key " + entry.getKey());
            }
        }
    }

    void checkClassFile(File file)
            throws IOException, ConstantPoolException, InvalidDescriptor {
        ClassFile classFile = ClassFile.read(file);
        ConstantPool constantPool = classFile.constant_pool;

        //lets get all the methods in the class file.
        for (Method method : classFile.methods) {
            for (ElementKey elementKey: aliveRangeMap.keySet()) {
                String methodDesc = method.getName(constantPool) +
                        method.descriptor.getParameterTypes(constantPool).replace(" ", "");
                if (methodDesc.equals(elementKey.elem.toString())) {
                    checkMethod(constantPool, method, aliveRangeMap.get(elementKey));
                    seenAliveRanges.add(elementKey);
                }
            }
        }
    }

    void checkMethod(ConstantPool constantPool, Method method, AliveRanges ranges)
            throws InvalidIndex, UnexpectedEntry, ConstantPoolException {
        Code_attribute code = (Code_attribute) method.attributes.get(Attribute.Code);
        LocalVariableTable_attribute lvt =
            (LocalVariableTable_attribute) (code.attributes.get(Attribute.LocalVariableTable));
        List<String> infoFromRanges = convertToStringList(ranges);
        List<String> infoFromLVT = convertToStringList(constantPool, lvt);

        // infoFromRanges most be contained in infoFromLVT
        int i = 0;
        int j = 0;
        while (i < infoFromRanges.size() && j < infoFromLVT.size()) {
            int comparison = infoFromRanges.get(i).compareTo(infoFromLVT.get(j));
            if (comparison == 0) {
                i++; j++;
            } else if (comparison > 0) {
                j++;
            } else {
                break;
            }
        }

        if (i < infoFromRanges.size()) {
            error(infoFromLVT, infoFromRanges, method.getName(constantPool).toString());
        }
    }

    List<String> convertToStringList(AliveRanges ranges) {
        List<String> result = new ArrayList<>();
        for (Annotation anno : ranges.value()) {
            AliveRange range = (AliveRange)anno;
            String str = formatLocalVariableData(range.varName(),
                    range.bytecodeStart(), range.bytecodeLength());
            result.add(str);
        }
        Collections.sort(result);
        return result;
    }

    List<String> convertToStringList(ConstantPool constantPool,
            LocalVariableTable_attribute lvt) throws InvalidIndex, UnexpectedEntry {
        List<String> result = new ArrayList<>();
        for (Entry entry : lvt.local_variable_table) {
            String str = formatLocalVariableData(constantPool.getUTF8Value(entry.name_index),
                    entry.start_pc, entry.length);
            result.add(str);
        }
        Collections.sort(result);
        return result;
    }

    String formatLocalVariableData(String varName, int start, int length) {
        StringBuilder sb = new StringBuilder()
                    .append("var name: ").append(varName)
                    .append(" start: ").append(start)
                    .append(" length: ").append(length);
        return sb.toString();
    }

    protected void error(List<String> infoFromLVT, List<String> infoFromRanges, String methodName) {
        nerrors++;
        System.err.printf("Error occurred while checking file: %s\n", jfo.getName());
        System.err.printf("at method: %s\n", methodName);
        System.err.println("The range info from the annotations is");
        printStringListToErrOutput(infoFromRanges);
        System.err.println();
        System.err.println("And the range info from the class file is");
        printStringListToErrOutput(infoFromLVT);
        System.err.println();
    }

    void printStringListToErrOutput(List<String> list) {
        for (String s : list) {
            System.err.println("\t" + s);
        }
    }

    protected void error(String msg) {
        nerrors++;
        System.err.printf("Error occurred while checking file: %s\nreason: %s\n",
                jfo.getName(), msg);
    }

    class AliveRangeFinder extends JavacTestingAbstractProcessor {

        @Override
        public boolean process(Set<? extends TypeElement> annotations,
            RoundEnvironment roundEnv) {
            if (roundEnv.processingOver())
                return true;

            TypeElement aliveRangeAnno = elements.getTypeElement("AliveRanges");

            if (!annotations.contains(aliveRangeAnno)) {
                error("no @AliveRanges annotation found in test class");
            }

            for (Element elem: roundEnv.getElementsAnnotatedWith(aliveRangeAnno)) {
                Annotation annotation = elem.getAnnotation(AliveRanges.class);
                aliveRangeMap.put(new ElementKey(elem), (AliveRanges)annotation);
            }
            return true;
        }
    }

    class ElementKey {

        String key;
        Element elem;

        public ElementKey(Element elem) {
            this.elem = elem;
            this.key = computeKey(elem);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof ElementKey) {
                ElementKey other = (ElementKey)obj;
                return other.key.equals(key);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return key.hashCode();
        }

        String computeKey(Element e) {
            StringBuilder buf = new StringBuilder();
            while (e != null) {
                buf.append(e.toString());
                e = e.getEnclosingElement();
            }
            buf.append(jfo.getName());
            return buf.toString();
        }

        @Override
        public String toString() {
            return "Key{" + key + "}";
        }
    }

}
