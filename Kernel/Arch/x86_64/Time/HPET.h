/*
 * Copyright (c) 2020, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/OwnPtr.h>
#include <AK/Types.h>
#include <AK/Vector.h>
#include <Kernel/Memory/PhysicalAddress.h>
#include <Kernel/Memory/Region.h>

namespace Kernel {

class HPETComparator;
struct HPETRegistersBlock;

class HPET {
public:
    static bool initialized();
    static bool test_and_initialize();
    static bool check_for_exisiting_periodic_timers();
    static HPET& the();

    u64 frequency() const { return m_frequency; }
    u64 raw_counter_ticks_to_ns(u64) const;
    u64 ns_to_raw_counter_ticks(u64) const;

    Vector<NonnullLockRefPtr<HPETComparator>> const& comparators() const { return m_comparators; }
    void disable(HPETComparator const&);
    void enable(HPETComparator const&);

    void update_periodic_comparator_value();
    void update_non_periodic_comparator_value(HPETComparator const& comparator);

    void set_comparator_irq_vector(u8 comparator_number, u8 irq_vector);

    void enable_periodic_interrupt(HPETComparator const& comparator);
    void disable_periodic_interrupt(HPETComparator const& comparator);

    u64 update_time(u64& seconds_since_boot, u32& ticks_this_second, bool query_only);
    u64 read_main_counter_unsafe() const;
    u64 read_main_counter() const;

    Vector<unsigned> capable_interrupt_numbers(u8 comparator_number);
    Vector<unsigned> capable_interrupt_numbers(HPETComparator const&);

private:
    HPETRegistersBlock const& registers() const;
    HPETRegistersBlock& registers();

    void global_disable();
    void global_enable();

    bool is_periodic_capable(u8 comparator_number) const;
    bool is_64bit_capable(u8 comparator_number) const;
    void set_comparators_to_optimal_interrupt_state(size_t timers_count);

    PhysicalAddress find_acpi_hpet_registers_block();
    explicit HPET(PhysicalAddress acpi_hpet);
    PhysicalAddress m_physical_acpi_hpet_table;
    PhysicalAddress m_physical_acpi_hpet_registers;
    OwnPtr<Memory::Region> m_hpet_mmio_region;

    u64 m_main_counter_last_read { 0 };
    u64 m_main_counter_drift { 0 };
    u32 m_32bit_main_counter_wraps { 0 };

    u16 m_vendor_id;
    u16 m_minimum_tick;
    u64 m_frequency;
    u8 m_revision_id;
    bool m_main_counter_64bits : 1;
    bool legacy_replacement_route_capable : 1;

    Vector<NonnullLockRefPtr<HPETComparator>> m_comparators;
};
}
