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
        void reset()
        {
            m_bitmap1 = 0;
            m_bitmap2 = 0;
        }
        void add(const StringView key)
        {
            m_bitmap1 |= hash1(key);
            m_bitmap2 |= hash2(key);
        }
        bool maybe_contains(const StringView key)
        {
            if (u32 hash = hash1(key); (hash & m_bitmap1) != hash)
                return false;
            if (u32 hash = hash2(key); (hash & m_bitmap2) != hash)
                return false;
            return true;
        }

    private:
        u32 hash1(StringView key)
        {
            auto key_chars = key.characters_without_null_termination();
            return string_hash(key_chars, key.length(), 0xdeadbeef);
        }
        u32 hash2(StringView key)
        {
            auto key_chars = key.characters_without_null_termination();
            return string_hash(key_chars, key.length(), 0xbebacafe);
        }
        u32 m_bitmap1 { 0 };
        u32 m_bitmap2 { 0 };
    };

    void try_fill_missing_info(Vector<M3UEntry>&, StringView);

    RefPtr<PlaylistModel> m_model;
    bool m_looping { false };
    bool m_shuffling { false };

    BloomFilter m_previously_played_paths;
    int m_next_index_to_play { 0 };
};
