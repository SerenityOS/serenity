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

/**
 * Internal TCK use only.
 * Add / Remove tests for PublisherVerificaSubscriberWhiteboxVerification here to make sure that they arre added/removed in the other places.
 */
public interface SubscriberWhiteboxVerificationRules {
  void required_exerciseWhiteboxHappyPath() throws Throwable;
  void required_spec201_mustSignalDemandViaSubscriptionRequest() throws Throwable;
  void untested_spec202_shouldAsynchronouslyDispatch() throws Exception;
  void required_spec203_mustNotCallMethodsOnSubscriptionOrPublisherInOnComplete() throws Throwable;
  void required_spec203_mustNotCallMethodsOnSubscriptionOrPublisherInOnError() throws Throwable;
  void untested_spec204_mustConsiderTheSubscriptionAsCancelledInAfterRecievingOnCompleteOrOnError() throws Exception;
  void required_spec205_mustCallSubscriptionCancelIfItAlreadyHasAnSubscriptionAndReceivesAnotherOnSubscribeSignal() throws Throwable;
  void untested_spec206_mustCallSubscriptionCancelIfItIsNoLongerValid() throws Exception;
  void untested_spec207_mustEnsureAllCallsOnItsSubscriptionTakePlaceFromTheSameThreadOrTakeCareOfSynchronization() throws Exception;
  void required_spec208_mustBePreparedToReceiveOnNextSignalsAfterHavingCalledSubscriptionCancel() throws Throwable;
  void required_spec209_mustBePreparedToReceiveAnOnCompleteSignalWithPrecedingRequestCall() throws Throwable;
  void required_spec209_mustBePreparedToReceiveAnOnCompleteSignalWithoutPrecedingRequestCall() throws Throwable;
  void required_spec210_mustBePreparedToReceiveAnOnErrorSignalWithPrecedingRequestCall() throws Throwable;
  void required_spec210_mustBePreparedToReceiveAnOnErrorSignalWithoutPrecedingRequestCall() throws Throwable;
  void untested_spec211_mustMakeSureThatAllCallsOnItsMethodsHappenBeforeTheProcessingOfTheRespectiveEvents() throws Exception;
  void untested_spec212_mustNotCallOnSubscribeMoreThanOnceBasedOnObjectEquality_specViolation() throws Throwable;
  void untested_spec213_failingOnSignalInvocation() throws Exception;
  void required_spec213_onSubscribe_mustThrowNullPointerExceptionWhenParametersAreNull() throws Throwable;
  void required_spec213_onNext_mustThrowNullPointerExceptionWhenParametersAreNull() throws Throwable;
  void required_spec213_onError_mustThrowNullPointerExceptionWhenParametersAreNull() throws Throwable;
  void untested_spec301_mustNotBeCalledOutsideSubscriberContext() throws Exception;
  void required_spec308_requestMustRegisterGivenNumberElementsToBeProduced() throws Throwable;
  void untested_spec310_requestMaySynchronouslyCallOnNextOnSubscriber() throws Exception;
  void untested_spec311_requestMaySynchronouslyCallOnCompleteOrOnError() throws Exception;
  void untested_spec314_cancelMayCauseThePublisherToShutdownIfNoOtherSubscriptionExists() throws Exception;
  void untested_spec315_cancelMustNotThrowExceptionAndMustSignalOnError() throws Exception;
  void untested_spec316_requestMustNotThrowExceptionAndMustOnErrorTheSubscriber() throws Exception;
}
