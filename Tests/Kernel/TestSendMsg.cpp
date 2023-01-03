/*
 * Copyright (c) 2023, Agustin Gianni <agustingianni@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/NumericLimits.h>
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

TEST_CASE(sendmsg_total_length_overflow)
{
    char data = 'A';

    struct iovec iov[2];
    iov[0].iov_base = &data;
    iov[0].iov_len = NumericLimits<ssize_t>::max();

    iov[1].iov_base = &data;
    iov[1].iov_len = sizeof(data);

    struct msghdr msg = {};
    msg.msg_iov = iov;
    msg.msg_iovlen = 2;

    EXPECT(sendmsg_helper(&msg).is_error());
    EXPECT_EQ(errno, EINVAL);
}

TEST_CASE(sendmsg_msg_iovlen_zero)
{
    char data = 'A';

    struct iovec iov = {};
    iov.iov_base = &data;
    iov.iov_len = sizeof(data);

    struct msghdr msg = {};
    msg.msg_iov = &iov;
    msg.msg_iovlen = 0;

    EXPECT(sendmsg_helper(&msg).is_error());
    EXPECT_EQ(errno, EMSGSIZE);
}

TEST_CASE(sendmsg_msg_iovlen_gt_iov_max)
{
    char data = 'A';

    struct iovec iov = {};
    iov.iov_base = &data;
    iov.iov_len = sizeof(data);

    struct msghdr msg = {};
    msg.msg_iov = &iov;
    msg.msg_iovlen = IOV_MAX + 1;

    EXPECT(sendmsg_helper(&msg).is_error());
    EXPECT_EQ(errno, EMSGSIZE);
}

TEST_CASE(sendmsg_msg_iovlen_negative)
{
    char data = 'A';

    struct iovec iov = {};
    iov.iov_base = &data;
    iov.iov_len = sizeof(data);

    struct msghdr msg = {};
    msg.msg_iov = &iov;
    msg.msg_iovlen = -1;

    EXPECT(sendmsg_helper(&msg).is_error());
    EXPECT_EQ(errno, EMSGSIZE);
}

TEST_CASE(sendmsg_single_message)
{
    char data = 'A';

    struct iovec iov = {};
    iov.iov_base = &data;
    iov.iov_len = sizeof(data);

    struct msghdr msg = {};
    msg.msg_iov = &iov;
    msg.msg_iovlen = 1;

    auto ret = sendmsg_helper(&msg);
    EXPECT(!ret.is_error());
    EXPECT_EQ(ret.value(), 1);
}

TEST_CASE(sendmsg_multiple_messages)
{
    char data = 'A';

    struct iovec iov[2] = {};
    iov[0].iov_base = &data;
    iov[0].iov_len = sizeof(data);

    iov[1].iov_base = &data;
    iov[1].iov_len = sizeof(data);

    struct msghdr msg = {};
    msg.msg_iov = iov;
    msg.msg_iovlen = 2;

    auto ret = sendmsg_helper(&msg);
    EXPECT(!ret.is_error());
    EXPECT_EQ(ret.value(), 2);
}

TEST_CASE(sendmsg_multiple_messages_large)
{
    char data[4096] = {};

    struct iovec iov[2] = {};
    iov[0].iov_base = data;
    iov[0].iov_len = sizeof(data);

    iov[1].iov_base = data;
    iov[1].iov_len = sizeof(data);

    struct msghdr msg = {};
    msg.msg_iov = iov;
    msg.msg_iovlen = 2;

    auto ret = sendmsg_helper(&msg);
    EXPECT(!ret.is_error());
    EXPECT_EQ(ret.value(), (off_t)sizeof(data) * 2);
}

TEST_CASE(sendmsg_multiple_messages_with_holes)
{
    char data = 'A';

    struct iovec iov[3] = {};
    iov[0].iov_base = &data;
    iov[0].iov_len = sizeof(data);

    iov[1].iov_base = nullptr;
    iov[1].iov_len = 0;

    iov[2].iov_base = &data;
    iov[2].iov_len = sizeof(data);

    struct msghdr msg = {};
    msg.msg_iov = iov;
    msg.msg_iovlen = 3;

    auto ret = sendmsg_helper(&msg);
    EXPECT(!ret.is_error());
    EXPECT_EQ(ret.value(), 2);
}

TEST_CASE(sendmsg_empty_vector)
{
    char data = 'A';

    struct iovec iov = {};
    iov.iov_base = &data;
    iov.iov_len = 0;

    struct msghdr msg = {};
    msg.msg_iov = &iov;
    msg.msg_iovlen = 1;

    auto ret = sendmsg_helper(&msg);
    EXPECT(!ret.is_error());
    EXPECT_EQ(ret.value(), 0);
}
