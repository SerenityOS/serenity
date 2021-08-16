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

/*
 * @test
 * @bug 8047338
 * @summary javac is not correctly filtering non-members methods to obtain the function descriptor
 * @compile FilterNonMembersToObtainFunctionDescriptorTest.java
 */

public class FilterNonMembersToObtainFunctionDescriptorTest {
    <V, E extends Exception> V fails(CallableFail<V, E> callable) throws E {
        return null;
    }

    <V, E extends Exception> V failsSub(CallableFailSub<V, E> callable) throws E {
        return null;
    }

    void m() throws Exception {
        fails((String s) -> 2);
        failsSub((String s) -> 2);
    }

    interface Callable<V> {
        V callFail(String s) throws Exception;
    }

    interface CallableFail<V, E extends Exception> extends Callable<V> {
        @Override
        V callFail(String s) throws E;
    }

    interface CallableFailSub<V, E extends Exception> extends CallableFail<V, E> {}
}
