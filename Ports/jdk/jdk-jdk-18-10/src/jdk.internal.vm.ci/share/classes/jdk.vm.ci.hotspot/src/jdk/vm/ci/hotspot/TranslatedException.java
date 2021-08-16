/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Formatter;
import java.util.List;
import java.util.Objects;

/**
 * Support for translating exceptions between different runtime heaps.
 */
@SuppressWarnings("serial")
final class TranslatedException extends Exception {

    /**
     * Class name of exception that could not be instantiated.
     */
    private String originalExceptionClassName;

    private TranslatedException(String message, String originalExceptionClassName) {
        super(message);
        this.originalExceptionClassName = originalExceptionClassName;
    }

    /**
     * No need to record an initial stack trace since it will be manually overwritten.
     */
    @SuppressWarnings("sync-override")
    @Override
    public Throwable fillInStackTrace() {
        return this;
    }

    @Override
    public String toString() {
        String s;
        if (originalExceptionClassName.equals(TranslatedException.class.getName())) {
            s = getClass().getName();
        } else {
            s = getClass().getName() + "[" + originalExceptionClassName + "]";
        }
        String message = getMessage();
        return (message != null) ? (s + ": " + message) : s;
    }

    /**
     * Prints a stack trace for {@code throwable} and returns {@code true}. Used to print stack
     * traces only when assertions are enabled.
     */
    private static boolean printStackTrace(Throwable throwable) {
        throwable.printStackTrace();
        return true;
    }

    private static Throwable initCause(Throwable throwable, Throwable cause) {
        if (cause != null) {
            try {
                throwable.initCause(cause);
            } catch (IllegalStateException e) {
                // Cause could not be set or overwritten.
                assert printStackTrace(e);
            }
        }
        return throwable;
    }

    private static Throwable create(String className, String message, Throwable cause) {
        // Try create with reflection first.
        try {
            Class<?> cls = Class.forName(className);
            if (cause != null) {
                // Handle known exception types whose cause must be set in the constructor
                if (cls == InvocationTargetException.class) {
                    return new InvocationTargetException(cause, message);
                }
                if (cls == ExceptionInInitializerError.class) {
                    return new ExceptionInInitializerError(cause);
                }
            }
            if (message == null) {
                return initCause((Throwable) cls.getConstructor().newInstance(), cause);
            }
            return initCause((Throwable) cls.getDeclaredConstructor(String.class).newInstance(message), cause);
        } catch (Throwable translationFailure) {
            return initCause(new TranslatedException(message, className), cause);
        }
    }

    /**
     * Encodes an exception message to distinguish a null message from an empty message.
     *
     * @return {@code value} with a space prepended iff {@code value != null}
     */
    private static String encodeMessage(String value) {
        return value != null ? ' ' + value : value;
    }

    private static String decodeMessage(String value) {
        if (value.length() == 0) {
            return null;
        }
        return value.substring(1);
    }

    private static String encodedString(String value) {
        return Objects.toString(value, "").replace('|', '_');
    }

