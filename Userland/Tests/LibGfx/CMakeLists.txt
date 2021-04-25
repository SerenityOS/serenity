file(GLOB CMD_SOURCES  CONFIGURE_DEPENDS "*.cpp")
list(REMOVE_ITEM CMD_SOURCES ${CMAKE_CURRENT_SOURCE_DIR}/painter.cpp)

# FIXME These tests do not use LibTest
foreach(CMD_SRC ${CMD_SOURCES})
    get_filename_component(CMD_NAME ${CMD_SRC} NAME_WE)
    add_executable(${CMD_NAME} ${CMD_SRC})
    target_link_libraries(${CMD_NAME} LibCore LibGUI)
    install(TARGETS ${CMD_NAME} RUNTIME DESTINATION usr/Tests/LibGfx)
endforeach()

serenity_test(painter.cpp LibGfx LIBS LibGUI)
