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

#include "Font.h"
#include <AK/FixedArray.h>
#include <LibGfx/FloatPoint.h>
#include <LibGfx/Path.h>
#include <math.h>

namespace TTF {

extern u16 be_u16(const u8* ptr);
extern u32 be_u32(const u8* ptr);
extern i16 be_i16(const u8* ptr);

enum class SimpleGlyfFlags {
    // From spec.
    OnCurve = 0x01,
    XShortVector = 0x02,
    YShortVector = 0x04,
    RepeatFlag = 0x08,
    XIsSameOrPositiveXShortVector = 0x10,
    YIsSameOrPositiveYShortVector = 0x20,
    // Combinations
    XMask = 0x12,
    YMask = 0x24,
    XLongVector = 0x00,
    YLongVector = 0x00,
    XNegativeShortVector = 0x02,
    YNegativeShortVector = 0x04,
    XPositiveShortVector = 0x12,
    YPositiveShortVector = 0x24,
};

class PointIterator {
public:
    struct Item {
        bool on_curve;
        Gfx::FloatPoint point;
    };

    PointIterator(const ByteBuffer& slice, u16 num_points, u32 flags_offset, u32 x_offset, u32 y_offset, float x_translate, float y_translate, float x_scale, float y_scale)
        : m_slice(slice)
        , m_points_remaining(num_points)
        , m_flags_offset(flags_offset)
        , m_x_offset(x_offset)
        , m_y_offset(y_offset)
        , m_x_translate(x_translate)
        , m_y_translate(y_translate)
        , m_x_scale(x_scale)
        , m_y_scale(y_scale)
    {
    }

    Optional<Item> next()
    {
        if (m_points_remaining == 0) {
            return {};
        }
        if (m_flags_remaining > 0) {
            m_flags_remaining--;
        } else {
            m_flag = m_slice[m_flags_offset++];
            if (m_flag & (u8) SimpleGlyfFlags::RepeatFlag) {
                m_flags_remaining = m_slice[m_flags_offset++];
            }
        }
        switch (m_flag & (u8) SimpleGlyfFlags::XMask) {
        case (u8) SimpleGlyfFlags::XLongVector:
            m_last_point.set_x(m_last_point.x() + be_i16(m_slice.offset_pointer(m_x_offset)));
            m_x_offset += 2;
            break;
        case (u8) SimpleGlyfFlags::XNegativeShortVector:
            m_last_point.set_x(m_last_point.x() - m_slice[m_x_offset++]);
            break;
        case (u8) SimpleGlyfFlags::XPositiveShortVector:
            m_last_point.set_x(m_last_point.x() + m_slice[m_x_offset++]);
            break;
        default:
            break;
        }
        switch (m_flag & (u8) SimpleGlyfFlags::YMask) {
        case (u8) SimpleGlyfFlags::YLongVector:
            m_last_point.set_y(m_last_point.y() + be_i16(m_slice.offset_pointer(m_y_offset)));
            m_y_offset += 2;
            break;
        case (u8) SimpleGlyfFlags::YNegativeShortVector:
            m_last_point.set_y(m_last_point.y() - m_slice[m_y_offset++]);
            break;
        case (u8) SimpleGlyfFlags::YPositiveShortVector:
            m_last_point.set_y(m_last_point.y() + m_slice[m_y_offset++]);
            break;
        default:
            break;
        }
        m_points_remaining--;
        Item ret = {
            .on_curve = (m_flag & (u8) SimpleGlyfFlags::OnCurve) != 0,
            .point = m_last_point,
        };
        ret.point.move_by(m_x_translate, m_y_translate);
        ret.point.set_x(ret.point.x() * m_x_scale);
        ret.point.set_y(ret.point.y() * m_y_scale);
        return ret;
    }

private:
    ByteBuffer m_slice;
    u16 m_points_remaining;
    u8 m_flag { 0 };
    Gfx::FloatPoint m_last_point = { 0.0f, 0.0f };
    u32 m_flags_remaining = { 0 };
    u32 m_flags_offset;
    u32 m_x_offset;
    u32 m_y_offset;
    float m_x_translate;
    float m_y_translate;
    float m_x_scale;
    float m_y_scale;
};

class Rasterizer {
public:
    Rasterizer(Gfx::Size size)
        : m_size(size)
        , m_data(m_size.width() * m_size.height())
    {
        for (int i = 0; i < m_size.width() * m_size.height(); i++) {
            m_data[i] = 0.0;
        }
    }

