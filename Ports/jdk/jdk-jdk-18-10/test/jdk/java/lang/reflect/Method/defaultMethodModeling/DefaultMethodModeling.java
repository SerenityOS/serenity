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
 * @bug 8011590
 * @summary Check modeling of default methods
 * @author Joseph D. Darcy
 */

import java.util.Objects;
import java.lang.reflect.*;
import java.lang.annotation.*;

import static java.lang.reflect.Modifier.*;

public class DefaultMethodModeling {
    public static void main(String... args) {
        int failures = 0;

        Class<?>[] classes = {SuperC.class, SuperCchild.class,
                              SuperI.class, SuperIchild.class,
                              SuperIwithDefault.class, SuperIwithDefaultChild.class,
                              Base.class, Combo1.class, Combo2.class,
                              SonSuperIwithDefault.class, DaughterSuperIwithDefault.class, GrandchildSuperIwithDefault.class, D.class,
                              B.class, C.class, B1.class, D1.class
        };

        for(Class<?> clazz : classes) {
            System.err.println(clazz.toString());
            for(Method m : clazz.getMethods()) {
                if (m.getDeclaringClass() != java.lang.Object.class)
                    failures += testMethod(m);
            }
        }

        if (failures > 0)
            throw new RuntimeException();
    }

    private static int testMethod(Method m) {
        ExpectedModel em = Objects.requireNonNull(m.getAnnotation(ExpectedModel.class));
        boolean failed = false;

        if (m.getModifiers() != em.modifiers()) {
            failed = true;
            System.err.printf("Unexpected modifiers %d; expected %d%n", m.getModifiers(), em.modifiers());
        }

        if (m.isDefault() != em.isDefault()) {
            failed = true;
            System.err.printf("Unexpected isDefualt %b; expected b%n", m.isDefault(), em.isDefault());
        }

        if (!m.getDeclaringClass().equals(em.declaringClass())) {
            failed = true;
            System.err.printf("Unexpected isDefualt %s; expected %s%n",
                              m.getDeclaringClass().toString(), em.declaringClass().toString());
        }

        return (!failed) ? 0 :1;
    }
}

@Retention(RetentionPolicy.RUNTIME)
@interface ExpectedModel {
    boolean isDefault() default false;
    int modifiers() default PUBLIC;
    Class<?> declaringClass();
}

abstract class SuperC {
    @ExpectedModel(modifiers=PUBLIC|ABSTRACT, declaringClass=SuperC.class)
    public abstract void foo();

    @ExpectedModel(declaringClass=SuperC.class)
    public void bar() {
        ;
    }
}

class SuperCchild extends SuperC {
    @ExpectedModel(declaringClass=SuperCchild.class)
    @Override
    public void foo() {;}
}

// -=-=-=-

interface SuperI {
    @ExpectedModel(modifiers=PUBLIC|ABSTRACT, declaringClass=SuperI.class)
    void foo();

    @ExpectedModel(modifiers=PUBLIC|ABSTRACT, declaringClass=SuperI.class)
    void bar();
}

class SuperIchild implements SuperI {
    @ExpectedModel(declaringClass=SuperIchild.class)
    public void foo() {;}

    @ExpectedModel(declaringClass=SuperIchild.class)
    public void bar() {;}
}

// -=-=-=-

interface SuperIwithDefault {
    @ExpectedModel(modifiers=PUBLIC|ABSTRACT, declaringClass=SuperIwithDefault.class)
    void foo();

    @ExpectedModel(isDefault=true, declaringClass=SuperIwithDefault.class)
    default void bar() {
        ;
    }
}

class SuperIwithDefaultChild implements SuperIwithDefault {
    @ExpectedModel(declaringClass=SuperIwithDefaultChild.class)
    @Override
    public void foo() {;}
}

// -=-=-=-

abstract class Base {
    @ExpectedModel(modifiers=PUBLIC|ABSTRACT, declaringClass=Base.class)
    public abstract void baz();

    @ExpectedModel(declaringClass=Base.class)
    public void quux() {;}
}

abstract class Combo1 extends Base implements SuperI {
    @ExpectedModel(declaringClass=Combo1.class)
    public void wombat() {}
}

abstract class Combo2 extends Base implements SuperIwithDefault {
    @ExpectedModel(declaringClass=Combo2.class)
    public void wombat() {}
}

// -=-=-=-

interface SonSuperIwithDefault extends SuperIwithDefault {
    @ExpectedModel(modifiers=PUBLIC|ABSTRACT, declaringClass=SonSuperIwithDefault.class)
    void baz();

    @ExpectedModel(isDefault=true, declaringClass=SonSuperIwithDefault.class)
    default void bazD() {;}
}

interface DaughterSuperIwithDefault extends SuperIwithDefault {
    @ExpectedModel(modifiers=PUBLIC|ABSTRACT, declaringClass=DaughterSuperIwithDefault.class)
    void quux();

    @ExpectedModel(isDefault=true, declaringClass=DaughterSuperIwithDefault.class)
    default void quuxD() {;}
}

interface GrandchildSuperIwithDefault extends SonSuperIwithDefault, DaughterSuperIwithDefault {
    @ExpectedModel(modifiers=PUBLIC|ABSTRACT, declaringClass=GrandchildSuperIwithDefault.class)
    void wombat();

    @ExpectedModel(isDefault=true, declaringClass=GrandchildSuperIwithDefault.class)
    default void wombatD() {;}

}

class D implements GrandchildSuperIwithDefault {
    @ExpectedModel(declaringClass=D.class)
    public void wombat(){}

    @ExpectedModel(declaringClass=D.class)
    public void  baz(){}

    @ExpectedModel(declaringClass=D.class)
    public void foo(){}

    @ExpectedModel(declaringClass=D.class)
    public void quux(){}
}

class D1 implements SonSuperIwithDefault, DaughterSuperIwithDefault {
    @ExpectedModel(declaringClass=D1.class)
    public void foo(){}

    @ExpectedModel(declaringClass=D1.class)
    public void  baz(){}

    @ExpectedModel(declaringClass=D1.class)
    public void quux(){}
}

// -=-=-=-

// What does re-abstraction look like?

class A implements SuperIwithDefault {
    @ExpectedModel(declaringClass=A.class)
    @Override
    public void foo(){;}
}

abstract class B extends A {
    @ExpectedModel(modifiers=PUBLIC|ABSTRACT, declaringClass=B.class)
    @Override
    public abstract void bar();
}

class C extends B implements SuperIwithDefault {
    @ExpectedModel(declaringClass=C.class)
    public void bar(){}
}

abstract class A1 implements SonSuperIwithDefault {
    @ExpectedModel(modifiers=PUBLIC|ABSTRACT, declaringClass=A1.class)
    public abstract void baz();

    @ExpectedModel(modifiers=PUBLIC|ABSTRACT, declaringClass=A1.class)
    public abstract void foo();
}

class B1 extends A1 {
    @ExpectedModel(declaringClass=B1.class)
    @Override
    public void foo(){;}

    @ExpectedModel(declaringClass=B1.class)
    @Override
    public void baz(){}
}
