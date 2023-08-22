/*
 * Copyright (c) 2020, the SerenityOS developers.
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/DeprecatedFlyString.h>

namespace Web::DOM {

class QualifiedName {
public:
    QualifiedName(DeprecatedFlyString const& local_name, DeprecatedFlyString const& prefix, DeprecatedFlyString const& namespace_);

    DeprecatedFlyString const& local_name() const { return m_impl->local_name; }
    DeprecatedFlyString const& prefix() const { return m_impl->prefix; }
    DeprecatedFlyString const& namespace_() const { return m_impl->namespace_; }

    DeprecatedFlyString const& as_string() const { return m_impl->as_string; }

    struct Impl : public RefCounted<Impl> {
        Impl(DeprecatedFlyString const& local_name, DeprecatedFlyString const& prefix, DeprecatedFlyString const& namespace_);
        ~Impl();

        void make_internal_string();
        DeprecatedFlyString local_name;
        DeprecatedFlyString prefix;
        DeprecatedFlyString namespace_;
        DeprecatedFlyString as_string;
    };

    void set_prefix(DeprecatedFlyString const& value);

private:
    NonnullRefPtr<Impl> m_impl;
};

}
