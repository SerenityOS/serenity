/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2023, Aliaksandr Kalenik <kalenik.aliaksandr@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Debug.h>
#include <AK/LexicalPath.h>
#include <LibGemini/Document.h>
#include <LibGfx/ImageFormats/ImageDecoder.h>
#include <LibMarkdown/Document.h>
#include <LibTextCodec/Decoder.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/DocumentLoading.h>
#include <LibWeb/HTML/Navigable.h>
#include <LibWeb/HTML/NavigationParams.h>
#include <LibWeb/HTML/Parser/HTMLEncodingDetection.h>
#include <LibWeb/HTML/Parser/HTMLParser.h>
#include <LibWeb/Namespace.h>
#include <LibWeb/Platform/ImageCodecPlugin.h>
#include <LibWeb/XML/XMLDocumentBuilder.h>

namespace Web {

static bool build_markdown_document(DOM::Document& document, ByteBuffer const& data)
{
    auto markdown_document = Markdown::Document::parse(data);
    if (!markdown_document)
        return false;

    auto extra_head_contents = R"~~~(
<style>
    .zoomable {
        cursor: zoom-in;
        max-width: 100%;
    }
    .zoomable.zoomed-in {
        cursor: zoom-out;
        max-width: none;
    }
</style>
<script>
    function imageClickEventListener(event) {
        let image = event.target;
        if (image.classList.contains("zoomable")) {
            image.classList.toggle("zoomed-in");
        }
    }
    function processImages() {
        let images = document.querySelectorAll("img");
        let windowWidth = window.innerWidth;
        images.forEach((image) => {
            if (image.naturalWidth > windowWidth) {
                image.classList.add("zoomable");
            } else {
                image.classList.remove("zoomable");
                image.classList.remove("zoomed-in");
            }

            image.addEventListener("click", imageClickEventListener);
        });
    }

    document.addEventListener("load", () => {
        processImages();
    });

    window.addEventListener("resize", () => {
        processImages();
    });
</script>
)~~~"sv;

    auto parser = HTML::HTMLParser::create(document, markdown_document->render_to_html(extra_head_contents), "utf-8");
    parser->run(document.url());
    return true;
}

static bool build_text_document(DOM::Document& document, ByteBuffer const& data)
{
    auto html_element = DOM::create_element(document, HTML::TagNames::html, Namespace::HTML).release_value_but_fixme_should_propagate_errors();
    MUST(document.append_child(html_element));

    auto head_element = DOM::create_element(document, HTML::TagNames::head, Namespace::HTML).release_value_but_fixme_should_propagate_errors();
    MUST(html_element->append_child(head_element));
    auto title_element = DOM::create_element(document, HTML::TagNames::title, Namespace::HTML).release_value_but_fixme_should_propagate_errors();
    MUST(head_element->append_child(title_element));

    auto title_text = document.create_text_node(MUST(String::from_deprecated_string(document.url().basename())));
    MUST(title_element->append_child(title_text));

    auto body_element = DOM::create_element(document, HTML::TagNames::body, Namespace::HTML).release_value_but_fixme_should_propagate_errors();
    MUST(html_element->append_child(body_element));

    auto pre_element = DOM::create_element(document, HTML::TagNames::pre, Namespace::HTML).release_value_but_fixme_should_propagate_errors();
    MUST(body_element->append_child(pre_element));

    MUST(pre_element->append_child(document.create_text_node(String::from_utf8(StringView { data }).release_value_but_fixme_should_propagate_errors())));
    return true;
}

static bool build_image_document(DOM::Document& document, ByteBuffer const& data)
{
    auto image = Platform::ImageCodecPlugin::the().decode_image(data);
    if (!image.has_value() || image->frames.is_empty())
        return false;
    auto const& frame = image->frames[0];
    auto const& bitmap = frame.bitmap;
    if (!bitmap)
        return false;

    auto html_element = DOM::create_element(document, HTML::TagNames::html, Namespace::HTML).release_value_but_fixme_should_propagate_errors();
    MUST(document.append_child(html_element));

    auto head_element = DOM::create_element(document, HTML::TagNames::head, Namespace::HTML).release_value_but_fixme_should_propagate_errors();
    MUST(html_element->append_child(head_element));
    auto title_element = DOM::create_element(document, HTML::TagNames::title, Namespace::HTML).release_value_but_fixme_should_propagate_errors();
    MUST(head_element->append_child(title_element));

    auto basename = LexicalPath::basename(document.url().serialize_path());
    auto title_text = document.heap().allocate<DOM::Text>(document.realm(), document, MUST(String::formatted("{} [{}x{}]", basename, bitmap->width(), bitmap->height())));
    MUST(title_element->append_child(*title_text));

    auto body_element = DOM::create_element(document, HTML::TagNames::body, Namespace::HTML).release_value_but_fixme_should_propagate_errors();
    MUST(html_element->append_child(body_element));

    auto image_element = DOM::create_element(document, HTML::TagNames::img, Namespace::HTML).release_value_but_fixme_should_propagate_errors();
    MUST(image_element->set_attribute(HTML::AttributeNames::src, document.url().to_deprecated_string()));
    MUST(body_element->append_child(image_element));

    return true;
}

