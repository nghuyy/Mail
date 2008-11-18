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

package org.logicprobe.LogicMail.mail.pop;

import java.io.IOException;
import org.logicprobe.LogicMail.conf.AccountConfig;
import org.logicprobe.LogicMail.conf.ConnectionConfig;
import org.logicprobe.LogicMail.conf.GlobalConfig;
import org.logicprobe.LogicMail.conf.MailSettings;
import org.logicprobe.LogicMail.conf.MailSettingsEvent;
import org.logicprobe.LogicMail.conf.MailSettingsListener;
import org.logicprobe.LogicMail.conf.PopConfig;
import org.logicprobe.LogicMail.mail.FolderTreeItem;
import org.logicprobe.LogicMail.mail.IncomingMailClient;
import org.logicprobe.LogicMail.mail.MailException;
import org.logicprobe.LogicMail.message.FolderMessage;
import org.logicprobe.LogicMail.message.Message;
import org.logicprobe.LogicMail.message.MessageEnvelope;
import org.logicprobe.LogicMail.message.MessagePart;
import org.logicprobe.LogicMail.util.Connection;
import org.logicprobe.LogicMail.util.MailMessageParser;
import org.logicprobe.LogicMail.util.StringParser;

/**
 * 
 * Implements the POP3 client
 * 
 */
public class PopClient implements IncomingMailClient {
    private GlobalConfig globalConfig;
    private PopConfig accountConfig;
    private Connection connection;
    private PopProtocol popProtocol;
    private String username;
    private String password;
    private boolean openStarted;
    private boolean configChanged;
    
    /**
     * Active mailbox.  Since POP3 does not support multiple
     * mailboxes for a user, it is used to contain some
     * relevant information for the user's single mailbox.
     */
    private FolderTreeItem activeMailbox = null;
    
    /** Creates a new instance of PopClient */
    public PopClient(GlobalConfig globalConfig, PopConfig accountConfig) {
        this.accountConfig = accountConfig;
        this.globalConfig = globalConfig;
        connection = new Connection(
                accountConfig.getServerName(),
                accountConfig.getServerPort(),
                accountConfig.getServerSSL(),
                accountConfig.getDeviceSide());
        popProtocol = new PopProtocol(connection);
        username = accountConfig.getServerUser();
        password = accountConfig.getServerPass();
        
        // Create our dummy folder item for the inbox
        activeMailbox = new FolderTreeItem("INBOX", "INBOX", "");
        activeMailbox.setMsgCount(0);
        openStarted = false;
        configChanged = false;
        MailSettings.getInstance().addMailSettingsListener(mailSettingsListener);
    }

    private MailSettingsListener mailSettingsListener = new MailSettingsListener() {
		public void mailSettingsSaved(MailSettingsEvent e) {
			mailSettings_MailSettingsSaved(e);
		}
    };
    
    private void mailSettings_MailSettingsSaved(MailSettingsEvent e) {
		if(MailSettings.getInstance().containsAccountConfig(accountConfig)) {
			// Refresh authentication information from the configuration
	        username = accountConfig.getServerUser();
	        password = accountConfig.getServerPass();
	        
	        if(!isConnected()) {
	        	// Rebuild the connection to include new settings
	            connection = new Connection(
	                    accountConfig.getServerName(),
	                    accountConfig.getServerPort(),
	                    accountConfig.getServerSSL(),
	                    accountConfig.getDeviceSide());
	            popProtocol = new PopProtocol(connection);
	        }
	        else {
		        // Set a flag to make sure we rebuild the Connection object
		        // the next time we close the connection.
		        configChanged = true;
	        }
		}
		else {
			// We have been deleted, so unregister to make sure we
			// no longer affect the system and can be garbage collected.
			MailSettings.getInstance().removeMailSettingsListener(mailSettingsListener);
		}
    }

    public AccountConfig getAcctConfig() {
        return accountConfig;
    }

    public ConnectionConfig getConnectionConfig() {
		return getAcctConfig();
	}

