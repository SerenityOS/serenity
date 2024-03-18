/*
 * Copyright (c) 2023, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Bus/PCI/Access.h>
#include <Kernel/Bus/PCI/Drivers/Driver.h>
#include <Kernel/Devices/Audio/AC97/AC97.h>

namespace Kernel::PCI {

class AC97Driver final : public PCI::Driver {
public:
    static void init();

    AC97Driver()
        : PCI::Driver("AC97Driver"sv)
    {
    }

    virtual ErrorOr<void> probe(PCI::Device&) override;
    virtual void detach(PCI::Device&) override;
    virtual ClassID class_id() const override { return PCI::ClassID::Multimedia; }
    virtual Span<HardwareIDMatch const> matches() override;

private:
    IntrusiveList<&AC97::m_driver_list_node> m_devices;
};

ErrorOr<void> AC97Driver::probe(PCI::Device& pci_device)
{
    dbgln("AC97 @ {} initializing", pci_device.device_id().address());
    auto device = TRY(AC97::create(pci_device));
    m_devices.append(*device);
    return {};
}

void AC97Driver::detach(PCI::Device&)
{
    TODO();
}

const static HardwareIDMatch __matches[] = {
    {
        .subclass_code = to_underlying(PCI::Multimedia::SubclassID::Audio),
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID { 0xffff, 0xffff },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
};

Span<HardwareIDMatch const> AC97Driver::matches()
{
    // TODO: Add PCI ids.
    return __matches;
}

void AC97Driver::init()
{
    auto driver = MUST(adopt_nonnull_ref_or_enomem(new AC97Driver()));
    PCI::Access::the().register_driver(driver);
}

PCI_DEVICE_DRIVER(AC97Driver);

}
