/*
 * Copyright (c) 2021, the SerenityOS developers.
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

#include <LibWeb/Bindings/HTMLImageElementPrototype.h>
#include <LibWeb/Bindings/HTMLImageElementWrapper.h>
#include <LibWeb/Bindings/ImageConstructor.h>
#include <LibWeb/Bindings/NodeWrapperFactory.h>
#include <LibWeb/DOM/ElementFactory.h>
#include <LibWeb/DOM/Window.h>
#include <LibWeb/Namespace.h>

namespace Web::Bindings {

ImageConstructor::ImageConstructor(JS::GlobalObject& global_object)
    : NativeFunction(*global_object.function_prototype())
{
}

void ImageConstructor::initialize(JS::GlobalObject& global_object)
{
    auto& vm = this->vm();
    auto& window = static_cast<WindowObject&>(global_object);
    NativeFunction::initialize(global_object);

    define_property(vm.names.prototype, &window.ensure_web_prototype<HTMLImageElementPrototype>("HTMLImageElement"), 0);
    define_property(vm.names.length, JS::Value(0), JS::Attribute::Configurable);
}

ImageConstructor::~ImageConstructor()
{
}

JS::Value ImageConstructor::call()
{
    vm().throw_exception<JS::TypeError>(global_object(), JS::ErrorType::ConstructorWithoutNew, "Image");
    return {};
}

// https://html.spec.whatwg.org/multipage/embedded-content.html#dom-image
JS::Value ImageConstructor::construct(Function&)
{
    auto& window = static_cast<WindowObject&>(global_object());
    auto& document = window.impl().document();
    auto image_element = DOM::create_element(document, HTML::TagNames::img, Namespace::HTML);

    if (vm().argument_count() > 0) {
        u32 width = vm().argument(0).to_u32(global_object());
        if (vm().exception())
            return {};
        image_element->set_attribute(HTML::AttributeNames::width, String::formatted("{}", width));
    }

    if (vm().argument_count() > 1) {
        u32 height = vm().argument(1).to_u32(global_object());
        if (vm().exception())
            return {};
        image_element->set_attribute(HTML::AttributeNames::height, String::formatted("{}", height));
    }

    return wrap(global_object(), image_element);
}

}
