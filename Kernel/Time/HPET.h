/*
 * Copyright (c) 2020, Liav A. <liavalb@hotmail.co.il>
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

#include <AK/NonnullRefPtrVector.h>
#include <AK/OwnPtr.h>
#include <AK/Types.h>
#include <AK/Vector.h>
#include <Kernel/PhysicalAddress.h>
#include <Kernel/VM/Region.h>

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

    const NonnullRefPtrVector<HPETComparator>& comparators() const { return m_comparators; }
    void disable(const HPETComparator&);
    void enable(const HPETComparator&);

    void update_periodic_comparator_value();
    void update_non_periodic_comparator_value(const HPETComparator& comparator);

    void set_comparator_irq_vector(u8 comparator_number, u8 irq_vector);

    void enable_periodic_interrupt(const HPETComparator& comparator);
    void disable_periodic_interrupt(const HPETComparator& comparator);

    u64 update_time(u64& seconds_since_boot, u32& ticks_this_second, bool query_only);
    u64 read_main_counter() const;

    Vector<unsigned> capable_interrupt_numbers(u8 comparator_number);
    Vector<unsigned> capable_interrupt_numbers(const HPETComparator&);

private:
    const HPETRegistersBlock& registers() const;
    HPETRegistersBlock& registers();

    void global_disable();
    void global_enable();

    bool is_periodic_capable(u8 comparator_number);
    void set_comparators_to_optimal_interrupt_state(size_t timers_count);

    u64 calculate_ticks_in_nanoseconds() const;

    PhysicalAddress find_acpi_hpet_registers_block();
    explicit HPET(PhysicalAddress acpi_hpet);
    PhysicalAddress m_physical_acpi_hpet_table;
    PhysicalAddress m_physical_acpi_hpet_registers;
    OwnPtr<Region> m_hpet_mmio_region;

    u64 m_main_counter_last_read { 0 };
    u64 m_main_counter_drift { 0 };

    u16 m_vendor_id;
    u16 m_minimum_tick;
    u64 m_frequency;
    u8 m_revision_id;
    bool counter_is_64_bit_capable : 1;
    bool legacy_replacement_route_capable : 1;

    NonnullRefPtrVector<HPETComparator> m_comparators;
};
}
