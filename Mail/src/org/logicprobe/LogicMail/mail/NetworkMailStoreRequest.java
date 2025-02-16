/*-
 * Copyright (c) 2011, Derek Konigsberg
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

import java.io.IOException;

import net.rim.device.api.i18n.MessageFormat;

import org.logicprobe.LogicMail.conf.AccountConfig;
import org.logicprobe.LogicMail.util.StringParser;

abstract class NetworkMailStoreRequest extends AbstractMailStoreRequest implements ConnectionHandlerRequest {
    protected final NetworkMailStore mailStore;
    protected final AccountConfig config;
    private boolean deliberate = true;
    
    NetworkMailStoreRequest(NetworkMailStore mailStore) {
        this.mailStore = mailStore;
        this.config = mailStore.getAccountConfig();
    }
    
    public AbstractMailStore getMailStore() {
        return mailStore;
    }

    public void setDeliberate(boolean deliberate) {
        this.deliberate = deliberate;
    }
    
    public boolean isDeliberate() {
        return deliberate;
    }
    
    public boolean isAdministrative() {
        return false;
    }
    
    protected abstract String getInitialStatus();
    
    public void showInitialStatus() {
        showStatus(getInitialStatus());
    }
    
    public void notifyConnectionRequestFailed(Throwable exception, boolean isFinal) {
        fireMailStoreRequestFailed(exception, isFinal);
    }
    
    protected boolean checkActiveFolder(IncomingMailClient incomingClient, FolderTreeItem requestFolder) throws IOException, MailException {
        if(incomingClient.getActiveFolder() == null || !incomingClient.getActiveFolder().getPath().equals(requestFolder.getPath())) {
            handleSetActiveFolder(incomingClient, requestFolder);
            return true;
        }
        else {
            return false;
        }
    }
    
    protected void checkActiveFolder(IncomingMailClient incomingClient, MessageToken messageToken) throws IOException, MailException {
        FolderTreeItem invalidFolder = incomingClient.setActiveFolder(messageToken, true);
        
        if(invalidFolder != null) {
            mailStore.fireFolderRefreshRequired(invalidFolder, this.deliberate);
        }
    }
    
    protected void handleSetActiveFolder(IncomingMailClient incomingClient, FolderTreeItem folder) throws IOException, MailException {
        boolean isStateValid = incomingClient.setActiveFolder(folder, true);
        
        if(!isStateValid) {
            mailStore.fireFolderRefreshRequired(folder, this.deliberate);
        }
    }
    
    /**
     * Show a status message.
     * 
     * @param message The message to show
     */
    protected void showStatus(String message) {
        MailConnectionManager.getInstance().fireMailConnectionStatus(config, this, message);
    }
    
    /**
     * Show a status message with a progress percentage.
     * 
     * @param message The message to show
     * @param progress The progress percentage
     */
    protected void showStatus(String message, int progress) {
        MailConnectionManager.getInstance().fireMailConnectionStatus(config, this, message, progress);
    }
    
    /**
     * Gets a progress handler for a request with the provided status message.
     * <p>
     * This default progress handler displays the message along with the
     * amount of data downloaded for network updates, and percentage complete
     * for processing updates.
     * </p>
     * 
     * @param message The status message for the request
     * @return The progress handler
     */
    protected MailProgressHandler getProgressHandler(String message) {
        return new MailConnectionProgressHandler(message);
    }

    private static final MessageFormat networkMessageFormat = new MessageFormat("{0} ({1})...");
    private static final MessageFormat processingMessageFormat = new MessageFormat("{0} ({1}%)...");
    
    /**
     * Standard progress handler to be used for mail connection handlers.
     * This takes the operation's message and appends a relevant status
     * indicator to it.
     */
    private class MailConnectionProgressHandler implements MailProgressHandler {
        private int total = 0;
        private int lastTotal = 0;
        private int threshold = 128;
        private final String message;
        
        public MailConnectionProgressHandler(String message) {
            this.message = message;
        }
        
        public void mailProgress(int type, int count, int max) {
            if(type == MailProgressHandler.TYPE_NETWORK) {
                total += count;
                if((total - lastTotal) >= threshold) {
                    showStatus(networkMessageFormat.format(new Object[] { message, StringParser.toDataSizeString(total) }));
                    lastTotal = total;
                    if(threshold < 1024 && total >= 1024) { threshold = 1024; }
                }
            }
            else if(type == MailProgressHandler.TYPE_PROCESSING && max > 0){
                if(count > 0) { count = (count * 100) / max; }
                showStatus(processingMessageFormat.format(new Object[] { message, Integer.toString(count) }));
            }
        }
    };
}
