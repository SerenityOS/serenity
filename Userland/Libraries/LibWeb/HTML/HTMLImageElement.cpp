/*
 * Copyright (c) 2018-2023, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/Timer.h>
#include <LibGfx/Bitmap.h>
#include <LibWeb/ARIA/Roles.h>
#include <LibWeb/CSS/Parser/Parser.h>
#include <LibWeb/CSS/StyleComputer.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/Event.h>
#include <LibWeb/Fetch/Fetching/Fetching.h>
#include <LibWeb/Fetch/Infrastructure/FetchController.h>
#include <LibWeb/Fetch/Response.h>
#include <LibWeb/HTML/CORSSettingAttribute.h>
#include <LibWeb/HTML/DecodedImageData.h>
#include <LibWeb/HTML/EventNames.h>
#include <LibWeb/HTML/HTMLImageElement.h>
#include <LibWeb/HTML/HTMLLinkElement.h>
#include <LibWeb/HTML/HTMLPictureElement.h>
#include <LibWeb/HTML/HTMLSourceElement.h>
#include <LibWeb/HTML/ImageRequest.h>
#include <LibWeb/HTML/ListOfAvailableImages.h>
#include <LibWeb/HTML/Parser/HTMLParser.h>
#include <LibWeb/HTML/PotentialCORSRequest.h>
#include <LibWeb/Layout/ImageBox.h>
#include <LibWeb/Loader/ResourceLoader.h>
#include <LibWeb/Painting/PaintableBox.h>
#include <LibWeb/Platform/ImageCodecPlugin.h>

namespace Web::HTML {

HTMLImageElement::HTMLImageElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
{
    m_animation_timer = Core::Timer::try_create().release_value_but_fixme_should_propagate_errors();
    m_animation_timer->on_timeout = [this] { animate(); };
}

HTMLImageElement::~HTMLImageElement() = default;

JS::ThrowCompletionOr<void> HTMLImageElement::initialize(JS::Realm& realm)
{
    MUST_OR_THROW_OOM(Base::initialize(realm));
    set_prototype(&Bindings::ensure_web_prototype<Bindings::HTMLImageElementPrototype>(realm, "HTMLImageElement"));

    m_current_request = TRY_OR_THROW_OOM(vm(), ImageRequest::create());

    return {};
}

void HTMLImageElement::apply_presentational_hints(CSS::StyleProperties& style) const
{
    for_each_attribute([&](auto& name, auto& value) {
        if (name == HTML::AttributeNames::width) {
            if (auto parsed_value = parse_dimension_value(value))
                style.set_property(CSS::PropertyID::Width, parsed_value.release_nonnull());
        } else if (name == HTML::AttributeNames::height) {
            if (auto parsed_value = parse_dimension_value(value))
                style.set_property(CSS::PropertyID::Height, parsed_value.release_nonnull());
        } else if (name == HTML::AttributeNames::hspace) {
            if (auto parsed_value = parse_dimension_value(value)) {
                style.set_property(CSS::PropertyID::MarginLeft, *parsed_value);
                style.set_property(CSS::PropertyID::MarginRight, *parsed_value);
            }
        } else if (name == HTML::AttributeNames::vspace) {
            if (auto parsed_value = parse_dimension_value(value)) {
                style.set_property(CSS::PropertyID::MarginTop, *parsed_value);
                style.set_property(CSS::PropertyID::MarginBottom, *parsed_value);
            }
        }
    });
}

void HTMLImageElement::parse_attribute(DeprecatedFlyString const& name, DeprecatedString const& value)
{
    HTMLElement::parse_attribute(name, value);

    if (name == HTML::AttributeNames::crossorigin) {
        m_cors_setting = cors_setting_attribute_from_keyword(String::from_deprecated_string(value).release_value_but_fixme_should_propagate_errors());
    }

    if (name.is_one_of(HTML::AttributeNames::src, HTML::AttributeNames::srcset)) {
        update_the_image_data(true).release_value_but_fixme_should_propagate_errors();
    }

    if (name == HTML::AttributeNames::alt) {
        if (layout_node())
            verify_cast<Layout::ImageBox>(*layout_node()).dom_node_did_update_alt_text({});
    }
}

void HTMLImageElement::did_remove_attribute(DeprecatedFlyString const& name)
{
    Base::did_remove_attribute(name);

    if (name == HTML::AttributeNames::crossorigin) {
        m_cors_setting = CORSSettingAttribute::NoCORS;
    }
}

JS::GCPtr<Layout::Node> HTMLImageElement::create_layout_node(NonnullRefPtr<CSS::StyleProperties> style)
{
    return heap().allocate_without_realm<Layout::ImageBox>(document(), *this, move(style), *this);
}

RefPtr<Gfx::Bitmap const> HTMLImageElement::bitmap() const
{
    return current_image_bitmap();
}

RefPtr<Gfx::Bitmap const> HTMLImageElement::current_image_bitmap() const
{
    if (auto data = m_current_request->image_data())
        return data->bitmap(m_current_frame_index);
    return nullptr;
}

void HTMLImageElement::set_visible_in_viewport(bool)
{
    // FIXME: Loosen grip on image data when it's not visible, e.g via volatile memory.
}

// https://html.spec.whatwg.org/multipage/embedded-content.html#dom-img-width
unsigned HTMLImageElement::width() const
{
    const_cast<DOM::Document&>(document()).update_layout();

    // Return the rendered width of the image, in CSS pixels, if the image is being rendered.
    if (auto* paintable_box = this->paintable_box())
        return paintable_box->content_width().value();

    // NOTE: This step seems to not be in the spec, but all browsers do it.
    auto width_attr = get_attribute(HTML::AttributeNames::width);
    if (auto converted = width_attr.to_uint(); converted.has_value())
        return *converted;

    // ...or else the density-corrected intrinsic width and height of the image, in CSS pixels,
    // if the image has intrinsic dimensions and is available but not being rendered.
    if (auto bitmap = current_image_bitmap())
        return bitmap->width();

    // ...or else 0, if the image is not available or does not have intrinsic dimensions.
    return 0;
}

void HTMLImageElement::set_width(unsigned width)
{
    MUST(set_attribute(HTML::AttributeNames::width, DeprecatedString::number(width)));
}

// https://html.spec.whatwg.org/multipage/embedded-content.html#dom-img-height
unsigned HTMLImageElement::height() const
{
    const_cast<DOM::Document&>(document()).update_layout();

    // Return the rendered height of the image, in CSS pixels, if the image is being rendered.
    if (auto* paintable_box = this->paintable_box())
        return paintable_box->content_height().value();

    // NOTE: This step seems to not be in the spec, but all browsers do it.
    auto height_attr = get_attribute(HTML::AttributeNames::height);
    if (auto converted = height_attr.to_uint(); converted.has_value())
        return *converted;

    // ...or else the density-corrected intrinsic height and height of the image, in CSS pixels,
    // if the image has intrinsic dimensions and is available but not being rendered.
    if (auto bitmap = current_image_bitmap())
        return bitmap->height();

    // ...or else 0, if the image is not available or does not have intrinsic dimensions.
    return 0;
}

void HTMLImageElement::set_height(unsigned height)
{
    MUST(set_attribute(HTML::AttributeNames::height, DeprecatedString::number(height)));
}

// https://html.spec.whatwg.org/multipage/embedded-content.html#dom-img-naturalwidth
unsigned HTMLImageElement::natural_width() const
{
    // Return the density-corrected intrinsic width of the image, in CSS pixels,
    // if the image has intrinsic dimensions and is available.
    if (auto bitmap = current_image_bitmap())
        return bitmap->width();

    // ...or else 0.
    return 0;
}

// https://html.spec.whatwg.org/multipage/embedded-content.html#dom-img-naturalheight
unsigned HTMLImageElement::natural_height() const
{
    // Return the density-corrected intrinsic height of the image, in CSS pixels,
    // if the image has intrinsic dimensions and is available.
    if (auto bitmap = current_image_bitmap())
        return bitmap->height();

    // ...or else 0.
    return 0;
}

// https://html.spec.whatwg.org/multipage/embedded-content.html#dom-img-complete
bool HTMLImageElement::complete() const
{
    // The IDL attribute complete must return true if any of the following conditions is true:

    // - Both the src attribute and the srcset attribute are omitted.
    if (!has_attribute(HTML::AttributeNames::src) && !has_attribute(HTML::AttributeNames::srcset))
        return true;

    // - The srcset attribute is omitted and the src attribute's value is the empty string.
    if (!has_attribute(HTML::AttributeNames::srcset) && attribute(HTML::AttributeNames::src) == ""sv)
        return true;

    // - The img element's current request's state is completely available and its pending request is null.
    // - The img element's current request's state is broken and its pending request is null.
    // FIXME: This is ad-hoc and should be updated once we are loading images via the Fetch mechanism.
    if (auto bitmap = current_image_bitmap())
        return true;

    return false;
}

Optional<ARIA::Role> HTMLImageElement::default_role() const
{
    // https://www.w3.org/TR/html-aria/#el-img
    // https://www.w3.org/TR/html-aria/#el-img-no-alt
    if (alt().is_null() || !alt().is_empty())
        return ARIA::Role::img;
    // https://www.w3.org/TR/html-aria/#el-img-empty-alt
    return ARIA::Role::presentation;
}

// https://html.spec.whatwg.org/multipage/images.html#use-srcset-or-picture
bool HTMLImageElement::uses_srcset_or_picture() const
{
    // An img element is said to use srcset or picture if it has a srcset attribute specified
    // or if it has a parent that is a picture element.
    return has_attribute(HTML::AttributeNames::srcset) || (parent() && is<HTMLPictureElement>(*parent()));
}

// https://html.spec.whatwg.org/multipage/images.html#update-the-image-data
ErrorOr<void> HTMLImageElement::update_the_image_data(bool restart_animations)
{
    // 1. If the element's node document is not fully active, then:
    if (!document().is_fully_active()) {
        // FIXME: 1. Continue running this algorithm in parallel.
        // FIXME: 2. Wait until the element's node document is fully active.
        // FIXME: 3. If another instance of this algorithm for this img element was started after this instance
        //           (even if it aborted and is no longer running), then return.
        // FIXME: 4. Queue a microtask to continue this algorithm.
    }

    // 2. FIXME: If the user agent cannot support images, or its support for images has been disabled,
    //           then abort the image request for the current request and the pending request,
    //           set current request's state to unavailable, set pending request to null, and return.

    // 3. Let selected source be null and selected pixel density be undefined.
    Optional<String> selected_source;
    Optional<float> selected_pixel_density;

    // 4. If the element does not use srcset or picture
    //    and it has a src attribute specified whose value is not the empty string,
    //    then set selected source to the value of the element's src attribute
    //    and set selected pixel density to 1.0.
    if (!uses_srcset_or_picture() && has_attribute(HTML::AttributeNames::src) && !attribute(HTML::AttributeNames::src).is_empty()) {
        selected_source = TRY(String::from_deprecated_string(attribute(HTML::AttributeNames::src)));
        selected_pixel_density = 1.0f;
    }

    // 5. Set the element's last selected source to selected source.
    m_last_selected_source = selected_source;

    // 6. If selected source is not null, then:
    if (selected_source.has_value()) {
        // 1. Parse selected source, relative to the element's node document.
        //    If that is not successful, then abort this inner set of steps.
        //    Otherwise, let urlString be the resulting URL string.
        auto url_string = document().parse_url(selected_source.value().to_deprecated_string());
        if (!url_string.is_valid())
            goto after_step_6;

        // 2. Let key be a tuple consisting of urlString, the img element's crossorigin attribute's mode,
        //    and, if that mode is not No CORS, the node document's origin.
        ListOfAvailableImages::Key key;
        key.url = url_string;
        key.mode = m_cors_setting;
        key.origin = document().origin();

        // 3. If the list of available images contains an entry for key, then:
        if (auto entry = document().list_of_available_images().get(key)) {
            // 1. Set the ignore higher-layer caching flag for that entry.
            entry->ignore_higher_layer_caching = true;

            // 2. Abort the image request for the current request and the pending request.
            m_current_request->abort(realm());

            // FIXME: Spec bug? Seems like pending request can be null here.
            if (m_pending_request)
                m_pending_request->abort(realm());

            // 3. Set pending request to null.
            m_pending_request = nullptr;

            // 4. Let current request be a new image request whose image data is that of the entry and whose state is completely available.
            m_current_request = ImageRequest::create().release_value_but_fixme_should_propagate_errors();
            m_current_request->set_image_data(entry->image_data);
            m_current_request->set_state(ImageRequest::State::CompletelyAvailable);

            // 5. Prepare current request for presentation given img.
            m_current_request->prepare_for_presentation(*this);

            // 6. Set current request's current pixel density to selected pixel density.
            // FIXME: Spec bug! `selected_pixel_density` can be undefined here, per the spec.
            //        That's why we value_or(1.0f) it.
            m_current_request->set_current_pixel_density(selected_pixel_density.value_or(1.0f));

            // 7. Queue an element task on the DOM manipulation task source given the img element and following steps:
            queue_an_element_task(HTML::Task::Source::DOMManipulation, [this, restart_animations, url_string] {
                // 1. If restart animation is set, then restart the animation.
                if (restart_animations)
                    restart_the_animation();

                // 2. Set current request's current URL to urlString.
                m_current_request->set_current_url(url_string);

                // 3. Fire an event named load at the img element.
                dispatch_event(DOM::Event::create(realm(), HTML::EventNames::load).release_value_but_fixme_should_propagate_errors());
            });

            // 8. Abort the update the image data algorithm.
            return {};
        }
    }
after_step_6:
    // 7. Queue a microtask to perform the rest of this algorithm, allowing the task that invoked this algorithm to continue.
    queue_a_microtask(&document(), [this, restart_animations]() mutable {
        // FIXME: 8. If another instance of this algorithm for this img element was started after this instance
        //           (even if it aborted and is no longer running), then return.

        // 9. Let selected source and selected pixel density be
        //    the URL and pixel density that results from selecting an image source, respectively.
        Optional<ImageSource> selected_source;
        Optional<float> pixel_density;
        if (auto result = select_an_image_source(); result.has_value()) {
            selected_source = result.value().source;
            pixel_density = result.value().pixel_density;
        }

        // 10. If selected source is null, then:
        if (!selected_source.has_value()) {
            // 1. Set the current request's state to broken,
            //    abort the image request for the current request and the pending request,
            //    and set pending request to null.
            m_current_request->set_state(ImageRequest::State::Broken);
            m_current_request->abort(realm());

            // FIXME: Spec bug? Seems like the image's pending request can be null here.
            if (m_pending_request)
                m_pending_request->abort(realm());
            m_pending_request = nullptr;

            // 2. Queue an element task on the DOM manipulation task source given the img element and the following steps:
            queue_an_element_task(HTML::Task::Source::DOMManipulation, [this] {
                // 1. Change the current request's current URL to the empty string.
                m_current_request->set_current_url(""sv);

                // 2. If the element has a src attribute or it uses srcset or picture, fire an event named error at the img element.
                if (has_attribute(HTML::AttributeNames::src) || uses_srcset_or_picture()) {
                    dispatch_event(DOM::Event::create(realm(), HTML::EventNames::error).release_value_but_fixme_should_propagate_errors());
                }
            });

            // 3. Return.
            return;
        }

        // 11. Parse selected source, relative to the element's node document, and let urlString be the resulting URL string.
        auto url_string = document().parse_url(selected_source.value().url.to_deprecated_string());
        // If that is not successful, then:
        if (!url_string.is_valid()) {
            // 1. Abort the image request for the current request and the pending request.
            m_current_request->abort(realm());

            // FIXME: Spec bug? Seems like pending request can be null here.
            if (m_pending_request)
                m_pending_request->abort(realm());

            // 2. Set the current request's state to broken.
            m_current_request->set_state(ImageRequest::State::Broken);

            // 3. Set pending request to null.
            m_pending_request = nullptr;

            // 4. Queue an element task on the DOM manipulation task source given the img element and the following steps:
            queue_an_element_task(HTML::Task::Source::DOMManipulation, [this, selected_source] {
                // 1. Change the current request's current URL to selected source.
                m_current_request->set_current_url(selected_source.value().url);

                // 2. Fire an event named error at the img element.
                dispatch_event(DOM::Event::create(realm(), HTML::EventNames::error).release_value_but_fixme_should_propagate_errors());
            });

            // 5. Return.
            return;
        }

        // 12. If the pending request is not null and urlString is the same as the pending request's current URL, then return.
        if (m_pending_request && url_string == m_pending_request->current_url())
            return;

        // 13. If urlString is the same as the current request's current URL and current request's state is partially available,
        //     then abort the image request for the pending request,
        //     queue an element task on the DOM manipulation task source given the img element
        //     to restart the animation if restart animation is set, and return.
        if (url_string == m_current_request->current_url() && m_current_request->state() == ImageRequest::State::PartiallyAvailable) {
            // FIXME: Spec bug? Seems like pending request can be null here.
            if (m_pending_request)
                m_pending_request->abort(realm());
            if (restart_animations) {
                queue_an_element_task(HTML::Task::Source::DOMManipulation, [this] {
                    restart_the_animation();
                });
            }
            return;
        }

        // 14. If the pending request is not null, then abort the image request for the pending request.
        if (m_pending_request)
            m_pending_request->abort(realm());

        // 15. Set image request to a new image request whose current URL is urlString.
        auto image_request = ImageRequest::create().release_value_but_fixme_should_propagate_errors();
        image_request->set_current_url(url_string);

        // 16. If current request's state is unavailable or broken, then set the current request to image request.
        //     Otherwise, set the pending request to image request.
        if (m_current_request->state() == ImageRequest::State::Unavailable || m_current_request->state() == ImageRequest::State::Broken)
            m_current_request = image_request;
        else
            m_pending_request = image_request;

        // 17. Let request be the result of creating a potential-CORS request given urlString, "image",
        //     and the current state of the element's crossorigin content attribute.
        auto request = create_potential_CORS_request(vm(), url_string, Fetch::Infrastructure::Request::Destination::Image, m_cors_setting);

        // 18. Set request's client to the element's node document's relevant settings object.
        request->set_client(&document().relevant_settings_object());

        // 19. If the element uses srcset or picture, set request's initiator to "imageset".
        if (uses_srcset_or_picture())
            request->set_initiator(Fetch::Infrastructure::Request::Initiator::ImageSet);

        // 20. Set request's referrer policy to the current state of the element's referrerpolicy attribute.
        request->set_referrer_policy(ReferrerPolicy::from_string(attribute(HTML::AttributeNames::referrerpolicy)));

        // FIXME: 21. Set request's priority to the current state of the element's fetchpriority attribute.

        // 22. Let delay load event be true if the img's lazy loading attribute is in the Eager state, or if scripting is disabled for the img, and false otherwise.
        auto delay_load_event = lazy_loading() == LazyLoading::Eager;

        // FIXME: 23. If the will lazy load element steps given the img return true, then:
        // FIXME:     1. Set the img's lazy load resumption steps to the rest of this algorithm starting with the step labeled fetch the image.
        // FIXME:     2. Start intersection-observing a lazy loading element for the img element.
        // FIXME:     3. Return.

        Fetch::Infrastructure::FetchAlgorithms::Input fetch_algorithms_input {};
        fetch_algorithms_input.process_response = [this, image_request, url_string](JS::NonnullGCPtr<Fetch::Infrastructure::Response> response) {
            // 25. As soon as possible, jump to the first applicable entry from the following list:

            // FIXME: - If the resource type is multipart/x-mixed-replace

            // - If the resource type and data corresponds to a supported image format, as described below
            // - The next task that is queued by the networking task source while the image is being fetched must run the following steps:
            queue_an_element_task(HTML::Task::Source::Networking, [this, response, image_request, url_string] {
                auto process_body = [image_request, url_string, this](ByteBuffer data) {
                    handle_successful_fetch(url_string, image_request, move(data));
                };
                auto process_body_error = [this](auto&) {
                    handle_failed_fetch();
                };
                // FIXME: See HTMLLinkElement::default_fetch_and_process_linked_resource for thorough notes on the workaround
                //        added here for CORS cross-origin responses. The gist is that all cross-origin responses will have a
                //        null bodyBytes. So we must read the actual body from the unsafe response.
                //        https://github.com/whatwg/html/issues/9066
                if (response->is_cors_cross_origin() && !response->body().has_value() && response->unsafe_response()->body().has_value()) {
                    auto unsafe_response = static_cast<Fetch::Infrastructure::OpaqueFilteredResponse const&>(*response).internal_response();
                    unsafe_response->body()->fully_read(realm(), move(process_body), move(process_body_error), JS::NonnullGCPtr { realm().global_object() }).release_value_but_fixme_should_propagate_errors();
                } else if (response->body().has_value()) {
                    response->body().value().fully_read(realm(), move(process_body), move(process_body_error), JS::NonnullGCPtr { realm().global_object() }).release_value_but_fixme_should_propagate_errors();
                }
            });
        };

        // 24. Fetch the image: Fetch request.
        //     Return from this algorithm, and run the remaining steps as part of the fetch's processResponse for the response response.

        // When delay load event is true, fetching the image must delay the load event of the element's node document
        // until the task that is queued by the networking task source once the resource has been fetched (defined below) has been run.
        if (delay_load_event)
            m_load_event_delayer.emplace(document());

        auto fetch_controller = Fetch::Fetching::fetch(
            realm(),
            request,
            Fetch::Infrastructure::FetchAlgorithms::create(vm(), move(fetch_algorithms_input)))
                                    .release_value_but_fixme_should_propagate_errors();

        image_request->set_fetch_controller(fetch_controller);
    });
    return {};
}

void HTMLImageElement::handle_successful_fetch(AK::URL const& url_string, ImageRequest& image_request, ByteBuffer data)
{
    // AD-HOC: At this point, things gets very ad-hoc.
    // FIXME: Bring this closer to spec.

    ScopeGuard undelay_load_event_guard = [this] {
        m_load_event_delayer.clear();
    };

    auto result = Web::Platform::ImageCodecPlugin::the().decode_image(data.bytes());
    if (!result.has_value()) {
        dispatch_event(DOM::Event::create(realm(), HTML::EventNames::error).release_value_but_fixme_should_propagate_errors());
        return;
    }

    Vector<DecodedImageData::Frame> frames;
    for (auto& frame : result.value().frames) {
        frames.append(DecodedImageData::Frame {
            .bitmap = frame.bitmap,
            .duration = static_cast<int>(frame.duration),
        });
    }

    auto image_data = DecodedImageData::create(move(frames), result.value().loop_count, result.value().is_animated).release_value_but_fixme_should_propagate_errors();
    image_request.set_image_data(image_data);

    ListOfAvailableImages::Key key;
    key.url = url_string;
    key.mode = m_cors_setting;
    key.origin = document().origin();

    // 1. If image request is the pending request, abort the image request for the current request,
    //    upgrade the pending request to the current request
    //    and prepare image request for presentation given the img element.
    if (image_request == m_pending_request) {
        m_current_request->abort(realm());
        upgrade_pending_request_to_current_request();
        image_request.prepare_for_presentation(*this);
    }

    // 2. Set image request to the completely available state.
    image_request.set_state(ImageRequest::State::CompletelyAvailable);

    // 3. Add the image to the list of available images using the key key, with the ignore higher-layer caching flag set.
    document().list_of_available_images().add(key, image_data, true).release_value_but_fixme_should_propagate_errors();

    // 4. Fire an event named load at the img element.
    dispatch_event(DOM::Event::create(realm(), HTML::EventNames::load).release_value_but_fixme_should_propagate_errors());

    set_needs_style_update(true);
    document().set_needs_layout();

    if (image_data->is_animated() && image_data->frame_count() > 1) {
        m_current_frame_index = 0;
        m_animation_timer->set_interval(image_data->frame_duration(0));
        m_animation_timer->start();
    }
}

// https://html.spec.whatwg.org/multipage/images.html#upgrade-the-pending-request-to-the-current-request
void HTMLImageElement::upgrade_pending_request_to_current_request()
{
    // 1. Let the img element's current request be the pending request.
    VERIFY(m_pending_request);
    m_current_request = m_pending_request;

    // 2. Let the img element's pending request be null.
    m_pending_request = nullptr;
}

void HTMLImageElement::handle_failed_fetch()
{
    // AD-HOC
    dispatch_event(DOM::Event::create(realm(), HTML::EventNames::error).release_value_but_fixme_should_propagate_errors());
}

// https://html.spec.whatwg.org/multipage/rendering.html#restart-the-animation
void HTMLImageElement::restart_the_animation()
{
    m_current_frame_index = 0;
    m_animation_timer->start();
}

// https://html.spec.whatwg.org/multipage/images.html#update-the-source-set
static void update_the_source_set(DOM::Element& element)
{
    // When asked to update the source set for a given img or link element el, user agents must do the following:
    VERIFY(is<HTMLImageElement>(element) || is<HTMLLinkElement>(element));

    // 1. Set el's source set to an empty source set.
    if (is<HTMLImageElement>(element))
        static_cast<HTMLImageElement&>(element).set_source_set(SourceSet {});
    else if (is<HTMLLinkElement>(element))
        TODO();

    // 2. Let elements be « el ».
    JS::MarkedVector<DOM::Element*> elements(element.heap());
    elements.append(&element);

    // 3. If el is an img element whose parent node is a picture element,
    //    then replace the contents of elements with el's parent node's child elements, retaining relative order.
    if (is<HTMLImageElement>(element) && element.parent() && is<HTMLPictureElement>(*element.parent())) {
        elements.clear();
        element.parent()->for_each_child_of_type<DOM::Element>([&](auto& child) {
            elements.append(&child);
        });
    }

    // 4. For each child in elements:
    for (auto child : elements) {
        // 1. If child is el:
        if (child == &element) {
            // 1. Let default source be the empty string.
            String default_source;

            // 2. Let srcset be the empty string.
            String srcset;

            // 3. Let sizes be the empty string.
            String sizes;

            // 4. If el is an img element that has a srcset attribute, then set srcset to that attribute's value.
            if (is<HTMLImageElement>(element)) {
                if (auto srcset_value = element.attribute(HTML::AttributeNames::srcset); !srcset_value.is_null())
                    srcset = String::from_deprecated_string(srcset_value).release_value_but_fixme_should_propagate_errors();
            }

            // 5. Otherwise, if el is a link element that has an imagesrcset attribute, then set srcset to that attribute's value.
            else if (is<HTMLLinkElement>(element)) {
                if (auto imagesrcset_value = element.attribute(HTML::AttributeNames::imagesrcset); !imagesrcset_value.is_null())
                    srcset = String::from_deprecated_string(imagesrcset_value).release_value_but_fixme_should_propagate_errors();
            }

            // 6. If el is an img element that has a sizes attribute, then set sizes to that attribute's value.
            if (is<HTMLImageElement>(element)) {
                if (auto sizes_value = element.attribute(HTML::AttributeNames::sizes); !sizes_value.is_null())
                    sizes = String::from_deprecated_string(sizes_value).release_value_but_fixme_should_propagate_errors();
            }

            // 7. Otherwise, if el is a link element that has an imagesizes attribute, then set sizes to that attribute's value.
            else if (is<HTMLLinkElement>(element)) {
                if (auto imagesizes_value = element.attribute(HTML::AttributeNames::imagesizes); !imagesizes_value.is_null())
                    sizes = String::from_deprecated_string(imagesizes_value).release_value_but_fixme_should_propagate_errors();
            }

            // 8. If el is an img element that has a src attribute, then set default source to that attribute's value.
            if (is<HTMLImageElement>(element)) {
                if (auto src_value = element.attribute(HTML::AttributeNames::src); !src_value.is_null())
                    default_source = String::from_deprecated_string(src_value).release_value_but_fixme_should_propagate_errors();
            }

            // 9. Otherwise, if el is a link element that has an href attribute, then set default source to that attribute's value.
            else if (is<HTMLLinkElement>(element)) {
                if (auto href_value = element.attribute(HTML::AttributeNames::href); !href_value.is_null())
                    default_source = String::from_deprecated_string(href_value).release_value_but_fixme_should_propagate_errors();
            }

            // 10. Let el's source set be the result of creating a source set given default source, srcset, and sizes.
            if (is<HTMLImageElement>(element))
                static_cast<HTMLImageElement&>(element).set_source_set(SourceSet::create(element.document(), default_source, srcset, sizes));
            else if (is<HTMLLinkElement>(element))
                TODO();
            return;
        }
        // 2. If child is not a source element, then continue.
        if (!is<HTMLSourceElement>(child))
            continue;

        // 3. If child does not have a srcset attribute, continue to the next child.
        if (!child->has_attribute(HTML::AttributeNames::srcset))
            continue;

        // 4. Parse child's srcset attribute and let the returned source set be source set.
        auto source_set = parse_a_srcset_attribute(child->attribute(HTML::AttributeNames::srcset));

        // 5. If source set has zero image sources, continue to the next child.
        if (source_set.is_empty())
            continue;

        // FIXME: 6. If child has a media attribute, and its value does not match the environment, continue to the next child.

        // 7. Parse child's sizes attribute, and let source set's source size be the returned value.
        source_set.m_source_size = parse_a_sizes_attribute(element.document(), child->attribute(HTML::AttributeNames::sizes));

        // FIXME: 8. If child has a type attribute, and its value is an unknown or unsupported MIME type, continue to the next child.
        if (child->has_attribute(HTML::AttributeNames::type)) {
        }

        // FIXME: 9. If child has width or height attributes, set el's dimension attribute source to child.
        //           Otherwise, set el's dimension attribute source to el.

        // 10. Normalize the source densities of source set.
        source_set.normalize_source_densities();

        // 11. Let el's source set be source set.
        if (is<HTMLImageElement>(element))
            static_cast<HTMLImageElement&>(element).set_source_set(move(source_set));
        else if (is<HTMLLinkElement>(element))
            TODO();

        // 12. Return.
        return;
    }
}

// https://html.spec.whatwg.org/multipage/images.html#select-an-image-source
Optional<ImageSourceAndPixelDensity> HTMLImageElement::select_an_image_source()
{
    // 1. Update the source set for el.
    update_the_source_set(*this);

    // 2. If el's source set is empty, return null as the URL and undefined as the pixel density.
    if (m_source_set.is_empty())
        return {};

    // 3. Return the result of selecting an image from el's source set.
    return m_source_set.select_an_image_source();
}

void HTMLImageElement::set_source_set(SourceSet source_set)
{
    m_source_set = move(source_set);
}

void HTMLImageElement::animate()
{
    auto image_data = m_current_request->image_data();
    if (!image_data) {
        return;
    }

    m_current_frame_index = (m_current_frame_index + 1) % image_data->frame_count();
    auto current_frame_duration = image_data->frame_duration(m_current_frame_index);

    if (current_frame_duration != m_animation_timer->interval()) {
        m_animation_timer->restart(current_frame_duration);
    }

    if (m_current_frame_index == image_data->frame_count() - 1) {
        ++m_loops_completed;
        if (m_loops_completed > 0 && m_loops_completed == image_data->loop_count()) {
            m_animation_timer->stop();
        }
    }

    if (layout_node())
        layout_node()->set_needs_display();
}

// https://html.spec.whatwg.org/multipage/urls-and-fetching.html#lazy-loading-attributes
HTMLImageElement::LazyLoading HTMLImageElement::lazy_loading() const
{
    auto value = attribute(HTML::AttributeNames::loading);
    if (value.equals_ignoring_ascii_case("lazy"sv))
        return LazyLoading::Lazy;
    return LazyLoading::Eager;
}

}
