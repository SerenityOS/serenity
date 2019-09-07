#include <AK/String.h>
#include <AK/FileSystemPath.h>
#include <AK/StringBuilder.h>
#include <LibCore/CArgsParser.h>
#include <assert.h>
#include <fcntl.h>
#include <stdio.h>
#include <sys/stat.h>
#include <unistd.h>

bool copy_file(String, String);

int main(int argc, char** argv)
{
    CArgsParser args_parser("cp");
    args_parser.add_required_single_value("source");
    args_parser.add_required_single_value("destination");

    CArgsParserResult args = args_parser.parse(argc, argv);
    Vector<String> values = args.get_single_values();
    if (values.size() == 0) {
        args_parser.print_usage();
        return 0;
    }
    String src_path = values[0];
    String dst_path = values[1];
    return copy_file(src_path, dst_path) ? 0 : 1;
}

/**
 * Copy a source file to a destination file. Returns true if successful, false 
 * otherwise. If there is an error, its description is output to stderr.
 */
bool copy_file(String src_path, String dst_path)
{
    int src_fd = open(src_path.characters(), O_RDONLY);
    if (src_fd < 0) {
        perror("open src");
        return false;
    }

    struct stat src_stat;
    int rc = fstat(src_fd, &src_stat);
    if (rc < 0) {
        perror("stat src");
        return false;
    }

    if (S_ISDIR(src_stat.st_mode)) {
        fprintf(stderr, "cp: FIXME: Copying directories is not yet supported\n");
        return false;
    }

    int dst_fd = creat(dst_path.characters(), 0666);
    if (dst_fd < 0) {
        if (errno != EISDIR) {
            perror("open dst");
            return false;
        }
        StringBuilder builder;
        builder.append(dst_path);
        builder.append('/');
        builder.append(FileSystemPath(src_path).basename());
        dst_path = builder.to_string();
        dst_fd = creat(dst_path.characters(), 0666);
        if (dst_fd < 0) {
            perror("open dst");
            return false;
        }
    }

    for (;;) {
        char buffer[BUFSIZ];
        ssize_t nread = read(src_fd, buffer, sizeof(buffer));
        if (nread < 0) {
            perror("read src");
            return false;
        }
        if (nread == 0)
            break;
        ssize_t remaining_to_write = nread;
        char* bufptr = buffer;
        while (remaining_to_write) {
            ssize_t nwritten = write(dst_fd, bufptr, remaining_to_write);
            if (nwritten < 0) {
                perror("write dst");
                return false;
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
        return false;
    }

    close(src_fd);
    close(dst_fd);
    return true;
}
