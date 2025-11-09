/*
 * Copyright (c) 2025, Kusekushi <0kusekushi0@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <errno.h>
#include <fcntl.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/wait.h>
#include <unistd.h>

static void dump_fd_table(char const* tag)
{
    printf("test-rfork: %s fd table:\n", tag);
    for (int fd = 0; fd <= 20; ++fd) {
        if (fcntl(fd, F_GETFD) != -1) {
            printf("  fd %d: valid\n", fd);
        } else {
            printf("  fd %d: invalid (errno=%d %s)\n", fd, errno, strerror(errno));
        }
    }
    fflush(stdout);
}

int main()
{
    printf("test-rfork: starting\n");

    printf("test-rfork: Running basic RFPROC test\n");
    pid_t pid = rfork(RFPROC);
    if (pid < 0) {
        perror("rfork");
        return 1;
    }
    if (pid == 0) {
        // child
        printf("test-rfork: child: exiting with 42\n");
        _exit(42);
    }
    // parent
    int status = 0;
    pid_t w = waitpid(pid, &status, 0);
    if (w < 0) {
        perror("waitpid");
        return 1;
    }
    if (!(WIFEXITED(status) && WEXITSTATUS(status) == 42)) {
        printf("test-rfork: basic RFPROC test FAILED\n");
        return 1;
    }
    printf("test-rfork: basic RFPROC test SUCCESS\n");

    printf("test-rfork: Running RFCFDG test\n");
    int pipefd[2];
    if (pipe(pipefd) < 0) {
        perror("pipe");
        return 1;
    }
    printf("test-rfork: parent pipe fds = [%d, %d]\n", pipefd[0], pipefd[1]);
    fflush(stdout);
    dump_fd_table("before RFCFDG");
    pid = rfork(RFPROC | RFCFDG);
    if (pid < 0) {
        perror("rfork");
        close(pipefd[0]);
        close(pipefd[1]);
        return 1;
    }
    if (pid == 0) {
        if (fcntl(pipefd[0], F_GETFD) == -1 && errno == EBADF) {
            printf("test-rfork: child (RFCFDG): no inherited fds - OK\n");
            _exit(0);
        }
        printf("test-rfork: child (RFCFDG): unexpectedly inherited fds (fcntl returned %d errno=%d %s)\n", -1, errno, strerror(errno));
        _exit(2);
    }
    int status2 = 0;
    pid_t w2 = waitpid(pid, &status2, 0);
    if (w2 < 0) {
        perror("waitpid");
        close(pipefd[0]);
        close(pipefd[1]);
        return 1;
    }
    if (WIFEXITED(status2) && WEXITSTATUS(status2) == 0) {
        printf("test-rfork: RFCFDG test SUCCESS\n");
    } else {
        printf("test-rfork: RFCFDG test FAILURE (child exit=%d)\n", WIFEXITED(status2) ? WEXITSTATUS(status2) : -1);
        close(pipefd[0]);
        close(pipefd[1]);
        return 1;
    }
    close(pipefd[0]);
    close(pipefd[1]);

    printf("test-rfork: Running RFFDG test\n");
    int pipefd2[2];
    if (pipe(pipefd2) < 0) {
        perror("pipe");
        return 1;
    }
    printf("test-rfork: parent pipe2 fds = [%d, %d]\n", pipefd2[0], pipefd2[1]);
    fflush(stdout);
    dump_fd_table("before RFFDG");
    pid = rfork(RFPROC | RFFDG);
    if (pid < 0) {
        perror("rfork");
        close(pipefd2[0]);
        close(pipefd2[1]);
        return 1;
    }
    if (pid == 0) {
        if (fcntl(pipefd2[0], F_GETFD) != -1) {
            printf("test-rfork: child (RFFDG): inherited fds - OK\n");
            _exit(0);
        }
        printf("test-rfork: child (RFFDG): did not inherit fds (fcntl returned %d errno=%d %s)\n", -1, errno, strerror(errno));
        _exit(3);
    }
    int status2b = 0;
    pid_t w2b = waitpid(pid, &status2b, 0);
    if (w2b < 0) {
        perror("waitpid");
        close(pipefd2[0]);
        close(pipefd2[1]);
        return 1;
    }
    if (WIFEXITED(status2b) && WEXITSTATUS(status2b) == 0) {
        printf("test-rfork: RFFDG test SUCCESS\n");
    } else {
        printf("test-rfork: RFFDG test FAILURE (child exit=%d)\n", WIFEXITED(status2b) ? WEXITSTATUS(status2b) : -1);
        close(pipefd2[0]);
        close(pipefd2[1]);
        return 1;
    }
    close(pipefd2[0]);
    close(pipefd2[1]);

    printf("test-rfork: Running shared FD table mutation test\n");
    int mainpipe[2];
    int syncpipe[2];
    if (pipe(mainpipe) < 0) {
        perror("pipe");
        return 1;
    }
    if (pipe(syncpipe) < 0) {
        perror("pipe");
        close(mainpipe[0]);
        close(mainpipe[1]);
        return 1;
    }
    printf("test-rfork: parent mainpipe fds = [%d, %d], syncpipe fds = [%d, %d]\n", mainpipe[0], mainpipe[1], syncpipe[0], syncpipe[1]);
    fflush(stdout);
    dump_fd_table("before shared-rfork");
    pid = rfork(RFPROC);
    if (pid < 0) {
        perror("rfork");
        close(mainpipe[0]);
        close(mainpipe[1]);
        close(syncpipe[0]);
        close(syncpipe[1]);
        return 1;
    }
    if (pid == 0) {
        char b;
        if (read(syncpipe[0], &b, 1) != 1) {
            perror("read");
            _exit(6);
        }
        if (fcntl(mainpipe[0], F_GETFD) == -1 && errno == EBADF) {
            printf("test-rfork: shared mutation: child sees parent's close - OK\n");
            _exit(0);
        }
        printf("test-rfork: shared mutation: child still sees fd - FAILURE (fcntl returned %d errno=%d %s)\n", -1, errno, strerror(errno));
        _exit(7);
    }
    if (close(mainpipe[0]) < 0) {
        perror("close");
        close(mainpipe[1]);
        close(syncpipe[0]);
        close(syncpipe[1]);
        return 1;
    }
    if (write(syncpipe[1], "x", 1) != 1) {
        perror("write");
        close(mainpipe[1]);
        close(syncpipe[0]);
        close(syncpipe[1]);
        return 1;
    }
    int status_shared = 0;
    pid_t w_shared = waitpid(pid, &status_shared, 0);
    if (w_shared < 0) {
        perror("waitpid");
        close(mainpipe[1]);
        close(syncpipe[0]);
        close(syncpipe[1]);
        return 1;
    }
    if (WIFEXITED(status_shared) && WEXITSTATUS(status_shared) == 0)
        printf("test-rfork: shared FD mutation test SUCCESS\n");
    else {
        printf("test-rfork: shared FD mutation test FAILURE (child exit=%d)\n", WIFEXITED(status_shared) ? WEXITSTATUS(status_shared) : -1);
        close(mainpipe[1]);
        close(syncpipe[0]);
        close(syncpipe[1]);
        return 1;
    }
    close(mainpipe[1]);
    close(syncpipe[0]);
    close(syncpipe[1]);

    printf("test-rfork: Running RFNOWAIT test\n");
    pid = rfork(RFPROC | RFNOWAIT);
    if (pid < 0) {
        perror("rfork");
        return 1;
    }
    if (pid == 0) {
        _exit(77);
    }
    errno = 0;
    int status3 = 0;
    pid_t w3 = waitpid(pid, &status3, 0);
    if (w3 == -1 && errno == ECHILD) {
        printf("test-rfork: RFNOWAIT test SUCCESS (waitpid failed with ECHILD)\n");
    } else {
        if (w3 == -1)
            printf("test-rfork: RFNOWAIT test FAILURE (waitpid returned -1 errno=%d %s)\n", errno, strerror(errno));
        else
            printf("test-rfork: RFNOWAIT test FAILURE (waitpid returned %d)\n", (int)w3);
        return 1;
    }

    printf("test-rfork: all tests PASSED\n");
    return 0;
}
