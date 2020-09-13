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

#include <AK/Buffered.h>
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

    static Result<Buffered<InputFileStream>, String> open_buffered(StringView filename, IODevice::OpenMode mode = IODevice::OpenMode::ReadOnly, mode_t permissions = 0644)
    {
        ASSERT((mode & 0xf) == IODevice::OpenMode::ReadOnly || (mode & 0xf) == IODevice::OpenMode::ReadWrite);

        auto file_result = File::open(filename, mode, permissions);

        if (file_result.is_error())
            return file_result.error();

        return Buffered<InputFileStream> { file_result.value() };
    }

    size_t read(Bytes bytes) override
    {
        if (has_any_error())
            return 0;

        const auto buffer = m_file->read(bytes.size());
        return buffer.bytes().copy_to(bytes);
    }

    bool read_or_error(Bytes bytes) override
    {
        if (read(bytes) < bytes.size()) {
            set_fatal_error();
            return false;
        }

        return true;
    }

    bool discard_or_error(size_t count) override { return m_file->seek(count, IODevice::SeekMode::FromCurrentPosition); }

    bool unreliable_eof() const override { return m_file->eof(); }

    void close()
    {
        if (!m_file->close())
            set_fatal_error();
    }

private:
    InputFileStream() = default;

    NonnullRefPtr<File> m_file;
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

    static Result<Buffered<OutputFileStream>, String> open_buffered(StringView filename, IODevice::OpenMode mode = IODevice::OpenMode::WriteOnly, mode_t permissions = 0644)
    {
        ASSERT((mode & 0xf) == IODevice::OpenMode::WriteOnly || (mode & 0xf) == IODevice::OpenMode::ReadWrite);

        auto file_result = File::open(filename, mode, permissions);

        if (file_result.is_error())
            return file_result.error();

        return Buffered<OutputFileStream> { file_result.value() };
    }

    static OutputFileStream stdout()
    {
        return OutputFileStream { Core::File::stdout() };
    }

    static Buffered<OutputFileStream> stdout_buffered()
    {
        return Buffered<OutputFileStream> { Core::File::stdout() };
    }

    size_t write(ReadonlyBytes bytes) override
    {
        if (!m_file->write(bytes.data(), bytes.size())) {
            set_fatal_error();
            return 0;
        }

        return bytes.size();
    }

    bool write_or_error(ReadonlyBytes bytes) override
    {
        if (write(bytes) < bytes.size()) {
            set_fatal_error();
            return false;
        }

        return true;
    }

    void close()
    {
        if (!m_file->close())
            set_fatal_error();
    }

private:
    NonnullRefPtr<File> m_file;
};

}
