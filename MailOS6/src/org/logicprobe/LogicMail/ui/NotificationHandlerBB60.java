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
package org.logicprobe.LogicMail.ui;

import net.rim.blackberry.api.homescreen.HomeScreen;
import net.rim.device.api.system.ApplicationDescriptor;
import net.rim.device.api.system.CodeModuleManager;

import org.logicprobe.LogicMail.AppInfo;

public class NotificationHandlerBB60 extends NotificationHandlerBB46 {
    private boolean iconSet;
    private final ApplicationDescriptor[] appDescriptors;

    public NotificationHandlerBB60() {
        super();
        this.appDescriptors = findApplicationDescriptors();
    }
    
    private static ApplicationDescriptor[] findApplicationDescriptors() {
        ApplicationDescriptor[] result = null;
        try {
            int moduleHandle = ApplicationDescriptor.currentApplicationDescriptor().getModuleHandle();
            result = CodeModuleManager.getApplicationDescriptors(moduleHandle);
        } catch (Exception e) { }
        return result;
    }
    
    protected void updateIndicator(int count, boolean recent) {
        indicator.setNotificationState(recent);
        indicator.setValue(count);
        indicator.setVisible(count > 0);
    }
    
    protected void setAppIcon(boolean newMessages) {
        if(appDescriptors != null) {
            if(!iconSet) {
                for(int i=0; i<appDescriptors.length; i++) {
                    HomeScreen.updateIcon(AppInfo.getIcon(), appDescriptors[i]);
                    HomeScreen.setRolloverIcon(AppInfo.getRolloverIcon(), appDescriptors[i]);
                }
                iconSet = true;
            }
            for(int i=0; i<appDescriptors.length; i++) {
                HomeScreen.setNewState(newMessages, appDescriptors[i]);
            }
        }
        else {
            super.setAppIcon(newMessages);
        }
    }
}
