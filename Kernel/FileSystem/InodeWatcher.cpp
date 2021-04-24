/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Memory.h>
#include <Kernel/FileSystem/Inode.h>
#include <Kernel/FileSystem/InodeWatcher.h>

namespace Kernel {

NonnullRefPtr<InodeWatcher> InodeWatcher::create(Inode& inode)
{
    return adopt_ref(*new InodeWatcher(inode));
}

InodeWatcher::InodeWatcher(Inode& inode)
    : m_inode(inode)
{
    inode.register_watcher({}, *this);
}

InodeWatcher::~InodeWatcher()
{
    if (auto inode = m_inode.strong_ref())
        inode->unregister_watcher({}, *this);
}

bool InodeWatcher::can_read(const FileDescription&, size_t) const
{
    return !m_queue.is_empty() || !m_inode;
}

bool InodeWatcher::can_write(const FileDescription&, size_t) const
{
    return true;
}

KResultOr<size_t> InodeWatcher::read(FileDescription&, u64, UserOrKernelBuffer& buffer, size_t buffer_size)
{
    Locker locker(m_lock);
    VERIFY(!m_queue.is_empty() || !m_inode);

    if (!m_inode)
        return 0;

    auto event = m_queue.dequeue();

    if (buffer_size < sizeof(InodeWatcherEvent))
        return buffer_size;

    size_t bytes_to_write = min(buffer_size, sizeof(event));

    ssize_t nwritten = buffer.write_buffered<sizeof(event)>(bytes_to_write, [&](u8* data, size_t data_bytes) {
        memcpy(data, &event, bytes_to_write);
        return (ssize_t)data_bytes;
    });
    if (nwritten < 0)
        return KResult((ErrnoCode)-nwritten);
    evaluate_block_conditions();
    return bytes_to_write;
}

KResultOr<size_t> InodeWatcher::write(FileDescription&, u64, const UserOrKernelBuffer&, size_t)
{
    return EIO;
}

String InodeWatcher::absolute_path(const FileDescription&) const
{
    if (auto inode = m_inode.strong_ref())
        return String::formatted("InodeWatcher:{}", inode->identifier().to_string());
    return "InodeWatcher:(gone)";
}

void InodeWatcher::notify_inode_event(Badge<Inode>, InodeWatcherEvent::Type event_type)
{
    Locker locker(m_lock);
    m_queue.enqueue({ event_type });
    evaluate_block_conditions();
}

void InodeWatcher::notify_child_added(Badge<Inode>, const InodeIdentifier& child_id)
{
    Locker locker(m_lock);
    m_queue.enqueue({ InodeWatcherEvent::Type::ChildAdded, child_id.index().value() });
    evaluate_block_conditions();
}

void InodeWatcher::notify_child_removed(Badge<Inode>, const InodeIdentifier& child_id)
{
    Locker locker(m_lock);
    m_queue.enqueue({ InodeWatcherEvent::Type::ChildRemoved, child_id.index().value() });
    evaluate_block_conditions();
}

}
