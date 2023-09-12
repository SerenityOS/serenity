/*
 * Copyright (c) 2021, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/aarch64/ASM_wrapper.h>
#include <Kernel/Arch/aarch64/RPi/MMIO.h>
#include <Kernel/Arch/aarch64/RPi/Mailbox.h>
#include <Kernel/Library/Panic.h>

namespace Kernel::RPi {

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
        Processor::wait_check();
}

static void wait_for_reply(MMIO& mmio)
{
    while (mmio.read(MBOX_READ_STATUS) & MBOX_EMPTY)
        Processor::wait_check();
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

    // The queue buffer might point to normal cached memory, so flush any writes that are in cache and not visible to VideoCore.
    Aarch64::Asm::flush_data_cache((FlatPtr)queue, queue_size);

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

class QueryFirmwareVersionMboxMessage : RPi::Mailbox::Message {
public:
    u32 version;

    QueryFirmwareVersionMboxMessage()
        : RPi::Mailbox::Message(0x0000'0001, 4)
    {
        version = 0;
    }
};

u32 Mailbox::query_firmware_version()
{
    struct __attribute__((aligned(16))) {
        MessageHeader header;
        QueryFirmwareVersionMboxMessage query_firmware_version;
        MessageTail tail;
    } message_queue;

    if (!the().send_queue(&message_queue, sizeof(message_queue))) {
        return 0xffff'ffff;
    }

    return message_queue.query_firmware_version.version;
}

// https://github.com/raspberrypi/firmware/wiki/Mailbox-property-interface#get-command-line
//
// Note: This function is called very early in the boot process, before the heap or the console
//       is initialized. Please ensure that it does the minimum amount of work possible.
StringView Mailbox::query_kernel_command_line(Bytes buffer)
{
    // We want to use the user-provided buffer rather than a fixed-size one on the stack,
    // so we need to construct the message manually.
    auto aligned_buffer = buffer.align_to(16);
    if (aligned_buffer.size() < 24)
        return ""sv;

    auto max_response_length = aligned_buffer.size() - 24;

    auto* message = reinterpret_cast<u32*>(aligned_buffer.data());
    message[0] = aligned_buffer.size();
    message[1] = MBOX_REQUEST;

    message[2] = 0x0005'0001; // Query command line
    message[3] = max_response_length;
    message[4] = max_response_length;

    message[aligned_buffer.size() / sizeof(u32) - 1] = 0;

    if (!the().send_queue(message, aligned_buffer.size()))
        return ""sv;

    // Bit 31 indicates that this is a response, the rest denote the length.
    auto response_length = message[4] & 0x7fff'ffff;

    if (response_length > max_response_length)
        return ""sv; // The buffer was too small to hold the response.

    return StringView { (char const*)&message[5], response_length };
}

class QueryARMMemoryMailboxMessage : RPi::Mailbox::Message {
public:
    u32 base;
    u32 size;

    QueryARMMemoryMailboxMessage()
        : RPi::Mailbox::Message(0x0001'0005, 8)
    {
        base = 0;
        size = 0;
    }
};

Mailbox::MemoryRange Mailbox::query_lower_arm_memory_range()
{
    struct __attribute__((aligned(16))) {
        MessageHeader header;
        QueryARMMemoryMailboxMessage query_arm_memory;
        MessageTail tail;
    } message_queue;

    if (!the().send_queue(&message_queue, sizeof(message_queue))) {
        PANIC("Failed to determine the available RAM range");
    }

    return { message_queue.query_arm_memory.base, message_queue.query_arm_memory.size };
}

class QueryVCMemoryMailboxMessage : RPi::Mailbox::Message {
public:
    u32 base;
    u32 size;

    QueryVCMemoryMailboxMessage()
        : RPi::Mailbox::Message(0x0001'0006, 8)
    {
        base = 0;
        size = 0;
    }
};

Mailbox::MemoryRange Mailbox::query_videocore_memory_range()
{
    struct __attribute__((aligned(16))) {
        MessageHeader header;
        QueryVCMemoryMailboxMessage query_vc_memory;
        MessageTail tail;
    } message_queue;

    if (!the().send_queue(&message_queue, sizeof(message_queue))) {
        PANIC("Failed to determine the VideoCore memory range");
    }

    return { message_queue.query_vc_memory.base, message_queue.query_vc_memory.size };
}

}
