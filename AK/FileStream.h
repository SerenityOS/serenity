/*
 * Copyright (c) 2020, the SerenityOS developers.
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

#include <AK/Stream.h>
#ifndef KERNEL
#    include <errno.h>
#    include <stdio.h>

namespace AK {

class InputFileStream : public InputStream {
public:
    explicit InputFileStream(int fd)
        : m_file(fdopen(fd, "r"))
        , m_owned(true)
    {
        if (!m_file)
            set_fatal_error();
    }

    explicit InputFileStream(FILE* fp)
        : m_file(fp)
    {
        if (!m_file)
            set_fatal_error();
    }

    ~InputFileStream()
    {
        if (m_file) {
            fflush(m_file);
            if (m_owned)
                fclose(m_file);
        }
    }

    bool unreliable_eof() const override { return eof(); }
    bool eof() const { return feof(m_file); }

    size_t read(Bytes bytes) override
    {
        if (has_any_error())
            return 0;
        return fread(bytes.data(), sizeof(u8), bytes.size(), m_file);
    }

    bool read_or_error(Bytes bytes) override
    {
        if (has_any_error())
            return false;
        auto size = read(bytes);
        if (size < bytes.size()) {
            set_recoverable_error();
            return false;
        }
        return true;
    }
    bool discard_or_error(size_t count) override
    {
        if (fseek(m_file, count, SEEK_CUR) == 0)
            return true;

        // TODO: Why not ferror(m_file)?
        if (errno != ESPIPE)
            return false;

        char buf[4];
        size_t i = 0;
        while (i < count) {
            auto size = min(count - i, 4ul);
            if (read({ buf, size }) < size) {
                // Can't reset here.
                return false;
            }
            i += size;
        }

        return true;
    }

    bool seek(size_t offset, int whence = SEEK_SET)
    {
        return fseek(m_file, offset, whence) == 0;
    }

    virtual bool handle_any_error() override
    {
        clearerr(m_file);
        return Stream::handle_any_error();
    }

    void make_unbuffered()
    {
        setvbuf(m_file, nullptr, _IONBF, 0);
    }

private:
    FILE* m_file { nullptr };
    bool m_owned { false };
};

class OutputFileStream : public OutputStream {
public:
    explicit OutputFileStream(int fd)
        : m_file(fdopen(fd, "w"))
        , m_owned(true)
    {
        if (!m_file)
            set_fatal_error();
    }

    explicit OutputFileStream(FILE* fp)
        : m_file(fp)
    {
        if (!m_file)
            set_fatal_error();
    }

    ~OutputFileStream()
    {
        if (m_file) {
            fflush(m_file);
            if (m_owned)
                fclose(m_file);
        }
    }

    size_t write(ReadonlyBytes bytes) override
    {
        auto nwritten = fwrite(bytes.data(), sizeof(u8), bytes.size(), m_file);
        m_bytes_written += nwritten;
        return nwritten;
    }

    bool write_or_error(ReadonlyBytes bytes) override
    {
        auto nwritten = write(bytes);
        if (nwritten < bytes.size()) {
            set_recoverable_error();
            return false;
        }
        return true;
    }

    size_t size() const { return m_bytes_written; }

    virtual bool handle_any_error() override
    {
        clearerr(m_file);
        return Stream::handle_any_error();
    }

    void make_unbuffered()
    {
        setvbuf(m_file, nullptr, _IONBF, 0);
    }

private:
    FILE* m_file { nullptr };
    size_t m_bytes_written { 0 };
    bool m_owned { false };
};

}

using AK::InputFileStream;
using AK::OutputFileStream;

#endif
