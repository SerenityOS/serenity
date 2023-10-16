/*
 * Copyright (c) 2023, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>

namespace Disassembly {

class InstructionStream {
public:
    virtual bool can_read() = 0;
    virtual u8 read8() = 0;
    virtual u16 read16() = 0;
    virtual u32 read32() = 0;
    virtual u64 read64() = 0;

protected:
    virtual ~InstructionStream() = default;
};

class SimpleInstructionStream final : public InstructionStream {
public:
    SimpleInstructionStream(u8 const* data, size_t size)
        : m_data(data)
        , m_size(size)
    {
    }

    virtual bool can_read() override { return m_offset < m_size; }

    virtual u8 read8() override
    {
        if (!can_read())
            return 0;
        return m_data[m_offset++];
    }
    virtual u16 read16() override
    {
        u8 lsb = read8();
        u8 msb = read8();
        return ((u16)msb << 8) | (u16)lsb;
    }

    virtual u32 read32() override
    {
        u16 lsw = read16();
        u16 msw = read16();
        return ((u32)msw << 16) | (u32)lsw;
    }

    virtual u64 read64() override
    {
        u32 lsw = read32();
        u32 msw = read32();
        return ((u64)msw << 32) | (u64)lsw;
    }
    size_t offset() const { return m_offset; }

private:
    u8 const* m_data { nullptr };
    size_t m_offset { 0 };
    size_t m_size { 0 };
};

}
