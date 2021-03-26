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
    static NonnullOwnPtr<M3UParser> from_memory(const String& m3u_contents, bool utf8);

    NonnullOwnPtr<Vector<M3UEntry>> parse(bool include_extended_info);

    Optional<String>& get_playlist_title_metadata() { return m_parsed_playlist_title; }

    M3UParser();

private:
    String m_m3u_raw_data;
    String m_playlist_path;
    bool m_use_utf8;
    Optional<String> m_parsed_playlist_title;
};
