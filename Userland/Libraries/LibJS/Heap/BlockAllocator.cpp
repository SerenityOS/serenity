/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Vector.h>
#include <LibJS/Forward.h>
#include <LibJS/Heap/BlockAllocator.h>
#include <LibJS/Heap/HeapBlock.h>
#include <stdlib.h>
#include <sys/mman.h>

namespace JS {

BlockAllocator::BlockAllocator()
{
}

BlockAllocator::~BlockAllocator()
{
    for (auto* block : m_blocks) {
#ifdef __serenity__
        if (munmap(block, HeapBlock::block_size) < 0) {
            perror("munmap");
            VERIFY_NOT_REACHED();
        }
#else
        free(block);
#endif
    }
}

void* BlockAllocator::allocate_block([[maybe_unused]] char const* name)
{
    if (!m_blocks.is_empty()) {
        auto* block = m_blocks.take_last();
#ifdef __serenity__
        if (set_mmap_name(block, HeapBlock::block_size, name) < 0) {
            perror("set_mmap_name");
            VERIFY_NOT_REACHED();
        }
#endif
        return block;
    }

#ifdef __serenity__
    auto* block = (HeapBlock*)serenity_mmap(nullptr, HeapBlock::block_size, PROT_READ | PROT_WRITE, MAP_ANONYMOUS | MAP_RANDOMIZED | MAP_PRIVATE, 0, 0, HeapBlock::block_size, name);
    VERIFY(block != MAP_FAILED);
#else
    auto* block = (HeapBlock*)aligned_alloc(HeapBlock::block_size, HeapBlock::block_size);
    VERIFY(block);
#endif
    return block;
}

void BlockAllocator::deallocate_block(void* block)
{
    VERIFY(block);
    if (m_blocks.size() >= max_cached_blocks) {
#ifdef __serenity__
        if (munmap(block, HeapBlock::block_size) < 0) {
            perror("munmap");
            VERIFY_NOT_REACHED();
        }
#else
        free(block);
#endif
        return;
    }

    m_blocks.append(block);
}

}
