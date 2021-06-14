/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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
#include <Kernel/FileSystem/Custody.h>
#include <Kernel/FileSystem/FileSystem.h>
#include <Kernel/FileSystem/InodeIdentifier.h>
#include <Kernel/FileSystem/InodeMetadata.h>
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

struct PathWithBase {
    Custody& base;
    StringView path;
};

class VFS {
    AK_MAKE_ETERNAL
public:
    class Mount {
    public:
        Mount(FS&, Custody* host_custody, int flags);
        Mount(Inode& source, Custody& host_custody, int flags);

        const Inode* host() const;
        Inode* host();

        const Inode& guest() const { return *m_guest; }
        Inode& guest() { return *m_guest; }

        const FS& guest_fs() const { return *m_guest_fs; }

        String absolute_path() const;

        int flags() const { return m_flags; }
        void set_flags(int flags) { m_flags = flags; }

    private:
        NonnullRefPtr<Inode> m_guest;
        NonnullRefPtr<FS> m_guest_fs;
        RefPtr<Custody> m_host_custody;
        int m_flags;
    };

    static void initialize();
    static VFS& the();

    VFS();
    ~VFS();

    using AtFlags = int;
    using OpenFlags = int;

    bool mount_root(FS&);
    KResult mount(FS&, Custody& mount_point, int flags);
    KResult bind_mount(Custody& source, Custody& mount_point, int flags);
    KResult remount(Custody& mount_point, int new_flags);
    KResult unmount(Inode& guest_inode);

    KResultOr<NonnullRefPtr<FileDescription>> open(StringView path, int options, mode_t mode, Custody& base, Optional<UidAndGid> = {});
    KResultOr<NonnullRefPtr<FileDescription>> create(StringView path, int options, mode_t mode, Custody& parent_custody, Optional<UidAndGid> = {});
    KResult mkdir(PathWithBase path, mode_t mode);
    KResult symlink(StringView target, PathWithBase linkpath);
    KResult link(PathWithBase old_path, PathWithBase new_path, AtFlags flags);
    KResult unlink(PathWithBase, AtFlags flags);
    KResult rmdir(StringView path, Custody& base);
    KResult chmod(PathWithBase, mode_t mode, AtFlags flags);
    KResult chmod(Custody&, mode_t);
    KResult chown(PathWithBase, uid_t, gid_t, int flags);
    KResult chown(Custody&, uid_t, gid_t);
    KResult access(PathWithBase, int mode, AtFlags flags);
    KResultOr<InodeMetadata> lookup_metadata(StringView path, Custody& base, int options = 0);
    KResult utime(StringView path, Custody& base, time_t atime, time_t mtime);
    KResult rename(PathWithBase oldpath, PathWithBase newpath);
    KResult mknod(PathWithBase, mode_t, dev_t);
    KResultOr<NonnullRefPtr<Custody>> open_directory(StringView path, Custody& base);

    size_t mount_count() const { return m_mounts.size(); }
    void for_each_mount(Function<void(const Mount&)>) const;

    InodeIdentifier root_inode_id() const;

    void sync();

    Custody& root_custody();
    KResultOr<NonnullRefPtr<Custody>> resolve_path(StringView path, Custody& base, RefPtr<Custody>* out_parent = nullptr, int options = 0, int symlink_recursion_level = 0);
    KResultOr<NonnullRefPtr<Custody>> resolve_path(PathWithBase path_with_base, RefPtr<Custody>* out_parent = nullptr, int options = 0, int symlink_recursion_level = 0)
    {
        return resolve_path(path_with_base.path, path_with_base.base, out_parent, options, symlink_recursion_level);
    }
    KResultOr<NonnullRefPtr<Custody>> resolve_path_without_veil(StringView path, Custody& base, RefPtr<Custody>* out_parent = nullptr, int options = 0, int symlink_recursion_level = 0);
    KResultOr<NonnullRefPtr<Custody>> resolve_path_without_veil(PathWithBase path_with_base, RefPtr<Custody>* out_parent = nullptr, int options = 0, int symlink_recursion_level = 0)
    {
        return resolve_path_without_veil(path_with_base.path, path_with_base.base, out_parent, options, symlink_recursion_level);
    }

private:
    friend class FileDescription;

    UnveilNode const& find_matching_unveiled_path(StringView path);
    KResult validate_path_against_process_veil(StringView path, int options);

    bool is_vfs_root(InodeIdentifier) const;

    KResult traverse_directory_inode(Inode&, Function<bool(const FS::DirectoryEntryView&)>);

    Mount* find_mount_for_host(Inode&);
    Mount* find_mount_for_host(InodeIdentifier);
    Mount* find_mount_for_guest(Inode&);
    Mount* find_mount_for_guest(InodeIdentifier);

    Lock m_lock { "VFSLock" };

    RefPtr<Inode> m_root_inode;
    Vector<Mount, 16> m_mounts;
    RefPtr<Custody> m_root_custody;
};

}
