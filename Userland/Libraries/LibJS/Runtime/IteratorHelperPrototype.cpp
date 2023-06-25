/*
 * Copyright (c) 2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/IteratorHelperPrototype.h>
#include <LibJS/Runtime/IteratorOperations.h>
#include <LibJS/Runtime/Realm.h>

namespace JS {

IteratorHelperPrototype::IteratorHelperPrototype(Realm& realm)
    : PrototypeObject(realm.intrinsics().iterator_prototype())
{
}

ThrowCompletionOr<void> IteratorHelperPrototype::initialize(Realm& realm)
{
    auto& vm = this->vm();
    MUST_OR_THROW_OOM(Base::initialize(realm));

    u8 attr = Attribute::Writable | Attribute::Configurable;
    define_native_function(realm, vm.names.next, next, 0, attr);
    define_native_function(realm, vm.names.return_, return_, 0, attr);

    // 3.1.2.1.3 %IteratorHelperPrototype% [ @@toStringTag ], https://tc39.es/proposal-iterator-helpers/#sec-%iteratorhelperprototype%-@@tostringtag
    define_direct_property(vm.well_known_symbol_to_string_tag(), MUST_OR_THROW_OOM(PrimitiveString::create(vm, "Iterator Helper"sv)), Attribute::Configurable);

    return {};
}

// 3.1.2.1.1 %IteratorHelperPrototype%.next ( ), https://tc39.es/proposal-iterator-helpers/#sec-%iteratorhelperprototype%.next
JS_DEFINE_NATIVE_FUNCTION(IteratorHelperPrototype::next)
{
    auto iterator = TRY(typed_this_object(vm));
    if (iterator->done())
        return create_iterator_result_object(vm, js_undefined(), true);

    // 1. Return ? GeneratorResume(this value, undefined, "Iterator Helper").
    auto result = TRY(iterator->closure()(*iterator));
    return create_iterator_result_object(vm, result, iterator->done());
}

// 3.1.2.1.2 %IteratorHelperPrototype%.return ( ), https://tc39.es/proposal-iterator-helpers/#sec-%iteratorhelperprototype%.return
JS_DEFINE_NATIVE_FUNCTION(IteratorHelperPrototype::return_)
{
    // 1. Let O be this value.
    // 2. Perform ? RequireInternalSlot(O, [[UnderlyingIterator]]).
    // 3. Assert: O has a [[GeneratorState]] slot.
    // 4. If O.[[GeneratorState]] is suspendedStart, then
    //     a. Set O.[[GeneratorState]] to completed.
    //     b. NOTE: Once a generator enters the completed state it never leaves it and its associated execution context is never resumed. Any execution state associated with O can be discarded at this point.
    //     c. Perform ? IteratorClose(O.[[UnderlyingIterator]], NormalCompletion(unused)).
    //     d. Return CreateIterResultObject(undefined, true).
    // 5. Let C be Completion { [[Type]]: return, [[Value]]: undefined, [[Target]]: empty }.
    // 6. Return ? GeneratorResumeAbrupt(O, C, "Iterator Helper").

    return vm.throw_completion<InternalError>(ErrorType::NotImplemented, "IteratorHelper.prototype.return"sv);
}

}
