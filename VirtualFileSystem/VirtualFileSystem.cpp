#include "VirtualFileSystem.h"
#include "FileHandle.h"
#include "FileSystem.h"
#include <AK/kmalloc.h>
#include <cstdio>
#include <cstdlib>

//#define VFS_DEBUG

VirtualFileSystem::VirtualFileSystem()
{
    m_maxNodeCount = 16;
    m_nodes = reinterpret_cast<Node*>(kmalloc(sizeof(Node) * maxNodeCount()));
    memset(m_nodes, 0, sizeof(Node) * maxNodeCount());

    for (unsigned i = 0; i < m_maxNodeCount; ++i)
        m_nodeFreeList.append(&m_nodes[i]);
}

VirtualFileSystem::~VirtualFileSystem()
{
    printf("[VFS] ~VirtualFileSystem with %u nodes allocated\n", allocatedNodeCount());
}

auto VirtualFileSystem::makeNode(InodeIdentifier inode) -> RetainPtr<Node>
{
    auto metadata = inode.metadata();
    if (!metadata.isValid())
        return nullptr;

    auto vnode = allocateNode();
    ASSERT(vnode);

    FileSystem* fileSystem = inode.fileSystem();
    fileSystem->retain();

    vnode->inode = inode;

#ifdef VFS_DEBUG
    printf("makeNode: inode=%u, size=%u, mode=%o, uid=%u, gid=%u\n", inode.index(), metadata.size, metadata.mode, metadata.uid, metadata.gid);
#endif

    m_inode2vnode.set(inode, vnode.ptr());
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
        printf("[VFS] mount can't resolve mount point '%s'\n", path.characters());
        return false;
    }

    printf("mounting %s{%p} at %s (inode: %u)\n", fileSystem->className(), fileSystem.ptr(), path.characters(), inode.index());
    // FIXME: check that this is not already a mount point
    auto mount = make<Mount>(inode, std::move(fileSystem));
    m_mounts.append(std::move(mount));
    return true;
}

bool VirtualFileSystem::mountRoot(RetainPtr<FileSystem>&& fileSystem)
{
    if (m_rootNode) {
        printf("[VFS] mountRoot can't mount another root\n");
        return false;
    }

    auto mount = make<Mount>(InodeIdentifier(), std::move(fileSystem));

    auto node = makeNode(mount->guest());
    if (!node->inUse()) {
        printf("[VFS] root inode for / is not in use :(\n");
        return false;
    }
    if (!node->inode.metadata().isDirectory()) {
        printf("[VFS] root inode for / is not in use :(\n");
        return false;
    }

    m_rootNode = std::move(node);

    printf("[VFS] mountRoot mounted %s{%p}\n",
        m_rootNode->fileSystem()->className(),
        m_rootNode->fileSystem());

    m_mounts.append(std::move(mount));
    return true;
}

auto VirtualFileSystem::allocateNode() -> RetainPtr<Node>
{
    if (m_nodeFreeList.isEmpty()) {
        printf("[VFS] allocateNode has no nodes left\n");
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
    m_nodeFreeList.append(std::move(node));
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

    printf("[VFS] ls %s -> %s %02u:%08u\n", path.characters(), directoryInode.fileSystem()->className(), directoryInode.fileSystemID(), directoryInode.index());
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
        printf("%02u:%08u ",
            metadata.inode.fileSystemID(),
            metadata.inode.index());

        if (metadata.isDirectory())
            printf("d");
        else if (metadata.isSymbolicLink())
            printf("l");
        else if (metadata.isBlockDevice())
            printf("b");
        else if (metadata.isCharacterDevice())
            printf("c");
        else if (metadata.isSocket())
            printf("s");
        else if (metadata.isFIFO())
            printf("f");
        else if (metadata.isRegularFile())
            printf("-");
        else
            printf("?");

        printf("%c%c%c%c%c%c%c%c",
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
            printf("t");
        else
            printf("%c", metadata.mode & 00001 ? 'x' : '-');

        if (metadata.isCharacterDevice() || metadata.isBlockDevice()) {
            char buf[16];
            sprintf(buf, "%u, %u", metadata.majorDevice, metadata.minorDevice);
            printf("%12s ", buf);
        } else {
            printf("%12u ", metadata.size);
        }

        printf("\033[30;1m");
        auto tm = *localtime(&metadata.mtime);
        printf("%04u-%02u-%02u %02u:%02u:%02u ",
                tm.tm_year + 1900,
                tm.tm_mon + 1,
                tm.tm_mday,
                tm.tm_hour,
                tm.tm_min,
                tm.tm_sec);
        printf("\033[0m");

        printf("%s%s%s",
            nameColorBegin,
            entry.name.characters(),
            nameColorEnd);

        if (metadata.isDirectory()) {
            printf("/");
        } else if (metadata.isSymbolicLink()) {
            auto symlinkContents = directoryInode.fileSystem()->readInode(metadata.inode);
            printf(" -> %s", String((const char*)symlinkContents.pointer(), symlinkContents.size()).characters());
        }
        printf("\n");
        return true;
    });
}

