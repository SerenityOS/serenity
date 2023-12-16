/*
 * Copyright (c) 2021, Itamar S. <itamar8910@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Inspector.h"

namespace Coredump {

OwnPtr<Inspector> Inspector::create(StringView coredump_path, Function<void(float)> on_progress)
{
    auto reader = Reader::create(coredump_path);
    if (!reader)
        return {};
    return AK::adopt_own_if_nonnull(new (nothrow) Inspector(reader.release_nonnull(), move(on_progress)));
}

Inspector::Inspector(NonnullOwnPtr<Reader>&& reader, Function<void(float)> on_progress)
    : m_reader(move(reader))
{
    parse_loaded_libraries(move(on_progress));
}

size_t Inspector::number_of_libraries_in_coredump() const
{
    size_t count = 0;
    m_reader->for_each_library([&count](Coredump::Reader::LibraryInfo) {
        ++count;
    });
    return count;
}

void Inspector::parse_loaded_libraries(Function<void(float)> on_progress)
{
    size_t number_of_libraries = number_of_libraries_in_coredump();
    size_t library_index = 0;

    m_reader->for_each_library([this, number_of_libraries, &library_index, &on_progress](Coredump::Reader::LibraryInfo library) {
        ++library_index;
        if (on_progress)
            on_progress(library_index / static_cast<float>(number_of_libraries));

        auto file_or_error = Core::MappedFile::map(library.path);
        if (file_or_error.is_error())
            return;

        auto image = make<ELF::Image>(file_or_error.value()->bytes());
        auto debug_info = make<Debug::DebugInfo>(*image, ByteString {}, library.base_address);
        m_loaded_libraries.append(make<Debug::LoadedLibrary>(library.name, file_or_error.release_value(), move(image), move(debug_info), library.base_address));
    });
}

bool Inspector::poke(FlatPtr, FlatPtr) { return false; }

Optional<FlatPtr> Inspector::peek(FlatPtr address) const
{
    return m_reader->peek_memory(address);
}

PtraceRegisters Inspector::get_registers() const
{
    PtraceRegisters registers {};
    m_reader->for_each_thread_info([&](ELF::Core::ThreadInfo const& thread_info) {
        registers = thread_info.regs;
        return IterationDecision::Break;
    });
    return registers;
}

void Inspector::set_registers(PtraceRegisters const&) {};

void Inspector::for_each_loaded_library(Function<IterationDecision(Debug::LoadedLibrary const&)> func) const
{
    for (auto& library : m_loaded_libraries) {
        if (func(*library) == IterationDecision::Break)
            break;
    }
}

}
