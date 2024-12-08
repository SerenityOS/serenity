/*
 * Copyright (c) 2023, Sönke Holz <sholz8530@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/AtomicRefCounted.h>
#include <AK/IntrusiveRedBlackTree.h>
#include <AK/RefPtr.h>

#include <Kernel/Arch/riscv64/VirtualMemoryDefinitions.h>
#include <Kernel/Forward.h>
#include <Kernel/Locking/Spinlock.h>
#include <Kernel/Memory/MemoryType.h>
#include <Kernel/Memory/PhysicalAddress.h>
#include <Kernel/Memory/PhysicalRAMPage.h>

#include <AK/Platform.h>
VALIDATE_IS_RISCV64()

namespace Kernel::Memory {

class PageDirectoryEntry {
public:
    PhysicalPtr page_table_base() const { return (m_raw >> PTE_PPN_OFFSET) << PADDR_PPN_OFFSET; }
    void set_page_table_base(PhysicalPtr value)
    {
        m_raw &= ~PTE_PPN_MASK;
        m_raw |= (value >> PADDR_PPN_OFFSET) << PTE_PPN_OFFSET;
    }

    void clear() { m_raw = 0; }

    bool is_present() const { return (m_raw & to_underlying(PageTableEntryBits::Valid)) != 0; }
    void set_present(bool b) { set_bit(PageTableEntryBits::Valid, b); }

    bool is_user_allowed() const { TODO_RISCV64(); }
    void set_user_allowed(bool) { }

    bool is_writable() const { TODO_RISCV64(); }
    void set_writable(bool) { }

    bool is_global() const { TODO_RISCV64(); }
    void set_global(bool) { }

private:
    void set_bit(PageTableEntryBits bit, bool value)
    {
        if (value)
            m_raw |= to_underlying(bit);
        else
            m_raw &= ~to_underlying(bit);
    }

    u64 m_raw;
};

class PageTableEntry {
public:
    PhysicalPtr physical_page_base() const { return PhysicalAddress::physical_page_base(m_raw); }
    void set_physical_page_base(PhysicalPtr value)
    {
        m_raw &= ~PTE_PPN_MASK;
        m_raw |= (value >> PADDR_PPN_OFFSET) << PTE_PPN_OFFSET;
    }

    bool is_present() const { return (m_raw & to_underlying(PageTableEntryBits::Valid)) != 0; }
    void set_present(bool b)
    {
        set_bit(PageTableEntryBits::Valid, b);
        set_bit(PageTableEntryBits::Readable, b);

        // Always set the A/D bits as we don't know if the hardware updates them automatically.
        // If the hardware doesn't update them automatically they act like additional permission bits.
        set_bit(PageTableEntryBits::Accessed, b);
        set_bit(PageTableEntryBits::Dirty, b);
    }

    bool is_user_allowed() const { TODO_RISCV64(); }
    void set_user_allowed(bool b) { set_bit(PageTableEntryBits::UserAllowed, b); }

    bool is_writable() const { return (m_raw & to_underlying(PageTableEntryBits::Writeable)) != 0; }
    void set_writable(bool b) { set_bit(PageTableEntryBits::Writeable, b); }

    void set_memory_type(MemoryType) { }

    bool is_global() const { TODO_RISCV64(); }
    void set_global(bool b) { set_bit(PageTableEntryBits::Global, b); }

    bool is_execute_disabled() const { TODO_RISCV64(); }
    void set_execute_disabled(bool b) { set_bit(PageTableEntryBits::Executable, !b); }

    bool is_null() const { return m_raw == 0; }
    void clear() { m_raw = 0; }

private:
    void set_bit(PageTableEntryBits bit, bool value)
    {
        if (value)
            m_raw |= to_underlying(bit);
        else
            m_raw &= ~to_underlying(bit);
    }

    u64 m_raw;
};

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

    RISCV64::CSR::SATP satp() const
    {
        return RISCV64::CSR::SATP {
            .PPN = m_directory_table->paddr().get() >> PADDR_PPN_OFFSET,
            .ASID = 0,
            .MODE = RISCV64::CSR::SATP::Mode::Sv39,
        };
    }

    bool is_root_table_initialized() const
    {
        return m_directory_table != nullptr;
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
    RefPtr<PhysicalRAMPage> m_directory_table;
    RefPtr<PhysicalRAMPage> m_directory_pages[512];
    RecursiveSpinlock<LockRank::None> m_lock {};
};

void activate_kernel_page_directory(PageDirectory const& page_directory);
void activate_page_directory(PageDirectory const& page_directory, Thread* current_thread);

}
