/*
 * Copyright (c) 2020, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/LexicalPath.h>
#include <AK/Platform.h>
#include <AK/StackUnwinder.h>
#include <AK/StringBuilder.h>
#include <AK/Types.h>
#include <LibCore/MappedFile.h>
#include <LibCoredump/Backtrace.h>
#include <LibCoredump/Reader.h>
#include <LibELF/Core.h>
#include <LibELF/Image.h>
#include <LibFileSystem/FileSystem.h>

namespace Coredump {

ELFObjectInfo const* Backtrace::object_info_for_region(Reader const& coredump, MemoryRegionInfo const& region)
{
    ByteString path = coredump.resolve_object_path(region.object_name());

    auto maybe_ptr = m_debug_info_cache.get(path);
    if (maybe_ptr.has_value())
        return *maybe_ptr;

    if (!FileSystem::exists(path))
        return nullptr;

    auto file_or_error = Core::MappedFile::map(path);
    if (file_or_error.is_error())
        return nullptr;

    auto image = make<ELF::Image>(file_or_error.value()->bytes());
    auto& image_reference = *image;
    auto info = make<ELFObjectInfo>(file_or_error.release_value(), make<Debug::DebugInfo>(image_reference), move(image));
    auto* info_ptr = info.ptr();
    m_debug_info_cache.set(path, move(info));
    return info_ptr;
}

Backtrace::Backtrace(Reader const& coredump, const ELF::Core::ThreadInfo& thread_info, Function<void(size_t, size_t)> on_progress)
    : m_thread_info(move(thread_info))
{
    // In order to provide progress updates, we first have to walk the
    // call stack to determine how many frames it has.
    size_t frame_count = 0;
    MUST(AK::unwind_stack_from_frame_pointer(
        thread_info.regs.bp(),
        [&coredump](FlatPtr address) -> ErrorOr<FlatPtr> {
            auto maybe_value = coredump.peek_memory(address);
            if (!maybe_value.has_value())
                return EFAULT;

            return maybe_value.value();
        },
        [&frame_count](AK::StackFrame) -> ErrorOr<IterationDecision> {
            ++frame_count;
            return IterationDecision::Continue;
        }));

    size_t frame_index = 0;

    auto on_entry = [this, &coredump, &on_progress, &frame_index, frame_count](FlatPtr address) {
        add_entry(coredump, address);
        if (on_progress)
            on_progress(frame_index, frame_count);
        ++frame_index;
    };

    on_entry(thread_info.regs.ip());

    MUST(AK::unwind_stack_from_frame_pointer(
        thread_info.regs.bp(),
        [&coredump](FlatPtr address) -> ErrorOr<FlatPtr> {
            auto maybe_value = coredump.peek_memory(address);
            if (!maybe_value.has_value())
                return EFAULT;

            return maybe_value.value();
        },
        [&on_entry](AK::StackFrame stack_frame) -> ErrorOr<IterationDecision> {
            // We use return_address - 1 because the return address from a function frame
            // is the instruction that comes after the calling instruction.
            // However, because the first frame represents the faulting
            // instruction rather than the return address we don't subtract
            // 1 there.
            VERIFY(stack_frame.return_address > 0);
            on_entry(stack_frame.return_address - 1);

            return IterationDecision::Continue;
        }));
}

void Backtrace::add_entry(Reader const& coredump, FlatPtr ip)
{
    auto ip_region = coredump.region_containing(ip);
    if (!ip_region.has_value()) {
        m_entries.append({ ip, {}, {}, {} });
        return;
    }
    auto object_name = ip_region->object_name();
    // Only skip addresses coming from Loader.so if the faulting instruction is not in Loader.so
    if (object_name == "Loader.so") {
        if (m_skip_loader_so)
            return;
    } else {
        m_skip_loader_so = true;
    }
    // We need to find the first region for the object, just in case
    // the PT_LOAD header for the .text segment isn't the first one
    // in the object file.
    auto region = coredump.first_region_for_object(object_name);
    auto object_info = object_info_for_region(coredump, *region);
    if (!object_info) {
        m_entries.append({ ip, object_name, {}, {} });
        return;
    }

    auto function_name = object_info->debug_info->elf().symbolicate(ip - region->region_start);
    auto source_position = object_info->debug_info->get_source_position_with_inlines(ip - region->region_start).release_value_but_fixme_should_propagate_errors();
    m_entries.append({ ip, object_name, function_name, source_position });
}

ByteString Backtrace::Entry::to_byte_string(bool color) const
{
    StringBuilder builder;
    builder.appendff("{:p}: ", eip);
    if (object_name.is_empty()) {
        builder.append("???"sv);
        return builder.to_byte_string();
    }
    builder.appendff("[{}] {}", object_name, function_name.is_empty() ? "???" : function_name);
    builder.append(" ("sv);

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
        auto fmt = color ? "\033[34;1m{}\033[0m:{}"sv : "{}:{}"sv;
        builder.appendff(fmt, LexicalPath::basename(position.file_path), position.line_number);
        if (i != source_positions.size() - 1) {
            builder.append(" => "sv);
        }
    }

    builder.append(')');

    return builder.to_byte_string();
}

}
