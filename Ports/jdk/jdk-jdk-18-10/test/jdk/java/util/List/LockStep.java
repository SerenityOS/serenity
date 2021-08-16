/*
 * Copyright (c) 2007, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     6359979
 * @summary Compare List implementations for identical behavior
 * @author  Martin Buchholz
 * @key randomness
 */

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Random;
import java.util.Vector;

@SuppressWarnings("unchecked")
public class LockStep {
    final int DEFAULT_SIZE = 5;
    int size;           // Running time is O(size**2)

    int intArg(String[] args, int i, int defaultValue) {
        return args.length > i ? Integer.parseInt(args[i]) : defaultValue;
    }

    boolean maybe(int n) { return rnd.nextInt(n) == 0; }

    void test(String[] args) {
        size = intArg(args, 0, DEFAULT_SIZE);

        lockSteps(new ArrayList(),
                  new LinkedList(),
                  new Vector());
    }

    void equalLists(List... lists) {
        for (List list : lists)
            equalLists(list, lists[0]);
    }

    void equalLists(List x, List y) {
        equal(x, y);
        equal(y, x);
        equal(x.size(),     y.size());
        equal(x.isEmpty(),  y.isEmpty());
        equal(x.hashCode(), y.hashCode());
        equal(x.toString(), y.toString());
        equal(x.toArray(),  y.toArray());
    }

    void lockSteps(List... lists) {
        for (int i = 0; i < lists.length; i++)
            if (maybe(4)) lists[i] = serialClone(lists[i]);
        for (final List list : lists)
            testEmptyList(list);
        for (int i = 0; i < size; i++) {
            ListFrobber adder = randomAdder();
            for (final List list : lists) {
                adder.frob(list);
                equal(list.size(), i+1);
            }
            equalLists(lists);
        }
        {
            final ListFrobber adder = randomAdder();
            final ListFrobber remover = randomRemover();
            for (final List list : lists) {

                THROWS(ConcurrentModificationException.class,
                       new F(){void f(){
                           Iterator it = list.iterator();
                           adder.frob(list);
                           it.next();}},
                       new F(){void f(){
                           Iterator it = asSubList(list).iterator();
                           remover.frob(list);
                           it.next();}},
                       new F(){void f(){
                           Iterator it = asSubList(asSubList(list)).iterator();
                           adder.frob(list);
                           it.next();}},
                       new F(){void f(){
                           List subList = asSubList(list);
                           remover.frob(list);
                           subList.get(0);}},
                       new F(){void f(){
                           List sl = asSubList(list);
                           List ssl = asSubList(sl);
                           adder.frob(sl);
                           ssl.get(0);}},
                       new F(){void f(){
                           List sl = asSubList(list);
                           List ssl = asSubList(sl);
                           remover.frob(sl);
                           ssl.get(0);}});
            }
        }

        for (final List l : lists) {
            final List sl = asSubList(l);
            final List ssl = asSubList(sl);
            ssl.add(0, 42);
            equal(ssl.get(0), 42);
            equal(sl.get(0), 42);
            equal(l.get(0), 42);
            final int s = l.size();
            final int rndIndex = rnd.nextInt(l.size());
            THROWS(IndexOutOfBoundsException.class,
                   new F(){void f(){l.subList(rndIndex, rndIndex).get(0);}},
                   new F(){void f(){l.subList(s/2, s).get(s/2 + 1);}},
                   new F(){void f(){l.subList(s/2, s).get(-1);}}
                   );
            THROWS(IllegalArgumentException.class,
                   new F(){void f(){  l.subList(1, 0);}},
                   new F(){void f(){ sl.subList(1, 0);}},
                   new F(){void f(){ssl.subList(1, 0);}});
        }

        equalLists(lists);

        for (final List list : lists) {
            equalLists(list, asSubList(list));
            equalLists(list, asSubList(asSubList(list)));
        }
        for (final List list : lists)
            System.out.println(list);

        for (int i = lists[0].size(); i > 0; i--) {
            ListFrobber remover = randomRemover();
            for (final List list : lists)
                remover.frob(list);
            equalLists(lists);
        }
    }

    <T> List<T> asSubList(List<T> list) {
        return list.subList(0, list.size());
    }

    void testEmptyCollection(Collection<?> c) {
        check(c.isEmpty());
        equal(c.size(), 0);
        equal(c.toString(),"[]");
        equal(c.toArray().length, 0);
        equal(c.toArray(new Object[0]).length, 0);

        Object[] a = new Object[1]; a[0] = Boolean.TRUE;
        equal(c.toArray(a), a);
        equal(a[0], null);
    }

    void testEmptyList(List list) {
        testEmptyCollection(list);
        equal(list.hashCode(), 1);
        equal(list, Collections.emptyList());
    }

    final Random rnd = new Random();

    abstract class ListFrobber { abstract void frob(List l); }

