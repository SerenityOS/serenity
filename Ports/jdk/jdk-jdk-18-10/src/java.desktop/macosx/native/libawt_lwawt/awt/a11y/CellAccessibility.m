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

#import "CellAccessibility.h"
#import "ThreadUtilities.h"
#import "TableAccessibility.h"

@implementation CellAccessibility

// NSAccessibilityElement protocol methods

- (NSAccessibilityRole)accessibilityRole
{
    return NSAccessibilityCellRole;;
}

- (NSArray *)accessibilityChildren
{
    NSArray *children = [super accessibilityChildren];
    if (children == NULL) {
        NSString *javaRole = [self  javaRole];
        CommonComponentAccessibility *newChild = [CommonComponentAccessibility createWithParent:self
                                                                       accessible:self->fAccessible
                                                                             role:javaRole
                                                                            index:self->fIndex
                                                                          withEnv:[ThreadUtilities getJNIEnv]
                                                                         withView:self->fView
                                                                        isWrapped:NO];
        return [NSArray arrayWithObject:newChild];
    } else {
        return children;
    }
}

- (NSRect)accessibilityFrame
{
    return [super accessibilityFrame];
}

- (id)accessibilityParent
{
    return [super accessibilityParent];
}

- (NSRange)accessibilityRowIndexRange {
    NSInteger location = -1;
    if ([[(CommonComponentAccessibility *)fParent accessibilityParent] isKindOfClass:[TableAccessibility class]]) {
        TableAccessibility *table = [(CommonComponentAccessibility *)fParent accessibilityParent];
        location = [table accessibleRowAtIndex:fIndex];
    }

    return NSMakeRange(location, 1);
}

- (NSRange)accessibilityColumnIndexRange {
    NSInteger location = -1;
    if ([[(CommonComponentAccessibility *)fParent accessibilityParent] isKindOfClass:[TableAccessibility class]]) {
        TableAccessibility *table = [(CommonComponentAccessibility *)fParent accessibilityParent];
        location = [table accessibleColumnAtIndex:fIndex];
    }

    return NSMakeRange(location, 1);
}

@end
