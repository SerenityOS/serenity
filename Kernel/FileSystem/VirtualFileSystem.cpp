/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022-2024, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/AnyOf.h>
#include <AK/GenericLexer.h>
#include <AK/RefPtr.h>
#include <AK/Singleton.h>
#include <AK/StringBuilder.h>
#include <Kernel/API/DeviceFileTypes.h>
#include <Kernel/API/POSIX/errno.h>
#include <Kernel/Debug.h>
#include <Kernel/Devices/BlockDevice.h>
#include <Kernel/Devices/Device.h>
#include <Kernel/Devices/Loop/LoopDevice.h>
#include <Kernel/FileSystem/Custody.h>
#include <Kernel/FileSystem/FileBackedFileSystem.h>
#include <Kernel/FileSystem/FileSystem.h>
#include <Kernel/FileSystem/OpenFileDescription.h>
#include <Kernel/FileSystem/VirtualFileSystem.h>
#include <Kernel/KSyms.h>
#include <Kernel/Library/KLexicalPath.h>
#include <Kernel/Sections.h>
#include <Kernel/Tasks/Process.h>

#include <Kernel/FileSystem/DevLoopFS/FileSystem.h>
#include <Kernel/FileSystem/DevPtsFS/FileSystem.h>
#include <Kernel/FileSystem/Ext2FS/FileSystem.h>
#include <Kernel/FileSystem/FATFS/FileSystem.h>
#include <Kernel/FileSystem/FUSE/FileSystem.h>
#include <Kernel/FileSystem/ISO9660FS/FileSystem.h>
#include <Kernel/FileSystem/Plan9FS/FileSystem.h>
#include <Kernel/FileSystem/ProcFS/FileSystem.h>
#include <Kernel/FileSystem/RAMFS/FileSystem.h>
#include <Kernel/FileSystem/SysFS/FileSystem.h>

