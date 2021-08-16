/*
 * Copyright (c) 2009, 2016, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 * @summary example code used in javadoc for java.lang.invoke API
 * @compile JavaDocExamplesTest.java
 * @run testng/othervm test.java.lang.invoke.JavaDocExamplesTest
 */

package test.java.lang.invoke;

import java.io.StringWriter;
import java.lang.invoke.*;
import static java.lang.invoke.MethodHandles.*;
import static java.lang.invoke.MethodType.*;

import java.util.*;

import org.testng.*;
import org.testng.annotations.*;

/**
 * @author jrose
 */
public class JavaDocExamplesTest {
    /** Wrapper for running the TestNG tests in this module.
     *  Put TestNG on the classpath!
     */
    public static void main(String... ignore) throws Throwable {
        new JavaDocExamplesTest().run();
    }

    public void run() throws Throwable {
        testMisc();
        testFindStatic();
        testFindConstructor();
        testFindVirtual();
        testFindSpecial();
        testPermuteArguments();
        testZero();
        testDropArguments();
        testDropArgumentsToMatch();
        testFilterArguments();
        testFoldArguments();
        testFoldArguments2();
        testMethodHandlesSummary();
        testAsSpreader();
        testAsCollector();
        testAsVarargsCollector();
        testAsFixedArity();
        testAsTypeCornerCases();
        testMutableCallSite();
    }
    // How much output?
    static final Class<?> THIS_CLASS = JavaDocExamplesTest.class;
    static int verbosity = Integer.getInteger(THIS_CLASS.getSimpleName()+".verbosity", 0);


{}
private static final Lookup LOOKUP = lookup();
// static final private MethodHandle CONCAT_1 = LOOKUP.findVirtual(String.class,
//     "concat", methodType(String.class, String.class));
// static final private MethodHandle HASHCODE_1 = LOOKUP.findVirtual(Object.class,
//     "hashCode", methodType(int.class));

// form required if ReflectiveOperationException is intercepted:
    private static final MethodHandle CONCAT_2, HASHCODE_2, ADD_2, SUB_2;
static {
  try {
    Class<?> THIS_CLASS = LOOKUP.lookupClass();
    CONCAT_2 = LOOKUP.findVirtual(String.class,
      "concat", methodType(String.class, String.class));
    HASHCODE_2 = LOOKUP.findVirtual(Object.class,
      "hashCode", methodType(int.class));
    ADD_2 = LOOKUP.findStatic(THIS_CLASS, "add", methodType(int.class, int.class, int.class));
    SUB_2 = LOOKUP.findStatic(THIS_CLASS, "sub", methodType(int.class, int.class, int.class));
   } catch (ReflectiveOperationException ex) {
     throw new RuntimeException(ex);
   }
}
    static int add(int x, int y) { return x + y; }
    static int sub(int x, int y) { return x - y; }

{}

    @Test public void testMisc() throws Throwable {
// Extra tests, not from javadoc:
{}
MethodHandle CONCAT_3 = LOOKUP.findVirtual(String.class,
  "concat", methodType(String.class, String.class));
MethodHandle HASHCODE_3 = LOOKUP.findVirtual(Object.class,
  "hashCode", methodType(int.class));
//assertEquals("xy", (String) CONCAT_1.invokeExact("x", "y"));
assertEquals("xy", (String) CONCAT_2.invokeExact("x", "y"));
assertEquals("xy", (String) CONCAT_3.invokeExact("x", "y"));
//assertEquals("xy".hashCode(), (int) HASHCODE_1.invokeExact((Object)"xy"));
assertEquals("xy".hashCode(), (int) HASHCODE_2.invokeExact((Object)"xy"));
assertEquals("xy".hashCode(), (int) HASHCODE_3.invokeExact((Object)"xy"));
{}
    }

    @Test public void testFindStatic() throws Throwable {
{}
MethodHandle MH_asList = publicLookup().findStatic(Arrays.class,
  "asList", methodType(List.class, Object[].class));
assertEquals("[x, y]", MH_asList.invoke("x", "y").toString());
{}
    }

    @Test public void testFindVirtual() throws Throwable {
{}
MethodHandle MH_concat = publicLookup().findVirtual(String.class,
  "concat", methodType(String.class, String.class));
MethodHandle MH_hashCode = publicLookup().findVirtual(Object.class,
  "hashCode", methodType(int.class));
MethodHandle MH_hashCode_String = publicLookup().findVirtual(String.class,
  "hashCode", methodType(int.class));
assertEquals("xy", (String) MH_concat.invokeExact("x", "y"));
assertEquals("xy".hashCode(), (int) MH_hashCode.invokeExact((Object)"xy"));
assertEquals("xy".hashCode(), (int) MH_hashCode_String.invokeExact("xy"));
// interface method:
MethodHandle MH_subSequence = publicLookup().findVirtual(CharSequence.class,
  "subSequence", methodType(CharSequence.class, int.class, int.class));
assertEquals("def", MH_subSequence.invoke("abcdefghi", 3, 6).toString());
// constructor "internal method" must be accessed differently:
MethodType MT_newString = methodType(void.class); //()V for new String()
try { assertEquals("impossible", lookup()
        .findVirtual(String.class, "<init>", MT_newString));
 } catch (NoSuchMethodException ex) { } // OK
MethodHandle MH_newString = publicLookup()
  .findConstructor(String.class, MT_newString);
assertEquals("", (String) MH_newString.invokeExact());
{}
    }

    @Test public void testFindConstructor() throws Throwable {
{}
MethodHandle MH_newArrayList = publicLookup().findConstructor(
  ArrayList.class, methodType(void.class, Collection.class));
Collection orig = Arrays.asList("x", "y");
Collection copy = (ArrayList) MH_newArrayList.invokeExact(orig);
assert(orig != copy);
assertEquals(orig, copy);
// a variable-arity constructor:
MethodHandle MH_newProcessBuilder = publicLookup().findConstructor(
  ProcessBuilder.class, methodType(void.class, String[].class));
ProcessBuilder pb = (ProcessBuilder)
  MH_newProcessBuilder.invoke("x", "y", "z");
assertEquals("[x, y, z]", pb.command().toString());
{}
    }

// for testFindSpecial
{}
static class Listie extends ArrayList {
  public String toString() { return "[wee Listie]"; }
  static Lookup lookup() { return MethodHandles.lookup(); }
}
{}

