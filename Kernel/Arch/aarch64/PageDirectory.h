/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, Timon Kruiper <timonkruiper@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/AtomicRefCounted.h>
#include <AK/Badge.h>
#include <AK/HashMap.h>
#include <AK/IntrusiveRedBlackTree.h>
#include <AK/RefPtr.h>
#include <AK/StdLibExtras.h>
#include <AK/Types.h>
#include <Kernel/Forward.h>
#include <Kernel/Locking/Spinlock.h>
#include <Kernel/Memory/MemoryType.h>
#include <Kernel/Memory/PhysicalAddress.h>
#include <Kernel/Memory/PhysicalRAMPage.h>

namespace Kernel::Memory {

// 4KiB page size was chosen to make this code slightly simpler
constexpr size_t GRANULE_SIZE = 0x1000;
constexpr size_t PAGE_TABLE_SIZE = 0x1000;

// Documentation for translation table format
// https://developer.arm.com/documentation/101811/0101/Controlling-address-translation
constexpr u64 PAGE_DESCRIPTOR = 0b11;
constexpr u64 TABLE_DESCRIPTOR = 0b11;
constexpr u64 DESCRIPTOR_MASK = ~0b11;

constexpr u64 ACCESS_FLAG = 1 << 10;

// shareability
constexpr u64 OUTER_SHAREABLE = (2 << 8);
constexpr u64 INNER_SHAREABLE = (3 << 8);

// these index into the MAIR attribute table
constexpr u64 NORMAL_MEMORY = (0 << 2);
constexpr u64 DEVICE_MEMORY = (1 << 2);
constexpr u64 NORMAL_NONCACHEABLE_MEMORY = (2 << 2);
constexpr u64 ATTR_INDX_MASK = (0b111 << 2);

constexpr u64 ACCESS_PERMISSION_EL0 = (1 << 6);
constexpr u64 ACCESS_PERMISSION_READONLY = (1 << 7);

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

    bool is_user_allowed() const { TODO_AARCH64(); }
    void set_user_allowed(bool) { }

    bool is_huge() const { TODO_AARCH64(); }
    void set_huge(bool) { }

    bool is_writable() const { TODO_AARCH64(); }
    void set_writable(bool) { }

    void set_memory_type(MemoryType) { }

    bool is_global() const { TODO_AARCH64(); }
    void set_global(bool) { }

    bool is_execute_disabled() const { TODO_AARCH64(); }
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
    void set_present(bool b) { set_bit(Present, b); }

    bool is_user_allowed() const { return (raw() & ACCESS_PERMISSION_EL0) == ACCESS_PERMISSION_EL0; }
    void set_user_allowed(bool b) { set_bit(ACCESS_PERMISSION_EL0, b); }

    bool is_writable() const { return !((raw() & ACCESS_PERMISSION_READONLY) == ACCESS_PERMISSION_READONLY); }
    void set_writable(bool b) { set_bit(ACCESS_PERMISSION_READONLY, !b); }

    void set_memory_type(MemoryType t)
    {
        m_raw &= ~ATTR_INDX_MASK;
        if (t == MemoryType::Normal)
            m_raw |= NORMAL_MEMORY;
        else if (t == MemoryType::NonCacheable)
            m_raw |= NORMAL_NONCACHEABLE_MEMORY;
        else if (t == MemoryType::IO)
            m_raw |= DEVICE_MEMORY;
    }

    bool is_global() const { TODO_AARCH64(); }
    void set_global(bool) { }

    bool is_execute_disabled() const { TODO_AARCH64(); }
    void set_execute_disabled(bool) { }

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

class PageDirectory final : public AtomicRefCounted<PageDirectory> {
    friend class MemoryManager;

public:
    static ErrorOr<NonnullLockRefPtr<PageDirectory>> try_create_for_userspace(Process&);
    static NonnullLockRefPtr<PageDirectory> must_create_kernel_page_directory();
    static LockRefPtr<PageDirectory> find_current();

    ~PageDirectory();

    void allocate_kernel_directory();

    FlatPtr ttbr0() const
    {
        return m_root_table->paddr().get();
    }

    bool is_root_table_initialized() const
    {
        return m_root_table;
    }

    Process* process() { return m_process; }

    RecursiveSpinlock<LockRank::None>& get_lock() { return m_lock; }

    // This has to be public to let the global singleton access the member pointer
    IntrusiveRedBlackTreeNode<FlatPtr, PageDirectory, RawPtr<PageDirectory>> m_tree_node;

private:
    PageDirectory();
    static void register_page_directory(PageDirectory* directory);
    static void deregister_page_directory(PageDirectory* directory);

    Process* m_process { nullptr };
    RefPtr<PhysicalRAMPage> m_root_table;
    RefPtr<PhysicalRAMPage> m_directory_table;
    RefPtr<PhysicalRAMPage> m_directory_pages[512];
    RecursiveSpinlock<LockRank::None> m_lock {};
};

void activate_kernel_page_directory(PageDirectory const& pgd);
void activate_page_directory(PageDirectory const& pgd, Thread* current_thread);

}
