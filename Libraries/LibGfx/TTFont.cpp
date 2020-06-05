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

#include "TTFont.h"
#include <AK/LogStream.h>
#include <bits/stdint.h>
#include <LibCore/File.h>
#include <math.h>

namespace Gfx {
namespace TTF {

static float min(float x, float y)
{
    return x < y ? x : y;
}

static float max(float x, float y)
{
    return x > y ? x : y;
}

static u16 be_u16(const u8* ptr)
{
    return (((u16) ptr[0]) << 8) | ((u16) ptr[1]);
}

static u32 be_u32(const u8* ptr)
{
    return (((u32) ptr[0]) << 24) | (((u32) ptr[1]) << 16) | (((u32) ptr[2]) << 8) | ((u32) ptr[3]);
}

static i16 be_i16(const u8* ptr)
{
    return (((i16) ptr[0]) << 8) | ((i16) ptr[1]);
}

static u32 tag_from_str(const char *str)
{
    return be_u32((const u8*) str);
}

u16 Font::Head::units_per_em() const
{
    return be_u16(m_slice.offset_pointer(18));
}

i16 Font::Head::xmin() const
{
    return be_i16(m_slice.offset_pointer(36));
}

i16 Font::Head::ymin() const
{
    return be_i16(m_slice.offset_pointer(38));
}

i16 Font::Head::xmax() const
{
    return be_i16(m_slice.offset_pointer(40));
}

i16 Font::Head::ymax() const
{
    return be_i16(m_slice.offset_pointer(42));
}

u16 Font::Head::lowest_recommended_ppem() const
{
    return be_u16(m_slice.offset_pointer(46));
}

Font::IndexToLocFormat Font::Head::index_to_loc_format() const
{
    i16 raw = be_i16(m_slice.offset_pointer(50));
    switch (raw) {
    case 0:
        return IndexToLocFormat::Offset16;
    case 1:
        return IndexToLocFormat::Offset32;
    default:
        ASSERT_NOT_REACHED();
    }
}

u16 Font::Hhea::number_of_h_metrics() const
{
    return be_u16(m_slice.offset_pointer(34));
}

u16 Font::Maxp::num_glyphs() const
{
    return be_u16(m_slice.offset_pointer(4));
}

struct Point {
    float x;
    float y;

    Point(float x, float y)
        : x(x)
        , y(y)
    {
    }

    static Point interpolate(const Point& a, const Point &b, float t)
    {
        return Point {
            .x = a.x * (1.0f - t) + b.x * t,
            .y = a.y * (1.0f - t) + b.y * t,
        };
    }

    static float squared_distance(const Point& a, const Point& b)
    {
        float x_diff = a.x - b.x;
        float y_diff = a.y - b.y;
        return x_diff * x_diff + y_diff * y_diff;
    }
};

class PointIterator {
public:
    struct Item {
        bool on_curve;
        Point point;
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
            if (m_flag & 0x08) {
                m_flags_remaining = m_slice[m_flags_offset++];
            }
        }
        switch (m_flag & 0x12) {
        case 0x00:
            m_last_point.x += be_i16(m_slice.offset_pointer(m_x_offset));
            m_x_offset += 2;
            break;
        case 0x02:
            m_last_point.x -= m_slice[m_x_offset++];
            break;
        case 0x12:
            m_last_point.x += m_slice[m_x_offset++];
            break;
        default:
            break;
        }
        switch (m_flag & 0x24) {
        case 0x00:
            m_last_point.y += be_i16(m_slice.offset_pointer(m_y_offset));
            m_y_offset += 2;
            break;
        case 0x04:
            m_last_point.y -= m_slice[m_y_offset++];
            break;
        case 0x24:
            m_last_point.y += m_slice[m_y_offset++];
            break;
        default:
            break;
        }
        m_points_remaining--;
        Item ret = {
            .on_curve = (m_flag & 0x01) != 0,
            .point = m_last_point,
        };
        ret.point.x += m_x_translate;
        ret.point.y += m_y_translate;
        ret.point.x *= m_x_scale;
        ret.point.y *= m_y_scale;
        return ret;
    }

private:
    ByteBuffer m_slice;
    u16 m_points_remaining;
    u8 m_flag { 0 };
    Point m_last_point = { 0.0f, 0.0f };
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
    Rasterizer(Size size)
        : m_size(size)
    {
        m_data = OwnPtr(new float[m_size.width() * m_size.height()]);
    }

