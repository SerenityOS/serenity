/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package org.reactivestreams.tck.flow.support;

import org.reactivestreams.tck.SubscriberBlackboxVerification;

/**
 * Internal TCK use only.
 * Add / Remove tests for SubscriberBlackboxVerification here to make sure that they arre added/removed in the other places.
 */
public interface SubscriberBlackboxVerificationRules {
  /**
   * Asks for a {@code Subscriber} instance, expects it to call {@code request()} in
   * a timely manner and signals as many {@code onNext} items as the very first request
   * amount specified by the {@code Subscriber}.
   * <p>
   * <b>Verifies rule:</b> <a href='https://github.com/reactive-streams/reactive-streams-jvm#2.1'>2.1</a>
   * <p>
   * Notes:
   * <ul>
   * <li>This test emits the number of items requested thus the {@code Subscriber} implementation
   * should not request too much.</li>
   * <li>Only the very first {@code request} amount is considered.</li>
   * <li>This test doesn't signal {@code onComplete} after the first set of {@code onNext} signals
   * has been emitted and may cause resource leak in
   * {@code Subscriber}s that expect a finite {@code Publisher}.</li>
   * <li>The test ignores cancellation from the {@code Subscriber} and emits the requested amount regardless.</li>
   * </ul>
   * <p>
   * If this test fails, the following could be checked within the {@code Subscriber} implementation:
   * <ul>
   * <li>if the {@code Subscriber} requires external stimulus to begin requesting; override the
   * {@link SubscriberBlackboxVerification#triggerRequest(org.reactivestreams.Subscriber)} method
   *  in this case,</li>
   * <li>the {@code TestEnvironment} has large enough timeout specified in case the {@code Subscriber} has some time-delay behavior,</li>
   * <li>if the {@code Subscriber} requests zero or a negative value in some circumstances,</li>
   * <li>if the {@code Subscriber} throws an unchecked exception from its {@code onSubscribe} or
   * {@code onNext} methods.
   * </ul>
   */
  void required_spec201_blackbox_mustSignalDemandViaSubscriptionRequest() throws Throwable;
  /**
   * Currently, this test is skipped because there is no agreed upon approach how
   * to detect if the {@code Subscriber} really goes async or just responds in
   * a timely manner.
   * <p>
   * <b>Verifies rule:</b> <a href='https://github.com/reactive-streams/reactive-streams-jvm#2.2'>2.2</a>
   */
  void untested_spec202_blackbox_shouldAsynchronouslyDispatch() throws Exception;
  /**
   * Asks for a {@code Subscriber}, signals an {@code onSubscribe} followed by an {@code onComplete} synchronously,
   * and checks if neither {@code request} nor {@code cancel} was called from within the {@code Subscriber}'s
   * {@code onComplete} implementation.
   * <p>
   * <b>Verifies rule:</b> <a href='https://github.com/reactive-streams/reactive-streams-jvm#2.3'>2.3</a>
   * <p>
   * Notes:
   * <ul>
   * <li>The test checks for the presensce of method named "onComplete" in the current stacktrace when handling
   * the {@code request} or {@code cancel} calls in the test's own {@code Subscription}.
   * </ul>
   * <p>
   * If this test fails, the following could be checked within the {@code Subscriber} implementation:
   * <ul>
   * <li>no calls happen to {@code request} or {@code cancel} in response to an {@code onComplete}
   * directly or indirectly,</li>
   * <li>if the {@code Subscriber} throws an unchecked exception from its {@code onSubscribe} or
   * {@code onComplete} methods.
   * </ul>
   */
  void required_spec203_blackbox_mustNotCallMethodsOnSubscriptionOrPublisherInOnComplete() throws Throwable;
  /**
   * Asks for a {@code Subscriber}, signals an {@code onSubscribe} followed by an {@code onError} synchronously,
   * and checks if neither {@code request} nor {@code cancel} was called from within the {@code Subscriber}'s
   * {@code onComplete} implementation.
   * <p>
   * <b>Verifies rule:</b> <a href='https://github.com/reactive-streams/reactive-streams-jvm#2.3'>2.3</a>
   * <p>
   * Notes:
   * <ul>
   * <li>The test checks for the presensce of method named "onError" in the current stacktrace when handling
   * the {@code request} or {@code cancel} calls in the test's own {@code Subscription}.
   * </ul>
   * <p>
   * If this test fails, the following could be checked within the {@code Subscriber} implementation:
   * <ul>
   * <li>no calls happen to {@code request} or {@code cancel} in response to an {@code onError}
   * directly or indirectly,</li>
   * <li>if the {@code Subscriber} throws an unchecked exception from its {@code onSubscribe} or
   * {@code onError} methods.
   * </ul>
   */
  void required_spec203_blackbox_mustNotCallMethodsOnSubscriptionOrPublisherInOnError() throws Throwable;
  /**
   * Currently, this test is skipped because there is no way to check what the {@code Subscriber} "considers"
   * since rule §2.3 forbids interaction from within the {@code onError} and {@code onComplete} methods.
   * <p>
   * <b>Verifies rule:</b> <a href='https://github.com/reactive-streams/reactive-streams-jvm#2.4'>2.4</a>
   * <p>
   * Notes:
   * <ul>
   * <li>It would be possible to check if there was an async interaction with the test's {@code Subscription}
   * within a grace period but such check is still not generally decisive.</li>
   * </ul>
   */
  void untested_spec204_blackbox_mustConsiderTheSubscriptionAsCancelledInAfterRecievingOnCompleteOrOnError() throws Exception;
  /**
   * Asks for a {@code Subscriber}, signals {@code onSubscribe} twice synchronously and expects the second {@code Subscription} gets
   * cancelled in a timely manner and without any calls to its {@code request} method.
   * <p>
   * <b>Verifies rule:</b> <a href='https://github.com/reactive-streams/reactive-streams-jvm#2.5'>2.5</a>
   * <p>
   * Notes:
   * <ul>
   * <li>The test doesn't signal any other events than {@code onSubscribe} and may cause resource leak in
   * {@code Subscriber}s that expect a finite {@code Publisher}.
   * </ul>
   * <p>
   * If this test fails, the following could be checked within the {@code Subscriber} implementation:
   * <ul>
   * <li>if the {@code Subscribe.onSubscribe} implementation actually tries to detect multiple calls to it,</li>
   * <li>if the second {@code Subscription} is cancelled asynchronously and that takes longer time than
   * the {@code TestEnvironment}'s timeout permits.</li>
   * </ul>
   */
  void required_spec205_blackbox_mustCallSubscriptionCancelIfItAlreadyHasAnSubscriptionAndReceivesAnotherOnSubscribeSignal() throws Exception;

