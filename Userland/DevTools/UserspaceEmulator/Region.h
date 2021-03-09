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

#include "Range.h"
#include "ValueWithShadow.h"
#include <AK/TypeCasts.h>
#include <AK/Types.h>

namespace UserspaceEmulator {

class Emulator;

class Region {
public:
    virtual ~Region() { }

    const Range& range() const { return m_range; }

    u32 base() const { return m_range.base().get(); }
    u32 size() const { return m_range.size(); }
    u32 end() const { return m_range.end().get(); }

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

    bool is_stack() const { return m_stack; }
    void set_stack(bool b) { m_stack = b; }

    bool is_text() const { return m_text; }
    void set_text(bool b) { m_text = b; }

    bool is_readable() const { return m_readable; }
    bool is_writable() const { return m_writable; }
    bool is_executable() const { return m_executable; }

    void set_readable(bool b) { m_readable = b; }
    void set_writable(bool b) { m_writable = b; }
    void set_executable(bool b) { m_executable = b; }

    virtual u8* data() = 0;
    virtual u8* shadow_data() = 0;

    Emulator& emulator() { return m_emulator; }
    const Emulator& emulator() const { return m_emulator; }

    template<typename T>
    bool fast_is() const = delete;

protected:
    Region(u32 base, u32 size, bool mmap = false);
    void set_range(Range r) { m_range = r; };

private:
    Emulator& m_emulator;

    Range m_range;

    bool m_mmap { false };
    bool m_stack { false };
    bool m_text { false };
    bool m_readable { true };
    bool m_writable { true };
    bool m_executable { true };
};

}
