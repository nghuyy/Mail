/*-
 * Copyright (c) 2006, Derek Konigsberg
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Hashtable;
import net.rim.device.api.mime.MIMEOutputStream;
import org.logicprobe.LogicMail.util.StringParser;
import org.logicprobe.LogicMail.util.UtilProxy;

/**
 * Converts a message into the equivalent MIME structure
 */
public class MessageMimeConverter implements MessagePartVisitor {
    private ByteArrayOutputStream byteArrayOutputStream;
    private MIMEOutputStream mimeOutputStream;
    private UtilProxy utilProxy;
    
    /** maps message parts to MIMEOutputStream objects */
    private Hashtable partMimeMap;
    
    /** Creates a new instance of MessageMimeConverter */
    public MessageMimeConverter() {
        byteArrayOutputStream = new ByteArrayOutputStream();
        mimeOutputStream = null;
        utilProxy = UtilProxy.getInstance();
        partMimeMap = new Hashtable();
    }

    /**
     * Get the contents of this converter.
     * Due to the internal implementation, this method
     * may only be called once per instance.
     *
     * @return Message encoded in MIME format
     */
    public String toMimeString() {
        try {
            mimeOutputStream.flush();
            mimeOutputStream.close();
        } catch (IOException ex) {
            System.err.println("Unable to close MIMEOutputStream");
            return "";
        }
        return byteArrayOutputStream.toString();
    }
    
    public void visitMultiPart(MultiPart part) {
        // Handle the case of this being the root part
        if(mimeOutputStream == null) {
            mimeOutputStream = new MIMEOutputStream(byteArrayOutputStream, true, null);
            mimeOutputStream.setContentType(part.getMimeType() + "/" + part.getMimeSubtype());
            partMimeMap.put(part, mimeOutputStream);
        }
        // Otherwise handle the case of this being a child part
        else {
            MIMEOutputStream parentStream = (MIMEOutputStream)partMimeMap.get(part.getParent());
            MIMEOutputStream currentStream = parentStream.getPartOutputStream(true, null);
            partMimeMap.put(part, currentStream);
        }
    }

    public void visitTextPart(TextPart part) {
        String charset = part.getCharset();
        boolean isBinary;
        boolean isQP;
        String encoding;
        
        if(charset.equalsIgnoreCase("US-ASCII")) {
            isBinary = false;
            isQP = false;
            encoding = "7bit";
        }
        else if(charset.equalsIgnoreCase("ISO-8859-1")) {
            isBinary = false;
            isQP = true;
            encoding = "quoted-printable";
        }
        else {
            isBinary = true;
            isQP = false;
            encoding = "base64";
        }
        
        MIMEOutputStream currentStream;
        if(mimeOutputStream == null) {
            mimeOutputStream = new MIMEOutputStream(byteArrayOutputStream, false, encoding);
            currentStream = mimeOutputStream;
        }
        else {
            MIMEOutputStream parentStream = (MIMEOutputStream)partMimeMap.get(part.getParent());
            currentStream = parentStream.getPartOutputStream(false, encoding);
        }
        partMimeMap.put(part, currentStream);
        currentStream.setContentType(part.getMimeType() + "/" + part.getMimeSubtype());
        currentStream.addContentTypeParameter("charset", charset.toLowerCase());

        // Add the content, encoding as necessary
        try {
            if(!isBinary) {
                if(isQP)
                    currentStream.write(StringParser.encodeQuotedPrintable(part.getText()).getBytes(charset));
                else
                    currentStream.write(part.getText().getBytes(charset));
            }
            else {
                byte[] data = part.getText().getBytes(charset);
                currentStream.write(utilProxy.Base64Encode(data, 0, data.length, true, true));
            }
        } catch (IOException e) {
            System.err.println("Error encoding content");
        }
    }

    public void visitImagePart(ImagePart part) {
        MIMEOutputStream currentStream;
        if(mimeOutputStream == null) {
            mimeOutputStream = new MIMEOutputStream(byteArrayOutputStream, false, "base64");
            currentStream = mimeOutputStream;
        }
        else {
            MIMEOutputStream parentStream = (MIMEOutputStream)partMimeMap.get(part.getParent());
            currentStream = parentStream.getPartOutputStream(false, "base64");
        }
        partMimeMap.put(part, currentStream);
        currentStream.setContentType(part.getMimeType() + "/" + part.getMimeSubtype());
        
        try {
            byte[] data = part.getImage().getData();
            currentStream.write(utilProxy.Base64Encode(data, 0, data.length, true, true));
        } catch (IOException e) {
            System.err.println("Error encoding content");
        }
    }

    public void visitUnsupportedPart(UnsupportedPart part) {
        MIMEOutputStream currentStream;
        if(mimeOutputStream == null) {
            mimeOutputStream = new MIMEOutputStream(byteArrayOutputStream, false, "7bit");
            currentStream = mimeOutputStream;
        }
        else {
            MIMEOutputStream parentStream = (MIMEOutputStream)partMimeMap.get(part.getParent());
            currentStream = parentStream.getPartOutputStream(false, "7bit");
        }
        partMimeMap.put(part, currentStream);
        currentStream.setContentType("text/plain");
        try {
            currentStream.write("Unable to encode part".getBytes());
        } catch (IOException e) {
            System.err.println("Error encoding content");
        }
    }
}