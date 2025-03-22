/*
 * Copyright (c) 2025, the SerenityOS developers
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/StdLibExtras.h>
#include <AK/Time.h>
#include <Kernel/Memory/TypedMapping.h>

// https://developer.arm.com/documentation/ddi0224/c/?lang=en

namespace Kernel {

class PL031 {
public:
    struct RTCRegisters {
        u32 RTCDR;
        u32 RTCMR;
        u32 RTCLR;
        u32 RTCCR;
        u32 RTCIMSC;
        u32 RTCRIS;
        u32 RTCMIS;
        u32 RTCICR;
        u32 reserved[1008];
        u32 RTCPeriphID0;
        u32 RTCPeriphID1;
        u32 RTCPeriphID2;
        u32 RTCPeriphID3;
        u32 RTCPCellID0;
        u32 RTCPCellID1;
        u32 RTCPCellID2;
        u32 RTCPCellID3;
    };
    static_assert(AssertSize<RTCRegisters, 0x1000>());

    PL031(Memory::TypedMapping<RTCRegisters volatile>);

    static RawPtr<PL031> get_instance();

    UnixDateTime boot_time() { return UnixDateTime::from_seconds_since_epoch(m_boot_time); }
    time_t now() { return m_rtc_registers->RTCDR; }

private:
    Memory::TypedMapping<RTCRegisters volatile> m_rtc_registers;
    time_t m_boot_time;
};

}
