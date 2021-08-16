/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

package ir_framework.tests;

import compiler.lib.ir_framework.*;

/*
 * @test
 * @requires vm.flagless
 * @summary Test different access modifiers an make sure, the framework can access all methods.
 * @library /test/lib /
 * @run driver ir_framework.tests.TestAccessModifiers
 */

public class TestAccessModifiers {
    public static void main(String[] args) {
        TestFramework.run(PackagePrivate.class);
    }
}

class PackagePrivate {
    @Test
    public void test() {
    }

    @Test
    @Arguments(Argument.DEFAULT)
    public void test2(int x) {
    }

    @Test
    public static int staticPublicPrivate() {
        return 42;
    }

    @Check(test = "staticPublicPrivate")
    private void staticPublicPrivateCheck(int retValue) {
        if (retValue != 42) {
            throw new RuntimeException("Needs to be 42");
        }
    }

    @Test
    protected static int staticProtectedPrivate() {
        return 42;
    }

    @Check(test = "staticProtectedPrivate")
    private void staticProtectedPrivateCheck(int retValue) {
        if (retValue != 42) {
            throw new RuntimeException("Needs to be 42");
        }
    }

    @Test
    static int staticDefaultPrivate() {
        return 42;
    }

    @Check(test = "staticDefaultPrivate")
    private void staticDefaultPrivateCheck(int retValue) {
        if (retValue != 42) {
            throw new RuntimeException("Needs to be 42");
        }
    }

    @Test
    private static int staticPrivatePrivate() {
        return 42;
    }

    @Check(test = "staticPrivatePrivate")
    private void staticPrivatePrivateCheck(int retValue) {
        if (retValue != 42) {
            throw new RuntimeException("Needs to be 42");
        }
    }

    @Test
    public static int staticPublicDefault() {
        return 42;
    }

    @Check(test = "staticPublicDefault")
    void staticPublicDefaultCheck(int retValue) {
        if (retValue != 42) {
            throw new RuntimeException("Needs to be 42");
        }
    }

    @Test
    protected static int staticProtectedDefault() {
        return 42;
    }

    @Check(test = "staticProtectedDefault")
    void staticProtectedDefaultCheck(int retValue) {
        if (retValue != 42) {
            throw new RuntimeException("Needs to be 42");
        }
    }

    @Test
    static int staticDefaultDefault() {
        return 42;
    }

    @Check(test = "staticDefaultDefault")
    void staticDefaultDefaultCheck(int retValue) {
        if (retValue != 42) {
            throw new RuntimeException("Needs to be 42");
        }
    }

    @Test
    private static int staticPrivateDefault() {
        return 42;
    }

    @Check(test = "staticPrivateDefault")
    void staticPrivateDefaultCheck(int retValue) {
        if (retValue != 42) {
            throw new RuntimeException("Needs to be 42");
        }
    }

    @Test
    public static int staticPublicProtected() {
        return 42;
    }

    @Check(test = "staticPublicProtected")
    protected void staticPublicProtectedCheck(int retValue) {
        if (retValue != 42) {
            throw new RuntimeException("Needs to be 42");
        }
    }

    @Test
    protected static int staticProtectedProtected() {
        return 42;
    }

    @Check(test = "staticProtectedProtected")
    protected void staticProtectedProtectedCheck(int retValue) {
        if (retValue != 42) {
            throw new RuntimeException("Needs to be 42");
        }
    }

    @Test
    static int staticDefaultProtected() {
        return 42;
    }

    @Check(test = "staticDefaultProtected")
    protected void staticDefaultProtectedCheck(int retValue) {
        if (retValue != 42) {
            throw new RuntimeException("Needs to be 42");
        }
    }

    @Test
    private static int staticPrivateProtected() {
        return 42;
    }

    @Check(test = "staticPrivateProtected")
    protected void staticPrivateProtectedCheck(int retValue) {
        if (retValue != 42) {
            throw new RuntimeException("Needs to be 42");
        }
    }

    @Test
    public static int staticPublicPublic() {
        return 42;
    }

    @Check(test = "staticPublicPublic")
    public void staticPublicPublicCheck(int retValue) {
        if (retValue != 42) {
            throw new RuntimeException("Needs to be 42");
        }
    }

    @Test
    protected static int staticProtectedPublic() {
        return 42;
    }

    @Check(test = "staticProtectedPublic")
    public void staticProtectedPublicCheck(int retValue) {
        if (retValue != 42) {
            throw new RuntimeException("Needs to be 42");
        }
    }

    @Test
    static int staticDefaultPublic() {
        return 42;
    }

    @Check(test = "staticDefaultPublic")
    public void staticDefaultPublicCheck(int retValue) {
        if (retValue != 42) {
            throw new RuntimeException("Needs to be 42");
        }
    }

