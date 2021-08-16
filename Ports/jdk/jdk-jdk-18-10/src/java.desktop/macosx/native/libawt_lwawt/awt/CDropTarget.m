/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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

#import "CDropTarget.h"
#import "AWTView.h"

#import "sun_lwawt_macosx_CDropTarget.h"
#import "java_awt_dnd_DnDConstants.h"

#import <JavaRuntimeSupport/JavaRuntimeSupport.h>
#include <objc/objc-runtime.h>


#import "CDragSource.h"
#import "CDataTransferer.h"
#import "DnDUtilities.h"
#import "ThreadUtilities.h"
#import "JNIUtilities.h"


static NSInteger        sDraggingSequenceNumber = -1;
static NSDragOperation    sDragOperation;
static NSDragOperation    sUpdateOperation;
static jint                sJavaDropOperation;
static NSPoint            sDraggingLocation;
static BOOL                sDraggingExited;
static BOOL                sDraggingError;

static NSUInteger        sPasteboardItemsCount = 0;
static NSArray*            sPasteboardTypes = nil;
static NSArray*            sPasteboardData = nil;
static jlongArray        sDraggingFormats = nil;

static CDropTarget*        sCurrentDropTarget;

extern jclass jc_CDropTargetContextPeer;
#define GET_DTCP_CLASS() \
    GET_CLASS(jc_CDropTargetContextPeer, "sun/lwawt/macosx/CDropTargetContextPeer");

#define GET_DTCP_CLASS_RETURN(ret) \
    GET_CLASS_RETURN(jc_CDropTargetContextPeer, "sun/lwawt/macosx/CDropTargetContextPeer", ret);

@implementation CDropTarget

+ (CDropTarget *) currentDropTarget {
    return sCurrentDropTarget;
}

- (id)init:(jobject)jdropTarget component:(jobject)jcomponent control:(id)control
{
    self = [super init];
    DLog2(@"[CDropTarget init]: %@\n", self);

    fView = nil;
    fComponent = nil;
    fDropTarget = nil;
    fDropTargetContextPeer = nil;


    if (control != nil) {
        JNIEnv *env = [ThreadUtilities getJNIEnvUncached];
        fComponent = (*env)->NewGlobalRef(env, jcomponent);
        fDropTarget = (*env)->NewGlobalRef(env, jdropTarget);

        fView = [((AWTView *) control) retain];
        [fView setDropTarget:self];


    } else {
        // This would be an error.
        [self release];
        self = nil;
    }
    return self;
}

// When [CDropTarget init] is called the ControlModel's fView may not have been set up yet. ControlModel
// (soon after) calls [CDropTarget controlModelControlValid] on the native event thread, once per CDropTarget,
// to let it know it's been set up now.
- (void)controlModelControlValid
{
    // 9-30-02 Note: [Radar 3065621]
    // List all known pasteboard types here (see AppKit's NSPasteboard.h)
    // How to register for non-standard data types remains to be determined.
    NSArray* dataTypes = [[NSArray alloc] initWithObjects:
        NSStringPboardType,
        NSFilenamesPboardType,
        NSPostScriptPboardType,
        NSTIFFPboardType,
        NSPasteboardTypePNG,
        NSRTFPboardType,
        NSTabularTextPboardType,
        NSFontPboardType,
        NSRulerPboardType,
        NSFileContentsPboardType,
        NSColorPboardType,
        NSRTFDPboardType,
        NSHTMLPboardType,
        NSURLPboardType,
        NSPDFPboardType,
        NSVCardPboardType,
        NSFilesPromisePboardType,
        [DnDUtilities javaPboardType],
        (NSString*)kUTTypeJPEG,
        nil];

    // Enable dragging events over this object:
    [fView registerForDraggedTypes:dataTypes];

    [dataTypes release];
}

- (void)releaseDraggingData
{
    DLog2(@"[CDropTarget releaseDraggingData]: %@\n", self);

    // Release any old pasteboard types, data and properties:
    [sPasteboardTypes release];
    sPasteboardTypes = nil;

    [sPasteboardData release];
    sPasteboardData = nil;

    if (sDraggingFormats != NULL) {
        JNIEnv *env = [ThreadUtilities getJNIEnv];
        (*env)->DeleteGlobalRef(env, sDraggingFormats);
        sDraggingFormats = NULL;
    }

    sPasteboardItemsCount = 0;
    sDraggingSequenceNumber = -1;
}

