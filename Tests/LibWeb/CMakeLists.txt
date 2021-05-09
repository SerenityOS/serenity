add_executable(test-web test-web.cpp)
target_link_libraries(test-web LibCore LibWeb)
install(TARGETS test-web RUNTIME DESTINATION bin)
