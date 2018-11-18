#pragma once

#include <AK/HashMap.h>
#include <AK/OwnPtr.h>
#include <AK/RetainPtr.h>
#include <AK/String.h>
#include <AK/Vector.h>
#include <AK/Function.h>
#include "InodeIdentifier.h"
#include "InodeMetadata.h"
#include "Limits.h"
#include "FileSystem.h"

#define O_RDONLY 0
#define O_WRONLY 1
#define O_RDWR 2
#define O_CREAT 0100
#define O_EXCL 0200
#define O_NOCTTY 0400
#define O_TRUNC 01000
#define O_APPEND 02000
#define O_NONBLOCK 04000
#define O_DIRECTORY 00200000
#define O_NOFOLLOW 00400000
#define O_CLOEXEC 02000000
#define O_NOFOLLOW_NOERROR 0x4000000

class CharacterDevice;
class FileDescriptor;

inline constexpr dword encodedDevice(unsigned major, unsigned minor)
{
    return (minor & 0xff) | (major << 8) | ((minor & ~0xff) << 12);
}

class VFS;

class Vnode {
public:
    InodeIdentifier inode;
    const InodeMetadata& metadata() const;

    bool inUse() const { return inode.isValid() || m_characterDevice; }

    bool isCharacterDevice() const { return m_characterDevice; }
    CharacterDevice* characterDevice() { return m_characterDevice; }
    const CharacterDevice* characterDevice() const { return m_characterDevice; }

    void retain();
    void release();

    FS* fileSystem() { return inode.fileSystem(); }
    const FS* fileSystem() const { return inode.fileSystem(); }

    VFS* vfs() { return m_vfs; }
    const VFS* vfs() const { return m_vfs; }

    void* vmo() { return m_vmo; }
    void set_vmo(void* vmo) { m_vmo = vmo; }

    unsigned retain_count() const { return retainCount; }

    CoreInode* core_inode() { return m_core_inode.ptr(); }

private:
    friend class VFS;
    VFS* m_vfs { nullptr };
    unsigned retainCount { 0 };
    CharacterDevice* m_characterDevice { nullptr };
    mutable InodeMetadata m_cachedMetadata;
    void* m_vmo { nullptr };
    RetainPtr<CoreInode> m_core_inode;
};

class VFS {
    AK_MAKE_ETERNAL
    friend ByteBuffer procfs$vnodes();
public:
    static void initialize_globals();

    class Mount {
    public:
        Mount(InodeIdentifier host, RetainPtr<FS>&&);

        InodeIdentifier host() const { return m_host; }
        InodeIdentifier guest() const { return m_guest; }

        const FS& guest_fs() const { return *m_guest_fs; }

    private:
        InodeIdentifier m_host;
        InodeIdentifier m_guest;
        RetainPtr<FS> m_guest_fs;
    };

    static VFS& the() PURE;

    VFS();
    ~VFS();

#ifndef SERENITY
    bool isDirectory(const String& path, InodeIdentifier base = InodeIdentifier());
    void listDirectory(const String& path, InodeIdentifier base);
    void listDirectoryRecursively(const String& path, InodeIdentifier base);
#endif

    unsigned max_vnode_count() const { return m_max_vnode_count; }
    unsigned allocated_vnode_count() const { return m_max_vnode_count - m_vnode_freelist.size(); }

    Vnode* root() { return m_root_vnode.ptr(); }
    const Vnode* root() const { return m_root_vnode.ptr(); }

    bool mount_root(RetainPtr<FS>&&);
    bool mount(RetainPtr<FS>&&, const String& path);

    RetainPtr<FileDescriptor> open(CharacterDevice&, int options);
    RetainPtr<FileDescriptor> open(const String& path, int& error, int options = 0, InodeIdentifier base = InodeIdentifier());
    RetainPtr<FileDescriptor> create(const String& path, InodeIdentifier base, int& error);
    bool mkdir(const String& path, mode_t mode, InodeIdentifier base, int& error);

    bool touch(const String&path);

    void register_character_device(CharacterDevice&);

    size_t mount_count() const { return m_mounts.size(); }
    void for_each_mount(Function<void(const Mount&)>) const;

    String absolute_path(CoreInode&);

private:
    friend class FileDescriptor;
    friend class Vnode;

    RetainPtr<CoreInode> get_inode(InodeIdentifier);

    bool is_vfs_root(InodeIdentifier) const;

    void traverse_directory_inode(CoreInode&, Function<bool(const FS::DirectoryEntry&)>);
    InodeIdentifier resolve_path(const String& path, int& error, InodeIdentifier base = InodeIdentifier(), int options = 0);
    InodeIdentifier resolveSymbolicLink(InodeIdentifier base, InodeIdentifier symlinkInode, int& error);

    RetainPtr<Vnode> allocateNode();
    void freeNode(Vnode*);

    RetainPtr<Vnode> makeNode(InodeIdentifier);
    RetainPtr<Vnode> makeNode(CharacterDevice&);
    RetainPtr<Vnode> get_or_create_node(InodeIdentifier);
    RetainPtr<Vnode> get_or_create_node(CharacterDevice&);

    Mount* find_mount_for_host(InodeIdentifier);
    Mount* find_mount_for_guest(InodeIdentifier);

    HashMap<InodeIdentifier, Vnode*> m_inode2vnode;
    HashMap<dword, Vnode*> m_device2vnode;

    Vector<OwnPtr<Mount>> m_mounts;

    unsigned m_max_vnode_count { 0 };
    Vnode* m_nodes { nullptr };

    Vector<Vnode*> m_vnode_freelist;

    RetainPtr<Vnode> m_root_vnode;

    HashMap<dword, CharacterDevice*> m_character_devices;
};