    void move_to(Point& point)
    {
        m_last_point = point;
    }

    void line_to(Point& point)
    {
        draw_line(m_last_point, point);
        m_last_point = point;
    }

    // FIXME: Use a better algorithm to split/approximate bezier curve.
    void quadratic_bezier_to(Point& control, Point& end_point)
    {
        float arbitrary = 15.0;
        auto mid_point = Point::interpolate(m_last_point, end_point, 0.5);
        float squared_distance = Point::squared_distance(mid_point, control);
        u32 num_sections = 1 + floor(sqrtf(arbitrary * squared_distance));
        float delta = 1.0 / num_sections;
        float t = 0.0;
        Point p_cur = m_last_point;
        for (u32 i = 0; i < num_sections - 1; i++) {
            t += delta;
            Point pn = Point::interpolate(Point::interpolate(m_last_point, control, t), Point::interpolate(control, end_point, t), t);
            draw_line(p_cur, pn);
            p_cur = pn;
        }
        draw_line(p_cur, end_point);
        m_last_point = end_point;
    }

    AABitmap accumulate()
    {
        AABitmap bitmap(m_size);
        float accumulator = 0.0;
        for (int y = 0; y < m_size.height(); y++) {
            for (int x = 0; x < m_size.width(); x++) {
                accumulator += m_data[y * m_size.width() + x];
                float value = accumulator;
                if (value < 0.0) {
                    value = -value;
                }
                if (value > 1.0) {
                    value = 1.0;
                }
                bitmap.set_byte_at(x, y, value * 255.0);
            }
        }
        return bitmap;
    }

private:
    void draw_line(Point& p0, Point& p1)
    {
        ASSERT(p0.x >= 0.0 && p0.y >= 0.0 && p0.x <= m_size.width() && p0.y <= m_size.height());
        ASSERT(p1.x >= 0.0 && p1.y >= 0.0 && p1.x <= m_size.width() && p1.y <= m_size.height());
        // If we're on the same Y, there's no need to draw
        if (p0.y == p1.y) {
            return;
        }

        float direction = 1.0;
        if (p1.y < p0.y) {
            direction = -1.0;
        }

        float dxdy = (p1.x - p0.x) / (p1.y - p0.y);
        u32 y0 = floor(p0.y);
        u32 y1 = ceil(p1.y);
        float x_cur = p0.x;

        for (u32 y = y0; y < y1; y++) {
            u32 line_offset = m_size.width() * y;

            float dy = min(y + 1, p1.y) - max(y, p0.y);
            float directed_dy = dy * direction;
            float x_next = x_cur + dy * dxdy;
            if (x_next < 0.0) {
                x_next = 0.0;
            }
            float x0 = x_cur;
            float x1 = x_next;
            if (x1 < x0) {
                x1 =  x_cur;
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

    Size m_size;
    Point m_last_point { 0.0, 0.0 };
    OwnPtr<float> m_data;
};

Font::GlyphHorizontalMetrics Font::Hmtx::get_glyph_horizontal_metrics(u32 glyph_id) const
{
    ASSERT(glyph_id < m_num_glyphs);
    auto offset = glyph_id * 2;
    i16 left_side_bearing = be_i16(m_slice.offset_pointer(offset + 2));
    if (glyph_id < m_number_of_h_metrics) {
        u16 advance_width = be_u16(m_slice.offset_pointer(offset));
        return GlyphHorizontalMetrics {
            .advance_width = advance_width,
            .left_side_bearing = left_side_bearing,
        };
    } else {
        u16 advance_width = be_u16(m_slice.offset_pointer((m_number_of_h_metrics - 1) * 2));
        return GlyphHorizontalMetrics {
            .advance_width = advance_width,
            .left_side_bearing = left_side_bearing,
        };
    }
}

Font::Cmap::Subtable::Platform Font::Cmap::Subtable::platform_id() const
{
    switch (m_raw_platform_id) {
    case 0:  return Platform::Unicode;
    case 1:  return Platform::Macintosh;
    case 3:  return Platform::Windows;
    case 4:  return Platform::Custom;
    default: ASSERT_NOT_REACHED();
    }
}

Font::Cmap::Subtable::Format Font::Cmap::Subtable::format() const
{
    switch (be_u16(m_slice.offset_pointer(0))) {
        case 0:  return Format::ByteEncoding;
        case 2:  return Format::HighByte;
        case 4:  return Format::SegmentToDelta;
        case 6:  return Format::TrimmedTable;
        case 8:  return Format::Mixed16And32;
        case 10: return Format::TrimmedArray;
        case 12: return Format::SegmentedCoverage;
        case 13: return Format::ManyToOneRange;
        case 14: return Format::UnicodeVariationSequences;
        default: ASSERT_NOT_REACHED();
    }
}

u32 Font::Cmap::num_subtables() const
{
    return be_u16(m_slice.offset_pointer(2));
}

Optional<Font::Cmap::Subtable> Font::Cmap::subtable(u32 index) const
{
    if (index >= num_subtables()) {
        return {};
    }
    u32 record_offset = 4 + index * 8;
    u16 platform_id = be_u16(m_slice.offset_pointer(record_offset));
    u16 encoding_id = be_u16(m_slice.offset_pointer(record_offset + 2));
    u32 subtable_offset = be_u32(m_slice.offset_pointer(record_offset + 4));
    ASSERT(subtable_offset < m_slice.size());
    auto subtable_slice = ByteBuffer::wrap(m_slice.offset_pointer(subtable_offset), m_slice.size() - subtable_offset);
    return Subtable(subtable_slice, platform_id, encoding_id);
}

// FIXME: This only handles formats 4 (SegmentToDelta) and 12 (SegmentedCoverage) for now.
u32 Font::Cmap::Subtable::glyph_id_for_codepoint(u32 codepoint) const
{
    switch (format()) {
    case Format::SegmentToDelta:
        return glyph_id_for_codepoint_table_4(codepoint);
    case Format::SegmentedCoverage:
        return glyph_id_for_codepoint_table_12(codepoint);
    default:
        return 0;
    }
}

u32 Font::Cmap::Subtable::glyph_id_for_codepoint_table_4(u32 codepoint) const
{
    u32 segcount_x2 = be_u16(m_slice.offset_pointer(6));
    if (m_slice.size() < segcount_x2 * 4 + 16) {
        return 0;
    }
    for (u32 offset = 0; offset < segcount_x2; offset += 2) {
        u32 end_codepoint = be_u16(m_slice.offset_pointer(14 + offset));
        if (codepoint > end_codepoint) {
            continue;
        }
        u32 start_codepoint = be_u16(m_slice.offset_pointer(16 + segcount_x2 + offset));
        if (codepoint < start_codepoint) {
            break;
        }
        u32 delta = be_u16(m_slice.offset_pointer(16 + segcount_x2 * 2 + offset));
        u32 range = be_u16(m_slice.offset_pointer(16 + segcount_x2 * 3 + offset));
        if (range == 0) {
            return (codepoint + delta) & 0xffff;
        } else {
            u32 glyph_offset = 16 + segcount_x2 * 3 + offset + range + (codepoint - start_codepoint) * 2;
            ASSERT(glyph_offset + 2 <= m_slice.size());
            return (be_u16(m_slice.offset_pointer(glyph_offset)) + delta) & 0xffff;
        }
    }
    return 0;
}

u32 Font::Cmap::Subtable::glyph_id_for_codepoint_table_12(u32 codepoint) const
{
    u32 num_groups = be_u32(m_slice.offset_pointer(12));
    ASSERT(m_slice.size() >= 16 + 12 * num_groups);
    for (u32 offset = 0; offset < num_groups * 12; offset += 12) {
        u32 start_codepoint = be_u32(m_slice.offset_pointer(16 + offset));
        if (codepoint < start_codepoint) {
            break;
        }
        u32 end_codepoint = be_u32(m_slice.offset_pointer(20 + offset));
        if (codepoint > end_codepoint) {
            continue;
        }
        u32 glyph_offset = be_u32(m_slice.offset_pointer(24 + offset));
        return codepoint - start_codepoint + glyph_offset;
    }
    return 0;
}

u32 Font::Cmap::glyph_id_for_codepoint(u32 codepoint) const
{
    auto opt_subtable = subtable(m_active_index);
    if (!opt_subtable.has_value()) {
        return 0;
    }
    auto subtable = opt_subtable.value();
    return subtable.glyph_id_for_codepoint(codepoint);
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
    auto ret = Glyph(slice, Type::Composite);
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

AABitmap Font::Glyf::Glyph::raster(float x_scale, float y_scale) const
{
    switch (m_type) {
    case Type::Simple:
        return raster_simple(x_scale, y_scale);
    case Type::Composite:
        // FIXME: Add support for composite glyphs
        ASSERT_NOT_REACHED();
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
        if (flag & 0x08) {
            flags_size++;
            repeat_count = slice[flags_offset + flags_size] + 1;
        } else {
            repeat_count = 1;
        }
        flags_size++;
        switch (flag & 0x12) {
        case 0x00:
            x_size += repeat_count * 2;
            break;
        case 0x02:
        case 0x12:
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

AABitmap Font::Glyf::Glyph::raster_simple(float x_scale, float y_scale) const
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
    u32 width = (u32) (ceil((simple.xmax - simple.xmin) * x_scale)) + 2;
    u32 height = (u32) (ceil((simple.ymax - simple.ymin) * y_scale)) + 2;
    Rasterizer rasterizer(Size(width, height));
    PointIterator point_iterator(m_slice, num_points, flags_offset, x_offset, y_offset, -simple.xmin, -simple.ymin, x_scale, -y_scale);

    int last_contour_end = -1;
    u32 contour_index = 0;
    u32 contour_size = 0;
    Optional<Point> contour_start = {};
    Optional<Point> last_offcurve_point = {};

    // Render glyph
    while (true) {
        if (!contour_start.has_value()) {
            int current_contour_end = be_u16(m_slice.offset_pointer(contour_index++ * 2));
            contour_size = current_contour_end - last_contour_end - 1;
            last_contour_end = current_contour_end;
            auto opt_item = point_iterator.next();
            if (!opt_item.has_value() || !opt_item.value().on_curve) {
                break;
            }
            contour_start = opt_item.value().point;
            rasterizer.move_to(contour_start.value());
        } else if (!last_offcurve_point.has_value()) {
            if (contour_size > 0) {
                auto opt_item = point_iterator.next();
                ASSERT(opt_item.has_value());
                auto item = opt_item.value();
                contour_size--;
                if (item.on_curve) {
                    rasterizer.line_to(item.point);
                } else if (contour_size > 0) {
                    auto opt_next_item = point_iterator.next();
                    ASSERT(opt_next_item.has_value());
                    auto next_item = opt_next_item.value();
                    contour_size--;
                    if (next_item.on_curve) {
                        rasterizer.quadratic_bezier_to(item.point, next_item.point);
                    } else {
                        auto mid_point = Point::interpolate(item.point, next_item.point, 0.5);
                        rasterizer.quadratic_bezier_to(item.point, mid_point);
                        last_offcurve_point = next_item.point;
                    }
                } else {
                    rasterizer.quadratic_bezier_to(item.point, contour_start.value());
                    contour_start = {};
                }
            } else {
                rasterizer.line_to(contour_start.value());
                contour_start = {};
            }
        } else {
            auto point0 = last_offcurve_point.value();
            last_offcurve_point = {};
            if (contour_size > 0) {
                auto opt_item = point_iterator.next();
                ASSERT(opt_item.has_value());
                auto item = opt_item.value();
                if (item.on_curve) {
                    rasterizer.quadratic_bezier_to(point0, item.point);
                } else {
                    auto mid_point = Point::interpolate(point0, item.point, 0.5);
                    rasterizer.quadratic_bezier_to(point0, mid_point);
                    last_offcurve_point = item.point;
                }
            } else {
                rasterizer.quadratic_bezier_to(point0, contour_start.value());
                contour_start = {};
            }
        }
    }

    return rasterizer.accumulate();
}

Font::Glyf::Glyph Font::Glyf::glyph(u32 offset) const
{
    ASSERT(m_slice.size() >= offset + 10);
    i16 num_contours = be_i16(m_slice.offset_pointer(offset));
    i16 xmin = be_i16(m_slice.offset_pointer(offset + 2));
    i16 ymin = be_i16(m_slice.offset_pointer(offset + 4));
    i16 xmax = be_i16(m_slice.offset_pointer(offset + 6));
    i16 ymax = be_i16(m_slice.offset_pointer(offset + 8));
    auto slice = ByteBuffer::wrap(m_slice.offset_pointer(offset), m_slice.size() - offset);
    if (num_contours < 0) {
        return Glyph::composite(slice);
    } else {
        return Glyph::simple(slice, num_contours, xmin, ymin, xmax, ymax);
    }
}

OwnPtr<Font> Font::load_from_file(const StringView& path, unsigned index)
{
    dbg() << "path: " << path << " | index: " << index;
    auto file_or_error = Core::File::open(String(path), Core::IODevice::ReadOnly);
    if (file_or_error.is_error()) {
        dbg() << "Could not open file: " << file_or_error.error();
        return nullptr;
    }
    auto file = file_or_error.value();
    if (!file->open(Core::IODevice::ReadOnly)) {
        dbg() << "Could not open file";
        return nullptr;
    }
    auto buffer = file->read_all();
    if (buffer.size() < 4) {
        dbg() << "Font file too small";
        return nullptr;
    }
    u32 tag = be_u32(buffer.data());
    if (tag == tag_from_str("ttcf")) {
        // It's a font collection
        if (buffer.size() < 12 + 4 * (index + 1)) {
            dbg() << "Font file too small";
            return nullptr;
        }
        u32 offset = be_u32(buffer.offset_pointer(12 + 4 * index));
        return OwnPtr(new Font(move(buffer), offset));
    } else if (tag == tag_from_str("OTTO")) {
        dbg() << "CFF fonts not supported yet";
        return nullptr;
    } else if (tag != 0x00010000) {
        dbg() << "Not a valid  font";
        return nullptr;
    } else {
        return OwnPtr(new Font(move(buffer), 0));
    }
}

// FIXME: "loca" and "glyf" are not available for CFF fonts.
AABitmap Font::raster_codepoint(u32 codepoint, float x_scale, float y_scale) const
{
    auto glyph_id = m_cmap.glyph_id_for_codepoint(codepoint);
    auto glyph_offset = m_loca.get_glyph_offset(glyph_id);
    auto glyph = m_glyf.glyph(glyph_offset);
    return glyph.raster(x_scale, y_scale);
}

// FIXME: "loca" and "glyf" are not available for CFF fonts.
Font::Font(ByteBuffer&& buffer, u32 offset)
    : m_buffer(move(buffer))
{
    ASSERT(m_buffer.size() >= offset + 12);
    Optional<ByteBuffer> head_slice = {};
    Optional<ByteBuffer> hhea_slice = {};
    Optional<ByteBuffer> maxp_slice = {};
    Optional<ByteBuffer> hmtx_slice = {};
    Optional<ByteBuffer> cmap_slice = {};
    Optional<ByteBuffer> loca_slice = {};
    Optional<ByteBuffer> glyf_slice = {};

    //auto sfnt_version = be_u32(data + offset);
    auto num_tables = be_u16(m_buffer.offset_pointer(offset + 4));
    ASSERT(m_buffer.size() >= offset + 12 + num_tables * 16);

    for (auto i = 0; i < num_tables; i++) {
        u32 record_offset = offset + 12 + i * 16;
        u32 tag = be_u32(m_buffer.offset_pointer(record_offset));
        u32 table_offset = be_u32(m_buffer.offset_pointer(record_offset + 8));
        u32 table_length = be_u32(m_buffer.offset_pointer(record_offset + 12));
        ASSERT(m_buffer.size() >= table_offset + table_length);
        auto buffer = ByteBuffer::wrap(m_buffer.offset_pointer(table_offset), table_length);

        // Get the table offsets we need.
        if (tag == tag_from_str("head")) {
            head_slice = buffer;
        } else if (tag == tag_from_str("hhea")) {
            hhea_slice = buffer;
        } else if (tag == tag_from_str("maxp")) {
            maxp_slice = buffer;
        } else if (tag == tag_from_str("hmtx")) {
            hmtx_slice = buffer;
        } else if (tag == tag_from_str("cmap")) {
            cmap_slice = buffer;
        } else if (tag == tag_from_str("loca")) {
            loca_slice = buffer;
        } else if (tag == tag_from_str("glyf")) {
            glyf_slice = buffer;
        }
    }

    // Check that we've got everything we need.
    ASSERT(head_slice.has_value());
    ASSERT(hhea_slice.has_value());
    ASSERT(maxp_slice.has_value());
    ASSERT(hmtx_slice.has_value());
    ASSERT(cmap_slice.has_value());
    ASSERT(loca_slice.has_value());
    ASSERT(glyf_slice.has_value());

    // Load the tables.
    m_head = Head(head_slice.value());
    m_hhea = Hhea(hhea_slice.value());
    m_maxp = Maxp(maxp_slice.value());
    m_hmtx = Hmtx(hmtx_slice.value(), m_maxp.num_glyphs(), m_hhea.number_of_h_metrics());
    m_cmap = Cmap(cmap_slice.value());
    m_loca = Loca(loca_slice.value(), m_maxp.num_glyphs(), m_head.index_to_loc_format());
    m_glyf = Glyf(glyf_slice.value());

    // Select cmap table. FIXME: Do this better. Right now, just looks for platform "Windows"
    // and corresponding encoding "Unicode full repertoire", or failing that, "Unicode BMP"
    for (u32 i = 0; i < m_cmap.num_subtables(); i++) {
        auto opt_subtable = m_cmap.subtable(i);
        if (!opt_subtable.has_value()) {
            continue;
        }
        auto subtable = opt_subtable.value();
        if (subtable.platform_id() == Cmap::Subtable::Platform::Windows) {
            if (subtable.encoding_id() == 10) {
                m_cmap.set_active_index(i);
                break;
            }
            if (subtable.encoding_id() == 1) {
                m_cmap.set_active_index(i);
                break;
            }
        }
    }

    dbg() << "Glyph ID for 'A': " << m_cmap.glyph_id_for_codepoint('A');
    dbg() << "Glyph ID for 'B': " << m_cmap.glyph_id_for_codepoint('B');
}

}
}
