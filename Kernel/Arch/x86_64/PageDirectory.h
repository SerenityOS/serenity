/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/AtomicRefCounted.h>
#include <AK/Badge.h>
#include <AK/HashMap.h>
#include <AK/IntrusiveRedBlackTree.h>
#include <AK/RefPtr.h>
#include <AK/Types.h>
#include <Kernel/Forward.h>
#include <Kernel/Locking/LockRank.h>
#include <Kernel/Locking/Spinlock.h>
#include <Kernel/Memory/MemoryType.h>
#include <Kernel/Memory/PhysicalAddress.h>
#include <Kernel/Memory/PhysicalRAMPage.h>

namespace Kernel::Memory {

class PageDirectoryEntry {
public:
    PhysicalPtr page_table_base() const { return PhysicalAddress::physical_page_base(m_raw); }
    void set_page_table_base(PhysicalPtr value)
    {
        m_raw &= 0x8000000000000fffULL;
        m_raw |= PhysicalAddress::physical_page_base(value);
    }

    bool is_null() const { return m_raw == 0; }
    void clear() { m_raw = 0; }

    u64 raw() const { return m_raw; }
    void copy_from(Badge<Memory::PageDirectory>, PageDirectoryEntry const& other) { m_raw = other.m_raw; }

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

    void set_memory_type(MemoryType t) { set_bit(CacheDisabled, t != MemoryType::Normal); }

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
        PAT = 1 << 7,
        Global = 1 << 8,
        NoExecute = 0x8000000000000000ULL,
    };

    bool is_present() const { return (raw() & Present) == Present; }
    void set_present(bool b) { set_bit(Present, b); }

    bool is_user_allowed() const { return (raw() & UserSupervisor) == UserSupervisor; }
    void set_user_allowed(bool b) { set_bit(UserSupervisor, b); }

    bool is_writable() const { return (raw() & ReadWrite) == ReadWrite; }
    void set_writable(bool b) { set_bit(ReadWrite, b); }

    void set_memory_type(MemoryType t)
    {
        // The PAT is indexed through the PWT (as bit 0), PCD (as bit 1), and PAT (as bit 2) bits.
        m_raw &= ~(WriteThrough | CacheDisabled | PAT);

        // We use the default PAT entries combined with a custom entry for PAT=b100, which maps to WC.
        // The default entries are backwards-compatible with systems without PAT (which only use the PWT and PCD bits for their original purpose).

        switch (t) {
        case MemoryType::Normal: // WB (write back) => PAT=0b000
            break;
        case MemoryType::NonCacheable: // WC (write combining) => PAT=0b100
            if (Processor::current().has_feature(CPUFeature::PAT)) {
                m_raw |= PAT;
                break;
            }

            // Fall back to MemoryType::IO if PAT is not supported.
            // TODO: Implement a MTRR fallback?
            [[fallthrough]];
        case MemoryType::IO: // UC- (uncacheable, can be overriden by WC in MTRRs) => PAT=0b010
            m_raw |= CacheDisabled;
        }
    }

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

class PageDirectory final : public AtomicRefCounted<PageDirectory> {
    friend class MemoryManager;

public:
    static ErrorOr<NonnullLockRefPtr<PageDirectory>> try_create_for_userspace(Process& process);
    static NonnullLockRefPtr<PageDirectory> must_create_kernel_page_directory();
    static LockRefPtr<PageDirectory> find_current();

    ~PageDirectory();

    void allocate_kernel_directory();

    FlatPtr cr3() const
    {
        return m_pml4t->paddr().get();
    }

    bool is_cr3_initialized() const
    {
        return m_pml4t;
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
    RefPtr<PhysicalRAMPage> m_pml4t;
    RefPtr<PhysicalRAMPage> m_directory_table;
    RefPtr<PhysicalRAMPage> m_directory_pages[512];
    RecursiveSpinlock<LockRank::None> m_lock {};
};

void activate_kernel_page_directory(PageDirectory const& pgd);
void activate_page_directory(PageDirectory const& pgd, Thread* current_thread);

}
