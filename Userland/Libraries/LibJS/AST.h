/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2020-2022, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2021-2022, David Tuin <davidot@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/FlyString.h>
#include <AK/HashMap.h>
#include <AK/NonnullRefPtrVector.h>
#include <AK/OwnPtr.h>
#include <AK/RefPtr.h>
#include <AK/String.h>
#include <AK/Variant.h>
#include <AK/Vector.h>
#include <LibJS/Bytecode/CodeGenerationError.h>
#include <LibJS/Forward.h>
#include <LibJS/Heap/Handle.h>
#include <LibJS/Runtime/Completion.h>
#include <LibJS/Runtime/EnvironmentCoordinate.h>
#include <LibJS/Runtime/FunctionKind.h>
#include <LibJS/Runtime/PropertyKey.h>
#include <LibJS/Runtime/Reference.h>
#include <LibJS/Runtime/Value.h>
#include <LibJS/SourceRange.h>
#include <LibRegex/Regex.h>

namespace JS {

class Declaration;
class ClassDeclaration;
class FunctionDeclaration;
class Identifier;
class MemberExpression;
class VariableDeclaration;

template<class T, class... Args>
static inline NonnullRefPtr<T>
create_ast_node(SourceRange range, Args&&... args)
{
    return adopt_ref(*new T(range, forward<Args>(args)...));
}

class ASTNode : public RefCounted<ASTNode> {
public:
    virtual ~ASTNode() { }
    virtual Completion execute(Interpreter&, GlobalObject&) const = 0;
    virtual Bytecode::CodeGenerationErrorOr<void> generate_bytecode(Bytecode::Generator&) const;
    virtual void dump(int indent) const;

    SourceRange const& source_range() const { return m_source_range; }
    SourceRange& source_range() { return m_source_range; }

    String class_name() const;

    template<typename T>
    bool fast_is() const = delete;

    virtual bool is_new_expression() const { return false; }
    virtual bool is_member_expression() const { return false; }
    virtual bool is_super_expression() const { return false; }
    virtual bool is_function_expression() const { return false; }
    virtual bool is_class_expression() const { return false; }
    virtual bool is_expression_statement() const { return false; }
    virtual bool is_identifier() const { return false; }
    virtual bool is_scope_node() const { return false; }
    virtual bool is_program() const { return false; }
    virtual bool is_class_declaration() const { return false; }
    virtual bool is_function_declaration() const { return false; }
    virtual bool is_variable_declaration() const { return false; }
    virtual bool is_import_call() const { return false; }
    virtual bool is_array_expression() const { return false; }
    virtual bool is_object_expression() const { return false; }
    virtual bool is_string_literal() const { return false; }
    virtual bool is_update_expression() const { return false; }
    virtual bool is_call_expression() const { return false; }
    virtual bool is_labelled_statement() const { return false; }
    virtual bool is_iteration_statement() const { return false; }
    virtual bool is_class_method() const { return false; }

protected:
    explicit ASTNode(SourceRange source_range)
        : m_source_range(source_range)
    {
    }

private:
    SourceRange m_source_range;
};

class Statement : public ASTNode {
public:
    explicit Statement(SourceRange source_range)
        : ASTNode(source_range)
    {
    }
};

// 14.13 Labelled Statements, https://tc39.es/ecma262/#sec-labelled-statements
class LabelledStatement : public Statement {
public:
    LabelledStatement(SourceRange source_range, FlyString label, NonnullRefPtr<Statement> labelled_item)
        : Statement(source_range)
        , m_label(move(label))
        , m_labelled_item(move(labelled_item))
    {
    }

    virtual Completion execute(Interpreter&, GlobalObject&) const override;
    virtual void dump(int indent) const override;

    FlyString const& label() const { return m_label; }
    FlyString& label() { return m_label; }
    NonnullRefPtr<Statement> const& labelled_item() const { return m_labelled_item; }
    NonnullRefPtr<Statement>& labelled_item() { return m_labelled_item; }

private:
    virtual bool is_labelled_statement() const final { return true; }

    FlyString m_label;
    NonnullRefPtr<Statement> m_labelled_item;
};

class LabelableStatement : public Statement {
public:
    using Statement::Statement;

    Vector<FlyString> const& labels() const { return m_labels; }
    virtual void add_label(FlyString string) { m_labels.append(move(string)); }

protected:
    Vector<FlyString> m_labels;
};

class IterationStatement : public Statement {
public:
    using Statement::Statement;

    virtual Completion loop_evaluation(Interpreter&, GlobalObject&, Vector<FlyString> const&) const = 0;

private:
    virtual bool is_iteration_statement() const final { return true; }
};

class EmptyStatement final : public Statement {
public:
    explicit EmptyStatement(SourceRange source_range)
        : Statement(source_range)
    {
    }
    Completion execute(Interpreter&, GlobalObject&) const override;
    virtual Bytecode::CodeGenerationErrorOr<void> generate_bytecode(Bytecode::Generator&) const override;
};

class ErrorStatement final : public Statement {
public:
    explicit ErrorStatement(SourceRange source_range)
        : Statement(source_range)
    {
    }
    Completion execute(Interpreter&, GlobalObject&) const override { return {}; }
};

class ExpressionStatement final : public Statement {
public:
    ExpressionStatement(SourceRange source_range, NonnullRefPtr<Expression> expression)
        : Statement(source_range)
        , m_expression(move(expression))
    {
    }

    virtual Completion execute(Interpreter&, GlobalObject&) const override;
    virtual void dump(int indent) const override;
    virtual Bytecode::CodeGenerationErrorOr<void> generate_bytecode(Bytecode::Generator&) const override;

    Expression const& expression() const { return m_expression; };

private:
    virtual bool is_expression_statement() const override { return true; }

    NonnullRefPtr<Expression> m_expression;
};

template<typename Func, typename... Args>
concept ThrowCompletionOrVoidFunction = requires(Func func, Args... args)
{
    {
        func(args...)
    }
    ->SameAs<ThrowCompletionOr<void>>;
};

template<typename... Args>
class ThrowCompletionOrVoidCallback : public Function<ThrowCompletionOr<void>(Args...)> {
public:
    template<typename CallableType>
    ThrowCompletionOrVoidCallback(CallableType&& callable) requires(VoidFunction<CallableType, Args...>)
        : Function<ThrowCompletionOr<void>(Args...)>([callable = forward<CallableType>(callable)](Args... args) {
            callable(args...);
            return ThrowCompletionOr<void> {};
        })
    {
    }

    template<typename CallableType>
    ThrowCompletionOrVoidCallback(CallableType&& callable) requires(ThrowCompletionOrVoidFunction<CallableType, Args...>)
        : Function<ThrowCompletionOr<void>(Args...)>(forward<CallableType>(callable))
    {
    }
};

class ScopeNode : public Statement {
public:
    template<typename T, typename... Args>
    T& append(SourceRange range, Args&&... args)
    {
        auto child = create_ast_node<T>(range, forward<Args>(args)...);
        m_children.append(move(child));
        return static_cast<T&>(m_children.last());
    }
    void append(NonnullRefPtr<Statement> child)
    {
        m_children.append(move(child));
    }

    NonnullRefPtrVector<Statement> const& children() const { return m_children; }
    virtual void dump(int indent) const override;
    virtual Bytecode::CodeGenerationErrorOr<void> generate_bytecode(Bytecode::Generator&) const override;

    Completion evaluate_statements(Interpreter& interpreter, GlobalObject& global_object) const;

    void add_var_scoped_declaration(NonnullRefPtr<Declaration> variables);
    void add_lexical_declaration(NonnullRefPtr<Declaration> variables);
    void add_hoisted_function(NonnullRefPtr<FunctionDeclaration> declaration);

    [[nodiscard]] bool has_lexical_declarations() const { return !m_lexical_declarations.is_empty(); }
    [[nodiscard]] bool has_var_declarations() const { return !m_var_declarations.is_empty(); }

    [[nodiscard]] size_t var_declaration_count() const { return m_var_declarations.size(); }
    [[nodiscard]] size_t lexical_declaration_count() const { return m_lexical_declarations.size(); }

    ThrowCompletionOr<void> for_each_lexically_scoped_declaration(ThrowCompletionOrVoidCallback<Declaration const&>&& callback) const;
    ThrowCompletionOr<void> for_each_lexically_declared_name(ThrowCompletionOrVoidCallback<FlyString const&>&& callback) const;

    ThrowCompletionOr<void> for_each_var_declared_name(ThrowCompletionOrVoidCallback<FlyString const&>&& callback) const;

    ThrowCompletionOr<void> for_each_var_function_declaration_in_reverse_order(ThrowCompletionOrVoidCallback<FunctionDeclaration const&>&& callback) const;
    ThrowCompletionOr<void> for_each_var_scoped_variable_declaration(ThrowCompletionOrVoidCallback<VariableDeclaration const&>&& callback) const;