    @Test public void testFindSpecial() throws Throwable {
{}
// no access to constructor via invokeSpecial:
MethodHandle MH_newListie = Listie.lookup()
  .findConstructor(Listie.class, methodType(void.class));
Listie l = (Listie) MH_newListie.invokeExact();
try { assertEquals("impossible", Listie.lookup().findSpecial(
        Listie.class, "<init>", methodType(void.class), Listie.class));
 } catch (NoSuchMethodException ex) { } // OK
// access to super and self methods via invokeSpecial:
MethodHandle MH_super = Listie.lookup().findSpecial(
  ArrayList.class, "toString" , methodType(String.class), Listie.class);
MethodHandle MH_this = Listie.lookup().findSpecial(
  Listie.class, "toString" , methodType(String.class), Listie.class);
MethodHandle MH_duper = Listie.lookup().findSpecial(
  Object.class, "toString" , methodType(String.class), Listie.class);
assertEquals("[]", (String) MH_super.invokeExact(l));
assertEquals(""+l, (String) MH_this.invokeExact(l));
assertEquals("[]", (String) MH_duper.invokeExact(l)); // ArrayList method
try { assertEquals("inaccessible", Listie.lookup().findSpecial(
        String.class, "toString", methodType(String.class), Listie.class));
 } catch (IllegalAccessException ex) { } // OK
Listie subl = new Listie() { public String toString() { return "[subclass]"; } };
assertEquals(""+l, (String) MH_this.invokeExact(subl)); // Listie method
{}
    }

    @Test public void testPermuteArguments() throws Throwable {
        {{
{} /// JAVADOC
MethodType intfn1 = methodType(int.class, int.class);
MethodType intfn2 = methodType(int.class, int.class, int.class);
MethodHandle sub = SUB_2;// ... {int x, int y => x-y} ...;
assert(sub.type().equals(intfn2));
MethodHandle sub1 = permuteArguments(sub, intfn2, 0, 1);
MethodHandle rsub = permuteArguments(sub, intfn2, 1, 0);
assert((int)rsub.invokeExact(1, 100) == 99);
MethodHandle add = ADD_2;// ... {int x, int y => x+y} ...;
assert(add.type().equals(intfn2));
MethodHandle twice = permuteArguments(add, intfn1, 0, 0);
assert(twice.type().equals(intfn1));
assert((int)twice.invokeExact(21) == 42);
            }}
        {{
{} /// JAVADOC
MethodHandle cat = lookup().findVirtual(String.class,
  "concat", methodType(String.class, String.class));
assertEquals("xy", (String) cat.invokeExact("x", "y"));
MethodHandle d0 = dropArguments(cat, 0, String.class);
assertEquals("yz", (String) d0.invokeExact("x", "y", "z"));
MethodHandle d1 = dropArguments(cat, 1, String.class);
assertEquals("xz", (String) d1.invokeExact("x", "y", "z"));
MethodHandle d2 = dropArguments(cat, 2, String.class);
assertEquals("xy", (String) d2.invokeExact("x", "y", "z"));
MethodHandle d12 = dropArguments(cat, 1, int.class, boolean.class);
assertEquals("xz", (String) d12.invokeExact("x", 12, true, "z"));
            }}
    }

@Test public void testZero() throws Throwable {
        {{
{} /// JAVADOC
Class<?> type = Double.class;
MethodHandle mh1 = MethodHandles.explicitCastArguments(MethodHandles.constant(Object.class, null), methodType(type));
assertEquals("()Double", mh1.type().toString());
MethodHandle mh2 = MethodHandles.empty(methodType(type));
assertEquals("()Double", mh2.type().toString());
        }}
   }

    @Test public void testDropArguments() throws Throwable {
        {{
{} /// JAVADOC
MethodHandle cat = lookup().findVirtual(String.class,
  "concat", methodType(String.class, String.class));
assertEquals("xy", (String) cat.invokeExact("x", "y"));
MethodType bigType = cat.type().insertParameterTypes(0, int.class, String.class);
MethodHandle d0 = dropArguments(cat, 0, bigType.parameterList().subList(0,2));
assertEquals(bigType, d0.type());
assertEquals("yz", (String) d0.invokeExact(123, "x", "y", "z"));
            }}
        {{
{} /// JAVADOC
MethodHandle cat = lookup().findVirtual(String.class,
  "concat", methodType(String.class, String.class));
assertEquals("xy", (String) cat.invokeExact("x", "y"));
MethodHandle d0 = dropArguments(cat, 0, String.class);
assertEquals("yz", (String) d0.invokeExact("x", "y", "z"));
MethodHandle d1 = dropArguments(cat, 1, String.class);
assertEquals("xz", (String) d1.invokeExact("x", "y", "z"));
MethodHandle d2 = dropArguments(cat, 2, String.class);
assertEquals("xy", (String) d2.invokeExact("x", "y", "z"));
MethodHandle d12 = dropArguments(cat, 1, int.class, boolean.class);
assertEquals("xz", (String) d12.invokeExact("x", 12, true, "z"));
            }}
    }

    @Test public void testDropArgumentsToMatch() throws Throwable {
        {{
{} /// JAVADOC
MethodHandle h0= constant(boolean.class, true);
MethodHandle h1 = lookup().findVirtual(String.class, "concat", methodType(String.class, String.class));
MethodType bigType = h1.type().insertParameterTypes(1, String.class, int.class);
MethodHandle h2 = dropArguments(h1, 0, bigType.parameterList());
if (h1.type().parameterCount() < h2.type().parameterCount()) {
    h1 = dropArgumentsToMatch(h1, 0, h2.type().parameterList(), 0);  // lengthen h1
}
else {
    h2 = dropArgumentsToMatch(h2, 0, h1.type().parameterList(), 0);    // lengthen h2
}
MethodHandle h3 = guardWithTest(h0, h1, h2);
assertEquals("xy", h3.invoke("x", "y", 1, "a", "b", "c"));
        }}
    }

