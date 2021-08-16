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

#import "TableRowAccessibility.h"
#import "JavaAccessibilityAction.h"
#import "JavaAccessibilityUtilities.h"
#import "TableAccessibility.h"
#import "CellAccessibility.h"
#import "ThreadUtilities.h"
#import "JNIUtilities.h"
#import "sun_lwawt_macosx_CAccessibility.h"

static jclass sjc_CAccessibility = NULL;

static jmethodID jm_getChildrenAndRoles = NULL;
#define GET_CHILDRENANDROLES_METHOD_RETURN(ret) \
    GET_CACCESSIBILITY_CLASS_RETURN(ret); \
    GET_STATIC_METHOD_RETURN(jm_getChildrenAndRoles, sjc_CAccessibility, "getChildrenAndRoles",\
                      "(Ljavax/accessibility/Accessible;Ljava/awt/Component;IZ)[Ljava/lang/Object;", ret);

@implementation TableRowAccessibility

// NSAccessibilityElement protocol methods

- (NSAccessibilityRole)accessibilityRole
{
    return NSAccessibilityRowRole;
}

- (NSAccessibilitySubrole)accessibilitySubrole
{
    return NSAccessibilityTableRowSubrole;
}

- (NSArray *)accessibilityChildren
{
    NSArray *children = [super accessibilityChildren];
    if (children == nil) {
        JNIEnv *env = [ThreadUtilities getJNIEnv];
        CommonComponentAccessibility *parent = [self accessibilityParent];
        if (parent->fAccessible == NULL) return nil;
        GET_CHILDRENANDROLES_METHOD_RETURN(nil);
        jobjectArray jchildrenAndRoles = (jobjectArray)(*env)->CallStaticObjectMethod(env, sjc_CAccessibility, jm_getChildrenAndRoles,
                                                                                      parent->fAccessible, parent->fComponent, sun_lwawt_macosx_CAccessibility_JAVA_AX_ALL_CHILDREN, NO);
        CHECK_EXCEPTION();
        if (jchildrenAndRoles == NULL) return nil;

        jsize arrayLen = (*env)->GetArrayLength(env, jchildrenAndRoles);
        NSMutableArray *childrenCells = [NSMutableArray arrayWithCapacity:arrayLen/2];

        NSUInteger childIndex = fIndex * [(TableAccessibility *)parent accessibilityColumnCount];
        NSInteger i = childIndex * 2;
        NSInteger n = (fIndex + 1) * [(TableAccessibility *)parent accessibilityColumnCount] * 2;
        for(i; i < n; i+=2)
        {
            jobject /* Accessible */ jchild = (*env)->GetObjectArrayElement(env, jchildrenAndRoles, i);
            jobject /* String */ jchildJavaRole = (*env)->GetObjectArrayElement(env, jchildrenAndRoles, i+1);

            NSString *childJavaRole = nil;
            if (jchildJavaRole != NULL) {
                DECLARE_CLASS_RETURN(sjc_AccessibleRole, "javax/accessibility/AccessibleRole", nil);
                DECLARE_FIELD_RETURN(sjf_key, sjc_AccessibleRole, "key", "Ljava/lang/String;", nil);
                jobject jkey = (*env)->GetObjectField(env, jchildJavaRole, sjf_key);
                CHECK_EXCEPTION();
                childJavaRole = JavaStringToNSString(env, jkey);
                (*env)->DeleteLocalRef(env, jkey);
            }

            CellAccessibility *child = [[CellAccessibility alloc] initWithParent:self
                                                                         withEnv:env
                                                                  withAccessible:jchild
                                                                       withIndex:childIndex
                                                                        withView:self->fView
                                                                    withJavaRole:childJavaRole];
            [childrenCells addObject:[[child retain] autorelease]];

            (*env)->DeleteLocalRef(env, jchild);
            (*env)->DeleteLocalRef(env, jchildJavaRole);

            childIndex++;
        }
        (*env)->DeleteLocalRef(env, jchildrenAndRoles);
        return childrenCells;
    } else {
        return children;
    }
}

- (NSInteger)accessibilityIndex
{
    return self->fIndex;
}

- (NSString *)accessibilityLabel
{
    NSString *accessibilityName = @"";
    NSArray *children = [self accessibilityChildren];
        for (id cell in children) {
            if ([accessibilityName isEqualToString:@""]) {
                accessibilityName = [cell accessibilityLabel];
            } else {
                accessibilityName = [accessibilityName stringByAppendingFormat:@", %@", [cell accessibilityLabel]];
            }
        }
        return accessibilityName;
}

- (id)accessibilityParent
{
    return [super accessibilityParent];
}

- (NSRect)accessibilityFrame
{
        int height = [[[self accessibilityChildren] objectAtIndex:0] accessibilityFrame].size.height;
        int width = 0;
        NSPoint point = [[[self accessibilityChildren] objectAtIndex:0] accessibilityFrame].origin;
        for (id cell in [self accessibilityChildren]) {
            width += [cell accessibilityFrame].size.width;
        }
        return NSMakeRect(point.x, point.y, width, height);
}

@end
