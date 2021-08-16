/*
 * Copyright (c) 1998, 2020, Oracle and/or its affiliates. All rights reserved.
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

#if defined(_ALLBSD_SOURCE)
#include <stdint.h>                     /* for uintptr_t */
#endif

#include "util.h"
#include "commonRef.h"

#define ALL_REFS -1

/*
 * Each object sent to the front end is tracked with the RefNode struct
 * (see util.h).
 * External to this module, objects are identified by a jlong id which is
 * simply the sequence number. A weak reference is usually used so that
 * the presence of a debugger-tracked object will not prevent
 * its collection. Once an object is collected, its RefNode may be
 * deleted and the weak ref inside may be reused (these may happen in
 * either order). Using the sequence number
 * as the object id prevents ambiguity in the object id when the weak ref
 * is reused. The RefNode* is stored with the object as it's JVMTI Tag.
 *
 * The ref member is changed from weak to strong when
 * gc of the object is to be prevented.
 * Whether or not it is strong, it is never exported from this module.
 *
 * A reference count of each jobject is also maintained here. It tracks
 * the number times an object has been referenced through
 * commonRef_refToID. A RefNode is freed once the reference
 * count is decremented to 0 (with commonRef_release*), even if the
 * corresponding object has not been collected.
 *
 * One hash table is maintained. The mapping of ID to jobject (or RefNode*)
 * is handled with one hash table that will re-size itself as the number
 * of RefNode's grow.
 */

/* Initial hash table size (must be power of 2) */
#define HASH_INIT_SIZE 512
/* If element count exceeds HASH_EXPAND_SCALE*hash_size we expand & re-hash */
#define HASH_EXPAND_SCALE 8
/* Maximum hash table size (must be power of 2) */
#define HASH_MAX_SIZE  (1024*HASH_INIT_SIZE)

/* Map a key (ID) to a hash bucket */
static jint
hashBucket(jlong key)
{
    /* Size should always be a power of 2, use mask instead of mod operator */
    /*LINTED*/
    return ((jint)key) & (gdata->objectsByIDsize-1);
}

/* Generate a new ID */
static jlong
newSeqNum(void)
{
    return gdata->nextSeqNum++;
}

/* Create a fresh RefNode structure, create a weak ref and tag the object */
static RefNode *
createNode(JNIEnv *env, jobject ref)
{
    RefNode   *node;
    jobject    strongOrWeakRef;
    jvmtiError error;
    jboolean   pin = gdata->pinAllCount != 0;

    /* Could allocate RefNode's in blocks, not sure it would help much */
    node = (RefNode*)jvmtiAllocate((int)sizeof(RefNode));
    if (node == NULL) {
        return NULL;
    }

    if (pin) {
        /* Create strong reference to make sure we have a reference */
        strongOrWeakRef = JNI_FUNC_PTR(env,NewGlobalRef)(env, ref);
    } else {
        /* Create weak reference to make sure we have a reference */
        strongOrWeakRef = JNI_FUNC_PTR(env,NewWeakGlobalRef)(env, ref);

        // NewWeakGlobalRef can throw OOM, clear exception here.
        if ((*env)->ExceptionCheck(env)) {
            (*env)->ExceptionClear(env);
            jvmtiDeallocate(node);
            return NULL;
        }
    }

    /* Set tag on strongOrWeakRef */
    error = JVMTI_FUNC_PTR(gdata->jvmti, SetTag)
                          (gdata->jvmti, strongOrWeakRef, ptr_to_jlong(node));
    if ( error != JVMTI_ERROR_NONE ) {
        if (pin) {
            JNI_FUNC_PTR(env,DeleteGlobalRef)(env, strongOrWeakRef);
        } else {
            JNI_FUNC_PTR(env,DeleteWeakGlobalRef)(env, strongOrWeakRef);
        }
        jvmtiDeallocate(node);
        return NULL;
    }

    /* Fill in RefNode */
    node->ref         = strongOrWeakRef;
    node->count       = 1;
    node->strongCount = pin ? 1 : 0;
    node->seqNum      = newSeqNum();

    /* Count RefNode's created */
    gdata->objectsByIDcount++;
    return node;
}

