/*
 * Copyright (c) 2023, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "VorbisComment.h"
#include <AK/Endian.h>
#include <AK/MemoryStream.h>
#include <AK/Span.h>
#include <AK/String.h>

namespace Audio {

// "Content vector format"
static ErrorOr<void> read_vorbis_field(Metadata& metadata_to_write_into, String const& unparsed_user_comment)
{
    // Technically the field name has to be ASCII, but we just accept all UTF-8.
    auto field_name_and_contents = TRY(unparsed_user_comment.split_limit('=', 2));

    if (field_name_and_contents.size() != 2)
        return Error::from_string_view("User comment does not contain '='"sv);
    auto contents = field_name_and_contents.take_last();
    auto field_name = TRY(field_name_and_contents.take_first().to_uppercase());

    // Some of these are taken from https://age.hobba.nl/audio/tag_frame_reference.html
    if (field_name == "TITLE"sv) {
        if (metadata_to_write_into.title.has_value())
            TRY(metadata_to_write_into.add_miscellaneous(field_name, contents));
        else
            metadata_to_write_into.title = contents;
    } else if (field_name == "VERSION"sv) {
        if (metadata_to_write_into.subtitle.has_value())
            TRY(metadata_to_write_into.add_miscellaneous(field_name, contents));
        else
            metadata_to_write_into.subtitle = contents;
    } else if (field_name == "ALBUM"sv) {
        if (metadata_to_write_into.album.has_value())
            TRY(metadata_to_write_into.add_miscellaneous(field_name, contents));
        else
            metadata_to_write_into.album = contents;
    } else if (field_name == "COPYRIGHT"sv) {
        if (metadata_to_write_into.copyright.has_value())
            TRY(metadata_to_write_into.add_miscellaneous(field_name, contents));
        else
            metadata_to_write_into.copyright = contents;
    } else if (field_name == "ISRC"sv) {
        if (metadata_to_write_into.isrc.has_value())
            TRY(metadata_to_write_into.add_miscellaneous(field_name, contents));
        else
            metadata_to_write_into.isrc = contents;
    } else if (field_name == "GENRE"sv) {
        if (metadata_to_write_into.genre.has_value())
            TRY(metadata_to_write_into.add_miscellaneous(field_name, contents));
        else
            metadata_to_write_into.genre = contents;
    } else if (field_name == "COMMENT"sv) {
        if (metadata_to_write_into.comment.has_value())
            TRY(metadata_to_write_into.add_miscellaneous(field_name, contents));
        else
            metadata_to_write_into.comment = contents;
    } else if (field_name == "TRACKNUMBER"sv) {
        if (metadata_to_write_into.track_number.has_value())
            TRY(metadata_to_write_into.add_miscellaneous(field_name, contents));
        else if (auto maybe_number = contents.to_number<unsigned>(); maybe_number.has_value())
            metadata_to_write_into.track_number = maybe_number.release_value();
        else
            TRY(metadata_to_write_into.add_miscellaneous(field_name, contents));
    } else if (field_name == "DATE"sv) {
        if (metadata_to_write_into.unparsed_time.has_value())
            TRY(metadata_to_write_into.add_miscellaneous(field_name, contents));
        else
            metadata_to_write_into.unparsed_time = contents;
    } else if (field_name == "PERFORMER"sv) {
        TRY(metadata_to_write_into.add_person(Person::Role::Performer, contents));
    } else if (field_name == "ARTIST"sv) {
        TRY(metadata_to_write_into.add_person(Person::Role::Artist, contents));
    } else if (field_name == "COMPOSER"sv) {
        TRY(metadata_to_write_into.add_person(Person::Role::Composer, contents));
    } else if (field_name == "CONDUCTOR"sv) {
        TRY(metadata_to_write_into.add_person(Person::Role::Conductor, contents));
    } else if (field_name == "LYRICIST"sv) {
        TRY(metadata_to_write_into.add_person(Person::Role::Lyricist, contents));
    } else if (field_name == "ORGANIZATION"sv) {
        TRY(metadata_to_write_into.add_person(Person::Role::Publisher, contents));
    } else if (field_name == "PUBLISHER"sv) {
        TRY(metadata_to_write_into.add_person(Person::Role::Publisher, contents));
    } else if (field_name == "ENCODED-BY"sv) {
        TRY(metadata_to_write_into.add_person(Person::Role::Engineer, contents));
    } else {
        TRY(metadata_to_write_into.add_miscellaneous(field_name, contents));
    }

    return {};
}

ErrorOr<Metadata, LoaderError> load_vorbis_comment(ByteBuffer const& vorbis_comment)
{
    FixedMemoryStream stream { vorbis_comment };
    auto vendor_length = TRY(stream.read_value<LittleEndian<u32>>());
    Vector<u8> raw_vendor_string;
    TRY(raw_vendor_string.try_resize(vendor_length));
    TRY(stream.read_until_filled(raw_vendor_string));
    auto vendor_string = TRY(String::from_utf8(StringView { raw_vendor_string.span() }));

    Metadata metadata;
    metadata.encoder = move(vendor_string);

    auto user_comment_count = TRY(stream.read_value<LittleEndian<u32>>());
    for (size_t i = 0; i < user_comment_count; ++i) {
        auto user_comment_length = TRY(stream.read_value<LittleEndian<u32>>());
        Vector<u8> raw_user_comment;
        TRY(raw_user_comment.try_resize(user_comment_length));
        TRY(stream.read_until_filled(raw_user_comment));
        auto unparsed_user_comment = TRY(String::from_utf8(StringView { raw_user_comment.span() }));
        TRY(read_vorbis_field(metadata, unparsed_user_comment));
    }

    return metadata;
}

}
