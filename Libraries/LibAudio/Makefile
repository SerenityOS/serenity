include ../../Makefile.common

OBJS = \
    AClientConnection.o \
    AWavLoader.o

LIBRARY = libaudio.a
DEFINES += -DUSERLAND

all: $(LIBRARY)

$(LIBRARY): $(OBJS)
	@echo "LIB $@"; $(AR) rcs $@ $(OBJS) $(LIBS)

.cpp.o:
	@echo "CXX $<"; $(CXX) $(CXXFLAGS) -o $@ -c $<

-include $(OBJS:%.o=%.d)

clean:
	@echo "CLEAN"; rm -f $(LIBRARY) $(OBJS) *.d

install: $(LIBRARY)
	mkdir -p ../../Root/usr/include/LibAudio
	mkdir -p ../../Root/usr/include/AK
	mkdir -p ../../Root/usr/lib
	# Copy headers
	rsync -r -a --include '*/' --include '*.h' --exclude '*' . ../../Root/usr/include/LibAudio
	rsync -r -a --include '*/' --include '*.h' --exclude '*' ../../AK/ ../../Root/usr/include/AK
	# Install the library
	cp $(LIBRARY) ../../Root/usr/lib
