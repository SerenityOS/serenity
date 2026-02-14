/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2024, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "UnresolvedPath.h"
#include <AK/Badge.h>
#include <AK/Error.h>
#include <AK/Function.h>
#include <AK/HashMap.h>
#include <AK/OwnPtr.h>
#include <AK/RefPtr.h>
#include <Kernel/FileSystem/FileBackedFileSystem.h>
#include <Kernel/FileSystem/FileSystem.h>
#include <Kernel/FileSystem/Initializer.h>
#include <Kernel/FileSystem/InodeIdentifier.h>
#include <Kernel/FileSystem/InodeMetadata.h>
#include <Kernel/FileSystem/Mount.h>
#include <Kernel/FileSystem/MountFile.h>
#include <Kernel/FileSystem/Path.h>
#include <Kernel/FileSystem/UnresolvedPath.h>
#include <Kernel/FileSystem/UnveilNode.h>
#include <Kernel/FileSystem/VFSRootContext.h>
#include <Kernel/Forward.h>

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

ErrorOr<void> mount(VFSRootContext&, MountFile&, OpenFileDescription*, Path& mount_point, int flags);

ErrorOr<void> bind_mount(VFSRootContext&, Custody& source, Path& mount_point, int flags);
ErrorOr<void> copy_mount(Path& source, VFSRootContext& destination, Path& mount_point, int flags);
ErrorOr<void> remount(VFSRootContext&, Path& mount_point, int new_flags);
ErrorOr<void> unmount(VFSRootContext&, Path& mount_point);
ErrorOr<void> unmount(VFSRootContext&, Inode& guest_inode, StringView custody_path);

ErrorOr<NonnullRefPtr<OpenFileDescription>> open(VFSRootContext const&, Credentials const&, UnresolvedPath const& path, int options, mode_t mode, Optional<UidAndGid> = {});
ErrorOr<NonnullRefPtr<OpenFileDescription>> open(Process const&, VFSRootContext const&, Credentials const&, UnresolvedPath const& path, int options, mode_t mode, Optional<UidAndGid> = {});
ErrorOr<NonnullRefPtr<OpenFileDescription>> create(Process const&, Credentials const&, UnresolvedPath const& path, int options, mode_t mode, Mount& parent_mount, Custody& parent_custody, Optional<UidAndGid> = {});
ErrorOr<void> mkdir(VFSRootContext const&, Credentials const&, UnresolvedPath const& target, mode_t mode);
ErrorOr<void> link(VFSRootContext const&, Credentials const&, UnresolvedPath const& source, UnresolvedPath const& target);
ErrorOr<void> unlink(VFSRootContext const&, Credentials const&, UnresolvedPath const& target);
ErrorOr<void> symlink(VFSRootContext const&, Credentials const&, UnresolvedPath const& target, UnresolvedPath const& linkpath);
ErrorOr<void> rmdir(VFSRootContext const&, Credentials const&, UnresolvedPath const&);
ErrorOr<void> chmod(VFSRootContext const&, Credentials const&, UnresolvedPath const&, mode_t, int options = 0);
ErrorOr<void> chmod(Credentials const&, Custody&, mode_t);
ErrorOr<void> chown(VFSRootContext const&, Credentials const&, UnresolvedPath const& target_path, UserID, GroupID, int options);
ErrorOr<void> chown(Credentials const&, Custody&, UserID, GroupID);
ErrorOr<void> access(VFSRootContext const&, Credentials const&, UnresolvedPath const& target, int mode, AccessFlags);
ErrorOr<InodeMetadata> lookup_metadata(VFSRootContext const&, Credentials const&, UnresolvedPath const& path, int options = 0);
ErrorOr<void> utime(VFSRootContext const&, Credentials const&, UnresolvedPath const& path, time_t atime, time_t mtime);
ErrorOr<void> utimensat(VFSRootContext const&, Credentials const&, UnresolvedPath const& target_path, timespec const& atime, timespec const& mtime, int options = 0);
ErrorOr<void> do_utimens(Credentials const&, Custody& custody, timespec const& atime, timespec const& mtime);
ErrorOr<void> rename(VFSRootContext const&, Credentials const&, UnresolvedPath const& old_path, UnresolvedPath const& new_path);
ErrorOr<void> mknod(VFSRootContext const&, Credentials const&, UnresolvedPath const& target, mode_t, dev_t);
ErrorOr<NonnullOwnPtr<Path>> open_directory(VFSRootContext const&, Credentials const&, UnresolvedPath const& path);

ErrorOr<NonnullOwnPtr<Path>> resolve_inode_as_link(VFSRootContext const&, Credentials const&, Custody const& parent, Inode const&, RefPtr<Mount>* out_mount, RefPtr<Custody>* out_parent, int options, int symlink_recursion_level);

ErrorOr<NonnullRefPtr<OptimizedPath>> optimize_path(UnresolvedPath const& path);

ErrorOr<NonnullOwnPtr<Path>> resolve_path(VFSRootContext const&, Credentials const&, UnresolvedPath const& path, RefPtr<Mount>* out_mount = nullptr, RefPtr<Custody>* out_parent = nullptr, int options = 0, int symlink_recursion_level = 0);
ErrorOr<NonnullOwnPtr<Path>> resolve_path(Process const&, VFSRootContext const&, Credentials const&, UnresolvedPath const& path, RefPtr<Mount>* out_mount = nullptr, RefPtr<Custody>* out_parent = nullptr, int options = 0, int symlink_recursion_level = 0);

ErrorOr<NonnullOwnPtr<Path>> resolve_path(VFSRootContext const&, Credentials const&, OptimizedPath const& path, RefPtr<Mount>* out_mount = nullptr, RefPtr<Custody>* out_parent = nullptr, int options = 0, int symlink_recursion_level = 0);
ErrorOr<NonnullOwnPtr<Path>> resolve_path(Process const&, VFSRootContext const&, Credentials const&, OptimizedPath const& path, RefPtr<Mount>* out_mount = nullptr, RefPtr<Custody>* out_parent = nullptr, int options = 0, int symlink_recursion_level = 0);
ErrorOr<NonnullOwnPtr<Path>> resolve_path_without_veil(VFSRootContext const&, Credentials const&, OptimizedPath const& path, RefPtr<Mount>* out_mount = nullptr, RefPtr<Custody>* out_parent = nullptr, int options = 0, int symlink_recursion_level = 0);

void sync_filesystems();

};

}
