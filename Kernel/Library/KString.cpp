/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>
#include <AK/SetOnce.h>
#include <AK/StringBuilder.h>
#include <Kernel/Library/KString.h>

extern SetOnce g_not_in_early_boot;

namespace Kernel {

ErrorOr<NonnullOwnPtr<KString>> KString::try_create(StringView string)
{
    char* characters = nullptr;
    size_t length = string.length();
    auto new_string = TRY(KString::try_create_uninitialized(length, characters));
    if (!string.is_empty())
        __builtin_memcpy(characters, string.characters_without_null_termination(), length);
    characters[length] = '\0';
    return new_string;
}

ErrorOr<NonnullOwnPtr<KString>> KString::vformatted(StringView fmtstr, AK::TypeErasedFormatParams& params)
{
    StringBuilder builder;
    TRY(AK::vformat(builder, fmtstr, params));
    return try_create(builder.string_view());
}

NonnullOwnPtr<KString> KString::must_create(StringView string)
{
    // We can only enforce success during early boot.
    VERIFY(!g_not_in_early_boot.was_set());
    return KString::try_create(string).release_value();
}

ErrorOr<NonnullOwnPtr<KString>> KString::try_create_uninitialized(size_t length, char*& characters)
{
    size_t allocation_size = sizeof(KString) + (sizeof(char) * length) + sizeof(char);
    auto* slot = kmalloc(allocation_size);
    if (!slot)
        return ENOMEM;
    auto new_string = TRY(adopt_nonnull_own_or_enomem(new (slot) KString(length)));
    characters = new_string->m_characters;
    return new_string;
}

NonnullOwnPtr<KString> KString::must_create_uninitialized(size_t length, char*& characters)
{
    // We can only enforce success during early boot.
    VERIFY(!g_not_in_early_boot.was_set());
    return KString::try_create_uninitialized(length, characters).release_value();
}

ErrorOr<NonnullOwnPtr<KString>> KString::try_clone() const
{
    return try_create(view());
}

void KString::operator delete(void* string)
{
    if (!string)
        return;
    size_t allocation_size = sizeof(KString) + (sizeof(char) * static_cast<KString*>(string)->m_length) + sizeof(char);
    kfree_sized(string, allocation_size);
}

}