    void block_declaration_instantiation(GlobalObject& global_object, Environment* environment) const;

    ThrowCompletionOr<void> for_each_function_hoistable_with_annexB_extension(ThrowCompletionOrVoidCallback<FunctionDeclaration&>&& callback) const;

protected:
    explicit ScopeNode(SourceRange source_range)
        : Statement(source_range)
    {
    }

private:
    virtual bool is_scope_node() const final { return true; }

    NonnullRefPtrVector<Statement> m_children;
    NonnullRefPtrVector<Declaration> m_lexical_declarations;
    NonnullRefPtrVector<Declaration> m_var_declarations;

    NonnullRefPtrVector<FunctionDeclaration> m_functions_hoistable_with_annexB_extension;
};

// 2.9 ModuleRequest Records, https://tc39.es/proposal-import-assertions/#sec-modulerequest-record
struct ModuleRequest {
    struct Assertion {
        String key;
        String value;
    };

    ModuleRequest() = default;

    explicit ModuleRequest(FlyString specifier)
        : module_specifier(move(specifier))
    {
    }

    ModuleRequest(FlyString module_specifier, Vector<Assertion> assertions);

    void add_assertion(String key, String value)
    {
        assertions.empend(move(key), move(value));
    }

    FlyString module_specifier;   // [[Specifier]]
    Vector<Assertion> assertions; // [[Assertions]]
};

class ImportStatement final : public Statement {
public:
    // ImportEntry Record, https://tc39.es/ecma262/#table-importentry-record-fields
    struct ImportEntry {
        FlyString import_name;       // [[ImportName]] if a String
        FlyString local_name;        // [[LocalName]]
        bool is_namespace { false }; // [[ImportName]] if `namespace-object`

        ImportEntry(FlyString import_name_, FlyString local_name_, bool is_namespace_ = false)
            : import_name(move(import_name_))
            , local_name(move(local_name_))
            , is_namespace(is_namespace_)
        {
            VERIFY(!is_namespace || import_name.is_null());
        }

        ModuleRequest const& module_request() const
        {
            VERIFY(m_module_request);
            return *m_module_request;
        }

    private:
        friend ImportStatement;
        ModuleRequest* m_module_request; // [[ModuleRequest]]
    };

    explicit ImportStatement(SourceRange source_range, ModuleRequest from_module, Vector<ImportEntry> entries = {})
        : Statement(source_range)
        , m_module_request(move(from_module))
        , m_entries(move(entries))
    {
        for (auto& entry : m_entries)
            entry.m_module_request = &m_module_request;
    }

    virtual Completion execute(Interpreter&, GlobalObject&) const override;

    virtual void dump(int indent) const override;

    bool has_bound_name(FlyString const& name) const;
    Vector<ImportEntry> const& entries() const { return m_entries; }
    ModuleRequest const& module_request() const { return m_module_request; }
    ModuleRequest& module_request() { return m_module_request; }

private:
    ModuleRequest m_module_request;
    Vector<ImportEntry> m_entries;
};

class ExportStatement final : public Statement {
public:
    static FlyString local_name_for_default;

    // ExportEntry Record, https://tc39.es/ecma262/#table-exportentry-records
    struct ExportEntry {
        enum class Kind {
            NamedExport,
            ModuleRequestAll,
            ModuleRequestAllButDefault,
        } kind;

        FlyString export_name;          // [[ExportName]]
        FlyString local_or_import_name; // Either [[ImportName]] or [[LocalName]]

        ExportEntry(Kind export_kind, FlyString export_name_, FlyString local_or_import_name_)
            : kind(export_kind)
            , export_name(move(export_name_))
            , local_or_import_name(move(local_or_import_name_))
        {
        }

        bool is_module_request() const
        {
            return m_module_request != nullptr;
        }

        static ExportEntry indirect_export_entry(ModuleRequest const& module_request, FlyString export_name, FlyString import_name)
        {
            ExportEntry entry { Kind::NamedExport, move(export_name), move(import_name) };
            entry.m_module_request = &module_request;
            return entry;
        }

        ModuleRequest const& module_request() const
        {
            VERIFY(m_module_request);
            return *m_module_request;
        }

    private:
        ModuleRequest const* m_module_request { nullptr }; // [[ModuleRequest]]
        friend ExportStatement;

    public:
        static ExportEntry named_export(FlyString export_name, FlyString local_name)
        {
            return ExportEntry { Kind::NamedExport, move(export_name), move(local_name) };
        }

        static ExportEntry all_but_default_entry()
        {
            return ExportEntry { Kind::ModuleRequestAllButDefault, {}, {} };
        }

        static ExportEntry all_module_request(FlyString export_name)
        {
            return ExportEntry { Kind::ModuleRequestAll, move(export_name), {} };
        }
    };

    ExportStatement(SourceRange source_range, RefPtr<ASTNode> statement, Vector<ExportEntry> entries, bool is_default_export, ModuleRequest module_request)
        : Statement(source_range)
        , m_statement(move(statement))
        , m_entries(move(entries))
        , m_is_default_export(is_default_export)
        , m_module_request(move(module_request))
    {
        if (!m_module_request.module_specifier.is_null()) {
            for (auto& entry : m_entries)
                entry.m_module_request = &m_module_request;
        }
    }

    virtual Completion execute(Interpreter&, GlobalObject&) const override;

    virtual void dump(int indent) const override;

    bool has_export(FlyString const& export_name) const;

    bool has_statement() const { return m_statement; }
    Vector<ExportEntry> const& entries() const { return m_entries; }

    bool is_default_export() const { return m_is_default_export; }

    ASTNode const& statement() const
    {
        VERIFY(m_statement);
        return *m_statement;
    }

    ModuleRequest& module_request()
    {
        VERIFY(!m_module_request.module_specifier.is_null());
        return m_module_request;
    }

private:
    RefPtr<ASTNode> m_statement;
    Vector<ExportEntry> m_entries;
    bool m_is_default_export { false };
    ModuleRequest m_module_request;
};

class Program final : public ScopeNode {
public:
    enum class Type {
        Script,
        Module
    };

    explicit Program(SourceRange source_range, Type program_type)
        : ScopeNode(source_range)
        , m_type(program_type)
    {
    }

    virtual Completion execute(Interpreter&, GlobalObject&) const override;

    bool is_strict_mode() const { return m_is_strict_mode; }
    void set_strict_mode() { m_is_strict_mode = true; }

    Type type() const { return m_type; }

    void append_import(NonnullRefPtr<ImportStatement> import_statement)
    {
        m_imports.append(import_statement);
        append(import_statement);
    }

    void append_export(NonnullRefPtr<ExportStatement> export_statement)
    {
        m_exports.append(export_statement);
        append(export_statement);
    }

    NonnullRefPtrVector<ImportStatement> const& imports() const { return m_imports; }
    NonnullRefPtrVector<ExportStatement> const& exports() const { return m_exports; }

    NonnullRefPtrVector<ImportStatement>& imports() { return m_imports; }
    NonnullRefPtrVector<ExportStatement>& exports() { return m_exports; }

    bool has_top_level_await() const { return m_has_top_level_await; }
    void set_has_top_level_await() { m_has_top_level_await = true; }

    ThrowCompletionOr<void> global_declaration_instantiation(Interpreter& interpreter, GlobalObject& global_object, GlobalEnvironment& global_environment) const;

private:
    virtual bool is_program() const override { return true; }

    bool m_is_strict_mode { false };
    Type m_type { Type::Script };

    NonnullRefPtrVector<ImportStatement> m_imports;
    NonnullRefPtrVector<ExportStatement> m_exports;
    bool m_has_top_level_await { false };
};

class BlockStatement final : public ScopeNode {
public:
    explicit BlockStatement(SourceRange source_range)
        : ScopeNode(source_range)
    {
    }
    Completion execute(Interpreter& interpreter, GlobalObject& object) const override;
};

class FunctionBody final : public ScopeNode {
public:
    explicit FunctionBody(SourceRange source_range)
        : ScopeNode(source_range)
    {
    }

    void set_strict_mode() { m_in_strict_mode = true; }

    bool in_strict_mode() const { return m_in_strict_mode; }

    virtual Completion execute(Interpreter&, GlobalObject&) const override;

private:
    bool m_in_strict_mode { false };
};

class Expression : public ASTNode {
public:
    explicit Expression(SourceRange source_range)
        : ASTNode(source_range)
    {
    }
    virtual ThrowCompletionOr<Reference> to_reference(Interpreter&, GlobalObject&) const;
};

class Declaration : public Statement {
public:
    explicit Declaration(SourceRange source_range)
        : Statement(source_range)
    {
    }

    virtual ThrowCompletionOr<void> for_each_bound_name(ThrowCompletionOrVoidCallback<FlyString const&>&& callback) const = 0;

    // 8.1.3 Static Semantics: IsConstantDeclaration, https://tc39.es/ecma262/#sec-static-semantics-isconstantdeclaration
    virtual bool is_constant_declaration() const { return false; }

