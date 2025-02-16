package org.logicprobe.LogicMail.ui;

import net.rim.device.api.ui.Color;
import net.rim.device.api.ui.Field;
import net.rim.device.api.ui.FieldChangeListener;
import net.rim.device.api.ui.Font;
import net.rim.device.api.ui.Graphics;
import net.rim.device.api.ui.component.BasicEditField;
import net.rim.device.api.ui.component.CheckboxField;
import net.rim.device.api.ui.component.Dialog;
import net.rim.device.api.ui.component.ObjectChoiceField;
import net.rim.device.api.ui.component.PasswordEditField;
import net.rim.device.api.ui.component.SeparatorField;
import net.rim.device.api.ui.text.TextFilter;

import org.logicprobe.LogicMail.AnalyticsDataCollector;
import org.logicprobe.LogicMail.LogicMailResource;
import org.logicprobe.LogicMail.conf.ConnectionConfig;
import org.logicprobe.LogicMail.conf.OutgoingConfig;

public class OutgoingConfigScreen extends AbstractConfigScreen {
    private BorderedFieldManager headerFieldManager;
    private BorderedFieldManager contentFieldManager;
    
    
    private BasicEditField acctNameField;
    private BasicEditField serverNameField;
    private ObjectChoiceField serverSecurityField;
    private BasicEditField serverPortField;
    private ObjectChoiceField useAuthField;
    private BasicEditField serverUserField;
    private PasswordEditField serverPassField;
    private ObjectChoiceField networkTransportChoiceField;
    private CheckboxField enableWiFiCheckboxField;
    
    private boolean acctSaved;
    private OutgoingConfig outgoingConfig;

    /**
     * Creates a new instance of OutgoingConfigScreen
     */
    public OutgoingConfigScreen(OutgoingConfig outgoingConfig) {
        super(LogicMailResource.APPNAME + " - " + LogicMailResource.CONFIG_OUTGOING_TITLE);
        
        this.outgoingConfig = outgoingConfig;
        acctSaved = false;
        
        initFields();

        if(outgoingConfig.getUseAuth() == 0) {
            serverUserField.setEditable(false);
            serverPassField.setEditable(false);
            serverUserField.setText("");
            serverPassField.setText("");
        }
    }

