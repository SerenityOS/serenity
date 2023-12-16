/*
 * Copyright (c) 2021, Itamar S. <itamar8910@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ProcessInspector.h"
#include "DebugInfo.h"

namespace Debug {

LoadedLibrary const* ProcessInspector::library_at(FlatPtr address) const
{
    LoadedLibrary const* result = nullptr;
    for_each_loaded_library([&result, address](auto const& lib) {
        if (address >= lib.base_address && address < lib.base_address + lib.debug_info->elf().size()) {
            result = &lib;
            return IterationDecision::Break;
        }
        return IterationDecision::Continue;
    });
    return result;
}

Optional<ProcessInspector::SymbolicationResult> ProcessInspector::symbolicate(FlatPtr address) const
{
    auto* lib = library_at(address);
    if (!lib)
        return {};
    // FIXME: ELF::Image symbolicate() API should return ByteString::empty() if symbol is not found (It currently returns ??)
    auto symbol = lib->debug_info->elf().symbolicate(address - lib->base_address);
    return { { lib->name, symbol } };
}

Optional<DebugInfo::SourcePositionAndAddress> ProcessInspector::get_address_from_source_position(ByteString const& file, size_t line) const
{
    Optional<DebugInfo::SourcePositionAndAddress> result;
    for_each_loaded_library([file, line, &result](auto& lib) {
        // The loader contains its own definitions for LibC symbols, so we don't want to include it in the search.
        if (lib.name == "Loader.so")
            return IterationDecision::Continue;

        auto source_position_and_address = lib.debug_info->get_address_from_source_position(file, line);
        if (!source_position_and_address.has_value())
            return IterationDecision::Continue;

        result = source_position_and_address;
        result.value().address += lib.base_address;
        return IterationDecision::Break;
    });
    return result;
}

Optional<DebugInfo::SourcePosition> ProcessInspector::get_source_position(FlatPtr address) const
{
    auto* lib = library_at(address);
    if (!lib)
        return {};
    return lib->debug_info->get_source_position(address - lib->base_address);
}

}