    virtual bool is_lexical_declaration() const { return false; }
};

class ErrorDeclaration final : public Declaration {
public:
    explicit ErrorDeclaration(SourceRange source_range)
        : Declaration(source_range)
    {
    }
    Completion execute(Interpreter&, GlobalObject&) const override { return {}; }

    ThrowCompletionOr<void> for_each_bound_name(ThrowCompletionOrVoidCallback<FlyString const&>&&) const override
    {
        VERIFY_NOT_REACHED();
    }
};

struct BindingPattern : RefCounted<BindingPattern> {
    // This covers both BindingProperty and BindingElement, hence the more generic name
    struct BindingEntry {
        // If this entry represents a BindingElement, then name will be Empty
        Variant<NonnullRefPtr<Identifier>, NonnullRefPtr<Expression>, Empty> name {};
        Variant<NonnullRefPtr<Identifier>, NonnullRefPtr<BindingPattern>, NonnullRefPtr<MemberExpression>, Empty> alias {};
        RefPtr<Expression> initializer {};
        bool is_rest { false };

        bool is_elision() const { return name.has<Empty>() && alias.has<Empty>(); }
    };

    enum class Kind {
        Array,
        Object,
    };

    void dump(int indent) const;

    ThrowCompletionOr<void> for_each_bound_name(ThrowCompletionOrVoidCallback<FlyString const&>&& callback) const;

    bool contains_expression() const;

    Vector<BindingEntry> entries;
    Kind kind { Kind::Object };
};

class FunctionNode {
public:
    struct Parameter {
        Variant<FlyString, NonnullRefPtr<BindingPattern>> binding;
        RefPtr<Expression> default_value;
        bool is_rest { false };
    };

    FlyString const& name() const { return m_name; }
    String const& source_text() const { return m_source_text; }
    Statement const& body() const { return *m_body; }
    Vector<Parameter> const& parameters() const { return m_parameters; };
    i32 function_length() const { return m_function_length; }
    bool is_strict_mode() const { return m_is_strict_mode; }
    bool might_need_arguments_object() const { return m_might_need_arguments_object; }
    bool contains_direct_call_to_eval() const { return m_contains_direct_call_to_eval; }
    bool is_arrow_function() const { return m_is_arrow_function; }
    FunctionKind kind() const { return m_kind; }

protected:
    FunctionNode(FlyString name, String source_text, NonnullRefPtr<Statement> body, Vector<Parameter> parameters, i32 function_length, FunctionKind kind, bool is_strict_mode, bool might_need_arguments_object, bool contains_direct_call_to_eval, bool is_arrow_function)
        : m_name(move(name))
        , m_source_text(move(source_text))
        , m_body(move(body))
        , m_parameters(move(parameters))
        , m_function_length(function_length)
        , m_kind(kind)
        , m_is_strict_mode(is_strict_mode)
        , m_might_need_arguments_object(might_need_arguments_object)
        , m_contains_direct_call_to_eval(contains_direct_call_to_eval)
        , m_is_arrow_function(is_arrow_function)
    {
        if (m_is_arrow_function)
            VERIFY(!m_might_need_arguments_object);
    }

    void dump(int indent, String const& class_name) const;

private:
    FlyString m_name;
    String m_source_text;
    NonnullRefPtr<Statement> m_body;
    Vector<Parameter> const m_parameters;
    const i32 m_function_length;
    FunctionKind m_kind;
    bool m_is_strict_mode { false };
    bool m_might_need_arguments_object { false };
    bool m_contains_direct_call_to_eval { false };
    bool m_is_arrow_function { false };
};

class FunctionDeclaration final
    : public Declaration
    , public FunctionNode {
public:
    static bool must_have_name() { return true; }

    FunctionDeclaration(SourceRange source_range, FlyString const& name, String source_text, NonnullRefPtr<Statement> body, Vector<Parameter> parameters, i32 function_length, FunctionKind kind, bool is_strict_mode, bool might_need_arguments_object, bool contains_direct_call_to_eval)
        : Declaration(source_range)
        , FunctionNode(name, move(source_text), move(body), move(parameters), function_length, kind, is_strict_mode, might_need_arguments_object, contains_direct_call_to_eval, false)
    {
    }

    virtual Completion execute(Interpreter&, GlobalObject&) const override;
    virtual void dump(int indent) const override;
    virtual Bytecode::CodeGenerationErrorOr<void> generate_bytecode(Bytecode::Generator&) const override;

    virtual ThrowCompletionOr<void> for_each_bound_name(ThrowCompletionOrVoidCallback<FlyString const&>&& callback) const override;

    virtual bool is_function_declaration() const override { return true; }

    void set_should_do_additional_annexB_steps() { m_is_hoisted = true; }

private:
    bool m_is_hoisted { false };
};

class FunctionExpression final
    : public Expression
    , public FunctionNode {
public:
    static bool must_have_name() { return false; }

    FunctionExpression(SourceRange source_range, FlyString const& name, String source_text, NonnullRefPtr<Statement> body, Vector<Parameter> parameters, i32 function_length, FunctionKind kind, bool is_strict_mode, bool might_need_arguments_object, bool contains_direct_call_to_eval, bool is_arrow_function = false)
        : Expression(source_range)
        , FunctionNode(name, move(source_text), move(body), move(parameters), function_length, kind, is_strict_mode, might_need_arguments_object, contains_direct_call_to_eval, is_arrow_function)
    {
    }

    virtual Completion execute(Interpreter&, GlobalObject&) const override;
    virtual void dump(int indent) const override;

    virtual Bytecode::CodeGenerationErrorOr<void> generate_bytecode(Bytecode::Generator&) const override;

    bool has_name() const { return !name().is_empty(); }

    Value instantiate_ordinary_function_expression(Interpreter& interpreter, GlobalObject& global_object, FlyString given_name) const;

private:
    virtual bool is_function_expression() const override { return true; }
};

class ErrorExpression final : public Expression {
public:
    explicit ErrorExpression(SourceRange source_range)
        : Expression(source_range)
    {
    }

    Completion execute(Interpreter&, GlobalObject&) const override { return {}; }
};

class YieldExpression final : public Expression {
public:
    explicit YieldExpression(SourceRange source_range, RefPtr<Expression> argument, bool is_yield_from)
        : Expression(source_range)
        , m_argument(move(argument))
        , m_is_yield_from(is_yield_from)
    {
    }

    Expression const* argument() const { return m_argument; }
    bool is_yield_from() const { return m_is_yield_from; }

    virtual Completion execute(Interpreter&, GlobalObject&) const override;
    virtual void dump(int indent) const override;
    virtual Bytecode::CodeGenerationErrorOr<void> generate_bytecode(Bytecode::Generator&) const override;

private:
    RefPtr<Expression> m_argument;
    bool m_is_yield_from { false };
};

class AwaitExpression final : public Expression {
public:
    explicit AwaitExpression(SourceRange source_range, NonnullRefPtr<Expression> argument)
        : Expression(source_range)
        , m_argument(move(argument))
    {
    }

    virtual Completion execute(Interpreter&, GlobalObject&) const override;
    virtual void dump(int indent) const override;
    virtual Bytecode::CodeGenerationErrorOr<void> generate_bytecode(Bytecode::Generator&) const override;

private:
    NonnullRefPtr<Expression> m_argument;
};

class ReturnStatement final : public Statement {
public:
    explicit ReturnStatement(SourceRange source_range, RefPtr<Expression> argument)
        : Statement(source_range)
        , m_argument(move(argument))
    {
    }

    Expression const* argument() const { return m_argument; }

    virtual Completion execute(Interpreter&, GlobalObject&) const override;
    virtual void dump(int indent) const override;
    virtual Bytecode::CodeGenerationErrorOr<void> generate_bytecode(Bytecode::Generator&) const override;

private:
    RefPtr<Expression> m_argument;
};

class IfStatement final : public Statement {
public:
    IfStatement(SourceRange source_range, NonnullRefPtr<Expression> predicate, NonnullRefPtr<Statement> consequent, RefPtr<Statement> alternate)
        : Statement(source_range)
        , m_predicate(move(predicate))
        , m_consequent(move(consequent))
        , m_alternate(move(alternate))
    {
    }

    Expression const& predicate() const { return *m_predicate; }
    Statement const& consequent() const { return *m_consequent; }
    Statement const* alternate() const { return m_alternate; }

    virtual Completion execute(Interpreter&, GlobalObject&) const override;
    virtual void dump(int indent) const override;
    virtual Bytecode::CodeGenerationErrorOr<void> generate_bytecode(Bytecode::Generator&) const override;

private:
    NonnullRefPtr<Expression> m_predicate;
    NonnullRefPtr<Statement> m_consequent;
    RefPtr<Statement> m_alternate;
};

class WhileStatement final : public IterationStatement {
public:
    WhileStatement(SourceRange source_range, NonnullRefPtr<Expression> test, NonnullRefPtr<Statement> body)
        : IterationStatement(source_range)
        , m_test(move(test))
        , m_body(move(body))
    {
    }

