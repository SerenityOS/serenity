/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <YAK/RefCounted.h>
#include <LibWeb/Bindings/Wrappable.h>
#include <LibWeb/Forward.h>

namespace Web::CSS {

class StyleSheet
    : public RefCounted<StyleSheet>
    , public Bindings::Wrappable {
public:
    using WrapperType = Bindings::StyleSheetWrapper;

    virtual ~StyleSheet() = default;

    virtual String type() const = 0;

    DOM::Element* owner_node() { return m_owner_node; }
    void set_owner_node(DOM::Element*);

protected:
    StyleSheet() = default;

private:
    WeakPtr<DOM::Element> m_owner_node;
};

}
