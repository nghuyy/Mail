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

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import net.rim.device.api.system.Bitmap;
import net.rim.device.api.ui.FocusChangeListener;
import net.rim.device.api.ui.Font;
import net.rim.device.api.ui.Graphics;
import net.rim.device.api.ui.Keypad;
import net.rim.device.api.ui.Manager;
import net.rim.device.api.ui.MenuItem;
import net.rim.device.api.ui.Screen;
import net.rim.device.api.ui.UiApplication;
import net.rim.device.api.ui.component.Menu;
import net.rim.device.api.ui.component.TreeField;
import net.rim.device.api.ui.component.TreeFieldCallback;
import net.rim.device.api.ui.Field;

import org.logicprobe.LogicMail.LogicMailResource;
import org.logicprobe.LogicMail.model.AccountNode;
import org.logicprobe.LogicMail.model.AccountNodeEvent;
import org.logicprobe.LogicMail.model.AccountNodeListener;
import org.logicprobe.LogicMail.model.MailManager;
import org.logicprobe.LogicMail.model.MailManagerEvent;
import org.logicprobe.LogicMail.model.MailManagerListener;
import org.logicprobe.LogicMail.model.MailRootNode;
import org.logicprobe.LogicMail.model.MailboxNode;
import org.logicprobe.LogicMail.model.MailboxNodeEvent;
import org.logicprobe.LogicMail.model.MailboxNodeListener;
import org.logicprobe.LogicMail.model.Node;

/**
 * Main screen for the application, providing a unified
 * view of accounts and folders.
 */
public class MailHomeScreen extends AbstractScreenProvider {
	private static final int SHORTCUT_COMPOSE = 0;
	private static final int SHORTCUT_FOLDER = 1;
	private static final int SHORTCUT_UP = 3;
	private static final int SHORTCUT_DOWN = 4;
	
	private Screen screen;
	private MailRootNode mailRootNode;
	private MailManager mailManager;
	private Hashtable accountTreeNodeMap;
	private Hashtable mailboxTreeNodeMap;
	private boolean firstVisible;
	private TreeNode mailTreeRootNode;
	private MailManagerListener mailManagerListener;
	private AccountNodeListener accountNodeListener;
	private MailboxNodeListener mailboxNodeListener;
	
	private TreeField treeField;

	private MenuItem selectFolderItem;
	private MenuItem refreshStatusItem;
	private MenuItem refreshFoldersItem;
	private MenuItem compositionItem;
	private MenuItem disconnectItem;
	
	private Hashtable nodeIdMap;

	public MailHomeScreen(MailRootNode mailRootNode) {
		this.mailRootNode = mailRootNode;
		this.nodeIdMap = new Hashtable();
		this.mailManager = MailManager.getInstance();
		this.accountTreeNodeMap = new Hashtable();
		this.mailboxTreeNodeMap = new Hashtable();
		
		this.mailManagerListener = new MailManagerListener() {
			public void mailConfigurationChanged(MailManagerEvent e) {
				mailManager_MailConfigurationChanged(e);
			}
		};
		
		this.accountNodeListener = new AccountNodeListener() {
			public void accountStatusChanged(AccountNodeEvent e) {
				accountNodeListener_AccountStatusChanged(e);
			}
		};

		this.mailboxNodeListener = new MailboxNodeListener() {
			public void mailboxStatusChanged(MailboxNodeEvent e) {
				mailboxNodeListener_MailboxStatusChanged(e);
			}
		};
	}

	/* (non-Javadoc)
	 * @see org.logicprobe.LogicMail.ui.AbstractScreenProvider#getTitle()
	 */
	public String getTitle() {
		return resources.getString(LogicMailResource.MAILHOME_TITLE);
	}
	
	/* (non-Javadoc)
	 * @see org.logicprobe.LogicMail.ui.AbstractScreenProvider#hasShortcuts()
	 */
	public boolean hasShortcuts() {
		return true;
	}
	
