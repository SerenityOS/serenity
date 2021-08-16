/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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
 *
 * @summary converted from VM Testbase nsk/jvmti/RedefineClasses/redefclass006.
 * VM Testbase keywords: [quick, jpda, jvmti, noras, redefine]
 * VM Testbase readme:
 * DESCRIPTION
 *     This test checks that the JVMTI function RedefineClasses()
 *     properly returns the error JVMTI_ERROR_NULL_POINTER:
 *     "Invalid pointer: classDefs or one of class_bytes is NULL".
 *     The test creates a dummy instance of tested class redefclass006r.
 *     Then the test tries twice to redefine the class redefclass006r by
 *     sequencely invoking the function RedefineClasses() with:
 *      - NULL pointer to the field class_bytes of the structure
 *        JVMTI_class_definition;
 *      - NULL pointer to the structure JVMTI_class_definition by itself.
 * COMMENTS
 *     Ported from JVMDI.
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jvmti.RedefineClasses.redefclass006r
 * @run main/othervm/native -agentlib:redefclass006 nsk.jvmti.RedefineClasses.redefclass006
 */

