/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/FlyString.h>

namespace Web {

class QualifiedName {
public:
    QualifiedName(FlyString const& local_name, FlyString const& prefix, FlyString const& namespace_)
        : m_local_name(local_name)
        , m_prefix(prefix)
        , m_namespace(namespace_)
    {
        make_internal_string();
    }

    FlyString const& local_name() const { return m_local_name; }
    FlyString const& prefix() const { return m_prefix; }
    FlyString const& namespace_() const { return m_namespace; }

    String const& as_string() const { return m_as_string; }

private:
    FlyString m_local_name;
    FlyString m_prefix;
    FlyString m_namespace;
    String m_as_string;

    // https://dom.spec.whatwg.org/#concept-attribute-qualified-name
    // https://dom.spec.whatwg.org/#concept-element-qualified-name
    void make_internal_string()
    {
        // This is possible to do according to the spec: "User agents could have this as an internal slot as an optimization."
        if (m_prefix.is_null()) {
            m_as_string = m_local_name;
            return;
        }

        m_as_string = String::formatted("{}:{}", m_prefix, m_local_name);
    }
};

}
