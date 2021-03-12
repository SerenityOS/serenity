/*
 * Copyright (c) 2020, Peter Elliott <pelliott@ualberta.ca>
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@gmail.com>
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

#include <AK/Span.h>
#include <AK/Stream.h>
#include <LibTar/Tar.h>

namespace Tar {

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
    const Header& header() const { return m_header; }
    TarFileStream file_contents();

private:
    Header m_header;
    InputStream& m_stream;
    unsigned long m_file_offset { 0 };
    int m_generation { 0 };
    bool m_finished { false };

    friend class TarFileStream;
};

class TarOutputStream {
public:
    TarOutputStream(OutputStream&);
    void add_file(const String& path, mode_t, const ReadonlyBytes&);
    void add_directory(const String& path, mode_t);
    void finish();

private:
    OutputStream& m_stream;
    bool m_finished { false };

    friend class TarFileStream;
};

}