- (void)removeFromView:(JNIEnv *)env
{
    DLog2(@"[CDropTarget removeFromView]: %@\n", self);

    // Remove this dragging destination from the view:
    [((AWTView *) fView) setDropTarget:nil];

    // Clean up JNI refs
    if (fComponent != NULL) {
        (*env)->DeleteGlobalRef(env, fComponent);
        fComponent = NULL;
    }
    if (fDropTarget != NULL) {
        (*env)->DeleteGlobalRef(env, fDropTarget);
        fDropTarget = NULL;
    }
    if (fDropTargetContextPeer != NULL) {
        (*env)->DeleteGlobalRef(env, fDropTargetContextPeer);
        fDropTargetContextPeer = NULL;
    }

    [self release];
}

- (void)dealloc
{
    DLog2(@"[CDropTarget dealloc]: %@\n", self);

    if(sCurrentDropTarget == self) {
        sCurrentDropTarget = nil;
    }

    [fView release];
    fView = nil;

    [super dealloc];
}

- (NSInteger) getDraggingSequenceNumber
{
    return sDraggingSequenceNumber;
}

// Debugging help:
- (void)dumpPasteboard:(NSPasteboard*)pasteboard
{
    NSArray* pasteboardTypes = [pasteboard types];
    NSUInteger pasteboardItemsCount = [pasteboardTypes count];
    NSUInteger i;

    // For each flavor on the pasteboard show the type, its data, and its property if there is one:
    for (i = 0; i < pasteboardItemsCount; i++) {
        NSString* pbType = [pasteboardTypes objectAtIndex:i];
        CFShow(pbType);

        NSData*    pbData = [pasteboard dataForType:pbType];
        CFShow(pbData);

        if ([pbType hasPrefix:@"CorePasteboardFlavorType"] == NO) {
            id pbDataProperty = [pasteboard propertyListForType:pbType];
            CFShow(pbDataProperty);
        }
    }
}

- (BOOL)copyDraggingTypes:(id<NSDraggingInfo>)sender
{
    DLog2(@"[CDropTarget copyDraggingTypes]: %@\n", self);
    JNIEnv*    env = [ThreadUtilities getJNIEnv];

    // Release any old pasteboard data:
    [self releaseDraggingData];

    NSPasteboard* pb = [sender draggingPasteboard];
    sPasteboardTypes = [[pb types] retain];
    sPasteboardItemsCount = [sPasteboardTypes count];
    if (sPasteboardItemsCount == 0)
        return FALSE;

    jlongArray formats = (*env)->NewLongArray(env, sPasteboardItemsCount);
    if (formats == nil)
        return FALSE;

    sDraggingFormats = (jlongArray) (*env)->NewGlobalRef(env, formats);
    (*env)->DeleteLocalRef(env, formats);
    if (sDraggingFormats == nil)
        return FALSE;

    jboolean isCopy;
    jlong* jformats = (*env)->GetLongArrayElements(env, sDraggingFormats, &isCopy);
    if (jformats == nil) {
        return FALSE;
    }

    // Copy all data formats and properties. In case of properties, if they are nil, we need to use
    // a special NilProperty since [NSArray addObject] would crash on adding a nil object.
    DLog2(@"[CDropTarget copyDraggingTypes]: typesCount = %lu\n", (unsigned long) sPasteboardItemsCount);
    NSUInteger i;
    for (i = 0; i < sPasteboardItemsCount; i++) {
        NSString* pbType = [sPasteboardTypes objectAtIndex:i];
        DLog3(@"[CDropTarget copyDraggingTypes]: type[%lu] = %@\n", (unsigned long) i, pbType);

        // 01-10-03 Note: until we need data properties for doing something useful don't copy them.
        // They're often copies of their flavor's data and copying them for all available pasteboard flavors
        // (which are often auto-translation of one another) can be a significant time/space hit.

        // If this is a remote object type (not a pre-defined format) register it with the pasteboard:
        jformats[i] = indexForFormat(pbType);
        if (jformats[i] == -1 && [pbType hasPrefix:@"JAVA_DATAFLAVOR:application/x-java-remote-object;"])
            jformats[i] = registerFormatWithPasteboard(pbType);
    }

    (*env)->ReleaseLongArrayElements(env, sDraggingFormats, jformats, JNI_COMMIT);

    return TRUE;
}

