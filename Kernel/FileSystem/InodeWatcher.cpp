#include <Kernel/FileSystem/Inode.h>
#include <Kernel/FileSystem/InodeWatcher.h>

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

bool InodeWatcher::can_read(FileDescription&) const
{
    return !m_queue.is_empty() || !m_inode;
}

bool InodeWatcher::can_write(FileDescription&) const
{
    return true;
}

ssize_t InodeWatcher::read(FileDescription&, u8* buffer, ssize_t buffer_size)
{
    ASSERT(!m_queue.is_empty() || !m_inode);

    if (!m_inode)
        return 0;

    // FIXME: What should we do if the output buffer is too small?
    ASSERT(buffer_size >= (int)sizeof(Event));
    auto event = m_queue.dequeue();
    memcpy(buffer, &event, sizeof(event));
    return sizeof(event);
}

ssize_t InodeWatcher::write(FileDescription&, const u8*, ssize_t)
{
    return -EIO;
}

String InodeWatcher::absolute_path(const FileDescription&) const
{
    if (!m_inode)
        return "InodeWatcher:(gone)";
    return String::format("InodeWatcher:%s", m_inode->identifier().to_string().characters());
}

void InodeWatcher::notify_inode_event(Badge<Inode>, Event::Type event_type)
{
    m_queue.enqueue({ event_type });
}
