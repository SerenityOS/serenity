serenity_testjs_test(test-js.cpp test-js)

install(TARGETS test-js RUNTIME DESTINATION bin OPTIONAL)
link_with_unicode_data(test-js)

serenity_test(test-invalid-unicode-js.cpp LibJS LIBS LibJS)
link_with_unicode_data(test-invalid-unicode-js)

serenity_test(test-bytecode-js.cpp LibJS LIBS LibJS)
link_with_unicode_data(test-bytecode-js)
