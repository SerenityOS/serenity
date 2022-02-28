/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Badge.h>
#include <AK/Error.h>
#include <AK/Function.h>
#include <AK/HashMap.h>
#include <AK/NonnullOwnPtrVector.h>
#include <AK/OwnPtr.h>
#include <AK/RefPtr.h>
#include <Kernel/FileSystem/FileSystem.h>
#include <Kernel/FileSystem/InodeIdentifier.h>
#include <Kernel/FileSystem/InodeMetadata.h>
#include <Kernel/FileSystem/Mount.h>
#include <Kernel/FileSystem/UnveilNode.h>
#include <Kernel/Forward.h>
#include <Kernel/Locking/SpinlockProtected.h>

namespace Kernel {

// Kernel internal options.
#define O_NOFOLLOW_NOERROR (1 << 29)
#define O_UNLINK_INTERNAL (1 << 30)

struct UidAndGid {
    UserID uid;
    GroupID gid;
};

class VirtualFileSystem {
public:
    // Required to be at least 8 by POSIX
    // https://pubs.opengroup.org/onlinepubs/9699919799/basedefs/limits.h.html
    static constexpr int symlink_recursion_limit = 8;

    static void initialize();
    static VirtualFileSystem& the();

    VirtualFileSystem();
    ~VirtualFileSystem();

    ErrorOr<void> mount_root(FileSystem&);
    ErrorOr<void> mount(FileSystem&, Custody& mount_point, int flags);
    ErrorOr<void> bind_mount(Custody& source, Custody& mount_point, int flags);
    ErrorOr<void> remount(Custody& mount_point, int new_flags);
    ErrorOr<void> unmount(Inode& guest_inode);

    ErrorOr<NonnullRefPtr<OpenFileDescription>> open(StringView path, int options, mode_t mode, Custody& base, Optional<UidAndGid> = {});
    ErrorOr<NonnullRefPtr<OpenFileDescription>> create(StringView path, int options, mode_t mode, Custody& parent_custody, Optional<UidAndGid> = {});
    ErrorOr<void> mkdir(StringView path, mode_t mode, Custody& base);
    ErrorOr<void> link(StringView old_path, StringView new_path, Custody& base);
    ErrorOr<void> unlink(StringView path, Custody& base);
    ErrorOr<void> symlink(StringView target, StringView linkpath, Custody& base);
    ErrorOr<void> rmdir(StringView path, Custody& base);
    ErrorOr<void> chmod(StringView path, mode_t, Custody& base, int options = 0);
    ErrorOr<void> chmod(Custody&, mode_t);
    ErrorOr<void> chown(StringView path, UserID, GroupID, Custody& base, int options);
    ErrorOr<void> chown(Custody&, UserID, GroupID);
    ErrorOr<void> access(StringView path, int mode, Custody& base);
    ErrorOr<InodeMetadata> lookup_metadata(StringView path, Custody& base, int options = 0);
    ErrorOr<void> utime(StringView path, Custody& base, time_t atime, time_t mtime);
    ErrorOr<void> rename(StringView oldpath, StringView newpath, Custody& base);
    ErrorOr<void> mknod(StringView path, mode_t, dev_t, Custody& base);
    ErrorOr<NonnullRefPtr<Custody>> open_directory(StringView path, Custody& base);

    ErrorOr<void> for_each_mount(Function<ErrorOr<void>(const Mount&)>) const;

    InodeIdentifier root_inode_id() const;

    static void sync();

    Custody& root_custody();
    ErrorOr<NonnullRefPtr<Custody>> resolve_path(StringView path, Custody& base, RefPtr<Custody>* out_parent = nullptr, int options = 0, int symlink_recursion_level = 0);
    ErrorOr<NonnullRefPtr<Custody>> resolve_path_without_veil(StringView path, Custody& base, RefPtr<Custody>* out_parent = nullptr, int options = 0, int symlink_recursion_level = 0);

private:
    friend class OpenFileDescription;

    UnveilNode const& find_matching_unveiled_path(StringView path);
    ErrorOr<void> validate_path_against_process_veil(Custody const& path, int options);
    ErrorOr<void> validate_path_against_process_veil(StringView path, int options);

    bool is_vfs_root(InodeIdentifier) const;

    ErrorOr<void> traverse_directory_inode(Inode&, Function<ErrorOr<void>(FileSystem::DirectoryEntryView const&)>);

    Mount* find_mount_for_host(InodeIdentifier);
    Mount* find_mount_for_guest(InodeIdentifier);

    RefPtr<Inode> m_root_inode;
    RefPtr<Custody> m_root_custody;

    SpinlockProtected<Vector<Mount, 16>> m_mounts;
};

}
