#include "VirtualFileSystem.h"
#include "FileDescriptor.h"
#include "FileSystem.h"
#include <AK/StringBuilder.h>
#include <AK/kmalloc.h>
#include <AK/kstdio.h>
#include <AK/ktime.h>
#include "CharacterDevice.h"
#include "sys-errno.h"

//#define VFS_DEBUG

static VirtualFileSystem* s_the;

#ifndef SERENITY
typedef int InterruptDisabler;
#endif

VirtualFileSystem& VirtualFileSystem::the()
{
    ASSERT(s_the);
    return *s_the;
}

void VirtualFileSystem::initializeGlobals()
{
    s_the = nullptr;
    FileSystem::initializeGlobals();
}

VirtualFileSystem::VirtualFileSystem()
{
#ifdef VFS_DEBUG
    kprintf("VFS: Constructing VFS\n");
#endif
    s_the = this;
    m_maxNodeCount = 16;
    m_nodes = reinterpret_cast<Node*>(kmalloc(sizeof(Node) * maxNodeCount()));
    memset(m_nodes, 0, sizeof(Node) * maxNodeCount());

    for (unsigned i = 0; i < m_maxNodeCount; ++i)
        m_nodeFreeList.append(&m_nodes[i]);
}

VirtualFileSystem::~VirtualFileSystem()
{
    kprintf("VFS: ~VirtualFileSystem with %u nodes allocated\n", allocatedNodeCount());
    // FIXME: m_nodes is never freed. Does it matter though?
}

auto VirtualFileSystem::makeNode(InodeIdentifier inode) -> RetainPtr<Node>
{
    auto metadata = inode.metadata();
    if (!metadata.isValid())
        return nullptr;

    InterruptDisabler disabler;

    CharacterDevice* characterDevice = nullptr;
    if (metadata.isCharacterDevice()) {
        auto it = m_characterDevices.find(encodedDevice(metadata.majorDevice, metadata.minorDevice));
        if (it != m_characterDevices.end()) {
            characterDevice = (*it).value;
        } else {
            kprintf("VFS: makeNode() no such character device %u,%u\n", metadata.majorDevice, metadata.minorDevice);
            return nullptr;
        }
    }

    auto vnode = allocateNode();
    ASSERT(vnode);

    FileSystem* fileSystem = inode.fileSystem();
    fileSystem->retain();

    vnode->inode = inode;
    vnode->m_cachedMetadata = { };

#ifdef VFS_DEBUG
    kprintf("makeNode: inode=%u, size=%u, mode=%o, uid=%u, gid=%u\n", inode.index(), metadata.size, metadata.mode, metadata.uid, metadata.gid);
#endif

    m_inode2vnode.set(inode, vnode.ptr());
    vnode->m_characterDevice = characterDevice;

    return vnode;
}

auto VirtualFileSystem::makeNode(CharacterDevice& device) -> RetainPtr<Node>
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

auto VirtualFileSystem::getOrCreateNode(InodeIdentifier inode) -> RetainPtr<Node>
{
    {
        InterruptDisabler disabler;
        auto it = m_inode2vnode.find(inode);
        if (it != m_inode2vnode.end())
            return (*it).value;
    }
    return makeNode(inode);
}

auto VirtualFileSystem::getOrCreateNode(CharacterDevice& device) -> RetainPtr<Node>
{
    {
        InterruptDisabler disabler;
        auto it = m_device2vnode.find(encodedDevice(device.major(), device.minor()));
        if (it != m_device2vnode.end())
            return (*it).value;
    }
    return makeNode(device);
}

bool VirtualFileSystem::mount(RetainPtr<FileSystem>&& fileSystem, const String& path)
{
    ASSERT(fileSystem);
    int error;
    auto inode = resolvePath(path, error);
    if (!inode.isValid()) {
        kprintf("VFS: mount can't resolve mount point '%s'\n", path.characters());
        return false;
    }

    kprintf("VFS: mounting %s{%p} at %s (inode: %u)\n", fileSystem->className(), fileSystem.ptr(), path.characters(), inode.index());
    // FIXME: check that this is not already a mount point
    auto mount = make<Mount>(inode, move(fileSystem));
    m_mounts.append(move(mount));
    return true;
}

bool VirtualFileSystem::mountRoot(RetainPtr<FileSystem>&& fileSystem)
{
    if (m_rootNode) {
        kprintf("VFS: mountRoot can't mount another root\n");
        return false;
    }

    auto mount = make<Mount>(InodeIdentifier(), move(fileSystem));

    auto node = makeNode(mount->guest());
    if (!node->inUse()) {
        kprintf("VFS: root inode for / is not in use :(\n");
        return false;
    }
    if (!node->inode.metadata().isDirectory()) {
        kprintf("VFS: root inode for / is not a directory :(\n");
        return false;
    }

    m_rootNode = move(node);

    kprintf("VFS: mounted root on %s{%p}\n",
        m_rootNode->fileSystem()->className(),
        m_rootNode->fileSystem());

    m_mounts.append(move(mount));
    return true;
}

