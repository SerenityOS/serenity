/*
 * Copyright (c) 2023, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/Geometry/DOMMatrixReadOnly.h>

namespace Web::Geometry {

// https://drafts.fxtf.org/geometry/#dommatrix
class DOMMatrix : public DOMMatrixReadOnly {
    WEB_PLATFORM_OBJECT(DOMMatrix, Bindings::PlatformObject);

public:
    static WebIDL::ExceptionOr<JS::NonnullGCPtr<DOMMatrix>> construct_impl(JS::Realm&, Optional<Variant<String, Vector<double>>> const& init);
    static WebIDL::ExceptionOr<JS::NonnullGCPtr<DOMMatrix>> create_from_dom_matrix_2d_init(JS::Realm&, DOMMatrix2DInit& init);
    static JS::NonnullGCPtr<DOMMatrix> create_from_dom_matrix_read_only(JS::Realm&, DOMMatrixReadOnly const& read_only_matrix);

    virtual ~DOMMatrix() override;

    static WebIDL::ExceptionOr<JS::NonnullGCPtr<DOMMatrix>> from_matrix(JS::VM&, DOMMatrixInit other = {});

    void set_m11(double value);
    void set_m12(double value);
    void set_m13(double value);
    void set_m14(double value);
    void set_m21(double value);
    void set_m22(double value);
    void set_m23(double value);
    void set_m24(double value);
    void set_m31(double value);
    void set_m32(double value);
    void set_m33(double value);
    void set_m34(double value);
    void set_m41(double value);
    void set_m42(double value);
    void set_m43(double value);
    void set_m44(double value);

    void set_a(double value);
    void set_b(double value);
    void set_c(double value);
    void set_d(double value);
    void set_e(double value);
    void set_f(double value);

    WebIDL::ExceptionOr<JS::NonnullGCPtr<DOMMatrix>> multiply_self(DOMMatrixInit other = {});
    WebIDL::ExceptionOr<JS::NonnullGCPtr<DOMMatrix>> pre_multiply_self(DOMMatrixInit other = {});
    JS::NonnullGCPtr<DOMMatrix> translate_self(Optional<double> tx, Optional<double> ty, Optional<double> tz);
    JS::NonnullGCPtr<DOMMatrix> invert_self();

private:
    DOMMatrix(JS::Realm&, double m11, double m12, double m21, double m22, double m41, double m42);
    DOMMatrix(JS::Realm&, Optional<Variant<String, Vector<double>>> const& init);
    DOMMatrix(JS::Realm&, DOMMatrixReadOnly const& read_only_matrix);

    virtual void initialize(JS::Realm&) override;
};

}
