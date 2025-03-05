/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringView.h>
#include <Kernel/Arch/x86_64/Firmware/PCBIOS/DMI/Definitions.h>
#include <Kernel/Arch/x86_64/Firmware/PCBIOS/Mapper.h>
#include <Kernel/Arch/x86_64/Firmware/PCBIOS/SysFSComponent.h>
#include <Kernel/Arch/x86_64/Firmware/PCBIOS/SysFSDirectory.h>
#include <Kernel/FileSystem/OpenFileDescription.h>
#include <Kernel/Library/KBufferBuilder.h>
#include <Kernel/Memory/MemoryManager.h>
#include <Kernel/Memory/TypedMapping.h>
#include <Kernel/Sections.h>

namespace Kernel {

#define SMBIOS_BASE_SEARCH_ADDR 0xf0000
#define SMBIOS_END_SEARCH_ADDR 0xfffff
#define SMBIOS_SEARCH_AREA_SIZE (SMBIOS_END_SEARCH_ADDR - SMBIOS_BASE_SEARCH_ADDR)

UNMAP_AFTER_INIT void SysFSBIOSDirectory::set_dmi_64_bit_entry_initialization_values()
{
    dbgln("SysFSBIOSDirectory: SMBIOS 64bit Entry point @ {}", m_dmi_entry_point);
    auto smbios_entry = Memory::map_typed<SMBIOS::EntryPoint64bit>(m_dmi_entry_point, SMBIOS_SEARCH_AREA_SIZE).release_value_but_fixme_should_propagate_errors();
    m_smbios_structure_table = PhysicalAddress(smbios_entry.ptr()->table_ptr);
    m_dmi_entry_point_length = smbios_entry.ptr()->length;
    m_smbios_structure_table_length = smbios_entry.ptr()->table_maximum_size;
}

UNMAP_AFTER_INIT void SysFSBIOSDirectory::set_dmi_32_bit_entry_initialization_values()
{
    dbgln("SysFSBIOSDirectory: SMBIOS 32bit Entry point @ {}", m_dmi_entry_point);
    auto smbios_entry = Memory::map_typed<SMBIOS::EntryPoint32bit>(m_dmi_entry_point, SMBIOS_SEARCH_AREA_SIZE).release_value_but_fixme_should_propagate_errors();
    m_smbios_structure_table = PhysicalAddress(smbios_entry.ptr()->legacy_structure.smbios_table_ptr);
    m_dmi_entry_point_length = smbios_entry.ptr()->length;
    m_smbios_structure_table_length = smbios_entry.ptr()->legacy_structure.smbios_table_length;
}

UNMAP_AFTER_INIT NonnullRefPtr<SysFSBIOSDirectory> SysFSBIOSDirectory::must_create(SysFSFirmwareDirectory& firmware_directory)
{
    auto bios_directory = MUST(adopt_nonnull_ref_or_enomem(new (nothrow) SysFSBIOSDirectory(firmware_directory)));
    bios_directory->create_components();
    return bios_directory;
}

void SysFSBIOSDirectory::create_components()
{
    if (m_dmi_entry_point.is_null() || m_smbios_structure_table.is_null())
        return;
    if (m_dmi_entry_point_length == 0) {
        dbgln("SysFSBIOSDirectory: invalid dmi entry length");
        return;
    }
    if (m_smbios_structure_table_length == 0) {
        dbgln("SysFSBIOSDirectory: invalid smbios structure table length");
        return;
    }
    MUST(m_child_components.with([&](auto& list) -> ErrorOr<void> {
        list.append(SysFSPCBIOSComponent::must_create(SysFSPCBIOSComponent::Type::DMIEntryPoint, m_dmi_entry_point, m_dmi_entry_point_length));
        list.append(SysFSPCBIOSComponent::must_create(SysFSPCBIOSComponent::Type::SMBIOSTable, m_smbios_structure_table, m_smbios_structure_table_length));
        return {};
    }));
}

UNMAP_AFTER_INIT void SysFSBIOSDirectory::initialize_dmi_exposer()
{
    VERIFY(!(m_dmi_entry_point.is_null()));
    if (m_using_64bit_dmi_entry_point.was_set()) {
        set_dmi_64_bit_entry_initialization_values();
    } else {
        set_dmi_32_bit_entry_initialization_values();
    }
    dbgln("SysFSBIOSDirectory: Data table @ {}", m_smbios_structure_table);
}

UNMAP_AFTER_INIT SysFSBIOSDirectory::SysFSBIOSDirectory(SysFSFirmwareDirectory& firmware_directory)
    : SysFSDirectory(firmware_directory)
{
    auto entry_32bit = find_dmi_entry32bit_point();
    if (entry_32bit.has_value()) {
        m_dmi_entry_point = entry_32bit.value();
    }

    auto entry_64bit = find_dmi_entry64bit_point();
    if (entry_64bit.has_value()) {
        m_dmi_entry_point = entry_64bit.value();
        m_using_64bit_dmi_entry_point.set();
    }
    if (m_dmi_entry_point.is_null())
        return;
    initialize_dmi_exposer();
}

UNMAP_AFTER_INIT Optional<PhysicalAddress> SysFSBIOSDirectory::find_dmi_entry64bit_point()
{
    if (!g_boot_info.smbios.entry_point_paddr.is_null() && g_boot_info.smbios.entry_point_is_64_bit)
        return g_boot_info.smbios.entry_point_paddr;

    if (g_boot_info.boot_method != BootMethod::Multiboot1)
        return {};

    auto bios_or_error = map_bios();
    if (bios_or_error.is_error())
        return {};
    return bios_or_error.value().find_chunk_starting_with("_SM3_"sv, 16);
}

UNMAP_AFTER_INIT Optional<PhysicalAddress> SysFSBIOSDirectory::find_dmi_entry32bit_point()
{
    if (!g_boot_info.smbios.entry_point_paddr.is_null() && !g_boot_info.smbios.entry_point_is_64_bit)
        return g_boot_info.smbios.entry_point_paddr;

    if (g_boot_info.boot_method != BootMethod::Multiboot1)
        return {};

    auto bios_or_error = map_bios();
    if (bios_or_error.is_error())
        return {};
    return bios_or_error.value().find_chunk_starting_with("_SM_"sv, 16);
}

}
