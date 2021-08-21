/*
 * Copyright (c) 2021, the SerenityOS developers.
 * Copyright (c) 2021, Kyle Pereira <hey@xylepereira.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Bus/VirtIO/VirtIOConsole.h>
#include <Kernel/Bus/VirtIO/VirtIOConsolePort.h>

namespace Kernel {

unsigned VirtIOConsolePort::next_device_id = 0;

VirtIOConsolePort::VirtIOConsolePort(unsigned port, VirtIOConsole& console)
    : CharacterDevice(229, next_device_id++)
    , m_console(console)
    , m_port(port)
{
    m_receive_buffer = make<Memory::RingBuffer>("VirtIOConsolePort Receive", RINGBUFFER_SIZE);
    m_transmit_buffer = make<Memory::RingBuffer>("VirtIOConsolePort Transmit", RINGBUFFER_SIZE);
    m_receive_queue = m_port == 0 ? 0 : m_port * 2 + 2;
    m_transmit_queue = m_port == 0 ? 1 : m_port * 2 + 3;
    init_receive_buffer();
}

void VirtIOConsolePort::init_receive_buffer()
{
    auto& queue = m_console.get_queue(m_receive_queue);
    SpinlockLocker queue_lock(queue.lock());
    VirtIOQueueChain chain(queue);

    auto buffer_start = m_receive_buffer->start_of_region();
    auto did_add_buffer = chain.add_buffer_to_chain(buffer_start, RINGBUFFER_SIZE, BufferType::DeviceWritable);
    VERIFY(did_add_buffer);
    m_console.supply_chain_and_notify(m_receive_queue, chain);
}

void VirtIOConsolePort::handle_queue_update(Badge<VirtIOConsole>, u16 queue_index)
{
    dbgln_if(VIRTIO_DEBUG, "VirtIOConsolePort: Handle queue update for port {}", m_port);
    VERIFY(queue_index == m_transmit_queue || queue_index == m_receive_queue);
    if (queue_index == m_receive_queue) {
        auto& queue = m_console.get_queue(m_receive_queue);
        SpinlockLocker queue_lock(queue.lock());
        size_t used;
        VirtIOQueueChain popped_chain = queue.pop_used_buffer_chain(used);

        SpinlockLocker ringbuffer_lock(m_receive_buffer->lock());
        auto used_space = m_receive_buffer->reserve_space(used).value();
        auto remaining_space = m_receive_buffer->bytes_till_end();

        // Our algorithm always has only one buffer in the queue.
        VERIFY(popped_chain.length() == 1);
        VERIFY(!queue.new_data_available());
        popped_chain.release_buffer_slots_to_queue();

        VirtIOQueueChain new_chain(queue);
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
        VirtIOQueueChain popped_chain = queue.pop_used_buffer_chain(used);
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

bool VirtIOConsolePort::can_read(const FileDescription&, size_t) const
{
    return m_receive_buffer->used_bytes() > 0;
}

KResultOr<size_t> VirtIOConsolePort::read(FileDescription& desc, u64, UserOrKernelBuffer& buffer, size_t size)
{
    if (!size)
        return 0;

    SpinlockLocker ringbuffer_lock(m_receive_buffer->lock());

    if (!can_read(desc, size))
        return EAGAIN;

    auto bytes_copied = m_receive_buffer->copy_data_out(size, buffer).value();
    m_receive_buffer->reclaim_space(m_receive_buffer->start_of_used(), bytes_copied);

    if (m_receive_buffer_exhausted && m_receive_buffer->used_bytes() == 0) {
        auto& queue = m_console.get_queue(m_receive_queue);
        SpinlockLocker queue_lock(queue.lock());
        VirtIOQueueChain new_chain(queue);
        new_chain.add_buffer_to_chain(m_receive_buffer->start_of_region(), RINGBUFFER_SIZE, BufferType::DeviceWritable);
        m_console.supply_chain_and_notify(m_receive_queue, new_chain);
        m_receive_buffer_exhausted = false;
    }

    return bytes_copied;
}

bool VirtIOConsolePort::can_write(const FileDescription&, size_t) const
{
    return m_console.get_queue(m_transmit_queue).has_free_slots() && m_transmit_buffer->has_space();
}

KResultOr<size_t> VirtIOConsolePort::write(FileDescription& desc, u64, const UserOrKernelBuffer& data, size_t size)
{
    if (!size)
        return 0;

    SpinlockLocker ringbuffer_lock(m_transmit_buffer->lock());
    auto& queue = m_console.get_queue(m_transmit_queue);
    SpinlockLocker queue_lock(queue.lock());

    if (!can_write(desc, size))
        return EAGAIN;

    VirtIOQueueChain chain(queue);

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

String VirtIOConsolePort::device_name() const
{
    return String::formatted("hvc{}p{}", m_console.device_id(), m_port);
}

KResultOr<NonnullRefPtr<FileDescription>> VirtIOConsolePort::open(int options)
{
    if (!m_open)
        m_console.send_open_control_message(m_port, true);

    return File::open(options);
}

}
