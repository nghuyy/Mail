/*-
 * Copyright (c) 2007, Derek Konigsberg
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

import net.rim.device.api.i18n.Locale;
import net.rim.device.api.system.Bitmap;
import net.rim.device.api.ui.DrawStyle;
import net.rim.device.api.ui.Field;
import net.rim.device.api.ui.FieldChangeListener;
import net.rim.device.api.ui.Graphics;
import net.rim.device.api.ui.Keypad;
import net.rim.device.api.ui.Manager;
import net.rim.device.api.ui.MenuItem;
import net.rim.device.api.ui.UiApplication;
import net.rim.device.api.ui.component.BasicEditField;
import net.rim.device.api.ui.component.ButtonField;
import net.rim.device.api.ui.component.CheckboxField;
import net.rim.device.api.ui.component.Dialog;
import net.rim.device.api.ui.component.LabelField;
import net.rim.device.api.ui.component.ListField;
import net.rim.device.api.ui.component.Menu;
import net.rim.device.api.ui.component.ObjectChoiceField;
import net.rim.device.api.ui.component.ObjectListField;
import net.rim.device.api.ui.container.HorizontalFieldManager;
import net.rim.device.api.ui.container.VerticalFieldManager;

import org.logicprobe.LogicMail.AnalyticsDataCollector;
import org.logicprobe.LogicMail.AppInfo;
import org.logicprobe.LogicMail.LogicMailResource;
import org.logicprobe.LogicMail.PlatformInfo;
import org.logicprobe.LogicMail.conf.AccountConfig;
import org.logicprobe.LogicMail.conf.ConnectionConfig;
import org.logicprobe.LogicMail.conf.GlobalConfig;
import org.logicprobe.LogicMail.conf.IdentityConfig;
import org.logicprobe.LogicMail.conf.ImapConfig;
import org.logicprobe.LogicMail.conf.MailSettings;
import org.logicprobe.LogicMail.conf.MailSettingsEvent;
import org.logicprobe.LogicMail.conf.MailSettingsListener;
import org.logicprobe.LogicMail.conf.OutgoingConfig;
import org.logicprobe.LogicMail.conf.PopConfig;
import org.logicprobe.LogicMail.model.MailManager;

/**
 * This screen is the main entry point to all the
 * other configuration screens.
 */
public class ConfigScreen extends AbstractConfigScreen {
    private MailSettings mailSettings;
    private GlobalConfig existingGlobalConfig;
    private String localHostname;
    private String[] languageChoices;
    private String[] languageCodes;
    private String[] fileSystemRoots;
    private String[] fileSystemRootChoices;
    private int selectedFileSystemRootIndex;
    
    int separatorHeight;
    
    // Account management
    private VerticalFieldManager identityFieldManager;
    private ObjectListField identityListField;
    private VerticalFieldManager accountFieldManager;
    private ObjectListField accountListField;
    private VerticalFieldManager outgoingFieldManager;
    private ObjectListField outgoingListField;
    
    // Message Display
    private VerticalFieldManager messageDisplayFieldManager;
    private ObjectChoiceField messageDisplayChoiceField;
    private ObjectChoiceField displayOrderChoiceField;
    private CheckboxField hideDeletedMessagesCheckboxField;
    private CheckboxField promptOnDeleteCheckboxField;
    private ObjectChoiceField expungeModeChoiceField;
    
    // Networking
    //private VerticalFieldManager networkingFieldManager;
    //private ObjectChoiceField networkTransportChoiceField;
    //private CheckboxField enableWiFiCheckboxField;
    //private CheckboxField enableHandoverCheckboxField;
    //private CheckboxField overrideHostnameCheckboxField;
    //private BasicEditField localHostnameEditField;

    // Other
    private VerticalFieldManager otherFieldManager;
    private CheckboxField autoStartupCheckboxField;
    private CheckboxField notificationIconCheckboxField;
    private ObjectChoiceField localDataLocationChoiceLabel;
    //private CheckboxField connectionDebuggingCheckboxField;
    //private CheckboxField unicodeNormalizationCheckboxField;
    private ObjectChoiceField languageChoiceField;
    private ButtonField clearCacheButtonField;
    private CheckboxField analyticsCheckboxField;
    
    private volatile boolean clearingCache;
    
    private MenuItem selectItem;
    private MenuItem moveUpItem;
    private MenuItem moveDownItem;
    private MenuItem newAccountWizardItem;
    private MenuItem addIdentityItem;
    private MenuItem deleteIdentityItem;
    private MenuItem addAccountItem;
    private MenuItem deleteAccountItem;
    private MenuItem addOutgoingItem;
    private MenuItem deleteOutgoingItem;

    /**
     * Instantiates a new configuration screen.
     */
    public ConfigScreen() {
        super(LogicMailResource.APPNAME + " - " + LogicMailResource.CONFIG_TITLE);
        
        mailSettings = MailSettings.getInstance();
        existingGlobalConfig = mailSettings.getGlobalConfig();
        
        localHostname = existingGlobalConfig.getLocalHostname();
        
        initLanguageChoices();
        initFileSystemChoices();

        separatorHeight = getFont().getHeight() >> 1;
        
        initFields();
        initMenuItems();

        buildAccountsList();
    }

