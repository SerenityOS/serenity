/*
 * Copyright (c) 2022-2023, Kenneth Myhra <kennethmyhra@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Time.h>
#include <LibJS/Runtime/Completion.h>
#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/FileAPI/File.h>
#include <LibWeb/Infra/Strings.h>

namespace Web::FileAPI {

File::File(JS::Realm& realm, ByteBuffer byte_buffer, String file_name, String type, i64 last_modified)
    : Blob(realm, move(byte_buffer), move(type))
    , m_name(move(file_name))
    , m_last_modified(last_modified)
{
}

void File::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    set_prototype(&Bindings::ensure_web_prototype<Bindings::FilePrototype>(realm, "File"));
}

File::~File() = default;

// https://w3c.github.io/FileAPI/#ref-for-dom-file-file
WebIDL::ExceptionOr<JS::NonnullGCPtr<File>> File::create(JS::Realm& realm, Vector<BlobPart> const& file_bits, String const& file_name, Optional<FilePropertyBag> const& options)
{
    auto& vm = realm.vm();

    // 1. Let bytes be the result of processing blob parts given fileBits and options.
    auto bytes = TRY_OR_THROW_OOM(realm.vm(), process_blob_parts(file_bits, options.has_value() ? static_cast<BlobPropertyBag const&>(*options) : Optional<BlobPropertyBag> {}));

    // 2. Let n be the fileName argument to the constructor.
    //    NOTE: Underlying OS filesystems use differing conventions for file name; with constructed files, mandating UTF-16 lessens ambiquity when file names are converted to byte sequences.
    auto name = file_name;

    auto type = String {};
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
            type = TRY_OR_THROW_OOM(vm, Infra::to_ascii_lowercase(type));

        // 3. If the lastModified member is provided, let d be set to the lastModified dictionary member. If it is not provided, set d to the current date and time represented as the number of milliseconds since the Unix Epoch (which is the equivalent of Date.now() [ECMA-262]).
        //    Note: Since ECMA-262 Date objects convert to long long values representing the number of milliseconds since the Unix Epoch, the lastModified member could be a Date object [ECMA-262].
        last_modified = options->last_modified.has_value() ? options->last_modified.value() : UnixDateTime::now().milliseconds_since_epoch();
    }

    // 4. Return a new File object F such that:
    //    2. F refers to the bytes byte sequence.
    //       NOTE: Spec started at 2 therefore keeping the same number sequence here.
    //    3. F.size is set to the number of total bytes in bytes.
    //    4. F.name is set to n.
    //    5. F.type is set to t.
    //    6. F.lastModified is set to d.
    return MUST_OR_THROW_OOM(realm.heap().allocate<File>(realm, realm, move(bytes), move(name), move(type), last_modified));
}

WebIDL::ExceptionOr<JS::NonnullGCPtr<File>> File::construct_impl(JS::Realm& realm, Vector<BlobPart> const& file_bits, String const& file_name, Optional<FilePropertyBag> const& options)
{
    return create(realm, file_bits, file_name, options);
}

}
