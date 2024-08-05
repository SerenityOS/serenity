/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, Thomas Keppler <serenity@tkeppler.de>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Base64.h>
#include <AK/CharacterTypes.h>
#include <AK/GenericLexer.h>
#include <AK/LexicalPath.h>
#include <AK/MaybeOwned.h>
#include <AK/NumberFormat.h>
#include <AK/String.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/EventLoop.h>
#include <LibCore/System.h>
#include <LibFileSystem/FileSystem.h>
#include <LibHTTP/HttpResponse.h>
#include <LibMain/Main.h>
#include <LibProtocol/Request.h>
#include <LibProtocol/RequestClient.h>
#include <LibURL/URL.h>
#include <ctype.h>
#include <stdio.h>

// FIXME: Move this somewhere else when it's needed (e.g. in the Browser)
class ContentDispositionParser {
public:
    ContentDispositionParser(StringView value)
    {
        GenericLexer lexer(value);

        lexer.ignore_while(is_ascii_space);

        if (lexer.consume_specific("inline"sv)) {
            m_kind = Kind::Inline;
            if (!lexer.is_eof())
                m_might_be_wrong = true;
            return;
        }

        if (lexer.consume_specific("attachment"sv)) {
            m_kind = Kind::Attachment;
            if (lexer.consume_specific(";"sv)) {
                lexer.ignore_while(is_ascii_space);
                if (lexer.consume_specific("filename="sv)) {
                    // RFC 2183: "A short (length <= 78 characters)
                    //            parameter value containing only non-`tspecials' characters SHOULD be
                    //            represented as a single `token'."
                    // Some people seem to take this as generic advice of "if it doesn't have special characters,
                    // it's safe to specify as a single token"
                    // So let's just be as lenient as possible.
                    if (lexer.next_is('"'))
                        m_filename = lexer.consume_quoted_string();
                    else
                        m_filename = lexer.consume_until(is_any_of("()<>@,;:\\\"/[]?= "sv));
                } else {
                    m_might_be_wrong = true;
                }
            }
            return;
        }

        if (lexer.consume_specific("form-data"sv)) {
            m_kind = Kind::FormData;
            while (lexer.consume_specific(";"sv)) {
                lexer.ignore_while(is_ascii_space);
                if (lexer.consume_specific("name="sv)) {
                    m_name = lexer.consume_quoted_string();
                } else if (lexer.consume_specific("filename="sv)) {
                    if (lexer.next_is('"'))
                        m_filename = lexer.consume_quoted_string();
                    else
                        m_filename = lexer.consume_until(is_any_of("()<>@,;:\\\"/[]?= "sv));
                } else {
                    m_might_be_wrong = true;
                }
            }

            return;
        }

        // FIXME: Support 'filename*'
        m_might_be_wrong = true;
    }

    enum class Kind {
        Inline,
        Attachment,
        FormData,
    };

    StringView filename() const { return m_filename; }
    StringView name() const { return m_name; }
    Kind kind() const { return m_kind; }
    bool might_be_wrong() const { return m_might_be_wrong; }

private:
    StringView m_filename;
    StringView m_name;
    Kind m_kind { Kind::Inline };
    bool m_might_be_wrong { false };
};

/// Wraps a stream to silently ignore writes when the condition isn't true.
template<typename ConditionT>
class ConditionalOutputStream final : public Stream {
public:
    ConditionalOutputStream(ConditionT&& condition, MaybeOwned<Stream> stream)
        : m_stream(move(stream))
        , m_condition(condition)
    {
    }

    virtual ErrorOr<Bytes> read_some(Bytes) override
    {
        return Error::from_errno(EBADF);
    }

    virtual ErrorOr<size_t> write_some(ReadonlyBytes bytes) override
    {
        // Pretend that we wrote the whole buffer if the condition is untrue.
        if (!m_condition())
            return bytes.size();

        return m_stream->write_some(bytes);
    }

    virtual bool is_eof() const override
    {
        return true;
    }

    virtual bool is_open() const override
    {
        return m_stream->is_open();
    }

