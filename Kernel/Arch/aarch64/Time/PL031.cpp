/*
 * Copyright (c) 2025, the SerenityOS developers
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Array.h>
#include <AK/NeverDestroyed.h>
#include <AK/Optional.h>
#include <Kernel/Arch/aarch64/Time/PL031.h>
#include <Kernel/Firmware/DeviceTree/Driver.h>
#include <Kernel/Firmware/DeviceTree/Management.h>

namespace Kernel {

static NeverDestroyed<Optional<PL031>> s_rtc;
static NeverDestroyed<Mutex> s_rtc_lock { "PL031"sv };

PL031::PL031(Memory::TypedMapping<RTCRegisters volatile> rtc_registers)
    : m_rtc_registers(move(rtc_registers))
{
    m_boot_time = m_rtc_registers->RTCDR;
}

RawPtr<PL031> PL031::get_instance()
{
    MutexLocker locker(*s_rtc_lock);
    if (!s_rtc->has_value())
        return nullptr;

    return &s_rtc->value();
}

static constinit Array const compatibles_array = {
    "arm,pl031"sv,
};

DEVICETREE_DRIVER(PL031Driver, compatibles_array);

ErrorOr<void> PL031Driver::probe(DeviceTree::Device const& device, StringView) const
{
    MutexLocker locker(*s_rtc_lock);
    if (s_rtc->has_value())
        return {};

    auto rtc_registers_resource = TRY(device.get_resource(0));
    if (rtc_registers_resource.size < sizeof(PL031::RTCRegisters))
        return EINVAL;
    auto rtc_registers = TRY(Memory::map_typed_writable<PL031::RTCRegisters volatile>(rtc_registers_resource.paddr));
    *s_rtc = PL031(move(rtc_registers));
    return {};
}

}
