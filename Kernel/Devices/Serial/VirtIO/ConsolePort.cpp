/*
 * Copyright (c) 2021, the SerenityOS developers.
 * Copyright (c) 2021, Kyle Pereira <hey@xylepereira.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/API/MajorNumberAllocation.h>
#include <Kernel/Devices/Serial/VirtIO/Console.h>
#include <Kernel/Devices/Serial/VirtIO/ConsolePort.h>

namespace Kernel::VirtIO {

unsigned ConsolePort::next_device_id = 0;

ErrorOr<NonnullRefPtr<ConsolePort>> ConsolePort::create(unsigned port, Console& console)
{
    auto receive_buffer = TRY(Memory::RingBuffer::try_create("VirtIO::ConsolePort Receive"sv, RINGBUFFER_SIZE));
    auto transmit_buffer = TRY(Memory::RingBuffer::try_create("VirtIO::ConsolePort Transmit"sv, RINGBUFFER_SIZE));
    return TRY(Device::try_create_device<ConsolePort>(port, console, move(receive_buffer), move(transmit_buffer)));
}

ConsolePort::ConsolePort(unsigned port, VirtIO::Console& console, NonnullOwnPtr<Memory::RingBuffer> receive_buffer, NonnullOwnPtr<Memory::RingBuffer> transmit_buffer)
    : CharacterDevice(MajorAllocation::CharacterDeviceFamily::VirtIOConsole, next_device_id++)
    , m_receive_buffer(move(receive_buffer))
    , m_transmit_buffer(move(transmit_buffer))
    , m_console(console)
    , m_port(port)
{
    m_receive_queue = m_port == 0 ? 0 : m_port * 2 + 2;
    m_transmit_queue = m_port == 0 ? 1 : m_port * 2 + 3;
}

void ConsolePort::init_receive_buffer(Badge<VirtIO::Console>)
{
    auto& queue = m_console.get_queue(m_receive_queue);
    SpinlockLocker queue_lock(queue.lock());
    QueueChain chain(queue);

    auto buffer_start = m_receive_buffer->start_of_region();
    auto did_add_buffer = chain.add_buffer_to_chain(buffer_start, RINGBUFFER_SIZE, BufferType::DeviceWritable);
    VERIFY(did_add_buffer);
    m_console.supply_chain_and_notify(m_receive_queue, chain);
}

void ConsolePort::handle_queue_update(Badge<VirtIO::Console>, u16 queue_index)
{
    dbgln_if(VIRTIO_DEBUG, "VirtIO::ConsolePort: Handle queue update for port {}", m_port);
    VERIFY(queue_index == m_transmit_queue || queue_index == m_receive_queue);
    if (queue_index == m_receive_queue) {
        auto& queue = m_console.get_queue(m_receive_queue);
        SpinlockLocker queue_lock(queue.lock());
        size_t used;
        QueueChain popped_chain = queue.pop_used_buffer_chain(used);

        SpinlockLocker ringbuffer_lock(m_receive_buffer->lock());
        auto used_space_or_error = m_receive_buffer->reserve_space(used);
        if (used_space_or_error.is_error()) {
            TODO();
        }
        auto used_space = used_space_or_error.release_value();
        auto remaining_space = m_receive_buffer->bytes_till_end();

        // Our algorithm always has only one buffer in the queue.
        VERIFY(popped_chain.length() == 1);
        VERIFY(!queue.new_data_available());
        popped_chain.release_buffer_slots_to_queue();

        QueueChain new_chain(queue);
        if (remaining_space != 0) {
            new_chain.add_buffer_to_chain(used_space.offset(used), remaining_space, BufferType::DeviceWritable);
            m_console.supply_chain_and_notify(m_receive_queue, new_chain);
        } else {
            m_receive_buffer_exhausted = true;
        }

        evaluate_block_conditions();
    } else {
        SpinlockLocker ringbuffer_lock(m_transmit_buffer->lock());
        auto& queue = m_console.get_queue(m_transmit_queue);
        SpinlockLocker queue_lock(queue.lock());
        size_t used;
        QueueChain popped_chain = queue.pop_used_buffer_chain(used);
        do {
            popped_chain.for_each([this](PhysicalAddress address, size_t length) {
                m_transmit_buffer->reclaim_space(address, length);
            });
            popped_chain.release_buffer_slots_to_queue();
            popped_chain = queue.pop_used_buffer_chain(used);
        } while (!popped_chain.is_empty());
        // Unblock any IO tasks that were blocked because can_write() returned false
        evaluate_block_conditions();
    }
}

bool ConsolePort::can_read(OpenFileDescription const&, u64) const
{
    return m_receive_buffer->used_bytes() > 0;
}

ErrorOr<size_t> ConsolePort::read(OpenFileDescription& desc, u64, UserOrKernelBuffer& buffer, size_t size)
{
    if (!size)
        return 0;

    SpinlockLocker ringbuffer_lock(m_receive_buffer->lock());

    if (!can_read(desc, size))
        return EAGAIN;

    auto bytes_copied = TRY(m_receive_buffer->copy_data_out(size, buffer));
    m_receive_buffer->reclaim_space(m_receive_buffer->start_of_used(), bytes_copied);

    if (m_receive_buffer_exhausted && m_receive_buffer->used_bytes() == 0) {
        auto& queue = m_console.get_queue(m_receive_queue);
        SpinlockLocker queue_lock(queue.lock());
        QueueChain new_chain(queue);
        new_chain.add_buffer_to_chain(m_receive_buffer->start_of_region(), RINGBUFFER_SIZE, BufferType::DeviceWritable);
        m_console.supply_chain_and_notify(m_receive_queue, new_chain);
        m_receive_buffer_exhausted = false;
    }

    return bytes_copied;
}

bool ConsolePort::can_write(OpenFileDescription const&, u64) const
{
    return m_console.get_queue(m_transmit_queue).has_free_slots() && m_transmit_buffer->has_space();
}

ErrorOr<size_t> ConsolePort::write(OpenFileDescription& desc, u64, UserOrKernelBuffer const& data, size_t size)
{
    if (!size)
        return 0;

    SpinlockLocker ringbuffer_lock(m_transmit_buffer->lock());
    auto& queue = m_console.get_queue(m_transmit_queue);
    SpinlockLocker queue_lock(queue.lock());

    if (!can_write(desc, size))
        return EAGAIN;

    QueueChain chain(queue);

    size_t total_bytes_copied = 0;
    do {
        PhysicalAddress start_of_chunk;
        size_t length_of_chunk;

        if (!m_transmit_buffer->copy_data_in(data, total_bytes_copied, size - total_bytes_copied, start_of_chunk, length_of_chunk)) {
            chain.release_buffer_slots_to_queue();
            return EINVAL;
        }

        bool did_add_buffer = chain.add_buffer_to_chain(start_of_chunk, length_of_chunk, BufferType::DeviceReadable);
        VERIFY(did_add_buffer);
        total_bytes_copied += length_of_chunk;
    } while (total_bytes_copied < size && can_write(desc, size));

    m_console.supply_chain_and_notify(m_transmit_queue, chain);

    return total_bytes_copied;
}

ErrorOr<NonnullRefPtr<OpenFileDescription>> ConsolePort::open(int options)
{
    if (!m_open)
        m_console.send_open_control_message(m_port, true);

    return Device::open(options);
}

}
