/*
 * Copyright (c) 2021, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Prekernel/Arch/aarch64/MMIO.h>
#include <Kernel/Prekernel/Arch/aarch64/Mailbox.h>

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

bool Mailbox::call(u8 channel, u32 volatile* __attribute__((aligned(16))) message)
{
    auto& mmio = MMIO::the();

    // The mailbox interface has a FIFO for message deliverly in both directions.
    // Responses can be delivered out of order to requests, but we currently ever only send on request at once.
    // It'd be nice to have an async interface here where we send a message, then return immediately, and read the response when an interrupt arrives.
    // But for now, this is synchronous.

    wait_until_we_can_write(mmio);

    // The mailbox message is 32-bit based, so this assumes that message is in the first 4 GiB.
    u32 request = static_cast<u32>(reinterpret_cast<FlatPtr>(message) & ~0xF) | (channel & 0xF);
    mmio.write(MBOX_WRITE_DATA, request);

    for (;;) {
        wait_for_reply(mmio);

        u32 response = mmio.read(MBOX_READ_DATA);
        // We keep at most one message in flight and do synchronous communication, so response will always be == request for us.
        if (response == request)
            return message[1] == MBOX_RESPONSE_SUCCESS;
    }

    return true;
}

constexpr u32 MBOX_TAG_GET_FIRMWARE_VERSION = 0x0000'0001;

u32 Mailbox::query_firmware_version()
{
    // See https://github.com/raspberrypi/firmware/wiki/Mailbox-property-interface for data format.
    u32 __attribute__((aligned(16))) message[7];
    message[0] = sizeof(message);
    message[1] = MBOX_REQUEST;

    message[2] = MBOX_TAG_GET_FIRMWARE_VERSION;
    message[3] = 0; // Tag data size. MBOX_TAG_GET_FIRMWARE_VERSION needs no arguments.
    message[4] = MBOX_REQUEST;
    message[5] = 0; // Trailing zero for request, room for data in response.

    message[6] = 0; // Room for trailing zero in response.

    if (call(ARM_TO_VIDEOCORE_CHANNEL, message) && message[2] == MBOX_TAG_GET_FIRMWARE_VERSION)
        return message[5];

    return 0xffff'ffff;
}

}
