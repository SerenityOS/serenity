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

#pragma once

#include <AK/Types.h>
#include <LibCoreDump/Reader.h>
#include <LibDebug/DebugInfo.h>

namespace CoreDump {

struct ELFObjectInfo {
    ELFObjectInfo(MappedFile&& file, Debug::DebugInfo&& debug_info)
        : file(move(file))
        , debug_info(move(debug_info))
    {
    }

    MappedFile file;
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

    Backtrace(const Reader&);
    ~Backtrace();

    const Vector<Entry> entries() const { return m_entries; }

private:
    void add_backtrace_entry(const Reader&, FlatPtr eip);

    Vector<Entry> m_entries;
};

}
