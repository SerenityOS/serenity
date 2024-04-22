/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/SetOnce.h>
#include <AK/Types.h>
#include <Kernel/Memory/MemoryManager.h>
#include <Kernel/Time/HardwareTimer.h>

namespace Kernel {

class APICTimer;

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
    void setup_ap_boot_environment();
    void boot_aps();
    void enable(u32 cpu);
    void init_finished(u32 cpu);
    void broadcast_ipi();
    void send_ipi(u32 cpu);
    static u8 spurious_interrupt_vector();
    Thread* get_idle_thread(u32 cpu) const;
    u32 enabled_processor_count() const { return m_processor_enabled_cnt; }

    APICTimer* initialize_timers(HardwareTimerBase&);
    APICTimer* get_timer() const { return m_apic_timer; }
    enum class TimerMode {
        OneShot,
        Periodic,
        TSCDeadline
    };
    void setup_local_timer(u32, TimerMode, bool);
    u32 get_timer_current_count();
    u32 get_timer_divisor();

private:
    struct ICRReg {
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

        u8 vector { 0 };
        u32 destination { 0 };
        DeliveryMode delivery_mode { DeliveryMode::Fixed };
        DestinationMode destination_mode { DestinationMode::Physical };
        Level level { Level::DeAssert };
        TriggerMode trigger_mode { TriggerMode::Edge };
        DestinationShorthand destination_short { DestinationShorthand::NoShorthand };

        u32 x_low() const { return (u32)vector | (delivery_mode << 8) | (destination_mode << 11) | (level << 14) | (static_cast<u32>(trigger_mode) << 15) | (destination_short << 18); }
        u32 x_high() const { return destination << 24; }
        u64 x2_value() const { return ((u64)destination << 32) | x_low(); }
    };

    OwnPtr<Memory::Region> m_apic_base;
    Vector<OwnPtr<Processor>> m_ap_processor_info;
    Vector<OwnPtr<Memory::Region>> m_ap_temporary_boot_stacks;
    Vector<Thread*> m_ap_idle_threads;
    OwnPtr<Memory::Region> m_ap_boot_environment;
    Atomic<u8> m_apic_ap_count { 0 };
    Atomic<u8> m_apic_ap_continue { 0 };
    u32 m_processor_cnt { 0 };
    u32 m_processor_enabled_cnt { 0 };
    APICTimer* m_apic_timer { nullptr };
    SetOnce m_is_x2;

    static PhysicalAddress get_base();
    void set_base(PhysicalAddress const& base);
    void write_register(u32 offset, u32 value);
    u32 read_register(u32 offset);
    void set_lvt(u32 offset, u8 interrupt);
    void set_siv(u32 offset, u8 interrupt);
    void wait_for_pending_icr();
    void write_icr(ICRReg const& icr);
    void do_boot_aps();
};

}
