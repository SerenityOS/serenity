/*
 * Copyright (c) 2011, 2018, Oracle and/or its affiliates. All rights reserved.
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

#import "JavaAccessibilityUtilities.h"
#import "JNIUtilities.h"

#import <AppKit/AppKit.h>
#import "ThreadUtilities.h"

static BOOL JavaAccessibilityIsSupportedAttribute(id element, NSString *attribute);
static void JavaAccessibilityLogError(NSString *message);
static void _JavaAccessibilityRaiseException(NSString *reason, SInt32 errorCode);
static NSString *AttributeWithoutAXPrefix(NSString *attribute);
static SEL JavaAccessibilityAttributeGetter(NSString *attribute);
static SEL JavaAccessibilityAttributeSettableTester(NSString *attribute);
static SEL JavaAccessibilityAttributeSetter(NSString *attribute);

NSString *const JavaAccessibilityIgnore = @"JavaAxIgnore";

NSMutableDictionary *sRoles = nil;
void initializeRoles();

// Unique
static jclass sjc_AccessibleState = NULL;
#define GET_ACCESSIBLESTATE_CLASS_RETURN(ret) \
     GET_CLASS_RETURN(sjc_AccessibleState, "javax/accessibility/AccessibleState", ret);

static jclass sjc_CAccessibility = NULL;

NSSize getAxComponentSize(JNIEnv *env, jobject axComponent, jobject component)
{
    DECLARE_CLASS_RETURN(jc_Dimension, "java/awt/Dimension", NSZeroSize);
    DECLARE_FIELD_RETURN(jf_width, jc_Dimension, "width", "I", NSZeroSize);
    DECLARE_FIELD_RETURN(jf_height, jc_Dimension, "height", "I", NSZeroSize);
    DECLARE_STATIC_METHOD_RETURN(jm_getSize, sjc_CAccessibility, "getSize",
           "(Ljavax/accessibility/AccessibleComponent;Ljava/awt/Component;)Ljava/awt/Dimension;", NSZeroSize);

    jobject dimension = (*env)->CallStaticObjectMethod(env, jc_Dimension, jm_getSize, axComponent, component);
    CHECK_EXCEPTION();

    if (dimension == NULL) return NSZeroSize;
    return NSMakeSize((*env)->GetIntField(env, dimension, jf_width), (*env)->GetIntField(env, dimension, jf_height));
}

NSString *getJavaRole(JNIEnv *env, jobject axComponent, jobject component)
{
    GET_CACCESSIBILITY_CLASS_RETURN(nil);
    DECLARE_STATIC_METHOD_RETURN(sjm_getAccessibleRole, sjc_CAccessibility, "getAccessibleRole",
                           "(Ljavax/accessibility/Accessible;Ljava/awt/Component;)Ljava/lang/String;", nil);
    jobject axRole = (*env)->CallStaticObjectMethod(env, sjc_CAccessibility, sjm_getAccessibleRole,
                      axComponent, component);
    CHECK_EXCEPTION();
    if (axRole == NULL) return @"unknown";

    NSString* str = JavaStringToNSString(env, axRole);
    (*env)->DeleteLocalRef(env, axRole);
    return str;
}

jobject getAxSelection(JNIEnv *env, jobject axContext, jobject component)
{
    GET_CACCESSIBILITY_CLASS_RETURN(nil);
    DECLARE_STATIC_METHOD_RETURN(jm_getAccessibleSelection, sjc_CAccessibility, "getAccessibleSelection",
            "(Ljavax/accessibility/AccessibleContext;Ljava/awt/Component;)Ljavax/accessibility/AccessibleSelection;", nil);
    jobject o = (*env)->CallStaticObjectMethod(env, sjc_CAccessibility, jm_getAccessibleSelection,
                      axContext, component);
    CHECK_EXCEPTION();
    return o;
}

jobject getAxContextSelection(JNIEnv *env, jobject axContext, jint index, jobject component)
{
    GET_CACCESSIBILITY_CLASS_RETURN(nil);
    DECLARE_STATIC_METHOD_RETURN(jm_ax_getAccessibleSelection, sjc_CAccessibility, "ax_getAccessibleSelection",
                  "(Ljavax/accessibility/AccessibleContext;ILjava/awt/Component;)Ljavax/accessibility/Accessible;", nil);
    return (*env)->CallStaticObjectMethod(env, sjc_CAccessibility, jm_ax_getAccessibleSelection,
                    axContext, index, component);
    CHECK_EXCEPTION();
}

void setAxContextSelection(JNIEnv *env, jobject axContext, jint index, jobject component)
{
    GET_CACCESSIBILITY_CLASS();
    DECLARE_STATIC_METHOD(jm_addAccessibleSelection, sjc_CAccessibility, "addAccessibleSelection",
                   "(Ljavax/accessibility/AccessibleContext;ILjava/awt/Component;)V");
    (*env)->CallStaticVoidMethod(env, sjc_CAccessibility, jm_addAccessibleSelection,
                    axContext, index, component);
    CHECK_EXCEPTION();
}

jobject getAxContext(JNIEnv *env, jobject accessible, jobject component)
{
    GET_CACCESSIBILITY_CLASS_RETURN(nil);
    DECLARE_STATIC_METHOD_RETURN(jm_getAccessibleContext, sjc_CAccessibility, "getAccessibleContext",
               "(Ljavax/accessibility/Accessible;Ljava/awt/Component;)Ljavax/accessibility/AccessibleContext;", nil);
    jobject o = (*env)->CallStaticObjectMethod(env, sjc_CAccessibility, jm_getAccessibleContext,
                    accessible, component);
    CHECK_EXCEPTION();
    return o;
}

BOOL isChildSelected(JNIEnv *env, jobject accessible, jint index, jobject component)
{
    GET_CACCESSIBILITY_CLASS_RETURN(NO);
    DECLARE_STATIC_METHOD_RETURN(jm_isAccessibleChildSelected, sjc_CAccessibility, "isAccessibleChildSelected",
                "(Ljavax/accessibility/Accessible;ILjava/awt/Component;)Z", NO);
    jboolean b = (*env)->CallStaticBooleanMethod(env, sjc_CAccessibility, jm_isAccessibleChildSelected,
                    accessible, index, component);
    CHECK_EXCEPTION();
    return b;
}

jobject getAxStateSet(JNIEnv *env, jobject axContext, jobject component)
{
    GET_CACCESSIBILITY_CLASS_RETURN(nil);
    DECLARE_STATIC_METHOD_RETURN(jm_getAccessibleStateSet, sjc_CAccessibility, "getAccessibleStateSet",
               "(Ljavax/accessibility/AccessibleContext;Ljava/awt/Component;)Ljavax/accessibility/AccessibleStateSet;", nil);
    jobject o = (*env)->CallStaticObjectMethod(env, sjc_CAccessibility, jm_getAccessibleStateSet,
                    axContext, component);
    CHECK_EXCEPTION();
    return o;
}

BOOL containsAxState(JNIEnv *env, jobject axContext, jobject axState, jobject component)
{
    GET_CACCESSIBILITY_CLASS_RETURN(NO);
    DECLARE_STATIC_METHOD_RETURN(jm_contains, sjc_CAccessibility, "contains",
               "(Ljavax/accessibility/AccessibleContext;Ljavax/accessibility/AccessibleState;Ljava/awt/Component;)Z", NO);
    jboolean b = (*env)->CallStaticBooleanMethod(env, sjc_CAccessibility, jm_contains, axContext, axState, component);
    CHECK_EXCEPTION();
    return b;
}

BOOL isVertical(JNIEnv *env, jobject axContext, jobject component)
{
    GET_ACCESSIBLESTATE_CLASS_RETURN(NO);
    DECLARE_STATIC_FIELD_RETURN(jm_VERTICAL, sjc_AccessibleState, "VERTICAL", "Ljavax/accessibility/AccessibleState;", NO);
    jobject axVertState = (*env)->GetStaticObjectField(env, sjc_AccessibleState, jm_VERTICAL);
    CHECK_EXCEPTION_NULL_RETURN(axVertState, NO);
    BOOL vertical = containsAxState(env, axContext, axVertState, component);
    (*env)->DeleteLocalRef(env, axVertState);
    return vertical;
}

BOOL isHorizontal(JNIEnv *env, jobject axContext, jobject component)
{
    GET_ACCESSIBLESTATE_CLASS_RETURN(NO);
    DECLARE_STATIC_FIELD_RETURN(jm_HORIZONTAL, sjc_AccessibleState, "HORIZONTAL", "Ljavax/accessibility/AccessibleState;", NO);
    jobject axHorizState = (*env)->GetStaticObjectField(env, sjc_AccessibleState, jm_HORIZONTAL);
    CHECK_EXCEPTION_NULL_RETURN(axHorizState, NO);
    BOOL horizontal = containsAxState(env, axContext, axHorizState, component);
    (*env)->DeleteLocalRef(env, axHorizState);
    return horizontal;
}

BOOL isShowing(JNIEnv *env, jobject axContext, jobject component)
{
    GET_ACCESSIBLESTATE_CLASS_RETURN(NO);
    DECLARE_STATIC_FIELD_RETURN(jm_SHOWING, sjc_AccessibleState, "SHOWING", "Ljavax/accessibility/AccessibleState;", NO);
    jobject axVisibleState = (*env)->GetStaticObjectField(env, sjc_AccessibleState, jm_SHOWING);
    CHECK_EXCEPTION_NULL_RETURN(axVisibleState, NO);
    BOOL showing = containsAxState(env, axContext, axVisibleState, component);
    (*env)->DeleteLocalRef(env, axVisibleState);
    return showing;
}

BOOL isSelectable(JNIEnv *env, jobject axContext, jobject component)
{
    GET_ACCESSIBLESTATE_CLASS_RETURN(NO);
    DECLARE_STATIC_FIELD_RETURN(jm_SELECTABLE,
                                    sjc_AccessibleState,
                                    "SELECTABLE",
                                    "Ljavax/accessibility/AccessibleState;", NO );
    jobject axSelectableState = (*env)->GetStaticObjectField(env, sjc_AccessibleState, jm_SELECTABLE);
    CHECK_EXCEPTION_NULL_RETURN(axSelectableState, NO);
    BOOL selectable = containsAxState(env, axContext, axSelectableState, component);
    (*env)->DeleteLocalRef(env, axSelectableState);
    return selectable;
}

BOOL isExpanded(JNIEnv *env, jobject axContext, jobject component)
{
    GET_ACCESSIBLESTATE_CLASS_RETURN(NO);
    DECLARE_STATIC_FIELD_RETURN(jm_EXPANDED,
                                    sjc_AccessibleState,
                                    "EXPANDED",
                                    "Ljavax/accessibility/AccessibleState;", NO );
    jobject axExpandedState = (*env)->GetStaticObjectField(env, sjc_AccessibleState, jm_EXPANDED);
    CHECK_EXCEPTION_NULL_RETURN(axExpandedState, NO);
    BOOL expanded = containsAxState(env, axContext, axExpandedState, component);
    (*env)->DeleteLocalRef(env, axExpandedState);
    return expanded;
}

NSPoint getAxComponentLocationOnScreen(JNIEnv *env, jobject axComponent, jobject component)
{
    GET_CACCESSIBILITY_CLASS_RETURN(NSZeroPoint);
    DECLARE_STATIC_METHOD_RETURN(jm_getLocationOnScreen, sjc_CAccessibility, "getLocationOnScreen",
                  "(Ljavax/accessibility/AccessibleComponent;Ljava/awt/Component;)Ljava/awt/Point;", NSZeroPoint);
    DECLARE_CLASS_RETURN(sjc_Point, "java/awt/Point", NSZeroPoint);
    DECLARE_FIELD_RETURN(sjf_X, sjc_Point, "x", "I", NSZeroPoint);
    DECLARE_FIELD_RETURN(sjf_Y, sjc_Point, "y", "I", NSZeroPoint);
    jobject jpoint = (*env)->CallStaticObjectMethod(env, sjc_CAccessibility, jm_getLocationOnScreen,
                      axComponent, component);
    CHECK_EXCEPTION();
    if (jpoint == NULL) return NSZeroPoint;
    return NSMakePoint((*env)->GetIntField(env, jpoint, sjf_X), (*env)->GetIntField(env, jpoint, sjf_Y));
}

jint getAxTextCharCount(JNIEnv *env, jobject axText, jobject component)
{
    GET_CACCESSIBILITY_CLASS_RETURN(0);
    DECLARE_STATIC_METHOD_RETURN(jm_getCharCount, sjc_CAccessibility, "getCharCount",
                  "(Ljavax/accessibility/AccessibleText;Ljava/awt/Component;)I", 0);
    int i = (*env)->CallStaticIntMethod(env, sjc_CAccessibility, jm_getCharCount, axText, component);
    CHECK_EXCEPTION();
    return i;
}

// The following JavaAccessibility methods are copied from the corresponding
// NSAccessibility methods in NSAccessibility.m.
//
// They implement a key-value-like coding scheme to transform messages like
//        [self accessibilityAttributeValue:NSAccessibilityEnabledAttribute]
// into calls on to specific methods like
//        [self accessibilityEnabledAttribute].

static NSString *AttributeWithoutAXPrefix(NSString *attribute)
{
    return [attribute hasPrefix:@"AX"] ? [attribute substringFromIndex:2] : attribute;
}

static SEL JavaAccessibilityAttributeGetter(NSString *attribute)
{
    return NSSelectorFromString([NSString stringWithFormat:@"accessibility%@Attribute", AttributeWithoutAXPrefix(attribute)]);
}

static SEL JavaAccessibilityAttributeSettableTester(NSString *attribute)
{
    return NSSelectorFromString([NSString stringWithFormat:@"accessibilityIs%@AttributeSettable", AttributeWithoutAXPrefix(attribute)]);
}

static SEL JavaAccessibilityAttributeSetter(NSString *attribute)
{
    return NSSelectorFromString([NSString stringWithFormat:@"accessibilitySet%@Attribute:", AttributeWithoutAXPrefix(attribute)]);
}

id JavaAccessibilityAttributeValue(id element, NSString *attribute)
{
    if (!JavaAccessibilityIsSupportedAttribute(element, attribute)) return nil;

    SEL getter = JavaAccessibilityAttributeGetter(attribute);
#ifdef JAVA_AX_DEBUG_PARMS
    if (![element respondsToSelector:getter]) {
        JavaAccessibilityRaiseUnimplementedAttributeException(__FUNCTION__, element, attribute);
        return nil;
    }
#endif

    return [element performSelector:getter];
}

BOOL JavaAccessibilityIsAttributeSettable(id element, NSString *attribute)
{
    if (!JavaAccessibilityIsSupportedAttribute(element, attribute)) return NO;

    SEL tester = JavaAccessibilityAttributeSettableTester(attribute);
#ifdef JAVA_AX_DEBUG_PARMS
    if (![element respondsToSelector:tester]) {
        JavaAccessibilityRaiseUnimplementedAttributeException(__FUNCTION__, element, attribute);
        return NO;
    }
#endif

    return [element performSelector:tester] != nil;
}

void JavaAccessibilitySetAttributeValue(id element, NSString *attribute ,id value)
{
    if (!JavaAccessibilityIsSupportedAttribute(element, attribute)) return;

    SEL setter = JavaAccessibilityAttributeSetter(attribute);
    if (![element accessibilityIsAttributeSettable:attribute]) return;

#ifdef JAVA_AX_DEBUG_PARMS
    if (![element respondsToSelector:setter]) {
        JavaAccessibilityRaiseUnimplementedAttributeException(__FUNCTION__, element, attribute);
        return;
    }
#endif

    [element performSelector:setter withObject:value];
}

static BOOL JavaAccessibilityIsSupportedAttribute(id element, NSString *attribute)
{
    return [[element accessibilityAttributeNames] indexOfObject:attribute] != NSNotFound;
}

/*
 * Class:     sun_lwawt_macosx_CAccessibility
 * Method:    roleKey
 * Signature: (Ljavax/accessibility/AccessibleRole;)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_sun_lwawt_macosx_CAccessibility_roleKey
(JNIEnv *env, jclass clz, jobject axRole)
{
    DECLARE_CLASS_RETURN(sjc_AccessibleRole, "javax/accessibility/AccessibleRole", NULL);
    DECLARE_FIELD_RETURN(sjf_key, sjc_AccessibleRole, "key", "Ljava/lang/String;", NULL);
    return (*env)->GetObjectField(env, axRole, sjf_key);
}


// errors from NSAccessibilityErrors
void JavaAccessibilityRaiseSetAttributeToIllegalTypeException(const char *functionName, id element, NSString *attribute, id value)
{
    NSString *reason = [NSString stringWithFormat:@"%s: Attempt set \"%@\" attribute to illegal type of value (%@:%@) for element: %@", functionName, attribute, [value class], value, element];
    _JavaAccessibilityRaiseException(reason, kAXErrorIllegalArgument);
}

void JavaAccessibilityRaiseUnimplementedAttributeException(const char *functionName, id element, NSString *attribute)
{
    NSString *reason = [NSString stringWithFormat:@"%s: \"%@\" attribute unimplemented by element: %@", functionName, attribute, element];
    _JavaAccessibilityRaiseException(reason, kAXErrorFailure);
}

void JavaAccessibilityRaiseIllegalParameterTypeException(const char *functionName, id element, NSString *attribute, id parameter)
{
    NSString *reason = [NSString stringWithFormat:@"%s: \"%@\" parameterized attribute passed illegal type of parameter (%@:%@) for element: %@", functionName, attribute, [parameter class], parameter, element];
    _JavaAccessibilityRaiseException(reason, kAXErrorIllegalArgument);
}

static void _JavaAccessibilityRaiseException(NSString *reason, SInt32 errorCode)
{
    JavaAccessibilityLogError(reason);
    [[NSException exceptionWithName:NSAccessibilityException reason:reason userInfo:[NSDictionary dictionaryWithObjectsAndKeys:[NSNumber numberWithInt:errorCode], NSAccessibilityErrorCodeExceptionInfo, nil]] raise];
}

static void JavaAccessibilityLogError(NSString *message)
{
    NSLog(@"!!! %@", message);
}

/*
 * Returns Object.equals for the two items
 * This may use LWCToolkit.invokeAndWait(); don't call while holding fLock
 * and try to pass a component so the event happens on the correct thread.
 */
