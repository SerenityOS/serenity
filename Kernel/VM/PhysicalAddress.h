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

class PhysicalAddress {
public:
    PhysicalAddress() {}
    explicit PhysicalAddress(uintptr_t address)
        : m_address(address)
    {
    }

    PhysicalAddress offset(uintptr_t o) const { return PhysicalAddress(m_address + o); }
    uintptr_t get() const { return m_address; }
    void set(uintptr_t address) { m_address = address; }
    void mask(uintptr_t m) { m_address &= m; }

    bool is_null() const { return m_address == 0; }

    u8* as_ptr() { return reinterpret_cast<u8*>(m_address); }
    const u8* as_ptr() const { return reinterpret_cast<const u8*>(m_address); }

    uintptr_t page_base() const { return m_address & 0xfffff000; }

    bool operator==(const PhysicalAddress& other) const { return m_address == other.m_address; }
    bool operator!=(const PhysicalAddress& other) const { return m_address != other.m_address; }
    bool operator>(const PhysicalAddress& other) const { return m_address > other.m_address; }
    bool operator>=(const PhysicalAddress& other) const { return m_address >= other.m_address; }
    bool operator<(const PhysicalAddress& other) const { return m_address < other.m_address; }
    bool operator<=(const PhysicalAddress& other) const { return m_address <= other.m_address; }

private:
    uintptr_t m_address { 0 };
};

inline const LogStream& operator<<(const LogStream& stream, PhysicalAddress value)
{
    return stream << 'P' << value.as_ptr();
}
