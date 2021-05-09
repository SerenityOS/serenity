/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/JsonArray.h>
#include <AK/JsonObject.h>
#include <AK/JsonValue.h>
#include <LibCore/File.h>
#include <LibSymbolClient/Client.h>

namespace SymbolClient {

Client::Client()
    : IPC::ServerConnection<SymbolClientEndpoint, SymbolServerEndpoint>(*this, "/tmp/portal/symbol")
{
    handshake();
}

void Client::handshake()
{
    greet();
}

void Client::dummy()
{
}

Optional<Symbol> Client::symbolicate(const String& path, FlatPtr address)
{
    auto response = IPCProxy::symbolicate(path, address);
    if (!response.success())
        return {};

    return Symbol {
        .address = address,
        .name = response.name(),
        .offset = response.offset(),
        .filename = response.filename(),
        .line_number = response.line()
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
        auto file_or_error = Core::File::open(stack_path, Core::IODevice::ReadOnly);
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
        auto file_or_error = Core::File::open(vm_path, Core::IODevice::ReadOnly);
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

    auto client = SymbolClient::Client::construct();

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

        auto result = client->symbolicate(found_region->path, adjusted_address);
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
