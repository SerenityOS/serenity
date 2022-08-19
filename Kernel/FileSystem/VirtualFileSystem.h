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
#include <Kernel/FileSystem/FileBackedFileSystem.h>
#include <Kernel/FileSystem/FileSystem.h>
#include <Kernel/FileSystem/InodeIdentifier.h>
#include <Kernel/FileSystem/InodeMetadata.h>
#include <Kernel/FileSystem/Mount.h>
#include <Kernel/FileSystem/UnveilNode.h>
#include <Kernel/Forward.h>
#include <Kernel/Library/LockRefPtr.h>
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
    ErrorOr<void> unmount(Custody& mount_point);

    ErrorOr<NonnullLockRefPtr<OpenFileDescription>> open(Credentials const&, StringView path, int options, mode_t mode, Custody& base, Optional<UidAndGid> = {});
    ErrorOr<NonnullLockRefPtr<OpenFileDescription>> create(Credentials const&, StringView path, int options, mode_t mode, Custody& parent_custody, Optional<UidAndGid> = {});
    ErrorOr<void> mkdir(Credentials const&, StringView path, mode_t mode, Custody& base);
    ErrorOr<void> link(Credentials const&, StringView old_path, StringView new_path, Custody& base);
    ErrorOr<void> unlink(Credentials const&, StringView path, Custody& base);
    ErrorOr<void> symlink(Credentials const&, StringView target, StringView linkpath, Custody& base);
    ErrorOr<void> rmdir(Credentials const&, StringView path, Custody& base);
    ErrorOr<void> chmod(Credentials const&, StringView path, mode_t, Custody& base, int options = 0);
    ErrorOr<void> chmod(Credentials const&, Custody&, mode_t);
    ErrorOr<void> chown(Credentials const&, StringView path, UserID, GroupID, Custody& base, int options);
    ErrorOr<void> chown(Credentials const&, Custody&, UserID, GroupID);
    ErrorOr<void> access(Credentials const&, StringView path, int mode, Custody& base);
    ErrorOr<InodeMetadata> lookup_metadata(Credentials const&, StringView path, Custody& base, int options = 0);
    ErrorOr<void> utime(Credentials const&, StringView path, Custody& base, time_t atime, time_t mtime);
    ErrorOr<void> utimensat(Credentials const&, StringView path, Custody& base, timespec const& atime, timespec const& mtime, int options = 0);
    ErrorOr<void> rename(Credentials const&, StringView oldpath, StringView newpath, Custody& base);
    ErrorOr<void> mknod(Credentials const&, StringView path, mode_t, dev_t, Custody& base);
    ErrorOr<NonnullRefPtr<Custody>> open_directory(Credentials const&, StringView path, Custody& base);

    ErrorOr<void> for_each_mount(Function<ErrorOr<void>(Mount const&)>) const;

    ErrorOr<NonnullLockRefPtr<FileBackedFileSystem>> find_already_existing_or_create_file_backed_file_system(OpenFileDescription& description, Function<ErrorOr<NonnullLockRefPtr<FileSystem>>(OpenFileDescription&)> callback);

    InodeIdentifier root_inode_id() const;

    static void sync();

    NonnullRefPtr<Custody> root_custody();
    ErrorOr<NonnullRefPtr<Custody>> resolve_path(Credentials const&, StringView path, NonnullRefPtr<Custody> base, RefPtr<Custody>* out_parent = nullptr, int options = 0, int symlink_recursion_level = 0);
    ErrorOr<NonnullRefPtr<Custody>> resolve_path_without_veil(Credentials const&, StringView path, NonnullRefPtr<Custody> base, RefPtr<Custody>* out_parent = nullptr, int options = 0, int symlink_recursion_level = 0);

private:
    friend class OpenFileDescription;

    UnveilNode const& find_matching_unveiled_path(StringView path);
    ErrorOr<void> validate_path_against_process_veil(Custody const& path, int options);
    ErrorOr<void> validate_path_against_process_veil(StringView path, int options);

    bool is_vfs_root(InodeIdentifier) const;

    ErrorOr<void> traverse_directory_inode(Inode&, Function<ErrorOr<void>(FileSystem::DirectoryEntryView const&)>);

    bool mount_point_exists_at_inode(InodeIdentifier inode);

    // FIXME: These functions are totally unsafe as someone could unmount the returned Mount underneath us.
    Mount* find_mount_for_host(InodeIdentifier);
    Mount* find_mount_for_guest(InodeIdentifier);

    LockRefPtr<Inode> m_root_inode;

    SpinlockProtected<RefPtr<Custody>> m_root_custody;

    SpinlockProtected<Vector<NonnullOwnPtr<Mount>, 16>> m_mounts { LockRank::None };
    SpinlockProtected<IntrusiveList<&FileBackedFileSystem::m_file_backed_file_system_node>> m_file_backed_file_systems_list { LockRank::None };
};

}
