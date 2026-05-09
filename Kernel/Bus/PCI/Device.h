/*
 * Copyright (c) 2020, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Format.h>
#include <AK/NonnullRefPtr.h>
#include <AK/StringBuilder.h>
#include <AK/Types.h>
#include <Kernel/Bus/PCI/Definitions.h>
#include <Kernel/Interrupts/Interrupts.h>

namespace Kernel::PCI {

enum class InterruptType {
    PIN,
    MSI,
    MSIX
};

struct InterruptRange {
    InterruptNumber m_start_irq { 0 };
    InterruptNumber m_irq_count { 0 };
    InterruptType m_type { InterruptType::PIN };
};

struct [[gnu::packed]] MSIxTableEntry {
    u32 address_low;
    u32 address_high;
    u32 data;
    u32 vector_control;
};

class Device {
public:
    DeviceIdentifier const& device_identifier() const { return *m_pci_identifier; }

    virtual ~Device() = default;

    virtual StringView device_name() const = 0;

    void enable_pin_based_interrupts() const;
    void disable_pin_based_interrupts() const;

    bool is_msi_capable() const;
    bool is_msix_capable() const;

    void enable_message_signalled_interrupts();
    void disable_message_signalled_interrupts();

    void enable_extended_message_signalled_interrupts();
    void disable_extended_message_signalled_interrupts();
    ErrorOr<InterruptType> reserve_irqs(size_t number_of_irqs, bool msi);
    ErrorOr<InterruptNumber> allocate_irq(size_t index);
    PCI::InterruptType get_interrupt_type();
    void enable_interrupt(InterruptNumber irq);
    void disable_interrupt(InterruptNumber irq);

protected:
    explicit Device(DeviceIdentifier const& pci_identifier);

private:
    PhysicalAddress msix_table_entry_address(InterruptNumber irq);

private:
    NonnullRefPtr<DeviceIdentifier> const m_pci_identifier;
    InterruptRange m_interrupt_range;
};

template<typename... Parameters>
void dmesgln_pci(Device const& device, AK::CheckedFormatString<Parameters...>&& fmt, Parameters const&... parameters)
{
    AK::StringBuilder builder;

    builder.appendff("{}: {}: ", device.device_name(), device.device_identifier().address());

    AK::VariadicFormatParams<AK::AllowDebugOnlyFormatters::Yes, Parameters...> variadic_format_params { parameters... };
    MUST(AK::vformat(builder, fmt.view(), variadic_format_params));

    dmesgln("{}", builder.string_view());
}

}
