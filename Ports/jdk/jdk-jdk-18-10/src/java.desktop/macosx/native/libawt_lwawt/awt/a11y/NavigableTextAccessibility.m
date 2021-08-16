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

#import "NavigableTextAccessibility.h"
#import "JavaAccessibilityUtilities.h"
#import "ThreadUtilities.h"
#import "JNIUtilities.h"

static jclass sjc_CAccessibility = NULL;
#define GET_CACCESSIBLITY_CLASS() \
     GET_CLASS(sjc_CAccessibility, "sun/lwawt/macosx/CAccessibility");
#define GET_CACCESSIBLITY_CLASS_RETURN(ret) \
     GET_CLASS_RETURN(sjc_CAccessibility, "sun/lwawt/macosx/CAccessibility", ret);

static jmethodID sjm_getAccessibleText = NULL;
#define GET_ACCESSIBLETEXT_METHOD_RETURN(ret) \
    GET_CACCESSIBLITY_CLASS_RETURN(ret); \
    GET_STATIC_METHOD_RETURN(sjm_getAccessibleText, sjc_CAccessibility, "getAccessibleText", \
              "(Ljavax/accessibility/Accessible;Ljava/awt/Component;)Ljavax/accessibility/AccessibleText;", ret);

static jclass sjc_CAccessibleText = NULL;
#define GET_CACCESSIBLETEXT_CLASS() \
    GET_CLASS(sjc_CAccessibleText, "sun/lwawt/macosx/CAccessibleText");
#define GET_CACCESSIBLETEXT_CLASS_RETURN(ret) \
    GET_CLASS_RETURN(sjc_CAccessibleText, "sun/lwawt/macosx/CAccessibleText", ret);

static jmethodID sjm_getAccessibleEditableText = NULL;
#define GET_ACCESSIBLEEDITABLETEXT_METHOD_RETURN(ret) \
    GET_CACCESSIBLETEXT_CLASS_RETURN(ret); \
    GET_STATIC_METHOD_RETURN(sjm_getAccessibleEditableText, sjc_CAccessibleText, "getAccessibleEditableText", \
              "(Ljavax/accessibility/Accessible;Ljava/awt/Component;)Ljavax/accessibility/AccessibleEditableText;", ret);


@implementation NavigableTextAccessibility

- (BOOL)accessibleIsPasswordText {
    return [fJavaRole isEqualToString:@"passwordtext"];
}

// NSAccessibilityElement protocol methods

- (NSRect)accessibilityFrameForRange:(NSRange)range
{
    JNIEnv *env = [ThreadUtilities getJNIEnv];
    GET_CACCESSIBLETEXT_CLASS_RETURN(NSMakeRect(0, 0, 0, 0));
    DECLARE_STATIC_METHOD_RETURN(jm_getBoundsForRange, sjc_CAccessibleText, "getBoundsForRange",
                         "(Ljavax/accessibility/Accessible;Ljava/awt/Component;II)[D", NSMakeRect(0, 0, 0, 0));
    jdoubleArray axBounds = (jdoubleArray)(*env)->CallStaticObjectMethod(env, sjc_CAccessibleText, jm_getBoundsForRange,
                              fAccessible, fComponent, range.location, range.length);
    CHECK_EXCEPTION();
    if (axBounds == NULL) return NSMakeRect(0, 0, 0, 0);

    // We cheat because we know that the array is 4 elements long (x, y, width, height)
    jdouble *values = (*env)->GetDoubleArrayElements(env, axBounds, 0);
    CHECK_EXCEPTION();

    NSRect bounds;
    bounds.origin.x = values[0];
    bounds.origin.y = [[[[self view] window] screen] frame].size.height - values[1] - values[3]; //values[1] is y-coord from top-left of screen. Flip. Account for the height (values[3]) when flipping
    bounds.size.width = values[2];
    bounds.size.height = values[3];
    (*env)->ReleaseDoubleArrayElements(env, axBounds, values, 0);
    return bounds;
}

