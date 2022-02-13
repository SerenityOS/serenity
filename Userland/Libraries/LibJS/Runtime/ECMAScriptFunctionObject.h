/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2020-2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/AST.h>
#include <LibJS/Bytecode/Generator.h>
#include <LibJS/Runtime/ExecutionContext.h>
#include <LibJS/Runtime/FunctionObject.h>

namespace JS {

void async_block_start(VM&, NonnullRefPtr<Statement> const& parse_node, PromiseCapability const&, ExecutionContext&);

// 10.2 ECMAScript Function Objects, https://tc39.es/ecma262/#sec-ecmascript-function-objects
class ECMAScriptFunctionObject final : public FunctionObject {
    JS_OBJECT(ECMAScriptFunctionObject, FunctionObject);

public:
    enum class ConstructorKind : u8 {
        Base,
        Derived,
    };

    enum class ThisMode : u8 {
        Lexical,
        Strict,
        Global,
    };

    static ECMAScriptFunctionObject* create(GlobalObject&, FlyString name, String source_text, Statement const& ecmascript_code, Vector<FunctionNode::Parameter> parameters, i32 m_function_length, Environment* parent_scope, PrivateEnvironment* private_scope, FunctionKind, bool is_strict, bool might_need_arguments_object = true, bool contains_direct_call_to_eval = true, bool is_arrow_function = false);
    static ECMAScriptFunctionObject* create(GlobalObject&, FlyString name, Object& prototype, String source_text, Statement const& ecmascript_code, Vector<FunctionNode::Parameter> parameters, i32 m_function_length, Environment* parent_scope, PrivateEnvironment* private_scope, FunctionKind, bool is_strict, bool might_need_arguments_object = true, bool contains_direct_call_to_eval = true, bool is_arrow_function = false);

    ECMAScriptFunctionObject(FlyString name, String source_text, Statement const& ecmascript_code, Vector<FunctionNode::Parameter> parameters, i32 m_function_length, Environment* parent_scope, PrivateEnvironment* private_scope, Object& prototype, FunctionKind, bool is_strict, bool might_need_arguments_object, bool contains_direct_call_to_eval, bool is_arrow_function);
    virtual void initialize(GlobalObject&) override;
    virtual ~ECMAScriptFunctionObject();

    virtual ThrowCompletionOr<Value> internal_call(Value this_argument, MarkedVector<Value> arguments_list) override;
    virtual ThrowCompletionOr<Object*> internal_construct(MarkedVector<Value> arguments_list, FunctionObject& new_target) override;

    void make_method(Object& home_object);

    Statement const& ecmascript_code() const { return m_ecmascript_code; }
    Vector<FunctionNode::Parameter> const& formal_parameters() const { return m_formal_parameters; };

    virtual const FlyString& name() const override { return m_name; };
    void set_name(const FlyString& name);

    void set_is_class_constructor() { m_is_class_constructor = true; };

    auto& bytecode_executable() const { return m_bytecode_executable; }

    Environment* environment() { return m_environment; }
    virtual Realm* realm() const override { return m_realm; }

    ConstructorKind constructor_kind() const { return m_constructor_kind; };
    void set_constructor_kind(ConstructorKind constructor_kind) { m_constructor_kind = constructor_kind; }

    ThisMode this_mode() const { return m_this_mode; }

    Object* home_object() const { return m_home_object; }
    void set_home_object(Object* home_object) { m_home_object = home_object; }

    String const& source_text() const { return m_source_text; }
    void set_source_text(String source_text) { m_source_text = move(source_text); }

    struct InstanceField {
        Variant<PropertyKey, PrivateName> name;
        ECMAScriptFunctionObject* initializer { nullptr };
    };

    Vector<InstanceField> const& fields() const { return m_fields; }
    void add_field(Variant<PropertyKey, PrivateName> property_key, ECMAScriptFunctionObject* initializer);

    Vector<PrivateElement> const& private_methods() const { return m_private_methods; }
    void add_private_method(PrivateElement method) { m_private_methods.append(move(method)); };

    // This is for IsSimpleParameterList (static semantics)
    bool has_simple_parameter_list() const { return m_has_simple_parameter_list; }

    // Equivalent to absence of [[Construct]]
    virtual bool has_constructor() const override { return m_kind == FunctionKind::Normal && !m_is_arrow_function; }

    FunctionKind kind() const { return m_kind; }

    // This is used by LibWeb to disassociate event handler attribute callback functions from the nearest script on the call stack.
    // https://html.spec.whatwg.org/multipage/webappapis.html#getting-the-current-value-of-the-event-handler Step 3.11
    void set_script_or_module(ScriptOrModule script_or_module) { m_script_or_module = move(script_or_module); }

protected:
    virtual bool is_strict_mode() const final { return m_strict; }

    virtual Completion ordinary_call_evaluate_body();

private:
    virtual bool is_ecmascript_function_object() const override { return true; }
    virtual void visit_edges(Visitor&) override;

    ThrowCompletionOr<void> prepare_for_ordinary_call(ExecutionContext& callee_context, Object* new_target);
    void ordinary_call_bind_this(ExecutionContext&, Value this_argument);

    void async_function_start(PromiseCapability const&);

    ThrowCompletionOr<void> function_declaration_instantiation(Interpreter*);

    FlyString m_name;
    OwnPtr<Bytecode::Executable> m_bytecode_executable;
    i32 m_function_length { 0 };

    // Internal Slots of ECMAScript Function Objects, https://tc39.es/ecma262/#table-internal-slots-of-ecmascript-function-objects
    Environment* m_environment { nullptr };                           // [[Environment]]
    PrivateEnvironment* m_private_environment { nullptr };            // [[PrivateEnvironment]]
    Vector<FunctionNode::Parameter> const m_formal_parameters;        // [[FormalParameters]]
    NonnullRefPtr<Statement> m_ecmascript_code;                       // [[ECMAScriptCode]]
    Realm* m_realm { nullptr };                                       // [[Realm]]
    ScriptOrModule m_script_or_module;                                // [[ScriptOrModule]]
    Object* m_home_object { nullptr };                                // [[HomeObject]]
    String m_source_text;                                             // [[SourceText]]
    Vector<InstanceField> m_fields;                                   // [[Fields]]
    Vector<PrivateElement> m_private_methods;                         // [[PrivateMethods]]
    ConstructorKind m_constructor_kind : 1 { ConstructorKind::Base }; // [[ConstructorKind]]
    bool m_strict : 1 { false };                                      // [[Strict]]
    bool m_is_class_constructor : 1 { false };                        // [[IsClassConstructor]]
    ThisMode m_this_mode : 2 { ThisMode::Global };                    // [[ThisMode]]

    bool m_might_need_arguments_object : 1 { true };
    bool m_contains_direct_call_to_eval : 1 { true };
    bool m_is_arrow_function : 1 { false };
    bool m_has_simple_parameter_list : 1 { false };
    FunctionKind m_kind : 3 { FunctionKind::Normal };
};

template<>
inline bool Object::fast_is<ECMAScriptFunctionObject>() const { return is_ecmascript_function_object(); }

}
