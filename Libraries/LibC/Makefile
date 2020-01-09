AK_OBJS = \
    ../../AK/StringImpl.o \
    ../../AK/String.o \
    ../../AK/StringView.o \
    ../../AK/StringBuilder.o \
    ../../AK/FileSystemPath.o \
    ../../AK/URL.o \
    ../../AK/JsonValue.o \
    ../../AK/JsonParser.o \
    ../../AK/LogStream.o \
    ../../AK/MappedFile.o \
    ../../AK/SharedBuffer.o \
    ../../AK/Utf8View.o

LIBC_OBJS = \
       stdio.o \
       unistd.o \
       string.o \
       strings.o \
       mman.o \
       dirent.o \
       malloc.o \
       stdlib.o \
       time.o \
       utsname.o \
       assert.o \
       signal.o \
       getopt.o \
       scanf.o \
       pwd.o \
       grp.o \
       times.o \
       termcap.o \
       stat.o \
       mntent.o \
       ctype.o \
       fcntl.o \
       termios.o \
       ulimit.o \
       qsort.o \
       ioctl.o \
       utime.o \
       sys/select.o \
       sys/socket.o \
       sys/wait.o \
       sys/uio.o \
       poll.o \
       locale.o \
       arpa/inet.o \
       netdb.o \
       sched.o \
       dlfcn.o \
       libgen.o \
       wchar.o \
       serenity.o \
       syslog.o \
       cxxabi.o \
       libcinit.o

ELF_OBJS = \
        ../LibELF/ELFDynamicObject.o \
        ../LibELF/ELFDynamicLoader.o \
        ../LibELF/ELFImage.o

OBJS = $(AK_OBJS) $(LIBC_OBJS) $(ELF_OBJS)

EXTRA_OBJS = \
        setjmp.ao \
        crti.ao \
        crtn.ao  \
        ../LibELF/Arch/i386/plt_trampoline.ao

crt0.o: crt0.cpp

crtio.o: crti.ao
	$(QUIET) cp crti.ao crti.o

crtn.o: crtin.ao
	$(QUIET) cp crtn.ao crtn.o

EXTRA_CLEAN = crt0.d crt0.o

DEFINES = -DSERENITY_LIBC_BUILD

LIBRARY = libc.a

POST_LIBRARY_BUILD = $(QUIET) $(MAKE) install

all: crt0.o $(EXTRA_OBJS) $(LIBRARY)

install:
	mkdir -p $(SERENITY_BASE_DIR)/Root/usr/include/sys/
	mkdir -p $(SERENITY_BASE_DIR)/Root/usr/include/bits/
	mkdir -p $(SERENITY_BASE_DIR)/Root/usr/include/netinet/
	mkdir -p $(SERENITY_BASE_DIR)/Root/usr/include/arpa/
	mkdir -p $(SERENITY_BASE_DIR)/Root/usr/lib/
	cp *.h $(SERENITY_BASE_DIR)/Root/usr/include/
	cp sys/*.h $(SERENITY_BASE_DIR)/Root/usr/include/sys/
	cp bits/*.h $(SERENITY_BASE_DIR)/Root/usr/include/bits/
	cp arpa/*.h $(SERENITY_BASE_DIR)/Root/usr/include/arpa/
	cp netinet/*.h $(SERENITY_BASE_DIR)/Root/usr/include/netinet/
	cp libc.a $(SERENITY_BASE_DIR)/Root/usr/lib/
	cp crt0.o $(SERENITY_BASE_DIR)/Root/usr/lib/
	cp crti.ao $(SERENITY_BASE_DIR)/Root/usr/lib/crti.o
	cp crtn.ao $(SERENITY_BASE_DIR)/Root/usr/lib/crtn.o

include ../../Makefile.common
