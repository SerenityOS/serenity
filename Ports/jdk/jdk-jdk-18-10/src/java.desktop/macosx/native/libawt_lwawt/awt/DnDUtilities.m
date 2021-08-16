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


/*
Documentation for Drag and Drop (Radar 3065640)
There are several problems with Drag and Drop - notably, the mismatch between Java, Cocoa, and Carbon

 Java reports both the original source actions, and the user-selected actions (selected using KB modifiers) to both the source and target during the drag. AppKit only reports to the destination during the drag. This was solved by directly asking CGS for the KB state during the source's image moved callback.

 Java uses Shift/Move, Control/Copy and Shift+Control/Link. AppKit uses Command/Move, Alternate/Copy and Control/Link. Carbon uses Command/Move, Alternate/Copy and Command+Alternate/Link. This is bad, because Control overlaps between Java and AppKit. In this case, we choose compatibility between Carbon and Java (Java wins over AppKit wrt Control). This means that drags between Java applications will work correctly, regardless of whether you use the Carbon or the Java key modifiers. Drags to Java applications will work correctly regardless of whether you use the Carbon or the Java key modifiers. Drags from Java applications to non-Java applications will only work if you use the Carbon modifiers.

 The reason we can't just set the CoreDrag(G/S)etAllowableActions directly (while ignoring the modifier keys) is because Carbon apps traditionally don't pay any attention - they only look at the modifier keys.
 */

#import <Cocoa/Cocoa.h>
#import "DnDUtilities.h"
#import "java_awt_dnd_DnDConstants.h"
#import "java_awt_event_InputEvent.h"

@implementation DnDUtilities

// Make sure we don't let other apps see local drags by using a process unique pasteboard type.
// This may not work in the Applet case, since they are all running in the same VM
+ (NSString *) javaPboardType {
    static NSString *customJavaPboardType = nil;
    if (customJavaPboardType == nil)
        customJavaPboardType = [[NSString stringWithFormat:@"NSJavaPboardType-%@", [[NSProcessInfo processInfo] globallyUniqueString]] retain];
    return customJavaPboardType;
}

+ (jint)mapNSDragOperationToJava:(NSDragOperation)dragOperation
{
    jint result = java_awt_dnd_DnDConstants_ACTION_NONE;

    if ((dragOperation & NSDragOperationCopy) != 0)                    // 1
        result = ((dragOperation & NSDragOperationMove) == 0) ? java_awt_dnd_DnDConstants_ACTION_COPY : java_awt_dnd_DnDConstants_ACTION_COPY_OR_MOVE;

    else if ((dragOperation & NSDragOperationMove) != 0)            // 16
        result = java_awt_dnd_DnDConstants_ACTION_MOVE;

    else if ((dragOperation & NSDragOperationLink) != 0)            // 2
        result = java_awt_dnd_DnDConstants_ACTION_LINK;

    else if ((dragOperation & NSDragOperationGeneric) != 0)            // 4
        result = java_awt_dnd_DnDConstants_ACTION_MOVE;

    // Pre-empted by the above cases:
    //else if (dragOperation == NSDragOperationEvery)                    // UINT_MAX
    //    result = java_awt_dnd_DnDConstants_ACTION_COPY_OR_MOVE;

    // To be rejected:
    //else if ((dragOperation & NSDragOperationPrivate) != 0)        // 8
    //else if ((dragOperation & NSDragOperationAll_Obsolete) != 0)    // 15
    //else if ((dragOperation & NSDragOperationDelete) != 0)        // 32

    return result;
}

+ (jint)mapNSDragOperationMaskToJava:(NSDragOperation)dragOperation
{
    jint result = java_awt_dnd_DnDConstants_ACTION_NONE;

    if (dragOperation & NSDragOperationMove)
        result |= java_awt_dnd_DnDConstants_ACTION_MOVE;

    if (dragOperation & NSDragOperationCopy)
        result |= java_awt_dnd_DnDConstants_ACTION_COPY;

    if (dragOperation & NSDragOperationLink)
        result |= java_awt_dnd_DnDConstants_ACTION_LINK;

    // Only look at Generic if none of the other options are specified
    if ( (dragOperation & NSDragOperationGeneric) && !(dragOperation & (NSDragOperationMove|NSDragOperationCopy|NSDragOperationLink)) )
        result |= java_awt_dnd_DnDConstants_ACTION_MOVE;

    return result;
}

+ (jint)narrowJavaDropActions:(jint)actions
{
    if (YES) {
        // Order is defined in the java.awt.dnd.DropTargetDropEvent JavaDoc
        if (actions & java_awt_dnd_DnDConstants_ACTION_MOVE) {
            return java_awt_dnd_DnDConstants_ACTION_MOVE;
        }
        if (actions & java_awt_dnd_DnDConstants_ACTION_COPY) {
            return java_awt_dnd_DnDConstants_ACTION_COPY;
        }
        if (actions & java_awt_dnd_DnDConstants_ACTION_LINK) {
            return java_awt_dnd_DnDConstants_ACTION_LINK;
        }
    } else {
        // Order is what is most intuitive on Mac OS X
        if (actions & java_awt_dnd_DnDConstants_ACTION_COPY) {
            return java_awt_dnd_DnDConstants_ACTION_COPY;
        }
        if (actions & java_awt_dnd_DnDConstants_ACTION_LINK) {
            return java_awt_dnd_DnDConstants_ACTION_LINK;
        }
        if (actions & java_awt_dnd_DnDConstants_ACTION_MOVE) {
            return java_awt_dnd_DnDConstants_ACTION_MOVE;
        }
    }

    return java_awt_dnd_DnDConstants_ACTION_NONE;
}