    @Test public void testFilterArguments() throws Throwable {
        {{
{} /// JAVADOC
MethodHandle cat = lookup().findVirtual(String.class,
  "concat", methodType(String.class, String.class));
MethodHandle upcase = lookup().findVirtual(String.class,
  "toUpperCase", methodType(String.class));
assertEquals("xy", (String) cat.invokeExact("x", "y"));
MethodHandle f0 = filterArguments(cat, 0, upcase);
assertEquals("Xy", (String) f0.invokeExact("x", "y")); // Xy
MethodHandle f1 = filterArguments(cat, 1, upcase);
assertEquals("xY", (String) f1.invokeExact("x", "y")); // xY
MethodHandle f2 = filterArguments(cat, 0, upcase, upcase);
assertEquals("XY", (String) f2.invokeExact("x", "y")); // XY
            }}
    }

    @Test public void testCollectArguments() throws Throwable {
        {{
{} /// JAVADOC
MethodHandle deepToString = publicLookup()
  .findStatic(Arrays.class, "deepToString", methodType(String.class, Object[].class));
MethodHandle ts1 = deepToString.asCollector(String[].class, 1);
assertEquals("[strange]", (String) ts1.invokeExact("strange"));
MethodHandle ts2 = deepToString.asCollector(String[].class, 2);
assertEquals("[up, down]", (String) ts2.invokeExact("up", "down"));
MethodHandle ts3 = deepToString.asCollector(String[].class, 3);
MethodHandle ts3_ts2 = collectArguments(ts3, 1, ts2);
assertEquals("[top, [up, down], strange]",
             (String) ts3_ts2.invokeExact("top", "up", "down", "strange"));
MethodHandle ts3_ts2_ts1 = collectArguments(ts3_ts2, 3, ts1);
assertEquals("[top, [up, down], [strange]]",
             (String) ts3_ts2_ts1.invokeExact("top", "up", "down", "strange"));
MethodHandle ts3_ts2_ts3 = collectArguments(ts3_ts2, 1, ts3);
assertEquals("[top, [[up, down, strange], charm], bottom]",
             (String) ts3_ts2_ts3.invokeExact("top", "up", "down", "strange", "charm", "bottom"));
            }}
    }

    @Test public void testFoldArguments() throws Throwable {
        {{
{} /// JAVADOC
MethodHandle trace = publicLookup().findVirtual(java.io.PrintStream.class,
  "println", methodType(void.class, String.class))
    .bindTo(System.out);
MethodHandle cat = lookup().findVirtual(String.class,
  "concat", methodType(String.class, String.class));
assertEquals("boojum", (String) cat.invokeExact("boo", "jum"));
MethodHandle catTrace = foldArguments(cat, trace);
// also prints "boo":
assertEquals("boojum", (String) catTrace.invokeExact("boo", "jum"));
            }}
    }

    static void assertEquals(Object exp, Object act) {
        if (verbosity > 0)
            System.out.println("result: "+act);
        Assert.assertEquals(exp, act);
    }

    static void assertTrue(boolean b) {
        if (verbosity > 0) {
            System.out.println("result: " + b);
        }
        Assert.assertTrue(b);
    }

    @Test public void testMethodHandlesSummary() throws Throwable {
        {{
{} /// JAVADOC
Object x, y; String s; int i;
MethodType mt; MethodHandle mh;
MethodHandles.Lookup lookup = MethodHandles.lookup();
// mt is (char,char)String
mt = MethodType.methodType(String.class, char.class, char.class);
mh = lookup.findVirtual(String.class, "replace", mt);
s = (String) mh.invokeExact("daddy",'d','n');
// invokeExact(Ljava/lang/String;CC)Ljava/lang/String;
assertEquals(s, "nanny");
// weakly typed invocation (using MHs.invoke)
s = (String) mh.invokeWithArguments("sappy", 'p', 'v');
assertEquals(s, "savvy");
// mt is (Object[])List
mt = MethodType.methodType(java.util.List.class, Object[].class);
mh = lookup.findStatic(java.util.Arrays.class, "asList", mt);
assert(mh.isVarargsCollector());
x = mh.invoke("one", "two");
// invoke(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/Object;
assertEquals(x, java.util.Arrays.asList("one","two"));
// mt is (Object,Object,Object)Object
mt = MethodType.genericMethodType(3);
mh = mh.asType(mt);
x = mh.invokeExact((Object)1, (Object)2, (Object)3);
// invokeExact(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
assertEquals(x, java.util.Arrays.asList(1,2,3));
// mt is ()int
mt = MethodType.methodType(int.class);
mh = lookup.findVirtual(java.util.List.class, "size", mt);
i = (int) mh.invokeExact(java.util.Arrays.asList(1,2,3));
// invokeExact(Ljava/util/List;)I
assert(i == 3);
mt = MethodType.methodType(void.class, String.class);
mh = lookup.findVirtual(java.io.PrintStream.class, "println", mt);
mh.invokeExact(System.out, "Hello, world.");
// invokeExact(Ljava/io/PrintStream;Ljava/lang/String;)V
{}
            }}
    }

