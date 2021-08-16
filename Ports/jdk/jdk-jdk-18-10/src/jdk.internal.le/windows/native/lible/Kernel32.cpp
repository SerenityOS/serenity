/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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

#include "jni.h"
#include "jni_util.h"
#include "jvm.h"
#include "jdk_internal_org_jline_terminal_impl_jna_win_Kernel32Impl.h"

#include <stdlib.h>
#include <wincon.h>
#include <winuser.h>

static jclass pointerClass;
static jmethodID pointerConstructor;
static jfieldID pointerValue;

static jclass intByReferenceClass;
static jfieldID intByReferenceValue;

static jclass lastErrorExceptionClass;
static jmethodID lastErrorExceptionConstructor;

//jdk.internal.org.jline.terminal.impl.jna.win.Kernel32.CHAR_INFO
static jclass CHAR_INFO_Class;
static jmethodID CHAR_INFO_Constructor;
static jfieldID CHAR_INFO_uChar;
static jfieldID CHAR_INFO_Attributes;

//jdk.internal.org.jline.terminal.impl.jna.win.Kernel32.CONSOLE_CURSOR_INFO
static jclass CONSOLE_CURSOR_INFO_Class;
static jmethodID CONSOLE_CURSOR_INFO_Constructor;
static jfieldID CONSOLE_CURSOR_INFO_dwSize;
static jfieldID CONSOLE_CURSOR_INFO_bVisible;

//jdk.internal.org.jline.terminal.impl.jna.win.Kernel32.CONSOLE_SCREEN_BUFFER_INFO
static jclass CONSOLE_SCREEN_BUFFER_INFO_Class;
static jmethodID CONSOLE_SCREEN_BUFFER_INFO_Constructor;
static jfieldID CONSOLE_SCREEN_BUFFER_INFO_dwSize;
static jfieldID CONSOLE_SCREEN_BUFFER_INFO_dwCursorPosition;
static jfieldID CONSOLE_SCREEN_BUFFER_INFO_wAttributes;
static jfieldID CONSOLE_SCREEN_BUFFER_INFO_srWindow;
static jfieldID CONSOLE_SCREEN_BUFFER_INFO_dwMaximumWindowSize;

//jdk.internal.org.jline.terminal.impl.jna.win.Kernel32.COORD
static jclass COORD_Class;
static jmethodID COORD_Constructor;
static jfieldID COORD_X;
static jfieldID COORD_Y;

//jdk.internal.org.jline.terminal.impl.jna.win.Kernel32.INPUT_RECORD
static jclass INPUT_RECORD_Class;
static jmethodID INPUT_RECORD_Constructor;
static jfieldID INPUT_RECORD_EventType;
static jfieldID INPUT_RECORD_Event;

//jdk.internal.org.jline.terminal.impl.jna.win.Kernel32.INPUT_RECORD.EventUnion
static jclass EventUnion_Class;
static jmethodID EventUnion_Constructor;
static jfieldID EventUnion_KeyEvent;
static jfieldID EventUnion_MouseEvent;
static jfieldID EventUnion_WindowBufferSizeEvent;
static jfieldID EventUnion_MenuEvent;
static jfieldID EventUnion_FocusEvent;

//jdk.internal.org.jline.terminal.impl.jna.win.Kernel32.KEY_EVENT_RECORD
static jclass KEY_EVENT_RECORD_Class;
static jmethodID KEY_EVENT_RECORD_Constructor;
static jfieldID KEY_EVENT_RECORD_bKeyDown;
static jfieldID KEY_EVENT_RECORD_wRepeatCount;
static jfieldID KEY_EVENT_RECORD_wVirtualKeyCode;
static jfieldID KEY_EVENT_RECORD_wVirtualScanCode;
static jfieldID KEY_EVENT_RECORD_uChar;
static jfieldID KEY_EVENT_RECORD_dwControlKeyState;

//jdk.internal.org.jline.terminal.impl.jna.win.Kernel32.MOUSE_EVENT_RECORD
static jclass MOUSE_EVENT_RECORD_Class;
static jmethodID MOUSE_EVENT_RECORD_Constructor;
static jfieldID MOUSE_EVENT_RECORD_dwMousePosition;
static jfieldID MOUSE_EVENT_RECORD_dwButtonState;
static jfieldID MOUSE_EVENT_RECORD_dwControlKeyState;
static jfieldID MOUSE_EVENT_RECORD_dwEventFlags;

//jdk.internal.org.jline.terminal.impl.jna.win.Kernel32.WINDOW_BUFFER_SIZE_RECORD
static jclass WINDOW_BUFFER_SIZE_RECORD_Class;
static jmethodID WINDOW_BUFFER_SIZE_RECORD_Constructor;
static jfieldID WINDOW_BUFFER_SIZE_RECORD_dwSize;

//jdk.internal.org.jline.terminal.impl.jna.win.Kernel32.MENU_EVENT_RECORD
static jclass MENU_EVENT_RECORD_Class;
static jmethodID MENU_EVENT_RECORD_Constructor;
static jfieldID MENU_EVENT_RECORD_dwCommandId;

//jdk.internal.org.jline.terminal.impl.jna.win.Kernel32.FOCUS_EVENT_RECORD
static jclass FOCUS_EVENT_RECORD_Class;
static jmethodID FOCUS_EVENT_RECORD_Constructor;
static jfieldID FOCUS_EVENT_RECORD_bSetFocus;

//jdk.internal.org.jline.terminal.impl.jna.win.Kernel32.SMALL_RECT
static jclass SMALL_RECT_Class;
static jmethodID SMALL_RECT_Constructor;
static jfieldID SMALL_RECT_Left;
static jfieldID SMALL_RECT_Top;
static jfieldID SMALL_RECT_Right;
static jfieldID SMALL_RECT_Bottom;

//jdk.internal.org.jline.terminal.impl.jna.win.Kernel32.UnionChar
static jclass UnionChar_Class;
static jmethodID UnionChar_Constructor;
static jfieldID UnionChar_UnicodeChar;

