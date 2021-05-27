/*
 * Copyright (c) 2021, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/FlyString.h>
#include <LibGfx/Color.h>
#include <LibPDF/Object.h>
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

    virtual Color color(const Vector<Value>& arguments) const = 0;
};

class DeviceGrayColorSpace final : public ColorSpace {
public:
    static RefPtr<DeviceGrayColorSpace> the();

    virtual ~DeviceGrayColorSpace() override = default;

    virtual Color color(const Vector<Value>& arguments) const override;

private:
    DeviceGrayColorSpace() = default;
};

class DeviceRGBColorSpace final : public ColorSpace {
public:
    static RefPtr<DeviceRGBColorSpace> the();

    virtual ~DeviceRGBColorSpace() override = default;

    virtual Color color(const Vector<Value>& arguments) const override;

private:
    DeviceRGBColorSpace() = default;
};

class DeviceCMYKColorSpace final : public ColorSpace {
public:
    static RefPtr<DeviceCMYKColorSpace> the();

    virtual ~DeviceCMYKColorSpace() override = default;

    virtual Color color(const Vector<Value>& arguments) const override;

private:
    DeviceCMYKColorSpace() = default;
};

}
