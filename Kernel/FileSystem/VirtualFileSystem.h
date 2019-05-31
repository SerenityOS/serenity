#pragma once

#include "FileSystem.h"
#include "InodeIdentifier.h"
#include "InodeMetadata.h"
#include <AK/AKString.h>
#include <AK/Function.h>
#include <AK/HashMap.h>
#include <AK/OwnPtr.h>
#include <AK/RetainPtr.h>
#include <AK/Vector.h>
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

class Custody;
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
        Mount(RetainPtr<Custody>&&, Retained<FS>&&);

        InodeIdentifier host() const;
        InodeIdentifier guest() const { return m_guest; }

        const FS& guest_fs() const { return *m_guest_fs; }

        String absolute_path() const;

    private:
        InodeIdentifier m_host;
        InodeIdentifier m_guest;
        Retained<FS> m_guest_fs;
        RetainPtr<Custody> m_host_custody;
    };

    [[gnu::pure]] static VFS& the();

    VFS();
    ~VFS();

    bool mount_root(Retained<FS>&&);
    bool mount(Retained<FS>&&, StringView path);

    KResultOr<Retained<FileDescriptor>> open(RetainPtr<Device>&&, int options);
    KResultOr<Retained<FileDescriptor>> open(StringView path, int options, mode_t mode, Custody& base);
    KResultOr<Retained<FileDescriptor>> create(StringView path, int options, mode_t mode, Custody& base);
    KResult mkdir(StringView path, mode_t mode, Custody& base);
    KResult link(StringView old_path, StringView new_path, Custody& base);
    KResult unlink(StringView path, Custody& base);
    KResult symlink(StringView target, StringView linkpath, Custody& base);
    KResult rmdir(StringView path, Custody& base);
    KResult chmod(StringView path, mode_t, Custody& base);
    KResult fchmod(Inode&, mode_t);
    KResult chown(StringView path, uid_t, gid_t, Custody& base);
    KResult access(StringView path, int mode, Custody& base);
    KResult stat(StringView path, int options, Custody& base, struct stat&);
    KResult utime(StringView path, Custody& base, time_t atime, time_t mtime);
    KResult rename(StringView oldpath, StringView newpath, Custody& base);
    KResult mknod(StringView path, mode_t, dev_t, Custody& base);
    KResultOr<Retained<Custody>> open_directory(StringView path, Custody& base);

    void register_device(Device&);
    void unregister_device(Device&);

    size_t mount_count() const { return m_mounts.size(); }
    void for_each_mount(Function<void(const Mount&)>) const;

    InodeIdentifier root_inode_id() const;

    void sync();

    Device* get_device(unsigned major, unsigned minor);

    Custody& root_custody();
    KResultOr<Retained<Custody>> resolve_path(StringView path, Custody& base, RetainPtr<Custody>* parent = nullptr, int options = 0);

private:
    friend class FileDescriptor;

    RetainPtr<Inode> get_inode(InodeIdentifier);

    bool is_vfs_root(InodeIdentifier) const;

    void traverse_directory_inode(Inode&, Function<bool(const FS::DirectoryEntry&)>);

    Mount* find_mount_for_host(InodeIdentifier);
    Mount* find_mount_for_guest(InodeIdentifier);

    RetainPtr<Inode> m_root_inode;
    Vector<OwnPtr<Mount>> m_mounts;
    HashMap<dword, Device*> m_devices;

    RetainPtr<Custody> m_root_custody;
};
