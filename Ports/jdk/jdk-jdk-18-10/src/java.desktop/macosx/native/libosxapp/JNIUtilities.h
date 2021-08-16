/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef __JNIUTILITIES_H
#define __JNIUTILITIES_H

#include "jni.h"
#include "jni_util.h"

#import <Cocoa/Cocoa.h>

/********        LOGGING SUPPORT    *********/

#define LOG_NULL(dst_var, name) \
   if (dst_var == NULL) { \
       NSLog(@"Bad JNI lookup %s\n", name); \
       NSLog(@"%@",[NSThread callStackSymbols]); \
       if ([NSThread isMainThread] == NO) { \
           if ((*env)->ExceptionOccurred(env) == NULL) { \
              JNU_ThrowInternalError(env, "Bad JNI Lookup"); \
           } \
       } else { \
              if ((*env)->ExceptionOccurred(env) != NULL) { \
                  (*env)->ExceptionDescribe(env); \
           } \
       } \
       [NSException raise:NSGenericException format:@"JNI Lookup Exception"];  \
    }

/********        GET CLASS SUPPORT    *********/

#define GET_CLASS(dst_var, cls) \
     if (dst_var == NULL) { \
         dst_var = (*env)->FindClass(env, cls); \
         if (dst_var != NULL) dst_var = (*env)->NewGlobalRef(env, dst_var); \
     } \
     LOG_NULL(dst_var, cls); \
     CHECK_NULL(dst_var);

#define DECLARE_CLASS(dst_var, cls) \
    static jclass dst_var = NULL; \
    GET_CLASS(dst_var, cls);

#define GET_CLASS_RETURN(dst_var, cls, ret) \
     if (dst_var == NULL) { \
         dst_var = (*env)->FindClass(env, cls); \
         if (dst_var != NULL) dst_var = (*env)->NewGlobalRef(env, dst_var); \
     } \
     LOG_NULL(dst_var, cls); \
     CHECK_NULL_RETURN(dst_var, ret);

#define DECLARE_CLASS_RETURN(dst_var, cls, ret) \
    static jclass dst_var = NULL; \
    GET_CLASS_RETURN(dst_var, cls, ret);


/********        GET METHOD SUPPORT    *********/

#define GET_METHOD(dst_var, cls, name, signature) \
     if (dst_var == NULL) { \
         dst_var = (*env)->GetMethodID(env, cls, name, signature); \
     } \
     LOG_NULL(dst_var, name); \
     CHECK_NULL(dst_var);

#define DECLARE_METHOD(dst_var, cls, name, signature) \
     static jmethodID dst_var = NULL; \
     GET_METHOD(dst_var, cls, name, signature);

#define GET_METHOD_RETURN(dst_var, cls, name, signature, ret) \
     if (dst_var == NULL) { \
         dst_var = (*env)->GetMethodID(env, cls, name, signature); \
     } \
     LOG_NULL(dst_var, name); \
     CHECK_NULL_RETURN(dst_var, ret);

#define DECLARE_METHOD_RETURN(dst_var, cls, name, signature, ret) \
     static jmethodID dst_var = NULL; \
     GET_METHOD_RETURN(dst_var, cls, name, signature, ret);

#define GET_STATIC_METHOD(dst_var, cls, name, signature) \
     if (dst_var == NULL) { \
         dst_var = (*env)->GetStaticMethodID(env, cls, name, signature); \
     } \
     LOG_NULL(dst_var, name); \
     CHECK_NULL(dst_var);

#define DECLARE_STATIC_METHOD(dst_var, cls, name, signature) \
     static jmethodID dst_var = NULL; \
     GET_STATIC_METHOD(dst_var, cls, name, signature);

#define GET_STATIC_METHOD_RETURN(dst_var, cls, name, signature, ret) \
     if (dst_var == NULL) { \
         dst_var = (*env)->GetStaticMethodID(env, cls, name, signature); \
     } \
     LOG_NULL(dst_var, name); \
     CHECK_NULL_RETURN(dst_var, ret);

#define DECLARE_STATIC_METHOD_RETURN(dst_var, cls, name, signature, ret) \
     static jmethodID dst_var = NULL; \
     GET_STATIC_METHOD_RETURN(dst_var, cls, name, signature, ret);

/********        GET FIELD SUPPORT    *********/


#define GET_FIELD(dst_var, cls, name, signature) \
     if (dst_var == NULL) { \
         dst_var = (*env)->GetFieldID(env, cls, name, signature); \
     } \
     LOG_NULL(dst_var, name); \
     CHECK_NULL(dst_var);

