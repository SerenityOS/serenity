/*
 * Copyright (c) 2023, Agustin Gianni <agustingianni@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/NumericLimits.h>
#include <AK/Vector.h>
#include <LibCore/EventLoop.h>
#include <LibCore/Stream.h>
#include <LibCore/System.h>
#include <LibTest/TestCase.h>
#include <arpa/inet.h>
#include <errno.h>

static ErrorOr<ssize_t> sendmsg_helper(const struct msghdr* msg)
{
    auto fd = TRY(Core::System::socket(AF_INET, SOCK_DGRAM, 0));

    sockaddr_in addr = {};
    addr.sin_family = AF_INET;
    addr.sin_port = htons(3333);
    addr.sin_addr.s_addr = INADDR_ANY;

    auto connectError = Core::System::connect(fd, (struct sockaddr const*)&addr, sizeof(addr));
    if (connectError.is_error()) {
        TRY(Core::System::close(fd));
        return connectError.error();
    }

    auto sendmsgError = Core::System::sendmsg(fd, msg, 0);
    if (sendmsgError.is_error()) {
        TRY(Core::System::close(fd));
        return sendmsgError.error();
    }

    TRY(Core::System::close(fd));

    return sendmsgError.release_value();
}

static ErrorOr<ssize_t> recvmsg_write_helper(Vector<Bytes> messages)
{
    Vector<iovec, 1> vectors;
    vectors.resize(messages.size());

    for (size_t i = 0; i < messages.size(); i++) {
        vectors[i].iov_base = (void*)messages[i].data();
        vectors[i].iov_len = messages[i].size();
    }

    msghdr msg = {};
    msg.msg_iov = vectors.data();
    msg.msg_iovlen = vectors.size();

    return sendmsg_helper(&msg);
}

static ErrorOr<ssize_t> recvmsg_read_helper(struct msghdr* msg)
{
    auto fd = TRY(Core::System::socket(AF_INET, SOCK_DGRAM, 0));

    sockaddr_in addr = {};
    addr.sin_family = AF_INET;
    addr.sin_port = htons(3333);
    addr.sin_addr.s_addr = INADDR_ANY;

    auto bindError = Core::System::bind(fd, (const struct sockaddr*)&addr, sizeof(addr));
    if (bindError.is_error()) {
        TRY(Core::System::close(fd));
        return bindError.error();
    }

    auto recvmsgError = Core::System::recvmsg(fd, msg, 0);
    if (recvmsgError.is_error()) {
        TRY(Core::System::close(fd));
        return recvmsgError.error();
    }

    TRY(Core::System::close(fd));

    return recvmsgError.release_value();
}

TEST_CASE(recvmsg_negative_msg_iovlen)
{
    struct msghdr msg = {};
    msg.msg_iovlen = -1;
    EXPECT(recvmsg_read_helper(&msg).is_error());
    EXPECT_EQ(errno, EMSGSIZE);
}

TEST_CASE(recvmsg_zero_msg_iovlen)
{
    struct msghdr msg = {};
    msg.msg_iovlen = 0;
    EXPECT(recvmsg_read_helper(&msg).is_error());
    EXPECT_EQ(errno, EMSGSIZE);
}

TEST_CASE(recvmsg_gt_iov_max_msg_iovlen)
{
    struct msghdr msg = {};
    msg.msg_iovlen = IOV_MAX + 1;
    EXPECT(recvmsg_read_helper(&msg).is_error());
    EXPECT_EQ(errno, EMSGSIZE);
}

TEST_CASE(recvmsg_nullptr_msg_iov)
{
    struct msghdr msg = {};
    msg.msg_iov = nullptr;
    msg.msg_iovlen = 1;
    EXPECT(recvmsg_read_helper(&msg).is_error());
    EXPECT_EQ(errno, EFAULT);
}

TEST_CASE(recvmsg_total_length_overflow)
{
    char buffer[32];

    struct iovec iov[2];
    iov[0].iov_base = buffer;
    iov[0].iov_len = NumericLimits<ssize_t>::max();

    iov[1].iov_base = buffer;
    iov[1].iov_len = 1;

    struct msghdr msg = {};
    msg.msg_iov = iov;
    msg.msg_iovlen = 2;

    EXPECT(recvmsg_read_helper(&msg).is_error());
    EXPECT_EQ(errno, EINVAL);
}

TEST_CASE(recvmsg_simple_msg)
{
    char buffer[32];

    struct iovec iov = {};
    iov.iov_base = buffer;
    iov.iov_len = sizeof(buffer);

    struct msghdr msg = {};
    msg.msg_iov = &iov;
    msg.msg_iovlen = 1;

    auto writeOrError = recvmsg_write_helper({ { buffer, sizeof(buffer) } });
    EXPECT(!writeOrError.is_error());

    auto ret = recvmsg_read_helper(&msg);
    EXPECT(!ret.is_error());
    EXPECT_EQ(ret.release_value(), (ssize_t)sizeof(buffer));
}

TEST_CASE(recvmsg_complex_msg)
{
    char buffer[32];

    struct iovec iov[2] = {};
    iov[0].iov_base = buffer;
    iov[0].iov_len = sizeof(buffer);

    iov[1].iov_base = buffer;
    iov[1].iov_len = sizeof(buffer);

    struct msghdr msg = {};
    msg.msg_iov = iov;
    msg.msg_iovlen = 2;

    auto writeOrError = recvmsg_write_helper({ { buffer, sizeof(buffer) }, { buffer, sizeof(buffer) } });
    EXPECT(!writeOrError.is_error());

    auto ret = recvmsg_read_helper(&msg);
    EXPECT(!ret.is_error());
    EXPECT_EQ(ret.release_value(), (ssize_t)sizeof(buffer) * 2);
}
