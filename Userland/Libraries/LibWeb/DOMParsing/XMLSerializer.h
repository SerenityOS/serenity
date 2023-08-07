/*
 * Copyright (c) 2022, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/Bindings/PlatformObject.h>

namespace Web::DOMParsing {

class XMLSerializer final : public Bindings::PlatformObject {
    WEB_PLATFORM_OBJECT(XMLSerializer, Bindings::PlatformObject);

public:
    static WebIDL::ExceptionOr<JS::NonnullGCPtr<XMLSerializer>> construct_impl(JS::Realm&);

    virtual ~XMLSerializer() override;

    WebIDL::ExceptionOr<DeprecatedString> serialize_to_string(JS::NonnullGCPtr<DOM::Node const> root);

private:
    explicit XMLSerializer(JS::Realm&);

    virtual void initialize(JS::Realm&) override;
};

enum class RequireWellFormed {
    No,
    Yes,
};

WebIDL::ExceptionOr<DeprecatedString> serialize_node_to_xml_string(JS::NonnullGCPtr<DOM::Node const> root, RequireWellFormed require_well_formed);
}
