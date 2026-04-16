/*
 * Copyright (c) 2026, miridav
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/riscv64/GoldfishRTC.h>
#include <Kernel/Memory/MemoryManager.h>




namespace Kernel {

static NeverDestroyed<Optional<GoldfishRTC>> s_rtc;

GoldfishRTC& GoldfishRTC::the()
{
    VERIFY(s_rtc->has_value());
    return s_rtc->value();
}

ErrorOr<void> GoldfishRTC::initialize(PhysicalAddress paddr)
{
    if (s_rtc->has_value())
        return {};

    auto mapping = TRY(Memory::map_typed<Registers volatile>(paddr, sizeof(Registers)));

    s_rtc->emplace(move(mapping));

    return {};
}


GoldfishRTC::GoldfishRTC(Memory::TypedMapping<Registers volatile> mapping)
    : m_regs(move(mapping))        
{
    m_boot_time = now();
}

UnixDateTime GoldfishRTC::now() const
{
    SpinlockLocker lock(m_lock);
    u32 high, low;
    do {
        high = m_regs->time_high;
        low  = m_regs->time_low;
    } while (m_regs->time_high != high);

    u64 time = (static_cast<u64>(high) << 32) | low;
    return UnixDateTime::from_nanoseconds_since_epoch(time);
}

UnixDateTime GoldfishRTC::boot_time() const
{
    return m_boot_time;
}

bool GoldfishRTC::is_initialized(){
    return s_rtc->has_value();
}

} // namespace Kernel