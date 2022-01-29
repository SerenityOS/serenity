/*
 * Copyright (c) 2022, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/AbstractOperations.h>
#include <LibJS/Runtime/Array.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Intl/AbstractOperations.h>
#include <LibJS/Runtime/Intl/Segmenter.h>
#include <LibJS/Runtime/Intl/SegmenterConstructor.h>
#include <LibJS/Runtime/Temporal/AbstractOperations.h>

namespace JS::Intl {

// 18.1 The Intl.Segmenter Constructor, https://tc39.es/ecma402/#sec-intl-segmenter-constructor
SegmenterConstructor::SegmenterConstructor(GlobalObject& global_object)
    : NativeFunction(vm().names.Segmenter.as_string(), *global_object.function_prototype())
{
}

void SegmenterConstructor::initialize(GlobalObject& global_object)
{
    NativeFunction::initialize(global_object);

    auto& vm = this->vm();

    // 18.2.1 Intl.Segmenter.prototype, https://tc39.es/ecma402/#sec-intl.segmenter.prototype
    define_direct_property(vm.names.prototype, global_object.intl_segmenter_prototype(), 0);
    define_direct_property(vm.names.length, Value(0), Attribute::Configurable);

    u8 attr = Attribute::Writable | Attribute::Configurable;
    define_native_function(vm.names.supportedLocalesOf, supported_locales_of, 1, attr);
}

// 18.1.1 Intl.Segmenter ( [ locales [ , options ] ] ), https://tc39.es/ecma402/#sec-intl.segmenter
ThrowCompletionOr<Value> SegmenterConstructor::call()
{
    // 1. If NewTarget is undefined, throw a TypeError exception.
    return vm().throw_completion<TypeError>(global_object(), ErrorType::ConstructorWithoutNew, "Intl.Segmenter");
}

// 18.1.1 Intl.Segmenter ( [ locales [ , options ] ] ), https://tc39.es/ecma402/#sec-intl.segmenter
ThrowCompletionOr<Object*> SegmenterConstructor::construct(FunctionObject& new_target)
{
    auto& vm = this->vm();
    auto& global_object = this->global_object();

    auto locales = vm.argument(0);
    auto options_value = vm.argument(1);

    // 2. Let internalSlotsList be « [[InitializedSegmenter]], [[Locale]], [[SegmenterGranularity]] ».
    // 3. Let segmenter be ? OrdinaryCreateFromConstructor(NewTarget, "%Segmenter.prototype%", internalSlotsList).
    auto* segmenter = TRY(ordinary_create_from_constructor<Segmenter>(global_object, new_target, &GlobalObject::intl_segmenter_prototype));

    // 4. Let requestedLocales be ? CanonicalizeLocaleList(locales).
    auto requested_locales = TRY(canonicalize_locale_list(global_object, locales));

    // 5. Set options to ? GetOptionsObject(options).
    auto* options = TRY(Temporal::get_options_object(global_object, options_value));

    // 6. Let opt be a new Record.
    LocaleOptions opt {};

    // 7. Let matcher be ? GetOption(options, "localeMatcher", "string", « "lookup", "best fit" », "best fit").
    auto matcher = TRY(get_option(global_object, *options, vm.names.localeMatcher, Value::Type::String, { "lookup"sv, "best fit"sv }, "best fit"sv));

    // 8. Set opt.[[localeMatcher]] to matcher.
    opt.locale_matcher = matcher;

    // 9. Let localeData be %Segmenter%.[[LocaleData]].

    // 10. Let r be ResolveLocale(%Segmenter%.[[AvailableLocales]], requestedLocales, opt, %Segmenter%.[[RelevantExtensionKeys]], localeData).
    auto result = resolve_locale(requested_locales, opt, {});

    // 11. Set segmenter.[[Locale]] to r.[[locale]].
    segmenter->set_locale(move(result.locale));

    // 12. Let granularity be ? GetOption(options, "granularity", "string", « "grapheme", "word", "sentence" », "grapheme").
    auto granularity = TRY(get_option(global_object, *options, vm.names.granularity, Value::Type::String, { "grapheme"sv, "word"sv, "sentence"sv }, "grapheme"sv));

    // 13. Set segmenter.[[SegmenterGranularity]] to granularity.
    segmenter->set_segmenter_granularity(granularity.as_string().string());

    // 14. Return segmenter.
    return segmenter;
}

// 18.2.2 Intl.Segmenter.supportedLocalesOf ( locales [ , options ] ), https://tc39.es/ecma402/#sec-intl.segmenter.supportedlocalesof
JS_DEFINE_NATIVE_FUNCTION(SegmenterConstructor::supported_locales_of)
{
    auto locales = vm.argument(0);
    auto options = vm.argument(1);

    // 1. Let availableLocales be %Segmenter%.[[AvailableLocales]].

    // 2. Let requestedLocales be ? CanonicalizeLocaleList(locales).
    auto requested_locales = TRY(canonicalize_locale_list(global_object, locales));

    // 3. Return ? SupportedLocales(availableLocales, requestedLocales, options).
    return TRY(supported_locales(global_object, requested_locales, options));
}

}
