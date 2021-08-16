/*
 * Copyright (c) 2011, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

package java.lang.invoke;

import jdk.internal.misc.CDS;
import jdk.internal.misc.Unsafe;
import jdk.internal.misc.VM;
import sun.security.action.GetPropertyAction;

import java.util.Properties;

import static java.lang.invoke.LambdaForm.basicTypeSignature;
import static java.lang.invoke.LambdaForm.shortenSignature;

/**
 * This class consists exclusively of static names internal to the
 * method handle implementation.
 * Usage:  {@code import static java.lang.invoke.MethodHandleStatics.*}
 * @author John Rose, JSR 292 EG
 */
/*non-public*/
class MethodHandleStatics {

    private MethodHandleStatics() { }  // do not instantiate

    static final Unsafe UNSAFE = Unsafe.getUnsafe();
    static final int CLASSFILE_VERSION = VM.classFileVersion();

    static final boolean DEBUG_METHOD_HANDLE_NAMES;
    static final boolean DUMP_CLASS_FILES;
    static final boolean TRACE_INTERPRETER;
    static final boolean TRACE_METHOD_LINKAGE;
    static final boolean TRACE_RESOLVE;
    static final int COMPILE_THRESHOLD;
    static final boolean LOG_LF_COMPILATION_FAILURE;
    static final int DONT_INLINE_THRESHOLD;
    static final int PROFILE_LEVEL;
    static final boolean PROFILE_GWT;
    static final int CUSTOMIZE_THRESHOLD;
    static final boolean VAR_HANDLE_GUARDS;
    static final int MAX_ARITY;
    static final boolean VAR_HANDLE_IDENTITY_ADAPT;

    static {
        Properties props = GetPropertyAction.privilegedGetProperties();
        DEBUG_METHOD_HANDLE_NAMES = Boolean.parseBoolean(
                props.getProperty("java.lang.invoke.MethodHandle.DEBUG_NAMES"));
        DUMP_CLASS_FILES = Boolean.parseBoolean(
                props.getProperty("java.lang.invoke.MethodHandle.DUMP_CLASS_FILES"));
        TRACE_INTERPRETER = Boolean.parseBoolean(
                props.getProperty("java.lang.invoke.MethodHandle.TRACE_INTERPRETER"));
        TRACE_METHOD_LINKAGE = Boolean.parseBoolean(
                props.getProperty("java.lang.invoke.MethodHandle.TRACE_METHOD_LINKAGE"));
        TRACE_RESOLVE = Boolean.parseBoolean(
                props.getProperty("java.lang.invoke.MethodHandle.TRACE_RESOLVE"));
        COMPILE_THRESHOLD = Integer.parseInt(
                props.getProperty("java.lang.invoke.MethodHandle.COMPILE_THRESHOLD", "0"));
        LOG_LF_COMPILATION_FAILURE = Boolean.parseBoolean(
                props.getProperty("java.lang.invoke.MethodHandle.LOG_LF_COMPILATION_FAILURE", "false"));
        DONT_INLINE_THRESHOLD = Integer.parseInt(
                props.getProperty("java.lang.invoke.MethodHandle.DONT_INLINE_THRESHOLD", "30"));
        PROFILE_LEVEL = Integer.parseInt(
                props.getProperty("java.lang.invoke.MethodHandle.PROFILE_LEVEL", "0"));
        PROFILE_GWT = Boolean.parseBoolean(
                props.getProperty("java.lang.invoke.MethodHandle.PROFILE_GWT", "true"));
        CUSTOMIZE_THRESHOLD = Integer.parseInt(
                props.getProperty("java.lang.invoke.MethodHandle.CUSTOMIZE_THRESHOLD", "127"));
        VAR_HANDLE_GUARDS = Boolean.parseBoolean(
                props.getProperty("java.lang.invoke.VarHandle.VAR_HANDLE_GUARDS", "true"));
        VAR_HANDLE_IDENTITY_ADAPT = Boolean.parseBoolean(
                props.getProperty("java.lang.invoke.VarHandle.VAR_HANDLE_IDENTITY_ADAPT", "false"));

        // Do not adjust this except for special platforms:
        MAX_ARITY = Integer.parseInt(
                props.getProperty("java.lang.invoke.MethodHandleImpl.MAX_ARITY", "255"));

        if (CUSTOMIZE_THRESHOLD < -1 || CUSTOMIZE_THRESHOLD > 127) {
            throw newInternalError("CUSTOMIZE_THRESHOLD should be in [-1...127] range");
        }
    }

