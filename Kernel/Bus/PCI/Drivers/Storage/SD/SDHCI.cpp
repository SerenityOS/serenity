/*
 * Copyright (c) 2023, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Bus/PCI/Access.h>
#include <Kernel/Bus/PCI/Drivers/Driver.h>
#include <Kernel/Devices/Storage/SD/PCISDHostController.h>

namespace Kernel::PCI {

class SDHCIDriver final : public PCI::Driver {
public:
    static void init();

    SDHCIDriver()
        : PCI::Driver("SDHCIDriver"sv)
    {
    }

    virtual ErrorOr<void> probe(PCI::Device&) override;
    virtual void detach(PCI::Device&) override;
    virtual ClassID class_id() const override { return PCI::ClassID::Base; }
    virtual Span<HardwareIDMatch const> matches() override;

private:
    IntrusiveList<&PCISDHostController::m_driver_list_node> m_devices;
};

ErrorOr<void> SDHCIDriver::probe(PCI::Device& pci_device)
{
    auto device = TRY(PCISDHostController::try_initialize(pci_device));
    m_devices.append(*device);
    return {};
}

void SDHCIDriver::detach(PCI::Device&)
{
    TODO();
}

const static HardwareIDMatch __matches[] = {
    {
        .subclass_code = to_underlying(PCI::Base::SubclassID::SDHostController),
        .revision_id = Optional<RevisionID> {},
        .hardware_id = {
            0xffff,
            0xffff,
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
};

Span<HardwareIDMatch const> SDHCIDriver::matches()
{
    return __matches;
}

void SDHCIDriver::init()
{
    auto driver = MUST(adopt_nonnull_ref_or_enomem(new SDHCIDriver()));
    PCI::Access::the().register_driver(driver);
}

PCI_DEVICE_DRIVER(SDHCIDriver);

}
