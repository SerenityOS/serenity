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
    return m_details.with([&](auto& details) -> ErrorOr<void> {
        for (auto& mount : details.mounts)
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
    TRY(context->m_details.with([&](auto& details) -> ErrorOr<void> {
        dbgln("VFSRootContext({}): Root (\"/\") FileSystemID {}, Mounting {} at inode {} with flags {}",
            context->id(),
            fs->fsid(),
            fs->class_name(),
            root_custody->inode().identifier(),
            0);
        add_to_mounts_list_and_increment_fs_mounted_count(DoBindMount::No, details.mounts, move(new_mount));
        return {};
    }));

    // Finally, add the context to the global list so it can be used.
    VFSRootContext::all_root_contexts_list().with([&](auto& list) {
        list.append(*context);
    });
    return context;
}

ErrorOr<void> VFSRootContext::pivot_root(FileBackedFileSystem::List& file_backed_file_systems_list, FileSystem& fs, NonnullOwnPtr<Mount> new_mount, NonnullRefPtr<Custody> root_mount_point, int root_mount_flags)
{
    return m_details.with([&](auto& details) -> ErrorOr<void> {
        return fs.mounted_count().with([&](auto& mounted_count) -> ErrorOr<void> {
            // NOTE: If the mounted count is 0, then this filesystem is about to be
            // deleted, so this must be a kernel bug as we don't include such filesystem
            // in the VirtualFileSystem s_details->file_backed_file_systems_list list anymore.
            VERIFY(mounted_count > 0);

            // NOTE: The mounts table should not be empty as it always need
            // to have at least one mount!
            VERIFY(!details.mounts.is_empty());

            // NOTE: If we have many mounts in the table, then simply don't allow
            // userspace to override them but instead require to unmount everything except
            // the root mount first.
            if (details.mounts.size_slow() != 1)
                return EPERM;

            auto& mount = *details.mounts.first();
            TRY(VirtualFileSystem::remove_mount(mount, file_backed_file_systems_list));
            VERIFY(details.mounts.is_empty());

            dbgln("VFSRootContext({}): Root mount set to FileSystemID {}, Mounting {} at inode {} with flags {}",
                id(),
                new_mount->guest_fs().fsid(),
                new_mount->guest_fs().class_name(),
                root_mount_point->inode().identifier(),
                root_mount_flags);

            // NOTE: Leak the mount pointer so it can be added to the mount list, but it won't be
            // deleted after being added.
            details.mounts.append(*new_mount.leak_ptr());

            // NOTE: We essentially do the same thing like VFSRootContext::add_to_mounts_list_and_increment_fs_mounted_count function
            // but because we already locked the spinlock of the attach count, we can't call that function here.
            mounted_count++;
            // NOTE: Now fill the root custody with a valid custody for the new root mount.
            m_root_custody.with([&root_mount_point](auto& custody) {
                custody = move(root_mount_point);
            });
            return {};
        });
    });
}

ErrorOr<void> VFSRootContext::do_full_teardown(Badge<PowerStateSwitchTask>)
{
    // NOTE: We are going to tear down the entire VFS root context from its mounts.
    // To do this properly, we swap out the original root custody with the empty
    // root custody for vfs root context of kernel processes.
    m_root_custody.with([](auto& custody) {
        custody = VFSRootContext::empty_context_custody_for_kernel_processes();
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

            TRY(VFSRootContext::validate_mount_not_immutable_while_being_used(details, mount));
            dbgln("VFSRootContext({}): Unmounting {}...", id(), custody_path);
            TRY(VirtualFileSystem::remove_mount(mount, file_backed_file_systems_list));
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

bool VFSRootContext::mount_point_exists_at_custody(Custody& mount_point)
{
    return m_details.with([&](auto& details) -> bool {
        return any_of(details.mounts, [&mount_point](auto const& existing_mount) {
            return existing_mount.host_custody() && VirtualFileSystem::check_matching_absolute_path_hierarchy(*existing_mount.host_custody(), mount_point);
        });
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

ErrorOr<VFSRootContext::CurrentMountState> VFSRootContext::current_mount_state_for_host_custody(Custody const& current_custody) const
{
    RefPtr<FileSystem> guest_fs;
    RefPtr<Inode> guest;
    int mount_flags_for_child { 0 };
    TRY(do_on_mount_for_host_custody(ValidateImmutableFlag::No, current_custody, [&guest, &guest_fs, &mount_flags_for_child](auto const& mount) {
        guest = mount.guest();
        guest_fs = mount.guest_fs();
        mount_flags_for_child = mount.flags();
    }));

    return VFSRootContext::CurrentMountState {
        Mount::Details { guest_fs.release_nonnull(), guest.release_nonnull() },
        mount_flags_for_child
    };
}

ErrorOr<void> VFSRootContext::add_new_mount(DoBindMount do_bind_mount, Inode& source, Custody& mount_point, int flags)
{
    auto new_mount = TRY(adopt_nonnull_own_or_enomem(new (nothrow) Mount(source, mount_point, flags)));
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

        if (mount_point_exists_at_custody(mount_point)) {
            dbgln("VFSRootContext({}): Mounting unsuccessful - inode {} is already a mount-point.", id(), mount_point.inode().identifier());
            return EBUSY;
        }
        add_to_mounts_list_and_increment_fs_mounted_count(do_bind_mount, details.mounts, move(new_mount));
        return {};
    });
}

}
