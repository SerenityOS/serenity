/*
 * Copyright (c) 2011, 2014, Oracle and/or its affiliates. All rights reserved.
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

#import <Cocoa/Cocoa.h>
#include <objc/objc-runtime.h>

#import "sun_lwawt_macosx_CInputMethod.h"
#import "sun_lwawt_macosx_CInputMethodDescriptor.h"
#import "ThreadUtilities.h"
#import "AWTView.h"
#import "JNIUtilities.h"

#import <JavaRuntimeSupport/JavaRuntimeSupport.h>

#define JAVA_LIST @"JAVA_LIST"
#define CURRENT_KB_DESCRIPTION @"CURRENT_KB_DESCRIPTION"

//
// NOTE: This returns a JNI Local Ref. Any code that calls must call DeleteLocalRef with the return value.
//
static jobject CreateLocaleObjectFromNSString(JNIEnv *env, NSString *name)
{
    DECLARE_CLASS_RETURN(jc_localeClass, "java/util/Locale", NULL);
    DECLARE_METHOD_RETURN(jm_localeCons, jc_localeClass, "<init>", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V", NULL);
    // Break apart the string into its components.
    // First, duplicate the NSString into a C string, since we're going to modify it.
    char * language = strdup([name UTF8String]);
    char * country;
    char * variant;

    // Convert _ to NULL -- this gives us three null terminated strings in place.
    for (country = language; *country != '_' && *country != '\0'; country++);
    if (*country == '_') {
        *country++ = '\0';
        for (variant = country; *variant != '_' && *variant != '\0'; variant++);
        if (*variant == '_') {
            *variant++ = '\0';
        }
    } else {
        variant = country;
    }

    // Create the java.util.Locale object
    jobject localeObj = NULL;
    jobject langObj = (*env)->NewStringUTF(env, language);
    if (langObj != NULL) {
        jobject ctryObj = (*env)->NewStringUTF(env, country);
        if(ctryObj != NULL) {
            jobject vrntObj = (*env)->NewStringUTF(env, variant);
            if (vrntObj != NULL) {
                localeObj = (*env)->NewObject(env, jc_localeClass, jm_localeCons,langObj, ctryObj,
                                         vrntObj);
                CHECK_EXCEPTION();
                (*env)->DeleteLocalRef(env, vrntObj);
            }
            (*env)->DeleteLocalRef(env, ctryObj);
        }
        (*env)->DeleteLocalRef(env, langObj);
    }
    // Clean up and return.
    free(language);
    return localeObj;
}

static id inputMethodController = nil;

static void initializeInputMethodController() {
    static BOOL checkedJRSInputMethodController = NO;
    if (!checkedJRSInputMethodController && (inputMethodController == nil)) {
        id jrsInputMethodController = objc_lookUpClass("JRSInputMethodController");
        if (jrsInputMethodController != nil) {
            inputMethodController = [jrsInputMethodController performSelector:@selector(controller)];
        }
        checkedJRSInputMethodController = YES;
    }
}


@interface CInputMethod : NSObject {}
@end

@implementation CInputMethod

+ (void) setKeyboardLayout:(NSString *)theLocale
{
    AWT_ASSERT_APPKIT_THREAD;
    if (!inputMethodController) return;

    [inputMethodController performSelector:@selector(setCurrentInputMethodForLocale) withObject:theLocale];
}

+ (void) _nativeNotifyPeerWithView:(AWTView *)view inputMethod:(jobject) inputMethod {
    AWT_ASSERT_APPKIT_THREAD;

    if (!view) return;
    if (!inputMethod) return;

    [view setInputMethod:inputMethod]; // inputMethod is a GlobalRef
}

+ (void) _nativeEndComposition:(AWTView *)view {
    if (view == nil) return;

    [view abandonInput];
}


@end

/*
 * Class:     sun_lwawt_macosx_CInputMethod
 * Method:    nativeInit
 * Signature: ();
 */
