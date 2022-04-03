/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
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
    virtual ~MimeData() = default;

    ByteBuffer data(String const& mime_type) const { return m_data.get(mime_type).value_or({}); }
    void set_data(String const& mime_type, ByteBuffer&& data) { m_data.set(mime_type, move(data)); }

    bool has_format(String const& mime_type) const { return m_data.contains(mime_type); }
    Vector<String> formats() const;

    // Convenience helpers for "text/plain"
    bool has_text() const { return has_format("text/plain"); }
    String text() const;
    void set_text(String const&);

    // Convenience helpers for "text/uri-list"
    bool has_urls() const { return has_format("text/uri-list"); }
    Vector<URL> urls() const;
    void set_urls(Vector<URL> const&);

    HashMap<String, ByteBuffer> const& all_data() const { return m_data; }

private:
    MimeData() = default;
    explicit MimeData(HashMap<String, ByteBuffer> const& data)
        : m_data(data)
    {
    }

    HashMap<String, ByteBuffer> m_data;
};

String guess_mime_type_based_on_filename(StringView);

Optional<String> guess_mime_type_based_on_sniffed_bytes(ReadonlyBytes);

}
