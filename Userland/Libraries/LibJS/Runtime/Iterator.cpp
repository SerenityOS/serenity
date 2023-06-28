/*
 * Copyright (c) 2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/AbstractOperations.h>
#include <LibJS/Runtime/Iterator.h>
#include <LibJS/Runtime/VM.h>

namespace JS {

ThrowCompletionOr<NonnullGCPtr<Iterator>> Iterator::create(Realm& realm, Object& prototype, IteratorRecord iterated)
{
    return MUST_OR_THROW_OOM(realm.heap().allocate<Iterator>(realm, prototype, move(iterated)));
}

Iterator::Iterator(Object& prototype, IteratorRecord iterated)
    : Object(ConstructWithPrototypeTag::Tag, prototype)
    , m_iterated(move(iterated))
{
}

Iterator::Iterator(Object& prototype)
    : Iterator(prototype, {})
{
}

// 2.1.1 GetIteratorDirect ( obj ), https://tc39.es/proposal-iterator-helpers/#sec-getiteratorflattenable
ThrowCompletionOr<IteratorRecord> get_iterator_direct(VM& vm, Object& object)
{
    // 1. Let nextMethod be ? Get(obj, "next").
    auto next_method = TRY(object.get(vm.names.next));

    // 2. Let iteratorRecord be Record { [[Iterator]]: obj, [[NextMethod]]: nextMethod, [[Done]]: false }.
    IteratorRecord iterator_record { .iterator = object, .next_method = next_method, .done = false };

    // 3. Return iteratorRecord.
    return iterator_record;
}

ThrowCompletionOr<IteratorRecord> get_iterator_flattenable(VM& vm, Value object)
{
    // 1. If obj is not an Object, throw a TypeError exception.
    if (!object.is_object())
        return vm.throw_completion<TypeError>(ErrorType::NotAnObject, "obj"sv);

    // 2. Let method be ? GetMethod(obj, @@iterator).
    auto method = TRY(object.get_method(vm, vm.well_known_symbol_iterator()));

    Value iterator;

    // 3. If method is undefined, then
    if (!method) {
        // a. Let iterator be obj.
        iterator = object;
    }
    // 4. Else,
    else {
        // a. Let iterator be ? Call(method, obj).
        iterator = TRY(call(vm, method, object));
    }

    // 5. If iterator is not an Object, throw a TypeError exception.
    if (!iterator.is_object())
        return vm.throw_completion<TypeError>(ErrorType::NotAnObject, "iterator"sv);

    // 6. Return ? GetIteratorDirect(iterator).
    return TRY(get_iterator_direct(vm, iterator.as_object()));
}

}
