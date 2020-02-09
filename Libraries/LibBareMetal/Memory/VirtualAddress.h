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

#include <AK/LogStream.h>
#include <AK/Types.h>

class VirtualAddress {
public:
    VirtualAddress() {}
    explicit VirtualAddress(uintptr_t address)
        : m_address(address)
    {
    }

    explicit VirtualAddress(const void* address)
        : m_address((uintptr_t)address)
    {
    }

    bool is_null() const { return m_address == 0; }
    bool is_page_aligned() const { return (m_address & 0xfff) == 0; }

    VirtualAddress offset(uintptr_t o) const { return VirtualAddress(m_address + o); }
    uintptr_t get() const { return m_address; }
    void set(uintptr_t address) { m_address = address; }
    void mask(uintptr_t m) { m_address &= m; }

    bool operator<=(const VirtualAddress& other) const { return m_address <= other.m_address; }
    bool operator>=(const VirtualAddress& other) const { return m_address >= other.m_address; }
    bool operator>(const VirtualAddress& other) const { return m_address > other.m_address; }
    bool operator<(const VirtualAddress& other) const { return m_address < other.m_address; }
    bool operator==(const VirtualAddress& other) const { return m_address == other.m_address; }
    bool operator!=(const VirtualAddress& other) const { return m_address != other.m_address; }

    u8* as_ptr() { return reinterpret_cast<u8*>(m_address); }
    const u8* as_ptr() const { return reinterpret_cast<const u8*>(m_address); }

    VirtualAddress page_base() const { return VirtualAddress(m_address & 0xfffff000); }

private:
    uintptr_t m_address { 0 };
};

inline VirtualAddress operator-(const VirtualAddress& a, const VirtualAddress& b)
{
    return VirtualAddress(a.get() - b.get());
}

inline const LogStream& operator<<(const LogStream& stream, VirtualAddress value)
{
    return stream << 'V' << value.as_ptr();
}