    RefPtr<Gfx::Bitmap> draw_path(Gfx::Path& path)
    {
        for (auto& line : path.split_lines()) {
            draw_line(line.from, line.to);
        }
        return accumulate();
    }

private:
    RefPtr<Gfx::Bitmap> accumulate()
    {
        auto bitmap = Gfx::Bitmap::create(Gfx::BitmapFormat::RGBA32, m_size);
        Color base_color = Color::from_rgb(0xffffff);
        for (int y = 0; y < m_size.height(); y++) {
            float accumulator = 0.0;
            for (int x = 0; x < m_size.width(); x++) {
                accumulator += m_data[y * m_size.width() + x];
                float value = accumulator;
                if (value < 0.0) {
                    value = -value;
                }
                if (value > 1.0) {
                    value = 1.0;
                }
                u8 alpha = value * 255.0;
                bitmap->set_pixel(x, y, base_color.with_alpha(alpha));
            }
        }
        return bitmap;
    }

    void draw_line(Gfx::FloatPoint p0, Gfx::FloatPoint p1)
    {
        ASSERT(p0.x() >= 0.0 && p0.y() >= 0.0 && p0.x() <= m_size.width() && p0.y() <= m_size.height());
        ASSERT(p1.x() >= 0.0 && p1.y() >= 0.0 && p1.x() <= m_size.width() && p1.y() <= m_size.height());
        // If we're on the same Y, there's no need to draw
        if (p0.y() == p1.y()) {
            return;
        }

        float direction = -1.0;
        if (p1.y() < p0.y()) {
            direction = 1.0;
            auto tmp = p0;
            p0 = p1;
            p1 = tmp;
        }

        float dxdy = (p1.x() - p0.x()) / (p1.y() - p0.y());
        u32 y0 = floor(p0.y());
        u32 y1 = ceil(p1.y());
        float x_cur = p0.x();

        for (u32 y = y0; y < y1; y++) {
            u32 line_offset = m_size.width() * y;

            float dy = min(y + 1.0f, p1.y()) - max((float) y, p0.y());
            float directed_dy = dy * direction;
            float x_next = x_cur + dy * dxdy;
            if (x_next < 0.0) {
                x_next = 0.0;
            }
            float x0 = x_cur;
            float x1 = x_next;
            if (x1 < x0) {
                x1 = x_cur;
                x0 = x_next;
            }
            float x0_floor = floor(x0);
            float x1_ceil = ceil(x1);
            u32 x0i = x0_floor;

            if (x1_ceil <= x0_floor + 1.0) {
                // If x0 and x1 are within the same pixel, then area to the right is (1 - (mid(x0, x1) - x0_floor)) * dy
                float area = ((x0 + x1) * 0.5) - x0_floor;
                m_data[line_offset + x0i] += directed_dy * (1.0 - area);
                m_data[line_offset + x0i + 1] += directed_dy * area;
            } else {
                float dydx = 1.0 / dxdy;
                float x0_right = 1.0 - (x0 - x0_floor);
                u32 x1_floor_i = floor(x1);
                float area_upto_here = 0.5 * x0_right * x0_right * dydx;
                m_data[line_offset + x0i] += direction * area_upto_here;
                for (u32 x = x0i + 1; x < x1_floor_i; x++) {
                    x0_right += 1.0;
                    float total_area_here = 0.5 * x0_right * x0_right * dydx;
                    m_data[line_offset + x] += direction * (total_area_here - area_upto_here);
                    area_upto_here = total_area_here;
                }
                m_data[line_offset + x1_floor_i] += direction * (dy - area_upto_here);
            }

            x_cur = x_next;
        }
    }

