/*
 * Copyright (c) 2020, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ByteReader.h>
#include <AK/HashTable.h>
#include <Kernel/API/KResult.h>
#include <Kernel/Arch/x86/IO.h>
#include <Kernel/Bus/PCI/API.h>
#include <Kernel/Bus/PCI/Access.h>
#include <Kernel/Debug.h>
#include <Kernel/Firmware/ACPI/Definitions.h>
#include <Kernel/Memory/MemoryManager.h>
#include <Kernel/Memory/Region.h>
#include <Kernel/Sections.h>

namespace Kernel::PCI {

struct QuirkMatch {
    u8 class_code;
    u8 subclass_code;
    u8 prog_if;
    u8 revision_id;
    HardwareID hardware_id;
    void (*quirk_apply)(DeviceIdentifier&);
};

class Quirks {
public:
    static void sb600_fix_sata_mode(DeviceIdentifier& identifier)
    {
        dbgln("PCI: Set {} to AHCI mode", identifier.address());
        u8 misc_control = PCI::read32(identifier.address(), (PCI::RegisterOffset)0x40);
        PCI::write32(identifier.address(), (PCI::RegisterOffset)0x40, misc_control | 1);
        PCI::write32(identifier.address(), PCI::RegisterOffset::SUBCLASS, to_underlying(MassStorage::SubclassID::SATAController));
        PCI::write32(identifier.address(), PCI::RegisterOffset::PROG_IF, to_underlying(MassStorage::SATAProgIF::AHCI));
        PCI::write32(identifier.address(), (PCI::RegisterOffset)0x40, misc_control);
        identifier.apply_subclass_code_change({}, 0x6);
        identifier.apply_prog_if_change({}, 0x1);
    }
};

static constexpr QuirkMatch pci_quirks[] {
    // Apply quirks on AMD SB600 IDE controllers
    { 0x1, 0x1, 0xFF, 0xFF, { 0x1002, 0x4390 }, Quirks::sb600_fix_sata_mode },
    { 0x1, 0x1, 0xFF, 0xFF, { 0x1002, 0x4380 }, Quirks::sb600_fix_sata_mode },
    { 0x1, 0x1, 0xFF, 0xFF, { 0x1022, 0x7800 }, Quirks::sb600_fix_sata_mode },
    { 0x1, 0x1, 0xFF, 0xFF, { 0x1022, 0x7900 }, Quirks::sb600_fix_sata_mode }
};

static bool is_matching_class_code_for_quirk(size_t quirk_index, ClassCode class_code)
{
    VERIFY(quirk_index < (sizeof(pci_quirks) / sizeof(QuirkMatch)));
    if (pci_quirks[quirk_index].class_code == 0xFF)
        return true;
    return pci_quirks[quirk_index].class_code == class_code.value();
}

static bool is_matching_subclass_code_for_quirk(size_t quirk_index, SubclassCode subclass_code)
{
    VERIFY(quirk_index < (sizeof(pci_quirks) / sizeof(QuirkMatch)));
    if (pci_quirks[quirk_index].subclass_code == 0xFF)
        return true;
    return pci_quirks[quirk_index].subclass_code == subclass_code.value();
}

static bool is_matching_programming_interface_for_quirk(size_t quirk_index, ProgrammingInterface prog_if)
{
    VERIFY(quirk_index < (sizeof(pci_quirks) / sizeof(QuirkMatch)));
    if (pci_quirks[quirk_index].prog_if == 0xFF)
        return true;
    return pci_quirks[quirk_index].prog_if == prog_if.value();
}

static bool is_matching_revision_id_for_quirk(size_t quirk_index, RevisionID revision_id)
{
    VERIFY(quirk_index < (sizeof(pci_quirks) / sizeof(QuirkMatch)));
    if (pci_quirks[quirk_index].revision_id == 0xFF)
        return true;
    return pci_quirks[quirk_index].revision_id == revision_id.value();
}

static bool is_matching_hardware_id_for_quirk(size_t quirk_index, HardwareID hardware_id)
{
    VERIFY(quirk_index < (sizeof(pci_quirks) / sizeof(QuirkMatch)));
    // Note: We don't support wildcards for Hardware ID as this can lead
    // to very dangerous quirk fixes being applied.
    VERIFY(pci_quirks[quirk_index].hardware_id.vendor_id != 0xFFFF);
    VERIFY(pci_quirks[quirk_index].hardware_id.device_id != 0xFFFF);
    return pci_quirks[quirk_index].hardware_id.vendor_id == hardware_id.vendor_id
        && pci_quirks[quirk_index].hardware_id.device_id == hardware_id.device_id;
}

UNMAP_AFTER_INIT void Access::apply_quirks()
{
    VERIFY(!m_device_identifiers.is_empty());
    for (auto& device_identifier : m_device_identifiers) {
        for (size_t quirk_index = 0; quirk_index < (sizeof(pci_quirks) / sizeof(QuirkMatch)); quirk_index++) {
            if (is_matching_class_code_for_quirk(quirk_index, device_identifier.class_code())
                && is_matching_subclass_code_for_quirk(quirk_index, device_identifier.subclass_code())
                && is_matching_programming_interface_for_quirk(quirk_index, device_identifier.prog_if())
                && is_matching_revision_id_for_quirk(quirk_index, device_identifier.revision_id())
                && is_matching_hardware_id_for_quirk(quirk_index, device_identifier.hardware_id())) {
                pci_quirks[quirk_index].quirk_apply(device_identifier);
            }
        }
    }
}

}
