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
    QualifiedName(const FlyString& local_name, const FlyString& prefix, const FlyString& namespace_)
        : m_local_name(local_name)
        , m_prefix(prefix)
        , m_namespace(namespace_)
    {
    }

    const FlyString& local_name() const { return m_local_name; }
    const FlyString& prefix() const { return m_prefix; }
    const FlyString& namespace_() const { return m_namespace; }

private:
    FlyString m_local_name;
    FlyString m_prefix;
    FlyString m_namespace;
};

}
