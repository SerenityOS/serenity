/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8014097
 * @summary Test the limited privilege scope version of doPrivileged
 */

import java.security.*;
import java.util.*;

public class LimitedDoPrivileged {
    /*
     * Test variations of doPrivileged() and doPrivileged() with a limited privilege scope
     * in a sandbox with the usual default permission to read the system properties for the
     * file and path separators.
     *
     * By passing in an "assigned" AccessControlContext that has
     * no default permissions we can test how code privileges are being scoped.
     */

    private static final ProtectionDomain domain =
        new ProtectionDomain(null, null, null, null);
    private static final AccessControlContext acc =
        new AccessControlContext(new ProtectionDomain[] { domain });
    private static final PropertyPermission pathPerm =
        new PropertyPermission("path.separator", "read");
    private static final PropertyPermission filePerm =
        new PropertyPermission("file.separator", "read");

    public static void main(String[] args) throws Exception {
        /*
         * Verify that we have the usual default property read permission.
         */
        AccessController.getContext().checkPermission(filePerm);
        AccessController.getContext().checkPermission(pathPerm);
        System.out.println("test 1 passed");

        /*
         * Inject the "no permission" AccessControlContext.
         */
        AccessController.doPrivileged(new PrivilegedAction() {
            public Object run() {

                /*
                 * Verify that we no longer have the "file.separator" permission.
                 */
                try {
                    AccessController.getContext().checkPermission(pathPerm);
                } catch (AccessControlException ace) {
                    System.out.println("test 2 passed");
                }

                /*
                 * Verify that we can give ourselves limited privilege to read
                 * any system property starting with "path.".
                 */
                AccessController.doPrivileged
                    (new PrivilegedAction() {
                        public Object run() {
                            AccessController.getContext().checkPermission(pathPerm);
                            return null;
                        }
                }, null, new PropertyPermission("path.*", "read"));
                System.out.println("test 3 passed");

                /*
                 * Verify that if we give ourselves limited privilege to read
                 * any system property starting with "path." it won't give us the
                 * the ability to read "file.separator".
                 */
                try {
                    AccessController.doPrivileged
                        (new PrivilegedAction() {
                            public Object run() {
                                AccessController.getContext().checkPermission(filePerm);
                                return null;
                            }
                    }, null, new PropertyPermission("path.*", "read"));
                } catch (AccessControlException ace) {
                    System.out.println("test 4 passed");
                }

                /*
                 * Verify that capturing and passing in the context with no default
                 * system property permission grants will prevent access that succeeded
                 * earlier without the context assignment.
                 */
                final AccessControlContext context = AccessController.getContext();
                try {
                    AccessController.doPrivileged
                        (new PrivilegedAction() {
                            public Object run() {
                                AccessController.getContext().checkPermission(pathPerm);
                                return null;
                            }
                    }, context, new PropertyPermission("path.*", "read"));
                } catch (AccessControlException ace) {
                    System.out.println("test 5 passed");
                }

                /*
                 * Verify that we can give ourselves full privilege to read
                 * any system property starting with "path.".
                 */
                AccessController.doPrivileged
                     (new PrivilegedAction() {
                        public Object run() {
                            AccessController.getContext().checkPermission(pathPerm);
                            return null;
                        }
                });
                System.out.println("test 6 passed");

                /*
                 * Verify that capturing and passing in the context with no default
                 * system property permission grants will prevent access that succeeded
                 * earlier without the context assignment.
                 */
                try {
                    AccessController.doPrivileged
                        (new PrivilegedAction() {
                            public Object run() {
                                AccessController.getContext().checkPermission(pathPerm);
                                return null;
                            }
                    }, context);
                } catch (AccessControlException ace) {
                    System.out.println("test 7 passed");
                }

                /*
                 * Verify that we can give ourselves limited privilege to read
                 * any system property starting with "path." when a limited
                 * privilege scope context is captured and passed to a regular
                 * doPrivileged() as an assigned context.
                 */
                AccessController.doPrivileged
                     (new PrivilegedAction() {
                        public Object run() {

                            /*
                             * Capture the limited privilege scope and inject it into the
                             * regular doPrivileged().
                             */
                            final AccessControlContext limitedContext = AccessController.getContext();
                            AccessController.doPrivileged
                                (new PrivilegedAction() {
                                    public Object run() {
                                        AccessController.getContext().checkPermission(pathPerm);
                                        return null;
                                }
                            }, limitedContext);
                            return null;
                        }
                }, null, new PropertyPermission("path.*", "read"));
                System.out.println("test 8 passed");

                /*
                 * Verify that we can give ourselves limited privilege to read
                 * any system property starting with "path." it won't give us the
                 * the ability to read "file.separator" when a limited
                 * privilege scope context is captured and passed to a regular
                 * doPrivileged() as an assigned context.
                 */
                AccessController.doPrivileged
                     (new PrivilegedAction() {
                        public Object run() {

                            /*
                             * Capture the limited privilege scope and inject it into the
                             * regular doPrivileged().
                             */
                            final AccessControlContext limitedContext = AccessController.getContext();
                            try {
                                AccessController.doPrivileged
                                    (new PrivilegedAction() {
                                        public Object run() {
                                            AccessController.getContext().checkPermission(filePerm);
                                            return null;
                                    }
                                }, limitedContext);
                            } catch (AccessControlException ace) {
                                System.out.println("test 9 passed");
                            }
                            return null;
                        }
                }, null, new PropertyPermission("path.*", "read"));

                return null;
            }
        }, acc);
    }
}
