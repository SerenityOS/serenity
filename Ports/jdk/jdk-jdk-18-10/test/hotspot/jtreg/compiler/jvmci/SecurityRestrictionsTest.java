/*
 * Copyright (c) 2015, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8136421
 * @requires vm.jvmci
 * @library /test/lib /
 * @library common/patches
 * @modules java.base/jdk.internal.misc
 * @modules jdk.internal.vm.ci/jdk.vm.ci.hotspot
 * @build jdk.internal.vm.ci/jdk.vm.ci.hotspot.CompilerToVMHelper
 * @run main/othervm -XX:+UnlockExperimentalVMOptions
 *      -XX:+EnableJVMCI
 *      compiler.jvmci.SecurityRestrictionsTest
 *      NO_SEC_MAN
 * @run main/othervm -XX:+UnlockExperimentalVMOptions
 *      -XX:+EnableJVMCI
 *      -Djava.security.manager=allow
 *      compiler.jvmci.SecurityRestrictionsTest
 *      NO_PERM
 * @run main/othervm -XX:+UnlockExperimentalVMOptions
 *      -XX:+EnableJVMCI
 *      -Djava.security.manager=allow
 *      compiler.jvmci.SecurityRestrictionsTest
 *      ALL_PERM
 * @run main/othervm -XX:+UnlockExperimentalVMOptions
 *      -XX:+EnableJVMCI -XX:-UseJVMCICompiler
 *      -Djava.security.manager=allow
 *      compiler.jvmci.SecurityRestrictionsTest
 *      NO_JVMCI_ACCESS_PERM
 * @run main/othervm -XX:+UnlockExperimentalVMOptions
 *      -XX:-EnableJVMCI -XX:-UseJVMCICompiler
 *      compiler.jvmci.SecurityRestrictionsTest
 *      NO_JVMCI
 */

package compiler.jvmci;

import jdk.test.lib.Utils;

import java.security.AccessControlException;
import java.security.Permission;
import java.util.PropertyPermission;
import java.util.function.Consumer;

public class SecurityRestrictionsTest {

    public static void main(String[] args) {
        try {
            // to init Utils before call SecurityManager
            Class.forName(Utils.class.getName(), true,
                    Utils.class.getClassLoader());
        } catch (ClassNotFoundException e) {
            throw new Error("[TEST BUG]: jdk.test.lib.Utils not found", e);
        }
        try {
            TestCase mode = TestCase.valueOf(args[0]);
            mode.run();
        } catch (IllegalArgumentException e) {
            throw new Error("[TEST BUG]: Unknown mode " + args[0], e);
        }
    }

    private enum TestCase {
        NO_SEC_MAN,
        NO_JVMCI {
            @Override
            public Class<? extends Throwable> getExpectedException() {
                return Error.class;
            }
        },
        ALL_PERM {
            @Override
            public SecurityManager getSecurityManager() {
                return new SecurityManager() {
                    @Override
                    public void checkPermission(Permission perm) {
                    }
                };
            }
        },
        NO_PERM {
            @Override
            public SecurityManager getSecurityManager() {
                return new SecurityManager();
            }

            @Override
            public Class<? extends Throwable> getExpectedException() {
                return AccessControlException.class;
            }
        },
        NO_JVMCI_ACCESS_PERM {
            @Override
            public SecurityManager getSecurityManager() {
                return new SecurityManager() {
                    @Override
                    public void checkPermission(Permission perm) {
                        if (isJvmciPermission(perm)) {
                            super.checkPermission(perm);
                        }
                    }

                    @Override
                    public void checkPropertyAccess(String key) {
                        if (key.startsWith(JVMCI_PROP_START)) {
                            super.checkPropertyAccess(key);
                        }
                    }
                };
            }

            private boolean isJvmciPermission(Permission perm) {
                String name = perm.getName();
                boolean isJvmciRuntime = perm instanceof RuntimePermission
                        && (JVMCI_SERVICES.equals(name)
                            || name.startsWith(JVMCI_RT_PERM_START));
                boolean isJvmciProperty = perm instanceof PropertyPermission
                        && name.startsWith(JVMCI_PROP_START);
                return isJvmciRuntime || isJvmciProperty;
            }

            @Override
            public Class<? extends Throwable> getExpectedException() {
                return AccessControlException.class;
            }
        };

        public void run() {
            System.setSecurityManager(getSecurityManager());
            Consumer<Throwable> exceptionCheck = e -> {
                if (e == null) {
                    if (getExpectedException() != null) {
                        String message = name() + ": Didn't get expected exception "
                                + getExpectedException();
                        throw new AssertionError(message);
                    }
                } else {
                    String message = name() + ": Got unexpected exception "
                            + e.getClass().getSimpleName();
                    if (getExpectedException() == null){
                        throw new AssertionError(message, e);
                    }

                    Throwable t = e;
                    while (t.getCause() != null) {
                        t = t.getCause();
                    }
                    if (!getExpectedException().isAssignableFrom(t.getClass())) {
                        message += " instead of " + getExpectedException()
                                .getSimpleName();
                        throw new AssertionError(message, e);
                    }
                }
            };
            Utils.runAndCheckException(() -> {
                // CompilerToVM::<cinit> provokes CompilerToVM::<init>
                Class.forName("jdk.vm.ci.hotspot.CompilerToVMHelper");
            }, exceptionCheck);
        }

        public SecurityManager getSecurityManager() {
            return null;
        }

        public Class<? extends Throwable> getExpectedException() {
            return null;
        }

        private static final String JVMCI_RT_PERM_START
                = "accessClassInPackage.jdk.vm.ci";
        private static final String JVMCI_SERVICES = "jvmciServices";
        private static final String JVMCI_PROP_START = "jvmci.";

    }
}
