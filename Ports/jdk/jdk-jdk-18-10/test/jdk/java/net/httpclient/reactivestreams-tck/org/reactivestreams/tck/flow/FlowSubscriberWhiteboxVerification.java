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
import org.reactivestreams.tck.SubscriberWhiteboxVerification;
import org.reactivestreams.tck.TestEnvironment;
import org.reactivestreams.tck.flow.support.SubscriberWhiteboxVerificationRules;

import java.util.concurrent.Flow;

/**
 * Provides whitebox style tests for verifying {@link java.util.concurrent.Flow.Subscriber}
 * and {@link java.util.concurrent.Flow.Subscription} specification rules.
 *
 * @see java.util.concurrent.Flow.Subscriber
 * @see java.util.concurrent.Flow.Subscription
 */
public abstract class FlowSubscriberWhiteboxVerification<T> extends SubscriberWhiteboxVerification<T>
  implements SubscriberWhiteboxVerificationRules {

  protected FlowSubscriberWhiteboxVerification(TestEnvironment env) {
    super(env);
  }

  @Override
  final public Subscriber<T> createSubscriber(WhiteboxSubscriberProbe<T> probe) {
    return FlowAdapters.toSubscriber(createFlowSubscriber(probe));
  }
  /**
   * This is the main method you must implement in your test incarnation.
   * It must create a new {@link org.reactivestreams.Subscriber} instance to be subjected to the testing logic.
   *
   * In order to be meaningfully testable your Subscriber must inform the given
   * `WhiteboxSubscriberProbe` of the respective events having been received.
   */
  protected abstract Flow.Subscriber<T> createFlowSubscriber(WhiteboxSubscriberProbe<T> probe);
}
