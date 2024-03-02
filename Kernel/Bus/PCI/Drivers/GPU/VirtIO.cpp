/*
 * Copyright (c) 2023, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Bus/PCI/Access.h>
#include <Kernel/Bus/PCI/Drivers/Driver.h>
#include <Kernel/Bus/VirtIO/Transport/PCIe/TransportLink.h>
#include <Kernel/Devices/GPU/VirtIO/GraphicsAdapter.h>

namespace Kernel::PCI {

class VirtIOGPUDriver final : public PCI::Driver {
public:
    static void init();

    VirtIOGPUDriver()
        : PCI::Driver("VirtIOGPUDriver"sv)
    {
    }

    virtual ErrorOr<void> probe(PCI::Device&) override;
    virtual void detach(PCI::Device&) override;
    virtual ClassID class_id() const override { return PCI::ClassID::Display; }
    virtual Span<HardwareIDMatch const> matches() override;

private:
    IntrusiveList<&VirtIOGraphicsAdapter::m_driver_list_node> m_devices;
};

ErrorOr<void> VirtIOGPUDriver::probe(PCI::Device& pci_device)
{
    auto pci_transport_link = TRY(VirtIO::PCIeTransportLink::create(pci_device));
    auto device = TRY(VirtIOGraphicsAdapter::create(move(pci_transport_link)));
    m_devices.append(*device);
    return {};
}

void VirtIOGPUDriver::detach(PCI::Device&)
{
    TODO();
}

const static HardwareIDMatch __matches[] = {
    {
        .subclass_code = to_underlying(PCI::Display::SubclassID::VGA),
        .revision_id = Optional<RevisionID> {},
        .hardware_id = {
            PCI::VendorID::VirtIO,
            0x1050,
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = to_underlying(PCI::Display::SubclassID::Other),
        .revision_id = Optional<RevisionID> {},
        .hardware_id = {
            PCI::VendorID::VirtIO,
            0x1050,
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
};

Span<HardwareIDMatch const> VirtIOGPUDriver::matches()
{
    return __matches;
}

void VirtIOGPUDriver::init()
{
    auto driver = MUST(adopt_nonnull_ref_or_enomem(new VirtIOGPUDriver()));
    PCI::Access::the().register_driver(driver);
}

PCI_DEVICE_DRIVER(VirtIOGPUDriver);

}
