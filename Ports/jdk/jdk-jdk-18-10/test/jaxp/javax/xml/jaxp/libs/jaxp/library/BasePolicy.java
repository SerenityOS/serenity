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
package jaxp.library;

import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

/**
 * This policy includes default permissions.
 * It should be used as one listener: either TestListener or MethodListener.
 */
public class BasePolicy implements ITestListener {

    @Override
    public void onFinish(ITestContext arg0) {
        try {
            JAXPPolicyManager.teardownPolicyManager();
        } catch (Exception e) {
            throw new RuntimeException("Failed to teardown the policy manager", e);
        }
    }

    @Override
    public void onStart(ITestContext arg0) {
        // suppose to only run othervm mode
        if (isRunWithSecurityManager())
            JAXPPolicyManager.getJAXPPolicyManager(true);
    }

    @Override
    public void onTestFailedButWithinSuccessPercentage(ITestResult arg0) {
    }

    @Override
    public void onTestFailure(ITestResult arg0) {
    }

    @Override
    public void onTestSkipped(ITestResult arg0) {
    }

    @Override
    public void onTestStart(ITestResult arg0) {
    }

    @Override
    public void onTestSuccess(ITestResult arg0) {
    }

    protected boolean isRunWithSecurityManager() {
        final String runSecMngr = JAXPTestUtilities.getSystemProperty("runSecMngr");
        return runSecMngr != null && runSecMngr.equals("true");
    }
}
