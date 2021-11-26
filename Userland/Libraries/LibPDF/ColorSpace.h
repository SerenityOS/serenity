/*
 * Copyright (c) 2021, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/FlyString.h>
#include <LibGfx/Color.h>
#include <LibPDF/Value.h>

#define ENUMERATE_COLOR_SPACES(V) \
    V(DeviceGray)                 \
    V(DeviceRGB)                  \
    V(DeviceCMYK)                 \
    V(CalGray)                    \
    V(CalRGB)                     \
    V(Lab)                        \
    V(ICCBased)                   \
    V(Indexed)                    \
    V(Pattern)                    \
    V(Separation)                 \
    V(DeviceN)

namespace PDF {

class ColorSpace : public RefCounted<ColorSpace> {
public:
    virtual ~ColorSpace() = default;

    virtual Color color(Vector<Value> const& arguments) const = 0;
};

class DeviceGrayColorSpace final : public ColorSpace {
public:
    static RefPtr<DeviceGrayColorSpace> the();

    virtual ~DeviceGrayColorSpace() override = default;

    virtual Color color(Vector<Value> const& arguments) const override;

private:
    DeviceGrayColorSpace() = default;
};

class DeviceRGBColorSpace final : public ColorSpace {
public:
    static RefPtr<DeviceRGBColorSpace> the();

    virtual ~DeviceRGBColorSpace() override = default;

    virtual Color color(Vector<Value> const& arguments) const override;

private:
    DeviceRGBColorSpace() = default;
};

class DeviceCMYKColorSpace final : public ColorSpace {
public:
    static RefPtr<DeviceCMYKColorSpace> the();

    virtual ~DeviceCMYKColorSpace() override = default;

    virtual Color color(Vector<Value> const& arguments) const override;

private:
    DeviceCMYKColorSpace() = default;
};

class CalRGBColorSpace final : public ColorSpace {
public:
    static RefPtr<CalRGBColorSpace> create(RefPtr<Document>, Vector<Value>&& parameters);
    virtual ~CalRGBColorSpace() override = default;

    virtual Color color(Vector<Value> const& arguments) const override;

private:
    CalRGBColorSpace() = default;

    Array<float, 3> m_whitepoint { 0, 0, 0 };
    Array<float, 3> m_blackpoint { 0, 0, 0 };
    Array<float, 3> m_gamma { 1, 1, 1 };
    Array<float, 9> m_matrix { 1, 0, 0, 0, 1, 0, 0, 0, 1 };
};

}
