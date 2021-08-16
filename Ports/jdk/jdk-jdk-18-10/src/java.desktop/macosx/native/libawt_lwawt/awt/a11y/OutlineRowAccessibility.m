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

#include "jni.h"
#import "OutlineRowAccessibility.h"
#import "JavaAccessibilityUtilities.h"
#import "ThreadUtilities.h"
#import "JNIUtilities.h"
#import "OutlineAccessibility.h"
#import "sun_lwawt_macosx_CAccessibility.h"

static jclass sjc_CAccessible = NULL;
#define GET_CACCESSIBLE_CLASS_RETURN(ret) \
    GET_CLASS_RETURN(sjc_CAccessible, "sun/lwawt/macosx/CAccessible", ret);

@implementation OutlineRowAccessibility

@synthesize accessibleLevel;

- (jobject)currentAccessibleWithENV:(JNIEnv *)env
{
    jobject jAxContext = getAxContext(env, fAccessible, fComponent);
    if (jAxContext == NULL) return NULL;
    jclass axContextClass = (*env)->GetObjectClass(env, jAxContext);
    DECLARE_METHOD_RETURN(jm_getCurrentComponent, axContextClass, "getCurrentComponent", "()Ljava/awt/Component;", NULL);
    jobject newComponent = (*env)->CallObjectMethod(env, jAxContext, jm_getCurrentComponent);
    CHECK_EXCEPTION();
    (*env)->DeleteLocalRef(env, jAxContext);
    if (newComponent != NULL) {
        GET_CACCESSIBLE_CLASS_RETURN(NULL);
        DECLARE_STATIC_METHOD_RETURN(sjm_getCAccessible, sjc_CAccessible, "getCAccessible", "(Ljavax/accessibility/Accessible;)Lsun/lwawt/macosx/CAccessible;", NULL);
        jobject currentAccessible = (*env)->CallStaticObjectMethod(env, sjc_CAccessible, sjm_getCAccessible, newComponent);
        CHECK_EXCEPTION();
        (*env)->DeleteLocalRef(env, newComponent);
        return currentAccessible;
    } else {
        return NULL;
    }
}

// NSAccessibilityElement protocol methods

- (NSArray *)accessibilityChildren
{
    JNIEnv *env = [ThreadUtilities getJNIEnv];
    jobject currentAccessible = [self currentAccessibleWithENV:env];
    if (currentAccessible != NULL) {
        CommonComponentAccessibility *currentElement = [CommonComponentAccessibility createWithAccessible:currentAccessible withEnv:env withView:self->fView isCurrent:YES];
        NSArray *children = [CommonComponentAccessibility childrenOfParent:currentElement withEnv:env withChildrenCode:sun_lwawt_macosx_CAccessibility_JAVA_AX_ALL_CHILDREN allowIgnored:YES];
        if ([children count] != 0) {
            return children;
        }
    }

    return [NSArray arrayWithObject:[CommonComponentAccessibility createWithParent:self
                                                                        accessible:self->fAccessible
                                                                              role:self->fJavaRole
                                                                             index:self->fIndex
                                                                           withEnv:env
                                                                          withView:self->fView
                                                                         isWrapped:YES]];
}

- (NSInteger)accessibilityDisclosureLevel
{
    int level = [self accessibleLevel];
    return [(OutlineAccessibility *)[self accessibilityParent] isTreeRootVisible] ? level - 1 : level;
}

- (BOOL)isAccessibilityDisclosed
{
    return isExpanded([ThreadUtilities getJNIEnv], [self axContextWithEnv:[ThreadUtilities getJNIEnv]], self->fComponent);
}

- (NSAccessibilitySubrole)accessibilitySubrole
{
    return NSAccessibilityOutlineRowSubrole;;
}

- (NSAccessibilityRole)accessibilityRole
{
    return NSAccessibilityRowRole;;
}

- (BOOL)isAccessibilitySelected
{
    return YES;
}

@end