    @Test public void testAsSpreader() throws Throwable {
        {{
{} /// JAVADOC
MethodHandle equals = publicLookup()
  .findVirtual(String.class, "equals", methodType(boolean.class, Object.class));
assert( (boolean) equals.invokeExact("me", (Object)"me"));
assert(!(boolean) equals.invokeExact("me", (Object)"thee"));
// spread both arguments from a 2-array:
MethodHandle eq2 = equals.asSpreader(Object[].class, 2);
assert( (boolean) eq2.invokeExact(new Object[]{ "me", "me" }));
assert(!(boolean) eq2.invokeExact(new Object[]{ "me", "thee" }));
// try to spread from anything but a 2-array:
for (int n = 0; n <= 10; n++) {
  Object[] badArityArgs = (n == 2 ? new Object[0] : new Object[n]);
  try { assert((boolean) eq2.invokeExact(badArityArgs) && false); }
  catch (IllegalArgumentException ex) { } // OK
}
// spread both arguments from a String array:
MethodHandle eq2s = equals.asSpreader(String[].class, 2);
assert( (boolean) eq2s.invokeExact(new String[]{ "me", "me" }));
assert(!(boolean) eq2s.invokeExact(new String[]{ "me", "thee" }));
// spread second arguments from a 1-array:
MethodHandle eq1 = equals.asSpreader(Object[].class, 1);
assert( (boolean) eq1.invokeExact("me", new Object[]{ "me" }));
assert(!(boolean) eq1.invokeExact("me", new Object[]{ "thee" }));
// spread no arguments from a 0-array or null:
MethodHandle eq0 = equals.asSpreader(Object[].class, 0);
assert( (boolean) eq0.invokeExact("me", (Object)"me", new Object[0]));
assert(!(boolean) eq0.invokeExact("me", (Object)"thee", (Object[])null));
// asSpreader and asCollector are approximate inverses:
for (int n = 0; n <= 2; n++) {
    for (Class<?> a : new Class<?>[]{Object[].class, String[].class, CharSequence[].class}) {
        MethodHandle equals2 = equals.asSpreader(a, n).asCollector(a, n);
        assert( (boolean) equals2.invokeWithArguments("me", "me"));
        assert(!(boolean) equals2.invokeWithArguments("me", "thee"));
    }
}
MethodHandle caToString = publicLookup()
  .findStatic(Arrays.class, "toString", methodType(String.class, char[].class));
assertEquals("[A, B, C]", (String) caToString.invokeExact("ABC".toCharArray()));
MethodHandle caString3 = caToString.asCollector(char[].class, 3);
assertEquals("[A, B, C]", (String) caString3.invokeExact('A', 'B', 'C'));
MethodHandle caToString2 = caString3.asSpreader(char[].class, 2);
assertEquals("[A, B, C]", (String) caToString2.invokeExact('A', "BC".toCharArray()));
            }}
    }

    @Test public void testAsCollector() throws Throwable {
        {{
{} /// JAVADOC
MethodHandle deepToString = publicLookup()
  .findStatic(Arrays.class, "deepToString", methodType(String.class, Object[].class));
assertEquals("[won]",   (String) deepToString.invokeExact(new Object[]{"won"}));
MethodHandle ts1 = deepToString.asCollector(Object[].class, 1);
assertEquals(methodType(String.class, Object.class), ts1.type());
//assertEquals("[won]", (String) ts1.invokeExact(         new Object[]{"won"})); //FAIL
assertEquals("[[won]]", (String) ts1.invokeExact((Object) new Object[]{"won"}));
// arrayType can be a subtype of Object[]
MethodHandle ts2 = deepToString.asCollector(String[].class, 2);
assertEquals(methodType(String.class, String.class, String.class), ts2.type());
assertEquals("[two, too]", (String) ts2.invokeExact("two", "too"));
MethodHandle ts0 = deepToString.asCollector(Object[].class, 0);
assertEquals("[]", (String) ts0.invokeExact());
// collectors can be nested, Lisp-style
MethodHandle ts22 = deepToString.asCollector(Object[].class, 3).asCollector(String[].class, 2);
assertEquals("[A, B, [C, D]]", ((String) ts22.invokeExact((Object)'A', (Object)"B", "C", "D")));
// arrayType can be any primitive array type
MethodHandle bytesToString = publicLookup()
  .findStatic(Arrays.class, "toString", methodType(String.class, byte[].class))
  .asCollector(byte[].class, 3);
assertEquals("[1, 2, 3]", (String) bytesToString.invokeExact((byte)1, (byte)2, (byte)3));
MethodHandle longsToString = publicLookup()
  .findStatic(Arrays.class, "toString", methodType(String.class, long[].class))
  .asCollector(long[].class, 1);
assertEquals("[123]", (String) longsToString.invokeExact((long)123));
            }}
    }

    @SuppressWarnings("rawtypes")
    @Test public void testAsVarargsCollector() throws Throwable {
        {{
{} /// JAVADOC
MethodHandle deepToString = publicLookup()
  .findStatic(Arrays.class, "deepToString", methodType(String.class, Object[].class));
MethodHandle ts1 = deepToString.asVarargsCollector(Object[].class);
assertEquals("[won]",   (String) ts1.invokeExact(    new Object[]{"won"}));
assertEquals("[won]",   (String) ts1.invoke(         new Object[]{"won"}));
assertEquals("[won]",   (String) ts1.invoke(                      "won" ));
assertEquals("[[won]]", (String) ts1.invoke((Object) new Object[]{"won"}));
// findStatic of Arrays.asList(...) produces a variable arity method handle:
MethodHandle asList = publicLookup()
  .findStatic(Arrays.class, "asList", methodType(List.class, Object[].class));
assertEquals(methodType(List.class, Object[].class), asList.type());
assert(asList.isVarargsCollector());
assertEquals("[]", asList.invoke().toString());
assertEquals("[1]", asList.invoke(1).toString());
assertEquals("[two, too]", asList.invoke("two", "too").toString());
String[] argv = { "three", "thee", "tee" };
assertEquals("[three, thee, tee]", asList.invoke(argv).toString());
assertEquals("[three, thee, tee]", asList.invoke((Object[])argv).toString());
List ls = (List) asList.invoke((Object)argv);
assertEquals(1, ls.size());
assertEquals("[three, thee, tee]", Arrays.toString((Object[])ls.get(0)));
            }}
    }

