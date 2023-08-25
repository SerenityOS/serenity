/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022-2023, Liav A. <liavalb@hotmail.co.il>
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
#include <Kernel/KSyms.h>
#include <Kernel/Library/KLexicalPath.h>
#include <Kernel/Sections.h>
#include <Kernel/Tasks/Process.h>

#include <Kernel/FileSystem/DevPtsFS/FileSystem.h>
#include <Kernel/FileSystem/Ext2FS/FileSystem.h>
#include <Kernel/FileSystem/FATFS/FileSystem.h>
#include <Kernel/FileSystem/ISO9660FS/FileSystem.h>
#include <Kernel/FileSystem/Plan9FS/FileSystem.h>
#include <Kernel/FileSystem/ProcFS/FileSystem.h>
#include <Kernel/FileSystem/RAMFS/FileSystem.h>
#include <Kernel/FileSystem/SysFS/FileSystem.h>

namespace Kernel {

static Singleton<VirtualFileSystem> s_the;
static constexpr int root_mount_flags = 0;

static ErrorOr<void> handle_mount_boolean_flag_as_invalid(Span<u8>, StringView, bool)
{
    return EINVAL;
}

static ErrorOr<void> handle_mount_unsigned_integer_flag_as_invalid(Span<u8>, StringView, u64)
{
    return EINVAL;
}

static ErrorOr<void> handle_mount_signed_integer_flag_as_invalid(Span<u8>, StringView, i64)
{
    return EINVAL;
}

static ErrorOr<void> handle_mount_ascii_string_flag_as_invalid(Span<u8>, StringView, StringView)
{
    return EINVAL;
}

static constexpr FileSystemInitializer s_initializers[] = {
    { "proc"sv, "ProcFS"sv, false, false, false, {}, ProcFS::try_create, handle_mount_boolean_flag_as_invalid, handle_mount_unsigned_integer_flag_as_invalid, handle_mount_signed_integer_flag_as_invalid, handle_mount_ascii_string_flag_as_invalid },
    { "devpts"sv, "DevPtsFS"sv, false, false, false, {}, DevPtsFS::try_create, handle_mount_boolean_flag_as_invalid, handle_mount_unsigned_integer_flag_as_invalid, handle_mount_signed_integer_flag_as_invalid, handle_mount_ascii_string_flag_as_invalid },
    { "sys"sv, "SysFS"sv, false, false, false, {}, SysFS::try_create, handle_mount_boolean_flag_as_invalid, handle_mount_unsigned_integer_flag_as_invalid, handle_mount_signed_integer_flag_as_invalid, handle_mount_ascii_string_flag_as_invalid },
    { "ram"sv, "RAMFS"sv, false, false, false, {}, RAMFS::try_create, handle_mount_boolean_flag_as_invalid, handle_mount_unsigned_integer_flag_as_invalid, handle_mount_signed_integer_flag_as_invalid, handle_mount_ascii_string_flag_as_invalid },
    { "ext2"sv, "Ext2FS"sv, true, true, true, Ext2FS::try_create, {}, handle_mount_boolean_flag_as_invalid, handle_mount_unsigned_integer_flag_as_invalid, handle_mount_signed_integer_flag_as_invalid, handle_mount_ascii_string_flag_as_invalid },
    { "9p"sv, "Plan9FS"sv, true, true, true, Plan9FS::try_create, {}, handle_mount_boolean_flag_as_invalid, handle_mount_unsigned_integer_flag_as_invalid, handle_mount_signed_integer_flag_as_invalid, handle_mount_ascii_string_flag_as_invalid },
    { "iso9660"sv, "ISO9660FS"sv, true, true, true, ISO9660FS::try_create, {}, handle_mount_boolean_flag_as_invalid, handle_mount_unsigned_integer_flag_as_invalid, handle_mount_signed_integer_flag_as_invalid, handle_mount_ascii_string_flag_as_invalid },
    { "fat"sv, "FATFS"sv, true, true, true, FATFS::try_create, {}, handle_mount_boolean_flag_as_invalid, handle_mount_unsigned_integer_flag_as_invalid, handle_mount_signed_integer_flag_as_invalid, handle_mount_ascii_string_flag_as_invalid },
};

ErrorOr<FileSystemInitializer const*> VirtualFileSystem::find_filesystem_type_initializer(StringView fs_type)
{
    for (auto& initializer_entry : s_initializers) {
        if (fs_type == initializer_entry.short_name || fs_type == initializer_entry.name)
            return &initializer_entry;
    }
    return ENODEV;
}

UNMAP_AFTER_INIT void VirtualFileSystem::initialize()
{
    s_the.ensure_instance();
}

VirtualFileSystem& VirtualFileSystem::the()
{
    return *s_the;
}

UNMAP_AFTER_INIT VirtualFileSystem::VirtualFileSystem()
{
}

UNMAP_AFTER_INIT VirtualFileSystem::~VirtualFileSystem() = default;

bool VirtualFileSystem::check_matching_absolute_path_hierarchy(Custody const& first_custody, Custody const& second_custody)
{
    // Are both custodies the root mount?
    if (!first_custody.parent() && !second_custody.parent())
        return true;
    if (first_custody.name() != second_custody.name())
        return false;
    auto const* custody1 = &first_custody;
    auto const* custody2 = &second_custody;
    while (custody1->parent()) {
        if (!custody2->parent())
            return false;
        if (custody1->parent().ptr() != custody2->parent().ptr())
            return false;
        custody1 = custody1->parent();
        custody2 = custody2->parent();
    }
    return true;
}

bool VirtualFileSystem::mount_point_exists_at_custody(Custody& mount_point)
{
    return m_mounts.with([&](auto& mounts) -> bool {
        return any_of(mounts, [&mount_point](auto const& existing_mount) {
            return existing_mount.host_custody() && check_matching_absolute_path_hierarchy(*existing_mount.host_custody(), mount_point);
        });
    });
}

ErrorOr<void> VirtualFileSystem::add_file_system_to_mount_table(FileSystem& file_system, Custody& mount_point, int flags)
{
    auto new_mount = TRY(adopt_nonnull_own_or_enomem(new (nothrow) Mount(file_system, &mount_point, flags)));
    return m_mounts.with([&](auto& mounts) -> ErrorOr<void> {
        auto& mount_point_inode = mount_point.inode();
        dbgln("VirtualFileSystem: FileSystemID {}, Mounting {} at inode {} with flags {}",
            file_system.fsid(),
            file_system.class_name(),
            mount_point_inode.identifier(),
            flags);
        if (mount_point_exists_at_custody(mount_point)) {
            dbgln("VirtualFileSystem: Mounting unsuccessful - inode {} is already a mount-point.", mount_point_inode.identifier());
            return EBUSY;
        }
        // Note: Actually add a mount for the filesystem and increment the filesystem mounted count
        new_mount->guest_fs().mounted_count({}).with([&](auto& mounted_count) {
            mounted_count++;

            // When this is the first time this FileSystem is mounted,
            // begin managing the FileSystem by adding it to the list of
            // managed file systems. This is symmetric with
            // VirtualFileSystem::unmount()'s `remove()` calls (which remove
            // the FileSystem once it is no longer mounted).
            if (mounted_count == 1) {
                m_file_systems_list.with([&](auto& fs_list) {
                    fs_list.append(file_system);
                });
            }
        });

        // NOTE: Leak the mount pointer so it can be added to the mount list, but it won't be
        // deleted after being added.
        mounts.append(*new_mount.leak_ptr());
        return {};
    });
}

ErrorOr<void> VirtualFileSystem::mount(MountFile& mount_file, OpenFileDescription* source_description, Custody& mount_point, int flags)
{
    auto const& file_system_initializer = mount_file.file_system_initializer();
    if (!source_description) {
        if (file_system_initializer.requires_open_file_description)
            return ENOTSUP;
        if (!file_system_initializer.create)
            return ENOTSUP;
        RefPtr<FileSystem> fs;
        TRY(mount_file.mount_file_system_specific_data().with_exclusive([&](auto& mount_specific_data) -> ErrorOr<void> {
            fs = TRY(file_system_initializer.create(mount_specific_data->bytes()));
            return {};
        }));
        VERIFY(fs);
        TRY(fs->initialize());
        TRY(add_file_system_to_mount_table(*fs, mount_point, flags));
        return {};
    }

    // NOTE: Although it might be OK to support creating filesystems
    // without providing an actual file descriptor to their create() method
    // because the caller of this function actually supplied a valid file descriptor,
    // this will only make things complicated in the future, so we should block
    // this kind of behavior.
    if (!file_system_initializer.requires_open_file_description)
        return ENOTSUP;

    if (file_system_initializer.requires_block_device && !source_description->file().is_block_device())
        return ENOTBLK;
    if (file_system_initializer.requires_seekable_file && !source_description->file().is_seekable()) {
        dbgln("mount: this is not a seekable file");
        return ENODEV;
    }

    // NOTE: If there's an associated file description with the filesystem, we could
    // try to first find it from the VirtualFileSystem filesystem list and if it was not found,
    // then create it and add it.
    VERIFY(file_system_initializer.create_with_fd);
    return m_file_backed_file_systems_list.with_exclusive([&](auto& list) -> ErrorOr<void> {
        RefPtr<FileSystem> fs;
        for (auto& node : list) {
            if ((&node.file_description() == source_description) || (&node.file() == &source_description->file())) {
                fs = node;
                break;
            }
        }
        if (!fs) {
            TRY(mount_file.mount_file_system_specific_data().with_exclusive([&](auto& mount_specific_data) -> ErrorOr<void> {
                fs = TRY(file_system_initializer.create_with_fd(*source_description, mount_specific_data->bytes()));
                return {};
            }));
            TRY(fs->initialize());
        }
        TRY(add_file_system_to_mount_table(*fs, mount_point, flags));
        list.append(static_cast<FileBackedFileSystem&>(*fs));
        return {};
    });
}

ErrorOr<void> VirtualFileSystem::bind_mount(Custody& source, Custody& mount_point, int flags)
{
    auto new_mount = TRY(adopt_nonnull_own_or_enomem(new (nothrow) Mount(source.inode(), mount_point, flags)));
    return m_mounts.with([&](auto& mounts) -> ErrorOr<void> {
        auto& inode = mount_point.inode();
        dbgln("VirtualFileSystem: Bind-mounting inode {} at inode {}", source.inode().identifier(), inode.identifier());
        if (mount_point_exists_at_custody(mount_point)) {
            dbgln("VirtualFileSystem: Bind-mounting unsuccessful - inode {} is already a mount-point.",
                mount_point.inode().identifier());
            return EBUSY;
        }

        // A bind mount also counts as a normal mount from the perspective of unmount(),
        // so we need to keep track of it in order for prepare_to_clear_last_mount() to work properly.
        new_mount->guest_fs().mounted_count({}).with([&](auto& count) { count++; });
        // NOTE: Leak the mount pointer so it can be added to the mount list, but it won't be
        // deleted after being added.
        mounts.append(*new_mount.leak_ptr());
        return {};
    });
}

ErrorOr<void> VirtualFileSystem::remount(Custody& mount_point, int new_flags)
{
    dbgln("VirtualFileSystem: Remounting inode {}", mount_point.inode().identifier());

    TRY(apply_to_mount_for_host_custody(mount_point, [new_flags](auto& mount) {
        mount.set_flags(new_flags);
    }));
    return {};
}

void VirtualFileSystem::sync_filesystems()
{
    Vector<NonnullRefPtr<FileSystem>, 32> file_systems;
    m_file_systems_list.with([&](auto const& list) {
        for (auto& fs : list)
            file_systems.append(fs);
    });

    for (auto& fs : file_systems) {
        auto result = fs->flush_writes();
        if (result.is_error()) {
            // TODO: Figure out how to propagate error to a higher function.
        }
    }
}

void VirtualFileSystem::lock_all_filesystems()
{
    Vector<NonnullRefPtr<FileSystem>, 32> file_systems;
    m_file_systems_list.with([&](auto const& list) {
        for (auto& fs : list)
            file_systems.append(fs);
    });

    for (auto& fs : file_systems)
        fs->m_lock.lock();
}

ErrorOr<void> VirtualFileSystem::unmount(Custody& mountpoint_custody)
{
    auto& guest_inode = mountpoint_custody.inode();
    auto custody_path = TRY(mountpoint_custody.try_serialize_absolute_path());
    return unmount(guest_inode, custody_path->view());
}

ErrorOr<void> VirtualFileSystem::unmount(Inode& guest_inode, StringView custody_path)
{
    return m_file_backed_file_systems_list.with_exclusive([&](auto& file_backed_fs_list) -> ErrorOr<void> {
        TRY(m_mounts.with([&](auto& mounts) -> ErrorOr<void> {
            for (auto& mount : mounts) {
                if (&mount.guest() != &guest_inode)
                    continue;
                auto mountpoint_path = TRY(mount.absolute_path());
                if (custody_path != mountpoint_path->view())
                    continue;
                NonnullRefPtr<FileSystem> fs = mount.guest_fs();
                TRY(fs->prepare_to_unmount(mount.guest()));
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
                            file_backed_fs_list.remove(file_backed_fs);
                        }
                    } else {
                        mounted_count--;
                    }
                });
                dbgln("VirtualFileSystem: Unmounting file system {}...", fs->fsid());
                mount.m_vfs_list_node.remove();
                // NOTE: This is balanced by a `new` statement that is happening in various places before inserting the Mount object to the list.
                delete &mount;
                return {};
            }
            dbgln("VirtualFileSystem: Nothing mounted on inode {}", guest_inode.identifier());
            return ENODEV;
        }));
        return {};
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
        m_file_backed_file_systems_list.with_exclusive([&](auto& list) {
            list.append(static_cast<FileBackedFileSystem&>(fs));
        });
    } else {
        dmesgln("VirtualFileSystem: mounted root({}) from {}", fs.fsid(), fs.class_name());
    }

    m_file_systems_list.with([&](auto& fs_list) {
        fs_list.append(fs);
    });

    fs.mounted_count({}).with([&](auto& mounted_count) {
        mounted_count++;
    });

    // Note: Actually add a mount for the filesystem and increment the filesystem mounted count
    m_mounts.with([&](auto& mounts) {
        // NOTE: Leak the mount pointer so it can be added to the mount list, but it won't be
        // deleted after being added.
        mounts.append(*new_mount.leak_ptr());
    });

    RefPtr<Custody> new_root_custody = TRY(Custody::try_create(nullptr, ""sv, *m_root_inode, root_mount_flags));
    m_root_custody.with([&](auto& root_custody) {
        swap(root_custody, new_root_custody);
    });
    return {};
}

