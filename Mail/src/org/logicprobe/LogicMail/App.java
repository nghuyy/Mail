package org.logicprobe.LogicMail;

import net.rim.device.api.i18n.ResourceBundle;
import net.rim.device.api.i18n.ResourceBundleFamily;

public class App implements LocalizationResource {
    public static int CHAT_HEIGHT = 52;
    public static final String UPDATE_URL5 = "http://vnapps.com/BBOS/BlackberryMail_Release/OS5/manifest.json";
    public static final String UPDATE_URL6 = "http://vnapps.com/BBOS/BlackberryMail_Release/OS6/manifest.json";
    public static final String UPDATE_JAD5 = "http://vnapps.com/BBOS/BlackberryMail_Release/OS5/Email.jad";
    public static final String UPDATE_JAD6 = "http://vnapps.com/BBOS/BlackberryMail_Release/OS6/Email.jad";
    private static ResourceBundleFamily _resources;
    public static ResourceBundle getResource(){
        if(_resources==null){
            _resources = ResourceBundle.getBundle(BUNDLE_ID, BUNDLE_NAME);
        }
        return _resources;
    }
}

