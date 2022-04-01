/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Array.h>
#include <AK/Checked.h>
#include <AK/JsonArray.h>
#include <AK/JsonObject.h>
#include <AK/JsonValue.h>
#include <AK/LexicalPath.h>
#include <LibCore/File.h>
#include <LibCore/MappedFile.h>
#include <LibDebug/DebugInfo.h>
#include <LibSymbolication/Symbolication.h>

namespace Symbolication {

struct CachedELF {
    NonnullRefPtr<Core::MappedFile> mapped_file;
    NonnullOwnPtr<Debug::DebugInfo> debug_info;
    NonnullOwnPtr<ELF::Image> image;
};

static HashMap<String, OwnPtr<CachedELF>> s_cache;

enum class KernelBaseState {
    Uninitialized,
    Valid,
    Invalid,
};

static FlatPtr s_kernel_base;
static KernelBaseState s_kernel_base_state = KernelBaseState::Uninitialized;

Optional<FlatPtr> kernel_base()
{
    if (s_kernel_base_state == KernelBaseState::Uninitialized) {
        auto file = Core::File::open("/proc/kernel_base", Core::OpenMode::ReadOnly);
        if (file.is_error()) {
            s_kernel_base_state = KernelBaseState::Invalid;
            return {};
        }
        auto kernel_base_str = String { file.value()->read_all(), NoChomp };
#if ARCH(I386)
        using AddressType = u32;
#else
        using AddressType = u64;
#endif
        auto maybe_kernel_base = kernel_base_str.to_uint<AddressType>();
        if (!maybe_kernel_base.has_value()) {
            s_kernel_base_state = KernelBaseState::Invalid;
            return {};
        }
        s_kernel_base = maybe_kernel_base.value();
        s_kernel_base_state = KernelBaseState::Valid;
    }
    if (s_kernel_base_state == KernelBaseState::Invalid)
        return {};
    return s_kernel_base;
}

Optional<Symbol> symbolicate(String const& path, FlatPtr address, IncludeSourcePosition include_source_positions)
{
    String full_path = path;
    if (!path.starts_with('/')) {
        Array<StringView, 2> search_paths { "/usr/lib"sv, "/usr/local/lib"sv };
        bool found = false;
        for (auto& search_path : search_paths) {
            full_path = LexicalPath::join(search_path, path).string();
            if (Core::File::exists(full_path)) {
                found = true;
                break;
            }
        }
        if (!found) {
            dbgln("Failed to find candidate for {}", path);
            s_cache.set(path, {});
            return {};
        }
    }
    if (!s_cache.contains(full_path)) {
        auto mapped_file = Core::MappedFile::map(full_path);
        if (mapped_file.is_error()) {
            dbgln("Failed to map {}: {}", full_path, mapped_file.error());
            s_cache.set(full_path, {});
            return {};
        }
        auto elf = make<ELF::Image>(mapped_file.value()->bytes());
        if (!elf->is_valid()) {
            dbgln("ELF not valid: {}", full_path);
            s_cache.set(full_path, {});
            return {};
        }
        auto cached_elf = make<CachedELF>(mapped_file.release_value(), make<Debug::DebugInfo>(*elf), move(elf));
        s_cache.set(full_path, move(cached_elf));
    }

    auto it = s_cache.find(full_path);
    VERIFY(it != s_cache.end());
    auto& cached_elf = it->value;

    if (!cached_elf)
        return {};

    u32 offset = 0;
    auto symbol = cached_elf->debug_info->elf().symbolicate(address, &offset);

    Vector<Debug::DebugInfo::SourcePosition> positions;
    if (include_source_positions == IncludeSourcePosition::Yes) {
        auto source_position_with_inlines = cached_elf->debug_info->get_source_position_with_inlines(address);

        for (auto& position : source_position_with_inlines.inline_chain) {
            if (!positions.contains_slow(position))
                positions.append(position);
        }

        if (source_position_with_inlines.source_position.has_value() && !positions.contains_slow(source_position_with_inlines.source_position.value())) {
            positions.insert(0, source_position_with_inlines.source_position.value());
        }
    }

    return Symbol {
        .address = address,
        .name = move(symbol),
        .object = LexicalPath::basename(path),
        .offset = offset,
        .source_positions = move(positions),
    };
}

Vector<Symbol> symbolicate_thread(pid_t pid, pid_t tid, IncludeSourcePosition include_source_positions)
{
    struct RegionWithSymbols {
        FlatPtr base { 0 };
        size_t size { 0 };
        String path;
    };

    Vector<FlatPtr> stack;
    Vector<RegionWithSymbols> regions;

    if (auto maybe_kernel_base = kernel_base(); maybe_kernel_base.has_value()) {
        regions.append(RegionWithSymbols {
            .base = maybe_kernel_base.value(),
            .size = 0x3fffffff,
            .path = "/boot/Kernel.debug",
        });
    }

    {
        auto stack_path = String::formatted("/proc/{}/stacks/{}", pid, tid);
        auto file_or_error = Core::File::open(stack_path, Core::OpenMode::ReadOnly);
        if (file_or_error.is_error()) {
            warnln("Could not open {}: {}", stack_path, file_or_error.error());
            return {};
        }

        auto json = JsonValue::from_string(file_or_error.value()->read_all());
        if (json.is_error() || !json.value().is_array()) {
            warnln("Invalid contents in {}", stack_path);
            return {};
        }

        stack.ensure_capacity(json.value().as_array().size());
        for (auto& value : json.value().as_array().values()) {
            stack.append(value.to_addr());
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
        if (json.is_error() || !json.value().is_array()) {
            warnln("Invalid contents in {}", vm_path);
            return {};
        }

        for (auto& region_value : json.value().as_array().values()) {
            auto& region = region_value.as_object();
            auto name = region.get("name").to_string();
            auto address = region.get("address").to_addr();
            auto size = region.get("size").to_addr();

            String path;
            if (name == "/usr/lib/Loader.so") {
                path = name;
            } else if (name.ends_with(": .text") || name.ends_with(": .rodata")) {
                auto parts = name.split_view(':');
                path = parts[0];
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
        RegionWithSymbols const* found_region = nullptr;
        for (auto& region : regions) {
            FlatPtr region_end;
            if (Checked<FlatPtr>::addition_would_overflow(region.base, region.size))
                region_end = NumericLimits<FlatPtr>::max();
            else
                region_end = region.base + region.size;
            if (address >= region.base && address < region_end) {
                found_region = &region;
                break;
            }
        }

        if (!found_region) {
            outln("{:p}  ??", address);
            continue;
        }

        // We found an address inside of a region, but the base of that region
        // may not be the base of the ELF image. For example, there could be an
        // .rodata mapping at a lower address than the first .text mapping from
        // the same image. look for the lowest address region with the same path.
        RegionWithSymbols const* base_region = nullptr;
        for (auto& region : regions) {
            if (region.path != found_region->path)
                continue;
            if (!base_region || region.base <= base_region->base)
                base_region = &region;
        }

        FlatPtr adjusted_address = address - base_region->base;

        // We're subtracting 1 from the address because this is the return address,
        // i.e. it is one instruction past the call instruction.
        // However, because the first frame represents the current
        // instruction pointer rather than the return address we don't
        // subtract 1 for that.
        auto result = symbolicate(found_region->path, adjusted_address - (first_frame ? 0 : 1), include_source_positions);
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