    private void initFields() {
        Font boldFont = getFont().derive(Font.BOLD);
        
        acctNameField = new BasicEditField(LogicMailResource.CONFIG_ACCOUNT_NAME + ' ',
                outgoingConfig.getAcctName());
        acctNameField.setFont(boldFont);
        
        serverNameField = new HostnameEditField(
                LogicMailResource.CONFIG_ACCOUNT_SERVER + ' ',
                outgoingConfig.getServerName());
        
        serverSecurityField = new ObjectChoiceField(
        		LogicMailResource.CONFIG_ACCOUNT_SECURITY,
        		new Object[] {
        			LogicMailResource.CONFIG_ACCOUNT_SECURITY_NEVER,
        			LogicMailResource.CONFIG_ACCOUNT_SECURITY_TLS_IF_AVAILABLE,
        			LogicMailResource.CONFIG_ACCOUNT_SECURITY_TLS,
        			LogicMailResource.CONFIG_ACCOUNT_SECURITY_SSL},
        			outgoingConfig.getServerSecurity());
        serverSecurityField.setChangeListener(new FieldChangeListener() {
            public void fieldChanged(Field field, int context) {
            	serverSecurityField_FieldChanged(field, context);
            }});
        
        serverPortField = new BasicEditField(LogicMailResource.CONFIG_ACCOUNT_PORT + ' ',
                Integer.toString(outgoingConfig.getServerPort()));
        serverPortField.setFilter(TextFilter.get(TextFilter.NUMERIC));
        
        String authTypes[] = {
        		LogicMailResource.MENUITEM_NONE,
        		"PLAIN", "LOGIN", "CRAM-MD5"/*, "DIGEST-MD5"*/ };
        useAuthField = new ObjectChoiceField(LogicMailResource.CONFIG_OUTGOING_AUTHENTICATION + ' ',
                authTypes,
                outgoingConfig.getUseAuth());
        useAuthField.setChangeListener(new FieldChangeListener(){
            public void fieldChanged(Field field, int context) {
                useAuthField_FieldChanged(field, context);
            }});

        serverUserField = new BasicEditField(
                LogicMailResource.CONFIG_ACCOUNT_USERNAME + ' ',
                outgoingConfig.getServerUser()) {
            protected void paint(Graphics graphics) {
                if(!isEditable()) {
                    int originalColor = graphics.getColor();
                    graphics.setColor(Color.GRAY);
                    super.paint(graphics);
                    graphics.setColor(originalColor);
                }
                else {
                    super.paint(graphics);
                }
            }

            public void setEditable(boolean editable) {
                super.setEditable(editable);
                invalidate();
            }

            public boolean isFocusable() {
                return isEditable();
            }
        };
        
        serverPassField = new PasswordEditField(
                LogicMailResource.CONFIG_ACCOUNT_PASSWORD + ' ',
                outgoingConfig.getServerPass()) {
            protected void paint(Graphics graphics) {
                if(!isEditable()) {
                    int originalColor = graphics.getColor();
                    graphics.setColor(Color.GRAY);
                    super.paint(graphics);
                    graphics.setColor(originalColor);
                }
                else {
                    super.paint(graphics);
                }
            }

            public void setEditable(boolean editable) {
                super.setEditable(editable);
                invalidate();
            }

            public boolean isFocusable() {
                return isEditable();
            }
        };
        
        String[] transportChoices = {
                LogicMailResource.CONFIG_ACCOUNT_TRANSPORT_USE_GLOBAL,
                LogicMailResource.CONFIG_GLOBAL_TRANSPORT_AUTO,
                LogicMailResource.CONFIG_GLOBAL_TRANSPORT_DIRECT_TCP,
                LogicMailResource.CONFIG_GLOBAL_TRANSPORT_MDS,
                LogicMailResource.CONFIG_GLOBAL_TRANSPORT_WAP2,
                LogicMailResource.CONFIG_GLOBAL_TRANSPORT_WIFI_ONLY
        };
        networkTransportChoiceField = new ObjectChoiceField(
                LogicMailResource.CONFIG_GLOBAL_NETWORK_TRANSPORT,
                transportChoices,
                getTransportChoice(outgoingConfig.getTransportType()));
        networkTransportChoiceField.setChangeListener(new FieldChangeListener() {
            public void fieldChanged(Field field, int context) {
                networkTransportChoiceField_FieldChanged(field, context);
            }
        });
        enableWiFiCheckboxField = new CheckboxField(
                LogicMailResource.CONFIG_GLOBAL_ENABLE_WIFI,
                outgoingConfig.getEnableWiFi());
        if(networkTransportChoiceField.getSelectedIndex() == 0) {
            enableWiFiCheckboxField.setChecked(false);
            enableWiFiCheckboxField.setEditable(false);
        }
        
        headerFieldManager = new BorderedFieldManager(BorderedFieldManager.BOTTOM_BORDER_NONE);
        headerFieldManager.add(acctNameField);
        
        contentFieldManager = new BorderedFieldManager(BorderedFieldManager.BOTTOM_BORDER_NORMAL | Field.USE_ALL_HEIGHT);
        contentFieldManager.add(serverNameField);
        contentFieldManager.add(serverSecurityField);
        contentFieldManager.add(serverPortField);
        contentFieldManager.add(useAuthField);
        contentFieldManager.add(serverUserField);
        contentFieldManager.add(serverPassField);
        contentFieldManager.add(new SeparatorField());
        contentFieldManager.add(networkTransportChoiceField);
        contentFieldManager.add(enableWiFiCheckboxField);
        
        add(headerFieldManager);
        add(contentFieldManager);
    }

    protected void onUiEngineAttached(boolean attached) {
        super.onUiEngineAttached(attached);
        if(attached) {
            AnalyticsDataCollector.getInstance().onScreenView(getScreenPath(), getScreenName(), "Outgoing", "Configuration");
        }
    }
    
    private void serverSecurityField_FieldChanged(Field field, int context) {
        if(serverSecurityField.getSelectedIndex() == ConnectionConfig.SECURITY_SSL) {
            serverPortField.setText("465");
        }
        else {
            serverPortField.setText("25");
        }
    }
    
