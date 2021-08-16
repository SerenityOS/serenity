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

#import "TabGroupAccessibility.h"
#import "TabButtonAccessibility.h"
#import "../JavaAccessibilityUtilities.h"
#import "ThreadUtilities.h"
#import "JNIUtilities.h"
#import "sun_lwawt_macosx_CAccessibility.h"

static jclass sjc_CAccessibility = NULL;

static jmethodID jm_getChildrenAndRoles = NULL;
#define GET_CHILDRENANDROLES_METHOD_RETURN(ret) \
    GET_CACCESSIBILITY_CLASS_RETURN(ret); \
    GET_STATIC_METHOD_RETURN(jm_getChildrenAndRoles, sjc_CAccessibility, "getChildrenAndRoles",\
                      "(Ljavax/accessibility/Accessible;Ljava/awt/Component;IZ)[Ljava/lang/Object;", ret);

@implementation TabGroupAccessibility

- (id)currentTabWithEnv:(JNIEnv *)env withAxContext:(jobject)axContext
{
    NSArray *tabs = [self tabButtonsWithEnv:env withTabGroupAxContext:axContext withTabCode:sun_lwawt_macosx_CAccessibility_JAVA_AX_ALL_CHILDREN allowIgnored:NO];

    // Looking at the JTabbedPane sources, there is always one AccessibleSelection.
    jobject selAccessible = getAxContextSelection(env, axContext, 0, fComponent);
    if (selAccessible == NULL) return nil;

    // Go through the tabs and find selAccessible
    _numTabs = [tabs count];
    CommonComponentAccessibility *aTab;
    NSInteger i;
    for (i = 0; i < _numTabs; i++) {
        aTab = (CommonComponentAccessibility *)[tabs objectAtIndex:i];
        if ([aTab isAccessibleWithEnv:env forAccessible:selAccessible]) {
            (*env)->DeleteLocalRef(env, selAccessible);
            return aTab;
        }
    }
    (*env)->DeleteLocalRef(env, selAccessible);
    return nil;
}

- (NSArray *)tabButtonsWithEnv:(JNIEnv *)env withTabGroupAxContext:(jobject)axContext withTabCode:(NSInteger)whichTabs allowIgnored:(BOOL)allowIgnored
{
    GET_CHILDRENANDROLES_METHOD_RETURN(nil);
    jobjectArray jtabsAndRoles = (jobjectArray)(*env)->CallStaticObjectMethod(env, sjc_CAccessibility, jm_getChildrenAndRoles,
                                  fAccessible, fComponent, whichTabs, allowIgnored);
    CHECK_EXCEPTION();
    if(jtabsAndRoles == NULL) return nil;

    jsize arrayLen = (*env)->GetArrayLength(env, jtabsAndRoles);
    if (arrayLen == 0) {
        (*env)->DeleteLocalRef(env, jtabsAndRoles);
        return nil;
    }
    NSMutableArray *tabs = [NSMutableArray arrayWithCapacity:(arrayLen/2)];

    // all of the tabs have the same role, so we can just find out what that is here and use it for all the tabs
    jobject jtabJavaRole = (*env)->GetObjectArrayElement(env, jtabsAndRoles, 1); // the array entries alternate between tab/role, starting with tab. so the first role is entry 1.
    if (jtabJavaRole == NULL) {
        (*env)->DeleteLocalRef(env, jtabsAndRoles);
        return nil;
    }
    DECLARE_CLASS_RETURN(sjc_AccessibleRole, "javax/accessibility/AccessibleRole", nil);
    DECLARE_FIELD_RETURN(sjf_key, sjc_AccessibleRole, "key", "Ljava/lang/String;", nil);
    jobject jkey = (*env)->GetObjectField(env, jtabJavaRole, sjf_key);
    CHECK_EXCEPTION();
    NSString *tabJavaRole = JavaStringToNSString(env, jkey);
    (*env)->DeleteLocalRef(env, jkey);

    NSInteger i;
    NSUInteger tabIndex = (whichTabs >= 0) ? whichTabs : 0; // if we're getting one particular child, make sure to set its index correctly
    for(i = 0; i < arrayLen; i+=2) {
        jobject jtab = (*env)->GetObjectArrayElement(env, jtabsAndRoles, i);
        CommonComponentAccessibility *tab = [[[TabButtonAccessibility alloc] initWithParent:self withEnv:env withAccessible:jtab withIndex:tabIndex withTabGroup:axContext withView:[self view] withJavaRole:tabJavaRole] autorelease];
        (*env)->DeleteLocalRef(env, jtab);
        [tabs addObject:tab];
        tabIndex++;
    }
    (*env)->DeleteLocalRef(env, jtabsAndRoles);
    return tabs;
}

