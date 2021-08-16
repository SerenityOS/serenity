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

#import "OutlineAccessibility.h"
#import "JavaAccessibilityUtilities.h"
#import "ThreadUtilities.h"
#import "JNIUtilities.h"

static jclass sjc_CAccessibility = NULL;

static jmethodID sjm_isTreeRootVisible = NULL;
#define GET_ISTREEROOTVISIBLE_METHOD_RETURN(ret) \
    GET_CACCESSIBILITY_CLASS_RETURN(ret); \
    GET_STATIC_METHOD_RETURN(sjm_isTreeRootVisible, sjc_CAccessibility, "isTreeRootVisible", \
                     "(Ljavax/accessibility/Accessible;Ljava/awt/Component;)Z", ret);

@implementation OutlineAccessibility

- (BOOL)isTreeRootVisible
{
    JNIEnv *env = [ThreadUtilities getJNIEnv];
    GET_ISTREEROOTVISIBLE_METHOD_RETURN(NO);
    bool isTreeRootVisible = (*env)->CallStaticBooleanMethod(env, sjc_CAccessibility, sjm_isTreeRootVisible, fAccessible, fComponent);
    CHECK_EXCEPTION();
    return isTreeRootVisible;
}

// NSAccessibilityElement protocol methods

- (NSString *)accessibilityLabel
{
    return [[super accessibilityLabel] isEqualToString:@"list"] ? @"tree" : [super accessibilityLabel];
}

@end
