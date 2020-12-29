/*
 * Copyright (c) 2020, Linus Groh <mail@linusgroh.de>
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

#include <AK/LexicalPath.h>
#include <AK/MappedFile.h>
#include <AK/StringBuilder.h>
#include <AK/Types.h>
#include <LibCore/File.h>
#include <LibCoreDump/Backtrace.h>
#include <LibCoreDump/Reader.h>
#include <LibELF/CoreDump.h>
#include <LibELF/Image.h>

namespace CoreDump {

// FIXME: This cache has to be invalidated when libraries/programs are re-compiled.
// We can store the last-modified timestamp of the elf files in ELFObjectInfo to invalidate cache entries.
static HashMap<String, NonnullOwnPtr<ELFObjectInfo>> s_debug_info_cache;

static const ELFObjectInfo* object_info_for_region(const ELF::Core::MemoryRegionInfo& region)
{
    auto name = region.object_name();

    String path;
    if (name.contains(".so"))
        path = String::formatted("/usr/lib/{}", name);
    else {
        path = name;
    }

    if (auto it = s_debug_info_cache.find(path); it != s_debug_info_cache.end())
        return it->value.ptr();

    if (!Core::File::exists(path.characters()))
        return nullptr;

    MappedFile object_file(path);
    if (!object_file.is_valid())
        return nullptr;

    auto info = make<ELFObjectInfo>(move(object_file), Debug::DebugInfo { make<ELF::Image>((const u8*)object_file.data(), object_file.size()) });
    auto* info_ptr = info.ptr();
    s_debug_info_cache.set(path, move(info));
    return info_ptr;
}

Backtrace::Backtrace(const Reader& coredump)
{
    coredump.for_each_thread_info([this, &coredump](const ELF::Core::ThreadInfo& thread_info) {
        uint32_t* ebp = (uint32_t*)thread_info.regs.ebp;
        uint32_t* eip = (uint32_t*)thread_info.regs.eip;
        while (ebp && eip) {
            add_backtrace_entry(coredump, (FlatPtr)eip);
            auto next_eip = coredump.peek_memory((FlatPtr)(ebp + 1));
            auto next_ebp = coredump.peek_memory((FlatPtr)(ebp));
            if (!next_eip.has_value() || !next_ebp.has_value())
                break;
            eip = (uint32_t*)next_eip.value();
            ebp = (uint32_t*)next_ebp.value();
        }
        return IterationDecision::Continue;
    });
}

Backtrace::~Backtrace()
{
}

void Backtrace::add_backtrace_entry(const Reader& coredump, FlatPtr eip)
{
    auto* region = coredump.region_containing((FlatPtr)eip);
    if (!region) {
        m_entries.append({ eip, {}, {}, {} });
        return;
    }
    auto object_name = region->object_name();
    if (object_name == "Loader.so")
        return;
    auto* object_info = object_info_for_region(*region);
    if (!object_info)
        return;
    auto function_name = object_info->debug_info.elf().symbolicate(eip - region->region_start);
    auto source_position = object_info->debug_info.get_source_position(eip - region->region_start);
    m_entries.append({ eip, object_name, function_name, source_position });
}

String Backtrace::Entry::to_string(bool color) const
{
    StringBuilder builder;
    builder.appendff("{:p}: ", eip);
    if (object_name.is_empty()) {
        builder.append("???");
        return builder.build();
    }
    builder.appendff("[{}] {}", object_name, function_name.is_empty() ? "???" : function_name);
    if (source_position.has_value()) {
        auto& source_position = this->source_position.value();
        auto fmt = color ? " (\033[34;1m{}\033[0m:{})" : " ({}:{})";
        builder.appendff(fmt, LexicalPath(source_position.file_path).basename(), source_position.line_number);
    }
    return builder.build();
}

}
