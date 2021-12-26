#include <AK/FileSystemPath.h>
#include <AK/StringBuilder.h>
#include <Kernel/Devices/CharacterDevice.h>
#include <Kernel/FileSystem/Custody.h>
#include <Kernel/FileSystem/DiskBackedFileSystem.h>
#include <Kernel/FileSystem/FileDescription.h>
#include <Kernel/FileSystem/FileSystem.h>
#include <Kernel/FileSystem/VirtualFileSystem.h>
#include <Kernel/Process.h>
#include <LibC/errno_numbers.h>

//#define VFS_DEBUG

static VFS* s_the;
static constexpr int symlink_recursion_limit { 5 }; // FIXME: increase?

VFS& VFS::the()
{
    ASSERT(s_the);
    return *s_the;
}

VFS::VFS()
{
#ifdef VFS_DEBUG
    kprintf("VFS: Constructing VFS\n");
#endif
    s_the = this;
}

VFS::~VFS()
{
}

InodeIdentifier VFS::root_inode_id() const
{
    ASSERT(m_root_inode);
    return m_root_inode->identifier();
}

KResult VFS::mount(FS& file_system, Custody& mount_point, int flags)
{
    auto& inode = mount_point.inode();
    dbg() << "VFS: Mounting " << file_system.class_name() << " at " << mount_point.absolute_path() << " (inode: " << inode.identifier() << ") with flags " << flags;
    // FIXME: check that this is not already a mount point
    Mount mount { file_system, &mount_point, flags };
    m_mounts.append(move(mount));
    mount_point.did_mount_on({});
    return KSuccess;
}

KResult VFS::bind_mount(Custody& source, Custody& mount_point, int flags)
{
    dbg() << "VFS: Bind-mounting " << source.absolute_path() << " at " << mount_point.absolute_path();
    // FIXME: check that this is not already a mount point
    Mount mount { source.inode(), mount_point, flags };
    m_mounts.append(move(mount));
    mount_point.did_mount_on({});
    return KSuccess;
}

KResult VFS::unmount(InodeIdentifier guest_inode_id)
{
    LOCKER(m_lock);
    dbg() << "VFS: unmount called with inode " << guest_inode_id;

    for (int i = 0; i < m_mounts.size(); ++i) {
        auto& mount = m_mounts.at(i);
        if (mount.guest() == guest_inode_id) {
            auto result = mount.guest_fs().prepare_to_unmount();
            if (result.is_error()) {
                dbg() << "VFS: Failed to unmount!";
                return result;
            }
            dbg() << "VFS: found fs " << mount.guest_fs().fsid() << " at mount index " << i << "! Unmounting...";
            m_mounts.unstable_remove(i);
            return KSuccess;
        }
    }

    dbg() << "VFS: Nothing mounted on inode " << guest_inode_id;
    return KResult(-ENODEV);
}

bool VFS::mount_root(FS& file_system)
{
    if (m_root_inode) {
        kprintf("VFS: mount_root can't mount another root\n");
        return false;
    }

    Mount mount { file_system, nullptr, MS_NODEV | MS_NOSUID };

    auto root_inode_id = mount.guest().fs()->root_inode();
    auto root_inode = mount.guest().fs()->get_inode(root_inode_id);
    if (!root_inode->is_directory()) {
        kprintf("VFS: root inode (%02u:%08u) for / is not a directory :(\n", root_inode_id.fsid(), root_inode_id.index());
        return false;
    }

    m_root_inode = move(root_inode);
    char device_name[32];
    if (m_root_inode->fs().is_disk_backed()) {
        auto& device = static_cast<DiskBackedFS&>(m_root_inode->fs()).device();
        sprintf(device_name, "%d,%d", device.major(), device.minor());
    } else {
        sprintf(device_name, "not-a-disk");
    }
    kprintf("VFS: mounted root on %s (%s)\n", m_root_inode->fs().class_name(), device_name);

    m_mounts.append(move(mount));
    return true;
}

