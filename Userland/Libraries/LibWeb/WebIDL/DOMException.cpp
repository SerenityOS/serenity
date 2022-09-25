/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/HTML/Window.h>
#include <LibWeb/WebIDL/DOMException.h>

namespace Web::WebIDL {

JS::NonnullGCPtr<DOMException> DOMException::create(JS::Object& global_object, FlyString const& name, FlyString const& message)
{
    auto& window = verify_cast<HTML::Window>(global_object);
    return *window.heap().allocate<DOMException>(window.realm(), window, name, message);
}

JS::NonnullGCPtr<DOMException> DOMException::create_with_global_object(JS::Object& global_object, FlyString const& message, FlyString const& name)
{
    auto& window = verify_cast<HTML::Window>(global_object);
    return *window.heap().allocate<DOMException>(window.realm(), window, name, message);
}

DOMException::DOMException(HTML::Window& window, FlyString const& name, FlyString const& message)
    : PlatformObject(window.realm())
    , m_name(name)
    , m_message(message)
{
    set_prototype(&window.cached_web_prototype("DOMException"));
}

DOMException::~DOMException() = default;

}
