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

#include <AK/IPv4Address.h>
#include <AK/Optional.h>
#include <AK/SharedBuffer.h>
#include <AK/StringBuilder.h>
#include <AK/URL.h>
#include <AK/URLParser.h>
#include <LibBencode/Dictionary.h>
#include <LibBencode/List.h>
#include <LibBencode/Parser.h>
#include <LibBencode/Value.h>
#include <LibBitTorrent/HTTPTracker.h>
#include <LibBitTorrent/Tracker.h>
#include <LibProtocol/Client.h>
#include <LibProtocol/Download.h>

void HTTPTracker::announce(const AnnounceRequest& request)
{
    URL request_url(url().to_string());

    StringBuilder query_string;
    if (!request_url.query().is_empty())
        query_string.appendff("{}&", request_url.query());

    query_string.appendff("info_hash={}&", urlencode(request.info_hash()));
    query_string.appendff("peer_id={}&", urlencode(request.peer_id()));
    if (!request.ip().is_zero())
        query_string.appendff("ip={}&", request.ip().to_string());
    query_string.appendff("port={}&", request.port());
    query_string.appendff("uploaded={}&", request.uploaded());
    query_string.appendff("downloaded={}&", request.downloaded());
    query_string.appendff("left={}&", request.left());
    if (request.numwant() != 0)
        query_string.appendff("numwant={}&", request.numwant());
    if (request.compact())
        query_string.append("compact=1");

    request_url.set_query(query_string.build());

    if (!m_protocol_client) {
        if (request.on_error)
            request.on_error();
        return;
    }

    auto download = m_protocol_client->start_download("GET", request_url.to_string());
    if (!download) {
        if (request.on_error)
            request.on_error();
        return;
    }

    download->on_finish = [this, &request](bool success, auto& payload, auto, auto&, auto) {
        if (!success) {
            if (request.on_error)
                request.on_error();
            return;
        }

        Bencode::Parser parser(payload);

        auto value = parser.parse();
        if (!value.has_value()) {
            if (request.on_error)
                request.on_error();
            return;
        }

        auto response = parse_announce_response(value.value());
        if (!response.has_value()) {
            if (request.on_error)
                request.on_error();
            return;
        }

        if (request.on_success)
            request.on_success(move(response.value()));
    };
}

Optional<AnnounceResponse> HTTPTracker::parse_announce_response(const Bencode::Value& value) const
{
    AnnounceResponse response;

    if (!value.is_dictionary())
        return {};

    if (value.as_dictionary().has("failure reason")) {
        auto failure_reason_value = value.as_dictionary().get("failure reason");
        if (!failure_reason_value.is_string())
            return {};
        response.set_failure_reason(failure_reason_value.as_string());
        return response;
    }

    auto peers = value.as_dictionary().get("peers");
    if (!peers.is_list() && !peers.is_string())
        return {};

    if (peers.is_list()) {
        for (auto& peer_value : peers.as_list().values()) {
            if (!peer_value.is_dictionary())
                return {};

            AnnounceResponse::Peer peer;

            // technically optional
            auto id = peer_value.as_dictionary().get("id");
            if (id.is_string())
                peer.set_id(id.as_string().to_byte_buffer());

            auto ip_value = peer_value.as_dictionary().get("ip");
            if (!ip_value.is_string())
                return {};
            auto ip = IPv4Address::from_string(ip_value.as_string());
            if (!ip.has_value())
                return {};
            peer.set_ip(ip.value());

            auto port_value = peer_value.as_dictionary().get("port");
            if (!port_value.is_integer())
                return {};
            peer.set_port(port_value.as_integer());

            response.add_peer(move(peer));
        }
    }

    if (peers.is_string()) {
        auto peers_buffer = peers.as_string().to_byte_buffer();

        for (size_t i = 0; i < peers_buffer.size(); i += 6) {
            AnnounceResponse::Peer peer;

            peer.set_ip(IPv4Address(peers_buffer[i + 0], peers_buffer[i + 1], peers_buffer[i + 2], peers_buffer[i + 3]));
            peer.set_port((u16(peers_buffer[i + 4]) << 8) + peers_buffer[i + 5]);

            response.add_peer(move(peer));
        }
    }

    return response;
}

void HTTPTracker::scrape(const ScrapeRequest& request)
{
    URL request_url(url().to_string());

    String path = request_url.path();
    if (!path.contains("/announce")) {
        if (request.on_error)
            request.on_error();
        return;
    }
    path.replace("/announce", "/scrape");
    request_url.set_path(path);

    StringBuilder query_string;
    if (!request_url.query().is_empty())
        query_string.append(request_url.query());
    for (auto& info_hash : request.info_hashes()) {
        if (!query_string.is_empty())
            query_string.append("&");
        query_string.append("info_hash=");
        query_string.append(urlencode(info_hash));
    }
    request_url.set_query(query_string.build());

    if (!m_protocol_client) {
        if (request.on_error)
            request.on_error();
        return;
    }

    auto download = m_protocol_client->start_download("GET", request_url.to_string());
    if (!download) {
        if (request.on_error)
            request.on_error();
        return;
    }

    download->on_finish = [this, &request](bool success, auto& payload, auto, auto&, auto) {
        if (!success) {
            if (request.on_error)
                request.on_error();
            return;
        }

        Bencode::Parser parser(payload);

        auto value = parser.parse();
        if (!value.has_value()) {
            if (request.on_error)
                request.on_error();
            return;
        }

        auto response = parse_scrape_response(value.value());
        if (!response.has_value()) {
            if (request.on_error)
                request.on_error();
            return;
        }

        if (request.on_success)
            request.on_success(move(response.value()));
    };
}

Optional<ScrapeResponse> HTTPTracker::parse_scrape_response(const Bencode::Value& value) const
{
    ScrapeResponse response;

    if (!value.is_dictionary())
        return {};

    auto files = value.as_dictionary().get("files");
    if (!files.is_dictionary())
        return {};

    files.as_dictionary().for_each_member([&response](auto& key, auto& value) {
        if (!value.is_dictionary())
            return;

        ScrapeResponse::File file;

        file.set_info_hash(key.to_byte_buffer());

        auto complete_value = value.as_dictionary().get("complete");
        if (complete_value.is_integer())
            file.set_complete(complete_value.as_integer());

        auto downloaded_value = value.as_dictionary().get("downloaded");
        if (downloaded_value.is_integer())
            file.set_downloaded(downloaded_value.as_integer());

        auto incomplete_value = value.as_dictionary().get("incomplete");
        if (incomplete_value.is_integer())
            file.set_incomplete(incomplete_value.as_integer());

        auto name_value = value.as_dictionary().get("name");
        if (name_value.is_string())
            file.set_name(name_value.as_string());

        response.add_file(move(file));
    });

    return response;
}