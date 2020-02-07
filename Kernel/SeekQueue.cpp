#include <Kernel/SeekQueue.h>
#include <Kernel/Thread.h>

void SeekQueue::insert_request(Thread& thread, u32 namespace_id, u16 priority, u32 lbal, u32 lbah, u32 block_count, u16 flags, u8* buf)
{
    m_seek_requests.append(new SeekQueueEntry(entry_counter, namespace_id, priority, lbal, lbah, block_count, flags, buf, thread));
    entry_counter++;
}
void SeekQueue::process_request()
{
}
void SeekQueue::wake_one()
{
    m_seek_requests.take_first()->get_thread().unblock();
}
void SeekQueue::clear_all()
{
    wake_all();
    m_seek_requests.clear();
}
void SeekQueue::wake_all()
{
    AK::SinglyLinkedList<SeekQueueEntry*>::Iterator iterator = m_seek_requests.begin();
    while (!iterator.is_end) {
        SeekQueueEntry* entry = *iterator;
        if (entry != nullptr) {
            entry->get_thread().unblock();
        }
        ++iterator;
    }
}

SeekQueueEntry::SeekQueueEntry(u32 entry_id, u32 namespace_id, u16 priority, u32 lbal, u32 lbah, u32 block_count, u16 flags, u8* buf, Thread& thread)
    : m_blocked_thread(thread)
    , m_entry_id(entry_id)
    , m_namespace_id(namespace_id)
    , m_priority(priority)
    , m_lbal(lbal)
    , m_lbah(lbah)
    , m_block_count(block_count)
    , m_flags(flags)
    , m_buf(buf)
{
}

u32 SeekQueueEntry::get_entry_id()
{
    return m_entry_id;
}
u32 SeekQueueEntry::get_namespace_id()
{
    return m_namespace_id;
}
u32 SeekQueueEntry::get_priority()
{
    return m_priority;
}
u32 SeekQueueEntry::get_lba_low()
{
    return m_lbal;
}
u32 SeekQueueEntry::get_lba_high()
{
    return m_lbah;
}
u32 SeekQueueEntry::get_block_count()
{
    return m_block_count;
}
u16 SeekQueueEntry::get_flags()
{
    return m_flags;
}
u8* SeekQueueEntry::get_io_buffer()
{
    return m_buf;
}
Thread& SeekQueueEntry::get_thread()
{
    return m_blocked_thread;
}