	/* (non-Javadoc)
	 * @see org.logicprobe.LogicMail.ui.AbstractScreenProvider#getShortcuts()
	 */
	public ShortcutItem[] getShortcuts() {
		// Note: This method is only called once, during initialization of the screen,
		// and only on devices that have touchscreen support.  The strings for the
		// shortcuts are contained within the main application library's resources.
		// However, the icons are contained within the platform support library
		// containing actual touchscreen API support.
		return new ShortcutItem[] {
			new ShortcutItem(
					SHORTCUT_COMPOSE,
					resources.getString(LogicMailResource.MENUITEM_COMPOSE_EMAIL),
					"shortcut-compose.png", "shortcut-compose-d.png"),
			new ShortcutItem(
					SHORTCUT_FOLDER,
					resources.getString(LogicMailResource.MENUITEM_TOGGLE_FOLDER),
					"shortcut-folder.png", "shortcut-folder-d.png"),
			null,
			new ShortcutItem(
					SHORTCUT_UP,
					resources.getString(LogicMailResource.MENUITEM_SCROLL_UP),
					"shortcut-up.png", "shortcut-up-d.png"),
			new ShortcutItem(
					SHORTCUT_DOWN,
					resources.getString(LogicMailResource.MENUITEM_SCROLL_DOWN),
					"shortcut-down.png", "shortcut-down-d.png")
		};
	}
	
	/* (non-Javadoc)
	 * @see org.logicprobe.LogicMail.ui.AbstractScreenProvider#initFields(net.rim.device.api.ui.Screen)
	 */
	public void initFields(Screen screen) {
		treeField = new TreeField(new TreeFieldCallback() {
			public void drawTreeItem(TreeField treeField, Graphics graphics,
					int node, int y, int width, int indent) {
				treeField_DrawTreeItem(treeField, graphics, node, y, width, indent);
			}
		}, Field.FOCUSABLE);
		treeField.setEmptyString(resources.getString(LogicMailResource.MAILHOME_NOACCOUNTS), 0);
		treeField.setDefaultExpanded(true);
		treeField.setIndentWidth(20);
		treeField.setFocusListener(new FocusChangeListener() {
			public void focusChanged(Field field, int eventType) {
				treeField_FocusChanged(field, eventType);
			}
		});
		screen.add(treeField);
		
		initMenuItems();
		this.screen = screen;
		
		refreshMailTree();
		mailManager.addMailManagerListener(mailManagerListener);
	}

	private void initMenuItems() {
	    selectFolderItem = new MenuItem(resources, LogicMailResource.MENUITEM_SELECT, 100, 8) {
			public void run() {
				int nodeId = treeField.getCurrentNode();
				if(nodeId != -1) {
					selectFolderItemHandler((TreeNode)treeField.getCookie(nodeId));
				}
			}
		};
		refreshStatusItem = new MenuItem(resources, LogicMailResource.MENUITEM_REFRESH_STATUS, 110, 10) {
			public void run() {
				int nodeId = treeField.getCurrentNode();
				if(nodeId != -1) {
					refreshStatusItemHandler((TreeNode)treeField.getCookie(nodeId));
				}
			}
		};
		refreshFoldersItem = new MenuItem(resources, LogicMailResource.MENUITEM_REFRESH_FOLDERS, 111, 10) {
			public void run() {
				int nodeId = treeField.getCurrentNode();
				if(nodeId != -1) {
					refreshFoldersItemHandler((TreeNode)treeField.getCookie(nodeId));
				}
			}
		};
		compositionItem = new MenuItem(resources, LogicMailResource.MENUITEM_COMPOSE_EMAIL, 200000, 9) {
			public void run() {
				int nodeId = treeField.getCurrentNode();
				if(nodeId != -1) {
					compositionItemHandler((TreeNode)treeField.getCookie(nodeId));
				}
			}
		};
		disconnectItem = new MenuItem(resources, LogicMailResource.MENUITEM_DISCONNECT, 200000, 9) {
			public void run() {
				int nodeId = treeField.getCurrentNode();
				if(nodeId != -1) {
					disconnectItemHandler((TreeNode)treeField.getCookie(nodeId));
				}
			}
		};
	}
	