JNIEXPORT void JNICALL Java_sun_lwawt_macosx_CInputMethod_nativeInit
(JNIEnv *env, jclass klass)
{
    initializeInputMethodController();
}

/*
 * Class:     sun_lwawt_macosx_CInputMethod
 * Method:    nativeGetCurrentInputMethodInfo
 * Signature: ()Ljava/lang/String;
 */
JNIEXPORT jobject JNICALL Java_sun_lwawt_macosx_CInputMethod_nativeGetCurrentInputMethodInfo
(JNIEnv *env, jclass klass)
{
    if (!inputMethodController) return NULL;
    jobject returnValue = 0;
    __block NSString *keyboardInfo = NULL;
JNI_COCOA_ENTER(env);

    [ThreadUtilities performOnMainThreadWaiting:YES block:^(){
        keyboardInfo = [inputMethodController performSelector:@selector(currentInputMethodName)];
        [keyboardInfo retain];
    }];

    if (keyboardInfo == nil) return NULL;
    returnValue = NSStringToJavaString(env, keyboardInfo);
    [keyboardInfo release];

JNI_COCOA_EXIT(env);
    return returnValue;
}

/*
 * Class:     sun_lwawt_macosx_CInputMethod
 * Method:    nativeActivate
 * Signature: (JLsun/lwawt/macosx/CInputMethod;)V
 */
JNIEXPORT void JNICALL Java_sun_lwawt_macosx_CInputMethod_nativeNotifyPeer
(JNIEnv *env, jobject this, jlong nativePeer, jobject inputMethod)
{
JNI_COCOA_ENTER(env);
    AWTView *view = (AWTView *)jlong_to_ptr(nativePeer);
    jobject inputMethodRef = (*env)->NewGlobalRef(env, inputMethod);
    [ThreadUtilities performOnMainThreadWaiting:NO block:^(){
        [CInputMethod _nativeNotifyPeerWithView:view inputMethod:inputMethodRef];
    }];

JNI_COCOA_EXIT(env);

}

/*
 * Class:     sun_lwawt_macosx_CInputMethod
 * Method:    nativeEndComposition
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_sun_lwawt_macosx_CInputMethod_nativeEndComposition
(JNIEnv *env, jobject this, jlong nativePeer)
{
JNI_COCOA_ENTER(env);
   AWTView *view = (AWTView *)jlong_to_ptr(nativePeer);

   [ThreadUtilities performOnMainThreadWaiting:NO block:^(){
        [CInputMethod _nativeEndComposition:view];
    }];

JNI_COCOA_EXIT(env);
}

/*
 * Class:     sun_lwawt_macosx_CInputMethod
 * Method:    getNativeLocale
 * Signature: ()Ljava/util/Locale;
 */
JNIEXPORT jobject JNICALL Java_sun_lwawt_macosx_CInputMethod_getNativeLocale
(JNIEnv *env, jobject this)
{
    if (!inputMethodController) return NULL;
    jobject returnValue = 0;
    __block NSString *isoAbbreviation;
JNI_COCOA_ENTER(env);

    [ThreadUtilities performOnMainThreadWaiting:YES block:^(){
        isoAbbreviation = (NSString *) [inputMethodController performSelector:@selector(currentInputMethodLocale)];
        [isoAbbreviation retain];
    }];

    if (isoAbbreviation == nil) return NULL;

    static NSString *sLastKeyboardStr = nil;
    static jobject sLastKeyboardLocaleObj = NULL;

    if (![isoAbbreviation isEqualTo:sLastKeyboardStr]) {
        [sLastKeyboardStr release];
        sLastKeyboardStr = [isoAbbreviation retain];
        jobject localObj = CreateLocaleObjectFromNSString(env, isoAbbreviation);
        [isoAbbreviation release];

        if (sLastKeyboardLocaleObj) {
            (*env)->DeleteGlobalRef(env, sLastKeyboardLocaleObj);
            sLastKeyboardLocaleObj = NULL;
        }
        if (localObj != NULL) {
            sLastKeyboardLocaleObj = (*env)->NewGlobalRef(env, localObj);
            (*env)->DeleteLocalRef(env, localObj);
        }
    }

    returnValue = sLastKeyboardLocaleObj;

JNI_COCOA_EXIT(env);
    return returnValue;
}


