set(TEST_SOURCES
    ${CMAKE_CURRENT_SOURCE_DIR}/snprintf-correctness.cpp
    ${CMAKE_CURRENT_SOURCE_DIR}/strlcpy-correctness.cpp
    ${CMAKE_CURRENT_SOURCE_DIR}/TestLibCTime.cpp
    ${CMAKE_CURRENT_SOURCE_DIR}/TestLibCMkTemp.cpp
    ${CMAKE_CURRENT_SOURCE_DIR}/TestLibCExec.cpp
    ${CMAKE_CURRENT_SOURCE_DIR}/TestLibCDirEnt.cpp
    ${CMAKE_CURRENT_SOURCE_DIR}/TestLibCInodeWatcher.cpp
    ${CMAKE_CURRENT_SOURCE_DIR}/TestLibCString.cpp
)

file(GLOB CMD_SOURCES  CONFIGURE_DEPENDS "*.cpp")
list(REMOVE_ITEM CMD_SOURCES ${TEST_SOURCES})

# FIXME: These tests do not use LibTest
foreach(CMD_SRC ${CMD_SOURCES})
    get_filename_component(CMD_NAME ${CMD_SRC} NAME_WE)
    add_executable(${CMD_NAME} ${CMD_SRC})
    target_link_libraries(${CMD_NAME} LibCore)
    install(TARGETS ${CMD_NAME} RUNTIME DESTINATION usr/Tests/LibC)
endforeach()

foreach(source ${TEST_SOURCES})
    serenity_test(${source} LibC)
endforeach()
