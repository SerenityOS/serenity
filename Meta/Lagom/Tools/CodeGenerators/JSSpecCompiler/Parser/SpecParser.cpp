/*
 * Copyright (c) 2023, Dan Klishch <danilklishch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/NonnullOwnPtr.h>
#include <LibCore/File.h>
#include <LibXML/Parser/Parser.h>

#include "Function.h"
#include "Parser/Lexer.h"
#include "Parser/SpecParser.h"
#include "Parser/TextParser.h"
#include "Parser/XMLUtils.h"

namespace JSSpecCompiler {

DiagnosticEngine& SpecificationParsingContext::diag()
{
    return m_translation_unit->diag();
}

template<typename Func>
auto SpecificationParsingContext::with_new_logical_scope(Func&& func)
{
    TemporaryChange<RefPtr<LogicalLocation>> change(m_current_logical_scope, make_ref_counted<LogicalLocation>());
    return func();
}

LogicalLocation& SpecificationParsingContext::current_logical_scope()
{
    return *m_current_logical_scope;
}

Location SpecificationParsingContext::file_scope() const
{
    return { .filename = m_translation_unit->filename() };
}

Location SpecificationParsingContext::location_from_xml_offset(XML::Offset offset) const
{
    return {
        .filename = m_translation_unit->filename(),
        .line = offset.line,
        .column = offset.column,
        .logical_location = m_current_logical_scope,
    };
}

ParseErrorOr<AlgorithmStep> AlgorithmStep::create(XML::Node const* node)
{
    VERIFY(node->as_element().name == tag_li);

    auto [tokens, substeps] = TRY(tokenize_tree(node, true));
    AlgorithmStep result { .m_tokens = move(tokens), .m_node = node };

    if (substeps)
        result.m_substeps = TRY(AlgorithmStepList::create(substeps->as_element())).m_expression;

    result.m_expression = TRY(result.parse());
    return result;
}

ParseErrorOr<Tree> AlgorithmStep::parse()
{
    TextParser parser(m_tokens, m_node);

    if (m_substeps)
        return parser.parse_step_with_substeps(RefPtr(m_substeps).release_nonnull());
    else
        return parser.parse_step_without_substeps();
}

ParseErrorOr<AlgorithmStepList> AlgorithmStepList::create(XML::Node::Element const& element)
{
    VERIFY(element.name == tag_ol);

    AlgorithmStepList result;
    auto& steps = result.m_steps;

    Vector<Tree> step_expressions;

    for (auto const& child : element.children) {
        TRY(child->content.visit(
            [&](XML::Node::Element const& element) -> ParseErrorOr<void> {
                if (element.name != tag_li)
                    return ParseError::create("<emu-alg> <ol> > :not(<li>) should not match any elements"sv, child);
                steps.append(TRY(AlgorithmStep::create(child)));
                step_expressions.append(steps.last().m_expression);
                return {};
            },
            [&](XML::Node::Text const&) -> ParseErrorOr<void> {
                if (!contains_empty_text(child))
                    return ParseError::create("<emu-alg> <ol> should not have non-empty child text nodes"sv, child);
                return {};
            },
            move(ignore_comments)));
    }

    result.m_expression = make_ref_counted<TreeList>(move(step_expressions));

    return result;
}

ParseErrorOr<Algorithm> Algorithm::create(XML::Node const* node)
{
    VERIFY(node->as_element().name == tag_emu_alg);

    XML::Node::Element const* steps_list = nullptr;
    for (auto const& child : node->as_element().children) {
        TRY(child->content.visit(
            [&](XML::Node::Element const& element) -> ParseErrorOr<void> {
                if (element.name == tag_ol) {
                    if (steps_list != nullptr)
                        return ParseError::create("<emu-alg> should have exactly one <ol> child"sv, child);
                    steps_list = &element;
                    return {};
                } else {
                    return ParseError::create("<emu-alg> should not have children other than <ol>"sv, child);
                }
            },
            [&](XML::Node::Text const&) -> ParseErrorOr<void> {
                if (!contains_empty_text(child))
                    return ParseError::create("<emu-alg> should not have non-empty child text nodes"sv, child);
                return {};
            },
            move(ignore_comments)));
    }

    if (steps_list == nullptr)
        return ParseError::create("<emu-alg> should have exactly one <ol> child"sv, node);

    Algorithm algorithm;
    algorithm.m_steps = TRY(AlgorithmStepList::create(*steps_list));
    algorithm.m_tree = algorithm.m_steps.m_expression;

    return algorithm;
}

NonnullOwnPtr<SpecificationClause> SpecificationClause::create(SpecificationParsingContext& ctx, XML::Node const* element)
{
    return ctx.with_new_logical_scope([&] {
        VERIFY(element->as_element().name == tag_emu_clause);

        SpecificationClause specification_clause;
        specification_clause.parse(ctx, element);

        OwnPtr<SpecificationClause> result;

        specification_clause.m_header.header.visit(
            [&](AK::Empty const&) {
                result = make<SpecificationClause>(move(specification_clause));
            },
            [&](ClauseHeader::FunctionDefinition const&) {
                result = make<SpecFunction>(move(specification_clause));
            });

        if (!result->post_initialize(ctx, element))
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

ParseErrorOr<void> SpecificationClause::parse_header(XML::Node const* element)
{
    VERIFY(element->as_element().name == tag_h1);
    auto tokens = TRY(tokenize_tree(element));
    TextParser parser(tokens.tokens, element);
    m_header = TRY(parser.parse_clause_header());
    return {};
}

void SpecificationClause::parse(SpecificationParsingContext& ctx, XML::Node const* element)
{
    u32 child_index = 0;

    Optional<NonnullRefPtr<ParseError>> header_parse_error;

    for (auto const& child : element->as_element().children) {
        child->content.visit(
            [&](XML::Node::Element const& element) {
                if (child_index == 0) {
                    if (element.name != tag_h1) {
                        ctx.diag().error(ctx.location_from_xml_offset(child->offset),
                            "<h1> must be the first child of <emu-clause>");
                        return;
                    }

                    if (auto error = parse_header(child); error.is_error())
                        header_parse_error = error.release_error();
                    else
                        ctx.current_logical_scope().section = MUST(String::from_utf8(m_header.section_number));
                } else {
                    if (element.name == tag_emu_clause) {
                        m_subclauses.append(create(ctx, child));
                        return;
                    }
                    if (header_parse_error.has_value()) {
                        ctx.diag().warn(ctx.location_from_xml_offset(child->offset),
                            "node content will be ignored since section header was not parsed successfully");
                        // TODO: Integrate backtracing parser errors better
                        ctx.diag().note(ctx.location_from_xml_offset(header_parse_error.value()->offset()),
                            "{}", header_parse_error.value()->to_string());
                        header_parse_error.clear();
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

bool SpecFunction::post_initialize(SpecificationParsingContext& ctx, XML::Node const* element)
{
    auto initialization_result = do_post_initialize(ctx, element);
    if (initialization_result.is_error()) {
        // TODO: Integrate backtracing parser errors better
        ctx.diag().error(ctx.location_from_xml_offset(initialization_result.error()->offset()),
            "{}", initialization_result.error()->to_string());
        return false;
    }
    return true;
}

void SpecFunction::do_collect(TranslationUnitRef translation_unit)
{
    translation_unit->adopt_function(make_ref_counted<FunctionDefinition>(m_name, m_algorithm.m_tree, move(m_arguments)));
}

ParseErrorOr<void> SpecFunction::do_post_initialize(SpecificationParsingContext& ctx, XML::Node const* element)
{
    VERIFY(element->as_element().name == tag_emu_clause);

    m_id = TRY(get_attribute_by_name(element, attribute_id));
    m_name = TRY(get_attribute_by_name(element, attribute_aoid));

    m_section_number = m_header.section_number;
    auto const& [function_name, arguments] = m_header.header.get<ClauseHeader::FunctionDefinition>();

    if (m_name != function_name) {
        ctx.diag().warn(ctx.location_from_xml_offset(element->offset),
            "function name in header and <emu-clause>[aoid] do not match");
    }
    m_arguments = arguments;

    u32 children_count = 0;

    XML::Node const* algorithm_node = nullptr;
    XML::Node const* prose_node = nullptr;

    for (auto const& child : element->as_element().children) {
        TRY(child->content.visit(
            [&](XML::Node::Element const& element) -> ParseErrorOr<void> {
                if (element.name == tag_h1) {
                    if (children_count != 0)
                        return ParseError::create("<h1> can only be the first child of <emu-clause>"sv, child);
                } else if (element.name == tag_p) {
                    if (prose_node == nullptr)
                        prose_node = child;
                } else if (element.name == tag_emu_alg) {
                    algorithm_node = child;
                } else {
                    return ParseError::create("Unknown child of <emu-clause>"sv, child);
                }
                ++children_count;
                return {};
            },
            [&](XML::Node::Text const&) -> ParseErrorOr<void> {
                if (!contains_empty_text(child)) {
                    return ParseError::create("<emu-clause> should not have non-empty child text nodes"sv, child);
                }
                return {};
            },
            move(ignore_comments)));
    }

    if (algorithm_node == nullptr)
        return ParseError::create("No <emu-alg>"sv, element);

    if (prose_node) {
        ctx.diag().warn(ctx.location_from_xml_offset(element->offset),
            "prose is ignored");
    }

    m_algorithm = TRY(Algorithm::create(algorithm_node));

    return {};
}

Specification Specification::create(SpecificationParsingContext& ctx, XML::Node const* element)
{
    VERIFY(element->as_element().name == tag_specification);

    Specification specification;
    specification.parse(ctx, element);
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

SpecParsingStep::SpecParsingStep()
    : CompilationStep("parser"sv)
{
}

SpecParsingStep::~SpecParsingStep() = default;

void SpecParsingStep::run(TranslationUnitRef translation_unit)
{
    SpecificationParsingContext ctx(translation_unit);
    auto filename = translation_unit->filename();

    auto file_or_error = Core::File::open_file_or_standard_stream(filename, Core::File::OpenMode::Read);
    if (file_or_error.is_error()) {
        ctx.diag().fatal_error(Location::global_scope(),
            "unable to open '{}': {}", filename, file_or_error.error());
        return;
    }

    auto input_or_error = file_or_error.value()->read_until_eof();
    if (input_or_error.is_error()) {
        ctx.diag().fatal_error(Location::global_scope(),
            "unable to read '{}': {}", filename, input_or_error.error());
        return;
    }
    m_input = input_or_error.release_value();

    XML::Parser parser { m_input };
    auto document_or_error = parser.parse();
    if (document_or_error.is_error()) {
        ctx.diag().fatal_error(ctx.file_scope(),
            "XML::Parser failed to parse input: {}", document_or_error.error());
        ctx.diag().note(ctx.file_scope(),
            "since XML::Parser backtracks on error, the message above is likely to point to the "
            "first tag in the input - use external XML verifier to find out the exact cause of error");
        return;
    }
    m_document = make<XML::Document>(document_or_error.release_value());

    auto const& root = m_document->root();
    if (!root.is_element() || root.as_element().name != tag_specification) {
        ctx.diag().fatal_error(ctx.location_from_xml_offset(root.offset),
            "document root must be <specification> tag");
        return;
    }

    auto specification = Specification::create(ctx, &root);
    specification.collect_into(translation_unit);
}
}
