/*
 * Copyright (c) 2011, 2018, Oracle and/or its affiliates. All rights reserved.
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

package vm.mlvm.share.jpda;

import vm.mlvm.share.Env;
import vm.mlvm.share.MlvmTest;
import vm.share.options.Option;

public abstract class Debuggee extends MlvmTest {

    @Option(name="debuggee.iterations", default_value="1", description="Iterations to run on debuggee")
    public long _iterations = 1;

    @Option(name="debuggee.hangAt", default_value="", description="Hang up in specified point")
    public String _hangAt = "";

    static {
        setName("Debuggee");
    }

    private boolean _isWarmingUp;

    public final boolean isWarmingUp() {
        return _isWarmingUp;
    }

    public final long getWarmupsCount() {
        return _iterations;
    }

    /**
     * Used in static methods for getting access to Debuggee instance
     *
     * @return The current debuggee instance (there should be only one)
     */
    public static Debuggee getDebuggeeInstance() {
        return (Debuggee) MlvmTest.getInstance();
    }

    @Override
    public boolean run() throws Throwable {
        startUp();
        boolean result = false;
        try {

            _isWarmingUp = true;
            Env.traceNormal("Warming up (" + _iterations + " iterations)");
            for (long w = _iterations - 1; w > 0; w--)
                warmUp();
            _isWarmingUp = false;

            Env.traceNormal("Starting main test");
            result = runDebuggee();

        } finally {
            tearDown();
        }

        return result;
    }

    protected void startUp() throws Throwable {
    }

    protected void warmUp() throws Throwable {
    }

    protected abstract boolean runDebuggee() throws Throwable;

    protected void tearDown() throws Throwable {
    }

    public final void hangUpIfNeeded(String at) throws InterruptedException {
        if (!_isWarmingUp && at.equals(_hangAt)) {
            Env.traceNormal("Hanging at " + at);
            hangupImpl();
        } else {
            if ( isWarmingUp() )
                Env.traceDebug("Passing " + at);
            else
                Env.traceNormal("Passing " + at);
        }
    }

    protected void hangupImpl() throws InterruptedException {
        for (;;)
            Thread.sleep(60000);
    }
}
