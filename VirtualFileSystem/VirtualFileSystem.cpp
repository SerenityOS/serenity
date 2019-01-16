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

void VFS::initialize_globals()
{
    s_the = nullptr;
    FS::initialize_globals();
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

bool VFS::mount(RetainPtr<FS>&& fileSystem, const String& path)
{
    ASSERT(fileSystem);
    int error;
    auto inode = resolve_path(path, root_inode_id(), error);
    if (!inode.is_valid()) {
        kprintf("VFS: mount can't resolve mount point '%s'\n", path.characters());
        return false;
    }

    kprintf("VFS: mounting %s{%p} at %s (inode: %u)\n", fileSystem->class_name(), fileSystem.ptr(), path.characters(), inode.index());
    // FIXME: check that this is not already a mount point
    auto mount = make<Mount>(inode, move(fileSystem));
    m_mounts.append(move(mount));
    return true;
}

bool VFS::mount_root(RetainPtr<FS>&& fileSystem)
{
    if (m_root_inode) {
        kprintf("VFS: mount_root can't mount another root\n");
        return false;
    }

    auto mount = make<Mount>(InodeIdentifier(), move(fileSystem));

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
        InodeIdentifier resolvedInode;
        if (auto mount = find_mount_for_host(entry.inode))
            resolvedInode = mount->guest();
        else
            resolvedInode = entry.inode;

        if (dir_inode.identifier().is_root_inode() && !is_vfs_root(dir_inode.identifier()) && !strcmp(entry.name, "..")) {
            auto mount = find_mount_for_guest(entry.inode);
            ASSERT(mount);
            resolvedInode = mount->host();
        }
        callback(FS::DirectoryEntry(entry.name, entry.name_length, resolvedInode, entry.fileType));
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

RetainPtr<FileDescriptor> VFS::open(const String& path, int& error, int options, InodeIdentifier base)
{
    auto inode_id = resolve_path(path, base, error, options);
    auto inode = get_inode(inode_id);
    if (!inode)
        return nullptr;
    auto metadata = inode->metadata();
    if (metadata.isCharacterDevice()) {
        auto it = m_character_devices.find(encodedDevice(metadata.majorDevice, metadata.minorDevice));
        if (it == m_character_devices.end()) {
            kprintf("VFS::open: no such character device %u,%u\n", metadata.majorDevice, metadata.minorDevice);
            return nullptr;
        }
        return (*it).value->open(error, options);
    }
    return FileDescriptor::create(move(inode));
}

RetainPtr<FileDescriptor> VFS::create(const String& path, InodeIdentifier base, int& error)
{
    // FIXME: Do the real thing, not just this fake thing!
    (void) path;
    (void) base;
    m_root_inode->fs().create_inode(m_root_inode->fs().root_inode(), "empty", 0100644, 0, error);
    return nullptr;
}

bool VFS::mkdir(const String& path, mode_t mode, InodeIdentifier base, int& error)
{
    error = -EWHYTHO;
    // FIXME: This won't work nicely across mount boundaries.
    FileSystemPath p(path);
    if (!p.is_valid()) {
        error = -EINVAL;
        return false;
    }

    InodeIdentifier parent_dir;
    auto existing_dir = resolve_path(path, base, error, 0, &parent_dir);
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
    auto new_dir = base.fs()->create_directory(parent_dir, p.basename(), mode, error);
    if (new_dir) {
        error = 0;
        return true;
    }
    return false;
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
    return builder.build();
}

InodeIdentifier VFS::resolve_path(const String& path, InodeIdentifier base, int& error, int options, InodeIdentifier* deepest_dir)
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

    if (deepest_dir)
        *deepest_dir = crumb_id;

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
        if (!metadata.isDirectory()) {
#ifdef VFS_DEBUG
            kprintf("parent of <%s> not directory, it's inode %u:%u / %u:%u, mode: %u, size: %u\n", part.characters(), inode.fsid(), inode.index(), metadata.inode.fsid(), metadata.inode.index(), metadata.mode, metadata.size);
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
        kprintf("<%s> %u:%u\n", part.characters(), inode.fsid(), inode.index());
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
        if (metadata.isDirectory()) {
            if (deepest_dir)
                *deepest_dir = crumb_id;
        }
        if (metadata.isSymbolicLink()) {
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
    m_character_devices.set(encodedDevice(device.major(), device.minor()), &device);
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
