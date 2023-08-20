/*
 * Copyright (c) 2023, Shannon Booth <shannon@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/DOM/HTMLCollection.h>
#include <LibWeb/DOM/RadioNodeList.h>

namespace Web::DOM {

class HTMLFormControlsCollection : public HTMLCollection {
    WEB_PLATFORM_OBJECT(HTMLFormControlsCollection, HTMLCollection);

public:
    [[nodiscard]] static JS::NonnullGCPtr<HTMLFormControlsCollection> create(ParentNode& root, Scope, Function<bool(Element const&)> filter);

    virtual ~HTMLFormControlsCollection() override;

    Variant<Empty, Element*, JS::Handle<RadioNodeList>> named_item_or_radio_node_list(FlyString const& name);

protected:
    virtual void initialize(JS::Realm&) override;

private:
    HTMLFormControlsCollection(ParentNode& root, Scope, Function<bool(Element const&)> filter);
};

}
