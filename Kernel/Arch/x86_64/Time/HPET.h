/*
 * Copyright (c) 2020, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/OwnPtr.h>
#include <AK/Types.h>
#include <AK/Vector.h>
#include <Kernel/Interrupts/Interrupts.h>
#include <Kernel/Memory/PhysicalAddress.h>
#include <Kernel/Memory/Region.h>
#include <Kernel/Memory/TypedMapping.h>

namespace Kernel {

class HPETComparator;

class HPET {
public:
    static bool initialized();
    static ErrorOr<bool> initialize();
    static HPET& the();

    u64 frequency() const { return m_frequency; }
    u64 raw_counter_ticks_to_ns(u64) const;
    u64 ns_to_raw_counter_ticks(u64) const;

    Vector<NonnullLockRefPtr<HPETComparator>> const& comparators() const { return m_comparators; }

    void update_periodic_comparator_value();
    void update_non_periodic_comparator_value(HPETComparator const& comparator);

    void set_comparator_irq_vector(u8 comparator_number, InterruptNumber irq_vector);

    void toggle_comparator(HPETComparator const& comparator, bool enable);
    void set_comparator_periodic(HPETComparator const& comparator, bool periodic);

    u64 update_time(u64& seconds_since_boot, u32& ticks_this_second, bool query_only);
    u64 read_main_counter_unsafe() const;
    u64 read_main_counter() const;

    struct Registers;

private:
    void global_disable();
    void global_enable();

    PhysicalAddress find_acpi_hpet_registers_block();

    HPET(Memory::TypedMapping<Registers volatile>);

    Memory::TypedMapping<Registers volatile> m_registers;

    u64 m_main_counter_last_read { 0 };
    u64 m_main_counter_drift { 0 };
    u32 m_32bit_main_counter_wraps { 0 };

    u64 m_frequency;
    bool m_main_counter_64bits;
    bool legacy_replacement_route_capable;

    Vector<NonnullLockRefPtr<HPETComparator>> m_comparators;
};
}
