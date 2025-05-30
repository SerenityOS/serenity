/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, sin-ack <sin-ack@protonmail.com>
 * Copyright (c) 2022, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Singleton.h>
#include <AK/StringView.h>
#include <Kernel/API/InodeWatcherEvent.h>
#include <Kernel/FileSystem/Custody.h>
#include <Kernel/FileSystem/Inode.h>
#include <Kernel/FileSystem/InodeWatcher.h>
#include <Kernel/FileSystem/OpenFileDescription.h>
#include <Kernel/FileSystem/VFSRootContext.h>
#include <Kernel/FileSystem/VirtualFileSystem.h>
#include <Kernel/Library/KBufferBuilder.h>
#include <Kernel/Memory/SharedInodeVMObject.h>
#include <Kernel/Net/LocalSocket.h>
#include <Kernel/Tasks/Process.h>

namespace Kernel {

static Singleton<SpinlockProtected<Inode::AllInstancesList, LockRank::None>> s_all_instances;

SpinlockProtected<Inode::AllInstancesList, LockRank::None>& Inode::all_instances()
{
    return s_all_instances;
}

void Inode::sync_all()
{
    Vector<NonnullRefPtr<Inode>, 32> inodes;
    Inode::all_instances().with([&](auto& all_inodes) {
        for (auto& inode : all_inodes) {
            if (inode.is_metadata_dirty())
                inodes.append(inode);
        }
    });

    for (auto& inode : inodes) {
        (void)inode->flush_metadata();
    }
}

void Inode::sync()
{
    (void)flush_metadata();
    auto result = fs().flush_writes();
    if (result.is_error()) {
        // TODO: Figure out how to propagate error to a higher function.
    }
}

ErrorOr<NonnullRefPtr<Custody>> Inode::resolve_as_link(VFSRootContext const& vfs_root_context, Credentials const& credentials, CustodyBase const& base, RefPtr<Custody>* out_parent, int options, int symlink_recursion_level) const
{
    // The default implementation simply treats the stored
    // contents as a path and resolves that. That is, it
    // behaves exactly how you would expect a symlink to work.

    // Make sure that our assumptions about the path length hold up.
    // Note that this doesn't mean that the reported size can be trusted, some inodes just report zero.
    VERIFY(size() <= MAXPATHLEN);

    Array<u8, MAXPATHLEN> contents;
    auto read_bytes = TRY(read_until_filled_or_end(0, contents.size(), UserOrKernelBuffer::for_kernel_buffer(contents.data()), nullptr));
    return VirtualFileSystem::resolve_path(vfs_root_context, credentials, StringView { contents.span().trim(read_bytes) }, base, out_parent, options, symlink_recursion_level);
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

ErrorOr<void> Inode::truncate(u64 size)
{
    MutexLocker locker(m_inode_lock);
    return truncate_locked(size);
}

ErrorOr<size_t> Inode::write_bytes(off_t offset, size_t length, UserOrKernelBuffer const& target_buffer, OpenFileDescription* open_description)
{
    MutexLocker locker(m_inode_lock);
    return prepare_and_write_bytes_locked(offset, length, target_buffer, open_description);
}

ErrorOr<size_t> Inode::prepare_and_write_bytes_locked(off_t offset, size_t length, UserOrKernelBuffer const& target_buffer, OpenFileDescription* open_description)
{
    VERIFY(m_inode_lock.is_locked());
    TRY(prepare_to_write_data());
    return write_bytes_locked(offset, length, target_buffer, open_description);
}

ErrorOr<size_t> Inode::read_bytes(off_t offset, size_t length, UserOrKernelBuffer& buffer, OpenFileDescription* open_description) const
{
    MutexLocker locker(m_inode_lock, Mutex::Mode::Shared);
    return read_bytes_locked(offset, length, buffer, open_description);
}

ErrorOr<size_t> Inode::read_until_filled_or_end(off_t offset, size_t length, UserOrKernelBuffer buffer, OpenFileDescription* open_description) const
{
    auto remaining_length = length;

    while (remaining_length > 0) {
        auto filled_bytes = TRY(read_bytes(offset, remaining_length, buffer, open_description));
        if (filled_bytes == 0)
            break;
        offset += filled_bytes;
        remaining_length -= filled_bytes;
    }

    return length - remaining_length;
}

ErrorOr<void> Inode::update_timestamps([[maybe_unused]] Optional<UnixDateTime> atime, [[maybe_unused]] Optional<UnixDateTime> ctime, [[maybe_unused]] Optional<UnixDateTime> mtime)
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

LockRefPtr<LocalSocket> Inode::bound_socket() const
{
    return m_bound_socket.strong_ref();
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
    // FIXME: What happens if this fails?
    //        ENOTIMPL would be a meaningless error to return here
    auto now = kgettimeofday();
    (void)update_timestamps({}, now, now);

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
    VERIFY(m_inode_lock.is_locked());
    if (fs().is_readonly())
        return EROFS;
    auto metadata = this->metadata();
    if (metadata.is_setuid() || metadata.is_setgid()) {
        dbgln("Inode::prepare_to_write_data(): Stripping SUID/SGID bits from {}", identifier());
        return chmod(metadata.mode & ~(04000 | 02000));
    }
    return {};
}

LockRefPtr<Memory::SharedInodeVMObject> Inode::shared_vmobject() const
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

bool Inode::can_apply_flock(flock const& new_lock, Optional<OpenFileDescription const&> description) const
{
    VERIFY(new_lock.l_whence == SEEK_SET);

    if (new_lock.l_type == F_UNLCK)
        return true;

    return m_flocks.with([&](auto& flocks) {
        for (auto const& lock : flocks) {
            if (!range_overlap(lock.start, lock.len, new_lock.l_start, new_lock.l_len))
                continue;

            // There are two cases where we can attempt downgrade:
            //
            // 1) We're the owner of this lock. The downgrade will immediately
            //    succeed.
            // 2) We're not the owner of this lock. Our downgrade attempt will
            //    fail, and the thread will start blocking on an FlockBlocker.
            //
            // For the first case, we get the description from try_apply_flock
            // below. For the second case, the check below would always be
            // false, so there is no need to store the description in the
            // blocker in the first place.
            if (new_lock.l_type == F_RDLCK && lock.type == F_WRLCK)
                return description.has_value() && lock.owner == &description.value() && lock.start == new_lock.l_start && lock.len == new_lock.l_len;

            if (new_lock.l_type == F_WRLCK)
                return false;
        }
        return true;
    });
}

ErrorOr<bool> Inode::try_apply_flock(Process const& process, OpenFileDescription const& description, flock const& new_lock)
{
    return m_flocks.with([&](auto& flocks) -> ErrorOr<bool> {
        if (!can_apply_flock(new_lock, description))
            return false;

        bool did_manipulate_lock = false;
        for (size_t i = 0; i < flocks.size(); ++i) {
            auto const& lock = flocks[i];

            bool is_potential_downgrade = new_lock.l_type == F_RDLCK && lock.type == F_WRLCK;
            bool is_potential_unlock = new_lock.l_type == F_UNLCK;

            bool is_lock_owner = &description == lock.owner;
            bool lock_range_exactly_matches = lock.start == new_lock.l_start && lock.len == new_lock.l_len;
            bool can_manage_this_lock = is_lock_owner && lock_range_exactly_matches;

            if ((is_potential_downgrade || is_potential_unlock) && can_manage_this_lock) {
                flocks.remove(i);
                did_manipulate_lock = true;
                break;
            }
        }

        if (new_lock.l_type != F_UNLCK)
            TRY(flocks.try_append(Flock { new_lock.l_start, new_lock.l_len, &description, process.pid().value(), new_lock.l_type }));

        if (did_manipulate_lock)
            m_flock_blocker_set.unblock_all_blockers_whose_conditions_are_met();

        // Judging by the Linux implementation, unlocking a non-existent lock
        // also works.
        return true;
    });
}

ErrorOr<void> Inode::apply_flock(Process const& process, OpenFileDescription const& description, Userspace<flock const*> input_lock, ShouldBlock should_block)
{
    auto new_lock = TRY(copy_typed_from_user(input_lock));
    TRY(normalize_flock(description, new_lock));

    while (true) {
        auto success = TRY(try_apply_flock(process, description, new_lock));
        if (success)
            return {};

        if (should_block == ShouldBlock::No)
            return EAGAIN;

        if (Thread::current()->block<Thread::FlockBlocker>({}, *this, new_lock).was_interrupted())
            return EINTR;
    }
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

            // Locks with the same owner can't conflict with each other.
            if (lock.pid == Process::current().pid())
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