    Gfx::Size m_size;
    FixedArray<float> m_data;
};

Font::GlyphHorizontalMetrics Font::Hmtx::get_glyph_horizontal_metrics(u32 glyph_id) const
{
    ASSERT(glyph_id < m_num_glyphs);
    if (glyph_id < m_number_of_h_metrics) {
        auto offset = glyph_id * (u32) Sizes::LongHorMetric;
        u16 advance_width = be_u16(m_slice.offset_pointer(offset));
        i16 left_side_bearing = be_i16(m_slice.offset_pointer(offset + 2));
        return GlyphHorizontalMetrics {
            .advance_width = advance_width,
            .left_side_bearing = left_side_bearing,
        };
    }
    auto offset = m_number_of_h_metrics * (u32) Sizes::LongHorMetric + (glyph_id - m_number_of_h_metrics) * (u32) Sizes::LeftSideBearing;
    u16 advance_width = be_u16(m_slice.offset_pointer((m_number_of_h_metrics - 1) * (u32) Sizes::LongHorMetric));
    i16 left_side_bearing = be_i16(m_slice.offset_pointer(offset));
    return GlyphHorizontalMetrics {
        .advance_width = advance_width,
        .left_side_bearing = left_side_bearing,
    };
}

u32 Font::Loca::get_glyph_offset(u32 glyph_id) const
{
    ASSERT(glyph_id < m_num_glyphs);
    switch (m_index_to_loc_format) {
    case IndexToLocFormat::Offset16:
        return ((u32) be_u16(m_slice.offset_pointer(glyph_id * 2))) * 2;
    case IndexToLocFormat::Offset32:
        return be_u32(m_slice.offset_pointer(glyph_id * 4));
    default:
        ASSERT_NOT_REACHED();
    }
}

Font::Glyf::Glyph Font::Glyf::Glyph::simple(const ByteBuffer& slice, u16 num_contours, i16 xmin, i16 ymin, i16 xmax, i16 ymax)
{
    auto ret = Glyph(slice, Type::Simple);
    ret.m_meta.simple = Simple {
        .num_contours = num_contours,
        .xmin = xmin,
        .ymin = ymin,
        .xmax = xmax,
        .ymax = ymax,
    };
    return ret;
}

// FIXME: This is currently just a dummy. Need to add support for composite glyphs.
Font::Glyf::Glyph Font::Glyf::Glyph::composite(const ByteBuffer& slice)
{
    auto ret = Glyph(slice, Type::Composite);
    ret.m_meta.composite = Composite();
    return ret;
}

RefPtr<Gfx::Bitmap> Font::Glyf::Glyph::raster(float x_scale, float y_scale) const
{
    switch (m_type) {
    case Type::Simple:
        return raster_simple(x_scale, y_scale);
    case Type::Composite:
        // FIXME: Add support for composite glyphs
        TODO();
    }
    ASSERT_NOT_REACHED();
}

static void get_ttglyph_offsets(const ByteBuffer& slice, u32 num_points, u32 flags_offset, u32 *x_offset, u32 *y_offset)
{
    u32 flags_size = 0;
    u32 x_size = 0;
    u32 repeat_count;
    while (num_points > 0) {
        u8 flag = slice[flags_offset + flags_size];
        if (flag & (u8) SimpleGlyfFlags::RepeatFlag) {
            flags_size++;
            repeat_count = slice[flags_offset + flags_size] + 1;
        } else {
            repeat_count = 1;
        }
        flags_size++;
        switch (flag & (u8) SimpleGlyfFlags::XMask) {
        case (u8) SimpleGlyfFlags::XLongVector:
            x_size += repeat_count * 2;
            break;
        case (u8) SimpleGlyfFlags::XNegativeShortVector:
        case (u8) SimpleGlyfFlags::XPositiveShortVector:
            x_size += repeat_count;
            break;
        default:
            break;
        }
        num_points -= repeat_count;
    }
    *x_offset = flags_offset + flags_size;
    *y_offset = *x_offset + x_size;
}

RefPtr<Gfx::Bitmap> Font::Glyf::Glyph::raster_simple(float x_scale, float y_scale) const
{
    auto simple = m_meta.simple;
    // Get offets for flags, x, and y.
    u16 num_points = be_u16(m_slice.offset_pointer((simple.num_contours - 1) * 2)) + 1;
    u16 num_instructions = be_u16(m_slice.offset_pointer(simple.num_contours * 2));
    u32 flags_offset = simple.num_contours * 2 + 2 + num_instructions;
    u32 x_offset = 0;
    u32 y_offset = 0;
    get_ttglyph_offsets(m_slice, num_points, flags_offset, &x_offset, &y_offset);

    // Prepare to render glyph.
    u32 width = (u32) (ceil((simple.xmax - simple.xmin) * x_scale)) + 1;
    u32 height = (u32) (ceil((simple.ymax - simple.ymin) * y_scale)) + 1;
    Gfx::Path path;
    PointIterator point_iterator(m_slice, num_points, flags_offset, x_offset, y_offset, -simple.xmin, -simple.ymax, x_scale, -y_scale);

    int last_contour_end = -1;
    u32 contour_index = 0;
    u32 contour_size = 0;
    Optional<Gfx::FloatPoint> contour_start = {};
    Optional<Gfx::FloatPoint> last_offcurve_point = {};

    // Render glyph
    while (true) {
        if (!contour_start.has_value()) {
            if (contour_index >= simple.num_contours) {
                break;
            }
            int current_contour_end = be_u16(m_slice.offset_pointer(contour_index++ * 2));
            contour_size = current_contour_end - last_contour_end;
            last_contour_end = current_contour_end;
            auto opt_item = point_iterator.next();
            if (!opt_item.has_value()) {
                ASSERT_NOT_REACHED();
            }
            contour_start = opt_item.value().point;
            path.move_to(contour_start.value());
            contour_size--;
        } else if (!last_offcurve_point.has_value()) {
            if (contour_size > 0) {
                auto opt_item = point_iterator.next();
                // FIXME: Should we draw a line to the first point here?
                if (!opt_item.has_value()) {
                    break;
                }
                auto item = opt_item.value();
                contour_size--;
                if (item.on_curve) {
                    path.line_to(item.point);
                } else if (contour_size > 0) {
                    auto opt_next_item = point_iterator.next();
                    // FIXME: Should we draw a quadratic bezier to the first point here?
                    if (!opt_next_item.has_value()) {
                        break;
                    }
                    auto next_item = opt_next_item.value();
                    contour_size--;
                    if (next_item.on_curve) {
                        path.quadratic_bezier_curve_to(item.point, next_item.point);
                    } else {
                        auto mid_point = Gfx::FloatPoint::interpolate(item.point, next_item.point, 0.5);
                        path.quadratic_bezier_curve_to(item.point, mid_point);
                        last_offcurve_point = next_item.point;
                    }
                } else {
                    path.quadratic_bezier_curve_to(item.point, contour_start.value());
                    contour_start = {};
                }
            } else {
                path.line_to(contour_start.value());
                contour_start = {};
            }
        } else {
            auto point0 = last_offcurve_point.value();
            last_offcurve_point = {};
            if (contour_size > 0) {
                auto opt_item = point_iterator.next();
                // FIXME: Should we draw a quadratic bezier to the first point here?
                if (!opt_item.has_value()) {
                    break;
                }
                auto item = opt_item.value();
                contour_size--;
                if (item.on_curve) {
                    path.quadratic_bezier_curve_to(point0, item.point);
                } else {
                    auto mid_point = Gfx::FloatPoint::interpolate(point0, item.point, 0.5);
                    path.quadratic_bezier_curve_to(point0, mid_point);
                    last_offcurve_point = item.point;
                }
            } else {
                path.quadratic_bezier_curve_to(point0, contour_start.value());
                contour_start = {};
            }
        }
    }

    return Rasterizer(Gfx::Size(width, height)).draw_path(path);
}

Font::Glyf::Glyph Font::Glyf::glyph(u32 offset) const
{
    ASSERT(m_slice.size() >= offset + (u32) Sizes::GlyphHeader);
    i16 num_contours = be_i16(m_slice.offset_pointer(offset));
    i16 xmin = be_i16(m_slice.offset_pointer(offset + (u32) Offsets::XMin));
    i16 ymin = be_i16(m_slice.offset_pointer(offset + (u32) Offsets::YMin));
    i16 xmax = be_i16(m_slice.offset_pointer(offset + (u32) Offsets::XMax));
    i16 ymax = be_i16(m_slice.offset_pointer(offset + (u32) Offsets::YMax));
    auto slice = ByteBuffer::wrap(m_slice.offset_pointer(offset + (u32) Sizes::GlyphHeader), m_slice.size() - offset - (u32) Sizes::GlyphHeader);
    if (num_contours < 0) {
        return Glyph::composite(slice);
    }
    return Glyph::simple(slice, num_contours, xmin, ymin, xmax, ymax);
}

}
