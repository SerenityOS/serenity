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
package jdk.vm.ci.hotspot;

import jdk.vm.ci.code.CompilationRequest;
import jdk.vm.ci.code.CompilationRequestResult;

/**
 * HotSpot specific information about the result of a {@link CompilationRequest}.
 */
public final class HotSpotCompilationRequestResult implements CompilationRequestResult {

    /**
     * A user readable description of the failure.
     *
     * This field is read by the VM.
     */
    private final String failureMessage;

    /**
     * Whether this is a transient failure where retrying would help.
     *
     * This field is read by the VM.
     */
    private final boolean retry;

    /**
     * Number of bytecodes inlined into the compilation, exclusive of the bytecodes in the root
     * method.
     *
     * This field is read by the VM.
     */
    private final int inlinedBytecodes;

    private HotSpotCompilationRequestResult(String failureMessage, boolean retry, int inlinedBytecodes) {
        this.failureMessage = failureMessage;
        this.retry = retry;
        this.inlinedBytecodes = inlinedBytecodes;
    }

    @Override
    public Object getFailure() {
        return failureMessage;
    }

    /**
     * Creates a result representing a successful compilation.
     *
     * @param inlinedBytecodes number of bytecodes inlined into the compilation, exclusive of the
     *            bytecodes in the root method
     */
    public static HotSpotCompilationRequestResult success(int inlinedBytecodes) {
        return new HotSpotCompilationRequestResult(null, true, inlinedBytecodes);
    }

    /**
     * Creates a result representing a failed compilation.
     *
     * @param failureMessage a description of the failure
     * @param retry whether this is a transient failure where retrying may succeed
     */
    public static HotSpotCompilationRequestResult failure(String failureMessage, boolean retry) {
        return new HotSpotCompilationRequestResult(failureMessage, retry, 0);
    }

    public String getFailureMessage() {
        return failureMessage;
    }

    public boolean getRetry() {
        return retry;
    }

    public int getInlinedBytecodes() {
        return inlinedBytecodes;
    }
}