    @Test public void testAsFixedArity() throws Throwable {
        {{
{} /// JAVADOC
MethodHandle asListVar = publicLookup()
  .findStatic(Arrays.class, "asList", methodType(List.class, Object[].class))
  .asVarargsCollector(Object[].class);
MethodHandle asListFix = asListVar.asFixedArity();
assertEquals("[1]", asListVar.invoke(1).toString());
Exception caught = null;
try { asListFix.invoke((Object)1); }
catch (Exception ex) { caught = ex; }
assert(caught instanceof ClassCastException);
assertEquals("[two, too]", asListVar.invoke("two", "too").toString());
try { asListFix.invoke("two", "too"); }
catch (Exception ex) { caught = ex; }
assert(caught instanceof WrongMethodTypeException);
Object[] argv = { "three", "thee", "tee" };
assertEquals("[three, thee, tee]", asListVar.invoke(argv).toString());
assertEquals("[three, thee, tee]", asListFix.invoke(argv).toString());
assertEquals(1, ((List) asListVar.invoke((Object)argv)).size());
assertEquals("[three, thee, tee]", asListFix.invoke((Object)argv).toString());
            }}
    }

    @Test public void testAsTypeCornerCases() throws Throwable {
        {{
{} /// JAVADOC
MethodHandle i2s = publicLookup()
  .findVirtual(Integer.class, "toString", methodType(String.class));
i2s = i2s.asType(i2s.type().unwrap());
MethodHandle l2s = publicLookup()
  .findVirtual(Long.class, "toString", methodType(String.class));
l2s = l2s.asType(l2s.type().unwrap());

Exception caught = null;
try { i2s.asType(methodType(String.class, String.class)); }
catch (Exception ex) { caught = ex; }
assert(caught instanceof WrongMethodTypeException);

i2s.asType(methodType(String.class, byte.class));
i2s.asType(methodType(String.class, Byte.class));
i2s.asType(methodType(String.class, Character.class));
i2s.asType(methodType(String.class, Integer.class));
l2s.asType(methodType(String.class, byte.class));
l2s.asType(methodType(String.class, Byte.class));
l2s.asType(methodType(String.class, Character.class));
l2s.asType(methodType(String.class, Integer.class));
l2s.asType(methodType(String.class, Long.class));

caught = null;
try { i2s.asType(methodType(String.class, Long.class)); }
catch (Exception ex) { caught = ex; }
assert(caught instanceof WrongMethodTypeException);

MethodHandle i2sGen = i2s.asType(methodType(String.class, Object.class));
MethodHandle l2sGen = l2s.asType(methodType(String.class, Object.class));

i2sGen.invoke(42);  // int -> Integer -> Object -> Integer -> int
i2sGen.invoke((byte)4);  // byte -> Byte -> Object -> Byte -> byte -> int
l2sGen.invoke(42);  // int -> Integer -> Object -> Integer -> int
l2sGen.invoke((byte)4);  // byte -> Byte -> Object -> Byte -> byte -> int
l2sGen.invoke(0x420000000L);

caught = null;
try { i2sGen.invoke(0x420000000L); } // long -> Long -> Object -> Integer CCE
catch (Exception ex) { caught = ex; }
assert(caught instanceof ClassCastException);

caught = null;
try { i2sGen.invoke("asdf"); } // String -> Object -> Integer CCE
catch (Exception ex) { caught = ex; }
assert(caught instanceof ClassCastException);
{}
            }}
    }

    @Test public void testMutableCallSite() throws Throwable {
        {{
{} /// JAVADOC
MutableCallSite name = new MutableCallSite(MethodType.methodType(String.class));
MethodHandle MH_name = name.dynamicInvoker();
MethodType MT_str1 = MethodType.methodType(String.class);
MethodHandle MH_upcase = MethodHandles.lookup()
    .findVirtual(String.class, "toUpperCase", MT_str1);
MethodHandle worker1 = MethodHandles.filterReturnValue(MH_name, MH_upcase);
name.setTarget(MethodHandles.constant(String.class, "Rocky"));
assertEquals("ROCKY", (String) worker1.invokeExact());
name.setTarget(MethodHandles.constant(String.class, "Fred"));
assertEquals("FRED", (String) worker1.invokeExact());
// (mutation can be continued indefinitely)
/*
 * </pre></blockquote>
 * <p>
 * The same call site may be used in several places at once.
 * <blockquote><pre>
 */
MethodType MT_str2 = MethodType.methodType(String.class, String.class);
MethodHandle MH_cat = lookup().findVirtual(String.class,
  "concat", methodType(String.class, String.class));
MethodHandle MH_dear = MethodHandles.insertArguments(MH_cat, 1, ", dear?");
MethodHandle worker2 = MethodHandles.filterReturnValue(MH_name, MH_dear);
assertEquals("Fred, dear?", (String) worker2.invokeExact());
name.setTarget(MethodHandles.constant(String.class, "Wilma"));
assertEquals("WILMA", (String) worker1.invokeExact());
assertEquals("Wilma, dear?", (String) worker2.invokeExact());
{}
            }}
    }

    @Test public void testSwitchPoint() throws Throwable {
        {{
{} /// JAVADOC
MethodHandle MH_strcat = MethodHandles.lookup()
    .findVirtual(String.class, "concat", MethodType.methodType(String.class, String.class));
SwitchPoint spt = new SwitchPoint();
assert(!spt.hasBeenInvalidated());
// the following steps may be repeated to re-use the same switch point:
MethodHandle worker1 = MH_strcat;
MethodHandle worker2 = MethodHandles.permuteArguments(MH_strcat, MH_strcat.type(), 1, 0);
MethodHandle worker = spt.guardWithTest(worker1, worker2);
assertEquals("method", (String) worker.invokeExact("met", "hod"));
SwitchPoint.invalidateAll(new SwitchPoint[]{ spt });
assert(spt.hasBeenInvalidated());
assertEquals("hodmet", (String) worker.invokeExact("met", "hod"));
{}
            }}
    }

