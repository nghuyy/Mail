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

import java.io.IOException;

public class AudioContent extends MimeMessageContent {
	private byte[] rawData;
	
	public AudioContent(AudioPart audioPart, String encoding, byte[] data) throws UnsupportedContentException {
		super(audioPart);
        // Decode the binary data
        if (encoding.equalsIgnoreCase(ENCODING_BASE64)) {
        	try {
		        this.rawData = decodeBase64(data);
        	} catch (IOException e) {
        		throw new UnsupportedContentException("Unable to decode");
        	}
        } else {
			throw new UnsupportedContentException("Unsupported encoding");
        }
	}

	public AudioContent(AudioPart audioPart, byte[] rawData) {
	    super(audioPart);
	    audioPart.setEncoding(ENCODING_BASE64);
	    this.rawData = rawData;
	}
	
	/**
	 * Instantiates a new audio content object for deserialization.
	 */
	public AudioContent() {
		super(null);
	}
	
	public void accept(MimeMessageContentVisitor visitor) {
		visitor.visit(this);
	}
	
	/**
	 * Find out if a content object can be created for the provided
	 * MIME structure part.
	 * @param audioPart MIME part to check.
	 * @return True if the content type is supported, false otherwise.
	 */
	public static boolean isPartSupported(AudioPart audioPart) {
		return true;
	}

	/* (non-Javadoc)
	 * @see org.logicprobe.LogicMail.message.MessageContent#getRawData()
	 */
	public byte[] getRawData() {
		return rawData;
	}

	/* (non-Javadoc)
	 * @see org.logicprobe.LogicMail.message.MimeMessageContent#putRawData(byte[])
	 */
	protected void putRawData(byte[] rawData) {
		this.rawData = rawData;
	}
}