JNIEXPORT void JNICALL Java_jdk_internal_org_jline_terminal_impl_jna_win_Kernel32Impl_initIDs
  (JNIEnv *env, jclass) {
    jclass cls;
    cls = env->FindClass("jdk/internal/org/jline/terminal/impl/jna/win/Pointer");
    CHECK_NULL(cls);
    pointerClass = (jclass) env->NewGlobalRef(cls);
    pointerConstructor = env->GetMethodID(cls, "<init>", "(J)V");
    CHECK_NULL(pointerConstructor);
    pointerValue  = env->GetFieldID(cls, "value", "J");
    CHECK_NULL(pointerValue);

    cls = env->FindClass("jdk/internal/org/jline/terminal/impl/jna/win/LastErrorException");
    CHECK_NULL(cls);
    lastErrorExceptionClass = (jclass) env->NewGlobalRef(cls);
    lastErrorExceptionConstructor = env->GetMethodID(cls, "<init>", "(J)V");
    CHECK_NULL(lastErrorExceptionConstructor);

    cls = env->FindClass("jdk/internal/org/jline/terminal/impl/jna/win/IntByReference");
    CHECK_NULL(cls);
    intByReferenceClass = (jclass) env->NewGlobalRef(cls);
    intByReferenceValue = env->GetFieldID(cls, "value", "I");
    CHECK_NULL(intByReferenceValue);

    //jdk.internal.org.jline.terminal.impl.jna.win.Kernel32.CHAR_INFO
    CHAR_INFO_Class = (jclass) env->NewGlobalRef(env->FindClass("jdk/internal/org/jline/terminal/impl/jna/win/Kernel32$CHAR_INFO"));
    CHECK_NULL(CHAR_INFO_Class);
    CHAR_INFO_Constructor = env->GetMethodID(cls, "<init>", "()V");
    CHECK_NULL(CHAR_INFO_Constructor);
    CHAR_INFO_uChar = env->GetFieldID(CHAR_INFO_Class, "uChar", "Ljdk/internal/org/jline/terminal/impl/jna/win/Kernel32$UnionChar;");
    CHECK_NULL(CHAR_INFO_uChar);
    CHAR_INFO_Attributes = env->GetFieldID(CHAR_INFO_Class, "Attributes", "S");
    CHECK_NULL(CHAR_INFO_Attributes);

    //jdk.internal.org.jline.terminal.impl.jna.win.Kernel32.CONSOLE_CURSOR_INFO
    CONSOLE_CURSOR_INFO_Class = (jclass) env->NewGlobalRef(env->FindClass("jdk/internal/org/jline/terminal/impl/jna/win/Kernel32$CONSOLE_CURSOR_INFO"));
    CHECK_NULL(CONSOLE_CURSOR_INFO_Class);
    CONSOLE_CURSOR_INFO_Constructor = env->GetMethodID(cls, "<init>", "()V");
    CHECK_NULL(CONSOLE_CURSOR_INFO_Constructor);
    CONSOLE_CURSOR_INFO_dwSize = env->GetFieldID(CONSOLE_CURSOR_INFO_Class, "dwSize", "I");
    CHECK_NULL(CONSOLE_CURSOR_INFO_dwSize);
    CONSOLE_CURSOR_INFO_bVisible = env->GetFieldID(CONSOLE_CURSOR_INFO_Class, "bVisible", "Z");
    CHECK_NULL(CONSOLE_CURSOR_INFO_bVisible);

    //jdk.internal.org.jline.terminal.impl.jna.win.Kernel32.CONSOLE_SCREEN_BUFFER_INFO
    CONSOLE_SCREEN_BUFFER_INFO_Class = (jclass) env->NewGlobalRef(env->FindClass("jdk/internal/org/jline/terminal/impl/jna/win/Kernel32$CONSOLE_SCREEN_BUFFER_INFO"));
    CHECK_NULL(CONSOLE_SCREEN_BUFFER_INFO_Class);
    CONSOLE_SCREEN_BUFFER_INFO_Constructor = env->GetMethodID(cls, "<init>", "()V");
    CHECK_NULL(CONSOLE_SCREEN_BUFFER_INFO_Constructor);
    CONSOLE_SCREEN_BUFFER_INFO_dwSize = env->GetFieldID(CONSOLE_SCREEN_BUFFER_INFO_Class, "dwSize", "Ljdk/internal/org/jline/terminal/impl/jna/win/Kernel32$COORD;");
    CHECK_NULL(CONSOLE_SCREEN_BUFFER_INFO_dwSize);
    CONSOLE_SCREEN_BUFFER_INFO_dwCursorPosition = env->GetFieldID(CONSOLE_SCREEN_BUFFER_INFO_Class, "dwCursorPosition", "Ljdk/internal/org/jline/terminal/impl/jna/win/Kernel32$COORD;");
    CHECK_NULL(CONSOLE_SCREEN_BUFFER_INFO_dwCursorPosition);
    CONSOLE_SCREEN_BUFFER_INFO_wAttributes = env->GetFieldID(CONSOLE_SCREEN_BUFFER_INFO_Class, "wAttributes", "S");
    CHECK_NULL(CONSOLE_SCREEN_BUFFER_INFO_wAttributes);
    CONSOLE_SCREEN_BUFFER_INFO_srWindow = env->GetFieldID(CONSOLE_SCREEN_BUFFER_INFO_Class, "srWindow", "Ljdk/internal/org/jline/terminal/impl/jna/win/Kernel32$SMALL_RECT;");
    CHECK_NULL(CONSOLE_SCREEN_BUFFER_INFO_srWindow);
    CONSOLE_SCREEN_BUFFER_INFO_dwMaximumWindowSize = env->GetFieldID(CONSOLE_SCREEN_BUFFER_INFO_Class, "dwMaximumWindowSize", "Ljdk/internal/org/jline/terminal/impl/jna/win/Kernel32$COORD;");
    CHECK_NULL(CONSOLE_SCREEN_BUFFER_INFO_dwMaximumWindowSize);

    //jdk.internal.org.jline.terminal.impl.jna.win.Kernel32.COORD
    COORD_Class = (jclass) env->NewGlobalRef(env->FindClass("jdk/internal/org/jline/terminal/impl/jna/win/Kernel32$COORD"));
    CHECK_NULL(COORD_Class);
    COORD_Constructor = env->GetMethodID(cls, "<init>", "()V");
    CHECK_NULL(COORD_Constructor);
    COORD_X = env->GetFieldID(COORD_Class, "X", "S");
    CHECK_NULL(COORD_X);
    COORD_Y = env->GetFieldID(COORD_Class, "Y", "S");
    CHECK_NULL(COORD_Y);

    //jdk.internal.org.jline.terminal.impl.jna.win.Kernel32.INPUT_RECORD
    INPUT_RECORD_Class = (jclass) env->NewGlobalRef(env->FindClass("jdk/internal/org/jline/terminal/impl/jna/win/Kernel32$INPUT_RECORD"));
    CHECK_NULL(INPUT_RECORD_Class);
    INPUT_RECORD_Constructor = env->GetMethodID(cls, "<init>", "()V");
    CHECK_NULL(INPUT_RECORD_Constructor);
    INPUT_RECORD_EventType = env->GetFieldID(INPUT_RECORD_Class, "EventType", "S");
    CHECK_NULL(INPUT_RECORD_EventType);
    INPUT_RECORD_Event = env->GetFieldID(INPUT_RECORD_Class, "Event", "Ljdk/internal/org/jline/terminal/impl/jna/win/Kernel32$INPUT_RECORD$EventUnion;");
    CHECK_NULL(INPUT_RECORD_Event);

    //jdk.internal.org.jline.terminal.impl.jna.win.Kernel32.INPUT_RECORD.EventUnion
    EventUnion_Class = (jclass) env->NewGlobalRef(env->FindClass("jdk/internal/org/jline/terminal/impl/jna/win/Kernel32$INPUT_RECORD$EventUnion"));
    CHECK_NULL(EventUnion_Class);
    EventUnion_Constructor = env->GetMethodID(cls, "<init>", "()V");
    CHECK_NULL(EventUnion_Constructor);
    EventUnion_KeyEvent = env->GetFieldID(EventUnion_Class, "KeyEvent", "Ljdk/internal/org/jline/terminal/impl/jna/win/Kernel32$KEY_EVENT_RECORD;");
    CHECK_NULL(EventUnion_KeyEvent);
    EventUnion_MouseEvent = env->GetFieldID(EventUnion_Class, "MouseEvent", "Ljdk/internal/org/jline/terminal/impl/jna/win/Kernel32$MOUSE_EVENT_RECORD;");
    CHECK_NULL(EventUnion_MouseEvent);
    EventUnion_WindowBufferSizeEvent = env->GetFieldID(EventUnion_Class, "WindowBufferSizeEvent", "Ljdk/internal/org/jline/terminal/impl/jna/win/Kernel32$WINDOW_BUFFER_SIZE_RECORD;");
    CHECK_NULL(EventUnion_WindowBufferSizeEvent);
    EventUnion_MenuEvent = env->GetFieldID(EventUnion_Class, "MenuEvent", "Ljdk/internal/org/jline/terminal/impl/jna/win/Kernel32$MENU_EVENT_RECORD;");
    CHECK_NULL(EventUnion_MenuEvent);
    EventUnion_FocusEvent = env->GetFieldID(EventUnion_Class, "FocusEvent", "Ljdk/internal/org/jline/terminal/impl/jna/win/Kernel32$FOCUS_EVENT_RECORD;");
    CHECK_NULL(EventUnion_FocusEvent);

    //jdk.internal.org.jline.terminal.impl.jna.win.Kernel32.KEY_EVENT_RECORD
    KEY_EVENT_RECORD_Class = (jclass) env->NewGlobalRef(env->FindClass("jdk/internal/org/jline/terminal/impl/jna/win/Kernel32$KEY_EVENT_RECORD"));
    CHECK_NULL(KEY_EVENT_RECORD_Class);
    KEY_EVENT_RECORD_Constructor = env->GetMethodID(cls, "<init>", "()V");
    CHECK_NULL(KEY_EVENT_RECORD_Constructor);
    KEY_EVENT_RECORD_bKeyDown = env->GetFieldID(KEY_EVENT_RECORD_Class, "bKeyDown", "Z");
    CHECK_NULL(KEY_EVENT_RECORD_bKeyDown);
    KEY_EVENT_RECORD_wRepeatCount = env->GetFieldID(KEY_EVENT_RECORD_Class, "wRepeatCount", "S");
    CHECK_NULL(KEY_EVENT_RECORD_wRepeatCount);
    KEY_EVENT_RECORD_wVirtualKeyCode = env->GetFieldID(KEY_EVENT_RECORD_Class, "wVirtualKeyCode", "S");
    CHECK_NULL(KEY_EVENT_RECORD_wVirtualKeyCode);
    KEY_EVENT_RECORD_wVirtualScanCode = env->GetFieldID(KEY_EVENT_RECORD_Class, "wVirtualScanCode", "S");
    CHECK_NULL(KEY_EVENT_RECORD_wVirtualScanCode);
    KEY_EVENT_RECORD_uChar = env->GetFieldID(KEY_EVENT_RECORD_Class, "uChar", "Ljdk/internal/org/jline/terminal/impl/jna/win/Kernel32$UnionChar;");
    CHECK_NULL(KEY_EVENT_RECORD_uChar);
    KEY_EVENT_RECORD_dwControlKeyState = env->GetFieldID(KEY_EVENT_RECORD_Class, "dwControlKeyState", "I");
    CHECK_NULL(KEY_EVENT_RECORD_dwControlKeyState);

    //jdk.internal.org.jline.terminal.impl.jna.win.Kernel32.MOUSE_EVENT_RECORD
    MOUSE_EVENT_RECORD_Class = (jclass) env->NewGlobalRef(env->FindClass("jdk/internal/org/jline/terminal/impl/jna/win/Kernel32$MOUSE_EVENT_RECORD"));
    CHECK_NULL(MOUSE_EVENT_RECORD_Class);
    MOUSE_EVENT_RECORD_Constructor = env->GetMethodID(cls, "<init>", "()V");
    CHECK_NULL(MOUSE_EVENT_RECORD_Constructor);
    MOUSE_EVENT_RECORD_dwMousePosition = env->GetFieldID(MOUSE_EVENT_RECORD_Class, "dwMousePosition", "Ljdk/internal/org/jline/terminal/impl/jna/win/Kernel32$COORD;");
    CHECK_NULL(MOUSE_EVENT_RECORD_dwMousePosition);
    MOUSE_EVENT_RECORD_dwButtonState = env->GetFieldID(MOUSE_EVENT_RECORD_Class, "dwButtonState", "I");
    CHECK_NULL(MOUSE_EVENT_RECORD_dwButtonState);
    MOUSE_EVENT_RECORD_dwControlKeyState = env->GetFieldID(MOUSE_EVENT_RECORD_Class, "dwControlKeyState", "I");
    CHECK_NULL(MOUSE_EVENT_RECORD_dwControlKeyState);
    MOUSE_EVENT_RECORD_dwEventFlags = env->GetFieldID(MOUSE_EVENT_RECORD_Class, "dwEventFlags", "I");
    CHECK_NULL(MOUSE_EVENT_RECORD_dwEventFlags);

    //jdk.internal.org.jline.terminal.impl.jna.win.Kernel32.WINDOW_BUFFER_SIZE_RECORD
    WINDOW_BUFFER_SIZE_RECORD_Class = (jclass) env->NewGlobalRef(env->FindClass("jdk/internal/org/jline/terminal/impl/jna/win/Kernel32$WINDOW_BUFFER_SIZE_RECORD"));
    CHECK_NULL(WINDOW_BUFFER_SIZE_RECORD_Class);
    WINDOW_BUFFER_SIZE_RECORD_Constructor = env->GetMethodID(cls, "<init>", "()V");
    CHECK_NULL(WINDOW_BUFFER_SIZE_RECORD_Constructor);
    WINDOW_BUFFER_SIZE_RECORD_dwSize = env->GetFieldID(WINDOW_BUFFER_SIZE_RECORD_Class, "dwSize", "Ljdk/internal/org/jline/terminal/impl/jna/win/Kernel32$COORD;");
    CHECK_NULL(WINDOW_BUFFER_SIZE_RECORD_dwSize);

    //jdk.internal.org.jline.terminal.impl.jna.win.Kernel32.MENU_EVENT_RECORD
    MENU_EVENT_RECORD_Class = (jclass) env->NewGlobalRef(env->FindClass("jdk/internal/org/jline/terminal/impl/jna/win/Kernel32$MENU_EVENT_RECORD"));
    CHECK_NULL(MENU_EVENT_RECORD_Class);
    MENU_EVENT_RECORD_Constructor = env->GetMethodID(cls, "<init>", "()V");
    CHECK_NULL(MENU_EVENT_RECORD_Constructor);
    MENU_EVENT_RECORD_dwCommandId = env->GetFieldID(MENU_EVENT_RECORD_Class, "dwCommandId", "I");
    CHECK_NULL(MENU_EVENT_RECORD_dwCommandId);

    //jdk.internal.org.jline.terminal.impl.jna.win.Kernel32.FOCUS_EVENT_RECORD
    FOCUS_EVENT_RECORD_Class = (jclass) env->NewGlobalRef(env->FindClass("jdk/internal/org/jline/terminal/impl/jna/win/Kernel32$FOCUS_EVENT_RECORD"));
    CHECK_NULL(FOCUS_EVENT_RECORD_Class);
    FOCUS_EVENT_RECORD_Constructor = env->GetMethodID(cls, "<init>", "()V");
    CHECK_NULL(FOCUS_EVENT_RECORD_Constructor);
    FOCUS_EVENT_RECORD_bSetFocus = env->GetFieldID(FOCUS_EVENT_RECORD_Class, "bSetFocus", "Z");
    CHECK_NULL(FOCUS_EVENT_RECORD_bSetFocus);

    //jdk.internal.org.jline.terminal.impl.jna.win.Kernel32.SMALL_RECT
    SMALL_RECT_Class = (jclass) env->NewGlobalRef(env->FindClass("jdk/internal/org/jline/terminal/impl/jna/win/Kernel32$SMALL_RECT"));
    CHECK_NULL(SMALL_RECT_Class);
    SMALL_RECT_Constructor = env->GetMethodID(cls, "<init>", "()V");
    CHECK_NULL(SMALL_RECT_Constructor);
    SMALL_RECT_Left = env->GetFieldID(SMALL_RECT_Class, "Left", "S");
    CHECK_NULL(SMALL_RECT_Left);
    SMALL_RECT_Top = env->GetFieldID(SMALL_RECT_Class, "Top", "S");
    CHECK_NULL(SMALL_RECT_Top);
    SMALL_RECT_Right = env->GetFieldID(SMALL_RECT_Class, "Right", "S");
    CHECK_NULL(SMALL_RECT_Right);
    SMALL_RECT_Bottom = env->GetFieldID(SMALL_RECT_Class, "Bottom", "S");
    CHECK_NULL(SMALL_RECT_Bottom);

    //jdk.internal.org.jline.terminal.impl.jna.win.Kernel32.UnionChar
    UnionChar_Class = (jclass) env->NewGlobalRef(env->FindClass("jdk/internal/org/jline/terminal/impl/jna/win/Kernel32$UnionChar"));
    CHECK_NULL(UnionChar_Class);
    UnionChar_Constructor = env->GetMethodID(cls, "<init>", "()V");
    CHECK_NULL(UnionChar_Constructor);
    UnionChar_UnicodeChar = env->GetFieldID(UnionChar_Class, "UnicodeChar", "C");
    CHECK_NULL(UnionChar_UnicodeChar);
}

