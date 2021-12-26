/*
 * Copyright (c) 2021, the SerenityOS developers.
 * Copyright (c) 2021, Kyle Pereira <hey@xylepereira.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Bus/VirtIO/VirtIOConsole.h>
#include <Kernel/Sections.h>

namespace Kernel::VirtIO {

unsigned Console::next_device_id = 0;

UNMAP_AFTER_INIT Console::Console(PCI::Address address)
    : VirtIO::Device(address)
    , m_device_id(next_device_id++)
{
    if (auto cfg = get_config(ConfigurationType::Device)) {
        bool success = negotiate_features([&](u64 supported_features) {
            u64 negotiated = 0;
            if (is_feature_set(supported_features, VIRTIO_CONSOLE_F_SIZE))
                dbgln("VirtIO::Console: Console size is not yet supported!");
            if (is_feature_set(supported_features, VIRTIO_CONSOLE_F_MULTIPORT))
                negotiated |= VIRTIO_CONSOLE_F_MULTIPORT;
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
                    m_ports.resize(max_nr_ports);
                }
            });
            dbgln("VirtIO::Console: cols: {}, rows: {}, max nr ports {}", cols, rows, max_nr_ports);
            // Base receiveq/transmitq for port0 + optional control queues and 2 per every additional port
            success = setup_queues(2 + max_nr_ports > 0 ? 2 + 2 * max_nr_ports : 0);
        }
        if (success) {
            finish_init();

            if (is_feature_accepted(VIRTIO_CONSOLE_F_MULTIPORT))
                setup_multiport();
            else
                m_ports.append(new VirtIO::ConsolePort(0u, *this));
        }
    }
}

bool Console::handle_device_config_change()
{
    dbgln("VirtIO::Console: Handle device config change");
    return true;
}

void Console::handle_queue_update(u16 queue_index)
{
    dbgln_if(VIRTIO_DEBUG, "VirtIO::Console: Handle queue update {}", queue_index);

    if (queue_index == CONTROL_RECEIVEQ) {
        SpinlockLocker ringbuffer_lock(m_control_receive_buffer->lock());
        auto& queue = get_queue(CONTROL_RECEIVEQ);
        SpinlockLocker queue_lock(queue.lock());
        size_t used;
        QueueChain popped_chain = queue.pop_used_buffer_chain(used);

        while (!popped_chain.is_empty()) {
            popped_chain.for_each([&](auto addr, auto) {
                auto offset = addr.as_ptr() - m_control_receive_buffer->start_of_region().as_ptr();
                auto* message = reinterpret_cast<ControlMessage*>(m_control_receive_buffer->vaddr().offset(offset).as_ptr());
                process_control_message(*message);
            });

            supply_chain_and_notify(CONTROL_RECEIVEQ, popped_chain);
            popped_chain = queue.pop_used_buffer_chain(used);
        }
    } else if (queue_index == CONTROL_TRANSMITQ) {
        SpinlockLocker ringbuffer_lock(m_control_transmit_buffer->lock());
        auto& queue = get_queue(CONTROL_TRANSMITQ);
        SpinlockLocker queue_lock(queue.lock());
        size_t used;
        QueueChain popped_chain = queue.pop_used_buffer_chain(used);
        auto number_of_messages = 0;
        do {
            popped_chain.for_each([this](PhysicalAddress address, size_t length) {
                m_control_transmit_buffer->reclaim_space(address, length);
            });
            popped_chain.release_buffer_slots_to_queue();
            popped_chain = queue.pop_used_buffer_chain(used);
            number_of_messages++;
        } while (!popped_chain.is_empty());
        m_control_wait_queue.wake_n(number_of_messages);
    } else {
        u32 port_index = queue_index < 2 ? 0 : (queue_index - 2) / 2;
        if (port_index >= m_ports.size() || !m_ports.at(port_index)) {
            dbgln("Invalid queue_index {}", queue_index);
            return;
        }
        m_ports.at(port_index)->handle_queue_update({}, queue_index);
    }
}

void Console::setup_multiport()
{
    m_control_receive_buffer = make<Memory::RingBuffer>("VirtIOConsole control receive queue", CONTROL_BUFFER_SIZE);
    m_control_transmit_buffer = make<Memory::RingBuffer>("VirtIOConsole control transmit queue", CONTROL_BUFFER_SIZE);

    auto& queue = get_queue(CONTROL_RECEIVEQ);
    SpinlockLocker queue_lock(queue.lock());
    QueueChain chain(queue);
    auto offset = 0ul;

    while (offset < CONTROL_BUFFER_SIZE) {
        auto buffer_start = m_control_receive_buffer->start_of_region().offset(offset);
        auto did_add_buffer = chain.add_buffer_to_chain(buffer_start, CONTROL_MESSAGE_SIZE, BufferType::DeviceWritable);
        VERIFY(did_add_buffer);
        offset += CONTROL_MESSAGE_SIZE;
        supply_chain_and_notify(CONTROL_RECEIVEQ, chain);
    }

    ControlMessage ready_event {
        .id = 0, // Unused
        .event = (u16)ControlEvent::DeviceReady,
        .value = (u16)ControlMessage::Status::Success
    };
    write_control_message(ready_event);
}

void Console::process_control_message(ControlMessage message)
{
    switch (message.event) {
    case (u16)ControlEvent::DeviceAdd: {
        u32 id = message.id;
        if (id >= m_ports.size()) {
            dbgln("Device provided an invalid port number {}. max_nr_ports: {}", id, m_ports.size());
            return;
        } else if (!m_ports.at(id).is_null()) {
            dbgln("Device tried to add port {} which was already added!", id);
            return;
        }

        m_ports.at(id) = new VirtIO::ConsolePort(id, *this);
        ControlMessage ready_event {
            .id = static_cast<u32>(id),
            .event = (u16)ControlEvent::PortReady,
            .value = (u16)ControlMessage::Status::Success
        };

        write_control_message(ready_event);
        break;
    }
    case (u16)ControlEvent::ConsolePort:
    case (u16)ControlEvent::PortOpen: {
        if (message.id >= m_ports.size()) {
            dbgln("Device provided an invalid port number {}. max_nr_ports: {}", message.id, m_ports.size());
            return;
        } else if (m_ports.at(message.id).is_null()) {
            dbgln("Device tried to open port {} which was not added!", message.id);
            return;
        }

        if (message.value == (u16)ControlMessage::PortStatus::Open) {
            auto is_open = m_ports.at(message.id)->is_open();
            if (!is_open) {
                m_ports.at(message.id)->set_open({}, true);
                send_open_control_message(message.id, true);
            }
        } else if (message.value == (u16)ControlMessage::PortStatus::Close) {
            m_ports.at(message.id)->set_open({}, false);
        } else {
            dbgln("Device specified invalid value {}. Must be 0 or 1.", message.value);
        }
        break;
    }
    default:
        dbgln("Unhandled message event {}!", message.event);
    }
}
void Console::write_control_message(ControlMessage message)
{
    SpinlockLocker ringbuffer_lock(m_control_transmit_buffer->lock());

    PhysicalAddress start_of_chunk;
    size_t length_of_chunk;

    auto data = UserOrKernelBuffer::for_kernel_buffer((u8*)&message);
    while (!m_control_transmit_buffer->copy_data_in(data, 0, sizeof(message), start_of_chunk, length_of_chunk)) {
        ringbuffer_lock.unlock();
        m_control_wait_queue.wait_forever();
        ringbuffer_lock.lock();
    }

    auto& queue = get_queue(CONTROL_TRANSMITQ);
    SpinlockLocker queue_lock(queue.lock());
    QueueChain chain(queue);

    bool did_add_buffer = chain.add_buffer_to_chain(start_of_chunk, length_of_chunk, BufferType::DeviceReadable);
    VERIFY(did_add_buffer);

    supply_chain_and_notify(CONTROL_TRANSMITQ, chain);
}

void Console::send_open_control_message(unsigned port_number, bool open)
{
    ControlMessage port_open {
        .id = static_cast<u32>(port_number),
        .event = (u16)ControlEvent::PortOpen,
        .value = open
    };
    write_control_message(port_open);
}
}
