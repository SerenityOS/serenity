/*
 * Copyright (c) 2023, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Bus/PCI/Access.h>
#include <Kernel/Bus/PCI/Drivers/Driver.h>
#include <Kernel/Devices/Storage/ATA/AHCI/Controller.h>

namespace Kernel::PCI {

class AHCIDriver final : public PCI::Driver {
public:
    static void init();

    AHCIDriver()
        : PCI::Driver("AHCIDriver"sv)
    {
    }

    virtual ErrorOr<void> probe(PCI::Device&) override;
    virtual void detach(PCI::Device&) override;
    virtual ClassID class_id() const override { return PCI::ClassID::MassStorage; }
    virtual Span<HardwareIDMatch const> matches() override;

private:
    IntrusiveList<&AHCIController::m_driver_list_node> m_devices;
};

ErrorOr<void> AHCIDriver::probe(PCI::Device& pci_device)
{
    if (pci_device.device_id().prog_if() == PCI::MassStorage::SATAProgIF::AHCI)
        return Error::from_errno(ENOTSUP);
    auto controller = TRY(AHCIController::initialize(pci_device));
    m_devices.append(*controller);
    return {};
}

void AHCIDriver::detach(PCI::Device&)
{
    TODO();
}

const static HardwareIDMatch __matches[] = {
    {
        .subclass_code = to_underlying(PCI::MassStorage::SubclassID::SATAController),
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID { 0xffff, 0xffff },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
};

Span<HardwareIDMatch const> AHCIDriver::matches()
{
    return __matches;
}

void AHCIDriver::init()
{
    auto driver = MUST(adopt_nonnull_ref_or_enomem(new AHCIDriver()));
    PCI::Access::the().register_driver(driver);
}

PCI_DEVICE_DRIVER(AHCIDriver);

}
