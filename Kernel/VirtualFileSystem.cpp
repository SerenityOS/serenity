#include "VirtualFileSystem.h"
#include "FileDescriptor.h"
#include "FileSystem.h"
#include <AK/FileSystemPath.h>
#include <AK/StringBuilder.h>
#include <AK/kmalloc.h>
#include <AK/kstdio.h>
#include <AK/ktime.h>
#include "CharacterDevice.h"
#include <LibC/errno_numbers.h>
#include <Kernel/Process.h>

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

bool VFS::mount(RetainPtr<FS>&& file_system, const String& path)
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

RetainPtr<FileDescriptor> VFS::open(RetainPtr<Device>&& device, int& error, int options)
{
    // FIXME: Respect options.
    (void) options;
    (void) error;
    return FileDescriptor::create(move(device));
}

KResult VFS::utime(const String& path, Inode& base, time_t atime, time_t mtime)
{
    int error;
    auto descriptor = VFS::the().open(move(path), error, 0, 0, base);
    if (!descriptor)
        return KResult(error);
    auto& inode = *descriptor->inode();
    if (inode.fs().is_readonly())
        return KResult(-EROFS);
    if (inode.metadata().uid != current->euid())
        return KResult(-EACCES);
    error = inode.set_atime(atime);
    if (error)
        return KResult(error);
    error = inode.set_mtime(mtime);
    if (error)
        return KResult(error);
    return KSuccess;
}

bool VFS::stat(const String& path, int& error, int options, Inode& base, struct stat& statbuf)
{
    auto inode_id = old_resolve_path(path, base.identifier(), error, options);
    if (!inode_id.is_valid())
        return false;
    error = FileDescriptor::create(get_inode(inode_id))->fstat(&statbuf);
    if (error)
        return false;
    return true;
}

RetainPtr<FileDescriptor> VFS::open(const String& path, int& error, int options, mode_t mode, Inode& base)
{
    auto inode_id = old_resolve_path(path, base.identifier(), error, options);
    auto inode = get_inode(inode_id);
    if (options & O_CREAT) {
        if (!inode)
            return create(path, error, options, mode, base);
        else if (options & O_EXCL) {
            error = -EEXIST;
            return nullptr;
        }
    }
    if (!inode)
        return nullptr;

    auto metadata = inode->metadata();

    // NOTE: Read permission is a bit weird, since O_RDONLY == 0,
    //       so we check if (NOT write_only OR read_and_write)
    if (!(options & O_WRONLY) || (options & O_RDWR)) {
        if (!metadata.may_read(*current)) {
            error = -EACCES;
            return nullptr;
        }
    }
    if ((options & O_WRONLY) || (options & O_RDWR)) {
        if (!metadata.may_write(*current)) {
            error = -EACCES;
            return nullptr;
        }
    }

    if (metadata.is_device()) {
        auto it = m_devices.find(encoded_device(metadata.major_device, metadata.minor_device));
        if (it == m_devices.end()) {
            kprintf("VFS::open: no such device %u,%u\n", metadata.major_device, metadata.minor_device);
            return nullptr;
        }
        auto descriptor = (*it).value->open(error, options);
        descriptor->set_original_inode(Badge<VFS>(), move(inode));
        return descriptor;
    }
    return FileDescriptor::create(move(inode));
}

RetainPtr<FileDescriptor> VFS::create(const String& path, int& error, int options, mode_t mode, Inode& base)
{
    (void) options;
    error = -EWHYTHO;

    if (!is_socket(mode) && !is_fifo(mode) && !is_block_device(mode) && !is_character_device(mode)) {
        // Turn it into a regular file. (This feels rather hackish.)
        mode |= 0100000;
    }

    RetainPtr<Inode> parent_inode;
    auto existing_file = resolve_path_to_inode(path, base, error, &parent_inode);
    if (existing_file) {
        error = -EEXIST;
        return nullptr;
    }
    if (!parent_inode) {
        error = -ENOENT;
        return nullptr;
    }
    if (error != -ENOENT) {
        return nullptr;
    }
    if (!parent_inode->metadata().may_write(*current)) {
        error = -EACCES;
        return nullptr;
    }

    FileSystemPath p(path);
    dbgprintf("VFS::create_file: '%s' in %u:%u\n", p.basename().characters(), parent_inode->fsid(), parent_inode->index());
    auto new_file = parent_inode->fs().create_inode(parent_inode->identifier(), p.basename(), mode, 0, error);
    if (!new_file)
        return nullptr;

    error = 0;
    return FileDescriptor::create(move(new_file));
}

