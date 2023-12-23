/*
 * Copyright (c) 2021, Cesar Torres <shortanemoia@protonmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "M3UParser.h"
#include <AK/OwnPtr.h>
#include <AK/RefPtr.h>
#include <AK/ScopeGuard.h>
#include <AK/Utf8View.h>
#include <LibCore/File.h>

M3UParser::M3UParser()
{
}

NonnullOwnPtr<M3UParser> M3UParser::from_file(StringView path)
{
    auto file_result = Core::File::open(path, Core::File::OpenMode::Read).release_value_but_fixme_should_propagate_errors();
    auto contents = file_result->read_until_eof().release_value_but_fixme_should_propagate_errors();
    auto use_utf8 = path.ends_with(".m3u8"sv, CaseSensitivity::CaseInsensitive);
    return from_memory(ByteString { contents, NoChomp }, use_utf8);
}

NonnullOwnPtr<M3UParser> M3UParser::from_memory(ByteString const& m3u_contents, bool utf8)
{
    auto parser = make<M3UParser>();
    VERIFY(!m3u_contents.is_empty() && !m3u_contents.is_whitespace());
    parser->m_m3u_raw_data = m3u_contents;
    parser->m_use_utf8 = utf8;
    return parser;
}

NonnullOwnPtr<Vector<M3UEntry>> M3UParser::parse(bool include_extended_info)
{
    auto vec = make<Vector<M3UEntry>>();

    if (m_use_utf8) {
        // TODO: Implement M3U8 parsing
        TODO();
        return vec;
    }

    auto lines = m_m3u_raw_data.split_view('\n');

    bool has_extended_info_tag = include_extended_info && (lines[0] == "#EXTM3U");

    M3UExtendedInfo metadata_for_next_file {};
    for (auto& line : lines) {
        line = line.trim_whitespace();

        if (!has_extended_info_tag || !line.starts_with('#')) {
            vec->append({ line, metadata_for_next_file });
            metadata_for_next_file = {};
            continue;
        }

        auto tag = [&line](StringView tag_name) -> Optional<StringView> {
            if (line.starts_with(tag_name)) {
                auto value = line.substring_view(tag_name.length());
                VERIFY(!value.is_empty());
                return value;
            }
            return {};
        };

        if (auto ext_inf = tag("#EXTINF:"sv); ext_inf.has_value()) {
            auto separator = ext_inf.value().find(',');
            VERIFY(separator.has_value());
            auto seconds = ext_inf.value().substring_view(0, separator.value());
            VERIFY(!seconds.is_whitespace() && !seconds.is_null() && !seconds.is_empty());
            metadata_for_next_file.track_length_in_seconds = seconds.to_number<unsigned>();
            auto display_name = ext_inf.value().substring_view(seconds.length() + 1);
            VERIFY(!display_name.is_empty() && !display_name.is_null() && !display_name.is_empty());
            metadata_for_next_file.track_display_title = display_name;
            // TODO: support the alternative, non-standard #EXTINF value of a key=value dictionary
            continue;
        }
        if (auto playlist = tag("#PLAYLIST:"sv); playlist.has_value())
            m_parsed_playlist_title = move(playlist.value());
        else if (auto ext_grp = tag("#EXTGRP:"sv); ext_grp.has_value())
            metadata_for_next_file.group_name = move(ext_grp.value());
        else if (auto ext_alb = tag("#EXTALB:"sv); ext_alb.has_value())
            metadata_for_next_file.album_title = move(ext_alb.value());
        else if (auto ext_art = tag("#EXTART:"sv); ext_art.has_value())
            metadata_for_next_file.album_artist = move(ext_art.value());
        else if (auto ext_genre = tag("#EXTGENRE:"sv); ext_genre.has_value())
            metadata_for_next_file.album_genre = move(ext_genre.value());
        // TODO: Support M3A files (M3U files with embedded mp3 files)
    }

    return vec;
}
