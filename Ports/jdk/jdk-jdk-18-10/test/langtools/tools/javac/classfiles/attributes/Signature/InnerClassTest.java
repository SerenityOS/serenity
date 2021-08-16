/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8049238
 * @summary Checks Signature attribute for inner classes.
 * @library /tools/lib /tools/javac/lib ../lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.jdeps/com.sun.tools.classfile
 * @build toolbox.ToolBox InMemoryFileManager TestResult TestBase
 * @build InnerClassTest Driver ExpectedSignature ExpectedSignatureContainer
 * @run main Driver InnerClassTest
 */

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

@ExpectedSignature(descriptor = "InnerClassTest",
        signature = "<T:Ljava/util/ArrayList<TT;>;:Ljava/lang/Runnable;>Ljava/lang/Object;")
@ExpectedSignature(descriptor = "InnerClassTest$1",
        signature = "LInnerClassTest$1Local1;", isAnonymous = true)
@ExpectedSignature(descriptor = "InnerClassTest$2",
        signature = "LInnerClassTest$1Local2<Ljava/util/ArrayList<TT;>;" +
                "Ljava/util/Map<Ljava/util/ArrayList<TT;>;Ljava/util/ArrayList<TT;>;>;>;", isAnonymous = true)
public class InnerClassTest<T extends ArrayList<T> & Runnable> {

    {
        class Local1 {
            // no Signature attribute
            Local1() {
            }

            @ExpectedSignature(descriptor = "genericType", signature = "TT;")
            T genericType;

            @ExpectedSignature(descriptor = "genericTypeArray", signature = "[TT;")
            T[] genericTypeArray;
        }

        @ExpectedSignature(descriptor = "InnerClassTest$1Local2",
                signature = "<T:Ljava/lang/Object;U::Ljava/util/Map<+TT;-TT;>;>Ljava/lang/Object;")
        class Local2<T, U extends Map<? extends T, ? super T>> {
            // no Signature attribute
            Local2() {
            }

            @ExpectedSignature(descriptor = "<init>(InnerClassTest, java.lang.Object, java.util.Map)",
                    signature = "(TT;TU;)V")
            Local2(T a, U b) {
            }

            @ExpectedSignature(descriptor = "genericMethod(java.lang.Object[])",
                    signature = "([TT;)[TU;")
            U[] genericMethod(T...a) {
                return null;
            }
        }

        @ExpectedSignature(descriptor = "InnerClassTest$1Local3",
                signature = "LInnerClassTest$1Local2<Ljava/util/ArrayList<TT;>;" +
                        "Ljava/util/Map<Ljava/util/ArrayList<TT;>;Ljava/util/ArrayList<TT;>;>;>;")
        class Local3 extends Local2<ArrayList<T>, Map<ArrayList<T>, ArrayList<T>>> {
        }

        new Local1() {
            @ExpectedSignature(descriptor = "genericType", signature = "TT;")
            T genericType;

            @ExpectedSignature(descriptor = "genericTypeArray", signature = "[TT;")
            T[] genericTypeArray;
        };

        new Local2<ArrayList<T>, Map<ArrayList<T>, ArrayList<T>>>() {
        };
    }

    @ExpectedSignature(descriptor = "InnerClassTest$InnerClass1",
            signature = "<E:TT;U::Ljava/util/Set<-TE;>;>Ljava/lang/Object;")
    class InnerClass1<E extends T, U extends Set<? super E>> {
        @ExpectedSignature(descriptor = "genericTypeArray", signature = "[TT;")
        T[] genericTypeArray;

        @ExpectedSignature(descriptor = "genericListExtendsBound", signature = "Ljava/util/List<+TT;>;")
        List<? extends T> genericListExtendsBound;

        @ExpectedSignature(descriptor = "genericListSuperBound", signature = "Ljava/util/List<-TU;>;")
        List<? super U> genericListSuperBound;

        @ExpectedSignature(descriptor = "genericListWildCard", signature = "Ljava/util/List<*>;")
        List<?> genericListWildCard;

        @ExpectedSignature(descriptor = "genericListExactType", signature = "Ljava/util/List<Ljava/lang/Integer;>;")
        List<Integer> genericListExactType;

        @ExpectedSignature(descriptor = "listWithGenericType", signature = "Ljava/util/List<TE;>;")
        List<E> listWithGenericType;

        List listNoSignatureAttribute;

        // no Signature attribute
        InnerClass1(List a) {
        }

        @ExpectedSignature(descriptor = "<init>(InnerClassTest, java.util.ArrayList)",
                signature = "(TT;)V")
        InnerClass1(T a) {
        }

        @ExpectedSignature(descriptor = "<init>(InnerClassTest, java.util.ArrayList, java.util.ArrayList)",
                signature = "(TT;TE;)V")
        InnerClass1(T a, E b) {
        }

        @ExpectedSignature(descriptor = "genericMethod(java.util.ArrayList)",
                signature = "(TT;)TE;")
        E genericMethod(T a) {
            return null;
        }
    }

