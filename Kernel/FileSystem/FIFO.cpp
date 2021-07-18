/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Atomic.h>
#include <AK/HashTable.h>
#include <AK/Singleton.h>
#include <AK/StdLibExtras.h>
#include <Kernel/FileSystem/FIFO.h>
#include <Kernel/FileSystem/FileDescription.h>
#include <Kernel/Locking/Mutex.h>
#include <Kernel/Locking/ProtectedValue.h>
#include <Kernel/Process.h>
#include <Kernel/Thread.h>

namespace Kernel {

static AK::Singleton<ProtectedValue<HashTable<FIFO*>>> s_table;

static ProtectedValue<HashTable<FIFO*>>& all_fifos()
{
    return *s_table;
}

static Atomic<int> s_next_fifo_id = 1;

RefPtr<FIFO> FIFO::try_create(uid_t uid)
{
    auto buffer = DoubleBuffer::try_create();
    if (buffer)
        return adopt_ref_if_nonnull(new (nothrow) FIFO(uid, buffer.release_nonnull()));
    return {};
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
    MutexLocker locker(m_open_lock);

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

FIFO::FIFO(uid_t uid, NonnullOwnPtr<DoubleBuffer> buffer)
    : m_buffer(move(buffer))
    , m_uid(uid)
{
    all_fifos().with_exclusive([&](auto& table) {
        table.set(this);
    });
    m_fifo_id = ++s_next_fifo_id;

    // Use the same block condition for read and write
    m_buffer->set_unblock_callback([this]() {
        evaluate_block_conditions();
    });
}

FIFO::~FIFO()
{
    all_fifos().with_exclusive([&](auto& table) {
        table.remove(this);
    });
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
    return !m_buffer->is_empty() || !m_writers;
}

bool FIFO::can_write(const FileDescription&, size_t) const
{
    return m_buffer->space_for_writing() || !m_readers;
}

KResultOr<size_t> FIFO::read(FileDescription&, u64, UserOrKernelBuffer& buffer, size_t size)
{
    if (!m_writers && m_buffer->is_empty())
        return 0;
    return m_buffer->read(buffer, size);
}

KResultOr<size_t> FIFO::write(FileDescription&, u64, const UserOrKernelBuffer& buffer, size_t size)
{
    if (!m_readers) {
        Thread::current()->send_signal(SIGPIPE, Process::current());
        return EPIPE;
    }

    return m_buffer->write(buffer, size);
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