/* Delete a RefNode allocation, delete weak/global ref and clear tag */
static void
deleteNode(JNIEnv *env, RefNode *node)
{
    LOG_MISC(("Freeing %d (%x)\n", (int)node->seqNum, node->ref));

    if ( node->ref != NULL ) {
        /* Clear tag */
        (void)JVMTI_FUNC_PTR(gdata->jvmti,SetTag)
                            (gdata->jvmti, node->ref, NULL_OBJECT_ID);
        if (node->strongCount != 0) {
            JNI_FUNC_PTR(env,DeleteGlobalRef)(env, node->ref);
        } else {
            JNI_FUNC_PTR(env,DeleteWeakGlobalRef)(env, node->ref);
        }
    }
    gdata->objectsByIDcount--;
    jvmtiDeallocate(node);
}

/* Change a RefNode to have a strong reference */
static jobject
strengthenNode(JNIEnv *env, RefNode *node)
{
    if (node->strongCount == 0) {
        jobject strongRef;

        strongRef = JNI_FUNC_PTR(env,NewGlobalRef)(env, node->ref);
        /*
         * NewGlobalRef on a weak ref will return NULL if the weak
         * reference has been collected or if out of memory.
         * It never throws OOM.
         * We need to distinguish those two occurrences.
         */
        if ((strongRef == NULL) && !isSameObject(env, node->ref, NULL)) {
            EXIT_ERROR(AGENT_ERROR_NULL_POINTER,"NewGlobalRef");
        }
        if (strongRef != NULL) {
            JNI_FUNC_PTR(env,DeleteWeakGlobalRef)(env, node->ref);
            node->ref         = strongRef;
            node->strongCount = 1;
        }
        return strongRef;
    } else {
        node->strongCount++;
        return node->ref;
    }
}

/* Change a RefNode to have a weak reference */
static jweak
weakenNode(JNIEnv *env, RefNode *node)
{
    if (node->strongCount == 1) {
        jweak weakRef;

        weakRef = JNI_FUNC_PTR(env,NewWeakGlobalRef)(env, node->ref);
        // NewWeakGlobalRef can throw OOM, clear exception here.
        if ((*env)->ExceptionCheck(env)) {
            (*env)->ExceptionClear(env);
        }

        if (weakRef != NULL) {
            JNI_FUNC_PTR(env,DeleteGlobalRef)(env, node->ref);
            node->ref         = weakRef;
            node->strongCount = 0;
        }
        return weakRef;
    } else {
        if (node->strongCount > 0) {
            node->strongCount--;
        }
        return node->ref;
    }
}

/*
 * Returns the node which contains the common reference for the
 * given object. The passed reference should not be a weak reference
 * managed in the object hash table (i.e. returned by commonRef_idToRef)
 * because no sequence number checking is done.
 */
static RefNode *
findNodeByRef(JNIEnv *env, jobject ref)
{
    jvmtiError error;
    jlong      tag;

    tag   = NULL_OBJECT_ID;
    error = JVMTI_FUNC_PTR(gdata->jvmti,GetTag)(gdata->jvmti, ref, &tag);
    if ( error == JVMTI_ERROR_NONE ) {
        RefNode   *node;

        node = (RefNode*)jlong_to_ptr(tag);
        return node;
    }
    return NULL;
}

/* Locate and delete a node based on ID */
static void
deleteNodeByID(JNIEnv *env, jlong id, jint refCount)
{
    jint     slot;
    RefNode *node;
    RefNode *prev;

    slot = hashBucket(id);
    node = gdata->objectsByID[slot];
    prev = NULL;

    while (node != NULL) {
        if (id == node->seqNum) {
            if (refCount != ALL_REFS) {
                node->count -= refCount;
            } else {
                node->count = 0;
            }
            if (node->count <= 0) {
                if ( node->count < 0 ) {
                    EXIT_ERROR(AGENT_ERROR_INTERNAL,"RefNode count < 0");
                }
                /* Detach from id hash table */
                if (prev == NULL) {
                    gdata->objectsByID[slot] = node->next;
                } else {
                    prev->next = node->next;
                }
                deleteNode(env, node);
            }
            break;
        }
        prev = node;
        node = node->next;
    }
}