    @ExpectedSignature(descriptor = "InnerClassTest$InnerInterface",
            signature = "<T:Ljava/lang/Object;>Ljava/lang/Object;")
    interface InnerInterface<T> {
        @ExpectedSignature(descriptor = "genericMethod(java.lang.Object)", signature = "(TT;)TT;")
        T genericMethod(T a);

        @ExpectedSignature(descriptor = "genericListExtendsBound", signature = "Ljava/util/List<+Ljava/lang/Number;>;")
        List<? extends Number> genericListExtendsBound = null;

        @ExpectedSignature(descriptor = "genericListSuperBound", signature = "Ljava/util/List<-Ljava/lang/Number;>;")
        List<? super Number> genericListSuperBound = null;

        @ExpectedSignature(descriptor = "genericListWildCard", signature = "Ljava/util/List<*>;")
        List<?> genericListWildCard = null;

        @ExpectedSignature(descriptor = "genericListExactType", signature = "Ljava/util/List<Ljava/lang/Integer;>;")
        List<Integer> genericListExactType = null;

        List listNoSignatureAttribute = null;

        @ExpectedSignature(descriptor = "genericBoundsMethod1(java.util.List)",
                signature = "(Ljava/util/List<-TT;>;)Ljava/util/List<+TT;>;")
        List<? extends T> genericBoundsMethod1(List<? super T> a);

        @ExpectedSignature(descriptor = "genericBoundsMethod2(java.util.List)",
                signature = "(Ljava/util/List<+TT;>;)Ljava/util/List<-TT;>;")
        List<? super T> genericBoundsMethod2(List<? extends T> a);

        @ExpectedSignature(descriptor = "genericWildCardMethod(java.util.Map)",
                signature = "(Ljava/util/Map<**>;)Ljava/util/Map<**>;")
        Map<?, ?> genericWildCardMethod(Map<?, ?> a);

        @ExpectedSignature(descriptor = "defaultGenericMethod(java.util.List, java.util.List, java.util.Map)",
                signature = "(Ljava/util/List<+TT;>;Ljava/util/List<-TT;>;Ljava/util/Map<**>;)Ljava/util/List<*>;")
        default List<?> defaultGenericMethod(List<? extends T> list1, List<? super T> list2, Map<?, ?> map) { return null; }


        default List defaultNoSignatureAttributeMethod(List list1, List list2, Map list3) { return null; }

        @ExpectedSignature(descriptor = "staticGenericMethod(java.util.List, java.util.List, java.util.Map)",
                signature = "<T::Ljava/lang/Runnable;>(Ljava/util/List<+TT;>;Ljava/util/List<-TT;>;Ljava/util/Map<**>;)Ljava/util/List<*>;")
        static <T extends Runnable> List<?> staticGenericMethod(List<? extends T> list1, List<? super T> list2, Map<?, ?> map) { return null; }


        static List staticNoSignatureAttributeMethod(List list1, List list2, Map list3) { return null; }
    }

    @ExpectedSignature(descriptor = "InnerClassTest$InnerClass2",
            signature = "LInnerClassTest<TT;>.InnerClass1<TT;Ljava/util/Set<TT;>;>;LInnerClassTest$InnerInterface<TT;>;")
    class InnerClass2 extends InnerClass1<T, Set<T>> implements InnerInterface<T> {

        // no Signature attribute
        InnerClass2() {
            super(null);
        }

        @ExpectedSignature(descriptor = "<init>(InnerClassTest, java.util.ArrayList)",
                signature = "(TT;)V")
        InnerClass2(T a) {
            super(a);
        }

        @ExpectedSignature(descriptor = "<init>(InnerClassTest, java.util.ArrayList, java.util.ArrayList)",
                signature = "(TT;TT;)V")
        InnerClass2(T a, T b) {
            super(a, b);
        }

        @ExpectedSignature(descriptor = "genericMethod(java.util.ArrayList)", signature = "(TT;)TT;")
        @Override
        public T genericMethod(T a) {
            return null;
        }

        @ExpectedSignature(descriptor = "genericBoundsMethod1(java.util.List)",
                signature = "(Ljava/util/List<-TT;>;)Ljava/util/List<+TT;>;")
        @Override
        public List<? extends T> genericBoundsMethod1(List<? super T> a) {
            return null;
        }

        @ExpectedSignature(descriptor = "genericBoundsMethod2(java.util.List)",
                signature = "(Ljava/util/List<+TT;>;)Ljava/util/List<-TT;>;")
        @Override
        public List<? super T> genericBoundsMethod2(List<? extends T> a) {
            return null;
        }

        @ExpectedSignature(descriptor = "genericWildCardMethod(java.util.Map)",
                signature = "(Ljava/util/Map<**>;)Ljava/util/Map<**>;")
        @Override
        public Map<?, ?> genericWildCardMethod(Map<?, ?> a) {
            return null;
        }
    }

    @ExpectedSignature(descriptor = "InnerClassTest$StaticInnerClass",
            signature = "<T:Ljava/lang/String;E::Ljava/util/Set<TT;>;>" +
                    "Ljava/lang/Object;LInnerClassTest$InnerInterface<TE;>;")
    static class StaticInnerClass<T extends String, E extends Set<T>> implements InnerInterface<E> {
        // no Signature attribute
        StaticInnerClass(List a) {
        }

