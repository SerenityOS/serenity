/*
 * Copyright (c) 2006, 2018, Oracle and/or its affiliates. All rights reserved.
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
#ifndef __FollowRefObject_h
#define __FollowRefObject_h

#include <jvmti.h>

extern "C" {

/* ============================================================================= */

void jvmti_FollowRefObject_init();

/* ============================================================================= */

#define DBG(x) x
#define DEREF(ptr) (((ptr) == NULL ? 0 : *(ptr)))

extern jvmtiHeapCallbacks g_wrongHeapCallbacks; /* Callbacks that blame */

extern const char * const g_refKindStr[28]; /* JVMTI_HEAP_REFERENCE_xxx */

/* ============================================================================= */

#define MAX_TAG 1000
#define MAX_REFS (MAX_TAG * 3)

#define FLAG_TAG_SET      0x01

extern char * g_szTagInfo[MAX_TAG];
extern char g_tagFlags[MAX_TAG];
extern int g_tagVisitCount[MAX_TAG];

void markTagSet(jlong tag_val);
void markTagVisited(jlong tag_val);

jboolean checkThatAllTagsVisited();

/* ============================================================================= */

typedef struct {

    jlong _tagFrom,
          _tagTo;

    jint  _refKind;

    int   _expectedCount,
          _actualCount;

} RefToVerify;

extern int g_refsToVerifyCnt;

extern RefToVerify g_refsToVerify[MAX_REFS];

jboolean markRefToVerify(jlong tagFrom, jlong tagTo, int refKind);

/* ============================================================================= */

extern int g_fakeUserData;
extern int g_userDataError;

#define CHECK_USER_DATA(p) checkUserData(__FILE__, __LINE__, (p))

void checkUserData(const char * szFile, const int line, void * user_data);

/* ============================================================================= */

void printHeapRefCallbackInfo(
     jvmtiHeapReferenceKind        reference_kind,
     const jvmtiHeapReferenceInfo* reference_info,
     jlong                         class_tag,
     jlong                         referrer_class_tag,
     jlong                         size,
     jlong*                        tag_ptr,
     jlong*                        referrer_tag_ptr,
     jint                          length);

/* ============================================================================= */

}

#endif
