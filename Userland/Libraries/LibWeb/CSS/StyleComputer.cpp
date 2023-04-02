/*
 * Copyright (c) 2018-2023, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, the SerenityOS developers.
 * Copyright (c) 2021-2023, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Debug.h>
#include <AK/Error.h>
#include <AK/HashMap.h>
#include <AK/QuickSort.h>
#include <AK/TemporaryChange.h>
#include <LibGfx/Font/Font.h>
#include <LibGfx/Font/FontDatabase.h>
#include <LibGfx/Font/FontStyleMapping.h>
#include <LibGfx/Font/OpenType/Font.h>
#include <LibGfx/Font/ScaledFont.h>
#include <LibGfx/Font/VectorFont.h>
#include <LibGfx/Font/WOFF/Font.h>
#include <LibWeb/CSS/CSSFontFaceRule.h>
#include <LibWeb/CSS/CSSImportRule.h>
#include <LibWeb/CSS/CSSStyleRule.h>
#include <LibWeb/CSS/Parser/Parser.h>
#include <LibWeb/CSS/SelectorEngine.h>
#include <LibWeb/CSS/StyleComputer.h>
#include <LibWeb/CSS/StyleSheet.h>
#include <LibWeb/CSS/StyleValues/BackgroundStyleValue.h>
#include <LibWeb/CSS/StyleValues/BorderRadiusShorthandStyleValue.h>
#include <LibWeb/CSS/StyleValues/BorderRadiusStyleValue.h>
#include <LibWeb/CSS/StyleValues/BorderStyleValue.h>
#include <LibWeb/CSS/StyleValues/ColorStyleValue.h>
#include <LibWeb/CSS/StyleValues/FilterValueListStyleValue.h>
#include <LibWeb/CSS/StyleValues/FlexFlowStyleValue.h>
#include <LibWeb/CSS/StyleValues/FlexStyleValue.h>
#include <LibWeb/CSS/StyleValues/FontStyleValue.h>
#include <LibWeb/CSS/StyleValues/GridAreaShorthandStyleValue.h>
#include <LibWeb/CSS/StyleValues/GridTrackPlacementShorthandStyleValue.h>
#include <LibWeb/CSS/StyleValues/GridTrackPlacementStyleValue.h>
#include <LibWeb/CSS/StyleValues/IdentifierStyleValue.h>
#include <LibWeb/CSS/StyleValues/LengthStyleValue.h>
#include <LibWeb/CSS/StyleValues/ListStyleStyleValue.h>
#include <LibWeb/CSS/StyleValues/NumericStyleValue.h>
#include <LibWeb/CSS/StyleValues/OverflowStyleValue.h>
#include <LibWeb/CSS/StyleValues/PercentageStyleValue.h>
#include <LibWeb/CSS/StyleValues/PositionStyleValue.h>
#include <LibWeb/CSS/StyleValues/StyleValueList.h>
#include <LibWeb/CSS/StyleValues/TextDecorationStyleValue.h>
#include <LibWeb/CSS/StyleValues/UnresolvedStyleValue.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/Element.h>
#include <LibWeb/FontCache.h>
#include <LibWeb/HTML/HTMLHtmlElement.h>
#include <LibWeb/Loader/ResourceLoader.h>
#include <LibWeb/Platform/FontPlugin.h>
#include <stdio.h>

namespace Web::CSS {

StyleComputer::StyleComputer(DOM::Document& document)
    : m_document(document)
{
}

StyleComputer::~StyleComputer() = default;

class StyleComputer::FontLoader : public ResourceClient {
public:
    explicit FontLoader(StyleComputer& style_computer, FlyString family_name, Vector<AK::URL> urls)
        : m_style_computer(style_computer)
        , m_family_name(move(family_name))
        , m_urls(move(urls))
    {
        start_loading_next_url();
    }

    virtual ~FontLoader() override { }

    virtual void resource_did_load() override
    {
        auto result = try_load_font();
        if (result.is_error())
            return start_loading_next_url();
        m_vector_font = result.release_value();
        m_style_computer.did_load_font(m_family_name);
    }

    virtual void resource_did_fail() override
    {
    }

    RefPtr<Gfx::Font> font_with_point_size(float point_size) const
    {
        if (!m_vector_font)
            return nullptr;

        if (auto it = m_cached_fonts.find(point_size); it != m_cached_fonts.end())
            return it->value;

        // FIXME: It might be nicer to have a global cap on the number of fonts we cache
        //        instead of doing it at the per-font level like this.
        constexpr size_t max_cached_font_size_count = 64;
        if (m_cached_fonts.size() > max_cached_font_size_count)
            m_cached_fonts.remove(m_cached_fonts.begin());

        auto font = adopt_ref(*new Gfx::ScaledFont(*m_vector_font, point_size, point_size));
        m_cached_fonts.set(point_size, font);
        return font;
    }

private:
    void start_loading_next_url()
    {
        if (m_urls.is_empty())
            return;
        LoadRequest request;
        request.set_url(m_urls.take_first());
        set_resource(ResourceLoader::the().load_resource(Resource::Type::Generic, request));
    }

    ErrorOr<NonnullRefPtr<Gfx::VectorFont>> try_load_font()
    {
        // FIXME: This could maybe use the format() provided in @font-face as well, since often the mime type is just application/octet-stream and we have to try every format
        auto mime_type = resource()->mime_type();
        if (mime_type == "font/ttf"sv || mime_type == "application/x-font-ttf"sv)
            return TRY(OpenType::Font::try_load_from_externally_owned_memory(resource()->encoded_data()));
        if (mime_type == "font/woff"sv || mime_type == "application/font-woff"sv)
            return TRY(WOFF::Font::try_load_from_externally_owned_memory(resource()->encoded_data()));
        auto ttf = OpenType::Font::try_load_from_externally_owned_memory(resource()->encoded_data());
        if (!ttf.is_error())
            return ttf.release_value();
        auto woff = WOFF::Font::try_load_from_externally_owned_memory(resource()->encoded_data());
        if (!woff.is_error())
            return woff.release_value();
        return ttf.release_error();
    }

    StyleComputer& m_style_computer;
    FlyString m_family_name;
    RefPtr<Gfx::VectorFont> m_vector_font;
    Vector<AK::URL> m_urls;

    HashMap<float, NonnullRefPtr<Gfx::ScaledFont>> mutable m_cached_fonts;
};

static CSSStyleSheet& default_stylesheet(DOM::Document const& document)
{
    static JS::Handle<CSSStyleSheet> sheet;
    if (!sheet.cell()) {
        extern StringView default_stylesheet_source;
        sheet = JS::make_handle(parse_css_stylesheet(CSS::Parser::ParsingContext(document), default_stylesheet_source));
    }
    return *sheet;
}

static CSSStyleSheet& quirks_mode_stylesheet(DOM::Document const& document)
{
    static JS::Handle<CSSStyleSheet> sheet;
    if (!sheet.cell()) {
        extern StringView quirks_mode_stylesheet_source;
        sheet = JS::make_handle(parse_css_stylesheet(CSS::Parser::ParsingContext(document), quirks_mode_stylesheet_source));
    }
    return *sheet;
}

template<typename Callback>
void StyleComputer::for_each_stylesheet(CascadeOrigin cascade_origin, Callback callback) const
{
    if (cascade_origin == CascadeOrigin::UserAgent) {
        callback(default_stylesheet(document()));
        if (document().in_quirks_mode())
            callback(quirks_mode_stylesheet(document()));
    }
    if (cascade_origin == CascadeOrigin::Author) {
        for (auto const& sheet : document().style_sheets().sheets())
            callback(*sheet);
    }
}

StyleComputer::RuleCache const& StyleComputer::rule_cache_for_cascade_origin(CascadeOrigin cascade_origin) const
{
    switch (cascade_origin) {
    case CascadeOrigin::Author:
        return *m_author_rule_cache;
    case CascadeOrigin::UserAgent:
        return *m_user_agent_rule_cache;
    default:
        TODO();
    }
}

Vector<MatchingRule> StyleComputer::collect_matching_rules(DOM::Element const& element, CascadeOrigin cascade_origin, Optional<CSS::Selector::PseudoElement> pseudo_element) const
{
    auto const& rule_cache = rule_cache_for_cascade_origin(cascade_origin);

    Vector<MatchingRule> rules_to_run;
    auto add_rules_to_run = [&](Vector<MatchingRule> const& rules) {
        if (pseudo_element.has_value()) {
            for (auto& rule : rules) {
                if (rule.contains_pseudo_element)
                    rules_to_run.append(rule);
            }
        } else {
            rules_to_run.extend(rules);
        }
    };

    for (auto const& class_name : element.class_names()) {
        if (auto it = rule_cache.rules_by_class.find(class_name); it != rule_cache.rules_by_class.end())
            add_rules_to_run(it->value);
    }
    if (auto id = element.get_attribute(HTML::AttributeNames::id); !id.is_null()) {
        if (auto it = rule_cache.rules_by_id.find(FlyString::from_deprecated_fly_string(id).release_value_but_fixme_should_propagate_errors()); it != rule_cache.rules_by_id.end())
            add_rules_to_run(it->value);
    }
    if (auto it = rule_cache.rules_by_tag_name.find(FlyString::from_deprecated_fly_string(element.local_name()).release_value_but_fixme_should_propagate_errors()); it != rule_cache.rules_by_tag_name.end())
        add_rules_to_run(it->value);
    add_rules_to_run(rule_cache.other_rules);

    Vector<MatchingRule> matching_rules;
    matching_rules.ensure_capacity(rules_to_run.size());
    for (auto const& rule_to_run : rules_to_run) {
        auto const& selector = rule_to_run.rule->selectors()[rule_to_run.selector_index];
        if (SelectorEngine::matches(selector, element, pseudo_element))
            matching_rules.append(rule_to_run);
    }
    return matching_rules;
}

static void sort_matching_rules(Vector<MatchingRule>& matching_rules)
{
    quick_sort(matching_rules, [&](MatchingRule& a, MatchingRule& b) {
        auto const& a_selector = a.rule->selectors()[a.selector_index];
        auto const& b_selector = b.rule->selectors()[b.selector_index];
        auto a_specificity = a_selector->specificity();
        auto b_specificity = b_selector->specificity();
        if (a_selector->specificity() == b_selector->specificity()) {
            if (a.style_sheet_index == b.style_sheet_index)
                return a.rule_index < b.rule_index;
            return a.style_sheet_index < b.style_sheet_index;
        }
        return a_specificity < b_specificity;
    });
}

enum class Edge {
    Top,
    Right,
    Bottom,
    Left,
    All,
};

static bool contains(Edge a, Edge b)
{
    return a == b || b == Edge::All;
}

static void set_property_expanding_shorthands(StyleProperties& style, CSS::PropertyID property_id, StyleValue const& value, DOM::Document& document)
{
    auto assign_edge_values = [&style](PropertyID top_property, PropertyID right_property, PropertyID bottom_property, PropertyID left_property, auto const& values) {
        if (values.size() == 4) {
            style.set_property(top_property, values[0]);
            style.set_property(right_property, values[1]);
            style.set_property(bottom_property, values[2]);
            style.set_property(left_property, values[3]);
        } else if (values.size() == 3) {
            style.set_property(top_property, values[0]);
            style.set_property(right_property, values[1]);
            style.set_property(bottom_property, values[2]);
            style.set_property(left_property, values[1]);
        } else if (values.size() == 2) {
            style.set_property(top_property, values[0]);
            style.set_property(right_property, values[1]);
            style.set_property(bottom_property, values[0]);
            style.set_property(left_property, values[1]);
        } else if (values.size() == 1) {
            style.set_property(top_property, values[0]);
            style.set_property(right_property, values[0]);
            style.set_property(bottom_property, values[0]);
            style.set_property(left_property, values[0]);
        }
    };

    if (property_id == CSS::PropertyID::TextDecoration) {
        if (value.is_text_decoration()) {
            auto const& text_decoration = value.as_text_decoration();
            style.set_property(CSS::PropertyID::TextDecorationLine, text_decoration.line());
            style.set_property(CSS::PropertyID::TextDecorationThickness, text_decoration.thickness());
            style.set_property(CSS::PropertyID::TextDecorationStyle, text_decoration.style());
            style.set_property(CSS::PropertyID::TextDecorationColor, text_decoration.color());
            return;
        }

        style.set_property(CSS::PropertyID::TextDecorationLine, value);
        style.set_property(CSS::PropertyID::TextDecorationThickness, value);
        style.set_property(CSS::PropertyID::TextDecorationStyle, value);
        style.set_property(CSS::PropertyID::TextDecorationColor, value);
        return;
    }

    if (property_id == CSS::PropertyID::Overflow) {
        if (value.is_overflow()) {
            auto const& overflow = value.as_overflow();
            style.set_property(CSS::PropertyID::OverflowX, overflow.overflow_x());
            style.set_property(CSS::PropertyID::OverflowY, overflow.overflow_y());
            return;
        }

        style.set_property(CSS::PropertyID::OverflowX, value);
        style.set_property(CSS::PropertyID::OverflowY, value);
        return;
    }

    if (property_id == CSS::PropertyID::Border) {
        set_property_expanding_shorthands(style, CSS::PropertyID::BorderTop, value, document);
        set_property_expanding_shorthands(style, CSS::PropertyID::BorderRight, value, document);
        set_property_expanding_shorthands(style, CSS::PropertyID::BorderBottom, value, document);
        set_property_expanding_shorthands(style, CSS::PropertyID::BorderLeft, value, document);
        // FIXME: Also reset border-image, in line with the spec: https://www.w3.org/TR/css-backgrounds-3/#border-shorthands
        return;
    }

    if (property_id == CSS::PropertyID::BorderRadius) {
        if (value.is_border_radius_shorthand()) {
            auto const& shorthand = value.as_border_radius_shorthand();
            style.set_property(CSS::PropertyID::BorderTopLeftRadius, shorthand.top_left());
            style.set_property(CSS::PropertyID::BorderTopRightRadius, shorthand.top_right());
            style.set_property(CSS::PropertyID::BorderBottomRightRadius, shorthand.bottom_right());
            style.set_property(CSS::PropertyID::BorderBottomLeftRadius, shorthand.bottom_left());
            return;
        }

        style.set_property(CSS::PropertyID::BorderTopLeftRadius, value);
        style.set_property(CSS::PropertyID::BorderTopRightRadius, value);
        style.set_property(CSS::PropertyID::BorderBottomRightRadius, value);
        style.set_property(CSS::PropertyID::BorderBottomLeftRadius, value);
        return;
    }

    if (property_id == CSS::PropertyID::BorderTop
        || property_id == CSS::PropertyID::BorderRight
        || property_id == CSS::PropertyID::BorderBottom
        || property_id == CSS::PropertyID::BorderLeft) {

        Edge edge = Edge::All;
        switch (property_id) {
        case CSS::PropertyID::BorderTop:
            edge = Edge::Top;
            break;
        case CSS::PropertyID::BorderRight:
            edge = Edge::Right;
            break;
        case CSS::PropertyID::BorderBottom:
            edge = Edge::Bottom;
            break;
        case CSS::PropertyID::BorderLeft:
            edge = Edge::Left;
            break;
        default:
            break;
        }

        if (value.is_border()) {
            auto const& border = value.as_border();
            if (contains(Edge::Top, edge)) {
                style.set_property(PropertyID::BorderTopWidth, border.border_width());
                style.set_property(PropertyID::BorderTopStyle, border.border_style());
                style.set_property(PropertyID::BorderTopColor, border.border_color());
            }
            if (contains(Edge::Right, edge)) {
                style.set_property(PropertyID::BorderRightWidth, border.border_width());
                style.set_property(PropertyID::BorderRightStyle, border.border_style());
                style.set_property(PropertyID::BorderRightColor, border.border_color());
            }
            if (contains(Edge::Bottom, edge)) {
                style.set_property(PropertyID::BorderBottomWidth, border.border_width());
                style.set_property(PropertyID::BorderBottomStyle, border.border_style());
                style.set_property(PropertyID::BorderBottomColor, border.border_color());
            }
            if (contains(Edge::Left, edge)) {
                style.set_property(PropertyID::BorderLeftWidth, border.border_width());
                style.set_property(PropertyID::BorderLeftStyle, border.border_style());
                style.set_property(PropertyID::BorderLeftColor, border.border_color());
            }
            return;
        }
        return;
    }

    if (property_id == CSS::PropertyID::BorderStyle) {
        if (value.is_value_list()) {
            auto const& values_list = value.as_value_list();
            assign_edge_values(PropertyID::BorderTopStyle, PropertyID::BorderRightStyle, PropertyID::BorderBottomStyle, PropertyID::BorderLeftStyle, values_list.values());
            return;
        }

        style.set_property(CSS::PropertyID::BorderTopStyle, value);
        style.set_property(CSS::PropertyID::BorderRightStyle, value);
        style.set_property(CSS::PropertyID::BorderBottomStyle, value);
        style.set_property(CSS::PropertyID::BorderLeftStyle, value);
        return;
    }

    if (property_id == CSS::PropertyID::BorderWidth) {
        if (value.is_value_list()) {
            auto const& values_list = value.as_value_list();
            assign_edge_values(PropertyID::BorderTopWidth, PropertyID::BorderRightWidth, PropertyID::BorderBottomWidth, PropertyID::BorderLeftWidth, values_list.values());
            return;
        }

        style.set_property(CSS::PropertyID::BorderTopWidth, value);
        style.set_property(CSS::PropertyID::BorderRightWidth, value);
        style.set_property(CSS::PropertyID::BorderBottomWidth, value);
        style.set_property(CSS::PropertyID::BorderLeftWidth, value);
        return;
    }

    if (property_id == CSS::PropertyID::BorderColor) {
        if (value.is_value_list()) {
            auto const& values_list = value.as_value_list();
            assign_edge_values(PropertyID::BorderTopColor, PropertyID::BorderRightColor, PropertyID::BorderBottomColor, PropertyID::BorderLeftColor, values_list.values());
            return;
        }

        style.set_property(CSS::PropertyID::BorderTopColor, value);
        style.set_property(CSS::PropertyID::BorderRightColor, value);
        style.set_property(CSS::PropertyID::BorderBottomColor, value);
        style.set_property(CSS::PropertyID::BorderLeftColor, value);
        return;
    }

    if (property_id == CSS::PropertyID::Background) {
        if (value.is_background()) {
            auto const& background = value.as_background();
            set_property_expanding_shorthands(style, CSS::PropertyID::BackgroundColor, background.color(), document);
            set_property_expanding_shorthands(style, CSS::PropertyID::BackgroundImage, background.image(), document);
            set_property_expanding_shorthands(style, CSS::PropertyID::BackgroundPosition, background.position(), document);
            set_property_expanding_shorthands(style, CSS::PropertyID::BackgroundSize, background.size(), document);
            set_property_expanding_shorthands(style, CSS::PropertyID::BackgroundRepeat, background.repeat(), document);
            set_property_expanding_shorthands(style, CSS::PropertyID::BackgroundAttachment, background.attachment(), document);
            set_property_expanding_shorthands(style, CSS::PropertyID::BackgroundOrigin, background.origin(), document);
            set_property_expanding_shorthands(style, CSS::PropertyID::BackgroundClip, background.clip(), document);
            return;
        }

        set_property_expanding_shorthands(style, CSS::PropertyID::BackgroundColor, value, document);
        set_property_expanding_shorthands(style, CSS::PropertyID::BackgroundImage, value, document);
        set_property_expanding_shorthands(style, CSS::PropertyID::BackgroundPosition, value, document);
        set_property_expanding_shorthands(style, CSS::PropertyID::BackgroundSize, value, document);
        set_property_expanding_shorthands(style, CSS::PropertyID::BackgroundRepeat, value, document);
        set_property_expanding_shorthands(style, CSS::PropertyID::BackgroundAttachment, value, document);
        set_property_expanding_shorthands(style, CSS::PropertyID::BackgroundOrigin, value, document);
        set_property_expanding_shorthands(style, CSS::PropertyID::BackgroundClip, value, document);
        return;
    }

    if (property_id == CSS::PropertyID::BackgroundPosition) {
        if (value.is_position()) {
            auto const& position = value.as_position();
            style.set_property(CSS::PropertyID::BackgroundPositionX, position.edge_x());
            style.set_property(CSS::PropertyID::BackgroundPositionY, position.edge_y());
            return;
        }

        style.set_property(CSS::PropertyID::BackgroundPositionX, value);
        style.set_property(CSS::PropertyID::BackgroundPositionY, value);
        return;
    }

    if (property_id == CSS::PropertyID::Margin) {
        if (value.is_value_list()) {
            auto const& values_list = value.as_value_list();
            assign_edge_values(PropertyID::MarginTop, PropertyID::MarginRight, PropertyID::MarginBottom, PropertyID::MarginLeft, values_list.values());
            return;
        }

        style.set_property(CSS::PropertyID::MarginTop, value);
        style.set_property(CSS::PropertyID::MarginRight, value);
        style.set_property(CSS::PropertyID::MarginBottom, value);
        style.set_property(CSS::PropertyID::MarginLeft, value);
        return;
    }

    if (property_id == CSS::PropertyID::Padding) {
        if (value.is_value_list()) {
            auto const& values_list = value.as_value_list();
            assign_edge_values(PropertyID::PaddingTop, PropertyID::PaddingRight, PropertyID::PaddingBottom, PropertyID::PaddingLeft, values_list.values());
            return;
        }

        style.set_property(CSS::PropertyID::PaddingTop, value);
        style.set_property(CSS::PropertyID::PaddingRight, value);
        style.set_property(CSS::PropertyID::PaddingBottom, value);
        style.set_property(CSS::PropertyID::PaddingLeft, value);
        return;
    }

    if (property_id == CSS::PropertyID::ListStyle) {
        if (value.is_list_style()) {
            auto const& list_style = value.as_list_style();
            style.set_property(CSS::PropertyID::ListStylePosition, list_style.position());
            style.set_property(CSS::PropertyID::ListStyleImage, list_style.image());
            style.set_property(CSS::PropertyID::ListStyleType, list_style.style_type());
            return;
        }

        style.set_property(CSS::PropertyID::ListStylePosition, value);
        style.set_property(CSS::PropertyID::ListStyleImage, value);
        style.set_property(CSS::PropertyID::ListStyleType, value);
        return;
    }

    if (property_id == CSS::PropertyID::Font) {
        if (value.is_font()) {
            auto const& font_shorthand = value.as_font();
            style.set_property(CSS::PropertyID::FontSize, font_shorthand.font_size());
            style.set_property(CSS::PropertyID::FontFamily, font_shorthand.font_families());
            style.set_property(CSS::PropertyID::FontStretch, font_shorthand.font_stretch());
            style.set_property(CSS::PropertyID::FontStyle, font_shorthand.font_style());
            style.set_property(CSS::PropertyID::FontWeight, font_shorthand.font_weight());
            style.set_property(CSS::PropertyID::LineHeight, font_shorthand.line_height());
            // FIXME: Implement font-variant
            return;
        }

        style.set_property(CSS::PropertyID::FontStretch, value);
        style.set_property(CSS::PropertyID::FontSize, value);
        style.set_property(CSS::PropertyID::FontFamily, value);
        style.set_property(CSS::PropertyID::FontStyle, value);
        style.set_property(CSS::PropertyID::FontWeight, value);
        style.set_property(CSS::PropertyID::LineHeight, value);
        // FIXME: Implement font-variant
        return;
    }

    if (property_id == CSS::PropertyID::Flex) {
        if (value.is_flex()) {
            auto const& flex = value.as_flex();
            style.set_property(CSS::PropertyID::FlexGrow, flex.grow());
            style.set_property(CSS::PropertyID::FlexShrink, flex.shrink());
            style.set_property(CSS::PropertyID::FlexBasis, flex.basis());
            return;
        }

        style.set_property(CSS::PropertyID::FlexGrow, value);
        style.set_property(CSS::PropertyID::FlexShrink, value);
        style.set_property(CSS::PropertyID::FlexBasis, value);
        return;
    }

    if (property_id == CSS::PropertyID::FlexFlow) {
        if (value.is_flex_flow()) {
            auto const& flex_flow = value.as_flex_flow();
            style.set_property(CSS::PropertyID::FlexDirection, flex_flow.flex_direction());
            style.set_property(CSS::PropertyID::FlexWrap, flex_flow.flex_wrap());
            return;
        }

        style.set_property(CSS::PropertyID::FlexDirection, value);
        style.set_property(CSS::PropertyID::FlexWrap, value);
        return;
    }

    if (property_id == CSS::PropertyID::GridArea) {
        if (value.is_grid_area_shorthand()) {
            auto const& shorthand = value.as_grid_area_shorthand();
            style.set_property(CSS::PropertyID::GridRowStart, shorthand.row_start());
            style.set_property(CSS::PropertyID::GridColumnStart, shorthand.column_start());
            style.set_property(CSS::PropertyID::GridRowEnd, shorthand.row_end());
            style.set_property(CSS::PropertyID::GridColumnEnd, shorthand.column_end());
            return;
        }
        style.set_property(CSS::PropertyID::GridRowStart, value);
        style.set_property(CSS::PropertyID::GridColumnStart, value);
        style.set_property(CSS::PropertyID::GridRowEnd, value);
        style.set_property(CSS::PropertyID::GridColumnEnd, value);
        return;
    }

    if (property_id == CSS::PropertyID::GridColumn) {
        if (value.is_grid_track_placement_shorthand()) {
            auto const& shorthand = value.as_grid_track_placement_shorthand();
            style.set_property(CSS::PropertyID::GridColumnStart, shorthand.start());
            style.set_property(CSS::PropertyID::GridColumnEnd, shorthand.end());
            return;
        }

        style.set_property(CSS::PropertyID::GridColumnStart, value);
        style.set_property(CSS::PropertyID::GridColumnEnd, value);
        return;
    }

    if (property_id == CSS::PropertyID::GridRow) {
        if (value.is_grid_track_placement_shorthand()) {
            auto const& shorthand = value.as_grid_track_placement_shorthand();
            style.set_property(CSS::PropertyID::GridRowStart, shorthand.start());
            style.set_property(CSS::PropertyID::GridRowEnd, shorthand.end());
            return;
        }

        style.set_property(CSS::PropertyID::GridRowStart, value);
        style.set_property(CSS::PropertyID::GridRowEnd, value);
        return;
    }

    if (property_id == CSS::PropertyID::Gap || property_id == CSS::PropertyID::GridGap) {
        if (value.is_value_list()) {
            auto const& values_list = value.as_value_list();
            style.set_property(CSS::PropertyID::RowGap, values_list.values()[0]);
            style.set_property(CSS::PropertyID::ColumnGap, values_list.values()[1]);
            return;
        }
        style.set_property(CSS::PropertyID::RowGap, value);
        style.set_property(CSS::PropertyID::ColumnGap, value);
        return;
    }

    if (property_id == CSS::PropertyID::RowGap || property_id == CSS::PropertyID::GridRowGap) {
        style.set_property(CSS::PropertyID::RowGap, value);
        return;
    }

    if (property_id == CSS::PropertyID::ColumnGap || property_id == CSS::PropertyID::GridColumnGap) {
        style.set_property(CSS::PropertyID::ColumnGap, value);
        return;
    }

    style.set_property(property_id, value);
}

static RefPtr<StyleValue const> get_custom_property(DOM::Element const& element, FlyString const& custom_property_name)
{
    for (auto const* current_element = &element; current_element; current_element = current_element->parent_element()) {
        if (auto it = current_element->custom_properties().find(custom_property_name.to_string().to_deprecated_string()); it != current_element->custom_properties().end())
            return it->value.value;
    }
    return nullptr;
}

bool StyleComputer::expand_variables(DOM::Element& element, StringView property_name, HashMap<FlyString, NonnullRefPtr<PropertyDependencyNode>>& dependencies, Parser::TokenStream<Parser::ComponentValue>& source, Vector<Parser::ComponentValue>& dest) const
{
    // Arbitrary large value chosen to avoid the billion-laughs attack.
    // https://www.w3.org/TR/css-variables-1/#long-variables
    const size_t MAX_VALUE_COUNT = 16384;
    if (source.remaining_token_count() + dest.size() > MAX_VALUE_COUNT) {
        dbgln("Stopped expanding CSS variables: maximum length reached.");
        return false;
    }

    auto get_dependency_node = [&](FlyString name) -> NonnullRefPtr<PropertyDependencyNode> {
        if (auto existing = dependencies.get(name); existing.has_value())
            return *existing.value();
        auto new_node = PropertyDependencyNode::create(name.to_string());
        dependencies.set(name, new_node);
        return new_node;
    };

    while (source.has_next_token()) {
        auto const& value = source.next_token();
        if (!value.is_function()) {
            dest.empend(value);
            continue;
        }
        if (!value.function().name().equals_ignoring_ascii_case("var"sv)) {
            auto const& source_function = value.function();
            Vector<Parser::ComponentValue> function_values;
            Parser::TokenStream source_function_contents { source_function.values() };
            if (!expand_variables(element, property_name, dependencies, source_function_contents, function_values))
                return false;
            NonnullRefPtr<Parser::Function> function = Parser::Function::create(FlyString::from_utf8(source_function.name()).release_value_but_fixme_should_propagate_errors(), move(function_values));
            dest.empend(function);
            continue;
        }

        Parser::TokenStream var_contents { value.function().values() };
        var_contents.skip_whitespace();
        if (!var_contents.has_next_token())
            return false;

        auto const& custom_property_name_token = var_contents.next_token();
        if (!custom_property_name_token.is(Parser::Token::Type::Ident))
            return false;
        auto custom_property_name = custom_property_name_token.token().ident();
        if (!custom_property_name.starts_with("--"sv))
            return false;

        // Detect dependency cycles. https://www.w3.org/TR/css-variables-1/#cycles
        // We do not do this by the spec, since we are not keeping a graph of var dependencies around,
        // but rebuilding it every time.
        if (custom_property_name == property_name)
            return false;
        auto parent = get_dependency_node(FlyString::from_utf8(property_name).release_value_but_fixme_should_propagate_errors());
        auto child = get_dependency_node(FlyString::from_utf8(custom_property_name).release_value_but_fixme_should_propagate_errors());
        parent->add_child(child);
        if (parent->has_cycles())
            return false;

        if (auto custom_property_value = get_custom_property(element, FlyString::from_utf8(custom_property_name).release_value_but_fixme_should_propagate_errors())) {
            VERIFY(custom_property_value->is_unresolved());
            Parser::TokenStream custom_property_tokens { custom_property_value->as_unresolved().values() };
            if (!expand_variables(element, custom_property_name, dependencies, custom_property_tokens, dest))
                return false;
            continue;
        }

        // Use the provided fallback value, if any.
        var_contents.skip_whitespace();
        if (var_contents.has_next_token()) {
            auto const& comma_token = var_contents.next_token();
            if (!comma_token.is(Parser::Token::Type::Comma))
                return false;
            var_contents.skip_whitespace();
            if (!expand_variables(element, property_name, dependencies, var_contents, dest))
                return false;
        }
    }
    return true;
}

bool StyleComputer::expand_unresolved_values(DOM::Element& element, StringView property_name, Parser::TokenStream<Parser::ComponentValue>& source, Vector<Parser::ComponentValue>& dest) const
{
    // FIXME: Do this better!
    // We build a copy of the tree of ComponentValues, with all var()s and attr()s replaced with their contents.
    // This is a very naive solution, and we could do better if the CSS Parser could accept tokens one at a time.

    while (source.has_next_token()) {
        auto const& value = source.next_token();
        if (value.is_function()) {
            if (value.function().name().equals_ignoring_ascii_case("attr"sv)) {
                // https://drafts.csswg.org/css-values-5/#attr-substitution
                Parser::TokenStream attr_contents { value.function().values() };
                attr_contents.skip_whitespace();
                if (!attr_contents.has_next_token())
                    return false;

                auto const& attr_name_token = attr_contents.next_token();
                if (!attr_name_token.is(Parser::Token::Type::Ident))
                    return false;
                auto attr_name = attr_name_token.token().ident();

                auto attr_value = element.get_attribute(attr_name);
                // 1. If the attr() function has a substitution value, replace the attr() function by the substitution value.
                if (!attr_value.is_null()) {
                    // FIXME: attr() should also accept an optional type argument, not just strings.
                    dest.empend(Parser::Token::of_string(FlyString::from_deprecated_fly_string(attr_value).release_value_but_fixme_should_propagate_errors()));
                    continue;
                }

                // 2. Otherwise, if the attr() function has a fallback value as its last argument, replace the attr() function by the fallback value.
                //    If there are any var() or attr() references in the fallback, substitute them as well.
                attr_contents.skip_whitespace();
                if (attr_contents.has_next_token()) {
                    auto const& comma_token = attr_contents.next_token();
                    if (!comma_token.is(Parser::Token::Type::Comma))
                        return false;
                    attr_contents.skip_whitespace();
                    if (!expand_unresolved_values(element, property_name, attr_contents, dest))
                        return false;
                    continue;
                }

                // 3. Otherwise, the property containing the attr() function is invalid at computed-value time.
                return false;
            }

            if (value.function().name().equals_ignoring_ascii_case("calc"sv)) {
                auto const& calc_function = value.function();
                if (auto calc_value = CSS::Parser::Parser::parse_calculated_value({}, Parser::ParsingContext { document() }, calc_function.values())) {
                    switch (calc_value->resolved_type()) {
                    case CalculatedStyleValue::ResolvedType::Integer: {
                        auto resolved_value = calc_value->resolve_integer();
                        dest.empend(Parser::Token::create_number(resolved_value.value()));
                        continue;
                    }
                    case CalculatedStyleValue::ResolvedType::Percentage: {
                        auto resolved_value = calc_value->resolve_percentage();
                        dest.empend(Parser::Token::create_percentage(resolved_value.value().value()));
                        continue;
                    }
                    default:
                        dbgln("FIXME: Unimplemented calc() expansion: {}", calc_value->to_string());
                        break;
                    }
                }
            }

            auto const& source_function = value.function();
            Vector<Parser::ComponentValue> function_values;
            Parser::TokenStream source_function_contents { source_function.values() };
            if (!expand_unresolved_values(element, property_name, source_function_contents, function_values))
                return false;
            // FIXME: This would be much nicer if we could access the source_function's FlyString value directly.
            NonnullRefPtr<Parser::Function> function = Parser::Function::create(FlyString::from_utf8(source_function.name()).release_value_but_fixme_should_propagate_errors(), move(function_values));
            dest.empend(function);
            continue;
        }
        if (value.is_block()) {
            auto const& source_block = value.block();
            Parser::TokenStream source_block_values { source_block.values() };
            Vector<Parser::ComponentValue> block_values;
            if (!expand_unresolved_values(element, property_name, source_block_values, block_values))
                return false;
            NonnullRefPtr<Parser::Block> block = Parser::Block::create(source_block.token(), move(block_values));
            dest.empend(move(block));
            continue;
        }
        dest.empend(value.token());
    }

    return true;
}

RefPtr<StyleValue> StyleComputer::resolve_unresolved_style_value(DOM::Element& element, PropertyID property_id, UnresolvedStyleValue const& unresolved) const
{
    // Unresolved always contains a var() or attr(), unless it is a custom property's value, in which case we shouldn't be trying
    // to produce a different StyleValue from it.
    VERIFY(unresolved.contains_var_or_attr());

    Parser::TokenStream unresolved_values_without_variables_expanded { unresolved.values() };
    Vector<Parser::ComponentValue> values_with_variables_expanded;

    HashMap<FlyString, NonnullRefPtr<PropertyDependencyNode>> dependencies;
    if (!expand_variables(element, string_from_property_id(property_id), dependencies, unresolved_values_without_variables_expanded, values_with_variables_expanded))
        return {};

    Parser::TokenStream unresolved_values_with_variables_expanded { values_with_variables_expanded };
    Vector<Parser::ComponentValue> expanded_values;
    if (!expand_unresolved_values(element, string_from_property_id(property_id), unresolved_values_with_variables_expanded, expanded_values))
        return {};

    if (auto parsed_value = Parser::Parser::parse_css_value({}, Parser::ParsingContext { document() }, property_id, expanded_values))
        return parsed_value.release_nonnull();

    return {};
}

void StyleComputer::cascade_declarations(StyleProperties& style, DOM::Element& element, Vector<MatchingRule> const& matching_rules, CascadeOrigin cascade_origin, Important important) const
{
    for (auto const& match : matching_rules) {
        for (auto const& property : verify_cast<PropertyOwningCSSStyleDeclaration>(match.rule->declaration()).properties()) {
            if (important != property.important)
                continue;
            auto property_value = property.value;
            if (property.value->is_unresolved()) {
                if (auto resolved = resolve_unresolved_style_value(element, property.property_id, property.value->as_unresolved()))
                    property_value = resolved.release_nonnull();
            }
            if (!property_value->is_unresolved())
                set_property_expanding_shorthands(style, property.property_id, property_value, m_document);
        }
    }

    if (cascade_origin == CascadeOrigin::Author) {
        if (auto const* inline_style = verify_cast<ElementInlineCSSStyleDeclaration>(element.inline_style())) {
            for (auto const& property : inline_style->properties()) {
                if (important != property.important)
                    continue;
                auto property_value = property.value;
                if (property.value->is_unresolved()) {
                    if (auto resolved = resolve_unresolved_style_value(element, property.property_id, property.value->as_unresolved()))
                        property_value = resolved.release_nonnull();
                }
                if (!property_value->is_unresolved())
                    set_property_expanding_shorthands(style, property.property_id, property_value, m_document);
            }
        }
    }
}

static ErrorOr<void> cascade_custom_properties(DOM::Element& element, Vector<MatchingRule> const& matching_rules)
{
    size_t needed_capacity = 0;
    for (auto const& matching_rule : matching_rules)
        needed_capacity += verify_cast<PropertyOwningCSSStyleDeclaration>(matching_rule.rule->declaration()).custom_properties().size();
    if (auto const* inline_style = verify_cast<PropertyOwningCSSStyleDeclaration>(element.inline_style()))
        needed_capacity += inline_style->custom_properties().size();

    HashMap<DeprecatedFlyString, StyleProperty> custom_properties;
    TRY(custom_properties.try_ensure_capacity(needed_capacity));

    for (auto const& matching_rule : matching_rules) {
        for (auto const& it : verify_cast<PropertyOwningCSSStyleDeclaration>(matching_rule.rule->declaration()).custom_properties())
            custom_properties.set(it.key, it.value);
    }

    if (auto const* inline_style = verify_cast<PropertyOwningCSSStyleDeclaration>(element.inline_style())) {
        for (auto const& it : inline_style->custom_properties())
            custom_properties.set(it.key, it.value);
    }

    element.set_custom_properties(move(custom_properties));

    return {};
}

// https://www.w3.org/TR/css-cascade/#cascading
ErrorOr<void> StyleComputer::compute_cascaded_values(StyleProperties& style, DOM::Element& element, Optional<CSS::Selector::PseudoElement> pseudo_element, bool& did_match_any_pseudo_element_rules, ComputeStyleMode mode) const
{
    // First, we collect all the CSS rules whose selectors match `element`:
    MatchingRuleSet matching_rule_set;
    matching_rule_set.user_agent_rules = collect_matching_rules(element, CascadeOrigin::UserAgent, pseudo_element);
    sort_matching_rules(matching_rule_set.user_agent_rules);
    matching_rule_set.author_rules = collect_matching_rules(element, CascadeOrigin::Author, pseudo_element);
    sort_matching_rules(matching_rule_set.author_rules);

    if (mode == ComputeStyleMode::CreatePseudoElementStyleIfNeeded) {
        VERIFY(pseudo_element.has_value());
        if (matching_rule_set.author_rules.is_empty() && matching_rule_set.user_agent_rules.is_empty()) {
            did_match_any_pseudo_element_rules = false;
            return {};
        }
        did_match_any_pseudo_element_rules = true;
    }

    // Then we resolve all the CSS custom properties ("variables") for this element:
    // FIXME: Look into how custom properties should interact with pseudo elements and support that properly.
    if (!pseudo_element.has_value())
        TRY(cascade_custom_properties(element, matching_rule_set.author_rules));

    // Then we apply the declarations from the matched rules in cascade order:

    // Normal user agent declarations
    cascade_declarations(style, element, matching_rule_set.user_agent_rules, CascadeOrigin::UserAgent, Important::No);

    // FIXME: Normal user declarations

    // Author presentational hints (NOTE: The spec doesn't say exactly how to prioritize these.)
    element.apply_presentational_hints(style);

    // Normal author declarations
    cascade_declarations(style, element, matching_rule_set.author_rules, CascadeOrigin::Author, Important::No);

    // FIXME: Animation declarations [css-animations-1]

    // Important author declarations
    cascade_declarations(style, element, matching_rule_set.author_rules, CascadeOrigin::Author, Important::Yes);

    // FIXME: Important user declarations

    // Important user agent declarations
    cascade_declarations(style, element, matching_rule_set.user_agent_rules, CascadeOrigin::UserAgent, Important::Yes);

    // FIXME: Transition declarations [css-transitions-1]

    return {};
}

static DOM::Element const* element_to_inherit_style_from(DOM::Element const* element, Optional<CSS::Selector::PseudoElement> pseudo_element)
{
    // Pseudo-elements treat their originating element as their parent.
    DOM::Element const* parent_element = nullptr;
    if (pseudo_element.has_value()) {
        parent_element = element;
    } else if (element) {
        parent_element = element->parent_or_shadow_host_element();
    }
    return parent_element;
}

static NonnullRefPtr<StyleValue const> get_inherit_value(JS::Realm& initial_value_context_realm, CSS::PropertyID property_id, DOM::Element const* element, Optional<CSS::Selector::PseudoElement> pseudo_element)
{
    auto* parent_element = element_to_inherit_style_from(element, pseudo_element);

    if (!parent_element || !parent_element->computed_css_values())
        return property_initial_value(initial_value_context_realm, property_id);
    return parent_element->computed_css_values()->property(property_id);
};

void StyleComputer::compute_defaulted_property_value(StyleProperties& style, DOM::Element const* element, CSS::PropertyID property_id, Optional<CSS::Selector::PseudoElement> pseudo_element) const
{
    // FIXME: If we don't know the correct initial value for a property, we fall back to InitialStyleValue.

    auto& value_slot = style.m_property_values[to_underlying(property_id)];
    if (!value_slot) {
        if (is_inherited_property(property_id))
            style.m_property_values[to_underlying(property_id)] = get_inherit_value(document().realm(), property_id, element, pseudo_element);
        else
            style.m_property_values[to_underlying(property_id)] = property_initial_value(document().realm(), property_id);
        return;
    }

    if (value_slot->is_initial()) {
        value_slot = property_initial_value(document().realm(), property_id);
        return;
    }

    if (value_slot->is_inherit()) {
        value_slot = get_inherit_value(document().realm(), property_id, element, pseudo_element);
        return;
    }

    // https://www.w3.org/TR/css-cascade-4/#inherit-initial
    // If the cascaded value of a property is the unset keyword,
    if (value_slot->is_unset()) {
        if (is_inherited_property(property_id)) {
            // then if it is an inherited property, this is treated as inherit,
            value_slot = get_inherit_value(document().realm(), property_id, element, pseudo_element);
        } else {
            // and if it is not, this is treated as initial.
            value_slot = property_initial_value(document().realm(), property_id);
        }
    }
}

// https://www.w3.org/TR/css-cascade/#defaulting
void StyleComputer::compute_defaulted_values(StyleProperties& style, DOM::Element const* element, Optional<CSS::Selector::PseudoElement> pseudo_element) const
{
    // Walk the list of all known CSS properties and:
    // - Add them to `style` if they are missing.
    // - Resolve `inherit` and `initial` as needed.
    for (auto i = to_underlying(CSS::first_longhand_property_id); i <= to_underlying(CSS::last_longhand_property_id); ++i) {
        auto property_id = (CSS::PropertyID)i;
        compute_defaulted_property_value(style, element, property_id, pseudo_element);
    }
}

CSSPixels StyleComputer::root_element_font_size() const
{
    constexpr float default_root_element_font_size = 16;

    auto const* root_element = m_document->first_child_of_type<HTML::HTMLHtmlElement>();
    if (!root_element)
        return default_root_element_font_size;

    auto const* computed_root_style = root_element->computed_css_values();
    if (!computed_root_style)
        return default_root_element_font_size;

    auto root_value = computed_root_style->property(CSS::PropertyID::FontSize);

    auto font_metrics = computed_root_style->computed_font().pixel_metrics();
    auto line_height = font_metrics.line_spacing();
    return root_value->to_length().to_px(viewport_rect(), font_metrics, default_root_element_font_size, default_root_element_font_size, line_height, line_height);
}

CSSPixels StyleComputer::root_element_line_height() const
{
    constexpr float default_root_element_line_height = 16;

    auto const* root_element = m_document->first_child_of_type<HTML::HTMLHtmlElement>();
    if (!root_element)
        return default_root_element_line_height;

    auto const* computed_root_style = root_element->computed_css_values();
    if (!computed_root_style)
        return default_root_element_line_height;

    auto font_metrics = computed_root_style->computed_font().pixel_metrics();
    auto font_size = root_element_font_size();
    auto line_height = font_metrics.line_spacing();
    return computed_root_style->line_height(viewport_rect(), font_metrics, font_size, font_size, line_height, line_height);
}

void StyleComputer::compute_font(StyleProperties& style, DOM::Element const* element, Optional<CSS::Selector::PseudoElement> pseudo_element) const
{
    // To compute the font, first ensure that we've defaulted the relevant CSS font properties.
    // FIXME: This should be more sophisticated.
    compute_defaulted_property_value(style, element, CSS::PropertyID::FontFamily, pseudo_element);
    compute_defaulted_property_value(style, element, CSS::PropertyID::FontSize, pseudo_element);
    compute_defaulted_property_value(style, element, CSS::PropertyID::FontStretch, pseudo_element);
    compute_defaulted_property_value(style, element, CSS::PropertyID::FontStyle, pseudo_element);
    compute_defaulted_property_value(style, element, CSS::PropertyID::FontWeight, pseudo_element);
    compute_defaulted_property_value(style, element, CSS::PropertyID::LineHeight, pseudo_element);

    auto* parent_element = element_to_inherit_style_from(element, pseudo_element);

    auto font_size = style.property(CSS::PropertyID::FontSize);
    auto font_style = style.property(CSS::PropertyID::FontStyle);
    auto font_weight = style.property(CSS::PropertyID::FontWeight);
    auto font_stretch = style.property(CSS::PropertyID::FontStretch);

    int width = Gfx::FontWidth::Normal;
    if (font_stretch->is_identifier()) {
        switch (static_cast<IdentifierStyleValue const&>(*font_stretch).id()) {
        case CSS::ValueID::UltraCondensed:
            width = Gfx::FontWidth::UltraCondensed;
            break;
        case CSS::ValueID::ExtraCondensed:
            width = Gfx::FontWidth::ExtraCondensed;
            break;
        case CSS::ValueID::Condensed:
            width = Gfx::FontWidth::Condensed;
            break;
        case CSS::ValueID::SemiCondensed:
            width = Gfx::FontWidth::SemiCondensed;
            break;
        case CSS::ValueID::Normal:
            width = Gfx::FontWidth::Normal;
            break;
        case CSS::ValueID::SemiExpanded:
            width = Gfx::FontWidth::SemiExpanded;
            break;
        case CSS::ValueID::Expanded:
            width = Gfx::FontWidth::Expanded;
            break;
        case CSS::ValueID::ExtraExpanded:
            width = Gfx::FontWidth::ExtraExpanded;
            break;
        case CSS::ValueID::UltraExpanded:
            width = Gfx::FontWidth::UltraExpanded;
            break;
        default:
            break;
        }
    } else if (font_stretch->is_percentage()) {
        float percentage = font_stretch->as_percentage().percentage().value();
        if (percentage <= 50) {
            width = Gfx::FontWidth::UltraCondensed;
        } else if (percentage <= 62.5f) {
            width = Gfx::FontWidth::ExtraCondensed;
        } else if (percentage <= 75.0f) {
            width = Gfx::FontWidth::Condensed;
        } else if (percentage <= 87.5f) {
            width = Gfx::FontWidth::SemiCondensed;
        } else if (percentage <= 100.0f) {
            width = Gfx::FontWidth::Normal;
        } else if (percentage <= 112.5f) {
            width = Gfx::FontWidth::SemiExpanded;
        } else if (percentage <= 125.0f) {
            width = Gfx::FontWidth::Expanded;
        } else if (percentage <= 150.0f) {
            width = Gfx::FontWidth::ExtraExpanded;
        } else {
            width = Gfx::FontWidth::UltraExpanded;
        }
    }

    int weight = Gfx::FontWeight::Regular;
    if (font_weight->is_identifier()) {
        switch (static_cast<IdentifierStyleValue const&>(*font_weight).id()) {
        case CSS::ValueID::Normal:
            weight = Gfx::FontWeight::Regular;
            break;
        case CSS::ValueID::Bold:
            weight = Gfx::FontWeight::Bold;
            break;
        case CSS::ValueID::Lighter:
            // FIXME: This should be relative to the parent.
            weight = Gfx::FontWeight::Regular;
            break;
        case CSS::ValueID::Bolder:
            // FIXME: This should be relative to the parent.
            weight = Gfx::FontWeight::Bold;
            break;
        default:
            break;
        }
    } else if (font_weight->has_integer()) {
        int font_weight_integer = font_weight->to_integer();
        if (font_weight_integer <= Gfx::FontWeight::Regular)
            weight = Gfx::FontWeight::Regular;
        else if (font_weight_integer <= Gfx::FontWeight::Bold)
            weight = Gfx::FontWeight::Bold;
        else
            weight = Gfx::FontWeight::Black;
    } else if (font_weight->is_calculated()) {
        auto maybe_weight = const_cast<CalculatedStyleValue&>(font_weight->as_calculated()).resolve_integer();
        if (maybe_weight.has_value())
            weight = maybe_weight.value();
    }

    bool bold = weight > Gfx::FontWeight::Regular;

    // FIXME: Should be based on "user's default font size"
    float font_size_in_px = 16;

    if (font_size->is_identifier()) {
        // https://w3c.github.io/csswg-drafts/css-fonts/#absolute-size-mapping
        AK::HashMap<Web::CSS::ValueID, float> absolute_size_mapping = {
            { CSS::ValueID::XxSmall, 0.6 },
            { CSS::ValueID::XSmall, 0.75 },
            { CSS::ValueID::Small, 8.0 / 9.0 },
            { CSS::ValueID::Medium, 1.0 },
            { CSS::ValueID::Large, 1.2 },
            { CSS::ValueID::XLarge, 1.5 },
            { CSS::ValueID::XxLarge, 2.0 },
            { CSS::ValueID::XxxLarge, 3.0 },
            { CSS::ValueID::Smaller, 0.8 },
            { CSS::ValueID::Larger, 1.25 },
        };

        auto const identifier = static_cast<IdentifierStyleValue const&>(*font_size).id();

        // https://w3c.github.io/csswg-drafts/css-fonts/#valdef-font-size-relative-size
        // TODO: If the parent element has a keyword font size in the absolute size keyword mapping table,
        //       larger may compute the font size to the next entry in the table,
        //       and smaller may compute the font size to the previous entry in the table.
        if (identifier == CSS::ValueID::Smaller || identifier == CSS::ValueID::Larger) {
            if (parent_element && parent_element->computed_css_values()) {
                font_size_in_px = parent_element->computed_css_values()->computed_font().pixel_metrics().size;
            }
        }
        auto const multiplier = absolute_size_mapping.get(identifier).value_or(1.0);
        font_size_in_px *= multiplier;

    } else {
        auto root_font_size = root_element_font_size();
        auto root_line_height = root_element_line_height();

        Gfx::FontPixelMetrics font_metrics;
        if (parent_element && parent_element->computed_css_values())
            font_metrics = parent_element->computed_css_values()->computed_font().pixel_metrics();
        else
            font_metrics = Platform::FontPlugin::the().default_font().pixel_metrics();

        auto parent_font_size = [&]() -> CSSPixels {
            if (!parent_element || !parent_element->computed_css_values())
                return font_size_in_px;
            auto value = parent_element->computed_css_values()->property(CSS::PropertyID::FontSize);
            if (value->is_length()) {
                auto length = static_cast<LengthStyleValue const&>(*value).to_length();
                auto parent_line_height = parent_or_root_element_line_height(parent_element, {});
                if (length.is_absolute() || length.is_relative())
                    return length.to_px(viewport_rect(), font_metrics, font_size_in_px, root_font_size, parent_line_height, root_line_height);
            }
            return font_size_in_px;
        };

        Optional<Length> maybe_length;
        if (font_size->is_percentage()) {
            // Percentages refer to parent element's font size
            maybe_length = Length::make_px(font_size->as_percentage().percentage().as_fraction() * parent_font_size());

        } else if (font_size->is_length()) {
            maybe_length = font_size->to_length();

        } else if (font_size->is_calculated()) {
            // FIXME: Support font-size: calc(...)
            //        Theoretically we can do this now, but to resolve it we need a layout_node which we might not have. :^(
        }
        if (maybe_length.has_value()) {
            auto parent_line_height = parent_or_root_element_line_height(element, pseudo_element);
            auto px = maybe_length.value().to_px(viewport_rect(), font_metrics, parent_font_size(), root_font_size, parent_line_height, root_line_height).value();
            if (px != 0)
                font_size_in_px = px;
        }
    }

    int slope = Gfx::name_to_slope("Normal"sv);
    // FIXME: Implement oblique <angle>
    if (font_style->is_identifier()) {
        switch (static_cast<IdentifierStyleValue const&>(*font_style).id()) {
        case CSS::ValueID::Italic:
            slope = Gfx::name_to_slope("Italic"sv);
            break;
        case CSS::ValueID::Oblique:
            slope = Gfx::name_to_slope("Oblique"sv);
            break;
        case CSS::ValueID::Normal:
        default:
            break;
        }
    }

    // FIXME: Implement the full font-matching algorithm: https://www.w3.org/TR/css-fonts-4/#font-matching-algorithm

    // Note: This is modified by the find_font() lambda
    FontSelector font_selector;
    bool monospace = false;

    auto find_font = [&](String const& family) -> RefPtr<Gfx::Font const> {
        float font_size_in_pt = font_size_in_px * 0.75f;
        font_selector = { family, font_size_in_pt, weight, width, slope };

        if (auto it = m_loaded_fonts.find(family); it != m_loaded_fonts.end()) {
            auto& loader = *it->value;
            if (auto found_font = loader.font_with_point_size(font_size_in_pt))
                return found_font;
        }

        if (auto found_font = FontCache::the().get(font_selector))
            return found_font;

        if (auto found_font = Gfx::FontDatabase::the().get(family.to_deprecated_string(), font_size_in_pt, weight, width, slope, Gfx::Font::AllowInexactSizeMatch::Yes))
            return found_font;

        return {};
    };

    auto find_generic_font = [&](ValueID font_id) -> RefPtr<Gfx::Font const> {
        Platform::GenericFont generic_font {};
        switch (font_id) {
        case ValueID::Monospace:
        case ValueID::UiMonospace:
            generic_font = Platform::GenericFont::Monospace;
            monospace = true;
            break;
        case ValueID::Serif:
            generic_font = Platform::GenericFont::Serif;
            break;
        case ValueID::Fantasy:
            generic_font = Platform::GenericFont::Fantasy;
            break;
        case ValueID::SansSerif:
            generic_font = Platform::GenericFont::SansSerif;
            break;
        case ValueID::Cursive:
            generic_font = Platform::GenericFont::Cursive;
            break;
        case ValueID::UiSerif:
            generic_font = Platform::GenericFont::UiSerif;
            break;
        case ValueID::UiSansSerif:
            generic_font = Platform::GenericFont::UiSansSerif;
            break;
        case ValueID::UiRounded:
            generic_font = Platform::GenericFont::UiRounded;
            break;
        default:
            return {};
        }
        return find_font(String::from_utf8(Platform::FontPlugin::the().generic_font_name(generic_font)).release_value_but_fixme_should_propagate_errors());
    };

    RefPtr<Gfx::Font const> found_font;

    auto family_value = style.property(PropertyID::FontFamily);
    if (family_value->is_value_list()) {
        auto const& family_list = static_cast<StyleValueList const&>(*family_value).values();
        for (auto const& family : family_list) {
            if (family->is_identifier()) {
                found_font = find_generic_font(family->to_identifier());
            } else if (family->is_string()) {
                found_font = find_font(family->to_string().release_value_but_fixme_should_propagate_errors());
            }
            if (found_font)
                break;
        }
    } else if (family_value->is_identifier()) {
        found_font = find_generic_font(family_value->to_identifier());
    } else if (family_value->is_string()) {
        found_font = find_font(family_value->to_string().release_value_but_fixme_should_propagate_errors());
    }

    if (!found_font) {
        found_font = StyleProperties::font_fallback(monospace, bold);
    }

    FontCache::the().set(font_selector, *found_font);

    style.set_property(CSS::PropertyID::FontSize, LengthStyleValue::create(CSS::Length::make_px(font_size_in_px)));
    style.set_property(CSS::PropertyID::FontWeight, NumericStyleValue::create_integer(weight));

    style.set_computed_font(found_font.release_nonnull());
}

Gfx::Font const& StyleComputer::initial_font() const
{
    // FIXME: This is not correct.
    return StyleProperties::font_fallback(false, false);
}

CSSPixels StyleComputer::parent_or_root_element_line_height(DOM::Element const* element, Optional<CSS::Selector::PseudoElement> pseudo_element) const
{
    auto* parent_element = element_to_inherit_style_from(element, pseudo_element);
    if (!parent_element)
        return root_element_line_height();
    auto root_font_size = root_element_font_size();
    auto root_line_height = root_element_line_height();
    auto* computed_values = parent_element->computed_css_values();
    auto parent_font_size = computed_values->property(CSS::PropertyID::FontSize)->to_length();
    // FIXME: Can the parent font size be non-absolute here?
    auto parent_font_size_value = parent_font_size.is_absolute() ? parent_font_size.absolute_length_to_px() : root_font_size;
    auto parent_parent_line_height = parent_or_root_element_line_height(parent_element, {});
    return computed_values->line_height(viewport_rect(), computed_values->computed_font().pixel_metrics(), parent_font_size_value, root_font_size, parent_parent_line_height, root_line_height);
}

void StyleComputer::absolutize_values(StyleProperties& style, DOM::Element const* element, Optional<CSS::Selector::PseudoElement> pseudo_element) const
{
    auto root_line_height = root_element_line_height();
    auto parent_or_root_line_height = parent_or_root_element_line_height(element, pseudo_element);

    auto font_metrics = style.computed_font().pixel_metrics();
    auto root_font_size = root_element_font_size();
    auto font_size = style.property(CSS::PropertyID::FontSize)->to_length().to_px(viewport_rect(), font_metrics, root_font_size, root_font_size, parent_or_root_line_height, root_line_height);

    // NOTE: Percentage line-height values are relative to the font-size of the element.
    //       We have to resolve them right away, so that the *computed* line-height is ready for inheritance.
    //       We can't simply absolutize *all* percentage values against the font size,
    //       because most percentages are relative to containing block metrics.
    auto& line_height_value_slot = style.m_property_values[to_underlying(CSS::PropertyID::LineHeight)];
    if (line_height_value_slot && line_height_value_slot->is_percentage()) {
        line_height_value_slot = LengthStyleValue::create(
            Length::make_px(font_size * line_height_value_slot->as_percentage().percentage().as_fraction()));
    }

    auto line_height = style.line_height(viewport_rect(), font_metrics, font_size.value(), root_font_size.value(), parent_or_root_line_height, root_line_height);

    // NOTE: line-height might be using lh which should be resolved against the parent line height (like we did here already)
    if (line_height_value_slot && line_height_value_slot->is_length())
        line_height_value_slot = LengthStyleValue::create(Length::make_px(line_height));

    for (size_t i = 0; i < style.m_property_values.size(); ++i) {
        auto& value_slot = style.m_property_values[i];
        if (!value_slot)
            continue;
        value_slot = value_slot->absolutized(viewport_rect(), font_metrics, font_size.value(), root_font_size.value(), line_height, root_line_height);
    }
}

enum class BoxTypeTransformation {
    None,
    Blockify,
    Inlinify,
};

static BoxTypeTransformation required_box_type_transformation(StyleProperties const& style, DOM::Element const& element, Optional<CSS::Selector::PseudoElement> const&)
{
    // Absolute positioning or floating an element blockifies the box’s display type. [CSS2]
    if (style.position() == CSS::Position::Absolute || style.position() == CSS::Position::Fixed || style.float_() != CSS::Float::None)
        return BoxTypeTransformation::Blockify;

    // FIXME: Containment in a ruby container inlinifies the box’s display type, as described in [CSS-RUBY-1].

    // A parent with a grid or flex display value blockifies the box’s display type. [CSS-GRID-1] [CSS-FLEXBOX-1]
    if (element.parent_element() && element.parent_element()->computed_css_values()) {
        auto const& parent_display = element.parent_element()->computed_css_values()->display();
        if (parent_display.is_grid_inside() || parent_display.is_flex_inside())
            return BoxTypeTransformation::Blockify;
    }

    return BoxTypeTransformation::None;
}

// https://drafts.csswg.org/css-display/#transformations
void StyleComputer::transform_box_type_if_needed(StyleProperties& style, DOM::Element const& element, Optional<CSS::Selector::PseudoElement> pseudo_element) const
{
    // 2.7. Automatic Box Type Transformations

    // Some layout effects require blockification or inlinification of the box type,
    // which sets the box’s computed outer display type to block or inline (respectively).
    // (This has no effect on display types that generate no box at all, such as none or contents.)

    // FIXME: If a block box (block flow) is inlinified, its inner display type is set to flow-root so that it remains a block container.
    //
    // FIXME: If an inline box (inline flow) is inlinified, it recursively inlinifies all of its in-flow children,
    //        so that no block-level descendants break up the inline formatting context in which it participates.
    //
    // FIXME: For legacy reasons, if an inline block box (inline flow-root) is blockified, it becomes a block box (losing its flow-root nature).
    //        For consistency, a run-in flow-root box also blockifies to a block box.
    //
    // FIXME: If a layout-internal box is blockified, its inner display type converts to flow so that it becomes a block container.
    //        Inlinification has no effect on layout-internal boxes. (However, placement in such an inline context will typically cause them
    //        to be wrapped in an appropriately-typed anonymous inline-level box.)

    auto display = style.display();
    if (display.is_none() || display.is_contents())
        return;

    switch (required_box_type_transformation(style, element, pseudo_element)) {
    case BoxTypeTransformation::None:
        break;
    case BoxTypeTransformation::Blockify:
        if (!display.is_block_outside()) {
            // FIXME: We only want to change the outer display type here, but we don't have a nice API
            //        to do that specifically. For now, we simply check for "inline-flex" and convert
            //        that to "flex".
            if (display.is_flex_inside())
                style.set_property(CSS::PropertyID::Display, IdentifierStyleValue::create(CSS::ValueID::Flex));
            else
                style.set_property(CSS::PropertyID::Display, IdentifierStyleValue::create(CSS::ValueID::Block));
        }
        break;
    case BoxTypeTransformation::Inlinify:
        if (!display.is_inline_outside())
            style.set_property(CSS::PropertyID::Display, IdentifierStyleValue::create(CSS::ValueID::Inline));
        break;
    }
}

NonnullRefPtr<StyleProperties> StyleComputer::create_document_style() const
{
    auto style = StyleProperties::create();
    compute_font(style, nullptr, {});
    compute_defaulted_values(style, nullptr, {});
    absolutize_values(style, nullptr, {});
    style->set_property(CSS::PropertyID::Width, CSS::LengthStyleValue::create(CSS::Length::make_px(viewport_rect().width())));
    style->set_property(CSS::PropertyID::Height, CSS::LengthStyleValue::create(CSS::Length::make_px(viewport_rect().height())));
    style->set_property(CSS::PropertyID::Display, CSS::IdentifierStyleValue::create(CSS::ValueID::Block));
    return style;
}

ErrorOr<NonnullRefPtr<StyleProperties>> StyleComputer::compute_style(DOM::Element& element, Optional<CSS::Selector::PseudoElement> pseudo_element) const
{
    auto style = TRY(compute_style_impl(element, move(pseudo_element), ComputeStyleMode::Normal));
    return style.release_nonnull();
}

ErrorOr<RefPtr<StyleProperties>> StyleComputer::compute_pseudo_element_style_if_needed(DOM::Element& element, Optional<CSS::Selector::PseudoElement> pseudo_element) const
{
    return compute_style_impl(element, move(pseudo_element), ComputeStyleMode::CreatePseudoElementStyleIfNeeded);
}

ErrorOr<RefPtr<StyleProperties>> StyleComputer::compute_style_impl(DOM::Element& element, Optional<CSS::Selector::PseudoElement> pseudo_element, ComputeStyleMode mode) const
{
    build_rule_cache_if_needed();

    auto style = StyleProperties::create();
    // 1. Perform the cascade. This produces the "specified style"
    bool did_match_any_pseudo_element_rules = false;
    TRY(compute_cascaded_values(style, element, pseudo_element, did_match_any_pseudo_element_rules, mode));

    if (mode == ComputeStyleMode::CreatePseudoElementStyleIfNeeded && !did_match_any_pseudo_element_rules)
        return nullptr;

    // 2. Compute the font, since that may be needed for font-relative CSS units
    compute_font(style, &element, pseudo_element);

    // 3. Absolutize values, turning font/viewport relative lengths into absolute lengths
    absolutize_values(style, &element, pseudo_element);

    // 4. Default the values, applying inheritance and 'initial' as needed
    compute_defaulted_values(style, &element, pseudo_element);

    // 5. Run automatic box type transformations
    transform_box_type_if_needed(style, element, pseudo_element);

    return style;
}

PropertyDependencyNode::PropertyDependencyNode(String name)
    : m_name(move(name))
{
}

void PropertyDependencyNode::add_child(NonnullRefPtr<PropertyDependencyNode> new_child)
{
    for (auto const& child : m_children) {
        if (child->m_name == new_child->m_name)
            return;
    }

    // We detect self-reference already.
    VERIFY(new_child->m_name != m_name);
    m_children.append(move(new_child));
}

bool PropertyDependencyNode::has_cycles()
{
    if (m_marked)
        return true;

    TemporaryChange change { m_marked, true };
    for (auto& child : m_children) {
        if (child->has_cycles())
            return true;
    }
    return false;
}

void StyleComputer::build_rule_cache_if_needed() const
{
    if (m_author_rule_cache && m_user_agent_rule_cache)
        return;
    const_cast<StyleComputer&>(*this).build_rule_cache();
}

NonnullOwnPtr<StyleComputer::RuleCache> StyleComputer::make_rule_cache_for_cascade_origin(CascadeOrigin cascade_origin)
{
    auto rule_cache = make<RuleCache>();

    size_t num_class_rules = 0;
    size_t num_id_rules = 0;
    size_t num_tag_name_rules = 0;
    size_t num_pseudo_element_rules = 0;

    Vector<MatchingRule> matching_rules;
    size_t style_sheet_index = 0;
    for_each_stylesheet(cascade_origin, [&](auto& sheet) {
        size_t rule_index = 0;
        sheet.for_each_effective_style_rule([&](auto const& rule) {
            size_t selector_index = 0;
            for (CSS::Selector const& selector : rule.selectors()) {
                MatchingRule matching_rule {
                    &rule,
                    style_sheet_index,
                    rule_index,
                    selector_index,
                    selector.specificity(),
                };

                for (auto const& simple_selector : selector.compound_selectors().last().simple_selectors) {
                    if (simple_selector.type == CSS::Selector::SimpleSelector::Type::PseudoElement) {
                        matching_rule.contains_pseudo_element = true;
                        ++num_pseudo_element_rules;
                        break;
                    }
                }

                bool added_to_bucket = false;
                for (auto const& simple_selector : selector.compound_selectors().last().simple_selectors) {
                    if (simple_selector.type == CSS::Selector::SimpleSelector::Type::Id) {
                        rule_cache->rules_by_id.ensure(simple_selector.name()).append(move(matching_rule));
                        ++num_id_rules;
                        added_to_bucket = true;
                        break;
                    }
                    if (simple_selector.type == CSS::Selector::SimpleSelector::Type::Class) {
                        rule_cache->rules_by_class.ensure(simple_selector.name()).append(move(matching_rule));
                        ++num_class_rules;
                        added_to_bucket = true;
                        break;
                    }
                    if (simple_selector.type == CSS::Selector::SimpleSelector::Type::TagName) {
                        rule_cache->rules_by_tag_name.ensure(simple_selector.name()).append(move(matching_rule));
                        ++num_tag_name_rules;
                        added_to_bucket = true;
                        break;
                    }
                }
                if (!added_to_bucket)
                    rule_cache->other_rules.append(move(matching_rule));

                ++selector_index;
            }
            ++rule_index;
        });
        ++style_sheet_index;
    });

    if constexpr (LIBWEB_CSS_DEBUG) {
        dbgln("Built rule cache!");
        dbgln("           ID: {}", num_id_rules);
        dbgln("        Class: {}", num_class_rules);
        dbgln("      TagName: {}", num_tag_name_rules);
        dbgln("PseudoElement: {}", num_pseudo_element_rules);
        dbgln("        Other: {}", rule_cache->other_rules.size());
        dbgln("        Total: {}", num_class_rules + num_id_rules + num_tag_name_rules + rule_cache->other_rules.size());
    }
    return rule_cache;
}

void StyleComputer::build_rule_cache()
{
    m_author_rule_cache = make_rule_cache_for_cascade_origin(CascadeOrigin::Author);
    m_user_agent_rule_cache = make_rule_cache_for_cascade_origin(CascadeOrigin::UserAgent);
}

void StyleComputer::invalidate_rule_cache()
{
    m_author_rule_cache = nullptr;

    // NOTE: It might not be necessary to throw away the UA rule cache.
    //       If we are sure that it's safe, we could keep it as an optimization.
    m_user_agent_rule_cache = nullptr;
}

CSSPixelRect StyleComputer::viewport_rect() const
{
    if (auto const* browsing_context = document().browsing_context())
        return browsing_context->viewport_rect();
    return {};
}

void StyleComputer::did_load_font([[maybe_unused]] FlyString const& family_name)
{
    document().invalidate_style();
}

void StyleComputer::load_fonts_from_sheet(CSSStyleSheet const& sheet)
{
    for (auto const& rule : static_cast<CSSStyleSheet const&>(sheet).rules()) {
        if (!is<CSSFontFaceRule>(*rule))
            continue;
        auto const& font_face = static_cast<CSSFontFaceRule const&>(*rule).font_face();
        if (font_face.sources().is_empty())
            continue;
        if (m_loaded_fonts.contains(font_face.font_family()))
            continue;

        Vector<AK::URL> urls;
        for (auto& source : font_face.sources()) {
            // FIXME: These should be loaded relative to the stylesheet URL instead of the document URL.
            urls.append(m_document->parse_url(source.url.to_deprecated_string()));
        }

        if (urls.is_empty())
            continue;

        auto loader = make<FontLoader>(const_cast<StyleComputer&>(*this), font_face.font_family(), move(urls));
        const_cast<StyleComputer&>(*this).m_loaded_fonts.set(font_face.font_family().to_string(), move(loader));
    }
}

}
