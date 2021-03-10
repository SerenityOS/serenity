/*
 * Copyright (c) 2021, James Puleo <james@jame.xyz>
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

#include <AK/Optional.h>
#include <AK/String.h>
#include <AK/StringView.h>
#include <AK/Traits.h>

namespace Core {
class MimeType {
public:
    MimeType(const String& type, const String& subtype);

    const StringView& type() const { return m_type; }
    const StringView& subtype() const { return m_subtype; }
    const String& as_string() const { return m_mime; }

    static Optional<MimeType> parse(const String&);

    bool operator==(const MimeType& other) const
    {
        return as_string() == other.as_string();
    }
    bool operator!=(const MimeType& other) const { return !(*this == other); }

    struct Type {
        static constexpr const char* text = "text";
        static constexpr const char* image = "image";
        static constexpr const char* audio = "audio";
        static constexpr const char* video = "video";
        static constexpr const char* application = "application";
    };

private:
    MimeType(String mime, const StringView& type, const StringView& subtype)
        : m_mime(move(mime))
        , m_type(type)
        , m_subtype(subtype)
    {
    }

    String m_mime;
    StringView m_type;
    StringView m_subtype;
};

}

namespace AK {
template<>
struct Traits<Core::MimeType> : public AK::GenericTraits<Core::MimeType> {
    static unsigned hash(const Core::MimeType& mime) { return mime.as_string().hash(); }
};
}
