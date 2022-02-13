/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, sin-ack <sin-ack@protonmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/NonnullRefPtrVector.h>
#include <AK/Singleton.h>
#include <AK/StringView.h>
#include <Kernel/API/InodeWatcherEvent.h>
#include <Kernel/FileSystem/Custody.h>
#include <Kernel/FileSystem/Inode.h>
#include <Kernel/FileSystem/InodeWatcher.h>
#include <Kernel/FileSystem/OpenFileDescription.h>
#include <Kernel/FileSystem/VirtualFileSystem.h>
#include <Kernel/KBufferBuilder.h>
#include <Kernel/Memory/SharedInodeVMObject.h>
#include <Kernel/Net/LocalSocket.h>
#include <Kernel/Process.h>

namespace Kernel {

static Singleton<SpinlockProtected<Inode::AllInstancesList>> s_all_instances;

SpinlockProtected<Inode::AllInstancesList>& Inode::all_instances()
{
    return s_all_instances;
}

void Inode::sync_all()
{
    NonnullRefPtrVector<Inode, 32> inodes;
    Inode::all_instances().with([&](auto& all_inodes) {
        for (auto& inode : all_inodes) {
            if (inode.is_metadata_dirty())
                inodes.append(inode);
        }
    });

    for (auto& inode : inodes) {
        VERIFY(inode.is_metadata_dirty());
        (void)inode.flush_metadata();
    }
}

void Inode::sync()
{
    if (is_metadata_dirty())
        (void)flush_metadata();
    fs().flush_writes();
}

ErrorOr<NonnullOwnPtr<KBuffer>> Inode::read_entire(OpenFileDescription* description) const
{
    auto builder = TRY(KBufferBuilder::try_create());

    u8 buffer[4096];
    off_t offset = 0;
    for (;;) {
        auto buf = UserOrKernelBuffer::for_kernel_buffer(buffer);
        auto nread = TRY(read_bytes(offset, sizeof(buffer), buf, description));
        VERIFY(nread <= sizeof(buffer));
        if (nread == 0)
            break;
        TRY(builder.append((const char*)buffer, nread));
        offset += nread;
        if (nread < sizeof(buffer))
            break;
    }

    auto entire_file = builder.build();
    if (!entire_file)
        return ENOMEM;
    return entire_file.release_nonnull();
}

ErrorOr<NonnullRefPtr<Custody>> Inode::resolve_as_link(Custody& base, RefPtr<Custody>* out_parent, int options, int symlink_recursion_level) const
{
    // The default implementation simply treats the stored
    // contents as a path and resolves that. That is, it
    // behaves exactly how you would expect a symlink to work.
    auto contents = TRY(read_entire());
    return VirtualFileSystem::the().resolve_path(StringView { contents->bytes() }, base, out_parent, options, symlink_recursion_level);
}

Inode::Inode(FileSystem& fs, InodeIndex index)
    : m_file_system(fs)
    , m_index(index)
{
    Inode::all_instances().with([&](auto& all_inodes) { all_inodes.append(*this); });
}

Inode::~Inode()
{
    m_watchers.for_each([&](auto& watcher) {
        watcher->unregister_by_inode({}, identifier());
    });
}

void Inode::will_be_destroyed()
{
    MutexLocker locker(m_inode_lock);
    if (m_metadata_dirty)
        (void)flush_metadata();
}

ErrorOr<void> Inode::set_atime(time_t)
{
    return ENOTIMPL;
}

ErrorOr<void> Inode::set_ctime(time_t)
{
    return ENOTIMPL;
}

ErrorOr<void> Inode::set_mtime(time_t)
{
    return ENOTIMPL;
}

ErrorOr<void> Inode::increment_link_count()
{
    return ENOTIMPL;
}

ErrorOr<void> Inode::decrement_link_count()
{
    return ENOTIMPL;
}

ErrorOr<void> Inode::set_shared_vmobject(Memory::SharedInodeVMObject& vmobject)
{
    MutexLocker locker(m_inode_lock);
    m_shared_vmobject = TRY(vmobject.try_make_weak_ptr<Memory::SharedInodeVMObject>());
    return {};
}

RefPtr<LocalSocket> Inode::bound_socket() const
{
    return m_bound_socket;
}

bool Inode::bind_socket(LocalSocket& socket)
{
    MutexLocker locker(m_inode_lock);
    if (m_bound_socket)
        return false;
    m_bound_socket = socket;
    return true;
}

bool Inode::unbind_socket()
{
    MutexLocker locker(m_inode_lock);
    if (!m_bound_socket)
        return false;
    m_bound_socket = nullptr;
    return true;
}

ErrorOr<void> Inode::register_watcher(Badge<InodeWatcher>, InodeWatcher& watcher)
{
    return m_watchers.with([&](auto& watchers) -> ErrorOr<void> {
        VERIFY(!watchers.contains(&watcher));
        TRY(watchers.try_set(&watcher));
        return {};
    });
}

void Inode::unregister_watcher(Badge<InodeWatcher>, InodeWatcher& watcher)
{
    m_watchers.with([&](auto& watchers) {
        VERIFY(watchers.contains(&watcher));
        watchers.remove(&watcher);
    });
}

ErrorOr<NonnullRefPtr<FIFO>> Inode::fifo()
{
    MutexLocker locker(m_inode_lock);
    VERIFY(metadata().is_fifo());

    // FIXME: Release m_fifo when it is closed by all readers and writers
    if (!m_fifo)
        m_fifo = TRY(FIFO::try_create(metadata().uid));

    return NonnullRefPtr { *m_fifo };
}

void Inode::set_metadata_dirty(bool metadata_dirty)
{
    MutexLocker locker(m_inode_lock);

    if (metadata_dirty) {
        // Sanity check.
        VERIFY(!fs().is_readonly());
    }

    if (m_metadata_dirty == metadata_dirty)
        return;

    m_metadata_dirty = metadata_dirty;
    if (m_metadata_dirty) {
        // FIXME: Maybe we should hook into modification events somewhere else, I'm not sure where.
        //        We don't always end up on this particular code path, for instance when writing to an ext2fs file.
        m_watchers.for_each([&](auto& watcher) {
            watcher->notify_inode_event({}, identifier(), InodeWatcherEvent::Type::MetadataModified);
        });
    }
}

void Inode::did_add_child(InodeIdentifier, StringView name)
{
    m_watchers.for_each([&](auto& watcher) {
        watcher->notify_inode_event({}, identifier(), InodeWatcherEvent::Type::ChildCreated, name);
    });
}

void Inode::did_remove_child(InodeIdentifier, StringView name)
{
    if (name == "." || name == "..") {
        // These are just aliases and are not interesting to userspace.
        return;
    }

    m_watchers.for_each([&](auto& watcher) {
        watcher->notify_inode_event({}, identifier(), InodeWatcherEvent::Type::ChildDeleted, name);
    });
}

void Inode::did_modify_contents()
{
    m_watchers.for_each([&](auto& watcher) {
        watcher->notify_inode_event({}, identifier(), InodeWatcherEvent::Type::ContentModified);
    });
}

void Inode::did_delete_self()
{
    m_watchers.for_each([&](auto& watcher) {
        watcher->notify_inode_event({}, identifier(), InodeWatcherEvent::Type::Deleted);
    });
}

ErrorOr<void> Inode::prepare_to_write_data()
{
    // FIXME: It's a poor design that filesystems are expected to call this before writing out data.
    //        We should funnel everything through an interface at the VirtualFileSystem layer so this can happen from a single place.
    MutexLocker locker(m_inode_lock);
    if (fs().is_readonly())
        return EROFS;
    auto metadata = this->metadata();
    if (metadata.is_setuid() || metadata.is_setgid()) {
        dbgln("Inode::prepare_to_write_data(): Stripping SUID/SGID bits from {}", identifier());
        return chmod(metadata.mode & ~(04000 | 02000));
    }
    return {};
}

RefPtr<Memory::SharedInodeVMObject> Inode::shared_vmobject() const
{
    MutexLocker locker(m_inode_lock);
    return m_shared_vmobject.strong_ref();
}

template<typename T>
static inline bool range_overlap(T start1, T len1, T start2, T len2)
{
    return ((start1 < start2 + len2) || len2 == 0) && ((start2 < start1 + len1) || len1 == 0);
}

static inline ErrorOr<void> normalize_flock(OpenFileDescription const& description, flock& lock)
{
    off_t start;
    switch (lock.l_whence) {
    case SEEK_SET:
        start = lock.l_start;
        break;
    case SEEK_CUR:
        start = description.offset() + lock.l_start;
        break;
    case SEEK_END:
        // FIXME: Implement SEEK_END and negative lengths.
        return ENOTSUP;
    default:
        return EINVAL;
    }
    lock = { lock.l_type, SEEK_SET, start, lock.l_len, 0 };
    return {};
}

ErrorOr<void> Inode::can_apply_flock(OpenFileDescription const& description, flock const& new_lock) const
{
    VERIFY(new_lock.l_whence == SEEK_SET);

    return m_flocks.with([&](auto& flocks) -> ErrorOr<void> {
        if (new_lock.l_type == F_UNLCK) {
            for (auto const& lock : flocks) {
                if (&description == lock.owner && lock.start == new_lock.l_start && lock.len == new_lock.l_len)
                    return {};
            }
            return EINVAL;
        }
        for (auto const& lock : flocks) {
            if (!range_overlap(lock.start, lock.len, new_lock.l_start, new_lock.l_len))
                continue;

            if (new_lock.l_type == F_RDLCK && lock.type == F_WRLCK)
                return EAGAIN;

            if (new_lock.l_type == F_WRLCK)
                return EAGAIN;
        }
        return {};
    });
}

ErrorOr<void> Inode::apply_flock(Process const& process, OpenFileDescription const& description, Userspace<flock const*> input_lock)
{
    auto new_lock = TRY(copy_typed_from_user(input_lock));
    TRY(normalize_flock(description, new_lock));

    return m_flocks.with([&](auto& flocks) -> ErrorOr<void> {
        TRY(can_apply_flock(description, new_lock));

        if (new_lock.l_type == F_UNLCK) {
            for (size_t i = 0; i < flocks.size(); ++i) {
                if (&description == flocks[i].owner && flocks[i].start == new_lock.l_start && flocks[i].len == new_lock.l_len) {
                    flocks.remove(i);
                    return {};
                }
            }
            return EINVAL;
        }

        TRY(flocks.try_append(Flock { new_lock.l_start, new_lock.l_len, &description, process.pid().value(), new_lock.l_type }));
        return {};
    });
}

ErrorOr<void> Inode::get_flock(OpenFileDescription const& description, Userspace<flock*> reference_lock) const
{
    flock lookup = {};
    TRY(copy_from_user(&lookup, reference_lock));
    TRY(normalize_flock(description, lookup));

    return m_flocks.with([&](auto& flocks) {
        for (auto const& lock : flocks) {
            if (!range_overlap(lock.start, lock.len, lookup.l_start, lookup.l_len))
                continue;

            if ((lookup.l_type == F_RDLCK && lock.type == F_WRLCK) || lookup.l_type == F_WRLCK) {
                lookup = { lock.type, SEEK_SET, lock.start, lock.len, lock.pid };
                return copy_to_user(reference_lock, &lookup);
            }
        }

        lookup.l_type = F_UNLCK;
        return copy_to_user(reference_lock, &lookup);
    });
}

void Inode::remove_flocks_for_description(OpenFileDescription const& description)
{
    m_flocks.with([&](auto& flocks) {
        flocks.remove_all_matching([&](auto& entry) { return entry.owner == &description; });
    });
}

bool Inode::has_watchers() const
{
    return !m_watchers.with([&](auto& watchers) { return watchers.is_empty(); });
}

}
