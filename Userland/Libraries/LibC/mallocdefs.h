/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/AllOf.h>
#include <AK/Array.h>
#include <AK/InlineLinkedList.h>
#include <AK/Types.h>

#define MAGIC_PAGE_HEADER 0x42657274     // 'Bert'
#define MAGIC_BIGALLOC_HEADER 0x42697267 // 'Birg'
#define MALLOC_SCRUB_BYTE 0xdc
#define FREE_SCRUB_BYTE 0xed

static constexpr Array<unsigned short, 13> size_classes { 8, 16, 32, 64, 128, 256, 504, 1016, 2032, 4088, 8184, 16376, 32752 };
static constexpr auto malloc_alignment = 8;
static_assert(all_of(size_classes.begin(), size_classes.end(),
    [](const auto val) { return val % malloc_alignment == 0; }));

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
    }

    ChunkedBlock* m_prev { nullptr };
    ChunkedBlock* m_next { nullptr };
    size_t m_next_lazy_freelist_index { 0 };
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
