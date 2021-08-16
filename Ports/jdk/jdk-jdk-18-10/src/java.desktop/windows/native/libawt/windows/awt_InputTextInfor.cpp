/*
 * Copyright (c) 2005, 2017, Oracle and/or its affiliates. All rights reserved.
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

#include "awt.h"
#include <imm.h>
#include "awt_Component.h"
#include "awt_InputTextInfor.h"

#define WCHAR_SZ sizeof(WCHAR)
#define DWORD_SZ sizeof(DWORD)

// The start and end index of the result and composition in GCS_INDEX array.
#define START_RESULTSTR 0
#define END_RESULTSTR 3
#define START_COMPSTR 4
#define END_COMPSTR 8

// The GCS_INDEX array is partitioned into 2 parts, one is result string related and the
// other is composing string related.
const DWORD AwtInputTextInfor::GCS_INDEX[9]= {GCS_RESULTSTR, GCS_RESULTREADSTR, GCS_RESULTCLAUSE,
                                              GCS_RESULTREADCLAUSE, GCS_COMPSTR, GCS_COMPREADSTR,
                                              GCS_COMPCLAUSE, GCS_COMPREADCLAUSE,GCS_COMPATTR};
/* Default constructor */
AwtInputTextInfor::AwtInputTextInfor() :
    m_flags(0), m_cursorPosW(0), m_jtext(NULL), m_pResultTextInfor(NULL), \
    m_cStrW(0), m_cReadStrW(0), m_cClauseW(0), m_cReadClauseW(0), m_cAttrW(0), \
    m_lpStrW(NULL), m_lpReadStrW(NULL), m_lpClauseW(NULL), m_lpReadClauseW(NULL), m_lpAttrW(NULL)
{}


/* Retrieve the context data from the current IMC.
   Params:
   HIMC hIMC - the input method context, must NOT be NULL
   LPARAMS flags - message param to WM_IME_COMPOSITION.
   Returns 0 if success.
*/
int
AwtInputTextInfor::GetContextData(HIMC hIMC, const LPARAM flags) {

    DASSERT(hIMC != 0);

    m_flags = flags;
    // Based on different flags received, we use different GCS_XXX from the
    // GCS_INDEX array.
    int startIndex = 0, endIndex = 0;

    if (flags & GCS_COMPSTR) {
        startIndex = START_COMPSTR;
        endIndex = END_COMPSTR;
        /* For some window input method such as Chinese QuanPing, when the user
         * commits some text, the IMM sends WM_IME_COMPOSITION with GCS_COMPSTR/GCS_RESULTSTR.
         * So we have to extract the result string from IMC. For most of other cases,
         * m_pResultTextInfor is NULL and this is why we choose to have a pointer as its member
         * rather than having a list of the result string information.
         */
        if (flags & GCS_RESULTSTR) {
            m_pResultTextInfor = new AwtInputTextInfor;
            m_pResultTextInfor->GetContextData(hIMC, GCS_RESULTSTR);
        }
    } else if (flags & GCS_RESULTSTR) {
        startIndex = START_RESULTSTR;
        endIndex = END_RESULTSTR;
    } else { // unknown flags.
        return -1;
    }

    /* Get the data from the input context */
    LONG   cbData[5] = {0};
    LPVOID lpData[5] = {NULL};
    for (int i = startIndex, j = 0; i <= endIndex; i++, j++) {
        cbData[j] = ::ImmGetCompositionString(hIMC, GCS_INDEX[i], NULL, 0);
        if (cbData[j] == 0) {
            lpData[j] = NULL;
        } else {
            LPBYTE lpTemp = new BYTE[cbData[j]];
            cbData[j] = ::ImmGetCompositionString(hIMC, GCS_INDEX[i], lpTemp, cbData[j]);
            if (IMM_ERROR_GENERAL != cbData[j]) {
                lpData[j] = (LPVOID)lpTemp;
            } else {
                lpData[j] = NULL;
                return -1;
            }
        }
    }

    // Assign the context data
    m_cStrW = cbData[0]/WCHAR_SZ;
    m_lpStrW = (LPWSTR)lpData[0];

    m_cReadStrW = cbData[1]/WCHAR_SZ;
    m_lpReadStrW = (LPWSTR)lpData[1];

    m_cClauseW = cbData[2]/DWORD_SZ - 1;
    m_lpClauseW = (LPDWORD)lpData[2];

    m_cReadClauseW = cbData[3]/DWORD_SZ - 1;
    m_lpReadClauseW = (LPDWORD)lpData[3];

    if (cbData[4] > 0) {
        m_cAttrW = cbData[4];
        m_lpAttrW = (LPBYTE)lpData[4];
    }

    // Get the cursor position
    if (flags & GCS_COMPSTR) {
        m_cursorPosW = ::ImmGetCompositionString(hIMC, GCS_CURSORPOS,
                                                NULL, 0);
    }

    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);
    if (m_cStrW > 0) {
        m_jtext = MakeJavaString(env, m_lpStrW, m_cStrW);
        JNU_CHECK_EXCEPTION_RETURN(env, -1);
    }

    // Merge the string if necessary
    if (m_pResultTextInfor != NULL) {
        jstring jresultText = m_pResultTextInfor->GetText();
        if (m_jtext != NULL && jresultText != NULL) {
            jstring jMergedtext = (jstring)JNU_CallMethodByName(env, NULL, jresultText,
                                                                "concat",
                                                                "(Ljava/lang/String;)Ljava/lang/String;",
                                                                m_jtext).l;
            DASSERT(!safe_ExceptionOccurred(env));
            DASSERT(jMergedtext != NULL);

            env->DeleteLocalRef(m_jtext);
            m_jtext = jMergedtext;
        }
        else if (m_jtext == NULL && jresultText != NULL) {
            /* No composing text, assign the committed text to m_jtext */
            m_jtext = (jstring)env->NewLocalRef(jresultText);
        }
    }

    return 0;
}

