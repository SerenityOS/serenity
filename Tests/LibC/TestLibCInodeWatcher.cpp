/*
 * Copyright (c) 2021, sin-ack <sin-ack@protonmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/NumericLimits.h>
#include <Kernel/API/InodeWatcherEvent.h>
#include <Kernel/API/InodeWatcherFlags.h>
#include <LibTest/TestCase.h>
#include <errno.h>
#include <fcntl.h>
#include <stdio.h>
#include <unistd.h>
#include <utime.h>

u8 buffer[MAXIMUM_EVENT_SIZE];
InodeWatcherEvent* event = reinterpret_cast<InodeWatcherEvent*>(buffer);

static int read_event(int fd)
{
    int rc = read(fd, &buffer, MAXIMUM_EVENT_SIZE);
    return rc;
}

static ByteString get_event_name()
{
    if (event->name_length == 0)
        return ByteString();

    return ByteString { event->name, event->name_length - 1 };
}

TEST_CASE(inode_watcher_metadata_modified_event)
{
    int fd = create_inode_watcher(0);
    EXPECT_NE(fd, -1);

    int test_fd = creat("/tmp/testfile", 0777);
    EXPECT_NE(test_fd, -1);

    int wd = inode_watcher_add_watch(fd, "/tmp/testfile", 13, static_cast<unsigned>(InodeWatcherEvent::Type::MetadataModified));
    EXPECT_NE(wd, -1);

    // "touch" the file
    int rc = utime("/tmp/testfile", nullptr);
    EXPECT_NE(rc, -1);

    rc = read_event(fd);
    EXPECT_EQ(event->watch_descriptor, wd);
    EXPECT_EQ(event->type, InodeWatcherEvent::Type::MetadataModified);

    close(fd);
    close(test_fd);
    unlink("/tmp/testfile");
}

TEST_CASE(inode_watcher_content_modified_event)
{
    int fd = create_inode_watcher(0);
    EXPECT_NE(fd, -1);

    int test_fd = creat("/tmp/testfile", 0777);
    EXPECT_NE(test_fd, -1);

    int wd = inode_watcher_add_watch(fd, "/tmp/testfile", 13, static_cast<unsigned>(InodeWatcherEvent::Type::ContentModified));
    EXPECT_NE(wd, -1);

    int rc = write(test_fd, "test", 4);
    EXPECT_NE(rc, -1);

    rc = read_event(fd);
    EXPECT_NE(rc, -1);
    EXPECT_EQ(event->watch_descriptor, wd);
    EXPECT_EQ(event->type, InodeWatcherEvent::Type::ContentModified);

    close(fd);
    close(test_fd);
    unlink("/tmp/testfile");
}

TEST_CASE(inode_watcher_deleted_event)
{
    int fd = create_inode_watcher(0);
    EXPECT_NE(fd, -1);

    int test_fd = creat("/tmp/testfile", 0777);
    EXPECT_NE(test_fd, -1);

    int wd = inode_watcher_add_watch(fd, "/tmp/testfile", 13, static_cast<unsigned>(InodeWatcherEvent::Type::Deleted));
    EXPECT_NE(wd, -1);

    int rc = unlink("/tmp/testfile");
    EXPECT_NE(rc, -1);

    rc = read_event(fd);
    EXPECT_NE(rc, -1);
    EXPECT_EQ(event->watch_descriptor, wd);
    EXPECT_EQ(event->type, InodeWatcherEvent::Type::Deleted);

    close(fd);
    close(test_fd);
}

TEST_CASE(inode_watcher_child_events)
{
    int fd = create_inode_watcher(0);
    EXPECT_NE(fd, -1);

    int wd = inode_watcher_add_watch(fd, "/tmp/", 5, static_cast<unsigned>(InodeWatcherEvent::Type::ChildCreated | InodeWatcherEvent::Type::ChildDeleted));
    EXPECT_NE(fd, -1);

    int rc = creat("/tmp/testfile", 0777);
    EXPECT_NE(rc, -1);

    rc = read_event(fd);
    EXPECT_NE(rc, -1);
    EXPECT_EQ(event->watch_descriptor, wd);
    EXPECT_EQ(event->type, InodeWatcherEvent::Type::ChildCreated);
    VERIFY(event->name_length > 0);
    EXPECT_EQ(get_event_name(), "testfile");

    rc = unlink("/tmp/testfile");
    EXPECT_NE(rc, -1);

    rc = read_event(fd);
    EXPECT_NE(rc, -1);
    EXPECT_EQ(event->watch_descriptor, wd);
    EXPECT_EQ(event->type, InodeWatcherEvent::Type::ChildDeleted);
    VERIFY(event->name_length > 0);
    EXPECT_EQ(get_event_name(), "testfile");

    close(fd);
}

TEST_CASE(inode_watcher_closes_children_on_close)
{
    int fd = create_inode_watcher(0);
    EXPECT_NE(fd, -1);

    int test_fd = creat("/tmp/testfile", 0777);
    EXPECT_NE(test_fd, -1);
    int wd = inode_watcher_add_watch(fd, "/tmp/testfile", 13, static_cast<unsigned>(InodeWatcherEvent::Type::MetadataModified));
    EXPECT_NE(wd, -1);

    int rc = utime("/tmp/testfile", nullptr);
    EXPECT_NE(rc, -1);

    close(fd);

    rc = read_event(fd);
    EXPECT_EQ(rc, -1);
    EXPECT_EQ(errno, EBADF);

    close(test_fd);
    unlink("/tmp/testfile");
}

TEST_CASE(inode_watcher_nonblock)
{
    int fd = create_inode_watcher(static_cast<unsigned>(InodeWatcherFlags::Nonblock));
    EXPECT_NE(fd, -1);

    int rc = read_event(fd);
    EXPECT_EQ(rc, -1);
    EXPECT_EQ(errno, EAGAIN);

    close(fd);
}