        @ExpectedSignature(descriptor = "<init>(java.lang.Runnable)",
                signature = "<E::Ljava/lang/Runnable;>(TE;)V")
        <E extends Runnable> StaticInnerClass(E a) {
        }

        @ExpectedSignature(descriptor = "<init>(java.lang.String)",
                signature = "(TT;)V")
        StaticInnerClass(T a) {
        }

        @ExpectedSignature(descriptor = "<init>(java.lang.String, java.util.Set)",
                signature = "(TT;TE;)V")
        StaticInnerClass(T a, E b) {
        }

        @ExpectedSignature(descriptor = "genericListExtendsBound", signature = "Ljava/util/List<+Ljava/lang/Number;>;")
        static List<? extends Number> genericListExtendsBound;

        @ExpectedSignature(descriptor = "genericListSuperBound", signature = "Ljava/util/List<-Ljava/lang/Number;>;")
        static List<? super Number> genericListSuperBound;

        @ExpectedSignature(descriptor = "genericListWildCard", signature = "Ljava/util/List<*>;")
        static List<?> genericListWildCard;

        @ExpectedSignature(descriptor = "genericListExactType", signature = "Ljava/util/List<Ljava/lang/Integer;>;")
        static List<Integer> genericListExactType;

        static List listNoSignatureAttribute;

        @ExpectedSignature(descriptor = "genericMethod(java.util.Set)",
                signature = "(TE;)TE;")
        @Override
        public E genericMethod(E a) {
            return null;
        }

        @ExpectedSignature(descriptor = "genericBoundsMethod1(java.util.List)",
                signature = "(Ljava/util/List<-TE;>;)Ljava/util/List<+TE;>;")
        @Override
        public List<? extends E> genericBoundsMethod1(List<? super E> a) {
            return null;
        }

        @ExpectedSignature(descriptor = "genericBoundsMethod2(java.util.List)",
                signature = "(Ljava/util/List<+TE;>;)Ljava/util/List<-TE;>;")
        @Override
        public List<? super E> genericBoundsMethod2(List<? extends E> a) {
            return null;
        }

        @ExpectedSignature(descriptor = "genericWildCardMethod(java.util.Map)",
                signature = "(Ljava/util/Map<**>;)Ljava/util/Map<**>;")
        @Override
        public Map<?, ?> genericWildCardMethod(Map<?, ?> a) {
            return null;
        }

        @ExpectedSignature(descriptor = "staticGenericMethod(java.lang.Object)",
                signature = "<E:Ljava/lang/Object;>(TE;)TE;")
        public static <E> E staticGenericMethod(E a) {
            return null;
        }

        @ExpectedSignature(descriptor = "staticGenericBoundsMethod1(java.util.List)",
                signature = "<E:Ljava/lang/Object;>(Ljava/util/List<-TE;>;)Ljava/util/List<+TE;>;")
        public static <E> List<? extends E> staticGenericBoundsMethod1(List<? super E> a) {
            return null;
        }

        @ExpectedSignature(descriptor = "staticGenericBoundsMethod2(java.util.List)",
                signature = "<E:Ljava/lang/Object;>(Ljava/util/List<+TE;>;)Ljava/util/List<-TE;>;")
        public static <E> List<? super E> staticGenericBoundsMethod2(List<? extends E> a) {
            return null;
        }

        @ExpectedSignature(descriptor = "staticGenericWildCardMethod(java.util.Map)",
                signature = "<E:Ljava/lang/Object;>(Ljava/util/Map<**>;)Ljava/util/Map<**>;")
        public static <E> Map<?, ?> staticGenericWildCardMethod(Map<?, ?> a) {
            return null;
        }
    }

    @ExpectedSignature(descriptor = "InnerClassTest$InnerClass3",
            signature = "Ljava/lang/Object;LInnerClassTest$ExceptionHolder" +
                    "<Ljava/lang/RuntimeException;>;Ljava/util/concurrent/Callable<Ljava/util/Map<**>;>;")
    public static class InnerClass3 implements  ExceptionHolder<RuntimeException>, Callable<Map<?, ?>> {
        @ExpectedSignature(descriptor = "call()", signature = "()Ljava/util/Map<**>;")
        @Override
        public Map<?, ?> call() throws Exception {
            return null;
        }

        @Override
        public void Throw() throws RuntimeException {
        }

        @Override
        public RuntimeException Return() {
            return null;
        }
    }

    /**
     * Class is for checking that the Signature attribute is not generated
     * for overridden methods despite of the appropriate methods in the parent class
     * have the Signature attribute.
     */
    @ExpectedSignature(descriptor = "InnerClassTest$ExceptionHolder",
            signature = "<E:Ljava/lang/Exception;>Ljava/lang/Object;")
    interface ExceptionHolder<E extends Exception> {
        @ExpectedSignature(descriptor = "Throw()", signature = "()V^TE;")
        void Throw() throws E;
        @ExpectedSignature(descriptor = "Return()", signature = "()TE;")
        E Return();
    }
}
