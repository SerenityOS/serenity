/*
 * Copyright (c) 2021, Ben Wiederhake <BenWiederhake.GitHub@gmx.de>
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

#include <AK/ByteBuffer.h>
#include <AK/JsonArray.h>
#include <AK/JsonObject.h>
#include <AK/JsonValue.h>
#include <AK/Optional.h>
#include <AK/Vector.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/DateTime.h>
#include <LibCore/File.h>
#include <stdio.h>
#include <stdlib.h>

class Quote {
public:
    static Optional<Quote> try_parse(const JsonValue& value)
    {
        if (!value.is_object())
            return {};
        auto entry = value.as_object();
        Quote q;
        if (!entry.has("quote") || !entry.has("author") || !entry.has("utc_time") || !entry.has("url"))
            return {};
        // From here on, trust that it's probably fine.
        q.m_quote = entry.get("quote").as_string();
        q.m_author = entry.get("author").as_string();
        // It is sometimes parsed as u32, sometimes as u64, depending on how large the number is.
        q.m_utc_time = entry.get("utc_time").to_number<u64>();
        q.m_url = entry.get("url").as_string();
        if (entry.has("context"))
            q.m_context = entry.get("context").as_string();

        return q;
    }

    const String& quote() const { return m_quote; }
    const String& author() const { return m_author; }
    const u64& utc_time() const { return m_utc_time; }
    const String& url() const { return m_url; }
    const Optional<String>& context() const { return m_context; }

private:
    Quote() = default;

    String m_quote;
    String m_author;
    u64 m_utc_time;
    String m_url;
    Optional<String> m_context;
};

static Vector<Quote> parse_all(const JsonArray& array)
{
    Vector<Quote> quotes;
    for (int i = 0; i < array.size(); ++i) {
        Optional<Quote> q = Quote::try_parse(array[i]);
        if (!q.has_value()) {
            warnln("WARNING: Could not parse quote #{}!", i);
        } else {
            quotes.append(q.value());
        }
    }
    return quotes;
}

int main(int argc, char** argv)
{
    if (pledge("stdio rpath", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    const char* path = "/res/fortunes.json";

    Core::ArgsParser args_parser;
    args_parser.set_general_help("Open a fortune cookie, recieve a free quote for the day!");
    args_parser.add_positional_argument(path, "Path to JSON file with quotes (/res/fortunes.json by default)", "path", Core::ArgsParser::Required::No);
    args_parser.parse(argc, argv);

    auto file = Core::File::construct(path);
    if (!file->open(Core::IODevice::ReadOnly)) {
        warnln("Couldn't open {} for reading: {}", path, file->error_string());
        return 1;
    }

    if (pledge("stdio", nullptr) < 0) {
        perror("pledge");
        return 1;
    }
    if (unveil(nullptr, nullptr) < 0) {
        perror("unveil");
        return 1;
    }

    auto file_contents = file->read_all();
    auto json = JsonValue::from_string(file_contents);
    if (!json.has_value()) {
        warnln("Couldn't parse {} as JSON", path);
        return 1;
    }
    if (!json->is_array()) {
        warnln("{} does not contain an array of quotes", path);
        return 1;
    }

    const auto quotes = parse_all(json->as_array());
    if (quotes.is_empty()) {
        warnln("{} does not contain any valid quotes", path);
        return 1;
    }

    u32 i = arc4random_uniform(quotes.size());
    const auto& chosen_quote = quotes[i];
    auto datetime = Core::DateTime::from_timestamp(chosen_quote.utc_time());

    outln(); // Tasteful spacing

    out("\033]8;;{}\033\\", chosen_quote.url());         // Begin link
    out("\033[34m({})\033[m", datetime.to_string());     // Datetime
    out(" \033[34;1m<{}>\033[m", chosen_quote.author()); // Author
    out(" \033[32m{}\033[m", chosen_quote.quote());      // Quote itself
    out("\033]8;;\033\\");                               // End link
    outln();

    if (chosen_quote.context().has_value())
        outln("\033[38;5;242m({})\033[m", chosen_quote.context().value()); // Some context

    outln(); // Tasteful spacing

    return 0;
}
