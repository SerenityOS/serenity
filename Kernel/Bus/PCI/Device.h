/*
 * Copyright (c) 2020, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Format.h>
#include <AK/StringBuilder.h>
#include <AK/Types.h>
#include <Kernel/Bus/PCI/Definitions.h>

namespace Kernel::PCI {

class Device {
public:
    Address pci_address() const { return m_pci_address; };

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

protected:
    explicit Device(Address pci_address);

private:
    Address m_pci_address;
};

template<typename... Parameters>
void dmesgln_pci(Device const& device, AK::CheckedFormatString<Parameters...>&& fmt, Parameters const&... parameters)
{
    AK::StringBuilder builder;
    if (builder.try_append("{}: {}: "sv).is_error())
        return;
    if (builder.try_append(fmt.view()).is_error())
        return;
    AK::VariadicFormatParams variadic_format_params { device.device_name(), device.pci_address(), parameters... };
    vdmesgln(builder.string_view(), variadic_format_params);
}

}
