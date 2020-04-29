/*
 * Copyright (c) 2020, The SerenityOS developers.
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

#include <AK/TestSuite.h>

#include <AK/HashMap.h>
#include <AK/JsonArray.h>
#include <AK/JsonObject.h>
#include <AK/JsonPathElement.h>
#include <AK/JsonValue.h>
#include <AK/StreamJsonBuilder.h>
#include <AK/StreamJsonParser.h>
#include <AK/String.h>
#include <AK/StringBuilder.h>

TEST_CASE(load_4chan_catalog)
{
    FILE* fp = fopen("4chan_catalog.json", "r");
    ASSERT(fp);

    StreamJsonParser parser;
    StreamJsonBuilder builder(move(parser));

    size_t count { 0 };
    builder.stream({ JsonPathElement::AnyArrayElement }, [&](const JsonValue& entry) {
        ASSERT(entry.is_object());
        auto object = entry.as_object();
        ASSERT(object.has("page"));
        ASSERT(object.has("threads"));

        ++count;

        return StreamJsonBuilder::VisitDecision::Discard;
    });

    for (;;) {
        char buffer[1024];
        if (!fgets(buffer, sizeof(buffer), fp))
            break;
        builder.append(buffer);
    }

    fclose(fp);

    ASSERT(count);
}

TEST_MAIN(StreamJson)
