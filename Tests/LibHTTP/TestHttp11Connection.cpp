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
    Variant<StringView, int> body_expectation;
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
    {
        .name = "Zlib compressed"sv,
        .method = HTTP::Method::GET,
        .url = "/"sv,
        .headers = {
            { "Host", "localhost" },
            { "Accept-Encoding", "deflate" },
        },
        .response = "HTTP/1.1 200 OK\r\n"
                    "Content-Encoding: deflate\r\n"
                    "Content-Length: 40\r\n"
                    "\r\n"
                    "\x78\x01\x01\x1d\x00\xe2\xff\x54\x68\x69\x73\x20\x69\x73\x20\x61"
                    "\x20\x73\x69\x6d\x70\x6c\x65\x20\x74\x65\x78\x74\x20\x66\x69\x6c"
                    "\x65\x20\x3a\x29\x99\x5e\x09\xe8"sv,
        .request_expectation = "GET / HTTP/1.1\r\n"
                               "Host: localhost\r\n"
                               "Accept-Encoding: deflate\r\n"
                               "\r\n"sv,
        .body_expectation = "This is a simple text file :)"sv,
    },
    {
        .name = "Invalid content encoding"sv,
        .method = HTTP::Method::GET,
        .url = "/"sv,
        .headers = {
            { "Host", "localhost" },
        },
        .response = "HTTP/1.1 200 OK\r\n"
                    "Content-Encoding: well-hello-friends\r\n"
                    "Content-Length: 0\r\n"
                    "\r\n"sv,
        .request_expectation = "GET / HTTP/1.1\r\n"
                               "Host: localhost\r\n"
                               "\r\n"sv,
        .body_expectation = 0,
    }
};

ASYNC_TEST_CASE(unit_tests_single)
{
    for (auto const& test : http_unit_tests) {
        outln("Running '{}'...", test.name);

        auto close_expectation = test.body_expectation.has<int>() ? Test::StreamCloseExpectation::Reset : Test::StreamCloseExpectation::Close;
        auto response_partitioning = Test::randomly_partition_input(1, AK::get_random_uniform(50) + 1, test.response.length());
        outln("Input partitioning: {}", response_partitioning);
        auto input = make<Test::AsyncMemoryInputStream>(test.response, close_expectation, move(response_partitioning));
        auto output = make<Test::AsyncMemoryOutputStream>(close_expectation);

        auto output_ref = output.ptr();

        auto stream_pair = make<AsyncStreamPair>(move(input), move(output));
        auto connection = make<HTTP::Http11Connection>(move(stream_pair));

        auto body_or_error = co_await [&] -> Coroutine<ErrorOr<ByteString>> {
            auto body = CO_TRY(co_await connection->request(
                {
                    .method = test.method,
                    .url = test.url,
                    .headers = test.headers,
                },
                [&](HTTP::Http11Response& response) -> Coroutine<ErrorOr<ByteString>> {
                    auto body = CO_TRY(co_await Test::read_until_eof(response.body()));
                    co_return ByteString { body };
                }));
            CO_TRY(co_await connection->close());
            co_return body;
        }();

        test.body_expectation.visit(
            [&](StringView view) {
                EXPECT(!body_or_error.is_error());
                if (!body_or_error.is_error())
                    EXPECT_EQ(view, StringView { body_or_error.value() });
            },
            [&](int error) {
                EXPECT(body_or_error.is_error());
                if (body_or_error.is_error())
                    EXPECT_EQ(error, body_or_error.error().code());
            });

        EXPECT_EQ(StringView { output_ref->view() }, test.request_expectation);
    }
}
