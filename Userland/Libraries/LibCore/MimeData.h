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

    ByteBuffer data(DeprecatedString const& mime_type) const { return m_data.get(mime_type).value_or({}); }
    void set_data(DeprecatedString const& mime_type, ByteBuffer&& data) { m_data.set(mime_type, move(data)); }

    bool has_format(DeprecatedString const& mime_type) const { return m_data.contains(mime_type); }
    Vector<DeprecatedString> formats() const;

    // Convenience helpers for "text/plain"
    bool has_text() const { return has_format("text/plain"); }
    DeprecatedString text() const;
    void set_text(DeprecatedString const&);

    // Convenience helpers for "text/uri-list"
    bool has_urls() const { return has_format("text/uri-list"); }
    Vector<URL> urls() const;
    ErrorOr<void> set_urls(Vector<URL> const&);

    HashMap<DeprecatedString, ByteBuffer> const& all_data() const { return m_data; }

private:
    MimeData() = default;
    explicit MimeData(HashMap<DeprecatedString, ByteBuffer> const& data)
        : m_data(data.clone().release_value_but_fixme_should_propagate_errors())
    {
    }

    HashMap<DeprecatedString, ByteBuffer> m_data;
};

StringView guess_mime_type_based_on_filename(StringView);

Optional<StringView> guess_mime_type_based_on_sniffed_bytes(ReadonlyBytes);
Optional<StringView> guess_mime_type_based_on_sniffed_bytes(Core::File&);

}
