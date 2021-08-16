/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @summary verify Lookup.revealDirect on a variety of input handles
 * @modules java.base/jdk.internal.reflect
 * @compile -XDignore.symbol.file RevealDirectTest.java
 * @run junit/othervm -ea -esa test.java.lang.invoke.RevealDirectTest
 *
 * @test
 * @summary verify Lookup.revealDirect on a variety of input handles, with security manager
 * @run main/othervm/policy=jtreg.security.policy/secure=java.lang.SecurityManager -ea -esa test.java.lang.invoke.RevealDirectTest
 */

/* To run manually:
 * $ $JAVA8X_HOME/bin/javac -cp $JUNIT4_JAR -d ../../../.. -XDignore.symbol.file RevealDirectTest.java
 * $ $JAVA8X_HOME/bin/java  -cp $JUNIT4_JAR:../../../.. -ea -esa org.junit.runner.JUnitCore test.java.lang.invoke.RevealDirectTest
 * $ $JAVA8X_HOME/bin/java  -cp $JUNIT4_JAR:../../../.. -ea -esa    -Djava.security.manager test.java.lang.invoke.RevealDirectTest
 */

package test.java.lang.invoke;

import java.lang.reflect.*;
import java.lang.invoke.*;
import static java.lang.invoke.MethodHandles.*;
import static java.lang.invoke.MethodType.*;
import static java.lang.invoke.MethodHandleInfo.*;
import java.util.*;
import static org.junit.Assert.*;
import org.junit.*;

public class RevealDirectTest {
    public static void main(String... av) throws Throwable {
        // Run the @Test methods explicitly, in case we don't want to use the JUnitCore driver.
        // This appears to be necessary when running with a security manager.
        Throwable fail = null;
        for (Method test : RevealDirectTest.class.getDeclaredMethods()) {
            if (!test.isAnnotationPresent(Test.class))  continue;
            try {
                test.invoke(new RevealDirectTest());
            } catch (Throwable ex) {
                if (ex instanceof InvocationTargetException)
                    ex = ex.getCause();
                if (fail == null)  fail = ex;
                System.out.println("Testcase: "+test.getName()
                                   +"("+test.getDeclaringClass().getName()
                                   +"):\tCaused an ERROR");
                System.out.println(ex);
                ex.printStackTrace(System.out);
            }
        }
        if (fail != null)  throw fail;
    }

    public interface SimpleSuperInterface {
        public abstract int getInt();
        public static void printAll(String... args) {
            System.out.println(Arrays.toString(args));
        }
        public int NICE_CONSTANT = 42;
    }
    public interface SimpleInterface extends SimpleSuperInterface {
        default float getFloat() { return getInt(); }
        public static void printAll(String[] args) {
            System.out.println(Arrays.toString(args));
        }
    }
    public static class Simple implements SimpleInterface, Cloneable {
        public int intField;
        public final int finalField;
        private static String stringField;
        public int getInt() { return NICE_CONSTANT; }
        private static Number getNum() { return 804; }
        public Simple clone() {
            try {
                return (Simple) super.clone();
            } catch (CloneNotSupportedException ex) {
                throw new RuntimeException(ex);
            }
        }
        Simple() { finalField = -NICE_CONSTANT; }
        private static Lookup localLookup() { return lookup(); }
        private static List<Member> members() { return getMembers(lookup().lookupClass()); };
    }
    static class Nestmate {
        private static Lookup localLookup() { return lookup(); }
    }

    static boolean VERBOSE = false;

