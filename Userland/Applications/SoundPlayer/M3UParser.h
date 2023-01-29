/*
 * Copyright (c) 2021, Cesar Torres <shortanemoia@protonmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/DeprecatedString.h>
#include <AK/Optional.h>
#include <AK/RefPtr.h>
#include <AK/StringView.h>
#include <AK/Vector.h>

// Extended M3U fields (de facto standard)
struct M3UExtendedInfo {
    Optional<u32> track_length_in_seconds;
    Optional<DeprecatedString> track_display_title;
    Optional<DeprecatedString> group_name;
    Optional<DeprecatedString> album_title;
    Optional<DeprecatedString> album_artist;
    Optional<DeprecatedString> album_genre;
    Optional<u64> file_size_in_bytes;
    Optional<ReadonlyBytes> embedded_mp3;
    Optional<DeprecatedString> cover_path;
};

struct M3UEntry {
    DeprecatedString path;
    Optional<M3UExtendedInfo> extended_info;
};

class M3UParser {
public:
    static NonnullOwnPtr<M3UParser> from_file(StringView path);
    static NonnullOwnPtr<M3UParser> from_memory(DeprecatedString const& m3u_contents, bool utf8);

    NonnullOwnPtr<Vector<M3UEntry>> parse(bool include_extended_info);

    Optional<DeprecatedString>& get_playlist_title_metadata() { return m_parsed_playlist_title; }

    M3UParser();

private:
    DeprecatedString m_m3u_raw_data;
    DeprecatedString m_playlist_path;
    bool m_use_utf8;
    Optional<DeprecatedString> m_parsed_playlist_title;
};
