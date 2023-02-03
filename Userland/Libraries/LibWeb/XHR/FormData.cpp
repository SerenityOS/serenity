/*
 * Copyright (c) 2023, Kenneth Myhra <kennethmyhra@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/TypeCasts.h>
#include <LibJS/Runtime/Completion.h>
#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/FileAPI/Blob.h>
#include <LibWeb/FileAPI/File.h>
#include <LibWeb/HTML/FormControlInfrastructure.h>
#include <LibWeb/WebIDL/DOMException.h>
#include <LibWeb/XHR/FormData.h>

namespace Web::XHR {

// https://xhr.spec.whatwg.org/#dom-formdata
WebIDL::ExceptionOr<JS::NonnullGCPtr<FormData>> FormData::construct_impl(JS::Realm& realm, Optional<JS::NonnullGCPtr<HTML::HTMLFormElement>>)
{
    // FIXME: 1. If form is given, then:
    // FIXME:    1. Let list be the result of constructing the entry list for form.
    // FIXME:    2. If list is null, then throw an "InvalidStateError" DOMException.
    // FIXME:    3. Set this’s entry list to list.

    return construct_impl(realm);
}

WebIDL::ExceptionOr<JS::NonnullGCPtr<FormData>> FormData::construct_impl(JS::Realm& realm, HashMap<DeprecatedString, Vector<FormDataEntryValue>> entry_list)
{
    return MUST_OR_THROW_OOM(realm.heap().allocate<FormData>(realm, realm, move(entry_list)));
}

FormData::FormData(JS::Realm& realm, HashMap<DeprecatedString, Vector<FormDataEntryValue>> entry_list)
    : PlatformObject(realm)
    , m_entry_list(move(entry_list))
{
    set_prototype(&Bindings::ensure_web_prototype<Bindings::FormDataPrototype>(realm, "FormData"));
}

FormData::~FormData() = default;

void FormData::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    for (auto const& entry : m_entry_list) {
        for (auto const& value : entry.value) {
            if (auto* file = value.get_pointer<JS::NonnullGCPtr<FileAPI::File>>())
                visitor.visit(*file);
        }
    }
}

// https://xhr.spec.whatwg.org/#dom-formdata-append
WebIDL::ExceptionOr<void> FormData::append(DeprecatedString const& name, DeprecatedString const& value)
{
    auto& vm = realm().vm();
    return append_impl(TRY_OR_THROW_OOM(vm, String::from_deprecated_string(name)), TRY_OR_THROW_OOM(vm, String::from_deprecated_string(value)));
}

// https://xhr.spec.whatwg.org/#dom-formdata-append-blob
WebIDL::ExceptionOr<void> FormData::append(DeprecatedString const& name, JS::NonnullGCPtr<FileAPI::Blob> const& blob_value, Optional<DeprecatedString> const& filename)
{
    auto& vm = realm().vm();
    auto inner_filename = filename.has_value() ? TRY_OR_THROW_OOM(vm, String::from_deprecated_string(filename.value())) : Optional<String> {};
    return append_impl(TRY_OR_THROW_OOM(vm, String::from_deprecated_string(name)), blob_value, inner_filename);
}

// https://xhr.spec.whatwg.org/#dom-formdata-append
// https://xhr.spec.whatwg.org/#dom-formdata-append-blob
WebIDL::ExceptionOr<void> FormData::append_impl(String const& name, Variant<JS::NonnullGCPtr<FileAPI::Blob>, String> const& value, Optional<String> const& filename)
{
    auto& realm = this->realm();
    auto& vm = realm.vm();

    // 1. Let value be value if given; otherwise blobValue.
    // 2. Let entry be the result of creating an entry with name, value, and filename if given.
    auto entry = TRY(HTML::create_entry(realm, name, value, filename));

    // FIXME: Remove this when our binding generator supports "new string".
    auto form_data_entry_value = entry.value.has<String>()
        ? FormDataEntryValue { entry.value.get<String>().to_deprecated_string() }
        : FormDataEntryValue { entry.value.get<JS::NonnullGCPtr<FileAPI::File>>() };

    // 3. Append entry to this’s entry list.
    if (auto entries = m_entry_list.get(entry.name.to_deprecated_string()); entries.has_value() && !entries->is_empty())
        TRY_OR_THROW_OOM(vm, entries->try_append(form_data_entry_value));
    else
        TRY_OR_THROW_OOM(vm, m_entry_list.try_set(entry.name.to_deprecated_string(), { form_data_entry_value }));

    return {};
}

// https://xhr.spec.whatwg.org/#dom-formdata-delete
void FormData::delete_(DeprecatedString const& name)
{
    // The delete(name) method steps are to remove all entries whose name is name from this’s entry list.
    m_entry_list.remove(name);
}

// https://xhr.spec.whatwg.org/#dom-formdata-get
Variant<JS::NonnullGCPtr<FileAPI::File>, DeprecatedString, Empty> FormData::get(DeprecatedString const& name)
{
    // 1. If there is no entry whose name is name in this’s entry list, then return null.
    if (!m_entry_list.contains(name))
        return Empty {};
    // 2. Return the value of the first entry whose name is name from this’s entry list.
    return m_entry_list.get(name)->at(0);
}

// https://xhr.spec.whatwg.org/#dom-formdata-getall
Vector<FormDataEntryValue> FormData::get_all(DeprecatedString const& name)
{
    // 1. If there is no entry whose name is name in this’s entry list, then return the empty list.
    if (!m_entry_list.contains(name))
        return {};
    // 2. Return the values of all entries whose name is name, in order, from this’s entry list.
    return *m_entry_list.get(name);
}

// https://xhr.spec.whatwg.org/#dom-formdata-has
bool FormData::has(DeprecatedString const& name)
{
    // The has(name) method steps are to return true if there is an entry whose name is name in this’s entry list; otherwise false.
    return m_entry_list.contains(name);
}

// https://xhr.spec.whatwg.org/#dom-formdata-set
WebIDL::ExceptionOr<void> FormData::set(DeprecatedString const& name, DeprecatedString const& value)
{
    auto& vm = realm().vm();
    return set_impl(TRY_OR_THROW_OOM(vm, String::from_deprecated_string(name)), TRY_OR_THROW_OOM(vm, String::from_deprecated_string(value)));
}

// https://xhr.spec.whatwg.org/#dom-formdata-set-blob
WebIDL::ExceptionOr<void> FormData::set(DeprecatedString const& name, JS::NonnullGCPtr<FileAPI::Blob> const& blob_value, Optional<DeprecatedString> const& filename)
{
    auto& vm = realm().vm();
    auto inner_filename = filename.has_value() ? TRY_OR_THROW_OOM(vm, String::from_deprecated_string(filename.value())) : Optional<String> {};
    return set_impl(TRY_OR_THROW_OOM(vm, String::from_deprecated_string(name)), blob_value, inner_filename);
}

// https://xhr.spec.whatwg.org/#dom-formdata-set
// https://xhr.spec.whatwg.org/#dom-formdata-set-blob
WebIDL::ExceptionOr<void> FormData::set_impl(String const& name, Variant<JS::NonnullGCPtr<FileAPI::Blob>, String> const& value, Optional<String> const& filename)
{
    auto& realm = this->realm();
    auto& vm = realm.vm();

    // 1. Let value be value if given; otherwise blobValue.
    // 2. Let entry be the result of creating an entry with name, value, and filename if given.
    auto entry = TRY(HTML::create_entry(realm, name, value, filename));

    // FIXME: Remove this when our binding generator supports "new string".
    auto form_data_entry_value = entry.value.has<String>()
        ? FormDataEntryValue { entry.value.get<String>().to_deprecated_string() }
        : FormDataEntryValue { entry.value.get<JS::NonnullGCPtr<FileAPI::File>>() };

    // 3. If there are entries in this’s entry list whose name is name, then replace the first such entry with entry and remove the others.
    if (auto entries = m_entry_list.get(entry.name.to_deprecated_string()); entries.has_value() && !entries->is_empty()) {
        entries->remove(0, entries->size());
        TRY_OR_THROW_OOM(vm, entries->try_append(form_data_entry_value));
    }
    // 4. Otherwise, append entry to this’s entry list.
    else {
        TRY_OR_THROW_OOM(vm, m_entry_list.try_set(entry.name.to_deprecated_string(), { form_data_entry_value }));
    }

    return {};
}

}
