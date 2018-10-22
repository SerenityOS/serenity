#include "VirtualFileSystem.h"
#include "FileHandle.h"
#include "FileSystem.h"
#include <AK/kmalloc.h>
#include <AK/kstdio.h>
#include <AK/ktime.h>

//#define VFS_DEBUG

void VirtualFileSystem::initializeGlobals()
{
    FileSystem::initializeGlobals();
}

static dword encodedDevice(unsigned major, unsigned minor)
{
    return (minor & 0xff) | (major << 8) | ((minor & ~0xff) << 12);
}

static VirtualFileSystem* s_the;

VirtualFileSystem& VirtualFileSystem::the()
{
    ASSERT(s_the);
    return *s_the;
}

VirtualFileSystem::VirtualFileSystem()
{
#ifdef VFS_DEBUG
    kprintf("[VFS] Constructing VFS\n");
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
    kprintf("[VFS] ~VirtualFileSystem with %u nodes allocated\n", allocatedNodeCount());
}

auto VirtualFileSystem::makeNode(InodeIdentifier inode) -> RetainPtr<Node>
{
    auto metadata = inode.metadata();
    if (!metadata.isValid())
        return nullptr;

    CharacterDevice* characterDevice = nullptr;
    if (metadata.isCharacterDevice()) {
        auto it = m_characterDevices.find(encodedDevice(metadata.majorDevice, metadata.minorDevice));
        if (it != m_characterDevices.end()) {
            characterDevice = (*it).value;
        } else {
            kprintf("[VFS] makeNode() no such character device %u,%u\n", metadata.majorDevice, metadata.minorDevice);
            return nullptr;
        }
    }

    auto vnode = allocateNode();
    ASSERT(vnode);

    FileSystem* fileSystem = inode.fileSystem();
    fileSystem->retain();

    vnode->inode = inode;

#ifdef VFS_DEBUG
    kprintf("makeNode: inode=%u, size=%u, mode=%o, uid=%u, gid=%u\n", inode.index(), metadata.size, metadata.mode, metadata.uid, metadata.gid);
#endif

    m_inode2vnode.set(inode, vnode.ptr());
    vnode->m_characterDevice = characterDevice;
    return vnode;
}

auto VirtualFileSystem::getOrCreateNode(InodeIdentifier inode) -> RetainPtr<Node>
{
    auto it = m_inode2vnode.find(inode);
    if (it != m_inode2vnode.end())
        return (*it).value;
    return makeNode(inode);
}

bool VirtualFileSystem::mount(RetainPtr<FileSystem>&& fileSystem, const String& path)
{
    ASSERT(fileSystem);

    auto inode = resolvePath(path);
    if (!inode.isValid()) {
        kprintf("[VFS] mount can't resolve mount point '%s'\n", path.characters());
        return false;
    }

    kprintf("mounting %s{%p} at %s (inode: %u)\n", fileSystem->className(), fileSystem.ptr(), path.characters(), inode.index());
    // FIXME: check that this is not already a mount point
    auto mount = make<Mount>(inode, move(fileSystem));
    m_mounts.append(move(mount));
    return true;
}

bool VirtualFileSystem::mountRoot(RetainPtr<FileSystem>&& fileSystem)
{
    if (m_rootNode) {
        kprintf("[VFS] mountRoot can't mount another root\n");
        return false;
    }

    auto mount = make<Mount>(InodeIdentifier(), move(fileSystem));

    auto node = makeNode(mount->guest());
    if (!node->inUse()) {
        kprintf("[VFS] root inode for / is not in use :(\n");
        return false;
    }
    if (!node->inode.metadata().isDirectory()) {
        kprintf("[VFS] root inode for / is not in use :(\n");
        return false;
    }

    m_rootNode = move(node);

    kprintf("[VFS] mountRoot mounted %s{%p}\n",
        m_rootNode->fileSystem()->className(),
        m_rootNode->fileSystem());

    m_mounts.append(move(mount));
    return true;
}

auto VirtualFileSystem::allocateNode() -> RetainPtr<Node>
{
    if (m_nodeFreeList.isEmpty()) {
        kprintf("[VFS] allocateNode has no nodes left\n");
        return nullptr;
    }
    auto* node = m_nodeFreeList.takeLast();
    ASSERT(node->retainCount == 0);
    node->retainCount = 1;
    node->vfs = this;
    return adopt(*node);
}

void VirtualFileSystem::freeNode(Node* node)
{
    ASSERT(node);
    ASSERT(node->inUse());
    m_inode2vnode.remove(node->inode);
    node->inode.fileSystem()->release();
    node->inode = InodeIdentifier();
    node->m_characterDevice = nullptr;
    m_nodeFreeList.append(move(node));
}

bool VirtualFileSystem::isDirectory(const String& path)
{
    auto inode = resolvePath(path);
    if (!inode.isValid())
        return false;

    return inode.metadata().isDirectory();
}

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

template<typename F>
void VirtualFileSystem::enumerateDirectoryInode(InodeIdentifier directoryInode, F func)
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
        func({ entry.name, resolvedInode });
        return true;
    });
}

