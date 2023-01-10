/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGfx/Point.h>
#include <LibWeb/Bindings/PlatformObject.h>
#include <LibWeb/Forward.h>

namespace Web::Geometry {

struct DOMPointInit {
    double x { 0 };
    double y { 0 };
    double z { 0 };
    double w { 1 };
};

// https://drafts.fxtf.org/geometry/#dompointreadonly
class DOMPointReadOnly : public Bindings::PlatformObject {
    WEB_PLATFORM_OBJECT(DOMPointReadOnly, Bindings::PlatformObject);

public:
    static JS::NonnullGCPtr<DOMPointReadOnly> construct_impl(JS::Realm&, double x = 0, double y = 0, double z = 0, double w = 1);

    static JS::NonnullGCPtr<DOMPointReadOnly> from_point(JS::VM&, DOMPointInit const&);

    virtual ~DOMPointReadOnly() override;

    double x() const { return m_x; }
    double y() const { return m_y; }
    double z() const { return m_z; }
    double w() const { return m_w; }

protected:
    DOMPointReadOnly(JS::Realm&, double x, double y, double z, double w);

    virtual void initialize(JS::Realm&) override;

    double m_x;
    double m_y;
    double m_z;
    double m_w;
};

}
