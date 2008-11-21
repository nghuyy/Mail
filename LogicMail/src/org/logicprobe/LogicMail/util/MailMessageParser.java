/*-
 * Copyright (c) 2008, Derek Konigsberg
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

import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Hashtable;

import net.rim.device.api.io.SharedInputStream;
import net.rim.device.api.mime.MIMEInputStream;
import net.rim.device.api.mime.MIMEParsingException;

import org.logicprobe.LogicMail.message.MessageEnvelope;
import org.logicprobe.LogicMail.message.MessagePart;
import org.logicprobe.LogicMail.message.MessagePartFactory;
import org.logicprobe.LogicMail.message.MultiPart;

/**
 * This class contains static parser functions used for
 * parsing raw message source text.
 */
public class MailMessageParser {
    private MailMessageParser() { }

    /**
     * Parses the message envelope from the message headers.
     * 
     * @param rawHeaders The raw header text, separated into lines.
     * @return The message envelope.
     */
    public static MessageEnvelope parseMessageEnvelope(String[] rawHeaders) {
        Hashtable headers = StringParser.parseMailHeaders(rawHeaders);
        MessageEnvelope env = new MessageEnvelope();
        
        // Populate the common header field bits of the envelope
        env.subject = StringParser.parseEncodedHeader((String)headers.get("subject"));
        if(env.subject == null) {
            env.subject = "<subject>";
        }
        env.from = parseAddressList((String)headers.get("from"));
        env.sender = parseAddressList((String)headers.get("sender"));
        env.to = parseAddressList((String)headers.get("to"));
        env.cc = parseAddressList((String)headers.get("cc"));
        env.bcc = parseAddressList((String)headers.get("bcc"));
        try {
            env.date = StringParser.parseDateString((String)headers.get("date"));
        } catch (Exception e) {
            env.date = Calendar.getInstance().getTime();
        }
        env.replyTo = parseAddressList((String)headers.get("reply-to"));
        env.messageId = (String)headers.get("message-id");
        env.inReplyTo = (String)headers.get("in-reply-to");
        return env;
    }
    
    /**
     * Separates a list of addresses contained within a message header.
     * This is slightly more complicated than a string tokenizer, as it
     * has to deal with quoting and escaping.
     * 
     * @param text The header line containing the addresses.
     * @return The separated addresses.
     */
    private static String[] parseAddressList(String text) {
        String[] addresses = StringParser.parseCsvString(text);
        for(int i=0; i<addresses.length; i++) {
            addresses[i] = StringParser.parseEncodedHeader(addresses[i]);
            if(addresses[i].length() > 0 && addresses[i].charAt(0) == '"') {
                int p = addresses[i].indexOf('<');
                while(p > 0 && addresses[i].charAt(p) != '"') p--;
                if(p > 0 && p+1 < addresses[i].length()) {
                    addresses[i] = addresses[i].substring(1, p) + addresses[i].substring(p+1);
                }
            }
        }
        return addresses;
    }
    
    /**
     * Parses the raw message body.
     * 
     * @param inputStream The stream to read the raw message from
     * @return The root message part.
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static MessagePart parseRawMessage(InputStream inputStream) throws IOException {
        MIMEInputStream mimeInputStream = null;
        try {
            mimeInputStream = new MIMEInputStream(inputStream);
        } catch (MIMEParsingException e) {
            return null;
        }
        MessagePart rootPart = getMessagePart(mimeInputStream);
        return rootPart;
    }
    
    /**
     * Recursively walk the provided MIMEInputStream, building a message
     * tree in the process.
     *
     * @param mimeInputStream MIMEInputStream of the downloaded message data
     * @return Root MessagePart element for this portion of the message tree
     */
    private static MessagePart getMessagePart(MIMEInputStream mimeInputStream) throws IOException {
    	//TODO: Refactor so MaildirFolder and PopClient use a common implementation
    	
        // Parse out the MIME type and relevant header fields
        String mimeType = mimeInputStream.getContentType();
        String type = mimeType.substring(0, mimeType.indexOf('/'));
        String subtype = mimeType.substring(mimeType.indexOf('/') + 1);
        String encoding = mimeInputStream.getHeader("Content-Transfer-Encoding");
        String charset = mimeInputStream.getContentTypeParameter("charset");
        
        // Default parameters used when headers are missing
        if(encoding == null) {
            encoding = "7bit";
        }
        
        // Handle the multi-part case
        if(mimeInputStream.isMultiPart() && type.equalsIgnoreCase("multipart")) {
            MessagePart part = MessagePartFactory.createMessagePart(type, subtype, null, null, null);
            MIMEInputStream[] mimeSubparts = mimeInputStream.getParts();
            for(int i=0;i<mimeSubparts.length;i++) {
                MessagePart subPart = getMessagePart(mimeSubparts[i]);
                if(subPart != null) {
                    ((MultiPart)part).addPart(subPart);
                }
            }
            return part;
        }
        // Handle the single-part case
        else {
            byte[] buffer;
            // Handle encoded binary data (should be more encoding-agnostic)
            if(encoding.equalsIgnoreCase("base64") && mimeInputStream.isPartComplete()!=0) {
                SharedInputStream sis = mimeInputStream.getRawMIMEInputStream();
                buffer = StringParser.readWholeStream(sis);

                int offset = 0;
                while((offset+3 < buffer.length) &&
                        !(buffer[offset]=='\r' && buffer[offset+1]=='\n' &&
                        buffer[offset+2]=='\r' && buffer[offset+3]=='\n')) {
                    offset++;
                }
                int size = buffer.length - offset;
                return MessagePartFactory.createMessagePart(type, subtype, encoding, charset, new String(buffer, offset, size));
            }
            else {
                buffer = StringParser.readWholeStream(mimeInputStream);
                return MessagePartFactory.createMessagePart(type, subtype, encoding, charset, new String(buffer));
            }
        }
    }
}