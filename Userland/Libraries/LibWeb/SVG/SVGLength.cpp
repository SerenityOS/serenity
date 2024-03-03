/*
 * Copyright (c) 2022, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/CSS/PercentageOr.h>
#include <LibWeb/SVG/SVGLength.h>

namespace Web::SVG {

JS_DEFINE_ALLOCATOR(SVGLength);

// Same as SVGLength.idl
constexpr unsigned short SVG_LENGTHTYPE_UNKNOWN = 0;
constexpr unsigned short SVG_LENGTHTYPE_NUMBER = 1;
constexpr unsigned short SVG_LENGTHTYPE_PERCENTAGE = 2;
constexpr unsigned short SVG_LENGTHTYPE_EMS = 3;
constexpr unsigned short SVG_LENGTHTYPE_EXS = 4;
constexpr unsigned short SVG_LENGTHTYPE_PX = 5;
constexpr unsigned short SVG_LENGTHTYPE_CM = 6;
constexpr unsigned short SVG_LENGTHTYPE_MM = 7;
constexpr unsigned short SVG_LENGTHTYPE_IN = 8;
constexpr unsigned short SVG_LENGTHTYPE_PT = 9;
constexpr unsigned short SVG_LENGTHTYPE_PC = 10;

JS::NonnullGCPtr<SVGLength> SVGLength::create(JS::Realm& realm, u8 unit_type, float value)
{
    return realm.heap().allocate<SVGLength>(realm, realm, unit_type, value);
}

JS::NonnullGCPtr<SVGLength> SVGLength::from_length_percentage(JS::Realm& realm, CSS::LengthPercentage const& length_percentage)
{
    // FIXME: We can't tell if a CSS::LengthPercentage was a unitless length.
    (void)SVG_LENGTHTYPE_NUMBER;
    if (length_percentage.is_percentage())
        return SVGLength::create(realm, SVG_LENGTHTYPE_PERCENTAGE, length_percentage.percentage().value());
    if (length_percentage.is_length())
        return SVGLength::create(
            realm, [&] {
                switch (length_percentage.length().type()) {
                case CSS::Length::Type::Em:
                    return SVG_LENGTHTYPE_EMS;
                case CSS::Length::Type::Ex:
                    return SVG_LENGTHTYPE_EXS;
                case CSS::Length::Type::Px:
                    return SVG_LENGTHTYPE_PX;
                case CSS::Length::Type::Cm:
                    return SVG_LENGTHTYPE_CM;
                case CSS::Length::Type::Mm:
                    return SVG_LENGTHTYPE_MM;
                case CSS::Length::Type::In:
                    return SVG_LENGTHTYPE_IN;
                case CSS::Length::Type::Pt:
                    return SVG_LENGTHTYPE_PT;
                case CSS::Length::Type::Pc:
                    return SVG_LENGTHTYPE_PC;
                default:
                    return SVG_LENGTHTYPE_UNKNOWN;
                }
            }(),
            length_percentage.length().raw_value());
    return SVGLength::create(realm, SVG_LENGTHTYPE_UNKNOWN, 0);
}

SVGLength::SVGLength(JS::Realm& realm, u8 unit_type, float value)
    : PlatformObject(realm)
    , m_unit_type(unit_type)
    , m_value(value)
{
}

void SVGLength::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    set_prototype(&Bindings::ensure_web_prototype<Bindings::SVGLengthPrototype>(realm, "SVGLength"_fly_string));
}

SVGLength::~SVGLength() = default;

// https://www.w3.org/TR/SVG11/types.html#__svg__SVGLength__value
WebIDL::ExceptionOr<void> SVGLength::set_value(float value)
{
    // FIXME: Raise an exception if this <length> is read-only.
    m_value = value;
    return {};
}

}
