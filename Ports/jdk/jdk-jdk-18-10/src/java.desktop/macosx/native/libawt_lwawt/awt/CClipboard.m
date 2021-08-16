/*
 * Copyright (c) 2011, 2014, Oracle and/or its affiliates. All rights reserved.
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

#import "CDataTransferer.h"
#import "ThreadUtilities.h"
#import "JNIUtilities.h"
#import <Cocoa/Cocoa.h>

@interface CClipboard : NSObject { }
@property NSInteger changeCount;
@property jobject clipboardOwner;

+ (CClipboard*)sharedClipboard;
- (void)declareTypes:(NSArray *)types withOwner:(jobject)owner jniEnv:(JNIEnv*)env;
- (void)checkPasteboard:(id)sender;
@end

@implementation CClipboard
@synthesize changeCount = _changeCount;
@synthesize clipboardOwner = _clipboardOwner;

// Clipboard creation is synchronized at the Java level
+ (CClipboard*)sharedClipboard {
    static CClipboard* sClipboard = nil;
    if (sClipboard == nil) {
        sClipboard = [[CClipboard alloc] init];
        [[NSNotificationCenter defaultCenter] addObserver:sClipboard selector: @selector(checkPasteboard:)
                                                     name: NSApplicationDidBecomeActiveNotification
                                                   object: nil];
    }

    return sClipboard;
}

- (id)init {
    if (self = [super init]) {
        self.changeCount = [[NSPasteboard generalPasteboard] changeCount];
    }
    return self;
}

- (void)declareTypes:(NSArray*)types withOwner:(jobject)owner jniEnv:(JNIEnv*)env {
    @synchronized(self) {
        if (owner != NULL) {
            if (self.clipboardOwner != NULL) {
                (*env)->DeleteGlobalRef(env, self.clipboardOwner);
            }
            self.clipboardOwner = (*env)->NewGlobalRef(env, owner);
        }
    }
    [ThreadUtilities performOnMainThreadWaiting:YES block:^() {
        self.changeCount = [[NSPasteboard generalPasteboard] declareTypes:types owner:self];
    }];
}

- (void)checkPasteboard:(id)sender {

    // This is called via NSApplicationDidBecomeActiveNotification.

    // If the change count on the general pasteboard is different than when we set it
    // someone else put data on the clipboard.  That means the current owner lost ownership.

    NSInteger newChangeCount = [[NSPasteboard generalPasteboard] changeCount];

    if (self.changeCount != newChangeCount) {
        self.changeCount = newChangeCount;

        JNIEnv *env = [ThreadUtilities getJNIEnv];
        // Notify that the content might be changed
        DECLARE_CLASS(jc_CClipboard, "sun/lwawt/macosx/CClipboard");
        DECLARE_STATIC_METHOD(jm_contentChanged, jc_CClipboard, "notifyChanged", "()V");
        (*env)->CallStaticVoidMethod(env, jc_CClipboard, jm_contentChanged);
        CHECK_EXCEPTION();

        // If we have a Java pasteboard owner, tell it that it doesn't own the pasteboard anymore.
        DECLARE_METHOD(jm_lostOwnership, jc_CClipboard, "notifyLostOwnership", "()V");
        @synchronized(self) {
            if (self.clipboardOwner) {
                (*env)->CallVoidMethod(env, self.clipboardOwner, jm_lostOwnership);
                CHECK_EXCEPTION();
                (*env)->DeleteGlobalRef(env, self.clipboardOwner);
                self.clipboardOwner = NULL;
            }
        }
    }
}

- (BOOL) checkPasteboardWithoutNotification:(id)application {
    AWT_ASSERT_APPKIT_THREAD;

    NSInteger newChangeCount = [[NSPasteboard generalPasteboard] changeCount];

    if (self.changeCount != newChangeCount) {
        self.changeCount = newChangeCount;
        return YES;
    } else {
        return NO;
    }
}

@end

/*
 * Class:     sun_lwawt_macosx_CClipboard
 * Method:    declareTypes
 * Signature: ([JLsun/awt/datatransfer/SunClipboard;)V
*/
JNIEXPORT void JNICALL Java_sun_lwawt_macosx_CClipboard_declareTypes
(JNIEnv *env, jobject inObject, jlongArray inTypes, jobject inJavaClip)
{
JNI_COCOA_ENTER(env);

    jint i;
    jint nElements = (*env)->GetArrayLength(env, inTypes);
    NSMutableArray *formatArray = [NSMutableArray arrayWithCapacity:nElements];
    jlong *elements = (*env)->GetPrimitiveArrayCritical(env, inTypes, NULL);

    for (i = 0; i < nElements; i++) {
        NSString *pbFormat = formatForIndex(elements[i]);
        if (pbFormat)
            [formatArray addObject:pbFormat];
    }

    (*env)->ReleasePrimitiveArrayCritical(env, inTypes, elements, JNI_ABORT);
    [[CClipboard sharedClipboard] declareTypes:formatArray withOwner:inJavaClip jniEnv:env];
JNI_COCOA_EXIT(env);
}

