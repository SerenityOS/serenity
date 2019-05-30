#include "VirtualFileSystem.h"
#include <Kernel/FileSystem/FileDescriptor.h>
#include "FileSystem.h"
#include <AK/FileSystemPath.h>
#include <AK/StringBuilder.h>
#include <Kernel/Devices/CharacterDevice.h>
#include <LibC/errno_numbers.h>
#include <Kernel/Process.h>
#include <Kernel/FileSystem/Custody.h>

//#define VFS_DEBUG

static VFS* s_the;

VFS& VFS::the()
{
    ASSERT(s_the);
    return *s_the;
}

VFS::VFS()
{
#ifdef VFS_DEBUG
    kprintf("VFS: Constructing VFS\n");
#endif
    s_the = this;
}

VFS::~VFS()
{
}

InodeIdentifier VFS::root_inode_id() const
{
    ASSERT(m_root_inode);
    return m_root_inode->identifier();
}

bool VFS::mount(RetainPtr<FS>&& file_system, StringView path)
{
    ASSERT(file_system);
    int error;
    auto inode = old_resolve_path(path, root_inode_id(), error);
    if (!inode.is_valid()) {
        kprintf("VFS: mount can't resolve mount point '%s'\n", path.characters());
        return false;
    }

    kprintf("VFS: mounting %s{%p} at %s (inode: %u)\n", file_system->class_name(), file_system.ptr(), path.characters(), inode.index());
    // FIXME: check that this is not already a mount point
    auto mount = make<Mount>(inode, move(file_system));
    m_mounts.append(move(mount));
    return true;
}

bool VFS::mount_root(RetainPtr<FS>&& file_system)
{
    if (m_root_inode) {
        kprintf("VFS: mount_root can't mount another root\n");
        return false;
    }

    auto mount = make<Mount>(InodeIdentifier(), move(file_system));

    auto root_inode_id = mount->guest().fs()->root_inode();
    auto root_inode = mount->guest().fs()->get_inode(root_inode_id);
    if (!root_inode->is_directory()) {
        kprintf("VFS: root inode (%02u:%08u) for / is not a directory :(\n", root_inode_id.fsid(), root_inode_id.index());
        return false;
    }

    m_root_inode = move(root_inode);

    kprintf("VFS: mounted root on %s{%p}\n",
        m_root_inode->fs().class_name(),
        &m_root_inode->fs());

    m_mounts.append(move(mount));
    return true;
}

auto VFS::find_mount_for_host(InodeIdentifier inode) -> Mount*
{
    for (auto& mount : m_mounts) {
        if (mount->host() == inode)
            return mount.ptr();
    }
    return nullptr;
}

auto VFS::find_mount_for_guest(InodeIdentifier inode) -> Mount*
{
    for (auto& mount : m_mounts) {
        if (mount->guest() == inode)
            return mount.ptr();
    }
    return nullptr;
}

bool VFS::is_vfs_root(InodeIdentifier inode) const
{
    return inode == root_inode_id();
}

void VFS::traverse_directory_inode(Inode& dir_inode, Function<bool(const FS::DirectoryEntry&)> callback)
{
    dir_inode.traverse_as_directory([&] (const FS::DirectoryEntry& entry) {
        InodeIdentifier resolved_inode;
        if (auto mount = find_mount_for_host(entry.inode))
            resolved_inode = mount->guest();
        else
            resolved_inode = entry.inode;

        if (dir_inode.identifier().is_root_inode() && !is_vfs_root(dir_inode.identifier()) && !strcmp(entry.name, "..")) {
            auto mount = find_mount_for_guest(entry.inode);
            ASSERT(mount);
            resolved_inode = mount->host();
        }
        callback(FS::DirectoryEntry(entry.name, entry.name_length, resolved_inode, entry.file_type));
        return true;
    });
}

