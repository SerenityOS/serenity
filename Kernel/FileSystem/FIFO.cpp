/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Atomic.h>
#include <AK/StdLibExtras.h>
#include <Kernel/FileSystem/FIFO.h>
#include <Kernel/FileSystem/OpenFileDescription.h>
#include <Kernel/Locking/Mutex.h>
#include <Kernel/Tasks/Process.h>
#include <Kernel/Tasks/Thread.h>

namespace Kernel {

static Atomic<int> s_next_fifo_id = 1;

ErrorOr<NonnullRefPtr<FIFO>> FIFO::try_create(UserID uid)
{
    auto buffer = TRY(DoubleBuffer::try_create("FIFO: Buffer"sv));
    return adopt_nonnull_ref_or_enomem(new (nothrow) FIFO(uid, move(buffer)));
}

ErrorOr<NonnullRefPtr<OpenFileDescription>> FIFO::open_direction(FIFO::Direction direction)
{
    auto description = TRY(OpenFileDescription::try_create(*this));
    if (direction == Direction::Reader) {
        ++m_readers;
    } else if (direction == Direction::Writer) {
        ++m_writers;
    }
    evaluate_block_conditions();
    description->set_fifo_direction({}, direction);
    return description;
}

ErrorOr<NonnullRefPtr<OpenFileDescription>> FIFO::open_direction_blocking(FIFO::Direction direction)
{
    MutexLocker locker(m_open_lock);

    auto description = TRY(open_direction(direction));

    if (direction == Direction::Reader) {
        m_read_open_queue.wake_all();

        if (m_writers == 0) {
            locker.unlock();
            m_write_open_queue.wait_forever("FIFO"sv);
            locker.lock();
        }
    }

    if (direction == Direction::Writer) {
        m_write_open_queue.wake_all();

        if (m_readers == 0) {
            locker.unlock();
            m_read_open_queue.wait_forever("FIFO"sv);
            locker.lock();
        }
    }

    return description;
}

FIFO::FIFO(UserID uid, NonnullOwnPtr<DoubleBuffer> buffer)
    : m_buffer(move(buffer))
    , m_uid(uid)
{
    m_fifo_id = ++s_next_fifo_id;

    // Use the same block condition for read and write
    m_buffer->set_unblock_callback([this]() {
        evaluate_block_conditions();
    });
}

FIFO::~FIFO() = default;

void FIFO::detach(OpenFileDescription& description)
{
    File::detach(description);

    auto direction = description.fifo_direction();
    if (direction == Direction::Reader) {
        VERIFY(m_readers);
        --m_readers;
    } else if (direction == Direction::Writer) {
        VERIFY(m_writers);
        --m_writers;
    }

    evaluate_block_conditions();
}

bool FIFO::can_read(OpenFileDescription const&, u64) const
{
    return !m_buffer->is_empty() || !m_writers;
}

bool FIFO::can_write(OpenFileDescription const&, u64) const
{
    return m_buffer->space_for_writing() || !m_readers;
}

ErrorOr<size_t> FIFO::read(OpenFileDescription& fd, u64, UserOrKernelBuffer& buffer, size_t size)
{
    if (m_buffer->is_empty()) {
        if (!m_writers)
            return 0;
        if (!fd.is_blocking())
            return EAGAIN;
    }
    return m_buffer->read(buffer, size);
}

ErrorOr<size_t> FIFO::write(OpenFileDescription& fd, u64, UserOrKernelBuffer const& buffer, size_t size)
{
    if (!m_readers)
        return EPIPE;
    if (!fd.is_blocking() && m_buffer->space_for_writing() == 0)
        return EAGAIN;

    return m_buffer->write(buffer, size);
}

ErrorOr<NonnullOwnPtr<KString>> FIFO::pseudo_path(OpenFileDescription const&) const
{
    return KString::formatted("fifo:{}", m_fifo_id);
}

ErrorOr<struct stat> FIFO::stat() const
{
    struct stat st = {};
    st.st_mode = S_IFIFO;
    return st;
}

}
