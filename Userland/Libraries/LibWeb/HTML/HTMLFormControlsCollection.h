/*
 * Copyright (c) 2022, Johan Dahlin <jdahlin@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Heap/Handle.h>
#include <LibWeb/DOM/Element.h>
#include <LibWeb/DOM/HTMLCollection.h>
#include <LibWeb/DOM/ParentNode.h>
#include <LibWeb/HTML/RadioNodeList.h>

namespace Web::HTML {

class HTMLFormControlsCollection final : public DOM::HTMLCollection {
    WEB_PLATFORM_OBJECT(HTMLFormControlsCollection, DOM::HTMLCollection);

public:
    virtual ~HTMLFormControlsCollection() override;

    static JS::NonnullGCPtr<HTMLFormControlsCollection> create(DOM::ParentNode& root, Function<bool(DOM::Element const&)> filter);

    Variant<JS::Handle<RadioNodeList>, JS::Handle<DOM::Element>, Empty> named_item(FlyString const& name);

private:
    HTMLFormControlsCollection(DOM::ParentNode& root, Function<bool(DOM::Element const&)> filter);
};

}
