/*
 * Copyright (c) 2013, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8005085 8005877 8004829 8005681 8006734 8006775 8006507
 * @summary Combinations of Target ElementTypes on (repeated)type annotations.
 * @modules jdk.jdeps/com.sun.tools.classfile
 */

import com.sun.tools.classfile.*;
import java.io.File;
import java.util.List;

public class CombinationsTargetTest2 extends ClassfileTestHelper {

    private static final String JDK_VERSION =
            Integer.toString(Runtime.getRuntime().version().feature());

    // Test count helps identify test case in event of failure.
    int testcount = 0;

    // Base test case template descriptions;true==annotations in code attribute.
    enum srce  {
        src1("(repeating) type annotations on on field in method body",true),
        src2("(repeating) type annotations on type parameters, bounds and  type arguments", true),
        src3("(repeating) type annotations on type parameters of class, method return value in method", true),
        src4("(repeating) type annotations on field in anonymous class", false),
        src5("(repeating) type annotations on field in anonymous class", false),
        src6("(repeating) type annotations on void method declaration", false),
        src7("(repeating) type annotations in use of instanceof", true),
        src7p("(repeating) type annotations in use of instanceof with type test pattern", true),
        src8("(repeating) type annotations in use of instanceof in method", true),
        src8p("(repeating) type annotations in use of instanceof with type test pattern in method", true);

        String description;
        Boolean local;

        srce(String desc, Boolean b) {
            this.description = this + ": " +desc;
            this.local = b;
        }
    }


    String[] ETypes={"TYPE", "FIELD", "METHOD", "PARAMETER", "CONSTRUCTOR",
                     "LOCAL_VARIABLE", "ANNOTATION_TYPE", "PACKAGE"};

    // local class tests will have an inner class.
    Boolean hasInnerClass=false;
    String innerClassname="";

    public static void main(String[] args) throws Exception {
        new CombinationsTargetTest2().run();
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
                       test( 8, 0, 0, 0, As, BDs, ABMix, "CLASS",   et, ++testrun, srce.src1);
                       test( 0, 8, 0, 0, As, BDs, ABMix, "RUNTIME", et, ++testrun, srce.src1);
                       test( 2, 0, 2, 0, As, BDs, ABMix, "CLASS",   et, ++testrun, srce.src5);
                       test( 0, 2, 0, 2, As, BDs, ABMix, "RUNTIME", et, ++testrun, srce.src5);
                       test( 0, 0, 2, 0, As, BDs, ABMix, "CLASS", et, ++testrun, srce.src6);
                       test( 0, 0, 0, 2, As, BDs, ABMix, "RUNTIME", et, ++testrun, srce.src6);
                       test( 2, 0, 0, 0, As, BDs, ABMix, "CLASS", et, ++testrun, srce.src7);
                       test( 0, 2, 0, 0, As, BDs, ABMix, "RUNTIME", et, ++testrun, srce.src7);
                       test( 2, 0, 0, 0, As, BDs, ABMix, "CLASS", et, ++testrun, srce.src7p);
                       test( 0, 2, 0, 0, As, BDs, ABMix, "RUNTIME", et, ++testrun, srce.src7p);
                       test( 4, 0, 0, 0, As, BDs, ABMix, "CLASS", et, ++testrun, srce.src8);
                       test( 0, 4, 0, 0, As, BDs, ABMix, "RUNTIME", et, ++testrun, srce.src8);
                       test( 4, 0, 0, 0, As, BDs, ABMix, "CLASS", et, ++testrun, srce.src8p);
                       test( 0, 4, 0, 0, As, BDs, ABMix, "RUNTIME", et, ++testrun, srce.src8p);
                       break;
                   case "FIELD":
                       test( 8, 0, 0, 0, As, BDs, ABMix, "CLASS",   et, ++testrun, srce.src1);
                       test( 8, 0, 0, 0, As, BDs, ABMix, "CLASS",   et, ++testrun, srce.src2);
                       test( 6, 0, 0, 0, As, BDs, ABMix, "CLASS",   et, ++testrun, srce.src3);
                       test( 2, 0, 2, 0, As, BDs, ABMix, "CLASS",   et, ++testrun, srce.src4);
                       test( 0, 8, 0, 0, As, BDs, ABMix, "RUNTIME", et, ++testrun, srce.src1);
                       test( 0, 8, 0, 0, As, BDs, ABMix, "RUNTIME", et, ++testrun, srce.src2);
                       test( 0, 6, 0, 0, As, BDs, ABMix, "RUNTIME", et, ++testrun, srce.src3);
                       test( 0, 2, 0, 2, As, BDs, ABMix, "RUNTIME", et, ++testrun, srce.src4);
                       break;
                   default:/*TYPE,PARAMETER,LOCAL_VARIABLE,ANNOTATION_TYPE,PACKAGE*/
                       test( 0, 2, 0, 0, As, BDs, ABMix, "RUNTIME", et, ++testrun, srce.src4);
                       test( 0, 2, 0, 0, As, BDs, ABMix, "RUNTIME", et, ++testrun, srce.src5);
                       break;
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
        extraOptions = List.of();
        File testFile = null;
        String tname="Test" + N.toString();
        hasInnerClass=false;
        String testDef = "Test " + testcount + " parameters: tinv=" + tinv +
                ", tvis=" + tvis + ", inv=" + inv + ", vis=" + vis +
                ", Arepeats=" + Arepeats + ", BDrepeats=" + BDrepeats +
                ", ABmix=" + ABmix + ", retention: " + rtn + ", anno2: " +
                et2 + ", src=" + source + "\n    " + source.description;

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
            classFile=new File(sb.insert(sb.lastIndexOf(".class"),innerClassname).toString());
            println("classfile: " + classFile.getAbsolutePath());
        }
        ClassFile cf = ClassFile.read(classFile);

