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

class VirtualFileSystem {
    friend class StorageManagement;

public:
    // Required to be at least 8 by POSIX
    // https://pubs.opengroup.org/onlinepubs/9699919799/basedefs/limits.h.html
    static constexpr int symlink_recursion_limit = 8;

    static void initialize();
    static VirtualFileSystem& the();

    static bool check_matching_absolute_path_hierarchy(Custody const& first_custody, Custody const& second_custody);

    static ErrorOr<FileSystemInitializer const*> find_filesystem_type_initializer(StringView fs_type);

    VirtualFileSystem();
    ~VirtualFileSystem();

    SpinlockProtected<IntrusiveList<&FileSystem::m_file_system_node>, LockRank::FileSystem>& all_file_systems_list(Badge<VFSRootContext>) { return m_file_systems_list; }
    SpinlockProtected<IntrusiveList<&VFSRootContext::m_list_node>, LockRank::FileSystem>& all_root_contexts_list(Badge<PowerStateSwitchTask>) { return m_root_contexts; }
    SpinlockProtected<IntrusiveList<&VFSRootContext::m_list_node>, LockRank::FileSystem>& all_root_contexts_list(Badge<VFSRootContext>) { return m_root_contexts; }
    SpinlockProtected<IntrusiveList<&VFSRootContext::m_list_node>, LockRank::FileSystem>& all_root_contexts_list(Badge<Process>) { return m_root_contexts; }

    ErrorOr<void> mount(VFSRootContext&, MountFile&, OpenFileDescription*, Custody& mount_point, int flags);
    ErrorOr<void> pivot_root_by_copying_mounted_fs_instance(VFSRootContext&, FileSystem& fs, int root_mount_flags);

    ErrorOr<void> bind_mount(VFSRootContext&, Custody& source, Custody& mount_point, int flags);
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

    ErrorOr<void> for_each_mount(VFSRootContext& context, Function<ErrorOr<void>(Mount const&)>) const;

    void sync_filesystems();
    void lock_all_filesystems();

    static void sync();

    ErrorOr<NonnullRefPtr<Custody>> resolve_path(VFSRootContext const&, Credentials const&, StringView path, CustodyBase const& base, RefPtr<Custody>* out_parent = nullptr, int options = 0, int symlink_recursion_level = 0);
    ErrorOr<NonnullRefPtr<Custody>> resolve_path(Process const&, VFSRootContext const&, Credentials const&, StringView path, CustodyBase const& base, RefPtr<Custody>* out_parent = nullptr, int options = 0, int symlink_recursion_level = 0);
    ErrorOr<NonnullRefPtr<Custody>> resolve_path_without_veil(VFSRootContext const&, Credentials const&, StringView path, NonnullRefPtr<Custody> base, RefPtr<Custody>* out_parent = nullptr, int options = 0, int symlink_recursion_level = 0);

private:
    friend class OpenFileDescription;

    UnveilNode const& find_matching_unveiled_path(Process const&, StringView path);
    ErrorOr<void> validate_path_against_process_veil(Process const&, StringView path, int options);
    ErrorOr<void> validate_path_against_process_veil(Process const& process, Custody const& custody, int options);
    ErrorOr<void> validate_path_against_process_veil(Custody const& path, int options);
    ErrorOr<void> validate_path_against_process_veil(StringView path, int options);

    static void delete_mount_from_list(Mount& mount);
    ErrorOr<void> remove_mount(Mount& mount, IntrusiveList<&FileBackedFileSystem::m_file_backed_file_system_node>& file_backed_fs_list);

    static ErrorOr<NonnullRefPtr<FileSystem>> create_and_initialize_filesystem_from_mount_file(MountFile& mount_file);
    static ErrorOr<NonnullRefPtr<FileSystem>> create_and_initialize_filesystem_from_mount_file_and_description(IntrusiveList<&FileBackedFileSystem::m_file_backed_file_system_node>& file_backed_fs_list, MountFile& mount_file, OpenFileDescription& source_description);
    static ErrorOr<void> verify_mount_file_and_description_requirements(MountFile& mount_file, OpenFileDescription& source_description);

    ErrorOr<void> traverse_directory_inode(Inode&, Function<ErrorOr<void>(FileSystem::DirectoryEntryView const&)>);

    ErrorOr<void> apply_to_mount_for_host_custody(VFSRootContext&, Custody const& current_custody, Function<void(Mount&)>);

    // NOTE: The FileBackedFileSystem list is protected by a mutex because we need to scan it
    // to search for existing filesystems for already used block devices and therefore when doing
    // that we could fail to find a filesystem so we need to create a new filesystem which might
    // need to do disk access (i.e. taking Mutexes in other places) and then register that new filesystem
    // in this list, to avoid TOCTOU bugs.
    MutexProtected<IntrusiveList<&FileBackedFileSystem::m_file_backed_file_system_node>> m_file_backed_file_systems_list {};

    SpinlockProtected<IntrusiveList<&FileSystem::m_file_system_node>, LockRank::FileSystem> m_file_systems_list {};

    SpinlockProtected<IntrusiveList<&VFSRootContext::m_list_node>, LockRank::FileSystem> m_root_contexts {};
};

}
