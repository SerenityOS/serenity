/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Debug.h>
#include <Kernel/Devices/BlockDevice.h>
#include <Kernel/FileSystem/ProcFS.h>
#include <Kernel/FileSystem/VirtualFileSystem.h>
#include <Kernel/KBufferBuilder.h>
#include <Kernel/PerformanceEventBuffer.h>
#include <Kernel/Process.h>
#include <Kernel/ProcessExposed.h>

namespace Kernel {

static Spinlock s_index_lock { LockRank::None };
static InodeIndex s_next_inode_index = 0;

namespace SegmentedProcFSIndex {
static InodeIndex __build_raw_segmented_index(u32 primary, u16 sub_directory, u32 property)
{
    VERIFY(primary < 0x10000000);
    VERIFY(property < 0x100000);
    // Note: The sub-directory part is already limited to 0xFFFF, so no need to VERIFY it.
    return static_cast<u64>((static_cast<u64>(primary) << 36) | (static_cast<u64>(sub_directory) << 20) | property);
}

static InodeIndex build_segmented_index_with_known_pid(ProcessID pid, u16 sub_directory, u32 property)
{
    return __build_raw_segmented_index(pid.value() + 1, sub_directory, property);
}

static InodeIndex build_segmented_index_with_unknown_property(ProcessID pid, ProcessSubDirectory sub_directory, unsigned property)
{
    return build_segmented_index_with_known_pid(pid, to_underlying(sub_directory), static_cast<u32>(property));
}

InodeIndex build_segmented_index_for_pid_directory(ProcessID pid)
{
    return build_segmented_index_with_unknown_property(pid, ProcessSubDirectory::Reserved, to_underlying(MainProcessProperty::Reserved));
}

InodeIndex build_segmented_index_for_sub_directory(ProcessID pid, ProcessSubDirectory sub_directory)
{
    return build_segmented_index_with_unknown_property(pid, sub_directory, to_underlying(MainProcessProperty::Reserved));
}

InodeIndex build_segmented_index_for_main_property(ProcessID pid, ProcessSubDirectory sub_directory, MainProcessProperty property)
{
    return build_segmented_index_with_known_pid(pid, to_underlying(sub_directory), to_underlying(property));
}

InodeIndex build_segmented_index_for_main_property_in_pid_directory(ProcessID pid, MainProcessProperty property)
{
    return build_segmented_index_with_known_pid(pid, to_underlying(ProcessSubDirectory::Reserved), to_underlying(property));
}

InodeIndex build_segmented_index_for_thread_stack(ProcessID pid, ThreadID thread_id)
{
    return build_segmented_index_with_unknown_property(pid, ProcessSubDirectory::Stacks, thread_id.value());
}

InodeIndex build_segmented_index_for_file_description(ProcessID pid, unsigned fd)
{
    return build_segmented_index_with_unknown_property(pid, ProcessSubDirectory::OpenFileDescriptions, fd);
}

InodeIndex build_segmented_index_for_children(ProcessID pid, ProcessID child_pid)
{
    return build_segmented_index_with_unknown_property(pid, ProcessSubDirectory::Children, child_pid.value());
}

}

static size_t s_allocate_global_inode_index()
{
    SpinlockLocker lock(s_index_lock);
    s_next_inode_index = s_next_inode_index.value() + 1;
    // Note: Global ProcFS indices must be above 0 and up to maximum of what 36 bit (2 ^ 36 - 1) can represent.
    VERIFY(s_next_inode_index > 0);
    VERIFY(s_next_inode_index < 0x100000000);
    return s_next_inode_index.value();
}

ProcFSExposedComponent::ProcFSExposedComponent() = default;

ProcFSExposedComponent::ProcFSExposedComponent(StringView name)
    : m_component_index(s_allocate_global_inode_index())
{
    auto name_or_error = KString::try_create(name);
    if (name_or_error.is_error())
        TODO();
    m_name = name_or_error.release_value();
}

ProcFSExposedDirectory::ProcFSExposedDirectory(StringView name)
    : ProcFSExposedComponent(name)
{
}

ProcFSExposedDirectory::ProcFSExposedDirectory(StringView name, ProcFSExposedDirectory const& parent_directory)
    : ProcFSExposedComponent(name)
    , m_parent_directory(parent_directory)
{
}

ProcFSExposedLink::ProcFSExposedLink(StringView name)
    : ProcFSExposedComponent(name)
{
}

ErrorOr<size_t> ProcFSExposedLink::read_bytes(off_t offset, size_t count, UserOrKernelBuffer& buffer, OpenFileDescription*) const
{
    VERIFY(offset == 0);
    MutexLocker locker(m_lock);
    auto builder = TRY(KBufferBuilder::try_create());
    if (!const_cast<ProcFSExposedLink&>(*this).acquire_link(builder))
        return Error::from_errno(EFAULT);
    auto blob = builder.build();
    if (!blob)
        return Error::from_errno(EFAULT);

    ssize_t nread = min(static_cast<off_t>(blob->size() - offset), static_cast<off_t>(count));
    TRY(buffer.write(blob->data() + offset, nread));
    return nread;
}

ErrorOr<NonnullLockRefPtr<Inode>> ProcFSExposedLink::to_inode(ProcFS const& procfs_instance) const
{
    return TRY(ProcFSLinkInode::try_create(procfs_instance, *this));
}

ErrorOr<NonnullLockRefPtr<Inode>> ProcFSExposedDirectory::to_inode(ProcFS const& procfs_instance) const
{
    return TRY(ProcFSDirectoryInode::try_create(procfs_instance, *this));
}

void ProcFSExposedDirectory::add_component(ProcFSExposedComponent const&)
{
    TODO();
}

ErrorOr<NonnullLockRefPtr<ProcFSExposedComponent>> ProcFSExposedDirectory::lookup(StringView name)
{
    for (auto& component : m_components) {
        if (component.name() == name) {
            return component;
        }
    }
    return ENOENT;
}

ErrorOr<void> ProcFSExposedDirectory::traverse_as_directory(FileSystemID fsid, Function<ErrorOr<void>(FileSystem::DirectoryEntryView const&)> callback) const
{
    MutexLocker locker(ProcFSComponentRegistry::the().get_lock());
    auto parent_directory = m_parent_directory.strong_ref();
    if (parent_directory.is_null())
        return Error::from_errno(EINVAL);
    TRY(callback({ "."sv, { fsid, component_index() }, DT_DIR }));
    TRY(callback({ ".."sv, { fsid, parent_directory->component_index() }, DT_DIR }));

    for (auto const& component : m_components) {
        InodeIdentifier identifier = { fsid, component.component_index() };
        TRY(callback({ component.name(), identifier, 0 }));
    }
    return {};
}

class ProcFSSelfProcessDirectory final : public ProcFSExposedLink {
public:
    static NonnullLockRefPtr<ProcFSSelfProcessDirectory> must_create();

private:
    ProcFSSelfProcessDirectory();
    virtual bool acquire_link(KBufferBuilder& builder) override
    {
        return !builder.appendff("{}", Process::current().pid().value()).is_error();
    }
};
UNMAP_AFTER_INIT NonnullLockRefPtr<ProcFSSelfProcessDirectory> ProcFSSelfProcessDirectory::must_create()
{
    return adopt_lock_ref_if_nonnull(new (nothrow) ProcFSSelfProcessDirectory()).release_nonnull();
}
UNMAP_AFTER_INIT ProcFSSelfProcessDirectory::ProcFSSelfProcessDirectory()
    : ProcFSExposedLink("self"sv)
{
}

UNMAP_AFTER_INIT NonnullLockRefPtr<ProcFSRootDirectory> ProcFSRootDirectory::must_create()
{
    auto directory = adopt_lock_ref(*new (nothrow) ProcFSRootDirectory);
    directory->m_components.append(ProcFSSelfProcessDirectory::must_create());
    return directory;
}

ErrorOr<void> ProcFSRootDirectory::traverse_as_directory(FileSystemID fsid, Function<ErrorOr<void>(FileSystem::DirectoryEntryView const&)> callback) const
{
    MutexLocker locker(ProcFSComponentRegistry::the().get_lock());
    TRY(callback({ "."sv, { fsid, component_index() }, 0 }));
    TRY(callback({ ".."sv, { fsid, 0 }, 0 }));

    for (auto const& component : m_components) {
        InodeIdentifier identifier = { fsid, component.component_index() };
        TRY(callback({ component.name(), identifier, 0 }));
    }

    return Process::all_instances().with([&](auto& list) -> ErrorOr<void> {
        for (auto& process : list) {
            VERIFY(!(process.pid() < 0));
            u64 process_id = (u64)process.pid().value();
            InodeIdentifier identifier = { fsid, static_cast<InodeIndex>(process_id << 36) };
            auto process_id_string = TRY(KString::formatted("{:d}", process_id));
            TRY(callback({ process_id_string->view(), identifier, 0 }));
        }
        return {};
    });
}

ErrorOr<NonnullLockRefPtr<ProcFSExposedComponent>> ProcFSRootDirectory::lookup(StringView name)
{
    auto maybe_candidate = ProcFSExposedDirectory::lookup(name);
    if (maybe_candidate.is_error()) {
        if (maybe_candidate.error().code() != ENOENT) {
            return maybe_candidate.release_error();
        }
    } else {
        return maybe_candidate.release_value();
    }

    auto pid = name.to_uint<unsigned>();
    if (!pid.has_value())
        return ESRCH;
    auto actual_pid = pid.value();

    if (auto maybe_process = Process::from_pid(actual_pid))
        return maybe_process->procfs_traits();

    return ENOENT;
}

UNMAP_AFTER_INIT ProcFSRootDirectory::~ProcFSRootDirectory() = default;

UNMAP_AFTER_INIT ProcFSRootDirectory::ProcFSRootDirectory()
    : ProcFSExposedDirectory("."sv)
{
}

}
