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

#import <Cocoa/Cocoa.h>

#import "AWTWindow.h"
#import "AWTView.h"
#import "CPopupMenu.h"
#import "ThreadUtilities.h"
#import "LWCToolkit.h"
#import "GeomUtilities.h"

@implementation CPopupMenu

- (id) initWithPeer:(jobject)peer {
    self = [super initWithPeer:peer];
    if (self == nil) {
        // TODO: not implemented
    }
    return self;
}

- (NSString *)description {
    return [NSString stringWithFormat:@"CMenuItem[ %@ ]", fMenuItem];
}

@end // implementationCPopupMenu : CMenu


  /*
   * Class:     sun_lwawt_macosx_CPopupMenu
   * Method:    nativeCreatePopupMenu
   * Signature: (JII)J
   */
JNIEXPORT jlong JNICALL Java_sun_lwawt_macosx_CPopupMenu_nativeCreatePopupMenu
(JNIEnv *env, jobject peer) {

    __block CPopupMenu *aCPopupMenu = nil;

JNI_COCOA_ENTER(env);

    jobject cPeerObjGlobal = (*env)->NewGlobalRef(env, peer);

    [ThreadUtilities performOnMainThreadWaiting:YES block:^(){
        aCPopupMenu = [[CPopupMenu alloc] initWithPeer:cPeerObjGlobal];
    }];

JNI_COCOA_EXIT(env);

    return ptr_to_jlong(aCPopupMenu);
}

JNIEXPORT void JNICALL Java_sun_lwawt_macosx_CPopupMenu_nativeShowPopupMenu
(JNIEnv *env, jobject peer, jlong menuPtr, jint x, jint y) {

    JNI_COCOA_ENTER(env);

    CPopupMenu* cPopupMenu = (CPopupMenu*)jlong_to_ptr(menuPtr);

    [ThreadUtilities performOnMainThreadWaiting:NO block:^(){
        NSPoint loc = ConvertNSScreenPoint(env, NSMakePoint(x, y));

        [[cPopupMenu menu] popUpMenuPositioningItem: nil
                                         atLocation: loc
                                             inView: nil];
    }];

    JNI_COCOA_EXIT(env);

}

