/*
 * Copyright (c) 2018-2023, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, the SerenityOS developers.
 * Copyright (c) 2021-2023, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2024, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/BinarySearch.h>
#include <AK/Debug.h>
#include <AK/Error.h>
#include <AK/Find.h>
#include <AK/Function.h>
#include <AK/HashMap.h>
#include <AK/Math.h>
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
#include <LibWeb/Animations/AnimationEffect.h>
#include <LibWeb/Animations/DocumentTimeline.h>
#include <LibWeb/Animations/TimingFunction.h>
#include <LibWeb/CSS/AnimationEvent.h>
#include <LibWeb/CSS/CSSAnimation.h>
#include <LibWeb/CSS/CSSFontFaceRule.h>
#include <LibWeb/CSS/CSSImportRule.h>
#include <LibWeb/CSS/CSSStyleRule.h>
#include <LibWeb/CSS/Parser/Parser.h>
#include <LibWeb/CSS/SelectorEngine.h>
#include <LibWeb/CSS/StyleComputer.h>
#include <LibWeb/CSS/StyleSheet.h>
#include <LibWeb/CSS/StyleValues/AngleStyleValue.h>
#include <LibWeb/CSS/StyleValues/BorderRadiusStyleValue.h>
#include <LibWeb/CSS/StyleValues/ColorStyleValue.h>
#include <LibWeb/CSS/StyleValues/CustomIdentStyleValue.h>
#include <LibWeb/CSS/StyleValues/DisplayStyleValue.h>
#include <LibWeb/CSS/StyleValues/EasingStyleValue.h>
#include <LibWeb/CSS/StyleValues/FilterValueListStyleValue.h>
#include <LibWeb/CSS/StyleValues/GridTrackPlacementStyleValue.h>
#include <LibWeb/CSS/StyleValues/GridTrackSizeListStyleValue.h>
#include <LibWeb/CSS/StyleValues/IdentifierStyleValue.h>
#include <LibWeb/CSS/StyleValues/IntegerStyleValue.h>
#include <LibWeb/CSS/StyleValues/LengthStyleValue.h>
#include <LibWeb/CSS/StyleValues/MathDepthStyleValue.h>
#include <LibWeb/CSS/StyleValues/NumberStyleValue.h>
#include <LibWeb/CSS/StyleValues/PercentageStyleValue.h>
#include <LibWeb/CSS/StyleValues/PositionStyleValue.h>
#include <LibWeb/CSS/StyleValues/RectStyleValue.h>
#include <LibWeb/CSS/StyleValues/ShorthandStyleValue.h>
#include <LibWeb/CSS/StyleValues/StringStyleValue.h>
#include <LibWeb/CSS/StyleValues/StyleValueList.h>
#include <LibWeb/CSS/StyleValues/TimeStyleValue.h>
#include <LibWeb/CSS/StyleValues/TransformationStyleValue.h>
#include <LibWeb/CSS/StyleValues/UnresolvedStyleValue.h>
#include <LibWeb/CSS/StyleValues/UnsetStyleValue.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/Element.h>
#include <LibWeb/HTML/HTMLBRElement.h>
#include <LibWeb/HTML/HTMLHtmlElement.h>
#include <LibWeb/HTML/Scripting/TemporaryExecutionContext.h>
#include <LibWeb/HighResolutionTime/TimeOrigin.h>
#include <LibWeb/Layout/Node.h>
#include <LibWeb/Loader/ResourceLoader.h>
#include <LibWeb/Namespace.h>
#include <LibWeb/Platform/FontPlugin.h>
#include <LibWeb/ReferrerPolicy/AbstractOperations.h>
#include <math.h>
#include <stdio.h>

namespace AK {

// traits for FontFaceKey
template<>
struct Traits<Web::CSS::FontFaceKey> : public DefaultTraits<Web::CSS::FontFaceKey> {
    static unsigned hash(Web::CSS::FontFaceKey const& key) { return pair_int_hash(key.family_name.hash(), pair_int_hash(key.weight, key.slope)); }
};

}

namespace Web::CSS {

static DOM::Element const* element_to_inherit_style_from(DOM::Element const*, Optional<CSS::Selector::PseudoElement::Type>);
static NonnullRefPtr<StyleValue const> get_inherit_value(JS::Realm& initial_value_context_realm, CSS::PropertyID, DOM::Element const*, Optional<CSS::Selector::PseudoElement::Type>);

StyleComputer::StyleComputer(DOM::Document& document)
    : m_document(document)
    , m_default_font_metrics(16, Gfx::FontDatabase::default_font().pixel_metrics())
    , m_root_element_font_metrics(m_default_font_metrics)
{
}

StyleComputer::~StyleComputer() = default;

class StyleComputer::FontLoader : public ResourceClient {
public:
    explicit FontLoader(StyleComputer& style_computer, FlyString family_name, Vector<Gfx::UnicodeRange> unicode_ranges, Vector<AK::URL> urls)
        : m_style_computer(style_computer)
        , m_family_name(move(family_name))
        , m_unicode_ranges(move(unicode_ranges))
        , m_urls(move(urls))
    {
    }

    virtual ~FontLoader() override { }

    Vector<Gfx::UnicodeRange> const& unicode_ranges() const { return m_unicode_ranges; }

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

    RefPtr<Gfx::Font> font_with_point_size(float point_size)
    {
        if (!m_vector_font) {
            start_loading_next_url();
            return nullptr;
        }
        return m_vector_font->scaled_font(point_size);
    }

private:
    void start_loading_next_url()
    {
        if (resource() && resource()->is_pending())
            return;
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
    Vector<Gfx::UnicodeRange> m_unicode_ranges;
    RefPtr<Gfx::VectorFont> m_vector_font;
    Vector<AK::URL> m_urls;
};

struct StyleComputer::MatchingFontCandidate {
    FontFaceKey key;
    Variant<FontLoaderList*, Gfx::Typeface const*> loader_or_typeface;

    [[nodiscard]] RefPtr<Gfx::FontCascadeList const> font_with_point_size(float point_size) const
    {
        RefPtr<Gfx::FontCascadeList> font_list = Gfx::FontCascadeList::create();
        if (auto* loader_list = loader_or_typeface.get_pointer<FontLoaderList*>(); loader_list) {
            for (auto const& loader : **loader_list) {
                if (auto font = loader->font_with_point_size(point_size); font)
                    font_list->add(*font, loader->unicode_ranges());
            }
            return font_list;
        }

        if (auto font = loader_or_typeface.get<Gfx::Typeface const*>()->get_font(point_size))
            font_list->add(*font);
        return font_list;
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

static CSSStyleSheet& svg_stylesheet(DOM::Document const& document)
{
    static JS::Handle<CSSStyleSheet> sheet;
    if (!sheet.cell()) {
        extern StringView svg_stylesheet_source;
        sheet = JS::make_handle(parse_css_stylesheet(CSS::Parser::ParsingContext(document), svg_stylesheet_source));
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
        callback(svg_stylesheet(document()));
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

Vector<MatchingRule> StyleComputer::collect_matching_rules(DOM::Element const& element, CascadeOrigin cascade_origin, Optional<CSS::Selector::PseudoElement::Type> pseudo_element) const
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
    if (auto id = element.id(); id.has_value()) {
        if (auto it = rule_cache.rules_by_id.find(id.value()); it != rule_cache.rules_by_id.end())
            add_rules_to_run(it->value);
    }
    if (auto it = rule_cache.rules_by_tag_name.find(element.local_name()); it != rule_cache.rules_by_tag_name.end())
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
        case PropertyID::WebkitAppearance:
            return PropertyID::Appearance;
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

    if (value.is_shorthand()) {
        auto& shorthand_value = value.as_shorthand();
        auto& properties = shorthand_value.sub_properties();
        auto& values = shorthand_value.values();
        for (size_t i = 0; i < properties.size(); ++i)
            set_property_expanding_shorthands(style, properties[i], values[i], document, declaration, properties_for_revert);
        return;
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

    if (property_id == CSS::PropertyID::Border) {
        set_property_expanding_shorthands(style, CSS::PropertyID::BorderTop, value, document, declaration, properties_for_revert);
        set_property_expanding_shorthands(style, CSS::PropertyID::BorderRight, value, document, declaration, properties_for_revert);
        set_property_expanding_shorthands(style, CSS::PropertyID::BorderBottom, value, document, declaration, properties_for_revert);
        set_property_expanding_shorthands(style, CSS::PropertyID::BorderLeft, value, document, declaration, properties_for_revert);
        // FIXME: Also reset border-image, in line with the spec: https://www.w3.org/TR/css-backgrounds-3/#border-shorthands
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

    if (property_is_shorthand(property_id)) {
        // ShorthandStyleValue was handled already.
        // That means if we got here, that `value` must be a CSS-wide keyword, which we should apply to our longhand properties.
        // We don't directly call `set_longhand_property()` because the longhands might have longhands of their own.
        // (eg `grid` -> `grid-template` -> `grid-template-areas` & `grid-template-rows` & `grid-template-columns`)
        VERIFY(value.is_css_wide_keyword());
        for (auto longhand : longhands_for_shorthand(property_id))
            set_property_expanding_shorthands(style, longhand, value, document, declaration, properties_for_revert);
        return;
    }

    set_longhand_property(property_id, value);
}

void StyleComputer::set_all_properties(DOM::Element& element, Optional<CSS::Selector::PseudoElement::Type> pseudo_element, StyleProperties& style, StyleValue const& value, DOM::Document& document, CSS::CSSStyleDeclaration const* declaration, StyleProperties::PropertyValues const& properties_for_revert) const
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

void StyleComputer::cascade_declarations(StyleProperties& style, DOM::Element& element, Optional<CSS::Selector::PseudoElement::Type> pseudo_element, Vector<MatchingRule> const& matching_rules, CascadeOrigin cascade_origin, Important important) const
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

static ErrorOr<void> cascade_custom_properties(DOM::Element& element, Optional<CSS::Selector::PseudoElement::Type> pseudo_element, Vector<MatchingRule> const& matching_rules)
{
    size_t needed_capacity = 0;
    for (auto const& matching_rule : matching_rules)
        needed_capacity += verify_cast<PropertyOwningCSSStyleDeclaration>(matching_rule.rule->declaration()).custom_properties().size();

    if (!pseudo_element.has_value()) {
        if (auto const* inline_style = verify_cast<PropertyOwningCSSStyleDeclaration>(element.inline_style()))
            needed_capacity += inline_style->custom_properties().size();
    }

    HashMap<FlyString, StyleProperty> custom_properties;
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
        // https://www.w3.org/TR/css-values-4/#combine-positions
        // FIXME: Interpolation of <position> is defined as the independent interpolation of each component (x, y) normalized as an offset from the top left corner as a <length-percentage>.
        auto& from_position = from.as_position();
        auto& to_position = to.as_position();
        return PositionStyleValue::create(
            TRY(interpolate_property(from_position.edge_x(), to_position.edge_x(), delta))->as_edge(),
            TRY(interpolate_property(from_position.edge_y(), to_position.edge_y(), delta))->as_edge());
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

ErrorOr<void> StyleComputer::collect_animation_into(JS::NonnullGCPtr<Animations::KeyframeEffect> effect, StyleProperties& style_properties) const
{
    auto animation = effect->associated_animation();
    if (!animation)
        return {};

    auto output_progress = effect->transformed_progress();
    if (!output_progress.has_value())
        return {};

    if (!effect->key_frame_set())
        return {};

    auto& keyframes = effect->key_frame_set()->keyframes_by_key;
    auto is_backwards = effect->current_direction() == Animations::AnimationDirection::Backwards;

    auto key = static_cast<u64>(output_progress.value() * 100.0 * Animations::KeyframeEffect::AnimationKeyFrameKeyScaleFactor);
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

    if constexpr (LIBWEB_CSS_ANIMATION_DEBUG) {
        auto valid_properties = keyframe_values.resolved_properties.size();
        dbgln("Animation {} contains {} properties to interpolate, progress = {}%", animation->id(), valid_properties, progress_in_keyframe * 100);
    }

    for (auto const& it : keyframe_values.resolved_properties) {
        auto resolve_property = [&](auto& property) {
            return property.visit(
                [&](Animations::KeyframeEffect::KeyFrameSet::UseInitial) -> RefPtr<StyleValue const> {
                    return style_properties.maybe_null_property(it.key);
                },
                [&](RefPtr<StyleValue const> value) { return value; });
        };

        auto resolved_start_property = resolve_property(it.value);

        auto const& end_property = keyframe_end_values.resolved_properties.get(it.key);
        if (!end_property.has_value()) {
            if (resolved_start_property) {
                style_properties.set_property(it.key, *resolved_start_property);
                dbgln_if(LIBWEB_CSS_ANIMATION_DEBUG, "No end property for property {}, using {}", string_from_property_id(it.key), resolved_start_property->to_string());
            }
            continue;
        }

        auto resolved_end_property = resolve_property(end_property.value());

        if (resolved_end_property && !resolved_start_property)
            resolved_start_property = CSS::property_initial_value(document().realm(), it.key);

        if (!resolved_start_property || !resolved_end_property)
            continue;

        auto start = resolved_start_property.release_nonnull();
        auto end = resolved_end_property.release_nonnull();

        auto next_value = TRY(interpolate_property(*start, *end, progress_in_keyframe));
        dbgln_if(LIBWEB_CSS_ANIMATION_DEBUG, "Interpolated value for property {} at {}: {} -> {} = {}", string_from_property_id(it.key), progress_in_keyframe, start->to_string(), end->to_string(), next_value->to_string());
        style_properties.set_property(it.key, next_value);
    }

    return {};
}

// https://www.w3.org/TR/css-cascade/#cascading
ErrorOr<void> StyleComputer::compute_cascaded_values(StyleProperties& style, DOM::Element& element, Optional<CSS::Selector::PseudoElement::Type> pseudo_element, bool& did_match_any_pseudo_element_rules, ComputeStyleMode mode) const
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

    // Then we resolve all the CSS custom properties ("variables") for this element:
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
    auto animation_name = [&]() -> Optional<String> {
        auto animation_name = style.maybe_null_property(PropertyID::AnimationName);
        if (animation_name.is_null())
            return OptionalNone {};
        if (animation_name->is_string())
            return animation_name->as_string().string_value();
        return animation_name->to_string();
    }();

    if (animation_name.has_value()) {
        if (auto source_declaration = style.property_source_declaration(PropertyID::AnimationName); source_declaration && source_declaration != element.cached_animation_name_source()) {
            // This animation name is new, so we need to create a new animation for it.
            element.set_cached_animation_name_source(source_declaration);

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

            double iteration_count = 1.0;
            if (auto iteration_count_value = style.maybe_null_property(PropertyID::AnimationIterationCount); iteration_count_value) {
                if (iteration_count_value->is_identifier() && iteration_count_value->to_identifier() == ValueID::Infinite)
                    iteration_count = HUGE_VAL;
                else if (iteration_count_value->is_number())
                    iteration_count = iteration_count_value->as_number().number();
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

            Animations::TimingFunction timing_function = Animations::ease_timing_function;
            if (auto timing_property = style.maybe_null_property(PropertyID::AnimationTimingFunction); timing_property && timing_property->is_easing()) {
                auto& easing_value = timing_property->as_easing();
                switch (easing_value.easing_function()) {
                case EasingFunction::Linear:
                    timing_function = Animations::linear_timing_function;
                    break;
                case EasingFunction::Ease:
                    timing_function = Animations::ease_timing_function;
                    break;
                case EasingFunction::EaseIn:
                    timing_function = Animations::ease_in_timing_function;
                    break;
                case EasingFunction::EaseOut:
                    timing_function = Animations::ease_out_timing_function;
                    break;
                case EasingFunction::EaseInOut:
                    timing_function = Animations::ease_in_out_timing_function;
                    break;
                case EasingFunction::CubicBezier: {
                    auto values = easing_value.values();
                    timing_function = {
                        Animations::CubicBezierTimingFunction {
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

                    timing_function = Animations::TimingFunction { Animations::StepsTimingFunction {
                        .number_of_steps = static_cast<size_t>(max(values[0]->as_integer().integer(), !(jump_at_end && jump_at_start) ? 1 : 0)),
                        .jump_at_start = jump_at_start,
                        .jump_at_end = jump_at_end,
                    } };
                    break;
                }
                case EasingFunction::StepEnd:
                    timing_function = Animations::TimingFunction { Animations::StepsTimingFunction {
                        .number_of_steps = 1,
                        .jump_at_start = false,
                        .jump_at_end = true,
                    } };
                    break;
                case EasingFunction::StepStart:
                    timing_function = Animations::TimingFunction { Animations::StepsTimingFunction {
                        .number_of_steps = 1,
                        .jump_at_start = true,
                        .jump_at_end = false,
                    } };
                    break;
                }
            }

            auto& realm = element.realm();

            auto effect = Animations::KeyframeEffect::create(realm);
            auto iteration_duration = duration.has_value()
                ? Variant<double, String> { duration.release_value().to_milliseconds() }
                : "auto"_string;
            effect->set_iteration_duration(iteration_duration);
            effect->set_start_delay(delay.to_milliseconds());
            effect->set_iteration_count(iteration_count);
            effect->set_timing_function(move(timing_function));
            effect->set_fill_mode(Animations::css_fill_mode_to_bindings_fill_mode(fill_mode));
            effect->set_playback_direction(Animations::css_animation_direction_to_bindings_playback_direction(direction));

            auto animation = CSSAnimation::create(realm);
            animation->set_id(animation_name.release_value());
            animation->set_timeline(m_document->timeline());
            animation->set_owning_element(element);
            animation->set_effect(effect);

            auto const& rule_cache = rule_cache_for_cascade_origin(CascadeOrigin::Author);
            if (auto keyframe_set = rule_cache.rules_by_animation_keyframes.get(animation->id()); keyframe_set.has_value())
                effect->set_key_frame_set(keyframe_set.value());

            element.associate_with_effect(effect);

            HTML::TemporaryExecutionContext context(m_document->relevant_settings_object());
            animation->play().release_value_but_fixme_should_propagate_errors();
        }
    }

    auto animations = element.get_animations({ .subtree = false });
    for (auto& animation : animations) {
        if (!animation->is_relevant())
            continue;

        if (auto effect = animation->effect(); effect && effect->is_keyframe_effect()) {
            auto& keyframe_effect = *static_cast<Animations::KeyframeEffect*>(effect.ptr());
            TRY(collect_animation_into(keyframe_effect, style));
        }
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

DOM::Element const* element_to_inherit_style_from(DOM::Element const* element, Optional<CSS::Selector::PseudoElement::Type> pseudo_element)
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

NonnullRefPtr<StyleValue const> get_inherit_value(JS::Realm& initial_value_context_realm, CSS::PropertyID property_id, DOM::Element const* element, Optional<CSS::Selector::PseudoElement::Type> pseudo_element)
{
    auto* parent_element = element_to_inherit_style_from(element, pseudo_element);

    if (!parent_element || !parent_element->computed_css_values())
        return property_initial_value(initial_value_context_realm, property_id);
    return parent_element->computed_css_values()->property(property_id);
}

void StyleComputer::compute_defaulted_property_value(StyleProperties& style, DOM::Element const* element, CSS::PropertyID property_id, Optional<CSS::Selector::PseudoElement::Type> pseudo_element) const
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
void StyleComputer::compute_defaulted_values(StyleProperties& style, DOM::Element const* element, Optional<CSS::Selector::PseudoElement::Type> pseudo_element) const
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

    auto font_pixel_metrics = style.first_available_computed_font().pixel_metrics();
    Length::FontMetrics font_metrics { m_default_font_metrics.font_size, font_pixel_metrics };
    font_metrics.font_size = root_value->as_length().length().to_px(viewport_rect(), font_metrics, font_metrics);
    font_metrics.line_height = style.compute_line_height(viewport_rect(), font_metrics, font_metrics);

    return font_metrics;
}

RefPtr<Gfx::FontCascadeList const> StyleComputer::find_matching_font_weight_ascending(Vector<MatchingFontCandidate> const& candidates, int target_weight, float font_size_in_pt, bool inclusive)
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

RefPtr<Gfx::FontCascadeList const> StyleComputer::find_matching_font_weight_descending(Vector<MatchingFontCandidate> const& candidates, int target_weight, float font_size_in_pt, bool inclusive)
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
RefPtr<Gfx::FontCascadeList const> StyleComputer::font_matching_algorithm(FontFaceKey const& key, float font_size_in_pt) const
{
    // If a font family match occurs, the user agent assembles the set of font faces in that family and then
    // narrows the set to a single face using other font properties in the order given below.
    Vector<MatchingFontCandidate> matching_family_fonts;
    for (auto const& font_key_and_loader : m_loaded_fonts) {
        if (font_key_and_loader.key.family_name.equals_ignoring_ascii_case(key.family_name))
            matching_family_fonts.empend(font_key_and_loader.key, const_cast<FontLoaderList*>(&font_key_and_loader.value));
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

RefPtr<Gfx::FontCascadeList const> StyleComputer::compute_font_for_style_values(DOM::Element const* element, Optional<CSS::Selector::PseudoElement::Type> pseudo_element, StyleValue const& font_family, StyleValue const& font_size, StyleValue const& font_style, StyleValue const& font_weight, StyleValue const& font_stretch, int math_depth) const
{
    auto* parent_element = element_to_inherit_style_from(element, pseudo_element);

    auto width = font_stretch.to_font_stretch_width();

    auto weight = font_weight.to_font_weight();
    bool bold = weight > Gfx::FontWeight::Regular;

    // FIXME: Should be based on "user's default font size"
    CSSPixels font_size_in_px = 16;

    Gfx::FontPixelMetrics font_pixel_metrics;
    if (parent_element && parent_element->computed_css_values())
        font_pixel_metrics = parent_element->computed_css_values()->first_available_computed_font().pixel_metrics();
    else
        font_pixel_metrics = Platform::FontPlugin::the().default_font().pixel_metrics();
    auto parent_font_size = [&]() -> CSSPixels {
        if (!parent_element || !parent_element->computed_css_values())
            return font_size_in_px;
        auto value = parent_element->computed_css_values()->property(CSS::PropertyID::FontSize);
        if (value->is_length()) {
            auto length = value->as_length().length();
            if (length.is_absolute() || length.is_relative()) {
                Length::FontMetrics font_metrics { font_size_in_px, font_pixel_metrics };
                return length.to_px(viewport_rect(), font_metrics, m_root_element_font_metrics);
            }
        }
        return font_size_in_px;
    };
    Length::FontMetrics font_metrics { parent_font_size(), font_pixel_metrics };

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
                    font_size_in_px = CSSPixels::nearest_value_for(parent_element->computed_css_values()->first_available_computed_font().pixel_metrics().size);
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
            if (font_size.as_calculated().contains_percentage()) {
                maybe_length = font_size.as_calculated().resolve_length_percentage(length_resolution_context, Length::make_px(parent_font_size()));
            } else {
                maybe_length = font_size.as_calculated().resolve_length(length_resolution_context);
            }
        }
        if (maybe_length.has_value()) {
            font_size_in_px = maybe_length.value().to_px(length_resolution_context);
        }
    }

    auto slope = font_style.to_font_slope();

    // FIXME: Implement the full font-matching algorithm: https://www.w3.org/TR/css-fonts-4/#font-matching-algorithm

    // Note: This is modified by the find_font() lambda
    bool monospace = false;

    float const font_size_in_pt = font_size_in_px * 0.75f;

    auto find_font = [&](FlyString const& family) -> RefPtr<Gfx::FontCascadeList const> {
        FontFaceKey key {
            .family_name = family,
            .weight = weight,
            .slope = slope,
        };

        auto result = Gfx::FontCascadeList::create();
        if (auto it = m_loaded_fonts.find(key); it != m_loaded_fonts.end()) {
            auto const& loaders = it->value;
            for (auto const& loader : loaders) {
                if (auto found_font = loader->font_with_point_size(font_size_in_pt))
                    result->add(*found_font, loader->unicode_ranges());
            }
            return result;
        }

        if (auto found_font = font_matching_algorithm(key, font_size_in_pt); found_font && !found_font->is_empty()) {
            return found_font;
        }

        if (auto found_font = Gfx::FontDatabase::the().get(family, font_size_in_pt, weight, width, slope, Gfx::Font::AllowInexactSizeMatch::Yes)) {
            result->add(*found_font);
            return result;
        }

        return {};
    };

    auto find_generic_font = [&](ValueID font_id) -> RefPtr<Gfx::FontCascadeList const> {
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

    auto font_list = Gfx::FontCascadeList::create();
    if (font_family.is_value_list()) {
        auto const& family_list = static_cast<StyleValueList const&>(font_family).values();
        for (auto const& family : family_list) {
            RefPtr<Gfx::FontCascadeList const> other_font_list;
            if (family->is_identifier()) {
                other_font_list = find_generic_font(family->to_identifier());
            } else if (family->is_string()) {
                other_font_list = find_font(family->as_string().string_value());
            } else if (family->is_custom_ident()) {
                other_font_list = find_font(family->as_custom_ident().custom_ident());
            }
            if (other_font_list)
                font_list->extend(*other_font_list);
        }
    } else if (font_family.is_identifier()) {
        if (auto other_font_list = find_generic_font(font_family.to_identifier()))
            font_list->extend(*other_font_list);
    } else if (font_family.is_string()) {
        if (auto other_font_list = find_font(font_family.as_string().string_value()))
            font_list->extend(*other_font_list);
    } else if (font_family.is_custom_ident()) {
        if (auto other_font_list = find_font(font_family.as_custom_ident().custom_ident()))
            font_list->extend(*other_font_list);
    }

    auto found_font = StyleProperties::font_fallback(monospace, bold);
    if (auto scaled_fallback_font = found_font->with_size(font_size_in_pt)) {
        font_list->add(*scaled_fallback_font);
    } else {
        font_list->add(*found_font);
    }

    return font_list;
}

void StyleComputer::compute_font(StyleProperties& style, DOM::Element const* element, Optional<CSS::Selector::PseudoElement::Type> pseudo_element) const
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

    auto font_list = compute_font_for_style_values(element, pseudo_element, font_family, font_size, font_style, font_weight, font_stretch, style.math_depth());
    VERIFY(font_list);
    VERIFY(!font_list->is_empty());

    RefPtr<Gfx::Font const> const found_font = font_list->first();

    style.set_property(CSS::PropertyID::FontSize, LengthStyleValue::create(CSS::Length::make_px(CSSPixels::nearest_value_for(found_font->pixel_size()))), nullptr);
    style.set_property(CSS::PropertyID::FontWeight, NumberStyleValue::create(font_weight->to_font_weight()));

    style.set_computed_font_list(*font_list);

    if (element && is<HTML::HTMLHtmlElement>(*element)) {
        const_cast<StyleComputer&>(*this).m_root_element_font_metrics = calculate_root_element_font_metrics(style);
    }
}

Gfx::Font const& StyleComputer::initial_font() const
{
    // FIXME: This is not correct.
    return StyleProperties::font_fallback(false, false);
}

void StyleComputer::absolutize_values(StyleProperties& style) const
{
    Length::FontMetrics font_metrics {
        m_root_element_font_metrics.font_size,
        style.first_available_computed_font().pixel_metrics()
    };

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

    auto line_height = style.compute_line_height(viewport_rect(), font_metrics, m_root_element_font_metrics);
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

    style.set_line_height({}, line_height);
}

void StyleComputer::resolve_effective_overflow_values(StyleProperties& style) const
{
    // https://www.w3.org/TR/css-overflow-3/#overflow-control
    // The visible/clip values of overflow compute to auto/hidden (respectively) if one of overflow-x or
    // overflow-y is neither visible nor clip.
    auto overflow_x = value_id_to_overflow(style.property(PropertyID::OverflowX)->to_identifier());
    auto overflow_y = value_id_to_overflow(style.property(PropertyID::OverflowY)->to_identifier());
    auto overflow_x_is_visible_or_clip = overflow_x == Overflow::Visible || overflow_x == Overflow::Clip;
    auto overflow_y_is_visible_or_clip = overflow_y == Overflow::Visible || overflow_y == Overflow::Clip;
    if (!overflow_x_is_visible_or_clip || !overflow_y_is_visible_or_clip) {
        if (overflow_x == CSS::Overflow::Visible)
            style.set_property(CSS::PropertyID::OverflowX, IdentifierStyleValue::create(CSS::ValueID::Auto), nullptr);
        if (overflow_x == CSS::Overflow::Clip)
            style.set_property(CSS::PropertyID::OverflowX, IdentifierStyleValue::create(CSS::ValueID::Hidden), nullptr);
        if (overflow_y == CSS::Overflow::Visible)
            style.set_property(CSS::PropertyID::OverflowY, IdentifierStyleValue::create(CSS::ValueID::Auto), nullptr);
        if (overflow_y == CSS::Overflow::Clip)
            style.set_property(CSS::PropertyID::OverflowY, IdentifierStyleValue::create(CSS::ValueID::Hidden), nullptr);
    }
}

enum class BoxTypeTransformation {
    None,
    Blockify,
    Inlinify,
};

static BoxTypeTransformation required_box_type_transformation(StyleProperties const& style, DOM::Element const& element, Optional<CSS::Selector::PseudoElement::Type> const& pseudo_element)
{
    // NOTE: We never blockify <br> elements. They are always inline.
    //       There is currently no way to express in CSS how a <br> element really behaves.
    //       Spec issue: https://github.com/whatwg/html/issues/2291
    if (is<HTML::HTMLBRElement>(element))
        return BoxTypeTransformation::None;

    // Absolute positioning or floating an element blockifies the box’s display type. [CSS2]
    if (style.position() == CSS::Positioning::Absolute || style.position() == CSS::Positioning::Fixed || style.float_() != CSS::Float::None)
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
void StyleComputer::transform_box_type_if_needed(StyleProperties& style, DOM::Element const& element, Optional<CSS::Selector::PseudoElement::Type> pseudo_element) const
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
        if (element.namespace_uri() != Namespace::MathML)
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
    absolutize_values(style);
    style->set_property(CSS::PropertyID::Width, CSS::LengthStyleValue::create(CSS::Length::make_px(viewport_rect().width())), nullptr);
    style->set_property(CSS::PropertyID::Height, CSS::LengthStyleValue::create(CSS::Length::make_px(viewport_rect().height())), nullptr);
    style->set_property(CSS::PropertyID::Display, CSS::DisplayStyleValue::create(CSS::Display::from_short(CSS::Display::Short::Block)), nullptr);
    return style;
}

ErrorOr<NonnullRefPtr<StyleProperties>> StyleComputer::compute_style(DOM::Element& element, Optional<CSS::Selector::PseudoElement::Type> pseudo_element) const
{
    auto style = TRY(compute_style_impl(element, move(pseudo_element), ComputeStyleMode::Normal));
    return style.release_nonnull();
}

ErrorOr<RefPtr<StyleProperties>> StyleComputer::compute_pseudo_element_style_if_needed(DOM::Element& element, Optional<CSS::Selector::PseudoElement::Type> pseudo_element) const
{
    return compute_style_impl(element, move(pseudo_element), ComputeStyleMode::CreatePseudoElementStyleIfNeeded);
}

ErrorOr<RefPtr<StyleProperties>> StyleComputer::compute_style_impl(DOM::Element& element, Optional<CSS::Selector::PseudoElement::Type> pseudo_element, ComputeStyleMode mode) const
{
    build_rule_cache_if_needed();

    // Special path for elements that use pseudo element as style selector
    if (element.use_pseudo_element().has_value()) {
        auto& parent_element = verify_cast<HTML::HTMLElement>(*element.root().parent_or_shadow_host());
        auto style = TRY(compute_style(parent_element, *element.use_pseudo_element()));

        // Merge back inline styles
        if (element.has_attribute(HTML::AttributeNames::style)) {
            auto* inline_style = parse_css_style_attribute(CSS::Parser::ParsingContext(document()), *element.get_attribute(HTML::AttributeNames::style), element);
            for (auto const& property : inline_style->properties())
                style->set_property(property.property_id, property.value);
        }
        return style;
    }

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
    absolutize_values(style);

    // 5. Default the values, applying inheritance and 'initial' as needed
    compute_defaulted_values(style, &element, pseudo_element);

    // 6. Run automatic box type transformations
    transform_box_type_if_needed(style, element, pseudo_element);

    // 7. Resolve effective overflow values
    resolve_effective_overflow_values(style);

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

        // Loosely based on https://drafts.csswg.org/css-animations-2/#keyframe-processing
        sheet.for_each_effective_keyframes_at_rule([&](CSSKeyframesRule const& rule) {
            auto keyframe_set = adopt_ref(*new Animations::KeyframeEffect::KeyFrameSet);
            HashTable<PropertyID> animated_properties;

            // Forwards pass, resolve all the user-specified keyframe properties.
            for (auto const& keyframe : rule.keyframes()) {
                Animations::KeyframeEffect::KeyFrameSet::ResolvedKeyFrame resolved_keyframe;

                auto key = static_cast<u64>(keyframe->key().value() * Animations::KeyframeEffect::AnimationKeyFrameKeyScaleFactor);
                auto keyframe_rule = keyframe->style();

                if (!is<PropertyOwningCSSStyleDeclaration>(*keyframe_rule))
                    continue;

                auto const& keyframe_style = static_cast<PropertyOwningCSSStyleDeclaration const&>(*keyframe_rule);
                for (auto const& property : keyframe_style.properties()) {
                    animated_properties.set(property.property_id);
                    resolved_keyframe.resolved_properties.set(property.property_id, property.value);
                }

                keyframe_set->keyframes_by_key.insert(key, resolved_keyframe);
            }

            Animations::KeyframeEffect::generate_initial_and_final_frames(keyframe_set, animated_properties);

            if constexpr (LIBWEB_CSS_DEBUG) {
                dbgln("Resolved keyframe set '{}' into {} keyframes:", rule.name(), keyframe_set->keyframes_by_key.size());
                for (auto it = keyframe_set->keyframes_by_key.begin(); it != keyframe_set->keyframes_by_key.end(); ++it)
                    dbgln("    - keyframe {}: {} properties", it.key(), it->resolved_properties.size());
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
    if (auto user_style_source = document().page().user_style(); user_style_source.has_value()) {
        m_user_style_sheet = JS::make_handle(parse_css_stylesheet(CSS::Parser::ParsingContext(document()), user_style_source.value()));
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

void StyleComputer::did_load_font(FlyString const&)
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

        Vector<AK::URL> urls;
        for (auto& source : font_face.sources()) {
            // FIXME: These should be loaded relative to the stylesheet URL instead of the document URL.
            if (source.local_or_url.has<AK::URL>())
                urls.append(m_document->parse_url(MUST(source.local_or_url.get<AK::URL>().to_string())));
            // FIXME: Handle local()
        }

        if (urls.is_empty())
            continue;

        auto loader = make<FontLoader>(const_cast<StyleComputer&>(*this), font_face.font_family(), font_face.unicode_ranges(), move(urls));
        auto maybe_font_loaders_list = const_cast<StyleComputer&>(*this).m_loaded_fonts.get(key);
        if (maybe_font_loaders_list.has_value()) {
            maybe_font_loaders_list->append(move(loader));
        } else {
            FontLoaderList loaders;
            loaders.append(move(loader));
            const_cast<StyleComputer&>(*this).m_loaded_fonts.set(key, move(loaders));
        }
    }
}

void StyleComputer::compute_math_depth(StyleProperties& style, DOM::Element const* element, Optional<CSS::Selector::PseudoElement::Type> pseudo_element) const
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
