/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.io.*;
import com.sun.tools.classfile.*;

/**
 * The {@code ClassFileVisitor} reads a class file using the
 * {@code com.sun.tools.classfile} library. It iterates over the methods
 * in a class, and checks MethodParameters attributes against JLS
 * requirements, as well as assumptions about the javac implementations.
 * <p>
 * It enforces the following rules:
 * <ul>
 * <li>All non-synthetic methods with arguments must have the
 * MethodParameters attribute. </li>
 * <li>At most one MethodParameters attribute per method.</li>
 * <li>An empty MethodParameters attribute is not allowed (i.e. no
 * attribute for methods taking no parameters).</li>
 * <li>The number of recorded parameter names much equal the number
 * of parameters, including any implicit or synthetic parameters generated
 * by the compiler.</li>
 * <li>Although the spec allow recording parameters with no name, the javac
 * implementation is assumed to record a name for all parameters. That is,
 * the Methodparameters attribute must record a non-zero, valid constant
 * pool index for each parameter.</li>
 * <li>Check presence, expected names (e.g. this$N, $enum$name, ...) and flags
 * (e.g. ACC_SYNTHETIC, ACC_MANDATED) for compiler generated parameters.</li>
 * <li>Names of explicit parameters must reflect the names in the Java source.
 * This is checked by assuming a design pattern where any name is permitted
 * for the first explicit parameter. For subsequent parameters the following
 * rule is checked: <i>param[n] == ++param[n-1].charAt(0) + param[n-1]</i>
 * </ul>
 */
class ClassFileVisitor extends MethodParametersTester.Visitor {

    MethodParametersTester tester;

    public String cname;
    public boolean isEnum;
    public boolean isInterface;
    public boolean isInner;
    public boolean isPublic;
    public boolean isStatic;
    public boolean isAnon;
    public ClassFile classFile;


    public ClassFileVisitor(MethodParametersTester tester) {
        super(tester);
    }

    public void error(String msg) {
        super.error("classfile: " + msg);
    }

    public void warn(String msg) {
        super.warn("classfile: " + msg);
    }

    /**
     * Read the class and determine some key characteristics, like if it's
     * an enum, or inner class, etc.
     */
    void visitClass(final String cname, final File cfile, final StringBuilder sb) throws Exception {
        this.cname = cname;
        classFile = ClassFile.read(cfile);
        isEnum = classFile.access_flags.is(AccessFlags.ACC_ENUM);
        isInterface = classFile.access_flags.is(AccessFlags.ACC_INTERFACE);
        isPublic = classFile.access_flags.is(AccessFlags.ACC_PUBLIC);
        isInner = false;
        isStatic = false;
        isAnon = false;

        Attribute attr = classFile.getAttribute("InnerClasses");
        if (attr != null) attr.accept(new InnerClassVisitor(), null);
        isAnon = isInner & isAnon;

        sb.append(isStatic ? "static " : "")
            .append(isPublic ? "public " : "")
            .append(isEnum ? "enum " : isInterface ? "interface " : "class ")
            .append(cname).append(" -- ");
        if (isInner) {
            sb.append(isAnon ? "anon" : "inner");
        }
        sb.append("\n");

        for (Method method : classFile.methods) {
            new MethodVisitor().visitMethod(method, sb);
        }
    }

    /**
     * Used to visit InnerClasses_attribute of a class,
     * to determne if this class is an local class, and anonymous
     * inner class or a none-static member class. These types of
     * classes all have an containing class instances field that
     * requires an implicit or synthetic constructor argument.
     */
    class InnerClassVisitor extends AttributeVisitor<Void, Void> {
        public Void visitInnerClasses(InnerClasses_attribute iattr, Void v) {
            try{
                for (InnerClasses_attribute.Info info : iattr.classes) {
                    if (info.getInnerClassInfo(classFile.constant_pool) == null) continue;
                    String in = info.getInnerClassInfo(classFile.constant_pool).getName();
                    if (in == null || !cname.equals(in)) continue;
                    isInner = true;
                    isAnon = null == info.getInnerName(classFile.constant_pool);
                    isStatic = info.inner_class_access_flags.is(AccessFlags.ACC_STATIC);
                    break;
                }
            } catch(Exception e) {
                throw new IllegalStateException(e);
            }
            return null;
        }
    }

    /**
     * Check the MethodParameters attribute of a method.
     */
    class MethodVisitor extends AttributeVisitor<Void, StringBuilder> {

