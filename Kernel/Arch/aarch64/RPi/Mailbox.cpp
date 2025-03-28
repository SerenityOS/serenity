/*
 * Copyright (c) 2021, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/aarch64/ASM_wrapper.h>
#include <Kernel/Arch/aarch64/RPi/Mailbox.h>
#include <Kernel/Memory/MemoryManager.h>

namespace Kernel::RPi {

// There's one mailbox at MBOX_BASE_OFFSET for reading responses from VideoCore, and one at MBOX_BASE_OFFSET + 0x20 for sending requests.
// Each has its own status word.

struct MailboxRegisters {
    u32 read_data;
    u32 reserved0[3];
    u32 read_poll;
    u32 read_sender;
    u32 read_status;
    u32 read_config;

    u32 write_data;
    u32 reserved1[5];
    u32 write_status;
};
static_assert(AssertSize<MailboxRegisters, 60>());

constexpr u32 MBOX_RESPONSE_SUCCESS = 0x8000'0000;
constexpr u32 MBOX_RESPONSE_PARTIAL = 0x8000'0001;
constexpr u32 MBOX_REQUEST = 0;
constexpr u32 MBOX_FULL = 0x8000'0000;
constexpr u32 MBOX_EMPTY = 0x4000'0000;

constexpr int ARM_TO_VIDEOCORE_CHANNEL = 8;

// We have to use a raw pointer here because this variable will be set before global constructors are called.
static Mailbox* s_the = nullptr;

Mailbox::Mailbox(Memory::TypedMapping<MailboxRegisters volatile> registers)
    : m_registers(move(registers))
{
}

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

ErrorOr<void> Mailbox::initialize(PhysicalAddress physical_address)
{
    auto registers = TRY(Memory::map_typed_writable<MailboxRegisters volatile>(physical_address));
    s_the = new (nothrow) Mailbox(move(registers));
    if (s_the == nullptr)
        return ENOMEM;
    return {};
}

bool Mailbox::is_initialized()
{
    return s_the != nullptr;
}

Mailbox& Mailbox::the()
{
    VERIFY(is_initialized());
    return *s_the;
}

void Mailbox::wait_until_we_can_write() const
{
    // Since nothing else writes to the mailbox, this wait is mostly cargo-culted.
    // Most baremetal tutorials on the internet query MBOX_READ_STATUS here, which I think is incorrect and only works because this wait really isn't needed.
    while ((m_registers->write_status & MBOX_FULL) != 0)
        Processor::wait_check();
}

void Mailbox::wait_for_reply() const
{
    while ((m_registers->read_status & MBOX_EMPTY) != 0)
        Processor::wait_check();
}

bool Mailbox::send_queue(void* queue, u32 queue_size)
{
    // According to Raspberry Pi specs this is the only channel implemented.
    u32 const channel = ARM_TO_VIDEOCORE_CHANNEL;

    auto message_header = reinterpret_cast<MessageHeader*>(queue);
    message_header->set_queue_size(queue_size);

    // The mailbox interface has a FIFO for message delivery in both directions.
    // Responses can be delivered out of order to requests, but we currently ever only send on request at once.
    // It'd be nice to have an async interface here where we send a message, then return immediately, and read the response when an interrupt arrives.
    // But for now, this is synchronous.

    wait_until_we_can_write();

    // The mailbox message is 32-bit based, so this assumes that message is in the first 4 GiB.
    // FIXME: Memory::virtual_to_low_physical only works for the initial kernel mappings (including the stack).
    //        Sending mailbox messages that are on the stack (which is most of them) won't work as soon as we enter init_stage2.
    //        We should instead use MM DMA functions to allocate memory for transferring messages.
    u32 queue_paddr = Memory::virtual_to_low_physical(bit_cast<FlatPtr>(queue));
    u32 request = static_cast<u32>(queue_paddr & ~0xF) | (channel & 0xF);

    // The queue buffer might point to normal cached memory, so flush any writes that are in cache and not visible to VideoCore.
    Aarch64::Asm::flush_data_cache((FlatPtr)queue, queue_size);

    m_registers->write_data = request;

    for (;;) {
        wait_for_reply();

        u32 response = m_registers->read_data;
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

}
