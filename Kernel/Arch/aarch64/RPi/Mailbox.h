/*
 * Copyright (c) 2021, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/StringView.h>
#include <AK/Types.h>
#include <Kernel/Memory/TypedMapping.h>

namespace Kernel::RPi {

struct MailboxRegisters;

// Can exchange mailbox messages with the Raspberry Pi's VideoCore chip.
// https://github.com/raspberrypi/firmware/wiki/Mailbox-property-interface
class Mailbox {
public:
    Mailbox();

    // Base class for Mailbox messages. Implemented in subsystems that use Mailbox.
    class Message {
    protected:
        Message(u32 tag, u32 arguments_size);

    private:
        u32 m_tag;
        u32 m_arguments_size;
        u32 m_command_tag;
    };

    // Must be at the beginning of every command message queue
    class MessageHeader {
    public:
        MessageHeader();

        u32 queue_size() { return m_message_queue_size; }
        void set_queue_size(u32 size) { m_message_queue_size = size; }
        bool success() const;

    private:
        u32 m_message_queue_size;
        u32 m_command_tag;
    };

    // Must be at the end of every command message queue
    class MessageTail {
    private:
        u32 m_empty_tag = 0;
    };

    static void initialize();
    static bool is_initialized();
    static Mailbox& the();

    // Sends message queue to VideoCore
    bool send_queue(void* queue, u32 queue_size);

    u32 query_firmware_version();

    Memory::TypedMapping<MailboxRegisters volatile> m_registers;

private:
    void wait_until_we_can_write() const;
    void wait_for_reply() const;
};

}
