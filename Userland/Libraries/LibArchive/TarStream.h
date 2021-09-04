/*
 * Copyright (c) 2020, Peter Elliott <pelliott@serenityos.org>
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Span.h>
#include <AK/Stream.h>
#include <LibArchive/Tar.h>

namespace Archive {

class TarInputStream;

class TarFileStream : public InputStream {
public:
    size_t read(Bytes) override;
    bool unreliable_eof() const override;

    bool read_or_error(Bytes) override;
    bool discard_or_error(size_t count) override;

private:
    TarFileStream(TarInputStream& stream);
    TarInputStream& m_tar_stream;
    int m_generation;

    friend class TarInputStream;
};

class TarInputStream {
public:
    TarInputStream(InputStream&);
    void advance();
    bool finished() const { return m_finished; }
    bool valid() const;
    TarFileHeader const& header() const { return m_header; }
    TarFileStream file_contents();

private:
    TarFileHeader m_header;
    InputStream& m_stream;
    unsigned long m_file_offset { 0 };
    int m_generation { 0 };
    bool m_finished { false };

    friend class TarFileStream;
};

class TarOutputStream {
public:
    TarOutputStream(OutputStream&);
    void add_file(String const& path, mode_t, ReadonlyBytes const&);
    void add_directory(String const& path, mode_t);
    void finish();

private:
    OutputStream& m_stream;
    bool m_finished { false };

    friend class TarFileStream;
};

}
