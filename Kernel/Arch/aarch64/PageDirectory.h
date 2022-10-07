/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, Timon Kruiper <timonkruiper@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Badge.h>
#include <AK/Types.h>
#include <Kernel/Forward.h>
#include <Kernel/PhysicalAddress.h>

namespace Kernel {

// 4KiB page size was chosen to make this code slightly simpler
constexpr u32 GRANULE_SIZE = 0x1000;
constexpr u32 PAGE_TABLE_SIZE = 0x1000;

// Documentation for translation table format
// https://developer.arm.com/documentation/101811/0101/Controlling-address-translation
constexpr u32 PAGE_DESCRIPTOR = 0b11;
constexpr u32 TABLE_DESCRIPTOR = 0b11;
constexpr u32 DESCRIPTOR_MASK = ~0b11;

constexpr u32 ACCESS_FLAG = 1 << 10;

// shareability
constexpr u32 OUTER_SHAREABLE = (2 << 8);
constexpr u32 INNER_SHAREABLE = (3 << 8);

// these index into the MAIR attribute table
constexpr u32 NORMAL_MEMORY = (0 << 2);
constexpr u32 DEVICE_MEMORY = (1 << 2);

// Figure D5-15 of Arm Architecture Reference Manual Armv8 - page D5-2588
class PageDirectoryEntry {
public:
    PhysicalPtr page_table_base() const { return PhysicalAddress::physical_page_base(m_raw); }
    void set_page_table_base(PhysicalPtr value)
    {
        m_raw &= 0xffff000000000fffULL;
        m_raw |= PhysicalAddress::physical_page_base(value);

        // FIXME: Do not hardcode this.
        m_raw |= TABLE_DESCRIPTOR;
    }

    bool is_null() const { return m_raw == 0; }
    void clear() { m_raw = 0; }

    u64 raw() const { return m_raw; }
    void copy_from(Badge<Memory::PageDirectory>, PageDirectoryEntry const& other) { m_raw = other.m_raw; }

    enum Flags {
        Present = 1 << 0,
    };

    bool is_present() const { return (raw() & Present) == Present; }
    void set_present(bool) { }

    bool is_user_allowed() const { VERIFY_NOT_REACHED(); }
    void set_user_allowed(bool) { }

    bool is_huge() const { VERIFY_NOT_REACHED(); }
    void set_huge(bool) { }

    bool is_writable() const { VERIFY_NOT_REACHED(); }
    void set_writable(bool) { }

    bool is_write_through() const { VERIFY_NOT_REACHED(); }
    void set_write_through(bool) { }

    bool is_cache_disabled() const { VERIFY_NOT_REACHED(); }
    void set_cache_disabled(bool) { }

    bool is_global() const { VERIFY_NOT_REACHED(); }
    void set_global(bool) { }

    bool is_execute_disabled() const { VERIFY_NOT_REACHED(); }
    void set_execute_disabled(bool) { }

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

// Figure D5-17 VMSAv8-64 level 3 descriptor format of Arm Architecture Reference Manual Armv8 - page D5-2592
class PageTableEntry {
public:
    PhysicalPtr physical_page_base() const { return PhysicalAddress::physical_page_base(m_raw); }
    void set_physical_page_base(PhysicalPtr value)
    {
        m_raw &= 0xffff000000000fffULL;
        m_raw |= PhysicalAddress::physical_page_base(value);

        // FIXME: For now we map everything with the same permissions.
        u64 normal_memory_flags = ACCESS_FLAG | PAGE_DESCRIPTOR | INNER_SHAREABLE | NORMAL_MEMORY;
        m_raw |= normal_memory_flags;
    }

    u64 raw() const { return m_raw; }

    enum Flags {
        Present = 1 << 0,
    };

    bool is_present() const { return (raw() & Present) == Present; }
    void set_present(bool) { }

    bool is_user_allowed() const { VERIFY_NOT_REACHED(); }
    void set_user_allowed(bool) { }

    bool is_writable() const { VERIFY_NOT_REACHED(); }
    void set_writable(bool) { }

    bool is_write_through() const { VERIFY_NOT_REACHED(); }
    void set_write_through(bool) { }

    bool is_cache_disabled() const { VERIFY_NOT_REACHED(); }
    void set_cache_disabled(bool) { }

    bool is_global() const { VERIFY_NOT_REACHED(); }
    void set_global(bool) { }

    bool is_execute_disabled() const { VERIFY_NOT_REACHED(); }
    void set_execute_disabled(bool) { }

    bool is_pat() const { VERIFY_NOT_REACHED(); }
    void set_pat(bool) { }

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