    @Test public void testFoldArguments2() throws Throwable {
        {{
{} /// JAVADOC
// argument-based dispatch for methods of the form boolean x.___(y: String)
Lookup lookup = publicLookup();
// first, a tracing hack:
MethodHandle println = lookup.findVirtual(java.io.PrintStream.class, "println", methodType(void.class, String.class));
MethodHandle arrayToString = lookup.findStatic(Arrays.class, "toString", methodType(String.class, Object[].class));
MethodHandle concat = lookup.findVirtual(String.class, "concat", methodType(String.class, String.class));
MethodHandle arrayToString_DIS = filterReturnValue(arrayToString, concat.bindTo("DIS:"));
MethodHandle arrayToString_INV = filterReturnValue(arrayToString, concat.bindTo("INV:"));
MethodHandle printArgs_DIS = filterReturnValue(arrayToString_DIS, println.bindTo(System.out)).asVarargsCollector(Object[].class);
MethodHandle printArgs_INV = filterReturnValue(arrayToString_INV, println.bindTo(System.out)).asVarargsCollector(Object[].class);
// metaobject protocol:
MethodType mtype = methodType(boolean.class, String.class);
MethodHandle findVirtual = lookup.findVirtual(Lookup.class,
  "findVirtual", methodType(MethodHandle.class, Class.class, String.class, MethodType.class));
MethodHandle getClass = lookup.findVirtual(Object.class,
  "getClass", methodType(Class.class));
MethodHandle dispatch = findVirtual;
dispatch = filterArguments(dispatch, 1, getClass);
dispatch = insertArguments(dispatch, 3, mtype);
dispatch = dispatch.bindTo(lookup);
assertEquals(methodType(MethodHandle.class, Object.class, String.class), dispatch.type());
MethodHandle invoker = invoker(mtype.insertParameterTypes(0, Object.class));
// wrap tracing around the dispatch and invoke steps:
dispatch = foldArguments(dispatch, printArgs_DIS.asType(dispatch.type().changeReturnType(void.class)));
invoker = foldArguments(invoker, printArgs_INV.asType(invoker.type().changeReturnType(void.class)));
invoker = dropArguments(invoker, 2, String.class);  // ignore selector
// compose the dispatcher and the invoker:
MethodHandle invokeDispatched = foldArguments(invoker, dispatch);
Object x = "football", y = new java.util.Scanner("bar");
assert( (boolean) invokeDispatched.invokeExact(x, "startsWith", "foo"));
assert(!(boolean) invokeDispatched.invokeExact(x, "startsWith", "#"));
assert( (boolean) invokeDispatched.invokeExact(x, "endsWith", "all"));
assert(!(boolean) invokeDispatched.invokeExact(x, "endsWith", "foo"));
assert( (boolean) invokeDispatched.invokeExact(y, "hasNext", "[abc]+[rst]"));
assert(!(boolean) invokeDispatched.invokeExact(y, "hasNext", "[123]+[789]"));
            }}
    }

    static int one(int k) { return 1; }
    static int inc(int i, int acc, int k) { return i + 1; }
    static int mult(int i, int acc, int k) { return i * acc; }
    static boolean pred(int i, int acc, int k) { return i < k; }
    static int fin(int i, int acc, int k) { return acc; }

    @Test public void testLoop() throws Throwable {
        MethodHandle MH_inc, MH_one, MH_mult, MH_pred, MH_fin;
        Class<?> I = int.class;
        MH_inc = LOOKUP.findStatic(THIS_CLASS, "inc", methodType(I, I, I, I));
        MH_one = LOOKUP.findStatic(THIS_CLASS, "one", methodType(I, I));
        MH_mult = LOOKUP.findStatic(THIS_CLASS, "mult", methodType(I, I, I, I));
        MH_pred = LOOKUP.findStatic(THIS_CLASS, "pred", methodType(boolean.class, I, I, I));
        MH_fin = LOOKUP.findStatic(THIS_CLASS, "fin", methodType(I, I, I, I));
        {{
{} /// JAVADOC
// iterative implementation of the factorial function as a loop handle
// null initializer for counter, should initialize to 0
MethodHandle[] counterClause = new MethodHandle[]{null, MH_inc};
MethodHandle[] accumulatorClause = new MethodHandle[]{MH_one, MH_mult, MH_pred, MH_fin};
MethodHandle loop = MethodHandles.loop(counterClause, accumulatorClause);
assertEquals(120, loop.invoke(5));
{}
        }}
    }

    static int inc(int i) { return i + 1; } // drop acc, k
    static int mult(int i, int acc) { return i * acc; } //drop k
    static boolean cmp(int i, int k) { return i < k; }

    @Test public void testSimplerLoop() throws Throwable {
        MethodHandle MH_inc, MH_mult, MH_cmp;
        Class<?> I = int.class;
        MH_inc = LOOKUP.findStatic(THIS_CLASS, "inc", methodType(I, I));
        MH_mult = LOOKUP.findStatic(THIS_CLASS, "mult", methodType(I, I, I));
        MH_cmp = LOOKUP.findStatic(THIS_CLASS, "cmp", methodType(boolean.class, I, I));
        {{
{} /// JAVADOC
// simplified implementation of the factorial function as a loop handle
// null initializer for counter, should initialize to 0
MethodHandle MH_one = MethodHandles.constant(int.class, 1);
MethodHandle MH_pred = MethodHandles.dropArguments(MH_cmp, 1, int.class); // drop acc
MethodHandle MH_fin = MethodHandles.dropArguments(MethodHandles.identity(int.class), 0, int.class); // drop i
MethodHandle[] counterClause = new MethodHandle[]{null, MH_inc};
MethodHandle[] accumulatorClause = new MethodHandle[]{MH_one, MH_mult, MH_pred, MH_fin};
MethodHandle loop = MethodHandles.loop(counterClause, accumulatorClause);
assertEquals(720, loop.invoke(6));
{}
        }}
    }