    MailSettingsListener mailSettingsListener = new MailSettingsListener() {
        public void mailSettingsSaved(MailSettingsEvent e) {
            buildAccountsList();
        }
    };
    
    protected void onUiEngineAttached(boolean attached) {
        super.onUiEngineAttached(attached);
        if(attached) {
            MailSettings.getInstance().addMailSettingsListener(mailSettingsListener);
            AnalyticsDataCollector.getInstance().onScreenView(getScreenPath(), getScreenName(), "Global", "Configuration");
        }
        else {
            MailSettings.getInstance().removeMailSettingsListener(mailSettingsListener);
        }
    };
    
    private void initFileSystemChoices() {
        // Populate fileSystemRoots with a list of all
        // available and writable storage devices
        String selectedFileSystemRoot = existingGlobalConfig.getFilesystemRoot();
        selectedFileSystemRootIndex = 0;
        
        fileSystemRoots = PlatformInfo.getInstance().getFilesystemRoots();
        fileSystemRootChoices = new String[fileSystemRoots.length + 1];
        for(int i=0; i<fileSystemRoots.length; i++) {
            String root = fileSystemRoots[i];
            if(selectedFileSystemRoot != null && selectedFileSystemRoot.indexOf(root) != -1) {
                selectedFileSystemRootIndex = i;
            }
            if(root.indexOf("Card/") != -1) {
                fileSystemRootChoices[i] =
                    LogicMailResource.CONFIG_GLOBAL_LOCAL_DATA_MEDIA_CARD;
            }
            else if(root.indexOf("store/") != -1) {
                fileSystemRootChoices[i] =
                    LogicMailResource.CONFIG_GLOBAL_LOCAL_DATA_DEVICE_MEMORY;
            }
            else {
                int p = root.indexOf('/', GlobalConfig.FILE_URL_PREFIX.length() - 1);
                int q = root.indexOf('/', p + 1);
                if(p != -1 && q != -1 && p < q) {
                    fileSystemRootChoices[i] = root.substring(p + 1, q);
                }
                else {
                    fileSystemRootChoices[i] = root;
                }
            }
        }
        fileSystemRootChoices[fileSystemRootChoices.length - 1] =
            LogicMailResource.CONFIG_GLOBAL_LOCAL_DATA_DISABLED;
        if(selectedFileSystemRoot.equals(GlobalConfig.FILESYSTEM_DISABLED)) {
            selectedFileSystemRootIndex = fileSystemRootChoices.length - 1;
        }
    }

    private void initLanguageChoices() {
        languageChoices = new String[] {
                "System",   // System default
                "English",      // English: en
                "Ti\u00ea\u0301ng Vi\u00ea\u0323t", // Vietnamese: vi
        };
        languageCodes = new String[] {
                "",   // System default
                "en", // English
                "vi", // Vietnamese
        };
    }

    /**
     * Initializes the fields.
     */
    private void initFields() {
        initAccountFields();
        initMessageDisplayFields();
        //initNetworkingFields();
        initOtherFields();
    }

    private void initAccountFields() {
        identityFieldManager = new VerticalFieldManager();
        identityFieldManager.add(new LabeledSeparatorField(
                LogicMailResource.CONFIG_IDENTITIES,
                Field.NON_FOCUSABLE | LabeledSeparatorField.TOP_BORDER | LabeledSeparatorField.BOTTOM_BORDER));
        identityListField = new AccountListField();
        identityListField.setEmptyString( LogicMailResource.MENUITEM_ADD_IDENTITY, DrawStyle.HCENTER);
        identityFieldManager.add(identityListField);
        identityFieldManager.add(new BlankSeparatorField(separatorHeight));
        add(identityFieldManager);
        
        accountFieldManager = new VerticalFieldManager();
        accountFieldManager.add(new LabeledSeparatorField(
                LogicMailResource.CONFIG_ACCOUNTS,
                Field.NON_FOCUSABLE | LabeledSeparatorField.TOP_BORDER | LabeledSeparatorField.BOTTOM_BORDER));
        accountListField = new AccountListField();
        accountListField.setEmptyString( LogicMailResource.MENUITEM_ADD_ACCOUNT, DrawStyle.HCENTER);
        accountFieldManager.add(accountListField);
        accountFieldManager.add(new BlankSeparatorField(separatorHeight));
        add(accountFieldManager);
        
        outgoingFieldManager = new VerticalFieldManager();
        outgoingFieldManager.add(new LabeledSeparatorField(
                LogicMailResource.CONFIG_OUTGOING_SERVERS,
                Field.NON_FOCUSABLE | LabeledSeparatorField.TOP_BORDER | LabeledSeparatorField.BOTTOM_BORDER));
        outgoingListField = new AccountListField();
        outgoingListField.setEmptyString( LogicMailResource.MENUITEM_ADD_OUTGOING_SERVER, DrawStyle.HCENTER);
        outgoingFieldManager.add(outgoingListField);
        outgoingFieldManager.add(new BlankSeparatorField(separatorHeight));
        add(outgoingFieldManager);
    }
    
    private static class AccountListField extends ObjectListField {
        private static Bitmap chevronIcon = Bitmap.getBitmapResource("chevron_right_black_10x16.png");
        private static Bitmap chevronIconHighlighted = Bitmap.getBitmapResource("chevron_right_white_10x16.png");
        
