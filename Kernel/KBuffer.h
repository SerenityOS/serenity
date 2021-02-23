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
#include <AK/LogStream.h>
#include <AK/Memory.h>
#include <AK/StringView.h>
#include <Kernel/VM/MemoryManager.h>
#include <Kernel/VM/Region.h>

namespace Kernel {

class KBufferImpl : public RefCounted<KBufferImpl> {
public:
    static RefPtr<KBufferImpl> try_create_with_size(size_t size, u8 access, const char* name = "KBuffer", AllocationStrategy strategy = AllocationStrategy::Reserve)
    {
        auto region = MM.allocate_kernel_region(page_round_up(size), name, access, strategy);
        if (!region)
            return nullptr;
        return adopt(*new KBufferImpl(region.release_nonnull(), size, strategy));
    }

    static RefPtr<KBufferImpl> try_create_with_bytes(ReadonlyBytes bytes, u8 access, const char* name = "KBuffer", AllocationStrategy strategy = AllocationStrategy::Reserve)
    {
        auto region = MM.allocate_kernel_region(page_round_up(bytes.size()), name, access, strategy);
        if (!region)
            return nullptr;
        memcpy(region->vaddr().as_ptr(), bytes.data(), bytes.size());
        return adopt(*new KBufferImpl(region.release_nonnull(), bytes.size(), strategy));
    }

    static RefPtr<KBufferImpl> create_with_size(size_t size, u8 access, const char* name, AllocationStrategy strategy = AllocationStrategy::Reserve)
    {
        return try_create_with_size(size, access, name, strategy);
    }

    static RefPtr<KBufferImpl> copy(const void* data, size_t size, u8 access, const char* name)
    {
        auto buffer = create_with_size(size, access, name, AllocationStrategy::AllocateNow);
        if (!buffer)
            return {};
        memcpy(buffer->data(), data, size);
        return buffer;
    }

    [[nodiscard]] bool expand(size_t new_capacity)
    {
        auto new_region = MM.allocate_kernel_region(page_round_up(new_capacity), m_region->name(), m_region->access(), m_allocation_strategy);
        if (!new_region)
            return false;
        if (m_region && m_size > 0)
            memcpy(new_region->vaddr().as_ptr(), data(), min(m_region->size(), m_size));
        m_region = new_region.release_nonnull();
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

    [[nodiscard]] const Region& region() const { return *m_region; }
    [[nodiscard]] Region& region() { return *m_region; }

private:
    explicit KBufferImpl(NonnullOwnPtr<Region>&& region, size_t size, AllocationStrategy strategy)
        : m_size(size)
        , m_allocation_strategy(strategy)
        , m_region(move(region))
    {
    }

    size_t m_size { 0 };
    AllocationStrategy m_allocation_strategy { AllocationStrategy::Reserve };
    NonnullOwnPtr<Region> m_region;
};

class [[nodiscard]] KBuffer {
public:
    explicit KBuffer(RefPtr<KBufferImpl>&& impl)
        : m_impl(move(impl))
    {
    }

    [[nodiscard]] static OwnPtr<KBuffer> try_create_with_size(size_t size, u8 access = Region::Access::Read | Region::Access::Write, const char* name = "KBuffer", AllocationStrategy strategy = AllocationStrategy::Reserve)
    {
        auto impl = KBufferImpl::try_create_with_size(size, access, name, strategy);
        if (!impl)
            return {};
        return adopt_own(*new KBuffer(impl.release_nonnull()));
    }

    [[nodiscard]] static OwnPtr<KBuffer> try_create_with_bytes(ReadonlyBytes bytes, u8 access = Region::Access::Read | Region::Access::Write, const char* name = "KBuffer", AllocationStrategy strategy = AllocationStrategy::Reserve)
    {
        auto impl = KBufferImpl::try_create_with_bytes(bytes, access, name, strategy);
        if (!impl)
            return {};
        return adopt_own(*new KBuffer(impl.release_nonnull()));
    }

    [[nodiscard]] static KBuffer create_with_size(size_t size, u8 access = Region::Access::Read | Region::Access::Write, const char* name = "KBuffer", AllocationStrategy strategy = AllocationStrategy::Reserve)
    {
        return KBuffer(KBufferImpl::create_with_size(size, access, name, strategy));
    }

    [[nodiscard]] static KBuffer copy(const void* data, size_t size, u8 access = Region::Access::Read | Region::Access::Write, const char* name = "KBuffer")
    {
        return KBuffer(KBufferImpl::copy(data, size, access, name));
    }

    [[nodiscard]] bool is_null() const { return !m_impl; }

    [[nodiscard]] u8* data() { return m_impl ? m_impl->data() : nullptr; }
    [[nodiscard]] const u8* data() const { return m_impl ? m_impl->data() : nullptr; }
    [[nodiscard]] size_t size() const { return m_impl ? m_impl->size() : 0; }
    [[nodiscard]] size_t capacity() const { return m_impl ? m_impl->capacity() : 0; }

    [[nodiscard]] void* end_pointer() { return data() + size(); }
    [[nodiscard]] const void* end_pointer() const { return data() + size(); }

    void set_size(size_t size) { m_impl->set_size(size); }

    [[nodiscard]] const KBufferImpl& impl() const { return *m_impl; }
    [[nodiscard]] RefPtr<KBufferImpl> take_impl() { return move(m_impl); }

    KBuffer(const ByteBuffer& buffer, u8 access = Region::Access::Read | Region::Access::Write, const char* name = "KBuffer")
        : m_impl(KBufferImpl::copy(buffer.data(), buffer.size(), access, name))
    {
    }

private:
    RefPtr<KBufferImpl> m_impl;
};

inline const LogStream& operator<<(const LogStream& stream, const KBuffer& value)
{
    return stream << StringView(value.data(), value.size());
}

}