    public boolean open() throws IOException, MailException {
        if(!openStarted) {
            connection.open();
            // Eat the initial server response
            connection.receive();
            openStarted = true;
        }
        
        try {
            // Login to the server
            popProtocol.executeUser(username);
            popProtocol.executePass(password);
        } catch (MailException exp) {
            return false;
        }
        // Update message counts
        activeMailbox.setMsgCount(popProtocol.executeStat());
        
        openStarted = false;
        return true;
    }

    public void close() throws IOException, MailException {
        if(connection.isConnected()) {
            try {
                popProtocol.executeQuit();
            } catch (Exception exp) { }
        }
        connection.close();
        
        if(configChanged) {
        	// Rebuild the connection to include new settings
            connection = new Connection(
                    accountConfig.getServerName(),
                    accountConfig.getServerPort(),
                    accountConfig.getServerSSL(),
                    accountConfig.getDeviceSide());
            popProtocol = new PopProtocol(connection);
            configChanged = false;
        }
    }

    public boolean isConnected() {
        return connection.isConnected();
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean hasFolders() {
        return false;
    }

    public boolean hasUndelete() {
        return false;
    }

    public boolean hasIdle() {
		return false;
	}

    public FolderTreeItem getFolderTree() throws IOException, MailException {
        return null;
    }

    public void refreshFolderStatus(FolderTreeItem[] folders) throws IOException, MailException {
        // Only one mailbox can exist, so we just pull the message counts
        activeMailbox.setMsgCount(popProtocol.executeStat());
        if(folders.length == 1 && folders[0] != activeMailbox) {
        	folders[0].setMsgCount(activeMailbox.getMsgCount());
        }
    }

    public FolderTreeItem getActiveFolder() {
        return activeMailbox;
    }

    public void setActiveFolder(FolderTreeItem mailbox) throws IOException, MailException {
        // Mailbox cannot be changed, so we just pull the message counts
        activeMailbox.setMsgCount(popProtocol.executeStat());
    }

    public FolderMessage[] getFolderMessages(int firstIndex, int lastIndex) throws IOException, MailException {
        FolderMessage[] folderMessages = new FolderMessage[(lastIndex - firstIndex)+1];
        int index = 0;
        String[] headerText;
        String uid;
        MessageEnvelope env;
        for(int i=firstIndex; i<=lastIndex; i++) {
            headerText = popProtocol.executeTop(i, 0);
            uid = popProtocol.executeUidl(i);
            env = MailMessageParser.parseMessageEnvelope(headerText);
            folderMessages[index++] = new FolderMessage(env, i, uid.hashCode());
        }
        return folderMessages;
    }

    public FolderMessage[] getNewFolderMessages() throws IOException, MailException {
    	int count = MailSettings.getInstance().getGlobalConfig().getRetMsgCount();
		int msgCount = activeMailbox.getMsgCount();
        int firstIndex = Math.max(1, msgCount - count);
    	return getFolderMessages(firstIndex, activeMailbox.getMsgCount());
    }

    public Message getMessage(FolderMessage folderMessage) throws IOException, MailException {
        // Figure out the max number of lines
        int maxLines = globalConfig.getPopMaxLines();

        // Download the message text
        String[] message = popProtocol.executeTop((folderMessage.getIndex()), maxLines);
        
        MessagePart rootPart = MailMessageParser.parseRawMessage(StringParser.createInputStream(message));
        Message msg = new Message(folderMessage.getEnvelope(), rootPart);
        return msg;
    }

    public void deleteMessage(FolderMessage folderMessage) throws IOException, MailException {
        popProtocol.executeDele(folderMessage.getIndex());
        folderMessage.setDeleted(true);
    }

    public void undeleteMessage(FolderMessage folderMessage) throws IOException, MailException {
        // Undelete is not supported, so we do nothing here.
    }

    public boolean noop() throws IOException, MailException {
    	popProtocol.executeNoop();
		return false;
	}

	public void idleModeBegin() throws IOException, MailException {
		// Idle mode is not supported, so we do nothing here.
	}

	public void idleModeEnd() throws IOException, MailException {
		// Idle mode is not supported, so we do nothing here.
	}

	public boolean idleModePoll() throws IOException, MailException {
		// Idle mode is not supported, so we do nothing here.
		return false;
	}

}
