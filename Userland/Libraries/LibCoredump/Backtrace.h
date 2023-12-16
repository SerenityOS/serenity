/*
 * Copyright (c) 2020, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>
#include <LibCoredump/Reader.h>
#include <LibDebug/DebugInfo.h>
#include <LibELF/Core.h>

namespace Coredump {

struct ELFObjectInfo {
    ELFObjectInfo(NonnullOwnPtr<Core::MappedFile> file, NonnullOwnPtr<Debug::DebugInfo>&& debug_info, NonnullOwnPtr<ELF::Image> image)
        : file(move(file))
        , debug_info(move(debug_info))
        , image(move(image))
    {
    }

    NonnullOwnPtr<Core::MappedFile> file;
    NonnullOwnPtr<Debug::DebugInfo> debug_info;
    NonnullOwnPtr<ELF::Image> image;
};

class Backtrace {
public:
    struct Entry {
        FlatPtr eip;
        ByteString object_name;
        ByteString function_name;
        Debug::DebugInfo::SourcePositionWithInlines source_position_with_inlines;

        ByteString to_byte_string(bool color = false) const;
    };

    Backtrace(Reader const&, const ELF::Core::ThreadInfo&, Function<void(size_t, size_t)> on_progress = {});
    ~Backtrace() = default;

    ELF::Core::ThreadInfo const& thread_info() const { return m_thread_info; }
    Vector<Entry> const& entries() const { return m_entries; }

private:
    void add_entry(Reader const&, FlatPtr ip);
    ELFObjectInfo const* object_info_for_region(Reader const&, MemoryRegionInfo const&);

    bool m_skip_loader_so { false };
    ELF::Core::ThreadInfo m_thread_info;
    Vector<Entry> m_entries;
    HashMap<ByteString, NonnullOwnPtr<ELFObjectInfo>> m_debug_info_cache;
};

}