/*
 * Class:     jdk_internal_org_jline_terminal_impl_jna_win_Kernel32Impl
 * Method:    WaitForSingleObject
 * Signature: (Ljdk/internal/org/jline/terminal/impl/jna/win/Pointer;I)I
 */
JNIEXPORT jint JNICALL Java_jdk_internal_org_jline_terminal_impl_jna_win_Kernel32Impl_WaitForSingleObject
  (JNIEnv *env, jobject kernel, jobject in_hHandle, jint in_dwMilliseconds) {
    HANDLE h = GetStdHandle((jint) env->GetLongField(in_hHandle, pointerValue));
    return WaitForSingleObject(h, in_dwMilliseconds);
}

/*
 * Class:     jdk_internal_org_jline_terminal_impl_jna_win_Kernel32Impl
 * Method:    GetStdHandle
 * Signature: (I)Ljdk/internal/org/jline/terminal/impl/jna/win/Pointer;
 */
JNIEXPORT jobject JNICALL Java_jdk_internal_org_jline_terminal_impl_jna_win_Kernel32Impl_GetStdHandle
  (JNIEnv *env, jobject, jint nStdHandle) {
    return env->NewObject(pointerClass,
                          pointerConstructor,
                          nStdHandle);
}

/*
 * Class:     jdk_internal_org_jline_terminal_impl_jna_win_Kernel32Impl
 * Method:    GetConsoleOutputCP
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_jdk_internal_org_jline_terminal_impl_jna_win_Kernel32Impl_GetConsoleOutputCP
  (JNIEnv *, jobject) {
    return GetConsoleOutputCP();
}

/*
 * Class:     jdk_internal_org_jline_terminal_impl_jna_win_Kernel32Impl
 * Method:    FillConsoleOutputCharacter
 * Signature: (Ljdk/internal/org/jline/terminal/impl/jna/win/Pointer;CILjdk/internal/org/jline/terminal/impl/jna/win/Kernel32/COORD;Ljdk/internal/org/jline/terminal/impl/jna/win/IntByReference;)V
 */
