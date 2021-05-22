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
    Debug::DebugInfo debug_info;
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
        Debug::DebugInfo debug_info(move(elf));
        auto cached_elf = make<CachedELF>(mapped_file.release_value(), move(debug_info));
        s_cache.set(path, move(cached_elf));
    }

    auto it = s_cache.find(path);
    VERIFY(it != s_cache.end());
    auto& cached_elf = it->value;

    if (!cached_elf)
        return {};

    u32 offset = 0;
    auto symbol = cached_elf->debug_info.elf().symbolicate(address, &offset);
    auto source_position = cached_elf->debug_info.get_source_position(address);
    String filename;
    u32 line_number = 0;
    if (source_position.has_value()) {
        filename = source_position.value().file_path;
        line_number = source_position.value().line_number;
    }

    return Symbol {
        .address = address,
        .name = move(symbol),
        .offset = offset,
        .filename = move(filename),
        .line_number = line_number
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

        auto result = symbolicate(found_region->path, adjusted_address);
        if (!result.has_value()) {
            symbols.append(Symbol {
                .address = address,
            });
            continue;
        }

        symbols.append(result.value());
    }
    return symbols;
}

}
