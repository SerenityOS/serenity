/*
 * Copyright (c) 2020, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
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

    auto file_or_error = MappedFile::map(path);
    if (file_or_error.is_error())
        return nullptr;

    auto image = make<ELF::Image>(file_or_error.value()->bytes());
    auto info = make<ELFObjectInfo>(file_or_error.release_value(), Debug::DebugInfo { move(image) });
    auto* info_ptr = info.ptr();
    s_debug_info_cache.set(path, move(info));
    return info_ptr;
}

Backtrace::Backtrace(const Reader& coredump, const ELF::Core::ThreadInfo& thread_info)
    : m_thread_info(move(thread_info))
{
    uint32_t* ebp = (uint32_t*)m_thread_info.regs.ebp;
    uint32_t* eip = (uint32_t*)m_thread_info.regs.eip;
    while (ebp && eip) {
        add_entry(coredump, (FlatPtr)eip);
        auto next_eip = coredump.peek_memory((FlatPtr)(ebp + 1));
        auto next_ebp = coredump.peek_memory((FlatPtr)(ebp));
        if (!next_eip.has_value() || !next_ebp.has_value())
            break;
        eip = (uint32_t*)next_eip.value();
        ebp = (uint32_t*)next_ebp.value();
    }
}

Backtrace::~Backtrace()
{
}

void Backtrace::add_entry(const Reader& coredump, FlatPtr eip)
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
