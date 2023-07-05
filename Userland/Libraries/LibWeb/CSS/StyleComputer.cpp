/*
 * Copyright (c) 2018-2023, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, the SerenityOS developers.
 * Copyright (c) 2021-2023, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Debug.h>
#include <AK/Error.h>
#include <AK/Find.h>
#include <AK/Function.h>
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
#include <LibWeb/CSS/StyleValues/AngleStyleValue.h>
#include <LibWeb/CSS/StyleValues/BackgroundStyleValue.h>
#include <LibWeb/CSS/StyleValues/BorderRadiusShorthandStyleValue.h>
#include <LibWeb/CSS/StyleValues/BorderRadiusStyleValue.h>
#include <LibWeb/CSS/StyleValues/BorderStyleValue.h>
#include <LibWeb/CSS/StyleValues/ColorStyleValue.h>
#include <LibWeb/CSS/StyleValues/CompositeStyleValue.h>
#include <LibWeb/CSS/StyleValues/DisplayStyleValue.h>
#include <LibWeb/CSS/StyleValues/FilterValueListStyleValue.h>
#include <LibWeb/CSS/StyleValues/FlexFlowStyleValue.h>
#include <LibWeb/CSS/StyleValues/FlexStyleValue.h>
#include <LibWeb/CSS/StyleValues/FontStyleValue.h>
#include <LibWeb/CSS/StyleValues/GridAreaShorthandStyleValue.h>
#include <LibWeb/CSS/StyleValues/GridTrackPlacementShorthandStyleValue.h>
#include <LibWeb/CSS/StyleValues/GridTrackPlacementStyleValue.h>
#include <LibWeb/CSS/StyleValues/GridTrackSizeListShorthandStyleValue.h>
#include <LibWeb/CSS/StyleValues/GridTrackSizeListStyleValue.h>
#include <LibWeb/CSS/StyleValues/IdentifierStyleValue.h>
#include <LibWeb/CSS/StyleValues/IntegerStyleValue.h>
#include <LibWeb/CSS/StyleValues/LengthStyleValue.h>
#include <LibWeb/CSS/StyleValues/ListStyleStyleValue.h>
#include <LibWeb/CSS/StyleValues/NumberStyleValue.h>
#include <LibWeb/CSS/StyleValues/OverflowStyleValue.h>
#include <LibWeb/CSS/StyleValues/PercentageStyleValue.h>
#include <LibWeb/CSS/StyleValues/PlaceContentStyleValue.h>
#include <LibWeb/CSS/StyleValues/PositionStyleValue.h>
#include <LibWeb/CSS/StyleValues/RectStyleValue.h>
#include <LibWeb/CSS/StyleValues/StyleValueList.h>
#include <LibWeb/CSS/StyleValues/TextDecorationStyleValue.h>
#include <LibWeb/CSS/StyleValues/TimeStyleValue.h>
#include <LibWeb/CSS/StyleValues/TransformationStyleValue.h>
#include <LibWeb/CSS/StyleValues/UnresolvedStyleValue.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/Element.h>
#include <LibWeb/FontCache.h>
#include <LibWeb/HTML/HTMLHtmlElement.h>
#include <LibWeb/Layout/Node.h>
#include <LibWeb/Loader/ResourceLoader.h>
#include <LibWeb/Platform/FontPlugin.h>
#include <stdio.h>

namespace AK {

// traits for FontFaceKey
template<>
struct Traits<Web::CSS::FontFaceKey> : public GenericTraits<Web::CSS::FontFaceKey> {
    static unsigned hash(Web::CSS::FontFaceKey const& key) { return pair_int_hash(key.family_name.hash(), pair_int_hash(key.weight, key.slope)); }
};

}

namespace Web::CSS {

StyleComputer::StyleComputer(DOM::Document& document)
    : m_document(document)
    , m_default_font_metrics(16, Gfx::FontDatabase::default_font().pixel_metrics(), 16)
    , m_root_element_font_metrics(m_default_font_metrics)
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

static void set_property_expanding_shorthands(StyleProperties& style, CSS::PropertyID property_id, StyleValue const& value, DOM::Document& document, CSS::CSSStyleDeclaration const* declaration)
{
    auto map_logical_property_to_real_property = [](PropertyID property_id) -> Optional<PropertyID> {
        // FIXME: Honor writing-mode, direction and text-orientation.
        switch (property_id) {
        case PropertyID::MarginBlockStart:
            return PropertyID::MarginTop;
        case PropertyID::MarginBlockEnd:
            return PropertyID::MarginBottom;
        case PropertyID::MarginInlineStart:
            return PropertyID::MarginLeft;
        case PropertyID::MarginInlineEnd:
            return PropertyID::MarginRight;
        case PropertyID::PaddingBlockStart:
            return PropertyID::PaddingTop;
        case PropertyID::PaddingBlockEnd:
            return PropertyID::PaddingBottom;
        case PropertyID::PaddingInlineStart:
            return PropertyID::PaddingLeft;
        case PropertyID::PaddingInlineEnd:
            return PropertyID::PaddingRight;
        default:
            return {};
        }
    };

    struct StartAndEndPropertyIDs {
        PropertyID start;
        PropertyID end;
    };
    auto map_logical_property_to_real_properties = [](PropertyID property_id) -> Optional<StartAndEndPropertyIDs> {
        // FIXME: Honor writing-mode, direction and text-orientation.
        switch (property_id) {
        case PropertyID::MarginBlock:
            return StartAndEndPropertyIDs { PropertyID::MarginTop, PropertyID::MarginBottom };
        case PropertyID::MarginInline:
            return StartAndEndPropertyIDs { PropertyID::MarginLeft, PropertyID::MarginRight };
        case PropertyID::PaddingBlock:
            return StartAndEndPropertyIDs { PropertyID::PaddingTop, PropertyID::PaddingBottom };
        case PropertyID::PaddingInline:
            return StartAndEndPropertyIDs { PropertyID::PaddingLeft, PropertyID::PaddingRight };
        default:
            return {};
        }
    };

    if (auto real_property_id = map_logical_property_to_real_property(property_id); real_property_id.has_value())
        return set_property_expanding_shorthands(style, real_property_id.value(), value, document, declaration);

    if (auto real_property_ids = map_logical_property_to_real_properties(property_id); real_property_ids.has_value()) {
        if (value.is_value_list() && value.as_value_list().size() == 2) {
            auto const& start = value.as_value_list().values()[0];
            auto const& end = value.as_value_list().values()[1];
            set_property_expanding_shorthands(style, real_property_ids->start, start, document, declaration);
            set_property_expanding_shorthands(style, real_property_ids->end, end, document, declaration);
            return;
        }
        set_property_expanding_shorthands(style, real_property_ids->start, value, document, declaration);
        set_property_expanding_shorthands(style, real_property_ids->end, value, document, declaration);
        return;
    }

    if (value.is_composite()) {
        auto& composite_value = value.as_composite();
        auto& properties = composite_value.sub_properties();
        auto& values = composite_value.values();
        for (size_t i = 0; i < properties.size(); ++i)
            set_property_expanding_shorthands(style, properties[i], values[i], document, declaration);
    }

    auto assign_edge_values = [&style, &declaration](PropertyID top_property, PropertyID right_property, PropertyID bottom_property, PropertyID left_property, auto const& values) {
        if (values.size() == 4) {
            style.set_property(top_property, values[0], declaration);
            style.set_property(right_property, values[1], declaration);
            style.set_property(bottom_property, values[2], declaration);
            style.set_property(left_property, values[3], declaration);
        } else if (values.size() == 3) {
            style.set_property(top_property, values[0], declaration);
            style.set_property(right_property, values[1], declaration);
            style.set_property(bottom_property, values[2], declaration);
            style.set_property(left_property, values[1], declaration);
        } else if (values.size() == 2) {
            style.set_property(top_property, values[0], declaration);
            style.set_property(right_property, values[1], declaration);
            style.set_property(bottom_property, values[0], declaration);
            style.set_property(left_property, values[1], declaration);
        } else if (values.size() == 1) {
            style.set_property(top_property, values[0], declaration);
            style.set_property(right_property, values[0], declaration);
            style.set_property(bottom_property, values[0], declaration);
            style.set_property(left_property, values[0], declaration);
        }
    };

    if (property_id == CSS::PropertyID::TextDecoration) {
        if (value.is_text_decoration()) {
            auto const& text_decoration = value.as_text_decoration();
            style.set_property(CSS::PropertyID::TextDecorationLine, text_decoration.line(), declaration);
            style.set_property(CSS::PropertyID::TextDecorationThickness, text_decoration.thickness(), declaration);
            style.set_property(CSS::PropertyID::TextDecorationStyle, text_decoration.style(), declaration);
            style.set_property(CSS::PropertyID::TextDecorationColor, text_decoration.color(), declaration);
            return;
        }

        style.set_property(CSS::PropertyID::TextDecorationLine, value, declaration);
        style.set_property(CSS::PropertyID::TextDecorationThickness, value, declaration);
        style.set_property(CSS::PropertyID::TextDecorationStyle, value, declaration);
        style.set_property(CSS::PropertyID::TextDecorationColor, value, declaration);
        return;
    }

    if (property_id == CSS::PropertyID::Overflow) {
        if (value.is_overflow()) {
            auto const& overflow = value.as_overflow();
            style.set_property(CSS::PropertyID::OverflowX, overflow.overflow_x(), declaration);
            style.set_property(CSS::PropertyID::OverflowY, overflow.overflow_y(), declaration);
            return;
        }

        style.set_property(CSS::PropertyID::OverflowX, value, declaration);
        style.set_property(CSS::PropertyID::OverflowY, value, declaration);
        return;
    }

    if (property_id == CSS::PropertyID::PlaceContent) {
        if (value.is_place_content()) {
            auto const& place_content = value.as_place_content();
            style.set_property(CSS::PropertyID::AlignContent, place_content.align_content());
            style.set_property(CSS::PropertyID::JustifyContent, place_content.justify_content());
            return;
        }

        style.set_property(CSS::PropertyID::AlignContent, value);
        style.set_property(CSS::PropertyID::JustifyContent, value);
        return;
    }

    if (property_id == CSS::PropertyID::Border) {
        set_property_expanding_shorthands(style, CSS::PropertyID::BorderTop, value, document, declaration);
        set_property_expanding_shorthands(style, CSS::PropertyID::BorderRight, value, document, declaration);
        set_property_expanding_shorthands(style, CSS::PropertyID::BorderBottom, value, document, declaration);
        set_property_expanding_shorthands(style, CSS::PropertyID::BorderLeft, value, document, declaration);
        // FIXME: Also reset border-image, in line with the spec: https://www.w3.org/TR/css-backgrounds-3/#border-shorthands
        return;
    }

    if (property_id == CSS::PropertyID::BorderRadius) {
        if (value.is_border_radius_shorthand()) {
            auto const& shorthand = value.as_border_radius_shorthand();
            style.set_property(CSS::PropertyID::BorderTopLeftRadius, shorthand.top_left(), declaration);
            style.set_property(CSS::PropertyID::BorderTopRightRadius, shorthand.top_right(), declaration);
            style.set_property(CSS::PropertyID::BorderBottomRightRadius, shorthand.bottom_right(), declaration);
            style.set_property(CSS::PropertyID::BorderBottomLeftRadius, shorthand.bottom_left(), declaration);
            return;
        }

        style.set_property(CSS::PropertyID::BorderTopLeftRadius, value, declaration);
        style.set_property(CSS::PropertyID::BorderTopRightRadius, value, declaration);
        style.set_property(CSS::PropertyID::BorderBottomRightRadius, value, declaration);
        style.set_property(CSS::PropertyID::BorderBottomLeftRadius, value, declaration);
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
                style.set_property(PropertyID::BorderTopWidth, border.border_width(), declaration);
                style.set_property(PropertyID::BorderTopStyle, border.border_style(), declaration);
                style.set_property(PropertyID::BorderTopColor, border.border_color(), declaration);
            }
            if (contains(Edge::Right, edge)) {
                style.set_property(PropertyID::BorderRightWidth, border.border_width(), declaration);
                style.set_property(PropertyID::BorderRightStyle, border.border_style(), declaration);
                style.set_property(PropertyID::BorderRightColor, border.border_color(), declaration);
            }
            if (contains(Edge::Bottom, edge)) {
                style.set_property(PropertyID::BorderBottomWidth, border.border_width(), declaration);
                style.set_property(PropertyID::BorderBottomStyle, border.border_style(), declaration);
                style.set_property(PropertyID::BorderBottomColor, border.border_color(), declaration);
            }
            if (contains(Edge::Left, edge)) {
                style.set_property(PropertyID::BorderLeftWidth, border.border_width(), declaration);
                style.set_property(PropertyID::BorderLeftStyle, border.border_style(), declaration);
                style.set_property(PropertyID::BorderLeftColor, border.border_color(), declaration);
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

        style.set_property(CSS::PropertyID::BorderTopStyle, value, declaration);
        style.set_property(CSS::PropertyID::BorderRightStyle, value, declaration);
        style.set_property(CSS::PropertyID::BorderBottomStyle, value, declaration);
        style.set_property(CSS::PropertyID::BorderLeftStyle, value, declaration);
        return;
    }

    if (property_id == CSS::PropertyID::BorderWidth) {
        if (value.is_value_list()) {
            auto const& values_list = value.as_value_list();
            assign_edge_values(PropertyID::BorderTopWidth, PropertyID::BorderRightWidth, PropertyID::BorderBottomWidth, PropertyID::BorderLeftWidth, values_list.values());
            return;
        }

        style.set_property(CSS::PropertyID::BorderTopWidth, value, declaration);
        style.set_property(CSS::PropertyID::BorderRightWidth, value, declaration);
        style.set_property(CSS::PropertyID::BorderBottomWidth, value, declaration);
        style.set_property(CSS::PropertyID::BorderLeftWidth, value, declaration);
        return;
    }

    if (property_id == CSS::PropertyID::BorderColor) {
        if (value.is_value_list()) {
            auto const& values_list = value.as_value_list();
            assign_edge_values(PropertyID::BorderTopColor, PropertyID::BorderRightColor, PropertyID::BorderBottomColor, PropertyID::BorderLeftColor, values_list.values());
            return;
        }

        style.set_property(CSS::PropertyID::BorderTopColor, value, declaration);
        style.set_property(CSS::PropertyID::BorderRightColor, value, declaration);
        style.set_property(CSS::PropertyID::BorderBottomColor, value, declaration);
        style.set_property(CSS::PropertyID::BorderLeftColor, value, declaration);
        return;
    }

    if (property_id == CSS::PropertyID::Background) {
        if (value.is_background()) {
            auto const& background = value.as_background();
            set_property_expanding_shorthands(style, CSS::PropertyID::BackgroundColor, background.color(), document, declaration);
            set_property_expanding_shorthands(style, CSS::PropertyID::BackgroundImage, background.image(), document, declaration);
            set_property_expanding_shorthands(style, CSS::PropertyID::BackgroundPosition, background.position(), document, declaration);
            set_property_expanding_shorthands(style, CSS::PropertyID::BackgroundSize, background.size(), document, declaration);
            set_property_expanding_shorthands(style, CSS::PropertyID::BackgroundRepeat, background.repeat(), document, declaration);
            set_property_expanding_shorthands(style, CSS::PropertyID::BackgroundAttachment, background.attachment(), document, declaration);
            set_property_expanding_shorthands(style, CSS::PropertyID::BackgroundOrigin, background.origin(), document, declaration);
            set_property_expanding_shorthands(style, CSS::PropertyID::BackgroundClip, background.clip(), document, declaration);
            return;
        }

        set_property_expanding_shorthands(style, CSS::PropertyID::BackgroundColor, value, document, declaration);
        set_property_expanding_shorthands(style, CSS::PropertyID::BackgroundImage, value, document, declaration);
        set_property_expanding_shorthands(style, CSS::PropertyID::BackgroundPosition, value, document, declaration);
        set_property_expanding_shorthands(style, CSS::PropertyID::BackgroundSize, value, document, declaration);
        set_property_expanding_shorthands(style, CSS::PropertyID::BackgroundRepeat, value, document, declaration);
        set_property_expanding_shorthands(style, CSS::PropertyID::BackgroundAttachment, value, document, declaration);
        set_property_expanding_shorthands(style, CSS::PropertyID::BackgroundOrigin, value, document, declaration);
        set_property_expanding_shorthands(style, CSS::PropertyID::BackgroundClip, value, document, declaration);
        return;
    }

    if (property_id == CSS::PropertyID::BackgroundPosition) {
        if (value.is_position()) {
            auto const& position = value.as_position();
            style.set_property(CSS::PropertyID::BackgroundPositionX, position.edge_x(), declaration);
            style.set_property(CSS::PropertyID::BackgroundPositionY, position.edge_y(), declaration);
        } else if (value.is_value_list()) {
            // Expand background-position layer list into separate lists for x and y positions:
            auto const& values_list = value.as_value_list();
            StyleValueVector x_positions {};
            StyleValueVector y_positions {};
            x_positions.ensure_capacity(values_list.size());
            y_positions.ensure_capacity(values_list.size());
            for (auto& layer : values_list.values()) {
                if (layer->is_position()) {
                    auto const& position = layer->as_position();
                    x_positions.unchecked_append(position.edge_x());
                    y_positions.unchecked_append(position.edge_y());
                } else {
                    x_positions.unchecked_append(layer);
                    y_positions.unchecked_append(layer);
                }
            }
            style.set_property(CSS::PropertyID::BackgroundPositionX, StyleValueList::create(move(x_positions), values_list.separator()).release_value_but_fixme_should_propagate_errors(), declaration);
            style.set_property(CSS::PropertyID::BackgroundPositionY, StyleValueList::create(move(y_positions), values_list.separator()).release_value_but_fixme_should_propagate_errors(), declaration);
        } else {
            style.set_property(CSS::PropertyID::BackgroundPositionX, value, declaration);
            style.set_property(CSS::PropertyID::BackgroundPositionY, value, declaration);
        }

        return;
    }

    if (property_id == CSS::PropertyID::Inset) {
        if (value.is_value_list()) {
            auto const& values_list = value.as_value_list();
            assign_edge_values(PropertyID::Top, PropertyID::Right, PropertyID::Bottom, PropertyID::Left, values_list.values());
            return;
        }

        style.set_property(CSS::PropertyID::Top, value, declaration);
        style.set_property(CSS::PropertyID::Right, value, declaration);
        style.set_property(CSS::PropertyID::Bottom, value, declaration);
        style.set_property(CSS::PropertyID::Left, value, declaration);
        return;
    }

    if (property_id == CSS::PropertyID::Margin) {
        if (value.is_value_list()) {
            auto const& values_list = value.as_value_list();
            assign_edge_values(PropertyID::MarginTop, PropertyID::MarginRight, PropertyID::MarginBottom, PropertyID::MarginLeft, values_list.values());
            return;
        }

        style.set_property(CSS::PropertyID::MarginTop, value, declaration);
        style.set_property(CSS::PropertyID::MarginRight, value, declaration);
        style.set_property(CSS::PropertyID::MarginBottom, value, declaration);
        style.set_property(CSS::PropertyID::MarginLeft, value, declaration);
        return;
    }

    if (property_id == CSS::PropertyID::Padding) {
        if (value.is_value_list()) {
            auto const& values_list = value.as_value_list();
            assign_edge_values(PropertyID::PaddingTop, PropertyID::PaddingRight, PropertyID::PaddingBottom, PropertyID::PaddingLeft, values_list.values());
            return;
        }

        style.set_property(CSS::PropertyID::PaddingTop, value, declaration);
        style.set_property(CSS::PropertyID::PaddingRight, value, declaration);
        style.set_property(CSS::PropertyID::PaddingBottom, value, declaration);
        style.set_property(CSS::PropertyID::PaddingLeft, value, declaration);
        return;
    }

    if (property_id == CSS::PropertyID::ListStyle) {
        if (value.is_list_style()) {
            auto const& list_style = value.as_list_style();
            style.set_property(CSS::PropertyID::ListStylePosition, list_style.position(), declaration);
            style.set_property(CSS::PropertyID::ListStyleImage, list_style.image(), declaration);
            style.set_property(CSS::PropertyID::ListStyleType, list_style.style_type(), declaration);
            return;
        }

        style.set_property(CSS::PropertyID::ListStylePosition, value, declaration);
        style.set_property(CSS::PropertyID::ListStyleImage, value, declaration);
        style.set_property(CSS::PropertyID::ListStyleType, value, declaration);
        return;
    }

    if (property_id == CSS::PropertyID::Font) {
        if (value.is_font()) {
            auto const& font_shorthand = value.as_font();
            style.set_property(CSS::PropertyID::FontSize, font_shorthand.font_size(), declaration);
            style.set_property(CSS::PropertyID::FontFamily, font_shorthand.font_families(), declaration);
            style.set_property(CSS::PropertyID::FontStretch, font_shorthand.font_stretch(), declaration);
            style.set_property(CSS::PropertyID::FontStyle, font_shorthand.font_style(), declaration);
            style.set_property(CSS::PropertyID::FontWeight, font_shorthand.font_weight(), declaration);
            style.set_property(CSS::PropertyID::LineHeight, font_shorthand.line_height(), declaration);
            // FIXME: Implement font-variant
            return;
        }

        style.set_property(CSS::PropertyID::FontStretch, value, declaration);
        style.set_property(CSS::PropertyID::FontSize, value, declaration);
        style.set_property(CSS::PropertyID::FontFamily, value, declaration);
        style.set_property(CSS::PropertyID::FontStyle, value, declaration);
        style.set_property(CSS::PropertyID::FontWeight, value, declaration);
        style.set_property(CSS::PropertyID::LineHeight, value, declaration);
        // FIXME: Implement font-variant
        return;
    }

    if (property_id == CSS::PropertyID::Flex) {
        if (value.is_flex()) {
            auto const& flex = value.as_flex();
            style.set_property(CSS::PropertyID::FlexGrow, flex.grow(), declaration);
            style.set_property(CSS::PropertyID::FlexShrink, flex.shrink(), declaration);
            style.set_property(CSS::PropertyID::FlexBasis, flex.basis(), declaration);
            return;
        }

        style.set_property(CSS::PropertyID::FlexGrow, value, declaration);
        style.set_property(CSS::PropertyID::FlexShrink, value, declaration);
        style.set_property(CSS::PropertyID::FlexBasis, value, declaration);
        return;
    }

    if (property_id == CSS::PropertyID::FlexFlow) {
        if (value.is_flex_flow()) {
            auto const& flex_flow = value.as_flex_flow();
            style.set_property(CSS::PropertyID::FlexDirection, flex_flow.flex_direction(), declaration);
            style.set_property(CSS::PropertyID::FlexWrap, flex_flow.flex_wrap(), declaration);
            return;
        }

        style.set_property(CSS::PropertyID::FlexDirection, value, declaration);
        style.set_property(CSS::PropertyID::FlexWrap, value, declaration);
        return;
    }

    if (property_id == CSS::PropertyID::GridArea) {
        if (value.is_grid_area_shorthand()) {
            auto const& shorthand = value.as_grid_area_shorthand();
            style.set_property(CSS::PropertyID::GridRowStart, shorthand.row_start(), declaration);
            style.set_property(CSS::PropertyID::GridColumnStart, shorthand.column_start(), declaration);
            style.set_property(CSS::PropertyID::GridRowEnd, shorthand.row_end(), declaration);
            style.set_property(CSS::PropertyID::GridColumnEnd, shorthand.column_end(), declaration);
            return;
        }
        style.set_property(CSS::PropertyID::GridRowStart, value, declaration);
        style.set_property(CSS::PropertyID::GridColumnStart, value, declaration);
        style.set_property(CSS::PropertyID::GridRowEnd, value, declaration);
        style.set_property(CSS::PropertyID::GridColumnEnd, value, declaration);
        return;
    }

    if (property_id == CSS::PropertyID::GridColumn) {
        if (value.is_grid_track_placement_shorthand()) {
            auto const& shorthand = value.as_grid_track_placement_shorthand();
            style.set_property(CSS::PropertyID::GridColumnStart, shorthand.start(), declaration);
            style.set_property(CSS::PropertyID::GridColumnEnd, shorthand.end(), declaration);
            return;
        }

        style.set_property(CSS::PropertyID::GridColumnStart, value, declaration);
        style.set_property(CSS::PropertyID::GridColumnEnd, value, declaration);
        return;
    }

    if (property_id == CSS::PropertyID::GridRow) {
        if (value.is_grid_track_placement_shorthand()) {
            auto const& shorthand = value.as_grid_track_placement_shorthand();
            style.set_property(CSS::PropertyID::GridRowStart, shorthand.start(), declaration);
            style.set_property(CSS::PropertyID::GridRowEnd, shorthand.end(), declaration);
            return;
        }

        style.set_property(CSS::PropertyID::GridRowStart, value, declaration);
        style.set_property(CSS::PropertyID::GridRowEnd, value, declaration);
        return;
    }

    if (property_id == CSS::PropertyID::GridTemplate || property_id == CSS::PropertyID::Grid) {
        if (value.is_grid_track_size_list_shorthand()) {
            auto const& shorthand = value.as_grid_track_size_list_shorthand();
            style.set_property(CSS::PropertyID::GridTemplateAreas, shorthand.areas(), declaration);
            style.set_property(CSS::PropertyID::GridTemplateRows, shorthand.rows(), declaration);
            style.set_property(CSS::PropertyID::GridTemplateColumns, shorthand.columns(), declaration);
            return;
        }
        style.set_property(CSS::PropertyID::GridTemplateAreas, value, declaration);
        style.set_property(CSS::PropertyID::GridTemplateRows, value, declaration);
        style.set_property(CSS::PropertyID::GridTemplateColumns, value, declaration);
        return;
    }

    if (property_id == CSS::PropertyID::Gap || property_id == CSS::PropertyID::GridGap) {
        if (value.is_value_list()) {
            auto const& values_list = value.as_value_list();
            style.set_property(CSS::PropertyID::RowGap, values_list.values()[0], declaration);
            style.set_property(CSS::PropertyID::ColumnGap, values_list.values()[1], declaration);
            return;
        }
        style.set_property(CSS::PropertyID::RowGap, value, declaration);
        style.set_property(CSS::PropertyID::ColumnGap, value, declaration);
        return;
    }

    if (property_id == CSS::PropertyID::RowGap || property_id == CSS::PropertyID::GridRowGap) {
        style.set_property(CSS::PropertyID::RowGap, value, declaration);
        return;
    }

    if (property_id == CSS::PropertyID::ColumnGap || property_id == CSS::PropertyID::GridColumnGap) {
        style.set_property(CSS::PropertyID::ColumnGap, value, declaration);
        return;
    }

    if (property_id == CSS::PropertyID::MaxInlineSize || property_id == CSS::PropertyID::MinInlineSize) {
        // FIXME: Use writing-mode to determine if we should set width or height.
        bool is_horizontal = true;

        if (is_horizontal) {
            if (property_id == CSS::PropertyID::MaxInlineSize) {
                style.set_property(CSS::PropertyID::MaxWidth, value, declaration);
            } else {
                style.set_property(CSS::PropertyID::MinWidth, value, declaration);
            }
        } else {
            if (property_id == CSS::PropertyID::MaxInlineSize) {
                style.set_property(CSS::PropertyID::MaxHeight, value, declaration);
            } else {
                style.set_property(CSS::PropertyID::MinHeight, value, declaration);
            }
        }
        return;
    }

    style.set_property(property_id, value, declaration);
}

static RefPtr<StyleValue const> get_custom_property(DOM::Element const& element, Optional<CSS::Selector::PseudoElement> pseudo_element, FlyString const& custom_property_name)
{
    if (pseudo_element.has_value()) {
        if (auto it = element.custom_properties(pseudo_element).find(custom_property_name.to_string().to_deprecated_string()); it != element.custom_properties(pseudo_element).end())
            return it->value.value;
    }

    for (auto const* current_element = &element; current_element; current_element = current_element->parent_element()) {
        if (auto it = current_element->custom_properties({}).find(custom_property_name.to_string().to_deprecated_string()); it != current_element->custom_properties({}).end())
            return it->value.value;
    }
    return nullptr;
}

bool StyleComputer::expand_variables(DOM::Element& element, Optional<CSS::Selector::PseudoElement> pseudo_element, StringView property_name, HashMap<FlyString, NonnullRefPtr<PropertyDependencyNode>>& dependencies, Parser::TokenStream<Parser::ComponentValue>& source, Vector<Parser::ComponentValue>& dest) const
{
    // Arbitrary large value chosen to avoid the billion-laughs attack.
    // https://www.w3.org/TR/css-variables-1/#long-variables
    size_t const MAX_VALUE_COUNT = 16384;
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
        if (value.is_block()) {
            auto const& source_block = value.block();
            Vector<Parser::ComponentValue> block_values;
            Parser::TokenStream source_block_contents { source_block.values() };
            if (!expand_variables(element, pseudo_element, property_name, dependencies, source_block_contents, block_values))
                return false;
            NonnullRefPtr<Parser::Block> block = Parser::Block::create(source_block.token(), move(block_values));
            dest.empend(block);
            continue;
        }
        if (!value.is_function()) {
            dest.empend(value);
            continue;
        }
        if (!value.function().name().equals_ignoring_ascii_case("var"sv)) {
            auto const& source_function = value.function();
            Vector<Parser::ComponentValue> function_values;
            Parser::TokenStream source_function_contents { source_function.values() };
            if (!expand_variables(element, pseudo_element, property_name, dependencies, source_function_contents, function_values))
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

        if (auto custom_property_value = get_custom_property(element, pseudo_element, FlyString::from_utf8(custom_property_name).release_value_but_fixme_should_propagate_errors())) {
            VERIFY(custom_property_value->is_unresolved());
            Parser::TokenStream custom_property_tokens { custom_property_value->as_unresolved().values() };
            if (!expand_variables(element, pseudo_element, custom_property_name, dependencies, custom_property_tokens, dest))
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
            if (!expand_variables(element, pseudo_element, property_name, dependencies, var_contents, dest))
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

            // FIXME: Handle all math functions.
            if (value.function().name().equals_ignoring_ascii_case("calc"sv)) {
                auto const& calc_function = value.function();
                if (auto calc_value = Parser::Parser::parse_calculated_value({}, Parser::ParsingContext { document() }, calc_function.values()).release_value_but_fixme_should_propagate_errors()) {
                    if (calc_value->resolves_to_number()) {
                        auto resolved_value = calc_value->resolve_number();
                        dest.empend(Parser::Token::create_number(resolved_value.value()));
                        continue;
                    } else if (calc_value->resolves_to_percentage()) {
                        auto resolved_value = calc_value->resolve_percentage();
                        dest.empend(Parser::Token::create_percentage(resolved_value.value().value()));
                        continue;
                    } else {
                        dbgln_if(LIBWEB_CSS_DEBUG, "FIXME: Unimplemented calc() expansion: {}", calc_value->to_string());
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

RefPtr<StyleValue> StyleComputer::resolve_unresolved_style_value(DOM::Element& element, Optional<CSS::Selector::PseudoElement> pseudo_element, PropertyID property_id, UnresolvedStyleValue const& unresolved) const
{
    // Unresolved always contains a var() or attr(), unless it is a custom property's value, in which case we shouldn't be trying
    // to produce a different StyleValue from it.
    VERIFY(unresolved.contains_var_or_attr());

    Parser::TokenStream unresolved_values_without_variables_expanded { unresolved.values() };
    Vector<Parser::ComponentValue> values_with_variables_expanded;

    HashMap<FlyString, NonnullRefPtr<PropertyDependencyNode>> dependencies;
    if (!expand_variables(element, pseudo_element, string_from_property_id(property_id), dependencies, unresolved_values_without_variables_expanded, values_with_variables_expanded))
        return {};

    Parser::TokenStream unresolved_values_with_variables_expanded { values_with_variables_expanded };
    Vector<Parser::ComponentValue> expanded_values;
    if (!expand_unresolved_values(element, string_from_property_id(property_id), unresolved_values_with_variables_expanded, expanded_values))
        return {};

    if (auto parsed_value = Parser::Parser::parse_css_value({}, Parser::ParsingContext { document() }, property_id, expanded_values).release_value_but_fixme_should_propagate_errors())
        return parsed_value.release_nonnull();

    return {};
}

void StyleComputer::cascade_declarations(StyleProperties& style, DOM::Element& element, Optional<CSS::Selector::PseudoElement> pseudo_element, Vector<MatchingRule> const& matching_rules, CascadeOrigin cascade_origin, Important important) const
{
    for (auto const& match : matching_rules) {
        for (auto const& property : verify_cast<PropertyOwningCSSStyleDeclaration>(match.rule->declaration()).properties()) {
            if (important != property.important)
                continue;
            auto property_value = property.value;
            if (property.value->is_unresolved()) {
                if (auto resolved = resolve_unresolved_style_value(element, pseudo_element, property.property_id, property.value->as_unresolved()))
                    property_value = resolved.release_nonnull();
            }
            if (!property_value->is_unresolved())
                set_property_expanding_shorthands(style, property.property_id, property_value, m_document, &match.rule->declaration());
        }
    }

    if (cascade_origin == CascadeOrigin::Author && !pseudo_element.has_value()) {
        if (auto const* inline_style = verify_cast<ElementInlineCSSStyleDeclaration>(element.inline_style())) {
            for (auto const& property : inline_style->properties()) {
                if (important != property.important)
                    continue;
                auto property_value = property.value;
                if (property.value->is_unresolved()) {
                    if (auto resolved = resolve_unresolved_style_value(element, pseudo_element, property.property_id, property.value->as_unresolved()))
                        property_value = resolved.release_nonnull();
                }
                if (!property_value->is_unresolved())
                    set_property_expanding_shorthands(style, property.property_id, property_value, m_document, inline_style);
            }
        }
    }
}

static ErrorOr<void> cascade_custom_properties(DOM::Element& element, Optional<CSS::Selector::PseudoElement> pseudo_element, Vector<MatchingRule> const& matching_rules)
{
    size_t needed_capacity = 0;
    for (auto const& matching_rule : matching_rules)
        needed_capacity += verify_cast<PropertyOwningCSSStyleDeclaration>(matching_rule.rule->declaration()).custom_properties().size();

    if (!pseudo_element.has_value()) {
        if (auto const* inline_style = verify_cast<PropertyOwningCSSStyleDeclaration>(element.inline_style()))
            needed_capacity += inline_style->custom_properties().size();
    }

    HashMap<DeprecatedFlyString, StyleProperty> custom_properties;
    TRY(custom_properties.try_ensure_capacity(needed_capacity));

    for (auto const& matching_rule : matching_rules) {
        for (auto const& it : verify_cast<PropertyOwningCSSStyleDeclaration>(matching_rule.rule->declaration()).custom_properties())
            custom_properties.set(it.key, it.value);
    }

    if (!pseudo_element.has_value()) {
        if (auto const* inline_style = verify_cast<PropertyOwningCSSStyleDeclaration>(element.inline_style())) {
            for (auto const& it : inline_style->custom_properties())
                custom_properties.set(it.key, it.value);
        }
    }

    element.set_custom_properties(pseudo_element, move(custom_properties));

    return {};
}

StyleComputer::AnimationStepTransition StyleComputer::Animation::step(CSS::Time const& time_step)
{
    auto delay_ms = remaining_delay.to_milliseconds();
    auto time_step_ms = time_step.to_milliseconds();

    if (delay_ms > time_step_ms) {
        remaining_delay = CSS::Time { static_cast<float>(delay_ms - time_step_ms), CSS::Time::Type::Ms };
        return AnimationStepTransition::NoTransition;
    }

    remaining_delay = CSS::Time { 0, CSS::Time::Type::Ms };
    time_step_ms -= delay_ms;

    // "auto": For time-driven animations, equivalent to 0s.
    // https://www.w3.org/TR/2023/WD-css-animations-2-20230602/#valdef-animation-duration-auto
    auto used_duration = duration.value_or(CSS::Time { 0, CSS::Time::Type::S });

    auto added_progress = time_step_ms / used_duration.to_milliseconds();
    auto new_progress = progress.as_fraction() + added_progress;
    auto changed_iteration = false;
    if (new_progress >= 1) {
        if (iteration_count.has_value()) {
            if (iteration_count.value() <= 1) {
                progress = CSS::Percentage(100);
                return AnimationStepTransition::ActiveToAfter;
            }
            --iteration_count.value();
            changed_iteration = true;
        }
        ++current_iteration;
        new_progress = 0;
    }
    progress = CSS::Percentage(new_progress * 100);

    if (changed_iteration)
        return AnimationStepTransition::ActiveToActiveChangingTheIteration;

    return AnimationStepTransition::AfterToActive;
}

static ErrorOr<NonnullRefPtr<StyleValue>> interpolate_property(StyleValue const& from, StyleValue const& to, float delta)
{
    if (from.type() != to.type()) {
        if (delta > 0.999f)
            return to;
        return from;
    }

    auto interpolate_raw = [delta = static_cast<double>(delta)](auto from, auto to) {
        return static_cast<RemoveCVReference<decltype(from)>>(static_cast<double>(from) + static_cast<double>(to - from) * delta);
    };

    switch (from.type()) {
    case StyleValue::Type::Angle:
        return AngleStyleValue::create(Angle::make_degrees(interpolate_raw(from.as_angle().angle().to_degrees(), to.as_angle().angle().to_degrees())));
    case StyleValue::Type::Color: {
        auto from_color = from.as_color().color();
        auto to_color = to.as_color().color();
        auto from_hsv = from_color.to_hsv();
        auto to_hsv = to_color.to_hsv();

        auto color = Color::from_hsv(
            interpolate_raw(from_hsv.hue, to_hsv.hue),
            interpolate_raw(from_hsv.saturation, to_hsv.saturation),
            interpolate_raw(from_hsv.value, to_hsv.value));
        color.set_alpha(interpolate_raw(from_color.alpha(), to_color.alpha()));

        return ColorStyleValue::create(color);
    }
    case StyleValue::Type::Integer:
        return IntegerStyleValue::create(interpolate_raw(from.as_integer().integer(), to.as_integer().integer()));
    case StyleValue::Type::Length: {
        auto& from_length = from.as_length().length();
        auto& to_length = to.as_length().length();
        return LengthStyleValue::create(Length(interpolate_raw(from_length.raw_value(), to_length.raw_value()), from_length.type()));
    }
    case StyleValue::Type::Number:
        return NumberStyleValue::create(interpolate_raw(from.as_number().number(), to.as_number().number()));
    case StyleValue::Type::Percentage:
        return PercentageStyleValue::create(Percentage(interpolate_raw(from.as_percentage().percentage().value(), to.as_percentage().percentage().value())));
    case StyleValue::Type::Position: {
        auto& from_position = from.as_position();
        auto& to_position = to.as_position();
        return PositionStyleValue::create(
            TRY(interpolate_property(from_position.edge_x(), to_position.edge_x(), delta)),
            TRY(interpolate_property(from_position.edge_y(), to_position.edge_y(), delta)));
    }
    case StyleValue::Type::Rect: {
        auto from_rect = from.as_rect().rect();
        auto to_rect = to.as_rect().rect();
        return RectStyleValue::create({
            Length(interpolate_raw(from_rect.top_edge.raw_value(), to_rect.top_edge.raw_value()), from_rect.top_edge.type()),
            Length(interpolate_raw(from_rect.right_edge.raw_value(), to_rect.right_edge.raw_value()), from_rect.right_edge.type()),
            Length(interpolate_raw(from_rect.bottom_edge.raw_value(), to_rect.bottom_edge.raw_value()), from_rect.bottom_edge.type()),
            Length(interpolate_raw(from_rect.left_edge.raw_value(), to_rect.left_edge.raw_value()), from_rect.left_edge.type()),
        });
    }
    case StyleValue::Type::Transformation: {
        auto& from_transform = from.as_transformation();
        auto& to_transform = to.as_transformation();
        if (from_transform.transform_function() != to_transform.transform_function())
            return from;

        auto from_input_values = from_transform.values();
        auto to_input_values = to_transform.values();
        if (from_input_values.size() != to_input_values.size())
            return from;

        StyleValueVector interpolated_values;
        interpolated_values.ensure_capacity(from_input_values.size());
        for (size_t i = 0; i < from_input_values.size(); ++i)
            interpolated_values.append(TRY(interpolate_property(*from_input_values[i], *to_input_values[i], delta)));

        return TransformationStyleValue::create(from_transform.transform_function(), move(interpolated_values));
    }
    case StyleValue::Type::ValueList: {
        auto& from_list = from.as_value_list();
        auto& to_list = to.as_value_list();
        if (from_list.size() != to_list.size())
            return from;

        StyleValueVector interpolated_values;
        interpolated_values.ensure_capacity(from_list.size());
        for (size_t i = 0; i < from_list.size(); ++i)
            interpolated_values.append(TRY(interpolate_property(from_list.values()[i], to_list.values()[i], delta)));

        return StyleValueList::create(move(interpolated_values), from_list.separator());
    }
    default:
        return from;
    }
}

bool StyleComputer::Animation::is_animating_backwards() const
{
    return (direction == CSS::AnimationDirection::AlternateReverse && current_iteration % 2 == 1)
        || (direction == CSS::AnimationDirection::Alternate && current_iteration % 2 == 0)
        || direction == CSS::AnimationDirection::Reverse;
}

ErrorOr<void> StyleComputer::Animation::collect_into(StyleProperties& style_properties, RuleCache const& rule_cache) const
{
    if (remaining_delay.to_milliseconds() != 0) {
        // If the fill mode is backwards or both, we'll pretend that the animation is started, but stuck at progress 0
        if (fill_mode != CSS::AnimationFillMode::Backwards && fill_mode != CSS::AnimationFillMode::Both)
            return {};
    }

    auto matching_keyframes = rule_cache.rules_by_animation_keyframes.get(name);
    if (!matching_keyframes.has_value())
        return {};

    auto& keyframes = matching_keyframes.value()->keyframes_by_key;

    auto output_progress = compute_output_progress(progress.as_fraction()) * 100.f;
    auto is_backwards = is_animating_backwards();

    auto key = static_cast<u64>(output_progress * AnimationKeyFrameKeyScaleFactor);
    auto matching_keyframe_it = is_backwards ? keyframes.find_smallest_not_below_iterator(key) : keyframes.find_largest_not_above_iterator(key);
    if (matching_keyframe_it.is_end()) {
        if constexpr (LIBWEB_CSS_ANIMATION_DEBUG) {
            dbgln("    Did not find any start keyframe for the current state ({}) :(", key);
            dbgln("    (have {} keyframes)", keyframes.size());
            for (auto it = keyframes.begin(); it != keyframes.end(); ++it)
                dbgln("        - {}", it.key());
        }
        return {};
    }

    auto keyframe_start = matching_keyframe_it.key();
    auto keyframe_values = *matching_keyframe_it;

    auto initial_keyframe_it = matching_keyframe_it;
    auto keyframe_end_it = is_backwards ? --matching_keyframe_it : ++matching_keyframe_it;
    if (keyframe_end_it.is_end())
        keyframe_end_it = initial_keyframe_it;

    auto keyframe_end = keyframe_end_it.key();
    auto keyframe_end_values = *keyframe_end_it;

    auto progress_in_keyframe = [&] {
        if (keyframe_start == keyframe_end)
            return is_backwards ? 1.f : 0.f;

        return is_backwards
            ? static_cast<float>(keyframe_start - key) / static_cast<float>(keyframe_start - keyframe_end)
            : static_cast<float>(key - keyframe_start) / static_cast<float>(keyframe_end - keyframe_start);
    }();

    auto valid_properties = 0;
    for (auto const& property : keyframe_values.resolved_properties) {
        if (property.has<Empty>())
            continue;
        valid_properties++;
    }

    dbgln_if(LIBWEB_CSS_ANIMATION_DEBUG, "Animation {} contains {} properties to interpolate, progress = {}%", name, valid_properties, progress_in_keyframe * 100);

    if (fill_mode == CSS::AnimationFillMode::Forwards || fill_mode == CSS::AnimationFillMode::Both) {
        if (!active_state_if_fill_forward)
            active_state_if_fill_forward = make<AnimationStateSnapshot>();
    }

    UnderlyingType<PropertyID> property_id_value = 0;
    for (auto const& property : keyframe_values.resolved_properties) {
        auto property_id = static_cast<PropertyID>(property_id_value++);
        if (property.has<Empty>())
            continue;

        auto resolve_property = [&](auto& property) {
            return property.visit(
                [](Empty) -> RefPtr<StyleValue const> { VERIFY_NOT_REACHED(); },
                [&](AnimationKeyFrameSet::ResolvedKeyFrame::UseInitial) {
                    if (auto value = initial_state.state[to_underlying(property_id)])
                        return value;

                    auto value = style_properties.maybe_null_property(property_id);
                    initial_state.state[to_underlying(property_id)] = value;
                    return value;
                },
                [&](RefPtr<StyleValue const> value) { return value; });
        };

        auto resolved_start_property = resolve_property(property);

        auto const& end_property = keyframe_end_values.resolved_properties[to_underlying(property_id)];
        if (end_property.has<Empty>()) {
            if (resolved_start_property) {
                style_properties.set_property(property_id, resolved_start_property.release_nonnull());
                dbgln_if(LIBWEB_CSS_ANIMATION_DEBUG, "No end property for property {}, using {}", string_from_property_id(property_id), resolved_start_property->to_string());
            }
            continue;
        }

        auto resolved_end_property = resolve_property(end_property);

        if (!resolved_start_property || !resolved_end_property)
            continue;

        auto start = resolved_start_property.release_nonnull();
        auto end = resolved_end_property.release_nonnull();

        auto next_value = TRY(interpolate_property(*start, *end, progress_in_keyframe));
        dbgln_if(LIBWEB_CSS_ANIMATION_DEBUG, "Interpolated value for property {} at {}: {} -> {} = {}", string_from_property_id(property_id), progress_in_keyframe, start->to_string(), end->to_string(), next_value->to_string());
        style_properties.set_property(property_id, next_value);
        if (active_state_if_fill_forward)
            active_state_if_fill_forward->state[to_underlying(property_id)] = next_value;
    }

    return {};
}

bool StyleComputer::Animation::is_done() const
{
    return progress.as_fraction() >= 0.9999 && iteration_count.has_value() && iteration_count.value() == 0;
}

float StyleComputer::Animation::compute_output_progress(float input_progress) const
{
    auto output_progress = input_progress;
    switch (direction) {
    case AnimationDirection::Alternate:
        if (current_iteration % 2 == 0)
            output_progress = 1.0f - output_progress;
        break;
    case AnimationDirection::AlternateReverse:
        if (current_iteration % 2 == 1)
            output_progress = 1.0f - output_progress;
        break;
    case AnimationDirection::Normal:
        break;
    case AnimationDirection::Reverse:
        output_progress = 1.0f - output_progress;
        break;
    }

    // FIXME: This should also be a function of the animation-timing-function, if not during the delay.
    return output_progress;
}

void StyleComputer::ensure_animation_timer() const
{
    constexpr static auto timer_delay_ms = 1000 / 60;
    if (!m_animation_driver_timer) {
        m_animation_driver_timer = Platform::Timer::create_repeating(timer_delay_ms, [this] {
            // If we run out of animations, stop the timer - it'll turn back on the next time we have an active animation.
            if (m_active_animations.is_empty()) {
                m_animation_driver_timer->stop();
                return;
            }

            HashTable<AnimationKey> animations_to_remove;
            HashTable<DOM::Element*> owning_elements_to_invalidate;

            for (auto& it : m_active_animations) {
                if (!it.value->owning_element) {
                    // The element disappeared since we last ran, just discard the animation.
                    animations_to_remove.set(it.key);
                    continue;
                }

                auto transition = it.value->step(CSS::Time { timer_delay_ms, CSS::Time::Type::Ms });
                owning_elements_to_invalidate.set(it.value->owning_element);

                switch (transition) {
                case AnimationStepTransition::NoTransition:
                    break;
                case AnimationStepTransition::IdleOrBeforeToActive:
                    // FIXME: Dispatch `animationstart`.
                    break;
                case AnimationStepTransition::IdleOrBeforeToAfter:
                    // FIXME: Dispatch `animationstart` then `animationend`.
                    m_finished_animations.set(it.key, move(it.value->active_state_if_fill_forward));
                    break;
                case AnimationStepTransition::ActiveToBefore:
                    // FIXME: Dispatch `animationend`.
                    m_finished_animations.set(it.key, move(it.value->active_state_if_fill_forward));
                    break;
                case AnimationStepTransition::ActiveToActiveChangingTheIteration:
                    // FIXME: Dispatch `animationiteration`.
                    break;
                case AnimationStepTransition::ActiveToAfter:
                    // FIXME: Dispatch `animationend`.
                    m_finished_animations.set(it.key, move(it.value->active_state_if_fill_forward));
                    break;
                case AnimationStepTransition::AfterToActive:
                    // FIXME: Dispatch `animationstart`.
                    break;
                case AnimationStepTransition::AfterToBefore:
                    // FIXME: Dispatch `animationstart` then `animationend`.
                    m_finished_animations.set(it.key, move(it.value->active_state_if_fill_forward));
                    break;
                case AnimationStepTransition::Cancelled:
                    // FIXME: Dispatch `animationcancel`.
                    m_finished_animations.set(it.key, nullptr);
                    break;
                }
                if (it.value->is_done())
                    animations_to_remove.set(it.key);
            }

            for (auto key : animations_to_remove)
                m_active_animations.remove(key);

            for (auto* element : owning_elements_to_invalidate)
                element->set_needs_style_update(true);
        });
    }

    m_animation_driver_timer->start();
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
    TRY(cascade_custom_properties(element, pseudo_element, matching_rule_set.author_rules));

    // Then we apply the declarations from the matched rules in cascade order:

    // Normal user agent declarations
    cascade_declarations(style, element, pseudo_element, matching_rule_set.user_agent_rules, CascadeOrigin::UserAgent, Important::No);

    // FIXME: Normal user declarations

    // Author presentational hints (NOTE: The spec doesn't say exactly how to prioritize these.)
    if (!pseudo_element.has_value()) {
        element.apply_presentational_hints(style);

        // SVG presentation attributes are parsed as CSS values, so we need to handle potential custom properties here.
        if (element.is_svg_element()) {
            // FIXME: This is not very efficient, we should only resolve the custom properties that are actually used.
            for (auto i = to_underlying(CSS::first_property_id); i <= to_underlying(CSS::last_property_id); ++i) {
                auto property_id = (CSS::PropertyID)i;
                auto& property = style.m_property_values[i];
                if (property.has_value() && property->style->is_unresolved()) {
                    if (auto resolved = resolve_unresolved_style_value(element, pseudo_element, property_id, property->style->as_unresolved()))
                        property->style = resolved.release_nonnull();
                }
            }
        }
    }

    // Normal author declarations
    cascade_declarations(style, element, pseudo_element, matching_rule_set.author_rules, CascadeOrigin::Author, Important::No);

    // Animation declarations [css-animations-2]
    if (auto animation_name = style.maybe_null_property(PropertyID::AnimationName)) {
        if (auto source_declaration = style.property_source_declaration(PropertyID::AnimationName)) {
            AnimationKey animation_key {
                .source_declaration = source_declaration,
                .element = &element,
            };

            if (auto finished_state = m_finished_animations.get(animation_key); finished_state.has_value()) {
                // We've already finished going through this animation, so drop it from the active animations.
                m_active_animations.remove(animation_key);
                // If the animation's fill mode was set to forwards/both, we need to collect and use the final frame's styles.
                if (*finished_state) {
                    auto& state = (*finished_state)->state;
                    for (size_t property_id_value = 0; property_id_value < state.size(); ++property_id_value) {
                        if (auto& property_value = state[property_id_value])
                            style.set_property(static_cast<PropertyID>(property_id_value), *property_value);
                    }
                }
            } else if (auto name = TRY(animation_name->to_string()); !name.is_empty()) {
                auto active_animation = m_active_animations.get(animation_key);
                if (!active_animation.has_value()) {
                    // New animation!
                    Optional<CSS::Time> duration;
                    if (auto duration_value = style.maybe_null_property(PropertyID::AnimationDuration); duration_value) {
                        if (duration_value->is_time()) {
                            duration = duration_value->as_time().time();
                        } else if (duration_value->is_identifier() && duration_value->as_identifier().id() == ValueID::Auto) {
                            // We use empty optional to represent "auto".
                            duration = {};
                        }
                    }

                    CSS::Time delay { 0, CSS::Time::Type::S };
                    if (auto delay_value = style.maybe_null_property(PropertyID::AnimationDelay); delay_value && delay_value->is_time())
                        delay = delay_value->as_time().time();

                    Optional<size_t> iteration_count = 1;
                    if (auto iteration_count_value = style.maybe_null_property(PropertyID::AnimationIterationCount); iteration_count_value) {
                        if (iteration_count_value->is_identifier() && iteration_count_value->to_identifier() == ValueID::Infinite)
                            iteration_count = {};
                        else if (iteration_count_value->is_number())
                            iteration_count = static_cast<size_t>(iteration_count_value->as_number().number());
                    }

                    CSS::AnimationFillMode fill_mode { CSS::AnimationFillMode::None };
                    if (auto fill_mode_property = style.maybe_null_property(PropertyID::AnimationFillMode); fill_mode_property && fill_mode_property->is_identifier()) {
                        if (auto fill_mode_value = value_id_to_animation_fill_mode(fill_mode_property->to_identifier()); fill_mode_value.has_value())
                            fill_mode = *fill_mode_value;
                    }

                    CSS::AnimationDirection direction { CSS::AnimationDirection::Normal };
                    if (auto direction_property = style.maybe_null_property(PropertyID::AnimationDirection); direction_property && direction_property->is_identifier()) {
                        if (auto direction_value = value_id_to_animation_direction(direction_property->to_identifier()); direction_value.has_value())
                            direction = *direction_value;
                    }

                    auto animation = make<Animation>(Animation {
                        .name = move(name),
                        .duration = duration,
                        .delay = delay,
                        .iteration_count = iteration_count,
                        .direction = direction,
                        .fill_mode = fill_mode,
                        .owning_element = TRY(element.try_make_weak_ptr<DOM::Element>()),
                        .progress = CSS::Percentage(0),
                        .remaining_delay = delay,
                    });
                    active_animation = animation;
                    m_active_animations.set(animation_key, move(animation));
                }

                TRY((*active_animation)->collect_into(style, rule_cache_for_cascade_origin(CascadeOrigin::Author)));
            } else {
                m_active_animations.remove(animation_key);
            }
        }

        if (!m_active_animations.is_empty())
            ensure_animation_timer();
    }

    // Important author declarations
    cascade_declarations(style, element, pseudo_element, matching_rule_set.author_rules, CascadeOrigin::Author, Important::Yes);

    // FIXME: Important user declarations

    // Important user agent declarations
    cascade_declarations(style, element, pseudo_element, matching_rule_set.user_agent_rules, CascadeOrigin::UserAgent, Important::Yes);

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
        return property_initial_value(initial_value_context_realm, property_id).release_value_but_fixme_should_propagate_errors();
    return parent_element->computed_css_values()->property(property_id);
};

void StyleComputer::compute_defaulted_property_value(StyleProperties& style, DOM::Element const* element, CSS::PropertyID property_id, Optional<CSS::Selector::PseudoElement> pseudo_element) const
{
    // FIXME: If we don't know the correct initial value for a property, we fall back to InitialStyleValue.

    auto& value_slot = style.m_property_values[to_underlying(property_id)];
    if (!value_slot.has_value()) {
        if (is_inherited_property(property_id))
            style.m_property_values[to_underlying(property_id)] = { { get_inherit_value(document().realm(), property_id, element, pseudo_element), nullptr } };
        else
            style.m_property_values[to_underlying(property_id)] = { { property_initial_value(document().realm(), property_id).release_value_but_fixme_should_propagate_errors(), nullptr } };
        return;
    }

    if (value_slot->style->is_initial()) {
        value_slot->style = property_initial_value(document().realm(), property_id).release_value_but_fixme_should_propagate_errors();
        return;
    }

    if (value_slot->style->is_inherit()) {
        value_slot->style = get_inherit_value(document().realm(), property_id, element, pseudo_element);
        return;
    }

    // https://www.w3.org/TR/css-cascade-4/#inherit-initial
    // If the cascaded value of a property is the unset keyword,
    if (value_slot->style->is_unset()) {
        if (is_inherited_property(property_id)) {
            // then if it is an inherited property, this is treated as inherit,
            value_slot->style = get_inherit_value(document().realm(), property_id, element, pseudo_element);
        } else {
            // and if it is not, this is treated as initial.
            value_slot->style = property_initial_value(document().realm(), property_id).release_value_but_fixme_should_propagate_errors();
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

    // https://www.w3.org/TR/css-color-4/#resolving-other-colors
    // In the color property, the used value of currentcolor is the inherited value.
    auto color = style.property(CSS::PropertyID::Color);
    if (color->to_identifier() == CSS::ValueID::Currentcolor) {
        color = get_inherit_value(document().realm(), CSS::PropertyID::Color, element, pseudo_element);
        style.set_property(CSS::PropertyID::Color, color);
    }
}

Length::FontMetrics StyleComputer::calculate_root_element_font_metrics(StyleProperties const& style) const
{
    auto root_value = style.property(CSS::PropertyID::FontSize);

    auto font_pixel_metrics = style.computed_font().pixel_metrics();
    Length::FontMetrics font_metrics { m_default_font_metrics.font_size, font_pixel_metrics, font_pixel_metrics.line_spacing() };
    font_metrics.font_size = root_value->as_length().length().to_px(viewport_rect(), font_metrics, font_metrics);
    font_metrics.line_height = style.line_height(viewport_rect(), font_metrics, font_metrics);

    return font_metrics;
}

RefPtr<Gfx::Font const> StyleComputer::find_matching_font_weight_ascending(Vector<MatchingFontCandidate> const& candidates, int target_weight, float font_size_in_pt, bool inclusive)
{
    using Fn = AK::Function<bool(MatchingFontCandidate const&)>;
    auto pred = inclusive ? Fn([&](auto const& matching_font_candidate) { return matching_font_candidate.key.weight >= target_weight; })
                          : Fn([&](auto const& matching_font_candidate) { return matching_font_candidate.key.weight > target_weight; });
    auto it = find_if(candidates.begin(), candidates.end(), pred);
    for (; it != candidates.end(); ++it)
        if (auto found_font = it->loader->font_with_point_size(font_size_in_pt))
            return found_font;
    return {};
}

RefPtr<Gfx::Font const> StyleComputer::find_matching_font_weight_descending(Vector<MatchingFontCandidate> const& candidates, int target_weight, float font_size_in_pt, bool inclusive)
{
    using Fn = AK::Function<bool(MatchingFontCandidate const&)>;
    auto pred = inclusive ? Fn([&](auto const& matching_font_candidate) { return matching_font_candidate.key.weight <= target_weight; })
                          : Fn([&](auto const& matching_font_candidate) { return matching_font_candidate.key.weight < target_weight; });
    auto it = find_if(candidates.rbegin(), candidates.rend(), pred);
    for (; it != candidates.rend(); ++it)
        if (auto found_font = it->loader->font_with_point_size(font_size_in_pt))
            return found_font;
    return {};
}

// Partial implementation of the font-matching algorithm: https://www.w3.org/TR/css-fonts-4/#font-matching-algorithm
// FIXME: This should be replaced by the full CSS font selection algorithm.
RefPtr<Gfx::Font const> StyleComputer::font_matching_algorithm(FontFaceKey const& key, float font_size_in_pt) const
{
    // If a font family match occurs, the user agent assembles the set of font faces in that family and then
    // narrows the set to a single face using other font properties in the order given below.
    Vector<MatchingFontCandidate> matching_family_fonts;
    for (auto const& font_key_and_loader : m_loaded_fonts) {
        if (font_key_and_loader.key.family_name.equals_ignoring_ascii_case(key.family_name))
            matching_family_fonts.empend(font_key_and_loader.key, font_key_and_loader.value.ptr());
    }
    // FIXME: 1. font-stretch is tried first.
    // FIXME: 2. font-style is tried next.
    // We don't have complete support of italic and oblique fonts, so matching on font-style can be simplified to:
    // If a matching slope is found, all faces which don't have that matching slope are excluded from the matching set.
    auto style_it = find_if(matching_family_fonts.begin(), matching_family_fonts.end(),
        [&](auto const& matching_font_candidate) { return matching_font_candidate.key.slope == key.slope; });
    if (style_it != matching_family_fonts.end()) {
        matching_family_fonts.remove_all_matching([&](auto const& matching_font_candidate) {
            return matching_font_candidate.key.slope != key.slope;
        });
    }
    // 3. font-weight is matched next.
    // If the desired weight is inclusively between 400 and 500, weights greater than or equal to the target weight
    // are checked in ascending order until 500 is hit and checked, followed by weights less than the target weight
    // in descending order, followed by weights greater than 500, until a match is found.
    if (key.weight >= 400 && key.weight <= 500) {
        auto it = find_if(matching_family_fonts.begin(), matching_family_fonts.end(),
            [&](auto const& matching_font_candidate) { return matching_font_candidate.key.weight >= key.weight; });
        for (; it != matching_family_fonts.end() && it->key.weight <= 500; ++it) {
            if (auto found_font = it->loader->font_with_point_size(font_size_in_pt))
                return found_font;
        }
        if (auto found_font = find_matching_font_weight_descending(matching_family_fonts, key.weight, font_size_in_pt, false))
            return found_font;
        for (; it != matching_family_fonts.end(); ++it) {
            if (auto found_font = it->loader->font_with_point_size(font_size_in_pt))
                return found_font;
        }
    }
    // If the desired weight is less than 400, weights less than or equal to the desired weight are checked in descending order
    // followed by weights above the desired weight in ascending order until a match is found.
    if (key.weight < 400) {
        if (auto found_font = find_matching_font_weight_descending(matching_family_fonts, key.weight, font_size_in_pt, true))
            return found_font;
        if (auto found_font = find_matching_font_weight_ascending(matching_family_fonts, key.weight, font_size_in_pt, false))
            return found_font;
    }
    // If the desired weight is greater than 500, weights greater than or equal to the desired weight are checked in ascending order
    // followed by weights below the desired weight in descending order until a match is found.
    if (key.weight > 500) {
        if (auto found_font = find_matching_font_weight_ascending(matching_family_fonts, key.weight, font_size_in_pt, true))
            return found_font;
        if (auto found_font = find_matching_font_weight_descending(matching_family_fonts, key.weight, font_size_in_pt, false))
            return found_font;
    }
    return {};
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

    auto weight = font_weight->to_font_weight();

    bool bold = weight > Gfx::FontWeight::Regular;

    // FIXME: Should be based on "user's default font size"
    float font_size_in_px = 16;

    auto parent_line_height = parent_or_root_element_line_height(element, pseudo_element);
    Gfx::FontPixelMetrics font_pixel_metrics;
    if (parent_element && parent_element->computed_css_values())
        font_pixel_metrics = parent_element->computed_css_values()->computed_font().pixel_metrics();
    else
        font_pixel_metrics = Platform::FontPlugin::the().default_font().pixel_metrics();
    auto parent_font_size = [&]() -> CSSPixels {
        if (!parent_element || !parent_element->computed_css_values())
            return font_size_in_px;
        auto value = parent_element->computed_css_values()->property(CSS::PropertyID::FontSize);
        if (value->is_length()) {
            auto length = value->as_length().length();
            auto parent_line_height = parent_or_root_element_line_height(parent_element, {});
            if (length.is_absolute() || length.is_relative()) {
                Length::FontMetrics font_metrics { font_size_in_px, font_pixel_metrics, parent_line_height };
                return length.to_px(viewport_rect(), font_metrics, m_root_element_font_metrics);
            }
        }
        return font_size_in_px;
    };
    Length::FontMetrics font_metrics { parent_font_size(), font_pixel_metrics, parent_line_height };

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
        Length::ResolutionContext const length_resolution_context {
            .viewport_rect = viewport_rect(),
            .font_metrics = font_metrics,
            .root_font_metrics = m_root_element_font_metrics,
        };

        Optional<Length> maybe_length;
        if (font_size->is_percentage()) {
            // Percentages refer to parent element's font size
            maybe_length = Length::make_px(font_size->as_percentage().percentage().as_fraction() * parent_font_size().to_double());

        } else if (font_size->is_length()) {
            maybe_length = font_size->as_length().length();
        } else if (font_size->is_calculated()) {
            maybe_length = font_size->as_calculated().resolve_length(length_resolution_context);
        }
        if (maybe_length.has_value()) {
            auto px = maybe_length.value().to_px(length_resolution_context).to_int();
            if (px != 0)
                font_size_in_px = px;
        }
    }

    auto slope = font_style->to_font_slope();

    // FIXME: Implement the full font-matching algorithm: https://www.w3.org/TR/css-fonts-4/#font-matching-algorithm

    // Note: This is modified by the find_font() lambda
    FontSelector font_selector;
    bool monospace = false;

    float const font_size_in_pt = font_size_in_px * 0.75f;

    auto find_font = [&](String const& family) -> RefPtr<Gfx::Font const> {
        font_selector = { family, font_size_in_pt, weight, width, slope };

        FontFaceKey key {
            .family_name = family,
            .weight = weight,
            .slope = slope,
        };

        if (auto it = m_loaded_fonts.find(key); it != m_loaded_fonts.end()) {
            auto& loader = *it->value;
            if (auto found_font = loader.font_with_point_size(font_size_in_pt))
                return found_font;
        }

        if (auto found_font = font_matching_algorithm(key, font_size_in_pt))
            return found_font;

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
        return find_font(String::from_deprecated_string(Platform::FontPlugin::the().generic_font_name(generic_font)).release_value_but_fixme_should_propagate_errors());
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
        if (found_font) {
            if (auto scaled_fallback_font = found_font->with_size(font_size_in_pt))
                found_font = scaled_fallback_font;
        }
    }

    FontCache::the().set(font_selector, *found_font);

    style.set_property(CSS::PropertyID::FontSize, LengthStyleValue::create(CSS::Length::make_px(font_size_in_px)).release_value_but_fixme_should_propagate_errors(), nullptr);
    style.set_property(CSS::PropertyID::FontWeight, NumberStyleValue::create(weight).release_value_but_fixme_should_propagate_errors(), nullptr);

    style.set_computed_font(found_font.release_nonnull());

    if (element && is<HTML::HTMLHtmlElement>(*element)) {
        const_cast<StyleComputer&>(*this).m_root_element_font_metrics = calculate_root_element_font_metrics(style);
    }
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
        return m_root_element_font_metrics.line_height;
    auto const* computed_values = parent_element->computed_css_values();
    auto parent_font_pixel_metrics = computed_values->computed_font().pixel_metrics();
    auto parent_font_size = computed_values->property(CSS::PropertyID::FontSize)->as_length().length();
    // FIXME: Can the parent font size be non-absolute here?
    auto parent_font_size_value = parent_font_size.is_absolute() ? parent_font_size.absolute_length_to_px() : m_root_element_font_metrics.font_size;
    auto parent_parent_line_height = parent_or_root_element_line_height(parent_element, {});
    Length::FontMetrics parent_font_metrics { parent_font_size_value, parent_font_pixel_metrics, parent_parent_line_height };
    return computed_values->line_height(viewport_rect(), parent_font_metrics, m_root_element_font_metrics);
}

ErrorOr<void> StyleComputer::absolutize_values(StyleProperties& style, DOM::Element const* element, Optional<CSS::Selector::PseudoElement> pseudo_element) const
{
    auto parent_or_root_line_height = parent_or_root_element_line_height(element, pseudo_element);

    auto font_pixel_metrics = style.computed_font().pixel_metrics();

    Length::FontMetrics font_metrics { m_root_element_font_metrics.font_size, font_pixel_metrics, parent_or_root_line_height };

    auto font_size = style.property(CSS::PropertyID::FontSize)->as_length().length().to_px(viewport_rect(), font_metrics, m_root_element_font_metrics);
    font_metrics.font_size = font_size;

    // NOTE: Percentage line-height values are relative to the font-size of the element.
    //       We have to resolve them right away, so that the *computed* line-height is ready for inheritance.
    //       We can't simply absolutize *all* percentage values against the font size,
    //       because most percentages are relative to containing block metrics.
    auto line_height_value_slot = style.m_property_values[to_underlying(CSS::PropertyID::LineHeight)].map([](auto& x) -> auto& { return x.style; });
    if (line_height_value_slot.has_value() && (*line_height_value_slot)->is_percentage()) {
        *line_height_value_slot = TRY(LengthStyleValue::create(
            Length::make_px(font_size * static_cast<double>((*line_height_value_slot)->as_percentage().percentage().as_fraction()))));
    }

    auto line_height = style.line_height(viewport_rect(), font_metrics, m_root_element_font_metrics);
    font_metrics.line_height = line_height;

    // NOTE: line-height might be using lh which should be resolved against the parent line height (like we did here already)
    if (line_height_value_slot.has_value() && (*line_height_value_slot)->is_length())
        (*line_height_value_slot) = TRY(LengthStyleValue::create(Length::make_px(line_height)));

    for (size_t i = 0; i < style.m_property_values.size(); ++i) {
        auto& value_slot = style.m_property_values[i];
        if (!value_slot.has_value())
            continue;
        value_slot->style = TRY(value_slot->style->absolutized(viewport_rect(), font_metrics, m_root_element_font_metrics));
    }
    return {};
}

enum class BoxTypeTransformation {
    None,
    Blockify,
    Inlinify,
};

static BoxTypeTransformation required_box_type_transformation(StyleProperties const& style, DOM::Element const& element, Optional<CSS::Selector::PseudoElement> const& pseudo_element)
{
    // Absolute positioning or floating an element blockifies the box’s display type. [CSS2]
    if (style.position() == CSS::Position::Absolute || style.position() == CSS::Position::Fixed || style.float_() != CSS::Float::None)
        return BoxTypeTransformation::Blockify;

    // FIXME: Containment in a ruby container inlinifies the box’s display type, as described in [CSS-RUBY-1].

    // NOTE: If we're computing style for a pseudo-element, the effective parent will be the originating element itself, not its parent.
    auto const* parent = pseudo_element.has_value() ? &element : element.parent_element();

    // A parent with a grid or flex display value blockifies the box’s display type. [CSS-GRID-1] [CSS-FLEXBOX-1]
    if (parent && parent->computed_css_values()) {
        auto const& parent_display = parent->computed_css_values()->display();
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

    auto display = style.display();
    if (display.is_none() || display.is_contents())
        return;

    auto new_display = display;

    switch (required_box_type_transformation(style, element, pseudo_element)) {
    case BoxTypeTransformation::None:
        break;
    case BoxTypeTransformation::Blockify:
        if (display.is_block_outside())
            return;
        // If a layout-internal box is blockified, its inner display type converts to flow so that it becomes a block container.
        if (display.is_internal()) {
            new_display = CSS::Display::from_short(CSS::Display::Short::Block);
        } else {
            VERIFY(display.is_outside_and_inside());

            // For legacy reasons, if an inline block box (inline flow-root) is blockified, it becomes a block box (losing its flow-root nature).
            // For consistency, a run-in flow-root box also blockifies to a block box.
            if (display.is_inline_block()) {
                new_display = CSS::Display { CSS::Display::Outside::Block, CSS::Display::Inside::Flow, display.list_item() };
            } else {
                new_display = CSS::Display { CSS::Display::Outside::Block, display.inside(), display.list_item() };
            }
        }
        break;
    case BoxTypeTransformation::Inlinify:
        if (display.is_inline_outside()) {
            // FIXME: If an inline box (inline flow) is inlinified, it recursively inlinifies all of its in-flow children,
            //        so that no block-level descendants break up the inline formatting context in which it participates.
            if (display.is_flow_inside()) {
                dbgln("FIXME: Inlinify inline box children recursively");
            }
            break;
        }
        if (display.is_internal()) {
            // Inlinification has no effect on layout-internal boxes. (However, placement in such an inline context will typically cause them
            // to be wrapped in an appropriately-typed anonymous inline-level box.)
        } else {
            VERIFY(display.is_outside_and_inside());

            // If a block box (block flow) is inlinified, its inner display type is set to flow-root so that it remains a block container.
            if (display.is_block_outside() && display.is_flow_inside()) {
                new_display = CSS::Display { CSS::Display::Outside::Inline, CSS::Display::Inside::FlowRoot, display.list_item() };
            }

            new_display = CSS::Display { CSS::Display::Outside::Inline, display.inside(), display.list_item() };
        }
        break;
    }

    if (new_display != display)
        style.set_property(CSS::PropertyID::Display, DisplayStyleValue::create(new_display).release_value_but_fixme_should_propagate_errors(), style.property_source_declaration(CSS::PropertyID::Display));
}

NonnullRefPtr<StyleProperties> StyleComputer::create_document_style() const
{
    auto style = StyleProperties::create();
    compute_font(style, nullptr, {});
    compute_defaulted_values(style, nullptr, {});
    absolutize_values(style, nullptr, {}).release_value_but_fixme_should_propagate_errors();
    style->set_property(CSS::PropertyID::Width, CSS::LengthStyleValue::create(CSS::Length::make_px(viewport_rect().width())).release_value_but_fixme_should_propagate_errors(), nullptr);
    style->set_property(CSS::PropertyID::Height, CSS::LengthStyleValue::create(CSS::Length::make_px(viewport_rect().height())).release_value_but_fixme_should_propagate_errors(), nullptr);
    style->set_property(CSS::PropertyID::Display, CSS::DisplayStyleValue::create(CSS::Display::from_short(CSS::Display::Short::Block)).release_value_but_fixme_should_propagate_errors(), nullptr);
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
    TRY(absolutize_values(style, &element, pseudo_element));

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

        sheet.for_each_effective_keyframes_at_rule([&](CSSKeyframesRule const& rule) {
            auto keyframe_set = make<AnimationKeyFrameSet>();
            AnimationKeyFrameSet::ResolvedKeyFrame resolved_keyframe;

            // Forwards pass, resolve all the user-specified keyframe properties.
            for (auto const& keyframe : rule.keyframes()) {
                auto key = static_cast<u64>(keyframe->key().value() * AnimationKeyFrameKeyScaleFactor);
                auto keyframe_rule = keyframe->style();

                if (!is<PropertyOwningCSSStyleDeclaration>(*keyframe_rule))
                    continue;

                auto current_keyframe = resolved_keyframe;
                auto& keyframe_style = static_cast<PropertyOwningCSSStyleDeclaration const&>(*keyframe_rule);
                for (auto& property : keyframe_style.properties())
                    current_keyframe.resolved_properties[to_underlying(property.property_id)] = property.value;

                resolved_keyframe = move(current_keyframe);
                keyframe_set->keyframes_by_key.insert(key, resolved_keyframe);
            }

            // If there is no 'from' keyframe, make a synthetic one.
            auto made_a_synthetic_from_keyframe = false;
            if (!keyframe_set->keyframes_by_key.find(0)) {
                keyframe_set->keyframes_by_key.insert(0, AnimationKeyFrameSet::ResolvedKeyFrame());
                made_a_synthetic_from_keyframe = true;
            }

            // Backwards pass, resolve all the implied properties, go read <https://drafts.csswg.org/css-animations-2/#keyframe-processing> to see why.
            auto first = true;
            for (auto const& keyframe : rule.keyframes().in_reverse()) {
                auto key = static_cast<u64>(keyframe->key().value() * AnimationKeyFrameKeyScaleFactor);
                auto keyframe_rule = keyframe->style();

                if (!is<PropertyOwningCSSStyleDeclaration>(*keyframe_rule))
                    continue;

                // The last keyframe is already fully resolved.
                if (first) {
                    first = false;
                    continue;
                }

                auto next_keyframe = resolved_keyframe;
                auto& current_keyframes = *keyframe_set->keyframes_by_key.find(key);

                for (auto it = next_keyframe.resolved_properties.begin(); !it.is_end(); ++it) {
                    auto& current_property = current_keyframes.resolved_properties[it.index()];
                    if (!current_property.has<Empty>() || it->has<Empty>())
                        continue;

                    if (key == 0)
                        current_property = AnimationKeyFrameSet::ResolvedKeyFrame::UseInitial();
                    else
                        current_property = *it;
                }

                resolved_keyframe = current_keyframes;
            }

            if (made_a_synthetic_from_keyframe && !first) {
                auto next_keyframe = resolved_keyframe;
                auto& current_keyframes = *keyframe_set->keyframes_by_key.find(0);

                for (auto it = next_keyframe.resolved_properties.begin(); !it.is_end(); ++it) {
                    auto& current_property = current_keyframes.resolved_properties[it.index()];
                    if (!current_property.has<Empty>() || it->has<Empty>())
                        continue;
                    current_property = AnimationKeyFrameSet::ResolvedKeyFrame::UseInitial();
                }

                resolved_keyframe = current_keyframes;
            }

            if constexpr (LIBWEB_CSS_DEBUG) {
                dbgln("Resolved keyframe set '{}' into {} keyframes:", rule.name(), keyframe_set->keyframes_by_key.size());
                for (auto it = keyframe_set->keyframes_by_key.begin(); it != keyframe_set->keyframes_by_key.end(); ++it) {
                    size_t props = 0;
                    for (auto& entry : it->resolved_properties)
                        props += !entry.has<Empty>();
                    dbgln("    - keyframe {}: {} properties", it.key(), props);
                }
            }

            rule_cache->rules_by_animation_keyframes.set(rule.name(), move(keyframe_set));
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
        FontFaceKey key {
            .family_name = font_face.font_family(),
            .weight = font_face.weight().value_or(0),
            .slope = font_face.slope().value_or(0),
        };
        if (m_loaded_fonts.contains(key))
            continue;

        Vector<AK::URL> urls;
        for (auto& source : font_face.sources()) {
            // FIXME: These should be loaded relative to the stylesheet URL instead of the document URL.
            urls.append(m_document->parse_url(source.url.to_deprecated_string()));
        }

        if (urls.is_empty())
            continue;

        auto loader = make<FontLoader>(const_cast<StyleComputer&>(*this), font_face.font_family(), move(urls));
        const_cast<StyleComputer&>(*this).m_loaded_fonts.set(key, move(loader));
    }
}

}
