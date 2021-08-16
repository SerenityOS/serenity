/*
 * Copyright (c) 2003, 2020, Oracle and/or its affiliates. All rights reserved.
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

/*
 * Copyright 2003 Wily Technology, Inc.
 */

#include    <jni.h>
#include    <jvmti.h>

#include    "JPLISAssert.h"
#include    "Utilities.h"
#include    "JavaExceptions.h"

/**
 * This module contains utility routines for manipulating Java throwables
 * and JNIEnv throwable state from native code.
 */

static jthrowable   sFallbackInternalError  = NULL;

/*
 * Local forward declarations.
 */

/* insist on having a throwable. If we already have one, return it.
 * If not, map to fallback
 */
jthrowable
forceFallback(jthrowable potentialException);


jthrowable
forceFallback(jthrowable potentialException) {
    if ( potentialException == NULL ) {
        return sFallbackInternalError;
    }
    else {
        return potentialException;
    }
}

/**
 *  Returns true if it properly sets up a fallback exception
 */
jboolean
initializeFallbackError(JNIEnv* jnienv) {
    jplis_assert(isSafeForJNICalls(jnienv));
    sFallbackInternalError = createInternalError(jnienv, NULL);
    jplis_assert(isSafeForJNICalls(jnienv));
    return (sFallbackInternalError != NULL);
}


/*
 *  Map everything to InternalError.
 */
jthrowable
mapAllCheckedToInternalErrorMapper( JNIEnv *    jnienv,
                                    jthrowable  throwableToMap) {
    jthrowable  mappedThrowable = NULL;
    jstring     message         = NULL;

    jplis_assert(throwableToMap != NULL);
    jplis_assert(isSafeForJNICalls(jnienv));
    jplis_assert(!isUnchecked(jnienv, throwableToMap));

    message = getMessageFromThrowable(jnienv, throwableToMap);
    mappedThrowable = createInternalError(jnienv, message);

    jplis_assert(isSafeForJNICalls(jnienv));
    return mappedThrowable;
}


jboolean
checkForThrowable(  JNIEnv*     jnienv) {
    return (*jnienv)->ExceptionCheck(jnienv);
}

jboolean
isSafeForJNICalls(  JNIEnv * jnienv) {
    return !(*jnienv)->ExceptionCheck(jnienv);
}


void
logThrowable(   JNIEnv * jnienv) {
    if ( checkForThrowable(jnienv) ) {
        (*jnienv)->ExceptionDescribe(jnienv);
    }
}



/**
 *  Creates an exception or error with the fully qualified classname (ie java/lang/Error)
 *  and message passed to its constructor
 */
jthrowable
createThrowable(    JNIEnv *        jnienv,
                    const char *    className,
                    jstring         message) {
    jthrowable  exception           = NULL;
    jmethodID   constructor         = NULL;
    jclass      exceptionClass      = NULL;
    jboolean    errorOutstanding    = JNI_FALSE;

    jplis_assert(className != NULL);
    jplis_assert(isSafeForJNICalls(jnienv));

    /* create new VMError with message from exception */
    exceptionClass = (*jnienv)->FindClass(jnienv, className);
    errorOutstanding = checkForAndClearThrowable(jnienv);
    jplis_assert(!errorOutstanding);

    if (!errorOutstanding) {
        constructor = (*jnienv)->GetMethodID(   jnienv,
                                                exceptionClass,
                                                "<init>",
                                                "(Ljava/lang/String;)V");
        errorOutstanding = checkForAndClearThrowable(jnienv);
        jplis_assert(!errorOutstanding);
    }

    if (!errorOutstanding) {
        exception = (*jnienv)->NewObject(jnienv, exceptionClass, constructor, message);
        errorOutstanding = checkForAndClearThrowable(jnienv);
        jplis_assert(!errorOutstanding);
    }

    jplis_assert(isSafeForJNICalls(jnienv));
    return exception;
}

jthrowable
createInternalError(JNIEnv * jnienv, jstring message) {
    return createThrowable( jnienv,
                            "java/lang/InternalError",
                            message);
}

