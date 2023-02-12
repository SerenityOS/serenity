/*
 * Copyright (c) 2023, Kenneth Myhra <kennethmyhra@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/Bindings/FormDataPrototype.h>
#include <LibWeb/Bindings/PlatformObject.h>
#include <LibWeb/Forward.h>
#include <LibWeb/HTML/HTMLFormElement.h>
#include <LibWeb/WebIDL/ExceptionOr.h>

namespace Web::XHR {

// https://xhr.spec.whatwg.org/#formdataentryvalue
using FormDataEntryValue = Variant<JS::NonnullGCPtr<FileAPI::File>, DeprecatedString>;

// https://xhr.spec.whatwg.org/#interface-formdata
class FormData : public Bindings::PlatformObject {
    WEB_PLATFORM_OBJECT(FormData, Bindings::PlatformObject);

public:
    virtual ~FormData() override;

    static WebIDL::ExceptionOr<JS::NonnullGCPtr<FormData>> construct_impl(JS::Realm&, Optional<JS::NonnullGCPtr<HTML::HTMLFormElement>> form = {});
    static WebIDL::ExceptionOr<JS::NonnullGCPtr<FormData>> construct_impl(JS::Realm&, HashMap<DeprecatedString, Vector<FormDataEntryValue>> entry_list);

    WebIDL::ExceptionOr<void> append(DeprecatedString const& name, DeprecatedString const& value);
    WebIDL::ExceptionOr<void> append(DeprecatedString const& name, JS::NonnullGCPtr<FileAPI::Blob> const& blob_value, Optional<DeprecatedString> const& filename = {});
    void delete_(DeprecatedString const& name);
    Variant<JS::NonnullGCPtr<FileAPI::File>, DeprecatedString, Empty> get(DeprecatedString const& name);
    Vector<FormDataEntryValue> get_all(DeprecatedString const& name);
    bool has(DeprecatedString const& name);
    WebIDL::ExceptionOr<void> set(DeprecatedString const& name, DeprecatedString const& value);
    WebIDL::ExceptionOr<void> set(DeprecatedString const& name, JS::NonnullGCPtr<FileAPI::Blob> const& blob_value, Optional<DeprecatedString> const& filename = {});

private:
    explicit FormData(JS::Realm&, HashMap<DeprecatedString, Vector<FormDataEntryValue>> entry_list = {});

    virtual JS::ThrowCompletionOr<void> initialize(JS::Realm&) override;
    virtual void visit_edges(Cell::Visitor&) override;

    WebIDL::ExceptionOr<void> append_impl(String const& name, Variant<JS::NonnullGCPtr<FileAPI::Blob>, String> const& value, Optional<String> const& filename = {});
    WebIDL::ExceptionOr<void> set_impl(String const& name, Variant<JS::NonnullGCPtr<FileAPI::Blob>, String> const& value, Optional<String> const& filename = {});

    HashMap<DeprecatedString, Vector<FormDataEntryValue>> m_entry_list;
};

}
