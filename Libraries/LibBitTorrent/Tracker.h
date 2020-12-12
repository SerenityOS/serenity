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

#pragma once

#include <AK/ByteBuffer.h>
#include <AK/Function.h>
#include <AK/IPv4Address.h>
#include <AK/Optional.h>
#include <AK/URL.h>
#include <LibBencode/Value.h>

class ScrapeRequest;
class AnnounceRequest;

class Tracker {
public:
    Tracker(URL url)
        : m_url(url) {};

    URL url() const { return m_url; }
    void set_url(URL url) { m_url = url; }

    virtual void scrape(const ScrapeRequest& request) = 0;
    virtual void announce(const AnnounceRequest& request) = 0;

private:
    URL m_url;
};

class ScrapeResponse {
public:
    class File {
    public:
        ByteBuffer info_hash() const { return m_info_hash; }
        int complete() const { return m_complete; }
        int downloaded() const { return m_downloaded; }
        int incomplete() const { return m_incomplete; }
        Optional<String> name() const { return m_name; }

        void set_info_hash(ByteBuffer info_hash) { m_info_hash = info_hash; }
        void set_complete(int complete) { m_complete = complete; }
        void set_downloaded(int downloaded) { m_downloaded = downloaded; }
        void set_incomplete(int incomplete) { m_incomplete = incomplete; }
        void set_name(Optional<String> name) { m_name = name; }

    private:
        ByteBuffer m_info_hash;
        int m_complete { 0 };
        int m_downloaded { 0 };
        int m_incomplete { 0 };
        Optional<String> m_name;
    };

    const Vector<File> files() const { return m_files; }
    const Optional<File> get_file(ByteBuffer info_hash) const;
    void add_file(File&& file);
    void remove_file(ByteBuffer info_hash);

private:
    Vector<File> m_files;
};

class ScrapeRequest {
public:
    const Vector<ByteBuffer> info_hashes() const { return m_info_hashes; }

    void add_info_hash(ByteBuffer&& info_hash) { m_info_hashes.append(move(info_hash)); }

    void remove_info_hash(ByteBuffer to_remove)
    {
        m_info_hashes.remove_first_matching([&to_remove](ByteBuffer& info_hash) {
            return info_hash == to_remove;
        });
    }

    Function<void(ScrapeResponse&&)> on_success;
    Function<void()> on_error;

private:
    Vector<ByteBuffer> m_info_hashes;
};

class AnnounceResponse {
public:
    class Peer {
    public:
        ByteBuffer id() const { return m_id; }
        IPv4Address ip() const { return m_ip; }
        u16 port() const { return m_port; }

        void set_id(ByteBuffer id) { m_id = id; }
        void set_ip(IPv4Address ip) { m_ip = ip; }
        void set_port(u16 port) { m_port = port; }

    private:
        ByteBuffer m_id;
        IPv4Address m_ip;
        u16 m_port { 0 };
    };

    int interval() const { return m_interval; }
    Vector<Peer> peers() const { return m_peers; }
    String failure_reason() const { return m_failure_reason; }

    void set_interval(int interval) { m_interval = interval; }
    void set_peers(Vector<Peer> peers) { m_peers = peers; }
    void set_failure_reason(String failure_reason) { m_failure_reason = failure_reason; }

    void add_peer(Peer&& peer) { m_peers.append(move(peer)); }

private:
    int m_interval { 0 };
    Vector<Peer> m_peers;
    String m_failure_reason;
};

class AnnounceRequest {
public:
    enum class Event {
        None,
        Started,
        Completed,
        Stopped,
    };

    Event event() const { return m_event; }
    ByteBuffer info_hash() const { return m_info_hash; }
    ByteBuffer peer_id() const { return m_peer_id; }
    IPv4Address ip() const { return m_ip; }
    u16 port() const { return m_port; }
    u64 uploaded() const { return m_uploaded; }
    u64 downloaded() const { return m_downloaded; }
    u64 left() const { return m_left; }
    u64 numwant() const { return m_numwant; }
    bool compact() const { return m_compact; }

    void set_event(Event event) { m_event = event; }
    void set_info_hash(ByteBuffer info_hash) { m_info_hash = info_hash; }
    void set_peer_id(ByteBuffer peer_id) { m_peer_id = peer_id; }
    void set_ip(IPv4Address ip) { m_ip = ip; }
    void set_port(u16 port) { m_port = port; }
    void set_uploaded(u64 uploaded) { m_uploaded = uploaded; }
    void set_downloaded(u64 downloaded) { m_downloaded = downloaded; }
    void set_left(u64 left) { m_left = left; }
    void set_numwant(u64 numwant) { m_numwant = numwant; }
    void set_compact(bool compact) { m_compact = compact; }

    Function<void(AnnounceResponse&&)> on_success;
    Function<void()> on_error;

private:
    Event m_event;
    ByteBuffer m_info_hash;
    ByteBuffer m_peer_id;
    IPv4Address m_ip;
    u16 m_port { 0 };
    u64 m_uploaded { 0 };
    u64 m_downloaded { 0 };
    u64 m_left { 0 };
    u64 m_numwant { 0 };
    bool m_compact { false };
};