jthrowable
createThrowableFromJVMTIErrorCode(JNIEnv * jnienv, jvmtiError errorCode) {
    const char * throwableClassName = NULL;
    const char * message            = NULL;
    jstring messageString           = NULL;

    switch ( errorCode ) {
        case JVMTI_ERROR_NULL_POINTER:
                throwableClassName = "java/lang/NullPointerException";
                break;

        case JVMTI_ERROR_ILLEGAL_ARGUMENT:
                throwableClassName = "java/lang/IllegalArgumentException";
                break;

        case JVMTI_ERROR_OUT_OF_MEMORY:
                throwableClassName = "java/lang/OutOfMemoryError";
                break;

        case JVMTI_ERROR_CIRCULAR_CLASS_DEFINITION:
                throwableClassName = "java/lang/ClassCircularityError";
                break;

        case JVMTI_ERROR_FAILS_VERIFICATION:
                throwableClassName = "java/lang/VerifyError";
                break;

        case JVMTI_ERROR_UNSUPPORTED_REDEFINITION_METHOD_ADDED:
                throwableClassName = "java/lang/UnsupportedOperationException";
                message = "class redefinition failed: attempted to add a method";
                break;

        case JVMTI_ERROR_UNSUPPORTED_REDEFINITION_SCHEMA_CHANGED:
                throwableClassName = "java/lang/UnsupportedOperationException";
                message = "class redefinition failed: attempted to change the schema (add/remove fields)";
                break;

        case JVMTI_ERROR_UNSUPPORTED_REDEFINITION_HIERARCHY_CHANGED:
                throwableClassName = "java/lang/UnsupportedOperationException";
                message = "class redefinition failed: attempted to change superclass or interfaces";
                break;

        case JVMTI_ERROR_UNSUPPORTED_REDEFINITION_METHOD_DELETED:
                throwableClassName = "java/lang/UnsupportedOperationException";
                message = "class redefinition failed: attempted to delete a method";
                break;

        case JVMTI_ERROR_UNSUPPORTED_REDEFINITION_CLASS_MODIFIERS_CHANGED:
                throwableClassName = "java/lang/UnsupportedOperationException";
                message = "class redefinition failed: attempted to change the class modifiers";
                break;

        case JVMTI_ERROR_UNSUPPORTED_REDEFINITION_CLASS_ATTRIBUTE_CHANGED:
                throwableClassName = "java/lang/UnsupportedOperationException";
                message = "class redefinition failed: attempted to change the class NestHost, NestMembers, Record, or PermittedSubclasses attribute";
                break;

        case JVMTI_ERROR_UNSUPPORTED_REDEFINITION_METHOD_MODIFIERS_CHANGED:
                throwableClassName = "java/lang/UnsupportedOperationException";
                message = "class redefinition failed: attempted to change method modifiers";
                break;

        case JVMTI_ERROR_UNSUPPORTED_VERSION:
                throwableClassName = "java/lang/UnsupportedClassVersionError";
                break;

        case JVMTI_ERROR_NAMES_DONT_MATCH:
                throwableClassName = "java/lang/NoClassDefFoundError";
                message = "class names don't match";
                break;

        case JVMTI_ERROR_INVALID_CLASS_FORMAT:
                throwableClassName = "java/lang/ClassFormatError";
                break;

        case JVMTI_ERROR_UNMODIFIABLE_CLASS:
                throwableClassName = "java/lang/instrument/UnmodifiableClassException";
                break;

        case JVMTI_ERROR_INVALID_CLASS:
                throwableClassName = "java/lang/InternalError";
                message = "class redefinition failed: invalid class";
                break;

        case JVMTI_ERROR_CLASS_LOADER_UNSUPPORTED:
                throwableClassName = "java/lang/UnsupportedOperationException";
                message = "unsupported operation";
                break;

        case JVMTI_ERROR_INTERNAL:
        default:
                throwableClassName = "java/lang/InternalError";
                break;
        }

    if ( message != NULL ) {
        jboolean errorOutstanding;

        messageString = (*jnienv)->NewStringUTF(jnienv, message);
        errorOutstanding = checkForAndClearThrowable(jnienv);
        jplis_assert_msg(!errorOutstanding, "can't create exception java string");
    }
    return createThrowable( jnienv,
                            throwableClassName,
                            messageString);

}


/**
 *  Calls toString() on the given message which is the same call made by
 *  Exception when passed a throwable to its constructor
 */
