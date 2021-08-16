/*
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
 * This file is available under and governed by the GNU General Public
 * License version 2 only, as published by the Free Software Foundation.
 * However, the following notice accompanied the original version of this
 * file:
 *
 * Written by Martin Buchholz with assistance from members of JCP
 * JSR-166 Expert Group and released to the public domain, as
 * explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

/*
 * @test
 * @bug 8054446 8137184 8137185
 * @modules java.base/java.util.concurrent:open
 *          java.base/java.util.concurrent.locks:open
 * @summary Regression test for memory leak in remove(Object)
 */

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.PriorityBlockingQueue;

public class RemoveLeak {
    public static void main(String[] args) throws Throwable {
        new RemoveLeak().main();
    }

    void main() throws Throwable {
        test(new ConcurrentLinkedDeque());
        test(new ConcurrentLinkedQueue());
        test(new LinkedBlockingDeque(10));
        test(new LinkedBlockingQueue(10));
        test(new LinkedTransferQueue());
        test(new ArrayBlockingQueue(10));
        test(new PriorityBlockingQueue());
    }

    void test(Collection c) throws Throwable {
        assertNoLeak(c, () -> addRemove(c));

        // A demo that the leak detection infrastructure works
        // assertNoLeak(c, () -> c.add(1));
        // printRetainedObjects(c);
    }

    static void addRemove(Collection c) {
        c.add(1);
        c.remove(1);
    }

    // -------- leak detection infrastructure ---------------
    void assertNoLeak(Object root, Runnable r) {
        int prev = retainedObjects(root).size();
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) r.run();
            int next = retainedObjects(root).size();
            if (next <= prev)
                return;
            prev = next;
        }
        throw new AssertionError(
            String.format("probable memory leak in %s: %s",
                          root.getClass().getSimpleName(), root));
    }

    ConcurrentHashMap<Class<?>, Collection<Field>> classFields
        = new ConcurrentHashMap<>();

    Collection<Field> referenceFieldsOf(Class<?> k) {
        Collection<Field> fields = classFields.get(k);
        if (fields == null) {
            fields = new ArrayDeque<Field>();
            ArrayDeque<Field> allFields = new ArrayDeque<>();
            for (Class<?> c = k; c != null; c = c.getSuperclass())
                for (Field field : c.getDeclaredFields())
                    if (!Modifier.isStatic(field.getModifiers())
                        && !field.getType().isPrimitive())
                        fields.add(field);
            AccessibleObject.setAccessible(
                fields.toArray(new AccessibleObject[0]), true);
            classFields.put(k, fields);
        }
        return fields;
    }

    static Object get(Field field, Object x) {
        try { return field.get(x); }
        catch (IllegalAccessException ex) { throw new AssertionError(ex); }
    }

    Set<Object> retainedObjects(Object x) {
        ArrayDeque<Object> todo = new ArrayDeque<>() {
            public void push(Object x) { if (x != null) super.push(x); }};
        Set<Object> uniqueObjects = Collections.newSetFromMap(
            new IdentityHashMap<Object, Boolean>());
        todo.push(x);
        while (!todo.isEmpty()) {
            Object y = todo.pop();
            if (uniqueObjects.contains(y))
                continue;
            uniqueObjects.add(y);
            Class<?> k = y.getClass();
            if (k.isArray() && !k.getComponentType().isPrimitive()) {
                for (int i = 0, len = Array.getLength(y); i < len; i++)
                    todo.push(Array.get(y, i));
            } else {
                for (Field field : referenceFieldsOf(k))
                    todo.push(get(field, y));
            }
        }
        return uniqueObjects;
    }

    /** for debugging the retained object graph */
    void printRetainedObjects(Object x) {
        for (Object y : retainedObjects(x))
            System.out.printf("%s : %s%n", y.getClass().getSimpleName(), y);
    }
}