    // for testFacLoop
{}
static class FacLoop {
  final int k;
  FacLoop(int k) { this.k = k; }
  int inc(int i) { return i + 1; }
  int mult(int i, int acc) { return i * acc; }
  boolean pred(int i) { return i < k; }
  int fin(int i, int acc) { return acc; }
}
{}

    // assume MH_inc, MH_mult, and MH_pred are handles to the above methods
    @Test public void testFacLoop() throws Throwable {
        MethodHandle MH_FacLoop, MH_inc, MH_mult, MH_pred, MH_fin;
        Class<?> I = int.class;
        MH_FacLoop = LOOKUP.findConstructor(FacLoop.class, methodType(void.class, I));
        MH_inc = LOOKUP.findVirtual(FacLoop.class, "inc", methodType(I, I));
        MH_mult = LOOKUP.findVirtual(FacLoop.class, "mult", methodType(I, I, I));
        MH_pred = LOOKUP.findVirtual(FacLoop.class, "pred", methodType(boolean.class, I));
        MH_fin = LOOKUP.findVirtual(FacLoop.class, "fin", methodType(I, I, I));
        {{
{} /// JAVADOC
// instance-based implementation of the factorial function as a loop handle
// null initializer for counter, should initialize to 0
MethodHandle MH_one = MethodHandles.constant(int.class, 1);
MethodHandle[] instanceClause = new MethodHandle[]{MH_FacLoop};
MethodHandle[] counterClause = new MethodHandle[]{null, MH_inc};
MethodHandle[] accumulatorClause = new MethodHandle[]{MH_one, MH_mult, MH_pred, MH_fin};
MethodHandle loop = MethodHandles.loop(instanceClause, counterClause, accumulatorClause);
assertEquals(5040, loop.invoke(7));
{}
        }}
    }

    static List<String> initZip(Iterator<String> a, Iterator<String> b) { return new ArrayList<>(); }
    static boolean zipPred(List<String> zip, Iterator<String> a, Iterator<String> b) { return a.hasNext() && b.hasNext(); }
    static List<String> zipStep(List<String> zip, Iterator<String> a, Iterator<String> b) {
        zip.add(a.next());
        zip.add(b.next());
        return zip;
    }

    @Test public void testWhileLoop() throws Throwable {
        MethodHandle MH_initZip, MH_zipPred, MH_zipStep;
        Class<?> IT = Iterator.class;
        Class<?> L = List.class;
        MH_initZip = LOOKUP.findStatic(THIS_CLASS, "initZip", methodType(L, IT, IT));
        MH_zipPred = LOOKUP.findStatic(THIS_CLASS, "zipPred", methodType(boolean.class, L, IT, IT));
        MH_zipStep = LOOKUP.findStatic(THIS_CLASS, "zipStep", methodType(L, L, IT, IT));
        {{
{} /// JAVADOC
// implement the zip function for lists as a loop handle
MethodHandle loop = MethodHandles.whileLoop(MH_initZip, MH_zipPred, MH_zipStep);
List<String> a = Arrays.asList("a", "b", "c", "d");
List<String> b = Arrays.asList("e", "f", "g", "h");
List<String> zipped = Arrays.asList("a", "e", "b", "f", "c", "g", "d", "h");
assertEquals(zipped, (List<String>) loop.invoke(a.iterator(), b.iterator()));
{}
        }}
    }

    static int zero(int limit) { return 0; }
    static int step(int i, int limit) { return i + 1; }
    static boolean pred(int i, int limit) { return i < limit; }

    @Test public void testDoWhileLoop() throws Throwable {
        MethodHandle MH_zero, MH_step, MH_pred;
        Class<?> I = int.class;
        MH_zero = LOOKUP.findStatic(THIS_CLASS, "zero", methodType(I, I));
        MH_step = LOOKUP.findStatic(THIS_CLASS, "step", methodType(I, I, I));
        MH_pred = LOOKUP.findStatic(THIS_CLASS, "pred", methodType(boolean.class, I, I));
        {{
{} /// JAVADOC
// int i = 0; while (i < limit) { ++i; } return i; => limit
MethodHandle loop = MethodHandles.doWhileLoop(MH_zero, MH_step, MH_pred);
assertEquals(23, loop.invoke(23));
{}
        }}
    }

    static String step(String v, int counter, String start_) { return "na " + v; }  //#0
    static String step(String v, int counter ) { return "na " + v; } //#1
    static String step(String v, int counter, int iterations_, String pre, String start_) { return pre + " " + v; } //#2
    static String step3(String v, int counter, String pre) { return pre + " " + v; } //#3

