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

void parse_table_type3(const SMBIOS::SysEnclosure& table)
{
    ASSERT(table.h.type == (u8)SMBIOS::TableType::SysEnclosure);
    ASSERT(table.h.length >= 9);

    title() << "System Enclosure";

    auto manufacturer_string = SMBIOS::Parsing::try_to_acquire_smbios_string((const SMBIOS::TableHeader&)table, table.manufacturer_str_number);
    tab() << "Manufacturer: " << (manufacturer_string.has_value() ? manufacturer_string.value() : "Unknown");
    String system_enclosure_type;
    switch (table.type & 0x7f) {
    case (u16)SMBIOS::SysEnclosureType::Unknown:
        system_enclosure_type = "Unknown";
        break;
    case (u16)SMBIOS::SysEnclosureType::Other:
        system_enclosure_type = "Other";
        break;
    case (u16)SMBIOS::SysEnclosureType::Desktop:
        system_enclosure_type = "Desktop";
        break;
    case (u16)SMBIOS::SysEnclosureType::Low_Profile_Desktop:
        system_enclosure_type = "Low Profile Desktop";
        break;
    case (u16)SMBIOS::SysEnclosureType::Pizza_Box:
        system_enclosure_type = "Pizza Box";
        break;
    case (u16)SMBIOS::SysEnclosureType::Mini_Tower:
        system_enclosure_type = "Mini Tower";
        break;
    case (u16)SMBIOS::SysEnclosureType::Tower:
        system_enclosure_type = "Tower";
        break;
    case (u16)SMBIOS::SysEnclosureType::Portable:
        system_enclosure_type = "Portable";
        break;
    case (u16)SMBIOS::SysEnclosureType::Laptop:
        system_enclosure_type = "Laptop";
        break;
    case (u16)SMBIOS::SysEnclosureType::Notebook:
        system_enclosure_type = "Notebook";
        break;
    case (u16)SMBIOS::SysEnclosureType::Hand_Held:
        system_enclosure_type = "Hand Held";
        break;
    case (u16)SMBIOS::SysEnclosureType::Docking_Station:
        system_enclosure_type = "Docking Station";
        break;
    case (u16)SMBIOS::SysEnclosureType::AIO:
        system_enclosure_type = "All in One";
        break;
    case (u16)SMBIOS::SysEnclosureType::Sub_Notebook:
        system_enclosure_type = "Sub Notebook";
        break;
    case (u16)SMBIOS::SysEnclosureType::Space_Saving:
        system_enclosure_type = "Space-saving";
        break;
    case (u16)SMBIOS::SysEnclosureType::Lunch_Box:
        system_enclosure_type = "Lunch Box";
        break;
    case (u16)SMBIOS::SysEnclosureType::Main_Server_Chassis:
        system_enclosure_type = "Main Server Chassis";
        break;
    case (u16)SMBIOS::SysEnclosureType::Expansion_Chassis:
        system_enclosure_type = "Expansion Chassis";
        break;
    case (u16)SMBIOS::SysEnclosureType::SubChassis:
        system_enclosure_type = "SubChassis";
        break;
    case (u16)SMBIOS::SysEnclosureType::Bus_Expansion_Chassis:
        system_enclosure_type = "Bus Expansion Chassis";
        break;
    case (u16)SMBIOS::SysEnclosureType::Peripheral_Chassis:
        system_enclosure_type = "Peripheral Chassis";
        break;
    case (u16)SMBIOS::SysEnclosureType::RAID_Chassis:
        system_enclosure_type = "RAID Chassis";
        break;
    case (u16)SMBIOS::SysEnclosureType::Rack_Mount_Chassis:
        system_enclosure_type = "Rack Mount Chassis";
        break;
    case (u16)SMBIOS::SysEnclosureType::Sealed_case_PC:
        system_enclosure_type = "Sealed-case PC";
        break;
    case (u16)SMBIOS::SysEnclosureType::Multi_System_Chasis:
        system_enclosure_type = "Multi-system chassis";
        break;
    case (u16)SMBIOS::SysEnclosureType::Compact_PCI:
        system_enclosure_type = "Compact PCI";
        break;
    case (u16)SMBIOS::SysEnclosureType::Advanced_TCA:
        system_enclosure_type = "Advanced TCA";
        break;
    case (u16)SMBIOS::SysEnclosureType::Blade:
        system_enclosure_type = "Blade";
        break;
    case (u16)SMBIOS::SysEnclosureType::Blade_Enclosure:
        system_enclosure_type = "Blade Enclosure";
        break;
    case (u16)SMBIOS::SysEnclosureType::Tablet:
        system_enclosure_type = "Tablet";
        break;
    case (u16)SMBIOS::SysEnclosureType::Convertible:
        system_enclosure_type = "Convertible";
        break;
    case (u16)SMBIOS::SysEnclosureType::Detachable:
        system_enclosure_type = "Detachable";
        break;
    case (u16)SMBIOS::SysEnclosureType::IoT_Gateway:
        system_enclosure_type = "IoT Gateway";
        break;
    case (u16)SMBIOS::SysEnclosureType::Embedded_PC:
        system_enclosure_type = "Embedded PC";
        break;
    case (u16)SMBIOS::SysEnclosureType::Mini_PC:
        system_enclosure_type = "Mini PC";
        break;
    case (u16)SMBIOS::SysEnclosureType::Stick_PC:
        system_enclosure_type = "Stick PC";
        break;
    default:
        system_enclosure_type = "Unknown";
        break;
    }
    tab() << "Type: " << system_enclosure_type;
    tab() << "Lock: " << ((table.type & 0x80) ? "Present" : "Not Present");
    auto version_string = SMBIOS::Parsing::try_to_acquire_smbios_string((const SMBIOS::TableHeader&)table, table.version_str_number);
    tab() << "Version: " << (version_string.has_value() ? version_string.value() : "Unknown");
    auto serial_number_string = SMBIOS::Parsing::try_to_acquire_smbios_string((const SMBIOS::TableHeader&)table, table.serial_number_str_number);
    tab() << "Serial Number: " << (serial_number_string.has_value() ? serial_number_string.value() : "Unknown");
    auto asset_tag_string = SMBIOS::Parsing::try_to_acquire_smbios_string((const SMBIOS::TableHeader&)table, table.asset_tag_str_number);
    tab() << "Asset Tag: " << (asset_tag_string.has_value() ? asset_tag_string.value() : "Unknown");

    String bootup_state;
    switch (table.boot_up_state) {
    case (u16)SMBIOS::SysEnclosureState::Unknown:
        bootup_state = "Unknown";
        break;
    case (u16)SMBIOS::SysEnclosureState::Other:
        bootup_state = "Other";
        break;
    case (u16)SMBIOS::SysEnclosureState::Safe:
        bootup_state = "Safe";
        break;
    case (u16)SMBIOS::SysEnclosureState::Warning:
        bootup_state = "Warning";
        break;
    case (u16)SMBIOS::SysEnclosureState::Critical:
        bootup_state = "Critical";
        break;
    case (u16)SMBIOS::SysEnclosureState::Non_Recoverable:
        bootup_state = "Non Recoverable";
        break;
    default:
        bootup_state = "Unknown";
        break;
    }
    tab() << "Boot-up State: " << bootup_state;

    if (table.h.length >= 10) {
        String power_supply_state;
        switch (table.thermal_state) {
        case (u16)SMBIOS::SysEnclosureState::Unknown:
            power_supply_state = "Unknown";
            break;
        case (u16)SMBIOS::SysEnclosureState::Other:
            power_supply_state = "Other";
            break;
        case (u16)SMBIOS::SysEnclosureState::Safe:
            power_supply_state = "Safe";
            break;
        case (u16)SMBIOS::SysEnclosureState::Warning:
            power_supply_state = "Warning";
            break;
        case (u16)SMBIOS::SysEnclosureState::Critical:
            power_supply_state = "Critical";
            break;
        case (u16)SMBIOS::SysEnclosureState::Non_Recoverable:
            power_supply_state = "Non Recoverable";
            break;
        default:
            power_supply_state = "Unknown";
            break;
        }
        tab() << "Power Supply State: " << power_supply_state;
    } else {
        printf("\n");
        return;
    }

    if (table.h.length >= 11) {
        String thermal_state;
        switch (table.thermal_state) {
        case (u16)SMBIOS::SysEnclosureState::Unknown:
            thermal_state = "Unknown";
            break;
        case (u16)SMBIOS::SysEnclosureState::Other:
            thermal_state = "Other";
            break;
        case (u16)SMBIOS::SysEnclosureState::Safe:
            thermal_state = "Safe";
            break;
        case (u16)SMBIOS::SysEnclosureState::Warning:
            thermal_state = "Warning";
            break;
        case (u16)SMBIOS::SysEnclosureState::Critical:
            thermal_state = "Critical";
            break;
        case (u16)SMBIOS::SysEnclosureState::Non_Recoverable:
            thermal_state = "Non Recoverable";
            break;
        default:
            thermal_state = "Unknown";
            break;
        }
        tab() << "Thermal State: " << thermal_state;
    } else {
        printf("\n");
        return;
    }

    if (table.h.length >= 12) {
        String security_status;
        switch (table.security_status) {
        case (u16)SMBIOS::SysEnclosureSecurityStatus::Unknown:
            security_status = "Unknown";
            break;
        case (u16)SMBIOS::SysEnclosureSecurityStatus::Other:
            security_status = "Other";
            break;
        case (u16)SMBIOS::SysEnclosureSecurityStatus::None:
            security_status = "None";
            break;
        case (u16)SMBIOS::SysEnclosureSecurityStatus::External_Interface_Locked_Out:
            security_status = "External Interface Locked Out";
            break;
        case (u16)SMBIOS::SysEnclosureSecurityStatus::External_Interface_Enabled:
            security_status = "External Interface Enabled";
            break;
        default:
            security_status = "Unknown";
            break;
        }
        tab() << "Security Status: " << security_status;
    } else {
        printf("\n");
        return;
    }
    if (table.h.length >= 16) {
        tab() << "OEM Information: 0x" << String::format("%x", table.vendor_specific_info);
    } else {
        printf("\n");
        return;
    }
    if (table.h.length >= 17) {
        tab() << "Height: " << table.height;
    } else {
        printf("\n");
        return;
    }

    if (table.h.length >= 18) {
        tab() << "Number of Power Cords: " << table.power_cords_number;
    } else {
        printf("\n");
        return;
    }
    if (table.h.length >= 19) {
        tab() << "Contained Elements: " << table.contained_element_count;
    } else {
        printf("\n");
        return;
    }

    if (is_verbose()) {
        if (table.h.length >= 20 && table.contained_element_count > 0) {
            tab() << "Contained Elements Record Length: " << table.contained_element_record_length;
            auto* contained_element = (SMBIOS::SysEnclosureContainedElement*)(const_cast<u8*>(table.contained_elements));

            for (size_t index = 0; index < table.contained_element_count; index++) {
                tab() << tab() << "Type: " << contained_element->type;
                tab() << tab() << "Minimum Contained Element Count: " << contained_element->min_contained_element_count;
                tab() << tab() << "Maximum Contained Element Count: " << contained_element->max_contained_element_count;
                tab() << tab() << "";
                contained_element = (SMBIOS::SysEnclosureContainedElement*)((u8*)contained_element + table.contained_element_record_length);
            }
        }
    }

    if (table.h.length >= 0x15 + (table.contained_element_count * table.contained_element_record_length)) {
        auto& extended_structure = (SMBIOS::ExtSysEnclosure&)*((u8*)&const_cast<SMBIOS::SysEnclosure&>(table) + (table.contained_element_count * table.contained_element_record_length));
        auto sku_number_string = SMBIOS::Parsing::try_to_acquire_smbios_string((const SMBIOS::TableHeader&)table, extended_structure.sku_str_number);
        tab() << "SKU Number: " << (sku_number_string.has_value() ? sku_number_string.value() : "Unknown");
    }
    printf("\n");
}

