/*
 * Copyright (c) 2019-2020, Jesse Buhagiar <jooster669@gmail.com>
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/OwnPtr.h>
#include <AK/Vector.h>
#include <Kernel/Forward.h>
#include <Kernel/Library/NonnullLockRefPtr.h>
#include <Kernel/Locking/SpinlockProtected.h>
#include <Kernel/Memory/Region.h>

namespace Kernel {

class Coredump {
public:
    static ErrorOr<NonnullOwnPtr<Coredump>> try_create(NonnullRefPtr<Process>, StringView output_path);
    static SpinlockProtected<OwnPtr<KString>, LockRank::None>& directory_path();

    ~Coredump() = default;
    ErrorOr<void> write();

private:
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

    Coredump(NonnullRefPtr<Process>, NonnullRefPtr<OpenFileDescription>, Vector<FlatRegionData>);
    static ErrorOr<NonnullRefPtr<OpenFileDescription>> try_create_target_file(Process const&, StringView output_path);

    ErrorOr<void> write_elf_header();
    ErrorOr<void> write_program_headers(size_t notes_size);
    ErrorOr<void> write_regions();
    ErrorOr<void> write_notes_segment(ReadonlyBytes);

    ErrorOr<void> create_notes_segment_data(auto&) const;
    ErrorOr<void> create_notes_process_data(auto&) const;
    ErrorOr<void> create_notes_threads_data(auto&) const;
    ErrorOr<void> create_notes_regions_data(auto&) const;
    ErrorOr<void> create_notes_metadata_data(auto&) const;

    NonnullRefPtr<Process> const m_process;
    NonnullRefPtr<OpenFileDescription> const m_description;
    size_t m_num_program_headers { 0 };
    Vector<FlatRegionData> m_regions;
};

}
