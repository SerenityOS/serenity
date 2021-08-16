/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2021, JetBrains s.r.o.. All rights reserved.
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

#import "ComboBoxAccessibility.h"
#import "../JavaAccessibilityUtilities.h"
#import "ThreadUtilities.h"
#import "JNIUtilities.h"

static jclass sjc_CAccessibility = NULL;

static jmethodID sjm_getAccessibleName = NULL;
#define GET_ACCESSIBLENAME_METHOD_RETURN(ret) \
    GET_CACCESSIBILITY_CLASS_RETURN(ret); \
    GET_STATIC_METHOD_RETURN(sjm_getAccessibleName, sjc_CAccessibility, "getAccessibleName", \
                     "(Ljavax/accessibility/Accessible;Ljava/awt/Component;)Ljava/lang/String;", ret);

@implementation ComboBoxAccessibility

// NSAccessibilityElement protocol methods

- (id)accessibilityValue {
    JNIEnv *env = [ThreadUtilities getJNIEnv];
    jobject axContext = [self axContextWithEnv:env];
    if (axContext == NULL) return nil;
    jclass axContextClass = (*env)->GetObjectClass(env, axContext);
    DECLARE_METHOD_RETURN(jm_getAccessibleSelection, axContextClass, "getAccessibleSelection", "(I)Ljavax/accessibility/Accessible;", nil);
    jobject axSelectedChild = (*env)->CallObjectMethod(env, axContext, jm_getAccessibleSelection, 0);
    CHECK_EXCEPTION();
    (*env)->DeleteLocalRef(env, axContext);
    if (axSelectedChild == NULL) {
        return nil;
    }
    GET_CACCESSIBILITY_CLASS_RETURN(nil);
    GET_ACCESSIBLENAME_METHOD_RETURN(nil);
    jobject childName = (*env)->CallStaticObjectMethod(env, sjc_CAccessibility, sjm_getAccessibleName, axSelectedChild, fComponent);
    CHECK_EXCEPTION();
    if (childName == NULL) {
        (*env)->DeleteLocalRef(env, axSelectedChild);
        return nil;
    }
    NSString *selectedText = JavaStringToNSString(env, childName);
    (*env)->DeleteLocalRef(env, axSelectedChild);
    (*env)->DeleteLocalRef(env, childName);
    return selectedText;
}

@end
