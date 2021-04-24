/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/VirtIO/VirtIOConsole.h>

namespace Kernel {

unsigned VirtIOConsole::next_device_id = 0;

VirtIOConsole::VirtIOConsole(PCI::Address address)
    : CharacterDevice(229, next_device_id++)
    , VirtIODevice(address, "VirtIOConsole")
{
    if (auto cfg = get_config(ConfigurationType::Device)) {
        bool success = negotiate_features([&](u64 supported_features) {
            u64 negotiated = 0;
            if (is_feature_set(supported_features, VIRTIO_CONSOLE_F_SIZE))
                dbgln("VirtIOConsole: Console size is not yet supported!");
            if (is_feature_set(supported_features, VIRTIO_CONSOLE_F_MULTIPORT))
                dbgln("VirtIOConsole: Multi port is not yet supported!");
            return negotiated;
        });
        if (success) {
            u32 max_nr_ports = 0;
            u16 cols = 0, rows = 0;
            read_config_atomic([&]() {
                if (is_feature_accepted(VIRTIO_CONSOLE_F_SIZE)) {
                    cols = config_read16(*cfg, 0x0);
                    rows = config_read16(*cfg, 0x2);
                }
                if (is_feature_accepted(VIRTIO_CONSOLE_F_MULTIPORT)) {
                    max_nr_ports = config_read32(*cfg, 0x4);
                }
            });
            dbgln("VirtIOConsole: cols: {}, rows: {}, max nr ports {}", cols, rows, max_nr_ports);
            success = setup_queues(2 + max_nr_ports * 2); // base receiveq/transmitq for port0 + 2 per every additional port
        }
        if (success) {
            finish_init();
            m_receive_buffer = make<RingBuffer>("VirtIOConsole Receive", RINGBUFFER_SIZE);
            m_transmit_buffer = make<RingBuffer>("VirtIOConsole Transmit", RINGBUFFER_SIZE);
        }
    }
}

VirtIOConsole::~VirtIOConsole()
{
}

bool VirtIOConsole::handle_device_config_change()
{
    dbgln("VirtIOConsole: Handle device config change");
    return true;
}

void VirtIOConsole::handle_queue_update(u16 queue_index)
{
    dbgln_if(VIRTIO_DEBUG, "VirtIOConsole: Handle queue update");
    VERIFY(queue_index <= TRANSMITQ);
    switch (queue_index) {
    case RECEIVEQ: {
        ScopedSpinLock lock(get_queue(RECEIVEQ).lock());
        get_queue(RECEIVEQ).discard_used_buffers(); // TODO: do something with incoming data (users writing into qemu console) instead of just clearing
        break;
    }
    case TRANSMITQ: {
        ScopedSpinLock ringbuffer_lock(m_transmit_buffer->lock());
        auto& queue = get_queue(TRANSMITQ);
        ScopedSpinLock queue_lock(queue.lock());
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
        break;
    }
    default:
        VERIFY_NOT_REACHED();
    }
}

bool VirtIOConsole::can_read(const FileDescription&, size_t) const
{
    return true;
}

KResultOr<size_t> VirtIOConsole::read(FileDescription&, u64, [[maybe_unused]] UserOrKernelBuffer& data, size_t)
{
    return ENOTSUP;
}

bool VirtIOConsole::can_write(const FileDescription&, size_t) const
{
    return get_queue(TRANSMITQ).has_free_slots() && m_transmit_buffer->has_space();
}

KResultOr<size_t> VirtIOConsole::write(FileDescription& desc, u64, const UserOrKernelBuffer& data, size_t size)
{
    if (!size)
        return 0;

    if (!can_write(desc, size))
        return EAGAIN;

    ScopedSpinLock ringbuffer_lock(m_transmit_buffer->lock());
    auto& queue = get_queue(TRANSMITQ);
    ScopedSpinLock queue_lock(queue.lock());
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
    } while (total_bytes_copied < size && can_write(desc, size - total_bytes_copied));

    supply_chain_and_notify(TRANSMITQ, chain);

    return total_bytes_copied;
}

}
