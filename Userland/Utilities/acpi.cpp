/*
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibCore/System.h>
#include <stdio.h>
#include <unistd.h>

// https://uefi.org/sites/default/files/resources/ACPI_Spec_6_4_Jan22.pdf#page=193
struct [[gnu::packed]] SDTHeader {
    char sig[4];
    u32 length;
    u8 revision;
    u8 checksum;
    char oem_id[6];
    char oem_table_id[8];
    u32 oem_revision;
    u32 creator_id;
    u32 creator_revision;
};
static_assert(AssertSize<SDTHeader, 36>());

// https://uefi.org/sites/default/files/resources/ACPI_Spec_6_4_Jan22.pdf#page=1020
static void pkg_length(AK::Detail::ByteBuffer<32> const& data, u32& next_in_block, u32& next_block)
{
    u32 pos = next_block;

    outln("");
    out("data: ");
    if (pos > 3) {
        for (auto i = -3; i < 0; i++)
            out("{:#02x} ", data[pos + i]);
    } else
        out("               ");
    out("  ");
    for (auto i = 0; i < 5; i++)
        out("{:#02x} ", data[pos + i]);
    outln("");

    u32 length = 0;
    u32 delta = 0;
    int const size = data[pos + 1] >> 6;
    if (size == 0) {
        length = data[pos + 1] & 0x3f;
        delta = 1;
    } else if (size == 1) {
        length = data[pos + 1] & 0x0f;
        length = length | (data[pos + 2] << 4);
        delta = 2;
    } else if (size == 2) {
        length = data[pos + 1] & 0x0f;
        length = length | (data[pos + 2] << 4);
        length = length | (data[pos + 3] << 12);
        delta = 3;
    } else {
        length = data[pos + 1] & 0x0f;
        length = length | (data[pos + 2] << 4);
        length = length | (data[pos + 3] << 12);
        length = length | (data[pos + 4] << 20);
        delta = 4;
    }
    length -= (delta - 1);

    outln("pos: {} {} {}", delta, pos + delta + length, data.size());
    next_in_block = next_block + 1 + delta;
    next_block = next_block + delta + length;
}

static bool control_checksum(SDTHeader const* const header, AK::Detail::ByteBuffer<32> const& data)
{
    // The sum of all bytes of the table should be zero
    u8 checksum = 0;
    u8 const* const h = reinterpret_cast<u8 const*>(header);
    for (unsigned int i = 0; i < sizeof(SDTHeader); i++)
        checksum += h[i];
    for (unsigned int i = 0; i < (header->length - sizeof(SDTHeader)); i++)
        checksum += data[i];
    return !checksum;
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio rpath"));

    StringView dsdt_file_name = "/sys/firmware/acpi/DSDT"sv;

    Core::ArgsParser args_parser;
    args_parser.add_positional_argument(dsdt_file_name, "Name of DSDT table", "DSDT table", Core::ArgsParser::Required::No);
    args_parser.parse(arguments);

    auto dsdt_file = TRY(Core::File::open_file_or_standard_stream(dsdt_file_name, Core::File::OpenMode::Read));

    TRY(Core::System::pledge("stdio"));

    Array<u8, sizeof(SDTHeader)> header_buffer;
    Bytes header_span = header_buffer.span();
    TRY(dsdt_file->read_some(header_span));
    if (header_span.size() != sizeof(SDTHeader)) {
        warnln("Failed to read SDTHeader from {}", dsdt_file_name);
        return EXIT_FAILURE;
    }

    SDTHeader const* const header = reinterpret_cast<SDTHeader*>(header_buffer.data());

    StringView signature(&(header->sig[0]), 4l);
    if (signature != "DSDT"sv) {
        warnln("Unknown signature ({}) in file {}", signature, dsdt_file_name);
        return EXIT_FAILURE;
    }

    auto data = TRY(dsdt_file->read_until_eof());

    if ((header->length - sizeof(SDTHeader)) != data.size()) {
        warnln("Bad data size, should be {} but is {}", header->length - sizeof(SDTHeader), data.size());
        return EXIT_FAILURE;
    }

    if (!control_checksum(header, data)) {
        warnln("bad checksum in {}", dsdt_file_name);
        return EXIT_FAILURE;
    }

    u32 next_in_block = 0;
    u32 next_block = 0;

    while (next_block < data.size()) {
        switch (data[next_block]) {
        case 0x10: // ScopeOp
            pkg_length(data, next_in_block, next_block);
            break;
        default:
            outln("Unknown Opcode: {:#02x}", data[next_block]);
            return EXIT_FAILURE;
        }
    }
    return EXIT_SUCCESS;
}