JNIEXPORT void JNICALL Java_jdk_internal_org_jline_terminal_impl_jna_win_Kernel32Impl_FillConsoleOutputCharacter
  (JNIEnv *env, jobject kernel, jobject in_hConsoleOutput, jchar in_cCharacter, jint in_nLength, jobject in_dwWriteCoord, jobject out_lpNumberOfCharsWritten) {
    HANDLE h = GetStdHandle((jint) env->GetLongField(in_hConsoleOutput, pointerValue));
    DWORD written;
    COORD coord;
    coord.X = (SHORT) env->GetLongField(in_dwWriteCoord, COORD_X);
    coord.Y = (SHORT) env->GetLongField(in_dwWriteCoord, COORD_Y);
    if (!FillConsoleOutputCharacter(h, (CHAR) in_cCharacter, in_nLength, coord, &written)) {
        DWORD error = GetLastError();
        jobject exc = env->NewObject(lastErrorExceptionClass,
                                     lastErrorExceptionConstructor,
                                     (jlong) error);
        env->Throw((jthrowable) exc);
        return ;
    }
    env->SetIntField(out_lpNumberOfCharsWritten, intByReferenceValue, written);
}

/*
 * Class:     jdk_internal_org_jline_terminal_impl_jna_win_Kernel32Impl
 * Method:    FillConsoleOutputAttribute
 * Signature: (Ljdk/internal/org/jline/terminal/impl/jna/win/Pointer;SILjdk/internal/org/jline/terminal/impl/jna/win/Kernel32/COORD;Ljdk/internal/org/jline/terminal/impl/jna/win/IntByReference;)V
 */
