/*
 * Copyright (c) 2021, Jesse Buhagiar <jooster669@gmail.com>
 * Copyright (c) 2022, blackcat <b14ckcat@protonmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NonnullOwnPtr.h>
#include <AK/OwnPtr.h>
#include <AK/Stack.h>
#include <AK/Vector.h>
#include <Kernel/Locking/Spinlock.h>
#include <Kernel/Memory/MemoryManager.h>
#include <Kernel/Memory/Region.h>
#include <Kernel/StdLib.h>

namespace Kernel::USB {

struct USBDMAHandle {
    VirtualAddress vaddr;
    PhysicalAddress paddr;
    u16 size;
};

// This pool is bound by PAGE_SIZE / sizeof(T). The underlying allocation for the pointers
// is AK::Stack. As such, we never dynamically allocate any memory past the amount
// that can fit in a single page.
template<typename T>
class USBDMAPool {
    AK_MAKE_NONCOPYABLE(USBDMAPool);
    AK_MAKE_NONMOVABLE(USBDMAPool);

    // Ensure that we can't get into a situation where we'll write past the page
    // and blow up
    static_assert(sizeof(T) <= PAGE_SIZE);

public:
    static ErrorOr<NonnullOwnPtr<USBDMAPool<T>>> try_create(StringView name)
    {
        auto pool_memory_block = TRY(MM.allocate_dma_buffer_page("USB DMA Pool"sv, Memory::Region::Access::ReadWrite));
        return adopt_nonnull_own_or_enomem(new (nothrow) USBDMAPool(move(pool_memory_block), name));
    }

    ~USBDMAPool() = default;

    [[nodiscard]] T* try_take_free_buffer()
    {
        SpinlockLocker locker(m_pool_lock);

        // We're out of buffers!
        if (m_free_buffer_stack.is_empty())
            return nullptr;

        dbgln_if(USB_DEBUG, "Got a free DMA buffer @ {} from pool {}", m_free_buffer_stack.top(), m_pool_name);
        T* buffer = m_free_buffer_stack.top();
        m_free_buffer_stack.pop();

        return buffer;
    }

    void release_to_pool(T* ptr)
    {
        SpinlockLocker locker(m_pool_lock);

        dbgln_if(USB_DEBUG, "Returning buffer @ {} to pool {}", ptr, m_pool_name);
        if (!m_free_buffer_stack.push(ptr))
            dbgln("Failed to return buffer to pool {}. Stack overflow!", m_pool_name);
    }

    void print_pool_information() const
    {
        dbgln("Pool {} allocated @ {}", m_pool_name, m_pool_region->physical_page(0)->paddr());
    }

private:
    USBDMAPool(NonnullOwnPtr<Memory::Region> pool_memory_block, StringView name)
        : m_pool_name(name)
        , m_pool_region(move(pool_memory_block))
        , m_pool_lock(LockRank::None)
    {
        // Go through the number of buffers to create in the pool, and create a virtual/physical address mapping
        for (size_t i = 0; i < PAGE_SIZE / sizeof(T); i++) {
            auto* placement_address = reinterpret_cast<void*>(m_pool_region->vaddr().get() + (i * sizeof(T)));
            auto physical_address = static_cast<u32>(m_pool_region->physical_page(0)->paddr().get() + (i * sizeof(T)));
            auto* object = new (placement_address) T(physical_address);
            m_free_buffer_stack.push(object); // Push the buffer's pointer onto the free list
        }
    }

    StringView m_pool_name;                               // Name of this pool
    NonnullOwnPtr<Memory::Region> m_pool_region;          // Memory region buffers are allocated from
    Stack<T*, PAGE_SIZE / sizeof(T)> m_free_buffer_stack; // Stack of currently free buffer pointers
    Spinlock m_pool_lock;
};

// For allocating a pool of generic memory buffers for use with DMA, where the size is known only at runtime
template<>
class USBDMAPool<USBDMAHandle> {
    AK_MAKE_NONCOPYABLE(USBDMAPool);
    AK_MAKE_NONMOVABLE(USBDMAPool);

public:
    static ErrorOr<NonnullOwnPtr<USBDMAPool<USBDMAHandle>>> try_create(StringView name, u16 buffer_size, u16 num_buffers)
    {
        u32 num_pool_pages = ((num_buffers * buffer_size) / PAGE_SIZE) + (((num_buffers * buffer_size) % PAGE_SIZE) != 0);
        auto pool_memory_block = TRY(MM.allocate_dma_buffer_pages(PAGE_SIZE * num_pool_pages, "USB DMA Pool"sv, Memory::Region::Access::ReadWrite));
        u32 num_handle_pages = ((num_buffers * sizeof(USBDMAHandle)) / PAGE_SIZE) + (((num_buffers * sizeof(USBDMAHandle)) % PAGE_SIZE) != 0);
        auto handle_memory_block = TRY(MM.allocate_dma_buffer_pages(PAGE_SIZE * num_handle_pages, "USB DMA Handles"sv, Memory::Region::Access::ReadWrite));
        return adopt_nonnull_own_or_enomem(new (nothrow) USBDMAPool(move(pool_memory_block), move(handle_memory_block), name, buffer_size, num_buffers));
    }

    ~USBDMAPool() = default;

    [[nodiscard]] USBDMAHandle* try_take_free_buffer()
    {
        SpinlockLocker locker(m_pool_lock);

        // We're out of buffers!
        if (m_dma_handles.is_empty())
            return nullptr;

        dbgln_if(USB_DEBUG, "Got a free DMA handle @ {} from pool {}", m_dma_handles.last(), m_pool_name);
        return m_dma_handles.take_last();
    }

    void release_to_pool(USBDMAHandle* handle)
    {
        SpinlockLocker locker(m_pool_lock);
        dbgln_if(USB_DEBUG, "Returning handle @ {} to pool {}", handle, m_pool_name);
        m_dma_handles.append(handle);
    }

    void print_pool_information()
    {
        dbgln("Pool {} allocated @ {}", m_pool_name, m_pool_region->physical_page(0)->paddr());
    }

private:
    USBDMAPool(NonnullOwnPtr<Memory::Region> pool_memory_block, NonnullOwnPtr<Memory::Region> handle_memory_block, StringView name, u16 buffer_size, u16 num_buffers)
        : m_pool_name(name)
        , m_pool_region(move(pool_memory_block))
        , m_handle_region(move(handle_memory_block))
        , m_buffer_size(buffer_size)
        , m_num_buffers(num_buffers)
        , m_pool_lock(LockRank::None)
    {
        for (size_t i = 0; i < m_num_buffers; i++) {
            VirtualAddress vaddr { m_pool_region->vaddr().get() + (m_buffer_size * i) };
            auto page_idx = i / PAGE_SIZE;
            PhysicalAddress paddr { m_pool_region->physical_page(page_idx)->paddr().offset(m_buffer_size * i) };
            auto handle_addr = reinterpret_cast<void*>(m_handle_region->vaddr().get() + (i * sizeof(USBDMAHandle)));
            USBDMAHandle* dma_handle = new (handle_addr) USBDMAHandle { vaddr, paddr, m_buffer_size };
            m_dma_handles.append(dma_handle);
        }
    }

    StringView m_pool_name;                        // Name of this pool
    NonnullOwnPtr<Memory::Region> m_pool_region;   // Memory region buffers are allocated from
    NonnullOwnPtr<Memory::Region> m_handle_region; // Memory region handles are stored in
    u16 const m_buffer_size;                       // How big each DMA buffer is
    u32 const m_num_buffers;                       // Number of DMA buffers
    Vector<USBDMAHandle*> m_dma_handles;
    Spinlock m_pool_lock;
};

}