    @Test public void testCountedLoop() throws Throwable {
        MethodHandle MH_step;
        Class<?> S = String.class, I = int.class;
        // Theme:
        MH_step = LOOKUP.findStatic(THIS_CLASS, "step", methodType(S, S, I, S));
        {{
{} /// JAVADOC
// String s = "Lambdaman!"; for (int i = 0; i < 13; ++i) { s = "na " + s; } return s;
// => a variation on a well known theme
MethodHandle fit13 = MethodHandles.constant(int.class, 13);
MethodHandle start = MethodHandles.identity(String.class);
MethodHandle loop = MethodHandles.countedLoop(fit13, start, MH_step);  // (v, i, _) -> "na " + v
assertEquals("na na na na na na na na na na na na na Lambdaman!", loop.invoke("Lambdaman!"));
{}
        }}
        // Variation #1:
        MH_step = LOOKUP.findStatic(THIS_CLASS, "step", methodType(S, S, I));
        {{
{} /// JAVADOC
// String s = "Lambdaman!"; for (int i = 0; i < 13; ++i) { s = "na " + s; } return s;
// => a variation on a well known theme
MethodHandle count = MethodHandles.dropArguments(MethodHandles.identity(int.class), 1, String.class);
MethodHandle start = MethodHandles.dropArguments(MethodHandles.identity(String.class), 0, int.class);
MethodHandle loop = MethodHandles.countedLoop(count, start, MH_step);  // (v, i) -> "na " + v
assertEquals("na na na na na na na na na na na na na Lambdaman!", loop.invoke(13, "Lambdaman!"));
{}
            assertEquals("na na Lambdaman!", loop.invoke(2, "Lambdaman!"));
            assertEquals("Lambdaman!", loop.invoke(0, "Lambdaman!"));
            assertEquals("Lambdaman!", loop.invoke(-1, "Lambdaman!"));
            assertEquals("Lambdaman!", loop.invoke(Integer.MIN_VALUE, "Lambdaman!"));
        }}
        // Variation #2:
        MH_step = LOOKUP.findStatic(THIS_CLASS, "step", methodType(S, S, I, I, S, S));
        {{
{} /// JAVADOC
// String s = "Lambdaman!", t = "na"; for (int i = 0; i < 13; ++i) { s = t + " " + s; } return s;
// => a variation on a well known theme
MethodHandle count = MethodHandles.identity(int.class);
MethodHandle start = MethodHandles.dropArguments(MethodHandles.identity(String.class), 0, int.class, String.class);
MethodHandle loop = MethodHandles.countedLoop(count, start, MH_step);  // (v, i, _, pre, _) -> pre + " " + v
assertEquals("na na na na na na na na na na na na na Lambdaman!", loop.invoke(13, "na", "Lambdaman!"));
{}
        }}
        // Variation #3:
        MH_step = LOOKUP.findStatic(THIS_CLASS, "step3", methodType(S, S, I, S));
        {{
{} /// JAVADOC
// String s = "Lambdaman!", t = "na"; for (int i = 0; i < 13; ++i) { s = t + " " + s; } return s;
// => a variation on a well known theme
MethodType loopType = methodType(String.class, String.class, int.class, String.class);
MethodHandle count = MethodHandles.dropArgumentsToMatch(MethodHandles.identity(int.class),    0, loopType.parameterList(), 1);
MethodHandle start = MethodHandles.dropArgumentsToMatch(MethodHandles.identity(String.class), 0, loopType.parameterList(), 2);
MethodHandle body  = MethodHandles.dropArgumentsToMatch(MH_step,                              2, loopType.parameterList(), 0);
MethodHandle loop = MethodHandles.countedLoop(count, start, body);  // (v, i, pre, _, _) -> pre + " " + v
assertEquals("na na na na na na na na na na na na na Lambdaman!", loop.invoke("na", 13, "Lambdaman!"));
{}
        }}
    }

    static List<String> reverseStep(List<String> r, String e) {
        r.add(0, e);
        return r;
    }
    static List<String> newArrayList() { return new ArrayList<>(); }

    @Test public void testIteratedLoop() throws Throwable {
        MethodHandle MH_newArrayList, MH_reverseStep;
        Class<?> L = List.class, S = String.class;
        MH_newArrayList = LOOKUP.findStatic(THIS_CLASS, "newArrayList", methodType(L));
        MH_reverseStep = LOOKUP.findStatic(THIS_CLASS, "reverseStep", methodType(L, L, S));
        {{
{} /// JAVADOC
// reverse a list
MethodHandle loop = MethodHandles.iteratedLoop(null, MH_newArrayList, MH_reverseStep);
List<String> list = Arrays.asList("a", "b", "c", "d", "e");
List<String> reversedList = Arrays.asList("e", "d", "c", "b", "a");
assertEquals(reversedList, (List<String>) loop.invoke(list));
{}
        }}
    }

    @Test public void testFoldArguments3() throws Throwable {
        {{
{} /// JAVADOC
MethodHandle trace = publicLookup().findVirtual(java.io.PrintStream.class,
        "println", methodType(void.class, String.class))
        .bindTo(System.out);
MethodHandle cat = lookup().findVirtual(String.class,
        "concat", methodType(String.class, String.class));
assertEquals("boojum", (String) cat.invokeExact("boo", "jum"));
MethodHandle catTrace = foldArguments(cat, 1, trace);
// also prints "jum":
assertEquals("boojum", (String) catTrace.invokeExact("boo", "jum"));
{}
        }}
    }

    @Test public void testAsCollector2() throws Throwable {
        {{
{} /// JAVADOC
StringWriter swr = new StringWriter();
MethodHandle swWrite = LOOKUP.findVirtual(StringWriter.class, "write", methodType(void.class, char[].class, int.class, int.class)).bindTo(swr);
MethodHandle swWrite4 = swWrite.asCollector(0, char[].class, 4);
swWrite4.invoke('A', 'B', 'C', 'D', 1, 2);
assertEquals("BC", swr.toString());
swWrite4.invoke('P', 'Q', 'R', 'S', 0, 4);
assertEquals("BCPQRS", swr.toString());
swWrite4.invoke('W', 'X', 'Y', 'Z', 3, 1);
assertEquals("BCPQRSZ", swr.toString());
{}
        }}
    }

    @Test public void testAsSpreader2() throws Throwable {
        {{
{} /// JAVADOC
MethodHandle compare = LOOKUP.findStatic(Objects.class, "compare", methodType(int.class, Object.class, Object.class, Comparator.class));
MethodHandle compare2FromArray = compare.asSpreader(0, Object[].class, 2);
Object[] ints = new Object[]{3, 9, 7, 7};
Comparator<Integer> cmp = (a, b) -> a - b;
assertTrue((int) compare2FromArray.invoke(Arrays.copyOfRange(ints, 0, 2), cmp) < 0);
assertTrue((int) compare2FromArray.invoke(Arrays.copyOfRange(ints, 1, 3), cmp) > 0);
assertTrue((int) compare2FromArray.invoke(Arrays.copyOfRange(ints, 2, 4), cmp) == 0);
{}
        }}
    }

    /* ---- TEMPLATE ----
    @Test public void testFoo() throws Throwable {
        {{
{} /// JAVADOC
{}
            }}
    }
    */
}
