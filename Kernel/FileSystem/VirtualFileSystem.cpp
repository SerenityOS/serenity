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

#include <AK/LexicalPath.h>
#include <AK/Singleton.h>
#include <AK/StringBuilder.h>
#include <Kernel/Devices/BlockDevice.h>
#include <Kernel/FileSystem/Custody.h>
#include <Kernel/FileSystem/FileBackedFileSystem.h>
#include <Kernel/FileSystem/FileDescription.h>
#include <Kernel/FileSystem/FileSystem.h>
#include <Kernel/FileSystem/VirtualFileSystem.h>
#include <Kernel/KSyms.h>
#include <Kernel/Process.h>
#include <LibC/errno_numbers.h>

//#define VFS_DEBUG

namespace Kernel {

static AK::Singleton<VFS> s_the;
static constexpr int symlink_recursion_limit { 5 }; // FIXME: increase?
static constexpr int root_mount_flags = MS_NODEV | MS_NOSUID | MS_RDONLY;

void VFS::initialize()
{
    s_the.ensure_instance();
}

VFS& VFS::the()
{
    return *s_the;
}

VFS::VFS()
{
#ifdef VFS_DEBUG
    klog() << "VFS: Constructing VFS";
#endif
}

VFS::~VFS()
{
}

InodeIdentifier VFS::root_inode_id() const
{
    ASSERT(m_root_inode);
    return m_root_inode->identifier();
}

KResult VFS::mount(FS& file_system, Custody& mount_point, int flags)
{
    LOCKER(m_lock);

    auto& inode = mount_point.inode();
    dbg() << "VFS: Mounting " << file_system.class_name() << " at " << mount_point.absolute_path() << " (inode: " << inode.identifier() << ") with flags " << flags;
    // FIXME: check that this is not already a mount point
    Mount mount { file_system, &mount_point, flags };
    m_mounts.append(move(mount));
    return KSuccess;
}

KResult VFS::bind_mount(Custody& source, Custody& mount_point, int flags)
{
    LOCKER(m_lock);

    dbg() << "VFS: Bind-mounting " << source.absolute_path() << " at " << mount_point.absolute_path();
    // FIXME: check that this is not already a mount point
    Mount mount { source.inode(), mount_point, flags };
    m_mounts.append(move(mount));
    return KSuccess;
}

KResult VFS::remount(Custody& mount_point, int new_flags)
{
    LOCKER(m_lock);

    dbg() << "VFS: Remounting " << mount_point.absolute_path();

    Mount* mount = find_mount_for_guest(mount_point.inode());
    if (!mount)
        return KResult(-ENODEV);

    mount->set_flags(new_flags);
    return KSuccess;
}

KResult VFS::unmount(Inode& guest_inode)
{
    LOCKER(m_lock);
    dbg() << "VFS: unmount called with inode " << guest_inode.identifier();

    for (size_t i = 0; i < m_mounts.size(); ++i) {
        auto& mount = m_mounts.at(i);
        if (&mount.guest() == &guest_inode) {
            auto result = mount.guest_fs().prepare_to_unmount();
            if (result.is_error()) {
                dbg() << "VFS: Failed to unmount!";
                return result;
            }
            dbg() << "VFS: found fs " << mount.guest_fs().fsid() << " at mount index " << i << "! Unmounting...";
            m_mounts.unstable_take(i);
            return KSuccess;
        }
    }

    dbg() << "VFS: Nothing mounted on inode " << guest_inode.identifier();
    return KResult(-ENODEV);
}

bool VFS::mount_root(FS& file_system)
{
    if (m_root_inode) {
        klog() << "VFS: mount_root can't mount another root";
        return false;
    }

    Mount mount { file_system, nullptr, root_mount_flags };

    auto root_inode = file_system.root_inode();
    if (!root_inode->is_directory()) {
        klog() << "VFS: root inode (" << String::format("%02u", file_system.fsid()) << ":" << String::format("%08u", root_inode->index()) << ") for / is not a directory :(";
        return false;
    }

    m_root_inode = move(root_inode);
    klog() << "VFS: mounted root from " << file_system.class_name() << " (" << static_cast<FileBackedFS&>(file_system).file_description().absolute_path() << ")";

    m_mounts.append(move(mount));
    return true;
}

auto VFS::find_mount_for_host(Inode& inode) -> Mount*
{
    for (auto& mount : m_mounts) {
        if (mount.host() == &inode)
            return &mount;
    }
    return nullptr;
}

auto VFS::find_mount_for_host(InodeIdentifier id) -> Mount*
{
    for (auto& mount : m_mounts) {
        if (mount.host() && mount.host()->identifier() == id)
            return &mount;
    }
    return nullptr;
}

auto VFS::find_mount_for_guest(Inode& inode) -> Mount*
{
    for (auto& mount : m_mounts) {
        if (&mount.guest() == &inode)
            return &mount;
    }
    return nullptr;
}

auto VFS::find_mount_for_guest(InodeIdentifier id) -> Mount*
{
    for (auto& mount : m_mounts) {
        if (mount.guest().identifier() == id)
            return &mount;
    }
    return nullptr;
}

bool VFS::is_vfs_root(InodeIdentifier inode) const
{
    return inode == root_inode_id();
}

KResult VFS::traverse_directory_inode(Inode& dir_inode, Function<bool(const FS::DirectoryEntryView&)> callback)
{
    return dir_inode.traverse_as_directory([&](auto& entry) {
        InodeIdentifier resolved_inode;
        if (auto mount = find_mount_for_host(entry.inode))
            resolved_inode = mount->guest().identifier();
        else
            resolved_inode = entry.inode;

        // FIXME: This is now broken considering chroot and bind mounts.
        bool is_root_inode = dir_inode.identifier() == dir_inode.fs().root_inode()->identifier();
        if (is_root_inode && !is_vfs_root(dir_inode.identifier()) && entry.name == "..") {
            auto mount = find_mount_for_guest(dir_inode);
            ASSERT(mount);
            ASSERT(mount->host());
            resolved_inode = mount->host()->identifier();
        }
        callback({ entry.name, resolved_inode, entry.file_type });
        return true;
    });
}

KResult VFS::utime(StringView path, Custody& base, time_t atime, time_t mtime)
{
    auto custody_or_error = VFS::the().resolve_path(move(path), base);
    if (custody_or_error.is_error())
        return custody_or_error.error();
    auto& custody = *custody_or_error.value();
    auto& inode = custody.inode();
    auto current_process = Process::current();
    if (!current_process->is_superuser() && inode.metadata().uid != current_process->euid())
        return KResult(-EACCES);
    if (custody.is_readonly())
        return KResult(-EROFS);

    int error = inode.set_atime(atime);
    if (error)
        return KResult(error);
    error = inode.set_mtime(mtime);
    if (error)
        return KResult(error);
    return KSuccess;
}

KResultOr<InodeMetadata> VFS::lookup_metadata(StringView path, Custody& base, int options)
{
    auto custody_or_error = resolve_path(path, base, nullptr, options);
    if (custody_or_error.is_error())
        return custody_or_error.error();
    return custody_or_error.value()->inode().metadata();
}

KResultOr<NonnullRefPtr<FileDescription>> VFS::open(StringView path, int options, mode_t mode, Custody& base, Optional<UidAndGid> owner)
{
    if ((options & O_CREAT) && (options & O_DIRECTORY))
        return KResult(-EINVAL);

    RefPtr<Custody> parent_custody;
    auto custody_or_error = resolve_path(path, base, &parent_custody, options);
    if (options & O_CREAT) {
        if (!parent_custody)
            return KResult(-ENOENT);
        if (custody_or_error.is_error()) {
            if (custody_or_error.error() != -ENOENT)
                return custody_or_error.error();
            return create(path, options, mode, *parent_custody, move(owner));
        }
        if (options & O_EXCL)
            return KResult(-EEXIST);
    }
    if (custody_or_error.is_error())
        return custody_or_error.error();

    auto& custody = *custody_or_error.value();
    auto& inode = custody.inode();
    auto metadata = inode.metadata();

    if ((options & O_DIRECTORY) && !metadata.is_directory())
        return KResult(-ENOTDIR);

    bool should_truncate_file = false;

    auto current_process = Process::current();
    if ((options & O_RDONLY) && !metadata.may_read(*current_process))
        return KResult(-EACCES);

    if (options & O_WRONLY) {
        if (!metadata.may_write(*current_process))
            return KResult(-EACCES);
        if (metadata.is_directory())
            return KResult(-EISDIR);
        should_truncate_file = options & O_TRUNC;
    }
    if (options & O_EXEC) {
        if (!metadata.may_execute(*current_process) || (custody.mount_flags() & MS_NOEXEC))
            return KResult(-EACCES);
    }

    if (auto preopen_fd = inode.preopen_fd())
        return *preopen_fd;

    if (metadata.is_fifo()) {
        if (options & O_WRONLY) {
            auto description = inode.fifo().open_direction_blocking(FIFO::Direction::Writer);
            description->set_rw_mode(options);
            description->set_file_flags(options);
            description->set_original_inode({}, inode);
            return description;
        } else if (options & O_RDONLY) {
            auto description = inode.fifo().open_direction_blocking(FIFO::Direction::Reader);
            description->set_rw_mode(options);
            description->set_file_flags(options);
            description->set_original_inode({}, inode);
            return description;
        }
        return KResult(-EINVAL);
    }

    if (metadata.is_device()) {
        if (custody.mount_flags() & MS_NODEV)
            return KResult(-EACCES);
        auto device = Device::get_device(metadata.major_device, metadata.minor_device);
        if (device == nullptr) {
            return KResult(-ENODEV);
        }
        auto descriptor_or_error = device->open(options);
        if (descriptor_or_error.is_error())
            return descriptor_or_error.error();
        descriptor_or_error.value()->set_original_inode({}, inode);
        return descriptor_or_error;
    }

    // Check for read-only FS. Do this after handling preopen FD and devices,
    // but before modifying the inode in any way.
    if ((options & O_WRONLY) && custody.is_readonly())
        return KResult(-EROFS);

    if (should_truncate_file) {
        KResult result = inode.truncate(0);
        if (result.is_error())
            return result;
        inode.set_mtime(kgettimeofday().tv_sec);
    }
    auto description = FileDescription::create(custody);
    description->set_rw_mode(options);
    description->set_file_flags(options);
    return description;
}

KResult VFS::mknod(StringView path, mode_t mode, dev_t dev, Custody& base)
{
    if (!is_regular_file(mode) && !is_block_device(mode) && !is_character_device(mode) && !is_fifo(mode) && !is_socket(mode))
        return KResult(-EINVAL);

    RefPtr<Custody> parent_custody;
    auto existing_file_or_error = resolve_path(path, base, &parent_custody);
    if (!existing_file_or_error.is_error())
        return KResult(-EEXIST);
    if (!parent_custody)
        return KResult(-ENOENT);
    if (existing_file_or_error.error() != -ENOENT)
        return existing_file_or_error.error();
    auto& parent_inode = parent_custody->inode();
    auto current_process = Process::current();
    if (!parent_inode.metadata().may_write(*current_process))
        return KResult(-EACCES);
    if (parent_custody->is_readonly())
        return KResult(-EROFS);

    LexicalPath p(path);
    dbg() << "VFS::mknod: '" << p.basename() << "' mode=" << mode << " dev=" << dev << " in " << parent_inode.identifier();
    return parent_inode.create_child(p.basename(), mode, dev, current_process->uid(), current_process->gid()).result();
}

KResultOr<NonnullRefPtr<FileDescription>> VFS::create(StringView path, int options, mode_t mode, Custody& parent_custody, Optional<UidAndGid> owner)
{
    auto result = validate_path_against_process_veil(path, options);
    if (result.is_error())
        return result;

    if (!is_socket(mode) && !is_fifo(mode) && !is_block_device(mode) && !is_character_device(mode)) {
        // Turn it into a regular file. (This feels rather hackish.)
        mode |= 0100000;
    }

    auto& parent_inode = parent_custody.inode();
    auto current_process = Process::current();
    if (!parent_inode.metadata().may_write(*current_process))
        return KResult(-EACCES);
    if (parent_custody.is_readonly())
        return KResult(-EROFS);

    LexicalPath p(path);
#ifdef VFS_DEBUG
    dbg() << "VFS::create: '" << p.basename() << "' in " << parent_inode.identifier();
#endif
    uid_t uid = owner.has_value() ? owner.value().uid : current_process->uid();
    gid_t gid = owner.has_value() ? owner.value().gid : current_process->gid();
    auto inode_or_error = parent_inode.create_child(p.basename(), mode, 0, uid, gid);
    if (inode_or_error.is_error())
        return inode_or_error.error();

    auto new_custody = Custody::create(&parent_custody, p.basename(), inode_or_error.value(), parent_custody.mount_flags());
    auto description = FileDescription::create(*new_custody);
    description->set_rw_mode(options);
    description->set_file_flags(options);
    return description;
}

KResult VFS::mkdir(StringView path, mode_t mode, Custody& base)
{
    // Unlike in basically every other case, where it's only the last
    // path component (the one being created) that is allowed not to
    // exist, POSIX allows mkdir'ed path to have trailing slashes.
    // Let's handle that case by trimming any trailing slashes.
    while (path.length() > 1 && path.ends_with("/"))
        path = path.substring_view(0, path.length() - 1);

    RefPtr<Custody> parent_custody;
    auto result = resolve_path(path, base, &parent_custody);
    if (!result.is_error())
        return KResult(-EEXIST);
    if (!parent_custody)
        return KResult(-ENOENT);
    if (result.error() != -ENOENT)
        return result.error();

    auto& parent_inode = parent_custody->inode();
    auto current_process = Process::current();
    if (!parent_inode.metadata().may_write(*current_process))
        return KResult(-EACCES);
    if (parent_custody->is_readonly())
        return KResult(-EROFS);

    LexicalPath p(path);
#ifdef VFS_DEBUG
    dbg() << "VFS::mkdir: '" << p.basename() << "' in " << parent_inode.identifier();
#endif
    return parent_inode.create_child(p.basename(), S_IFDIR | mode, 0, current_process->uid(), current_process->gid()).result();
}

KResult VFS::access(StringView path, int mode, Custody& base)
{
    auto custody_or_error = resolve_path(path, base);
    if (custody_or_error.is_error())
        return custody_or_error.error();
    auto& custody = *custody_or_error.value();
    auto& inode = custody.inode();
    auto metadata = inode.metadata();
    auto current_process = Process::current();
    if (mode & R_OK) {
        if (!metadata.may_read(*current_process))
            return KResult(-EACCES);
    }
    if (mode & W_OK) {
        if (!metadata.may_write(*current_process))
            return KResult(-EACCES);
        if (custody.is_readonly())
            return KResult(-EROFS);
    }
    if (mode & X_OK) {
        if (!metadata.may_execute(*current_process))
            return KResult(-EACCES);
    }
    return KSuccess;
}

KResultOr<NonnullRefPtr<Custody>> VFS::open_directory(StringView path, Custody& base)
{
    auto inode_or_error = resolve_path(path, base);
    if (inode_or_error.is_error())
        return inode_or_error.error();
    auto& custody = *inode_or_error.value();
    auto& inode = custody.inode();
    if (!inode.is_directory())
        return KResult(-ENOTDIR);
    if (!inode.metadata().may_execute(*Process::current()))
        return KResult(-EACCES);
    return custody;
}

KResult VFS::chmod(Custody& custody, mode_t mode)
{
    auto& inode = custody.inode();

    auto current_process = Process::current();
    if (current_process->euid() != inode.metadata().uid && !current_process->is_superuser())
        return KResult(-EPERM);
    if (custody.is_readonly())
        return KResult(-EROFS);

    // Only change the permission bits.
    mode = (inode.mode() & ~04777u) | (mode & 04777u);
    return inode.chmod(mode);
}

KResult VFS::chmod(StringView path, mode_t mode, Custody& base)
{
    auto custody_or_error = resolve_path(path, base);
    if (custody_or_error.is_error())
        return custody_or_error.error();
    auto& custody = *custody_or_error.value();
    return chmod(custody, mode);
}

KResult VFS::rename(StringView old_path, StringView new_path, Custody& base)
{
    RefPtr<Custody> old_parent_custody;
    auto old_custody_or_error = resolve_path(old_path, base, &old_parent_custody);
    if (old_custody_or_error.is_error())
        return old_custody_or_error.error();
    auto& old_custody = *old_custody_or_error.value();
    auto& old_inode = old_custody.inode();

    RefPtr<Custody> new_parent_custody;
    auto new_custody_or_error = resolve_path(new_path, base, &new_parent_custody);
    if (new_custody_or_error.is_error()) {
        if (new_custody_or_error.error() != -ENOENT || !new_parent_custody)
            return new_custody_or_error.error();
    }

    auto& old_parent_inode = old_parent_custody->inode();
    auto& new_parent_inode = new_parent_custody->inode();

    if (&old_parent_inode.fs() != &new_parent_inode.fs())
        return KResult(-EXDEV);

    auto current_process = Process::current();
    if (!new_parent_inode.metadata().may_write(*current_process))
        return KResult(-EACCES);

    if (!old_parent_inode.metadata().may_write(*current_process))
        return KResult(-EACCES);

    if (old_parent_inode.metadata().is_sticky()) {
        if (!current_process->is_superuser() && old_inode.metadata().uid != current_process->euid())
            return KResult(-EACCES);
    }

    if (old_parent_custody->is_readonly() || new_parent_custody->is_readonly())
        return KResult(-EROFS);

    auto new_basename = LexicalPath(new_path).basename();

    if (!new_custody_or_error.is_error()) {
        auto& new_custody = *new_custody_or_error.value();
        auto& new_inode = new_custody.inode();
        // FIXME: Is this really correct? Check what other systems do.
        if (&new_inode == &old_inode)
            return KSuccess;
        if (new_parent_inode.metadata().is_sticky()) {
            if (!current_process->is_superuser() && new_inode.metadata().uid != current_process->euid())
                return KResult(-EACCES);
        }
        if (new_inode.is_directory() && !old_inode.is_directory())
            return KResult(-EISDIR);
        auto result = new_parent_inode.remove_child(new_basename);
        if (result.is_error())
            return result;
    }

    auto result = new_parent_inode.add_child(old_inode, new_basename, old_inode.mode());
    if (result.is_error())
        return result;

    result = old_parent_inode.remove_child(LexicalPath(old_path).basename());
    if (result.is_error())
        return result;

    return KSuccess;
}

KResult VFS::chown(Custody& custody, uid_t a_uid, gid_t a_gid)
{
    auto& inode = custody.inode();
    auto metadata = inode.metadata();

    auto current_process = Process::current();
    if (current_process->euid() != metadata.uid && !current_process->is_superuser())
        return KResult(-EPERM);

    uid_t new_uid = metadata.uid;
    gid_t new_gid = metadata.gid;

    if (a_uid != (uid_t)-1) {
        if (current_process->euid() != a_uid && !current_process->is_superuser())
            return KResult(-EPERM);
        new_uid = a_uid;
    }
    if (a_gid != (gid_t)-1) {
        if (!current_process->in_group(a_gid) && !current_process->is_superuser())
            return KResult(-EPERM);
        new_gid = a_gid;
    }

    if (custody.is_readonly())
        return KResult(-EROFS);

    dbg() << "VFS::chown(): inode " << inode.identifier() << " <- uid:" << new_uid << " gid:" << new_gid;

    if (metadata.is_setuid() || metadata.is_setgid()) {
        dbg() << "VFS::chown(): Stripping SUID/SGID bits from " << inode.identifier();
        auto result = inode.chmod(metadata.mode & ~(04000 | 02000));
        if (result.is_error())
            return result;
    }

    return inode.chown(new_uid, new_gid);
}

KResult VFS::chown(StringView path, uid_t a_uid, gid_t a_gid, Custody& base)
{
    auto custody_or_error = resolve_path(path, base);
    if (custody_or_error.is_error())
        return custody_or_error.error();
    auto& custody = *custody_or_error.value();
    return chown(custody, a_uid, a_gid);
}

KResult VFS::link(StringView old_path, StringView new_path, Custody& base)
{
    auto old_custody_or_error = resolve_path(old_path, base);
    if (old_custody_or_error.is_error())
        return old_custody_or_error.error();
    auto& old_custody = *old_custody_or_error.value();
    auto& old_inode = old_custody.inode();

    RefPtr<Custody> parent_custody;
    auto new_custody_or_error = resolve_path(new_path, base, &parent_custody);
    if (!new_custody_or_error.is_error())
        return KResult(-EEXIST);

    if (!parent_custody)
        return KResult(-ENOENT);

    auto& parent_inode = parent_custody->inode();

    if (parent_inode.fsid() != old_inode.fsid())
        return KResult(-EXDEV);

    if (!parent_inode.metadata().may_write(*Process::current()))
        return KResult(-EACCES);

    if (old_inode.is_directory())
        return KResult(-EPERM);

    if (parent_custody->is_readonly())
        return KResult(-EROFS);

    return parent_inode.add_child(old_inode, LexicalPath(new_path).basename(), old_inode.mode());
}

KResult VFS::unlink(StringView path, Custody& base)
{
    RefPtr<Custody> parent_custody;
    auto custody_or_error = resolve_path(path, base, &parent_custody, O_NOFOLLOW_NOERROR | O_UNLINK_INTERNAL);
    if (custody_or_error.is_error())
        return custody_or_error.error();
    auto& custody = *custody_or_error.value();
    auto& inode = custody.inode();

    if (inode.is_directory())
        return KResult(-EISDIR);

    // We have just checked that the inode is not a directory, and thus it's not
    // the root. So it should have a parent. Note that this would be invalidated
    // if we were to support bind-mounting regular files on top of the root.
    ASSERT(parent_custody);

    auto& parent_inode = parent_custody->inode();
    auto current_process = Process::current();
    if (!parent_inode.metadata().may_write(*current_process))
        return KResult(-EACCES);

    if (parent_inode.metadata().is_sticky()) {
        if (!current_process->is_superuser() && inode.metadata().uid != current_process->euid())
            return KResult(-EACCES);
    }

    if (parent_custody->is_readonly())
        return KResult(-EROFS);

    auto result = parent_inode.remove_child(LexicalPath(path).basename());
    if (result.is_error())
        return result;

    return KSuccess;
}

KResult VFS::symlink(StringView target, StringView linkpath, Custody& base)
{
    RefPtr<Custody> parent_custody;
    auto existing_custody_or_error = resolve_path(linkpath, base, &parent_custody);
    if (!existing_custody_or_error.is_error())
        return KResult(-EEXIST);
    if (!parent_custody)
        return KResult(-ENOENT);
    if (existing_custody_or_error.error() != -ENOENT)
        return existing_custody_or_error.error();
    auto& parent_inode = parent_custody->inode();
    auto current_process = Process::current();
    if (!parent_inode.metadata().may_write(*current_process))
        return KResult(-EACCES);
    if (parent_custody->is_readonly())
        return KResult(-EROFS);

    LexicalPath p(linkpath);
    dbg() << "VFS::symlink: '" << p.basename() << "' (-> '" << target << "') in " << parent_inode.identifier();
    auto inode_or_error = parent_inode.create_child(p.basename(), S_IFLNK | 0644, 0, current_process->uid(), current_process->gid());
    if (inode_or_error.is_error())
        return inode_or_error.error();
    auto& inode = inode_or_error.value();
    auto target_buffer = UserOrKernelBuffer::for_kernel_buffer(const_cast<u8*>((const u8*)target.characters_without_null_termination()));
    ssize_t nwritten = inode->write_bytes(0, target.length(), target_buffer, nullptr);
    if (nwritten < 0)
        return KResult(nwritten);
    return KSuccess;
}

KResult VFS::rmdir(StringView path, Custody& base)
{
    RefPtr<Custody> parent_custody;
    auto custody_or_error = resolve_path(path, base, &parent_custody);
    if (custody_or_error.is_error())
        return KResult(custody_or_error.error());

    auto& custody = *custody_or_error.value();
    auto& inode = custody.inode();

    // FIXME: We should return EINVAL if the last component of the path is "."
    // FIXME: We should return ENOTEMPTY if the last component of the path is ".."

    if (!inode.is_directory())
        return KResult(-ENOTDIR);

    if (!parent_custody)
        return KResult(-EBUSY);

    auto& parent_inode = parent_custody->inode();

    if (!parent_inode.metadata().may_write(*Process::current()))
        return KResult(-EACCES);

    KResultOr<size_t> dir_count_result = inode.directory_entry_count();
    if (dir_count_result.is_error())
        return dir_count_result.result();

    if (dir_count_result.value() != 2)
        return KResult(-ENOTEMPTY);

    if (custody.is_readonly())
        return KResult(-EROFS);

    auto result = inode.remove_child(".");
    if (result.is_error())
        return result;

    result = inode.remove_child("..");
    if (result.is_error())
        return result;

    return parent_inode.remove_child(LexicalPath(path).basename());
}

VFS::Mount::Mount(FS& guest_fs, Custody* host_custody, int flags)
    : m_guest(guest_fs.root_inode())
    , m_guest_fs(guest_fs)
    , m_host_custody(host_custody)
    , m_flags(flags)
{
}

VFS::Mount::Mount(Inode& source, Custody& host_custody, int flags)
    : m_guest(source)
    , m_guest_fs(source.fs())
    , m_host_custody(host_custody)
    , m_flags(flags)
{
}

String VFS::Mount::absolute_path() const
{
    if (!m_host_custody)
        return "/";
    return m_host_custody->absolute_path();
}

Inode* VFS::Mount::host()
{
    if (!m_host_custody)
        return nullptr;
    return &m_host_custody->inode();
}

const Inode* VFS::Mount::host() const
{
    if (!m_host_custody)
        return nullptr;
    return &m_host_custody->inode();
}

void VFS::for_each_mount(Function<void(const Mount&)> callback) const
{
    for (auto& mount : m_mounts) {
        callback(mount);
    }
}

void VFS::sync()
{
    FS::sync();
}

Custody& VFS::root_custody()
{
    if (!m_root_custody)
        m_root_custody = Custody::create(nullptr, "", *m_root_inode, root_mount_flags);
    return *m_root_custody;
}

const UnveiledPath* VFS::find_matching_unveiled_path(StringView path)
{
    for (auto& unveiled_path : Process::current()->unveiled_paths()) {
        if (path == unveiled_path.path)
            return &unveiled_path;
        if (path.starts_with(unveiled_path.path) && path.length() > unveiled_path.path.length() && path[unveiled_path.path.length()] == '/')
            return &unveiled_path;
    }
    return nullptr;
}

KResult VFS::validate_path_against_process_veil(StringView path, int options)
{
    if (Process::current()->veil_state() == VeilState::None)
        return KSuccess;

    // FIXME: Figure out a nicer way to do this.
    if (String(path).contains("/.."))
        return KResult(-EINVAL);

    auto* unveiled_path = find_matching_unveiled_path(path);
    if (!unveiled_path) {
        dbg() << "Rejecting path '" << path << "' since it hasn't been unveiled.";
        dump_backtrace();
        return KResult(-ENOENT);
    }

    if (options & O_CREAT) {
        if (!(unveiled_path->permissions & UnveiledPath::Access::CreateOrRemove)) {
            dbg() << "Rejecting path '" << path << "' since it hasn't been unveiled with 'c' permission.";
            dump_backtrace();
            return KResult(-EACCES);
        }
    }
    if (options & O_UNLINK_INTERNAL) {
        if (!(unveiled_path->permissions & UnveiledPath::Access::CreateOrRemove)) {
            dbg() << "Rejecting path '" << path << "' for unlink since it hasn't been unveiled with 'c' permission.";
            dump_backtrace();
            return KResult(-EACCES);
        }
        return KSuccess;
    }
    if (options & O_RDONLY) {
        if (!(unveiled_path->permissions & UnveiledPath::Access::Read)) {
            dbg() << "Rejecting path '" << path << "' since it hasn't been unveiled with 'r' permission.";
            dump_backtrace();
            return KResult(-EACCES);
        }
    }
    if (options & O_WRONLY) {
        if (!(unveiled_path->permissions & UnveiledPath::Access::Write)) {
            dbg() << "Rejecting path '" << path << "' since it hasn't been unveiled with 'w' permission.";
            dump_backtrace();
            return KResult(-EACCES);
        }
    }
    if (options & O_EXEC) {
        if (!(unveiled_path->permissions & UnveiledPath::Access::Execute)) {
            dbg() << "Rejecting path '" << path << "' since it hasn't been unveiled with 'x' permission.";
            dump_backtrace();
            return KResult(-EACCES);
        }
    }
    return KSuccess;
}

KResultOr<NonnullRefPtr<Custody>> VFS::resolve_path(StringView path, Custody& base, RefPtr<Custody>* out_parent, int options, int symlink_recursion_level)
{
    auto custody_or_error = resolve_path_without_veil(path, base, out_parent, options, symlink_recursion_level);
    if (custody_or_error.is_error())
        return custody_or_error.error();

    auto& custody = custody_or_error.value();
    auto result = validate_path_against_process_veil(custody->absolute_path(), options);
    if (result.is_error())
        return result;

    return custody;
}

KResultOr<NonnullRefPtr<Custody>> VFS::resolve_path_without_veil(StringView path, Custody& base, RefPtr<Custody>* out_parent, int options, int symlink_recursion_level)
{
    if (symlink_recursion_level >= symlink_recursion_limit)
        return KResult(-ELOOP);

    if (path.is_empty())
        return KResult(-EINVAL);

    auto parts = path.split_view('/', true);
    auto current_process = Process::current();
    auto& current_root = current_process->root_directory();

    NonnullRefPtr<Custody> custody = path[0] == '/' ? current_root : base;

    for (size_t i = 0; i < parts.size(); ++i) {
        Custody& parent = custody;
        auto parent_metadata = parent.inode().metadata();
        if (!parent_metadata.is_directory())
            return KResult(-ENOTDIR);
        // Ensure the current user is allowed to resolve paths inside this directory.
        if (!parent_metadata.may_execute(*current_process))
            return KResult(-EACCES);

        auto& part = parts[i];
        bool have_more_parts = i + 1 < parts.size();

        if (part == "..") {
            // If we encounter a "..", take a step back, but don't go beyond the root.
            if (custody->parent())
                custody = *custody->parent();
            continue;
        } else if (part == "." || part.is_empty()) {
            continue;
        }

        // Okay, let's look up this part.
        auto child_inode = parent.inode().lookup(part);
        if (!child_inode) {
            if (out_parent) {
                // ENOENT with a non-null parent custody signals to caller that
                // we found the immediate parent of the file, but the file itself
                // does not exist yet.
                *out_parent = have_more_parts ? nullptr : &parent;
            }
            return KResult(-ENOENT);
        }

        int mount_flags_for_child = parent.mount_flags();

        // See if there's something mounted on the child; in that case
        // we would need to return the guest inode, not the host inode.
        if (auto mount = find_mount_for_host(*child_inode)) {
            child_inode = mount->guest();
            mount_flags_for_child = mount->flags();
        }

        custody = Custody::create(&parent, part, *child_inode, mount_flags_for_child);

        if (child_inode->metadata().is_symlink()) {
            if (!have_more_parts) {
                if (options & O_NOFOLLOW)
                    return KResult(-ELOOP);
                if (options & O_NOFOLLOW_NOERROR)
                    break;
            }
            auto symlink_target = child_inode->resolve_as_link(parent, out_parent, options, symlink_recursion_level + 1);
            if (symlink_target.is_error() || !have_more_parts)
                return symlink_target;

            // Now, resolve the remaining path relative to the symlink target.
            // We prepend a "." to it to ensure that it's not empty and that
            // any initial slashes it might have get interpreted properly.
            StringBuilder remaining_path;
            remaining_path.append('.');
            remaining_path.append(path.substring_view_starting_after_substring(part));

            return resolve_path_without_veil(remaining_path.to_string(), *symlink_target.value(), out_parent, options, symlink_recursion_level + 1);
        }
    }

    if (out_parent)
        *out_parent = custody->parent();
    return custody;
}

}
