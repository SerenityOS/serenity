/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

package typeannos;

import java.lang.annotation.*;
import static java.lang.annotation.ElementType.*;

/*
 * This class is partially replicated from test/tools/javac/annotations/typeAnnotations/classfile/CombinationsTargetTest1.java; CombinationsTargetTest2.java; CombinationsTargetTest3.java
 */
@RepTypeA @RepTypeA @RepTypeB @RepTypeB class RepeatingAtClassLevel {
}

@RepTypeUseA @RepTypeUseA @RepTypeUseB @RepTypeUseB class RepeatingAtClassLevel2 {
}

@RepAllContextsA @RepAllContextsA @RepAllContextsB @RepAllContextsB class RepeatingAtClassLevel3 {
}

class RepeatingOnConstructor {

    @RepConstructorA @RepConstructorA @RepConstructorB @RepConstructorB
    RepeatingOnConstructor() {
    }

    @RepTypeUseA @RepTypeUseA @RepTypeUseB @RepTypeUseB RepeatingOnConstructor(int i) {
    }

    @RepConstructorA @RepConstructorA @RepConstructorB @RepConstructorB
    @RepTypeUseA @RepTypeUseA @RepTypeUseB @RepTypeUseB RepeatingOnConstructor(int i, int j) {
    }

    @RepAllContextsA @RepAllContextsA @RepAllContextsB @RepAllContextsB RepeatingOnConstructor(int i, int j, int k) {
    }

    RepeatingOnConstructor(@RepParameterA @RepParameterA @RepParameterB @RepParameterB String parameter, @RepParameterA @RepParameterA @RepParameterB @RepParameterB String @RepTypeUseA @RepTypeUseA @RepTypeUseB @RepTypeUseB ... vararg) {
    }

    class Inner {
        Inner(@RepTypeUseA @RepTypeUseA @RepTypeUseB @RepTypeUseB RepeatingOnConstructor RepeatingOnConstructor.this, @RepParameterA @RepParameterA @RepParameterB @RepParameterB String parameter, @RepParameterA @RepParameterA @RepParameterB @RepParameterB String @RepTypeUseA @RepTypeUseA @RepTypeUseB @RepTypeUseB ... vararg) {
        }
    }
}

class RepeatingOnField {
    @RepFieldA @RepFieldA @RepFieldB @RepFieldB
    Integer i1;

    @RepTypeUseA @RepTypeUseA @RepTypeUseB @RepTypeUseB Integer i2;

    @RepFieldA @RepFieldA @RepFieldB @RepFieldB
    @RepTypeUseA @RepTypeUseA @RepTypeUseB @RepTypeUseB Integer i3;

    @RepAllContextsA @RepAllContextsA @RepAllContextsB @RepAllContextsB Integer i4;

    String @RepTypeUseA @RepTypeUseA @RepTypeUseB @RepTypeUseB [] @RepTypeUseA @RepTypeUseA @RepTypeUseB @RepTypeUseB [] sa = null;
}

class RepeatingOnMethod {

    @RepMethodA @RepMethodA @RepMethodB @RepMethodB
    String test1() {
        return null;
    }

    @RepTypeUseA @RepTypeUseA @RepTypeUseB @RepTypeUseB String test2() {
        return null;
    }

    @RepMethodA @RepMethodA @RepMethodB @RepMethodB
    @RepTypeUseA @RepTypeUseA @RepTypeUseB @RepTypeUseB String test3() {
        return null;
    }

    @RepAllContextsA @RepAllContextsA @RepAllContextsB @RepAllContextsB String test4() {
        return null;
    }

    String test5(@RepTypeUseA @RepTypeUseA @RepTypeUseB @RepTypeUseB RepeatingOnMethod this, @RepParameterA @RepParameterA @RepParameterB @RepParameterB String parameter, @RepParameterA @RepParameterA @RepParameterB @RepParameterB String @RepTypeUseA @RepTypeUseA @RepTypeUseB @RepTypeUseB ... vararg) {
        return null;
    }
}

class RepeatingOnTypeParametersBoundsTypeArgumentsOnClassDecl <@RepTypeParameterA @RepTypeParameterA @RepTypeParameterB @RepTypeParameterB T extends @RepTypeUseA @RepTypeUseA @RepTypeUseB @RepTypeUseB Object> {

    <T> String genericMethod(T t) {
        return null;
    }
}

class RepeatingOnTypeParametersBoundsTypeArgumentsOnClassDecl2 <@RepTypeUseA @RepTypeUseA @RepTypeUseB @RepTypeUseB T extends @RepTypeUseA @RepTypeUseA @RepTypeUseB @RepTypeUseB Object> {

    <T> String genericMethod(T t) {
        return null;
    }
}

class RepeatingOnTypeParametersBoundsTypeArgumentsOnMethod <T extends Object> {

    String test(@RepTypeUseA @RepTypeUseA @RepTypeUseB @RepTypeUseB RepeatingOnTypeParametersBoundsTypeArgumentsOnMethod<@RepTypeUseA @RepTypeUseA @RepTypeUseB @RepTypeUseB T> this) {
        return null;
    }

    <@RepTypeParameterA @RepTypeParameterA @RepTypeParameterB @RepTypeParameterB T> String genericMethod(@RepParameterA @RepParameterA @RepParameterB @RepParameterB T t) {
        return null;
    }

    <@RepTypeUseA @RepTypeUseA @RepTypeUseB @RepTypeUseB T> String genericMethod2(@RepTypeUseA @RepTypeUseA @RepTypeUseB @RepTypeUseB T t) {
        return null;
    }
}