    Expression const& test() const { return *m_test; }
    Statement const& body() const { return *m_body; }

    virtual Completion execute(Interpreter&, GlobalObject&) const override;
    virtual Completion loop_evaluation(Interpreter&, GlobalObject&, Vector<FlyString> const&) const override;
    virtual void dump(int indent) const override;
    virtual Bytecode::CodeGenerationErrorOr<void> generate_bytecode(Bytecode::Generator&) const override;

private:
    NonnullRefPtr<Expression> m_test;
    NonnullRefPtr<Statement> m_body;
};

class DoWhileStatement final : public IterationStatement {
public:
    DoWhileStatement(SourceRange source_range, NonnullRefPtr<Expression> test, NonnullRefPtr<Statement> body)
        : IterationStatement(source_range)
        , m_test(move(test))
        , m_body(move(body))
    {
    }

    Expression const& test() const { return *m_test; }
    Statement const& body() const { return *m_body; }

    virtual Completion execute(Interpreter&, GlobalObject&) const override;
    virtual Completion loop_evaluation(Interpreter&, GlobalObject&, Vector<FlyString> const&) const override;
    virtual void dump(int indent) const override;
    virtual Bytecode::CodeGenerationErrorOr<void> generate_bytecode(Bytecode::Generator&) const override;

private:
    NonnullRefPtr<Expression> m_test;
    NonnullRefPtr<Statement> m_body;
};

class WithStatement final : public Statement {
public:
    WithStatement(SourceRange source_range, NonnullRefPtr<Expression> object, NonnullRefPtr<Statement> body)
        : Statement(source_range)
        , m_object(move(object))
        , m_body(move(body))
    {
    }

    Expression const& object() const { return *m_object; }
    Statement const& body() const { return *m_body; }

    virtual Completion execute(Interpreter&, GlobalObject&) const override;
    virtual void dump(int indent) const override;

private:
    NonnullRefPtr<Expression> m_object;
    NonnullRefPtr<Statement> m_body;
};

class ForStatement final : public IterationStatement {
public:
    ForStatement(SourceRange source_range, RefPtr<ASTNode> init, RefPtr<Expression> test, RefPtr<Expression> update, NonnullRefPtr<Statement> body)
        : IterationStatement(source_range)
        , m_init(move(init))
        , m_test(move(test))
        , m_update(move(update))
        , m_body(move(body))
    {
    }

    ASTNode const* init() const { return m_init; }
    Expression const* test() const { return m_test; }
    Expression const* update() const { return m_update; }
    Statement const& body() const { return *m_body; }

    virtual Completion execute(Interpreter&, GlobalObject&) const override;
    virtual Completion loop_evaluation(Interpreter&, GlobalObject&, Vector<FlyString> const&) const override;
    virtual void dump(int indent) const override;
    virtual Bytecode::CodeGenerationErrorOr<void> generate_bytecode(Bytecode::Generator&) const override;

private:
    RefPtr<ASTNode> m_init;
    RefPtr<Expression> m_test;
    RefPtr<Expression> m_update;
    NonnullRefPtr<Statement> m_body;
};

class ForInStatement final : public IterationStatement {
public:
    ForInStatement(SourceRange source_range, Variant<NonnullRefPtr<ASTNode>, NonnullRefPtr<BindingPattern>> lhs, NonnullRefPtr<Expression> rhs, NonnullRefPtr<Statement> body)
        : IterationStatement(source_range)
        , m_lhs(move(lhs))
        , m_rhs(move(rhs))
        , m_body(move(body))
    {
    }

    auto const& lhs() const { return m_lhs; }
    Expression const& rhs() const { return *m_rhs; }
    Statement const& body() const { return *m_body; }

    virtual Completion execute(Interpreter&, GlobalObject&) const override;
    virtual Completion loop_evaluation(Interpreter&, GlobalObject&, Vector<FlyString> const&) const override;
    virtual void dump(int indent) const override;

private:
    Variant<NonnullRefPtr<ASTNode>, NonnullRefPtr<BindingPattern>> m_lhs;
    NonnullRefPtr<Expression> m_rhs;
    NonnullRefPtr<Statement> m_body;
};

class ForOfStatement final : public IterationStatement {
public:
    ForOfStatement(SourceRange source_range, Variant<NonnullRefPtr<ASTNode>, NonnullRefPtr<BindingPattern>> lhs, NonnullRefPtr<Expression> rhs, NonnullRefPtr<Statement> body)
        : IterationStatement(source_range)
        , m_lhs(move(lhs))
        , m_rhs(move(rhs))
        , m_body(move(body))
    {
    }

    auto const& lhs() const { return m_lhs; }
    Expression const& rhs() const { return *m_rhs; }
    Statement const& body() const { return *m_body; }

    virtual Completion execute(Interpreter&, GlobalObject&) const override;
    virtual Completion loop_evaluation(Interpreter&, GlobalObject&, Vector<FlyString> const&) const override;
    virtual void dump(int indent) const override;

private:
    Variant<NonnullRefPtr<ASTNode>, NonnullRefPtr<BindingPattern>> m_lhs;
    NonnullRefPtr<Expression> m_rhs;
    NonnullRefPtr<Statement> m_body;
};

class ForAwaitOfStatement final : public IterationStatement {
public:
    ForAwaitOfStatement(SourceRange source_range, Variant<NonnullRefPtr<ASTNode>, NonnullRefPtr<BindingPattern>> lhs, NonnullRefPtr<Expression> rhs, NonnullRefPtr<Statement> body)
        : IterationStatement(source_range)
        , m_lhs(move(lhs))
        , m_rhs(move(rhs))
        , m_body(move(body))
    {
    }

    virtual Completion execute(Interpreter&, GlobalObject&) const override;
    virtual Completion loop_evaluation(Interpreter&, GlobalObject&, Vector<FlyString> const&) const override;
    virtual void dump(int indent) const override;

private:
    Variant<NonnullRefPtr<ASTNode>, NonnullRefPtr<BindingPattern>> m_lhs;
    NonnullRefPtr<Expression> m_rhs;
    NonnullRefPtr<Statement> m_body;
};

enum class BinaryOp {
    Addition,
    Subtraction,
    Multiplication,
    Division,
    Modulo,
    Exponentiation,
    StrictlyEquals,
    StrictlyInequals,
    LooselyEquals,
    LooselyInequals,
    GreaterThan,
    GreaterThanEquals,
    LessThan,
    LessThanEquals,
    BitwiseAnd,
    BitwiseOr,
    BitwiseXor,
    LeftShift,
    RightShift,
    UnsignedRightShift,
    In,
    InstanceOf,
};

class BinaryExpression final : public Expression {
public:
    BinaryExpression(SourceRange source_range, BinaryOp op, NonnullRefPtr<Expression> lhs, NonnullRefPtr<Expression> rhs)
        : Expression(source_range)
        , m_op(op)
        , m_lhs(move(lhs))
        , m_rhs(move(rhs))
    {
    }

    virtual Completion execute(Interpreter&, GlobalObject&) const override;
    virtual void dump(int indent) const override;
    virtual Bytecode::CodeGenerationErrorOr<void> generate_bytecode(Bytecode::Generator&) const override;

private:
    BinaryOp m_op;
    NonnullRefPtr<Expression> m_lhs;
    NonnullRefPtr<Expression> m_rhs;
};

enum class LogicalOp {
    And,
    Or,
    NullishCoalescing,
};

class LogicalExpression final : public Expression {
public:
    LogicalExpression(SourceRange source_range, LogicalOp op, NonnullRefPtr<Expression> lhs, NonnullRefPtr<Expression> rhs)
        : Expression(source_range)
        , m_op(op)
        , m_lhs(move(lhs))
        , m_rhs(move(rhs))
    {
    }

    virtual Completion execute(Interpreter&, GlobalObject&) const override;
    virtual void dump(int indent) const override;
    virtual Bytecode::CodeGenerationErrorOr<void> generate_bytecode(Bytecode::Generator&) const override;

private:
    LogicalOp m_op;
    NonnullRefPtr<Expression> m_lhs;
    NonnullRefPtr<Expression> m_rhs;
};

enum class UnaryOp {
    BitwiseNot,
    Not,
    Plus,
    Minus,
    Typeof,
    Void,
    Delete,
};

class UnaryExpression final : public Expression {
public:
    UnaryExpression(SourceRange source_range, UnaryOp op, NonnullRefPtr<Expression> lhs)
        : Expression(source_range)
        , m_op(op)
        , m_lhs(move(lhs))
    {
    }

