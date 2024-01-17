/*
 * Copyright (c) 2023, Dan Klishch <danilklishch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/OwnPtr.h>

#include "AST/AST.h"
#include "CompilationPipeline.h"
#include "Forward.h"
#include "Parser/ParseError.h"
#include "Parser/TextParser.h"
#include "Parser/Token.h"

namespace JSSpecCompiler {

class SpecificationParsingContext {
    AK_MAKE_NONCOPYABLE(SpecificationParsingContext);
    AK_MAKE_NONMOVABLE(SpecificationParsingContext);

public:
    SpecificationParsingContext(TranslationUnitRef translation_unit)
        : m_translation_unit(translation_unit)
    {
    }

    DiagnosticEngine& diag();

    template<typename Func>
    auto with_new_logical_scope(Func&& func);
    LogicalLocation& current_logical_scope();

    Location file_scope() const;
    Location location_from_xml_offset(XML::Offset offset) const;

private:
    TranslationUnitRef m_translation_unit;
    RefPtr<LogicalLocation> m_current_logical_scope;
};

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
    static Optional<Algorithm> create(SpecificationParsingContext& ctx, XML::Node const* element);

    AlgorithmStepList m_steps;
    Tree m_tree = error_tree;
};

class SpecificationClause {
    AK_MAKE_DEFAULT_MOVABLE(SpecificationClause);

public:
    static NonnullOwnPtr<SpecificationClause> create(SpecificationParsingContext& ctx, XML::Node const* element);

    virtual ~SpecificationClause() = default;

    void collect_into(TranslationUnitRef translation_unit);

protected:
    virtual bool post_initialize(SpecificationParsingContext& /*ctx*/, XML::Node const* /*element*/) { return true; }
    virtual void do_collect(TranslationUnitRef /*translation_unit*/) { }

    ClauseHeader m_header;

private:
    SpecificationClause() = default;
    ParseErrorOr<void> parse_header(XML::Node const* element);
    void parse(SpecificationParsingContext& ctx, XML::Node const* element);

    Vector<NonnullOwnPtr<SpecificationClause>> m_subclauses;
};

class SpecFunction : public SpecificationClause {
public:
    SpecFunction(SpecificationClause&& clause)
        : SpecificationClause(move(clause))
    {
    }

protected:
    bool post_initialize(SpecificationParsingContext& ctx, XML::Node const* element) override;
    void do_collect(TranslationUnitRef translation_unit) override;

private:
    StringView m_section_number;
    StringView m_id;
    StringView m_name;

    Vector<FunctionArgument> m_arguments;
    Algorithm m_algorithm;
};

class Specification {
public:
    static Specification create(SpecificationParsingContext& ctx, XML::Node const* element);

    void collect_into(TranslationUnitRef translation_unit);

private:
    void parse(SpecificationParsingContext& ctx, XML::Node const* element);

    Vector<NonnullOwnPtr<SpecificationClause>> m_clauses;
};

class SpecParsingStep : public CompilationStep {
public:
    SpecParsingStep();
    ~SpecParsingStep();

    void run(TranslationUnitRef translation_unit) override;

private:
    OwnPtr<XML::Document> m_document;
    ByteBuffer m_input;
};

}
