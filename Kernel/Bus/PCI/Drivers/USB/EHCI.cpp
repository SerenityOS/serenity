/*
 * Copyright (c) 2023, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Bus/PCI/Access.h>
#include <Kernel/Bus/PCI/Drivers/Driver.h>
#include <Kernel/Bus/USB/EHCI/EHCIController.h>

namespace Kernel::PCI {

class EHCIDriver final : public PCI::Driver {
public:
    static void init();

    EHCIDriver()
        : Driver("EHCIDriver"sv)
    {
    }

    virtual ErrorOr<void> probe(PCI::Device&) override;
    virtual void detach(PCI::Device&) override;
    virtual ClassID class_id() const override { return PCI::ClassID::SerialBus; }
    virtual Span<HardwareIDMatch const> matches() override;

private:
    IntrusiveList<&USB::EHCI::EHCIController::m_driver_list_node> m_devices;
};

ErrorOr<void> EHCIDriver::probe(PCI::Device& pci_device)
{
    auto device = TRY(USB::EHCI::EHCIController::try_to_initialize(pci_device));
    m_devices.append(*device);
    return {};
}

void EHCIDriver::detach(PCI::Device&)
{
    TODO();
}

const static HardwareIDMatch __matches[] = {
    {
        .subclass_code = to_underlying(PCI::SerialBus::SubclassID::USB),
        .revision_id = Optional<RevisionID> {},
        .hardware_id = {
            0xffff,
            0xffff,
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = to_underlying(PCI::SerialBus::USBProgIf::EHCI),
    },
};

Span<HardwareIDMatch const> EHCIDriver::matches()
{
    return __matches;
}

void EHCIDriver::init()
{
    auto driver = MUST(adopt_nonnull_ref_or_enomem(new EHCIDriver()));
    PCI::Access::the().register_driver(driver);
}

PCI_DEVICE_DRIVER(EHCIDriver);

}
