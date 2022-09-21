/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGfx/Point.h>
#include <LibWeb/Bindings/PlatformObject.h>
#include <LibWeb/Forward.h>

namespace Web::Geometry {

// https://drafts.fxtf.org/geometry/#dompointreadonly
class DOMPointReadOnly : public Bindings::PlatformObject {
    WEB_PLATFORM_OBJECT(DOMPointReadOnly, Bindings::PlatformObject);

public:
    static JS::NonnullGCPtr<DOMPointReadOnly> create_with_global_object(HTML::Window&, double x = 0, double y = 0, double z = 0, double w = 0);

    virtual ~DOMPointReadOnly() override;

    double x() const { return m_x; }
    double y() const { return m_y; }
    double z() const { return m_z; }
    double w() const { return m_w; }

protected:
    DOMPointReadOnly(HTML::Window&, double x, double y, double z, double w);

    double m_x;
    double m_y;
    double m_z;
    double m_w;
};

}
