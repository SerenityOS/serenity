/*
 * Copyright (c) 2005, Oracle and/or its affiliates. All rights reserved.
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

#ifndef AWT_INPUTTEXTINFOR_H
#define AWT_INPUTTEXTINFOR_H

/***************************************************************
 * AwtInputTextInfor
 *
 * A class encapsulating the composition string and result string
 * used in windows input method implementation.
 *
 */
#include <windows.h>
#include <imm.h>
#include <jni.h>

class AwtInputTextInfor {
 public:
    /* Default constructor provided just for the clients who
       want to use the SendInputMethodEvent service.
    */
    AwtInputTextInfor();

    int GetContextData(HIMC hIMC, const LPARAM flags);

    int GetCursorPosition() const;

    int GetCommittedTextLength() const;

    jstring GetText() const { return m_jtext; }

    int GetClauseInfor(int*& lpBndClauseW, jstring*& lpReadingClauseW);
    int GetAttributeInfor(int*& lpBndAttrW, BYTE*& lpValAttrW);

    ~AwtInputTextInfor();
 private:
    /* helper function to return a java string.*/
    static jstring MakeJavaString(JNIEnv* env, LPWSTR lpStrW, int cStrW);


    LPARAM m_flags;            /* The message LPARAM. */
    int m_cursorPosW;          /* the current cursor position of composition string */
    jstring m_jtext;           /* Composing string/result string or merged one */
    AwtInputTextInfor* m_pResultTextInfor; /* pointer to result string */

    int m_cStrW;            /* size of the current composition/result string */
    int m_cReadStrW;        /* size of the reading string */
    int m_cClauseW;         /* size of the clause */
    int m_cReadClauseW;     /* size of the read clause */
    int m_cAttrW;           /* size of the attribute (composition only) */

    LPWSTR  m_lpStrW;       /* pointer to the current composition/result string */
    LPWSTR  m_lpReadStrW;   /* pointer to the reading string */
    LPDWORD m_lpClauseW;    /* pointer to the clause information */
    LPDWORD m_lpReadClauseW;/* pointer to the reading clause information */
    LPBYTE  m_lpAttrW;      /* pointer to the attribute information (composition only) */

    /* GCS_XXX index for result string */
    static const DWORD GCS_INDEX[9];
};

#endif // AWT_INPUTTEXTINFOR_H
