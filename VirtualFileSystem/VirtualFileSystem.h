#pragma once

#include <AK/HashMap.h>
#include <AK/OwnPtr.h>
#include <AK/RetainPtr.h>
#include <AK/String.h>
#include <AK/Vector.h>
#include "InodeIdentifier.h"
#include "Limits.h"

class CharacterDevice;
class FileHandle;
class FileSystem;

class VirtualFileSystem {
public:
    struct Node {
        InodeIdentifier inode;

        bool inUse() const { return inode.isValid(); }

        bool isCharacterDevice() const { return m_characterDevice; }
        CharacterDevice* characterDevice() { return m_characterDevice; }

        void retain();
        void release();

        FileSystem* fileSystem() { return inode.fileSystem(); }
        const FileSystem* fileSystem() const { return inode.fileSystem(); }

    private:
        friend class VirtualFileSystem;
        VirtualFileSystem* vfs { nullptr };
        unsigned retainCount { 0 };
        CharacterDevice* m_characterDevice { nullptr };
    };

    VirtualFileSystem();
    ~VirtualFileSystem();

    bool isDirectory(const String& path);
    void listDirectory(const String& path);
    void listDirectoryRecursively(const String& path);

    unsigned maxNodeCount() const { return m_maxNodeCount; }
    unsigned allocatedNodeCount() const { return m_maxNodeCount - m_nodeFreeList.size(); }

    Node* root() { return m_rootNode.ptr(); }
    const Node* root() const { return m_rootNode.ptr(); }

    bool mountRoot(RetainPtr<FileSystem>&&);
    bool mount(RetainPtr<FileSystem>&&, const String& path);

    OwnPtr<FileHandle> open(const String& path);
    OwnPtr<FileHandle> create(const String& path);

    bool isRoot(InodeIdentifier) const;

    bool touch(const String&path);

    void registerCharacterDevice(unsigned major, unsigned minor, CharacterDevice&);

private:
    template<typename F> void enumerateDirectoryInode(InodeIdentifier, F func);
    InodeIdentifier resolvePath(const String& path);
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