        private boolean isFocus;
        
        protected void onFocus(int direction) {
            isFocus = true;
            super.onFocus(direction);
            invalidate();
        }
        
        protected void onUnfocus() {
            isFocus = false;
            super.onUnfocus();
            invalidate();
        }
        
        protected int moveFocus(int amount, int status, int time) {
            invalidate();
            return super.moveFocus(amount, status, time);
        }
        
        public void drawListRow(ListField listField, Graphics graphics, int index, int y, int width) {
            int fontHeight = graphics.getFont().getHeight();
            
            int yPos = y + (fontHeight >> 1) - 8;
            if(isFocus && index == getSelectedIndex()) {
                graphics.drawBitmap(width - 15, yPos, 10, 16, chevronIconHighlighted, 0, 0);
            }
            else {
                graphics.drawBitmap(width - 15, yPos, 10, 16, chevronIcon, 0, 0);
            }
            super.drawListRow(listField, graphics, index, y, width - 25);
        }
    }
    
    private void initMessageDisplayFields() {
        messageDisplayFieldManager = new VerticalFieldManager();
        
        messageDisplayChoiceField = new ObjectChoiceField(
                LogicMailResource.CONFIG_GLOBAL_MESSAGE_FORMAT,
                new String[] {
                    LogicMailResource.CONFIG_GLOBAL_MESSAGE_FORMAT_PLAIN_TEXT,
                "HTML" },
                existingGlobalConfig.getMessageDisplayFormat());

        String[] orderTypes = {
                LogicMailResource.MENUITEM_ORDER_ASCENDING,
                LogicMailResource.MENUITEM_ORDER_DESCENDING
        };

        if (existingGlobalConfig.getDispOrder()) {
            displayOrderChoiceField = new ObjectChoiceField(
                    LogicMailResource.CONFIG_GLOBAL_MESSAGE_ORDER + ' ',
                    orderTypes, 0);
        } else {
            displayOrderChoiceField = new ObjectChoiceField(
                   LogicMailResource.CONFIG_GLOBAL_MESSAGE_ORDER + ' ',
                    orderTypes, 1);
        }

        hideDeletedMessagesCheckboxField = new CheckboxField(
                LogicMailResource.CONFIG_GLOBAL_HIDE_DELETED_MESSAGES,
                existingGlobalConfig.getHideDeletedMsg());
        
        promptOnDeleteCheckboxField = new CheckboxField(
                LogicMailResource.CONFIG_GLOBAL_PROMPT_ON_MESSAGE_DELETE,
                existingGlobalConfig.getPromptOnDelete());
        
        expungeModeChoiceField = new ObjectChoiceField(
                LogicMailResource.CONFIG_GLOBAL_EXPUNGE_BEHAVIOR,
                new Object[] {
                    LogicMailResource.CONFIG_GLOBAL_EXPUNGE_PROMPT,
                    LogicMailResource.CONFIG_GLOBAL_EXPUNGE_ALWAYS,
                    LogicMailResource.CONFIG_GLOBAL_EXPUNGE_NEVER
                },
                existingGlobalConfig.getExpungeMode());

        messageDisplayFieldManager.add(new LabeledSeparatorField(
                LogicMailResource.CONFIG_GLOBAL_SECTION_MESSAGE_DISPLAY,
                Field.NON_FOCUSABLE | LabeledSeparatorField.TOP_BORDER | LabeledSeparatorField.BOTTOM_BORDER));
        messageDisplayFieldManager.add(messageDisplayChoiceField);
        messageDisplayFieldManager.add(displayOrderChoiceField);
        messageDisplayFieldManager.add(hideDeletedMessagesCheckboxField);
        messageDisplayFieldManager.add(promptOnDeleteCheckboxField);
        messageDisplayFieldManager.add(expungeModeChoiceField);
        messageDisplayFieldManager.add(new BlankSeparatorField(separatorHeight));
        add(messageDisplayFieldManager);
    }
    /*
    private void initNetworkingFields() {
        networkingFieldManager = new VerticalFieldManager();
        
        String[] transportChoices = {
                LogicMailResource.CONFIG_GLOBAL_TRANSPORT_AUTO,
                LogicMailResource.CONFIG_GLOBAL_TRANSPORT_DIRECT_TCP,
                LogicMailResource.CONFIG_GLOBAL_TRANSPORT_MDS,
                LogicMailResource.CONFIG_GLOBAL_TRANSPORT_WAP2,
                LogicMailResource.CONFIG_GLOBAL_TRANSPORT_WIFI_ONLY
        };
        networkTransportChoiceField = new ObjectChoiceField(
                LogicMailResource.CONFIG_GLOBAL_NETWORK_TRANSPORT,
                transportChoices,
                getTransportChoice(existingGlobalConfig.getTransportType()));
        networkTransportChoiceField.setChangeListener(new FieldChangeListener() {
            public void fieldChanged(Field field, int context) {
                networkTransportChoiceFieldChanged(field, context);
            }
        });
        
        enableWiFiCheckboxField = new CheckboxField(
                LogicMailResource.CONFIG_GLOBAL_ENABLE_WIFI,
                existingGlobalConfig.getEnableWiFi());

        enableHandoverCheckboxField = new CheckboxField(
                LogicMailResource.CONFIG_GLOBAL_ENABLE_HANDOVER,
                existingGlobalConfig.isConnectionHandoverEnabled());
        
        boolean overrideHostname = localHostname.length() > 0;
        overrideHostnameCheckboxField = new CheckboxField(
                LogicMailResource.CONFIG_GLOBAL_OVERRIDE_HOSTNAME,
                overrideHostname);
        overrideHostnameCheckboxField.setChangeListener(new FieldChangeListener() {
            public void fieldChanged(Field field, int context) {
                overrideHostnameCheckboxFieldChanged(field, context);
            }
        });

        if (overrideHostname) {
            localHostnameEditField = new HostnameEditField(
                    LogicMailResource.CONFIG_GLOBAL_HOSTNAME + ' ',
                    localHostname);
        } else {
            String hostname = System.getProperty("microedition.hostname");
            localHostnameEditField = new HostnameEditField(
                    LogicMailResource.CONFIG_GLOBAL_HOSTNAME + ' ',
                    ((hostname != null) ? hostname : "localhost"));
            localHostnameEditField.setEditable(false);
        }
        
        //networkingFieldManager.add(new LabeledSeparatorField(
        //        LogicMailResource.CONFIG_GLOBAL_SECTION_NETWORKING,
       //         Field.NON_FOCUSABLE | LabeledSeparatorField.TOP_BORDER | LabeledSeparatorField.BOTTOM_BORDER));
        //networkingFieldManager.add(networkTransportChoiceField);
        //networkingFieldManager.add(enableWiFiCheckboxField);
        //networkingFieldManager.add(enableHandoverCheckboxField);
        //networkingFieldManager.add(overrideHostnameCheckboxField);
        //networkingFieldManager.add(localHostnameEditField);
        //networkingFieldManager.add(new BlankSeparatorField(separatorHeight));
        //add(networkingFieldManager);
    }
        */
    private void initOtherFields() {
        otherFieldManager = new VerticalFieldManager();
        
        autoStartupCheckboxField = new CheckboxField(
                LogicMailResource.CONFIG_GLOBAL_AUTO_STARTUP,
                existingGlobalConfig.isAutoStartupEnabled());
        
        if(hasIndicators) {
            notificationIconCheckboxField = new CheckboxField(
                    LogicMailResource.CONFIG_GLOBAL_SHOW_NOTIFICATION_ICON,
                    existingGlobalConfig.isNotificationIconShown());
        }
        
        localDataLocationChoiceLabel = new ObjectChoiceField(
                LogicMailResource.CONFIG_GLOBAL_LOCAL_DATA_LOCATION + ' ',
                fileSystemRootChoices,
                selectedFileSystemRootIndex);
        
        String languageCode = existingGlobalConfig.getLanguageCode();
        int languageIndex = 0;
        if(languageCode != null && languageCode.length() != 0) {
            for(int i=0; i<languageCodes.length; i++) {
                if(languageCodes[i].equals(languageCode)) {
                    languageIndex = i;
                    break;
                }
            }
        }
        languageChoiceField = new ObjectChoiceField(
                LogicMailResource.CONFIG_GLOBAL_LANGUAGE,
                languageChoices,
                languageIndex);
        /*
        unicodeNormalizationCheckboxField = new CheckboxField(
                LogicMailResource.CONFIG_GLOBAL_UNICODE_NORMALIZATION,
                existingGlobalConfig.getUnicodeNormalization());

        connectionDebuggingCheckboxField = new CheckboxField(
                LogicMailResource.CONFIG_GLOBAL_CONNECTION_DEBUGGING,
                existingGlobalConfig.getConnDebug());
        */

        LabelField clearCacheLabelField = new LabelField(
                LogicMailResource.CONFIG_GLOBAL_MAILBOX_CACHE + ' ',
                Field.FIELD_VCENTER);
        clearCacheButtonField = new ButtonField(
                LogicMailResource.MENUITEM_CLEAR,
                ButtonField.CONSUME_CLICK | ButtonField.NEVER_DIRTY | Field.FIELD_VCENTER);
        clearCacheButtonField.setChangeListener(new FieldChangeListener() {
            public void fieldChanged(Field field, int context) {
                clearCacheButtonFieldChanged(field, context);
            }
        });
        
        HorizontalFieldManager clearCacheManager = new HorizontalFieldManager(Manager.NO_HORIZONTAL_SCROLL | Manager.FIELD_LEFT);
        clearCacheManager.add(clearCacheLabelField);
        clearCacheManager.add(clearCacheButtonField);


        
        otherFieldManager.add(new LabeledSeparatorField(
                LogicMailResource.CONFIG_GLOBAL_SECTION_OTHER,
                Field.NON_FOCUSABLE | LabeledSeparatorField.TOP_BORDER | LabeledSeparatorField.BOTTOM_BORDER));
        otherFieldManager.add(autoStartupCheckboxField);
        if(hasIndicators) {
            otherFieldManager.add(notificationIconCheckboxField);
        }
        otherFieldManager.add(localDataLocationChoiceLabel);
        otherFieldManager.add(clearCacheManager);
        
        // Locale override is not used in release builds
        otherFieldManager.add(languageChoiceField);
        //otherFieldManager.add(unicodeNormalizationCheckboxField);
        //otherFieldManager.add(connectionDebuggingCheckboxField);

        if(AnalyticsDataCollector.isWebtrendsAvailable()) {
            otherFieldManager.add(analyticsCheckboxField);
        }

        otherFieldManager.add(new BlankSeparatorField(separatorHeight));
        add(otherFieldManager);
    }