  /**
   * Currently, this test is skipped because it requires more control over the {@code Subscriber} implementation
   * to make it cancel the {@code Subscription} for some external condition.
   * <p>
   * <b>Verifies rule:</b> <a href='https://github.com/reactive-streams/reactive-streams-jvm#2.6'>2.6</a>
   */
  void untested_spec206_blackbox_mustCallSubscriptionCancelIfItIsNoLongerValid() throws Exception;
  /**
   * Currently, this test is skipped because it requires more control over the {@code Subscriber} implementation
   * to issue requests based on external stimulus.
   * <p>
   * <b>Verifies rule:</b> <a href='https://github.com/reactive-streams/reactive-streams-jvm#2.7'>2.7</a>
   */
  void untested_spec207_blackbox_mustEnsureAllCallsOnItsSubscriptionTakePlaceFromTheSameThreadOrTakeCareOfSynchronization() throws Exception;
  /**
   * Currently, this test is skipped because there is no way to make the {@code Subscriber} implementation
   * cancel the test's {@code Subscription} and check the outcome of sending {@code onNext}s after such
   * cancel.
   * <p>
   * <b>Verifies rule:</b> <a href='https://github.com/reactive-streams/reactive-streams-jvm#2.8'>2.8</a>
   */
  void untested_spec208_blackbox_mustBePreparedToReceiveOnNextSignalsAfterHavingCalledSubscriptionCancel() throws Throwable;
  /**
   * Asks for a {@code Subscriber}, expects it to request some amount and in turn be able to receive an {@code onComplete}
   * synchronously from the {@code request} call without any {@code onNext} signals before that.
   * <p>
   * <b>Verifies rule:</b> <a href='https://github.com/reactive-streams/reactive-streams-jvm#2.9'>2.9</a>
   * <p>
   * Notes:
   * <ul>
   * <li>The test ignores cancellation from the {@code Subscriber}.</li>
   * <li>Invalid request amounts are ignored by this test.</li>
   * <li>Concurrent calls to the test's {@code Subscription.request()} must be externally synchronized, otherwise
   * such case results probabilistically in multiple {@code onComplete} calls by the test.</li>
   * </ul>
   * <p>
   * If this test fails, the following could be checked within the {@code Subscriber} implementation:
   * <ul>
   * <li>if the {@code Subscriber} throws an unchecked exception from its {@code onSubscribe} or
   * {@code onComplete} methods.
   * <li>if the {@code Subscriber} requires external stimulus to begin requesting; override the
   * {@link SubscriberBlackboxVerification#triggerRequest(org.reactivestreams.Subscriber)} method
   *  in this case,</li>
   * </ul>
   */
  void required_spec209_blackbox_mustBePreparedToReceiveAnOnCompleteSignalWithPrecedingRequestCall() throws Throwable;
  /**
   * Asks for a {@code Subscriber} and expects it to handle {@code onComplete} independent of whether the {@code Subscriber}
   * requests items or not.
   * <p>
   * <b>Verifies rule:</b> <a href='https://github.com/reactive-streams/reactive-streams-jvm#2.9'>2.9</a>
   * <p>
   * Notes:
   * <ul>
   * <li>Currently, the test doesn't call {@code onSubscribe} on the {@code Subscriber} which violates §1.9.
   * </ul>
   * <p>
   * If this test fails, the following could be checked within the {@code Subscriber} implementation:
   * <ul>
   * <li>if the {@code Subscriber} throws an unchecked exception from its {@code onSubscribe} or
   * {@code onComplete} methods.
   * </ul>
   */
  void required_spec209_blackbox_mustBePreparedToReceiveAnOnCompleteSignalWithoutPrecedingRequestCall() throws Throwable;
  /**
   * Asks for a {@code Subscriber}, signals {@code onSubscribe} followed by an {@code onError} synchronously.
   * <p>
   * <b>Verifies rule:</b> <a href='https://github.com/reactive-streams/reactive-streams-jvm#2.10'>2.10</a>
   * <p>
   * Notes:
   * <ul>
   * <li>Despite the method name, the test doesn't expect a request signal from {@code Subscriber} and emits the
   * {@code onError} signal anyway.
   * </ul>
   * <p>
   * If this test fails, the following could be checked within the {@code Subscriber} implementation:
   * <ul>
   * <li>if the {@code Subscriber} throws an unchecked exception from its {@code onSubscribe} or
   * {@code onError} methods.
   * </ul>
   */
  void required_spec210_blackbox_mustBePreparedToReceiveAnOnErrorSignalWithPrecedingRequestCall() throws Throwable;

