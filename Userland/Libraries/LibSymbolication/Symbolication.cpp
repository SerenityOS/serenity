/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <YAK/Checked.h>
#include <YAK/JsonArray.h>
#include <YAK/JsonObject.h>
#include <YAK/JsonValue.h>
#include <YAK/MappedFile.h>
#include <LibCore/File.h>
#include <LibDebug/DebugInfo.h>
#include <LibSymbolication/Symbolication.h>

namespace Symbolication {

struct CachedELF {
    NonnullRefPtr<MappedFile> mapped_file;
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

Optional<Symbol> symbolicate(String const& path, FlatPtr address)
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
        auto cached_elf = make<CachedELF>(mapped_file.release_value(), make<Debug::DebugInfo>(*elf), move(elf));
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
        if (!json.has_value() || !json.value().is_array()) {
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
        if (!json.has_value() || !json.value().is_array()) {
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

        FlatPtr adjusted_address = address - found_region->base;

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