+ (NSDragOperation)mapJavaDragOperationToNS:(jint)dragOperation
{
    NSDragOperation result = NSDragOperationNone;

    switch (dragOperation) {
        case java_awt_dnd_DnDConstants_ACTION_NONE:            // 0
            result = NSDragOperationNone;
            break;
        case java_awt_dnd_DnDConstants_ACTION_COPY:            // 1
            result = NSDragOperationCopy;
            break;
        case java_awt_dnd_DnDConstants_ACTION_MOVE:            // 2
            result = NSDragOperationMove;
            break;
        case java_awt_dnd_DnDConstants_ACTION_COPY_OR_MOVE:    // 3
            result = NSDragOperationCopy | NSDragOperationMove;
            break;
        case java_awt_dnd_DnDConstants_ACTION_LINK:            // 1073741824L
            result = NSDragOperationLink;
            break;
        case (java_awt_dnd_DnDConstants_ACTION_COPY_OR_MOVE | java_awt_dnd_DnDConstants_ACTION_LINK):
            result = NSDragOperationCopy | NSDragOperationMove | NSDragOperationLink;
            break;
    }

        if (result != NSDragOperationNone) {
            result |= NSDragOperationGeneric;
        }

    return result;
}

// Mouse and key modifiers mapping:
+ (NSUInteger)mapJavaExtModifiersToNSMouseDownButtons:(jint)modifiers
{
    NSUInteger result = NSLeftMouseDown;

    if ((modifiers & java_awt_event_InputEvent_BUTTON1_DOWN_MASK) != 0)
        result = NSLeftMouseDown;

    if ((modifiers & java_awt_event_InputEvent_BUTTON2_DOWN_MASK) != 0)
        result = NSOtherMouseDown;

    if ((modifiers & java_awt_event_InputEvent_BUTTON3_DOWN_MASK) != 0)
        result = NSRightMouseDown;

    return result;
}

+ (NSUInteger)mapJavaExtModifiersToNSMouseUpButtons:(jint)modifiers
{
    NSUInteger result = NSLeftMouseUp;

    if ((modifiers & java_awt_event_InputEvent_BUTTON1_DOWN_MASK) != 0)
        result = NSLeftMouseUp;

    if ((modifiers & java_awt_event_InputEvent_BUTTON2_DOWN_MASK) != 0)
        result = NSOtherMouseUp;

    if ((modifiers & java_awt_event_InputEvent_BUTTON3_DOWN_MASK) != 0)
        result = NSRightMouseUp;

    return result;
}


// Specialized key modifiers mappings (for DragSource.operationChanged)

// Returns just the key modifiers from a java modifier flag
+ (jint)extractJavaExtKeyModifiersFromJavaExtModifiers:(jint)modifiers
{
    // Build the mask
    static jint mask = java_awt_event_InputEvent_SHIFT_DOWN_MASK | java_awt_event_InputEvent_CTRL_DOWN_MASK | java_awt_event_InputEvent_META_DOWN_MASK | java_awt_event_InputEvent_ALT_DOWN_MASK;
    //static int mask = java_awt_event_InputEvent_SHIFT_DOWN_MASK | java_awt_event_InputEvent_CTRL_DOWN_MASK;

    // Get results
    jint result = modifiers & mask;

    // Java appears to have 2 ALT buttons - combine them.
    if (modifiers & java_awt_event_InputEvent_ALT_GRAPH_DOWN_MASK)
        result |= java_awt_event_InputEvent_ALT_DOWN_MASK;

    return result;
}

// Returns just the mouse modifiers from a java modifier flag
+ (jint)extractJavaExtMouseModifiersFromJavaExtModifiers:(jint)modifiers
{
    // Build the mask
    static jint mask = java_awt_event_InputEvent_BUTTON1_DOWN_MASK | java_awt_event_InputEvent_BUTTON2_DOWN_MASK | java_awt_event_InputEvent_BUTTON3_DOWN_MASK;

    // Get results
    return modifiers & mask;
}

+ (NSDragOperation) nsDragOperationForModifiers:(NSUInteger)modifiers {

    // Java first
    if ( (modifiers & NSShiftKeyMask) && (modifiers & NSControlKeyMask) ) {
        return NSDragOperationLink;
    }
    if (modifiers & NSShiftKeyMask) {
        return NSDragOperationMove;
    }
    if (modifiers & NSControlKeyMask) {
        return NSDragOperationCopy;
    }

    // Then native
    if ( (modifiers & NSCommandKeyMask) && (modifiers & NSAlternateKeyMask) ) {
        return NSDragOperationLink;
    }
    if (modifiers & NSCommandKeyMask) {
        return NSDragOperationMove;
    }
    if (modifiers & NSAlternateKeyMask) {
        return NSDragOperationCopy;
    }

    // Otherwise, we allow anything
    return NSDragOperationEvery;
}

+ (jint) javaKeyModifiersForNSDragOperation:(NSDragOperation)dragOperation {
    if (dragOperation & NSDragOperationMove)
        return java_awt_event_InputEvent_SHIFT_DOWN_MASK;

    if (dragOperation & NSDragOperationCopy)
        return java_awt_event_InputEvent_CTRL_DOWN_MASK;

    if (dragOperation & NSDragOperationLink) {
        return java_awt_event_InputEvent_SHIFT_DOWN_MASK | java_awt_event_InputEvent_CTRL_DOWN_MASK;
    }
    return 0;
}

@end
