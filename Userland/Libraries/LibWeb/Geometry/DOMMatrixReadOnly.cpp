/*
 * Copyright (c) 2023, Luke Wilde <lukew@serenityos.org>
 * Copyright (c) 2023, Bastiaan van der Plaat <bastiaan.v.d.plaat@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/Geometry/DOMMatrix.h>
#include <LibWeb/Geometry/DOMMatrixReadOnly.h>
#include <LibWeb/Geometry/DOMPoint.h>
#include <LibWeb/WebIDL/ExceptionOr.h>

namespace Web::Geometry {

WebIDL::ExceptionOr<JS::NonnullGCPtr<DOMMatrixReadOnly>> DOMMatrixReadOnly::construct_impl(JS::Realm& realm, Optional<Variant<String, Vector<double>>> const& init)
{
    auto& vm = realm.vm();

    // https://drafts.fxtf.org/geometry/#dom-dommatrixreadonly-dommatrixreadonly
    if (init.has_value()) {
        // -> Otherwise
        //        Throw a TypeError exception.
        // The only condition where this can be met is with a sequence type which doesn't have exactly 6 or 16 elements.
        if (auto* double_sequence = init.value().get_pointer<Vector<double>>(); double_sequence && (double_sequence->size() != 6 && double_sequence->size() != 16))
            return WebIDL::SimpleException { WebIDL::SimpleExceptionType::TypeError, TRY_OR_THROW_OOM(vm, String::formatted("Sequence must contain exactly 6 or 16 elements, got {} element(s)", double_sequence->size())) };
    }

    return realm.heap().allocate<DOMMatrixReadOnly>(realm, realm, init);
}

// https://drafts.fxtf.org/geometry/#create-a-dommatrixreadonly-from-the-2d-dictionary
WebIDL::ExceptionOr<JS::NonnullGCPtr<DOMMatrixReadOnly>> DOMMatrixReadOnly::create_from_dom_matrix_2d_init(JS::Realm& realm, DOMMatrix2DInit& init)
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
    return realm.heap().allocate<DOMMatrixReadOnly>(realm, realm, init.m11.value(), init.m12.value(), init.m21.value(), init.m22.value(), init.m41.value(), init.m42.value());
}

DOMMatrixReadOnly::DOMMatrixReadOnly(JS::Realm& realm, double m11, double m12, double m21, double m22, double m41, double m42)
    : Bindings::PlatformObject(realm)
{
    initialize_from_create_2d_matrix(m11, m12, m21, m22, m41, m42);
}

DOMMatrixReadOnly::DOMMatrixReadOnly(JS::Realm& realm, double m11, double m12, double m13, double m14, double m21, double m22, double m23, double m24, double m31, double m32, double m33, double m34, double m41, double m42, double m43, double m44)
    : Bindings::PlatformObject(realm)
{
    initialize_from_create_3d_matrix(m11, m12, m13, m14, m21, m22, m23, m24, m31, m32, m33, m34, m41, m42, m43, m44);
}

DOMMatrixReadOnly::DOMMatrixReadOnly(JS::Realm& realm, Optional<Variant<String, Vector<double>>> const& init)
    : Bindings::PlatformObject(realm)
{
    // https://drafts.fxtf.org/geometry/#dom-dommatrixreadonly-dommatrixreadonly
    // -> If init is omitted
    if (!init.has_value()) {
        // Return the result of invoking create a 2d matrix of type DOMMatrixReadOnly or DOMMatrix as appropriate, with the sequence [1, 0, 0, 1, 0, 0].
        initialize_from_create_2d_matrix(1, 0, 0, 1, 0, 0);
        return;
    }
    auto const& init_value = init.value();

    // -> If init is a DOMString
    if (init_value.has<String>()) {
        dbgln("FIXME: Implement initializing DOMMatrix(ReadOnly) from DOMString: '{}'", init_value.get<String>());
        // NOTE: This will result in an identity matrix for now.
        return;
    }

    auto const& double_sequence = init_value.get<Vector<double>>();

    // -> If init is a sequence with 6 elements
    if (double_sequence.size() == 6) {
        // Return the result of invoking create a 2d matrix of type DOMMatrixReadOnly or DOMMatrix as appropriate, with the sequence init.
        initialize_from_create_2d_matrix(double_sequence[0], double_sequence[1], double_sequence[2], double_sequence[3], double_sequence[4], double_sequence[5]);
        return;
    }

    // -> If init is a sequence with 16 elements
    // NOTE: The "otherwise" case should be handled in construct_impl, leaving the only other possible condition here to be 16 elements.
    VERIFY(double_sequence.size() == 16);

    // Return the result of invoking create a 3d matrix of type DOMMatrixReadOnly or DOMMatrix as appropriate, with the sequence init.
    initialize_from_create_3d_matrix(
        double_sequence[0], double_sequence[1], double_sequence[2], double_sequence[3],
        double_sequence[4], double_sequence[5], double_sequence[6], double_sequence[7],
        double_sequence[8], double_sequence[9], double_sequence[10], double_sequence[11],
        double_sequence[12], double_sequence[13], double_sequence[14], double_sequence[15]);
}

DOMMatrixReadOnly::DOMMatrixReadOnly(JS::Realm& realm, DOMMatrixReadOnly const& other)
    : Bindings::PlatformObject(realm)
    , m_matrix(other.m_matrix)
    , m_is_2d(other.m_is_2d)
{
}

DOMMatrixReadOnly::~DOMMatrixReadOnly() = default;

void DOMMatrixReadOnly::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    set_prototype(&Bindings::ensure_web_prototype<Bindings::DOMMatrixReadOnlyPrototype>(realm, "DOMMatrixReadOnly"));
}

// https://drafts.fxtf.org/geometry/#create-a-2d-matrix
void DOMMatrixReadOnly::initialize_from_create_2d_matrix(double m11, double m12, double m21, double m22, double m41, double m42)
{
    // NOTE: The matrix used in the spec is column-major (https://drafts.fxtf.org/geometry/#4x4-abstract-matrix) but Gfx::Matrix4x4 is row-major so we need to transpose the values.

    // 1. Let matrix be a new instance of type.
    // 2. Set m11 element, m12 element, m21 element, m22 element, m41 element and m42 element to the values of init in order starting with the first value.
    auto* elements = m_matrix.elements();
    elements[0][0] = m11;
    elements[1][0] = m12;
    elements[0][1] = m21;
    elements[1][1] = m22;
    elements[0][3] = m41;
    elements[1][3] = m42;

    // 3. Set m13 element, m14 element, m23 element, m24 element, m31 element, m32 element, m34 element, and m43 element to 0.
    elements[2][0] = 0.0;
    elements[3][0] = 0.0;
    elements[2][1] = 0.0;
    elements[3][1] = 0.0;
    elements[0][2] = 0.0;
    elements[1][2] = 0.0;
    elements[3][2] = 0.0;
    elements[2][3] = 0.0;

    // 4. Set m33 element and m44 element to 1.
    elements[2][2] = 1.0;
    elements[3][3] = 1.0;

    // 5. Set is 2D to true.
    m_is_2d = true;

    // 6. Return matrix
}

// https://drafts.fxtf.org/geometry/#create-a-3d-matrix
void DOMMatrixReadOnly::initialize_from_create_3d_matrix(double m11, double m12, double m13, double m14, double m21, double m22, double m23, double m24, double m31, double m32, double m33, double m34, double m41, double m42, double m43, double m44)
{
    // NOTE: The matrix used in the spec is column-major (https://drafts.fxtf.org/geometry/#4x4-abstract-matrix) but Gfx::Matrix4x4 is row-major so we need to transpose the values.

    // 1. Let matrix be a new instance of type.
    // 2. Set m11 element to m44 element to the values of init in column-major order.
    auto* elements = m_matrix.elements();
    elements[0][0] = m11;
    elements[1][0] = m12;
    elements[2][0] = m13;
    elements[3][0] = m14;
    elements[0][1] = m21;
    elements[1][1] = m22;
    elements[2][1] = m23;
    elements[3][1] = m24;
    elements[0][2] = m31;
    elements[1][2] = m32;
    elements[2][2] = m33;
    elements[3][2] = m34;
    elements[0][3] = m41;
    elements[1][3] = m42;
    elements[2][3] = m43;
    elements[3][3] = m44;

    // 3. Set is 2D to false.
    m_is_2d = false;

    // 4. Return matrix
}

// https://drafts.fxtf.org/geometry/#dom-dommatrixreadonly-frommatrix
WebIDL::ExceptionOr<JS::NonnullGCPtr<DOMMatrixReadOnly>> DOMMatrixReadOnly::from_matrix(JS::VM& vm, DOMMatrixInit& other)
{
    return create_from_dom_matrix_2d_init(*vm.current_realm(), other);
}

// https://drafts.fxtf.org/geometry/#dom-dommatrixreadonly-isidentity
bool DOMMatrixReadOnly::is_identity() const
{
    // The isIdentity attribute must return true if
    // m12 element, m13 element, m14 element,
    // m21 element, m23 element, m24 element,
    // m31 element, m32 element, m34 element
    // m41 element, m42 element, m43 element
    // are 0 or -0 and
    // m11 element, m22 element, m33 element, m44 element are 1.
    // Otherwise it must return false.
    if (m12() != 0.0 && m12() != -0.0)
        return false;

    if (m13() != 0.0 && m13() != -0.0)
        return false;

    if (m14() != 0.0 && m14() != -0.0)
        return false;

    if (m21() != 0.0 && m21() != -0.0)
        return false;

    if (m23() != 0.0 && m24() != -0.0)
        return false;

    if (m31() != 0.0 && m32() != -0.0)
        return false;

    if (m34() != 0.0 && m34() != -0.0)
        return false;

    if (m41() != 0.0 && m42() != -0.0)
        return false;

    if (m43() != 0.0 && m43() != -0.0)
        return false;

    if (m11() != 1.0)
        return false;

    if (m22() != 1.0)
        return false;

    if (m33() != 1.0)
        return false;

    if (m44() != 1.0)
        return false;

    return true;
}

// https://drafts.fxtf.org/geometry/#dom-dommatrixreadonly-translate
JS::NonnullGCPtr<DOMMatrix> DOMMatrixReadOnly::translate(Optional<double> const& tx, Optional<double> const& ty, Optional<double> const& tz) const
{
    // 1. Let result be the resulting matrix initialized to the values of the current matrix.
    auto result = DOMMatrix::create_from_dom_matrix_read_only(realm(), *this);

    // 2. Perform a translateSelf() transformation on result with the arguments tx, ty, tz.
    // 3. Return result.
    return result->translate_self(tx, ty, tz);
}

// https://drafts.fxtf.org/geometry/#dom-dommatrixreadonly-skewx
JS::NonnullGCPtr<DOMMatrix> DOMMatrixReadOnly::skew_x(double sx) const
{
    // 1. Let result be the resulting matrix initialized to the values of the current matrix.
    auto result = DOMMatrix::create_from_dom_matrix_read_only(realm(), *this);

    // 2. Perform a skewXSelf() transformation on result with the argument sx.
    // 3. Return result.
    return result->skew_x_self(sx);
}

// https://drafts.fxtf.org/geometry/#dom-dommatrixreadonly-skewy
JS::NonnullGCPtr<DOMMatrix> DOMMatrixReadOnly::skew_y(double sy) const
{
    // 1. Let result be the resulting matrix initialized to the values of the current matrix.
    auto result = DOMMatrix::create_from_dom_matrix_read_only(realm(), *this);

    // 2. Perform a skewYSelf() transformation on result with the argument sy.
    // 3. Return result.
    return result->skew_y_self(sy);
}

// https://drafts.fxtf.org/geometry/#dom-dommatrixreadonly-multiply
WebIDL::ExceptionOr<JS::NonnullGCPtr<DOMMatrix>> DOMMatrixReadOnly::multiply(DOMMatrixInit other)
{
    // 1. Let result be the resulting matrix initialized to the values of the current matrix.
    auto result = DOMMatrix::create_from_dom_matrix_read_only(realm(), *this);

    // 2. Perform a multiplySelf() transformation on result with the argument other.
    // 3. Return result.
    return result->multiply_self(other);
}

// https://drafts.fxtf.org/geometry/#dom-dommatrixreadonly-flipx
JS::NonnullGCPtr<DOMMatrix> DOMMatrixReadOnly::flip_x()
{
    // 1. Let result be the resulting matrix initialized to the values of the current matrix.
    auto result = DOMMatrix::create_from_dom_matrix_read_only(realm(), *this);

    // 2. Post-multiply result with new DOMMatrix([-1, 0, 0, 1, 0, 0]).
    // clang-format off
    Gfx::DoubleMatrix4x4 flip_matrix = { -1, 0, 0, 0,
        0, 1, 0, 0,
        0, 0, 1, 0,
        0, 0, 0, 1 };
    // clang-format on
    result->m_matrix = result->m_matrix * flip_matrix;

    // 3. Return result.
    return result;
}

// https://drafts.fxtf.org/geometry/#dom-dommatrixreadonly-flipy
JS::NonnullGCPtr<DOMMatrix> DOMMatrixReadOnly::flip_y()
{
    // 1. Let result be the resulting matrix initialized to the values of the current matrix.
    auto result = DOMMatrix::create_from_dom_matrix_read_only(realm(), *this);

    // 2. Post-multiply result with new DOMMatrix([1, 0, 0, -1, 0, 0]).
    // clang-format off
    Gfx::DoubleMatrix4x4 flip_matrix = { 1, 0, 0, 0,
        0, -1, 0, 0,
        0, 0, 1, 0,
        0, 0, 0, 1 };
    // clang-format on
    result->m_matrix = result->m_matrix * flip_matrix;

    // 3. Return result.
    return result;
}

// https://drafts.fxtf.org/geometry/#dom-dommatrixreadonly-inverse
JS::NonnullGCPtr<DOMMatrix> DOMMatrixReadOnly::inverse() const
{
    // 1. Let result be the resulting matrix initialized to the values of the current matrix.
    auto result = DOMMatrix::create_from_dom_matrix_read_only(realm(), *this);

    // 2. Perform a invertSelf() transformation on result.
    // 3. Return result.
    // The current matrix is not modified.
    return result->invert_self();
}

// https://drafts.fxtf.org/geometry/#dom-dommatrixreadonly-transformpoint
JS::NonnullGCPtr<DOMPoint> DOMMatrixReadOnly::transform_point(DOMPointInit const& point) const
{
    // Let pointObject be the result of invoking create a DOMPoint from the dictionary point.
    auto point_object = DOMPoint::from_point(realm().vm(), point);

    // Return the result of invoking transform a point with a matrix, given pointObject and the current matrix. The passed argument does not get modified.
    return transform_point(point_object);
}

// https://drafts.fxtf.org/geometry/#transform-a-point-with-a-matrix
JS::NonnullGCPtr<DOMPoint> DOMMatrixReadOnly::transform_point(DOMPointReadOnly const& point) const
{
    // 1. Let x be point’s x coordinate.
    // 2. Let y be point’s y coordinate.
    // 3. Let z be point’s z coordinate.
    // 4. Let w be point’s w perspective.
    // 5. Let pointVector be a new column vector with the elements being x, y, z, and w, respectively.
    Vector4<double> point_vector { point.x(), point.y(), point.z(), point.w() };

    // 6. Set pointVector to pointVector pre-multiplied by matrix.
    // This is really a post multiply because of the transposed m_matrix.
    point_vector = m_matrix * point_vector;

    // 7. Let transformedPoint be a new DOMPoint object.
    // 8. Set transformedPoint’s x coordinate to pointVector’s first element.
    // 9. Set transformedPoint’s y coordinate to pointVector’s second element.
    // 10. Set transformedPoint’s z coordinate to pointVector’s third element.
    // 11. Set transformedPoint’s w perspective to pointVector’s fourth element.
    // 12. Return transformedPoint.
    return DOMPoint::construct_impl(realm(), point_vector.x(), point_vector.y(), point_vector.z(), point_vector.w());
}

// https://drafts.fxtf.org/geometry/#dommatrixreadonly-stringification-behavior
WebIDL::ExceptionOr<String> DOMMatrixReadOnly::to_string() const
{
    auto& vm = this->vm();

    // 1. If one or more of m11 element through m44 element are a non-finite value, then throw an "InvalidStateError" DOMException.
    // Spec Note: The CSS syntax cannot represent NaN or Infinity values.
    if (!isfinite(m11()) || !isfinite(m12()) || !isfinite(m13()) || !isfinite(m14())
        || !isfinite(m21()) || !isfinite(m22()) || !isfinite(m23()) || !isfinite(m24())
        || !isfinite(m31()) || !isfinite(m32()) || !isfinite(m33()) || !isfinite(m34())
        || !isfinite(m41()) || !isfinite(m42()) || !isfinite(m43()) || !isfinite(m44())) {
        return WebIDL::InvalidStateError::create(realm(), "Cannot stringify non-finite matrix values"_fly_string);
    }

    // 2. Let string be the empty string.
    StringBuilder builder;

    // 3. If is 2D is true, then:
    if (m_is_2d) {
        // 1. Append "matrix(" to string.
        TRY_OR_THROW_OOM(vm, builder.try_append("matrix("sv));

        // 2. Append ! ToString(m11 element) to string.
        TRY_OR_THROW_OOM(vm, builder.try_append(JS::Value(m11()).to_string_without_side_effects()));

        // 3. Append ", " to string.
        TRY_OR_THROW_OOM(vm, builder.try_append(", "sv));

        // 4. Append ! ToString(m12 element) to string.
        TRY_OR_THROW_OOM(vm, builder.try_append(JS::Value(m12()).to_string_without_side_effects()));

        // 5. Append ", " to string.
        TRY_OR_THROW_OOM(vm, builder.try_append(", "sv));

        // 6. Append ! ToString(m21 element) to string.
        TRY_OR_THROW_OOM(vm, builder.try_append(JS::Value(m21()).to_string_without_side_effects()));

        // 7. Append ", " to string.
        TRY_OR_THROW_OOM(vm, builder.try_append(", "sv));

        // 8. Append ! ToString(m22 element) to string.
        TRY_OR_THROW_OOM(vm, builder.try_append(JS::Value(m22()).to_string_without_side_effects()));

        // 9. Append ", " to string.
        TRY_OR_THROW_OOM(vm, builder.try_append(", "sv));

        // 10. Append ! ToString(m41 element) to string.
        TRY_OR_THROW_OOM(vm, builder.try_append(JS::Value(m41()).to_string_without_side_effects()));

        // 11. Append ", " to string.
        TRY_OR_THROW_OOM(vm, builder.try_append(", "sv));

        // 12. Append ! ToString(m42 element) to string.
        TRY_OR_THROW_OOM(vm, builder.try_append(JS::Value(m42()).to_string_without_side_effects()));

        // 13. Append ")" to string.
        TRY_OR_THROW_OOM(vm, builder.try_append(")"sv));
    } else {
        // 1. Append "matrix3d(" to string.
        TRY_OR_THROW_OOM(vm, builder.try_append("matrix3d("sv));

        // 2. Append ! ToString(m11 element) to string.
        TRY_OR_THROW_OOM(vm, builder.try_append(JS::number_to_string(m11())));

        // 3. Append ", " to string.
        TRY_OR_THROW_OOM(vm, builder.try_append(", "sv));

        // 4. Append ! ToString(m12 element) to string.
        TRY_OR_THROW_OOM(vm, builder.try_append(JS::number_to_string(m12())));

        // 5. Append ", " to string.
        TRY_OR_THROW_OOM(vm, builder.try_append(", "sv));

        // 6. Append ! ToString(m13 element) to string.
        TRY_OR_THROW_OOM(vm, builder.try_append(JS::number_to_string(m13())));

        // 7. Append ", " to string.
        TRY_OR_THROW_OOM(vm, builder.try_append(", "sv));

        // 8. Append ! ToString(m14 element) to string.
        TRY_OR_THROW_OOM(vm, builder.try_append(JS::number_to_string(m14())));

        // 9. Append ", " to string.
        TRY_OR_THROW_OOM(vm, builder.try_append(", "sv));

        // 10. Append ! ToString(m21 element) to string.
        TRY_OR_THROW_OOM(vm, builder.try_append(JS::number_to_string(m21())));

        // 11. Append ", " to string.
        TRY_OR_THROW_OOM(vm, builder.try_append(", "sv));

        // 12. Append ! ToString(m22 element) to string.
        TRY_OR_THROW_OOM(vm, builder.try_append(JS::number_to_string(m22())));

        // 13. Append ", " to string.
        TRY_OR_THROW_OOM(vm, builder.try_append(", "sv));

        // 14. Append ! ToString(m23 element) to string.
        TRY_OR_THROW_OOM(vm, builder.try_append(JS::number_to_string(m23())));

        // 15. Append ", " to string.
        TRY_OR_THROW_OOM(vm, builder.try_append(", "sv));

        // 16. Append ! ToString(m24 element) to string.
        TRY_OR_THROW_OOM(vm, builder.try_append(JS::number_to_string(m24())));

        // 17. Append ", " to string.
        TRY_OR_THROW_OOM(vm, builder.try_append(", "sv));

        // NOTE: The spec doesn't include the steps to append m31 to m34, but they are required as matrix3d requires 16 elements.
        TRY_OR_THROW_OOM(vm, builder.try_append(JS::number_to_string(m31())));
        TRY_OR_THROW_OOM(vm, builder.try_append(", "sv));
        TRY_OR_THROW_OOM(vm, builder.try_append(JS::number_to_string(m32())));
        TRY_OR_THROW_OOM(vm, builder.try_append(", "sv));
        TRY_OR_THROW_OOM(vm, builder.try_append(JS::number_to_string(m33())));
        TRY_OR_THROW_OOM(vm, builder.try_append(", "sv));
        TRY_OR_THROW_OOM(vm, builder.try_append(JS::number_to_string(m34())));
        TRY_OR_THROW_OOM(vm, builder.try_append(", "sv));

        // 18. Append ! ToString(m41 element) to string.
        TRY_OR_THROW_OOM(vm, builder.try_append(JS::number_to_string(m41())));

        // 19. Append ", " to string.
        TRY_OR_THROW_OOM(vm, builder.try_append(", "sv));

        // 20. Append ! ToString(m42 element) to string.
        TRY_OR_THROW_OOM(vm, builder.try_append(JS::number_to_string(m42())));

        // 21. Append ", " to string.
        TRY_OR_THROW_OOM(vm, builder.try_append(", "sv));

        // 22. Append ! ToString(m43 element) to string.
        TRY_OR_THROW_OOM(vm, builder.try_append(JS::number_to_string(m43())));

        // 23. Append ", " to string.
        TRY_OR_THROW_OOM(vm, builder.try_append(", "sv));

        // 24. Append ! ToString(m44 element) to string.
        TRY_OR_THROW_OOM(vm, builder.try_append(JS::number_to_string(m44())));

        // 25. Append ")" to string.
        TRY_OR_THROW_OOM(vm, builder.try_append(")"sv));
    }

    // 5. Return string.
    return TRY_OR_THROW_OOM(vm, builder.to_string());
}

// https://drafts.fxtf.org/geometry/#matrix-validate-and-fixup-2d
WebIDL::ExceptionOr<void> validate_and_fixup_dom_matrix_2d_init(DOMMatrix2DInit& init)
{
    // 1. If at least one of the following conditions are true for dict, then throw a TypeError exception and abort these steps.
    // - a and m11 are both present and SameValueZero(a, m11) is false.
    if (init.a.has_value() && init.m11.has_value() && !JS::same_value_zero(JS::Value(init.a.value()), JS::Value(init.m11.value())))
        return WebIDL::SimpleException { WebIDL::SimpleExceptionType::TypeError, "DOMMatrix2DInit.a and DOMMatrix2DInit.m11 must have the same value if they are both present"sv };

    // - b and m12 are both present and SameValueZero(b, m12) is false.
    if (init.b.has_value() && init.m12.has_value() && !JS::same_value_zero(JS::Value(init.b.value()), JS::Value(init.m12.value())))
        return WebIDL::SimpleException { WebIDL::SimpleExceptionType::TypeError, "DOMMatrix2DInit.b and DOMMatrix2DInit.m12 must have the same value if they are both present"sv };

    // - c and m21 are both present and SameValueZero(c, m21) is false.
    if (init.c.has_value() && init.m21.has_value() && !JS::same_value_zero(JS::Value(init.c.value()), JS::Value(init.m21.value())))
        return WebIDL::SimpleException { WebIDL::SimpleExceptionType::TypeError, "DOMMatrix2DInit.c and DOMMatrix2DInit.m21 must have the same value if they are both present"sv };

    // - d and m22 are both present and SameValueZero(d, m22) is false.
    if (init.d.has_value() && init.m22.has_value() && !JS::same_value_zero(JS::Value(init.d.value()), JS::Value(init.m22.value())))
        return WebIDL::SimpleException { WebIDL::SimpleExceptionType::TypeError, "DOMMatrix2DInit.d and DOMMatrix2DInit.m22 must have the same value if they are both present"sv };

    // - e and m41 are both present and SameValueZero(e, m41) is false.
    if (init.e.has_value() && init.m41.has_value() && !JS::same_value_zero(JS::Value(init.e.value()), JS::Value(init.m41.value())))
        return WebIDL::SimpleException { WebIDL::SimpleExceptionType::TypeError, "DOMMatrix2DInit.e and DOMMatrix2DInit.m41 must have the same value if they are both present"sv };

    // - f and m42 are both present and SameValueZero(f, m42) is false.
    if (init.f.has_value() && init.m42.has_value() && !JS::same_value_zero(JS::Value(init.f.value()), JS::Value(init.m42.value())))
        return WebIDL::SimpleException { WebIDL::SimpleExceptionType::TypeError, "DOMMatrix2DInit.f and DOMMatrix2DInit.m42 must have the same value if they are both present"sv };

    // 2. If m11 is not present then set it to the value of member a, or value 1 if a is also not present.
    if (!init.m11.has_value())
        init.m11 = init.a.value_or(1.0);

    // 3. If m12 is not present then set it to the value of member b, or value 0 if b is also not present.
    if (!init.m12.has_value())
        init.m12 = init.b.value_or(0.0);

    // 4. If m21 is not present then set it to the value of member c, or value 0 if c is also not present.
    if (!init.m21.has_value())
        init.m21 = init.c.value_or(0.0);

    // 5. If m22 is not present then set it to the value of member d, or value 1 if d is also not present.
    if (!init.m22.has_value())
        init.m22 = init.d.value_or(1.0);

    // 6. If m41 is not present then set it to the value of member e, or value 0 if e is also not present.
    if (!init.m41.has_value())
        init.m41 = init.e.value_or(0.0);

    // 7. If m42 is not present then set it to the value of member f, or value 0 if f is also not present.
    if (!init.m42.has_value())
        init.m42 = init.f.value_or(0.0);

    return {};
}

// https://drafts.fxtf.org/geometry/#matrix-validate-and-fixup
WebIDL::ExceptionOr<void> validate_and_fixup_dom_matrix_init(DOMMatrixInit& init)
{
    // 1. Validate and fixup (2D) dict.
    TRY(validate_and_fixup_dom_matrix_2d_init(init));

    // 2. If is2D is true and: at least one of m13, m14, m23, m24, m31, m32, m34, m43 are present with a value other than 0 or -0,
    //    or at least one of m33, m44 are present with a value other than 1, then throw a TypeError exception and abort these steps.
    if (init.is2d.has_value() && init.is2d.value()) {
        if ((init.m13 != 0.0 && init.m13 != -0.0)
            || (init.m14 != 0.0 && init.m14 != -0.0)
            || (init.m23 != 0.0 && init.m23 != -0.0)
            || (init.m24 != 0.0 && init.m24 != -0.0)
            || (init.m31 != 0.0 && init.m31 != -0.0)
            || (init.m32 != 0.0 && init.m32 != -0.0)
            || (init.m34 != 0.0 && init.m34 != -0.0)
            || (init.m43 != 0.0 && init.m43 != -0.0)
            || init.m33 != 1.0
            || init.m44 != 1.0) {
            return WebIDL::SimpleException { WebIDL::SimpleExceptionType::TypeError, "DOMMatrixInit.is2D is true, but the given matrix is not a 2D matrix"sv };
        }
    }

    // If is2D is not present and at least one of m13, m14, m23, m24, m31, m32, m34, m43 are present with a value other than 0 or -0,
    // or at least one of m33, m44 are present with a value other than 1, set is2D to false.
    if (!init.is2d.has_value()) {
        if ((init.m13 != 0.0 && init.m13 != -0.0)
            || (init.m14 != 0.0 && init.m14 != -0.0)
            || (init.m23 != 0.0 && init.m23 != -0.0)
            || (init.m24 != 0.0 && init.m24 != -0.0)
            || (init.m31 != 0.0 && init.m31 != -0.0)
            || (init.m32 != 0.0 && init.m32 != -0.0)
            || (init.m34 != 0.0 && init.m34 != -0.0)
            || (init.m43 != 0.0 && init.m43 != -0.0)
            || init.m33 != 1.0
            || init.m44 != 1.0) {
            init.is2d = false;
        }
    }

    // 4. If is2D is still not present, set it to true.
    if (!init.is2d.has_value())
        init.is2d = true;

    return {};
}

}
