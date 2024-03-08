/*
 * Copyright (c) 2023-2024, Dan Klishch <danilklishch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Parser/Lexer.h"
#include "Parser/SpecificationParsing.h"
#include "Parser/XMLUtils.h"

namespace JSSpecCompiler {

NonnullOwnPtr<Specification> Specification::create(SpecificationParsingContext& ctx, XML::Node const* element)
{
    VERIFY(element->as_element().name == tag_specification);

    auto specification = make<Specification>();
    specification->parse(ctx, element);
    return specification;
}

void Specification::collect_into(TranslationUnitRef translation_unit)
{
    for (auto& clause : m_clauses)
        clause->collect_into(translation_unit);
}

void Specification::parse(SpecificationParsingContext& ctx, XML::Node const* element)
{
    for (auto const& child : element->as_element().children) {
        child->content.visit(
            [&](XML::Node::Element const& element) {
                if (element.name == tag_emu_intro) {
                    // Introductory comments are ignored.
                } else if (element.name == tag_emu_clause) {
                    m_clauses.append(SpecificationClause::create(ctx, child));
                } else if (element.name == tag_emu_import) {
                    parse(ctx, child);
                } else {
                    ctx.diag().error(ctx.location_from_xml_offset(child->offset),
                        "<{}> should not be a child of <specification>", element.name);
                }
            },
            [&](XML::Node::Text const&) {
                if (!contains_empty_text(child)) {
                    ctx.diag().error(ctx.location_from_xml_offset(child->offset),
                        "non-empty text node should not be a child of <specification>");
                }
            },
            [&](auto) {});
    }
}

}