class RepeatingOnVoidMethodDeclaration {

    @RepMethodA @RepMethodA @RepMethodB @RepMethodB void test() {}
}

class RepeatingOnStaticMethodOfInterface {

    interface I {
        static @RepMethodA @RepMethodA @RepMethodB @RepMethodB String m() {
            return null;
        }
    }
}

//------------------------------------------------------------------------------
@Target({TYPE})
@Repeatable(ContTypeA.class)
@Documented
@interface RepTypeA { }

@Target({TYPE})
@Documented
@interface ContTypeA { RepTypeA[] value(); }

@Target({TYPE})
@Repeatable(ContTypeB.class)
@Documented
@interface RepTypeB { }

@Target({TYPE})
@Documented
@interface ContTypeB { RepTypeB[] value(); }

//------------------------------------------------------------------------------
@Target({CONSTRUCTOR})
@Repeatable(ContConstructorA.class)
@Documented
@interface RepConstructorA { }

@Target({CONSTRUCTOR})
@Documented
@interface ContConstructorA { RepConstructorA[] value(); }

@Target({CONSTRUCTOR})
@Repeatable(ContConstructorB.class )
@Documented
@interface RepConstructorB { }

@Target({CONSTRUCTOR})
@Documented
@interface ContConstructorB { RepConstructorB[] value(); }

//------------------------------------------------------------------------------
@Target({METHOD})
@Repeatable(ContMethodA.class)
@Documented
@interface RepMethodA {}

@Target({METHOD})
@Documented
@interface ContMethodA {
    RepMethodA[] value();
}

@Target({METHOD})
@Repeatable(ContMethodB.class)
@Documented
@interface RepMethodB {}

@Target({METHOD})
@Documented
@interface ContMethodB {
    RepMethodB[] value();
}

//------------------------------------------------------------------------------
@Target({FIELD})
@Repeatable(ContFieldA.class)
@Documented
@interface RepFieldA {}

@Target({FIELD})
@Documented
@interface ContFieldA {
    RepFieldA[] value();
}

@Target({FIELD})
@Repeatable(ContFieldB.class)
@Documented
@interface RepFieldB {}

@Target({FIELD})
@Documented
@interface ContFieldB {
    RepFieldB[] value();
}

//------------------------------------------------------------------------------
@Target({TYPE_USE})
@Repeatable(ContTypeUseA.class)
@Documented
@interface RepTypeUseA {}

@Target({TYPE_USE})
@Documented
@interface ContTypeUseA {
    RepTypeUseA[] value();
}

@Target({TYPE_USE})
@Repeatable(ContTypeUseB.class)
@Documented
@interface RepTypeUseB {}

@Target({TYPE_USE})
@Documented
@interface ContTypeUseB {
    RepTypeUseB[] value();
}

//------------------------------------------------------------------------------
@Target({TYPE_PARAMETER})
@Repeatable(ContTypeParameterA.class)
@Documented
@interface RepTypeParameterA {}

@Target({TYPE_PARAMETER})
@Documented
@interface ContTypeParameterA {
    RepTypeParameterA[] value();
}

@Target({TYPE_PARAMETER})
@Repeatable(ContTypeParameterB.class)
@Documented
@interface RepTypeParameterB {}

@Target({TYPE_PARAMETER})
@Documented
@interface ContTypeParameterB {
    RepTypeParameterB[] value();
}

//------------------------------------------------------------------------------
@Target({PARAMETER})
@Repeatable(ContParameterA.class)
@Documented
@interface RepParameterA {}

@Target({PARAMETER})
@Documented
@interface ContParameterA {
    RepParameterA[] value();
}

@Target({PARAMETER})
@Repeatable(ContParameterB.class)
@Documented
@interface RepParameterB {}

@Target({PARAMETER})
@Documented
@interface ContParameterB {
    RepParameterB[] value();
}


//------------------------------------------------------------------------------
@Target({PACKAGE})
@Repeatable(ContPackageA.class)
@Documented
@interface RepPackageA {}

@Target({PACKAGE})
@Documented
@interface ContPackageA {
    RepPackageA[] value();
}

@Target({PACKAGE})
@Repeatable(ContPackageB.class)
@Documented
@interface RepPackageB {}

@Target({PACKAGE})
@Documented
@interface ContPackageB {
    RepPackageB[] value();
}

//------------------------------------------------------------------------------
@Target({TYPE, FIELD, METHOD, PARAMETER, CONSTRUCTOR, LOCAL_VARIABLE, ANNOTATION_TYPE, PACKAGE, TYPE_PARAMETER, TYPE_USE})
@Repeatable(ContAllContextsA.class)
@Documented
@interface RepAllContextsA {}

@Target({TYPE, FIELD, METHOD, PARAMETER, CONSTRUCTOR, LOCAL_VARIABLE, ANNOTATION_TYPE, PACKAGE, TYPE_PARAMETER, TYPE_USE})
@Documented
@interface ContAllContextsA {
    RepAllContextsA[] value();
}

@Target({TYPE, FIELD, METHOD, PARAMETER, CONSTRUCTOR, LOCAL_VARIABLE, ANNOTATION_TYPE, PACKAGE, TYPE_PARAMETER, TYPE_USE})
@Repeatable(ContAllContextsB.class)
@Documented
@interface RepAllContextsB {}

@Target({TYPE, FIELD, METHOD, PARAMETER, CONSTRUCTOR, LOCAL_VARIABLE, ANNOTATION_TYPE, PACKAGE, TYPE_PARAMETER, TYPE_USE})
@Documented
@interface ContAllContextsB {
    RepAllContextsB[] value();
}
