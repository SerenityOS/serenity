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

bool parse_data(ByteStream data)
{
    size_t remaining_table_length = smbios_data_payload_size;
    ByteStream current_table = data;
    while (remaining_table_length > 0) {
        auto& table = *(SMBIOS::TableHeader*)(current_table);
        size_t table_size = SMBIOS::Parsing::calculate_full_table_size(table);
        parse_table_header(table);
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