void parse_table_type4(const SMBIOS::ProcessorInfo& table)
{
    ASSERT(table.h.type == (u8)SMBIOS::TableType::ProcessorInfo);
    ASSERT(table.h.length >= 0x1A);

    title() << "Processor Information";

    auto socket_designation_string = SMBIOS::Parsing::try_to_acquire_smbios_string((const SMBIOS::TableHeader&)table, table.socket_designation_str_number);
    tab() << "Socket Designation: " << (socket_designation_string.has_value() ? socket_designation_string.value() : "Unknown");

    String processor_type;
    switch (table.processor_type) {
    case (u16)SMBIOS::ProcessorType::Unknown:
        processor_type = "Unknown";
        break;
    case (u16)SMBIOS::ProcessorType::Other:
        processor_type = "Other";
        break;
    case (u16)SMBIOS::ProcessorType::Video_Processor:
        processor_type = "Video Processor";
        break;
    case (u16)SMBIOS::ProcessorType::Math_Processor:
        processor_type = "Math Processor";
        break;
    case (u16)SMBIOS::ProcessorType::Central_Processor:
        processor_type = "Central Processor";
        break;
    case (u16)SMBIOS::ProcessorType::DSP_Processor:
        processor_type = "DSP Processor";
        break;
    default:
        processor_type = "Unknown";
        break;
    }
    tab() << "Processor Type: " << processor_type;

    String processor_family;
    switch (table.processor_family) {
    case (u16)SMBIOS::ProcessorFamily::Other:
        processor_family = "Other";
        break;
    case (u16)SMBIOS::ProcessorFamily::Unknown:
        processor_family = "Unknown";
        break;
    case (u16)SMBIOS::ProcessorFamily::Intel_8086:
        processor_family = "Intel 8086";
        break;
    case (u16)SMBIOS::ProcessorFamily::Intel_80826:
        processor_family = "Intel 80826";
        break;
    case (u16)SMBIOS::ProcessorFamily::Intel_386:
        processor_family = "Intel 386";
        break;
    case (u16)SMBIOS::ProcessorFamily::Intel_486:
        processor_family = "Intel 486";
        break;
    case (u16)SMBIOS::ProcessorFamily::Intel_8087:
        processor_family = "Intel 8087";
        break;
    case (u16)SMBIOS::ProcessorFamily::Intel_80287:
        processor_family = "Intel 80287";
        break;
    case (u16)SMBIOS::ProcessorFamily::Intel_80387:
        processor_family = "Intel 80387";
        break;
    case (u16)SMBIOS::ProcessorFamily::Intel_80487:
        processor_family = "Intel 80487";
        break;
    case (u16)SMBIOS::ProcessorFamily::Intel_Pentium:
        processor_family = "Intel Pentium";
        break;
    case (u16)SMBIOS::ProcessorFamily::Intel_Pentium_Pro:
        processor_family = "Intel Pentium Pro";
        break;
    case (u16)SMBIOS::ProcessorFamily::Intel_Pentium_2:
        processor_family = "Intel Pentium 2";
        break;
    case (u16)SMBIOS::ProcessorFamily::Intel_Pentium_MMX:
        processor_family = "Intel Pentium MMX";
        break;
    case (u16)SMBIOS::ProcessorFamily::Intel_Celeron:
        processor_family = "Intel Celeron";
        break;
    case (u16)SMBIOS::ProcessorFamily::Intel_Pentium_2_Xeon:
        processor_family = "Intel Pentium 2 Xeon";
        break;
    case (u16)SMBIOS::ProcessorFamily::Intel_Pentium_3:
        processor_family = "Intel Pentium 3";
        break;
    case (u16)SMBIOS::ProcessorFamily::M1_Family:
        processor_family = "M1 Family";
        break;
    case (u16)SMBIOS::ProcessorFamily::M2_Family:
        processor_family = "M2 Family";
        break;
    case (u16)SMBIOS::ProcessorFamily::Intel_Celeron_M:
        processor_family = "Intel Celeron M";
        break;
    case (u16)SMBIOS::ProcessorFamily::Intel_Pentium_4HT:
        processor_family = "Intel Pentium 4 HT";
        break;
    case (u16)SMBIOS::ProcessorFamily::AMD_Duron_Family:
        processor_family = "AMD Duron Family";
        break;
    case (u16)SMBIOS::ProcessorFamily::K5_Family:
        processor_family = "K5 Family";
        break;
    case (u16)SMBIOS::ProcessorFamily::K6_Family:
        processor_family = "K6 Family";
        break;
    case (u16)SMBIOS::ProcessorFamily::K6_2:
        processor_family = "K6-2";
        break;
    case (u16)SMBIOS::ProcessorFamily::K6_3:
        processor_family = "K6-3";
        break;
    case (u16)SMBIOS::ProcessorFamily::AMD_Athlon_Family:
        processor_family = "AMD Athlon Family";
        break;
    case (u16)SMBIOS::ProcessorFamily::AMD_29000_Family:
        processor_family = "AMD 29000 Family";
        break;
    case (u16)SMBIOS::ProcessorFamily::K6_2_Plus:
        processor_family = "K6-2 Plus";
        break;
    case (u16)SMBIOS::ProcessorFamily::PowerPC:
        processor_family = "PowerPC";
        break;
    case (u16)SMBIOS::ProcessorFamily::PowerPC_601:
        processor_family = "PowerPC 601";
        break;
    case (u16)SMBIOS::ProcessorFamily::PowerPC_603:
        processor_family = "PowerPC 603";
        break;
    case (u16)SMBIOS::ProcessorFamily::PowerPC_603_Plus:
        processor_family = "PowerPC 603 Plus";
        break;
    case (u16)SMBIOS::ProcessorFamily::PowerPC_604:
        processor_family = "PowerPC 604";
        break;
    case (u16)SMBIOS::ProcessorFamily::PowerPC_620:
        processor_family = "PowerPC 620";
        break;
    case (u16)SMBIOS::ProcessorFamily::PowerPC_x704:
        processor_family = "PowerPC x704";
        break;
    case (u16)SMBIOS::ProcessorFamily::PowerPC_750:
        processor_family = "PowerPC 750";
        break;
    case (u16)SMBIOS::ProcessorFamily::Intel_Core_Duo:
        processor_family = "Intel Core Duo";
        break;
    case (u16)SMBIOS::ProcessorFamily::Intel_Core_Duo_Mobile:
        processor_family = "Intel Core Duo Mobile";
        break;
    case (u16)SMBIOS::ProcessorFamily::Intel_Core_Solo_Mobile:
        processor_family = "Intel Core Solo Mobile";
        break;
    case (u16)SMBIOS::ProcessorFamily::Intel_Atom:
        processor_family = "Intel Atom";
        break;
    case (u16)SMBIOS::ProcessorFamily::Intel_Core_M:
        processor_family = "Intel Core M";
        break;
    case (u16)SMBIOS::ProcessorFamily::Intel_Core_m3:
        processor_family = "Intel Core m3";
        break;
    case (u16)SMBIOS::ProcessorFamily::Intel_Core_m5:
        processor_family = "Intel Core m3";
        break;
    case (u16)SMBIOS::ProcessorFamily::Intel_Core_m7:
        processor_family = "Intel Core m7";
        break;
    case (u16)SMBIOS::ProcessorFamily::Alpha_Family:
        processor_family = "Alpha Family";
        break;
    case (u16)SMBIOS::ProcessorFamily::Alpha_21064:
        processor_family = "Alpha 21064";
        break;
    case (u16)SMBIOS::ProcessorFamily::Alpha_21066:
        processor_family = "Alpha 21066";
        break;
    case (u16)SMBIOS::ProcessorFamily::Alpha_21164:
        processor_family = "Alpha 21164";
        break;
    case (u16)SMBIOS::ProcessorFamily::Alpha_21164PC:
        processor_family = "Alpha 21164PC";
        break;
    case (u16)SMBIOS::ProcessorFamily::Alpha_21164a:
        processor_family = "Alpha 21164a";
        break;
    case (u16)SMBIOS::ProcessorFamily::Alpha_21264:
        processor_family = "Alpha 21264";
        break;
    case (u16)SMBIOS::ProcessorFamily::Alpha_21364:
        processor_family = "Alpha 21364";
        break;
    case (u16)SMBIOS::ProcessorFamily::AMD_Turion_2_Ultra_DualCore_Mobile_M_Family:
        processor_family = "AMD Turion 2 Ultra Dual-Core Mobile M Family";
        break;
    case (u16)SMBIOS::ProcessorFamily::AMD_Turion_2_DualCore_Mobile_M_Family:
        processor_family = "AMD Turion 2 Dual-Core Mobile M Family";
        break;
    case (u16)SMBIOS::ProcessorFamily::AMD_Athlon_2_DualCore_M_Family:
        processor_family = "AMD Athlon 2 Dual-Core M Family";
        break;
    case (u16)SMBIOS::ProcessorFamily::AMD_Opteron_6100_Series:
        processor_family = "AMD Opteron 6100 Series";
        break;
    case (u16)SMBIOS::ProcessorFamily::AMD_Opteron_4100_Series:
        processor_family = "AMD Opteron 4100 Series";
        break;
    case (u16)SMBIOS::ProcessorFamily::AMD_Opteron_6200_Series:
        processor_family = "AMD Opteron 6200 Series";
        break;
    case (u16)SMBIOS::ProcessorFamily::AMD_Opteron_4200_Series:
        processor_family = "AMD Opteron 4200 Series";
        break;
    case (u16)SMBIOS::ProcessorFamily::AMD_FX_Series:
        processor_family = "AMD FX Series";
        break;
    case (u16)SMBIOS::ProcessorFamily::MIPS_Family:
        processor_family = "MIPS Family";
        break;
    case (u16)SMBIOS::ProcessorFamily::MIPS_R4000:
        processor_family = "MIPS R4000";
        break;
    case (u16)SMBIOS::ProcessorFamily::MIPS_R4200:
        processor_family = "MIPS R4200";
        break;
    case (u16)SMBIOS::ProcessorFamily::MIPS_R4400:
        processor_family = "MIPS R4400";
        break;
    case (u16)SMBIOS::ProcessorFamily::MIPS_R4600:
        processor_family = "MIPS R4600";
        break;
    case (u16)SMBIOS::ProcessorFamily::MIPS_R10000:
        processor_family = "MIPS R10000";
        break;
    case (u16)SMBIOS::ProcessorFamily::AMD_C_Series:
        processor_family = "AMD C Series";
        break;
    case (u16)SMBIOS::ProcessorFamily::AMD_E_Series:
        processor_family = "AMD E Series";
        break;
    case (u16)SMBIOS::ProcessorFamily::AMD_A_Series:
        processor_family = "AMD A Series";
        break;
    case (u16)SMBIOS::ProcessorFamily::AMD_G_Series:
        processor_family = "AMD G Series";
        break;
    case (u16)SMBIOS::ProcessorFamily::AMD_Z_Series:
        processor_family = "AMD Z Series";
        break;
    case (u16)SMBIOS::ProcessorFamily::AMD_R_Series:
        processor_family = "AMD R Series";
        break;
    case (u16)SMBIOS::ProcessorFamily::AMD_Opteron_4300_Series:
        processor_family = "AMD Opteron 4300 Series";
        break;
    case (u16)SMBIOS::ProcessorFamily::AMD_Opteron_6300_Series:
        processor_family = "AMD Opteron 6300 Series";
        break;
    case (u16)SMBIOS::ProcessorFamily::AMD_Opteron_3300_Series:
        processor_family = "AMD Opteron 3300 Series";
        break;
    case (u16)SMBIOS::ProcessorFamily::AMD_FirePro_Series:
        processor_family = "AMD FirePro Series";
        break;
    case (u16)SMBIOS::ProcessorFamily::SPARC_Family:
        processor_family = "SPARC Family";
        break;
    case (u16)SMBIOS::ProcessorFamily::SuperSPARC:
        processor_family = "SuperSPARC";
        break;
    case (u16)SMBIOS::ProcessorFamily::microSPARC_2:
        processor_family = "microSPARC 2";
        break;
    case (u16)SMBIOS::ProcessorFamily::microSPARC_2_ep:
        processor_family = "microSPARC 2 ep";
        break;
    case (u16)SMBIOS::ProcessorFamily::UltraSPARC:
        processor_family = "UltraSPARC";
        break;
    case (u16)SMBIOS::ProcessorFamily::UltraSPARC_2:
        processor_family = "UltraSPARC 2";
        break;
    case (u16)SMBIOS::ProcessorFamily::UltraSPARC_Iii:
        processor_family = "UltraSPARC Iii";
        break;
    case (u16)SMBIOS::ProcessorFamily::UltraSPARC_3:
        processor_family = "UltraSPARC 3";
        break;
    case (u16)SMBIOS::ProcessorFamily::UltraSPARC_3i:
        processor_family = "UltraSPARC 3i";
        break;
    case (u16)SMBIOS::ProcessorFamily::Motorola_68040_Family:
        processor_family = "Motorola 68040 Family";
        break;
    case (u16)SMBIOS::ProcessorFamily::Motorola_68xxx:
        processor_family = "Motorola 68xxx";
        break;
    case (u16)SMBIOS::ProcessorFamily::Motorola_68000:
        processor_family = "Motorola 68000";
        break;
    case (u16)SMBIOS::ProcessorFamily::Motorola_68010:
        processor_family = "Motorola 68010";
        break;
    case (u16)SMBIOS::ProcessorFamily::Motorola_68020:
        processor_family = "Motorola 68020";
        break;
    case (u16)SMBIOS::ProcessorFamily::Motorola_68030:
        processor_family = "Motorola 68030";
        break;
    case (u16)SMBIOS::ProcessorFamily::AMD_Athlon_X4_QuadCore_Family:
        processor_family = "AMD Athlon X4 Quad-Core Family";
        break;
    case (u16)SMBIOS::ProcessorFamily::AMD_Opteron_X1000_Series:
        processor_family = "AMD Opteron X1000 Series";
        break;
    case (u16)SMBIOS::ProcessorFamily::AMD_Opteron_X2000_Series_APU:
        processor_family = "AMD Opteron X2000 Series APU";
        break;
    case (u16)SMBIOS::ProcessorFamily::AMD_Opteron_A_Series:
        processor_family = "AMD Opteron A Series";
        break;
    case (u16)SMBIOS::ProcessorFamily::AMD_Opteron_X3000_Series_APU:
        processor_family = "AMD Opteron X3000 Series APU";
        break;
    case (u16)SMBIOS::ProcessorFamily::AMD_Zen_Family:
        processor_family = "AMD Zen Family";
        break;
    case (u16)SMBIOS::ProcessorFamily::Hobbit_Family:
        processor_family = "Hobbit Family";
        break;
    case (u16)SMBIOS::ProcessorFamily::Crusoe_TM5000_Family:
        processor_family = "Crusoe TM5000 Family";
        break;
    case (u16)SMBIOS::ProcessorFamily::Crusoe_TM3000_Family:
        processor_family = "Crusoe TM3000 Family";
        break;
    case (u16)SMBIOS::ProcessorFamily::Efficeon_TM8000_Family:
        processor_family = "Efficeon TM8000 Family";
        break;
    case (u16)SMBIOS::ProcessorFamily::Weitek:
        processor_family = "Weitek";
        break;
    case (u16)SMBIOS::ProcessorFamily::Itanium:
        processor_family = "Itanium";
        break;
    case (u16)SMBIOS::ProcessorFamily::AMD_Athlon_64:
        processor_family = "AMD Athlon 64";
        break;
    case (u16)SMBIOS::ProcessorFamily::AMD_Operton_Family:
        processor_family = "AMD Operton Family";
        break;
    case (u16)SMBIOS::ProcessorFamily::AMD_Sempron_Family:
        processor_family = "AMD Sempron Family";
        break;
    case (u16)SMBIOS::ProcessorFamily::AMD_Turion_64_Mobile:
        processor_family = "AMD Turion 64 Mobile";
        break;
    case (u16)SMBIOS::ProcessorFamily::DualCore_AMD_Opteron_Family:
        processor_family = "Dual-Core AMD Opteron Family";
        break;
    case (u16)SMBIOS::ProcessorFamily::AMD_Athlon_64_X2_DualCore_Family:
        processor_family = "AMD Athlon 64 X2 Dual-Core Family";
        break;
    case (u16)SMBIOS::ProcessorFamily::AMD_Turion_64_X2_Mobile_Technology:
        processor_family = "AMD Turion 64 X2 Mobile Technology";
        break;
    case (u16)SMBIOS::ProcessorFamily::QuadCore_AMD_Opteron_Family:
        processor_family = "Quad-Core AMD Opteron Family";
        break;
    case (u16)SMBIOS::ProcessorFamily::Third_Generation_AMD_Opteron_Family:
        processor_family = "Third Generation AMD Opteron Family";
        break;
    case (u16)SMBIOS::ProcessorFamily::AMD_Phenom_FX_QuadCore_Family:
        processor_family = "AMD Phenom FX Quad-Core Family";
        break;
    case (u16)SMBIOS::ProcessorFamily::AMD_Phenom_X4_QuadCore_Family:
        processor_family = "AMD Phenom X4 Quad-Core Family";
        break;
    case (u16)SMBIOS::ProcessorFamily::AMD_Phenom_X2_QuadCore_Family:
        processor_family = "AMD Phenom X2 Quad-Core Family";
        break;
    case (u16)SMBIOS::ProcessorFamily::AMD_Athlon_X2_DualCore_Family:
        processor_family = "AMD Athlon X2 Dual-Core Family";
        break;
    case (u16)SMBIOS::ProcessorFamily::PA_RISC_Family:
        processor_family = "PA RISC Family";
        break;
    case (u16)SMBIOS::ProcessorFamily::PA_RISC_8500:
        processor_family = "PA RISC 8500";
        break;
    case (u16)SMBIOS::ProcessorFamily::PA_RISC_8000:
        processor_family = "PA RISC 8000";
        break;
    case (u16)SMBIOS::ProcessorFamily::PA_RISC_7300LC:
        processor_family = "PA RISC 7300LC";
        break;
    case (u16)SMBIOS::ProcessorFamily::PA_RISC_7200:
        processor_family = "PA RISC 7200";
        break;
    case (u16)SMBIOS::ProcessorFamily::PA_RISC_7100LC:
        processor_family = "PA RISC 7100LC";
        break;
    case (u16)SMBIOS::ProcessorFamily::PA_RISC_7100:
        processor_family = "PA RISC 7100";
        break;
    case (u16)SMBIOS::ProcessorFamily::V30_Family:
        processor_family = "V30 Family";
        break;
    case (u16)SMBIOS::ProcessorFamily::QuadCore_Intel_Xeon_3200_Series:
        processor_family = "Quad-Core Intel Xeon 3200 Series";
        break;
    case (u16)SMBIOS::ProcessorFamily::DualCore_Intel_Xeon_3000_Series:
        processor_family = "Dual-Core Intel Xeon 3000 Series";
        break;
    case (u16)SMBIOS::ProcessorFamily::QuadCore_Intel_Xeon_5300_Series:
        processor_family = "Quad-Core Intel Xeon 5300 Series";
        break;
    case (u16)SMBIOS::ProcessorFamily::DualCore_Intel_Xeon_5100_Series:
        processor_family = "Dual-Core Intel Xeon 5100 Series";
        break;
    case (u16)SMBIOS::ProcessorFamily::DualCore_Intel_Xeon_5000_Series:
        processor_family = "Dual-Core Intel Xeon 5000 Series";
        break;
    case (u16)SMBIOS::ProcessorFamily::DualCore_Intel_Xeon_LV:
        processor_family = "Dual-Core Intel Xeon LV";
        break;
    case (u16)SMBIOS::ProcessorFamily::DualCore_Intel_Xeon_ULV:
        processor_family = "Dual-Core Intel Xeon ULV";
        break;
    case (u16)SMBIOS::ProcessorFamily::DualCore_Intel_Xeon_7100_Series:
        processor_family = "Dual-Core Intel Xeon 7100 Series";
        break;
    case (u16)SMBIOS::ProcessorFamily::QuadCore_Intel_Xeon_5400_Series:
        processor_family = "Quad-Core Intel Xeon 5400 Series";
        break;
    case (u16)SMBIOS::ProcessorFamily::QuadCore_Intel_Xeon:
        processor_family = "Quad-Core Intel Xeon";
        break;
    case (u16)SMBIOS::ProcessorFamily::DualCore_Intel_Xeon_5200_Series:
        processor_family = "Dual-Core Intel Xeon 5200 Series";
        break;
    case (u16)SMBIOS::ProcessorFamily::DualCore_Intel_Xeon_7200_Series:
        processor_family = "Dual-Core Intel Xeon 7200 Series";
        break;
    case (u16)SMBIOS::ProcessorFamily::QuadCore_Intel_Xeon_7300_Series:
        processor_family = "Quad-Core Intel Xeon 7300 Series";
        break;
    case (u16)SMBIOS::ProcessorFamily::QuadCore_Intel_Xeon_7400_Series:
        processor_family = "Quad-Core Intel Xeon 7400 Series";
        break;
    case (u16)SMBIOS::ProcessorFamily::MultiCore_Intel_Xeon_7400_Series:
        processor_family = "MultiCore Intel Xeon 7400 Series";
        break;
    case (u16)SMBIOS::ProcessorFamily::Intel_Pentium_3_Xeon:
        processor_family = "Intel Pentium 3 Xeon";
        break;
    case (u16)SMBIOS::ProcessorFamily::Intel_Pentium_3_SpeedStep:
        processor_family = "Intel Pentium 3 SpeedStep";
        break;
    case (u16)SMBIOS::ProcessorFamily::Intel_Pentium_4:
        processor_family = "Intel Pentium 4";
        break;
    case (u16)SMBIOS::ProcessorFamily::Intel_Xeon:
        processor_family = "Intel Xeon";
        break;
    case (u16)SMBIOS::ProcessorFamily::AS400_Family:
        processor_family = "AS400 Family";
        break;
    case (u16)SMBIOS::ProcessorFamily::Intel_Xeon_MP:
        processor_family = "Intel Xeon MP";
        break;
    case (u16)SMBIOS::ProcessorFamily::AMD_Athlon_XP_Family:
        processor_family = "AMD Athlon XP Family";
        break;
    case (u16)SMBIOS::ProcessorFamily::AMD_Athlon_MP_Family:
        processor_family = "AMD Athlon MP Family";
        break;
    case (u16)SMBIOS::ProcessorFamily::Intel_Itanium_2:
        processor_family = "Intel Itanium 2";
        break;
    case (u16)SMBIOS::ProcessorFamily::Intel_Pentium_M:
        processor_family = "Intel Pentium M";
        break;
    case (u16)SMBIOS::ProcessorFamily::Intel_Celeron_D:
        processor_family = "Intel Celeron D";
        break;
    case (u16)SMBIOS::ProcessorFamily::Intel_Pentium_D:
        processor_family = "Intel Pentium D";
        break;
    case (u16)SMBIOS::ProcessorFamily::Intel_Pentium_Extreme_Edition:
        processor_family = "Intel Pentium Extreme Edition";
        break;
    case (u16)SMBIOS::ProcessorFamily::Intel_Core_Solo:
        processor_family = "Intel Core Solo";
        break;
    case (u16)SMBIOS::ProcessorFamily::Reserved:
        processor_family = "Reserved";
        break;
    case (u16)SMBIOS::ProcessorFamily::Intel_Core_2_Duo:
        processor_family = "Intel Core 2 Duo";
        break;
    case (u16)SMBIOS::ProcessorFamily::Intel_Core_2_Solo:
        processor_family = "Intel Core 2 Solo";
        break;
    case (u16)SMBIOS::ProcessorFamily::Intel_Core_2_Extreme:
        processor_family = "Intel Core 2 Extreme";
        break;
    case (u16)SMBIOS::ProcessorFamily::Intel_Core_2_Quad:
        processor_family = "Intel Core 2 Quad";
        break;
    case (u16)SMBIOS::ProcessorFamily::Intel_Core_2_Extreme_Mobile:
        processor_family = "Intel Core 2 Extreme Mobile";
        break;
    case (u16)SMBIOS::ProcessorFamily::Intel_Core_2_Duo_Mobile:
        processor_family = "Intel Core 2 Duo Mobile";
        break;
    case (u16)SMBIOS::ProcessorFamily::Intel_Core_2_Solo_Mobile:
        processor_family = "Intel Core 2 Solo Mobile";
        break;
    case (u16)SMBIOS::ProcessorFamily::Intel_Core_i7:
        processor_family = "Intel Core i7";
        break;
    case (u16)SMBIOS::ProcessorFamily::DualCore_Intel_Celeron:
        processor_family = "Dual-Core Intel Celeron";
        break;
    case (u16)SMBIOS::ProcessorFamily::IBM390_Family:
        processor_family = "IBM390 Family";
        break;
    case (u16)SMBIOS::ProcessorFamily::G4:
        processor_family = "G4";
        break;
    case (u16)SMBIOS::ProcessorFamily::G5:
        processor_family = "G5";
        break;
    case (u16)SMBIOS::ProcessorFamily::ESA_390_G6:
        processor_family = "ESA 390 G6";
        break;
    case (u16)SMBIOS::ProcessorFamily::z_Architecture_base:
        processor_family = "z/Architecture base";
        break;
    case (u16)SMBIOS::ProcessorFamily::Intel_Core_i5:
        processor_family = "Intel Core i5";
        break;
    case (u16)SMBIOS::ProcessorFamily::Intel_Core_i3:
        processor_family = "Intel Core i3";
        break;
    case (u16)SMBIOS::ProcessorFamily::Intel_Core_i9:
        processor_family = "Intel Core i9";
        break;
    case (u16)SMBIOS::ProcessorFamily::VIA_C7_M_Family:
        processor_family = "VIA C7 M Family";
        break;
    case (u16)SMBIOS::ProcessorFamily::VIA_C7_D_Family:
        processor_family = "VIA C7 D Family";
        break;
    case (u16)SMBIOS::ProcessorFamily::VIA_C7_Family:
        processor_family = "VIA C7 Family";
        break;
    case (u16)SMBIOS::ProcessorFamily::VIA_Eden_Family:
        processor_family = "VIA Eden Family";
        break;
    case (u16)SMBIOS::ProcessorFamily::MultiCore_Intel_Xeon:
        processor_family = "MultiCore Intel Xeon";
        break;
    case (u16)SMBIOS::ProcessorFamily::DualCore_Intel_Xeon_3xxx_Series:
        processor_family = "Dual-Core Intel Xeon 3xxx Series";
        break;
    case (u16)SMBIOS::ProcessorFamily::QuadCore_Intel_Xeon_3xxx_Series:
        processor_family = "Quad-Core Intel Xeon 3xxx Series";
        break;
    case (u16)SMBIOS::ProcessorFamily::VIA_Nano_Family:
        processor_family = "VIA Nano Family";
        break;
    case (u16)SMBIOS::ProcessorFamily::DualCore_Intel_Xeon_5xxx_Series:
        processor_family = "Dual-Core Intel Xeon 5xxx Series";
        break;
    case (u16)SMBIOS::ProcessorFamily::QuadCore_Intel_Xeon_5xxx_Series:
        processor_family = "Quad-Core Intel Xeon 5xxx Series";
        break;
    case (u16)SMBIOS::ProcessorFamily::DualCore_Intel_Xeon_7xxx_Series:
        processor_family = "Dual-Core Intel Xeon 7xxx Series";
        break;
    case (u16)SMBIOS::ProcessorFamily::QuadCore_Intel_Xeon_7xxx_Series:
        processor_family = "Quad-Core Intel Xeon 7xxx Series";
        break;
    case (u16)SMBIOS::ProcessorFamily::MultiCore_Intel_Xeon_7xxx_Series:
        processor_family = "MultiCore Intel Xeon 7xxx Series";
        break;
    case (u16)SMBIOS::ProcessorFamily::MultiCore_Intel_Xeon_3400_Series:
        processor_family = "MultiCore Intel Xeon 3400 Series";
        break;
    case (u16)SMBIOS::ProcessorFamily::AMD_Opteron_3000_Series:
        processor_family = "AMD Opteron 3000 Series";
        break;
    case (u16)SMBIOS::ProcessorFamily::AMD_Sempron_2:
        processor_family = "AMD Sempron 2";
        break;
    case (u16)SMBIOS::ProcessorFamily::Embedded_AMD_Opteron_QuadCore_Family:
        processor_family = "Embedded AMD Opteron Quad-Core Family";
        break;
    case (u16)SMBIOS::ProcessorFamily::AMD_Phenom_TripleCore_Family:
        processor_family = "AMD Phenom Triple-Core Family";
        break;
    case (u16)SMBIOS::ProcessorFamily::AMD_Turion_Ultra_DualCore_Mobile_Family:
        processor_family = "AMD Turion Ultra Dual-Core Mobile Family";
        break;
    case (u16)SMBIOS::ProcessorFamily::AMD_Turion_DualCore_Mobile_Family:
        processor_family = "AMD Turion Dual-Core Mobile Family";
        break;
    case (u16)SMBIOS::ProcessorFamily::AMD_Athlon_DualCore_Family:
        processor_family = "AMD Athlon Dual-Core Family";
        break;
    case (u16)SMBIOS::ProcessorFamily::AMD_Sempron_SI_Family:
        processor_family = "AMD Sempron SI Family";
        break;
    case (u16)SMBIOS::ProcessorFamily::AMD_Phenom_2_Family:
        processor_family = "AMD Phenom 2 Family";
        break;
    case (u16)SMBIOS::ProcessorFamily::AMD_Athlon_2_Family:
        processor_family = "AMD Athlon 2 Family";
        break;
    case (u16)SMBIOS::ProcessorFamily::SixCore_AMD_Opteron_Family:
        processor_family = "SixCore AMD Opteron Family";
        break;
    case (u16)SMBIOS::ProcessorFamily::AMD_Sempron_M_Family:
        processor_family = "AMD Sempron M Family";
        break;
    case (u16)SMBIOS::ProcessorFamily::i860:
        processor_family = "i860";
        break;
    case (u16)SMBIOS::ProcessorFamily::i960:
        processor_family = "i960";
        break;
    case (u16)SMBIOS::ProcessorFamily::ProcessorFamily2Indicator: // Indicator to obtain the processor family from the Processor Family 2 field
        switch (table.processor_family2) {
        case (u16)SMBIOS::ProcessorFamily::ARMv7:
            processor_family = "ARMv7";
            break;
        case (u16)SMBIOS::ProcessorFamily::ARMv8:
            processor_family = "ARMv8";
            break;
        case (u16)SMBIOS::ProcessorFamily::SH_3:
            processor_family = "SH-3";
            break;
        case (u16)SMBIOS::ProcessorFamily::SH_4:
            processor_family = "SH-4";
            break;
        case (u16)SMBIOS::ProcessorFamily::ARM:
            processor_family = "ARM";
            break;
        case (u16)SMBIOS::ProcessorFamily::StrongARM:
            processor_family = "StrongARM";
            break;
        case (u16)SMBIOS::ProcessorFamily::Cyrix_6x86:
            processor_family = "Cyrix 6x86";
            break;
        case (u16)SMBIOS::ProcessorFamily::MediaGX:
            processor_family = "MediaGX";
            break;
        case (u16)SMBIOS::ProcessorFamily::MII:
            processor_family = "MII";
            break;
        case (u16)SMBIOS::ProcessorFamily::WinChip:
            processor_family = "WinChip";
            break;
        case (u16)SMBIOS::ProcessorFamily::DSP:
            processor_family = "DSP";
            break;
        case (u16)SMBIOS::ProcessorFamily::VideoProcessor:
            processor_family = "Video Processor";
            break;
        case (u16)SMBIOS::ProcessorFamily::RISC_V_RV32:
            processor_family = "RISC-V RV32";
            break;
        case (u16)SMBIOS::ProcessorFamily::RISC_V_RV64:
            processor_family = "RISC-V RV64";
            break;
        }
        break;
    case (u16)SMBIOS::ProcessorFamily::Reserved2:
        processor_family = "Unknown";
        break;
    default:
        processor_family = "Unknown";
        break;
    }
    tab() << "Processor Family: " << processor_family;
    auto processor_manufacturer_string = SMBIOS::Parsing::try_to_acquire_smbios_string((const SMBIOS::TableHeader&)table, table.processor_manufacturer_str_number);
    tab() << "Processor Manufacturer: " << (processor_manufacturer_string.has_value() ? processor_manufacturer_string.value() : "Unknown");
    tab() << "Processor ID: 0x" << String::format("%x", table.processor_id);
    auto processor_version_string = SMBIOS::Parsing::try_to_acquire_smbios_string((const SMBIOS::TableHeader&)table, table.processor_version_str_number);
    tab() << "Processor Version: " << (processor_version_string.has_value() ? processor_version_string.value() : "Unknown");

    switch (table.voltage) {
    case (1 << 0):
        tab() << "Processor Voltage: 5V";
        __attribute__((fallthrough));
    case (1 << 1):
        tab() << "Processor Voltage: 3.3V";
        __attribute__((fallthrough));
    case (1 << 2):
        tab() << "Processor Voltage: 2.9V";
        __attribute__((fallthrough));
    default:
        tab() << "Processor Voltage: Unknown";
        break;
    }
    tab() << "External Clock Frequency: " << ((table.external_clock > 0) ? String::format("%d MHz", table.external_clock).characters() : "Unknown");
    tab() << "Max Speed: " << ((table.max_speed > 0) ? String::format("%d MHz", table.max_speed).characters() : "Unknown");
    tab() << "Current Speed: " << ((table.current_speed > 0) ? String::format("%d MHz", table.current_speed).characters() : "Unknown");
    tab() << "Status:";
    tab() << tab() << ((table.status & (1 << 6)) ? "CPU Socket Populated" : "CPU Socket Unpopulated");
    String cpu_status;
    switch (table.status & 0b111) {
    case 0:
        cpu_status = "Unknown";
        break;
    case 1:
        cpu_status = "CPU Enabled";
        break;
    case 2:
        cpu_status = "CPU Disabled by User through BIOS Setup";
        break;
    case 3:
        cpu_status = "CPU Disabled By BIOS (POST Error)";
        break;
    case 4:
        cpu_status = "CPU is Idle, waiting to be enabled";
        break;
    case 7:
        cpu_status = "Other";
        break;
    default:
        cpu_status = "Unknown";
        break;
    }
    tab() << tab() << "CPU Status: " << cpu_status;

    String processor_upgrade;
    switch (table.processor_upgrade) {
    case (u16)SMBIOS::ProcessorUpgrade::Other:
        processor_upgrade = "Other";
        break;
    case (u16)SMBIOS::ProcessorUpgrade::Unknown:
        processor_upgrade = "Unknown";
        break;
    case (u16)SMBIOS::ProcessorUpgrade::Daughter_Board:
        processor_upgrade = "Daughter Board";
        break;
    case (u16)SMBIOS::ProcessorUpgrade::ZIF_Socket:
        processor_upgrade = "ZIF Socket";
        break;
    case (u16)SMBIOS::ProcessorUpgrade::Replaceable_Piggy_Back:
        processor_upgrade = "Replaceable Piggy Back";
        break;
    case (u16)SMBIOS::ProcessorUpgrade::None:
        processor_upgrade = "None";
        break;
    case (u16)SMBIOS::ProcessorUpgrade::LIF_Sokcet:
        processor_upgrade = "LIF Sokcet";
        break;
    case (u16)SMBIOS::ProcessorUpgrade::Slot_1:
        processor_upgrade = "Slot 1";
        break;
    case (u16)SMBIOS::ProcessorUpgrade::Slot_2:
        processor_upgrade = "Slot 2";
        break;
    case (u16)SMBIOS::ProcessorUpgrade::Socket_370_pin:
        processor_upgrade = "Socket 370 pin";
        break;
    case (u16)SMBIOS::ProcessorUpgrade::Slot_A:
        processor_upgrade = "Slot A";
        break;
    case (u16)SMBIOS::ProcessorUpgrade::Slot_M:
        processor_upgrade = "Slot M";
        break;
    case (u16)SMBIOS::ProcessorUpgrade::Socket_423:
        processor_upgrade = "Socket 423";
        break;
    case (u16)SMBIOS::ProcessorUpgrade::Socket_A_462:
        processor_upgrade = "Socket A 462";
        break;
    case (u16)SMBIOS::ProcessorUpgrade::Socket_478:
        processor_upgrade = "Socket 478";
        break;
    case (u16)SMBIOS::ProcessorUpgrade::Socket_754:
        processor_upgrade = "Socket 754";
        break;
    case (u16)SMBIOS::ProcessorUpgrade::Socket_940:
        processor_upgrade = "Socket 940";
        break;
    case (u16)SMBIOS::ProcessorUpgrade::Socket_939:
        processor_upgrade = "Socket 939";
        break;
    case (u16)SMBIOS::ProcessorUpgrade::Socket_mPGA604:
        processor_upgrade = "Socket mPGA604";
        break;
    case (u16)SMBIOS::ProcessorUpgrade::Socket_LGA771:
        processor_upgrade = "Socket LGA771";
        break;
    case (u16)SMBIOS::ProcessorUpgrade::Socket_LGA775:
        processor_upgrade = "Socket LGA775";
        break;
    case (u16)SMBIOS::ProcessorUpgrade::Socket_S1:
        processor_upgrade = "Socket S1";
        break;
    case (u16)SMBIOS::ProcessorUpgrade::Socket_AM2:
        processor_upgrade = "Socket AM2";
        break;
    case (u16)SMBIOS::ProcessorUpgrade::Socket_F_1207:
        processor_upgrade = "Socket F 1207";
        break;
    case (u16)SMBIOS::ProcessorUpgrade::Socket_LGA1366:
        processor_upgrade = "Socket LGA1366";
        break;
    case (u16)SMBIOS::ProcessorUpgrade::Socket_G34:
        processor_upgrade = "Socket G34";
        break;
    case (u16)SMBIOS::ProcessorUpgrade::Socket_AM3:
        processor_upgrade = "Socket AM3";
        break;
    case (u16)SMBIOS::ProcessorUpgrade::Socket_C32:
        processor_upgrade = "Socket C32";
        break;
    case (u16)SMBIOS::ProcessorUpgrade::Socket_LGA1156:
        processor_upgrade = "Socket LGA1156";
        break;
    case (u16)SMBIOS::ProcessorUpgrade::Socket_LGA1567:
        processor_upgrade = "Socket LGA1567";
        break;
    case (u16)SMBIOS::ProcessorUpgrade::Socket_PGA988A:
        processor_upgrade = "Socket PGA988A";
        break;
    case (u16)SMBIOS::ProcessorUpgrade::Socket_BGA1288:
        processor_upgrade = "Socket BGA1288";
        break;
    case (u16)SMBIOS::ProcessorUpgrade::Socket_rPGA988B:
        processor_upgrade = "Socket rPGA988B";
        break;
    case (u16)SMBIOS::ProcessorUpgrade::Socket_BGA1023:
        processor_upgrade = "Socket BGA1023";
        break;
    case (u16)SMBIOS::ProcessorUpgrade::Socket_BGA1224:
        processor_upgrade = "Socket BGA1224";
        break;
    case (u16)SMBIOS::ProcessorUpgrade::Socket_LGA1155:
        processor_upgrade = "Socket LGA1155";
        break;
    case (u16)SMBIOS::ProcessorUpgrade::Socket_LGA1356:
        processor_upgrade = "Socket LGA1356";
        break;
    case (u16)SMBIOS::ProcessorUpgrade::Socket_LGA2011:
        processor_upgrade = "Socket LGA2011";
        break;
    case (u16)SMBIOS::ProcessorUpgrade::Socket_FS1:
        processor_upgrade = "Socket FS1";
        break;
    case (u16)SMBIOS::ProcessorUpgrade::Socket_FS2:
        processor_upgrade = "Socket FS2";
        break;
    case (u16)SMBIOS::ProcessorUpgrade::Socket_FM1:
        processor_upgrade = "Socket FM1";
        break;
    case (u16)SMBIOS::ProcessorUpgrade::Socket_FM2:
        processor_upgrade = "Socket FM2";
        break;
    case (u16)SMBIOS::ProcessorUpgrade::Socket_LGA2011_3:
        processor_upgrade = "Socket LGA2011 3";
        break;
    case (u16)SMBIOS::ProcessorUpgrade::Socket_LGA1356_3:
        processor_upgrade = "Socket LGA1356 3";
        break;
    case (u16)SMBIOS::ProcessorUpgrade::Socket_LGA1150:
        processor_upgrade = "Socket LGA1150";
        break;
    case (u16)SMBIOS::ProcessorUpgrade::Socket_BGA1168:
        processor_upgrade = "Socket BGA1168";
        break;
    case (u16)SMBIOS::ProcessorUpgrade::Socket_BGA1234:
        processor_upgrade = "Socket BGA1234";
        break;
    case (u16)SMBIOS::ProcessorUpgrade::Socket_BGA1364:
        processor_upgrade = "Socket BGA1364";
        break;
    case (u16)SMBIOS::ProcessorUpgrade::Socket_AM4:
        processor_upgrade = "Socket AM4";
        break;
    case (u16)SMBIOS::ProcessorUpgrade::Socket_LGA1151:
        processor_upgrade = "Socket LGA1151";
        break;
    case (u16)SMBIOS::ProcessorUpgrade::Socket_BGA1356:
        processor_upgrade = "Socket BGA1356";
        break;
    case (u16)SMBIOS::ProcessorUpgrade::Socket_BGA1440:
        processor_upgrade = "Socket BGA1440";
        break;
    case (u16)SMBIOS::ProcessorUpgrade::Socket_BGA1515:
        processor_upgrade = "Socket BGA1515";
        break;
    case (u16)SMBIOS::ProcessorUpgrade::Socket_LGA3647_1:
        processor_upgrade = "ocket LGA3647 1";
        break;
    case (u16)SMBIOS::ProcessorUpgrade::Socket_SP3:
        processor_upgrade = "Socket SP3";
        break;
    case (u16)SMBIOS::ProcessorUpgrade::Socket_SP3r2:
        processor_upgrade = "Socket SP3r2";
        break;
    case (u16)SMBIOS::ProcessorUpgrade::Socket_LGA2066:
        processor_upgrade = "Socket LGA2066";
        break;
    case (u16)SMBIOS::ProcessorUpgrade::Socket_BGA1392:
        processor_upgrade = "Socket BGA1392";
        break;
    case (u16)SMBIOS::ProcessorUpgrade::Socket_BGA1510:
        processor_upgrade = "Socket BGA1510";
        break;
    case (u16)SMBIOS::ProcessorUpgrade::Socket_BGA1528:
        processor_upgrade = "Socket BGA1528";
        break;
    default:
        processor_upgrade = "Unknown";
        break;
    }
    tab() << "Processor Upgrade: " << processor_upgrade;

    tab() << "L1 Cache Handle: 0x" << String::format("%x", table.l1_cache_handle);
    if (table.h.length >= 0x1C) {
        tab() << "L2 Cache Handle: 0x" << String::format("%x", table.l2_cache_handle);
    } else {
        printf("\n");
        return;
    }

    if (table.h.length >= 0x1E) {
        tab() << "L3 Cache Handle: 0x" << String::format("%x", table.l3_cache_handle);
    } else {
        printf("\n");
        return;
    }

    if (table.h.length >= 0x20) {
        auto serial_number_string = SMBIOS::Parsing::try_to_acquire_smbios_string((const SMBIOS::TableHeader&)table, table.serial_number_str_number);
        tab() << "Serial Number: " << (serial_number_string.has_value() ? serial_number_string.value() : "Unknown");
    } else {
        printf("\n");
        return;
    }

    if (table.h.length >= 0x21) {
        auto asset_tag_number_string = SMBIOS::Parsing::try_to_acquire_smbios_string((const SMBIOS::TableHeader&)table, table.asset_tag_str_number);
        tab() << "Asset Tag: " << (asset_tag_number_string.has_value() ? asset_tag_number_string.value() : "Unknown");
    } else {
        printf("\n");
        return;
    }

    if (table.h.length >= 0x22) {
        auto part_number_string = SMBIOS::Parsing::try_to_acquire_smbios_string((const SMBIOS::TableHeader&)table, table.part_number_str_number);
        tab() << "Part Number: " << (part_number_string.has_value() ? part_number_string.value() : "Unknown");
    } else {
        printf("\n");
        return;
    }

    if (table.h.length >= 0x23) {
        String core_count;
        switch (table.core_count) {
        case 0:
            core_count = "Unknown";
            break;
        case 0xFF: {
            if (table.h.length >= 0x2A) {
                switch (table.core_count2) {
                case 0xFFFF:
                case 0:
                    core_count = "Unknown";
                    break;
                default:
                    core_count = String::format("%u", table.core_count2);
                }
            } else {
                core_count = "Unknown";
            }
            break;
        }
        default:
            core_count = String::format("%u", table.core_count);
            break;
        }
        tab() << "Core Count: " << core_count;
    } else {
        printf("\n");
        return;
    }

    if (table.h.length >= 0x24) {
        String core_enabled;
        switch (table.core_enabled) {
        case 0:
            core_enabled = "Unknown";
            break;
        case 0xFF: {
            if (table.h.length >= 0x2C) {
                switch (table.core_enabled2) {
                case 0xFFFF:
                case 0:
                    core_enabled = "Unknown";
                    break;
                default:
                    core_enabled = String::format("%u", table.core_enabled2);
                }
            } else {
                core_enabled = "Unknown";
            }
            break;
        }
        default:
            core_enabled = String::format("%u", table.core_enabled);
            break;
        }
        tab() << "Core Enabled Count: " << core_enabled;
    } else {
        printf("\n");
        return;
    }

    if (table.h.length >= 0x25) {
        String thread_count;
        switch (table.thread_count) {
        case 0:
            thread_count = "Unknown";
            break;
        case 0xFF: {
            if (table.h.length >= 0x2E) {
                switch (table.thread_count2) {
                case 0xFFFF:
                case 0:
                    thread_count = "Unknown";
                    break;
                default:
                    thread_count = String::format("%u", table.thread_count2);
                }
            } else {
                thread_count = "Unknown";
            }
            break;
        }
        default:
            thread_count = String::format("%u", table.thread_count);
            break;
        }
        tab() << "Thread Count: " << thread_count;
    } else {
        printf("\n");
        return;
    }
    if (table.h.length >= 0x26) {
        tab() << "Processor Characteristics:";
        switch (table.processor_characteristics) {
        case (u16)SMBIOS::ProcessorCharacteristics::Unknown:
            tab() << tab() << "Unknown";
            break;
        case (u16)SMBIOS::ProcessorCharacteristics::Capable_64_Bit:
            tab() << tab() << "64-bit Capable";
            __attribute__((fallthrough));
        case (u16)SMBIOS::ProcessorCharacteristics::Multi_Core:
            tab() << tab() << "Multi-Core";
            __attribute__((fallthrough));
        case (u16)SMBIOS::ProcessorCharacteristics::Hardware_Thread:
            tab() << tab() << "Hardware Thread";
            __attribute__((fallthrough));
        case (u16)SMBIOS::ProcessorCharacteristics::Enhanced_Virtualization:
            tab() << tab() << "Enhanced Virtualization";
            __attribute__((fallthrough));
        case (u16)SMBIOS::ProcessorCharacteristics::Power_Performance_Control:
            tab() << tab() << "Power/Performance Control";
            __attribute__((fallthrough));
        case (u16)SMBIOS::ProcessorCharacteristics::Capable_128_Bit:
            tab() << tab() << "128-bit Capable";
            __attribute__((fallthrough));
        default:
            tab() << tab() << "Unknown";
            break;
        }
    }

    printf("\n");
}

