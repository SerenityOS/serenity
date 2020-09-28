/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <AK/GenericLexer.h>
#include <AK/LexicalPath.h>
#include <AK/NumberFormat.h>
#include <AK/SharedBuffer.h>
#include <AK/URL.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/EventLoop.h>
#include <LibCore/File.h>
#include <LibProtocol/Client.h>
#include <LibProtocol/Download.h>
#include <ctype.h>
#include <stdio.h>

// FIXME: Move this somewhere else when it's needed (e.g. in the Browser)
class ContentDispositionParser {
public:
    ContentDispositionParser(const StringView& value)
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

    const StringView& filename() const { return m_filename; }
    const StringView& name() const { return m_name; }
    Kind kind() const { return m_kind; }
    bool might_be_wrong() const { return m_might_be_wrong; }

private:
    StringView m_filename;
    StringView m_name;
    Kind m_kind { Kind::Inline };
    bool m_might_be_wrong { false };
};

static void do_write(const ByteBuffer& payload)
{
    size_t length_remaining = payload.size();
    size_t length_written = 0;
    while (length_remaining > 0) {
        auto nwritten = fwrite(payload.offset_pointer(length_written), sizeof(char), length_remaining, stdout);
        if (nwritten > 0) {
            length_remaining -= nwritten;
            length_written += nwritten;
            continue;
        }

        if (feof(stdout)) {
            fprintf(stderr, "pro: unexpected eof while writing\n");
            return;
        }

        if (ferror(stdout)) {
            fprintf(stderr, "pro: error while writing\n");
            return;
        }
    }
}

int main(int argc, char** argv)
{
    const char* url_str = nullptr;
    bool save_at_provided_name = false;

    Core::ArgsParser args_parser;
    args_parser.add_option(save_at_provided_name, "Write to a file named as the remote file", nullptr, 'O');
    args_parser.add_positional_argument(url_str, "URL to download from", "url");
    args_parser.parse(argc, argv);

    URL url(url_str);
    if (!url.is_valid()) {
        fprintf(stderr, "'%s' is not a valid URL\n", url_str);
        return 1;
    }

    Core::EventLoop loop;
    auto protocol_client = Protocol::Client::construct();

    auto download = protocol_client->start_download("GET", url.to_string());
    if (!download) {
        fprintf(stderr, "Failed to start download for '%s'\n", url_str);
        return 1;
    }

    u32 previous_downloaded_size { 0 };
    timeval prev_time, current_time, time_diff;
    gettimeofday(&prev_time, nullptr);

    download->on_progress = [&](Optional<u32> maybe_total_size, u32 downloaded_size) {
        fprintf(stderr, "\r\033[2K");
        if (maybe_total_size.has_value()) {
            fprintf(stderr, "\033]9;%d;%d;\033\\", downloaded_size, maybe_total_size.value());
            fprintf(stderr, "Download progress: %s / %s", human_readable_size(downloaded_size).characters(), human_readable_size(maybe_total_size.value()).characters());
        } else {
            fprintf(stderr, "Download progress: %s / ???", human_readable_size(downloaded_size).characters());
        }

        gettimeofday(&current_time, nullptr);
        timersub(&current_time, &prev_time, &time_diff);

        auto time_diff_ms = time_diff.tv_sec * 1000 + time_diff.tv_usec / 1000;
        auto size_diff = downloaded_size - previous_downloaded_size;

        fprintf(stderr, " at %s/s", human_readable_size(((float)size_diff / (float)time_diff_ms) * 1000).characters());

        previous_downloaded_size = downloaded_size;
        prev_time = current_time;
    };
    download->on_finish = [&](bool success, auto& payload, auto, auto& response_headers, auto) {
        fprintf(stderr, "\033]9;-1;\033\\");
        fprintf(stderr, "\n");
        if (success && save_at_provided_name) {
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
                        output_name = String::format("%s.%d", output_name.characters(), i);
                    ++i;
                } while (Core::File::exists(output_name));
            }

            if (freopen(output_name.characters(), "w", stdout) == nullptr) {
                perror("freopen");
                success = false; // oops!
                loop.quit(1);
            }
        }
        if (success)
            do_write(payload);
        else
            fprintf(stderr, "Download failed :(\n");
        loop.quit(0);
    };
    dbgprintf("started download with id %d\n", download->id());

    return loop.exec();
}
