/*
 * Copyright (c) 2022, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/OwnPtr.h>
#include <LibGfx/Point.h>
#include <LibPDF/Fonts/PDFFont.h>

namespace PDF {

class CIDFontType;

struct CIDSystemInfo {
    ByteString registry;
    ByteString ordering;
    u8 supplement;
};

class CIDIterator {
public:
    virtual ~CIDIterator() = default;
    virtual bool has_next() const = 0;
    virtual u32 next() = 0;
};

class Type0CMap {
public:
    virtual ~Type0CMap() = default;
    virtual PDFErrorOr<NonnullOwnPtr<CIDIterator>> iterate(ReadonlyBytes) const = 0;
};

class Type0Font : public PDFFont {
public:
    Type0Font();
    ~Type0Font();

    void set_font_size(float font_size) override;
    PDFErrorOr<Gfx::FloatPoint> draw_string(Gfx::Painter&, Gfx::FloatPoint, ByteString const&, Renderer const&) override;

    DeprecatedFlyString base_font_name() const { return m_base_font_name; }

protected:
    PDFErrorOr<void> initialize(Document*, NonnullRefPtr<DictObject> const&, float) override;

private:
    float get_char_width(u16 char_code) const;

    DeprecatedFlyString m_base_font_name;
    CIDSystemInfo m_system_info;
    HashMap<u16, u16> m_widths;
    u16 m_missing_width;

    int m_default_position_vector_y;
    int m_default_displacement_vector_y;
    struct VerticalMetric {
        int vertical_displacement_vector_y;
        int position_vector_x;
        int position_vector_y;
    };
    HashMap<u16, VerticalMetric> m_vertical_metrics;

    OwnPtr<CIDFontType> m_cid_font_type;
    OwnPtr<Type0CMap> m_cmap;
};

}
