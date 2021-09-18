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

class XMLHttpRequestEventTarget
    : public DOM::EventTarget
    , public Bindings::Wrappable {
public:
    using WrapperType = Bindings::XMLHttpRequestEventTargetWrapper;

    virtual ~XMLHttpRequestEventTarget() override {};

    HTML::EventHandler onloadstart();
    void set_onloadstart(HTML::EventHandler);
    HTML::EventHandler onprogress();
    void set_onprogress(HTML::EventHandler);
    HTML::EventHandler onabort();
    void set_onabort(HTML::EventHandler);
    HTML::EventHandler onerror();
    void set_onerror(HTML::EventHandler);
    HTML::EventHandler onload();
    void set_onload(HTML::EventHandler);
    HTML::EventHandler ontimeout();
    void set_ontimeout(HTML::EventHandler);
    HTML::EventHandler onloadend();
    void set_onloadend(HTML::EventHandler);

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
