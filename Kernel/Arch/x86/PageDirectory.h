/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Badge.h>
#include <AK/Types.h>
#include <Kernel/Forward.h>
#include <Kernel/PhysicalAddress.h>

#include <AK/Platform.h>
VALIDATE_IS_X86()

namespace Kernel {

class PageDirectoryEntry {
public:
    PhysicalPtr page_table_base() const { return PhysicalAddress::physical_page_base(m_raw); }
    void set_page_table_base(u32 value)
    {
        m_raw &= 0x8000000000000fffULL;
        m_raw |= PhysicalAddress::physical_page_base(value);
    }

    bool is_null() const { return m_raw == 0; }
    void clear() { m_raw = 0; }

    u64 raw() const { return m_raw; }
    void copy_from(Badge<Memory::PageDirectory>, const PageDirectoryEntry& other) { m_raw = other.m_raw; }

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

    bool is_present() const { return (raw() & Present) == Present; }
    void set_present(bool b) { set_bit(Present, b); }

    bool is_user_allowed() const { return (raw() & UserSupervisor) == UserSupervisor; }
    void set_user_allowed(bool b) { set_bit(UserSupervisor, b); }

    bool is_huge() const { return (raw() & Huge) == Huge; }
    void set_huge(bool b) { set_bit(Huge, b); }

    bool is_writable() const { return (raw() & ReadWrite) == ReadWrite; }
    void set_writable(bool b) { set_bit(ReadWrite, b); }

    bool is_write_through() const { return (raw() & WriteThrough) == WriteThrough; }
    void set_write_through(bool b) { set_bit(WriteThrough, b); }

    bool is_cache_disabled() const { return (raw() & CacheDisabled) == CacheDisabled; }
    void set_cache_disabled(bool b) { set_bit(CacheDisabled, b); }

    bool is_global() const { return (raw() & Global) == Global; }
    void set_global(bool b) { set_bit(Global, b); }

    bool is_execute_disabled() const { return (raw() & NoExecute) == NoExecute; }
    void set_execute_disabled(bool b) { set_bit(NoExecute, b); }

private:
    void set_bit(u64 bit, bool value)
    {
        if (value)
            m_raw |= bit;
        else
            m_raw &= ~bit;
    }

    u64 m_raw;
};

class PageTableEntry {
public:
    PhysicalPtr physical_page_base() const { return PhysicalAddress::physical_page_base(m_raw); }
    void set_physical_page_base(PhysicalPtr value)
    {
        m_raw &= 0x8000000000000fffULL;
        m_raw |= PhysicalAddress::physical_page_base(value);
    }

    u64 raw() const { return m_raw; }

    enum Flags {
        Present = 1 << 0,
        ReadWrite = 1 << 1,
        UserSupervisor = 1 << 2,
        WriteThrough = 1 << 3,
        CacheDisabled = 1 << 4,
        Global = 1 << 8,
        NoExecute = 0x8000000000000000ULL,
    };

    bool is_present() const { return (raw() & Present) == Present; }
    void set_present(bool b) { set_bit(Present, b); }

    bool is_user_allowed() const { return (raw() & UserSupervisor) == UserSupervisor; }
    void set_user_allowed(bool b) { set_bit(UserSupervisor, b); }

    bool is_writable() const { return (raw() & ReadWrite) == ReadWrite; }
    void set_writable(bool b) { set_bit(ReadWrite, b); }

    bool is_write_through() const { return (raw() & WriteThrough) == WriteThrough; }
    void set_write_through(bool b) { set_bit(WriteThrough, b); }

    bool is_cache_disabled() const { return (raw() & CacheDisabled) == CacheDisabled; }
    void set_cache_disabled(bool b) { set_bit(CacheDisabled, b); }

    bool is_global() const { return (raw() & Global) == Global; }
    void set_global(bool b) { set_bit(Global, b); }

    bool is_execute_disabled() const { return (raw() & NoExecute) == NoExecute; }
    void set_execute_disabled(bool b) { set_bit(NoExecute, b); }

    bool is_null() const { return m_raw == 0; }
    void clear() { m_raw = 0; }

private:
    void set_bit(u64 bit, bool value)
    {
        if (value)
            m_raw |= bit;
        else
            m_raw &= ~bit;
    }

    u64 m_raw;
};

static_assert(AssertSize<PageDirectoryEntry, 8>());
static_assert(AssertSize<PageTableEntry, 8>());

class PageDirectoryPointerTable {
public:
    PageDirectoryEntry* directory(size_t index)
    {
        VERIFY(index <= (NumericLimits<size_t>::max() << 30));
        return (PageDirectoryEntry*)(PhysicalAddress::physical_page_base(raw[index]));
    }

    u64 raw[512];
};

}
