PROGRAM = test

AK_OBJS = \
    ../AK/String.o \
    ../AK/StringImpl.o \
    ../AK/MappedFile.o \
    ../AK/TemporaryFile.o \
    ../AK/SimpleMalloc.o \
    ../AK/StringBuilder.o \
    ../AK/kmalloc.o

VFS_OBJS = \
    DiskDevice.o \
    FileBackedDiskDevice.o \
    FileSystem.o \
    Ext2FileSystem.o \
    VirtualFileSystem.o \
    FileHandle.o \
    DiskBackedFileSystem.o \
    SyntheticFileSystem.o \
    InodeIdentifier.o \
    CharacterDevice.o \
    ZeroDevice.o \
    NullDevice.o \
    FullDevice.o \
    RandomDevice.o \
    test.o

OBJS = $(AK_OBJS) $(VFS_OBJS)

CXXFLAGS = -std=c++17 -O0 -W -Wall -Wextra -Wconversion -I. -I.. -ggdb3 -Wno-class-memaccess

all:	$(PROGRAM)

.cpp.o:
	$(CXX) $(CXXFLAGS) -o $@ -c $<

clean:
	rm -f $(OBJS) $(PROGRAM)

$(PROGRAM): $(OBJS)
	$(CXX) $(LDFLAGS) -o $@ $(OBJS)

