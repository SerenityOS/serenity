add_executable(test-web test-web.cpp)
target_link_libraries(test-web LibCore LibWeb)
install(TARGETS ${CMD_NAME} RUNTIME DESTINATION bin)
