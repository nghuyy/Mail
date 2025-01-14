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
package org.logicprobe.LogicMail.ui;

import net.rim.device.api.ui.Field;
import net.rim.device.api.ui.component.TreeField;
import net.rim.device.api.ui.component.TreeFieldCallback;

import org.logicprobe.LogicMail.message.TextContent;
import org.logicprobe.LogicMail.model.MessageNode;
import org.logicprobe.LogicMail.util.PlatformUtils;

public abstract class FieldFactory {
    private static FieldFactory instance;

    /**
     * Array of concrete FieldFactory classes, in order from the highest
     * API version to the lowest.
     */
    private static String[] factoryClasses = {
        "org.logicprobe.LogicMail.ui.FieldFactoryBB60",
        "org.logicprobe.LogicMail.ui.FieldFactoryBB50",
        "org.logicprobe.LogicMail.ui.FieldFactoryBB47",
        "org.logicprobe.LogicMail.ui.FieldFactoryBB46",
        "org.logicprobe.LogicMail.ui.FieldFactoryBB45"
    };
    
    public static synchronized FieldFactory getInstance() {
        if(instance == null) {
            instance = (FieldFactory)PlatformUtils.getFactoryInstance(factoryClasses);
        }
        return instance;
    }

    public TreeField getScreenTreeField(TreeFieldCallback callback, long style) {
        return getScreenTreeField(callback, true, style);
    }
    
    public abstract TreeField getScreenTreeField(TreeFieldCallback callback, boolean navigation, long style);
    
    public abstract BorderedFieldManager getBorderedFieldManager();
    
    public abstract BorderedFieldManager getBorderedFieldManager(long style);
    
    /**
     * Gets a new instance of a field for displaying HTML message content.
     * This method needs a reference to the message node itself, since HTML
     * content may reference embedded elements contained elsewhere in the
     * message data.
     *
     * @param messageNode the message node being displayed
     * @param content the specific content object containing the HTML text
     * @return a field that displays the content.
     */
    public abstract Field getHtmlMessageContentField(MessageNode messageNode, TextContent content);

    public abstract void addRoundedBorder(Field field);
}