    virtual Completion execute(Interpreter&, GlobalObject&) const override;
    virtual void dump(int indent) const override;
    virtual Bytecode::CodeGenerationErrorOr<void> generate_bytecode(Bytecode::Generator&) const override;

private:
    UnaryOp m_op;
    NonnullRefPtr<Expression> m_lhs;
};

class SequenceExpression final : public Expression {
public:
    SequenceExpression(SourceRange source_range, NonnullRefPtrVector<Expression> expressions)
        : Expression(source_range)
        , m_expressions(move(expressions))
    {
        VERIFY(m_expressions.size() >= 2);
    }

    virtual void dump(int indent) const override;
    virtual Completion execute(Interpreter&, GlobalObject&) const override;
    virtual Bytecode::CodeGenerationErrorOr<void> generate_bytecode(Bytecode::Generator&) const override;

private:
    NonnullRefPtrVector<Expression> m_expressions;
};

class Literal : public Expression {
protected:
    explicit Literal(SourceRange source_range)
        : Expression(source_range)
    {
    }
};

class BooleanLiteral final : public Literal {
public:
    explicit BooleanLiteral(SourceRange source_range, bool value)
        : Literal(source_range)
        , m_value(value)
    {
    }

    virtual Completion execute(Interpreter&, GlobalObject&) const override;
    virtual void dump(int indent) const override;
    virtual Bytecode::CodeGenerationErrorOr<void> generate_bytecode(Bytecode::Generator&) const override;

private:
    bool m_value { false };
};

class NumericLiteral final : public Literal {
public:
    explicit NumericLiteral(SourceRange source_range, double value)
        : Literal(source_range)
        , m_value(value)
    {
    }

    virtual Completion execute(Interpreter&, GlobalObject&) const override;
    virtual void dump(int indent) const override;
    virtual Bytecode::CodeGenerationErrorOr<void> generate_bytecode(Bytecode::Generator&) const override;

private:
    Value m_value;
};

class BigIntLiteral final : public Literal {
public:
    explicit BigIntLiteral(SourceRange source_range, String value)
        : Literal(source_range)
        , m_value(move(value))
    {
    }

    virtual Completion execute(Interpreter&, GlobalObject&) const override;
    virtual void dump(int indent) const override;
    virtual Bytecode::CodeGenerationErrorOr<void> generate_bytecode(Bytecode::Generator&) const override;

private:
    String m_value;
};

class StringLiteral final : public Literal {
public:
    explicit StringLiteral(SourceRange source_range, String value, bool is_use_strict_directive = false)
        : Literal(source_range)
        , m_value(move(value))
        , m_is_use_strict_directive(is_use_strict_directive)
    {
    }

    virtual Completion execute(Interpreter&, GlobalObject&) const override;
    virtual void dump(int indent) const override;
    virtual Bytecode::CodeGenerationErrorOr<void> generate_bytecode(Bytecode::Generator&) const override;

    StringView value() const { return m_value; }
    bool is_use_strict_directive() const { return m_is_use_strict_directive; };

private:
    virtual bool is_string_literal() const override { return true; }

    String m_value;
    bool m_is_use_strict_directive;
};

class NullLiteral final : public Literal {
public:
    explicit NullLiteral(SourceRange source_range)
        : Literal(source_range)
    {
    }

    virtual Completion execute(Interpreter&, GlobalObject&) const override;
    virtual void dump(int indent) const override;
    virtual Bytecode::CodeGenerationErrorOr<void> generate_bytecode(Bytecode::Generator&) const override;
};

class RegExpLiteral final : public Literal {
public:
    RegExpLiteral(SourceRange source_range, regex::Parser::Result parsed_regex, String parsed_pattern, regex::RegexOptions<ECMAScriptFlags> parsed_flags, String pattern, String flags)
        : Literal(source_range)
        , m_parsed_regex(move(parsed_regex))
        , m_parsed_pattern(move(parsed_pattern))
        , m_parsed_flags(move(parsed_flags))
        , m_pattern(move(pattern))
        , m_flags(move(flags))
    {
    }

    virtual Completion execute(Interpreter&, GlobalObject&) const override;
    virtual void dump(int indent) const override;
    virtual Bytecode::CodeGenerationErrorOr<void> generate_bytecode(Bytecode::Generator&) const override;

    regex::Parser::Result const& parsed_regex() const { return m_parsed_regex; }
    String const& parsed_pattern() const { return m_parsed_pattern; }
    regex::RegexOptions<ECMAScriptFlags> const& parsed_flags() const { return m_parsed_flags; }
    String const& pattern() const { return m_pattern; }
    String const& flags() const { return m_flags; }

private:
    regex::Parser::Result m_parsed_regex;
    String m_parsed_pattern;
    regex::RegexOptions<ECMAScriptFlags> m_parsed_flags;
    String m_pattern;
    String m_flags;
};

class Identifier final : public Expression {
public:
    explicit Identifier(SourceRange source_range, FlyString string)
        : Expression(source_range)
        , m_string(move(string))
    {
    }

    FlyString const& string() const { return m_string; }
    void set_lexically_bound_function_argument_index(size_t index) { m_lexically_bound_function_argument = index; }

    virtual Completion execute(Interpreter&, GlobalObject&) const override;
    virtual void dump(int indent) const override;
    virtual ThrowCompletionOr<Reference> to_reference(Interpreter&, GlobalObject&) const override;
    virtual Bytecode::CodeGenerationErrorOr<void> generate_bytecode(Bytecode::Generator&) const override;

private:
    virtual bool is_identifier() const override { return true; }

    FlyString m_string;
    Optional<size_t> m_lexically_bound_function_argument;
    mutable Optional<EnvironmentCoordinate> m_cached_environment_coordinate;
};

class PrivateIdentifier final : public Expression {
public:
    explicit PrivateIdentifier(SourceRange source_range, FlyString string)
        : Expression(source_range)
        , m_string(move(string))
    {
    }

    FlyString const& string() const { return m_string; }

    virtual Completion execute(Interpreter&, GlobalObject&) const override;
    virtual void dump(int indent) const override;

private:
    FlyString m_string;
};

class ClassElement : public ASTNode {
public:
    ClassElement(SourceRange source_range, bool is_static)
        : ASTNode(source_range)
        , m_is_static(is_static)
    {
    }

    virtual Completion execute(Interpreter&, GlobalObject&) const override;

    enum class ElementKind {
        Method,
        Field,
        StaticInitializer,
    };

    virtual ElementKind class_element_kind() const = 0;
    bool is_static() const { return m_is_static; }

    using ClassElementName = Variant<PropertyKey, PrivateName>;

    struct ClassFieldDefinition {
        ClassElementName name;
        Handle<ECMAScriptFunctionObject> initializer;
    };

    // We use the Completion also as a ClassStaticBlockDefinition Record.
    using ClassValue = Variant<ClassFieldDefinition, Completion, PrivateElement>;
    virtual ThrowCompletionOr<ClassValue> class_element_evaluation(Interpreter&, GlobalObject&, Object& home_object) const = 0;

    virtual Optional<FlyString> private_bound_identifier() const { return {}; };

private:
    bool m_is_static { false };
};

class ClassMethod final : public ClassElement {
public:
    enum class Kind {
        Method,
        Getter,
        Setter,
    };

    ClassMethod(SourceRange source_range, NonnullRefPtr<Expression> key, NonnullRefPtr<FunctionExpression> function, Kind kind, bool is_static)
        : ClassElement(source_range, is_static)
        , m_key(move(key))
        , m_function(move(function))
        , m_kind(kind)
    {
    }

    Expression const& key() const { return *m_key; }
    Kind kind() const { return m_kind; }
    virtual ElementKind class_element_kind() const override { return ElementKind::Method; }

    virtual void dump(int indent) const override;
    virtual ThrowCompletionOr<ClassValue> class_element_evaluation(Interpreter&, GlobalObject&, Object& home_object) const override;
    virtual Optional<FlyString> private_bound_identifier() const override;

private:
    virtual bool is_class_method() const override { return true; }
    NonnullRefPtr<Expression> m_key;
    NonnullRefPtr<FunctionExpression> m_function;
    Kind m_kind;
};

class ClassField final : public ClassElement {
public:
    ClassField(SourceRange source_range, NonnullRefPtr<Expression> key, RefPtr<Expression> init, bool contains_direct_call_to_eval, bool is_static)
        : ClassElement(source_range, is_static)
        , m_key(move(key))
        , m_initializer(move(init))
        , m_contains_direct_call_to_eval(contains_direct_call_to_eval)
    {
    }

    Expression const& key() const { return *m_key; }
    RefPtr<Expression> const& initializer() const { return m_initializer; }
    RefPtr<Expression>& initializer() { return m_initializer; }

    virtual ElementKind class_element_kind() const override { return ElementKind::Field; }

