/*
 * Copyright (c) 2023, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Bus/PCI/Access.h>
#include <Kernel/Bus/PCI/Drivers/Driver.h>
#include <Kernel/Bus/VirtIO/Transport/PCIe/TransportLink.h>
#include <Kernel/Security/Random/VirtIO/RNG.h>

namespace Kernel::PCI {

class VirtIORNGDriver final : public PCI::Driver {
public:
    static void init();

    VirtIORNGDriver()
        : PCI::Driver("VirtIORNGDriver"sv)
    {
    }

    virtual ErrorOr<void> probe(PCI::Device&) override;
    virtual void detach(PCI::Device&) override;
    virtual ClassID class_id() const override { return PCI::ClassID::Legacy; }
    virtual Span<HardwareIDMatch const> matches() override;

private:
    IntrusiveList<&VirtIO::RNG::m_driver_list_node> m_devices;
};

ErrorOr<void> VirtIORNGDriver::probe(PCI::Device& pci_device)
{
    auto pci_transport_link = TRY(VirtIO::PCIeTransportLink::create(pci_device));
    auto virtio_device = TRY(VirtIO::RNG::create(move(pci_transport_link)));
    m_devices.append(*virtio_device);
    return {};
}

void VirtIORNGDriver::detach(PCI::Device&)
{
    TODO();
}

const static HardwareIDMatch __matches[] = {
    // QEMU VirtIO RNG device
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::RedHat,
            PCI::DeviceID::VirtIOEntropy,
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
};

Span<HardwareIDMatch const> VirtIORNGDriver::matches()
{
    return __matches;
}

void VirtIORNGDriver::init()
{
    auto driver = MUST(adopt_nonnull_ref_or_enomem(new VirtIORNGDriver()));
    PCI::Access::the().register_driver(driver);
}

PCI_DEVICE_DRIVER(VirtIORNGDriver);

}
