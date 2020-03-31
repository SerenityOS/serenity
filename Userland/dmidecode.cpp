/*
 * Copyright (c) 2020, Liav A. <liavalb@hotmail.co.il>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <AK/Assertions.h>
#include <AK/LogStream.h>
#include <AK/Types.h>
#include <LibHardware/SMBIOS/ParserUtility.h>
#include <fcntl.h>
#include <stdio.h>
#include <string.h>

#define DMI_DATA_RAW_BLOB "/proc/smbios_data"
#define DMI_ENTRY_RAW_BLOB "/proc/smbios_entry"
typedef void* ByteStream;

class TitleLogStream final : public LogStream {
public:
    TitleLogStream() {}

    virtual ~TitleLogStream() override
    {
        char newline = '\n';
        write(&newline, 1);
    }

    virtual void write(const char* characters, int length) const override
    {
        for (int i = 0; i < length; i++)
            putc(characters[i], stdout);
    }
};

class TabLogStream final : public LogStream {
public:
    TabLogStream() {}

    virtual ~TabLogStream() override
    {
        if (m_put_newline) {
            char newline = '\n';
            write(&newline, 1);
        }
    }

    void disable_new_line()
    {
        m_put_newline = false;
    }

    virtual void write(const char* characters, int length) const override
    {
        for (int i = 0; i < length; i++)
            putc(characters[i], stdout);
    }
    bool m_put_newline { true };
};

inline const TabLogStream& operator<<(const TabLogStream& original, const TabLogStream&)
{
    const_cast<TabLogStream&>(original).disable_new_line();
    return original;
}

TitleLogStream title()
{
    TitleLogStream stream;
    return stream;
}

TabLogStream tab()
{
    TabLogStream stream;
    stream << "\t";
    return stream;
}

void help()
{
    printf("Usage: dmidecode [options] [source] [table_type|table_handle] [string_number]\n");
    printf("\nStandard Options:\n");
    printf("-h or --help\tShow this help and exit\n");
    printf("-l or --list\tList all available SMBIOS tables and exit\n");
    printf("-v or --verbose\tPrint verbose output\n");
    printf("-t or --specific-type\tPrint a specific table by type, depending on [table_type] input.\n");
    printf("-H or --specific-handle\tPrint a specific table by handle, depending on [table_handle] input.\n");
    printf("-S or --specific-string\tPrint a specific string, depending on [table_type|table_handle] and [string_number] input.\n");
    printf("-d or --from-dump\tUse a source as binary dump to decode.\n");
}

static u8 flags;

enum Flags {
    List = (1 << 0),
    Verbose = (1 << 1),
    SpecificTableByType = (1 << 2),
    SpecificTableByHandle = (1 << 3),
    SpecificString = (1 << 4),
    FromSource = (1 << 5)
};

bool is_verbose()
{
    return flags & Flags::Verbose;
}

bool is_showing_specific_table()
{
    return flags & (Flags::SpecificTableByType | Flags::SpecificTableByHandle);
}

bool is_showing_specific_string()
{
    return flags & Flags::SpecificString;
}

bool is_showing_list()
{
    return flags & Flags::List;
}

bool is_flags_invalid()
{
    return flags & (Flags::List | (Flags::SpecificTableByType | Flags::SpecificTableByHandle) | Flags::SpecificString);
}

int try_to_open_file(const char* filename)
{
    int fd = open(filename, O_RDONLY);
    if (fd < 0) {
        fprintf(stderr, "Failed to open: %s", filename);
    }
    return fd;
}

size_t smbios_data_payload_size;

void parse_32bit_entry(SMBIOS::EntryPoint32bit& entry)
{
    printf("SMBIOS version %u.%u\n", entry.major_version, entry.minor_version);
    printf("Table @ %p\n", (void*)entry.legacy_structure.smbios_table_ptr);
    smbios_data_payload_size = entry.legacy_structure.smbios_table_length;
}

void parse_64bit_entry(SMBIOS::EntryPoint64bit& entry)
{
    printf("SMBIOS version %u.%u, 64 bit entry\n", entry.major_version, entry.minor_version);
    printf("Table @ %p\n", (void*)entry.table_ptr);
    smbios_data_payload_size = entry.table_maximum_size;
}

void parse_table_header(const SMBIOS::TableHeader& header)
{
    printf("Handle 0x%x, DMI type %u", header.handle, header.type);
    if (!is_verbose()) {
        printf("\n");
    } else {
        printf(", %u bytes, %lu bytes (strings included)\n", header.length, SMBIOS::Parsing::calculate_full_table_size(header));
    }
}

void parse_table_type0(const SMBIOS::BIOSInfo& table)
{
    ASSERT(table.h.type == (u8)SMBIOS::TableType::BIOSInfo);

    title() << "BIOS Information";

    auto bios_vendor_string = SMBIOS::Parsing::try_to_acquire_smbios_string((const SMBIOS::TableHeader&)table, table.bios_vendor_str_number);
    tab() << "Vendor: " << (bios_vendor_string.has_value() ? bios_vendor_string.value() : "Unknown");
    auto bios_version_string = SMBIOS::Parsing::try_to_acquire_smbios_string((const SMBIOS::TableHeader&)table, table.bios_version_str_number);
    tab() << "Version: " << (bios_version_string.has_value() ? bios_version_string.value() : "Unknown");
    auto bios_release_date_string = SMBIOS::Parsing::try_to_acquire_smbios_string((const SMBIOS::TableHeader&)table, table.bios_release_date_str_number);
    tab() << "Release Date: " << (bios_release_date_string.has_value() ? bios_release_date_string.value() : "Unknown");
    tab() << "Address: " << PhysicalAddress(table.bios_segment << 4);
    tab() << "BIOS ROM Size: " << ((table.bios_rom_size + 1) * 64 * KB) << " bytes";
    tab() << "Characteristics:";
    if (table.bios_characteristics & (u16)SMBIOS::BIOSCharacteristics::NotSupported) {
        tab() << tab() << "Not supported";
    } else {
        if (table.bios_characteristics & (u16)SMBIOS::BIOSCharacteristics::ISA_support) {
            tab() << tab() << "ISA is supported";
        } else {
            if (is_verbose())
                tab() << tab() << "ISA is not supported";
        }
        if (table.bios_characteristics & (u16)SMBIOS::BIOSCharacteristics::MCA_support) {
            tab() << tab() << "MCA is supported";
        } else {
            if (is_verbose())
                tab() << tab() << "MCA is not supported";
        }
        if (table.bios_characteristics & (u16)SMBIOS::BIOSCharacteristics::EISA_support) {
            tab() << tab() << "EISA is supported";
        } else {
            if (is_verbose())
                tab() << tab() << "EISA is not supported";
        }
        if (table.bios_characteristics & (u16)SMBIOS::BIOSCharacteristics::PCI_support) {
            tab() << tab() << "PCI is supported";
        } else {
            if (is_verbose())
                tab() << tab() << "PCI is not supported";
        }
        if (table.bios_characteristics & (u16)SMBIOS::BIOSCharacteristics::PCMCIA_support) {
            tab() << tab() << "PC card (PCMCIA) is supported";
        } else {
            if (is_verbose())
                tab() << tab() << "PC card (PCMCIA) is not supported";
        }

        if (table.bios_characteristics & (u16)SMBIOS::BIOSCharacteristics::PnP_support) {
            tab() << tab() << "Plug & Play is supported";
        } else {
            if (is_verbose())
                tab() << tab() << "Plug & Play is not supported";
        }
        if (table.bios_characteristics & (u16)SMBIOS::BIOSCharacteristics::APM_support) {
            tab() << tab() << "APM is supported";
        } else {
            if (is_verbose())
                tab() << tab() << "APM is not supported";
        }
        if (table.bios_characteristics & (u16)SMBIOS::BIOSCharacteristics::UpgradeableBIOS) {
            tab() << tab() << "BIOS is upgradeable";
        } else {
            if (is_verbose())
                tab() << tab() << "BIOS is not upgradeable";
        }
        if (table.bios_characteristics & (u16)SMBIOS::BIOSCharacteristics::Shadowing_BIOS) {
            tab() << tab() << "BIOS shadowing is allowed";
        } else {
            if (is_verbose())
                tab() << tab() << "BIOS shadowing is not allowed";
        }
        if (table.bios_characteristics & (u16)SMBIOS::BIOSCharacteristics::VL_VESA_support) {
            tab() << tab() << "VL-VESA is supported";
        } else {
            if (is_verbose())
                tab() << tab() << "VL-VESA is not supported";
        }
        if (table.bios_characteristics & (u16)SMBIOS::BIOSCharacteristics::ESCD_support) {
            tab() << tab() << "ESCD is supported";
        } else {
            if (is_verbose())
                tab() << tab() << "ESCD is not supported";
        }
        if (table.bios_characteristics & (u32)SMBIOS::BIOSCharacteristics::CD_boot_support) {
            tab() << tab() << "Boot from CD is allowed";
        } else {
            if (is_verbose())
                tab() << tab() << "Boot from CD is not allowed";
        }
        if (table.bios_characteristics & (u32)SMBIOS::BIOSCharacteristics::select_boot_support) {
            tab() << tab() << "Selectable boot is supported";
        } else {
            if (is_verbose())
                tab() << tab() << "Selectable boot is supported";
        }
        if (table.bios_characteristics & (u32)SMBIOS::BIOSCharacteristics::BIOS_ROM_socketed) {
            tab() << tab() << "BIOS ROM is socketed";
        } else {
            if (is_verbose())
                tab() << tab() << "BIOS ROM is not socketed";
        }
        if (table.bios_characteristics & (u32)SMBIOS::BIOSCharacteristics::PCMCIA_boot_support) {
            tab() << tab() << "Boot from PC card (PCMCIA) is supported";
        } else {
            if (is_verbose())
                tab() << tab() << "Boot from PC card (PCMCIA) is not supported";
        }
        if (table.bios_characteristics & (u32)SMBIOS::BIOSCharacteristics::EDD_spec_support) {
            tab() << tab() << "EDD is supported";
        } else {
            if (is_verbose())
                tab() << tab() << "EDD is not supported";
        }
        if (table.bios_characteristics & (u32)SMBIOS::BIOSCharacteristics::floppy_nec98_1200k_support) {
            tab() << tab() << "Japanese floppy for NEC 9800 1.2 MB (3.5”, 1K bytes/sector, 360 RPM) is supported (int 13h)";
        } else {
            if (is_verbose())
                tab() << tab() << "Japanese floppy for NEC 9800 1.2 MB (3.5”, 1K bytes/sector, 360 RPM) is not supported (int 13h)";
        }
        if (table.bios_characteristics & (u32)SMBIOS::BIOSCharacteristics::floppy_toshiba_1200k_support) {
            tab() << tab() << "Japanese floppy for Toshiba 1.2 MB (3.5”, 360 RPM) is supported (int 13h)";
        } else {
            if (is_verbose())
                tab() << tab() << "Japanese floppy for Toshiba 1.2 MB (3.5”, 360 RPM) is not supported (int 13h)";
        }
        if (table.bios_characteristics & (u32)SMBIOS::BIOSCharacteristics::floppy_360k_support) {
            tab() << tab() << "5.25” / 360 KB floppy services are supported (int 13h)";
        } else {
            if (is_verbose())
                tab() << tab() << "5.25” / 360 KB floppy services are not supported (int 13h)";
        }
        if (table.bios_characteristics & (u32)SMBIOS::BIOSCharacteristics::floppy_1200k_services_support) {
            tab() << tab() << "5.25” /1.2 MB floppy services are supported (int 13h)";
        } else {
            if (is_verbose())
                tab() << tab() << "5.25” /1.2 MB floppy services are not supported (int 13h)";
        }
        if (table.bios_characteristics & (u32)SMBIOS::BIOSCharacteristics::floppy_720k_services_support) {
            tab() << tab() << "3.5” / 720 KB floppy services are supported (int 13h)";
        } else {
            if (is_verbose())
                tab() << tab() << "3.5” / 720 KB floppy services are not supported (int 13h)";
        }
        if (table.bios_characteristics & (u32)SMBIOS::BIOSCharacteristics::floppy_2880k_services_support) {
            tab() << tab() << "3.5” / 2.88 MB floppy services are supported (int 13h)";
        } else {
            if (is_verbose())
                tab() << tab() << "3.5” / 2.88 MB floppy services are supported (int 13h)";
        }
        if (table.bios_characteristics & (u32)SMBIOS::BIOSCharacteristics::int5_print_screen_support) {
            tab() << tab() << "Print screen service is supported (int 5h)";
        } else {
            if (is_verbose())
                tab() << tab() << "Print screen service is not supported (int 5h)";
        }
        if (table.bios_characteristics & (u32)SMBIOS::BIOSCharacteristics::int9_8042_keyboard_support) {
            tab() << tab() << "8042 keyboard services are supported (int 9h)";
        } else {
            if (is_verbose())
                tab() << tab() << "8042 keyboard services are not supported (int 9h)";
        }
        if (table.bios_characteristics & (u32)SMBIOS::BIOSCharacteristics::int14_serial_support) {
            tab() << tab() << "Serial services are supported (int 14h)";
        } else {
            if (is_verbose())
                tab() << tab() << "Serial services are not supported (int 14h)";
        }
        if (table.bios_characteristics & (u32)SMBIOS::BIOSCharacteristics::int17_printer_support) {
            tab() << tab() << "Printer services are supported (int 17h)";
        } else {
            if (is_verbose())
                tab() << tab() << "Printer services are not supported (int 17h)";
        }
        if (table.bios_characteristics & (u32)SMBIOS::BIOSCharacteristics::int10_video_support) {
            tab() << tab() << "CGA/Mono Video Services are supported (int 10h)";
        } else {
            if (is_verbose())
                tab() << tab() << "CGA/Mono Video Services are supported (int 10h)";
        }
        if (table.bios_characteristics & (u32)SMBIOS::BIOSCharacteristics::nec_pc98) {
            tab() << tab() << "NEC PC-98";
        } else {
            if (is_verbose())
                tab() << tab() << "Not a NEC PC-98";
        }

        if (table.ext_bios_characteristics[0] & (u32)SMBIOS::ExtendedBIOSCharacteristics::ACPI_support) {
            tab() << tab() << "ACPI is supported";
        } else {
            if (is_verbose())
                tab() << tab() << "ACPI is not supported";
        }
        if (table.ext_bios_characteristics[0] & (u32)SMBIOS::ExtendedBIOSCharacteristics::USB_Legacy_support) {
            tab() << tab() << "USB Legacy is supported";
        } else {
            if (is_verbose())
                tab() << tab() << "USB Legacy is not supported";
        }
        if (table.ext_bios_characteristics[0] & (u32)SMBIOS::ExtendedBIOSCharacteristics::AGP_support) {
            tab() << tab() << "AGP is supported";
        } else {
            if (is_verbose())
                tab() << tab() << "AGP is not supported";
        }
        if (table.ext_bios_characteristics[0] & (u32)SMBIOS::ExtendedBIOSCharacteristics::I2O_boot_support) {
            tab() << tab() << "I2O boot is supported";
        } else {
            if (is_verbose())
                tab() << tab() << "I2O boot is not supported";
        }
        if (table.h.length >= 0x13) {
            if (table.ext_bios_characteristics[0] & (u32)SMBIOS::ExtendedBIOSCharacteristics::LS120_SuperDisk_boot_support) {
                tab() << tab() << "LS-120 SuperDisk boot is supported";
            } else {
                if (is_verbose())
                    tab() << tab() << "LS-120 SuperDisk boot is not supported";
            }
            if (table.ext_bios_characteristics[0] & (u32)SMBIOS::ExtendedBIOSCharacteristics::ATAPI_ZIP_drive_boot_support) {
                tab() << tab() << "ATAPI ZIP drive boot is supported";
            } else {
                if (is_verbose())
                    tab() << tab() << "ATAPI ZIP drive boot is not supported";
            }
            if (table.ext_bios_characteristics[0] & (u32)SMBIOS::ExtendedBIOSCharacteristics::boot_1394_support) {
                tab() << tab() << "1394 boot is supported";
            } else {
                if (is_verbose())
                    tab() << tab() << "1394 boot is not supported";
            }
            if (table.ext_bios_characteristics[0] & (u32)SMBIOS::ExtendedBIOSCharacteristics::smary_battery_support) {
                tab() << tab() << "Smart battery is supported";
            } else {
                if (is_verbose())
                    tab() << tab() << "Smart battery is not supported";
            }
        }

        if (table.h.length >= 0x14) {
            if (table.ext_bios_characteristics[1] & (u32)SMBIOS::ExtendedBIOSCharacteristics2::BIOS_Boot_Specification_support) {
                tab() << tab() << "BIOS Boot Specification is supported";
            } else {
                if (is_verbose())
                    tab() << tab() << "BIOS Boot Specification is not supported";
            }
            if (table.ext_bios_characteristics[1] & (u32)SMBIOS::ExtendedBIOSCharacteristics2::Func_key_initiated_network_service_boot_support) {
                tab() << tab() << "Function key-initiated network service boot is supported";
            } else {
                if (is_verbose())
                    tab() << tab() << "Function key-initiated network service boot is not supported";
            }
            if (table.ext_bios_characteristics[1] & (u32)SMBIOS::ExtendedBIOSCharacteristics2::Targeted_content_distribution) {
                tab() << tab() << "Enable targeted content distribution";
            } else {
                if (is_verbose())
                    tab() << tab() << "Disable targeted content distribution";
            }
            if (table.ext_bios_characteristics[1] & (u32)SMBIOS::ExtendedBIOSCharacteristics2::UEFI_support) {
                tab() << tab() << "UEFI Specification is supported";
            } else {
                if (is_verbose())
                    tab() << tab() << "UEFI Specification is not supported";
            }
            if (table.ext_bios_characteristics[1] & (u32)SMBIOS::ExtendedBIOSCharacteristics2::SMBIOS_describes_Virtual_Machine) {
                tab() << tab() << "SMBIOS table describes a virtual machine";
            } else {
                if (is_verbose())
                    tab() << tab() << "SMBIOS table does not describe a virtual machine";
            }
        }
    }
    if (table.h.length >= 0x15)
        tab() << "BIOS Revision: " << table.bios_major_release << "." << table.bios_minor_release;
    if (table.h.length >= 0x17) {
        if (table.embedded_controller_firmware_major_release == 0xFF && table.embedded_controller_firmware_minor_release == 0xFF)
            tab() << "Embedded Controller is not upgradeable";
        else
            tab() << "Embedded Controller Revision: " << table.embedded_controller_firmware_major_release << "." << table.embedded_controller_firmware_minor_release;
    }
    printf("\n");
}

void parse_table_type1(const SMBIOS::SysInfo& table)
{
    ASSERT(table.h.type == (u8)SMBIOS::TableType::SysInfo);

    title() << "System Information";

    auto manufacturer_string = SMBIOS::Parsing::try_to_acquire_smbios_string((const SMBIOS::TableHeader&)table, table.manufacturer_str_number);
    tab() << "Manufacturer: " << (manufacturer_string.has_value() ? manufacturer_string.value() : "Unknown");
    auto product_name_string = SMBIOS::Parsing::try_to_acquire_smbios_string((const SMBIOS::TableHeader&)table, table.product_name_str_number);
    tab() << "Product Name: " << (product_name_string.has_value() ? product_name_string.value() : "Unknown");
    auto version_string = SMBIOS::Parsing::try_to_acquire_smbios_string((const SMBIOS::TableHeader&)table, table.version_str_number);
    tab() << "Version: " << (version_string.has_value() ? version_string.value() : "Unknown");
    auto serial_number_string = SMBIOS::Parsing::try_to_acquire_smbios_string((const SMBIOS::TableHeader&)table, table.serial_number_str_number);
    tab() << "Serial Number: " << (serial_number_string.has_value() ? serial_number_string.value() : "Unknown");
    tab() << "UUID: " << SMBIOS::Parsing::create_uuid(table.uuid[0], table.uuid[1]);
    String wake_type;
    switch (table.wake_up_type) {
    case (u8)SMBIOS::WakeUpType::Other:
        wake_type = "Other";
        break;
    case (u8)SMBIOS::WakeUpType::Unknown:
        wake_type = "Unknown";
        break;
    case (u8)SMBIOS::WakeUpType::APM_TIMER:
        wake_type = "APM Timer";
        break;
    case (u8)SMBIOS::WakeUpType::MODEM_RING:
        wake_type = "Modem Ring";
        break;
    case (u8)SMBIOS::WakeUpType::LAN_REMOTE:
        wake_type = "LAN Remote";
        break;
    case (u8)SMBIOS::WakeUpType::POWER_SWITCH:
        wake_type = "Power Switch";
        break;
    case (u8)SMBIOS::WakeUpType::PCI_PME:
        wake_type = "PCI PME#";
        break;
    case (u8)SMBIOS::WakeUpType::AC_RESTORE:
        wake_type = "AC Power Restored";
        break;
    default:
        wake_type = "Unknown";
        break;
    }
    tab() << "Wake-up Type: " << wake_type;
    auto sku_number_string = SMBIOS::Parsing::try_to_acquire_smbios_string((const SMBIOS::TableHeader&)table, table.sku_str_number);
    tab() << "SKU Number: " << (sku_number_string.has_value() ? sku_number_string.value() : "Unknown");
    auto family_string = SMBIOS::Parsing::try_to_acquire_smbios_string((const SMBIOS::TableHeader&)table, table.family_str_number);
    tab() << "Family: " << (family_string.has_value() ? family_string.value() : "Unknown");
    printf("\n");
}

void parse_table_type2(const SMBIOS::ModuleInfo& table)
{
    ASSERT(table.h.type == (u8)SMBIOS::TableType::ModuleInfo);
    ASSERT(table.h.length >= 8); // According to the SMBIOS specification 3.3.0

    title() << "Module Information";

    auto manufacturer_string = SMBIOS::Parsing::try_to_acquire_smbios_string((const SMBIOS::TableHeader&)table, table.manufacturer_str_number);
    tab() << "Manufacturer: " << (manufacturer_string.has_value() ? manufacturer_string.value() : "Unknown");
    auto product_string = SMBIOS::Parsing::try_to_acquire_smbios_string((const SMBIOS::TableHeader&)table, table.product_name_str_number);
    tab() << "Product Name: " << (product_string.has_value() ? product_string.value() : "Unknown");
    auto version_string = SMBIOS::Parsing::try_to_acquire_smbios_string((const SMBIOS::TableHeader&)table, table.version_str_number);
    tab() << "Version: " << (version_string.has_value() ? version_string.value() : "Unknown");
    auto serial_number_string = SMBIOS::Parsing::try_to_acquire_smbios_string((const SMBIOS::TableHeader&)table, table.serial_number_str_number);
    tab() << "Serial Number: " << (serial_number_string.has_value() ? serial_number_string.value() : "Unknown");
    auto asset_tag_string = SMBIOS::Parsing::try_to_acquire_smbios_string((const SMBIOS::TableHeader&)table, table.asset_tag_str_number);
    tab() << "Asset Tag: " << (asset_tag_string.has_value() ? asset_tag_string.value() : "Unknown");

    if (table.h.length >= 9) {
        if (table.feature_flags & (u8)SMBIOS::ModuleFeatures::HostingBoard) {
            tab() << tab() << "Board is a hosting board";
        } else {
            if (is_verbose())
                tab() << tab() << "Board is not a hosting board";
        }
        if (table.feature_flags & (u8)SMBIOS::ModuleFeatures::RequiresDaughterBoard) {
            tab() << tab() << "Board requires at least one daughter board or auxiliary card";
        } else {
            if (is_verbose())
                tab() << tab() << "Board does not require a daughter board or auxiliary card";
        }
        if (table.feature_flags & (u8)SMBIOS::ModuleFeatures::Removable) {
            tab() << tab() << "Board is removable";
        } else {
            if (is_verbose())
                tab() << tab() << "Board is not removable";
        }
        if (table.feature_flags & (u8)SMBIOS::ModuleFeatures::Replaceable) {
            tab() << tab() << "Board is replaceable";
        } else {
            if (is_verbose())
                tab() << tab() << "Board is not replaceable";
        }
        if (table.feature_flags & (u8)SMBIOS::ModuleFeatures::HotSwappable) {
            tab() << tab() << "Board is hot swappable";
        } else {
            if (is_verbose())
                tab() << tab() << "Board is not hot swappable";
        }
    } else {
        printf("\n");
        return;
    }

    if (table.h.length >= 10) {
        auto location_in_chassis_string = SMBIOS::Parsing::try_to_acquire_smbios_string((const SMBIOS::TableHeader&)table, table.chassis_location);
        tab() << "Serial Number: " << (location_in_chassis_string.has_value() ? location_in_chassis_string.value() : "Unknown");
    } else {
        printf("\n");
        return;
    }

    if (table.h.length >= 11) {
        tab() << "Chassis Handle: " << table.chassis_handle;
    } else {
        printf("\n");
        return;
    }

    if (table.h.length >= 12) {
        String board_type;
        switch (table.board_type) {
        case (u16)SMBIOS::BoardType::Unknown:
            board_type = "Unknown";
            break;
        case (u16)SMBIOS::BoardType::Other:
            board_type = "Other";
            break;
        case (u16)SMBIOS::BoardType::Server_Blade:
            board_type = "Server Blade";
            break;
        case (u16)SMBIOS::BoardType::Connectivity_Switch:
            board_type = "Connectivity Switch";
            break;
        case (u16)SMBIOS::BoardType::System_Management_Module:
            board_type = "System Management Module";
            break;
        case (u16)SMBIOS::BoardType::Processor_Module:
            board_type = "Processor Module";
            break;
        case (u16)SMBIOS::BoardType::IO_Module:
            board_type = "I/O Module";
            break;
        case (u16)SMBIOS::BoardType::Memory_Module:
            board_type = "Memory Module";
            break;
        case (u16)SMBIOS::BoardType::Daughter_Board:
            board_type = "Daughter Board";
            break;
        case (u16)SMBIOS::BoardType::Motherboard:
            board_type = "Motherboard";
            break;
        case (u16)SMBIOS::BoardType::Processor_Memory_Module:
            board_type = "Processor Memory Module";
            break;
        case (u16)SMBIOS::BoardType::Processor_IO_Module:
            board_type = "Processor I/O Module";
            break;
        case (u16)SMBIOS::BoardType::Interconnect_Board:
            board_type = "Interconnect Board";
            break;
        default:
            board_type = "Unknown";
            break;
        }
    } else {
        printf("\n");
        return;
    }

    if (table.h.length >= 13) {
        tab() << "Contained Object Handles Count: " << table.contained_object_handles_count;
        if (table.contained_object_handles_count > 0 && is_verbose()) {
            tab() << "Contained Object Handles: ";
            for (size_t index = 0; index < table.contained_object_handles_count; index++)
                tab() << tab() << (index + 1) << ": " << table.contained_object_handles[index];
        }
    }

    printf("\n");
}

bool parse_data(ByteStream data)
{
    size_t remaining_table_length = smbios_data_payload_size;
    ByteStream current_table = data;
    while (remaining_table_length > 0) {
        auto& table = *(SMBIOS::TableHeader*)(current_table);
        size_t table_size = SMBIOS::Parsing::calculate_full_table_size(table);
        parse_table_header(table);
        switch (table.type) {
        case (u8)SMBIOS::TableType::BIOSInfo:
            parse_table_type0((SMBIOS::BIOSInfo&)table);
            break;
        case (u8)SMBIOS::TableType::SysInfo:
            parse_table_type1((SMBIOS::SysInfo&)table);
            break;
        case (u8)SMBIOS::TableType::ModuleInfo:
            parse_table_type2((SMBIOS::ModuleInfo&)table);
            break;
        default:
            printf("\n");
            break;
        }
        current_table = (void*)((u8*)current_table + table_size);
        remaining_table_length -= table_size;
    }
    return true;
}

void parse_entry(const char* entry)
{
    if (strncmp("_SM_", entry, 4) == 0) {
        parse_32bit_entry(*(SMBIOS::EntryPoint32bit*)const_cast<char*>(entry));
        return;
    }
    if (strncmp("_SM3_", entry, 5) == 0) {
        parse_64bit_entry(*(SMBIOS::EntryPoint64bit*)const_cast<char*>(entry));
        return;
    }
    ASSERT_NOT_REACHED();
}

int main(int argc, char** argv)
{
    if (argc == 1) {
        flags |= Flags::List;
    } else if (argc >= 2) {
        if ((!strcmp("-h", argv[1])) || (!strcmp("--help", argv[1]))) {
            help();
            return 1;
        }
        for (int index = 1; index < argc; index++) {
            if ((!strcmp("-l", argv[index])) || (!strcmp("--list", argv[index]))) {
                if (is_showing_list()) {
                    fprintf(stderr, "error: repetitive argument.\n");
                    help();
                    return 1;
                }
                flags |= Flags::List;
                continue;
            }
            if ((!strcmp("-v", argv[index])) || (!strcmp("--verbose", argv[index]))) {
                if (is_verbose()) {
                    fprintf(stderr, "error: repetitive argument.\n");
                    help();
                    return 1;
                }
                flags |= Flags::Verbose;
                continue;
            }
            if ((!strcmp("-t", argv[index])) || (!strcmp("--specific-type", argv[index]))) {
                if (is_showing_specific_table()) {
                    fprintf(stderr, "error: repetitive argument.\n");
                    help();
                    return 1;
                }
                flags |= Flags::SpecificTableByType;
                continue;
            }
            if ((!strcmp("-H", argv[index])) || (!strcmp("--specific-handle", argv[index]))) {
                if (is_showing_specific_table()) {
                    fprintf(stderr, "error: repetitive argument.\n");
                    help();
                    return 1;
                }
                flags |= Flags::SpecificTableByHandle;
                continue;
            }
            if ((!strcmp("-S", argv[index])) || (!strcmp("--specific-string", argv[index]))) {
                if (is_showing_specific_string()) {
                    fprintf(stderr, "error: repetitive argument.\n");
                    help();
                    return 1;
                }
                flags |= Flags::SpecificString;
                continue;
            }
        }
        if (is_flags_invalid()) {
            fprintf(stderr, "error: conflicting arguments specified.\n");
            help();
            return 1;
        }
    }

    int data_fd = try_to_open_file(DMI_DATA_RAW_BLOB);
    int entry_fd = try_to_open_file(DMI_ENTRY_RAW_BLOB);

    if (entry_fd < 0 || data_fd < 0)
        return 1;

    auto* entry = kmalloc(sizeof(SMBIOS::EntryPoint32bit) + sizeof(SMBIOS::EntryPoint64bit));
    if (lseek(entry_fd, SEEK_SET, 0) < 0)
        return 1;
    if (read(entry_fd, entry, (sizeof(SMBIOS::EntryPoint32bit) + sizeof(SMBIOS::EntryPoint64bit))) < 0)
        return 1;
    parse_entry((const char*)entry);
    kfree(entry);

    printf("\n");

    auto* data = kmalloc(smbios_data_payload_size);
    if (lseek(data_fd, SEEK_SET, 0) < 0)
        return 1;
    if (read(data_fd, data, smbios_data_payload_size) < 0)
        return 1;
    if (!parse_data(data))
        return false;
    return 0;
}
