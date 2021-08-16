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
 * @bug 8005085 8005877 8004829 8005681 8006734 8006775
 * @summary Combinations of Target ElementTypes on (repeated)type annotations.
 * @modules jdk.jdeps/com.sun.tools.classfile
 */

import com.sun.tools.classfile.*;
import java.io.File;

public class CombinationsTargetTest1 extends ClassfileTestHelper {

    // Test count helps identify test case in event of failure.
    int testcount = 0;

    // Base test case template descriptions
    enum srce  {
        src1("(repeating) type annotations at class level"),
        src2("(repeating) type annotations on method"),
        src3("(repeating) type annotations on wildcard, type arguments in anonymous class"),
        src4("(repeating) type annotations on type parameters, bounds and  type arguments on class decl"),
        src5("(repeating) type annotations on type parameters, bounds and  type arguments on method"),
        src6("(repeating) type annotations on type parameters, bounds and  type arguments in method");

        String description;

        srce(String desc) {
            this.description = this + ": " +desc;
        }
    }

    String[] ETypes={"TYPE", "FIELD", "METHOD", "PARAMETER", "CONSTRUCTOR",
                     "LOCAL_VARIABLE", "ANNOTATION_TYPE", "PACKAGE"};

    // local class tests will have an inner class.
    Boolean hasInnerClass=false;
    String innerClassname="";

    public static void main(String[] args) throws Exception {
        new CombinationsTargetTest1().run();
    }

    void run() throws Exception {
        // Determines which repeat and order in source(ABMix).
        Boolean As= false, BDs=true, ABMix=false;
        int testrun=0;
        Boolean [][] bRepeat = new Boolean[][]{{false,false,false},//no repeats
                                               {true,false,false}, //repeat @A
                                               {false,true,false}, //repeat @B
                                               {true,true,false},  //repeat both
                                               {false,false,true}  //repeat mix
        };
        for(Boolean[] bCombo : bRepeat) {
            As=bCombo[0]; BDs=bCombo[1]; ABMix=bCombo[2];
            for(String et : ETypes) {
               switch(et) {
                   case "METHOD":
                       test( 8,  0, 2, 0, As, BDs, ABMix, "CLASS",   et, ++testrun, srce.src1);
                       test(10,  0, 2, 0, As, BDs, ABMix, "CLASS",   et, ++testrun, srce.src2);
                       test( 6,  0, 0, 0, As, BDs, ABMix, "CLASS",   et, ++testrun, srce.src3);
                       test(10,  0, 2, 0, As, BDs, ABMix, "CLASS",   et, ++testrun, srce.src5);
                       test( 0,  8, 0, 2, As, BDs, ABMix, "RUNTIME", et, ++testrun, srce.src1);
                       test( 0, 10, 0, 2, As, BDs, ABMix, "RUNTIME", et, ++testrun, srce.src2);
                       test( 0,  6, 0, 0, As, BDs, ABMix, "RUNTIME", et, ++testrun, srce.src3);
                       test( 0, 10, 0, 2, As, BDs, ABMix, "RUNTIME", et, ++testrun, srce.src5);
                       break;
                   case "CONSTRUCTOR":
                   case "FIELD":
                       test( 8,  0, 4, 0, As, BDs, ABMix, "CLASS",   et, ++testrun, srce.src1);
                       test( 6,  0, 3, 0, As, BDs, ABMix, "CLASS",   et, ++testrun, srce.src4);
                       test( 9,  0, 0, 0, As, BDs, ABMix, "CLASS",   et, ++testrun, srce.src6);
                       test( 0,  8, 0, 4, As, BDs, ABMix, "RUNTIME", et, ++testrun, srce.src1);
                       test( 0,  6, 0, 3, As, BDs, ABMix, "RUNTIME", et, ++testrun, srce.src4);
                       test( 0,  9, 0, 0, As, BDs, ABMix, "RUNTIME", et, ++testrun, srce.src6);
                       break;
                   default:/*TYPE,PARAMETER,LOCAL_VARIABLE,ANNOTATION_TYPE,PACKAGE*/
                       test( 8,  0, 2, 0, As, BDs, ABMix, "CLASS",   et, ++testrun, srce.src1);
                       test( 6,  0, 3, 0, As, BDs, ABMix, "CLASS",   et, ++testrun, srce.src4);
                       test( 0,  8, 0, 2, As, BDs, ABMix, "RUNTIME", et, ++testrun, srce.src1);
                       test( 0,  6, 0, 3, As, BDs, ABMix, "RUNTIME", et, ++testrun, srce.src4);
               }
            }
        }
    }