KResult VFS::utime(StringView path, Inode& base, time_t atime, time_t mtime)
{
    auto descriptor_or_error = VFS::the().open(move(path), 0, 0, base);
    if (descriptor_or_error.is_error())
        return descriptor_or_error.error();
    auto& inode = *descriptor_or_error.value()->inode();
    if (inode.fs().is_readonly())
        return KResult(-EROFS);
    if (inode.metadata().uid != current->process().euid())
        return KResult(-EACCES);

    int error = inode.set_atime(atime);
    if (error)
        return KResult(error);
    error = inode.set_mtime(mtime);
    if (error)
        return KResult(error);
    return KSuccess;
}

KResult VFS::stat(StringView path, int options, Inode& base, struct stat& statbuf)
{
    auto inode_or_error = resolve_path_to_inode(path, base, nullptr, options);
    if (inode_or_error.is_error())
        return inode_or_error.error();
    return FileDescriptor::create(inode_or_error.value().ptr())->fstat(statbuf);
}

KResultOr<Retained<FileDescriptor>> VFS::open(StringView path, int options, mode_t mode, Inode& base)
{
    auto inode_or_error = resolve_path_to_inode(path, base, nullptr, options);
    if (options & O_CREAT) {
        if (inode_or_error.is_error())
            return create(path, options, mode, base);
        if (options & O_EXCL)
            return KResult(-EEXIST);
    }
    if (inode_or_error.is_error())
        return inode_or_error.error();

    auto inode = inode_or_error.value();
    auto metadata = inode->metadata();

    bool should_truncate_file = false;

    // NOTE: Read permission is a bit weird, since O_RDONLY == 0,
    //       so we check if (NOT write_only OR read_and_write)
    if (!(options & O_WRONLY) || (options & O_RDWR)) {
        if (!metadata.may_read(current->process()))
            return KResult(-EACCES);
    }
    if ((options & O_WRONLY) || (options & O_RDWR)) {
        if (!metadata.may_write(current->process()))
            return KResult(-EACCES);
        if (metadata.is_directory())
            return KResult(-EISDIR);
        should_truncate_file = options & O_TRUNC;
    }

    if (metadata.is_device()) {
        auto it = m_devices.find(encoded_device(metadata.major_device, metadata.minor_device));
        if (it == m_devices.end()) {
            return KResult(-ENODEV);
        }
        auto descriptor_or_error = (*it).value->open(options);
        if (descriptor_or_error.is_error())
            return descriptor_or_error.error();
        descriptor_or_error.value()->set_original_inode(Badge<VFS>(), *inode);
        return descriptor_or_error;
    }
    if (should_truncate_file)
        inode->truncate(0);
    return FileDescriptor::create(*inode);
}

KResult VFS::mknod(StringView path, mode_t mode, dev_t dev, Inode& base)
{
    if (!is_regular_file(mode) && !is_block_device(mode) && !is_character_device(mode) && !is_fifo(mode) && !is_socket(mode))
        return KResult(-EINVAL);

    RetainPtr<Inode> parent_inode;
    auto existing_file_or_error = resolve_path_to_inode(path, base, &parent_inode);
    if (!existing_file_or_error.is_error())
        return KResult(-EEXIST);
    if (!parent_inode)
        return KResult(-ENOENT);
    if (existing_file_or_error.error() != -ENOENT)
        return existing_file_or_error.error();
    if (!parent_inode->metadata().may_write(current->process()))
        return KResult(-EACCES);

    FileSystemPath p(path);
    dbgprintf("VFS::mknod: '%s' mode=%o dev=%u in %u:%u\n", p.basename().characters(), mode, dev, parent_inode->fsid(), parent_inode->index());
    int error;
    auto new_file = parent_inode->fs().create_inode(parent_inode->identifier(), p.basename(), mode, 0, dev, error);
    if (!new_file)
        return KResult(error);

    return KSuccess;
}

