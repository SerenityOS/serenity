/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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
#include <Kernel/KResult.h>

namespace Kernel {

class Custody;
class Device;
class FileDescription;
struct UnveiledPath;

struct UidAndGid {
    uid_t uid;
    gid_t gid;
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

    bool mount_root(FS&);
    KResult mount(FS&, Custody& mount_point, int flags);
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

    void sync();

    Custody& root_custody();
    KResultOr<NonnullRefPtr<Custody>> resolve_path(StringView path, Custody& base, RefPtr<Custody>* out_parent = nullptr, int options = 0, int symlink_recursion_level = 0);
    KResultOr<NonnullRefPtr<Custody>> resolve_path_without_veil(StringView path, Custody& base, RefPtr<Custody>* out_parent = nullptr, int options = 0, int symlink_recursion_level = 0);

private:
    friend class FileDescription;

    const UnveiledPath* find_matching_unveiled_path(StringView path);
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
