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
#include <LibCore/Stream.h>
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
        if (!entry.has("quote"sv) || !entry.has("author"sv) || !entry.has("utc_time"sv) || !entry.has("url"sv))
            return {};
        // From here on, trust that it's probably fine.
        q.m_quote = entry.get_deprecated("quote"sv).as_string();
        q.m_author = entry.get_deprecated("author"sv).as_string();
        // It is sometimes parsed as u32, sometimes as u64, depending on how large the number is.
        q.m_utc_time = entry.get_deprecated("utc_time"sv).to_number<u64>();
        q.m_url = entry.get_deprecated("url"sv).as_string();
        if (entry.has("context"sv))
            q.m_context = entry.get_deprecated("context"sv).as_string();

        return q;
    }

    DeprecatedString const& quote() const { return m_quote; }
    DeprecatedString const& author() const { return m_author; }
    u64 const& utc_time() const { return m_utc_time; }
    DeprecatedString const& url() const { return m_url; }
    Optional<DeprecatedString> const& context() const { return m_context; }

private:
    Quote() = default;

    DeprecatedString m_quote;
    DeprecatedString m_author;
    u64 m_utc_time;
    DeprecatedString m_url;
    Optional<DeprecatedString> m_context;
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

    Core::ArgsParser args_parser;
    args_parser.set_general_help("Open a fortune cookie, receive a free quote for the day!");
    args_parser.add_positional_argument(path, "Path to JSON file with quotes (/res/fortunes.json by default)", "path", Core::ArgsParser::Required::No);
    args_parser.parse(arguments);

    auto file = TRY(Core::Stream::File::open(path, Core::Stream::OpenMode::Read));

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

    outln(); // Tasteful spacing

    out("\033]8;;{}\033\\", chosen_quote.url());                // Begin link
    out("\033[34m({})\033[m", datetime.to_deprecated_string()); // Datetime
    out(" \033[34;1m<{}>\033[m", chosen_quote.author());        // Author
    out(" \033[32m{}\033[m", chosen_quote.quote());             // Quote itself
    out("\033]8;;\033\\");                                      // End link
    outln();

    if (chosen_quote.context().has_value())
        outln("\033[38;5;242m({})\033[m", chosen_quote.context().value()); // Some context

    outln(); // Tasteful spacing

    return 0;
}
