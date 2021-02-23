/*
 * Copyright (c) 2020, Srimanta Barua <srimanta.barua1@gmail.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#include <AK/Span.h>
#include <AK/Vector.h>
#include <LibGfx/AffineTransform.h>
#include <LibGfx/Bitmap.h>
#include <LibTTF/Tables.h>
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
    AK::Vector<float> m_data;
};

class Loca {
public:
    static Optional<Loca> from_slice(const ReadonlyBytes&, u32 num_glyphs, IndexToLocFormat);
    u32 get_glyph_offset(u32 glyph_id) const;

private:
    Loca(const ReadonlyBytes& slice, u32 num_glyphs, IndexToLocFormat index_to_loc_format)
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
        Glyph(const ReadonlyBytes& slice, i16 xmin, i16 ymin, i16 xmax, i16 ymax, i16 num_contours = -1)
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
        RefPtr<Gfx::Bitmap> raster(float x_scale, float y_scale, GlyphCb glyph_callback) const
        {
            switch (m_type) {
            case Type::Simple:
                return raster_simple(x_scale, y_scale);
            case Type::Composite:
                return raster_composite(x_scale, y_scale, glyph_callback);
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

            ComponentIterator(const ReadonlyBytes& slice)
                : m_slice(slice)
            {
            }
            Optional<Item> next();

        private:
            ReadonlyBytes m_slice;
            bool m_has_more { true };
            u32 m_offset { 0 };
        };

        void raster_inner(Rasterizer&, Gfx::AffineTransform&) const;
        RefPtr<Gfx::Bitmap> raster_simple(float x_scale, float y_scale) const;
        template<typename GlyphCb>
        RefPtr<Gfx::Bitmap> raster_composite(float x_scale, float y_scale, GlyphCb glyph_callback) const
        {
            u32 width = (u32)(ceil((m_xmax - m_xmin) * x_scale)) + 1;
            u32 height = (u32)(ceil((m_ymax - m_ymin) * y_scale)) + 1;
            Rasterizer rasterizer(Gfx::IntSize(width, height));
            auto affine = Gfx::AffineTransform().scale(x_scale, -y_scale).translate(-m_xmin, -m_ymax);
            ComponentIterator component_iterator(m_slice);
            while (true) {
                auto opt_item = component_iterator.next();
                if (!opt_item.has_value()) {
                    break;
                }
                auto item = opt_item.value();
                auto affine_here = affine.multiply(item.affine);
                auto glyph = glyph_callback(item.glyph_id);
                glyph.raster_inner(rasterizer, affine_here);
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

    Glyf(const ReadonlyBytes& slice)
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
