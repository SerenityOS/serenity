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

    TranslationUnitRef translation_unit();
    DiagnosticEngine& diag();

    template<typename Func>
    auto with_new_logical_scope(Func&& func);
    LogicalLocation& current_logical_scope();

    template<typename Func>
    auto with_new_step_list_nesting_level(Func&& func);
    int step_list_nesting_level() const;

    Location file_scope() const;
    Location location_from_xml_offset(LineTrackingLexer::Position position) const;

private:
    TranslationUnitRef m_translation_unit;
    RefPtr<LogicalLocation> m_current_logical_scope;
    int m_step_list_nesting_level = 0;
};

class AlgorithmStepList {
public:
    static Optional<AlgorithmStepList> create(SpecificationParsingContext& ctx, XML::Node const* element);

    Tree tree() const { return m_expression; }

private:
    static void update_logical_scope_for_step(SpecificationParsingContext& ctx, LogicalLocation const& parent_scope, int step_number);

    Tree m_expression = error_tree;
};

class AlgorithmStep {
public:
    static Optional<AlgorithmStep> create(SpecificationParsingContext& ctx, XML::Node const* node);

    Tree tree() const { return m_expression; }

private:
    AlgorithmStep(SpecificationParsingContext& ctx)
        : m_ctx(ctx)
    {
    }

    bool parse();

    SpecificationParsingContext& m_ctx;
    Vector<Token> m_tokens;
    XML::Node const* m_node;
    Tree m_expression = error_tree;
    NullableTree m_substeps;
};

class Algorithm {
public:
    static Optional<Algorithm> create(SpecificationParsingContext& ctx, XML::Node const* element);

    Tree tree() const { return m_tree; }

private:
    Tree m_tree = error_tree;
};

class SpecificationClause {
    AK_MAKE_DEFAULT_MOVABLE(SpecificationClause);

public:
    static NonnullOwnPtr<SpecificationClause> create(SpecificationParsingContext& ctx, XML::Node const* element);

    virtual ~SpecificationClause() = default;

    void collect_into(TranslationUnitRef translation_unit);

protected:
    virtual bool post_initialize(XML::Node const* /*element*/) { return true; }
    virtual void do_collect(TranslationUnitRef /*translation_unit*/) { }

    SpecificationParsingContext& context() { return *m_ctx_pointer; }

    ClauseHeader m_header;

private:
    SpecificationClause(SpecificationParsingContext& ctx)
        : m_ctx_pointer(&ctx)
    {
    }

    Optional<FailedTextParseDiagnostic> parse_header(XML::Node const* element);
    void parse(XML::Node const* element);

    TextParser::ClauseHasAoidAttribute m_clause_has_aoid_attribute;
    SpecificationParsingContext* m_ctx_pointer;
    Vector<NonnullOwnPtr<SpecificationClause>> m_subclauses;
};

class SpecFunction : public SpecificationClause {
public:
    SpecFunction(SpecificationClause&& clause)
        : SpecificationClause(move(clause))
    {
    }

protected:
    bool post_initialize(XML::Node const* element) override;
    void do_collect(TranslationUnitRef translation_unit) override;

private:
    StringView m_id;
    String m_name;

    Vector<FunctionArgument> m_arguments;
    Algorithm m_algorithm;
};

class Specification {
public:
    static NonnullOwnPtr<Specification> create(SpecificationParsingContext& ctx, XML::Node const* element);

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
    OwnPtr<Specification> m_specification;

    ByteBuffer m_input;
};

}
