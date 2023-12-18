/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2023, Aliaksandr Kalenik <kalenik.aliaksandr@gmail.com>
 * Copyright (c) 2023, Sam Atkins <atkinssj@serenityos.org>
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
#include <LibWeb/Loader/GeneratedPagesLoader.h>
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

    auto title_text = document.create_text_node(MUST(String::from_byte_string(document.url().basename())));
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
    MUST(image_element->set_attribute(HTML::AttributeNames::src, MUST(document.url().to_string())));
    MUST(body_element->append_child(image_element));

    return true;
}

static bool build_gemini_document(DOM::Document& document, ByteBuffer const& data)
{
    StringView gemini_data { data };
    auto gemini_document = Gemini::Document::parse(gemini_data, document.url());
    ByteString html_data = gemini_document->render_to_html();

    dbgln_if(GEMINI_DEBUG, "Gemini data:\n\"\"\"{}\"\"\"", gemini_data);
    dbgln_if(GEMINI_DEBUG, "Converted to HTML:\n\"\"\"{}\"\"\"", html_data);

    auto parser = HTML::HTMLParser::create(document, html_data, "utf-8");
    parser->run(document.url());
    return true;
}

bool build_xml_document(DOM::Document& document, ByteBuffer const& data, Optional<String> content_encoding)
{
    Optional<TextCodec::Decoder&> decoder;
    // The actual HTTP headers and other metadata, not the headers as mutated or implied by the algorithms given in this specification,
    // are the ones that must be used when determining the character encoding according to the rules given in the above specifications.
    if (content_encoding.has_value())
        decoder = TextCodec::decoder_for(*content_encoding);
    if (!decoder.has_value()) {
        auto encoding = HTML::run_encoding_sniffing_algorithm(document, data);
        decoder = TextCodec::decoder_for(encoding);
    }
    VERIFY(decoder.has_value());
    // Well-formed XML documents contain only properly encoded characters
    if (!decoder->validate(data))
        return false;
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
    MUST(video_element->set_attribute(HTML::AttributeNames::src, MUST(document.url().to_string())));
    MUST(video_element->set_attribute(HTML::AttributeNames::autoplay, String {}));
    MUST(video_element->set_attribute(HTML::AttributeNames::controls, String {}));
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
    MUST(video_element->set_attribute(HTML::AttributeNames::src, MUST(document.url().to_string())));
    MUST(video_element->set_attribute(HTML::AttributeNames::autoplay, String {}));
    MUST(video_element->set_attribute(HTML::AttributeNames::controls, String {}));
    MUST(body_element->append_child(video_element));

    return true;
}

