/*
 * Copyright (c) 1999, 2021, Oracle and/or its affiliates. All rights reserved.
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

#if defined(DEBUG)

#include "debug_util.h"

#define DMEM_MIN(a,b)   (a) < (b) ? (a) : (b)
#define DMEM_MAX(a,b)   (a) > (b) ? (a) : (b)

typedef char byte_t;

static const byte_t ByteInited = '\xCD';
static const byte_t ByteFreed = '\xDD';
static const byte_t ByteGuard = '\xFD';

enum {
    MAX_LINENUM = 50000,        /* I certainly hope we don't have source files bigger than this */
    MAX_CHECK_BYTES = 27,       /* max bytes to check at start of block */
    MAX_GUARD_BYTES = 8,        /* size of guard areas on either side of a block */
    MAX_DECIMAL_DIGITS = 15
};

/* Debug Info Header to precede allocated block */
typedef struct MemoryBlockHeader {
    char                        filename[FILENAME_MAX+1]; /* filename where alloc occurred */
    int                         linenumber;             /* line where alloc occurred */
    size_t                      size;                   /* size of the allocation */
    int                         order;                  /* the order the block was allocated in */
    struct MemoryListLink *     listEnter;              /* pointer to the free list node */
    byte_t                      guard[MAX_GUARD_BYTES]; /* guard area for underrun check */
} MemoryBlockHeader;

/* Tail to follow allocated block */
typedef struct MemoryBlockTail {
    byte_t                      guard[MAX_GUARD_BYTES]; /* guard area overrun check */
} MemoryBlockTail;

/* Linked list of allocated memory blocks */
typedef struct MemoryListLink {
    struct MemoryListLink *     next;
    MemoryBlockHeader *         header;
    int                         freed;
} MemoryListLink;

/**************************************************
 * Global Data structures
 */
static DMemState                DMemGlobalState;
extern const DMemState *        DMemStatePtr = &DMemGlobalState;
static MemoryListLink           MemoryList = {NULL,NULL,FALSE};
static dmutex_t                 DMemMutex = NULL;

/**************************************************/

/*************************************************
 * Client callback invocation functions
 */
static void * DMem_ClientAllocate(size_t size) {
    if (DMemGlobalState.pfnAlloc != NULL) {
        return (*DMemGlobalState.pfnAlloc)(size);
    }
    return malloc(size);
}

static void DMem_ClientFree(void * ptr) {
    if (DMemGlobalState.pfnFree != NULL) {
        (*DMemGlobalState.pfnFree)(ptr);
    }
    free(ptr);
}

static dbool_t DMem_ClientCheckPtr(void * ptr, size_t size) {
    if (DMemGlobalState.pfnCheckPtr != NULL) {
        return (*DMemGlobalState.pfnCheckPtr)(ptr, size);
    }
    return ptr != NULL;
}

/**************************************************/

/*************************************************
 * Debug Memory Manager implementation
 */

static MemoryListLink * DMem_TrackBlock(MemoryBlockHeader * header) {
    MemoryListLink *    link;

    link = (MemoryListLink *)DMem_ClientAllocate(sizeof(MemoryListLink));
    if (link != NULL) {
        link->header = header;
        link->header->listEnter = link;
        link->next = MemoryList.next;
        link->freed = FALSE;
        MemoryList.next = link;
    }

    return link;
}

static int DMem_VerifyGuardArea(const byte_t * area) {
    int         nbyte;

    for ( nbyte = 0; nbyte < MAX_GUARD_BYTES; nbyte++ ) {
        if (area[nbyte] != ByteGuard) {
            return FALSE;
        }
    }
    return TRUE;
}

