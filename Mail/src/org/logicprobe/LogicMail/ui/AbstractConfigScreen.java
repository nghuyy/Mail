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
package org.logicprobe.LogicMail.ui;

import org.logicprobe.LogicMail.LogicMailResource;
import org.logicprobe.LogicMail.PlatformInfo;

import net.rim.device.api.i18n.ResourceBundle;
import net.rim.device.api.ui.Screen;
import net.rim.device.api.ui.container.MainScreen;
import net.rim.device.api.ui.component.LabelField;

/**
 * This class is the base for all configuration screens
 * in LogicMail. Its purpose is to provide uniform menu
 * and event handler interfaces for those screens.
 */
public abstract class AbstractConfigScreen extends MainScreen {
	protected static final boolean hasIndicators = PlatformInfo.getInstance().hasApplicationIndicators();
	private final String screenName;
	private String screenPath = "";
	
    /**
     * Instantiates a new abstract configuration screen.
     * 
     * @param title the title
     */
    public AbstractConfigScreen(String title) {
        LabelField titleField =
                new LabelField(title, LabelField.ELLIPSIS | LabelField.USE_ALL_WIDTH);
        setTitle(titleField);
        screenName = getSimpleClassName(getClass().getName());
    }
    
    private static String getSimpleClassName(String className) {
        int p = className.lastIndexOf('.');
        if(p != -1 && p < className.length() - 1) {
            return className.substring(p + 1);
        }
        else {
            return className;
        }
    }
    
    protected String getScreenName() {
        return screenName;
    }
    
    protected String getScreenPath() {
        return screenPath;
    }
    
    protected void onUiEngineAttached(boolean attached) {
        if(attached) {
            Screen screen = getScreenBelow();
            if(screen instanceof StandardScreen) {
                screenPath = ((StandardScreen)screen).getScreenPath() + '/' + screenName;
            }
            else {
                screenPath = '/' + screenName;
            }
        }
        else {
            screenPath = "";
        }
    }
}
