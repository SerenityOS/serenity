/*
 * Copyright (c) 2021, Cesar Torres <shortanemoia@protonmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteString.h>
#include <AK/Optional.h>
#include <AK/RefPtr.h>
#include <AK/StringView.h>
#include <AK/Vector.h>

// Extended M3U fields (de facto standard)
struct M3UExtendedInfo {
    Optional<u32> track_length_in_seconds;
    Optional<ByteString> track_display_title;
    Optional<ByteString> group_name;
    Optional<ByteString> album_title;
    Optional<ByteString> album_artist;
    Optional<ByteString> album_genre;
    Optional<u64> file_size_in_bytes;
    Optional<ReadonlyBytes> embedded_mp3;
    Optional<ByteString> cover_path;
};

struct M3UEntry {
    ByteString path;
    Optional<M3UExtendedInfo> extended_info;
};

class M3UParser {
public:
    static NonnullOwnPtr<M3UParser> from_file(StringView path);
    static NonnullOwnPtr<M3UParser> from_memory(ByteString const& m3u_contents, bool utf8);

    NonnullOwnPtr<Vector<M3UEntry>> parse(bool include_extended_info);

    Optional<ByteString>& get_playlist_title_metadata() { return m_parsed_playlist_title; }

    M3UParser();

private:
    ByteString m_m3u_raw_data;
    ByteString m_playlist_path;
    bool m_use_utf8;
    Optional<ByteString> m_parsed_playlist_title;
};
