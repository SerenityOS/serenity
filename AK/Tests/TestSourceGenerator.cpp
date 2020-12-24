/*
 * Copyright (c) 2020, the SerenityOS developers.
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

#include <AK/SourceGenerator.h>

TEST_CASE(wrap_builder)
{
    StringBuilder builder;
    SourceGenerator generator { builder };

    generator.append("Hello, World!");

    EXPECT_EQ(builder.string_view(), "Hello, World!");
}

TEST_CASE(generate_c_code)
{
    StringBuilder builder;
    SourceGenerator generator { builder };
    generator.set("name", "foo");

    generator.append("const char* @name@ (void) { return \"@name@\"; }");

    EXPECT_EQ(generator.as_string_view(), "const char* foo (void) { return \"foo\"; }");
}

TEST_CASE(scoped)
{
    StringBuilder builder;
    SourceGenerator global_generator { builder };

    global_generator.append("\n");

    global_generator.set("foo", "foo-0");
    global_generator.set("bar", "bar-0");
    global_generator.append("@foo@ @bar@\n"); // foo-0 bar-0

    {
        auto scoped_generator_1 = global_generator.fork();
        scoped_generator_1.set("bar", "bar-1");
        global_generator.append("@foo@ @bar@\n"); // foo-0 bar-0
    }

    global_generator.append("@foo@ @bar@\n"); // foo-0 bar-0

    {
        auto scoped_generator_2 = global_generator.fork();
        scoped_generator_2.set("foo", "foo-2");
        scoped_generator_2.append("@foo@ @bar@\n"); // foo-2 bar-0

        {
            auto scoped_generator_3 = scoped_generator_2.fork();
            scoped_generator_3.set("bar", "bar-3");
            scoped_generator_3.append("@foo@ @bar@\n"); // foo-2 bar-3
        }

        {
            auto scoped_generator_4 = global_generator.fork();
            scoped_generator_4.append("@foo@ @bar@\n"); // foo-0 bar-0
        }

        scoped_generator_2.append("@foo@ @bar@\n"); // foo-2 bar-0
    }

    EXPECT_EQ(global_generator.as_string_view(), "\nfoo-0 bar-0\nfoo-0 bar-0\nfoo-0 bar-0\nfoo-2 bar-0\nfoo-2 bar-3\nfoo-0 bar-0\nfoo-2 bar-0\n");
}

TEST_MAIN(SourceGenerator)