BOOL ObjectEquals(JNIEnv *env, jobject a, jobject b, jobject component)
{
    DECLARE_CLASS_RETURN(sjc_Object, "java/lang/Object", NO);
    DECLARE_METHOD_RETURN(jm_equals, sjc_Object, "equals", "(Ljava/lang/Object;)Z", NO);

    if ((a == NULL) && (b == NULL)) return YES;
    if ((a == NULL) || (b == NULL)) return NO;

    if (pthread_main_np() != 0) {
        // If we are on the AppKit thread
        DECLARE_CLASS_RETURN(sjc_LWCToolkit, "sun/lwawt/macosx/LWCToolkit", NO);
        DECLARE_STATIC_METHOD_RETURN(jm_doEquals, sjc_LWCToolkit, "doEquals",
                                     "(Ljava/lang/Object;Ljava/lang/Object;Ljava/awt/Component;)Z", NO);
        return (*env)->CallStaticBooleanMethod(env, sjc_LWCToolkit, jm_doEquals, a, b, component);
        CHECK_EXCEPTION();
    }

    jboolean jb = (*env)->CallBooleanMethod(env, a, jm_equals, b);
    CHECK_EXCEPTION();
    return jb;
}

/*
 * The java/lang/Number concrete class could be for any of the Java primitive
 * numerical types or some other subclass.
 * All existing A11Y code uses Integer so that is what we look for first
 * But all must be able to return a double and NSNumber accepts a double,
 * so that's the fall back.
 */
