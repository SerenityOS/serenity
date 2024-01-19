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

ParseErrorOr<SpecFunction> SpecFunction::create(XML::Node const* element)
{
    VERIFY(element->as_element().name == tag_emu_clause);

    SpecFunction result;
    result.m_id = TRY(get_attribute_by_name(element, attribute_id));
    result.m_name = TRY(get_attribute_by_name(element, attribute_aoid));

    u32 children_count = 0;
    bool has_definition = false;

    XML::Node const* algorithm_node = nullptr;
    XML::Node const* prose_node = nullptr;

    for (auto const& child : element->as_element().children) {
        TRY(child->content.visit(
            [&](XML::Node::Element const& element) -> ParseErrorOr<void> {
                ++children_count;
                if (element.name == tag_h1) {
                    if (children_count != 1)
                        return ParseError::create("<h1> should be the first child of a <emu-clause>"sv, child);
                    TRY(result.parse_definition(child));
                    has_definition = true;
                } else if (element.name == tag_p) {
                    if (prose_node == nullptr)
                        prose_node = child;
                } else if (element.name == tag_emu_alg) {
                    algorithm_node = child;
                } else {
                    return ParseError::create("Unknown child of <emu-clause>"sv, child);
                }
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
    if (prose_node == nullptr)
        return ParseError::create("No prose element"sv, element);
    if (!has_definition)
        return ParseError::create("Definition was not found"sv, element);

    result.m_algorithm = TRY(Algorithm::create(algorithm_node));
    return result;
}

ParseErrorOr<void> SpecFunction::parse_definition(XML::Node const* element)
{
    auto tokens = TRY(tokenize_tree(element));
    TextParser parser(tokens.tokens, element);

    auto [section_number, function_name, arguments] = TRY(parser.parse_definition());

    if (function_name != m_name)
        return ParseError::create("Function name in definition differs from <emu-clause>[aoid]"sv, element);

    m_section_number = section_number;
    for (auto const& argument : arguments)
        m_arguments.append({ argument });

    return {};
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

    auto spec_function = SpecFunction::create(&m_document->root()).release_value_but_fixme_should_propagate_errors();

    Vector<StringView> argument_names;
    for (auto const& argument : spec_function.m_arguments)
        argument_names.append(argument.name);

    translation_unit->adopt_function(
        make_ref_counted<FunctionDefinition>(spec_function.m_name, spec_function.m_algorithm.m_tree, move(argument_names)));
}

}
