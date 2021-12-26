/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/JsonArray.h>
#include <AK/JsonObject.h>
#include <AK/JsonValue.h>
#include <AK/MappedFile.h>
#include <LibCore/File.h>
#include <LibDebug/DebugInfo.h>
#include <LibSymbolication/Symbolication.h>

namespace Symbolication {

struct CachedELF {
    NonnullRefPtr<MappedFile> mapped_file;
    NonnullOwnPtr<Debug::DebugInfo> debug_info;
};

static HashMap<String, OwnPtr<CachedELF>> s_cache;

Optional<Symbol> symbolicate(String const& path, u32 address)
{
    if (!s_cache.contains(path)) {
        auto mapped_file = MappedFile::map(path);
        if (mapped_file.is_error()) {
            dbgln("Failed to map {}: {}", path, mapped_file.error().string());
            s_cache.set(path, {});
            return {};
        }
        auto elf = make<ELF::Image>(mapped_file.value()->bytes());
        if (!elf->is_valid()) {
            dbgln("ELF not valid: {}", path);
            s_cache.set(path, {});
            {};
        }
        auto cached_elf = make<CachedELF>(mapped_file.release_value(), make<Debug::DebugInfo>(move(elf)));
        s_cache.set(path, move(cached_elf));
    }

    auto it = s_cache.find(path);
    VERIFY(it != s_cache.end());
    auto& cached_elf = it->value;

    if (!cached_elf)
        return {};

    u32 offset = 0;
    auto symbol = cached_elf->debug_info->elf().symbolicate(address, &offset);
    auto source_position_with_inlines = cached_elf->debug_info->get_source_position_with_inlines(address);

    Vector<Debug::DebugInfo::SourcePosition> positions;
    for (auto& position : source_position_with_inlines.inline_chain) {
        if (!positions.contains_slow(position))
            positions.append(position);
    }

    if (source_position_with_inlines.source_position.has_value() && !positions.contains_slow(source_position_with_inlines.source_position.value())) {
        positions.insert(0, source_position_with_inlines.source_position.value());
    }

    return Symbol {
        .address = address,
        .name = move(symbol),
        .offset = offset,
        .source_positions = move(positions),
    };
}

Vector<Symbol> symbolicate_thread(pid_t pid, pid_t tid)
{
    struct RegionWithSymbols {
        FlatPtr base { 0 };
        size_t size { 0 };
        String path;
        bool is_relative { true };
    };

    Vector<FlatPtr> stack;
    Vector<RegionWithSymbols> regions;

    regions.append(RegionWithSymbols {
        .base = 0xc0000000,
        .size = 0x3fffffff,
        .path = "/boot/Kernel",
        .is_relative = false });

    {
        auto stack_path = String::formatted("/proc/{}/stacks/{}", pid, tid);
        auto file_or_error = Core::File::open(stack_path, Core::OpenMode::ReadOnly);
        if (file_or_error.is_error()) {
            warnln("Could not open {}: {}", stack_path, file_or_error.error());
            return {};
        }

        auto json = JsonValue::from_string(file_or_error.value()->read_all());
        if (!json.has_value() || !json.value().is_array()) {
            warnln("Invalid contents in {}", stack_path);
            return {};
        }

        stack.ensure_capacity(json.value().as_array().size());
        for (auto& value : json.value().as_array().values()) {
            stack.append(value.to_u32());
        }
    }

    {
        auto vm_path = String::formatted("/proc/{}/vm", pid);
        auto file_or_error = Core::File::open(vm_path, Core::OpenMode::ReadOnly);
        if (file_or_error.is_error()) {
            warnln("Could not open {}: {}", vm_path, file_or_error.error());
            return {};
        }

        auto json = JsonValue::from_string(file_or_error.value()->read_all());
        if (!json.has_value() || !json.value().is_array()) {
            warnln("Invalid contents in {}", vm_path);
            return {};
        }

        for (auto& region_value : json.value().as_array().values()) {
            auto& region = region_value.as_object();
            auto name = region.get("name").to_string();
            auto address = region.get("address").to_u32();
            auto size = region.get("size").to_u32();

            String path;
            if (name == "/usr/lib/Loader.so") {
                path = name;
            } else if (name.ends_with(": .text")) {
                auto parts = name.split_view(':');
                path = parts[0];
                if (!path.starts_with('/'))
                    path = String::formatted("/usr/lib/{}", path);
            } else {
                continue;
            }

            RegionWithSymbols r;
            r.base = address;
            r.size = size;
            r.path = path;
            regions.append(move(r));
        }
    }

    Vector<Symbol> symbols;
    bool first_frame = true;

    for (auto address : stack) {
        const RegionWithSymbols* found_region = nullptr;
        for (auto& region : regions) {
            if (address >= region.base && address < (region.base + region.size)) {
                found_region = &region;
                break;
            }
        }

        if (!found_region) {
            outln("{:p}  ??", address);
            continue;
        }

        FlatPtr adjusted_address;
        if (found_region->is_relative)
            adjusted_address = address - found_region->base;
        else
            adjusted_address = address;

        // We're subtracting 1 from the address because this is the return address,
        // i.e. it is one instruction past the call instruction.
        // However, because the first frame represents the current
        // instruction pointer rather than the return address we don't
        // subtract 1 for that.
        auto result = symbolicate(found_region->path, adjusted_address - (first_frame ? 0 : 1));
        first_frame = false;
        if (!result.has_value()) {
            symbols.append(Symbol {
                .address = address,
                .source_positions = {},
            });
            continue;
        }

        symbols.append(result.value());
    }
    return symbols;
}

}