    @Test public void testSimple() throws Throwable {
        if (VERBOSE)  System.out.println("@Test testSimple");
        testOnMembers("testSimple", Simple.members(), Simple.localLookup());
    }
    @Test public void testPublicLookup() throws Throwable {
        if (VERBOSE)  System.out.println("@Test testPublicLookup");
        List<Member> mems = publicOnly(Simple.members());
        Lookup pubLookup = publicLookup(), privLookup = Simple.localLookup();
        testOnMembers("testPublicLookup/1", mems, pubLookup);
        // reveal using publicLookup:
        testOnMembers("testPublicLookup/2", mems, privLookup, pubLookup);
        // lookup using publicLookup, but reveal using private:
        testOnMembers("testPublicLookup/3", mems, pubLookup, privLookup);
    }
    @Test public void testPublicLookupNegative() throws Throwable {
        if (VERBOSE)  System.out.println("@Test testPublicLookupNegative");
        List<Member> mems = nonPublicOnly(Simple.members());
        Lookup pubLookup = publicLookup(), privLookup = Simple.localLookup();
        testOnMembersNoLookup("testPublicLookupNegative/1", mems, pubLookup);
        testOnMembersNoReveal("testPublicLookupNegative/2", mems, privLookup, pubLookup);
        testOnMembersNoReflect("testPublicLookupNegative/3", mems, privLookup, pubLookup);
    }
    @Test public void testJavaLangClass() throws Throwable {
        if (VERBOSE)  System.out.println("@Test testJavaLangClass");
        List<Member> mems = callerSensitive(false, publicOnly(getMembers(Class.class)));
        mems = limit(20, mems);
        testOnMembers("testJavaLangClass", mems, Simple.localLookup());
    }
    @Test public void testCallerSensitive() throws Throwable {
        if (VERBOSE)  System.out.println("@Test testCallerSensitive");
        List<Member> mems = union(getMembers(MethodHandles.class, "lookup"),
                                  getMembers(Method.class, "invoke"),
                                  getMembers(Field.class, "get", "set", "getLong"),
                                  getMembers(Class.class));
        mems = callerSensitive(true, publicOnly(mems));
        mems = limit(10, mems);
        testOnMembers("testCallerSensitive", mems, Simple.localLookup());
    }
    @Test public void testCallerSensitiveNegative() throws Throwable {
        if (VERBOSE)  System.out.println("@Test testCallerSensitiveNegative");
        List<Member> mems = union(getMembers(MethodHandles.class, "lookup"),
                                  getMembers(Class.class, "forName"),
                                  getMembers(Method.class, "invoke"));
        mems = callerSensitive(true, publicOnly(mems));
        // CS methods cannot be looked up with publicLookup
        testOnMembersNoLookup("testCallerSensitiveNegative/1", mems, publicLookup());
        // CS methods have to be revealed with a matching lookupClass
        testOnMembersNoReveal("testCallerSensitiveNegative/2", mems, Simple.localLookup(), publicLookup());
        testOnMembersNoReveal("testCallerSensitiveNegative/3", mems, Simple.localLookup(), Nestmate.localLookup());
        // CS methods have to have original access
        Lookup lookup = Simple.localLookup().dropLookupMode(Lookup.ORIGINAL);
        testOnMembersNoLookup("testCallerSensitiveNegative/4", mems, lookup);
        testOnMembersNoReveal("testCallerSensitiveNegative/5", mems, Simple.localLookup(), lookup);
    }
    @Test public void testMethodHandleNatives() throws Throwable {
        if (VERBOSE)  System.out.println("@Test testMethodHandleNatives");
        List<Member> mems = getMembers(MethodHandle.class, "invoke", "invokeExact");
        testOnMembers("testMethodHandleNatives", mems, Simple.localLookup());
    }
    @Test public void testMethodHandleInvokes() throws Throwable {
        if (VERBOSE)  System.out.println("@Test testMethodHandleInvokes");
        List<MethodType> types = new ArrayList<>();
        Class<?>[] someParamTypes = { void.class, int.class, Object.class, Object[].class };
        for (Class<?> rt : someParamTypes) {
            for (Class<?> p0 : someParamTypes) {
                if (p0 == void.class) { types.add(methodType(rt)); continue; }
                for (Class<?> p1 : someParamTypes) {
                    if (p1 == void.class) { types.add(methodType(rt, p0)); continue; }
                    for (Class<?> p2 : someParamTypes) {
                        if (p2 == void.class) { types.add(methodType(rt, p0, p1)); continue; }
                        types.add(methodType(rt, p0, p1, p2));
                    }
                }
            }
        }
        List<Member> mems = union(getPolyMembers(MethodHandle.class, "invoke", types),
                                  getPolyMembers(MethodHandle.class, "invokeExact", types));
        testOnMembers("testMethodHandleInvokes/1", mems, Simple.localLookup());
        testOnMembers("testMethodHandleInvokes/2", mems, publicLookup());
    }