    @Test
    private static int staticPrivatePublic() {
        return 42;
    }

    @Check(test = "staticPrivatePublic")
    public void staticPrivatePublicCheck(int retValue) {
        if (retValue != 42) {
            throw new RuntimeException("Needs to be 42");
        }
    }

    @Test
    static int staticDefaultPrivate2() {
        return 42;
    }

    @Run(test = "staticDefaultPrivate2")
    private void staticDefaultPrivateRun() {
        int retValue = staticDefaultPrivate2();
        if (retValue != 42) {
            throw new RuntimeException("Needs to be 42");
        }
    }

    @Test
    private static int staticPrivatePrivate2() {
        return 42;
    }

    @Run(test = "staticPrivatePrivate2")
    private void staticPrivatePrivateRun() {
        int retValue = staticPrivatePrivate2();
        if (retValue != 42) {
            throw new RuntimeException("Needs to be 42");
        }
    }

    @Test
    public static int staticPublicDefault2() {
        return 42;
    }

    @Run(test = "staticPublicDefault2")
    void staticPublicDefaultRun() {
        int retValue = staticPublicDefault2();
        if (retValue != 42) {
            throw new RuntimeException("Needs to be 42");
        }
    }

    @Test
    protected static int staticProtectedDefault2() {
        return 42;
    }

    @Run(test = "staticProtectedDefault2")
    void staticProtectedDefaultRun() {
        int retValue = staticProtectedDefault2();
        if (retValue != 42) {
            throw new RuntimeException("Needs to be 42");
        }
    }

    @Test
    static int staticDefaultDefault2() {
        return 42;
    }

    @Run(test = "staticDefaultDefault2")
    void staticDefaultDefaultRun() {
        int retValue = staticDefaultDefault2();
        if (retValue != 42) {
            throw new RuntimeException("Needs to be 42");
        }
    }

    @Test
    private static int staticPrivateDefault2() {
        return 42;
    }

    @Run(test = "staticPrivateDefault2")
    void staticPrivateDefaultRun() {
        int retValue = staticPrivateDefault2();
        if (retValue != 42) {
            throw new RuntimeException("Needs to be 42");
        }
    }

    @Test
    public static int staticPublicProtected2() {
        return 42;
    }

    @Run(test = "staticPublicProtected2")
    protected void staticPublicProtectedRun() {
        int retValue = staticPublicProtected2();
        if (retValue != 42) {
            throw new RuntimeException("Needs to be 42");
        }
    }

    @Test
    protected static int staticProtectedProtected2() {
        return 42;
    }

    @Run(test = "staticProtectedProtected2")
    protected void staticProtectedProtectedRun() {
        int retValue = staticProtectedProtected2();
        if (retValue != 42) {
            throw new RuntimeException("Needs to be 42");
        }
    }

    @Test
    static int staticDefaultProtected2() {
        return 42;
    }

    @Run(test = "staticDefaultProtected2")
    protected void staticDefaultProtectedRun() {
        int retValue = staticDefaultProtected2();
        if (retValue != 42) {
            throw new RuntimeException("Needs to be 42");
        }
    }

    @Test
    private static int staticPrivateProtected2() {
        return 42;
    }

    @Run(test = "staticPrivateProtected2")
    protected void staticPrivateProtectedRun() {
        int retValue = staticPrivateProtected2();
        if (retValue != 42) {
            throw new RuntimeException("Needs to be 42");
        }
    }

    @Test
    public static int staticPublicPublic2() {
        return 42;
    }

    @Run(test = "staticPublicPublic2")
    public void staticPublicPublicRun() {
        int retValue = staticPublicPublic2();
        if (retValue != 42) {
            throw new RuntimeException("Needs to be 42");
        }
    }

    @Test
    protected static int staticProtectedPublic2() {
        return 42;
    }

    @Run(test = "staticProtectedPublic2")
    public void staticProtectedPublicRun() {
        int retValue = staticProtectedPublic2();
        if (retValue != 42) {
            throw new RuntimeException("Needs to be 42");
        }
    }

    @Test
    static int staticDefaultPublic2() {
        return 42;
    }

    @Run(test = "staticDefaultPublic2")
    public void staticDefaultPublicRun() {
        int retValue = staticDefaultPublic2();
        if (retValue != 42) {
            throw new RuntimeException("Needs to be 42");
        }
    }

    @Test
    private static int staticPrivatePublic2() {
        return 42;
    }

    @Run(test = "staticPrivatePublic2")
    public void staticPrivatePublicRun() {
        int retValue = staticPrivatePublic2();
        if (retValue != 42) {
            throw new RuntimeException("Needs to be 42");
        }
    }


}
