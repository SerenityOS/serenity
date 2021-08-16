/*
 * Copyright (c) 2013, 2021, Oracle and/or its affiliates. All rights reserved.
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

package vm.runtime.defmeth.shared;

import vm.runtime.defmeth.shared.data.Clazz;
import java.io.PrintWriter;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import nsk.share.Pair;
import nsk.share.TestFailure;
import vm.runtime.defmeth.shared.data.method.param.*;


/**
 * Utility class with auxiliary miscellaneous methods.
 */
public class Util {
    public static class Transformer {
        private static Instrumentation inst;

        public static void premain(String agentArgs, Instrumentation inst) {
            Transformer.inst = inst;

            /*
            inst.addTransformer(new ClassFileTransformer() {
                @Override
                public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
                    System.out.println("Retransform (initial): " + className);
                    return classfileBuffer;
                }
            });
            */
        }
    }

    /**
     * Concatenate {@code strings} array interleaving with {@code sep} in between.
     *
     * @param sep
     * @param strings
     * @return
     */
    public static String intersperse(String sep, String... strings) {
        StringBuilder sb = new StringBuilder();
        if (strings.length == 0) {
            return "";
        } else if (strings.length == 1) {
            return strings[0];
        } else {
            sb.append(strings[0]);

            for (int i=1; i<strings.length; i++) {
                sb.append(sep).append(strings[i]);
            }

            return sb.toString();
        }
    }

    /**
     * Construct array of names for an array of {@code Clazz} instances.
     *
     * @param clazzes
     * @return
     */
    public static String[] asStrings(Clazz[] clazzes) {
        String[] result = new String[clazzes.length];
        for (int i = 0; i < clazzes.length; i++) {
            result[i] = clazzes[i].intlName();
        }

        return result;
    }

    /**
     * Get the name of the test currently being executed
     *
     * @return name of the test being executed
     */
    public static String getTestName() {
        // Hack: examine stack trace and extract test method's name from there
        try {
            throw new Exception();
        } catch (Exception e) {
            for (StackTraceElement elem : e.getStackTrace()) {
                String className = elem.getClassName();
                String methodName = elem.getMethodName();

                if (className.startsWith("vm.runtime.defmeth.") &&
                        methodName.startsWith("test")) {
                    return String.format("%s.%s",
                            className.replaceAll(".*\\.", ""), methodName);
                }
            }

            return "Unknown";
        }
    }

    /**
     * Pretty-print {@code byte[] classFile} to stdout.
     *
     * @param classFile
     */
    public static void printClassFile(byte[] classFile) {
        int flags =  jdk.internal.org.objectweb.asm.ClassReader.SKIP_DEBUG;

        classFile = classFile.clone();

        jdk.internal.org.objectweb.asm.ClassReader cr =
                new  jdk.internal.org.objectweb.asm.ClassReader(classFile);

        cr.accept(new  jdk.internal.org.objectweb.asm.util.TraceClassVisitor(new PrintWriter(System.out)), flags);
    }

    /**
     * Print ASM version (sequence of calls to ASM API to produce same class file)
     * of {@code classFile} to stdout.
     *
     * @param classFile
     */
    public static void asmifyClassFile(byte[] classFile) {
        int flags =  jdk.internal.org.objectweb.asm.ClassReader.SKIP_DEBUG;

        jdk.internal.org.objectweb.asm.ClassReader cr =
                new  jdk.internal.org.objectweb.asm.ClassReader(classFile);

        //cr.accept(new TraceClassVisitor(new PrintWriter(System.out)), flags);
        cr.accept(new jdk.internal.org.objectweb.asm.util.TraceClassVisitor(null,
                        new jdk.internal.org.objectweb.asm.util.ASMifier(),
                        new PrintWriter(System.out)), flags);
    }

    /**
     * Parse method descriptor and split it into parameter types names and
     * return type name.
     *
     * @param desc
     * @return {@code Pair} of parameter types names and
     */
    public static Pair<String[],String> parseDesc(String desc) {
        Pattern p = Pattern.compile("\\((.*)\\)(.*)");
        Matcher m = p.matcher(desc);

        if (m.matches()) {
            String opts = m.group(1);
            String returnVal = m.group(2);

            return Pair.of(parseParams(opts), returnVal);
        } else {
            throw new IllegalArgumentException(desc);
        }

    }

    /**
     * Check whether a type isn't Void by it's name.
     *
     * @param type return type name
     * @return
     */
    public static boolean isNonVoid(String type) {
        return !("V".equals(type));
    }

    /**
     * Split a sequence of type names (in VM internal form).
     *
     * Example:
     *   "BCD[[ALA;I" => [ "B", "C", "D", "[[A", "LA;", "I" ]
     *
     * @param str
     * @return
     */
    public static String[] parseParams(String str) {
        List<String> params = new ArrayList<>();

        /* VM basic type notation:
                B   byte    signed byte
                C   char    Unicode character code point in the Basic Multilingual Plane, encoded with UTF-16
                D   double  double-precision floating-point value
                F   float   single-precision floating-point value
                I   int     integer
                J   long    long integer
                L Classname ;   reference   an instance of class Classname
                S   short   signed short
                Z   boolean true or false
                [   reference   one array dimension
         */
        int i = 0;
        int start = 0;
        while (i < str.length()) {
            char c = str.charAt(i);
            switch (c) {
                case 'B': case 'C': case 'D': case 'F':
                case 'I': case 'J': case 'S': case 'Z':
                    params.add(str.substring(start, i+1));
                    start = i+1;
                    break;
                case 'L':
                    int k = str.indexOf(';', i);
                    if (k != 1) {
                        params.add(str.substring(start, k+1));
                        start = k+1;
                        i = k;
                    } else {
                        throw new IllegalArgumentException(str);
                    }
                    break;
                case '[':
                    break;
                default:
                    throw new IllegalArgumentException(
                            String.format("%d(%d): %c \'%s\'", i, start, c, str));
            }

            i++;
        }

        if (start != str.length()) {
            throw new IllegalArgumentException(str);
        }

        return params.toArray(new String[0]);
    }

