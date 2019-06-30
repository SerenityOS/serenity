include ../Makefile.common

APPLICATIONS = About \
			   Downloader \
			   FileManager \
			   FontEditor \
			   IRCClient \
			   Launcher \
			   PaintBrush \
			   ProcessManager \
			   QuickShow \
			   Taskbar \
			   Terminal \
			   TextEditor

APPLICATIONS_BUILD = ${APPLICATIONS:=.out}
APPLICATIONS_INSTALL = ${APPLICATIONS:=.install}
APPLICATIONS_CLEAN = ${APPLICATIONS:=.clean}

all: $(APPLICATIONS_BUILD)

install: $(APPLICATIONS_INSTALL)

clean: $(APPLICATIONS_CLEAN)

%.out: %
	@echo -e "\033[1mBuilding $^...\033[0m"
	$(CXX) $(CXXFLAGS) $(LDFLAGS) ${wildcard $^/*.cpp} -lgui -lcore -lc -o $@
	@echo -e "\033[33;1mDone building $^ !\033[0m"

%.install: %.out
	@echo -e "\033[1mInstalling $^...\033[0m"	
	cp $^ $(SYSROOT)/bin/$(patsubst %.out,%,$^)
	@echo -e "\033[33;1mDone installing $^!\033[0m"

%.clean: %
	@echo -e "\033[1mCleanning $^...\033[0m"
	@rm -f $^.out
	@rm -f $^.d
	@echo -e "\033[33;1mDone cleanning $^!\033[0m"
	@echo ""	

-include %.d