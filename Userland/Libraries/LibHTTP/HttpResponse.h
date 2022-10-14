/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/DeprecatedString.h>
#include <AK/HashMap.h>
#include <LibCore/NetworkResponse.h>

namespace HTTP {

class HttpResponse : public Core::NetworkResponse {
public:
    virtual ~HttpResponse() override = default;
    static NonnullRefPtr<HttpResponse> create(int code, HashMap<DeprecatedString, DeprecatedString, CaseInsensitiveStringTraits>&& headers, size_t downloaded_size)
    {
        return adopt_ref(*new HttpResponse(code, move(headers), downloaded_size));
    }

    int code() const { return m_code; }
    size_t downloaded_size() const { return m_downloaded_size; }
    StringView reason_phrase() const { return reason_phrase_for_code(m_code); }
    HashMap<DeprecatedString, DeprecatedString, CaseInsensitiveStringTraits> const& headers() const { return m_headers; }

    static StringView reason_phrase_for_code(int code);

private:
    HttpResponse(int code, HashMap<DeprecatedString, DeprecatedString, CaseInsensitiveStringTraits>&&, size_t size);

    int m_code { 0 };
    HashMap<DeprecatedString, DeprecatedString, CaseInsensitiveStringTraits> m_headers;
    size_t m_downloaded_size { 0 };
};

}