auto VFS::find_mount_for_host(InodeIdentifier inode) -> Mount*
{
    for (auto& mount : m_mounts) {
        if (mount.host() == inode)
            return &mount;
    }
    return nullptr;
}

auto VFS::find_mount_for_guest(InodeIdentifier inode) -> Mount*
{
    for (auto& mount : m_mounts) {
        if (mount.guest() == inode)
            return &mount;
    }
    return nullptr;
}

bool VFS::is_vfs_root(InodeIdentifier inode) const
{
    return inode == root_inode_id();
}

void VFS::traverse_directory_inode(Inode& dir_inode, Function<bool(const FS::DirectoryEntry&)> callback)
{
    dir_inode.traverse_as_directory([&](const FS::DirectoryEntry& entry) {
        InodeIdentifier resolved_inode;
        if (auto mount = find_mount_for_host(entry.inode))
            resolved_inode = mount->guest();
        else
            resolved_inode = entry.inode;

        if (dir_inode.identifier().is_root_inode() && !is_vfs_root(dir_inode.identifier()) && !strcmp(entry.name, "..")) {
            auto mount = find_mount_for_guest(entry.inode);
            ASSERT(mount);
            resolved_inode = mount->host();
        }
        callback(FS::DirectoryEntry(entry.name, entry.name_length, resolved_inode, entry.file_type));
        return true;
    });
}

KResult VFS::utime(StringView path, Custody& base, time_t atime, time_t mtime)
{
    auto descriptor_or_error = VFS::the().open(move(path), 0, 0, base);
    if (descriptor_or_error.is_error())
        return descriptor_or_error.error();
    auto& inode = *descriptor_or_error.value()->inode();
    if (inode.fs().is_readonly())
        return KResult(-EROFS);
    if (!current->process().is_superuser() && inode.metadata().uid != current->process().euid())
        return KResult(-EACCES);

    int error = inode.set_atime(atime);
    if (error)
        return KResult(error);
    error = inode.set_mtime(mtime);
    if (error)
        return KResult(error);
    return KSuccess;
}

KResultOr<InodeMetadata> VFS::lookup_metadata(StringView path, Custody& base, int options)
{
    auto custody_or_error = resolve_path(path, base, nullptr, options);
    if (custody_or_error.is_error())
        return custody_or_error.error();
    return custody_or_error.value()->inode().metadata();
}

KResultOr<NonnullRefPtr<FileDescription>> VFS::open(StringView path, int options, mode_t mode, Custody& base, Optional<UidAndGid> owner)
{
    if ((options & O_CREAT) && (options & O_DIRECTORY))
        return KResult(-EINVAL);

    RefPtr<Custody> parent_custody;
    auto custody_or_error = resolve_path(path, base, &parent_custody, options);
    if (options & O_CREAT) {
        if (!parent_custody)
            return KResult(-ENOENT);
        if (custody_or_error.is_error()) {
            if (custody_or_error.error() != -ENOENT)
                return custody_or_error.error();
            return create(path, options, mode, *parent_custody, move(owner));
        }
        if (options & O_EXCL)
            return KResult(-EEXIST);
    }
    if (custody_or_error.is_error())
        return custody_or_error.error();

    auto& custody = *custody_or_error.value();
    auto& inode = custody.inode();
    auto metadata = inode.metadata();

    if ((options & O_DIRECTORY) && !metadata.is_directory())
        return KResult(-ENOTDIR);

    bool should_truncate_file = false;

    // NOTE: Read permission is a bit weird, since O_RDONLY == 0,
    //       so we check if (NOT write_only OR read_and_write)
    if (!(options & O_WRONLY) || (options & O_RDWR)) {
        if (!metadata.may_read(current->process()))
            return KResult(-EACCES);
    }
    if ((options & O_WRONLY) || (options & O_RDWR)) {
        if (!metadata.may_write(current->process()))
            return KResult(-EACCES);
        if (metadata.is_directory())
            return KResult(-EISDIR);
        should_truncate_file = options & O_TRUNC;
    }
    if (options & O_EXEC) {
        if (!metadata.may_execute(current->process()) || (custody.mount_flags() & MS_NOEXEC))
            return KResult(-EACCES);
    }

    if (metadata.is_device()) {
        if (custody.mount_flags() & MS_NODEV)
            return KResult(-EACCES);
        auto device = Device::get_device(metadata.major_device, metadata.minor_device);
        if (device == nullptr) {
            return KResult(-ENODEV);
        }
        auto descriptor_or_error = device->open(options);
        if (descriptor_or_error.is_error())
            return descriptor_or_error.error();
        descriptor_or_error.value()->set_original_inode({}, inode);
        return descriptor_or_error;
    }
    if (should_truncate_file) {
        inode.truncate(0);
        inode.set_mtime(kgettimeofday().tv_sec);
    }
    return FileDescription::create(custody);
}

