/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
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

#include "ValueWithShadow.h"
#include <AK/HashMap.h>
#include <AK/NonnullOwnPtrVector.h>
#include <AK/OwnPtr.h>
#include <AK/Types.h>
#include <LibX86/Instruction.h>

namespace UserspaceEmulator {

class SharedBufferRegion;

class SoftMMU {
public:
    class Region {
    public:
        virtual ~Region() { }

        u32 base() const { return m_base; }
        u32 size() const { return m_size; }
        u32 end() const { return m_base + m_size; }

        bool contains(u32 address) const { return address >= base() && address < end(); }

        virtual void write8(u32 offset, ValueWithShadow<u8>) = 0;
        virtual void write16(u32 offset, ValueWithShadow<u16>) = 0;
        virtual void write32(u32 offset, ValueWithShadow<u32>) = 0;
        virtual void write64(u32 offset, ValueWithShadow<u64>) = 0;

        virtual ValueWithShadow<u8> read8(u32 offset) = 0;
        virtual ValueWithShadow<u16> read16(u32 offset) = 0;
        virtual ValueWithShadow<u32> read32(u32 offset) = 0;
        virtual ValueWithShadow<u64> read64(u32 offset) = 0;

        virtual u8* cacheable_ptr([[maybe_unused]] u32 offset) { return nullptr; }
        virtual bool is_shared_buffer() const { return false; }
        virtual bool is_mmap() const { return false; }

        bool is_stack() const { return m_stack; }
        void set_stack(bool b) { m_stack = b; }

        bool is_text() const { return m_text; }
        void set_text(bool b) { m_text = b; }

    protected:
        Region(u32 base, u32 size)
            : m_base(base)
            , m_size(size)
        {
        }

    private:
        u32 m_base { 0 };
        u32 m_size { 0 };

        bool m_stack { false };
        bool m_text { false };
    };

    ValueWithShadow<u8> read8(X86::LogicalAddress);
    ValueWithShadow<u16> read16(X86::LogicalAddress);
    ValueWithShadow<u32> read32(X86::LogicalAddress);
    ValueWithShadow<u64> read64(X86::LogicalAddress);

    void write8(X86::LogicalAddress, ValueWithShadow<u8>);
    void write16(X86::LogicalAddress, ValueWithShadow<u16>);
    void write32(X86::LogicalAddress, ValueWithShadow<u32>);
    void write64(X86::LogicalAddress, ValueWithShadow<u64>);

    Region* find_region(X86::LogicalAddress);

    void add_region(NonnullOwnPtr<Region>);
    void remove_region(Region&);

    void set_tls_region(NonnullOwnPtr<Region>);

    void copy_to_vm(FlatPtr destination, const void* source, size_t);
    void copy_from_vm(void* destination, const FlatPtr source, size_t);
    ByteBuffer copy_buffer_from_vm(const FlatPtr source, size_t);

    SharedBufferRegion* shbuf_region(int shbuf_id);

    template<typename Callback>
    void for_each_region(Callback callback)
    {
        if (m_tls_region) {
            if (callback(*m_tls_region) == IterationDecision::Break)
                return;
        }
        for (auto& region : m_regions) {
            if (callback(region) == IterationDecision::Break)
                return;
        }
    }

private:
    OwnPtr<Region> m_tls_region;
    NonnullOwnPtrVector<Region> m_regions;
    HashMap<int, Region*> m_shbuf_regions;
};

}
