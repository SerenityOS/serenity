#pragma once

#include <AK/HashMap.h>
#include <AK/OwnPtr.h>
#include <AK/RetainPtr.h>
#include <AK/AKString.h>
#include <AK/Vector.h>
#include <AK/Function.h>
#include "InodeIdentifier.h"
#include "InodeMetadata.h"
#include "Limits.h"
#include "FileSystem.h"
#include <Kernel/KResult.h>

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

class Device;
class FileDescriptor;

inline constexpr dword encoded_device(unsigned major, unsigned minor)
{
    return (minor & 0xff) | (major << 8) | ((minor & ~0xff) << 12);
}

class VFS;

class VFS {
    AK_MAKE_ETERNAL
public:
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

    [[gnu::pure]] static VFS& the();

    VFS();
    ~VFS();

    bool mount_root(RetainPtr<FS>&&);
    bool mount(RetainPtr<FS>&&, const String& path);

    KResultOr<Retained<FileDescriptor>> open(RetainPtr<Device>&&, int options);
    KResultOr<Retained<FileDescriptor>> open(const String& path, int options, mode_t mode, Inode& base);
    KResultOr<Retained<FileDescriptor>> create(const String& path, int options, mode_t mode, Inode& base);
    KResult mkdir(const String& path, mode_t mode, Inode& base);
    KResult link(const String& old_path, const String& new_path, Inode& base);
    KResult unlink(const String& path, Inode& base);
    KResult symlink(const String& target, const String& linkpath, Inode& base);
    KResult rmdir(const String& path, Inode& base);
    KResult chmod(const String& path, mode_t, Inode& base);
    KResult chmod(Inode&, mode_t);
    KResult chown(const String& path, uid_t, gid_t, Inode& base);
    KResult access(const String& path, int mode, Inode& base);
    KResult stat(const String& path, int options, Inode& base, struct stat&);
    KResult utime(const String& path, Inode& base, time_t atime, time_t mtime);
    KResultOr<Retained<Inode>> open_directory(const String& path, Inode& base);

    void register_device(Device&);
    void unregister_device(Device&);

    size_t mount_count() const { return m_mounts.size(); }
    void for_each_mount(Function<void(const Mount&)>) const;

    KResultOr<String> absolute_path(Inode&);
    KResultOr<String> absolute_path(InodeIdentifier);

    InodeIdentifier root_inode_id() const;
    Inode* root_inode() { return m_root_inode.ptr(); }
    const Inode* root_inode() const { return m_root_inode.ptr(); }

    void sync();

    Device* get_device(unsigned major, unsigned minor);

private:
    friend class FileDescriptor;

    RetainPtr<Inode> get_inode(InodeIdentifier);

    bool is_vfs_root(InodeIdentifier) const;

    void traverse_directory_inode(Inode&, Function<bool(const FS::DirectoryEntry&)>);
    InodeIdentifier old_resolve_path(const String& path, InodeIdentifier base, int& error, int options = 0, InodeIdentifier* parent_id = nullptr);
    KResultOr<InodeIdentifier> resolve_path(const String& path, InodeIdentifier base, int options = 0, InodeIdentifier* parent_id = nullptr);
    KResultOr<Retained<Inode>> resolve_path_to_inode(const String& path, Inode& base, RetainPtr<Inode>* parent_id = nullptr, int options = 0);
    KResultOr<InodeIdentifier> resolve_symbolic_link(InodeIdentifier base, Inode& symlink_inode);

    Mount* find_mount_for_host(InodeIdentifier);
    Mount* find_mount_for_guest(InodeIdentifier);

    RetainPtr<Inode> m_root_inode;
    Vector<OwnPtr<Mount>> m_mounts;
    HashMap<dword, Device*> m_devices;
};

