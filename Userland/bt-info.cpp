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

#include <AK/Hex.h>
#include <LibBencode/Parser.h>
#include <LibBencode/Value.h>
#include <LibBitTorrent/MetaInfo.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibCrypto/Hash/SHA1.h>

int main(int argc, char** argv)
{
    const char* file_path = nullptr;
    bool show_pieces = false;
    bool show_files = false;

    Core::ArgsParser args_parser;
    args_parser.set_general_help("Parse a .torrent file and print some information about it.");
    args_parser.add_positional_argument(file_path, "Path to .torrent file", "", Core::ArgsParser::Required::Yes);
    args_parser.add_option(show_pieces, "Show pieces", "show-pieces", 's');
    args_parser.add_option(show_files, "Show files", "show-files", 'f');
    args_parser.parse(argc, argv);

    auto file = Core::File::construct(file_path);
    if (!file->open(Core::IODevice::ReadOnly)) {
        out("Failed to open {}: {}\n", file_path, file->error_string());
        return 1;
    }
    auto file_contents = file->read_all();

    Bencode::Parser parser(file_contents);

    auto value = parser.parse();
    if (!value.has_value()) {
        out("Failed to parse file contents\n");
        return 1;
    }

    auto meta_info = MetaInfo::from_value(value.value());
    if (!meta_info.has_value()) {
        out("Couldn't build MetaInfo structure from decoded torrent\n");
        return 1;
    }

    out("Info hash: {}\n", meta_info.value().info_hash_hex());
    out("Announce: {}\n", meta_info.value().announce().characters());
    for (auto& announce_list_tier : meta_info.value().announce_list())
        for (auto& announce_list_url : announce_list_tier)
            out("Announce list entry: {}\n", announce_list_url.characters());
    out("Comment: {}\n", meta_info.value().comment().characters());
    out("Created By: {} @ {}\n", meta_info.value().created_by().characters(), meta_info.value().creation_date());
    out("Name: {}\n", meta_info.value().name().characters());
    out("Pieces: {} * {}\n", meta_info.value().pieces().size(), meta_info.value().piece_length());
    if (show_pieces)
        for (auto& piece : meta_info.value().pieces())
            out("  Piece: {}\n", encode_hex(piece));
    out("Files: {}\n", meta_info.value().files().size());
    if (show_files)
        for (auto& file : meta_info.value().files())
            out("  File: {} ({} bytes)\n", file.path(), file.length());

    return 0;
}
