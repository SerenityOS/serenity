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
 * @bug 8005085 8005681 8008769 8010015
 * @summary Check (repeating)type annotations on lambda usage.
 * @modules jdk.jdeps/com.sun.tools.classfile
 * @run main CombinationsTargetTest3
 */

import com.sun.tools.classfile.*;
import java.io.File;
import java.util.Vector;

public class CombinationsTargetTest3 extends ClassfileTestHelper {

    // Helps identify test case in event of failure.
    int testcount = 0;

    // Known failure cases due to open bugs.
    Vector<String> skippedTests = new Vector<>();
    void printSkips() {
        if(!skippedTests.isEmpty()) {
            println(skippedTests.size() + " tests were skipped:");
            for(String t : skippedTests)
                println("    " + t);
        }
    }

    // Test case descriptions and expected annotation counts.
    enum srce  {
        src1("type annotations on lambda expression as method arg.",4,0),
        src2("type annotations on new in single line lambda expression",2,0),
        src3("type annotations in lambda expression code block",4,0),
        src4("type annotations in code block with recursion,cast",2,0),
        src5("type annotations in lambda expression code block",4,0),
        src6("type annotations on type parm in method reference",4,0),
        src7("type annotations on inner class field of lambda expression",2,2),
        src8("type annotations in inner class of lambda expression",4,2),
        src9("type annotations on static method of interface",4,2);

        String description;
        // Expected annotation counts are same for Vis or Invis, but which one
        // depends on retention type.
        Integer[] exp = { 0, 0 };

        // If class to test is inner class, this may be set in SourceString()
        String innerClassname = null ;

        // If class to test is not main or inner class; set in sourceString()
        String altClassName = null;

        srce(String desc, int e1, int e2) {
            description = this + ": " +desc;
            exp[0]=e1;
            exp[1]=e2;
        }
    }

    // Check for RuntimeInvisible or RuntimeVisible annotations.
    String[] RType={"CLASS", "RUNTIME"};

    // This can be a compile only test.
    static boolean compileonly=false;

    // Collect failure for end of test report()
    Vector<String> vFailures = new Vector<>();

    // pass/fail determined after all tests have run.
    void report() {
        if(vFailures.isEmpty()) {
            printSkips();
            println("PASS");
        } else {
           System.err.println("FAILED: There were failures:");
           for(String f : vFailures)
               System.err.println(f);
           throw new RuntimeException("There were failures. See test log.");
        }
    }

    public static void main(String[] args) throws Exception {
        if(args.length>0 && args[0].compareTo("compileonly")==0)
            compileonly=true;
        new CombinationsTargetTest3().run();
    }

    void run() throws Exception {
        // Determines which repeat and order in source(ABMix).
        Boolean As= false, BDs=true, ABMix=false;
        int testrun=0;
        // A repeats and/or B/D repeats, ABMix for order of As and Bs.
        Boolean [][] bRepeat = new Boolean[][]{{false,false,false}, //no repeats
                                               {true,false,false}, //repeat @A
                                               {false,true,false}, //repeat @B
                                               {true,true,false},  //repeat both
                                               {false,false,true}  //repeat mix
        };
        // Added ElementType's. All set; not permuted (so far) for this test
        String et = "TYPE,FIELD,METHOD,PARAMETER,CONSTRUCTOR,LOCAL_VARIABLE";

        // test loop
        for(Boolean[] bCombo : bRepeat) {
            As=bCombo[0]; BDs=bCombo[1]; ABMix=bCombo[2];
            for(srce src : srce.values())
                for( String rtype : RType ) {
                   switch( rtype ) {
                       case "RUNTIME":
                           test(0,src.exp[0],0,src.exp[1],As, BDs, ABMix,
                                "RUNTIME", et, ++testrun, src);
                           break;
                       case "CLASS":
                           test(src.exp[0],0,src.exp[1],0,As, BDs, ABMix,
                                "CLASS", et, ++testrun, src);
                           break;
                }
            }
        }
        report();
    }

