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

#pragma once

#include <AK/ByteBuffer.h>
#include <AK/HashMap.h>
#include <AK/URL.h>
#include <LibCore/Object.h>

namespace Core {

class MimeData : public Object {
    C_OBJECT(MimeData);

public:
    virtual ~MimeData() { }

    ByteBuffer data(const String& mime_type) const { return m_data.get(mime_type).value_or({}); }
    void set_data(const String& mime_type, const ByteBuffer& data) { m_data.set(mime_type, data); }

    bool has_format(const String& mime_type) const { return m_data.contains(mime_type); }
    Vector<String> formats() const;

    // Convenience helpers for "text/plain"
    bool has_text() const { return has_format("text/plain"); }
    String text() const;
    void set_text(const String&);

    // Convenience helpers for "text/uri-list"
    bool has_urls() const { return has_format("text/uri-list"); }
    Vector<URL> urls() const;
    void set_urls(const Vector<URL>&);

private:
    MimeData() { }

    HashMap<String, ByteBuffer> m_data;
};

String guess_mime_type_based_on_filename(const URL&);

}
