/*
 * Copyright (c) 2023-2024, Dan Klishch <danilklishch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Parser/Lexer.h"
#include "Parser/SpecificationParsing.h"
#include "Parser/XMLUtils.h"

namespace JSSpecCompiler {

bool SpecificationFunction::post_initialize(XML::Node const* element)
{
    VERIFY(element->as_element().name == tag_emu_clause);

    auto& ctx = context();

    auto maybe_id = get_attribute_by_name(element, attribute_id);
    if (!maybe_id.has_value()) {
        ctx.diag().error(ctx.location_from_xml_offset(element->offset),
            "no id attribute");
    } else {
        m_id = maybe_id.value();
    }

    m_header.header.visit(
        [&](ClauseHeader::AbstractOperation const& abstract_operation) {
            auto maybe_abstract_operation_id = get_attribute_by_name(element, attribute_aoid);
            if (maybe_abstract_operation_id.has_value())
                m_name = MUST(String::from_utf8(maybe_abstract_operation_id.value()));

            auto const& [function_name, arguments] = abstract_operation;
            m_arguments = arguments;

            if (m_name != function_name) {
                ctx.diag().warn(ctx.location_from_xml_offset(element->offset),
                    "function name in header and <emu-clause>[aoid] do not match");
            }
        },
        [&](ClauseHeader::Accessor const& accessor) {
            m_name = MUST(String::formatted("%get {}%", MUST(String::join("."sv, accessor.qualified_name))));
        },
        [&](ClauseHeader::Method const& method) {
            m_name = MUST(String::formatted("%{}%", MUST(String::join("."sv, method.qualified_name))));
            m_arguments = method.arguments;
        },
        [&](auto const&) {
            VERIFY_NOT_REACHED();
        });

    Vector<XML::Node const*> algorithm_nodes;

    for (auto const& child : element->as_element().children) {
        child->content.visit(
            [&](XML::Node::Element const& element) {
                if (element.name == tag_h1) {
                    // Processed in SpecificationClause
                } else if (element.name == tag_p) {
                    ctx.diag().warn(ctx.location_from_xml_offset(child->offset),
                        "prose is ignored");
                } else if (element.name == tag_emu_alg) {
                    algorithm_nodes.append(child);
                } else {
                    ctx.diag().error(ctx.location_from_xml_offset(child->offset),
                        "<{}> should not be a child of <emu-clause> specifing function"sv, element.name);
                }
            },
            [&](auto const&) {});
    }

    if (algorithm_nodes.size() != 1) {
        ctx.diag().error(ctx.location_from_xml_offset(element->offset),
            "<emu-clause> specifing function should have exactly one <emu-alg> child"sv);
        return false;
    }

    auto maybe_algorithm = Algorithm::create(ctx, algorithm_nodes[0]);
    if (maybe_algorithm.has_value()) {
        m_algorithm = maybe_algorithm.release_value();
        return true;
    } else {
        return false;
    }
}

void SpecificationFunction::do_collect(TranslationUnitRef translation_unit)
{
    translation_unit->adopt_function(make_ref_counted<FunctionDefinition>(m_name, m_algorithm.tree(), move(m_arguments)));
}

}
