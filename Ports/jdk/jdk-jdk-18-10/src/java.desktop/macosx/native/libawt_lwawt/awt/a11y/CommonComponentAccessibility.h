/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef JAVA_COMPONENT_ACCESSIBILITY
#define JAVA_COMPONENT_ACCESSIBILITY

#include "jni.h"

#import <AppKit/AppKit.h>
#import "JavaAccessibilityUtilities.h"

@interface CommonComponentAccessibility : NSAccessibilityElement {
    NSView *fView;
    NSObject *fParent;

    NSString *fNSRole;
    NSString *fJavaRole;

    jint fIndex;
    jobject fAccessible;
    jobject fComponent;

    NSMutableDictionary *fActions;
    NSMutableArray *fActionSelectors;
    NSObject *fActionsLOCK;
}

@property(nonnull, readonly) NSArray *actionSelectors;

- (id _Nonnull)initWithParent:(NSObject* _Nonnull)parent withEnv:(JNIEnv _Nonnull * _Nonnull)env withAccessible:(jobject _Nullable)accessible withIndex:(jint)index withView:(NSView* _Nonnull)view withJavaRole:(NSString* _Nullable)javaRole;
- (void)unregisterFromCocoaAXSystem;
- (void)postValueChanged;
- (void)postSelectedTextChanged;
- (void)postSelectionChanged;
- (void)postTitleChanged;
- (void)postTreeNodeExpanded;
- (void)postTreeNodeCollapsed;
- (BOOL)isEqual:(nonnull id)anObject;
- (BOOL)isAccessibleWithEnv:(JNIEnv _Nonnull * _Nonnull)env forAccessible:(nonnull jobject)accessible;

+ (void)postFocusChanged:(nullable id)message;

+ (void) initializeRolesMap;

+ (CommonComponentAccessibility* _Nullable) getComponentAccessibility:(NSString* _Nonnull)role;
+ (CommonComponentAccessibility * _Nullable) getComponentAccessibility:(NSString * _Nonnull)role andParent:(CommonComponentAccessibility * _Nonnull)parent;

+ (NSArray* _Nullable)childrenOfParent:(CommonComponentAccessibility* _Nonnull)parent withEnv:(JNIEnv _Nonnull * _Nonnull)env withChildrenCode:(NSInteger)whichChildren allowIgnored:(BOOL)allowIgnored;
+ (NSArray* _Nullable)childrenOfParent:(CommonComponentAccessibility* _Nonnull)parent withEnv:(JNIEnv _Nonnull * _Nonnull)env withChildrenCode:(NSInteger)whichChildren allowIgnored:(BOOL)allowIgnored recursive:(BOOL)recursive;
+ (CommonComponentAccessibility* _Nullable) createWithParent:(CommonComponentAccessibility* _Nullable)parent accessible:(jobject _Nonnull)jaccessible role:(NSString* _Nonnull)javaRole index:(jint)index withEnv:(JNIEnv _Nonnull * _Nonnull)env withView:(NSView* _Nonnull)view;
+ (CommonComponentAccessibility* _Nullable) createWithAccessible:(jobject _Nonnull)jaccessible role:(NSString* _Nonnull)role index:(jint)index withEnv:(JNIEnv _Nonnull * _Nonnull)env withView:(NSView* _Nonnull)view;
+ (CommonComponentAccessibility* _Nullable) createWithAccessible:(jobject _Nonnull)jaccessible withEnv:(JNIEnv _Nonnull * _Nonnull)env withView:(NSView* _Nonnull)view;

// If the isWraped parameter is true, then the object passed as a parent was created based on the same java component,
// but performs a different NSAccessibilityRole of a table cell, or a list row, or tree row,
// and we need to create an element whose role corresponds to the role in Java.
+ (CommonComponentAccessibility* _Nullable) createWithParent:(CommonComponentAccessibility* _Nullable)parent accessible:(jobject _Nonnull)jaccessible role:(NSString* _Nonnull)javaRole index:(jint)index withEnv:(JNIEnv _Nonnull * _Nonnull)env withView:(NSView* _Nonnull)view isWrapped:(BOOL)wrapped;

// The current parameter is used to bypass the check for an item's index on the parent so that the item is created. This is necessary,
// for example, for AccessibleJTreeNode, whose currentComponent has index -1
+ (CommonComponentAccessibility* _Nullable) createWithAccessible:(jobject _Nonnull)jaccessible withEnv:(JNIEnv _Nonnull * _Nonnull)env withView:(NSView* _Nonnull)view isCurrent:(BOOL)current;

- (jobject _Nullable)axContextWithEnv:(JNIEnv _Nonnull * _Nonnull)env;
- (NSView* _Nonnull)view;
- (NSWindow* _Nonnull)window;
- (id _Nonnull)parent;
- (NSString* _Nonnull)javaRole;

- (BOOL)isMenu;
- (BOOL)isSelected:(JNIEnv _Nonnull * _Nonnull)env;
- (BOOL)isSelectable:(JNIEnv _Nonnull * _Nonnull)env;
- (BOOL)isVisible:(JNIEnv _Nonnull * _Nonnull)env;

- (NSArray* _Nullable)accessibleChildrenWithChildCode:(NSInteger)childCode;

- (NSDictionary* _Nullable)getActions:(JNIEnv _Nonnull * _Nonnull)env;
- (void)getActionsWithEnv:(JNIEnv _Nonnull * _Nonnull)env;
- (BOOL)accessiblePerformAction:(NSAccessibilityActionName _Nonnull)actionName;

- (BOOL)performAccessibleAction:(int)index;

- (NSRect)accessibilityFrame;
- (id _Nullable)accessibilityParent;
- (BOOL)isAccessibilityElement;
@end

#endif
