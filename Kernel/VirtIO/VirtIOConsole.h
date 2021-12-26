/*
 * Copyright (c) 2021, Kyle Pereira <hey@xylepereira.me>
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/VM/RingBuffer.h>
#include <Kernel/VirtIO/VirtIO.h>
#include <Kernel/VirtIO/VirtIOConsolePort.h>

namespace Kernel {
class VirtIOConsole
    : public VirtIODevice
    , public RefCounted<VirtIOConsole> {
    friend VirtIOConsolePort;

public:
    VirtIOConsole(PCI::Address);
    virtual ~VirtIOConsole() override = default;

    virtual StringView purpose() const override { return "VirtIOConsole"; }

    unsigned device_id() const
    {
        return m_device_id;
    }

private:
    enum class ControlEvent : u16 {
        DeviceReady = 0,
        DeviceAdd = 1,
        PortReady = 3,
        ConsolePort = 4,
        PortOpen = 6,
    };
    struct [[gnu::packed]] ControlMessage {
        u32 id;
        u16 event;
        u16 value;

        enum class Status : u16 {
            Success = 1,
            Failure = 0
        };

        enum class PortStatus : u16 {
            Open = 1,
            Close = 0
        };
    };

    constexpr static u16 CONTROL_RECEIVEQ = 2;
    constexpr static u16 CONTROL_TRANSMITQ = 3;
    constexpr static size_t CONTROL_MESSAGE_SIZE = sizeof(ControlMessage);
    constexpr static size_t CONTROL_BUFFER_SIZE = CONTROL_MESSAGE_SIZE * 32;

    virtual bool handle_device_config_change() override;
    virtual void handle_queue_update(u16 queue_index) override;

    Vector<RefPtr<VirtIOConsolePort>> m_ports;
    void setup_multiport();
    void process_control_message(ControlMessage message);
    void write_control_message(ControlMessage message);
    void send_open_control_message(unsigned port_number, bool open);

    unsigned m_device_id;

    OwnPtr<RingBuffer> m_control_transmit_buffer;
    OwnPtr<RingBuffer> m_control_receive_buffer;

    WaitQueue m_control_wait_queue;

    static unsigned next_device_id;
};
}
