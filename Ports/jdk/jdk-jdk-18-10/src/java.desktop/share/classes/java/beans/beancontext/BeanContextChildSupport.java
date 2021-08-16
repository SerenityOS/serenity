/*
 * Copyright (c) 1998, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

package java.beans.beancontext;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.beans.PropertyVetoException;
import java.beans.VetoableChangeListener;
import java.beans.VetoableChangeSupport;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.io.Serializable;

/**
 * <p>
 * This is a general support class to provide support for implementing the
 * BeanContextChild protocol.
 *
 * This class may either be directly subclassed, or encapsulated and delegated
 * to in order to implement this interface for a given component.
 * </p>
 *
 * @author      Laurence P. G. Cable
 * @since       1.2
 *
 * @see java.beans.beancontext.BeanContext
 * @see java.beans.beancontext.BeanContextServices
 * @see java.beans.beancontext.BeanContextChild
 */

public class BeanContextChildSupport implements BeanContextChild, BeanContextServicesListener, Serializable {

    /**
     * Use serialVersionUID from JDK 1.2 for interoperability.
     */
    @Serial
    private static final long serialVersionUID = 6328947014421475877L;

    /**
     * construct a BeanContextChildSupport where this class has been
     * subclassed in order to implement the JavaBean component itself.
     */

    public BeanContextChildSupport() {
        super();

        beanContextChildPeer = this;

        pcSupport = new PropertyChangeSupport(beanContextChildPeer);
        vcSupport = new VetoableChangeSupport(beanContextChildPeer);
    }

    /**
     * construct a BeanContextChildSupport where the JavaBean component
     * itself implements BeanContextChild, and encapsulates this, delegating
     * that interface to this implementation
     * @param bcc the underlying bean context child
     */

    public BeanContextChildSupport(BeanContextChild bcc) {
        super();

        beanContextChildPeer = (bcc != null) ? bcc : this;

        pcSupport = new PropertyChangeSupport(beanContextChildPeer);
        vcSupport = new VetoableChangeSupport(beanContextChildPeer);
    }

    /**
     * Sets the {@code BeanContext} for
     * this {@code BeanContextChildSupport}.
     * @param bc the new value to be assigned to the {@code BeanContext}
     * property
     * @throws PropertyVetoException if the change is rejected
     */
    public synchronized void setBeanContext(BeanContext bc) throws PropertyVetoException {
        if (bc == beanContext) return;

        BeanContext oldValue = beanContext;
        BeanContext newValue = bc;

        if (!rejectedSetBCOnce) {
            if (rejectedSetBCOnce = !validatePendingSetBeanContext(bc)) {
                throw new PropertyVetoException(
                    "setBeanContext() change rejected:",
                    new PropertyChangeEvent(beanContextChildPeer, "beanContext", oldValue, newValue)
                );
            }

            try {
                fireVetoableChange("beanContext",
                                   oldValue,
                                   newValue
                );
            } catch (PropertyVetoException pve) {
                rejectedSetBCOnce = true;

                throw pve; // re-throw
            }
        }

        if (beanContext != null) releaseBeanContextResources();

        beanContext       = newValue;
        rejectedSetBCOnce = false;

        firePropertyChange("beanContext",
                           oldValue,
                           newValue
        );

        if (beanContext != null) initializeBeanContextResources();
    }

    /**
     * Gets the nesting {@code BeanContext}
     * for this {@code BeanContextChildSupport}.
     * @return the nesting {@code BeanContext} for
     * this {@code BeanContextChildSupport}.
     */
    public synchronized BeanContext getBeanContext() { return beanContext; }

    /**
     * Add a PropertyChangeListener for a specific property.
     * The same listener object may be added more than once.  For each
     * property,  the listener will be invoked the number of times it was added
     * for that property.
     * If {@code name} or {@code pcl} is null, no exception is thrown
     * and no action is taken.
     *
     * @param name The name of the property to listen on
     * @param pcl The {@code PropertyChangeListener} to be added
     */
    public void addPropertyChangeListener(String name, PropertyChangeListener pcl) {
        pcSupport.addPropertyChangeListener(name, pcl);
    }

    /**
     * Remove a PropertyChangeListener for a specific property.
     * If {@code pcl} was added more than once to the same event
     * source for the specified property, it will be notified one less time
     * after being removed.
     * If {@code name} is null, no exception is thrown
     * and no action is taken.
     * If {@code pcl} is null, or was never added for the specified
     * property, no exception is thrown and no action is taken.
     *
     * @param name The name of the property that was listened on
     * @param pcl The PropertyChangeListener to be removed
     */
    public void removePropertyChangeListener(String name, PropertyChangeListener pcl) {
        pcSupport.removePropertyChangeListener(name, pcl);
    }

    /**
     * Add a VetoableChangeListener for a specific property.
     * The same listener object may be added more than once.  For each
     * property,  the listener will be invoked the number of times it was added
     * for that property.
     * If {@code name} or {@code vcl} is null, no exception is thrown
     * and no action is taken.
     *
     * @param name The name of the property to listen on
     * @param vcl The {@code VetoableChangeListener} to be added
     */
    public void addVetoableChangeListener(String name, VetoableChangeListener vcl) {
        vcSupport.addVetoableChangeListener(name, vcl);
    }

