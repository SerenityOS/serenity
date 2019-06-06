#include <Kernel/FileSystem/FIFO.h>
#include <Kernel/FileSystem/FileDescriptor.h>
#include <Kernel/Lock.h>
#include <AK/StdLibExtras.h>
#include <AK/HashTable.h>

//#define FIFO_DEBUG

Lockable<HashTable<FIFO*>>& all_fifos()
{
    static Lockable<HashTable<FIFO*>>* s_table;
    if (!s_table)
        s_table = new Lockable<HashTable<FIFO*>>;
    return *s_table;
}

RetainPtr<FIFO> FIFO::from_fifo_id(dword id)
{
    auto* ptr = reinterpret_cast<FIFO*>(id);
    LOCKER(all_fifos().lock());
    if (auto it = all_fifos().resource().find(ptr); it == all_fifos().resource().end())
        return nullptr;
    return ptr;
}

Retained<FIFO> FIFO::create(uid_t uid)
{
    return adopt(*new FIFO(uid));
}

Retained<FileDescriptor> FIFO::open_direction(FIFO::Direction direction)
{
    auto descriptor = FileDescriptor::create(this);
    attach(direction);
    descriptor->set_fifo_direction({ }, direction);
    return descriptor;
}

FIFO::FIFO(uid_t uid)
    : m_uid(uid)
{
    LOCKER(all_fifos().lock());
    all_fifos().resource().set(this);
}

FIFO::~FIFO()
{
    LOCKER(all_fifos().lock());
    all_fifos().resource().remove(this);
}

void FIFO::attach(Direction direction)
{
    if (direction == Direction::Reader) {
        ++m_readers;
#ifdef FIFO_DEBUG
        kprintf("open reader (%u)\n", m_readers);
#endif
    } else if (direction == Direction::Writer) {
        ++m_writers;
#ifdef FIFO_DEBUG
        kprintf("open writer (%u)\n", m_writers);
#endif
    }
}

void FIFO::detach(Direction direction)
{
    if (direction == Direction::Reader) {
#ifdef FIFO_DEBUG
        kprintf("close reader (%u - 1)\n", m_readers);
#endif
        ASSERT(m_readers);
        --m_readers;
    } else if (direction == Direction::Writer) {
#ifdef FIFO_DEBUG
        kprintf("close writer (%u - 1)\n", m_writers);
#endif
        ASSERT(m_writers);
        --m_writers;
    }
}

bool FIFO::can_read(FileDescriptor&) const
{
    return !m_buffer.is_empty() || !m_writers;
}

bool FIFO::can_write(FileDescriptor&) const
{
    return m_buffer.bytes_in_write_buffer() < 4096 || !m_readers;
}

ssize_t FIFO::read(FileDescriptor&, byte* buffer, ssize_t size)
{
    if (!m_writers && m_buffer.is_empty())
        return 0;
#ifdef FIFO_DEBUG
    dbgprintf("fifo: read(%u)\n",size);
#endif
    ssize_t nread = m_buffer.read(buffer, size);
#ifdef FIFO_DEBUG
    dbgprintf("   -> read (%c) %u\n", buffer[0], nread);
#endif
    return nread;
}

ssize_t FIFO::write(FileDescriptor&, const byte* buffer, ssize_t size)
{
    if (!m_readers)
        return -EPIPE;
#ifdef FIFO_DEBUG
    dbgprintf("fifo: write(%p, %u)\n", buffer, size);
#endif
    return m_buffer.write(buffer, size);
}

String FIFO::absolute_path(const FileDescriptor&) const
{
    return String::format("fifo:%u", this);
}
