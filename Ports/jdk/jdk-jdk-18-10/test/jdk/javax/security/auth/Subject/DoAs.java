/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8214583
 * @summary Check that getSubject works after JIT compiler escape analysis.
 */
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedExceptionAction;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import javax.security.auth.Subject;

public class DoAs {

    public static void main(String[] args) throws Exception {
        final Set<String> outer = new HashSet<>(Arrays.asList("Outer"));
        final Subject subject = new Subject(true, Collections.EMPTY_SET, outer, Collections.EMPTY_SET);

        for (int i = 0; i < 100_000; ++i) {
            final int index = i;
            Subject.doAs(subject, (PrivilegedExceptionAction<Integer>)() -> {
                AccessControlContext c1 = AccessController.getContext();
                Subject s = Subject.getSubject(c1);
                if (s != subject) {
                    throw new AssertionError("outer Oops! " + "iteration " + index + " " + s + " != " + subject);
                }
                return 0;
            });
        }
    }
}