String parse_sram_type(u8 sram_type_value)
{
    String sram_type;
    switch (sram_type_value) {
    case (u16)SMBIOS::SRAMType::Other:
        sram_type = "Other";
        break;
    case (u16)SMBIOS::SRAMType::Unknown:
        sram_type = "Unknown";
        break;
    case (u16)SMBIOS::SRAMType::Non_Burst:
        sram_type = "Non-Burst";
        break;
    case (u16)SMBIOS::SRAMType::Burst:
        sram_type = "Burst";
        break;
    case (u16)SMBIOS::SRAMType::Pipeline_Burst:
        sram_type = "Pipeline Burst";
        break;
    case (u16)SMBIOS::SRAMType::Synchronous:
        sram_type = "Synchronous";
        break;
    case (u16)SMBIOS::SRAMType::Asynchronous:
        sram_type = "Asynchronous";
        break;
    default:
        sram_type = "Unknown";
        break;
    }
    return sram_type;
}

void parse_table_type7(const SMBIOS::CacheInfo& table)
{
    ASSERT(table.h.type == (u8)SMBIOS::TableType::CacheInfo);
    ASSERT(table.h.length >= 0xF);
    title() << "Cache Information";

    auto socket_designation_string = SMBIOS::Parsing::try_to_acquire_smbios_string((const SMBIOS::TableHeader&)table, table.socket_designation_str_number);
    tab() << "Socket Designation: " << (socket_designation_string.has_value() ? socket_designation_string.value() : "Unknown");

    tab() << "Cache Configuration:";

    String operational_mode;
    switch ((table.cache_config >> 8) & 0b11) {
    case 0:
        operational_mode = "Write Through";
        break;
    case 1:
        operational_mode = "Write Back";
        break;
    case 2:
        operational_mode = "Varies with Memory Address";
        break;
    case 3:
        operational_mode = "Unknown";
        break;
    default:
        operational_mode = "Unknown";
        break;
    }
    tab() << tab() << "Operational Mode: " << operational_mode;
    tab() << tab() << ((table.cache_config & (1 << 7)) ? "Enabled" : "Disabled");
    String location;
    switch ((table.cache_config >> 8) & 0b11) {
    case 0:
        location = "Internal";
        break;
    case 1:
        location = "External";
        break;
    case 2:
        location = "Reserved";
        break;
    case 3:
        location = "Unknown";
        break;
    default:
        location = "Unknown";
        break;
    }
    tab() << tab() << "Location, relative to the CPU module: " << location;
    tab() << tab() << ((table.cache_config & (1 << 3)) ? "Socketed" : "Not Socketed");
    tab() << tab() << "Cache Level: L" << ((table.cache_config & 0b111) + 1);

    if ((((table.max_cache_size & 0x7fff) ^ 0x7fff) == 0) && table.h.length >= 0x13) {
        tab() << "Maximum Cache Size: " << table.max_cache_size2 * ((table.max_cache_size2 & (1 << 31)) ? (64 * KB) : (1 * KB));
    } else {
        tab() << "Maximum Cache Size: " << table.max_cache_size * ((table.max_cache_size & (1 << 15)) ? (64 * KB) : (1 * KB));
    }

    if (table.installed_size == 0) {
        tab() << "Installed Size: 0";
    } else {
        if ((((table.installed_size & 0x7fff) ^ 0x7fff) == 0) && table.h.length >= 0x17) {
            tab() << "Installed Size: " << table.installed_size2 * ((table.installed_size2 & (1 << 31)) ? (64 * KB) : (1 * KB));
        } else {
            tab() << "Installed Size: " << table.installed_size * ((table.installed_size & (1 << 15)) ? (64 * KB) : (1 * KB));
        }
    }

    tab() << "Supported SRAM Type: " << parse_sram_type(table.supported_sram_type);
    tab() << "Current SRAM Type: " << parse_sram_type(table.current_sram_type);

    if (table.cache_speed == 0) {
        tab() << "Cache Speed: Unknown";
    } else {
        tab() << "Cache Speed: " << table.cache_speed << " nanoseconds";
    }

    if (table.h.length >= 0x10) {
        String error_correction_type;
        switch (table.error_correction_type) {
        case (u16)SMBIOS::ErrorCorrectionType::Other:
            error_correction_type = "Other";
            break;
        case (u16)SMBIOS::ErrorCorrectionType::Unknown:
            error_correction_type = "Unknown";
            break;
        case (u16)SMBIOS::ErrorCorrectionType::None:
            error_correction_type = "None";
            break;
        case (u16)SMBIOS::ErrorCorrectionType::Parity:
            error_correction_type = "Parity";
            break;
        case (u16)SMBIOS::ErrorCorrectionType::Single_Bit_ECC:
            error_correction_type = "Single-Bit ECC";
            break;
        case (u16)SMBIOS::ErrorCorrectionType::Multi_Bit_ECC:
            error_correction_type = "Multi-Bit ECC";
            break;
        default:
            error_correction_type = "Unknown";
            break;
        }
        tab() << "Error Correction Type: " << error_correction_type;
    } else {
        printf("\n");
        return;
    }

    if (table.h.length >= 0x11) {
        String system_cache_type;
        switch (table.system_cache_type) {
        case (u16)SMBIOS::SystemCacheType::Other:
            system_cache_type = "Other";
            break;
        case (u16)SMBIOS::SystemCacheType::Unknown:
            system_cache_type = "Unknown";
            break;
        case (u16)SMBIOS::SystemCacheType::Instruction:
            system_cache_type = "Instruction";
            break;
        case (u16)SMBIOS::SystemCacheType::Data:
            system_cache_type = "Data";
            break;
        case (u16)SMBIOS::SystemCacheType::Unified:
            system_cache_type = "Unified";
            break;
        default:
            system_cache_type = "Unknown";
            break;
        }
        tab() << "System Cache Type: " << system_cache_type;
    } else {
        printf("\n");
        return;
    }

    String associativity;
    switch (table.system_cache_type) {
    case (u16)SMBIOS::Associativity::Other:
        associativity = "Other";
        break;
    case (u16)SMBIOS::Associativity::Unknown:
        associativity = "Unknown";
        break;
    case (u16)SMBIOS::Associativity::DirectMapped:
        associativity = "Direct Mapped";
        break;
    case (u16)SMBIOS::Associativity::Set_Associative_2_way:
        associativity = "2-way Set-Associative";
        break;
    case (u16)SMBIOS::Associativity::Set_Associative_4_way:
        associativity = "4-way Set-Associative";
        break;
    case (u16)SMBIOS::Associativity::Fully_Associative:
        associativity = "Fully Associative";
        break;
    case (u16)SMBIOS::Associativity::Set_Associative_8_way:
        associativity = "8-way Set-Associative";
        break;
    case (u16)SMBIOS::Associativity::Set_Associative_16_way:
        associativity = "16-way Set-Associative";
        break;
    case (u16)SMBIOS::Associativity::Set_Associative_12_way:
        associativity = "12-way Set-Associative";
        break;
    case (u16)SMBIOS::Associativity::Set_Associative_24_way:
        associativity = "24-way Set-Associative";
        break;
    case (u16)SMBIOS::Associativity::Set_Associative_32_way:
        associativity = "32-way Set-Associative";
        break;
    case (u16)SMBIOS::Associativity::Set_Associative_48_way:
        associativity = "48-way Set-Associative";
        break;
    case (u16)SMBIOS::Associativity::Set_Associative_64_way:
        associativity = "64-way Set-Associative";
        break;
    case (u16)SMBIOS::Associativity::Set_Associative_20_way:
        associativity = "20-way Set-Associative";
        break;
    default:
        associativity = "Unknown";
        break;
    }
    tab() << "Associativity: " << associativity;

    printf("\n");
    return;
}

