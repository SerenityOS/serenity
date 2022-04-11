/*
 * Copyright (c) 2021, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/aarch64/RPi/MMIO.h>
#include <Kernel/Arch/aarch64/RPi/Mailbox.h>

namespace Prekernel {

// There's one mailbox at MBOX_BASE_OFFSET for reading responses from VideoCore, and one at MBOX_BASE_OFFSET + 0x20 for sending requests.
// Each has its own status word.

constexpr u32 MBOX_BASE_OFFSET = 0xB880;
constexpr u32 MBOX_0 = MBOX_BASE_OFFSET;
constexpr u32 MBOX_1 = MBOX_BASE_OFFSET + 0x20;

constexpr u32 MBOX_READ_DATA = MBOX_0;
constexpr u32 MBOX_READ_POLL = MBOX_0 + 0x10;
constexpr u32 MBOX_READ_SENDER = MBOX_0 + 0x14;
constexpr u32 MBOX_READ_STATUS = MBOX_0 + 0x18;
constexpr u32 MBOX_READ_CONFIG = MBOX_0 + 0x1C;

constexpr u32 MBOX_WRITE_DATA = MBOX_1;
constexpr u32 MBOX_WRITE_STATUS = MBOX_1 + 0x18;

constexpr u32 MBOX_RESPONSE_SUCCESS = 0x8000'0000;
constexpr u32 MBOX_RESPONSE_PARTIAL = 0x8000'0001;
constexpr u32 MBOX_REQUEST = 0;
constexpr u32 MBOX_FULL = 0x8000'0000;
constexpr u32 MBOX_EMPTY = 0x4000'0000;

constexpr int ARM_TO_VIDEOCORE_CHANNEL = 8;

Mailbox::Message::Message(u32 tag, u32 arguments_size)
{
    m_tag = tag;
    m_arguments_size = arguments_size;
    m_command_tag = MBOX_REQUEST;
}

Mailbox::MessageHeader::MessageHeader()
{
    m_message_queue_size = 0;
    m_command_tag = MBOX_REQUEST;
}

bool Mailbox::MessageHeader::success() const
{
    return m_command_tag == MBOX_RESPONSE_SUCCESS;
}

Mailbox& Mailbox::the()
{
    static Mailbox instance;
    return instance;
}

static void wait_until_we_can_write(MMIO& mmio)
{
    // Since nothing else writes to the mailbox, this wait is mostly cargo-culted.
    // Most baremetal tutorials on the internet query MBOX_READ_STATUS here, which I think is incorrect and only works because this wait really isn't needed.
    while (mmio.read(MBOX_WRITE_STATUS) & MBOX_FULL)
        ;
}

static void wait_for_reply(MMIO& mmio)
{
    while (mmio.read(MBOX_READ_STATUS) & MBOX_EMPTY)
        ;
}

bool Mailbox::send_queue(void* queue, u32 queue_size) const
{
    // According to Raspberry Pi specs this is the only channel implemented.
    const u32 channel = ARM_TO_VIDEOCORE_CHANNEL;

    auto message_header = reinterpret_cast<MessageHeader*>(queue);
    message_header->set_queue_size(queue_size);

    auto& mmio = MMIO::the();

    // The mailbox interface has a FIFO for message delivery in both directions.
    // Responses can be delivered out of order to requests, but we currently ever only send on request at once.
    // It'd be nice to have an async interface here where we send a message, then return immediately, and read the response when an interrupt arrives.
    // But for now, this is synchronous.

    wait_until_we_can_write(mmio);

    // The mailbox message is 32-bit based, so this assumes that message is in the first 4 GiB.
    u32 request = static_cast<u32>(reinterpret_cast<FlatPtr>(queue) & ~0xF) | (channel & 0xF);
    mmio.write(MBOX_WRITE_DATA, request);

    for (;;) {
        wait_for_reply(mmio);

        u32 response = mmio.read(MBOX_READ_DATA);
        // We keep at most one message in flight and do synchronous communication, so response will always be == request for us.
        if (response == request)
            return message_header->success();
    }

    return true;
}

}
