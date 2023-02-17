/*
 * Copyright (c) 2023, Kenneth Myhra <kennethmyhra@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/XHR/FormData.h>

namespace Web::HTML {

WebIDL::ExceptionOr<XHR::FormDataEntry> create_entry(JS::Realm& realm, String const& name, Variant<JS::NonnullGCPtr<FileAPI::Blob>, String> const& value, Optional<String> const& filename = {});
WebIDL::ExceptionOr<Optional<Vector<XHR::FormDataEntry>>> construct_entry_list(JS::Realm&, HTMLFormElement&);

}
