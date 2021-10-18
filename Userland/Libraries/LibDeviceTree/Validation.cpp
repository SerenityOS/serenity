/*
 * Copyright (c) 2021, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Endian.h>
#include <AK/Format.h>
#include <AK/String.h>
#include <LibDeviceTree/Validation.h>

namespace DeviceTree {

bool validate_flattened_device_tree(FlattenedDeviceTreeHeader const& header, u8 const* blob_start, size_t blob_size, Verbose verbose)
{
    if (header.magic != 0xD00DFEEDU) {
        if (verbose == Verbose::Yes)
            warnln("FDT Header has invalid magic value 0x{:08x}. Are you sure it's a flattened device tree?", header.magic);
        return false;
    }

    if ((header.off_mem_rsvmap & ~0x7) != header.off_mem_rsvmap) {
        if (verbose == Verbose::Yes)
            warnln("FDT Header's MemoryReservationBlock is not 8 byte aligned! Offset: 0x{:08x}", header.off_mem_rsvmap);
        return false;
    }

    if ((header.off_dt_struct & ~0x3) != header.off_dt_struct) {
        if (verbose == Verbose::Yes)
            warnln("FDT Header's StructureBlock is not 4 byte aligned! Offset: 0x{:08x}", header.off_dt_struct);
        return false;
    }

    if (header.totalsize != blob_size) {
        if (verbose == Verbose::Yes)
            warnln("FDT Header total size mismatch: {}, expected {}!", header.totalsize, blob_size);
        return false;
    }

    if (header.off_dt_struct > blob_size) {
        if (verbose == Verbose::Yes)
            warnln("FDT Header reports larger StructureBlock offset than possible: {} but total size is {}!", header.off_dt_struct, blob_size);
        return false;
    }

    if (header.off_dt_strings > blob_size) {
        if (verbose == Verbose::Yes)
            warnln("FDT Header reports larger StringsBlock offset than possible: {} but total size is {}!", header.off_dt_strings, blob_size);
        return false;
    }

    if (header.off_mem_rsvmap > blob_size) {
        if (verbose == Verbose::Yes)
            warnln("FDT Header reports larger MemoryReservationBlock offset than possible: {} but total size is {}!", header.off_mem_rsvmap, blob_size);
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

    auto* mem_reserve_block = reinterpret_cast<FlattenedDeviceTreeReserveEntry const*>(&blob_start[header.off_mem_rsvmap]);
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
    if ((structure_block_size > header.off_dt_strings) || (structure_block_size > blob_size)) {
        if (verbose == Verbose::Yes)
            warnln("FDT Header reports invalid StructureBlock block size: {} is too large given StringsBlock offset {} and total size {}", structure_block_size, header.off_dt_strings, blob_size);
        return false;
    }

    u64 strings_block_size = header.off_dt_strings + header.size_dt_strings;
    if (strings_block_size > blob_size) {
        if (verbose == Verbose::Yes)
            warnln("FDT Header reports invalid StringsBlock size: {} is too large given total size {}", structure_block_size, blob_size);
        return false;
    }

    return true;
}

bool dump(FlattenedDeviceTreeHeader const& header, u8 const* blob_start, size_t blob_size)
{
    outln("/dts-v1/;");
    outln("// magic:               0x{:08x}", header.magic);
    outln("// totalsize:           0x{:08x} ({})", header.totalsize, header.totalsize);
    outln("// off_dt_struct:       0x{:08x}", header.off_dt_struct);
    outln("// off_dt_strings:      0x{:08x}", header.off_dt_strings);
    outln("// off_mem_rsvmap:      0x{:08x}", header.off_mem_rsvmap);
    outln("// version:             0x{:08x}", header.version);
    outln("// last_comp_version:   0x{:08x}", header.last_comp_version);
    outln("// boot_cpuid_phys:     0x{:08x}", header.boot_cpuid_phys);
    outln("// size_dt_strings:     0x{:08x}", header.size_dt_strings);
    outln("// size_dt_struct:      0x{:08x}", header.size_dt_struct);

    if (!validate_flattened_device_tree(header, blob_start, blob_size, Verbose::Yes))
        return false;

    // Now that we know the device tree is valid, print out the rest of the information
    auto* mem_reserve_block = reinterpret_cast<FlattenedDeviceTreeReserveEntry const*>(&blob_start[header.off_mem_rsvmap]);
    u64 next_block_offset = header.off_mem_rsvmap + sizeof(FlattenedDeviceTreeReserveEntry);
    while ((next_block_offset < header.off_dt_struct) && (*mem_reserve_block != FlattenedDeviceTreeReserveEntry {})) {
        outln("/memreserve/ 0x{:08x} 0x{:08x};", mem_reserve_block->address, mem_reserve_block->size);
        ++mem_reserve_block;
        next_block_offset += sizeof(FlattenedDeviceTreeReserveEntry);
    }

    return true;
}

} // namespace DeviceTree
