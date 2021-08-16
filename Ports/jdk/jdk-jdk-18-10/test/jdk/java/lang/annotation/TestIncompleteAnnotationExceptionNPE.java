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

/*
 * @test
 * @bug     7021922
 * @summary Test null handling of IncompleteAnnotationException constructor
 * @author  Joseph D. Darcy
 */

import java.lang.annotation.*;

public class TestIncompleteAnnotationExceptionNPE {
    public static void main(String... args) {
        int errors = 0;
        Class<? extends Annotation> annotationType = Annotation.class;
        String elementName = "name";

        try {
            Object o = new IncompleteAnnotationException(null, null);
            errors++;
        } catch(NullPointerException npe) {
            ; // Expected
        }

        try {
            Object o = new IncompleteAnnotationException(annotationType, null);
            errors++;
        } catch(NullPointerException npe) {
            ; // Expected
        }

        try {
            Object o = new IncompleteAnnotationException(null, elementName);
            errors++;
        } catch(NullPointerException npe) {
            ; // Expected
        }

        if (errors != 0)
            throw new RuntimeException("Encountered " + errors +
                                       " error(s) during construction.");
    }
}
