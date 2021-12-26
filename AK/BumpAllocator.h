/*
 * Copyright (c) 2021, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/StdLibExtras.h>
#include <AK/Types.h>
#include <AK/kmalloc.h>
#include <sys/mman.h>

namespace AK {

template<bool use_mmap = false, size_t chunk_size = use_mmap ? 4 * MiB : 4 * KiB>
class BumpAllocator {
public:
    BumpAllocator()
    {
        if constexpr (use_mmap)
            m_chunk_size = chunk_size;
        else
            m_chunk_size = kmalloc_good_size(chunk_size);
    }

    ~BumpAllocator()
    {
        deallocate_all();
    }

    void* allocate(size_t size, size_t align)
    {
        VERIFY(size < m_chunk_size - sizeof(ChunkHeader));
        if (!m_current_chunk) {
            if (!allocate_a_chunk())
                return nullptr;
        }

    allocate_again:;
        VERIFY(m_current_chunk != 0);

        auto aligned_ptr = align_up_to(m_byte_offset_into_current_chunk + m_current_chunk, align);
        auto next_offset = aligned_ptr + size - m_current_chunk;
        if (next_offset > m_chunk_size) {
            if (!allocate_a_chunk())
                return nullptr;
            goto allocate_again;
        }
        m_byte_offset_into_current_chunk = next_offset;
        return (void*)aligned_ptr;
    }

    template<typename T>
    T* allocate()
    {
        return (T*)allocate(sizeof(T), alignof(T));
    }

    void deallocate_all()
    {
        if (!m_head_chunk)
            return;
        for_each_chunk([this](auto chunk) {
            if constexpr (use_mmap) {
                munmap((void*)chunk, m_chunk_size);
            } else {
                kfree_sized((void*)chunk, m_chunk_size);
            }
        });
    }

protected:
    template<typename TFn>
    void for_each_chunk(TFn&& fn)
    {
        auto head_chunk = m_head_chunk;
        while (head_chunk) {
            auto& chunk_header = *(ChunkHeader const*)head_chunk;
            VERIFY(chunk_header.magic == chunk_magic);
            if (head_chunk == m_current_chunk)
                VERIFY(chunk_header.next_chunk == 0);
            auto next_chunk = chunk_header.next_chunk;
            fn(head_chunk);
            head_chunk = next_chunk;
        }
    }

    bool allocate_a_chunk()
    {
        // dbgln("Allocated {} entries in previous chunk and have {} unusable bytes", m_allocations_in_previous_chunk, m_chunk_size - m_byte_offset_into_current_chunk);
        // m_allocations_in_previous_chunk = 0;
        void* new_chunk;
        if constexpr (use_mmap) {
#ifdef __serenity__
            new_chunk = serenity_mmap(nullptr, m_chunk_size, PROT_READ | PROT_WRITE, MAP_ANONYMOUS | MAP_RANDOMIZED | MAP_PRIVATE, 0, 0, m_chunk_size, "BumpAllocator Chunk");
#else
            new_chunk = mmap(nullptr, m_chunk_size, PROT_READ | PROT_WRITE, MAP_ANONYMOUS | MAP_PRIVATE, 0, 0);
#endif
            if (new_chunk == MAP_FAILED)
                return false;
        } else {
            new_chunk = kmalloc(m_chunk_size);
            if (!new_chunk)
                return false;
        }

        auto& new_header = *(ChunkHeader*)new_chunk;
        new_header.magic = chunk_magic;
        new_header.next_chunk = 0;
        m_byte_offset_into_current_chunk = sizeof(ChunkHeader);

        if (!m_head_chunk) {
            VERIFY(!m_current_chunk);
            m_head_chunk = (FlatPtr)new_chunk;
            m_current_chunk = (FlatPtr)new_chunk;
            return true;
        }

        VERIFY(m_current_chunk);
        auto& old_header = *(ChunkHeader*)m_current_chunk;
        VERIFY(old_header.next_chunk == 0);
        old_header.next_chunk = (FlatPtr)new_chunk;
        m_current_chunk = (FlatPtr)new_chunk;
        return true;
    }

    constexpr static FlatPtr chunk_magic = explode_byte(0xdf);
    struct ChunkHeader {
        FlatPtr magic;
        FlatPtr next_chunk;
    };
    FlatPtr m_head_chunk { 0 };
    FlatPtr m_current_chunk { 0 };
    size_t m_byte_offset_into_current_chunk { 0 };
    size_t m_chunk_size { 0 };
    // size_t m_allocations_in_previous_chunk { 0 };
};

template<typename T, bool use_mmap = false, size_t chunk_size = use_mmap ? 4 * MiB : 4 * KiB>
class UniformBumpAllocator : protected BumpAllocator<use_mmap, chunk_size> {
    using Allocator = BumpAllocator<use_mmap, chunk_size>;

public:
    UniformBumpAllocator() = default;
    ~UniformBumpAllocator()
    {
        destroy_all();
    }

    T* allocate()
    {
        return Allocator::template allocate<T>();
    }

    void deallocate_all()
    {
        destroy_all();
        Allocator::deallocate_all();
    }

    void destroy_all()
    {
        this->for_each_chunk([&](auto chunk) {
            auto base_ptr = align_up_to(chunk + sizeof(typename Allocator::ChunkHeader), alignof(T));
            FlatPtr end_offset = this->m_chunk_size - sizeof(typename Allocator::ChunkHeader);
            if (chunk == this->m_current_chunk)
                end_offset = this->m_byte_offset_into_current_chunk;
            for (; base_ptr - chunk < end_offset; base_ptr += sizeof(T))
                reinterpret_cast<T*>(base_ptr)->~T();
        });
    }
};

}

using AK::BumpAllocator;
using AK::UniformBumpAllocator;