NSNumber* JavaNumberToNSNumber(JNIEnv *env, jobject jnumber) {
    if (jnumber == NULL) {
        return nil;
    }
    DECLARE_CLASS_RETURN(jnumber_Class, "java/lang/Number", nil);
    DECLARE_CLASS_RETURN(jinteger_Class, "java/lang/Integer", nil);
    DECLARE_METHOD_RETURN(jm_intValue, jnumber_Class, "intValue", "()I", nil);
    DECLARE_METHOD_RETURN(jm_doubleValue, jnumber_Class, "doubleValue", "()D", nil);
    if ((*env)->IsInstanceOf(env, jnumber, jinteger_Class)) {
        jint i = (*env)->CallIntMethod(env, jnumber, jm_intValue);
        CHECK_EXCEPTION();
        return [NSNumber numberWithInteger:i];
    } else {
        jdouble d = (*env)->CallDoubleMethod(env, jnumber, jm_doubleValue);
        CHECK_EXCEPTION();
        return [NSNumber numberWithDouble:d];
    }
}

/*
 * Converts an int array to an NSRange wrapped inside an NSValue
 * takes [start, end] values and returns [start, end - start]
 */
NSValue *javaIntArrayToNSRangeValue(JNIEnv* env, jintArray array) {
    jint *values = (*env)->GetIntArrayElements(env, array, 0);
    if (values == NULL) {
        // Note: Java will not be on the stack here so a java exception can't happen and no need to call ExceptionCheck.
        NSLog(@"%s failed calling GetIntArrayElements", __FUNCTION__);
        return nil;
    };
    NSValue *value = [NSValue valueWithRange:NSMakeRange(values[0], values[1] - values[0])];
    (*env)->ReleaseIntArrayElements(env, array, values, 0);
    return value;
}