    private void useAuthField_FieldChanged(Field field, int context) {
        if(useAuthField.getSelectedIndex() > 0) {
            serverUserField.setEditable(true);
            serverPassField.setEditable(true);
            serverUserField.setText(outgoingConfig.getServerUser());
            serverPassField.setText(outgoingConfig.getServerPass());
        }
        else {
            serverUserField.setEditable(false);
            serverPassField.setEditable(false);
            serverUserField.setText("");
            serverPassField.setText("");
        }
    }
    
    private void networkTransportChoiceField_FieldChanged(Field field, int context) {
        if(networkTransportChoiceField.getSelectedIndex() == 0) {
            enableWiFiCheckboxField.setChecked(false);
            enableWiFiCheckboxField.setEditable(false);
        }
        else if(networkTransportChoiceField.getSelectedIndex() == 5) {
            enableWiFiCheckboxField.setChecked(true);
            enableWiFiCheckboxField.setEditable(false);
        }
        else {
            enableWiFiCheckboxField.setChecked(true);
            enableWiFiCheckboxField.setEditable(true);
        }
    }

    private static int getTransportChoice(int transportSetting) {
        int result;
        switch(transportSetting) {
        case ConnectionConfig.TRANSPORT_GLOBAL:
            result = 0;
            break;
        case ConnectionConfig.TRANSPORT_AUTO:
            result = 1;
            break;
        case ConnectionConfig.TRANSPORT_DIRECT_TCP:
            result = 2;
            break;
        case ConnectionConfig.TRANSPORT_MDS:
            result = 3;
            break;
        case ConnectionConfig.TRANSPORT_WAP2:
            result = 4;
            break;
        case ConnectionConfig.TRANSPORT_WIFI_ONLY:
            result = 5;
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
            result = ConnectionConfig.TRANSPORT_GLOBAL;
            break;
        case 1:
            result = ConnectionConfig.TRANSPORT_AUTO;
            break;
        case 2:
            result = ConnectionConfig.TRANSPORT_DIRECT_TCP;
            break;
        case 3:
            result = ConnectionConfig.TRANSPORT_MDS;
            break;
        case 4:
            result = ConnectionConfig.TRANSPORT_WAP2;
            break;
        case 5:
            result = ConnectionConfig.TRANSPORT_WIFI_ONLY;
            break;
        default:
            result = ConnectionConfig.TRANSPORT_GLOBAL;
            break;
        }
        return result;
    }

    protected boolean onSavePrompt() {
        if(acctNameField.getText().length() > 0 &&
           serverNameField.getText().length() > 0 &&
           serverPortField.getText().length() > 0) {
            return super.onSavePrompt();
        }
        else {
            int result =
            	Dialog.ask(LogicMailResource.CONFIG_PROMPT_INCOMPLETE,
            			new String[] {
            				LogicMailResource.MENUITEM_DISCARD,
            				LogicMailResource.MENUITEM_CANCEL }, 0);
            if(result == 0) {
                return true;
            }
            else {
                return false;
            }
        }
    }
    
    public void save() {
        this.outgoingConfig.setAcctName(acctNameField.getText());
        this.outgoingConfig.setServerName(serverNameField.getText());
        this.outgoingConfig.setServerSecurity(serverSecurityField.getSelectedIndex());
        this.outgoingConfig.setServerPort(Integer.parseInt(serverPortField.getText()));
        this.outgoingConfig.setServerUser(serverUserField.getText());
        this.outgoingConfig.setServerPass(serverPassField.getText());
        this.outgoingConfig.setTransportType(getTransportSetting(networkTransportChoiceField.getSelectedIndex()));
        this.outgoingConfig.setEnableWiFi(enableWiFiCheckboxField.getChecked());
        this.outgoingConfig.setUseAuth(useAuthField.getSelectedIndex());
        if(useAuthField.getSelectedIndex() > 0) {
            this.outgoingConfig.setServerUser(serverUserField.getText());
            this.outgoingConfig.setServerPass(serverPassField.getText());
        }
        acctSaved = true;
    }
    
    public boolean acctSaved() {
        return acctSaved;
    }
}
