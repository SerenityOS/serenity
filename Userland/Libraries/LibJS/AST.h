/*
 * Copyright (c) 2020-2023, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2020-2022, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2021-2022, David Tuin <davidot@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/DeprecatedFlyString.h>
#include <AK/DeprecatedString.h>
#include <AK/HashMap.h>
#include <AK/OwnPtr.h>
#include <AK/RefPtr.h>
#include <AK/Variant.h>
#include <AK/Vector.h>
#include <LibJS/Bytecode/CodeGenerationError.h>
#include <LibJS/Forward.h>
#include <LibJS/Heap/Handle.h>
#include <LibJS/Runtime/ClassFieldDefinition.h>
#include <LibJS/Runtime/Completion.h>
#include <LibJS/Runtime/EnvironmentCoordinate.h>
#include <LibJS/Runtime/FunctionKind.h>
#include <LibJS/Runtime/ModuleRequest.h>
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
    return adopt_ref(*new T(move(range), forward<Args>(args)...));
}

class ASTNode : public RefCounted<ASTNode> {
public:
    virtual ~ASTNode() = default;

    // NOTE: This is here to stop ASAN complaining about mismatch between new/delete sizes in ASTNodeWithTailArray.
    void operator delete(void* ptr) { ::operator delete(ptr); }

    virtual Completion execute(Interpreter&) const = 0;
    virtual Bytecode::CodeGenerationErrorOr<void> generate_bytecode(Bytecode::Generator&) const;
    virtual void dump(int indent) const;

    [[nodiscard]] SourceRange source_range() const;
    u32 start_offset() const { return m_start_offset; }

    void set_end_offset(Badge<Parser>, u32 end_offset) { m_end_offset = end_offset; }

    DeprecatedString class_name() const;

    template<typename T>
    bool fast_is() const = delete;

    virtual bool is_new_expression() const { return false; }
    virtual bool is_member_expression() const { return false; }
    virtual bool is_super_expression() const { return false; }
    virtual bool is_function_expression() const { return false; }
    virtual bool is_class_expression() const { return false; }
    virtual bool is_expression_statement() const { return false; }
    virtual bool is_identifier() const { return false; }
    virtual bool is_private_identifier() const { return false; }
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
    explicit ASTNode(SourceRange);

private:
    // NOTE: These members are carefully ordered so that `m_start_offset` is packed with the padding after RefCounted::m_ref_count.
    //       This creates a 4-byte padding hole after `m_end_offset` which is used to pack subclasses better.
    u32 m_start_offset { 0 };
    RefPtr<SourceCode const> m_source_code;
    u32 m_end_offset { 0 };
};

// This is a helper class that packs an array of T after the AST node, all in the same allocation.
template<typename Derived, typename Base, typename T>
class ASTNodeWithTailArray : public Base {
public:
    virtual ~ASTNodeWithTailArray() override
    {
        for (auto& value : tail_span())
            value.~T();
    }

    ReadonlySpan<T> tail_span() const { return { tail_data(), tail_size() }; }

    T const* tail_data() const { return reinterpret_cast<T const*>(reinterpret_cast<uintptr_t>(this) + sizeof(Derived)); }
    size_t tail_size() const { return m_tail_size; }

protected:
    template<typename ActualDerived = Derived, typename... Args>
    static NonnullRefPtr<ActualDerived> create(size_t tail_size, SourceRange source_range, Args&&... args)
    {
        static_assert(sizeof(ActualDerived) == sizeof(Derived), "This leaf class cannot add more members");
        static_assert(alignof(ActualDerived) % alignof(T) == 0, "Need padding for tail array");
        auto memory = ::operator new(sizeof(ActualDerived) + tail_size * sizeof(T));
        return adopt_ref(*::new (memory) ActualDerived(move(source_range), forward<Args>(args)...));
    }

    ASTNodeWithTailArray(SourceRange source_range, ReadonlySpan<T> values)
        : Base(move(source_range))
        , m_tail_size(values.size())
    {
        VERIFY(values.size() <= NumericLimits<u32>::max());
        for (size_t i = 0; i < values.size(); ++i)
            new (&tail_data()[i]) T(values[i]);
    }

private:
    T* tail_data() { return reinterpret_cast<T*>(reinterpret_cast<uintptr_t>(this) + sizeof(Derived)); }

    u32 m_tail_size { 0 };
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
    LabelledStatement(SourceRange source_range, DeprecatedFlyString label, NonnullRefPtr<Statement const> labelled_item)
        : Statement(source_range)
        , m_label(move(label))
        , m_labelled_item(move(labelled_item))
    {
    }

    virtual Completion execute(Interpreter&) const override;
    virtual void dump(int indent) const override;
    virtual Bytecode::CodeGenerationErrorOr<void> generate_bytecode(Bytecode::Generator&) const override;
    virtual Bytecode::CodeGenerationErrorOr<void> generate_labelled_evaluation(Bytecode::Generator&, Vector<DeprecatedFlyString> const&) const;

    DeprecatedFlyString const& label() const { return m_label; }
    DeprecatedFlyString& label() { return m_label; }
    NonnullRefPtr<Statement const> const& labelled_item() const { return m_labelled_item; }

private:
    virtual bool is_labelled_statement() const final { return true; }

    DeprecatedFlyString m_label;
    NonnullRefPtr<Statement const> m_labelled_item;
};

class LabelableStatement : public Statement {
public:
    using Statement::Statement;

    Vector<DeprecatedFlyString> const& labels() const { return m_labels; }
    virtual void add_label(DeprecatedFlyString string) { m_labels.append(move(string)); }

protected:
    Vector<DeprecatedFlyString> m_labels;
};

class IterationStatement : public Statement {
public:
    using Statement::Statement;

    virtual Completion loop_evaluation(Interpreter&, Vector<DeprecatedFlyString> const&) const = 0;
    virtual Bytecode::CodeGenerationErrorOr<void> generate_labelled_evaluation(Bytecode::Generator&, Vector<DeprecatedFlyString> const&) const;

private:
    virtual bool is_iteration_statement() const final { return true; }
};

class EmptyStatement final : public Statement {
public:
    explicit EmptyStatement(SourceRange source_range)
        : Statement(source_range)
    {
    }
    Completion execute(Interpreter&) const override;
    virtual Bytecode::CodeGenerationErrorOr<void> generate_bytecode(Bytecode::Generator&) const override;
};

class ErrorStatement final : public Statement {
public:
    explicit ErrorStatement(SourceRange source_range)
        : Statement(source_range)
    {
    }
    Completion execute(Interpreter&) const override { return {}; }
};

class ExpressionStatement final : public Statement {
public:
    ExpressionStatement(SourceRange source_range, NonnullRefPtr<Expression const> expression)
        : Statement(source_range)
        , m_expression(move(expression))
    {
    }

    virtual Completion execute(Interpreter&) const override;
    virtual void dump(int indent) const override;
    virtual Bytecode::CodeGenerationErrorOr<void> generate_bytecode(Bytecode::Generator&) const override;

    Expression const& expression() const { return m_expression; };

private:
    virtual bool is_expression_statement() const override { return true; }

    NonnullRefPtr<Expression const> m_expression;
};

template<typename Func, typename... Args>
concept ThrowCompletionOrVoidFunction = requires(Func func, Args... args) {
                                            {
                                                func(args...)
                                                }
                                                -> SameAs<ThrowCompletionOr<void>>;
                                        };

template<typename... Args>
class ThrowCompletionOrVoidCallback : public Function<ThrowCompletionOr<void>(Args...)> {
public:
    template<typename CallableType>
    ThrowCompletionOrVoidCallback(CallableType&& callable)
    requires(VoidFunction<CallableType, Args...>)
        : Function<ThrowCompletionOr<void>(Args...)>([callable = forward<CallableType>(callable)](Args... args) {
            callable(args...);
            return ThrowCompletionOr<void> {};
        })
    {
    }

    template<typename CallableType>
    ThrowCompletionOrVoidCallback(CallableType&& callable)
    requires(ThrowCompletionOrVoidFunction<CallableType, Args...>)
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
        return static_cast<T&>(*m_children.last());
    }
    void append(NonnullRefPtr<Statement const> child)
    {
        m_children.append(move(child));
    }

    void shrink_to_fit()
    {
        m_children.shrink_to_fit();
        m_lexical_declarations.shrink_to_fit();
        m_var_declarations.shrink_to_fit();
        m_functions_hoistable_with_annexB_extension.shrink_to_fit();
    }

    Vector<NonnullRefPtr<Statement const>> const& children() const { return m_children; }
    virtual void dump(int indent) const override;
    virtual Bytecode::CodeGenerationErrorOr<void> generate_bytecode(Bytecode::Generator&) const override;

    Completion evaluate_statements(Interpreter&) const;

    void add_var_scoped_declaration(NonnullRefPtr<Declaration const> variables);
    void add_lexical_declaration(NonnullRefPtr<Declaration const> variables);
    void add_hoisted_function(NonnullRefPtr<FunctionDeclaration const> declaration);

    [[nodiscard]] bool has_lexical_declarations() const { return !m_lexical_declarations.is_empty(); }
    [[nodiscard]] bool has_var_declarations() const { return !m_var_declarations.is_empty(); }

    [[nodiscard]] size_t var_declaration_count() const { return m_var_declarations.size(); }
    [[nodiscard]] size_t lexical_declaration_count() const { return m_lexical_declarations.size(); }

    ThrowCompletionOr<void> for_each_lexically_scoped_declaration(ThrowCompletionOrVoidCallback<Declaration const&>&& callback) const;
    ThrowCompletionOr<void> for_each_lexically_declared_name(ThrowCompletionOrVoidCallback<DeprecatedFlyString const&>&& callback) const;

    ThrowCompletionOr<void> for_each_var_declared_name(ThrowCompletionOrVoidCallback<DeprecatedFlyString const&>&& callback) const;

    ThrowCompletionOr<void> for_each_var_function_declaration_in_reverse_order(ThrowCompletionOrVoidCallback<FunctionDeclaration const&>&& callback) const;
    ThrowCompletionOr<void> for_each_var_scoped_variable_declaration(ThrowCompletionOrVoidCallback<VariableDeclaration const&>&& callback) const;

    void block_declaration_instantiation(Interpreter&, Environment*) const;

    ThrowCompletionOr<void> for_each_function_hoistable_with_annexB_extension(ThrowCompletionOrVoidCallback<FunctionDeclaration&>&& callback) const;

protected:
    explicit ScopeNode(SourceRange source_range)
        : Statement(source_range)
    {
    }

private:
    virtual bool is_scope_node() const final { return true; }

    Vector<NonnullRefPtr<Statement const>> m_children;
    Vector<NonnullRefPtr<Declaration const>> m_lexical_declarations;
    Vector<NonnullRefPtr<Declaration const>> m_var_declarations;

    Vector<NonnullRefPtr<FunctionDeclaration const>> m_functions_hoistable_with_annexB_extension;
};

// ImportEntry Record, https://tc39.es/ecma262/#table-importentry-record-fields
struct ImportEntry {
    DeprecatedFlyString import_name; // [[ImportName]] if a String
    DeprecatedFlyString local_name;  // [[LocalName]]
    bool is_namespace { false };     // [[ImportName]] if `namespace-object`

    ImportEntry(DeprecatedFlyString import_name_, DeprecatedFlyString local_name_, bool is_namespace_ = false)
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
    friend class ImportStatement;
    ModuleRequest* m_module_request; // [[ModuleRequest]]
};

class ImportStatement final : public Statement {
public:
    explicit ImportStatement(SourceRange source_range, ModuleRequest from_module, Vector<ImportEntry> entries = {})
        : Statement(source_range)
        , m_module_request(move(from_module))
        , m_entries(move(entries))
    {
        for (auto& entry : m_entries)
            entry.m_module_request = &m_module_request;
    }

    virtual Completion execute(Interpreter&) const override;

    virtual void dump(int indent) const override;

    bool has_bound_name(DeprecatedFlyString const& name) const;
    Vector<ImportEntry> const& entries() const { return m_entries; }
    ModuleRequest const& module_request() const { return m_module_request; }

private:
    ModuleRequest m_module_request;
    Vector<ImportEntry> m_entries;
};

// ExportEntry Record, https://tc39.es/ecma262/#table-exportentry-records
struct ExportEntry {
    enum class Kind {
        NamedExport,
        ModuleRequestAll,
        ModuleRequestAllButDefault,
        // EmptyNamedExport is a special type for export {} from "module",
        // which should import the module without getting any of the exports
        // however we don't want give it a fake export name which may get
        // duplicates
        EmptyNamedExport,
    } kind;

    DeprecatedFlyString export_name;          // [[ExportName]]
    DeprecatedFlyString local_or_import_name; // Either [[ImportName]] or [[LocalName]]

    ExportEntry(Kind export_kind, DeprecatedFlyString export_name_, DeprecatedFlyString local_or_import_name_)
        : kind(export_kind)
        , export_name(move(export_name_))
        , local_or_import_name(move(local_or_import_name_))
    {
    }

    bool is_module_request() const
    {
        return m_module_request != nullptr;
    }

    static ExportEntry indirect_export_entry(ModuleRequest const& module_request, DeprecatedFlyString export_name, DeprecatedFlyString import_name)
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
    friend class ExportStatement;

public:
    static ExportEntry named_export(DeprecatedFlyString export_name, DeprecatedFlyString local_name)
    {
        return ExportEntry { Kind::NamedExport, move(export_name), move(local_name) };
    }

    static ExportEntry all_but_default_entry()
    {
        return ExportEntry { Kind::ModuleRequestAllButDefault, {}, {} };
    }

    static ExportEntry all_module_request(DeprecatedFlyString export_name)
    {
        return ExportEntry { Kind::ModuleRequestAll, move(export_name), {} };
    }

    static ExportEntry empty_named_export()
    {
        return ExportEntry { Kind::EmptyNamedExport, {}, {} };
    }
};

class ExportStatement final : public Statement {
public:
    static DeprecatedFlyString local_name_for_default;

    ExportStatement(SourceRange source_range, RefPtr<ASTNode const> statement, Vector<ExportEntry> entries, bool is_default_export, ModuleRequest module_request)
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

    virtual Completion execute(Interpreter&) const override;

    virtual void dump(int indent) const override;

    bool has_export(DeprecatedFlyString const& export_name) const;

    bool has_statement() const { return m_statement; }
    Vector<ExportEntry> const& entries() const { return m_entries; }

    bool is_default_export() const { return m_is_default_export; }

    ASTNode const& statement() const
    {
        VERIFY(m_statement);
        return *m_statement;
    }

    ModuleRequest const& module_request() const
    {
        VERIFY(!m_module_request.module_specifier.is_null());
        return m_module_request;
    }

private:
    RefPtr<ASTNode const> m_statement;
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

    virtual Completion execute(Interpreter&) const override;

    bool is_strict_mode() const { return m_is_strict_mode; }
    void set_strict_mode() { m_is_strict_mode = true; }

    Type type() const { return m_type; }

    void append_import(NonnullRefPtr<ImportStatement const> import_statement)
    {
        m_imports.append(import_statement);
        append(import_statement);
    }

    void append_export(NonnullRefPtr<ExportStatement const> export_statement)
    {
        m_exports.append(export_statement);
        append(export_statement);
    }

    Vector<NonnullRefPtr<ImportStatement const>> const& imports() const { return m_imports; }
    Vector<NonnullRefPtr<ExportStatement const>> const& exports() const { return m_exports; }

    Vector<NonnullRefPtr<ImportStatement const>>& imports() { return m_imports; }
    Vector<NonnullRefPtr<ExportStatement const>>& exports() { return m_exports; }

    bool has_top_level_await() const { return m_has_top_level_await; }
    void set_has_top_level_await() { m_has_top_level_await = true; }

    ThrowCompletionOr<void> global_declaration_instantiation(Interpreter&, GlobalEnvironment&) const;

private:
    virtual bool is_program() const override { return true; }

    bool m_is_strict_mode { false };
    Type m_type { Type::Script };

    Vector<NonnullRefPtr<ImportStatement const>> m_imports;
    Vector<NonnullRefPtr<ExportStatement const>> m_exports;
    bool m_has_top_level_await { false };
};

class BlockStatement final : public ScopeNode {
public:
    explicit BlockStatement(SourceRange source_range)
        : ScopeNode(source_range)
    {
    }
    Completion execute(Interpreter&) const override;
};

class FunctionBody final : public ScopeNode {
public:
    explicit FunctionBody(SourceRange source_range)
        : ScopeNode(source_range)
    {
    }

    void set_strict_mode() { m_in_strict_mode = true; }

    bool in_strict_mode() const { return m_in_strict_mode; }

    virtual Completion execute(Interpreter&) const override;

private:
    bool m_in_strict_mode { false };
};

class Expression : public ASTNode {
public:
    explicit Expression(SourceRange source_range)
        : ASTNode(source_range)
    {
    }
    virtual ThrowCompletionOr<Reference> to_reference(Interpreter&) const;
};

class Declaration : public Statement {
public:
    explicit Declaration(SourceRange source_range)
        : Statement(source_range)
    {
    }

    virtual ThrowCompletionOr<void> for_each_bound_name(ThrowCompletionOrVoidCallback<DeprecatedFlyString const&>&& callback) const = 0;

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
    Completion execute(Interpreter&) const override { return {}; }

    ThrowCompletionOr<void> for_each_bound_name(ThrowCompletionOrVoidCallback<DeprecatedFlyString const&>&&) const override
    {
        VERIFY_NOT_REACHED();
    }
};

struct BindingPattern : RefCounted<BindingPattern> {
    // This covers both BindingProperty and BindingElement, hence the more generic name
    struct BindingEntry {
        // If this entry represents a BindingElement, then name will be Empty
        Variant<NonnullRefPtr<Identifier const>, NonnullRefPtr<Expression const>, Empty> name {};
        Variant<NonnullRefPtr<Identifier const>, NonnullRefPtr<BindingPattern const>, NonnullRefPtr<MemberExpression const>, Empty> alias {};
        RefPtr<Expression const> initializer {};
        bool is_rest { false };

        bool is_elision() const { return name.has<Empty>() && alias.has<Empty>(); }
    };

    enum class Kind {
        Array,
        Object,
    };

    void dump(int indent) const;

    ThrowCompletionOr<void> for_each_bound_name(ThrowCompletionOrVoidCallback<DeprecatedFlyString const&>&& callback) const;

    bool contains_expression() const;

    Vector<BindingEntry> entries;
    Kind kind { Kind::Object };
};

struct FunctionParameter {
    Variant<DeprecatedFlyString, NonnullRefPtr<BindingPattern const>> binding;
    RefPtr<Expression const> default_value;
    bool is_rest { false };
};

class FunctionNode {
public:
    DeprecatedFlyString const& name() const { return m_name; }
    DeprecatedString const& source_text() const { return m_source_text; }
    Statement const& body() const { return *m_body; }
    Vector<FunctionParameter> const& parameters() const { return m_parameters; };
    i32 function_length() const { return m_function_length; }
    bool is_strict_mode() const { return m_is_strict_mode; }
    bool might_need_arguments_object() const { return m_might_need_arguments_object; }
    bool contains_direct_call_to_eval() const { return m_contains_direct_call_to_eval; }
    bool is_arrow_function() const { return m_is_arrow_function; }
    FunctionKind kind() const { return m_kind; }

protected:
    FunctionNode(DeprecatedFlyString name, DeprecatedString source_text, NonnullRefPtr<Statement const> body, Vector<FunctionParameter> parameters, i32 function_length, FunctionKind kind, bool is_strict_mode, bool might_need_arguments_object, bool contains_direct_call_to_eval, bool is_arrow_function)
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

    void dump(int indent, DeprecatedString const& class_name) const;

private:
    DeprecatedFlyString m_name;
    DeprecatedString m_source_text;
    NonnullRefPtr<Statement const> m_body;
    Vector<FunctionParameter> const m_parameters;
    const i32 m_function_length;
    FunctionKind m_kind;
    bool m_is_strict_mode : 1 { false };
    bool m_might_need_arguments_object : 1 { false };
    bool m_contains_direct_call_to_eval : 1 { false };
    bool m_is_arrow_function : 1 { false };
};

class FunctionDeclaration final
    : public Declaration
    , public FunctionNode {
public:
    static bool must_have_name() { return true; }

    FunctionDeclaration(SourceRange source_range, DeprecatedFlyString const& name, DeprecatedString source_text, NonnullRefPtr<Statement const> body, Vector<FunctionParameter> parameters, i32 function_length, FunctionKind kind, bool is_strict_mode, bool might_need_arguments_object, bool contains_direct_call_to_eval)
        : Declaration(source_range)
        , FunctionNode(name, move(source_text), move(body), move(parameters), function_length, kind, is_strict_mode, might_need_arguments_object, contains_direct_call_to_eval, false)
    {
    }

    virtual Completion execute(Interpreter&) const override;
    virtual void dump(int indent) const override;
    virtual Bytecode::CodeGenerationErrorOr<void> generate_bytecode(Bytecode::Generator&) const override;

    virtual ThrowCompletionOr<void> for_each_bound_name(ThrowCompletionOrVoidCallback<DeprecatedFlyString const&>&& callback) const override;

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

    FunctionExpression(SourceRange source_range, DeprecatedFlyString const& name, DeprecatedString source_text, NonnullRefPtr<Statement const> body, Vector<FunctionParameter> parameters, i32 function_length, FunctionKind kind, bool is_strict_mode, bool might_need_arguments_object, bool contains_direct_call_to_eval, bool is_arrow_function = false)
        : Expression(source_range)
        , FunctionNode(name, move(source_text), move(body), move(parameters), function_length, kind, is_strict_mode, might_need_arguments_object, contains_direct_call_to_eval, is_arrow_function)
    {
    }

    virtual Completion execute(Interpreter&) const override;
    virtual void dump(int indent) const override;

    virtual Bytecode::CodeGenerationErrorOr<void> generate_bytecode(Bytecode::Generator&) const override;

    bool has_name() const { return !name().is_empty(); }

    Value instantiate_ordinary_function_expression(Interpreter&, DeprecatedFlyString given_name) const;

private:
    virtual bool is_function_expression() const override { return true; }
};

class ErrorExpression final : public Expression {
public:
    explicit ErrorExpression(SourceRange source_range)
        : Expression(source_range)
    {
    }

    Completion execute(Interpreter&) const override { return {}; }
};

class YieldExpression final : public Expression {
public:
    explicit YieldExpression(SourceRange source_range, RefPtr<Expression const> argument, bool is_yield_from)
        : Expression(source_range)
        , m_argument(move(argument))
        , m_is_yield_from(is_yield_from)
    {
    }

    Expression const* argument() const { return m_argument; }
    bool is_yield_from() const { return m_is_yield_from; }

    virtual Completion execute(Interpreter&) const override;
    virtual void dump(int indent) const override;
    virtual Bytecode::CodeGenerationErrorOr<void> generate_bytecode(Bytecode::Generator&) const override;

private:
    RefPtr<Expression const> m_argument;
    bool m_is_yield_from { false };
};

class AwaitExpression final : public Expression {
public:
    explicit AwaitExpression(SourceRange source_range, NonnullRefPtr<Expression const> argument)
        : Expression(source_range)
        , m_argument(move(argument))
    {
    }

    virtual Completion execute(Interpreter&) const override;
    virtual void dump(int indent) const override;
    virtual Bytecode::CodeGenerationErrorOr<void> generate_bytecode(Bytecode::Generator&) const override;

private:
    NonnullRefPtr<Expression const> m_argument;
};

class ReturnStatement final : public Statement {
public:
    explicit ReturnStatement(SourceRange source_range, RefPtr<Expression const> argument)
        : Statement(source_range)
        , m_argument(move(argument))
    {
    }

    Expression const* argument() const { return m_argument; }

    virtual Completion execute(Interpreter&) const override;
    virtual void dump(int indent) const override;
    virtual Bytecode::CodeGenerationErrorOr<void> generate_bytecode(Bytecode::Generator&) const override;

private:
    RefPtr<Expression const> m_argument;
};

class IfStatement final : public Statement {
public:
    IfStatement(SourceRange source_range, NonnullRefPtr<Expression const> predicate, NonnullRefPtr<Statement const> consequent, RefPtr<Statement const> alternate)
        : Statement(source_range)
        , m_predicate(move(predicate))
        , m_consequent(move(consequent))
        , m_alternate(move(alternate))
    {
    }

    Expression const& predicate() const { return *m_predicate; }
    Statement const& consequent() const { return *m_consequent; }
    Statement const* alternate() const { return m_alternate; }

    virtual Completion execute(Interpreter&) const override;
    virtual void dump(int indent) const override;
    virtual Bytecode::CodeGenerationErrorOr<void> generate_bytecode(Bytecode::Generator&) const override;

private:
    NonnullRefPtr<Expression const> m_predicate;
    NonnullRefPtr<Statement const> m_consequent;
    RefPtr<Statement const> m_alternate;
};

class WhileStatement final : public IterationStatement {
public:
    WhileStatement(SourceRange source_range, NonnullRefPtr<Expression const> test, NonnullRefPtr<Statement const> body)
        : IterationStatement(source_range)
        , m_test(move(test))
        , m_body(move(body))
    {
    }

    Expression const& test() const { return *m_test; }
    Statement const& body() const { return *m_body; }

    virtual Completion execute(Interpreter&) const override;
    virtual Completion loop_evaluation(Interpreter&, Vector<DeprecatedFlyString> const&) const override;
    virtual void dump(int indent) const override;
    virtual Bytecode::CodeGenerationErrorOr<void> generate_bytecode(Bytecode::Generator&) const override;
    virtual Bytecode::CodeGenerationErrorOr<void> generate_labelled_evaluation(Bytecode::Generator&, Vector<DeprecatedFlyString> const&) const override;

private:
    NonnullRefPtr<Expression const> m_test;
    NonnullRefPtr<Statement const> m_body;
};

class DoWhileStatement final : public IterationStatement {
public:
    DoWhileStatement(SourceRange source_range, NonnullRefPtr<Expression const> test, NonnullRefPtr<Statement const> body)
        : IterationStatement(source_range)
        , m_test(move(test))
        , m_body(move(body))
    {
    }

    Expression const& test() const { return *m_test; }
    Statement const& body() const { return *m_body; }

    virtual Completion execute(Interpreter&) const override;
    virtual Completion loop_evaluation(Interpreter&, Vector<DeprecatedFlyString> const&) const override;
    virtual void dump(int indent) const override;
    virtual Bytecode::CodeGenerationErrorOr<void> generate_bytecode(Bytecode::Generator&) const override;
    virtual Bytecode::CodeGenerationErrorOr<void> generate_labelled_evaluation(Bytecode::Generator&, Vector<DeprecatedFlyString> const&) const override;

private:
    NonnullRefPtr<Expression const> m_test;
    NonnullRefPtr<Statement const> m_body;
};

class WithStatement final : public Statement {
public:
    WithStatement(SourceRange source_range, NonnullRefPtr<Expression const> object, NonnullRefPtr<Statement const> body)
        : Statement(source_range)
        , m_object(move(object))
        , m_body(move(body))
    {
    }

    Expression const& object() const { return *m_object; }
    Statement const& body() const { return *m_body; }

    virtual Completion execute(Interpreter&) const override;
    virtual void dump(int indent) const override;
    virtual Bytecode::CodeGenerationErrorOr<void> generate_bytecode(Bytecode::Generator&) const override;

private:
    NonnullRefPtr<Expression const> m_object;
    NonnullRefPtr<Statement const> m_body;
};

class ForStatement final : public IterationStatement {
public:
    ForStatement(SourceRange source_range, RefPtr<ASTNode const> init, RefPtr<Expression const> test, RefPtr<Expression const> update, NonnullRefPtr<Statement const> body)
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

    virtual Completion execute(Interpreter&) const override;
    virtual Completion loop_evaluation(Interpreter&, Vector<DeprecatedFlyString> const&) const override;
    virtual void dump(int indent) const override;
    virtual Bytecode::CodeGenerationErrorOr<void> generate_bytecode(Bytecode::Generator&) const override;
    virtual Bytecode::CodeGenerationErrorOr<void> generate_labelled_evaluation(Bytecode::Generator&, Vector<DeprecatedFlyString> const&) const override;

private:
    Completion for_body_evaluation(Interpreter&, Vector<DeprecatedFlyString> const&, size_t per_iteration_bindings_size) const;

    RefPtr<ASTNode const> m_init;
    RefPtr<Expression const> m_test;
    RefPtr<Expression const> m_update;
    NonnullRefPtr<Statement const> m_body;
};

class ForInStatement final : public IterationStatement {
public:
    ForInStatement(SourceRange source_range, Variant<NonnullRefPtr<ASTNode const>, NonnullRefPtr<BindingPattern const>> lhs, NonnullRefPtr<Expression const> rhs, NonnullRefPtr<Statement const> body)
        : IterationStatement(source_range)
        , m_lhs(move(lhs))
        , m_rhs(move(rhs))
        , m_body(move(body))
    {
    }

    auto const& lhs() const { return m_lhs; }
    Expression const& rhs() const { return *m_rhs; }
    Statement const& body() const { return *m_body; }

    virtual Completion execute(Interpreter&) const override;
    virtual Bytecode::CodeGenerationErrorOr<void> generate_bytecode(Bytecode::Generator&) const override;
    virtual Bytecode::CodeGenerationErrorOr<void> generate_labelled_evaluation(Bytecode::Generator&, Vector<DeprecatedFlyString> const&) const override;
    virtual Completion loop_evaluation(Interpreter&, Vector<DeprecatedFlyString> const&) const override;
    virtual void dump(int indent) const override;

private:
    Variant<NonnullRefPtr<ASTNode const>, NonnullRefPtr<BindingPattern const>> m_lhs;
    NonnullRefPtr<Expression const> m_rhs;
    NonnullRefPtr<Statement const> m_body;
};

class ForOfStatement final : public IterationStatement {
public:
    ForOfStatement(SourceRange source_range, Variant<NonnullRefPtr<ASTNode const>, NonnullRefPtr<BindingPattern const>> lhs, NonnullRefPtr<Expression const> rhs, NonnullRefPtr<Statement const> body)
        : IterationStatement(source_range)
        , m_lhs(move(lhs))
        , m_rhs(move(rhs))
        , m_body(move(body))
    {
    }

    auto const& lhs() const { return m_lhs; }
    Expression const& rhs() const { return *m_rhs; }
    Statement const& body() const { return *m_body; }

    virtual Completion execute(Interpreter&) const override;
    virtual Bytecode::CodeGenerationErrorOr<void> generate_bytecode(Bytecode::Generator&) const override;
    virtual Bytecode::CodeGenerationErrorOr<void> generate_labelled_evaluation(Bytecode::Generator&, Vector<DeprecatedFlyString> const&) const override;
    virtual Completion loop_evaluation(Interpreter&, Vector<DeprecatedFlyString> const&) const override;
    virtual void dump(int indent) const override;

private:
    Variant<NonnullRefPtr<ASTNode const>, NonnullRefPtr<BindingPattern const>> m_lhs;
    NonnullRefPtr<Expression const> m_rhs;
    NonnullRefPtr<Statement const> m_body;
};

class ForAwaitOfStatement final : public IterationStatement {
public:
    ForAwaitOfStatement(SourceRange source_range, Variant<NonnullRefPtr<ASTNode const>, NonnullRefPtr<BindingPattern const>> lhs, NonnullRefPtr<Expression const> rhs, NonnullRefPtr<Statement const> body)
        : IterationStatement(source_range)
        , m_lhs(move(lhs))
        , m_rhs(move(rhs))
        , m_body(move(body))
    {
    }

    virtual Completion execute(Interpreter&) const override;
    virtual Completion loop_evaluation(Interpreter&, Vector<DeprecatedFlyString> const&) const override;
    virtual void dump(int indent) const override;

private:
    Variant<NonnullRefPtr<ASTNode const>, NonnullRefPtr<BindingPattern const>> m_lhs;
    NonnullRefPtr<Expression const> m_rhs;
    NonnullRefPtr<Statement const> m_body;
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
    BinaryExpression(SourceRange source_range, BinaryOp op, NonnullRefPtr<Expression const> lhs, NonnullRefPtr<Expression const> rhs)
        : Expression(source_range)
        , m_op(op)
        , m_lhs(move(lhs))
        , m_rhs(move(rhs))
    {
    }

    virtual Completion execute(Interpreter&) const override;
    virtual void dump(int indent) const override;
    virtual Bytecode::CodeGenerationErrorOr<void> generate_bytecode(Bytecode::Generator&) const override;

private:
    BinaryOp m_op;
    NonnullRefPtr<Expression const> m_lhs;
    NonnullRefPtr<Expression const> m_rhs;
};

enum class LogicalOp {
    And,
    Or,
    NullishCoalescing,
};

class LogicalExpression final : public Expression {
public:
    LogicalExpression(SourceRange source_range, LogicalOp op, NonnullRefPtr<Expression const> lhs, NonnullRefPtr<Expression const> rhs)
        : Expression(source_range)
        , m_op(op)
        , m_lhs(move(lhs))
        , m_rhs(move(rhs))
    {
    }

    virtual Completion execute(Interpreter&) const override;
    virtual void dump(int indent) const override;
    virtual Bytecode::CodeGenerationErrorOr<void> generate_bytecode(Bytecode::Generator&) const override;

private:
    LogicalOp m_op;
    NonnullRefPtr<Expression const> m_lhs;
    NonnullRefPtr<Expression const> m_rhs;
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
    UnaryExpression(SourceRange source_range, UnaryOp op, NonnullRefPtr<Expression const> lhs)
        : Expression(source_range)
        , m_op(op)
        , m_lhs(move(lhs))
    {
    }

    virtual Completion execute(Interpreter&) const override;
    virtual void dump(int indent) const override;
    virtual Bytecode::CodeGenerationErrorOr<void> generate_bytecode(Bytecode::Generator&) const override;

private:
    UnaryOp m_op;
    NonnullRefPtr<Expression const> m_lhs;
};

class SequenceExpression final : public Expression {
public:
    SequenceExpression(SourceRange source_range, Vector<NonnullRefPtr<Expression const>> expressions)
        : Expression(source_range)
        , m_expressions(move(expressions))
    {
        VERIFY(m_expressions.size() >= 2);
    }

    virtual void dump(int indent) const override;
    virtual Completion execute(Interpreter&) const override;
    virtual Bytecode::CodeGenerationErrorOr<void> generate_bytecode(Bytecode::Generator&) const override;

private:
    Vector<NonnullRefPtr<Expression const>> m_expressions;
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

    virtual Completion execute(Interpreter&) const override;
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

    virtual Completion execute(Interpreter&) const override;
    virtual void dump(int indent) const override;
    virtual Bytecode::CodeGenerationErrorOr<void> generate_bytecode(Bytecode::Generator&) const override;

private:
    Value m_value;
};

class BigIntLiteral final : public Literal {
public:
    explicit BigIntLiteral(SourceRange source_range, DeprecatedString value)
        : Literal(source_range)
        , m_value(move(value))
    {
    }

    virtual Completion execute(Interpreter&) const override;
    virtual void dump(int indent) const override;
    virtual Bytecode::CodeGenerationErrorOr<void> generate_bytecode(Bytecode::Generator&) const override;

private:
    DeprecatedString m_value;
};

class StringLiteral final : public Literal {
public:
    explicit StringLiteral(SourceRange source_range, DeprecatedString value)
        : Literal(source_range)
        , m_value(move(value))
    {
    }

    virtual Completion execute(Interpreter&) const override;
    virtual void dump(int indent) const override;
    virtual Bytecode::CodeGenerationErrorOr<void> generate_bytecode(Bytecode::Generator&) const override;

    StringView value() const { return m_value; }

private:
    virtual bool is_string_literal() const override { return true; }

    DeprecatedString m_value;
};

class NullLiteral final : public Literal {
public:
    explicit NullLiteral(SourceRange source_range)
        : Literal(source_range)
    {
    }

    virtual Completion execute(Interpreter&) const override;
    virtual void dump(int indent) const override;
    virtual Bytecode::CodeGenerationErrorOr<void> generate_bytecode(Bytecode::Generator&) const override;
};

class RegExpLiteral final : public Literal {
public:
    RegExpLiteral(SourceRange source_range, regex::Parser::Result parsed_regex, DeprecatedString parsed_pattern, regex::RegexOptions<ECMAScriptFlags> parsed_flags, DeprecatedString pattern, DeprecatedString flags)
        : Literal(source_range)
        , m_parsed_regex(move(parsed_regex))
        , m_parsed_pattern(move(parsed_pattern))
        , m_parsed_flags(move(parsed_flags))
        , m_pattern(move(pattern))
        , m_flags(move(flags))
    {
    }

    virtual Completion execute(Interpreter&) const override;
    virtual void dump(int indent) const override;
    virtual Bytecode::CodeGenerationErrorOr<void> generate_bytecode(Bytecode::Generator&) const override;

    regex::Parser::Result const& parsed_regex() const { return m_parsed_regex; }
    DeprecatedString const& parsed_pattern() const { return m_parsed_pattern; }
    regex::RegexOptions<ECMAScriptFlags> const& parsed_flags() const { return m_parsed_flags; }
    DeprecatedString const& pattern() const { return m_pattern; }
    DeprecatedString const& flags() const { return m_flags; }

private:
    regex::Parser::Result m_parsed_regex;
    DeprecatedString m_parsed_pattern;
    regex::RegexOptions<ECMAScriptFlags> m_parsed_flags;
    DeprecatedString m_pattern;
    DeprecatedString m_flags;
};

class Identifier final : public Expression {
public:
    explicit Identifier(SourceRange source_range, DeprecatedFlyString string)
        : Expression(source_range)
        , m_string(move(string))
    {
    }

    DeprecatedFlyString const& string() const { return m_string; }

    virtual Completion execute(Interpreter&) const override;
    virtual void dump(int indent) const override;
    virtual ThrowCompletionOr<Reference> to_reference(Interpreter&) const override;
    virtual Bytecode::CodeGenerationErrorOr<void> generate_bytecode(Bytecode::Generator&) const override;

private:
    virtual bool is_identifier() const override { return true; }

    DeprecatedFlyString m_string;
    mutable EnvironmentCoordinate m_cached_environment_coordinate;
};

class PrivateIdentifier final : public Expression {
public:
    explicit PrivateIdentifier(SourceRange source_range, DeprecatedFlyString string)
        : Expression(source_range)
        , m_string(move(string))
    {
    }

    DeprecatedFlyString const& string() const { return m_string; }

    virtual Completion execute(Interpreter&) const override;
    virtual void dump(int indent) const override;

    virtual bool is_private_identifier() const override { return true; }

private:
    DeprecatedFlyString m_string;
};

class ClassElement : public ASTNode {
public:
    ClassElement(SourceRange source_range, bool is_static)
        : ASTNode(source_range)
        , m_is_static(is_static)
    {
    }

    virtual Completion execute(Interpreter&) const override;

    enum class ElementKind {
        Method,
        Field,
        StaticInitializer,
    };

    virtual ElementKind class_element_kind() const = 0;
    bool is_static() const { return m_is_static; }

    // We use the Completion also as a ClassStaticBlockDefinition Record.
    using ClassValue = Variant<ClassFieldDefinition, Completion, PrivateElement>;
    virtual ThrowCompletionOr<ClassValue> class_element_evaluation(Interpreter&, Object& home_object) const = 0;

    virtual Optional<DeprecatedFlyString> private_bound_identifier() const { return {}; };

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

    ClassMethod(SourceRange source_range, NonnullRefPtr<Expression const> key, NonnullRefPtr<FunctionExpression const> function, Kind kind, bool is_static)
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
    virtual ThrowCompletionOr<ClassValue> class_element_evaluation(Interpreter&, Object& home_object) const override;
    virtual Optional<DeprecatedFlyString> private_bound_identifier() const override;

private:
    virtual bool is_class_method() const override { return true; }
    NonnullRefPtr<Expression const> m_key;
    NonnullRefPtr<FunctionExpression const> m_function;
    Kind m_kind;
};

class ClassField final : public ClassElement {
public:
    ClassField(SourceRange source_range, NonnullRefPtr<Expression const> key, RefPtr<Expression const> init, bool contains_direct_call_to_eval, bool is_static)
        : ClassElement(source_range, is_static)
        , m_key(move(key))
        , m_initializer(move(init))
        , m_contains_direct_call_to_eval(contains_direct_call_to_eval)
    {
    }

    Expression const& key() const { return *m_key; }
    RefPtr<Expression const> const& initializer() const { return m_initializer; }
    RefPtr<Expression const>& initializer() { return m_initializer; }

    virtual ElementKind class_element_kind() const override { return ElementKind::Field; }

    virtual void dump(int indent) const override;
    virtual ThrowCompletionOr<ClassValue> class_element_evaluation(Interpreter&, Object& home_object) const override;
    virtual Optional<DeprecatedFlyString> private_bound_identifier() const override;

private:
    NonnullRefPtr<Expression const> m_key;
    RefPtr<Expression const> m_initializer;
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
    virtual ThrowCompletionOr<ClassValue> class_element_evaluation(Interpreter&, Object& home_object) const override;

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

    virtual Completion execute(Interpreter&) const override;
    virtual void dump(int indent) const override;

    virtual bool is_super_expression() const override { return true; }
};

class ClassExpression final : public Expression {
public:
    ClassExpression(SourceRange source_range, DeprecatedString name, DeprecatedString source_text, RefPtr<FunctionExpression const> constructor, RefPtr<Expression const> super_class, Vector<NonnullRefPtr<ClassElement const>> elements)
        : Expression(source_range)
        , m_name(move(name))
        , m_source_text(move(source_text))
        , m_constructor(move(constructor))
        , m_super_class(move(super_class))
        , m_elements(move(elements))
    {
    }

    StringView name() const { return m_name; }
    DeprecatedString const& source_text() const { return m_source_text; }
    RefPtr<FunctionExpression const> constructor() const { return m_constructor; }

    virtual Completion execute(Interpreter&) const override;
    virtual void dump(int indent) const override;
    virtual Bytecode::CodeGenerationErrorOr<void> generate_bytecode(Bytecode::Generator&) const override;

    bool has_name() const { return !m_name.is_empty(); }

    ThrowCompletionOr<ECMAScriptFunctionObject*> class_definition_evaluation(Interpreter&, DeprecatedFlyString const& binding_name = {}, DeprecatedFlyString const& class_name = {}) const;

private:
    virtual bool is_class_expression() const override { return true; }

    DeprecatedString m_name;
    DeprecatedString m_source_text;
    RefPtr<FunctionExpression const> m_constructor;
    RefPtr<Expression const> m_super_class;
    Vector<NonnullRefPtr<ClassElement const>> m_elements;
};

class ClassDeclaration final : public Declaration {
public:
    ClassDeclaration(SourceRange source_range, NonnullRefPtr<ClassExpression const> class_expression)
        : Declaration(source_range)
        , m_class_expression(move(class_expression))
    {
    }

    virtual Completion execute(Interpreter&) const override;
    virtual void dump(int indent) const override;
    virtual Bytecode::CodeGenerationErrorOr<void> generate_bytecode(Bytecode::Generator&) const override;

    virtual ThrowCompletionOr<void> for_each_bound_name(ThrowCompletionOrVoidCallback<DeprecatedFlyString const&>&& callback) const override;

    virtual bool is_lexical_declaration() const override { return true; }

    StringView name() const { return m_class_expression->name(); }

private:
    virtual bool is_class_declaration() const override { return true; }

    friend ExportStatement;

    NonnullRefPtr<ClassExpression const> m_class_expression;
};

class SpreadExpression final : public Expression {
public:
    explicit SpreadExpression(SourceRange source_range, NonnullRefPtr<Expression const> target)
        : Expression(source_range)
        , m_target(move(target))
    {
    }

    virtual Completion execute(Interpreter&) const override;
    virtual void dump(int indent) const override;
    virtual Bytecode::CodeGenerationErrorOr<void> generate_bytecode(Bytecode::Generator&) const override;

private:
    NonnullRefPtr<Expression const> m_target;
};

class ThisExpression final : public Expression {
public:
    explicit ThisExpression(SourceRange source_range)
        : Expression(source_range)
    {
    }
    virtual Completion execute(Interpreter&) const override;
    virtual void dump(int indent) const override;
    virtual Bytecode::CodeGenerationErrorOr<void> generate_bytecode(Bytecode::Generator&) const override;
};

struct CallExpressionArgument {
    NonnullRefPtr<Expression const> value;
    bool is_spread;
};

class CallExpression : public ASTNodeWithTailArray<CallExpression, Expression, CallExpressionArgument> {
    friend class ASTNodeWithTailArray;

public:
    using Argument = CallExpressionArgument;

    static NonnullRefPtr<CallExpression> create(SourceRange, NonnullRefPtr<Expression const> callee, ReadonlySpan<Argument> arguments);

    virtual Completion execute(Interpreter&) const override;
    virtual void dump(int indent) const override;
    virtual Bytecode::CodeGenerationErrorOr<void> generate_bytecode(Bytecode::Generator&) const override;

    Expression const& callee() const { return m_callee; }

    ReadonlySpan<Argument> arguments() const { return tail_span(); }

protected:
    CallExpression(SourceRange source_range, NonnullRefPtr<Expression const> callee, ReadonlySpan<Argument> arguments)
        : ASTNodeWithTailArray(move(source_range), arguments)
        , m_callee(move(callee))
    {
    }

private:
    struct ThisAndCallee {
        Value this_value;
        Value callee;
    };
    ThrowCompletionOr<ThisAndCallee> compute_this_and_callee(Interpreter&, Reference const&) const;

protected:
    virtual bool is_call_expression() const override { return true; }

    Completion throw_type_error_for_callee(Interpreter&, Value callee_value, StringView call_type) const;
    Optional<DeprecatedString> expression_string() const;

    NonnullRefPtr<Expression const> m_callee;
};

class NewExpression final : public CallExpression {
    friend class ASTNodeWithTailArray;

public:
    static NonnullRefPtr<NewExpression> create(SourceRange, NonnullRefPtr<Expression const> callee, ReadonlySpan<Argument> arguments);

    virtual Completion execute(Interpreter&) const override;

    virtual bool is_new_expression() const override { return true; }

private:
    NewExpression(SourceRange source_range, NonnullRefPtr<Expression const> callee, ReadonlySpan<Argument> arguments)
        : CallExpression(move(source_range), move(callee), arguments)
    {
    }
};

static_assert(sizeof(NewExpression) == sizeof(CallExpression), "Adding members to NewExpression will break CallExpression memory layout");

class SuperCall final : public Expression {
public:
    // This is here to be able to make a constructor like
    // constructor(...args) { super(...args); } which does not use @@iterator of %Array.prototype%.
    enum class IsPartOfSyntheticConstructor {
        No,
        Yes,
    };

    SuperCall(SourceRange source_range, Vector<CallExpression::Argument> arguments)
        : Expression(source_range)
        , m_arguments(move(arguments))
        , m_is_synthetic(IsPartOfSyntheticConstructor::No)
    {
    }

    SuperCall(SourceRange source_range, IsPartOfSyntheticConstructor is_part_of_synthetic_constructor, CallExpression::Argument constructor_argument)
        : Expression(source_range)
        , m_arguments({ move(constructor_argument) })
        , m_is_synthetic(IsPartOfSyntheticConstructor::Yes)
    {
        VERIFY(is_part_of_synthetic_constructor == IsPartOfSyntheticConstructor::Yes);
    }

    virtual Completion execute(Interpreter&) const override;
    virtual void dump(int indent) const override;
    virtual Bytecode::CodeGenerationErrorOr<void> generate_bytecode(Bytecode::Generator&) const override;

private:
    Vector<CallExpression::Argument> const m_arguments;
    IsPartOfSyntheticConstructor const m_is_synthetic;
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
    AssignmentExpression(SourceRange source_range, AssignmentOp op, NonnullRefPtr<Expression const> lhs, NonnullRefPtr<Expression const> rhs)
        : Expression(source_range)
        , m_op(op)
        , m_lhs(move(lhs))
        , m_rhs(move(rhs))
    {
    }

    AssignmentExpression(SourceRange source_range, AssignmentOp op, NonnullRefPtr<BindingPattern const> lhs, NonnullRefPtr<Expression const> rhs)
        : Expression(source_range)
        , m_op(op)
        , m_lhs(move(lhs))
        , m_rhs(move(rhs))
    {
    }

    virtual Completion execute(Interpreter&) const override;
    virtual void dump(int indent) const override;
    virtual Bytecode::CodeGenerationErrorOr<void> generate_bytecode(Bytecode::Generator&) const override;

private:
    AssignmentOp m_op;
    Variant<NonnullRefPtr<Expression const>, NonnullRefPtr<BindingPattern const>> m_lhs;
    NonnullRefPtr<Expression const> m_rhs;
};

enum class UpdateOp {
    Increment,
    Decrement,
};

class UpdateExpression final : public Expression {
public:
    UpdateExpression(SourceRange source_range, UpdateOp op, NonnullRefPtr<Expression const> argument, bool prefixed = false)
        : Expression(source_range)
        , m_op(op)
        , m_argument(move(argument))
        , m_prefixed(prefixed)
    {
    }

    virtual Completion execute(Interpreter&) const override;
    virtual void dump(int indent) const override;
    virtual Bytecode::CodeGenerationErrorOr<void> generate_bytecode(Bytecode::Generator&) const override;

private:
    virtual bool is_update_expression() const override { return true; }

    UpdateOp m_op;
    NonnullRefPtr<Expression const> m_argument;
    bool m_prefixed;
};

enum class DeclarationKind {
    Var,
    Let,
    Const,
};

class VariableDeclarator final : public ASTNode {
public:
    VariableDeclarator(SourceRange source_range, NonnullRefPtr<Identifier const> id)
        : ASTNode(source_range)
        , m_target(move(id))
    {
    }

    VariableDeclarator(SourceRange source_range, NonnullRefPtr<Identifier const> target, RefPtr<Expression const> init)
        : ASTNode(source_range)
        , m_target(move(target))
        , m_init(move(init))
    {
    }

    VariableDeclarator(SourceRange source_range, Variant<NonnullRefPtr<Identifier const>, NonnullRefPtr<BindingPattern const>> target, RefPtr<Expression const> init)
        : ASTNode(source_range)
        , m_target(move(target))
        , m_init(move(init))
    {
    }

    auto& target() const { return m_target; }
    Expression const* init() const { return m_init; }

    virtual Completion execute(Interpreter&) const override;
    virtual void dump(int indent) const override;

private:
    Variant<NonnullRefPtr<Identifier const>, NonnullRefPtr<BindingPattern const>> m_target;
    RefPtr<Expression const> m_init;
};

class VariableDeclaration final : public Declaration {
public:
    VariableDeclaration(SourceRange source_range, DeclarationKind declaration_kind, Vector<NonnullRefPtr<VariableDeclarator const>> declarations)
        : Declaration(source_range)
        , m_declaration_kind(declaration_kind)
        , m_declarations(move(declarations))
    {
    }

    DeclarationKind declaration_kind() const { return m_declaration_kind; }

    virtual Completion execute(Interpreter&) const override;
    virtual void dump(int indent) const override;
    virtual Bytecode::CodeGenerationErrorOr<void> generate_bytecode(Bytecode::Generator&) const override;

    Vector<NonnullRefPtr<VariableDeclarator const>> const& declarations() const { return m_declarations; }

    virtual ThrowCompletionOr<void> for_each_bound_name(ThrowCompletionOrVoidCallback<DeprecatedFlyString const&>&& callback) const override;

    virtual bool is_constant_declaration() const override { return m_declaration_kind == DeclarationKind::Const; };

    virtual bool is_lexical_declaration() const override { return m_declaration_kind != DeclarationKind::Var; }

private:
    virtual bool is_variable_declaration() const override { return true; }

    DeclarationKind m_declaration_kind;
    Vector<NonnullRefPtr<VariableDeclarator const>> m_declarations;
};

class UsingDeclaration final : public Declaration {
public:
    UsingDeclaration(SourceRange source_range, Vector<NonnullRefPtr<VariableDeclarator const>> declarations)
        : Declaration(move(source_range))
        , m_declarations(move(declarations))
    {
    }

    virtual Completion execute(Interpreter&) const override;
    virtual void dump(int indent) const override;

    virtual ThrowCompletionOr<void> for_each_bound_name(ThrowCompletionOrVoidCallback<DeprecatedFlyString const&>&& callback) const override;

    virtual bool is_constant_declaration() const override { return true; };

    virtual bool is_lexical_declaration() const override { return true; }

    Vector<NonnullRefPtr<VariableDeclarator const>> const& declarations() const { return m_declarations; }

private:
    Vector<NonnullRefPtr<VariableDeclarator const>> m_declarations;
};

class ObjectProperty final : public ASTNode {
public:
    enum class Type : u8 {
        KeyValue,
        Getter,
        Setter,
        Spread,
        ProtoSetter,
    };

    ObjectProperty(SourceRange source_range, NonnullRefPtr<Expression const> key, RefPtr<Expression const> value, Type property_type, bool is_method)
        : ASTNode(source_range)
        , m_property_type(property_type)
        , m_is_method(is_method)
        , m_key(move(key))
        , m_value(move(value))
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
    virtual Completion execute(Interpreter&) const override;

private:
    Type m_property_type;
    bool m_is_method { false };
    NonnullRefPtr<Expression const> m_key;
    RefPtr<Expression const> m_value;
};

class ObjectExpression final : public Expression {
public:
    explicit ObjectExpression(SourceRange source_range, Vector<NonnullRefPtr<ObjectProperty>> properties = {})
        : Expression(source_range)
        , m_properties(move(properties))
    {
    }

    virtual Completion execute(Interpreter&) const override;
    virtual void dump(int indent) const override;
    virtual Bytecode::CodeGenerationErrorOr<void> generate_bytecode(Bytecode::Generator&) const override;

private:
    virtual bool is_object_expression() const override { return true; }

    Vector<NonnullRefPtr<ObjectProperty>> m_properties;
};

class ArrayExpression final : public Expression {
public:
    ArrayExpression(SourceRange source_range, Vector<RefPtr<Expression const>> elements)
        : Expression(source_range)
        , m_elements(move(elements))
    {
    }

    Vector<RefPtr<Expression const>> const& elements() const { return m_elements; }

    virtual Completion execute(Interpreter&) const override;
    virtual void dump(int indent) const override;
    virtual Bytecode::CodeGenerationErrorOr<void> generate_bytecode(Bytecode::Generator&) const override;

private:
    virtual bool is_array_expression() const override { return true; }

    Vector<RefPtr<Expression const>> m_elements;
};

class TemplateLiteral final : public Expression {
public:
    TemplateLiteral(SourceRange source_range, Vector<NonnullRefPtr<Expression const>> expressions)
        : Expression(source_range)
        , m_expressions(move(expressions))
    {
    }

    TemplateLiteral(SourceRange source_range, Vector<NonnullRefPtr<Expression const>> expressions, Vector<NonnullRefPtr<Expression const>> raw_strings)
        : Expression(source_range)
        , m_expressions(move(expressions))
        , m_raw_strings(move(raw_strings))
    {
    }

    virtual Completion execute(Interpreter&) const override;
    virtual void dump(int indent) const override;
    virtual Bytecode::CodeGenerationErrorOr<void> generate_bytecode(Bytecode::Generator&) const override;

    Vector<NonnullRefPtr<Expression const>> const& expressions() const { return m_expressions; }
    Vector<NonnullRefPtr<Expression const>> const& raw_strings() const { return m_raw_strings; }

private:
    Vector<NonnullRefPtr<Expression const>> const m_expressions;
    Vector<NonnullRefPtr<Expression const>> const m_raw_strings;
};

class TaggedTemplateLiteral final : public Expression {
public:
    TaggedTemplateLiteral(SourceRange source_range, NonnullRefPtr<Expression const> tag, NonnullRefPtr<TemplateLiteral const> template_literal)
        : Expression(source_range)
        , m_tag(move(tag))
        , m_template_literal(move(template_literal))
    {
    }

    virtual Completion execute(Interpreter&) const override;
    virtual void dump(int indent) const override;
    virtual Bytecode::CodeGenerationErrorOr<void> generate_bytecode(Bytecode::Generator&) const override;

    ThrowCompletionOr<Value> get_template_object(Interpreter&) const;

private:
    NonnullRefPtr<Expression const> const m_tag;
    NonnullRefPtr<TemplateLiteral const> const m_template_literal;
    mutable HashMap<GCPtr<Realm>, Handle<Array>> m_cached_values;
};

class MemberExpression final : public Expression {
public:
    MemberExpression(SourceRange source_range, NonnullRefPtr<Expression const> object, NonnullRefPtr<Expression const> property, bool computed = false)
        : Expression(source_range)
        , m_computed(computed)
        , m_object(move(object))
        , m_property(move(property))
    {
    }

    virtual Completion execute(Interpreter&) const override;
    virtual void dump(int indent) const override;
    virtual ThrowCompletionOr<Reference> to_reference(Interpreter&) const override;
    virtual Bytecode::CodeGenerationErrorOr<void> generate_bytecode(Bytecode::Generator&) const override;

    bool is_computed() const { return m_computed; }
    Expression const& object() const { return *m_object; }
    Expression const& property() const { return *m_property; }

    DeprecatedString to_string_approximation() const;

    bool ends_in_private_name() const;

private:
    virtual bool is_member_expression() const override { return true; }

    bool m_computed { false };
    NonnullRefPtr<Expression const> m_object;
    NonnullRefPtr<Expression const> m_property;
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
        NonnullRefPtr<Expression const> expression;
        Mode mode;
    };
    struct MemberReference {
        NonnullRefPtr<Identifier const> identifier;
        Mode mode;
    };
    struct PrivateMemberReference {
        NonnullRefPtr<PrivateIdentifier const> private_identifier;
        Mode mode;
    };

    using Reference = Variant<Call, ComputedReference, MemberReference, PrivateMemberReference>;

    OptionalChain(SourceRange source_range, NonnullRefPtr<Expression const> base, Vector<Reference> references)
        : Expression(source_range)
        , m_base(move(base))
        , m_references(move(references))
    {
    }

    virtual Completion execute(Interpreter&) const override;
    virtual ThrowCompletionOr<JS::Reference> to_reference(Interpreter&) const override;
    virtual void dump(int indent) const override;

private:
    struct ReferenceAndValue {
        JS::Reference reference;
        Value value;
    };
    ThrowCompletionOr<ReferenceAndValue> to_reference_and_value(Interpreter&) const;

    NonnullRefPtr<Expression const> m_base;
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

    virtual Completion execute(Interpreter&) const override;
    virtual void dump(int indent) const override;
    virtual Bytecode::CodeGenerationErrorOr<void> generate_bytecode(Bytecode::Generator&) const override;

private:
    Type m_type;
};

class ImportCall final : public Expression {
public:
    ImportCall(SourceRange source_range, NonnullRefPtr<Expression const> specifier, RefPtr<Expression const> options)
        : Expression(source_range)
        , m_specifier(move(specifier))
        , m_options(move(options))
    {
    }

    virtual void dump(int indent) const override;
    virtual Completion execute(Interpreter&) const override;

private:
    virtual bool is_import_call() const override { return true; }

    NonnullRefPtr<Expression const> m_specifier;
    RefPtr<Expression const> m_options;
};

class ConditionalExpression final : public Expression {
public:
    ConditionalExpression(SourceRange source_range, NonnullRefPtr<Expression const> test, NonnullRefPtr<Expression const> consequent, NonnullRefPtr<Expression const> alternate)
        : Expression(source_range)
        , m_test(move(test))
        , m_consequent(move(consequent))
        , m_alternate(move(alternate))
    {
    }

    virtual void dump(int indent) const override;
    virtual Completion execute(Interpreter&) const override;
    virtual Bytecode::CodeGenerationErrorOr<void> generate_bytecode(Bytecode::Generator&) const override;

private:
    NonnullRefPtr<Expression const> m_test;
    NonnullRefPtr<Expression const> m_consequent;
    NonnullRefPtr<Expression const> m_alternate;
};

class CatchClause final : public ASTNode {
public:
    CatchClause(SourceRange source_range, DeprecatedFlyString parameter, NonnullRefPtr<BlockStatement const> body)
        : ASTNode(source_range)
        , m_parameter(move(parameter))
        , m_body(move(body))
    {
    }

    CatchClause(SourceRange source_range, NonnullRefPtr<BindingPattern const> parameter, NonnullRefPtr<BlockStatement const> body)
        : ASTNode(source_range)
        , m_parameter(move(parameter))
        , m_body(move(body))
    {
    }

    auto& parameter() const { return m_parameter; }
    BlockStatement const& body() const { return m_body; }

    virtual void dump(int indent) const override;
    virtual Completion execute(Interpreter&) const override;

private:
    Variant<DeprecatedFlyString, NonnullRefPtr<BindingPattern const>> m_parameter;
    NonnullRefPtr<BlockStatement const> m_body;
};

class TryStatement final : public Statement {
public:
    TryStatement(SourceRange source_range, NonnullRefPtr<BlockStatement const> block, RefPtr<CatchClause const> handler, RefPtr<BlockStatement const> finalizer)
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
    virtual Completion execute(Interpreter&) const override;
    virtual Bytecode::CodeGenerationErrorOr<void> generate_bytecode(Bytecode::Generator&) const override;

private:
    NonnullRefPtr<BlockStatement const> m_block;
    RefPtr<CatchClause const> m_handler;
    RefPtr<BlockStatement const> m_finalizer;
};

class ThrowStatement final : public Statement {
public:
    explicit ThrowStatement(SourceRange source_range, NonnullRefPtr<Expression const> argument)
        : Statement(source_range)
        , m_argument(move(argument))
    {
    }

    Expression const& argument() const { return m_argument; }

    virtual void dump(int indent) const override;
    virtual Completion execute(Interpreter&) const override;
    virtual Bytecode::CodeGenerationErrorOr<void> generate_bytecode(Bytecode::Generator&) const override;

private:
    NonnullRefPtr<Expression const> m_argument;
};

class SwitchCase final : public ScopeNode {
public:
    SwitchCase(SourceRange source_range, RefPtr<Expression const> test)
        : ScopeNode(source_range)
        , m_test(move(test))
    {
    }

    Expression const* test() const { return m_test; }

    virtual void dump(int indent) const override;
    virtual Completion execute(Interpreter&) const override;

private:
    RefPtr<Expression const> m_test;
};

class SwitchStatement final : public ScopeNode {
public:
    SwitchStatement(SourceRange source_range, NonnullRefPtr<Expression const> discriminant)
        : ScopeNode(source_range)
        , m_discriminant(move(discriminant))
    {
    }

    virtual void dump(int indent) const override;
    virtual Completion execute(Interpreter&) const override;
    virtual Bytecode::CodeGenerationErrorOr<void> generate_bytecode(Bytecode::Generator&) const override;
    virtual Bytecode::CodeGenerationErrorOr<void> generate_labelled_evaluation(Bytecode::Generator&, Vector<DeprecatedFlyString> const&) const;

    Completion execute_impl(Interpreter&) const;
    void add_case(NonnullRefPtr<SwitchCase const> switch_case) { m_cases.append(move(switch_case)); }

private:
    NonnullRefPtr<Expression const> m_discriminant;
    Vector<NonnullRefPtr<SwitchCase const>> m_cases;
};

class BreakStatement final : public Statement {
public:
    BreakStatement(SourceRange source_range, DeprecatedFlyString target_label)
        : Statement(source_range)
        , m_target_label(move(target_label))
    {
    }

    virtual Completion execute(Interpreter&) const override;

    DeprecatedFlyString const& target_label() const { return m_target_label; }
    virtual Bytecode::CodeGenerationErrorOr<void> generate_bytecode(Bytecode::Generator&) const override;

private:
    DeprecatedFlyString m_target_label;
};

class ContinueStatement final : public Statement {
public:
    ContinueStatement(SourceRange source_range, DeprecatedFlyString target_label)
        : Statement(source_range)
        , m_target_label(move(target_label))
    {
    }

    virtual Completion execute(Interpreter&) const override;
    virtual Bytecode::CodeGenerationErrorOr<void> generate_bytecode(Bytecode::Generator&) const override;

    DeprecatedFlyString const& target_label() const { return m_target_label; }

private:
    DeprecatedFlyString m_target_label;
};

class DebuggerStatement final : public Statement {
public:
    explicit DebuggerStatement(SourceRange source_range)
        : Statement(source_range)
    {
    }

    virtual Completion execute(Interpreter&) const override;
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

    virtual Completion execute(Interpreter&) const override { return m_value; }
    virtual ThrowCompletionOr<Reference> to_reference(Interpreter&) const override { return m_reference; }

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
inline bool ASTNode::fast_is<PrivateIdentifier>() const { return is_private_identifier(); }

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
