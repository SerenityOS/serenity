if (NOT ${CMAKE_SYSTEM_NAME} MATCHES "Darwin")
    add_subdirectory(LinkDemo)
    add_subdirectory(LinkLib)
endif()
