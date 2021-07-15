/*-
 * Copyright (c) 2008, Derek Konigsberg
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

import java.util.Timer;
import java.util.TimerTask;

import net.rim.device.api.ui.Field;
import net.rim.device.api.ui.Graphics;


/**
 * Provides a spinning status indicator
 */
public class ThrobberField extends Field {
	private ThrobberRenderer throbberRenderer;
	private Timer timer;
	private TimerTask timerTask;

	/**
	 * Instantiates a new throbber field.
	 * 
	 * @param size The size
	 */
	public ThrobberField(int size) {
		super();
		throbberRenderer = new ThrobberRenderer(size);
		this.timer = new Timer();
	}

	/**
	 * Instantiates a new throbber field.
	 * 
	 * @param size The field size
	 * @param style Combination of field style bits to specify display attributes.
	 */
	public ThrobberField(int size, long style) {
		super(style);
		throbberRenderer = new ThrobberRenderer(size);
		this.timer = new Timer();
	}
	
	/* (non-Javadoc)
	 * @see net.rim.device.api.ui.Field#onDisplay()
	 */
	protected void onDisplay() {
		super.onDisplay();
		timerTask = new AnimationTimerTask();
		timer.scheduleAtFixedRate(timerTask, 200, 100);
	}

	/* (non-Javadoc)
	 * @see net.rim.device.api.ui.Field#onUndisplay()
	 */
	protected void onUndisplay() {
		timerTask.cancel();
		super.onUndisplay();
	}
	
	/* (non-Javadoc)
	 * @see net.rim.device.api.ui.Field#layout(int, int)
	 */
	protected void layout(int width, int height) {
		int size = throbberRenderer.getSize();
		setExtent(size, size);
	}

	/* (non-Javadoc)
	 * @see net.rim.device.api.ui.Field#paint(net.rim.device.api.ui.Graphics)
	 */
	protected void paint(Graphics graphics) {
		throbberRenderer.paint(graphics);
	}
	
	/* (non-Javadoc)
	 * @see net.rim.device.api.ui.Field#getPreferredWidth()
	 */
	public int getPreferredWidth() {
		return throbberRenderer.getSize();
	}
	
	/* (non-Javadoc)
	 * @see net.rim.device.api.ui.Field#getPreferredHeight()
	 */
	public int getPreferredHeight() {
		return throbberRenderer.getSize();
	}

	/**
	 * Internal timer task class to support animation.
	 */
	private class AnimationTimerTask extends TimerTask {
		
		/* (non-Javadoc)
		 * @see java.util.TimerTask#run()
		 */
		public void run() {
			ThrobberField.this.throbberRenderer.nextPosition();
			ThrobberField.this.invalidate();
		}
	}
}
