/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/AnyOf.h>
#include <AK/GenericLexer.h>
#include <AK/RefPtr.h>
#include <AK/Singleton.h>
#include <AK/StringBuilder.h>
#include <Kernel/API/POSIX/errno.h>
#include <Kernel/Debug.h>
#include <Kernel/Devices/BlockDevice.h>
#include <Kernel/Devices/DeviceManagement.h>
#include <Kernel/FileSystem/Custody.h>
#include <Kernel/FileSystem/FileBackedFileSystem.h>
#include <Kernel/FileSystem/FileSystem.h>
#include <Kernel/FileSystem/OpenFileDescription.h>
#include <Kernel/FileSystem/VirtualFileSystem.h>
#include <Kernel/KLexicalPath.h>
#include <Kernel/KSyms.h>
#include <Kernel/Process.h>
#include <Kernel/Sections.h>

namespace Kernel {

static Singleton<VirtualFileSystem> s_the;
static constexpr int root_mount_flags = MS_NODEV | MS_NOSUID | MS_RDONLY;

UNMAP_AFTER_INIT void VirtualFileSystem::initialize()
{
    s_the.ensure_instance();
}

VirtualFileSystem& VirtualFileSystem::the()
{
    return *s_the;
}

UNMAP_AFTER_INIT VirtualFileSystem::VirtualFileSystem()
    : m_root_custody(LockRank::None)
{
}

UNMAP_AFTER_INIT VirtualFileSystem::~VirtualFileSystem() = default;

InodeIdentifier VirtualFileSystem::root_inode_id() const
{
    VERIFY(m_root_inode);
    return m_root_inode->identifier();
}

bool VirtualFileSystem::mount_point_exists_at_inode(InodeIdentifier inode_identifier)
{
    return m_mounts.with([&](auto& mounts) -> bool {
        return any_of(mounts, [&inode_identifier](auto const& existing_mount) {
            return existing_mount->host() && existing_mount->host()->identifier() == inode_identifier;
        });
    });
}

ErrorOr<void> VirtualFileSystem::mount(FileSystem& fs, Custody& mount_point, int flags)
{
    auto new_mount = TRY(adopt_nonnull_own_or_enomem(new (nothrow) Mount(fs, &mount_point, flags)));
    return m_mounts.with([&](auto& mounts) -> ErrorOr<void> {
        auto& inode = mount_point.inode();
        dbgln("VirtualFileSystem: FileSystemID {}, Mounting {} at inode {} with flags {}",
            fs.fsid(),
            fs.class_name(),
            inode.identifier(),
            flags);
        if (mount_point_exists_at_inode(inode.identifier())) {
            dbgln("VirtualFileSystem: Mounting unsuccessful - inode {} is already a mount-point.", inode.identifier());
            return EBUSY;
        }
        // Note: Actually add a mount for the filesystem and increment the filesystem mounted count
        new_mount->guest_fs().mounted_count({}).with([&](auto& mounted_count) {
            mounted_count++;
        });
        mounts.append(move(new_mount));
        return {};
    });
}

ErrorOr<void> VirtualFileSystem::bind_mount(Custody& source, Custody& mount_point, int flags)
{
    auto new_mount = TRY(adopt_nonnull_own_or_enomem(new (nothrow) Mount(source.inode(), mount_point, flags)));
    return m_mounts.with([&](auto& mounts) -> ErrorOr<void> {
        auto& inode = mount_point.inode();
        dbgln("VirtualFileSystem: Bind-mounting inode {} at inode {}", source.inode().identifier(), inode.identifier());
        if (mount_point_exists_at_inode(inode.identifier())) {
            dbgln("VirtualFileSystem: Bind-mounting unsuccessful - inode {} is already a mount-point.",
                mount_point.inode().identifier());
            return EBUSY;
        }
        mounts.append(move(new_mount));
        return {};
    });
}

ErrorOr<void> VirtualFileSystem::remount(Custody& mount_point, int new_flags)
{
    dbgln("VirtualFileSystem: Remounting inode {}", mount_point.inode().identifier());

    auto* mount = find_mount_for_guest(mount_point.inode().identifier());
    if (!mount)
        return ENODEV;

    mount->set_flags(new_flags);
    return {};
}

void VirtualFileSystem::sync_filesystems()
{
    NonnullLockRefPtrVector<FileSystem, 32> file_systems;
    m_file_systems_list.with([&](auto const& list) {
        for (auto& fs : list)
            file_systems.append(fs);
    });

    for (auto& fs : file_systems)
        fs.flush_writes();
}

void VirtualFileSystem::lock_all_filesystems()
{
    NonnullLockRefPtrVector<FileSystem, 32> file_systems;
    m_file_systems_list.with([&](auto const& list) {
        for (auto& fs : list)
            file_systems.append(fs);
    });

    for (auto& fs : file_systems)
        fs.m_lock.lock();
}

ErrorOr<void> VirtualFileSystem::unmount(Custody& mountpoint_custody)
{
    auto& guest_inode = mountpoint_custody.inode();
    auto custody_path = TRY(mountpoint_custody.try_serialize_absolute_path());
    dbgln("VirtualFileSystem: unmount called with inode {} on mountpoint {}", guest_inode.identifier(), custody_path->view());

    return m_mounts.with([&](auto& mounts) -> ErrorOr<void> {
        for (size_t i = 0; i < mounts.size(); ++i) {
            auto& mount = mounts[i];
            if (&mount->guest() != &guest_inode)
                continue;
            auto mountpoint_path = TRY(mount->absolute_path());
            if (custody_path->view() != mountpoint_path->view())
                continue;
            NonnullRefPtr<FileSystem> fs = mount->guest_fs();
            TRY(fs->prepare_to_unmount());
            fs->mounted_count({}).with([&](auto& mounted_count) {
                VERIFY(mounted_count > 0);
                if (mounted_count == 1) {
                    dbgln("VirtualFileSystem: Unmounting file system {} for the last time...", fs->fsid());
                    m_file_systems_list.with([&](auto& list) {
                        list.remove(*fs);
                    });
                    if (fs->is_file_backed()) {
                        dbgln("VirtualFileSystem: Unmounting file backed file system {} for the last time...", fs->fsid());
                        auto& file_backed_fs = static_cast<FileBackedFileSystem&>(*fs);
                        m_file_backed_file_systems_list.with([&](auto& list) {
                            list.remove(file_backed_fs);
                        });
                    }
                } else {
                    mounted_count--;
                }
            });
            dbgln("VirtualFileSystem: Unmounting file system {}...", fs->fsid());
            (void)mounts.unstable_take(i);
            return {};
        }
        dbgln("VirtualFileSystem: Nothing mounted on inode {}", guest_inode.identifier());
        return ENODEV;
    });
}

ErrorOr<void> VirtualFileSystem::mount_root(FileSystem& fs)
{
    if (m_root_inode) {
        dmesgln("VirtualFileSystem: mount_root can't mount another root");
        return EEXIST;
    }

    auto new_mount = TRY(adopt_nonnull_own_or_enomem(new (nothrow) Mount(fs, nullptr, root_mount_flags)));

    auto& root_inode = fs.root_inode();
    if (!root_inode.is_directory()) {
        dmesgln("VirtualFileSystem: root inode ({}) for / is not a directory :(", root_inode.identifier());
        return ENOTDIR;
    }

    m_root_inode = root_inode;
    if (fs.is_file_backed()) {
        auto pseudo_path = TRY(static_cast<FileBackedFileSystem&>(fs).file_description().pseudo_path());
        dmesgln("VirtualFileSystem: mounted root({}) from {} ({})", fs.fsid(), fs.class_name(), pseudo_path);
        m_file_backed_file_systems_list.with([&](auto& list) {
            list.append(static_cast<FileBackedFileSystem&>(fs));
        });
    } else {
        dmesgln("VirtualFileSystem: mounted root({}) from {}", fs.fsid(), fs.class_name());
    }

    m_file_systems_list.with([&](auto& fs_list) {
        fs_list.append(fs);
    });

    // Note: Actually add a mount for the filesystem and increment the filesystem mounted count
    m_mounts.with([&](auto& mounts) {
        new_mount->guest_fs().mounted_count({}).with([&](auto& mounted_count) {
            mounted_count++;
        });
        mounts.append(move(new_mount));
    });

    RefPtr<Custody> new_root_custody = TRY(Custody::try_create(nullptr, ""sv, *m_root_inode, root_mount_flags));
    m_root_custody.with([&](auto& root_custody) {
        swap(root_custody, new_root_custody);
    });
    return {};
}

auto VirtualFileSystem::find_mount_for_host(InodeIdentifier id) -> Mount*
{
    return m_mounts.with([&](auto& mounts) -> Mount* {
        for (auto& mount : mounts) {
            if (mount->host() && mount->host()->identifier() == id)
                return mount.ptr();
        }
        return nullptr;
    });
}

auto VirtualFileSystem::find_mount_for_guest(InodeIdentifier id) -> Mount*
{
    return m_mounts.with([&](auto& mounts) -> Mount* {
        for (auto& mount : mounts) {
            if (mount->guest().identifier() == id)
                return mount.ptr();
        }
        return nullptr;
    });
}

bool VirtualFileSystem::is_vfs_root(InodeIdentifier inode) const
{
    return inode == root_inode_id();
}

ErrorOr<void> VirtualFileSystem::traverse_directory_inode(Inode& dir_inode, Function<ErrorOr<void>(FileSystem::DirectoryEntryView const&)> callback)
{
    return dir_inode.traverse_as_directory([&](auto& entry) -> ErrorOr<void> {
        InodeIdentifier resolved_inode;
        if (auto mount = find_mount_for_host(entry.inode))
            resolved_inode = mount->guest().identifier();
        else
            resolved_inode = entry.inode;

        // FIXME: This is now broken considering chroot and bind mounts.
        bool is_root_inode = dir_inode.identifier() == dir_inode.fs().root_inode().identifier();
        if (is_root_inode && !is_vfs_root(dir_inode.identifier()) && entry.name == "..") {
            auto mount = find_mount_for_guest(dir_inode.identifier());
            VERIFY(mount);
            VERIFY(mount->host());
            resolved_inode = mount->host()->identifier();
        }
        TRY(callback({ entry.name, resolved_inode, entry.file_type }));
        return {};
    });
}

ErrorOr<void> VirtualFileSystem::utime(Credentials const& credentials, StringView path, Custody& base, time_t atime, time_t mtime)
{
    auto custody = TRY(resolve_path(credentials, path, base));
    auto& inode = custody->inode();
    if (!credentials.is_superuser() && inode.metadata().uid != credentials.euid())
        return EACCES;
    if (custody->is_readonly())
        return EROFS;

    TRY(inode.update_timestamps(atime, {}, mtime));
    return {};
}

ErrorOr<void> VirtualFileSystem::utimensat(Credentials const& credentials, StringView path, Custody& base, timespec const& atime, timespec const& mtime, int options)
{
    auto custody = TRY(resolve_path(credentials, path, base, nullptr, options));
    auto& inode = custody->inode();
    if (!credentials.is_superuser() && inode.metadata().uid != credentials.euid())
        return EACCES;
    if (custody->is_readonly())
        return EROFS;

    // NOTE: A standard ext2 inode cannot store nanosecond timestamps.
    TRY(inode.update_timestamps(
        (atime.tv_nsec != UTIME_OMIT) ? atime.tv_sec : Optional<time_t> {},
        {},
        (mtime.tv_nsec != UTIME_OMIT) ? mtime.tv_sec : Optional<time_t> {}));

    return {};
}

ErrorOr<InodeMetadata> VirtualFileSystem::lookup_metadata(Credentials const& credentials, StringView path, Custody& base, int options)
{
    auto custody = TRY(resolve_path(credentials, path, base, nullptr, options));
    return custody->inode().metadata();
}

ErrorOr<NonnullLockRefPtr<FileBackedFileSystem>> VirtualFileSystem::find_already_existing_or_create_file_backed_file_system(OpenFileDescription& description, Function<ErrorOr<NonnullLockRefPtr<FileSystem>>(OpenFileDescription&)> callback)
{
    return TRY(m_file_backed_file_systems_list.with([&](auto& list) -> ErrorOr<NonnullLockRefPtr<FileBackedFileSystem>> {
        for (auto& node : list) {
            if (&node.file_description() == &description) {
                return node;
            }
            if (&node.file() == &description.file()) {
                return node;
            }
        }
        auto fs = TRY(callback(description));
        VERIFY(fs->is_file_backed());
        list.append(static_cast<FileBackedFileSystem&>(*fs));
        m_file_systems_list.with([&](auto& fs_list) {
            fs_list.append(*fs);
        });
        return static_ptr_cast<FileBackedFileSystem>(fs);
    }));
}

ErrorOr<NonnullLockRefPtr<OpenFileDescription>> VirtualFileSystem::open(Credentials const& credentials, StringView path, int options, mode_t mode, Custody& base, Optional<UidAndGid> owner)
{
    if ((options & O_CREAT) && (options & O_DIRECTORY))
        return EINVAL;

    RefPtr<Custody> parent_custody;
    auto custody_or_error = resolve_path(credentials, path, base, &parent_custody, options);
    if (custody_or_error.is_error()) {
        // NOTE: ENOENT with a non-null parent custody signals us that the immediate parent
        //       of the file exists, but the file itself does not.
        if ((options & O_CREAT) && custody_or_error.error().code() == ENOENT && parent_custody)
            return create(credentials, path, options, mode, *parent_custody, move(owner));
        return custody_or_error.release_error();
    }

    if ((options & O_CREAT) && (options & O_EXCL))
        return EEXIST;

    auto& custody = *custody_or_error.value();
    auto& inode = custody.inode();
    auto metadata = inode.metadata();

    if (metadata.is_regular_file() && (custody.mount_flags() & MS_NOREGULAR))
        return EACCES;

    if ((options & O_DIRECTORY) && !metadata.is_directory())
        return ENOTDIR;

    bool should_truncate_file = false;

    if ((options & O_RDONLY) && !metadata.may_read(credentials))
        return EACCES;

    if (options & O_WRONLY) {
        if (!metadata.may_write(credentials))
            return EACCES;
        if (metadata.is_directory())
            return EISDIR;
        should_truncate_file = options & O_TRUNC;
    }
    if (options & O_EXEC) {
        if (!metadata.may_execute(credentials) || (custody.mount_flags() & MS_NOEXEC))
            return EACCES;
    }

    if (metadata.is_fifo()) {
        auto fifo = TRY(inode.fifo());
        if (options & O_WRONLY) {
            auto description = TRY(fifo->open_direction_blocking(FIFO::Direction::Writer));
            description->set_rw_mode(options);
            description->set_file_flags(options);
            description->set_original_inode({}, inode);
            return description;
        } else if (options & O_RDONLY) {
            auto description = TRY(fifo->open_direction_blocking(FIFO::Direction::Reader));
            description->set_rw_mode(options);
            description->set_file_flags(options);
            description->set_original_inode({}, inode);
            return description;
        }
        return EINVAL;
    }

    if (metadata.is_device()) {
        if (custody.mount_flags() & MS_NODEV)
            return EACCES;
        auto device = DeviceManagement::the().get_device(metadata.major_device, metadata.minor_device);
        if (device == nullptr) {
            return ENODEV;
        }
        auto description = TRY(device->open(options));
        description->set_original_inode({}, inode);
        description->set_original_custody({}, custody);
        return description;
    }

    // Check for read-only FS. Do this after handling devices, but before modifying the inode in any way.
    if ((options & O_WRONLY) && custody.is_readonly())
        return EROFS;

    if (should_truncate_file) {
        TRY(inode.truncate(0));
        TRY(inode.update_timestamps({}, {}, kgettimeofday().to_truncated_seconds()));
    }
    auto description = TRY(OpenFileDescription::try_create(custody));
    description->set_rw_mode(options);
    description->set_file_flags(options);
    return description;
}

ErrorOr<void> VirtualFileSystem::mknod(Credentials const& credentials, StringView path, mode_t mode, dev_t dev, Custody& base)
{
    if (!is_regular_file(mode) && !is_block_device(mode) && !is_character_device(mode) && !is_fifo(mode) && !is_socket(mode))
        return EINVAL;

    RefPtr<Custody> parent_custody;
    auto existing_file_or_error = resolve_path(credentials, path, base, &parent_custody);
    if (!existing_file_or_error.is_error())
        return EEXIST;
    if (!parent_custody)
        return ENOENT;
    if (existing_file_or_error.error().code() != ENOENT)
        return existing_file_or_error.release_error();
    auto& parent_inode = parent_custody->inode();
    if (!parent_inode.metadata().may_write(credentials))
        return EACCES;
    if (parent_custody->is_readonly())
        return EROFS;

    auto basename = KLexicalPath::basename(path);
    dbgln_if(VFS_DEBUG, "VirtualFileSystem::mknod: '{}' mode={} dev={} in {}", basename, mode, dev, parent_inode.identifier());
    (void)TRY(parent_inode.create_child(basename, mode, dev, credentials.euid(), credentials.egid()));
    return {};
}

ErrorOr<NonnullLockRefPtr<OpenFileDescription>> VirtualFileSystem::create(Credentials const& credentials, StringView path, int options, mode_t mode, Custody& parent_custody, Optional<UidAndGid> owner)
{
    auto basename = KLexicalPath::basename(path);
    auto parent_path = TRY(parent_custody.try_serialize_absolute_path());
    auto full_path = TRY(KLexicalPath::try_join(parent_path->view(), basename));
    TRY(validate_path_against_process_veil(full_path->view(), options));

    if (!is_socket(mode) && !is_fifo(mode) && !is_block_device(mode) && !is_character_device(mode)) {
        // Turn it into a regular file. (This feels rather hackish.)
        mode |= 0100000;
    }

    auto& parent_inode = parent_custody.inode();
    if (!parent_inode.metadata().may_write(credentials))
        return EACCES;
    if (parent_custody.is_readonly())
        return EROFS;
    if (is_regular_file(mode) && (parent_custody.mount_flags() & MS_NOREGULAR))
        return EACCES;

    dbgln_if(VFS_DEBUG, "VirtualFileSystem::create: '{}' in {}", basename, parent_inode.identifier());
    auto uid = owner.has_value() ? owner.value().uid : credentials.euid();
    auto gid = owner.has_value() ? owner.value().gid : credentials.egid();

    auto inode = TRY(parent_inode.create_child(basename, mode, 0, uid, gid));
    auto custody = TRY(Custody::try_create(&parent_custody, basename, inode, parent_custody.mount_flags()));

    auto description = TRY(OpenFileDescription::try_create(move(custody)));
    description->set_rw_mode(options);
    description->set_file_flags(options);
    return description;
}

ErrorOr<void> VirtualFileSystem::mkdir(Credentials const& credentials, StringView path, mode_t mode, Custody& base)
{
    // Unlike in basically every other case, where it's only the last
    // path component (the one being created) that is allowed not to
    // exist, POSIX allows mkdir'ed path to have trailing slashes.
    // Let's handle that case by trimming any trailing slashes.
    path = path.trim("/"sv, TrimMode::Right);
    if (path.is_empty()) {
        // NOTE: This means the path was a series of slashes, which resolves to "/".
        path = "/"sv;
    }

    RefPtr<Custody> parent_custody;
    // FIXME: The errors returned by resolve_path_without_veil can leak information about paths that are not unveiled,
    //        e.g. when the error is EACCESS or similar.
    auto result = resolve_path_without_veil(credentials, path, base, &parent_custody);
    if (!result.is_error())
        return EEXIST;
    else if (!parent_custody)
        return result.release_error();
    // NOTE: If resolve_path fails with a non-null parent custody, the error should be ENOENT.
    VERIFY(result.error().code() == ENOENT);

    TRY(validate_path_against_process_veil(*parent_custody, O_CREAT));
    auto& parent_inode = parent_custody->inode();
    if (!parent_inode.metadata().may_write(credentials))
        return EACCES;
    if (parent_custody->is_readonly())
        return EROFS;

    auto basename = KLexicalPath::basename(path);
    dbgln_if(VFS_DEBUG, "VirtualFileSystem::mkdir: '{}' in {}", basename, parent_inode.identifier());
    (void)TRY(parent_inode.create_child(basename, S_IFDIR | mode, 0, credentials.euid(), credentials.egid()));
    return {};
}

ErrorOr<void> VirtualFileSystem::access(Credentials const& credentials, StringView path, int mode, Custody& base)
{
    auto custody = TRY(resolve_path(credentials, path, base));

    auto& inode = custody->inode();
    auto metadata = inode.metadata();
    if (mode & R_OK) {
        if (!metadata.may_read(credentials))
            return EACCES;
    }
    if (mode & W_OK) {
        if (!metadata.may_write(credentials))
            return EACCES;
        if (custody->is_readonly())
            return EROFS;
    }
    if (mode & X_OK) {
        if (!metadata.may_execute(credentials))
            return EACCES;
    }
    return {};
}

ErrorOr<NonnullRefPtr<Custody>> VirtualFileSystem::open_directory(Credentials const& credentials, StringView path, Custody& base)
{
    auto custody = TRY(resolve_path(credentials, path, base));
    auto& inode = custody->inode();
    if (!inode.is_directory())
        return ENOTDIR;
    if (!inode.metadata().may_execute(credentials))
        return EACCES;
    return custody;
}

ErrorOr<void> VirtualFileSystem::chmod(Credentials const& credentials, Custody& custody, mode_t mode)
{
    auto& inode = custody.inode();

    if (credentials.euid() != inode.metadata().uid && !credentials.is_superuser())
        return EPERM;
    if (custody.is_readonly())
        return EROFS;

    // Only change the permission bits.
    mode = (inode.mode() & ~07777u) | (mode & 07777u);
    return inode.chmod(mode);
}

ErrorOr<void> VirtualFileSystem::chmod(Credentials const& credentials, StringView path, mode_t mode, Custody& base, int options)
{
    auto custody = TRY(resolve_path(credentials, path, base, nullptr, options));
    return chmod(credentials, custody, mode);
}

ErrorOr<void> VirtualFileSystem::rename(Credentials const& credentials, StringView old_path, StringView new_path, Custody& base)
{
    RefPtr<Custody> old_parent_custody;
    auto old_custody = TRY(resolve_path(credentials, old_path, base, &old_parent_custody, O_NOFOLLOW_NOERROR));
    auto& old_inode = old_custody->inode();

    RefPtr<Custody> new_parent_custody;
    auto new_custody_or_error = resolve_path(credentials, new_path, base, &new_parent_custody);
    if (new_custody_or_error.is_error()) {
        if (new_custody_or_error.error().code() != ENOENT || !new_parent_custody)
            return new_custody_or_error.release_error();
    }

    if (!old_parent_custody || !new_parent_custody) {
        return EPERM;
    }

    if (!new_custody_or_error.is_error()) {
        auto& new_inode = new_custody_or_error.value()->inode();

        if (old_inode.index() != new_inode.index() && old_inode.is_directory() && new_inode.is_directory()) {
            size_t child_count = 0;
            TRY(new_inode.traverse_as_directory([&child_count](auto&) -> ErrorOr<void> {
                ++child_count;
                return {};
            }));
            if (child_count > 2)
                return ENOTEMPTY;
        }
    }

    auto& old_parent_inode = old_parent_custody->inode();
    auto& new_parent_inode = new_parent_custody->inode();

    if (&old_parent_inode.fs() != &new_parent_inode.fs())
        return EXDEV;

    for (auto* new_ancestor = new_parent_custody.ptr(); new_ancestor; new_ancestor = new_ancestor->parent()) {
        if (&old_inode == &new_ancestor->inode())
            return EDIRINTOSELF;
    }

    if (!new_parent_inode.metadata().may_write(credentials))
        return EACCES;

    if (!old_parent_inode.metadata().may_write(credentials))
        return EACCES;

    if (old_parent_inode.metadata().is_sticky()) {
        if (!credentials.is_superuser() && old_inode.metadata().uid != credentials.euid())
            return EACCES;
    }

    if (old_parent_custody->is_readonly() || new_parent_custody->is_readonly())
        return EROFS;

    auto old_basename = KLexicalPath::basename(old_path);
    if (old_basename.is_empty() || old_basename == "."sv || old_basename == ".."sv)
        return EINVAL;

    auto new_basename = KLexicalPath::basename(new_path);
    if (new_basename.is_empty() || new_basename == "."sv || new_basename == ".."sv)
        return EINVAL;

    if (old_basename == new_basename && old_parent_inode.index() == new_parent_inode.index())
        return {};

    if (!new_custody_or_error.is_error()) {
        auto& new_custody = *new_custody_or_error.value();
        auto& new_inode = new_custody.inode();
        // FIXME: Is this really correct? Check what other systems do.
        if (&new_inode == &old_inode)
            return {};
        if (new_parent_inode.metadata().is_sticky()) {
            if (!credentials.is_superuser() && new_inode.metadata().uid != credentials.euid())
                return EACCES;
        }
        if (new_inode.is_directory() && !old_inode.is_directory())
            return EISDIR;
        TRY(new_parent_inode.remove_child(new_basename));
    }

    TRY(new_parent_inode.add_child(old_inode, new_basename, old_inode.mode()));
    TRY(old_parent_inode.remove_child(old_basename));
    return {};
}

ErrorOr<void> VirtualFileSystem::chown(Credentials const& credentials, Custody& custody, UserID a_uid, GroupID a_gid)
{
    auto& inode = custody.inode();
    auto metadata = inode.metadata();

    if (credentials.euid() != metadata.uid && !credentials.is_superuser())
        return EPERM;

    UserID new_uid = metadata.uid;
    GroupID new_gid = metadata.gid;

    if (a_uid != (uid_t)-1) {
        if (credentials.euid() != a_uid && !credentials.is_superuser())
            return EPERM;
        new_uid = a_uid;
    }
    if (a_gid != (gid_t)-1) {
        if (!credentials.in_group(a_gid) && !credentials.is_superuser())
            return EPERM;
        new_gid = a_gid;
    }

    if (custody.is_readonly())
        return EROFS;

    dbgln_if(VFS_DEBUG, "VirtualFileSystem::chown(): inode {} <- uid={} gid={}", inode.identifier(), new_uid, new_gid);

    if (metadata.is_setuid() || metadata.is_setgid()) {
        dbgln_if(VFS_DEBUG, "VirtualFileSystem::chown(): Stripping SUID/SGID bits from {}", inode.identifier());
        TRY(inode.chmod(metadata.mode & ~(04000 | 02000)));
    }

    return inode.chown(new_uid, new_gid);
}

ErrorOr<void> VirtualFileSystem::chown(Credentials const& credentials, StringView path, UserID a_uid, GroupID a_gid, Custody& base, int options)
{
    auto custody = TRY(resolve_path(credentials, path, base, nullptr, options));
    return chown(credentials, custody, a_uid, a_gid);
}

static bool hard_link_allowed(Credentials const& credentials, Inode const& inode)
{
    auto metadata = inode.metadata();

    if (credentials.euid() == metadata.uid)
        return true;

    if (metadata.is_regular_file()
        && !metadata.is_setuid()
        && !(metadata.is_setgid() && metadata.mode & S_IXGRP)
        && metadata.may_write(credentials)) {
        return true;
    }

    return false;
}

ErrorOr<void> VirtualFileSystem::link(Credentials const& credentials, StringView old_path, StringView new_path, Custody& base)
{
    auto old_custody = TRY(resolve_path(credentials, old_path, base));
    auto& old_inode = old_custody->inode();

    RefPtr<Custody> parent_custody;
    auto new_custody_or_error = resolve_path(credentials, new_path, base, &parent_custody);
    if (!new_custody_or_error.is_error())
        return EEXIST;

    if (!parent_custody)
        return ENOENT;

    auto& parent_inode = parent_custody->inode();

    if (parent_inode.fsid() != old_inode.fsid())
        return EXDEV;

    if (!parent_inode.metadata().may_write(credentials))
        return EACCES;

    if (old_inode.is_directory())
        return EPERM;

    if (parent_custody->is_readonly())
        return EROFS;

    if (!hard_link_allowed(credentials, old_inode))
        return EPERM;

    return parent_inode.add_child(old_inode, KLexicalPath::basename(new_path), old_inode.mode());
}

ErrorOr<void> VirtualFileSystem::unlink(Credentials const& credentials, StringView path, Custody& base)
{
    RefPtr<Custody> parent_custody;
    auto custody = TRY(resolve_path(credentials, path, base, &parent_custody, O_NOFOLLOW_NOERROR | O_UNLINK_INTERNAL));
    auto& inode = custody->inode();

    if (inode.is_directory())
        return EISDIR;

    // We have just checked that the inode is not a directory, and thus it's not
    // the root. So it should have a parent. Note that this would be invalidated
    // if we were to support bind-mounting regular files on top of the root.
    VERIFY(parent_custody);

    auto& parent_inode = parent_custody->inode();
    if (!parent_inode.metadata().may_write(credentials))
        return EACCES;

    if (parent_inode.metadata().is_sticky()) {
        if (!credentials.is_superuser() && inode.metadata().uid != credentials.euid())
            return EACCES;
    }

    if (parent_custody->is_readonly())
        return EROFS;

    return parent_inode.remove_child(KLexicalPath::basename(path));
}

ErrorOr<void> VirtualFileSystem::symlink(Credentials const& credentials, StringView target, StringView linkpath, Custody& base)
{
    RefPtr<Custody> parent_custody;
    auto existing_custody_or_error = resolve_path(credentials, linkpath, base, &parent_custody);
    if (!existing_custody_or_error.is_error())
        return EEXIST;
    if (!parent_custody)
        return ENOENT;
    if (existing_custody_or_error.is_error() && existing_custody_or_error.error().code() != ENOENT)
        return existing_custody_or_error.release_error();
    auto& parent_inode = parent_custody->inode();
    if (!parent_inode.metadata().may_write(credentials))
        return EACCES;
    if (parent_custody->is_readonly())
        return EROFS;

    auto basename = KLexicalPath::basename(linkpath);
    dbgln_if(VFS_DEBUG, "VirtualFileSystem::symlink: '{}' (-> '{}') in {}", basename, target, parent_inode.identifier());

    auto inode = TRY(parent_inode.create_child(basename, S_IFLNK | 0644, 0, credentials.euid(), credentials.egid()));

    auto target_buffer = UserOrKernelBuffer::for_kernel_buffer(const_cast<u8*>((u8 const*)target.characters_without_null_termination()));
    TRY(inode->write_bytes(0, target.length(), target_buffer, nullptr));
    return {};
}

ErrorOr<void> VirtualFileSystem::rmdir(Credentials const& credentials, StringView path, Custody& base)
{
    RefPtr<Custody> parent_custody;
    auto custody = TRY(resolve_path(credentials, path, base, &parent_custody));
    auto& inode = custody->inode();

    // FIXME: We should return EINVAL if the last component of the path is "."
    // FIXME: We should return ENOTEMPTY if the last component of the path is ".."

    if (!inode.is_directory())
        return ENOTDIR;

    if (!parent_custody)
        return EBUSY;

    auto& parent_inode = parent_custody->inode();
    auto parent_metadata = parent_inode.metadata();

    if (!parent_metadata.may_write(credentials))
        return EACCES;

    if (parent_metadata.is_sticky()) {
        if (!credentials.is_superuser() && inode.metadata().uid != credentials.euid())
            return EACCES;
    }

    size_t child_count = 0;
    TRY(inode.traverse_as_directory([&child_count](auto&) -> ErrorOr<void> {
        ++child_count;
        return {};
    }));

    if (child_count != 2)
        return ENOTEMPTY;

    if (custody->is_readonly())
        return EROFS;

    TRY(inode.remove_child("."sv));
    TRY(inode.remove_child(".."sv));

    return parent_inode.remove_child(KLexicalPath::basename(path));
}

ErrorOr<void> VirtualFileSystem::for_each_mount(Function<ErrorOr<void>(Mount const&)> callback) const
{
    return m_mounts.with([&](auto& mounts) -> ErrorOr<void> {
        for (auto& mount : mounts)
            TRY(callback(*mount));
        return {};
    });
}

void VirtualFileSystem::sync()
{
    FileSystem::sync();
}

NonnullRefPtr<Custody> VirtualFileSystem::root_custody()
{
    return m_root_custody.with([](auto& root_custody) -> NonnullRefPtr<Custody> { return *root_custody; });
}

UnveilNode const& VirtualFileSystem::find_matching_unveiled_path(StringView path)
{
    auto& current_process = Process::current();
    VERIFY(current_process.veil_state() != VeilState::None);
    return current_process.unveil_data().with([&](auto const& unveil_data) -> UnveilNode const& {
        auto path_parts = KLexicalPath::parts(path);
        return unveil_data.paths.traverse_until_last_accessible_node(path_parts.begin(), path_parts.end());
    });
}

ErrorOr<void> VirtualFileSystem::validate_path_against_process_veil(Custody const& custody, int options)
{
    if (Process::current().veil_state() == VeilState::None)
        return {};
    auto absolute_path = TRY(custody.try_serialize_absolute_path());
    return validate_path_against_process_veil(absolute_path->view(), options);
}

ErrorOr<void> VirtualFileSystem::validate_path_against_process_veil(StringView path, int options)
{
    if (Process::current().veil_state() == VeilState::None)
        return {};
    if (options == O_EXEC && path == "/usr/lib/Loader.so")
        return {};

    VERIFY(path.starts_with('/'));
    VERIFY(!path.contains("/../"sv) && !path.ends_with("/.."sv));
    VERIFY(!path.contains("/./"sv) && !path.ends_with("/."sv));

#ifdef SKIP_PATH_VALIDATION_FOR_COVERAGE_INSTRUMENTATION
    // Skip veil validation against profile data when coverage is enabled for userspace
    // so that all processes can write out coverage data even with veils in place
    if (KLexicalPath::basename(path).ends_with(".profraw"sv))
        return {};
#endif

    auto& unveiled_path = find_matching_unveiled_path(path);
    if (unveiled_path.permissions() == UnveilAccess::None) {
        dbgln("Rejecting path '{}' since it hasn't been unveiled.", path);
        dump_backtrace();
        return ENOENT;
    }

    if (options & O_CREAT) {
        if (!(unveiled_path.permissions() & UnveilAccess::CreateOrRemove)) {
            dbgln("Rejecting path '{}' since it hasn't been unveiled with 'c' permission.", path);
            dump_backtrace();
            return EACCES;
        }
    }
    if (options & O_UNLINK_INTERNAL) {
        if (!(unveiled_path.permissions() & UnveilAccess::CreateOrRemove)) {
            dbgln("Rejecting path '{}' for unlink since it hasn't been unveiled with 'c' permission.", path);
            dump_backtrace();
            return EACCES;
        }
        return {};
    }
    if (options & O_RDONLY) {
        if (options & O_DIRECTORY) {
            if (!(unveiled_path.permissions() & (UnveilAccess::Read | UnveilAccess::Browse))) {
                dbgln("Rejecting path '{}' since it hasn't been unveiled with 'r' or 'b' permissions.", path);
                dump_backtrace();
                return EACCES;
            }
        } else {
            if (!(unveiled_path.permissions() & UnveilAccess::Read)) {
                dbgln("Rejecting path '{}' since it hasn't been unveiled with 'r' permission.", path);
                dump_backtrace();
                return EACCES;
            }
        }
    }
    if (options & O_WRONLY) {
        if (!(unveiled_path.permissions() & UnveilAccess::Write)) {
            dbgln("Rejecting path '{}' since it hasn't been unveiled with 'w' permission.", path);
            dump_backtrace();
            return EACCES;
        }
    }
    if (options & O_EXEC) {
        if (!(unveiled_path.permissions() & UnveilAccess::Execute)) {
            dbgln("Rejecting path '{}' since it hasn't been unveiled with 'x' permission.", path);
            dump_backtrace();
            return EACCES;
        }
    }
    return {};
}

ErrorOr<NonnullRefPtr<Custody>> VirtualFileSystem::resolve_path(Credentials const& credentials, StringView path, NonnullRefPtr<Custody> base, RefPtr<Custody>* out_parent, int options, int symlink_recursion_level)
{
    // FIXME: The errors returned by resolve_path_without_veil can leak information about paths that are not unveiled,
    //        e.g. when the error is EACCESS or similar.
    auto custody = TRY(resolve_path_without_veil(credentials, path, base, out_parent, options, symlink_recursion_level));
    if (auto result = validate_path_against_process_veil(*custody, options); result.is_error()) {
        if (out_parent)
            out_parent->clear();
        return result.release_error();
    }
    return custody;
}

static bool safe_to_follow_symlink(Credentials const& credentials, Inode const& inode, InodeMetadata const& parent_metadata)
{
    auto metadata = inode.metadata();
    if (credentials.euid() == metadata.uid)
        return true;

    if (!(parent_metadata.is_sticky() && parent_metadata.mode & S_IWOTH))
        return true;

    if (metadata.uid == parent_metadata.uid)
        return true;

    return false;
}

ErrorOr<NonnullRefPtr<Custody>> VirtualFileSystem::resolve_path_without_veil(Credentials const& credentials, StringView path, NonnullRefPtr<Custody> base, RefPtr<Custody>* out_parent, int options, int symlink_recursion_level)
{
    if (symlink_recursion_level >= symlink_recursion_limit)
        return ELOOP;

    if (path.is_empty())
        return EINVAL;

    GenericLexer path_lexer(path);

    NonnullRefPtr<Custody> custody = path[0] == '/' ? root_custody() : base;
    bool extra_iteration = path[path.length() - 1] == '/';

    while (!path_lexer.is_eof() || extra_iteration) {
        if (path_lexer.is_eof())
            extra_iteration = false;
        auto part = path_lexer.consume_until('/');
        path_lexer.ignore();

        Custody& parent = custody;
        auto parent_metadata = parent.inode().metadata();
        if (!parent_metadata.is_directory())
            return ENOTDIR;
        // Ensure the current user is allowed to resolve paths inside this directory.
        if (!parent_metadata.may_execute(credentials))
            return EACCES;

        bool have_more_parts = !path_lexer.is_eof() || extra_iteration;

        if (part == "..") {
            // If we encounter a "..", take a step back, but don't go beyond the root.
            if (custody->parent())
                custody = *custody->parent();
            continue;
        } else if (part == "." || part.is_empty()) {
            continue;
        }

        // Okay, let's look up this part.
        auto child_or_error = parent.inode().lookup(part);
        if (child_or_error.is_error()) {
            if (out_parent) {
                // ENOENT with a non-null parent custody signals to caller that
                // we found the immediate parent of the file, but the file itself
                // does not exist yet.
                *out_parent = have_more_parts ? nullptr : &parent;
            }
            return child_or_error.release_error();
        }
        auto child_inode = child_or_error.release_value();

        int mount_flags_for_child = parent.mount_flags();

        // See if there's something mounted on the child; in that case
        // we would need to return the guest inode, not the host inode.
        if (auto mount = find_mount_for_host(child_inode->identifier())) {
            child_inode = mount->guest();
            mount_flags_for_child = mount->flags();
        }

        custody = TRY(Custody::try_create(&parent, part, *child_inode, mount_flags_for_child));

        if (child_inode->metadata().is_symlink()) {
            if (!have_more_parts) {
                if (options & O_NOFOLLOW)
                    return ELOOP;
                if (options & O_NOFOLLOW_NOERROR)
                    break;
            }

            if (!safe_to_follow_symlink(credentials, *child_inode, parent_metadata))
                return EACCES;

            TRY(validate_path_against_process_veil(*custody, options));

            auto symlink_target = TRY(child_inode->resolve_as_link(credentials, parent, out_parent, options, symlink_recursion_level + 1));
            if (!have_more_parts)
                return symlink_target;

            // Now, resolve the remaining path relative to the symlink target.
            // We prepend a "." to it to ensure that it's not empty and that
            // any initial slashes it might have get interpreted properly.
            StringBuilder remaining_path;
            TRY(remaining_path.try_append('.'));
            TRY(remaining_path.try_append(path.substring_view_starting_after_substring(part)));

            return resolve_path_without_veil(credentials, remaining_path.string_view(), symlink_target, out_parent, options, symlink_recursion_level + 1);
        }
    }

    if (out_parent)
        *out_parent = custody->parent();
    return custody;
}
}
