/*
 * Copyright (c) 2021, Cesar Torres <shortanemoia@protonmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Optional.h>
#include <AK/RefPtr.h>
#include <AK/String.h>
#include <AK/StringView.h>
#include <AK/Vector.h>

// Extended M3U fields (de facto standard)
struct M3UExtendedInfo {
    Optional<u32> track_length_in_seconds;
    Optional<String> track_display_title;
    Optional<String> group_name;
    Optional<String> album_title;
    Optional<String> album_artist;
    Optional<String> album_genre;
    Optional<u64> file_size_in_bytes;
    Optional<ReadonlyBytes> embedded_mp3;
    Optional<String> cover_path;
};

struct M3UEntry {
    String path;
    Optional<M3UExtendedInfo> extended_info;
};

class M3UParser {
public:
    static NonnullOwnPtr<M3UParser> from_file(String path);
    static NonnullOwnPtr<M3UParser> from_memory(String const& m3u_contents, bool utf8);

    NonnullOwnPtr<Vector<M3UEntry>> parse(bool include_extended_info);

    Optional<String>& get_playlist_title_metadata() { return m_parsed_playlist_title; }

    M3UParser();

private:
    String m_m3u_raw_data;
    String m_playlist_path;
    bool m_use_utf8;
    Optional<String> m_parsed_playlist_title;
};
