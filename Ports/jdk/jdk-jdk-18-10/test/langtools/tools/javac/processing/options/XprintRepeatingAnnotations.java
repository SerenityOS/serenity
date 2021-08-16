/*
 * Copyright (c) 2010, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8005295
 * @summary Verify repeating annotations are printed as expected
 * @compile/ref=XprintRepeatingAnnotations.out -Xprint  XprintRepeatingAnnotations.java
 */

import java.lang.annotation.*;
import static java.lang.annotation.RetentionPolicy.*;

@Foo(1)
@Foo(2)
@Bar(3)
@Bar(4)
public class XprintRepeatingAnnotations {
}

@Retention(RUNTIME)
@Documented
@Repeatable(Foos.class)
@interface Foo {
    int value();
}

@Retention(RUNTIME)
@Documented
@interface Foos {
    Foo[] value();
}

@Retention(RUNTIME)
@Documented
@Repeatable(Bars.class)
@interface Bar {
    int value();
}

@Retention(RUNTIME)
@Documented
@interface Bars {
    Bar[] value();
    int quux() default 1;
}