    private void initMenuItems() {
        selectItem = new MenuItem( LogicMailResource.MENUITEM_EDIT, 300100, 1) {
            public void run() {
                openSelectedNode();
            }
        };
        moveUpItem = new MenuItem(LogicMailResource.MENUITEM_MOVE_UP, 300200, 1020) {
            public void run() {
                moveSelectedNodeUp();
            }
        };
        moveDownItem = new MenuItem( LogicMailResource.MENUITEM_MOVE_DOWN, 300250, 1020) {
            public void run() {
                moveSelectedNodeDown();
            }
        };
        deleteIdentityItem = new MenuItem( LogicMailResource.MENUITEM_DELETE_IDENTITY, 300310, 4000) {
            public void run() {
                deleteSelectedIdentity();
            }
        };
        deleteAccountItem = new MenuItem( LogicMailResource.MENUITEM_DELETE_ACCOUNT, 300320, 4000) {
            public void run() {
                deleteSelectedAccount();
            }
        };
        deleteOutgoingItem = new MenuItem( LogicMailResource.MENUITEM_DELETE_OUTGOING_SERVER, 300330, 4000) {
            public void run() {
                deleteSelectedOutgoingServer();
            }
        };
        newAccountWizardItem = new MenuItem( LogicMailResource.MENUITEM_NEW_ACCOUNT_WIZARD, 400100, 4000) {
            public void run() {
                newAccountWizard();
            }
        };
        addIdentityItem = new MenuItem( LogicMailResource.MENUITEM_ADD_IDENTITY, 400210, 4000) {
            public void run() {
                addIdentity();
            }
        };
        addAccountItem = new MenuItem( LogicMailResource.MENUITEM_ADD_ACCOUNT, 400220, 4000) {
            public void run() {
                addAccount();
            }
        };
        addOutgoingItem = new MenuItem( LogicMailResource.MENUITEM_ADD_OUTGOING_SERVER, 400230, 4000) {
            public void run() {
                addOutgoingServer();
            }
        };
    }
    
