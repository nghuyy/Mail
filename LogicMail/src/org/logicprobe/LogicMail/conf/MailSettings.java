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

package org.logicprobe.LogicMail.conf;

import java.util.Vector;
import javax.microedition.rms.*;

/**
 * Provide an interface to global and account-specific
 * configuration, along with a front-end to the Record Store
 */
public class MailSettings {
    private static MailSettings _instance;
    private GlobalConfig _globalConfig;
    private Vector _accountConfigs;
    
    private MailSettings() {
        _globalConfig = new GlobalConfig();
        _accountConfigs = new Vector();
    }
    
    public static synchronized MailSettings getInstance() {
        if(_instance == null)
            _instance = new MailSettings();
        return _instance;
    }
    
    public GlobalConfig getGlobalConfig() {
        return _globalConfig;
    }
    
    public int getNumAccounts() {
        return _accountConfigs.size();
    }
    
    public AccountConfig getAccountConfig(int index) {
        return (AccountConfig)_accountConfigs.elementAt(index);
    }
    
    public void addAccountConfig(AccountConfig accountConfig) {
        _accountConfigs.addElement(accountConfig);
    }
    
    public void removeAccountConfig(int index) {
        _accountConfigs.removeElementAt(index);
    }
    
    public void saveSettings() {
        try {
            RecordStore.deleteRecordStore("LogicMail_config");
        } catch (RecordStoreException exp) {
            // do nothing
        }
        
        RecordStore store = null;
        try {
            store = RecordStore.openRecordStore("LogicMail_config", true);
            byte[] buffer;
            buffer = _globalConfig.serialize();
            store.addRecord(buffer, 0, buffer.length);
        
            for(int i=0;i<_accountConfigs.size();i++) {
                buffer = ((AccountConfig)_accountConfigs.elementAt(i)).serialize();
                store.addRecord(buffer, 0, buffer.length);
            }
        } catch (RecordStoreException exp) {
            // do nothing
        }
        
        try {
            if(store != null) store.closeRecordStore();
        } catch (RecordStoreException exp) {
            // do nothing
        }
    }
    
    public void loadSettings() {
        _accountConfigs.removeAllElements();
        RecordStore store = null;
        try {
            store = RecordStore.openRecordStore("LogicMail_config", false);
            byte[] buffer;
            
            int records = store.getNumRecords();
            
            if(records >= 1) {
                buffer = store.getRecord(1);
                _globalConfig.deserialize(buffer);
            
                if(records > 1) {
                    for(int i=2;i<=store.getNumRecords();i++) {
                        buffer = store.getRecord(i);
                        _accountConfigs.addElement(new AccountConfig(buffer));
                    }
                }
            }
        } catch (RecordStoreException exp) {
            // do nothing
        }
        
        try {
            if(store != null) store.closeRecordStore();
        } catch (RecordStoreException exp) {
            // do nothing
        }
    }
}

