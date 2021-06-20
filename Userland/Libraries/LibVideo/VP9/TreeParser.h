/*
 * Copyright (c) 2021, Hunter Salyer <thefalsehonesty@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "BitStream.h"
#include "Enums.h"
#include "ProbabilityTables.h"
#include "SyntaxElementCounter.h"

namespace Video::VP9 {

class Decoder;

class TreeParser {
public:
    explicit TreeParser(Decoder& decoder)
        : m_decoder(decoder)
    {
    }

    class TreeSelection {
    public:
        union TreeSelectionValue {
            int const* m_tree;
            int m_value;
        };

        TreeSelection(int const* values);
        TreeSelection(int value);

        bool is_single_value() const { return m_is_single_value; }
        int get_single_value() const { return m_value.m_value; }
        int const* get_tree_value() const { return m_value.m_tree; }

    private:
        bool m_is_single_value;
        TreeSelectionValue m_value;
    };

    template<typename T = int>
    T parse_tree(SyntaxElementType type);
    TreeSelection select_tree(SyntaxElementType type);
    u8 select_tree_probability(SyntaxElementType type, u8 node);
    void count_syntax_element(SyntaxElementType type, int value);

private:
    u8 calculate_partition_probability(u8 node);
    u8 calculate_skip_probability();

    Decoder& m_decoder;
    // m_ctx is a member variable because it is required for syntax element counting (section 9.3.4)
    u8 m_ctx { 0 };
};

}