KResultOr<Retained<FileDescriptor>> VFS::create(StringView path, int options, mode_t mode, Inode& base)
{
    (void)options;

    if (!is_socket(mode) && !is_fifo(mode) && !is_block_device(mode) && !is_character_device(mode)) {
        // Turn it into a regular file. (This feels rather hackish.)
        mode |= 0100000;
    }

    RetainPtr<Inode> parent_inode;
    auto existing_file_or_error = resolve_path_to_inode(path, base, &parent_inode);
    if (!existing_file_or_error.is_error())
        return KResult(-EEXIST);
    if (!parent_inode)
        return KResult(-ENOENT);
    if (existing_file_or_error.error() != -ENOENT)
        return existing_file_or_error.error();
    if (!parent_inode->metadata().may_write(current->process()))
        return KResult(-EACCES);

    FileSystemPath p(path);
    dbgprintf("VFS::create_file: '%s' in %u:%u\n", p.basename().characters(), parent_inode->fsid(), parent_inode->index());
    int error;
    auto new_file = parent_inode->fs().create_inode(parent_inode->identifier(), p.basename(), mode, 0, 0, error);
    if (!new_file)
        return KResult(error);

    return FileDescriptor::create(move(new_file));
}

KResult VFS::mkdir(StringView path, mode_t mode, Inode& base)
{
    RetainPtr<Inode> parent_inode;
    auto result = resolve_path_to_inode(path, base, &parent_inode);
    if (!result.is_error())
        return KResult(-EEXIST);
    if (!parent_inode)
        return KResult(-ENOENT);
    if (result.error() != -ENOENT)
        return result.error();

    if (!parent_inode->metadata().may_write(current->process()))
        return KResult(-EACCES);

    FileSystemPath p(path);
    dbgprintf("VFS::mkdir: '%s' in %u:%u\n", p.basename().characters(), parent_inode->fsid(), parent_inode->index());
    int error;
    auto new_dir = parent_inode->fs().create_directory(parent_inode->identifier(), p.basename(), mode, error);
    if (new_dir)
        return KSuccess;
    return KResult(error);
}

KResult VFS::access(StringView path, int mode, Inode& base)
{
    auto inode_or_error = resolve_path_to_inode(path, base);
    if (inode_or_error.is_error())
        return inode_or_error.error();
    auto inode = inode_or_error.value();
    auto metadata = inode->metadata();
    if (mode & R_OK) {
        if (!metadata.may_read(current->process()))
            return KResult(-EACCES);
    }
    if (mode & W_OK) {
        if (!metadata.may_write(current->process()))
            return KResult(-EACCES);
    }
    if (mode & X_OK) {
        if (!metadata.may_execute(current->process()))
            return KResult(-EACCES);
    }
    return KSuccess;
}

KResultOr<Retained<Inode>> VFS::open_directory(StringView path, Inode& base)
{
    auto inode_or_error = resolve_path_to_inode(path, base);
    if (inode_or_error.is_error())
        return inode_or_error.error();
    auto inode = inode_or_error.value();
    if (!inode->is_directory())
        return KResult(-ENOTDIR);
    if (!inode->metadata().may_execute(current->process()))
        return KResult(-EACCES);
    return Retained<Inode>(*inode);
}

KResult VFS::chmod(Inode& inode, mode_t mode)
{
    if (inode.fs().is_readonly())
        return KResult(-EROFS);

    if (current->process().euid() != inode.metadata().uid && !current->process().is_superuser())
        return KResult(-EPERM);

    // Only change the permission bits.
    mode = (inode.mode() & ~04777u) | (mode & 04777u);
    return inode.chmod(mode);
}

KResult VFS::chmod(StringView path, mode_t mode, Inode& base)
{
    auto inode_or_error = resolve_path_to_inode(path, base);
    if (inode_or_error.is_error())
        return inode_or_error.error();
    auto inode = inode_or_error.value();
    return chmod(*inode, mode);
}

