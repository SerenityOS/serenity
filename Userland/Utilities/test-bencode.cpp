/*
 * Copyright (c) 2021, Cesar Torres <shortanemoia@protonmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>
#include <AK/NonnullOwnPtrVector.h>
#include <LibTorrent/Bencode/Bencode.h>

using namespace Torrent::Bencode;

constinit StringView decode_result_3 { "( (announce:  7564703a2f2f747261636b65722e746f74616c6c796e6f74636f707065727375726665722e746b3a363936392f616e6e6f756e6365,\n"
                                       " )\n"
                                       " (info:  (   (name:    73616d706c655f66696c652e66696c65,\n"
                                       "   )\n"
                                       "   (piece length:    16384,\n"
                                       "   )\n"
                                       "   (pieces:    000102030405060708090a0b0c0d0e0f10111213,\n"
                                       "   )\n"
                                       "   (length:    420,\n"
                                       "   )\n"
                                       " )\n"
                                       " (dummy:  (\n"
                                       "   6974656d31,\n"
                                       "   2,\n"
                                       "   6974656d33,\n"
                                       "  )\n"
                                       " )\n" };

void print_node(size_t indent_level, StringBuilder& sb, BencodeNode const& node);
void print_node(size_t indent_level, StringBuilder& sb, BencodeNode const& node)
{
    switch (node.type()) {

    case BencodeNodeType::Integer:
        sb.appendff("{}{},\n", String::repeated(' ', indent_level), node.get<i64>());
        break;
    case BencodeNodeType::String:
        sb.appendff("{}{:hex-dump},\n", String::repeated(' ', indent_level), node.get<String>());
        break;
    case BencodeNodeType::List:
        sb.appendff("{}(\n", String::repeated(' ', indent_level));
        for (auto& item : *node.get<NonnullOwnPtr<NonnullOwnPtrVector<BencodeNode>>>())
            print_node(indent_level + 1, sb, item);
        sb.appendff("{})\n", String::repeated(' ', indent_level));
        break;
    case BencodeNodeType::Dictionary:
        sb.appendff("{}(", String::repeated(' ', indent_level));
        for (auto& item : *node.get<NonnullOwnPtr<NonnullOwnPtrVector<BencodeNode>>>())
            print_node(indent_level + 1, sb, item);
        break;
    case BencodeNodeType::KeyValuePair: {
        auto& pair = node.get<NonnullOwnPtr<KeyValuePair>>();
        sb.appendff("{}({}:", String::repeated(' ', indent_level), pair->key);
        print_node(indent_level + 1, sb, *pair->value);
        sb.appendff("{})\n", String::repeated(' ', indent_level));
    } break;
    }
}
int main()
{
    BencodeEncoder encoder;
    encoder.append(1234567890);
    auto buffer = encoder.to_buffer();
    outln("{}", buffer->span());
    StringView case1result { "i1234567890e" };
    VERIFY(buffer->span() == case1result.bytes());

    encoder.clear();
    encoder.append("Sample String");
    buffer = encoder.to_buffer();
    outln("{}", buffer->span());
    StringView case2result { "13:Sample String" };
    VERIFY(buffer->span() == case2result.bytes());

    encoder.clear();
    encoder.start_dict();
    encoder.append("announce");
    encoder.append("udp://tracker.totallynotcoppersurfer.tk:6969/announce");
    encoder.append("info");
    encoder.start_dict();
    encoder.append("name");
    encoder.append("sample_file.file");
    encoder.append("piece length");
    encoder.append(16384);
    encoder.append("pieces");
    encoder.append(Array<u8, 20> { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19 });
    encoder.append("length");
    encoder.append(420);
    encoder.end_dict();
    encoder.append("dummy");
    encoder.start_list();
    encoder.append("item1");
    encoder.append(2);
    encoder.append("item3");
    encoder.end_list();
    encoder.end_dict();
    buffer = encoder.to_buffer();
    outln("{:hex-dump}", buffer->span());
    StringView case3result { "d8:announce53:udp://tracker.totallynotcoppersurfer.tk:6969/announce4:infod4:name16:sample_file.file12:piece"
                             " lengthi16384e6:pieces20:\x00\x01\x02\x03\x04\x05\x06\x07\x08\x09\x0a\x0b\x0c\x0d\x0e\x0f\x10\x11\x12\x13"
                             "6:lengthi420ee5:dummyl5:item1i2e5:item3ee",
        193 };
    outln("{:hex-dump}", case3result.bytes());
    outln("expected {} bytes, got {} bytes", case3result.length(), buffer->size());
    VERIFY(buffer->size() == case3result.bytes().size());
    VERIFY(buffer->span() == case3result.bytes());

    auto maybe_node1 = decode("i1234567890e");
    VERIFY(maybe_node1);
    outln("{}", maybe_node1->get<i64>());

    auto maybe_node2 = decode("15:hello world :^)");
    VERIFY(maybe_node2);
    outln("{}\n", maybe_node2->get<String>());

    auto maybe_node = decode(buffer->bytes());
    VERIFY(maybe_node);
    StringBuilder sb;
    print_node(0, sb, *maybe_node);
    String result = sb.to_string();
    out(result);
    VERIFY(result == decode_result_3);

    auto maybe_node_ptr = maybe_node->get_node_with_path("/'announce'");
    VERIFY(maybe_node_ptr != nullptr);
    VERIFY(maybe_node_ptr->get<String>() == "udp://tracker.totallynotcoppersurfer.tk:6969/announce");
    outln(maybe_node_ptr->get<String>());

    maybe_node_ptr = maybe_node->get_node_with_path("/'info'/'length'");
    VERIFY(maybe_node_ptr != nullptr);
    VERIFY(maybe_node_ptr->get<i64>() == 420);
    outln("{}", maybe_node_ptr->get<i64>());
    outln("Test completed succesfully!");
}
