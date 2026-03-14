/*
 * Copyright (c) 2024, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "CanonicalizedPath.h"
#include <AK/Singleton.h>
#include <Kernel/FileSystem/FileSystem.h>
#include <Kernel/FileSystem/RAMFS/FileSystem.h>
#include <Kernel/FileSystem/VFSRootContext.h>
#include <Kernel/FileSystem/VirtualFileSystem.h>

extern SetOnce g_not_in_early_boot;

namespace Kernel {

static RawPtr<VFSRootContext> s_empty_context;
static RawPtr<Custody> s_empty_context_custody;
static RawPtr<Mount> s_empty_context_mount;
static Atomic<u64> s_vfs_root_context_id = 0;

ErrorOr<NonnullRefPtr<VFSRootContext>> VFSRootContext::create_with_empty_ramfs(AddToGlobalContextList add_to_global_context_list)
{
    // NOTE: It's not expected that this will be called for creating a
    // context that will not be added into the global list after some
    // point during the boot process.
    if (g_not_in_early_boot.was_set())
        VERIFY(add_to_global_context_list == AddToGlobalContextList::Yes);

    auto fs = TRY(RAMFS::try_create({}));
    TRY(fs->initialize());
    return create_with_filesystem(add_to_global_context_list, fs);
}

ErrorOr<NonnullRefPtr<VFSRootContext>> VFSRootContext::create_with_filesystem(AddToGlobalContextList add_to_global_context_list, FileSystem& fs)
{
    auto root_custody = TRY(Custody::try_create(nullptr, ""sv, fs.root_inode(), 0));
    auto new_mount = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) Mount(fs.root_inode(), 0)));
    auto context = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) VFSRootContext(root_custody, new_mount, CanonicalizedPath::fake_root_path())));

    // NOTE: We passed the new mount to the VFSRootContext constructor because we don't want to allow
    // a null RefPtr to exist in the Path struct.
    // It should be noted, however, that we still need to add this mount to the mount table of new
    // VFSRootContext so it will appear there.
    TRY(context->m_details.with([&](auto& details) -> ErrorOr<void> {
        if (add_to_global_context_list == AddToGlobalContextList::Yes) {
            dbgln("VFSRootContext({}): Root (\"/\") FileSystemID {}, Mounting {} at inode {} with flags {}",
                context->id(),
                fs.fsid(),
                fs.class_name(),
                root_custody->inode().identifier(),
                0);
        }
        add_to_mounts_list_and_increment_fs_mounted_count(DoBindMount::No, details.mounts, move(new_mount));
        return {};
    }));

    if (add_to_global_context_list == AddToGlobalContextList::Yes) {
        // Finally, add the context to the global list so it can be used.
        VFSRootContext::all_root_contexts_list().with([&](auto& list) {
            list.append(*context);
        });
    }
    return context;
}

UNMAP_AFTER_INIT void VFSRootContext::initialize_empty_ramfs_root_context_for_kernel_processes()
{
    // NOTE: We don't add the context to the vfs root contexts list and
    // we leak a ref, as this context is artificially created only for
    // kernel processes.
    s_empty_context = &MUST(VFSRootContext::create_with_empty_ramfs(AddToGlobalContextList::No)).leak_ref();
    // NOTE: This custody is immutable, so we expose it also outside of the SpinlockProtected
    // template so it could be accessed immediately.
    // We do the same for the mount point.
    s_empty_context->m_root_path.with([&](auto& path) -> void {
        s_empty_context_custody = &path.custody();
        s_empty_context_mount = &path.mount();
    });
}

VFSRootContext::VFSRootContext(NonnullRefPtr<Custody> custody, NonnullRefPtr<Mount> mount, NonnullRefPtr<CanonicalizedPath> root_path)
    : m_root_path(Path(custody, mount, move(root_path)))
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

Mount const& VFSRootContext::empty_context_mount_for_kernel_processes()
{
    VERIFY(s_empty_context_mount);
    return *s_empty_context_mount;
}

ErrorOr<void> VFSRootContext::for_each_mount(Function<ErrorOr<void>(Mount const&)> callback) const
{
    return m_details.with([&](auto& details) -> ErrorOr<void> {
        for (auto& mount : details.mounts)
            TRY(callback(mount));
        return {};
    });
}

void VFSRootContext::add_to_mounts_list_and_increment_fs_mounted_count(DoBindMount do_bind_mount, IntrusiveList<&Mount::m_vfs_list_node>& mounts_list, NonnullRefPtr<Mount> new_mount)
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

    mounts_list.append(new_mount);
}

ErrorOr<void> VFSRootContext::remove_mount(Mount& mount, FileBackedFileSystem::List& file_backed_fs_list)
{
    NonnullRefPtr<FileSystem> fs = mount.guest_fs();
    fs->mounted_count().with([&](auto& mounted_count) {
        VERIFY(mounted_count > 0);
        if (mounted_count == 1) {
            dbgln("VirtualFileSystem: Unmounting file system {} for the last time...", fs->fsid());
            FileSystem::all_file_systems_list().with([&fs](auto& list) {
                list.remove(*fs);
            });
            if (fs->is_file_backed()) {
                dbgln("VirtualFileSystem: Unmounting file backed file system {} for the last time...", fs->fsid());
                auto& file_backed_fs = static_cast<FileBackedFileSystem&>(*fs);
                file_backed_fs_list.remove(file_backed_fs);
            }
        } else {
            mounted_count--;
        }
    });
    Mount::delete_mount_from_list(mount);
    return {};
}

ErrorOr<void> VFSRootContext::do_full_teardown(Badge<PowerStateSwitchTask>)
{
    // NOTE: We are going to tear down the entire VFS root context from its mounts.
    // To do this properly, we swap out the original root custody with the empty
    // root custody for vfs root context of kernel processes.
    m_root_path.with([&](auto& path) -> void {
        path = Path(VFSRootContext::empty_context_custody_for_kernel_processes(),
            VFSRootContext::empty_context_mount_for_kernel_processes(),
            CanonicalizedPath::fake_root_path());
    });

    auto unmount_was_successful = true;
    while (unmount_was_successful) {
        unmount_was_successful = false;
        Vector<Mount&, 16> mounts;
        TRY(m_details.with([&mounts](auto& details) -> ErrorOr<void> {
            for (auto& mount : details.mounts) {
                TRY(mounts.try_append(const_cast<Mount&>(mount)));
            }
            return {};
        }));
        if (mounts.is_empty())
            break;
        auto const remaining_mounts = mounts.size();

        while (!mounts.is_empty()) {
            auto& mount = mounts.take_last();
            TRY(mount.guest_fs().flush_writes());

            auto mount_path = TRY(mount.absolute_path());
            auto& mount_inode = mount.guest();
            auto const result = VirtualFileSystem::unmount(*this, mount_inode, mount_path->view());
            if (result.is_error()) {
                dbgln("Error during unmount of {}: {}", mount_path, result.error());
                // FIXME: For unknown reasons the root FS stays busy even after everything else has shut down and was unmounted.
                //        Until we find the underlying issue, allow an unclean shutdown here.
                if (remaining_mounts <= 1)
                    dbgln("BUG! One mount remaining; the root file system may not be unmountable at all. Shutting down anyways.");
            } else {
                unmount_was_successful = true;
            }
        }
    }
    return {};
}

ErrorOr<void> VFSRootContext::unmount(FileBackedFileSystem::List& file_backed_file_systems_list, Inode& guest_inode, StringView custody_path)
{
    return m_details.with([&](auto& details) -> ErrorOr<void> {
        bool did_unmount = false;
        for (auto& mount : details.mounts) {
            if (&mount.guest() != &guest_inode)
                continue;
            auto mountpoint_path = TRY(mount.absolute_path());
            if (custody_path != mountpoint_path->view())
                continue;

            auto mount_ref_count = mount.ref_count();
            // Technically, a reference count should be 2 under normal conditions (when the system is done booting
            // and userspace started), however, because during a shutdown the init process is dead, then the reference
            // count could be one.
            VERIFY(mount_ref_count >= 1);
            // The Mount object is referenced normally at the mount table
            // and also now, so if we have more than 2 in the refcounting, then
            // someone else uses the Mount, and we should refuse unmounting it.
            if (mount_ref_count > 2)
                return EBUSY;

            TRY(VFSRootContext::validate_mount_not_immutable_while_being_used(details, mount));
            dbgln("VFSRootContext({}): Unmounting {}...", id(), custody_path);
            TRY(VFSRootContext::remove_mount(mount, file_backed_file_systems_list));
            did_unmount = true;
            break;
        }
        if (!did_unmount) {
            dbgln("VFSRootContext: Nothing mounted on inode {}", guest_inode.identifier());
            return ENODEV;
        }

        // NOTE: The VFSRootContext mount table is not empty and we
        // successfully deleted the desired mount from it, so return
        // a success now.
        if (!details.mounts.is_empty())
            return {};

        // NOTE: If the mount table is empty, then the VFSRootContext
        // is no longer in valid state (each VFSRootContext at least should
        // have a root mount), so remove it now.
        VFSRootContext::all_root_contexts_list().with([this](auto& list) {
            dbgln("VFSRootContext: Nothing mounted in VFSRootContext({}), removing it", id());
            list.remove(*this);
        });
        return {};
    });
}

void VFSRootContext::detach(Badge<Process>)
{
    m_details.with([](auto& details) {
        VERIFY(details.attached_by_process.was_set());
        VERIFY(details.attach_count > 0);
        details.attach_count--;
    });
}

void VFSRootContext::attach(Badge<Process>)
{
    m_details.with([](auto& details) {
        details.attached_by_process.set();
        details.attach_count++;
    });
}

bool VFSRootContext::mount_point_exists_at_custody(Custody& mount_point, Details& details)
{
    return any_of(details.mounts, [&mount_point](auto const& existing_mount) {
        return existing_mount.host_custody() && VirtualFileSystem::check_matching_absolute_path_hierarchy(*existing_mount.host_custody(), mount_point);
    });
}

ErrorOr<void> VFSRootContext::do_on_mount_for_host_custody(ValidateImmutableFlag validate_immutable_flag, Custody const& current_custody, Function<void(Mount&)> callback) const
{
    VERIFY(validate_immutable_flag == ValidateImmutableFlag::Yes || validate_immutable_flag == ValidateImmutableFlag::No);
    return m_details.with([&](auto& details) -> ErrorOr<void> {
        // NOTE: We either search for the root mount or for a mount that has a parent custody!
        if (!current_custody.parent()) {
            for (auto& mount : details.mounts) {
                if (!mount.host_custody()) {
                    if (validate_immutable_flag == ValidateImmutableFlag::Yes)
                        TRY(VFSRootContext::validate_mount_not_immutable_while_being_used(details, mount));

                    callback(mount);
                    return {};
                }
            }
            // NOTE: There must be a root mount entry, so fail if we don't find it.
            VERIFY_NOT_REACHED();
        } else {
            for (auto& mount : details.mounts) {
                if (mount.host_custody() && VirtualFileSystem::check_matching_absolute_path_hierarchy(*mount.host_custody(), current_custody)) {
                    if (validate_immutable_flag == ValidateImmutableFlag::Yes)
                        TRY(VFSRootContext::validate_mount_not_immutable_while_being_used(details, mount));

                    callback(mount);
                    return {};
                }
            }
        }
        return Error::from_errno(ENODEV);
    });
}

ErrorOr<void> VFSRootContext::apply_to_mount_for_host_custody(Custody const& current_custody, Function<void(Mount&)> callback)
{
    return do_on_mount_for_host_custody(ValidateImmutableFlag::Yes, current_custody, move(callback));
}

ErrorOr<NonnullRefPtr<Mount>> VFSRootContext::find_custody_as_mountpoint(Custody const& custody) const
{
    return m_details.with([&](auto& details) -> ErrorOr<NonnullRefPtr<Mount>> {
        // NOTE: We either search for the root mount or for a mount that has a parent custody!
        if (!custody.parent()) {
            for (auto& mount : details.mounts) {
                if (!mount.host_custody())
                    return mount;
            }
            // There must be a root mount entry, so fail if we don't find it.
            VERIFY_NOT_REACHED();
        } else {
            for (auto& mount : details.mounts) {
                if (mount.host_custody() && VirtualFileSystem::check_matching_absolute_path_hierarchy(*mount.host_custody(), custody))
                    return mount;
            }
        }
        return ENODEV;
    });
}

ErrorOr<void> VFSRootContext::add_new_mount(DoBindMount do_bind_mount, Inode& source, Custody& mount_point, int flags)
{
    auto new_mount = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) Mount(source, mount_point, flags)));
    return m_details.with([&](auto& details) -> ErrorOr<void> {
        // NOTE: The VFSRootContext should be attached to the list if there's
        // at least one mount in the mount table.
        // We also should have at least one mount in the table because
        // this method shouldn't be called for new contexts when adding
        // their root mounts.
        VERIFY(!details.mounts.is_empty());
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

        if (mount_point_exists_at_custody(mount_point, details)) {
            dbgln("VFSRootContext({}): Mounting unsuccessful - inode {} is already a mount-point.", id(), mount_point.inode().identifier());
            return EBUSY;
        }
        add_to_mounts_list_and_increment_fs_mounted_count(do_bind_mount, details.mounts, move(new_mount));
        return {};
    });
}

}
