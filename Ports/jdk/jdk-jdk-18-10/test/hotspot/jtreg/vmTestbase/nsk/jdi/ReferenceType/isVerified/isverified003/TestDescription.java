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
 * @summary converted from VM Testbase nsk/jdi/ReferenceType/isVerified/isverified003.
 * VM Testbase keywords: [quick, jpda, jdi]
 * VM Testbase readme:
 * DESCRIPTION:
 *    The test checks up that an implementation of the
 *    com.sun.jdi.ReferenceType.isVerified() method conforms
 *    with its spec.
 *    The test verifies an assertion:
 *      public boolean isVerified()
 *          Determines if this type has been verified.
 *    The test consists of:
 *      debugger application  - nsk.jdi.ReferenceType.isVerified.isverified003,
 *      debuggee application  - nsk.jdi.ReferenceType.isVerified.isverified003a,
 *      custom-loaded classes - nsk.jdi.ReferenceType.isVerified.isverified003b,
 *                              nsk.jdi.ReferenceType.isVerified.isverified003c
 *    The test checks up results of the method for two classes loaded by
 *    custom isverified003aClassLoader loader. The custom loader loads
 *    auxuliary classes until preparation point exclusively. The test fails
 *    if isVerified() method returns diffrent results for custom-loaded classes
 * COMMENTS:
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.ReferenceType.isVerified.isverified003
 *        nsk.jdi.ReferenceType.isVerified.isverified003a
 *
 * @comment compile loadclassXX to bin/loadclassXX
 * @run driver nsk.share.ExtraClassesBuilder
 *      loadclass
 *
 * @run main/othervm
 *      nsk.jdi.ReferenceType.isVerified.isverified003
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}" ./bin
 */

