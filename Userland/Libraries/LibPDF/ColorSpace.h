/*
 * Copyright (c) 2021-2022, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/DeprecatedFlyString.h>
#include <AK/Forward.h>
#include <LibGfx/Color.h>
#include <LibGfx/ICC/Profile.h>
#include <LibGfx/PaintStyle.h>
#include <LibPDF/Function.h>
#include <LibPDF/Value.h>

#define ENUMERATE_COLOR_SPACE_FAMILIES(V) \
    V(DeviceGray, true)                   \
    V(DeviceRGB, true)                    \
    V(DeviceCMYK, true)                   \
    V(CalGray, false)                     \
    V(CalRGB, false)                      \
    V(Lab, false)                         \
    V(ICCBased, false)                    \
    V(Indexed, false)                     \
    V(Pattern, true)                      \
    V(Separation, false)                  \
    V(DeviceN, false)

namespace PDF {

typedef Variant<Gfx::Color, NonnullRefPtr<Gfx::PaintStyle>> ColorOrStyle;
class Renderer;

class ColorSpaceFamily {
public:
    ColorSpaceFamily(DeprecatedFlyString name, bool may_be_specified_directly)
        : m_name(move(name))
        , m_may_be_specified_directly(may_be_specified_directly)
    {
    }

    DeprecatedFlyString name() const { return m_name; }
    bool may_be_specified_directly() const { return m_may_be_specified_directly; }
    static PDFErrorOr<ColorSpaceFamily> get(DeprecatedFlyString const&);

#define ENUMERATE(name, may_be_specified_directly) static ColorSpaceFamily name;
    ENUMERATE_COLOR_SPACE_FAMILIES(ENUMERATE)
#undef ENUMERATE

    bool operator==(ColorSpaceFamily const& other) const
    {
        return m_name == other.m_name;
    }

private:
    DeprecatedFlyString m_name;
    bool m_may_be_specified_directly;
};

class ColorSpace : public RefCounted<ColorSpace> {
public:
    static PDFErrorOr<NonnullRefPtr<ColorSpace>> create(Document*, NonnullRefPtr<Object>, Renderer&);
    static PDFErrorOr<NonnullRefPtr<ColorSpace>> create(DeprecatedFlyString const&, Renderer&);
    static PDFErrorOr<NonnullRefPtr<ColorSpace>> create(Document*, NonnullRefPtr<ArrayObject>, Renderer&);

    virtual ~ColorSpace() = default;

    virtual PDFErrorOr<ColorOrStyle> style(ReadonlySpan<float> arguments) const = 0;
    virtual PDFErrorOr<ColorOrStyle> style(ReadonlySpan<Value> arguments) const
    {
        Vector<float, 4> float_arguments;
        for (auto& argument : arguments)
            float_arguments.append(argument.to_float());
        return style(float_arguments);
    }

    virtual int number_of_components() const = 0;
    virtual Vector<float> default_decode() const = 0; // "TABLE 4.40 Default Decode arrays"
    virtual ColorSpaceFamily const& family() const = 0;
};

class DeviceGrayColorSpace final : public ColorSpace {
public:
    static NonnullRefPtr<DeviceGrayColorSpace> the();

    ~DeviceGrayColorSpace() override = default;

    PDFErrorOr<ColorOrStyle> style(ReadonlySpan<float> arguments) const override;
    int number_of_components() const override { return 1; }
    Vector<float> default_decode() const override;
    ColorSpaceFamily const& family() const override { return ColorSpaceFamily::DeviceGray; }

private:
    DeviceGrayColorSpace() = default;
};

class DeviceRGBColorSpace final : public ColorSpace {
public:
    static NonnullRefPtr<DeviceRGBColorSpace> the();

    ~DeviceRGBColorSpace() override = default;

    PDFErrorOr<ColorOrStyle> style(ReadonlySpan<float> arguments) const override;
    int number_of_components() const override { return 3; }
    Vector<float> default_decode() const override;
    ColorSpaceFamily const& family() const override { return ColorSpaceFamily::DeviceRGB; }

private:
    DeviceRGBColorSpace() = default;
};

class DeviceCMYKColorSpace final : public ColorSpace {
public:
    static ErrorOr<NonnullRefPtr<DeviceCMYKColorSpace>> the();

    ~DeviceCMYKColorSpace() override = default;

    PDFErrorOr<ColorOrStyle> style(ReadonlySpan<float> arguments) const override;
    int number_of_components() const override { return 4; }
    Vector<float> default_decode() const override;
    ColorSpaceFamily const& family() const override { return ColorSpaceFamily::DeviceCMYK; }

private:
    DeviceCMYKColorSpace() = default;
};

class DeviceNColorSpace final : public ColorSpace {
public:
    static PDFErrorOr<NonnullRefPtr<DeviceNColorSpace>> create(Document*, Vector<Value>&& parameters, Renderer&);

    ~DeviceNColorSpace() override = default;

    PDFErrorOr<ColorOrStyle> style(ReadonlySpan<float> arguments) const override;
    int number_of_components() const override;
    Vector<float> default_decode() const override;
    ColorSpaceFamily const& family() const override { return ColorSpaceFamily::DeviceN; }

private:
    DeviceNColorSpace(NonnullRefPtr<ColorSpace>, NonnullRefPtr<Function>);

    Vector<ByteString> m_names;
    NonnullRefPtr<ColorSpace> m_alternate_space;
    NonnullRefPtr<Function> m_tint_transform;
    Vector<Value> mutable m_tint_output_values;
};

class CalGrayColorSpace final : public ColorSpace {
public:
    static PDFErrorOr<NonnullRefPtr<CalGrayColorSpace>> create(Document*, Vector<Value>&& parameters);

    ~CalGrayColorSpace() override = default;

    PDFErrorOr<ColorOrStyle> style(ReadonlySpan<float> arguments) const override;
    int number_of_components() const override { return 1; }
    Vector<float> default_decode() const override;
    ColorSpaceFamily const& family() const override { return ColorSpaceFamily::CalGray; }

private:
    CalGrayColorSpace() = default;

    Array<float, 3> m_whitepoint { 0, 0, 0 };
    Array<float, 3> m_blackpoint { 0, 0, 0 };
    float m_gamma { 1 };
};

class CalRGBColorSpace final : public ColorSpace {
public:
    static PDFErrorOr<NonnullRefPtr<CalRGBColorSpace>> create(Document*, Vector<Value>&& parameters);

    ~CalRGBColorSpace() override = default;

    PDFErrorOr<ColorOrStyle> style(ReadonlySpan<float> arguments) const override;
    int number_of_components() const override { return 3; }
    Vector<float> default_decode() const override;
    ColorSpaceFamily const& family() const override { return ColorSpaceFamily::CalRGB; }

private:
    CalRGBColorSpace() = default;

    Array<float, 3> m_whitepoint { 0, 0, 0 };
    Array<float, 3> m_blackpoint { 0, 0, 0 };
    Array<float, 3> m_gamma { 1, 1, 1 };
    Array<float, 9> m_matrix { 1, 0, 0, 0, 1, 0, 0, 0, 1 };
};

class ICCBasedColorSpace final : public ColorSpace {
public:
    static PDFErrorOr<NonnullRefPtr<ColorSpace>> create(Document*, Vector<Value>&& parameters, Renderer&);

    ~ICCBasedColorSpace() override = default;

    PDFErrorOr<ColorOrStyle> style(ReadonlySpan<float> arguments) const override;
    int number_of_components() const override;
    Vector<float> default_decode() const override;
    ColorSpaceFamily const& family() const override { return ColorSpaceFamily::ICCBased; }

    static NonnullRefPtr<Gfx::ICC::Profile> sRGB();

private:
    ICCBasedColorSpace(NonnullRefPtr<Gfx::ICC::Profile>);

    static RefPtr<Gfx::ICC::Profile> s_srgb_profile;
    NonnullRefPtr<Gfx::ICC::Profile> m_profile;
    mutable Vector<float, 4> m_components;
    mutable Vector<u8, 4> m_bytes;
    Optional<Gfx::ICC::MatrixMatrixConversion> m_map;
};

class LabColorSpace final : public ColorSpace {
public:
    static PDFErrorOr<NonnullRefPtr<LabColorSpace>> create(Document*, Vector<Value>&& parameters);

    ~LabColorSpace() override = default;

    PDFErrorOr<ColorOrStyle> style(ReadonlySpan<float> arguments) const override;
    int number_of_components() const override { return 3; }
    Vector<float> default_decode() const override;
    ColorSpaceFamily const& family() const override { return ColorSpaceFamily::Lab; }

private:
    LabColorSpace() = default;

    Array<float, 3> m_whitepoint { 0, 0, 0 };
    Array<float, 3> m_blackpoint { 0, 0, 0 };
    Array<float, 4> m_range { -100, 100, -100, 100 };
};

class IndexedColorSpace final : public ColorSpace {
public:
    static PDFErrorOr<NonnullRefPtr<ColorSpace>> create(Document*, Vector<Value>&& parameters, Renderer&);

    ~IndexedColorSpace() override = default;

    PDFErrorOr<ColorOrStyle> style(ReadonlySpan<float> arguments) const override;
    int number_of_components() const override { return 1; }
    Vector<float> default_decode() const override;
    ColorSpaceFamily const& family() const override { return ColorSpaceFamily::Indexed; }

private:
    IndexedColorSpace(NonnullRefPtr<ColorSpace>);

    NonnullRefPtr<ColorSpace> m_base;
    int m_hival { 0 };
    Vector<u8> m_lookup;
};

class SeparationColorSpace final : public ColorSpace {
public:
    static PDFErrorOr<NonnullRefPtr<SeparationColorSpace>> create(Document*, Vector<Value>&& parameters, Renderer&);

    ~SeparationColorSpace() override = default;

    PDFErrorOr<ColorOrStyle> style(ReadonlySpan<float> arguments) const override;
    int number_of_components() const override { return 1; }
    Vector<float> default_decode() const override;
    ColorSpaceFamily const& family() const override { return ColorSpaceFamily::Separation; }

private:
    SeparationColorSpace(NonnullRefPtr<ColorSpace>, NonnullRefPtr<Function>);

    ByteString m_name;
    NonnullRefPtr<ColorSpace> m_alternate_space;
    NonnullRefPtr<Function> m_tint_transform;
    Vector<Value> mutable m_tint_output_values;
};
}