/*
 * Returns the node stored in the object hash table for the given object
 * id. The id should be a value previously returned by
 * commonRef_refToID.
 *
 *  NOTE: It is possible that a match is found here, but that the object
 *        is garbage collected by the time the caller inspects node->ref.
 *        Callers should take care using the node->ref object returned here.
 *
 */
static RefNode *
findNodeByID(JNIEnv *env, jlong id)
{
    jint     slot;
    RefNode *node;
    RefNode *prev;

    slot = hashBucket(id);
    node = gdata->objectsByID[slot];
    prev = NULL;

    while (node != NULL) {
        if ( id == node->seqNum ) {
            if ( prev != NULL ) {
                /* Re-order hash list so this one is up front */
                prev->next = node->next;
                node->next = gdata->objectsByID[slot];
                gdata->objectsByID[slot] = node;
            }
            break;
        }
        node = node->next;
    }
    return node;
}

/* Initialize the hash table stored in gdata area */
static void
initializeObjectsByID(int size)
{
    /* Size should always be a power of 2 */
    if ( size > HASH_MAX_SIZE ) size = HASH_MAX_SIZE;
    gdata->objectsByIDsize  = size;
    gdata->objectsByIDcount = 0;
    gdata->objectsByID      = (RefNode**)jvmtiAllocate((int)sizeof(RefNode*)*size);
    (void)memset(gdata->objectsByID, 0, (int)sizeof(RefNode*)*size);
}

/* hash in a RefNode */
static void
hashIn(RefNode *node)
{
    jint     slot;

    /* Add to id hashtable */
    slot                     = hashBucket(node->seqNum);
    node->next               = gdata->objectsByID[slot];
    gdata->objectsByID[slot] = node;
}

/* Allocate and add RefNode to hash table */
static RefNode *
newCommonRef(JNIEnv *env, jobject ref)
{
    RefNode *node;

    /* Allocate the node and set it up */
    node = createNode(env, ref);
    if ( node == NULL ) {
        return NULL;
    }

    /* See if hash table needs expansion */
    if ( gdata->objectsByIDcount > gdata->objectsByIDsize*HASH_EXPAND_SCALE &&
         gdata->objectsByIDsize < HASH_MAX_SIZE ) {
        RefNode **old;
        int       oldsize;
        int       newsize;
        int       i;

        /* Save old information */
        old     = gdata->objectsByID;
        oldsize = gdata->objectsByIDsize;
        /* Allocate new hash table */
        gdata->objectsByID = NULL;
        newsize = oldsize*HASH_EXPAND_SCALE;
        if ( newsize > HASH_MAX_SIZE ) newsize = HASH_MAX_SIZE;
        initializeObjectsByID(newsize);
        /* Walk over old one and hash in all the RefNodes */
        for ( i = 0 ; i < oldsize ; i++ ) {
            RefNode *onode;

            onode = old[i];
            while (onode != NULL) {
                RefNode *next;

                next = onode->next;
                hashIn(onode);
                onode = next;
            }
        }
        jvmtiDeallocate(old);
    }

    /* Add to id hashtable */
    hashIn(node);
    return node;
}

/* Initialize the commonRefs usage */
void
commonRef_initialize(void)
{
    gdata->refLock = debugMonitorCreate("JDWP Reference Table Monitor");
    gdata->nextSeqNum = 1; /* 0 used for error indication */
    gdata->pinAllCount = 0;
    initializeObjectsByID(HASH_INIT_SIZE);
}