- (BOOL)copyDraggingData:(id<NSDraggingInfo>)sender
{
    DLog2(@"[CDropTarget copyDraggingData]: %@\n", self);

    sPasteboardData = [[NSMutableArray alloc] init];
    if (sPasteboardData == nil)
        return FALSE;

    // Copy all data items to a safe place since the pasteboard may go away before we'll need them:
    NSPasteboard* pb = [sender draggingPasteboard];
    NSUInteger i;
    for (i = 0; i < sPasteboardItemsCount; i++) {
        // Get a type and its data and save the data:
        NSString* pbType = [sPasteboardTypes objectAtIndex:i];
        // 01-10-03 Note: copying only NS-type data (until Java-specified types can make it through the AppKit)
        // would be a good idea since we can't do anything with those CoreFoundation unknown types anyway.
        // But I'm worried that it would break something in Fuller so I'm leaving this here as a reminder,
        // to be evaluated later.
        //id pbData = [pbType hasPrefix:@"NS"] ? [pb dataForType:pbType] : nil; // Copy only NS-type data!
        id pbData = [pb dataForType:pbType];

        // If the data is null we can't store it in the array - an exception would be thrown.
        // We use the special object NSNull instead which is kosher.
        if (pbData == nil)
            pbData = [NSNull null];

        [((NSMutableArray*) sPasteboardData) addObject:pbData];
    }

    return TRUE;
}

- (NSData*) getDraggingDataForURL:(NSData*)data
{
    NSData* result = nil;

    // Convert data into a property list if possible:
    NSPropertyListFormat propertyListFormat;
    NSString* errorString = nil;
    id propertyList = [NSPropertyListSerialization propertyListFromData:data mutabilityOption:NSPropertyListImmutable
        format:&propertyListFormat errorDescription:&errorString];

    // URL types have only a single URL string in an array:
    if (propertyList != nil && errorString == nil && [propertyList isKindOfClass:[NSArray class]]) {
        NSArray*  array = (NSArray*) propertyList;
        if ([array count] > 0) {
            NSString* url = (NSString*) [array objectAtIndex:0];
            if (url != nil && [url length] > 0)
                result = [url dataUsingEncoding:[url fastestEncoding]];
        }
    }

    return result;
}

- (jobject) copyDraggingDataForFormat:(jlong)format
{
    JNIEnv*      env = [ThreadUtilities getJNIEnvUncached]; // Join the main thread by requesting uncached environment

    NSData*      data = nil;

    // Convert the Java format (datatransferer int index) to a pasteboard format (NSString):
    NSString* pbType = formatForIndex(format);
    if ([sPasteboardTypes containsObject:pbType]) {
        NSUInteger dataIndex = [sPasteboardTypes indexOfObject:pbType];
        data = [sPasteboardData objectAtIndex:dataIndex];

        if ((id) data == [NSNull null])
            data = nil;

        // format == 8 (CF_URL in CDataTransferer): we need a URL-to-String conversion:
        else if ([pbType isEqualToString:@"Apple URL pasteboard type"])
            data = [self getDraggingDataForURL:data];
    }

    // Get NS data:
    char* dataBytes = (data != nil) ? (char*) [data bytes] : "Unsupported type";
    NSUInteger dataLength = (data != nil) ? [data length] : sizeof("Unsupported type");

    // Create a global byte array:
    jbyteArray lbyteArray = (*env)->NewByteArray(env, dataLength);
    if (lbyteArray == nil)
        return nil;
    jbyteArray gbyteArray = (jbyteArray) (*env)->NewGlobalRef(env, lbyteArray);
    (*env)->DeleteLocalRef(env, lbyteArray);
    if (gbyteArray == nil)
        return nil;

    // Get byte array elements:
    jboolean isCopy;
    jbyte* jbytes = (*env)->GetByteArrayElements(env, gbyteArray, &isCopy);
    if (jbytes == nil)
        return nil;

    // Copy data to byte array and release elements:
    memcpy(jbytes, dataBytes, dataLength);
    (*env)->ReleaseByteArrayElements(env, gbyteArray, jbytes, JNI_COMMIT);

    // In case of an error make sure to return nil:
    if ((*env)->ExceptionOccurred(env)) {
                (*env)->ExceptionDescribe(env);
        gbyteArray = nil;
        }

    return gbyteArray;
}

