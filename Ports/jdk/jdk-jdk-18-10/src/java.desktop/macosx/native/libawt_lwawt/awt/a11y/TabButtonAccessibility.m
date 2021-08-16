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

#import "TabButtonAccessibility.h"
#import "JavaAccessibilityAction.h"
#import "JavaAccessibilityUtilities.h"
#import "ThreadUtilities.h"
#import "JNIUtilities.h"

@implementation TabButtonAccessibility

- (id)initWithParent:(NSObject *)parent withEnv:(JNIEnv *)env withAccessible:(jobject)accessible withIndex:(jint)index withTabGroup:(jobject)tabGroup withView:(NSView *)view withJavaRole:(NSString *)javaRole
{
    self = [super initWithParent:parent withEnv:env withAccessible:accessible withIndex:index withView:view withJavaRole:javaRole];
    if (self) {
        if (tabGroup != NULL) {
            fTabGroupAxContext = (*env)->NewWeakGlobalRef(env, tabGroup);
            CHECK_EXCEPTION();
        } else {
            fTabGroupAxContext = NULL;
        }
    }
    return self;
}

- (void)dealloc
{
    JNIEnv *env = [ThreadUtilities getJNIEnvUncached];

    if (fTabGroupAxContext != NULL) {
        (*env)->DeleteWeakGlobalRef(env, fTabGroupAxContext);
        fTabGroupAxContext = NULL;
    }

    [super dealloc];
}

- (jobject)tabGroup
{
    if (fTabGroupAxContext == NULL) {
        JNIEnv* env = [ThreadUtilities getJNIEnv];
        jobject tabGroupAxContext = [(CommonComponentAccessibility *)[self parent] axContextWithEnv:env];
        fTabGroupAxContext = (*env)->NewWeakGlobalRef(env, tabGroupAxContext);
        CHECK_EXCEPTION();
        (*env)->DeleteLocalRef(env, tabGroupAxContext);
    }
    return fTabGroupAxContext;
}

- (void)performPressAction {
    JNIEnv *env = [ThreadUtilities getJNIEnv];
    TabGroupAction *action = [[TabGroupAction alloc] initWithEnv:env withTabGroup:[self tabGroup] withIndex:fIndex withComponent:fComponent];
    [action perform];
    [action release];
}

// NSAccessibilityElement protocol methods

- (NSAccessibilitySubrole)accessibilitySubrole
{
    if (@available(macOS 10.13, *)) {
        return NSAccessibilityTabButtonSubrole;
    }
    return NSAccessibilityUnknownSubrole;
}

- (id)accessibilityValue
{
    JNIEnv *env = [ThreadUtilities getJNIEnv];
    jobject axContext = [self axContextWithEnv:env];
    jobject selAccessible = getAxContextSelection(env, [self tabGroup], fIndex, fComponent);

    // Returns the current selection of the page tab list
    id val = [NSNumber numberWithBool:ObjectEquals(env, axContext, selAccessible, fComponent)];

    (*env)->DeleteLocalRef(env, selAccessible);
    (*env)->DeleteLocalRef(env, axContext);
    return val;
}

- (BOOL)accessibilityPerformPress {
    [self performPressAction];
    return YES;
}

@end