    /**
     * Removes a {@code VetoableChangeListener}.
     * If {@code pcl} was added more than once to the same event
     * source for the specified property, it will be notified one less time
     * after being removed.
     * If {@code name} is null, no exception is thrown
     * and no action is taken.
     * If {@code vcl} is null, or was never added for the specified
     * property, no exception is thrown and no action is taken.
     *
     * @param name The name of the property that was listened on
     * @param vcl The {@code VetoableChangeListener} to be removed
     */
    public void removeVetoableChangeListener(String name, VetoableChangeListener vcl) {
        vcSupport.removeVetoableChangeListener(name, vcl);
    }

    /**
     * A service provided by the nesting BeanContext has been revoked.
     *
     * Subclasses may override this method in order to implement their own
     * behaviors.
     * @param bcsre The {@code BeanContextServiceRevokedEvent} fired as a
     * result of a service being revoked
     */
    public void serviceRevoked(BeanContextServiceRevokedEvent bcsre) { }

    /**
     * A new service is available from the nesting BeanContext.
     *
     * Subclasses may override this method in order to implement their own
     * behaviors
     * @param bcsae The BeanContextServiceAvailableEvent fired as a
     * result of a service becoming available
     *
     */
    public void serviceAvailable(BeanContextServiceAvailableEvent bcsae) { }

    /**
     * Gets the {@code BeanContextChild} associated with this
     * {@code BeanContextChildSupport}.
     *
     * @return the {@code BeanContextChild} peer of this class
     */
    public BeanContextChild getBeanContextChildPeer() { return beanContextChildPeer; }

    /**
     * Reports whether or not this class is a delegate of another.
     *
     * @return true if this class is a delegate of another
     */
    public boolean isDelegated() { return !this.equals(beanContextChildPeer); }

    /**
     * Report a bound property update to any registered listeners. No event is
     * fired if old and new are equal and non-null.
     * @param name The programmatic name of the property that was changed
     * @param oldValue  The old value of the property
     * @param newValue  The new value of the property
     */
    public void firePropertyChange(String name, Object oldValue, Object newValue) {
        pcSupport.firePropertyChange(name, oldValue, newValue);
    }

    /**
     * Report a vetoable property update to any registered listeners.
     * If anyone vetos the change, then fire a new event
     * reverting everyone to the old value and then rethrow
     * the PropertyVetoException. <P>
     *
     * No event is fired if old and new are equal and non-null.
     *
     * @param name The programmatic name of the property that is about to
     * change
     *
     * @param oldValue The old value of the property
     * @param newValue - The new value of the property
     *
     * @throws PropertyVetoException if the recipient wishes the property
     * change to be rolled back.
     */
    public void fireVetoableChange(String name, Object oldValue, Object newValue) throws PropertyVetoException {
        vcSupport.fireVetoableChange(name, oldValue, newValue);
    }

    /**
     * Called from setBeanContext to validate (or otherwise) the
     * pending change in the nesting BeanContext property value.
     * Returning false will cause setBeanContext to throw
     * PropertyVetoException.
     * @param newValue the new value that has been requested for
     *  the BeanContext property
     * @return {@code true} if the change operation is to be vetoed
     */
    public boolean validatePendingSetBeanContext(BeanContext newValue) {
        return true;
    }

    /**
     * This method may be overridden by subclasses to provide their own
     * release behaviors. When invoked any resources held by this instance
     * obtained from its current BeanContext property should be released
     * since the object is no longer nested within that BeanContext.
     */

    protected  void releaseBeanContextResources() {
        // do nothing
    }

    /**
     * This method may be overridden by subclasses to provide their own
     * initialization behaviors. When invoked any resources required by the
     * BeanContextChild should be obtained from the current BeanContext.
     */

    protected void initializeBeanContextResources() {
        // do nothing
    }

    /**
     * Write the persistence state of the object.
     *
     * @param  oos the {@code ObjectOutputStream} to write
     * @throws IOException if an I/O error occurs
     */
    @Serial
    private void writeObject(ObjectOutputStream oos) throws IOException {

        /*
         * don't serialize if we are delegated and the delegator is not also
         * serializable.
         */

        if (!equals(beanContextChildPeer) && !(beanContextChildPeer instanceof Serializable))
            throw new IOException("BeanContextChildSupport beanContextChildPeer not Serializable");

        else
            oos.defaultWriteObject();

    }


    /**
     * Restore a persistent object, must wait for subsequent setBeanContext()
     * to fully restore any resources obtained from the new nesting BeanContext.
     *
     * @param  ois the {@code ObjectInputStream} to read
     * @throws ClassNotFoundException if the class of a serialized object could
     *         not be found
     * @throws IOException if an I/O error occurs
     */
    @Serial
    private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
        ois.defaultReadObject();
    }

    /*
     * fields
     */

    /**
     * The {@code BeanContext} in which
     * this {@code BeanContextChild} is nested.
     */
    @SuppressWarnings("serial") // Not statically typed as Serializable
    public    BeanContextChild      beanContextChildPeer;

   /**
    * The {@code PropertyChangeSupport} associated with this
    * {@code BeanContextChildSupport}.
    */
    protected PropertyChangeSupport pcSupport;

   /**
    * The {@code VetoableChangeSupport} associated with this
    * {@code BeanContextChildSupport}.
    */
    protected VetoableChangeSupport vcSupport;

    /**
     * The bean context.
     */
    protected transient BeanContext           beanContext;

   /**
    * A flag indicating that there has been
    * at least one {@code PropertyChangeVetoException}
    * thrown for the attempted setBeanContext operation.
    */
    protected transient boolean               rejectedSetBCOnce;

}
