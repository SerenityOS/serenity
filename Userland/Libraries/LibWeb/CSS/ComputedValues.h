/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Optional.h>
#include <LibWeb/CSS/LengthBox.h>
#include <LibWeb/CSS/StyleValue.h>

namespace Web::CSS {

class InitialValues {
public:
    static CSS::Float float_() { return CSS::Float::None; }
    static CSS::Clear clear() { return CSS::Clear::None; }
    static CSS::Cursor cursor() { return CSS::Cursor::Auto; }
    static CSS::WhiteSpace white_space() { return CSS::WhiteSpace::Normal; }
    static CSS::TextAlign text_align() { return CSS::TextAlign::Left; }
    static CSS::Position position() { return CSS::Position::Static; }
    static CSS::TextDecorationLine text_decoration_line() { return CSS::TextDecorationLine::None; }
    static CSS::TextDecorationStyle text_decoration_style() { return CSS::TextDecorationStyle::Solid; }
    static CSS::TextTransform text_transform() { return CSS::TextTransform::None; }
    static CSS::Display display() { return CSS::Display { CSS::Display::Outside::Inline, CSS::Display::Inside::Flow }; }
    static Color color() { return Color::Black; }
    static Color background_color() { return Color::Transparent; }
    static CSS::ListStyleType list_style_type() { return CSS::ListStyleType::Disc; }
    static CSS::FlexDirection flex_direction() { return CSS::FlexDirection::Row; }
    static CSS::FlexWrap flex_wrap() { return CSS::FlexWrap::Nowrap; }
    static CSS::JustifyContent justify_content() { return CSS::JustifyContent::FlexStart; }
    static CSS::AlignItems align_items() { return CSS::AlignItems::Stretch; }
    static CSS::Overflow overflow() { return CSS::Overflow::Visible; }
    static CSS::BoxSizing box_sizing() { return CSS::BoxSizing::ContentBox; }
    static CSS::PointerEvents pointer_events() { return CSS::PointerEvents::Auto; }
    static float flex_grow() { return 0.0f; }
    static float flex_shrink() { return 1.0f; }
    static float opacity() { return 1.0f; }
    static CSS::Length border_radius() { return Length::make_px(0); }
};

struct BackgroundLayerData {
    RefPtr<CSS::ImageStyleValue> image { nullptr };
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

struct Transformation {
    CSS::TransformFunction function;
    Vector<Variant<CSS::Length, float>> values;
};

struct FlexBasisData {
    CSS::FlexBasis type { CSS::FlexBasis::Auto };
    Optional<CSS::LengthPercentage> length_percentage;

    bool is_definite() const { return type == CSS::FlexBasis::LengthPercentage; }
};

struct BoxShadowData {
    CSS::Length offset_x {};
    CSS::Length offset_y {};
    CSS::Length blur_radius {};
    Color color {};
};

class ComputedValues {
public:
    CSS::Float float_() const { return m_noninherited.float_; }
    CSS::Clear clear() const { return m_noninherited.clear; }
    CSS::Cursor cursor() const { return m_inherited.cursor; }
    CSS::PointerEvents pointer_events() const { return m_inherited.pointer_events; }
    CSS::Display display() const { return m_noninherited.display; }
    Optional<int> const& z_index() const { return m_noninherited.z_index; }
    CSS::TextAlign text_align() const { return m_inherited.text_align; }
    CSS::TextDecorationLine text_decoration_line() const { return m_noninherited.text_decoration_line; }
    CSS::TextDecorationStyle text_decoration_style() const { return m_noninherited.text_decoration_style; }
    CSS::TextTransform text_transform() const { return m_inherited.text_transform; }
    CSS::Position position() const { return m_noninherited.position; }
    CSS::WhiteSpace white_space() const { return m_inherited.white_space; }
    CSS::FlexDirection flex_direction() const { return m_noninherited.flex_direction; }
    CSS::FlexWrap flex_wrap() const { return m_noninherited.flex_wrap; }
    FlexBasisData const& flex_basis() const { return m_noninherited.flex_basis; }
    float flex_grow() const { return m_noninherited.flex_grow; }
    float flex_shrink() const { return m_noninherited.flex_shrink; }
    CSS::AlignItems align_items() const { return m_noninherited.align_items; }
    float opacity() const { return m_noninherited.opacity; }
    CSS::JustifyContent justify_content() const { return m_noninherited.justify_content; }
    Optional<BoxShadowData> const& box_shadow() const { return m_noninherited.box_shadow; }
    CSS::BoxSizing box_sizing() const { return m_noninherited.box_sizing; }
    CSS::LengthPercentage const& width() const { return m_noninherited.width; }
    CSS::LengthPercentage const& min_width() const { return m_noninherited.min_width; }
    CSS::LengthPercentage const& max_width() const { return m_noninherited.max_width; }
    CSS::LengthPercentage const& height() const { return m_noninherited.height; }
    CSS::LengthPercentage const& min_height() const { return m_noninherited.min_height; }
    CSS::LengthPercentage const& max_height() const { return m_noninherited.max_height; }