KResult VFS::mknod(StringView path, mode_t mode, dev_t dev, Custody& base)
{
    if (!is_regular_file(mode) && !is_block_device(mode) && !is_character_device(mode) && !is_fifo(mode) && !is_socket(mode))
        return KResult(-EINVAL);

    RefPtr<Custody> parent_custody;
    auto existing_file_or_error = resolve_path(path, base, &parent_custody);
    if (!existing_file_or_error.is_error())
        return KResult(-EEXIST);
    if (!parent_custody)
        return KResult(-ENOENT);
    if (existing_file_or_error.error() != -ENOENT)
        return existing_file_or_error.error();
    auto& parent_inode = parent_custody->inode();
    if (!parent_inode.metadata().may_write(current->process()))
        return KResult(-EACCES);

    FileSystemPath p(path);
    dbg() << "VFS::mknod: '" << p.basename() << "' mode=" << mode << " dev=" << dev << " in " << parent_inode.identifier();
    int error;
    auto new_file = parent_inode.fs().create_inode(parent_inode.identifier(), p.basename(), mode, 0, dev, current->process().uid(), current->process().gid(), error);
    if (!new_file)
        return KResult(error);

    return KSuccess;
}

KResultOr<NonnullRefPtr<FileDescription>> VFS::create(StringView path, int options, mode_t mode, Custody& parent_custody, Optional<UidAndGid> owner)
{
    (void)options;

    if (!is_socket(mode) && !is_fifo(mode) && !is_block_device(mode) && !is_character_device(mode)) {
        // Turn it into a regular file. (This feels rather hackish.)
        mode |= 0100000;
    }

    auto& parent_inode = parent_custody.inode();
    if (!parent_inode.metadata().may_write(current->process()))
        return KResult(-EACCES);
    FileSystemPath p(path);
#ifdef VFS_DEBUG
    dbg() << "VFS::create: '" << p.basename() << "' in " << parent_inode.identifier();
#endif
    int error;

    uid_t uid = owner.has_value() ? owner.value().uid : current->process().uid();
    gid_t gid = owner.has_value() ? owner.value().gid : current->process().gid();
    auto new_file = parent_inode.fs().create_inode(parent_inode.identifier(), p.basename(), mode, 0, 0, uid, gid, error);
    if (!new_file)
        return KResult(error);

    auto new_custody = Custody::create(&parent_custody, p.basename(), *new_file, parent_custody.mount_flags());
    return FileDescription::create(*new_custody);
}