KResult VFS::mkdir(const String& path, mode_t mode, Inode& base)
{
    RetainPtr<Inode> parent_inode;
    auto result = resolve_path_to_inode(path, base, &parent_inode);
    if (!result.is_error())
        return KResult(-EEXIST);
    if (!parent_inode)
        return KResult(-ENOENT);
    if (result.error() != -ENOENT)
        return result.error();

    if (!parent_inode->metadata().may_write(*current))
        return KResult(-EACCES);

    FileSystemPath p(path);
    dbgprintf("VFS::mkdir: '%s' in %u:%u\n", p.basename().characters(), parent_inode->fsid(), parent_inode->index());
    int error;
    auto new_dir = parent_inode->fs().create_directory(parent_inode->identifier(), p.basename(), mode, error);
    if (new_dir)
        return KSuccess;
    return KResult(error);
}

KResult VFS::access(const String& path, int mode, Inode& base)
{
    auto inode_or_error = resolve_path_to_inode(path, base);
    if (inode_or_error.is_error())
        return inode_or_error.error();
    auto inode = inode_or_error.value();
    auto metadata = inode->metadata();
    if (mode & R_OK) {
        if (!metadata.may_read(*current))
            return KResult(-EACCES);
    }
    if (mode & W_OK) {
        if (!metadata.may_write(*current))
            return KResult(-EACCES);
    }
    if (mode & X_OK) {
        if (!metadata.may_execute(*current))
            return KResult(-EACCES);
    }
    return KSuccess;
}

KResult VFS::chmod(const String& path, mode_t mode, Inode& base)
{
    auto inode_or_error = resolve_path_to_inode(path, base);
    if (inode_or_error.is_error())
        return inode_or_error.error();
    auto inode = inode_or_error.value();

    if (inode->fs().is_readonly())
        return KResult(-EROFS);

    if (current->euid() != inode->metadata().uid)
        return KResult(-EPERM);

    // Only change the permission bits.
    mode = (inode->mode() & ~04777) | (mode & 04777);

    kprintf("VFS::chmod(): %u:%u mode %o\n", inode->fsid(), inode->index(), mode);
    return inode->chmod(mode);
}

KResultOr<RetainPtr<Inode>> VFS::resolve_path_to_inode(const String& path, Inode& base, RetainPtr<Inode>* parent_inode)
{
    // FIXME: This won't work nicely across mount boundaries.
    FileSystemPath p(path);
    if (!p.is_valid())
        return KResult(-EINVAL);
    InodeIdentifier parent_id;
    auto result = resolve_path(path, base.identifier(), 0, &parent_id);
    if (parent_inode && parent_id.is_valid())
        *parent_inode = get_inode(parent_id);
    if (result.is_error())
        return result.error();
    return get_inode(result.value());
}

RetainPtr<Inode> VFS::resolve_path_to_inode(const String& path, Inode& base, int& error, RetainPtr<Inode>* parent_inode)
{
    // FIXME: This won't work nicely across mount boundaries.
    FileSystemPath p(path);
    if (!p.is_valid()) {
        error = -EINVAL;
        return nullptr;
    }
    InodeIdentifier parent_id;
    auto inode_id = old_resolve_path(path, base.identifier(), error, 0, &parent_id);
    if (parent_inode && parent_id.is_valid())
        *parent_inode = get_inode(parent_id);
    if (!inode_id.is_valid()) {
        error = -ENOENT;
        return nullptr;
    }
    return get_inode(inode_id);
}