ErrorOr<void> VirtualFileSystem::apply_to_mount_for_host_custody(Custody const& current_custody, Function<void(Mount&)> callback)
{
    return m_mounts.with([&](auto& mounts) -> ErrorOr<void> {
        // NOTE: We either search for the root mount or for a mount that has a parent custody!
        if (!current_custody.parent()) {
            for (auto& mount : mounts) {
                if (!mount.host_custody()) {
                    callback(mount);
                    return {};
                }
            }
            // NOTE: There must be a root mount entry, so fail if we don't find it.
            VERIFY_NOT_REACHED();
        } else {
            for (auto& mount : mounts) {
                if (mount.host_custody() && check_matching_absolute_path_hierarchy(*mount.host_custody(), current_custody)) {
                    callback(mount);
                    return {};
                }
            }
        }
        return Error::from_errno(ENODEV);
    });
}

ErrorOr<void> VirtualFileSystem::traverse_directory_inode(Inode& dir_inode, Function<ErrorOr<void>(FileSystem::DirectoryEntryView const&)> callback)
{
    return dir_inode.traverse_as_directory([&](auto& entry) -> ErrorOr<void> {
        TRY(callback({ entry.name, entry.inode, entry.file_type }));
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

    TRY(inode.update_timestamps(UnixDateTime::from_seconds_since_epoch(atime), {}, UnixDateTime::from_seconds_since_epoch(mtime)));
    return {};
}

ErrorOr<void> VirtualFileSystem::utimensat(Credentials const& credentials, StringView path, Custody& base, timespec const& atime, timespec const& mtime, int options)
{
    auto custody = TRY(resolve_path(credentials, path, base, nullptr, options));
    return do_utimens(credentials, custody, atime, mtime);
}

ErrorOr<void> VirtualFileSystem::do_utimens(Credentials const& credentials, Custody& custody, timespec const& atime, timespec const& mtime)
{
    auto& inode = custody.inode();
    if (!credentials.is_superuser() && inode.metadata().uid != credentials.euid())
        return EACCES;
    if (custody.is_readonly())
        return EROFS;

    // NOTE: A standard ext2 inode cannot store nanosecond timestamps.
    TRY(inode.update_timestamps(
        (atime.tv_nsec != UTIME_OMIT) ? UnixDateTime::from_unix_timespec(atime) : Optional<UnixDateTime> {},
        {},
        (mtime.tv_nsec != UTIME_OMIT) ? UnixDateTime::from_unix_timespec(mtime) : Optional<UnixDateTime> {}));

    return {};
}

ErrorOr<InodeMetadata> VirtualFileSystem::lookup_metadata(Credentials const& credentials, StringView path, Custody& base, int options)
{
    auto custody = TRY(resolve_path(credentials, path, base, nullptr, options));
    return custody->inode().metadata();
}

ErrorOr<NonnullRefPtr<OpenFileDescription>> VirtualFileSystem::open(Credentials const& credentials, StringView path, int options, mode_t mode, Custody& base, Optional<UidAndGid> owner)
{
    return open(Process::current(), credentials, path, options, mode, base, owner);
}

ErrorOr<NonnullRefPtr<OpenFileDescription>> VirtualFileSystem::open(Process const& process, Credentials const& credentials, StringView path, int options, mode_t mode, Custody& base, Optional<UidAndGid> owner)
{
    if ((options & O_CREAT) && (options & O_DIRECTORY))
        return EINVAL;

    RefPtr<Custody> parent_custody;
    auto custody_or_error = resolve_path(process, credentials, path, base, &parent_custody, options);
    if (custody_or_error.is_error()) {
        // NOTE: ENOENT with a non-null parent custody signals us that the immediate parent
        //       of the file exists, but the file itself does not.
        if ((options & O_CREAT) && custody_or_error.error().code() == ENOENT && parent_custody)
            return create(process, credentials, path, options, mode, *parent_custody, move(owner));
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
        TRY(inode.update_timestamps({}, {}, kgettimeofday()));
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

ErrorOr<NonnullRefPtr<OpenFileDescription>> VirtualFileSystem::create(Credentials const& credentials, StringView path, int options, mode_t mode, Custody& parent_custody, Optional<UidAndGid> owner)
{
    return create(Process::current(), credentials, path, options, mode, parent_custody, owner);
}

ErrorOr<NonnullRefPtr<OpenFileDescription>> VirtualFileSystem::create(Process const& process, Credentials const& credentials, StringView path, int options, mode_t mode, Custody& parent_custody, Optional<UidAndGid> owner)
{
    auto basename = KLexicalPath::basename(path);
    auto parent_path = TRY(parent_custody.try_serialize_absolute_path());
    auto full_path = TRY(KLexicalPath::try_join(parent_path->view(), basename));
    TRY(validate_path_against_process_veil(process, full_path->view(), options));

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

ErrorOr<void> VirtualFileSystem::access(Credentials const& credentials, StringView path, int mode, Custody& base, AccessFlags access_flags)
{
    auto should_follow_symlinks = !has_flag(access_flags, AccessFlags::DoNotFollowSymlinks);
    auto custody = TRY(resolve_path(credentials, path, base, nullptr, should_follow_symlinks ? 0 : O_NOFOLLOW_NOERROR));

    auto& inode = custody->inode();
    auto metadata = inode.metadata();
    auto use_effective_ids = has_flag(access_flags, AccessFlags::EffectiveAccess) ? UseEffectiveIDs::Yes : UseEffectiveIDs::No;
    if (mode & R_OK) {
        if (!metadata.may_read(credentials, use_effective_ids))
            return EACCES;
    }
    if (mode & W_OK) {
        if (!metadata.may_write(credentials, use_effective_ids))
            return EACCES;
        if (custody->is_readonly())
            return EROFS;
    }
    if (mode & X_OK) {
        if (!metadata.may_execute(credentials, use_effective_ids))
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

ErrorOr<void> VirtualFileSystem::rename(Credentials const& credentials, Custody& old_base, StringView old_path, Custody& new_base, StringView new_path)
{
    RefPtr<Custody> old_parent_custody;
    auto old_custody = TRY(resolve_path(credentials, old_path, old_base, &old_parent_custody, O_NOFOLLOW_NOERROR));
    auto& old_inode = old_custody->inode();

    RefPtr<Custody> new_parent_custody;
    auto new_custody_or_error = resolve_path(credentials, new_path, new_base, &new_parent_custody);
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
        if (!credentials.is_superuser() && old_parent_inode.metadata().uid != credentials.euid() && old_inode.metadata().uid != credentials.euid())
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
        // When the source/dest inodes are the same (in other words,
        // when `old_path` and `new_path` are the same), perform a no-op
        // and return success.
        // Linux (`vfs_rename()`) and OpenBSD (`dorenameat()`) appear to have
        // this same no-op behavior.
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

    // If the inode that we moved is a directory and we changed parent
    // directories, then we also have to make .. point to the new parent inode,
    // because .. is its own inode.
    if (old_inode.is_directory() && old_parent_inode.index() != new_parent_inode.index()) {
        TRY(old_inode.replace_child(".."sv, new_parent_inode));
    }

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
    // NOTE: To prevent unveil bypass by creating an hardlink after unveiling a path as read-only,
    // check that if write permission is allowed by the veil info on the old_path.
    auto old_custody = TRY(resolve_path(credentials, old_path, base, nullptr, O_RDWR));
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
    auto custody = TRY(resolve_path(credentials, path, base, &parent_custody, O_WRONLY | O_NOFOLLOW_NOERROR | O_UNLINK_INTERNAL));
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
        if (!credentials.is_superuser() && parent_inode.metadata().uid != credentials.euid() && inode.metadata().uid != credentials.euid())
            return EACCES;
    }

    if (parent_custody->is_readonly())
        return EROFS;

    return parent_inode.remove_child(KLexicalPath::basename(path));
}

ErrorOr<void> VirtualFileSystem::symlink(Credentials const& credentials, StringView target, StringView linkpath, Custody& base)
{
    // NOTE: Check that the actual target (if it exists right now) is unveiled and prevent creating symlinks on veiled paths!
    if (auto target_custody_or_error = resolve_path_without_veil(credentials, target, base, nullptr, O_RDWR, 0); !target_custody_or_error.is_error()) {
        auto target_custody = target_custody_or_error.release_value();
        TRY(validate_path_against_process_veil(*target_custody, O_RDWR));
    }

    RefPtr<Custody> parent_custody;
    auto existing_custody_or_error = resolve_path(credentials, linkpath, base, &parent_custody, O_RDWR);
    if (!existing_custody_or_error.is_error())
        return EEXIST;
    if (!parent_custody)
        return ENOENT;

    // NOTE: VERY IMPORTANT! We prevent creating symlinks in case the program didn't unveil the parent_custody
    // path! For example, say the program wanted to create a symlink in /tmp/symlink to /tmp/test/pointee_symlink
    // and unveiled the /tmp/test/ directory path beforehand, but not the /tmp directory path - the symlink syscall will
    // fail here because we can't create the symlink in a parent directory path we didn't unveil beforehand.
    TRY(validate_path_against_process_veil(*parent_custody, O_RDWR));

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

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/rmdir.html
ErrorOr<void> VirtualFileSystem::rmdir(Credentials const& credentials, StringView path, Custody& base)
{
    RefPtr<Custody> parent_custody;
    auto custody = TRY(resolve_path(credentials, path, base, &parent_custody, O_CREAT));
    auto& inode = custody->inode();

    auto last_component = KLexicalPath::basename(path);

    // [EINVAL] The path argument contains a last component that is dot.
    if (last_component == "."sv)
        return EINVAL;

    // [ENOTDIR] A component of path names an existing file that is neither a directory
    //           nor a symbolic link to a directory.
    if (!inode.is_directory())
        return ENOTDIR;

    // [EBUSY] The directory to be removed is currently in use by the system or some process
    //         and the implementation considers this to be an error.
    // NOTE: If there is no parent, that means we're trying to rmdir the root directory!
    if (!parent_custody)
        return EBUSY;

    auto& parent_inode = parent_custody->inode();
    auto parent_metadata = parent_inode.metadata();

    // [EACCES] Search permission is denied on a component of the path prefix,
    //          or write permission is denied on the parent directory of the directory to be removed.
    if (!parent_metadata.may_write(credentials))
        return EACCES;

    if (parent_metadata.is_sticky()) {
        // [EACCES] The S_ISVTX flag is set on the directory containing the file referred to by the path argument
        //          and the process does not satisfy the criteria specified in XBD Directory Protection.
        if (!credentials.is_superuser()
            && inode.metadata().uid != credentials.euid()
            && parent_metadata.uid != credentials.euid()) {
            return EACCES;
        }
    }

    size_t child_count = 0;
    TRY(inode.traverse_as_directory([&child_count](auto&) -> ErrorOr<void> {
        ++child_count;
        return {};
    }));

    // [ENOTEMPTY] The path argument names a directory that is not an empty directory,
    //             or there are hard links to the directory other than dot or a single entry in dot-dot.
    if (child_count != 2)
        return ENOTEMPTY;

    // [EROFS] The directory entry to be removed resides on a read-only file system.
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
            TRY(callback(mount));
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

UnveilNode const& VirtualFileSystem::find_matching_unveiled_path(Process const& process, StringView path)
{
    VERIFY(process.veil_state() != VeilState::None);
    return process.unveil_data().with([&](auto const& unveil_data) -> UnveilNode const& {
        auto path_parts = KLexicalPath::parts(path);
        return unveil_data.paths.traverse_until_last_accessible_node(path_parts.begin(), path_parts.end());
    });
}

ErrorOr<void> VirtualFileSystem::validate_path_against_process_veil(Custody const& custody, int options)
{
    return validate_path_against_process_veil(Process::current(), custody, options);
}

ErrorOr<void> VirtualFileSystem::validate_path_against_process_veil(Process const& process, Custody const& custody, int options)
{
    if (process.veil_state() == VeilState::None)
        return {};
    auto absolute_path = TRY(custody.try_serialize_absolute_path());
    return validate_path_against_process_veil(process, absolute_path->view(), options);
}

ErrorOr<void> VirtualFileSystem::validate_path_against_process_veil(Process const& process, StringView path, int options)
{
    if (process.veil_state() == VeilState::None)
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

    auto log_veiled_path = [&](Optional<StringView> const& with_permissions = {}) {
        if (with_permissions.has_value())
            dbgln("\033[31;1mRejecting path '{}' because it hasn't been unveiled with {} permissions\033[0m", path, *with_permissions);
        else
            dbgln("\033[31;1mRejecting path '{}' because it hasn't been unveiled\033[0m", path);

        dump_backtrace();
    };

    auto& unveiled_path = find_matching_unveiled_path(process, path);
    if (unveiled_path.permissions() == UnveilAccess::None) {
        log_veiled_path();
        return ENOENT;
    }

    if (options & O_CREAT) {
        if (!(unveiled_path.permissions() & UnveilAccess::CreateOrRemove)) {
            log_veiled_path("'c'"sv);
            return EACCES;
        }
    }
    if (options & O_UNLINK_INTERNAL) {
        if (!(unveiled_path.permissions() & UnveilAccess::CreateOrRemove)) {
            log_veiled_path("'c'"sv);
            return EACCES;
        }
        return {};
    }
    if (options & O_RDONLY) {
        if (options & O_DIRECTORY) {
            if (!(unveiled_path.permissions() & (UnveilAccess::Read | UnveilAccess::Browse))) {
                log_veiled_path("'r' or 'b'"sv);
                return EACCES;
            }
        } else {
            if (!(unveiled_path.permissions() & UnveilAccess::Read)) {
                log_veiled_path("'r'"sv);
                return EACCES;
            }
        }
    }
    if (options & O_WRONLY) {
        if (!(unveiled_path.permissions() & UnveilAccess::Write)) {
            log_veiled_path("'w'"sv);
            return EACCES;
        }
    }
    if (options & O_EXEC) {
        if (!(unveiled_path.permissions() & UnveilAccess::Execute)) {
            log_veiled_path("'x'"sv);
            return EACCES;
        }
    }
    return {};
}

ErrorOr<void> VirtualFileSystem::validate_path_against_process_veil(StringView path, int options)
{
    return validate_path_against_process_veil(Process::current(), path, options);
}

ErrorOr<NonnullRefPtr<Custody>> VirtualFileSystem::resolve_path(Credentials const& credentials, StringView path, NonnullRefPtr<Custody> base, RefPtr<Custody>* out_parent, int options, int symlink_recursion_level)
{
    return resolve_path(Process::current(), credentials, path, base, out_parent, options, symlink_recursion_level);
}

ErrorOr<NonnullRefPtr<Custody>> VirtualFileSystem::resolve_path(Process const& process, Credentials const& credentials, StringView path, NonnullRefPtr<Custody> base, RefPtr<Custody>* out_parent, int options, int symlink_recursion_level)
{
    // FIXME: The errors returned by resolve_path_without_veil can leak information about paths that are not unveiled,
    //        e.g. when the error is EACCESS or similar.
    auto custody = TRY(resolve_path_without_veil(credentials, path, base, out_parent, options, symlink_recursion_level));
    if (auto result = validate_path_against_process_veil(process, *custody, options); result.is_error()) {
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

        auto current_custody = TRY(Custody::try_create(&parent, part, *child_inode, mount_flags_for_child));

        // See if there's something mounted on the child; in that case
        // we would need to return the guest inode, not the host inode.
        auto found_mount_or_error = apply_to_mount_for_host_custody(current_custody, [&child_inode, &mount_flags_for_child](auto& mount) {
            child_inode = mount.guest();
            mount_flags_for_child = mount.flags();
        });
        if (!found_mount_or_error.is_error()) {
            custody = TRY(Custody::try_create(&parent, part, *child_inode, mount_flags_for_child));
        } else {
            custody = current_custody;
        }

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