static bool build_gemini_document(DOM::Document& document, ByteBuffer const& data)
{
    StringView gemini_data { data };
    auto gemini_document = Gemini::Document::parse(gemini_data, document.url());
    DeprecatedString html_data = gemini_document->render_to_html();

    dbgln_if(GEMINI_DEBUG, "Gemini data:\n\"\"\"{}\"\"\"", gemini_data);
    dbgln_if(GEMINI_DEBUG, "Converted to HTML:\n\"\"\"{}\"\"\"", html_data);

    auto parser = HTML::HTMLParser::create(document, html_data, "utf-8");
    parser->run(document.url());
    return true;
}

bool build_xml_document(DOM::Document& document, ByteBuffer const& data)
{
    auto encoding = HTML::run_encoding_sniffing_algorithm(document, data);
    auto decoder = TextCodec::decoder_for(encoding);
    VERIFY(decoder.has_value());
    auto source = decoder->to_utf8(data).release_value_but_fixme_should_propagate_errors();
    XML::Parser parser(source, { .resolve_external_resource = resolve_xml_resource });
    XMLDocumentBuilder builder { document };
    auto result = parser.parse_with_listener(builder);
    return !result.is_error() && !builder.has_error();
}

static bool build_video_document(DOM::Document& document)
{
    auto html_element = DOM::create_element(document, HTML::TagNames::html, Namespace::HTML).release_value_but_fixme_should_propagate_errors();
    MUST(document.append_child(html_element));

    auto head_element = DOM::create_element(document, HTML::TagNames::head, Namespace::HTML).release_value_but_fixme_should_propagate_errors();
    MUST(html_element->append_child(head_element));

    auto body_element = DOM::create_element(document, HTML::TagNames::body, Namespace::HTML).release_value_but_fixme_should_propagate_errors();
    MUST(html_element->append_child(body_element));

    auto video_element = DOM::create_element(document, HTML::TagNames::video, Namespace::HTML).release_value_but_fixme_should_propagate_errors();
    MUST(video_element->set_attribute(HTML::AttributeNames::src, document.url().to_deprecated_string()));
    MUST(video_element->set_attribute(HTML::AttributeNames::autoplay, DeprecatedString::empty()));
    MUST(video_element->set_attribute(HTML::AttributeNames::controls, DeprecatedString::empty()));
    MUST(body_element->append_child(video_element));

    return true;
}

static bool build_audio_document(DOM::Document& document)
{
    auto html_element = DOM::create_element(document, HTML::TagNames::html, Namespace::HTML).release_value_but_fixme_should_propagate_errors();
    MUST(document.append_child(html_element));

    auto head_element = DOM::create_element(document, HTML::TagNames::head, Namespace::HTML).release_value_but_fixme_should_propagate_errors();
    MUST(html_element->append_child(head_element));

    auto body_element = DOM::create_element(document, HTML::TagNames::body, Namespace::HTML).release_value_but_fixme_should_propagate_errors();
    MUST(html_element->append_child(body_element));

    auto video_element = DOM::create_element(document, HTML::TagNames::audio, Namespace::HTML).release_value_but_fixme_should_propagate_errors();
    MUST(video_element->set_attribute(HTML::AttributeNames::src, document.url().to_deprecated_string()));
    MUST(video_element->set_attribute(HTML::AttributeNames::autoplay, DeprecatedString::empty()));
    MUST(video_element->set_attribute(HTML::AttributeNames::controls, DeprecatedString::empty()));
    MUST(body_element->append_child(video_element));

    return true;
}

bool parse_document(DOM::Document& document, ByteBuffer const& data)
{
    auto& mime_type = document.content_type();
    if (mime_type == "text/html") {
        auto parser = HTML::HTMLParser::create_with_uncertain_encoding(document, data);
        parser->run(document.url());
        return true;
    }
    if (mime_type.ends_with_bytes("+xml"sv) || mime_type.is_one_of("text/xml", "application/xml"))
        return build_xml_document(document, data);
    if (mime_type.starts_with_bytes("image/"sv))
        return build_image_document(document, data);
    if (mime_type.starts_with_bytes("video/"sv))
        return build_video_document(document);
    if (mime_type.starts_with_bytes("audio/"sv))
        return build_audio_document(document);
    if (mime_type == "text/plain" || mime_type == "application/json")
        return build_text_document(document, data);
    if (mime_type == "text/markdown")
        return build_markdown_document(document, data);
    if (mime_type == "text/gemini")
        return build_gemini_document(document, data);

    return false;
}