- (void)safeReleaseDraggingData:(NSNumber *)arg
{
    jlong draggingSequenceNumber = [arg longLongValue];

    // Make sure dragging data is released only if no new drag is under way. If a new drag
    // has been initiated it has released the old dragging data already. This has to be called
    // on the native event thread - otherwise we'd need to start synchronizing.
    if (draggingSequenceNumber == sDraggingSequenceNumber)
        [self releaseDraggingData];
}

- (void)javaDraggingEnded:(jlong)draggingSequenceNumber success:(BOOL)jsuccess action:(jint)jdropaction
{
    NSNumber *draggingSequenceNumberID = [NSNumber numberWithLongLong:draggingSequenceNumber];
        // Report back actual Swing success, not what AppKit thinks
        sDraggingError = !jsuccess;
        sDragOperation = [DnDUtilities mapJavaDragOperationToNS:jdropaction];

    // Release dragging data if any when Java's AWT event thread is all finished.
    // Make sure dragging data is released on the native event thread.
    [ThreadUtilities performOnMainThread:@selector(safeReleaseDraggingData:) on:self withObject:draggingSequenceNumberID waitUntilDone:NO];
}

- (jint)currentJavaActions {
    return [DnDUtilities mapNSDragOperationToJava:sUpdateOperation];
}

/********************************  BEGIN NSDraggingDestination Interface  ********************************/


// Private API to calculate the current Java actions
- (void) calculateCurrentSourceActions:(jint *)actions dropAction:(jint *)dropAction
{
    // Get the raw (unmodified by keys) source actions
    id jrsDrag = objc_lookUpClass("JRSDrag");
    if (jrsDrag != nil) {
        NSDragOperation rawDragActions = (NSDragOperation) [jrsDrag performSelector:@selector(currentAllowableActions)];
        if (rawDragActions != NSDragOperationNone) {
            // Both actions and dropAction default to the rawActions
            *actions = [DnDUtilities mapNSDragOperationMaskToJava:rawDragActions];
            *dropAction = *actions;

            // Get the current key modifiers.
            NSUInteger dragModifiers = (NSUInteger) [jrsDrag performSelector:@selector(currentModifiers)];
            // Either the drop action is narrowed as per Java rules (MOVE, COPY, LINK, NONE) or by the drag modifiers
            if (dragModifiers) {
                // Get the user selected operation based on the drag modifiers, then return the intersection
                NSDragOperation currentOp = [DnDUtilities nsDragOperationForModifiers:dragModifiers];
                NSDragOperation allowedOp = rawDragActions & currentOp;

                *dropAction = [DnDUtilities mapNSDragOperationToJava:allowedOp];
            }
        }
    }
    *dropAction = [DnDUtilities narrowJavaDropActions:*dropAction];
}

