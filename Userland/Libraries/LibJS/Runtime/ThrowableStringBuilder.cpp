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
    TRY_OR_THROW_OOM(m_vm, try_append(ch));
    return {};
}

ThrowCompletionOr<void> ThrowableStringBuilder::append(StringView string)
{
    TRY_OR_THROW_OOM(m_vm, try_append(string));
    return {};
}

ThrowCompletionOr<void> ThrowableStringBuilder::append(Utf16View const& string)
{
    TRY_OR_THROW_OOM(m_vm, try_append(string));
    return {};
}

ThrowCompletionOr<void> ThrowableStringBuilder::append_code_point(u32 value)
{
    TRY_OR_THROW_OOM(m_vm, try_append_code_point(value));
    return {};
}

ThrowCompletionOr<String> ThrowableStringBuilder::to_string() const
{
    return TRY_OR_THROW_OOM(m_vm, StringBuilder::to_string());
}

}
