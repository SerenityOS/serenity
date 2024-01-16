/*
 * Copyright (c) 2021, Cesar Torres <shortanemoia@protonmail.com>
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Playlist.h"

#include <AK/LexicalPath.h>
#include <AK/Random.h>
#include <LibAudio/Loader.h>
#include <LibFileSystem/FileSystem.h>
#include <LibGUI/MessageBox.h>

bool Playlist::load(StringView path)
{
    auto parser = M3UParser::from_file(path);
    auto items = parser->parse(true);

    if (items->size() <= 0)
        return false;

    try_fill_missing_info(*items, path);
    for (auto& item : *items)
        m_model->items().append(item);
    m_model->invalidate();

    return true;
}

void Playlist::try_fill_missing_info(Vector<M3UEntry>& entries, StringView path)
{
    LexicalPath playlist_path(path);
    Vector<M3UEntry*> to_delete;

    for (auto& entry : entries) {
        if (!LexicalPath { entry.path }.is_absolute())
            entry.path = ByteString::formatted("{}/{}", playlist_path.dirname(), entry.path);

        if (!entry.extended_info->file_size_in_bytes.has_value()) {
            auto size = FileSystem::size_from_stat(entry.path);
            if (size.is_error())
                continue;
            entry.extended_info->file_size_in_bytes = size.value();
        } else if (!FileSystem::exists(entry.path)) {
            to_delete.append(&entry);
            continue;
        }

        if (!entry.extended_info->track_display_title.has_value())
            entry.extended_info->track_display_title = LexicalPath::title(entry.path);

        if (!entry.extended_info->track_length_in_seconds.has_value()) {
            // TODO: Implement embedded metadata extractor for other audio formats
            if (auto reader = Audio::Loader::create(entry.path); !reader.is_error())
                entry.extended_info->track_length_in_seconds = reader.value()->total_samples() / reader.value()->sample_rate();
        }

        // TODO: Implement a metadata parser for the uncomfortably numerous popular embedded metadata formats
    }

    for (auto& entry : to_delete)
        entries.remove_first_matching([&](M3UEntry& e) { return &e == entry; });
}

StringView Playlist::next()
{
    if (m_next_index_to_play >= size()) {
        if (!looping())
            return {};
        m_next_index_to_play = 0;
    }

    auto next = m_model->items().at(m_next_index_to_play).path;
    if (!shuffling()) {
        m_next_index_to_play++;
        return next;
    }

    // Try a few times getting an item to play that has not been
    // recently played.  But do not try too hard, as we don't want
    // to wait forever.
    int shuffle_try;
    int const max_times_to_try = min(4, size());
    for (shuffle_try = 0; shuffle_try < max_times_to_try; shuffle_try++) {
        if (!m_previously_played_paths.maybe_contains(next))
            break;

        m_next_index_to_play = get_random_uniform(size());
        next = m_model->items().at(m_next_index_to_play).path;
    }
    if (shuffle_try == max_times_to_try) {
        // If we tried too much, maybe it's time to try resetting
        // the bloom filter and start over.
        m_previously_played_paths.reset();
    }

    m_previously_played_paths.add(next);
    return next;
}

StringView Playlist::previous()
{
    m_next_index_to_play--;
    if (m_next_index_to_play < 0) {
        m_next_index_to_play = 0;
        return {};
    }
    return m_model->items().at(m_next_index_to_play).path;
}
