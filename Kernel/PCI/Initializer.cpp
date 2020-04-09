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

#include <Kernel/ACPI/ACPIParser.h>
#include <Kernel/CommandLine.h>
#include <Kernel/Net/E1000NetworkAdapter.h>
#include <Kernel/Net/RTL8139NetworkAdapter.h>
#include <Kernel/PCI/IOAccess.h>
#include <Kernel/PCI/Initializer.h>
#include <Kernel/PCI/MMIOAccess.h>
#include <LibBareMetal/IO.h>

namespace Kernel {
namespace PCI {

static bool test_acpi();
static bool test_pci_io();
static bool test_pci_mmio();

static Access::Type detect_optimal_access_type(bool mmio_allowed)
{
    if (mmio_allowed && test_acpi() && test_pci_mmio())
        return Access::Type::MMIO;

    if (test_pci_io())
        return Access::Type::IO;

    klog() << "No PCI bus access method detected!";
    hang();
}

void initialize()
{
    bool mmio_allowed = kernel_command_line().lookup("pci_mmio").value_or("off") == "on";

    if (detect_optimal_access_type(mmio_allowed) == Access::Type::MMIO)
        MMIOAccess::initialize(ACPI::Parser::the().find_table("MCFG"));
    else
        IOAccess::initialize();

    enumerate_all([&](const Address& address, ID id) {
        klog() << "PCI: Device @ " << String::format("%w", address.seg()) << ":" << String::format("%b", address.bus()) << ":" << String::format("%b", address.slot()) << "." << String::format("%d", address.function()) << " [" << String::format("%w", id.vendor_id) << ":" << String::format("%w", id.device_id) << "]";
        E1000NetworkAdapter::detect(address);
        RTL8139NetworkAdapter::detect(address);
    });
}

bool test_acpi()
{
    if ((kernel_command_line().contains("noacpi")) || !ACPI::Parser::the().is_operable())
        return false;
    return true;
}

bool test_pci_io()
{
    klog() << "Testing PCI via manual probing... ";
    u32 tmp = 0x80000000;
    IO::out32(PCI_ADDRESS_PORT, tmp);
    tmp = IO::in32(PCI_ADDRESS_PORT);
    if (tmp == 0x80000000) {
        klog() << "PCI IO Supported!";
        return true;
    }

    klog() << "PCI IO Not Supported!";
    return false;
}

bool test_pci_mmio()
{
    return !ACPI::Parser::the().find_table("MCFG").is_null();
}

}
}
