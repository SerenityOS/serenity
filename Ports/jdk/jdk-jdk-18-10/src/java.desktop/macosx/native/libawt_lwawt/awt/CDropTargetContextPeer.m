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

#import "sun_lwawt_macosx_CDropTargetContextPeer.h"

#import "CDataTransferer.h"
#import "CDropTarget.h"
#import "DnDUtilities.h"
#import "ThreadUtilities.h"
#import "JNIUtilities.h"

jclass jc_CDropTargetContextPeer = NULL;
#define GET_DTCP_CLASS() \
    GET_CLASS(jc_CDropTargetContextPeer, "sun/lwawt/macosx/CDropTargetContextPeer");

#define GET_DTCP_CLASS_RETURN(ret) \
    GET_CLASS_RETURN(jc_CDropTargetContextPeer, "sun/lwawt/macosx/CDropTargetContextPeer", ret);


static void TransferFailed(JNIEnv *env, jobject jthis, jlong jdroptarget, jlong jdroptransfer, jlong jformat) {
    AWT_ASSERT_NOT_APPKIT_THREAD;
    GET_DTCP_CLASS();
    DECLARE_METHOD(transferFailedMethod, jc_CDropTargetContextPeer, "transferFailed", "(J)V");
    (*env)->CallVoidMethod(env, jthis, transferFailedMethod, jformat); // AWT_THREADING Safe (!appKit)
    CHECK_EXCEPTION();
}

static CDropTarget* GetCDropTarget(jlong jdroptarget) {
    CDropTarget* dropTarget = (CDropTarget*) jlong_to_ptr(jdroptarget);

    // Make sure the drop target is of the right kind:
    if ([dropTarget isKindOfClass:[CDropTarget class]]) {
        return dropTarget;
    }

    return nil;
}


/*
 * Class:     sun_lwawt_macosx_CDropTargetContextPeer
 * Method:    startTransfer
 * Signature: (JJ)J
 */
JNIEXPORT jlong JNICALL Java_sun_lwawt_macosx_CDropTargetContextPeer_startTransfer
  (JNIEnv *env, jobject jthis, jlong jdroptarget, jlong jformat)
{

    jlong result = (jlong) 0L;

    // Currently startTransfer and endTransfer are synchronous since [CDropTarget copyDraggingDataForFormat]
    // works off a data copy and doesn't have to go to the native event thread to get the data.
    // We can have endTransfer just call startTransfer.

JNI_COCOA_ENTER(env);
    // Get the drop target native object:
    CDropTarget* dropTarget = GetCDropTarget(jdroptarget);
    if (dropTarget == nil) {
        DLog2(@"[CDropTargetContextPeer startTransfer]: GetCDropTarget failed for %d.\n", (NSInteger) jdroptarget);
        TransferFailed(env, jthis, jdroptarget, (jlong) 0L, jformat);
        return result;
    }

    GET_DTCP_CLASS_RETURN(result);
    DECLARE_METHOD_RETURN(newDataMethod, jc_CDropTargetContextPeer, "newData", "(J[B)V", result);
    if ((*env)->ExceptionOccurred(env) || !newDataMethod) {
        DLog2(@"[CDropTargetContextPeer startTransfer]: couldn't get newData method for %d.\n", (NSInteger) jdroptarget);
        TransferFailed(env, jthis, jdroptarget, (jlong) 0L, jformat);
        return result;
    }

    // Get data from drop target:
    jobject jdropdata = [dropTarget copyDraggingDataForFormat:jformat];
    if (!jdropdata) {
        DLog2(@"[CDropTargetContextPeer startTransfer]: copyDraggingDataForFormat failed for %d.\n", (NSInteger) jdroptarget);
        TransferFailed(env, jthis, jdroptarget, (jlong) 0L, jformat);
        return result;
    }

    // Pass the data to drop target:
    @try {
        (*env)->CallVoidMethod(env, jthis, newDataMethod, jformat, jdropdata); // AWT_THREADING Safe (!appKit)
    } @catch (NSException *ex) {
        DLog2(@"[CDropTargetContextPeer startTransfer]: exception in newData() for %d.\n", (NSInteger) jdroptarget);
        (*env)->DeleteGlobalRef(env, jdropdata);
        TransferFailed(env, jthis, jdroptarget, (jlong) 0L, jformat);
        return result;
    }

    // if no error return dropTarget's draggingSequence
    result = [dropTarget getDraggingSequenceNumber];
JNI_COCOA_EXIT(env);

    return result;
}

/*
 * Class:     sun_lwawt_macosx_CDropTargetContextPeer
 * Method:    addTransfer
 * Signature: (JJJ)V
 */
JNIEXPORT void JNICALL Java_sun_lwawt_macosx_CDropTargetContextPeer_addTransfer
  (JNIEnv *env, jobject jthis, jlong jdroptarget, jlong jdroptransfer, jlong jformat)
{
    // Currently startTransfer and endTransfer are synchronous since [CDropTarget copyDraggingDataForFormat]
    // works off a data copy and doesn't have to go to the native event thread to get the data.
    // We can have endTransfer just call startTransfer.

    Java_sun_lwawt_macosx_CDropTargetContextPeer_startTransfer(env, jthis, jdroptarget, jformat);

    return;
}

/*
 * Class:     sun_lwawt_macosx_CDropTargetContextPeer
 * Method:    dropDone
 * Signature: (JJZZI)V
 */
JNIEXPORT void JNICALL Java_sun_lwawt_macosx_CDropTargetContextPeer_dropDone
  (JNIEnv *env, jobject jthis, jlong jdroptarget, jlong jdroptransfer, jboolean jislocal, jboolean jsuccess, jint jdropaction)
{
    // Get the drop target native object:
JNI_COCOA_ENTER(env);
    CDropTarget* dropTarget = GetCDropTarget(jdroptarget);
    if (dropTarget == nil) {
        DLog2(@"[CDropTargetContextPeer dropDone]: GetCDropTarget failed for %d.\n", (NSInteger) jdroptarget);
        return;
    }

    // Notify drop target Java is all done with this dragging sequence:
    [dropTarget javaDraggingEnded:(jlong)jdroptransfer success:jsuccess action:jdropaction];
JNI_COCOA_EXIT(env);

    return;
}
