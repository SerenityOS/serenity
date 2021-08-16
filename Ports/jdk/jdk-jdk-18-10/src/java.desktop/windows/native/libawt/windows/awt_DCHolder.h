/*
 * Copyright (c) 2009, Oracle and/or its affiliates. All rights reserved.
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

#ifndef _AWT_DCHolder_H
#define _AWT_DCHolder_H

struct DCHolder
{
    HDC m_hMemoryDC;
    int m_iWidth;
    int m_iHeight;
    BOOL m_bForImage;
    HBITMAP m_hBitmap;
    HBITMAP m_hOldBitmap;
    void *m_pPoints;

    DCHolder();
    ~DCHolder();

    void Create(
        HDC hRelDC,
        int iWidth,
        int iHeght,
        BOOL bForImage);

    operator HDC()
    {
        if (NULL == m_hOldBitmap && NULL != m_hBitmap) {
            m_hOldBitmap = (HBITMAP)::SelectObject(m_hMemoryDC, m_hBitmap);
        }
        return m_hMemoryDC;
    }

    operator HBITMAP()
    {
        if (NULL != m_hOldBitmap) {
            m_hBitmap = (HBITMAP)::SelectObject(m_hMemoryDC, m_hOldBitmap);
            m_hOldBitmap = NULL;
        }
        return m_hBitmap;
    }

    static HBITMAP CreateJavaContextBitmap(
        HDC hdc,
        int iWidth,
        int iHeight,
        void **ppPoints);
};

#endif //_AWT_DCHolder_H
