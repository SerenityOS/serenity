/*
 * Copyright (c) 2021, Dana Burkart <dana.burkart@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "TextAlignment.h"

namespace Markdown {

String TextAlignment::justify(String text, int justification_width, bool ignore_terminal_sequences)
{
    StringBuilder justified_text;

    auto add_text_from_nodes = [&justified_text](RefPtr<WordNode> node) {
        // We stop when next is null since we don't want to add the trailing space
        while (!node->next.is_null()) {
            justified_text.append(node->value);
            node = node->next;
        }
        justified_text.append("\n");
    };

    for (auto sentence : text.split_view('\n')) {
        RefPtr<WordNode> root, current;
        bool at_beginning_of_line = true;
        unsigned long free_space_on_line = justification_width;
        Vector<RefPtr<WordNode>> spaces;

        for (auto word_text : sentence.split_view(' ')) {
            size_t word_length = word_text.length();
            if (ignore_terminal_sequences)
                word_length = unadorned_text_length(word_text);

            if (word_length > free_space_on_line && spaces.size()) {
                spaces.remove(spaces.size() - 1);

                while (free_space_on_line) {
                    auto index = random() % spaces.size();
                    auto space = spaces[index];

                    if (spaces.size() >= free_space_on_line)
                        spaces.remove(index);

                    space->value = String::formatted("{}{}", space->value, " ");
                    free_space_on_line -= 1;
                }

                add_text_from_nodes(root);
                free_space_on_line = justification_width;
                at_beginning_of_line = true;
                spaces = Vector<RefPtr<WordNode>>();
            }

            auto word = try_make_ref_counted<WordNode>(word_text);
            auto space = try_make_ref_counted<WordNode>(" ");

            if (at_beginning_of_line) {
                current = root = word;
                at_beginning_of_line = false;
            } else {
                current->next = word;
                current = current->next;
            }

            current->next = space;
            current = current->next;
            spaces.append(current);

            free_space_on_line -= word_length + 1;
        }

        add_text_from_nodes(root);
    }

    return justified_text.build();
}

size_t TextAlignment::unadorned_text_length(String text)
{
    size_t length = 0;
    bool in_terminal_sequence = false;
    for (auto c : text) {
        if (c == '\e')
            in_terminal_sequence = true;

        if (!in_terminal_sequence)
            length += 1;

        if (in_terminal_sequence && c == 'm')
            in_terminal_sequence = false;
    }

    return length;
}

}
