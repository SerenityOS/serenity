/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/FileStream.h>
#include <AK/GenericLexer.h>
#include <AK/LexicalPath.h>
#include <AK/NumberFormat.h>
#include <AK/URL.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/EventLoop.h>
#include <LibCore/File.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>
#include <LibProtocol/Request.h>
#include <LibProtocol/RequestClient.h>
#include <ctype.h>
#include <stdio.h>

// FIXME: Move this somewhere else when it's needed (e.g. in the Browser)
class ContentDispositionParser {
public:
    ContentDispositionParser(StringView value)
    {
        GenericLexer lexer(value);

        lexer.ignore_while(isspace);

        if (lexer.consume_specific("inline")) {
            m_kind = Kind::Inline;
            if (!lexer.is_eof())
                m_might_be_wrong = true;
            return;
        }

        if (lexer.consume_specific("attachment")) {
            m_kind = Kind::Attachment;
            if (lexer.consume_specific(";")) {
                lexer.ignore_while(isspace);
                if (lexer.consume_specific("filename=")) {
                    // RFC 2183: "A short (length <= 78 characters)
                    //            parameter value containing only non-`tspecials' characters SHOULD be
                    //            represented as a single `token'."
                    // Some people seem to take this as generic advice of "if it doesn't have special characters,
                    // it's safe to specify as a single token"
                    // So let's just be as lenient as possible.
                    if (lexer.next_is('"'))
                        m_filename = lexer.consume_quoted_string();
                    else
                        m_filename = lexer.consume_until(is_any_of("()<>@,;:\\\"/[]?= "));
                } else {
                    m_might_be_wrong = true;
                }
            }
            return;
        }

        if (lexer.consume_specific("form-data")) {
            m_kind = Kind::FormData;
            while (lexer.consume_specific(";")) {
                lexer.ignore_while(isspace);
                if (lexer.consume_specific("name=")) {
                    m_name = lexer.consume_quoted_string();
                } else if (lexer.consume_specific("filename=")) {
                    if (lexer.next_is('"'))
                        m_filename = lexer.consume_quoted_string();
                    else
                        m_filename = lexer.consume_until(is_any_of("()<>@,;:\\\"/[]?= "));
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

template<typename ConditionT>
class ConditionalOutputFileStream final : public OutputFileStream {
public:
    template<typename... Args>
    ConditionalOutputFileStream(ConditionT&& condition, Args... args)
        : OutputFileStream(args...)
        , m_condition(condition)
    {
    }

    ~ConditionalOutputFileStream()
    {
        if (!m_condition())
            return;

        if (!m_buffer.is_empty()) {
            OutputFileStream::write(m_buffer);
            m_buffer.clear();
        }
    }

private:
    size_t write(ReadonlyBytes bytes) override
    {
        if (!m_condition()) {
        write_to_buffer:;
            // FIXME: Propagate errors.
            if (m_buffer.try_append(bytes.data(), bytes.size()).is_error())
                return 0;
            return bytes.size();
        }

        if (!m_buffer.is_empty()) {
            auto size = OutputFileStream::write(m_buffer);
            m_buffer = m_buffer.slice(size, m_buffer.size() - size);
        }

        if (!m_buffer.is_empty())
            goto write_to_buffer;

        return OutputFileStream::write(bytes);
    }

    ConditionT m_condition;
    ByteBuffer m_buffer;
};

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    const char* url_str = nullptr;
    bool save_at_provided_name = false;
    const char* data = nullptr;
    String method = "GET";
    HashMap<String, String, CaseInsensitiveStringTraits> request_headers;

    Core::ArgsParser args_parser;
    args_parser.set_general_help(
        "Request a file from an arbitrary URL. This command uses RequestServer, "
        "and thus supports at least http, https, and gemini.");
    args_parser.add_option(save_at_provided_name, "Write to a file named as the remote file", nullptr, 'O');
    args_parser.add_option(data, "(HTTP only) Send the provided data via an HTTP POST request", "data", 'd', "data");
    args_parser.add_option(Core::ArgsParser::Option {
        .requires_argument = true,
        .help_string = "Add a header entry to the request",
        .long_name = "header",
        .short_name = 'H',
        .value_name = "header-value",
        .accept_value = [&](auto* s) {
            StringView header { s };
            auto split = header.find(':');
            if (!split.has_value())
                return false;
            request_headers.set(header.substring_view(0, split.value()), header.substring_view(split.value() + 1));
            return true;
        } });
    args_parser.add_positional_argument(url_str, "URL to download from", "url");
    args_parser.parse(arguments);

    if (data) {
        method = "POST";
        // FIXME: Content-Type?
    }

    URL url(url_str);
    if (!url.is_valid()) {
        warnln("'{}' is not a valid URL", url_str);
        return 1;
    }

    Core::EventLoop loop;
    auto protocol_client = TRY(Protocol::RequestClient::try_create());

    auto request = protocol_client->start_request(method, url, request_headers, data ? StringView { data }.bytes() : ReadonlyBytes {});
    if (!request) {
        warnln("Failed to start request for '{}'", url_str);
        return 1;
    }

    u32 previous_downloaded_size { 0 };
    u32 previous_midpoint_downloaded_size { 0 };
    timeval prev_time, prev_midpoint_time, current_time, time_diff;
    static constexpr auto download_speed_rolling_average_time_in_ms = 4000;
    gettimeofday(&prev_time, nullptr);

    bool received_actual_headers = false;

    request->on_progress = [&](Optional<u32> maybe_total_size, u32 downloaded_size) {
        warn("\r\033[2K");
        if (maybe_total_size.has_value()) {
            warn("\033]9;{};{};\033\\", downloaded_size, maybe_total_size.value());
            warn("Download progress: {} / {}", human_readable_size(downloaded_size), human_readable_size(maybe_total_size.value()));
        } else {
            warn("Download progress: {} / ???", human_readable_size(downloaded_size));
        }

        gettimeofday(&current_time, nullptr);
        timersub(&current_time, &prev_time, &time_diff);

        auto time_diff_ms = time_diff.tv_sec * 1000 + time_diff.tv_usec / 1000;
        auto size_diff = downloaded_size - previous_downloaded_size;

        warn(" at {}/s", human_readable_size(((float)size_diff / (float)time_diff_ms) * 1000));

        if (time_diff_ms >= download_speed_rolling_average_time_in_ms) {
            previous_downloaded_size = previous_midpoint_downloaded_size;
            prev_time = prev_midpoint_time;
        } else if (time_diff_ms >= download_speed_rolling_average_time_in_ms / 2) {
            previous_midpoint_downloaded_size = downloaded_size;
            prev_midpoint_time = current_time;
        }
    };

    if (save_at_provided_name) {
        request->on_headers_received = [&](auto& response_headers, auto status_code) {
            if (received_actual_headers)
                return;
            dbgln("Received headers! response code = {}", status_code.value_or(0));
            received_actual_headers = true; // And not trailers!
            String output_name;
            if (auto content_disposition = response_headers.get("Content-Disposition"); content_disposition.has_value()) {
                auto& value = content_disposition.value();
                ContentDispositionParser parser(value);
                output_name = parser.filename();
            }

            if (output_name.is_empty())
                output_name = url.path();

            LexicalPath path { output_name };
            output_name = path.basename();

            // The URL didn't have a name component, e.g. 'serenityos.org'
            if (output_name.is_empty() || output_name == "/") {
                int i = -1;
                do {
                    output_name = url.host();
                    if (i > -1)
                        output_name = String::formatted("{}.{}", output_name, i);
                    ++i;
                } while (Core::File::exists(output_name));
            }

            if (freopen(output_name.characters(), "w", stdout) == nullptr) {
                perror("freopen");
                loop.quit(1);
                return;
            }
        };
    }
    request->on_finish = [&](bool success, auto) {
        warn("\033]9;-1;\033\\");
        warnln();
        if (!success)
            warnln("Request failed :(");
        loop.quit(0);
    };

    auto output_stream = ConditionalOutputFileStream { [&] { return save_at_provided_name ? received_actual_headers : true; }, stdout };
    request->stream_into(output_stream);

    dbgln("started request with id {}", request->id());

    auto rc = loop.exec();
    // FIXME: This shouldn't be needed.
    fclose(stdout);
    return rc;
}
