/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
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