    static List<Member> getPolyMembers(Class<?> cls, String name, List<MethodType> types) {
        assert(cls == MethodHandle.class);
        ArrayList<Member> mems = new ArrayList<>();
        for (MethodType type : types) {
            mems.add(new SignaturePolymorphicMethod(name, type));
        }
        return mems;
    }
    static List<Member> getMembers(Class<?> cls) {
        return getMembers(cls, (String[]) null);
    }
    static List<Member> getMembers(Class<?> cls, String... onlyNames) {
        List<String> names = (onlyNames == null || onlyNames.length == 0 ? null : Arrays.asList(onlyNames));
        ArrayList<Member> res = new ArrayList<>();
        for (Class<?> sup : getSupers(cls)) {
            res.addAll(getDeclaredMembers(sup, "getDeclaredFields"));
            res.addAll(getDeclaredMembers(sup, "getDeclaredMethods"));
            res.addAll(getDeclaredMembers(sup, "getDeclaredConstructors"));
        }
        res = new ArrayList<>(new LinkedHashSet<>(res));
        for (int i = 0; i < res.size(); i++) {
            Member mem = res.get(i);
            if (!canBeReached(mem, cls) ||
                res.indexOf(mem) != i ||
                mem.isSynthetic() ||
                (names != null && !names.contains(mem.getName()))
                ) {
                res.remove(i--);
            }
        }
        return res;
    }
    static List<Class<?>> getSupers(Class<?> cls) {
        ArrayList<Class<?>> res = new ArrayList<>();
        ArrayList<Class<?>> intfs = new ArrayList<>();
        for (Class<?> sup = cls; sup != null; sup = sup.getSuperclass()) {
            res.add(sup);
            for (Class<?> intf : cls.getInterfaces()) {
                if (!intfs.contains(intf))
                    intfs.add(intf);
            }
        }
        for (int i = 0; i < intfs.size(); i++) {
            for (Class<?> intf : intfs.get(i).getInterfaces()) {
                if (!intfs.contains(intf))
                    intfs.add(intf);
            }
        }
        res.addAll(intfs);
        //System.out.println("getSupers => "+res);
        return res;
    }
    static boolean hasSM() {
        return (System.getSecurityManager() != null);
    }
    static List<Member> getDeclaredMembers(Class<?> cls, String accessor) {
        Member[] mems = {};
        Method getter = getMethod(Class.class, accessor);
        if (hasSM()) {
            try {
                mems = (Member[]) invokeMethod(getter, cls);
            } catch (SecurityException ex) {
                //if (VERBOSE)  ex.printStackTrace();
                accessor = accessor.replace("Declared", "");
                getter = getMethod(Class.class, accessor);
                if (VERBOSE)  System.out.println("replaced accessor: "+getter);
            }
        }
        if (mems.length == 0) {
            try {
                mems = (Member[]) invokeMethod(getter, cls);
            } catch (SecurityException ex) {
                ex.printStackTrace();
            }
        }
        if (VERBOSE)  System.out.println(accessor+" "+cls.getName()+" => "+mems.length+" members");
        return Arrays.asList(mems);
    }
    static Method getMethod(Class<?> cls, String name) {
        try {
            return cls.getMethod(name);
        } catch (ReflectiveOperationException ex) {
            throw new AssertionError(ex);
        }
    }
    static Object invokeMethod(Method m, Object recv, Object... args) {
        try {
            return m.invoke(recv, args);
        } catch (InvocationTargetException ex) {
            Throwable ex2 = ex.getCause();
            if (ex2 instanceof RuntimeException)  throw (RuntimeException) ex2;
            if (ex2 instanceof Error)  throw (Error) ex2;
            throw new AssertionError(ex);
        } catch (ReflectiveOperationException ex) {
            throw new AssertionError(ex);
        }
    }

