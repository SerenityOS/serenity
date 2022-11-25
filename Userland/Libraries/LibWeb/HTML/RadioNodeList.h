/*
 * Copyright (c) 2022, Johan Dahlin <jdahlin@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>
#include <LibWeb/DOM/NodeList.h>
#include <LibWeb/HTML/HTMLInputElement.h>

namespace Web::HTML {

// https://html.spec.whatwg.org/multipage/common-dom-interfaces.html#radionodelist
class RadioNodeList : public DOM::NodeList {
    WEB_PLATFORM_OBJECT(RadioNodeList, DOM::NodeList);

public:
    virtual ~RadioNodeList() override;

    String value() const;
    WebIDL::ExceptionOr<void> set_value(String const& name);

protected:
    explicit RadioNodeList(JS::Realm&);
};

}