    virtual void dump(int indent) const override;
    virtual ThrowCompletionOr<ClassValue> class_element_evaluation(Interpreter& interpreter, GlobalObject& object, Object& home_object) const override;
    virtual Optional<FlyString> private_bound_identifier() const override;

private:
    NonnullRefPtr<Expression> m_key;
    RefPtr<Expression> m_initializer;
    bool m_contains_direct_call_to_eval { false };
};

class StaticInitializer final : public ClassElement {
public:
    StaticInitializer(SourceRange source_range, NonnullRefPtr<FunctionBody> function_body, bool contains_direct_call_to_eval)
        : ClassElement(source_range, true)
        , m_function_body(move(function_body))
        , m_contains_direct_call_to_eval(contains_direct_call_to_eval)
    {
    }

    virtual ElementKind class_element_kind() const override { return ElementKind::StaticInitializer; }
    virtual ThrowCompletionOr<ClassValue> class_element_evaluation(Interpreter&, GlobalObject&, Object& home_object) const override;

    virtual void dump(int indent) const override;

private:
    NonnullRefPtr<FunctionBody> m_function_body;
    bool m_contains_direct_call_to_eval { false };
};

class SuperExpression final : public Expression {
public:
    explicit SuperExpression(SourceRange source_range)
        : Expression(source_range)
    {
    }

    virtual Completion execute(Interpreter&, GlobalObject&) const override;
    virtual void dump(int indent) const override;

    virtual bool is_super_expression() const override { return true; }
};

class ClassExpression final : public Expression {
public:
    ClassExpression(SourceRange source_range, String name, String source_text, RefPtr<FunctionExpression> constructor, RefPtr<Expression> super_class, NonnullRefPtrVector<ClassElement> elements)
        : Expression(source_range)
        , m_name(move(name))
        , m_source_text(move(source_text))
        , m_constructor(move(constructor))
        , m_super_class(move(super_class))
        , m_elements(move(elements))
    {
    }

    StringView name() const { return m_name; }
    String const& source_text() const { return m_source_text; }
    RefPtr<FunctionExpression> constructor() const { return m_constructor; }

    virtual Completion execute(Interpreter&, GlobalObject&) const override;
    virtual void dump(int indent) const override;
    virtual Bytecode::CodeGenerationErrorOr<void> generate_bytecode(Bytecode::Generator&) const override;

    bool has_name() const { return !m_name.is_empty(); }

    ThrowCompletionOr<ECMAScriptFunctionObject*> class_definition_evaluation(Interpreter& interpreter, GlobalObject& global_object, FlyString const& binding_name = {}, FlyString const& class_name = {}) const;

private:
    virtual bool is_class_expression() const override { return true; }

    String m_name;
    String m_source_text;
    RefPtr<FunctionExpression> m_constructor;
    RefPtr<Expression> m_super_class;
    NonnullRefPtrVector<ClassElement> m_elements;
};

class ClassDeclaration final : public Declaration {
public:
    ClassDeclaration(SourceRange source_range, NonnullRefPtr<ClassExpression> class_expression)
        : Declaration(source_range)
        , m_class_expression(move(class_expression))
    {
    }

    virtual Completion execute(Interpreter&, GlobalObject&) const override;
    virtual void dump(int indent) const override;
    virtual Bytecode::CodeGenerationErrorOr<void> generate_bytecode(Bytecode::Generator&) const override;

    virtual ThrowCompletionOr<void> for_each_bound_name(ThrowCompletionOrVoidCallback<FlyString const&>&& callback) const override;

    virtual bool is_lexical_declaration() const override { return true; }

    StringView name() const { return m_class_expression->name(); }

private:
    virtual bool is_class_declaration() const override { return true; }

    friend ExportStatement;

    NonnullRefPtr<ClassExpression> m_class_expression;
};

class SpreadExpression final : public Expression {
public:
    explicit SpreadExpression(SourceRange source_range, NonnullRefPtr<Expression> target)
        : Expression(source_range)
        , m_target(move(target))
    {
    }

    virtual Completion execute(Interpreter&, GlobalObject&) const override;
    virtual void dump(int indent) const override;

private:
    NonnullRefPtr<Expression> m_target;
};

class ThisExpression final : public Expression {
public:
    explicit ThisExpression(SourceRange source_range)
        : Expression(source_range)
    {
    }
    virtual Completion execute(Interpreter&, GlobalObject&) const override;
    virtual void dump(int indent) const override;
    virtual Bytecode::CodeGenerationErrorOr<void> generate_bytecode(Bytecode::Generator&) const override;
};

class CallExpression : public Expression {
public:
    struct Argument {
        NonnullRefPtr<Expression> value;
        bool is_spread;
    };

    CallExpression(SourceRange source_range, NonnullRefPtr<Expression> callee, Vector<Argument> arguments = {})
        : Expression(source_range)
        , m_callee(move(callee))
        , m_arguments(move(arguments))
    {
    }

    virtual Completion execute(Interpreter&, GlobalObject&) const override;
    virtual void dump(int indent) const override;
    virtual Bytecode::CodeGenerationErrorOr<void> generate_bytecode(Bytecode::Generator&) const override;

    Expression const& callee() const { return m_callee; }

protected:
    virtual bool is_call_expression() const override { return true; }

    Completion throw_type_error_for_callee(Interpreter&, GlobalObject&, Value callee_value, StringView call_type) const;

    NonnullRefPtr<Expression> m_callee;
    Vector<Argument> const m_arguments;

private:
    struct ThisAndCallee {
        Value this_value;
        Value callee;
    };
    ThrowCompletionOr<ThisAndCallee> compute_this_and_callee(Interpreter&, GlobalObject&, Reference const&) const;
};

class NewExpression final : public CallExpression {
public:
    NewExpression(SourceRange source_range, NonnullRefPtr<Expression> callee, Vector<Argument> arguments = {})
        : CallExpression(source_range, move(callee), move(arguments))
    {
    }

    virtual Completion execute(Interpreter&, GlobalObject&) const override;

    virtual bool is_new_expression() const override { return true; }
};

class SuperCall final : public Expression {
public:
    SuperCall(SourceRange source_range, Vector<CallExpression::Argument> arguments)
        : Expression(source_range)
        , m_arguments(move(arguments))
    {
    }

    virtual Completion execute(Interpreter&, GlobalObject&) const override;
    virtual void dump(int indent) const override;

private:
    Vector<CallExpression::Argument> const m_arguments;
};

enum class AssignmentOp {
    Assignment,
    AdditionAssignment,
    SubtractionAssignment,
    MultiplicationAssignment,
    DivisionAssignment,
    ModuloAssignment,
    ExponentiationAssignment,
    BitwiseAndAssignment,
    BitwiseOrAssignment,
    BitwiseXorAssignment,
    LeftShiftAssignment,
    RightShiftAssignment,
    UnsignedRightShiftAssignment,
    AndAssignment,
    OrAssignment,
    NullishAssignment,
};

class AssignmentExpression final : public Expression {
public:
    AssignmentExpression(SourceRange source_range, AssignmentOp op, NonnullRefPtr<Expression> lhs, NonnullRefPtr<Expression> rhs)
        : Expression(source_range)
        , m_op(op)
        , m_lhs(move(lhs))
        , m_rhs(move(rhs))
    {
    }

    AssignmentExpression(SourceRange source_range, AssignmentOp op, NonnullRefPtr<BindingPattern> lhs, NonnullRefPtr<Expression> rhs)
        : Expression(source_range)
        , m_op(op)
        , m_lhs(move(lhs))
        , m_rhs(move(rhs))
    {
    }

    virtual Completion execute(Interpreter&, GlobalObject&) const override;
    virtual void dump(int indent) const override;
    virtual Bytecode::CodeGenerationErrorOr<void> generate_bytecode(Bytecode::Generator&) const override;

private:
    AssignmentOp m_op;
    Variant<NonnullRefPtr<Expression>, NonnullRefPtr<BindingPattern>> m_lhs;
    NonnullRefPtr<Expression> m_rhs;
};

enum class UpdateOp {
    Increment,
    Decrement,
};

class UpdateExpression final : public Expression {
public:
    UpdateExpression(SourceRange source_range, UpdateOp op, NonnullRefPtr<Expression> argument, bool prefixed = false)
        : Expression(source_range)
        , m_op(op)
        , m_argument(move(argument))
        , m_prefixed(prefixed)
    {
    }

    virtual Completion execute(Interpreter&, GlobalObject&) const override;
    virtual void dump(int indent) const override;
    virtual Bytecode::CodeGenerationErrorOr<void> generate_bytecode(Bytecode::Generator&) const override;

private:
    virtual bool is_update_expression() const override { return true; }

    UpdateOp m_op;
    NonnullRefPtr<Expression> m_argument;
    bool m_prefixed;
};

enum class DeclarationKind {
    Var,
    Let,
    Const,
};

class VariableDeclarator final : public ASTNode {
public:
    VariableDeclarator(SourceRange source_range, NonnullRefPtr<Identifier> id)
        : ASTNode(source_range)
        , m_target(move(id))
    {
    }

    VariableDeclarator(SourceRange source_range, NonnullRefPtr<Identifier> target, RefPtr<Expression> init)
        : ASTNode(source_range)
        , m_target(move(target))
        , m_init(move(init))
    {
    }