static void DMem_VerifyHeader(MemoryBlockHeader * header) {
    DASSERTMSG( DMem_ClientCheckPtr(header, sizeof(MemoryBlockHeader)), "Invalid header" );
    DASSERTMSG( DMem_VerifyGuardArea(header->guard), "Header corruption, possible underwrite" );
    DASSERTMSG( header->linenumber > 0 && header->linenumber < MAX_LINENUM, "Header corruption, bad line number" );
    DASSERTMSG( header->size <= DMemGlobalState.biggestBlock, "Header corruption, block size is too large");
    DASSERTMSG( header->order <= DMemGlobalState.totalAllocs, "Header corruption, block order out of range");
}

static void DMem_VerifyTail(MemoryBlockTail * tail) {
    DASSERTMSG( DMem_ClientCheckPtr(tail, sizeof(MemoryBlockTail)), "Tail corruption, invalid pointer");
    DASSERTMSG( DMem_VerifyGuardArea(tail->guard), "Tail corruption, possible overwrite" );
}

static MemoryBlockHeader * DMem_VerifyBlock(void * memptr) {
    MemoryBlockHeader * header;
    MemoryBlockTail *   tail;

    /* check if the pointer is valid */
    DASSERTMSG( DMem_ClientCheckPtr(memptr, 1), "Invalid pointer");

    /* check if the block header is valid */
    header = (MemoryBlockHeader *)((byte_t *)memptr - sizeof(MemoryBlockHeader));
    DMem_VerifyHeader(header);
    /* check that the memory itself is valid */
    DASSERTMSG( DMem_ClientCheckPtr(memptr, DMEM_MIN(MAX_CHECK_BYTES,header->size)), "Block memory invalid" );
    /* check that the pointer to the alloc list is valid */
    DASSERTMSG( DMem_ClientCheckPtr(header->listEnter, sizeof(MemoryListLink)), "Header corruption, alloc list pointer invalid" );
    /* check the tail of the block for overruns */
    tail = (MemoryBlockTail *) ( (byte_t *)memptr + header->size );
    DMem_VerifyTail(tail);

    return header;
}

static MemoryBlockHeader * DMem_GetHeader(void * memptr) {
    MemoryBlockHeader * header = DMem_VerifyBlock(memptr);
    return header;
}

/*
 * Should be called before any other DMem_XXX function
 */
void DMem_Initialize() {
    DMemMutex = DMutex_Create();
    DMutex_Enter(DMemMutex);
    DMemGlobalState.pfnAlloc = NULL;
    DMemGlobalState.pfnFree = NULL;
    DMemGlobalState.pfnCheckPtr = NULL;
    DMemGlobalState.biggestBlock = 0;
    DMemGlobalState.maxHeap = INT_MAX;
    DMemGlobalState.totalHeapUsed = 0;
    DMemGlobalState.failNextAlloc = FALSE;
    DMemGlobalState.totalAllocs = 0;
    DMutex_Exit(DMemMutex);
}

void DMem_Shutdown() {
    DMutex_Destroy(DMemMutex);
}
/*
 * Allocates a block of memory, reserving extra space at the start and end of the
 * block to store debug info on where the block was allocated, it's size, and
 * 'guard' areas to catch overwrite/underwrite bugs
 */
