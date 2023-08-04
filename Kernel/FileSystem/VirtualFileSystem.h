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
#include <AK/OwnPtr.h>
#include <AK/RefPtr.h>
#include <Kernel/FileSystem/FileBackedFileSystem.h>
#include <Kernel/FileSystem/FileSystem.h>
#include <Kernel/FileSystem/Initializer.h>
#include <Kernel/FileSystem/InodeIdentifier.h>
#include <Kernel/FileSystem/InodeMetadata.h>
#include <Kernel/FileSystem/Mount.h>
#include <Kernel/FileSystem/MountFile.h>
#include <Kernel/FileSystem/UnveilNode.h>
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

class VirtualFileSystem {
public:
    // Required to be at least 8 by POSIX
    // https://pubs.opengroup.org/onlinepubs/9699919799/basedefs/limits.h.html
    static constexpr int symlink_recursion_limit = 8;

    static void initialize();
    static VirtualFileSystem& the();

    static ErrorOr<FileSystemInitializer const*> find_filesystem_type_initializer(StringView fs_type);

    VirtualFileSystem();
    ~VirtualFileSystem();

    ErrorOr<void> mount_root(FileSystem&);
    ErrorOr<void> mount(MountFile&, OpenFileDescription*, Custody& mount_point, int flags);
    ErrorOr<void> bind_mount(Custody& source, Custody& mount_point, int flags);
    ErrorOr<void> remount(Custody& mount_point, int new_flags);
    ErrorOr<void> unmount(Custody& mount_point);
    ErrorOr<void> unmount(Inode& guest_inode, StringView custody_path);

    ErrorOr<NonnullRefPtr<OpenFileDescription>> open(Credentials const&, StringView path, int options, mode_t mode, Custody& base, Optional<UidAndGid> = {});
    ErrorOr<NonnullRefPtr<OpenFileDescription>> open(Process const&, Credentials const&, StringView path, int options, mode_t mode, Custody& base, Optional<UidAndGid> = {});
    ErrorOr<NonnullRefPtr<OpenFileDescription>> create(Credentials const&, StringView path, int options, mode_t mode, Custody& parent_custody, Optional<UidAndGid> = {});
    ErrorOr<NonnullRefPtr<OpenFileDescription>> create(Process const&, Credentials const&, StringView path, int options, mode_t mode, Custody& parent_custody, Optional<UidAndGid> = {});
    ErrorOr<void> mkdir(Credentials const&, StringView path, mode_t mode, Custody& base);
    ErrorOr<void> link(Credentials const&, StringView old_path, StringView new_path, Custody& base);
    ErrorOr<void> unlink(Credentials const&, StringView path, Custody& base);
    ErrorOr<void> symlink(Credentials const&, StringView target, StringView linkpath, Custody& base);
    ErrorOr<void> rmdir(Credentials const&, StringView path, Custody& base);
    ErrorOr<void> chmod(Credentials const&, StringView path, mode_t, Custody& base, int options = 0);
    ErrorOr<void> chmod(Credentials const&, Custody&, mode_t);
    ErrorOr<void> chown(Credentials const&, StringView path, UserID, GroupID, Custody& base, int options);
    ErrorOr<void> chown(Credentials const&, Custody&, UserID, GroupID);
    ErrorOr<void> access(Credentials const&, StringView path, int mode, Custody& base, AccessFlags);
    ErrorOr<InodeMetadata> lookup_metadata(Credentials const&, StringView path, Custody& base, int options = 0);
    ErrorOr<void> utime(Credentials const&, StringView path, Custody& base, time_t atime, time_t mtime);
    ErrorOr<void> utimensat(Credentials const&, StringView path, Custody& base, timespec const& atime, timespec const& mtime, int options = 0);
    ErrorOr<void> do_utimens(Credentials const& credentials, Custody& custody, timespec const& atime, timespec const& mtime);
    ErrorOr<void> rename(Credentials const&, Custody& old_base, StringView oldpath, Custody& new_base, StringView newpath);
    ErrorOr<void> mknod(Credentials const&, StringView path, mode_t, dev_t, Custody& base);
    ErrorOr<NonnullRefPtr<Custody>> open_directory(Credentials const&, StringView path, Custody& base);

    ErrorOr<void> for_each_mount(Function<ErrorOr<void>(Mount const&)>) const;

    void sync_filesystems();
    void lock_all_filesystems();

    static void sync();

    NonnullRefPtr<Custody> root_custody();
    ErrorOr<NonnullRefPtr<Custody>> resolve_path(Credentials const&, StringView path, NonnullRefPtr<Custody> base, RefPtr<Custody>* out_parent = nullptr, int options = 0, int symlink_recursion_level = 0);
    ErrorOr<NonnullRefPtr<Custody>> resolve_path(Process const&, Credentials const&, StringView path, NonnullRefPtr<Custody> base, RefPtr<Custody>* out_parent = nullptr, int options = 0, int symlink_recursion_level = 0);
    ErrorOr<NonnullRefPtr<Custody>> resolve_path_without_veil(Credentials const&, StringView path, NonnullRefPtr<Custody> base, RefPtr<Custody>* out_parent = nullptr, int options = 0, int symlink_recursion_level = 0);

private:
    friend class OpenFileDescription;

    UnveilNode const& find_matching_unveiled_path(Process const&, StringView path);
    ErrorOr<void> validate_path_against_process_veil(Process const&, StringView path, int options);
    ErrorOr<void> validate_path_against_process_veil(Process const& process, Custody const& custody, int options);
    ErrorOr<void> validate_path_against_process_veil(Custody const& path, int options);
    ErrorOr<void> validate_path_against_process_veil(StringView path, int options);

    ErrorOr<void> add_file_system_to_mount_table(FileSystem& file_system, Custody& mount_point, int flags);

    ErrorOr<void> traverse_directory_inode(Inode&, Function<ErrorOr<void>(FileSystem::DirectoryEntryView const&)>);

    static bool check_matching_absolute_path_hierarchy(Custody const& first_custody, Custody const& second_custody);
    bool mount_point_exists_at_custody(Custody& mount_point);

    // FIXME: This function is totally unsafe as someone could unmount the returned Mount underneath us.
    Mount* find_mount_for_host_custody(Custody const& current_custody);

    RefPtr<Inode> m_root_inode;

    SpinlockProtected<RefPtr<Custody>, LockRank::None> m_root_custody {};

    SpinlockProtected<IntrusiveList<&Mount::m_vfs_list_node>, LockRank::None> m_mounts {};

    // NOTE: The FileBackedFileSystem list is protected by a mutex because we need to scan it
    // to search for existing filesystems for already used block devices and therefore when doing
    // that we could fail to find a filesystem so we need to create a new filesystem which might
    // need to do disk access (i.e. taking Mutexes in other places) and then register that new filesystem
    // in this list, to avoid TOCTOU bugs.
    MutexProtected<IntrusiveList<&FileBackedFileSystem::m_file_backed_file_system_node>> m_file_backed_file_systems_list {};

    SpinlockProtected<IntrusiveList<&FileSystem::m_file_system_node>, LockRank::FileSystem> m_file_systems_list {};
};

}
