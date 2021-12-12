/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefPtr.h>
#include <AK/Types.h>
#include <AK/Vector.h>
#include <Kernel/FileSystem/SysFS.h>
#include <Kernel/Firmware/SysFSFirmware.h>
#include <Kernel/KBuffer.h>
#include <Kernel/Memory/MappedROM.h>
#include <Kernel/Memory/Region.h>
#include <Kernel/PhysicalAddress.h>
#include <Kernel/VirtualAddress.h>

namespace Kernel::SMBIOS {

struct [[gnu::packed]] LegacyEntryPoint32bit {
    char legacy_sig[5];
    u8 checksum2;
    u16 smboios_table_length;
    u32 smbios_table_ptr;
    u16 smbios_tables_count;
    u8 smbios_bcd_revision;
};

struct [[gnu::packed]] EntryPoint32bit {
    char sig[4];
    u8 checksum;
    u8 length;
    u8 major_version;
    u8 minor_version;
    u16 maximum_structure_size;
    u8 implementation_revision;
    char formatted_area[5];
    LegacyEntryPoint32bit legacy_structure;
};

struct [[gnu::packed]] EntryPoint64bit {
    char sig[5];
    u8 checksum;
    u8 length;
    u8 major_version;
    u8 minor_version;
    u8 document_revision;
    u8 revision;
    u8 reserved;
    u32 table_maximum_size;
    u64 table_ptr;
};
}

namespace Kernel {

Memory::MappedROM map_bios();
Memory::MappedROM map_ebda();

class BIOSSysFSComponent : public SysFSComponent {
public:
    virtual ErrorOr<size_t> read_bytes(off_t, size_t, UserOrKernelBuffer&, OpenFileDescription*) const override;

protected:
    virtual ErrorOr<NonnullOwnPtr<KBuffer>> try_to_generate_buffer() const = 0;
    BIOSSysFSComponent();
};

class DMIEntryPointExposedBlob : public BIOSSysFSComponent {
public:
    virtual StringView name() const override { return "smbios_entry_point"sv; }
    static NonnullRefPtr<DMIEntryPointExposedBlob> must_create(PhysicalAddress dmi_entry_point, size_t blob_size);

private:
    DMIEntryPointExposedBlob(PhysicalAddress dmi_entry_point, size_t blob_size);
    virtual ErrorOr<NonnullOwnPtr<KBuffer>> try_to_generate_buffer() const override;
    PhysicalAddress m_dmi_entry_point;
    size_t m_dmi_entry_point_length;
};

class SMBIOSExposedTable : public BIOSSysFSComponent {
public:
    virtual StringView name() const override { return "DMI"sv; }
    static NonnullRefPtr<SMBIOSExposedTable> must_create(PhysicalAddress, size_t blob_size);

private:
    SMBIOSExposedTable(PhysicalAddress dmi_entry_point, size_t blob_size);
    virtual ErrorOr<NonnullOwnPtr<KBuffer>> try_to_generate_buffer() const override;

    PhysicalAddress m_smbios_structure_table;
    size_t m_smbios_structure_table_length;
};

class BIOSSysFSDirectory : public SysFSDirectory {
public:
    virtual StringView name() const override { return "bios"sv; }
    static NonnullRefPtr<BIOSSysFSDirectory> must_create(FirmwareSysFSDirectory&);

    void create_components();

private:
    explicit BIOSSysFSDirectory(FirmwareSysFSDirectory&);

    void set_dmi_64_bit_entry_initialization_values();
    void set_dmi_32_bit_entry_initialization_values();
    void initialize_dmi_exposer();

    Optional<PhysicalAddress> find_dmi_entry64bit_point();
    Optional<PhysicalAddress> find_dmi_entry32bit_point();

    PhysicalAddress m_dmi_entry_point;
    PhysicalAddress m_smbios_structure_table;
    bool m_using_64bit_dmi_entry_point { false };
    size_t m_smbios_structure_table_length { 0 };
    size_t m_dmi_entry_point_length { 0 };
};

}