- (NSInteger)accessibilityLineForIndex:(NSInteger)index
{
    JNIEnv *env = [ThreadUtilities getJNIEnv];
    GET_CACCESSIBLETEXT_CLASS_RETURN(-1);
    DECLARE_STATIC_METHOD_RETURN(jm_getLineNumberForIndex, sjc_CAccessibleText, "getLineNumberForIndex",
                           "(Ljavax/accessibility/Accessible;Ljava/awt/Component;I)I", -1);
    jint row = (*env)->CallStaticIntMethod(env, sjc_CAccessibleText, jm_getLineNumberForIndex,
                       fAccessible, fComponent, index);
    CHECK_EXCEPTION();
    if (row < 0) return -1;
    return row;
}

- (NSRange)accessibilityRangeForLine:(NSInteger)line
{
    JNIEnv *env = [ThreadUtilities getJNIEnv];
    GET_CACCESSIBLETEXT_CLASS_RETURN(NSRangeFromString(@""));
    DECLARE_STATIC_METHOD_RETURN(jm_getRangeForLine, sjc_CAccessibleText, "getRangeForLine",
                 "(Ljavax/accessibility/Accessible;Ljava/awt/Component;I)[I", NSRangeFromString(@""));
    jintArray axTextRange = (jintArray)(*env)->CallStaticObjectMethod(env, sjc_CAccessibleText,
                jm_getRangeForLine, fAccessible, fComponent, line);
    CHECK_EXCEPTION();
    if (axTextRange == NULL) return NSRangeFromString(@"");

    NSRange range = [javaIntArrayToNSRangeValue(env,axTextRange) rangeValue];
    (*env)->DeleteLocalRef(env, axTextRange);
    return range;
}

- (NSString *)accessibilityStringForRange:(NSRange)range
{
    JNIEnv *env = [ThreadUtilities getJNIEnv];
    GET_CACCESSIBLETEXT_CLASS_RETURN(nil);
    DECLARE_STATIC_METHOD_RETURN(jm_getStringForRange, sjc_CAccessibleText, "getStringForRange",
                 "(Ljavax/accessibility/Accessible;Ljava/awt/Component;II)Ljava/lang/String;", nil);
    jstring jstringForRange = (jstring)(*env)->CallStaticObjectMethod(env, sjc_CAccessibleText, jm_getStringForRange,
                            fAccessible, fComponent, range.location, range.length);
    CHECK_EXCEPTION();
    if (jstringForRange == NULL) return @"";
    NSString* str = JavaStringToNSString(env, jstringForRange);
    (*env)->DeleteLocalRef(env, jstringForRange);
    return str;
}

- (id)accessibilityValue
{
    JNIEnv *env = [ThreadUtilities getJNIEnv];
    GET_CACCESSIBLITY_CLASS_RETURN(nil);
    DECLARE_STATIC_METHOD_RETURN(sjm_getAccessibleName, sjc_CAccessibility, "getAccessibleName",
                          "(Ljavax/accessibility/Accessible;Ljava/awt/Component;)Ljava/lang/String;", nil);
    // cmcnote: inefficient to make three distinct JNI calls. Coalesce. radr://3951923
    GET_ACCESSIBLETEXT_METHOD_RETURN(@"");
    jobject axText = (*env)->CallStaticObjectMethod(env, sjc_CAccessibility,
                      sjm_getAccessibleText, fAccessible, fComponent);
    CHECK_EXCEPTION();
    if (axText == NULL) return nil;
    (*env)->DeleteLocalRef(env, axText);

    GET_ACCESSIBLEEDITABLETEXT_METHOD_RETURN(nil);
    jobject axEditableText = (*env)->CallStaticObjectMethod(env, sjc_CAccessibleText,
                       sjm_getAccessibleEditableText, fAccessible, fComponent);
    CHECK_EXCEPTION();
    if (axEditableText == NULL) return nil;

    DECLARE_STATIC_METHOD_RETURN(jm_getTextRange, sjc_CAccessibleText, "getTextRange",
                    "(Ljavax/accessibility/AccessibleEditableText;IILjava/awt/Component;)Ljava/lang/String;", nil);
    jobject jrange = (*env)->CallStaticObjectMethod(env, sjc_CAccessibleText, jm_getTextRange,
                       axEditableText, 0, getAxTextCharCount(env, axEditableText, fComponent), fComponent);
    CHECK_EXCEPTION();
    NSString *string = JavaStringToNSString(env, jrange);

    (*env)->DeleteLocalRef(env, jrange);
    (*env)->DeleteLocalRef(env, axEditableText);

    if (string == nil) string = @"";
    return string;
}