	private void mailManager_MailConfigurationChanged(MailManagerEvent e) {
		refreshMailTree();
	}

	private void accountNodeListener_AccountStatusChanged(AccountNodeEvent e) {
		if(e.getType() == AccountNodeEvent.TYPE_CONNECTION) {
			TreeNode node = (TreeNode)accountTreeNodeMap.get(e.getSource());
			if(node != null) {
				refreshMailTreeNode(node);
			}
		}
		else if(e.getType() == AccountNodeEvent.TYPE_MAILBOX_TREE) {
			refreshAccountFolders((AccountNode)e.getSource());
		}
	}

	private void mailboxNodeListener_MailboxStatusChanged(MailboxNodeEvent e) {
		TreeNode mailboxTreeNode = (TreeNode)mailboxTreeNodeMap.get(e.getSource());
		if(mailboxTreeNode != null) {
			refreshMailTreeNode(mailboxTreeNode);
		}
	}

	private void refreshMailTree() {
		clearMailTreeSubscriptions();
		generateMailTree();
		populateMailTree(mailTreeRootNode);
	}
	
	private void generateMailTree() {
		mailTreeRootNode = new TreeNode(null, 0);
		
		AccountNode[] accounts = mailRootNode.getAccounts();
		mailTreeRootNode.children = new TreeNode[accounts.length];
		for(int i=0; i<accounts.length; i++) {
			TreeNode accountTreeNode = new TreeNode(accounts[i], TreeNode.TYPE_ACCOUNT);
			MailboxNode rootMailbox = accounts[i].getRootMailbox();
			if(rootMailbox != null) {
				MailboxNode[] mailboxNodes = rootMailbox.getMailboxes();
				accountTreeNode.children = new TreeNode[mailboxNodes.length];
				for(int j=0; j < mailboxNodes.length; j++) {
					accountTreeNode.children[j] = populateMailboxTreeNode(mailboxNodes[j]);
				}
			}
			accounts[i].addAccountNodeListener(accountNodeListener);
			accountTreeNodeMap.put(accounts[i], accountTreeNode);
			mailTreeRootNode.children[i] = accountTreeNode;
		}
	}

	private TreeNode populateMailboxTreeNode(MailboxNode mailboxNode) {
		TreeNode mailboxTreeNode = new TreeNode(mailboxNode, TreeNode.TYPE_MAILBOX);

		mailboxNode.addMailboxNodeListener(mailboxNodeListener);
		mailboxTreeNodeMap.put(mailboxNode, mailboxTreeNode);
		
		MailboxNode[] mailboxes = mailboxNode.getMailboxes();
		mailboxTreeNode.children = new TreeNode[mailboxes.length];
		for(int i=0; i < mailboxes.length; i++) {
			mailboxTreeNode.children[i] = populateMailboxTreeNode(mailboxes[i]);
		}
		
		return mailboxTreeNode;
	}

	private void clearMailTreeSubscriptions() {
		for (Enumeration e = accountTreeNodeMap.keys(); e.hasMoreElements() ;) {
			AccountNode node = (AccountNode)e.nextElement();
			node.removeAccountNodeListener(accountNodeListener);
		}
		accountTreeNodeMap.clear();
		
		for (Enumeration e = mailboxTreeNodeMap.keys(); e.hasMoreElements() ;) {
			MailboxNode node = (MailboxNode)e.nextElement();
			node.removeMailboxNodeListener(mailboxNodeListener);
		}
		mailboxTreeNodeMap.clear();
	}