    const CSS::LengthBox& offset() const { return m_noninherited.offset; }
    const CSS::LengthBox& margin() const { return m_noninherited.margin; }
    const CSS::LengthBox& padding() const { return m_noninherited.padding; }

    const BorderData& border_left() const { return m_noninherited.border_left; }
    const BorderData& border_top() const { return m_noninherited.border_top; }
    const BorderData& border_right() const { return m_noninherited.border_right; }
    const BorderData& border_bottom() const { return m_noninherited.border_bottom; }

    const CSS::LengthPercentage& border_bottom_left_radius() const { return m_noninherited.border_bottom_left_radius; }
    const CSS::LengthPercentage& border_bottom_right_radius() const { return m_noninherited.border_bottom_right_radius; }
    const CSS::LengthPercentage& border_top_left_radius() const { return m_noninherited.border_top_left_radius; }
    const CSS::LengthPercentage& border_top_right_radius() const { return m_noninherited.border_top_right_radius; }

    CSS::Overflow overflow_x() const { return m_noninherited.overflow_x; }
    CSS::Overflow overflow_y() const { return m_noninherited.overflow_y; }

    Color color() const { return m_inherited.color; }
    Color background_color() const { return m_noninherited.background_color; }
    Vector<BackgroundLayerData> const& background_layers() const { return m_noninherited.background_layers; }

    CSS::ListStyleType list_style_type() const { return m_inherited.list_style_type; }

    Optional<Color> fill() const { return m_inherited.fill; }
    Optional<Color> stroke() const { return m_inherited.stroke; }
    Optional<LengthPercentage> const& stroke_width() const { return m_inherited.stroke_width; }

    Vector<CSS::Transformation> transformations() const { return m_noninherited.transformations; }

    ComputedValues clone_inherited_values() const
    {
        ComputedValues clone;
        clone.m_inherited = m_inherited;
        return clone;
    }

protected:
    struct {
        Color color { InitialValues::color() };
        CSS::Cursor cursor { InitialValues::cursor() };
        CSS::PointerEvents pointer_events { InitialValues::pointer_events() };
        CSS::TextAlign text_align { InitialValues::text_align() };
        CSS::TextTransform text_transform { InitialValues::text_transform() };
        CSS::WhiteSpace white_space { InitialValues::white_space() };
        CSS::ListStyleType list_style_type { InitialValues::list_style_type() };

        Optional<Color> fill;
        Optional<Color> stroke;
        Optional<LengthPercentage> stroke_width;
    } m_inherited;

