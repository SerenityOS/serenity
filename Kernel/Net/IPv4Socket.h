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

#include <AK/HashMap.h>
#include <AK/SinglyLinkedList.h>
#include <Kernel/DoubleBuffer.h>
#include <Kernel/KBuffer.h>
#include <Kernel/Lock.h>
#include <Kernel/Net/IPv4.h>
#include <Kernel/Net/IPv4SocketTuple.h>
#include <Kernel/Net/Socket.h>

namespace Kernel {

class NetworkAdapter;
class TCPPacket;
class TCPSocket;

class IPv4Socket : public Socket {
public:
    static KResultOr<NonnullRefPtr<Socket>> create(int type, int protocol);
    virtual ~IPv4Socket() override;

    static Lockable<HashTable<IPv4Socket*>>& all_sockets();

    virtual void close() override;
    virtual KResult bind(const sockaddr*, socklen_t) override;
    virtual KResult connect(FileDescription&, const sockaddr*, socklen_t, ShouldBlock = ShouldBlock::Yes) override;
    virtual KResult listen(int) override;
    virtual void get_local_address(sockaddr*, socklen_t*) override;
    virtual void get_peer_address(sockaddr*, socklen_t*) override;
    virtual void attach(FileDescription&) override;
    virtual void detach(FileDescription&) override;
    virtual bool can_read(const FileDescription&) const override;
    virtual bool can_write(const FileDescription&) const override;
    virtual ssize_t sendto(FileDescription&, const void*, size_t, int, const sockaddr*, socklen_t) override;
    virtual ssize_t recvfrom(FileDescription&, void*, size_t, int flags, sockaddr*, socklen_t*) override;
    virtual KResult setsockopt(int level, int option, const void*, socklen_t) override;
    virtual KResult getsockopt(FileDescription&, int level, int option, void*, socklen_t*) override;

    virtual int ioctl(FileDescription&, unsigned request, unsigned arg) override;

    bool did_receive(const IPv4Address& peer_address, u16 peer_port, KBuffer&&);

    const IPv4Address& local_address() const { return m_local_address; }
    u16 local_port() const { return m_local_port; }
    void set_local_port(u16 port) { m_local_port = port; }
    bool has_specific_local_address() { return m_local_address.to_u32() != 0; }

    const IPv4Address& peer_address() const { return m_peer_address; }
    u16 peer_port() const { return m_peer_port; }
    void set_peer_port(u16 port) { m_peer_port = port; }

    IPv4SocketTuple tuple() const { return IPv4SocketTuple(m_local_address, m_local_port, m_peer_address, m_peer_port); }

    String absolute_path(const FileDescription& description) const override;

    u8 ttl() const { return m_ttl; }

    enum class BufferMode {
        Packets,
        Bytes,
    };
    BufferMode buffer_mode() const { return m_buffer_mode; }

protected:
    IPv4Socket(int type, int protocol);
    virtual const char* class_name() const override { return "IPv4Socket"; }

    int allocate_local_port_if_needed();

    virtual KResult protocol_bind() { return KSuccess; }
    virtual KResult protocol_listen() { return KSuccess; }
    virtual int protocol_receive(const KBuffer&, void*, size_t, int) { return -ENOTIMPL; }
    virtual int protocol_send(const void*, size_t) { return -ENOTIMPL; }
    virtual KResult protocol_connect(FileDescription&, ShouldBlock) { return KSuccess; }
    virtual int protocol_allocate_local_port() { return 0; }
    virtual bool protocol_is_disconnected() const { return false; }

    virtual void shut_down_for_reading() override;

    void set_local_address(IPv4Address address) { m_local_address = address; }
    void set_peer_address(IPv4Address address) { m_peer_address = address; }

private:
    virtual bool is_ipv4() const override { return true; }

    ssize_t receive_byte_buffered(FileDescription&, void* buffer, size_t buffer_length, int flags, sockaddr*, socklen_t*);
    ssize_t receive_packet_buffered(FileDescription&, void* buffer, size_t buffer_length, int flags, sockaddr*, socklen_t*);

    IPv4Address m_local_address;
    IPv4Address m_peer_address;

    struct ReceivedPacket {
        IPv4Address peer_address;
        u16 peer_port;
        Optional<KBuffer> data;
    };

    SinglyLinkedList<ReceivedPacket> m_receive_queue;

    DoubleBuffer m_receive_buffer;

    u16 m_local_port { 0 };
    u16 m_peer_port { 0 };

    u32 m_bytes_received { 0 };

    u8 m_ttl { 64 };

    bool m_can_read { false };

    BufferMode m_buffer_mode { BufferMode::Packets };

    Optional<KBuffer> m_scratch_buffer;
};

}
