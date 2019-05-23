include ../Makefile.common

AK_OBJS = \
    ../AK/StringImpl.o \
    ../AK/String.o \
    ../AK/StringView.o \
    ../AK/StringBuilder.o \
    ../AK/FileSystemPath.o \
    ../AK/StdLibExtras.o \
    ../AK/MappedFile.o

LIBC_OBJS = \
       SharedBuffer.o \
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
       sched.o

ASM_OBJS = setjmp.ao crti.ao crtn.ao

CPP_OBJS = $(AK_OBJS) $(WIDGETS_OBJS) $(LIBC_OBJS)

LIBRARY = libc.a
DEFINES += -DUSERLAND

all: $(LIBRARY) startfiles

startfiles:
	@echo "CXX crt0.o"; $(CXX) $(CXXFLAGS) -o crt0.o -c crt0.cpp
	cp crti.ao crti.o
	cp crtn.ao crtn.o

$(LIBRARY): $(CPP_OBJS) $(ASM_OBJS)
	@echo "LIB $@"; $(AR) rcs $@ $(CPP_OBJS) $(ASM_OBJS)

.cpp.o:
	@echo "CXX $<"; $(CXX) $(CXXFLAGS) -o $@ -c $<

%.ao: %.S
	@echo "AS $@"; $(AS) -o $@ $<

-include $(OBJS:%.o=%.d)

clean:
	@echo "CLEAN"; rm -f $(LIBRARY) $(CPP_OBJS) $(ASM_OBJS) *.d

install: $(LIBRARY)
	mkdir -p ../Root/usr/include
	mkdir -p ../Root/usr/lib
	# Copy headers
	rsync -r -a --include '*/' --include '*.h' --exclude '*' . ../Root/usr/include
	# Install the library
	cp $(LIBRARY) ../Root/usr/lib
	cp crt0.o ../Root/usr/lib/
	cp crti.ao ../Root/usr/lib/crti.o
	cp crtn.ao ../Root/usr/lib/crtn.o