    /** Tell if any of the debugging switches are turned on.
     *  If this is the case, it is reasonable to perform extra checks or save extra information.
     */
    /*non-public*/
    static boolean debugEnabled() {
        return (DEBUG_METHOD_HANDLE_NAMES |
                DUMP_CLASS_FILES |
                TRACE_INTERPRETER |
                TRACE_METHOD_LINKAGE |
                LOG_LF_COMPILATION_FAILURE);
    }

    /**
     * If requested, logs the result of resolving the LambdaForm to stdout
     * and informs the CDS subsystem about it.
     */
    /*non-public*/
    static void traceLambdaForm(String name, MethodType type, Class<?> holder, MemberName resolvedMember) {
        if (TRACE_RESOLVE) {
            System.out.println("[LF_RESOLVE] " + holder.getName() + " " + name + " " +
                    shortenSignature(basicTypeSignature(type)) +
                    (resolvedMember != null ? " (success)" : " (fail)"));
        }
        if (CDS.isDumpingClassList()) {
            CDS.traceLambdaFormInvoker("[LF_RESOLVE]", holder.getName(), name, shortenSignature(basicTypeSignature(type)));
        }
    }

    /**
     * If requested, logs the result of resolving the species type to stdout
     * and the CDS subsystem.
     */
    /*non-public*/
    static void traceSpeciesType(String cn, Class<?> salvage) {
        if (TRACE_RESOLVE) {
            System.out.println("[SPECIES_RESOLVE] " + cn + (salvage != null ? " (salvaged)" : " (generated)"));
        }
        if (CDS.isDumpingClassList()) {
            CDS.traceSpeciesType("[SPECIES_RESOLVE]", cn);
        }
    }
    // handy shared exception makers (they simplify the common case code)
    /*non-public*/
    static InternalError newInternalError(String message) {
        return new InternalError(message);
    }
    /*non-public*/
    static InternalError newInternalError(String message, Exception cause) {
        return new InternalError(message, cause);
    }
    /*non-public*/
    static InternalError newInternalError(Exception cause) {
        return new InternalError(cause);
    }
    /*non-public*/
    static RuntimeException newIllegalStateException(String message) {
        return new IllegalStateException(message);
    }
    /*non-public*/
    static RuntimeException newIllegalStateException(String message, Object obj) {
        return new IllegalStateException(message(message, obj));
    }
    /*non-public*/
    static RuntimeException newIllegalArgumentException(String message) {
        return new IllegalArgumentException(message);
    }
    /*non-public*/
    static RuntimeException newIllegalArgumentException(String message, Object obj) {
        return new IllegalArgumentException(message(message, obj));
    }
    /*non-public*/
    static RuntimeException newIllegalArgumentException(String message, Object obj, Object obj2) {
        return new IllegalArgumentException(message(message, obj, obj2));
    }
    /** Propagate unchecked exceptions and errors, but wrap anything checked and throw that instead. */
    /*non-public*/
    static Error uncaughtException(Throwable ex) {
        if (ex instanceof Error)  throw (Error) ex;
        if (ex instanceof RuntimeException)  throw (RuntimeException) ex;
        throw new InternalError("uncaught exception", ex);
    }
    private static String message(String message, Object obj) {
        if (obj != null)  message = message + ": " + obj;
        return message;
    }
    private static String message(String message, Object obj, Object obj2) {
        if (obj != null || obj2 != null)  message = message + ": " + obj + ", " + obj2;
        return message;
    }
}
