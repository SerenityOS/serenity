/*
 * Copyright (c) 2009, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6889255
 * @summary ClassReader does not read parameter names correctly
 * @modules jdk.compiler/com.sun.tools.javac.code
 *          jdk.compiler/com.sun.tools.javac.file
 *          jdk.compiler/com.sun.tools.javac.jvm
 *          jdk.compiler/com.sun.tools.javac.util
 */

import java.io.*;
import java.util.*;
import javax.tools.StandardLocation;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.*;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Type.ClassType;
import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.file.JavacFileManager;
import com.sun.tools.javac.jvm.ClassReader;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Names;

import static com.sun.tools.javac.code.Scope.LookupKind.NON_RECURSIVE;

public class T6889255 {
    boolean testInterfaces = true;
    boolean testSyntheticMethods = true;

    // The following enums control the generation of the test methods to be compiled.
    enum GenericKind {
        NOT_GENERIC,
        GENERIC
    };

    enum ClassKind {
        CLASS("Clss"),
        INTERFACE("Intf"),
        ENUM("Enum");
        final String base;
        ClassKind(String base) { this.base = base; }
    };

    enum NestedKind {
        /** Declare methods inside the outermost container. */
        NONE,
        /** Declare methods inside a container with a 'static' modifier. */
        NESTED,
        /** Declare methods inside a container without a 'static' modifier. */
        INNER,
        /** Declare methods inside a local class in an initializer. */
        INIT_LOCAL,
        /** Declare methods inside an anonymous class in an initializer. */
        INIT_ANON,
        /** Declare methods inside a local class in a method. */
        METHOD_LOCAL,
        /** Declare methods inside an anonymous class in a method. */
        METHOD_ANON
    };

    enum MethodKind {
        ABSTRACT,
        CONSTRUCTOR,
        METHOD,
        STATIC_METHOD,
        BRIDGE_METHOD
    };

    enum FinalKind {
        /** Method body does not reference external final variables. */
        NO_FINAL,
        /** Method body references external final variables. */
        USE_FINAL
    };

    public static void main(String... args) throws Exception {
        new T6889255().run();
    }

    void run() throws Exception {
        genTest();

        test("no-args", false);
        test("g",       true,  "-g");

        if (errors > 0)
            throw new Exception(errors + " errors found");
    }

