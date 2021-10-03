serenity_testjs_test(test-js.cpp test-js)

install(TARGETS test-js RUNTIME DESTINATION bin OPTIONAL)

serenity_test(test-invalid-unicode-js.cpp LibJS LIBS LibJS)