    public void test(int tinv, int tvis, int inv, int vis, Boolean Arepeats,
                     Boolean BDrepeats, Boolean ABmix, String rtn, String et2,
                     Integer N, srce source) throws Exception {
        ++testcount;
        expected_tvisibles = tvis;
        expected_tinvisibles = tinv;
        expected_visibles = vis;
        expected_invisibles = inv;
        File testFile = null;
        String tname="Test" + N.toString();
        hasInnerClass=false;
        String testDef = "Test " + testcount + " parameters: tinv=" + tinv +
                ", tvis=" + tvis + ", inv=" + inv + ", vis=" + vis +
                ", Arepeats=" + Arepeats + ", BDrepeats=" + BDrepeats +
                ", ABmix=" + ABmix + ", retention: " + rtn + ", anno2: " +
                et2 + ", src=" + source;

        println(testDef);
        // Create test source and File.
        String sourceString = sourceString(tname, rtn, et2, Arepeats,
                                           BDrepeats, ABmix, source);
        testFile = writeTestFile(tname+".java", sourceString);
        // Compile test source and read classfile.
        File classFile = null;
        try {
            classFile = compile(testFile);
        } catch (Error err) {
            System.err.println("Failed compile. Source:\n" + sourceString);
            throw err;
        }
        //if sourcString() set hasInnerClass it also set innerClassname.
        if(hasInnerClass) {
            StringBuffer sb = new StringBuffer(classFile.getAbsolutePath());
            classFile=new File(sb.insert(sb.lastIndexOf(".class"),
                                         innerClassname).toString());
        }
        ClassFile cf = ClassFile.read(classFile);

        //Test class,fields and method counts.
        test(cf);

        for (Field f : cf.fields) {
            test(cf, f);
        }
        for (Method m: cf.methods) {
            test(cf, m);
        }
        countAnnotations();
        if (errors > 0) {
            System.err.println( testDef );
            System.err.println( "Source:\n" + sourceString );
            throw new Exception( errors + " errors found" );
        }
        println("Pass");
    }

