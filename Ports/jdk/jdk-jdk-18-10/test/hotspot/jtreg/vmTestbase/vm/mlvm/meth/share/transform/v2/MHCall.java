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

package vm.mlvm.meth.share.transform.v2;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Arrays;

import nsk.share.test.LazyObjectArrayToString;
import nsk.share.test.LazyFormatString;
import vm.mlvm.meth.share.Argument;
import vm.mlvm.meth.share.Arguments;
import vm.mlvm.meth.share.MHUtils;
import vm.mlvm.share.Env;

public class MHCall {

    private static final boolean TRACE = false;
    private static final boolean TRACE_COMPLAIN_VALUE_MISMATCHES = false;

    private final MHTF _target;
    private final Argument[] _args;
    private final Argument _retVal;
    private final MethodHandle _targetMH;

    MHCall(Argument retVal, MHTF target, MethodHandle targetMH, Argument[] args) {
        _args = args;
        _target = target;
        _retVal = retVal;

        if ( TRACE ) {
            try {
                targetMH = MethodHandles.explicitCastArguments(
                        MethodHandles.lookup().findVirtual(MHCall.class, "trace", MethodType.methodType(Object.class, MethodHandle.class, Object[].class))
                        .bindTo(this)
                        .bindTo(targetMH.asSpreader(Object[].class, targetMH.type().parameterCount()))
                        .asCollector(Object[].class, targetMH.type().parameterCount()),
                        targetMH.type());
            } catch ( Exception e ) {
                Env.complain(e, "Can't add tracing to MHCall %s", this);
            }
        }

        _targetMH = targetMH;
        Env.traceDebug("Created MHCall: %s", this);
    }

    @SuppressWarnings("unused")
    private Object trace(MethodHandle targetMH, Object[] args) throws Throwable {
        try {
            Env.traceNormal("Invoking %s\n\t\targuments=%s", _target, new LazyObjectArrayToString(args));

            for ( int i = 0; i < args.length; i++ ) {
                Object actualArg = args[i];
                Object goldenArg = _args[i].getValue();
                if ( actualArg != null && ! actualArg.equals(goldenArg) || actualArg == null && goldenArg != null ) {
                    if ( TRACE_COMPLAIN_VALUE_MISMATCHES )
                        Env.complain("\t\tArgument " + i + " mismatch: actual=%s, required=%s", actualArg, goldenArg);
                    else
                        Env.traceNormal("\t\tArgument " + i + " mismatch: actual=%s, required=%s", actualArg, goldenArg);
                }
            }

            Object result = targetMH.invoke((Object[]) args);

            Env.traceNormal("Returning from %s\n\t\tresult=%s", _target, result);

            Object requiredRetVal = _retVal.getValue();
            if ( result != null && ! result.equals(requiredRetVal) || result == null && requiredRetVal != null ) {
                if ( TRACE_COMPLAIN_VALUE_MISMATCHES )
                    Env.complain("\t\tResult mismatch: actual=%s, required=%s", result, requiredRetVal);
                else
                    Env.traceNormal("\t\tResult mismatch: actual=%s, required=%s", result, requiredRetVal);
            }

            return result;
        } catch ( Throwable t ){
            Env.traceNormal(t, "Exception caught after calling %s", _target);
            throw t;
        }
    }

    public Argument getRetVal() {
        return _retVal;
    }

    /**
     * @return May return null if target is not a transformation (but, say, a user's MH)
     */
    public MHTF getTarget() {
        return _target;
    }

    public MethodHandle getTargetMH() {
        return _targetMH;
    }

    public Argument[] getArgs() {
        return _args;
    }

    public void check() throws IllegalArgumentException {
        MethodType mt = _targetMH.type();
        for ( int i = 0; i < mt.parameterCount(); i++ ) {
            MHUtils.assertAssignableType(new LazyFormatString("argument %i in %s", i, this), mt.parameterType(i), _args[i].getType());
        }

        MHUtils.assertAssignableType(new LazyFormatString("return type in %s", this), mt.returnType(), _retVal.getType());
    }

    public Object call() throws Throwable {
        if ( ! _retVal.getType().equals(void.class) )
            return (Object) _targetMH.invokeWithArguments(Arguments.valuesArray(_args));
        else {
            _targetMH.invokeWithArguments(Arguments.valuesArray(_args));
            return null;
        }
    }

    public Object callAndCheckRetVal() throws Throwable {
        Object r = call();

        if ( ! _retVal.getType().equals(void.class) && ! r.equals(_retVal.getValue()) )
            throw new IllegalArgumentException("Call returned wrong value: "
                    + "actual=" + r
                    + "; expected=" + _retVal.getValue());
        return r;
    }

    @Override
    public String toString() {
        return "MHCall: target=" + _target + "; args=" + Arrays.toString(_args) + "; retVal=" + _retVal + "; targetMH=" + _targetMH;
    }

    String prettyPrint(String topPrefix, String subPrefix) {
        return topPrefix + "MHCall:   target = " + _target + "\n"
             + subPrefix + "       arguments = " + Arrays.toString(_args) + "\n"
             + subPrefix + "       retVal    = " + _retVal + "\n"
             + subPrefix + "       targetMH  = " + _targetMH;
    }
}
