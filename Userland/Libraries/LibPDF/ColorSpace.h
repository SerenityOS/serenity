/*
 * Copyright (c) 2021-2022, Matthew Olsson <mattco@serenityos.org>
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

struct Page;

class ColorSpace : public RefCounted<ColorSpace> {
public:
    static PDFErrorOr<NonnullRefPtr<ColorSpace>> create(Document*, FlyString const& name, Page const& page);

    virtual ~ColorSpace() = default;

    virtual Color color(Vector<Value> const& arguments) const = 0;
};

class DeviceGrayColorSpace final : public ColorSpace {
public:
    static NonnullRefPtr<DeviceGrayColorSpace> the();

    ~DeviceGrayColorSpace() override = default;

    Color color(Vector<Value> const& arguments) const override;

private:
    DeviceGrayColorSpace() = default;
};

class DeviceRGBColorSpace final : public ColorSpace {
public:
    static NonnullRefPtr<DeviceRGBColorSpace> the();

    ~DeviceRGBColorSpace() override = default;

    Color color(Vector<Value> const& arguments) const override;

private:
    DeviceRGBColorSpace() = default;
};

class DeviceCMYKColorSpace final : public ColorSpace {
public:
    static NonnullRefPtr<DeviceCMYKColorSpace> the();

    ~DeviceCMYKColorSpace() override = default;

    Color color(Vector<Value> const& arguments) const override;

private:
    DeviceCMYKColorSpace() = default;
};

class CalRGBColorSpace final : public ColorSpace {
public:
    static PDFErrorOr<NonnullRefPtr<CalRGBColorSpace>> create(Document*, Vector<Value>&& parameters);

    ~CalRGBColorSpace() override = default;

    Color color(Vector<Value> const& arguments) const override;

private:
    CalRGBColorSpace() = default;

    Array<float, 3> m_whitepoint { 0, 0, 0 };
    Array<float, 3> m_blackpoint { 0, 0, 0 };
    Array<float, 3> m_gamma { 1, 1, 1 };
    Array<float, 9> m_matrix { 1, 0, 0, 0, 1, 0, 0, 0, 1 };
};

class ICCBasedColorSpace final : public ColorSpace {
public:
    static PDFErrorOr<NonnullRefPtr<ColorSpace>> create(Document*, Page const&, Vector<Value>&& parameters);

    ~ICCBasedColorSpace() override = default;

    Color color(Vector<Value> const& arguments) const override;

private:
    ICCBasedColorSpace() = delete;
};

}
