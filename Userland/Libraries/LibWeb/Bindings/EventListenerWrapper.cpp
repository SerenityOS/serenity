/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/FunctionObject.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibWeb/Bindings/EventListenerWrapper.h>
#include <LibWeb/DOM/IDLEventListener.h>

namespace Web {
namespace Bindings {

EventListenerWrapper::EventListenerWrapper(JS::Realm& realm, DOM::IDLEventListener& impl)
    : Wrapper(*realm.intrinsics().object_prototype())
    , m_impl(impl)
{
}

}
}