auto VirtualFileSystem::allocateNode() -> RetainPtr<Node>
{
    if (m_nodeFreeList.isEmpty()) {
        kprintf("VFS: allocateNode has no nodes left\n");
        return nullptr;
    }
    auto* node = m_nodeFreeList.takeLast();
    ASSERT(node->retainCount == 0);
    node->retainCount = 1;
    node->m_vfs = this;
    return adopt(*node);
}

void VirtualFileSystem::freeNode(Node* node)
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
    m_nodeFreeList.append(move(node));
}

#ifndef SERENITY
bool VirtualFileSystem::isDirectory(const String& path, InodeIdentifier base)
{
    int error;
    auto inode = resolvePath(path, error, base);
    if (!inode.isValid())
        return false;

    return inode.metadata().isDirectory();
}
#endif

auto VirtualFileSystem::findMountForHost(InodeIdentifier inode) -> Mount*
{
    for (auto& mount : m_mounts) {
        if (mount->host() == inode)
            return mount.ptr();
    }
    return nullptr;
}

auto VirtualFileSystem::findMountForGuest(InodeIdentifier inode) -> Mount*
{
    for (auto& mount : m_mounts) {
        if (mount->guest() == inode)
            return mount.ptr();
    }
    return nullptr;
}

bool VirtualFileSystem::isRoot(InodeIdentifier inode) const
{
    return inode == m_rootNode->inode;
}

void VirtualFileSystem::enumerateDirectoryInode(InodeIdentifier directoryInode, Function<bool(const FileSystem::DirectoryEntry&)> callback)
{
    if (!directoryInode.isValid())
        return;

    directoryInode.fileSystem()->enumerateDirectoryInode(directoryInode, [&] (const FileSystem::DirectoryEntry& entry) {
        InodeIdentifier resolvedInode;
        if (auto mount = findMountForHost(entry.inode))
            resolvedInode = mount->guest();
        else
            resolvedInode = entry.inode;

        if (directoryInode.isRootInode() && !isRoot(directoryInode) && entry.name == "..") {
            auto mount = findMountForGuest(entry.inode);
            ASSERT(mount);
            resolvedInode = mount->host();
        }
        callback({ entry.name, resolvedInode });
        return true;
    });
}

#ifndef SERENITY
void VirtualFileSystem::listDirectory(const String& path, InodeIdentifier base)
{
    int error;
    auto directoryInode = resolvePath(path, error, base);
    if (!directoryInode.isValid())
        return;

    kprintf("VFS: ls %s -> %s %02u:%08u\n", path.characters(), directoryInode.fileSystem()->className(), directoryInode.fileSystemID(), directoryInode.index());
    enumerateDirectoryInode(directoryInode, [&] (const FileSystem::DirectoryEntry& entry) {
        const char* nameColorBegin = "";
        const char* nameColorEnd = "";
        auto metadata = entry.inode.metadata();
        ASSERT(metadata.isValid());
        if (metadata.isDirectory()) {
            nameColorBegin = "\033[34;1m";
            nameColorEnd = "\033[0m";
        } else if (metadata.isSymbolicLink()) {
            nameColorBegin = "\033[36;1m";
            nameColorEnd = "\033[0m";
        }
        if (metadata.isSticky()) {
            nameColorBegin = "\033[42;30m";
            nameColorEnd = "\033[0m";
        }
        if (metadata.isCharacterDevice() || metadata.isBlockDevice()) {
            nameColorBegin = "\033[33;1m";
            nameColorEnd = "\033[0m";
        }
        kprintf("%02u:%08u ",
            metadata.inode.fileSystemID(),
            metadata.inode.index());

        if (metadata.isDirectory())
            kprintf("d");
        else if (metadata.isSymbolicLink())
            kprintf("l");
        else if (metadata.isBlockDevice())
            kprintf("b");
        else if (metadata.isCharacterDevice())
            kprintf("c");
        else if (metadata.isSocket())
            kprintf("s");
        else if (metadata.isFIFO())
            kprintf("f");
        else if (metadata.isRegularFile())
            kprintf("-");
        else
            kprintf("?");

        kprintf("%c%c%c%c%c%c%c%c",
            metadata.mode & 00400 ? 'r' : '-',
            metadata.mode & 00200 ? 'w' : '-',
            metadata.mode & 00100 ? 'x' : '-',
            metadata.mode & 00040 ? 'r' : '-',
            metadata.mode & 00020 ? 'w' : '-',
            metadata.mode & 00010 ? 'x' : '-',
            metadata.mode & 00004 ? 'r' : '-',
            metadata.mode & 00002 ? 'w' : '-'
        );

        if (metadata.isSticky())
            kprintf("t");
        else
            kprintf("%c", metadata.mode & 00001 ? 'x' : '-');

        if (metadata.isCharacterDevice() || metadata.isBlockDevice()) {
            char buf[16];
            ksprintf(buf, "%u, %u", metadata.majorDevice, metadata.minorDevice);
            kprintf("%12s ", buf);
        } else {
            kprintf("%12lld ", metadata.size);
        }

        kprintf("\033[30;1m");
        time_t mtime = metadata.mtime;
        auto tm = *klocaltime(&mtime);
        kprintf("%04u-%02u-%02u %02u:%02u:%02u ",
                tm.tm_year + 1900,
                tm.tm_mon + 1,
                tm.tm_mday,
                tm.tm_hour,
                tm.tm_min,
                tm.tm_sec);
        kprintf("\033[0m");

        kprintf("%s%s%s",
            nameColorBegin,
            entry.name.characters(),
            nameColorEnd);

        if (metadata.isDirectory()) {
            kprintf("/");
        } else if (metadata.isSymbolicLink()) {
            auto symlinkContents = directoryInode.fileSystem()->readEntireInode(metadata.inode);
            kprintf(" -> %s", String((const char*)symlinkContents.pointer(), symlinkContents.size()).characters());
        }
        kprintf("\n");
        return true;
    });
}

