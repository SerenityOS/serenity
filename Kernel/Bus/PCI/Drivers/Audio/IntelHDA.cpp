/*
 * Copyright (c) 2023, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Bus/PCI/Access.h>
#include <Kernel/Bus/PCI/Drivers/Driver.h>
#include <Kernel/Devices/Audio/IntelHDA/Controller.h>

namespace Kernel::PCI {

class IntelHDADriver final : public PCI::Driver {
public:
    static void init();

    IntelHDADriver()
        : PCI::Driver("IntelHDADriver"sv)
    {
    }

    virtual ErrorOr<void> probe(PCI::Device&) override;
    virtual void detach(PCI::Device&) override;
    virtual ClassID class_id() const override { return PCI::ClassID::Multimedia; }
    virtual Span<HardwareIDMatch const> matches() override;

private:
    IntrusiveList<&Audio::IntelHDA::Controller::m_driver_list_node> m_devices;
};

ErrorOr<void> IntelHDADriver::probe(PCI::Device& pci_device)
{
    auto device = TRY(Audio::IntelHDA::Controller::create(pci_device));
    m_devices.append(*device);
    return {};
}

void IntelHDADriver::detach(PCI::Device&)
{
    TODO();
}

const static HardwareIDMatch __matches[] = {
    {
        .subclass_code = to_underlying(PCI::Multimedia::SubclassID::HDACompatible),
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID { 0xffff, 0xffff },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
};

Span<HardwareIDMatch const> IntelHDADriver::matches()
{
    return __matches;
}

void IntelHDADriver::init()
{
    auto driver = MUST(adopt_nonnull_ref_or_enomem(new IntelHDADriver()));
    PCI::Access::the().register_driver(driver);
}

PCI_DEVICE_DRIVER(IntelHDADriver);

}
