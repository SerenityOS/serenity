/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/EventTargetWrapperFactory.h>
#include <LibWeb/DOM/EventTarget.h>

namespace Web::Bindings {

JS::Object* wrap(JS::GlobalObject& global_object, DOM::EventTarget& target)
{
    return target.create_wrapper(global_object);
}

}
