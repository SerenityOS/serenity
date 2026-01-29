/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringView.h>
#include <Kernel/Arch/x86_64/Firmware/PCBIOS/Mapper.h>
#include <Kernel/FileSystem/OpenFileDescription.h>
#include <Kernel/Firmware/SMBIOS/Definitions.h>
#include <Kernel/Firmware/SMBIOS/SysFSComponent.h>
#include <Kernel/Firmware/SMBIOS/SysFSDirectory.h>
#include <Kernel/Library/KBufferBuilder.h>
#include <Kernel/Memory/MemoryManager.h>
#include <Kernel/Memory/TypedMapping.h>
#include <Kernel/Sections.h>

// SMBIOS Specification Version 3.9.0

namespace Kernel {

// 5.2.2 SMBIOS 3.0 (64-bit) Entry Point
// "On non-UEFI systems, the 64-bit SMBIOS Entry Point structure can be located by application software by
//  searching for the anchor-string on paragraph (16-byte) boundaries within the physical memory address
//  range 000F0000h to 000FFFFFh."
// This same text also appears for the 32-bit entry point.
static constexpr PhysicalPtr SMBIOS_BASE_SEARCH_ADDR = 0xf'0000;
static constexpr PhysicalPtr SMBIOS_END_SEARCH_ADDR = 0xf'ffff;
static constexpr size_t SMBIOS_SEARCH_AREA_SIZE = SMBIOS_END_SEARCH_ADDR - SMBIOS_BASE_SEARCH_ADDR;

UNMAP_AFTER_INIT void SysFSSMBIOSDirectory::set_smbios_64_bit_entry_initialization_values()
{
    dbgln("SysFSSMBIOSDirectory: SMBIOS 64bit Entry point @ {}", m_smbios_entry_point);
    auto smbios_entry = Memory::map_typed<SMBIOS::EntryPoint64bit>(m_smbios_entry_point, SMBIOS_SEARCH_AREA_SIZE).release_value_but_fixme_should_propagate_errors();
    m_smbios_structure_table = PhysicalAddress(smbios_entry.ptr()->table_ptr);
    m_smbios_entry_point_length = smbios_entry.ptr()->length;
    m_smbios_structure_table_length = smbios_entry.ptr()->table_maximum_size;
}

UNMAP_AFTER_INIT void SysFSSMBIOSDirectory::set_smbios_32_bit_entry_initialization_values()
{
    dbgln("SysFSSMBIOSDirectory: SMBIOS 32bit Entry point @ {}", m_smbios_entry_point);
    auto smbios_entry = Memory::map_typed<SMBIOS::EntryPoint32bit>(m_smbios_entry_point, SMBIOS_SEARCH_AREA_SIZE).release_value_but_fixme_should_propagate_errors();
    m_smbios_structure_table = PhysicalAddress(smbios_entry.ptr()->legacy_structure.smbios_table_ptr);
    m_smbios_entry_point_length = smbios_entry.ptr()->length;
    m_smbios_structure_table_length = smbios_entry.ptr()->legacy_structure.smbios_table_length;
}

UNMAP_AFTER_INIT NonnullRefPtr<SysFSSMBIOSDirectory> SysFSSMBIOSDirectory::must_create(SysFSFirmwareDirectory& firmware_directory)
{
    auto bios_directory = MUST(adopt_nonnull_ref_or_enomem(new (nothrow) SysFSSMBIOSDirectory(firmware_directory)));
    bios_directory->create_components();
    return bios_directory;
}

void SysFSSMBIOSDirectory::create_components()
{
    if (m_smbios_entry_point.is_null() || m_smbios_structure_table.is_null())
        return;
    if (m_smbios_entry_point_length == 0) {
        dbgln("SysFSSMBIOSDirectory: invalid smbios entry length");
        return;
    }
    if (m_smbios_structure_table_length == 0) {
        dbgln("SysFSSMBIOSDirectory: invalid smbios structure table length");
        return;
    }
    MUST(m_child_components.with([&](auto& list) -> ErrorOr<void> {
        list.append(SysFSSMBIOSComponent::must_create(SysFSSMBIOSComponent::Type::SMBIOSEntryPoint, m_smbios_entry_point, m_smbios_entry_point_length));
        list.append(SysFSSMBIOSComponent::must_create(SysFSSMBIOSComponent::Type::SMBIOSTable, m_smbios_structure_table, m_smbios_structure_table_length));
        return {};
    }));
}

UNMAP_AFTER_INIT void SysFSSMBIOSDirectory::initialize_smbios_exposer()
{
    VERIFY(!(m_smbios_entry_point.is_null()));
    if (m_using_64bit_smbios_entry_point.was_set()) {
        set_smbios_64_bit_entry_initialization_values();
    } else {
        set_smbios_32_bit_entry_initialization_values();
    }
    dbgln("SysFSSMBIOSDirectory: Data table @ {}", m_smbios_structure_table);
}

UNMAP_AFTER_INIT SysFSSMBIOSDirectory::SysFSSMBIOSDirectory(SysFSFirmwareDirectory& firmware_directory)
    : SysFSDirectory(firmware_directory)
{
    auto entry_32bit = find_smbios_entry32bit_point();
    if (entry_32bit.has_value()) {
        m_smbios_entry_point = entry_32bit.value();
    }

    auto entry_64bit = find_smbios_entry64bit_point();
    if (entry_64bit.has_value()) {
        m_smbios_entry_point = entry_64bit.value();
        m_using_64bit_smbios_entry_point.set();
    }
    if (m_smbios_entry_point.is_null())
        return;
    initialize_smbios_exposer();
}

template<typename EntryPoint>
static bool is_entry_point_valid(ReadonlyBytes entry_point)
{
    if (entry_point.size() < sizeof(EntryPoint))
        return false;

    auto entry_point_length = reinterpret_cast<EntryPoint const*>(entry_point.data())->length;
    if (entry_point.size() < entry_point_length)
        return false;

    u8 checksum = 0;
    for (u8 byte : ReadonlyBytes { entry_point.data(), entry_point_length })
        checksum += byte;

    return checksum == 0;
}

UNMAP_AFTER_INIT Optional<PhysicalAddress> SysFSSMBIOSDirectory::find_smbios_entry64bit_point()
{
    if (!g_boot_info.smbios.entry_point_paddr.is_null() && g_boot_info.smbios.entry_point_is_64_bit)
        return g_boot_info.smbios.entry_point_paddr;

#if ARCH(X86_64)
    if (g_boot_info.boot_method != BootMethod::Multiboot1)
        return {};

    auto bios_or_error = map_bios();
    if (bios_or_error.is_error())
        return {};
    return bios_or_error.value().find_chunk_starting_with("_SM3_"sv, 16, is_entry_point_valid<SMBIOS::EntryPoint64bit>);
#else
    return {};
#endif
}

UNMAP_AFTER_INIT Optional<PhysicalAddress> SysFSSMBIOSDirectory::find_smbios_entry32bit_point()
{
    if (!g_boot_info.smbios.entry_point_paddr.is_null() && !g_boot_info.smbios.entry_point_is_64_bit)
        return g_boot_info.smbios.entry_point_paddr;

#if ARCH(X86_64)
    if (g_boot_info.boot_method != BootMethod::Multiboot1)
        return {};

    auto bios_or_error = map_bios();
    if (bios_or_error.is_error())
        return {};
    return bios_or_error.value().find_chunk_starting_with("_SM_"sv, 16, is_entry_point_valid<SMBIOS::EntryPoint32bit>);
#else
    return {};
#endif
}

}