JNIEXPORT void JNICALL Java_jdk_internal_org_jline_terminal_impl_jna_win_Kernel32Impl_FillConsoleOutputAttribute
  (JNIEnv *env, jobject kernel, jobject in_hConsoleOutput, jshort in_wAttribute, jint in_nLength, jobject in_dwWriteCoord, jobject out_lpNumberOfAttrsWritten) {
    HANDLE h = GetStdHandle((jint) env->GetLongField(in_hConsoleOutput, pointerValue));
    DWORD written;
    COORD coord;
    coord.X = (SHORT) env->GetLongField(in_dwWriteCoord, COORD_X);
    coord.Y = (SHORT) env->GetLongField(in_dwWriteCoord, COORD_Y);
    if (!FillConsoleOutputAttribute(h, in_wAttribute, in_nLength, coord, &written)) {
        DWORD error = GetLastError();
        jobject exc = env->NewObject(lastErrorExceptionClass,
                                     lastErrorExceptionConstructor,
                                     (jlong) error);
        env->Throw((jthrowable) exc);
        return ;
    }
    env->SetIntField(out_lpNumberOfAttrsWritten, intByReferenceValue, written);
}

/*
 * Class:     jdk_internal_org_jline_terminal_impl_jna_win_Kernel32Impl
 * Method:    GetConsoleMode
 * Signature: (Ljdk/internal/org/jline/terminal/impl/jna/win/Pointer;Ljdk/internal/org/jline/terminal/impl/jna/win/IntByReference;)V
 */
JNIEXPORT void JNICALL Java_jdk_internal_org_jline_terminal_impl_jna_win_Kernel32Impl_GetConsoleMode
  (JNIEnv *env, jobject, jobject in_hConsoleOutput, jobject out_lpMode) {
    HANDLE h = GetStdHandle((jint) env->GetLongField(in_hConsoleOutput, pointerValue));
    DWORD mode;
    if (!GetConsoleMode(h, &mode)) {
        DWORD error = GetLastError();
        jobject exc = env->NewObject(lastErrorExceptionClass,
                                     lastErrorExceptionConstructor,
                                     (jlong) error);
        env->Throw((jthrowable) exc);
        return ;
    }
    env->SetIntField(out_lpMode, intByReferenceValue, mode);
}

/*
 * Class:     jdk_internal_org_jline_terminal_impl_jna_win_Kernel32Impl
 * Method:    GetConsoleScreenBufferInfo
 * Signature: (Ljdk/internal/org/jline/terminal/impl/jna/win/Pointer;Ljdk/internal/org/jline/terminal/impl/jna/win/Kernel32/CONSOLE_SCREEN_BUFFER_INFO;)V
 */
