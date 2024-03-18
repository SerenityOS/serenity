/*
 * Copyright (c) 2023, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Bus/PCI/Access.h>
#include <Kernel/Bus/PCI/Drivers/Driver.h>
#include <Kernel/Bus/USB/UHCI/UHCIController.h>

namespace Kernel::PCI {

class UHCIDriver final : public PCI::Driver {
public:
    static void init();

    UHCIDriver()
        : Driver("UHCIDriver"sv)
    {
    }

    virtual ErrorOr<void> probe(PCI::Device&) override;
    virtual void detach(PCI::Device&) override;
    virtual ClassID class_id() const override { return PCI::ClassID::SerialBus; }
    virtual Span<HardwareIDMatch const> matches() override;

private:
    IntrusiveList<&USB::UHCIController::m_driver_list_node> m_devices;
};

ErrorOr<void> UHCIDriver::probe(PCI::Device& pci_device)
{
    auto device = TRY(USB::UHCIController::try_to_initialize(pci_device));
    m_devices.append(*device);
    return {};
}

void UHCIDriver::detach(PCI::Device&)
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
        .programming_interface = to_underlying(PCI::SerialBus::USBProgIf::UHCI),
    },
};

Span<HardwareIDMatch const> UHCIDriver::matches()
{
    return __matches;
}

void UHCIDriver::init()
{
    auto driver = MUST(adopt_nonnull_ref_or_enomem(new UHCIDriver()));
    PCI::Access::the().register_driver(driver);
}

PCI_DEVICE_DRIVER(UHCIDriver);

}
