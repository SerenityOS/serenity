/*
 * Copyright (c) 2023, Kenneth Myhra <kennethmyhra@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/XHR/FormData.h>

namespace Web::HTML {

using HashMapWithVectorOfFormDataEntryValue = HashMap<DeprecatedString, Vector<XHR::FormDataEntryValue>>;

struct Entry {
    String name;
    Variant<JS::NonnullGCPtr<FileAPI::File>, String> value;
};

WebIDL::ExceptionOr<Entry> create_entry(JS::Realm& realm, String const& name, Variant<JS::NonnullGCPtr<FileAPI::Blob>, String> const& value, Optional<String> const& filename = {});
WebIDL::ExceptionOr<Optional<HashMapWithVectorOfFormDataEntryValue>> construct_entry_list(JS::Realm&, HTMLFormElement&);

}
