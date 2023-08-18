/*
 * Copyright (c) 2023, Dan Klishch <danilklishch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Parser/ParseError.h"
#include "Parser/Token.h"

namespace JSSpecCompiler {

inline constexpr StringView tag_emu_alg = "emu-alg"sv;
inline constexpr StringView tag_emu_clause = "emu-clause"sv;
inline constexpr StringView tag_emu_val = "emu-val"sv;
inline constexpr StringView tag_emu_xref = "emu-xref"sv;
inline constexpr StringView tag_h1 = "h1"sv;
inline constexpr StringView tag_li = "li"sv;
inline constexpr StringView tag_ol = "ol"sv;
inline constexpr StringView tag_p = "p"sv;
inline constexpr StringView tag_span = "span"sv;
inline constexpr StringView tag_var = "var"sv;

inline constexpr StringView attribute_aoid = "aoid"sv;
inline constexpr StringView attribute_class = "class"sv;
inline constexpr StringView attribute_id = "id"sv;

inline constexpr StringView class_secnum = "secnum"sv;

ParseErrorOr<void> tokenize_string(XML::Node const* node, StringView view, Vector<Token>& tokens);

struct TokenizeTreeResult {
    Vector<Token> tokens;
    XML::Node const* substeps = nullptr;
};

ParseErrorOr<TokenizeTreeResult> tokenize_tree(XML::Node const* node, bool allow_substeps = false);

}
