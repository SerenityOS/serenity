/*
 * Copyright (c) 2020, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Function.h>
#include <LibJS/Runtime/AbstractOperations.h>
#include <LibJS/Runtime/FunctionObject.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/IteratorHelper.h>
#include <LibJS/Runtime/IteratorOperations.h>
#include <LibJS/Runtime/IteratorPrototype.h>

namespace JS {

// 27.1.2 The %IteratorPrototype% Object, https://tc39.es/ecma262/#sec-%iteratorprototype%-object
IteratorPrototype::IteratorPrototype(Realm& realm)
    : PrototypeObject(realm.intrinsics().object_prototype())
{
}

ThrowCompletionOr<void> IteratorPrototype::initialize(Realm& realm)
{
    auto& vm = this->vm();
    MUST_OR_THROW_OOM(Base::initialize(realm));

    // 3.1.3.13 Iterator.prototype [ @@toStringTag ], https://tc39.es/proposal-iterator-helpers/#sec-iteratorprototype-@@tostringtag
    define_direct_property(vm.well_known_symbol_to_string_tag(), MUST_OR_THROW_OOM(PrimitiveString::create(vm, "Iterator"sv)), Attribute::Configurable | Attribute::Writable);

    u8 attr = Attribute::Writable | Attribute::Configurable;
    define_native_function(realm, vm.well_known_symbol_iterator(), symbol_iterator, 0, attr);
    define_native_function(realm, vm.names.map, map, 1, attr);

    return {};
}

// 27.1.2.1 %IteratorPrototype% [ @@iterator ] ( ), https://tc39.es/ecma262/#sec-%iteratorprototype%-@@iterator
JS_DEFINE_NATIVE_FUNCTION(IteratorPrototype::symbol_iterator)
{
    // 1. Return the this value.
    return vm.this_value();
}

// 3.1.3.2 Iterator.prototype.map ( mapper ), https://tc39.es/proposal-iterator-helpers/#sec-iteratorprototype.map
JS_DEFINE_NATIVE_FUNCTION(IteratorPrototype::map)
{
    auto& realm = *vm.current_realm();

    auto mapper = vm.argument(0);

    // 1. Let O be the this value.
    // 2. If O is not an Object, throw a TypeError exception.
    auto object = TRY(this_object(vm));

    // 3. If IsCallable(mapper) is false, throw a TypeError exception.
    if (!mapper.is_function())
        return vm.throw_completion<TypeError>(ErrorType::NotAFunction, "mapper"sv);

    // 4. Let iterated be ? GetIteratorDirect(O).
    auto iterated = TRY(get_iterator_direct(vm, object));

    // 5. Let closure be a new Abstract Closure with no parameters that captures iterated and mapper and performs the following steps when called:
    IteratorHelper::Closure closure = [mapper = NonnullGCPtr { mapper.as_function() }](auto& iterator) -> ThrowCompletionOr<Value> {
        auto& vm = iterator.vm();

        auto const& iterated = iterator.underlying_iterator();

        // a. Let counter be 0.
        // b. Repeat,

        // i. Let next be ? IteratorStep(iterated).
        auto next = TRY(iterator_step(vm, iterated));

        // ii. If next is false, return undefined.
        if (!next)
            return iterator.result(js_undefined());

        // iii. Let value be ? IteratorValue(next).
        auto value = TRY(iterator_value(vm, *next));

        // iv. Let mapped be Completion(Call(mapper, undefined, « value, 𝔽(counter) »)).
        auto mapped = call(vm, *mapper, js_undefined(), value, Value { iterator.counter() });

        // v. IfAbruptCloseIterator(mapped, iterated).
        if (mapped.is_error())
            return iterator.close_result(mapped.release_error());

        // viii. Set counter to counter + 1.
        // NOTE: We do this step early to ensure it occurs before returning.
        iterator.increment_counter();

        // vi. Let completion be Completion(Yield(mapped)).
        // vii. IfAbruptCloseIterator(completion, iterated).
        return iterator.result(mapped.release_value());
    };

    // 6. Let result be CreateIteratorFromClosure(closure, "Iterator Helper", %IteratorHelperPrototype%, « [[UnderlyingIterator]] »).
    // 7. Set result.[[UnderlyingIterator]] to iterated.
    auto result = TRY(IteratorHelper::create(realm, move(iterated), move(closure)));

    // 8. Return result.
    return result;
}

}
