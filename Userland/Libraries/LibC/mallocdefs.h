/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#include <AK/InlineLinkedList.h>
#include <AK/Types.h>

#define MAGIC_PAGE_HEADER 0x42657274     // 'Bert'
#define MAGIC_BIGALLOC_HEADER 0x42697267 // 'Birg'
#define MALLOC_SCRUB_BYTE 0xdc
#define FREE_SCRUB_BYTE 0xed

static constexpr unsigned short size_classes[] = { 8, 16, 32, 64, 128, 256, 500, 1016, 2032, 4088, 8184, 16376, 32752, 0 };
static constexpr size_t num_size_classes = (sizeof(size_classes) / sizeof(unsigned short)) - 1;

struct CommonHeader {
    size_t m_magic;
    size_t m_size;
};

struct BigAllocationBlock : public CommonHeader {
    BigAllocationBlock(size_t size)
    {
        m_magic = MAGIC_BIGALLOC_HEADER;
        m_size = size;
    }
    unsigned char* m_slot[0];
};

struct FreelistEntry {
    FreelistEntry* next;
};

struct ChunkedBlock
    : public CommonHeader
    , public InlineLinkedListNode<ChunkedBlock> {

    static constexpr size_t block_size = 64 * KiB;
    static constexpr size_t block_mask = ~(block_size - 1);

    ChunkedBlock(size_t bytes_per_chunk)
    {
        m_magic = MAGIC_PAGE_HEADER;
        m_size = bytes_per_chunk;
        m_free_chunks = chunk_capacity();
        m_freelist = (FreelistEntry*)chunk(0);
        for (size_t i = 0; i < chunk_capacity(); ++i) {
            auto* entry = (FreelistEntry*)chunk(i);
            if (i != chunk_capacity() - 1)
                entry->next = (FreelistEntry*)chunk(i + 1);
            else
                entry->next = nullptr;
        }
    }

    ChunkedBlock* m_prev { nullptr };
    ChunkedBlock* m_next { nullptr };
    FreelistEntry* m_freelist { nullptr };
    size_t m_free_chunks { 0 };
    [[gnu::aligned(8)]] unsigned char m_slot[0];

    void* chunk(size_t index)
    {
        return &m_slot[index * m_size];
    }
    bool is_full() const { return m_free_chunks == 0; }
    size_t bytes_per_chunk() const { return m_size; }
    size_t free_chunks() const { return m_free_chunks; }
    size_t used_chunks() const { return chunk_capacity() - m_free_chunks; }
    size_t chunk_capacity() const { return (block_size - sizeof(ChunkedBlock)) / m_size; }
};