KResult VFS::rename(StringView old_path, StringView new_path, Inode& base)
{
    RetainPtr<Inode> old_parent_inode;
    auto old_inode_or_error = resolve_path_to_inode(old_path, base, &old_parent_inode);
    if (old_inode_or_error.is_error())
        return old_inode_or_error.error();
    auto old_inode = old_inode_or_error.value();

    RetainPtr<Inode> new_parent_inode;
    auto new_inode_or_error = resolve_path_to_inode(new_path, base, &new_parent_inode);
    if (new_inode_or_error.is_error()) {
        if (new_inode_or_error.error() != -ENOENT)
            return new_inode_or_error.error();
    }

    if (!new_parent_inode->metadata().may_write(current->process()))
        return KResult(-EACCES);

    if (!old_parent_inode->metadata().may_write(current->process()))
        return KResult(-EACCES);

    if (old_parent_inode->metadata().is_sticky()) {
        if (!current->process().is_superuser() && old_inode->metadata().uid != current->process().euid())
            return KResult(-EACCES);
    }

    if (!new_inode_or_error.is_error()) {
        auto new_inode = new_inode_or_error.value();
        // FIXME: Is this really correct? Check what other systems do.
        if (new_inode == old_inode)
            return KSuccess;
        if (new_parent_inode->metadata().is_sticky()) {
            if (!current->process().is_superuser() && new_inode->metadata().uid != current->process().euid())
                return KResult(-EACCES);
        }
        if (new_inode->is_directory() && !old_inode->is_directory())
            return KResult(-EISDIR);
        auto result = new_parent_inode->remove_child(FileSystemPath(new_path).basename());
        if (result.is_error())
            return result;
    }

    auto result = new_parent_inode->add_child(old_inode->identifier(), FileSystemPath(new_path).basename(), 0 /* FIXME: file type? */);
    if (result.is_error())
        return result;

    result = old_parent_inode->remove_child(FileSystemPath(old_path).basename());
    if (result.is_error())
        return result;

    return KSuccess;
}

KResult VFS::chown(StringView path, uid_t a_uid, gid_t a_gid, Inode& base)
{
    auto inode_or_error = resolve_path_to_inode(path, base);
    if (inode_or_error.is_error())
        return inode_or_error.error();
    auto inode = inode_or_error.value();

    if (inode->fs().is_readonly())
        return KResult(-EROFS);

    if (current->process().euid() != inode->metadata().uid && !current->process().is_superuser())
        return KResult(-EPERM);

    uid_t new_uid = inode->metadata().uid;
    gid_t new_gid = inode->metadata().gid;

    if (a_uid != (uid_t)-1) {
        if (current->process().euid() != a_uid && !current->process().is_superuser())
            return KResult(-EPERM);
        new_uid = a_uid;
    }
    if (a_gid != (gid_t)-1) {
        if (!current->process().in_group(a_gid) && !current->process().is_superuser())
            return KResult(-EPERM);
        new_gid = a_gid;
    }

    dbgprintf("VFS::chown(): inode %u:%u <- uid:%d, gid:%d\n", inode->fsid(), inode->index(), new_uid, new_gid);
    return inode->chown(new_uid, new_gid);
}

KResultOr<Retained<Inode>> VFS::resolve_path_to_inode(StringView path, Inode& base, RetainPtr<Inode>* parent_inode, int options)
{
    // FIXME: This won't work nicely across mount boundaries.
    FileSystemPath p(path);
    if (!p.is_valid())
        return KResult(-EINVAL);
    InodeIdentifier parent_id;
    auto result = resolve_path(path, base.identifier(), options, &parent_id);
    if (parent_inode && parent_id.is_valid())
        *parent_inode = get_inode(parent_id);
    if (result.is_error())
        return result.error();
    return Retained<Inode>(*get_inode(result.value()));
}

KResult VFS::link(StringView old_path, StringView new_path, Inode& base)
{
    auto old_inode_or_error = resolve_path_to_inode(old_path, base);
    if (old_inode_or_error.is_error())
        return old_inode_or_error.error();
    auto old_inode = old_inode_or_error.value();

    RetainPtr<Inode> parent_inode;
    auto new_inode_or_error = resolve_path_to_inode(new_path, base, &parent_inode);
    if (!new_inode_or_error.is_error())
        return KResult(-EEXIST);

    if (!parent_inode)
        return KResult(-ENOENT);

    if (parent_inode->fsid() != old_inode->fsid())
        return KResult(-EXDEV);

    if (parent_inode->fs().is_readonly())
        return KResult(-EROFS);

    if (!parent_inode->metadata().may_write(current->process()))
        return KResult(-EACCES);

    return parent_inode->add_child(old_inode->identifier(), FileSystemPath(new_path).basename(), 0);
}

