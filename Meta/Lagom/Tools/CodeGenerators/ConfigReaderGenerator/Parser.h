/*
 * Copyright (c) 2022, Maciej <sppmacd@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/GenericLexer.h>
#include <AK/String.h>

#include "AST.h"

struct ParserError {
    String message;
};

template<class T>
using ParserErrorOr = ErrorOr<T, ParserError>;

class Parser : public GenericLexer {
public:
    Parser(StringView input, String domain_name)
        : GenericLexer(input)
        , m_domain_name(move(domain_name))
    {
    }

    ParserErrorOr<AST::ConfigFile> parse();

private:
    ParserErrorOr<AST::Group> parse_group(String const& group_name);
    ParserErrorOr<Optional<AST::Annotation>> parse_comment_with_annotation();
    bool consume_comments();

    String m_domain_name;
};
