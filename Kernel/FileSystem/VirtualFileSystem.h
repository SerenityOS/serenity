#pragma once

#include <AK/AKString.h>
#include <AK/Badge.h>
#include <AK/Function.h>
#include <AK/HashMap.h>
#include <AK/NonnullOwnPtrVector.h>
#include <AK/OwnPtr.h>
#include <AK/RefPtr.h>
#include <Kernel/FileSystem/FileSystem.h>
#include <Kernel/FileSystem/InodeIdentifier.h>
#include <Kernel/FileSystem/InodeMetadata.h>
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
class FileDescription;

class VFS {
    AK_MAKE_ETERNAL
public:
    class Mount {
    public:
        Mount(RefPtr<Custody>&&, NonnullRefPtr<FS>&&);

        InodeIdentifier host() const;
        InodeIdentifier guest() const { return m_guest; }

        const FS& guest_fs() const { return *m_guest_fs; }

        String absolute_path() const;

    private:
        InodeIdentifier m_host;
        InodeIdentifier m_guest;
        NonnullRefPtr<FS> m_guest_fs;
        RefPtr<Custody> m_host_custody;
    };

    static VFS& the();

    VFS();
    ~VFS();

    bool mount_root(NonnullRefPtr<FS>&&);
    KResult mount(NonnullRefPtr<FS>&&, StringView path);
    KResult mount(NonnullRefPtr<FS>&&, Custody& mount_point);
    KResult unmount(InodeIdentifier guest_inode_id);

    KResultOr<NonnullRefPtr<FileDescription>> open(StringView path, int options, mode_t mode, Custody& base);
    KResultOr<NonnullRefPtr<FileDescription>> create(StringView path, int options, mode_t mode, Custody& parent_custody);
    KResult mkdir(StringView path, mode_t mode, Custody& base);
    KResult link(StringView old_path, StringView new_path, Custody& base);
    KResult unlink(StringView path, Custody& base);
    KResult symlink(StringView target, StringView linkpath, Custody& base);
    KResult rmdir(StringView path, Custody& base);
    KResult chmod(StringView path, mode_t, Custody& base);
    KResult chmod(Inode&, mode_t);
    KResult chown(StringView path, uid_t, gid_t, Custody& base);
    KResult chown(Inode&, uid_t, gid_t);
    KResult access(StringView path, int mode, Custody& base);
    KResultOr<InodeMetadata> lookup_metadata(StringView path, Custody& base, int options = 0);
    KResult utime(StringView path, Custody& base, time_t atime, time_t mtime);
    KResult rename(StringView oldpath, StringView newpath, Custody& base);
    KResult mknod(StringView path, mode_t, dev_t, Custody& base);
    KResultOr<NonnullRefPtr<Custody>> open_directory(StringView path, Custody& base);

    void register_device(Badge<Device>, Device&);
    void unregister_device(Badge<Device>, Device&);

    size_t mount_count() const { return m_mounts.size(); }
    void for_each_mount(Function<void(const Mount&)>) const;

    InodeIdentifier root_inode_id() const;

    void sync();

    Device* get_device(unsigned major, unsigned minor);

    Custody& root_custody();
    KResultOr<NonnullRefPtr<Custody>> resolve_path(StringView path, Custody& base, RefPtr<Custody>* parent = nullptr, int options = 0);

private:
    friend class FileDescription;

    RefPtr<Inode> get_inode(InodeIdentifier);

    bool is_vfs_root(InodeIdentifier) const;

    void traverse_directory_inode(Inode&, Function<bool(const FS::DirectoryEntry&)>);

    Mount* find_mount_for_host(InodeIdentifier);
    Mount* find_mount_for_guest(InodeIdentifier);

    Lock m_lock { "VFSLock" };

    RefPtr<Inode> m_root_inode;
    NonnullOwnPtrVector<Mount> m_mounts;
    HashMap<u32, Device*> m_devices;

    RefPtr<Custody> m_root_custody;
};