    static List<Member> limit(int len, List<Member> mems) {
        if (mems.size() <= len)  return mems;
        return mems.subList(0, len);
    }
    @SafeVarargs
    static List<Member> union(List<Member> mems, List<Member>... mem2s) {
        for (List<Member> mem2 : mem2s) {
            for (Member m : mem2) {
                if (!mems.contains(m))
                    mems.add(m);
            }
        }
        return mems;
    }
    static List<Member> callerSensitive(boolean cond, List<Member> members) {
        for (Iterator<Member> i = members.iterator(); i.hasNext(); ) {
            Member mem = i.next();
            if (isCallerSensitive(mem) != cond)
                i.remove();
        }
        if (members.isEmpty())  throw new AssertionError("trivial result");
        return members;
    }
    static boolean isCallerSensitive(Member mem) {
        if (!(mem instanceof AnnotatedElement))  return false;
        AnnotatedElement ae = (AnnotatedElement) mem;
        if (CS_CLASS != null)
            return ae.isAnnotationPresent(jdk.internal.reflect.CallerSensitive.class);
        for (java.lang.annotation.Annotation a : ae.getDeclaredAnnotations()) {
            if (a.toString().contains(".CallerSensitive"))
                return true;
        }
        return false;
    }
    static final Class<?> CS_CLASS;
    static {
        Class<?> c = null;
        try {
            c = jdk.internal.reflect.CallerSensitive.class;
        } catch (SecurityException | LinkageError ex) {
        }
        CS_CLASS = c;
    }
    static List<Member> publicOnly(List<Member> members) {
        return removeMods(members, Modifier.PUBLIC, 0);
    }
    static List<Member> nonPublicOnly(List<Member> members) {
        return removeMods(members, Modifier.PUBLIC, -1);
    }
    static List<Member> removeMods(List<Member> members, int mask, int bits) {
        int publicMods = (mask & Modifier.PUBLIC);
        members = new ArrayList<>(members);
        for (Iterator<Member> i = members.iterator(); i.hasNext(); ) {
            Member mem = i.next();
            int mods = mem.getModifiers();
            if ((publicMods & mods) != 0 &&
                (publicMods & mem.getDeclaringClass().getModifiers()) == 0)
                mods -= publicMods;
            if ((mods & mask) == (bits & mask))
                i.remove();
        }
        return members;
    }