KResult VFS::unlink(StringView path, Inode& base)
{
    RetainPtr<Inode> parent_inode;
    auto inode_or_error = resolve_path_to_inode(path, base, &parent_inode);
    if (inode_or_error.is_error())
        return inode_or_error.error();
    auto inode = inode_or_error.value();

    if (inode->is_directory())
        return KResult(-EISDIR);

    if (!parent_inode->metadata().may_write(current->process()))
        return KResult(-EACCES);

    if (parent_inode->metadata().is_sticky()) {
        if (!current->process().is_superuser() && inode->metadata().uid != current->process().euid())
            return KResult(-EACCES);
    }

    return parent_inode->remove_child(FileSystemPath(path).basename());
}

KResult VFS::symlink(StringView target, StringView linkpath, Inode& base)
{
    RetainPtr<Inode> parent_inode;
    auto existing_file_or_error = resolve_path_to_inode(linkpath, base, &parent_inode);
    if (!existing_file_or_error.is_error())
        return KResult(-EEXIST);
    if (!parent_inode)
        return KResult(-ENOENT);
    if (existing_file_or_error.error() != -ENOENT)
        return existing_file_or_error.error();
    if (!parent_inode->metadata().may_write(current->process()))
        return KResult(-EACCES);

    FileSystemPath p(linkpath);
    dbgprintf("VFS::symlink: '%s' (-> '%s') in %u:%u\n", p.basename().characters(), target.characters(), parent_inode->fsid(), parent_inode->index());
    int error;
    auto new_file = parent_inode->fs().create_inode(parent_inode->identifier(), p.basename(), 0120644, 0, 0, error);
    if (!new_file)
        return KResult(error);
    ssize_t nwritten = new_file->write_bytes(0, target.length(), (const byte*)target.characters(), nullptr);
    if (nwritten < 0)
        return KResult(nwritten);
    return KSuccess;
}

KResult VFS::rmdir(StringView path, Inode& base)
{
    RetainPtr<Inode> parent_inode;
    auto inode_or_error = resolve_path_to_inode(path, base, &parent_inode);
    if (inode_or_error.is_error())
        return KResult(inode_or_error.error());

    auto inode = inode_or_error.value();
    if (inode->fs().is_readonly())
        return KResult(-EROFS);

    // FIXME: We should return EINVAL if the last component of the path is "."
    // FIXME: We should return ENOTEMPTY if the last component of the path is ".."

    if (!inode->is_directory())
        return KResult(-ENOTDIR);

    if (!parent_inode->metadata().may_write(current->process()))
        return KResult(-EACCES);

    if (inode->directory_entry_count() != 2)
        return KResult(-ENOTEMPTY);

    auto result = inode->remove_child(".");
    if (result.is_error())
        return result;

    result = inode->remove_child("..");
    if (result.is_error())
        return result;

    return parent_inode->remove_child(FileSystemPath(path).basename());
}

KResultOr<InodeIdentifier> VFS::resolve_symbolic_link(InodeIdentifier base, Inode& symlink_inode)
{
    auto symlink_contents = symlink_inode.read_entire();
    if (!symlink_contents)
        return KResult(-ENOENT);
    auto linkee = StringView(symlink_contents.pointer(), symlink_contents.size());
#ifdef VFS_DEBUG
    kprintf("linkee (%s)(%u) from %u:%u\n", linkee.characters(), linkee.length(), base.fsid(), base.index());
#endif
    return resolve_path(linkee, base);
}

RetainPtr<Inode> VFS::get_inode(InodeIdentifier inode_id)
{
    if (!inode_id.is_valid())
        return nullptr;
    return inode_id.fs()->get_inode(inode_id);
}

KResultOr<String> VFS::absolute_path(InodeIdentifier inode_id)
{
    auto inode = get_inode(inode_id);
    if (!inode)
        return KResult(-EIO);
    return absolute_path(*inode);
}

