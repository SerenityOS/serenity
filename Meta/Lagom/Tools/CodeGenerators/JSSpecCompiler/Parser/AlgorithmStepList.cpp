/*
 * Copyright (c) 2023-2024, Dan Klishch <danilklishch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Parser/Lexer.h"
#include "Parser/SpecificationParsing.h"
#include "Parser/XMLUtils.h"

namespace JSSpecCompiler {

Optional<AlgorithmStepList> AlgorithmStepList::create(SpecificationParsingContext& ctx, XML::Node const* element)
{
    VERIFY(element->as_element().name == tag_ol);

    AlgorithmStepList result;

    Vector<Tree> step_expressions;
    bool all_steps_parsed = true;
    int step_number = 0;

    auto const& parent_scope = ctx.current_logical_scope();

    for (auto const& child : element->as_element().children) {
        child->content.visit(
            [&](XML::Node::Element const& element) {
                if (element.name == tag_li) {
                    auto step_creation_result = ctx.with_new_logical_scope([&] {
                        update_logical_scope_for_step(ctx, parent_scope, step_number);
                        return AlgorithmStep::create(ctx, child);
                    });
                    if (!step_creation_result.has_value()) {
                        all_steps_parsed = false;
                    } else {
                        if (auto expression = step_creation_result.release_value().tree())
                            step_expressions.append(expression.release_nonnull());
                    }
                    ++step_number;
                    return;
                }

                ctx.diag().error(ctx.location_from_xml_offset(child->offset),
                    "<{}> should not be a child of algorithm step list"sv, element.name);
            },
            [&](XML::Node::Text const&) {
                if (!contains_empty_text(child)) {
                    ctx.diag().error(ctx.location_from_xml_offset(child->offset),
                        "non-empty text node should not be a child of algorithm step list");
                }
            },
            [&](auto const&) {});
    }

    if (!all_steps_parsed)
        return {};

    result.m_expression = make_ref_counted<TreeList>(move(step_expressions));
    return result;
}

void AlgorithmStepList::update_logical_scope_for_step(SpecificationParsingContext& ctx, LogicalLocation const& parent_scope, int step_number)
{
    int nesting_level = ctx.step_list_nesting_level();
    String list_step_number;

    if (nesting_level == 0 || nesting_level == 3) {
        list_step_number = MUST(String::formatted("{}", step_number + 1));
    } else if (nesting_level == 1 || nesting_level == 4) {
        if (step_number < 26)
            list_step_number = String::from_code_point('a' + step_number);
        else
            list_step_number = MUST(String::formatted("{}", step_number + 1));
    } else {
        list_step_number = MUST(String::from_byte_string(ByteString::roman_number_from(step_number + 1).to_lowercase()));
    }

    auto& scope = ctx.current_logical_scope();
    scope.section = parent_scope.section;

    if (parent_scope.step.is_empty())
        scope.step = list_step_number;
    else
        scope.step = MUST(String::formatted("{}.{}", parent_scope.step, list_step_number));
}

}