	private void refreshAccountFolders(AccountNode accountNode) {
		// Unsubscribe from all existing mailbox nodes, remove them
		// from the node-id map, and remove them from the tree.
		Vector mailboxNodeList = new Vector();
		TreeNode accountTreeNode = (TreeNode)accountTreeNodeMap.get(accountNode);
		getMailboxNodes(mailboxNodeList, accountTreeNode);
		int size = mailboxNodeList.size();
		for(int i=0; i<size; i++) {
			MailboxNode mailboxNode = (MailboxNode)mailboxNodeList.elementAt(i);
			if(mailboxTreeNodeMap.containsKey(mailboxNode)) {
				mailboxNode.removeMailboxNodeListener(mailboxNodeListener);
				mailboxTreeNodeMap.remove(mailboxNode);
			}
		}

		// Now get the new mailbox list for the account
		// and repopulate the tree.
		MailboxNode rootMailbox = accountNode.getRootMailbox();
		if(rootMailbox != null) {
			MailboxNode[] mailboxNodes = rootMailbox.getMailboxes();
			accountTreeNode.children = new TreeNode[mailboxNodes.length];
			for(int i=0; i < mailboxNodes.length; i++) {
				accountTreeNode.children[i] = populateMailboxTreeNode(mailboxNodes[i]);
			}
		}
		populateMailTree(mailTreeRootNode);
	}
	
	private void selectFolderItemHandler(TreeNode treeNode) {
		if(treeNode.node instanceof MailboxNode) {
			MailboxNode mailboxNode = (MailboxNode)treeNode.node;
			navigationController.displayMailbox(mailboxNode);
		}
	}

	private void refreshStatusItemHandler(TreeNode treeNode) {
		AccountNode accountNode = getAccountForTreeNode(treeNode);
		
		if(accountNode != null) {
			accountNode.refreshMailboxStatus();
		}
	}

	private void refreshFoldersItemHandler(TreeNode treeNode) {
		AccountNode accountNode = getAccountForTreeNode(treeNode);
		
		if(accountNode != null) {
			accountNode.refreshMailboxes();
		}
	}

	private void disconnectItemHandler(TreeNode treeNode) {
		AccountNode accountNode = getAccountForTreeNode(treeNode);

		if(accountNode != null) {
			accountNode.requestDisconnect(false);
		}
	}

	private void compositionItemHandler(TreeNode treeNode) {
		AccountNode accountNode = getAccountForTreeNode(treeNode);

		if(accountNode != null && accountNode.getAccountConfig() != null) {
			navigationController.displayComposition(accountNode);
		}
	}
	
	public void populateMailTree(TreeNode rootNode) {
		synchronized(UiApplication.getEventLock()) {
			// Clear any existing nodes
			treeField.deleteAll();
			nodeIdMap.clear();
	
			// Freshly populate the tree
			int firstNode = -1;
			TreeNode[] nodes = rootNode.children;
			if(nodes != null) {
				for(int i = nodes.length - 1; i >= 0; --i) {
					int id = treeField.addChildNode(0, nodes[i]);
					nodeIdMap.put(nodes[i], new Integer(id));
					if(i == 0) { firstNode = id; }
					populateMailTreeChildren(id, nodes[i]);
				}
			}
			if(firstNode != -1) {
				treeField.setCurrentNode(firstNode);
			}
		}
	}

	private void populateMailTreeChildren(int parent, TreeNode node) {
		if(node.children != null) {
			for(int i = node.children.length - 1; i >= 0; --i) {
				int id = treeField.addChildNode(parent, node.children[i]);
				nodeIdMap.put(node.children[i], new Integer(id));
				populateMailTreeChildren(id, node.children[i]);
			}
		}
	}
	
