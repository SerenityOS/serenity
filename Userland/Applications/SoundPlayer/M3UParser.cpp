/*
 * Copyright (c) 2021, Cesar Torres <shortanemoia@protonmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "M3UParser.h"
#include <AK/OwnPtr.h>
#include <AK/RefPtr.h>
#include <AK/ScopeGuard.h>

M3UParser::M3UParser()
{
}

NonnullOwnPtr<M3UParser> M3UParser::from_file(const String path)
{
    auto parser = make<M3UParser>();
    VERIFY(!path.is_null() && !path.is_empty() && !path.is_whitespace());
    parser->m_use_utf8 = path.ends_with(".m3u8", AK::CaseSensitivity::CaseInsensitive);
    FILE* file = fopen(path.characters(), "r");
    ScopeGuard file_guard = [&] { fclose(file); };
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

    bool has_extended_info_tag = false;
    if (!m_use_utf8) {
        auto lines = m_m3u_raw_data.split_view('\n');

        if (include_extended_info) {
            if (lines[0] == "#EXTM3U")
                has_extended_info_tag = true;
        }

        M3UExtendedInfo metadata_for_next_file {};
        for (auto& line : lines) {
            line = line.trim_whitespace();
            M3UEntry entry {};
            if (line.starts_with('#') && has_extended_info_tag) {
                if (line.starts_with("#EXTINF:")) {
                    auto data = line.substring_view(8);
                    auto separator = data.find(',');
                    VERIFY(separator.has_value());
                    auto seconds = data.substring_view(0, separator.value());
                    VERIFY(!seconds.is_whitespace() && !seconds.is_null() && !seconds.is_empty());
                    metadata_for_next_file.track_length_in_seconds = seconds.to_uint();
                    auto display_name = data.substring_view(seconds.length() + 1);
                    VERIFY(!display_name.is_empty() && !display_name.is_null() && !display_name.is_empty());
                    metadata_for_next_file.track_display_title = display_name;
                    //TODO: support the alternative, non-standard #EXTINF value of a key=value dictionary
                } else if (line.starts_with("#PLAYLIST:")) {
                    auto name = line.substring_view(10);
                    VERIFY(!name.is_empty());
                    m_parsed_playlist_title = name;
                } else if (line.starts_with("#EXTGRP:")) {
                    auto name = line.substring_view(8);
                    VERIFY(!name.is_empty());
                    metadata_for_next_file.group_name = name;
                } else if (line.starts_with("#EXTALB:")) {
                    auto name = line.substring_view(8);
                    VERIFY(!name.is_empty());
                    metadata_for_next_file.album_title = name;
                } else if (line.starts_with("#EXTART:")) {
                    auto name = line.substring_view(8);
                    VERIFY(!name.is_empty());
                    metadata_for_next_file.album_artist = name;
                } else if (line.starts_with("#EXTGENRE:")) {
                    auto name = line.substring_view(10);
                    VERIFY(!name.is_empty());
                    metadata_for_next_file.album_genre = name;
                }
                //TODO: Support M3A files (M3U files with embedded mp3 files)
            } else {
                entry.path = line;
                entry.extended_info = metadata_for_next_file;
                vec->append(entry);
                metadata_for_next_file = {};
            }
        }
    } else {
        //TODO: Implement M3U8 parsing
        TODO();
    }
    return vec;
}
