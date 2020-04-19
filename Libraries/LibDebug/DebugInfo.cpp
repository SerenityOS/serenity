/*
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
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

#include "DebugInfo.h"
#include <AK/QuickSort.h>

DebugInfo::DebugInfo(NonnullRefPtr<const ELF::Loader> elf)
    : m_elf(elf)
{
    prepare_lines();
}

void DebugInfo::prepare_lines()
{

    auto section = m_elf->image().lookup_section(".debug_line");
    ASSERT(!section.is_undefined());

    auto buffer = ByteBuffer::wrap(reinterpret_cast<const u8*>(section.raw_data()), section.size());
    BufferStream stream(buffer);

    Vector<LineProgram::LineInfo> all_lines;
    while (!stream.at_end()) {
        LineProgram program(stream);
        all_lines.append(move(program.lines()));
    }

    for (auto& line_info : all_lines) {
        String file_path = line_info.file;
        if (file_path.contains("Toolchain/"))
            continue;
        if (file_path.contains("serenity/")) {
            auto start_index = file_path.index_of("serenity/").value() + String("serenity/").length();
            file_path = file_path.substring(start_index, file_path.length() - start_index);
        }
        m_sorted_lines.append({ line_info.address, file_path, line_info.line });
    }
    quick_sort(m_sorted_lines, [](auto& a, auto& b) {
        return a.address < b.address;
    });
}

Optional<DebugInfo::SourcePosition> DebugInfo::get_source_position(u32 target_address) const
{

    if (m_sorted_lines.is_empty())
        return {};
    if (target_address < m_sorted_lines[0].address)
        return {};

    // TODO: We can do a binray search here
    for (size_t i = 0; i < m_sorted_lines.size() - 1; ++i) {
        if (m_sorted_lines[i + 1].address > target_address) {
            return Optional<SourcePosition>({ m_sorted_lines[i].file, m_sorted_lines[i].line });
        }
    }
    return {};
}

Optional<u32> DebugInfo::get_instruction_from_source(const String& file, size_t line) const
{
    for (const auto& line_entry : m_sorted_lines) {
        dbg() << line_entry.file;
        if (line_entry.file == file && line_entry.line == line)
            return Optional<u32>(line_entry.address);
    }
    return {};
}