void VirtualFileSystem::listDirectoryRecursively(const String& path, InodeIdentifier base)
{
    int error;
    auto directory = resolvePath(path, error, base);
    if (!directory.isValid())
        return;

    kprintf("%s\n", path.characters());

    enumerateDirectoryInode(directory, [&] (const FileSystem::DirectoryEntry& entry) {
        auto metadata = entry.inode.metadata();
        if (metadata.isDirectory()) {
            if (entry.name != "." && entry.name != "..") {
                char buf[4096];
                ksprintf(buf, "%s/%s", path.characters(), entry.name.characters());
                listDirectoryRecursively(buf, base);
            }
        } else {
            kprintf("%s/%s\n", path.characters(), entry.name.characters());
        }
        return true;
    });
}
#endif

bool VirtualFileSystem::touch(const String& path)
{
    int error;
    auto inode = resolvePath(path, error);
    if (!inode.isValid())
        return false;
    return inode.fileSystem()->setModificationTime(inode, ktime(nullptr));
}

RetainPtr<FileDescriptor> VirtualFileSystem::open(CharacterDevice& device, int options)
{
    auto vnode = getOrCreateNode(device);
    if (!vnode)
        return nullptr;
    return FileDescriptor::create(move(vnode));
}

RetainPtr<FileDescriptor> VirtualFileSystem::open(const String& path, int& error, int options, InodeIdentifier base)
{
    auto inode = resolvePath(path, error, base, options);
    if (!inode.isValid())
        return nullptr;
    auto vnode = getOrCreateNode(inode);
    if (!vnode)
        return nullptr;
    return FileDescriptor::create(move(vnode));
}

RetainPtr<FileDescriptor> VirtualFileSystem::create(const String& path, InodeIdentifier base)
{
    // FIXME: Do the real thing, not just this fake thing!
    (void) path;
    m_rootNode->fileSystem()->createInode(m_rootNode->fileSystem()->rootInode(), "empty", 0100644, 0);
    return nullptr;
}

RetainPtr<FileDescriptor> VirtualFileSystem::mkdir(const String& path, InodeIdentifier base)
{
    // FIXME: Do the real thing, not just this fake thing!
    (void) path;
    m_rootNode->fileSystem()->makeDirectory(m_rootNode->fileSystem()->rootInode(), "mydir", 0400755);
    return nullptr;
}

InodeIdentifier VirtualFileSystem::resolveSymbolicLink(InodeIdentifier base, InodeIdentifier symlinkInode, int& error)
{
    auto symlinkContents = symlinkInode.readEntireFile();
    if (!symlinkContents)
        return { };
    auto linkee = String((const char*)symlinkContents.pointer(), symlinkContents.size());
#ifdef VFS_DEBUG
    kprintf("linkee (%s)(%u) from %u:%u\n", linkee.characters(), linkee.length(), base.fileSystemID(), base.index());
#endif
    return resolvePath(linkee, error, base);
}