void VirtualFileSystem::listDirectoryRecursively(const String& path)
{
    auto directory = resolvePath(path);
    if (!directory.isValid())
        return;

    printf("%s\n", path.characters());

    enumerateDirectoryInode(directory, [&] (const FileSystem::DirectoryEntry& entry) {
        auto metadata = entry.inode.metadata();
        if (metadata.isDirectory()) {
            if (entry.name != "." && entry.name != "..") {
                char buf[4096];
                sprintf(buf, "%s/%s", path.characters(), entry.name.characters());
                listDirectoryRecursively(buf);
            }
        } else {
            printf("%s/%s\n", path.characters(), entry.name.characters());
        }
    });
}

bool VirtualFileSystem::touch(const String& path)
{
    auto inode = resolvePath(path);
    if (!inode.isValid())
        return false;
    return inode.fileSystem()->setModificationTime(inode, time(nullptr));
}

OwnPtr<FileHandle> VirtualFileSystem::open(const String& path)
{
    auto inode = resolvePath(path);
    if (!inode.isValid())
        return nullptr;
    auto vnode = getOrCreateNode(inode);
    if (!vnode)
        return nullptr;
    return make<FileHandle>(std::move(vnode));
}

OwnPtr<FileHandle> VirtualFileSystem::create(const String& path)
{
    // FIXME: Do the real thing, not just this fake thing!
    m_rootNode->fileSystem()->createInode(m_rootNode->fileSystem()->rootInode(), "empty", 0100644);
    return nullptr;
}

InodeIdentifier VirtualFileSystem::resolveSymbolicLink(const String& basePath, InodeIdentifier symlinkInode)
{
    auto symlinkContents = symlinkInode.readEntireFile();
    if (!symlinkContents)
        return { };
    char buf[4096];
    sprintf(buf, "/%s/%s", basePath.characters(), String((const char*)symlinkContents.pointer(), symlinkContents.size()).characters());
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
            printf("invalid metadata\n");
#endif
            return InodeIdentifier();
        }
        if (!metadata.isDirectory()) {
#ifdef VFS_DEBUG
            printf("not directory\n");
#endif
            return InodeIdentifier();
        }
        inode = inode.fileSystem()->childOfDirectoryInodeWithName(inode, part);
        if (!inode.isValid()) {
#ifdef VFS_DEBUG
            printf("bad child\n");
#endif
            return InodeIdentifier();
        }
#ifdef VFS_DEBUG
        printf("<%s> %02u:%08u\n", part.characters(), inode.fileSystemID(), inode.index());
#endif
        if (auto mount = findMountForHost(inode)) {
#ifdef VFS_DEBUG
            printf("  -- is host\n");
#endif
            inode = mount->guest();
        }
        if (inode.isRootInode() && !isRoot(inode) && part == "..") {
#ifdef VFS_DEBUG
            printf("  -- is guest\n");
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
                p += sprintf(p, "/%s", parts[j].characters());
            }
            inode = resolveSymbolicLink(buf, inode);
            if (!inode.isValid()) {
                printf("Symbolic link resolution failed :(\n");
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
    , m_fileSystem(std::move(guestFileSystem))
{
}

