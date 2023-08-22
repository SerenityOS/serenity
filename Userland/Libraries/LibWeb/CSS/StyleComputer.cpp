/*
 * Copyright (c) 2018-2023, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, the SerenityOS developers.
 * Copyright (c) 2021-2023, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/BinarySearch.h>
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
#include <LibGfx/Font/WOFF2/Font.h>
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
#include <LibWeb/CSS/StyleValues/EasingStyleValue.h>
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
#include <LibWeb/CSS/StyleValues/MathDepthStyleValue.h>
#include <LibWeb/CSS/StyleValues/NumberStyleValue.h>
#include <LibWeb/CSS/StyleValues/OverflowStyleValue.h>
#include <LibWeb/CSS/StyleValues/PercentageStyleValue.h>
#include <LibWeb/CSS/StyleValues/PlaceContentStyleValue.h>
#include <LibWeb/CSS/StyleValues/PlaceItemsStyleValue.h>
#include <LibWeb/CSS/StyleValues/PlaceSelfStyleValue.h>
#include <LibWeb/CSS/StyleValues/PositionStyleValue.h>
#include <LibWeb/CSS/StyleValues/RectStyleValue.h>
#include <LibWeb/CSS/StyleValues/StringStyleValue.h>
#include <LibWeb/CSS/StyleValues/StyleValueList.h>
#include <LibWeb/CSS/StyleValues/TextDecorationStyleValue.h>
#include <LibWeb/CSS/StyleValues/TimeStyleValue.h>
#include <LibWeb/CSS/StyleValues/TransformationStyleValue.h>
#include <LibWeb/CSS/StyleValues/UnresolvedStyleValue.h>
#include <LibWeb/CSS/StyleValues/UnsetStyleValue.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/Element.h>
#include <LibWeb/FontCache.h>
#include <LibWeb/HTML/HTMLBRElement.h>
#include <LibWeb/HTML/HTMLHtmlElement.h>
#include <LibWeb/Layout/Node.h>
#include <LibWeb/Loader/ResourceLoader.h>
#include <LibWeb/Namespace.h>
#include <LibWeb/Platform/FontPlugin.h>
#include <LibWeb/ReferrerPolicy/AbstractOperations.h>
#include <stdio.h>

namespace AK {

// traits for FontFaceKey
template<>
struct Traits<Web::CSS::FontFaceKey> : public GenericTraits<Web::CSS::FontFaceKey> {
    static unsigned hash(Web::CSS::FontFaceKey const& key) { return pair_int_hash(key.family_name.hash(), pair_int_hash(key.weight, key.slope)); }
};

}

namespace Web::CSS {

static DOM::Element const* element_to_inherit_style_from(DOM::Element const*, Optional<CSS::Selector::PseudoElement>);
static NonnullRefPtr<StyleValue const> get_inherit_value(JS::Realm& initial_value_context_realm, CSS::PropertyID, DOM::Element const*, Optional<CSS::Selector::PseudoElement>);

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

        // HACK: We're crudely computing the referer value and shoving it into the
        //       request until fetch infrastructure is used here.
        auto referrer_url = ReferrerPolicy::strip_url_for_use_as_referrer(m_style_computer.document().url());
        if (referrer_url.has_value() && !request.headers().contains("Referer"))
            request.set_header("Referer", referrer_url->serialize());

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
        if (mime_type == "font/woff2"sv || mime_type == "application/font-woff2"sv) {
            auto woff2 = WOFF2::Font::try_load_from_externally_owned_memory(resource()->encoded_data());
            if (woff2.is_error()) {
                dbgln("WOFF2 error: {}", woff2.error());
                return woff2.release_error();
            }
            return woff2.release_value();
        }
        auto ttf = OpenType::Font::try_load_from_externally_owned_memory(resource()->encoded_data());
        if (!ttf.is_error())
            return ttf.release_value();
        auto woff = WOFF::Font::try_load_from_externally_owned_memory(resource()->encoded_data());
        if (!woff.is_error())
            return woff.release_value();
        auto woff2 = WOFF2::Font::try_load_from_externally_owned_memory(resource()->encoded_data());
        if (!woff2.is_error())
            return woff2.release_value();
        return woff2.release_error();
    }

    StyleComputer& m_style_computer;
    FlyString m_family_name;
    RefPtr<Gfx::VectorFont> m_vector_font;
    Vector<AK::URL> m_urls;

    HashMap<float, NonnullRefPtr<Gfx::ScaledFont>> mutable m_cached_fonts;
};

struct StyleComputer::MatchingFontCandidate {
    FontFaceKey key;
    Variant<FontLoader*, Gfx::Typeface const*> loader_or_typeface;

    [[nodiscard]] RefPtr<Gfx::Font const> font_with_point_size(float point_size) const
    {
        if (auto* loader = loader_or_typeface.get_pointer<FontLoader*>(); loader) {
            return (*loader)->font_with_point_size(point_size);
        }
        return loader_or_typeface.get<Gfx::Typeface const*>()->get_font(point_size);
    }
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

static CSSStyleSheet& mathml_stylesheet(DOM::Document const& document)
{
    static JS::Handle<CSSStyleSheet> sheet;
    if (!sheet.cell()) {
        extern StringView mathml_stylesheet_source;
        sheet = JS::make_handle(parse_css_stylesheet(CSS::Parser::ParsingContext(document), mathml_stylesheet_source));
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
        callback(mathml_stylesheet(document()));
    }
    if (cascade_origin == CascadeOrigin::User) {
        if (m_user_style_sheet)
            callback(*m_user_style_sheet);
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
    case CascadeOrigin::User:
        return *m_user_rule_cache;
    case CascadeOrigin::UserAgent:
        return *m_user_agent_rule_cache;
    default:
        TODO();
    }
}

[[nodiscard]] static bool filter_namespace_rule(DOM::Element const& element, MatchingRule const& rule)
{
    // FIXME: Filter out non-default namespace using prefixes
    auto namespace_uri = rule.sheet->default_namespace();
    if (namespace_uri.has_value() && namespace_uri.value() != element.namespace_uri()) {
        return false;
    }
    return true;
}

Vector<MatchingRule> StyleComputer::collect_matching_rules(DOM::Element const& element, CascadeOrigin cascade_origin, Optional<CSS::Selector::PseudoElement> pseudo_element) const
{
    auto const& rule_cache = rule_cache_for_cascade_origin(cascade_origin);

    Vector<MatchingRule> rules_to_run;
    auto add_rules_to_run = [&](Vector<MatchingRule> const& rules) {
        rules_to_run.grow_capacity(rules_to_run.size() + rules.size());
        if (pseudo_element.has_value()) {
            for (auto const& rule : rules) {
                if (rule.contains_pseudo_element && filter_namespace_rule(element, rule))
                    rules_to_run.append(rule);
            }
        } else {
            for (auto const& rule : rules) {
                if (filter_namespace_rule(element, rule))
                    rules_to_run.append(rule);
            }
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
        if (SelectorEngine::matches(selector, *rule_to_run.sheet, element, pseudo_element))
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

static void set_property_expanding_shorthands(StyleProperties& style, CSS::PropertyID property_id, StyleValue const& value, DOM::Document& document, CSS::CSSStyleDeclaration const* declaration, StyleProperties::PropertyValues const& properties_for_revert)
{
    auto set_longhand_property = [&](CSS::PropertyID property_id, StyleValue const& value) {
        if (value.is_revert()) {
            auto& property_in_previous_cascade_origin = properties_for_revert[to_underlying(property_id)];
            if (property_in_previous_cascade_origin.has_value())
                style.set_property(property_id, property_in_previous_cascade_origin->style, property_in_previous_cascade_origin->declaration);
        } else {
            style.set_property(property_id, value, declaration);
        }
    };

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
        case PropertyID::InsetBlockStart:
            return PropertyID::Top;
        case PropertyID::InsetBlockEnd:
            return PropertyID::Bottom;
        case PropertyID::InsetInlineStart:
            return PropertyID::Left;
        case PropertyID::InsetInlineEnd:
            return PropertyID::Right;
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
        case PropertyID::InsetBlock:
            return StartAndEndPropertyIDs { PropertyID::Top, PropertyID::Bottom };
        case PropertyID::InsetInline:
            return StartAndEndPropertyIDs { PropertyID::Left, PropertyID::Right };
        default:
            return {};
        }
    };

    if (auto real_property_id = map_logical_property_to_real_property(property_id); real_property_id.has_value())
        return set_property_expanding_shorthands(style, real_property_id.value(), value, document, declaration, properties_for_revert);

    if (auto real_property_ids = map_logical_property_to_real_properties(property_id); real_property_ids.has_value()) {
        if (value.is_value_list() && value.as_value_list().size() == 2) {
            auto const& start = value.as_value_list().values()[0];
            auto const& end = value.as_value_list().values()[1];
            set_property_expanding_shorthands(style, real_property_ids->start, start, document, declaration, properties_for_revert);
            set_property_expanding_shorthands(style, real_property_ids->end, end, document, declaration, properties_for_revert);
            return;
        }
        set_property_expanding_shorthands(style, real_property_ids->start, value, document, declaration, properties_for_revert);
        set_property_expanding_shorthands(style, real_property_ids->end, value, document, declaration, properties_for_revert);
        return;
    }

    if (value.is_composite()) {
        auto& composite_value = value.as_composite();
        auto& properties = composite_value.sub_properties();
        auto& values = composite_value.values();
        for (size_t i = 0; i < properties.size(); ++i)
            set_property_expanding_shorthands(style, properties[i], values[i], document, declaration, properties_for_revert);
    }

    auto assign_edge_values = [&](PropertyID top_property, PropertyID right_property, PropertyID bottom_property, PropertyID left_property, auto const& values) {
        if (values.size() == 4) {
            set_longhand_property(top_property, values[0]);
            set_longhand_property(right_property, values[1]);
            set_longhand_property(bottom_property, values[2]);
            set_longhand_property(left_property, values[3]);
        } else if (values.size() == 3) {
            set_longhand_property(top_property, values[0]);
            set_longhand_property(right_property, values[1]);
            set_longhand_property(bottom_property, values[2]);
            set_longhand_property(left_property, values[1]);
        } else if (values.size() == 2) {
            set_longhand_property(top_property, values[0]);
            set_longhand_property(right_property, values[1]);
            set_longhand_property(bottom_property, values[0]);
            set_longhand_property(left_property, values[1]);
        } else if (values.size() == 1) {
            set_longhand_property(top_property, values[0]);
            set_longhand_property(right_property, values[0]);
            set_longhand_property(bottom_property, values[0]);
            set_longhand_property(left_property, values[0]);
        }
    };

    if (property_id == CSS::PropertyID::TextDecoration) {
        if (value.is_text_decoration()) {
            auto const& text_decoration = value.as_text_decoration();
            set_longhand_property(CSS::PropertyID::TextDecorationLine, text_decoration.line());
            set_longhand_property(CSS::PropertyID::TextDecorationThickness, text_decoration.thickness());
            set_longhand_property(CSS::PropertyID::TextDecorationStyle, text_decoration.style());
            set_longhand_property(CSS::PropertyID::TextDecorationColor, text_decoration.color());
            return;
        }

        set_longhand_property(CSS::PropertyID::TextDecorationLine, value);
        set_longhand_property(CSS::PropertyID::TextDecorationThickness, value);
        set_longhand_property(CSS::PropertyID::TextDecorationStyle, value);
        set_longhand_property(CSS::PropertyID::TextDecorationColor, value);
        return;
    }

    if (property_id == CSS::PropertyID::Overflow) {
        if (value.is_overflow()) {
            auto const& overflow = value.as_overflow();
            set_longhand_property(CSS::PropertyID::OverflowX, overflow.overflow_x());
            set_longhand_property(CSS::PropertyID::OverflowY, overflow.overflow_y());
            return;
        }

        set_longhand_property(CSS::PropertyID::OverflowX, value);
        set_longhand_property(CSS::PropertyID::OverflowY, value);
        return;
    }

    if (property_id == CSS::PropertyID::PlaceContent) {
        if (value.is_place_content()) {
            auto const& place_content = value.as_place_content();
            set_longhand_property(CSS::PropertyID::AlignContent, place_content.align_content());
            set_longhand_property(CSS::PropertyID::JustifyContent, place_content.justify_content());
            return;
        }

        style.set_property(CSS::PropertyID::AlignContent, value);
        style.set_property(CSS::PropertyID::JustifyContent, value);
        return;
    }

    if (property_id == CSS::PropertyID::PlaceItems) {
        if (value.is_place_items()) {
            auto const& place_items = value.as_place_items();
            set_longhand_property(CSS::PropertyID::AlignItems, place_items.align_items());
            set_longhand_property(CSS::PropertyID::JustifyItems, place_items.justify_items());
            return;
        }

        set_longhand_property(CSS::PropertyID::AlignItems, value);
        set_longhand_property(CSS::PropertyID::JustifyItems, value);
        return;
    }

    if (property_id == CSS::PropertyID::PlaceSelf) {
        if (value.is_place_self()) {
            auto const& place_self = value.as_place_self();
            set_longhand_property(CSS::PropertyID::AlignSelf, place_self.align_self());
            set_longhand_property(CSS::PropertyID::JustifySelf, place_self.justify_self());
            return;
        }

        set_longhand_property(CSS::PropertyID::AlignSelf, value);
        set_longhand_property(CSS::PropertyID::JustifySelf, value);
        return;
    }

    if (property_id == CSS::PropertyID::Border) {
        set_property_expanding_shorthands(style, CSS::PropertyID::BorderTop, value, document, declaration, properties_for_revert);
        set_property_expanding_shorthands(style, CSS::PropertyID::BorderRight, value, document, declaration, properties_for_revert);
        set_property_expanding_shorthands(style, CSS::PropertyID::BorderBottom, value, document, declaration, properties_for_revert);
        set_property_expanding_shorthands(style, CSS::PropertyID::BorderLeft, value, document, declaration, properties_for_revert);
        // FIXME: Also reset border-image, in line with the spec: https://www.w3.org/TR/css-backgrounds-3/#border-shorthands
        return;
    }

    if (property_id == CSS::PropertyID::BorderRadius) {
        if (value.is_border_radius_shorthand()) {
            auto const& shorthand = value.as_border_radius_shorthand();
            set_longhand_property(CSS::PropertyID::BorderTopLeftRadius, shorthand.top_left());
            set_longhand_property(CSS::PropertyID::BorderTopRightRadius, shorthand.top_right());
            set_longhand_property(CSS::PropertyID::BorderBottomRightRadius, shorthand.bottom_right());
            set_longhand_property(CSS::PropertyID::BorderBottomLeftRadius, shorthand.bottom_left());
            return;
        }

        set_longhand_property(CSS::PropertyID::BorderTopLeftRadius, value);
        set_longhand_property(CSS::PropertyID::BorderTopRightRadius, value);
        set_longhand_property(CSS::PropertyID::BorderBottomRightRadius, value);
        set_longhand_property(CSS::PropertyID::BorderBottomLeftRadius, value);
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
                set_longhand_property(PropertyID::BorderTopWidth, border.border_width());
                set_longhand_property(PropertyID::BorderTopStyle, border.border_style());
                set_longhand_property(PropertyID::BorderTopColor, border.border_color());
            }
            if (contains(Edge::Right, edge)) {
                set_longhand_property(PropertyID::BorderRightWidth, border.border_width());
                set_longhand_property(PropertyID::BorderRightStyle, border.border_style());
                set_longhand_property(PropertyID::BorderRightColor, border.border_color());
            }
            if (contains(Edge::Bottom, edge)) {
                set_longhand_property(PropertyID::BorderBottomWidth, border.border_width());
                set_longhand_property(PropertyID::BorderBottomStyle, border.border_style());
                set_longhand_property(PropertyID::BorderBottomColor, border.border_color());
            }
            if (contains(Edge::Left, edge)) {
                set_longhand_property(PropertyID::BorderLeftWidth, border.border_width());
                set_longhand_property(PropertyID::BorderLeftStyle, border.border_style());
                set_longhand_property(PropertyID::BorderLeftColor, border.border_color());
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

        set_longhand_property(CSS::PropertyID::BorderTopStyle, value);
        set_longhand_property(CSS::PropertyID::BorderRightStyle, value);
        set_longhand_property(CSS::PropertyID::BorderBottomStyle, value);
        set_longhand_property(CSS::PropertyID::BorderLeftStyle, value);
        return;
    }

    if (property_id == CSS::PropertyID::BorderWidth) {
        if (value.is_value_list()) {
            auto const& values_list = value.as_value_list();
            assign_edge_values(PropertyID::BorderTopWidth, PropertyID::BorderRightWidth, PropertyID::BorderBottomWidth, PropertyID::BorderLeftWidth, values_list.values());
            return;
        }

        set_longhand_property(CSS::PropertyID::BorderTopWidth, value);
        set_longhand_property(CSS::PropertyID::BorderRightWidth, value);
        set_longhand_property(CSS::PropertyID::BorderBottomWidth, value);
        set_longhand_property(CSS::PropertyID::BorderLeftWidth, value);
        return;
    }

    if (property_id == CSS::PropertyID::BorderColor) {
        if (value.is_value_list()) {
            auto const& values_list = value.as_value_list();
            assign_edge_values(PropertyID::BorderTopColor, PropertyID::BorderRightColor, PropertyID::BorderBottomColor, PropertyID::BorderLeftColor, values_list.values());
            return;
        }

        set_longhand_property(CSS::PropertyID::BorderTopColor, value);
        set_longhand_property(CSS::PropertyID::BorderRightColor, value);
        set_longhand_property(CSS::PropertyID::BorderBottomColor, value);
        set_longhand_property(CSS::PropertyID::BorderLeftColor, value);
        return;
    }

    if (property_id == CSS::PropertyID::Background) {
        if (value.is_background()) {
            auto const& background = value.as_background();
            set_property_expanding_shorthands(style, CSS::PropertyID::BackgroundColor, background.color(), document, declaration, properties_for_revert);
            set_property_expanding_shorthands(style, CSS::PropertyID::BackgroundImage, background.image(), document, declaration, properties_for_revert);
            set_property_expanding_shorthands(style, CSS::PropertyID::BackgroundPosition, background.position(), document, declaration, properties_for_revert);
            set_property_expanding_shorthands(style, CSS::PropertyID::BackgroundSize, background.size(), document, declaration, properties_for_revert);
            set_property_expanding_shorthands(style, CSS::PropertyID::BackgroundRepeat, background.repeat(), document, declaration, properties_for_revert);
            set_property_expanding_shorthands(style, CSS::PropertyID::BackgroundAttachment, background.attachment(), document, declaration, properties_for_revert);
            set_property_expanding_shorthands(style, CSS::PropertyID::BackgroundOrigin, background.origin(), document, declaration, properties_for_revert);
            set_property_expanding_shorthands(style, CSS::PropertyID::BackgroundClip, background.clip(), document, declaration, properties_for_revert);
            return;
        }

        set_property_expanding_shorthands(style, CSS::PropertyID::BackgroundColor, value, document, declaration, properties_for_revert);
        set_property_expanding_shorthands(style, CSS::PropertyID::BackgroundImage, value, document, declaration, properties_for_revert);
        set_property_expanding_shorthands(style, CSS::PropertyID::BackgroundPosition, value, document, declaration, properties_for_revert);
        set_property_expanding_shorthands(style, CSS::PropertyID::BackgroundSize, value, document, declaration, properties_for_revert);
        set_property_expanding_shorthands(style, CSS::PropertyID::BackgroundRepeat, value, document, declaration, properties_for_revert);
        set_property_expanding_shorthands(style, CSS::PropertyID::BackgroundAttachment, value, document, declaration, properties_for_revert);
        set_property_expanding_shorthands(style, CSS::PropertyID::BackgroundOrigin, value, document, declaration, properties_for_revert);
        set_property_expanding_shorthands(style, CSS::PropertyID::BackgroundClip, value, document, declaration, properties_for_revert);
        return;
    }

    if (property_id == CSS::PropertyID::BackgroundPosition) {
        if (value.is_position()) {
            auto const& position = value.as_position();
            set_longhand_property(CSS::PropertyID::BackgroundPositionX, position.edge_x());
            set_longhand_property(CSS::PropertyID::BackgroundPositionY, position.edge_y());
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
            set_longhand_property(CSS::PropertyID::BackgroundPositionX, StyleValueList::create(move(x_positions), values_list.separator()));
            set_longhand_property(CSS::PropertyID::BackgroundPositionY, StyleValueList::create(move(y_positions), values_list.separator()));
        } else {
            set_longhand_property(CSS::PropertyID::BackgroundPositionX, value);
            set_longhand_property(CSS::PropertyID::BackgroundPositionY, value);
        }

        return;
    }

    if (property_id == CSS::PropertyID::Inset) {
        if (value.is_value_list()) {
            auto const& values_list = value.as_value_list();
            assign_edge_values(PropertyID::Top, PropertyID::Right, PropertyID::Bottom, PropertyID::Left, values_list.values());
            return;
        }

        set_longhand_property(CSS::PropertyID::Top, value);
        set_longhand_property(CSS::PropertyID::Right, value);
        set_longhand_property(CSS::PropertyID::Bottom, value);
        set_longhand_property(CSS::PropertyID::Left, value);
        return;
    }

    if (property_id == CSS::PropertyID::Margin) {
        if (value.is_value_list()) {
            auto const& values_list = value.as_value_list();
            assign_edge_values(PropertyID::MarginTop, PropertyID::MarginRight, PropertyID::MarginBottom, PropertyID::MarginLeft, values_list.values());
            return;
        }

        set_longhand_property(CSS::PropertyID::MarginTop, value);
        set_longhand_property(CSS::PropertyID::MarginRight, value);
        set_longhand_property(CSS::PropertyID::MarginBottom, value);
        set_longhand_property(CSS::PropertyID::MarginLeft, value);
        return;
    }

    if (property_id == CSS::PropertyID::Padding) {
        if (value.is_value_list()) {
            auto const& values_list = value.as_value_list();
            assign_edge_values(PropertyID::PaddingTop, PropertyID::PaddingRight, PropertyID::PaddingBottom, PropertyID::PaddingLeft, values_list.values());
            return;
        }

        set_longhand_property(CSS::PropertyID::PaddingTop, value);
        set_longhand_property(CSS::PropertyID::PaddingRight, value);
        set_longhand_property(CSS::PropertyID::PaddingBottom, value);
        set_longhand_property(CSS::PropertyID::PaddingLeft, value);
        return;
    }

    if (property_id == CSS::PropertyID::ListStyle) {
        if (value.is_list_style()) {
            auto const& list_style = value.as_list_style();
            set_longhand_property(CSS::PropertyID::ListStylePosition, list_style.position());
            set_longhand_property(CSS::PropertyID::ListStyleImage, list_style.image());
            set_longhand_property(CSS::PropertyID::ListStyleType, list_style.style_type());
            return;
        }

        set_longhand_property(CSS::PropertyID::ListStylePosition, value);
        set_longhand_property(CSS::PropertyID::ListStyleImage, value);
        set_longhand_property(CSS::PropertyID::ListStyleType, value);
        return;
    }

    if (property_id == CSS::PropertyID::Font) {
        if (value.is_font()) {
            auto const& font_shorthand = value.as_font();
            set_longhand_property(CSS::PropertyID::FontSize, font_shorthand.font_size());
            set_longhand_property(CSS::PropertyID::FontFamily, font_shorthand.font_families());
            set_longhand_property(CSS::PropertyID::FontStretch, font_shorthand.font_stretch());
            set_longhand_property(CSS::PropertyID::FontStyle, font_shorthand.font_style());
            set_longhand_property(CSS::PropertyID::FontWeight, font_shorthand.font_weight());
            set_longhand_property(CSS::PropertyID::LineHeight, font_shorthand.line_height());
            // FIXME: Implement font-variant
            return;
        }

        set_longhand_property(CSS::PropertyID::FontStretch, value);
        set_longhand_property(CSS::PropertyID::FontSize, value);
        set_longhand_property(CSS::PropertyID::FontFamily, value);
        set_longhand_property(CSS::PropertyID::FontStyle, value);
        set_longhand_property(CSS::PropertyID::FontWeight, value);
        set_longhand_property(CSS::PropertyID::LineHeight, value);
        // FIXME: Implement font-variant
        return;
    }

    if (property_id == CSS::PropertyID::Flex) {
        if (value.is_flex()) {
            auto const& flex = value.as_flex();
            set_longhand_property(CSS::PropertyID::FlexGrow, flex.grow());
            set_longhand_property(CSS::PropertyID::FlexShrink, flex.shrink());
            set_longhand_property(CSS::PropertyID::FlexBasis, flex.basis());
            return;
        }

        set_longhand_property(CSS::PropertyID::FlexGrow, value);
        set_longhand_property(CSS::PropertyID::FlexShrink, value);
        set_longhand_property(CSS::PropertyID::FlexBasis, value);
        return;
    }

    if (property_id == CSS::PropertyID::FlexFlow) {
        if (value.is_flex_flow()) {
            auto const& flex_flow = value.as_flex_flow();
            set_longhand_property(CSS::PropertyID::FlexDirection, flex_flow.flex_direction());
            set_longhand_property(CSS::PropertyID::FlexWrap, flex_flow.flex_wrap());
            return;
        }

        set_longhand_property(CSS::PropertyID::FlexDirection, value);
        set_longhand_property(CSS::PropertyID::FlexWrap, value);
        return;
    }

    if (property_id == CSS::PropertyID::GridArea) {
        if (value.is_grid_area_shorthand()) {
            auto const& shorthand = value.as_grid_area_shorthand();
            set_longhand_property(CSS::PropertyID::GridRowStart, shorthand.row_start());
            set_longhand_property(CSS::PropertyID::GridColumnStart, shorthand.column_start());
            set_longhand_property(CSS::PropertyID::GridRowEnd, shorthand.row_end());
            set_longhand_property(CSS::PropertyID::GridColumnEnd, shorthand.column_end());
            return;
        }
        set_longhand_property(CSS::PropertyID::GridRowStart, value);
        set_longhand_property(CSS::PropertyID::GridColumnStart, value);
        set_longhand_property(CSS::PropertyID::GridRowEnd, value);
        set_longhand_property(CSS::PropertyID::GridColumnEnd, value);
        return;
    }

    if (property_id == CSS::PropertyID::GridColumn) {
        if (value.is_grid_track_placement_shorthand()) {
            auto const& shorthand = value.as_grid_track_placement_shorthand();
            set_longhand_property(CSS::PropertyID::GridColumnStart, shorthand.start());
            set_longhand_property(CSS::PropertyID::GridColumnEnd, shorthand.end());
            return;
        }

        set_longhand_property(CSS::PropertyID::GridColumnStart, value);
        set_longhand_property(CSS::PropertyID::GridColumnEnd, value);
        return;
    }

    if (property_id == CSS::PropertyID::GridRow) {
        if (value.is_grid_track_placement_shorthand()) {
            auto const& shorthand = value.as_grid_track_placement_shorthand();
            set_longhand_property(CSS::PropertyID::GridRowStart, shorthand.start());
            set_longhand_property(CSS::PropertyID::GridRowEnd, shorthand.end());
            return;
        }

        set_longhand_property(CSS::PropertyID::GridRowStart, value);
        set_longhand_property(CSS::PropertyID::GridRowEnd, value);
        return;
    }

    if (property_id == CSS::PropertyID::GridTemplate || property_id == CSS::PropertyID::Grid) {
        if (value.is_grid_track_size_list_shorthand()) {
            auto const& shorthand = value.as_grid_track_size_list_shorthand();
            set_longhand_property(CSS::PropertyID::GridTemplateAreas, shorthand.areas());
            set_longhand_property(CSS::PropertyID::GridTemplateRows, shorthand.rows());
            set_longhand_property(CSS::PropertyID::GridTemplateColumns, shorthand.columns());
            return;
        }
        set_longhand_property(CSS::PropertyID::GridTemplateAreas, value);
        set_longhand_property(CSS::PropertyID::GridTemplateRows, value);
        set_longhand_property(CSS::PropertyID::GridTemplateColumns, value);
        return;
    }

    if (property_id == CSS::PropertyID::Gap || property_id == CSS::PropertyID::GridGap) {
        if (value.is_value_list()) {
            auto const& values_list = value.as_value_list();
            set_longhand_property(CSS::PropertyID::RowGap, values_list.values()[0]);
            set_longhand_property(CSS::PropertyID::ColumnGap, values_list.values()[1]);
            return;
        }
        set_longhand_property(CSS::PropertyID::RowGap, value);
        set_longhand_property(CSS::PropertyID::ColumnGap, value);
        return;
    }

    if (property_id == CSS::PropertyID::RowGap || property_id == CSS::PropertyID::GridRowGap) {
        set_longhand_property(CSS::PropertyID::RowGap, value);
        return;
    }

    if (property_id == CSS::PropertyID::ColumnGap || property_id == CSS::PropertyID::GridColumnGap) {
        set_longhand_property(CSS::PropertyID::ColumnGap, value);
        return;
    }

    if (property_id == CSS::PropertyID::MaxInlineSize || property_id == CSS::PropertyID::MinInlineSize) {
        // FIXME: Use writing-mode to determine if we should set width or height.
        bool is_horizontal = true;

        if (is_horizontal) {
            if (property_id == CSS::PropertyID::MaxInlineSize) {
                set_longhand_property(CSS::PropertyID::MaxWidth, value);
            } else {
                set_longhand_property(CSS::PropertyID::MinWidth, value);
            }
        } else {
            if (property_id == CSS::PropertyID::MaxInlineSize) {
                set_longhand_property(CSS::PropertyID::MaxHeight, value);
            } else {
                set_longhand_property(CSS::PropertyID::MinHeight, value);
            }
        }
        return;
    }

    set_longhand_property(property_id, value);
}

void StyleComputer::set_all_properties(DOM::Element& element, Optional<CSS::Selector::PseudoElement> pseudo_element, StyleProperties& style, StyleValue const& value, DOM::Document& document, CSS::CSSStyleDeclaration const* declaration, StyleProperties::PropertyValues const& properties_for_revert) const
{
    for (auto i = to_underlying(CSS::first_longhand_property_id); i <= to_underlying(CSS::last_longhand_property_id); ++i) {
        auto property_id = (CSS::PropertyID)i;

        if (value.is_revert()) {
            style.m_property_values[to_underlying(property_id)] = properties_for_revert[to_underlying(property_id)];
            continue;
        }

        if (value.is_unset()) {
            if (is_inherited_property(property_id))
                style.m_property_values[to_underlying(property_id)] = { { get_inherit_value(document.realm(), property_id, &element, pseudo_element), nullptr } };
            else
                style.m_property_values[to_underlying(property_id)] = { { property_initial_value(document.realm(), property_id), nullptr } };
            continue;
        }

        NonnullRefPtr<StyleValue> property_value = value;
        if (property_value->is_unresolved())
            property_value = Parser::Parser::resolve_unresolved_style_value({}, Parser::ParsingContext { document }, element, pseudo_element, property_id, property_value->as_unresolved());
        if (!property_value->is_unresolved())
            set_property_expanding_shorthands(style, property_id, property_value, document, declaration, properties_for_revert);

        set_property_expanding_shorthands(style, property_id, value, document, declaration, properties_for_revert);
    }
}

void StyleComputer::cascade_declarations(StyleProperties& style, DOM::Element& element, Optional<CSS::Selector::PseudoElement> pseudo_element, Vector<MatchingRule> const& matching_rules, CascadeOrigin cascade_origin, Important important) const
{
    auto properties_for_revert = style.properties();

    for (auto const& match : matching_rules) {
        for (auto const& property : verify_cast<PropertyOwningCSSStyleDeclaration>(match.rule->declaration()).properties()) {
            if (important != property.important)
                continue;

            if (property.property_id == CSS::PropertyID::All) {
                set_all_properties(element, pseudo_element, style, property.value, m_document, &match.rule->declaration(), properties_for_revert);
                continue;
            }

            auto property_value = property.value;
            if (property.value->is_unresolved())
                property_value = Parser::Parser::resolve_unresolved_style_value({}, Parser::ParsingContext { document() }, element, pseudo_element, property.property_id, property.value->as_unresolved());
            if (!property_value->is_unresolved())
                set_property_expanding_shorthands(style, property.property_id, property_value, m_document, &match.rule->declaration(), properties_for_revert);
        }
    }

    if (cascade_origin == CascadeOrigin::Author && !pseudo_element.has_value()) {
        if (auto const* inline_style = verify_cast<ElementInlineCSSStyleDeclaration>(element.inline_style())) {
            for (auto const& property : inline_style->properties()) {
                if (important != property.important)
                    continue;

                if (property.property_id == CSS::PropertyID::All) {
                    set_all_properties(element, pseudo_element, style, property.value, m_document, inline_style, properties_for_revert);
                    continue;
                }

                auto property_value = property.value;
                if (property.value->is_unresolved())
                    property_value = Parser::Parser::resolve_unresolved_style_value({}, Parser::ParsingContext { document() }, element, pseudo_element, property.property_id, property.value->as_unresolved());
                if (!property_value->is_unresolved())
                    set_property_expanding_shorthands(style, property.property_id, property_value, m_document, inline_style, properties_for_revert);
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

// NOTE: Magic values from <https://www.w3.org/TR/css-easing-1/#valdef-cubic-bezier-easing-function-ease>
static auto ease_timing_function = StyleComputer::AnimationTiming::CubicBezier { 0.25, 0.1, 0.25, 1.0 };
static auto ease_in_timing_function = StyleComputer::AnimationTiming::CubicBezier { 0.42, 0.0, 1.0, 1.0 };
static auto ease_out_timing_function = StyleComputer::AnimationTiming::CubicBezier { 0.0, 0.0, 0.58, 1.0 };
static auto ease_in_out_timing_function = StyleComputer::AnimationTiming::CubicBezier { 0.42, 0.0, 0.58, 1.0 };

float StyleComputer::Animation::compute_output_progress(float input_progress) const
{
    auto output_progress = input_progress;
    auto going_forwards = true;
    switch (direction) {
    case AnimationDirection::Alternate:
        if (current_iteration % 2 == 0) {
            output_progress = 1.0f - output_progress;
            going_forwards = false;
        }
        break;
    case AnimationDirection::AlternateReverse:
        if (current_iteration % 2 == 1) {
            output_progress = 1.0f - output_progress;
            going_forwards = false;
        }
        break;
    case AnimationDirection::Normal:
        break;
    case AnimationDirection::Reverse:
        output_progress = 1.0f - output_progress;
        going_forwards = false;
        break;
    }

    if (remaining_delay.to_milliseconds() != 0)
        return output_progress;

    return timing_function.timing_function.visit(
        [&](AnimationTiming::Linear) { return output_progress; },
        [&](AnimationTiming::Steps const& steps) {
            auto before_flag = (current_state == AnimationState::Before && going_forwards) || (current_state == AnimationState::After && !going_forwards);
            auto progress_step = output_progress * static_cast<float>(steps.number_of_steps);
            auto current_step = floorf(progress_step);
            if (steps.jump_at_start)
                current_step += 1;
            if (before_flag && truncf(progress_step) == progress_step)
                current_step -= 1;
            if (output_progress >= 0 && current_step < 0)
                current_step = 0;
            size_t jumps;
            if (steps.jump_at_start ^ steps.jump_at_end)
                jumps = steps.number_of_steps;
            else if (steps.jump_at_start && steps.jump_at_end)
                jumps = steps.number_of_steps + 1;
            else
                jumps = steps.number_of_steps - 1;

            if (output_progress <= 1 && current_step > static_cast<float>(jumps))
                current_step = static_cast<float>(jumps);
            return current_step / static_cast<float>(steps.number_of_steps);
        },
        [&](AnimationTiming::CubicBezier const& bezier) {
            // Special cases first:
            if (bezier == AnimationTiming::CubicBezier { 0.0, 0.0, 1.0, 1.0 })
                return output_progress;
            // FIXME: This is quite inefficient on memory and CPU, find a better way to do this.
            auto sample = bezier.sample_around(static_cast<double>(output_progress));
            return static_cast<float>(sample.y);
        });
}

static double cubic_bezier_at(double x1, double x2, double t)
{
    auto a = 1.0 - 3.0 * x2 + 3.0 * x1;
    auto b = 3.0 * x2 - 6.0 * x1;
    auto c = 3.0 * x1;

    auto t2 = t * t;
    auto t3 = t2 * t;

    return (a * t3) + (b * t2) + (c * t);
}

StyleComputer::AnimationTiming::CubicBezier::CachedSample StyleComputer::AnimationTiming::CubicBezier::sample_around(double x) const
{
    x = clamp(x, 0, 1);

    auto solve = [&](auto t) {
        auto x = cubic_bezier_at(x1, x2, t);
        auto y = cubic_bezier_at(y1, y2, t);
        return CachedSample { x, y, t };
    };

    if (m_cached_x_samples.is_empty())
        m_cached_x_samples.append(solve(0.));

    size_t nearby_index = 0;
    if (auto found = binary_search(m_cached_x_samples, x, &nearby_index, [](auto x, auto& sample) {
            if (x > sample.x)
                return 1;
            if (x < sample.x)
                return -1;
            return 0;
        }))
        return *found;

    if (nearby_index == m_cached_x_samples.size() || nearby_index + 1 == m_cached_x_samples.size()) {
        // Produce more samples until we have enough.
        auto last_t = m_cached_x_samples.is_empty() ? 0 : m_cached_x_samples.last().t;
        auto last_x = m_cached_x_samples.is_empty() ? 0 : m_cached_x_samples.last().x;
        while (last_x <= x) {
            last_t += 1. / 60.;
            auto solution = solve(last_t);
            m_cached_x_samples.append(solution);
            last_x = solution.x;
        }

        if (auto found = binary_search(m_cached_x_samples, x, &nearby_index, [](auto x, auto& sample) {
                if (x > sample.x)
                    return 1;
                if (x < sample.x)
                    return -1;
                return 0;
            }))
            return *found;
    }

    // We have two samples on either side of the x value we want, so we can linearly interpolate between them.
    auto& sample1 = m_cached_x_samples[nearby_index];
    auto& sample2 = m_cached_x_samples[nearby_index + 1];
    auto factor = (x - sample1.x) / (sample2.x - sample1.x);
    return CachedSample {
        x,
        clamp(sample1.y + factor * (sample2.y - sample1.y), 0, 1),
        sample1.t + factor * (sample2.t - sample1.t),
    };
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
    matching_rule_set.user_rules = collect_matching_rules(element, CascadeOrigin::User, pseudo_element);
    sort_matching_rules(matching_rule_set.user_rules);
    matching_rule_set.author_rules = collect_matching_rules(element, CascadeOrigin::Author, pseudo_element);
    sort_matching_rules(matching_rule_set.author_rules);

    if (mode == ComputeStyleMode::CreatePseudoElementStyleIfNeeded) {
        VERIFY(pseudo_element.has_value());
        if (matching_rule_set.author_rules.is_empty() && matching_rule_set.user_rules.is_empty() && matching_rule_set.user_agent_rules.is_empty()) {
            did_match_any_pseudo_element_rules = false;
            return {};
        }
        did_match_any_pseudo_element_rules = true;
    }

    // Then we resolve all the CSS custom pr`operties ("variables") for this element:
    TRY(cascade_custom_properties(element, pseudo_element, matching_rule_set.author_rules));

    // Then we apply the declarations from the matched rules in cascade order:

    // Normal user agent declarations
    cascade_declarations(style, element, pseudo_element, matching_rule_set.user_agent_rules, CascadeOrigin::UserAgent, Important::No);

    // Normal user declarations
    cascade_declarations(style, element, pseudo_element, matching_rule_set.user_rules, CascadeOrigin::User, Important::No);

    // Author presentational hints (NOTE: The spec doesn't say exactly how to prioritize these.)
    if (!pseudo_element.has_value()) {
        element.apply_presentational_hints(style);

        // SVG presentation attributes are parsed as CSS values, so we need to handle potential custom properties here.
        if (element.is_svg_element()) {
            // FIXME: This is not very efficient, we should only resolve the custom properties that are actually used.
            for (auto i = to_underlying(CSS::first_property_id); i <= to_underlying(CSS::last_property_id); ++i) {
                auto property_id = (CSS::PropertyID)i;
                auto& property = style.m_property_values[i];
                if (property.has_value() && property->style->is_unresolved())
                    property->style = Parser::Parser::resolve_unresolved_style_value({}, Parser::ParsingContext { document() }, element, pseudo_element, property_id, property->style->as_unresolved());
            }
        }
    }

    // Normal author declarations
    cascade_declarations(style, element, pseudo_element, matching_rule_set.author_rules, CascadeOrigin::Author, Important::No);

    // Animation declarations [css-animations-2]
    auto get_animation_name = [&]() -> Optional<String> {
        auto animation_name = style.maybe_null_property(PropertyID::AnimationName);
        if (animation_name.is_null())
            return OptionalNone {};
        if (animation_name->is_string())
            return animation_name->as_string().string_value();
        return animation_name->to_string();
    };
    if (auto animation_name = get_animation_name(); animation_name.has_value()) {
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
            } else if (!animation_name->is_empty()) {
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

                    AnimationTiming timing_function { ease_timing_function };
                    if (auto timing_property = style.maybe_null_property(PropertyID::AnimationTimingFunction); timing_property && timing_property->is_easing()) {
                        auto& easing_value = timing_property->as_easing();
                        switch (easing_value.easing_function()) {
                        case EasingFunction::Linear:
                            timing_function = AnimationTiming { AnimationTiming::Linear {} };
                            break;
                        case EasingFunction::Ease:
                            timing_function = AnimationTiming { ease_timing_function };
                            break;
                        case EasingFunction::EaseIn:
                            timing_function = AnimationTiming { ease_in_timing_function };
                            break;
                        case EasingFunction::EaseOut:
                            timing_function = AnimationTiming { ease_out_timing_function };
                            break;
                        case EasingFunction::EaseInOut:
                            timing_function = AnimationTiming { ease_in_out_timing_function };
                            break;
                        case EasingFunction::CubicBezier: {
                            auto values = easing_value.values();
                            timing_function = AnimationTiming {
                                AnimationTiming::CubicBezier {
                                    values[0]->as_number().number(),
                                    values[1]->as_number().number(),
                                    values[2]->as_number().number(),
                                    values[3]->as_number().number(),
                                },
                            };
                            break;
                        }
                        case EasingFunction::Steps: {
                            auto values = easing_value.values();
                            auto jump_at_start = false;
                            auto jump_at_end = true;

                            if (values.size() > 1) {
                                auto identifier = values[1]->to_identifier();
                                switch (identifier) {
                                case ValueID::JumpStart:
                                case ValueID::Start:
                                    jump_at_start = true;
                                    jump_at_end = false;
                                    break;
                                case ValueID::JumpEnd:
                                case ValueID::End:
                                    jump_at_start = false;
                                    jump_at_end = true;
                                    break;
                                case ValueID::JumpNone:
                                    jump_at_start = false;
                                    jump_at_end = false;
                                    break;
                                default:
                                    break;
                                }
                            }

                            timing_function = AnimationTiming { AnimationTiming::Steps {
                                .number_of_steps = static_cast<size_t>(max(values[0]->as_integer().integer(), !(jump_at_end && jump_at_start) ? 1 : 0)),
                                .jump_at_start = jump_at_start,
                                .jump_at_end = jump_at_end,
                            } };
                            break;
                        }
                        case EasingFunction::StepEnd:
                            timing_function = AnimationTiming { AnimationTiming::Steps {
                                .number_of_steps = 1,
                                .jump_at_start = false,
                                .jump_at_end = true,
                            } };
                            break;
                        case EasingFunction::StepStart:
                            timing_function = AnimationTiming { AnimationTiming::Steps {
                                .number_of_steps = 1,
                                .jump_at_start = true,
                                .jump_at_end = false,
                            } };
                            break;
                        }
                    }

                    auto animation = make<Animation>(Animation {
                        .name = animation_name.release_value(),
                        .duration = duration,
                        .delay = delay,
                        .iteration_count = iteration_count,
                        .timing_function = timing_function,
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

    // Important user declarations
    cascade_declarations(style, element, pseudo_element, matching_rule_set.user_rules, CascadeOrigin::User, Important::Yes);

    // Important user agent declarations
    cascade_declarations(style, element, pseudo_element, matching_rule_set.user_agent_rules, CascadeOrigin::UserAgent, Important::Yes);

    // FIXME: Transition declarations [css-transitions-1]

    return {};
}

DOM::Element const* element_to_inherit_style_from(DOM::Element const* element, Optional<CSS::Selector::PseudoElement> pseudo_element)
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

NonnullRefPtr<StyleValue const> get_inherit_value(JS::Realm& initial_value_context_realm, CSS::PropertyID property_id, DOM::Element const* element, Optional<CSS::Selector::PseudoElement> pseudo_element)
{
    auto* parent_element = element_to_inherit_style_from(element, pseudo_element);

    if (!parent_element || !parent_element->computed_css_values())
        return property_initial_value(initial_value_context_realm, property_id);
    return parent_element->computed_css_values()->property(property_id);
}

void StyleComputer::compute_defaulted_property_value(StyleProperties& style, DOM::Element const* element, CSS::PropertyID property_id, Optional<CSS::Selector::PseudoElement> pseudo_element) const
{
    // FIXME: If we don't know the correct initial value for a property, we fall back to InitialStyleValue.

    auto& value_slot = style.m_property_values[to_underlying(property_id)];
    if (!value_slot.has_value()) {
        if (is_inherited_property(property_id))
            style.m_property_values[to_underlying(property_id)] = { { get_inherit_value(document().realm(), property_id, element, pseudo_element), nullptr } };
        else
            style.m_property_values[to_underlying(property_id)] = { { property_initial_value(document().realm(), property_id), nullptr } };
        return;
    }

    if (value_slot->style->is_initial()) {
        value_slot->style = property_initial_value(document().realm(), property_id);
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
            value_slot->style = property_initial_value(document().realm(), property_id);
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
    Length::FontMetrics font_metrics { m_default_font_metrics.font_size, font_pixel_metrics, CSSPixels::nearest_value_for(font_pixel_metrics.line_spacing()) };
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
    for (; it != candidates.end(); ++it) {
        if (auto found_font = it->font_with_point_size(font_size_in_pt))
            return found_font;
    }
    return {};
}

RefPtr<Gfx::Font const> StyleComputer::find_matching_font_weight_descending(Vector<MatchingFontCandidate> const& candidates, int target_weight, float font_size_in_pt, bool inclusive)
{
    using Fn = AK::Function<bool(MatchingFontCandidate const&)>;
    auto pred = inclusive ? Fn([&](auto const& matching_font_candidate) { return matching_font_candidate.key.weight <= target_weight; })
                          : Fn([&](auto const& matching_font_candidate) { return matching_font_candidate.key.weight < target_weight; });
    auto it = find_if(candidates.rbegin(), candidates.rend(), pred);
    for (; it != candidates.rend(); ++it) {
        if (auto found_font = it->font_with_point_size(font_size_in_pt))
            return found_font;
    }
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
    Gfx::FontDatabase::the().for_each_typeface_with_family_name(key.family_name.to_string(), [&](Gfx::Typeface const& typeface) {
        matching_family_fonts.empend(
            FontFaceKey {
                .family_name = typeface.family(),
                .weight = static_cast<int>(typeface.weight()),
                .slope = typeface.slope(),
            },
            &typeface);
    });
    quick_sort(matching_family_fonts, [](auto const& a, auto const& b) {
        return a.key.weight < b.key.weight;
    });
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
            if (auto found_font = it->font_with_point_size(font_size_in_pt))
                return found_font;
        }
        if (auto found_font = find_matching_font_weight_descending(matching_family_fonts, key.weight, font_size_in_pt, false))
            return found_font;
        for (; it != matching_family_fonts.end(); ++it) {
            if (auto found_font = it->font_with_point_size(font_size_in_pt))
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

RefPtr<Gfx::Font const> StyleComputer::compute_font_for_style_values(DOM::Element const* element, Optional<CSS::Selector::PseudoElement> pseudo_element, StyleValue const& font_family, StyleValue const& font_size, StyleValue const& font_style, StyleValue const& font_weight, StyleValue const& font_stretch, int math_depth) const
{
    auto* parent_element = element_to_inherit_style_from(element, pseudo_element);

    auto width = font_stretch.to_font_stretch_width();

    auto weight = font_weight.to_font_weight();
    bool bold = weight > Gfx::FontWeight::Regular;

    // FIXME: Should be based on "user's default font size"
    CSSPixels font_size_in_px = 16;

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

    if (font_size.is_identifier()) {
        // https://w3c.github.io/csswg-drafts/css-fonts/#absolute-size-mapping
        auto get_absolute_size_mapping = [](Web::CSS::ValueID identifier) -> CSSPixelFraction {
            switch (identifier) {
            case CSS::ValueID::XxSmall:
                return CSSPixels(3) / 5;
            case CSS::ValueID::XSmall:
                return CSSPixels(3) / 4;
            case CSS::ValueID::Small:
                return CSSPixels(8) / 9;
            case CSS::ValueID::Medium:
                return 1;
            case CSS::ValueID::Large:
                return CSSPixels(6) / 5;
            case CSS::ValueID::XLarge:
                return CSSPixels(3) / 2;
            case CSS::ValueID::XxLarge:
                return 2;
            case CSS::ValueID::XxxLarge:
                return 3;
            case CSS::ValueID::Smaller:
                return CSSPixels(4) / 5;
            case CSS::ValueID::Larger:
                return CSSPixels(5) / 4;
            default:
                return 1;
            }
        };

        auto const identifier = static_cast<IdentifierStyleValue const&>(font_size).id();

        if (identifier == ValueID::Math) {
            auto math_scaling_factor = [&]() {
                // https://w3c.github.io/mathml-core/#the-math-script-level-property
                // If the specified value font-size is math then the computed value of font-size is obtained by multiplying
                // the inherited value of font-size by a nonzero scale factor calculated by the following procedure:
                // 1. Let A be the inherited math-depth value, B the computed math-depth value, C be 0.71 and S be 1.0
                int inherited_math_depth = parent_element && parent_element->computed_css_values()
                    ? parent_element->computed_css_values()->math_depth()
                    : InitialValues::math_depth();
                int computed_math_depth = math_depth;
                auto size_ratio = 0.71;
                auto scale = 1.0;
                // 2. If A = B then return S.
                bool invert_scale_factor = false;
                if (inherited_math_depth == computed_math_depth) {
                    return scale;
                }
                //    If B < A, swap A and B and set InvertScaleFactor to true.
                else if (computed_math_depth < inherited_math_depth) {
                    AK::swap(inherited_math_depth, computed_math_depth);
                    invert_scale_factor = true;
                }
                //    Otherwise B > A and set InvertScaleFactor to false.
                else {
                    invert_scale_factor = false;
                }
                // 3. Let E be B - A > 0.
                double e = (computed_math_depth - inherited_math_depth) > 0;
                // FIXME: 4. If the inherited first available font has an OpenType MATH table:
                //    - If A ≤ 0 and B ≥ 2 then multiply S by scriptScriptPercentScaleDown and decrement E by 2.
                //    - Otherwise if A = 1 then multiply S by scriptScriptPercentScaleDown / scriptPercentScaleDown and decrement E by 1.
                //    - Otherwise if B = 1 then multiply S by scriptPercentScaleDown and decrement E by 1.
                // 5. Multiply S by C^E.
                scale *= AK::pow(size_ratio, e);
                // 6. Return S if InvertScaleFactor is false and 1/S otherwise.
                if (!invert_scale_factor)
                    return scale;
                return 1.0 / scale;
            };
            font_size_in_px = parent_font_size().scale_by(math_scaling_factor());
        } else {
            // https://w3c.github.io/csswg-drafts/css-fonts/#valdef-font-size-relative-size
            // TODO: If the parent element has a keyword font size in the absolute size keyword mapping table,
            //       larger may compute the font size to the next entry in the table,
            //       and smaller may compute the font size to the previous entry in the table.
            if (identifier == CSS::ValueID::Smaller || identifier == CSS::ValueID::Larger) {
                if (parent_element && parent_element->computed_css_values()) {
                    font_size_in_px = CSSPixels::nearest_value_for(parent_element->computed_css_values()->computed_font().pixel_metrics().size);
                }
            }
            font_size_in_px *= get_absolute_size_mapping(identifier);
        }
    } else {
        Length::ResolutionContext const length_resolution_context {
            .viewport_rect = viewport_rect(),
            .font_metrics = font_metrics,
            .root_font_metrics = m_root_element_font_metrics,
        };

        Optional<Length> maybe_length;
        if (font_size.is_percentage()) {
            // Percentages refer to parent element's font size
            maybe_length = Length::make_px(CSSPixels::nearest_value_for(font_size.as_percentage().percentage().as_fraction() * parent_font_size().to_double()));

        } else if (font_size.is_length()) {
            maybe_length = font_size.as_length().length();
        } else if (font_size.is_calculated()) {
            maybe_length = font_size.as_calculated().resolve_length(length_resolution_context);
        }
        if (maybe_length.has_value()) {
            font_size_in_px = maybe_length.value().to_px(length_resolution_context);
        }
    }

    auto slope = font_style.to_font_slope();

    // FIXME: Implement the full font-matching algorithm: https://www.w3.org/TR/css-fonts-4/#font-matching-algorithm

    // Note: This is modified by the find_font() lambda
    FontSelector font_selector;
    bool monospace = false;

    float const font_size_in_pt = font_size_in_px * 0.75f;

    auto find_font = [&](FlyString const& family) -> RefPtr<Gfx::Font const> {
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

        if (auto found_font = m_font_cache.get(font_selector))
            return found_font;

        if (auto found_font = font_matching_algorithm(key, font_size_in_pt))
            return found_font;

        if (auto found_font = Gfx::FontDatabase::the().get(family, font_size_in_pt, weight, width, slope, Gfx::Font::AllowInexactSizeMatch::Yes))
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
        return find_font(Platform::FontPlugin::the().generic_font_name(generic_font));
    };

    RefPtr<Gfx::Font const> found_font;

    if (font_family.is_value_list()) {
        auto const& family_list = static_cast<StyleValueList const&>(font_family).values();
        for (auto const& family : family_list) {
            if (family->is_identifier()) {
                found_font = find_generic_font(family->to_identifier());
            } else if (family->is_string()) {
                found_font = find_font(family->as_string().string_value());
            }
            if (found_font)
                break;
        }
    } else if (font_family.is_identifier()) {
        found_font = find_generic_font(font_family.to_identifier());
    } else if (font_family.is_string()) {
        found_font = find_font(font_family.as_string().string_value());
    }

    if (!found_font) {
        found_font = StyleProperties::font_fallback(monospace, bold);
        if (found_font) {
            if (auto scaled_fallback_font = found_font->with_size(font_size_in_pt))
                found_font = scaled_fallback_font;
        }
    }

    m_font_cache.set(font_selector, *found_font);

    return found_font;
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

    auto font_family = style.property(CSS::PropertyID::FontFamily);
    auto font_size = style.property(CSS::PropertyID::FontSize);
    auto font_style = style.property(CSS::PropertyID::FontStyle);
    auto font_weight = style.property(CSS::PropertyID::FontWeight);
    auto font_stretch = style.property(CSS::PropertyID::FontStretch);

    auto found_font = compute_font_for_style_values(element, pseudo_element, font_family, font_size, font_style, font_weight, font_stretch, style.math_depth());

    style.set_property(CSS::PropertyID::FontSize, LengthStyleValue::create(CSS::Length::make_px(CSSPixels::nearest_value_for(found_font->pixel_size()))), nullptr);
    style.set_property(CSS::PropertyID::FontWeight, NumberStyleValue::create(font_weight->to_font_weight()));

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
    if (!computed_values)
        return m_root_element_font_metrics.line_height;
    auto parent_font_pixel_metrics = computed_values->computed_font().pixel_metrics();
    auto parent_font_size = computed_values->property(CSS::PropertyID::FontSize)->as_length().length();
    // FIXME: Can the parent font size be non-absolute here?
    auto parent_font_size_value = parent_font_size.is_absolute() ? parent_font_size.absolute_length_to_px() : m_root_element_font_metrics.font_size;
    auto parent_parent_line_height = parent_or_root_element_line_height(parent_element, {});
    Length::FontMetrics parent_font_metrics { parent_font_size_value, parent_font_pixel_metrics, parent_parent_line_height };
    return computed_values->line_height(viewport_rect(), parent_font_metrics, m_root_element_font_metrics);
}

void StyleComputer::absolutize_values(StyleProperties& style, DOM::Element const* element, Optional<CSS::Selector::PseudoElement> pseudo_element) const
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
        *line_height_value_slot = LengthStyleValue::create(
            Length::make_px(CSSPixels::nearest_value_for(font_size * static_cast<double>((*line_height_value_slot)->as_percentage().percentage().as_fraction()))));
    }

    auto line_height = style.line_height(viewport_rect(), font_metrics, m_root_element_font_metrics);
    font_metrics.line_height = line_height;

    // NOTE: line-height might be using lh which should be resolved against the parent line height (like we did here already)
    if (line_height_value_slot.has_value() && (*line_height_value_slot)->is_length())
        (*line_height_value_slot) = LengthStyleValue::create(Length::make_px(line_height));

    for (size_t i = 0; i < style.m_property_values.size(); ++i) {
        auto& value_slot = style.m_property_values[i];
        if (!value_slot.has_value())
            continue;
        value_slot->style = value_slot->style->absolutized(viewport_rect(), font_metrics, m_root_element_font_metrics);
    }
}

enum class BoxTypeTransformation {
    None,
    Blockify,
    Inlinify,
};

static BoxTypeTransformation required_box_type_transformation(StyleProperties const& style, DOM::Element const& element, Optional<CSS::Selector::PseudoElement> const& pseudo_element)
{
    // NOTE: We never blockify <br> elements. They are always inline.
    //       There is currently no way to express in CSS how a <br> element really behaves.
    //       Spec issue: https://github.com/whatwg/html/issues/2291
    if (is<HTML::HTMLBRElement>(element))
        return BoxTypeTransformation::None;

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

    if (display.is_math_inside()) {
        // https://w3c.github.io/mathml-core/#new-display-math-value
        // For elements that are not MathML elements, if the specified value of display is inline math or block math
        // then the computed value is block flow and inline flow respectively.
        if (element.namespace_() != Namespace::MathML)
            new_display = CSS::Display { display.outside(), CSS::DisplayInside::Flow };
        // For the mtable element the computed value is block table and inline table respectively.
        else if (element.tag_name().equals_ignoring_ascii_case("mtable"sv))
            new_display = CSS::Display { display.outside(), CSS::DisplayInside::Table };
        // For the mtr element, the computed value is table-row.
        else if (element.tag_name().equals_ignoring_ascii_case("mtr"sv))
            new_display = CSS::Display { CSS::DisplayInternal::TableRow };
        // For the mtd element, the computed value is table-cell.
        else if (element.tag_name().equals_ignoring_ascii_case("mtd"sv))
            new_display = CSS::Display { CSS::DisplayInternal::TableCell };
    }

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
                new_display = CSS::Display { CSS::DisplayOutside::Block, CSS::DisplayInside::Flow, display.list_item() };
            } else {
                new_display = CSS::Display { CSS::DisplayOutside::Block, display.inside(), display.list_item() };
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
                new_display = CSS::Display { CSS::DisplayOutside::Inline, CSS::DisplayInside::FlowRoot, display.list_item() };
            }

            new_display = CSS::Display { CSS::DisplayOutside::Inline, display.inside(), display.list_item() };
        }
        break;
    }

    if (new_display != display)
        style.set_property(CSS::PropertyID::Display, DisplayStyleValue::create(new_display), style.property_source_declaration(CSS::PropertyID::Display));
}

NonnullRefPtr<StyleProperties> StyleComputer::create_document_style() const
{
    auto style = StyleProperties::create();
    compute_math_depth(style, nullptr, {});
    compute_font(style, nullptr, {});
    compute_defaulted_values(style, nullptr, {});
    absolutize_values(style, nullptr, {});
    style->set_property(CSS::PropertyID::Width, CSS::LengthStyleValue::create(CSS::Length::make_px(viewport_rect().width())), nullptr);
    style->set_property(CSS::PropertyID::Height, CSS::LengthStyleValue::create(CSS::Length::make_px(viewport_rect().height())), nullptr);
    style->set_property(CSS::PropertyID::Display, CSS::DisplayStyleValue::create(CSS::Display::from_short(CSS::Display::Short::Block)), nullptr);
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

    // 2. Compute the math-depth property, since that might affect the font-size
    compute_math_depth(style, &element, pseudo_element);

    // 3. Compute the font, since that may be needed for font-relative CSS units
    compute_font(style, &element, pseudo_element);

    // 4. Absolutize values, turning font/viewport relative lengths into absolute lengths
    absolutize_values(style, &element, pseudo_element);

    // 5. Default the values, applying inheritance and 'initial' as needed
    compute_defaulted_values(style, &element, pseudo_element);

    // 6. Run automatic box type transformations
    transform_box_type_if_needed(style, element, pseudo_element);

    return style;
}

void StyleComputer::build_rule_cache_if_needed() const
{
    if (m_author_rule_cache && m_user_rule_cache && m_user_agent_rule_cache)
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
                    sheet,
                    style_sheet_index,
                    rule_index,
                    selector_index,
                    selector.specificity()
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
                        rule_cache->rules_by_tag_name.ensure(simple_selector.qualified_name().name.lowercase_name).append(move(matching_rule));
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
    // FIXME: How are we sometimes calculating style before the Document has a Page?
    if (document().page()) {
        if (auto user_style_source = document().page()->user_style(); user_style_source.has_value()) {
            m_user_style_sheet = JS::make_handle(parse_css_stylesheet(CSS::Parser::ParsingContext(document()), user_style_source.value()));
        }
    }

    m_author_rule_cache = make_rule_cache_for_cascade_origin(CascadeOrigin::Author);
    m_user_rule_cache = make_rule_cache_for_cascade_origin(CascadeOrigin::User);
    m_user_agent_rule_cache = make_rule_cache_for_cascade_origin(CascadeOrigin::UserAgent);
}

void StyleComputer::invalidate_rule_cache()
{
    m_author_rule_cache = nullptr;

    // NOTE: We could be smarter about keeping the user rule cache, and style sheet.
    //       Currently we are re-parsing the user style sheet every time we build the caches,
    //       as it may have changed.
    m_user_rule_cache = nullptr;
    m_user_style_sheet = nullptr;

    // NOTE: It might not be necessary to throw away the UA rule cache.
    //       If we are sure that it's safe, we could keep it as an optimization.
    m_user_agent_rule_cache = nullptr;
}

CSSPixelRect StyleComputer::viewport_rect() const
{
    if (auto const navigable = document().navigable())
        return navigable->viewport_rect();
    return {};
}

void StyleComputer::did_load_font(FlyString const& family_name)
{
    m_font_cache.did_load_font({}, family_name);
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
            if (source.local_or_url.has<AK::URL>())
                urls.append(m_document->parse_url(source.local_or_url.get<AK::URL>().to_deprecated_string()));
            // FIXME: Handle local()
        }

        if (urls.is_empty())
            continue;

        auto loader = make<FontLoader>(const_cast<StyleComputer&>(*this), font_face.font_family(), move(urls));
        const_cast<StyleComputer&>(*this).m_loaded_fonts.set(key, move(loader));
    }
}

void StyleComputer::compute_math_depth(StyleProperties& style, DOM::Element const* element, Optional<CSS::Selector::PseudoElement> pseudo_element) const
{
    // https://w3c.github.io/mathml-core/#propdef-math-depth

    // First, ensure that the relevant CSS properties have been defaulted.
    // FIXME: This should be more sophisticated.
    compute_defaulted_property_value(style, element, CSS::PropertyID::MathDepth, pseudo_element);
    compute_defaulted_property_value(style, element, CSS::PropertyID::MathStyle, pseudo_element);

    auto inherited_math_depth = [&]() {
        if (!element || !element->parent_element())
            return InitialValues::math_depth();
        return element->parent_element()->computed_css_values()->math_depth();
    };

    auto value = style.property(CSS::PropertyID::MathDepth);
    if (!value->is_math_depth()) {
        style.set_math_depth(inherited_math_depth());
        return;
    }
    auto& math_depth = value->as_math_depth();

    auto resolve_integer = [&](StyleValue const& integer_value) {
        if (integer_value.is_integer())
            return integer_value.as_integer().integer();
        if (integer_value.is_calculated())
            return integer_value.as_calculated().resolve_integer().value();
        VERIFY_NOT_REACHED();
    };

    // The computed value of the math-depth value is determined as follows:
    // - If the specified value of math-depth is auto-add and the inherited value of math-style is compact
    //   then the computed value of math-depth of the element is its inherited value plus one.
    if (math_depth.is_auto_add() && style.property(CSS::PropertyID::MathStyle)->to_identifier() == CSS::ValueID::Compact) {
        style.set_math_depth(inherited_math_depth() + 1);
        return;
    }
    // - If the specified value of math-depth is of the form add(<integer>) then the computed value of
    //   math-depth of the element is its inherited value plus the specified integer.
    if (math_depth.is_add()) {
        style.set_math_depth(inherited_math_depth() + resolve_integer(*math_depth.integer_value()));
        return;
    }
    // - If the specified value of math-depth is of the form <integer> then the computed value of math-depth
    //   of the element is the specified integer.
    if (math_depth.is_integer()) {
        style.set_math_depth(resolve_integer(*math_depth.integer_value()));
        return;
    }
    // - Otherwise, the computed value of math-depth of the element is the inherited one.
    style.set_math_depth(inherited_math_depth());
}

}
