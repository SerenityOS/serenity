/*
 * Copyright (c) 2023, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Bus/PCI/Access.h>
#include <Kernel/Bus/PCI/Drivers/Driver.h>
#include <Kernel/Bus/VirtIO/Transport/PCIe/TransportLink.h>
#include <Kernel/Devices/Serial/VirtIO/Console.h>

namespace Kernel::PCI {

class VirtIOConsoleDriver final : public PCI::Driver {
public:
    static void init();

    VirtIOConsoleDriver()
        : PCI::Driver("VirtIOConsoleDriver"sv)
    {
    }

    virtual ErrorOr<void> probe(PCI::Device&) override;
    virtual void detach(PCI::Device&) override;
    virtual ClassID class_id() const override { return PCI::ClassID::SimpleCommunication; }
    virtual Span<HardwareIDMatch const> matches() override;

private:
    IntrusiveList<&VirtIO::Console::m_driver_list_node> m_devices;
};

ErrorOr<void> VirtIOConsoleDriver::probe(PCI::Device& pci_device)
{
    auto pci_transport_link = TRY(VirtIO::PCIeTransportLink::create(pci_device));
    auto virtio_device = TRY(VirtIO::Console::create(move(pci_transport_link)));
    m_devices.append(*virtio_device);
    return {};
}

void VirtIOConsoleDriver::detach(PCI::Device&)
{
    TODO();
}

const static HardwareIDMatch __matches[] = {
    // QEMU VirtIO console device
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::RedHat,
            PCI::DeviceID::VirtIOConsole,
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
};

Span<HardwareIDMatch const> VirtIOConsoleDriver::matches()
{
    return __matches;
}

void VirtIOConsoleDriver::init()
{
    auto driver = MUST(adopt_nonnull_ref_or_enomem(new VirtIOConsoleDriver()));
    PCI::Access::the().register_driver(driver);
}

PCI_DEVICE_DRIVER(VirtIOConsoleDriver);

}
