/*
 * Copyright (c) 2025, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>

#include <LibLine/Editor.h>

TEST_CASE(count_ascii_glyphs_u8)
{
    constexpr auto string = "Hello, World!"sv; // length in bytes: 13, code points: 13, glyphs: 13
    auto metrics = Line::Editor::actual_rendered_string_metrics(string);
    EXPECT_EQ(metrics.grapheme_breaks.size(), 13u);
    EXPECT_EQ(metrics.line_metrics.size(), 1u);
    EXPECT_EQ(metrics.line_metrics[0].length, 13u);
    EXPECT_EQ(metrics.line_metrics[0].visible_length, 13u);
}

TEST_CASE(count_ascii_glyphs_u32)
{
    constexpr u32 string[] = { 'H', 'e', 'l', 'l', 'o', ',', ' ', 'W', 'o', 'r', 'l', 'd', '!' }; // length in code points: 13, glyphs: 13
    auto metrics = Line::Editor::actual_rendered_string_metrics(Utf32View(string));
    EXPECT_EQ(metrics.grapheme_breaks.size(), 13u);
    EXPECT_EQ(metrics.line_metrics.size(), 1u);
    EXPECT_EQ(metrics.line_metrics[0].length, 13u);
    EXPECT_EQ(metrics.line_metrics[0].visible_length, 13u);
}

TEST_CASE(count_simple_multibyte_glyphs_u8)
{
    constexpr auto string = "Héllo, Wörld!"sv; // length in bytes: 15, code points: 13, glyphs: 13
    auto metrics = Line::Editor::actual_rendered_string_metrics(string);
    EXPECT_EQ(metrics.grapheme_breaks.size(), 13u);
    EXPECT_EQ(metrics.line_metrics.size(), 1u);
    EXPECT_EQ(metrics.line_metrics[0].length, 13u);
    EXPECT_EQ(metrics.line_metrics[0].visible_length, 13u);
}

TEST_CASE(count_simple_multibyte_glyphs_u32)
{
    constexpr u32 string[] = { 'H', 0xe9, 'l', 'l', 'o', ',', ' ', 'W', 0xf6, 'r', 'l', 'd', '!' }; // length in code points: 13, glyphs: 13
    auto metrics = Line::Editor::actual_rendered_string_metrics(Utf32View(string));
    EXPECT_EQ(metrics.grapheme_breaks.size(), 13u);
    EXPECT_EQ(metrics.line_metrics.size(), 1u);
    EXPECT_EQ(metrics.line_metrics[0].length, 13u);
    EXPECT_EQ(metrics.line_metrics[0].visible_length, 13u);
}

TEST_CASE(count_multi_codepoint_glyphs_u8)
{
    constexpr auto string = "Héllo, Wörld! 👩‍💻"sv; // length in bytes: 25, code points: 17, glyphs: 15
    auto metrics = Line::Editor::actual_rendered_string_metrics(string);
    EXPECT_EQ(metrics.grapheme_breaks.size(), 15u);
    EXPECT_EQ(metrics.line_metrics.size(), 1u);
    EXPECT_EQ(metrics.line_metrics[0].length, 17u);
    EXPECT_EQ(metrics.line_metrics[0].visible_length, 17u);
}

TEST_CASE(count_jp_glyphs_u8)
{
    {
        constexpr auto string = "コンニチハ、ワールド！"sv; // length in bytes: 33, code points: 11, glyphs: 11
        auto metrics = Line::Editor::actual_rendered_string_metrics(string);
        EXPECT_EQ(metrics.grapheme_breaks.size(), 11u);
        EXPECT_EQ(metrics.line_metrics.size(), 1u);
        EXPECT_EQ(metrics.line_metrics[0].length, 11u);
        EXPECT_EQ(metrics.line_metrics[0].visible_length, 11u);
    }

    {
        constexpr auto string = "がぎぐげご"sv; // length in bytes: 18, code points: 10, glyphs: 5
        auto metrics = Line::Editor::actual_rendered_string_metrics(string);
        EXPECT_EQ(metrics.grapheme_breaks.size(), 5u);
        EXPECT_EQ(metrics.line_metrics.size(), 1u);
        EXPECT_EQ(metrics.line_metrics[0].length, 10u);
        EXPECT_EQ(metrics.line_metrics[0].visible_length, 10u);
    }

    {
        constexpr auto string = "食べる"sv; // length in bytes: 12, code points: 4, glyphs: 3
        auto metrics = Line::Editor::actual_rendered_string_metrics(string);
        EXPECT_EQ(metrics.grapheme_breaks.size(), 3u);
        EXPECT_EQ(metrics.line_metrics.size(), 1u);
        EXPECT_EQ(metrics.line_metrics[0].length, 4u);
        EXPECT_EQ(metrics.line_metrics[0].visible_length, 4u);
    }
}

TEST_CASE(count_multi_codepoint_glyphs_mixed_u8)
{
    constexpr auto string = "Héllo, コンニチハ! 👩‍💻 persian word: کتاب"sv; // length in bytes: 59, code points: 36, glyphs: 34
    auto metrics = Line::Editor::actual_rendered_string_metrics(string);
    EXPECT_EQ(metrics.grapheme_breaks.size(), 34u);
    EXPECT_EQ(metrics.line_metrics.size(), 1u);
    EXPECT_EQ(metrics.line_metrics[0].length, 36u);
    EXPECT_EQ(metrics.line_metrics[0].visible_length, 36u);
}
