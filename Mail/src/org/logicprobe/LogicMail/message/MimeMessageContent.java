/*-
 * Copyright (c) 2009, Derek Konigsberg
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
package org.logicprobe.LogicMail.message;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import net.rim.device.api.io.Base64InputStream;

import org.logicprobe.LogicMail.util.Serializable;
import org.logicprobe.LogicMail.util.SerializationUtils;
import org.logicprobe.LogicMail.util.UniqueIdGenerator;

/**
 * Represents content for a message, maintained separately
 * from its structure.  There should be a subclass for every
 * major MIME type that is supported.
 */
public abstract class MimeMessageContent implements Serializable {
    private long uniqueId;
    private ContentPart messagePart;
    private int partComplete;
    protected static String ENCODING_BASE64 = "base64";
    protected static String ENCODING_QUOTED_PRINTABLE = "quoted-printable";

    public static final int PART_COMPLETE = 1;
    public static final int PART_INCOMPLETE = 0;
    public static final int PART_UNKNOWN = -1;

    /**
     * Instantiates a new message content object
     * 
     * @param messagePart the message part
     */
    protected MimeMessageContent(ContentPart messagePart) {
        this.uniqueId = UniqueIdGenerator.getInstance().getUniqueId();
        this.messagePart = messagePart;
        this.partComplete = PART_COMPLETE;
    }

    /**
     * Sets whether the part is complete, or has been truncated.
     *
     * @param partComplete the completeness of the message content
     */
    public void setPartComplete(int partComplete) {
        this.partComplete = partComplete;
    }
    
    /**
     * Gets whether the part is complete, or has been truncated.
     *
     * @return the completeness of the message content
     */
    public int isPartComplete() {
        return partComplete;
    }
    
    /**
     * Gets the message part representing the placement of
     * this content within the message structure.
     * 
     * @return the message part
     */
    public ContentPart getMessagePart() {
        return this.messagePart;
    }

    /**
     * Accept a visitor on this message content.
     * @param visitor The visitor instance
     */
    public abstract void accept(MimeMessageContentVisitor visitor);

    /**
     * Gets the raw data representing this message content.
     * Necessary for saving the content to a file, or
     * transmitting it to external systems.
     * 
     * @return Raw data if available, or null otherwise
     */
    public abstract byte[] getRawData();

    /**
     * Populates the message content object from raw data.
     * Necessary for recreating the object from a serialized
     * form.
     * 
     * @param rawData Raw data for the message content
     */
    protected abstract void putRawData(byte[] rawData);

    /**
     * Decode the provided data as Base64.
     * 
     * @param data byte array representing a Base64-encoded string
     * @return decoded form of the input data
     * @throws IOException thrown if a decoding error occurred.
     */
    protected static byte[] decodeBase64(byte[] data) throws IOException {
        if(data == null || data.length == 0) {
            return new byte[0];
        }
        
        byte[] decodedData;
        if(data.length < 32768) {
            decodedData = Base64InputStream.decode(data, 0, data.length);
        }
        else {
            Base64InputStream inputStream = new Base64InputStream(new ByteArrayInputStream(data, 0, data.length));
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream(65536);
            
            byte[] buf = new byte[1024];
            int count;
            while ((count = inputStream.read(buf)) >= 0)
            {
                outputStream.write(buf, 0, count);
            }
            inputStream.close();
            
            decodedData = outputStream.toByteArray();
        }
        return decodedData;
    }
    
    /* (non-Javadoc)
     * @see org.logicprobe.LogicMail.util.Serializable#getUniqueId()
     */
    public long getUniqueId() {
        return this.uniqueId;
    }

    /* (non-Javadoc)
     * @see org.logicprobe.LogicMail.util.Serializable#serialize(java.io.DataOutput)
     */
    public void serialize(DataOutput output) throws IOException {
        output.writeLong(uniqueId);
        SerializationUtils.serializeClass(messagePart, output);
        byte[] data = getRawData();
        output.writeInt(data.length);
        output.write(data);
    }

    /* (non-Javadoc)
     * @see org.logicprobe.LogicMail.util.Serializable#deserialize(java.io.DataInput)
     */
    public void deserialize(DataInput input) throws IOException {
        uniqueId = input.readLong();
        messagePart = (ContentPart)SerializationUtils.deserializeClass(input);
        int len = input.readInt();
        byte[] data = new byte[len];
        input.readFully(data, 0, len);
        putRawData(data);
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
        int prime = 31;
        int result = 1;
        result = prime * result + (int) (uniqueId ^ (uniqueId >>> 32));
        return result;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        MimeMessageContent other = (MimeMessageContent) obj;
        if (uniqueId != other.uniqueId)
            return false;
        return true;
    }
}
