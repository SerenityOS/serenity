/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
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

#include <AK/Types.h>
#include <Kernel/VirtualAddress.h>

namespace UserspaceEmulator {

class Range {
    friend class RangeAllocator;

public:
    Range() = delete;
    Range(VirtualAddress base, size_t size)
        : m_base(base)
        , m_size(size)
    {
    }

    VirtualAddress base() const { return m_base; }
    size_t size() const { return m_size; }
    bool is_valid() const { return !m_base.is_null(); }

    bool contains(VirtualAddress vaddr) const { return vaddr >= base() && vaddr < end(); }

    VirtualAddress end() const { return m_base.offset(m_size); }

    bool operator==(const Range& other) const
    {
        return m_base == other.m_base && m_size == other.m_size;
    }

    bool contains(VirtualAddress base, size_t size) const
    {
        if (base.offset(size) < base)
            return false;
        return base >= m_base && base.offset(size) <= end();
    }

    bool contains(const Range& other) const
    {
        return contains(other.base(), other.size());
    }

    Vector<Range, 2> carve(const Range&) const;

    Range split_at(VirtualAddress address)
    {
        VERIFY(address.is_page_aligned());
        VERIFY(m_base < address);
        size_t new_size = (address - m_base).get();
        VERIFY(new_size < m_size);
        size_t other_size = m_size - new_size;
        m_size = new_size;
        return { address, other_size };
    }

private:
    VirtualAddress m_base;
    size_t m_size { 0 };
};

}

namespace AK {
template<>
struct Traits<UserspaceEmulator::Range> : public GenericTraits<UserspaceEmulator::Range> {
    static constexpr bool is_trivial() { return true; }
};
}