    /**
     * Returns default values for different types:
     *   - byte:    0
     *   - short:   0
     *   - int:     0
     *   - long:    0L
     *   - char:    \U0000
     *   - boolean: false
     *   - float:   0.0f
     *   - double:  0.0d
     *   - array:   null
     *   - Object:  null
     *
     * @param types
     * @return
     */
    public static Param[] getDefaultValues(String[] types) {
        List<Param> values = new ArrayList<>();

        for (String type : types) {
            switch (type) {
                case "I":  case "B": case "C": case "Z": case "S":
                    values.add(new IntParam(0));
                    break;
                case "J":
                    values.add(new LongParam(0L));
                    break;
                case "D":
                    values.add(new DoubleParam(0.0d));
                    break;
                case "F":
                    values.add(new FloatParam(0.0f));
                    break;
                default:
                    if (type.startsWith("L") || type.startsWith("[")) {
                        values.add(new NullParam());
                    } else {
                        throw new IllegalArgumentException(Arrays.toString(types));
                    }
                    break;
            }
        }

        return values.toArray(new Param[0]);
    }

    /**
     * Decode class name from internal VM representation into normal Java name.
     * Internal class naming convention is extensively used to describe method type (descriptor).
     *
     * Examples:
     *    "Ljava/lang/Object" => "java.lang.Object"
     *    "I" => "int"
     *    "[[[C" => "char[][][]"
     *
     * @param name
     * @return
     */
    public static String decodeClassName(String name) {
        switch (name) {
            case "Z": return "boolean";
            case "B": return "byte";
            case "S": return "short";
            case "C": return "char";
            case "I": return "int";
            case "J": return "long";
            case "F": return "float";
            case "D": return "double";
            default:
                if (name.startsWith("L")) {
                    // "Ljava/lang/String;" => "java.lang.String"
                    return name.substring(1, name.length()-1).replaceAll("/", ".");
                } else if (name.startsWith("[")) {
                    // "[[[C" => "char[][][]"
                    return decodeClassName(name.substring(1)) + "[]";
                } else {
                    throw new IllegalArgumentException(name);
                }
        }
    }

    /**
     * Decode class name from internal VM format into regular name and resolve it using {@code cl} {@code ClassLoader}.
     * It is used during conversion of method type from string representation to strongly typed variants (e.g. MethodType).
     *
     * @param name
     * @param cl
     * @return
     */
    public static Class decodeClass(String name, ClassLoader cl) {
        switch (name) {
            case "Z": return boolean.class;
            case "B": return byte.class;
            case "S": return short.class;
            case "C": return char.class;
            case "I": return int.class;
            case "J": return long.class;
            case "F": return float.class;
            case "D": return double.class;
            case "V": return void.class;
            default:
                if (name.startsWith("L")) {
                    // "Ljava/lang/String;" => "java.lang.String"
                    String decodedName = name.substring(1, name.length()-1).replaceAll("/", ".");
                    try {
                        return cl.loadClass(decodedName);
                    } catch (Exception e) {
                        throw new Error(e);
                    }
                } else if (name.startsWith("[")) {
                    // "[[[C" => "char[][][]"
                    //return decodeClassName(name.substring(1)) + "[]";
                    throw new UnsupportedOperationException("Resolution of arrays isn't supported yet: "+name);
                } else {
                    throw new IllegalArgumentException(name);
                }
        }
    }

    /**
     * Redefine a class with a new version.
     *
     * @param clz class for redefinition
     */
    static public void retransformClass(final Class<?> clz, final byte[] classFile) {
        ClassFileTransformer transformer = new ClassFileTransformer() {
            @Override
            public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
                if (clz.getClassLoader() == loader && className.equals(clz.getName())) {
                    if (Constants.TRACE_CLASS_REDEF)  System.out.println("RETRANSFORM: " + className);

                    return classFile;
                } else {
                    // leave the class as-is
                    return classfileBuffer;
                }
            }
        };

        Transformer.inst.addTransformer(transformer, true);
        try {
            Transformer.inst.retransformClasses(clz);
        } catch (UnmodifiableClassException e) {
            throw new TestFailure(e);
        } finally {
            Transformer.inst.removeTransformer(transformer);
        }
    }

    /**
     * Redefine a class with a new version (class file in byte array).
     *
     * @param clz class for redefinition
     * @param classFile new version as a byte array
     * @return false if any errors occurred during class redefinition
     */
    static public void redefineClass(Class<?> clz, byte[] classFile) {
        if (clz ==  null) {
            throw new IllegalArgumentException("clz == null");
        }

        if (classFile == null || classFile.length == 0) {
            throw new IllegalArgumentException("Incorrect classFile");
        }

        if (Constants.TRACE_CLASS_REDEF)  System.out.println("REDEFINE: "+clz.getName());

        if (!redefineClassIntl(clz, classFile)) {
            throw new TestFailure("redefineClass failed: "+clz.getName());
        }
    }


    native static public boolean redefineClassIntl(Class<?> clz, byte[] classFile);

    /**
     * Get VM internal name of {@code Class<?> clz}.
     *
     * @param clz
     * @return
     */
    public static String getInternalName(Class<?> clz) {
        if (!clz.isPrimitive()) {
            return clz.getName().replaceAll("\\.", "/");
        } else {
            throw new UnsupportedOperationException();
        }
    }
}
