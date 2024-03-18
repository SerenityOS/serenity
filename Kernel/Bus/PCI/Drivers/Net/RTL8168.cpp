/*
 * Copyright (c) 2023, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Bus/PCI/Access.h>
#include <Kernel/Bus/PCI/Drivers/Driver.h>
#include <Kernel/Bus/PCI/IDs.h>
#include <Kernel/Net/NetworkingManagement.h>
#include <Kernel/Net/Realtek/RTL8168NetworkAdapter.h>

namespace Kernel::PCI {

class RTL8168Driver final : public PCI::Driver {
public:
    static void init();

    RTL8168Driver()
        : PCI::Driver("RTL8168Driver"sv)
    {
    }

    virtual ErrorOr<void> probe(PCI::Device&) override;
    virtual void detach(PCI::Device&) override;
    virtual ClassID class_id() const override { return PCI::ClassID::Network; }
    virtual Span<HardwareIDMatch const> matches() override;

private:
    IntrusiveList<&RTL8168NetworkAdapter::m_driver_list_node> m_devices;
};

ErrorOr<void> RTL8168Driver::probe(PCI::Device& device)
{
    auto adapter = TRY(RTL8168NetworkAdapter::create(device));
    m_devices.append(*adapter);
    NetworkingManagement::the().attach_adapter(*adapter);
    return {};
}

void RTL8168Driver::detach(PCI::Device&)
{
    TODO();
}

const static HardwareIDMatch __matches[] = {
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Realtek,
            0x8168,
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
};

Span<HardwareIDMatch const> RTL8168Driver::matches()
{
    return __matches;
}

void RTL8168Driver::init()
{
    auto driver = MUST(adopt_nonnull_ref_or_enomem(new RTL8168Driver()));
    PCI::Access::the().register_driver(driver);
}

PCI_DEVICE_DRIVER(RTL8168Driver);

}
