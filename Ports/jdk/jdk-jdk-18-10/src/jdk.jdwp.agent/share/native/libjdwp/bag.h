/*
 * Copyright (c) 1998, 2003, Oracle and/or its affiliates. All rights reserved.
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

#ifndef JDWP_BAG_H
#define JDWP_BAG_H

#include <jni.h>

/* Declare general routines for manipulating a bag data structure.
 * Synchronized use is the responsibility of caller.
 */

struct bag;

/* Must be used to create a bag.  itemSize is the size
 * of the items stored in the bag. initialAllocation is a hint
 * for the initial number of items to allocate. Returns the
 * allocated bag, returns NULL if out of memory.
 */
struct bag *bagCreateBag(int itemSize, int initialAllocation);

/*
 * Copy bag contents to another new bag. The new bag is returned, or
 * NULL if out of memory.
 */
struct bag *bagDup(struct bag *);

/* Destroy the bag and reclaim the space it uses.
 */
void bagDestroyBag(struct bag *theBag);

/* Find 'key' in bag.  Assumes first entry in item is a pointer.
 * Return found item pointer, NULL if not found.
 */
void *bagFind(struct bag *theBag, void *key);

/* Add space for an item in the bag.
 * Return allocated item pointer, NULL if no memory.
 */
void *bagAdd(struct bag *theBag);

/* Delete specified item from bag.
 * Does no checks.
 */
void bagDelete(struct bag *theBag, void *condemned);

/* Delete all items from the bag.
 */
void bagDeleteAll(struct bag *theBag);

/* Return the count of items stored in the bag.
 */
int bagSize(struct bag *theBag);

/* Enumerate over the items in the bag, calling 'func' for
 * each item.  The function is passed the item and the user
 * supplied 'arg'.  Abort the enumeration if the function
 * returns FALSE.  Return TRUE if the enumeration completed
 * successfully and FALSE if it was aborted.
 * Addition and deletion during enumeration is not supported.
 */
typedef jboolean (*bagEnumerateFunction)(void *item, void *arg);

jboolean bagEnumerateOver(struct bag *theBag,
                        bagEnumerateFunction func, void *arg);

#endif
