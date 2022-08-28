/*
 * Copyright (c) 2022, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefCounted.h>
#include <LibWeb/Bindings/Wrappable.h>
#include <LibWeb/Forward.h>

namespace Web::DOMParsing {

class XMLSerializer final
    : public RefCounted<XMLSerializer>
    , public Bindings::Wrappable {
public:
    using WrapperType = Bindings::XMLSerializerWrapper;

    static NonnullRefPtr<XMLSerializer> create_with_global_object(HTML::Window&)
    {
        return adopt_ref(*new XMLSerializer());
    }

    virtual ~XMLSerializer() override;

    DOM::ExceptionOr<String> serialize_to_string(JS::NonnullGCPtr<DOM::Node> root);

private:
    XMLSerializer();
};

enum class RequireWellFormed {
    No,
    Yes,
};

DOM::ExceptionOr<String> serialize_node_to_xml_string(JS::NonnullGCPtr<DOM::Node> root, RequireWellFormed require_well_formed);

}