/*
 * Class:     sun_lwawt_macosx_CInputMethod
 * Method:    setNativeLocale
 * Signature: (Ljava/lang/String;Z)Z
 */
JNIEXPORT jboolean JNICALL Java_sun_lwawt_macosx_CInputMethod_setNativeLocale
(JNIEnv *env, jobject this, jstring locale, jboolean isActivating)
{
JNI_COCOA_ENTER(env);
    NSString *localeStr = JavaStringToNSString(env, locale);
    [localeStr retain];

    [ThreadUtilities performOnMainThreadWaiting:YES block:^(){
        [CInputMethod setKeyboardLayout:localeStr];
    }];

    [localeStr release];
JNI_COCOA_EXIT(env);
    return JNI_TRUE;
}

/*
 * Class:     sun_lwawt_macosx_CInputMethodDescriptor
 * Method:    nativeInit
 * Signature: ();
 */
JNIEXPORT void JNICALL Java_sun_lwawt_macosx_CInputMethodDescriptor_nativeInit
(JNIEnv *env, jclass klass)
{
    initializeInputMethodController();
}

/*
 * Class:     sun_lwawt_macosx_CInputMethodDescriptor
 * Method:    nativeGetAvailableLocales
 * Signature: ()[Ljava/util/Locale;
     */
JNIEXPORT jobject JNICALL Java_sun_lwawt_macosx_CInputMethodDescriptor_nativeGetAvailableLocales
(JNIEnv *env, jclass klass)
{
    DECLARE_CLASS_RETURN(jc_arrayListClass, "java/util/ArrayList", NULL);
    DECLARE_METHOD_RETURN(jm_arrayListCons, jc_arrayListClass, "<init>", "()V", NULL);
    DECLARE_METHOD_RETURN(jm_listAdd, jc_arrayListClass, "add", "(Ljava/lang/Object;)Z", NULL);
    DECLARE_METHOD_RETURN(jm_listContains, jc_arrayListClass, "contains", "(Ljava/lang/Object;)Z", NULL);

    if (!inputMethodController) return NULL;
    jobject returnValue = 0;

    __block NSArray *selectableArray = nil;
JNI_COCOA_ENTER(env);

    [ThreadUtilities performOnMainThreadWaiting:YES block:^(){
        selectableArray = (NSArray *)[inputMethodController performSelector:@selector(availableInputMethodLocales)];
        [selectableArray retain];
    }];

    if (selectableArray == nil) return NULL;

     // Create an ArrayList to return back to the caller.
    returnValue = (*env)->NewObject(env, jc_arrayListClass, jm_arrayListCons);
    CHECK_EXCEPTION_NULL_RETURN(returnValue, NULL);

    for(NSString *locale in selectableArray) {
        jobject localeObj = CreateLocaleObjectFromNSString(env, locale);
        if (localeObj == NULL) {
            break;
        }

        if ((*env)->CallBooleanMethod(env, returnValue, jm_listContains, localeObj) == JNI_FALSE) {
            if ((*env)->ExceptionOccurred(env)) (*env)->ExceptionClear(env);
            (*env)->CallBooleanMethod(env, returnValue, jm_listAdd, localeObj);
        }
        if ((*env)->ExceptionOccurred(env)) (*env)->ExceptionClear(env);

        (*env)->DeleteLocalRef(env, localeObj);
    }
    [selectableArray release];
JNI_COCOA_EXIT(env);
    return returnValue;
}

