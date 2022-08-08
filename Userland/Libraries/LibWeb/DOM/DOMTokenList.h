/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@serenityos.org>
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/FlyString.h>
#include <AK/Optional.h>
#include <AK/String.h>
#include <AK/StringView.h>
#include <AK/Vector.h>
#include <LibWeb/Bindings/LegacyPlatformObject.h>
#include <LibWeb/DOM/ExceptionOr.h>
#include <LibWeb/Forward.h>

namespace Web::DOM {

// https://dom.spec.whatwg.org/#domtokenlist
class DOMTokenList final : public Bindings::LegacyPlatformObject {
    JS_OBJECT(DOMTokenList, Bindings::LegacyPlatformObject);

public:
    static DOMTokenList* create(Element const& associated_element, FlyString associated_attribute);
    DOMTokenList(Element const& associated_element, FlyString associated_attribute);
    ~DOMTokenList() = default;

    DOMTokenList& impl() { return *this; }

    void associated_attribute_changed(StringView value);

    virtual bool is_supported_property_index(u32 index) const override;
    virtual JS::Value item_value(size_t index) const override;

    size_t length() const { return m_token_set.size(); }
    String const& item(size_t index) const;
    bool contains(StringView token);
    ExceptionOr<void> add(Vector<String> const& tokens);
    ExceptionOr<void> remove(Vector<String> const& tokens);
    ExceptionOr<bool> toggle(String const& token, Optional<bool> force);
    ExceptionOr<bool> replace(String const& token, String const& new_token);
    ExceptionOr<bool> supports(StringView token);
    String value() const;
    void set_value(String value);

private:
    ExceptionOr<void> validate_token(StringView token) const;
    void run_update_steps();

    WeakPtr<Element> m_associated_element;
    FlyString m_associated_attribute;
    Vector<String> m_token_set;
};

}

namespace Web::Bindings {
inline JS::Object* wrap(JS::Realm&, Web::DOM::DOMTokenList& object) { return &object; }
using DOMTokenListWrapper = Web::DOM::DOMTokenList;
}
