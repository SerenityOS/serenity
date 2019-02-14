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
    auto inode = resolve_path(path, root_inode_id(), error);
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

RetainPtr<FileDescriptor> VFS::open(RetainPtr<CharacterDevice>&& device, int& error, int options)
{
    // FIXME: Respect options.
    (void) options;
    (void) error;
    return FileDescriptor::create(move(device));
}

RetainPtr<FileDescriptor> VFS::open(const String& path, int& error, int options, mode_t mode, Inode& base)
{
    auto inode_id = resolve_path(path, base.identifier(), error, options);
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
    if (!(options & O_DONT_OPEN_DEVICE) && metadata.is_character_device()) {
        auto it = m_character_devices.find(encoded_device(metadata.major_device, metadata.minor_device));
        if (it == m_character_devices.end()) {
            kprintf("VFS::open: no such character device %u,%u\n", metadata.major_device, metadata.minor_device);
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

    // FIXME: This won't work nicely across mount boundaries.
    FileSystemPath p(path);
    if (!p.is_valid()) {
        error = -EINVAL;
        return nullptr;
    }

    InodeIdentifier parent_dir;
    auto existing_file = resolve_path(path, base.identifier(), error, 0, &parent_dir);
    if (existing_file.is_valid()) {
        error = -EEXIST;
        return nullptr;
    }
    if (!parent_dir.is_valid()) {
        error = -ENOENT;
        return nullptr;
    }
    if (error != -ENOENT) {
        return nullptr;
    }
    dbgprintf("VFS::create_file: '%s' in %u:%u\n", p.basename().characters(), parent_dir.fsid(), parent_dir.index());
    auto new_file = parent_dir.fs()->create_inode(parent_dir, p.basename(), mode, 0, error);
    if (!new_file)
        return nullptr;

    error = 0;
    return FileDescriptor::create(move(new_file));
}

bool VFS::mkdir(const String& path, mode_t mode, Inode& base, int& error)
{
    error = -EWHYTHO;
    // FIXME: This won't work nicely across mount boundaries.
    FileSystemPath p(path);
    if (!p.is_valid()) {
        error = -EINVAL;
        return false;
    }

    InodeIdentifier parent_dir;
    auto existing_dir = resolve_path(path, base.identifier(), error, 0, &parent_dir);
    if (existing_dir.is_valid()) {
        error = -EEXIST;
        return false;
    }
    if (!parent_dir.is_valid()) {
        error = -ENOENT;
        return false;
    }
    if (error != -ENOENT) {
        return false;
    }
    dbgprintf("VFS::mkdir: '%s' in %u:%u\n", p.basename().characters(), parent_dir.fsid(), parent_dir.index());
    auto new_dir = parent_dir.fs()->create_directory(parent_dir, p.basename(), mode, error);
    if (new_dir) {
        error = 0;
        return true;
    }
    return false;
}

bool VFS::chmod(const String& path, mode_t mode, Inode& base, int& error)
{
    error = -EWHYTHO;
    // FIXME: This won't work nicely across mount boundaries.
    FileSystemPath p(path);
    if (!p.is_valid()) {
        error = -EINVAL;
        return false;
    }

    InodeIdentifier parent_dir;
    auto inode_id = resolve_path(path, base.identifier(), error, 0, &parent_dir);
    if (!inode_id.is_valid()) {
        error = -ENOENT;
        return false;
    }

    auto inode = get_inode(inode_id);

    // FIXME: Permission checks.

    // Only change the permission bits.
    mode = (inode->mode() & ~04777) | (mode & 04777);

    kprintf("VFS::chmod(): %u:%u mode %o\n", inode_id.fsid(), inode_id.index(), mode);
    if (!inode->chmod(mode, error))
        return false;
    error = 0;
    return true;
}

bool VFS::unlink(const String& path, Inode& base, int& error)
{
    error = -EWHYTHO;
    // FIXME: This won't work nicely across mount boundaries.
    FileSystemPath p(path);
    if (!p.is_valid()) {
        error = -EINVAL;
        return false;
    }

    InodeIdentifier parent_dir;
    auto inode_id = resolve_path(path, base.identifier(), error, 0, &parent_dir);
    if (!inode_id.is_valid()) {
        error = -ENOENT;
        return false;
    }

    auto inode = get_inode(inode_id);
    if (inode->is_directory()) {
        error = -EISDIR;
        return false;
    }

    auto parent_inode = get_inode(parent_dir);
    // FIXME: The reverse_lookup here can definitely be avoided.
    if (!parent_inode->remove_child(parent_inode->reverse_lookup(inode_id), error))
        return false;

    error = 0;
    return true;
}

bool VFS::rmdir(const String& path, Inode& base, int& error)
{
    error = -EWHYTHO;
    // FIXME: This won't work nicely across mount boundaries.
    FileSystemPath p(path);
    if (!p.is_valid()) {
        error = -EINVAL;
        return false;
    }

    InodeIdentifier parent_dir;
    auto inode_id = resolve_path(path, base.identifier(), error, 0, &parent_dir);
    if (!inode_id.is_valid()) {
        error = -ENOENT;
        return false;
    }

    if (inode_id.fs()->is_readonly()) {
        error = -EROFS;
        return false;
    }

    // FIXME: We should return EINVAL if the last component of the path is "."
    // FIXME: We should return ENOTEMPTY if the last component of the path is ".."

    auto inode = get_inode(inode_id);
    if (!inode->is_directory()) {
        error = -ENOTDIR;
        return false;
    }

    if (inode->directory_entry_count() != 2) {
        error = -ENOTEMPTY;
        return false;
    }

    auto parent_inode = get_inode(parent_dir);
    ASSERT(parent_inode);

    dbgprintf("VFS::rmdir: Removing inode %u:%u from parent %u:%u\n", inode_id.fsid(), inode_id.index(), parent_dir.fsid(), parent_dir.index());

    // To do:
    // - Remove '.' in target (--child.link_count)
    // - Remove '..' in target (--parent.link_count)
    // - Remove target from its parent (--parent.link_count)
    if (!inode->remove_child(".", error))
        return false;

    if (!inode->remove_child("..", error))
        return false;

    // FIXME: The reverse_lookup here can definitely be avoided.
    if (!parent_inode->remove_child(parent_inode->reverse_lookup(inode_id), error))
        return false;

    error = 0;
    return true;
}

InodeIdentifier VFS::resolve_symbolic_link(InodeIdentifier base, Inode& symlink_inode, int& error)
{
    auto symlink_contents = symlink_inode.read_entire();
    if (!symlink_contents)
        return { };
    auto linkee = String((const char*)symlink_contents.pointer(), symlink_contents.size());
#ifdef VFS_DEBUG
    kprintf("linkee (%s)(%u) from %u:%u\n", linkee.characters(), linkee.length(), base.fsid(), base.index());
#endif
    return resolve_path(linkee, base, error);
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
            parent_id = resolve_path("..", inode->identifier(), error);
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

InodeIdentifier VFS::resolve_path(const String& path, InodeIdentifier base, int& error, int options, InodeIdentifier* parent_id)
{
    if (path.is_empty()) {
        error = -EINVAL;
        return { };
    }

    auto parts = path.split('/');
    InodeIdentifier crumb_id;

    if (path[0] == '/')
        crumb_id = root_inode_id();
    else
        crumb_id = base.is_valid() ? base : root_inode_id();

    if (parent_id)
        *parent_id = crumb_id;

    for (unsigned i = 0; i < parts.size(); ++i) {
        bool inode_was_root_at_head_of_loop = crumb_id.is_root_inode();
        auto& part = parts[i];
        if (part.is_empty())
            break;
        auto crumb_inode = get_inode(crumb_id);
        if (!crumb_inode) {
#ifdef VFS_DEBUG
            kprintf("invalid metadata\n");
#endif
            error = -EIO;
            return { };
        }
        auto metadata = crumb_inode->metadata();
        if (!metadata.is_directory()) {
#ifdef VFS_DEBUG
            kprintf("parent of <%s> not directory, it's inode %u:%u / %u:%u, mode: %u, size: %u\n", part.characters(), crumb_id.fsid(), crumb_id.index(), metadata.inode.fsid(), metadata.inode.index(), metadata.mode, metadata.size);
#endif
            error = -ENOTDIR;
            return { };
        }
        auto parent = crumb_id;
        crumb_id = crumb_inode->lookup(part);
        if (!crumb_id.is_valid()) {
#ifdef VFS_DEBUG
            kprintf("child <%s>(%u) not found in directory, %02u:%08u\n", part.characters(), part.length(), parent.fsid(), parent.index());
#endif
            error = -ENOENT;
            return { };
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
            crumb_id = dir_inode->lookup("..");
        }
        crumb_inode = get_inode(crumb_id);
        metadata = crumb_inode->metadata();
        if (metadata.is_directory()) {
            if (i != parts.size() - 1) {
                if (parent_id)
                    *parent_id = crumb_id;
            }
        }
        if (metadata.is_symlink()) {
            if (i == parts.size() - 1) {
                if (options & O_NOFOLLOW) {
                    error = -ELOOP;
                    return { };
                }
                if (options & O_NOFOLLOW_NOERROR)
                    return crumb_id;
            }
            crumb_id = resolve_symbolic_link(parent, *crumb_inode, error);
            if (!crumb_id.is_valid()) {
                kprintf("Symbolic link resolution failed :(\n");
                return { };
            }
        }
    }

    return crumb_id;
}

VFS::Mount::Mount(InodeIdentifier host, RetainPtr<FS>&& guest_fs)
    : m_host(host)
    , m_guest(guest_fs->root_inode())
    , m_guest_fs(move(guest_fs))
{
}

void VFS::register_character_device(CharacterDevice& device)
{
    m_character_devices.set(encoded_device(device.major(), device.minor()), &device);
}

void VFS::unregister_character_device(CharacterDevice& device)
{
    m_character_devices.remove(encoded_device(device.major(), device.minor()));
}

CharacterDevice* VFS::get_device(unsigned major, unsigned minor)
{
    auto it = m_character_devices.find(encoded_device(major, minor));
    if (it == m_character_devices.end())
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
