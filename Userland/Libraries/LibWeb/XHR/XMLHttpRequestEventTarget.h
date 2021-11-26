/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/Bindings/EventTargetWrapperFactory.h>
#include <LibWeb/DOM/EventDispatcher.h>
#include <LibWeb/DOM/EventTarget.h>

namespace Web::XHR {

#define ENUMERATE_XML_HTTP_REQUEST_EVENT_TARGET_EVENT_HANDLERS(E) \
    E(onloadstart, XHR::EventNames::loadstart)                    \
    E(onprogress, XHR::EventNames::progress)                      \
    E(onabort, XHR::EventNames::abort)                            \
    E(onerror, XHR::EventNames::error)                            \
    E(onload, XHR::EventNames::load)                              \
    E(ontimeout, XHR::EventNames::timeout)                        \
    E(onloadend, XHR::EventNames::loadend)

class XMLHttpRequestEventTarget
    : public DOM::EventTarget
    , public Bindings::Wrappable {
public:
    using WrapperType = Bindings::XMLHttpRequestEventTargetWrapper;

    virtual ~XMLHttpRequestEventTarget() override {};

#undef __ENUMERATE
#define __ENUMERATE(attribute_name, event_name)    \
    void set_##attribute_name(HTML::EventHandler); \
    HTML::EventHandler attribute_name();
    ENUMERATE_XML_HTTP_REQUEST_EVENT_TARGET_EVENT_HANDLERS(__ENUMERATE)
#undef __ENUMERATE

protected:
    explicit XMLHttpRequestEventTarget(Bindings::ScriptExecutionContext& script_execution_context)
        : DOM::EventTarget(script_execution_context)
    {
    }

private:
    virtual JS::Object* create_wrapper(JS::GlobalObject& global_object) override
    {
        return wrap(global_object, *this);
    }
};

}
