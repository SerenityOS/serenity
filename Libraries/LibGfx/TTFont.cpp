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
#include <AK/FixedArray.h>
#include <AK/LogStream.h>
#include <AK/Utf8View.h>
#include <AK/Utf32View.h>
#include <bits/stdint.h>
#include <LibCore/File.h>
#include <LibGfx/FloatPoint.h>
#include <LibGfx/Path.h>
#include <math.h>

namespace Gfx {
namespace TTF {

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
    return be_u16(m_slice.offset_pointer((u32) Offsets::UnitsPerEM));
}

i16 Font::Head::xmin() const
{
    return be_i16(m_slice.offset_pointer((u32) Offsets::XMin));
}

i16 Font::Head::ymin() const
{
    return be_i16(m_slice.offset_pointer((u32) Offsets::YMin));
}

i16 Font::Head::xmax() const
{
    return be_i16(m_slice.offset_pointer((u32) Offsets::XMax));
}

i16 Font::Head::ymax() const
{
    return be_i16(m_slice.offset_pointer((u32) Offsets::YMax));
}

u16 Font::Head::lowest_recommended_ppem() const
{
    return be_u16(m_slice.offset_pointer((u32) Offsets::LowestRecPPEM));
}

Font::IndexToLocFormat Font::Head::index_to_loc_format() const
{
    i16 raw = be_i16(m_slice.offset_pointer((u32) Offsets::IndexToLocFormat));
    switch (raw) {
    case 0:
        return IndexToLocFormat::Offset16;
    case 1:
        return IndexToLocFormat::Offset32;
    default:
        ASSERT_NOT_REACHED();
    }
}

i16 Font::Hhea::ascender() const
{
    return be_i16(m_slice.offset_pointer((u32) Offsets::Ascender));
}

i16 Font::Hhea::descender() const
{
    return be_i16(m_slice.offset_pointer((u32) Offsets::Descender));
}

i16 Font::Hhea::line_gap() const
{
    return be_i16(m_slice.offset_pointer((u32) Offsets::LineGap));
}

u16 Font::Hhea::advance_width_max() const
{
    return be_u16(m_slice.offset_pointer((u32) Offsets::AdvanceWidthMax));
}

u16 Font::Hhea::number_of_h_metrics() const
{
    return be_u16(m_slice.offset_pointer((u32) Offsets::NumberOfHMetrics));
}

u16 Font::Maxp::num_glyphs() const
{
    return be_u16(m_slice.offset_pointer((u32) Offsets::NumGlyphs));
}

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
        FloatPoint point;
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
    FloatPoint m_last_point = { 0.0f, 0.0f };
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
        , m_data(m_size.width() * m_size.height())
    {
        for (int i = 0; i < m_size.width() * m_size.height(); i++) {
            m_data[i] = 0.0;
        }
    }

