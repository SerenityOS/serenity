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

#include <AK/Badge.h>
#include <AK/Types.h>

namespace Kernel {

class PageDirectory;
class PageTableEntry;

class PageDirectoryEntry {
public:
    const PageTableEntry* page_table_base() const { return reinterpret_cast<PageTableEntry*>(m_raw & 0xfffff000u); }
    PageTableEntry* page_table_base() { return reinterpret_cast<PageTableEntry*>(m_raw & 0xfffff000u); }
    void set_page_table_base(u32 value)
    {
        m_raw &= 0x8000000000000fffULL;
        m_raw |= value & 0xfffff000;
    }

    bool is_null() const { return m_raw == 0; }
    void clear() { m_raw = 0; }

    u64 raw() const { return m_raw; }
    void copy_from(Badge<PageDirectory>, const PageDirectoryEntry& other) { m_raw = other.m_raw; }

    enum Flags {
        Present = 1 << 0,
        ReadWrite = 1 << 1,
        UserSupervisor = 1 << 2,
        WriteThrough = 1 << 3,
        CacheDisabled = 1 << 4,
        Huge = 1 << 7,
        Global = 1 << 8,
        NoExecute = 0x8000000000000000ULL,
    };

    bool is_present() const { return raw() & Present; }
    void set_present(bool b) { set_bit(Present, b); }

    bool is_user_allowed() const { return raw() & UserSupervisor; }
    void set_user_allowed(bool b) { set_bit(UserSupervisor, b); }

    bool is_huge() const { return raw() & Huge; }
    void set_huge(bool b) { set_bit(Huge, b); }

    bool is_writable() const { return raw() & ReadWrite; }
    void set_writable(bool b) { set_bit(ReadWrite, b); }

    bool is_write_through() const { return raw() & WriteThrough; }
    void set_write_through(bool b) { set_bit(WriteThrough, b); }

    bool is_cache_disabled() const { return raw() & CacheDisabled; }
    void set_cache_disabled(bool b) { set_bit(CacheDisabled, b); }

    bool is_global() const { return raw() & Global; }
    void set_global(bool b) { set_bit(Global, b); }

    bool is_execute_disabled() const { return raw() & NoExecute; }
    void set_execute_disabled(bool b) { set_bit(NoExecute, b); }

    void set_bit(u64 bit, bool value)
    {
        if (value)
            m_raw |= bit;
        else
            m_raw &= ~bit;
    }

private:
    u64 m_raw;
};

class PageTableEntry {
public:
    void* physical_page_base() { return reinterpret_cast<void*>(m_raw & 0xfffff000u); }
    void set_physical_page_base(u32 value)
    {
        m_raw &= 0x8000000000000fffULL;
        m_raw |= value & 0xfffff000;
    }

    u64 raw() const { return (u32)m_raw; }

    enum Flags {
        Present = 1 << 0,
        ReadWrite = 1 << 1,
        UserSupervisor = 1 << 2,
        WriteThrough = 1 << 3,
        CacheDisabled = 1 << 4,
        Global = 1 << 8,
        NoExecute = 0x8000000000000000ULL,
    };

    bool is_present() const { return raw() & Present; }
    void set_present(bool b) { set_bit(Present, b); }

    bool is_user_allowed() const { return raw() & UserSupervisor; }
    void set_user_allowed(bool b) { set_bit(UserSupervisor, b); }

    bool is_writable() const { return raw() & ReadWrite; }
    void set_writable(bool b) { set_bit(ReadWrite, b); }

    bool is_write_through() const { return raw() & WriteThrough; }
    void set_write_through(bool b) { set_bit(WriteThrough, b); }

    bool is_cache_disabled() const { return raw() & CacheDisabled; }
    void set_cache_disabled(bool b) { set_bit(CacheDisabled, b); }

    bool is_global() const { return raw() & Global; }
    void set_global(bool b) { set_bit(Global, b); }

    bool is_execute_disabled() const { return raw() & NoExecute; }
    void set_execute_disabled(bool b) { set_bit(NoExecute, b); }

    bool is_null() const { return m_raw == 0; }
    void clear() { m_raw = 0; }

    void set_bit(u64 bit, bool value)
    {
        if (value)
            m_raw |= bit;
        else
            m_raw &= ~bit;
    }

private:
    u64 m_raw;
};

static_assert(sizeof(PageDirectoryEntry) == 8);
static_assert(sizeof(PageTableEntry) == 8);

class PageDirectoryPointerTable {
public:
    PageDirectoryEntry* directory(size_t index)
    {
        return (PageDirectoryEntry*)(raw[index] & ~0xfffu);
    }

    u64 raw[4];
};

}