KResult VFS::mkdir(StringView path, mode_t mode, Custody& base)
{
    RefPtr<Custody> parent_custody;
    auto result = resolve_path(path, base, &parent_custody);
    if (!result.is_error())
        return KResult(-EEXIST);
    if (!parent_custody)
        return KResult(-ENOENT);
    if (result.error() != -ENOENT)
        return result.error();

    auto& parent_inode = parent_custody->inode();
    if (!parent_inode.metadata().may_write(current->process()))
        return KResult(-EACCES);

    FileSystemPath p(path);
#ifdef VFS_DEBUG
    dbg() << "VFS::mkdir: '" << p.basename() << "' in " << parent_inode.identifier();
#endif
    int error;
    auto new_dir = parent_inode.fs().create_directory(parent_inode.identifier(), p.basename(), mode, current->process().uid(), current->process().gid(), error);
    if (new_dir)
        return KSuccess;
    return KResult(error);
}

KResult VFS::access(StringView path, int mode, Custody& base)
{
    auto custody_or_error = resolve_path(path, base);
    if (custody_or_error.is_error())
        return custody_or_error.error();
    auto& custody = *custody_or_error.value();
    auto& inode = custody.inode();
    auto metadata = inode.metadata();
    if (mode & R_OK) {
        if (!metadata.may_read(current->process()))
            return KResult(-EACCES);
    }
    if (mode & W_OK) {
        if (!metadata.may_write(current->process()))
            return KResult(-EACCES);
    }
    if (mode & X_OK) {
        if (!metadata.may_execute(current->process()))
            return KResult(-EACCES);
    }
    return KSuccess;
}

KResultOr<NonnullRefPtr<Custody>> VFS::open_directory(StringView path, Custody& base)
{
    auto inode_or_error = resolve_path(path, base);
    if (inode_or_error.is_error())
        return inode_or_error.error();
    auto& custody = *inode_or_error.value();
    auto& inode = custody.inode();
    if (!inode.is_directory())
        return KResult(-ENOTDIR);
    if (!inode.metadata().may_execute(current->process()))
        return KResult(-EACCES);
    return custody;
}

KResult VFS::chmod(Inode& inode, mode_t mode)
{
    if (inode.fs().is_readonly())
        return KResult(-EROFS);

    if (current->process().euid() != inode.metadata().uid && !current->process().is_superuser())
        return KResult(-EPERM);

    // Only change the permission bits.
    mode = (inode.mode() & ~04777u) | (mode & 04777u);
    return inode.chmod(mode);
}

KResult VFS::chmod(StringView path, mode_t mode, Custody& base)
{
    auto custody_or_error = resolve_path(path, base);
    if (custody_or_error.is_error())
        return custody_or_error.error();
    auto& custody = *custody_or_error.value();
    auto& inode = custody.inode();
    return chmod(inode, mode);
}

KResult VFS::rename(StringView old_path, StringView new_path, Custody& base)
{
    RefPtr<Custody> old_parent_custody;
    auto old_custody_or_error = resolve_path(old_path, base, &old_parent_custody);
    if (old_custody_or_error.is_error())
        return old_custody_or_error.error();
    auto& old_custody = *old_custody_or_error.value();
    auto& old_inode = old_custody.inode();

    RefPtr<Custody> new_parent_custody;
    auto new_custody_or_error = resolve_path(new_path, base, &new_parent_custody);
    if (new_custody_or_error.is_error()) {
        if (new_custody_or_error.error() != -ENOENT)
            return new_custody_or_error.error();
    }

    auto& old_parent_inode = old_parent_custody->inode();
    auto& new_parent_inode = new_parent_custody->inode();

    if (&old_parent_inode.fs() != &new_parent_inode.fs())
        return KResult(-EXDEV);

    if (!new_parent_inode.metadata().may_write(current->process()))
        return KResult(-EACCES);

    if (!old_parent_inode.metadata().may_write(current->process()))
        return KResult(-EACCES);

    if (old_parent_inode.metadata().is_sticky()) {
        if (!current->process().is_superuser() && old_inode.metadata().uid != current->process().euid())
            return KResult(-EACCES);
    }

    auto new_basename = FileSystemPath(new_path).basename();

    if (!new_custody_or_error.is_error()) {
        auto& new_custody = *new_custody_or_error.value();
        auto& new_inode = new_custody.inode();
        // FIXME: Is this really correct? Check what other systems do.
        if (&new_inode == &old_inode)
            return KSuccess;
        if (new_parent_inode.metadata().is_sticky()) {
            if (!current->process().is_superuser() && new_inode.metadata().uid != current->process().euid())
                return KResult(-EACCES);
        }
        if (new_inode.is_directory() && !old_inode.is_directory())
            return KResult(-EISDIR);
        auto result = new_parent_inode.remove_child(new_basename);
        if (result.is_error())
            return result;
        new_custody.did_delete({});
    }

    auto result = new_parent_inode.add_child(old_inode.identifier(), new_basename, old_inode.mode());
    if (result.is_error())
        return result;

    result = old_parent_inode.remove_child(FileSystemPath(old_path).basename());
    if (result.is_error())
        return result;
    old_custody.did_rename({}, new_basename);

    return KSuccess;
}

