/*
 * Copyright (c) 2023-2024, Dan Klishch <danilklishch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Parser/Lexer.h"
#include "Parser/SpecificationParsing.h"

namespace JSSpecCompiler {

Optional<AlgorithmStep> AlgorithmStep::create(SpecificationParsingContext& ctx, XML::Node const* element)
{
    VERIFY(element->as_element().name == tag_li);

    auto [maybe_tokens, substeps] = tokenize_step(ctx, element);

    AlgorithmStep result(ctx);
    result.m_node = element;

    if (substeps) {
        // FIXME: Remove this once macOS Lagom CI updates to Clang >= 16.
        auto substeps_copy = substeps;

        auto step_list = ctx.with_new_step_list_nesting_level([&] {
            return AlgorithmStepList::create(ctx, substeps_copy);
        });
        result.m_substeps = step_list.has_value() ? step_list->tree() : error_tree;
    }

    if (!maybe_tokens.has_value())
        return {};
    result.m_tokens = maybe_tokens.release_value();

    if (!result.parse())
        return {};
    return result;
}

bool AlgorithmStep::parse()
{
    TextParser parser(m_ctx, m_tokens, m_node);

    TextParseErrorOr<NullableTree> parse_result = TextParseError {};
    if (m_substeps)
        parse_result = parser.parse_step_with_substeps(RefPtr(m_substeps).release_nonnull());
    else
        parse_result = parser.parse_step_without_substeps();

    if (parse_result.is_error()) {
        auto [location, message] = parser.get_diagnostic();
        m_ctx.diag().error(location, "{}", message);
        return false;
    } else {
        m_expression = parse_result.release_value();
        return true;
    }
}

}
