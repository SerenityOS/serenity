/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Range.h"
#include "ValueWithShadow.h"
#include <AK/TypeCasts.h>
#include <AK/Types.h>
#include <AK/UFixedBigInt.h>

namespace UserspaceEmulator {

class Emulator;

class Region {
public:
    virtual ~Region() = default;

    Range const& range() const { return m_range; }

    FlatPtr base() const { return m_range.base().get(); }
    FlatPtr size() const { return m_range.size(); }
    FlatPtr end() const { return m_range.end().get(); }

    bool contains(FlatPtr address) const { return address >= base() && address < end(); }

    virtual void write8(FlatPtr offset, ValueWithShadow<u8>) = 0;
    virtual void write16(FlatPtr offset, ValueWithShadow<u16>) = 0;
    virtual void write32(FlatPtr offset, ValueWithShadow<u32>) = 0;
    virtual void write64(FlatPtr offset, ValueWithShadow<u64>) = 0;
    virtual void write128(FlatPtr offset, ValueWithShadow<u128>) = 0;
    virtual void write256(FlatPtr offset, ValueWithShadow<u256>) = 0;

    virtual ValueWithShadow<u8> read8(FlatPtr offset) = 0;
    virtual ValueWithShadow<u16> read16(FlatPtr offset) = 0;
    virtual ValueWithShadow<u32> read32(FlatPtr offset) = 0;
    virtual ValueWithShadow<u64> read64(FlatPtr offset) = 0;
    virtual ValueWithShadow<u128> read128(FlatPtr offset) = 0;
    virtual ValueWithShadow<u256> read256(FlatPtr offset) = 0;

    virtual u8* cacheable_ptr([[maybe_unused]] FlatPtr offset) { return nullptr; }

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
    Emulator const& emulator() const { return m_emulator; }

    template<typename T>
    bool fast_is() const = delete;

protected:
    Region(FlatPtr base, FlatPtr size, bool mmap = false);
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