JNIEXPORT void JNICALL Java_jdk_internal_org_jline_terminal_impl_jna_win_Kernel32Impl_GetConsoleScreenBufferInfo
  (JNIEnv *env, jobject, jobject in_hConsoleOutput, jobject/*CONSOLE_SCREEN_BUFFER_INFO*/ out_lpConsoleScreenBufferInfo) {
    HANDLE h = GetStdHandle((jint) env->GetLongField(in_hConsoleOutput, pointerValue));
    CONSOLE_SCREEN_BUFFER_INFO buffer;
    if (!GetConsoleScreenBufferInfo(h, &buffer)) {
        DWORD error = GetLastError();
        jobject exc = env->NewObject(lastErrorExceptionClass,
                                     lastErrorExceptionConstructor,
                                     (jlong) error);
        env->Throw((jthrowable) exc);
        return ;
    }

    jobject dwSize = env->NewObject(COORD_Class,
                                    COORD_Constructor);
    env->SetIntField(dwSize, COORD_X, buffer.dwSize.X);
    env->SetIntField(dwSize, COORD_Y, buffer.dwSize.Y);
    env->SetObjectField(out_lpConsoleScreenBufferInfo, CONSOLE_SCREEN_BUFFER_INFO_dwSize, dwSize);

    jobject dwCursorPosition = env->NewObject(COORD_Class,
                                              COORD_Constructor);
    env->SetIntField(dwCursorPosition, COORD_X, buffer.dwCursorPosition.X);
    env->SetIntField(dwCursorPosition, COORD_Y, buffer.dwCursorPosition.Y);
    env->SetObjectField(out_lpConsoleScreenBufferInfo, CONSOLE_SCREEN_BUFFER_INFO_dwCursorPosition, dwCursorPosition);

    env->SetIntField(out_lpConsoleScreenBufferInfo, CONSOLE_SCREEN_BUFFER_INFO_wAttributes, buffer.wAttributes);

    jobject srWindow = env->NewObject(SMALL_RECT_Class,
                                      SMALL_RECT_Constructor);
    env->SetIntField(srWindow, SMALL_RECT_Left, buffer.srWindow.Left);
    env->SetIntField(srWindow, SMALL_RECT_Top, buffer.srWindow.Top);
    env->SetIntField(srWindow, SMALL_RECT_Right, buffer.srWindow.Right);
    env->SetIntField(srWindow, SMALL_RECT_Bottom, buffer.srWindow.Bottom);
    env->SetObjectField(out_lpConsoleScreenBufferInfo, CONSOLE_SCREEN_BUFFER_INFO_srWindow, srWindow);

    jobject dwMaximumWindowSize = env->NewObject(COORD_Class,
                                                 COORD_Constructor);
    env->SetIntField(dwMaximumWindowSize, COORD_X, buffer.dwMaximumWindowSize.X);
    env->SetIntField(dwMaximumWindowSize, COORD_Y, buffer.dwMaximumWindowSize.Y);
    env->SetObjectField(out_lpConsoleScreenBufferInfo, CONSOLE_SCREEN_BUFFER_INFO_dwMaximumWindowSize, dwMaximumWindowSize);
}

/*
 * Class:     jdk_internal_org_jline_terminal_impl_jna_win_Kernel32Impl
 * Method:    ReadConsoleInput
 * Signature: (Ljdk/internal/org/jline/terminal/impl/jna/win/Pointer;[Ljdk/internal/org/jline/terminal/impl/jna/win/Kernel32/INPUT_RECORD;ILjdk/internal/org/jline/terminal/impl/jna/win/IntByReference;)V
 */
