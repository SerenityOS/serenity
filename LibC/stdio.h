#pragma once

#include <sys/cdefs.h>
#include <sys/types.h>
#include <limits.h>

__BEGIN_DECLS
 #ifndef EOF
#define EOF (-1)
#endif

#define SEEK_SET 0
#define SEEK_CUR 1
#define SEEK_END 2

#define _IOFBF 0
#define _IOLBF 1
#define _IONBF 2

struct __STDIO_FILE {
    int fd;
    int eof;
    int error;
    int mode;
    char* buffer;
    size_t buffer_size;
    size_t buffer_index;
    char default_buffer[BUFSIZ];
};

typedef struct __STDIO_FILE FILE;

extern FILE* stdin;
extern FILE* stdout;
extern FILE* stderr;

char* fgets(char* buffer, int size, FILE*);
int fileno(FILE*);
int fgetc(FILE*);
int getc(FILE*);
int getchar();
FILE* fdopen(int fd, const char* mode);
FILE* fopen(const char* pathname, const char* mode);
int fclose(FILE*);
void rewind(FILE*);
void clearerr(FILE*);
int ferror(FILE*);
int feof(FILE*);
int fflush(FILE*);
size_t fread(void* ptr, size_t size, size_t nmemb, FILE*);
size_t fwrite(const void* ptr, size_t size, size_t nmemb, FILE*);
int fprintf(FILE*, const char* fmt, ...);
int printf(const char* fmt, ...);
int sprintf(char* buffer, const char* fmt, ...);
int putchar(int ch);
int putc(int ch, FILE*);
int puts(const char*);
int fputs(const char*, FILE*);
void perror(const char*);
int sscanf (const char* buf, const char* fmt, ...);
int fscanf(FILE*, const char* fmt, ...);
int setvbuf(FILE*, char* buf, int mode, size_t);
void setbuf(FILE*, char* buf);
void setlinebuf(FILE*, char* buf);

__END_DECLS

