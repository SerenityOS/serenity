/*
 * Copyright (c) 2021, Robin Allen <r@foon.uk>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/MappedFile.h>
#include <AK/String.h>
#include <AK/StringView.h>

namespace Dictionary {

class Dictionary {
public:
    Dictionary(char const* filename);

    String definition_of(int word_index) const;

    StringView word_at_index(int word_index) const;

    int word_count() const { return m_num_words; }

    void prefix_query(
        String query,
        int start_index,
        int& first_index,
        int& num_indices) const;

private:
    template<typename T>
    struct Offset {
        u32 value;
    };

    struct Definition {
        char part_of_speech;
        char text[];
    } __attribute__((packed));

    struct Sense {
        uint8_t sensenum;
        Offset<Definition> definition_offset;
    } __attribute__((packed));

    struct WordData {
        u8 num_senses;
        Sense senses[];
    } __attribute__((packed));

    typedef char IndexEntry[16];
    typedef Offset<WordData> WordDataIndexEntry;

    struct Header {
        u32 num_words;
        u32 max_word_length;
        Offset<IndexEntry> index_offset;
        Offset<WordDataIndexEntry> word_data_index_offset;
    };

    RefPtr<MappedFile> m_dictionary_file;
    u8 const* m_dictionary_bytes { nullptr };

    int m_num_words { 0 };
    int m_max_word_length { 0 };

    IndexEntry const* m_index { nullptr };
    WordDataIndexEntry const* m_word_data_index { nullptr };

    template<typename T>
    T const* offset_to_pointer(Offset<T> offset) const
    {
        return (T const*)(m_dictionary_bytes + offset.value);
    }
};

}