KResult VFS::chown(Inode& inode, uid_t a_uid, gid_t a_gid)
{
    if (inode.fs().is_readonly())
        return KResult(-EROFS);

    auto metadata = inode.metadata();

    if (current->process().euid() != metadata.uid && !current->process().is_superuser())
        return KResult(-EPERM);

    uid_t new_uid = metadata.uid;
    gid_t new_gid = metadata.gid;

    if (a_uid != (uid_t)-1) {
        if (current->process().euid() != a_uid && !current->process().is_superuser())
            return KResult(-EPERM);
        new_uid = a_uid;
    }
    if (a_gid != (gid_t)-1) {
        if (!current->process().in_group(a_gid) && !current->process().is_superuser())
            return KResult(-EPERM);
        new_gid = a_gid;
    }

    dbg() << "VFS::chown(): inode " << inode.identifier() << " <- uid:" << new_uid << " gid:" << new_gid;
    return inode.chown(new_uid, new_gid);
}

KResult VFS::chown(StringView path, uid_t a_uid, gid_t a_gid, Custody& base)
{
    auto custody_or_error = resolve_path(path, base);
    if (custody_or_error.is_error())
        return custody_or_error.error();
    auto& custody = *custody_or_error.value();
    auto& inode = custody.inode();
    return chown(inode, a_uid, a_gid);
}

KResult VFS::link(StringView old_path, StringView new_path, Custody& base)
{
    auto old_custody_or_error = resolve_path(old_path, base);
    if (old_custody_or_error.is_error())
        return old_custody_or_error.error();
    auto& old_custody = *old_custody_or_error.value();
    auto& old_inode = old_custody.inode();

    RefPtr<Custody> parent_custody;
    auto new_custody_or_error = resolve_path(new_path, base, &parent_custody);
    if (!new_custody_or_error.is_error())
        return KResult(-EEXIST);

    if (!parent_custody)
        return KResult(-ENOENT);

    auto& parent_inode = parent_custody->inode();

    if (parent_inode.fsid() != old_inode.fsid())
        return KResult(-EXDEV);

    if (parent_inode.fs().is_readonly())
        return KResult(-EROFS);

    if (!parent_inode.metadata().may_write(current->process()))
        return KResult(-EACCES);

    return parent_inode.add_child(old_inode.identifier(), FileSystemPath(new_path).basename(), old_inode.mode());
}

KResult VFS::unlink(StringView path, Custody& base)
{
    RefPtr<Custody> parent_custody;
    auto custody_or_error = resolve_path(path, base, &parent_custody, O_NOFOLLOW_NOERROR);
    if (custody_or_error.is_error())
        return custody_or_error.error();
    auto& custody = *custody_or_error.value();
    auto& inode = custody.inode();

    if (inode.is_directory())
        return KResult(-EISDIR);

    auto& parent_inode = parent_custody->inode();
    if (!parent_inode.metadata().may_write(current->process()))
        return KResult(-EACCES);

    if (parent_inode.metadata().is_sticky()) {
        if (!current->process().is_superuser() && inode.metadata().uid != current->process().euid())
            return KResult(-EACCES);
    }

    auto result = parent_inode.remove_child(FileSystemPath(path).basename());
    if (result.is_error())
        return result;

    custody.did_delete({});
    return KSuccess;
}

