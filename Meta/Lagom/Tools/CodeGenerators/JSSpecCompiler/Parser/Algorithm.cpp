/*
 * Copyright (c) 2023-2024, Dan Klishch <danilklishch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Parser/Lexer.h"
#include "Parser/SpecificationParsing.h"
#include "Parser/XMLUtils.h"

namespace JSSpecCompiler {

Optional<Algorithm> Algorithm::create(SpecificationParsingContext& ctx, XML::Node const* element)
{
    VERIFY(element->as_element().name == tag_emu_alg);

    Vector<XML::Node const*> steps_list;
    for (auto const& child : element->as_element().children) {
        child->content.visit(
            [&](XML::Node::Element const& element) {
                if (element.name == tag_ol) {
                    steps_list.append(child);
                    return;
                }

                ctx.diag().error(ctx.location_from_xml_offset(child->offset),
                    "<{}> should not be a child of <emu-alg>"sv, element.name);
            },
            [&](XML::Node::Text const&) {
                if (!contains_empty_text(child)) {
                    ctx.diag().error(ctx.location_from_xml_offset(child->offset),
                        "non-empty text node should not be a child of <emu-alg>");
                }
            },
            [&](auto const&) {});
    }

    if (steps_list.size() != 1) {
        ctx.diag().error(ctx.location_from_xml_offset(element->offset),
            "<emu-alg> should have exactly one <ol> child"sv);
        return {};
    }

    auto steps_creation_result = AlgorithmStepList::create(ctx, steps_list[0]);
    if (steps_creation_result.has_value()) {
        Algorithm algorithm;
        algorithm.m_tree = steps_creation_result.release_value().tree();
        return algorithm;
    }
    return {};
}

}
