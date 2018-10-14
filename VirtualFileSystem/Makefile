PROGRAM = test

AK_OBJS = \
    ../AK/String.o \
    ../AK/StringImpl.o \
    ../AK/MappedFile.o \
    ../AK/TemporaryFile.o \
    ../AK/SimpleMalloc.o \
    ../AK/kmalloc.o

VFS_OBJS = \
    BlockDevice.o \
    FileBackedBlockDevice.o \
    FileSystem.o \
    Ext2FileSystem.o \
    VirtualFileSystem.o \
    FileHandle.o \
    DeviceBackedFileSystem.o \
    SyntheticFileSystem.o \
    InodeIdentifier.o \
    CharacterDevice.o \
    ZeroDevice.o \
    NullDevice.o \
    FullDevice.o \
    test.o

OBJS = $(AK_OBJS) $(VFS_OBJS)

CXXFLAGS = -std=c++17 -Os -W -Wall -Wextra -Wconversion -I. -I.. -ggdb3 -Wno-class-memaccess

#test.o: BlockDevice.h FileBackedBlockDevice.h FileSystem.h Ext2FileSystem.h VirtualFileSystem.h FileHandle.h

all:	$(PROGRAM)

.cpp.o:
	$(CXX) $(CXXFLAGS) -o $@ -c $<

clean:
	rm -f $(OBJS) $(PROGRAM)

$(PROGRAM): $(OBJS)
	$(CXX) $(LDFLAGS) -o $@ $(OBJS)

