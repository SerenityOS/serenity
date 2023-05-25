/*
 * Copyright (c) 2018-2023, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteBuffer.h>
#include <AK/OwnPtr.h>
#include <LibGfx/Forward.h>
#include <LibWeb/DOM/DocumentLoadEventDelayer.h>
#include <LibWeb/HTML/CORSSettingAttribute.h>
#include <LibWeb/HTML/FormAssociatedElement.h>
#include <LibWeb/HTML/HTMLElement.h>
#include <LibWeb/HTML/SourceSet.h>
#include <LibWeb/Layout/ImageProvider.h>

namespace Web::HTML {

class HTMLImageElement final
    : public HTMLElement
    , public FormAssociatedElement
    , public Layout::ImageProvider {
    WEB_PLATFORM_OBJECT(HTMLImageElement, HTMLElement);
    FORM_ASSOCIATED_ELEMENT(HTMLElement, HTMLImageElement)

public:
    virtual ~HTMLImageElement() override;

    virtual void parse_attribute(DeprecatedFlyString const& name, DeprecatedString const& value) override;
    virtual void did_remove_attribute(DeprecatedFlyString const& name) override;

    DeprecatedString alt() const { return attribute(HTML::AttributeNames::alt); }
    DeprecatedString src() const { return attribute(HTML::AttributeNames::src); }

    RefPtr<Gfx::Bitmap const> bitmap() const;

    unsigned width() const;
    WebIDL::ExceptionOr<void> set_width(unsigned);

    unsigned height() const;
    WebIDL::ExceptionOr<void> set_height(unsigned);

    unsigned natural_width() const;
    unsigned natural_height() const;

    // https://html.spec.whatwg.org/multipage/embedded-content.html#dom-img-complete
    bool complete() const;

    virtual Optional<ARIA::Role> default_role() const override;

    // https://html.spec.whatwg.org/multipage/images.html#update-the-image-data
    ErrorOr<void> update_the_image_data(bool restart_the_animations = false);

    // https://html.spec.whatwg.org/multipage/images.html#use-srcset-or-picture
    [[nodiscard]] bool uses_srcset_or_picture() const;

    // https://html.spec.whatwg.org/multipage/rendering.html#restart-the-animation
    void restart_the_animation();

    // https://html.spec.whatwg.org/multipage/images.html#select-an-image-source
    [[nodiscard]] Optional<ImageSourceAndPixelDensity> select_an_image_source();

    void set_source_set(SourceSet);

    ImageRequest& current_request() { return *m_current_request; }
    ImageRequest const& current_request() const { return *m_current_request; }

    size_t current_frame_index() const { return m_current_frame_index; }
    enum class LazyLoading {
        Lazy,
        Eager,
    };
    [[nodiscard]] LazyLoading lazy_loading() const;

    // https://html.spec.whatwg.org/multipage/images.html#upgrade-the-pending-request-to-the-current-request
    void upgrade_pending_request_to_current_request();

    // ^Layout::ImageProvider
    virtual Optional<CSSPixels> intrinsic_width() const override;
    virtual Optional<CSSPixels> intrinsic_height() const override;
    virtual Optional<float> intrinsic_aspect_ratio() const override;
    virtual RefPtr<Gfx::Bitmap const> current_image_bitmap(Gfx::IntSize = {}) const override;
    virtual void set_visible_in_viewport(bool) override;

private:
    HTMLImageElement(DOM::Document&, DOM::QualifiedName);

    virtual JS::ThrowCompletionOr<void> initialize(JS::Realm&) override;

    virtual void apply_presentational_hints(CSS::StyleProperties&) const override;

    virtual JS::GCPtr<Layout::Node> create_layout_node(NonnullRefPtr<CSS::StyleProperties>) override;

    void handle_successful_fetch(AK::URL const&, StringView mime_type, ImageRequest&, ByteBuffer);
    void handle_failed_fetch();

    void animate();

    RefPtr<Core::Timer> m_animation_timer;
    size_t m_current_frame_index { 0 };
    size_t m_loops_completed { 0 };

    Optional<DOM::DocumentLoadEventDelayer> m_load_event_delayer;

    CORSSettingAttribute m_cors_setting { CORSSettingAttribute::NoCORS };

    // https://html.spec.whatwg.org/multipage/images.html#last-selected-source
    // Each img element has a last selected source, which must initially be null.
    Optional<String> m_last_selected_source;

    // https://html.spec.whatwg.org/multipage/images.html#current-request
    RefPtr<ImageRequest> m_current_request;

    // https://html.spec.whatwg.org/multipage/images.html#pending-request
    RefPtr<ImageRequest> m_pending_request;

    SourceSet m_source_set;
};

}
