/*
 * Copyright (c) 2020-2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Optional.h>
#include <LibWeb/CSS/BackdropFilter.h>
#include <LibWeb/CSS/Clip.h>
#include <LibWeb/CSS/LengthBox.h>
#include <LibWeb/CSS/Size.h>
#include <LibWeb/CSS/StyleValue.h>

namespace Web::CSS {

class InitialValues {
public:
    static float font_size() { return 16; }
    static int font_weight() { return 400; }
    static CSS::FontVariant font_variant() { return CSS::FontVariant::Normal; }
    static CSS::Float float_() { return CSS::Float::None; }
    static CSS::Clear clear() { return CSS::Clear::None; }
    static CSS::Clip clip() { return CSS::Clip::make_auto(); }
    static CSS::Cursor cursor() { return CSS::Cursor::Auto; }
    static CSS::WhiteSpace white_space() { return CSS::WhiteSpace::Normal; }
    static CSS::TextAlign text_align() { return CSS::TextAlign::Left; }
    static CSS::TextJustify text_justify() { return CSS::TextJustify::Auto; }
    static CSS::Position position() { return CSS::Position::Static; }
    static CSS::TextDecorationLine text_decoration_line() { return CSS::TextDecorationLine::None; }
    static CSS::Length text_decoration_thickness() { return Length::make_auto(); }
    static CSS::TextDecorationStyle text_decoration_style() { return CSS::TextDecorationStyle::Solid; }
    static CSS::TextTransform text_transform() { return CSS::TextTransform::None; }
    static CSS::Display display() { return CSS::Display { CSS::Display::Outside::Inline, CSS::Display::Inside::Flow }; }
    static Color color() { return Color::Black; }
    static CSS::BackdropFilter backdrop_filter() { return BackdropFilter::make_none(); }
    static Color background_color() { return Color::Transparent; }
    static CSS::ListStyleType list_style_type() { return CSS::ListStyleType::Disc; }
    static CSS::Visibility visibility() { return CSS::Visibility::Visible; }
    static CSS::FlexDirection flex_direction() { return CSS::FlexDirection::Row; }
    static CSS::FlexWrap flex_wrap() { return CSS::FlexWrap::Nowrap; }
    static CSS::ImageRendering image_rendering() { return CSS::ImageRendering::Auto; }
    static CSS::JustifyContent justify_content() { return CSS::JustifyContent::FlexStart; }
    static CSS::AlignItems align_items() { return CSS::AlignItems::Stretch; }
    static CSS::AlignSelf align_self() { return CSS::AlignSelf::Auto; }
    static CSS::Appearance appearance() { return CSS::Appearance::Auto; }
    static CSS::Overflow overflow() { return CSS::Overflow::Visible; }
    static CSS::BoxSizing box_sizing() { return CSS::BoxSizing::ContentBox; }
    static CSS::PointerEvents pointer_events() { return CSS::PointerEvents::Auto; }
    static float flex_grow() { return 0.0f; }
    static float flex_shrink() { return 1.0f; }
    static int order() { return 0; }
    static float opacity() { return 1.0f; }
    static CSS::Length border_radius() { return Length::make_px(0); }
    static Variant<CSS::VerticalAlign, CSS::LengthPercentage> vertical_align() { return CSS::VerticalAlign::Baseline; }
    static CSS::LengthBox inset() { return { CSS::Length::make_auto(), CSS::Length::make_auto(), CSS::Length::make_auto(), CSS::Length::make_auto() }; }
    static CSS::LengthBox margin() { return { CSS::Length::make_px(0), CSS::Length::make_px(0), CSS::Length::make_px(0), CSS::Length::make_px(0) }; }
    static CSS::LengthBox padding() { return { CSS::Length::make_px(0), CSS::Length::make_px(0), CSS::Length::make_px(0), CSS::Length::make_px(0) }; }
    static CSS::Size width() { return CSS::Size::make_auto(); }
    static CSS::Size min_width() { return CSS::Size::make_auto(); }
    static CSS::Size max_width() { return CSS::Size::make_none(); }
    static CSS::Size height() { return CSS::Size::make_auto(); }
    static CSS::Size min_height() { return CSS::Size::make_auto(); }
    static CSS::Size max_height() { return CSS::Size::make_none(); }
    static Vector<CSS::GridTrackSize> grid_template_columns() { return Vector<CSS::GridTrackSize> {}; }
    static Vector<CSS::GridTrackSize> grid_template_rows() { return Vector<CSS::GridTrackSize> {}; }
    static CSS::GridTrackPlacement grid_column_end() { return CSS::GridTrackPlacement::make_auto(); }
    static CSS::GridTrackPlacement grid_column_start() { return CSS::GridTrackPlacement::make_auto(); }
    static CSS::GridTrackPlacement grid_row_end() { return CSS::GridTrackPlacement::make_auto(); }
    static CSS::GridTrackPlacement grid_row_start() { return CSS::GridTrackPlacement::make_auto(); }
};

struct BackgroundLayerData {
    RefPtr<CSS::AbstractImageStyleValue> background_image { nullptr };
    CSS::BackgroundAttachment attachment { CSS::BackgroundAttachment::Scroll };
    CSS::BackgroundBox origin { CSS::BackgroundBox::PaddingBox };
    CSS::BackgroundBox clip { CSS::BackgroundBox::BorderBox };
    CSS::PositionEdge position_edge_x { CSS::PositionEdge::Left };
    CSS::LengthPercentage position_offset_x { CSS::Length::make_px(0) };
    CSS::PositionEdge position_edge_y { CSS::PositionEdge::Top };
    CSS::LengthPercentage position_offset_y { CSS::Length::make_px(0) };
    CSS::BackgroundSize size_type { CSS::BackgroundSize::LengthPercentage };
    CSS::LengthPercentage size_x { CSS::Length::make_auto() };
    CSS::LengthPercentage size_y { CSS::Length::make_auto() };
    CSS::Repeat repeat_x { CSS::Repeat::Repeat };
    CSS::Repeat repeat_y { CSS::Repeat::Repeat };
};

struct BorderData {
public:
    Color color { Color::Transparent };
    CSS::LineStyle line_style { CSS::LineStyle::None };
    float width { 0 };
};

using TransformValue = Variant<CSS::Angle, CSS::LengthPercentage, float>;

struct Transformation {
    CSS::TransformFunction function;
    Vector<TransformValue> values;
};

struct TransformOrigin {
    CSS::LengthPercentage x { Percentage(50) };
    CSS::LengthPercentage y { Percentage(50) };
};

struct FlexBasisData {
    CSS::FlexBasis type { CSS::FlexBasis::Auto };
    Optional<CSS::LengthPercentage> length_percentage;
};

struct ShadowData {
    Color color {};
    CSS::Length offset_x { Length::make_px(0) };
    CSS::Length offset_y { Length::make_px(0) };
    CSS::Length blur_radius { Length::make_px(0) };
    CSS::Length spread_distance { Length::make_px(0) };
    CSS::ShadowPlacement placement { CSS::ShadowPlacement::Outer };
};

struct ContentData {
    enum class Type {
        Normal,
        None,
        String,
    } type { Type::Normal };

    // FIXME: Data is a list of identifiers, strings and image values.
    String data {};
    String alt_text {};
};

struct BorderRadiusData {
    CSS::LengthPercentage horizontal_radius { InitialValues::border_radius() };
    CSS::LengthPercentage vertical_radius { InitialValues::border_radius() };
};

class ComputedValues {
public:
    CSS::Float float_() const { return m_noninherited.float_; }
    CSS::Clear clear() const { return m_noninherited.clear; }
    CSS::Clip clip() const { return m_noninherited.clip; }
    CSS::Cursor cursor() const { return m_inherited.cursor; }
    CSS::ContentData content() const { return m_noninherited.content; }
    CSS::PointerEvents pointer_events() const { return m_inherited.pointer_events; }
    CSS::Display display() const { return m_noninherited.display; }
    Optional<int> const& z_index() const { return m_noninherited.z_index; }
    CSS::TextAlign text_align() const { return m_inherited.text_align; }
    CSS::TextJustify text_justify() const { return m_inherited.text_justify; }
    Vector<CSS::TextDecorationLine> const& text_decoration_line() const { return m_noninherited.text_decoration_line; }
    CSS::LengthPercentage const& text_decoration_thickness() const { return m_noninherited.text_decoration_thickness; }
    CSS::TextDecorationStyle text_decoration_style() const { return m_noninherited.text_decoration_style; }
    Color text_decoration_color() const { return m_noninherited.text_decoration_color; }
    CSS::TextTransform text_transform() const { return m_inherited.text_transform; }
    Vector<ShadowData> const& text_shadow() const { return m_noninherited.text_shadow; }
    CSS::Position position() const { return m_noninherited.position; }
    CSS::WhiteSpace white_space() const { return m_inherited.white_space; }
    CSS::FlexDirection flex_direction() const { return m_noninherited.flex_direction; }
    CSS::FlexWrap flex_wrap() const { return m_noninherited.flex_wrap; }
    FlexBasisData const& flex_basis() const { return m_noninherited.flex_basis; }
    float flex_grow() const { return m_noninherited.flex_grow; }
    float flex_shrink() const { return m_noninherited.flex_shrink; }
    int order() const { return m_noninherited.order; }
    CSS::AlignItems align_items() const { return m_noninherited.align_items; }
    CSS::AlignSelf align_self() const { return m_noninherited.align_self; }
    CSS::Appearance appearance() const { return m_noninherited.appearance; }
    float opacity() const { return m_noninherited.opacity; }
    CSS::Visibility visibility() const { return m_inherited.visibility; }
    CSS::ImageRendering image_rendering() const { return m_inherited.image_rendering; }
    CSS::JustifyContent justify_content() const { return m_noninherited.justify_content; }
    CSS::BackdropFilter const& backdrop_filter() const { return m_noninherited.backdrop_filter; }
    Vector<ShadowData> const& box_shadow() const { return m_noninherited.box_shadow; }
    CSS::BoxSizing box_sizing() const { return m_noninherited.box_sizing; }
    CSS::Size const& width() const { return m_noninherited.width; }
    CSS::Size const& min_width() const { return m_noninherited.min_width; }
    CSS::Size const& max_width() const { return m_noninherited.max_width; }
    CSS::Size const& height() const { return m_noninherited.height; }
    CSS::Size const& min_height() const { return m_noninherited.min_height; }
    CSS::Size const& max_height() const { return m_noninherited.max_height; }
    Variant<CSS::VerticalAlign, CSS::LengthPercentage> const& vertical_align() const { return m_noninherited.vertical_align; }
    Vector<CSS::GridTrackSize> const& grid_template_columns() const { return m_noninherited.grid_template_columns; }
    Vector<CSS::GridTrackSize> const& grid_template_rows() const { return m_noninherited.grid_template_rows; }
    CSS::GridTrackPlacement const& grid_column_end() const { return m_noninherited.grid_column_end; }
    CSS::GridTrackPlacement const& grid_column_start() const { return m_noninherited.grid_column_start; }
    CSS::GridTrackPlacement const& grid_row_end() const { return m_noninherited.grid_row_end; }
    CSS::GridTrackPlacement const& grid_row_start() const { return m_noninherited.grid_row_start; }

    CSS::LengthBox const& inset() const { return m_noninherited.inset; }
    const CSS::LengthBox& margin() const { return m_noninherited.margin; }
    const CSS::LengthBox& padding() const { return m_noninherited.padding; }

    BorderData const& border_left() const { return m_noninherited.border_left; }
    BorderData const& border_top() const { return m_noninherited.border_top; }
    BorderData const& border_right() const { return m_noninherited.border_right; }
    BorderData const& border_bottom() const { return m_noninherited.border_bottom; }

    const CSS::BorderRadiusData& border_bottom_left_radius() const { return m_noninherited.border_bottom_left_radius; }
    const CSS::BorderRadiusData& border_bottom_right_radius() const { return m_noninherited.border_bottom_right_radius; }
    const CSS::BorderRadiusData& border_top_left_radius() const { return m_noninherited.border_top_left_radius; }
    const CSS::BorderRadiusData& border_top_right_radius() const { return m_noninherited.border_top_right_radius; }

    CSS::Overflow overflow_x() const { return m_noninherited.overflow_x; }
    CSS::Overflow overflow_y() const { return m_noninherited.overflow_y; }

    Color color() const { return m_inherited.color; }
    Color background_color() const { return m_noninherited.background_color; }
    Vector<BackgroundLayerData> const& background_layers() const { return m_noninherited.background_layers; }

    CSS::ListStyleType list_style_type() const { return m_inherited.list_style_type; }

    Optional<Color> const& fill() const { return m_inherited.fill; }
    Optional<Color> const& stroke() const { return m_inherited.stroke; }
    Optional<LengthPercentage> const& stroke_width() const { return m_inherited.stroke_width; }

    Vector<CSS::Transformation> const& transformations() const { return m_noninherited.transformations; }
    CSS::TransformOrigin const& transform_origin() const { return m_noninherited.transform_origin; }

    float font_size() const { return m_inherited.font_size; }
    int font_weight() const { return m_inherited.font_weight; }
    CSS::FontVariant font_variant() const { return m_inherited.font_variant; }

    ComputedValues clone_inherited_values() const
    {
        ComputedValues clone;
        clone.m_inherited = m_inherited;
        return clone;
    }

protected:
    struct {
        float font_size { InitialValues::font_size() };
        int font_weight { InitialValues::font_weight() };
        CSS::FontVariant font_variant { InitialValues::font_variant() };
        Color color { InitialValues::color() };
        CSS::Cursor cursor { InitialValues::cursor() };
        CSS::ImageRendering image_rendering { InitialValues::image_rendering() };
        CSS::PointerEvents pointer_events { InitialValues::pointer_events() };
        CSS::TextAlign text_align { InitialValues::text_align() };
        CSS::TextJustify text_justify { InitialValues::text_justify() };
        CSS::TextTransform text_transform { InitialValues::text_transform() };
        CSS::WhiteSpace white_space { InitialValues::white_space() };
        CSS::ListStyleType list_style_type { InitialValues::list_style_type() };
        CSS::Visibility visibility { InitialValues::visibility() };

        Optional<Color> fill;
        Optional<Color> stroke;
        Optional<LengthPercentage> stroke_width;
    } m_inherited;

    struct {
        CSS::Float float_ { InitialValues::float_() };
        CSS::Clear clear { InitialValues::clear() };
        CSS::Clip clip { InitialValues::clip() };
        CSS::Display display { InitialValues::display() };
        Optional<int> z_index;
        // FIXME: Store this as flags in a u8.
        Vector<CSS::TextDecorationLine> text_decoration_line { InitialValues::text_decoration_line() };
        CSS::LengthPercentage text_decoration_thickness { InitialValues::text_decoration_thickness() };
        CSS::TextDecorationStyle text_decoration_style { InitialValues::text_decoration_style() };
        Color text_decoration_color { InitialValues::color() };
        Vector<ShadowData> text_shadow {};
        CSS::Position position { InitialValues::position() };
        CSS::Size width { InitialValues::width() };
        CSS::Size min_width { InitialValues::min_width() };
        CSS::Size max_width { InitialValues::max_width() };
        CSS::Size height { InitialValues::height() };
        CSS::Size min_height { InitialValues::min_height() };
        CSS::Size max_height { InitialValues::max_height() };
        CSS::LengthBox inset { InitialValues::inset() };
        CSS::LengthBox margin { InitialValues::margin() };
        CSS::LengthBox padding { InitialValues::padding() };
        CSS::BackdropFilter backdrop_filter { InitialValues::backdrop_filter() };
        BorderData border_left;
        BorderData border_top;
        BorderData border_right;
        BorderData border_bottom;
        BorderRadiusData border_bottom_left_radius;
        BorderRadiusData border_bottom_right_radius;
        BorderRadiusData border_top_left_radius;
        BorderRadiusData border_top_right_radius;
        Color background_color { InitialValues::background_color() };
        Vector<BackgroundLayerData> background_layers;
        CSS::FlexDirection flex_direction { InitialValues::flex_direction() };
        CSS::FlexWrap flex_wrap { InitialValues::flex_wrap() };
        CSS::FlexBasisData flex_basis {};
        float flex_grow { InitialValues::flex_grow() };
        float flex_shrink { InitialValues::flex_shrink() };
        int order { InitialValues::order() };
        CSS::AlignItems align_items { InitialValues::align_items() };
        CSS::AlignSelf align_self { InitialValues::align_self() };
        CSS::Appearance appearance { InitialValues::appearance() };
        CSS::JustifyContent justify_content { InitialValues::justify_content() };
        CSS::Overflow overflow_x { InitialValues::overflow() };
        CSS::Overflow overflow_y { InitialValues::overflow() };
        float opacity { InitialValues::opacity() };
        Vector<ShadowData> box_shadow {};
        Vector<CSS::Transformation> transformations {};
        CSS::TransformOrigin transform_origin {};
        CSS::BoxSizing box_sizing { InitialValues::box_sizing() };
        CSS::ContentData content;
        Variant<CSS::VerticalAlign, CSS::LengthPercentage> vertical_align { InitialValues::vertical_align() };
        Vector<CSS::GridTrackSize> grid_template_columns;
        Vector<CSS::GridTrackSize> grid_template_rows;
        CSS::GridTrackPlacement grid_column_end { InitialValues::grid_column_end() };
        CSS::GridTrackPlacement grid_column_start { InitialValues::grid_column_start() };
        CSS::GridTrackPlacement grid_row_end { InitialValues::grid_row_end() };
        CSS::GridTrackPlacement grid_row_start { InitialValues::grid_row_start() };
    } m_noninherited;
};

class ImmutableComputedValues final : public ComputedValues {
};

class MutableComputedValues final : public ComputedValues {
public:
    void set_font_size(float font_size) { m_inherited.font_size = font_size; }
    void set_font_weight(int font_weight) { m_inherited.font_weight = font_weight; }
    void set_font_variant(CSS::FontVariant font_variant) { m_inherited.font_variant = font_variant; }
    void set_color(Color const& color) { m_inherited.color = color; }
    void set_clip(CSS::Clip const& clip) { m_noninherited.clip = clip; }
    void set_content(ContentData const& content) { m_noninherited.content = content; }
    void set_cursor(CSS::Cursor cursor) { m_inherited.cursor = cursor; }
    void set_image_rendering(CSS::ImageRendering value) { m_inherited.image_rendering = value; }
    void set_pointer_events(CSS::PointerEvents value) { m_inherited.pointer_events = value; }
    void set_background_color(Color const& color) { m_noninherited.background_color = color; }
    void set_background_layers(Vector<BackgroundLayerData>&& layers) { m_noninherited.background_layers = move(layers); }
    void set_float(CSS::Float value) { m_noninherited.float_ = value; }
    void set_clear(CSS::Clear value) { m_noninherited.clear = value; }
    void set_z_index(Optional<int> value) { m_noninherited.z_index = value; }
    void set_text_align(CSS::TextAlign text_align) { m_inherited.text_align = text_align; }
    void set_text_justify(CSS::TextJustify text_justify) { m_inherited.text_justify = text_justify; }
    void set_text_decoration_line(Vector<CSS::TextDecorationLine> value) { m_noninherited.text_decoration_line = move(value); }
    void set_text_decoration_thickness(CSS::LengthPercentage value) { m_noninherited.text_decoration_thickness = move(value); }
    void set_text_decoration_style(CSS::TextDecorationStyle value) { m_noninherited.text_decoration_style = value; }
    void set_text_decoration_color(Color value) { m_noninherited.text_decoration_color = value; }
    void set_text_transform(CSS::TextTransform value) { m_inherited.text_transform = value; }
    void set_text_shadow(Vector<ShadowData>&& value) { m_noninherited.text_shadow = move(value); }
    void set_position(CSS::Position position) { m_noninherited.position = position; }
    void set_white_space(CSS::WhiteSpace value) { m_inherited.white_space = value; }
    void set_width(CSS::Size const& width) { m_noninherited.width = width; }
    void set_min_width(CSS::Size const& width) { m_noninherited.min_width = width; }
    void set_max_width(CSS::Size const& width) { m_noninherited.max_width = width; }
    void set_height(CSS::Size const& height) { m_noninherited.height = height; }
    void set_min_height(CSS::Size const& height) { m_noninherited.min_height = height; }
    void set_max_height(CSS::Size const& height) { m_noninherited.max_height = height; }
    void set_inset(CSS::LengthBox const& inset) { m_noninherited.inset = inset; }
    void set_margin(const CSS::LengthBox& margin) { m_noninherited.margin = margin; }
    void set_padding(const CSS::LengthBox& padding) { m_noninherited.padding = padding; }
    void set_overflow_x(CSS::Overflow value) { m_noninherited.overflow_x = value; }
    void set_overflow_y(CSS::Overflow value) { m_noninherited.overflow_y = value; }
    void set_list_style_type(CSS::ListStyleType value) { m_inherited.list_style_type = value; }
    void set_display(CSS::Display value) { m_noninherited.display = value; }
    void set_backdrop_filter(CSS::BackdropFilter backdrop_filter) { m_noninherited.backdrop_filter = move(backdrop_filter); }
    void set_border_bottom_left_radius(CSS::BorderRadiusData value) { m_noninherited.border_bottom_left_radius = move(value); }
    void set_border_bottom_right_radius(CSS::BorderRadiusData value) { m_noninherited.border_bottom_right_radius = move(value); }
    void set_border_top_left_radius(CSS::BorderRadiusData value) { m_noninherited.border_top_left_radius = move(value); }
    void set_border_top_right_radius(CSS::BorderRadiusData value) { m_noninherited.border_top_right_radius = move(value); }
    BorderData& border_left() { return m_noninherited.border_left; }
    BorderData& border_top() { return m_noninherited.border_top; }
    BorderData& border_right() { return m_noninherited.border_right; }
    BorderData& border_bottom() { return m_noninherited.border_bottom; }
    void set_flex_direction(CSS::FlexDirection value) { m_noninherited.flex_direction = value; }
    void set_flex_wrap(CSS::FlexWrap value) { m_noninherited.flex_wrap = value; }
    void set_flex_basis(FlexBasisData value) { m_noninherited.flex_basis = move(value); }
    void set_flex_grow(float value) { m_noninherited.flex_grow = value; }
    void set_flex_shrink(float value) { m_noninherited.flex_shrink = value; }
    void set_order(int value) { m_noninherited.order = value; }
    void set_align_items(CSS::AlignItems value) { m_noninherited.align_items = value; }
    void set_align_self(CSS::AlignSelf value) { m_noninherited.align_self = value; }
    void set_appearance(CSS::Appearance value) { m_noninherited.appearance = value; }
    void set_opacity(float value) { m_noninherited.opacity = value; }
    void set_justify_content(CSS::JustifyContent value) { m_noninherited.justify_content = value; }
    void set_box_shadow(Vector<ShadowData>&& value) { m_noninherited.box_shadow = move(value); }
    void set_transformations(Vector<CSS::Transformation> value) { m_noninherited.transformations = move(value); }
    void set_transform_origin(CSS::TransformOrigin value) { m_noninherited.transform_origin = value; }
    void set_box_sizing(CSS::BoxSizing value) { m_noninherited.box_sizing = value; }
    void set_vertical_align(Variant<CSS::VerticalAlign, CSS::LengthPercentage> value) { m_noninherited.vertical_align = move(value); }
    void set_visibility(CSS::Visibility value) { m_inherited.visibility = value; }
    void set_grid_template_columns(Vector<CSS::GridTrackSize> value) { m_noninherited.grid_template_columns = move(value); }
    void set_grid_template_rows(Vector<CSS::GridTrackSize> value) { m_noninherited.grid_template_rows = move(value); }
    void set_grid_column_end(CSS::GridTrackPlacement value) { m_noninherited.grid_column_end = value; }
    void set_grid_column_start(CSS::GridTrackPlacement value) { m_noninherited.grid_column_start = value; }
    void set_grid_row_end(CSS::GridTrackPlacement value) { m_noninherited.grid_row_end = value; }
    void set_grid_row_start(CSS::GridTrackPlacement value) { m_noninherited.grid_row_start = value; }

    void set_fill(Color value) { m_inherited.fill = value; }
    void set_stroke(Color value) { m_inherited.stroke = value; }
    void set_stroke_width(LengthPercentage value) { m_inherited.stroke_width = value; }
};

}
