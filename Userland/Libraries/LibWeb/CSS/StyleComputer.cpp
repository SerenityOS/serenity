/*
 * Copyright (c) 2018-2022, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, the SerenityOS developers.
 * Copyright (c) 2021-2022, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Debug.h>
#include <AK/QuickSort.h>
#include <AK/TemporaryChange.h>
#include <LibGfx/Font/Font.h>
#include <LibGfx/Font/FontDatabase.h>
#include <LibGfx/Font/FontStyleMapping.h>
#include <LibGfx/Font/ScaledFont.h>
#include <LibGfx/Font/TrueType/Font.h>
#include <LibGfx/Font/VectorFont.h>
#include <LibGfx/Font/WOFF/Font.h>
#include <LibWeb/CSS/CSSFontFaceRule.h>
#include <LibWeb/CSS/CSSStyleRule.h>
#include <LibWeb/CSS/Parser/Parser.h>
#include <LibWeb/CSS/SelectorEngine.h>
#include <LibWeb/CSS/StyleComputer.h>
#include <LibWeb/CSS/StyleSheet.h>
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
    explicit FontLoader(StyleComputer& style_computer, FlyString family_name, AK::URL url)
        : m_style_computer(style_computer)
        , m_family_name(move(family_name))
    {
        LoadRequest request;
        request.set_url(move(url));
        set_resource(ResourceLoader::the().load_resource(Resource::Type::Generic, request));
    }

    virtual ~FontLoader() override { }

    virtual void resource_did_load() override
    {
        auto result = try_load_font();
        if (result.is_error())
            return;
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
    ErrorOr<NonnullRefPtr<Gfx::VectorFont>> try_load_font()
    {
        // FIXME: This could maybe use the format() provided in @font-face as well, since often the mime type is just application/octet-stream and we have to try every format
        auto mime_type = resource()->mime_type();
        if (mime_type == "font/ttf"sv || mime_type == "application/x-font-ttf"sv)
            return TRY(TTF::Font::try_load_from_externally_owned_memory(resource()->encoded_data()));
        if (mime_type == "font/woff"sv)
            return TRY(WOFF::Font::try_load_from_externally_owned_memory(resource()->encoded_data()));
        auto ttf = TTF::Font::try_load_from_externally_owned_memory(resource()->encoded_data());
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

    HashMap<float, NonnullRefPtr<Gfx::ScaledFont>> mutable m_cached_fonts;
};

static CSSStyleSheet& default_stylesheet()
{
    static JS::Handle<CSSStyleSheet> sheet;
    if (!sheet.cell()) {
        extern char const default_stylesheet_source[];
        String css = default_stylesheet_source;
        sheet = JS::make_handle(parse_css_stylesheet(CSS::Parser::ParsingContext(), css));
    }
    return *sheet;
}

static CSSStyleSheet& quirks_mode_stylesheet()
{
    static JS::Handle<CSSStyleSheet> sheet;
    if (!sheet.cell()) {
        extern char const quirks_mode_stylesheet_source[];
        String css = quirks_mode_stylesheet_source;
        sheet = JS::make_handle(parse_css_stylesheet(CSS::Parser::ParsingContext(), css));
    }
    return *sheet;
}

template<typename Callback>
void StyleComputer::for_each_stylesheet(CascadeOrigin cascade_origin, Callback callback) const
{
    if (cascade_origin == CascadeOrigin::UserAgent) {
        callback(default_stylesheet());
        if (document().in_quirks_mode())
            callback(quirks_mode_stylesheet());
    }
    if (cascade_origin == CascadeOrigin::Author) {
        for (auto const& sheet : document().style_sheets().sheets()) {
            callback(*sheet);
        }
    }
}

Vector<MatchingRule> StyleComputer::collect_matching_rules(DOM::Element const& element, CascadeOrigin cascade_origin, Optional<CSS::Selector::PseudoElement> pseudo_element) const
{
    if (cascade_origin == CascadeOrigin::Author) {
        Vector<MatchingRule> rules_to_run;
        if (pseudo_element.has_value()) {
            if (auto it = m_rule_cache->rules_by_pseudo_element.find(pseudo_element.value()); it != m_rule_cache->rules_by_pseudo_element.end())
                rules_to_run.extend(it->value);
        } else {
            for (auto const& class_name : element.class_names()) {
                if (auto it = m_rule_cache->rules_by_class.find(class_name); it != m_rule_cache->rules_by_class.end())
                    rules_to_run.extend(it->value);
            }
            if (auto id = element.get_attribute(HTML::AttributeNames::id); !id.is_null()) {
                if (auto it = m_rule_cache->rules_by_id.find(id); it != m_rule_cache->rules_by_id.end())
                    rules_to_run.extend(it->value);
            }
            if (auto it = m_rule_cache->rules_by_tag_name.find(element.local_name()); it != m_rule_cache->rules_by_tag_name.end())
                rules_to_run.extend(it->value);
            rules_to_run.extend(m_rule_cache->other_rules);
        }

        Vector<MatchingRule> matching_rules;
        matching_rules.ensure_capacity(rules_to_run.size());
        for (auto const& rule_to_run : rules_to_run) {
            auto const& selector = rule_to_run.rule->selectors()[rule_to_run.selector_index];
            if (SelectorEngine::matches(selector, element, pseudo_element))
                matching_rules.append(rule_to_run);
        }
        return matching_rules;
    }

    Vector<MatchingRule> matching_rules;
    size_t style_sheet_index = 0;
    for_each_stylesheet(cascade_origin, [&](auto& sheet) {
        size_t rule_index = 0;
        sheet.for_each_effective_style_rule([&](auto const& rule) {
            size_t selector_index = 0;
            for (auto& selector : rule.selectors()) {
                if (SelectorEngine::matches(selector, element, pseudo_element)) {
                    matching_rules.append({ &rule, style_sheet_index, rule_index, selector_index, selector.specificity() });
                    break;
                }
                ++selector_index;
            }
            ++rule_index;
        });
        ++style_sheet_index;
    });

    return matching_rules;
}

static void sort_matching_rules(Vector<MatchingRule>& matching_rules)
{
    quick_sort(matching_rules, [&](MatchingRule& a, MatchingRule& b) {
        auto const& a_selector = a.rule->selectors()[a.selector_index];
        auto const& b_selector = b.rule->selectors()[b.selector_index];
        auto a_specificity = a_selector.specificity();
        auto b_specificity = b_selector.specificity();
        if (a_selector.specificity() == b_selector.specificity()) {
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
            style.set_property(CSS::PropertyID::FontStyle, font_shorthand.font_style());
            style.set_property(CSS::PropertyID::FontWeight, font_shorthand.font_weight());
            style.set_property(CSS::PropertyID::LineHeight, font_shorthand.line_height());
            // FIXME: Implement font-stretch and font-variant
            return;
        }

        style.set_property(CSS::PropertyID::FontSize, value);
        style.set_property(CSS::PropertyID::FontFamily, value);
        style.set_property(CSS::PropertyID::FontStyle, value);
        style.set_property(CSS::PropertyID::FontWeight, value);
        style.set_property(CSS::PropertyID::LineHeight, value);
        // FIXME: Implement font-stretch and font-variant
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

    style.set_property(property_id, value);
}

static RefPtr<StyleValue> get_custom_property(DOM::Element const& element, FlyString const& custom_property_name)
{
    for (auto const* current_element = &element; current_element; current_element = current_element->parent_element()) {
        if (auto it = current_element->custom_properties().find(custom_property_name); it != current_element->custom_properties().end())
            return it->value.value;
    }
    return nullptr;
}

bool StyleComputer::expand_unresolved_values(DOM::Element& element, StringView property_name, HashMap<FlyString, NonnullRefPtr<PropertyDependencyNode>>& dependencies, Parser::TokenStream<Parser::ComponentValue>& source, Vector<Parser::ComponentValue>& dest) const
{
    // FIXME: Do this better!
    // We build a copy of the tree of ComponentValues, with all var()s and attr()s replaced with their contents.
    // This is a very naive solution, and we could do better if the CSS Parser could accept tokens one at a time.

    // Arbitrary large value chosen to avoid the billion-laughs attack.
    // https://www.w3.org/TR/css-variables-1/#long-variables
    const size_t MAX_VALUE_COUNT = 16384;
    if (source.remaining_token_count() + dest.size() > MAX_VALUE_COUNT) {
        dbgln("Stopped expanding CSS variables: maximum length reached.");
        return false;
    }

    auto get_dependency_node = [&](auto name) -> NonnullRefPtr<PropertyDependencyNode> {
        if (auto existing = dependencies.get(name); existing.has_value())
            return *existing.value();
        auto new_node = PropertyDependencyNode::create(name);
        dependencies.set(name, new_node);
        return new_node;
    };

    while (source.has_next_token()) {
        auto const& value = source.next_token();
        if (value.is_function()) {
            if (value.function().name().equals_ignoring_case("var"sv)) {
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
                auto parent = get_dependency_node(property_name);
                auto child = get_dependency_node(custom_property_name);
                parent->add_child(child);
                if (parent->has_cycles())
                    return false;

                if (auto custom_property_value = get_custom_property(element, custom_property_name)) {
                    VERIFY(custom_property_value->is_unresolved());
                    Parser::TokenStream custom_property_tokens { custom_property_value->as_unresolved().values() };
                    if (!expand_unresolved_values(element, custom_property_name, dependencies, custom_property_tokens, dest))
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
                    if (!expand_unresolved_values(element, property_name, dependencies, var_contents, dest))
                        return false;
                }
                continue;
            }
            if (value.function().name().equals_ignoring_case("attr"sv)) {
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
                    dest.empend(Parser::Token::of_string(attr_value));
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
                    if (!expand_unresolved_values(element, property_name, dependencies, attr_contents, dest))
                        return false;
                    continue;
                }

                // 3. Otherwise, the property containing the attr() function is invalid at computed-value time.
                return false;
            }

            auto const& source_function = value.function();
            Vector<Parser::ComponentValue> function_values;
            Parser::TokenStream source_function_contents { source_function.values() };
            if (!expand_unresolved_values(element, property_name, dependencies, source_function_contents, function_values))
                return false;
            NonnullRefPtr<Parser::Function> function = Parser::Function::create(source_function.name(), move(function_values));
            dest.empend(function);
            continue;
        }
        if (value.is_block()) {
            auto const& source_block = value.block();
            Parser::TokenStream source_block_values { source_block.values() };
            Vector<Parser::ComponentValue> block_values;
            if (!expand_unresolved_values(element, property_name, dependencies, source_block_values, block_values))
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

    Vector<Parser::ComponentValue> expanded_values;
    HashMap<FlyString, NonnullRefPtr<PropertyDependencyNode>> dependencies;
    Parser::TokenStream unresolved_values { unresolved.values() };
    if (!expand_unresolved_values(element, string_from_property_id(property_id), dependencies, unresolved_values, expanded_values))
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
                set_property_expanding_shorthands(style, property.property_id, property_value, m_document);
            }
        }
    }
}

static void cascade_custom_properties(DOM::Element& element, Vector<MatchingRule> const& matching_rules)
{
    size_t needed_capacity = 0;
    for (auto const& matching_rule : matching_rules)
        needed_capacity += verify_cast<PropertyOwningCSSStyleDeclaration>(matching_rule.rule->declaration()).custom_properties().size();
    if (auto const* inline_style = verify_cast<PropertyOwningCSSStyleDeclaration>(element.inline_style()))
        needed_capacity += inline_style->custom_properties().size();

    HashMap<FlyString, StyleProperty> custom_properties;
    custom_properties.ensure_capacity(needed_capacity);

    for (auto const& matching_rule : matching_rules) {
        for (auto const& it : verify_cast<PropertyOwningCSSStyleDeclaration>(matching_rule.rule->declaration()).custom_properties())
            custom_properties.set(it.key, it.value);
    }

    if (auto const* inline_style = verify_cast<PropertyOwningCSSStyleDeclaration>(element.inline_style())) {
        for (auto const& it : inline_style->custom_properties())
            custom_properties.set(it.key, it.value);
    }

    element.set_custom_properties(move(custom_properties));
}

// https://www.w3.org/TR/css-cascade/#cascading
void StyleComputer::compute_cascaded_values(StyleProperties& style, DOM::Element& element, Optional<CSS::Selector::PseudoElement> pseudo_element) const
{
    // First, we collect all the CSS rules whose selectors match `element`:
    MatchingRuleSet matching_rule_set;
    matching_rule_set.user_agent_rules = collect_matching_rules(element, CascadeOrigin::UserAgent, pseudo_element);
    sort_matching_rules(matching_rule_set.user_agent_rules);
    matching_rule_set.author_rules = collect_matching_rules(element, CascadeOrigin::Author, pseudo_element);
    sort_matching_rules(matching_rule_set.author_rules);

    // Then we resolve all the CSS custom properties ("variables") for this element:
    // FIXME: Look into how custom properties should interact with pseudo elements and support that properly.
    if (!pseudo_element.has_value())
        cascade_custom_properties(element, matching_rule_set.author_rules);

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
}

static DOM::Element const* get_parent_element(DOM::Element const* element, Optional<CSS::Selector::PseudoElement> pseudo_element)
{
    // Pseudo-elements treat their originating element as their parent.
    DOM::Element const* parent_element = nullptr;
    if (pseudo_element.has_value()) {
        parent_element = element;
    } else if (element) {
        parent_element = element->parent_element();
    }
    return parent_element;
}

static NonnullRefPtr<StyleValue> get_inherit_value(CSS::PropertyID property_id, DOM::Element const* element, Optional<CSS::Selector::PseudoElement> pseudo_element)
{
    auto* parent_element = get_parent_element(element, pseudo_element);

    if (!parent_element || !parent_element->computed_css_values())
        return property_initial_value(property_id);
    return parent_element->computed_css_values()->property(property_id);
};

void StyleComputer::compute_defaulted_property_value(StyleProperties& style, DOM::Element const* element, CSS::PropertyID property_id, Optional<CSS::Selector::PseudoElement> pseudo_element) const
{
    // FIXME: If we don't know the correct initial value for a property, we fall back to InitialStyleValue.

    auto& value_slot = style.m_property_values[to_underlying(property_id)];
    if (!value_slot) {
        if (is_inherited_property(property_id))
            style.m_property_values[to_underlying(property_id)] = get_inherit_value(property_id, element, pseudo_element);
        else
            style.m_property_values[to_underlying(property_id)] = property_initial_value(property_id);
        return;
    }

    if (value_slot->is_initial()) {
        value_slot = property_initial_value(property_id);
        return;
    }

    if (value_slot->is_inherit()) {
        value_slot = get_inherit_value(property_id, element, pseudo_element);
        return;
    }

    // https://www.w3.org/TR/css-cascade-4/#inherit-initial
    // If the cascaded value of a property is the unset keyword,
    if (value_slot->is_unset()) {
        if (is_inherited_property(property_id)) {
            // then if it is an inherited property, this is treated as inherit,
            value_slot = get_inherit_value(property_id, element, pseudo_element);
        } else {
            // and if it is not, this is treated as initial.
            value_slot = property_initial_value(property_id);
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

float StyleComputer::root_element_font_size() const
{
    constexpr float default_root_element_font_size = 16;

    auto const* root_element = m_document.first_child_of_type<HTML::HTMLHtmlElement>();
    if (!root_element)
        return default_root_element_font_size;

    auto const* computed_root_style = root_element->computed_css_values();
    if (!computed_root_style)
        return default_root_element_font_size;

    auto root_value = computed_root_style->property(CSS::PropertyID::FontSize);

    return root_value->to_length().to_px(viewport_rect(), computed_root_style->computed_font().pixel_metrics(), default_root_element_font_size, default_root_element_font_size);
}

void StyleComputer::compute_font(StyleProperties& style, DOM::Element const* element, Optional<CSS::Selector::PseudoElement> pseudo_element) const
{
    // To compute the font, first ensure that we've defaulted the relevant CSS font properties.
    // FIXME: This should be more sophisticated.
    compute_defaulted_property_value(style, element, CSS::PropertyID::FontFamily, pseudo_element);
    compute_defaulted_property_value(style, element, CSS::PropertyID::FontSize, pseudo_element);
    compute_defaulted_property_value(style, element, CSS::PropertyID::FontStyle, pseudo_element);
    compute_defaulted_property_value(style, element, CSS::PropertyID::FontWeight, pseudo_element);

    auto* parent_element = get_parent_element(element, pseudo_element);

    auto font_size = style.property(CSS::PropertyID::FontSize);
    auto font_style = style.property(CSS::PropertyID::FontStyle);
    auto font_weight = style.property(CSS::PropertyID::FontWeight);

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
        auto maybe_weight = font_weight->as_calculated().resolve_integer();
        if (maybe_weight.has_value())
            weight = maybe_weight.value();
    }

    bool bold = weight > Gfx::FontWeight::Regular;

    float font_size_in_px = 16;

    if (font_size->is_identifier()) {
        switch (static_cast<IdentifierStyleValue const&>(*font_size).id()) {
        case CSS::ValueID::XxSmall:
        case CSS::ValueID::XSmall:
        case CSS::ValueID::Small:
        case CSS::ValueID::Medium:
            // FIXME: Should be based on "user's default font size"
            font_size_in_px = 16;
            break;
        case CSS::ValueID::Large:
        case CSS::ValueID::XLarge:
        case CSS::ValueID::XxLarge:
        case CSS::ValueID::XxxLarge:
            // FIXME: Should be based on "user's default font size"
            font_size_in_px = 12;
            break;
        case CSS::ValueID::Smaller:
        case CSS::ValueID::Larger:
            // FIXME: Should be based on parent element
            break;
        default:
            break;
        }
    } else {
        float root_font_size = root_element_font_size();

        Gfx::FontPixelMetrics font_metrics;
        if (parent_element && parent_element->computed_css_values())
            font_metrics = parent_element->computed_css_values()->computed_font().pixel_metrics();
        else
            font_metrics = Platform::FontPlugin::the().default_font().pixel_metrics();

        auto parent_font_size = [&]() -> float {
            if (!parent_element || !parent_element->computed_css_values())
                return font_size_in_px;
            auto value = parent_element->computed_css_values()->property(CSS::PropertyID::FontSize);
            if (value->is_length()) {
                auto length = static_cast<LengthStyleValue const&>(*value).to_length();
                if (length.is_absolute() || length.is_relative())
                    return length.to_px(viewport_rect(), font_metrics, font_size_in_px, root_font_size);
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
            maybe_length = Length::make_calculated(font_size->as_calculated());
        }
        if (maybe_length.has_value()) {
            // FIXME: Support font-size: calc(...)
            //        Theoretically we can do this now, but to resolve it we need a layout_node which we might not have. :^(
            if (!maybe_length->is_calculated()) {
                auto px = maybe_length.value().to_px(viewport_rect(), font_metrics, parent_font_size(), root_font_size);
                if (px != 0)
                    font_size_in_px = px;
            }
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

    auto find_font = [&](String const& family) -> RefPtr<Gfx::Font> {
        float font_size_in_pt = font_size_in_px * 0.75f;
        font_selector = { family, font_size_in_pt, weight, slope };

        if (auto it = m_loaded_fonts.find(family); it != m_loaded_fonts.end()) {
            auto& loader = *it->value;
            if (auto found_font = loader.font_with_point_size(font_size_in_pt))
                return found_font;
        }

        if (auto found_font = FontCache::the().get(font_selector))
            return found_font;

        if (auto found_font = Gfx::FontDatabase::the().get(family, font_size_in_pt, weight, slope, Gfx::Font::AllowInexactSizeMatch::Yes))
            return found_font;

        return {};
    };

    auto find_generic_font = [&](ValueID font_id) -> RefPtr<Gfx::Font> {
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
        return find_font(Platform::FontPlugin::the().generic_font_name(generic_font));
    };

    RefPtr<Gfx::Font> found_font;

    auto family_value = style.property(PropertyID::FontFamily);
    if (family_value->is_value_list()) {
        auto const& family_list = static_cast<StyleValueList const&>(*family_value).values();
        for (auto const& family : family_list) {
            if (family.is_identifier()) {
                found_font = find_generic_font(family.to_identifier());
            } else if (family.is_string()) {
                found_font = find_font(family.to_string());
            }
            if (found_font)
                break;
        }
    } else if (family_value->is_identifier()) {
        found_font = find_generic_font(family_value->to_identifier());
    } else if (family_value->is_string()) {
        found_font = find_font(family_value->to_string());
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

void StyleComputer::absolutize_values(StyleProperties& style, DOM::Element const*, Optional<CSS::Selector::PseudoElement>) const
{
    auto font_metrics = style.computed_font().pixel_metrics();
    float root_font_size = root_element_font_size();
    float font_size = style.property(CSS::PropertyID::FontSize)->to_length().to_px(viewport_rect(), font_metrics, root_font_size, root_font_size);

    for (size_t i = 0; i < style.m_property_values.size(); ++i) {
        auto& value_slot = style.m_property_values[i];
        if (!value_slot)
            continue;
        value_slot = value_slot->absolutized(viewport_rect(), font_metrics, font_size, root_font_size);
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

NonnullRefPtr<StyleProperties> StyleComputer::compute_style(DOM::Element& element, Optional<CSS::Selector::PseudoElement> pseudo_element) const
{
    build_rule_cache_if_needed();

    auto style = StyleProperties::create();
    // 1. Perform the cascade. This produces the "specified style"
    compute_cascaded_values(style, element, pseudo_element);

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
        if (child.m_name == new_child->m_name)
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
        if (child.has_cycles())
            return true;
    }
    return false;
}

void StyleComputer::build_rule_cache_if_needed() const
{
    if (m_rule_cache)
        return;
    const_cast<StyleComputer&>(*this).build_rule_cache();
}

void StyleComputer::build_rule_cache()
{
    // FIXME: Make a rule cache for UA style as well.

    m_rule_cache = make<RuleCache>();

    size_t num_class_rules = 0;
    size_t num_id_rules = 0;
    size_t num_tag_name_rules = 0;
    size_t num_pseudo_element_rules = 0;

    Vector<MatchingRule> matching_rules;
    size_t style_sheet_index = 0;
    for_each_stylesheet(CascadeOrigin::Author, [&](auto& sheet) {
        size_t rule_index = 0;
        sheet.for_each_effective_style_rule([&](auto const& rule) {
            size_t selector_index = 0;
            for (CSS::Selector const& selector : rule.selectors()) {
                MatchingRule matching_rule { &rule, style_sheet_index, rule_index, selector_index, selector.specificity() };

                bool added_to_bucket = false;
                for (auto const& simple_selector : selector.compound_selectors().last().simple_selectors) {
                    if (simple_selector.type == CSS::Selector::SimpleSelector::Type::PseudoElement) {
                        m_rule_cache->rules_by_pseudo_element.ensure(simple_selector.pseudo_element()).append(move(matching_rule));
                        ++num_pseudo_element_rules;
                        added_to_bucket = true;
                        break;
                    }
                }
                if (!added_to_bucket) {
                    for (auto const& simple_selector : selector.compound_selectors().last().simple_selectors) {
                        if (simple_selector.type == CSS::Selector::SimpleSelector::Type::Id) {
                            m_rule_cache->rules_by_id.ensure(simple_selector.name()).append(move(matching_rule));
                            ++num_id_rules;
                            added_to_bucket = true;
                            break;
                        }
                        if (simple_selector.type == CSS::Selector::SimpleSelector::Type::Class) {
                            m_rule_cache->rules_by_class.ensure(simple_selector.name()).append(move(matching_rule));
                            ++num_class_rules;
                            added_to_bucket = true;
                            break;
                        }
                        if (simple_selector.type == CSS::Selector::SimpleSelector::Type::TagName) {
                            m_rule_cache->rules_by_tag_name.ensure(simple_selector.name()).append(move(matching_rule));
                            ++num_tag_name_rules;
                            added_to_bucket = true;
                            break;
                        }
                    }
                }
                if (!added_to_bucket)
                    m_rule_cache->other_rules.append(move(matching_rule));

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
        dbgln("        Other: {}", m_rule_cache->other_rules.size());
        dbgln("        Total: {}", num_class_rules + num_id_rules + num_tag_name_rules + m_rule_cache->other_rules.size());
    }
}

void StyleComputer::invalidate_rule_cache()
{
    m_rule_cache = nullptr;
}

Gfx::IntRect StyleComputer::viewport_rect() const
{
    if (auto const* browsing_context = document().browsing_context())
        return browsing_context->viewport_rect();
    return {};
}

void StyleComputer::did_load_font([[maybe_unused]] FlyString const& family_name)
{
    document().invalidate_layout();
}

void StyleComputer::load_fonts_from_sheet(CSSStyleSheet const& sheet)
{
    for (auto const& rule : static_cast<CSSStyleSheet const&>(sheet).rules()) {
        if (!is<CSSFontFaceRule>(rule))
            continue;
        auto const& font_face = static_cast<CSSFontFaceRule const&>(rule).font_face();
        if (font_face.sources().is_empty())
            continue;
        if (m_loaded_fonts.contains(font_face.font_family()))
            continue;

        // NOTE: This is rather ad-hoc, we just look for the first valid
        //       source URL that's either a WOFF or TTF file and try loading that.
        // FIXME: Find out exactly which resources we need to load and how.
        Optional<AK::URL> candidate_url;
        for (auto& source : font_face.sources()) {
            if (!source.url.is_valid())
                continue;

            if (source.url.scheme() != "data") {
                auto path = source.url.path();
                if (!path.ends_with(".woff"sv, AK::CaseSensitivity::CaseInsensitive)
                    && !path.ends_with(".ttf"sv, AK::CaseSensitivity::CaseInsensitive)) {
                    continue;
                }
            }

            candidate_url = source.url;
            break;
        }

        if (!candidate_url.has_value())
            continue;

        LoadRequest request;
        auto url = m_document.parse_url(candidate_url.value().to_string());
        auto loader = make<FontLoader>(const_cast<StyleComputer&>(*this), font_face.font_family(), move(url));
        const_cast<StyleComputer&>(*this).m_loaded_fonts.set(font_face.font_family(), move(loader));
    }
}

}
