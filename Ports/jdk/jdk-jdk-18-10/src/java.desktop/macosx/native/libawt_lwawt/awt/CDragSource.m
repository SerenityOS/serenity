/*
 * Copyright (c) 2011, 2017, Oracle and/or its affiliates. All rights reserved.
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

//#define DND_DEBUG TRUE

#import "java_awt_dnd_DnDConstants.h"

#import <Cocoa/Cocoa.h>

#import "AWTEvent.h"
#import "AWTView.h"
#import "CDataTransferer.h"
#import "CDropTarget.h"
#import "CDragSource.h"
#import "DnDUtilities.h"
#import "ThreadUtilities.h"
#import "LWCToolkit.h"
#import "JNIUtilities.h"


// When sIsJavaDragging is true Java drag gesture has been recognized and a drag is/has been initialized.
// We must stop posting MouseEvent.MOUSE_DRAGGED events for the duration of the drag or all hell will break
// loose in shared code - tracking state going haywire.
static BOOL sIsJavaDragging;


@interface NSEvent(AWTAdditions)

+ (void)javaDraggingBegin;
+ (void)javaDraggingEnd;

@end


@implementation NSEvent(AWTAdditions)


+ (void)javaDraggingBegin
{
    sIsJavaDragging = YES;
}

+ (void)javaDraggingEnd
{
    // sIsJavaDragging is reset on mouseDown as well.
    sIsJavaDragging = NO;
}
@end

static jclass DataTransfererClass = NULL;
static jclass CDragSourceContextPeerClass =  NULL;

#define GET_DT_CLASS() \
    GET_CLASS(DataTransfererClass, "sun/awt/datatransfer/DataTransferer");

#define GET_DT_CLASS_RETURN(ret) \
    GET_CLASS_RETURN(DataTransfererClass, "sun/awt/datatransfer/DataTransferer", ret);

#define GET_DSCP_CLASS() \
    GET_CLASS(CDragSourceContextPeerClass, "sun/lwawt/macosx/CDragSourceContextPeer");

static NSDragOperation    sDragOperation;
static NSPoint            sDraggingLocation;

static BOOL                sNeedsEnter;

@interface CDragSource ()
// Updates from the destination to the source
- (void) postDragEnter;
- (void) postDragExit;
// Utility
- (NSPoint) mapNSScreenPointToJavaWithOffset:(NSPoint) point;
@end

@implementation CDragSource

- (id)        init:(jobject)jDragSourceContextPeer
         component:(jobject)jComponent
           control:(id)control
      transferable:(jobject)jTransferable
      triggerEvent:(jobject)jTrigger
          dragPosX:(jint)dragPosX
          dragPosY:(jint)dragPosY
         modifiers:(jint)extModifiers
        clickCount:(jint)clickCount
         timeStamp:(jlong)timeStamp
         dragImage:(jlong)nsDragImagePtr
  dragImageOffsetX:(jint)jDragImageOffsetX
  dragImageOffsetY:(jint)jDragImageOffsetY
     sourceActions:(jint)jSourceActions
           formats:(jlongArray)jFormats
         formatMap:(jobject)jFormatMap
{
    self = [super init];
    DLog2(@"[CDragSource init]: %@\n", self);

    fView = nil;
    fComponent = nil;

    // Construct the object if we have a valid model for it:
    if (control != nil) {
        fComponent = jComponent;
        fDragSourceContextPeer = jDragSourceContextPeer;
        fTransferable = jTransferable;
        fTriggerEvent = jTrigger;

        if (nsDragImagePtr) {
            fDragImage = (NSImage*) jlong_to_ptr(nsDragImagePtr);
            [fDragImage retain];
        }

        fDragImageOffset = NSMakePoint(jDragImageOffsetX, jDragImageOffsetY);

        fSourceActions = jSourceActions;
        fFormats = jFormats;
        fFormatMap = jFormatMap;

        fTriggerEventTimeStamp = timeStamp;
        fDragPos = NSMakePoint(dragPosX, dragPosY);
        fClickCount = clickCount;
        fModifiers = extModifiers;

        // Set this object as a dragging source:

        fView = [(AWTView *) control retain];
        [fView setDragSource:self];

        // Let AWTEvent know Java drag is getting underway:
        [NSEvent javaDraggingBegin];
    }

    else {
        [self release];
        self = nil;
    }

    return self;
}

- (void)removeFromView:(JNIEnv *)env
{
    DLog2(@"[CDragSource removeFromView]: %@\n", self);

    // Remove this dragging source from the view:
    [((AWTView *) fView) setDragSource:nil];

    // Clean up JNI refs
    if (fComponent != NULL) {
        (*env)->DeleteGlobalRef(env, fComponent);
        fComponent = NULL;
    }

    if (fDragSourceContextPeer != NULL) {
        (*env)->DeleteGlobalRef(env, fDragSourceContextPeer);
        fDragSourceContextPeer = NULL;
    }

    if (fTransferable != NULL) {
        (*env)->DeleteGlobalRef(env, fTransferable);
        fTransferable = NULL;
    }

    if (fTriggerEvent != NULL) {
        (*env)->DeleteGlobalRef(env, fTriggerEvent);
        fTriggerEvent = NULL;
    }

    if (fFormats != NULL) {
        (*env)->DeleteGlobalRef(env, fFormats);
        fFormats = NULL;
    }

    if (fFormatMap != NULL) {
        (*env)->DeleteGlobalRef(env, fFormatMap);
        fFormatMap = NULL;
    }

    [self release];
}

- (void)dealloc
{
    DLog2(@"[CDragSource dealloc]: %@\n", self);

    // Delete or release local data:
    [fView release];
    fView = nil;

    [fDragImage release];
    fDragImage = nil;

    [super dealloc];
}

// Appropriated from Windows' awt_DataTransferer.cpp:
//
// * NOTE: This returns a JNI Local Ref. Any code that calls must call DeleteLocalRef with the return value.
//
- (jobject)dataTransferer:(JNIEnv*)env
{
    GET_DT_CLASS_RETURN(NULL);
    DECLARE_STATIC_METHOD_RETURN(getInstanceMethod, DataTransfererClass, "getInstance", "()Lsun/awt/datatransfer/DataTransferer;", NULL);
    jobject o = (*env)->CallStaticObjectMethod(env, DataTransfererClass, getInstanceMethod);
    CHECK_EXCEPTION();
    return o;
}

// Appropriated from Windows' awt_DataTransferer.cpp:
//
// * NOTE: This returns a JNI Local Ref. Any code that calls must call DeleteLocalRef with the return value.
//
- (jbyteArray)convertData:(jlong)format
{
    JNIEnv*    env = [ThreadUtilities getJNIEnv];
    jobject    transferer = [self dataTransferer:env];
    jbyteArray data = nil;

    if (transferer != NULL) {
        GET_DT_CLASS_RETURN(NULL);
        DECLARE_METHOD_RETURN(convertDataMethod, DataTransfererClass, "convertData", "(Ljava/lang/Object;Ljava/awt/datatransfer/Transferable;JLjava/util/Map;Z)[B", NULL);
        data = (*env)->CallObjectMethod(env, transferer, convertDataMethod, fComponent, fTransferable, format, fFormatMap, (jboolean) TRUE);
    }
    CHECK_EXCEPTION();

    return data;
}


// Encodes a byte array of zero-terminated filenames into an NSArray of NSStrings representing them.
// Borrowed and adapted from awt_DataTransferer.c, convertFileType().
- (id)getFileList:(jbyte *)jbytes dataLength:(jsize)jbytesLength
{
    jsize  strings = 0;
    jsize  i;

    // Get number of filenames while making sure to skip over empty strings.
    for (i = 1; i < jbytesLength; i++) {
        if (jbytes[i] == '\0' && jbytes[i - 1] != '\0')
            strings++;
    }

    // Create the file list to return:
    NSMutableArray* fileList = [NSMutableArray arrayWithCapacity:strings];

    for (i = 0; i < jbytesLength; i++) {
        char* start = (char *) &jbytes[i];

        // Skip over empty strings:
        if (start[0] == '\0') {
            continue;
        }

        // Update the position marker:
        i += strlen(start);

        // Add this filename to the file list:
        NSMutableString* fileName = [NSMutableString stringWithUTF8String:start];
        // Decompose the filename
        CFStringNormalize((CFMutableStringRef)fileName, kCFStringNormalizationFormD);
        [fileList addObject:fileName];
    }

    // 03-01-09 Note: keep this around for debugging.
    // return [NSArray arrayWithObjects:@"/tmp/foo1", @"/tmp/foo2", nil];

    return ([fileList count] > 0 ? fileList : nil);
}


// Set up the pasteboard for dragging:
- (BOOL)declareTypesToPasteboard:(NSPasteboard *)pb withEnv:(JNIEnv *) env {
    // 9-20-02 Note: leave this here for debugging:
    //[pb declareTypes: [NSArray arrayWithObject: NSStringPboardType] owner: self];
    //return TRUE;

    // Get byte array elements:
    jboolean isCopy;
    jlong* jformats = (*env)->GetLongArrayElements(env, fFormats, &isCopy);
    if (jformats == nil)
        return FALSE;

    // Allocate storage arrays for dragging types to register with the pasteboard:
    jsize formatsLength = (*env)->GetArrayLength(env, fFormats);
    NSMutableArray* pbTypes = [[NSMutableArray alloc] initWithCapacity:formatsLength];

    // And assume there are no NS-type data: [Radar 3065621]
    // This is to be able to drop transferables containing only a serialized object flavor, e.g.:
    //   "JAVA_DATAFLAVOR:application/x-java-serialized-object; class=java.awt.Label".
    BOOL hasNSTypeData = false;

    // Collect all supported types in a pasteboard format into the storage arrays:
    jsize i;
    for (i = 0; i < formatsLength; i++) {
        jlong jformat = jformats[i];

        if (jformat >= 0) {
            NSString* type = formatForIndex(jformat);

            // Add element type to the storage array.
            if (type != nil) {
                if ([type hasPrefix:@"JAVA_DATAFLAVOR:application/x-java-jvm-local-objectref;"] == false) {
                    [pbTypes addObject:type];

                    // This is a good approximation if not perfect. A conclusive search would
                    // have to be done matching all defined strings in AppKit's commonStrings.h.
                    hasNSTypeData = [type hasPrefix:@"NS"] || [type hasPrefix:@"NeXT"] || [type hasPrefix:@"public."];
                }
            }
        }
    }

    // 1-16-03 Note: [Radar 3065621]
    // When TransferHandler is used with Swing components it puts only a type like this on the pasteboard:
    //   "JAVA_DATAFLAVOR:application/x-java-jvm-local-objectref; class=java.lang.String"
    // And there's similar type for serialized object only transferables.
    // Since our drop targets aren't trained for arbitrary data types yet we need to fake an empty string
    // which will cause drop target handlers to fire.
    // KCH  - 3550405 If the drag is between Swing components, formatsLength == 0, so expand the check.
    // Also, use a custom format rather than NSString, since that will prevent random views from accepting the drag
    if (hasNSTypeData == false && formatsLength >= 0) {
        [pbTypes addObject:[DnDUtilities javaPboardType]];
    }

    (*env)->ReleaseLongArrayElements(env, fFormats, jformats, JNI_ABORT);

    // Declare pasteboard types. If the types array is empty we still want to declare them
    // as otherwise an old set of types/data would remain on the pasteboard.
    NSUInteger typesCount = [pbTypes count];
    [pb declareTypes:pbTypes owner: self];

    // KCH - Lame conversion bug between Cocoa and Carbon drag types
    // If I provide the filenames _right now_, NSFilenamesPboardType is properly converted to CoreDrag flavors
    // If I try to wait until pasteboard:provideDataForType:, the conversion won't happen
    // and pasteboard:provideDataForType: won't even get called! (unless I go over a Cocoa app)
    if ([pbTypes containsObject:NSFilenamesPboardType]) {
        [self pasteboard:pb provideDataForType:NSFilenamesPboardType];
    }

    [pbTypes release];

    return typesCount > 0 ? TRUE : FALSE;
}

// This is an NSPasteboard callback. In declareTypesToPasteboard:withEnv:, we only declared the types
// When the AppKit DnD system actually needs the data, this method will be invoked.
// Note that if the transfer is handled entirely from Swing (as in a local-vm drag), this method may never be called.
- (void)pasteboard:(NSPasteboard *)pb provideDataForType:(NSString *)type {
    AWT_ASSERT_APPKIT_THREAD;

    // 9-20-02 Note: leave this here for debugging:
    //[pb setString: @"Hello, World!" forType: NSStringPboardType];
    // return;

    // Set up Java environment:
    JNIEnv* env = [ThreadUtilities getJNIEnv];

    id pbData = nil;

    // Collect data in a pasteboard format:
    jlong jformat = indexForFormat(type);
    if (jformat >= 0) {
        // Convert DataTransfer data to a Java byte array:
        // Note that this will eventually call getTransferData()
        jbyteArray jdata = [self convertData:jformat];

        if (jdata != nil) {
            jboolean isCopy;
            jsize jdataLength = (*env)->GetArrayLength(env, jdata);
            jbyte* jbytedata = (*env)->GetByteArrayElements(env, jdata, &isCopy);

            if (jdataLength > 0 && jbytedata != nil) {
                // Get element data to the storage array. For NSFilenamesPboardType type we use
                // an NSArray-type data - NSData-type data would cause a crash.
                if (type != nil) {
                    pbData = ([type isEqualTo:NSFilenamesPboardType]) ?
                        [self getFileList:jbytedata dataLength:jdataLength] :
                        [NSData dataWithBytes:jbytedata length:jdataLength];
                }
            }

            (*env)->ReleaseByteArrayElements(env, jdata, jbytedata, JNI_ABORT);

            (*env)->DeleteLocalRef(env, jdata);
        }
    }

    // If we are the custom type that matches local-vm drags, set an empty NSData
    if ( (pbData == nil) && ([type isEqualTo:[DnDUtilities javaPboardType]]) ) {
        pbData = [NSData dataWithBytes:"" length:1];
    }

    // Add pasteboard data for the type:
    // Remember, NSFilenamesPboardType's data is NSArray (property list), not NSData!
    // We must use proper pb accessor depending on the data type.
    if ([pbData isKindOfClass:[NSArray class]])
        [pb setPropertyList:pbData forType:type];
    else
        [pb setData:pbData forType:type];
}


- (void)validateDragImage
{
    // Make a small blank image if we don't have a drag image:
    if (fDragImage == nil) {
        // 9-30-02 Note: keep this around for debugging:
        fDragImage = [[NSImage alloc] initWithSize:NSMakeSize(21, 21)];
        NSSize imageSize = [fDragImage size];

        NSBitmapImageRep *imageRep = [[NSBitmapImageRep alloc] initWithBitmapDataPlanes:NULL
            pixelsWide:imageSize.width pixelsHigh:imageSize.height bitsPerSample:8 samplesPerPixel:4
            hasAlpha:YES isPlanar:NO colorSpaceName:NSCalibratedRGBColorSpace bytesPerRow:0 bitsPerPixel:32];

        [fDragImage addRepresentation:imageRep];
        fDragImageOffset = NSMakePoint(0, 0);

        [imageRep release];
    }
}

- (NSEvent*)nsDragEvent:(BOOL)isDrag
{
    // Get NSView for the drag source:
    NSWindow* window = [fView window];

    NSInteger windowNumber = [window windowNumber];

    // Convert mouse coordinates to NS:
    NSPoint eventLocation = [fView convertPoint:NSMakePoint(fDragPos.x, fDragPos.y) toView:nil];
    eventLocation.y = [[fView window] frame].size.height - eventLocation.y;

    // Convert fTriggerEventTimeStamp to NS - AWTEvent.h defines UTC(nsEvent) as ((jlong)[event timestamp] * 1000):
    NSTimeInterval timeStamp = fTriggerEventTimeStamp / 1000;

    // Convert fModifiers (extModifiers) to NS:
    NSEventType mouseButtons = 0;
    float pressure = 0.0;
    if (isDrag) {
        mouseButtons = (NSEventType) [DnDUtilities mapJavaExtModifiersToNSMouseDownButtons:fModifiers];
        pressure = 1.0;
    } else {
        mouseButtons = (NSEventType) [DnDUtilities mapJavaExtModifiersToNSMouseUpButtons:fModifiers];
    }

    // Convert fModifiers (extModifiers) to NS:
    NSUInteger modifiers = JavaModifiersToNsKeyModifiers(fModifiers, TRUE);

    // Just a dummy value ...
    NSInteger eventNumber = 0;
    // NSEvent.context is deprecated and unused
    NSGraphicsContext* unusedPassNil = nil;

    // Make a native autoreleased dragging event:
    NSEvent* dragEvent = [NSEvent mouseEventWithType:mouseButtons location:eventLocation
        modifierFlags:modifiers timestamp:timeStamp windowNumber:windowNumber context:unusedPassNil
        eventNumber:eventNumber clickCount:fClickCount pressure:pressure];

    return dragEvent;
}

- (void)doDrag
{
    AWT_ASSERT_APPKIT_THREAD;

    DLog2(@"[CDragSource doDrag]: %@\n", self);

    // Set up Java environment:
    JNIEnv *env = [ThreadUtilities getJNIEnv];

    // Set up the pasteboard:
    NSPasteboard *pb = [NSPasteboard pasteboardWithName: NSDragPboard];
    [self declareTypesToPasteboard:pb withEnv:env];

    // Make a native autoreleased NS dragging event:
    NSEvent *dragEvent = [self nsDragEvent:YES];

    // Get NSView for the drag source:
    NSView *view = fView;

    // Make sure we have a valid drag image:
    [self validateDragImage];
    NSImage* dragImage = fDragImage;

    // Get drag origin and offset:
    NSPoint dragOrigin = [dragEvent locationInWindow];
    dragOrigin.x += fDragImageOffset.x;
    dragOrigin.y -= fDragImageOffset.y + [dragImage size].height;

    // Drag offset values don't seem to matter:
    NSSize dragOffset = NSMakeSize(0, 0);

    // These variables should be set based on the transferable:
    BOOL isFileDrag = FALSE;
    BOOL fileDragPromises = FALSE;

    DLog(@"[CDragSource drag]: calling dragImage/File:");
    DLog3(@"  - drag origin: %f, %f", fDragPos.x, fDragPos.y);
    DLog5(@"  - drag image: %f, %f (%f x %f)", fDragImageOffset.x, fDragImageOffset.y, [dragImage size].width, [dragImage size].height);
    DLog3(@"  - event point (window) %f, %f", [dragEvent locationInWindow].x, [dragEvent locationInWindow].y);
    DLog3(@"  - drag point (view) %f, %f", dragOrigin.x, dragOrigin.y);
    // Set up the fDragKeyModifier, so we know if the operation has changed
    // Set up the fDragMouseModifier, so we can |= it later (since CoreDrag doesn't tell us mouse states during a drag)
    fDragKeyModifiers = [DnDUtilities extractJavaExtKeyModifiersFromJavaExtModifiers:fModifiers];
    fDragMouseModifiers = [DnDUtilities extractJavaExtMouseModifiersFromJavaExtModifiers:fModifiers];

    @try {
        sNeedsEnter = YES;
        AWTToolkit.inDoDragDropLoop = YES;
        // Data dragging:
        if (isFileDrag == FALSE) {
            [view dragImage:dragImage at:dragOrigin offset:dragOffset event:dragEvent pasteboard:pb source:view slideBack:YES];
        } else if (fileDragPromises == FALSE) {
            // File dragging:
            NSLog(@"[CDragSource drag]: file dragging is unsupported.");
            NSString* fileName = nil;                                // This should be set based on the transferable.
            NSRect    fileLocationRect = NSMakeRect(0, 0, 0, 0);    // This should be set based on the filename.

            BOOL success = [view dragFile:fileName fromRect:fileLocationRect slideBack:YES event:dragEvent];
            if (success == TRUE) {                                    // One would erase dragged file if this was a move operation.
            }
        } else {
            // Promised file dragging:
            NSLog(@"[CDragSource drag]: file dragging promises are unsupported.");
            NSArray* fileTypesArray = nil;                            // This should be set based on the transferable.
            NSRect   fileLocationRect = NSMakeRect(0, 0, 0, 0);        // This should be set based on all filenames.

            BOOL success = [view dragPromisedFilesOfTypes:fileTypesArray fromRect:fileLocationRect source:view slideBack:YES event:dragEvent];
            if (success == TRUE) {                                    // One would write out the promised files here.
            }
        }

        NSPoint point = [self mapNSScreenPointToJavaWithOffset:sDraggingLocation];

        // Convert drag operation to Java:
        jint dragOp = [DnDUtilities mapNSDragOperationToJava:sDragOperation];

        // Drag success must acount for DragOperationNone:
        jboolean success = (dragOp != java_awt_dnd_DnDConstants_ACTION_NONE);

        // We have a problem here... we don't send DragSource dragEnter/Exit messages outside of our own process
        // because we don't get anything from AppKit/CoreDrag
        // This means that if you drag outside of the app and drop, even if it's valid, a dragDropFinished is posted without dragEnter
        // I'm worried that this might confuse Java, so we're going to send a "bogus" dragEnter if necessary (only if the drag succeeded)
        if (success && sNeedsEnter) {
            [self postDragEnter];
        }

        // DragSourceContextPeer.dragDropFinished() should be called even if there was an error:
        GET_DSCP_CLASS();
        DECLARE_METHOD(dragDropFinishedMethod, CDragSourceContextPeerClass, "dragDropFinished", "(ZIII)V");
        DLog3(@"  -> posting dragDropFinished, point %f, %f", point.x, point.y);
        (*env)->CallVoidMethod(env, fDragSourceContextPeer, dragDropFinishedMethod, success, dragOp, (jint) point.x, (jint) point.y);
        CHECK_EXCEPTION();
        DECLARE_METHOD(resetHoveringMethod, CDragSourceContextPeerClass, "resetHovering", "()V");
        (*env)->CallVoidMethod(env, fDragSourceContextPeer, resetHoveringMethod); // Hust reset static variable
        CHECK_EXCEPTION();
    } @finally {
        sNeedsEnter = NO;
        AWTToolkit.inDoDragDropLoop = NO;
    }

    // We have to do this, otherwise AppKit doesn't know we're finished dragging. Yup, it's that bad.
    if ([[[NSRunLoop currentRunLoop] currentMode] isEqualTo:NSEventTrackingRunLoopMode]) {
        [NSApp postEvent:[self nsDragEvent:NO] atStart:YES];
    }

    DLog2(@"[CDragSource doDrag] end: %@\n", self);
}

- (void)drag
{
    AWT_ASSERT_NOT_APPKIT_THREAD;

    [self performSelectorOnMainThread:@selector(doDrag) withObject:nil waitUntilDone:YES];
}

/********************************  BEGIN NSDraggingSource Interface  ********************************/