    RefPtr<Bitmap> draw_path(Path& path)
    {
        for (auto& line : path.split_lines()) {
            draw_line(line.from, line.to);
        }
        return accumulate();
    }

private:
    RefPtr<Bitmap> accumulate()
    {
        auto bitmap = Bitmap::create(BitmapFormat::RGBA32, m_size);
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

    void draw_line(FloatPoint p0, FloatPoint p1)
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

    Size m_size;
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
    return be_u16(m_slice.offset_pointer((u32) Offsets::NumTables));
}

Optional<Font::Cmap::Subtable> Font::Cmap::subtable(u32 index) const
{
    if (index >= num_subtables()) {
        return {};
    }
    u32 record_offset = (u32) Sizes::TableHeader + index * (u32) Sizes::EncodingRecord;
    u16 platform_id = be_u16(m_slice.offset_pointer(record_offset));
    u16 encoding_id = be_u16(m_slice.offset_pointer(record_offset + (u32) Offsets::EncodingRecord_EncodingID));
    u32 subtable_offset = be_u32(m_slice.offset_pointer(record_offset + (u32) Offsets::EncodingRecord_Offset));
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
    u32 segcount_x2 = be_u16(m_slice.offset_pointer((u32) Table4Offsets::SegCountX2));
    if (m_slice.size() < segcount_x2 * (u32) Table4Sizes::NonConstMultiplier + (u32) Table4Sizes::Constant) {
        return 0;
    }
    for (u32 offset = 0; offset < segcount_x2; offset += 2) {
        u32 end_codepoint = be_u16(m_slice.offset_pointer((u32) Table4Offsets::EndConstBase + offset));
        if (codepoint > end_codepoint) {
            continue;
        }
        u32 start_codepoint = be_u16(m_slice.offset_pointer((u32) Table4Offsets::StartConstBase + segcount_x2 + offset));
        if (codepoint < start_codepoint) {
            break;
        }
        u32 delta = be_u16(m_slice.offset_pointer((u32) Table4Offsets::DeltaConstBase + segcount_x2 * 2 + offset));
        u32 range = be_u16(m_slice.offset_pointer((u32) Table4Offsets::RangeConstBase + segcount_x2 * 3 + offset));
        if (range == 0) {
            return (codepoint + delta) & 0xffff;
        }
        u32 glyph_offset = (u32) Table4Offsets::GlyphOffsetConstBase + segcount_x2 * 3 + offset + range + (codepoint - start_codepoint) * 2;
        ASSERT(glyph_offset + 2 <= m_slice.size());
        return (be_u16(m_slice.offset_pointer(glyph_offset)) + delta) & 0xffff;
    }
    return 0;
}

u32 Font::Cmap::Subtable::glyph_id_for_codepoint_table_12(u32 codepoint) const
{
    u32 num_groups = be_u32(m_slice.offset_pointer((u32) Table12Offsets::NumGroups));
    ASSERT(m_slice.size() >= (u32) Table12Sizes::Header + (u32) Table12Sizes::Record * num_groups);
    for (u32 offset = 0; offset < num_groups * (u32) Table12Sizes::Record; offset += (u32) Table12Sizes::Record) {
        u32 start_codepoint = be_u32(m_slice.offset_pointer((u32) Table12Offsets::Record_StartCode + offset));
        if (codepoint < start_codepoint) {
            break;
        }
        u32 end_codepoint = be_u32(m_slice.offset_pointer((u32) Table12Offsets::Record_EndCode + offset));
        if (codepoint > end_codepoint) {
            continue;
        }
        u32 glyph_offset = be_u32(m_slice.offset_pointer((u32) Table12Offsets::Record_StartGlyph + offset));
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

RefPtr<Bitmap> Font::Glyf::Glyph::raster(float x_scale, float y_scale) const
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

RefPtr<Bitmap> Font::Glyf::Glyph::raster_simple(float x_scale, float y_scale) const
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
    Path path;
    PointIterator point_iterator(m_slice, num_points, flags_offset, x_offset, y_offset, -simple.xmin, -simple.ymax, x_scale, -y_scale);

    int last_contour_end = -1;
    u32 contour_index = 0;
    u32 contour_size = 0;
    Optional<FloatPoint> contour_start = {};
    Optional<FloatPoint> last_offcurve_point = {};

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
                        auto mid_point = FloatPoint::interpolate(item.point, next_item.point, 0.5);
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
                    auto mid_point = FloatPoint::interpolate(point0, item.point, 0.5);
                    path.quadratic_bezier_curve_to(point0, mid_point);
                    last_offcurve_point = item.point;
                }
            } else {
                path.quadratic_bezier_curve_to(point0, contour_start.value());
                contour_start = {};
            }
        }
    }

    return Rasterizer(Size(width, height)).draw_path(path);
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

RefPtr<Font> Font::load_from_file(const StringView& path, unsigned index)
{
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
        if (buffer.size() < (u32) Sizes::TTCHeaderV1 + sizeof(u32) * (index + 1)) {
            dbg() << "Font file too small";
            return nullptr;
        }
        u32 offset = be_u32(buffer.offset_pointer((u32) Sizes::TTCHeaderV1 + sizeof(u32) * index));
        return adopt(*new Font(move(buffer), offset));
    }
    if (tag == tag_from_str("OTTO")) {
        dbg() << "CFF fonts not supported yet";
        return nullptr;
    }
    if (tag != 0x00010000) {
        dbg() << "Not a valid  font";
        return nullptr;
    }
    return adopt(*new Font(move(buffer), 0));
}