    // Filter out skipped cases, compile, pass class file to test method,
    // count annotations and asses results.
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
        String testDef = "Test " + testcount + " parameters: tinv=" + tinv +
                ", tvis=" + tvis + ", inv=" + inv + ", vis=" + vis +
                ", Arepeats=" + Arepeats + ", BDrepeats=" + BDrepeats +
                ", ABmix=" + ABmix + ", retention: " + rtn + ", anno2: " +
                et2 + ", src=" + source;

        // Skip failing cases with bug ID's
        if ((source.equals(srce.src2) || source.equals(srce.src4) ||
            source.equals(srce.src5)) &&
            (ABmix || (Arepeats && BDrepeats))) {
                skippedTests.add(testDef +
                  "\n--8005681 repeated type-annotations on new/cast/array in" +
                  " inner class in lambda expression.");
            return;
        }//8008769 Repeated type-annotations on type parm of local variable
         else if (source.equals(srce.src6) &&
                   (ABmix || (Arepeats && BDrepeats))) {
            skippedTests.add(testDef +  "\n--8008769 Repeated " +
                             "type-annotations on type parm of local variable");
            return;
        }

        println(testDef);
        // Create test source and File.
        String sourceString = sourceString(tname, rtn, et2, Arepeats,
                                           BDrepeats, ABmix, source);
        testFile = writeTestFile(tname+".java", sourceString);
        // Compile test source and read classfile.
        File classFile = null;
        try {
            classFile = compile(testFile);
            System.out.println("pass compile: " + tname + ".java");
        } catch (Error err) {
            System.err.println("fail compile. Source:\n" + sourceString);
            throw err;
        }
        if(!compileonly) {
            //check if innerClassname is set
            String classdir = classFile.getAbsolutePath();
            if(source.innerClassname != null) {
                StringBuffer sb = new StringBuffer(classdir);
                classFile=new File(sb.insert(sb.lastIndexOf(".class"),
                                   source.innerClassname).toString());
                source.innerClassname=null;
            } else if (source.altClassName != null) {
                classdir = classdir.substring(0,classdir.lastIndexOf("Test"));
                classFile=new File(classdir.concat(source.altClassName));
                source.innerClassname=null;
            }
            ClassFile cf = ClassFile.read(classFile);

            println("Testing classfile: " + cf.getName());
            //Test class,fields and method counts.
            test(cf);

            for (Field f : cf.fields) {
                test(cf, f);
                test(cf, f, true);
            }
            for (Method m: cf.methods) {
                test(cf, m);
                test(cf, m, true);
            }

            countAnnotations(); // sets errors=0 before counting.
            if (errors > 0) {
                System.err.println( testDef );
                System.err.println( "Source:\n" + sourceString );
                vFailures.add(testDef);
            }
        }
        if(errors==0) println("Pass"); println("");
    }

    /*
     * Source definitions for test cases.
     * To add a test:
     *   Add enum to srce(near top of file) with expected annotation counts.
     *   Add source defintion below.
     */
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
            "import java.util.ArrayList;\n\n");

            String sourceBase = new String(
            "@Retention("+retentn+") @Target({TYPE_USE,_OTHER_}) @Repeatable( AC.class ) @interface A { }\n" +
            "@Retention("+retentn+") @Target({TYPE_USE,_OTHER_}) @interface AC { A[] value(); } \n" +
            "@Retention("+retentn+") @Target({TYPE_USE,_OTHER_}) @Repeatable( BC.class ) @interface B { }\n" +
            "@Retention("+retentn+") @Target({TYPE_USE,_OTHER_}) @interface BC { B[] value(); } \n" +
            "@Retention("+retentn+") @Target({TYPE_USE,TYPE_PARAMETER,_OTHER_}) @Repeatable(DC.class) @interface D { }\n" +
            "@Retention("+retentn+") @Target({TYPE_USE,TYPE_PARAMETER,_OTHER_}) @interface DC { D[] value(); }");

        // Test case sources with sample generated source
        switch(src) {
            case src1: //(repeating) type annotations on lambda expressions.
                /*
                 * class Test1 {
                 * Test1(){}
                 * interface MapFun<T,R> {  R m( T n); }
                 * void meth( MapFun<String,Integer> mf ) {
                 *     assert( mf.m("four") == 4);
                 * }
                 * void test(Integer i) {
                 *     // lambda expression as method arg
                 *     meth( (@A @B String s) -> { @A @B Integer len = s.length(); return len; } );
                 * }}
                 */
                source = new String( source +
                "// " + src.description + "\n" +
                "class " + testname + " {\n" +
                "  " + testname +"(){} \n" +
                "  interface MapFun<T,R> {  R m( T n); }\n\n" +
                "  void meth( MapFun<String,Integer> mf ) {\n" +
                "    assert( mf.m(\"four\") == 4);\n" +
                "  }\n\n" +
                "  void test(Integer i) {\n" +
                "    // lambda expression as method arg\n" +
                "    meth( (_As_ _Bs_ String s) -> { _As_ _Bs_ Integer len = s.length(); return len; } );\n" +
                "}}\n\n").concat(sourceBase).replace("_OTHER_", annot2).replace("_As_",As).replace("_Bs_",Bs) +
                "\n";
                break;
            case src2: //(repeating) type annotations on new in single line lambda expression.
                /*
                 * //case2: (repeating) type annotations on new in single lambda expressions.
                 * class Test2{
                 *   interface MapFun<T, R> {  R m( T n); }
                 *   MapFun<Integer, String> its;
                 * void test(Integer i) {
                 *   its = a -> "~"+new @A @B Integer(a).toString()+"~";
                 *   System.out.println("in: " + i + " out: " + its.m(i));
                 * }}
                 */
                source = new String( source +
                "// " + src.description + "\n" +
                "class " + testname + "{\n" +
                "  interface MapFun<T, R> {  R m( T n); }\n" +
                "  MapFun<Integer, String> its;\n" +
                "  void test(Integer i) {\n" +
                "    its = a -> \"~\"+new _As_ _Bs_ Integer(a).toString()+\"~\";\n" +
                "    System.out.println(\"in: \" + i + \" out: \" + its.m(i));\n" +
                "  }\n" +
                "}\n\n").concat(sourceBase).replace("_OTHER_", annot2).replace("_As_",As).replace("_Bs_",Bs) +
                "\n";
            break;
            case src3: //(repeating) type annotations in lambda expression code block.
                /*
                 * class Test183{
                 *   interface MapFun<T, R> {  R m( T n); }
                 *   MapFun<List<Integer>, String> iLs;
                 *   void testm(Integer i) {
                 *       iLs = l -> { @A @B @A @B String ret = new String();
                 *                    for( @A @B @A @B Integer i2 : l)
                 *                        ret=ret.concat(i2.toString() + " ");
                 *                    return ret; };
                 *   List<Integer> li = new ArrayList<>();
                 *   for(int j=0; j<i; j++) li.add(j);
                 *   System.out.println(iLs.m(li) );
                 * }}
                 */
                source = new String( source +
                "// " + src.description + "\n" +
                "class "+ testname + "{\n" +
                "  interface MapFun<T, R> {  R m( T n); }\n" +
                "  MapFun<List<Integer>, String> iLs;\n" +
                "  void testm(Integer i) {\n" +
                "    iLs = l -> { _As_ _Bs_ String ret = new String();\n" +
                "                 for( _As_ _Bs_ Integer i2 : l)\n" +
                "                   ret=ret.concat(i2.toString() + \" \");\n" +
                "                 return ret; };\n" +
                "  List<Integer> li = new ArrayList<>();\n" +
                "  for(int j=0; j<i; j++) li.add(j);\n" +
                "  System.out.println(iLs.m(li) );\n" +
                "}\n" +
                "\n" +
                "    public static void main(String... args) {new " + testname + "().testm(5); }\n" +
                "}\n\n").concat(sourceBase).replace("_OTHER_", annot2).replace("_As_",As).replace("_Bs_",Bs) +
                "\n";
            break;
            case src4: //(repeating) type annotations in code block with recursion,cast
                /*
                 * class Test194{
                 *   interface MapFun<T, R> {  R m( T n); }
                 *   MapFun<Integer, Double>  nf;
                 *   void testm(Integer i) {
                 *       nf = j -> { return j == 1 ? 1.0 : (@A @B @A @B  Double)(nf.m(j-1) * j); };
                 *       System.out.println( "nf.m(" + i + "): " + nf.m(i));
                 *   }
                 * }
                 */
                source = new String( source +
                "// " + src.description + "\n" +
                "class "+ testname + "{\n" +
                "  interface MapFun<T, R> {  R m( T n); }\n" +
                "  MapFun<Integer, Double>  nf;\n" +
                "  void testm(Integer i) {\n" +
                "    nf = j -> { return j == 1 ? 1.0 : (_As_ _Bs_  Double)(nf.m(j-1) * j); };\n" +
                "    System.out.println( \"nf.m(\" + i + \"): \" + nf.m(i));\n" +
                "  }\n" +
                "  public static void main(String... args) {new " + testname + "().testm(5); }\n" +
                "}\n\n").concat(sourceBase).replace("_OTHER_", annot2).replace("_As_",As).replace("_Bs_",Bs) +
                "\n";
            break;
            case src5: //(repeating) type annotations in lambda expression code block.
                   /*
                    * class Test180 {
                    *   interface MapFun<T, R> {  R m( T n); }
                    *   MapFun<Integer,List<Integer>> iLi;
                    *   void test(Integer i) {
                    *     // type parameter use.
                    *     iLi = n -> { List<@A @B @A @B Integer> LI = new ArrayList<@A @B @A @B Integer>(n);
                    *                  for(int nn = n; nn >=0; nn--) LI.add(nn);
                    *                  return LI; };
                    *     List<Integer> li = iLi.m(i);
                    *     for(Integer k : li) System.out.print(k);
                    *   }
                    * }
                    */
                source = new String( source +
                "// " + src.description + "\n" +
                "class "+ testname + "{\n" +
                "  interface MapFun<T, R> {  R m( T n); }\n" +
                "  MapFun<Integer,List<Integer>> iLi;\n" +
                "  void test(Integer i) {\n" +
                "    // type parameter use.\n" +
                "    iLi = n -> { List<_As_ _Bs_ Integer> LI = new ArrayList<_As_ _Bs_ Integer>(n);\n" +
                "                 for(int nn = n; nn >=0; nn--) LI.add(nn);\n" +
                "                 return LI; };\n" +
                "    List<Integer> li = iLi.m(i);\n" +
                "    for(Integer k : li) System.out.print(k);\n" +
                "}\n" +
                "  public static void main(String... args) {new " + testname + "().test(5); }\n" +
                "}\n\n").concat(sourceBase).replace("_OTHER_", annot2).replace("_As_",As).replace("_Bs_",Bs).replace("_Ds_",Ds) +
                "\n";
            break;
            case src6: //(repeating) type annotations on type parm in method reference.
                /*
                 * class Test240{
                 *   interface PrintString { void print(String s); }
                 *   public void printArray(Object[] oa, PrintString ps) {
                 *       for(Object o : oa ) ps.print(o.toString());
                 *   }
                 *   public void test() {
                 *       Integer[] intarray = {1,2,3,4,5};
                 *       printArray(intarray, @A @B @A @B TPrint::<@A @B @A @B String>print);
                 *   }
                 * }
                 * class TPrint {
                 *    public static <T> void print(T t) { System.out.println( t.toString()); }
                 * }
                 */
                source = new String( source +
                "// " + src.description + "\n" +
                "class "+ testname + "{\n" +
                "  interface PrintString { void print(String s); }\n" +
                "  public void printArray(Object[] oa, PrintString ps) {\n" +
                "      for(Object o : oa ) ps.print(o.toString());\n" +
                "  }\n" +
                "  public void test() {\n" +
                "    Integer[] intarray = {1,2,3,4,5};\n" +
                "    printArray(intarray, _As_ _Bs_ TPrint::<_As_ _Bs_ String>print);\n" +
                "  }\n" +
                "  public static void main(String... args) {new " + testname + "().test(); }\n" +
                "}\n\n" +
                "class TPrint {\n" +
                "  public static <T> void print(T t) { System.out.println( t.toString()); }\n" +
                "}\n\n").concat(sourceBase).replace("_OTHER_", annot2).replace("_As_",As).replace("_Bs_",Bs) +
                "\n";
            break;
            case src7: //(repeating)type annotations in inner class of lambda expression.
                /*
                 * class Test2{
                 *   interface MapFun<T, R> {  R m( T n); }
                 *   MapFun<Class<?>,String> cs;
                 *   void test() {
                 *     cs = c -> {
                 *         class innerClass   {
                 *           @A @B Class<?> icc = null;
                 *           String getString() { return icc.toString(); }
                 *         }
                 *         return new innerClass().getString();
                 *     };
                 *     System.out.println("cs.m : " + cs.m(Integer.class));
                 *   }
                 * }
                 */
                source = new String( source +
                "// " + src.description + "\n" +
                "class "+ testname + "{\n" +
                "  interface MapFun<T, R> {  R m( T n); }\n" +
                "  MapFun<Class<?>,String> cs;\n" +
                "  void test() {\n" +
                "    cs = c -> {\n" +
                "        class innerClass   {\n" +
                "          _As_ _Bs_ Class<?> icc = null;\n" +
                "          innerClass(Class<?> _c) { icc = _c; }\n" +
                "          String getString() { return icc.toString(); }\n" +
                "        }\n" +
                "        return new innerClass(c).getString();\n" +
                "    };\n" +
                "    System.out.println(\"cs.m : \" + cs.m(Integer.class));\n" +
                "  }\n" +
                "\n" +
                "    public static void main(String... args) {new " + testname + "().test(); }\n" +
                "}\n\n").concat(sourceBase).replace("_OTHER_", annot2).replace("_As_",As).replace("_Bs_",Bs) +
                "\n";
                src.innerClassname="$1innerClass";
            break;
            case src8: //(repeating)type annotations in inner class of lambda expression.
                /*
                 * class Test2{
                 *   interface MapFun<T, R> {  R m( T n); }
                 *   MapFun<Class<?>,String> cs;
                 *   void test() {
                 *     cs = c -> {
                 *         class innerClass   {
                 *             Class<?> icc;
                 *             innerClass(@A @B Class<?> _c) { icc = _c; }
                 *             @A @B String getString() { return icc.toString(); }
                 *         }
                 *         return new innerClass(c).getString();
                 *     };
                 *     System.out.println("cs.m : " + cs.m(Integer.class));
                 *   }
                 * }
                 */
                source = new String( source +
                "// " + src.description + "\n" +
                "class "+ testname + "{\n" +
                "  interface MapFun<T, R> {  R m( T n); }\n" +
                "  MapFun<Class<?>,String> cs;\n" +
                "  void test() {\n" +
                "    cs = c -> {\n" +
                "        class innerClass {\n" +
                "            Class<?> icc;\n" +
                "            innerClass(_As_ _Bs_ Class<?> _c) { icc = _c; }\n" +
                "            _As_ _Bs_ String getString() { return icc.toString(); }\n" +
                "        }\n" +
                "        return new innerClass(c).getString();\n" +
                "    };\n" +
                "    System.out.println(\"cs.m : \" + cs.m(Integer.class));\n" +
                "  }\n" +
                "\n" +
                "    public static void main(String... args) {new " + testname + "().test(); }\n" +
                "}\n\n").concat(sourceBase).replace("_OTHER_", annot2).replace("_As_",As).replace("_Bs_",Bs) +
                "\n";
                src.innerClassname="$1innerClass";
            break;
            case src9: //(repeating)type annotations on static method of interface
                /*
                 *  class Test90{
                 *    interface I  {
                 *      static @A @B @A @B String m() { @A @B @A @B String ret = "I.m"; return ret; }
                 *    }
                 *  }
                 */
                source = new String( source +
                "// " + src.description + "\n" +
                "class "+ testname + "{\n" +
                "  interface I  { \n" +
                "    static _As_ _Bs_ String m() { _As_ _Bs_ String ret = \"I.m\"; return ret; }\n" +
                "  }\n" +
                "}\n\n").concat(sourceBase).replace("_OTHER_", annot2).replace("_As_",As).replace("_Bs_",Bs) +
                "\n";
                src.innerClassname="$I";
            break;
        }
        return imports + source;
    }
}