String VirtualFileSystem::absolutePath(InodeIdentifier inode)
{
    if (!inode.isValid())
        return String();

    int error;
    Vector<InodeIdentifier> lineage;
    while (inode != m_rootNode->inode) {
        if (auto* mount = findMountForGuest(inode))
            lineage.append(mount->host());
        else
            lineage.append(inode);
        if (inode.metadata().isDirectory()) {
            inode = resolvePath("..", error, inode);
        } else
            inode = inode.fileSystem()->findParentOfInode(inode);
        ASSERT(inode.isValid());
    }
    if (lineage.isEmpty())
        return "/";
    lineage.append(m_rootNode->inode);
    StringBuilder builder;
    for (size_t i = lineage.size() - 1; i >= 1; --i) {
        auto& child = lineage[i - 1];
        auto parent = lineage[i];
        if (auto* mount = findMountForHost(parent))
            parent = mount->guest();
        builder.append('/');
        builder.append(parent.fileSystem()->nameOfChildInDirectory(parent, child));
    }
    return builder.build();
}

InodeIdentifier VirtualFileSystem::resolvePath(const String& path, int& error, InodeIdentifier base, int options)
{
    if (path.isEmpty())
        return { };

    auto parts = path.split('/');
    InodeIdentifier inode;

    if (path[0] == '/')
        inode = m_rootNode->inode;
    else
        inode = base.isValid() ? base : m_rootNode->inode;

    for (unsigned i = 0; i < parts.size(); ++i) {
        bool wasRootInodeAtHeadOfLoop = inode.isRootInode();
        auto& part = parts[i];
        auto metadata = inode.metadata();
        if (!metadata.isValid()) {
#ifdef VFS_DEBUG
            kprintf("invalid metadata\n");
#endif
            error = -EIO;
            return { };
        }
        if (!metadata.isDirectory()) {
#ifdef VFS_DEBUG
            kprintf("parent of <%s> not directory, it's inode %u:%u / %u:%u, mode: %u, size: %u\n", part.characters(), inode.fileSystemID(), inode.index(), metadata.inode.fileSystemID(), metadata.inode.index(), metadata.mode, metadata.size);
#endif
            error = -EIO;
            return { };
        }
        auto parent = inode;
        inode = inode.fileSystem()->childOfDirectoryInodeWithName(inode, part);
        if (!inode.isValid()) {
#ifdef VFS_DEBUG
            kprintf("child <%s>(%u) not found in directory, %02u:%08u\n", part.characters(), part.length(), parent.fileSystemID(), parent.index());
#endif
            error = -ENOENT;
            return { };
        }
#ifdef VFS_DEBUG
        kprintf("<%s> %u:%u\n", part.characters(), inode.fileSystemID(), inode.index());
#endif
        if (auto mount = findMountForHost(inode)) {
#ifdef VFS_DEBUG
            kprintf("  -- is host\n");
#endif
            inode = mount->guest();
        }
        if (wasRootInodeAtHeadOfLoop && inode.isRootInode() && !isRoot(inode) && part == "..") {
#ifdef VFS_DEBUG
            kprintf("  -- is guest\n");
#endif
            auto mount = findMountForGuest(inode);
            inode = mount->host();
            inode = inode.fileSystem()->childOfDirectoryInodeWithName(inode, "..");
        }
        metadata = inode.metadata();
        if (metadata.isSymbolicLink()) {
            if (i == parts.size() - 1) {
                if (options & O_NOFOLLOW) {
                    error = -ELOOP;
                    return { };
                }
                if (options & O_NOFOLLOW_NOERROR)
                    return inode;
            }
            inode = resolveSymbolicLink(parent, inode, error);
            if (!inode.isValid()) {
                kprintf("Symbolic link resolution failed :(\n");
                return { };
            }
        }
    }

    return inode;
}

void VirtualFileSystem::Node::retain()
{
    InterruptDisabler disabler; // FIXME: Make a Retainable with atomic retain count instead.
    ++retainCount;
}

void VirtualFileSystem::Node::release()
{
    InterruptDisabler disabler; // FIXME: Make a Retainable with atomic retain count instead.
    ASSERT(retainCount);
    if (--retainCount == 0) {
        m_vfs->freeNode(this);
    }
}

const InodeMetadata& VirtualFileSystem::Node::metadata() const
{
    if (!m_cachedMetadata.isValid())
        m_cachedMetadata = inode.metadata();
    return m_cachedMetadata;
}

VirtualFileSystem::Mount::Mount(InodeIdentifier host, RetainPtr<FileSystem>&& guestFileSystem)
    : m_host(host)
    , m_guest(guestFileSystem->rootInode())
    , m_fileSystem(move(guestFileSystem))
{
}

void VirtualFileSystem::registerCharacterDevice(CharacterDevice& device)
{
    m_characterDevices.set(encodedDevice(device.major(), device.minor()), &device);
}

void VirtualFileSystem::forEachMount(Function<void(const Mount&)> callback) const
{
    for (auto& mount : m_mounts) {
        callback(*mount);
    }
}