    private static int getTransportChoice(int transportSetting) {
        int result;
        switch(transportSetting) {
        case ConnectionConfig.TRANSPORT_AUTO:
            result = 0;
            break;
        case ConnectionConfig.TRANSPORT_DIRECT_TCP:
            result = 1;
            break;
        case ConnectionConfig.TRANSPORT_MDS:
            result = 2;
            break;
        case ConnectionConfig.TRANSPORT_WAP2:
            result = 3;
            break;
        case ConnectionConfig.TRANSPORT_WIFI_ONLY:
            result = 4;
            break;
        default:
            result = 0;
            break;
        }
        return result;
    }
    
    private static int getTransportSetting(int transportChoice) {
        int result;
        switch(transportChoice) {
        case 0:
            result = ConnectionConfig.TRANSPORT_AUTO;
            break;
        case 1:
            result = ConnectionConfig.TRANSPORT_DIRECT_TCP;
            break;
        case 2:
            result = ConnectionConfig.TRANSPORT_MDS;
            break;
        case 3:
            result = ConnectionConfig.TRANSPORT_WAP2;
            break;
        case 4:
            result = ConnectionConfig.TRANSPORT_WIFI_ONLY;
            break;
        default:
            result = ConnectionConfig.TRANSPORT_AUTO;
            break;
        }
        return result;
    }
    /*
    private void networkTransportChoiceFieldChanged(Field field, int context) {
        if(networkTransportChoiceField.getSelectedIndex() == 4) {
            enableWiFiCheckboxField.setChecked(true);
            enableWiFiCheckboxField.setEditable(false);
        }
        else if(!enableWiFiCheckboxField.isEditable()) {
            enableWiFiCheckboxField.setEditable(true);
        }
    }
    
    private void overrideHostnameCheckboxFieldChanged(Field field, int context) {
        if (overrideHostnameCheckboxField.getChecked()) {
            localHostnameEditField.setText(localHostname);
            localHostnameEditField.setEditable(true);
        }
        else {
            String hostname = System.getProperty("microedition.hostname");
            localHostnameEditField.setText((hostname != null) ? hostname
                    : "localhost");
            localHostnameEditField.setEditable(false);
        }
    }
    */
    private void clearCacheButtonFieldChanged(Field field, int context) {
        if(!clearingCache) {
            clearingCache = true;
            clearCacheButtonField.setEditable(false);
            Thread clearCacheThread = new Thread() {
                public void run() {
                    MailManager.getInstance().clearFolderMessageCache();
                    clearingCache = false;
                    UiApplication.getUiApplication().invokeLater(new Runnable() {
                        public void run() {
                            clearCacheButtonField.setEditable(true);
                        }
                    });
                }
            };
            clearCacheThread.start();
        }
    }