/*
 * Class:     sun_lwawt_macosx_CClipboard
 * Method:    setData
 * Signature: ([BJ)V
*/
JNIEXPORT void JNICALL Java_sun_lwawt_macosx_CClipboard_setData
(JNIEnv *env, jobject inObject, jbyteArray inBytes, jlong inFormat)
{
    if (inBytes == NULL) {
        return;
    }

JNI_COCOA_ENTER(env);
    jint nBytes = (*env)->GetArrayLength(env, inBytes);
    jbyte *rawBytes = (*env)->GetPrimitiveArrayCritical(env, inBytes, NULL);
    CHECK_NULL(rawBytes);
    NSData *bytesAsData = [NSData dataWithBytes:rawBytes length:nBytes];
    (*env)->ReleasePrimitiveArrayCritical(env, inBytes, rawBytes, JNI_ABORT);
    NSString *format = formatForIndex(inFormat);
    [ThreadUtilities performOnMainThreadWaiting:YES block:^() {
        [[NSPasteboard generalPasteboard] setData:bytesAsData forType:format];
    }];
JNI_COCOA_EXIT(env);
}

/*
 * Class:     sun_lwawt_macosx_CClipboard
 * Method:    getClipboardFormats
 * Signature: (J)[J
     */
JNIEXPORT jlongArray JNICALL Java_sun_lwawt_macosx_CClipboard_getClipboardFormats
(JNIEnv *env, jobject inObject)
{
    jlongArray returnValue = NULL;
JNI_COCOA_ENTER(env);

    __block NSArray* dataTypes;
    [ThreadUtilities performOnMainThreadWaiting:YES block:^() {
        dataTypes = [[[NSPasteboard generalPasteboard] types] retain];
    }];
    [dataTypes autorelease];

    NSUInteger nFormats = [dataTypes count];
    NSUInteger knownFormats = 0;
    NSUInteger i;

    // There can be any number of formats on the general pasteboard.  Find out which ones
    // we know about (i.e., live in the flavormap.properties).
    for (i = 0; i < nFormats; i++) {
        NSString *format = (NSString *)[dataTypes objectAtIndex:i];
        if (indexForFormat(format) != -1)
            knownFormats++;
    }

    returnValue = (*env)->NewLongArray(env, knownFormats);
    if (returnValue == NULL) {
        return NULL;
    }

    if (knownFormats == 0) {
        return returnValue;
    }

    // Now go back and map the formats we found back to Java indexes.
    jboolean isCopy;
    jlong *lFormats = (*env)->GetLongArrayElements(env, returnValue, &isCopy);
    jlong *saveFormats = lFormats;

    for (i = 0; i < nFormats; i++) {
        NSString *format = (NSString *)[dataTypes objectAtIndex:i];
        jlong index = indexForFormat(format);

        if (index != -1) {
            *lFormats = index;
            lFormats++;
        }
    }

    (*env)->ReleaseLongArrayElements(env, returnValue, saveFormats, JNI_COMMIT);
JNI_COCOA_EXIT(env);
    return returnValue;
}

/*
 * Class:     sun_lwawt_macosx_CClipboard
 * Method:    getClipboardData
 * Signature: (JJ)[B
     */
JNIEXPORT jbyteArray JNICALL Java_sun_lwawt_macosx_CClipboard_getClipboardData
(JNIEnv *env, jobject inObject, jlong format)
{
    jbyteArray returnValue = NULL;

    // Note that this routine makes no attempt to interpret the data, since we're returning
    // a byte array back to Java.  CDataTransferer will do that if necessary.
JNI_COCOA_ENTER(env);

    NSString *formatAsString = formatForIndex(format);
    __block NSData* clipData;
    [ThreadUtilities performOnMainThreadWaiting:YES block:^() {
        clipData = [[[NSPasteboard generalPasteboard] dataForType:formatAsString] retain];
    }];

    if (clipData == NULL) {
        JNU_ThrowIOException(env, "Font transform has NaN position");
        return NULL;
    } else {
        [clipData autorelease];
    }

    NSUInteger dataSize = [clipData length];
    returnValue = (*env)->NewByteArray(env, dataSize);
    if (returnValue == NULL) {
        return NULL;
    }

    if (dataSize != 0) {
        const void *dataBuffer = [clipData bytes];
        (*env)->SetByteArrayRegion(env, returnValue, 0, dataSize, (jbyte *)dataBuffer);
    }

JNI_COCOA_EXIT(env);
    return returnValue;
}

/*
 * Class:     sun_lwawt_macosx_CClipboard
 * Method:    checkPasteboard
 * Signature: ()V
 */
JNIEXPORT jboolean JNICALL Java_sun_lwawt_macosx_CClipboard_checkPasteboardWithoutNotification
(JNIEnv *env, jobject inObject)
{
    __block BOOL ret = NO;
    JNI_COCOA_ENTER(env);
    [ThreadUtilities performOnMainThreadWaiting:YES block:^(){
        ret = [[CClipboard sharedClipboard] checkPasteboardWithoutNotification:nil];
    }];

    JNI_COCOA_EXIT(env);
    return ret;
}
