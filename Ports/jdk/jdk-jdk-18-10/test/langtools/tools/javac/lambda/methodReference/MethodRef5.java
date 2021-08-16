/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 8003280
 * @summary Add lambda tests
 *   Test method reference with SAM interface Comparator<T>
 * @compile MethodRef5.java
 * @run main MethodRef5
 */

import java.util.Collections;
import java.util.List;
import java.util.ArrayList;

public class MethodRef5 {

    static class Person {

        private String firstName;
        private String lastName;
        private int age;

        public Person() { }

        public Person(String fn, String ln, int a) {
            firstName = fn;
            lastName = ln;
            age = a;
        }

        public String getLastName() {
            return lastName;
        }

        public int getAge() {
            return age;
        }

        //the following 2 methods are signature-compatible with Comparator<Person>.compare():
        public static int compareByAge(Person a, Person b) {
            return a.age - b.age;
        }

        public int compareByLastName(Person a, Person b) {
            return a.lastName.compareToIgnoreCase(b.lastName);
        }
    }

    private static void assertTrue(boolean cond) {
        if (!cond)
            throw new AssertionError();
    }

    public static void main(String[] args) {

        List<Person> persons = new ArrayList<Person>();
        persons.add(new Person("John", "Smith", 49));
        persons.add(new Person("Abraham", "Lincoln", 30));
        persons.add(new Person("George", "Washington", 29));
        persons.add(new Person("Peter", "Derby", 50));
        Collections.sort(persons, Person::compareByAge);//static method reference to compareByAge(Person, Person)
        String age = "";
        for (Person p : persons) {
            age += p.getAge() + " ";
        }
        assertTrue( (age.equals("29 30 49 50 ")) );
        Collections.sort(persons, new Person()::compareByLastName);//instance method reference to compareByLastName(Person, Person)
        String lastName = "";
        for (Person p : persons) {
            lastName += p.getLastName() + " ";
        }
        assertTrue( lastName.equals("Derby Lincoln Smith Washington ") );
    }
}