    /**
     * Create a file containing lots of method definitions to be tested.
     * There are 3 sets of nested loops that generate the methods.
     * 1. The outermost set declares [generic] (class | interface | enum)
     * 2. The middle set declares [(nested | inner | anon | local)] class
     * 3. The innermost set declares
     *      [generic] (constructor|method|static-method|bridge-method) [using final variables in outer scope]
     * Invalid combinations are filtered out.
     */
    void genTest() throws Exception {
        BufferedWriter out = new BufferedWriter(new FileWriter("Test.java"));

        // This interface is used to force bridge methods to be generated, by
        // implementing its methods with subtypes of Object
        out.write("interface Base {\n");
        out.write("    Object base_m1(int i1);\n");
        out.write("    Object base_m2(int i1);\n");
        out.write("}\n");

        int outerNum = 0;
        // Outermost set of loops, to generate a top level container
        for (GenericKind outerGenericKind: GenericKind.values()) {
            for (ClassKind outerClassKind: ClassKind.values()) {
                if (outerGenericKind == GenericKind.GENERIC && outerClassKind == ClassKind.ENUM)
                    continue;
                String outerClassName = outerClassKind.base + (outerNum++);
                String outerTypeArg = outerClassKind.toString().charAt(0) + "T";
                if (outerClassKind == ClassKind.CLASS)
                    out.write("abstract ");
                out.write(outerClassKind.toString().toLowerCase() + " " + outerClassName);
                if (outerGenericKind == GenericKind.GENERIC)
                    out.write("<" + outerTypeArg + ">");
                if (outerClassKind == ClassKind.INTERFACE)
                    out.write(" extends Base");
                else
                    out.write(" implements Base");
                out.write(" {\n");
                if (outerClassKind == ClassKind.ENUM) {
                    out.write("    E1(0,0,0), E2(0,0,0), E3(0,0,0);\n");
                    out.write("    " + outerClassName + "(int i1, int i2, int i3) { }\n");
                }
                // Middle set of loops, to generate an optional nested container
                int nestedNum = 0;
                int methodNum = 0;
                for (GenericKind nestedGenericKind: GenericKind.values()) {
                    nextNestedKind:
                    for (NestedKind nestedKind: NestedKind.values()) {
                        // if the nested kind is none, there is no point iterating over all
                        // nested generic kinds, so arbitarily limit it to just one kind
                        if (nestedKind == NestedKind.NONE && nestedGenericKind != GenericKind.NOT_GENERIC)
                            continue;
                        if ((nestedKind == NestedKind.METHOD_ANON || nestedKind == NestedKind.INIT_ANON)
                                && nestedGenericKind == GenericKind.GENERIC)
                            continue;
                        String indent = "    ";
                        boolean haveFinal = false;
                        switch (nestedKind) {
                            case METHOD_ANON: case METHOD_LOCAL:
                                if (outerClassKind == ClassKind.INTERFACE)
                                    continue nextNestedKind;
                                out.write(indent + "void m" +  + (nestedNum++) + "() {\n");
                                indent += "    ";
                                out.write(indent + "final int fi1 = 0;\n");
                                haveFinal = true;
                                break;
                            case INIT_ANON: case INIT_LOCAL:
                                if (outerClassKind == ClassKind.INTERFACE)
                                    continue nextNestedKind;
                                out.write(indent + "{\n");
                                indent += "    ";
                                break;
                        }
                        for (ClassKind nestedClassKind: ClassKind.values()) {
                            if ((nestedGenericKind == GenericKind.GENERIC)
                                    && (nestedClassKind == ClassKind.ENUM))
                                continue;
                            if ((nestedKind == NestedKind.METHOD_ANON || nestedKind == NestedKind.METHOD_LOCAL
                                    || nestedKind == NestedKind.INIT_ANON || nestedKind == NestedKind.INIT_LOCAL)
                                    && nestedClassKind != ClassKind.CLASS)
                                continue;
                            // if the nested kind is none, there is no point iterating over all
                            // nested class kinds, so arbitarily limit it to just one kind
                            if (nestedKind == NestedKind.NONE && nestedClassKind != ClassKind.CLASS)
                                continue;

                            ClassKind methodClassKind;
                            String methodClassName;
                            boolean allowAbstractMethods;
                            boolean allowStaticMethods;
                            switch (nestedKind) {
                                case NONE:
                                    methodClassKind = outerClassKind;
                                    methodClassName = outerClassName;
                                    allowAbstractMethods = (outerClassKind == ClassKind.CLASS);
                                    allowStaticMethods = (outerClassKind != ClassKind.INTERFACE);
                                    break;
                                case METHOD_ANON:
                                case INIT_ANON:
                                    out.write(indent + "new Base() {\n");
                                    indent += "    ";
                                    methodClassKind = ClassKind.CLASS;
                                    methodClassName = null;
                                    allowAbstractMethods = false;
                                    allowStaticMethods = false;
                                    break;
                                default: { // INNER, NESTED, LOCAL
                                    String nestedClassName = "N" + nestedClassKind.base + (nestedNum++);
                                    String nestedTypeArg = nestedClassKind.toString().charAt(0) + "T";
                                    out.write(indent);
                                    if (nestedKind == NestedKind.NESTED)
                                        out.write("static ");
                                    if (nestedClassKind == ClassKind.CLASS)
                                        out.write("abstract ");
                                    out.write(nestedClassKind.toString().toLowerCase() + " " + nestedClassName);
                                    if (nestedGenericKind == GenericKind.GENERIC)
                                        out.write("<" + nestedTypeArg + ">");
                                    if (nestedClassKind == ClassKind.INTERFACE)
                                        out.write(" extends Base ");
                                    else
                                        out.write(" implements Base ");
                                    out.write(" {\n");
                                    indent += "    ";
                                    if (nestedClassKind == ClassKind.ENUM) {
                                        out.write(indent + "E1(0,0,0), E2(0,0,0), E3(0,0,0);\n");
                                        out.write(indent + nestedClassName + "(int i1, int i2, int i3) { }\n");
                                    }
                                    methodClassKind = nestedClassKind;
                                    methodClassName = nestedClassName;
                                    allowAbstractMethods = (nestedClassKind == ClassKind.CLASS);
                                    allowStaticMethods = (nestedKind == NestedKind.NESTED && nestedClassKind != ClassKind.INTERFACE);
                                    break;
                                }
                            }

                            // Innermost loops, to generate methods
                            for (GenericKind methodGenericKind: GenericKind.values()) {
                                for (FinalKind finalKind: FinalKind.values()) {
                                    for (MethodKind methodKind: MethodKind.values()) {
//                                        out.write("// " + outerGenericKind
//                                                + " " + outerClassKind
//                                                + " " + nestedKind
//                                                + " " + nestedGenericKind
//                                                + " " + nestedClassKind
//                                                + " " + methodGenericKind
//                                                + " " + finalKind
//                                                + " " + methodKind
//                                                + "\n");
                                        switch (methodKind) {
                                            case CONSTRUCTOR:
                                                if (nestedKind == NestedKind.METHOD_ANON || nestedKind == NestedKind.INIT_ANON)
                                                    break;
                                                if (methodClassKind != ClassKind.CLASS)
                                                    break;
                                                if (finalKind == FinalKind.USE_FINAL && !haveFinal)
                                                    break;
                                                out.write(indent);
                                                if (methodGenericKind == GenericKind.GENERIC) {
                                                    out.write("<CT> " + methodClassName + "(CT c1, CT c2");
                                                } else {
                                                    out.write(methodClassName + "(boolean b1, char c2");
                                                }
                                                if (finalKind == FinalKind.USE_FINAL) {
                                                    // add a dummy parameter to avoid duplicate declaration
                                                    out.write(", int i3) { int i = fi1; }\n");
                                                } else
                                                    out.write(") { }\n");
                                                break;
                                            case ABSTRACT:
                                                if (!allowAbstractMethods)
                                                    continue;
                                                // fallthrough
                                            case METHOD:
                                                if (finalKind == FinalKind.USE_FINAL && !haveFinal)
                                                    break;
                                                out.write(indent);
                                                if (methodKind == MethodKind.ABSTRACT)
                                                    out.write("abstract ");
                                                if (methodGenericKind == GenericKind.GENERIC)
                                                    out.write("<MT> ");
                                                out.write("void m" + (methodNum++) + "(int i1, long l2, float f3)");
                                                if (methodKind == MethodKind.ABSTRACT || methodClassKind == ClassKind.INTERFACE)
                                                    out.write(";\n");
                                                else {
                                                    out.write(" {");
                                                    if (finalKind == FinalKind.USE_FINAL)
                                                        out.write(" int i = fi1;");
                                                    out.write(" }\n");
                                                }
                                                break;
                                            case BRIDGE_METHOD:
                                                if (methodGenericKind == GenericKind.GENERIC)
                                                    break;
                                                out.write(indent);
                                                // methods Base.base_m1 and Base.base_m2 are declared for the
                                                // benefit of bridge methods. They need to be implemented
                                                // whether or not a final variable is used.
                                                String methodName = (finalKind == FinalKind.NO_FINAL ? "base_m1" : "base_m2");
                                                out.write("public String " + methodName + "(int i1)");
                                                if (methodClassKind == ClassKind.INTERFACE)
                                                    out.write(";\n");
                                                else {
                                                    out.write(" {");
                                                    if (finalKind == FinalKind.USE_FINAL && haveFinal)
                                                        out.write(" int i = fi1;");
                                                    out.write(" return null; }\n");
                                                }
                                                break;
                                            case STATIC_METHOD:
                                                if (!allowStaticMethods)
                                                    break;
                                                if (finalKind == FinalKind.USE_FINAL && !haveFinal)
                                                    break;
                                                out.write(indent + "static ");
                                                if (methodGenericKind == GenericKind.GENERIC)
                                                    out.write("<MT> ");
                                                out.write("void m" + (methodNum++) + "(int i1, long l2, float f3) {");
                                                if (finalKind == FinalKind.USE_FINAL)
                                                    out.write(" int i = fi1;");
                                                out.write(" }\n");
                                                break;
                                        }

                                    }
                                }
                            }
                            if (nestedKind != NestedKind.NONE) {
                                indent = indent.substring(0, indent.length() - 4);
                                out.write(indent + "};\n");
                            }
                        }
                        switch (nestedKind) {
                            case METHOD_ANON: case METHOD_LOCAL:
                            case INIT_ANON: case INIT_LOCAL:
                                indent = indent.substring(0, indent.length() - 4);
                                out.write(indent + "}\n\n");
                        }
                    }
                }
                out.write("}\n\n");
            }
        }
        out.close();
    }


