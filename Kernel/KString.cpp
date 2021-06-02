/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/KString.h>

extern bool g_in_early_boot;

namespace Kernel {

OwnPtr<KString> KString::try_create(StringView const& string)
{
    char* characters = nullptr;
    size_t length = string.length();
    auto new_string = KString::try_create_uninitialized(length, characters);
    if (!new_string)
        return {};
    if (!string.is_empty())
        __builtin_memcpy(characters, string.characters_without_null_termination(), length);
    characters[length] = '\0';
    return new_string;
}

NonnullOwnPtr<KString> KString::must_create(StringView const& string)
{
    // We can only enforce success during early boot.
    VERIFY(g_in_early_boot);
    auto kstring = KString::try_create(string);
    VERIFY(kstring != nullptr);
    return kstring.release_nonnull();
}

OwnPtr<KString> KString::try_create_uninitialized(size_t length, char*& characters)
{
    size_t allocation_size = sizeof(KString) + (sizeof(char) * length) + sizeof(char);
    auto* slot = kmalloc(allocation_size);
    if (!slot)
        return {};
    auto* new_string = new (slot) KString(length);
    characters = new_string->m_characters;
    return adopt_own_if_nonnull(new_string);
}

NonnullOwnPtr<KString> KString::must_create_uninitialized(size_t length, char*& characters)
{
    // We can only enforce success during early boot.
    VERIFY(g_in_early_boot);
    auto kstring = KString::try_create_uninitialized(length, characters);
    VERIFY(kstring != nullptr);
    return kstring.release_nonnull();
}

OwnPtr<KString> KString::try_clone() const
{
    return try_create(view());
}

void KString::operator delete(void* string)
{
    kfree(string);
}

}
