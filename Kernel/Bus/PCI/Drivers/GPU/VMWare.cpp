/*
 * Copyright (c) 2023, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Bus/PCI/Access.h>
#include <Kernel/Bus/PCI/Drivers/Driver.h>
#include <Kernel/Devices/GPU/VMWare/GraphicsAdapter.h>

namespace Kernel::PCI {

class VMWareGPUDriver final : public PCI::Driver {
public:
    static void init();

    VMWareGPUDriver()
        : PCI::Driver("VMWareGPUDriver"sv)
    {
    }

    virtual ErrorOr<void> probe(PCI::Device&) override;
    virtual void detach(PCI::Device&) override;
    virtual ClassID class_id() const override { return PCI::ClassID::Display; }
    virtual Span<HardwareIDMatch const> matches() override;

private:
    IntrusiveList<&VMWareGraphicsAdapter::m_driver_list_node> m_devices;
};

ErrorOr<void> VMWareGPUDriver::probe(PCI::Device& pci_device)
{
    auto device = TRY(VMWareGraphicsAdapter::create(pci_device));
    m_devices.append(*device);
    return {};
}

void VMWareGPUDriver::detach(PCI::Device&)
{
    TODO();
}

const static HardwareIDMatch __matches[] = {
    {
        .subclass_code = to_underlying(PCI::Display::SubclassID::VGA),
        .revision_id = Optional<RevisionID> {},
        .hardware_id = {
            PCI::VendorID::VMWare,
            0x0405,
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
};

Span<HardwareIDMatch const> VMWareGPUDriver::matches()
{
    return __matches;
}

void VMWareGPUDriver::init()
{
    auto driver = MUST(adopt_nonnull_ref_or_enomem(new VMWareGPUDriver()));
    PCI::Access::the().register_driver(driver);
}

PCI_DEVICE_DRIVER(VMWareGPUDriver);

}
