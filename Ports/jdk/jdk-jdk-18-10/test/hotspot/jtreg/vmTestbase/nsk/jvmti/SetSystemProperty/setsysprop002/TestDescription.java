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
 * @summary converted from VM Testbase nsk/jvmti/SetSystemProperty/setsysprop002.
 * VM Testbase keywords: [quick, jpda, jvmti, onload_only_logic, noras]
 * VM Testbase readme:
 * DESCRIPTION
 *     This JVMTI test exercises JVMTI function SetSystemProperty().
 *     This test checks that function SetSystemProperty() successfully
 *     sets values for the system properties defined via command line
 *     option "-Dproperty=value".
 *     Also the test checks that the new values are returned by function
 *     GetSystemProperty(), even if they were set in the same or other phase.
 *     This is checked either for OnLoad and live phases.
 *     The following properties are checked:
 *         nsk.jvmti.property              - value is just changed
 *         nsk.jvmti.property.empty.old    - value is changed from empty string
 *         nsk.jvmti.property.empty.new    - value is changed to empty string
 * COMMENTS
 *     Fixed due to spec change according to the bug:
 *     5062403 TEST_BUG: AddToBootstrapClassLoaderSearch and SetSystemProperties only OnLoad
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm/native
 *      -agentlib:setsysprop002=-waittime=5
 *      -Dnsk.jvmti.test.property=initial_value_of_nsk.jvmti.test.property
 *      -Dnsk.jvmti.test.property.empty.old=
 *      -Dnsk.jvmti.test.property.empty.new=initial_value_of_nsk.jvmti.test.property.empty.new
 *      nsk.jvmti.SetSystemProperty.setsysprop002
 */

