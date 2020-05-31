/*
 * Copyright (c) 2020, The SerenityOS developers.
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

#pragma once

#include <AK/JsonObject.h>
#include <AK/String.h>

namespace VT {

enum Property {
    Progress,
    Hyperlink,
    Title,
};

struct EscapeSequenceBase {
    operator String() const
    {
        ASSERT(!cached.is_empty());
        return cached;
    }

    const char* characters() const
    {
        return cached.characters();
    }

    constexpr static const char OSC = '\033';

protected:
    template<typename KeyT, typename ValueT, typename... Rest>
    static void add_to_json(JsonObject& object, KeyT key, ValueT value, Rest... rest)
    {
        object.set(key, JsonValue(value));
        add_to_json(object, rest...);
    }

    static void add_to_json(JsonObject&)
    {
    }

    template<typename... Args>
    String encode(const StringView& name, Args... args) const
    {
        JsonObject object, containing_object;
        add_to_json(object, args...);
        containing_object.set(name, move(object));

        return String::format("%c{S%s%c", OSC, containing_object.to_string().characters(), OSC);
    }

    EscapeSequenceBase(String data)
        : cached(data)
    {
    }

    String cached;
};

template<int PropertyName>
struct EscapeSequenceFor : public EscapeSequenceBase {
};

enum UnsetTag {
    Unset
};

template<>
struct EscapeSequenceFor<Progress> : public EscapeSequenceBase {
    explicit EscapeSequenceFor(int value, int max = 100)
        : EscapeSequenceBase(encode("SetProgress", "value", value, "maximum", max))
    {
    }

    explicit EscapeSequenceFor(const UnsetTag&)
        : EscapeSequenceFor(-1, 1)
    {
    }

private:
    mutable String cached;
};

template<>
struct EscapeSequenceFor<Hyperlink> : public EscapeSequenceBase {
    explicit EscapeSequenceFor(const StringView& link, StringView id = "")
        : EscapeSequenceBase(
            String::format("%c]8;%.*s;%.*s%c\\",
                OSC,
                id.length(), id.characters_without_null_termination(),
                link.length(), link.characters_without_null_termination(),
                OSC))
    {
    }

    explicit EscapeSequenceFor(const UnsetTag&)
        : EscapeSequenceFor("", "")
    {
    }
};

template<>
struct EscapeSequenceFor<Title> : public EscapeSequenceBase {
    explicit EscapeSequenceFor(const StringView& title)
        : EscapeSequenceBase(String::format("%c]0;%s%c\\", OSC, title.length(), title.characters_without_null_termination(), OSC))
    {
    }
};

}