    //
    // Source for test cases
    //
    String sourceString(String testname, String retentn, String annot2,
                        Boolean Arepeats, Boolean BDrepeats, Boolean ABmix,
                        srce src) {

        String As = "@A", Bs = "@B", Ds = "@D";
        if(Arepeats) As = "@A @A";
        if(BDrepeats) {
            Bs = "@B @B";
            Ds = "@D @D";
        }
        if(ABmix) { As = "@A @B"; Bs = "@A @B"; Ds = "@D @D"; }

        // Source to check for TYPE_USE and TYPE_PARAMETER annotations.
        // Source base (annotations) is same for all test cases.
        String source = new String();
        String imports = new String("import java.lang.annotation.*; \n" +
            "import static java.lang.annotation.RetentionPolicy.*; \n" +
            "import static java.lang.annotation.ElementType.*; \n" +
            "import java.util.List; \n" +
            "import java.util.HashMap; \n" +
            "import java.util.Map; \n\n");

            String sourceBase = new String("@Retention("+retentn+")\n" +
            "@Target({TYPE_USE,_OTHER_})\n" +
            "@Repeatable( AC.class )\n" +
            "@interface A { }\n\n" +

            "@Retention("+retentn+")\n" +
            "@Target({TYPE_USE,_OTHER_})\n" +
            "@interface AC { A[] value(); }\n\n" +

            "@Retention("+retentn+")\n" +
            "@Target({TYPE_USE,_OTHER_})\n" +
            "@Repeatable( BC.class )\n" +
            "@interface B { }\n\n" +

            "@Retention("+retentn+")\n" +
            "@Target({TYPE_USE,_OTHER_})\n" +
            "@interface BC { B[] value(); } \n\n" +

            "@Retention("+retentn+")\n" +
            "@Target({TYPE_PARAMETER,_OTHER_})\n" +
            "@interface C { }\n\n" +

            "@Retention("+retentn+")\n" +
            "@Target({TYPE_USE,TYPE_PARAMETER,_OTHER_})\n" +
            "@Repeatable(DC.class)\n" +
            "@interface D { }\n\n" +

            "@Retention("+retentn+")\n" +
            "@Target({TYPE_USE,TYPE_PARAMETER,_OTHER_})\n" +
            "@interface DC { D[] value(); }\n");

        // Test case sources with sample generated source.
        switch(src) {
            case src1: // repeating type annotations at class level
                    /*
                     * @A @B class Test1 {
                     * @A @B Test1(){}
                     * @A @B Integer i1 = 0;
                     * String @A @B [] @A @B [] sa = null;
                     * // type usage in method body
                     * String test(Test1 this, String param, String ... vararg) {
                     *     Object o = new  String  [3];
                     *     return (String) null;
                     * }}
                     */
                source = new String(
                    "// " + src.description + "\n" +
                    "_As_ _Bs_ class " + testname + " {\n" +
                    "_As_ _Bs_ " + testname +"(){} \n" +
                    "_As_ _Bs_ Integer i1 = 0; \n" +
                    "String _As_ _Bs_ [] _As_ _Bs_ [] sa = null; \n" +
                    "// type usage in method body \n" +
                    "String test("+testname+" this, " +
                       "String param, String ... vararg) { \n" +
                    "    Object o = new  String  [3]; \n" +
                    "    return (String) null; \n" +
                    "}\n" +
                    "}\n\n").concat(sourceBase).replace("_OTHER_", annot2).replace("_As_",As).replace("_Bs_",Bs) +
                    "\n";
                break;
            case src2: // (repeating) type annotations on method.
                    /*
                     * class Test12 {
                     * Test12(){}
                     * // type usage on method
                     * @A @B String test(@A @B  Test12 this, @A @B  String param, @A @B  String @A @B  ... vararg) {
                     *     Object o = new String [3];
                     *     return (String) null;
                     * }}
                     */
                source = new String(
                    "// " + src.description + "\n" +
                    "class " + testname + " {\n" +
                    testname +"(){} \n" +
                    "// type usage on method \n" +
                    "_As_ _Bs_ String test(_As_ _Bs_  "+testname+" this, " +
                       "_As_ _Bs_  String param, _As_ _Bs_  String _As_ _Bs_  ... vararg) { \n" +
                    "    Object o = new String [3]; \n" +
                    "    return (String) null; \n" +
                    "}\n" +
                    "}\n\n").concat(sourceBase).replace("_OTHER_", annot2).replace("_As_",As).replace("_Bs_",Bs) +
                    "\n";
                break;
            case src3: //(repeating) annotations on wildcard, type arguments in anonymous class.
                    /*
                     * class Test13<T extends Object> {
                     *     public T data = null;
                     *     T getData() { return data;}
                     *     String mtest( Test13<String> t){ return t.getData(); }
                     *     public void test() {
                     *         mtest( new Test13<String>() {
                     *                  void m1(List<@A @B ? extends @A @B  Object> lst) {}
                     *                  void m2() throws@A @B Exception { }
                     *                });
                     *     }
                     * }
                     */
                source = new String( source +
                    "// " + src.description + "\n" +
                    "class " + testname + "<T extends Object> {\n" +
                    "    public T data = null;\n" +
                    "    T getData() { return data;}\n" +
                    "    String mtest( " + testname + "<String> t){ return t.getData(); }\n" +
                    "    public void test() {\n" +
                    "        mtest( new " + testname + "<String>() {\n" +
                    "                 void m1(List<_As_ _Bs_ ? extends _As_ _Bs_  Object> lst) {}\n" +
                    "                 void m2() throws_As_ _Bs_ Exception { }\n" +
                    "               });\n" +
                    "    }\n" +
                    "}\n\n").concat(sourceBase).replace("_OTHER_", annot2).replace("_As_",As).replace("_Bs_",Bs) +
                    "\n";
                    hasInnerClass=true;
                    innerClassname="$1";
            break;
            case src4: // (repeating)annotations on type parameters, bounds and  type arguments on class decl.
                    /*
                     * @A @B @D
                     * class Test2<@A @B @C @D T extends @A @B Object> {
                     *     Map<List<String>, Integer> map =
                     *         new HashMap<List< String>, Integer>();
                     *     Map<List<String>,Integer> map2 = new HashMap<>();
                     *     String test(Test2<T> this) { return null;}
                     *     <T> String genericMethod(T t) { return null; }
                     * }
                     */
                source = new String( source +
                    "// " + src.description + "\n" +
                    "_As_ _Bs_ _Ds_\n" +  //8004829: A and B on type parameter below.
                    "class " + testname + "<_As_ _Bs_ @C _Ds_ T extends _As_ _Bs_ Object> {\n" +
                    "    Map<List<String>, Integer> map =\n" +
                    "        new HashMap<List< String>, Integer>();\n" +
                    "    Map<List<String>,Integer> map2 = new HashMap<>();\n" +
                    "    String test(" + testname + "<T> this) { return null;}\n" +
                    "    <T> String genericMethod(T t) { return null; }\n" +
                    "}\n\n").concat(sourceBase).replace("_OTHER_", annot2).replace("_As_",As).replace("_Bs_",Bs).replace("_Ds_",Ds) +
                    "\n";
                break;
            case src5: // (repeating) annotations on type parameters, bounds and  type arguments on method.
                    /*
                     * class Test14<T extends Object> {
                     *     Map<List<String>, Integer> map =
                     *         new HashMap<List<String>, Integer>();
                     *     Map<List<String>, Integer> map2 = new HashMap<>();
                     *     String test(@A @B Test14<@D T> this) { return null;}
                     *     <@C @D T> @A @B String genericMethod(@A @B @D T t) { return null; }
                     * }
                     */
                source = new String( source +
                    "// " + src.description + "\n" +
                    "class " + testname + "<T extends Object> {\n" +
                    "    Map<List<String>, Integer> map =\n" +
                    "        new HashMap<List<String>, Integer>();\n" +
                    "    Map<List<String>, Integer> map2 = new HashMap<>();\n" +
                    "    String test(_As_ _Bs_ " + testname + "<_Ds_ T> this) { return null;}\n" +
                    "    <@C _Ds_ T> _As_ _Bs_ String genericMethod(_As_ _Bs_ _Ds_ T t) { return null; }\n" +
                    "}\n\n").concat(sourceBase).replace("_OTHER_", annot2).replace("_As_",As).replace("_Bs_",Bs).replace("_Ds_",Ds) +
                    "\n";
                break;
            case src6: // repeating annotations on type parameters, bounds and  type arguments in method.
                    /*
                     * class Test7{
                     *     <E extends Comparable> Map<List<E>, E > foo(E e) {
                     *         class maptest <@A @B @D E> {
                     *             Map<List<@A @B @D E>,@A @B @D E> getMap() {
                     *                 return new HashMap<List<E>,E>();
                     *             }
                     *         }
                     *         return new maptest<E>().getMap();
                     *    }
                     *    Map<List<String>,String> shm = foo(new String("hello"));
                     * }
                     */
                source = new String( source +
                    "// " + src.description + "\n" +
                    "class "+ testname + "{\n" +
                    "    <E extends Comparable> Map<List<E>, E > foo(E e) {\n" +
                    "        class maptest <_As_ _Bs_ _Ds_ E> {\n" +                  // inner class $1maptest
                    "            Map<List<_As_ _Bs_ _Ds_ E>,_As_ _Bs_ _Ds_ E> getMap() { \n" +
                    "                return new HashMap<List<E>,E>();\n" +
                    "            }\n" +
                    "        }\n" +
                    "        return new maptest<E>().getMap();\n" +
                    "   }\n" +
                    "   Map<List<String>,String> shm = foo(new String(\"hello\"));\n" +
                    "}\n\n").concat(sourceBase).replace("_OTHER_", annot2).replace("_As_",As).replace("_Bs_",Bs).replace("_Ds_",Ds) +
                    "\n";
                    hasInnerClass=true;
                    innerClassname="$1maptest";
                break;
        }
        return imports + source;
    }
}
