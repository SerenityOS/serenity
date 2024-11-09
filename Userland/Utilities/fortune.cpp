/*
 * Copyright (c) 2021, Ben Wiederhake <BenWiederhake.GitHub@gmx.de>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/JsonArray.h>
#include <AK/JsonObject.h>
#include <AK/JsonValue.h>
#include <AK/Optional.h>
#include <AK/Random.h>
#include <AK/Vector.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/DateTime.h>
#include <LibCore/File.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>

class Quote {
public:
    static Optional<Quote> try_parse(JsonValue const& value)
    {
        if (!value.is_object())
            return {};
        auto& entry = value.as_object();
        Quote q;
        if (!entry.has_string("quote"sv) || !entry.has_string("author"sv) || !entry.has_u64("utc_time"sv) || !entry.has_string("url"sv))
            return {};
        // From here on, trust that it's probably fine.
        q.m_quote = entry.get_byte_string("quote"sv).value();
        q.m_author = entry.get_byte_string("author"sv).value();
        q.m_utc_time = entry.get_u64("utc_time"sv).value();
        q.m_url = entry.get_byte_string("url"sv).value();
        if (entry.has("context"sv))
            q.m_context = entry.get_byte_string("context"sv).value();

        return q;
    }

    ByteString const& quote() const { return m_quote; }
    ByteString const& author() const { return m_author; }
    u64 const& utc_time() const { return m_utc_time; }
    ByteString const& url() const { return m_url; }
    Optional<ByteString> const& context() const { return m_context; }

private:
    Quote() = default;

    ByteString m_quote;
    ByteString m_author;
    u64 m_utc_time;
    ByteString m_url;
    Optional<ByteString> m_context;
};

static Vector<Quote> parse_all(JsonArray const& array)
{
    Vector<Quote> quotes;
    for (size_t i = 0; i < array.size(); ++i) {
        Optional<Quote> q = Quote::try_parse(array[i]);
        if (!q.has_value()) {
            warnln("WARNING: Could not parse quote #{}!", i);
        } else {
            quotes.append(q.value());
        }
    }
    return quotes;
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio rpath"));

    StringView path = "/res/fortunes.json"sv;

    Optional<bool> force_color;

    Core::ArgsParser args_parser;
    args_parser.set_general_help("Open a fortune cookie, receive a free quote for the day!");
    args_parser.add_option(Core::ArgsParser::Option {
        .argument_mode = Core::ArgsParser::OptionArgumentMode::Required,
        .help_string = "Choose when to color the output. Valid options are always, never, or auto (default). When color is set to auto, color codes will be emitted when stdout is a terminal",
        .long_name = "color",
        .value_name = "when",
        .accept_value = [&force_color](StringView color_when_string) {
            if (color_when_string.equals_ignoring_ascii_case("always"sv)) {
                force_color = true;
            } else if (color_when_string.equals_ignoring_ascii_case("never"sv)) {
                force_color = false;
            } else if (!color_when_string.equals_ignoring_ascii_case("auto"sv)) {
                warnln("Unknown argument '{}'. Valid arguments for --color are always, never, or auto (default)", color_when_string);
                return false;
            }

            return true;
        },
    });
    args_parser.add_positional_argument(path, "Path to JSON file with quotes (/res/fortunes.json by default)", "path", Core::ArgsParser::Required::No);
    args_parser.parse(arguments);

    auto file = TRY(Core::File::open(path, Core::File::OpenMode::Read));

    TRY(Core::System::unveil("/etc/timezone", "r"));
    TRY(Core::System::unveil(nullptr, nullptr));

    auto file_contents = TRY(file->read_until_eof());
    auto json = TRY(JsonValue::from_string(file_contents));
    if (!json.is_array()) {
        warnln("{} does not contain an array of quotes", path);
        return 1;
    }

    auto const quotes = parse_all(json.as_array());
    if (quotes.is_empty()) {
        warnln("{} does not contain any valid quotes", path);
        return 1;
    }

    u32 i = get_random_uniform(quotes.size());
    auto const& chosen_quote = quotes[i];
    auto datetime = Core::DateTime::from_timestamp(chosen_quote.utc_time());
    auto stdout_is_tty = TRY(Core::System::isatty(STDOUT_FILENO));
    auto show_color = force_color.has_value() ? force_color.value() : stdout_is_tty;

    if (stdout_is_tty) {
        outln();                                     // Tasteful spacing
        out("\033]8;;{}\033\\", chosen_quote.url()); // Begin link
    }

    if (show_color) {
        out("\033[34m({})\033[m", datetime.to_byte_string());
        out(" \033[34;1m<{}>\033[m", chosen_quote.author());
        out(" \033[32m{}\033[m", chosen_quote.quote());
    } else {
        out("({})", datetime.to_byte_string());
        out(" <{}>", chosen_quote.author());
        out(" {}", chosen_quote.quote());
    }

    if (stdout_is_tty)
        out("\033]8;;\033\\"); // End link

    outln();

    if (chosen_quote.context().has_value())
        outln("{}", chosen_quote.context().value());

    if (stdout_is_tty)
        outln(); // Tasteful spacing

    return 0;
}
