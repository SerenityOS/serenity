/*
 * Copyright (c) 2021, timmot <tiwwot@protonmail.com>
 * Copyright (c) 2022, Mustafa Quraish <mustafa@serenityos.org>
 * Copyright (c) 2023, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NonnullRefPtr.h>
#include <AK/String.h>
#include <LibCore/Stream.h>

namespace FileSystemAccessClient {
class File {
public:
    File(NonnullOwnPtr<Core::Stream::File> stream, String filename)
        : m_stream(move(stream))
        , m_filename(move(filename))
    {
    }

    Core::Stream::File& stream() const { return *m_stream; }
    NonnullOwnPtr<Core::Stream::File> release_stream() { return move(m_stream); }
    String filename() const { return m_filename; }

private:
    NonnullOwnPtr<Core::Stream::File> m_stream;
    String m_filename;
};
}
