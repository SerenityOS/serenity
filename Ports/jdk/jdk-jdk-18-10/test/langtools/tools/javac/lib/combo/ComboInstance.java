/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

package combo;

import javax.tools.StandardJavaFileManager;
import java.util.Optional;

import com.sun.tools.javac.file.BaseFileManager;

/**
 * This class is the common superclass of all combo test instances. It defines few helper methods
 * to build compilation tasks using the shared context object, as well as entry points for
 * signalling test failures.
 */
public abstract class ComboInstance<X extends ComboInstance<X>> {

    /** The test instance result status. */
    private ResultStatus resultStatus = ResultStatus.PASSED;

    /** The test instance execution environment. */
    private ComboTestHelper<X>.Env env;

    /**
     * Entry point for executing a combo test instance; first, the test environment is saved
     * in the corresponding field, then the instance is run (see {@link ComboInstance#doWork()}.
     * During execution, the result status will be updated to match the test outcome.
     */
    final void run(ComboTestHelper<X>.Env env) {
        try {
            this.env = env;
            doWork();
            if (resultStatus.isSuccess()) {
                env.info().passCount++;
            }
        } catch (Throwable ex) {
            resultStatus = ResultStatus.ERROR;
            env.info().errCount++;
            env.info().lastError = Optional.of(ex);
        } finally {
            this.env = null;
            ((BaseFileManager) env.fileManager()).clear();
        }
    }

    /**
     * Retrieve a unique ID associated with this test instance.
     */
    public int id() {
        return env.info().comboCount;
    }

    /**
     * Retrieve shared file manager.
     */
    public StandardJavaFileManager fileManager() {
        return env.fileManager();
    }

    /**
     * Create a new compilation task using shared compilation context.
     */
    protected ComboTask newCompilationTask() {
        return new ComboTask(env);
    }

    /**
     * Main test execution entry point; subclasses must implement this method to define the test
     * logic.
     */
    protected abstract void doWork() throws Throwable;

    /**
     * Report a test failure.
     */
    protected void fail() {
        //dump some default info (such as dimension bindings)
        fail("Combo instance failed; " + env.bindings);
    }

    /**
     * Report a test failure with corresponding failure message.
     */
    protected void fail(String msg) {
        resultStatus = ResultStatus.FAILED;
        env.info().failCount++;
        env.info().lastFailure = Optional.of(msg);
    }

    /**
     * The status associated with this test instance execution.
     */
    enum ResultStatus {
        /** Test passed. */
        PASSED(true),
        /** Test failed. */
        FAILED(false),
        /** Test thrown unexpected error/exception. */
        ERROR(false);

        boolean success;

        ResultStatus(boolean success) {
            this.success = success;
        }

        boolean isSuccess() {
            return success;
        }
    }
}