- (NSAccessibilitySubrole)accessibilitySubrole {
    if ([self accessibleIsPasswordText]) {
        return NSAccessibilitySecureTextFieldSubrole;
    }
    return nil;
}

- (NSRange)accessibilityRangeForIndex:(NSInteger)index
{
    JNIEnv *env = [ThreadUtilities getJNIEnv];
    GET_CACCESSIBLETEXT_CLASS_RETURN(NSRangeFromString(@""));
    DECLARE_STATIC_METHOD_RETURN(jm_getRangeForIndex, sjc_CAccessibleText, "getRangeForIndex",
                    "(Ljavax/accessibility/Accessible;Ljava/awt/Component;I)[I", NSRangeFromString(@""));
    jintArray axTextRange = (jintArray)(*env)->CallStaticObjectMethod(env, sjc_CAccessibleText, jm_getRangeForIndex,
                              fAccessible, fComponent, index);
    CHECK_EXCEPTION();
    if (axTextRange == NULL) return NSRangeFromString(@"");

    return [javaIntArrayToNSRangeValue(env, axTextRange) rangeValue];
}

- (NSAccessibilityRole)accessibilityRole {
    return [sRoles objectForKey:self.javaRole];
}

- (NSRange)accessibilityRangeForPosition:(NSPoint)point
{
   point.y = [[[[self view] window] screen] frame].size.height - point.y; // flip into java screen coords (0 is at upper-left corner of screen)

    JNIEnv *env = [ThreadUtilities getJNIEnv];
    GET_CACCESSIBLETEXT_CLASS_RETURN(NSRangeFromString(@""));
    DECLARE_STATIC_METHOD_RETURN(jm_getCharacterIndexAtPosition, sjc_CAccessibleText, "getCharacterIndexAtPosition",
                           "(Ljavax/accessibility/Accessible;Ljava/awt/Component;II)I", NSRangeFromString(@""));
    jint charIndex = (*env)->CallStaticIntMethod(env, sjc_CAccessibleText, jm_getCharacterIndexAtPosition,
                            fAccessible, fComponent, point.x, point.y);
    CHECK_EXCEPTION();
    if (charIndex == -1) return NSRangeFromString(@"");

    // AccessibleText.getIndexAtPoint returns -1 for an invalid point
    NSRange range = NSMakeRange(charIndex, 1); //range's length is 1 - one-character range
    return range;
}

- (NSString *)accessibilitySelectedText
{
    JNIEnv* env = [ThreadUtilities getJNIEnv];
    GET_CACCESSIBLETEXT_CLASS_RETURN(nil);
    DECLARE_STATIC_METHOD_RETURN(jm_getSelectedText, sjc_CAccessibleText, "getSelectedText",
              "(Ljavax/accessibility/Accessible;Ljava/awt/Component;)Ljava/lang/String;", nil);
    jobject axText = (*env)->CallStaticObjectMethod(env, sjc_CAccessibleText, jm_getSelectedText,
                        fAccessible, fComponent);
    CHECK_EXCEPTION();
    if (axText == NULL) return @"";
    NSString* str = JavaStringToNSString(env, axText);
    (*env)->DeleteLocalRef(env, axText);
    return str;
}

