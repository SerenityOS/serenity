/*
 * Copyright (c) 2023, kleines Filmröllchen <filmroellchen@serenityos.org>.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "HardwareClocks.h"
#include <AK/Format.h>
#include <Kernel/Arch/aarch64/RPi/Mailbox.h>

namespace Kernel::HardwareClocks {

class SetClockRateMboxMessage : RPi::Mailbox::Message {
public:
    u32 clock_id;
    u32 rate_hz;
    u32 skip_setting_turbo;

    SetClockRateMboxMessage()
        : RPi::Mailbox::Message(0x0003'8002, 12)
    {
        clock_id = 0;
        rate_hz = 0;
        skip_setting_turbo = 0;
    }
};

u32 set_clock_rate(ClockID clock_id, u32 rate_hz, bool skip_setting_turbo)
{
    struct __attribute__((aligned(16))) {
        RPi::Mailbox::MessageHeader header;
        SetClockRateMboxMessage set_clock_rate;
        RPi::Mailbox::MessageTail tail;
    } message_queue;

    message_queue.set_clock_rate.clock_id = static_cast<u32>(clock_id);
    message_queue.set_clock_rate.rate_hz = rate_hz;
    message_queue.set_clock_rate.skip_setting_turbo = skip_setting_turbo ? 1 : 0;

    if (!RPi::Mailbox::the().send_queue(&message_queue, sizeof(message_queue))) {
        dbgln("Timer::set_clock_rate() failed!");
        return 0;
    }

    return message_queue.set_clock_rate.rate_hz;
}

class GetClockRateMboxMessage : RPi::Mailbox::Message {
public:
    u32 clock_id;
    u32 rate_hz;

    GetClockRateMboxMessage()
        : RPi::Mailbox::Message(0x0003'0002, 8)
    {
        clock_id = 0;
        rate_hz = 0;
    }
};

u32 get_clock_rate(ClockID clock_id)
{
    struct __attribute__((aligned(16))) {
        RPi::Mailbox::MessageHeader header;
        GetClockRateMboxMessage get_clock_rate;
        RPi::Mailbox::MessageTail tail;
    } message_queue;

    message_queue.get_clock_rate.clock_id = static_cast<u32>(clock_id);

    if (!RPi::Mailbox::the().send_queue(&message_queue, sizeof(message_queue))) {
        dbgln("Timer::get_clock_rate() failed!");
        return 0;
    }

    return message_queue.get_clock_rate.rate_hz;
}

}
