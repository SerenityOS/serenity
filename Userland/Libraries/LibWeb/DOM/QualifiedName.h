/*
 * Copyright (c) 2020, the SerenityOS developers.
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/FlyString.h>

namespace Web::DOM {

class QualifiedName {
public:
    QualifiedName(FlyString const& local_name, FlyString const& prefix, FlyString const& namespace_);

    FlyString const& local_name() const { return m_impl->local_name; }
    FlyString const& prefix() const { return m_impl->prefix; }
    FlyString const& namespace_() const { return m_impl->namespace_; }

    String const& as_string() const { return m_impl->as_string; }

    struct Impl : public RefCounted<Impl> {
        Impl(FlyString const& local_name, FlyString const& prefix, FlyString const& namespace_);
        ~Impl();

        void make_internal_string();
        FlyString local_name;
        FlyString prefix;
        FlyString namespace_;
        String as_string;
    };

private:
    NonnullRefPtr<Impl> m_impl;
};

}
