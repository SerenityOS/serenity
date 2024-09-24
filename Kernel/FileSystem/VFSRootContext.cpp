/*
 * Copyright (c) 2024, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/RAMFS/FileSystem.h>
#include <Kernel/FileSystem/VFSRootContext.h>
#include <Kernel/FileSystem/VirtualFileSystem.h>

namespace Kernel {

static RawPtr<VFSRootContext> s_empty_context;
static RawPtr<Custody> s_empty_context_custody;
static Atomic<u64> s_vfs_root_context_id = 0;

UNMAP_AFTER_INIT void VFSRootContext::initialize_empty_ramfs_root_context_for_kernel_processes()
{
    s_empty_context = &MUST(VFSRootContext::create_with_empty_ramfs()).leak_ref();
    // NOTE: This custody is immutable, so we expose it also outside of the SpinlockProtected
    // template so it could be accessed immediately.
    s_empty_context_custody = &s_empty_context->root_custody().with([](auto& custody) -> NonnullRefPtr<Custody> { return *custody; }).leak_ref();

    // NOTE: We remove the context from the vfs root contexts list because
    // we leaked a ref, and this context is artificially created only for
    // kernel processes.
    dbgln("VFSRootContext({}): Context is artificially made, detach from global list", s_empty_context->id());
    VFSRootContext::all_root_contexts_list().with([&](auto& list) {
        list.remove(*s_empty_context);
    });
}

VFSRootContext::VFSRootContext(NonnullRefPtr<Custody> custody)
    : m_root_custody(custody)
    , m_id(s_vfs_root_context_id.fetch_add(1))
{
}

VFSRootContext const& VFSRootContext::empty_context_for_kernel_processes()
{
    VERIFY(s_empty_context);
    return *s_empty_context;
}

Custody const& VFSRootContext::empty_context_custody_for_kernel_processes()
{
    VERIFY(s_empty_context_custody);
    return *s_empty_context_custody;
}

ErrorOr<void> VFSRootContext::for_each_mount(Function<ErrorOr<void>(Mount const&)> callback) const
{
    return mounts().with([&](auto& mounts) -> ErrorOr<void> {
        for (auto& mount : mounts)
            TRY(callback(mount));
        return {};
    });
}

void VFSRootContext::add_to_mounts_list_and_increment_fs_mounted_count(DoBindMount do_bind_mount, IntrusiveList<&Mount::m_vfs_list_node>& mounts_list, NonnullOwnPtr<Mount> new_mount)
{
    new_mount->guest_fs().mounted_count().with([&](auto& mounted_count) {
        // NOTE: We increment the mounted counter for the given filesystem regardless of the mount type,
        // as a bind mount also counts as a normal mount from the perspective of unmount(),
        // so we need to keep track of it in order for prepare_to_clear_last_mount() to work properly.
        mounted_count++;

        // NOTE: Add the filesystem to the file systems list if it's not a bind mount (this
        // condition is VERIFYed within the if-case) and this is the first time this FileSystem is mounted.
        // This is symmetric with VirtualFileSystem::unmount()'s `remove()` calls (which remove
        // the FileSystem once it is no longer mounted).
        if (mounted_count == 1) {
            // NOTE: If the mounted_count is 1, and we try to do a bind-mount on an inode
            // from this filesystem this means we have a bug because it's expected that
            // we will always have an already-mounted filesystem when creating a new bind-mount.
            //
            // Even in the odd case of mounting a new filesystem, creating a new bind mount
            // from a source Inode within the same filesystem and then removing the original mountpoint
            // we should still maintain a mounted_count > 1 if somehow new bind mounts from the filesystem inodes
            // appear.
            VERIFY(do_bind_mount != DoBindMount::Yes);

            FileSystem::all_file_systems_list().with([&](auto& fs_list) {
                fs_list.append(new_mount->guest_fs());
            });
        }
    });

    // NOTE: Leak the mount pointer so it can be added to the mount list, but it won't be
    // deleted after being added.
    mounts_list.append(*new_mount.leak_ptr());
}

ErrorOr<NonnullRefPtr<VFSRootContext>> VFSRootContext::create_with_empty_ramfs()
{
    auto fs = TRY(RAMFS::try_create({}));
    TRY(fs->initialize());
    auto root_custody = TRY(Custody::try_create(nullptr, ""sv, fs->root_inode(), 0));
    auto context = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) VFSRootContext(root_custody)));
    auto new_mount = TRY(adopt_nonnull_own_or_enomem(new (nothrow) Mount(fs->root_inode(), 0)));
    TRY(context->mounts().with([&](auto& mounts) -> ErrorOr<void> {
        dbgln("VFSRootContext({}): Root (\"/\") FileSystemID {}, Mounting {} at inode {} with flags {}",
            context->id(),
            fs->fsid(),
            fs->class_name(),
            root_custody->inode().identifier(),
            0);
        add_to_mounts_list_and_increment_fs_mounted_count(DoBindMount::No, mounts, move(new_mount));
        return {};
    }));

    // Finally, add the context to the global list so it can be used.
    VFSRootContext::all_root_contexts_list().with([&](auto& list) {
        list.append(*context);
    });
    return context;
}

void VFSRootContext::set_attached(Badge<Process>)
{
    m_attributes.with([](auto& attributes) {
        attributes.attached_by_process.set();
    });
}

bool VFSRootContext::mount_point_exists_at_custody(Custody& mount_point)
{
    return m_mounts.with([&](auto& mounts) -> bool {
        return any_of(mounts, [&mount_point](auto const& existing_mount) {
            return existing_mount.host_custody() && VirtualFileSystem::check_matching_absolute_path_hierarchy(*existing_mount.host_custody(), mount_point);
        });
    });
}

ErrorOr<void> VFSRootContext::add_new_mount(DoBindMount do_bind_mount, Inode& source, Custody& mount_point, int flags)
{
    auto new_mount = TRY(adopt_nonnull_own_or_enomem(new (nothrow) Mount(source, mount_point, flags)));
    return m_mounts.with([&](auto& mounts) -> ErrorOr<void> {
        // NOTE: The VFSRootContext should be attached to the list if there's
        // at least one mount in the mount table.
        // We also should have at least one mount in the table because
        // this method shouldn't be called for new contexts when adding
        // their root mounts.
        VERIFY(!mounts.is_empty());
        VFSRootContext::all_root_contexts_list().with([&](auto& list) {
            VERIFY(list.contains(*this));
        });

        VERIFY(&new_mount->guest_fs() == &source.fs());
        if (do_bind_mount == DoBindMount::No) {
            VERIFY(&source == &source.fs().root_inode());
            dbgln("VFSRootContext({}): FileSystemID {}, Mounting {} at inode {} with flags {}",
                id(),
                source.fs().fsid(),
                source.fs().class_name(),
                mount_point.inode().identifier(),
                flags);
        } else {
            dbgln("VFSRootContext({}): Bind-mounting inode {} at inode {}", id(), source.identifier(), mount_point.inode().identifier());
        }

        if (mount_point_exists_at_custody(mount_point)) {
            dbgln("VFSRootContext({}): Mounting unsuccessful - inode {} is already a mount-point.", id(), mount_point.inode().identifier());
            return EBUSY;
        }
        add_to_mounts_list_and_increment_fs_mounted_count(do_bind_mount, mounts, move(new_mount));
        return {};
    });
}

}
