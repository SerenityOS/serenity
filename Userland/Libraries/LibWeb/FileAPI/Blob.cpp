/*
 * Copyright (c) 2022, Kenneth Myhra <kennethmyhra@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/GenericLexer.h>
#include <AK/StdLibExtras.h>
#include <LibJS/Runtime/ArrayBuffer.h>
#include <LibWeb/Bindings/BlobPrototype.h>
#include <LibWeb/Bindings/DOMExceptionWrapper.h>
#include <LibWeb/Bindings/IDLAbstractOperations.h>
#include <LibWeb/FileAPI/Blob.h>
#include <LibWeb/HTML/Window.h>

namespace Web::FileAPI {

DOM::ExceptionOr<JS::NonnullGCPtr<Blob>> Blob::create(HTML::Window& window, ByteBuffer byte_buffer, String type)
{
    return JS::NonnullGCPtr(*window.heap().allocate<Blob>(window.realm(), window, move(byte_buffer), move(type)));
}

// https://w3c.github.io/FileAPI/#convert-line-endings-to-native
ErrorOr<String> convert_line_endings_to_native(String const& string)
{
    // 1. Let native line ending be be the code point U+000A LF.
    auto native_line_ending = "\n"sv;

    // 2. If the underlying platform’s conventions are to represent newlines as a carriage return and line feed sequence, set native line ending to the code point U+000D CR followed by the code point U+000A LF.
    // NOTE: this step is a no-op since LibWeb does not compile on Windows, which is the only platform we know of that that uses a carriage return and line feed sequence for line endings.

    // 3. Set result to the empty string.
    StringBuilder result;

    // 4. Let position be a position variable for s, initially pointing at the start of s.
    auto lexer = GenericLexer { string.view() };

    // 5. Let token be the result of collecting a sequence of code points that are not equal to U+000A LF or U+000D CR from s given position.
    // 6. Append token to result.
    TRY(result.try_append(lexer.consume_until(is_any_of("\n\r"sv))));

    // 7. While position is not past the end of s:
    while (!lexer.is_eof()) {
        // 1. If the code point at position within s equals U+000D CR:
        if (lexer.peek() == '\r') {
            // 1. Append native line ending to result.
            TRY(result.try_append(native_line_ending));

            // 2. Advance position by 1.
            lexer.ignore(1);

            // 3. If position is not past the end of s and the code point at position within s equals U+000A LF advance position by 1.
            if (!lexer.is_eof() && lexer.peek() == '\n')
                lexer.ignore(1);
        }
        // 2. Otherwise if the code point at position within s equals U+000A LF, advance position by 1 and append native line ending to result.
        else if (lexer.peek() == '\n') {
            lexer.ignore(1);
            TRY(result.try_append(native_line_ending));
        }

        // 3. Let token be the result of collecting a sequence of code points that are not equal to U+000A LF or U+000D CR from s given position.
        // 4. Append token to result.
        TRY(result.try_append(lexer.consume_until(is_any_of("\n\r"sv))));
    }
    // 5. Return result.
    return result.to_string();
}

// https://w3c.github.io/FileAPI/#process-blob-parts
ErrorOr<ByteBuffer> process_blob_parts(Vector<BlobPart> const& blob_parts, Optional<BlobPropertyBag> const& options)
{
    // 1. Let bytes be an empty sequence of bytes.
    ByteBuffer bytes {};

    // 2. For each element in parts:
    for (auto const& blob_part : blob_parts) {
        TRY(blob_part.visit(
            // 1. If element is a USVString, run the following sub-steps:
            [&](String const& string) -> ErrorOr<void> {
                // 1. Let s be element.
                auto s = string;

                // 2. If the endings member of options is "native", set s to the result of converting line endings to native of element.
                if (options.has_value() && options->endings == Bindings::EndingType::Native)
                    s = TRY(convert_line_endings_to_native(s));

                // NOTE: The AK::String is always UTF-8.
                // 3. Append the result of UTF-8 encoding s to bytes.
                return bytes.try_append(s.to_byte_buffer());
            },
            // 2. If element is a BufferSource, get a copy of the bytes held by the buffer source, and append those bytes to bytes.
            [&](JS::Handle<JS::Object> const& buffer_source) -> ErrorOr<void> {
                auto data_buffer = TRY(Bindings::IDL::get_buffer_source_copy(*buffer_source.cell()));
                return bytes.try_append(data_buffer.bytes());
            },
            // 3. If element is a Blob, append the bytes it represents to bytes.
            [&](JS::Handle<Blob> const& blob) -> ErrorOr<void> {
                return bytes.try_append(blob->bytes());
            }));
    }
    // 3. Return bytes.
    return bytes;
}

bool is_basic_latin(StringView view)
{
    for (auto code_point : view) {
        if (code_point < 0x0020 || code_point > 0x007E)
            return false;
    }
    return true;
}

Blob::Blob(HTML::Window& window)
    : PlatformObject(window.realm())
{
    set_prototype(&window.cached_web_prototype("Blob"));
}

Blob::Blob(HTML::Window& window, ByteBuffer byte_buffer, String type)
    : PlatformObject(window.realm())
    , m_byte_buffer(move(byte_buffer))
    , m_type(move(type))
{
    set_prototype(&window.cached_web_prototype("Blob"));
}

Blob::Blob(HTML::Window& window, ByteBuffer byte_buffer)
    : PlatformObject(window.realm())
    , m_byte_buffer(move(byte_buffer))
{
    set_prototype(&window.cached_web_prototype("Blob"));
}

Blob::~Blob() = default;

// https://w3c.github.io/FileAPI/#ref-for-dom-blob-blob
DOM::ExceptionOr<JS::NonnullGCPtr<Blob>> Blob::create(HTML::Window& window, Optional<Vector<BlobPart>> const& blob_parts, Optional<BlobPropertyBag> const& options)
{
    // 1. If invoked with zero parameters, return a new Blob object consisting of 0 bytes, with size set to 0, and with type set to the empty string.
    if (!blob_parts.has_value() && !options.has_value())
        return JS::NonnullGCPtr(*window.heap().allocate<Blob>(window.realm(), window));

    ByteBuffer byte_buffer {};
    // 2. Let bytes be the result of processing blob parts given blobParts and options.
    if (blob_parts.has_value()) {
        byte_buffer = TRY_OR_RETURN_OOM(process_blob_parts(blob_parts.value(), options));
    }

    auto type = String::empty();
    // 3. If the type member of the options argument is not the empty string, run the following sub-steps:
    if (options.has_value() && !options->type.is_empty()) {
        // 1. If the type member is provided and is not the empty string, let t be set to the type dictionary member.
        //    If t contains any characters outside the range U+0020 to U+007E, then set t to the empty string and return from these substeps.
        //    NOTE: t is set to empty string at declaration.
        if (!options->type.is_empty()) {
            if (is_basic_latin(options->type))
                type = options->type;
        }

        // 2. Convert every character in t to ASCII lowercase.
        if (!type.is_empty())
            type = options->type.to_lowercase();
    }

    // 4. Return a Blob object referring to bytes as its associated byte sequence, with its size set to the length of bytes, and its type set to the value of t from the substeps above.
    return JS::NonnullGCPtr(*window.heap().allocate<Blob>(window.realm(), window, move(byte_buffer), move(type)));
}

DOM::ExceptionOr<JS::NonnullGCPtr<Blob>> Blob::create_with_global_object(HTML::Window& window, Optional<Vector<BlobPart>> const& blob_parts, Optional<BlobPropertyBag> const& options)
{
    return Blob::create(window, blob_parts, options);
}

// https://w3c.github.io/FileAPI/#dfn-slice
DOM::ExceptionOr<JS::NonnullGCPtr<Blob>> Blob::slice(Optional<i64> start, Optional<i64> end, Optional<String> const& content_type)
{
    // 1. The optional start parameter is a value for the start point of a slice() call, and must be treated as a byte-order position, with the zeroth position representing the first byte.
    //    User agents must process slice() with start normalized according to the following:
    i64 relative_start;
    if (!start.has_value()) {
        // a. If the optional start parameter is not used as a parameter when making this call, let relativeStart be 0.
        relative_start = 0;
    } else {
        auto start_value = start.value();
        // b. If start is negative, let relativeStart be max((size + start), 0).
        if (start_value < 0) {
            relative_start = max((size() + start_value), 0);
        }
        // c. Else, let relativeStart be min(start, size).
        else {
            relative_start = min(start_value, size());
        }
    }

    // 2. The optional end parameter is a value for the end point of a slice() call. User agents must process slice() with end normalized according to the following:
    i64 relative_end;
    if (!end.has_value()) {
        // a. If the optional end parameter is not used as a parameter when making this call, let relativeEnd be size.
        relative_end = size();
    } else {
        auto end_value = end.value();
        // b. If end is negative, let relativeEnd be max((size + end), 0).
        if (end_value < 0) {
            relative_end = max((size() + end_value), 0);
        }
        // c Else, let relativeEnd be min(end, size).
        else {
            relative_end = min(end_value, size());
        }
    }

    // 3. The optional contentType parameter is used to set the ASCII-encoded string in lower case representing the media type of the Blob.
    //    User agents must process the slice() with contentType normalized according to the following:
    String relative_content_type;
    if (!content_type.has_value()) {
        // a. If the contentType parameter is not provided, let relativeContentType be set to the empty string.
        relative_content_type = "";
    } else {
        // b. Else let relativeContentType be set to contentType and run the substeps below:

        // FIXME: 1. If relativeContentType contains any characters outside the range of U+0020 to U+007E, then set relativeContentType to the empty string and return from these substeps.

        // 2. Convert every character in relativeContentType to ASCII lowercase.
        relative_content_type = content_type->to_lowercase();
    }

    // 4. Let span be max((relativeEnd - relativeStart), 0).
    auto span = max((relative_end - relative_start), 0);

    // 5. Return a new Blob object S with the following characteristics:
    // a. S refers to span consecutive bytes from this, beginning with the byte at byte-order position relativeStart.
    // b. S.size = span.
    // c. S.type = relativeContentType.
    auto byte_buffer = TRY_OR_RETURN_OOM(m_byte_buffer.slice(relative_start, span));
    return JS::NonnullGCPtr(*heap().allocate<Blob>(realm(), global_object(), move(byte_buffer), move(relative_content_type)));
}

// https://w3c.github.io/FileAPI/#dom-blob-text
JS::Promise* Blob::text()
{
    // FIXME: 1. Let stream be the result of calling get stream on this.
    // FIXME: 2. Let reader be the result of getting a reader from stream. If that threw an exception, return a new promise rejected with that exception.

    // FIXME: We still need to implement ReadableStream for this step to be fully valid.
    // 3. Let promise be the result of reading all bytes from stream with reader
    auto* promise = JS::Promise::create(realm());
    auto* result = JS::js_string(vm(), String { m_byte_buffer.bytes() });

    // 4. Return the result of transforming promise by a fulfillment handler that returns the result of running UTF-8 decode on its first argument.
    promise->fulfill(result);
    return promise;
}

// https://w3c.github.io/FileAPI/#dom-blob-arraybuffer
JS::Promise* Blob::array_buffer()
{
    // FIXME: 1. Let stream be the result of calling get stream on this.
    // FIXME: 2. Let reader be the result of getting a reader from stream. If that threw an exception, return a new promise rejected with that exception.

    // FIXME: We still need to implement ReadableStream for this step to be fully valid.
    // 3. Let promise be the result of reading all bytes from stream with reader.
    auto* promise = JS::Promise::create(realm());
    auto buffer_result = JS::ArrayBuffer::create(realm(), m_byte_buffer.size());
    if (buffer_result.is_error()) {
        promise->reject(buffer_result.release_error().value().release_value());
        return promise;
    }
    auto* buffer = buffer_result.release_value();
    buffer->buffer().overwrite(0, m_byte_buffer.data(), m_byte_buffer.size());

    // 4. Return the result of transforming promise by a fulfillment handler that returns a new ArrayBuffer whose contents are its first argument.
    promise->fulfill(buffer);
    return promise;
}

}
