/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Smoke test for repeating annotations
 * @bug 7151010
 *
 * @run clean NestedContainers BasicRepeatingAnnos BasicRepeatingAnnos2 Foo Foos FoosFoos
 * @run compile NestedContainers.java
 * @run main NestedContainers
 */

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Repeatable(Foos.class)
@interface Foo {}

@Retention(RetentionPolicy.RUNTIME)
@Repeatable(FoosFoos.class)
@interface Foos {
    Foo[] value();
}

@Retention(RetentionPolicy.RUNTIME)
@interface FoosFoos {
    Foos[] value();
}

@Foo
@Foo
class BasicRepeatingAnnos {}

@Foos({})
@Foos({})
class BasicRepeatingAnnos2 {}

public class NestedContainers {
    public static void main(String[] args) throws Exception {
        Annotation a = BasicRepeatingAnnos.class.getAnnotation(Foos.class);
        if (a == null) {
            throw new RuntimeException("Container annotation missing");
        }

        // Check 2:nd level container
        a = BasicRepeatingAnnos2.class.getAnnotation(FoosFoos.class);
        if (a == null) {
            throw new RuntimeException("Container annotation missing");
        }
    }
}
