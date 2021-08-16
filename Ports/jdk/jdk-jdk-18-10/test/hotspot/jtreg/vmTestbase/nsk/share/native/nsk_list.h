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

#ifndef NSK_LIST
#define NSK_LIST

extern "C" {

/**
 * Prepares array of pointers which has fixed INITIAL_SIZE.
 * Memory for this array will be reallocated at the nsk_list_add()
 * invocation if it is required.
 *
 * To release memory call nsk_list_destroy()
 *
 */
const void* nsk_list_create();

/**
 * Releases memory allocated for array of pointer
 * Returns NSK_TRUE if array was successfully released
 */
int nsk_list_destroy(const void *plist);

/**
 * Returns number of elements
 */
int nsk_list_getCount(const void *plist);

/**
 * Returns pointer to i-th element.
 * User must care for type cast of this pointer
 */
const void* nsk_list_get(const void *plist, int i);

/**
 * Adds new element into array.
 * If array is full then memory is reallocated so as
 * array could contain additional INITIAL_SIZE elements
 * Returns NSK_TRUE if pointer was successfully added
 */
int nsk_list_add(const void *plist, const void *p);

/**
 * Removes i-th pointer from array
 */
int nsk_list_remove(const void *plist, int i);

}

#endif
