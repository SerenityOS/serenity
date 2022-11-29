/*
 * Copyright (c) 2019-2020, Jesse Buhagiar <jooster669@gmail.com>
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/IntrusiveListRelaxedConst.h>
#include <AK/OwnPtr.h>
#include <AK/Vector.h>
#include <Kernel/FileSystem/File.h>
#include <Kernel/Forward.h>
#include <Kernel/KBufferBuilder.h>
#include <Kernel/Library/ListedRefCounted.h>
#include <Kernel/Library/NonnullLockRefPtr.h>
#include <Kernel/Locking/SpinlockProtected.h>
#include <Kernel/Memory/Region.h>

namespace Kernel {

class CoredumpFSInode;
class CoredumpFile final
    : public ListedRefCounted<CoredumpFile, LockType::Spinlock>
    , public LockWeakable<Process> {

    friend class CoredumpFSInode;

public:
    static ErrorOr<NonnullLockRefPtr<CoredumpFile>> try_create(NonnullLockRefPtr<Process>);

    virtual ~CoredumpFile() = default;

    class FlatRegionData {
    public:
        explicit FlatRegionData(Memory::Region const& region, NonnullOwnPtr<KString> name)
            : m_access(region.access())
            , m_is_executable(region.is_executable())
            , m_is_kernel(region.is_kernel())
            , m_is_readable(region.is_readable())
            , m_is_writable(region.is_writable())
            , m_name(move(name))
            , m_page_count(region.page_count())
            , m_size(region.size())
            , m_vaddr(region.vaddr())
        {
        }

        auto access() const { return m_access; }
        auto name() const { return m_name->view(); }
        auto is_executable() const { return m_is_executable; }
        auto is_kernel() const { return m_is_kernel; }
        auto is_readable() const { return m_is_readable; }
        auto is_writable() const { return m_is_writable; }
        auto page_count() const { return m_page_count; }
        auto size() const { return m_size; }
        auto vaddr() const { return m_vaddr; }

        bool looks_like_userspace_heap_region() const;
        bool is_consistent_with_region(Memory::Region const& region) const;

    private:
        Memory::Region::Access m_access;
        bool m_is_executable;
        bool m_is_kernel;
        bool m_is_readable;
        bool m_is_writable;
        NonnullOwnPtr<KString> m_name;
        size_t m_page_count;
        size_t m_size;
        VirtualAddress m_vaddr;
    };

    ErrorOr<size_t> read(Badge<CoredumpFSInode>, u64, UserOrKernelBuffer&, size_t);
    void truncate(Badge<CoredumpFSInode>);

    bool process_was_associated_to_jail() const { return m_process_was_associated_to_jail; }
    LockWeakPtr<Jail> associated_jail() const { return m_associated_jail; }
    ProcessID associated_pid() const { return m_associated_pid; }
    size_t size() const;
    Time creation_time() const { return m_creation_time; }

    UserID associated_uid() const { return m_associated_uid; }
    GroupID associated_gid() const { return m_associated_gid; }

private:
    static ErrorOr<void> create_notes_threads_data(NonnullLockRefPtr<Process> process, KBufferBuilder& builder);
    static ErrorOr<void> create_notes_segment_data_buffer(NonnullLockRefPtr<Process> process, Vector<FlatRegionData>& regions, KBufferBuilder& builder);

    CoredumpFile(ProcessID, UserID, GroupID, Jail&, NonnullOwnPtr<KBuffer>);
    CoredumpFile(ProcessID, UserID, GroupID, NonnullOwnPtr<KBuffer>);

    LockWeakPtr<Jail> m_associated_jail;
    bool const m_process_was_associated_to_jail { false };
    ProcessID const m_associated_pid { 0 };
    UserID const m_associated_uid { 0 };
    GroupID const m_associated_gid { 0 };

    // NOTE: We allow these to be nullptr in case of truncating to zero.
    // This can be used in a situation when Userspace wants to retain a remainder of
    // that there was a coredump, without the penalty of having its content.
    OwnPtr<KBuffer> m_content;
    mutable Mutex m_content_lock;

    IntrusiveListNode<CoredumpFile, RefPtr<CoredumpFile>> m_list_node;

    Time const m_creation_time;

public:
    using List = IntrusiveListRelaxedConst<&CoredumpFile::m_list_node>;
    static ErrorOr<void> for_each_in_same_associated_jail(Function<ErrorOr<void>(CoredumpFile&)>);
    static LockRefPtr<CoredumpFile> from_pid_in_same_associated_jail(ProcessID pid);
    static SpinlockProtected<CoredumpFile::List>& all_instances();
};

}
