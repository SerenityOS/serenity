/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashMap.h>
#include <AK/String.h>
#include <LibCore/NetworkResponse.h>

namespace HTTP {

class HttpResponse : public Core::NetworkResponse {
public:
    virtual ~HttpResponse() override;
    static NonnullRefPtr<HttpResponse> create(int code, HashMap<String, String, CaseInsensitiveStringTraits>&& headers, size_t downloaded_size)
    {
        return adopt_ref(*new HttpResponse(code, move(headers), downloaded_size));
    }

    int code() const { return m_code; }
    size_t downloaded_size() const { return m_downloaded_size; }
    StringView reason_phrase() const { return reason_phrase_for_code(m_code); }
    HashMap<String, String, CaseInsensitiveStringTraits> const& headers() const { return m_headers; }

    static StringView reason_phrase_for_code(int code);

private:
    HttpResponse(int code, HashMap<String, String, CaseInsensitiveStringTraits>&&, size_t size);

    int m_code { 0 };
    HashMap<String, String, CaseInsensitiveStringTraits> m_headers;
    size_t m_downloaded_size { 0 };
};

}
