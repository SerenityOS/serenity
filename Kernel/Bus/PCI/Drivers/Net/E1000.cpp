/*
 * Copyright (c) 2023, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Bus/PCI/Access.h>
#include <Kernel/Bus/PCI/Drivers/Driver.h>
#include <Kernel/Net/Intel/E1000NetworkAdapter.h>
#include <Kernel/Net/NetworkingManagement.h>

namespace Kernel::PCI {

class E1000Driver final : public PCI::Driver {
public:
    static void init();

    E1000Driver()
        : PCI::Driver("E1000Driver"sv)
    {
    }

    virtual ErrorOr<void> probe(PCI::Device&) override;
    virtual void detach(PCI::Device&) override;
    virtual ClassID class_id() const override { return PCI::ClassID::Network; }
    virtual Span<HardwareIDMatch const> matches() override;

private:
    IntrusiveList<&E1000NetworkAdapter::m_driver_list_node> m_devices;
};

ErrorOr<void> E1000Driver::probe(PCI::Device& pci_device)
{
    auto device = TRY(E1000NetworkAdapter::create(pci_device));
    m_devices.append(*device);
    NetworkingManagement::the().attach_adapter(*device);
    return {};
}

void E1000Driver::detach(PCI::Device&)
{
    TODO();
}

const static HardwareIDMatch __matches[] = {
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = {
            PCI::VendorID::Intel,
            0x1019, // 82547EI-A0, 82547EI-A1, 82547EI-B0, 82547GI-B0
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = {
            PCI::VendorID::Intel,
            0x101A, // 82547EI-B0
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = {
            PCI::VendorID::Intel,
            0x1010, // 82546EB-A1
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = {
            PCI::VendorID::Intel,
            0x1012, // 82546EB-A1
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = {
            PCI::VendorID::Intel,
            0x101D, // 82546EB-A1
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = {
            PCI::VendorID::Intel,
            0x1079, // 82546GB-B0
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = {
            PCI::VendorID::Intel,
            0x107A, // 82546GB-B0
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = {
            PCI::VendorID::Intel,
            0x107B, // 82546GB-B0
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = {
            PCI::VendorID::Intel,
            0x100F, // 82545EM-A
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = {
            PCI::VendorID::Intel,
            0x1011, // 82545EM-A
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = {
            PCI::VendorID::Intel,
            0x1026, // 82545GM-B
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = {
            PCI::VendorID::Intel,
            0x1027, // 82545GM-B
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = {
            PCI::VendorID::Intel,
            0x1028, // 82545GM-B
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = {
            PCI::VendorID::Intel,
            0x1107, // 82544EI-A4
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = {
            PCI::VendorID::Intel,
            0x1112, // 82544GC-A4
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = {
            PCI::VendorID::Intel,
            0x1013, // 82541EI-A0, 82541EI-B0
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = {
            PCI::VendorID::Intel,
            0x1018, // 82541EI-B0
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = {
            PCI::VendorID::Intel,
            0x1076, // 82541GI-B1, 82541PI-C0
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = {
            PCI::VendorID::Intel,
            0x1077, // 82541GI-B1
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = {
            PCI::VendorID::Intel,
            0x1078, // 82541ER-C0
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = {
            PCI::VendorID::Intel,
            0x1017, // 82540EP-A
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = {
            PCI::VendorID::Intel,
            0x1016, // 82540EP-A
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = {
            PCI::VendorID::Intel,
            0x100E, // 82540EM-A
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = {
            PCI::VendorID::Intel,
            0x1015, // 82540EM-A
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
};

Span<HardwareIDMatch const> E1000Driver::matches()
{
    return __matches;
}

void E1000Driver::init()
{
    auto driver = MUST(adopt_nonnull_ref_or_enomem(new E1000Driver()));
    PCI::Access::the().register_driver(driver);
}

PCI_DEVICE_DRIVER(E1000Driver);

}
