/*
 * Copyright (c) 2021, the SerenityOS developers.
 * Copyright (c) 2021, Leandro A. F. Pereira <leandro@tia.mat.br>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "M3UParser.h"
#include "PlaylistWidget.h"
#include <AK/StringHash.h>
#include <AK/StringView.h>
#include <AK/Vector.h>

class Playlist {
public:
    Playlist()
        : m_model(adopt_ref(*new PlaylistModel()))
    {
    }

    bool load(StringView);

    RefPtr<PlaylistModel> model() { return m_model; }
    int size() { return m_model->items().size(); }

    StringView next();
    StringView previous();

    void set_looping(bool looping) { m_looping = looping; }
    bool looping() const { return m_looping; }

    void set_shuffling(bool shuffling) { m_shuffling = shuffling; }
    bool shuffling() const { return m_shuffling; }

private:
    // This naïve bloom filter is used in the shuffling algorithm to
    // provide random uniformity, avoiding playing items that were recently
    // played.
    class BloomFilter {
    public:
        void reset() { m_bitmap = 0; }
        void add(StringView const key) { m_bitmap |= mask_for_key(key); }
        bool maybe_contains(StringView const key) const
        {
            auto mask = mask_for_key(key);
            return (m_bitmap & mask) == mask;
        }

    private:
        u64 mask_for_key(StringView key) const
        {
            auto key_chars = key.characters_without_null_termination();
            auto hash1 = string_hash(key_chars, key.length(), 0xdeadbeef);
            auto hash2 = string_hash(key_chars, key.length(), 0xbebacafe);
            return 1ULL << (hash1 & 63) | 1ULL << (hash2 & 63);
        }
        u64 m_bitmap { 0 };
    };

    void try_fill_missing_info(Vector<M3UEntry>&, StringView);

    RefPtr<PlaylistModel> m_model;
    bool m_looping { false };
    bool m_shuffling { false };

    BloomFilter m_previously_played_paths;
    int m_next_index_to_play { 0 };
};
