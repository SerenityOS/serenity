/*
 * Copyright (c) 2024, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Heap/Heap.h>
#include <LibJS/Runtime/Realm.h>
#include <LibWeb/Bindings/FontFacePrototype.h>
#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/CSS/FontFace.h>
#include <LibWeb/CSS/Parser/Parser.h>
#include <LibWeb/WebIDL/Promise.h>

namespace Web::CSS {

JS_DEFINE_ALLOCATOR(FontFace);

template<CSS::PropertyID PropertyID>
RefPtr<CSS::StyleValue const> parse_property_string(JS::Realm& realm, StringView value)
{
    auto maybe_parser = CSS::Parser::Parser::create(CSS::Parser::ParsingContext(realm), value);
    if (maybe_parser.is_error())
        return {};

    return maybe_parser.release_value().parse_as_css_value(PropertyID);
}

JS::NonnullGCPtr<FontFace> FontFace::construct_impl(JS::Realm& realm, String family, FontFaceSource source, FontFaceDescriptors const& descriptors)
{
    return realm.heap().allocate<FontFace>(realm, realm, move(family), move(source), descriptors);
}

FontFace::FontFace(JS::Realm& realm, String font_family, FontFaceSource, FontFaceDescriptors const& descriptors)
    : Bindings::PlatformObject(realm)
{
    // FIXME: Validate these values the same way as the setters
    m_family = move(font_family);
    m_style = descriptors.style;
    m_weight = descriptors.weight;
    m_stretch = descriptors.stretch;
    m_unicode_range = descriptors.unicode_range;
    m_feature_settings = descriptors.feature_settings;
    m_variation_settings = descriptors.variation_settings;
    m_display = descriptors.display;
    m_ascent_override = descriptors.ascent_override;
    m_descent_override = descriptors.descent_override;
    m_line_gap_override = descriptors.line_gap_override;
}

void FontFace::initialize(JS::Realm& realm)
{
    Base::initialize(realm);

    WEB_SET_PROTOTYPE_FOR_INTERFACE(FontFace);
}

// https://drafts.csswg.org/css-font-loading/#dom-fontface-family
WebIDL::ExceptionOr<void> FontFace::set_family(String const& string)
{
    auto property = parse_property_string<CSS::PropertyID::FontFamily>(realm(), string);
    if (!property)
        return WebIDL::SyntaxError::create(realm(), "FontFace.family setter: Invalid font descriptor"_fly_string);

    if (m_is_css_connected) {
        // FIXME: Propagate to the CSSFontFaceRule and update the font-family property
    }

    m_family = property->to_string();

    return {};
}

// https://drafts.csswg.org/css-font-loading/#dom-fontface-style
WebIDL::ExceptionOr<void> FontFace::set_style(String const& string)
{
    auto property = parse_property_string<CSS::PropertyID::FontStyle>(realm(), string);
    if (!property)
        return WebIDL::SyntaxError::create(realm(), "FontFace.style setter: Invalid font descriptor"_fly_string);

    if (m_is_css_connected) {
        // FIXME: Propagate to the CSSFontFaceRule and update the font-style property
    }

    m_style = property->to_string();

    return {};
}

// https://drafts.csswg.org/css-font-loading/#dom-fontface-weight
WebIDL::ExceptionOr<void> FontFace::set_weight(String const& string)
{
    auto property = parse_property_string<CSS::PropertyID::FontWeight>(realm(), string);
    if (!property)
        return WebIDL::SyntaxError::create(realm(), "FontFace.weight setter: Invalid font descriptor"_fly_string);

    if (m_is_css_connected) {
        // FIXME: Propagate to the CSSFontFaceRule and update the font-weight property
    }

    m_weight = property->to_string();

    return {};
}

// https://drafts.csswg.org/css-font-loading/#dom-fontface-stretch
WebIDL::ExceptionOr<void> FontFace::set_stretch(String const& string)
{
    auto property = parse_property_string<CSS::PropertyID::FontStretch>(realm(), string);
    if (!property)
        return WebIDL::SyntaxError::create(realm(), "FontFace.stretch setter: Invalid font descriptor"_fly_string);

    if (m_is_css_connected) {
        // FIXME: Propagate to the CSSFontFaceRule and update the font-stretch property
    }

    m_stretch = property->to_string();

    return {};
}

// https://drafts.csswg.org/css-font-loading/#dom-fontface-unicoderange
WebIDL::ExceptionOr<void> FontFace::set_unicode_range(String const&)
{
    // FIXME: This *should* work, but the <urange> production is hard to parse
    //        from just a value string in our implementation
    return WebIDL::NotSupportedError::create(realm(), "unicode range is not yet implemented"_fly_string);
}

// https://drafts.csswg.org/css-font-loading/#dom-fontface-featuresettings
WebIDL::ExceptionOr<void> FontFace::set_feature_settings(String const&)
{
    return WebIDL::NotSupportedError::create(realm(), "feature settings is not yet implemented"_fly_string);
}

// https://drafts.csswg.org/css-font-loading/#dom-fontface-variationsettings
WebIDL::ExceptionOr<void> FontFace::set_variation_settings(String const&)
{
    return WebIDL::NotSupportedError::create(realm(), "variation settings is not yet implemented"_fly_string);
}

// https://drafts.csswg.org/css-font-loading/#dom-fontface-display
WebIDL::ExceptionOr<void> FontFace::set_display(String const&)
{
    return WebIDL::NotSupportedError::create(realm(), "display is not yet implemented"_fly_string);
}

// https://drafts.csswg.org/css-font-loading/#dom-fontface-ascentoverride
WebIDL::ExceptionOr<void> FontFace::set_ascent_override(String const&)
{
    return WebIDL::NotSupportedError::create(realm(), "ascent override is not yet implemented"_fly_string);
}

// https://drafts.csswg.org/css-font-loading/#dom-fontface-descentoverride
WebIDL::ExceptionOr<void> FontFace::set_descent_override(String const&)
{
    return WebIDL::NotSupportedError::create(realm(), "descent override is not yet implemented"_fly_string);
}

// https://drafts.csswg.org/css-font-loading/#dom-fontface-linegapoverride
WebIDL::ExceptionOr<void> FontFace::set_line_gap_override(String const&)
{
    return WebIDL::NotSupportedError::create(realm(), "line gap override is not yet implemented"_fly_string);
}

// https://drafts.csswg.org/css-font-loading/#dom-fontface-load
JS::ThrowCompletionOr<JS::NonnullGCPtr<JS::Promise>> FontFace::load()
{
    // FIXME: Do the steps
    auto promise = WebIDL::create_rejected_promise(realm(), WebIDL::NotSupportedError::create(realm(), "FontFace::load is not yet implemented"_fly_string));
    return verify_cast<JS::Promise>(*promise->promise());
}

}
