/*
 * Copyright (c) 2026, Sönke Holz <soenke.holz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/NeverDestroyed.h>
#include <Kernel/Devices/Time/GoldfishRTC.h>
#include <Kernel/Firmware/DeviceTree/Driver.h>
#include <Kernel/Firmware/DeviceTree/Management.h>

namespace Kernel {

// III. Goldfish real-time clock (RTC) https://android.googlesource.com/platform/external/qemu/+/master/docs/GOLDFISH-VIRTUAL-HARDWARE.TXT

struct GoldfishRTC::Registers {
    u32 time_low;        // 0x00  TIME_LOW         R: Get current time, then return low-order 32-bits.
    u32 time_high;       // 0x04  TIME_HIGH        R: Return high 32-bits from previous TIME_LOW read.
    u32 alarm_low;       // 0x08  ALARM_LOW        W: Set low 32-bit value of alarm, then arm it.
    u32 alarm_high;      // 0x0c  ALARM_HIGH       W: Set high 32-bit value of alarm.
    u32 clear_interrupt; // 0x10  CLEAR_INTERRUPT  W: Lower device's irq level.
};
static_assert(AssertSize<GoldfishRTC::Registers, 0x14>());

static NeverDestroyed<Optional<GoldfishRTC>> s_goldfish_rtc;

GoldfishRTC::GoldfishRTC(Memory::TypedMapping<Registers volatile> rtc_registers)
    : m_registers(move(rtc_registers))
{
    m_boot_time = current_time();
}

GoldfishRTC* GoldfishRTC::the()
{
    if (!s_goldfish_rtc->has_value())
        return nullptr;

    return &s_goldfish_rtc->value();
}

UnixDateTime GoldfishRTC::current_time() const
{
    // "To read the value, the kernel must perform an IO_READ(TIME_LOW), which returns
    //  an unsigned 32-bit value, before an IO_READ(TIME_HIGH), which returns a signed
    //  32-bit value, corresponding to the higher half of the full value."
    u64 time_in_nanoseconds = m_registers->time_low;
    time_in_nanoseconds |= static_cast<u64>(m_registers->time_high) << 32;

    return UnixDateTime::from_nanoseconds_since_epoch(bit_cast<i64>(time_in_nanoseconds));
}

static constinit Array const compatibles_array = {
    "google,goldfish-rtc"sv,
};

EARLY_DEVICETREE_DRIVER(GoldfishRTCDriver, compatibles_array);

// https://www.kernel.org/doc/Documentation/devicetree/bindings/rtc/trivial-rtc.yaml
ErrorOr<void> GoldfishRTCDriver::probe(DeviceTree::Device const& device, StringView) const
{
    if (s_goldfish_rtc->has_value())
        return {};

    auto registers_resource = TRY(device.get_resource(0));
    if (registers_resource.size < sizeof(GoldfishRTC::Registers))
        return EINVAL;

    auto rtc_registers = TRY(Memory::map_typed_writable<GoldfishRTC::Registers volatile>(registers_resource.paddr));
    *s_goldfish_rtc = GoldfishRTC(move(rtc_registers));

    return {};
}

}
