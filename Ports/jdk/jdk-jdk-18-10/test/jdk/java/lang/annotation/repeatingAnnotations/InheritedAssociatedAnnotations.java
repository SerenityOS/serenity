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
 * @bug     8027170
 * @summary getAnnotationsByType needs to take the class hierarchy into account
 *          when determining which annotations are associated with a given
 *          class.
 * @run main InheritedAssociatedAnnotations
 */

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.Arrays;

public class InheritedAssociatedAnnotations {

    public static void main(String[] args) {
        checkAssociated(A3.class);
        checkAssociated(B3.class);
        checkAssociated(C3.class);
        checkAssociated(D3.class);
    }

    private static void checkAssociated(AnnotatedElement ae) {
        Ann[] actual = ae.getAnnotationsByType(Ann.class);
        Ann[] expected = ae.getAnnotation(ExpectedAssociated.class).value();

        if (!Arrays.equals(actual, expected)) {
            throw new RuntimeException(String.format(
                    "Test failed for %s: Expected %s but got %s.",
                    ae,
                    Arrays.toString(expected),
                    Arrays.toString(actual)));
        }
    }

}

@Retention(RetentionPolicy.RUNTIME)
@interface ExpectedAssociated {
    Ann[] value();
}


@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Repeatable(AnnCont.class)
@interface Ann {
    int value();
}

@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@interface AnnCont {
    Ann[] value();
}


@Ann(10)
class A1 {}

@Ann(20)
class A2 extends A1 {}

@ExpectedAssociated({@Ann(20)})
class A3 extends A2 {}


@Ann(10) @Ann(11)
class B1 {}

@Ann(20)
class B2 extends B1 {}

@ExpectedAssociated({@Ann(20)})
class B3 extends B2 {}


@Ann(10)
class C1 {}

@Ann(20) @Ann(21)
class C2 extends C1 {}

@ExpectedAssociated({@Ann(20), @Ann(21)})
class C3 extends C2 {}


@Ann(10) @Ann(11)
class D1 {}

@Ann(20) @Ann(21)
class D2 extends D1 {}

@ExpectedAssociated({@Ann(20), @Ann(21)})
class D3 extends D2 {}