KResult VFS::symlink(StringView target, StringView linkpath, Custody& base)
{
    RefPtr<Custody> parent_custody;
    auto existing_custody_or_error = resolve_path(linkpath, base, &parent_custody);
    if (!existing_custody_or_error.is_error())
        return KResult(-EEXIST);
    if (!parent_custody)
        return KResult(-ENOENT);
    if (existing_custody_or_error.error() != -ENOENT)
        return existing_custody_or_error.error();
    auto& parent_inode = parent_custody->inode();
    if (!parent_inode.metadata().may_write(current->process()))
        return KResult(-EACCES);

    FileSystemPath p(linkpath);
    dbg() << "VFS::symlink: '" << p.basename() << "' (-> '" << target << "') in " << parent_inode.identifier();
    int error;
    auto new_file = parent_inode.fs().create_inode(parent_inode.identifier(), p.basename(), 0120644, 0, 0, current->process().uid(), current->process().gid(), error);
    if (!new_file)
        return KResult(error);
    ssize_t nwritten = new_file->write_bytes(0, target.length(), (const u8*)target.characters_without_null_termination(), nullptr);
    if (nwritten < 0)
        return KResult(nwritten);
    return KSuccess;
}

KResult VFS::rmdir(StringView path, Custody& base)
{
    RefPtr<Custody> parent_custody;
    auto custody_or_error = resolve_path(path, base, &parent_custody);
    if (custody_or_error.is_error())
        return KResult(custody_or_error.error());

    auto& custody = *custody_or_error.value();
    auto& inode = custody.inode();
    if (inode.fs().is_readonly())
        return KResult(-EROFS);

    // FIXME: We should return EINVAL if the last component of the path is "."
    // FIXME: We should return ENOTEMPTY if the last component of the path is ".."

    if (!inode.is_directory())
        return KResult(-ENOTDIR);

    auto& parent_inode = parent_custody->inode();

    if (!parent_inode.metadata().may_write(current->process()))
        return KResult(-EACCES);

    if (inode.directory_entry_count() != 2)
        return KResult(-ENOTEMPTY);

    auto result = inode.remove_child(".");
    if (result.is_error())
        return result;

    result = inode.remove_child("..");
    if (result.is_error())
        return result;

    return parent_inode.remove_child(FileSystemPath(path).basename());
}

RefPtr<Inode> VFS::get_inode(InodeIdentifier inode_id)
{
    if (!inode_id.is_valid())
        return nullptr;
    return inode_id.fs()->get_inode(inode_id);
}

VFS::Mount::Mount(FS& guest_fs, Custody* host_custody, int flags)
    : m_guest(guest_fs.root_inode())
    , m_guest_fs(guest_fs)
    , m_host_custody(host_custody)
    , m_flags(flags)
{
}

VFS::Mount::Mount(Inode& source, Custody& host_custody, int flags)
    : m_guest(source.identifier())
    , m_guest_fs(source.fs())
    , m_host_custody(host_custody)
    , m_flags(flags)
{
}

String VFS::Mount::absolute_path() const
{
    if (!m_host_custody)
        return "/";
    return m_host_custody->absolute_path();
}

InodeIdentifier VFS::Mount::host() const
{
    if (!m_host_custody)
        return {};
    return m_host_custody->inode().identifier();
}

void VFS::for_each_mount(Function<void(const Mount&)> callback) const
{
    for (auto& mount : m_mounts) {
        callback(mount);
    }
}

void VFS::sync()
{
    FS::sync();
}