/*
 * Destructor
 * free the pointer in the m_lpInfoStrW array
 */
AwtInputTextInfor::~AwtInputTextInfor() {

    if (m_jtext) {
        JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);
        env->DeleteLocalRef(m_jtext);
        m_jtext = NULL;
    }

    delete [] m_lpStrW;
    delete [] m_lpReadStrW;
    delete [] m_lpClauseW;
    delete [] m_lpReadClauseW;
    delete [] m_lpAttrW;

    if (m_pResultTextInfor) {
        delete m_pResultTextInfor;
        m_pResultTextInfor = NULL;
    }
}


jstring AwtInputTextInfor::MakeJavaString(JNIEnv* env, LPWSTR lpStrW, int cStrW) {

    if (env == NULL || lpStrW == NULL || cStrW == 0) {
        return NULL;
    } else {
        return env->NewString(reinterpret_cast<jchar*>(lpStrW), cStrW);
    }
}

//
//  Convert Clause and Reading Information for DBCS string to that for Unicode string
//  *lpBndClauseW and *lpReadingClauseW  must be deleted by caller.
//
int AwtInputTextInfor::GetClauseInfor(int*& lpBndClauseW, jstring*& lpReadingClauseW) {

    if ( m_cStrW ==0 || m_cClauseW ==0 || m_cClauseW != m_cReadClauseW ||
         m_lpClauseW == NULL || m_lpReadClauseW == NULL ||
         m_lpClauseW[0] != 0 || m_lpClauseW[m_cClauseW] != (DWORD)m_cStrW ||
         m_lpReadClauseW[0] != 0 || m_lpReadClauseW[m_cReadClauseW] != (DWORD)m_cReadStrW) {
        // For cases where IMM sends WM_IME_COMPOSITION with both GCS_COMPSTR and GCS_RESULTSTR
        // The GCS_RESULTSTR part may have Caluse and Reading information which should not be ignored
        if (NULL == m_pResultTextInfor) {
            lpBndClauseW = NULL;
            lpReadingClauseW = NULL;
            return 0;
        } else {
            return m_pResultTextInfor->GetClauseInfor(lpBndClauseW, lpReadingClauseW);
        }
    }

    int*    bndClauseW = NULL;
    jstring* readingClauseW = NULL;

    //Convert ANSI string caluse information to UNICODE string clause information.
    try {
        bndClauseW = new int[m_cClauseW + 1];
        readingClauseW = new jstring[m_cClauseW];
    } catch (std::bad_alloc&) {
        lpBndClauseW = NULL;
        lpReadingClauseW = NULL;
        delete [] bndClauseW;
        throw;
    }

    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);

    for ( int cls = 0; cls < m_cClauseW; cls++ ) {
        bndClauseW[cls] = m_lpClauseW[cls];

        if ( m_lpReadClauseW[cls + 1] <= (DWORD)m_cReadStrW ) {
            LPWSTR lpHWStrW = m_lpReadStrW + m_lpReadClauseW[cls];
            int cHWStrW = m_lpReadClauseW[cls+1] - m_lpReadClauseW[cls];

            if (PRIMARYLANGID(AwtComponent::GetInputLanguage()) == LANG_JAPANESE) {
                LCID lcJPN = MAKELCID(MAKELANGID(LANG_JAPANESE,SUBLANG_DEFAULT),SORT_DEFAULT);
                // Reading string is given in half width katakana in Japanese Windows
                //  Convert it to full width katakana.
                int cFWStrW = ::LCMapString(lcJPN, LCMAP_FULLWIDTH, lpHWStrW, cHWStrW, NULL, 0);
                LPWSTR lpFWStrW;
                try {
                    lpFWStrW = new WCHAR[cFWStrW];
                } catch (std::bad_alloc&) {
                    lpBndClauseW = NULL;
                    lpReadingClauseW = NULL;
                    delete [] bndClauseW;
                    delete [] readingClauseW;
                    throw;
                }

                ::LCMapString(lcJPN, LCMAP_FULLWIDTH, lpHWStrW, cHWStrW, lpFWStrW, cFWStrW);
                readingClauseW[cls] = MakeJavaString(env, lpFWStrW, cFWStrW);
                delete [] lpFWStrW;
            } else {
                readingClauseW[cls] = MakeJavaString(env, lpHWStrW, cHWStrW);
            }
            if (env->ExceptionCheck()) {
                lpBndClauseW = NULL;
                lpReadingClauseW = NULL;
                delete [] bndClauseW;
                delete [] readingClauseW;
                return 0;
            }
        }
        else {
            readingClauseW[cls] = NULL;
        }
    }

    bndClauseW[m_cClauseW] = m_cStrW;

    int retVal = 0;
    int cCommittedStrW = GetCommittedTextLength();

    /* The conditions to merge the clause information are described below:
       Senario 1:
       m_flags & GCS_RESULTSTR is true only, this case m_pResultTextInfor must be NULL.
       No need to merge.

       Senario 2:
       m_flags & GCS_COMPSTR is true only, this case m_pResultTextInfor is also NULL.
       No need to merge either.

       Senario 3:
       m_flags & GCS_COMPSTR and m_flags & GCS_RESULTSTR both yield to true, in this case
       m_pResultTextInfor won't be NULL and if there is nothing to commit though, we don't
       have to merge. Or if the current composing string size is 0, we don't have to merge either.

       So in clusion, the three conditions not not merge are:
       1. no committed string
       2. m_pResultTextInfor points to NULL
       3. the current string size is 0;

       Same rule applies to merge the attribute information.
    */
    if (m_cStrW == 0 || cCommittedStrW == 0 ||
        m_pResultTextInfor == NULL) {
        lpBndClauseW = bndClauseW;
        lpReadingClauseW = readingClauseW;
        retVal = m_cClauseW;
    } else { /* partial commit case */
        int* bndResultClauseW = NULL;
        jstring* readingResultClauseW = NULL;
        int cResultClauseW = m_pResultTextInfor->GetClauseInfor(bndResultClauseW, readingResultClauseW);

        // Concatenate Clause information.
        int cMergedClauseW = m_cClauseW + cResultClauseW;
        int* bndMergedClauseW = NULL;
        jstring* readingMergedClauseW = NULL;
        try {
            bndMergedClauseW = new int[cMergedClauseW+1];
            readingMergedClauseW = new jstring[cMergedClauseW];
        } catch (std::bad_alloc&) {
            delete [] bndMergedClauseW;
            delete [] bndClauseW;
            delete [] readingClauseW;
            throw;
        }

        int i = 0;
        if (cResultClauseW > 0 && bndResultClauseW && readingResultClauseW) {
            for (; i < cResultClauseW; i++) {
                bndMergedClauseW[i] = bndResultClauseW[i];
                readingMergedClauseW[i] = readingResultClauseW[i];
            }
        }

        if (m_cClauseW > 0 && bndClauseW && readingClauseW) {
            for(int j = 0; j < m_cClauseW; j++, i++) {
                bndMergedClauseW[i] = bndClauseW[j] + cCommittedStrW;
                readingMergedClauseW[i] = readingClauseW[j];
            }
        }
        delete [] bndClauseW;
        delete [] readingClauseW;
        bndMergedClauseW[cMergedClauseW] = m_cStrW + cCommittedStrW;
        lpBndClauseW = bndMergedClauseW;
        lpReadingClauseW = readingMergedClauseW;
        retVal = cMergedClauseW;
    }

    return retVal;
}

