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

import net.rim.device.api.system.KeypadListener;
import net.rim.device.api.ui.Field;
import net.rim.device.api.ui.FieldChangeListener;
import net.rim.device.api.ui.Keypad;
import net.rim.device.api.ui.Manager;
import net.rim.device.api.ui.component.EditField;
import net.rim.device.api.ui.component.SeparatorField;
import net.rim.device.api.util.Arrays;

/**
 * This is the message composition screen.
 */
public class CompositionScreen extends BaseScreen implements FieldChangeListener {
    private EmailAddressBookEditField[] fldTo;
    private EmailAddressBookEditField[] fldCC;
    private EditField fldSubject;
    private EditField fldEdit;
    
    /** Creates a new instance of CompositionScreen */
    public CompositionScreen() {
        fldTo = new EmailAddressBookEditField[1];
        fldTo[0] = new EmailAddressBookEditField("To: ", "");
        fldCC = new EmailAddressBookEditField[1];
        fldCC[0] = new EmailAddressBookEditField("CC: ", "");
        
        fldSubject = new EditField("Subject: ", "");
        fldEdit = new EditField();
        this.showFields();
    }
    
    private void addToField() {
        EmailAddressBookEditField fld =
            new EmailAddressBookEditField("To: ", "");
        if(fldTo == null) {
            fldTo = new EmailAddressBookEditField[1];
            this.add(fld);
            fldTo[0] = fld;
        }
        else {
            this.insert(fld, fldTo[fldTo.length-1].getIndex()+1);
            Arrays.add(fldTo, fld);
        }
    }

    private void addCCField() {
        EmailAddressBookEditField fld =
            new EmailAddressBookEditField("CC: ", "");
        int index = 0;
        // continue here
        if(fldCC == null) {
            fldCC = new EmailAddressBookEditField[1];
            fldTo[0] = fld;
        }
        else {
            this.insert(fld, fldTo[fldTo.length-1].getIndex()+1);
            Arrays.add(fldTo, fld);
        }
    }

    private void showFields() {
        int i;
        this.deleteAll();
        for(i=0;i<fldTo.length;i++)
            this.add(fldTo[i]);
        for(i=0;i<fldCC.length;i++)
            this.add(fldCC[i]);
        this.add(fldSubject);
        this.add(new SeparatorField());
        this.add(fldEdit);
    }
    
    public boolean keyChar(char key,
                           int status,
                           int time)
    {
        boolean retval = false;
        switch(key) {
            case Keypad.KEY_ENTER:
            case Keypad.KEY_SPACE:
                if(status == 0) {
                    scroll(Manager.DOWNWARD);
                    retval = true;
                }
                else if(status == KeypadListener.STATUS_ALT) {
                    scroll(Manager.UPWARD);
                    retval = true;
                }
                break;
        }
        return retval;
    }

    public void fieldChanged(Field field, int context) {
        // @TODO
    }
}