/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <AK/FlyString.h>
#include <AK/HashTable.h>
#include <AK/String.h>
#include <AK/StringUtils.h>
#include <AK/StringView.h>

namespace AK {

struct FlyStringImplTraits : public AK::Traits<StringImpl*> {
    static unsigned hash(const StringImpl* s) { return s ? s->hash() : 0; }
    static bool equals(const StringImpl* a, const StringImpl* b)
    {
        ASSERT(a);
        ASSERT(b);
        if (a == b)
            return true;
        if (a->length() != b->length())
            return false;
        return !__builtin_memcmp(a->characters(), b->characters(), a->length());
    }
};

static HashTable<StringImpl*, FlyStringImplTraits>& fly_impls()
{
    static HashTable<StringImpl*, FlyStringImplTraits>* table;
    if (!table)
        table = new HashTable<StringImpl*, FlyStringImplTraits>;
    return *table;
}

void FlyString::did_destroy_impl(Badge<StringImpl>, StringImpl& impl)
{
    fly_impls().remove(&impl);
}

FlyString::FlyString(const String& string)
{
    if (string.is_null())
        return;
    auto it = fly_impls().find(const_cast<StringImpl*>(string.impl()));
    if (it == fly_impls().end()) {
        fly_impls().set(const_cast<StringImpl*>(string.impl()));
        string.impl()->set_fly({}, true);
        m_impl = string.impl();
    } else {
        ASSERT((*it)->is_fly());
        m_impl = *it;
    }
}

FlyString::FlyString(const StringView& string)
    : FlyString(static_cast<String>(string))
{
}

FlyString::FlyString(const char* string)
    : FlyString(static_cast<String>(string))
{
}

int FlyString::to_int(bool& ok) const
{
    return StringUtils::convert_to_int(view(), ok);
}

bool FlyString::equals_ignoring_case(const StringView& other) const
{
    return StringUtils::equals_ignoring_case(view(), other);
}

FlyString FlyString::to_lowercase() const
{
    return String(*m_impl).to_lowercase();
}

StringView FlyString::view() const
{
    return { characters(), length() };
}

}