        public String mName;
        public Descriptor mDesc;
        public int mParams;
        public int mAttrs;
        public int mNumParams;
        public boolean mSynthetic;
        public boolean mIsConstructor;
        public boolean mIsClinit;
        public boolean mIsBridge;
        public boolean isFinal;
        public String prefix;

        void visitMethod(Method method, StringBuilder sb) throws Exception {

            mName = method.getName(classFile.constant_pool);
            mDesc = method.descriptor;
            mParams =  mDesc.getParameterCount(classFile.constant_pool);
            mAttrs = method.attributes.attrs.length;
            mNumParams = -1; // no MethodParameters attribute found
            mSynthetic = method.access_flags.is(AccessFlags.ACC_SYNTHETIC);
            mIsConstructor = mName.equals("<init>");
            mIsClinit = mName.equals("<clinit>");
            prefix = cname + "." + mName + "() - ";
            mIsBridge = method.access_flags.is(AccessFlags.ACC_BRIDGE);

            if (mIsClinit) {
                sb = new StringBuilder(); // Discard output
            }
            sb.append(cname).append(".").append(mName).append("(");

            for (Attribute a : method.attributes) {
                a.accept(this, sb);
            }
            if (mNumParams == -1) {
                if (mSynthetic) {
                    // We don't generate MethodParameters attribute for synthetic
                    // methods, so we are creating a parameter pattern to match
                    // ReflectionVisitor API output.
                    for (int i = 0; i < mParams; i++) {
                        if (i == 0)
                            sb.append("arg").append(i);
                        else
                            sb.append(", arg").append(i);
                    }
                    sb.append(")/*synthetic*/");
                } else {
                    sb.append(")");
                }
            }
            sb.append("\n");

            // IMPL: methods with arguments must have a MethodParameters
            // attribute, except possibly some synthetic methods.
            if (mNumParams == -1 && mParams > 0 && ! mSynthetic) {
                error(prefix + "missing MethodParameters attribute");
            }
        }

        public Void visitMethodParameters(MethodParameters_attribute mp,
                                          StringBuilder sb) {

            // SPEC: At most one MethodParameters attribute allowed
            if (mNumParams != -1) {
                error(prefix + "Multiple MethodParameters attributes");
                return null;
            }

            mNumParams = mp.method_parameter_table_length;

            // SPEC: An empty attribute is not allowed!
            if (mNumParams == 0) {
                error(prefix + "0 length MethodParameters attribute");
                return null;
            }

            // SPEC: one name per parameter.
            if (mNumParams != mParams) {
                error(prefix + "found " + mNumParams +
                      " parameters, expected " + mParams);
                return null;
            }

            // IMPL: Whether MethodParameters attributes will be generated
            // for some synthetics is unresolved. For now, assume no.
            if (mSynthetic) {
                warn(prefix + "synthetic has MethodParameter attribute");
            }

            String sep = "";
            String userParam = null;
            for (int x = 0; x <  mNumParams; x++) {
                isFinal = (mp.method_parameter_table[x].flags & AccessFlags.ACC_FINAL) != 0;
                // IMPL: Assume all parameters are named, something.
                int cpi = mp.method_parameter_table[x].name_index;
                if (cpi == 0) {
                    error(prefix + "name expected, param[" + x + "]");
                    return null;
                }

                // SPEC: a non 0 index, must be valid!
                String param = null;
                try {
                    param = classFile.constant_pool.getUTF8Value(cpi);
                    if (isFinal)
                        param = "final " + param;
                    sb.append(sep).append(param);
                    sep = ", ";
                } catch(ConstantPoolException e) {
                    error(prefix + "invalid index " + cpi + " for param["
                          + x + "]");
                    return null;
                }


                // Check availability, flags and special names
                int check = checkParam(mp, param, x, sb, isFinal);
                if (check < 0) {
                    return null;
                }

                // TEST: check test assumptions about parameter name.
                // Expected names are calculated starting with the
                // 2nd explicit (user given) parameter.
                // param[n] == ++param[n-1].charAt(0) + param[n-1]
                String expect = null;
                if (userParam != null) {
                    char c = userParam.charAt(0);
                    expect =  (++c) + userParam;
                }
                if(isFinal && expect != null)
                    expect = "final " + expect;
                if (check > 0) {
                    if(isFinal) {
                        userParam = param.substring(6);
                    } else {
                    userParam = param;
                }
                }
                if (check > 0 && expect != null && !param.equals(expect)) {
                    error(prefix + "param[" + x + "]='"
                          + param + "' expected '" + expect + "'");
                    return null;
                }
            }
            if (mSynthetic) {
                sb.append(")/*synthetic*/");
            } else {
                sb.append(")");
            }
            return null;
        }