    void test(String testName, boolean expectNames, String... opts) throws Exception {
        System.err.println("Test " + testName
                + ": expectNames:" + expectNames
                + " javacOpts:" + Arrays.asList(opts));

        File outDir = new File(testName);
        outDir.mkdirs();
        compile(outDir, opts);

        Context ctx = new Context();
        JavacFileManager fm = new JavacFileManager(ctx, true, null);
        fm.setLocation(StandardLocation.CLASS_PATH, Arrays.asList(outDir));
        Symtab syms = Symtab.instance(ctx);
        ClassReader cr = ClassReader.instance(ctx);
        cr.saveParameterNames = true;
        Names names = Names.instance(ctx);

        Set<String> classes = getTopLevelClasses(outDir);
        Deque<String> work = new LinkedList<String>(classes);
        String classname;
        while ((classname = work.poll()) != null) {
            System.err.println("Checking class " + classname);
            ClassSymbol sym = syms.enterClass(syms.noModule, names.table.fromString(classname));
            sym.complete();

            if ((sym.flags() & Flags.INTERFACE) != 0 && !testInterfaces)
                continue;

            for (Symbol s : sym.members_field.getSymbols(NON_RECURSIVE)) {
                System.err.println("Checking member " + s);
                switch (s.kind) {
                    case TYP: {
                        String name = s.flatName().toString();
                        if (!classes.contains(name)) {
                            classes.add(name);
                            work.add(name);
                        }
                        break;
                    }
                    case MTH:
                        verify((MethodSymbol) s, expectNames);
                        break;
                }

            }
        }
    }

