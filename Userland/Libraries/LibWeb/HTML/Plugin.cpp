/*
 * Copyright (c) 2023, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/HTML/Plugin.h>
#include <LibWeb/HTML/Scripting/Environments.h>
#include <LibWeb/HTML/Window.h>
#include <LibWeb/Page/Page.h>

namespace Web::HTML {

Plugin::Plugin(JS::Realm& realm, String name)
    : Bindings::LegacyPlatformObject(realm)
    , m_name(move(name))
{
}

Plugin::~Plugin() = default;

void Plugin::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    set_prototype(&Bindings::ensure_web_prototype<Bindings::PluginPrototype>(realm, "Plugin"));
}

// https://html.spec.whatwg.org/multipage/system-state.html#dom-plugin-name
String const& Plugin::name() const
{
    // The Plugin interface's name getter steps are to return this's name.
    return m_name;
}

// https://html.spec.whatwg.org/multipage/system-state.html#dom-plugin-description
String Plugin::description() const
{
    // The Plugin interface's description getter steps are to return "Portable Document Format".
    static String description_string = "Portable Document Format"_string;
    return description_string;
}

// https://html.spec.whatwg.org/multipage/system-state.html#dom-plugin-filename
String Plugin::filename() const
{
    // The Plugin interface's filename getter steps are to return "internal-pdf-viewer".
    static String filename_string = "internal-pdf-viewer"_string;
    return filename_string;
}

// https://html.spec.whatwg.org/multipage/system-state.html#pdf-viewing-support:support-named-properties-3
Vector<DeprecatedString> Plugin::supported_property_names() const
{
    // The Plugin interface supports named properties. If the user agent's PDF viewer supported is true, then they are the PDF viewer mime types. Otherwise, they are the empty list.
    auto const& window = verify_cast<HTML::Window>(HTML::relevant_global_object(*this));
    VERIFY(window.page());
    if (!window.page()->pdf_viewer_supported())
        return {};

    // https://html.spec.whatwg.org/multipage/system-state.html#pdf-viewer-mime-types
    static Vector<DeprecatedString> mime_types = {
        "application/pdf"sv,
        "text/pdf"sv,
    };

    return mime_types;
}

// https://html.spec.whatwg.org/multipage/system-state.html#pdf-viewing-support:supports-indexed-properties-3
bool Plugin::is_supported_property_index(u32 index) const
{
    // The Plugin interface supports indexed properties. The supported property indices are the indices of this's relevant global object's PDF viewer mime type objects.
    auto& window = verify_cast<HTML::Window>(HTML::relevant_global_object(*this));
    return index < window.pdf_viewer_mime_type_objects().size();
}

// https://html.spec.whatwg.org/multipage/system-state.html#dom-plugin-length
size_t Plugin::length() const
{
    // The Plugin interface's length getter steps are to return this's relevant global object's PDF viewer mime type objects's size.
    auto& window = verify_cast<HTML::Window>(HTML::relevant_global_object(*this));
    return window.pdf_viewer_mime_type_objects().size();
}

// https://html.spec.whatwg.org/multipage/system-state.html#dom-plugin-item
JS::GCPtr<MimeType> Plugin::item(u32 index) const
{
    // 1. Let mimeTypes be this's relevant global object's PDF viewer mime type objects.
    auto& window = verify_cast<HTML::Window>(HTML::relevant_global_object(*this));
    auto mime_types = window.pdf_viewer_mime_type_objects();

    // 2. If index < mimeType's size, then return mimeTypes[index].
    if (index < mime_types.size())
        return mime_types[index];

    // 3. Return null.
    return nullptr;
}

JS::GCPtr<MimeType> Plugin::named_item(String const& name) const
{
    // 1. For each MimeType mimeType of this's relevant global object's PDF viewer mime type objects: if mimeType's type is name, then return mimeType.
    auto& window = verify_cast<HTML::Window>(HTML::relevant_global_object(*this));
    auto mime_types = window.pdf_viewer_mime_type_objects();

    for (auto& mime_type : mime_types) {
        if (mime_type->type() == name)
            return mime_type;
    }

    // 2. Return null.
    return nullptr;
}

WebIDL::ExceptionOr<JS::Value> Plugin::item_value(size_t index) const
{
    auto return_value = item(index);
    if (!return_value)
        return JS::js_null();
    return return_value.ptr();
}

WebIDL::ExceptionOr<JS::Value> Plugin::named_item_value(DeprecatedFlyString const& name) const
{
    auto converted_name = TRY_OR_THROW_OOM(vm(), String::from_deprecated_string(name));
    auto return_value = named_item(converted_name);
    if (!return_value)
        return JS::js_null();
    return return_value.ptr();
}

}