- (NSDragOperation)draggingEntered:(id<NSDraggingInfo>)sender
{
    DLog2(@"[CDropTarget draggingEntered]: %@\n", self);

    sCurrentDropTarget = self;

    JNIEnv* env = [ThreadUtilities getJNIEnv];
    NSInteger draggingSequenceNumber = [sender draggingSequenceNumber];

    // Set the initial drag operation return value:
    NSDragOperation dragOp = NSDragOperationNone;
        sJavaDropOperation = java_awt_dnd_DnDConstants_ACTION_NONE;

    // We could probably special-case some stuff if drag and drop objects match:
    //if ([sender dragSource] == fView)

    if (draggingSequenceNumber != sDraggingSequenceNumber) {
        sDraggingSequenceNumber = draggingSequenceNumber;
        sDraggingError = FALSE;

        // Delete any drop target context peer left over from a previous drag:
        if (fDropTargetContextPeer != NULL) {
            (*env)->DeleteGlobalRef(env, fDropTargetContextPeer);
            fDropTargetContextPeer = NULL;
        }

        // Look up the CDropTargetContextPeer class:
        GET_DTCP_CLASS_RETURN(dragOp);
        DECLARE_STATIC_METHOD_RETURN(getDropTargetContextPeerMethod, jc_CDropTargetContextPeer,
                                     "getDropTargetContextPeer", "()Lsun/lwawt/macosx/CDropTargetContextPeer;", dragOp)
        if (sDraggingError == FALSE) {
            // Create a new drop target context peer:
            jobject dropTargetContextPeer = (*env)->CallStaticObjectMethod(env, jc_CDropTargetContextPeer, getDropTargetContextPeerMethod);
            CHECK_EXCEPTION();

            if (dropTargetContextPeer != nil) {
                fDropTargetContextPeer = (*env)->NewGlobalRef(env, dropTargetContextPeer);
                (*env)->DeleteLocalRef(env, dropTargetContextPeer);
            }
        }

        // Get dragging types (dragging data is only copied if dropped):
        if (sDraggingError == FALSE && [self copyDraggingTypes:sender] == FALSE)
            sDraggingError = TRUE;
    }

    if (sDraggingError == FALSE) {
        sDraggingExited = FALSE;
        sDraggingLocation = [sender draggingLocation];
        NSPoint javaLocation = [fView convertPoint:sDraggingLocation fromView:nil];
        javaLocation.y = fView.window.frame.size.height - javaLocation.y;

        DLog5(@"+ dragEnter: loc native %f, %f, java %f, %f\n", sDraggingLocation.x, sDraggingLocation.y, javaLocation.x, javaLocation.y);

                ////////// BEGIN Calculate the current drag actions //////////
                jint actions = java_awt_dnd_DnDConstants_ACTION_NONE;
        jint dropAction = actions;

                [self calculateCurrentSourceActions:&actions dropAction:&dropAction];

                sJavaDropOperation = dropAction;
                ////////// END Calculate the current drag actions //////////

        jlongArray formats = sDraggingFormats;

        GET_DTCP_CLASS_RETURN(dragOp);
        DECLARE_METHOD_RETURN(handleEnterMessageMethod, jc_CDropTargetContextPeer,
                              "handleEnterMessage", "(Ljava/awt/Component;IIII[JJ)I", dragOp);
        if (sDraggingError == FALSE) {
            // Double-casting self gets rid of 'different size' compiler warning:
            // AWT_THREADING Safe (CToolkitThreadBlockedHandler)
            actions = (*env)->CallIntMethod(env, fDropTargetContextPeer, handleEnterMessageMethod,
                                       fComponent, (jint) javaLocation.x, (jint) javaLocation.y,
                                       dropAction, actions, formats, ptr_to_jlong(self));
            CHECK_EXCEPTION();
        }

        if (sDraggingError == FALSE) {
            // Initialize drag operation:
            sDragOperation = NSDragOperationNone;

            // Map Java actions back to NSDragOperation.
            // 1-6-03 Note: if the entry point of this CDropTarget isn't covered by a droppable component
            // (as can be the case with lightweight children) we must not return NSDragOperationNone
            // since that would prevent dropping into any of the contained drop targets.
            // Unfortunately there is no easy way to test this so we just test actions and override them
            // with GENERIC if necessary. Proper drag operations will be returned by draggingUpdated: which is
            // called right away, taking care of setting the right cursor and snap-back action.
            dragOp = ((actions != java_awt_dnd_DnDConstants_ACTION_NONE) ?
                [DnDUtilities mapJavaDragOperationToNS:dropAction] : NSDragOperationGeneric);

            // Remember the dragOp for no-op'd update messages:
            sUpdateOperation = dragOp;
        }
    }

    // 9-11-02 Note: the native event thread would not handle an exception gracefully:
    //if (sDraggingError == TRUE)
    //    [NSException raise:NSGenericException format:@"[CDropTarget draggingEntered] failed."];

    DLog2(@"[CDropTarget draggingEntered]: returning %lu\n", (unsigned long) dragOp);

    return dragOp;
}

