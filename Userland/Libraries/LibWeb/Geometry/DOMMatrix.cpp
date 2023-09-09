/*
 * Copyright (c) 2023, Luke Wilde <lukew@serenityos.org>
 * Copyright (c) 2023, Bastiaan van der Plaat <bastiaan.v.d.plaat@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/Geometry/DOMMatrix.h>
#include <LibWeb/WebIDL/ExceptionOr.h>

namespace Web::Geometry {

WebIDL::ExceptionOr<JS::NonnullGCPtr<DOMMatrix>> DOMMatrix::construct_impl(JS::Realm& realm, Optional<Variant<String, Vector<double>>> const& init)
{
    auto& vm = realm.vm();

    // https://drafts.fxtf.org/geometry/#dom-dommatrix-dommatrix
    if (init.has_value()) {
        // -> Otherwise
        //        Throw a TypeError exception.
        // The only condition where this can be met is with a sequence type which doesn't have exactly 6 or 16 elements.
        if (auto* double_sequence = init.value().get_pointer<Vector<double>>(); double_sequence && (double_sequence->size() != 6 && double_sequence->size() != 16))
            return WebIDL::SimpleException { WebIDL::SimpleExceptionType::TypeError, TRY_OR_THROW_OOM(vm, String::formatted("Sequence must contain exactly 6 or 16 elements, got {} element(s)", double_sequence->size())) };
    }

    return realm.heap().allocate<DOMMatrix>(realm, realm, init);
}

// https://drafts.fxtf.org/geometry/#create-a-dommatrix-from-the-2d-dictionary
WebIDL::ExceptionOr<JS::NonnullGCPtr<DOMMatrix>> DOMMatrix::create_from_dom_matrix_2d_init(JS::Realm& realm, DOMMatrix2DInit& init)
{
    // 1. Validate and fixup (2D) other.
    TRY(validate_and_fixup_dom_matrix_2d_init(init));

    // These should all have values after calling `validate_and_fixup_dom_matrix_2d_init`
    VERIFY(init.m11.has_value());
    VERIFY(init.m12.has_value());
    VERIFY(init.m21.has_value());
    VERIFY(init.m22.has_value());
    VERIFY(init.m41.has_value());
    VERIFY(init.m42.has_value());

    // 2. Return the result of invoking create a 2d matrix of type DOMMatrixReadOnly or DOMMatrix as appropriate, with a sequence of numbers,
    //    the values being the 6 elements m11, m12, m21, m22, m41 and m42 of other in the given order.
    return realm.heap().allocate<DOMMatrix>(realm, realm, init.m11.value(), init.m12.value(), init.m21.value(), init.m22.value(), init.m41.value(), init.m42.value());
}

JS::NonnullGCPtr<DOMMatrix> DOMMatrix::create_from_dom_matrix_read_only(JS::Realm& realm, DOMMatrixReadOnly const& read_only_matrix)
{
    return realm.heap().allocate<DOMMatrix>(realm, realm, read_only_matrix);
}

DOMMatrix::DOMMatrix(JS::Realm& realm, double m11, double m12, double m21, double m22, double m41, double m42)
    : DOMMatrixReadOnly(realm, m11, m12, m21, m22, m41, m42)
{
}

DOMMatrix::DOMMatrix(JS::Realm& realm, Optional<Variant<String, Vector<double>>> const& init)
    : DOMMatrixReadOnly(realm, init)
{
}

DOMMatrix::DOMMatrix(JS::Realm& realm, DOMMatrixReadOnly const& read_only_matrix)
    : DOMMatrixReadOnly(realm, read_only_matrix)
{
}

DOMMatrix::~DOMMatrix() = default;

void DOMMatrix::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    set_prototype(&Bindings::ensure_web_prototype<Bindings::DOMMatrixPrototype>(realm, "DOMMatrix"));
}

// https://drafts.fxtf.org/geometry/#dom-dommatrix-frommatrix
WebIDL::ExceptionOr<JS::NonnullGCPtr<DOMMatrix>> DOMMatrix::from_matrix(JS::VM& vm, DOMMatrixInit other)
{
    return create_from_dom_matrix_2d_init(*vm.current_realm(), other);
}

// https://drafts.fxtf.org/geometry/#dom-dommatrixreadonly-m11
void DOMMatrix::set_m11(double value)
{
    // For the DOMMatrix interface, setting the m11 or the a attribute must set the m11 element to the new value.
    m_matrix.elements()[0][0] = value;
}

// https://drafts.fxtf.org/geometry/#dom-dommatrixreadonly-m12
void DOMMatrix::set_m12(double value)
{
    // For the DOMMatrix interface, setting the m12 or the b attribute must set the m12 element to the new value.
    m_matrix.elements()[1][0] = value;
}

// https://drafts.fxtf.org/geometry/#dom-dommatrixreadonly-m13
void DOMMatrix::set_m13(double value)
{
    // For the DOMMatrix interface, setting the m13 attribute must set the m13 element to the new value and, if the new value is not 0 or -0, set is 2D to false.
    m_matrix.elements()[2][0] = value;
    if (value != 0.0 && value != -0.0)
        m_is_2d = false;
}

// https://drafts.fxtf.org/geometry/#dom-dommatrixreadonly-m14
void DOMMatrix::set_m14(double value)
{
    // For the DOMMatrix interface, setting the m14 attribute must set the m14 element to the new value and, if the new value is not 0 or -0, set is 2D to false.
    m_matrix.elements()[3][0] = value;
    if (value != 0.0 && value != -0.0)
        m_is_2d = false;
}

// https://drafts.fxtf.org/geometry/#dom-dommatrixreadonly-m21
void DOMMatrix::set_m21(double value)
{
    // For the DOMMatrix interface, setting the m21 or the c attribute must set the m21 element to the new value.
    m_matrix.elements()[0][1] = value;
}

// https://drafts.fxtf.org/geometry/#dom-dommatrixreadonly-m22
void DOMMatrix::set_m22(double value)
{
    // For the DOMMatrix interface, setting the m22 or the d attribute must set the m22 element to the new value.
    m_matrix.elements()[1][1] = value;
}

// https://drafts.fxtf.org/geometry/#dom-dommatrixreadonly-m23
void DOMMatrix::set_m23(double value)
{
    // For the DOMMatrix interface, setting the m23 attribute must set the m23 element to the new value and, if the new value is not 0 or -0, set is 2D to false.
    m_matrix.elements()[2][1] = value;
    if (value != 0.0 && value != -0.0)
        m_is_2d = false;
}

// https://drafts.fxtf.org/geometry/#dom-dommatrixreadonly-m24
void DOMMatrix::set_m24(double value)
{
    // For the DOMMatrix interface, setting the m24 attribute must set the m24 element to the new value and, if the new value is not 0 or -0, set is 2D to false.
    m_matrix.elements()[3][1] = value;
    if (value != 0.0 && value != -0.0)
        m_is_2d = false;
}

// https://drafts.fxtf.org/geometry/#dom-dommatrixreadonly-m31
void DOMMatrix::set_m31(double value)
{
    // For the DOMMatrix interface, setting the m31 attribute must set the m31 element to the new value and, if the new value is not 0 or -0, set is 2D to false.
    m_matrix.elements()[0][2] = value;
    if (value != 0.0 && value != -0.0)
        m_is_2d = false;
}

// https://drafts.fxtf.org/geometry/#dom-dommatrixreadonly-m32
void DOMMatrix::set_m32(double value)
{
    // For the DOMMatrix interface, setting the m32 attribute must set the m32 element to the new value and, if the new value is not 0 or -0, set is 2D to false.
    m_matrix.elements()[1][2] = value;
    if (value != 0.0 && value != -0.0)
        m_is_2d = false;
}

// https://drafts.fxtf.org/geometry/#dom-dommatrixreadonly-m33
void DOMMatrix::set_m33(double value)
{
    // For the DOMMatrix interface, setting the m33 attribute must set the m33 element to the new value and, if the new value is not 1, set is 2D to false.
    m_matrix.elements()[2][2] = value;
    if (value != 1.0)
        m_is_2d = false;
}

// https://drafts.fxtf.org/geometry/#dom-dommatrixreadonly-m34
void DOMMatrix::set_m34(double value)
{
    // For the DOMMatrix interface, setting the m34 attribute must set the m34 element to the new value and, if the new value is not 0 or -0, set is 2D to false.
    m_matrix.elements()[3][2] = value;
    if (value != 0.0 && value != -0.0)
        m_is_2d = false;
}

// https://drafts.fxtf.org/geometry/#dom-dommatrixreadonly-m41
void DOMMatrix::set_m41(double value)
{
    // For the DOMMatrix interface, setting the m41 or the e attribute must set the m41 element to the new value.
    m_matrix.elements()[0][3] = value;
}

// https://drafts.fxtf.org/geometry/#dom-dommatrixreadonly-m42
void DOMMatrix::set_m42(double value)
{
    // For the DOMMatrix interface, setting the m42 or the f attribute must set the m42 element to the new value.
    m_matrix.elements()[1][3] = value;
}

// https://drafts.fxtf.org/geometry/#dom-dommatrixreadonly-m43
void DOMMatrix::set_m43(double value)
{
    // For the DOMMatrix interface, setting the m43 attribute must set the m43 element to the new value and, if the new value is not 0 or -0, set is 2D to false.
    m_matrix.elements()[2][3] = value;
    if (value != 0.0 && value != -0.0)
        m_is_2d = false;
}

// https://drafts.fxtf.org/geometry/#dom-dommatrixreadonly-m44
void DOMMatrix::set_m44(double value)
{
    // For the DOMMatrix interface, setting the m44 attribute must set the m44 element to the new value and, if the new value is not 1, set is 2D to false.
    m_matrix.elements()[3][3] = value;
    if (value != 1.0)
        m_is_2d = false;
}

// https://drafts.fxtf.org/geometry/#dom-dommatrixreadonly-a
void DOMMatrix::set_a(double value)
{
    // For the DOMMatrix interface, setting the m11 or the a attribute must set the m11 element to the new value.
    set_m11(value);
}

// https://drafts.fxtf.org/geometry/#dom-dommatrixreadonly-b
void DOMMatrix::set_b(double value)
{
    // For the DOMMatrix interface, setting the m12 or the b attribute must set the m12 element to the new value.
    set_m12(value);
}

// https://drafts.fxtf.org/geometry/#dom-dommatrixreadonly-c
void DOMMatrix::set_c(double value)
{
    // For the DOMMatrix interface, setting the m21 or the c attribute must set the m21 element to the new value.
    set_m21(value);
}

// https://drafts.fxtf.org/geometry/#dom-dommatrixreadonly-d
void DOMMatrix::set_d(double value)
{
    // For the DOMMatrix interface, setting the m22 or the d attribute must set the m22 element to the new value.
    set_m22(value);
}

// https://drafts.fxtf.org/geometry/#dom-dommatrixreadonly-e
void DOMMatrix::set_e(double value)
{
    // For the DOMMatrix interface, setting the m41 or the e attribute must set the m41 element to the new value.
    set_m41(value);
}

// https://drafts.fxtf.org/geometry/#dom-dommatrixreadonly-f
void DOMMatrix::set_f(double value)
{
    // For the DOMMatrix interface, setting the m42 or the f attribute must set the m42 element to the new value.
    set_m42(value);
}

// https://drafts.fxtf.org/geometry/#dom-dommatrix-multiplyself
WebIDL::ExceptionOr<JS::NonnullGCPtr<DOMMatrix>> DOMMatrix::multiply_self(DOMMatrixInit other)
{
    // 1. Let otherObject be the result of invoking create a DOMMatrix from the dictionary other.
    auto maybe_other_object = DOMMatrix::create_from_dom_matrix_2d_init(realm(), other);
    if (maybe_other_object.is_exception())
        return maybe_other_object.exception();
    auto other_object = maybe_other_object.release_value();

    // 2. The otherObject matrix gets post-multiplied to the current matrix.
    m_matrix = m_matrix * other_object->m_matrix;

    // 3. If is 2D of otherObject is false, set is 2D of the current matrix to false.
    if (!other_object->m_is_2d)
        m_is_2d = false;

    // 4. Return the current matrix.
    return JS::NonnullGCPtr<DOMMatrix>(*this);
}

// https://drafts.fxtf.org/geometry/#dom-dommatrix-premultiplyself
WebIDL::ExceptionOr<JS::NonnullGCPtr<DOMMatrix>> DOMMatrix::pre_multiply_self(DOMMatrixInit other)
{
    // 1. Let otherObject be the result of invoking create a DOMMatrix from the dictionary other.
    auto maybe_other_object = DOMMatrix::create_from_dom_matrix_2d_init(realm(), other);
    if (maybe_other_object.is_exception())
        return maybe_other_object.exception();
    auto other_object = maybe_other_object.release_value();

    // 2. The otherObject matrix gets pre-multiplied to the current matrix.
    m_matrix = other_object->m_matrix * m_matrix;

    // 3. If is 2D of otherObject is false, set is 2D of the current matrix to false.
    if (!other_object->m_is_2d)
        m_is_2d = false;

    // 4. Return the current matrix.
    return JS::NonnullGCPtr<DOMMatrix>(*this);
}

// https://drafts.fxtf.org/geometry/#dom-dommatrix-translateself
JS::NonnullGCPtr<DOMMatrix> DOMMatrix::translate_self(Optional<double> tx, Optional<double> ty, Optional<double> tz)
{
    // 1. Post-multiply a translation transformation on the current matrix. The 3D translation matrix is described in CSS Transforms.
    m_matrix = m_matrix * Gfx::translation_matrix(Vector3<double> { tx.value_or(0), ty.value_or(0), tz.value_or(0) });

    // 2. If tz is specified and not 0 or -0, set is 2D of the current matrix to false.
    if (tz.has_value() && (tz != 0 || tz != -0))
        m_is_2d = false;

    // 3. Return the current matrix.
    return *this;
}

// https://drafts.fxtf.org/geometry/#dom-dommatrix-skewxself
JS::NonnullGCPtr<DOMMatrix> DOMMatrix::skew_x_self(double sx)
{
    // 1. Post-multiply a skewX transformation on the current matrix by the specified angle sx in degrees. The 2D skewX matrix is described in CSS Transforms with alpha = sx in degrees. [CSS3-TRANSFORMS]
    // clang-format off
    Gfx::DoubleMatrix4x4 skew_matrix = { 1, tan(AK::to_radians(sx)), 0, 0,
        0, 1, 0, 0,
        0, 0, 1, 0,
        0, 0, 0, 1 };
    // clang-format on
    m_matrix = m_matrix * skew_matrix;

    // 3. Return the current matrix.
    return *this;
}

// https://drafts.fxtf.org/geometry/#dom-dommatrix-skewyself
JS::NonnullGCPtr<DOMMatrix> DOMMatrix::skew_y_self(double sy)
{
    // 1. Post-multiply a skewX transformation on the current matrix by the specified angle sy in degrees. The 2D skewY matrix is described in CSS Transforms with beta = sy in degrees. [CSS3-TRANSFORMS]
    // clang-format off
    Gfx::DoubleMatrix4x4 skew_matrix = { 1, 0, 0, 0,
        tan(AK::to_radians(sy)), 1, 0, 0,
        0, 0, 1, 0,
        0, 0, 0, 1 };
    // clang-format on
    m_matrix = m_matrix * skew_matrix;

    // 3. Return the current matrix.
    return *this;
}

// https://drafts.fxtf.org/geometry/#dom-dommatrix-invertself
JS::NonnullGCPtr<DOMMatrix> DOMMatrix::invert_self()
{
    bool is_invertible = m_matrix.is_invertible();

    // 1. Invert the current matrix.
    if (is_invertible)
        m_matrix = m_matrix.inverse();

    // 2. If the current matrix is not invertible set all attributes to NaN and set is 2D to false.
    if (!is_invertible) {
        for (u8 i = 0; i < 4; i++) {
            for (u8 j = 0; j < 4; j++)
                m_matrix.elements()[j][i] = NAN;
        }
        m_is_2d = false;
    }

    // 3. Return the current matrix.
    return *this;
}

}
