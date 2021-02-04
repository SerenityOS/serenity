/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
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

#include <AK/JsonArray.h>
#include <AK/JsonObject.h>
#include <AK/JsonValue.h>
#include <AK/LogStream.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/EventLoop.h>
#include <LibCore/File.h>
#include <LibSymbolClient/Client.h>

int main(int argc, char** argv)
{
    if (pledge("stdio rpath unix fattr", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    if (unveil("/proc", "r") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil("/tmp/rpc", "crw") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil("/tmp/portal/symbol", "rw") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil(nullptr, nullptr) < 0) {
        perror("unveil");
        return 1;
    }

    Core::ArgsParser args_parser;
    pid_t pid = 0;
    args_parser.add_positional_argument(pid, "PID", "pid");

    args_parser.parse(argc, argv);

    struct RegionWithSymbols {
        FlatPtr base { 0 };
        size_t size { 0 };
        String path;
    };

    Vector<FlatPtr> stack;
    Vector<RegionWithSymbols> regions;

    {
        // FIXME: Support multiple threads in the same process!
        auto stack_path = String::formatted("/proc/{}/stacks/{}", pid, pid);
        auto file_or_error = Core::File::open(stack_path, Core::IODevice::ReadOnly);
        if (file_or_error.is_error()) {
            warnln("Could not open {}: {}", stack_path, file_or_error.error());
            return 1;
        }

        auto json = JsonValue::from_string(file_or_error.value()->read_all());
        if (!json.has_value() || !json.value().is_array()) {
            warnln("Invalid contents in {}", stack_path);
            return 1;
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
            return 1;
        }

        auto json = JsonValue::from_string(file_or_error.value()->read_all());
        if (!json.has_value() || !json.value().is_array()) {
            warnln("Invalid contents in {}", vm_path);
            return 1;
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

    Core::EventLoop loop;

    auto client = SymbolClient::Client::construct();

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

        Vector<FlatPtr> addresses;
        addresses.append(address - found_region->base);

        auto symbols = client->symbolicate(found_region->path, addresses);
        if (symbols.is_empty()) {
            outln("{:p}  ??", address);
            continue;
        }

        outln("{:p}  {}", address, symbols[0].name);
    }

    return 0;
}
