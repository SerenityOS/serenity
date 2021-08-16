/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Comparator API narrowing type test
 * @bug 8009736 8033590
 * @run testng TypeTest
 */

import java.util.function.Function;
import java.util.Map;
import java.util.TreeMap;
import java.util.Comparator;
import org.testng.annotations.Test;

import static org.testng.Assert.assertTrue;

@Test(groups = "unit")
public class TypeTest {
    static class Person {
        String name;
        static Comparator<Person> C = (p1, p2) -> p1.name.compareTo(p2.name);

        Person(String name) {
            this.name = name;
        }

        String getName() { return name; }
    }

    static class Employee extends Person {
        int id;
        static Comparator<Employee> C = (e1, e2) -> e1.id - e2.id;

        Employee(int id, String name) {
            super(name);
            this.id = id;
        }
    }

    static class Manager extends Employee {
        long reports;
        static Comparator<Manager> C = (e1, e2) -> (int) (e1.reports - e2.reports);

        Manager(String name, int id, long reports) {
            super(id, name);
            this.reports = reports;
        }
    }

    static class Department {
        Manager mgr;
        String hr_code;

        Department(Manager mgr, String hr) {
            this.mgr = mgr;
            this.hr_code = hr;
        }

        Manager getManager() {
            return mgr;
        }

        String getHR() {
            return hr_code;
        }
    }

    static <T> void assertOrder(T o1, T o2, Comparator<? super T> cmp) {
        if (cmp.compare(o1, o2) > 0) {
            System.out.println("Fail!!");
        }
        if (cmp.compare(o1, o2) == 0) {
            System.out.println("Equal!!");
        }
    }

    // Type tests just to make sure the code can compile and build
    // Not necessarily need a meaningful result
    public void testOrder() {
        Manager m1 = new Manager("Manager", 2, 2000);
        Manager m2 = new Manager("Manager", 4, 1300);

        // Comparator<Employee> tmp = Person.C;

        // Comparator<Manager> cmp = Employee.C.thenComparing(Person.C);
        Comparator<Employee> cmp = Employee.C.thenComparing(Person.C);
        assertOrder(m1, m2, Employee.C.thenComparing(Person.C));
        assertOrder(m1, m2, cmp);
        assertOrder(m1, new Employee(1, "Z"), Person.C);
        assertOrder(new Employee(1, "Z"), m2, Employee.C);

        assertOrder(m1, m2, Comparator.comparing(Employee::getName, String.CASE_INSENSITIVE_ORDER));

        Map<String, Integer> map = new TreeMap<>();
        map.entrySet().stream().sorted(Map.Entry.comparingByKey(String.CASE_INSENSITIVE_ORDER));
    }

    public void testJDK8033590() {
        Manager a = new Manager("John Doe", 1234, 16);
        Manager b = new Manager("Jane Roe", 2468, 16);
        Department da = new Department(a, "X");
        Department db = new Department(b, "X");

        Comparator<Department> cmp = Comparator.comparing(Department::getHR)
                .thenComparing(Department::getManager, Employee.C);
        assertTrue(cmp.compare(da, db) < 0);

        cmp = Comparator.comparing(Department::getHR)
                .thenComparing(Department::getManager, Manager.C);
        assertTrue(cmp.compare(da, db) == 0);

        cmp = Comparator.comparing(Department::getHR)
                .thenComparing(Department::getManager, Person.C);
        assertTrue(cmp.compare(da, db) > 0);
    }
}
