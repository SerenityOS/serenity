/*
 * Copyright (c) 2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Utf16View.h>
#include <LibJS/Runtime/ThrowableStringBuilder.h>

namespace JS {

ThrowableStringBuilder::ThrowableStringBuilder(VM& vm)
    : m_vm(vm)
{
}

ThrowCompletionOr<void> ThrowableStringBuilder::append(char ch)
{
    if (try_append(ch).is_error())
        return m_vm.throw_completion<InternalError>(ErrorType::NotEnoughMemoryToAllocate, length() + 1);
    return {};
}

ThrowCompletionOr<void> ThrowableStringBuilder::append(StringView string)
{
    if (try_append(string).is_error())
        return m_vm.throw_completion<InternalError>(ErrorType::NotEnoughMemoryToAllocate, length() + string.length());
    return {};
}

ThrowCompletionOr<void> ThrowableStringBuilder::append(Utf16View const& string)
{
    if (try_append(string).is_error())
        return m_vm.throw_completion<InternalError>(ErrorType::NotEnoughMemoryToAllocate, length() + (string.length_in_code_units() * 2));
    return {};
}

ThrowCompletionOr<void> ThrowableStringBuilder::append_code_point(u32 value)
{
    if (auto result = try_append_code_point(value); result.is_error())
        return m_vm.throw_completion<InternalError>(ErrorType::NotEnoughMemoryToAllocate, length() + sizeof(value));
    return {};
}

}
