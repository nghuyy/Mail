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

/**
 * Abstract implementation for classes wanting to only partially implement
 * the visitor interface for handling a message part structure.
 */
public class AbstractMimeMessagePartVisitor implements MimeMessagePartVisitor {
	public void visitApplicationPart(ApplicationPart part) { }
	public void visitAudioPart(AudioPart part) { }
	public void visitImagePart(ImagePart part) { }
	public void visitMessagePart(MessagePart part) { }
	public void visitTextPart(TextPart part) { }
	public void visitVideoPart(VideoPart part) { }
	public void visitUnsupportedPart(UnsupportedPart part) { }
    public void visitMultiPart(MultiPart part) {
        MimeMessagePart[] parts = part.getParts();
        if(parts != null) {
            for(int i=0;i<parts.length;i++)
                parts[i].accept(this);
        }
    }
}
