/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/HTML/EventHandler.h>
#include <LibWeb/XHR/EventNames.h>
#include <LibWeb/XHR/XMLHttpRequestEventTarget.h>

namespace Web::XHR {

HTML::EventHandler XMLHttpRequestEventTarget::onabort()
{
    return event_handler_attribute(Web::XHR::EventNames::abort);
}

void XMLHttpRequestEventTarget::set_onabort(HTML::EventHandler value)
{
    set_event_handler_attribute(Web::XHR::EventNames::abort, move(value));
}

HTML::EventHandler XMLHttpRequestEventTarget::onloadstart()
{
    return event_handler_attribute(Web::XHR::EventNames::loadstart);
}

void XMLHttpRequestEventTarget::set_onloadstart(HTML::EventHandler value)
{
    set_event_handler_attribute(Web::XHR::EventNames::loadstart, move(value));
}

HTML::EventHandler XMLHttpRequestEventTarget::onloadend()
{
    return event_handler_attribute(Web::XHR::EventNames::loadend);
}

void XMLHttpRequestEventTarget::set_onloadend(HTML::EventHandler value)
{
    set_event_handler_attribute(Web::XHR::EventNames::loadend, move(value));
}

HTML::EventHandler XMLHttpRequestEventTarget::onprogress()
{
    return event_handler_attribute(Web::XHR::EventNames::progress);
}

void XMLHttpRequestEventTarget::set_onprogress(HTML::EventHandler value)
{
    set_event_handler_attribute(Web::XHR::EventNames::progress, move(value));
}

HTML::EventHandler XMLHttpRequestEventTarget::onerror()
{
    return event_handler_attribute(Web::XHR::EventNames::error);
}

void XMLHttpRequestEventTarget::set_onerror(HTML::EventHandler value)
{
    set_event_handler_attribute(Web::XHR::EventNames::error, move(value));
}

HTML::EventHandler XMLHttpRequestEventTarget::onload()
{
    return event_handler_attribute(Web::XHR::EventNames::load);
}

void XMLHttpRequestEventTarget::set_onload(HTML::EventHandler value)
{
    set_event_handler_attribute(Web::XHR::EventNames::load, move(value));
}

HTML::EventHandler XMLHttpRequestEventTarget::ontimeout()
{
    return event_handler_attribute(Web::XHR::EventNames::timeout);
}

void XMLHttpRequestEventTarget::set_ontimeout(HTML::EventHandler value)
{
    set_event_handler_attribute(Web::XHR::EventNames::timeout, move(value));
}

}