	public void refreshMailTreeNode(TreeNode node) {
		Integer nodeInt = (Integer)nodeIdMap.get(node);
		if(nodeInt != null) {
			synchronized(UiApplication.getEventLock()) {
				treeField.invalidateNode(nodeInt.intValue());
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see org.logicprobe.LogicMail.ui.AbstractScreenProvider#onClose()
	 */
	public boolean onClose() {
		// Roundabout kludge for now
		((StandardScreen)screen).tryShutdownApplication();
		return false;
	}

	/* (non-Javadoc)
	 * @see org.logicprobe.LogicMail.ui.AbstractScreenProvider#onVisibilityChange(boolean)
	 */
	public void onVisibilityChange(boolean visible) {
		if(visible && firstVisible) {
			firstVisible = false;
			// Check to see if there are no configured accounts
			if(mailRootNode.getAccounts().length <= 1) {
				navigationController.displayAccountConfigurationWizard();
			}
		}
	}
	
    /* (non-Javadoc)
     * @see org.logicprobe.LogicMail.ui.AbstractScreenProvider#makeMenu(net.rim.device.api.ui.component.Menu, int)
     */
    public void makeMenu(Menu menu, int instance) {
    	TreeNode treeNode = (TreeNode)treeField.getCookie(treeField.getCurrentNode());
		if(treeNode.node instanceof MailboxNode) {
			menu.add(selectFolderItem);
			if(((MailboxNode)treeNode.node).getParentAccount().hasMailSender()) {
				menu.add(compositionItem);
			}
		}
		else if(treeNode.node instanceof AccountNode) {
			AccountNode accountNode = (AccountNode)treeNode.node; 
			if(accountNode.getRootMailbox() != null) {
				menu.add(refreshStatusItem);
			}
			if(accountNode.hasFolders()) {
				menu.add(refreshFoldersItem);
			}
			if(accountNode.hasMailSender()) {
				menu.add(compositionItem);			}
			if(accountNode.getStatus() == AccountNode.STATUS_ONLINE) {
				menu.add(disconnectItem);			}
		}
    }

    /* (non-Javadoc)
     * @see net.rim.device.api.ui.Screen#navigationClick(int, int)
     */
    public boolean navigationClick(int status, int time) {
    	selectFolderItem.run();
    	return true;
    }
    
    /* (non-Javadoc)
     * @see org.logicprobe.LogicMail.ui.AbstractScreenProvider#keyChar(char, int, int)
     */
    public boolean keyChar(char key, int status, int time) {
		boolean retval = false;
		switch (key) {
		case Keypad.KEY_ENTER:
			selectFolderItem.run();
			retval = true;
			break;
		case Keypad.KEY_SPACE:
			toggleSelectedNode();
			retval = true;
			break;
		}
		return retval;
	}
    
    /* (non-Javadoc)
     * @see org.logicprobe.LogicMail.ui.AbstractScreenProvider#shortcutAction(org.logicprobe.LogicMail.ui.ScreenProvider.ShortcutItem)
     */
    public void shortcutAction(ShortcutItem item) {
    	switch(item.getId()) {
    	case SHORTCUT_COMPOSE:
    		compositionItem.run();
    		break;
    	case SHORTCUT_FOLDER:
    		toggleSelectedNode();
    		break;
    	case SHORTCUT_UP:
    		screen.scroll(Manager.UPWARD);
    		break;
    	case SHORTCUT_DOWN:
    		screen.scroll(Manager.DOWNWARD);
    		break;
    	}
    }
    
    /**
     * Toggles the expansion state of the currently selected node.
     */
    private void toggleSelectedNode() {
        int curNode = treeField.getCurrentNode();
        
        // Make sure a node is selected
        if(curNode == -1) {
            return;
        }
        
        // Make sure the selected node has children
        if(treeField.getFirstChild(curNode) == -1) {
            return;
        }

        // Toggle the expansion state of the current node
        treeField.setExpanded(curNode, !treeField.getExpanded(curNode));
    }
	
	private void treeField_FocusChanged(Field field, int eventType) {
    	boolean enableCompose;
    	boolean enableFolder;
    	
    	int curNode = treeField.getCurrentNode();
        if(curNode == -1) {
        	enableFolder = false;
        	enableCompose = false;
        }
        else {
        	// Check whether we can enable folder expansion
        	if(treeField.getFirstChild(curNode) == -1) {
        		enableFolder = false;
        	}
        	else {
        		enableFolder = true;
        	}
        	
        	// Check whether we can enable composition
        	TreeNode treeNode = (TreeNode)treeField.getCookie(curNode);
        	if(treeNode.node instanceof AccountNode) {
        		if(((AccountNode)treeNode.node).hasMailSender()) {
        			enableCompose = true;
        		}
        		else {
        			enableCompose = false;
        		}
        	}
        	else if(treeNode.node instanceof MailboxNode) {
        		if(((MailboxNode)treeNode.node).getParentAccount().hasMailSender()) {
        			enableCompose = true;
        		}
        		else {
        			enableCompose = false;
        		}
        	}
        	else {
        		enableCompose = false;
        	}
        }
        
        ((StandardScreen)screen).setShortcutEnabled(SHORTCUT_COMPOSE, enableCompose);
        ((StandardScreen)screen).setShortcutEnabled(SHORTCUT_FOLDER, enableFolder);
	}
    
	private void treeField_DrawTreeItem(
			TreeField treeField,
			Graphics graphics,
			int node, int y, int width, int indent) {
		TreeNode treeNode = (TreeNode)treeField.getCookie(node);
		int rowHeight = treeField.getRowHeight();
		int fontHeight = graphics.getFont().getHeight();
		
		Bitmap icon = NodeIcons.getIcon(treeNode.node);

		graphics.drawBitmap(
				indent + (rowHeight/2 - icon.getWidth()/2),
				y + (fontHeight/2 - icon.getWidth()/2),
				icon.getWidth(),
				icon.getHeight(),
				icon, 0, 0);
		
		Font origFont = graphics.getFont();
		StringBuffer buf = new StringBuffer();
		buf.append(treeNode.node.toString());
		
		if(treeNode.type == TreeNode.TYPE_ACCOUNT) {
			graphics.setFont(origFont.derive(Font.BOLD));
		}
		else if(treeNode.type == TreeNode.TYPE_MAILBOX) {
			MailboxNode mailboxNode = (MailboxNode)treeNode.node;
			
			if(!mailboxNode.isSelectable()) {
                graphics.setFont(origFont.derive(Font.ITALIC));
			}
			
			int unseenCount = mailboxNode.getUnseenMessageCount();
			if(unseenCount > 0) {
                buf.append(" (");
                buf.append(unseenCount);
                buf.append(")");
                graphics.setFont(origFont.derive(Font.BOLD));
			}
			else {
                graphics.setFont(origFont.derive(Font.PLAIN));
			}
		}
		graphics.drawText(buf.toString(), indent + rowHeight, y, Graphics.ELLIPSIS, width);
	}
	
	/** Tree node data class */
	private static class TreeNode {
		public static final int TYPE_ACCOUNT = 1;
		public static final int TYPE_MAILBOX = 2;
		
		public Node node;
		public int type;
		public TreeNode[] children;
		
		public TreeNode(Node node, int type) {
			this.node = node;
			this.type = type;
		}
	}
	
	/**
	 * Recursively traverse a tree of TreeNodes and build a
	 * linear vector of all the MailboxNodes contained within.
	 * 
	 * @param result Result vector to add to.
	 * @param nodeId Starting node.
	 */
	private static void getMailboxNodes(Vector result, TreeNode treeNode) {
		if(treeNode.node instanceof MailboxNode) {
			result.addElement(treeNode.node);
		}
		if(treeNode.children != null) {
			for(int i=0; i<treeNode.children.length; i++) {
				getMailboxNodes(result, treeNode.children[i]);
			}
		}
	}

	private static AccountNode getAccountForTreeNode(TreeNode treeNode) {
		AccountNode accountNode;
		if(treeNode.node instanceof AccountNode) {
			accountNode = (AccountNode)treeNode.node;
		}
		else if(treeNode.node instanceof MailboxNode) {
			MailboxNode mailboxNode = (MailboxNode)treeNode.node;
			accountNode = mailboxNode.getParentAccount();
		}
		else {
			accountNode = null;
		}
		return accountNode;
	}
}