jstring
getMessageFromThrowable(    JNIEnv*     jnienv,
                            jthrowable  exception) {
    jclass      exceptionClass      = NULL;
    jmethodID   method              = NULL;
    jstring     message             = NULL;
    jboolean    errorOutstanding    = JNI_FALSE;

    jplis_assert(isSafeForJNICalls(jnienv));

    /* call getMessage on exception */
    exceptionClass = (*jnienv)->GetObjectClass(jnienv, exception);
    errorOutstanding = checkForAndClearThrowable(jnienv);
    jplis_assert(!errorOutstanding);

    if (!errorOutstanding) {
        method = (*jnienv)->GetMethodID(jnienv,
                                        exceptionClass,
                                        "toString",
                                        "()Ljava/lang/String;");
        errorOutstanding = checkForAndClearThrowable(jnienv);
        jplis_assert(!errorOutstanding);
    }

    if (!errorOutstanding) {
        message = (*jnienv)->CallObjectMethod(jnienv, exception, method);
        errorOutstanding = checkForAndClearThrowable(jnienv);
        jplis_assert(!errorOutstanding);
    }

    jplis_assert(isSafeForJNICalls(jnienv));

    return message;
}


/**
 *  Returns whether the exception given is an unchecked exception:
 *  a subclass of Error or RuntimeException
 */
jboolean
isUnchecked(    JNIEnv*     jnienv,
                jthrowable  exception) {
    jboolean result = JNI_FALSE;

    jplis_assert(isSafeForJNICalls(jnienv));
    result =    (exception == NULL) ||
                isInstanceofClassName(jnienv, exception, "java/lang/Error") ||
                isInstanceofClassName(jnienv, exception, "java/lang/RuntimeException");
    jplis_assert(isSafeForJNICalls(jnienv));
    return result;
}

/*
 *  Returns the current throwable, if any. Clears the throwable state.
 *  Clients can use this to preserve the current throwable state on the stack.
 */
jthrowable
preserveThrowable(JNIEnv * jnienv) {
    jthrowable result = (*jnienv)->ExceptionOccurred(jnienv);
    if ( result != NULL ) {
        (*jnienv)->ExceptionClear(jnienv);
    }
    return result;
}

/*
 *  Installs the supplied throwable into the JNIEnv if the throwable is not null.
 *  Clients can use this to preserve the current throwable state on the stack.
 */
void
restoreThrowable(   JNIEnv *    jnienv,
                    jthrowable  preservedException) {
    throwThrowable( jnienv,
                    preservedException);
    return;
}

void
throwThrowable(     JNIEnv *    jnienv,
                    jthrowable  exception) {
    if ( exception != NULL ) {
        jint result = (*jnienv)->Throw(jnienv, exception);
        jplis_assert_msg(result == JNI_OK, "throwThrowable failed to re-throw");
    }
    return;
}


/*
 *  Always clears the JNIEnv throwable state. Returns true if an exception was present
 *  before the clearing operation.
 */
jboolean
checkForAndClearThrowable(  JNIEnv *    jnienv) {
    jboolean result = (*jnienv)->ExceptionCheck(jnienv);
    if ( result ) {
        (*jnienv)->ExceptionClear(jnienv);
    }
    return result;
}

/* creates a java.lang.InternalError and installs it into the JNIEnv */
void
createAndThrowInternalError(JNIEnv * jnienv) {
    jthrowable internalError = createInternalError( jnienv, NULL);
    throwThrowable(jnienv, forceFallback(internalError));
}

void
createAndThrowThrowableFromJVMTIErrorCode(JNIEnv * jnienv, jvmtiError errorCode) {
    jthrowable throwable = createThrowableFromJVMTIErrorCode(jnienv, errorCode);
    throwThrowable(jnienv, forceFallback(throwable));
}

void
mapThrownThrowableIfNecessary(  JNIEnv *                jnienv,
                                CheckedExceptionMapper  mapper) {
    jthrowable  originalThrowable   = NULL;
    jthrowable  resultThrowable     = NULL;

    originalThrowable = preserveThrowable(jnienv);

    /* the throwable is now cleared, so JNI calls are safe */
    if ( originalThrowable != NULL ) {
        /* if there is an exception: we can just throw it if it is unchecked. If checked,
         * we need to map it (mapper is conditional, will vary by usage, hence the callback)
         */
        if ( isUnchecked(jnienv, originalThrowable) ) {
            resultThrowable = originalThrowable;
        }
        else {
            resultThrowable = (*mapper) (jnienv, originalThrowable);
        }
    }

    /* re-establish the correct throwable */
    if ( resultThrowable != NULL ) {
        throwThrowable(jnienv, forceFallback(resultThrowable));
    }

}
