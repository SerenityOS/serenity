#pragma once

#include <AK/HashMap.h>
#include <AK/OwnPtr.h>
#include <AK/RetainPtr.h>
#include <AK/String.h>
#include <AK/Vector.h>
#include <AK/Lock.h>
#include <AK/Function.h>
#include "InodeIdentifier.h"
#include "InodeMetadata.h"
#include "Limits.h"
#include "FileSystem.h"

#define O_RDONLY 0
#define O_WRONLY 1
#define O_RDWR 2
#define O_DIRECTORY 00200000
#define O_NOFOLLOW 00400000
#define O_NOFOLLOW_NOERROR 0x4000000

class CharacterDevice;
class FileHandle;

inline constexpr dword encodedDevice(unsigned major, unsigned minor)
{
    return (minor & 0xff) | (major << 8) | ((minor & ~0xff) << 12);
}

class VirtualFileSystem {
public:
    static void initializeGlobals();
    static SpinLock& lock();

    class Mount {
    public:
        Mount(InodeIdentifier host, RetainPtr<FileSystem>&&);

        InodeIdentifier host() const { return m_host; }
        InodeIdentifier guest() const { return m_guest; }

        const FileSystem& fileSystem() const { return *m_fileSystem; }

    private:
        InodeIdentifier m_host;
        InodeIdentifier m_guest;
        RetainPtr<FileSystem> m_fileSystem;
    };

    struct Node {
        InodeIdentifier inode;
        const InodeMetadata& metadata() const;

        bool inUse() const { return inode.isValid() || m_characterDevice; }

        bool isCharacterDevice() const { return m_characterDevice; }
        CharacterDevice* characterDevice() { return m_characterDevice; }

        void retain();
        void release();

        FileSystem* fileSystem() { return inode.fileSystem(); }
        const FileSystem* fileSystem() const { return inode.fileSystem(); }

        VirtualFileSystem* vfs() { return m_vfs; }
        const VirtualFileSystem* vfs() const { return m_vfs; }

    private:
        friend class VirtualFileSystem;
        VirtualFileSystem* m_vfs { nullptr };
        unsigned retainCount { 0 };
        CharacterDevice* m_characterDevice { nullptr };
        mutable InodeMetadata m_cachedMetadata;
    };

    static VirtualFileSystem& the() PURE;

    VirtualFileSystem();
    ~VirtualFileSystem();

    bool isDirectory(const String& path, InodeIdentifier base = InodeIdentifier());
    void listDirectory(const String& path);
    void listDirectoryRecursively(const String& path);

    unsigned maxNodeCount() const { return m_maxNodeCount; }
    unsigned allocatedNodeCount() const { return m_maxNodeCount - m_nodeFreeList.size(); }

    Node* root() { return m_rootNode.ptr(); }
    const Node* root() const { return m_rootNode.ptr(); }

    bool mountRoot(RetainPtr<FileSystem>&&);
    bool mount(RetainPtr<FileSystem>&&, const String& path);

    OwnPtr<FileHandle> open(CharacterDevice&, int options);
    OwnPtr<FileHandle> open(const String& path, int& error, int options = 0, InodeIdentifier base = InodeIdentifier());
    OwnPtr<FileHandle> create(const String& path, InodeIdentifier base = InodeIdentifier());
    OwnPtr<FileHandle> mkdir(const String& path, InodeIdentifier base = InodeIdentifier());

    bool isRoot(InodeIdentifier) const;

    bool touch(const String&path);

    void registerCharacterDevice(CharacterDevice&);

    size_t mountCount() const { return m_mounts.size(); }
    void forEachMount(Function<void(const Mount&)>) const;

    String absolutePath(InodeIdentifier);

private:
    friend class FileHandle;

    void enumerateDirectoryInode(InodeIdentifier, Function<bool(const FileSystem::DirectoryEntry&)>);
    InodeIdentifier resolvePath(const String& path, int& error, InodeIdentifier base = InodeIdentifier(), int options = 0);
    InodeIdentifier resolveSymbolicLink(InodeIdentifier base, InodeIdentifier symlinkInode, int& error);

    RetainPtr<Node> allocateNode();
    void freeNode(Node*);

    RetainPtr<Node> makeNode(InodeIdentifier);
    RetainPtr<Node> makeNode(CharacterDevice&);
    RetainPtr<Node> getOrCreateNode(InodeIdentifier);
    RetainPtr<Node> getOrCreateNode(CharacterDevice&);

    Mount* findMountForHost(InodeIdentifier);
    Mount* findMountForGuest(InodeIdentifier);

    HashMap<InodeIdentifier, Node*> m_inode2vnode;
    HashMap<dword, Node*> m_device2vnode;

    Vector<OwnPtr<Mount>> m_mounts;

    unsigned m_maxNodeCount { 0 };
    Node* m_nodes { nullptr };

    Vector<Node*> m_nodeFreeList;

    RetainPtr<Node> m_rootNode;

    HashMap<dword, CharacterDevice*> m_characterDevices;
};

