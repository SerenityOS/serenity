/*
 * Copyright (c) 2023, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Bus/PCI/Access.h>
#include <Kernel/Bus/PCI/Drivers/Driver.h>
#include <Kernel/Devices/Storage/NVMe/NVMeController.h>

namespace Kernel::PCI {

class NVMeDriver final : public PCI::Driver {
public:
    static void init();

    NVMeDriver()
        : PCI::Driver("NVMeDriver"sv)
    {
    }

    virtual ErrorOr<void> probe(PCI::Device&) override;
    virtual void detach(PCI::Device&) override;
    virtual ClassID class_id() const override { return PCI::ClassID::MassStorage; }
    virtual Span<HardwareIDMatch const> matches() override;

private:
    IntrusiveList<&NVMeController::m_driver_list_node> m_devices;
};

ErrorOr<void> NVMeDriver::probe(PCI::Device& pci_device)
{
    auto device = TRY(NVMeController::try_initialize(pci_device, false));
    m_devices.append(*device);
    return {};
}

void NVMeDriver::detach(PCI::Device&)
{
    TODO();
}

const static HardwareIDMatch __matches[] = {
    {
        .subclass_code = to_underlying(PCI::MassStorage::SubclassID::NVMeController),
        .revision_id = Optional<RevisionID> {},
        .hardware_id = {
            0xffff,
            0xffff,
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
};

Span<HardwareIDMatch const> NVMeDriver::matches()
{
    return __matches;
}

void NVMeDriver::init()
{
    auto driver = MUST(adopt_nonnull_ref_or_enomem(new NVMeDriver()));
    PCI::Access::the().register_driver(driver);
}

PCI_DEVICE_DRIVER(NVMeDriver);

}