String parse_connector_type(u8 connector_type_value)
{
    String connector_type;
    switch (connector_type_value) {
    case (u16)SMBIOS::ConnectorType::None:
        connector_type = "None";
        break;
    case (u16)SMBIOS::ConnectorType::Centronics:
        connector_type = "Centronics";
        break;
    case (u16)SMBIOS::ConnectorType::Mini_Centronics:
        connector_type = "Mini Centronics";
        break;
    case (u16)SMBIOS::ConnectorType::Proprietary:
        connector_type = "Proprietary";
        break;
    case (u16)SMBIOS::ConnectorType::DB_25_pin_male:
        connector_type = "DB-25 pin male";
        break;
    case (u16)SMBIOS::ConnectorType::DB_25_pin_female:
        connector_type = "DB-25 pin female";
        break;
    case (u16)SMBIOS::ConnectorType::DB_15_pin_male:
        connector_type = "DB-15 pin male";
        break;
    case (u16)SMBIOS::ConnectorType::DB_15_pin_female:
        connector_type = "DB-15 pin female";
        break;
    case (u16)SMBIOS::ConnectorType::DB_9_pin_male:
        connector_type = "DB-9 pin male";
        break;
    case (u16)SMBIOS::ConnectorType::DB_9_pin_female:
        connector_type = "DB-9 pin female";
        break;
    case (u16)SMBIOS::ConnectorType::RJ_11:
        connector_type = "RJ-11";
        break;
    case (u16)SMBIOS::ConnectorType::RJ_45:
        connector_type = "RJ-45";
        break;
    case (u16)SMBIOS::ConnectorType::MiniSCSI_50_pin:
        connector_type = "50-pin MiniSCSI";
        break;
    case (u16)SMBIOS::ConnectorType::MiniDIN:
        connector_type = "Mini-DIN";
        break;
    case (u16)SMBIOS::ConnectorType::MicroDIN:
        connector_type = "Micro-DIN";
        break;
    case (u16)SMBIOS::ConnectorType::PS2:
        connector_type = "PS/2";
        break;
    case (u16)SMBIOS::ConnectorType::Infrared:
        connector_type = "Infrared";
        break;
    case (u16)SMBIOS::ConnectorType::HP_HIL:
        connector_type = "HP-HIL";
        break;
    case (u16)SMBIOS::ConnectorType::AccessBus_USB:
        connector_type = "Access Bus (USB)";
        break;
    case (u16)SMBIOS::ConnectorType::SSA_SCSI:
        connector_type = "SSA SCSI";
        break;
    case (u16)SMBIOS::ConnectorType::Circular_DIN8_male:
        connector_type = "Circular DIN-8 male";
        break;
    case (u16)SMBIOS::ConnectorType::OnBoard_IDE:
        connector_type = "On Board IDE";
        break;
    case (u16)SMBIOS::ConnectorType::OnBoard_Floppy:
        connector_type = "On Board Floppy";
        break;
    case (u16)SMBIOS::ConnectorType::Dual_Inline_9pin:
        connector_type = "9-pin Dual Inline (pin 10 cut)";
        break;
    case (u16)SMBIOS::ConnectorType::Dual_Inline_25pin:
        connector_type = "25-pin Dual Inline (pin 26 cut)";
        break;
    case (u16)SMBIOS::ConnectorType::Dual_Inline_50pin:
        connector_type = "50-pin Dual Inline";
        break;
    case (u16)SMBIOS::ConnectorType::Dual_Inline_68pin:
        connector_type = "68-pin Dual Inline";
        break;
    case (u16)SMBIOS::ConnectorType::OnBoard_SoundInput_CDROM:
        connector_type = "On Board Sound Input from CD-ROM";
        break;
    case (u16)SMBIOS::ConnectorType::Mini_Centronics_Type14:
        connector_type = "Mini-Centronics Type-14";
        break;
    case (u16)SMBIOS::ConnectorType::Mini_Centronics_Type26:
        connector_type = "Mini-Centronics Type-26";
        break;
    case (u16)SMBIOS::ConnectorType::Mini_Jack_Headphones:
        connector_type = "Mini-jack (headphones)";
        break;
    case (u16)SMBIOS::ConnectorType::BNC:
        connector_type = "BNC";
        break;
    case (u16)SMBIOS::ConnectorType::Connector_1394:
        connector_type = "1394";
        break;
    case (u16)SMBIOS::ConnectorType::SAS_SATA_Plug_Receptacle:
        connector_type = "SAS/SATA Plug Receptacle";
        break;
    case (u16)SMBIOS::ConnectorType::USB_TypeC_Receptacle:
        connector_type = "USB Type-C Receptacle";
        break;
    case (u16)SMBIOS::ConnectorType::PC98:
        connector_type = "PC-98";
        break;
    case (u16)SMBIOS::ConnectorType::PC98_Hireso:
        connector_type = "PC-98Hireso";
        break;
    case (u16)SMBIOS::ConnectorType::PC_H98:
        connector_type = "PC-H98";
        break;
    case (u16)SMBIOS::ConnectorType::PC98_Note:
        connector_type = "PC-98Note";
        break;
    case (u16)SMBIOS::ConnectorType::PC98_Full:
        connector_type = "PC-98Full";
        break;
    case (u16)SMBIOS::ConnectorType::Other:
        connector_type = "Other";
        break;
    default:
        connector_type = "Unknown";
        break;
    }
    return connector_type;
}

