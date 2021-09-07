/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

// KBuffer: Statically sized kernel-only memory buffer.
//
// A KBuffer is a value-type convenience class that wraps a NonnullRefPtr<KBufferImpl>.
// The memory is allocated via the global kernel-only page allocator, rather than via
// kmalloc() which is what ByteBuffer/Vector/etc will use.
//
// This makes KBuffer a little heavier to allocate, but much better for large and/or
// long-lived allocations, since they don't put all that weight and pressure on the
// severely limited kmalloc heap.

#include <AK/Assertions.h>
#include <AK/ByteBuffer.h>
#include <AK/Memory.h>
#include <AK/StringView.h>
#include <Kernel/Memory/MemoryManager.h>
#include <Kernel/Memory/Region.h>

namespace Kernel {

class KBufferImpl : public RefCounted<KBufferImpl> {
public:
    static RefPtr<KBufferImpl> try_create_with_size(size_t size, Memory::Region::Access access, StringView name = "KBuffer", AllocationStrategy strategy = AllocationStrategy::Reserve)
    {
        auto region_or_error = MM.allocate_kernel_region(Memory::page_round_up(size), name, access, strategy);
        if (region_or_error.is_error())
            return nullptr;
        return adopt_ref_if_nonnull(new (nothrow) KBufferImpl(region_or_error.release_value(), size, strategy));
    }

    static RefPtr<KBufferImpl> try_create_with_bytes(ReadonlyBytes bytes, Memory::Region::Access access, StringView name = "KBuffer", AllocationStrategy strategy = AllocationStrategy::Reserve)
    {
        auto region_or_error = MM.allocate_kernel_region(Memory::page_round_up(bytes.size()), name, access, strategy);
        if (region_or_error.is_error())
            return nullptr;
        auto region = region_or_error.release_value();
        memcpy(region->vaddr().as_ptr(), bytes.data(), bytes.size());

        return adopt_ref_if_nonnull(new (nothrow) KBufferImpl(move(region), bytes.size(), strategy));
    }

    static RefPtr<KBufferImpl> create_with_size(size_t size, Memory::Region::Access access, StringView name, AllocationStrategy strategy = AllocationStrategy::Reserve)
    {
        return try_create_with_size(size, access, name, strategy);
    }

    static RefPtr<KBufferImpl> copy(const void* data, size_t size, Memory::Region::Access access, StringView name)
    {
        auto buffer = create_with_size(size, access, name, AllocationStrategy::AllocateNow);
        if (!buffer)
            return {};
        memcpy(buffer->data(), data, size);
        return buffer;
    }

    [[nodiscard]] bool expand(size_t new_capacity)
    {
        auto new_region_or_error = MM.allocate_kernel_region(Memory::page_round_up(new_capacity), m_region->name(), m_region->access(), m_allocation_strategy);
        if (new_region_or_error.is_error())
            return false;
        auto new_region = new_region_or_error.release_value();
        if (m_size > 0)
            memcpy(new_region->vaddr().as_ptr(), data(), min(m_region->size(), m_size));
        m_region = move(new_region);
        return true;
    }

    [[nodiscard]] u8* data() { return m_region->vaddr().as_ptr(); }
    [[nodiscard]] const u8* data() const { return m_region->vaddr().as_ptr(); }
    [[nodiscard]] size_t size() const { return m_size; }
    [[nodiscard]] size_t capacity() const { return m_region->size(); }

    void set_size(size_t size)
    {
        VERIFY(size <= capacity());
        m_size = size;
    }

    [[nodiscard]] Memory::Region const& region() const { return *m_region; }
    [[nodiscard]] Memory::Region& region() { return *m_region; }

private:
    explicit KBufferImpl(NonnullOwnPtr<Memory::Region>&& region, size_t size, AllocationStrategy strategy)
        : m_size(size)
        , m_allocation_strategy(strategy)
        , m_region(move(region))
    {
    }

    size_t m_size { 0 };
    AllocationStrategy m_allocation_strategy { AllocationStrategy::Reserve };
    NonnullOwnPtr<Memory::Region> m_region;
};

class [[nodiscard]] KBuffer {
public:
    explicit KBuffer(RefPtr<KBufferImpl>&& impl)
        : m_impl(move(impl))
    {
    }

    static KResultOr<NonnullOwnPtr<KBuffer>> try_create_with_size(size_t size, Memory::Region::Access access = Memory::Region::Access::ReadWrite, StringView name = "KBuffer", AllocationStrategy strategy = AllocationStrategy::Reserve)
    {
        auto impl = KBufferImpl::try_create_with_size(size, access, name, strategy);
        if (!impl)
            return ENOMEM;
        return adopt_nonnull_own_or_enomem(new (nothrow) KBuffer(impl.release_nonnull()));
    }

    static KResultOr<NonnullOwnPtr<KBuffer>> try_create_with_bytes(ReadonlyBytes bytes, Memory::Region::Access access = Memory::Region::Access::ReadWrite, StringView name = "KBuffer", AllocationStrategy strategy = AllocationStrategy::Reserve)
    {
        auto impl = KBufferImpl::try_create_with_bytes(bytes, access, name, strategy);
        if (!impl)
            return ENOMEM;
        return adopt_nonnull_own_or_enomem(new (nothrow) KBuffer(impl.release_nonnull()));
    }

    static KResultOr<NonnullOwnPtr<KBuffer>> try_copy(const void* data, size_t size, Memory::Region::Access access = Memory::Region::Access::ReadWrite, StringView name = "KBuffer")
    {
        auto impl = KBufferImpl::copy(data, size, access, name);
        if (!impl)
            return ENOMEM;
        return adopt_nonnull_own_or_enomem(new (nothrow) KBuffer(impl.release_nonnull()));
    }

    [[nodiscard]] bool is_null() const { return !m_impl; }

    [[nodiscard]] u8* data() { return m_impl ? m_impl->data() : nullptr; }
    [[nodiscard]] const u8* data() const { return m_impl ? m_impl->data() : nullptr; }
    [[nodiscard]] size_t size() const { return m_impl ? m_impl->size() : 0; }
    [[nodiscard]] size_t capacity() const { return m_impl ? m_impl->capacity() : 0; }

    [[nodiscard]] void* end_pointer() { return data() + size(); }
    [[nodiscard]] const void* end_pointer() const { return data() + size(); }

    void set_size(size_t size) { m_impl->set_size(size); }

    [[nodiscard]] KBufferImpl& impl() { return *m_impl; }
    [[nodiscard]] const KBufferImpl& impl() const { return *m_impl; }
    [[nodiscard]] RefPtr<KBufferImpl> take_impl() { return move(m_impl); }

    KBuffer(const ByteBuffer& buffer, Memory::Region::Access access = Memory::Region::Access::ReadWrite, StringView name = "KBuffer")
        : m_impl(KBufferImpl::copy(buffer.data(), buffer.size(), access, name))
    {
    }

private:
    RefPtr<KBufferImpl> m_impl;
};

}