KResultOr<String> VFS::absolute_path(Inode& core_inode)
{
    Vector<InodeIdentifier> lineage;
    RetainPtr<Inode> inode = &core_inode;
    while (inode->identifier() != root_inode_id()) {
        if (auto* mount = find_mount_for_guest(inode->identifier()))
            lineage.append(mount->host());
        else
            lineage.append(inode->identifier());

        InodeIdentifier parent_id;
        if (inode->is_directory()) {
            auto result = resolve_path("..", inode->identifier());
            if (result.is_error())
                return result.error();
            parent_id = result.value();
        } else {
            parent_id = inode->parent()->identifier();
        }
        if (!parent_id.is_valid())
            return KResult(-EIO);
        inode = get_inode(parent_id);
    }
    if (lineage.is_empty())
        return "/";
    lineage.append(root_inode_id());
    StringBuilder builder;
    for (size_t i = lineage.size() - 1; i >= 1; --i) {
        auto& child = lineage[i - 1];
        auto parent = lineage[i];
        if (auto* mount = find_mount_for_host(parent))
            parent = mount->guest();
        builder.append('/');
        auto parent_inode = get_inode(parent);
        builder.append(parent_inode->reverse_lookup(child));
    }
    return builder.to_string();
}

KResultOr<InodeIdentifier> VFS::resolve_path(StringView path, InodeIdentifier base, int options, InodeIdentifier* parent_id)
{
    if (path.is_empty())
        return KResult(-EINVAL);

    auto parts = path.split_view('/');
    InodeIdentifier crumb_id;

    if (path[0] == '/')
        crumb_id = root_inode_id();
    else
        crumb_id = base.is_valid() ? base : root_inode_id();

    if (parent_id)
        *parent_id = crumb_id;

    for (int i = 0; i < parts.size(); ++i) {
        bool inode_was_root_at_head_of_loop = crumb_id.is_root_inode();
        auto& part = parts[i];
        if (part.is_empty())
            break;
        auto crumb_inode = get_inode(crumb_id);
        if (!crumb_inode) {
#ifdef VFS_DEBUG
            kprintf("invalid metadata\n");
#endif
            return KResult(-EIO);
        }
        auto metadata = crumb_inode->metadata();
        if (!metadata.is_directory()) {
#ifdef VFS_DEBUG
            kprintf("parent of <%s> not directory, it's inode %u:%u / %u:%u, mode: %u, size: %u\n", part.characters(), crumb_id.fsid(), crumb_id.index(), metadata.inode.fsid(), metadata.inode.index(), metadata.mode, metadata.size);
#endif
            return KResult(-ENOTDIR);
        }
        if (!metadata.may_execute(current->process()))
            return KResult(-EACCES);
        auto parent = crumb_id;
        crumb_id = crumb_inode->lookup(part);
        if (!crumb_id.is_valid()) {
#ifdef VFS_DEBUG
            kprintf("child <%s>(%u) not found in directory, %02u:%08u\n", part.characters(), part.length(), parent.fsid(), parent.index());
#endif
            return KResult(-ENOENT);
        }
#ifdef VFS_DEBUG
        kprintf("<%s> %u:%u\n", part.characters(), crumb_id.fsid(), crumb_id.index());
#endif
        if (auto mount = find_mount_for_host(crumb_id)) {
#ifdef VFS_DEBUG
            kprintf("  -- is host\n");
#endif
            crumb_id = mount->guest();
        }
        if (inode_was_root_at_head_of_loop && crumb_id.is_root_inode() && !is_vfs_root(crumb_id) && part == "..") {
#ifdef VFS_DEBUG
            kprintf("  -- is guest\n");
#endif
            auto mount = find_mount_for_guest(crumb_id);
            auto dir_inode = get_inode(mount->host());
            ASSERT(dir_inode);
            crumb_id = dir_inode->lookup("..");
        }
        crumb_inode = get_inode(crumb_id);
        ASSERT(crumb_inode);
        metadata = crumb_inode->metadata();
        if (metadata.is_directory()) {
            if (i != parts.size() - 1) {
                if (parent_id)
                    *parent_id = crumb_id;
            }
        }
        if (metadata.is_symlink()) {
            if (i == parts.size() - 1) {
                if (options & O_NOFOLLOW)
                    return KResult(-ELOOP);
                if (options & O_NOFOLLOW_NOERROR)
                    return crumb_id;
            }
            auto result = resolve_symbolic_link(parent, *crumb_inode);
            if (result.is_error())
                return KResult(-ENOENT);
            crumb_id = result.value();
            ASSERT(crumb_id.is_valid());
        }
    }
    return crumb_id;
}