Custody& VFS::root_custody()
{
    if (!m_root_custody)
        m_root_custody = Custody::create(nullptr, "", *m_root_inode, MS_NODEV | MS_NOSUID);
    return *m_root_custody;
}

KResultOr<NonnullRefPtr<Custody>> VFS::resolve_path(StringView path, Custody& base, RefPtr<Custody>* parent_custody, int options, int symlink_recursion_level)
{
    if (symlink_recursion_level >= symlink_recursion_limit)
        return KResult(-ELOOP);

    if (path.is_empty())
        return KResult(-EINVAL);

    auto parts = path.split_view('/', true);

    auto& current_root = current->process().root_directory();

    NonnullRefPtrVector<Custody, 32> custody_chain;

    if (path[0] == '/') {
        custody_chain.append(current_root);
    } else {
        for (auto* custody = &base; custody; custody = custody->parent()) {
            // FIXME: Prepending here is not efficient! Fix this.
            custody_chain.prepend(*custody);
        }
    }

    for (int i = 0; i < parts.size(); ++i) {
        auto& current_parent = custody_chain.last();
        auto parent_metadata = current_parent.inode().metadata();
        if (!parent_metadata.is_directory())
            return KResult(-ENOTDIR);
        // Ensure the current user is allowed to resolve paths inside this directory.
        if (!parent_metadata.may_execute(current->process()))
            return KResult(-EACCES);

        auto& part = parts[i];
        bool have_more_parts = i + 1 < parts.size();

        if (part == "..") {
            // If we encounter a "..", take a step back, but don't go beyond the root.
            if (custody_chain.size() > 1)
                custody_chain.take_last();
            continue;
        } else if (part == "." || part.is_empty()) {
            continue;
        }

        // Okay, let's look up this part.
        auto child_id = current_parent.inode().lookup(part);

        if (!child_id.is_valid()) {
            if (parent_custody) {
                // ENOENT with a non-null parent custody signals to caller that
                // we found the immediate parent of the file, but the file itself
                // does not exist yet.
                *parent_custody = have_more_parts ? nullptr : &current_parent;
            }
            return KResult(-ENOENT);
        }

        int mount_flags_for_child = current_parent.mount_flags();
        // See if there's something mounted on the child; in that case
        // we would need to return the guest inode, not the host inode.
        if (auto mount = find_mount_for_host(child_id)) {
            child_id = mount->guest();
            mount_flags_for_child = mount->flags();
        }
        auto child_inode = get_inode(child_id);
        ASSERT(child_inode);

        custody_chain.append(Custody::create(&current_parent, part, *child_inode, mount_flags_for_child));

        if (child_inode->metadata().is_symlink()) {
            if (!have_more_parts) {
                if (options & O_NOFOLLOW)
                    return KResult(-ELOOP);
                if (options & O_NOFOLLOW_NOERROR)
                    break;
            }
            auto symlink_contents = child_inode->read_entire();
            if (!symlink_contents) {
                if (parent_custody)
                    *parent_custody = nullptr;
                return KResult(-ENOENT);
            }

            auto symlink_path = StringView(symlink_contents.data(), symlink_contents.size());
            auto symlink_target = resolve_path(symlink_path, current_parent, parent_custody, options, symlink_recursion_level + 1);

            if (symlink_target.is_error() || !have_more_parts)
                return symlink_target;

            // Now, resolve the remaining path relative to the symlink target.
            // We prepend a "." to it to ensure that it's not empty and that
            // any initial slashes it might have get interpreted properly.
            StringBuilder remaining_path;
            remaining_path.append('.');
            remaining_path.append(path.substring_view_starting_after_substring(part));

            return resolve_path(remaining_path.to_string(), *symlink_target.value(), parent_custody, options, symlink_recursion_level + 1);
        }
    }

    if (parent_custody)
        *parent_custody = custody_chain.last().parent();
    return custody_chain.last();
}
