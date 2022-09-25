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
    static JS::NonnullGCPtr<XMLSerializer> create_with_global_object(HTML::Window&);

    virtual ~XMLSerializer() override;

    WebIDL::ExceptionOr<String> serialize_to_string(JS::NonnullGCPtr<DOM::Node> root);

private:
    explicit XMLSerializer(HTML::Window&);
};

enum class RequireWellFormed {
    No,
    Yes,
};

WebIDL::ExceptionOr<String> serialize_node_to_xml_string(JS::NonnullGCPtr<DOM::Node> root, RequireWellFormed require_well_formed);

}
