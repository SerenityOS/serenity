/*
 * Copyright (c) 2018-2023, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteBuffer.h>
#include <AK/OwnPtr.h>
#include <LibGfx/Forward.h>
#include <LibJS/Heap/HeapFunction.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/DocumentLoadEventDelayer.h>
#include <LibWeb/HTML/BrowsingContext.h>
#include <LibWeb/HTML/CORSSettingAttribute.h>
#include <LibWeb/HTML/FormAssociatedElement.h>
#include <LibWeb/HTML/HTMLElement.h>
#include <LibWeb/HTML/SourceSet.h>
#include <LibWeb/Layout/ImageProvider.h>

namespace Web::HTML {

class HTMLImageElement final
    : public HTMLElement
    , public FormAssociatedElement
    , public Layout::ImageProvider
    , public DOM::Document::ViewportClient {
    WEB_PLATFORM_OBJECT(HTMLImageElement, HTMLElement);
    FORM_ASSOCIATED_ELEMENT(HTMLElement, HTMLImageElement)

public:
    virtual ~HTMLImageElement() override;

    virtual void attribute_changed(FlyString const& name, DeprecatedString const& value) override;

    DeprecatedString alt() const { return deprecated_attribute(HTML::AttributeNames::alt); }
    DeprecatedString src() const { return deprecated_attribute(HTML::AttributeNames::src); }

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

    // https://html.spec.whatwg.org/multipage/images.html#img-environment-changes
    void react_to_changes_in_the_environment();

    // https://html.spec.whatwg.org/multipage/images.html#update-the-image-data
    ErrorOr<void> update_the_image_data(bool restart_the_animations = false, bool maybe_omit_events = false);

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
    [[nodiscard]] bool will_lazy_load() const;

    // https://html.spec.whatwg.org/multipage/images.html#upgrade-the-pending-request-to-the-current-request
    void upgrade_pending_request_to_current_request();

    // ^Layout::ImageProvider
    virtual Optional<CSSPixels> intrinsic_width() const override;
    virtual Optional<CSSPixels> intrinsic_height() const override;
    virtual Optional<CSSPixelFraction> intrinsic_aspect_ratio() const override;
    virtual RefPtr<Gfx::Bitmap const> current_image_bitmap(Gfx::IntSize = {}) const override;
    virtual void set_visible_in_viewport(bool) override;

    void set_lazy_load_resumption_steps(Function<void()>);
    JS::GCPtr<JS::HeapFunction<void()>> take_lazy_load_resumption_steps(Badge<DOM::Document>);

    virtual void visit_edges(Cell::Visitor&) override;

private:
    HTMLImageElement(DOM::Document&, DOM::QualifiedName);

    virtual void initialize(JS::Realm&) override;
    virtual void finalize() override;

    virtual void adopted_from(DOM::Document&) override;

    virtual void apply_presentational_hints(CSS::StyleProperties&) const override;

    virtual JS::GCPtr<Layout::Node> create_layout_node(NonnullRefPtr<CSS::StyleProperties>) override;

    virtual void did_set_viewport_rect(CSSPixelRect const&) override;

    void handle_successful_fetch(AK::URL const&, StringView mime_type, ImageRequest&, ByteBuffer, bool maybe_omit_events, AK::URL const& previous_url);
    void handle_failed_fetch();
    void add_callbacks_to_image_request(JS::NonnullGCPtr<ImageRequest>, bool maybe_omit_events, AK::URL const& url_string, AK::URL const& previous_url);

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
    JS::GCPtr<ImageRequest> m_current_request;

    // https://html.spec.whatwg.org/multipage/images.html#pending-request
    JS::GCPtr<ImageRequest> m_pending_request;

    SourceSet m_source_set;

    // https://html.spec.whatwg.org/multipage/urls-and-fetching.html#lazy-load-resumption-steps
    // Each img and iframe element has associated lazy load resumption steps, initially null.
    JS::GCPtr<JS::HeapFunction<void()>> m_lazy_load_resumption_steps;

    CSSPixelSize m_last_seen_viewport_size;
};

}
