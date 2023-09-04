/*
 * Copyright (c) 2022, Idan Horowitz <idan.horowitz@serenityos.org>
 * Copyright (c) 2023, Tim Flynn <trflynn89@serenityos.org>
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
SegmenterConstructor::SegmenterConstructor(Realm& realm)
    : NativeFunction(realm.vm().names.Segmenter.as_string(), realm.intrinsics().function_prototype())
{
}

void SegmenterConstructor::initialize(Realm& realm)
{
    Base::initialize(realm);

    auto& vm = this->vm();

    // 18.2.1 Intl.Segmenter.prototype, https://tc39.es/ecma402/#sec-intl.segmenter.prototype
    define_direct_property(vm.names.prototype, realm.intrinsics().intl_segmenter_prototype(), 0);
    define_direct_property(vm.names.length, Value(0), Attribute::Configurable);

    u8 attr = Attribute::Writable | Attribute::Configurable;
    define_native_function(realm, vm.names.supportedLocalesOf, supported_locales_of, 1, attr);
}

// 18.1.1 Intl.Segmenter ( [ locales [ , options ] ] ), https://tc39.es/ecma402/#sec-intl.segmenter
ThrowCompletionOr<Value> SegmenterConstructor::call()
{
    // 1. If NewTarget is undefined, throw a TypeError exception.
    return vm().throw_completion<TypeError>(ErrorType::ConstructorWithoutNew, "Intl.Segmenter");
}

// 18.1.1 Intl.Segmenter ( [ locales [ , options ] ] ), https://tc39.es/ecma402/#sec-intl.segmenter
ThrowCompletionOr<NonnullGCPtr<Object>> SegmenterConstructor::construct(FunctionObject& new_target)
{
    auto& vm = this->vm();

    auto locales = vm.argument(0);
    auto options_value = vm.argument(1);

    // 2. Let internalSlotsList be « [[InitializedSegmenter]], [[Locale]], [[SegmenterGranularity]] ».
    // 3. Let segmenter be ? OrdinaryCreateFromConstructor(NewTarget, "%Segmenter.prototype%", internalSlotsList).
    auto segmenter = TRY(ordinary_create_from_constructor<Segmenter>(vm, new_target, &Intrinsics::intl_segmenter_prototype));

    // 4. Let requestedLocales be ? CanonicalizeLocaleList(locales).
    auto requested_locales = TRY(canonicalize_locale_list(vm, locales));

    // 5. Set options to ? GetOptionsObject(options).
    auto* options = TRY(Temporal::get_options_object(vm, options_value));

    // 6. Let opt be a new Record.
    LocaleOptions opt {};

    // 7. Let matcher be ? GetOption(options, "localeMatcher", string, « "lookup", "best fit" », "best fit").
    auto matcher = TRY(get_option(vm, *options, vm.names.localeMatcher, OptionType::String, { "lookup"sv, "best fit"sv }, "best fit"sv));

    // 8. Set opt.[[localeMatcher]] to matcher.
    opt.locale_matcher = matcher;

    // 9. Let localeData be %Segmenter%.[[LocaleData]].

    // 10. Let r be ResolveLocale(%Segmenter%.[[AvailableLocales]], requestedLocales, opt, %Segmenter%.[[RelevantExtensionKeys]], localeData).
    auto result = resolve_locale(requested_locales, opt, {});

    // 11. Set segmenter.[[Locale]] to r.[[locale]].
    segmenter->set_locale(move(result.locale));

    // 12. Let granularity be ? GetOption(options, "granularity", string, « "grapheme", "word", "sentence" », "grapheme").
    auto granularity = TRY(get_option(vm, *options, vm.names.granularity, OptionType::String, { "grapheme"sv, "word"sv, "sentence"sv }, "grapheme"sv));

    // 13. Set segmenter.[[SegmenterGranularity]] to granularity.
    segmenter->set_segmenter_granularity(granularity.as_string().utf8_string_view());

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
    auto requested_locales = TRY(canonicalize_locale_list(vm, locales));

    // 3. Return ? SupportedLocales(availableLocales, requestedLocales, options).
    return TRY(supported_locales(vm, requested_locales, options));
}

}
