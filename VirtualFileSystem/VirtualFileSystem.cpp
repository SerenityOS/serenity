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
    FS::initializeGlobals();
}

VFS::VFS()
{
#ifdef VFS_DEBUG
    kprintf("VFS: Constructing VFS\n");
#endif
    s_the = this;
    m_max_vnode_count = 16;
    m_nodes = reinterpret_cast<Vnode*>(kmalloc(sizeof(Vnode) * max_vnode_count()));
    memset(m_nodes, 0, sizeof(Vnode) * max_vnode_count());

    for (unsigned i = 0; i < m_max_vnode_count; ++i)
        m_vnode_freelist.append(&m_nodes[i]);
}

VFS::~VFS()
{
    kprintf("VFS: ~VirtualFileSystem with %u nodes allocated\n", allocated_vnode_count());
    // FIXME: m_nodes is never freed. Does it matter though?
}

auto VFS::makeNode(InodeIdentifier inode) -> RetainPtr<Vnode>
{
    auto metadata = inode.metadata();
    if (!metadata.isValid())
        return nullptr;

    auto core_inode = inode.fileSystem()->get_inode(inode);
    if (core_inode)
        core_inode->m_metadata = metadata;

    InterruptDisabler disabler;

    CharacterDevice* characterDevice = nullptr;
    if (metadata.isCharacterDevice()) {
        auto it = m_character_devices.find(encodedDevice(metadata.majorDevice, metadata.minorDevice));
        if (it != m_character_devices.end()) {
            characterDevice = (*it).value;
        } else {
            kprintf("VFS: makeNode() no such character device %u,%u\n", metadata.majorDevice, metadata.minorDevice);
            return nullptr;
        }
    }

    auto vnode = allocateNode();
    ASSERT(vnode);

    FS* fileSystem = inode.fileSystem();
    fileSystem->retain();

    vnode->inode = inode;
    vnode->m_core_inode = move(core_inode);
    vnode->m_cachedMetadata = metadata;

#ifdef VFS_DEBUG
    kprintf("makeNode: inode=%u, size=%u, mode=%o, uid=%u, gid=%u\n", inode.index(), metadata.size, metadata.mode, metadata.uid, metadata.gid);
#endif

    m_inode2vnode.set(inode, vnode.ptr());
    vnode->m_characterDevice = characterDevice;

    return vnode;
}

auto VFS::makeNode(CharacterDevice& device) -> RetainPtr<Vnode>
{
    InterruptDisabler disabler;
    auto vnode = allocateNode();
    ASSERT(vnode);

#ifdef VFS_DEBUG
    kprintf("makeNode: device=%p (%u,%u)\n", &device, device.major(), device.minor());
#endif

    m_device2vnode.set(encodedDevice(device.major(), device.minor()), vnode.ptr());
    vnode->m_characterDevice = &device;

    return vnode;
}

auto VFS::get_or_create_node(InodeIdentifier inode) -> RetainPtr<Vnode>
{
    {
        InterruptDisabler disabler;
        auto it = m_inode2vnode.find(inode);
        if (it != m_inode2vnode.end())
            return (*it).value;
    }
    return makeNode(inode);
}

auto VFS::get_or_create_node(CharacterDevice& device) -> RetainPtr<Vnode>
{
    {
        InterruptDisabler disabler;
        auto it = m_device2vnode.find(encodedDevice(device.major(), device.minor()));
        if (it != m_device2vnode.end())
            return (*it).value;
    }
    return makeNode(device);
}

InodeIdentifier VFS::root_inode_id() const
{
    return m_root_vnode->inode;
}