- (NSRange)accessibilitySelectedTextRange
{
    JNIEnv *env = [ThreadUtilities getJNIEnv];
    GET_CACCESSIBLETEXT_CLASS_RETURN(NSRangeFromString(@""));
    DECLARE_STATIC_METHOD_RETURN(jm_getSelectedTextRange, sjc_CAccessibleText, "getSelectedTextRange",
           "(Ljavax/accessibility/Accessible;Ljava/awt/Component;)[I", NSRangeFromString(@""));
    jintArray axTextRange = (*env)->CallStaticObjectMethod(env, sjc_CAccessibleText,
                jm_getSelectedTextRange, fAccessible, fComponent);
    CHECK_EXCEPTION();
    if (axTextRange == NULL) return NSRangeFromString(@"");

    return [javaIntArrayToNSRangeValue(env, axTextRange) rangeValue];
}

- (NSInteger)accessibilityNumberOfCharacters
{
    // cmcnote: should coalesce these two calls - radr://3951923
    // also, static text doesn't always have accessibleText. if axText is null, should get the charcount of the accessibleName instead
    JNIEnv *env = [ThreadUtilities getJNIEnv];
    GET_ACCESSIBLETEXT_METHOD_RETURN(0);
    jobject axText = (*env)->CallStaticObjectMethod(env, sjc_CAccessibility,
                     sjm_getAccessibleText, fAccessible, fComponent);
    CHECK_EXCEPTION();
    NSInteger num = getAxTextCharCount(env, axText, fComponent);
    (*env)->DeleteLocalRef(env, axText);
    return num;
}

- (NSInteger)accessibilityInsertionPointLineNumber
{
    JNIEnv *env = [ThreadUtilities getJNIEnv];
    GET_CACCESSIBLETEXT_CLASS_RETURN(0);
    DECLARE_STATIC_METHOD_RETURN(jm_getLineNumberForInsertionPoint, sjc_CAccessibleText,
             "getLineNumberForInsertionPoint", "(Ljavax/accessibility/Accessible;Ljava/awt/Component;)I", 0);
    jint row = (*env)->CallStaticIntMethod(env, sjc_CAccessibleText,
                  jm_getLineNumberForInsertionPoint, fAccessible, fComponent);
    CHECK_EXCEPTION();
    return row >= 0 ? row : 0;
}

- (void)setAccessibilitySelectedText:(NSString *)accessibilitySelectedText
{
    JNIEnv *env = [ThreadUtilities getJNIEnv];
    jstring jstringValue = NSStringToJavaString(env, accessibilitySelectedText);
    GET_CACCESSIBLETEXT_CLASS();
    DECLARE_STATIC_METHOD(jm_setSelectedText, sjc_CAccessibleText, "setSelectedText",
                   "(Ljavax/accessibility/Accessible;Ljava/awt/Component;Ljava/lang/String;)V");
    (*env)->CallStaticVoidMethod(env, sjc_CAccessibleText, jm_setSelectedText,
              fAccessible, fComponent, jstringValue);
    CHECK_EXCEPTION();
}

- (void)setAccessibilitySelectedTextRange:(NSRange)accessibilitySelectedTextRange
{
   jint startIndex = accessibilitySelectedTextRange.location;
    jint endIndex = startIndex + accessibilitySelectedTextRange.length;

    JNIEnv *env = [ThreadUtilities getJNIEnv];
    GET_CACCESSIBLETEXT_CLASS();
    DECLARE_STATIC_METHOD(jm_setSelectedTextRange, sjc_CAccessibleText, "setSelectedTextRange",
                  "(Ljavax/accessibility/Accessible;Ljava/awt/Component;II)V");
    (*env)->CallStaticVoidMethod(env, sjc_CAccessibleText, jm_setSelectedTextRange,
                  fAccessible, fComponent, startIndex, endIndex);
    CHECK_EXCEPTION();
}

- (BOOL)isAccessibilityEdited {
    return YES;
}

- (BOOL)isAccessibilityEnabled {
    return YES;
}

- (NSRect)accessibilityFrame
{
    return [super accessibilityFrame];
}

- (id)accessibilityParent
{
    return [super accessibilityParent];
}

/*
* Other text methods
- (NSRange)accessibilitySharedCharacterRange;
- (NSArray *)accessibilitySharedTextUIElements;
- (NSData *)accessibilityRTFForRange:(NSRange)range;
- (NSRange)accessibilityStyleRangeForIndex:(NSInteger)index;
*/

@end

