/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashMap.h>
#include <AK/NonnullRefPtr.h>
#include <LibGfx/Font.h>
#include <LibGfx/Forward.h>
#include <LibWeb/CSS/ComputedValues.h>
#include <LibWeb/CSS/LengthBox.h>
#include <LibWeb/CSS/StyleValue.h>

namespace Web::CSS {

class StyleProperties : public RefCounted<StyleProperties> {
public:
    StyleProperties();

    explicit StyleProperties(const StyleProperties&);

    static NonnullRefPtr<StyleProperties> create() { return adopt_ref(*new StyleProperties); }

    NonnullRefPtr<StyleProperties> clone() const;

    template<typename Callback>
    inline void for_each_property(Callback callback) const
    {
        for (size_t i = 0; i < m_property_values.size(); ++i) {
            if (m_property_values[i])
                callback((CSS::PropertyID)i, *m_property_values[i]);
        }
    }

    auto& properties() { return m_property_values; }
    auto const& properties() const { return m_property_values; }

    void set_property(CSS::PropertyID, NonnullRefPtr<StyleValue> value);
    Optional<NonnullRefPtr<StyleValue>> property(CSS::PropertyID) const;

    Length length_or_fallback(CSS::PropertyID, Length const& fallback) const;
    LengthPercentage length_percentage_or_fallback(CSS::PropertyID, LengthPercentage const& fallback) const;
    Optional<LengthPercentage> length_percentage(CSS::PropertyID) const;
    LengthBox length_box(CSS::PropertyID left_id, CSS::PropertyID top_id, CSS::PropertyID right_id, CSS::PropertyID bottom_id, const CSS::Length& default_value) const;
    Color color_or_fallback(CSS::PropertyID, Layout::NodeWithStyle const&, Color fallback) const;
    Optional<CSS::TextAlign> text_align() const;
    CSS::Display display() const;
    Optional<CSS::Float> float_() const;
    Optional<CSS::Clear> clear() const;
    CSS::ContentData content() const;
    Optional<CSS::Cursor> cursor() const;
    Optional<CSS::WhiteSpace> white_space() const;
    Optional<CSS::LineStyle> line_style(CSS::PropertyID) const;
    Optional<CSS::TextDecorationLine> text_decoration_line() const;
    Optional<CSS::TextDecorationStyle> text_decoration_style() const;
    Optional<CSS::TextTransform> text_transform() const;
    Optional<CSS::ListStyleType> list_style_type() const;
    Optional<CSS::FlexDirection> flex_direction() const;
    Optional<CSS::FlexWrap> flex_wrap() const;
    Optional<CSS::FlexBasisData> flex_basis() const;
    float flex_grow() const;
    float flex_shrink() const;
    Optional<CSS::AlignItems> align_items() const;
    float opacity() const;
    Optional<CSS::ImageRendering> image_rendering() const;
    Optional<CSS::JustifyContent> justify_content() const;
    Optional<CSS::Overflow> overflow_x() const;
    Optional<CSS::Overflow> overflow_y() const;
    Vector<CSS::BoxShadowData> box_shadow() const;
    CSS::BoxSizing box_sizing() const;
    Optional<CSS::PointerEvents> pointer_events() const;
    Variant<CSS::VerticalAlign, CSS::LengthPercentage> vertical_align() const;

    Vector<CSS::Transformation> transformations() const;

    Gfx::Font const& computed_font() const
    {
        VERIFY(m_font);
        return *m_font;
    }

    void set_computed_font(NonnullRefPtr<Gfx::Font> font)
    {
        m_font = move(font);
    }

    float line_height(const Layout::Node&) const;

    bool operator==(const StyleProperties&) const;
    bool operator!=(const StyleProperties& other) const { return !(*this == other); }

    Optional<CSS::Position> position() const;
    Optional<int> z_index() const;

    static NonnullRefPtr<Gfx::Font> font_fallback(bool monospace, bool bold);

private:
    friend class StyleComputer;

    Array<RefPtr<StyleValue>, to_underlying(CSS::last_property_id) + 1> m_property_values;
    Optional<CSS::Overflow> overflow(CSS::PropertyID) const;

    mutable RefPtr<Gfx::Font> m_font;
};

}
