#include <AK/AKString.h>
#include <AK/FileSystemPath.h>
#include <AK/StringBuilder.h>
#include <assert.h>
#include <fcntl.h>
#include <stdio.h>
#include <sys/stat.h>
#include <unistd.h>

int main(int argc, char** argv)
{
    if (argc != 3) {
        printf("usage: cp <source> <destination>\n");
        return 0;
    }
    String src_path = argv[1];
    String dst_path = argv[2];

    int src_fd = open(src_path.characters(), O_RDONLY);
    if (src_fd < 0) {
        perror("open src");
        return 1;
    }

    struct stat src_stat;
    int rc = fstat(src_fd, &src_stat);
    if (rc < 0) {
        perror("stat src");
        return 1;
    }

    if (S_ISDIR(src_stat.st_mode)) {
        fprintf(stderr, "cp: FIXME: Copying directories is not yet supported\n");
        return 1;
    }

    int dst_fd = creat(dst_path.characters(), 0666);
    if (dst_fd < 0) {
        if (errno != EISDIR) {
            perror("open dst");
            return 1;
        }
        StringBuilder builder;
        builder.append(dst_path);
        builder.append('/');
        builder.append(FileSystemPath(src_path).basename());
        dst_path = builder.to_string();
        dst_fd = creat(dst_path.characters(), 0666);
        if (dst_fd < 0) {
            perror("open dst");
            return 1;
        }
    }

    for (;;) {
        char buffer[BUFSIZ];
        ssize_t nread = read(src_fd, buffer, sizeof(buffer));
        if (nread < 0) {
            perror("read src");
            return 1;
        }
        if (nread == 0)
            break;
        ssize_t remaining_to_write = nread;
        char* bufptr = buffer;
        while (remaining_to_write) {
            ssize_t nwritten = write(dst_fd, bufptr, remaining_to_write);
            if (nwritten < 0) {
                perror("write dst");
                return 1;
            }
            assert(nwritten > 0);
            remaining_to_write -= nwritten;
            bufptr += nwritten;
        }
    }

    auto my_umask = umask(0);
    umask(my_umask);
    rc = fchmod(dst_fd, src_stat.st_mode & ~my_umask);
    if (rc < 0) {
        perror("fchmod dst");
        return 1;
    }

    close(src_fd);
    close(dst_fd);
    return 0;
}
