/*
 * Copyright (c) 2003, 2011, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4893489
 * @summary     Subject.doAs not thread-safe against changes to principals
 */

import java.security.Principal;
import java.security.PrivilegedAction;
import java.util.Collections;
import java.util.Set;
import javax.security.auth.Subject;
import javax.security.auth.x500.X500Principal;

public class Synch {
    static volatile boolean finished = false;
    public static void main(String[] args) {
        Subject subject = new Subject();
        final Set principals = subject.getPrincipals();
        principals.add(new X500Principal("CN=Alice"));
        new Thread() {
            public void run() {
                Principal last = new X500Principal("CN=Bob");
                for (int i = 0; !finished; i++) {
                    Principal next = new X500Principal("CN=Bob" + i);
                    principals.add(next);
                    principals.remove(last);
                    last = next;
                }
            }
        }.start();
        for (int i = 0; i < 1000; i++) {
            Subject.doAs(
                subject,
                new PrivilegedAction() {
                    public Object run() {
                        return Subject.doAs(
                            new Subject(true,
                                        Collections.singleton(
                                            new X500Principal("CN=Claire")),
                                        Collections.EMPTY_SET,
                                        Collections.EMPTY_SET),
                            new PrivilegedAction() {
                                public Object run() {
                                    return null;
                                }
                            });
                    }
                });
        }
        finished = true;
    }
}