namespace Kernel {

static UnveilNode const& find_matching_unveiled_path(Process const&, StringView path);
static ErrorOr<void> validate_path_against_process_veil(Process const&, StringView path, int options);
static ErrorOr<void> validate_path_against_process_veil(Process const& process, Custody const& custody, int options);
static ErrorOr<void> validate_path_against_process_veil(Custody const& path, int options);
static ErrorOr<NonnullRefPtr<FileSystem>> create_and_initialize_filesystem_from_mount_file(MountFile& mount_file);
static ErrorOr<NonnullRefPtr<FileSystem>> create_and_initialize_filesystem_from_mount_file_and_description(FileBackedFileSystem::List& file_backed_fs_list, MountFile& mount_file, OpenFileDescription& source_description);
static ErrorOr<void> verify_mount_file_and_description_requirements(MountFile& mount_file, OpenFileDescription& source_description);

struct VirtualFileSystemDetails {
    // NOTE: The FileBackedFileSystem list is protected by a mutex because we need to scan it
    // to search for existing filesystems for already used block devices and therefore when doing
    // that we could fail to find a filesystem so we need to create a new filesystem which might
    // need to do disk access (i.e. taking Mutexes in other places) and then register that new filesystem
    // in this list, to avoid TOCTOU bugs.
    MutexProtected<FileBackedFileSystem::List> file_backed_file_systems_list {};
    SpinlockProtected<FileSystem::List, LockRank::FileSystem> file_systems_list {};
    SpinlockProtected<VFSRootContext::List, LockRank::FileSystem> root_contexts {};
};

static Singleton<VirtualFileSystemDetails> s_details;

SpinlockProtected<FileSystem::List, LockRank::FileSystem>& FileSystem::all_file_systems_list()
{
    return s_details->file_systems_list;
}

SpinlockProtected<IntrusiveList<&VFSRootContext::m_list_node>, LockRank::FileSystem>& VFSRootContext::all_root_contexts_list()
{
    return s_details->root_contexts;
}

SpinlockProtected<VFSRootContext::List, LockRank::FileSystem>& VFSRootContext::all_root_contexts_list(Badge<PowerStateSwitchTask>)
{
    return s_details->root_contexts;
}

SpinlockProtected<VFSRootContext::List, LockRank::FileSystem>& VFSRootContext::all_root_contexts_list(Badge<Process>)
{
    return s_details->root_contexts;
}

static ErrorOr<void> validate_mount_boolean_flag_as_invalid(StringView, bool)
{
    return EINVAL;
}

static ErrorOr<void> validate_mount_unsigned_integer_flag_as_invalid(StringView, u64)
{
    return EINVAL;
}

static ErrorOr<void> validate_mount_signed_integer_flag_as_invalid(StringView, i64)
{
    return EINVAL;
}

static ErrorOr<void> validate_mount_ascii_string_flag_as_invalid(StringView, StringView)
{
    return EINVAL;
}

static constexpr FileSystemInitializer s_initializers[] = {
    { "proc"sv, "ProcFS"sv, false, false, false, {}, ProcFS::try_create, validate_mount_boolean_flag_as_invalid, validate_mount_unsigned_integer_flag_as_invalid, validate_mount_signed_integer_flag_as_invalid, validate_mount_ascii_string_flag_as_invalid },
    { "devpts"sv, "DevPtsFS"sv, false, false, false, {}, DevPtsFS::try_create, validate_mount_boolean_flag_as_invalid, validate_mount_unsigned_integer_flag_as_invalid, validate_mount_signed_integer_flag_as_invalid, validate_mount_ascii_string_flag_as_invalid },
    { "sys"sv, "SysFS"sv, false, false, false, {}, SysFS::try_create, validate_mount_boolean_flag_as_invalid, validate_mount_unsigned_integer_flag_as_invalid, validate_mount_signed_integer_flag_as_invalid, validate_mount_ascii_string_flag_as_invalid },
    { "ram"sv, "RAMFS"sv, false, false, false, {}, RAMFS::try_create, validate_mount_boolean_flag_as_invalid, validate_mount_unsigned_integer_flag_as_invalid, validate_mount_signed_integer_flag_as_invalid, validate_mount_ascii_string_flag_as_invalid },
    { "ext2"sv, "Ext2FS"sv, true, true, true, Ext2FS::try_create, {}, validate_mount_boolean_flag_as_invalid, validate_mount_unsigned_integer_flag_as_invalid, validate_mount_signed_integer_flag_as_invalid, validate_mount_ascii_string_flag_as_invalid },
    { "9p"sv, "Plan9FS"sv, true, true, true, Plan9FS::try_create, {}, validate_mount_boolean_flag_as_invalid, validate_mount_unsigned_integer_flag_as_invalid, validate_mount_signed_integer_flag_as_invalid, validate_mount_ascii_string_flag_as_invalid },
    { "iso9660"sv, "ISO9660FS"sv, true, true, true, ISO9660FS::try_create, {}, validate_mount_boolean_flag_as_invalid, validate_mount_unsigned_integer_flag_as_invalid, validate_mount_signed_integer_flag_as_invalid, validate_mount_ascii_string_flag_as_invalid },
    { "fat"sv, "FATFS"sv, true, true, true, FATFS::try_create, {}, validate_mount_boolean_flag_as_invalid, validate_mount_unsigned_integer_flag_as_invalid, validate_mount_signed_integer_flag_as_invalid, validate_mount_ascii_string_flag_as_invalid },
    { "devloop"sv, "DevLoopFS"sv, false, false, false, {}, DevLoopFS::try_create, validate_mount_boolean_flag_as_invalid, validate_mount_unsigned_integer_flag_as_invalid, validate_mount_signed_integer_flag_as_invalid, validate_mount_ascii_string_flag_as_invalid },
    { "fuse"sv, "FUSE"sv, false, false, false, {}, FUSE::try_create, validate_mount_boolean_flag_as_invalid, FUSE::validate_mount_unsigned_integer_flag, validate_mount_signed_integer_flag_as_invalid, validate_mount_ascii_string_flag_as_invalid },
};

ErrorOr<FileSystemInitializer const*> VirtualFileSystem::find_filesystem_type_initializer(StringView fs_type)
{
    for (auto& initializer_entry : s_initializers) {
        if (fs_type == initializer_entry.short_name || fs_type == initializer_entry.name)
            return &initializer_entry;
    }
    return ENODEV;
}

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

ErrorOr<NonnullRefPtr<FileBackedFileSystem>> FileBackedFileSystem::create_and_append_filesystems_list_from_mount_file_and_description(MountFile& mount_file, OpenFileDescription& source_description)
{
    return s_details->file_backed_file_systems_list.with_exclusive([&](auto& list) -> ErrorOr<NonnullRefPtr<FileBackedFileSystem>> {
        auto fs = TRY(create_and_initialize_filesystem_from_mount_file_and_description(list, mount_file, source_description));
        list.append(static_cast<FileBackedFileSystem&>(*fs));
        return static_ptr_cast<FileBackedFileSystem>(fs);
    });
}

ErrorOr<NonnullRefPtr<FileSystem>> create_and_initialize_filesystem_from_mount_file(MountFile& mount_file)
{
    auto const& file_system_initializer = mount_file.file_system_initializer();
    if (file_system_initializer.requires_open_file_description)
        return ENOTSUP;
    if (!file_system_initializer.create)
        return ENOTSUP;
    RefPtr<FileSystem> fs;
    TRY(mount_file.filesystem_specific_options().with_exclusive([&](auto const& filesystem_specific_options) -> ErrorOr<void> {
        fs = TRY(file_system_initializer.create(filesystem_specific_options));
        return {};
    }));
    VERIFY(fs);
    TRY(fs->initialize());
    return fs.release_nonnull();
}

ErrorOr<void> verify_mount_file_and_description_requirements(MountFile& mount_file, OpenFileDescription& source_description)
{
    auto const& file_system_initializer = mount_file.file_system_initializer();
    // NOTE: Although it might be OK to support creating filesystems
    // without providing an actual file descriptor to their create() method
    // because the caller of this function actually supplied a valid file descriptor,
    // this will only make things complicated in the future, so we should block
    // this kind of behavior.
    if (!file_system_initializer.requires_open_file_description)
        return ENOTSUP;

    if (file_system_initializer.requires_block_device && !source_description.file().is_block_device())
        return ENOTBLK;
    if (file_system_initializer.requires_seekable_file && !source_description.file().is_seekable()) {
        dbgln("mount: this is not a seekable file");
        return ENODEV;
    }
    return {};
}

ErrorOr<NonnullRefPtr<FileSystem>> create_and_initialize_filesystem_from_mount_file_and_description(FileBackedFileSystem::List& file_backed_fs_list, MountFile& mount_file, OpenFileDescription& source_description)
{
    // NOTE: If there's an associated file description with the filesystem, we could
    // try to first find it from the VirtualFileSystem filesystem list and if it was not found,
    // then create it and add it.
    auto const& file_system_initializer = mount_file.file_system_initializer();
    VERIFY(file_system_initializer.create_with_fd);
    RefPtr<FileSystem> fs;
    for (auto& node : file_backed_fs_list) {
        if ((&node.file_description() == &source_description) || (&node.file() == &source_description.file())) {
            fs = node;
            break;
        }
    }
    if (!fs) {
        TRY(mount_file.filesystem_specific_options().with_exclusive([&](auto const& filesystem_specific_options) -> ErrorOr<void> {
            fs = TRY(file_system_initializer.create_with_fd(source_description, filesystem_specific_options));
            return {};
        }));
        TRY(fs->initialize());
    }
    if (source_description.file().is_loop_device()) {
        auto& device = static_cast<LoopDevice&>(source_description.file());
        auto path = TRY(device.custody().try_serialize_absolute_path());
        dbgln("VirtualFileSystem: mounting from loop device {}, originated from {}", device.index(), path->view());
    }
    return fs.release_nonnull();
}

ErrorOr<void> VirtualFileSystem::mount(VFSRootContext& context, MountFile& mount_file, OpenFileDescription* source_description, Custody& mount_point, int flags)
{
    if (!source_description) {
        auto fs = TRY(create_and_initialize_filesystem_from_mount_file(mount_file));
        TRY(context.add_new_mount(VFSRootContext::DoBindMount::No, fs->root_inode(), mount_point, flags));
        return {};
    }

    TRY(verify_mount_file_and_description_requirements(mount_file, *source_description));
    return s_details->file_backed_file_systems_list.with_exclusive([&](auto& list) -> ErrorOr<void> {
        auto fs = TRY(create_and_initialize_filesystem_from_mount_file_and_description(list, mount_file, *source_description));
        TRY(context.add_new_mount(VFSRootContext::DoBindMount::No, fs->root_inode(), mount_point, flags));
        list.append(static_cast<FileBackedFileSystem&>(*fs));
        return {};
    });
}

ErrorOr<void> VirtualFileSystem::copy_mount(Custody& original_custody, VFSRootContext& destination_context, Custody& new_mount_point, int flags)
{
    // NOTE: Don't allow moving mounts of inode which are not the root inode
    // of a filesystem. This will prevent copying bindmounts, but the intention
    // of this functionality was never to allow such thing.
    if (&original_custody.inode() != &original_custody.inode().fs().root_inode())
        return EINVAL;

    // NOTE: If the user specified the root custody ("/") on the destination context
    // then try to `pivot_root` the destination context root mount with the desired
    // custody.
    auto destination_context_root_custody = destination_context.root_custody().with([](auto& custody) -> NonnullRefPtr<Custody> { return custody; });
    if (&new_mount_point == destination_context_root_custody.ptr())
        return pivot_root_by_copying_mounted_fs_instance(destination_context, original_custody.inode().fs(), flags);

    TRY(destination_context.add_new_mount(VFSRootContext::DoBindMount::No, original_custody.inode(), new_mount_point, flags));
    return {};
}

ErrorOr<void> VirtualFileSystem::bind_mount(VFSRootContext& context, Custody& source, Custody& mount_point, int flags)
{
    return context.add_new_mount(VFSRootContext::DoBindMount::Yes, source.inode(), mount_point, flags);
}

ErrorOr<void> VirtualFileSystem::remount(VFSRootContext& context, Custody& mount_point, int new_flags)
{
    dbgln("VirtualFileSystem: Remounting inode {}", mount_point.inode().identifier());

    TRY(context.apply_to_mount_for_host_custody(mount_point, [new_flags](auto& mount) {
        mount.set_flags(new_flags);
    }));
    return {};
}

void VirtualFileSystem::sync_filesystems()
{
    Vector<NonnullRefPtr<FileSystem>, 32> file_systems;
    s_details->file_systems_list.with([&](auto const& list) {
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

ErrorOr<void> VirtualFileSystem::unmount(VFSRootContext& context, Custody& mountpoint_custody)
{
    auto& guest_inode = mountpoint_custody.inode();
    auto custody_path = TRY(mountpoint_custody.try_serialize_absolute_path());
    return unmount(context, guest_inode, custody_path->view());
}

ErrorOr<void> VirtualFileSystem::remove_mount(Mount& mount, FileBackedFileSystem::List& file_backed_fs_list)
{
    NonnullRefPtr<FileSystem> fs = mount.guest_fs();
    TRY(fs->prepare_to_unmount(mount.guest()));
    fs->mounted_count().with([&](auto& mounted_count) {
        VERIFY(mounted_count > 0);
        if (mounted_count == 1) {
            dbgln("VirtualFileSystem: Unmounting file system {} for the last time...", fs->fsid());
            s_details->file_systems_list.with([&fs](auto& list) {
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
    Mount::delete_mount_from_list(mount);
    return {};
}

ErrorOr<void> VirtualFileSystem::pivot_root_by_copying_mounted_fs_instance(VFSRootContext& context, FileSystem& fs, int root_mount_flags)
{
    auto root_mount_point = TRY(Custody::try_create(nullptr, ""sv, fs.root_inode(), root_mount_flags));
    auto new_mount = TRY(adopt_nonnull_own_or_enomem(new (nothrow) Mount(fs.root_inode(), root_mount_flags)));

    return s_details->file_backed_file_systems_list.with_exclusive([&](auto& file_backed_file_systems_list) -> ErrorOr<void> {
        return context.pivot_root(file_backed_file_systems_list, fs, move(new_mount), move(root_mount_point), root_mount_flags);
    });
}

ErrorOr<void> VirtualFileSystem::unmount(VFSRootContext& context, Inode& guest_inode, StringView custody_path)
{
    return s_details->file_backed_file_systems_list.with_exclusive([&](auto& file_backed_file_systems_list) -> ErrorOr<void> {
        return context.unmount(file_backed_file_systems_list, guest_inode, custody_path);
    });
}

ErrorOr<void> VirtualFileSystem::utime(VFSRootContext const& vfs_root_context, Credentials const& credentials, StringView path, CustodyBase const& base, time_t atime, time_t mtime)
{
    auto custody = TRY(resolve_path(vfs_root_context, credentials, path, base));
    auto& inode = custody->inode();
    if (!credentials.is_superuser() && inode.metadata().uid != credentials.euid())
        return EACCES;
    if (custody->is_readonly())
        return EROFS;

    TRY(inode.update_timestamps(UnixDateTime::from_seconds_since_epoch(atime), {}, UnixDateTime::from_seconds_since_epoch(mtime)));
    return {};
}

ErrorOr<void> VirtualFileSystem::utimensat(VFSRootContext const& vfs_root_context, Credentials const& credentials, StringView path, CustodyBase const& base, timespec const& atime, timespec const& mtime, int options)
{
    auto custody = TRY(resolve_path(vfs_root_context, credentials, path, base, nullptr, options));
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

ErrorOr<InodeMetadata> VirtualFileSystem::lookup_metadata(VFSRootContext const& vfs_root_context, Credentials const& credentials, StringView path, CustodyBase const& base, int options)
{
    auto custody = TRY(resolve_path(vfs_root_context, credentials, path, base, nullptr, options));
    return custody->inode().metadata();
}

ErrorOr<NonnullRefPtr<OpenFileDescription>> VirtualFileSystem::open(VFSRootContext const& vfs_root_context, Credentials const& credentials, StringView path, int options, mode_t mode, CustodyBase const& base, Optional<UidAndGid> owner)
{
    return open(Process::current(), vfs_root_context, credentials, path, options, mode, base, owner);
}

ErrorOr<NonnullRefPtr<OpenFileDescription>> VirtualFileSystem::open(Process const& process, VFSRootContext const& vfs_root_context, Credentials const& credentials, StringView path, int options, mode_t mode, CustodyBase const& base, Optional<UidAndGid> owner)
{
    if ((options & O_CREAT) && (options & O_DIRECTORY))
        return EINVAL;

    RefPtr<Custody> parent_custody;
    auto custody_or_error = resolve_path(process, vfs_root_context, credentials, path, base, &parent_custody, options);
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
            description->set_original_inode(inode);
            return description;
        } else if (options & O_RDONLY) {
            auto description = TRY(fifo->open_direction_blocking(FIFO::Direction::Reader));
            description->set_rw_mode(options);
            description->set_file_flags(options);
            description->set_original_inode(inode);
            return description;
        }
        return EINVAL;
    }

    if (metadata.is_device()) {
        if (custody.mount_flags() & MS_NODEV)
            return EACCES;
        auto device_type = metadata.is_block_device() ? DeviceNodeType::Block : DeviceNodeType::Character;
        auto device = Device::acquire_by_type_and_major_minor_numbers(device_type, metadata.major_device, metadata.minor_device);
        if (device == nullptr) {
            return ENODEV;
        }
        auto description = TRY(device->open(options));
        description->set_original_inode(inode);
        description->set_original_custody(custody);
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

ErrorOr<void> VirtualFileSystem::mknod(VFSRootContext const& vfs_root_context, Credentials const& credentials, StringView path, mode_t mode, dev_t dev, CustodyBase const& base)
{
    if (!is_regular_file(mode) && !is_block_device(mode) && !is_character_device(mode) && !is_fifo(mode) && !is_socket(mode))
        return EINVAL;

    RefPtr<Custody> parent_custody;
    auto existing_file_or_error = resolve_path(vfs_root_context, credentials, path, base, &parent_custody);
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

ErrorOr<void> VirtualFileSystem::mkdir(VFSRootContext const& vfs_root_context, Credentials const& credentials, StringView path, mode_t mode, CustodyBase const& base)
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
    auto base_custody = TRY(base.resolve());
    auto result = resolve_path_without_veil(vfs_root_context, credentials, path, base_custody, &parent_custody);
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

ErrorOr<void> VirtualFileSystem::access(VFSRootContext const& vfs_root_context, Credentials const& credentials, StringView path, int mode, CustodyBase const& base, AccessFlags access_flags)
{
    auto should_follow_symlinks = !has_flag(access_flags, AccessFlags::DoNotFollowSymlinks);
    auto custody = TRY(resolve_path(vfs_root_context, credentials, path, base, nullptr, should_follow_symlinks ? 0 : O_NOFOLLOW_NOERROR));

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

ErrorOr<NonnullRefPtr<Custody>> VirtualFileSystem::open_directory(VFSRootContext const& vfs_root_context, Credentials const& credentials, StringView path, CustodyBase const& base)
{
    auto custody = TRY(resolve_path(vfs_root_context, credentials, path, base));
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

ErrorOr<void> VirtualFileSystem::chmod(VFSRootContext const& vfs_root_context, Credentials const& credentials, StringView path, mode_t mode, CustodyBase const& base, int options)
{
    auto custody = TRY(resolve_path(vfs_root_context, credentials, path, base, nullptr, options));
    return chmod(credentials, custody, mode);
}

ErrorOr<void> VirtualFileSystem::rename(VFSRootContext const& vfs_root_context, Credentials const& credentials, CustodyBase const& old_base, StringView old_path, CustodyBase const& new_base, StringView new_path)
{
    RefPtr<Custody> old_parent_custody;
    auto old_custody = TRY(resolve_path(vfs_root_context, credentials, old_path, old_base, &old_parent_custody, O_NOFOLLOW_NOERROR));
    auto& old_inode = old_custody->inode();

    RefPtr<Custody> new_parent_custody;
    auto new_custody_or_error = resolve_path(vfs_root_context, credentials, new_path, new_base, &new_parent_custody);
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
    }

    TRY(new_parent_inode.fs().rename(old_parent_inode, old_basename, new_parent_inode, new_basename));

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

ErrorOr<void> VirtualFileSystem::chown(VFSRootContext const& vfs_root_context, Credentials const& credentials, StringView path, UserID a_uid, GroupID a_gid, CustodyBase const& base, int options)
{
    auto custody = TRY(resolve_path(vfs_root_context, credentials, path, base, nullptr, options));
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

ErrorOr<void> VirtualFileSystem::link(VFSRootContext const& vfs_root_context, Credentials const& credentials, StringView old_path, StringView new_path, CustodyBase const& base)
{
    // NOTE: To prevent unveil bypass by creating an hardlink after unveiling a path as read-only,
    // check that if write permission is allowed by the veil info on the old_path.
    auto old_custody = TRY(resolve_path(vfs_root_context, credentials, old_path, base, nullptr, O_RDWR));
    auto& old_inode = old_custody->inode();

    RefPtr<Custody> parent_custody;
    auto new_custody_or_error = resolve_path(vfs_root_context, credentials, new_path, base, &parent_custody);
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

ErrorOr<void> VirtualFileSystem::unlink(VFSRootContext const& vfs_root_context, Credentials const& credentials, StringView path, CustodyBase const& base)
{
    RefPtr<Custody> parent_custody;
    auto custody = TRY(resolve_path(vfs_root_context, credentials, path, base, &parent_custody, O_WRONLY | O_NOFOLLOW_NOERROR | O_UNLINK_INTERNAL));
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

ErrorOr<void> VirtualFileSystem::symlink(VFSRootContext const& vfs_root_context, Credentials const& credentials, StringView target, StringView linkpath, CustodyBase const& base)
{
    auto base_custody = TRY(base.resolve());
    // NOTE: Check that the actual target (if it exists right now) is unveiled and prevent creating symlinks on veiled paths!
    if (auto target_custody_or_error = resolve_path_without_veil(vfs_root_context, credentials, target, base_custody, nullptr, O_RDWR, 0); !target_custody_or_error.is_error()) {
        auto target_custody = target_custody_or_error.release_value();
        TRY(validate_path_against_process_veil(*target_custody, O_RDWR));
    }

    RefPtr<Custody> parent_custody;
    auto existing_custody_or_error = resolve_path(vfs_root_context, credentials, linkpath, base, &parent_custody, O_RDWR);
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
ErrorOr<void> VirtualFileSystem::rmdir(VFSRootContext const& vfs_root_context, Credentials const& credentials, StringView path, CustodyBase const& base)
{
    RefPtr<Custody> parent_custody;
    auto custody = TRY(resolve_path(vfs_root_context, credentials, path, base, &parent_custody, O_CREAT));
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

    return parent_inode.remove_child(KLexicalPath::basename(path));
}

UnveilNode const& find_matching_unveiled_path(Process const& process, StringView path)
{
    VERIFY(process.veil_state() != VeilState::None);
    return process.unveil_data().with([&](auto const& unveil_data) -> UnveilNode const& {
        auto path_parts = KLexicalPath::parts(path);
        return unveil_data.paths.traverse_until_last_accessible_node(path_parts.begin(), path_parts.end());
    });
}

ErrorOr<void> validate_path_against_process_veil(Custody const& custody, int options)
{
    return validate_path_against_process_veil(Process::current(), custody, options);
}

ErrorOr<void> validate_path_against_process_veil(Process const& process, Custody const& custody, int options)
{
    if (process.veil_state() == VeilState::None)
        return {};
    auto absolute_path = TRY(custody.try_serialize_absolute_path());
    return validate_path_against_process_veil(process, absolute_path->view(), options);
}

ErrorOr<void> validate_path_against_process_veil(Process const& process, StringView path, int options)
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

ErrorOr<NonnullRefPtr<Custody>> VirtualFileSystem::resolve_path(VFSRootContext const& vfs_root_context, Credentials const& credentials, StringView path, CustodyBase const& base, RefPtr<Custody>* out_parent, int options, int symlink_recursion_level)
{
    return resolve_path(Process::current(), vfs_root_context, credentials, path, base, out_parent, options, symlink_recursion_level);
}

ErrorOr<NonnullRefPtr<Custody>> VirtualFileSystem::resolve_path(Process const& process, VFSRootContext const& vfs_root_context, Credentials const& credentials, StringView path, CustodyBase const& base, RefPtr<Custody>* out_parent, int options, int symlink_recursion_level)
{
    auto base_custody = TRY(base.resolve());
    // FIXME: The errors returned by resolve_path_without_veil can leak information about paths that are not unveiled,
    //        e.g. when the error is EACCESS or similar.
    auto custody = TRY(resolve_path_without_veil(vfs_root_context, credentials, path, base_custody, out_parent, options, symlink_recursion_level));
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

ErrorOr<NonnullRefPtr<Custody>> VirtualFileSystem::resolve_path_without_veil(VFSRootContext const& vfs_root_context, Credentials const& credentials, StringView path, NonnullRefPtr<Custody> base, RefPtr<Custody>* out_parent, int options, int symlink_recursion_level)
{
    if (symlink_recursion_level >= symlink_recursion_limit)
        return ELOOP;

    if (path.is_empty())
        return EINVAL;

    GenericLexer path_lexer(path);

    auto vfs_root_context_custody = vfs_root_context.root_custody().with([](auto& custody) -> NonnullRefPtr<Custody> {
        return custody;
    });

    NonnullRefPtr<Custody> custody = path[0] == '/' ? vfs_root_context_custody : base;
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
        auto found_mount_state_or_error = vfs_root_context.current_mount_state_for_host_custody(current_custody);
        if (!found_mount_state_or_error.is_error()) {
            auto found_mount_state = found_mount_state_or_error.release_value();
            child_inode = found_mount_state.details.guest;
            mount_flags_for_child = found_mount_state.flags;
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

            auto symlink_target = TRY(child_inode->resolve_as_link(vfs_root_context, credentials, parent, out_parent, options, symlink_recursion_level + 1));
            if (!have_more_parts)
                return symlink_target;

            // Now, resolve the remaining path relative to the symlink target.
            // We prepend a "." to it to ensure that it's not empty and that
            // any initial slashes it might have get interpreted properly.
            StringBuilder remaining_path;
            TRY(remaining_path.try_append('.'));
            TRY(remaining_path.try_append(path.substring_view_starting_after_substring(part)));

            return resolve_path_without_veil(vfs_root_context, credentials, remaining_path.string_view(), symlink_target, out_parent, options, symlink_recursion_level + 1);
        }
    }

    if (out_parent)
        *out_parent = custody->parent();
    return custody;
}
}