InodeIdentifier VFS::old_resolve_path(StringView path, InodeIdentifier base, int& error, int options, InodeIdentifier* parent_id)
{
    auto result = resolve_path(path, base, options, parent_id);
    if (result.is_error()) {
        error = result.error();
        return { };
    }
    return result.value();
}

VFS::Mount::Mount(InodeIdentifier host, RetainPtr<FS>&& guest_fs)
    : m_host(host)
    , m_guest(guest_fs->root_inode())
    , m_guest_fs(move(guest_fs))
{
}

void VFS::register_device(Device& device)
{
    m_devices.set(encoded_device(device.major(), device.minor()), &device);
}

void VFS::unregister_device(Device& device)
{
    m_devices.remove(encoded_device(device.major(), device.minor()));
}

Device* VFS::get_device(unsigned major, unsigned minor)
{
    auto it = m_devices.find(encoded_device(major, minor));
    if (it == m_devices.end())
        return nullptr;
    return (*it).value;
}

void VFS::for_each_mount(Function<void(const Mount&)> callback) const
{
    for (auto& mount : m_mounts) {
        callback(*mount);
    }
}

void VFS::sync()
{
    FS::sync();
}

Custody& VFS::root_custody()
{
    if (!m_root_custody)
        m_root_custody = Custody::create(nullptr, "", *root_inode());
    return *m_root_custody;
}

KResultOr<Retained<Custody>> VFS::resolve_path_to_custody(StringView path, Custody& base, int options)
{
    if (path.is_empty())
        return KResult(-EINVAL);

    auto parts = path.split_view('/');
    InodeIdentifier crumb_id;

    Vector<Retained<Custody>, 32> custody_chain;

    if (path[0] == '/') {
        custody_chain.append(Retained<Custody>(base));
        crumb_id = root_inode_id();
    } else {
        for (auto* custody = &base; custody; custody = custody->parent()) {
            // FIXME: Prepending here is not efficient! Fix this.
            custody_chain.prepend(*custody);
        }
        crumb_id = base.inode().identifier();
    }

    for (int i = 0; i < parts.size(); ++i) {
        bool inode_was_root_at_head_of_loop = crumb_id.is_root_inode();
        auto& part = parts[i];
        if (part.is_empty())
            break;
        auto crumb_inode = get_inode(crumb_id);
        if (!crumb_inode)
            return KResult(-EIO);
        auto metadata = crumb_inode->metadata();
        if (!metadata.is_directory())
            return KResult(-ENOTDIR);
        if (!metadata.may_execute(current->process()))
            return KResult(-EACCES);
        auto parent = crumb_id;
        crumb_id = crumb_inode->lookup(part);
        if (!crumb_id.is_valid())
            return KResult(-ENOENT);
        if (auto mount = find_mount_for_host(crumb_id))
            crumb_id = mount->guest();
        if (inode_was_root_at_head_of_loop && crumb_id.is_root_inode() && !is_vfs_root(crumb_id) && part == "..") {
            auto mount = find_mount_for_guest(crumb_id);
            auto dir_inode = get_inode(mount->host());
            ASSERT(dir_inode);
            crumb_id = dir_inode->lookup("..");
        }
        crumb_inode = get_inode(crumb_id);
        ASSERT(crumb_inode);
        custody_chain.append(Custody::create(custody_chain.last().ptr(), part, *crumb_inode));
        metadata = crumb_inode->metadata();
        if (metadata.is_symlink()) {
            if (i == parts.size() - 1) {
                if (options & O_NOFOLLOW)
                    return KResult(-ELOOP);
                if (options & O_NOFOLLOW_NOERROR)
                    return custody_chain.last();
            }
            auto result = resolve_symbolic_link(parent, *crumb_inode);
            if (result.is_error())
                return KResult(-ENOENT);
            crumb_id = result.value();
            ASSERT(crumb_id.is_valid());
        }
    }
    return custody_chain.last();
}
