/*
 * Copyright (c) 2023, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGfx/Matrix4x4.h>
#include <LibWeb/Bindings/PlatformObject.h>
#include <LibWeb/Forward.h>

namespace Web::Geometry {

// https://drafts.fxtf.org/geometry/#dictdef-dommatrix2dinit
struct DOMMatrix2DInit {
    Optional<double> a;
    Optional<double> b;
    Optional<double> c;
    Optional<double> d;
    Optional<double> e;
    Optional<double> f;
    Optional<double> m11;
    Optional<double> m12;
    Optional<double> m21;
    Optional<double> m22;
    Optional<double> m41;
    Optional<double> m42;
};

// https://drafts.fxtf.org/geometry/#dictdef-dommatrixinit
struct DOMMatrixInit : public DOMMatrix2DInit {
    double m13 { 0.0 };
    double m14 { 0.0 };
    double m23 { 0.0 };
    double m24 { 0.0 };
    double m31 { 0.0 };
    double m32 { 0.0 };
    double m33 { 0.0 };
    double m34 { 0.0 };
    double m43 { 0.0 };
    double m44 { 0.0 };
    Optional<bool> is2d;
};

// https://drafts.fxtf.org/geometry/#dommatrixreadonly
class DOMMatrixReadOnly : public Bindings::PlatformObject {
    WEB_PLATFORM_OBJECT(DOMMatrixReadOnly, Bindings::PlatformObject);

public:
    static WebIDL::ExceptionOr<JS::NonnullGCPtr<DOMMatrixReadOnly>> construct_impl(JS::Realm&, Optional<Variant<String, Vector<double>>> const& init);
    static WebIDL::ExceptionOr<JS::NonnullGCPtr<DOMMatrixReadOnly>> create_from_dom_matrix_2d_init(JS::Realm&, DOMMatrix2DInit& init);

    virtual ~DOMMatrixReadOnly() override;

    // https://drafts.fxtf.org/geometry/#dommatrix-attributes
    double m11() const { return m_matrix.elements()[0][0]; }
    double m12() const { return m_matrix.elements()[0][1]; }
    double m13() const { return m_matrix.elements()[0][2]; }
    double m14() const { return m_matrix.elements()[0][3]; }
    double m21() const { return m_matrix.elements()[1][0]; }
    double m22() const { return m_matrix.elements()[1][1]; }
    double m23() const { return m_matrix.elements()[1][2]; }
    double m24() const { return m_matrix.elements()[1][3]; }
    double m31() const { return m_matrix.elements()[2][0]; }
    double m32() const { return m_matrix.elements()[2][1]; }
    double m33() const { return m_matrix.elements()[2][2]; }
    double m34() const { return m_matrix.elements()[2][3]; }
    double m41() const { return m_matrix.elements()[3][0]; }
    double m42() const { return m_matrix.elements()[3][1]; }
    double m43() const { return m_matrix.elements()[3][2]; }
    double m44() const { return m_matrix.elements()[3][3]; }

    double a() const { return m11(); }
    double b() const { return m12(); }
    double c() const { return m21(); }
    double d() const { return m22(); }
    double e() const { return m41(); }
    double f() const { return m42(); }

    bool is2d() const { return m_is_2d; }
    bool is_identity() const;

    JS::NonnullGCPtr<DOMMatrix> inverse() const;

    WebIDL::ExceptionOr<String> to_string() const;

protected:
    DOMMatrixReadOnly(JS::Realm&, double m11, double m12, double m21, double m22, double m41, double m42);
    DOMMatrixReadOnly(JS::Realm&, double m11, double m12, double m13, double m14, double m21, double m22, double m23, double m24, double m31, double m32, double m33, double m34, double m41, double m42, double m43, double m44);
    DOMMatrixReadOnly(JS::Realm&, Optional<Variant<String, Vector<double>>> const& init);
    DOMMatrixReadOnly(JS::Realm&, DOMMatrixReadOnly const& other);

    Gfx::DoubleMatrix4x4 m_matrix { Gfx::DoubleMatrix4x4::identity() };
    bool m_is_2d { true };

private:
    void initialize_from_create_2d_matrix(double m11, double m12, double m21, double m22, double m41, double m42);
    void initialize_from_create_3d_matrix(double m11, double m12, double m13, double m14, double m21, double m22, double m23, double m24, double m31, double m32, double m33, double m34, double m41, double m42, double m43, double m44);

    virtual JS::ThrowCompletionOr<void> initialize(JS::Realm&) override;
};

WebIDL::ExceptionOr<void> validate_and_fixup_dom_matrix_2d_init(DOMMatrix2DInit& init);
WebIDL::ExceptionOr<void> validate_and_fixup_dom_matrix_init(DOMMatrixInit& init);

}