static bool is_supported_document_mime_type(StringView mime_type)
{
    if (mime_type == "text/html")
        return true;
    if (mime_type.ends_with("+xml"sv) || mime_type.is_one_of("text/xml", "application/xml"))
        return true;
    if (mime_type.starts_with("image/"sv))
        return true;
    if (mime_type.starts_with("video/"sv))
        return true;
    if (mime_type.starts_with("audio/"sv))
        return true;
    if (mime_type == "text/plain" || mime_type == "application/json")
        return true;
    if (mime_type == "text/markdown")
        return true;
    if (mime_type == "text/gemini")
        return true;
    return false;
}

// https://html.spec.whatwg.org/multipage/browsing-the-web.html#loading-a-document
JS::GCPtr<DOM::Document> load_document(Optional<HTML::NavigationParams> navigation_params)
{
    VERIFY(navigation_params.has_value());

    auto extracted_mime_type = navigation_params->response->header_list()->extract_mime_type().release_value_but_fixme_should_propagate_errors();
    auto mime_type = extracted_mime_type.has_value() ? extracted_mime_type.value().essence().bytes_as_string_view() : StringView {};

    if (!is_supported_document_mime_type(mime_type)) {
        return nullptr;
    }

    auto document = DOM::Document::create_and_initialize(DOM::Document::Type::HTML, "text/html", *navigation_params).release_value_but_fixme_should_propagate_errors();
    document->set_content_type(String::from_utf8(mime_type).release_value_but_fixme_should_propagate_errors());

    auto& realm = document->realm();

    if (navigation_params->response->body()) {
        auto process_body = [navigation_params, document](ByteBuffer bytes) {
            if (!parse_document(*document, bytes)) {
                dbgln("FIXME: Load html page with an error if parsing failed.");
            }
        };

        auto process_body_error = [](auto) {
            dbgln("FIXME: Load html page with an error if read of body failed.");
        };

        navigation_params->response->body()->fully_read(
                                               realm,
                                               move(process_body),
                                               move(process_body_error),
                                               JS::NonnullGCPtr { realm.global_object() })
            .release_value_but_fixme_should_propagate_errors();
    }

    return document;
}

// https://html.spec.whatwg.org/multipage/document-lifecycle.html#read-ua-inline
JS::GCPtr<DOM::Document> create_document_for_inline_content(JS::GCPtr<HTML::Navigable> navigable, Optional<String> navigation_id, StringView content_html)
{
    auto& vm = navigable->vm();

    // 1. Let origin be a new opaque origin.
    HTML::Origin origin {};

    // 2. Let coop be a new cross-origin opener policy.
    auto coop = HTML::CrossOriginOpenerPolicy {};

    // 3. Let coopEnforcementResult be a new cross-origin opener policy enforcement result with
    //    url: response's URL
    //    origin: origin
    //    cross-origin opener policy: coop
    HTML::CrossOriginOpenerPolicyEnforcementResult coop_enforcement_result {
        .url = AK::URL("about:error"), // AD-HOC
        .origin = origin,
        .cross_origin_opener_policy = coop
    };

    // 4. Let navigationParams be a new navigation params with
    //    id: navigationId
    //    request: null
    //    response: a new response
    //    origin: origin
    //    policy container: a new policy container
    //    final sandboxing flag set: an empty set
    //    cross-origin opener policy: coop
    //    COOP enforcement result: coopEnforcementResult
    //    reserved environment: null
    //    navigable: navigable
    //    FIXME: navigation timing type: navTimingType
    //    FIXME: fetch controller: fetch controller
    //    FIXME: commit early hints: null
    auto response = Fetch::Infrastructure::Response::create(vm);
    response->url_list().append(AK::URL("about:error")); // AD-HOC: https://github.com/whatwg/html/issues/9122
    HTML::NavigationParams navigation_params {
        .id = navigation_id,
        .request = {},
        .response = *response,
        .origin = move(origin),
        .policy_container = HTML::PolicyContainer {},
        .final_sandboxing_flag_set = HTML::SandboxingFlagSet {},
        .cross_origin_opener_policy = move(coop),
        .coop_enforcement_result = move(coop_enforcement_result),
        .reserved_environment = {},
        .browsing_context = navigable->active_browsing_context(),
        .navigable = navigable,
    };

    // 5. Let document be the result of creating and initializing a Document object given "html", "text/html", and navigationParams.
    auto document = DOM::Document::create_and_initialize(DOM::Document::Type::HTML, "text/html", navigation_params).release_value_but_fixme_should_propagate_errors();

    // 6. Either associate document with a custom rendering that is not rendered using the normal Document rendering rules, or mutate document until it represents the content the
    //    user agent wants to render.
    auto parser = HTML::HTMLParser::create(document, content_html, "utf-8");
    parser->run(AK::URL("about:error"));

    // 7. Return document.
    return document;
}

}