        /*
         * Check a parameter for conformity to JLS and javac specific
         * assumptions.
         * Return -1, if an error is detected. Otherwise, return 0, if
         * the parameter is compiler generated, or 1 for an (presumably)
         * explicitly declared parameter.
         */
        int checkParam(MethodParameters_attribute mp, String param, int index,
                       StringBuilder sb, boolean isFinal) {

            boolean synthetic = (mp.method_parameter_table[index].flags
                                 & AccessFlags.ACC_SYNTHETIC) != 0;
            boolean mandated = (mp.method_parameter_table[index].flags
                                & AccessFlags.ACC_MANDATED) != 0;

            // Setup expectations for flags and special names
            String expect = null;
            boolean allowMandated = false;
            boolean allowSynthetic = false;
            if (mSynthetic || synthetic) {
                // not an implementation gurantee, but okay for now
                expect = "arg" + index; // default
            }
            if (mIsConstructor) {
                if (isEnum) {
                    if (index == 0) {
                        expect = "\\$enum\\$name";
                        allowSynthetic = true;
                    } else if(index == 1) {
                        expect = "\\$enum\\$ordinal";
                        allowSynthetic = true;
                    }
                } else if (index == 0) {
                    if (isAnon) {
                        expect = "this\\$[0-9]+";
                        allowMandated = true;
                        if (isFinal) {
                            expect = "final this\\$[0-9]+";
                        }
                    } else if (isInner && !isStatic) {
                        expect = "this\\$[0-9]+";
                        allowMandated = true;
                        if (!isPublic) {
                            // some but not all non-public inner classes
                            // have synthetic argument. For now we give
                            // the test a bit of slack and allow either.
                            allowSynthetic = true;
                        }
                        if (isFinal) {
                            expect = "final this\\$[0-9]+";
                        }
                    }
                }

                if (synthetic && !mandated && !allowSynthetic) {
                    //patch treatment for local captures
                    if (isAnon || (isInner & !isStatic)) {
                        expect = "val\\$.*";
                        allowSynthetic = true;
                        if (isFinal) {
                            expect = "final val\\$.*";
                        }
                    }
                }
            } else if (isEnum && mNumParams == 1 && index == 0 && mName.equals("valueOf")) {
                expect = "name";
                allowMandated = true;
            } else if (mIsBridge) {
                allowSynthetic = true;
                /*  you can't expect an special name for bridges' parameters.
                 *  The name of the original parameters are now copied.
                 */
                expect = null;
            }
            if (mandated) sb.append("/*implicit*/");
            if (synthetic) sb.append("/*synthetic*/");

            // IMPL: our rules a somewhat fuzzy, sometimes allowing both mandated
            // and synthetic. However, a parameters cannot be both.
            if (mandated && synthetic) {
                error(prefix + "param[" + index + "] == \"" + param
                      + "\" ACC_SYNTHETIC and ACC_MANDATED");
                return -1;
            }
            // ... but must be either, if both "allowed".
            if (!(mandated || synthetic) && allowMandated && allowSynthetic) {
                error(prefix + "param[" + index + "] == \"" + param
                      + "\" expected ACC_MANDATED or ACC_SYNTHETIC");
                return -1;
            }

            // ... if only one is "allowed", we meant "required".
            if (!mandated && allowMandated && !allowSynthetic) {
                error(prefix + "param[" + index + "] == \"" + param
                      + "\" expected ACC_MANDATED");
                return -1;
            }
            if (!synthetic && !allowMandated && allowSynthetic) {
                error(prefix + "param[" + index + "] == \"" + param
                      + "\" expected ACC_SYNTHETIC");
                return -1;
            }

            // ... and not "allowed", means prohibited.
            if (mandated && !allowMandated) {
                error(prefix + "param[" + index + "] == \"" + param
                      + "\" unexpected, is ACC_MANDATED");
                return -1;
            }
            if (synthetic && !allowSynthetic) {
                error(prefix + "param[" + index + "] == \"" + param
                      + "\" unexpected, is ACC_SYNTHETIC");
                return -1;
            }

            // Test special name expectations
            if (expect != null) {
                if (param.matches(expect)) {
                    return 0;
                }
                error(prefix + "param[" + index + "]='" + param +
                      "' expected '" + expect + "'");
                return -1;
            }

            // No further checking for synthetic methods.
            if (mSynthetic) {
                return 0;
            }
            // Otherwise, do check test parameter naming convention.
            return 1;
        }
    }
}
