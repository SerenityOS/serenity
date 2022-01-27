/*
 * Copyright (c) 2020, Linus Groh <linusg@serenityos.org>
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
    ELFObjectInfo(NonnullRefPtr<Core::MappedFile> file, NonnullOwnPtr<Debug::DebugInfo>&& debug_info, NonnullOwnPtr<ELF::Image> image)
        : file(move(file))
        , debug_info(move(debug_info))
        , image(move(image))
    {
    }

    NonnullRefPtr<Core::MappedFile> file;
    NonnullOwnPtr<Debug::DebugInfo> debug_info;
    NonnullOwnPtr<ELF::Image> image;
};

class Backtrace {
public:
    struct Entry {
        FlatPtr eip;
        String object_name;
        String function_name;
        Debug::DebugInfo::SourcePositionWithInlines source_position_with_inlines;

        String to_string(bool color = false) const;
    };

    Backtrace(const Reader&, const ELF::Core::ThreadInfo&, Function<void(size_t, size_t)> on_progress = {});
    ~Backtrace();

    ELF::Core::ThreadInfo const& thread_info() const { return m_thread_info; }
    Vector<Entry> const& entries() const { return m_entries; }

private:
    void add_entry(const Reader&, FlatPtr ip);
    ELFObjectInfo const* object_info_for_region(MemoryRegionInfo const&);

    bool m_skip_loader_so { false };
    ELF::Core::ThreadInfo m_thread_info;
    Vector<Entry> m_entries;
    HashMap<String, NonnullOwnPtr<ELFObjectInfo>> m_debug_info_cache;
};

}
