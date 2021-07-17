/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/HashTable.h>
#include <AK/Singleton.h>
#include <AK/StdLibExtras.h>
#include <Kernel/FileSystem/FIFO.h>
#include <Kernel/FileSystem/FileDescription.h>
#include <Kernel/Mutex.h>
#include <Kernel/Process.h>
#include <Kernel/Thread.h>

namespace Kernel {

static AK::Singleton<Lockable<HashTable<FIFO*>>> s_table;

static Lockable<HashTable<FIFO*>>& all_fifos()
{
    return *s_table;
}

static int s_next_fifo_id = 1;

NonnullRefPtr<FIFO> FIFO::create(uid_t uid)
{
    return adopt_ref(*new FIFO(uid));
}

KResultOr<NonnullRefPtr<FileDescription>> FIFO::open_direction(FIFO::Direction direction)
{
    auto description = FileDescription::create(*this);
    if (!description.is_error()) {
        attach(direction);
        description.value()->set_fifo_direction({}, direction);
    }
    return description;
}

KResultOr<NonnullRefPtr<FileDescription>> FIFO::open_direction_blocking(FIFO::Direction direction)
{
    Locker locker(m_open_lock);

    auto description = open_direction(direction);
    if (description.is_error())
        return description;

    if (direction == Direction::Reader) {
        m_read_open_queue.wake_all();

        if (m_writers == 0) {
            locker.unlock();
            m_write_open_queue.wait_forever("FIFO");
            locker.lock();
        }
    }

    if (direction == Direction::Writer) {
        m_write_open_queue.wake_all();

        if (m_readers == 0) {
            locker.unlock();
            m_read_open_queue.wait_forever("FIFO");
            locker.lock();
        }
    }

    return description;
}

FIFO::FIFO(uid_t uid)
    : m_uid(uid)
{
    Locker locker(all_fifos().lock());
    all_fifos().resource().set(this);
    m_fifo_id = ++s_next_fifo_id;

    // Use the same block condition for read and write
    m_buffer.set_unblock_callback([this]() {
        evaluate_block_conditions();
    });
}

FIFO::~FIFO()
{
    Locker locker(all_fifos().lock());
    all_fifos().resource().remove(this);
}

void FIFO::attach(Direction direction)
{
    if (direction == Direction::Reader) {
        ++m_readers;
    } else if (direction == Direction::Writer) {
        ++m_writers;
    }

    evaluate_block_conditions();
}

void FIFO::detach(Direction direction)
{
    if (direction == Direction::Reader) {
        VERIFY(m_readers);
        --m_readers;
    } else if (direction == Direction::Writer) {
        VERIFY(m_writers);
        --m_writers;
    }

    evaluate_block_conditions();
}

bool FIFO::can_read(const FileDescription&, size_t) const
{
    return !m_buffer.is_empty() || !m_writers;
}

bool FIFO::can_write(const FileDescription&, size_t) const
{
    return m_buffer.space_for_writing() || !m_readers;
}

KResultOr<size_t> FIFO::read(FileDescription&, u64, UserOrKernelBuffer& buffer, size_t size)
{
    if (!m_writers && m_buffer.is_empty())
        return 0;
    return m_buffer.read(buffer, size);
}

KResultOr<size_t> FIFO::write(FileDescription&, u64, const UserOrKernelBuffer& buffer, size_t size)
{
    if (!m_readers) {
        Thread::current()->send_signal(SIGPIPE, Process::current());
        return EPIPE;
    }

    return m_buffer.write(buffer, size);
}

String FIFO::absolute_path(const FileDescription&) const
{
    return String::formatted("fifo:{}", m_fifo_id);
}

KResult FIFO::stat(::stat& st) const
{
    memset(&st, 0, sizeof(st));
    st.st_mode = S_IFIFO;
    return KSuccess;
}

}
