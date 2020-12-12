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

#include <AK/Hex.h>
#include <AK/URL.h>
#include <AK/URLParser.h>
#include <LibBitTorrent/HTTPTracker.h>
#include <LibBitTorrent/Tracker.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/EventLoop.h>

int main(int argc, char** argv)
{
    const char* tracker_url_string = nullptr;
    const char* info_hash_string = nullptr;
    const char* event_string = nullptr;
    const char* peer_id_string = nullptr;
    const char* ip_string = nullptr;
    int port = 0;
    int uploaded = 0;
    int downloaded = 0;
    int left = 0;
    int numwant = 0;
    bool compact = false;

    Core::ArgsParser args_parser;
    args_parser.set_general_help("Announce to a BitTorrent tracker to get peers.");
    args_parser.add_positional_argument(tracker_url_string, "URL of the tracker", "tracker_url");
    args_parser.add_positional_argument(info_hash_string, "info_hash to query the tracker for (hex encoded)", "info_hash");
    args_parser.add_option(event_string, "event", "event", 'e', "");
    args_parser.add_option(peer_id_string, "peer id (url encoded)", "peer_id", 'P', "XX_00000000000000000");
    args_parser.add_option(ip_string, "ip", "ip", 'i', "");
    args_parser.add_option(port, "port", "port", 'p', "6881");
    args_parser.add_option(uploaded, "uploaded", "uploaded", 'u', "0");
    args_parser.add_option(downloaded, "downloaded", "downloaded", 'd', "0");
    args_parser.add_option(left, "left", "left", 'l', "0");
    args_parser.add_option(numwant, "numwant", "numwant", 'n', "50");
    args_parser.add_option(compact, "compact", "compact", 'c');
    args_parser.parse(argc, argv);

    URL tracker_url(tracker_url_string);
    if (!tracker_url.is_valid()) {
        out("'{}' is not a valid URL\n", tracker_url_string);
        return 1;
    }

    AnnounceRequest request;

    if (strlen(info_hash_string) != 40) {
        out("'{}' is not a valid info_hash; it should be 40 characters\n", info_hash_string);
        return 1;
    }
    auto info_hash = decode_hex(info_hash_string);
    if (!info_hash.has_value()) {
        out("'{}' is not a valid info_hash; parsing it as hex failed\n", info_hash_string);
        return 1;
    }
    request.set_info_hash(info_hash.value());

    if (peer_id_string) {
        auto peer_id = urldecode(peer_id_string);
        if (peer_id.length() != 20) {
            out("'{}' is not a valid peer_id; it should be 20 characters after decoding\n", peer_id_string);
            return 1;
        }
        request.set_peer_id(peer_id.to_byte_buffer());
    } else {
        request.set_peer_id(String("XX_00000000000000000").to_byte_buffer());
    }

    if (ip_string) {
        auto ip = IPv4Address::from_string(ip_string);
        if (!ip.has_value()) {
            out("'{}' is not a valid ip\n", ip_string);
            return 1;
        }
        request.set_ip(ip.value());
    }

    if (port != 0)
        request.set_port(port);
    else
        request.set_port(6881);

    request.set_uploaded(uploaded);
    request.set_downloaded(downloaded);
    request.set_left(left);

    if (numwant > 0)
        request.set_numwant(numwant);

    if (compact)
        request.set_compact(true);

    Core::EventLoop loop;

    request.on_error = [&loop]() {
        out("Announce failed!\n");
        loop.quit(1);
    };
    request.on_success = [&loop](AnnounceResponse&& response) {
        for (auto& peer : response.peers()) {
            if (peer.id().is_empty())
                out("{}:{}\n", peer.ip().to_string(), peer.port());
            else
                out("{}:{} ({})\n", peer.ip().to_string(), peer.port(), urlencode(peer.id()));
        }
        loop.quit(0);
    };

    HTTPTracker tracker(tracker_url);

    tracker.announce(request);

    return loop.exec();
}