- (NSDragOperation)draggingUpdated:(id<NSDraggingInfo>)sender
{
    //DLog2(@"[CDropTarget draggingUpdated]: %@\n", self);

    sCurrentDropTarget = self;

    // Set the initial drag operation return value:
    NSDragOperation dragOp = (sDraggingError == FALSE ? sUpdateOperation : NSDragOperationNone);

    // There are two things we would be interested in:
    // a) mouse pointer has moved
    // b) drag actions (key modifiers) have changed

    NSPoint draggingLocation = [sender draggingLocation];
    JNIEnv* env = [ThreadUtilities getJNIEnv];

    BOOL notifyJava = FALSE;

    // a) mouse pointer has moved:
    if (NSEqualPoints(draggingLocation, sDraggingLocation) == FALSE) {
        //DLog2(@"[CDropTarget draggingUpdated]: mouse moved, %@\n", self);
        sDraggingLocation = draggingLocation;
        notifyJava = TRUE;
    }

    // b) drag actions (key modifiers) have changed (handleMotionMessage() will do proper notifications):
        ////////// BEGIN Calculate the current drag actions //////////
        jint actions = java_awt_dnd_DnDConstants_ACTION_NONE;
        jint dropAction = actions;

        [self calculateCurrentSourceActions:&actions dropAction:&dropAction];

        if (sJavaDropOperation != dropAction) {
            sJavaDropOperation = dropAction;
            notifyJava = TRUE;
        }
        ////////// END Calculate the current drag actions //////////

    jint userAction = dropAction;

    // Should we notify Java things have changed?
    if (sDraggingError == FALSE && notifyJava) {
        NSPoint javaLocation = [fView convertPoint:sDraggingLocation fromView:nil];
        javaLocation.y = fView.window.frame.size.height - javaLocation.y;
        //DLog5(@"  : dragMoved: loc native %f, %f, java %f, %f\n", sDraggingLocation.x, sDraggingLocation.y, javaLocation.x, javaLocation.y);

        jlongArray formats = sDraggingFormats;

        GET_DTCP_CLASS_RETURN(dragOp);
        DECLARE_METHOD_RETURN(handleMotionMessageMethod, jc_CDropTargetContextPeer, "handleMotionMessage", "(Ljava/awt/Component;IIII[JJ)I", dragOp);
        if (sDraggingError == FALSE) {
            DLog3(@"  >> posting handleMotionMessage, point %f, %f", javaLocation.x, javaLocation.y);
            userAction = (*env)->CallIntMethod(env, fDropTargetContextPeer, handleMotionMessageMethod, fComponent,
                         (jint) javaLocation.x, (jint) javaLocation.y, dropAction, actions, formats, ptr_to_jlong(self)); // AWT_THREADING Safe (CToolkitThreadBlockedHandler)
        CHECK_EXCEPTION();
        }

        if (sDraggingError == FALSE) {
            dragOp = [DnDUtilities mapJavaDragOperationToNS:userAction];

            // Remember the dragOp for no-op'd update messages:
            sUpdateOperation = dragOp;
        } else {
            dragOp = NSDragOperationNone;
        }
    }

    DLog2(@"[CDropTarget draggingUpdated]: returning %lu\n", (unsigned long) dragOp);

    return dragOp;
}

- (void)draggingExited:(id<NSDraggingInfo>)sender
{
    DLog2(@"[CDropTarget draggingExited]: %@\n", self);

    sCurrentDropTarget = nil;

    JNIEnv* env = [ThreadUtilities getJNIEnv];

    if (sDraggingExited == FALSE && sDraggingError == FALSE) {
        GET_DTCP_CLASS();
        DECLARE_METHOD(handleExitMessageMethod, jc_CDropTargetContextPeer, "handleExitMessage", "(Ljava/awt/Component;J)V");
        if (sDraggingError == FALSE) {
            DLog3(@"  - dragExit: loc native %f, %f\n", sDraggingLocation.x, sDraggingLocation.y);
             // AWT_THREADING Safe (CToolkitThreadBlockedHandler)
            (*env)->CallVoidMethod(env, fDropTargetContextPeer,
                              handleExitMessageMethod, fComponent, ptr_to_jlong(self));
            CHECK_EXCEPTION();
        }

        // 5-27-03 Note: [Radar 3270455]
        // -draggingExited: can be called both by the AppKit and by -performDragOperation: but shouldn't execute
        // twice per drop since cleanup code like that in swing/plaf/basic/BasicDropTargetListener would throw NPEs.
        sDraggingExited = TRUE;
    }

    DLog(@"[CDropTarget draggingExited]: returning.\n");
}

- (BOOL)prepareForDragOperation:(id <NSDraggingInfo>)sender
{
    DLog2(@"[CDropTarget prepareForDragOperation]: %@\n", self);
    DLog2(@"[CDropTarget prepareForDragOperation]: returning %@\n", (sDraggingError ? @"NO" : @"YES"));

    return sDraggingError ? NO : YES;
}

