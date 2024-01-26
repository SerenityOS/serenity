/*
 * Copyright (c) 2021-2023, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/AllOf.h>
#include <AK/CharacterTypes.h>
#include <AK/Endian.h>
#include <AK/Format.h>
#include <AK/MemoryStream.h>
#include <AK/StringBuilder.h>
#include <AK/Try.h>
#include <AK/Vector.h>
#include <LibDeviceTree/Validation.h>

namespace DeviceTree {

#ifdef KERNEL
#    define warnln dbgln
#    define outln dbgln
#endif

bool validate_flattened_device_tree(FlattenedDeviceTreeHeader const& header, ReadonlyBytes raw_device_tree, Verbose verbose)
{
    if (header.magic != 0xD00DFEEDU) {
        if (verbose == Verbose::Yes)
            warnln("FDT Header has invalid magic value {:#08x}. Are you sure it's a flattened device tree?", header.magic);
        return false;
    }

    if ((header.off_mem_rsvmap & ~0x7) != header.off_mem_rsvmap) {
        if (verbose == Verbose::Yes)
            warnln("FDT Header's MemoryReservationBlock is not 8 byte aligned! Offset: {:#08x}", header.off_mem_rsvmap);
        return false;
    }

    if ((header.off_dt_struct & ~0x3) != header.off_dt_struct) {
        if (verbose == Verbose::Yes)
            warnln("FDT Header's StructureBlock is not 4 byte aligned! Offset: {:#08x}", header.off_dt_struct);
        return false;
    }

    if (header.totalsize != raw_device_tree.size()) {
        if (verbose == Verbose::Yes)
            warnln("FDT Header total size mismatch: {}, expected {}!", header.totalsize, raw_device_tree.size());
        return false;
    }

    if (header.off_dt_struct > raw_device_tree.size()) {
        if (verbose == Verbose::Yes)
            warnln("FDT Header reports larger StructureBlock offset than possible: {} but total size is {}!", header.off_dt_struct, raw_device_tree.size());
        return false;
    }

    if (header.off_dt_strings > raw_device_tree.size()) {
        if (verbose == Verbose::Yes)
            warnln("FDT Header reports larger StringsBlock offset than possible: {} but total size is {}!", header.off_dt_strings, raw_device_tree.size());
        return false;
    }

    if (header.off_mem_rsvmap > raw_device_tree.size()) {
        if (verbose == Verbose::Yes)
            warnln("FDT Header reports larger MemoryReservationBlock offset than possible: {} but total size is {}!", header.off_mem_rsvmap, raw_device_tree.size());
        return false;
    }

    // Verify format is correct. Header --> MemoryReservation --> Structures --> Strings
    if (header.off_dt_strings <= header.off_dt_struct) {
        if (verbose == Verbose::Yes)
            warnln("FDT Header has invalid StringsBlock offset {}, must be after v (@ {})", header.off_dt_strings, header.off_dt_struct);
        return false;
    }

    if (header.off_dt_struct <= header.off_mem_rsvmap) {
        if (verbose == Verbose::Yes)
            warnln("FDT Header has invalid StructureBlock offset {}, must be after MemoryReservationBlock (@ {})", header.off_dt_struct, header.off_mem_rsvmap);
        return false;
    }

    if (header.version != 17) {
        if (verbose == Verbose::Yes)
            warnln("Expected FDT header version 17, got {}", header.version);
        return false;
    }

    if (header.last_comp_version != 16) {
        if (verbose == Verbose::Yes)
            warnln("Expected FDT header last compatible version 16, got {}", header.last_comp_version);
        return false;
    }

    auto* mem_reserve_block = reinterpret_cast<FlattenedDeviceTreeReserveEntry const*>(&raw_device_tree[header.off_mem_rsvmap]);
    u64 next_block_offset = header.off_mem_rsvmap + sizeof(FlattenedDeviceTreeReserveEntry);
    while ((next_block_offset < header.off_dt_struct) && (*mem_reserve_block != FlattenedDeviceTreeReserveEntry {})) {
        ++mem_reserve_block;
        next_block_offset += sizeof(FlattenedDeviceTreeReserveEntry);
    }

    if (next_block_offset >= header.off_dt_strings) {
        if (verbose == Verbose::Yes)
            warnln("FDT malformed, MemoryReservationBlock spill into StructureBlock section!");
        return false;
    }

    // check for overlap. Overflow not possible b/c the fields are u32
    u64 structure_block_size = header.off_dt_struct + header.size_dt_struct;
    if ((structure_block_size > header.off_dt_strings) || (structure_block_size > raw_device_tree.size())) {
        if (verbose == Verbose::Yes)
            warnln("FDT Header reports invalid StructureBlock block size: {} is too large given StringsBlock offset {} and total size {}", structure_block_size, header.off_dt_strings, raw_device_tree.size());
        return false;
    }

    u64 strings_block_size = header.off_dt_strings + header.size_dt_strings;
    if (strings_block_size > raw_device_tree.size()) {
        if (verbose == Verbose::Yes)
            warnln("FDT Header reports invalid StringsBlock size: {} is too large given total size {}", strings_block_size, raw_device_tree.size());
        return false;
    }

    return true;
}

ErrorOr<void> dump(FlattenedDeviceTreeHeader const& header, ReadonlyBytes raw_device_tree)
{
    outln("/dts-v1/;");
    outln("// magic:             {:#08x}", header.magic);
    outln("// totalsize:         {:#08x} ({})", header.totalsize, header.totalsize);
    outln("// off_dt_struct:     {:#x}", header.off_dt_struct);
    outln("// off_dt_strings:    {:#x}", header.off_dt_strings);
    outln("// off_mem_rsvmap:    {:#x}", header.off_mem_rsvmap);
    outln("// version:           {}", header.version);
    outln("// last_comp_version: {}", header.last_comp_version);
    outln("// boot_cpuid_phys:   {:#x}", header.boot_cpuid_phys);
    outln("// size_dt_strings:   {:#x}", header.size_dt_strings);
    outln("// size_dt_struct:    {:#x}", header.size_dt_struct);

    if (!validate_flattened_device_tree(header, raw_device_tree, Verbose::Yes))
        return Error::from_errno(EINVAL);

    // Now that we know the device tree is valid, print out the rest of the information
    auto const* mem_reserve_block = reinterpret_cast<FlattenedDeviceTreeReserveEntry const*>(&raw_device_tree[header.off_mem_rsvmap]);
    u64 next_block_offset = header.off_mem_rsvmap + sizeof(FlattenedDeviceTreeReserveEntry);
    while ((next_block_offset < header.off_dt_struct) && (*mem_reserve_block != FlattenedDeviceTreeReserveEntry {})) {
        outln("/memreserve/ {:#08x} {:#08x};", mem_reserve_block->address, mem_reserve_block->size);
        ++mem_reserve_block;
        next_block_offset += sizeof(FlattenedDeviceTreeReserveEntry);
    }

    return dump_flattened_device_tree_structure(header, raw_device_tree);
}

ErrorOr<void> dump_flattened_device_tree_structure(FlattenedDeviceTreeHeader const& header, ReadonlyBytes raw_device_tree)
{
    u8 indent = 0;
    DeviceTreeCallbacks callbacks = {
        .on_node_begin = [&](StringView token_name) -> ErrorOr<IterationDecision> {
            // Root Entry:
            if (indent == 0)
                outln("/ {{");
            else
                outln("{: >{}}{} {{", ""sv, indent * 2, token_name);
            ++indent;
            return IterationDecision::Continue;
        },
        .on_node_end = [&](StringView) -> ErrorOr<IterationDecision> {
            --indent;
            outln("{: >{}}}};", ""sv, indent * 2);
            return IterationDecision::Continue;
        },
        .on_property = [&](StringView property_name, ReadonlyBytes property_value) -> ErrorOr<IterationDecision> {
            // Note: We want to figure out if the value is a string, a stringlist, a number or something unprintable.
            //     In reality, the entity retrieving the value needs to know if it's a u32, u64, string, stringlist, or "property-encoded-value" a priori
            bool had_valid_character = false;
            bool const is_print = all_of(property_value, [&had_valid_character](char c) -> bool {
                if (AK::is_ascii_printable(c)) {
                    had_valid_character = true;
                    return true;
                }
                if (had_valid_character) {
                    had_valid_character = false;
                    return c == 0;
                }
                return false;
            });
            if (property_value.size() == 0) {
                outln("{: >{}}{};", ""sv, indent * 2, property_name);
            } else if (is_print) {
                StringView property_as_string { property_value };
                StringBuilder property;
                TRY(property.try_appendff("{: >{}}{} = ", ""sv, indent * 2, property_name));
                TRY(property.try_join(", "sv, property_as_string.split_view('\00'), "\"{}\""sv));
                outln("{};", property.string_view());
            } else {
                StringBuilder property;
                if (property_value.size() % 4 != 0) {
                    // This is the best hint we can use, that we are given an array
                    // without looking at the schema of the current tree node
                    TRY(property.try_appendff("{: >{}}{} = [", ""sv, indent * 2, property_name));
                    TRY(property.try_join(' ', property_value, "{:02x}"sv));
                    outln("{}];", property.string_view());
                } else {
                    TRY(property.try_appendff("{: >{}}{} = <", ""sv, indent * 2, property_name));
                    auto value_stream = FixedMemoryStream(property_value);
                    bool first_value = true;
                    while (!value_stream.is_eof()) {
                        if (first_value)
                            property.appendff("{:#08x}", TRY(value_stream.read_value<BigEndian<u32>>()));
                        else
                            property.appendff(" {:#08x}", TRY(value_stream.read_value<BigEndian<u32>>()));
                        first_value = false;
                    }
                    outln("{}>;", property.string_view());
                }
            }
            return IterationDecision::Continue;
        },
        .on_noop = []() -> ErrorOr<IterationDecision> { return IterationDecision::Continue; },
        .on_end = []() -> ErrorOr<void> { return {}; }
    };

    return walk_device_tree(header, raw_device_tree, move(callbacks));
}
} // namespace DeviceTree