bool VFS::mount(RetainPtr<FS>&& fileSystem, const String& path)
{
    ASSERT(fileSystem);
    int error;
    auto inode = resolve_path(path, root_inode_id(), error);
    if (!inode.isValid()) {
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
    if (m_root_vnode) {
        kprintf("VFS: mount_root can't mount another root\n");
        return false;
    }

    auto mount = make<Mount>(InodeIdentifier(), move(fileSystem));

    auto node = makeNode(mount->guest());
    if (!node->inUse()) {
        kprintf("VFS: root inode for / is not in use :(\n");
        return false;
    }
    if (!node->metadata().isDirectory()) {
        kprintf("VFS: root inode for / is not a directory :(\n");
        return false;
    }

    m_root_vnode = move(node);

    kprintf("VFS: mounted root on %s{%p}\n",
        m_root_vnode->fileSystem()->class_name(),
        m_root_vnode->fileSystem());

    m_mounts.append(move(mount));
    return true;
}

auto VFS::allocateNode() -> RetainPtr<Vnode>
{
    if (m_vnode_freelist.isEmpty()) {
        kprintf("VFS: allocateNode has no nodes left\n");
        return nullptr;
    }
    auto* node = m_vnode_freelist.takeLast();
    ASSERT(node->retainCount == 0);
    node->retainCount = 1;
    node->m_vfs = this;
    node->m_vmo = nullptr;
    return adopt(*node);
}

void VFS::freeNode(Vnode* node)
{
    InterruptDisabler disabler;
    ASSERT(node);
    ASSERT(node->inUse());
    if (node->inode.isValid()) {
        m_inode2vnode.remove(node->inode);
        node->inode.fileSystem()->release();
        node->inode = InodeIdentifier();
    }
    if (node->m_characterDevice) {
        m_device2vnode.remove(encodedDevice(node->m_characterDevice->major(), node->m_characterDevice->minor()));
        node->m_characterDevice = nullptr;
    }
    node->m_vfs = nullptr;
    node->m_vmo = nullptr;
    m_vnode_freelist.append(move(node));
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
    return inode == m_root_vnode->inode;
}

void VFS::traverse_directory_inode(CoreInode& dir_inode, Function<bool(const FS::DirectoryEntry&)> callback)
{
    dir_inode.traverse_as_directory([&] (const FS::DirectoryEntry& entry) {
        InodeIdentifier resolvedInode;
        if (auto mount = find_mount_for_host(entry.inode))
            resolvedInode = mount->guest();
        else
            resolvedInode = entry.inode;

        if (dir_inode.identifier().isRootInode() && !is_vfs_root(dir_inode.identifier()) && !strcmp(entry.name, "..")) {
            auto mount = find_mount_for_guest(entry.inode);
            ASSERT(mount);
            resolvedInode = mount->host();
        }
        callback(FS::DirectoryEntry(entry.name, entry.name_length, resolvedInode, entry.fileType));
        return true;
    });
}

bool VFS::touch(const String& path)
{
    int error;
    auto inode = resolve_path(path, root_inode_id(), error);
    if (!inode.isValid())
        return false;
    return inode.fileSystem()->set_mtime(inode, ktime(nullptr));
}

RetainPtr<FileDescriptor> VFS::open(CharacterDevice& device, int options)
{
    // FIXME: Respect options.
    (void) options;
    auto vnode = get_or_create_node(device);
    if (!vnode)
        return nullptr;
    return FileDescriptor::create(move(vnode));
}

RetainPtr<FileDescriptor> VFS::open(const String& path, int& error, int options, InodeIdentifier base)
{
    auto inode = resolve_path(path, base, error, options);
    if (!inode.isValid())
        return nullptr;
    auto vnode = get_or_create_node(inode);
    if (!vnode)
        return nullptr;
    return FileDescriptor::create(move(vnode));
}

RetainPtr<FileDescriptor> VFS::create(const String& path, InodeIdentifier base, int& error)
{
    // FIXME: Do the real thing, not just this fake thing!
    (void) path;
    (void) base;
    m_root_vnode->fileSystem()->create_inode(m_root_vnode->fileSystem()->rootInode(), "empty", 0100644, 0, error);
    return nullptr;
}

bool VFS::mkdir(const String& path, mode_t mode, InodeIdentifier base, int& error)
{
    error = EWHYTHO;
    // FIXME: This won't work nicely across mount boundaries.
    FileSystemPath p(path);
    if (!p.isValid()) {
        error = -EINVAL;
        return false;
    }

    InodeIdentifier parent_dir;
    auto existing_dir = resolve_path(path, base, error, 0, &parent_dir);
    if (existing_dir.isValid()) {
        error = -EEXIST;
        return false;
    }
    if (!parent_dir.isValid()) {
        error = -ENOENT;
        return false;
    }
    if (error != -ENOENT) {
        return false;
    }
    dbgprintf("VFS::mkdir: '%s' in %u:%u\n", p.basename().characters(), parent_dir.fsid(), parent_dir.index());
    auto new_dir = base.fileSystem()->create_directory(parent_dir, p.basename(), mode, error);
    if (new_dir.isValid()) {
        error = 0;
        return true;
    }
    return false;
}

InodeIdentifier VFS::resolveSymbolicLink(InodeIdentifier base, InodeIdentifier symlinkInode, int& error)
{
    auto symlinkContents = symlinkInode.readEntireFile();
    if (!symlinkContents)
        return { };
    auto linkee = String((const char*)symlinkContents.pointer(), symlinkContents.size());
#ifdef VFS_DEBUG
    kprintf("linkee (%s)(%u) from %u:%u\n", linkee.characters(), linkee.length(), base.fsid(), base.index());
#endif
    return resolve_path(linkee, base, error);
}

RetainPtr<CoreInode> VFS::get_inode(InodeIdentifier inode_id)
{
    if (!inode_id.isValid())
        return nullptr;
    return inode_id.fileSystem()->get_inode(inode_id);
}

String VFS::absolute_path(CoreInode& core_inode)
{
    int error;
    Vector<InodeIdentifier> lineage;
    RetainPtr<CoreInode> inode = &core_inode;
    while (inode->identifier() != m_root_vnode->inode) {
        if (auto* mount = find_mount_for_guest(inode->identifier()))
            lineage.append(mount->host());
        else
            lineage.append(inode->identifier());

        InodeIdentifier parent_id;
        if (inode->is_directory()) {
            parent_id = resolve_path("..", inode->identifier(), error);
        } else {
            parent_id = inode->fs().find_parent_of_inode(inode->identifier());
        }
        ASSERT(parent_id.isValid());
        inode = get_inode(parent_id);
    }
    if (lineage.isEmpty())
        return "/";
    lineage.append(m_root_vnode->inode);
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
    if (path.isEmpty()) {
        error = -EINVAL;
        return { };
    }

    auto parts = path.split('/');
    InodeIdentifier crumb_id;

    if (path[0] == '/')
        crumb_id = m_root_vnode->inode;
    else
        crumb_id = base.isValid() ? base : m_root_vnode->inode;

    if (deepest_dir)
        *deepest_dir = crumb_id;

    for (unsigned i = 0; i < parts.size(); ++i) {
        bool inode_was_root_at_head_of_loop = crumb_id.isRootInode();
        auto& part = parts[i];
        if (part.isEmpty())
            break;
        auto metadata = crumb_id.metadata();
        if (!metadata.isValid()) {
#ifdef VFS_DEBUG
            kprintf("invalid metadata\n");
#endif
            error = -EIO;
            return { };
        }
        if (!metadata.isDirectory()) {
#ifdef VFS_DEBUG
            kprintf("parent of <%s> not directory, it's inode %u:%u / %u:%u, mode: %u, size: %u\n", part.characters(), inode.fsid(), inode.index(), metadata.inode.fsid(), metadata.inode.index(), metadata.mode, metadata.size);
#endif
            error = -ENOTDIR;
            return { };
        }
        auto parent = crumb_id;
        auto dir_inode = get_inode(crumb_id);
        crumb_id = dir_inode->lookup(part);
        if (!crumb_id.isValid()) {
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
        if (inode_was_root_at_head_of_loop && crumb_id.isRootInode() && !is_vfs_root(crumb_id) && part == "..") {
#ifdef VFS_DEBUG
            kprintf("  -- is guest\n");
#endif
            auto mount = find_mount_for_guest(crumb_id);
            auto dir_inode = get_inode(mount->host());
            crumb_id = dir_inode->lookup("..");
        }
        metadata = crumb_id.metadata();
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
            crumb_id = resolveSymbolicLink(parent, crumb_id, error);
            if (!crumb_id.isValid()) {
                kprintf("Symbolic link resolution failed :(\n");
                return { };
            }
        }
    }

    return crumb_id;
}

void Vnode::retain()
{
    InterruptDisabler disabler; // FIXME: Make a Retainable with atomic retain count instead.
    ++retainCount;
}

void Vnode::release()
{
    InterruptDisabler disabler; // FIXME: Make a Retainable with atomic retain count instead.
    ASSERT(retainCount);
    if (--retainCount == 0) {
        m_vfs->freeNode(this);
    }
}

const InodeMetadata& Vnode::metadata() const
{
    if (m_core_inode)
        return m_core_inode->metadata();
    if (!m_cachedMetadata.isValid())
        m_cachedMetadata = inode.metadata();
    return m_cachedMetadata;
}

VFS::Mount::Mount(InodeIdentifier host, RetainPtr<FS>&& guest_fs)
    : m_host(host)
    , m_guest(guest_fs->rootInode())
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
