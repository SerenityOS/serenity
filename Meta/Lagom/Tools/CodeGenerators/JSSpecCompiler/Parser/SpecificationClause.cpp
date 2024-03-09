/*
 * Copyright (c) 2023-2024, Dan Klishch <danilklishch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Parser/Lexer.h"
#include "Parser/SpecificationParsing.h"
#include "Parser/XMLUtils.h"

namespace JSSpecCompiler {

NonnullOwnPtr<SpecificationClause> SpecificationClause::create(SpecificationParsingContext& ctx, XML::Node const* element)
{
    return ctx.with_new_logical_scope([&] {
        VERIFY(element->as_element().name == tag_emu_clause);

        SpecificationClause specification_clause(ctx);
        specification_clause.parse(element);

        OwnPtr<SpecificationClause> result;

        specification_clause.m_header.header.visit(
            [&](AK::Empty const&) {
                result = make<SpecificationClause>(move(specification_clause));
            },
            [&](OneOf<AbstractOperationDeclaration, AccessorDeclaration, MethodDeclaration> auto const&) {
                result = make<SpecificationFunction>(move(specification_clause));
            },
            [&](ClauseHeader::PropertiesList const&) {
                result = make<ObjectProperties>(move(specification_clause));
            });

        if (!result->post_initialize(element))
            result = make<SpecificationClause>(move(*result));

        return result.release_nonnull();
    });
}

void SpecificationClause::collect_into(TranslationUnitRef translation_unit)
{
    do_collect(translation_unit);
    for (auto& subclause : m_subclauses)
        subclause->collect_into(translation_unit);
}

Optional<FailedTextParseDiagnostic> SpecificationClause::parse_header(XML::Node const* element)
{
    auto& ctx = *m_ctx_pointer;
    VERIFY(element->as_element().name == tag_h1);

    auto maybe_tokens = tokenize_header(ctx, element);
    if (!maybe_tokens.has_value())
        return {};

    auto const& tokens = maybe_tokens.release_value();

    TextParser parser(ctx, tokens, element);
    auto parse_result = parser.parse_clause_header(m_clause_has_aoid_attribute);
    if (parse_result.is_error()) {
        // Still try to at least scavenge section number.
        if (tokens.size() && tokens[0].type == TokenType::SectionNumber)
            ctx.current_logical_scope().section = MUST(String::from_utf8(tokens[0].data));

        return parser.get_diagnostic();
    }

    m_header = parse_result.release_value();
    ctx.current_logical_scope().section = MUST(String::from_utf8(m_header.section_number));
    return {};
}

void SpecificationClause::parse(XML::Node const* element)
{
    auto& ctx = context();
    u32 child_index = 0;

    bool node_ignored_warning_issued = false;
    Optional<FailedTextParseDiagnostic> header_parse_error;

    m_clause_has_aoid_attribute = element->as_element().attributes.get("aoid").has_value()
        ? TextParser::ClauseHasAoidAttribute::Yes
        : TextParser::ClauseHasAoidAttribute::No;

    for (auto const& child : element->as_element().children) {
        child->content.visit(
            [&](XML::Node::Element const& element) {
                if (child_index == 0) {
                    if (element.name != tag_h1) {
                        ctx.diag().error(ctx.location_from_xml_offset(child->offset),
                            "<h1> must be the first child of <emu-clause>");
                        return;
                    }
                    header_parse_error = parse_header(child);
                } else {
                    if (element.name == tag_h1) {
                        ctx.diag().error(ctx.location_from_xml_offset(child->offset),
                            "<h1> can only be the first child of <emu-clause>");
                        return;
                    }

                    if (element.name == tag_emu_clause) {
                        m_subclauses.append(create(ctx, child));
                        return;
                    }
                    if (!node_ignored_warning_issued && m_header.header.has<AK::Empty>()) {
                        node_ignored_warning_issued = true;
                        ctx.diag().warn(ctx.location_from_xml_offset(child->offset),
                            "node content will be ignored since section header was not parsed successfully");
                        if (header_parse_error.has_value())
                            ctx.diag().note(header_parse_error->location, "{}", header_parse_error->message);
                    }
                }
                ++child_index;
            },
            [&](XML::Node::Text const&) {
                if (!contains_empty_text(child)) {
                    ctx.diag().error(ctx.location_from_xml_offset(child->offset),
                        "non-empty text node should not be a child of <emu-clause>");
                }
            },
            [&](auto) {});
    }
}

}
