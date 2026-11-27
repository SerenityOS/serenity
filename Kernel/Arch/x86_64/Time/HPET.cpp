/*
 * Copyright (c) 2020, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringView.h>
#include <Kernel/Arch/x86_64/Time/HPET.h>
#include <Kernel/Arch/x86_64/Time/HPETComparator.h>
#include <Kernel/Debug.h>
#include <Kernel/Firmware/ACPI/Parser.h>
#include <Kernel/Memory/MemoryManager.h>
#include <Kernel/Memory/TypedMapping.h>
#include <Kernel/Sections.h>
#include <Kernel/Time/TimeManagement.h>
#include <LibACPI/Definitions.h>

namespace Kernel {

constexpr u32 ABSOLUTE_MAXIMUM_COUNTER_TICK_PERIOD_NS = 100'000'000;

enum class Attributes {
    Counter64BitCapable = 1 << 13,
    LegacyReplacementRouteCapable = 1 << 15
};

enum class Configuration {
    Enable = 1 << 0,
    LegacyReplacementRoute = 1 << 1
};

enum class TimerConfiguration : u32 {
    LevelTriggered = 1 << 1,
    InterruptEnable = 1 << 2,
    GeneratePeriodicInterrupt = 1 << 3,
    PeriodicInterruptCapable = 1 << 4,
    Timer64BitsCapable = 1 << 5,
    ValueSet = 1 << 6,
    Force32BitMode = 1 << 8,
    FSBInterruptEnable = 1 << 14,
    FSBInterruptDelivery = 1 << 15
};
AK_ENUM_BITWISE_OPERATORS(TimerConfiguration);

struct [[gnu::packed]] TimerStructure {
    TimerConfiguration capabilities;
    u32 interrupt_routing;
    u64 comparator_value;
    u64 fsb_interrupt_route;
    u64 _;
};

struct [[gnu::packed]] HPET::Registers {
    u32 attributes;
    u32 main_counter_tick_period;
    u64 _;
    u64 configuration;
    u64 _;
    u64 interrupt_status;
    u8 _[0xF0 - 0x28];
    u64 main_counter_value;
    u64 _;
    TimerStructure timers[32];
};
static_assert(AssertSize<HPET::Registers, 0x500>());

static HPET* s_hpet;
static bool s_hpet_initialized { false };

bool HPET::initialized()
{
    return s_hpet_initialized;
}

HPET& HPET::the()
{
    VERIFY(HPET::initialized());
    VERIFY(s_hpet != nullptr);
    return *s_hpet;
}

UNMAP_AFTER_INIT ErrorOr<bool> HPET::initialize()
{
    VERIFY(!HPET::initialized());
    s_hpet_initialized = true;

    auto hpet_table = ACPI::Parser::the()->find_table("HPET"sv);
    if (!hpet_table.has_value())
        return false;
    dmesgln("HPET @ {}", hpet_table.value());

    auto sdt = TRY(Memory::map_typed<::ACPI::Structures::HPET>(hpet_table.value()));
    if (sdt->event_timer_block.address_space != ::ACPI::GenericAddressStructure::AddressSpace::SystemMemory)
        return EINVAL;

    auto registers = TRY(Memory::map_typed_writable<Registers volatile>(PhysicalAddress(sdt->event_timer_block.address)));

    auto hpet = TRY(adopt_nonnull_lock_ref_or_enomem(new (nothrow) HPET(move(registers))));
    TRY(TimeManagement::the().register_time_source(hpet));
    return true;
}

void HPET::global_disable()
{
    m_registers->configuration &= ~(u32)Configuration::Enable;
}

void HPET::global_enable()
{
    m_registers->configuration |= (u32)Configuration::Enable;
}

void HPET::update_periodic_comparator_value()
{
    // According to 2.3.9.2.2 the only safe way to change the periodic timer frequency
    // is to disable all periodic timers, reset the main counter and each timer's comparator value.
    // This introduces time drift, so it should be avoided unless absolutely necessary.
    global_disable();

    u64 previous_main_value = m_registers->main_counter_value;
    add_drift(previous_main_value - counter_last_read());
    clear_counter_last_read();
    m_registers->main_counter_value = 0;

    for (auto& comparator : m_comparators) {
        auto& timer = m_registers->timers[comparator->comparator_number()];
        if (!comparator->is_enabled())
            continue;

        if (comparator->mode() == TimerMode::Periodic) {
            // NOTE: This means we're restarting all periodic timers. There is no way
            // to resume periodic timers properly because we reset the main counter
            // and we can only write the period into the comparator value...
            timer.capabilities = timer.capabilities | TimerConfiguration::ValueSet;
            u64 value = ns_to_raw_counter_ticks(1'000'000'000ull / comparator->frequency());
            dbgln_if(HPET_DEBUG, "HPET: Update periodic comparator {} comparator value to {} main value was: {}", comparator->comparator_number(), value, previous_main_value);

            if (comparator->is_64bit_capable())
                timer.comparator_value = value;
            else
                timer.comparator_value = static_cast<u32>(value);
        } else {
            // Set the new target comparator value to the delta to the remaining ticks
            u64 current_value = timer.comparator_value;
            u64 value = current_value - previous_main_value;
            dbgln_if(HPET_DEBUG, "HPET: Update non-periodic comparator {} comparator value from {} to {} main value was: {}", comparator->comparator_number(), current_value, value, previous_main_value);

            if (comparator->is_64bit_capable())
                timer.comparator_value = value;
            else
                timer.comparator_value = static_cast<u32>(value);
        }
    }

    global_enable();
}

void HPET::update_non_periodic_comparator_value(HPETComparator const& comparator)
{
    VERIFY_INTERRUPTS_DISABLED();

    VERIFY(comparator.mode() == TimerMode::OneShot);

    auto& timer = m_registers->timers[comparator.comparator_number()];
    u64 value = frequency() / comparator.frequency();

    // NOTE: If the main counter passes this new value before we finish writing it, we will never receive an interrupt!
    u64 new_counter_value = read_counter() + value;
    timer.comparator_value = new_counter_value;
}

u64 HPET::read_counter_raw() const
{
    return m_registers->main_counter_value;
}

void HPET::toggle_comparator(HPETComparator const& comparator, bool enable)
{
    dbgln_if(HPET_DEBUG, "HPET: {} comparator {}", enable ? "Enable" : "Disable", comparator.comparator_number());

    auto& timer = m_registers->timers[comparator.comparator_number()];
    if (enable)
        timer.capabilities = timer.capabilities | TimerConfiguration::InterruptEnable;
    else
        timer.capabilities = timer.capabilities & ~TimerConfiguration::InterruptEnable;
}

void HPET::set_comparator_periodic(HPETComparator const& comparator, bool periodic)
{
    VERIFY(comparator.comparator_number() < m_comparators.size());

    dbgln_if(HPET_DEBUG, "HPET: Set comparator {} to be periodic: {}", comparator.comparator_number(), periodic);
    toggle_comparator(comparator, false);

    auto& timer = m_registers->timers[comparator.comparator_number()];

    if (periodic) {
        VERIFY(has_flag(timer.capabilities, TimerConfiguration::PeriodicInterruptCapable));
        timer.capabilities = timer.capabilities | TimerConfiguration::GeneratePeriodicInterrupt;
    } else {
        timer.capabilities = timer.capabilities & ~TimerConfiguration::GeneratePeriodicInterrupt;
    }

    if (comparator.is_enabled())
        toggle_comparator(comparator, true);
}

void HPET::set_comparator_irq_vector(u8 comparator_number, InterruptNumber irq_vector)
{
    VERIFY(comparator_number <= m_comparators.size());
    auto& timer = m_registers->timers[comparator_number];
    timer.capabilities = timer.capabilities | static_cast<TimerConfiguration>(irq_vector.value() << 9);
}

u64 HPET::raw_counter_ticks_to_ns(u64 raw_ticks) const
{
    return (raw_ticks * (u64)m_registers->main_counter_tick_period * 100ull) / ABSOLUTE_MAXIMUM_COUNTER_TICK_PERIOD_NS;
}

u64 HPET::ns_to_raw_counter_ticks(u64 ns) const
{
    return (ns * 1'000'000ull) / m_registers->main_counter_tick_period;
}

UNMAP_AFTER_INIT HPET::HPET(Memory::TypedMapping<Registers volatile> registers)
    : TimeSource((registers->attributes & static_cast<u32>(Attributes::Counter64BitCapable)) != 0 ? TimeSource::CounterSize::Bits64 : TimeSource::CounterSize::Bits32)
    , m_registers(move(registers))
{
    s_hpet = this; // Make available as soon as possible so that IRQs can use it

    size_t timers_count = ((m_registers->attributes >> 8) & 0x1F) + 1;
    dmesgln("HPET: Timers count: {}", timers_count);
    dmesgln("HPET: Main counter size: {}", (counter_size() == TimeSource::CounterSize::Bits64 ? "64-bit" : "32-bit"));

    for (size_t i = 0; i < timers_count; i++) {
        auto const& timer = m_registers->timers[i];
        bool capable_64_bit = has_flag(timer.capabilities, TimerConfiguration::Timer64BitsCapable);
        dmesgln("HPET: Timer[{}] comparator size: {}", i, capable_64_bit ? "64-bit" : "32-bit");
    }

    VERIFY(timers_count >= 2);

    global_disable();

    m_frequency = 1'000'000'000 / raw_counter_ticks_to_ns(1);
    dmesgln("HPET: Frequency {} MHz (Resolution: {} ns)", (m_frequency + 500'000) / 1'000'000, raw_counter_ticks_to_ns(1));

    VERIFY(m_registers->main_counter_tick_period <= ABSOLUTE_MAXIMUM_COUNTER_TICK_PERIOD_NS);

    // Reset the counter, just in case... (needs to match m_main_counter_last_read)
    m_registers->main_counter_value = 0;
    if (m_registers->attributes & (u32)Attributes::LegacyReplacementRouteCapable)
        m_registers->configuration |= (u32)Configuration::LegacyReplacementRoute;

    for (size_t i = 0; i < 2; i++) {
        auto const& timer = m_registers->timers[i];
        bool is_periodic = has_flag(timer.capabilities, TimerConfiguration::PeriodicInterruptCapable);
        bool is_64bit = has_flag(timer.capabilities, TimerConfiguration::Timer64BitsCapable);

        m_comparators.try_append(HPETComparator::create({}, i, i * 8, is_periodic, is_64bit)).release_value_but_fixme_should_propagate_errors();
    }

    global_enable();
}
}
