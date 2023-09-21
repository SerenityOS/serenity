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
#include <LibCore/EventReceiver.h>

namespace Core {

class MimeData : public EventReceiver {
    C_OBJECT(MimeData);

public:
    virtual ~MimeData() = default;

    ByteBuffer data(StringView mime_type) const { return m_data.get(mime_type).value_or({}); }
    void set_data(DeprecatedString const& mime_type, ByteBuffer&& data) { m_data.set(mime_type, move(data)); }

    bool has_format(StringView mime_type) const { return m_data.contains(mime_type); }
    Vector<DeprecatedString> formats() const;

    // Convenience helpers for "text/plain"
    bool has_text() const { return has_format("text/plain"sv); }
    DeprecatedString text() const;
    void set_text(DeprecatedString const&);

    // Convenience helpers for "text/uri-list"
    bool has_urls() const { return has_format("text/uri-list"sv); }
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

struct MimeType {
    StringView name {};
    Vector<StringView> common_extensions {};
    StringView description {};
    Optional<Vector<u8>> magic_bytes {};
    u64 offset { 0 };
};

Optional<StringView> get_description_from_mime_type(StringView);

}
