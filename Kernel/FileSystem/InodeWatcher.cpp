/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <AK/Memory.h>
#include <Kernel/FileSystem/Inode.h>
#include <Kernel/FileSystem/InodeWatcher.h>

namespace Kernel {

NonnullRefPtr<InodeWatcher> InodeWatcher::create(Inode& inode)
{
    return adopt(*new InodeWatcher(inode));
}

InodeWatcher::InodeWatcher(Inode& inode)
    : m_inode(inode.make_weak_ptr())
{
    inode.register_watcher({}, *this);
}

InodeWatcher::~InodeWatcher()
{
    if (RefPtr<Inode> safe_inode = m_inode.ptr())
        safe_inode->unregister_watcher({}, *this);
}

bool InodeWatcher::can_read(const FileDescription&, size_t) const
{
    return !m_queue.is_empty() || !m_inode;
}

bool InodeWatcher::can_write(const FileDescription&, size_t) const
{
    return true;
}

KResultOr<size_t> InodeWatcher::read(FileDescription&, size_t, UserOrKernelBuffer& buffer, size_t buffer_size)
{
    LOCKER(m_lock);
    ASSERT(!m_queue.is_empty() || !m_inode);

    if (!m_inode)
        return 0;

    auto event = m_queue.dequeue();

    if (buffer_size < sizeof(Event))
        return buffer_size;

    size_t bytes_to_write = min(buffer_size, sizeof(event));

    ssize_t nwritten = buffer.write_buffered<sizeof(event)>(bytes_to_write, [&](u8* data, size_t data_bytes) {
        memcpy(data, &event, bytes_to_write);
        return (ssize_t)data_bytes;
    });
    if (nwritten < 0)
        return KResult(nwritten);
    return bytes_to_write;
}

KResultOr<size_t> InodeWatcher::write(FileDescription&, size_t, const UserOrKernelBuffer&, size_t)
{
    return KResult(-EIO);
}

String InodeWatcher::absolute_path(const FileDescription&) const
{
    if (!m_inode)
        return "InodeWatcher:(gone)";
    return String::format("InodeWatcher:%s", m_inode->identifier().to_string().characters());
}

void InodeWatcher::notify_inode_event(Badge<Inode>, Event::Type event_type)
{
    LOCKER(m_lock);
    m_queue.enqueue({ event_type });
}

void InodeWatcher::notify_child_added(Badge<Inode>, const InodeIdentifier& child_id)
{
    LOCKER(m_lock);
    m_queue.enqueue({ Event::Type::ChildAdded, child_id.index() });
}

void InodeWatcher::notify_child_removed(Badge<Inode>, const InodeIdentifier& child_id)
{
    LOCKER(m_lock);
    m_queue.enqueue({ Event::Type::ChildRemoved, child_id.index() });
}

}