/* Reset the commonRefs usage */
void
commonRef_reset(JNIEnv *env)
{
    debugMonitorEnter(gdata->refLock); {
        int i;

        for (i = 0; i < gdata->objectsByIDsize; i++) {
            RefNode *node;

            node = gdata->objectsByID[i];
            while (node != NULL) {
                RefNode *next;

                next = node->next;
                deleteNode(env, node);
                node = next;
            }
            gdata->objectsByID[i] = NULL;
        }

        /* Toss entire hash table and re-create a new one */
        jvmtiDeallocate(gdata->objectsByID);
        gdata->objectsByID      = NULL;
        gdata->nextSeqNum       = 1; /* 0 used for error indication */
        initializeObjectsByID(HASH_INIT_SIZE);

    } debugMonitorExit(gdata->refLock);
}

/*
 * Given a reference obtained from JNI or JVMTI, return an object
 * id suitable for sending to the debugger front end.
 */
jlong
commonRef_refToID(JNIEnv *env, jobject ref)
{
    jlong id;

    if (ref == NULL) {
        return NULL_OBJECT_ID;
    }

    id = NULL_OBJECT_ID;
    debugMonitorEnter(gdata->refLock); {
        RefNode *node;

        node = findNodeByRef(env, ref);
        if (node == NULL) {
            node = newCommonRef(env, ref);
            if ( node != NULL ) {
                id = node->seqNum;
            }
        } else {
            id = node->seqNum;
            node->count++;
        }
    } debugMonitorExit(gdata->refLock);
    return id;
}

/*
 * Given an object ID obtained from the debugger front end, return a
 * strong, global reference to that object (or NULL if the object
 * has been collected). The reference can then be used for JNI and
 * JVMTI calls. Caller is resposible for deleting the returned reference.
 */
jobject
commonRef_idToRef(JNIEnv *env, jlong id)
{
    jobject ref;

    ref = NULL;
    debugMonitorEnter(gdata->refLock); {
        RefNode *node;

        node = findNodeByID(env, id);
        if (node != NULL) {
            if (node->strongCount != 0) {
                saveGlobalRef(env, node->ref, &ref);
            } else {
                jobject lref;

                lref = JNI_FUNC_PTR(env,NewLocalRef)(env, node->ref);
                // NewLocalRef never throws OOM.
                if ( lref == NULL ) {
                    /* Object was GC'd shortly after we found the node */
                    deleteNodeByID(env, node->seqNum, ALL_REFS);
                } else {
                    saveGlobalRef(env, node->ref, &ref);
                    JNI_FUNC_PTR(env,DeleteLocalRef)(env, lref);
                }
            }
        }
    } debugMonitorExit(gdata->refLock);
    return ref;
}

/* Deletes the global reference that commonRef_idToRef() created */
void
commonRef_idToRef_delete(JNIEnv *env, jobject ref)
{
    if ( ref==NULL ) {
        return;
    }
    tossGlobalRef(env, &ref);
}


/* Prevent garbage collection of an object */
jvmtiError
commonRef_pin(jlong id)
{
    jvmtiError error;

    error = JVMTI_ERROR_NONE;
    if (id == NULL_OBJECT_ID) {
        return error;
    }
    debugMonitorEnter(gdata->refLock); {
        JNIEnv  *env;
        RefNode *node;

        env  = getEnv();
        node = findNodeByID(env, id);
        if (node == NULL) {
            error = AGENT_ERROR_INVALID_OBJECT;
        } else {
            jobject strongRef;

            strongRef = strengthenNode(env, node);
            if (strongRef == NULL) {
                /*
                 * Referent has been collected, clean up now.
                 */
                error = AGENT_ERROR_INVALID_OBJECT;
                deleteNodeByID(env, id, ALL_REFS);
            }
        }
    } debugMonitorExit(gdata->refLock);
    return error;
}

/* Permit garbage collection of an object */
jvmtiError
commonRef_unpin(jlong id)
{
    jvmtiError error;

    error = JVMTI_ERROR_NONE;
    debugMonitorEnter(gdata->refLock); {
        JNIEnv  *env;
        RefNode *node;

        env  = getEnv();
        node = findNodeByID(env, id);
        if (node != NULL) {
            jweak weakRef;

            weakRef = weakenNode(env, node);
            if (weakRef == NULL) {
                error = AGENT_ERROR_OUT_OF_MEMORY;
            }
        }
    } debugMonitorExit(gdata->refLock);
    return error;
}

