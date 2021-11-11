/*
 * Copyright (c) 2020, Srimanta Barua <srimanta.barua1@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Span.h>
#include <AK/Vector.h>
#include <LibGfx/AffineTransform.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/TrueTypeFont/Tables.h>
#include <math.h>

namespace TTF {

class Rasterizer {
public:
    Rasterizer(Gfx::IntSize);
    void draw_path(Gfx::Path&);
    RefPtr<Gfx::Bitmap> accumulate();

private:
    void draw_line(Gfx::FloatPoint, Gfx::FloatPoint);

    Gfx::IntSize m_size;
    Vector<float> m_data;
};

class Loca {
public:
    static Optional<Loca> from_slice(ReadonlyBytes, u32 num_glyphs, IndexToLocFormat);
    u32 get_glyph_offset(u32 glyph_id) const;

private:
    Loca(ReadonlyBytes slice, u32 num_glyphs, IndexToLocFormat index_to_loc_format)
        : m_slice(slice)
        , m_num_glyphs(num_glyphs)
        , m_index_to_loc_format(index_to_loc_format)
    {
    }

    ReadonlyBytes m_slice;
    u32 m_num_glyphs { 0 };
    IndexToLocFormat m_index_to_loc_format;
};

class Glyf {
public:
    class Glyph {
    public:
        Glyph(ReadonlyBytes slice, i16 xmin, i16 ymin, i16 xmax, i16 ymax, i16 num_contours = -1)
            : m_xmin(xmin)
            , m_ymin(ymin)
            , m_xmax(xmax)
            , m_ymax(ymax)
            , m_num_contours(num_contours)
            , m_slice(slice)
        {
            if (m_num_contours >= 0) {
                m_type = Type::Simple;
            }
        }
        template<typename GlyphCb>
        RefPtr<Gfx::Bitmap> rasterize(i16 font_ascender, i16 font_descender, float x_scale, float y_scale, GlyphCb glyph_callback) const
        {
            switch (m_type) {
            case Type::Simple:
                return rasterize_simple(font_ascender, font_descender, x_scale, y_scale);
            case Type::Composite:
                return rasterize_composite(font_ascender, font_descender, x_scale, y_scale, glyph_callback);
            }
            VERIFY_NOT_REACHED();
        }
        int ascender() const { return m_ymax; }
        int descender() const { return m_ymin; }

    private:
        enum class Type {
            Simple,
            Composite,
        };

        class ComponentIterator {
        public:
            struct Item {
                u16 glyph_id;
                Gfx::AffineTransform affine;
            };

            ComponentIterator(ReadonlyBytes slice)
                : m_slice(slice)
            {
            }
            Optional<Item> next();

        private:
            ReadonlyBytes m_slice;
            bool m_has_more { true };
            u32 m_offset { 0 };
        };

        void rasterize_impl(Rasterizer&, Gfx::AffineTransform const&) const;
        RefPtr<Gfx::Bitmap> rasterize_simple(i16 ascender, i16 descender, float x_scale, float y_scale) const;
        template<typename GlyphCb>
        RefPtr<Gfx::Bitmap> rasterize_composite(i16 font_ascender, i16 font_descender, float x_scale, float y_scale, GlyphCb glyph_callback) const
        {
            u32 width = (u32)(ceilf((m_xmax - m_xmin) * x_scale)) + 1;
            u32 height = (u32)(ceilf((font_ascender - font_descender) * y_scale)) + 1;
            Rasterizer rasterizer(Gfx::IntSize(width, height));
            auto affine = Gfx::AffineTransform().scale(x_scale, -y_scale).translate(-m_xmin, -font_ascender);
            ComponentIterator component_iterator(m_slice);
            while (true) {
                auto opt_item = component_iterator.next();
                if (!opt_item.has_value()) {
                    break;
                }
                auto item = opt_item.value();
                auto affine_here = affine.multiply(item.affine);
                auto glyph = glyph_callback(item.glyph_id);
                glyph.rasterize_impl(rasterizer, affine_here);
            }
            return rasterizer.accumulate();
        }

        Type m_type { Type::Composite };
        i16 m_xmin { 0 };
        i16 m_ymin { 0 };
        i16 m_xmax { 0 };
        i16 m_ymax { 0 };
        i16 m_num_contours { -1 };
        ReadonlyBytes m_slice;
    };

    Glyf(ReadonlyBytes slice)
        : m_slice(slice)
    {
    }
    Glyph glyph(u32 offset) const;

private:
    enum class Offsets {
        XMin = 2,
        YMin = 4,
        XMax = 6,
        YMax = 8,
    };
    enum class Sizes {
        GlyphHeader = 10,
    };

    ReadonlyBytes m_slice;
};

}
