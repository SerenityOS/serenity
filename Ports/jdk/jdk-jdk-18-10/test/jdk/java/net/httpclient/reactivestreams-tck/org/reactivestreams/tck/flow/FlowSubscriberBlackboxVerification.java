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

package org.reactivestreams.tck.flow;

import org.reactivestreams.FlowAdapters;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.reactivestreams.tck.SubscriberBlackboxVerification;
import org.reactivestreams.tck.TestEnvironment;
import org.reactivestreams.tck.flow.support.SubscriberBlackboxVerificationRules;

import java.util.concurrent.Flow;

/**
 * Provides tests for verifying {@link java.util.concurrent.Flow.Subscriber} and {@link java.util.concurrent.Flow.Subscription}
 * specification rules, without any modifications to the tested implementation (also known as "Black Box" testing).
 *
 * This verification is NOT able to check many of the rules of the spec, and if you want more
 * verification of your implementation you'll have to implement {@code org.reactivestreams.tck.SubscriberWhiteboxVerification}
 * instead.
 *
 * @see java.util.concurrent.Flow.Subscriber
 * @see java.util.concurrent.Flow.Subscription
 */
public abstract class FlowSubscriberBlackboxVerification<T> extends SubscriberBlackboxVerification<T>
  implements SubscriberBlackboxVerificationRules {

  protected FlowSubscriberBlackboxVerification(TestEnvironment env) {
    super(env);
  }

  @Override
  public final void triggerRequest(Subscriber<? super T> subscriber) {
    triggerFlowRequest(FlowAdapters.toFlowSubscriber(subscriber));
  }
  /**
   * Override this method if the {@link java.util.concurrent.Flow.Subscriber} implementation you are verifying
   * needs an external signal before it signals demand to its Publisher.
   *
   * By default this method does nothing.
   */
  public void triggerFlowRequest(Flow.Subscriber<? super T> subscriber) {
    // this method is intentionally left blank
  }

  @Override
  public final Subscriber<T> createSubscriber() {
    return FlowAdapters.<T>toSubscriber(createFlowSubscriber());
  }
  /**
   * This is the main method you must implement in your test incarnation.
   * It must create a new {@link Flow.Subscriber} instance to be subjected to the testing logic.
   */
  abstract public Flow.Subscriber<T> createFlowSubscriber();

}
