/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Buffered.h>
#include <AK/ByteBuffer.h>
#include <AK/Stream.h>
#include <LibCore/File.h>

namespace Core {

class InputFileStream final : public InputStream {
public:
    explicit InputFileStream(NonnullRefPtr<File> file)
        : m_file(file)
    {
    }

    static Result<InputFileStream, String> open(StringView filename, OpenMode mode = OpenMode::ReadOnly, mode_t permissions = 0644)
    {
        VERIFY(has_flag(mode, OpenMode::ReadOnly));

        auto file_result = File::open(filename, mode, permissions);

        if (file_result.is_error())
            return file_result.error();

        return InputFileStream { file_result.value() };
    }

    static Result<Buffered<InputFileStream>, String> open_buffered(StringView filename, OpenMode mode = OpenMode::ReadOnly, mode_t permissions = 0644)
    {
        VERIFY(has_flag(mode, OpenMode::ReadOnly));

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

    bool discard_or_error(size_t count) override { return m_file->seek(count, SeekMode::FromCurrentPosition); }

    bool unreliable_eof() const override { return m_file->eof(); }

    void close()
    {
        if (!m_file->close())
            set_fatal_error();
    }

private:
    NonnullRefPtr<File> m_file;
};

class OutputFileStream : public OutputStream {
public:
    explicit OutputFileStream(NonnullRefPtr<File> file)
        : m_file(file)
    {
    }

    static Result<OutputFileStream, String> open(StringView filename, OpenMode mode = OpenMode::WriteOnly, mode_t permissions = 0644)
    {
        VERIFY(has_flag(mode, OpenMode::WriteOnly));

        auto file_result = File::open(filename, mode, permissions);

        if (file_result.is_error())
            return file_result.error();

        return OutputFileStream { file_result.value() };
    }

    static Result<Buffered<OutputFileStream>, String> open_buffered(StringView filename, OpenMode mode = OpenMode::WriteOnly, mode_t permissions = 0644)
    {
        VERIFY(has_flag(mode, OpenMode::WriteOnly));

        auto file_result = File::open(filename, mode, permissions);

        if (file_result.is_error())
            return file_result.error();

        return Buffered<OutputFileStream> { file_result.value() };
    }

    static OutputFileStream standard_output()
    {
        return OutputFileStream { Core::File::standard_output() };
    }

    static OutputFileStream standard_error()
    {
        return OutputFileStream { Core::File::standard_error() };
    }

    static Buffered<OutputFileStream> stdout_buffered()
    {
        return Buffered<OutputFileStream> { Core::File::standard_output() };
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
