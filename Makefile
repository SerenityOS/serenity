# Build the host-side tools first, since they are needed to build some programs.
SUBDIRS = \
	DevTools/IPCCompiler \
	DevTools/FormCompiler \
	Libraries/LibHTML/CodeGenerators/Generate_CSS_PropertyID_cpp \
	Libraries/LibHTML/CodeGenerators/Generate_CSS_PropertyID_h

# Build LibC, LibCore, LibIPC and LibThread before IPC servers, since they depend on them.
SUBDIRS += \
	Libraries/LibC \
	Libraries/LibCore \
	Libraries/LibDraw \
	Libraries/LibIPC \
	Libraries/LibThread \
	Libraries/LibPthread

# Build IPC servers before their client code to ensure the IPC definitions are available.
SUBDIRS += \
	Servers/AudioServer \
	Servers/LookupServer \
	Servers/ProtocolServer \
	Libraries/LibAudio \
	Servers/WindowServer

SUBDIRS += \
	AK

SUBDIRS += \
	Libraries \
	Applications \
	DevTools \
	Servers \
	Shell \
	Userland \
	MenuApplets \
	Demos \
	Games \
	Kernel

include Makefile.subdir

.PHONY: test
test:
	$(QUIET) $(MAKE) -C AK/Tests clean all clean
