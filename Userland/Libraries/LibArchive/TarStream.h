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
    const TarFileHeader& header() const { return m_header; }
    TarFileStream file_contents();

    template<VoidFunction<StringView, StringView> F>
    ErrorOr<void> for_each_extended_header(F func);

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
    void add_file(const String& path, mode_t, ReadonlyBytes);
    void add_directory(const String& path, mode_t);
    void finish();

private:
    OutputStream& m_stream;
    bool m_finished { false };

    friend class TarFileStream;
};

template<VoidFunction<StringView, StringView> F>
inline ErrorOr<void> TarInputStream::for_each_extended_header(F func)
{
    VERIFY(header().content_is_like_extended_header());

    Archive::TarFileStream file_stream = file_contents();

    ByteBuffer file_contents_buffer = TRY(ByteBuffer::create_zeroed(header().size()));
    VERIFY(file_stream.read(file_contents_buffer) == header().size());

    StringView file_contents { file_contents_buffer };

    while (!file_contents.is_empty()) {
        // Split off the length (until the first space).
        Optional<size_t> length_end_index = file_contents.find(' ');
        if (!length_end_index.has_value())
            return Error::from_string_literal("Malformed extended header: No length found.");
        Optional<unsigned int> length = file_contents.substring_view(0, length_end_index.value()).to_uint();
        if (!length.has_value())
            return Error::from_string_literal("Malformed extended header: Could not parse length.");
        unsigned int remaining_length = length.value();

        remaining_length -= length_end_index.value() + 1;
        file_contents = file_contents.substring_view(length_end_index.value() + 1);

        // Extract the header.
        StringView header = file_contents.substring_view(0, remaining_length - 1);
        file_contents = file_contents.substring_view(remaining_length - 1);

        // Ensure that the header ends at the expected location.
        if (file_contents.length() < 1 || !file_contents.starts_with('\n'))
            return Error::from_string_literal("Malformed extended header: Header does not end at expected location.");
        file_contents = file_contents.substring_view(1);

        // Find the delimiting '='.
        Optional<size_t> header_delimiter_index = header.find('=');
        if (!header_delimiter_index.has_value())
            return Error::from_string_literal("Malformed extended header: Header does not have a delimiter.");
        StringView key = header.substring_view(0, header_delimiter_index.value());
        StringView value = header.substring_view(header_delimiter_index.value() + 1);

        func(key, value);
    }

    return {};
}

}