- (void)draggingOperationChanged:(NSDragOperation)dragOp {
    //DLog2(@"[CDragSource draggingOperationChanged]: %@\n", self);

    JNIEnv* env = [ThreadUtilities getJNIEnv];

    jint targetActions = fSourceActions;
    if ([CDropTarget currentDropTarget]) targetActions = [[CDropTarget currentDropTarget] currentJavaActions];

    NSPoint point = [self mapNSScreenPointToJavaWithOffset:sDraggingLocation];
    DLog3(@"  -> posting operationChanged, point %f, %f", point.x, point.y);
    jint modifiedModifiers = fDragKeyModifiers | fDragMouseModifiers | [DnDUtilities javaKeyModifiersForNSDragOperation:dragOp];

    GET_DSCP_CLASS();
    DECLARE_METHOD(operationChangedMethod, CDragSourceContextPeerClass, "operationChanged", "(IIII)V");
    (*env)->CallVoidMethod(env, fDragSourceContextPeer, operationChangedMethod, targetActions, modifiedModifiers, (jint) point.x, (jint) point.y);
    CHECK_EXCEPTION();
}

- (NSDragOperation)draggingSourceOperationMaskForLocal:(BOOL)localDrag {
    //DLog2(@"[CDragSource draggingSourceOperationMaskForLocal]: %@\n", self);
    return [DnDUtilities mapJavaDragOperationToNS:fSourceActions];
}

