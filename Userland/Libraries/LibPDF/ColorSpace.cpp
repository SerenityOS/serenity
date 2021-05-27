/*
 * Copyright (c) 2021, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibPDF/ColorSpace.h>
#include <LibPDF/CommonNames.h>

namespace PDF {

RefPtr<DeviceGrayColorSpace> DeviceGrayColorSpace::the()
{
    static auto instance = adopt_ref(*new DeviceGrayColorSpace());
    return instance;
}

Color DeviceGrayColorSpace::color(const Vector<Value>& arguments) const
{
    VERIFY(arguments.size() == 1);
    auto gray = static_cast<u8>(arguments[0].to_float() * 255.0f);
    return Color(gray, gray, gray);
}

RefPtr<DeviceRGBColorSpace> DeviceRGBColorSpace::the()
{
    static auto instance = adopt_ref(*new DeviceRGBColorSpace());
    return instance;
}

Color DeviceRGBColorSpace::color(const Vector<Value>& arguments) const
{
    VERIFY(arguments.size() == 3);
    auto r = static_cast<u8>(arguments[0].to_float() * 255.0f);
    auto g = static_cast<u8>(arguments[1].to_float() * 255.0f);
    auto b = static_cast<u8>(arguments[2].to_float() * 255.0f);
    return Color(r, g, b);
}

RefPtr<DeviceCMYKColorSpace> DeviceCMYKColorSpace::the()
{
    static auto instance = adopt_ref(*new DeviceCMYKColorSpace());
    return instance;
}

Color DeviceCMYKColorSpace::color(const Vector<Value>& arguments) const
{
    VERIFY(arguments.size() == 4);
    auto c = arguments[0].to_float();
    auto m = arguments[1].to_float();
    auto y = arguments[2].to_float();
    auto k = arguments[3].to_float();
    return Color::from_cmyk(c, m, y, k);
}

}