void VirtualFileSystem::listDirectory(const String& path)
{
    auto directoryInode = resolvePath(path);
    if (!directoryInode.isValid())
        return;

    kprintf("[VFS] ls %s -> %s %02u:%08u\n", path.characters(), directoryInode.fileSystem()->className(), directoryInode.fileSystemID(), directoryInode.index());
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

void VirtualFileSystem::listDirectoryRecursively(const String& path)
{
    auto directory = resolvePath(path);
    if (!directory.isValid())
        return;

    kprintf("%s\n", path.characters());

    enumerateDirectoryInode(directory, [&] (const FileSystem::DirectoryEntry& entry) {
        auto metadata = entry.inode.metadata();
        if (metadata.isDirectory()) {
            if (entry.name != "." && entry.name != "..") {
                char buf[4096];
                ksprintf(buf, "%s/%s", path.characters(), entry.name.characters());
                listDirectoryRecursively(buf);
            }
        } else {
            kprintf("%s/%s\n", path.characters(), entry.name.characters());
        }
    });
}

bool VirtualFileSystem::touch(const String& path)
{
    auto inode = resolvePath(path);
    if (!inode.isValid())
        return false;
    return inode.fileSystem()->setModificationTime(inode, ktime(nullptr));
}

OwnPtr<FileHandle> VirtualFileSystem::open(const String& path)
{
    auto inode = resolvePath(path);
    if (!inode.isValid())
        return nullptr;
    auto vnode = getOrCreateNode(inode);
    if (!vnode)
        return nullptr;
    return make<FileHandle>(move(vnode));
}

OwnPtr<FileHandle> VirtualFileSystem::create(const String& path)
{
    // FIXME: Do the real thing, not just this fake thing!
    (void) path;
    m_rootNode->fileSystem()->createInode(m_rootNode->fileSystem()->rootInode(), "empty", 0100644, 0);
    return nullptr;
}

OwnPtr<FileHandle> VirtualFileSystem::mkdir(const String& path)
{
    // FIXME: Do the real thing, not just this fake thing!
    (void) path;
    m_rootNode->fileSystem()->makeDirectory(m_rootNode->fileSystem()->rootInode(), "mydir", 0400755);
    return nullptr;
}

InodeIdentifier VirtualFileSystem::resolveSymbolicLink(const String& basePath, InodeIdentifier symlinkInode)
{
    auto symlinkContents = symlinkInode.readEntireFile();
    if (!symlinkContents)
        return { };
    char buf[4096];
    ksprintf(buf, "/%s/%s", basePath.characters(), String((const char*)symlinkContents.pointer(), symlinkContents.size()).characters());
    return resolvePath(buf);
}

InodeIdentifier VirtualFileSystem::resolvePath(const String& path)
{
    auto parts = path.split('/');
    InodeIdentifier inode = m_rootNode->inode;

    for (unsigned i = 0; i < parts.size(); ++i) {
        auto& part = parts[i];
        auto metadata = inode.metadata();
        if (!metadata.isValid()) {
#ifdef VFS_DEBUG
            kprintf("invalid metadata\n");
#endif
            return InodeIdentifier();
        }
        if (!metadata.isDirectory()) {
#ifdef VFS_DEBUG
            kprintf("not directory\n");
#endif
            return InodeIdentifier();
        }
        inode = inode.fileSystem()->childOfDirectoryInodeWithName(inode, part);
        if (!inode.isValid()) {
#ifdef VFS_DEBUG
            kprintf("bad child\n");
#endif
            return InodeIdentifier();
        }
#ifdef VFS_DEBUG
        kprintf("<%s> %02u:%08u\n", part.characters(), inode.fileSystemID(), inode.index());
#endif
        if (auto mount = findMountForHost(inode)) {
#ifdef VFS_DEBUG
            kprintf("  -- is host\n");
#endif
            inode = mount->guest();
        }
        if (inode.isRootInode() && !isRoot(inode) && part == "..") {
#ifdef VFS_DEBUG
            kprintf("  -- is guest\n");
#endif
            auto mount = findMountForGuest(inode);
            inode = mount->host();
            inode = inode.fileSystem()->childOfDirectoryInodeWithName(inode, "..");
        }
        metadata = inode.metadata();
        if (metadata.isSymbolicLink()) {
            char buf[4096] = "";
            char* p = buf;
            for (unsigned j = 0; j < i; ++j) {
                p += ksprintf(p, "/%s", parts[j].characters());
            }
            inode = resolveSymbolicLink(buf, inode);
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
    ++retainCount;
}

void VirtualFileSystem::Node::release()
{
    ASSERT(retainCount);
    if (--retainCount == 0) {
        vfs->freeNode(this);
    }
}

VirtualFileSystem::Mount::Mount(InodeIdentifier host, RetainPtr<FileSystem>&& guestFileSystem)
    : m_host(host)
    , m_guest(guestFileSystem->rootInode())
    , m_fileSystem(move(guestFileSystem))
{
}

void VirtualFileSystem::registerCharacterDevice(unsigned major, unsigned minor, CharacterDevice& device)
{
    m_characterDevices.set(encodedDevice(major, minor), &device);
}
