/*-
 * Copyright (c) 2007, Derek Konigsberg
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

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Vector;

/**
 * Provides an implementation of java.util.Vector that implements
 * the LogicMail.util.Serializable interface.
 * The maximum number of items that can be stored is 1000.
 * This limit exists so that the deserialization code can quickly
 * handle data corruption that could result in a bad size value.
 */
public class SerializableVector extends Vector implements Serializable {
    private int hashcode = -1;

    final private static int TYPE_NULL    = 0;
    final private static int TYPE_BOOLEAN = 1;
    final private static int TYPE_BYTE    = 2;
    final private static int TYPE_CHAR    = 3;
    final private static int TYPE_STRING  = 4;
    final private static int TYPE_DOUBLE  = 5;
    final private static int TYPE_FLOAT   = 6;
    final private static int TYPE_INT     = 7;
    final private static int TYPE_LONG    = 8;
    final private static int TYPE_SHORT   = 9;

    final private static int MAX_ITEMS = 1000;

    private long uniqueId;

    /**
     * Creates a new instance of SerializableVector.
     * This class only supports vectors containing objects which
     * wrap the various primitive types supported by {@link DataOutput}
     * and {@link DataInput}.
     */
    public SerializableVector() {
        super();
        uniqueId = UniqueIdGenerator.getInstance().getUniqueId();
    }

    /**
     * Creates a new instance of SerializableVector.
     * This class only supports vectors containing objects which
     * wrap the various primitive types supported by {@link DataOutput}
     * and {@link DataInput}.
     *
     * @param initialCapacity Initial capacity of the vector.
     */
    public SerializableVector(int initialCapacity) {
        super(initialCapacity);
        uniqueId = UniqueIdGenerator.getInstance().getUniqueId();
    }

    private static void writeObject(DataOutput output, Object item) throws IOException {
        if(item instanceof Boolean) {
            output.writeInt(TYPE_BOOLEAN);
            output.writeBoolean(((Boolean)item).booleanValue());
        }
        else if(item instanceof Byte) {
            output.writeInt(TYPE_BYTE);
            output.writeByte(((Byte)item).byteValue());
        }
        else if(item instanceof Character) {
            output.writeInt(TYPE_CHAR);
            output.writeChar(((Character)item).charValue());
        }
        else if(item instanceof String) {
            output.writeInt(TYPE_STRING);
            output.writeUTF(((String)item));
        }
        else if(item instanceof Double) {
            output.writeInt(TYPE_DOUBLE);
            output.writeDouble(((Double)item).doubleValue());
        }
        else if(item instanceof Float) {
            output.writeInt(TYPE_FLOAT);
            output.writeFloat(((Float)item).floatValue());
        }
        else if(item instanceof Integer) {
            output.writeInt(TYPE_INT);
            output.writeInt(((Integer)item).intValue());
        }
        else if(item instanceof Long) {
            output.writeInt(TYPE_LONG);
            output.writeLong(((Long)item).longValue());
        }
        else if(item instanceof Short) {
            output.writeInt(TYPE_SHORT);
            output.writeShort(((Short)item).shortValue());
        }
        else {
            output.writeInt(TYPE_NULL);
            output.write(0);
        }
    }

    private static Object readObject(DataInput input) throws IOException {
        int type = input.readInt();
        switch(type) {
        case TYPE_BOOLEAN:
            return new Boolean(input.readBoolean());
        case TYPE_BYTE:
            return new Byte(input.readByte());
        case TYPE_CHAR:
            return new Character(input.readChar());
        case TYPE_STRING:
            return input.readUTF();
        case TYPE_DOUBLE:
            return new Double(input.readDouble());
        case TYPE_FLOAT:
            return new Float(input.readFloat());
        case TYPE_INT:
            return new Integer(input.readInt());
        case TYPE_LONG:
            return new Long(input.readLong());
        case TYPE_SHORT:
            return new Short(input.readShort());
        case TYPE_NULL:
            return null;
        default:
            return null;
        }
    }

    public void serialize(DataOutput output) throws IOException {
        output.writeLong(uniqueId);
        int size = this.size();
        output.writeInt(size);
        for(int i=0; i<size; ++i) {
            writeObject(output, this.elementAt(i));
        }
    }

    public void deserialize(DataInput input) throws IOException {
        this.removeAllElements();
        uniqueId = input.readLong();
        int size = input.readInt();
        if(size > MAX_ITEMS) {
            throw new IOException();
        }

        for(int i=0; i<size; i++) {
            this.addElement(readObject(input));
        }
    }

    public long getUniqueId() {
        return uniqueId;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
        if(hashcode == -1) {
            hashcode = 31 * 1 + (int) (uniqueId ^ (uniqueId >>> 32));
        }
        return hashcode;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        SerializableVector other = (SerializableVector) obj;
        if (uniqueId != other.uniqueId) {
            return false;
        }
        return true;
    }
}