    /**
     * Encodes {@code throwable} including its stack and causes as a string. The encoding format of
     * a single exception is:
     *
     * <pre>
     * <exception class name> '|' <exception message> '|' <stack size> '|' [ <classLoader> '|' <module> '|' <moduleVersion> '|' <class> '|' <method> '|' <file> '|' <line> '|' ]*
     * </pre>
     *
     * Each exception is encoded before the exception it causes.
     */
    @VMEntryPoint
    static String encodeThrowable(Throwable throwable) throws Throwable {
        try {
            Formatter enc = new Formatter();
            List<Throwable> throwables = new ArrayList<>();
            for (Throwable current = throwable; current != null; current = current.getCause()) {
                throwables.add(current);
            }

            // Encode from inner most cause outwards
            Collections.reverse(throwables);

            for (Throwable current : throwables) {
                enc.format("%s|%s|", current.getClass().getName(), encodedString(encodeMessage(current.getMessage())));
                StackTraceElement[] stackTrace = current.getStackTrace();
                if (stackTrace == null) {
                    stackTrace = new StackTraceElement[0];
                }
                enc.format("%d|", stackTrace.length);
                for (int i = 0; i < stackTrace.length; i++) {
                    StackTraceElement frame = stackTrace[i];
                    if (frame != null) {
                        enc.format("%s|%s|%s|%s|%s|%s|%d|", encodedString(frame.getClassLoaderName()),
                                encodedString(frame.getModuleName()), encodedString(frame.getModuleVersion()),
                                frame.getClassName(), frame.getMethodName(),
                                encodedString(frame.getFileName()), frame.getLineNumber());
                    }
                }
            }
            return enc.toString();
        } catch (Throwable e) {
            assert printStackTrace(e);
            try {
                return e.getClass().getName() + "|" + encodedString(e.getMessage()) + "|0|";
            } catch (Throwable e2) {
                assert printStackTrace(e2);
                return "java.lang.Throwable|too many errors during encoding|0|";
            }
        }
    }

    /**
     * Gets the stack of the current thread without the frames between this call and the one just
     * below the frame of the first method in {@link CompilerToVM}. The chopped frames are specific
     * to the implementation of {@link HotSpotJVMCIRuntime#decodeThrowable(String)}.
     */
    private static StackTraceElement[] getStackTraceSuffix() {
        StackTraceElement[] stack = new Exception().getStackTrace();
        for (int i = 0; i < stack.length; i++) {
            StackTraceElement e = stack[i];
            if (e.getClassName().equals(CompilerToVM.class.getName())) {
                return Arrays.copyOfRange(stack, i, stack.length);
            }
        }
        // This should never happen but since we're in exception handling
        // code, just return a safe value instead raising a nested exception.
        return new StackTraceElement[0];
    }

    /**
     * Decodes {@code encodedThrowable} into a {@link TranslatedException}.
     *
     * @param encodedThrowable an encoded exception in the format specified by
     *            {@link #encodeThrowable}
     */
    @VMEntryPoint
    static Throwable decodeThrowable(String encodedThrowable) {
        try {
            int i = 0;
            String[] parts = encodedThrowable.split("\\|");
            Throwable cause = null;
            Throwable throwable = null;
            while (i != parts.length) {
                String exceptionClassName = parts[i++];
                String exceptionMessage = decodeMessage(parts[i++]);
                throwable = create(exceptionClassName, exceptionMessage, cause);
                int stackTraceDepth = Integer.parseInt(parts[i++]);

                StackTraceElement[] suffix = getStackTraceSuffix();
                StackTraceElement[] stackTrace = new StackTraceElement[stackTraceDepth + suffix.length];
                for (int j = 0; j < stackTraceDepth; j++) {
                    String classLoaderName = parts[i++];
                    String moduleName = parts[i++];
                    String moduleVersion = parts[i++];
                    String className = parts[i++];
                    String methodName = parts[i++];
                    String fileName = parts[i++];
                    int lineNumber = Integer.parseInt(parts[i++]);
                    if (classLoaderName.isEmpty()) {
                        classLoaderName = null;
                    }
                    if (moduleName.isEmpty()) {
                        moduleName = null;
                    }
                    if (moduleVersion.isEmpty()) {
                        moduleVersion = null;
                    }
                    if (fileName.isEmpty()) {
                        fileName = null;
                    }
                    stackTrace[j] = new StackTraceElement(classLoaderName, moduleName, moduleVersion, className, methodName, fileName, lineNumber);
                }
                System.arraycopy(suffix, 0, stackTrace, stackTraceDepth, suffix.length);
                throwable.setStackTrace(stackTrace);
                cause = throwable;
            }
            return throwable;
        } catch (Throwable translationFailure) {
            assert printStackTrace(translationFailure);
            return new TranslatedException("Error decoding exception: " + encodedThrowable, translationFailure.getClass().getName());
        }
    }
}
