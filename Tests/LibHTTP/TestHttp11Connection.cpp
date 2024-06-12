/*
 * Copyright (c) 2024, Dan Klishch <danilklishch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/AsyncStreamHelpers.h>
#include <LibHTTP/Http11Connection.h>
#include <LibTest/AsyncTestCase.h>
#include <LibTest/AsyncTestStreams.h>

struct HTTPUnitTest {
    StringView name;
    HTTP::Method method;
    StringView url;
    Vector<HTTP::Header> headers;
    StringView response;

    StringView request_expectation;
    StringView body_expectation;
};

Vector<HTTPUnitTest> const http_unit_tests = {
    {
        .name = "Basic"sv,
        .method = HTTP::Method::GET,
        .url = "/"sv,
        .headers = {
            { "Host", "localhost" },
        },
        .response = "HTTP/1.1 200 OK\r\n"
                    "Content-Length: 16\r\n"
                    "\r\n"
                    "0123456789abcdef"sv,
        .request_expectation = "GET / HTTP/1.1\r\n"
                               "Host: localhost\r\n"
                               "\r\n"sv,
        .body_expectation = "0123456789abcdef"sv,
    },
    {
        .name = "Chunked"sv,
        .method = HTTP::Method::GET,
        .url = "/"sv,
        .headers = {
            { "Host", "localhost" },
        },
        .response = "HTTP/1.1 200 OK\r\n"
                    "Transfer-Encoding: chunked\r\n"
                    "\r\n"
                    "18\r\n"
                    "0123456789abcdef\r\n\r\n"
                    "19\r\n"
                    "Well hello friends!\r\n"
                    "0\r\n"
                    "\r\n"sv,
        .request_expectation = "GET / HTTP/1.1\r\n"
                               "Host: localhost\r\n"
                               "\r\n"sv,
        .body_expectation = "0123456789abcdef\r\nWell hello friends!"sv,
    },
};

ASYNC_TEST_CASE(unit_tests_single)
{
    for (auto const& test : http_unit_tests) {
        outln("Running '{}'...", test.name);

        auto response_partitioning = Test::randomly_partition_input(1, AK::get_random_uniform(50) + 1, test.response.length());
        outln("Input partitioning: {}", response_partitioning);
        auto input = make<Test::AsyncMemoryInputStream>(test.response, Test::StreamCloseExpectation::Close, move(response_partitioning));
        auto output = make<Test::AsyncMemoryOutputStream>(Test::StreamCloseExpectation::Close);

        auto output_ref = output.ptr();

        auto stream_pair = make<AsyncStreamPair>(move(input), move(output));
        auto connection = make<HTTP::Http11Connection>(move(stream_pair));

        CO_TRY_OR_FAIL(co_await connection->request(
            {
                .method = test.method,
                .url = test.url,
                .headers = test.headers,
            },
            [&](HTTP::Http11Response& response) -> Coroutine<ErrorOr<void>> {
                auto body = CO_TRY(co_await Test::read_until_eof(response.body()));
                EXPECT_EQ(StringView { body }, test.body_expectation);
                co_return {};
            }));

        CO_TRY_OR_FAIL(co_await connection->close());

        EXPECT_EQ(StringView { output_ref->view() }, test.request_expectation);
    }
}
