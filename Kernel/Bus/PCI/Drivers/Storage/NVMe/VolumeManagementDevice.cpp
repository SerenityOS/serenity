/*
 * Copyright (c) 2023, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Bus/PCI/Access.h>
#include <Kernel/Bus/PCI/Controller/VolumeManagementDevice.h>
#include <Kernel/Bus/PCI/Drivers/Driver.h>

namespace Kernel::PCI {

class VolumeManagementDeviceDriver final : public PCI::Driver {
public:
    static void init();

    VolumeManagementDeviceDriver()
        : PCI::Driver("VolumeManagementDeviceDriver"sv)
    {
    }

    virtual ErrorOr<void> probe(PCI::Device&) override;
    virtual void detach(PCI::Device&) override;
    virtual ClassID class_id() const override { return PCI::ClassID::MassStorage; }
    virtual Span<HardwareIDMatch const> matches() override;

private:
    IntrusiveList<&PCI::VolumeManagementDevice::m_driver_list_node> m_devices;
};

ErrorOr<void> VolumeManagementDeviceDriver::probe(PCI::Device& pci_device)
{
    if (!pci_device.parent_bus())
        return Error::from_errno(EIO);
    auto controller = TRY(PCI::VolumeManagementDevice::create(pci_device, *pci_device.parent_bus()));
    TRY(PCI::Access::the().add_host_controller_and_scan_for_devices(controller));
    m_devices.append(*controller);
    return {};
}

void VolumeManagementDeviceDriver::detach(PCI::Device&)
{
    TODO();
}

const static HardwareIDMatch __matches[] = {
    {
        .subclass_code = to_underlying(PCI::MassStorage::SubclassID::RAIDController),
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            0x8086,
            0x9a0b,
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
};

Span<HardwareIDMatch const> VolumeManagementDeviceDriver::matches()
{
    return __matches;
}

void VolumeManagementDeviceDriver::init()
{
    auto driver = MUST(adopt_nonnull_ref_or_enomem(new VolumeManagementDeviceDriver()));
    PCI::Access::the().register_driver(driver);
}

PCI_DEVICE_DRIVER(VolumeManagementDeviceDriver);

}
