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

TranslationUnitRef SpecificationParsingContext::translation_unit()
{
    return m_translation_unit;
}

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

template<typename Func>
auto SpecificationParsingContext::with_new_step_list_nesting_level(Func&& func)
{
    TemporaryChange change(m_step_list_nesting_level, m_step_list_nesting_level + 1);
    return func();
}

int SpecificationParsingContext::step_list_nesting_level() const
{
    return m_step_list_nesting_level;
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

    TextParseErrorOr<Tree> parse_result = TextParseError {};
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
                    if (!step_creation_result.has_value())
                        all_steps_parsed = false;
                    else
                        step_expressions.append(step_creation_result.release_value().tree());
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
            [&](OneOf<ClauseHeader::AbstractOperation, ClauseHeader::Accessor> auto const&) {
                result = make<SpecFunction>(move(specification_clause));
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
    auto parse_result = parser.parse_clause_header();
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

bool SpecFunction::post_initialize(XML::Node const* element)
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

void SpecFunction::do_collect(TranslationUnitRef translation_unit)
{
    translation_unit->adopt_function(make_ref_counted<FunctionDefinition>(m_name, m_algorithm.tree(), move(m_arguments)));
}

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

    m_specification = Specification::create(ctx, &root);
    m_specification->collect_into(translation_unit);
}
}
