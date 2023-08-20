/*
 * Copyright (c) 2023, Dan Klishch <danilklishch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "AST/AST.h"
#include "Forward.h"
#include "Parser/ParseError.h"
#include "Parser/Token.h"

namespace JSSpecCompiler {
class AlgorithmStepList {
public:
    static ParseErrorOr<AlgorithmStepList> create(XML::Node::Element const& element);

    Vector<AlgorithmStep> m_steps;
    Tree m_expression = error_tree;
};

class AlgorithmStep {
public:
    static ParseErrorOr<AlgorithmStep> create(XML::Node const* node);

    ParseErrorOr<Tree> parse();

    Tree m_expression = error_tree;
    Vector<Token> m_tokens;
    NullableTree m_substeps;
    XML::Node const* m_node;
};

class Algorithm {
public:
    static ParseErrorOr<Algorithm> create(XML::Node const* node);

    AlgorithmStepList m_steps;
    Tree m_tree = error_tree;
};

class SpecFunction {
public:
    struct Argument {
        StringView name;
    };

    static ParseErrorOr<SpecFunction> create(XML::Node const* element);

    ParseErrorOr<void> parse_definition(XML::Node const* element);

    StringView m_section_number;
    StringView m_id;
    StringView m_name;

    Vector<Argument> m_arguments;
    Algorithm m_algorithm;
};

}