    private ObjectListField getSelectedListField() {
        if(identityFieldManager.isFocus()) {
            return identityListField;
        }
        else if(accountFieldManager.isFocus()) {
            return accountListField;
        }
        else if(outgoingFieldManager.isFocus()) {
            return outgoingListField;
        }
        else {
            return null;
        }
    }

    /* (non-Javadoc)
     * @see net.rim.device.api.ui.container.MainScreen#makeMenu(net.rim.device.api.ui.component.Menu, int)
     */
    protected void makeMenu(Menu menu, int instance) {
        ObjectListField selectedListField = getSelectedListField();
        
        if(selectedListField != null) {
            int index = selectedListField.getSelectedIndex();
            if(index != -1) {
                menu.add(selectItem);
                if(index < selectedListField.getSize() - 1) {
                    menu.add(moveDownItem);
                }
                else if(index > 0) {
                    menu.add(moveUpItem);
                }
            }
            
            if(selectedListField == identityListField) {
                menu.add(addIdentityItem);
                if(identityListField.getSize() > 0) {
                    menu.add(deleteIdentityItem);
                }
            }
            else if(selectedListField == accountListField) {
                menu.add(newAccountWizardItem);
                menu.add(addAccountItem);
                if(accountListField.getSize() > 0) {
                    menu.add(deleteAccountItem);
                }
            }
            else if(selectedListField == outgoingListField) {
                menu.add(addOutgoingItem);
                if(outgoingListField.getSize() > 0) {
                    menu.add(deleteOutgoingItem);
                }
            }
        }
        
        super.makeMenu(menu, instance);
    }

    /* (non-Javadoc)
     * @see net.rim.device.api.ui.Screen#keyChar(char, int, int)
     */
    public boolean keyChar(char key, int status, int time) {
        boolean retval = false;
        switch(key) {
        case Keypad.KEY_ENTER:
            openSelectedNode();
            retval = true;
            break;
        default:
            retval = super.keyChar(key, status, time);
        }
        return retval;
    }

    /* (non-Javadoc)
     * @see net.rim.device.api.ui.Screen#navigationClick(int, int)
     */
    protected boolean navigationClick(int status, int time) {
        return openSelectedNode();
    }
    
    /**
     * Open selected node.
     * 
     * @return true, if successful
     */
    private boolean openSelectedNode() {
        boolean result = false;

        ObjectListField selectedListField = getSelectedListField();

        if(selectedListField == identityListField) {
            int index = identityListField.getSelectedIndex();
            if(index != -1) {
                IdentityConfig identityConfig = (IdentityConfig)identityListField.get(identityListField, index);
                IdentityConfigScreen identityConfigScreen = new IdentityConfigScreen(identityConfig);
                ScreenFactory.getInstance().attachScreenTransition(identityConfigScreen, ScreenFactory.TRANSITION_SLIDE);
                UiApplication.getUiApplication().pushModalScreen(identityConfigScreen);
                if(identityConfigScreen.configSaved()) {
                    mailSettings.saveSettings();
                }
            }
            else {
                addIdentity();
            }
            result = true;
        }
        else if(selectedListField == accountListField) {
            int index = accountListField.getSelectedIndex();
            if(index != -1) {
                AccountConfig accountConfig = (AccountConfig)accountListField.get(accountListField, index);
                AccountConfigScreen accountConfigScreen = new AccountConfigScreen(accountConfig);
                ScreenFactory.getInstance().attachScreenTransition(accountConfigScreen, ScreenFactory.TRANSITION_SLIDE);
                UiApplication.getUiApplication().pushModalScreen(accountConfigScreen);
                if(accountConfigScreen.acctSaved()) {
                    mailSettings.saveSettings();
                }
            }
            else {
                addAccount();
            }
            result = true;
        }
        else if(selectedListField == outgoingListField) {
            int index = outgoingListField.getSelectedIndex();
            if(index != -1) {
                OutgoingConfig outgoingConfig = (OutgoingConfig)outgoingListField.get(outgoingListField, index);
                OutgoingConfigScreen outgoingConfigScreen = new OutgoingConfigScreen(outgoingConfig);
                ScreenFactory.getInstance().attachScreenTransition(outgoingConfigScreen, ScreenFactory.TRANSITION_SLIDE);
                UiApplication.getUiApplication().pushModalScreen(outgoingConfigScreen);
                if(outgoingConfigScreen.acctSaved()) {
                    mailSettings.saveSettings();
                }
            }
            else {
                addOutgoingServer();
            }
            result = true;
        }
        return result;
    }