//
//  Convert Attribute Information for DBCS string to that for Unicode string
//  *lpBndAttrW and *lpValAttrW  must be deleted by caller.
//
int AwtInputTextInfor::GetAttributeInfor(int*& lpBndAttrW, BYTE*& lpValAttrW) {
    if (m_cStrW == 0 || m_cAttrW != m_cStrW) {
        if (NULL == m_pResultTextInfor) {
            lpBndAttrW = NULL;
            lpValAttrW = NULL;

            return 0;
        } else {
            return m_pResultTextInfor->GetAttributeInfor(lpBndAttrW, lpValAttrW);
        }
    }

    int* bndAttrW = NULL;
    BYTE* valAttrW = NULL;

    //Scan attribute byte array and make attribute run information.
    try {
        bndAttrW = new int[m_cAttrW + 1];
        valAttrW = new BYTE[m_cAttrW];
    } catch (std::bad_alloc&) {
        lpBndAttrW = NULL;
        lpValAttrW = NULL;
        delete [] bndAttrW;
        throw;
    }

    int cAttrWT = 0;
    bndAttrW[0] = 0;
    valAttrW[0] = m_lpAttrW[0];
    /* remove duplicate attribute in the m_lpAttrW array. */
    for ( int offW = 1; offW < m_cAttrW; offW++ ) {
        if ( m_lpAttrW[offW] != valAttrW[cAttrWT]) {
            cAttrWT++;
            bndAttrW[cAttrWT] = offW;
            valAttrW[cAttrWT] = m_lpAttrW[offW];
        }
    }
    bndAttrW[++cAttrWT] =  m_cStrW;

    int retVal = 0;

    int cCommittedStrW = GetCommittedTextLength();
    if (m_cStrW == 0 ||
        cCommittedStrW == 0 || m_pResultTextInfor == NULL) {
        lpBndAttrW = bndAttrW;
        lpValAttrW = valAttrW;
        retVal = cAttrWT;
    } else {
        int cMergedAttrW = 1 + cAttrWT;
        int*    bndMergedAttrW = NULL;
        BYTE*   valMergedAttrW = NULL;
        try {
            bndMergedAttrW = new int[cMergedAttrW+1];
            valMergedAttrW = new BYTE[cMergedAttrW];
        } catch (std::bad_alloc&) {
            delete [] bndMergedAttrW;
            delete [] bndAttrW;
            delete [] valAttrW;
            throw;
        }
        bndMergedAttrW[0] = 0;
        valMergedAttrW[0] = ATTR_CONVERTED;
        for (int j = 0; j < cAttrWT; j++) {
            bndMergedAttrW[j+1] = bndAttrW[j]+cCommittedStrW;
            valMergedAttrW[j+1] = valAttrW[j];
        }
        bndMergedAttrW[cMergedAttrW] = m_cStrW + cCommittedStrW;

        delete [] bndAttrW;
        delete [] valAttrW;
        lpBndAttrW = bndMergedAttrW;
        lpValAttrW = valMergedAttrW;
        retVal = cMergedAttrW;
    }

    return retVal;
}

//
// Returns the cursor position of the current composition.
// returns 0 if the current mode is not GCS_COMPSTR
//
int AwtInputTextInfor::GetCursorPosition() const {
    if (m_flags & GCS_COMPSTR) {
        return m_cursorPosW;
    } else {
        return 0;
    }
}


//
// Returns the committed text length
//
int AwtInputTextInfor::GetCommittedTextLength() const {

    if ((m_flags & GCS_COMPSTR) && m_pResultTextInfor) {
        return m_pResultTextInfor->GetCommittedTextLength();
    }

    if (m_flags & GCS_RESULTSTR)
        return m_cStrW;
    else
        return 0;
}
