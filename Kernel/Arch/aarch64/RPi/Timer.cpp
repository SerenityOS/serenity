/*
 * Copyright (c) 2021, Nico Weber <thakis@chromium.org>
 * Copyright (c) 2022, Timon Kruiper <timonkruiper@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>
#include <AK/NeverDestroyed.h>
#include <Kernel/Arch/aarch64/RPi/MMIO.h>
#include <Kernel/Arch/aarch64/RPi/Mailbox.h>
#include <Kernel/Arch/aarch64/RPi/Timer.h>
#include <Kernel/Firmware/DeviceTree/DeviceTree.h>
#include <Kernel/Firmware/DeviceTree/Driver.h>
#include <Kernel/Firmware/DeviceTree/Management.h>

namespace Kernel::RPi {

// "12.1 System Timer Registers" / "10.2 System Timer Registers"
struct TimerRegisters {
    u32 control_and_status;
    u32 counter_low;
    u32 counter_high;
    u32 compare[4];
};

// Bits of the `control_and_status` register.
// See "CS register" in Broadcom doc for details.
enum FlagBits {
    SystemTimerMatch0 = 1 << 0,
    SystemTimerMatch1 = 1 << 1,
    SystemTimerMatch2 = 1 << 2,
    SystemTimerMatch3 = 1 << 3,
};

Timer::Timer(Memory::TypedMapping<TimerRegisters volatile> registers_mapping, InterruptNumber interrupt_number)
    : HardwareTimer(interrupt_number)
    , TimeSource(TimeSource::CounterSize::Bits64)
    , m_registers(move(registers_mapping))
{
    // FIXME: Actually query the frequency of the timer. By default it is 100MHz.
    m_frequency = 1e6;

    set_interrupt_interval_usec(m_frequency / OPTIMAL_TICKS_PER_SECOND_RATE);
    enable_interrupt_mode();
}

Timer::~Timer() = default;

bool Timer::handle_irq()
{
    auto result = HardwareTimer::handle_irq();

    set_compare(TimerID::Timer1, read_counter() + m_interrupt_interval);
    clear_interrupt(TimerID::Timer1);

    return result;
}

void Timer::disable()
{
    disable_irq();
}

u64 Timer::read_counter_raw() const
{
    u32 high = m_registers->counter_high;
    u32 low = m_registers->counter_low;
    if (high != m_registers->counter_high) {
        high = m_registers->counter_high;
        low = m_registers->counter_low;
    }
    return (static_cast<u64>(high) << 32) | low;
}

size_t Timer::frequency() const
{
    return m_frequency;
}

void Timer::enable_interrupt_mode()
{
    set_compare(TimerID::Timer1, read_counter() + m_interrupt_interval);
    enable_irq();
}

void Timer::set_interrupt_interval_usec(u32 interrupt_interval)
{
    m_interrupt_interval = interrupt_interval;
}

void Timer::clear_interrupt(TimerID id)
{
    m_registers->control_and_status = 1 << to_underlying(id);
}

void Timer::set_compare(TimerID id, u32 compare)
{
    m_registers->compare[to_underlying(id)] = compare;
}

class SetClockRateMboxMessage : Mailbox::Message {
public:
    u32 clock_id;
    u32 rate_hz;
    u32 skip_setting_turbo;

    SetClockRateMboxMessage()
        : Mailbox::Message(0x0003'8002, 12)
    {
        clock_id = 0;
        rate_hz = 0;
        skip_setting_turbo = 0;
    }
};

u32 Timer::set_clock_rate(ClockID clock_id, u32 rate_hz, bool skip_setting_turbo)
{
    struct {
        Mailbox::MessageHeader header;
        SetClockRateMboxMessage set_clock_rate;
        Mailbox::MessageTail tail;
    } message_queue;

    message_queue.set_clock_rate.clock_id = static_cast<u32>(clock_id);
    message_queue.set_clock_rate.rate_hz = rate_hz;
    message_queue.set_clock_rate.skip_setting_turbo = skip_setting_turbo ? 1 : 0;

    if (!Mailbox::the().send_queue(&message_queue, sizeof(message_queue))) {
        dbgln("Timer::set_clock_rate() failed!");
        return 0;
    }

    return message_queue.set_clock_rate.rate_hz;
}

class GetClockRateMboxMessage : Mailbox::Message {
public:
    u32 clock_id;
    u32 rate_hz;

    GetClockRateMboxMessage()
        : Mailbox::Message(0x0003'0002, 8)
    {
        clock_id = 0;
        rate_hz = 0;
    }
};

u32 Timer::get_clock_rate(ClockID clock_id)
{
    struct {
        Mailbox::MessageHeader header;
        GetClockRateMboxMessage get_clock_rate;
        Mailbox::MessageTail tail;
    } message_queue;

    message_queue.get_clock_rate.clock_id = static_cast<u32>(clock_id);

    if (!Mailbox::the().send_queue(&message_queue, sizeof(message_queue))) {
        dbgln("Timer::get_clock_rate() failed!");
        return 0;
    }

    return message_queue.get_clock_rate.rate_hz;
}

static constinit Array const compatibles_array = {
    "brcm,bcm2835-system-timer"sv,
};

EARLY_DEVICETREE_DRIVER(BCM2835TimerDriver, compatibles_array);

// https://www.kernel.org/doc/Documentation/devicetree/bindings/timer/brcm,bcm2835-system-timer.txt
ErrorOr<void> BCM2835TimerDriver::probe(DeviceTree::Device const& device, StringView) const
{
    auto physical_address = TRY(device.get_resource(0)).paddr;
    auto registers_mapping = TRY(Memory::map_typed_writable<TimerRegisters volatile>(physical_address));

    // This driver currently only uses channel 1.
    auto interrupt_number = TRY(device.get_interrupt_number(1));

    auto timer = TRY(adopt_nonnull_lock_ref_or_enomem(new (nothrow) Timer(move(registers_mapping), interrupt_number)));

    MUST(TimeManagement::the().register_hardware_timer(move(timer)));
    MUST(TimeManagement::the().register_time_source(move(timer)));

    return {};
}

}
