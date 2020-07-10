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

#include <AK/NonnullOwnPtrVector.h>
#include <AK/Types.h>

namespace UserspaceEmulator {

class SoftMMU {
public:
    class Region {
    public:
        virtual ~Region() { }

        u32 base() const { return m_base; }
        u32 size() const { return m_size; }
        u32 end() const { return m_base + m_size; }

        bool contains(u32 address) const { return address >= base() && address < end(); }

        virtual void write8(u32 offset, u8 value) = 0;
        virtual void write16(u32 offset, u16 value) = 0;
        virtual void write32(u32 offset, u32 value) = 0;

        virtual u8 read8(u32 offset) = 0;
        virtual u16 read16(u32 offset) = 0;
        virtual u32 read32(u32 offset) = 0;

    protected:
        Region(u32 base, u32 size)
            : m_base(base)
            , m_size(size)
        {
        }

    private:
        u32 m_base { 0 };
        u32 m_size { 0 };
    };

    u8 read8(u32 address);
    u16 read16(u32 address);
    u32 read32(u32 address);

    void write8(u32 address, u8 value);
    void write16(u32 address, u16 value);
    void write32(u32 address, u32 value);

    Region* find_region(u32 address);
    void add_region(NonnullOwnPtr<Region>);

private:
    NonnullOwnPtrVector<Region> m_regions;
};

}
