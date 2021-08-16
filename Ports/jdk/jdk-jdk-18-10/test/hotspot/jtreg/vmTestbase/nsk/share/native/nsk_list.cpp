/*
 * Copyright (c) 2004, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "nsk_list.h"
#include "nsk_tools.h"

extern "C" {

#define NSK_LIST_INIT_COUNT 20

typedef struct nsk_list_infoStruct {
    const void **arr;
    int elements_count;
    int allocated_count;
} nsk_list_info;


static int nsk_list_size_void = sizeof(void *);

/* ============================================================================= */

const void* nsk_list_create() {

    nsk_list_info *list_info;

    /* create nsk_list_info */
    list_info = (nsk_list_info *)malloc(sizeof(nsk_list_info));
    if (list_info == NULL) {
        return NULL;
    }

    list_info->allocated_count = NSK_LIST_INIT_COUNT;
    list_info->elements_count = 0;
    list_info->arr = (const void **)malloc(list_info->allocated_count * nsk_list_size_void);
    if (list_info->arr == NULL) {
        free(list_info);
        return NULL;
    }

    return list_info;
}

/* ============================================================================= */

int nsk_list_destroy(const void *plist) {

    const nsk_list_info *list_info = (const nsk_list_info *)plist;

    free((void *)list_info->arr);
    free((void *)plist);

    return NSK_TRUE;
}

/* ============================================================================= */

int nsk_list_add(const void *plist, const void *p) {

    nsk_list_info *list_info = (nsk_list_info *)plist;

    if (list_info->elements_count >= list_info->allocated_count) {
        list_info->allocated_count += NSK_LIST_INIT_COUNT;
        list_info->arr = (const void **)realloc((void *)list_info->arr, list_info->allocated_count * nsk_list_size_void);
        if (list_info->arr == NULL) {
            return NSK_FALSE;
        }
    }
    list_info->arr[list_info->elements_count++] = p;

    return NSK_TRUE;
}

/* ============================================================================= */

int nsk_list_remove(const void *plist, int ind) {

    nsk_list_info *list_info = (nsk_list_info *)plist;

    if ((list_info->elements_count <= 0)
            || ((ind < 0) || (ind >= list_info->elements_count)))
        return NSK_FALSE;

    {
        int i;
        for (i = ind+1; i < list_info->elements_count; i++) {
            list_info->arr[i - 1] = list_info->arr[i];
        }
    }
    list_info->arr[--list_info->elements_count] = 0;

    return NSK_TRUE;
}

/* ============================================================================= */

int nsk_list_getCount(const void *plist) {

    return ((const nsk_list_info *)plist)->elements_count;
}

/* ============================================================================= */

const void* nsk_list_get(const void *plist, int i) {

    const nsk_list_info *list_info = (const nsk_list_info *)plist;

    if ((i >= 0) && (i < list_info->elements_count)) {
        return list_info->arr[i];
    }

    return NULL;
}

}

/* ============================================================================= */
