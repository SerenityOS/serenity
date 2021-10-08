/*
 * Copyright (c) 2020, Jack Karamanian <karamanian.jack@gmail.com>
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/AbstractOperations.h>
#include <LibJS/Runtime/BoundFunction.h>
#include <LibJS/Runtime/GlobalObject.h>

namespace JS {

BoundFunction::BoundFunction(GlobalObject& global_object, FunctionObject& bound_target_function, Value bound_this, Vector<Value> bound_arguments, i32 length, Object* constructor_prototype)
    : FunctionObject(*global_object.function_prototype())
    , m_bound_target_function(&bound_target_function)
    , m_bound_this(bound_this)
    , m_bound_arguments(move(bound_arguments))
    , m_constructor_prototype(constructor_prototype)
    , m_name(String::formatted("bound {}", bound_target_function.name()))
    , m_length(length)
{
}

void BoundFunction::initialize(GlobalObject& global_object)
{
    auto& vm = this->vm();
    Base::initialize(global_object);
    define_direct_property(vm.names.length, Value(m_length), Attribute::Configurable);
}

BoundFunction::~BoundFunction()
{
}

// 10.4.1.1 [[Call]] ( thisArgument, argumentsList ), https://tc39.es/ecma262/#sec-bound-function-exotic-objects-call-thisargument-argumentslist
ThrowCompletionOr<Value> BoundFunction::internal_call([[maybe_unused]] Value this_argument, MarkedValueList arguments_list)
{
    // 1. Let target be F.[[BoundTargetFunction]].
    auto& target = *m_bound_target_function;

    // 2. Let boundThis be F.[[BoundThis]].
    auto bound_this = m_bound_this;

    // 3. Let boundArgs be F.[[BoundArguments]].
    auto& bound_args = m_bound_arguments;

    // 4. Let args be the list-concatenation of boundArgs and argumentsList.
    auto args = MarkedValueList { heap() };
    args.extend(bound_args);
    args.extend(move(arguments_list));

    // 5. Return ? Call(target, boundThis, args).
    return call(global_object(), &target, bound_this, move(args));
}

// 10.4.1.2 [[Construct]] ( argumentsList, newTarget ), https://tc39.es/ecma262/#sec-bound-function-exotic-objects-construct-argumentslist-newtarget
ThrowCompletionOr<Object*> BoundFunction::internal_construct(MarkedValueList arguments_list, FunctionObject& new_target)
{
    // 1. Let target be F.[[BoundTargetFunction]].
    auto& target = *m_bound_target_function;

    // 2. Assert: IsConstructor(target) is true.
    VERIFY(Value(&target).is_constructor());

    // 3. Let boundArgs be F.[[BoundArguments]].
    auto& bound_args = m_bound_arguments;

    // 4. Let args be the list-concatenation of boundArgs and argumentsList.
    auto args = MarkedValueList { heap() };
    args.extend(bound_args);
    args.extend(move(arguments_list));

    // 5. If SameValue(F, newTarget) is true, set newTarget to target.
    auto* final_new_target = &new_target;
    if (this == &new_target)
        final_new_target = &target;

    // 6. Return ? Construct(target, args, newTarget).
    return construct(global_object(), target, move(args), final_new_target);
}

void BoundFunction::visit_edges(Visitor& visitor)
{
    Base::visit_edges(visitor);

    visitor.visit(m_bound_target_function);
    visitor.visit(m_bound_this);
    for (auto argument : m_bound_arguments)
        visitor.visit(argument);

    visitor.visit(m_constructor_prototype);
}

}