/* 9-16-02 Note: we don't support promises yet.
- (NSArray *)namesOfPromisedFilesDroppedAtDestination:(NSURL *)dropDestination {
}*/

- (void)draggedImage:(NSImage *)image beganAt:(NSPoint)screenPoint {
    DLog4(@"[CDragSource draggedImage beganAt]: (%f, %f) %@\n", screenPoint.x, screenPoint.y, self);
    [AWTToolkit eventCountPlusPlus];
    // Initialize static variables:
    sDragOperation = NSDragOperationNone;
    sDraggingLocation = screenPoint;
}

- (void)draggedImage:(NSImage *)image endedAt:(NSPoint)screenPoint operation:(NSDragOperation)operation {
    DLog4(@"[CDragSource draggedImage endedAt:]: (%f, %f) %@\n", screenPoint.x, screenPoint.y, self);
    [AWTToolkit eventCountPlusPlus];
    sDraggingLocation = screenPoint;
    sDragOperation = operation;
}

- (void)draggedImage:(NSImage *)image movedTo:(NSPoint)screenPoint {
    //DLog4(@"[CDragSource draggedImage moved]: (%d, %d) %@\n", (int) screenPoint.x, (int) screenPoint.y, self);
    JNIEnv* env = [ThreadUtilities getJNIEnv];

JNI_COCOA_ENTER(env);
    [AWTToolkit eventCountPlusPlus];
    // There are two things we would be interested in:
    // a) mouse pointer has moved
    // b) drag actions (key modifiers) have changed

    BOOL notifyJava = FALSE;

    // a) mouse pointer has moved:
    if (NSEqualPoints(screenPoint, sDraggingLocation) == FALSE) {
        //DLog2(@"[CDragSource draggedImage:movedTo]: mouse moved, %@\n", self);
        notifyJava = TRUE;
    }

    // b) drag actions (key modifiers) have changed:
    jint modifiers = NsKeyModifiersToJavaModifiers([NSEvent modifierFlags], YES);
    if (fDragKeyModifiers != modifiers) {
        NSDragOperation currentOp = [DnDUtilities nsDragOperationForModifiers:[NSEvent modifierFlags]];
        NSDragOperation allowedOp = [DnDUtilities mapJavaDragOperationToNS:fSourceActions] & currentOp;

        fDragKeyModifiers = modifiers;

        if (sDragOperation != allowedOp) {
            sDragOperation = allowedOp;
            [self draggingOperationChanged:allowedOp];
        }
    }

    // Should we notify Java things have changed?
    if (notifyJava) {
        sDraggingLocation = screenPoint;

        NSPoint point = [self mapNSScreenPointToJavaWithOffset:screenPoint];

        jint targetActions = fSourceActions;
        if ([CDropTarget currentDropTarget]) targetActions = [[CDropTarget currentDropTarget] currentJavaActions];

        // Motion: dragMotion, dragMouseMoved
        DLog4(@"[CDragSource draggedImage moved]: (%f, %f) %@\n", screenPoint.x, screenPoint.y, self);

        DLog3(@"  -> posting dragMotion, point %f, %f", point.x, point.y);
        GET_DSCP_CLASS();
        DECLARE_METHOD(dragMotionMethod, CDragSourceContextPeerClass, "dragMotion", "(IIII)V");
        (*env)->CallVoidMethod(env, fDragSourceContextPeer, dragMotionMethod, targetActions, (jint) fModifiers, (jint) point.x, (jint) point.y);
        CHECK_EXCEPTION();
        DLog3(@"  -> posting dragMouseMoved, point %f, %f", point.x, point.y);
        DECLARE_METHOD(dragMouseMovedMethod, CDragSourceContextPeerClass, "dragMouseMoved", "(IIII)V");
        (*env)->CallVoidMethod(env, fDragSourceContextPeer, dragMouseMovedMethod, targetActions, (jint) fModifiers, (jint) point.x, (jint) point.y);
        CHECK_EXCEPTION();
    }
JNI_COCOA_EXIT(env);
}

