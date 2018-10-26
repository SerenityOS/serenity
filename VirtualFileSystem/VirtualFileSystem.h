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

class CharacterDevice;
class FileHandle;

class VirtualFileSystem {
public:
    static void initializeGlobals();
    static SpinLock& lock();

    struct Node {
        InodeIdentifier inode;
        const InodeMetadata& metadata() const;

        bool inUse() const { return inode.isValid(); }

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

    static VirtualFileSystem& the();

    VirtualFileSystem();
    ~VirtualFileSystem();

    bool isDirectory(const String& path, Node* base = nullptr);
    void listDirectory(const String& path);
    void listDirectoryRecursively(const String& path);

    unsigned maxNodeCount() const { return m_maxNodeCount; }
    unsigned allocatedNodeCount() const { return m_maxNodeCount - m_nodeFreeList.size(); }

    Node* root() { return m_rootNode.ptr(); }
    const Node* root() const { return m_rootNode.ptr(); }

    bool mountRoot(RetainPtr<FileSystem>&&);
    bool mount(RetainPtr<FileSystem>&&, const String& path);

    OwnPtr<FileHandle> open(const String& path, Node* base = nullptr);
    OwnPtr<FileHandle> create(const String& path, Node* base = nullptr);
    OwnPtr<FileHandle> mkdir(const String& path, Node* base = nullptr);

    bool isRoot(InodeIdentifier) const;

    bool touch(const String&path);

    void registerCharacterDevice(unsigned major, unsigned minor, CharacterDevice&);

private:
    friend class FileHandle;

    void enumerateDirectoryInode(InodeIdentifier, Function<bool(const FileSystem::DirectoryEntry&)>);
    String absolutePath(InodeIdentifier);
    InodeIdentifier resolvePath(const String& path, Node* base = nullptr);
    InodeIdentifier resolveSymbolicLink(const String& basePath, InodeIdentifier symlinkInode);

    RetainPtr<Node> allocateNode();
    void freeNode(Node*);

    RetainPtr<Node> makeNode(InodeIdentifier);
    RetainPtr<Node> getOrCreateNode(InodeIdentifier);

    class Mount {
    public:
        Mount(InodeIdentifier host, RetainPtr<FileSystem>&&);

        InodeIdentifier host() const { return m_host; }
        InodeIdentifier guest() const { return m_guest; }

    private:
        InodeIdentifier m_host;
        InodeIdentifier m_guest;
        RetainPtr<FileSystem> m_fileSystem;
    };

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

