/*
 * Copyright (c) 2023, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/Error.h>
#include <LibWeb/WebIDL/ExceptionOr.h>

namespace Web::Streams {

// https://streams.spec.whatwg.org/#value-with-size
struct ValueWithSize {
    JS::Value value;
    double size;
};

// https://streams.spec.whatwg.org/#dequeue-value
template<typename T>
JS::Value dequeue_value(T& container)
{
    // 1. Assert: container has [[queue]] and [[queueTotalSize]] internal slots.

    // 2. Assert: container.[[queue]] is not empty.
    VERIFY(!container.queue().is_empty());

    // 3. Let valueWithSize be container.[[queue]][0].
    // 4. Remove valueWithSize from container.[[queue]].
    auto value_with_size = container.queue().take_first();

    // 5. Set container.[[queueTotalSize]] to container.[[queueTotalSize]] − valueWithSize’s size.
    container.set_queue_total_size(container.queue_total_size() - value_with_size.size);

    // 6. If container.[[queueTotalSize]] < 0, set container.[[queueTotalSize]] to 0. (This can occur due to rounding errors.)
    if (container.queue_total_size() < 0.0)
        container.set_queue_total_size(0.0);

    // 7. Return valueWithSize’s value.
    return value_with_size.value;
}

// https://streams.spec.whatwg.org/#enqueue-value-with-size
template<typename T>
WebIDL::ExceptionOr<void> enqueue_value_with_size(T& container, JS::Value value, double size)
{
    // 1. Assert: container has [[queue]] and [[queueTotalSize]] internal slots.

    // 2. If ! IsNonNegativeNumber(size) is false, throw a RangeError exception.
    if (size < 0.0)
        return WebIDL::SimpleException { WebIDL::SimpleExceptionType::RangeError, "Chunk has negative size"sv };

    // 3. If size is +∞, throw a RangeError exception.
    if (size == HUGE_VAL)
        return WebIDL::SimpleException { WebIDL::SimpleExceptionType::RangeError, "Chunk has infinite size"sv };

    // 4. Append a new value-with-size with value value and size size to container.[[queue]].
    container.queue().append({ value, size });

    // 5. Set container.[[queueTotalSize]] to container.[[queueTotalSize]] + size.
    container.set_queue_total_size(container.queue_total_size() + size);

    return {};
}

// https://streams.spec.whatwg.org/#peek-queue-value
template<typename T>
JS::Value peek_queue_value(T& container)
{
    // 1. Assert: container has [[queue]] and [[queueTotalSize]] internal slots.

    // 2. Assert: container.[[queue]] is not empty.
    VERIFY(!container.queue().is_empty());

    // 3. Let valueWithSize be container.[[queue]][0].
    auto& value_with_size = container.queue().first();

    // 4. Return valueWithSize’s value.
    return value_with_size.value;
}

// https://streams.spec.whatwg.org/#reset-queue
template<typename T>
void reset_queue(T& container)
{
    // 1. Assert: container has [[queue]] and [[queueTotalSize]] internal slots.

    // 2. Set container.[[queue]] to a new empty list.
    container.queue().clear();

    // 3. Set container.[[queueTotalSize]] to 0.
    container.set_queue_total_size(0);
}

}
