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

/*
 * @test
 * @bug 8262891 8270151
 * @summary Verify pattern switches work properly when the set of sealed types changes.
 * @compile --enable-preview -source ${jdk.version} SealedTypeChanges.java
 * @compile --enable-preview -source ${jdk.version} SealedTypeChanges2.java
 * @run main/othervm --enable-preview SealedTypeChanges
 */

import java.util.function.Consumer;

public class SealedTypeChanges {

    public static void main(String... args) throws Exception {
        new SealedTypeChanges().run();
    }

    void run() throws Exception {
        doRun(this::expressionIntf, this::validateIncompatibleClassChangeError);
        doRun(this::statementIntf, this::validateIncompatibleClassChangeError);
        doRun(this::expressionCls, this::validateIncompatibleClassChangeError);
        doRun(this::statementCls, this::validateIncompatibleClassChangeError);
        doRun(this::statementFallThrough, this::validateIncompatibleClassChangeError);
        doRun(this::expressionCoveredIntf, this::validateTestException);
        doRun(this::statementCoveredIntf, this::validateTestException);
        doRun(this::expressionCoveredCls, this::validateTestException);
        doRun(this::statementCoveredCls, this::validateTestException);
    }

    <T> void doRun(Consumer<T> t, Consumer<Throwable> validateException) throws Exception {
        t.accept((T) new A());
        try {
            t.accept((T) Class.forName("SealedTypeChangesClass").newInstance());
            throw new AssertionError("Expected an exception, but none thrown.");
        } catch (Throwable ex) {
            validateException.accept(ex);
        }
    }

    void validateIncompatibleClassChangeError(Throwable t) {
        if (!(t instanceof IncompatibleClassChangeError)) {
            throw new AssertionError("Unexpected exception", t);
        }
    }

    void validateTestException(Throwable t) {
        if (!(t instanceof TestException)) {
            throw new AssertionError("Unexpected exception", t);
        }
    }

    void statementIntf(SealedTypeChangesIntf obj) {
        switch (obj) {
            case A a -> System.err.println(1);
        }
    }

    int expressionIntf(SealedTypeChangesIntf obj) {
        return switch (obj) {
            case A a -> 0;
        };
    }

    void statementCls(SealedTypeChangesCls obj) {
        switch (obj) {
            case A a -> System.err.println(1);
        }
    }

    void statementFallThrough(SealedTypeChangesCls obj) {
        switch (obj) {
            case A a: System.err.println(1);
        }
    }

    int expressionCls(SealedTypeChangesCls obj) {
        return switch (obj) {
            case A a -> 0;
        };
    }

    void statementCoveredIntf(SealedTypeChangesIntf obj) {
        switch (obj) {
            case A a -> System.err.println(1);
            case SealedTypeChangesIntf o -> throw new TestException();
        }
    }

    int expressionCoveredIntf(SealedTypeChangesIntf obj) {
        return switch (obj) {
            case A a -> 0;
            case SealedTypeChangesIntf o -> throw new TestException();
        };
    }

    void statementCoveredCls(SealedTypeChangesCls obj) {
        switch (obj) {
            case A a -> System.err.println(1);
            case SealedTypeChangesCls o -> throw new TestException();
        }
    }

    int expressionCoveredCls(SealedTypeChangesCls obj) {
        return switch (obj) {
            case A a -> 0;
            case SealedTypeChangesCls o -> throw new TestException();
        };
    }

    final static class A extends SealedTypeChangesCls implements SealedTypeChangesIntf {}
    class TestException extends RuntimeException {}
}

sealed interface SealedTypeChangesIntf permits SealedTypeChanges.A {}
sealed abstract class SealedTypeChangesCls permits SealedTypeChanges.A {}