    void testOnMembers(String tname, List<Member> mems, Lookup lookup, Lookup... lookups) throws Throwable {
        if (VERBOSE)  System.out.println("testOnMembers "+mems);
        Lookup revLookup = (lookups.length > 0) ? lookups[0] : null;
        if (revLookup == null)  revLookup = lookup;
        Lookup refLookup = (lookups.length > 1) ? lookups[1] : null;
        if (refLookup == null)  refLookup = lookup;
        assert(lookups.length <= 2);
        testOnMembersImpl(tname, mems, lookup, revLookup, refLookup, NO_FAIL);
    }
    void testOnMembersNoLookup(String tname, List<Member> mems, Lookup lookup) throws Throwable {
        if (VERBOSE)  System.out.println("testOnMembersNoLookup "+mems);
        testOnMembersImpl(tname, mems, lookup, null, null, FAIL_LOOKUP);
    }
    void testOnMembersNoReveal(String tname, List<Member> mems,
                               Lookup lookup, Lookup negLookup) throws Throwable {
        if (VERBOSE)  System.out.println("testOnMembersNoReveal "+mems);
        testOnMembersImpl(tname, mems, lookup, negLookup, null, FAIL_REVEAL);
    }
    void testOnMembersNoReflect(String tname, List<Member> mems,
                                Lookup lookup, Lookup negLookup) throws Throwable {
        if (VERBOSE)  System.out.println("testOnMembersNoReflect "+mems);
        testOnMembersImpl(tname, mems, lookup, lookup, negLookup, FAIL_REFLECT);
    }
    void testOnMembersImpl(String tname, List<Member> mems,
                           Lookup lookup,
                           Lookup revLookup,
                           Lookup refLookup,
                           int failureMode) throws Throwable {
        Throwable fail = null;
        int failCount = 0;
        failureModeCounts = new int[FAIL_MODE_COUNT];
        long tm0 = System.currentTimeMillis();
        for (Member mem : mems) {
            try {
                testWithMember(mem, lookup, revLookup, refLookup, failureMode);
            } catch (Throwable ex) {
                if (fail == null)  fail = ex;
                if (++failCount > 10) { System.out.println("*** FAIL: too many failures"); break; }
                System.out.println("*** FAIL: "+mem+" => "+ex);
                if (VERBOSE)  ex.printStackTrace(System.out);
            }
        }
        long tm1 = System.currentTimeMillis();
        System.out.printf("@Test %s executed %s tests in %d ms",
                          tname, testKinds(failureModeCounts), (tm1-tm0)).println();
        if (fail != null)  throw fail;
    }
    static String testKinds(int[] modes) {
        int pos = modes[0], neg = -pos;
        for (int n : modes)  neg += n;
        if (neg == 0)  return pos + " positive";
        String negs = "";
        for (int n : modes)  negs += "/"+n;
        negs = negs.replaceFirst("/"+pos+"/", "");
        negs += " negative";
        if (pos == 0)  return negs;
        return pos + " positive, " + negs;
    }
    static class SignaturePolymorphicMethod implements Member {  // non-reflected instance of MH.invoke*
        final String name;
        final MethodType type;
        SignaturePolymorphicMethod(String name, MethodType type) {
            this.name = name;
            this.type = type;
        }
        public String toString() {
            String typeStr = type.toString();
            if (isVarArgs())  typeStr = typeStr.replaceFirst("\\[\\])$", "...)");
            return (Modifier.toString(getModifiers())
                    +typeStr.substring(0, typeStr.indexOf('('))+" "
                    +getDeclaringClass().getTypeName()+"."
                    +getName()+typeStr.substring(typeStr.indexOf('(')));
        }
        public boolean equals(Object x) {
            return (x instanceof SignaturePolymorphicMethod && equals((SignaturePolymorphicMethod)x));
        }
        public boolean equals(SignaturePolymorphicMethod that) {
            return this.name.equals(that.name) && this.type.equals(that.type);
        }
        public int hashCode() {
            return name.hashCode() * 31 + type.hashCode();
        }
        public Class<?> getDeclaringClass() { return MethodHandle.class; }
        public String getName() { return name; }
        public MethodType getMethodType() { return type; }
        public int getModifiers() { return Modifier.PUBLIC | Modifier.FINAL | Modifier.NATIVE | SYNTHETIC; }
        public boolean isVarArgs() { return Modifier.isTransient(getModifiers()); }
        public boolean isSynthetic() { return true; }
        public Class<?> getReturnType() { return type.returnType(); }
        public Class<?>[] getParameterTypes() { return type.parameterArray(); }
        static final int SYNTHETIC = 0x00001000;
    }
    static class UnreflectResult {  // a tuple
        final MethodHandle mh;
        final Throwable ex;
        final byte kind;
        final Member mem;
        final int var;
        UnreflectResult(MethodHandle mh, byte kind, Member mem, int var) {
            this.mh = mh;
            this.ex = null;
            this.kind = kind;
            this.mem = mem;
            this.var = var;
        }
        UnreflectResult(Throwable ex, byte kind, Member mem, int var) {
            this.mh = null;
            this.ex = ex;
            this.kind = kind;
            this.mem = mem;
            this.var = var;
        }
        public String toString() {
            return toInfoString()+"/v"+var;
        }
        public String toInfoString() {
            return String.format("%s %s.%s:%s", MethodHandleInfo.referenceKindToString(kind),
                                 mem.getDeclaringClass().getName(), name(mem), type(mem, kind));
        }
        static String name(Member mem) {
            if (mem instanceof Constructor)  return "<init>";
            return mem.getName();
        }
        static MethodType type(Member mem, byte kind) {
            if (mem instanceof Field) {
                Class<?> type = ((Field)mem).getType();
                if (kind == REF_putStatic || kind == REF_putField)
                    return methodType(void.class, type);
                return methodType(type);
            } else if (mem instanceof SignaturePolymorphicMethod) {
                return ((SignaturePolymorphicMethod)mem).getMethodType();
            }
            Class<?>[] params = ((Executable)mem).getParameterTypes();
            if (mem instanceof Constructor)
                return methodType(void.class, params);
            Class<?> type = ((Method)mem).getReturnType();
            return methodType(type, params);
        }
    }
    static UnreflectResult unreflectMember(Lookup lookup, Member mem, int variation) {
        byte[] refKind = {0};
        try {
            return unreflectMemberOrThrow(lookup, mem, variation, refKind);
        } catch (ReflectiveOperationException|SecurityException ex) {
            return new UnreflectResult(ex, refKind[0], mem, variation);
        }
    }
    static UnreflectResult unreflectMemberOrThrow(Lookup lookup, Member mem, int variation,
                                                  byte[] refKind) throws ReflectiveOperationException {
        Class<?> cls = lookup.lookupClass();
        Class<?> defc = mem.getDeclaringClass();
        String   name = mem.getName();
        int      mods = mem.getModifiers();
        boolean isStatic = Modifier.isStatic(mods);
        MethodHandle mh = null;
        byte kind = 0;
        if (mem instanceof Method) {
            Method m = (Method) mem;
            MethodType type = methodType(m.getReturnType(), m.getParameterTypes());
            boolean canBeSpecial = (!isStatic &&
                                    (lookup.lookupModes() & Modifier.PRIVATE) != 0 &&
                                    defc.isAssignableFrom(cls) &&
                                    (!defc.isInterface() || Arrays.asList(cls.getInterfaces()).contains(defc)));
            if (variation >= 2)
                kind = REF_invokeSpecial;
            else if (isStatic)
                kind = REF_invokeStatic;
            else if (defc.isInterface())
                kind = REF_invokeInterface;
            else
                kind = REF_invokeVirtual;
            refKind[0] = kind;
            switch (variation) {
            case 0:
                mh = lookup.unreflect(m);
                break;
            case 1:
                if (defc == MethodHandle.class &&
                    !isStatic &&
                    m.isVarArgs() &&
                    Modifier.isFinal(mods) &&
                    Modifier.isNative(mods)) {
                    break;
                }
                if (isStatic)
                    mh = lookup.findStatic(defc, name, type);
                else
                    mh = lookup.findVirtual(defc, name, type);
                break;
            case 2:
                if (!canBeSpecial)
                    break;
                mh = lookup.unreflectSpecial(m, lookup.lookupClass());
                break;
            case 3:
                if (!canBeSpecial)
                    break;
                mh = lookup.findSpecial(defc, name, type, lookup.lookupClass());
                break;
            }
        } else if (mem instanceof SignaturePolymorphicMethod) {
            SignaturePolymorphicMethod m = (SignaturePolymorphicMethod) mem;
            MethodType type = methodType(m.getReturnType(), m.getParameterTypes());
            kind = REF_invokeVirtual;
            refKind[0] = kind;
            switch (variation) {
            case 0:
                mh = lookup.findVirtual(defc, name, type);
                break;
            }
        } else if (mem instanceof Constructor) {
            name = "<init>";  // not used
            Constructor<?> m = (Constructor<?>) mem;
            MethodType type = methodType(void.class, m.getParameterTypes());
            kind = REF_newInvokeSpecial;
            refKind[0] = kind;
            switch (variation) {
            case 0:
                mh = lookup.unreflectConstructor(m);
                break;
            case 1:
                mh = lookup.findConstructor(defc, type);
                break;
            }
        } else if (mem instanceof Field) {
            Field m = (Field) mem;
            Class<?> type = m.getType();
            boolean canHaveSetter = !Modifier.isFinal(mods);
            if (variation >= 2)
                kind = (byte)(isStatic ? REF_putStatic : REF_putField);
            else
                kind = (byte)(isStatic ? REF_getStatic : REF_getField);
            refKind[0] = kind;
            switch (variation) {
            case 0:
                mh = lookup.unreflectGetter(m);
                break;
            case 1:
                if (isStatic)
                    mh = lookup.findStaticGetter(defc, name, type);
                else
                    mh = lookup.findGetter(defc, name, type);
                break;
            case 3:
                if (!canHaveSetter)
                    break;
                mh = lookup.unreflectSetter(m);
                break;
            case 2:
                if (!canHaveSetter)
                    break;
                if (isStatic)
                    mh = lookup.findStaticSetter(defc, name, type);
                else
                    mh = lookup.findSetter(defc, name, type);
                break;
            }
        } else {
            throw new IllegalArgumentException(String.valueOf(mem));
        }
        if (mh == null)
            // ran out of valid variations; return null to caller
            return null;
        return new UnreflectResult(mh, kind, mem, variation);
    }
    static boolean canBeReached(Member mem, Class<?> cls) {
        Class<?> defc = mem.getDeclaringClass();
        String   name = mem.getName();
        int      mods = mem.getModifiers();
        if (mem instanceof Constructor) {
            name = "<init>";  // according to 292 spec.
        }
        if (defc == cls)
            return true;
        if (name.startsWith("<"))
            return false;  // only my own constructors
        if (Modifier.isPrivate(mods))
            return false;  // only my own constructors
        if (defc.getPackage() == cls.getPackage())
            return true;   // package access or greater OK
        if (Modifier.isPublic(mods))
            return true;   // publics always OK
        if (Modifier.isProtected(mods) && defc.isAssignableFrom(cls))
            return true;   // protected OK
        return false;
    }
    static boolean consistent(UnreflectResult res, MethodHandleInfo info) {
        assert(res.mh != null);
        assertEquals(res.kind, info.getReferenceKind());
        assertEquals(res.mem.getModifiers(), info.getModifiers());
        assertEquals(res.mem.getDeclaringClass(), info.getDeclaringClass());
        String expectName = res.mem.getName();
        if (res.kind == REF_newInvokeSpecial)
            expectName = "<init>";
        assertEquals(expectName, info.getName());
        MethodType expectType = res.mh.type();
        if ((res.kind & 1) == (REF_getField & 1))
            expectType = expectType.dropParameterTypes(0, 1);
        if (res.kind == REF_newInvokeSpecial)
            expectType = expectType.changeReturnType(void.class);
        assertEquals(expectType, info.getMethodType());
        assertEquals(res.mh.isVarargsCollector(), isVarArgs(info));
        assertEquals(res.toInfoString(), info.toString());
        assertEquals(res.toInfoString(), MethodHandleInfo.toString(info.getReferenceKind(), info.getDeclaringClass(), info.getName(), info.getMethodType()));
        return true;
    }
    static boolean isVarArgs(MethodHandleInfo info) {
        return info.isVarArgs();
    }
    static boolean consistent(Member mem, Member mem2) {
        assertEquals(mem, mem2);
        return true;
    }
    static boolean consistent(MethodHandleInfo info, MethodHandleInfo info2) {
        assertEquals(info.getReferenceKind(), info2.getReferenceKind());
        assertEquals(info.getModifiers(), info2.getModifiers());
        assertEquals(info.getDeclaringClass(), info2.getDeclaringClass());
        assertEquals(info.getName(), info2.getName());
        assertEquals(info.getMethodType(), info2.getMethodType());
        assertEquals(isVarArgs(info), isVarArgs(info));
        return true;
    }
    static boolean consistent(MethodHandle mh, MethodHandle mh2) {
        assertEquals(mh.type(), mh2.type());
        assertEquals(mh.isVarargsCollector(), mh2.isVarargsCollector());
        return true;
    }
    int[] failureModeCounts;
    static final int NO_FAIL=0, FAIL_LOOKUP=1, FAIL_REVEAL=2, FAIL_REFLECT=3, FAIL_MODE_COUNT=4;
    void testWithMember(Member mem,
                        Lookup lookup,      // initial lookup of member => MH
                        Lookup revLookup,   // reveal MH => info
                        Lookup refLookup,   // reflect info => member
                        int failureMode) throws Throwable {
        boolean expectEx1 = (failureMode == FAIL_LOOKUP);   // testOnMembersNoLookup
        boolean expectEx2 = (failureMode == FAIL_REVEAL);   // testOnMembersNoReveal
        boolean expectEx3 = (failureMode == FAIL_REFLECT);  // testOnMembersNoReflect
        for (int variation = 0; ; variation++) {
            UnreflectResult res = unreflectMember(lookup, mem, variation);
            failureModeCounts[failureMode] += 1;
            if (variation == 0)  assert(res != null);
            if (res == null)  break;
            if (VERBOSE && variation == 0)
                System.out.println("from "+mem.getDeclaringClass().getSimpleName());
            MethodHandle mh = res.mh;
            Throwable   ex1 = res.ex;
            if (VERBOSE)  System.out.println("  "+variation+": "+res+"  << "+(mh != null ? mh : ex1));
            if (expectEx1 && ex1 != null)
                continue;  // this is OK; we expected that lookup to fail
            if (expectEx1)
                throw new AssertionError("unexpected lookup for negative test");
            if (ex1 != null && !expectEx1) {
                if (failureMode != NO_FAIL)
                    throw new AssertionError("unexpected lookup failure for negative test", ex1);
                throw ex1;
            }
            MethodHandleInfo info;
            try {
                info = revLookup.revealDirect(mh);
                if (expectEx2)  throw new AssertionError("unexpected revelation for negative test");
            } catch (IllegalArgumentException|SecurityException ex2) {
                if (VERBOSE)  System.out.println("  "+variation+": "+res+" => "+mh.getClass().getName()+" => (EX2)"+ex2);
                if (expectEx2)
                    continue;  // this is OK; we expected the reflect to fail
                if (failureMode != NO_FAIL)
                    throw new AssertionError("unexpected revelation failure for negative test", ex2);
                throw ex2;
            }
            assert(consistent(res, info));
            Member mem2;
            try {
                mem2 = info.reflectAs(Member.class, refLookup);
                if (expectEx3)  throw new AssertionError("unexpected reflection for negative test");
                assert(!(mem instanceof SignaturePolymorphicMethod));
            } catch (IllegalArgumentException ex3) {
                if (VERBOSE)  System.out.println("  "+variation+": "+info+" => (EX3)"+ex3);
                if (expectEx3)
                    continue;  // this is OK; we expected the reflect to fail
                if (mem instanceof SignaturePolymorphicMethod)
                    continue;  // this is OK; we cannot reflect MH.invokeExact(a,b,c)
                if (failureMode != NO_FAIL)
                    throw new AssertionError("unexpected reflection failure for negative test", ex3);
                throw ex3;
            }
            assert(consistent(mem, mem2));
            UnreflectResult res2 = unreflectMember(lookup, mem2, variation);
            MethodHandle mh2 = res2.mh;
            assert(consistent(mh, mh2));
            MethodHandleInfo info2 = lookup.revealDirect(mh2);
            assert(consistent(info, info2));
            assert(consistent(res, info2));
            Member mem3;
            if (hasSM())
                mem3 = info2.reflectAs(Member.class, lookup);
            else
                mem3 = MethodHandles.reflectAs(Member.class, mh2);
            assert(consistent(mem2, mem3));
            if (hasSM()) {
                try {
                    MethodHandles.reflectAs(Member.class, mh2);
                    throw new AssertionError("failed to throw on "+mem3);
                } catch (SecurityException ex3) {
                    // OK...
                }
            }
        }
    }
}
