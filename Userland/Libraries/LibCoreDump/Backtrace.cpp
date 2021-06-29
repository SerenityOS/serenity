/*
 * Copyright (c) 2020, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/LexicalPath.h>
#include <AK/MappedFile.h>
#include <AK/Platform.h>
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
#if !ARCH(I386)
    // FIXME: Fix LibDebug
    return nullptr;
#endif
    auto info = make<ELFObjectInfo>(file_or_error.release_value(), make<Debug::DebugInfo>(move(image)));
    auto* info_ptr = info.ptr();
    s_debug_info_cache.set(path, move(info));
    return info_ptr;
}

Backtrace::Backtrace(const Reader& coredump, const ELF::Core::ThreadInfo& thread_info)
    : m_thread_info(move(thread_info))
{
    FlatPtr* bp;
    FlatPtr* ip;
#if ARCH(I386)
    bp = (FlatPtr*)m_thread_info.regs.ebp;
    ip = (FlatPtr*)m_thread_info.regs.eip;
#else
    bp = (FlatPtr*)m_thread_info.regs.rbp;
    ip = (FlatPtr*)m_thread_info.regs.rip;
#endif

    bool first_frame = true;
    while (bp && ip) {
        // We use eip - 1 because the return address from a function frame
        // is the instruction that comes after the 'call' instruction.
        // However, because the first frame represents the faulting
        // instruction rather than the return address we don't subtract
        // 1 there.
        VERIFY((FlatPtr)ip > 0);
        add_entry(coredump, (FlatPtr)ip - (first_frame ? 0 : 1));
        first_frame = false;
        auto next_ip = coredump.peek_memory((FlatPtr)(bp + 1));
        auto next_bp = coredump.peek_memory((FlatPtr)(bp));
        if (!next_ip.has_value() || !next_bp.has_value())
            break;
        ip = (FlatPtr*)next_ip.value();
        bp = (FlatPtr*)next_bp.value();
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

#if ARCH(I386)
    auto function_name = object_info->debug_info->elf().symbolicate(eip - region->region_start);
    auto source_position = object_info->debug_info->get_source_position_with_inlines(eip - region->region_start);
#else
    // FIXME: Fix symbolication.
    auto function_name = "";
    Debug::DebugInfo::SourcePositionWithInlines source_position;
#endif
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
    builder.append(" (");

    Vector<Debug::DebugInfo::SourcePosition> source_positions;

    for (auto& position : source_position_with_inlines.inline_chain) {
        if (!source_positions.contains_slow(position))
            source_positions.append(position);
    }

    if (source_position_with_inlines.source_position.has_value() && !source_positions.contains_slow(source_position_with_inlines.source_position.value())) {
        source_positions.insert(0, source_position_with_inlines.source_position.value());
    }

    for (size_t i = 0; i < source_positions.size(); ++i) {
        auto& position = source_positions[i];
        auto fmt = color ? "\033[34;1m{}\033[0m:{}" : "{}:{}";
        builder.appendff(fmt, LexicalPath::basename(position.file_path), position.line_number);
        if (i != source_positions.size() - 1) {
            builder.append(" => ");
        }
    }

    builder.append(")");

    return builder.build();
}

}
