/*
 * Copyright (c) 2004, Oracle and/or its affiliates. All rights reserved.
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
#include <windows.h>
#include <stdlib.h>
#include <stdio.h>


/*
 * Convert UTF-8 to a platform string
 */
int
convertUft8ToPlatformString(char* utf8_str, int utf8_len, char* platform_str, int platform_len) {
    LANGID langID;
    LCID localeID;
    TCHAR strCodePage[7];       // ANSI code page id
    UINT codePage;
    int wlen, plen;
    WCHAR* wstr;

    /*
     * Get the code page for this locale
     */
    langID = LANGIDFROMLCID(GetUserDefaultLCID());
    localeID = MAKELCID(langID, SORT_DEFAULT);
    if (GetLocaleInfo(localeID, LOCALE_IDEFAULTANSICODEPAGE,
                      strCodePage, sizeof(strCodePage)/sizeof(TCHAR)) > 0 ) {
        codePage = atoi(strCodePage);
    } else {
        codePage = GetACP();
    }

    /*
     * To convert the string to platform encoding we must first convert
     * to unicode, and then convert to the platform encoding
     */
    plen = -1;
    wlen = MultiByteToWideChar(CP_UTF8, 0, utf8_str, utf8_len, NULL, 0);
    if (wlen > 0) {
        wstr = (WCHAR*)malloc(wlen * sizeof(WCHAR));
        if (wstr != NULL) {
            if (MultiByteToWideChar(CP_UTF8,
                                    0,
                                    utf8_str,
                                    utf8_len,
                                    wstr, wlen) > 0) {
                plen = WideCharToMultiByte(codePage,
                                           0,
                                           wstr,
                                           wlen,
                                           platform_str,
                                           platform_len,
                                           NULL,
                                           NULL);
                if (plen >= 0) {
                    platform_str[plen] = '\0';
                }
                free(wstr);
            }
        }
    }
    return plen;
}