JNIEXPORT void JNICALL Java_jdk_internal_org_jline_terminal_impl_jna_win_Kernel32Impl_ReadConsoleInput
  (JNIEnv *env, jobject kernel, jobject in_hConsoleOutput, jobjectArray/*INPUT_RECORD[]*/ out_lpBuffer, jint in_nLength, jobject out_lpNumberOfEventsRead) {
    HANDLE h = GetStdHandle((jint) env->GetLongField(in_hConsoleOutput, pointerValue));
    INPUT_RECORD *buffer = new INPUT_RECORD[in_nLength];
    DWORD numberOfEventsRead;
    if (!ReadConsoleInputW(h, buffer, in_nLength, &numberOfEventsRead)) {
        delete buffer;
        DWORD error = GetLastError();
        jobject exc = env->NewObject(lastErrorExceptionClass,
                                     lastErrorExceptionConstructor,
                                     (jlong) error);
        env->Throw((jthrowable) exc);
        return ;
    }
    for (unsigned int i = 0; i < numberOfEventsRead; i++) {
        jobject record = env->NewObject(INPUT_RECORD_Class,
                                        INPUT_RECORD_Constructor);
        env->SetShortField(record, INPUT_RECORD_EventType, buffer[i].EventType);
        switch (buffer[i].EventType) {
            case KEY_EVENT: {
                jobject keyEvent = env->NewObject(KEY_EVENT_RECORD_Class,
                                                  KEY_EVENT_RECORD_Constructor);
                env->SetBooleanField(keyEvent, KEY_EVENT_RECORD_bKeyDown, buffer[i].Event.KeyEvent.bKeyDown);
                env->SetShortField(keyEvent, KEY_EVENT_RECORD_wRepeatCount, buffer[i].Event.KeyEvent.wRepeatCount);
                env->SetShortField(keyEvent, KEY_EVENT_RECORD_wVirtualKeyCode, buffer[i].Event.KeyEvent.wVirtualKeyCode);
                env->SetShortField(keyEvent, KEY_EVENT_RECORD_wVirtualScanCode, buffer[i].Event.KeyEvent.wVirtualScanCode);

                jobject unionChar = env->NewObject(UnionChar_Class,
                                                   UnionChar_Constructor);

                env->SetIntField(unionChar, UnionChar_UnicodeChar, buffer[i].Event.KeyEvent.uChar.UnicodeChar);

                env->SetObjectField(keyEvent, KEY_EVENT_RECORD_uChar, unionChar);

                env->SetIntField(keyEvent, KEY_EVENT_RECORD_dwControlKeyState, buffer[i].Event.KeyEvent.dwControlKeyState);

                jobject event = env->NewObject(EventUnion_Class,
                                               EventUnion_Constructor);

                env->SetObjectField(event, EventUnion_KeyEvent, keyEvent);
                env->SetObjectField(record, INPUT_RECORD_Event, event);
                break;
            }
            case MOUSE_EVENT: {
                jobject mouseEvent = env->NewObject(MOUSE_EVENT_RECORD_Class,
                                                    MOUSE_EVENT_RECORD_Constructor);

                jobject dwMousePosition = env->NewObject(COORD_Class,
                                                         COORD_Constructor);
                env->SetIntField(dwMousePosition, COORD_X, buffer[i].Event.MouseEvent.dwMousePosition.X);
                env->SetIntField(dwMousePosition, COORD_Y, buffer[i].Event.MouseEvent.dwMousePosition.Y);
                env->SetObjectField(mouseEvent, MOUSE_EVENT_RECORD_dwMousePosition, dwMousePosition);
                env->SetIntField(mouseEvent, MOUSE_EVENT_RECORD_dwButtonState, buffer[i].Event.MouseEvent.dwButtonState);
                env->SetIntField(mouseEvent, MOUSE_EVENT_RECORD_dwControlKeyState, buffer[i].Event.MouseEvent.dwControlKeyState);
                env->SetIntField(mouseEvent, MOUSE_EVENT_RECORD_dwEventFlags, buffer[i].Event.MouseEvent.dwEventFlags);

                jobject event = env->NewObject(EventUnion_Class,
                                               EventUnion_Constructor);

                env->SetObjectField(event, EventUnion_MouseEvent, mouseEvent);
                env->SetObjectField(record, INPUT_RECORD_Event, event);
                break;
            }
            case WINDOW_BUFFER_SIZE_EVENT: {
                jobject windowBufferSize = env->NewObject(WINDOW_BUFFER_SIZE_RECORD_Class,
                                                          WINDOW_BUFFER_SIZE_RECORD_Constructor);

                jobject dwSize = env->NewObject(COORD_Class,
                                                COORD_Constructor);
                env->SetIntField(dwSize, COORD_X, buffer[i].Event.WindowBufferSizeEvent.dwSize.X);
                env->SetIntField(dwSize, COORD_Y, buffer[i].Event.WindowBufferSizeEvent.dwSize.Y);
                env->SetObjectField(windowBufferSize, WINDOW_BUFFER_SIZE_RECORD_dwSize, dwSize);

                jobject event = env->NewObject(EventUnion_Class,
                                               EventUnion_Constructor);

                env->SetObjectField(event, EventUnion_WindowBufferSizeEvent, windowBufferSize);
                env->SetObjectField(record, INPUT_RECORD_Event, event);
                break;
            }
            case MENU_EVENT: {
                jobject menuEvent = env->NewObject(MENU_EVENT_RECORD_Class,
                                                          MENU_EVENT_RECORD_Constructor);

                env->SetBooleanField(menuEvent, MENU_EVENT_RECORD_dwCommandId, buffer[i].Event.MenuEvent.dwCommandId);

                jobject event = env->NewObject(EventUnion_Class,
                                               EventUnion_Constructor);

                env->SetObjectField(event, EventUnion_MenuEvent, menuEvent);
                env->SetObjectField(record, INPUT_RECORD_Event, event);
                break;
            }
            case FOCUS_EVENT: {
                jobject focusEvent = env->NewObject(FOCUS_EVENT_RECORD_Class,
                                                    FOCUS_EVENT_RECORD_Constructor);

                env->SetIntField(focusEvent, FOCUS_EVENT_RECORD_bSetFocus, buffer[i].Event.FocusEvent.bSetFocus);

                jobject event = env->NewObject(EventUnion_Class,
                                               EventUnion_Constructor);

                env->SetObjectField(event, EventUnion_FocusEvent, focusEvent);
                env->SetObjectField(record, INPUT_RECORD_Event, event);
                break;
            }
        }
        env->SetObjectArrayElement(out_lpBuffer, i, record);
    }
    env->SetIntField(out_lpNumberOfEventsRead, intByReferenceValue, numberOfEventsRead);
    delete buffer;
}

/*
 * Class:     jdk_internal_org_jline_terminal_impl_jna_win_Kernel32Impl
 * Method:    SetConsoleCursorPosition
 * Signature: (Ljdk/internal/org/jline/terminal/impl/jna/win/Pointer;Ljdk/internal/org/jline/terminal/impl/jna/win/Kernel32/COORD;)V
 */
JNIEXPORT void JNICALL Java_jdk_internal_org_jline_terminal_impl_jna_win_Kernel32Impl_SetConsoleCursorPosition
  (JNIEnv *env, jobject kernel, jobject in_hConsoleOutput, jobject in_dwCursorPosition) {
    HANDLE h = GetStdHandle((jint) env->GetLongField(in_hConsoleOutput, pointerValue));
    COORD coord;
    coord.X = (SHORT) env->GetLongField(in_dwCursorPosition, COORD_X);
    coord.Y = (SHORT) env->GetLongField(in_dwCursorPosition, COORD_Y);
    if (!SetConsoleCursorPosition(h, coord)) {
        DWORD error = GetLastError();
        jobject exc = env->NewObject(lastErrorExceptionClass,
                                     lastErrorExceptionConstructor,
                                     (jlong) error);
        env->Throw((jthrowable) exc);
        return;
    }
}

/*
 * Class:     jdk_internal_org_jline_terminal_impl_jna_win_Kernel32Impl
 * Method:    SetConsoleMode
 * Signature: (Ljdk/internal/org/jline/terminal/impl/jna/win/Pointer;I)V
 */
JNIEXPORT void JNICALL Java_jdk_internal_org_jline_terminal_impl_jna_win_Kernel32Impl_SetConsoleMode
  (JNIEnv *env, jobject kernel, jobject in_hConsoleOutput, jint in_dwMode) {
    HANDLE h = GetStdHandle((jint) env->GetLongField(in_hConsoleOutput, pointerValue));
    if (!SetConsoleMode(h, in_dwMode)) {
        DWORD error = GetLastError();
        jobject exc = env->NewObject(lastErrorExceptionClass,
                                     lastErrorExceptionConstructor,
                                     (jlong) error);
        env->Throw((jthrowable) exc);
        return ;
    }
}

/*
 * Class:     jdk_internal_org_jline_terminal_impl_jna_win_Kernel32Impl
 * Method:    SetConsoleTextAttribute
 * Signature: (Ljdk/internal/org/jline/terminal/impl/jna/win/Pointer;S)V
 */