        //Test class,fields and method counts.
        test(cf);

        for (Field f : cf.fields) {
            if(source.local)
                test(cf, f, true);
            else
                test(cf,f);
        }
        for (Method m: cf.methods) {
            if(source.local)
                test(cf, m, true);
            else
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

    // Source for test cases
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
            "@Target({TYPE_USE,TYPE_PARAMETER,_OTHER_})\n" +
            "@Repeatable(DC.class)\n" +
            "@interface D { }\n\n" +

            "@Retention("+retentn+")\n" +
            "@Target({TYPE_USE,TYPE_PARAMETER,_OTHER_})\n" +
            "@interface DC { D[] value(); }\n\n");

        // Test case sources with sample generated source
        switch(src) {
            case src1: // (repeating) type annotations on field in method body
                    /*
                     * class Test1 {
                     * Test1(){}
                     * // type usage in method body
                     * String test(Test1 this, String param, String ... vararg) {
                     *     @A @B
                     *     Object o = new @A @B  String @A @B  [3];
                     *         return (@A @B  String) null;
                     * }}
                      */
                source = new String(
                    "// " + src.description + "\n" +
                    "class " + testname + " {\n" +
                    "" + testname +"(){} \n" +
                    "// type usage in method body \n" +
                    "String test("+testname+" this, " +
                       "String param, String ... vararg) { \n" +
                    "    _As_ _Bs_\n    Object o = new _As_ _Bs_  String _As_ _Bs_  [3]; \n" +
                    "        return (_As_ _Bs_  String) null; \n" +
                    "} \n" +
                    "} \n").concat(sourceBase).replace("_OTHER_", annot2).replace("_As_",As).replace("_Bs_",Bs) +
                    "\n\n";
                    break;
            case src2: // (repeating) annotations on type parameters, bounds and  type arguments in new statement.
                    /*
                     * class Test2<T extends Object> {
                     *     Map<List<String>, Integer> map =
                     *         new HashMap<@A @B List<@A @B String>, @A @B Integer>();
                     *     Map<List<String>, Integer> map2 = new @A @B HashMap<>();
                     *     String test(Test2<T> this) { return null;}
                     *     <T> String genericMethod(T t) { return null; }
                     * }
                     */
                source = new String( source +
                    "// " + src.description + "\n" +
                    "class " + testname + "<T extends Object> {\n" +
                    "    Map<List<String>, Integer> map =\n" +
                    "        new HashMap<_As_ _Bs_ List<_As_ _Bs_ String>, _As_ _Bs_ Integer>();\n" +
                    "    Map<List<String>, Integer> map2 = new _As_ _Bs_ HashMap<>();\n" +
                    "    String test(" + testname + "<T> this) { return null;}\n" +
                    "    <T> String genericMethod(T t) { return null; }\n" +
                    "}\n").concat(sourceBase).replace("_OTHER_", annot2).replace("_As_",As).replace("_Bs_",Bs) +
                    "\n\n";
                break;
            case src3: // (repeating)annotations on type parameters of class, method return value in method.
                    /*
                     * class Test3{
                     *     <E extends Comparable> Map<List<E>, E > foo(E e) {
                     *         class maptest <E> {
                     *             Map<List<E>,E> getMap() {
                     *                 Map<List<E>,E> Em = new HashMap<List<@A @B @D E>,@A @B @D E>();
                     *                 return Em;
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
                    "        class maptest <E> {\n" +                  // inner class $1maptest
                    "            Map<List<E>,E> getMap() { \n" +
                    "                Map<List<E>,E> Em = new HashMap<List<_As_ _Bs_ _Ds_ E>,_As_ _Bs_ _Ds_ E>();\n" +
                    "                return Em;\n" +
                    "            }\n" +
                    "        }\n" +
                    "        return new maptest<E>().getMap();\n" +
                    "   }\n" +
                    "   Map<List<String>,String> shm = foo(new String(\"hello\"));\n" +
                    "}\n").concat(sourceBase).replace("_OTHER_", annot2).replace("_As_",As).replace("_Bs_",Bs).replace("_Ds_",Ds) +
                    "\n\n";
                    hasInnerClass=true;
                    innerClassname="$1maptest";
                break;
            case src4: // (repeating)annotations on field in anonymous class
                    /*
                     * class Test95{
                     *     void mtest( Test95 t){  }
                     *     public void test() {
                     *         mtest( new Test95() {
                     *             @A @A @B @B String data2 = "test";
                     *         });
                     *     }
                     * }
                     */
                source = new String( source +
                    "// " + src.description + "\n" +
                    "class "+ testname + "{\n" +
                    "    void mtest( "+ testname + " t){  }\n" +
                    "    public void test() {\n" +
                    "        mtest( new "+ testname + "() {\n" +
                    "            _As_ _Bs_ String data2 = \"test\";\n" +
                    "        });\n" +
                    "    }\n" +
                    "}\n").concat(sourceBase).replace("_OTHER_", annot2).replace("_As_",As).replace("_Bs_",Bs) +
                    "\n\n";
                    hasInnerClass=true;
                    innerClassname="$1";
                break;
            case src5: // (repeating)annotations on method in anonymous class
                    /*
                     * class Test120{
                     *     void mtest( Test120 t){  }
                     *     public void test() {
                     *         mtest( new Test120() {
                     *             @A @B @A @B String m2(){return null;};
                     *         });
                     *     }
                     */
                source = new String( source +
                    "// " + src.description + "\n" +
                    "class "+ testname + "{\n" +
                    "    void mtest( "+ testname + " t){  }\n" +
                    "    public void test() {\n" +
                    "        mtest( new "+ testname + "() {\n" +
                    "            _As_ _Bs_ String m2(){return null;};\n" +
                    "        });\n" +
                    "    }\n" +
                    "}\n").concat(sourceBase).replace("_OTHER_", annot2).replace("_As_",As).replace("_Bs_",Bs) +
                    "\n\n";
                    hasInnerClass=true;
                    innerClassname="$1";
                break;
            case src6: // (repeating)annotations on void method declaration
                    /*
                     * class Test95{
                     *     @A @A @B @B public void test() { };
                     * }
                     */
                source = new String( source +
                    "// " + src.description + "\n" +
                    "class "+ testname + "{\n" +
                    "    _As_ _Bs_ public void test() { }\n" +
                    "}\n").concat(sourceBase).replace("_OTHER_", annot2).replace("_As_",As).replace("_Bs_",Bs) +
                    "\n\n";
                    hasInnerClass=false;
                break;
            case src7: // (repeating) type annotations in use of instanceof
                    /*
                     *   class Test10{
                     *       String data = "test";
                     *       boolean dataIsString = ( data instanceof @A @B @A @B String);
                     *   }
                     */
                source = new String( source +
                    "// " + src.description + "\n" +
                    "class "+ testname + "{\n" +
                    "    String data = \"test\";\n" +
                    "    boolean dataIsString = ( data instanceof _As_ _Bs_ String);\n" +
                    "}\n").concat(sourceBase).replace("_OTHER_", annot2).replace("_As_",As).replace("_Bs_",Bs) +
                    "\n\n";
                    hasInnerClass=false;
                break;
            case src7p: // (repeating) type annotations in use of instanceof with type test pattern
                    /*
                     *   class Test10{
                     *       Object data = "test";
                     *       boolean dataIsString = ( data instanceof @A @B @A @B String str);
                     *   }
                     */
                source = new String( source +
                    "// " + src.description + "\n" +
                    "class "+ testname + "{\n" +
                    "    Object data = \"test\";\n" +
                    "    boolean dataIsString = ( data instanceof _As_ _Bs_ String str && str.isEmpty());\n" +
                    "}\n").concat(sourceBase).replace("_OTHER_", annot2).replace("_As_",As).replace("_Bs_",Bs) +
                    "\n\n";
                hasInnerClass=false;
                break;
            case src8: // (repeating) type annotations in use of instanceof
                    /*
                     *   class Test20{
                     *       String data = "test";
                     *       Boolean isString() {
                     *           if( data instanceof @A @B @A @B String )
                     *               return true;
                     *           else
                     *               return( data instanceof @A @B @A @B String );
                     *       }
                     *   }
                     */
                source = new String( source +
                    "// " + src.description + "\n" +
                    "class "+ testname + "{\n" +
                    "    String data = \"test\";\n" +
                    "    Boolean isString() { \n" +
                    "        if( data instanceof _As_ _Bs_ String )\n" +
                    "            return true;\n" +
                    "        else\n" +
                    "            return( data instanceof _As_ _Bs_ String );\n" +
                    "    }\n" +
                    "}\n").concat(sourceBase).replace("_OTHER_", annot2).replace("_As_",As).replace("_Bs_",Bs) +
                    "\n\n";
                    hasInnerClass=false;
                break;
            case src8p: // (repeating) type annotations in use of instanceof with type test pattern
                   /*
                     *   class Test20{
                     *       String data = "test";
                     *       Boolean isString() {
                     *           if( data instanceof @A @B @A @B String )
                     *               return true;
                     *           else
                     *               return( data instanceof @A @B @A @B String );
                     *       }
                     *   }
                     */
                source = new String( source +
                    "// " + src.description + "\n" +
                    "class "+ testname + "{\n" +
                    "    Object data = \"test\";\n" +
                    "    Boolean isString() { \n" +
                    "        if( data instanceof _As_ _Bs_ String str)\n" +
                    "            return true;\n" +
                    "        else\n" +
                    "            return( data instanceof _As_ _Bs_ String str && str.isEmpty());\n" +
                    "    }\n" +
                    "}\n").concat(sourceBase).replace("_OTHER_", annot2).replace("_As_",As).replace("_Bs_",Bs) +
                    "\n\n";
                hasInnerClass=false;
                break;

        }
        return imports + source;
    }
}