- (NSArray *)contentsWithEnv:(JNIEnv *)env withTabGroupAxContext:(jobject)axContext withTabCode:(NSInteger)whichTabs allowIgnored:(BOOL)allowIgnored
{
    // Contents are the children of the selected tab.
    id currentTab = [self currentTabWithEnv:env withAxContext:axContext];
    if (currentTab == nil) return nil;

    NSArray *contents = [CommonComponentAccessibility childrenOfParent:currentTab withEnv:env withChildrenCode:whichTabs allowIgnored:allowIgnored];
    if ([contents count] <= 0) return nil;
    return contents;
}

- (NSInteger)numTabs
{
    if (_numTabs == -1) {
        _numTabs = [[self accessibilityTabsAttribute] count];
    }
    return _numTabs;
}

// NSAccessibilityElement protocol methods

- (NSArray *)accessibilityTabs
{
    JNIEnv *env = [ThreadUtilities getJNIEnv];
    jobject axContext = [self axContextWithEnv:env];
    id tabs = [self tabButtonsWithEnv:env withTabGroupAxContext:axContext withTabCode:sun_lwawt_macosx_CAccessibility_JAVA_AX_ALL_CHILDREN allowIgnored:NO];
    (*env)->DeleteLocalRef(env, axContext);
    return tabs;
}

- (NSArray *)accessibilityContents
{
    JNIEnv *env = [ThreadUtilities getJNIEnv];
    jobject axContext = [self axContextWithEnv:env];
    NSArray* cont = [self contentsWithEnv:env withTabGroupAxContext:axContext withTabCode:sun_lwawt_macosx_CAccessibility_JAVA_AX_ALL_CHILDREN allowIgnored:NO];
    (*env)->DeleteLocalRef(env, axContext);
    return cont;
}

- (id)accessibilityValue
{
    JNIEnv *env = [ThreadUtilities getJNIEnv];
    jobject axContext = [self axContextWithEnv:env];
    id val = [self currentTabWithEnv:env withAxContext:axContext];
    (*env)->DeleteLocalRef(env, axContext);
    return val;
}

- (NSArray *)accessibilityChildren
{
    //children = AXTabs + AXContents
    NSArray *tabs = [self accessibilityTabs];
    NSArray *contents = [self accessibilityContents];

    NSMutableArray *children = [NSMutableArray arrayWithCapacity:[tabs count] + [contents count]];
    [children addObjectsFromArray:tabs];
    [children addObjectsFromArray:contents];

    return (NSArray *)children;
}

- (NSArray *)accessibilityArrayAttributeValues:(NSAccessibilityAttributeName)attribute index:(NSUInteger)index maxCount:(NSUInteger)maxCount
{
    NSArray *result = nil;
    if ( (maxCount == 1) && [attribute isEqualToString:NSAccessibilityChildrenAttribute]) {
        // Children codes for ALL, SELECTED, VISIBLE are <0. If the code is >=0, we treat it as an index to a single child
        JNIEnv *env = [ThreadUtilities getJNIEnv];
        jobject axContext = [self axContextWithEnv:env];

        //children = AXTabs + AXContents
        NSArray *children = [self tabButtonsWithEnv:env withTabGroupAxContext:axContext withTabCode:index allowIgnored:NO]; // first look at the tabs
        if ([children count] > 0) {
            result = children;
         } else {
            children= [self contentsWithEnv:env withTabGroupAxContext:axContext withTabCode:(index-[self numTabs]) allowIgnored:NO];
            if ([children count] > 0) {
                result = children;
            }
        }
        (*env)->DeleteLocalRef(env, axContext);
    } else {
        result = [super accessibilityArrayAttributeValues:attribute index:index maxCount:maxCount];
    }
    return result;
}

- (void)setAccessibilityValue:(id)accessibilityValue
{
    // set the current tab
    NSNumber *number = (NSNumber *)accessibilityValue;
    if (![number boolValue]) return;

    JNIEnv *env = [ThreadUtilities getJNIEnv];
    jobject axContext = [self axContextWithEnv:env];
    setAxContextSelection(env, axContext, fIndex, fComponent);
    (*env)->DeleteLocalRef(env, axContext);
}

@end
