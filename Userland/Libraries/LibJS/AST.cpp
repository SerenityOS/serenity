/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2020-2022, Linus Groh <linusg@serenityos.org>
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
#include <LibJS/Runtime/IteratorOperations.h>
#include <LibJS/Runtime/NativeFunction.h>
#include <LibJS/Runtime/ObjectEnvironment.h>
#include <LibJS/Runtime/PrimitiveString.h>
#include <LibJS/Runtime/PromiseConstructor.h>
#include <LibJS/Runtime/PromiseReaction.h>
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
        m_interpreter.vm().running_execution_context().current_node = &node;
        m_interpreter.push_ast_node(m_chain_node);
    }

    ~InterpreterNodeScope()
    {
        m_interpreter.pop_ast_node();
    }

private:
    Interpreter& m_interpreter;
    ExecutingASTNodeChain m_chain_node;
};

String ASTNode::class_name() const
{
    // NOTE: We strip the "JS::" prefix.
    return demangle(typeid(*this).name()).substring(4);
}

static void print_indent(int indent)
{
    out("{}", String::repeated(' ', indent * 2));
}

static void update_function_name(Value value, FlyString const& name)
{
    if (!value.is_function())
        return;
    auto& function = value.as_function();
    if (is<ECMAScriptFunctionObject>(function) && function.name().is_empty())
        static_cast<ECMAScriptFunctionObject&>(function).set_name(name);
}

static ThrowCompletionOr<String> get_function_property_name(PropertyKey key)
{
    if (key.is_symbol())
        return String::formatted("[{}]", key.as_symbol()->description());
    return key.to_string();
}

// 14.2.2 Runtime Semantics: Evaluation, https://tc39.es/ecma262/#sec-block-runtime-semantics-evaluation
// StatementList : StatementList StatementListItem
Completion ScopeNode::evaluate_statements(Interpreter& interpreter, GlobalObject& global_object) const
{
    auto completion = normal_completion({});
    for (auto const& node : children()) {
        completion = node.execute(interpreter, global_object).update_empty(completion.value());
        if (completion.is_abrupt())
            break;
    }
    return completion;
}

// 14.13.4 Runtime Semantics: LabelledEvaluation, https://tc39.es/ecma262/#sec-runtime-semantics-labelledevaluation
// BreakableStatement : IterationStatement
static Completion labelled_evaluation(Interpreter& interpreter, GlobalObject& global_object, IterationStatement const& statement, Vector<FlyString> const& label_set)
{
    // 1. Let stmtResult be LoopEvaluation of IterationStatement with argument labelSet.
    auto result = statement.loop_evaluation(interpreter, global_object, label_set);

    // 2. If stmtResult.[[Type]] is break, then
    if (result.type() == Completion::Type::Break) {
        // a. If stmtResult.[[Target]] is empty, then
        if (!result.target().has_value()) {
            // i. If stmtResult.[[Value]] is empty, set stmtResult to NormalCompletion(undefined).
            // ii. Else, set stmtResult to NormalCompletion(stmtResult.[[Value]]).
            result = normal_completion(result.value().value_or(js_undefined()));
        }
    }

    // 3. Return Completion(stmtResult).
    return result;
}

// 14.13.4 Runtime Semantics: LabelledEvaluation, https://tc39.es/ecma262/#sec-runtime-semantics-labelledevaluation
// BreakableStatement : SwitchStatement
static Completion labelled_evaluation(Interpreter& interpreter, GlobalObject& global_object, SwitchStatement const& statement, Vector<FlyString> const&)
{
    // 1. Let stmtResult be the result of evaluating SwitchStatement.
    auto result = statement.execute_impl(interpreter, global_object);

    // 2. If stmtResult.[[Type]] is break, then
    if (result.type() == Completion::Type::Break) {
        // a. If stmtResult.[[Target]] is empty, then
        if (!result.target().has_value()) {
            // i. If stmtResult.[[Value]] is empty, set stmtResult to NormalCompletion(undefined).
            // ii. Else, set stmtResult to NormalCompletion(stmtResult.[[Value]]).
            result = normal_completion(result.value().value_or(js_undefined()));
        }
    }

    // 3. Return Completion(stmtResult).
    return result;
}

// 14.13.4 Runtime Semantics: LabelledEvaluation, https://tc39.es/ecma262/#sec-runtime-semantics-labelledevaluation
// LabelledStatement : LabelIdentifier : LabelledItem
static Completion labelled_evaluation(Interpreter& interpreter, GlobalObject& global_object, LabelledStatement const& statement, Vector<FlyString> const& label_set)
{
    auto const& labelled_item = *statement.labelled_item();

    // 1. Let label be the StringValue of LabelIdentifier.
    auto const& label = statement.label();

    // 2. Let newLabelSet be the list-concatenation of labelSet and « label ».
    // Optimization: Avoid vector copy if possible.
    Optional<Vector<FlyString>> new_label_set;
    if (is<IterationStatement>(labelled_item) || is<SwitchStatement>(labelled_item) || is<LabelledStatement>(labelled_item)) {
        new_label_set = label_set;
        new_label_set->append(label);
    }

    // 3. Let stmtResult be LabelledEvaluation of LabelledItem with argument newLabelSet.
    Completion result;
    if (is<IterationStatement>(labelled_item))
        result = labelled_evaluation(interpreter, global_object, static_cast<IterationStatement const&>(labelled_item), *new_label_set);
    else if (is<SwitchStatement>(labelled_item))
        result = labelled_evaluation(interpreter, global_object, static_cast<SwitchStatement const&>(labelled_item), *new_label_set);
    else if (is<LabelledStatement>(labelled_item))
        result = labelled_evaluation(interpreter, global_object, static_cast<LabelledStatement const&>(labelled_item), *new_label_set);
    else
        result = labelled_item.execute(interpreter, global_object);

    // 4. If stmtResult.[[Type]] is break and SameValue(stmtResult.[[Target]], label) is true, then
    if (result.type() == Completion::Type::Break && result.target() == label) {
        // a. Set stmtResult to NormalCompletion(stmtResult.[[Value]]).
        result = normal_completion(result.value());
    }

    // 5. Return Completion(stmtResult).
    return result;
}

