/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/GenericLexer.h>
#include <AK/Singleton.h>
#include <AK/StringBuilder.h>
#include <Kernel/Debug.h>
#include <Kernel/Devices/BlockDevice.h>
#include <Kernel/FileSystem/Custody.h>
#include <Kernel/FileSystem/FileBackedFileSystem.h>
#include <Kernel/FileSystem/FileDescription.h>
#include <Kernel/FileSystem/FileSystem.h>
#include <Kernel/FileSystem/VirtualFileSystem.h>
#include <Kernel/KLexicalPath.h>
#include <Kernel/KSyms.h>
#include <Kernel/Process.h>
#include <Kernel/Sections.h>
#include <LibC/errno_numbers.h>

namespace Kernel {

static Singleton<VirtualFileSystem> s_the;
static constexpr int symlink_recursion_limit { 5 }; // FIXME: increase?
static constexpr int root_mount_flags = MS_NODEV | MS_NOSUID | MS_RDONLY;

UNMAP_AFTER_INIT void VirtualFileSystem::initialize()
{
    s_the.ensure_instance();
}

VirtualFileSystem& VirtualFileSystem::the()
{
    return *s_the;
}

UNMAP_AFTER_INIT VirtualFileSystem::VirtualFileSystem()
{
}

UNMAP_AFTER_INIT VirtualFileSystem::~VirtualFileSystem()
{
}

InodeIdentifier VirtualFileSystem::root_inode_id() const
{
    VERIFY(m_root_inode);
    return m_root_inode->identifier();
}

KResult VirtualFileSystem::mount(FileSystem& fs, Custody& mount_point, int flags)
{
    return m_mounts.with_exclusive([&](auto& mounts) -> KResult {
        auto& inode = mount_point.inode();
        dbgln("VirtualFileSystem: Mounting {} at {} (inode: {}) with flags {}",
            fs.class_name(),
            mount_point.try_create_absolute_path(),
            inode.identifier(),
            flags);
        // FIXME: check that this is not already a mount point
        Mount mount { fs, &mount_point, flags };
        mounts.append(move(mount));
        return KSuccess;
    });
}

KResult VirtualFileSystem::bind_mount(Custody& source, Custody& mount_point, int flags)
{
    return m_mounts.with_exclusive([&](auto& mounts) -> KResult {
        dbgln("VirtualFileSystem: Bind-mounting {} at {}", source.try_create_absolute_path(), mount_point.try_create_absolute_path());
        // FIXME: check that this is not already a mount point
        Mount mount { source.inode(), mount_point, flags };
        mounts.append(move(mount));
        return KSuccess;
    });
}

KResult VirtualFileSystem::remount(Custody& mount_point, int new_flags)
{
    dbgln("VirtualFileSystem: Remounting {}", mount_point.try_create_absolute_path());

    auto* mount = find_mount_for_guest(mount_point.inode().identifier());
    if (!mount)
        return ENODEV;

    mount->set_flags(new_flags);
    return KSuccess;
}

KResult VirtualFileSystem::unmount(Inode& guest_inode)
{
    dbgln("VirtualFileSystem: unmount called with inode {}", guest_inode.identifier());

    return m_mounts.with_exclusive([&](auto& mounts) -> KResult {
        for (size_t i = 0; i < mounts.size(); ++i) {
            auto& mount = mounts[i];
            if (&mount.guest() != &guest_inode)
                continue;
            if (auto result = mount.guest_fs().prepare_to_unmount(); result.is_error()) {
                dbgln("VirtualFileSystem: Failed to unmount!");
                return result;
            }
            dbgln("VirtualFileSystem: Unmounting file system {}...", mount.guest_fs().fsid());
            mounts.unstable_take(i);
            return KSuccess;
        }
        dbgln("VirtualFileSystem: Nothing mounted on inode {}", guest_inode.identifier());
        return ENODEV;
    });
}

bool VirtualFileSystem::mount_root(FileSystem& fs)
{
    if (m_root_inode) {
        dmesgln("VirtualFileSystem: mount_root can't mount another root");
        return false;
    }

    Mount mount { fs, nullptr, root_mount_flags };

    auto& root_inode = fs.root_inode();
    if (!root_inode.is_directory()) {
        dmesgln("VirtualFileSystem: root inode ({}) for / is not a directory :(", root_inode.identifier());
        return false;
    }

    m_root_inode = root_inode;
    dmesgln("VirtualFileSystem: mounted root from {} ({})", fs.class_name(), static_cast<FileBackedFileSystem&>(fs).file_description().absolute_path());

    m_mounts.with_exclusive([&](auto& mounts) {
        mounts.append(move(mount));
    });

    auto custody_or_error = Custody::try_create(nullptr, "", *m_root_inode, root_mount_flags);
    if (custody_or_error.is_error())
        return false;
    m_root_custody = custody_or_error.release_value();
    return true;
}

auto VirtualFileSystem::find_mount_for_host(InodeIdentifier id) -> Mount*
{
    return m_mounts.with_exclusive([&](auto& mounts) -> Mount* {
        for (auto& mount : mounts) {
            if (mount.host() && mount.host()->identifier() == id)
                return &mount;
        }
        return nullptr;
    });
}

auto VirtualFileSystem::find_mount_for_guest(InodeIdentifier id) -> Mount*
{
    return m_mounts.with_exclusive([&](auto& mounts) -> Mount* {
        for (auto& mount : mounts) {
            if (mount.guest().identifier() == id)
                return &mount;
        }
        return nullptr;
    });
}

bool VirtualFileSystem::is_vfs_root(InodeIdentifier inode) const
{
    return inode == root_inode_id();
}

KResult VirtualFileSystem::traverse_directory_inode(Inode& dir_inode, Function<bool(FileSystem::DirectoryEntryView const&)> callback)
{
    return dir_inode.traverse_as_directory([&](auto& entry) {
        InodeIdentifier resolved_inode;
        if (auto mount = find_mount_for_host(entry.inode))
            resolved_inode = mount->guest().identifier();
        else
            resolved_inode = entry.inode;

        // FIXME: This is now broken considering chroot and bind mounts.
        bool is_root_inode = dir_inode.identifier() == dir_inode.fs().root_inode().identifier();
        if (is_root_inode && !is_vfs_root(dir_inode.identifier()) && entry.name == "..") {
            auto mount = find_mount_for_guest(dir_inode.identifier());
            VERIFY(mount);
            VERIFY(mount->host());
            resolved_inode = mount->host()->identifier();
        }
        callback({ entry.name, resolved_inode, entry.file_type });
        return true;
    });
}

KResult VirtualFileSystem::utime(StringView path, Custody& base, time_t atime, time_t mtime)
{
    auto custody = TRY(resolve_path(path, base));
    auto& inode = custody->inode();
    auto& current_process = Process::current();
    if (!current_process.is_superuser() && inode.metadata().uid != current_process.euid())
        return EACCES;
    if (custody->is_readonly())
        return EROFS;

    if (auto result = inode.set_atime(atime); result.is_error())
        return result;
    if (auto result = inode.set_mtime(mtime); result.is_error())
        return result;
    return KSuccess;
}

KResultOr<InodeMetadata> VirtualFileSystem::lookup_metadata(StringView path, Custody& base, int options)
{
    auto custody = TRY(resolve_path(path, base, nullptr, options));
    return custody->inode().metadata();
}

KResultOr<NonnullRefPtr<FileDescription>> VirtualFileSystem::open(StringView path, int options, mode_t mode, Custody& base, Optional<UidAndGid> owner)
{
    if ((options & O_CREAT) && (options & O_DIRECTORY))
        return EINVAL;

    RefPtr<Custody> parent_custody;
    auto custody_or_error = resolve_path(path, base, &parent_custody, options);
    if (custody_or_error.is_error()) {
        // NOTE: ENOENT with a non-null parent custody signals us that the immediate parent
        //       of the file exists, but the file itself does not.
        if ((options & O_CREAT) && custody_or_error.error() == ENOENT && parent_custody)
            return create(path, options, mode, *parent_custody, move(owner));
        return custody_or_error.error();
    }

    if ((options & O_CREAT) && (options & O_EXCL))
        return EEXIST;

    auto& custody = *custody_or_error.value();
    auto& inode = custody.inode();
    auto metadata = inode.metadata();

    if ((options & O_DIRECTORY) && !metadata.is_directory())
        return ENOTDIR;

    bool should_truncate_file = false;

    auto& current_process = Process::current();
    if ((options & O_RDONLY) && !metadata.may_read(current_process))
        return EACCES;

    if (options & O_WRONLY) {
        if (!metadata.may_write(current_process))
            return EACCES;
        if (metadata.is_directory())
            return EISDIR;
        should_truncate_file = options & O_TRUNC;
    }
    if (options & O_EXEC) {
        if (!metadata.may_execute(current_process) || (custody.mount_flags() & MS_NOEXEC))
            return EACCES;
    }

    if (auto preopen_fd = inode.preopen_fd())
        return *preopen_fd;

    if (metadata.is_fifo()) {
        auto fifo = inode.fifo();
        if (options & O_WRONLY) {
            auto open_result = fifo->open_direction_blocking(FIFO::Direction::Writer);
            if (open_result.is_error())
                return open_result.error();
            auto& description = open_result.value();
            description->set_rw_mode(options);
            description->set_file_flags(options);
            description->set_original_inode({}, inode);
            return description;
        } else if (options & O_RDONLY) {
            auto open_result = fifo->open_direction_blocking(FIFO::Direction::Reader);
            if (open_result.is_error())
                return open_result.error();
            auto& description = open_result.value();
            description->set_rw_mode(options);
            description->set_file_flags(options);
            description->set_original_inode({}, inode);
            return description;
        }
        return EINVAL;
    }

    if (metadata.is_device()) {
        if (custody.mount_flags() & MS_NODEV)
            return EACCES;
        auto device = Device::get_device(metadata.major_device, metadata.minor_device);
        if (device == nullptr) {
            return ENODEV;
        }
        auto description = TRY(device->open(options));
        description->set_original_inode({}, inode);
        return description;
    }

    // Check for read-only FS. Do this after handling preopen FD and devices,
    // but before modifying the inode in any way.
    if ((options & O_WRONLY) && custody.is_readonly())
        return EROFS;

    if (should_truncate_file) {
        if (auto result = inode.truncate(0); result.is_error())
            return result;
        if (auto result = inode.set_mtime(kgettimeofday().to_truncated_seconds()); result.is_error())
            return result;
    }
    auto description = FileDescription::try_create(custody);
    if (!description.is_error()) {
        description.value()->set_rw_mode(options);
        description.value()->set_file_flags(options);
    }
    return description;
}

KResult VirtualFileSystem::mknod(StringView path, mode_t mode, dev_t dev, Custody& base)
{
    if (!is_regular_file(mode) && !is_block_device(mode) && !is_character_device(mode) && !is_fifo(mode) && !is_socket(mode))
        return EINVAL;

    RefPtr<Custody> parent_custody;
    auto existing_file_or_error = resolve_path(path, base, &parent_custody);
    if (!existing_file_or_error.is_error())
        return EEXIST;
    if (!parent_custody)
        return ENOENT;
    if (existing_file_or_error.error() != ENOENT)
        return existing_file_or_error.error();
    auto& parent_inode = parent_custody->inode();
    auto& current_process = Process::current();
    if (!parent_inode.metadata().may_write(current_process))
        return EACCES;
    if (parent_custody->is_readonly())
        return EROFS;

    auto basename = KLexicalPath::basename(path);
    dbgln("VirtualFileSystem::mknod: '{}' mode={} dev={} in {}", basename, mode, dev, parent_inode.identifier());
    return parent_inode.create_child(basename, mode, dev, current_process.euid(), current_process.egid()).result();
}

KResultOr<NonnullRefPtr<FileDescription>> VirtualFileSystem::create(StringView path, int options, mode_t mode, Custody& parent_custody, Optional<UidAndGid> owner)
{
    auto basename = KLexicalPath::basename(path);
    auto parent_path = parent_custody.try_create_absolute_path();
    if (!parent_path)
        return ENOMEM;
    auto full_path = KLexicalPath::try_join(parent_path->view(), basename);
    if (!full_path)
        return ENOMEM;
    if (auto result = validate_path_against_process_veil(full_path->view(), options); result.is_error())
        return result;

    if (!is_socket(mode) && !is_fifo(mode) && !is_block_device(mode) && !is_character_device(mode)) {
        // Turn it into a regular file. (This feels rather hackish.)
        mode |= 0100000;
    }

    auto& parent_inode = parent_custody.inode();
    auto& current_process = Process::current();
    if (!parent_inode.metadata().may_write(current_process))
        return EACCES;
    if (parent_custody.is_readonly())
        return EROFS;

    dbgln_if(VFS_DEBUG, "VirtualFileSystem::create: '{}' in {}", basename, parent_inode.identifier());
    auto uid = owner.has_value() ? owner.value().uid : current_process.euid();
    auto gid = owner.has_value() ? owner.value().gid : current_process.egid();

    auto inode = TRY(parent_inode.create_child(basename, mode, 0, uid, gid));
    auto custody = TRY(Custody::try_create(&parent_custody, basename, inode, parent_custody.mount_flags()));

    auto description = FileDescription::try_create(move(custody));
    if (!description.is_error()) {
        description.value()->set_rw_mode(options);
        description.value()->set_file_flags(options);
    }
    return description;
}

KResult VirtualFileSystem::mkdir(StringView path, mode_t mode, Custody& base)
{
    // Unlike in basically every other case, where it's only the last
    // path component (the one being created) that is allowed not to
    // exist, POSIX allows mkdir'ed path to have trailing slashes.
    // Let's handle that case by trimming any trailing slashes.
    path = path.trim("/"sv, TrimMode::Right);
    if (path.is_empty()) {
        // NOTE: This means the path was a series of slashes, which resolves to "/".
        path = "/";
    }

    RefPtr<Custody> parent_custody;
    auto result = resolve_path(path, base, &parent_custody);
    if (!result.is_error())
        return EEXIST;
    else if (!parent_custody)
        return result.error();
    // NOTE: If resolve_path fails with a non-null parent custody, the error should be ENOENT.
    VERIFY(result.error() == ENOENT);

    auto& parent_inode = parent_custody->inode();
    auto& current_process = Process::current();
    if (!parent_inode.metadata().may_write(current_process))
        return EACCES;
    if (parent_custody->is_readonly())
        return EROFS;

    auto basename = KLexicalPath::basename(path);
    dbgln_if(VFS_DEBUG, "VirtualFileSystem::mkdir: '{}' in {}", basename, parent_inode.identifier());
    return parent_inode.create_child(basename, S_IFDIR | mode, 0, current_process.euid(), current_process.egid()).result();
}

KResult VirtualFileSystem::access(StringView path, int mode, Custody& base)
{
    auto custody = TRY(resolve_path(path, base));

    auto& inode = custody->inode();
    auto metadata = inode.metadata();
    auto& current_process = Process::current();
    if (mode & R_OK) {
        if (!metadata.may_read(current_process))
            return EACCES;
    }
    if (mode & W_OK) {
        if (!metadata.may_write(current_process))
            return EACCES;
        if (custody->is_readonly())
            return EROFS;
    }
    if (mode & X_OK) {
        if (!metadata.may_execute(current_process))
            return EACCES;
    }
    return KSuccess;
}

KResultOr<NonnullRefPtr<Custody>> VirtualFileSystem::open_directory(StringView path, Custody& base)
{
    auto custody = TRY(resolve_path(path, base));
    auto& inode = custody->inode();
    if (!inode.is_directory())
        return ENOTDIR;
    if (!inode.metadata().may_execute(Process::current()))
        return EACCES;
    return custody;
}

KResult VirtualFileSystem::chmod(Custody& custody, mode_t mode)
{
    auto& inode = custody.inode();

    auto& current_process = Process::current();
    if (current_process.euid() != inode.metadata().uid && !current_process.is_superuser())
        return EPERM;
    if (custody.is_readonly())
        return EROFS;

    // Only change the permission bits.
    mode = (inode.mode() & ~07777u) | (mode & 07777u);
    return inode.chmod(mode);
}

KResult VirtualFileSystem::chmod(StringView path, mode_t mode, Custody& base)
{
    auto custody = TRY(resolve_path(path, base));
    return chmod(custody, mode);
}

KResult VirtualFileSystem::rename(StringView old_path, StringView new_path, Custody& base)
{
    RefPtr<Custody> old_parent_custody;
    auto old_custody = TRY(resolve_path(old_path, base, &old_parent_custody, O_NOFOLLOW_NOERROR));
    auto& old_inode = old_custody->inode();

    RefPtr<Custody> new_parent_custody;
    auto new_custody_or_error = resolve_path(new_path, base, &new_parent_custody);
    if (new_custody_or_error.is_error()) {
        if (new_custody_or_error.error() != ENOENT || !new_parent_custody)
            return new_custody_or_error.error();
    }

    if (!old_parent_custody || !new_parent_custody) {
        return EPERM;
    }

    if (!new_custody_or_error.is_error()) {
        auto& new_inode = new_custody_or_error.value()->inode();

        if (old_inode.index() != new_inode.index() && old_inode.is_directory() && new_inode.is_directory()) {
            size_t child_count = 0;
            auto traversal_result = new_inode.traverse_as_directory([&child_count](auto&) {
                ++child_count;
                return child_count <= 2;
            });
            if (traversal_result.is_error())
                return traversal_result;
            if (child_count > 2)
                return ENOTEMPTY;
        }
    }

    auto& old_parent_inode = old_parent_custody->inode();
    auto& new_parent_inode = new_parent_custody->inode();

    if (&old_parent_inode.fs() != &new_parent_inode.fs())
        return EXDEV;

    for (auto* new_ancestor = new_parent_custody.ptr(); new_ancestor; new_ancestor = new_ancestor->parent()) {
        if (&old_inode == &new_ancestor->inode())
            return EDIRINTOSELF;
    }

    auto& current_process = Process::current();
    if (!new_parent_inode.metadata().may_write(current_process))
        return EACCES;

    if (!old_parent_inode.metadata().may_write(current_process))
        return EACCES;

    if (old_parent_inode.metadata().is_sticky()) {
        if (!current_process.is_superuser() && old_inode.metadata().uid != current_process.euid())
            return EACCES;
    }

    if (old_parent_custody->is_readonly() || new_parent_custody->is_readonly())
        return EROFS;

    auto old_basename = KLexicalPath::basename(old_path);
    if (old_basename.is_empty() || old_basename == "."sv || old_basename == ".."sv)
        return EINVAL;

    auto new_basename = KLexicalPath::basename(new_path);
    if (new_basename.is_empty() || new_basename == "."sv || new_basename == ".."sv)
        return EINVAL;

    if (old_basename == new_basename && old_parent_inode.index() == new_parent_inode.index())
        return KSuccess;

    if (!new_custody_or_error.is_error()) {
        auto& new_custody = *new_custody_or_error.value();
        auto& new_inode = new_custody.inode();
        // FIXME: Is this really correct? Check what other systems do.
        if (&new_inode == &old_inode)
            return KSuccess;
        if (new_parent_inode.metadata().is_sticky()) {
            if (!current_process.is_superuser() && new_inode.metadata().uid != current_process.euid())
                return EACCES;
        }
        if (new_inode.is_directory() && !old_inode.is_directory())
            return EISDIR;
        if (auto result = new_parent_inode.remove_child(new_basename); result.is_error())
            return result;
    }

    if (auto result = new_parent_inode.add_child(old_inode, new_basename, old_inode.mode()); result.is_error())
        return result;

    if (auto result = old_parent_inode.remove_child(old_basename); result.is_error())
        return result;

    return KSuccess;
}

KResult VirtualFileSystem::chown(Custody& custody, UserID a_uid, GroupID a_gid)
{
    auto& inode = custody.inode();
    auto metadata = inode.metadata();

    auto& current_process = Process::current();
    if (current_process.euid() != metadata.uid && !current_process.is_superuser())
        return EPERM;

    UserID new_uid = metadata.uid;
    GroupID new_gid = metadata.gid;

    if (a_uid != (uid_t)-1) {
        if (current_process.euid() != a_uid && !current_process.is_superuser())
            return EPERM;
        new_uid = a_uid;
    }
    if (a_gid != (gid_t)-1) {
        if (!current_process.in_group(a_gid) && !current_process.is_superuser())
            return EPERM;
        new_gid = a_gid;
    }

    if (custody.is_readonly())
        return EROFS;

    dbgln_if(VFS_DEBUG, "VirtualFileSystem::chown(): inode {} <- uid={} gid={}", inode.identifier(), new_uid, new_gid);

    if (metadata.is_setuid() || metadata.is_setgid()) {
        dbgln_if(VFS_DEBUG, "VirtualFileSystem::chown(): Stripping SUID/SGID bits from {}", inode.identifier());
        if (auto result = inode.chmod(metadata.mode & ~(04000 | 02000)); result.is_error())
            return result;
    }

    return inode.chown(new_uid, new_gid);
}

KResult VirtualFileSystem::chown(StringView path, UserID a_uid, GroupID a_gid, Custody& base)
{
    auto custody = TRY(resolve_path(path, base));
    return chown(custody, a_uid, a_gid);
}

static bool hard_link_allowed(const Inode& inode)
{
    auto metadata = inode.metadata();

    if (Process::current().euid() == metadata.uid)
        return true;

    if (metadata.is_regular_file()
        && !metadata.is_setuid()
        && !(metadata.is_setgid() && metadata.mode & S_IXGRP)
        && metadata.may_write(Process::current())) {
        return true;
    }

    return false;
}

KResult VirtualFileSystem::link(StringView old_path, StringView new_path, Custody& base)
{
    auto old_custody = TRY(resolve_path(old_path, base));
    auto& old_inode = old_custody->inode();

    RefPtr<Custody> parent_custody;
    auto new_custody_or_error = resolve_path(new_path, base, &parent_custody);
    if (!new_custody_or_error.is_error())
        return EEXIST;

    if (!parent_custody)
        return ENOENT;

    auto& parent_inode = parent_custody->inode();

    if (parent_inode.fsid() != old_inode.fsid())
        return EXDEV;

    if (!parent_inode.metadata().may_write(Process::current()))
        return EACCES;

    if (old_inode.is_directory())
        return EPERM;

    if (parent_custody->is_readonly())
        return EROFS;

    if (!hard_link_allowed(old_inode))
        return EPERM;

    return parent_inode.add_child(old_inode, KLexicalPath::basename(new_path), old_inode.mode());
}

KResult VirtualFileSystem::unlink(StringView path, Custody& base)
{
    RefPtr<Custody> parent_custody;
    auto custody = TRY(resolve_path(path, base, &parent_custody, O_NOFOLLOW_NOERROR | O_UNLINK_INTERNAL));
    auto& inode = custody->inode();

    if (inode.is_directory())
        return EISDIR;

    // We have just checked that the inode is not a directory, and thus it's not
    // the root. So it should have a parent. Note that this would be invalidated
    // if we were to support bind-mounting regular files on top of the root.
    VERIFY(parent_custody);

    auto& parent_inode = parent_custody->inode();
    auto& current_process = Process::current();
    if (!parent_inode.metadata().may_write(current_process))
        return EACCES;

    if (parent_inode.metadata().is_sticky()) {
        if (!current_process.is_superuser() && inode.metadata().uid != current_process.euid())
            return EACCES;
    }

    if (parent_custody->is_readonly())
        return EROFS;

    if (auto result = parent_inode.remove_child(KLexicalPath::basename(path)); result.is_error())
        return result;

    return KSuccess;
}

KResult VirtualFileSystem::symlink(StringView target, StringView linkpath, Custody& base)
{
    RefPtr<Custody> parent_custody;
    auto existing_custody_or_error = resolve_path(linkpath, base, &parent_custody);
    if (!existing_custody_or_error.is_error())
        return EEXIST;
    if (!parent_custody)
        return ENOENT;
    if (existing_custody_or_error.is_error() && existing_custody_or_error.error() != ENOENT)
        return existing_custody_or_error.error();
    auto& parent_inode = parent_custody->inode();
    auto& current_process = Process::current();
    if (!parent_inode.metadata().may_write(current_process))
        return EACCES;
    if (parent_custody->is_readonly())
        return EROFS;

    auto basename = KLexicalPath::basename(linkpath);
    dbgln_if(VFS_DEBUG, "VirtualFileSystem::symlink: '{}' (-> '{}') in {}", basename, target, parent_inode.identifier());

    auto inode = TRY(parent_inode.create_child(basename, S_IFLNK | 0644, 0, current_process.euid(), current_process.egid()));

    auto target_buffer = UserOrKernelBuffer::for_kernel_buffer(const_cast<u8*>((const u8*)target.characters_without_null_termination()));
    TRY(inode->write_bytes(0, target.length(), target_buffer, nullptr));
    return KSuccess;
}

KResult VirtualFileSystem::rmdir(StringView path, Custody& base)
{
    RefPtr<Custody> parent_custody;
    auto custody = TRY(resolve_path(path, base, &parent_custody));
    auto& inode = custody->inode();

    // FIXME: We should return EINVAL if the last component of the path is "."
    // FIXME: We should return ENOTEMPTY if the last component of the path is ".."

    if (!inode.is_directory())
        return ENOTDIR;

    if (!parent_custody)
        return EBUSY;

    auto& parent_inode = parent_custody->inode();
    auto parent_metadata = parent_inode.metadata();

    auto& current_process = Process::current();
    if (!parent_metadata.may_write(current_process))
        return EACCES;

    if (parent_metadata.is_sticky()) {
        if (!current_process.is_superuser() && inode.metadata().uid != current_process.euid())
            return EACCES;
    }

    size_t child_count = 0;
    auto traversal_result = inode.traverse_as_directory([&child_count](auto&) {
        ++child_count;
        return true;
    });
    if (traversal_result.is_error())
        return traversal_result;

    if (child_count != 2)
        return ENOTEMPTY;

    if (custody->is_readonly())
        return EROFS;

    if (auto result = inode.remove_child("."); result.is_error())
        return result;

    if (auto result = inode.remove_child(".."); result.is_error())
        return result;

    return parent_inode.remove_child(KLexicalPath::basename(path));
}

void VirtualFileSystem::for_each_mount(Function<void(Mount const&)> callback) const
{
    m_mounts.with_shared([&](auto& mounts) {
        for (auto& mount : mounts) {
            callback(mount);
        }
    });
}

void VirtualFileSystem::sync()
{
    FileSystem::sync();
}

Custody& VirtualFileSystem::root_custody()
{
    return *m_root_custody;
}

UnveilNode const& VirtualFileSystem::find_matching_unveiled_path(StringView path)
{
    auto& current_process = Process::current();
    VERIFY(current_process.veil_state() != VeilState::None);
    auto& unveil_root = current_process.unveiled_paths();

    auto path_parts = KLexicalPath::parts(path);
    return unveil_root.traverse_until_last_accessible_node(path_parts.begin(), path_parts.end());
}

KResult VirtualFileSystem::validate_path_against_process_veil(Custody const& custody, int options)
{
    if (Process::current().veil_state() == VeilState::None)
        return KSuccess;
    auto absolute_path = custody.try_create_absolute_path();
    if (!absolute_path)
        return ENOMEM;
    return validate_path_against_process_veil(absolute_path->view(), options);
}

KResult VirtualFileSystem::validate_path_against_process_veil(StringView path, int options)
{
    if (Process::current().veil_state() == VeilState::None)
        return KSuccess;
    if (path == "/usr/lib/Loader.so")
        return KSuccess;

    VERIFY(path.starts_with('/'));
    VERIFY(!path.contains("/../"sv) && !path.ends_with("/.."sv));
    VERIFY(!path.contains("/./"sv) && !path.ends_with("/."sv));

    auto& unveiled_path = find_matching_unveiled_path(path);
    if (unveiled_path.permissions() == UnveilAccess::None) {
        dbgln("Rejecting path '{}' since it hasn't been unveiled.", path);
        dump_backtrace();
        return ENOENT;
    }

    if (options & O_CREAT) {
        if (!(unveiled_path.permissions() & UnveilAccess::CreateOrRemove)) {
            dbgln("Rejecting path '{}' since it hasn't been unveiled with 'c' permission.", path);
            dump_backtrace();
            return EACCES;
        }
    }
    if (options & O_UNLINK_INTERNAL) {
        if (!(unveiled_path.permissions() & UnveilAccess::CreateOrRemove)) {
            dbgln("Rejecting path '{}' for unlink since it hasn't been unveiled with 'c' permission.", path);
            dump_backtrace();
            return EACCES;
        }
        return KSuccess;
    }
    if (options & O_RDONLY) {
        if (options & O_DIRECTORY) {
            if (!(unveiled_path.permissions() & (UnveilAccess::Read | UnveilAccess::Browse))) {
                dbgln("Rejecting path '{}' since it hasn't been unveiled with 'r' or 'b' permissions.", path);
                dump_backtrace();
                return EACCES;
            }
        } else {
            if (!(unveiled_path.permissions() & UnveilAccess::Read)) {
                dbgln("Rejecting path '{}' since it hasn't been unveiled with 'r' permission.", path);
                dump_backtrace();
                return EACCES;
            }
        }
    }
    if (options & O_WRONLY) {
        if (!(unveiled_path.permissions() & UnveilAccess::Write)) {
            dbgln("Rejecting path '{}' since it hasn't been unveiled with 'w' permission.", path);
            dump_backtrace();
            return EACCES;
        }
    }
    if (options & O_EXEC) {
        if (!(unveiled_path.permissions() & UnveilAccess::Execute)) {
            dbgln("Rejecting path '{}' since it hasn't been unveiled with 'x' permission.", path);
            dump_backtrace();
            return EACCES;
        }
    }
    return KSuccess;
}

KResultOr<NonnullRefPtr<Custody>> VirtualFileSystem::resolve_path(StringView path, Custody& base, RefPtr<Custody>* out_parent, int options, int symlink_recursion_level)
{
    auto custody = TRY(resolve_path_without_veil(path, base, out_parent, options, symlink_recursion_level));
    if (auto result = validate_path_against_process_veil(*custody, options); result.is_error())
        return result;
    return custody;
}

static bool safe_to_follow_symlink(const Inode& inode, const InodeMetadata& parent_metadata)
{
    auto metadata = inode.metadata();
    if (Process::current().euid() == metadata.uid)
        return true;

    if (!(parent_metadata.is_sticky() && parent_metadata.mode & S_IWOTH))
        return true;

    if (metadata.uid == parent_metadata.uid)
        return true;

    return false;
}

KResultOr<NonnullRefPtr<Custody>> VirtualFileSystem::resolve_path_without_veil(StringView path, Custody& base, RefPtr<Custody>* out_parent, int options, int symlink_recursion_level)
{
    if (symlink_recursion_level >= symlink_recursion_limit)
        return ELOOP;

    if (path.is_empty())
        return EINVAL;

    GenericLexer path_lexer(path);
    auto& current_process = Process::current();

    NonnullRefPtr<Custody> custody = path[0] == '/' ? root_custody() : base;
    bool extra_iteration = path[path.length() - 1] == '/';

    while (!path_lexer.is_eof() || extra_iteration) {
        if (path_lexer.is_eof())
            extra_iteration = false;
        auto part = path_lexer.consume_until('/');
        path_lexer.consume_specific('/');

        Custody& parent = custody;
        auto parent_metadata = parent.inode().metadata();
        if (!parent_metadata.is_directory())
            return ENOTDIR;
        // Ensure the current user is allowed to resolve paths inside this directory.
        if (!parent_metadata.may_execute(current_process))
            return EACCES;

        bool have_more_parts = !path_lexer.is_eof() || extra_iteration;

        if (part == "..") {
            // If we encounter a "..", take a step back, but don't go beyond the root.
            if (custody->parent())
                custody = *custody->parent();
            continue;
        } else if (part == "." || part.is_empty()) {
            continue;
        }

        // Okay, let's look up this part.
        auto child_or_error = parent.inode().lookup(part);
        if (child_or_error.is_error()) {
            if (out_parent) {
                // ENOENT with a non-null parent custody signals to caller that
                // we found the immediate parent of the file, but the file itself
                // does not exist yet.
                *out_parent = have_more_parts ? nullptr : &parent;
            }
            return child_or_error.error();
        }
        auto child_inode = child_or_error.release_value();

        int mount_flags_for_child = parent.mount_flags();

        // See if there's something mounted on the child; in that case
        // we would need to return the guest inode, not the host inode.
        if (auto mount = find_mount_for_host(child_inode->identifier())) {
            child_inode = mount->guest();
            mount_flags_for_child = mount->flags();
        }

        custody = TRY(Custody::try_create(&parent, part, *child_inode, mount_flags_for_child));

        if (child_inode->metadata().is_symlink()) {
            if (!have_more_parts) {
                if (options & O_NOFOLLOW)
                    return ELOOP;
                if (options & O_NOFOLLOW_NOERROR)
                    break;
            }

            if (!safe_to_follow_symlink(*child_inode, parent_metadata))
                return EACCES;

            if (auto result = validate_path_against_process_veil(*custody, options); result.is_error())
                return result;

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
