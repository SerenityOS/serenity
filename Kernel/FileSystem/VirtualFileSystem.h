/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Badge.h>
#include <AK/Function.h>
#include <AK/HashMap.h>
#include <AK/NonnullOwnPtrVector.h>
#include <AK/OwnPtr.h>
#include <AK/RefPtr.h>
#include <AK/String.h>
#include <Kernel/FileSystem/FileSystem.h>
#include <Kernel/FileSystem/InodeIdentifier.h>
#include <Kernel/FileSystem/InodeMetadata.h>
#include <Kernel/FileSystem/Mount.h>
#include <Kernel/KResult.h>
#include <Kernel/UnveilNode.h>

namespace Kernel {

class Custody;
class Device;
class FileDescription;

struct UidAndGid {
    uid_t uid;
    gid_t gid;
};

class VirtualFileSystem {
    AK_MAKE_ETERNAL
public:
    static void initialize();
    static VirtualFileSystem& the();

    VirtualFileSystem();
    ~VirtualFileSystem();

    bool mount_root(FileSystem&);
    KResult mount(FileSystem&, Custody& mount_point, int flags);
    KResult bind_mount(Custody& source, Custody& mount_point, int flags);
    KResult remount(Custody& mount_point, int new_flags);
    KResult unmount(Inode& guest_inode);

    KResultOr<NonnullRefPtr<FileDescription>> open(StringView path, int options, mode_t mode, Custody& base, Optional<UidAndGid> = {});
    KResultOr<NonnullRefPtr<FileDescription>> create(StringView path, int options, mode_t mode, Custody& parent_custody, Optional<UidAndGid> = {});
    KResult mkdir(StringView path, mode_t mode, Custody& base);
    KResult link(StringView old_path, StringView new_path, Custody& base);
    KResult unlink(StringView path, Custody& base);
    KResult symlink(StringView target, StringView linkpath, Custody& base);
    KResult rmdir(StringView path, Custody& base);
    KResult chmod(StringView path, mode_t, Custody& base);
    KResult chmod(Custody&, mode_t);
    KResult chown(StringView path, uid_t, gid_t, Custody& base);
    KResult chown(Custody&, uid_t, gid_t);
    KResult access(StringView path, int mode, Custody& base);
    KResultOr<InodeMetadata> lookup_metadata(StringView path, Custody& base, int options = 0);
    KResult utime(StringView path, Custody& base, time_t atime, time_t mtime);
    KResult rename(StringView oldpath, StringView newpath, Custody& base);
    KResult mknod(StringView path, mode_t, dev_t, Custody& base);
    KResultOr<NonnullRefPtr<Custody>> open_directory(StringView path, Custody& base);

    size_t mount_count() const { return m_mounts.size(); }
    void for_each_mount(Function<void(const Mount&)>) const;

    InodeIdentifier root_inode_id() const;

    static void sync();

    Custody& root_custody();
    KResultOr<NonnullRefPtr<Custody>> resolve_path(StringView path, Custody& base, RefPtr<Custody>* out_parent = nullptr, int options = 0, int symlink_recursion_level = 0);
    KResultOr<NonnullRefPtr<Custody>> resolve_path_without_veil(StringView path, Custody& base, RefPtr<Custody>* out_parent = nullptr, int options = 0, int symlink_recursion_level = 0);

private:
    friend class FileDescription;

    UnveilNode const& find_matching_unveiled_path(StringView path);
    KResult validate_path_against_process_veil(Custody const& path, int options);
    KResult validate_path_against_process_veil(StringView path, int options);

    bool is_vfs_root(InodeIdentifier) const;

    KResult traverse_directory_inode(Inode&, Function<bool(FileSystem::DirectoryEntryView const&)>);

    Mount* find_mount_for_host(InodeIdentifier);
    Mount* find_mount_for_guest(InodeIdentifier);

    Lock m_lock { "VFSLock" };

    RefPtr<Inode> m_root_inode;
    Vector<Mount, 16> m_mounts;
    RefPtr<Custody> m_root_custody;
};

}
