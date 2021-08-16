/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8029017
 * @summary sanity testing of ElementType validation for repeating annotations
 * @compile TypeUseTarget.java
 */

import java.lang.annotation.*;

public class TypeUseTarget {}


// Case 1:
@Target({
    ElementType.TYPE_USE,
})
@Repeatable(Case1Container.class)
@interface Case1 {}

@Target({
    ElementType.ANNOTATION_TYPE,
    ElementType.TYPE,
    ElementType.TYPE_USE,
    ElementType.TYPE_PARAMETER,
})
@interface Case1Container {
  Case1[] value();
}


// Case 2:
@Target({
    ElementType.TYPE_USE,
})
@Repeatable(Case2Container.class)
@interface Case2 {}

@Target({
    ElementType.ANNOTATION_TYPE,
    ElementType.TYPE,
    ElementType.TYPE_USE,
})
@interface Case2Container {
  Case2[] value();
}


// Case 3:
@Target({
    ElementType.TYPE_USE,
})
@Repeatable(Case3Container.class)
@interface Case3 {}

@Target({
    ElementType.ANNOTATION_TYPE,
    ElementType.TYPE,
})
@interface Case3Container {
  Case3[] value();
}


// Case 4:
@Target({
    ElementType.TYPE_USE,
})
@Repeatable(Case4Container.class)
@interface Case4 {}

@Target({
    ElementType.ANNOTATION_TYPE,
})
@interface Case4Container {
  Case4[] value();
}


// Case 5:
@Target({
    ElementType.TYPE_USE,
})
@Repeatable(Case5Container.class)
@interface Case5 {}

@Target({
    ElementType.TYPE,
})
@interface Case5Container {
  Case5[] value();
}


// Case 6:
@Target({
    ElementType.TYPE_USE,
})
@Repeatable(Case6Container.class)
@interface Case6 {}

@Target({
    ElementType.TYPE_PARAMETER,
})
@interface Case6Container {
  Case6[] value();
}
