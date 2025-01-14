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

import org.logicprobe.LogicMail.PlatformInfo;

import net.rim.device.api.system.Application;
import net.rim.device.api.system.Bitmap;
import net.rim.device.api.system.DeviceInfo;
import net.rim.device.api.system.Display;
import net.rim.device.api.system.RadioInfo;
import net.rim.device.api.system.RadioStatusListener;
import net.rim.device.api.system.SystemListener;
import net.rim.device.api.ui.Color;
import net.rim.device.api.ui.DrawStyle;
import net.rim.device.api.ui.Field;
import net.rim.device.api.ui.Font;
import net.rim.device.api.ui.Graphics;
import net.rim.device.api.util.Arrays;

/**
 * General purpose header field for application screens.
 * Based on the sample code provided here:
 * http://www.northcubed.com/site/?p=15
 */
public class HeaderField extends Field {
    private Font headerFont;
    private String title;
    private boolean showSignal;
    private boolean showBattery;
    private boolean showTitle;
    private int fieldHeight;
    private int fontColor;
    private int backgroundColor;
    private int batteryBackground;
    private int signalBarColor;
    private SystemListener systemListener;
    private RadioStatusListener radioStatusListener;
    private boolean listenersActive;
    private int signalLevel;
    private int batteryLevel;
    private final boolean heightManager;
    private final Bitmap batteryFillBitmap;
    
    public HeaderField(String title) {
        super(Field.NON_FOCUSABLE);
        this.title = title;
        this.showSignal = true;
        this.showBattery = true;
        this.showTitle = true;
        this.fontColor = -1;
        this.headerFont = Font.getDefault();
        this.backgroundColor = 0;
        this.batteryBackground = 0x999999;
        this.signalBarColor = Color.BLUE;
        this.fieldHeight = headerFont.getHeight();
        signalLevel = RadioInfo.getSignalLevel();
        batteryLevel = DeviceInfo.getBatteryLevel();
        
        this.listenersActive = false;
        
        String platformVersion = PlatformInfo.getInstance().getPlatformVersion();
        heightManager = !platformVersion.startsWith("4.5");
        
        batteryFillBitmap = new Bitmap(34, 8);
        updateBatteryFillBitmap();
        
        this.systemListener = new SystemListener() {
            public void powerOff() {
            }
            public void powerUp() {
            }
            public void batteryLow() {
                onBatteryStatusChanged();
            }
            public void batteryGood() {
                onBatteryStatusChanged();
            }
            public void batteryStatusChange(int status) {
                onBatteryStatusChanged();
            }
        };
        this.radioStatusListener = new RadioStatusListener() {
            public void signalLevel(int level) {
                onRadioStatusChanged();
            }
            public void networkStarted(int networkId, int service) {
                onRadioStatusChanged();
            }
            public void baseStationChange() {
                onRadioStatusChanged();
            }
            public void radioTurnedOff() {
                onRadioStatusChanged();
            }
            public void pdpStateChange(int apn, int state, int cause) {
                onRadioStatusChanged();
            }
            public void networkStateChange(int state) {
                onRadioStatusChanged();
            }
            public void networkScanComplete(boolean success) {
                onRadioStatusChanged();
            }
            public void networkServiceChange(int networkId, int service) {
                onRadioStatusChanged();
            }
        };
    }

    protected void onBatteryStatusChanged() {
        batteryLevel = DeviceInfo.getBatteryLevel();
        updateBatteryFillBitmap();
        invalidate();
    }
    
    private void updateBatteryFillBitmap() {
        // Pick the battery color
        int batteryColor;
        if(batteryLevel > 75) { batteryColor = 0x28f300; }
        else if(batteryLevel > 50) { batteryColor = 0x91dc00; }
        else if(batteryLevel > 25) { batteryColor = 0xefec00; }
        else { batteryColor = 0xff2200; }

        // Clear the battery fill bitmap
        int dataTransparent[] = new int[34 * 8];
        Arrays.fill(dataTransparent, 0, 0, dataTransparent.length);
        batteryFillBitmap.setARGB(dataTransparent, 0, 34, 0, 0, 34, 8);
        
        // Paint the battery fill in the appropriate color
        Graphics g = new Graphics(batteryFillBitmap);
        g.setGlobalAlpha(255);
        g.setColor(batteryColor);
        g.fillRect(0, 0, 6, 8);
        g.fillRect(7, 0, 6, 8);
        g.fillRect(14, 0, 6, 8);
        g.fillRect(21, 0, 6, 8);
        g.fillRect(28, 0, 6, 8);
    }
    
    protected void onRadioStatusChanged() {
        signalLevel = RadioInfo.getSignalLevel();
        invalidate();
    }
    
    protected void onDisplay() {
        checkAddListeners();
        super.onExposed();
    }

    protected void onExposed() {
        checkAddListeners();
        super.onExposed();
    }
    
    protected void onObscured() {
        checkRemoveListeners();
        super.onObscured();
    }
    
    protected void onUndisplay() {
        checkRemoveListeners();
        super.onUndisplay();
    }

