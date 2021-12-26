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
#include <LibCore/File.h>

namespace Core {

class InputFileStream final : public InputStream {
public:
    explicit InputFileStream(NonnullRefPtr<File> file)
        : m_file(file)
    {
    }

    static Result<InputFileStream, String> open(StringView filename, IODevice::OpenMode mode = IODevice::OpenMode::ReadOnly, mode_t permissions = 0644)
    {
        ASSERT((mode & 0xf) == IODevice::OpenMode::ReadOnly || (mode & 0xf) == IODevice::OpenMode::ReadWrite);

        auto file_result = File::open(filename, mode, permissions);

        if (file_result.is_error())
            return file_result.error();

        return InputFileStream { file_result.value() };
    }

    size_t read(Bytes bytes) override
    {
        size_t nread = 0;

        if (!m_buffered.is_empty()) {
            nread += m_buffered.bytes().copy_trimmed_to(bytes);

            m_buffered.bytes().slice(nread).copy_to(m_buffered);
            m_buffered.trim(m_buffered.size() - nread);
        }

        while (nread < bytes.size() && !eof()) {
            if (m_file->has_error()) {
                m_error = true;
                return 0;
            }

            const auto buffer = m_file->read(bytes.size() - nread);
            nread += buffer.bytes().copy_to(bytes.slice(nread));
        }

        return nread;
    }

    bool read_or_error(Bytes bytes) override
    {
        if (read(bytes) < bytes.size()) {
            m_error = true;
            return false;
        }

        return true;
    }

    bool discard_or_error(size_t count) override
    {
        u8 buffer[4096];

        size_t ndiscarded = 0;
        while (ndiscarded < count && !eof())
            ndiscarded += read({ buffer, min<size_t>(count - ndiscarded, sizeof(buffer)) });

        if (eof()) {
            m_error = true;
            return false;
        }

        return true;
    }

    bool eof() const override
    {
        if (m_buffered.size() > 0)
            return false;

        if (m_file->eof())
            return true;

        m_buffered = m_file->read(4096);

        return m_buffered.size() == 0;
    }

    void close()
    {
        if (!m_file->close())
            m_error = true;
    }

private:
    InputFileStream() = default;

    mutable NonnullRefPtr<File> m_file;
    mutable ByteBuffer m_buffered;
};

class OutputFileStream : public OutputStream {
public:
    explicit OutputFileStream(NonnullRefPtr<File> file)
        : m_file(file)
    {
    }

    static Result<OutputFileStream, String> open(StringView filename, IODevice::OpenMode mode = IODevice::OpenMode::WriteOnly, mode_t permissions = 0644)
    {
        ASSERT((mode & 0xf) == IODevice::OpenMode::WriteOnly || (mode & 0xf) == IODevice::OpenMode::ReadWrite);

        auto file_result = File::open(filename, mode, permissions);

        if (file_result.is_error())
            return file_result.error();

        return OutputFileStream { file_result.value() };
    }

    static OutputFileStream stdout()
    {
        return OutputFileStream { Core::File::stdout() };
    }

    size_t write(ReadonlyBytes bytes) override
    {
        if (!m_file->write(bytes.data(), bytes.size())) {
            m_error = true;
            return 0;
        }

        return bytes.size();
    }

    bool write_or_error(ReadonlyBytes bytes) override
    {
        if (write(bytes) < bytes.size()) {
            m_error = true;
            return false;
        }

        return true;
    }

    void close()
    {
        if (!m_file->close())
            m_error = true;
    }

private:
    NonnullRefPtr<File> m_file;
};

}
