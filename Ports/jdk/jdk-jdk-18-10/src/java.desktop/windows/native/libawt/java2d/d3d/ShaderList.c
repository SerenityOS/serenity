/*
 * Copyright (c) 2007, 2008, Oracle and/or its affiliates. All rights reserved.
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

#include <malloc.h>
#include <string.h>

#include "ShaderList.h"
#include "Trace.h"

/**
 * Creates a new ShaderInfo that wraps the given fragment program handle
 * and related data and stores it at the front of the provided ShaderList.
 * If the addition causes the ShaderList to outgrow its defined capacity,
 * the least-recently used item in the list (including its fragment program
 * object) will be disposed.
 */
void
ShaderList_AddProgram(ShaderList *programList,
                      jlong programID,
                      jint compType, jint compMode, jint flags)
{
    ShaderInfo *info;

    J2dTraceLn(J2D_TRACE_INFO, "ShaderList_AddProgram");

    // create new ShaderInfo
    info = (ShaderInfo *)malloc(sizeof(ShaderInfo));
    if (info == NULL) {
        J2dTraceLn(J2D_TRACE_ERROR,
                   "D3DContext_AddProgram: could not allocate ShaderInfo");
        return;
    }

    // fill in the information
    info->next = programList->head;
    info->programID = programID;
    info->compType = compType;
    info->compMode = compMode;
    info->flags = flags;

    // insert it at the head of the list
    programList->head = info;

    // run through the list and see if we need to delete the least
    // recently used item
    {
        int i = 1;
        ShaderInfo *prev = NULL;
        ShaderInfo *curr = info->next;
        while (curr != NULL) {
            if (i >= programList->maxItems) {
                prev->next = NULL;
                programList->dispose(curr->programID);
                free(curr);
                break;
            }
            i++;
            prev = curr;
            curr = curr->next;
        }
    }
}

/**
 * Locates a fragment program handle given a list of shader programs
 * (ShaderInfos), using the provided composite state and flags as search
 * parameters.  The "flags" parameter is a bitwise-or'd value that helps
 * differentiate one program for another; the interpretation of this value
 * varies depending on the type of shader (BufImgOp, Paint, etc) but here
 * it is only used to find another ShaderInfo with that same "flags" value.
 * If no matching program can be located, this method returns 0.
 */
jlong
ShaderList_FindProgram(ShaderList *programList,
                       jint compType, jint compMode, jint flags)
{
    ShaderInfo *prev = NULL;
    ShaderInfo *info = programList->head;

    J2dTraceLn(J2D_TRACE_INFO, "ShaderList_FindProgram");

    while (info != NULL) {
        if (compType == info->compType &&
            compMode == info->compMode &&
            flags == info->flags)
        {
            // it's a match: move it to the front of the list (if it's not
            // there already) and patch up the links
            if (info != programList->head) {
                prev->next = info->next;
                info->next = programList->head;
                programList->head = info;
            }
            return info->programID;
        }
        prev = info;
        info = info->next;
    }
    return 0;
}

/**
 * Disposes all entries (and their associated shader program objects)
 * contained in the given ShaderList.
 */
void
ShaderList_Dispose(ShaderList *programList)
{
    ShaderInfo *info = programList->head;

    J2dTraceLn(J2D_TRACE_INFO, "ShaderList_Dispose");

    while (info != NULL) {
        ShaderInfo *tmp = info->next;
        programList->dispose(info->programID);
        free(info);
        info = tmp;
    }

    programList->head = NULL;
}
