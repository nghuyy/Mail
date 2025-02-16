/*-
 * Copyright (c) 2010, Derek Konigsberg
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer. 
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution. 
 * 3. Neither the name of the project nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.logicprobe.LogicMail.util;

import net.rim.device.api.util.IntHashtable;
import net.rim.device.api.util.Persistable;

/**
 * Base class for persistable containers.
 * Subclasses should only add integer constant keys for their specific
 * contents.  The purpose of this class is to contain the underlying storage
 * container and access methods.
 */
public abstract class PersistableContainer implements Persistable {
    private IntHashtable contents;
    
    /**
     * Instantiates a new persistable container.
     *
     * @param initialCapacity the initial capacity for the storage container
     */
    protected PersistableContainer(int initialCapacity) {
        contents = new IntHashtable(initialCapacity);
    }
    
    /**
     * Sets the specified element within the container.
     *
     * @param id the ID for the element, represented by a class constant
     * @param value the value to set for the element
     */
    public void setElement(int id, Object value) {
        contents.put(id, value);
    }
    
    /**
     * Gets the specified element from the container.
     *
     * @param id the ID for the element, represented by a class constant
     * @return the element, if it exists
     */
    public Object getElement(int id) {
        return contents.get(id);
    }
}
