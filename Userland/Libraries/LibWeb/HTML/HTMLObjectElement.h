/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibCore/Forward.h>
#include <LibGfx/Forward.h>
#include <LibWeb/HTML/FormAssociatedElement.h>
#include <LibWeb/Loader/ImageLoader.h>

namespace Web::HTML {

class HTMLObjectElement final
    : public FormAssociatedElement
    , public ResourceClient {
public:
    using WrapperType = Bindings::HTMLObjectElementWrapper;

    HTMLObjectElement(DOM::Document&, DOM::QualifiedName);
    virtual ~HTMLObjectElement() override;

    virtual void parse_attribute(const FlyString& name, const String& value) override;

    String data() const;
    void set_data(String const& data) { set_attribute(HTML::AttributeNames::data, data); }

    String type() const { return attribute(HTML::AttributeNames::type); }

    // ^FormAssociatedElement
    // https://html.spec.whatwg.org/multipage/forms.html#category-listed
    virtual bool is_listed() const override { return true; }

private:
    virtual RefPtr<Layout::Node> create_layout_node(NonnullRefPtr<CSS::StyleProperties>) override;

    void queue_element_task_to_run_object_representation_steps();
    void run_object_representation_handler_steps(StringView resource_type);
    void run_object_representation_completed_steps();
    void run_object_representation_fallback_steps();

    void convert_resource_to_image();
    void update_layout_and_child_objects();

    // ^ResourceClient
    virtual void resource_did_load() override;
    virtual void resource_did_fail() override;

    Optional<ImageLoader> m_image_loader;
    bool m_should_show_fallback_content { false };
};

}