  /**
   * Asks for a {@code Subscriber}, signals {@code onSubscribe} followed by an {@code onError} synchronously.
   * <p>
   * <b>Verifies rule:</b> <a href='https://github.com/reactive-streams/reactive-streams-jvm#2.10'>2.10</a>
   * <p>
   * If this test fails, the following could be checked within the {@code Subscriber} implementation:
   * <ul>
   * <li>if the {@code Subscriber} throws an unchecked exception from its {@code onSubscribe} or
   * {@code onError} methods.
   * </ul>
   */
  void required_spec210_blackbox_mustBePreparedToReceiveAnOnErrorSignalWithoutPrecedingRequestCall() throws Throwable;

  /**
   * Currently, this test is skipped because it would require analyzing what the {@code Subscriber} implementation
   * does.
   * <p>
   * <b>Verifies rule:</b> <a href='https://github.com/reactive-streams/reactive-streams-jvm#2.11'>2.11</a>
   */
  void untested_spec211_blackbox_mustMakeSureThatAllCallsOnItsMethodsHappenBeforeTheProcessingOfTheRespectiveEvents() throws Exception;
  /**
   * Currently, this test is skipped because the test for
   * {@link #required_spec205_blackbox_mustCallSubscriptionCancelIfItAlreadyHasAnSubscriptionAndReceivesAnotherOnSubscribeSignal §2.5}
   * is in a better position to test for handling the reuse of the same {@code Subscriber}.
   * <p>
   * <b>Verifies rule:</b> <a href='https://github.com/reactive-streams/reactive-streams-jvm#2.12'>2.12</a>
   * <p>
   * Notes:
   * <ul>
   * <li>In addition to §2.5, this rule could be better verified when testing a {@code Publisher}'s subscription behavior.
   * </ul>
   */
  void untested_spec212_blackbox_mustNotCallOnSubscribeMoreThanOnceBasedOnObjectEquality() throws Throwable;
  /**
   * Currently, this test is skipped because it would require more control over the {@code Subscriber} to
   * fail internally in response to a set of legal event emissions, not throw any exception from the {@code Subscriber}
   * methods and have it cancel the {@code Subscription}.
   * <p>
   * <b>Verifies rule:</b> <a href='https://github.com/reactive-streams/reactive-streams-jvm#2.13'>2.13</a>
   */
  void untested_spec213_blackbox_failingOnSignalInvocation() throws Exception;
  /**
   * Asks for a {@code Subscriber} and signals an {@code onSubscribe} event with {@code null} as a parameter and
   * expects an immediate {@code NullPointerException} to be thrown by the {@code Subscriber.onSubscribe} method.
   * <p>
   * <b>Verifies rule:</b> <a href='https://github.com/reactive-streams/reactive-streams-jvm#2.13'>2.13</a>
   * <p>
   * If this test fails, the following could be checked within the {@code Subscriber} implementation:
   * <ul>
   * <li>if the {@code Subscriber} throws a {@code NullPointerException} from its {@code onSubscribe} method
   * in response to a {@code null} parameter and not some other unchecked exception or no exception at all.
   * </ul>
   */
  void required_spec213_blackbox_onSubscribe_mustThrowNullPointerExceptionWhenParametersAreNull() throws Throwable;
  /**
   * Asks for a {@code Subscriber}, signals an {@code onSubscribe} event followed by a
   * {@code onNext} with {@code null} as a parameter and
   * expects an immediate {@code NullPointerException} to be thrown by the {@code Subscriber.onNext} method.
   * <p>
   * <b>Verifies rule:</b> <a href='https://github.com/reactive-streams/reactive-streams-jvm#2.13'>2.13</a>
   * <p>
   * Notes:
   * <ul>
   * <li>The test ignores cancellation and requests from the {@code Subscriber} and emits the {@code onNext}
   * signal with a {@code null} parameter anyway.</li>
   * </ul>
   * <p>
   * If this test fails, the following could be checked within the {@code Subscriber} implementation:
   * <ul>
   * <li>if the {@code Subscriber} throws a {@code NullPointerException} from its {@code onNext} method
   * in response to a {@code null} parameter and not some other unchecked exception or no exception at all.
   * </ul>
   */
  void required_spec213_blackbox_onNext_mustThrowNullPointerExceptionWhenParametersAreNull() throws Throwable;
  /**
   * Asks for a {@code Subscriber}, signals an {@code onSubscribe} event followed by a
   * {@code onError} with {@code null} as a parameter and
   * expects an immediate {@code NullPointerException} to be thrown by the {@code Subscriber.onError} method.
   * <p>
   * <b>Verifies rule:</b> <a href='https://github.com/reactive-streams/reactive-streams-jvm#2.13'>2.13</a>
   * <p>
   * Notes:
   * <ul>
   * <li>The test ignores cancellation from the {@code Subscriber} and emits the {@code onError}
   * signal with a {@code null} parameter anyway.</li>
   * </ul>
   * <p>
   * If this test fails, the following could be checked within the {@code Subscriber} implementation:
   * <ul>
   * <li>if the {@code Subscriber} throws a {@code NullPointerException} from its {@code onNext} method
   * in response to a {@code null} parameter and not some other unchecked exception or no exception at all.
   * </ul>
   */
  void required_spec213_blackbox_onError_mustThrowNullPointerExceptionWhenParametersAreNull() throws Throwable;
  /**
   * Currently, this test is skipped because there is no agreed upon way for specifying, enforcing and testing
   * a {@code Subscriber} with an arbitrary context.
   * <p>
   * <b>Verifies rule:</b> <a href='https://github.com/reactive-streams/reactive-streams-jvm#3.1'>3.1</a>
   */
  void untested_spec301_blackbox_mustNotBeCalledOutsideSubscriberContext() throws Exception;
  /**
   * Currently, this test is skipped because element production is the responsibility of the {@code Publisher} and
   * a {@code Subscription} is not expected to be the active element in an established subscription.
   * <p>
   * <b>Verifies rule:</b> <a href='https://github.com/reactive-streams/reactive-streams-jvm#3.8'>3.8</a>
   */
  void untested_spec308_blackbox_requestMustRegisterGivenNumberElementsToBeProduced() throws Throwable;
  /**
   * Currently, this test is skipped because element production is the responsibility of the {@code Publisher} and
   * a {@code Subscription} is not expected to be the active element in an established subscription.
   * <p>
   * <b>Verifies rule:</b> <a href='https://github.com/reactive-streams/reactive-streams-jvm#3.10'>3.10</a>
   * <p>
   * Notes:
   * <ul>
   * <li>This could be tested with a synchronous source currently not available within the TCK.</li>
   * </ul>
   */
  void untested_spec310_blackbox_requestMaySynchronouslyCallOnNextOnSubscriber() throws Exception;
  /**
   * Currently, this test is skipped because signal production is the responsibility of the {@code Publisher} and
   * a {@code Subscription} is not expected to be the active element in an established subscription.
   * <p>
   * <b>Verifies rule:</b> <a href='https://github.com/reactive-streams/reactive-streams-jvm#3.11'>3.11</a>
   * <p>
   * Notes:
   * <ul>
   * <li>Tests {@link #required_spec209_blackbox_mustBePreparedToReceiveAnOnCompleteSignalWithPrecedingRequestCall() §2.9}
   * and {@link #required_spec210_blackbox_mustBePreparedToReceiveAnOnErrorSignalWithPrecedingRequestCall() §2.10} are
   * supposed to cover this case from the {@code Subscriber's} perspective.</li>
   * </ul>
   */
  void untested_spec311_blackbox_requestMaySynchronouslyCallOnCompleteOrOnError() throws Exception;
  /**
   * Currently, this test is skipped because it is the responsibility of the {@code Publisher} deal with the case
   * that all subscribers have cancelled their subscription.
   * <p>
   * <b>Verifies rule:</b> <a href='https://github.com/reactive-streams/reactive-streams-jvm#3.14'>3.14</a>
   * <p>
   * Notes:
   * <ul>
   * <li>The specification lists this as an optional behavior because only some {@code Publisher} implementations
   * (most likely {@code Processor}s) would coordinate with multiple {@code Subscriber}s.</li>
   * </ul>
   */
  void untested_spec314_blackbox_cancelMayCauseThePublisherToShutdownIfNoOtherSubscriptionExists() throws Exception;
  /**
   * Currently, this test is skipped because it requires more control over the {@code Subscriber} implementation
   * thus there is no way to detect that the {@code Subscriber} called its own {@code onError} method in response
   * to an exception thrown from {@code Subscription.cancel}.
   * <p>
   * <b>Verifies rule:</b> <a href='https://github.com/reactive-streams/reactive-streams-jvm#3.15'>3.15</a>
   */
  void untested_spec315_blackbox_cancelMustNotThrowExceptionAndMustSignalOnError() throws Exception;
  /**
   * Currently, this test is skipped because it requires more control over the {@code Subscriber} implementation
   * thus there is no way to detect that the {@code Subscriber} called its own {@code onError} method in response
   * to an exception thrown from {@code Subscription.request}.
   * <p>
   * <b>Verifies rule:</b> <a href='https://github.com/reactive-streams/reactive-streams-jvm#3.16'>3.16</a>
   */
  void untested_spec316_blackbox_requestMustNotThrowExceptionAndMustOnErrorTheSubscriber() throws Exception;
}