    VariableDeclarator(SourceRange source_range, Variant<NonnullRefPtr<Identifier>, NonnullRefPtr<BindingPattern>> target, RefPtr<Expression> init)
        : ASTNode(source_range)
        , m_target(move(target))
        , m_init(move(init))
    {
    }

    auto& target() const { return m_target; }
    Expression const* init() const { return m_init; }

    virtual Completion execute(Interpreter&, GlobalObject&) const override;
    virtual void dump(int indent) const override;

private:
    Variant<NonnullRefPtr<Identifier>, NonnullRefPtr<BindingPattern>> m_target;
    RefPtr<Expression> m_init;
};

class VariableDeclaration final : public Declaration {
public:
    VariableDeclaration(SourceRange source_range, DeclarationKind declaration_kind, NonnullRefPtrVector<VariableDeclarator> declarations)
        : Declaration(source_range)
        , m_declaration_kind(declaration_kind)
        , m_declarations(move(declarations))
    {
    }

    DeclarationKind declaration_kind() const { return m_declaration_kind; }

    virtual Completion execute(Interpreter&, GlobalObject&) const override;
    virtual void dump(int indent) const override;
    virtual Bytecode::CodeGenerationErrorOr<void> generate_bytecode(Bytecode::Generator&) const override;

    NonnullRefPtrVector<VariableDeclarator> const& declarations() const { return m_declarations; }

    virtual ThrowCompletionOr<void> for_each_bound_name(ThrowCompletionOrVoidCallback<FlyString const&>&& callback) const override;

    virtual bool is_constant_declaration() const override { return m_declaration_kind == DeclarationKind::Const; };

    virtual bool is_lexical_declaration() const override { return m_declaration_kind != DeclarationKind::Var; }

private:
    virtual bool is_variable_declaration() const override { return true; }

    DeclarationKind m_declaration_kind;
    NonnullRefPtrVector<VariableDeclarator> m_declarations;
};

class ObjectProperty final : public ASTNode {
public:
    enum class Type {
        KeyValue,
        Getter,
        Setter,
        Spread,
    };

    ObjectProperty(SourceRange source_range, NonnullRefPtr<Expression> key, RefPtr<Expression> value, Type property_type, bool is_method)
        : ASTNode(source_range)
        , m_key(move(key))
        , m_value(move(value))
        , m_property_type(property_type)
        , m_is_method(is_method)
    {
    }

    Expression const& key() const { return m_key; }
    Expression const& value() const
    {
        VERIFY(m_value);
        return *m_value;
    }

    Type type() const { return m_property_type; }
    bool is_method() const { return m_is_method; }

    virtual void dump(int indent) const override;
    virtual Completion execute(Interpreter&, GlobalObject&) const override;

private:
    NonnullRefPtr<Expression> m_key;
    RefPtr<Expression> m_value;
    Type m_property_type;
    bool m_is_method { false };
};

class ObjectExpression final : public Expression {
public:
    explicit ObjectExpression(SourceRange source_range, NonnullRefPtrVector<ObjectProperty> properties = {}, Optional<SourceRange> first_invalid_property_range = {})
        : Expression(source_range)
        , m_properties(move(properties))
        , m_first_invalid_property_range(move(first_invalid_property_range))
    {
    }

    virtual Completion execute(Interpreter&, GlobalObject&) const override;
    virtual void dump(int indent) const override;
    virtual Bytecode::CodeGenerationErrorOr<void> generate_bytecode(Bytecode::Generator&) const override;

    Optional<SourceRange> const& invalid_property_range() const { return m_first_invalid_property_range; }

private:
    virtual bool is_object_expression() const override { return true; }

    NonnullRefPtrVector<ObjectProperty> m_properties;
    Optional<SourceRange> m_first_invalid_property_range;
};

class ArrayExpression final : public Expression {
public:
    ArrayExpression(SourceRange source_range, Vector<RefPtr<Expression>> elements)
        : Expression(source_range)
        , m_elements(move(elements))
    {
    }

    Vector<RefPtr<Expression>> const& elements() const { return m_elements; }

    virtual Completion execute(Interpreter&, GlobalObject&) const override;
    virtual void dump(int indent) const override;
    virtual Bytecode::CodeGenerationErrorOr<void> generate_bytecode(Bytecode::Generator&) const override;

private:
    virtual bool is_array_expression() const override { return true; }

    Vector<RefPtr<Expression>> m_elements;
};

class TemplateLiteral final : public Expression {
public:
    TemplateLiteral(SourceRange source_range, NonnullRefPtrVector<Expression> expressions)
        : Expression(source_range)
        , m_expressions(move(expressions))
    {
    }

    TemplateLiteral(SourceRange source_range, NonnullRefPtrVector<Expression> expressions, NonnullRefPtrVector<Expression> raw_strings)
        : Expression(source_range)
        , m_expressions(move(expressions))
        , m_raw_strings(move(raw_strings))
    {
    }

    virtual Completion execute(Interpreter&, GlobalObject&) const override;
    virtual void dump(int indent) const override;
    virtual Bytecode::CodeGenerationErrorOr<void> generate_bytecode(Bytecode::Generator&) const override;

    NonnullRefPtrVector<Expression> const& expressions() const { return m_expressions; }
    NonnullRefPtrVector<Expression> const& raw_strings() const { return m_raw_strings; }

private:
    NonnullRefPtrVector<Expression> const m_expressions;
    NonnullRefPtrVector<Expression> const m_raw_strings;
};

class TaggedTemplateLiteral final : public Expression {
public:
    TaggedTemplateLiteral(SourceRange source_range, NonnullRefPtr<Expression> tag, NonnullRefPtr<TemplateLiteral> template_literal)
        : Expression(source_range)
        , m_tag(move(tag))
        , m_template_literal(move(template_literal))
    {
    }

    virtual Completion execute(Interpreter&, GlobalObject&) const override;
    virtual void dump(int indent) const override;
    virtual Bytecode::CodeGenerationErrorOr<void> generate_bytecode(Bytecode::Generator&) const override;

private:
    NonnullRefPtr<Expression> const m_tag;
    NonnullRefPtr<TemplateLiteral> const m_template_literal;
};

class MemberExpression final : public Expression {
public:
    MemberExpression(SourceRange source_range, NonnullRefPtr<Expression> object, NonnullRefPtr<Expression> property, bool computed = false)
        : Expression(source_range)
        , m_object(move(object))
        , m_property(move(property))
        , m_computed(computed)
    {
    }

    virtual Completion execute(Interpreter&, GlobalObject&) const override;
    virtual void dump(int indent) const override;
    virtual ThrowCompletionOr<Reference> to_reference(Interpreter&, GlobalObject&) const override;
    virtual Bytecode::CodeGenerationErrorOr<void> generate_bytecode(Bytecode::Generator&) const override;

    bool is_computed() const { return m_computed; }
    Expression const& object() const { return *m_object; }
    Expression const& property() const { return *m_property; }

    String to_string_approximation() const;

    bool ends_in_private_name() const;

private:
    virtual bool is_member_expression() const override { return true; }

    NonnullRefPtr<Expression> m_object;
    NonnullRefPtr<Expression> m_property;
    bool m_computed { false };
};

class OptionalChain final : public Expression {
public:
    enum class Mode {
        Optional,
        NotOptional,
    };

    struct Call {
        Vector<CallExpression::Argument> arguments;
        Mode mode;
    };
    struct ComputedReference {
        NonnullRefPtr<Expression> expression;
        Mode mode;
    };
    struct MemberReference {
        NonnullRefPtr<Identifier> identifier;
        Mode mode;
    };
    struct PrivateMemberReference {
        NonnullRefPtr<PrivateIdentifier> private_identifier;
        Mode mode;
    };

    using Reference = Variant<Call, ComputedReference, MemberReference, PrivateMemberReference>;

    OptionalChain(SourceRange source_range, NonnullRefPtr<Expression> base, Vector<Reference> references)
        : Expression(source_range)
        , m_base(move(base))
        , m_references(move(references))
    {
    }

    virtual Completion execute(Interpreter& interpreter, GlobalObject& global_object) const override;
    virtual ThrowCompletionOr<JS::Reference> to_reference(Interpreter& interpreter, GlobalObject& global_object) const override;
    virtual void dump(int indent) const override;

private:
    struct ReferenceAndValue {
        JS::Reference reference;
        Value value;
    };
    ThrowCompletionOr<ReferenceAndValue> to_reference_and_value(Interpreter&, GlobalObject&) const;

    NonnullRefPtr<Expression> m_base;
    Vector<Reference> m_references;
};

class MetaProperty final : public Expression {
public:
    enum class Type {
        NewTarget,
        ImportMeta,
    };

    MetaProperty(SourceRange source_range, Type type)
        : Expression(source_range)
        , m_type(type)
    {
    }

