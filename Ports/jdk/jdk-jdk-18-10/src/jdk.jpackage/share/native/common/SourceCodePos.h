/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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


#ifndef SourceCodePos_h
#define SourceCodePos_h


//
// Position in source code.
//

struct SourceCodePos
{
    SourceCodePos(const char* fl, const char* fnc, int l):
                                                file(fl), func(fnc), lno(l)
    {
    }

    const char* file;
    const char* func;
    int lno;
};


// Initializes SourceCodePos instance with the
// information from the point of calling.
#ifdef THIS_FILE
    #define JP_SOURCE_CODE_POS SourceCodePos(THIS_FILE, __FUNCTION__, __LINE__)
#else
    #define JP_SOURCE_CODE_POS SourceCodePos(__FILE__, __FUNCTION__, __LINE__)
#endif


#endif // #ifndef SourceCodePos_h
