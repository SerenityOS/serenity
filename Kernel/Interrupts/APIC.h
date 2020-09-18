/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#include <AK/Types.h>
#include <Kernel/VM/MemoryManager.h>

namespace Kernel {

struct LocalAPIC {
    u32 apic_id;
};

class APIC {
public:
    static APIC& the();
    static void initialize();
    static bool initialized();

    bool init_bsp();
    void eoi();
    void boot_aps();
    void enable(u32 cpu);
    void init_finished(u32 cpu);
    void broadcast_ipi();
    void send_ipi(u32 cpu);
    static u8 spurious_interrupt_vector();
    Thread* get_idle_thread(u32 cpu) const;
    u32 enabled_processor_count() const { return m_processor_enabled_cnt; }

private:
    class ICRReg {
        u32 m_low { 0 };
        u32 m_high { 0 };

    public:
        enum DeliveryMode {
            Fixed = 0x0,
            LowPriority = 0x1,
            SMI = 0x2,
            NMI = 0x4,
            INIT = 0x5,
            StartUp = 0x6,
        };
        enum DestinationMode {
            Physical = 0x0,
            Logical = 0x1,
        };
        enum Level {
            DeAssert = 0x0,
            Assert = 0x1
        };
        enum class TriggerMode {
            Edge = 0x0,
            Level = 0x1,
        };
        enum DestinationShorthand {
            NoShorthand = 0x0,
            Self = 0x1,
            AllIncludingSelf = 0x2,
            AllExcludingSelf = 0x3,
        };

        ICRReg(u8 vector, DeliveryMode delivery_mode, DestinationMode destination_mode, Level level, TriggerMode trigger_mode, DestinationShorthand destinationShort, u8 destination = 0)
            : m_low(vector | (delivery_mode << 8) | (destination_mode << 11) | (level << 14) | (static_cast<u32>(trigger_mode) << 15) | (destinationShort << 18))
            , m_high((u32)destination << 24)
        {
        }

        u32 low() const { return m_low; }
        u32 high() const { return m_high; }
    };

    OwnPtr<Region> m_apic_base;
    Vector<OwnPtr<Processor>> m_ap_processor_info;
    Vector<Thread*> m_ap_idle_threads;
    AK::Atomic<u8> m_apic_ap_count { 0 };
    AK::Atomic<u8> m_apic_ap_continue { 0 };
    u32 m_processor_cnt { 0 };
    u32 m_processor_enabled_cnt { 0 };

    static PhysicalAddress get_base();
    static void set_base(const PhysicalAddress& base);
    void write_register(u32 offset, u32 value);
    u32 read_register(u32 offset);
    void set_lvt(u32 offset, u8 interrupt);
    void set_siv(u32 offset, u8 interrupt);
    void wait_for_pending_icr();
    void write_icr(const ICRReg& icr);
    void do_boot_aps();
};

}
