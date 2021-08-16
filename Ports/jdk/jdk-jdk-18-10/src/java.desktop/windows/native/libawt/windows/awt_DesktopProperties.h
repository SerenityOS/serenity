/*
 * Copyright (c) 1999, 2012, Oracle and/or its affiliates. All rights reserved.
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

#ifndef AWT_DESKTOP_PROPERTIES_H
#define AWT_DESKTOP_PROPERTIES_H

#include "awt.h"
#include "jni.h"

class AwtDesktopProperties {
    public:
        enum {
            MAX_PROPERTIES = 100,
            AWT_DESKTOP_PROPERTIES_1_3 = 1, // properties version for Java 2 SDK 1.3
            // NOTE: MUST INCREMENT this whenever you add new
            // properties for a given public release
            AWT_DESKTOP_PROPERTIES_1_4 = 2, // properties version for Java 2 SDK 1.4
            AWT_DESKTOP_PROPERTIES_1_5 = 3, // properties version for Java 2 SDK 1.5
            AWT_DESKTOP_PROPERTIES_VERSION = AWT_DESKTOP_PROPERTIES_1_5
        };

        AwtDesktopProperties(jobject self);
        ~AwtDesktopProperties();

        void GetWindowsParameters();
        void PlayWindowsSound(LPCTSTR eventName);
        static BOOL IsXPStyle();

        static jfieldID pDataID;
        static jmethodID setStringPropertyID;
        static jmethodID setIntegerPropertyID;
        static jmethodID setBooleanPropertyID;
        static jmethodID setColorPropertyID;
        static jmethodID setFontPropertyID;
        static jmethodID setSoundPropertyID;

    private:
        void GetXPStyleProperties();
        void GetSystemProperties();
        void GetNonClientParameters();
        void GetIconParameters();
        void GetColorParameters();
        void GetOtherParameters();
        void GetSoundEvents();
        void GetCaretParameters();

        static BOOL GetBooleanParameter(UINT spi);
        static UINT GetIntegerParameter(UINT spi);

        void SetBooleanProperty(LPCTSTR, BOOL);
        void SetIntegerProperty(LPCTSTR, int);
        void SetStringProperty(LPCTSTR, LPTSTR);
        void SetColorProperty(LPCTSTR, DWORD);
        void SetFontProperty(HDC, int, LPCTSTR, float invScale);
        void SetFontProperty(LPCTSTR, const LOGFONT &, float invScale);
        void SetSoundProperty(LPCTSTR, LPCTSTR);

        JNIEnv * GetEnv() {
            return (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);
        }

        jobject         self;
};

#endif // AWT_DESKTOP_PROPERTIES_H
