/*
 * Copyright (c) 2020-2023, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2020-2023, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2021-2022, David Tuin <davidot@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Demangle.h>
#include <AK/HashMap.h>
#include <AK/HashTable.h>
#include <AK/QuickSort.h>
#include <AK/ScopeGuard.h>
#include <AK/StringBuilder.h>
#include <AK/TemporaryChange.h>
#include <LibCrypto/BigInt/SignedBigInteger.h>
#include <LibJS/AST.h>
#include <LibJS/Heap/MarkedVector.h>
#include <LibJS/Interpreter.h>
#include <LibJS/Runtime/AbstractOperations.h>
#include <LibJS/Runtime/Accessor.h>
#include <LibJS/Runtime/Array.h>
#include <LibJS/Runtime/BigInt.h>
#include <LibJS/Runtime/ECMAScriptFunctionObject.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/FunctionEnvironment.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Iterator.h>
#include <LibJS/Runtime/NativeFunction.h>
#include <LibJS/Runtime/ObjectEnvironment.h>
#include <LibJS/Runtime/PrimitiveString.h>
#include <LibJS/Runtime/PromiseCapability.h>
#include <LibJS/Runtime/PromiseConstructor.h>
#include <LibJS/Runtime/Reference.h>
#include <LibJS/Runtime/RegExpObject.h>
#include <LibJS/Runtime/Shape.h>
#include <typeinfo>

namespace JS {

class InterpreterNodeScope {
    AK_MAKE_NONCOPYABLE(InterpreterNodeScope);
    AK_MAKE_NONMOVABLE(InterpreterNodeScope);

public:
    InterpreterNodeScope(Interpreter& interpreter, ASTNode const& node)
        : m_interpreter(interpreter)
        , m_chain_node { nullptr, node }
    {
        m_interpreter.vm().running_execution_context().source_range = node.unrealized_source_range();
#pragma GCC diagnostic push
#pragma GCC diagnostic ignored "-Wdangling-pointer"
        // The node pointer is popped from the interpreter in the destructor.
        m_interpreter.push_ast_node(m_chain_node);
#pragma GCC diagnostic push
    }

    ~InterpreterNodeScope()
    {
        m_interpreter.pop_ast_node();
    }

private:
    Interpreter& m_interpreter;
    ExecutingASTNodeChain m_chain_node;
};

ASTNode::ASTNode(SourceRange source_range)
    : m_start_offset(source_range.start.offset)
    , m_source_code(source_range.code)
    , m_end_offset(source_range.end.offset)
{
}

SourceRange ASTNode::source_range() const
{
    return m_source_code->range_from_offsets(m_start_offset, m_end_offset);
}

DeprecatedString ASTNode::class_name() const
{
    // NOTE: We strip the "JS::" prefix.
    auto const* typename_ptr = typeid(*this).name();
    return demangle({ typename_ptr, strlen(typename_ptr) }).substring(4);
}

static void print_indent(int indent)
{
    out("{}", DeprecatedString::repeated(' ', indent * 2));
}

static void update_function_name(Value value, DeprecatedFlyString const& name)
{
    if (!value.is_function())
        return;
    auto& function = value.as_function();
    if (is<ECMAScriptFunctionObject>(function) && function.name().is_empty())
        static_cast<ECMAScriptFunctionObject&>(function).set_name(name);
}

static ThrowCompletionOr<DeprecatedString> get_function_property_name(PropertyKey key)
{
    if (key.is_symbol())
        return DeprecatedString::formatted("[{}]", key.as_symbol()->description().value_or(String {}));
    return key.to_string();
}

// 14.2.2 Runtime Semantics: Evaluation, https://tc39.es/ecma262/#sec-block-runtime-semantics-evaluation
// StatementList : StatementList StatementListItem
Completion ScopeNode::evaluate_statements(Interpreter& interpreter) const
{
    auto completion = normal_completion({});
    for (auto const& node : children()) {
        completion = node->execute(interpreter).update_empty(completion.value());
        if (completion.is_abrupt())
            break;
    }
    return completion;
}

// 14.13.4 Runtime Semantics: LabelledEvaluation, https://tc39.es/ecma262/#sec-runtime-semantics-labelledevaluation
// BreakableStatement : IterationStatement
static Completion labelled_evaluation(Interpreter& interpreter, IterationStatement const& statement, Vector<DeprecatedFlyString> const& label_set)
{
    // 1. Let stmtResult be Completion(LoopEvaluation of IterationStatement with argument labelSet).
    auto result = statement.loop_evaluation(interpreter, label_set);

    // 2. If stmtResult.[[Type]] is break, then
    if (result.type() == Completion::Type::Break) {
        // a. If stmtResult.[[Target]] is empty, then
        if (!result.target().has_value()) {
            // i. If stmtResult.[[Value]] is empty, set stmtResult to NormalCompletion(undefined).
            // ii. Else, set stmtResult to NormalCompletion(stmtResult.[[Value]]).
            result = normal_completion(result.value().value_or(js_undefined()));
        }
    }

    // 3. Return ? stmtResult.
    return result;
}

// 14.13.4 Runtime Semantics: LabelledEvaluation, https://tc39.es/ecma262/#sec-runtime-semantics-labelledevaluation
// BreakableStatement : SwitchStatement
static Completion labelled_evaluation(Interpreter& interpreter, SwitchStatement const& statement, Vector<DeprecatedFlyString> const&)
{
    // 1. Let stmtResult be the result of evaluating SwitchStatement.
    auto result = statement.execute_impl(interpreter);

    // 2. If stmtResult.[[Type]] is break, then
    if (result.type() == Completion::Type::Break) {
        // a. If stmtResult.[[Target]] is empty, then
        if (!result.target().has_value()) {
            // i. If stmtResult.[[Value]] is empty, set stmtResult to NormalCompletion(undefined).
            // ii. Else, set stmtResult to NormalCompletion(stmtResult.[[Value]]).
            result = normal_completion(result.value().value_or(js_undefined()));
        }
    }

    // 3. Return ? stmtResult.
    return result;
}

// 14.13.4 Runtime Semantics: LabelledEvaluation, https://tc39.es/ecma262/#sec-runtime-semantics-labelledevaluation
// LabelledStatement : LabelIdentifier : LabelledItem
static Completion labelled_evaluation(Interpreter& interpreter, LabelledStatement const& statement, Vector<DeprecatedFlyString> const& label_set)
{
    auto const& labelled_item = *statement.labelled_item();

    // 1. Let label be the StringValue of LabelIdentifier.
    auto const& label = statement.label();

    // 2. Let newLabelSet be the list-concatenation of labelSet and « label ».
    // Optimization: Avoid vector copy if possible.
    Optional<Vector<DeprecatedFlyString>> new_label_set;
    if (is<IterationStatement>(labelled_item) || is<SwitchStatement>(labelled_item) || is<LabelledStatement>(labelled_item)) {
        new_label_set = label_set;
        new_label_set->append(label);
    }

    // 3. Let stmtResult be Completion(LabelledEvaluation of LabelledItem with argument newLabelSet).
    Completion result;
    if (is<IterationStatement>(labelled_item))
        result = labelled_evaluation(interpreter, static_cast<IterationStatement const&>(labelled_item), *new_label_set);
    else if (is<SwitchStatement>(labelled_item))
        result = labelled_evaluation(interpreter, static_cast<SwitchStatement const&>(labelled_item), *new_label_set);
    else if (is<LabelledStatement>(labelled_item))
        result = labelled_evaluation(interpreter, static_cast<LabelledStatement const&>(labelled_item), *new_label_set);
    else
        result = labelled_item.execute(interpreter);

    // 4. If stmtResult.[[Type]] is break and SameValue(stmtResult.[[Target]], label) is true, then
    if (result.type() == Completion::Type::Break && result.target() == label) {
        // a. Set stmtResult to NormalCompletion(stmtResult.[[Value]]).
        result = normal_completion(result.value());
    }

    // 5. Return ? stmtResult.
    return result;
}

// 14.13.3 Runtime Semantics: Evaluation, https://tc39.es/ecma262/#sec-labelled-statements-runtime-semantics-evaluation
Completion LabelledStatement::execute(Interpreter& interpreter) const
{
    InterpreterNodeScope node_scope { interpreter, *this };

    // 1. Return ? LabelledEvaluation of this LabelledStatement with argument « ».
    return labelled_evaluation(interpreter, *this, {});
}

void LabelledStatement::dump(int indent) const
{
    ASTNode::dump(indent);

    print_indent(indent + 1);
    outln("(Label)");
    print_indent(indent + 2);
    outln("\"{}\"", m_label);

    print_indent(indent + 1);
    outln("(Labelled item)");
    m_labelled_item->dump(indent + 2);
}

// 10.2.1.3 Runtime Semantics: EvaluateBody, https://tc39.es/ecma262/#sec-runtime-semantics-evaluatebody
Completion FunctionBody::execute(Interpreter& interpreter) const
{
    InterpreterNodeScope node_scope { interpreter, *this };

    // Note: Scoping should have already been set up by whoever is calling this FunctionBody.
    // 1. Return ? EvaluateFunctionBody of FunctionBody with arguments functionObject and argumentsList.
    return evaluate_statements(interpreter);
}

// 14.2.2 Runtime Semantics: Evaluation, https://tc39.es/ecma262/#sec-block-runtime-semantics-evaluation
Completion BlockStatement::execute(Interpreter& interpreter) const
{
    InterpreterNodeScope node_scope { interpreter, *this };

    auto& vm = interpreter.vm();

    Environment* old_environment { nullptr };

    // Optimization: We only need a new lexical environment if there are any lexical declarations. :^)
    if (!has_lexical_declarations())
        return evaluate_statements(interpreter);

    old_environment = vm.running_execution_context().lexical_environment;
    auto block_environment = new_declarative_environment(*old_environment);
    block_declaration_instantiation(vm, block_environment);
    vm.running_execution_context().lexical_environment = block_environment;

    // 5. Let blockValue be the result of evaluating StatementList.
    auto block_value = evaluate_statements(interpreter);

    // 6. Set blockValue to DisposeResources(blockEnv, blockValue).
    block_value = dispose_resources(vm, block_environment, block_value);

    vm.running_execution_context().lexical_environment = old_environment;

    return block_value;
}

Completion Program::execute(Interpreter& interpreter) const
{
    InterpreterNodeScope node_scope { interpreter, *this };

    return evaluate_statements(interpreter);
}

// 15.2.6 Runtime Semantics: Evaluation, https://tc39.es/ecma262/#sec-function-definitions-runtime-semantics-evaluation
Completion FunctionDeclaration::execute(Interpreter& interpreter) const
{
    InterpreterNodeScope node_scope { interpreter, *this };
    auto& vm = interpreter.vm();

    if (m_is_hoisted) {
        // Perform special annexB steps see step 3 of: https://tc39.es/ecma262/#sec-web-compat-functiondeclarationinstantiation

        // i. Let genv be the running execution context's VariableEnvironment.
        auto variable_environment = interpreter.vm().running_execution_context().variable_environment;

        // ii. Let benv be the running execution context's LexicalEnvironment.
        auto lexical_environment = interpreter.vm().running_execution_context().lexical_environment;

        // iii. Let fobj be ! benv.GetBindingValue(F, false).
        auto function_object = MUST(lexical_environment->get_binding_value(vm, name(), false));

        // iv. Perform ? genv.SetMutableBinding(F, fobj, false).
        TRY(variable_environment->set_mutable_binding(vm, name(), function_object, false));

        // v. Return unused.
        return Optional<Value> {};
    }

    // 1. Return unused.
    return Optional<Value> {};
}

// 15.2.6 Runtime Semantics: Evaluation, https://tc39.es/ecma262/#sec-function-definitions-runtime-semantics-evaluation
Completion FunctionExpression::execute(Interpreter& interpreter) const
{
    InterpreterNodeScope node_scope { interpreter, *this };

    // 1. Return InstantiateOrdinaryFunctionExpression of FunctionExpression.
    return instantiate_ordinary_function_expression(interpreter.vm(), name());
}

// 15.2.5 Runtime Semantics: InstantiateOrdinaryFunctionExpression, https://tc39.es/ecma262/#sec-runtime-semantics-instantiateordinaryfunctionexpression
Value FunctionExpression::instantiate_ordinary_function_expression(VM& vm, DeprecatedFlyString given_name) const
{
    auto& realm = *vm.current_realm();

    if (given_name.is_empty())
        given_name = "";
    auto has_own_name = !name().is_empty();

    auto const used_name = has_own_name ? name() : given_name.view();
    auto environment = NonnullGCPtr { *vm.running_execution_context().lexical_environment };
    if (has_own_name) {
        VERIFY(environment);
        environment = new_declarative_environment(*environment);
        MUST(environment->create_immutable_binding(vm, name(), false));
    }

    auto private_environment = vm.running_execution_context().private_environment;

    auto closure = ECMAScriptFunctionObject::create(realm, used_name, source_text(), body(), parameters(), function_length(), local_variables_names(), environment, private_environment, kind(), is_strict_mode(), might_need_arguments_object(), contains_direct_call_to_eval(), is_arrow_function());

    // FIXME: 6. Perform SetFunctionName(closure, name).
    // FIXME: 7. Perform MakeConstructor(closure).

    if (has_own_name)
        MUST(environment->initialize_binding(vm, name(), closure, Environment::InitializeBindingHint::Normal));

    return closure;
}

// 14.4.1 Runtime Semantics: Evaluation, https://tc39.es/ecma262/#sec-empty-statement-runtime-semantics-evaluation
Completion EmptyStatement::execute(Interpreter&) const
{
    // 1. Return empty.
    return Optional<Value> {};
}

// 14.5.1 Runtime Semantics: Evaluation, https://tc39.es/ecma262/#sec-expression-statement-runtime-semantics-evaluation
Completion ExpressionStatement::execute(Interpreter& interpreter) const
{
    InterpreterNodeScope node_scope { interpreter, *this };

    // 1. Let exprRef be the result of evaluating Expression.
    // 2. Return ? GetValue(exprRef).
    return m_expression->execute(interpreter);
}

// TODO: This shouldn't exist. Refactor into EvaluateCall.
ThrowCompletionOr<CallExpression::ThisAndCallee> CallExpression::compute_this_and_callee(Interpreter& interpreter, Reference const& callee_reference) const
{
    auto& vm = interpreter.vm();
    if (callee_reference.is_property_reference()) {
        auto this_value = callee_reference.get_this_value();
        auto callee = TRY(callee_reference.get_value(vm));

        return ThisAndCallee { this_value, callee };
    }

    Value this_value = js_undefined();
    if (callee_reference.is_environment_reference()) {
        if (Object* base_object = callee_reference.base_environment().with_base_object(); base_object != nullptr)
            this_value = base_object;
    }

    // [[Call]] will handle that in non-strict mode the this value becomes the global object
    return ThisAndCallee {
        this_value,
        callee_reference.is_unresolvable()
            ? TRY(m_callee->execute(interpreter)).release_value()
            : TRY(callee_reference.get_value(vm))
    };
}

// 13.3.8.1 Runtime Semantics: ArgumentListEvaluation, https://tc39.es/ecma262/#sec-runtime-semantics-argumentlistevaluation
static ThrowCompletionOr<void> argument_list_evaluation(Interpreter& interpreter, ReadonlySpan<CallExpression::Argument> const arguments, MarkedVector<Value>& list)
{
    auto& vm = interpreter.vm();
    list.ensure_capacity(arguments.size());

    for (auto& argument : arguments) {
        auto value = TRY(argument.value->execute(interpreter)).release_value();
        if (argument.is_spread) {
            TRY(get_iterator_values(vm, value, [&](Value iterator_value) -> Optional<Completion> {
                list.append(iterator_value);
                return {};
            }));
        } else {
            list.append(value);
        }
    }
    return {};
}

// 13.3.5.1 Runtime Semantics: Evaluation, https://tc39.es/ecma262/#sec-new-operator-runtime-semantics-evaluation
// 13.3.5.1.1 EvaluateNew ( constructExpr, arguments ), https://tc39.es/ecma262/#sec-evaluatenew
Completion NewExpression::execute(Interpreter& interpreter) const
{
    InterpreterNodeScope node_scope { interpreter, *this };
    auto& vm = interpreter.vm();

    // 1. Let ref be the result of evaluating constructExpr.
    // 2. Let constructor be ? GetValue(ref).
    auto constructor = TRY(m_callee->execute(interpreter)).release_value();

    // 3. If arguments is empty, let argList be a new empty List.
    // 4. Else,
    //    a. Let argList be ? ArgumentListEvaluation of arguments.
    MarkedVector<Value> arg_list(vm.heap());
    TRY(argument_list_evaluation(interpreter, arguments(), arg_list));

    // 5. If IsConstructor(constructor) is false, throw a TypeError exception.
    if (!constructor.is_constructor())
        return throw_type_error_for_callee(interpreter, constructor, "constructor"sv);

    // 6. Return ? Construct(constructor, argList).
    return Value { TRY(construct(vm, constructor.as_function(), move(arg_list))) };
}

Optional<DeprecatedString> CallExpression::expression_string() const
{
    if (is<Identifier>(*m_callee))
        return static_cast<Identifier const&>(*m_callee).string();

    if (is<MemberExpression>(*m_callee))
        return static_cast<MemberExpression const&>(*m_callee).to_string_approximation();

    return {};
}

Completion CallExpression::throw_type_error_for_callee(Interpreter& interpreter, Value callee_value, StringView call_type) const
{
    auto& vm = interpreter.vm();

    if (auto expression_string = this->expression_string(); expression_string.has_value())
        return vm.throw_completion<TypeError>(ErrorType::IsNotAEvaluatedFrom, TRY_OR_THROW_OOM(vm, callee_value.to_string_without_side_effects()), call_type, expression_string.release_value());

    return vm.throw_completion<TypeError>(ErrorType::IsNotA, TRY_OR_THROW_OOM(vm, callee_value.to_string_without_side_effects()), call_type);
}

// 13.3.6.1 Runtime Semantics: Evaluation, https://tc39.es/ecma262/#sec-function-calls-runtime-semantics-evaluation
Completion CallExpression::execute(Interpreter& interpreter) const
{
    InterpreterNodeScope node_scope { interpreter, *this };
    auto& vm = interpreter.vm();
    auto& realm = *vm.current_realm();

    auto callee_reference = TRY(m_callee->to_reference(interpreter));

    auto [this_value, callee] = TRY(compute_this_and_callee(interpreter, callee_reference));

    VERIFY(!callee.is_empty());

    MarkedVector<Value> arg_list(vm.heap());
    TRY(argument_list_evaluation(interpreter, arguments(), arg_list));

    if (!callee.is_function())
        return throw_type_error_for_callee(interpreter, callee, "function"sv);

    auto& function = callee.as_function();

    if (&function == realm.intrinsics().eval_function()
        && callee_reference.is_environment_reference()
        && callee_reference.name().is_string()
        && callee_reference.name().as_string() == vm.names.eval.as_string()) {

        auto script_value = arg_list.size() == 0 ? js_undefined() : arg_list[0];
        return perform_eval(vm, script_value, vm.in_strict_mode() ? CallerMode::Strict : CallerMode::NonStrict, EvalMode::Direct);
    }

    return call(vm, function, this_value, move(arg_list));
}

// 13.3.7.1 Runtime Semantics: Evaluation, https://tc39.es/ecma262/#sec-super-keyword-runtime-semantics-evaluation
// SuperCall : super Arguments
Completion SuperCall::execute(Interpreter& interpreter) const
{
    InterpreterNodeScope node_scope { interpreter, *this };
    auto& vm = interpreter.vm();

    // 1. Let newTarget be GetNewTarget().
    auto new_target = vm.get_new_target();

    // 2. Assert: Type(newTarget) is Object.
    VERIFY(new_target.is_function());

    // 3. Let func be GetSuperConstructor().
    auto* func = get_super_constructor(interpreter.vm());

    // 4. Let argList be ? ArgumentListEvaluation of Arguments.
    MarkedVector<Value> arg_list(vm.heap());
    if (m_is_synthetic == IsPartOfSyntheticConstructor::Yes) {
        // NOTE: This is the case where we have a fake constructor(...args) { super(...args); } which
        //       shouldn't call @@iterator of %Array.prototype%.
        VERIFY(m_arguments.size() == 1);
        VERIFY(m_arguments[0].is_spread);
        auto const& argument = m_arguments[0];
        auto value = MUST(argument.value->execute(interpreter)).release_value();
        VERIFY(value.is_object() && is<Array>(value.as_object()));

        auto& array_value = static_cast<Array const&>(value.as_object());
        auto length = MUST(length_of_array_like(vm, array_value));
        for (size_t i = 0; i < length; ++i)
            arg_list.append(array_value.get_without_side_effects(PropertyKey { i }));
    } else {
        TRY(argument_list_evaluation(interpreter, m_arguments, arg_list));
    }

    // 5. If IsConstructor(func) is false, throw a TypeError exception.
    if (!func || !Value(func).is_constructor())
        return vm.throw_completion<TypeError>(ErrorType::NotAConstructor, "Super constructor");

    // 6. Let result be ? Construct(func, argList, newTarget).
    auto result = TRY(construct(vm, static_cast<FunctionObject&>(*func), move(arg_list), &new_target.as_function()));

    // 7. Let thisER be GetThisEnvironment().
    auto& this_er = verify_cast<FunctionEnvironment>(*get_this_environment(vm));

    // 8. Perform ? thisER.BindThisValue(result).
    TRY(this_er.bind_this_value(vm, result));

    // 9. Let F be thisER.[[FunctionObject]].
    // 10. Assert: F is an ECMAScript function object.
    // NOTE: This is implied by the strong C++ type.
    [[maybe_unused]] auto& f = this_er.function_object();

    // 11. Perform ? InitializeInstanceElements(result, F).
    TRY(result->initialize_instance_elements(f));

    // 12. Return result.
    return Value { result };
}

// 15.5.5 Runtime Semantics: Evaluation, https://tc39.es/ecma262/#sec-generator-function-definitions-runtime-semantics-evaluation
Completion YieldExpression::execute(Interpreter&) const
{
    // This should be transformed to a return.
    VERIFY_NOT_REACHED();
}

// 15.8.5 Runtime Semantics: Evaluation, https://tc39.es/ecma262/#sec-async-function-definitions-runtime-semantics-evaluation
Completion AwaitExpression::execute(Interpreter& interpreter) const
{
    InterpreterNodeScope node_scope { interpreter, *this };
    auto& vm = interpreter.vm();

    // 1. Let exprRef be the result of evaluating UnaryExpression.
    // 2. Let value be ? GetValue(exprRef).
    auto value = TRY(m_argument->execute(interpreter)).release_value();

    // 3. Return ? Await(value).
    return await(vm, value);
}

// 14.10.1 Runtime Semantics: Evaluation, https://tc39.es/ecma262/#sec-return-statement-runtime-semantics-evaluation
Completion ReturnStatement::execute(Interpreter& interpreter) const
{
    InterpreterNodeScope node_scope { interpreter, *this };

    // ReturnStatement : return ;
    if (!m_argument) {
        // 1. Return Completion Record { [[Type]]: return, [[Value]]: undefined, [[Target]]: empty }.
        return { Completion::Type::Return, js_undefined(), {} };
    }

    // ReturnStatement : return Expression ;
    // 1. Let exprRef be the result of evaluating Expression.
    // 2. Let exprValue be ? GetValue(exprRef).
    auto value = TRY(m_argument->execute(interpreter));

    // NOTE: Generators are not supported in the AST interpreter
    // 3. If GetGeneratorKind() is async, set exprValue to ? Await(exprValue).

    // 4. Return Completion Record { [[Type]]: return, [[Value]]: exprValue, [[Target]]: empty }.
    return { Completion::Type::Return, value, {} };
}

// 14.6.2 Runtime Semantics: Evaluation, https://tc39.es/ecma262/#sec-if-statement-runtime-semantics-evaluation
Completion IfStatement::execute(Interpreter& interpreter) const
{
    InterpreterNodeScope node_scope { interpreter, *this };

    // IfStatement : if ( Expression ) Statement else Statement
    // 1. Let exprRef be the result of evaluating Expression.
    // 2. Let exprValue be ToBoolean(? GetValue(exprRef)).
    auto predicate_result = TRY(m_predicate->execute(interpreter)).release_value();

    // 3. If exprValue is true, then
    if (predicate_result.to_boolean()) {
        // a. Let stmtCompletion be the result of evaluating the first Statement.
        // 5. Return ? UpdateEmpty(stmtCompletion, undefined).
        return m_consequent->execute(interpreter).update_empty(js_undefined());
    }

    // 4. Else,
    if (m_alternate) {
        // a. Let stmtCompletion be the result of evaluating the second Statement.
        // 5. Return ? UpdateEmpty(stmtCompletion, undefined).
        return m_alternate->execute(interpreter).update_empty(js_undefined());
    }

    // IfStatement : if ( Expression ) Statement
    // 3. If exprValue is false, then
    //    a. Return undefined.
    return js_undefined();
}

// 14.11.2 Runtime Semantics: Evaluation, https://tc39.es/ecma262/#sec-with-statement-runtime-semantics-evaluation
// WithStatement : with ( Expression ) Statement
Completion WithStatement::execute(Interpreter& interpreter) const
{
    InterpreterNodeScope node_scope { interpreter, *this };
    auto& vm = interpreter.vm();

    // 1. Let value be the result of evaluating Expression.
    auto value = TRY(m_object->execute(interpreter)).release_value();

    // 2. Let obj be ? ToObject(? GetValue(value)).
    auto object = TRY(value.to_object(vm));

    // 3. Let oldEnv be the running execution context's LexicalEnvironment.
    auto old_environment = vm.running_execution_context().lexical_environment;

    // 4. Let newEnv be NewObjectEnvironment(obj, true, oldEnv).
    auto new_environment = new_object_environment(object, true, old_environment);

    // 5. Set the running execution context's LexicalEnvironment to newEnv.
    vm.running_execution_context().lexical_environment = new_environment;

    // 6. Let C be the result of evaluating Statement.
    auto result = m_body->execute(interpreter);

    // 7. Set the running execution context's LexicalEnvironment to oldEnv.
    vm.running_execution_context().lexical_environment = old_environment;

    // 8. Return ? UpdateEmpty(C, undefined).
    return result.update_empty(js_undefined());
}

// 14.7.1.1 LoopContinues ( completion, labelSet ), https://tc39.es/ecma262/#sec-loopcontinues
static bool loop_continues(Completion const& completion, Vector<DeprecatedFlyString> const& label_set)
{
    // 1. If completion.[[Type]] is normal, return true.
    if (completion.type() == Completion::Type::Normal)
        return true;

    // 2. If completion.[[Type]] is not continue, return false.
    if (completion.type() != Completion::Type::Continue)
        return false;

    // 3. If completion.[[Target]] is empty, return true.
    if (!completion.target().has_value())
        return true;

    // 4. If completion.[[Target]] is an element of labelSet, return true.
    if (label_set.contains_slow(*completion.target()))
        return true;

    // 5. Return false.
    return false;
}

// 14.1.1 Runtime Semantics: Evaluation, https://tc39.es/ecma262/#sec-statement-semantics-runtime-semantics-evaluation
// BreakableStatement : IterationStatement
Completion WhileStatement::execute(Interpreter& interpreter) const
{
    // 1. Let newLabelSet be a new empty List.
    // 2. Return ? LabelledEvaluation of this BreakableStatement with argument newLabelSet.
    return labelled_evaluation(interpreter, *this, {});
}

// 14.7.3.2 Runtime Semantics: WhileLoopEvaluation, https://tc39.es/ecma262/#sec-runtime-semantics-whileloopevaluation
Completion WhileStatement::loop_evaluation(Interpreter& interpreter, Vector<DeprecatedFlyString> const& label_set) const
{
    InterpreterNodeScope node_scope { interpreter, *this };

    // 1. Let V be undefined.
    auto last_value = js_undefined();

    // 2. Repeat,
    for (;;) {
        // a. Let exprRef be the result of evaluating Expression.
        // b. Let exprValue be ? GetValue(exprRef).
        auto test_result = TRY(m_test->execute(interpreter)).release_value();

        // c. If ToBoolean(exprValue) is false, return V.
        if (!test_result.to_boolean())
            return last_value;

        // d. Let stmtResult be the result of evaluating Statement.
        auto body_result = m_body->execute(interpreter);

        // e. If LoopContinues(stmtResult, labelSet) is false, return ? UpdateEmpty(stmtResult, V).
        if (!loop_continues(body_result, label_set))
            return body_result.update_empty(last_value);

        // f. If stmtResult.[[Value]] is not empty, set V to stmtResult.[[Value]].
        if (body_result.value().has_value())
            last_value = *body_result.value();
    }

    VERIFY_NOT_REACHED();
}

// 14.1.1 Runtime Semantics: Evaluation, https://tc39.es/ecma262/#sec-statement-semantics-runtime-semantics-evaluation
// BreakableStatement : IterationStatement
Completion DoWhileStatement::execute(Interpreter& interpreter) const
{
    // 1. Let newLabelSet be a new empty List.
    // 2. Return ? LabelledEvaluation of this BreakableStatement with argument newLabelSet.
    return labelled_evaluation(interpreter, *this, {});
}

// 14.7.2.2 Runtime Semantics: DoWhileLoopEvaluation, https://tc39.es/ecma262/#sec-runtime-semantics-dowhileloopevaluation
Completion DoWhileStatement::loop_evaluation(Interpreter& interpreter, Vector<DeprecatedFlyString> const& label_set) const
{
    InterpreterNodeScope node_scope { interpreter, *this };

    // 1. Let V be undefined.
    auto last_value = js_undefined();

    // 2. Repeat,
    for (;;) {
        // a. Let stmtResult be the result of evaluating Statement.
        auto body_result = m_body->execute(interpreter);

        // b. If LoopContinues(stmtResult, labelSet) is false, return ? UpdateEmpty(stmtResult, V).
        if (!loop_continues(body_result, label_set))
            return body_result.update_empty(last_value);

        // c. If stmtResult.[[Value]] is not empty, set V to stmtResult.[[Value]].
        if (body_result.value().has_value())
            last_value = *body_result.value();

        // d. Let exprRef be the result of evaluating Expression.
        // e. Let exprValue be ? GetValue(exprRef).
        auto test_result = TRY(m_test->execute(interpreter)).release_value();

        // f. If ToBoolean(exprValue) is false, return V.
        if (!test_result.to_boolean())
            return last_value;
    }

    VERIFY_NOT_REACHED();
}

// 14.1.1 Runtime Semantics: Evaluation, https://tc39.es/ecma262/#sec-statement-semantics-runtime-semantics-evaluation
// BreakableStatement : IterationStatement
Completion ForStatement::execute(Interpreter& interpreter) const
{
    // 1. Let newLabelSet be a new empty List.
    // 2. Return ? LabelledEvaluation of this BreakableStatement with argument newLabelSet.
    return labelled_evaluation(interpreter, *this, {});
}

// 14.7.4.2 Runtime Semantics: ForLoopEvaluation, https://tc39.es/ecma262/#sec-runtime-semantics-forloopevaluation
Completion ForStatement::loop_evaluation(Interpreter& interpreter, Vector<DeprecatedFlyString> const& label_set) const
{
    InterpreterNodeScope node_scope { interpreter, *this };
    auto& vm = interpreter.vm();

    // Note we don't always set a new environment but to use RAII we must do this here.
    auto* old_environment = interpreter.lexical_environment();

    size_t per_iteration_bindings_size = 0;
    GCPtr<DeclarativeEnvironment> loop_env;

    if (m_init) {
        Declaration const* declaration = nullptr;

        if (is<VariableDeclaration>(*m_init) && static_cast<VariableDeclaration const&>(*m_init).declaration_kind() != DeclarationKind::Var)
            declaration = static_cast<VariableDeclaration const*>(m_init.ptr());
        else if (is<UsingDeclaration>(*m_init))
            declaration = static_cast<UsingDeclaration const*>(m_init.ptr());

        if (declaration) {
            loop_env = new_declarative_environment(*old_environment);
            auto is_const = declaration->is_constant_declaration();
            // NOTE: Due to the use of MUST with `create_immutable_binding` and `create_mutable_binding` below,
            //       an exception should not result from `for_each_bound_identifier`.
            MUST(declaration->for_each_bound_identifier([&](auto const& identifier) {
                auto const& name = identifier.string();
                if (is_const) {
                    MUST(loop_env->create_immutable_binding(vm, name, true));
                } else {
                    MUST(loop_env->create_mutable_binding(vm, name, false));
                    ++per_iteration_bindings_size;
                }
            }));

            interpreter.vm().running_execution_context().lexical_environment = loop_env;
        }

        (void)TRY(m_init->execute(interpreter));
    }

    // 10. Let bodyResult be Completion(ForBodyEvaluation(the first Expression, the second Expression, Statement, perIterationLets, labelSet)).
    auto body_result = for_body_evaluation(interpreter, label_set, per_iteration_bindings_size);

    // 11. Set bodyResult to DisposeResources(loopEnv, bodyResult).
    if (loop_env)
        body_result = dispose_resources(vm, loop_env.ptr(), body_result);

    // 12. Set the running execution context's LexicalEnvironment to oldEnv.
    interpreter.vm().running_execution_context().lexical_environment = old_environment;

    // 13. Return ? bodyResult.
    return body_result;
}

// 14.7.4.3 ForBodyEvaluation ( test, increment, stmt, perIterationBindings, labelSet ), https://tc39.es/ecma262/#sec-forbodyevaluation
// 6.3.1.2 ForBodyEvaluation ( test, increment, stmt, perIterationBindings, labelSet ), https://tc39.es/proposal-explicit-resource-management/#sec-forbodyevaluation
Completion ForStatement::for_body_evaluation(JS::Interpreter& interpreter, Vector<DeprecatedFlyString> const& label_set, size_t per_iteration_bindings_size) const
{
    auto& vm = interpreter.vm();

    // 14.7.4.4 CreatePerIterationEnvironment ( perIterationBindings ), https://tc39.es/ecma262/#sec-createperiterationenvironment
    // NOTE: Our implementation of this AO is heavily dependent on DeclarativeEnvironment using a Vector with constant indices.
    //       For performance, we can take advantage of the fact that the declarations of the initialization statement are created
    //       in the same order each time CreatePerIterationEnvironment is invoked.
    auto create_per_iteration_environment = [&]() -> GCPtr<DeclarativeEnvironment> {
        // 1. If perIterationBindings has any elements, then
        if (per_iteration_bindings_size == 0) {
            // 2. Return unused.
            return nullptr;
        }

        // a. Let lastIterationEnv be the running execution context's LexicalEnvironment.
        auto* last_iteration_env = verify_cast<DeclarativeEnvironment>(interpreter.lexical_environment());

        // b. Let outer be lastIterationEnv.[[OuterEnv]].
        // c. Assert: outer is not null.
        VERIFY(last_iteration_env->outer_environment());

        // d. Let thisIterationEnv be NewDeclarativeEnvironment(outer).
        auto this_iteration_env = DeclarativeEnvironment::create_for_per_iteration_bindings({}, *last_iteration_env, per_iteration_bindings_size);

        // e. For each element bn of perIterationBindings, do
        //     i. Perform ! thisIterationEnv.CreateMutableBinding(bn, false).
        //     ii. Let lastValue be ? lastIterationEnv.GetBindingValue(bn, true).
        //     iii. Perform ! thisIterationEnv.InitializeBinding(bn, lastValue).
        //
        // NOTE: This is handled by DeclarativeEnvironment::create_for_per_iteration_bindings. Step e.ii indicates it may throw,
        //       but that is not possible. The potential for throwing was added to accommodate support for do-expressions in the
        //       initialization statement, but that idea was dropped: https://github.com/tc39/ecma262/issues/299#issuecomment-172950045

        // f. Set the running execution context's LexicalEnvironment to thisIterationEnv.
        interpreter.vm().running_execution_context().lexical_environment = this_iteration_env;

        // g. Return thisIterationEnv.
        return this_iteration_env;
    };

    // 1. Let V be undefined.
    auto last_value = js_undefined();

    // 2. Let thisIterationEnv be ? CreatePerIterationEnvironment(perIterationBindings).
    auto this_iteration_env = create_per_iteration_environment();

    // 3. Repeat,
    while (true) {
        // a. If test is not [empty], then
        if (m_test) {
            // i. Let testRef be the result of evaluating test.
            // ii. Let testValue be Completion(GetValue(testRef)).
            auto test_value = m_test->execute(interpreter);

            // iii. If testValue is an abrupt completion, then
            if (test_value.is_abrupt()) {
                // 1. Return ? DisposeResources(thisIterationEnv, testValue).
                return TRY(dispose_resources(vm, this_iteration_env, test_value));
            }
            // iv. Else,
            // 1. Set testValue to testValue.[[Value]].
            VERIFY(test_value.value().has_value());

            // iii. If ToBoolean(testValue) is false, return ? DisposeResources(thisIterationEnv, Completion(V)).
            if (!test_value.release_value().value().to_boolean())
                return TRY(dispose_resources(vm, this_iteration_env, test_value));
        }

        // b. Let result be the result of evaluating stmt.
        auto result = m_body->execute(interpreter);

        // c. Perform ? DisposeResources(thisIterationEnv, result).
        TRY(dispose_resources(vm, this_iteration_env, result));

        // d. If LoopContinues(result, labelSet) is false, return ? UpdateEmpty(result, V).
        if (!loop_continues(result, label_set))
            return result.update_empty(last_value);

        // e. If result.[[Value]] is not empty, set V to result.[[Value]].
        if (result.value().has_value())
            last_value = *result.value();

        // f. Set thisIterationEnv to ? CreatePerIterationEnvironment(perIterationBindings).
        this_iteration_env = create_per_iteration_environment();

        // g. If increment is not [empty], then
        if (m_update) {
            // i. Let incRef be the result of evaluating increment.
            // ii. Let incrResult be Completion(GetValue(incrRef)).
            auto inc_ref = m_update->execute(interpreter);

            // ii. If incrResult is an abrupt completion, then
            if (inc_ref.is_abrupt()) {
                // 1. Return ? DisposeResources(thisIterationEnv, incrResult).
                return TRY(dispose_resources(vm, this_iteration_env, inc_ref));
            }
        }
    }

    VERIFY_NOT_REACHED();
}

struct ForInOfHeadState {
    explicit ForInOfHeadState(Variant<NonnullRefPtr<ASTNode const>, NonnullRefPtr<BindingPattern const>> lhs)
    {
        lhs.visit(
            [&](NonnullRefPtr<ASTNode const>& ast_node) {
                expression_lhs = ast_node.ptr();
            },
            [&](NonnullRefPtr<BindingPattern const>& pattern) {
                pattern_lhs = pattern.ptr();
                destructuring = true;
                lhs_kind = Assignment;
            });
    }

    ASTNode const* expression_lhs = nullptr;
    BindingPattern const* pattern_lhs = nullptr;
    enum LhsKind {
        Assignment,
        VarBinding,
        LexicalBinding
    };
    LhsKind lhs_kind = Assignment;
    bool destructuring = false;

    Value rhs_value;

    // 14.7.5.7 ForIn/OfBodyEvaluation ( lhs, stmt, iteratorRecord, iterationKind, lhsKind, labelSet [ , iteratorKind ] ), https://tc39.es/ecma262/#sec-runtime-semantics-forin-div-ofbodyevaluation-lhs-stmt-iterator-lhskind-labelset
    // Note: This is only steps 6.g through 6.j of the method because we currently implement for-in without an iterator so to prevent duplicated code we do this part here.
    ThrowCompletionOr<void> execute_head(Interpreter& interpreter, Value next_value) const
    {
        VERIFY(!next_value.is_empty());

        auto& vm = interpreter.vm();

        Optional<Reference> lhs_reference;
        GCPtr<Environment> iteration_environment;

        // g. If lhsKind is either assignment or varBinding, then
        if (lhs_kind == Assignment || lhs_kind == VarBinding) {
            if (!destructuring) {
                VERIFY(expression_lhs);
                if (is<VariableDeclaration>(*expression_lhs)) {
                    auto& declaration = static_cast<VariableDeclaration const&>(*expression_lhs);
                    VERIFY(declaration.declarations().first()->target().has<NonnullRefPtr<Identifier const>>());
                    lhs_reference = TRY(declaration.declarations().first()->target().get<NonnullRefPtr<Identifier const>>()->to_reference(interpreter));
                } else if (is<UsingDeclaration>(*expression_lhs)) {
                    auto& declaration = static_cast<UsingDeclaration const&>(*expression_lhs);
                    VERIFY(declaration.declarations().first()->target().has<NonnullRefPtr<Identifier const>>());
                    lhs_reference = TRY(declaration.declarations().first()->target().get<NonnullRefPtr<Identifier const>>()->to_reference(interpreter));
                } else {
                    VERIFY(is<Identifier>(*expression_lhs) || is<MemberExpression>(*expression_lhs) || is<CallExpression>(*expression_lhs));
                    auto& expression = static_cast<Expression const&>(*expression_lhs);
                    lhs_reference = TRY(expression.to_reference(interpreter));
                }
            }
        }
        // h. Else,
        else {
            VERIFY(expression_lhs && (is<VariableDeclaration>(*expression_lhs) || is<UsingDeclaration>(*expression_lhs)));
            iteration_environment = new_declarative_environment(*interpreter.lexical_environment());

            auto& for_declaration = static_cast<Declaration const&>(*expression_lhs);
            DeprecatedFlyString first_name;

            // 14.7.5.4 Runtime Semantics: ForDeclarationBindingInstantiation, https://tc39.es/ecma262/#sec-runtime-semantics-fordeclarationbindinginstantiation
            // 1. For each element name of the BoundNames of ForBinding, do
            // NOTE: Due to the use of MUST with `create_immutable_binding` and `create_mutable_binding` below,
            //       an exception should not result from `for_each_bound_identifier`.
            MUST(for_declaration.for_each_bound_identifier([&](auto const& identifier) {
                auto const& name = identifier.string();
                if (first_name.is_empty())
                    first_name = name;

                // a. If IsConstantDeclaration of LetOrConst is true, then
                if (for_declaration.is_constant_declaration()) {
                    // i. Perform ! environment.CreateImmutableBinding(name, true).
                    MUST(iteration_environment->create_immutable_binding(vm, name, true));
                }
                // b. Else,
                else {
                    // i. Perform ! environment.CreateMutableBinding(name, false).
                    MUST(iteration_environment->create_mutable_binding(vm, name, false));
                }
            }));
            interpreter.vm().running_execution_context().lexical_environment = iteration_environment;

            if (!destructuring) {
                VERIFY(!first_name.is_empty());
                lhs_reference = MUST(interpreter.vm().resolve_binding(first_name));
            }
        }

        // i. If destructuring is false, then
        if (!destructuring) {
            VERIFY(lhs_reference.has_value());
            if (lhs_kind == LexicalBinding) {
                // 2. If IsUsingDeclaration of lhs is true, then
                if (is<UsingDeclaration>(expression_lhs)) {
                    // a. Let status be Completion(InitializeReferencedBinding(lhsRef, nextValue, sync-dispose)).
                    return lhs_reference->initialize_referenced_binding(vm, next_value, Environment::InitializeBindingHint::SyncDispose);
                }
                // 3. Else,
                else {
                    // a. Let status be Completion(InitializeReferencedBinding(lhsRef, nextValue, normal)).
                    return lhs_reference->initialize_referenced_binding(vm, next_value, Environment::InitializeBindingHint::Normal);
                }
            } else {
                return lhs_reference->put_value(vm, next_value);
            }
        }

        // j. Else,
        if (lhs_kind == Assignment) {
            VERIFY(pattern_lhs);
            return interpreter.vm().destructuring_assignment_evaluation(*pattern_lhs, next_value);
        }
        VERIFY(expression_lhs && is<VariableDeclaration>(*expression_lhs));
        auto& for_declaration = static_cast<VariableDeclaration const&>(*expression_lhs);
        auto& binding_pattern = for_declaration.declarations().first()->target().get<NonnullRefPtr<BindingPattern const>>();
        VERIFY(lhs_kind == VarBinding || iteration_environment);

        // At this point iteration_environment is undefined if lhs_kind == VarBinding which means this does both
        // branch j.ii and j.iii because ForBindingInitialization is just a forwarding call to BindingInitialization.
        return interpreter.vm().binding_initialization(binding_pattern, next_value, iteration_environment);
    }
};

// 14.7.5.5 Runtime Semantics: ForInOfLoopEvaluation, https://tc39.es/ecma262/#sec-runtime-semantics-forinofloopevaluation
// 14.7.5.6 ForIn/OfHeadEvaluation ( uninitializedBoundNames, expr, iterationKind ), https://tc39.es/ecma262/#sec-runtime-semantics-forinofheadevaluation
// This method combines ForInOfLoopEvaluation and ForIn/OfHeadEvaluation for similar reason as ForIn/OfBodyEvaluation, to prevent code duplication.
// For the same reason we also skip step 6 and 7 of ForIn/OfHeadEvaluation as this is done by the appropriate for loop type.
static ThrowCompletionOr<ForInOfHeadState> for_in_of_head_execute(Interpreter& interpreter, Variant<NonnullRefPtr<ASTNode const>, NonnullRefPtr<BindingPattern const>> lhs, Expression const& rhs)
{
    auto& vm = interpreter.vm();

    ForInOfHeadState state(lhs);
    if (auto* ast_ptr = lhs.get_pointer<NonnullRefPtr<ASTNode const>>(); ast_ptr && is<Declaration>(ast_ptr->ptr())) {
        // Runtime Semantics: ForInOfLoopEvaluation, for any of:
        //  ForInOfStatement : for ( var ForBinding in Expression ) Statement
        //  ForInOfStatement : for ( ForDeclaration in Expression ) Statement
        //  ForInOfStatement : for ( var ForBinding of AssignmentExpression ) Statement
        //  ForInOfStatement : for ( ForDeclaration of AssignmentExpression ) Statement

        // 14.7.5.6 ForIn/OfHeadEvaluation ( uninitializedBoundNames, expr, iterationKind ), https://tc39.es/ecma262/#sec-runtime-semantics-forinofheadevaluation
        Environment* new_environment = nullptr;

        if (is<VariableDeclaration>(ast_ptr->ptr())) {
            auto& variable_declaration = static_cast<VariableDeclaration const&>(*(*ast_ptr));
            VERIFY(variable_declaration.declarations().size() == 1);
            state.destructuring = variable_declaration.declarations().first()->target().has<NonnullRefPtr<BindingPattern const>>();
            if (variable_declaration.declaration_kind() == DeclarationKind::Var) {
                state.lhs_kind = ForInOfHeadState::VarBinding;
                auto& variable = variable_declaration.declarations().first();
                // B.3.5 Initializers in ForIn Statement Heads, https://tc39.es/ecma262/#sec-initializers-in-forin-statement-heads
                if (variable->init()) {
                    VERIFY(variable->target().has<NonnullRefPtr<Identifier const>>());
                    auto& binding_id = variable->target().get<NonnullRefPtr<Identifier const>>()->string();
                    auto reference = TRY(interpreter.vm().resolve_binding(binding_id));
                    auto result = TRY(interpreter.vm().named_evaluation_if_anonymous_function(*variable->init(), binding_id));
                    TRY(reference.put_value(vm, result));
                }
            } else {
                state.lhs_kind = ForInOfHeadState::LexicalBinding;
                new_environment = new_declarative_environment(*interpreter.lexical_environment());
                // NOTE: Due to the use of MUST with `create_mutable_binding` below, an exception should not result from `for_each_bound_identifier.
                MUST(variable_declaration.for_each_bound_identifier([&](auto const& identifier) {
                    MUST(new_environment->create_mutable_binding(vm, identifier.string(), false));
                }));
            }
        } else {
            VERIFY(is<UsingDeclaration>(ast_ptr->ptr()));
            auto& declaration = static_cast<UsingDeclaration const&>(*(*ast_ptr));
            state.lhs_kind = ForInOfHeadState::LexicalBinding;
            new_environment = new_declarative_environment(*interpreter.lexical_environment());
            // NOTE: Due to the use of MUST with `create_mutable_binding` below, an exception should not result from `for_each_bound_identifier.
            MUST(declaration.for_each_bound_identifier([&](auto const& identifier) {
                MUST(new_environment->create_mutable_binding(vm, identifier.string(), false));
            }));
        }

        if (new_environment) {
            // 2.d Set the running execution context's LexicalEnvironment to newEnv.
            TemporaryChange<GCPtr<Environment>> scope_change(interpreter.vm().running_execution_context().lexical_environment, new_environment);

            // 3. Let exprRef be the result of evaluating expr.
            // 5. Let exprValue be ? GetValue(exprRef).
            state.rhs_value = TRY(rhs.execute(interpreter)).release_value();

            // Note that since a reference stores its environment it doesn't matter we only reset
            // this after step 5. (Also we have no way of separating these steps at this point)
            // 4. Set the running execution context's LexicalEnvironment to oldEnv.
        } else {
            // 3. Let exprRef be the result of evaluating expr.
            // 5. Let exprValue be ? GetValue(exprRef).
            state.rhs_value = TRY(rhs.execute(interpreter)).release_value();
        }

        return state;
    }

    // Runtime Semantics: ForInOfLoopEvaluation, for any of:
    //  ForInOfStatement : for ( LeftHandSideExpression in Expression ) Statement
    //  ForInOfStatement : for ( LeftHandSideExpression of AssignmentExpression ) Statement

    // 14.7.5.6 ForIn/OfHeadEvaluation ( uninitializedBoundNames, expr, iterationKind ), https://tc39.es/ecma262/#sec-runtime-semantics-forinofheadevaluation

    // We can skip step 1, 2 and 4 here (on top of already skipping step 6 and 7).
    // 3. Let exprRef be the result of evaluating expr.
    // 5. Let exprValue be ? GetValue(exprRef).
    state.rhs_value = TRY(rhs.execute(interpreter)).release_value();
    return state;
}

// 14.1.1 Runtime Semantics: Evaluation, https://tc39.es/ecma262/#sec-statement-semantics-runtime-semantics-evaluation
// BreakableStatement : IterationStatement
Completion ForInStatement::execute(Interpreter& interpreter) const
{
    // 1. Let newLabelSet be a new empty List.
    // 2. Return ? LabelledEvaluation of this BreakableStatement with argument newLabelSet.
    return labelled_evaluation(interpreter, *this, {});
}

// 14.7.5.5 Runtime Semantics: ForInOfLoopEvaluation, https://tc39.es/ecma262/#sec-runtime-semantics-forinofloopevaluation
Completion ForInStatement::loop_evaluation(Interpreter& interpreter, Vector<DeprecatedFlyString> const& label_set) const
{
    InterpreterNodeScope node_scope { interpreter, *this };
    auto& vm = interpreter.vm();

    auto for_in_head_state = TRY(for_in_of_head_execute(interpreter, m_lhs, *m_rhs));

    auto rhs_result = for_in_head_state.rhs_value;

    // 14.7.5.6 ForIn/OfHeadEvaluation ( uninitializedBoundNames, expr, iterationKind ), https://tc39.es/ecma262/#sec-runtime-semantics-forinofheadevaluation

    // a. If exprValue is undefined or null, then
    if (rhs_result.is_nullish()) {
        // i. Return Completion Record { [[Type]]: break, [[Value]]: empty, [[Target]]: empty }.
        return { Completion::Type::Break, {}, {} };
    }

    // b. Let obj be ! ToObject(exprValue).
    auto object = MUST(rhs_result.to_object(vm));

    // 14.7.5.7 ForIn/OfBodyEvaluation ( lhs, stmt, iteratorRecord, iterationKind, lhsKind, labelSet [ , iteratorKind ] ), https://tc39.es/ecma262/#sec-runtime-semantics-forin-div-ofbodyevaluation-lhs-stmt-iterator-lhskind-labelset

    // 2. Let oldEnv be the running execution context's LexicalEnvironment.
    Environment* old_environment = interpreter.lexical_environment();
    auto restore_scope = ScopeGuard([&] {
        vm.running_execution_context().lexical_environment = old_environment;
    });

    // 3. Let V be undefined.
    auto last_value = js_undefined();

    auto result = object->enumerate_object_properties([&](auto value) -> Optional<Completion> {
        TRY(for_in_head_state.execute_head(interpreter, value));

        // l. Let result be the result of evaluating stmt.
        auto result = m_body->execute(interpreter);

        // NOTE: Because of optimizations we only create a new lexical environment if there are bindings
        //       so we should only dispose if that is the case.
        if (vm.running_execution_context().lexical_environment != old_environment) {
            VERIFY(is<DeclarativeEnvironment>(*vm.running_execution_context().lexical_environment));
            // m. Set result to DisposeResources(iterationEnv, result).
            result = dispose_resources(vm, static_cast<DeclarativeEnvironment*>(vm.running_execution_context().lexical_environment.ptr()), result);
        }

        // n. Set the running execution context's LexicalEnvironment to oldEnv.
        vm.running_execution_context().lexical_environment = old_environment;

        // o. If LoopContinues(result, labelSet) is false, then
        if (!loop_continues(result, label_set)) {
            // 1. Return UpdateEmpty(result, V).
            return result.update_empty(last_value);
        }

        // p. If result.[[Value]] is not empty, set V to result.[[Value]].
        if (result.value().has_value())
            last_value = *result.value();

        return {};
    });

    return result.value_or(last_value);
}

// 14.1.1 Runtime Semantics: Evaluation, https://tc39.es/ecma262/#sec-statement-semantics-runtime-semantics-evaluation
// BreakableStatement : IterationStatement
Completion ForOfStatement::execute(Interpreter& interpreter) const
{
    // 1. Let newLabelSet be a new empty List.
    // 2. Return ? LabelledEvaluation of this BreakableStatement with argument newLabelSet.
    return labelled_evaluation(interpreter, *this, {});
}

// 14.7.5.5 Runtime Semantics: ForInOfLoopEvaluation, https://tc39.es/ecma262/#sec-runtime-semantics-forinofloopevaluation
Completion ForOfStatement::loop_evaluation(Interpreter& interpreter, Vector<DeprecatedFlyString> const& label_set) const
{
    InterpreterNodeScope node_scope { interpreter, *this };
    auto& vm = interpreter.vm();

    auto for_of_head_state = TRY(for_in_of_head_execute(interpreter, m_lhs, m_rhs));

    auto rhs_result = for_of_head_state.rhs_value;

    // 14.7.5.7 ForIn/OfBodyEvaluation ( lhs, stmt, iteratorRecord, iterationKind, lhsKind, labelSet [ , iteratorKind ] ), https://tc39.es/ecma262/#sec-runtime-semantics-forin-div-ofbodyevaluation-lhs-stmt-iterator-lhskind-labelset
    // We use get_iterator_values which behaves like ForIn/OfBodyEvaluation with iteratorKind iterate.

    // 2. Let oldEnv be the running execution context's LexicalEnvironment.
    Environment* old_environment = interpreter.lexical_environment();
    auto restore_scope = ScopeGuard([&] {
        vm.running_execution_context().lexical_environment = old_environment;
    });

    // 3. Let V be undefined.
    auto last_value = js_undefined();

    Optional<Completion> status;

    (void)TRY(get_iterator_values(vm, rhs_result, [&](Value value) -> Optional<Completion> {
        TRY(for_of_head_state.execute_head(interpreter, value));

        // l. Let result be the result of evaluating stmt.
        auto result = m_body->execute(interpreter);

        if (vm.running_execution_context().lexical_environment != old_environment) {
            VERIFY(is<DeclarativeEnvironment>(*vm.running_execution_context().lexical_environment));
            result = dispose_resources(vm, static_cast<DeclarativeEnvironment*>(vm.running_execution_context().lexical_environment.ptr()), result);
        }

        // m. Set the running execution context's LexicalEnvironment to oldEnv.
        vm.running_execution_context().lexical_environment = old_environment;

        // n. If LoopContinues(result, labelSet) is false, then
        if (!loop_continues(result, label_set)) {
            // 2. Set status to UpdateEmpty(result, V).
            status = result.update_empty(last_value);

            // 4. Return ? IteratorClose(iteratorRecord, status).
            // NOTE: This is done by returning a completion from the callback.
            return status;
        }

        // o. If result.[[Value]] is not empty, set V to result.[[Value]].
        if (result.value().has_value())
            last_value = *result.value();

        return {};
    }));

    // Return `status` set during step n.2. in the callback, or...
    // e. If done is true, return V.
    return status.value_or(last_value);
}

// 14.1.1 Runtime Semantics: Evaluation, https://tc39.es/ecma262/#sec-statement-semantics-runtime-semantics-evaluation
// BreakableStatement : IterationStatement
Completion ForAwaitOfStatement::execute(Interpreter& interpreter) const
{
    // 1. Let newLabelSet be a new empty List.
    // 2. Return ? LabelledEvaluation of this BreakableStatement with argument newLabelSet.
    return labelled_evaluation(interpreter, *this, {});
}

// 14.7.5.5 Runtime Semantics: ForInOfLoopEvaluation, https://tc39.es/ecma262/#sec-runtime-semantics-forinofloopevaluation
Completion ForAwaitOfStatement::loop_evaluation(Interpreter& interpreter, Vector<DeprecatedFlyString> const& label_set) const
{
    InterpreterNodeScope node_scope { interpreter, *this };
    auto& vm = interpreter.vm();

    // 14.7.5.6 ForIn/OfHeadEvaluation ( uninitializedBoundNames, expr, iterationKind ), https://tc39.es/ecma262/#sec-runtime-semantics-forinofheadevaluation
    // Note: Performs only steps 1 through 5.
    auto for_of_head_state = TRY(for_in_of_head_execute(interpreter, m_lhs, m_rhs));

    auto rhs_result = for_of_head_state.rhs_value;

    // NOTE: Perform step 7 from ForIn/OfHeadEvaluation. And since this is always async we only have to do step 7.d.
    // d. Return ? GetIterator(exprValue, iteratorKind).
    auto iterator = TRY(get_iterator(vm, rhs_result, IteratorHint::Async));

    // 14.7.5.7 ForIn/OfBodyEvaluation ( lhs, stmt, iteratorRecord, iterationKind, lhsKind, labelSet [ , iteratorKind ] ), https://tc39.es/ecma262/#sec-runtime-semantics-forin-div-ofbodyevaluation-lhs-stmt-iterator-lhskind-labelset
    // NOTE: Here iteratorKind is always async.
    // 2. Let oldEnv be the running execution context's LexicalEnvironment.
    Environment* old_environment = interpreter.lexical_environment();
    auto restore_scope = ScopeGuard([&] {
        vm.running_execution_context().lexical_environment = old_environment;
    });
    // 3. Let V be undefined.
    auto last_value = js_undefined();

    // NOTE: Step 4 and 5 are just extracting properties from the head which is done already in for_in_of_head_execute.
    //       And these are only used in step 6.g through 6.k which is done with for_of_head_state.execute_head.

    // 6. Repeat,
    while (true) {
        // a. Let nextResult be ? Call(iteratorRecord.[[NextMethod]], iteratorRecord.[[Iterator]]).
        auto next_result = TRY(call(vm, iterator.next_method, iterator.iterator));

        // b. If iteratorKind is async, set nextResult to ? Await(nextResult).
        next_result = TRY(await(vm, next_result));

        // c. If Type(nextResult) is not Object, throw a TypeError exception.
        if (!next_result.is_object())
            return vm.throw_completion<TypeError>(ErrorType::IterableNextBadReturn);

        // d. Let done be ? IteratorComplete(nextResult).
        auto done = TRY(iterator_complete(vm, next_result.as_object()));

        // e. If done is true, return V.
        if (done)
            return last_value;

        // f. Let nextValue be ? IteratorValue(nextResult).
        auto next_value = TRY(iterator_value(vm, next_result.as_object()));

        // NOTE: This performs steps g. through to k.
        TRY(for_of_head_state.execute_head(interpreter, next_value));

        // l. Let result be the result of evaluating stmt.
        auto result = m_body->execute(interpreter);

        // m. Set the running execution context's LexicalEnvironment to oldEnv.
        interpreter.vm().running_execution_context().lexical_environment = old_environment;

        // n. If LoopContinues(result, labelSet) is false, then
        if (!loop_continues(result, label_set)) {
            // 2. Set status to UpdateEmpty(result, V).
            auto status = result.update_empty(last_value);

            // 3. If iteratorKind is async, return ? AsyncIteratorClose(iteratorRecord, status).
            return async_iterator_close(vm, iterator, move(status));
        }

        // o. If result.[[Value]] is not empty, set V to result.[[Value]].
        if (result.value().has_value())
            last_value = *result.value();
    }

    VERIFY_NOT_REACHED();
}

// 13.6.1 Runtime Semantics: Evaluation, https://tc39.es/ecma262/#sec-exp-operator-runtime-semantics-evaluation
// 13.7.1 Runtime Semantics: Evaluation, https://tc39.es/ecma262/#sec-multiplicative-operators-runtime-semantics-evaluation
// 13.8.1.1 Runtime Semantics: Evaluation, https://tc39.es/ecma262/#sec-addition-operator-plus-runtime-semantics-evaluation
// 13.8.2.1 Runtime Semantics: Evaluation, https://tc39.es/ecma262/#sec-subtraction-operator-minus-runtime-semantics-evaluation
// 13.9.1.1 Runtime Semantics: Evaluation, https://tc39.es/ecma262/#sec-left-shift-operator-runtime-semantics-evaluation
// 13.9.2.1 Runtime Semantics: Evaluation, https://tc39.es/ecma262/#sec-signed-right-shift-operator-runtime-semantics-evaluation
// 13.9.3.1 Runtime Semantics: Evaluation, https://tc39.es/ecma262/#sec-unsigned-right-shift-operator-runtime-semantics-evaluation
// 13.10.1 Runtime Semantics: Evaluation, https://tc39.es/ecma262/#sec-relational-operators-runtime-semantics-evaluation
// 13.11.1 Runtime Semantics: Evaluation, https://tc39.es/ecma262/#sec-equality-operators-runtime-semantics-evaluation
Completion BinaryExpression::execute(Interpreter& interpreter) const
{
    InterpreterNodeScope node_scope { interpreter, *this };
    auto& vm = interpreter.vm();

    // Special case in which we cannot execute the lhs.  RelationalExpression : PrivateIdentifier in ShiftExpression
    //  RelationalExpression : PrivateIdentifier in ShiftExpression, https://tc39.es/ecma262/#sec-relational-operators-runtime-semantics-evaluation
    if (m_op == BinaryOp::In && is<PrivateIdentifier>(*m_lhs)) {
        auto& private_identifier = static_cast<PrivateIdentifier const&>(*m_lhs).string();

        auto rhs_result = TRY(m_rhs->execute(interpreter)).release_value();
        if (!rhs_result.is_object())
            return interpreter.vm().throw_completion<TypeError>(ErrorType::InOperatorWithObject);
        auto private_environment = interpreter.vm().running_execution_context().private_environment;
        VERIFY(private_environment);
        auto private_name = private_environment->resolve_private_identifier(private_identifier);
        return Value(rhs_result.as_object().private_element_find(private_name) != nullptr);
    }

    auto lhs_result = TRY(m_lhs->execute(interpreter)).release_value();
    auto rhs_result = TRY(m_rhs->execute(interpreter)).release_value();

    switch (m_op) {
    case BinaryOp::Addition:
        return TRY(add(vm, lhs_result, rhs_result));
    case BinaryOp::Subtraction:
        return TRY(sub(vm, lhs_result, rhs_result));
    case BinaryOp::Multiplication:
        return TRY(mul(vm, lhs_result, rhs_result));
    case BinaryOp::Division:
        return TRY(div(vm, lhs_result, rhs_result));
    case BinaryOp::Modulo:
        return TRY(mod(vm, lhs_result, rhs_result));
    case BinaryOp::Exponentiation:
        return TRY(exp(vm, lhs_result, rhs_result));
    case BinaryOp::StrictlyEquals:
        return Value(is_strictly_equal(lhs_result, rhs_result));
    case BinaryOp::StrictlyInequals:
        return Value(!is_strictly_equal(lhs_result, rhs_result));
    case BinaryOp::LooselyEquals:
        return Value(TRY(is_loosely_equal(vm, lhs_result, rhs_result)));
    case BinaryOp::LooselyInequals:
        return Value(!TRY(is_loosely_equal(vm, lhs_result, rhs_result)));
    case BinaryOp::GreaterThan:
        return TRY(greater_than(vm, lhs_result, rhs_result));
    case BinaryOp::GreaterThanEquals:
        return TRY(greater_than_equals(vm, lhs_result, rhs_result));
    case BinaryOp::LessThan:
        return TRY(less_than(vm, lhs_result, rhs_result));
    case BinaryOp::LessThanEquals:
        return TRY(less_than_equals(vm, lhs_result, rhs_result));
    case BinaryOp::BitwiseAnd:
        return TRY(bitwise_and(vm, lhs_result, rhs_result));
    case BinaryOp::BitwiseOr:
        return TRY(bitwise_or(vm, lhs_result, rhs_result));
    case BinaryOp::BitwiseXor:
        return TRY(bitwise_xor(vm, lhs_result, rhs_result));
    case BinaryOp::LeftShift:
        return TRY(left_shift(vm, lhs_result, rhs_result));
    case BinaryOp::RightShift:
        return TRY(right_shift(vm, lhs_result, rhs_result));
    case BinaryOp::UnsignedRightShift:
        return TRY(unsigned_right_shift(vm, lhs_result, rhs_result));
    case BinaryOp::In:
        return TRY(in(vm, lhs_result, rhs_result));
    case BinaryOp::InstanceOf:
        return TRY(instance_of(vm, lhs_result, rhs_result));
    }

    VERIFY_NOT_REACHED();
}

// 13.13.1 Runtime Semantics: Evaluation, https://tc39.es/ecma262/#sec-binary-logical-operators-runtime-semantics-evaluation
Completion LogicalExpression::execute(Interpreter& interpreter) const
{
    InterpreterNodeScope node_scope { interpreter, *this };

    // 1. Let lref be the result of evaluating <Expression>.
    // 2. Let lval be ? GetValue(lref).
    auto lhs_result = TRY(m_lhs->execute(interpreter)).release_value();

    switch (m_op) {
    // LogicalANDExpression : LogicalANDExpression && BitwiseORExpression
    case LogicalOp::And:
        // 3. Let lbool be ToBoolean(lval).
        // 4. If lbool is false, return lval.
        if (!lhs_result.to_boolean())
            return lhs_result;

        // 5. Let rref be the result of evaluating BitwiseORExpression.
        // 6. Return ? GetValue(rref).
        return m_rhs->execute(interpreter);

    // LogicalORExpression : LogicalORExpression || LogicalANDExpression
    case LogicalOp::Or:
        // 3. Let lbool be ToBoolean(lval).
        // 4. If lbool is true, return lval.
        if (lhs_result.to_boolean())
            return lhs_result;

        // 5. Let rref be the result of evaluating LogicalANDExpression.
        // 6. Return ? GetValue(rref).
        return m_rhs->execute(interpreter);

    // CoalesceExpression : CoalesceExpressionHead ?? BitwiseORExpression
    case LogicalOp::NullishCoalescing:
        // 3. If lval is undefined or null, then
        if (lhs_result.is_nullish()) {
            // a. Let rref be the result of evaluating BitwiseORExpression.
            // b. Return ? GetValue(rref).
            return m_rhs->execute(interpreter);
        }

        // 4. Otherwise, return lval.
        return lhs_result;
    }

    VERIFY_NOT_REACHED();
}

ThrowCompletionOr<Reference> Expression::to_reference(Interpreter&) const
{
    return Reference {};
}

ThrowCompletionOr<Reference> Identifier::to_reference(Interpreter& interpreter) const
{
    if (m_cached_environment_coordinate.is_valid()) {
        auto environment = interpreter.vm().running_execution_context().lexical_environment;
        for (size_t i = 0; i < m_cached_environment_coordinate.hops; ++i)
            environment = environment->outer_environment();
        VERIFY(environment);
        VERIFY(environment->is_declarative_environment());
        if (!environment->is_permanently_screwed_by_eval()) {
            return Reference { *environment, string(), interpreter.vm().in_strict_mode(), m_cached_environment_coordinate };
        }
        m_cached_environment_coordinate = {};
    }

    auto reference = TRY(interpreter.vm().resolve_binding(string()));
    if (reference.environment_coordinate().has_value())
        m_cached_environment_coordinate = reference.environment_coordinate().value();
    return reference;
}

ThrowCompletionOr<Reference> MemberExpression::to_reference(Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();

    // 13.3.7.1 Runtime Semantics: Evaluation
    // SuperProperty : super [ Expression ]
    // SuperProperty : super . IdentifierName
    // https://tc39.es/ecma262/#sec-super-keyword-runtime-semantics-evaluation
    if (is<SuperExpression>(object())) {
        // 1. Let env be GetThisEnvironment().
        auto environment = get_this_environment(vm);

        // 2. Let actualThis be ? env.GetThisBinding().
        auto actual_this = TRY(environment->get_this_binding(vm));

        PropertyKey property_key;

        if (is_computed()) {
            // SuperProperty : super [ Expression ]

            // 3. Let propertyNameReference be the result of evaluating Expression.
            // 4. Let propertyNameValue be ? GetValue(propertyNameReference).
            auto property_name_value = TRY(m_property->execute(interpreter)).release_value();

            // 5. Let propertyKey be ? ToPropertyKey(propertyNameValue).
            property_key = TRY(property_name_value.to_property_key(vm));
        } else {
            // SuperProperty : super . IdentifierName

            // 3. Let propertyKey be StringValue of IdentifierName.
            VERIFY(is<Identifier>(property()));
            property_key = static_cast<Identifier const&>(property()).string();
        }

        // 6. If the source text matched by this SuperProperty is strict mode code, let strict be true; else let strict be false.
        bool strict = interpreter.vm().in_strict_mode();

        // 7. Return ? MakeSuperPropertyReference(actualThis, propertyKey, strict).
        return TRY(make_super_property_reference(vm, actual_this, property_key, strict));
    }

    auto base_reference = TRY(m_object->to_reference(interpreter));

    Value base_value;

    if (base_reference.is_valid_reference())
        base_value = TRY(base_reference.get_value(vm));
    else
        base_value = TRY(m_object->execute(interpreter)).release_value();

    VERIFY(!base_value.is_empty());

    // From here on equivalent to
    // 13.3.4 EvaluatePropertyAccessWithIdentifierKey ( baseValue, identifierName, strict ), https://tc39.es/ecma262/#sec-evaluate-property-access-with-identifier-key
    PropertyKey property_key;
    if (is_computed()) {
        // Weird order which I can't quite find from the specs.
        auto value = TRY(m_property->execute(interpreter)).release_value();
        VERIFY(!value.is_empty());

        TRY(require_object_coercible(vm, base_value));

        property_key = TRY(value.to_property_key(vm));
    } else if (is<PrivateIdentifier>(*m_property)) {
        auto& private_identifier = static_cast<PrivateIdentifier const&>(*m_property);
        return make_private_reference(interpreter.vm(), base_value, private_identifier.string());
    } else {
        property_key = verify_cast<Identifier>(*m_property).string();
        TRY(require_object_coercible(vm, base_value));
    }
    if (!property_key.is_valid())
        return Reference {};

    auto strict = interpreter.vm().in_strict_mode();
    return Reference { base_value, move(property_key), {}, strict };
}

// 13.5.1.2 Runtime Semantics: Evaluation, https://tc39.es/ecma262/#sec-delete-operator-runtime-semantics-evaluation
// 13.5.2.1 Runtime Semantics: Evaluation, https://tc39.es/ecma262/#sec-void-operator-runtime-semantics-evaluation
// 13.5.3.1 Runtime Semantics: Evaluation, https://tc39.es/ecma262/#sec-typeof-operator-runtime-semantics-evaluation
// 13.5.4.1 Runtime Semantics: Evaluation, https://tc39.es/ecma262/#sec-unary-plus-operator-runtime-semantics-evaluation
// 13.5.5.1 Runtime Semantics: Evaluation, https://tc39.es/ecma262/#sec-unary-minus-operator-runtime-semantics-evaluation
// 13.5.6.1 Runtime Semantics: Evaluation, https://tc39.es/ecma262/#sec-bitwise-not-operator-runtime-semantics-evaluation
// 13.5.7.1 Runtime Semantics: Evaluation, https://tc39.es/ecma262/#sec-logical-not-operator-runtime-semantics-evaluation
Completion UnaryExpression::execute(Interpreter& interpreter) const
{
    InterpreterNodeScope node_scope { interpreter, *this };
    auto& vm = interpreter.vm();

    if (m_op == UnaryOp::Delete) {
        auto reference = TRY(m_lhs->to_reference(interpreter));
        return Value(TRY(reference.delete_(vm)));
    }

    Value lhs_result;
    if (m_op == UnaryOp::Typeof && is<Identifier>(*m_lhs)) {
        auto reference = TRY(m_lhs->to_reference(interpreter));

        if (reference.is_unresolvable())
            lhs_result = js_undefined();
        else
            lhs_result = TRY(reference.get_value(vm));
        VERIFY(!lhs_result.is_empty());
    } else {
        // 1. Let expr be the result of evaluating UnaryExpression.
        lhs_result = TRY(m_lhs->execute(interpreter)).release_value();
    }

    switch (m_op) {
    case UnaryOp::BitwiseNot:
        return TRY(bitwise_not(vm, lhs_result));
    case UnaryOp::Not:
        return Value(!lhs_result.to_boolean());
    case UnaryOp::Plus:
        return TRY(unary_plus(vm, lhs_result));
    case UnaryOp::Minus:
        return TRY(unary_minus(vm, lhs_result));
    case UnaryOp::Typeof:
        return Value { MUST_OR_THROW_OOM(PrimitiveString::create(vm, lhs_result.typeof())) };
    case UnaryOp::Void:
        return js_undefined();
    case UnaryOp::Delete:
        VERIFY_NOT_REACHED();
    }

    VERIFY_NOT_REACHED();
}

Completion SuperExpression::execute(Interpreter&) const
{
    // The semantics for SuperExpression are handled in CallExpression and SuperCall.
    VERIFY_NOT_REACHED();
}

Completion ClassElement::execute(Interpreter&) const
{
    // Note: The semantics of class element are handled in class_element_evaluation
    VERIFY_NOT_REACHED();
}

static ThrowCompletionOr<ClassElementName> class_key_to_property_name(VM& vm, Expression const& key)
{
    if (is<PrivateIdentifier>(key)) {
        auto& private_identifier = static_cast<PrivateIdentifier const&>(key);
        auto private_environment = vm.running_execution_context().private_environment;
        VERIFY(private_environment);
        return ClassElementName { private_environment->resolve_private_identifier(private_identifier.string()) };
    }

    auto prop_key = TRY(vm.execute_ast_node(key));

    if (prop_key.is_object())
        prop_key = TRY(prop_key.to_primitive(vm, Value::PreferredType::String));

    auto property_key = TRY(PropertyKey::from_value(vm, prop_key));
    return ClassElementName { property_key };
}

// 15.4.5 Runtime Semantics: MethodDefinitionEvaluation, https://tc39.es/ecma262/#sec-runtime-semantics-methoddefinitionevaluation
ThrowCompletionOr<ClassElement::ClassValue> ClassMethod::class_element_evaluation(VM& vm, Object& target) const
{
    auto property_key_or_private_name = TRY(class_key_to_property_name(vm, *m_key));

    auto method_value = TRY(vm.execute_ast_node(*m_function));

    auto function_handle = make_handle(&method_value.as_function());

    auto& method_function = static_cast<ECMAScriptFunctionObject&>(method_value.as_function());
    method_function.make_method(target);

    auto set_function_name = [&](DeprecatedString prefix = "") {
        auto name = property_key_or_private_name.visit(
            [&](PropertyKey const& property_key) -> DeprecatedString {
                if (property_key.is_symbol()) {
                    auto description = property_key.as_symbol()->description();
                    if (!description.has_value() || description->is_empty())
                        return "";
                    return DeprecatedString::formatted("[{}]", *description);
                } else {
                    return property_key.to_string();
                }
            },
            [&](PrivateName const& private_name) -> DeprecatedString {
                return private_name.description;
            });

        update_function_name(method_value, DeprecatedString::formatted("{}{}{}", prefix, prefix.is_empty() ? "" : " ", name));
    };

    if (property_key_or_private_name.has<PropertyKey>()) {
        auto& property_key = property_key_or_private_name.get<PropertyKey>();
        switch (kind()) {
        case ClassMethod::Kind::Method:
            set_function_name();
            TRY(target.define_property_or_throw(property_key, { .value = method_value, .writable = true, .enumerable = false, .configurable = true }));
            break;
        case ClassMethod::Kind::Getter:
            set_function_name("get");
            TRY(target.define_property_or_throw(property_key, { .get = &method_function, .enumerable = true, .configurable = true }));
            break;
        case ClassMethod::Kind::Setter:
            set_function_name("set");
            TRY(target.define_property_or_throw(property_key, { .set = &method_function, .enumerable = true, .configurable = true }));
            break;
        default:
            VERIFY_NOT_REACHED();
        }

        return ClassValue { normal_completion({}) };
    } else {
        auto& private_name = property_key_or_private_name.get<PrivateName>();
        switch (kind()) {
        case Kind::Method:
            set_function_name();
            return ClassValue { PrivateElement { private_name, PrivateElement::Kind::Method, make_handle(method_value) } };
        case Kind::Getter:
            set_function_name("get");
            return ClassValue { PrivateElement { private_name, PrivateElement::Kind::Accessor, make_handle(Value(Accessor::create(vm, &method_function, nullptr))) } };
        case Kind::Setter:
            set_function_name("set");
            return ClassValue { PrivateElement { private_name, PrivateElement::Kind::Accessor, make_handle(Value(Accessor::create(vm, nullptr, &method_function))) } };
        default:
            VERIFY_NOT_REACHED();
        }
    }
}

Completion ClassFieldInitializerStatement::execute(Interpreter& interpreter) const
{
    // 1. Assert: argumentsList is empty.
    VERIFY(interpreter.vm().argument_count() == 0);

    // 2. Assert: functionObject.[[ClassFieldInitializerName]] is not empty.
    VERIFY(!m_class_field_identifier_name.is_empty());

    // 3. If IsAnonymousFunctionDefinition(AssignmentExpression) is true, then
    //    a. Let value be ? NamedEvaluation of Initializer with argument functionObject.[[ClassFieldInitializerName]].
    // 4. Else,
    //    a. Let rhs be the result of evaluating AssignmentExpression.
    //    b. Let value be ? GetValue(rhs).
    auto value = TRY(interpreter.vm().named_evaluation_if_anonymous_function(m_expression, m_class_field_identifier_name));

    // 5. Return Completion Record { [[Type]]: return, [[Value]]: value, [[Target]]: empty }.
    return { Completion::Type::Return, value, {} };
}

void ClassFieldInitializerStatement::dump(int) const
{
    // This should not be dumped as it is never part of an actual AST.
    VERIFY_NOT_REACHED();
}

// 15.7.10 Runtime Semantics: ClassFieldDefinitionEvaluation, https://tc39.es/ecma262/#sec-runtime-semantics-classfielddefinitionevaluation
ThrowCompletionOr<ClassElement::ClassValue> ClassField::class_element_evaluation(VM& vm, Object& target) const
{
    auto& realm = *vm.current_realm();

    auto property_key_or_private_name = TRY(class_key_to_property_name(vm, *m_key));
    Handle<ECMAScriptFunctionObject> initializer {};
    if (m_initializer) {
        auto copy_initializer = m_initializer;
        auto name = property_key_or_private_name.visit(
            [&](PropertyKey const& property_key) -> DeprecatedString {
                return property_key.is_number() ? property_key.to_string() : property_key.to_string_or_symbol().to_display_string();
            },
            [&](PrivateName const& private_name) -> DeprecatedString {
                return private_name.description;
            });

        // FIXME: A potential optimization is not creating the functions here since these are never directly accessible.
        auto function_code = create_ast_node<ClassFieldInitializerStatement>(m_initializer->source_range(), copy_initializer.release_nonnull(), name);
        initializer = make_handle(*ECMAScriptFunctionObject::create(realm, DeprecatedString::empty(), DeprecatedString::empty(), *function_code, {}, 0, {}, vm.lexical_environment(), vm.running_execution_context().private_environment, FunctionKind::Normal, true, false, m_contains_direct_call_to_eval, false, property_key_or_private_name));
        initializer->make_method(target);
    }

    return ClassValue {
        ClassFieldDefinition {
            move(property_key_or_private_name),
            move(initializer),
        }
    };
}

static Optional<DeprecatedFlyString> nullopt_or_private_identifier_description(Expression const& expression)
{
    if (is<PrivateIdentifier>(expression))
        return static_cast<PrivateIdentifier const&>(expression).string();
    return {};
}

Optional<DeprecatedFlyString> ClassField::private_bound_identifier() const
{
    return nullopt_or_private_identifier_description(*m_key);
}

Optional<DeprecatedFlyString> ClassMethod::private_bound_identifier() const
{
    return nullopt_or_private_identifier_description(*m_key);
}

// 15.7.11 Runtime Semantics: ClassStaticBlockDefinitionEvaluation, https://tc39.es/ecma262/#sec-runtime-semantics-classstaticblockdefinitionevaluation
ThrowCompletionOr<ClassElement::ClassValue> StaticInitializer::class_element_evaluation(VM& vm, Object& home_object) const
{
    auto& realm = *vm.current_realm();

    // 1. Let lex be the running execution context's LexicalEnvironment.
    auto lexical_environment = vm.running_execution_context().lexical_environment;

    // 2. Let privateEnv be the running execution context's PrivateEnvironment.
    auto private_environment = vm.running_execution_context().private_environment;

    // 3. Let sourceText be the empty sequence of Unicode code points.
    // 4. Let formalParameters be an instance of the production FormalParameters : [empty] .
    // 5. Let bodyFunction be OrdinaryFunctionCreate(%Function.prototype%, sourceText, formalParameters, ClassStaticBlockBody, non-lexical-this, lex, privateEnv).
    // Note: The function bodyFunction is never directly accessible to ECMAScript code.
    auto body_function = ECMAScriptFunctionObject::create(realm, DeprecatedString::empty(), DeprecatedString::empty(), *m_function_body, {}, 0, m_function_body->local_variables_names(), lexical_environment, private_environment, FunctionKind::Normal, true, false, m_contains_direct_call_to_eval, false);

    // 6. Perform MakeMethod(bodyFunction, homeObject).
    body_function->make_method(home_object);

    // 7. Return the ClassStaticBlockDefinition Record { [[BodyFunction]]: bodyFunction }.
    return ClassValue { normal_completion(body_function) };
}

// 15.7.16 Runtime Semantics: Evaluation, https://tc39.es/ecma262/#sec-class-definitions-runtime-semantics-evaluation
// ClassExpression : class BindingIdentifier ClassTail
Completion ClassExpression::execute(Interpreter& interpreter) const
{
    InterpreterNodeScope node_scope { interpreter, *this };

    // 1. Let className be StringValue of BindingIdentifier.
    // 2. Let value be ? ClassDefinitionEvaluation of ClassTail with arguments className and className.
    auto* value = TRY(class_definition_evaluation(interpreter.vm(), name(), name()));

    // 3. Set value.[[SourceText]] to the source text matched by ClassExpression.
    value->set_source_text(m_source_text);

    // 4. Return value.
    return Value { value };
}

// 15.7.15 Runtime Semantics: BindingClassDeclarationEvaluation, https://tc39.es/ecma262/#sec-runtime-semantics-bindingclassdeclarationevaluation
static ThrowCompletionOr<Value> binding_class_declaration_evaluation(Interpreter& interpreter, ClassExpression const& class_expression)
{
    auto& vm = interpreter.vm();

    // ClassDeclaration : class ClassTail
    if (!class_expression.has_name()) {
        // 1. Let value be ? ClassDefinitionEvaluation of ClassTail with arguments undefined and "default".
        auto value = TRY(class_expression.class_definition_evaluation(vm, {}, "default"));

        // 2. Set value.[[SourceText]] to the source text matched by ClassDeclaration.
        value->set_source_text(class_expression.source_text());

        // 3. Return value.
        return value;
    }

    // ClassDeclaration : class BindingIdentifier ClassTail

    // 1. Let className be StringValue of BindingIdentifier.
    auto class_name = class_expression.name();
    VERIFY(!class_name.is_empty());

    // 2. Let value be ? ClassDefinitionEvaluation of ClassTail with arguments className and className.
    auto value = TRY(class_expression.class_definition_evaluation(vm, class_name, class_name));

    // 3. Set value.[[SourceText]] to the source text matched by ClassDeclaration.
    value->set_source_text(class_expression.source_text());

    // 4. Let env be the running execution context's LexicalEnvironment.
    auto* env = interpreter.lexical_environment();

    // 5. Perform ? InitializeBoundName(className, value, env).
    TRY(initialize_bound_name(vm, class_name, value, env));

    // 6. Return value.
    return value;
}

// 15.7.16 Runtime Semantics: Evaluation, https://tc39.es/ecma262/#sec-class-definitions-runtime-semantics-evaluation
// ClassDeclaration : class BindingIdentifier ClassTail
Completion ClassDeclaration::execute(Interpreter& interpreter) const
{
    InterpreterNodeScope node_scope { interpreter, *this };

    // 1. Perform ? BindingClassDeclarationEvaluation of this ClassDeclaration.
    (void)TRY(binding_class_declaration_evaluation(interpreter, m_class_expression));

    // 2. Return empty.
    return Optional<Value> {};
}

ThrowCompletionOr<ECMAScriptFunctionObject*> ClassExpression::create_class_constructor(VM& vm, Environment* class_environment, Environment* environment, Value super_class, DeprecatedFlyString const& binding_name, DeprecatedFlyString const& class_name) const
{
    auto& realm = *vm.current_realm();

    // We might not set the lexical environment but we always want to restore it eventually.
    ArmedScopeGuard restore_environment = [&] {
        vm.running_execution_context().lexical_environment = environment;
    };

    auto outer_private_environment = vm.running_execution_context().private_environment;
    auto class_private_environment = new_private_environment(vm, outer_private_environment);

    auto proto_parent = GCPtr { realm.intrinsics().object_prototype() };
    auto constructor_parent = realm.intrinsics().function_prototype();

    for (auto const& element : m_elements) {
        auto opt_private_name = element->private_bound_identifier();
        if (opt_private_name.has_value())
            class_private_environment->add_private_name({}, opt_private_name.release_value());
    }

    if (!m_super_class.is_null()) {
        if (super_class.is_null()) {
            proto_parent = nullptr;
        } else if (!super_class.is_constructor()) {
            return vm.throw_completion<TypeError>(ErrorType::ClassExtendsValueNotAConstructorOrNull, TRY_OR_THROW_OOM(vm, super_class.to_string_without_side_effects()));
        } else {
            auto super_class_prototype = TRY(super_class.get(vm, vm.names.prototype));
            if (!super_class_prototype.is_null() && !super_class_prototype.is_object())
                return vm.throw_completion<TypeError>(ErrorType::ClassExtendsValueInvalidPrototype, TRY_OR_THROW_OOM(vm, super_class_prototype.to_string_without_side_effects()));

            if (super_class_prototype.is_null())
                proto_parent = nullptr;
            else
                proto_parent = super_class_prototype.as_object();

            constructor_parent = super_class.as_object();
        }
    }

    auto prototype = Object::create(realm, proto_parent);
    VERIFY(prototype);

    vm.running_execution_context().lexical_environment = class_environment;
    vm.running_execution_context().private_environment = class_private_environment;
    ScopeGuard restore_private_environment = [&] {
        vm.running_execution_context().private_environment = outer_private_environment;
    };

    // FIXME: Step 14.a is done in the parser. By using a synthetic super(...args) which does not call @@iterator of %Array.prototype%
    auto const& constructor = *m_constructor;
    auto class_constructor = ECMAScriptFunctionObject::create(
        realm,
        constructor.name(),
        constructor.source_text(),
        constructor.body(),
        constructor.parameters(),
        constructor.function_length(),
        constructor.local_variables_names(),
        vm.lexical_environment(),
        vm.running_execution_context().private_environment,
        constructor.kind(),
        constructor.is_strict_mode(),
        constructor.might_need_arguments_object(),
        constructor.contains_direct_call_to_eval(),
        constructor.is_arrow_function());

    class_constructor->set_name(class_name);
    class_constructor->set_home_object(prototype);
    class_constructor->set_is_class_constructor();
    class_constructor->define_direct_property(vm.names.prototype, prototype, Attribute::Writable);
    TRY(class_constructor->internal_set_prototype_of(constructor_parent));

    if (!m_super_class.is_null())
        class_constructor->set_constructor_kind(ECMAScriptFunctionObject::ConstructorKind::Derived);

    prototype->define_direct_property(vm.names.constructor, class_constructor, Attribute::Writable | Attribute::Configurable);

    using StaticElement = Variant<ClassFieldDefinition, Handle<ECMAScriptFunctionObject>>;

    Vector<PrivateElement> static_private_methods;
    Vector<PrivateElement> instance_private_methods;
    Vector<ClassFieldDefinition> instance_fields;
    Vector<StaticElement> static_elements;

    for (auto const& element : m_elements) {
        // Note: All ClassElementEvaluation start with evaluating the name (or we fake it).
        auto element_value = TRY(element->class_element_evaluation(vm, element->is_static() ? *class_constructor : *prototype));

        if (element_value.has<PrivateElement>()) {
            auto& container = element->is_static() ? static_private_methods : instance_private_methods;

            auto& private_element = element_value.get<PrivateElement>();

            auto added_to_existing = false;
            // FIXME: We can skip this loop in most cases.
            for (auto& existing : container) {
                if (existing.key == private_element.key) {
                    VERIFY(existing.kind == PrivateElement::Kind::Accessor);
                    VERIFY(private_element.kind == PrivateElement::Kind::Accessor);
                    auto& accessor = private_element.value.value().as_accessor();
                    if (!accessor.getter())
                        existing.value.value().as_accessor().set_setter(accessor.setter());
                    else
                        existing.value.value().as_accessor().set_getter(accessor.getter());
                    added_to_existing = true;
                }
            }

            if (!added_to_existing)
                container.append(move(element_value.get<PrivateElement>()));
        } else if (auto* class_field_definition_ptr = element_value.get_pointer<ClassFieldDefinition>()) {
            if (element->is_static())
                static_elements.append(move(*class_field_definition_ptr));
            else
                instance_fields.append(move(*class_field_definition_ptr));
        } else if (element->class_element_kind() == ClassElement::ElementKind::StaticInitializer) {
            // We use Completion to hold the ClassStaticBlockDefinition Record.
            VERIFY(element_value.has<Completion>() && element_value.get<Completion>().value().has_value());
            auto& element_object = element_value.get<Completion>().value()->as_object();
            VERIFY(is<ECMAScriptFunctionObject>(element_object));
            static_elements.append(make_handle(static_cast<ECMAScriptFunctionObject*>(&element_object)));
        }
    }

    vm.running_execution_context().lexical_environment = environment;
    restore_environment.disarm();

    if (!binding_name.is_null())
        MUST(class_environment->initialize_binding(vm, binding_name, class_constructor, Environment::InitializeBindingHint::Normal));

    for (auto& field : instance_fields)
        class_constructor->add_field(field);

    for (auto& private_method : instance_private_methods)
        class_constructor->add_private_method(private_method);

    for (auto& method : static_private_methods)
        TRY(class_constructor->private_method_or_accessor_add(move(method)));

    for (auto& element : static_elements) {
        TRY(element.visit(
            [&](ClassFieldDefinition& field) -> ThrowCompletionOr<void> {
                return TRY(class_constructor->define_field(field));
            },
            [&](Handle<ECMAScriptFunctionObject> static_block_function) -> ThrowCompletionOr<void> {
                VERIFY(!static_block_function.is_null());
                // We discard any value returned here.
                TRY(call(vm, *static_block_function.cell(), class_constructor));
                return {};
            }));
    }

    class_constructor->set_source_text(source_text());

    return { class_constructor };
}

// 15.7.14 Runtime Semantics: ClassDefinitionEvaluation, https://tc39.es/ecma262/#sec-runtime-semantics-classdefinitionevaluation
ThrowCompletionOr<ECMAScriptFunctionObject*> ClassExpression::class_definition_evaluation(VM& vm, DeprecatedFlyString const& binding_name, DeprecatedFlyString const& class_name) const
{
    auto* environment = vm.lexical_environment();
    VERIFY(environment);
    auto class_environment = new_declarative_environment(*environment);

    Value super_class;

    if (!binding_name.is_null())
        MUST(class_environment->create_immutable_binding(vm, binding_name, true));

    if (!m_super_class.is_null()) {
        vm.running_execution_context().lexical_environment = class_environment;

        // Note: Since our execute does evaluation and GetValue in once we must check for a valid reference first
        auto reference = TRY(m_super_class->to_reference(vm.interpreter()));
        if (reference.is_valid_reference()) {
            super_class = TRY(reference.get_value(vm));
        } else {
            super_class = TRY(vm.execute_ast_node(*m_super_class));
        }

        vm.running_execution_context().lexical_environment = environment;
    }

    return create_class_constructor(vm, class_environment, environment, super_class, binding_name, class_name);
}

void ASTNode::dump(int indent) const
{
    print_indent(indent);
    outln("{}", class_name());
}

void ScopeNode::dump(int indent) const
{
    ASTNode::dump(indent);
    if (!m_lexical_declarations.is_empty()) {
        print_indent(indent + 1);
        outln("(Lexical declarations)");
        for (auto& declaration : m_lexical_declarations)
            declaration->dump(indent + 2);
    }

    if (!m_var_declarations.is_empty()) {
        print_indent(indent + 1);
        outln("(Variable declarations)");
        for (auto& declaration : m_var_declarations)
            declaration->dump(indent + 2);
    }

    if (!m_functions_hoistable_with_annexB_extension.is_empty()) {
        print_indent(indent + 1);
        outln("(Hoisted functions via annexB extension)");
        for (auto& declaration : m_functions_hoistable_with_annexB_extension)
            declaration->dump(indent + 2);
    }

    if (!m_children.is_empty()) {
        print_indent(indent + 1);
        outln("(Children)");
        for (auto& child : children())
            child->dump(indent + 2);
    }
}

void BinaryExpression::dump(int indent) const
{
    char const* op_string = nullptr;
    switch (m_op) {
    case BinaryOp::Addition:
        op_string = "+";
        break;
    case BinaryOp::Subtraction:
        op_string = "-";
        break;
    case BinaryOp::Multiplication:
        op_string = "*";
        break;
    case BinaryOp::Division:
        op_string = "/";
        break;
    case BinaryOp::Modulo:
        op_string = "%";
        break;
    case BinaryOp::Exponentiation:
        op_string = "**";
        break;
    case BinaryOp::StrictlyEquals:
        op_string = "===";
        break;
    case BinaryOp::StrictlyInequals:
        op_string = "!==";
        break;
    case BinaryOp::LooselyEquals:
        op_string = "==";
        break;
    case BinaryOp::LooselyInequals:
        op_string = "!=";
        break;
    case BinaryOp::GreaterThan:
        op_string = ">";
        break;
    case BinaryOp::GreaterThanEquals:
        op_string = ">=";
        break;
    case BinaryOp::LessThan:
        op_string = "<";
        break;
    case BinaryOp::LessThanEquals:
        op_string = "<=";
        break;
    case BinaryOp::BitwiseAnd:
        op_string = "&";
        break;
    case BinaryOp::BitwiseOr:
        op_string = "|";
        break;
    case BinaryOp::BitwiseXor:
        op_string = "^";
        break;
    case BinaryOp::LeftShift:
        op_string = "<<";
        break;
    case BinaryOp::RightShift:
        op_string = ">>";
        break;
    case BinaryOp::UnsignedRightShift:
        op_string = ">>>";
        break;
    case BinaryOp::In:
        op_string = "in";
        break;
    case BinaryOp::InstanceOf:
        op_string = "instanceof";
        break;
    }

    print_indent(indent);
    outln("{}", class_name());
    m_lhs->dump(indent + 1);
    print_indent(indent + 1);
    outln("{}", op_string);
    m_rhs->dump(indent + 1);
}

void LogicalExpression::dump(int indent) const
{
    char const* op_string = nullptr;
    switch (m_op) {
    case LogicalOp::And:
        op_string = "&&";
        break;
    case LogicalOp::Or:
        op_string = "||";
        break;
    case LogicalOp::NullishCoalescing:
        op_string = "??";
        break;
    }

    print_indent(indent);
    outln("{}", class_name());
    m_lhs->dump(indent + 1);
    print_indent(indent + 1);
    outln("{}", op_string);
    m_rhs->dump(indent + 1);
}

void UnaryExpression::dump(int indent) const
{
    char const* op_string = nullptr;
    switch (m_op) {
    case UnaryOp::BitwiseNot:
        op_string = "~";
        break;
    case UnaryOp::Not:
        op_string = "!";
        break;
    case UnaryOp::Plus:
        op_string = "+";
        break;
    case UnaryOp::Minus:
        op_string = "-";
        break;
    case UnaryOp::Typeof:
        op_string = "typeof ";
        break;
    case UnaryOp::Void:
        op_string = "void ";
        break;
    case UnaryOp::Delete:
        op_string = "delete ";
        break;
    }

    print_indent(indent);
    outln("{}", class_name());
    print_indent(indent + 1);
    outln("{}", op_string);
    m_lhs->dump(indent + 1);
}

void CallExpression::dump(int indent) const
{
    print_indent(indent);
    if (is<NewExpression>(*this))
        outln("CallExpression [new]");
    else
        outln("CallExpression");
    m_callee->dump(indent + 1);
    for (auto& argument : arguments())
        argument.value->dump(indent + 1);
}

void SuperCall::dump(int indent) const
{
    print_indent(indent);
    outln("SuperCall");
    for (auto& argument : m_arguments)
        argument.value->dump(indent + 1);
}

void ClassDeclaration::dump(int indent) const
{
    ASTNode::dump(indent);
    m_class_expression->dump(indent + 1);
}

ThrowCompletionOr<void> ClassDeclaration::for_each_bound_identifier(ThrowCompletionOrVoidCallback<Identifier const&>&& callback) const
{
    if (!m_class_expression->m_name)
        return {};

    return callback(*m_class_expression->m_name);
}

void ClassExpression::dump(int indent) const
{
    print_indent(indent);
    outln("ClassExpression: \"{}\"", name());

    print_indent(indent);
    outln("(Constructor)");
    m_constructor->dump(indent + 1);

    if (!m_super_class.is_null()) {
        print_indent(indent);
        outln("(Super Class)");
        m_super_class->dump(indent + 1);
    }

    print_indent(indent);
    outln("(Elements)");
    for (auto& method : m_elements)
        method->dump(indent + 1);
}

void ClassMethod::dump(int indent) const
{
    ASTNode::dump(indent);

    print_indent(indent);
    outln("(Key)");
    m_key->dump(indent + 1);

    char const* kind_string = nullptr;
    switch (m_kind) {
    case Kind::Method:
        kind_string = "Method";
        break;
    case Kind::Getter:
        kind_string = "Getter";
        break;
    case Kind::Setter:
        kind_string = "Setter";
        break;
    }
    print_indent(indent);
    outln("Kind: {}", kind_string);

    print_indent(indent);
    outln("Static: {}", is_static());

    print_indent(indent);
    outln("(Function)");
    m_function->dump(indent + 1);
}

void ClassField::dump(int indent) const
{
    ASTNode::dump(indent);
    print_indent(indent);
    outln("(Key)");
    m_key->dump(indent + 1);

    print_indent(indent);
    outln("Static: {}", is_static());

    if (m_initializer) {
        print_indent(indent);
        outln("(Initializer)");
        m_initializer->dump(indent + 1);
    }
}

void StaticInitializer::dump(int indent) const
{
    ASTNode::dump(indent);
    m_function_body->dump(indent + 1);
}

void StringLiteral::dump(int indent) const
{
    print_indent(indent);
    outln("StringLiteral \"{}\"", m_value);
}

void SuperExpression::dump(int indent) const
{
    print_indent(indent);
    outln("super");
}

void NumericLiteral::dump(int indent) const
{
    print_indent(indent);
    outln("NumericLiteral {}", m_value);
}

void BigIntLiteral::dump(int indent) const
{
    print_indent(indent);
    outln("BigIntLiteral {}", m_value);
}

void BooleanLiteral::dump(int indent) const
{
    print_indent(indent);
    outln("BooleanLiteral {}", m_value);
}

void NullLiteral::dump(int indent) const
{
    print_indent(indent);
    outln("null");
}

bool BindingPattern::contains_expression() const
{
    for (auto& entry : entries) {
        if (entry.initializer)
            return true;
        if (auto binding_ptr = entry.alias.get_pointer<NonnullRefPtr<BindingPattern const>>(); binding_ptr && (*binding_ptr)->contains_expression())
            return true;
    }
    return false;
}

ThrowCompletionOr<void> BindingPattern::for_each_bound_identifier(ThrowCompletionOrVoidCallback<Identifier const&>&& callback) const
{
    for (auto const& entry : entries) {
        auto const& alias = entry.alias;
        if (alias.has<NonnullRefPtr<Identifier const>>()) {
            TRY(callback(alias.get<NonnullRefPtr<Identifier const>>()));
        } else if (alias.has<NonnullRefPtr<BindingPattern const>>()) {
            TRY(alias.get<NonnullRefPtr<BindingPattern const>>()->for_each_bound_identifier(forward<decltype(callback)>(callback)));
        } else {
            auto const& name = entry.name;
            if (name.has<NonnullRefPtr<Identifier const>>())
                TRY(callback(name.get<NonnullRefPtr<Identifier const>>()));
        }
    }
    return {};
}

void BindingPattern::dump(int indent) const
{
    print_indent(indent);
    outln("BindingPattern {}", kind == Kind::Array ? "Array" : "Object");

    for (auto& entry : entries) {
        print_indent(indent + 1);
        outln("(Property)");

        if (kind == Kind::Object) {
            print_indent(indent + 2);
            outln("(Identifier)");
            if (entry.name.has<NonnullRefPtr<Identifier const>>()) {
                entry.name.get<NonnullRefPtr<Identifier const>>()->dump(indent + 3);
            } else if (entry.name.has<NonnullRefPtr<Expression const>>()) {
                entry.name.get<NonnullRefPtr<Expression const>>()->dump(indent + 3);
            } else {
                VERIFY(entry.name.has<Empty>());
                print_indent(indent + 3);
                outln("<empty>");
            }
        } else if (entry.is_elision()) {
            print_indent(indent + 2);
            outln("(Elision)");
            continue;
        }

        print_indent(indent + 2);
        outln("(Pattern{})", entry.is_rest ? " rest=true" : "");
        if (entry.alias.has<NonnullRefPtr<Identifier const>>()) {
            entry.alias.get<NonnullRefPtr<Identifier const>>()->dump(indent + 3);
        } else if (entry.alias.has<NonnullRefPtr<BindingPattern const>>()) {
            entry.alias.get<NonnullRefPtr<BindingPattern const>>()->dump(indent + 3);
        } else if (entry.alias.has<NonnullRefPtr<MemberExpression const>>()) {
            entry.alias.get<NonnullRefPtr<MemberExpression const>>()->dump(indent + 3);
        } else {
            print_indent(indent + 3);
            outln("<empty>");
        }

        if (entry.initializer) {
            print_indent(indent + 2);
            outln("(Initializer)");
            entry.initializer->dump(indent + 3);
        }
    }
}

void FunctionNode::dump(int indent, DeprecatedString const& class_name) const
{
    print_indent(indent);
    auto is_async = m_kind == FunctionKind::Async || m_kind == FunctionKind::AsyncGenerator;
    auto is_generator = m_kind == FunctionKind::Generator || m_kind == FunctionKind::AsyncGenerator;
    outln("{}{}{} '{}'", class_name, is_async ? " async" : "", is_generator ? "*" : "", name());
    if (m_contains_direct_call_to_eval) {
        print_indent(indent + 1);
        outln("\033[31;1m(direct eval)\033[0m");
    }
    if (!m_parameters.is_empty()) {
        print_indent(indent + 1);
        outln("(Parameters)");

        for (auto& parameter : m_parameters) {
            parameter.binding.visit(
                [&](Identifier const& identifier) {
                    if (parameter.is_rest) {
                        print_indent(indent + 2);
                        out("...");
                        identifier.dump(0);
                    } else {
                        identifier.dump(indent + 2);
                    }
                },
                [&](BindingPattern const& pattern) {
                    pattern.dump(indent + 2);
                });
            if (parameter.default_value)
                parameter.default_value->dump(indent + 3);
        }
    }
    print_indent(indent + 1);
    outln("(Body)");
    body().dump(indent + 2);
}

void FunctionDeclaration::dump(int indent) const
{
    FunctionNode::dump(indent, class_name());
}

ThrowCompletionOr<void> FunctionDeclaration::for_each_bound_identifier(ThrowCompletionOrVoidCallback<Identifier const&>&& callback) const
{
    if (!m_name)
        return {};
    return callback(*m_name);
}

void FunctionExpression::dump(int indent) const
{
    FunctionNode::dump(indent, class_name());
}

void YieldExpression::dump(int indent) const
{
    ASTNode::dump(indent);
    if (argument())
        argument()->dump(indent + 1);
}

void AwaitExpression::dump(int indent) const
{
    ASTNode::dump(indent);
    m_argument->dump(indent + 1);
}

void ReturnStatement::dump(int indent) const
{
    ASTNode::dump(indent);
    if (argument())
        argument()->dump(indent + 1);
}

void IfStatement::dump(int indent) const
{
    ASTNode::dump(indent);

    print_indent(indent);
    outln("If");
    predicate().dump(indent + 1);
    consequent().dump(indent + 1);
    if (alternate()) {
        print_indent(indent);
        outln("Else");
        alternate()->dump(indent + 1);
    }
}

void WhileStatement::dump(int indent) const
{
    ASTNode::dump(indent);

    print_indent(indent);
    outln("While");
    test().dump(indent + 1);
    body().dump(indent + 1);
}

void WithStatement::dump(int indent) const
{
    ASTNode::dump(indent);

    print_indent(indent + 1);
    outln("Object");
    object().dump(indent + 2);
    print_indent(indent + 1);
    outln("Body");
    body().dump(indent + 2);
}

void DoWhileStatement::dump(int indent) const
{
    ASTNode::dump(indent);

    print_indent(indent);
    outln("DoWhile");
    test().dump(indent + 1);
    body().dump(indent + 1);
}

void ForStatement::dump(int indent) const
{
    ASTNode::dump(indent);

    print_indent(indent);
    outln("For");
    if (init())
        init()->dump(indent + 1);
    if (test())
        test()->dump(indent + 1);
    if (update())
        update()->dump(indent + 1);
    body().dump(indent + 1);
}

void ForInStatement::dump(int indent) const
{
    ASTNode::dump(indent);

    print_indent(indent);
    outln("ForIn");
    lhs().visit([&](auto& lhs) { lhs->dump(indent + 1); });
    rhs().dump(indent + 1);
    body().dump(indent + 1);
}

void ForOfStatement::dump(int indent) const
{
    ASTNode::dump(indent);

    print_indent(indent);
    outln("ForOf");
    lhs().visit([&](auto& lhs) { lhs->dump(indent + 1); });
    rhs().dump(indent + 1);
    body().dump(indent + 1);
}

void ForAwaitOfStatement::dump(int indent) const
{
    ASTNode::dump(indent);

    print_indent(indent);
    outln("ForAwaitOf");
    m_lhs.visit([&](auto& lhs) { lhs->dump(indent + 1); });
    m_rhs->dump(indent + 1);
    m_body->dump(indent + 1);
}

// 13.1.3 Runtime Semantics: Evaluation, https://tc39.es/ecma262/#sec-identifiers-runtime-semantics-evaluation
Completion Identifier::execute(Interpreter& interpreter) const
{
    InterpreterNodeScope node_scope { interpreter, *this };
    auto& vm = interpreter.vm();

    // 1. Return ? ResolveBinding(StringValue of Identifier).
    // OPTIMIZATION: We call Identifier::to_reference() here, which acts as a caching layer around ResolveBinding.
    auto reference = TRY(to_reference(interpreter));

    // NOTE: The spec wants us to return the reference directly; this is not possible with ASTNode::execute() (short of letting it return a variant).
    // So, instead of calling GetValue at the call site, we do it here.
    return TRY(reference.get_value(vm));
}

void Identifier::dump(int indent) const
{
    print_indent(indent);
    if (is_local()) {
        outln("Identifier \"{}\" is_local=(true) index=({})", m_string, m_local_variable_index);
    } else if (is_global()) {
        outln("Identifier \"{}\" is_global=(true)", m_string);
    } else {
        outln("Identifier \"{}\"", m_string);
    }
}

Completion PrivateIdentifier::execute(Interpreter&) const
{
    // Note: This should be handled by either the member expression this is part of
    //       or the binary expression in the case of `#foo in bar`.
    VERIFY_NOT_REACHED();
}

void PrivateIdentifier::dump(int indent) const
{
    print_indent(indent);
    outln("PrivateIdentifier \"{}\"", m_string);
}

void SpreadExpression::dump(int indent) const
{
    ASTNode::dump(indent);
    m_target->dump(indent + 1);
}

Completion SpreadExpression::execute(Interpreter& interpreter) const
{
    InterpreterNodeScope node_scope { interpreter, *this };
    return m_target->execute(interpreter);
}

// 13.2.1.1 Runtime Semantics: Evaluation, https://tc39.es/ecma262/#sec-this-keyword-runtime-semantics-evaluation
Completion ThisExpression::execute(Interpreter& interpreter) const
{
    InterpreterNodeScope node_scope { interpreter, *this };
    auto& vm = interpreter.vm();

    // 1. Return ? ResolveThisBinding().
    return vm.resolve_this_binding();
}

void ThisExpression::dump(int indent) const
{
    ASTNode::dump(indent);
}

// 13.15.2 Runtime Semantics: Evaluation, https://tc39.es/ecma262/#sec-assignment-operators-runtime-semantics-evaluation
Completion AssignmentExpression::execute(Interpreter& interpreter) const
{
    InterpreterNodeScope node_scope { interpreter, *this };
    auto& vm = interpreter.vm();

    if (m_op == AssignmentOp::Assignment) {
        // AssignmentExpression : LeftHandSideExpression = AssignmentExpression
        return m_lhs.visit(
            // 1. If LeftHandSideExpression is neither an ObjectLiteral nor an ArrayLiteral, then
            [&](NonnullRefPtr<Expression const> const& lhs) -> ThrowCompletionOr<Value> {
                // a. Let lref be the result of evaluating LeftHandSideExpression.
                // b. ReturnIfAbrupt(lref).
                auto reference = TRY(lhs->to_reference(interpreter));

                Value rhs_result;

                // c. If IsAnonymousFunctionDefinition(AssignmentExpression) and IsIdentifierRef of LeftHandSideExpression are both true, then
                if (lhs->is_identifier()) {
                    // i. Let rval be ? NamedEvaluation of AssignmentExpression with argument lref.[[ReferencedName]].
                    auto& identifier_name = static_cast<Identifier const&>(*lhs).string();
                    rhs_result = TRY(vm.named_evaluation_if_anonymous_function(m_rhs, identifier_name));
                }
                // d. Else,
                else {
                    // i. Let rref be the result of evaluating AssignmentExpression.
                    // ii. Let rval be ? GetValue(rref).
                    rhs_result = TRY(m_rhs->execute(interpreter)).release_value();
                }

                // e. Perform ? PutValue(lref, rval).
                TRY(reference.put_value(vm, rhs_result));

                // f. Return rval.
                return rhs_result;
            },
            // 2. Let assignmentPattern be the AssignmentPattern that is covered by LeftHandSideExpression.
            [&](NonnullRefPtr<BindingPattern const> const& pattern) -> ThrowCompletionOr<Value> {
                // 3. Let rref be the result of evaluating AssignmentExpression.
                // 4. Let rval be ? GetValue(rref).
                auto rhs_result = TRY(m_rhs->execute(interpreter)).release_value();

                // 5. Perform ? DestructuringAssignmentEvaluation of assignmentPattern with argument rval.
                TRY(vm.destructuring_assignment_evaluation(pattern, rhs_result));

                // 6. Return rval.
                return rhs_result;
            });
    }
    VERIFY(m_lhs.has<NonnullRefPtr<Expression const>>());

    // 1. Let lref be the result of evaluating LeftHandSideExpression.
    auto& lhs_expression = *m_lhs.get<NonnullRefPtr<Expression const>>();
    auto reference = TRY(lhs_expression.to_reference(interpreter));

    // 2. Let lval be ? GetValue(lref).
    auto lhs_result = TRY(reference.get_value(vm));

    //  AssignmentExpression : LeftHandSideExpression {&&=, ||=, ??=} AssignmentExpression
    if (m_op == AssignmentOp::AndAssignment || m_op == AssignmentOp::OrAssignment || m_op == AssignmentOp::NullishAssignment) {
        switch (m_op) {
        // AssignmentExpression : LeftHandSideExpression &&= AssignmentExpression
        case AssignmentOp::AndAssignment:
            // 3. Let lbool be ToBoolean(lval).
            // 4. If lbool is false, return lval.
            if (!lhs_result.to_boolean())
                return lhs_result;
            break;

        // AssignmentExpression : LeftHandSideExpression ||= AssignmentExpression
        case AssignmentOp::OrAssignment:
            // 3. Let lbool be ToBoolean(lval).
            // 4. If lbool is true, return lval.
            if (lhs_result.to_boolean())
                return lhs_result;
            break;

        // AssignmentExpression : LeftHandSideExpression ??= AssignmentExpression
        case AssignmentOp::NullishAssignment:
            // 3. If lval is neither undefined nor null, return lval.
            if (!lhs_result.is_nullish())
                return lhs_result;
            break;

        default:
            VERIFY_NOT_REACHED();
        }

        Value rhs_result;

        // 5. If IsAnonymousFunctionDefinition(AssignmentExpression) is true and IsIdentifierRef of LeftHandSideExpression is true, then
        if (lhs_expression.is_identifier()) {
            // a. Let rval be ? NamedEvaluation of AssignmentExpression with argument lref.[[ReferencedName]].
            auto& identifier_name = static_cast<Identifier const&>(lhs_expression).string();
            rhs_result = TRY(interpreter.vm().named_evaluation_if_anonymous_function(m_rhs, identifier_name));
        }
        // 6. Else,
        else {
            // a. Let rref be the result of evaluating AssignmentExpression.
            // b. Let rval be ? GetValue(rref).
            rhs_result = TRY(m_rhs->execute(interpreter)).release_value();
        }

        // 7. Perform ? PutValue(lref, rval).
        TRY(reference.put_value(vm, rhs_result));

        // 8. Return rval.
        return rhs_result;
    }

    // AssignmentExpression : LeftHandSideExpression AssignmentOperator AssignmentExpression

    // 3. Let rref be the result of evaluating AssignmentExpression.
    // 4. Let rval be ? GetValue(rref).
    auto rhs_result = TRY(m_rhs->execute(interpreter)).release_value();

    // 5. Let assignmentOpText be the source text matched by AssignmentOperator.
    // 6. Let opText be the sequence of Unicode code points associated with assignmentOpText in the following table:
    // 7. Let r be ? ApplyStringOrNumericBinaryOperator(lval, opText, rval).
    switch (m_op) {
    case AssignmentOp::AdditionAssignment:
        rhs_result = TRY(add(vm, lhs_result, rhs_result));
        break;
    case AssignmentOp::SubtractionAssignment:
        rhs_result = TRY(sub(vm, lhs_result, rhs_result));
        break;
    case AssignmentOp::MultiplicationAssignment:
        rhs_result = TRY(mul(vm, lhs_result, rhs_result));
        break;
    case AssignmentOp::DivisionAssignment:
        rhs_result = TRY(div(vm, lhs_result, rhs_result));
        break;
    case AssignmentOp::ModuloAssignment:
        rhs_result = TRY(mod(vm, lhs_result, rhs_result));
        break;
    case AssignmentOp::ExponentiationAssignment:
        rhs_result = TRY(exp(vm, lhs_result, rhs_result));
        break;
    case AssignmentOp::BitwiseAndAssignment:
        rhs_result = TRY(bitwise_and(vm, lhs_result, rhs_result));
        break;
    case AssignmentOp::BitwiseOrAssignment:
        rhs_result = TRY(bitwise_or(vm, lhs_result, rhs_result));
        break;
    case AssignmentOp::BitwiseXorAssignment:
        rhs_result = TRY(bitwise_xor(vm, lhs_result, rhs_result));
        break;
    case AssignmentOp::LeftShiftAssignment:
        rhs_result = TRY(left_shift(vm, lhs_result, rhs_result));
        break;
    case AssignmentOp::RightShiftAssignment:
        rhs_result = TRY(right_shift(vm, lhs_result, rhs_result));
        break;
    case AssignmentOp::UnsignedRightShiftAssignment:
        rhs_result = TRY(unsigned_right_shift(vm, lhs_result, rhs_result));
        break;
    case AssignmentOp::Assignment:
    case AssignmentOp::AndAssignment:
    case AssignmentOp::OrAssignment:
    case AssignmentOp::NullishAssignment:
        VERIFY_NOT_REACHED();
    }

    // 8. Perform ? PutValue(lref, r).
    TRY(reference.put_value(vm, rhs_result));

    // 9. Return r.
    return rhs_result;
}

// 13.4.2.1 Runtime Semantics: Evaluation, https://tc39.es/ecma262/#sec-postfix-increment-operator-runtime-semantics-evaluation
// 13.4.3.1 Runtime Semantics: Evaluation, https://tc39.es/ecma262/#sec-postfix-decrement-operator-runtime-semantics-evaluation
// 13.4.4.1 Runtime Semantics: Evaluation, https://tc39.es/ecma262/#sec-prefix-increment-operator-runtime-semantics-evaluation
// 13.4.5.1 Runtime Semantics: Evaluation, https://tc39.es/ecma262/#sec-prefix-decrement-operator-runtime-semantics-evaluation
Completion UpdateExpression::execute(Interpreter& interpreter) const
{
    InterpreterNodeScope node_scope { interpreter, *this };
    auto& vm = interpreter.vm();

    // 1. Let expr be the result of evaluating <Expression>.
    auto reference = TRY(m_argument->to_reference(interpreter));

    // 2. Let oldValue be ? ToNumeric(? GetValue(expr)).
    auto old_value = TRY(reference.get_value(vm));
    old_value = TRY(old_value.to_numeric(vm));

    Value new_value;
    switch (m_op) {
    case UpdateOp::Increment:
        // 3. If Type(oldValue) is Number, then
        if (old_value.is_number()) {
            // a. Let newValue be Number::add(oldValue, 1𝔽).
            new_value = Value(old_value.as_double() + 1);
        }
        // 4. Else,
        else {
            // a. Assert: Type(oldValue) is BigInt.
            // b. Let newValue be BigInt::add(oldValue, 1ℤ).
            new_value = BigInt::create(vm, old_value.as_bigint().big_integer().plus(Crypto::SignedBigInteger { 1 }));
        }
        break;
    case UpdateOp::Decrement:
        // 3. If Type(oldValue) is Number, then
        if (old_value.is_number()) {
            // a. Let newValue be Number::subtract(oldValue, 1𝔽).
            new_value = Value(old_value.as_double() - 1);
        }
        // 4. Else,
        else {
            // a. Assert: Type(oldValue) is BigInt.
            // b. Let newValue be BigInt::subtract(oldValue, 1ℤ).
            new_value = BigInt::create(vm, old_value.as_bigint().big_integer().minus(Crypto::SignedBigInteger { 1 }));
        }
        break;
    default:
        VERIFY_NOT_REACHED();
    }

    // 5. Perform ? PutValue(expr, newValue).
    TRY(reference.put_value(vm, new_value));

    // 6. Return newValue.
    // 6. Return oldValue.
    return m_prefixed ? new_value : old_value;
}

void AssignmentExpression::dump(int indent) const
{
    char const* op_string = nullptr;
    switch (m_op) {
    case AssignmentOp::Assignment:
        op_string = "=";
        break;
    case AssignmentOp::AdditionAssignment:
        op_string = "+=";
        break;
    case AssignmentOp::SubtractionAssignment:
        op_string = "-=";
        break;
    case AssignmentOp::MultiplicationAssignment:
        op_string = "*=";
        break;
    case AssignmentOp::DivisionAssignment:
        op_string = "/=";
        break;
    case AssignmentOp::ModuloAssignment:
        op_string = "%=";
        break;
    case AssignmentOp::ExponentiationAssignment:
        op_string = "**=";
        break;
    case AssignmentOp::BitwiseAndAssignment:
        op_string = "&=";
        break;
    case AssignmentOp::BitwiseOrAssignment:
        op_string = "|=";
        break;
    case AssignmentOp::BitwiseXorAssignment:
        op_string = "^=";
        break;
    case AssignmentOp::LeftShiftAssignment:
        op_string = "<<=";
        break;
    case AssignmentOp::RightShiftAssignment:
        op_string = ">>=";
        break;
    case AssignmentOp::UnsignedRightShiftAssignment:
        op_string = ">>>=";
        break;
    case AssignmentOp::AndAssignment:
        op_string = "&&=";
        break;
    case AssignmentOp::OrAssignment:
        op_string = "||=";
        break;
    case AssignmentOp::NullishAssignment:
        op_string = "\?\?=";
        break;
    }

    ASTNode::dump(indent);
    print_indent(indent + 1);
    outln("{}", op_string);
    m_lhs.visit([&](auto& lhs) { lhs->dump(indent + 1); });
    m_rhs->dump(indent + 1);
}

void UpdateExpression::dump(int indent) const
{
    char const* op_string = nullptr;
    switch (m_op) {
    case UpdateOp::Increment:
        op_string = "++";
        break;
    case UpdateOp::Decrement:
        op_string = "--";
        break;
    }

    ASTNode::dump(indent);
    if (m_prefixed) {
        print_indent(indent + 1);
        outln("{}", op_string);
    }
    m_argument->dump(indent + 1);
    if (!m_prefixed) {
        print_indent(indent + 1);
        outln("{}", op_string);
    }
}

// 14.3.1.2 Runtime Semantics: Evaluation, https://tc39.es/ecma262/#sec-let-and-const-declarations-runtime-semantics-evaluation
// 14.3.2.1 Runtime Semantics: Evaluation, https://tc39.es/ecma262/#sec-variable-statement-runtime-semantics-evaluation
Completion VariableDeclaration::execute(Interpreter& interpreter) const
{
    InterpreterNodeScope node_scope { interpreter, *this };
    auto& vm = interpreter.vm();

    for (auto& declarator : m_declarations) {
        if (auto* init = declarator->init()) {
            TRY(declarator->target().visit(
                [&](NonnullRefPtr<Identifier const> const& id) -> ThrowCompletionOr<void> {
                    auto reference = TRY(id->to_reference(interpreter));
                    auto initializer_result = TRY(interpreter.vm().named_evaluation_if_anonymous_function(*init, id->string()));
                    VERIFY(!initializer_result.is_empty());

                    if (m_declaration_kind == DeclarationKind::Var)
                        return reference.put_value(vm, initializer_result);
                    else
                        return reference.initialize_referenced_binding(vm, initializer_result);
                },
                [&](NonnullRefPtr<BindingPattern const> const& pattern) -> ThrowCompletionOr<void> {
                    auto initializer_result = TRY(init->execute(interpreter)).release_value();

                    Environment* environment = m_declaration_kind == DeclarationKind::Var ? nullptr : interpreter.lexical_environment();

                    return vm.binding_initialization(pattern, initializer_result, environment);
                }));
        } else if (m_declaration_kind != DeclarationKind::Var) {
            VERIFY(declarator->target().has<NonnullRefPtr<Identifier const>>());
            auto& identifier = declarator->target().get<NonnullRefPtr<Identifier const>>();
            auto reference = TRY(identifier->to_reference(interpreter));
            TRY(reference.initialize_referenced_binding(vm, js_undefined()));
        }
    }
    return normal_completion({});
}

Completion VariableDeclarator::execute(Interpreter& interpreter) const
{
    InterpreterNodeScope node_scope { interpreter, *this };

    // NOTE: VariableDeclarator execution is handled by VariableDeclaration.
    VERIFY_NOT_REACHED();
}

ThrowCompletionOr<void> VariableDeclaration::for_each_bound_identifier(ThrowCompletionOrVoidCallback<Identifier const&>&& callback) const
{
    for (auto const& entry : declarations()) {
        TRY(entry->target().visit(
            [&](NonnullRefPtr<Identifier const> const& id) {
                return callback(id);
            },
            [&](NonnullRefPtr<BindingPattern const> const& binding) {
                return binding->for_each_bound_identifier([&](auto const& id) {
                    return callback(id);
                });
            }));
    }

    return {};
}

void VariableDeclaration::dump(int indent) const
{
    char const* declaration_kind_string = nullptr;
    switch (m_declaration_kind) {
    case DeclarationKind::Let:
        declaration_kind_string = "Let";
        break;
    case DeclarationKind::Var:
        declaration_kind_string = "Var";
        break;
    case DeclarationKind::Const:
        declaration_kind_string = "Const";
        break;
    }

    ASTNode::dump(indent);
    print_indent(indent + 1);
    outln("{}", declaration_kind_string);

    for (auto& declarator : m_declarations)
        declarator->dump(indent + 1);
}

// 6.2.1.2 Runtime Semantics: Evaluation, https://tc39.es/proposal-explicit-resource-management/#sec-let-and-const-declarations-runtime-semantics-evaluation
Completion UsingDeclaration::execute(Interpreter& interpreter) const
{
    // 1. Let next be BindingEvaluation of BindingList with parameter sync-dispose.
    InterpreterNodeScope node_scope { interpreter, *this };
    auto& vm = interpreter.vm();

    for (auto& declarator : m_declarations) {
        VERIFY(declarator->target().has<NonnullRefPtr<Identifier const>>());
        VERIFY(declarator->init());

        auto& id = declarator->target().get<NonnullRefPtr<Identifier const>>();

        // 2. ReturnIfAbrupt(next).
        auto reference = TRY(id->to_reference(interpreter));
        auto initializer_result = TRY(interpreter.vm().named_evaluation_if_anonymous_function(*declarator->init(), id->string()));
        VERIFY(!initializer_result.is_empty());
        TRY(reference.initialize_referenced_binding(vm, initializer_result, Environment::InitializeBindingHint::SyncDispose));
    }

    // 3. Return empty.
    return normal_completion({});
}

ThrowCompletionOr<void> UsingDeclaration::for_each_bound_identifier(ThrowCompletionOrVoidCallback<Identifier const&>&& callback) const
{
    for (auto const& entry : m_declarations) {
        VERIFY(entry->target().has<NonnullRefPtr<Identifier const>>());
        TRY(callback(entry->target().get<NonnullRefPtr<Identifier const>>()));
    }

    return {};
}

void UsingDeclaration::dump(int indent) const
{
    ASTNode::dump(indent);
    print_indent(indent + 1);
    for (auto& declarator : m_declarations)
        declarator->dump(indent + 1);
}

void VariableDeclarator::dump(int indent) const
{
    ASTNode::dump(indent);
    m_target.visit([indent](auto const& value) { value->dump(indent + 1); });
    if (m_init)
        m_init->dump(indent + 1);
}

void ObjectProperty::dump(int indent) const
{
    ASTNode::dump(indent);

    if (m_property_type == Type::Spread) {
        print_indent(indent + 1);
        outln("...Spreading");
        m_key->dump(indent + 1);
    } else {
        m_key->dump(indent + 1);
        m_value->dump(indent + 1);
    }
}

void ObjectExpression::dump(int indent) const
{
    ASTNode::dump(indent);
    for (auto& property : m_properties) {
        property->dump(indent + 1);
    }
}

void ExpressionStatement::dump(int indent) const
{
    ASTNode::dump(indent);
    m_expression->dump(indent + 1);
}

Completion ObjectProperty::execute(Interpreter& interpreter) const
{
    InterpreterNodeScope node_scope { interpreter, *this };

    // NOTE: ObjectProperty execution is handled by ObjectExpression.
    VERIFY_NOT_REACHED();
}

// 13.2.5.4 Runtime Semantics: Evaluation, https://tc39.es/ecma262/#sec-object-initializer-runtime-semantics-evaluation
Completion ObjectExpression::execute(Interpreter& interpreter) const
{
    InterpreterNodeScope node_scope { interpreter, *this };
    auto& vm = interpreter.vm();
    auto& realm = *vm.current_realm();

    // 1. Let obj be OrdinaryObjectCreate(%Object.prototype%).
    auto object = Object::create(realm, realm.intrinsics().object_prototype());

    // 2. Perform ? PropertyDefinitionEvaluation of PropertyDefinitionList with argument obj.
    for (auto& property : m_properties) {
        auto key = TRY(property->key().execute(interpreter)).release_value();

        // PropertyDefinition : ... AssignmentExpression
        if (property->type() == ObjectProperty::Type::Spread) {
            // 4. Perform ? CopyDataProperties(object, fromValue, excludedNames).
            TRY(object->copy_data_properties(vm, key, {}));

            // 5. Return unused.
            continue;
        }

        auto value = TRY(property->value().execute(interpreter)).release_value();

        // 8. If isProtoSetter is true, then
        if (property->type() == ObjectProperty::Type::ProtoSetter) {
            // a. If Type(propValue) is either Object or Null, then
            if (value.is_object() || value.is_null()) {
                // i. Perform ! object.[[SetPrototypeOf]](propValue).
                MUST(object->internal_set_prototype_of(value.is_object() ? &value.as_object() : nullptr));
            }
            // b. Return unused.
            continue;
        }

        auto property_key = TRY(PropertyKey::from_value(vm, key));

        if (property->is_method()) {
            VERIFY(value.is_function());
            static_cast<ECMAScriptFunctionObject&>(value.as_function()).set_home_object(object);

            auto name = MUST(get_function_property_name(property_key));
            if (property->type() == ObjectProperty::Type::Getter) {
                name = DeprecatedString::formatted("get {}", name);
            } else if (property->type() == ObjectProperty::Type::Setter) {
                name = DeprecatedString::formatted("set {}", name);
            }

            update_function_name(value, name);
        }

        switch (property->type()) {
        case ObjectProperty::Type::Getter:
            VERIFY(value.is_function());
            object->define_direct_accessor(property_key, &value.as_function(), nullptr, Attribute::Configurable | Attribute::Enumerable);
            break;
        case ObjectProperty::Type::Setter:
            VERIFY(value.is_function());
            object->define_direct_accessor(property_key, nullptr, &value.as_function(), Attribute::Configurable | Attribute::Enumerable);
            break;
        case ObjectProperty::Type::KeyValue:
            object->define_direct_property(property_key, value, default_attributes);
            break;
        case ObjectProperty::Type::Spread:
        default:
            VERIFY_NOT_REACHED();
        }
    }

    // 3. Return obj.
    return Value { object };
}

void MemberExpression::dump(int indent) const
{
    print_indent(indent);
    outln("{}(computed={})", class_name(), is_computed());
    m_object->dump(indent + 1);
    m_property->dump(indent + 1);
}

DeprecatedString MemberExpression::to_string_approximation() const
{
    DeprecatedString object_string = "<object>";
    if (is<Identifier>(*m_object))
        object_string = static_cast<Identifier const&>(*m_object).string();
    if (is_computed())
        return DeprecatedString::formatted("{}[<computed>]", object_string);
    if (is<PrivateIdentifier>(*m_property))
        return DeprecatedString::formatted("{}.{}", object_string, verify_cast<PrivateIdentifier>(*m_property).string());
    return DeprecatedString::formatted("{}.{}", object_string, verify_cast<Identifier>(*m_property).string());
}

// 13.3.2.1 Runtime Semantics: Evaluation, https://tc39.es/ecma262/#sec-property-accessors-runtime-semantics-evaluation
Completion MemberExpression::execute(Interpreter& interpreter) const
{
    InterpreterNodeScope node_scope { interpreter, *this };
    auto& vm = interpreter.vm();

    auto reference = TRY(to_reference(interpreter));
    return TRY(reference.get_value(vm));
}

bool MemberExpression::ends_in_private_name() const
{
    if (is_computed())
        return false;
    if (is<PrivateIdentifier>(*m_property))
        return true;
    if (is<MemberExpression>(*m_property))
        return static_cast<MemberExpression const&>(*m_property).ends_in_private_name();
    return false;
}

void OptionalChain::dump(int indent) const
{
    print_indent(indent);
    outln("{}", class_name());
    m_base->dump(indent + 1);
    for (auto& reference : m_references) {
        reference.visit(
            [&](Call const& call) {
                print_indent(indent + 1);
                outln("Call({})", call.mode == Mode::Optional ? "Optional" : "Not Optional");
                for (auto& argument : call.arguments)
                    argument.value->dump(indent + 2);
            },
            [&](ComputedReference const& ref) {
                print_indent(indent + 1);
                outln("ComputedReference({})", ref.mode == Mode::Optional ? "Optional" : "Not Optional");
                ref.expression->dump(indent + 2);
            },
            [&](MemberReference const& ref) {
                print_indent(indent + 1);
                outln("MemberReference({})", ref.mode == Mode::Optional ? "Optional" : "Not Optional");
                ref.identifier->dump(indent + 2);
            },
            [&](PrivateMemberReference const& ref) {
                print_indent(indent + 1);
                outln("PrivateMemberReference({})", ref.mode == Mode::Optional ? "Optional" : "Not Optional");
                ref.private_identifier->dump(indent + 2);
            });
    }
}

ThrowCompletionOr<OptionalChain::ReferenceAndValue> OptionalChain::to_reference_and_value(Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();

    auto base_reference = TRY(m_base->to_reference(interpreter));
    auto base = base_reference.is_unresolvable()
        ? TRY(m_base->execute(interpreter)).release_value()
        : TRY(base_reference.get_value(vm));

    for (auto& reference : m_references) {
        auto is_optional = reference.visit([](auto& ref) { return ref.mode; }) == Mode::Optional;
        if (is_optional && base.is_nullish())
            return ReferenceAndValue { {}, js_undefined() };

        auto expression = reference.visit(
            [&](Call const& call) -> NonnullRefPtr<Expression const> {
                return CallExpression::create(source_range(),
                    create_ast_node<SyntheticReferenceExpression>(source_range(), base_reference, base),
                    call.arguments, InvocationStyleEnum::Parenthesized, InsideParenthesesEnum::NotInsideParentheses);
            },
            [&](ComputedReference const& ref) -> NonnullRefPtr<Expression const> {
                return create_ast_node<MemberExpression>(source_range(),
                    create_ast_node<SyntheticReferenceExpression>(source_range(), base_reference, base),
                    ref.expression,
                    true);
            },
            [&](MemberReference const& ref) -> NonnullRefPtr<Expression const> {
                return create_ast_node<MemberExpression>(source_range(),
                    create_ast_node<SyntheticReferenceExpression>(source_range(), base_reference, base),
                    ref.identifier,
                    false);
            },
            [&](PrivateMemberReference const& ref) -> NonnullRefPtr<Expression const> {
                return create_ast_node<MemberExpression>(source_range(),
                    create_ast_node<SyntheticReferenceExpression>(source_range(), base_reference, base),
                    ref.private_identifier,
                    false);
            });
        if (is<CallExpression>(*expression)) {
            base_reference = JS::Reference {};
            base = TRY(expression->execute(interpreter)).release_value();
        } else {
            base_reference = TRY(expression->to_reference(interpreter));
            base = TRY(base_reference.get_value(vm));
        }
    }

    return ReferenceAndValue { move(base_reference), base };
}

// 13.3.9.1 Runtime Semantics: Evaluation, https://tc39.es/ecma262/#sec-optional-chaining-evaluation
Completion OptionalChain::execute(Interpreter& interpreter) const
{
    InterpreterNodeScope node_scope { interpreter, *this };
    return TRY(to_reference_and_value(interpreter)).value;
}

ThrowCompletionOr<JS::Reference> OptionalChain::to_reference(Interpreter& interpreter) const
{
    return TRY(to_reference_and_value(interpreter)).reference;
}

void MetaProperty::dump(int indent) const
{
    DeprecatedString name;
    if (m_type == MetaProperty::Type::NewTarget)
        name = "new.target";
    else if (m_type == MetaProperty::Type::ImportMeta)
        name = "import.meta";
    else
        VERIFY_NOT_REACHED();
    print_indent(indent);
    outln("{} {}", class_name(), name);
}

// 13.3.12.1 Runtime Semantics: Evaluation, https://tc39.es/ecma262/#sec-meta-properties-runtime-semantics-evaluation
Completion MetaProperty::execute(Interpreter& interpreter) const
{
    InterpreterNodeScope node_scope { interpreter, *this };
    auto& vm = interpreter.vm();

    // NewTarget : new . target
    if (m_type == MetaProperty::Type::NewTarget) {
        // 1. Return GetNewTarget().
        return interpreter.vm().get_new_target();
    }

    // ImportMeta : import . meta
    if (m_type == MetaProperty::Type::ImportMeta) {
        return Value(vm.get_import_meta());
    }

    VERIFY_NOT_REACHED();
}

void ImportCall::dump(int indent) const
{
    ASTNode::dump(indent);
    print_indent(indent);
    outln("(Specifier)");
    m_specifier->dump(indent + 1);
    if (m_options) {
        outln("(Options)");
        m_options->dump(indent + 1);
    }
}

// 13.3.10.1 Runtime Semantics: Evaluation, https://tc39.es/ecma262/#sec-import-call-runtime-semantics-evaluation
// Also includes assertions from proposal: https://tc39.es/proposal-import-assertions/#sec-import-call-runtime-semantics-evaluation
Completion ImportCall::execute(Interpreter& interpreter) const
{
    InterpreterNodeScope node_scope { interpreter, *this };
    auto& vm = interpreter.vm();

    // 2. Let specifierRef be the result of evaluating specifierExpression.
    // 3. Let specifier be ? GetValue(specifierRef).
    auto specifier = TRY(m_specifier->execute(interpreter));

    auto options_value = js_undefined();
    // 4. If optionsExpression is present, then
    if (m_options) {
        // a. Let optionsRef be the result of evaluating optionsExpression.
        // b. Let options be ? GetValue(optionsRef).
        options_value = TRY(m_options->execute(interpreter)).release_value();
    }
    // 5. Else,
    // a. Let options be undefined.
    // Note: options_value is undefined by default.

    return perform_import_call(vm, *specifier, options_value);
}

// 13.2.3.1 Runtime Semantics: Evaluation, https://tc39.es/ecma262/#sec-literals-runtime-semantics-evaluation
Completion StringLiteral::execute(Interpreter& interpreter) const
{
    InterpreterNodeScope node_scope { interpreter, *this };
    auto& vm = interpreter.vm();

    // 1. Return the SV of StringLiteral as defined in 12.8.4.2.
    return Value { PrimitiveString::create(vm, m_value) };
}

// 13.2.3.1 Runtime Semantics: Evaluation, https://tc39.es/ecma262/#sec-literals-runtime-semantics-evaluation
Completion NumericLiteral::execute(Interpreter& interpreter) const
{
    InterpreterNodeScope node_scope { interpreter, *this };

    // 1. Return the NumericValue of NumericLiteral as defined in 12.8.3.
    return Value(m_value);
}

// 13.2.3.1 Runtime Semantics: Evaluation, https://tc39.es/ecma262/#sec-literals-runtime-semantics-evaluation
Completion BigIntLiteral::execute(Interpreter& interpreter) const
{
    InterpreterNodeScope node_scope { interpreter, *this };
    auto& vm = interpreter.vm();

    // 1. Return the NumericValue of NumericLiteral as defined in 12.8.3.
    Crypto::SignedBigInteger integer;
    if (m_value[0] == '0' && m_value.length() >= 3) {
        if (m_value[1] == 'x' || m_value[1] == 'X') {
            return Value { BigInt::create(vm, Crypto::SignedBigInteger::from_base(16, m_value.substring(2, m_value.length() - 3))) };
        } else if (m_value[1] == 'o' || m_value[1] == 'O') {
            return Value { BigInt::create(vm, Crypto::SignedBigInteger::from_base(8, m_value.substring(2, m_value.length() - 3))) };
        } else if (m_value[1] == 'b' || m_value[1] == 'B') {
            return Value { BigInt::create(vm, Crypto::SignedBigInteger::from_base(2, m_value.substring(2, m_value.length() - 3))) };
        }
    }
    return Value { BigInt::create(vm, Crypto::SignedBigInteger::from_base(10, m_value.substring(0, m_value.length() - 1))) };
}

// 13.2.3.1 Runtime Semantics: Evaluation, https://tc39.es/ecma262/#sec-literals-runtime-semantics-evaluation
Completion BooleanLiteral::execute(Interpreter& interpreter) const
{
    InterpreterNodeScope node_scope { interpreter, *this };

    // 1. If BooleanLiteral is the token false, return false.
    // 2. If BooleanLiteral is the token true, return true.
    return Value(m_value);
}

// 13.2.3.1 Runtime Semantics: Evaluation, https://tc39.es/ecma262/#sec-literals-runtime-semantics-evaluation
Completion NullLiteral::execute(Interpreter& interpreter) const
{
    InterpreterNodeScope node_scope { interpreter, *this };

    // 1. Return null.
    return js_null();
}

void RegExpLiteral::dump(int indent) const
{
    print_indent(indent);
    outln("{} (/{}/{})", class_name(), pattern(), flags());
}

// 13.2.7.3 Runtime Semantics: Evaluation, https://tc39.es/ecma262/#sec-regular-expression-literals-runtime-semantics-evaluation
Completion RegExpLiteral::execute(Interpreter& interpreter) const
{
    InterpreterNodeScope node_scope { interpreter, *this };
    auto& vm = interpreter.vm();
    auto& realm = *vm.current_realm();

    // 1. Let pattern be CodePointsToString(BodyText of RegularExpressionLiteral).
    auto pattern = this->pattern();

    // 2. Let flags be CodePointsToString(FlagText of RegularExpressionLiteral).
    auto flags = this->flags();

    // 3. Return ! RegExpCreate(pattern, flags).
    Regex<ECMA262> regex(parsed_regex(), parsed_pattern(), parsed_flags());
    // NOTE: We bypass RegExpCreate and subsequently RegExpAlloc as an optimization to use the already parsed values.
    auto regexp_object = RegExpObject::create(realm, move(regex), move(pattern), move(flags));
    // RegExpAlloc has these two steps from the 'Legacy RegExp features' proposal.
    regexp_object->set_realm(*vm.current_realm());
    // We don't need to check 'If SameValue(newTarget, thisRealm.[[Intrinsics]].[[%RegExp%]]) is true'
    // here as we know RegExpCreate calls RegExpAlloc with %RegExp% for newTarget.
    regexp_object->set_legacy_features_enabled(true);
    return Value { regexp_object };
}

void ArrayExpression::dump(int indent) const
{
    ASTNode::dump(indent);
    for (auto& element : m_elements) {
        if (element) {
            element->dump(indent + 1);
        } else {
            print_indent(indent + 1);
            outln("<empty>");
        }
    }
}

// 13.2.4.2 Runtime Semantics: Evaluation, https://tc39.es/ecma262/#sec-array-initializer-runtime-semantics-evaluation
Completion ArrayExpression::execute(Interpreter& interpreter) const
{
    InterpreterNodeScope node_scope { interpreter, *this };
    auto& vm = interpreter.vm();
    auto& realm = *vm.current_realm();

    // 1. Let array be ! ArrayCreate(0).
    auto array = MUST(Array::create(realm, 0));

    // 2. Perform ? ArrayAccumulation of ElementList with arguments array and 0.

    array->indexed_properties();
    size_t index = 0;
    for (auto& element : m_elements) {
        auto value = Value();
        if (element) {
            value = TRY(element->execute(interpreter)).release_value();

            if (is<SpreadExpression>(*element)) {
                (void)TRY(get_iterator_values(vm, value, [&](Value iterator_value) -> Optional<Completion> {
                    array->indexed_properties().put(index++, iterator_value, default_attributes);
                    return {};
                }));
                continue;
            }
        }
        array->indexed_properties().put(index++, value, default_attributes);
    }

    // 3. Return array.
    return Value { array };
}

void TemplateLiteral::dump(int indent) const
{
    ASTNode::dump(indent);
    for (auto& expression : m_expressions)
        expression->dump(indent + 1);
}

// 13.2.8.5 Runtime Semantics: Evaluation, https://tc39.es/ecma262/#sec-template-literals-runtime-semantics-evaluation
Completion TemplateLiteral::execute(Interpreter& interpreter) const
{
    InterpreterNodeScope node_scope { interpreter, *this };
    auto& vm = interpreter.vm();

    StringBuilder string_builder;

    for (auto& expression : m_expressions) {
        // 1. Let head be the TV of TemplateHead as defined in 12.8.6.

        // 2. Let subRef be the result of evaluating Expression.
        // 3. Let sub be ? GetValue(subRef).
        auto sub = TRY(expression->execute(interpreter)).release_value();

        // 4. Let middle be ? ToString(sub).
        auto string = TRY(sub.to_deprecated_string(vm));
        string_builder.append(string);

        // 5. Let tail be the result of evaluating TemplateSpans.
        // 6. ReturnIfAbrupt(tail).
    }

    // 7. Return the string-concatenation of head, middle, and tail.
    return Value { PrimitiveString::create(vm, string_builder.to_deprecated_string()) };
}

void TaggedTemplateLiteral::dump(int indent) const
{
    ASTNode::dump(indent);
    print_indent(indent + 1);
    outln("(Tag)");
    m_tag->dump(indent + 2);
    print_indent(indent + 1);
    outln("(Template Literal)");
    m_template_literal->dump(indent + 2);
}

// 13.3.11.1 Runtime Semantics: Evaluation, https://tc39.es/ecma262/#sec-tagged-templates-runtime-semantics-evaluation
Completion TaggedTemplateLiteral::execute(Interpreter& interpreter) const
{
    InterpreterNodeScope node_scope { interpreter, *this };
    auto& vm = interpreter.vm();

    // NOTE: This is both
    //  MemberExpression : MemberExpression TemplateLiteral
    //  CallExpression : CallExpression TemplateLiteral

    // 1. Let tagRef be ? Evaluation of MemberExpression.
    // 1. Let tagRef be ? Evaluation of CallExpression.
    // 2. Let tagFunc be ? GetValue(tagRef).
    // NOTE: This is much more complicated than the spec because we have to
    //       handle every type of reference. If we handle evaluation closer
    //       to the spec this could be improved.
    Value tag_this_value;
    Value tag;
    if (auto tag_reference = TRY(m_tag->to_reference(interpreter)); tag_reference.is_valid_reference()) {
        tag = TRY(tag_reference.get_value(vm));
        if (tag_reference.is_environment_reference()) {
            auto& environment = tag_reference.base_environment();
            if (environment.has_this_binding())
                tag_this_value = TRY(environment.get_this_binding(vm));
            else
                tag_this_value = js_undefined();
        } else {
            tag_this_value = tag_reference.get_this_value();
        }
    } else {
        auto result = TRY(m_tag->execute(interpreter));
        VERIFY(result.has_value());
        tag = result.release_value();
        tag_this_value = js_undefined();
    }

    // 3. Let thisCall be this CallExpression.
    // 3. Let thisCall be this MemberExpression.
    // FIXME: 4. Let tailCall be IsInTailPosition(thisCall).

    // NOTE: A tagged template is a function call where the arguments of the call are derived from a
    //       TemplateLiteral (13.2.8). The actual arguments include a template object (13.2.8.3)
    //       and the values produced by evaluating the expressions embedded within the TemplateLiteral.
    auto template_ = TRY(get_template_object(interpreter));
    MarkedVector<Value> arguments(interpreter.vm().heap());
    arguments.append(template_);

    auto& expressions = m_template_literal->expressions();

    // tag`${foo}`             -> "", foo, ""                -> tag(["", ""], foo)
    // tag`foo${bar}baz${qux}` -> "foo", bar, "baz", qux, "" -> tag(["foo", "baz", ""], bar, qux)
    // So we want all the odd expressions
    for (size_t i = 1; i < expressions.size(); i += 2)
        arguments.append(TRY(expressions[i]->execute(interpreter)).release_value());

    // 5. Return ? EvaluateCall(tagFunc, tagRef, TemplateLiteral, tailCall).
    return call(vm, tag, tag_this_value, move(arguments));
}

// 13.2.8.3 GetTemplateObject ( templateLiteral ), https://tc39.es/ecma262/#sec-gettemplateobject
ThrowCompletionOr<Value> TaggedTemplateLiteral::get_template_object(Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();

    // 1. Let realm be the current Realm Record.
    auto& realm = *vm.current_realm();

    // 2. Let templateRegistry be realm.[[TemplateMap]].
    // 3. For each element e of templateRegistry, do
    //    a. If e.[[Site]] is the same Parse Node as templateLiteral, then
    //        i. Return e.[[Array]].
    // NOTE: Instead of caching on the realm we cache on the Parse Node side as
    //       this makes it easier to track whether it is the same parse node.
    if (auto cached_value_or_end = m_cached_values.find(&realm); cached_value_or_end != m_cached_values.end())
        return Value { cached_value_or_end->value.cell() };

    // 4. Let rawStrings be TemplateStrings of templateLiteral with argument true.
    auto& raw_strings = m_template_literal->raw_strings();

    // 5. Let cookedStrings be TemplateStrings of templateLiteral with argument false.
    auto& expressions = m_template_literal->expressions();

    // 6. Let count be the number of elements in the List cookedStrings.
    // NOTE: Only the even expression in expression are the cooked strings
    //       so we use rawStrings for the size here
    VERIFY(raw_strings.size() == (expressions.size() + 1) / 2);
    auto count = raw_strings.size();

    // 7. Assert: count ≤ 2^32 - 1.
    VERIFY(count <= 0xffffffff);

    // 8. Let template be ! ArrayCreate(count).
    // NOTE: We don't set count since we push the values using append which
    //       would then append after count. Same for 9.
    auto template_ = MUST(Array::create(realm, 0));

    // 9. Let rawObj be ! ArrayCreate(count).
    auto raw_obj = MUST(Array::create(realm, 0));

    // 10. Let index be 0.
    // 11. Repeat, while index < count,
    for (size_t i = 0; i < count; ++i) {
        auto cooked_string_index = i * 2;
        // a. Let prop be ! ToString(𝔽(index)).
        // b. Let cookedValue be cookedStrings[index].
        auto cooked_value = TRY(expressions[cooked_string_index]->execute(interpreter)).release_value();

        // NOTE: If the string contains invalid escapes we get a null expression here,
        //       which we then convert to the expected `undefined` TV. See
        //       12.9.6.1 Static Semantics: TV, https://tc39.es/ecma262/#sec-static-semantics-tv
        if (cooked_value.is_null())
            cooked_value = js_undefined();

        // c. Perform ! DefinePropertyOrThrow(template, prop, PropertyDescriptor { [[Value]]: cookedValue, [[Writable]]: false, [[Enumerable]]: true, [[Configurable]]: false }).
        template_->indexed_properties().append(cooked_value);

        // d. Let rawValue be the String value rawStrings[index].
        // e. Perform ! DefinePropertyOrThrow(rawObj, prop, PropertyDescriptor { [[Value]]: rawValue, [[Writable]]: false, [[Enumerable]]: true, [[Configurable]]: false }).
        raw_obj->indexed_properties().append(TRY(raw_strings[i]->execute(interpreter)).release_value());

        // f. Set index to index + 1.
    }

    // 12. Perform ! SetIntegrityLevel(rawObj, frozen).
    MUST(raw_obj->set_integrity_level(Object::IntegrityLevel::Frozen));

    // 13. Perform ! DefinePropertyOrThrow(template, "raw", PropertyDescriptor { [[Value]]: rawObj, [[Writable]]: false, [[Enumerable]]: false, [[Configurable]]: false }).
    template_->define_direct_property(interpreter.vm().names.raw, raw_obj, 0);

    // 14. Perform ! SetIntegrityLevel(template, frozen).
    MUST(template_->set_integrity_level(Object::IntegrityLevel::Frozen));

    // 15. Append the Record { [[Site]]: templateLiteral, [[Array]]: template } to templateRegistry.
    m_cached_values.set(&realm, make_handle(template_));

    // 16. Return template.
    return template_;
}

void TryStatement::dump(int indent) const
{
    ASTNode::dump(indent);
    print_indent(indent);
    outln("(Block)");
    block().dump(indent + 1);

    if (handler()) {
        print_indent(indent);
        outln("(Handler)");
        handler()->dump(indent + 1);
    }

    if (finalizer()) {
        print_indent(indent);
        outln("(Finalizer)");
        finalizer()->dump(indent + 1);
    }
}

void CatchClause::dump(int indent) const
{
    print_indent(indent);
    m_parameter.visit(
        [&](DeprecatedFlyString const& parameter) {
            if (parameter.is_null())
                outln("CatchClause");
            else
                outln("CatchClause ({})", parameter);
        },
        [&](NonnullRefPtr<BindingPattern const> const& pattern) {
            outln("CatchClause");
            print_indent(indent);
            outln("(Parameter)");
            pattern->dump(indent + 2);
        });

    body().dump(indent + 1);
}

void ThrowStatement::dump(int indent) const
{
    ASTNode::dump(indent);
    argument().dump(indent + 1);
}

// 14.15.3 Runtime Semantics: Evaluation, https://tc39.es/ecma262/#sec-try-statement-runtime-semantics-evaluation
Completion TryStatement::execute(Interpreter& interpreter) const
{
    InterpreterNodeScope node_scope { interpreter, *this };
    auto& vm = interpreter.vm();

    // 14.15.2 Runtime Semantics: CatchClauseEvaluation, https://tc39.es/ecma262/#sec-runtime-semantics-catchclauseevaluation
    auto catch_clause_evaluation = [&](Value thrown_value) {
        // 1. Let oldEnv be the running execution context's LexicalEnvironment.
        auto old_environment = vm.running_execution_context().lexical_environment;

        // 2. Let catchEnv be NewDeclarativeEnvironment(oldEnv).
        auto catch_environment = new_declarative_environment(*old_environment);

        m_handler->parameter().visit(
            [&](DeprecatedFlyString const& parameter) {
                // 3. For each element argName of the BoundNames of CatchParameter, do
                // a. Perform ! catchEnv.CreateMutableBinding(argName, false).
                MUST(catch_environment->create_mutable_binding(vm, parameter, false));
            },
            [&](NonnullRefPtr<BindingPattern const> const& pattern) {
                // 3. For each element argName of the BoundNames of CatchParameter, do
                // NOTE: Due to the use of MUST with `create_mutable_binding` below, an exception should not result from `for_each_bound_name`.
                MUST(pattern->for_each_bound_identifier([&](auto& identifier) {
                    // a. Perform ! catchEnv.CreateMutableBinding(argName, false).
                    MUST(catch_environment->create_mutable_binding(vm, identifier.string(), false));
                }));
            });

        // 4. Set the running execution context's LexicalEnvironment to catchEnv.
        vm.running_execution_context().lexical_environment = catch_environment;

        // 5. Let status be Completion(BindingInitialization of CatchParameter with arguments thrownValue and catchEnv).
        auto status = m_handler->parameter().visit(
            [&](DeprecatedFlyString const& parameter) {
                return catch_environment->initialize_binding(vm, parameter, thrown_value, Environment::InitializeBindingHint::Normal);
            },
            [&](NonnullRefPtr<BindingPattern const> const& pattern) {
                return vm.binding_initialization(pattern, thrown_value, catch_environment);
            });

        // 6. If status is an abrupt completion, then
        if (status.is_error()) {
            // a. Set the running execution context's LexicalEnvironment to oldEnv.
            vm.running_execution_context().lexical_environment = old_environment;

            // b. Return ? status.
            return status.release_error();
        }

        // 7. Let B be the result of evaluating Block.
        auto handler_result = m_handler->body().execute(interpreter);

        // 8. Set the running execution context's LexicalEnvironment to oldEnv.
        vm.running_execution_context().lexical_environment = old_environment;

        // 9. Return ? B.
        return handler_result;
    };

    Completion result;

    // 1. Let B be the result of evaluating Block.
    auto block_result = m_block->execute(interpreter);

    // TryStatement : try Block Catch
    // TryStatement : try Block Catch Finally
    if (m_handler) {
        // 2. If B.[[Type]] is throw, let C be Completion(CatchClauseEvaluation of Catch with argument B.[[Value]]).
        if (block_result.type() == Completion::Type::Throw)
            result = catch_clause_evaluation(*block_result.value());
        // 3. Else, let C be B.
        else
            result = move(block_result);
    } else {
        // TryStatement : try Block Finally
        // This variant doesn't have C & uses B in the finalizer step.
        result = move(block_result);
    }

    // TryStatement : try Block Finally
    // TryStatement : try Block Catch Finally
    if (m_finalizer) {
        // 4. Let F be the result of evaluating Finally.
        auto finalizer_result = m_finalizer->execute(interpreter);

        // 5. If F.[[Type]] is normal, set F to C.
        if (finalizer_result.type() == Completion::Type::Normal)
            finalizer_result = move(result);

        // 6. Return ? UpdateEmpty(F, undefined).
        return finalizer_result.update_empty(js_undefined());
    }

    // 4. Return ? UpdateEmpty(C, undefined).
    return result.update_empty(js_undefined());
}

Completion CatchClause::execute(Interpreter& interpreter) const
{
    InterpreterNodeScope node_scope { interpreter, *this };

    // NOTE: CatchClause execution is handled by TryStatement.
    VERIFY_NOT_REACHED();
    return {};
}

// 14.14.1 Runtime Semantics: Evaluation, https://tc39.es/ecma262/#sec-throw-statement-runtime-semantics-evaluation
Completion ThrowStatement::execute(Interpreter& interpreter) const
{
    InterpreterNodeScope node_scope { interpreter, *this };

    // 1. Let exprRef be the result of evaluating Expression.
    // 2. Let exprValue be ? GetValue(exprRef).
    auto value = TRY(m_argument->execute(interpreter)).release_value();

    // 3. Return ThrowCompletion(exprValue).
    return throw_completion(value);
}

// 14.1.1 Runtime Semantics: Evaluation, https://tc39.es/ecma262/#sec-statement-semantics-runtime-semantics-evaluation
// BreakableStatement : SwitchStatement
Completion SwitchStatement::execute(Interpreter& interpreter) const
{
    // 1. Let newLabelSet be a new empty List.
    // 2. Return ? LabelledEvaluation of this BreakableStatement with argument newLabelSet.
    return labelled_evaluation(interpreter, *this, {});
}

// NOTE: Since we don't have the 'BreakableStatement' from the spec as a separate ASTNode that wraps IterationStatement / SwitchStatement,
// execute() needs to take care of LabelledEvaluation, which in turn calls execute_impl().
// 14.12.4 Runtime Semantics: Evaluation, https://tc39.es/ecma262/#sec-switch-statement-runtime-semantics-evaluation
Completion SwitchStatement::execute_impl(Interpreter& interpreter) const
{
    InterpreterNodeScope node_scope { interpreter, *this };

    auto& vm = interpreter.vm();

    // 14.12.3 CaseClauseIsSelected ( C, input ), https://tc39.es/ecma262/#sec-runtime-semantics-caseclauseisselected
    auto case_clause_is_selected = [&](auto const& case_clause, auto input) -> ThrowCompletionOr<bool> {
        // 1. Assert: C is an instance of the production CaseClause : case Expression : StatementList[opt] .
        VERIFY(case_clause->test());

        // 2. Let exprRef be the result of evaluating the Expression of C.
        // 3. Let clauseSelector be ? GetValue(exprRef).
        auto clause_selector = TRY(case_clause->test()->execute(interpreter)).release_value();

        // 4. Return IsStrictlyEqual(input, clauseSelector).
        return is_strictly_equal(input, clause_selector);
    };

    // 14.12.2 Runtime Semantics: CaseBlockEvaluation, https://tc39.es/ecma262/#sec-runtime-semantics-caseblockevaluation
    auto case_block_evaluation = [&](auto input) -> Completion {
        // CaseBlock : { }
        if (m_cases.is_empty()) {
            // 1. Return undefined.
            return js_undefined();
        }

        Vector<NonnullRefPtr<SwitchCase const>> case_clauses_1;
        Vector<NonnullRefPtr<SwitchCase const>> case_clauses_2;
        RefPtr<SwitchCase const> default_clause;
        for (auto const& switch_case : m_cases) {
            if (!switch_case->test())
                default_clause = switch_case;
            else if (!default_clause)
                case_clauses_1.append(switch_case);
            else
                case_clauses_2.append(switch_case);
        }

        // CaseBlock : { CaseClauses }
        if (!default_clause) {
            VERIFY(!case_clauses_1.is_empty());
            VERIFY(case_clauses_2.is_empty());

            // 1. Let V be undefined.
            auto last_value = js_undefined();

            // 2. Let A be the List of CaseClause items in CaseClauses, in source text order.
            // NOTE: A is case_clauses_1.

            // 3. Let found be false.
            auto found = false;

            // 4. For each CaseClause C of A, do
            for (auto const& case_clause : case_clauses_1) {
                // a. If found is false, then
                if (!found) {
                    // i. Set found to ? CaseClauseIsSelected(C, input).
                    found = TRY(case_clause_is_selected(case_clause, input));
                }

                // b. If found is true, then
                if (found) {
                    // i. Let R be the result of evaluating C.
                    auto result = case_clause->evaluate_statements(interpreter);

                    // ii. If R.[[Value]] is not empty, set V to R.[[Value]].
                    if (result.value().has_value())
                        last_value = *result.value();

                    // iii. If R is an abrupt completion, return ? UpdateEmpty(R, V).
                    if (result.is_abrupt())
                        return result.update_empty(last_value);
                }
            }

            // 5. Return V.
            return last_value;
        }
        // CaseBlock : { CaseClauses[opt] DefaultClause CaseClauses[opt] }
        else {
            // 1. Let V be undefined.
            auto last_value = js_undefined();

            // 2. If the first CaseClauses is present, then
            //    a. Let A be the List of CaseClause items in the first CaseClauses, in source text order.
            // 3. Else,
            //    a. Let A be a new empty List.
            // NOTE: A is case_clauses_1.

            // 4. Let found be false.
            auto found = false;

            // 5. For each CaseClause C of A, do
            for (auto const& case_clause : case_clauses_1) {
                // a. If found is false, then
                if (!found) {
                    // i. Set found to ? CaseClauseIsSelected(C, input).
                    found = TRY(case_clause_is_selected(case_clause, input));
                }

                // b. If found is true, then
                if (found) {
                    // i. Let R be the result of evaluating C.
                    auto result = case_clause->evaluate_statements(interpreter);

                    // ii. If R.[[Value]] is not empty, set V to R.[[Value]].
                    if (result.value().has_value())
                        last_value = *result.value();

                    // iii. If R is an abrupt completion, return ? UpdateEmpty(R, V).
                    if (result.is_abrupt())
                        return result.update_empty(last_value);
                }
            }

            // 6. Let foundInB be false.
            auto found_in_b = false;

            // 7. If the second CaseClauses is present, then
            //    a. Let B be the List of CaseClause items in the second CaseClauses, in source text order.
            // 8. Else,
            //    a. Let B be a new empty List.
            // NOTE: B is case_clauses_2.

            // 9. If found is false, then
            if (!found) {
                // a. For each CaseClause C of B, do
                for (auto const& case_clause : case_clauses_2) {
                    // i. If foundInB is false, then
                    if (!found_in_b) {
                        // 1. Set foundInB to ? CaseClauseIsSelected(C, input).
                        found_in_b = TRY(case_clause_is_selected(case_clause, input));
                    }

                    // ii. If foundInB is true, then
                    if (found_in_b) {
                        // 1. Let R be the result of evaluating CaseClause C.
                        auto result = case_clause->evaluate_statements(interpreter);

                        // 2. If R.[[Value]] is not empty, set V to R.[[Value]].
                        if (result.value().has_value())
                            last_value = *result.value();

                        // 3. If R is an abrupt completion, return ? UpdateEmpty(R, V).
                        if (result.is_abrupt())
                            return result.update_empty(last_value);
                    }
                }
            }

            // 10. If foundInB is true, return V.
            if (found_in_b)
                return last_value;

            // 11. Let R be the result of evaluating DefaultClause.
            auto result = default_clause->evaluate_statements(interpreter);

            // 12. If R.[[Value]] is not empty, set V to R.[[Value]].
            if (result.value().has_value())
                last_value = *result.value();

            // 13. If R is an abrupt completion, return ? UpdateEmpty(R, V).
            if (result.is_abrupt())
                return result.update_empty(last_value);

            // 14. NOTE: The following is another complete iteration of the second CaseClauses.
            // 15. For each CaseClause C of B, do
            for (auto const& case_clause : case_clauses_2) {
                // a. Let R be the result of evaluating CaseClause C.
                result = case_clause->evaluate_statements(interpreter);

                // b. If R.[[Value]] is not empty, set V to R.[[Value]].
                if (result.value().has_value())
                    last_value = *result.value();

                // c. If R is an abrupt completion, return ? UpdateEmpty(R, V).
                if (result.is_abrupt())
                    return result.update_empty(last_value);
            }

            // 16. Return V.
            return last_value;
        }

        VERIFY_NOT_REACHED();
    };

    // SwitchStatement : switch ( Expression ) CaseBlock
    // 1. Let exprRef be the result of evaluating Expression.
    // 2. Let switchValue be ? GetValue(exprRef).
    auto switch_value = TRY(m_discriminant->execute(interpreter)).release_value();

    Completion result;

    // Optimization: Avoid creating a lexical environment if there are no lexical declarations.
    if (has_lexical_declarations()) {
        // 3. Let oldEnv be the running execution context's LexicalEnvironment.
        auto* old_environment = interpreter.lexical_environment();

        // 4. Let blockEnv be NewDeclarativeEnvironment(oldEnv).
        auto block_environment = new_declarative_environment(*old_environment);

        // 5. Perform BlockDeclarationInstantiation(CaseBlock, blockEnv).
        block_declaration_instantiation(vm, block_environment);

        // 6. Set the running execution context's LexicalEnvironment to blockEnv.
        vm.running_execution_context().lexical_environment = block_environment;

        // 7. Let R be Completion(CaseBlockEvaluation of CaseBlock with argument switchValue).
        result = case_block_evaluation(switch_value);

        // 8. Let env be blockEnv's LexicalEnvironment.
        // FIXME: blockEnv doesn't have a lexical env it is one?? Probably a spec issue

        // 9. Set R to DisposeResources(env, R).
        result = dispose_resources(vm, block_environment, result);

        // 10. Set the running execution context's LexicalEnvironment to oldEnv.
        vm.running_execution_context().lexical_environment = old_environment;
    } else {
        result = case_block_evaluation(switch_value);
    }

    // 11. Return R.
    return result;
}

Completion SwitchCase::execute(Interpreter& interpreter) const
{
    InterpreterNodeScope node_scope { interpreter, *this };

    // NOTE: SwitchCase execution is handled by SwitchStatement.
    VERIFY_NOT_REACHED();
    return {};
}

// 14.9.2 Runtime Semantics: Evaluation, https://tc39.es/ecma262/#sec-break-statement-runtime-semantics-evaluation
Completion BreakStatement::execute(Interpreter& interpreter) const
{
    InterpreterNodeScope node_scope { interpreter, *this };

    // BreakStatement : break ;
    if (m_target_label.is_null()) {
        // 1. Return Completion Record { [[Type]]: break, [[Value]]: empty, [[Target]]: empty }.
        return { Completion::Type::Break, {}, {} };
    }

    // BreakStatement : break LabelIdentifier ;
    // 1. Let label be the StringValue of LabelIdentifier.
    // 2. Return Completion Record { [[Type]]: break, [[Value]]: empty, [[Target]]: label }.
    return { Completion::Type::Break, {}, m_target_label };
}

// 14.8.2 Runtime Semantics: Evaluation, https://tc39.es/ecma262/#sec-continue-statement-runtime-semantics-evaluation
Completion ContinueStatement::execute(Interpreter& interpreter) const
{
    InterpreterNodeScope node_scope { interpreter, *this };

    // ContinueStatement : continue ;
    if (m_target_label.is_null()) {
        // 1. Return Completion Record { [[Type]]: continue, [[Value]]: empty, [[Target]]: empty }.
        return { Completion::Type::Continue, {}, {} };
    }

    // ContinueStatement : continue LabelIdentifier ;
    // 1. Let label be the StringValue of LabelIdentifier.
    // 2. Return Completion Record { [[Type]]: continue, [[Value]]: empty, [[Target]]: label }.
    return { Completion::Type::Continue, {}, m_target_label };
}

void SwitchStatement::dump(int indent) const
{
    ASTNode::dump(indent);
    m_discriminant->dump(indent + 1);
    for (auto& switch_case : m_cases) {
        switch_case->dump(indent + 1);
    }
}

void SwitchCase::dump(int indent) const
{
    print_indent(indent + 1);
    if (m_test) {
        outln("(Test)");
        m_test->dump(indent + 2);
    } else {
        outln("(Default)");
    }
    print_indent(indent + 1);
    outln("(Consequent)");
    ScopeNode::dump(indent + 2);
}

// 13.14.1 Runtime Semantics: Evaluation, https://tc39.es/ecma262/#sec-conditional-operator-runtime-semantics-evaluation
Completion ConditionalExpression::execute(Interpreter& interpreter) const
{
    InterpreterNodeScope node_scope { interpreter, *this };

    // 1. Let lref be the result of evaluating ShortCircuitExpression.
    // 2. Let lval be ToBoolean(? GetValue(lref)).
    auto test_result = TRY(m_test->execute(interpreter)).release_value();

    // 3. If lval is true, then
    if (test_result.to_boolean()) {
        // a. Let trueRef be the result of evaluating the first AssignmentExpression.
        // b. Return ? GetValue(trueRef).
        return m_consequent->execute(interpreter);
    }
    // 4. Else,
    else {
        // a. Let falseRef be the result of evaluating the second AssignmentExpression.
        // b. Return ? GetValue(falseRef).
        return m_alternate->execute(interpreter);
    }
}

void ConditionalExpression::dump(int indent) const
{
    ASTNode::dump(indent);
    print_indent(indent + 1);
    outln("(Test)");
    m_test->dump(indent + 2);
    print_indent(indent + 1);
    outln("(Consequent)");
    m_consequent->dump(indent + 2);
    print_indent(indent + 1);
    outln("(Alternate)");
    m_alternate->dump(indent + 2);
}

void SequenceExpression::dump(int indent) const
{
    ASTNode::dump(indent);
    for (auto& expression : m_expressions)
        expression->dump(indent + 1);
}

// 13.16.1 Runtime Semantics: Evaluation, https://tc39.es/ecma262/#sec-comma-operator-runtime-semantics-evaluation
Completion SequenceExpression::execute(Interpreter& interpreter) const
{
    InterpreterNodeScope node_scope { interpreter, *this };

    // NOTE: Not sure why the last node is an AssignmentExpression in the spec :yakfused:
    // 1. Let lref be the result of evaluating Expression.
    // 2. Perform ? GetValue(lref).
    // 3. Let rref be the result of evaluating AssignmentExpression.
    // 4. Return ? GetValue(rref).
    Value last_value;
    for (auto const& expression : m_expressions)
        last_value = TRY(expression->execute(interpreter)).release_value();
    return { move(last_value) };
}

// 14.16.1 Runtime Semantics: Evaluation, https://tc39.es/ecma262/#sec-debugger-statement-runtime-semantics-evaluation
Completion DebuggerStatement::execute(Interpreter& interpreter) const
{
    InterpreterNodeScope node_scope { interpreter, *this };

    Completion result;

    // 1. If an implementation-defined debugging facility is available and enabled, then
    if (false) {
        // a. Perform an implementation-defined debugging action.
        // b. Return a new implementation-defined Completion Record.
        VERIFY_NOT_REACHED();
    }
    // 2. Else,
    else {
        // a. Return empty.
        return Optional<Value> {};
    }
}

ThrowCompletionOr<void> ScopeNode::for_each_lexically_scoped_declaration(ThrowCompletionOrVoidCallback<Declaration const&>&& callback) const
{
    for (auto& declaration : m_lexical_declarations)
        TRY(callback(declaration));

    return {};
}

ThrowCompletionOr<void> ScopeNode::for_each_lexically_declared_identifier(ThrowCompletionOrVoidCallback<Identifier const&>&& callback) const
{
    for (auto const& declaration : m_lexical_declarations) {
        TRY(declaration->for_each_bound_identifier([&](auto const& identifier) {
            return callback(identifier);
        }));
    }
    return {};
}

ThrowCompletionOr<void> ScopeNode::for_each_var_declared_identifier(ThrowCompletionOrVoidCallback<Identifier const&>&& callback) const
{
    for (auto& declaration : m_var_declarations) {
        TRY(declaration->for_each_bound_identifier([&](auto const& id) {
            return callback(id);
        }));
    }
    return {};
}

ThrowCompletionOr<void> ScopeNode::for_each_var_function_declaration_in_reverse_order(ThrowCompletionOrVoidCallback<FunctionDeclaration const&>&& callback) const
{
    for (ssize_t i = m_var_declarations.size() - 1; i >= 0; i--) {
        auto& declaration = m_var_declarations[i];
        if (is<FunctionDeclaration>(declaration))
            TRY(callback(static_cast<FunctionDeclaration const&>(*declaration)));
    }
    return {};
}

ThrowCompletionOr<void> ScopeNode::for_each_var_scoped_variable_declaration(ThrowCompletionOrVoidCallback<VariableDeclaration const&>&& callback) const
{
    for (auto& declaration : m_var_declarations) {
        if (!is<FunctionDeclaration>(declaration)) {
            VERIFY(is<VariableDeclaration>(declaration));
            TRY(callback(static_cast<VariableDeclaration const&>(*declaration)));
        }
    }
    return {};
}

ThrowCompletionOr<void> ScopeNode::for_each_function_hoistable_with_annexB_extension(ThrowCompletionOrVoidCallback<FunctionDeclaration&>&& callback) const
{
    for (auto& function : m_functions_hoistable_with_annexB_extension) {
        // We need const_cast here since it might have to set a property on function declaration.
        TRY(callback(const_cast<FunctionDeclaration&>(*function)));
    }
    return {};
}

void ScopeNode::add_lexical_declaration(NonnullRefPtr<Declaration const> declaration)
{
    m_lexical_declarations.append(move(declaration));
}

void ScopeNode::add_var_scoped_declaration(NonnullRefPtr<Declaration const> declaration)
{
    m_var_declarations.append(move(declaration));
}

void ScopeNode::add_hoisted_function(NonnullRefPtr<FunctionDeclaration const> declaration)
{
    m_functions_hoistable_with_annexB_extension.append(move(declaration));
}

// 16.2.1.11 Runtime Semantics: Evaluation, https://tc39.es/ecma262/#sec-module-semantics-runtime-semantics-evaluation
Completion ImportStatement::execute(Interpreter& interpreter) const
{
    InterpreterNodeScope node_scope { interpreter, *this };

    // 1. Return empty.
    return Optional<Value> {};
}

DeprecatedFlyString ExportStatement::local_name_for_default = "*default*";

// 16.2.3.7 Runtime Semantics: Evaluation, https://tc39.es/ecma262/#sec-exports-runtime-semantics-evaluation
Completion ExportStatement::execute(Interpreter& interpreter) const
{
    InterpreterNodeScope node_scope { interpreter, *this };
    auto& vm = interpreter.vm();

    if (!is_default_export()) {
        if (m_statement) {
            // 1. Return the result of evaluating <Thing>.
            return m_statement->execute(interpreter);
        }

        // 1. Return empty.
        return Optional<Value> {};
    }

    VERIFY(m_statement);

    // ExportDeclaration : export default HoistableDeclaration
    if (is<FunctionDeclaration>(*m_statement)) {
        // 1. Return the result of evaluating HoistableDeclaration.
        return m_statement->execute(interpreter);
    }

    // ExportDeclaration : export default ClassDeclaration
    // ClassDeclaration: class BindingIdentifier[?Yield, ?Await] ClassTail[?Yield, ?Await]
    if (is<ClassDeclaration>(*m_statement)) {
        auto const& class_declaration = static_cast<ClassDeclaration const&>(*m_statement);

        // 1. Let value be ? BindingClassDeclarationEvaluation of ClassDeclaration.
        auto value = TRY(binding_class_declaration_evaluation(interpreter, class_declaration.m_class_expression));

        // 2. Let className be the sole element of BoundNames of ClassDeclaration.
        // 3. If className is "*default*", then
        // Note: We never go into step 3. since a ClassDeclaration always has a name and "*default*" is not a class name.
        (void)value;

        // 4. Return empty.
        return Optional<Value> {};
    }

    // ExportDeclaration : export default ClassDeclaration
    // ClassDeclaration: [+Default] class ClassTail [?Yield, ?Await]
    if (is<ClassExpression>(*m_statement)) {
        auto& class_expression = static_cast<ClassExpression const&>(*m_statement);

        // 1. Let value be ? BindingClassDeclarationEvaluation of ClassDeclaration.
        auto value = TRY(binding_class_declaration_evaluation(interpreter, class_expression));

        // 2. Let className be the sole element of BoundNames of ClassDeclaration.
        // 3. If className is "*default*", then
        if (!class_expression.has_name()) {
            // Note: This can only occur if the class does not have a name since "*default*" is normally not valid.

            // a. Let env be the running execution context's LexicalEnvironment.
            auto* env = interpreter.lexical_environment();

            // b. Perform ? InitializeBoundName("*default*", value, env).
            TRY(initialize_bound_name(vm, ExportStatement::local_name_for_default, value, env));
        }

        // 4. Return empty.
        return Optional<Value> {};
    }

    // ExportDeclaration : export default AssignmentExpression ;

    // 1. If IsAnonymousFunctionDefinition(AssignmentExpression) is true, then
    //     a. Let value be ? NamedEvaluation of AssignmentExpression with argument "default".
    // 2. Else,
    //     a. Let rhs be the result of evaluating AssignmentExpression.
    //     b. Let value be ? GetValue(rhs).
    auto value = TRY(vm.named_evaluation_if_anonymous_function(*m_statement, "default"));

    // 3. Let env be the running execution context's LexicalEnvironment.
    auto* env = interpreter.lexical_environment();

    // 4. Perform ? InitializeBoundName("*default*", value, env).
    TRY(initialize_bound_name(vm, ExportStatement::local_name_for_default, value, env));

    // 5. Return empty.
    return Optional<Value> {};
}

static void dump_assert_clauses(ModuleRequest const& request)
{
    if (!request.assertions.is_empty()) {
        out("[ ");
        for (auto& assertion : request.assertions)
            out("{}: {}, ", assertion.key, assertion.value);
        out(" ]");
    }
}

void ExportStatement::dump(int indent) const
{
    ASTNode::dump(indent);
    print_indent(indent + 1);
    outln("(ExportEntries)");

    auto string_or_null = [](DeprecatedString const& string) -> DeprecatedString {
        if (string.is_empty()) {
            return "null";
        }
        return DeprecatedString::formatted("\"{}\"", string);
    };

    for (auto& entry : m_entries) {
        print_indent(indent + 2);
        out("ExportName: {}, ImportName: {}, LocalName: {}, ModuleRequest: ",
            string_or_null(entry.export_name),
            entry.is_module_request() ? string_or_null(entry.local_or_import_name) : "null",
            entry.is_module_request() ? "null" : string_or_null(entry.local_or_import_name));
        if (entry.is_module_request()) {
            out("{}", entry.m_module_request->module_specifier);
            dump_assert_clauses(*entry.m_module_request);
            outln();
        } else {
            outln("null");
        }
    }

    if (m_statement) {
        print_indent(indent + 1);
        outln("(Statement)");
        m_statement->dump(indent + 2);
    }
}

void ImportStatement::dump(int indent) const
{
    ASTNode::dump(indent);
    print_indent(indent + 1);
    if (m_entries.is_empty()) {
        // direct from "module" import
        outln("Entire module '{}'", m_module_request.module_specifier);
        dump_assert_clauses(m_module_request);
    } else {
        outln("(ExportEntries) from {}", m_module_request.module_specifier);
        dump_assert_clauses(m_module_request);

        for (auto& entry : m_entries) {
            print_indent(indent + 2);
            outln("ImportName: {}, LocalName: {}", entry.import_name, entry.local_name);
        }
    }
}

bool ExportStatement::has_export(DeprecatedFlyString const& export_name) const
{
    return any_of(m_entries.begin(), m_entries.end(), [&](auto& entry) {
        // Make sure that empty exported names does not overlap with anything
        if (entry.kind != ExportEntry::Kind::NamedExport)
            return false;
        return entry.export_name == export_name;
    });
}

bool ImportStatement::has_bound_name(DeprecatedFlyString const& name) const
{
    return any_of(m_entries.begin(), m_entries.end(), [&](auto& entry) {
        return entry.local_name == name;
    });
}

// 14.2.3 BlockDeclarationInstantiation ( code, env ), https://tc39.es/ecma262/#sec-blockdeclarationinstantiation
void ScopeNode::block_declaration_instantiation(VM& vm, Environment* environment) const
{
    // See also B.3.2.6 Changes to BlockDeclarationInstantiation, https://tc39.es/ecma262/#sec-web-compat-blockdeclarationinstantiation
    auto& realm = *vm.current_realm();

    VERIFY(environment);
    auto private_environment = vm.running_execution_context().private_environment;
    // Note: All the calls here are ! and thus we do not need to TRY this callback.
    //       We use MUST to ensure it does not throw and to avoid discarding the returned ThrowCompletionOr<void>.
    MUST(for_each_lexically_scoped_declaration([&](Declaration const& declaration) {
        auto is_constant_declaration = declaration.is_constant_declaration();
        // NOTE: Due to the use of MUST with `create_immutable_binding` and `create_mutable_binding` below,
        //       an exception should not result from `for_each_bound_name`.
        MUST(declaration.for_each_bound_identifier([&](auto const& identifier) {
            if (vm.bytecode_interpreter_if_exists() && identifier.is_local()) {
                // NOTE: No need to create bindings for local variables as their values are not stored in an environment.
                return;
            }

            auto const& name = identifier.string();
            if (is_constant_declaration) {
                MUST(environment->create_immutable_binding(vm, name, true));
            } else {
                if (!MUST(environment->has_binding(name)))
                    MUST(environment->create_mutable_binding(vm, name, false));
            }
        }));

        if (is<FunctionDeclaration>(declaration)) {
            auto& function_declaration = static_cast<FunctionDeclaration const&>(declaration);
            auto function = ECMAScriptFunctionObject::create(realm, function_declaration.name(), function_declaration.source_text(), function_declaration.body(), function_declaration.parameters(), function_declaration.function_length(), function_declaration.local_variables_names(), environment, private_environment, function_declaration.kind(), function_declaration.is_strict_mode(), function_declaration.might_need_arguments_object(), function_declaration.contains_direct_call_to_eval());
            if (vm.bytecode_interpreter_if_exists() && function_declaration.name_identifier()->is_local()) {
                vm.running_execution_context().local_variables[function_declaration.name_identifier()->local_variable_index()] = function;
            } else {
                VERIFY(is<DeclarativeEnvironment>(*environment));
                static_cast<DeclarativeEnvironment&>(*environment).initialize_or_set_mutable_binding({}, vm, function_declaration.name(), function);
            }
        }
    }));
}

// 16.1.7 GlobalDeclarationInstantiation ( script, env ), https://tc39.es/ecma262/#sec-globaldeclarationinstantiation
ThrowCompletionOr<void> Program::global_declaration_instantiation(VM& vm, GlobalEnvironment& global_environment) const
{
    auto& realm = *vm.current_realm();

    // 1. Let lexNames be the LexicallyDeclaredNames of script.
    // 2. Let varNames be the VarDeclaredNames of script.
    // 3. For each element name of lexNames, do
    TRY(for_each_lexically_declared_identifier([&](Identifier const& identifier) -> ThrowCompletionOr<void> {
        auto const& name = identifier.string();

        // a. If env.HasVarDeclaration(name) is true, throw a SyntaxError exception.
        if (global_environment.has_var_declaration(name))
            return vm.throw_completion<SyntaxError>(ErrorType::TopLevelVariableAlreadyDeclared, name);

        // b. If env.HasLexicalDeclaration(name) is true, throw a SyntaxError exception.
        if (global_environment.has_lexical_declaration(name))
            return vm.throw_completion<SyntaxError>(ErrorType::TopLevelVariableAlreadyDeclared, name);

        // c. Let hasRestrictedGlobal be ? env.HasRestrictedGlobalProperty(name).
        auto has_restricted_global = TRY(global_environment.has_restricted_global_property(name));

        // d. If hasRestrictedGlobal is true, throw a SyntaxError exception.
        if (has_restricted_global)
            return vm.throw_completion<SyntaxError>(ErrorType::RestrictedGlobalProperty, name);

        return {};
    }));

    // 4. For each element name of varNames, do
    TRY(for_each_var_declared_identifier([&](auto const& identifier) -> ThrowCompletionOr<void> {
        // a. If env.HasLexicalDeclaration(name) is true, throw a SyntaxError exception.
        if (global_environment.has_lexical_declaration(identifier.string()))
            return vm.throw_completion<SyntaxError>(ErrorType::TopLevelVariableAlreadyDeclared, identifier.string());

        return {};
    }));

    // 5. Let varDeclarations be the VarScopedDeclarations of script.
    // 6. Let functionsToInitialize be a new empty List.
    Vector<FunctionDeclaration const&> functions_to_initialize;

    // 7. Let declaredFunctionNames be a new empty List.
    HashTable<DeprecatedFlyString> declared_function_names;

    // 8. For each element d of varDeclarations, in reverse List order, do

    TRY(for_each_var_function_declaration_in_reverse_order([&](FunctionDeclaration const& function) -> ThrowCompletionOr<void> {
        // a. If d is neither a VariableDeclaration nor a ForBinding nor a BindingIdentifier, then
        // i. Assert: d is either a FunctionDeclaration, a GeneratorDeclaration, an AsyncFunctionDeclaration, or an AsyncGeneratorDeclaration.
        // Note: This is checked in for_each_var_function_declaration_in_reverse_order.

        // ii. NOTE: If there are multiple function declarations for the same name, the last declaration is used.

        // iii. Let fn be the sole element of the BoundNames of d.

        // iv. If fn is not an element of declaredFunctionNames, then
        if (declared_function_names.set(function.name()) != AK::HashSetResult::InsertedNewEntry)
            return {};

        // 1. Let fnDefinable be ? env.CanDeclareGlobalFunction(fn).
        auto function_definable = TRY(global_environment.can_declare_global_function(function.name()));

        // 2. If fnDefinable is false, throw a TypeError exception.
        if (!function_definable)
            return vm.throw_completion<TypeError>(ErrorType::CannotDeclareGlobalFunction, function.name());

        // 3. Append fn to declaredFunctionNames.
        // Note: Already done in step iv. above.

        // 4. Insert d as the first element of functionsToInitialize.
        // NOTE: Since prepending is much slower, we just append
        //       and iterate in reverse order in step 16 below.
        functions_to_initialize.append(function);
        return {};
    }));

    // 9. Let declaredVarNames be a new empty List.
    HashTable<DeprecatedFlyString> declared_var_names;

    // 10. For each element d of varDeclarations, do
    TRY(for_each_var_scoped_variable_declaration([&](Declaration const& declaration) {
        // a. If d is a VariableDeclaration, a ForBinding, or a BindingIdentifier, then
        // Note: This is done in for_each_var_scoped_variable_declaration.

        // i. For each String vn of the BoundNames of d, do
        return declaration.for_each_bound_identifier([&](auto const& identifier) -> ThrowCompletionOr<void> {
            auto const& name = identifier.string();
            // 1. If vn is not an element of declaredFunctionNames, then
            if (declared_function_names.contains(name))
                return {};

            // a. Let vnDefinable be ? env.CanDeclareGlobalVar(vn).
            auto var_definable = TRY(global_environment.can_declare_global_var(name));

            // b. If vnDefinable is false, throw a TypeError exception.
            if (!var_definable)
                return vm.throw_completion<TypeError>(ErrorType::CannotDeclareGlobalVariable, name);

            // c. If vn is not an element of declaredVarNames, then
            // i. Append vn to declaredVarNames.
            declared_var_names.set(name);
            return {};
        });
    }));

    // 11. NOTE: No abnormal terminations occur after this algorithm step if the global object is an ordinary object. However, if the global object is a Proxy exotic object it may exhibit behaviours that cause abnormal terminations in some of the following steps.
    // 12. NOTE: Annex B.3.2.2 adds additional steps at this point.

    // 12. Let strict be IsStrict of script.
    // 13. If strict is false, then
    if (!m_is_strict_mode) {
        // a. Let declaredFunctionOrVarNames be the list-concatenation of declaredFunctionNames and declaredVarNames.
        // b. For each FunctionDeclaration f that is directly contained in the StatementList of a Block, CaseClause, or DefaultClause Contained within script, do
        TRY(for_each_function_hoistable_with_annexB_extension([&](FunctionDeclaration& function_declaration) -> ThrowCompletionOr<void> {
            // i. Let F be StringValue of the BindingIdentifier of f.
            auto function_name = function_declaration.name();

            // ii. If replacing the FunctionDeclaration f with a VariableStatement that has F as a BindingIdentifier would not produce any Early Errors for script, then
            // Note: This step is already performed during parsing and for_each_function_hoistable_with_annexB_extension so this always passes here.

            // 1. If env.HasLexicalDeclaration(F) is false, then
            if (global_environment.has_lexical_declaration(function_name))
                return {};

            // a. Let fnDefinable be ? env.CanDeclareGlobalVar(F).
            auto function_definable = TRY(global_environment.can_declare_global_function(function_name));
            // b. If fnDefinable is true, then

            if (!function_definable)
                return {};

            // i. NOTE: A var binding for F is only instantiated here if it is neither a VarDeclaredName nor the name of another FunctionDeclaration.

            // ii. If declaredFunctionOrVarNames does not contain F, then

            if (!declared_function_names.contains(function_name) && !declared_var_names.contains(function_name)) {
                // i. Perform ? env.CreateGlobalVarBinding(F, false).
                TRY(global_environment.create_global_var_binding(function_name, false));

                // ii. Append F to declaredFunctionOrVarNames.
                declared_function_names.set(function_name);
            }

            // iii. When the FunctionDeclaration f is evaluated, perform the following steps in place of the FunctionDeclaration Evaluation algorithm provided in 15.2.6:
            //     i. Let genv be the running execution context's VariableEnvironment.
            //     ii. Let benv be the running execution context's LexicalEnvironment.
            //     iii. Let fobj be ! benv.GetBindingValue(F, false).
            //     iv. Perform ? genv.SetMutableBinding(F, fobj, false).
            //     v. Return unused.
            function_declaration.set_should_do_additional_annexB_steps();

            return {};
        }));

        // We should not use declared function names below here anymore since these functions are not in there in the spec.
        declared_function_names.clear();
    }

    // 13. Let lexDeclarations be the LexicallyScopedDeclarations of script.
    // 14. Let privateEnv be null.
    PrivateEnvironment* private_environment = nullptr;

    // 15. For each element d of lexDeclarations, do
    TRY(for_each_lexically_scoped_declaration([&](Declaration const& declaration) {
        // a. NOTE: Lexically declared names are only instantiated here but not initialized.
        // b. For each element dn of the BoundNames of d, do
        return declaration.for_each_bound_identifier([&](auto const& identifier) -> ThrowCompletionOr<void> {
            auto const& name = identifier.string();
            // i. If IsConstantDeclaration of d is true, then
            if (declaration.is_constant_declaration()) {
                // 1. Perform ? env.CreateImmutableBinding(dn, true).
                TRY(global_environment.create_immutable_binding(vm, name, true));
            }
            // ii. Else,
            else {
                // 1. Perform ? env.CreateMutableBinding(dn, false).
                TRY(global_environment.create_mutable_binding(vm, name, false));
            }

            return {};
        });
    }));

    // 16. For each Parse Node f of functionsToInitialize, do
    // NOTE: We iterate in reverse order since we appended the functions
    //       instead of prepending. We append because prepending is much slower
    //       and we only use the created vector here.
    for (auto& declaration : functions_to_initialize.in_reverse()) {
        // a. Let fn be the sole element of the BoundNames of f.
        // b. Let fo be InstantiateFunctionObject of f with arguments env and privateEnv.
        auto function = ECMAScriptFunctionObject::create(realm, declaration.name(), declaration.source_text(), declaration.body(), declaration.parameters(), declaration.function_length(), declaration.local_variables_names(), &global_environment, private_environment, declaration.kind(), declaration.is_strict_mode(), declaration.might_need_arguments_object(), declaration.contains_direct_call_to_eval());

        // c. Perform ? env.CreateGlobalFunctionBinding(fn, fo, false).
        TRY(global_environment.create_global_function_binding(declaration.name(), function, false));
    }

    // 17. For each String vn of declaredVarNames, do
    for (auto& var_name : declared_var_names) {
        // a. Perform ? env.CreateGlobalVarBinding(vn, false).
        TRY(global_environment.create_global_var_binding(var_name, false));
    }

    // 18. Return unused.
    return {};
}

ModuleRequest::ModuleRequest(DeprecatedFlyString module_specifier_, Vector<Assertion> assertions_)
    : module_specifier(move(module_specifier_))
    , assertions(move(assertions_))
{
    // Perform step 10.e. from EvaluateImportCall, https://tc39.es/proposal-import-assertions/#sec-evaluate-import-call
    // or step 2. from 2.7 Static Semantics: AssertClauseToAssertions, https://tc39.es/proposal-import-assertions/#sec-assert-clause-to-assertions
    // e. / 2. Sort assertions by the code point order of the [[Key]] of each element.
    // NOTE: This sorting is observable only in that hosts are prohibited from distinguishing among assertions by the order they occur in.
    quick_sort(assertions, [](Assertion const& lhs, Assertion const& rhs) {
        return lhs.key < rhs.key;
    });
}

DeprecatedString SourceRange::filename() const
{
    return code->filename().to_deprecated_string();
}

NonnullRefPtr<CallExpression> CallExpression::create(SourceRange source_range, NonnullRefPtr<Expression const> callee, ReadonlySpan<Argument> arguments, InvocationStyleEnum invocation_style, InsideParenthesesEnum inside_parens)
{
    return ASTNodeWithTailArray::create<CallExpression>(arguments.size(), move(source_range), move(callee), arguments, invocation_style, inside_parens);
}

NonnullRefPtr<NewExpression> NewExpression::create(SourceRange source_range, NonnullRefPtr<Expression const> callee, ReadonlySpan<Argument> arguments, InvocationStyleEnum invocation_style, InsideParenthesesEnum inside_parens)
{
    return ASTNodeWithTailArray::create<NewExpression>(arguments.size(), move(source_range), move(callee), arguments, invocation_style, inside_parens);
}

}
