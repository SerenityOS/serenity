add_compile_options(-O2)

add_subdirectory(Libraries)

# FIXME: Add the rest of the directories for aarch64.
if (NOT "${SERENITY_ARCH}" STREQUAL "aarch64")
	add_subdirectory(Applications)
	add_subdirectory(Demos)
	add_subdirectory(DevTools)
	add_subdirectory(DynamicLoader)
	add_subdirectory(Games)
	add_subdirectory(Applets)
	add_subdirectory(Services)
	add_subdirectory(Shell)
	add_subdirectory(Utilities)
endif ()