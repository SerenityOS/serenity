add_subdirectory(Inspector)
add_subdirectory(Playground)
add_subdirectory(Profiler)
add_subdirectory(HackStudio)

if("${SERENITY_ARCH}" STREQUAL "i686")
	add_subdirectory(UserspaceEmulator)
endif()
