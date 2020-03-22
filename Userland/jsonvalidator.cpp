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

#include <AK/JsonObject.h>
#include <AK/JsonValue.h>
#include <AK/StringBuilder.h>
#include <LibCore/File.h>
#include <LibJsonValidator/JsonSchemaNode.h>
#include <LibJsonValidator/Parser.h>
#include <LibJsonValidator/Validator.h>
#include <stdio.h>
#include <unistd.h>

int main(int argc, char** argv)
{
#ifdef __serenity__
    if (pledge("stdio rpath", nullptr) < 0) {
        perror("pledge");
        return 1;
    }
#endif

    if (argc != 3) {
        fprintf(stderr, "usage: jsonvalidator <schema-file> <json-file>\n");
        return 0;
    }
    auto schema_file = Core::File::construct(argv[1]);
    if (!schema_file->open(Core::IODevice::ReadOnly)) {
        fprintf(stderr, "Couldn't open %s for reading: %s\n", argv[1], schema_file->error_string());
        return 1;
    }

    auto json_file = Core::File::construct(argv[2]);
    if (!json_file->open(Core::IODevice::ReadOnly)) {
        fprintf(stderr, "Couldn't open %s for reading: %s\n", argv[2], json_file->error_string());
        return 1;
    }

#ifdef __serenity__
    if (pledge("stdio", nullptr) < 0) {
        perror("pledge");
        return 1;
    }
#endif

    auto schema_json = JsonValue::from_string(schema_file->read_all());

    JsonValidator::Parser parser;
    JsonValue parser_result = parser.run(schema_json);
    if (parser_result.is_bool() && parser_result.as_bool()) {
        fprintf(stdout, "Parsing sucessfull.\n\n");
        parser.root_node()->dump(0);

    } else {
        fprintf(stderr, "Parser returned error: %s\n",
            parser_result.to_string().characters());
        return 1;
    }

    auto json_file_content = JsonValue::from_string(json_file->read_all());

    JsonValidator::Validator validator;
    JsonValue validator_result = validator.run(*parser.root_node(), json_file_content);

    if (validator_result.is_bool() && validator_result.as_bool()) {
        fprintf(stdout, "Validation sucessfull.\n\n");

    } else {
        if (validator_result.is_object())
            fprintf(stderr, "Validator returned error: %s\n",
                validator_result.as_object().get("message").as_string_or("").characters());
        else if (validator_result.is_array()) {
            fprintf(stderr, "Validator returned errors:\n");
            for (auto& value : validator_result.as_array().values())
                if (value.is_string())
                    fprintf(stderr, "%s\n", value.as_string().characters());
                else
                    fprintf(stderr, "Value is not string, but %s\n", value.to_string().characters());
        } else if (validator_result.is_string())
            fprintf(stderr, "Validator returned error: %s\n",
                validator_result.as_string().characters());

        return 1;
    }

    return 0;
}
