#include "RequestManagerSoup.h"
#include <AK/JsonArray.h>
#include <AK/JsonObject.h>
#include <AK/JsonValue.h>
#include <libsoup/soup.h>

NonnullRefPtr<RequestManagerSoup> RequestManagerSoup::create()
{
    return adopt_ref(*new RequestManagerSoup);
}

RequestManagerSoup::RequestManagerSoup()
{
    m_session = soup_session_new();
}

RequestManagerSoup::~RequestManagerSoup()
{
    soup_session_abort(m_session);
    g_object_unref(m_session);
}

RefPtr<Web::ResourceLoaderConnectorRequest> RequestManagerSoup::start_request(
    DeprecatedString const& method, AK::URL const& url,
    HashMap<DeprecatedString, DeprecatedString> const& request_headers,
    ReadonlyBytes request_body, Core::ProxyData const& proxy)
{
    if (!url.scheme().bytes_as_string_view().is_one_of_ignoring_ascii_case("http"sv, "https"sv))
        return nullptr;

    auto request_or_error = RequestSoup::create(m_session, method, url, request_headers, request_body, proxy);
    if (request_or_error.is_error())
        return nullptr;

    return request_or_error.release_value();
}

RequestSoup::RequestSoup(SoupSession* session, SoupMessage* message)
    : m_message(message)
{
    m_cancellable = g_cancellable_new();

    soup_session_send_and_read_async(
        session, message, G_PRIORITY_DEFAULT, m_cancellable,
        +[](GObject* object, GAsyncResult* res, void* user_data) {
            SoupSession* session = SOUP_SESSION(object);
            RequestSoup* self = reinterpret_cast<RequestSoup*>(user_data);

            GError* error = nullptr;
            GBytes* bytes = soup_session_send_and_read_finish(session, res, &error);
            self->complete(bytes, error);
        },
        this);
}

RequestSoup::~RequestSoup()
{
    // FIXME: This this actually OK?
    g_cancellable_cancel(m_cancellable);
    g_object_unref(m_cancellable);
    g_object_unref(m_message);
}

bool RequestSoup::stop()
{
    g_cancellable_cancel(m_cancellable);
    // TODO: What's the return value supposed to be?
    return true;
}

void RequestSoup::complete(GBytes* bytes, GError* error)
{
    if (error) {
        on_buffered_request_finish(false, 0, {}, 0, {});
        g_error_free(error);
        return;
    }

    SoupStatus status_code = soup_message_get_status(m_message);
    ReadonlyBytes body { g_bytes_get_data(bytes, nullptr), g_bytes_get_size(bytes) };

    struct Closure {
        HashMap<DeprecatedString, DeprecatedString, CaseInsensitiveStringTraits> response_headers;
        JsonArray set_cookies;
    };
    Closure closure;

    soup_message_headers_foreach(
        soup_message_get_response_headers(m_message),
        +[](char const* name, char const* value, void* user_data) {
            Closure& closure = *reinterpret_cast<Closure*>(user_data);
            if (!strcasecmp(name, "set-cookie")) {
                closure.set_cookies.must_append(value);
            } else {
                closure.response_headers.set(name, value);
            }
        },
        &closure);

    closure.response_headers.set("Set-Cookie", move(closure.set_cookies).to_deprecated_string());
    on_buffered_request_finish(true, g_bytes_get_size(bytes), move(closure.response_headers), status_code, body);
    // FIXME: Is this it? Are we expected to drop the bytes here?
    g_bytes_unref(bytes);
}

ErrorOr<NonnullRefPtr<RequestSoup>> RequestSoup::create(SoupSession* session,
    DeprecatedString const& method, AK::URL const& url,
    HashMap<DeprecatedString, DeprecatedString> const& request_headers,
    ReadonlyBytes request_body, [[maybe_unused]] Core::ProxyData const& proxy)
{
    char const* soup_method;
    if (method.equals_ignoring_ascii_case("head"sv)) {
        soup_method = SOUP_METHOD_HEAD;
    } else if (method.equals_ignoring_ascii_case("get"sv)) {
        soup_method = SOUP_METHOD_GET;
    } else if (method.equals_ignoring_ascii_case("post"sv)) {
        soup_method = SOUP_METHOD_POST;
    } else if (method.equals_ignoring_ascii_case("put"sv)) {
        soup_method = SOUP_METHOD_PUT;
    } else if (method.equals_ignoring_ascii_case("delete"sv)) {
        soup_method = SOUP_METHOD_DELETE;
    } else {
        soup_method = method.characters();
    }

    SoupMessage* message = soup_message_new(soup_method, url.to_deprecated_string().characters());
    SoupMessageHeaders* soup_headers = soup_message_get_request_headers(message);

    for (auto& it : request_headers) {
        soup_message_headers_append(soup_headers, it.key.characters(), it.value.characters());
    }

    GBytes* body_bytes = g_bytes_new(request_body.data(), request_body.size());
    soup_message_set_request_body_from_bytes(message, nullptr, body_bytes);
    g_bytes_unref(body_bytes);

    return adopt_ref(*new RequestSoup(session, message));
}
