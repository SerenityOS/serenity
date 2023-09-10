/*
 * Copyright (c) 2020, the SerenityOS developers.
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/DeprecatedFlyString.h>
#include <AK/FlyString.h>
#include <AK/Optional.h>

namespace Web::DOM {

class QualifiedName {
public:
    QualifiedName(FlyString const& local_name, Optional<FlyString> const& prefix, Optional<FlyString> const& namespace_);
    QualifiedName(FlyString const& local_name, DeprecatedFlyString const& prefix, DeprecatedFlyString const& namespace_);

    FlyString const& local_name() const { return m_impl->local_name; }
    Optional<FlyString> const& prefix() const { return m_impl->prefix; }
    Optional<FlyString> const& namespace_() const { return m_impl->namespace_; }

    DeprecatedFlyString deprecated_prefix() const
    {
        if (!m_impl->prefix.has_value())
            return {};
        return m_impl->prefix->to_deprecated_fly_string();
    }

    DeprecatedFlyString deprecated_namespace_() const
    {
        if (!m_impl->namespace_.has_value())
            return {};
        return m_impl->namespace_->to_deprecated_fly_string();
    }

    FlyString const& as_string() const { return m_impl->as_string; }

    struct Impl : public RefCounted<Impl> {
        Impl(FlyString const& local_name, Optional<FlyString> const& prefix, Optional<FlyString> const& namespace_);
        ~Impl();

        void make_internal_string();
        FlyString local_name;
        Optional<FlyString> prefix;
        Optional<FlyString> namespace_;
        FlyString as_string;
    };

    void set_prefix(Optional<FlyString> value);

private:
    NonnullRefPtr<Impl> m_impl;
};

}