    void verify(MethodSymbol m, boolean expectNames) {
        if ((m.flags() & Flags.SYNTHETIC) != 0 && !testSyntheticMethods)
            return;

        //System.err.println("verify: " + m.params());
        int i = 1;
        for (VarSymbol v: m.params()) {
            String expectName;
            if (expectNames)
                expectName = getExpectedName(v, i);
            else
                expectName = "arg" + (i - 1);
            checkEqual(expectName, v.name.toString());
            i++;
        }
    }

    String getExpectedName(VarSymbol v, int i) {
        // special cases:
        // synthetic method
        if (((v.owner.owner.flags() & Flags.ENUM) != 0)
                && v.owner.name.toString().equals("valueOf"))
            return "name";
        // interfaces don't have saved names
        // -- no Code attribute for the LocalVariableTable attribute
        if ((v.owner.owner.flags() & Flags.INTERFACE) != 0)
            return "arg" + (i - 1);
        // abstract methods don't have saved names
        // -- no Code attribute for the LocalVariableTable attribute
        if ((v.owner.flags() & Flags.ABSTRACT) != 0)
            return "arg" + (i - 1);
        // bridge methods use argN. No LVT for them anymore
        if ((v.owner.flags() & Flags.BRIDGE) != 0)
            return "arg" + (i - 1);

        // The rest of this method assumes the local conventions in the test program
        Type t = v.type;
        String s;
        if (t.hasTag(TypeTag.CLASS))
            s = ((ClassType) t).tsym.name.toString();
        else
            s = t.toString();
        return String.valueOf(Character.toLowerCase(s.charAt(0))) + i;
    }

    void compile(File outDir, String... opts) throws Exception {
        //File testSrc = new File(System.getProperty("test.src"), ".");
        List<String> args = new ArrayList<String>();
        args.add("-d");
        args.add(outDir.getPath());
        args.addAll(Arrays.asList(opts));
        //args.add(new File(testSrc, "Test.java").getPath());
        args.add("Test.java");
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        int rc = com.sun.tools.javac.Main.compile(args.toArray(new String[args.size()]), pw);
        pw.close();
        if (rc != 0) {
            System.err.println(sw.toString());
            throw new Exception("compilation failed unexpectedly");
        }
    }

    Set<String> getTopLevelClasses(File outDir) {
        Set<String> classes = new HashSet<String>();
        for (String f: outDir.list()) {
            if (f.endsWith(".class") && !f.contains("$"))
                classes.add(f.replace(".class", ""));
        }
        return classes;
    }

    void checkEqual(String expect, String found) {
        if (!expect.equals(found))
            error("mismatch: expected:" + expect + " found:" + found);
    }

    void error(String msg) {
        System.err.println(msg);
        errors++;
        throw new Error();
    }

    int errors;
}