// FIXME: "loca" and "glyf" are not available for CFF fonts.
Font::Font(ByteBuffer&& buffer, u32 offset)
    : m_buffer(move(buffer))
{
    ASSERT(m_buffer.size() >= offset + (u32) Sizes::OffsetTable);
    Optional<ByteBuffer> head_slice = {};
    Optional<ByteBuffer> hhea_slice = {};
    Optional<ByteBuffer> maxp_slice = {};
    Optional<ByteBuffer> hmtx_slice = {};
    Optional<ByteBuffer> cmap_slice = {};
    Optional<ByteBuffer> loca_slice = {};
    Optional<ByteBuffer> glyf_slice = {};

    //auto sfnt_version = be_u32(data + offset);
    auto num_tables = be_u16(m_buffer.offset_pointer(offset + (u32) Offsets::NumTables));
    ASSERT(m_buffer.size() >= offset + (u32) Sizes::OffsetTable + num_tables * (u32) Sizes::TableRecord);

    for (auto i = 0; i < num_tables; i++) {
        u32 record_offset = offset + (u32) Sizes::OffsetTable + i * (u32) Sizes::TableRecord;
        u32 tag = be_u32(m_buffer.offset_pointer(record_offset));
        u32 table_offset = be_u32(m_buffer.offset_pointer(record_offset + (u32) Offsets::TableRecord_Offset));
        u32 table_length = be_u32(m_buffer.offset_pointer(record_offset + (u32) Offsets::TableRecord_Length));
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
            if (subtable.encoding_id() == (u16) Cmap::Subtable::WindowsEncoding::UnicodeFullRepertoire) {
                m_cmap.set_active_index(i);
                break;
            }
            if (subtable.encoding_id() == (u16) Cmap::Subtable::WindowsEncoding::UnicodeBMP) {
                m_cmap.set_active_index(i);
                break;
            }
        }
    }
}

ScaledFontMetrics Font::metrics(float x_scale, float y_scale) const
{
    auto ascender = m_hhea.ascender() * y_scale;
    auto descender = m_hhea.descender() * y_scale;
    auto line_gap = m_hhea.line_gap() * y_scale;
    auto advance_width_max = m_hhea.advance_width_max() * x_scale;
    return ScaledFontMetrics {
        .ascender = (int) roundf(ascender),
        .descender = (int) roundf(descender),
        .line_gap = (int) roundf(line_gap),
        .advance_width_max = (int) roundf(advance_width_max),
    };
}

ScaledGlyphMetrics Font::glyph_metrics(u32 glyph_id, float x_scale, float y_scale) const
{
    if (glyph_id >= m_maxp.num_glyphs()) {
        glyph_id = 0;
    }
    auto horizontal_metrics = m_hmtx.get_glyph_horizontal_metrics(glyph_id);
    auto glyph_offset = m_loca.get_glyph_offset(glyph_id);
    auto glyph = m_glyf.glyph(glyph_offset);
    int ascender = glyph.ascender();
    int descender = glyph.descender();
    return ScaledGlyphMetrics {
        .ascender = (int) roundf(ascender * y_scale),
        .descender = (int) roundf(descender * y_scale),
        .advance_width = (int) roundf(horizontal_metrics.advance_width * x_scale),
        .left_side_bearing = (int) roundf(horizontal_metrics.left_side_bearing * x_scale),
    };
}

// FIXME: "loca" and "glyf" are not available for CFF fonts.
RefPtr<Bitmap> Font::raster_glyph(u32 glyph_id, float x_scale, float y_scale) const
{
    if (glyph_id >= m_maxp.num_glyphs()) {
        glyph_id = 0;
    }
    auto glyph_offset = m_loca.get_glyph_offset(glyph_id);
    auto glyph = m_glyf.glyph(glyph_offset);
    return glyph.raster(x_scale, y_scale);
}

int ScaledFont::width(const StringView& string) const
{
    Utf8View utf8 { string };
    return width(utf8);
}

int ScaledFont::width(const Utf8View& utf8) const
{
    int width = 0;
    for (u32 codepoint : utf8) {
        u32 glyph_id = glyph_id_for_codepoint(codepoint);
        auto metrics = glyph_metrics(glyph_id);
        width += metrics.advance_width;
    }
    return width;
}

int ScaledFont::width(const Utf32View& utf32) const
{
    int width = 0;
    for (size_t i = 0; i < utf32.length(); i++) {
        u32 glyph_id = glyph_id_for_codepoint(utf32.codepoints()[i]);
        auto metrics = glyph_metrics(glyph_id);
        width += metrics.advance_width;
    }
    return width;
}

}
}