    private void moveSelectedNodeUp() {
        Object cookie = null;
        Object prevCookie = null;
        ObjectListField selectedListField = getSelectedListField();
        if(selectedListField != null) {
            int index = selectedListField.getSelectedIndex();
            if(index > 0) {
                prevCookie = selectedListField.get(selectedListField, index - 1);
                cookie = selectedListField.get(selectedListField, index);
            }
        }
        if(cookie == null || prevCookie == null) { return; }

        boolean result = false;

        if(cookie instanceof IdentityConfig) {
            IdentityConfig curConfig = (IdentityConfig)cookie;
            IdentityConfig prevConfig = (IdentityConfig)prevCookie;

            int curConfigIndex = mailSettings.indexOfIdentityConfig(curConfig);
            mailSettings.removeIdentityConfig(curConfigIndex);

            int prevConfigIndex = mailSettings.indexOfIdentityConfig(prevConfig);
            mailSettings.insertIdentityConfigAt(curConfig, prevConfigIndex);
            result = true;
        }
        else if(cookie instanceof AccountConfig) {
            AccountConfig curConfig = (AccountConfig)cookie;
            AccountConfig prevConfig = (AccountConfig)prevCookie;

            int curConfigIndex = mailSettings.indexOfAccountConfig(curConfig);
            mailSettings.removeAccountConfig(curConfigIndex);

            int prevConfigIndex = mailSettings.indexOfAccountConfig(prevConfig);
            mailSettings.insertAccountConfigAt(curConfig, prevConfigIndex);
            result = true;
        }
        else if(cookie instanceof OutgoingConfig) {
            OutgoingConfig curConfig = (OutgoingConfig)cookie;
            OutgoingConfig prevConfig = (OutgoingConfig)prevCookie;

            int curConfigIndex = mailSettings.indexOfOutgoingConfig(curConfig);
            mailSettings.removeOutgoingConfig(curConfigIndex);

            int prevConfigIndex = mailSettings.indexOfOutgoingConfig(prevConfig);
            mailSettings.insertOutgoingConfigAt(curConfig, prevConfigIndex);
            result = true;
        }

        if(result) {
            mailSettings.saveSettings();
        }
    }

    private void moveSelectedNodeDown() {
        Object cookie = null;
        Object nextCookie = null;
        ObjectListField selectedListField = getSelectedListField();
        if(selectedListField != null) {
            int index = selectedListField.getSelectedIndex();
            if(index < selectedListField.getSize() - 1) {
                cookie = selectedListField.get(selectedListField, index);
                nextCookie = selectedListField.get(selectedListField, index + 1);
            }
        }
        if(cookie == null || nextCookie == null) { return; }

        boolean result = false;

        if(cookie instanceof IdentityConfig) {
            IdentityConfig curConfig = (IdentityConfig)cookie;
            IdentityConfig nextConfig = (IdentityConfig)nextCookie;

            int curConfigIndex = mailSettings.indexOfIdentityConfig(curConfig);
            mailSettings.removeIdentityConfig(curConfigIndex);

            int nextConfigIndex = mailSettings.indexOfIdentityConfig(nextConfig);
            mailSettings.insertIdentityConfigAt(curConfig, nextConfigIndex + 1);
            result = true;
        }
        else if(cookie instanceof AccountConfig) {
            AccountConfig curConfig = (AccountConfig)cookie;
            AccountConfig nextConfig = (AccountConfig)nextCookie;

            int curConfigIndex = mailSettings.indexOfAccountConfig(curConfig);
            mailSettings.removeAccountConfig(curConfigIndex);

            int nextConfigIndex = mailSettings.indexOfAccountConfig(nextConfig);
            mailSettings.insertAccountConfigAt(curConfig, nextConfigIndex + 1);
            result = true;
        }
        else if(cookie instanceof OutgoingConfig) {
            OutgoingConfig curConfig = (OutgoingConfig)cookie;
            OutgoingConfig nextConfig = (OutgoingConfig)nextCookie;

            int curConfigIndex = mailSettings.indexOfOutgoingConfig(curConfig);
            mailSettings.removeOutgoingConfig(curConfigIndex);

            int nextConfigIndex = mailSettings.indexOfOutgoingConfig(nextConfig);
            mailSettings.insertOutgoingConfigAt(curConfig, nextConfigIndex + 1);
            result = true;
        }

        if(result) {
            mailSettings.saveSettings();
        }
    }

    /**
     * Builds the accounts list.
     */
    private void buildAccountsList() {
        // Identities
        int numIdentities = mailSettings.getNumIdentities();
        while(identityListField.getSize() != 0) {
            identityListField.delete(0);
        }
        
        IdentityConfig identityConfig;
        for(int i = 0; i < numIdentities; i++) {
            identityConfig = mailSettings.getIdentityConfig(i);
            identityListField.insert(i, identityConfig);
        }
        
        // Accounts
        int numAccounts = mailSettings.getNumAccounts();
        while(accountListField.getSize() != 0) {
            accountListField.delete(0);
        }
        
        AccountConfig acctConfig;
        for(int i = 0; i < numAccounts; i++) {
            acctConfig = mailSettings.getAccountConfig(i);
            accountListField.insert(i, acctConfig);
        }
        
        // Outgoing servers
        int numOutgoing = mailSettings.getNumOutgoing();
        while(outgoingListField.getSize() != 0) {
            outgoingListField.delete(0);
        }

        OutgoingConfig outgoingConfig;
        for(int i = 0; i < numOutgoing; i++) {
            outgoingConfig = mailSettings.getOutgoingConfig(i);
            outgoingListField.insert(i, outgoingConfig);
        }
    }

    /**
     * Starts the new account wizard.
     */
    private void newAccountWizard() {
        AccountConfigWizard wizard = new AccountConfigWizard();
        wizard.start();
    }