#define DECLARE_FIELD(dst_var, cls, name, signature) \
     static jfieldID dst_var = NULL; \
     GET_FIELD(dst_var, cls, name, signature);

#define GET_FIELD_RETURN(dst_var, cls, name, signature, ret) \
     if (dst_var == NULL) { \
         dst_var = (*env)->GetFieldID(env, cls, name, signature); \
     } \
     LOG_NULL(dst_var, name); \
     CHECK_NULL_RETURN(dst_var, ret);

#define DECLARE_FIELD_RETURN(dst_var, cls, name, signature, ret) \
     static jfieldID dst_var = NULL; \
     GET_FIELD_RETURN(dst_var, cls, name, signature, ret);

#define GET_STATIC_FIELD_RETURN(dst_var, cls, name, signature, ret) \
     if (dst_var == NULL) { \
         dst_var = (*env)->GetStaticFieldID(env, cls, name, signature); \
     } \
     LOG_NULL(dst_var, name); \
     CHECK_NULL_RETURN(dst_var, ret);

#define DECLARE_STATIC_FIELD_RETURN(dst_var, cls, name, signature, ret) \
     static jfieldID dst_var = NULL; \
     GET_STATIC_FIELD_RETURN(dst_var, cls, name, signature, ret);

/*********       EXCEPTION_HANDLING    *********/

/*
 * Some explanation to set context of the bigger picture.
 * Before returning to Java from JNI, NSExceptions are caught - so long as
 * the body of the native method is wrapped in the ENTER/EXIT macros.
 * So if we want to directly return to Java from some nested Objective-C
 * function when detecting a Java exception, we just need to raise an
 * NSException. Then clear that right before returning to Java,
 * leaving the Java exception to be seen back in Java-land.
 *
 * But if the current thread is the Appkit thread we might as well clear
 * the Java Exception right now since there's nothing to receive it.
 * In such a case control will propagate back to the run loop which might
 * terminate the application. One drawback of that is that the location of
 * termination does not show where the NSException originated.
 * And for whatever reason, something swallows that exception.
 * So as a debugging aid, when on the AppKit thread we can provide a
 * way (via an env. var.) to log the location.
 * Additionally provide a similar way to prevent the NSException being
 * raised and instead just clear the Java Exception.
 * Together these provide alternate behaviours for more debugging info
 * or maybe a way for the app to continue running depending on the exact
 * nature of the problem that has been detected and how survivable it is.
 */
#define CHECK_EXCEPTION() \
    if ((*env)->ExceptionOccurred(env) != NULL) { \
        if ([NSThread isMainThread] == YES) { \
            if (getenv("JNU_APPKIT_TRACE")) { \
                (*env)->ExceptionDescribe(env); \
                NSLog(@"%@",[NSThread callStackSymbols]); \
              } else { \
                  (*env)->ExceptionClear(env); \
              } \
         }  \
        if (getenv("JNU_NO_COCOA_EXCEPTION") == NULL) { \
            [NSException raise:NSGenericException format:@"Java Exception"]; \
        } else { \
            (*env)->ExceptionClear(env); \
        } \
    };

#define CHECK_EXCEPTION_NULL_RETURN(x, y) \
    CHECK_EXCEPTION(); \
    if ((x) == NULL) { \
       return y; \
    };

/* Create a pool and initiate a try block to catch any exception */
#define JNI_COCOA_ENTER(env) \
 NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init]; \
 @try {

/* Don't allow NSExceptions to escape to Java.
 * If there is a Java exception that has been thrown that should escape.
 * And ensure we drain the auto-release pool.
 */
#define JNI_COCOA_EXIT(env) \
 } \
 @catch (NSException *e) { \
     NSLog(@"%@", [e callStackSymbols]); \
 } \
 @finally { \
    [pool drain]; \
 };

/* Same as above but adds a clean up action.
 * Requires that whatever is being cleaned up is in scope.
 */
#define JNI_COCOA_EXIT_WITH_ACTION(env, action) \
 } \
 @catch (NSException *e) { \
     { action; }; \
     NSLog(@"%@", [e callStackSymbols]); \
 } \
 @finally { \
    [pool drain]; \
 };

/********        STRING CONVERSION SUPPORT    *********/

JNIEXPORT NSString* JavaStringToNSString(JNIEnv *env, jstring jstr);

JNIEXPORT jstring NSStringToJavaString(JNIEnv* env, NSString *str);

JNIEXPORT NSString* NormalizedPathNSStringFromJavaString(JNIEnv *env, jstring pathStr);

JNIEXPORT jstring NormalizedPathJavaStringFromNSString(JNIEnv* env, NSString *str);

#endif /* __JNIUTILITIES_H */
