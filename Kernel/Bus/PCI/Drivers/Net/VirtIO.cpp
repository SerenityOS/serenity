/*
 * Copyright (c) 2023, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Bus/PCI/Access.h>
#include <Kernel/Bus/PCI/Drivers/Driver.h>
#include <Kernel/Bus/PCI/IDs.h>
#include <Kernel/Bus/VirtIO/Transport/PCIe/TransportLink.h>
#include <Kernel/Net/NetworkingManagement.h>
#include <Kernel/Net/VirtIO/VirtIONetworkAdapter.h>

namespace Kernel::PCI {

class VirtIONetDriver final : public PCI::Driver {
public:
    static void init();

    VirtIONetDriver()
        : PCI::Driver("VirtIONetDriver"sv)
    {
    }

    virtual ErrorOr<void> probe(PCI::Device&) override;
    virtual void detach(PCI::Device&) override;
    virtual ClassID class_id() const override { return PCI::ClassID::Network; }
    virtual Span<HardwareIDMatch const> matches() override;

private:
    IntrusiveList<&VirtIONetworkAdapter::m_driver_list_node> m_devices;
};

ErrorOr<void> VirtIONetDriver::probe(PCI::Device& device)
{
    auto interface_name = TRY(NetworkingManagement::generate_interface_name_from_pci_address(device));
    auto pci_transport_link = TRY(VirtIO::PCIeTransportLink::create(device));
    auto virtio_device = TRY(VirtIONetworkAdapter::create(interface_name.representable_view(), move(pci_transport_link)));
    m_devices.append(*virtio_device);
    NetworkingManagement::the().attach_adapter(virtio_device);
    return {};
}

void VirtIONetDriver::detach(PCI::Device&)
{
    TODO();
}

const static HardwareIDMatch __matches[] = {
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            0x1af4,
            0x1000,
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
};

Span<HardwareIDMatch const> VirtIONetDriver::matches()
{
    return __matches;
}

void VirtIONetDriver::init()
{
    auto driver = MUST(adopt_nonnull_ref_or_enomem(new VirtIONetDriver()));
    PCI::Access::the().register_driver(driver);
}

PCI_DEVICE_DRIVER(VirtIONetDriver);
}