    ListFrobber randomAdder() {
        final Integer e = rnd.nextInt(1024);
        final int subListCount = rnd.nextInt(3);
        final boolean atBeginning = rnd.nextBoolean();
        final boolean useIterator = rnd.nextBoolean();
        final boolean simpleIterator = rnd.nextBoolean();
        return new ListFrobber() {void frob(List l) {
            final int s = l.size();
            List ll = l;
            for (int i = 0; i < subListCount; i++)
                ll = asSubList(ll);
            if (! useIterator) {
                if (atBeginning) {
                    switch (rnd.nextInt(3)) {
                    case 0: ll.add(0, e); break;
                    case 1: ll.subList(0, rnd.nextInt(s+1)).add(0, e); break;
                    case 2: ll.subList(0, rnd.nextInt(s+1)).subList(0,0).add(0,e); break;
                    default: throw new Error();
                    }
                } else {
                    switch (rnd.nextInt(3)) {
                    case 0: check(ll.add(e)); break;
                    case 1: ll.subList(s/2, s).add(s - s/2, e); break;
                    case 2: ll.subList(s, s).subList(0, 0).add(0, e); break;
                    default: throw new Error();
                    }
                }
            } else {
                if (atBeginning) {
                    ListIterator it = ll.listIterator();
                    equal(it.nextIndex(), 0);
                    check(! it.hasPrevious());
                    it.add(e);
                    equal(it.previousIndex(), 0);
                    equal(it.nextIndex(), 1);
                    check(it.hasPrevious());
                } else {
                    final int siz = ll.size();
                    ListIterator it = ll.listIterator(siz);
                    equal(it.previousIndex(), siz-1);
                    check(! it.hasNext());
                    it.add(e);
                    equal(it.previousIndex(), siz);
                    equal(it.nextIndex(), siz+1);
                    check(! it.hasNext());
                    check(it.hasPrevious());
                }
            }}};
    }

    ListFrobber randomRemover() {
        final int position = rnd.nextInt(3);
        final int subListCount = rnd.nextInt(3);
        return new ListFrobber() {void frob(List l) {
            final int s = l.size();
            List ll = l;
            for (int i = 0; i < subListCount; i++)
                ll = asSubList(ll);
            switch (position) {
            case 0: // beginning
                switch (rnd.nextInt(3)) {
                case 0: ll.remove(0); break;
                case 1: {
                    final Iterator it = ll.iterator();
                    check(it.hasNext());
                    THROWS(IllegalStateException.class,
                           new F(){void f(){it.remove();}});
                    it.next();
                    it.remove();
                    THROWS(IllegalStateException.class,
                           new F(){void f(){it.remove();}});
                    break;}
                case 2: {
                    final ListIterator it = ll.listIterator();
                    check(it.hasNext());
                    THROWS(IllegalStateException.class,
                           new F(){void f(){it.remove();}});
                    it.next();
                    it.remove();
                    THROWS(IllegalStateException.class,
                           new F(){void f(){it.remove();}});
                    break;}
                default: throw new Error();
                }
                break;
            case 1: // midpoint
                switch (rnd.nextInt(3)) {
                case 0: ll.remove(s/2); break;
                case 1: {
                    final ListIterator it = ll.listIterator(s/2);
                    it.next();
                    it.remove();
                    break;
                }
                case 2: {
                    final ListIterator it = ll.listIterator(s/2+1);
                    it.previous();
                    it.remove();
                    break;
                }
                default: throw new Error();
                }
                break;
            case 2: // end
                switch (rnd.nextInt(3)) {
                case 0: ll.remove(s-1); break;
                case 1: ll.subList(s-1, s).clear(); break;
                case 2:
                    final ListIterator it = ll.listIterator(s);
                    check(! it.hasNext());
                    check(it.hasPrevious());
                    THROWS(IllegalStateException.class,
                           new F(){void f(){it.remove();}});
                    it.previous();
                    equal(it.nextIndex(), s-1);
                    check(it.hasNext());
                    it.remove();
                    equal(it.nextIndex(), s-1);
                    check(! it.hasNext());
                    THROWS(IllegalStateException.class,
                           new F(){void f(){it.remove();}});
                    break;
                default: throw new Error();
                }
                break;
            default: throw new Error();
            }}};
    }

    //--------------------- Infrastructure ---------------------------
    volatile int passed = 0, failed = 0;
    void pass() {passed++;}
    void fail() {failed++; Thread.dumpStack();}
    void fail(String msg) {System.err.println(msg); fail();}
    void unexpected(Throwable t) {failed++; t.printStackTrace();}
    void check(boolean cond) {if (cond) pass(); else fail();}
    void equal(Object x, Object y) {
        if (x == null ? y == null : x.equals(y)) pass();
        else fail(x + " not equal to " + y);}
    <T> void equal(T[] x, T[] y) {check(Arrays.equals(x,y));}
    public static void main(String[] args) throws Throwable {
        new LockStep().instanceMain(args);}
    void instanceMain(String[] args) throws Throwable {
        try {test(args);} catch (Throwable t) {unexpected(t);}
        System.out.printf("%nPassed = %d, failed = %d%n%n", passed, failed);
        if (failed > 0) throw new AssertionError("Some tests failed");}
    abstract class F {abstract void f() throws Throwable;}
    void THROWS(Class<? extends Throwable> k, F... fs) {
        for (F f : fs)
            try {f.f(); fail("Expected " + k.getName() + " not thrown");}
            catch (Throwable t) {
                if (k.isAssignableFrom(t.getClass())) pass();
                else unexpected(t);}}
    static byte[] serializedForm(Object obj) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            new ObjectOutputStream(baos).writeObject(obj);
            return baos.toByteArray();
        } catch (IOException e) { throw new RuntimeException(e); }}
    static Object readObject(byte[] bytes)
        throws IOException, ClassNotFoundException {
        InputStream is = new ByteArrayInputStream(bytes);
        return new ObjectInputStream(is).readObject();}
    @SuppressWarnings("unchecked")
    static <T> T serialClone(T obj) {
        try { return (T) readObject(serializedForm(obj)); }
        catch (Exception e) { throw new RuntimeException(e); }}
}
