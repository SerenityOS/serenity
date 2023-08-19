/*
 * Copyright (c) 2023, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/URL.h>
#include <LibWeb/CSS/Serialize.h>
#include <LibWeb/CSS/StyleValue.h>

namespace Web::CSS {

class URLStyleValue final : public StyleValueWithDefaultOperators<URLStyleValue> {
public:
    static ValueComparingNonnullRefPtr<URLStyleValue> create(AK::URL const& url)
    {
        return adopt_ref(*new (nothrow) URLStyleValue(url));
    }

    virtual ~URLStyleValue() override = default;

    AK::URL const& url() const { return m_url; }

    bool properties_equal(URLStyleValue const& other) const { return m_url == other.m_url; }

    virtual ErrorOr<String> to_string() const override
    {
        return serialize_a_url(m_url.to_deprecated_string());
    }

private:
    URLStyleValue(AK::URL const& url)
        : StyleValueWithDefaultOperators(Type::Url)
        , m_url(url)
    {
    }

    AK::URL m_url;
};

}
