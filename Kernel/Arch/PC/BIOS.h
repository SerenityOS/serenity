/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>
#include <AK/Vector.h>
#include <Kernel/FileSystem/SysFS.h>
#include <Kernel/KBuffer.h>
#include <Kernel/PhysicalAddress.h>
#include <Kernel/VM/MappedROM.h>
#include <Kernel/VM/Region.h>
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

MappedROM map_bios();
MappedROM map_ebda();

class BIOSSysFSComponent : public SysFSComponent {
public:
    virtual KResultOr<size_t> read_bytes(off_t, size_t, UserOrKernelBuffer&, FileDescription*) const override;

protected:
    virtual OwnPtr<KBuffer> try_to_generate_buffer() const = 0;
    explicit BIOSSysFSComponent(String name);
};

class DMIEntryPointExposedBlob : public BIOSSysFSComponent {
public:
    static NonnullRefPtr<DMIEntryPointExposedBlob> create(PhysicalAddress dmi_entry_point, size_t blob_size);
    virtual size_t size() const override { return m_dmi_entry_point_length; }

private:
    DMIEntryPointExposedBlob(PhysicalAddress dmi_entry_point, size_t blob_size);
    virtual OwnPtr<KBuffer> try_to_generate_buffer() const override;
    PhysicalAddress m_dmi_entry_point;
    size_t m_dmi_entry_point_length;
};

class SMBIOSExposedTable : public BIOSSysFSComponent {
public:
    static NonnullRefPtr<SMBIOSExposedTable> create(PhysicalAddress, size_t blob_size);
    virtual size_t size() const override { return m_smbios_structure_table_length; }

private:
    SMBIOSExposedTable(PhysicalAddress dmi_entry_point, size_t blob_size);
    virtual OwnPtr<KBuffer> try_to_generate_buffer() const override;

    PhysicalAddress m_smbios_structure_table;
    size_t m_smbios_structure_table_length;
};

class BIOSSysFSDirectory : public SysFSDirectory {
public:
    static void initialize();

    void create_components();

private:
    OwnPtr<KBuffer> dmi_entry_point() const;
    OwnPtr<KBuffer> smbios_structure_table() const;
    size_t dmi_entry_point_length() const;
    size_t smbios_structure_table_length() const;

    BIOSSysFSDirectory();

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
