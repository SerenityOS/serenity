add_subdirectory(Inspector)
add_subdirectory(Playground)
add_subdirectory(Profiler)

if("${SERENITY_ARCH}" STREQUAL "i686")
	add_subdirectory(HackStudio)
	add_subdirectory(UserspaceEmulator)
endif()