// 14.13.3 Runtime Semantics: Evaluation, https://tc39.es/ecma262/#sec-labelled-statements-runtime-semantics-evaluation
Completion LabelledStatement::execute(Interpreter& interpreter, GlobalObject& global_object) const
{
    InterpreterNodeScope node_scope { interpreter, *this };

    // 1. Let newLabelSet be a new empty List.
    // 2. Return LabelledEvaluation of this LabelledStatement with argument newLabelSet.
    return labelled_evaluation(interpreter, global_object, *this, {});
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
Completion FunctionBody::execute(Interpreter& interpreter, GlobalObject& global_object) const
{
    InterpreterNodeScope node_scope { interpreter, *this };

    // Note: Scoping should have already been set up by whoever is calling this FunctionBody.
    // 1. Return ? EvaluateFunctionBody of FunctionBody with arguments functionObject and argumentsList.
    return evaluate_statements(interpreter, global_object);
}

// 14.2.2 Runtime Semantics: Evaluation, https://tc39.es/ecma262/#sec-block-runtime-semantics-evaluation
Completion BlockStatement::execute(Interpreter& interpreter, GlobalObject& global_object) const
{
    InterpreterNodeScope node_scope { interpreter, *this };

    auto& vm = interpreter.vm();

    Environment* old_environment { nullptr };
    ArmedScopeGuard restore_environment = [&] {
        vm.running_execution_context().lexical_environment = old_environment;
    };

    // Optimization: We only need a new lexical environment if there are any lexical declarations. :^)
    if (has_lexical_declarations()) {
        old_environment = vm.running_execution_context().lexical_environment;
        auto* block_environment = new_declarative_environment(*old_environment);
        block_declaration_instantiation(global_object, block_environment);
        vm.running_execution_context().lexical_environment = block_environment;
    } else {
        restore_environment.disarm();
    }

    return evaluate_statements(interpreter, global_object);
}

Completion Program::execute(Interpreter& interpreter, GlobalObject& global_object) const
{
    InterpreterNodeScope node_scope { interpreter, *this };

    return evaluate_statements(interpreter, global_object);
}

// 15.2.6 Runtime Semantics: Evaluation, https://tc39.es/ecma262/#sec-function-definitions-runtime-semantics-evaluation
Completion FunctionDeclaration::execute(Interpreter& interpreter, GlobalObject& global_object) const
{
    InterpreterNodeScope node_scope { interpreter, *this };

    if (m_is_hoisted) {
        // Perform special annexB steps see step 3 of: https://tc39.es/ecma262/#sec-web-compat-functiondeclarationinstantiation

        // i. Let genv be the running execution context's VariableEnvironment.
        auto* variable_environment = interpreter.vm().running_execution_context().variable_environment;

        // ii. Let benv be the running execution context's LexicalEnvironment.
        auto* lexical_environment = interpreter.vm().running_execution_context().lexical_environment;

        // iii. Let fobj be ! benv.GetBindingValue(F, false).
        auto function_object = MUST(lexical_environment->get_binding_value(global_object, name(), false));

        // iv. Perform ? genv.SetMutableBinding(F, fobj, false).
        TRY(variable_environment->set_mutable_binding(global_object, name(), function_object, false));

        // v. Return NormalCompletion(empty).
        return normal_completion({});
    }

    // 1. Return NormalCompletion(empty).
    return normal_completion({});
}

// 15.2.6 Runtime Semantics: Evaluation, https://tc39.es/ecma262/#sec-function-definitions-runtime-semantics-evaluation
Completion FunctionExpression::execute(Interpreter& interpreter, GlobalObject& global_object) const
{
    InterpreterNodeScope node_scope { interpreter, *this };

    // 1. Return InstantiateOrdinaryFunctionExpression of FunctionExpression.
    return instantiate_ordinary_function_expression(interpreter, global_object, name());
}

// 15.2.5 Runtime Semantics: InstantiateOrdinaryFunctionExpression, https://tc39.es/ecma262/#sec-runtime-semantics-instantiateordinaryfunctionexpression
Value FunctionExpression::instantiate_ordinary_function_expression(Interpreter& interpreter, GlobalObject& global_object, FlyString given_name) const
{
    if (given_name.is_empty())
        given_name = "";
    auto has_own_name = !name().is_empty();

    auto const& used_name = has_own_name ? name() : given_name;
    auto* scope = interpreter.lexical_environment();
    if (has_own_name) {
        VERIFY(scope);
        scope = new_declarative_environment(*scope);
        MUST(scope->create_immutable_binding(global_object, name(), false));
    }

    auto* private_scope = interpreter.vm().running_execution_context().private_environment;

    auto closure = ECMAScriptFunctionObject::create(global_object, used_name, source_text(), body(), parameters(), function_length(), scope, private_scope, kind(), is_strict_mode(), might_need_arguments_object(), contains_direct_call_to_eval(), is_arrow_function());

    // FIXME: 6. Perform SetFunctionName(closure, name).
    // FIXME: 7. Perform MakeConstructor(closure).

    if (has_own_name)
        MUST(scope->initialize_binding(global_object, name(), closure));

    return closure;
}

// 14.4.1 Runtime Semantics: Evaluation, https://tc39.es/ecma262/#sec-empty-statement-runtime-semantics-evaluation
Completion EmptyStatement::execute(Interpreter&, GlobalObject&) const
{
    // 1. Return NormalCompletion(empty).
    return normal_completion({});
}

// 14.5.1 Runtime Semantics: Evaluation, https://tc39.es/ecma262/#sec-expression-statement-runtime-semantics-evaluation
Completion ExpressionStatement::execute(Interpreter& interpreter, GlobalObject& global_object) const
{
    InterpreterNodeScope node_scope { interpreter, *this };

    // 1. Let exprRef be the result of evaluating Expression.
    // 2. Return ? GetValue(exprRef).
    return m_expression->execute(interpreter, global_object);
}

// TODO: This shouldn't exist. Refactor into EvaluateCall.
ThrowCompletionOr<CallExpression::ThisAndCallee> CallExpression::compute_this_and_callee(Interpreter& interpreter, GlobalObject& global_object, Reference const& callee_reference) const
{
    if (callee_reference.is_property_reference()) {
        auto this_value = callee_reference.get_this_value();
        auto callee = TRY(callee_reference.get_value(global_object));

        return ThisAndCallee { this_value, callee };
    }

    // [[Call]] will handle that in non-strict mode the this value becomes the global object
    return ThisAndCallee {
        js_undefined(),
        callee_reference.is_unresolvable()
            ? TRY(m_callee->execute(interpreter, global_object)).release_value()
            : TRY(callee_reference.get_value(global_object))
    };
}

// 13.3.8.1 Runtime Semantics: ArgumentListEvaluation, https://tc39.es/ecma262/#sec-runtime-semantics-argumentlistevaluation
static ThrowCompletionOr<void> argument_list_evaluation(Interpreter& interpreter, GlobalObject& global_object, Vector<CallExpression::Argument> const& arguments, MarkedVector<Value>& list)
{
    list.ensure_capacity(arguments.size());

    for (auto& argument : arguments) {
        auto value = TRY(argument.value->execute(interpreter, global_object)).release_value();
        if (argument.is_spread) {
            auto result = TRY(get_iterator_values(global_object, value, [&](Value iterator_value) -> Optional<Completion> {
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
Completion NewExpression::execute(Interpreter& interpreter, GlobalObject& global_object) const
{
    InterpreterNodeScope node_scope { interpreter, *this };
    auto& vm = interpreter.vm();

    // 1. Let ref be the result of evaluating constructExpr.
    // 2. Let constructor be ? GetValue(ref).
    auto constructor = TRY(m_callee->execute(interpreter, global_object)).release_value();

    // 3. If arguments is empty, let argList be a new empty List.
    // 4. Else,
    //    a. Let argList be ? ArgumentListEvaluation of arguments.
    MarkedVector<Value> arg_list(vm.heap());
    TRY(argument_list_evaluation(interpreter, global_object, m_arguments, arg_list));

    // 5. If IsConstructor(constructor) is false, throw a TypeError exception.
    if (!constructor.is_constructor())
        return throw_type_error_for_callee(interpreter, global_object, constructor, "constructor"sv);

    // 6. Return ? Construct(constructor, argList).
    return Value { TRY(construct(global_object, constructor.as_function(), move(arg_list))) };
}

Completion CallExpression::throw_type_error_for_callee(Interpreter& interpreter, GlobalObject& global_object, Value callee_value, StringView call_type) const
{
    auto& vm = interpreter.vm();
    if (is<Identifier>(*m_callee) || is<MemberExpression>(*m_callee)) {
        String expression_string;
        if (is<Identifier>(*m_callee)) {
            expression_string = static_cast<Identifier const&>(*m_callee).string();
        } else {
            expression_string = static_cast<MemberExpression const&>(*m_callee).to_string_approximation();
        }
        return vm.throw_completion<TypeError>(global_object, ErrorType::IsNotAEvaluatedFrom, callee_value.to_string_without_side_effects(), call_type, expression_string);
    } else {
        return vm.throw_completion<TypeError>(global_object, ErrorType::IsNotA, callee_value.to_string_without_side_effects(), call_type);
    }
}

// 13.3.6.1 Runtime Semantics: Evaluation, https://tc39.es/ecma262/#sec-function-calls-runtime-semantics-evaluation
Completion CallExpression::execute(Interpreter& interpreter, GlobalObject& global_object) const
{
    InterpreterNodeScope node_scope { interpreter, *this };
    auto& vm = interpreter.vm();
    auto callee_reference = TRY(m_callee->to_reference(interpreter, global_object));

    auto [this_value, callee] = TRY(compute_this_and_callee(interpreter, global_object, callee_reference));

    VERIFY(!callee.is_empty());

    MarkedVector<Value> arg_list(vm.heap());
    TRY(argument_list_evaluation(interpreter, global_object, m_arguments, arg_list));

    if (!callee.is_function())
        return throw_type_error_for_callee(interpreter, global_object, callee, "function"sv);

    auto& function = callee.as_function();

    if (&function == global_object.eval_function()
        && callee_reference.is_environment_reference()
        && callee_reference.name().is_string()
        && callee_reference.name().as_string() == vm.names.eval.as_string()) {

        auto script_value = arg_list.size() == 0 ? js_undefined() : arg_list[0];
        return perform_eval(script_value, global_object, vm.in_strict_mode() ? CallerMode::Strict : CallerMode::NonStrict, EvalMode::Direct);
    }

    return call(global_object, function, this_value, move(arg_list));
}

// 13.3.7.1 Runtime Semantics: Evaluation, https://tc39.es/ecma262/#sec-super-keyword-runtime-semantics-evaluation
// SuperCall : super Arguments
Completion SuperCall::execute(Interpreter& interpreter, GlobalObject& global_object) const
{
    InterpreterNodeScope node_scope { interpreter, *this };
    auto& vm = interpreter.vm();

    // 1. Let newTarget be GetNewTarget().
    auto new_target = vm.get_new_target();

    // 2. Assert: Type(newTarget) is Object.
    VERIFY(new_target.is_function());

    // 3. Let func be ! GetSuperConstructor().
    auto* func = get_super_constructor(interpreter.vm());

    // 4. Let argList be ? ArgumentListEvaluation of Arguments.
    MarkedVector<Value> arg_list(vm.heap());
    TRY(argument_list_evaluation(interpreter, global_object, m_arguments, arg_list));

    // 5. If IsConstructor(func) is false, throw a TypeError exception.
    if (!func || !Value(func).is_constructor())
        return vm.throw_completion<TypeError>(global_object, ErrorType::NotAConstructor, "Super constructor");

    // 6. Let result be ? Construct(func, argList, newTarget).
    auto* result = TRY(construct(global_object, static_cast<FunctionObject&>(*func), move(arg_list), &new_target.as_function()));

    // 7. Let thisER be GetThisEnvironment().
    auto& this_er = verify_cast<FunctionEnvironment>(get_this_environment(interpreter.vm()));

    // 8. Perform ? thisER.BindThisValue(result).
    TRY(this_er.bind_this_value(global_object, result));

    // 9. Let F be thisER.[[FunctionObject]].
    // 10. Assert: F is an ECMAScript function object. (NOTE: This is implied by the strong C++ type.)
    [[maybe_unused]] auto& f = this_er.function_object();

    // 11. Perform ? InitializeInstanceElements(result, F).
    TRY(vm.initialize_instance_elements(*result, f));

    // 12. Return result.
    return Value { result };
}

// 15.5.5 Runtime Semantics: Evaluation, https://tc39.es/ecma262/#sec-generator-function-definitions-runtime-semantics-evaluation
Completion YieldExpression::execute(Interpreter&, GlobalObject&) const
{
    // This should be transformed to a return.
    VERIFY_NOT_REACHED();
}

// 15.8.5 Runtime Semantics: Evaluation, https://tc39.es/ecma262/#sec-async-function-definitions-runtime-semantics-evaluation
Completion AwaitExpression::execute(Interpreter& interpreter, GlobalObject& global_object) const
{
    InterpreterNodeScope node_scope { interpreter, *this };

    // 1. Let exprRef be the result of evaluating UnaryExpression.
    // 2. Let value be ? GetValue(exprRef).
    auto value = TRY(m_argument->execute(interpreter, global_object)).release_value();

    // 3. Return ? Await(value).
    return await(global_object, value);
}

// 14.10.1 Runtime Semantics: Evaluation, https://tc39.es/ecma262/#sec-return-statement-runtime-semantics-evaluation
Completion ReturnStatement::execute(Interpreter& interpreter, GlobalObject& global_object) const
{
    InterpreterNodeScope node_scope { interpreter, *this };

    // ReturnStatement : return ;
    if (!m_argument) {
        // 1. Return Completion { [[Type]]: return, [[Value]]: undefined, [[Target]]: empty }.
        return { Completion::Type::Return, js_undefined(), {} };
    }

    // ReturnStatement : return Expression ;
    // 1. Let exprRef be the result of evaluating Expression.
    // 2. Let exprValue be ? GetValue(exprRef).
    auto value = TRY(m_argument->execute(interpreter, global_object));

    // NOTE: Generators are not supported in the AST interpreter
    // 3. If ! GetGeneratorKind() is async, set exprValue to ? Await(exprValue).

    // 4. Return Completion { [[Type]]: return, [[Value]]: exprValue, [[Target]]: empty }.
    return { Completion::Type::Return, value, {} };
}

// 14.6.2 Runtime Semantics: Evaluation, https://tc39.es/ecma262/#sec-if-statement-runtime-semantics-evaluation
Completion IfStatement::execute(Interpreter& interpreter, GlobalObject& global_object) const
{
    InterpreterNodeScope node_scope { interpreter, *this };

    // IfStatement : if ( Expression ) Statement else Statement
    // 1. Let exprRef be the result of evaluating Expression.
    // 2. Let exprValue be ! ToBoolean(? GetValue(exprRef)).
    auto predicate_result = TRY(m_predicate->execute(interpreter, global_object)).release_value();

    // 3. If exprValue is true, then
    if (predicate_result.to_boolean()) {
        // a. Let stmtCompletion be the result of evaluating the first Statement.
        // 5. Return Completion(UpdateEmpty(stmtCompletion, undefined)).
        return m_consequent->execute(interpreter, global_object).update_empty(js_undefined());
    }

    // 4. Else,
    if (m_alternate) {
        // a. Let stmtCompletion be the result of evaluating the second Statement.
        // 5. Return Completion(UpdateEmpty(stmtCompletion, undefined)).
        return m_alternate->execute(interpreter, global_object).update_empty(js_undefined());
    }

    // IfStatement : if ( Expression ) Statement
    // 3. If exprValue is false, then
    //    a. Return NormalCompletion(undefined).
    return normal_completion(js_undefined());
}

// 14.11.2 Runtime Semantics: Evaluation, https://tc39.es/ecma262/#sec-with-statement-runtime-semantics-evaluation
// WithStatement : with ( Expression ) Statement
Completion WithStatement::execute(Interpreter& interpreter, GlobalObject& global_object) const
{
    InterpreterNodeScope node_scope { interpreter, *this };

    // 1. Let value be the result of evaluating Expression.
    auto value = TRY(m_object->execute(interpreter, global_object)).release_value();

    // 2. Let obj be ? ToObject(? GetValue(value)).
    auto* object = TRY(value.to_object(global_object));

    // 3. Let oldEnv be the running execution context's LexicalEnvironment.
    auto* old_environment = interpreter.vm().running_execution_context().lexical_environment;

    // 4. Let newEnv be NewObjectEnvironment(obj, true, oldEnv).
    auto* new_environment = new_object_environment(*object, true, old_environment);

    // 5. Set the running execution context's LexicalEnvironment to newEnv.
    interpreter.vm().running_execution_context().lexical_environment = new_environment;

    // 6. Let C be the result of evaluating Statement.
    auto result = m_body->execute(interpreter, global_object);

    // 7. Set the running execution context's LexicalEnvironment to oldEnv.
    interpreter.vm().running_execution_context().lexical_environment = old_environment;

    // 8. Return Completion(UpdateEmpty(C, undefined)).
    return result.update_empty(js_undefined());
}

// 14.7.1.1 LoopContinues ( completion, labelSet ), https://tc39.es/ecma262/#sec-loopcontinues
static bool loop_continues(Completion const& completion, Vector<FlyString> const& label_set)
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
Completion WhileStatement::execute(Interpreter& interpreter, GlobalObject& global_object) const
{
    // 1. Let newLabelSet be a new empty List.
    // 2. Return the result of performing LabelledEvaluation of this BreakableStatement with argument newLabelSet.
    return labelled_evaluation(interpreter, global_object, *this, {});
}

// 14.7.3.2 Runtime Semantics: WhileLoopEvaluation, https://tc39.es/ecma262/#sec-runtime-semantics-whileloopevaluation
Completion WhileStatement::loop_evaluation(Interpreter& interpreter, GlobalObject& global_object, Vector<FlyString> const& label_set) const
{
    InterpreterNodeScope node_scope { interpreter, *this };

    // 1. Let V be undefined.
    auto last_value = js_undefined();

    // 2. Repeat,
    for (;;) {
        // a. Let exprRef be the result of evaluating Expression.
        // b. Let exprValue be ? GetValue(exprRef).
        auto test_result = TRY(m_test->execute(interpreter, global_object)).release_value();

        // c. If ! ToBoolean(exprValue) is false, return NormalCompletion(V).
        if (!test_result.to_boolean())
            return normal_completion(last_value);

        // d. Let stmtResult be the result of evaluating Statement.
        auto body_result = m_body->execute(interpreter, global_object);

        // e. If LoopContinues(stmtResult, labelSet) is false, return Completion(UpdateEmpty(stmtResult, V)).
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
Completion DoWhileStatement::execute(Interpreter& interpreter, GlobalObject& global_object) const
{
    // 1. Let newLabelSet be a new empty List.
    // 2. Return the result of performing LabelledEvaluation of this BreakableStatement with argument newLabelSet.
    return labelled_evaluation(interpreter, global_object, *this, {});
}

// 14.7.2.2 Runtime Semantics: DoWhileLoopEvaluation, https://tc39.es/ecma262/#sec-runtime-semantics-dowhileloopevaluation
Completion DoWhileStatement::loop_evaluation(Interpreter& interpreter, GlobalObject& global_object, Vector<FlyString> const& label_set) const
{
    InterpreterNodeScope node_scope { interpreter, *this };

    // 1. Let V be undefined.
    auto last_value = js_undefined();

    // 2. Repeat,
    for (;;) {
        // a. Let stmtResult be the result of evaluating Statement.
        auto body_result = m_body->execute(interpreter, global_object);

        // b. If LoopContinues(stmtResult, labelSet) is false, return Completion(UpdateEmpty(stmtResult, V)).
        if (!loop_continues(body_result, label_set))
            return body_result.update_empty(last_value);

        // c. If stmtResult.[[Value]] is not empty, set V to stmtResult.[[Value]].
        if (body_result.value().has_value())
            last_value = *body_result.value();

        // d. Let exprRef be the result of evaluating Expression.
        // e. Let exprValue be ? GetValue(exprRef).
        auto test_result = TRY(m_test->execute(interpreter, global_object)).release_value();

        // f. If ! ToBoolean(exprValue) is false, return NormalCompletion(V).
        if (!test_result.to_boolean())
            return normal_completion(last_value);
    }

    VERIFY_NOT_REACHED();
}

// 14.1.1 Runtime Semantics: Evaluation, https://tc39.es/ecma262/#sec-statement-semantics-runtime-semantics-evaluation
// BreakableStatement : IterationStatement
Completion ForStatement::execute(Interpreter& interpreter, GlobalObject& global_object) const
{
    // 1. Let newLabelSet be a new empty List.
    // 2. Return the result of performing LabelledEvaluation of this BreakableStatement with argument newLabelSet.
    return labelled_evaluation(interpreter, global_object, *this, {});
}

// 14.7.4.2 Runtime Semantics: ForLoopEvaluation, https://tc39.es/ecma262/#sec-runtime-semantics-forloopevaluation
Completion ForStatement::loop_evaluation(Interpreter& interpreter, GlobalObject& global_object, Vector<FlyString> const& label_set) const
{
    InterpreterNodeScope node_scope { interpreter, *this };

    // Note we don't always set a new environment but to use RAII we must do this here.
    auto* old_environment = interpreter.lexical_environment();
    ScopeGuard restore_old_environment = [&] {
        interpreter.vm().running_execution_context().lexical_environment = old_environment;
    };

    Vector<FlyString> let_declarations;

    if (m_init) {
        if (is<VariableDeclaration>(*m_init) && static_cast<VariableDeclaration const&>(*m_init).declaration_kind() != DeclarationKind::Var) {
            auto* loop_environment = new_declarative_environment(*old_environment);
            auto& declaration = static_cast<VariableDeclaration const&>(*m_init);
            declaration.for_each_bound_name([&](auto const& name) {
                if (declaration.declaration_kind() == DeclarationKind::Const) {
                    MUST(loop_environment->create_immutable_binding(global_object, name, true));
                } else {
                    MUST(loop_environment->create_mutable_binding(global_object, name, false));
                    let_declarations.append(name);
                }
            });

            interpreter.vm().running_execution_context().lexical_environment = loop_environment;
        }

        (void)TRY(m_init->execute(interpreter, global_object));
    }

    // 14.7.4.4 CreatePerIterationEnvironment ( perIterationBindings ), https://tc39.es/ecma262/#sec-createperiterationenvironment
    auto create_per_iteration_environment = [&]() -> ThrowCompletionOr<void> {
        // 1. If perIterationBindings has any elements, then
        if (let_declarations.is_empty())
            return {};

        // a. Let lastIterationEnv be the running execution context's LexicalEnvironment.
        auto* last_iteration_env = interpreter.lexical_environment();

        // b. Let outer be lastIterationEnv.[[OuterEnv]].
        auto* outer = last_iteration_env->outer_environment();

        // c. Assert: outer is not null.
        VERIFY(outer);

        // d. Let thisIterationEnv be NewDeclarativeEnvironment(outer).
        auto* this_iteration_env = new_declarative_environment(*outer);

        // e. For each element bn of perIterationBindings, do
        for (auto& name : let_declarations) {
            // i. Perform ! thisIterationEnv.CreateMutableBinding(bn, false).
            MUST(this_iteration_env->create_mutable_binding(global_object, name, false));

            // ii. Let lastValue be ? lastIterationEnv.GetBindingValue(bn, true).
            auto last_value = TRY(last_iteration_env->get_binding_value(global_object, name, true));
            VERIFY(!last_value.is_empty());

            // iii. Perform thisIterationEnv.InitializeBinding(bn, lastValue).
            MUST(this_iteration_env->initialize_binding(global_object, name, last_value));
        }

        // f. Set the running execution context's LexicalEnvironment to thisIterationEnv.
        interpreter.vm().running_execution_context().lexical_environment = this_iteration_env;

        // 2. Return undefined.
        return {};
    };

    // 14.7.4.3 ForBodyEvaluation ( test, increment, stmt, perIterationBindings, labelSet ), https://tc39.es/ecma262/#sec-forbodyevaluation

    // 1. Let V be undefined.
    auto last_value = js_undefined();

    // 2. Perform ? CreatePerIterationEnvironment(perIterationBindings).
    TRY(create_per_iteration_environment());

    // 3. Repeat,
    while (true) {
        // a. If test is not [empty], then
        if (m_test) {
            // i. Let testRef be the result of evaluating test.
            // ii. Let testValue be ? GetValue(testRef).
            auto test_value = TRY(m_test->execute(interpreter, global_object)).release_value();

            // iii. If ! ToBoolean(testValue) is false, return NormalCompletion(V).
            if (!test_value.to_boolean())
                return normal_completion(last_value);
        }

        // b. Let result be the result of evaluating stmt.
        auto result = m_body->execute(interpreter, global_object);

        // c. If LoopContinues(result, labelSet) is false, return Completion(UpdateEmpty(result, V)).
        if (!loop_continues(result, label_set))
            return result.update_empty(last_value);

        // d. If result.[[Value]] is not empty, set V to result.[[Value]].
        if (result.value().has_value())
            last_value = *result.value();

        // e. Perform ? CreatePerIterationEnvironment(perIterationBindings).
        TRY(create_per_iteration_environment());

        // f. If increment is not [empty], then
        if (m_update) {
            // i. Let incRef be the result of evaluating increment.
            // ii. Perform ? GetValue(incRef).
            (void)TRY(m_update->execute(interpreter, global_object));
        }
    }

    VERIFY_NOT_REACHED();
}

struct ForInOfHeadState {
    explicit ForInOfHeadState(Variant<NonnullRefPtr<ASTNode>, NonnullRefPtr<BindingPattern>> lhs)
    {
        lhs.visit(
            [&](NonnullRefPtr<ASTNode>& ast_node) {
                expression_lhs = ast_node.ptr();
            },
            [&](NonnullRefPtr<BindingPattern>& pattern) {
                pattern_lhs = pattern.ptr();
                destructuring = true;
                lhs_kind = Assignment;
            });
    }

    ASTNode* expression_lhs = nullptr;
    BindingPattern* pattern_lhs = nullptr;
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
    ThrowCompletionOr<void> execute_head(Interpreter& interpreter, GlobalObject& global_object, Value next_value) const
    {
        VERIFY(!next_value.is_empty());

        Optional<Reference> lhs_reference;
        Environment* iteration_environment = nullptr;

        // g. If lhsKind is either assignment or varBinding, then
        if (lhs_kind == Assignment || lhs_kind == VarBinding) {
            if (!destructuring) {
                VERIFY(expression_lhs);
                if (is<VariableDeclaration>(*expression_lhs)) {
                    auto& declaration = static_cast<VariableDeclaration const&>(*expression_lhs);
                    VERIFY(declaration.declarations().first().target().has<NonnullRefPtr<Identifier>>());
                    lhs_reference = TRY(declaration.declarations().first().target().get<NonnullRefPtr<Identifier>>()->to_reference(interpreter, global_object));
                } else {
                    VERIFY(is<Identifier>(*expression_lhs) || is<MemberExpression>(*expression_lhs) || is<CallExpression>(*expression_lhs));
                    auto& expression = static_cast<Expression const&>(*expression_lhs);
                    lhs_reference = TRY(expression.to_reference(interpreter, global_object));
                }
            }
        }
        // h. Else,
        else {
            VERIFY(expression_lhs && is<VariableDeclaration>(*expression_lhs));
            iteration_environment = new_declarative_environment(*interpreter.lexical_environment());
            auto& for_declaration = static_cast<VariableDeclaration const&>(*expression_lhs);
            for_declaration.for_each_bound_name([&](auto const& name) {
                if (for_declaration.declaration_kind() == DeclarationKind::Const)
                    MUST(iteration_environment->create_immutable_binding(global_object, name, false));
                else
                    MUST(iteration_environment->create_mutable_binding(global_object, name, true));
            });
            interpreter.vm().running_execution_context().lexical_environment = iteration_environment;

            if (!destructuring) {
                VERIFY(for_declaration.declarations().first().target().has<NonnullRefPtr<Identifier>>());
                lhs_reference = MUST(interpreter.vm().resolve_binding(for_declaration.declarations().first().target().get<NonnullRefPtr<Identifier>>()->string()));
            }
        }

        // i. If destructuring is false, then
        if (!destructuring) {
            VERIFY(lhs_reference.has_value());
            if (lhs_kind == LexicalBinding)
                return lhs_reference->initialize_referenced_binding(global_object, next_value);
            else
                return lhs_reference->put_value(global_object, next_value);
        }

        // j. Else,
        if (lhs_kind == Assignment) {
            VERIFY(pattern_lhs);
            return interpreter.vm().destructuring_assignment_evaluation(*pattern_lhs, next_value, global_object);
        }
        VERIFY(expression_lhs && is<VariableDeclaration>(*expression_lhs));
        auto& for_declaration = static_cast<VariableDeclaration const&>(*expression_lhs);
        auto& binding_pattern = for_declaration.declarations().first().target().get<NonnullRefPtr<BindingPattern>>();
        VERIFY(lhs_kind == VarBinding || iteration_environment);

        // At this point iteration_environment is undefined if lhs_kind == VarBinding which means this does both
        // branch j.ii and j.iii because ForBindingInitialization is just a forwarding call to BindingInitialization.
        return interpreter.vm().binding_initialization(binding_pattern, next_value, iteration_environment, global_object);
    }
};

// 14.7.5.5 Runtime Semantics: ForInOfLoopEvaluation, https://tc39.es/ecma262/#sec-runtime-semantics-forinofloopevaluation
// 14.7.5.6 ForIn/OfHeadEvaluation ( uninitializedBoundNames, expr, iterationKind ), https://tc39.es/ecma262/#sec-runtime-semantics-forinofheadevaluation
// This method combines ForInOfLoopEvaluation and ForIn/OfHeadEvaluation for similar reason as ForIn/OfBodyEvaluation, to prevent code duplication.
// For the same reason we also skip step 6 and 7 of ForIn/OfHeadEvaluation as this is done by the appropriate for loop type.
static ThrowCompletionOr<ForInOfHeadState> for_in_of_head_execute(Interpreter& interpreter, GlobalObject& global_object, Variant<NonnullRefPtr<ASTNode>, NonnullRefPtr<BindingPattern>> lhs, Expression const& rhs)
{
    ForInOfHeadState state(lhs);
    if (auto* ast_ptr = lhs.get_pointer<NonnullRefPtr<ASTNode>>(); ast_ptr && is<VariableDeclaration>(*(*ast_ptr))) {
        // Runtime Semantics: ForInOfLoopEvaluation, for any of:
        //  ForInOfStatement : for ( var ForBinding in Expression ) Statement
        //  ForInOfStatement : for ( ForDeclaration in Expression ) Statement
        //  ForInOfStatement : for ( var ForBinding of AssignmentExpression ) Statement
        //  ForInOfStatement : for ( ForDeclaration of AssignmentExpression ) Statement

        // 14.7.5.6 ForIn/OfHeadEvaluation ( uninitializedBoundNames, expr, iterationKind ), https://tc39.es/ecma262/#sec-runtime-semantics-forinofheadevaluation
        Environment* new_environment = nullptr;

        auto& variable_declaration = static_cast<VariableDeclaration const&>(*(*ast_ptr));
        VERIFY(variable_declaration.declarations().size() == 1);
        state.destructuring = variable_declaration.declarations().first().target().has<NonnullRefPtr<BindingPattern>>();
        if (variable_declaration.declaration_kind() == DeclarationKind::Var) {
            state.lhs_kind = ForInOfHeadState::VarBinding;
            auto& variable = variable_declaration.declarations().first();
            // B.3.5 Initializers in ForIn Statement Heads, https://tc39.es/ecma262/#sec-initializers-in-forin-statement-heads
            if (variable.init()) {
                VERIFY(variable.target().has<NonnullRefPtr<Identifier>>());
                auto& binding_id = variable.target().get<NonnullRefPtr<Identifier>>()->string();
                auto reference = TRY(interpreter.vm().resolve_binding(binding_id));
                auto result = TRY(interpreter.vm().named_evaluation_if_anonymous_function(global_object, *variable.init(), binding_id));
                TRY(reference.put_value(global_object, result));
            }
        } else {
            state.lhs_kind = ForInOfHeadState::LexicalBinding;
            new_environment = new_declarative_environment(*interpreter.lexical_environment());
            variable_declaration.for_each_bound_name([&](auto const& name) {
                MUST(new_environment->create_mutable_binding(global_object, name, false));
            });
        }

        if (new_environment) {
            // 2.d Set the running execution context's LexicalEnvironment to newEnv.
            TemporaryChange<Environment*> scope_change(interpreter.vm().running_execution_context().lexical_environment, new_environment);

            // 3. Let exprRef be the result of evaluating expr.
            // 5. Let exprValue be ? GetValue(exprRef).
            state.rhs_value = TRY(rhs.execute(interpreter, global_object)).release_value();

            // Note that since a reference stores its environment it doesn't matter we only reset
            // this after step 5. (Also we have no way of separating these steps at this point)
            // 4. Set the running execution context's LexicalEnvironment to oldEnv.
        } else {
            // 3. Let exprRef be the result of evaluating expr.
            // 5. Let exprValue be ? GetValue(exprRef).
            state.rhs_value = TRY(rhs.execute(interpreter, global_object)).release_value();
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
    state.rhs_value = TRY(rhs.execute(interpreter, global_object)).release_value();
    return state;
}

// 14.1.1 Runtime Semantics: Evaluation, https://tc39.es/ecma262/#sec-statement-semantics-runtime-semantics-evaluation
// BreakableStatement : IterationStatement
Completion ForInStatement::execute(Interpreter& interpreter, GlobalObject& global_object) const
{
    // 1. Let newLabelSet be a new empty List.
    // 2. Return the result of performing LabelledEvaluation of this BreakableStatement with argument newLabelSet.
    return labelled_evaluation(interpreter, global_object, *this, {});
}

// 14.7.5.5 Runtime Semantics: ForInOfLoopEvaluation, https://tc39.es/ecma262/#sec-runtime-semantics-forinofloopevaluation
Completion ForInStatement::loop_evaluation(Interpreter& interpreter, GlobalObject& global_object, Vector<FlyString> const& label_set) const
{
    InterpreterNodeScope node_scope { interpreter, *this };

    auto for_in_head_state = TRY(for_in_of_head_execute(interpreter, global_object, m_lhs, *m_rhs));

    auto rhs_result = for_in_head_state.rhs_value;

    // 14.7.5.6 ForIn/OfHeadEvaluation ( uninitializedBoundNames, expr, iterationKind ), https://tc39.es/ecma262/#sec-runtime-semantics-forinofheadevaluation

    // a. If exprValue is undefined or null, then
    if (rhs_result.is_nullish()) {
        // i. Return Completion { [[Type]]: break, [[Value]]: empty, [[Target]]: empty }.
        return { Completion::Type::Break, {}, {} };
    }

    // b. Let obj be ! ToObject(exprValue).
    auto* object = MUST(rhs_result.to_object(global_object));

    // 14.7.5.7 ForIn/OfBodyEvaluation ( lhs, stmt, iteratorRecord, iterationKind, lhsKind, labelSet [ , iteratorKind ] ), https://tc39.es/ecma262/#sec-runtime-semantics-forin-div-ofbodyevaluation-lhs-stmt-iterator-lhskind-labelset

    // 2. Let oldEnv be the running execution context's LexicalEnvironment.
    Environment* old_environment = interpreter.lexical_environment();
    auto restore_scope = ScopeGuard([&] {
        interpreter.vm().running_execution_context().lexical_environment = old_environment;
    });

    // 3. Let V be undefined.
    auto last_value = js_undefined();

    while (object) {
        auto property_names = TRY(object->enumerable_own_property_names(Object::PropertyKind::Key));
        for (auto& value : property_names) {
            TRY(for_in_head_state.execute_head(interpreter, global_object, value));

            // l. Let result be the result of evaluating stmt.
            auto result = m_body->execute(interpreter, global_object);

            // m. Set the running execution context's LexicalEnvironment to oldEnv.
            interpreter.vm().running_execution_context().lexical_environment = old_environment;

            // n. If LoopContinues(result, labelSet) is false, then
            if (!loop_continues(result, label_set)) {
                // 1. Return Completion(UpdateEmpty(result, V)).
                return result.update_empty(last_value);
            }

            // o. If result.[[Value]] is not empty, set V to result.[[Value]].
            if (result.value().has_value())
                last_value = *result.value();
        }
        object = TRY(object->internal_get_prototype_of());
    }
    return last_value;
}

// 14.1.1 Runtime Semantics: Evaluation, https://tc39.es/ecma262/#sec-statement-semantics-runtime-semantics-evaluation
// BreakableStatement : IterationStatement
Completion ForOfStatement::execute(Interpreter& interpreter, GlobalObject& global_object) const
{
    // 1. Let newLabelSet be a new empty List.
    // 2. Return the result of performing LabelledEvaluation of this BreakableStatement with argument newLabelSet.
    return labelled_evaluation(interpreter, global_object, *this, {});
}

// 14.7.5.5 Runtime Semantics: ForInOfLoopEvaluation, https://tc39.es/ecma262/#sec-runtime-semantics-forinofloopevaluation
Completion ForOfStatement::loop_evaluation(Interpreter& interpreter, GlobalObject& global_object, Vector<FlyString> const& label_set) const
{
    InterpreterNodeScope node_scope { interpreter, *this };

    auto for_of_head_state = TRY(for_in_of_head_execute(interpreter, global_object, m_lhs, m_rhs));

    auto rhs_result = for_of_head_state.rhs_value;

    // 14.7.5.7 ForIn/OfBodyEvaluation ( lhs, stmt, iteratorRecord, iterationKind, lhsKind, labelSet [ , iteratorKind ] ), https://tc39.es/ecma262/#sec-runtime-semantics-forin-div-ofbodyevaluation-lhs-stmt-iterator-lhskind-labelset
    // We use get_iterator_values which behaves like ForIn/OfBodyEvaluation with iteratorKind iterate.

    // 2. Let oldEnv be the running execution context's LexicalEnvironment.
    Environment* old_environment = interpreter.lexical_environment();
    auto restore_scope = ScopeGuard([&] {
        interpreter.vm().running_execution_context().lexical_environment = old_environment;
    });

    // 3. Let V be undefined.
    auto last_value = js_undefined();

    Optional<Completion> status;

    (void)TRY(get_iterator_values(global_object, rhs_result, [&](Value value) -> Optional<Completion> {
        TRY(for_of_head_state.execute_head(interpreter, global_object, value));

        // l. Let result be the result of evaluating stmt.
        auto result = m_body->execute(interpreter, global_object);

        // m. Set the running execution context's LexicalEnvironment to oldEnv.
        interpreter.vm().running_execution_context().lexical_environment = old_environment;

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
    // e. If done is true, return NormalCompletion(V).
    return status.value_or(normal_completion(last_value));
}

// 14.1.1 Runtime Semantics: Evaluation, https://tc39.es/ecma262/#sec-statement-semantics-runtime-semantics-evaluation
// BreakableStatement : IterationStatement
Completion ForAwaitOfStatement::execute(Interpreter& interpreter, GlobalObject& global_object) const
{
    // 1. Let newLabelSet be a new empty List.
    // 2. Return the result of performing LabelledEvaluation of this BreakableStatement with argument newLabelSet.
    return labelled_evaluation(interpreter, global_object, *this, {});
}

// 14.7.5.5 Runtime Semantics: ForInOfLoopEvaluation, https://tc39.es/ecma262/#sec-runtime-semantics-forinofloopevaluation
Completion ForAwaitOfStatement::loop_evaluation(Interpreter& interpreter, GlobalObject& global_object, Vector<FlyString> const& label_set) const
{
    InterpreterNodeScope node_scope { interpreter, *this };

    // 14.7.5.6 ForIn/OfHeadEvaluation ( uninitializedBoundNames, expr, iterationKind ), https://tc39.es/ecma262/#sec-runtime-semantics-forinofheadevaluation
    // Note: Performs only steps 1 through 5.
    auto for_of_head_state = TRY(for_in_of_head_execute(interpreter, global_object, m_lhs, m_rhs));

    auto rhs_result = for_of_head_state.rhs_value;

    // NOTE: Perform step 7 from ForIn/OfHeadEvaluation. And since this is always async we only have to do step 7.d.
    // d. Return ? GetIterator(exprValue, iteratorHint).
    auto iterator = TRY(get_iterator(global_object, rhs_result, IteratorHint::Async));

    auto& vm = interpreter.vm();

    // 14.7.5.7 ForIn/OfBodyEvaluation ( lhs, stmt, iteratorRecord, iterationKind, lhsKind, labelSet [ , iteratorKind ] ), https://tc39.es/ecma262/#sec-runtime-semantics-forin-div-ofbodyevaluation-lhs-stmt-iterator-lhskind-labelset
    // NOTE: Here iteratorKind is always async.
    // 2. Let oldEnv be the running execution context's LexicalEnvironment.
    Environment* old_environment = interpreter.lexical_environment();
    auto restore_scope = ScopeGuard([&] {
        interpreter.vm().running_execution_context().lexical_environment = old_environment;
    });
    // 3. Let V be undefined.
    auto last_value = js_undefined();

    // NOTE: Step 4 and 5 are just extracting properties from the head which is done already in for_in_of_head_execute.
    //       And these are only used in step 6.g through 6.k which is done with for_of_head_state.execute_head.

    // 6. Repeat,
    while (true) {
        // a. Let nextResult be ? Call(iteratorRecord.[[NextMethod]], iteratorRecord.[[Iterator]]).
        auto next_result = TRY(call(global_object, iterator.next_method, iterator.iterator));

        // b. If iteratorKind is async, set nextResult to ? Await(nextResult).
        next_result = TRY(await(global_object, next_result));

        // c. If Type(nextResult) is not Object, throw a TypeError exception.
        if (!next_result.is_object())
            return vm.throw_completion<TypeError>(global_object, ErrorType::IterableNextBadReturn);

        // d. Let done be ? IteratorComplete(nextResult).
        auto done = TRY(iterator_complete(global_object, next_result.as_object()));

        // e. If done is true, return NormalCompletion(V).
        if (done)
            return last_value;

        // f. Let nextValue be ? IteratorValue(nextResult).
        auto next_value = TRY(iterator_value(global_object, next_result.as_object()));

        // NOTE: This performs steps g. through to k.
        TRY(for_of_head_state.execute_head(interpreter, global_object, next_value));

        // l. Let result be the result of evaluating stmt.
        auto result = m_body->execute(interpreter, global_object);

        // m. Set the running execution context's LexicalEnvironment to oldEnv.
        interpreter.vm().running_execution_context().lexical_environment = old_environment;

        // n. If LoopContinues(result, labelSet) is false, then
        if (!loop_continues(result, label_set)) {
            // 2. Set status to UpdateEmpty(result, V).
            auto status = result.update_empty(last_value);

            // 3. If iteratorKind is async, return ? AsyncIteratorClose(iteratorRecord, status).
            return async_iterator_close(global_object, iterator, move(status));
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
Completion BinaryExpression::execute(Interpreter& interpreter, GlobalObject& global_object) const
{
    InterpreterNodeScope node_scope { interpreter, *this };

    // Special case in which we cannot execute the lhs.  RelationalExpression : PrivateIdentifier in ShiftExpression
    //  RelationalExpression : PrivateIdentifier in ShiftExpression, https://tc39.es/ecma262/#sec-relational-operators-runtime-semantics-evaluation
    if (m_op == BinaryOp::In && is<PrivateIdentifier>(*m_lhs)) {
        auto& private_identifier = static_cast<PrivateIdentifier const&>(*m_lhs).string();

        auto rhs_result = TRY(m_rhs->execute(interpreter, global_object)).release_value();
        if (!rhs_result.is_object())
            return interpreter.vm().throw_completion<TypeError>(global_object, ErrorType::InOperatorWithObject);
        auto* private_environment = interpreter.vm().running_execution_context().private_environment;
        VERIFY(private_environment);
        auto private_name = private_environment->resolve_private_identifier(private_identifier);
        return Value(rhs_result.as_object().private_element_find(private_name) != nullptr);
    }

    auto lhs_result = TRY(m_lhs->execute(interpreter, global_object)).release_value();
    auto rhs_result = TRY(m_rhs->execute(interpreter, global_object)).release_value();

    switch (m_op) {
    case BinaryOp::Addition:
        return TRY(add(global_object, lhs_result, rhs_result));
    case BinaryOp::Subtraction:
        return TRY(sub(global_object, lhs_result, rhs_result));
    case BinaryOp::Multiplication:
        return TRY(mul(global_object, lhs_result, rhs_result));
    case BinaryOp::Division:
        return TRY(div(global_object, lhs_result, rhs_result));
    case BinaryOp::Modulo:
        return TRY(mod(global_object, lhs_result, rhs_result));
    case BinaryOp::Exponentiation:
        return TRY(exp(global_object, lhs_result, rhs_result));
    case BinaryOp::StrictlyEquals:
        return Value(is_strictly_equal(lhs_result, rhs_result));
    case BinaryOp::StrictlyInequals:
        return Value(!is_strictly_equal(lhs_result, rhs_result));
    case BinaryOp::LooselyEquals:
        return Value(TRY(is_loosely_equal(global_object, lhs_result, rhs_result)));
    case BinaryOp::LooselyInequals:
        return Value(!TRY(is_loosely_equal(global_object, lhs_result, rhs_result)));
    case BinaryOp::GreaterThan:
        return TRY(greater_than(global_object, lhs_result, rhs_result));
    case BinaryOp::GreaterThanEquals:
        return TRY(greater_than_equals(global_object, lhs_result, rhs_result));
    case BinaryOp::LessThan:
        return TRY(less_than(global_object, lhs_result, rhs_result));
    case BinaryOp::LessThanEquals:
        return TRY(less_than_equals(global_object, lhs_result, rhs_result));
    case BinaryOp::BitwiseAnd:
        return TRY(bitwise_and(global_object, lhs_result, rhs_result));
    case BinaryOp::BitwiseOr:
        return TRY(bitwise_or(global_object, lhs_result, rhs_result));
    case BinaryOp::BitwiseXor:
        return TRY(bitwise_xor(global_object, lhs_result, rhs_result));
    case BinaryOp::LeftShift:
        return TRY(left_shift(global_object, lhs_result, rhs_result));
    case BinaryOp::RightShift:
        return TRY(right_shift(global_object, lhs_result, rhs_result));
    case BinaryOp::UnsignedRightShift:
        return TRY(unsigned_right_shift(global_object, lhs_result, rhs_result));
    case BinaryOp::In:
        return TRY(in(global_object, lhs_result, rhs_result));
    case BinaryOp::InstanceOf:
        return TRY(instance_of(global_object, lhs_result, rhs_result));
    }

    VERIFY_NOT_REACHED();
}

// 13.13.1 Runtime Semantics: Evaluation, https://tc39.es/ecma262/#sec-binary-logical-operators-runtime-semantics-evaluation
Completion LogicalExpression::execute(Interpreter& interpreter, GlobalObject& global_object) const
{
    InterpreterNodeScope node_scope { interpreter, *this };

    // 1. Let lref be the result of evaluating <Expression>.
    // 2. Let lval be ? GetValue(lref).
    auto lhs_result = TRY(m_lhs->execute(interpreter, global_object)).release_value();

    switch (m_op) {
    // LogicalANDExpression : LogicalANDExpression && BitwiseORExpression
    case LogicalOp::And:
        // 3. Let lbool be ! ToBoolean(lval).
        // 4. If lbool is false, return lval.
        if (!lhs_result.to_boolean())
            return lhs_result;

        // 5. Let rref be the result of evaluating BitwiseORExpression.
        // 6. Return ? GetValue(rref).
        return m_rhs->execute(interpreter, global_object);

    // LogicalORExpression : LogicalORExpression || LogicalANDExpression
    case LogicalOp::Or:
        // 3. Let lbool be ! ToBoolean(lval).
        // 4. If lbool is true, return lval.
        if (lhs_result.to_boolean())
            return lhs_result;

        // 5. Let rref be the result of evaluating LogicalANDExpression.
        // 6. Return ? GetValue(rref).
        return m_rhs->execute(interpreter, global_object);

    // CoalesceExpression : CoalesceExpressionHead ?? BitwiseORExpression
    case LogicalOp::NullishCoalescing:
        // 3. If lval is undefined or null, then
        if (lhs_result.is_nullish()) {
            // a. Let rref be the result of evaluating BitwiseORExpression.
            // b. Return ? GetValue(rref).
            return m_rhs->execute(interpreter, global_object);
        }

        // 4. Otherwise, return lval.
        return lhs_result;
    }

    VERIFY_NOT_REACHED();
}

ThrowCompletionOr<Reference> Expression::to_reference(Interpreter&, GlobalObject&) const
{
    return Reference {};
}

ThrowCompletionOr<Reference> Identifier::to_reference(Interpreter& interpreter, GlobalObject&) const
{
    if (m_cached_environment_coordinate.has_value()) {
        auto* environment = interpreter.vm().running_execution_context().lexical_environment;
        for (size_t i = 0; i < m_cached_environment_coordinate->hops; ++i)
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
        m_cached_environment_coordinate = reference.environment_coordinate();
    return reference;
}

ThrowCompletionOr<Reference> MemberExpression::to_reference(Interpreter& interpreter, GlobalObject& global_object) const
{
    // 13.3.7.1 Runtime Semantics: Evaluation
    // SuperProperty : super [ Expression ]
    // SuperProperty : super . IdentifierName
    // https://tc39.es/ecma262/#sec-super-keyword-runtime-semantics-evaluation
    if (is<SuperExpression>(object())) {
        // 1. Let env be GetThisEnvironment().
        auto& environment = get_this_environment(interpreter.vm());
        // 2. Let actualThis be ? env.GetThisBinding().
        auto actual_this = TRY(environment.get_this_binding(global_object));

        PropertyKey property_key;

        if (is_computed()) {
            // SuperProperty : super [ Expression ]

            // 3. Let propertyNameReference be the result of evaluating Expression.
            // 4. Let propertyNameValue be ? GetValue(propertyNameReference).
            auto property_name_value = TRY(m_property->execute(interpreter, global_object)).release_value();

            // 5. Let propertyKey be ? ToPropertyKey(propertyNameValue).
            property_key = TRY(property_name_value.to_property_key(global_object));
        } else {
            // SuperProperty : super . IdentifierName

            // 3. Let propertyKey be StringValue of IdentifierName.
            VERIFY(is<Identifier>(property()));
            property_key = static_cast<Identifier const&>(property()).string();
        }

        // 6. If the code matched by this SuperProperty is strict mode code, let strict be true; else let strict be false.
        bool strict = interpreter.vm().in_strict_mode();

        // 7. Return ? MakeSuperPropertyReference(actualThis, propertyKey, strict).
        return TRY(make_super_property_reference(global_object, actual_this, property_key, strict));
    }

    auto base_reference = TRY(m_object->to_reference(interpreter, global_object));

    Value base_value;

    if (base_reference.is_valid_reference())
        base_value = TRY(base_reference.get_value(global_object));
    else
        base_value = TRY(m_object->execute(interpreter, global_object)).release_value();

    VERIFY(!base_value.is_empty());

    // From here on equivalent to
    // 13.3.4 EvaluatePropertyAccessWithIdentifierKey ( baseValue, identifierName, strict ), https://tc39.es/ecma262/#sec-evaluate-property-access-with-identifier-key
    PropertyKey property_key;
    if (is_computed()) {
        // Weird order which I can't quite find from the specs.
        auto value = TRY(m_property->execute(interpreter, global_object)).release_value();
        VERIFY(!value.is_empty());

        TRY(require_object_coercible(global_object, base_value));

        property_key = TRY(PropertyKey::from_value(global_object, value));
    } else if (is<PrivateIdentifier>(*m_property)) {
        auto& private_identifier = static_cast<PrivateIdentifier const&>(*m_property);
        return make_private_reference(interpreter.vm(), base_value, private_identifier.string());
    } else {
        property_key = verify_cast<Identifier>(*m_property).string();
        TRY(require_object_coercible(global_object, base_value));
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
Completion UnaryExpression::execute(Interpreter& interpreter, GlobalObject& global_object) const
{
    InterpreterNodeScope node_scope { interpreter, *this };

    auto& vm = interpreter.vm();
    if (m_op == UnaryOp::Delete) {
        auto reference = TRY(m_lhs->to_reference(interpreter, global_object));
        return Value(TRY(reference.delete_(global_object)));
    }

    Value lhs_result;
    if (m_op == UnaryOp::Typeof && is<Identifier>(*m_lhs)) {
        auto reference = TRY(m_lhs->to_reference(interpreter, global_object));

        if (reference.is_unresolvable())
            lhs_result = js_undefined();
        else
            lhs_result = TRY(reference.get_value(global_object));
        VERIFY(!lhs_result.is_empty());
    } else {
        // 1. Let expr be the result of evaluating UnaryExpression.
        lhs_result = TRY(m_lhs->execute(interpreter, global_object)).release_value();
    }

    switch (m_op) {
    case UnaryOp::BitwiseNot:
        return TRY(bitwise_not(global_object, lhs_result));
    case UnaryOp::Not:
        return Value(!lhs_result.to_boolean());
    case UnaryOp::Plus:
        return TRY(unary_plus(global_object, lhs_result));
    case UnaryOp::Minus:
        return TRY(unary_minus(global_object, lhs_result));
    case UnaryOp::Typeof:
        return Value { js_string(vm, lhs_result.typeof()) };
    case UnaryOp::Void:
        return js_undefined();
    case UnaryOp::Delete:
        VERIFY_NOT_REACHED();
    }

    VERIFY_NOT_REACHED();
}

Completion SuperExpression::execute(Interpreter&, GlobalObject&) const
{
    // The semantics for SuperExpression are handled in CallExpression and SuperCall.
    VERIFY_NOT_REACHED();
}

Completion ClassElement::execute(Interpreter&, GlobalObject&) const
{
    // Note: The semantics of class element are handled in class_element_evaluation
    VERIFY_NOT_REACHED();
}

static ThrowCompletionOr<ClassElement::ClassElementName> class_key_to_property_name(Interpreter& interpreter, GlobalObject& global_object, Expression const& key)
{
    if (is<PrivateIdentifier>(key)) {
        auto& private_identifier = static_cast<PrivateIdentifier const&>(key);
        auto* private_environment = interpreter.vm().running_execution_context().private_environment;
        VERIFY(private_environment);
        return ClassElement::ClassElementName { private_environment->resolve_private_identifier(private_identifier.string()) };
    }

    auto prop_key = TRY(key.execute(interpreter, global_object)).release_value();

    if (prop_key.is_object())
        prop_key = TRY(prop_key.to_primitive(global_object, Value::PreferredType::String));

    auto property_key = TRY(PropertyKey::from_value(global_object, prop_key));
    return ClassElement::ClassElementName { property_key };
}

// 15.4.5 Runtime Semantics: MethodDefinitionEvaluation, https://tc39.es/ecma262/#sec-runtime-semantics-methoddefinitionevaluation
ThrowCompletionOr<ClassElement::ClassValue> ClassMethod::class_element_evaluation(Interpreter& interpreter, GlobalObject& global_object, Object& target) const
{
    auto property_key_or_private_name = TRY(class_key_to_property_name(interpreter, global_object, *m_key));

    auto method_value = TRY(m_function->execute(interpreter, global_object)).release_value();

    auto function_handle = make_handle(&method_value.as_function());

    auto& method_function = static_cast<ECMAScriptFunctionObject&>(method_value.as_function());
    method_function.make_method(target);

    auto set_function_name = [&](String prefix = "") {
        auto name = property_key_or_private_name.visit(
            [&](PropertyKey const& property_key) -> String {
                if (property_key.is_symbol()) {
                    auto description = property_key.as_symbol()->description();
                    if (description.is_empty())
                        return "";
                    return String::formatted("[{}]", description);
                } else {
                    return property_key.to_string();
                }
            },
            [&](PrivateName const& private_name) -> String {
                return private_name.description;
            });

        update_function_name(method_value, String::formatted("{}{}{}", prefix, prefix.is_empty() ? "" : " ", name));
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
            return ClassValue { PrivateElement { private_name, PrivateElement::Kind::Method, method_value } };
        case Kind::Getter:
            set_function_name("get");
            return ClassValue { PrivateElement { private_name, PrivateElement::Kind::Accessor, Accessor::create(interpreter.vm(), &method_function, nullptr) } };
        case Kind::Setter:
            set_function_name("set");
            return ClassValue { PrivateElement { private_name, PrivateElement::Kind::Accessor, Accessor::create(interpreter.vm(), nullptr, &method_function) } };
        default:
            VERIFY_NOT_REACHED();
        }
    }
}

// We use this class to mimic  Initializer : = AssignmentExpression of
// 10.2.1.3 Runtime Semantics: EvaluateBody, https://tc39.es/ecma262/#sec-runtime-semantics-evaluatebody
class ClassFieldInitializerStatement : public Statement {
public:
    ClassFieldInitializerStatement(SourceRange source_range, NonnullRefPtr<Expression> expression, FlyString field_name)
        : Statement(source_range)
        , m_expression(move(expression))
        , m_class_field_identifier_name(move(field_name))
    {
    }

    Completion execute(Interpreter& interpreter, GlobalObject& global_object) const override
    {
        // 1. Assert: argumentsList is empty.
        VERIFY(interpreter.vm().argument_count() == 0);

        // 2. Assert: functionObject.[[ClassFieldInitializerName]] is not empty.
        VERIFY(!m_class_field_identifier_name.is_empty());

        // 3. If IsAnonymousFunctionDefinition(AssignmentExpression) is true, then
        //    a. Let value be NamedEvaluation of Initializer with argument functionObject.[[ClassFieldInitializerName]].
        // 4. Else,
        //    a. Let rhs be the result of evaluating AssignmentExpression.
        //    b. Let value be ? GetValue(rhs).
        auto value = TRY(interpreter.vm().named_evaluation_if_anonymous_function(global_object, m_expression, m_class_field_identifier_name));

        // 5. Return Completion { [[Type]]: return, [[Value]]: value, [[Target]]: empty }.
        return { Completion::Type::Return, value, {} };
    }

    void dump(int) const override
    {
        // This should not be dumped as it is never part of an actual AST.
        VERIFY_NOT_REACHED();
    }

private:
    NonnullRefPtr<Expression> m_expression;
    FlyString m_class_field_identifier_name; // [[ClassFieldIdentifierName]]
};

// 15.7.10 Runtime Semantics: ClassFieldDefinitionEvaluation, https://tc39.es/ecma262/#sec-runtime-semantics-classfielddefinitionevaluation
ThrowCompletionOr<ClassElement::ClassValue> ClassField::class_element_evaluation(Interpreter& interpreter, GlobalObject& global_object, Object& target) const
{
    auto property_key_or_private_name = TRY(class_key_to_property_name(interpreter, global_object, *m_key));
    Handle<ECMAScriptFunctionObject> initializer {};
    if (m_initializer) {
        auto copy_initializer = m_initializer;
        auto name = property_key_or_private_name.visit(
            [&](PropertyKey const& property_key) -> String {
                return property_key.is_number() ? property_key.to_string() : property_key.to_string_or_symbol().to_display_string();
            },
            [&](PrivateName const& private_name) -> String {
                return private_name.description;
            });

        // FIXME: A potential optimization is not creating the functions here since these are never directly accessible.
        auto function_code = create_ast_node<ClassFieldInitializerStatement>(m_initializer->source_range(), copy_initializer.release_nonnull(), name);
        initializer = make_handle(ECMAScriptFunctionObject::create(interpreter.global_object(), String::empty(), String::empty(), *function_code, {}, 0, interpreter.lexical_environment(), interpreter.vm().running_execution_context().private_environment, FunctionKind::Normal, true, false, m_contains_direct_call_to_eval, false));
        initializer->make_method(target);
    }

    return ClassValue {
        ClassFieldDefinition {
            move(property_key_or_private_name),
            move(initializer),
        }
    };
}

static Optional<FlyString> nullopt_or_private_identifier_description(Expression const& expression)
{
    if (is<PrivateIdentifier>(expression))
        return static_cast<PrivateIdentifier const&>(expression).string();
    return {};
}

Optional<FlyString> ClassField::private_bound_identifier() const
{
    return nullopt_or_private_identifier_description(*m_key);
}

Optional<FlyString> ClassMethod::private_bound_identifier() const
{
    return nullopt_or_private_identifier_description(*m_key);
}

// 15.7.11 Runtime Semantics: ClassStaticBlockDefinitionEvaluation, https://tc39.es/ecma262/#sec-runtime-semantics-classstaticblockdefinitionevaluation
ThrowCompletionOr<ClassElement::ClassValue> StaticInitializer::class_element_evaluation(Interpreter& interpreter, GlobalObject& global_object, Object& home_object) const
{
    // 1. Let lex be the running execution context's LexicalEnvironment.
    auto* lexical_environment = interpreter.vm().running_execution_context().lexical_environment;

    // 2. Let privateScope be the running execution context's PrivateEnvironment.
    auto* private_scope = interpreter.vm().running_execution_context().private_environment;

    // 3. Let sourceText be the empty sequence of Unicode code points.
    // 4. Let formalParameters be an instance of the production FormalParameters : [empty] .
    // 5. Let bodyFunction be OrdinaryFunctionCreate(%Function.prototype%, sourceText, formalParameters, ClassStaticBlockBody, non-lexical-this, lex, privateScope).
    // Note: The function bodyFunction is never directly accessible to ECMAScript code.
    auto* body_function = ECMAScriptFunctionObject::create(global_object, String::empty(), String::empty(), *m_function_body, {}, 0, lexical_environment, private_scope, FunctionKind::Normal, true, false, m_contains_direct_call_to_eval, false);

    // 6. Perform MakeMethod(bodyFunction, homeObject).
    body_function->make_method(home_object);

    // 7. Return the ClassStaticBlockDefinition Record { [[BodyFunction]]: bodyFunction }.
    return ClassValue { normal_completion(body_function) };
}

// 15.7.16 Runtime Semantics: Evaluation, https://tc39.es/ecma262/#sec-class-definitions-runtime-semantics-evaluation
// ClassExpression : class BindingIdentifier ClassTail
Completion ClassExpression::execute(Interpreter& interpreter, GlobalObject& global_object) const
{
    InterpreterNodeScope node_scope { interpreter, *this };

    // 1. Let className be StringValue of BindingIdentifier.
    // 2. Let value be ? ClassDefinitionEvaluation of ClassTail with arguments className and className.
    auto* value = TRY(class_definition_evaluation(interpreter, global_object, m_name, m_name.is_null() ? "" : m_name));

    // 3. Set value.[[SourceText]] to the source text matched by ClassExpression.
    value->set_source_text(m_source_text);

    // 4. Return value.
    return Value { value };
}

// 15.7.15 Runtime Semantics: BindingClassDeclarationEvaluation, https://tc39.es/ecma262/#sec-runtime-semantics-bindingclassdeclarationevaluation
static ThrowCompletionOr<Value> binding_class_declaration_evaluation(Interpreter& interpreter, GlobalObject& global_object, ClassExpression const& class_expression)
{
    // ClassDeclaration : class ClassTail
    if (!class_expression.has_name()) {
        // 1. Let value be ? ClassDefinitionEvaluation of ClassTail with arguments undefined and "default".
        auto value = TRY(class_expression.class_definition_evaluation(interpreter, global_object, {}, "default"));

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
    auto value = TRY(class_expression.class_definition_evaluation(interpreter, global_object, class_name, class_name));

    // 3. Set value.[[SourceText]] to the source text matched by ClassDeclaration.
    value->set_source_text(class_expression.source_text());

    // 4. Let env be the running execution context's LexicalEnvironment.
    auto* env = interpreter.lexical_environment();

    // 5. Perform ? InitializeBoundName(className, value, env).
    TRY(initialize_bound_name(global_object, class_name, value, env));

    // 6. Return value.
    return value;
}

// 15.7.16 Runtime Semantics: Evaluation, https://tc39.es/ecma262/#sec-class-definitions-runtime-semantics-evaluation
// ClassDeclaration : class BindingIdentifier ClassTail
Completion ClassDeclaration::execute(Interpreter& interpreter, GlobalObject& global_object) const
{
    InterpreterNodeScope node_scope { interpreter, *this };

    // 1. Perform ? BindingClassDeclarationEvaluation of this ClassDeclaration.
    (void)TRY(binding_class_declaration_evaluation(interpreter, global_object, m_class_expression));

    // 2. Return NormalCompletion(empty).
    return normal_completion({});
}

// 15.7.14 Runtime Semantics: ClassDefinitionEvaluation, https://tc39.es/ecma262/#sec-runtime-semantics-classdefinitionevaluation
ThrowCompletionOr<ECMAScriptFunctionObject*> ClassExpression::class_definition_evaluation(Interpreter& interpreter, GlobalObject& global_object, FlyString const& binding_name, FlyString const& class_name) const
{
    auto& vm = interpreter.vm();
    auto* environment = vm.lexical_environment();
    VERIFY(environment);
    auto* class_scope = new_declarative_environment(*environment);

    // We might not set the lexical environment but we always want to restore it eventually.
    ArmedScopeGuard restore_environment = [&] {
        vm.running_execution_context().lexical_environment = environment;
    };

    if (!binding_name.is_null())
        MUST(class_scope->create_immutable_binding(global_object, binding_name, true));

    auto* outer_private_environment = vm.running_execution_context().private_environment;
    auto* class_private_environment = new_private_environment(vm, outer_private_environment);

    for (auto const& element : m_elements) {
        auto opt_private_name = element.private_bound_identifier();
        if (opt_private_name.has_value())
            class_private_environment->add_private_name({}, opt_private_name.release_value());
    }

    auto* proto_parent = vm.current_realm()->global_object().object_prototype();

    auto* constructor_parent = vm.current_realm()->global_object().function_prototype();

    if (!m_super_class.is_null()) {
        vm.running_execution_context().lexical_environment = class_scope;

        // Note: Since our execute does evaluation and GetValue in once we must check for a valid reference first

        Value super_class;

        auto reference = TRY(m_super_class->to_reference(interpreter, global_object));
        if (reference.is_valid_reference()) {
            super_class = TRY(reference.get_value(global_object));
        } else {
            super_class = TRY(m_super_class->execute(interpreter, global_object)).release_value();
        }
        vm.running_execution_context().lexical_environment = environment;

        if (super_class.is_null()) {
            proto_parent = nullptr;
        } else if (!super_class.is_constructor()) {
            return vm.throw_completion<TypeError>(global_object, ErrorType::ClassExtendsValueNotAConstructorOrNull, super_class.to_string_without_side_effects());
        } else {
            auto super_class_prototype = TRY(super_class.get(global_object, vm.names.prototype));
            if (!super_class_prototype.is_null() && !super_class_prototype.is_object())
                return vm.throw_completion<TypeError>(global_object, ErrorType::ClassExtendsValueInvalidPrototype, super_class_prototype.to_string_without_side_effects());

            if (super_class_prototype.is_null())
                proto_parent = nullptr;
            else
                proto_parent = &super_class_prototype.as_object();

            constructor_parent = &super_class.as_object();
        }
    }

    auto* prototype = Object::create(global_object, proto_parent);
    VERIFY(prototype);

    vm.running_execution_context().lexical_environment = class_scope;
    vm.running_execution_context().private_environment = class_private_environment;
    ScopeGuard restore_private_environment = [&] {
        vm.running_execution_context().private_environment = outer_private_environment;
    };

    // FIXME: Step 14.a is done in the parser. But maybe it shouldn't?
    auto class_constructor_value = TRY(m_constructor->execute(interpreter, global_object)).release_value();

    update_function_name(class_constructor_value, class_name);

    VERIFY(class_constructor_value.is_function() && is<ECMAScriptFunctionObject>(class_constructor_value.as_function()));
    auto* class_constructor = static_cast<ECMAScriptFunctionObject*>(&class_constructor_value.as_function());
    class_constructor->set_home_object(prototype);
    class_constructor->set_is_class_constructor();
    class_constructor->define_direct_property(vm.names.prototype, prototype, Attribute::Writable);
    TRY(class_constructor->internal_set_prototype_of(constructor_parent));

    if (!m_super_class.is_null())
        class_constructor->set_constructor_kind(ECMAScriptFunctionObject::ConstructorKind::Derived);

    prototype->define_direct_property(vm.names.constructor, class_constructor, Attribute::Writable | Attribute::Configurable);

    using StaticElement = Variant<ClassElement::ClassFieldDefinition, Handle<ECMAScriptFunctionObject>>;

    Vector<PrivateElement> static_private_methods;
    Vector<PrivateElement> instance_private_methods;
    Vector<ClassElement::ClassFieldDefinition> instance_fields;
    Vector<StaticElement> static_elements;

    for (auto const& element : m_elements) {
        // Note: All ClassElementEvaluation start with evaluating the name (or we fake it).
        auto element_value = TRY(element.class_element_evaluation(interpreter, global_object, element.is_static() ? *class_constructor : *prototype));

        if (element_value.has<PrivateElement>()) {
            auto& container = element.is_static() ? static_private_methods : instance_private_methods;

            auto& private_element = element_value.get<PrivateElement>();

            auto added_to_existing = false;
            // FIXME: We can skip this loop in most cases.
            for (auto& existing : container) {
                if (existing.key == private_element.key) {
                    VERIFY(existing.kind == PrivateElement::Kind::Accessor);
                    VERIFY(private_element.kind == PrivateElement::Kind::Accessor);
                    auto& accessor = private_element.value.as_accessor();
                    if (!accessor.getter())
                        existing.value.as_accessor().set_setter(accessor.setter());
                    else
                        existing.value.as_accessor().set_getter(accessor.getter());
                    added_to_existing = true;
                }
            }

            if (!added_to_existing)
                container.append(move(element_value.get<PrivateElement>()));
        } else if (auto* class_field_definition_ptr = element_value.get_pointer<ClassElement::ClassFieldDefinition>()) {
            if (element.is_static())
                static_elements.append(move(*class_field_definition_ptr));
            else
                instance_fields.append(move(*class_field_definition_ptr));
        } else if (element.class_element_kind() == ClassElement::ElementKind::StaticInitializer) {
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
        MUST(class_scope->initialize_binding(global_object, binding_name, class_constructor));

    for (auto& field : instance_fields)
        class_constructor->add_field(field.name, field.initializer.is_null() ? nullptr : field.initializer.cell());

    for (auto& private_method : instance_private_methods)
        class_constructor->add_private_method(private_method);

    for (auto& method : static_private_methods)
        class_constructor->private_method_or_accessor_add(move(method));

    for (auto& element : static_elements) {
        TRY(element.visit(
            [&](ClassElement::ClassFieldDefinition& field) -> ThrowCompletionOr<void> {
                return TRY(class_constructor->define_field(field.name, field.initializer.is_null() ? nullptr : field.initializer.cell()));
            },
            [&](Handle<ECMAScriptFunctionObject> static_block_function) -> ThrowCompletionOr<void> {
                VERIFY(!static_block_function.is_null());
                // We discard any value returned here.
                TRY(call(global_object, *static_block_function.cell(), class_constructor_value));
                return {};
            }));
    }

    return class_constructor;
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
            declaration.dump(indent + 2);
    }

    if (!m_var_declarations.is_empty()) {
        print_indent(indent + 1);
        outln("(Variable declarations)");
        for (auto& declaration : m_var_declarations)
            declaration.dump(indent + 2);
    }

    if (!m_functions_hoistable_with_annexB_extension.is_empty()) {
        print_indent(indent + 1);
        outln("(Hoisted functions via annexB extension)");
        for (auto& declaration : m_functions_hoistable_with_annexB_extension)
            declaration.dump(indent + 2);
    }

    if (!m_children.is_empty()) {
        print_indent(indent + 1);
        outln("(Children)");
        for (auto& child : children())
            child.dump(indent + 2);
    }
}

void BinaryExpression::dump(int indent) const
{
    const char* op_string = nullptr;
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
    const char* op_string = nullptr;
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
    const char* op_string = nullptr;
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
    for (auto& argument : m_arguments)
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

ThrowCompletionOr<void> ClassDeclaration::for_each_bound_name(ThrowCompletionOrVoidCallback<FlyString const&>&& callback) const
{
    if (m_class_expression->name().is_empty())
        return {};

    return callback(m_class_expression->name());
}

void ClassExpression::dump(int indent) const
{
    print_indent(indent);
    outln("ClassExpression: \"{}\"", m_name);

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
        method.dump(indent + 1);
}

void ClassMethod::dump(int indent) const
{
    ASTNode::dump(indent);

    print_indent(indent);
    outln("(Key)");
    m_key->dump(indent + 1);

    const char* kind_string = nullptr;
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
        if (auto binding_ptr = entry.alias.get_pointer<NonnullRefPtr<BindingPattern>>(); binding_ptr && (*binding_ptr)->contains_expression())
            return true;
    }
    return false;
}

ThrowCompletionOr<void> BindingPattern::for_each_bound_name(ThrowCompletionOrVoidCallback<FlyString const&>&& callback) const
{
    for (auto const& entry : entries) {
        auto const& alias = entry.alias;
        if (alias.has<NonnullRefPtr<Identifier>>()) {
            TRY(callback(alias.get<NonnullRefPtr<Identifier>>()->string()));
        } else if (alias.has<NonnullRefPtr<BindingPattern>>()) {
            TRY(alias.get<NonnullRefPtr<BindingPattern>>()->for_each_bound_name(forward<decltype(callback)>(callback)));
        } else {
            auto const& name = entry.name;
            if (name.has<NonnullRefPtr<Identifier>>())
                TRY(callback(name.get<NonnullRefPtr<Identifier>>()->string()));
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
            if (entry.name.has<NonnullRefPtr<Identifier>>()) {
                entry.name.get<NonnullRefPtr<Identifier>>()->dump(indent + 3);
            } else {
                entry.name.get<NonnullRefPtr<Expression>>()->dump(indent + 3);
            }
        } else if (entry.is_elision()) {
            print_indent(indent + 2);
            outln("(Elision)");
            continue;
        }

        print_indent(indent + 2);
        outln("(Pattern{})", entry.is_rest ? " rest=true" : "");
        if (entry.alias.has<NonnullRefPtr<Identifier>>()) {
            entry.alias.get<NonnullRefPtr<Identifier>>()->dump(indent + 3);
        } else if (entry.alias.has<NonnullRefPtr<BindingPattern>>()) {
            entry.alias.get<NonnullRefPtr<BindingPattern>>()->dump(indent + 3);
        } else if (entry.alias.has<NonnullRefPtr<MemberExpression>>()) {
            entry.alias.get<NonnullRefPtr<MemberExpression>>()->dump(indent + 3);
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

void FunctionNode::dump(int indent, String const& class_name) const
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
            print_indent(indent + 2);
            if (parameter.is_rest)
                out("...");
            parameter.binding.visit(
                [&](FlyString const& name) {
                    outln("{}", name);
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

ThrowCompletionOr<void> FunctionDeclaration::for_each_bound_name(ThrowCompletionOrVoidCallback<const FlyString&>&& callback) const
{
    if (name().is_empty())
        return {};
    return callback(name());
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
Completion Identifier::execute(Interpreter& interpreter, GlobalObject& global_object) const
{
    InterpreterNodeScope node_scope { interpreter, *this };

    // 1. Return ? ResolveBinding(StringValue of Identifier).
    auto reference = TRY(interpreter.vm().resolve_binding(m_string));

    // NOTE: The spec wants us to return the reference directly; this is not possible with ASTNode::execute() (short of letting it return a variant).
    // So, instead of calling GetValue at the call site, we do it here.
    return TRY(reference.get_value(global_object));
}

void Identifier::dump(int indent) const
{
    print_indent(indent);
    outln("Identifier \"{}\"", m_string);
}

Completion PrivateIdentifier::execute(Interpreter&, GlobalObject&) const
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

Completion SpreadExpression::execute(Interpreter& interpreter, GlobalObject& global_object) const
{
    InterpreterNodeScope node_scope { interpreter, *this };
    return m_target->execute(interpreter, global_object);
}

// 13.2.1.1 Runtime Semantics: Evaluation, https://tc39.es/ecma262/#sec-this-keyword-runtime-semantics-evaluation
Completion ThisExpression::execute(Interpreter& interpreter, GlobalObject& global_object) const
{
    InterpreterNodeScope node_scope { interpreter, *this };

    // 1. Return ? ResolveThisBinding().
    return interpreter.vm().resolve_this_binding(global_object);
}

void ThisExpression::dump(int indent) const
{
    ASTNode::dump(indent);
}

// 13.15.2 Runtime Semantics: Evaluation, https://tc39.es/ecma262/#sec-assignment-operators-runtime-semantics-evaluation
Completion AssignmentExpression::execute(Interpreter& interpreter, GlobalObject& global_object) const
{
    InterpreterNodeScope node_scope { interpreter, *this };

    if (m_op == AssignmentOp::Assignment) {
        // AssignmentExpression : LeftHandSideExpression = AssignmentExpression
        return m_lhs.visit(
            // 1. If LeftHandSideExpression is neither an ObjectLiteral nor an ArrayLiteral, then
            [&](NonnullRefPtr<Expression> const& lhs) -> ThrowCompletionOr<Value> {
                // a. Let lref be the result of evaluating LeftHandSideExpression.
                // b. ReturnIfAbrupt(lref).
                auto reference = TRY(lhs->to_reference(interpreter, global_object));

                Value rhs_result;

                // c. If IsAnonymousFunctionDefinition(AssignmentExpression) and IsIdentifierRef of LeftHandSideExpression are both true, then
                if (lhs->is_identifier()) {
                    // i. Let rval be NamedEvaluation of AssignmentExpression with argument lref.[[ReferencedName]].
                    auto& identifier_name = static_cast<Identifier const&>(*lhs).string();
                    rhs_result = TRY(interpreter.vm().named_evaluation_if_anonymous_function(global_object, m_rhs, identifier_name));
                }
                // d. Else,
                else {
                    // i. Let rref be the result of evaluating AssignmentExpression.
                    // ii. Let rval be ? GetValue(rref).
                    rhs_result = TRY(m_rhs->execute(interpreter, global_object)).release_value();
                }

                // e. Perform ? PutValue(lref, rval).
                TRY(reference.put_value(global_object, rhs_result));

                // f. Return rval.
                return rhs_result;
            },
            // 2. Let assignmentPattern be the AssignmentPattern that is covered by LeftHandSideExpression.
            [&](NonnullRefPtr<BindingPattern> const& pattern) -> ThrowCompletionOr<Value> {
                // 3. Let rref be the result of evaluating AssignmentExpression.
                // 4. Let rval be ? GetValue(rref).
                auto rhs_result = TRY(m_rhs->execute(interpreter, global_object)).release_value();

                // 5. Perform ? DestructuringAssignmentEvaluation of assignmentPattern using rval as the argument.
                TRY(interpreter.vm().destructuring_assignment_evaluation(pattern, rhs_result, global_object));

                // 6. Return rval.
                return rhs_result;
            });
    }
    VERIFY(m_lhs.has<NonnullRefPtr<Expression>>());

    // 1. Let lref be the result of evaluating LeftHandSideExpression.
    auto& lhs_expression = *m_lhs.get<NonnullRefPtr<Expression>>();
    auto reference = TRY(lhs_expression.to_reference(interpreter, global_object));

    // 2. Let lval be ? GetValue(lref).
    auto lhs_result = TRY(reference.get_value(global_object));

    //  AssignmentExpression : LeftHandSideExpression {&&=, ||=, ??=} AssignmentExpression
    if (m_op == AssignmentOp::AndAssignment || m_op == AssignmentOp::OrAssignment || m_op == AssignmentOp::NullishAssignment) {
        switch (m_op) {
        // AssignmentExpression : LeftHandSideExpression &&= AssignmentExpression
        case AssignmentOp::AndAssignment:
            // 3. Let lbool be ! ToBoolean(lval).
            // 4. If lbool is false, return lval.
            if (!lhs_result.to_boolean())
                return lhs_result;
            break;

        // AssignmentExpression : LeftHandSideExpression ||= AssignmentExpression
        case AssignmentOp::OrAssignment:
            // 3. Let lbool be ! ToBoolean(lval).
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
            // a. Let rval be NamedEvaluation of AssignmentExpression with argument lref.[[ReferencedName]].
            auto& identifier_name = static_cast<Identifier const&>(lhs_expression).string();
            rhs_result = TRY(interpreter.vm().named_evaluation_if_anonymous_function(global_object, m_rhs, identifier_name));
        }
        // 6. Else,
        else {
            // a. Let rref be the result of evaluating AssignmentExpression.
            // b. Let rval be ? GetValue(rref).
            rhs_result = TRY(m_rhs->execute(interpreter, global_object)).release_value();
        }

        // 7. Perform ? PutValue(lref, rval).
        TRY(reference.put_value(global_object, rhs_result));

        // 8. Return rval.
        return rhs_result;
    }

    // AssignmentExpression : LeftHandSideExpression AssignmentOperator AssignmentExpression

    // 3. Let rref be the result of evaluating AssignmentExpression.
    // 4. Let rval be ? GetValue(rref).
    auto rhs_result = TRY(m_rhs->execute(interpreter, global_object)).release_value();

    // 5. Let assignmentOpText be the source text matched by AssignmentOperator.
    // 6. Let opText be the sequence of Unicode code points associated with assignmentOpText in the following table:
    // 7. Let r be ApplyStringOrNumericBinaryOperator(lval, opText, rval).
    switch (m_op) {
    case AssignmentOp::AdditionAssignment:
        rhs_result = TRY(add(global_object, lhs_result, rhs_result));
        break;
    case AssignmentOp::SubtractionAssignment:
        rhs_result = TRY(sub(global_object, lhs_result, rhs_result));
        break;
    case AssignmentOp::MultiplicationAssignment:
        rhs_result = TRY(mul(global_object, lhs_result, rhs_result));
        break;
    case AssignmentOp::DivisionAssignment:
        rhs_result = TRY(div(global_object, lhs_result, rhs_result));
        break;
    case AssignmentOp::ModuloAssignment:
        rhs_result = TRY(mod(global_object, lhs_result, rhs_result));
        break;
    case AssignmentOp::ExponentiationAssignment:
        rhs_result = TRY(exp(global_object, lhs_result, rhs_result));
        break;
    case AssignmentOp::BitwiseAndAssignment:
        rhs_result = TRY(bitwise_and(global_object, lhs_result, rhs_result));
        break;
    case AssignmentOp::BitwiseOrAssignment:
        rhs_result = TRY(bitwise_or(global_object, lhs_result, rhs_result));
        break;
    case AssignmentOp::BitwiseXorAssignment:
        rhs_result = TRY(bitwise_xor(global_object, lhs_result, rhs_result));
        break;
    case AssignmentOp::LeftShiftAssignment:
        rhs_result = TRY(left_shift(global_object, lhs_result, rhs_result));
        break;
    case AssignmentOp::RightShiftAssignment:
        rhs_result = TRY(right_shift(global_object, lhs_result, rhs_result));
        break;
    case AssignmentOp::UnsignedRightShiftAssignment:
        rhs_result = TRY(unsigned_right_shift(global_object, lhs_result, rhs_result));
        break;
    case AssignmentOp::Assignment:
    case AssignmentOp::AndAssignment:
    case AssignmentOp::OrAssignment:
    case AssignmentOp::NullishAssignment:
        VERIFY_NOT_REACHED();
    }

    // 8. Perform ? PutValue(lref, r).
    TRY(reference.put_value(global_object, rhs_result));

    // 9. Return r.
    return rhs_result;
}

// 13.4.2.1 Runtime Semantics: Evaluation, https://tc39.es/ecma262/#sec-postfix-increment-operator-runtime-semantics-evaluation
// 13.4.3.1 Runtime Semantics: Evaluation, https://tc39.es/ecma262/#sec-postfix-decrement-operator-runtime-semantics-evaluation
// 13.4.4.1 Runtime Semantics: Evaluation, https://tc39.es/ecma262/#sec-prefix-increment-operator-runtime-semantics-evaluation
// 13.4.5.1 Runtime Semantics: Evaluation, https://tc39.es/ecma262/#sec-prefix-decrement-operator-runtime-semantics-evaluation
Completion UpdateExpression::execute(Interpreter& interpreter, GlobalObject& global_object) const
{
    InterpreterNodeScope node_scope { interpreter, *this };

    // 1. Let expr be the result of evaluating <Expression>.
    auto reference = TRY(m_argument->to_reference(interpreter, global_object));

    // 2. Let oldValue be ? ToNumeric(? GetValue(expr)).
    auto old_value = TRY(reference.get_value(global_object));
    old_value = TRY(old_value.to_numeric(global_object));

    Value new_value;
    switch (m_op) {
    case UpdateOp::Increment:
        // 3. If Type(oldValue) is Number, then
        if (old_value.is_number()) {
            // a. Let newValue be ! Number::add(oldValue, 1𝔽).
            new_value = Value(old_value.as_double() + 1);
        }
        // 4. Else,
        else {
            // a. Assert: Type(oldValue) is BigInt.
            // b. Let newValue be ! BigInt::add(oldValue, 1ℤ).
            new_value = js_bigint(interpreter.heap(), old_value.as_bigint().big_integer().plus(Crypto::SignedBigInteger { 1 }));
        }
        break;
    case UpdateOp::Decrement:
        // 3. If Type(oldValue) is Number, then
        if (old_value.is_number()) {
            // a. Let newValue be ! Number::subtract(oldValue, 1𝔽).
            new_value = Value(old_value.as_double() - 1);
        }
        // 4. Else,
        else {
            // a. Assert: Type(oldValue) is BigInt.
            // b. Let newValue be ! BigInt::subtract(oldValue, 1ℤ).
            new_value = js_bigint(interpreter.heap(), old_value.as_bigint().big_integer().minus(Crypto::SignedBigInteger { 1 }));
        }
        break;
    default:
        VERIFY_NOT_REACHED();
    }

    // 5. Perform ? PutValue(expr, newValue).
    TRY(reference.put_value(global_object, new_value));

    // 6. Return newValue.
    // 6. Return oldValue.
    return m_prefixed ? new_value : old_value;
}

void AssignmentExpression::dump(int indent) const
{
    const char* op_string = nullptr;
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
    const char* op_string = nullptr;
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
Completion VariableDeclaration::execute(Interpreter& interpreter, GlobalObject& global_object) const
{
    InterpreterNodeScope node_scope { interpreter, *this };

    for (auto& declarator : m_declarations) {
        if (auto* init = declarator.init()) {
            TRY(declarator.target().visit(
                [&](NonnullRefPtr<Identifier> const& id) -> ThrowCompletionOr<void> {
                    auto reference = TRY(id->to_reference(interpreter, global_object));
                    auto initializer_result = TRY(interpreter.vm().named_evaluation_if_anonymous_function(global_object, *init, id->string()));
                    VERIFY(!initializer_result.is_empty());

                    if (m_declaration_kind == DeclarationKind::Var)
                        return reference.put_value(global_object, initializer_result);
                    else
                        return reference.initialize_referenced_binding(global_object, initializer_result);
                },
                [&](NonnullRefPtr<BindingPattern> const& pattern) -> ThrowCompletionOr<void> {
                    auto initializer_result = TRY(init->execute(interpreter, global_object)).release_value();

                    Environment* environment = m_declaration_kind == DeclarationKind::Var ? nullptr : interpreter.lexical_environment();

                    return interpreter.vm().binding_initialization(pattern, initializer_result, environment, global_object);
                }));
        } else if (m_declaration_kind != DeclarationKind::Var) {
            VERIFY(declarator.target().has<NonnullRefPtr<Identifier>>());
            auto& identifier = declarator.target().get<NonnullRefPtr<Identifier>>();
            auto reference = TRY(identifier->to_reference(interpreter, global_object));
            TRY(reference.initialize_referenced_binding(global_object, js_undefined()));
        }
    }
    return normal_completion({});
}

Completion VariableDeclarator::execute(Interpreter& interpreter, GlobalObject&) const
{
    InterpreterNodeScope node_scope { interpreter, *this };

    // NOTE: VariableDeclarator execution is handled by VariableDeclaration.
    VERIFY_NOT_REACHED();
}

ThrowCompletionOr<void> VariableDeclaration::for_each_bound_name(ThrowCompletionOrVoidCallback<FlyString const&>&& callback) const
{
    for (auto const& entry : declarations()) {
        TRY(entry.target().visit(
            [&](NonnullRefPtr<Identifier> const& id) {
                return callback(id->string());
            },
            [&](NonnullRefPtr<BindingPattern> const& binding) {
                return binding->for_each_bound_name([&](auto const& name) {
                    return callback(name);
                });
            }));
    }

    return {};
}

void VariableDeclaration::dump(int indent) const
{
    const char* declaration_kind_string = nullptr;
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
        declarator.dump(indent + 1);
}

void VariableDeclarator::dump(int indent) const
{
    ASTNode::dump(indent);
    m_target.visit([indent](const auto& value) { value->dump(indent + 1); });
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
        property.dump(indent + 1);
    }
}

void ExpressionStatement::dump(int indent) const
{
    ASTNode::dump(indent);
    m_expression->dump(indent + 1);
}

Completion ObjectProperty::execute(Interpreter& interpreter, GlobalObject&) const
{
    InterpreterNodeScope node_scope { interpreter, *this };

    // NOTE: ObjectProperty execution is handled by ObjectExpression.
    VERIFY_NOT_REACHED();
}

// 13.2.5.4 Runtime Semantics: Evaluation, https://tc39.es/ecma262/#sec-object-initializer-runtime-semantics-evaluation
Completion ObjectExpression::execute(Interpreter& interpreter, GlobalObject& global_object) const
{
    InterpreterNodeScope node_scope { interpreter, *this };

    // 1. Let obj be ! OrdinaryObjectCreate(%Object.prototype%).
    auto* object = Object::create(global_object, global_object.object_prototype());

    // 2. Perform ? PropertyDefinitionEvaluation of PropertyDefinitionList with argument obj.
    for (auto& property : m_properties) {
        auto key = TRY(property.key().execute(interpreter, global_object)).release_value();

        // PropertyDefinition : ... AssignmentExpression
        if (property.type() == ObjectProperty::Type::Spread) {
            // 4. Return ? CopyDataProperties(object, fromValue, excludedNames).
            TRY(object->copy_data_properties(key, {}, global_object));
            continue;
        }

        auto value = TRY(property.value().execute(interpreter, global_object)).release_value();

        if (value.is_function() && property.is_method())
            static_cast<ECMAScriptFunctionObject&>(value.as_function()).set_home_object(object);

        auto property_key = TRY(PropertyKey::from_value(global_object, key));
        auto name = TRY(get_function_property_name(property_key));
        if (property.type() == ObjectProperty::Type::Getter) {
            name = String::formatted("get {}", name);
        } else if (property.type() == ObjectProperty::Type::Setter) {
            name = String::formatted("set {}", name);
        }

        update_function_name(value, name);

        switch (property.type()) {
        case ObjectProperty::Type::Getter:
            VERIFY(value.is_function());
            object->define_direct_accessor(property_key, &value.as_function(), nullptr, Attribute::Configurable | Attribute::Enumerable);
            break;
        case ObjectProperty::Type::Setter:
            VERIFY(value.is_function());
            object->define_direct_accessor(property_key, nullptr, &value.as_function(), Attribute::Configurable | Attribute::Enumerable);
            break;
        case ObjectProperty::Type::KeyValue:
            object->define_direct_property(property_key, value, JS::default_attributes);
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

String MemberExpression::to_string_approximation() const
{
    String object_string = "<object>";
    if (is<Identifier>(*m_object))
        object_string = static_cast<Identifier const&>(*m_object).string();
    if (is_computed())
        return String::formatted("{}[<computed>]", object_string);
    return String::formatted("{}.{}", object_string, verify_cast<Identifier>(*m_property).string());
}

// 13.3.2.1 Runtime Semantics: Evaluation, https://tc39.es/ecma262/#sec-property-accessors-runtime-semantics-evaluation
Completion MemberExpression::execute(Interpreter& interpreter, GlobalObject& global_object) const
{
    InterpreterNodeScope node_scope { interpreter, *this };

    auto reference = TRY(to_reference(interpreter, global_object));
    return TRY(reference.get_value(global_object));
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

ThrowCompletionOr<OptionalChain::ReferenceAndValue> OptionalChain::to_reference_and_value(JS::Interpreter& interpreter, JS::GlobalObject& global_object) const
{
    auto base_reference = TRY(m_base->to_reference(interpreter, global_object));
    auto base = base_reference.is_unresolvable()
        ? TRY(m_base->execute(interpreter, global_object)).release_value()
        : TRY(base_reference.get_value(global_object));

    for (auto& reference : m_references) {
        auto is_optional = reference.visit([](auto& ref) { return ref.mode; }) == Mode::Optional;
        if (is_optional && base.is_nullish())
            return ReferenceAndValue { {}, js_undefined() };

        auto expression = reference.visit(
            [&](Call const& call) -> NonnullRefPtr<Expression> {
                return create_ast_node<CallExpression>(source_range(),
                    create_ast_node<SyntheticReferenceExpression>(source_range(), base_reference, base),
                    call.arguments);
            },
            [&](ComputedReference const& ref) -> NonnullRefPtr<Expression> {
                return create_ast_node<MemberExpression>(source_range(),
                    create_ast_node<SyntheticReferenceExpression>(source_range(), base_reference, base),
                    ref.expression,
                    true);
            },
            [&](MemberReference const& ref) -> NonnullRefPtr<Expression> {
                return create_ast_node<MemberExpression>(source_range(),
                    create_ast_node<SyntheticReferenceExpression>(source_range(), base_reference, base),
                    ref.identifier,
                    false);
            },
            [&](PrivateMemberReference const& ref) -> NonnullRefPtr<Expression> {
                return create_ast_node<MemberExpression>(source_range(),
                    create_ast_node<SyntheticReferenceExpression>(source_range(), base_reference, base),
                    ref.private_identifier,
                    false);
            });
        if (is<CallExpression>(*expression)) {
            base_reference = JS::Reference {};
            base = TRY(expression->execute(interpreter, global_object)).release_value();
        } else {
            base_reference = TRY(expression->to_reference(interpreter, global_object));
            base = TRY(base_reference.get_value(global_object));
        }
    }

    return ReferenceAndValue { move(base_reference), base };
}

// 13.3.9.1 Runtime Semantics: Evaluation, https://tc39.es/ecma262/#sec-optional-chaining-evaluation
Completion OptionalChain::execute(Interpreter& interpreter, GlobalObject& global_object) const
{
    InterpreterNodeScope node_scope { interpreter, *this };
    return TRY(to_reference_and_value(interpreter, global_object)).value;
}

ThrowCompletionOr<JS::Reference> OptionalChain::to_reference(Interpreter& interpreter, GlobalObject& global_object) const
{
    return TRY(to_reference_and_value(interpreter, global_object)).reference;
}

void MetaProperty::dump(int indent) const
{
    String name;
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
Completion MetaProperty::execute(Interpreter& interpreter, GlobalObject& global_object) const
{
    InterpreterNodeScope node_scope { interpreter, *this };

    // NewTarget : new . target
    if (m_type == MetaProperty::Type::NewTarget) {
        // 1. Return GetNewTarget().
        return interpreter.vm().get_new_target();
    }

    // ImportMeta : import . meta
    if (m_type == MetaProperty::Type::ImportMeta) {
        // 1. Let module be ! GetActiveScriptOrModule().
        auto script_or_module = interpreter.vm().get_active_script_or_module();

        // 2. Assert: module is a Source Text Module Record.
        VERIFY(script_or_module.has<WeakPtr<Module>>());
        VERIFY(script_or_module.get<WeakPtr<Module>>());
        VERIFY(is<SourceTextModule>(*script_or_module.get<WeakPtr<Module>>()));
        auto& module = static_cast<SourceTextModule&>(*script_or_module.get<WeakPtr<Module>>());

        // 3. Let importMeta be module.[[ImportMeta]].
        auto* import_meta = module.import_meta();

        // 4. If importMeta is empty, then
        if (import_meta == nullptr) {
            // a. Set importMeta to ! OrdinaryObjectCreate(null).
            import_meta = Object::create(global_object, nullptr);

            // b. Let importMetaValues be ! HostGetImportMetaProperties(module).
            auto import_meta_values = interpreter.vm().host_get_import_meta_properties(module);

            // c. For each Record { [[Key]], [[Value]] } p of importMetaValues, do
            for (auto& entry : import_meta_values) {
                // i. Perform ! CreateDataPropertyOrThrow(importMeta, p.[[Key]], p.[[Value]]).
                MUST(import_meta->create_data_property_or_throw(entry.key, entry.value));
            }

            // d. Perform ! HostFinalizeImportMeta(importMeta, module).
            interpreter.vm().host_finalize_import_meta(import_meta, module);

            // e. Set module.[[ImportMeta]] to importMeta.
            module.set_import_meta({}, import_meta);

            // f. Return importMeta.
            return Value { import_meta };
        }
        // 5. Else,
        else {
            // a. Assert: Type(importMeta) is Object.
            // Note: This is always true by the type.

            // b. Return importMeta.
            return Value { import_meta };
        }
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
Completion ImportCall::execute(Interpreter& interpreter, GlobalObject& global_object) const
{
    InterpreterNodeScope node_scope { interpreter, *this };

    // 2.1.1.1 EvaluateImportCall ( specifierExpression [ , optionsExpression ] ), https://tc39.es/proposal-import-assertions/#sec-evaluate-import-call
    //  1. Let referencingScriptOrModule be ! GetActiveScriptOrModule().
    auto referencing_script_or_module = interpreter.vm().get_active_script_or_module();

    // 2. Let specifierRef be the result of evaluating specifierExpression.
    // 3. Let specifier be ? GetValue(specifierRef).
    auto specifier = TRY(m_specifier->execute(interpreter, global_object));

    auto options_value = js_undefined();
    // 4. If optionsExpression is present, then
    if (m_options) {
        // a. Let optionsRef be the result of evaluating optionsExpression.
        // b. Let options be ? GetValue(optionsRef).
        options_value = TRY(m_options->execute(interpreter, global_object)).release_value();
    }
    // 5. Else,
    // a. Let options be undefined.
    // Note: options_value is undefined by default.

    // 6. Let promiseCapability be ! NewPromiseCapability(%Promise%).
    auto promise_capability = MUST(new_promise_capability(global_object, global_object.promise_constructor()));

    // 7. Let specifierString be ToString(specifier).
    // 8. IfAbruptRejectPromise(specifierString, promiseCapability).
    auto specifier_string = TRY_OR_REJECT_WITH_VALUE(global_object, promise_capability, specifier->to_string(global_object));

    // 9. Let assertions be a new empty List.
    Vector<ModuleRequest::Assertion> assertions;

    // 10. If options is not undefined, then
    if (!options_value.is_undefined()) {
        // a. If Type(options) is not Object,
        if (!options_value.is_object()) {
            auto* error = TypeError::create(global_object, String::formatted(ErrorType::NotAnObject.message(), "ImportOptions"));
            // i. Perform ! Call(promiseCapability.[[Reject]], undefined, « a newly created TypeError object »).
            MUST(call(global_object, *promise_capability.reject, js_undefined(), error));

            // ii. Return promiseCapability.[[Promise]].
            return Value { promise_capability.promise };
        }

        // b. Let assertionsObj be Get(options, "assert").
        // c. IfAbruptRejectPromise(assertionsObj, promiseCapability).
        auto assertion_object = TRY_OR_REJECT_WITH_VALUE(global_object, promise_capability, options_value.get(global_object, interpreter.vm().names.assert));

        // d. If assertionsObj is not undefined,
        if (!assertion_object.is_undefined()) {
            // i. If Type(assertionsObj) is not Object,
            if (!assertion_object.is_object()) {
                auto* error = TypeError::create(global_object, String::formatted(ErrorType::NotAnObject.message(), "ImportOptionsAssertions"));
                // 1. Perform ! Call(promiseCapability.[[Reject]], undefined, « a newly created TypeError object »).
                MUST(call(global_object, *promise_capability.reject, js_undefined(), error));

                // 2. Return promiseCapability.[[Promise]].
                return Value { promise_capability.promise };
            }

            // ii. Let keys be EnumerableOwnPropertyNames(assertionsObj, key).
            // iii. IfAbruptRejectPromise(keys, promiseCapability).
            auto keys = TRY_OR_REJECT_WITH_VALUE(global_object, promise_capability, assertion_object.as_object().enumerable_own_property_names(Object::PropertyKind::Key));

            // iv. Let supportedAssertions be ! HostGetSupportedImportAssertions().
            auto supported_assertions = interpreter.vm().host_get_supported_import_assertions();

            // v. For each String key of keys,
            for (auto const& key : keys) {
                auto property_key = MUST(key.to_property_key(global_object));

                // 1. Let value be Get(assertionsObj, key).
                // 2. IfAbruptRejectPromise(value, promiseCapability).
                auto value = TRY_OR_REJECT_WITH_VALUE(global_object, promise_capability, assertion_object.get(global_object, property_key));

                // 3. If Type(value) is not String, then
                if (!value.is_string()) {
                    auto* error = TypeError::create(global_object, String::formatted(ErrorType::NotAString.message(), "Import Assertion option value"));
                    // a. Perform ! Call(promiseCapability.[[Reject]], undefined, « a newly created TypeError object »).
                    MUST(call(global_object, *promise_capability.reject, js_undefined(), error));

                    // b. Return promiseCapability.[[Promise]].
                    return Value { promise_capability.promise };
                }

                // 4. If supportedAssertions contains key, then
                if (supported_assertions.contains_slow(property_key.to_string())) {
                    // a. Append { [[Key]]: key, [[Value]]: value } to assertions.
                    assertions.empend(property_key.to_string(), value.as_string().string());
                }
            }
        }
        // e. Sort assertions by the code point order of the [[Key]] of each element. NOTE: This sorting is observable only in that hosts are prohibited from distinguishing among assertions by the order they occur in.
        // Note: This is done when constructing the ModuleRequest.
    }

    // 11. Let moduleRequest be a new ModuleRequest Record { [[Specifier]]: specifierString, [[Assertions]]: assertions }.
    ModuleRequest request { specifier_string, assertions };

    // 12. Perform ! HostImportModuleDynamically(referencingScriptOrModule, moduleRequest, promiseCapability).
    interpreter.vm().host_import_module_dynamically(referencing_script_or_module, move(request), promise_capability);

    // 13. Return promiseCapability.[[Promise]].
    return Value { promise_capability.promise };
}

// 13.2.3.1 Runtime Semantics: Evaluation, https://tc39.es/ecma262/#sec-literals-runtime-semantics-evaluation
Completion StringLiteral::execute(Interpreter& interpreter, GlobalObject&) const
{
    InterpreterNodeScope node_scope { interpreter, *this };

    // 1. Return the SV of StringLiteral as defined in 12.8.4.2.
    return Value { js_string(interpreter.heap(), m_value) };
}

// 13.2.3.1 Runtime Semantics: Evaluation, https://tc39.es/ecma262/#sec-literals-runtime-semantics-evaluation
Completion NumericLiteral::execute(Interpreter& interpreter, GlobalObject&) const
{
    InterpreterNodeScope node_scope { interpreter, *this };

    // 1. Return the NumericValue of NumericLiteral as defined in 12.8.3.
    return Value(m_value);
}

// 13.2.3.1 Runtime Semantics: Evaluation, https://tc39.es/ecma262/#sec-literals-runtime-semantics-evaluation
Completion BigIntLiteral::execute(Interpreter& interpreter, GlobalObject&) const
{
    InterpreterNodeScope node_scope { interpreter, *this };

    // 1. Return the NumericValue of NumericLiteral as defined in 12.8.3.
    Crypto::SignedBigInteger integer;
    if (m_value[0] == '0' && m_value.length() >= 3) {
        if (m_value[1] == 'x' || m_value[1] == 'X') {
            return Value { js_bigint(interpreter.heap(), Crypto::SignedBigInteger::from_base(16, m_value.substring(2, m_value.length() - 3))) };
        } else if (m_value[1] == 'o' || m_value[1] == 'O') {
            return Value { js_bigint(interpreter.heap(), Crypto::SignedBigInteger::from_base(8, m_value.substring(2, m_value.length() - 3))) };
        } else if (m_value[1] == 'b' || m_value[1] == 'B') {
            return Value { js_bigint(interpreter.heap(), Crypto::SignedBigInteger::from_base(2, m_value.substring(2, m_value.length() - 3))) };
        }
    }
    return Value { js_bigint(interpreter.heap(), Crypto::SignedBigInteger::from_base(10, m_value.substring(0, m_value.length() - 1))) };
}

// 13.2.3.1 Runtime Semantics: Evaluation, https://tc39.es/ecma262/#sec-literals-runtime-semantics-evaluation
Completion BooleanLiteral::execute(Interpreter& interpreter, GlobalObject&) const
{
    InterpreterNodeScope node_scope { interpreter, *this };

    // 1. If BooleanLiteral is the token false, return false.
    // 2. If BooleanLiteral is the token true, return true.
    return Value(m_value);
}

// 13.2.3.1 Runtime Semantics: Evaluation, https://tc39.es/ecma262/#sec-literals-runtime-semantics-evaluation
Completion NullLiteral::execute(Interpreter& interpreter, GlobalObject&) const
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
Completion RegExpLiteral::execute(Interpreter& interpreter, GlobalObject& global_object) const
{
    InterpreterNodeScope node_scope { interpreter, *this };

    // 1. Let pattern be ! CodePointsToString(BodyText of RegularExpressionLiteral).
    auto pattern = this->pattern();

    // 2. Let flags be ! CodePointsToString(FlagText of RegularExpressionLiteral).
    auto flags = this->flags();

    // 3. Return RegExpCreate(pattern, flags).
    Regex<ECMA262> regex(parsed_regex(), parsed_pattern(), parsed_flags());
    return Value { RegExpObject::create(global_object, move(regex), move(pattern), move(flags)) };
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
Completion ArrayExpression::execute(Interpreter& interpreter, GlobalObject& global_object) const
{
    InterpreterNodeScope node_scope { interpreter, *this };

    // 1. Let array be ! ArrayCreate(0).
    auto* array = MUST(Array::create(global_object, 0));

    // 2. Let len be the result of performing ArrayAccumulation of ElementList with arguments array and 0.
    // 3. ReturnIfAbrupt(len).

    array->indexed_properties();
    size_t index = 0;
    for (auto& element : m_elements) {
        auto value = Value();
        if (element) {
            value = TRY(element->execute(interpreter, global_object)).release_value();

            if (is<SpreadExpression>(*element)) {
                (void)TRY(get_iterator_values(global_object, value, [&](Value iterator_value) -> Optional<Completion> {
                    array->indexed_properties().put(index++, iterator_value, default_attributes);
                    return {};
                }));
                continue;
            }
        }
        array->indexed_properties().put(index++, value, default_attributes);
    }

    // 4. Return array.
    return Value { array };
}

void TemplateLiteral::dump(int indent) const
{
    ASTNode::dump(indent);
    for (auto& expression : m_expressions)
        expression.dump(indent + 1);
}

// 13.2.8.5 Runtime Semantics: Evaluation, https://tc39.es/ecma262/#sec-template-literals-runtime-semantics-evaluation
Completion TemplateLiteral::execute(Interpreter& interpreter, GlobalObject& global_object) const
{
    InterpreterNodeScope node_scope { interpreter, *this };

    StringBuilder string_builder;

    for (auto& expression : m_expressions) {
        // 1. Let head be the TV of TemplateHead as defined in 12.8.6.

        // 2. Let subRef be the result of evaluating Expression.
        // 3. Let sub be ? GetValue(subRef).
        auto sub = TRY(expression.execute(interpreter, global_object)).release_value();

        // 4. Let middle be ? ToString(sub).
        auto string = TRY(sub.to_string(global_object));
        string_builder.append(string);

        // 5. Let tail be the result of evaluating TemplateSpans.
        // 6. ReturnIfAbrupt(tail).
    }

    // 7. Return the string-concatenation of head, middle, and tail.
    return Value { js_string(interpreter.heap(), string_builder.build()) };
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
Completion TaggedTemplateLiteral::execute(Interpreter& interpreter, GlobalObject& global_object) const
{
    InterpreterNodeScope node_scope { interpreter, *this };

    auto& vm = interpreter.vm();
    auto tag = TRY(m_tag->execute(interpreter, global_object)).release_value();
    auto& expressions = m_template_literal->expressions();
    auto* strings = MUST(Array::create(global_object, 0));
    MarkedVector<Value> arguments(vm.heap());
    arguments.append(strings);
    for (size_t i = 0; i < expressions.size(); ++i) {
        auto value = TRY(expressions[i].execute(interpreter, global_object)).release_value();
        // tag`${foo}`             -> "", foo, ""                -> tag(["", ""], foo)
        // tag`foo${bar}baz${qux}` -> "foo", bar, "baz", qux, "" -> tag(["foo", "baz", ""], bar, qux)
        if (i % 2 == 0) {
            strings->indexed_properties().append(value);
        } else {
            arguments.append(value);
        }
    }

    auto* raw_strings = MUST(Array::create(global_object, 0));
    for (auto& raw_string : m_template_literal->raw_strings()) {
        auto value = TRY(raw_string.execute(interpreter, global_object)).release_value();
        raw_strings->indexed_properties().append(value);
    }
    strings->define_direct_property(vm.names.raw, raw_strings, 0);
    return call(global_object, tag, js_undefined(), move(arguments));
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
        [&](FlyString const& parameter) {
            if (parameter.is_null())
                outln("CatchClause");
            else
                outln("CatchClause ({})", parameter);
        },
        [&](NonnullRefPtr<BindingPattern> const& pattern) {
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
Completion TryStatement::execute(Interpreter& interpreter, GlobalObject& global_object) const
{
    InterpreterNodeScope node_scope { interpreter, *this };

    auto& vm = interpreter.vm();

    // 14.15.2 Runtime Semantics: CatchClauseEvaluation, https://tc39.es/ecma262/#sec-runtime-semantics-catchclauseevaluation
    auto catch_clause_evaluation = [&](Value thrown_value) {
        // 1. Let oldEnv be the running execution context's LexicalEnvironment.
        auto* old_environment = vm.running_execution_context().lexical_environment;

        // 2. Let catchEnv be NewDeclarativeEnvironment(oldEnv).
        auto* catch_environment = new_declarative_environment(*old_environment);

        m_handler->parameter().visit(
            [&](FlyString const& parameter) {
                // 3. For each element argName of the BoundNames of CatchParameter, do
                // a. Perform ! catchEnv.CreateMutableBinding(argName, false).
                MUST(catch_environment->create_mutable_binding(global_object, parameter, false));
            },
            [&](NonnullRefPtr<BindingPattern> const& pattern) {
                // 3. For each element argName of the BoundNames of CatchParameter, do
                pattern->for_each_bound_name([&](auto& name) {
                    // a. Perform ! catchEnv.CreateMutableBinding(argName, false).
                    MUST(catch_environment->create_mutable_binding(global_object, name, false));
                });
            });

        // 4. Set the running execution context's LexicalEnvironment to catchEnv.
        vm.running_execution_context().lexical_environment = catch_environment;

        // 5. Let status be BindingInitialization of CatchParameter with arguments thrownValue and catchEnv.
        auto status = m_handler->parameter().visit(
            [&](FlyString const& parameter) {
                return catch_environment->initialize_binding(global_object, parameter, thrown_value);
            },
            [&](NonnullRefPtr<BindingPattern> const& pattern) {
                return vm.binding_initialization(pattern, thrown_value, catch_environment, global_object);
            });

        // 6. If status is an abrupt completion, then
        if (status.is_error()) {
            // a. Set the running execution context's LexicalEnvironment to oldEnv.
            vm.running_execution_context().lexical_environment = old_environment;

            // b. Return Completion(status).
            return status.release_error();
        }

        // 7. Let B be the result of evaluating Block.
        auto handler_result = m_handler->body().execute(interpreter, global_object);

        // 8. Set the running execution context's LexicalEnvironment to oldEnv.
        vm.running_execution_context().lexical_environment = old_environment;

        // 9. Return Completion(B).
        return handler_result;
    };

    Completion result;

    // 1. Let B be the result of evaluating Block.
    auto block_result = m_block->execute(interpreter, global_object);

    // TryStatement : try Block Catch
    // TryStatement : try Block Catch Finally
    if (m_handler) {
        // 2. If B.[[Type]] is throw, let C be CatchClauseEvaluation of Catch with argument B.[[Value]].
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
        auto finalizer_result = m_finalizer->execute(interpreter, global_object);

        // 5. If F.[[Type]] is normal, set F to C.
        if (finalizer_result.type() == Completion::Type::Normal)
            finalizer_result = move(result);

        // 6. Return Completion(UpdateEmpty(F, undefined)).
        return finalizer_result.update_empty(js_undefined());
    }

    // 4. Return Completion(UpdateEmpty(C, undefined)).
    return result.update_empty(js_undefined());
}

Completion CatchClause::execute(Interpreter& interpreter, GlobalObject&) const
{
    InterpreterNodeScope node_scope { interpreter, *this };

    // NOTE: CatchClause execution is handled by TryStatement.
    VERIFY_NOT_REACHED();
    return {};
}

// 14.14.1 Runtime Semantics: Evaluation, https://tc39.es/ecma262/#sec-throw-statement-runtime-semantics-evaluation
Completion ThrowStatement::execute(Interpreter& interpreter, GlobalObject& global_object) const
{
    InterpreterNodeScope node_scope { interpreter, *this };

    // 1. Let exprRef be the result of evaluating Expression.
    // 2. Let exprValue be ? GetValue(exprRef).
    auto value = TRY(m_argument->execute(interpreter, global_object)).release_value();

    // 3. Return ThrowCompletion(exprValue).
    return throw_completion(value);
}

// 14.1.1 Runtime Semantics: Evaluation, https://tc39.es/ecma262/#sec-statement-semantics-runtime-semantics-evaluation
// BreakableStatement : SwitchStatement
Completion SwitchStatement::execute(Interpreter& interpreter, GlobalObject& global_object) const
{
    // 1. Let newLabelSet be a new empty List.
    // 2. Return the result of performing LabelledEvaluation of this BreakableStatement with argument newLabelSet.
    return labelled_evaluation(interpreter, global_object, *this, {});
}

// NOTE: Since we don't have the 'BreakableStatement' from the spec as a separate ASTNode that wraps IterationStatement / SwitchStatement,
// execute() needs to take care of LabelledEvaluation, which in turn calls execute_impl().
// 14.12.4 Runtime Semantics: Evaluation, https://tc39.es/ecma262/#sec-switch-statement-runtime-semantics-evaluation
Completion SwitchStatement::execute_impl(Interpreter& interpreter, GlobalObject& global_object) const
{
    InterpreterNodeScope node_scope { interpreter, *this };

    auto& vm = interpreter.vm();

    // 14.12.3 CaseClauseIsSelected ( C, input ), https://tc39.es/ecma262/#sec-runtime-semantics-caseclauseisselected
    auto case_clause_is_selected = [&](auto const& case_clause, auto input) -> ThrowCompletionOr<bool> {
        // 1. Assert: C is an instance of the production CaseClause : case Expression : StatementList[opt] .
        VERIFY(case_clause.test());

        // 2. Let exprRef be the result of evaluating the Expression of C.
        // 3. Let clauseSelector be ? GetValue(exprRef).
        auto clause_selector = TRY(case_clause.test()->execute(interpreter, global_object)).release_value();

        // 4. Return IsStrictlyEqual(input, clauseSelector).
        return is_strictly_equal(input, clause_selector);
    };

    // 14.12.2 Runtime Semantics: CaseBlockEvaluation, https://tc39.es/ecma262/#sec-runtime-semantics-caseblockevaluation
    auto case_block_evaluation = [&](auto input) {
        // CaseBlock : { }
        if (m_cases.is_empty()) {
            // 1. Return NormalCompletion(undefined).
            return normal_completion(js_undefined());
        }

        NonnullRefPtrVector<SwitchCase> case_clauses_1;
        NonnullRefPtrVector<SwitchCase> case_clauses_2;
        RefPtr<SwitchCase> default_clause;
        for (auto const& switch_case : m_cases) {
            if (!switch_case.test())
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
                    auto result = case_clause.evaluate_statements(interpreter, global_object);

                    // ii. If R.[[Value]] is not empty, set V to R.[[Value]].
                    if (result.value().has_value())
                        last_value = *result.value();

                    // iii. If R is an abrupt completion, return Completion(UpdateEmpty(R, V)).
                    if (result.is_abrupt())
                        return result.update_empty(last_value);
                }
            }

            // 5. Return NormalCompletion(V).
            return normal_completion(last_value);
        }
        // CaseBlock : { CaseClauses[opt] DefaultClause CaseClauses[opt] }
        else {
            // 1. Let V be undefined.
            auto last_value = js_undefined();

            // 2. If the first CaseClauses is present, then
            //    a. Let A be the List of CaseClause items in the first CaseClauses, in source text order.
            // 3. Else,
            //    a. Let A be « ».
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
                    auto result = case_clause.evaluate_statements(interpreter, global_object);

                    // ii. If R.[[Value]] is not empty, set V to R.[[Value]].
                    if (result.value().has_value())
                        last_value = *result.value();

                    // iii. If R is an abrupt completion, return Completion(UpdateEmpty(R, V)).
                    if (result.is_abrupt())
                        return result.update_empty(last_value);
                }
            }

            // 6. Let foundInB be false.
            auto found_in_b = false;

            // 7. If the second CaseClauses is present, then
            //    a. Let B be the List of CaseClause items in the second CaseClauses, in source text order.
            // 8. Else,
            //    a. Let B be « ».
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
                        auto result = case_clause.evaluate_statements(interpreter, global_object);

                        // 2. If R.[[Value]] is not empty, set V to R.[[Value]].
                        if (result.value().has_value())
                            last_value = *result.value();

                        // 3. If R is an abrupt completion, return Completion(UpdateEmpty(R, V)).
                        if (result.is_abrupt())
                            return result.update_empty(last_value);
                    }
                }
            }

            // 10. If foundInB is true, return NormalCompletion(V).
            if (found_in_b)
                return normal_completion(last_value);

            // 11. Let R be the result of evaluating DefaultClause.
            auto result = default_clause->evaluate_statements(interpreter, global_object);

            // 12. If R.[[Value]] is not empty, set V to R.[[Value]].
            if (result.value().has_value())
                last_value = *result.value();

            // 13. If R is an abrupt completion, return Completion(UpdateEmpty(R, V)).
            if (result.is_abrupt())
                return result.update_empty(last_value);

            // 14. NOTE: The following is another complete iteration of the second CaseClauses.
            // 15. For each CaseClause C of B, do
            for (auto const& case_clause : case_clauses_2) {
                // a. Let R be the result of evaluating CaseClause C.
                result = case_clause.evaluate_statements(interpreter, global_object);

                // b. If R.[[Value]] is not empty, set V to R.[[Value]].
                if (result.value().has_value())
                    last_value = *result.value();

                // c. If R is an abrupt completion, return Completion(UpdateEmpty(R, V)).
                if (result.is_abrupt())
                    return result.update_empty(last_value);
            }

            // 16. Return NormalCompletion(V).
            return normal_completion(last_value);
        }

        VERIFY_NOT_REACHED();
    };

    // SwitchStatement : switch ( Expression ) CaseBlock
    // 1. Let exprRef be the result of evaluating Expression.
    // 2. Let switchValue be ? GetValue(exprRef).
    auto switch_value = TRY(m_discriminant->execute(interpreter, global_object)).release_value();

    // 3. Let oldEnv be the running execution context's LexicalEnvironment.
    auto* old_environment = interpreter.lexical_environment();

    // Optimization: Avoid creating a lexical environment if there are no lexical declarations.
    if (has_lexical_declarations()) {
        // 4. Let blockEnv be NewDeclarativeEnvironment(oldEnv).
        auto* block_environment = new_declarative_environment(*old_environment);

        // 5. Perform BlockDeclarationInstantiation(CaseBlock, blockEnv).
        block_declaration_instantiation(global_object, block_environment);

        // 6. Set the running execution context's LexicalEnvironment to blockEnv.
        vm.running_execution_context().lexical_environment = block_environment;
    }

    // 7. Let R be CaseBlockEvaluation of CaseBlock with argument switchValue.
    auto result = case_block_evaluation(switch_value);

    // 8. Set the running execution context's LexicalEnvironment to oldEnv.
    vm.running_execution_context().lexical_environment = old_environment;

    // 9. Return R.
    return result;
}

Completion SwitchCase::execute(Interpreter& interpreter, GlobalObject&) const
{
    InterpreterNodeScope node_scope { interpreter, *this };

    // NOTE: SwitchCase execution is handled by SwitchStatement.
    VERIFY_NOT_REACHED();
    return {};
}

// 14.9.2 Runtime Semantics: Evaluation, https://tc39.es/ecma262/#sec-break-statement-runtime-semantics-evaluation
Completion BreakStatement::execute(Interpreter& interpreter, GlobalObject&) const
{
    InterpreterNodeScope node_scope { interpreter, *this };

    // BreakStatement : break ;
    if (m_target_label.is_null()) {
        // 1. Return Completion { [[Type]]: break, [[Value]]: empty, [[Target]]: empty }.
        return { Completion::Type::Break, {}, {} };
    }

    // BreakStatement : break LabelIdentifier ;
    // 1. Let label be the StringValue of LabelIdentifier.
    // 2. Return Completion { [[Type]]: break, [[Value]]: empty, [[Target]]: label }.
    return { Completion::Type::Break, {}, m_target_label };
}

// 14.8.2 Runtime Semantics: Evaluation, https://tc39.es/ecma262/#sec-continue-statement-runtime-semantics-evaluation
Completion ContinueStatement::execute(Interpreter& interpreter, GlobalObject&) const
{
    InterpreterNodeScope node_scope { interpreter, *this };

    // ContinueStatement : continue ;
    if (m_target_label.is_null()) {
        // 1. Return Completion { [[Type]]: continue, [[Value]]: empty, [[Target]]: empty }.
        return { Completion::Type::Continue, {}, {} };
    }

    // ContinueStatement : continue LabelIdentifier ;
    // 1. Let label be the StringValue of LabelIdentifier.
    // 2. Return Completion { [[Type]]: continue, [[Value]]: empty, [[Target]]: label }.
    return { Completion::Type::Continue, {}, m_target_label };
}

void SwitchStatement::dump(int indent) const
{
    ASTNode::dump(indent);
    m_discriminant->dump(indent + 1);
    for (auto& switch_case : m_cases) {
        switch_case.dump(indent + 1);
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
Completion ConditionalExpression::execute(Interpreter& interpreter, GlobalObject& global_object) const
{
    InterpreterNodeScope node_scope { interpreter, *this };

    // 1. Let lref be the result of evaluating ShortCircuitExpression.
    // 2. Let lval be ! ToBoolean(? GetValue(lref)).
    auto test_result = TRY(m_test->execute(interpreter, global_object)).release_value();

    // 3. If lval is true, then
    if (test_result.to_boolean()) {
        // a. Let trueRef be the result of evaluating the first AssignmentExpression.
        // b. Return ? GetValue(trueRef).
        return m_consequent->execute(interpreter, global_object);
    }
    // 4. Else,
    else {
        // a. Let falseRef be the result of evaluating the second AssignmentExpression.
        // b. Return ? GetValue(falseRef).
        return m_alternate->execute(interpreter, global_object);
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
        expression.dump(indent + 1);
}

// 13.16.1 Runtime Semantics: Evaluation, https://tc39.es/ecma262/#sec-comma-operator-runtime-semantics-evaluation
Completion SequenceExpression::execute(Interpreter& interpreter, GlobalObject& global_object) const
{
    InterpreterNodeScope node_scope { interpreter, *this };

    // NOTE: Not sure why the last node is an AssignmentExpression in the spec :yakfused:
    // 1. Let lref be the result of evaluating Expression.
    // 2. Perform ? GetValue(lref).
    // 3. Let rref be the result of evaluating AssignmentExpression.
    // 4. Return ? GetValue(rref).
    Value last_value;
    for (auto const& expression : m_expressions)
        last_value = TRY(expression.execute(interpreter, global_object)).release_value();
    return { move(last_value) };
}

// 14.16.1 Runtime Semantics: Evaluation, https://tc39.es/ecma262/#sec-debugger-statement-runtime-semantics-evaluation
Completion DebuggerStatement::execute(Interpreter& interpreter, GlobalObject&) const
{
    InterpreterNodeScope node_scope { interpreter, *this };

    Completion result;

    // 1. If an implementation-defined debugging facility is available and enabled, then
    if (false) {
        // a. Perform an implementation-defined debugging action.
        // b. Let result be an implementation-defined Completion value.
    }
    // 2. Else,
    else {
        // a. Let result be NormalCompletion(empty).
        result = normal_completion({});
    }

    // 3. Return result.
    return result;
}
ThrowCompletionOr<void> ScopeNode::for_each_lexically_scoped_declaration(ThrowCompletionOrVoidCallback<Declaration const&>&& callback) const
{
    for (auto& declaration : m_lexical_declarations)
        TRY(callback(declaration));

    return {};
}

ThrowCompletionOr<void> ScopeNode::for_each_lexically_declared_name(ThrowCompletionOrVoidCallback<FlyString const&>&& callback) const
{
    for (auto const& declaration : m_lexical_declarations) {
        TRY(declaration.for_each_bound_name([&](auto const& name) {
            return callback(name);
        }));
    }
    return {};
}

ThrowCompletionOr<void> ScopeNode::for_each_var_declared_name(ThrowCompletionOrVoidCallback<FlyString const&>&& callback) const
{
    for (auto& declaration : m_var_declarations) {
        TRY(declaration.for_each_bound_name([&](auto const& name) {
            return callback(name);
        }));
    }
    return {};
}

ThrowCompletionOr<void> ScopeNode::for_each_var_function_declaration_in_reverse_order(ThrowCompletionOrVoidCallback<FunctionDeclaration const&>&& callback) const
{
    for (ssize_t i = m_var_declarations.size() - 1; i >= 0; i--) {
        auto& declaration = m_var_declarations[i];
        if (is<FunctionDeclaration>(declaration))
            TRY(callback(static_cast<FunctionDeclaration const&>(declaration)));
    }
    return {};
}

ThrowCompletionOr<void> ScopeNode::for_each_var_scoped_variable_declaration(ThrowCompletionOrVoidCallback<VariableDeclaration const&>&& callback) const
{
    for (auto& declaration : m_var_declarations) {
        if (!is<FunctionDeclaration>(declaration)) {
            VERIFY(is<VariableDeclaration>(declaration));
            TRY(callback(static_cast<VariableDeclaration const&>(declaration)));
        }
    }
    return {};
}

ThrowCompletionOr<void> ScopeNode::for_each_function_hoistable_with_annexB_extension(ThrowCompletionOrVoidCallback<FunctionDeclaration&>&& callback) const
{
    for (auto& function : m_functions_hoistable_with_annexB_extension) {
        // We need const_cast here since it might have to set a property on function declaration.
        TRY(callback(const_cast<FunctionDeclaration&>(function)));
    }
    return {};
}

void ScopeNode::add_lexical_declaration(NonnullRefPtr<Declaration> declaration)
{
    m_lexical_declarations.append(move(declaration));
}

void ScopeNode::add_var_scoped_declaration(NonnullRefPtr<Declaration> declaration)
{
    m_var_declarations.append(move(declaration));
}

void ScopeNode::add_hoisted_function(NonnullRefPtr<FunctionDeclaration> declaration)
{
    m_functions_hoistable_with_annexB_extension.append(move(declaration));
}

// 16.2.1.11 Runtime Semantics: Evaluation, https://tc39.es/ecma262/#sec-module-semantics-runtime-semantics-evaluation
Completion ImportStatement::execute(Interpreter& interpreter, GlobalObject&) const
{
    InterpreterNodeScope node_scope { interpreter, *this };

    // 1. Return NormalCompletion(empty).
    return normal_completion({});
}

FlyString ExportStatement::local_name_for_default = "*default*";

// 16.2.3.7 Runtime Semantics: Evaluation, https://tc39.es/ecma262/#sec-exports-runtime-semantics-evaluation
Completion ExportStatement::execute(Interpreter& interpreter, GlobalObject& global_object) const
{
    InterpreterNodeScope node_scope { interpreter, *this };

    if (!is_default_export()) {
        if (m_statement) {
            // 1. Return the result of evaluating <Thing>.
            return m_statement->execute(interpreter, global_object);
        }

        // 1. Return NormalCompletion(empty).
        return normal_completion({});
    }

    VERIFY(m_statement);

    // ExportDeclaration : export default HoistableDeclaration
    if (is<FunctionDeclaration>(*m_statement)) {
        // 1. Return the result of evaluating HoistableDeclaration.
        return m_statement->execute(interpreter, global_object);
    }

    // ExportDeclaration : export default ClassDeclaration
    // ClassDeclaration: class BindingIdentifier[?Yield, ?Await] ClassTail[?Yield, ?Await]
    if (is<ClassDeclaration>(*m_statement)) {
        auto const& class_declaration = static_cast<ClassDeclaration const&>(*m_statement);

        // 1. Let value be ? BindingClassDeclarationEvaluation of ClassDeclaration.
        auto value = TRY(binding_class_declaration_evaluation(interpreter, global_object, class_declaration.m_class_expression));

        // 2. Let className be the sole element of BoundNames of ClassDeclaration.
        // 3. If className is "*default*", then
        // Note: We never go into step 3. since a ClassDeclaration always has a name and "*default*" is not a class name.
        (void)value;

        // 4. Return NormalCompletion(empty).
        return normal_completion({});
    }

    // ExportDeclaration : export default ClassDeclaration
    // ClassDeclaration: [+Default] class ClassTail [?Yield, ?Await]
    if (is<ClassExpression>(*m_statement)) {
        auto& class_expression = static_cast<ClassExpression const&>(*m_statement);

        // 1. Let value be ? BindingClassDeclarationEvaluation of ClassDeclaration.
        auto value = TRY(binding_class_declaration_evaluation(interpreter, global_object, class_expression));

        // 2. Let className be the sole element of BoundNames of ClassDeclaration.
        // 3. If className is "*default*", then
        if (!class_expression.has_name()) {
            // Note: This can only occur if the class does not have a name since "*default*" is normally not valid.

            // a. Let env be the running execution context's LexicalEnvironment.
            auto* env = interpreter.lexical_environment();

            // b. Perform ? InitializeBoundName("*default*", value, env).
            TRY(initialize_bound_name(global_object, ExportStatement::local_name_for_default, value, env));
        }

        // 4. Return NormalCompletion(empty).
        return normal_completion({});
    }

    // ExportDeclaration : export default AssignmentExpression ;

    // 1. If IsAnonymousFunctionDefinition(AssignmentExpression) is true, then
    //     a. Let value be ? NamedEvaluation of AssignmentExpression with argument "default".
    // 2. Else,
    //     a. Let rhs be the result of evaluating AssignmentExpression.
    //     b. Let value be ? GetValue(rhs).
    auto value = TRY(interpreter.vm().named_evaluation_if_anonymous_function(global_object, *m_statement, "default"));

    // 3. Let env be the running execution context's LexicalEnvironment.
    auto* env = interpreter.lexical_environment();

    // 4. Perform ? InitializeBoundName("*default*", value, env).
    TRY(initialize_bound_name(global_object, ExportStatement::local_name_for_default, value, env));

    // 5. Return NormalCompletion(empty).
    return normal_completion({});
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

    auto string_or_null = [](String const& string) -> String {
        if (string.is_empty()) {
            return "null";
        }
        return String::formatted("\"{}\"", string);
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

bool ExportStatement::has_export(FlyString const& export_name) const
{
    return any_of(m_entries.begin(), m_entries.end(), [&](auto& entry) {
        return entry.export_name == export_name;
    });
}

bool ImportStatement::has_bound_name(FlyString const& name) const
{
    return any_of(m_entries.begin(), m_entries.end(), [&](auto& entry) {
        return entry.local_name == name;
    });
}

// 14.2.3 BlockDeclarationInstantiation ( code, env ), https://tc39.es/ecma262/#sec-blockdeclarationinstantiation
void ScopeNode::block_declaration_instantiation(GlobalObject& global_object, Environment* environment) const
{
    // See also B.3.2.6 Changes to BlockDeclarationInstantiation, https://tc39.es/ecma262/#sec-web-compat-blockdeclarationinstantiation
    VERIFY(environment);
    auto* private_environment = global_object.vm().running_execution_context().private_environment;
    // Note: All the calls here are ! and thus we do not need to TRY this callback.
    for_each_lexically_scoped_declaration([&](Declaration const& declaration) {
        auto is_constant_declaration = declaration.is_constant_declaration();
        declaration.for_each_bound_name([&](auto const& name) {
            if (is_constant_declaration) {
                MUST(environment->create_immutable_binding(global_object, name, true));
            } else {
                if (!MUST(environment->has_binding(name)))
                    MUST(environment->create_mutable_binding(global_object, name, false));
            }
        });

        if (is<FunctionDeclaration>(declaration)) {
            auto& function_declaration = static_cast<FunctionDeclaration const&>(declaration);
            auto* function = ECMAScriptFunctionObject::create(global_object, function_declaration.name(), function_declaration.source_text(), function_declaration.body(), function_declaration.parameters(), function_declaration.function_length(), environment, private_environment, function_declaration.kind(), function_declaration.is_strict_mode(), function_declaration.might_need_arguments_object(), function_declaration.contains_direct_call_to_eval());
            VERIFY(is<DeclarativeEnvironment>(*environment));
            static_cast<DeclarativeEnvironment&>(*environment).initialize_or_set_mutable_binding({}, global_object, function_declaration.name(), function);
        }
    });
}

// 16.1.7 GlobalDeclarationInstantiation ( script, env ), https://tc39.es/ecma262/#sec-globaldeclarationinstantiation
ThrowCompletionOr<void> Program::global_declaration_instantiation(Interpreter& interpreter, GlobalObject& global_object, GlobalEnvironment& global_environment) const
{
    // 1. Let lexNames be the LexicallyDeclaredNames of script.
    // 2. Let varNames be the VarDeclaredNames of script.
    // 3. For each element name of lexNames, do
    TRY(for_each_lexically_declared_name([&](FlyString const& name) -> ThrowCompletionOr<void> {
        // a. If env.HasVarDeclaration(name) is true, throw a SyntaxError exception.
        if (global_environment.has_var_declaration(name))
            return interpreter.vm().throw_completion<SyntaxError>(global_object, ErrorType::TopLevelVariableAlreadyDeclared, name);

        // b. If env.HasLexicalDeclaration(name) is true, throw a SyntaxError exception.
        if (global_environment.has_lexical_declaration(name))
            return interpreter.vm().throw_completion<SyntaxError>(global_object, ErrorType::TopLevelVariableAlreadyDeclared, name);

        // c. Let hasRestrictedGlobal be ? env.HasRestrictedGlobalProperty(name).
        auto has_restricted_global = TRY(global_environment.has_restricted_global_property(name));

        // d. If hasRestrictedGlobal is true, throw a SyntaxError exception.
        if (has_restricted_global)
            return interpreter.vm().throw_completion<SyntaxError>(global_object, ErrorType::RestrictedGlobalProperty, name);

        return {};
    }));

    // 4. For each element name of varNames, do
    TRY(for_each_var_declared_name([&](auto const& name) -> ThrowCompletionOr<void> {
        // a. If env.HasLexicalDeclaration(name) is true, throw a SyntaxError exception.
        if (global_environment.has_lexical_declaration(name))
            return interpreter.vm().throw_completion<SyntaxError>(global_object, ErrorType::TopLevelVariableAlreadyDeclared, name);

        return {};
    }));

    // 5. Let varDeclarations be the VarScopedDeclarations of script.
    // 6. Let functionsToInitialize be a new empty List.
    Vector<FunctionDeclaration const&> functions_to_initialize;

    // 7. Let declaredFunctionNames be a new empty List.
    HashTable<FlyString> declared_function_names;

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
            return interpreter.vm().throw_completion<TypeError>(global_object, ErrorType::CannotDeclareGlobalFunction, function.name());

        // 3. Append fn to declaredFunctionNames.
        // Note: Already done in step iv. above.

        // 4. Insert d as the first element of functionsToInitialize.
        functions_to_initialize.append(function);
        return {};
    }));

    // 9. Let declaredVarNames be a new empty List.
    HashTable<FlyString> declared_var_names;

    // 10. For each element d of varDeclarations, do
    TRY(for_each_var_scoped_variable_declaration([&](Declaration const& declaration) {
        // a. If d is a VariableDeclaration, a ForBinding, or a BindingIdentifier, then
        // Note: This is done in for_each_var_scoped_variable_declaration.

        // i. For each String vn of the BoundNames of d, do
        return declaration.for_each_bound_name([&](auto const& name) -> ThrowCompletionOr<void> {
            // 1. If vn is not an element of declaredFunctionNames, then
            if (declared_function_names.contains(name))
                return {};

            // a. Let vnDefinable be ? env.CanDeclareGlobalVar(vn).
            auto var_definable = TRY(global_environment.can_declare_global_var(name));

            // b. If vnDefinable is false, throw a TypeError exception.
            if (!var_definable)
                return interpreter.vm().throw_completion<TypeError>(global_object, ErrorType::CannotDeclareGlobalVariable, name);

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
            auto& function_name = function_declaration.name();

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
            //     v. Return NormalCompletion(empty).
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
        return declaration.for_each_bound_name([&](auto const& name) -> ThrowCompletionOr<void> {
            // i. If IsConstantDeclaration of d is true, then
            if (declaration.is_constant_declaration()) {
                // 1. Perform ? env.CreateImmutableBinding(dn, true).
                TRY(global_environment.create_immutable_binding(global_object, name, true));
            }
            // ii. Else,
            else {
                // 1. Perform ? env.CreateMutableBinding(dn, false).
                TRY(global_environment.create_mutable_binding(global_object, name, false));
            }

            return {};
        });
    }));

    // 16. For each Parse Node f of functionsToInitialize, do
    for (auto& declaration : functions_to_initialize) {
        // a. Let fn be the sole element of the BoundNames of f.
        // b. Let fo be InstantiateFunctionObject of f with arguments env and privateEnv.
        auto* function = ECMAScriptFunctionObject::create(global_object, declaration.name(), declaration.source_text(), declaration.body(), declaration.parameters(), declaration.function_length(), &global_environment, private_environment, declaration.kind(), declaration.is_strict_mode(), declaration.might_need_arguments_object(), declaration.contains_direct_call_to_eval());

        // c. Perform ? env.CreateGlobalFunctionBinding(fn, fo, false).
        TRY(global_environment.create_global_function_binding(declaration.name(), function, false));
    }

    // 17. For each String vn of declaredVarNames, do
    for (auto& var_name : declared_var_names) {
        // a. Perform ? env.CreateGlobalVarBinding(vn, false).
        TRY(global_environment.create_global_var_binding(var_name, false));
    }

    // 18. Return NormalCompletion(empty).
    return {};
}

ModuleRequest::ModuleRequest(FlyString module_specifier_, Vector<Assertion> assertions_)
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

}
