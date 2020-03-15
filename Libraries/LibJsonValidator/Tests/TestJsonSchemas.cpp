/*
 * Copyright (c) 2020, Emanuel Sprung <emanuel.sprung@gmail.com>
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

#include <AK/JsonArray.h>
#include <AK/JsonObject.h>
#include <AK/JsonValue.h>
#include <AK/String.h>
#include <AK/StringBuilder.h>
#include <LibJsonValidator/Parser.h>
#include <LibJsonValidator/Validator.h>

inline void execute(FILE* fp);

TEST_CASE(additionalProperties)
{
    FILE* fp = fopen("draft2019-09/additionalItems.json", "r");
    ASSERT(fp);

    execute(fp);
}

TEST_MAIN(JsonSchemas)

inline void execute(FILE* fp)
{
    StringBuilder builder;
    for (;;) {
        char buffer[1024];
        if (!fgets(buffer, sizeof(buffer), fp))
            break;
        builder.append(buffer);
    }

    fclose(fp);

    JsonValue test_json = JsonValue::from_string(builder.to_string());
    ASSERT(test_json.is_array());

    JsonValidator::Parser parser;
    JsonValidator::Validator validator;

    for (auto& item : test_json.as_array().values()) {
        ASSERT(item.is_object());
        auto item_obj = item.as_object();

        ASSERT(item_obj.has("description"));
        ASSERT(item_obj.has("schema"));
        ASSERT(item_obj.has("tests"));

        printf("CASE \"%s\":\n", item_obj.get("description").as_string().characters());
        printf("==============================\n");

        JsonValue res = parser.run(item_obj.get("schema"));
#ifdef JSON_SCHEMA_TEST_DEBUG
        if (!(res.is_bool() && res.as_bool()))
            printf("Parser result: %s\n", res.to_string().characters());
#endif
        EXPECT(res.is_bool());
        EXPECT(res.as_bool());

        auto tests = item_obj.get("tests");
        ASSERT(tests.is_array());
        auto tests_array = tests.as_array();

        for (auto& test_item : tests_array.values()) {
            ASSERT(test_item.is_object());
            auto test_item_obj = test_item.as_object();
            ASSERT(test_item_obj.has("description"));
            ASSERT(test_item_obj.has("data"));
            ASSERT(test_item_obj.has("valid"));

            printf("%s: ", test_item_obj.get("description").as_string().characters());

            EXPECT(parser.root_node() != nullptr);

            if (parser.root_node())
                res = validator.run(*parser.root_node(), test_item_obj.get("data"));
#ifdef JSON_SCHEMA_TEST_DEBUG
            else {
                printf("Parser has no root node. JSON data:\n%s\n", item.to_string().characters());
                fflush(stdout);
            }
#endif

            bool valid = test_item_obj.get("valid").as_bool();
            if (valid == (res.is_bool() && res.as_bool()))
                printf("✔\n");
            else {
                printf("✘\n");
#ifdef JSON_SCHEMA_TEST_DEBUG
                printf("Validator result: %s\n", res.to_string().characters());
#endif
            }
            EXPECT(valid == (res.is_bool() && res.as_bool()));
        }
        printf("\n");
    }
}
