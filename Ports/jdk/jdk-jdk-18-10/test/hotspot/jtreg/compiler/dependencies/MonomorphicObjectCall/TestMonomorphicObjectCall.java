/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8050079
 * @summary Compiles a monomorphic call to finalizeObject() on a modified java.lang.Object to test C1 CHA.
 *
 * @build java.base/java.lang.Object
 * @run main/othervm -XX:+IgnoreUnrecognizedVMOptions -Xcomp -XX:-VerifyDependencies
 *                   -XX:TieredStopAtLevel=1
 *                   -XX:CompileCommand=compileonly,compiler.dependencies.MonomorphicObjectCall.TestMonomorphicObjectCall::callFinalize
 *                   -XX:CompileCommand=compileonly,java.lang.Object::finalizeObject
 *                   compiler.dependencies.MonomorphicObjectCall.TestMonomorphicObjectCall
 */

package compiler.dependencies.MonomorphicObjectCall;

public class TestMonomorphicObjectCall {

    private static void callFinalize(Object object) throws Throwable {
        // Call modified version of java.lang.Object::finalize() that is
        // not overridden by any subclass. C1 CHA should mark the call site
        // as monomorphic and inline the method.
        object.finalizeObject();
    }

    public static void main(String[] args) throws Throwable {
        // Trigger compilation of 'callFinalize'
        callFinalize(new Object());
    }
}