/* Prevent garbage collection of object */
void
commonRef_pinAll()
{
    debugMonitorEnter(gdata->refLock); {
        gdata->pinAllCount++;

        if (gdata->pinAllCount == 1) {
            JNIEnv  *env;
            RefNode *node;
            RefNode *prev;
            int     i;

            env = getEnv();

            /*
             * Walk through the id-based hash table. Detach any nodes
             * for which the ref has been collected.
             */
            for (i = 0; i < gdata->objectsByIDsize; i++) {
                node = gdata->objectsByID[i];
                prev = NULL;
                while (node != NULL) {
                    jobject strongRef;

                    strongRef = strengthenNode(env, node);

                    /* Has the object been collected? */
                    if (strongRef == NULL) {
                        RefNode *freed;

                        /* Detach from the ID list */
                        if (prev == NULL) {
                            gdata->objectsByID[i] = node->next;
                        } else {
                            prev->next = node->next;
                        }
                        freed = node;
                        node = node->next;
                        deleteNode(env, freed);
                    } else {
                        prev = node;
                        node = node->next;
                    }
                }
            }
        }
    } debugMonitorExit(gdata->refLock);
}

/* Permit garbage collection of objects */
void
commonRef_unpinAll()
{
    debugMonitorEnter(gdata->refLock); {
        gdata->pinAllCount--;

        if (gdata->pinAllCount == 0) {
            JNIEnv  *env;
            RefNode *node;
            int     i;

            env = getEnv();

            for (i = 0; i < gdata->objectsByIDsize; i++) {
                for (node = gdata->objectsByID[i]; node != NULL; node = node->next) {
                    jweak weakRef;

                    weakRef = weakenNode(env, node);
                    if (weakRef == NULL) {
                        EXIT_ERROR(AGENT_ERROR_NULL_POINTER,"NewWeakGlobalRef");
                    }
                }
            }
        }
    } debugMonitorExit(gdata->refLock);
}

/* Release tracking of an object by ID */
void
commonRef_release(JNIEnv *env, jlong id)
{
    debugMonitorEnter(gdata->refLock); {
        deleteNodeByID(env, id, 1);
    } debugMonitorExit(gdata->refLock);
}

void
commonRef_releaseMultiple(JNIEnv *env, jlong id, jint refCount)
{
    debugMonitorEnter(gdata->refLock); {
        deleteNodeByID(env, id, refCount);
    } debugMonitorExit(gdata->refLock);
}

/* Get rid of RefNodes for objects that no longer exist */
void
commonRef_compact(void)
{
    JNIEnv  *env;
    RefNode *node;
    RefNode *prev;
    int      i;

    env = getEnv();
    debugMonitorEnter(gdata->refLock); {
        if ( gdata->objectsByIDsize > 0 ) {
            /*
             * Walk through the id-based hash table. Detach any nodes
             * for which the ref has been collected.
             */
            for (i = 0; i < gdata->objectsByIDsize; i++) {
                node = gdata->objectsByID[i];
                prev = NULL;
                while (node != NULL) {
                    /* Has the object been collected? */
                    if ( (node->strongCount == 0) &&
                          isSameObject(env, node->ref, NULL)) {
                        RefNode *freed;

                        /* Detach from the ID list */
                        if (prev == NULL) {
                            gdata->objectsByID[i] = node->next;
                        } else {
                            prev->next = node->next;
                        }
                        freed = node;
                        node = node->next;
                        deleteNode(env, freed);
                    } else {
                        prev = node;
                        node = node->next;
                    }
                }
            }
        }
    } debugMonitorExit(gdata->refLock);
}

/* Lock the commonRef tables */
void
commonRef_lock(void)
{
    debugMonitorEnter(gdata->refLock);
}

/* Unlock the commonRef tables */
void
commonRef_unlock(void)
{
    debugMonitorExit(gdata->refLock);
}