    virtual Completion execute(Interpreter&, GlobalObject&) const override;
    virtual void dump(int indent) const override;

private:
    Type m_type;
};

class ImportCall final : public Expression {
public:
    ImportCall(SourceRange source_range, NonnullRefPtr<Expression> specifier, RefPtr<Expression> options)
        : Expression(source_range)
        , m_specifier(move(specifier))
        , m_options(move(options))
    {
    }

    virtual void dump(int indent) const override;
    virtual Completion execute(Interpreter&, GlobalObject&) const override;

private:
    virtual bool is_import_call() const override { return true; }

    NonnullRefPtr<Expression> m_specifier;
    RefPtr<Expression> m_options;
};

class ConditionalExpression final : public Expression {
public:
    ConditionalExpression(SourceRange source_range, NonnullRefPtr<Expression> test, NonnullRefPtr<Expression> consequent, NonnullRefPtr<Expression> alternate)
        : Expression(source_range)
        , m_test(move(test))
        , m_consequent(move(consequent))
        , m_alternate(move(alternate))
    {
    }

    virtual void dump(int indent) const override;
    virtual Completion execute(Interpreter&, GlobalObject&) const override;
    virtual Bytecode::CodeGenerationErrorOr<void> generate_bytecode(Bytecode::Generator&) const override;

private:
    NonnullRefPtr<Expression> m_test;
    NonnullRefPtr<Expression> m_consequent;
    NonnullRefPtr<Expression> m_alternate;
};

class CatchClause final : public ASTNode {
public:
    CatchClause(SourceRange source_range, FlyString parameter, NonnullRefPtr<BlockStatement> body)
        : ASTNode(source_range)
        , m_parameter(move(parameter))
        , m_body(move(body))
    {
    }

    CatchClause(SourceRange source_range, NonnullRefPtr<BindingPattern> parameter, NonnullRefPtr<BlockStatement> body)
        : ASTNode(source_range)
        , m_parameter(move(parameter))
        , m_body(move(body))
    {
    }

    auto& parameter() const { return m_parameter; }
    BlockStatement const& body() const { return m_body; }

    virtual void dump(int indent) const override;
    virtual Completion execute(Interpreter&, GlobalObject&) const override;

private:
    Variant<FlyString, NonnullRefPtr<BindingPattern>> m_parameter;
    NonnullRefPtr<BlockStatement> m_body;
};

class TryStatement final : public Statement {
public:
    TryStatement(SourceRange source_range, NonnullRefPtr<BlockStatement> block, RefPtr<CatchClause> handler, RefPtr<BlockStatement> finalizer)
        : Statement(source_range)
        , m_block(move(block))
        , m_handler(move(handler))
        , m_finalizer(move(finalizer))
    {
    }

    BlockStatement const& block() const { return m_block; }
    CatchClause const* handler() const { return m_handler; }
    BlockStatement const* finalizer() const { return m_finalizer; }

    virtual void dump(int indent) const override;
    virtual Completion execute(Interpreter&, GlobalObject&) const override;
    virtual Bytecode::CodeGenerationErrorOr<void> generate_bytecode(Bytecode::Generator&) const override;

private:
    NonnullRefPtr<BlockStatement> m_block;
    RefPtr<CatchClause> m_handler;
    RefPtr<BlockStatement> m_finalizer;
};

class ThrowStatement final : public Statement {
public:
    explicit ThrowStatement(SourceRange source_range, NonnullRefPtr<Expression> argument)
        : Statement(source_range)
        , m_argument(move(argument))
    {
    }

    Expression const& argument() const { return m_argument; }

    virtual void dump(int indent) const override;
    virtual Completion execute(Interpreter&, GlobalObject&) const override;
    virtual Bytecode::CodeGenerationErrorOr<void> generate_bytecode(Bytecode::Generator&) const override;

private:
    NonnullRefPtr<Expression> m_argument;
};

class SwitchCase final : public ScopeNode {
public:
    SwitchCase(SourceRange source_range, RefPtr<Expression> test)
        : ScopeNode(source_range)
        , m_test(move(test))
    {
    }

    Expression const* test() const { return m_test; }

    virtual void dump(int indent) const override;
    virtual Completion execute(Interpreter&, GlobalObject&) const override;

private:
    RefPtr<Expression> m_test;
};

class SwitchStatement final : public ScopeNode {
public:
    SwitchStatement(SourceRange source_range, NonnullRefPtr<Expression> discriminant)
        : ScopeNode(source_range)
        , m_discriminant(move(discriminant))
    {
    }

    virtual void dump(int indent) const override;
    virtual Completion execute(Interpreter&, GlobalObject&) const override;
    virtual Bytecode::CodeGenerationErrorOr<void> generate_bytecode(Bytecode::Generator&) const override;

    Completion execute_impl(Interpreter&, GlobalObject&) const;
    void add_case(NonnullRefPtr<SwitchCase> switch_case) { m_cases.append(move(switch_case)); }

private:
    NonnullRefPtr<Expression> m_discriminant;
    NonnullRefPtrVector<SwitchCase> m_cases;
};

class BreakStatement final : public Statement {
public:
    BreakStatement(SourceRange source_range, FlyString target_label)
        : Statement(source_range)
        , m_target_label(move(target_label))
    {
    }

    virtual Completion execute(Interpreter&, GlobalObject&) const override;

    FlyString const& target_label() const { return m_target_label; }
    virtual Bytecode::CodeGenerationErrorOr<void> generate_bytecode(Bytecode::Generator&) const override;

private:
    FlyString m_target_label;
};

class ContinueStatement final : public Statement {
public:
    ContinueStatement(SourceRange source_range, FlyString target_label)
        : Statement(source_range)
        , m_target_label(move(target_label))
    {
    }

    virtual Completion execute(Interpreter&, GlobalObject&) const override;
    virtual Bytecode::CodeGenerationErrorOr<void> generate_bytecode(Bytecode::Generator&) const override;

    FlyString const& target_label() const { return m_target_label; }

private:
    FlyString m_target_label;
};

class DebuggerStatement final : public Statement {
public:
    explicit DebuggerStatement(SourceRange source_range)
        : Statement(source_range)
    {
    }

    virtual Completion execute(Interpreter&, GlobalObject&) const override;
    virtual Bytecode::CodeGenerationErrorOr<void> generate_bytecode(Bytecode::Generator&) const override;
};

class SyntheticReferenceExpression final : public Expression {
public:
    explicit SyntheticReferenceExpression(SourceRange source_range, Reference reference, Value value)
        : Expression(source_range)
        , m_reference(move(reference))
        , m_value(value)
    {
    }

    virtual Completion execute(Interpreter&, GlobalObject&) const override { return m_value; }
    virtual ThrowCompletionOr<Reference> to_reference(Interpreter&, GlobalObject&) const override { return m_reference; }

private:
    Reference m_reference;
    Value m_value;
};

template<>
inline bool ASTNode::fast_is<NewExpression>() const { return is_new_expression(); }

template<>
inline bool ASTNode::fast_is<MemberExpression>() const { return is_member_expression(); }

template<>
inline bool ASTNode::fast_is<SuperExpression>() const { return is_super_expression(); }

template<>
inline bool ASTNode::fast_is<FunctionExpression>() const { return is_function_expression(); }

template<>
inline bool ASTNode::fast_is<ClassExpression>() const { return is_class_expression(); }

template<>
inline bool ASTNode::fast_is<Identifier>() const { return is_identifier(); }

template<>
inline bool ASTNode::fast_is<ExpressionStatement>() const { return is_expression_statement(); }

template<>
inline bool ASTNode::fast_is<ScopeNode>() const { return is_scope_node(); }

template<>
inline bool ASTNode::fast_is<Program>() const { return is_program(); }

template<>
inline bool ASTNode::fast_is<ClassDeclaration>() const { return is_class_declaration(); }

template<>
inline bool ASTNode::fast_is<FunctionDeclaration>() const { return is_function_declaration(); }

template<>
inline bool ASTNode::fast_is<VariableDeclaration>() const { return is_variable_declaration(); }

template<>
inline bool ASTNode::fast_is<ArrayExpression>() const { return is_array_expression(); }

template<>
inline bool ASTNode::fast_is<ObjectExpression>() const { return is_object_expression(); }

template<>
inline bool ASTNode::fast_is<ImportCall>() const { return is_import_call(); }

template<>
inline bool ASTNode::fast_is<StringLiteral>() const { return is_string_literal(); }

template<>
inline bool ASTNode::fast_is<UpdateExpression>() const { return is_update_expression(); }

template<>
inline bool ASTNode::fast_is<CallExpression>() const { return is_call_expression(); }

template<>
inline bool ASTNode::fast_is<LabelledStatement>() const { return is_labelled_statement(); }

template<>
inline bool ASTNode::fast_is<IterationStatement>() const { return is_iteration_statement(); }

template<>
inline bool ASTNode::fast_is<ClassMethod>() const { return is_class_method(); }

}
