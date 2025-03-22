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
        u32 data;                        // RTCDR
        u32 match;                       // RTCMR
        u32 load;                        // RTCLR
        u32 control;                     // RTCCR
        u32 interrupt_mask_set_or_clear; // RTCIMSC
        u32 raw_interrupt_status;        // RTCRIS
        u32 masked_interrupt_status;     // RTCMIS
        u32 interrupt_clear_register;    // RTCICR
        u32 reserved[1008];
        u32 peripheral_id_bits_7_0;   // RTCPeriphID0
        u32 peripheral_id_bits_15_8;  // RTCPeriphID1
        u32 peripheral_id_bits_23_16; // RTCPeriphID2
        u32 peripheral_id_bits_31_24; // RTCPeriphID3
        u32 primecell_id_bits_7_0;    // RTCPCellID0
        u32 primecell_id_bits_15_8;   // RTCPCellID1
        u32 primecell_id_bits_23_16;  // RTCPCellID2
        u32 primecell_id_bits_31_24;  // RTCPCellID3
    };
    static_assert(AssertSize<RTCRegisters, 0x1000>());

    PL031(Memory::TypedMapping<RTCRegisters volatile>);

    static RawPtr<PL031> the();

    UnixDateTime boot_time() { return UnixDateTime::from_seconds_since_epoch(m_boot_time); }

private:
    Memory::TypedMapping<RTCRegisters volatile> m_rtc_registers;
    time_t m_boot_time;
};

}