JNIEXPORT void JNICALL Java_jdk_internal_org_jline_terminal_impl_jna_win_Kernel32Impl_SetConsoleTextAttribute
  (JNIEnv *env, jobject kernel, jobject in_hConsoleOutput, jshort in_wAttributes) {
    HANDLE h = GetStdHandle((jint) env->GetLongField(in_hConsoleOutput, pointerValue));
    if (!SetConsoleTextAttribute(h, in_wAttributes)) {
        DWORD error = GetLastError();
        jobject exc = env->NewObject(lastErrorExceptionClass,
                                     lastErrorExceptionConstructor,
                                     (jlong) error);
        env->Throw((jthrowable) exc);
        return ;
    }
}

/*
 * Class:     jdk_internal_org_jline_terminal_impl_jna_win_Kernel32Impl
 * Method:    SetConsoleTitle
 * Signature: (Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_jdk_internal_org_jline_terminal_impl_jna_win_Kernel32Impl_SetConsoleTitle
  (JNIEnv *env, jobject, jstring in_lpConsoleTitle) {
    const char *chars = env->GetStringUTFChars(in_lpConsoleTitle, NULL);
    if (!SetConsoleTitle(chars)) {
        env->ReleaseStringUTFChars(in_lpConsoleTitle, chars);
        DWORD error = GetLastError();
        jobject exc = env->NewObject(lastErrorExceptionClass,
                                     lastErrorExceptionConstructor,
                                     (jlong) error);
        env->Throw((jthrowable) exc);
        return ;
    }
    env->ReleaseStringUTFChars(in_lpConsoleTitle, chars);
}

/*
 * Class:     jdk_internal_org_jline_terminal_impl_jna_win_Kernel32Impl
 * Method:    WriteConsoleW
 * Signature: (Ljdk/internal/org/jline/terminal/impl/jna/win/Pointer;[CILjdk/internal/org/jline/terminal/impl/jna/win/IntByReference;Ljdk/internal/org/jline/terminal/impl/jna/win/Pointer;)V
 */
JNIEXPORT void JNICALL Java_jdk_internal_org_jline_terminal_impl_jna_win_Kernel32Impl_WriteConsoleW
  (JNIEnv *env, jobject kernel, jobject in_hConsoleOutput, jcharArray in_lpBuffer, jint in_nNumberOfCharsToWrite, jobject out_lpNumberOfCharsWritten, jobject) {
    HANDLE h = GetStdHandle((jint) env->GetLongField(in_hConsoleOutput, pointerValue));
    jchar *chars = new jchar[in_nNumberOfCharsToWrite];
    env->GetCharArrayRegion(in_lpBuffer, 0, in_nNumberOfCharsToWrite, chars);
    DWORD written;
    if (!WriteConsoleW(h, chars, in_nNumberOfCharsToWrite, &written, NULL)) {
        delete chars;
        DWORD error = GetLastError();
        jobject exc = env->NewObject(lastErrorExceptionClass,
                                     lastErrorExceptionConstructor,
                                     (jlong) error);
        env->Throw((jthrowable) exc);
        return ;
    }

    env->SetIntField(out_lpNumberOfCharsWritten, intByReferenceValue, written);
    delete chars;
}

/*
 * Class:     jdk_internal_org_jline_terminal_impl_jna_win_Kernel32Impl
 * Method:    ScrollConsoleScreenBuffer
 * Signature: (Ljdk/internal/org/jline/terminal/impl/jna/win/Pointer;Ljdk/internal/org/jline/terminal/impl/jna/win/Kernel32/SMALL_RECT;Ljdk/internal/org/jline/terminal/impl/jna/win/Kernel32/SMALL_RECT;Ljdk/internal/org/jline/terminal/impl/jna/win/Kernel32/COORD;Ljdk/internal/org/jline/terminal/impl/jna/win/Kernel32/CHAR_INFO;)V
 */
JNIEXPORT void JNICALL Java_jdk_internal_org_jline_terminal_impl_jna_win_Kernel32Impl_ScrollConsoleScreenBuffer
  (JNIEnv *env, jobject kernel, jobject in_hConsoleOutput, jobject in_lpScrollRectangle, jobject in_lpClipRectangle, jobject in_dwDestinationOrigin, jobject in_lpFill) {
    HANDLE h = GetStdHandle((jint) env->GetLongField(in_hConsoleOutput, pointerValue));

    SMALL_RECT scrollRectangle;
    scrollRectangle.Left = (SHORT) env->GetLongField(in_lpScrollRectangle, SMALL_RECT_Left);
    scrollRectangle.Top = (SHORT) env->GetLongField(in_lpScrollRectangle, SMALL_RECT_Top);
    scrollRectangle.Right = (SHORT) env->GetLongField(in_lpScrollRectangle, SMALL_RECT_Right);
    scrollRectangle.Bottom = (SHORT) env->GetLongField(in_lpScrollRectangle, SMALL_RECT_Bottom);

    SMALL_RECT clipRectangle;
    clipRectangle.Left = (SHORT) env->GetLongField(in_lpClipRectangle, SMALL_RECT_Left);
    clipRectangle.Top = (SHORT) env->GetLongField(in_lpClipRectangle, SMALL_RECT_Top);
    clipRectangle.Right = (SHORT) env->GetLongField(in_lpClipRectangle, SMALL_RECT_Right);
    clipRectangle.Bottom = (SHORT) env->GetLongField(in_lpClipRectangle, SMALL_RECT_Bottom);

    COORD destinationOrigin;
    destinationOrigin.X = (SHORT) env->GetLongField(in_dwDestinationOrigin, COORD_X);
    destinationOrigin.Y = (SHORT) env->GetLongField(in_dwDestinationOrigin, COORD_Y);

    CHAR_INFO charInfo;
    charInfo.Char.UnicodeChar = env->GetCharField(env->GetObjectField(in_lpFill, CHAR_INFO_uChar), UnionChar_UnicodeChar);
    charInfo.Attributes = env->GetShortField(in_lpFill, CHAR_INFO_Attributes);

    if (!ScrollConsoleScreenBuffer(h, &scrollRectangle, &clipRectangle, destinationOrigin, &charInfo)) {
        DWORD error = GetLastError();
        jobject exc = env->NewObject(lastErrorExceptionClass,
                                     lastErrorExceptionConstructor,
                                     (jlong) error);
        env->Throw((jthrowable) exc);
        return ;
    }
}