bool parse_document(DOM::Document& document, ByteBuffer const& data, [[maybe_unused]] Optional<String> content_encoding)
{
    auto& mime_type = document.content_type();
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

// https://html.spec.whatwg.org/multipage/document-lifecycle.html#navigate-html
static WebIDL::ExceptionOr<JS::NonnullGCPtr<DOM::Document>> load_html_document(HTML::NavigationParams& navigation_params)
{
    // To load an HTML document, given navigation params navigationParams:

    // 1. Let document be the result of creating and initializing a Document object given "html", "text/html", and navigationParams.
    auto document = TRY(DOM::Document::create_and_initialize(DOM::Document::Type::HTML, "text/html"_string, navigation_params));

    // 2. If document's URL is about:blank, then populate with html/head/body given document.
    // FIXME: The additional check for a non-empty body fixes issues with loading javascript urls in iframes, which
    //        default to an "about:blank" url. Is this a spec bug?
    if (document->url_string() == "about:blank"_string
        && navigation_params.response->body()->length().value_or(0) == 0) {
        TRY(document->populate_with_html_head_and_body());
        // Nothing else is added to the document, so mark it as loaded.
        HTML::HTMLParser::the_end(document);
    }

    // 3. Otherwise, create an HTML parser and associate it with the document.
    //    Each task that the networking task source places on the task queue while fetching runs must then fill the
    //    parser's input byte stream with the fetched bytes and cause the HTML parser to perform the appropriate
    //    processing of the input stream.
    //    The first task that the networking task source places on the task queue while fetching runs must process link
    //    headers given document, navigationParams's response, and "media", after the task has been processed by the
    //    HTML parser.
    //    Before any script execution occurs, the user agent must wait for scripts may run for the newly-created
    //    document to be true for document.
    //    When no more bytes are available, the user agent must queue a global task on the networking task source given
    //    document's relevant global object to have the parser to process the implied EOF character, which eventually
    //    causes a load event to be fired.
    else {
        // FIXME: Parse as we receive the document data, instead of waiting for the whole document to be fetched first.
        auto process_body = [document, url = navigation_params.response->url().value()](ByteBuffer data) {
            auto parser = HTML::HTMLParser::create_with_uncertain_encoding(document, data);
            parser->run(url);
        };

        auto process_body_error = [](auto) {
            dbgln("FIXME: Load html page with an error if read of body failed.");
        };

        auto& realm = document->realm();
        TRY(navigation_params.response->body()->fully_read(realm, move(process_body), move(process_body_error), JS::NonnullGCPtr { realm.global_object() }));
    }

    // 4. Return document.
    return document;
}

// https://html.spec.whatwg.org/multipage/document-lifecycle.html#read-xml
static WebIDL::ExceptionOr<JS::NonnullGCPtr<DOM::Document>> load_xml_document(HTML::NavigationParams& navigation_params, MimeSniff::MimeType type)
{
    // When faced with displaying an XML file inline, provided navigation params navigationParams and a string type, user agents
    // must follow the requirements defined in XML and Namespaces in XML, XML Media Types, DOM, and other relevant specifications
    // to create and initialize a Document object document, given "xml", type, and navigationParams, and return that Document.
    // They must also create a corresponding XML parser. [XML] [XMLNS] [RFC7303] [DOM]
    //
    // Note: At the time of writing, the XML specification community had not actually yet specified how XML and the DOM interact.
    //
    // The first task that the networking task source places on the task queue while fetching runs must process link headers
    // given document, navigationParams's response, and "media", after the task has been processed by the XML parser.
    //
    // The actual HTTP headers and other metadata, not the headers as mutated or implied by the algorithms given in this
    // specification, are the ones that must be used when determining the character encoding according to the rules given in the
    // above specifications. Once the character encoding is established, the document's character encoding must be set to that
    // character encoding.
    //
    // Before any script execution occurs, the user agent must wait for scripts may run for the newly-created document to be
    // true for the newly-created Document.
    //
    // Once parsing is complete, the user agent must set document's during-loading navigation ID for WebDriver BiDi to null.
    //
    // Note: For HTML documents this is reset when parsing is complete, after firing the load event.
    //
    // Error messages from the parse process (e.g., XML namespace well-formedness errors) may be reported inline by mutating
    // the Document.

    // FIXME: Actually follow the spec! This is just the ad-hoc code we had before, modified somewhat.

    auto document = TRY(DOM::Document::create_and_initialize(DOM::Document::Type::XML, "application/xhtml+xml"_string, navigation_params));

    Optional<String> content_encoding;
    if (auto maybe_encoding = type.parameters().get("charset"sv); maybe_encoding.has_value())
        content_encoding = maybe_encoding.value();

    auto process_body = [document, url = navigation_params.response->url().value(), content_encoding = move(content_encoding)](ByteBuffer data) {
        Optional<TextCodec::Decoder&> decoder;
        // The actual HTTP headers and other metadata, not the headers as mutated or implied by the algorithms given in this specification,
        // are the ones that must be used when determining the character encoding according to the rules given in the above specifications.
        if (content_encoding.has_value())
            decoder = TextCodec::decoder_for(*content_encoding);
        if (!decoder.has_value()) {
            auto encoding = HTML::run_encoding_sniffing_algorithm(document, data);
            decoder = TextCodec::decoder_for(encoding);
        }
        VERIFY(decoder.has_value());
        // Well-formed XML documents contain only properly encoded characters
        if (!decoder->validate(data)) {
            // FIXME: Insert error message into the document.
            dbgln("XML Document contains improperly-encoded characters");
            return;
        }
        auto source = decoder->to_utf8(data);
        if (source.is_error()) {
            // FIXME: Insert error message into the document.
            dbgln("Failed to decode XML document: {}", source.error());
            return;
        }
        XML::Parser parser(source.value(), { .resolve_external_resource = resolve_xml_resource });
        XMLDocumentBuilder builder { document };
        auto result = parser.parse_with_listener(builder);
        if (result.is_error()) {
            // FIXME: Insert error message into the document.
            dbgln("Failed to parse XML document: {}", result.error());
        }
    };

    auto process_body_error = [](auto) {
        dbgln("FIXME: Load html page with an error if read of body failed.");
    };

    auto& realm = document->realm();
    TRY(navigation_params.response->body()->fully_read(realm, move(process_body), move(process_body_error), JS::NonnullGCPtr { realm.global_object() }));

    return document;
}

// https://html.spec.whatwg.org/multipage/browsing-the-web.html#loading-a-document
JS::GCPtr<DOM::Document> load_document(HTML::NavigationParams navigation_params)
{
    // To load a document given navigation params navigationParams, source snapshot params sourceSnapshotParams,
    // and origin initiatorOrigin, perform the following steps. They return a Document or null.

    // 1. Let type be the computed type of navigationParams's response.
    auto extracted_mime_type = navigation_params.response->header_list()->extract_mime_type().release_value_but_fixme_should_propagate_errors();
    if (!extracted_mime_type.has_value())
        return nullptr;
    auto type = extracted_mime_type.release_value();

    VERIFY(navigation_params.response->body());

    // 2. If the user agent has been configured to process resources of the given type using some mechanism other than
    //    rendering the content in a navigable, then skip this step.
    //    Otherwise, if the type is one of the following types:

    // -> an HTML MIME type
    if (type.is_html()) {
        // Return the result of loading an HTML document, given navigationParams.
        return load_html_document(navigation_params).release_value_but_fixme_should_propagate_errors();
    }

    // -> an XML MIME type that is not an explicitly supported XML MIME type
    //    FIXME: that is not an explicitly supported XML MIME type
    if (type.is_xml()) {
        // Return the result of loading an XML document given navigationParams and type.
        return load_xml_document(navigation_params, type).release_value_but_fixme_should_propagate_errors();
    }

    // -> a JavaScript MIME type
    // -> a JSON MIME type that is not an explicitly supported JSON MIME type
    // -> "text/css"
    // -> "text/plain"
    // -> "text/vtt"
    if (type.is_javascript()
        || type.is_json()
        || type.essence() == "text/css"_string
        || type.essence() == "text/plain"_string
        || type.essence() == "text/vtt"_string) {
        // FIXME: Return the result of loading a text document given navigationParams and type.
    }

    // -> "multipart/x-mixed-replace"
    if (type.essence() == "multipart/x-mixed-replace"_string) {
        // FIXME: Return the result of loading a multipart/x-mixed-replace document, given navigationParams,
        //        sourceSnapshotParams, and initiatorOrigin.
    }

    // -> A supported image, video, or audio type
    if (type.is_image()
        || type.is_audio_or_video()) {
        // FIXME: Return the result of loading a media document given navigationParams and type.
    }

    // -> "application/pdf"
    // -> "text/pdf"
    if (type.essence() == "application/pdf"_string
        || type.essence() == "text/pdf"_string) {
        // FIXME: If the user agent's PDF viewer supported is true, return the result of creating a document for inline
        //        content that doesn't have a DOM given navigationParams's navigable.
    }

    // Otherwise, proceed onward.

    // FIXME: 3. If, given type, the new resource is to be handled by displaying some sort of inline content, e.g., a
    //        native rendering of the content or an error message because the specified type is not supported, then
    //        return the result of creating a document for inline content that doesn't have a DOM given navigationParams's
    //        navigable, navigationParams's id, and navigationParams's navigation timing type.

    // FIXME: 4. Otherwise, the document's type is such that the resource will not affect navigationParams's navigable,
    //        e.g., because the resource is to be handed to an external application or because it is an unknown type
    //        that will be processed as a download. Hand-off to external software given navigationParams's response,
    //        navigationParams's navigable, navigationParams's final sandboxing flag set, sourceSnapshotParams's has
    //        transient activation, and initiatorOrigin.

    // FIXME: Start of old, ad-hoc code

    if (!is_supported_document_mime_type(type.essence()))
        return nullptr;

    auto document = DOM::Document::create_and_initialize(DOM::Document::Type::HTML, "text/html"_string, navigation_params).release_value_but_fixme_should_propagate_errors();
    document->set_content_type(type.essence());

    auto& realm = document->realm();

    if (navigation_params.response->body()) {
        Optional<String> content_encoding = type.parameters().get("charset"sv);
        auto process_body = [document, url = navigation_params.response->url().value(), encoding = move(content_encoding)](ByteBuffer bytes) {
            if (parse_document(*document, bytes, move(encoding)))
                return;
            document->remove_all_children(true);
            auto error_html = load_error_page(url).release_value_but_fixme_should_propagate_errors();
            auto parser = HTML::HTMLParser::create(document, error_html, "utf-8");
            document->set_url(AK::URL("about:error"));
            parser->run();
        };

        auto process_body_error = [](auto) {
            dbgln("FIXME: Load html page with an error if read of body failed.");
        };

        navigation_params.response->body()->fully_read(
                                              realm,
                                              move(process_body),
                                              move(process_body_error),
                                              JS::NonnullGCPtr { realm.global_object() })
            .release_value_but_fixme_should_propagate_errors();
    }

    return document;

    // FIXME: End of old, ad-hoc code

    // 5. Return null.
    return nullptr;
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
    //    navigable: navigable
    //    request: null
    //    response: a new response
    //    origin: origin
    //    fetch controller: null
    //    commit early hints: null
    //    COOP enforcement result: coopEnforcementResult
    //    reserved environment: null
    //    policy container: a new policy container
    //    final sandboxing flag set: an empty set
    //    cross-origin opener policy: coop
    //    FIXME: navigation timing type: navTimingType
    //    about base URL: null
    auto response = Fetch::Infrastructure::Response::create(vm);
    response->url_list().append(AK::URL("about:error")); // AD-HOC: https://github.com/whatwg/html/issues/9122
    HTML::NavigationParams navigation_params {
        .id = navigation_id,
        .navigable = navigable,
        .request = {},
        .response = *response,
        .fetch_controller = nullptr,
        .commit_early_hints = nullptr,
        .coop_enforcement_result = move(coop_enforcement_result),
        .reserved_environment = {},
        .origin = move(origin),
        .policy_container = HTML::PolicyContainer {},
        .final_sandboxing_flag_set = HTML::SandboxingFlagSet {},
        .cross_origin_opener_policy = move(coop),
        .about_base_url = {},
    };

    // 5. Let document be the result of creating and initializing a Document object given "html", "text/html", and navigationParams.
    auto document = DOM::Document::create_and_initialize(DOM::Document::Type::HTML, "text/html"_string, navigation_params).release_value_but_fixme_should_propagate_errors();

    // 6. Either associate document with a custom rendering that is not rendered using the normal Document rendering rules, or mutate document until it represents the content the
    //    user agent wants to render.
    auto parser = HTML::HTMLParser::create(document, content_html, "utf-8");
    document->set_url(AK::URL("about:error"));
    parser->run();

    // 7. Return document.
    return document;
}

}
