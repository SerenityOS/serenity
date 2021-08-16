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

#import "JavaAccessibilityAction.h"
#import "JavaAccessibilityUtilities.h"

#import "ThreadUtilities.h"
#import "JNIUtilities.h"

NSMutableDictionary *sActions = nil;
NSMutableDictionary *sActionSelectors = nil;
NSMutableArray *sAllActionSelectors = nil;
void initializeActions();

@implementation JavaAxAction

- (id)initWithEnv:(JNIEnv *)env withAccessibleAction:(jobject)accessibleAction withIndex:(jint)index withComponent:(jobject)component
{
    self = [super init];
    if (self) {
        fAccessibleAction = (*env)->NewWeakGlobalRef(env, accessibleAction);
        CHECK_EXCEPTION();
        fIndex = index;
        fComponent = (*env)->NewWeakGlobalRef(env, component);
        CHECK_EXCEPTION();
    }
    return self;
}

- (void)dealloc
{
    JNIEnv *env = [ThreadUtilities getJNIEnvUncached];

    (*env)->DeleteWeakGlobalRef(env, fAccessibleAction);
    fAccessibleAction = NULL;

    (*env)->DeleteWeakGlobalRef(env, fComponent);
    fComponent = NULL;

    [super dealloc];
}

- (NSString *)getDescription
{
    JNIEnv* env = [ThreadUtilities getJNIEnv];
    DECLARE_CLASS_RETURN(sjc_CAccessibility, "sun/lwawt/macosx/CAccessibility", nil);
    DECLARE_STATIC_METHOD_RETURN(jm_getAccessibleActionDescription, sjc_CAccessibility,
                          "getAccessibleActionDescription",
                          "(Ljavax/accessibility/AccessibleAction;ILjava/awt/Component;)Ljava/lang/String;", nil);

    /* WeakGlobalRefs can be cleared at any time, so first get strong local refs and use those */
    jobject fCompLocal = (*env)->NewLocalRef(env, fComponent);
    if ((*env)->IsSameObject(env, fCompLocal, NULL)) {
        return nil;
    }
    jobject fAccessibleActionLocal = (*env)->NewLocalRef(env, fAccessibleAction);
    if ((*env)->IsSameObject(env, fAccessibleActionLocal, NULL)) {
        (*env)->DeleteLocalRef(env, fCompLocal);
        return nil;
    }
    NSString *str = nil;
    jstring jstr = (*env)->CallStaticObjectMethod(env, sjc_CAccessibility,
                                              jm_getAccessibleActionDescription,
                                              fAccessibleActionLocal,
                                              fIndex,
                                              fCompLocal );
    CHECK_EXCEPTION();
    if (jstr != NULL) {
        str = JavaStringToNSString(env, jstr);
        (*env)->DeleteLocalRef(env, jstr);
    }
    (*env)->DeleteLocalRef(env, fCompLocal);
    (*env)->DeleteLocalRef(env, fAccessibleActionLocal);
    return str;
}

- (void)perform
{
    JNIEnv* env = [ThreadUtilities getJNIEnv];
    DECLARE_CLASS(sjc_CAccessibility, "sun/lwawt/macosx/CAccessibility");
    DECLARE_STATIC_METHOD(jm_doAccessibleAction, sjc_CAccessibility, "doAccessibleAction",
                    "(Ljavax/accessibility/AccessibleAction;ILjava/awt/Component;)V");

    (*env)->CallStaticVoidMethod(env, sjc_CAccessibility, jm_doAccessibleAction,
             fAccessibleAction, fIndex, fComponent);
    CHECK_EXCEPTION();
}

@end


@implementation TabGroupAction

- (id)initWithEnv:(JNIEnv *)env withTabGroup:(jobject)tabGroup withIndex:(jint)index withComponent:(jobject)component
{
    self = [super init];
    if (self) {
        fTabGroup = (*env)->NewWeakGlobalRef(env, tabGroup);
        CHECK_EXCEPTION();
        fIndex = index;
        fComponent = (*env)->NewWeakGlobalRef(env, component);
        CHECK_EXCEPTION();
    }
    return self;
}

- (void)dealloc
{
    JNIEnv *env = [ThreadUtilities getJNIEnvUncached];

    (*env)->DeleteWeakGlobalRef(env, fTabGroup);
    fTabGroup = NULL;

    (*env)->DeleteWeakGlobalRef(env, fComponent);
    fComponent = NULL;

    [super dealloc];
}

- (NSString *)getDescription
{
    return @"click";
}

- (void)perform
{
    JNIEnv* env = [ThreadUtilities getJNIEnv];

    setAxContextSelection(env, fTabGroup, fIndex, fComponent);
}

@end

void initializeActions() {
    int actionsCount = 5;

    sActions = [[NSMutableDictionary alloc] initWithCapacity:actionsCount];

    [sActions setObject:NSAccessibilityPressAction forKey:@"click"];
    [sActions setObject:NSAccessibilityIncrementAction forKey:@"increment"];
    [sActions setObject:NSAccessibilityDecrementAction forKey:@"decrement"];
    [sActions setObject:NSAccessibilityShowMenuAction forKey:@"togglePopup"];
    [sActions setObject:NSAccessibilityPressAction forKey:@"toggleExpand"];

    sActionSelectors = [[NSMutableDictionary alloc] initWithCapacity:actionsCount];

    [sActionSelectors setObject:NSStringFromSelector(@selector(accessibilityPerformPress)) forKey:NSAccessibilityPressAction];
    [sActionSelectors setObject:NSStringFromSelector(@selector(accessibilityPerformShowMenu)) forKey:NSAccessibilityShowMenuAction];
    [sActionSelectors setObject:NSStringFromSelector(@selector(accessibilityPerformDecrement)) forKey:NSAccessibilityDecrementAction];
    [sActionSelectors setObject:NSStringFromSelector(@selector(accessibilityPerformIncrement)) forKey:NSAccessibilityIncrementAction];
    [sActionSelectors setObject:NSStringFromSelector(@selector(accessibilityPerformPick)) forKey:NSAccessibilityPickAction];

    sAllActionSelectors = [[NSMutableArray alloc] initWithCapacity:actionsCount];

    [sAllActionSelectors addObject:NSStringFromSelector(@selector(accessibilityPerformPick))];
    [sAllActionSelectors addObject:NSStringFromSelector(@selector(accessibilityPerformIncrement))];
    [sAllActionSelectors addObject:NSStringFromSelector(@selector(accessibilityPerformDecrement))];
    [sAllActionSelectors addObject:NSStringFromSelector(@selector(accessibilityPerformShowMenu))];
    [sAllActionSelectors addObject:NSStringFromSelector(@selector(accessibilityPerformPress))];
}