- (BOOL)ignoreModifierKeysWhileDragging {
    //DLog2(@"[CDragSource ignoreModifierKeysWhileDragging]: %@\n", self);
    return NO;
}

/********************************  END NSDraggingSource Interface  ********************************/


// postDragEnter and postDragExit are called from CDropTarget when possible and appropriate
// Currently only possible if source and target are in the same process
- (void) postDragEnter {
    JNIEnv *env = [ThreadUtilities getJNIEnv];
    sNeedsEnter = NO;

    jint targetActions = fSourceActions;
    if ([CDropTarget currentDropTarget]) targetActions = [[CDropTarget currentDropTarget] currentJavaActions];

    NSPoint point = [self mapNSScreenPointToJavaWithOffset:sDraggingLocation];
    DLog3(@"  -> posting dragEnter, point %f, %f", point.x, point.y);
    GET_DSCP_CLASS();
    DECLARE_METHOD(dragEnterMethod, CDragSourceContextPeerClass, "dragEnter", "(IIII)V");
    (*env)->CallVoidMethod(env, fDragSourceContextPeer, dragEnterMethod, targetActions, (jint) fModifiers, (jint) point.x, (jint) point.y);
     CHECK_EXCEPTION();
}

- (void) postDragExit {
    JNIEnv *env = [ThreadUtilities getJNIEnv];
    sNeedsEnter = YES;

    NSPoint point = [self mapNSScreenPointToJavaWithOffset:sDraggingLocation];
    DLog3(@"  -> posting dragExit, point %f, %f", point.x, point.y);
    GET_DSCP_CLASS();
    DECLARE_METHOD(dragExitMethod, CDragSourceContextPeerClass, "dragExit", "(II)V");
    (*env)->CallVoidMethod(env, fDragSourceContextPeer, dragExitMethod, (jint) point.x, (jint) point.y);
    CHECK_EXCEPTION();

}


// Java assumes that the origin is the top-left corner of the screen.
// Cocoa assumes that the origin is the bottom-left corner of the screen.
// Adjust the y coordinate to account for this.
// NOTE: Also need to take into account the 0 screen relative screen coords.
//  This is because all screen coords in Cocoa are relative to the 0 screen.
// Also see +[CWindow convertAWTToCocoaScreenRect]
// NSScreen-to-JavaScreen mapping:
- (NSPoint) mapNSScreenPointToJavaWithOffset:(NSPoint)screenPoint {
    NSRect mainR = [[[NSScreen screens] objectAtIndex:0] frame];
    NSPoint point = NSMakePoint(screenPoint.x, mainR.size.height - (screenPoint.y));

    // Adjust the point with the drag image offset to get the real mouse hotspot:
    // The point should remain in screen coordinates (as per DragSourceEvent.getLocation() documentation)
    point.x -= fDragImageOffset.x;
    point.y -= ([fDragImage size].height + fDragImageOffset.y);

    return point;
}

@end