void parse_table_type8(const SMBIOS::PortConnectorInfo& table)
{
    ASSERT(table.h.type == (u8)SMBIOS::TableType::PortConnectorInfo);
    ASSERT(table.h.length >= 0x9);
    title() << "Port Connector Information";

    auto internal_reference_Designator_string = SMBIOS::Parsing::try_to_acquire_smbios_string((const SMBIOS::TableHeader&)table, table.internal_reference_designator_str_number);
    tab() << "Internal Reference Designator: " << (internal_reference_Designator_string.has_value() ? internal_reference_Designator_string.value() : "Unknown");
    tab() << "Internal Connector Type: " << parse_connector_type(table.internal_connector_type);

    auto external_reference_Designator_string = SMBIOS::Parsing::try_to_acquire_smbios_string((const SMBIOS::TableHeader&)table, table.external_reference_designator_str_number);
    tab() << "External Reference Designator: " << (external_reference_Designator_string.has_value() ? external_reference_Designator_string.value() : "Unknown");
    tab() << "External Connector Type: " << parse_connector_type(table.external_connector_type);

    String port_type;
    switch (table.port_type) {
    case (u8)SMBIOS::PortType::None:
        port_type = "None";
        break;
    case (u8)SMBIOS::PortType::Parallel_Port_XT_AT_Compatible:
        port_type = "Parallel Port XT/AT Compatible";
        break;
    case (u8)SMBIOS::PortType::Parallel_Port_PS2:
        port_type = "Parallel Port PS/2";
        break;
    case (u8)SMBIOS::PortType::Parallel_Port_ECP:
        port_type = "Parallel Port ECP";
        break;
    case (u8)SMBIOS::PortType::Parallel_Port_EPP:
        port_type = "Parallel Port EPP";
        break;
    case (u8)SMBIOS::PortType::Parallel_Port_ECP_EPP:
        port_type = "Parallel Port ECP/EPP";
        break;
    case (u8)SMBIOS::PortType::Serial_Port_XT_AT_Compatible:
        port_type = "Serial Port XT/AT Compatible";
        break;
    case (u8)SMBIOS::PortType::Serial_Port_16450_Compatible:
        port_type = "Serial Port 16450 Compatible";
        break;
    case (u8)SMBIOS::PortType::Serial_Port_16550_Compatible:
        port_type = "Serial Port 16550 Compatible";
        break;
    case (u8)SMBIOS::PortType::Serial_Port_16550A_Compatible:
        port_type = "Serial Port 16550A Compatible";
        break;
    case (u8)SMBIOS::PortType::SCSI_Port:
        port_type = "SCSI Port";
        break;
    case (u8)SMBIOS::PortType::MIDI_Port:
        port_type = "MIDI Port";
        break;
    case (u8)SMBIOS::PortType::Joy_Stick_Port:
        port_type = "Joy Stick Port";
        break;
    case (u8)SMBIOS::PortType::Keyboard_Port:
        port_type = "Keyboard Port";
        break;
    case (u8)SMBIOS::PortType::Mouse_Port:
        port_type = "Mouse Port";
        break;
    case (u8)SMBIOS::PortType::SSA_SCSI:
        port_type = "SSA SCSI";
        break;
    case (u8)SMBIOS::PortType::USB:
        port_type = "USB";
        break;
    case (u8)SMBIOS::PortType::FireWire:
        port_type = "FireWire (IEEE P1394)";
        break;
    case (u8)SMBIOS::PortType::PCMCIA_Type1:
        port_type = "PCMCIA Type 1";
        break;
    case (u8)SMBIOS::PortType::PCMCIA_Type2:
        port_type = "PCMCIA Type 2";
        break;
    case (u8)SMBIOS::PortType::PCMCIA_Type3:
        port_type = "PCMCIA Type 3";
        break;
    case (u8)SMBIOS::PortType::Cardbus:
        port_type = "Cardbus";
        break;
    case (u8)SMBIOS::PortType::AccessBus_Port:
        port_type = "Access Bus Port";
        break;
    case (u8)SMBIOS::PortType::SCSI_2:
        port_type = "SCSI II";
        break;
    case (u8)SMBIOS::PortType::SCSI_Wide:
        port_type = "SCSI Wide";
        break;
    case (u8)SMBIOS::PortType::PC98:
        port_type = "PC-98";
        break;
    case (u8)SMBIOS::PortType::PC98_Hireso:
        port_type = "PC-98-Hireso";
        break;
    case (u8)SMBIOS::PortType::PC_H98:
        port_type = "PC-H98";
        break;
    case (u8)SMBIOS::PortType::Video_Port:
        port_type = "Video Port";
        break;
    case (u8)SMBIOS::PortType::Audio_Port:
        port_type = "Audio Port";
        break;
    case (u8)SMBIOS::PortType::Modem_Port:
        port_type = "Modem Port";
        break;
    case (u8)SMBIOS::PortType::Network_Port:
        port_type = "Network Port";
        break;
    case (u8)SMBIOS::PortType::SATA:
        port_type = "SATA";
        break;
    case (u8)SMBIOS::PortType::SAS:
        port_type = "SAS";
        break;
    case (u8)SMBIOS::PortType::MFDP:
        port_type = "MFDP (Multi-Function Display Port)";
        break;
    case (u8)SMBIOS::PortType::Thunderbolt:
        port_type = "Thunderbolt";
        break;
    case (u8)SMBIOS::PortType::Intel_8251_Compatible:
        port_type = "8251 Compatible";
        break;
    case (u8)SMBIOS::PortType::Intel_8251_FIFO_Compatible:
        port_type = "8251 FIFO Compatible";
        break;
    case (u8)SMBIOS::PortType::Other:
        port_type = "Other";
        break;
    default:
        port_type = "Unknown";
        break;
    }
    tab() << "Port Type: " << port_type;
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
        case (u8)SMBIOS::TableType::SysEnclosure:
            parse_table_type3((SMBIOS::SysEnclosure&)table);
            break;
        case (u8)SMBIOS::TableType::ProcessorInfo:
            parse_table_type4((SMBIOS::ProcessorInfo&)table);
            break;
        case (u8)SMBIOS::TableType::CacheInfo:
            parse_table_type7((SMBIOS::CacheInfo&)table);
            break;
        case (u8)SMBIOS::TableType::PortConnectorInfo:
            parse_table_type8((SMBIOS::PortConnectorInfo&)table);
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