    /**
     * Adds a new identity.
     */
    private void addIdentity() {
        IdentityConfig identityConfig = new IdentityConfig();
        IdentityConfigScreen identityConfigScreen = new IdentityConfigScreen(identityConfig);
        UiApplication.getUiApplication().pushModalScreen(identityConfigScreen);
        if(identityConfigScreen.configSaved()) {
            mailSettings.addIdentityConfig(identityConfig);
            mailSettings.saveSettings();
        }
    }

    /**
     * Delete the currently selected identity.
     */
    private void deleteSelectedIdentity() {
        // Assume that all the focus checking has been done prior to
        // this method being called.
        int index = identityListField.getSelectedIndex();
        
        int response = Dialog.ask(Dialog.D_DELETE);
        if(response == Dialog.DELETE) {
            mailSettings.removeIdentityConfig(index);
            identityListField.delete(index);
            mailSettings.saveSettings();
        }
    }

    /**
     * Adds a new account.
     */
    private void addAccount() {
        int response = Dialog.ask(LogicMailResource.CONFIG_WHAT_ACCOUNT_TYPE, new String[] { "IMAP", "POP" }, 0);
        if(response != Dialog.CANCEL) {
            AccountConfig acctConfig;
            if(response == 0) {
                acctConfig = new ImapConfig();
            }
            else {
                acctConfig = new PopConfig();
            }
            AccountConfigScreen accountConfigScreen = new AccountConfigScreen(acctConfig);
            UiApplication.getUiApplication().pushModalScreen(accountConfigScreen);
            if(accountConfigScreen.acctSaved()) {
                mailSettings.addAccountConfig(acctConfig);
                mailSettings.saveSettings();
            }
        }
    }

    /**
     * Delete the currently selected account.
     */
    private void deleteSelectedAccount() {
        // Assume that all the focus checking has been done prior to
        // this method being called.
        int index = accountListField.getSelectedIndex();
        
        int response = Dialog.ask(Dialog.D_DELETE);
        if(response == Dialog.DELETE) {
            mailSettings.removeAccountConfig(index);
            accountListField.delete(index);
            mailSettings.saveSettings();
        }
    }

    /**
     * Adds a new outgoing server.
     */
    private void addOutgoingServer() {
        OutgoingConfig outgoingConfig = new OutgoingConfig();
        OutgoingConfigScreen outgoingConfigScreen = new OutgoingConfigScreen(outgoingConfig);
        UiApplication.getUiApplication().pushModalScreen(outgoingConfigScreen);
        if(outgoingConfigScreen.acctSaved()) {
            mailSettings.addOutgoingConfig(outgoingConfig);
            mailSettings.saveSettings();
        }
    }

    /**
     * Delete the currently selected outgoing server.
     */
    private void deleteSelectedOutgoingServer() {
        // Assume that all the focus checking has been done prior to
        // this method being called.
        int index = outgoingListField.getSelectedIndex();
        
        int response = Dialog.ask(Dialog.D_DELETE);
        if(response == Dialog.DELETE) {
            mailSettings.removeOutgoingConfig(index);
            outgoingListField.delete(index);
            mailSettings.saveSettings();
        }
    }

    public void save() {
        GlobalConfig config = mailSettings.getGlobalConfig();

        String languageCode = languageCodes[languageChoiceField.getSelectedIndex()];
        if(languageCode != null && languageCode.length() != 0) {
            try {
                Locale.setDefault(Locale.get(languageCode));
                config.setLanguageCode(languageCode);
            } catch (Exception e) { }
        }
        else {
            Locale.setDefault(Locale.getDefault());
            config.setLanguageCode("");
        }

        //config.setUnicodeNormalization(unicodeNormalizationCheckboxField.getChecked());

        config.setMessageDisplayFormat(messageDisplayChoiceField.getSelectedIndex());

        if (displayOrderChoiceField.getSelectedIndex() == 0) {
            config.setDispOrder(true);
        } else {
            config.setDispOrder(false);
        }

        config.setHideDeletedMsg(hideDeletedMessagesCheckboxField.getChecked());

        config.setPromptOnDelete(promptOnDeleteCheckboxField.getChecked());
        config.setExpungeMode(expungeModeChoiceField.getSelectedIndex());
        
        //config.setTransportType(getTransportSetting(networkTransportChoiceField.getSelectedIndex()));
        
        //config.setEnableWiFi(enableWiFiCheckboxField.getChecked());
        //config.setConnectionHandoverEnabled(enableHandoverCheckboxField.getChecked());

        config.setAutoStartupEnabled(autoStartupCheckboxField.getChecked());
        
        if(hasIndicators) {
            config.setNotificationIconShown(notificationIconCheckboxField.getChecked());
        }
        
        int fsRootIndex = localDataLocationChoiceLabel.getSelectedIndex();
        if(fsRootIndex >= fileSystemRoots.length) {
            config.setFilesystemRoot(GlobalConfig.FILESYSTEM_DISABLED);
        }
        else {
            config.setFilesystemRoot(fileSystemRoots[fsRootIndex]);
        }

        //if (overrideHostnameCheckboxField.getChecked()) {
         //   config.setLocalHostname(localHostnameEditField.getText().trim());
        //} else {
        //    config.setLocalHostname("");
        //}

        //config.setConnDebug(connectionDebuggingCheckboxField.getChecked());
        
        mailSettings.saveSettings();
    }

}