void * DMem_AllocateBlock(size_t size, const char * filename, int linenumber) {
    MemoryBlockHeader * header;
    MemoryBlockTail *   tail;
    size_t              debugBlockSize;
    byte_t *            memptr = NULL;

    DMutex_Enter(DMemMutex);
    if (DMemGlobalState.failNextAlloc) {
    /* force an allocation failure if so ordered */
        DMemGlobalState.failNextAlloc = FALSE; /* reset flag */
        goto Exit;
    }

    /* allocate a block large enough to hold extra debug info */
    debugBlockSize = sizeof(MemoryBlockHeader) + size + sizeof(MemoryBlockTail);
    header = (MemoryBlockHeader *)DMem_ClientAllocate(debugBlockSize);
    if (header == NULL) {
        goto Exit;
    }

    /* add block to list of allocated memory */
    header->listEnter = DMem_TrackBlock(header);
    if ( header->listEnter == NULL ) {
        DMem_ClientFree(header);
        goto Exit;
    }

    /* store size of requested block */
    header->size = size;
    /* update maximum block size */
    DMemGlobalState.biggestBlock = DMEM_MAX(header->size, DMemGlobalState.biggestBlock);
    /* update used memory total */
    DMemGlobalState.totalHeapUsed += header->size;
    /* store filename and linenumber where allocation routine was called */
    strncpy(header->filename, filename, FILENAME_MAX);
    header->linenumber = linenumber;
    /* store the order the block was allocated in */
    header->order = DMemGlobalState.totalAllocs++;
    /* initialize memory to a recognizable 'inited' value */
    memptr = (byte_t *)header + sizeof(MemoryBlockHeader);
    memset(memptr, ByteInited, size);
    /* put guard area before block */
    memset(header->guard, ByteGuard, MAX_GUARD_BYTES);
    /* put guard area after block */
    tail = (MemoryBlockTail *)(memptr + size);
    memset(tail->guard, ByteGuard, MAX_GUARD_BYTES);

Exit:
    DMutex_Exit(DMemMutex);
    return memptr;
}

/*
 * Frees block of memory allocated with DMem_AllocateBlock
 */
void DMem_FreeBlock(void * memptr) {
    MemoryBlockHeader * header;

    DMutex_Enter(DMemMutex);
    if ( memptr == NULL) {
        goto Exit;
    }

    /* get the debug block header preceding the allocated memory */
    header = DMem_GetHeader(memptr);
    /* fill memory with recognizable 'freed' value */
    memset(memptr, ByteFreed, header->size);
    /* mark block as freed */
    header->listEnter->freed = TRUE;
    /* update used memory total */
    DMemGlobalState.totalHeapUsed -= header->size;
Exit:
    DMutex_Exit(DMemMutex);
}

static void DMem_DumpHeader(MemoryBlockHeader * header) {
    char        report[FILENAME_MAX+MAX_DECIMAL_DIGITS*3+1];
    static const char * reportFormat =
        "file:  %s, line %d\n"
        "size:  %d bytes\n"
        "order: %d\n"
        "-------";

    DMem_VerifyHeader(header);
    sprintf(report, reportFormat, header->filename, header->linenumber, header->size, header->order);
    DTRACE_PRINTLN(report);
}

/*
 * Call this function at shutdown time to report any leaked blocks
 */
void DMem_ReportLeaks() {
    MemoryListLink *    link;

    DMutex_Enter(DMemMutex);

    /* Force memory leaks to be output regardless of trace settings */
    DTrace_EnableFile(__FILE__, TRUE);
    DTRACE_PRINTLN("--------------------------");
    DTRACE_PRINTLN("Debug Memory Manager Leaks");
    DTRACE_PRINTLN("--------------------------");

    /* walk through allocated list and dump any blocks not marked as freed */
    link = MemoryList.next;
    while (link != NULL) {
        if ( !link->freed ) {
            DMem_DumpHeader(link->header);
        }
        link = link->next;
    }

    DMutex_Exit(DMemMutex);
}

void DMem_SetAllocCallback( DMEM_ALLOCFN pfn ) {
    DMutex_Enter(DMemMutex);
    DMemGlobalState.pfnAlloc = pfn;
    DMutex_Exit(DMemMutex);
}

void DMem_SetFreeCallback( DMEM_FREEFN pfn ) {
    DMutex_Enter(DMemMutex);
    DMemGlobalState.pfnFree = pfn;
    DMutex_Exit(DMemMutex);
}

void DMem_SetCheckPtrCallback( DMEM_CHECKPTRFN pfn ) {
    DMutex_Enter(DMemMutex);
    DMemGlobalState.pfnCheckPtr = pfn;
    DMutex_Exit(DMemMutex);
}

void DMem_DisableMutex() {
    DMemMutex = NULL;
}

#endif  /* defined(DEBUG) */

/* The following line is only here to prevent compiler warnings
 * on release (non-debug) builds
 */
static int dummyVariable = 0;