bool VFS::link(const String& old_path, const String& new_path, Inode& base, int& error)
{
    auto old_inode = resolve_path_to_inode(old_path, base, error);
    if (!old_inode)
        return false;

    RetainPtr<Inode> parent_inode;
    auto new_inode = resolve_path_to_inode(new_path, base, error, &parent_inode);
    if (new_inode) {
        error = -EEXIST;
        return false;
    }
    if (!parent_inode) {
        error = -ENOENT;
        return false;
    }
    if (parent_inode->fsid() != old_inode->fsid()) {
        error = -EXDEV;
        return false;
    }
    if (parent_inode->fs().is_readonly()) {
        error = -EROFS;
        return false;
    }
    if (!parent_inode->metadata().may_write(*current)) {
        error = -EACCES;
        return false;
    }

    if (!parent_inode->add_child(old_inode->identifier(), FileSystemPath(new_path).basename(), 0, error))
        return false;
    error = 0;
    return true;
}

bool VFS::unlink(const String& path, Inode& base, int& error)
{
    RetainPtr<Inode> parent_inode;
    auto inode = resolve_path_to_inode(path, base, error, &parent_inode);
    if (!inode)
        return false;

    if (inode->is_directory()) {
        error = -EISDIR;
        return false;
    }

    if (!parent_inode->metadata().may_write(*current)) {
        error = -EACCES;
        return false;
    }

    if (!parent_inode->remove_child(FileSystemPath(path).basename(), error))
        return false;

    error = 0;
    return true;
}

bool VFS::rmdir(const String& path, Inode& base, int& error)
{
    error = -EWHYTHO;

    RetainPtr<Inode> parent_inode;
    auto inode = resolve_path_to_inode(path, base, error, &parent_inode);
    if (!inode)
        return false;

    if (inode->fs().is_readonly()) {
        error = -EROFS;
        return false;
    }

    // FIXME: We should return EINVAL if the last component of the path is "."
    // FIXME: We should return ENOTEMPTY if the last component of the path is ".."

    if (!inode->is_directory()) {
        error = -ENOTDIR;
        return false;
    }

    if (!parent_inode->metadata().may_write(*current)) {
        error = -EACCES;
        return false;
    }

    if (inode->directory_entry_count() != 2) {
        error = -ENOTEMPTY;
        return false;
    }

    dbgprintf("VFS::rmdir: Removing inode %u:%u from parent %u:%u\n", inode->fsid(), inode->index(), parent_inode->fsid(), parent_inode->index());

    // To do:
    // - Remove '.' in target (--child.link_count)
    // - Remove '..' in target (--parent.link_count)
    // - Remove target from its parent (--parent.link_count)
    if (!inode->remove_child(".", error))
        return false;

    if (!inode->remove_child("..", error))
        return false;

    // FIXME: The reverse_lookup here can definitely be avoided.
    if (!parent_inode->remove_child(parent_inode->reverse_lookup(inode->identifier()), error))
        return false;

    error = 0;
    return true;
}

KResultOr<InodeIdentifier> VFS::resolve_symbolic_link(InodeIdentifier base, Inode& symlink_inode)
{
    auto symlink_contents = symlink_inode.read_entire();
    if (!symlink_contents)
        return KResult(-ENOENT);
    auto linkee = String((const char*)symlink_contents.pointer(), symlink_contents.size());
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

String VFS::absolute_path(InodeIdentifier inode_id)
{
    auto inode = get_inode(inode_id);
    if (!inode)
        return { };
    return absolute_path(*inode);
}

String VFS::absolute_path(Inode& core_inode)
{
    int error;
    Vector<InodeIdentifier> lineage;
    RetainPtr<Inode> inode = &core_inode;
    while (inode->identifier() != root_inode_id()) {
        if (auto* mount = find_mount_for_guest(inode->identifier()))
            lineage.append(mount->host());
        else
            lineage.append(inode->identifier());

        InodeIdentifier parent_id;
        if (inode->is_directory()) {
            parent_id = old_resolve_path("..", inode->identifier(), error);
        } else {
            parent_id = inode->parent()->identifier();
        }
        ASSERT(parent_id.is_valid());
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

KResultOr<InodeIdentifier> VFS::resolve_path(const String& path, InodeIdentifier base, int options, InodeIdentifier* parent_id)
{
    if (path.is_empty())
        return KResult(-EINVAL);

    auto parts = path.split('/');
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
        if (!metadata.may_execute(*current))
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

InodeIdentifier VFS::old_resolve_path(const String& path, InodeIdentifier base, int& error, int options, InodeIdentifier* parent_id)
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
