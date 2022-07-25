add_subdirectory(GMLPlayground)
add_subdirectory(Inspector)
add_subdirectory(Profiler)
add_subdirectory(HackStudio)
add_subdirectory(SQLStudio)

if("${SERENITY_ARCH}" STREQUAL "i686")
	add_subdirectory(UserspaceEmulator)
endif()
