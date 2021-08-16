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
 * @bug 7118412
 * @summary Shadowing of type-variables vs. member types
 * @modules jdk.compiler
 */
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

public class ShadowingTest {

    // We generate a method "test" that tries to call T.<something,
    // depending on the value of MethodCall>.  This controls whether
    // "test" is static or not.
    private enum MethodContext {
        STATIC("static "),
        INSTANCE("");

        public final String methodcontext;

        MethodContext(final String methodcontext) {
            this.methodcontext = methodcontext;
        }
    }

    // These control whether or not a type parameter, method type
    // parameter, or inner class get declared (and in the case of
    // inner classes, whether it's static or not.

    private enum MethodTypeParameterDecl {
        NO(""),
        YES("<T extends Number> ");

        public final String tyvar;

        MethodTypeParameterDecl(final String tyvar) {
            this.tyvar = tyvar;
        }
    }

    private enum InsideDef {
        NONE(""),
        STATIC("static class T { public void inner() {} }\n"),
        INSTANCE("class T { public void inner() {} }\n");

        public final String instancedef;

        InsideDef(final String instancedef) {
            this.instancedef = instancedef;
        }
    }

    private enum TypeParameterDecl {
        NO(""),
        YES("<T extends Collection>");

        public final String tyvar;

        TypeParameterDecl(final String tyvar) {
            this.tyvar = tyvar;
        }
    }

    // Represents what method we try to call.  This is a way of
    // checking which T we're seeing.
    private enum MethodCall {
        // Method type variables extend Number, so we have intValue
        METHOD_TYPEVAR("intValue"),
        // The inner class declaration has a method called "inner"
        INNER_CLASS("inner"),
        // The class type variables extend Collection, so we call iterator
        TYPEVAR("iterator"),
        // The outer class declaration has a method called "outer"
        OUTER_CLASS("outer");

        public final String methodcall;

        MethodCall(final String methodcall) {
            this.methodcall = methodcall;
        }

    }

    public boolean succeeds(final MethodCall call,
                            final MethodTypeParameterDecl mtyvar,
                            final MethodContext ctx,
                            final InsideDef inside,
                            final TypeParameterDecl tyvar) {
        switch(call) {
            // We want to resolve to the method type variable
        case METHOD_TYPEVAR: switch(mtyvar) {
                // If the method type variable exists, then T will
                // resolve to it, and we'll have intValue.
            case YES: return true;
                // Otherwise, this cannot succeed.
            default: return false;
            }
            // We want to resolve to the inner class
        case INNER_CLASS: switch(mtyvar) {
                // The method type parameter will shadow the inner
                // class, so there can't be one.
            case NO: switch(ctx) {
                    // If we're not static, then either one should succeed.
                case INSTANCE: switch(inside) {
                    case INSTANCE:
                    case STATIC:
                        return true;
                    default: return false;
                    }
                case STATIC: switch(inside) {
                        // If we are static, and the inner class is
                        // static, then we also succeed, because we
                        // can't see the type variable.
                    case STATIC: return true;
                    case INSTANCE: switch(tyvar) {
                            // If we're calling from a non-static
                            // context, there can't be a class type
                            // variable, because that will shadow the
                            // static inner class definition.
                        case NO: return true;
                        default: return false;
                        }
                        // If the inner class isn't declared, we can't
                        // see it.
                    default: return false;
                    }
                    // Can't get here.
                default: return false;
                }
            default: return false;
            }
            // We want to resolve to the class type parameter
        case TYPEVAR: switch(mtyvar) {
                // We can't have a method type parameter, as that would
                // shadow the class type parameter
            case NO: switch(ctx) {
                case INSTANCE: switch(inside) {
                        // We have to be in an instance context.  If
                        // we're static, we can't see the type
                        // variable.
                    case NONE: switch(tyvar) {
                            // Obviously, the type parameter has to be declared.
                        case YES: return true;
                        default: return false;
                        }
                    default: return false;
                    }
                default: return false;
                }
            default: return false;
            }
            // We want to resolve to the outer class
        case OUTER_CLASS: switch(mtyvar) {
            case NO: switch(inside) {
                case NONE: switch(tyvar) {
                        // Basically, nothing else can be declared, or
                        // else we can't see it.  Even if our context
                        // is static, the compiler will complain if
                        // non-static T's exist, because they will
                        // shadow the outer class.
                    case NO: return true;
                    default: return false;
                    }
                default: return false;
                }
            default: return false;
            }
        }
        return false;
    }

    private static final File classesdir = new File("7118412");

    private int errors = 0;

    private int dirnum = 0;

    private void doTest(final MethodTypeParameterDecl mtyvar,
                        final TypeParameterDecl tyvar,
                        final InsideDef insidedef, final MethodContext ctx,
                        final MethodCall call)
        throws IOException {
        final String content = "import java.util.Collection;\n" +
            "class Test" + tyvar.tyvar + " {\n" +
            "  " + insidedef.instancedef +
            "  " + ctx.methodcontext + mtyvar.tyvar + "void test(T t) { t." +
            call.methodcall + "(); }\n" +
            "}\n" +
            "class T { void outer() {} }\n";
        final File dir = new File(classesdir, "" + dirnum);
        final File Test_java = writeFile(dir, "Test.java", content);
        dirnum++;
        if(succeeds(call, mtyvar, ctx, insidedef, tyvar)) {
            if(!assert_compile_succeed(Test_java))
                System.err.println("Failed file:\n" + content);
        }
        else {
            if(!assert_compile_fail(Test_java))
                System.err.println("Failed file:\n" + content);
        }
    }

    private void run() throws Exception {
        classesdir.mkdir();
        for(MethodTypeParameterDecl mtyvar : MethodTypeParameterDecl.values())
            for(TypeParameterDecl tyvar : TypeParameterDecl.values())
                for(InsideDef insidedef : InsideDef.values())
                    for(MethodContext ctx : MethodContext.values())
                        for(MethodCall methodcall : MethodCall.values())
                            doTest(mtyvar, tyvar, insidedef, ctx, methodcall);
        if (errors != 0)
            throw new Exception("ShadowingTest test failed with " +
                                errors + " errors.");
    }

    private boolean assert_compile_fail(final File file) {
        final String filename = file.getPath();
        final String[] args = { filename };
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw);
        final int rc = com.sun.tools.javac.Main.compile(args, pw);
        pw.close();
        if (rc == 0) {
            System.err.println("Compilation of " + file.getName() +
                               " didn't fail as expected.");
            errors++;
            return false;
        } else return true;
    }

    private boolean assert_compile_succeed(final File file) {
        final String filename = file.getPath();
        final String[] args = { filename };
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw);
        final int rc = com.sun.tools.javac.Main.compile(args, pw);
        pw.close();
        if (rc != 0) {
            System.err.println("Compilation of " + file.getName() +
                               " didn't succeed as expected.  Output:");
            System.err.println(sw.toString());
            errors++;
            return false;
        } else return true;
    }

    private File writeFile(final File dir,
                           final String path,
                           final String body) throws IOException {
        final File f = new File(dir, path);
        f.getParentFile().mkdirs();
        final FileWriter out = new FileWriter(f);
        out.write(body);
        out.close();
        return f;
    }

    public static void main(String... args) throws Exception {
        new ShadowingTest().run();
    }

}
