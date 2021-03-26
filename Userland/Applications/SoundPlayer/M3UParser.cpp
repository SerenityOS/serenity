/*
 * Copyright (c) 2021, Cesar Torres <shortanemoia@protonmail.com>
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

#include "M3UParser.h"
#include <AK/NonnullOwnPtrVector.h>
#include <AK/OwnPtr.h>
#include <AK/RefPtr.h>
#include <AK/Utf8View.h>
#include <LibCore/FileStream.h>
#include <ctype.h>

M3UParser::M3UParser()
{
}

NonnullOwnPtr<M3UParser> M3UParser::from_file(const String path)
{

    auto parser = make<M3UParser>();
    VERIFY(!path.is_null() && !path.is_empty() && !path.is_whitespace());
    parser->m_use_utf8 = path.ends_with(".m3u8", AK::CaseSensitivity::CaseInsensitive);
    FILE* file = fopen(path.characters(), "r");
    VERIFY(file != nullptr);
    fseek(file, 0, SEEK_END);
    size_t file_size = ftell(file);
    fseek(file, 0, SEEK_SET);
    VERIFY(file_size > 0);
    Vector<u8> temp_buffer;
    temp_buffer.resize(file_size);
    auto bytes_read = fread(temp_buffer.data(), 1, file_size, file);
    VERIFY(bytes_read == file_size);
    parser->m_m3u_raw_data = *new String(temp_buffer.span(), NoChomp);
    return parser;
}

NonnullOwnPtr<M3UParser> M3UParser::from_memory(const String& m3u_contents, bool utf8)
{
    auto parser = make<M3UParser>();
    VERIFY(!m3u_contents.is_null() && !m3u_contents.is_empty() && !m3u_contents.is_whitespace());
    parser->m_m3u_raw_data = m3u_contents;
    parser->m_use_utf8 = utf8;
    return parser;
}

NonnullOwnPtr<Vector<M3UEntry>> M3UParser::parse(bool include_extended_info)
{
    auto vec = make<Vector<M3UEntry>>();

    bool has_exteded_info_tag = false;
    if (!m_use_utf8) {
        auto lines = m_m3u_raw_data.split_view('\n');

        if (include_extended_info) {
            if (lines[0] == "#EXTM3U")
                has_exteded_info_tag = true;
        }

        M3UExtendedInfo metadata_for_next_file {};
        for (auto& line : lines) {
            line = line.trim_whitespace();
            M3UEntry entry {};
            if (line.starts_with('#') && has_exteded_info_tag) {
                if (line.starts_with("#EXTINF")) {
                    auto data = line.substring_view(8);
                    auto separator = data.find_first_of(',');
                    VERIFY(separator.has_value());
                    auto seconds = data.substring_view(0, separator.value());
                    VERIFY(!seconds.is_whitespace() && !seconds.is_null() && !seconds.is_empty());
                    metadata_for_next_file.track_length_in_seconds = seconds.to_uint();
                    auto display_name = data.substring_view(seconds.length() + 1);
                    VERIFY(!display_name.is_empty() && !display_name.is_null() && !display_name.is_empty());
                    metadata_for_next_file.track_display_title = display_name;
                    //TODO: support the alternative, non-standard #EXTINF value of a key=value dictionary
                } else if (line.starts_with("#PLAYLIST")) {
                    auto name = line.substring_view(10);
                    VERIFY(!name.is_empty());
                    m_parsed_playlist_title = name;
                } else if (line.starts_with("#EXTGRP")) {
                    auto name = line.substring_view(8);
                    VERIFY(!name.is_empty());
                    metadata_for_next_file.group_name = name;
                } else if (line.starts_with("#EXTALB")) {
                    auto name = line.substring_view(8);
                    VERIFY(!name.is_empty());
                    metadata_for_next_file.album_title = name;
                } else if (line.starts_with("#EXTART")) {
                    auto name = line.substring_view(8);
                    VERIFY(!name.is_empty());
                    metadata_for_next_file.album_artist = name;
                } else if (line.starts_with("#EXTGENRE")) {
                    auto name = line.substring_view(10);
                    VERIFY(!name.is_empty());
                    metadata_for_next_file.album_genre = name;
                }
                //TODO: Support M3A files (M3U files with embedded mp3 files)
            } else {
                entry.path = line;
                entry.extended_info = metadata_for_next_file;
                vec->append(entry);
            }
        }
    } else {
        //TODO: Implement M3U8 parsing
        TODO();
    }
    return vec;
}