    struct {
        CSS::Float float_ { InitialValues::float_() };
        CSS::Clear clear { InitialValues::clear() };
        CSS::Display display { InitialValues::display() };
        Optional<int> z_index;
        CSS::TextDecorationLine text_decoration_line { InitialValues::text_decoration_line() };
        CSS::TextDecorationStyle text_decoration_style { InitialValues::text_decoration_style() };
        CSS::Position position { InitialValues::position() };
        CSS::LengthPercentage width { Length::make_auto() };
        CSS::LengthPercentage min_width { Length::make_auto() };
        CSS::LengthPercentage max_width { Length::make_auto() };
        CSS::LengthPercentage height { Length::make_auto() };
        CSS::LengthPercentage min_height { Length::make_auto() };
        CSS::LengthPercentage max_height { Length::make_auto() };
        CSS::LengthBox offset;
        CSS::LengthBox margin;
        CSS::LengthBox padding;
        BorderData border_left;
        BorderData border_top;
        BorderData border_right;
        BorderData border_bottom;
        LengthPercentage border_bottom_left_radius { InitialValues::border_radius() };
        LengthPercentage border_bottom_right_radius { InitialValues::border_radius() };
        LengthPercentage border_top_left_radius { InitialValues::border_radius() };
        LengthPercentage border_top_right_radius { InitialValues::border_radius() };
        Color background_color { InitialValues::background_color() };
        Vector<BackgroundLayerData> background_layers;
        CSS::FlexDirection flex_direction { InitialValues::flex_direction() };
        CSS::FlexWrap flex_wrap { InitialValues::flex_wrap() };
        CSS::FlexBasisData flex_basis {};
        float flex_grow { InitialValues::flex_grow() };
        float flex_shrink { InitialValues::flex_shrink() };
        CSS::AlignItems align_items { InitialValues::align_items() };
        CSS::JustifyContent justify_content { InitialValues::justify_content() };
        CSS::Overflow overflow_x { InitialValues::overflow() };
        CSS::Overflow overflow_y { InitialValues::overflow() };
        float opacity { InitialValues::opacity() };
        Optional<BoxShadowData> box_shadow {};
        Vector<CSS::Transformation> transformations {};
        CSS::BoxSizing box_sizing { InitialValues::box_sizing() };
    } m_noninherited;
};

class ImmutableComputedValues final : public ComputedValues {
};

class MutableComputedValues final : public ComputedValues {
public:
    void set_color(const Color& color) { m_inherited.color = color; }
    void set_cursor(CSS::Cursor cursor) { m_inherited.cursor = cursor; }
    void set_pointer_events(CSS::PointerEvents value) { m_inherited.pointer_events = value; }
    void set_background_color(const Color& color) { m_noninherited.background_color = color; }
    void set_background_layers(Vector<BackgroundLayerData>&& layers) { m_noninherited.background_layers = move(layers); }
    void set_float(CSS::Float value) { m_noninherited.float_ = value; }
    void set_clear(CSS::Clear value) { m_noninherited.clear = value; }
    void set_z_index(Optional<int> value) { m_noninherited.z_index = value; }
    void set_text_align(CSS::TextAlign text_align) { m_inherited.text_align = text_align; }
    void set_text_decoration_line(CSS::TextDecorationLine value) { m_noninherited.text_decoration_line = value; }
    void set_text_decoration_style(CSS::TextDecorationStyle value) { m_noninherited.text_decoration_style = value; }
    void set_text_transform(CSS::TextTransform value) { m_inherited.text_transform = value; }
    void set_position(CSS::Position position) { m_noninherited.position = position; }
    void set_white_space(CSS::WhiteSpace value) { m_inherited.white_space = value; }
    void set_width(CSS::LengthPercentage const& width) { m_noninherited.width = width; }
    void set_min_width(CSS::LengthPercentage const& width) { m_noninherited.min_width = width; }
    void set_max_width(CSS::LengthPercentage const& width) { m_noninherited.max_width = width; }
    void set_height(CSS::LengthPercentage const& height) { m_noninherited.height = height; }
    void set_min_height(CSS::LengthPercentage const& height) { m_noninherited.min_height = height; }
    void set_max_height(CSS::LengthPercentage const& height) { m_noninherited.max_height = height; }
    void set_offset(const CSS::LengthBox& offset) { m_noninherited.offset = offset; }
    void set_margin(const CSS::LengthBox& margin) { m_noninherited.margin = margin; }
    void set_padding(const CSS::LengthBox& padding) { m_noninherited.padding = padding; }
    void set_overflow_x(CSS::Overflow value) { m_noninherited.overflow_x = value; }
    void set_overflow_y(CSS::Overflow value) { m_noninherited.overflow_y = value; }
    void set_list_style_type(CSS::ListStyleType value) { m_inherited.list_style_type = value; }
    void set_display(CSS::Display value) { m_noninherited.display = value; }
    void set_border_bottom_left_radius(CSS::LengthPercentage value) { m_noninherited.border_bottom_left_radius = value; }
    void set_border_bottom_right_radius(CSS::LengthPercentage value) { m_noninherited.border_bottom_right_radius = value; }
    void set_border_top_left_radius(CSS::LengthPercentage value) { m_noninherited.border_top_left_radius = value; }
    void set_border_top_right_radius(CSS::LengthPercentage value) { m_noninherited.border_top_right_radius = value; }
    BorderData& border_left() { return m_noninherited.border_left; }
    BorderData& border_top() { return m_noninherited.border_top; }
    BorderData& border_right() { return m_noninherited.border_right; }
    BorderData& border_bottom() { return m_noninherited.border_bottom; }
    void set_flex_direction(CSS::FlexDirection value) { m_noninherited.flex_direction = value; }
    void set_flex_wrap(CSS::FlexWrap value) { m_noninherited.flex_wrap = value; }
    void set_flex_basis(FlexBasisData value) { m_noninherited.flex_basis = value; }
    void set_flex_grow(float value) { m_noninherited.flex_grow = value; }
    void set_flex_shrink(float value) { m_noninherited.flex_shrink = value; }
    void set_align_items(CSS::AlignItems value) { m_noninherited.align_items = value; }
    void set_opacity(float value) { m_noninherited.opacity = value; }
    void set_justify_content(CSS::JustifyContent value) { m_noninherited.justify_content = value; }
    void set_box_shadow(Optional<BoxShadowData> value) { m_noninherited.box_shadow = move(value); }
    void set_transformations(Vector<CSS::Transformation> value) { m_noninherited.transformations = move(value); }
    void set_box_sizing(CSS::BoxSizing value) { m_noninherited.box_sizing = value; }

    void set_fill(Color value) { m_inherited.fill = value; }
    void set_stroke(Color value) { m_inherited.stroke = value; }
    void set_stroke_width(LengthPercentage value) { m_inherited.stroke_width = value; }
};

}
