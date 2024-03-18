/*
 * Copyright (c) 2023, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Bus/PCI/Access.h>
#include <Kernel/Bus/PCI/Drivers/Driver.h>
#include <Kernel/Devices/GPU/Bochs/GraphicsAdapter.h>

namespace Kernel::PCI {

class BochsDisplayDriver final : public PCI::Driver {
public:
    static void init();

    BochsDisplayDriver()
        : PCI::Driver("BochsDisplayDriver"sv)
    {
    }

    virtual ErrorOr<void> probe(PCI::Device&) override;
    virtual void detach(PCI::Device&) override;
    virtual ClassID class_id() const override { return PCI::ClassID::Display; }
    virtual Span<HardwareIDMatch const> matches() override;

private:
    IntrusiveList<&BochsGraphicsAdapter::m_driver_list_node> m_devices;
};

ErrorOr<void> BochsDisplayDriver::probe(PCI::Device& pci_device)
{
    auto device = TRY(BochsGraphicsAdapter::create(pci_device));
    m_devices.append(*device);
    return {};
}

void BochsDisplayDriver::detach(PCI::Device&)
{
    TODO();
}

const static HardwareIDMatch __matches[] = {
    // qemu VGA bochs device
    {
        .subclass_code = to_underlying(PCI::Display::SubclassID::VGA),
        .revision_id = Optional<RevisionID> {},
        .hardware_id = {
            PCI::VendorID::QEMUOld,
            0x1111,
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    // VirtualBox VGA bochs compatible device
    {
        .subclass_code = to_underlying(PCI::Display::SubclassID::VGA),
        .revision_id = Optional<RevisionID> {},
        .hardware_id = {
            PCI::VendorID::VirtualBox,
            0xbeef,
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    // qemu bochs-display device
    {
        .subclass_code = to_underlying(PCI::Display::SubclassID::Other),
        .revision_id = Optional<RevisionID> {},
        .hardware_id = {
            PCI::VendorID::QEMUOld,
            0x1111,
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
};

Span<HardwareIDMatch const> BochsDisplayDriver::matches()
{
    return __matches;
}

void BochsDisplayDriver::init()
{
    auto driver = MUST(adopt_nonnull_ref_or_enomem(new BochsDisplayDriver()));
    PCI::Access::the().register_driver(driver);
}

PCI_DEVICE_DRIVER(BochsDisplayDriver);

}
