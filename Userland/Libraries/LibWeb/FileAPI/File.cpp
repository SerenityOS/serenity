/*
 * Copyright (c) 2022, Kenneth Myhra <kennethmyhra@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/FileAPI/File.h>
#include <LibWeb/HTML/Window.h>

namespace Web::FileAPI {

File::File(HTML::Window& window, ByteBuffer byte_buffer, String file_name, String type, i64 last_modified)
    : Blob(window, move(byte_buffer), move(type))
    , m_name(move(file_name))
    , m_last_modified(last_modified)
{
    set_prototype(&window.cached_web_prototype("File"));
}

File::~File() = default;

// https://w3c.github.io/FileAPI/#ref-for-dom-file-file
WebIDL::ExceptionOr<JS::NonnullGCPtr<File>> File::create(HTML::Window& window, Vector<BlobPart> const& file_bits, String const& file_name, Optional<FilePropertyBag> const& options)
{
    // 1. Let bytes be the result of processing blob parts given fileBits and options.
    auto bytes = TRY_OR_RETURN_OOM(window, process_blob_parts(file_bits, static_cast<Optional<BlobPropertyBag> const&>(*options)));

    // 2. Let n be the fileName argument to the constructor.
    //    NOTE: Underlying OS filesystems use differing conventions for file name; with constructed files, mandating UTF-16 lessens ambiquity when file names are converted to byte sequences.
    auto name = file_name;

    auto type = String::empty();
    i64 last_modified = 0;
    // 3. Process FilePropertyBag dictionary argument by running the following substeps:
    if (options.has_value()) {
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

        // 3. If the lastModified member is provided, let d be set to the lastModified dictionary member. If it is not provided, set d to the current date and time represented as the number of milliseconds since the Unix Epoch (which is the equivalent of Date.now() [ECMA-262]).
        //    Note: Since ECMA-262 Date objects convert to long long values representing the number of milliseconds since the Unix Epoch, the lastModified member could be a Date object [ECMA-262].
        last_modified = options->last_modified.has_value() ? options->last_modified.value() : Time::now_realtime().to_milliseconds();
    }

    // 4. Return a new File object F such that:
    //    2. F refers to the bytes byte sequence.
    //       NOTE: Spec started at 2 therefore keeping the same number sequence here.
    //    3. F.size is set to the number of total bytes in bytes.
    //    4. F.name is set to n.
    //    5. F.type is set to t.
    //    6. F.lastModified is set to d.
    return JS::NonnullGCPtr(*window.heap().allocate<File>(window.realm(), window, move(bytes), move(name), move(type), last_modified));
}

WebIDL::ExceptionOr<JS::NonnullGCPtr<File>> File::create_with_global_object(HTML::Window& window, Vector<BlobPart> const& file_bits, String const& file_name, Optional<FilePropertyBag> const& options)
{
    return create(window, file_bits, file_name, options);
}

}
