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

package org.logicprobe.LogicMail.mail;

import org.logicprobe.LogicMail.util.EventListener;

/**
 * Listener for Folder events.
 */
public interface FolderListener extends EventListener {
	/**
	 * Invoked when the status of a folder has changed.
	 * 
	 * @param e Folder event data
	 */
	public void folderStatusChanged(FolderEvent e);
	
	/**
	 * Invoked when the message list for a folder has been loaded.
	 * 
	 * @param e Folder event data
	 */
	public void folderMessagesAvailable(FolderMessagesEvent e);
    
    /**
     * Invoked when a folder has been expunged.
     * 
     * @param e Folder event data
     */
    public void folderExpunged(FolderExpungedEvent e);
    
    /**
     * Invoked when the folder state has changed significantly enough that a
     * refresh operation is required to reliably maintain the folder selection.
     * <p>
     * This listener method exists to support the <code>MailStoreServices</code>
     * layer, and should be cleaned up in a future refactoring.
     * </p>
     * 
     * @param e Folder event data
     */
    public void folderRefreshRequired(FolderEvent e);
}
