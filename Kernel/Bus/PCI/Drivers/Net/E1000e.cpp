/*
 * Copyright (c) 2023, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Bus/PCI/Access.h>
#include <Kernel/Bus/PCI/Drivers/Driver.h>
#include <Kernel/Bus/PCI/IDs.h>
#include <Kernel/Net/Intel/E1000ENetworkAdapter.h>
#include <Kernel/Net/NetworkingManagement.h>

namespace Kernel::PCI {

class E1000eDriver final : public PCI::Driver {
public:
    static void init();

    E1000eDriver()
        : PCI::Driver("E1000eDriver"sv)
    {
    }

    virtual ErrorOr<void> probe(PCI::Device&) override;
    virtual void detach(PCI::Device&) override;
    virtual ClassID class_id() const override { return PCI::ClassID::Network; }
    virtual Span<HardwareIDMatch const> matches() override;

private:
    IntrusiveList<&E1000ENetworkAdapter::m_driver_list_node> m_devices;
};

ErrorOr<void> E1000eDriver::probe(PCI::Device& pci_device)
{
    // NOTE: Although we advertise for many supported device IDs,
    // we actually only support the Intel 82574L device currently.
    if (pci_device.device_id().hardware_id().device_id != 0x10D3)
        return Error::from_errno(ENOTSUP);
    auto device = TRY(E1000ENetworkAdapter::create(pci_device));
    m_devices.append(*device);
    NetworkingManagement::the().attach_adapter(*device);
    return {};
}

void E1000eDriver::detach(PCI::Device&)
{
    TODO();
}

const static HardwareIDMatch __matches[] = {
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x10D3, // 82574L
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x1000, // 82542
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x0438, // DH89XXCC_SGMII
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x043A, // DH89XXCC_SERDES
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x043C, // DH89XXCC_BACKPLANE
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x0440, // DH89XXCC_SFP
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x1001, // 82543GC_FIBER
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x1004, // 82543GC_COPPER
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x1008, // 82544EI_COPPER
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x1009, // 82544EI_FIBER
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x100C, // 82544GC_COPPER
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x100D, // 82544GC_LOM
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x100E, // 82540EM
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x100F, // 82545EM_COPPER
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x1010, // 82546EB_COPPER
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x1011, // 82545EM_FIBER
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x1012, // 82546EB_FIBER
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x1013, // 82541EI
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x1014, // 82541ER_LOM
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x1015, // 82540EM_LOM
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x1016, // 82540EP_LOM
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x1017, // 82540EP
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x1018, // 82541EI_MOBILE
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x1019, // 82547EI
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x101A, // 82547EI_MOBILE
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x101D, // 82546EB_QUAD_COPPER
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x101E, // 82540EP_LP
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x1026, // 82545GM_COPPER
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x1027, // 82545GM_FIBER
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x1028, // 82545GM_SERDES
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x1049, // ICH8_IGP_M_AMT
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x104A, // ICH8_IGP_AMT
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x104B, // ICH8_IGP_C
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x104C, // ICH8_IFE
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x104D, // ICH8_IGP_M
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x105E, // 82571EB_COPPER
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x105F, // 82571EB_FIBER
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x1060, // 82571EB_SERDES
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x1075, // 82547GI
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x1076, // 82541GI
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x1077, // 82541GI_MOBILE
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x1078, // 82541ER
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x1079, // 82546GB_COPPER
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x107A, // 82546GB_FIBER
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x107B, // 82546GB_SERDES
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x107C, // 82541GI_LF
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x107D, // 82572EI_COPPER
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x107E, // 82572EI_FIBER
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x107F, // 82572EI_SERDES
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x108A, // 82546GB_PCIE
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x108B, // 82573E
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x108C, // 82573E_IAMT
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x1096, // 80003ES2LAN_COPPER_DPT
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x1098, // 80003ES2LAN_SERDES_DPT
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x1099, // 82546GB_QUAD_COPPER
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x109A, // 82573L
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x10A4, // 82571EB_QUAD_COPPER
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x10A5, // 82571EB_QUAD_FIBER
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x10A7, // 82575EB_COPPER
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x10A9, // 82575EB_FIBER_SERDES
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x10B5, // 82546GB_QUAD_COPPER_KSP3
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x10B9, // 82572EI
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x10BA, // 80003ES2LAN_COPPER_SPT
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x10BB, // 80003ES2LAN_SERDES_SPT
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x10BC, // 82571EB_QUAD_COPPER_LP
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x10BD, // ICH9_IGP_AMT
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x10BF, // ICH9_IGP_M
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x10C0, // ICH9_IFE
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x10C2, // ICH9_IFE_G
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x10C3, // ICH9_IFE_GT
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x10C4, // ICH8_IFE_GT
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x10C5, // ICH8_IFE_G
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x10C9, // 82576
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x10CA, // 82576_VF
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x10CB, // ICH9_IGP_M_V
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x10CC, // ICH10_R_BM_LM
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x10CD, // ICH10_R_BM_LF
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x10CE, // ICH10_R_BM_V
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x10D5, // 82571PT_QUAD_COPPER
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x10D6, // 82575GB_QUAD_COPPER
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x10D9, // 82571EB_SERDES_DUAL
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x10DA, // 82571EB_SERDES_QUAD
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x10DE, // ICH10_D_BM_LM
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x10DF, // ICH10_D_BM_LF
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x10E5, // ICH9_BM
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x10E6, // 82576_FIBER
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x10E7, // 82576_SERDES
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x10E8, // 82576_QUAD_COPPER
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x10EA, // PCH_M_HV_LM
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x10EB, // PCH_M_HV_LC
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x10EF, // PCH_D_HV_DM
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x10F0, // PCH_D_HV_DC
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x10F5, // ICH9_IGP_M_AMT
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x10F6, // 82574LA
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x1501, // ICH8_82567V_3
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x1502, // PCH2_LV_LM
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x1503, // PCH2_LV_V
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x150A, // 82576_NS
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x150C, // 82583V
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x150D, // 82576_SERDES_QUAD
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x150E, // 82580_COPPER
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x150F, // 82580_FIBER
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x1510, // 82580_SERDES
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x1511, // 82580_SGMII
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x1516, // 82580_COPPER_DUAL
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x1518, // 82576_NS_SERDES
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x1520, // I350_VF
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x1521, // I350_COPPER
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x1522, // I350_FIBER
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x1523, // I350_SERDES
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x1524, // I350_SGMII
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x1525, // ICH10_D_BM_V
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x1526, // 82576_QUAD_COPPER_ET2
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x1527, // 82580_QUAD_FIBER
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x152D, // 82576_VF_HV
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x152F, // I350_VF_HV
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x1533, // I210_COPPER
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x1534, // I210_COPPER_OEM1
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x1535, // I210_COPPER_IT
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x1536, // I210_FIBER
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x1537, // I210_SERDES
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x1538, // I210_SGMII
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x1539, // I211_COPPER
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x153A, // PCH_LPT_I217_LM
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x153B, // PCH_LPT_I217_V
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x1546, // I350_DA4
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x1559, // PCH_LPTLP_I218_V
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x155A, // PCH_LPTLP_I218_LM
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x156F, // PCH_SPT_I219_LM
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x1570, // PCH_SPT_I219_V
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x157B, // I210_COPPER_FLASHLESS
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x157C, // I210_SERDES_FLASHLESS
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x15A0, // PCH_I218_LM2
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x15A1, // PCH_I218_V2
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x15A2, // PCH_I218_LM3
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x15A3, // PCH_I218_V3
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x15B7, // PCH_SPT_I219_LM2
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x15B8, // PCH_SPT_I219_V2
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x15B9, // PCH_LBG_I219_LM3
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x15BB, // PCH_CNP_I219_LM7
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x15BC, // PCH_CNP_I219_V7
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x15BD, // PCH_CNP_I219_LM6
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x15BE, // PCH_CNP_I219_V6
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x15D6, // PCH_SPT_I219_V5
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x15D7, // PCH_SPT_I219_LM4
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x15D8, // PCH_SPT_I219_V4
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x15DF, // PCH_ICP_I219_LM8
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x15E0, // PCH_ICP_I219_V8
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x15E1, // PCH_ICP_I219_LM9
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x15E2, // PCH_ICP_I219_V9
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x15E3, // PCH_SPT_I219_LM5
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x1F40, // I354_BACKPLANE_1GBPS
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x1F41, // I354_SGMII
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x1F45, // I354_BACKPLANE_2_5GBPS
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::Intel,
            0x294C, // ICH9_IGP_C
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
};

Span<HardwareIDMatch const> E1000eDriver::matches()
{
    return __matches;
}

void E1000eDriver::init()
{
    auto driver = MUST(adopt_nonnull_ref_or_enomem(new E1000eDriver()));
    PCI::Access::the().register_driver(driver);
}

PCI_DEVICE_DRIVER(E1000eDriver);

}