- (BOOL)performDragOperation:(id<NSDraggingInfo>)sender
{
    DLog2(@"[CDropTarget performDragOperation]: %@\n", self);

    sCurrentDropTarget = nil;

    JNIEnv* env = [ThreadUtilities getJNIEnv];

    // Now copy dragging data:
    if (sDraggingError == FALSE && [self copyDraggingData:sender] == FALSE)
        sDraggingError = TRUE;

    if (sDraggingError == FALSE) {
        sDraggingLocation = [sender draggingLocation];
        NSPoint javaLocation = [fView convertPoint:sDraggingLocation fromView:nil];
        // The y coordinate that comes in the NSDraggingInfo seems to be reversed - probably
        // has to do something with the type of view it comes to.
        // This is the earliest place where we can correct it.
        javaLocation.y = fView.window.frame.size.height - javaLocation.y;

        jint actions = [DnDUtilities mapNSDragOperationMaskToJava:[sender draggingSourceOperationMask]];
        jint dropAction = sJavaDropOperation;

        jlongArray formats = sDraggingFormats;

        GET_DTCP_CLASS_RETURN(NO);
        DECLARE_METHOD_RETURN(handleDropMessageMethod, jc_CDropTargetContextPeer, "handleDropMessage", "(Ljava/awt/Component;IIII[JJ)V", NO);

        if (sDraggingError == FALSE) {
            (*env)->CallVoidMethod(env, fDropTargetContextPeer, handleDropMessageMethod, fComponent,
                     (jint) javaLocation.x, (jint) javaLocation.y, dropAction, actions, formats, ptr_to_jlong(self)); // AWT_THREADING Safe (event)
            CHECK_EXCEPTION();
        }
    } else {
        // 8-19-03 Note: [Radar 3368754]
        // draggingExited: is not called after a drop - we must do that here ... but only in case
        // of an error, instead of drop(). Otherwise we get twice the cleanup in shared code.
        [self draggingExited:sender];
    }

// TODO:BG
//   [(id)sender _setLastDragDestinationOperation:sDragOperation];


    DLog2(@"[CDropTarget performDragOperation]: returning %@\n", (sDraggingError ? @"NO" : @"YES"));

    return !sDraggingError;
}

- (void)concludeDragOperation:(id<NSDraggingInfo>)sender
{
    sCurrentDropTarget = nil;

    DLog2(@"[CDropTarget concludeDragOperation]: %@\n", self);
    DLog(@"[CDropTarget concludeDragOperation]: returning.\n");
}

// 9-11-02 Note: draggingEnded is not yet implemented by the AppKit.
- (void)draggingEnded:(id<NSDraggingInfo>)sender
{
    sCurrentDropTarget = nil;

    DLog2(@"[CDropTarget draggingEnded]: %@\n", self);
    DLog(@"[CDropTarget draggingEnded]: returning.\n");
}

/********************************  END NSDraggingDestination Interface  ********************************/

@end


/*
 * Class:     sun_lwawt_macosx_CDropTarget
 * Method:    createNativeDropTarget
 * Signature: (Ljava/awt/dnd/DropTarget;Ljava/awt/Component;Ljava/awt/peer/ComponentPeer;J)J
 */
JNIEXPORT jlong JNICALL Java_sun_lwawt_macosx_CDropTarget_createNativeDropTarget
  (JNIEnv *env, jobject jthis, jobject jdroptarget, jobject jcomponent, jlong jnativepeer)
{
    CDropTarget* dropTarget = nil;

JNI_COCOA_ENTER(env);
    id controlObj = (id) jlong_to_ptr(jnativepeer);
    dropTarget = [[CDropTarget alloc] init:jdroptarget component:jcomponent control:controlObj];
JNI_COCOA_EXIT(env);

    return ptr_to_jlong(dropTarget);
}

/*
 * Class:     sun_lwawt_macosx_CDropTarget
 * Method:    releaseNativeDropTarget
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_sun_lwawt_macosx_CDropTarget_releaseNativeDropTarget
  (JNIEnv *env, jobject jthis, jlong nativeDropTargetVal)
{
    id dropTarget = (id)jlong_to_ptr(nativeDropTargetVal);

JNI_COCOA_ENTER(env);
    [dropTarget removeFromView:env];
JNI_COCOA_EXIT(env);
}
