/*
 * Copyright (c) 2023, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/HTML/MimeTypeArray.h>
#include <LibWeb/HTML/Scripting/Environments.h>
#include <LibWeb/HTML/Window.h>
#include <LibWeb/Page/Page.h>

namespace Web::HTML {

MimeTypeArray::MimeTypeArray(JS::Realm& realm)
    : Bindings::LegacyPlatformObject(realm)
{
}

MimeTypeArray::~MimeTypeArray() = default;

void MimeTypeArray::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    set_prototype(&Bindings::ensure_web_prototype<Bindings::MimeTypeArrayPrototype>(realm, "MimeTypeArray"));
}

// https://html.spec.whatwg.org/multipage/system-state.html#pdf-viewing-support:support-named-properties-2
Vector<DeprecatedString> MimeTypeArray::supported_property_names() const
{
    // The MimeTypeArray interface supports named properties. If the user agent's PDF viewer supported is true, then they are the PDF viewer mime types. Otherwise, they are the empty list.
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

// https://html.spec.whatwg.org/multipage/system-state.html#pdf-viewing-support:supports-indexed-properties-2
bool MimeTypeArray::is_supported_property_index(u32 index) const
{
    // The MimeTypeArray interface supports indexed properties. The supported property indices are the indices of this's relevant global object's PDF viewer mime type objects.
    auto& window = verify_cast<HTML::Window>(HTML::relevant_global_object(*this));
    return index < window.pdf_viewer_mime_type_objects().size();
}

// https://html.spec.whatwg.org/multipage/system-state.html#dom-mimetypearray-length
size_t MimeTypeArray::length() const
{
    // The MimeTypeArray interface's length getter steps are to return this's relevant global object's PDF viewer mime type objects's size.
    auto& window = verify_cast<HTML::Window>(HTML::relevant_global_object(*this));
    return window.pdf_viewer_mime_type_objects().size();
}

// https://html.spec.whatwg.org/multipage/system-state.html#dom-mimetypearray-item
JS::GCPtr<MimeType> MimeTypeArray::item(u32 index) const
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

// https://html.spec.whatwg.org/multipage/system-state.html#dom-mimetypearray-nameditem
JS::GCPtr<MimeType> MimeTypeArray::named_item(FlyString const& name) const
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

WebIDL::ExceptionOr<JS::Value> MimeTypeArray::item_value(size_t index) const
{
    auto return_value = item(index);
    if (!return_value)
        return JS::js_null();
    return return_value.ptr();
}

WebIDL::ExceptionOr<JS::Value> MimeTypeArray::named_item_value(FlyString const& name) const
{
    auto return_value = named_item(name);
    if (!return_value)
        return JS::js_null();
    return return_value.ptr();
}

}