// end appKit copies

/*
 To get the roles below, verify the perl has table below called macRoleCodes is correct.
 Then copy the perl code into a perl script called makeAxTables.pl (make
 sure to chmod +x makeAxTables.pl). Then run the perl script like this:

 ./makeAxTables.pl /Builds/jdk1_4_1/

 It will then write the void initializeRoles() method below to stdout.

 Any new AccessibleRole items that aren't in the perl hash table will be written out as follows:
 // Unknown AccessibleRole: <role>

 Add these unknowns to the perl hash table and re-run the script, and use the new generated table.
*/

// NOTE: Don't modify this directly. It is machine generated. See below
void initializeRoles()
{
    sRoles = [[NSMutableDictionary alloc] initWithCapacity:56];

    [sRoles setObject:JavaAccessibilityIgnore forKey:@"alert"];
    [sRoles setObject:NSAccessibilityGroupRole forKey:@"awtcomponent"];
    [sRoles setObject:NSAccessibilityGroupRole forKey:@"canvas"];
    [sRoles setObject:NSAccessibilityCheckBoxRole forKey:@"checkbox"];
    [sRoles setObject:JavaAccessibilityIgnore forKey:@"colorchooser"];
    [sRoles setObject:NSAccessibilityColumnRole forKey:@"columnheader"];
    [sRoles setObject:NSAccessibilityComboBoxRole forKey:@"combobox"];
    [sRoles setObject:NSAccessibilityTextFieldRole forKey:@"dateeditor"];
    [sRoles setObject:NSAccessibilityImageRole forKey:@"desktopicon"];
    [sRoles setObject:JavaAccessibilityIgnore forKey:@"desktoppane"];
    [sRoles setObject:JavaAccessibilityIgnore forKey:@"dialog"];
    [sRoles setObject:JavaAccessibilityIgnore forKey:@"directorypane"];
    [sRoles setObject:JavaAccessibilityIgnore forKey:@"filechooser"];
    [sRoles setObject:JavaAccessibilityIgnore forKey:@"filler"];
    [sRoles setObject:JavaAccessibilityIgnore forKey:@"fontchooser"];
    [sRoles setObject:JavaAccessibilityIgnore forKey:@"frame"];
    [sRoles setObject:JavaAccessibilityIgnore forKey:@"glasspane"];
    [sRoles setObject:NSAccessibilityGroupRole forKey:@"groupbox"];
    [sRoles setObject:NSAccessibilityStaticTextRole forKey:@"hyperlink"]; //maybe a group?
    [sRoles setObject:NSAccessibilityImageRole forKey:@"icon"];
    [sRoles setObject:NSAccessibilityGroupRole forKey:@"internalframe"];
    [sRoles setObject:NSAccessibilityStaticTextRole forKey:@"label"];
    [sRoles setObject:JavaAccessibilityIgnore forKey:@"layeredpane"];
    [sRoles setObject:NSAccessibilityListRole forKey:@"list"]; // maybe a group? AccessibleRole.java says a list is: "An object that presents a list of objects to the user and allows the user to select one or more of them."
    [sRoles setObject:NSAccessibilityListRole forKey:@"listitem"];
    [sRoles setObject:NSAccessibilityMenuRole forKey:@"menu"];
    [sRoles setObject:NSAccessibilityMenuBarRole forKey:@"menubar"];
    [sRoles setObject:NSAccessibilityMenuItemRole forKey:@"menuitem"];
    [sRoles setObject:JavaAccessibilityIgnore forKey:@"optionpane"];
    [sRoles setObject:NSAccessibilityRadioButtonRole forKey:@"pagetab"]; // cmcnote: cocoa tabs are radio buttons - one selected button out of a group of options
    [sRoles setObject:NSAccessibilityTabGroupRole forKey:@"pagetablist"];
    [sRoles setObject:JavaAccessibilityIgnore forKey:@"panel"];
    [sRoles setObject:NSAccessibilityTextFieldRole forKey:@"passwordtext"];
    [sRoles setObject:NSAccessibilityPopUpButtonRole forKey:@"popupmenu"];
    [sRoles setObject:NSAccessibilityProgressIndicatorRole forKey:@"progressbar"];
    [sRoles setObject:NSAccessibilityButtonRole forKey:@"pushbutton"];
    [sRoles setObject:NSAccessibilityRadioButtonRole forKey:@"radiobutton"];
    [sRoles setObject:JavaAccessibilityIgnore forKey:@"rootpane"];
    [sRoles setObject:NSAccessibilityRowRole forKey:@"rowheader"];
    [sRoles setObject:NSAccessibilityScrollBarRole forKey:@"scrollbar"];
    [sRoles setObject:NSAccessibilityScrollAreaRole forKey:@"scrollpane"];
    [sRoles setObject:JavaAccessibilityIgnore forKey:@"separator"];
    [sRoles setObject:NSAccessibilitySliderRole forKey:@"slider"];
    [sRoles setObject:NSAccessibilityIncrementorRole forKey:@"spinbox"];
    [sRoles setObject:NSAccessibilitySplitGroupRole forKey:@"splitpane"];
    [sRoles setObject:NSAccessibilityValueIndicatorRole forKey:@"statusbar"];
    [sRoles setObject:NSAccessibilityGroupRole forKey:@"swingcomponent"];
    [sRoles setObject:NSAccessibilityTableRole forKey:@"table"];
    [sRoles setObject:NSAccessibilityTextFieldRole forKey:@"text"];
    [sRoles setObject:NSAccessibilityTextAreaRole forKey:@"textarea"]; // supports top/bottom of document notifications: CAccessability.getAccessibleRole()
    [sRoles setObject:NSAccessibilityCheckBoxRole forKey:@"togglebutton"];
    [sRoles setObject:NSAccessibilityToolbarRole forKey:@"toolbar"];
    [sRoles setObject:JavaAccessibilityIgnore forKey:@"tooltip"];
    [sRoles setObject:NSAccessibilityOutlineRole forKey:@"tree"];
    [sRoles setObject:NSAccessibilityUnknownRole forKey:@"unknown"];
    [sRoles setObject:JavaAccessibilityIgnore forKey:@"viewport"];
    [sRoles setObject:JavaAccessibilityIgnore forKey:@"window"];
}
