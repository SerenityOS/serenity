include ../Makefile.common

OBJS = math.o

LIBRARY = libm.a
DEFINES += -DUSERLAND

all: $(LIBRARY)

$(LIBRARY): $(OBJS)
	@echo "LIB $@"; $(AR) rcs $@ $(OBJS)

.cpp.o:
	@echo "CXX $<"; $(CXX) $(CXXFLAGS) -o $@ -c $<

%.no: %.asm
	@echo "NASM $@"; nasm -f elf -o $@ $<

-include $(OBJS:%.o=%.d)

clean:
	@echo "CLEAN"; rm -f $(LIBRARY) $(OBJS) *.d

install: $(LIBRARY)
	# Copy headers
	rsync -a --include '*/' --include '*.h' --exclude '*' . ../Base/usr/include
	# Install the library
	cp $(LIBRARY) ../Base/usr/lib