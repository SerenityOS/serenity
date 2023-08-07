/*
 * Copyright (c) 2018-2023, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, the SerenityOS developers.
 * Copyright (c) 2021, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2023, Srikavin Ramkumar <me@srikavin.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ByteBuffer.h>
#include <AK/Debug.h>
#include <AK/URL.h>
#include <LibTextCodec/Decoder.h>
#include <LibWeb/CSS/Parser/Parser.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/Event.h>
#include <LibWeb/Fetch/Fetching/Fetching.h>
#include <LibWeb/Fetch/Infrastructure/FetchAlgorithms.h>
#include <LibWeb/Fetch/Infrastructure/HTTP/Requests.h>
#include <LibWeb/Fetch/Infrastructure/HTTP/Responses.h>
#include <LibWeb/HTML/EventNames.h>
#include <LibWeb/HTML/HTMLLinkElement.h>
#include <LibWeb/HTML/PotentialCORSRequest.h>
#include <LibWeb/Infra/CharacterTypes.h>
#include <LibWeb/Loader/ResourceLoader.h>
#include <LibWeb/Page/Page.h>
#include <LibWeb/Platform/ImageCodecPlugin.h>

namespace Web::HTML {

HTMLLinkElement::HTMLLinkElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
{
}

HTMLLinkElement::~HTMLLinkElement() = default;

void HTMLLinkElement::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    set_prototype(&Bindings::ensure_web_prototype<Bindings::HTMLLinkElementPrototype>(realm, "HTMLLinkElement"));
}

void HTMLLinkElement::inserted()
{
    HTMLElement::inserted();

    // FIXME: Handle alternate stylesheets properly
    if (m_relationship & Relationship::Stylesheet && !(m_relationship & Relationship::Alternate)) {
        // https://html.spec.whatwg.org/multipage/links.html#link-type-stylesheet:fetch-and-process-the-linked-resource
        // The appropriate times to fetch and process this type of link are:
        //  - When the external resource link is created on a link element that is already browsing-context connected.
        //  - When the external resource link's link element becomes browsing-context connected.
        fetch_and_process_linked_resource();
    }

    // FIXME: Follow spec for fetching and processing these attributes as well
    if (m_relationship & Relationship::Preload) {
        // FIXME: Respect the "as" attribute.
        LoadRequest request;
        request.set_url(document().parse_url(attribute(HTML::AttributeNames::href)));
        set_resource(ResourceLoader::the().load_resource(Resource::Type::Generic, request));
    } else if (m_relationship & Relationship::DNSPrefetch) {
        ResourceLoader::the().prefetch_dns(document().parse_url(attribute(HTML::AttributeNames::href)));
    } else if (m_relationship & Relationship::Preconnect) {
        ResourceLoader::the().preconnect(document().parse_url(attribute(HTML::AttributeNames::href)));
    } else if (m_relationship & Relationship::Icon) {
        auto favicon_url = document().parse_url(href());
        auto favicon_request = LoadRequest::create_for_url_on_page(favicon_url, document().page());
        set_resource(ResourceLoader::the().load_resource(Resource::Type::Generic, favicon_request));
    }
}

bool HTMLLinkElement::has_loaded_icon() const
{
    return m_relationship & Relationship::Icon && resource() && resource()->is_loaded() && resource()->has_encoded_data();
}

void HTMLLinkElement::attribute_changed(DeprecatedFlyString const& name, DeprecatedString const& value)
{
    HTMLElement::attribute_changed(name, value);

    // 4.6.7 Link types - https://html.spec.whatwg.org/multipage/links.html#linkTypes
    if (name == HTML::AttributeNames::rel) {
        m_relationship = 0;
        // Keywords are always ASCII case-insensitive, and must be compared as such.
        auto lowercased_value = value.to_lowercase();
        // To determine which link types apply to a link, a, area, or form element,
        // the element's rel attribute must be split on ASCII whitespace.
        // The resulting tokens are the keywords for the link types that apply to that element.
        auto parts = lowercased_value.split_view(Infra::is_ascii_whitespace);
        for (auto& part : parts) {
            if (part == "stylesheet"sv)
                m_relationship |= Relationship::Stylesheet;
            else if (part == "alternate"sv)
                m_relationship |= Relationship::Alternate;
            else if (part == "preload"sv)
                m_relationship |= Relationship::Preload;
            else if (part == "dns-prefetch"sv)
                m_relationship |= Relationship::DNSPrefetch;
            else if (part == "preconnect"sv)
                m_relationship |= Relationship::Preconnect;
            else if (part == "icon"sv)
                m_relationship |= Relationship::Icon;
        }
    }

    // FIXME: Handle alternate stylesheets properly
    if (m_relationship & Relationship::Stylesheet && !(m_relationship & Relationship::Alternate)) {
        if (name == HTML::AttributeNames::disabled && m_loaded_style_sheet)
            document().style_sheets().remove_sheet(*m_loaded_style_sheet);

        // https://html.spec.whatwg.org/multipage/links.html#link-type-stylesheet:fetch-and-process-the-linked-resource
        // The appropriate times to fetch and process this type of link are:
        if (
            // AD-HOC: When the rel attribute changes
            name == AttributeNames::rel ||
            // - When the href attribute of the link element of an external resource link that is already browsing-context connected is changed.
            name == AttributeNames::href ||
            // - When the disabled attribute of the link element of an external resource link that is already browsing-context connected is set, changed, or removed.
            name == AttributeNames::disabled ||
            // - When the crossorigin attribute of the link element of an external resource link that is already browsing-context connected is set, changed, or removed.
            name == AttributeNames::crossorigin
            // FIXME: - When the type attribute of the link element of an external resource link that is already browsing-context connected is set or changed to a value that does not or no longer matches the Content-Type metadata of the previous obtained external resource, if any.
            // FIXME: - When the type attribute of the link element of an external resource link that is already browsing-context connected, but was previously not obtained due to the type attribute specifying an unsupported type, is removed or changed.
        ) {
            fetch_and_process_linked_resource();
        }
    }
}

void HTMLLinkElement::resource_did_fail()
{
    dbgln_if(CSS_LOADER_DEBUG, "HTMLLinkElement: Resource did fail. URL: {}", resource()->url());
    if (m_relationship & Relationship::Preload) {
        dispatch_event(*DOM::Event::create(realm(), HTML::EventNames::error).release_value_but_fixme_should_propagate_errors());
    }
}

void HTMLLinkElement::resource_did_load()
{
    VERIFY(resource());
    if (m_relationship & Relationship::Icon) {
        resource_did_load_favicon();
        m_document_load_event_delayer.clear();
    }
    if (m_relationship & Relationship::Preload) {
        dispatch_event(*DOM::Event::create(realm(), HTML::EventNames::load).release_value_but_fixme_should_propagate_errors());
    }
}

// https://html.spec.whatwg.org/multipage/semantics.html#create-link-options-from-element
HTMLLinkElement::LinkProcessingOptions HTMLLinkElement::create_link_options()
{
    // 1. Let document be el's node document.
    auto& document = this->document();

    // 2. Let options be a new link processing options with
    LinkProcessingOptions options;
    // FIXME: destination                      the result of translating the state of el's as attribute
    // crossorigin                      the state of el's crossorigin content attribute
    options.crossorigin = cors_setting_attribute_from_keyword(
        has_attribute(AttributeNames::crossorigin) ? String::from_deprecated_string(get_attribute(AttributeNames::crossorigin)).release_value_but_fixme_should_propagate_errors()
                                                   : Optional<String> {});
    // FIXME: referrer policy                  the state of el's referrerpolicy content attribute
    // FIXME: source set                       el's source set
    // base URL                         document's URL
    options.base_url = document.url();
    // origin                           document's origin
    options.origin = document.origin();
    // environment                      document's relevant settings object
    options.environment = &document.relevant_settings_object();
    // policy container                 document's policy container
    options.policy_container = document.policy_container();
    // document                         document
    options.document = &document;
    // FIXME: cryptographic nonce metadata     The current value of el's [[CryptographicNonce]] internal slot

    // 3. If el has an href attribute, then set options's href to the value of el's href attribute.
    if (has_attribute(AttributeNames::href))
        options.href = String::from_deprecated_string(get_attribute(AttributeNames::href)).release_value_but_fixme_should_propagate_errors();

    // 4. If el has an integrity attribute, then set options's integrity to the value of el's integrity content attribute.
    if (has_attribute(AttributeNames::integrity))
        options.integrity = String::from_deprecated_string(get_attribute(AttributeNames::integrity)).release_value_but_fixme_should_propagate_errors();

    // 5. If el has a type attribute, then set options's type to the value of el's type attribute.
    if (has_attribute(AttributeNames::type))
        options.type = String::from_deprecated_string(get_attribute(AttributeNames::type)).release_value_but_fixme_should_propagate_errors();

    // FIXME: 6. Assert: options's href is not the empty string, or options's source set is not null.
    //           A link element with neither an href or an imagesrcset does not represent a link.

    // 7. Return options.
    return options;
}

// https://html.spec.whatwg.org/multipage/semantics.html#create-a-link-request
JS::GCPtr<Fetch::Infrastructure::Request> HTMLLinkElement::create_link_request(HTMLLinkElement::LinkProcessingOptions const& options)
{
    // 1. Assert: options's href is not the empty string.

    // FIXME: 2. If options's destination is not a destination, then return null.

    // 3. Parse a URL given options's href, relative to options's base URL. If that fails, then return null. Otherwise, let url be the resulting URL record.
    auto url = options.base_url.complete_url(options.href);
    if (!url.is_valid())
        return nullptr;

    // 4. Let request be the result of creating a potential-CORS request given url, options's destination, and options's crossorigin.
    auto request = create_potential_CORS_request(vm(), url, options.destination, options.crossorigin);

    // 5. Set request's policy container to options's policy container.
    request->set_policy_container(options.policy_container);

    // 6. Set request's integrity metadata to options's integrity.
    request->set_integrity_metadata(options.integrity);

    // 7. Set request's cryptographic nonce metadata to options's cryptographic nonce metadata.
    request->set_cryptographic_nonce_metadata(options.cryptographic_nonce_metadata);

    // 8. Set request's referrer policy to options's referrer policy.
    request->set_referrer_policy(options.referrer_policy);

    // 9. Set request's client to options's environment.
    request->set_client(options.environment);

    // 10. Return request.
    return request;
}

// https://html.spec.whatwg.org/multipage/semantics.html#fetch-and-process-the-linked-resource
void HTMLLinkElement::fetch_and_process_linked_resource()
{
    default_fetch_and_process_linked_resource();
}

// https://html.spec.whatwg.org/multipage/semantics.html#default-fetch-and-process-the-linked-resource
void HTMLLinkElement::default_fetch_and_process_linked_resource()
{
    // https://html.spec.whatwg.org/multipage/semantics.html#the-link-element:attr-link-href-4
    // If both the href and imagesrcset attributes are absent, then the element does not define a link.
    // FIXME: Support imagesrcset attribute
    if (!has_attribute(AttributeNames::href) || href().is_empty())
        return;

    // 1. Let options be the result of creating link options from el.
    auto options = create_link_options();

    // 2. Let request be the result of creating a link request given options.
    auto request = create_link_request(options);

    // 3. If request is null, then return.
    if (request == nullptr) {
        return;
    }

    // FIXME: 4. Set request's synchronous flag.

    // 5. Run the linked resource fetch setup steps, given el and request. If the result is false, then return.
    if (!linked_resource_fetch_setup_steps(*request))
        return;

    // 6. Set request's initiator type to "css" if el's rel attribute contains the keyword stylesheet; "link" otherwise.
    if (m_relationship & Relationship::Stylesheet) {
        request->set_initiator_type(Fetch::Infrastructure::Request::InitiatorType::CSS);
    } else {
        request->set_initiator_type(Fetch::Infrastructure::Request::InitiatorType::Link);
    }

    // 7. Fetch request with processResponseConsumeBody set to the following steps given response response and null, failure, or a byte sequence bodyBytes:
    Fetch::Infrastructure::FetchAlgorithms::Input fetch_algorithms_input {};
    fetch_algorithms_input.process_response_consume_body = [this, hr = options](auto response, auto body_bytes) {
        // FIXME: If the response is CORS cross-origin, we must use its internal response to query any of its data. See:
        //        https://github.com/whatwg/html/issues/9355
        response = response->unsafe_response();

        // 1. Let success be true.
        bool success = true;

        // 2. If either of the following conditions are met:
        // - bodyBytes is null or failure; or
        // - response's status is not an ok status,
        if (body_bytes.template has<Empty>() || body_bytes.template has<Fetch::Infrastructure::FetchAlgorithms::ConsumeBodyFailureTag>() || !Fetch::Infrastructure::is_ok_status(response->status())) {
            // then set success to false.
            success = false;
        }

        // FIXME: 3. Otherwise, wait for the link resource's critical subresources to finish loading.

        // 4. Process the linked resource given el, success, response, and bodyBytes.
        process_linked_resource(success, response, body_bytes);
    };

    Fetch::Fetching::fetch(realm(), *request, Fetch::Infrastructure::FetchAlgorithms::create(vm(), move(fetch_algorithms_input))).release_value_but_fixme_should_propagate_errors();
}

// https://html.spec.whatwg.org/multipage/links.html#link-type-stylesheet:process-the-linked-resource
void HTMLLinkElement::process_stylesheet_resource(bool success, Fetch::Infrastructure::Response const& response, Variant<Empty, Fetch::Infrastructure::FetchAlgorithms::ConsumeBodyFailureTag, ByteBuffer> body_bytes)
{
    // 1. If the resource's Content-Type metadata is not text/css, then set success to false.
    auto extracted_mime_type = response.header_list()->extract_mime_type().release_value_but_fixme_should_propagate_errors();
    if (!extracted_mime_type.has_value() || extracted_mime_type->essence() != "text/css") {
        success = false;
    }

    // FIXME: 2. If el no longer creates an external resource link that contributes to the styling processing model,
    //           or if, since the resource in question was fetched, it has become appropriate to fetch it again, then return.

    // 3. If el has an associated CSS style sheet, remove the CSS style sheet.
    if (m_loaded_style_sheet) {
        document().style_sheets().remove_sheet(*m_loaded_style_sheet);
        m_loaded_style_sheet = nullptr;
    }

    // 4. If success is true, then:
    if (success) {
        // 1. Create a CSS style sheet with the following properties:
        //        type
        //            text/css
        //        location
        //            The resulting URL string determined during the fetch and process the linked resource algorithm.
        //        owner node
        //            element
        //        media
        //            The media attribute of element.
        //        title
        //            The title attribute of element, if element is in a document tree, or the empty string otherwise.
        //        alternate flag
        //            Set if the link is an alternative style sheet and element's explicitly enabled is false; unset otherwise.
        //        origin-clean flag
        //            Set if the resource is CORS-same-origin; unset otherwise.
        //        parent CSS style sheet
        //        owner CSS rule
        //            null
        //        disabled flag
        //            Left at its default value.
        //        CSS rules
        //          Left uninitialized.
        //
        // The CSS environment encoding is the result of running the following steps: [CSSSYNTAX]
        //     1. If the element has a charset attribute, get an encoding from that attribute's value. If that succeeds, return the resulting encoding. [ENCODING]
        //     2. Otherwise, return the document's character encoding. [DOM]

        DeprecatedString encoding;
        if (auto charset = attribute(HTML::AttributeNames::charset); !charset.is_null())
            encoding = charset;
        else
            encoding = document().encoding_or_default();

        auto decoder = TextCodec::decoder_for(encoding);

        if (!decoder.has_value()) {
            // If we don't support the encoding yet, let's error out instead of trying to decode it as something it's most likely not.
            dbgln("FIXME: Style sheet encoding '{}' is not supported yet", encoding);
            dispatch_event(*DOM::Event::create(realm(), HTML::EventNames::error).release_value_but_fixme_should_propagate_errors());
        } else {
            auto const& encoded_string = body_bytes.get<ByteBuffer>();
            auto maybe_decoded_string = TextCodec::convert_input_to_utf8_using_given_decoder_unless_there_is_a_byte_order_mark(*decoder, encoded_string);
            if (maybe_decoded_string.is_error()) {
                dbgln("Style sheet {} claimed to be '{}' but decoding failed", response.url().value_or(AK::URL()), encoding);
                dispatch_event(*DOM::Event::create(realm(), HTML::EventNames::error).release_value_but_fixme_should_propagate_errors());
            } else {
                auto const decoded_string = maybe_decoded_string.release_value();
                m_loaded_style_sheet = parse_css_stylesheet(CSS::Parser::ParsingContext(document(), *response.url()), decoded_string);

                if (m_loaded_style_sheet) {
                    m_loaded_style_sheet->set_owner_node(this);
                    m_loaded_style_sheet->set_media(attribute(HTML::AttributeNames::media));
                    document().style_sheets().add_sheet(*m_loaded_style_sheet);
                } else {
                    dbgln_if(CSS_LOADER_DEBUG, "HTMLLinkElement: Failed to parse stylesheet: {}", resource()->url());
                }

                // 2. Fire an event named load at el.
                dispatch_event(*DOM::Event::create(realm(), HTML::EventNames::load).release_value_but_fixme_should_propagate_errors());
            }
        }
    }
    // 5. Otherwise, fire an event named error at el.
    else {
        dispatch_event(*DOM::Event::create(realm(), HTML::EventNames::error).release_value_but_fixme_should_propagate_errors());
    }

    // FIXME: 6. If el contributes a script-blocking style sheet, then:
    //     FIXME: 1. Assert: el's node document's script-blocking style sheet counter is greater than 0.
    //     FIXME: 2. Decrement el's node document's script-blocking style sheet counter by 1.

    // 7. Unblock rendering on el.
    m_document_load_event_delayer.clear();
}

// https://html.spec.whatwg.org/multipage/semantics.html#process-the-linked-resource
void HTMLLinkElement::process_linked_resource(bool success, Fetch::Infrastructure::Response const& response, Variant<Empty, Fetch::Infrastructure::FetchAlgorithms::ConsumeBodyFailureTag, ByteBuffer> body_bytes)
{
    if (m_relationship & Relationship::Stylesheet)
        process_stylesheet_resource(success, response, body_bytes);
}

// https://html.spec.whatwg.org/multipage/semantics.html#linked-resource-fetch-setup-steps
bool HTMLLinkElement::linked_resource_fetch_setup_steps(Fetch::Infrastructure::Request& request)
{
    if (m_relationship & Relationship::Stylesheet)
        return stylesheet_linked_resource_fetch_setup_steps(request);

    return true;
}

// https://html.spec.whatwg.org/multipage/links.html#link-type-stylesheet:linked-resource-fetch-setup-steps
bool HTMLLinkElement::stylesheet_linked_resource_fetch_setup_steps(Fetch::Infrastructure::Request& request)
{
    // 1. If el's disabled attribute is set, then return false.
    if (has_attribute(AttributeNames::disabled))
        return false;
    // FIXME: 2. If el contributes a script-blocking style sheet, increment el's node document's script-blocking style sheet counter by 1.

    // 3. If el's media attribute's value matches the environment and el is potentially render-blocking, then block rendering on el.
    // FIXME: Check media attribute value.
    m_document_load_event_delayer.emplace(document());

    // 4. If el is currently render-blocking, then set request's render-blocking to true.
    // FIXME: Check if el is currently render-blocking.
    request.set_render_blocking(true);

    // 5. Return true.
    return true;
}

void HTMLLinkElement::resource_did_load_favicon()
{
    VERIFY(m_relationship & (Relationship::Icon));
    if (!resource()->has_encoded_data()) {
        dbgln_if(SPAM_DEBUG, "Favicon downloaded, no encoded data");
        return;
    }

    dbgln_if(SPAM_DEBUG, "Favicon downloaded, {} bytes from {}", resource()->encoded_data().size(), resource()->url());

    document().check_favicon_after_loading_link_resource();
}

bool HTMLLinkElement::load_favicon_and_use_if_window_is_active()
{
    if (!has_loaded_icon())
        return false;

    RefPtr<Gfx::Bitmap> favicon_bitmap;
    auto decoded_image = Platform::ImageCodecPlugin::the().decode_image(resource()->encoded_data());
    if (!decoded_image.has_value() || decoded_image->frames.is_empty()) {
        dbgln("Could not decode favicon {}", resource()->url());
        return false;
    }

    favicon_bitmap = decoded_image->frames[0].bitmap;
    dbgln_if(IMAGE_DECODER_DEBUG, "Decoded favicon, {}", favicon_bitmap->size());

    auto* page = document().page();
    if (!page)
        return favicon_bitmap;

    if (document().browsing_context() == &page->top_level_browsing_context())
        if (favicon_bitmap) {
            page->client().page_did_change_favicon(*favicon_bitmap);
            return true;
        }

    return false;
}

void HTMLLinkElement::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_loaded_style_sheet);
}

}