    virtual void close() override
    {
    }

private:
    MaybeOwned<Stream> m_stream;
    ConditionT m_condition;
};

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    StringView url_str;
    bool save_at_provided_name = false;
    bool should_follow_url = false;
    bool verbose_output = false;
    StringView data;
    StringView proxy_spec;
    ByteString method = "GET";
    StringView method_override;
    HTTP::HeaderMap request_headers;
    String credentials;

    Core::ArgsParser args_parser;
    args_parser.set_general_help(
        "Request a file from an arbitrary URL. This command uses RequestServer, "
        "and thus supports at least http, https, and gemini.");
    args_parser.add_option(save_at_provided_name, "Write to a file named as the remote file", nullptr, 'O');
    args_parser.add_option(data, "(HTTP only) Send the provided data via an HTTP POST request", "data", 'd', "data");
    args_parser.add_option(method_override, "(HTTP only) HTTP method to use for the request (eg, GET, POST, etc)", "method", 'm', "method");
    args_parser.add_option(should_follow_url, "(HTTP only) Follow the Location header if a 3xx status is encountered", "follow", 'l');
    args_parser.add_option(Core::ArgsParser::Option {
        .argument_mode = Core::ArgsParser::OptionArgumentMode::Required,
        .help_string = "Add a header entry to the request",
        .long_name = "header",
        .short_name = 'H',
        .value_name = "key:value",
        .accept_value = [&](StringView header) {
            auto split = header.find(':');
            if (!split.has_value())
                return false;
            request_headers.set(header.substring_view(0, split.value()), header.substring_view(split.value() + 1));
            return true;
        } });
    args_parser.add_option(Core::ArgsParser::Option {
        .argument_mode = Core::ArgsParser::OptionArgumentMode::Required,
        .help_string = "(HTTP only) Provide basic authentication credentials",
        .long_name = "auth",
        .short_name = 'u',
        .value_name = "username:password",
        .accept_value = [&](StringView input) {
            if (!input.contains(':'))
                return false;

            // NOTE: Input is explicitly not trimmed, but instead taken in raw;
            //       Space prepended usernames and appended passwords might be legal in the user's context.
            auto maybe_credentials = String::from_utf8(input);
            if (maybe_credentials.is_error())
                return false;

            credentials = maybe_credentials.release_value();
            return true;
        } });
    args_parser.add_option(proxy_spec, "Specify a proxy server to use for this request (proto://ip:port)", "proxy", 'p', "proxy");
    args_parser.add_option(verbose_output, "(HTTP only) Log request and response metadata", "verbose", 'v');
    args_parser.add_positional_argument(url_str, "URL to download from", "url");
    args_parser.parse(arguments);

    // If writing to a file was requested, we'll open a new file descriptor with the same number later.
    // Until then, we just clone the stdout file descriptor, because we shouldn't be reopening the actual stdout.
    int const output_fd = TRY(Core::System::dup(STDOUT_FILENO));

    if (!method_override.is_empty()) {
        method = method_override;
    } else if (!data.is_empty()) {
        method = "POST";
        // FIXME: Content-Type?
    }

    URL::URL url(url_str);
    if (!url.is_valid()) {
        warnln("'{}' is not a valid URL", url_str);
        return 1;
    }

    bool const is_http_url = url.scheme().is_one_of("http"sv, "https"sv);

    Core::ProxyData proxy_data {};
    if (!proxy_spec.is_empty())
        proxy_data = TRY(Core::ProxyData::parse_url(proxy_spec));

    Core::EventLoop loop;
    bool received_actual_headers = false;
    bool should_save_stream_data = false;
    bool following_url = false;

    u64 previous_downloaded_size = 0;
    u64 current_bytes_per_second_speed = 0;
    u32 const report_time_in_ms = 100;
    u32 const speed_update_time_in_ms = 1000;

    auto previous_report_time = MonotonicTime::now();
    auto previous_speed_update_time = previous_report_time;

    RefPtr<Protocol::Request> request;
    auto protocol_client = TRY(Protocol::RequestClient::try_create());
    auto output_stream = ConditionalOutputStream { [&] { return should_save_stream_data; }, TRY(Core::File::adopt_fd(output_fd, Core::File::OpenMode::Write)) };

    // https://httpwg.org/specs/rfc9110.html#authentication
    auto const has_credentials = !credentials.is_empty();
    auto const has_manual_authorization_header = request_headers.contains("Authorization");
    if (is_http_url && has_credentials && !has_manual_authorization_header) {
        // 11.2. Authentication Parameters
        // The authentication scheme is followed by additional information necessary for achieving authentication via
        // that scheme as (...) or a single sequence of characters capable of holding base64-encoded information.
        auto const encoded_credentials = TRY(encode_base64(credentials.bytes()));
        auto const authorization = TRY(String::formatted("Basic {}", encoded_credentials));
        request_headers.set("Authorization", authorization.to_byte_string());
    } else {
        if (is_http_url && has_credentials && has_manual_authorization_header)
            warnln("* Skipping encoding provided authorization, manual header present.");
        if (!is_http_url && has_credentials)
            warnln("* Skipping adding Authorization header, request was not for the HTTP protocol.");
    }

    auto update_progress = [&](Optional<u64> maybe_total_size, u64 downloaded_size, bool force_update) {
        auto current_time = MonotonicTime::now();
        if (!force_update && (current_time - previous_report_time).to_milliseconds() < report_time_in_ms)
            return;

        previous_report_time = current_time;
        warn("\r\033[2K");
        if (maybe_total_size.has_value()) {
            warn("\033]9;{};{};\033\\", downloaded_size, maybe_total_size.value());
            warn("Download progress: {} / {}", human_readable_size(downloaded_size), human_readable_size(maybe_total_size.value()));
        } else {
            warn("Download progress: {} / ???", human_readable_size(downloaded_size));
        }

        auto time_diff_ms = (current_time - previous_speed_update_time).to_milliseconds();
        if ((force_update && previous_downloaded_size == 0) || time_diff_ms > speed_update_time_in_ms) {
            auto size_diff = downloaded_size - previous_downloaded_size;
            previous_speed_update_time = current_time;
            previous_downloaded_size = downloaded_size;
            current_bytes_per_second_speed = size_diff * 1000 / time_diff_ms;
        }

        if (previous_downloaded_size == 0)
            warn(" at --.-B/s");
        else
            warn(" at {}/s", human_readable_size(current_bytes_per_second_speed));
    };

    Function<void()> setup_request = [&] {
        if (!request) {
            warnln("Failed to start request for '{}'", url_str);
            exit(1);
        }

        if (verbose_output && is_http_url) {
            warnln("* Setting up request");
            warnln("> Method={}, URL={}", method, url);
            for (auto const& header : request_headers.headers()) {
                warnln("> {}: {}", header.name, header.value);
            }
        }

        auto on_headers_received = [&](auto& response_headers, auto status_code) {
            if (received_actual_headers)
                return;
            dbgln("Received headers! response code = {}", status_code.value_or(0));
            received_actual_headers = true; // And not trailers!
            should_save_stream_data = true;

            if (verbose_output && is_http_url) {
                warnln("* Received headers");
                auto const value = status_code.value_or(0);
                auto const reason_phrase = (value != 0)
                    ? HTTP::HttpResponse::reason_phrase_for_code(value)
                    : "UNKNOWN"sv;
                warnln("< Code={}, Reason={}", value, reason_phrase);
                for (auto const& header : response_headers.headers()) {
                    warnln("< {}: {}", header.name, header.value);
                }
            }

            if (!following_url && save_at_provided_name) {
                ByteString output_name;
                if (auto content_disposition = response_headers.get("Content-Disposition"); content_disposition.has_value()) {
                    auto& value = content_disposition.value();
                    ContentDispositionParser parser(value);
                    output_name = parser.filename();
                }

                if (output_name.is_empty())
                    output_name = URL::percent_decode(url.serialize_path());

                LexicalPath path { output_name };
                output_name = path.basename();

                // The URL didn't have a name component, e.g. 'serenityos.org'
                if (output_name.is_empty() || output_name == "/") {
                    int i = -1;
                    do {
                        output_name = url.serialized_host().release_value_but_fixme_should_propagate_errors().to_byte_string();
                        if (i > -1)
                            output_name = ByteString::formatted("{}.{}", output_name, i);
                        ++i;
                    } while (FileSystem::exists(output_name));
                }

                int target_file_fd = open(output_name.characters(), O_WRONLY | O_CREAT | O_TRUNC, 0644);
                if (target_file_fd < 0) {
                    perror("target file open");
                    loop.quit(1);
                    return;
                }

                if (dup2(target_file_fd, output_fd) < 0) {
                    perror("target file dup2");
                    loop.quit(1);
                    return;
                }

                if (close(target_file_fd) < 0) {
                    perror("target file close");
                    loop.quit(1);
                    return;
                }
            }

            auto status_code_value = status_code.value_or(0);
            if (should_follow_url && status_code_value >= 300 && status_code_value < 400) {
                if (auto location = response_headers.get("Location"); location.has_value()) {
                    auto was_following_url = following_url;
                    following_url = true;
                    received_actual_headers = false;
                    should_save_stream_data = false;
                    request->stop();

                    Core::deferred_invoke([&, was_following_url, url = location.value()] {
                        warnln("{}Following to {}", was_following_url ? "" : "\n", url);
                        request = protocol_client->start_request(method, url, request_headers, ReadonlyBytes {}, proxy_data);
                        setup_request();
                    });
                }
            } else {
                following_url = false;

                if (status_code_value >= 400)
                    warnln("Request returned error {}", status_code_value);
            }
        };

        auto on_data_received = [&](auto data) {
            output_stream.write_until_depleted(data).release_value_but_fixme_should_propagate_errors();
        };

        auto on_finished = [&](bool success, u64 total_size) {
            if (following_url)
                return;

            if (success)
                update_progress(total_size, total_size, true);

            warn("\033]9;-1;\033\\");
            warnln();
            if (!success)
                warnln("Request failed :(");
            loop.quit(0);
        };

        request->set_unbuffered_request_callbacks(move(on_headers_received), move(on_data_received), move(on_finished));

        request->on_progress = [&](Optional<u64> maybe_total_size, u64 downloaded_size) {
            update_progress(move(maybe_total_size), downloaded_size, false);
        };
    };

    request = protocol_client->start_request(method, url, request_headers, data.bytes(), proxy_data);
    setup_request();

    dbgln("started request with id {}", request->id());

    return loop.exec();
}
