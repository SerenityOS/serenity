/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <AK/FlyString.h>
#include <LibJS/Runtime/Array.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibWeb/Bindings/NavigatorObject.h>
#include <LibWeb/Loader/ResourceLoader.h>

namespace Web {
namespace Bindings {

NavigatorObject::NavigatorObject(JS::GlobalObject& global_object)
    : Object(*global_object.object_prototype())
{
}

void NavigatorObject::initialize(JS::GlobalObject& global_object)
{
    auto& heap = this->heap();
    auto* languages = JS::Array::create(global_object);
    languages->indexed_properties().append(js_string(heap, "en-US"));

    define_property("appCodeName", js_string(heap, "Mozilla"));
    define_property("appName", js_string(heap, "Netscape"));
    define_property("appVersion", js_string(heap, "4.0"));
    define_property("language", languages->get(0));
    define_property("languages", languages);
    define_property("platform", js_string(heap, "SerenityOS"));
    define_property("product", js_string(heap, "Gecko"));

    define_native_property("userAgent", user_agent_getter, nullptr);
}

NavigatorObject::~NavigatorObject()
{
}

JS_DEFINE_NATIVE_GETTER(NavigatorObject::user_agent_getter)
{
    return JS::js_string(vm, ResourceLoader::the().user_agent());
}

}

}
