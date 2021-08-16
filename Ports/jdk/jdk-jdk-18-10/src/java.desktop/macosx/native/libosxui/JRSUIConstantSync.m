/*
 * Copyright (c) 2011, 2012, Oracle and/or its affiliates. All rights reserved.
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

#import "JNIUtilities.h"
#import <JavaRuntimeSupport/JavaRuntimeSupport.h>

#import "apple_laf_JRSUIConstants.h"
#import "apple_laf_JRSUIConstants_Key.h"
#import "apple_laf_JRSUIConstants_AlignmentVertical.h"
#import "apple_laf_JRSUIConstants_AlignmentHorizontal.h"
#import "apple_laf_JRSUIConstants_Animating.h"
#import "apple_laf_JRSUIConstants_ArrowsOnly.h"
#import "apple_laf_JRSUIConstants_BooleanValue.h"
#import "apple_laf_JRSUIConstants_Direction.h"
#import "apple_laf_JRSUIConstants_Focused.h"
#import "apple_laf_JRSUIConstants_FrameOnly.h"
#import "apple_laf_JRSUIConstants_IndicatorOnly.h"
#import "apple_laf_JRSUIConstants_NoIndicator.h"
#import "apple_laf_JRSUIConstants_NothingToScroll.h"
#import "apple_laf_JRSUIConstants_Orientation.h"
#import "apple_laf_JRSUIConstants_ScrollBarPart.h"
#import "apple_laf_JRSUIConstants_SegmentPosition.h"
#import "apple_laf_JRSUIConstants_SegmentTrailingSeparator.h"
#import "apple_laf_JRSUIConstants_SegmentLeadingSeparator.h"
#import "apple_laf_JRSUIConstants_ShowArrows.h"
#import "apple_laf_JRSUIConstants_Size.h"
#import "apple_laf_JRSUIConstants_State.h"
#import "apple_laf_JRSUIConstants_Variant.h"
#import "apple_laf_JRSUIConstants_Widget.h"
#import "apple_laf_JRSUIConstants_WindowType.h"
#import "apple_laf_JRSUIConstants_WindowTitleBarSeparator.h"
#import "apple_laf_JRSUIConstants_WindowClipCorners.h"

#import "JRSUIConstantSync.h"


static CFTypeRef widgetKey = NULL;
static CFTypeRef stateKey = NULL;
static CFTypeRef sizeKey = NULL;
static CFTypeRef directionKey = NULL;
static CFTypeRef orientationKey = NULL;
static CFTypeRef verticalAlignmentKey = NULL;
static CFTypeRef horizontalAlignmentKey = NULL;
static CFTypeRef positionKey = NULL;
static CFTypeRef pressedPartKey = NULL;
static CFTypeRef variantKey = NULL;
static CFTypeRef windowTypeKey = NULL;
static CFTypeRef focusedKey = NULL;
static CFTypeRef indicatorOnlyKey = NULL;
static CFTypeRef noIndicatorKey = NULL;
static CFTypeRef nothingToScrollKey = NULL;
static CFTypeRef arrowsOnlyKey = NULL;
static CFTypeRef frameOnlyKey = NULL;
static CFTypeRef segmentTrailingSeparatorKey = NULL;
static CFTypeRef segmentLeadingSeparatorKey = NULL;
static CFTypeRef windowFrameDrawClippedKey = NULL;
static CFTypeRef windowFrameDrawTitleSeparatorKey = NULL;
static CFTypeRef maximumValueKey = NULL;
static CFTypeRef valueKey = NULL;
static CFTypeRef animationStartTimeKey = NULL;
static CFTypeRef animationTimeKey = NULL;


#define JRS_CONSTANT(clazz, constant)                                \
    kJRSUI_ ## clazz ## _ ## constant

#define JNI_CONSTANT(clazz, constant)                                \
    apple_laf_JRSUIConstants_ ## clazz ## __ ## constant

#define CONSTANT_CHECK(clazz, constant)                                \
    ( JRS_CONSTANT(clazz, constant) == JNI_CONSTANT(clazz, constant) )

#define CONSISTENCY_CHECK(clazz, constant)                            \
    if ( !CONSTANT_CHECK(clazz, constant) ) return NO;

#define ASSIGN_KEY(key)                                                \
    key ## Key = JRSUIGetKey(JRS_CONSTANT(Key, key));                \
    if (key ## Key == NULL) return NO;

#define ASSIGN_KEY_IF_EXISTS(key, constant)                          \
    key ## Key = JRSUIGetKey(constant);

static BOOL init_and_check_constant_coherency() {
    ASSIGN_KEY(widget);
    ASSIGN_KEY(state);
    ASSIGN_KEY(size);
    ASSIGN_KEY(direction);
    ASSIGN_KEY(orientation);
    ASSIGN_KEY(verticalAlignment);
    ASSIGN_KEY(horizontalAlignment);
    ASSIGN_KEY(position);
    ASSIGN_KEY(pressedPart);
    ASSIGN_KEY(variant);
    ASSIGN_KEY(windowType);
    ASSIGN_KEY(focused);
    ASSIGN_KEY(indicatorOnly);
    ASSIGN_KEY(noIndicator);
    ASSIGN_KEY(nothingToScroll);
    ASSIGN_KEY(arrowsOnly);
    ASSIGN_KEY(frameOnly);
    ASSIGN_KEY(segmentTrailingSeparator);
    ASSIGN_KEY_IF_EXISTS(segmentLeadingSeparator, 29); // kJRSUI_Key_segmentLeadingSeparator = 29
    ASSIGN_KEY(windowFrameDrawClipped);
    ASSIGN_KEY(windowFrameDrawTitleSeparator);
    ASSIGN_KEY(maximumValue);
    ASSIGN_KEY(value);
    ASSIGN_KEY(animationStartTime);
    ASSIGN_KEY(animationTime);

    CONSISTENCY_CHECK(Key, value);
    CONSISTENCY_CHECK(Key, thumbProportion);
    CONSISTENCY_CHECK(Key, thumbStart);
    CONSISTENCY_CHECK(Key, animationFrame);
    CONSISTENCY_CHECK(Key, windowTitleBarHeight);

    CONSISTENCY_CHECK(Widget, background);
    CONSISTENCY_CHECK(Widget, buttonBevel);
    CONSISTENCY_CHECK(Widget, buttonBevelInset);
    CONSISTENCY_CHECK(Widget, buttonBevelRound);
    CONSISTENCY_CHECK(Widget, buttonCheckBox);
    CONSISTENCY_CHECK(Widget, buttonComboBox);
    CONSISTENCY_CHECK(Widget, buttonComboBoxInset);
    CONSISTENCY_CHECK(Widget, buttonDisclosure);
    CONSISTENCY_CHECK(Widget, buttonListHeader);
    CONSISTENCY_CHECK(Widget, buttonLittleArrows);
    CONSISTENCY_CHECK(Widget, buttonPopDown);
    CONSISTENCY_CHECK(Widget, buttonPopDownInset);
    CONSISTENCY_CHECK(Widget, buttonPopDownSquare);
    CONSISTENCY_CHECK(Widget, buttonPopUp);
    CONSISTENCY_CHECK(Widget, buttonPopUpInset);
    CONSISTENCY_CHECK(Widget, buttonPopUpSquare);
    CONSISTENCY_CHECK(Widget, buttonPush);
    CONSISTENCY_CHECK(Widget, buttonPushScope);
    CONSISTENCY_CHECK(Widget, buttonPushScope2);
    CONSISTENCY_CHECK(Widget, buttonPushTextured);
    CONSISTENCY_CHECK(Widget, buttonPushInset);
    CONSISTENCY_CHECK(Widget, buttonPushInset2);
    CONSISTENCY_CHECK(Widget, buttonRadio);
    CONSISTENCY_CHECK(Widget, buttonRound);
    CONSISTENCY_CHECK(Widget, buttonRoundHelp);
    CONSISTENCY_CHECK(Widget, buttonRoundInset);
    CONSISTENCY_CHECK(Widget, buttonRoundInset2);
    CONSISTENCY_CHECK(Widget, buttonSearchFieldCancel);
    CONSISTENCY_CHECK(Widget, buttonSearchFieldFind);
    CONSISTENCY_CHECK(Widget, buttonSegmented);
    CONSISTENCY_CHECK(Widget, buttonSegmentedInset);
    CONSISTENCY_CHECK(Widget, buttonSegmentedInset2);
    CONSISTENCY_CHECK(Widget, buttonSegmentedSCurve);
    CONSISTENCY_CHECK(Widget, buttonSegmentedTextured);
    CONSISTENCY_CHECK(Widget, buttonSegmentedToolbar);
    CONSISTENCY_CHECK(Widget, dial);
    CONSISTENCY_CHECK(Widget, disclosureTriangle);
    CONSISTENCY_CHECK(Widget, dividerGrabber);
    CONSISTENCY_CHECK(Widget, dividerSeparatorBar);
    CONSISTENCY_CHECK(Widget, dividerSplitter);
    CONSISTENCY_CHECK(Widget, focus);
    CONSISTENCY_CHECK(Widget, frameGroupBox);
    CONSISTENCY_CHECK(Widget, frameGroupBoxSecondary);
    CONSISTENCY_CHECK(Widget, frameListBox);
    CONSISTENCY_CHECK(Widget, framePlacard);
    CONSISTENCY_CHECK(Widget, frameTextField);
    CONSISTENCY_CHECK(Widget, frameTextFieldRound);
    CONSISTENCY_CHECK(Widget, frameWell);
    CONSISTENCY_CHECK(Widget, growBox);
    CONSISTENCY_CHECK(Widget, growBoxTextured);
    CONSISTENCY_CHECK(Widget, gradient);
    CONSISTENCY_CHECK(Widget, menu);
    CONSISTENCY_CHECK(Widget, menuItem);
    CONSISTENCY_CHECK(Widget, menuBar);
    CONSISTENCY_CHECK(Widget, menuTitle);
    CONSISTENCY_CHECK(Widget, progressBar);
    CONSISTENCY_CHECK(Widget, progressIndeterminateBar);
    CONSISTENCY_CHECK(Widget, progressRelevance);
    CONSISTENCY_CHECK(Widget, progressSpinner);
    CONSISTENCY_CHECK(Widget, scrollBar);
    CONSISTENCY_CHECK(Widget, scrollColumnSizer);
    CONSISTENCY_CHECK(Widget, slider);
    CONSISTENCY_CHECK(Widget, sliderThumb);
    CONSISTENCY_CHECK(Widget, synchronization);
    CONSISTENCY_CHECK(Widget, tab);
    CONSISTENCY_CHECK(Widget, titleBarCloseBox);
    CONSISTENCY_CHECK(Widget, titleBarCollapseBox);
    CONSISTENCY_CHECK(Widget, titleBarZoomBox);
    CONSISTENCY_CHECK(Widget, titleBarToolbarButton);
    CONSISTENCY_CHECK(Widget, toolbarItemWell);
    CONSISTENCY_CHECK(Widget, windowFrame);

    CONSISTENCY_CHECK(State, active);
    CONSISTENCY_CHECK(State, inactive);
    CONSISTENCY_CHECK(State, disabled);
    CONSISTENCY_CHECK(State, pressed);
    CONSISTENCY_CHECK(State, pulsed);
    CONSISTENCY_CHECK(State, rollover);
    CONSISTENCY_CHECK(State, drag);

    CONSISTENCY_CHECK(Size, mini);
    CONSISTENCY_CHECK(Size, small);
    CONSISTENCY_CHECK(Size, regular);
    CONSISTENCY_CHECK(Size, large);

    CONSISTENCY_CHECK(Direction, none);
    CONSISTENCY_CHECK(Direction, up);
    CONSISTENCY_CHECK(Direction, down);
    CONSISTENCY_CHECK(Direction, left);
    CONSISTENCY_CHECK(Direction, right);
    CONSISTENCY_CHECK(Direction, north);
    CONSISTENCY_CHECK(Direction, south);
    CONSISTENCY_CHECK(Direction, east);
    CONSISTENCY_CHECK(Direction, west);

    CONSISTENCY_CHECK(Orientation, horizontal);
    CONSISTENCY_CHECK(Orientation, vertical);

    CONSISTENCY_CHECK(AlignmentHorizontal, left);
    CONSISTENCY_CHECK(AlignmentHorizontal, center);
    CONSISTENCY_CHECK(AlignmentHorizontal, right);

    CONSISTENCY_CHECK(AlignmentVertical, top);
    CONSISTENCY_CHECK(AlignmentVertical, center);
    CONSISTENCY_CHECK(AlignmentVertical, bottom);

    CONSISTENCY_CHECK(SegmentPosition, first);
    CONSISTENCY_CHECK(SegmentPosition, middle);
    CONSISTENCY_CHECK(SegmentPosition, last);
    CONSISTENCY_CHECK(SegmentPosition, only);

    CONSISTENCY_CHECK(ScrollBarPart, none);
    CONSISTENCY_CHECK(ScrollBarPart, thumb);
    CONSISTENCY_CHECK(ScrollBarPart, arrowMin);
    CONSISTENCY_CHECK(ScrollBarPart, arrowMax);
    CONSISTENCY_CHECK(ScrollBarPart, arrowMaxInside);
    CONSISTENCY_CHECK(ScrollBarPart, arrowMinInside);
    CONSISTENCY_CHECK(ScrollBarPart, trackMin);
    CONSISTENCY_CHECK(ScrollBarPart, trackMax);

    CONSISTENCY_CHECK(Variant, menuGlyph);
    CONSISTENCY_CHECK(Variant, menuPopup);
    CONSISTENCY_CHECK(Variant, menuPulldown);
    CONSISTENCY_CHECK(Variant, menuHierarchical);
    CONSISTENCY_CHECK(Variant, gradientListBackgroundEven);
    CONSISTENCY_CHECK(Variant, gradientListBackgroundOdd);
    CONSISTENCY_CHECK(Variant, gradientSideBar);
    CONSISTENCY_CHECK(Variant, gradientSideBarSelection);
    CONSISTENCY_CHECK(Variant, gradientSideBarFocusedSelection);

    CONSISTENCY_CHECK(WindowType, document);
    CONSISTENCY_CHECK(WindowType, utility);
    CONSISTENCY_CHECK(WindowType, titlelessUtility);

    return YES;
}

static CFBooleanRef get_boolean_value_for(jbyte value) {
    return (value != 0) ? kCFBooleanTrue : kCFBooleanFalse;
}

static CFNumberRef get_boolean_number_value_for(jbyte value) {
    static CFNumberRef zero = NULL;
    static CFNumberRef one = NULL;

    if (!zero) {
        double zeroVal = 0.0;
        zero = CFNumberCreate(NULL, kCFNumberDoubleType, &zeroVal);
        double oneVal = 1.0;
        one = CFNumberCreate(NULL, kCFNumberDoubleType, &oneVal);
    }

    return (value != 0) ? one : zero;
}

BOOL _InitializeJRSProperties() {
    static BOOL initialized = NO;
    static BOOL coherent = NO;

    if (!initialized) {
        coherent = init_and_check_constant_coherency();
        initialized = YES;
    }

    return coherent;
}

#define MASK(property) \
    apple_laf_JRSUIConstants_ ## property ## _MASK

#define SHIFT(property) \
    apple_laf_JRSUIConstants_ ## property ## _SHIFT

#define IF_CHANGED_SET_USING(property, setter)                        \
{                                                                    \
    jlong value = (newProperties & MASK(property));                    \
    if ((value - (oldProperties & MASK(property))) != 0L) {            \
        setter(control, value >> SHIFT(property));                    \
    }                                                                \
}

#define IF_CHANGED_SET_KEYED_BOOLEAN(property, key, getter)            \
{                                                                    \
    jlong value = (newProperties & MASK(property));                    \
    if ((value - (oldProperties & MASK(property))) != 0L) {            \
        CFTypeRef cfValue = getter(value >> SHIFT(property));        \
        if (cfValue) {                                                \
            JRSUIControlSetValueByKey(control, key, cfValue);        \
        }                                                            \
    }                                                                \
}

#define IF_KEY_EXISTS_DO(key, operation)                             \
{                                                                    \
    if (key != NULL) {                                               \
        operation;                                                   \
    }                                                                \
}

jint _SyncEncodedProperties(JRSUIControlRef control, jlong oldProperties, jlong newProperties) {
    if (!_InitializeJRSProperties()) abort();

    IF_CHANGED_SET_USING(Widget, JRSUIControlSetWidget);
    IF_CHANGED_SET_USING(State, JRSUIControlSetState);
    IF_CHANGED_SET_USING(Size, JRSUIControlSetSize);
    IF_CHANGED_SET_USING(Direction, JRSUIControlSetDirection);
    IF_CHANGED_SET_USING(Orientation, JRSUIControlSetOrientation);
    IF_CHANGED_SET_USING(AlignmentVertical, JRSUIControlSetAlignmentVertical);
    IF_CHANGED_SET_USING(AlignmentHorizontal, JRSUIControlSetAlignmentHorizontal);
    IF_CHANGED_SET_USING(SegmentPosition, JRSUIControlSetSegmentPosition);
    IF_CHANGED_SET_USING(ScrollBarPart, JRSUIControlSetScrollBarPart);
    IF_CHANGED_SET_USING(Variant, JRSUIControlSetVariant);
    IF_CHANGED_SET_USING(WindowType, JRSUIControlSetWindowType);
    IF_CHANGED_SET_USING(ShowArrows, JRSUIControlSetShowArrows);

    IF_CHANGED_SET_KEYED_BOOLEAN(Focused, focusedKey, get_boolean_value_for);
    IF_CHANGED_SET_KEYED_BOOLEAN(IndicatorOnly, indicatorOnlyKey, get_boolean_value_for);
    IF_CHANGED_SET_KEYED_BOOLEAN(NoIndicator, noIndicatorKey, get_boolean_value_for);
    IF_CHANGED_SET_KEYED_BOOLEAN(ArrowsOnly, arrowsOnlyKey, get_boolean_value_for);
    IF_CHANGED_SET_KEYED_BOOLEAN(FrameOnly, frameOnlyKey, get_boolean_value_for);
    IF_CHANGED_SET_KEYED_BOOLEAN(SegmentTrailingSeparator, segmentTrailingSeparatorKey, get_boolean_value_for);
    IF_KEY_EXISTS_DO(segmentLeadingSeparatorKey, IF_CHANGED_SET_KEYED_BOOLEAN(SegmentLeadingSeparator, segmentLeadingSeparatorKey, get_boolean_value_for));
    IF_CHANGED_SET_KEYED_BOOLEAN(NothingToScroll, nothingToScrollKey, get_boolean_value_for);
    IF_CHANGED_SET_KEYED_BOOLEAN(WindowTitleBarSeparator, windowFrameDrawTitleSeparatorKey, get_boolean_value_for);
    IF_CHANGED_SET_KEYED_BOOLEAN(WindowClipCorners, windowFrameDrawClippedKey, get_boolean_value_for);
    IF_CHANGED_SET_KEYED_BOOLEAN(BooleanValue, valueKey, get_boolean_number_value_for);

    { // animation is special: keep setting while true
        jlong value = (newProperties & MASK(Animating));
        Boolean animating = value != 0L;
        Boolean changed = ((oldProperties & MASK(Animating)) - value) != 0L;
        if (animating || changed) {
            JRSUIControlSetAnimating(control, animating);
        }
    }

    return 0;
}


/*
 * Class:     apple_laf_JRSUIConstants
 * Method:    getPtrForConstant
 * Signature: (I)J
 */
JNIEXPORT jlong JNICALL Java_apple_laf_JRSUIConstants_getPtrForConstant
(JNIEnv *env, jclass clazz, jint constant){
    return ptr_to_jlong(JRSUIGetKey(constant));
}
