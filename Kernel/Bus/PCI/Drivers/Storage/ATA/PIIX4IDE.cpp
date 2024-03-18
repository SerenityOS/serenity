/*
 * Copyright (c) 2023, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Bus/PCI/Access.h>
#include <Kernel/Bus/PCI/Drivers/Driver.h>
#include <Kernel/Devices/Storage/ATA/PIIX4IDE/Controller.h>

namespace Kernel::PCI {

class PIIX4ATADriver final : public PCI::Driver {
public:
    static void init();

    PIIX4ATADriver()
        : PCI::Driver("PIIX4ATADriver"sv)
    {
    }

    virtual ErrorOr<void> probe(PCI::Device&) override;
    virtual void detach(PCI::Device&) override;
    virtual ClassID class_id() const override { return PCI::ClassID::MassStorage; }
    virtual Span<HardwareIDMatch const> matches() override;

private:
    IntrusiveList<&PIIX4IDEController::m_driver_list_node> m_devices;
};

ErrorOr<void> PIIX4ATADriver::probe(PCI::Device& pci_device)
{
    auto controller = TRY(PIIX4IDEController::initialize(pci_device, false));
    m_devices.append(*controller);
    return {};
}

void PIIX4ATADriver::detach(PCI::Device&)
{
    TODO();
}

const static HardwareIDMatch __matches[] = {
    {
        .subclass_code = to_underlying(PCI::MassStorage::SubclassID::IDEController),
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID { 0xffff, 0xffff },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
};

Span<HardwareIDMatch const> PIIX4ATADriver::matches()
{
    return __matches;
}

void PIIX4ATADriver::init()
{
    auto driver = MUST(adopt_nonnull_ref_or_enomem(new PIIX4ATADriver()));
    PCI::Access::the().register_driver(driver);
}

PCI_DEVICE_DRIVER(PIIX4ATADriver);

}
