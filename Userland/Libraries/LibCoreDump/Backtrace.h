/*
 * Copyright (c) 2020, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>
#include <LibCoreDump/Reader.h>
#include <LibDebug/DebugInfo.h>
#include <LibELF/CoreDump.h>

namespace CoreDump {

struct ELFObjectInfo {
    ELFObjectInfo(NonnullRefPtr<MappedFile> file, Debug::DebugInfo&& debug_info)
        : file(move(file))
        , debug_info(move(debug_info))
    {
    }

    NonnullRefPtr<MappedFile> file;
    Debug::DebugInfo debug_info;
};

class Backtrace {
public:
    struct Entry {
        FlatPtr eip;
        String object_name;
        String function_name;
        Optional<Debug::DebugInfo::SourcePosition> source_position;

        String to_string(bool color = false) const;
    };

    Backtrace(const Reader&, const ELF::Core::ThreadInfo&);
    ~Backtrace();

    const ELF::Core::ThreadInfo thread_info() const { return m_thread_info; }
    const Vector<Entry> entries() const { return m_entries; }

private:
    void add_entry(const Reader&, FlatPtr eip);

    ELF::Core::ThreadInfo m_thread_info;
    Vector<Entry> m_entries;
};

}
