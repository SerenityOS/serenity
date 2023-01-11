/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibCore/Forward.h>
#include <LibGfx/Forward.h>
#include <LibWeb/HTML/BrowsingContextContainer.h>
#include <LibWeb/HTML/FormAssociatedElement.h>
#include <LibWeb/HTML/HTMLElement.h>
#include <LibWeb/Loader/ImageLoader.h>

namespace Web::HTML {

class HTMLObjectElement final
    : public BrowsingContextContainer
    , public FormAssociatedElement
    , public ResourceClient {
    WEB_PLATFORM_OBJECT(HTMLObjectElement, BrowsingContextContainer)
    FORM_ASSOCIATED_ELEMENT(BrowsingContextContainer, HTMLObjectElement)

    enum class Representation {
        Unknown,
        Image,
        NestedBrowsingContext,
        Children,
    };

public:
    virtual ~HTMLObjectElement() override;

    virtual void parse_attribute(DeprecatedFlyString const& name, DeprecatedString const& value) override;

    DeprecatedString data() const;
    void set_data(DeprecatedString const& data) { MUST(set_attribute(HTML::AttributeNames::data, data)); }

    DeprecatedString type() const { return attribute(HTML::AttributeNames::type); }

    // ^FormAssociatedElement
    // https://html.spec.whatwg.org/multipage/forms.html#category-listed
    virtual bool is_listed() const override { return true; }

private:
    HTMLObjectElement(DOM::Document&, DOM::QualifiedName);

    virtual void initialize(JS::Realm&) override;

    virtual JS::GCPtr<Layout::Node> create_layout_node(NonnullRefPtr<CSS::StyleProperties>) override;

    bool has_ancestor_media_element_or_object_element_not_showing_fallback_content() const;

    void queue_element_task_to_run_object_representation_steps();
    void run_object_representation_handler_steps(Optional<DeprecatedString> resource_type);
    void run_object_representation_completed_steps(Representation);
    void run_object_representation_fallback_steps();

    void convert_resource_to_image();
    void update_layout_and_child_objects(Representation);

    // ^ResourceClient
    virtual void resource_did_load() override;
    virtual void resource_did_fail() override;

    // ^DOM::Element
    virtual i32 default_tab_index_value() const override;

    Representation m_representation { Representation::Unknown };
    Optional<ImageLoader> m_image_loader;
};

}
