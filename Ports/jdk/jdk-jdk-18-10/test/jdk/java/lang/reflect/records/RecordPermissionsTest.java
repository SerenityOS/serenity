/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8235369
 * @summary Security manager checks for record related core reflection
 * @compile RecordPermissionsTest.java
 * @run testng/othervm/java.security.policy=allPermissions.policy RecordPermissionsTest
 */

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.Permission;
import java.security.Permissions;
import java.security.PrivilegedAction;
import java.security.ProtectionDomain;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;
import org.testng.annotations.*;
import org.testng.annotations.Test;
import static java.lang.System.out;
import static org.testng.Assert.*;

public class RecordPermissionsTest {

    class C { }
    record R1() { }
    record R2(int i, int j) { }
    record R3(List<String> ls) { }
    record R4(R1 r1, R2 r2, R3 r3) { }
    record R5(String... args) { }
    record R6(long l, String... args) implements java.io.Serializable { }

    /** A list of classes loaded by a different loader to that of this class' loader. */
    static final List<Class<?>> OTHER_LOADER_CLASSES = otherLoaderClasses();

    /** A list of classes loaded by the same loader as this class' loader. */
    static final List<Class<?>> SAME_LOADER_CLASSES = sameLoaderClasses();

    static List<Class<?>> otherLoaderClasses() {
        try {
            ClassLoader cl = new URLClassLoader(
                "other-classloader",
                new URL[]{Path.of(System.getProperty("test.classes")).toUri().toURL()},
                ClassLoader.getPlatformClassLoader()); // skip the system classloader

            return List.of(cl.loadClass("RecordPermissionsTest$R1"),
                           cl.loadClass("RecordPermissionsTest$R2"),
                           Record.class,
                           Record[].class,
                           byte.class,
                           byte[].class);
        } catch (Exception e) {
            throw new AssertionError("should not reach here", e);
        }
    }

    static List<Class<?>> sameLoaderClasses() {
        return List.of(C.class,
                       R3.class,
                       R4.class,
                       R5.class,
                       R6.class);
    }

    @BeforeTest
    public void setup() throws Exception {
        assertTrue(System.getSecurityManager() != null);
    }

    @DataProvider(name = "isRecordScenarios")
    public Object[][] isRecordScenarios() {
         return Stream.of(OTHER_LOADER_CLASSES, SAME_LOADER_CLASSES)
                      .flatMap(Collection::stream)
                      .map(cls -> new Object[]{"isRecord-" + cls.getName(),
                                               (PrivilegedAction<?>)cls::isRecord })
                      .toArray(Object[][]::new);
     }

     @DataProvider(name = "otherGetRecordComponentsScenarios")
     public Object[][] otherGetRecordComponentsScenarios() {
         return OTHER_LOADER_CLASSES.stream()
                     .map(cls -> new Object[]{"getRecordComponents-other-" + cls.getName(),
                                             (PrivilegedAction<?>)cls::getRecordComponents })
                     .toArray(Object[][]::new);
    }

    @DataProvider(name = "sameGetRecordComponentsScenarios")
    public Object[][] sameGetRecordComponentsScenarios() {
        return SAME_LOADER_CLASSES.stream()
                     .map(cls -> new Object[]{"getRecordComponents-same-" + cls.getName(),
                                             (PrivilegedAction<?>)cls::getRecordComponents })
                     .toArray(Object[][]::new);
    }

    @DataProvider(name = "allScenarios")
    public Object[][] allScenarios() {
        return Stream.of(isRecordScenarios(),
                         sameGetRecordComponentsScenarios(),
                         otherGetRecordComponentsScenarios())
                     .flatMap(Arrays::stream)
                     .toArray(Object[][]::new);
    }

    @DataProvider(name = "allNonThrowingScenarios")
    public Object[][] allNonThrowingScenarios() {
        return Stream.of(isRecordScenarios(),
                         sameGetRecordComponentsScenarios())
                     .flatMap(Arrays::stream)
                     .toArray(Object[][]::new);
    }

    /** Tests all scenarios without any security manager - sanity. */
    @Test(dataProvider = "allScenarios")
    public void testWithNoSecurityManager(String description,
                                          PrivilegedAction<?> action) {
        System.setSecurityManager(null);
        try {
            AccessController.doPrivileged(action);
        } finally {
            System.setSecurityManager(new SecurityManager());
        }
    }

    /** Tests all scenarios with all permissions. */
    @Test(dataProvider = "allScenarios")
    public void testWithAllPermissions(String description,
                                       PrivilegedAction<?> action) {
        // Run with all permissions, i.e. no further restrictions than test's AllPermission
        assert System.getSecurityManager() != null;
        AccessController.doPrivileged(action);
    }

    /** Tests given scenarios with no permissions - expect should not require any. */
    @Test(dataProvider = "allNonThrowingScenarios")
    public void testWithNoPermissionsPass(String description,
                                          PrivilegedAction<?> action) {
        assert System.getSecurityManager() != null;
        AccessController.doPrivileged(action, noPermissions());
    }

    static Class<SecurityException> SE = SecurityException.class;

    /**
     * Tests getRecordComponents with no permissions, and classes
     * loaded by a loader other than the test class' loader - expects
     * security exception.
     */
    @Test(dataProvider = "otherGetRecordComponentsScenarios")
    public void testWithNoPermissionsFail(String description,
                                          PrivilegedAction<?> action) {
        // Run with NO permissions, i.e. expect SecurityException
        assert System.getSecurityManager() != null;
        SecurityException se = expectThrows(SE, () -> AccessController.doPrivileged(action, noPermissions()));
        out.println("Got expected SecurityException: " + se);
    }

    /**
     * Tests getRecordComponents with minimal permissions, and classes
     * loaded by a loader other than the test class' loader.
     */
    @Test(dataProvider = "otherGetRecordComponentsScenarios")
    public void testWithMinimalPermissions(String description,
                                           PrivilegedAction<?> action) {
        // Run with minimal permissions, i.e. just what is required
        assert System.getSecurityManager() != null;
        AccessControlContext minimalACC = withPermissions(
                new RuntimePermission("accessDeclaredMembers"),
                new RuntimePermission("accessClassInPackage.*")
        );
        AccessController.doPrivileged(action, minimalACC);
    }

    static AccessControlContext withPermissions(Permission... perms) {
        Permissions p = new Permissions();
        for (Permission perm : perms) {
            p.add(perm);
        }
        ProtectionDomain pd = new ProtectionDomain(null, p);
        return new AccessControlContext(new ProtectionDomain[]{ pd });
    }

    static AccessControlContext noPermissions() {
        return withPermissions(/*empty*/);
    }
}
