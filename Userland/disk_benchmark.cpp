#include <AK/ByteBuffer.h>
#include <AK/String.h>
#include <AK/Types.h>
#include <AK/Vector.h>
#include <LibCore/CElapsedTimer.h>
#include <getopt.h>
#include <stdio.h>
#include <stdlib.h>
#include <sys/stat.h>

struct Result {
    int write_bps;
    int read_bps;
};

Result average_result(const Vector<Result>& results)
{
    Result average;

    for (auto& res : results) {
        average.write_bps += res.write_bps;
        average.read_bps += res.read_bps;
    }

    average.write_bps /= results.size();
    average.read_bps /= results.size();

    return average;
}

void exit_with_usage(int rc)
{
    fprintf(stderr, "Usage: disk_benchmark [-h] [-d directory] [-t time_per_benchmark] [-f file_size1,file_size2,...] [-b block_size1,block_size2,...]\n");
    exit(rc);
}

Result benchmark(const String& filename, int file_size, int block_size, ByteBuffer& buffer);

int main(int argc, char** argv)
{
    char* directory = strdup(".");
    int time_per_benchmark = 10;
    Vector<int> file_sizes;
    Vector<int> block_sizes;

    int opt;
    while ((opt = getopt(argc, argv, "hd:t:f:b:")) != -1) {
        switch (opt) {
        case 'h':
            exit_with_usage(0);
            break;
        case 'd':
            directory = strdup(optarg);
            break;
        case 't':
            time_per_benchmark = atoi(optarg);
            break;
        case 'f':
            for (auto size : String(optarg).split(','))
                file_sizes.append(atoi(size.characters()));
            break;
        case 'b':
            for (auto size : String(optarg).split(','))
                block_sizes.append(atoi(size.characters()));
            break;
        }
    }

    if (file_sizes.size() == 0) {
        file_sizes = { 131072, 262144, 524288, 1048576, 5242880 };
    }
    if (block_sizes.size() == 0) {
        block_sizes = { 8192, 32768, 65536 };
    }

    umask(0644);

    auto filename = String::format("%s/disk_benchmark", directory);

    for (auto file_size : file_sizes) {
        for (auto block_size : block_sizes) {
            if (block_size > file_size)
                continue;

            auto buffer = ByteBuffer::create_uninitialized(block_size);

            Vector<Result> results;

            printf("Running: file_size=%d block_size=%d\n", file_size, block_size);
            CElapsedTimer timer;
            timer.start();
            while (timer.elapsed() < time_per_benchmark * 1000) {
                printf(".");
                fflush(stdout);
                results.append(benchmark(filename, file_size, block_size, buffer));
                usleep(100);
            }
            auto average = average_result(results);
            printf("\nFinished: runs=%d time=%dms write_bps=%d read_bps=%d\n", results.size(), timer.elapsed(), average.write_bps, average.read_bps);

            sleep(1);
        }
    }

    if (isatty(0)) {
        printf("Press any key to exit...\n");
        fgetc(stdin);
    }
}

Result benchmark(const String& filename, int file_size, int block_size, ByteBuffer& buffer)
{
    int fd = open(filename.characters(), O_CREAT | O_TRUNC | O_WRONLY);
    if (fd == -1) {
        perror("open");
        exit(1);
    }

    auto cleanup_and_exit = [fd, filename]() {
        close(fd);
        unlink(filename.characters());
        exit(1);
    };

    Result res;

    CElapsedTimer timer;

    timer.start();
    int nwrote = 0;
    for (int j = 0; j < file_size; j += block_size) {
        int n = write(fd, buffer.pointer(), block_size);
        if (n < 0) {
            perror("write");
            cleanup_and_exit();
        }
        nwrote += n;
    }
    res.write_bps = (file_size / timer.elapsed()) * 1000;

    if (lseek(fd, 0, SEEK_SET) < 0) {
        perror("lseek");
        cleanup_and_exit();
    }

    timer.start();
    int nread = 0;
    while (nread < file_size) {
        int n = read(fd, buffer.pointer(), block_size);
        if (n < 0) {
            perror("read");
            cleanup_and_exit();
        }
        nread += n;
    }
    res.read_bps = (file_size / timer.elapsed()) * 1000;

    if (close(fd) != 0) {
        perror("close");
        cleanup_and_exit();
    }

    if (unlink(filename.characters()) != 0) {
        perror("unlink");
        cleanup_and_exit();
    }

    return res;
}
