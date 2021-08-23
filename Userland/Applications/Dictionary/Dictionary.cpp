/*
 * Copyright (c) 2021, Robin Allen <r@foon.uk>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <stdio.h>
#include <string.h>

#include <AK/StringBuilder.h>

#include "Dictionary.h"

namespace Dictionary {

static StringView part_of_speech_name(char pos)
{
    switch (pos) {
    case 'n':
        return "noun"sv;
    case 'v':
        return "verb"sv;
    case 's':
    case 'a':
        return "adjective"sv;
    case 'r':
        return "adverb"sv;
    default:
        return "misc"sv;
    }
}

Dictionary::Dictionary(char const* filename)
{
    auto file_or_error = MappedFile::map(filename);
    if (file_or_error.is_error()) {
        m_num_words = 0;
        m_dictionary_bytes = nullptr;
        return;
    }

    m_dictionary_file = file_or_error.value();
    m_dictionary_bytes = (u8*)m_dictionary_file->data();

    Header const* header = (Header const*)m_dictionary_bytes;

    m_num_words = header->num_words;
    m_max_word_length = header->max_word_length;

    m_index = offset_to_pointer(header->index_offset);
    m_word_data_index = offset_to_pointer(header->word_data_index_offset);

    outln("Number of words: {}", m_num_words);
    outln("Max word length: {}", m_max_word_length);

    VERIFY(m_num_words < 1000 * 1000);
    VERIFY(m_max_word_length == 16);
}

StringView Dictionary::word_at_index(int word_index) const
{
    if (0 <= word_index && word_index < m_num_words) {
        char const* c = m_index[word_index];

        int n = strnlen(c, 16);

        return StringView(c, n);
    }
    return ""sv;
}

String Dictionary::definition_of(int word_index) const
{
    if (word_index < 0 || word_index >= m_num_words) {
        return "";
    }

    Offset<WordData> word_data_offset = m_word_data_index[word_index];

    WordData const& word_data = *offset_to_pointer(word_data_offset);

    StringBuilder string_builder;

    string_builder.append(word_at_index(word_index));
    string_builder.append("\n\n");

    char last_part_of_speech = 0;
    int sense_number = 0;

    int n = word_data.num_senses;
    for (int i = 0; i < n; i++) {
        Sense const& sense = word_data.senses[i];

        Definition const& definition = *offset_to_pointer(sense.definition_offset);

        if (definition.part_of_speech != last_part_of_speech) {
            string_builder.append(part_of_speech_name(definition.part_of_speech));
            string_builder.append("\n\n");

            last_part_of_speech = definition.part_of_speech;
            sense_number = 0;
        }

        if (n > 1)
            string_builder.appendff("  {}. ", sense_number + 1);

        string_builder.append(definition.text);
        string_builder.append("\n\n");

        sense_number += 1;
    }

    return string_builder.build();
}

void Dictionary::prefix_query(
    String query,
    int start_index,
    int& first_index,
    int& num_indices) const
{
    if (m_num_words == 0) {
        return;
    }

    first_index = start_index;

    if (query == "") {
        num_indices = m_num_words - start_index;
        return;
    }

    /* First, find the new first_index by padding the query with zeroes
           and finding the first entry that's >= it. */
    int query_length = min(16, query.length());
    char query_chars[16] = {};
    memcpy(query_chars, query.to_lowercase().characters(), query_length);

    for (int letter_index = 0; letter_index < query_length; letter_index++) {

        char query_char = query_chars[letter_index];

        char const* p_item_char = &m_index[first_index][letter_index];

        /* OR with 32 to get the lowercase char */
        while ((*p_item_char | 32) < query_char) {
            if (first_index + 1 == m_num_words)
                break;
            first_index += 1;
            p_item_char += 16;
        }
    }

    char const* item_chars = m_index[first_index];

    /* Now, find the last_index by looking for the first word after first_index
       that doesn't start with the query. */
    int last_index = first_index;

    for (;;) {

        bool is_prefix = true;
        for (int i = 0; i < query_length; i++) {
            if (query_chars[i] && query_chars[i] != (item_chars[i] | 32)) {
                is_prefix = false;
                break;
            }
        }

        if (!is_prefix)
            break;

        if (last_index + 1 == m_num_words)
            break;

        last_index += 1;
        item_chars += 16;
    }

    num_indices = last_index - first_index;
}
}
