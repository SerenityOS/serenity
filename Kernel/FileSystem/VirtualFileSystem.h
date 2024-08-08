/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2024, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Badge.h>
#include <AK/Error.h>
#include <AK/Function.h>
#include <AK/HashMap.h>
#include <AK/OwnPtr.h>
#include <AK/RefPtr.h>
#include <Kernel/FileSystem/CustodyBase.h>
#include <Kernel/FileSystem/FileBackedFileSystem.h>
#include <Kernel/FileSystem/FileSystem.h>
#include <Kernel/FileSystem/Initializer.h>
#include <Kernel/FileSystem/InodeIdentifier.h>
#include <Kernel/FileSystem/InodeMetadata.h>
#include <Kernel/FileSystem/Mount.h>
#include <Kernel/FileSystem/MountFile.h>
#include <Kernel/FileSystem/UnveilNode.h>
#include <Kernel/FileSystem/VFSRootContext.h>
#include <Kernel/Forward.h>
#include <Kernel/Locking/MutexProtected.h>
#include <Kernel/Locking/SpinlockProtected.h>

namespace Kernel {

// Kernel internal options.
#define O_NOFOLLOW_NOERROR (1 << 29)
#define O_UNLINK_INTERNAL (1 << 30)

struct UidAndGid {
    UserID uid;
    GroupID gid;
};

enum class AccessFlags {
    None = 0,
    EffectiveAccess = 1 << 0,
    DoNotFollowSymlinks = 1 << 1,
};

AK_ENUM_BITWISE_OPERATORS(AccessFlags);

namespace VirtualFileSystem {

// Required to be at least 8 by POSIX
// https://pubs.opengroup.org/onlinepubs/9699919799/basedefs/limits.h.html
static constexpr int symlink_recursion_limit = 8;

bool check_matching_absolute_path_hierarchy(Custody const& first_custody, Custody const& second_custody);

ErrorOr<FileSystemInitializer const*> find_filesystem_type_initializer(StringView fs_type);

ErrorOr<void> remove_mount(Mount& mount, FileBackedFileSystem::List& file_backed_fs_list);

ErrorOr<void> mount(VFSRootContext&, MountFile&, OpenFileDescription*, Custody& mount_point, int flags);
ErrorOr<void> pivot_root_by_copying_mounted_fs_instance(VFSRootContext&, FileSystem& fs, int root_mount_flags);

ErrorOr<void> bind_mount(VFSRootContext&, Custody& source, Custody& mount_point, int flags);
ErrorOr<void> copy_mount(Custody& source, VFSRootContext& destination, Custody& mount_point, int flags);
ErrorOr<void> remount(VFSRootContext&, Custody& mount_point, int new_flags);
ErrorOr<void> unmount(VFSRootContext&, Custody& mount_point);
ErrorOr<void> unmount(VFSRootContext&, Inode& guest_inode, StringView custody_path);

ErrorOr<NonnullRefPtr<OpenFileDescription>> open(VFSRootContext const&, Credentials const&, StringView path, int options, mode_t mode, CustodyBase const& base, Optional<UidAndGid> = {});
ErrorOr<NonnullRefPtr<OpenFileDescription>> open(Process const&, VFSRootContext const&, Credentials const&, StringView path, int options, mode_t mode, CustodyBase const& base, Optional<UidAndGid> = {});
ErrorOr<NonnullRefPtr<OpenFileDescription>> create(Credentials const&, StringView path, int options, mode_t mode, Custody& parent_custody, Optional<UidAndGid> = {});
ErrorOr<NonnullRefPtr<OpenFileDescription>> create(Process const&, Credentials const&, StringView path, int options, mode_t mode, Custody& parent_custody, Optional<UidAndGid> = {});
ErrorOr<void> mkdir(VFSRootContext const&, Credentials const&, StringView path, mode_t mode, CustodyBase const& base);
ErrorOr<void> link(VFSRootContext const&, Credentials const&, StringView old_path, StringView new_path, CustodyBase const& base);
ErrorOr<void> unlink(VFSRootContext const&, Credentials const&, StringView path, CustodyBase const& base);
ErrorOr<void> symlink(VFSRootContext const&, Credentials const&, StringView target, StringView linkpath, CustodyBase const& base);
ErrorOr<void> rmdir(VFSRootContext const&, Credentials const&, StringView path, CustodyBase const& base);
ErrorOr<void> chmod(VFSRootContext const&, Credentials const&, StringView path, mode_t, CustodyBase const& base, int options = 0);
ErrorOr<void> chmod(Credentials const&, Custody&, mode_t);
ErrorOr<void> chown(VFSRootContext const&, Credentials const&, StringView path, UserID, GroupID, CustodyBase const& base, int options);
ErrorOr<void> chown(Credentials const&, Custody&, UserID, GroupID);
ErrorOr<void> access(VFSRootContext const&, Credentials const&, StringView path, int mode, CustodyBase const& base, AccessFlags);
ErrorOr<InodeMetadata> lookup_metadata(VFSRootContext const&, Credentials const&, StringView path, CustodyBase const& base, int options = 0);
ErrorOr<void> utime(VFSRootContext const&, Credentials const&, StringView path, CustodyBase const& base, time_t atime, time_t mtime);
ErrorOr<void> utimensat(VFSRootContext const&, Credentials const&, StringView path, CustodyBase const& base, timespec const& atime, timespec const& mtime, int options = 0);
ErrorOr<void> do_utimens(Credentials const&, Custody& custody, timespec const& atime, timespec const& mtime);
ErrorOr<void> rename(VFSRootContext const&, Credentials const&, CustodyBase const& old_base, StringView oldpath, CustodyBase const& new_base, StringView newpath);
ErrorOr<void> mknod(VFSRootContext const&, Credentials const&, StringView path, mode_t, dev_t, CustodyBase const& base);
ErrorOr<NonnullRefPtr<Custody>> open_directory(VFSRootContext const&, Credentials const&, StringView path, CustodyBase const& base);

ErrorOr<NonnullRefPtr<Custody>> resolve_path(VFSRootContext const&, Credentials const&, StringView path, CustodyBase const& base, RefPtr<Custody>* out_parent = nullptr, int options = 0, int symlink_recursion_level = 0);
ErrorOr<NonnullRefPtr<Custody>> resolve_path(Process const&, VFSRootContext const&, Credentials const&, StringView path, CustodyBase const& base, RefPtr<Custody>* out_parent = nullptr, int options = 0, int symlink_recursion_level = 0);
ErrorOr<NonnullRefPtr<Custody>> resolve_path_without_veil(VFSRootContext const&, Credentials const&, StringView path, NonnullRefPtr<Custody> base, RefPtr<Custody>* out_parent = nullptr, int options = 0, int symlink_recursion_level = 0);

void sync_filesystems();

};

}