    private void checkAddListeners() {
		if(!listenersActive) {
            Application.getApplication().addSystemListener(systemListener);
            Application.getApplication().addRadioListener(radioStatusListener);
            onBatteryStatusChanged();
            onRadioStatusChanged();
            listenersActive = true;
        }
	}
    
	private void checkRemoveListeners() {
		if(listenersActive) {
            Application.getApplication().removeSystemListener(systemListener);
            Application.getApplication().removeRadioListener(radioStatusListener);
            listenersActive = false;
        }
	}
	
    /**
     * Remove any global event listeners.  Intended to be called on shutdown,
     * where the active screen may not get popped off the stack prior to
     * System.exit() being called.
     */
    public void removeListeners() {
        if(listenersActive) {
            Application.getApplication().removeSystemListener(systemListener);
            Application.getApplication().removeRadioListener(radioStatusListener);
            listenersActive = false;
        }
    }
    
    public void setTitle(String title) {
        this.title = title;
        invalidate();
    }
    
    public void setFontColor(int fontColor) {
        this.fontColor = fontColor;
        invalidate();
    }
    
    public void setBatteryBackground(int batteryBackground) {
        this.batteryBackground = batteryBackground;
        invalidate();
    }
    
    public void setBackgroundColor(int backgroundColor) {
        this.backgroundColor = backgroundColor;
        invalidate();
    }
    
    public void showSignal(boolean bool) {
        showSignal = bool;
        invalidate();
    }
    
    public void showBattery(boolean bool) {
        showBattery = bool;
        invalidate();
    }
    
    public void showTitle(boolean bool) {
        showTitle = bool;
        invalidate();
    }
    
    protected void layout(int width, int height) {
        setExtent(getPreferredWidth(), getPreferredHeight());
    }
    
    public int getPreferredWidth() {
        return Display.getWidth();
    }
    
    public int getPreferredHeight() {
        return fieldHeight;
    }
    
    protected void paint(Graphics graphics) {
        if(fontColor == -1) {
            fontColor = graphics.getColor();
        }

        graphics.setFont(headerFont);
        int width = getWidth();
        
        int height = heightManager ? getManager().getHeight() : getHeight();
        int midPoint = height >>> 1;
        int graphicsDiff = midPoint - 7;
        
        if(backgroundColor != 0) {
            graphics.setColor(backgroundColor);
            graphics.fillRect(0, 0, width, height);
        }
        
        if(showSignal) {
        	graphics.pushRegion(width - 37 - graphicsDiff, midPoint - 7, 35, 14, 0, 0);
        	drawSignalIndicator(graphics);
        	graphics.popContext();
        	
            graphicsDiff += 37;
        }
        
        if(showBattery) {
        	graphics.pushRegion(width - 48 - graphicsDiff, midPoint - 7, 44, 14, 0, 0);
        	drawBatteryIndicator(graphics);
        	graphics.popContext();
        	
        	graphicsDiff += 48;
        }
        
        graphics.setColor(fontColor);
        
        if(showTitle) {
            graphics.drawText(title, 4, 0, DrawStyle.ELLIPSIS, width - graphicsDiff - 4);
        }
    }
    
    private void drawSignalIndicator(Graphics graphics) {
    	graphics.setColor(Color.DARKGRAY);
        graphics.fillRect(7, 12, 4, 2);
        graphics.fillRect(13, 9, 4, 5);
        graphics.fillRect(19, 6, 4, 8);
        graphics.fillRect(25, 3, 4, 11);
        graphics.fillRect(31, 0, 4, 14);
    	
    	graphics.setColor(signalBarColor);
    	graphics.drawLine(0, 0, 8, 0);
    	graphics.drawLine(0, 0, 4, 4);
    	graphics.drawLine(8, 0, 4, 4);
    	graphics.drawLine(4, 4, 4, 13);
    	
        if(signalLevel >= -120) {
            //1 band
            graphics.fillRect(7, 12, 4, 2);
        }
        if(signalLevel >= -101) {
            //2 bands
            graphics.fillRect(13, 9, 4, 5);
        }
        if(signalLevel >= -92) {
            //3 bands
            graphics.fillRect(19, 6, 4, 8);
        }
        if(signalLevel >= -86) {
            //4 bands
            graphics.fillRect(25, 3, 4, 11);
        }
        if(signalLevel >= -77) {
            //5 bands
            graphics.fillRect(31, 0, 4, 14);
        }
    }

    private void drawBatteryIndicator(Graphics graphics) {
        // Paint the battery background
    	graphics.setColor(batteryBackground);
    	graphics.drawRect(1, 0, 40, 14);
    	graphics.drawRect(2, 1, 38, 12);
    	graphics.drawLine(0, 2, 0, 11);
    	graphics.fillRect(41, 3, 3, 8);

    	// Paint the battery level indicator
        int power = (int)((34.00/100) * batteryLevel);
        power = Math.max(power, 0);
        power = Math.min(power, 34);
        graphics.drawBitmap(4, 3, power, 8, batteryFillBitmap, 0, 0);
    }
}
