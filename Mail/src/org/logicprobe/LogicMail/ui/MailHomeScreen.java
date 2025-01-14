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

import net.rim.device.api.i18n.ResourceBundle;
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
import net.rim.device.api.util.ToIntHashtable;

import org.logicprobe.LogicMail.AnalyticsDataCollector;
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
import org.logicprobe.LogicMail.model.MessageNode;
import org.logicprobe.LogicMail.model.NetworkAccountNode;
import org.logicprobe.LogicMail.model.Node;
import org.logicprobe.LogicMail.model.OutboxMailboxNode;
import org.logicprobe.LogicMail.util.DataStoreFactory;
import org.logicprobe.LogicMail.util.Serializable;
import org.logicprobe.LogicMail.util.SerializableHashtable;


/**
 * Main screen for the application, providing a unified
 * view of accounts and folders.
 */
public class MailHomeScreen extends AbstractScreenProvider {
    private static final int SHORTCUT_COMPOSE = 0;
    private static final int SHORTCUT_FOLDER = 1;
    private static final int SHORTCUT_UP = 3;
    private static final int SHORTCUT_DOWN = 4;

    private MailRootNode mailRootNode;
    private MailManager mailManager;
    private Hashtable accountTreeNodeMap;
    private Hashtable mailboxTreeNodeMap;
    private boolean firstVisible;
    private MailHomeTreeNode mailTreeRootNode;
    private MailManagerListener mailManagerListener;
    private AccountNodeListener accountNodeListener;
    private MailboxNodeListener mailboxNodeListener;

    private TreeField treeField;

    private MenuItem selectFolderItem;
    private MenuItem refreshStatusItem;
    private MenuItem refreshFoldersItem;
    private MenuItem compositionItem;
    private MenuItem sendUnsentItem;
    private MenuItem disconnectItem;

    private ToIntHashtable nodeIdMap;
    private MailHomeTreeNode _focusTree;

    public MailHomeScreen(MailRootNode mailRootNode) {
        this.firstVisible = true;
        this.mailRootNode = mailRootNode;
        this.nodeIdMap = new ToIntHashtable();
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
        return LogicMailResource.MAILHOME_TITLE;
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
        return new ShortcutItem[]{
                new ShortcutItem(
                        SHORTCUT_COMPOSE,
                        LogicMailResource.MENUITEM_COMPOSE_EMAIL,
                        "shortcut-compose.png", "shortcut-compose-d.png"),
                new ShortcutItem(
                        SHORTCUT_FOLDER,
                        LogicMailResource.MENUITEM_TOGGLE_FOLDER,
                        "shortcut-folder.png", "shortcut-folder-d.png"),
                null,
                new ShortcutItem(
                        SHORTCUT_UP,
                        LogicMailResource.MENUITEM_SCROLL_UP,
                        "shortcut-up.png", "shortcut-up-d.png"),
                new ShortcutItem(
                        SHORTCUT_DOWN,
                        LogicMailResource.MENUITEM_SCROLL_DOWN,
                        "shortcut-down.png", "shortcut-down-d.png")
        };
    }

    /* (non-Javadoc)
     * @see org.logicprobe.LogicMail.ui.AbstractScreenProvider#initFields(net.rim.device.api.ui.Screen)
     */
    public void initFields(Screen screen) {
        super.initFields(screen);

        treeField = FieldFactory.getInstance().getScreenTreeField(new TreeFieldCallback() {
            public void drawTreeItem(TreeField treeField, Graphics graphics,
                                     int node, int y, int width, int indent) {
                treeField_DrawTreeItem(treeField, graphics, node, y, width, indent);
            }
        }, Field.FOCUSABLE);
        treeField.setEmptyString(LogicMailResource.MAILHOME_NOACCOUNTS, 0);
        treeField.setDefaultExpanded(true);
        treeField.setIndentWidth(20);
        treeField.setFocusListener(new FocusChangeListener() {
            public void focusChanged(Field field, int eventType) {
                treeField_FocusChanged(field, eventType);
            }
        });
        screen.add(treeField);

        initMenuItems();
        refreshMailTree();
        mailManager.addMailManagerListener(mailManagerListener);
    }

    private void initMenuItems() {
        selectFolderItem = new TreeNodeMenuItem(LogicMailResource.MENUITEM_SELECT, 300100, 1) {
            public void runNode(MailHomeTreeNode treeNode) {
                selectFolderItemHandler(treeNode);
            }
        };

        sendUnsentItem = new TreeNodeMenuItem(LogicMailResource.MENUITEM_SEND_UNSENT_MESSAGES, 300200, 1020) {
            public void runNode(MailHomeTreeNode treeNode) {
                sendUnsentItemHandler(treeNode);
            }
        };

        refreshStatusItem = new TreeNodeMenuItem(LogicMailResource.MENUITEM_REFRESH_STATUS, 300300, 2000) {
            public void runNode(MailHomeTreeNode treeNode) {
                refreshStatusItemHandler(treeNode);
            }
        };

        refreshFoldersItem = new TreeNodeMenuItem(LogicMailResource.MENUITEM_REFRESH_FOLDERS, 300400, 2000) {
            public void runNode(MailHomeTreeNode treeNode) {
                _focusTree = treeNode;
                refreshFoldersItemHandler(treeNode);
            }
        };

        compositionItem = new TreeNodeMenuItem(LogicMailResource.MENUITEM_COMPOSE_EMAIL, 400100, 2000) {
            public void runNode(MailHomeTreeNode treeNode) {
                compositionItemHandler(treeNode);
            }
        };

        disconnectItem = new TreeNodeMenuItem(LogicMailResource.MENUITEM_DISCONNECT, 700100, 8000) {
            public void runNode(MailHomeTreeNode treeNode) {
                disconnectItemHandler(treeNode);
            }
        };
    }

    public void onDisplay() {
        super.onDisplay();
        AnalyticsDataCollector.getInstance().onScreenView(getScreenPath(), getScreenName(), "MailHome", "Home");
    }

    private abstract class TreeNodeMenuItem extends MenuItem {
        public TreeNodeMenuItem(String id, int ordinal, int priority) {
            super(id, ordinal, priority);
        }

        public final void run() {
            int nodeId = treeField.getCurrentNode();
            if (nodeId != -1) {
                runNode((MailHomeTreeNode) treeField.getCookie(nodeId));
            }
        }

        public abstract void runNode(MailHomeTreeNode treeNode);
    }

    private void mailManager_MailConfigurationChanged(MailManagerEvent e) {
        refreshMailTree();
    }

    private void accountNodeListener_AccountStatusChanged(AccountNodeEvent e) {
        if (e.getType() == AccountNodeEvent.TYPE_CONNECTION) {
            MailHomeTreeNode node = (MailHomeTreeNode) accountTreeNodeMap.get(e.getSource());
            if (node != null) {
                refreshMailTreeNode(node);
            }
        } else if (e.getType() == AccountNodeEvent.TYPE_MAILBOX_TREE) {
            refreshAccountFolders((AccountNode) e.getSource());
        }
    }

    private void mailboxNodeListener_MailboxStatusChanged(MailboxNodeEvent e) {
        MailHomeTreeNode mailboxTreeNode = (MailHomeTreeNode) mailboxTreeNodeMap.get(e.getSource());
        if (mailboxTreeNode != null) {
            refreshMailTreeNode(mailboxTreeNode);
        }
    }

    private void refreshMailTree() {
        clearMailTreeSubscriptions();
        generateMailTree();
        populateMailTree(mailTreeRootNode, null);
        loadScreenMetadata();
    }

    private void generateMailTree() {
        mailTreeRootNode = new MailHomeTreeNode(null, 0);

        AccountNode[] accounts = mailRootNode.getAccounts();
        mailTreeRootNode.children = new MailHomeTreeNode[accounts.length];
        for (int i = 0; i < accounts.length; i++) {
            MailHomeTreeNode accountTreeNode = new MailHomeTreeNode(accounts[i], MailHomeTreeNode.TYPE_ACCOUNT);
            MailboxNode rootMailbox = accounts[i].getRootMailbox();
            if (rootMailbox != null) {
                MailboxNode[] mailboxNodes = rootMailbox.getMailboxes();
                accountTreeNode.children = new MailHomeTreeNode[mailboxNodes.length];
                for (int j = 0; j < mailboxNodes.length; j++) {
                    accountTreeNode.children[j] = populateMailboxTreeNode(mailboxNodes[j]);
                }
            }
            accounts[i].addAccountNodeListener(accountNodeListener);
            accountTreeNodeMap.put(accounts[i], accountTreeNode);
            mailTreeRootNode.children[i] = accountTreeNode;
        }
    }

    private MailHomeTreeNode populateMailboxTreeNode(MailboxNode mailboxNode) {
        MailHomeTreeNode mailboxTreeNode = new MailHomeTreeNode(mailboxNode, MailHomeTreeNode.TYPE_MAILBOX);

        mailboxNode.addMailboxNodeListener(mailboxNodeListener);
        mailboxTreeNodeMap.put(mailboxNode, mailboxTreeNode);

        MailboxNode[] mailboxes = mailboxNode.getMailboxes();
        mailboxTreeNode.children = new MailHomeTreeNode[mailboxes.length];
        for (int i = 0; i < mailboxes.length; i++) {
            mailboxTreeNode.children[i] = populateMailboxTreeNode(mailboxes[i]);
        }

        return mailboxTreeNode;
    }

    private void clearMailTreeSubscriptions() {
        for (Enumeration e = accountTreeNodeMap.keys(); e.hasMoreElements(); ) {
            AccountNode node = (AccountNode) e.nextElement();
            node.removeAccountNodeListener(accountNodeListener);
        }
        accountTreeNodeMap.clear();

        for (Enumeration e = mailboxTreeNodeMap.keys(); e.hasMoreElements(); ) {
            MailboxNode node = (MailboxNode) e.nextElement();
            node.removeMailboxNodeListener(mailboxNodeListener);
        }
        mailboxTreeNodeMap.clear();
    }

    private void refreshAccountFolders(AccountNode accountNode) {
        final Hashtable stateTable = new Hashtable();
        UiApplication.getUiApplication().invokeAndWait(new Runnable() {
            public void run() {
                saveScreenState(stateTable);
            }
        });

        // Unsubscribe from all existing mailbox nodes, remove them
        // from the node-id map, and remove them from the tree.
        Vector mailboxNodeList = new Vector();
        MailHomeTreeNode accountTreeNode = (MailHomeTreeNode) accountTreeNodeMap.get(accountNode);
        getMailboxNodes(mailboxNodeList, accountTreeNode);
        int size = mailboxNodeList.size();
        for (int i = 0; i < size; i++) {
            MailboxNode mailboxNode = (MailboxNode) mailboxNodeList.elementAt(i);
            if (mailboxTreeNodeMap.containsKey(mailboxNode)) {
                mailboxNode.removeMailboxNodeListener(mailboxNodeListener);
                mailboxTreeNodeMap.remove(mailboxNode);
            }
        }

        // Now get the new mailbox list for the account
        // and repopulate the tree.
        MailboxNode rootMailbox = accountNode.getRootMailbox();
        if (rootMailbox != null) {
            MailboxNode[] mailboxNodes = rootMailbox.getMailboxes();
            accountTreeNode.children = new MailHomeTreeNode[mailboxNodes.length];
            for (int i = 0; i < mailboxNodes.length; i++) {
                accountTreeNode.children[i] = populateMailboxTreeNode(mailboxNodes[i]);
            }
        }
        populateMailTree(mailTreeRootNode, stateTable);
    }

    private void selectFolderItemHandler(MailHomeTreeNode treeNode) {
        if (treeNode.isNodeSelectable()) {
            if (treeNode.node instanceof MailboxNode) {
                MailboxNode mailboxNode = (MailboxNode) treeNode.node;
                navigationController.displayMailbox(mailboxNode);
            }
        }
    }

    private void refreshStatusItemHandler(MailHomeTreeNode treeNode) {
        AccountNode accountNode = getAccountForTreeNode(treeNode);

        if (accountNode != null) {
            accountNode.refreshMailboxStatus();
        }
    }

    private void refreshFoldersItemHandler(MailHomeTreeNode treeNode) {
        AccountNode accountNode = getAccountForTreeNode(treeNode);

        if (accountNode != null) {
            accountNode.refreshMailboxes();
        }
    }

    private void disconnectItemHandler(MailHomeTreeNode treeNode) {
        AccountNode accountNode = getAccountForTreeNode(treeNode);

        if (accountNode instanceof NetworkAccountNode) {
            ((NetworkAccountNode) accountNode).requestDisconnect(false);
        }
    }

    private void compositionItemHandler(MailHomeTreeNode treeNode) {
        AccountNode accountNode = getAccountForTreeNode(treeNode);

        if (accountNode instanceof NetworkAccountNode) {
            navigationController.displayComposition((NetworkAccountNode) accountNode);
        }
    }

    private void sendUnsentItemHandler(MailHomeTreeNode treeNode) {
        MessageActions messageActions = navigationController.getMessageActions();
        if (treeNode.node instanceof OutboxMailboxNode) {
            OutboxMailboxNode outboxNode = (OutboxMailboxNode) treeNode.node;
            MessageNode[] messages = outboxNode.getMessages();
            for (int i = 0; i < messages.length; i++) {
                messageActions.sendMessage(messages[i]);
            }
        }
    }

    public void populateMailTree(final MailHomeTreeNode rootNode, final Hashtable stateTable) {
        UiApplication.getUiApplication().invokeAndWait(new Runnable() {
            public void run() {
                // Clear any existing nodes
                treeField.deleteAll();
                nodeIdMap.clear();

                // Freshly populate the tree
                int firstNode = -1;
                MailHomeTreeNode[] nodes = rootNode.children;
                if (nodes != null) {
                    for (int i = nodes.length - 1; i >= 0; --i) {
                        int id = treeField.addChildNode(0, nodes[i]);
                        nodeIdMap.put(nodes[i], id);
                        if (i == 0) {
                            firstNode = id;
                        }
                        populateMailTreeChildren(id, nodes[i]);
                    }
                }
                if (stateTable != null) {
                    restoreScreenState(stateTable);
                } else if (firstNode != -1) {
                    treeField.setCurrentNode(firstNode);
                }
            }
        });
    }

    private void populateMailTreeChildren(int parent, MailHomeTreeNode node) {
        if (node.children != null) {
            for (int i = node.children.length - 1; i >= 0; --i) {
                int id = treeField.addChildNode(parent, node.children[i]);
                nodeIdMap.put(node.children[i], id);
                populateMailTreeChildren(id, node.children[i]);
            }
        }
    }

    public void refreshMailTreeNode(MailHomeTreeNode node) {
        final int nodeId = nodeIdMap.get(node);
        if (nodeId != -1) {
            invokeLater(new Runnable() {
                public void run() {
                    treeField.invalidateNode(nodeId);
                }
            });
        }
    }

    /* (non-Javadoc)
     * @see org.logicprobe.LogicMail.ui.AbstractScreenProvider#onClose()
     */
    public boolean onClose() {
        saveScreenMetadata();
        UiApplication.getUiApplication().requestBackground();
        return false;
    }

    /* (non-Javadoc)
     * @see org.logicprobe.LogicMail.ui.AbstractScreenProvider#onVisibilityChange(boolean)
     */
    public void onVisibilityChange(boolean visible) {
        if (visible && firstVisible) {
            firstVisible = false;
            // Check to see if there are no configured accounts
            if (mailRootNode.getNetworkAccounts().length == 0) {
                navigationController.displayAccountConfigurationWizard();
            }
        }
    }

    /* (non-Javadoc)
     * @see org.logicprobe.LogicMail.ui.AbstractScreenProvider#makeMenu(net.rim.device.api.ui.component.Menu, int)
     */
    public void makeMenu(Menu menu, int instance) {
        MailHomeTreeNode treeNode = (MailHomeTreeNode) treeField.getCookie(treeField.getCurrentNode());
        if (treeNode.node instanceof MailboxNode) {
            menu.add(selectFolderItem);
            AccountNode parentAccount = ((MailboxNode) treeNode.node).getParentAccount();
            if (parentAccount instanceof NetworkAccountNode && ((NetworkAccountNode) parentAccount).hasMailSender()) {
                menu.add(compositionItem);
            }
            if (treeNode.node instanceof OutboxMailboxNode
                    && ((OutboxMailboxNode) treeNode.node).hasUnsentMessages()) {
                menu.add(sendUnsentItem);
            }
        } else if (treeNode.node instanceof AccountNode) {
            AccountNode accountNode = (AccountNode) treeNode.node;
            if (accountNode.getRootMailbox() != null) {
                menu.add(refreshStatusItem);
            }
            if (accountNode.hasFolders()) {
                menu.add(refreshFoldersItem);
            }
            if (accountNode instanceof NetworkAccountNode && ((NetworkAccountNode) accountNode).hasMailSender()) {
                menu.add(compositionItem);
            }
            if (accountNode.getStatus() == AccountNode.STATUS_ONLINE) {
                menu.add(disconnectItem);
            }
        }
    }

    /* (non-Javadoc)
     * @see net.rim.device.api.ui.Screen#navigationClick(int, int)
     */
    public boolean navigationClick(int status, int time) {
        selectFolderItem.run();
        return true;
    }


    public boolean keyChar(char key, int status, int time) {
        switch (key) {
            case Keypad.KEY_ENTER:
                selectFolderItem.run();
                return true;
            case Keypad.KEY_SPACE:
                toggleSelectedNode();
                return true;
            case 'u':
                if(_focusTree != null)refreshFoldersItemHandler(_focusTree);
                return true;
        }

        switch (KeyHandler.keyCharShortcut(key, status)) {
            case KeyHandler.SCROLL_TOP:
                screen.scroll(Manager.TOPMOST);
                return true;
            case KeyHandler.SCROLL_BOTTOM:
                screen.scroll(Manager.BOTTOMMOST);
                return true;
            case KeyHandler.SCROLL_NEXT_DATE:
                scrollNextAccount(true);
                return true;
            case KeyHandler.SCROLL_PREV_DATE:
                scrollNextAccount(false);
                return true;
            case KeyHandler.MESSAGE_COMPOSE:
                MailHomeTreeNode treeNode = (MailHomeTreeNode) treeField.getCookie(treeField.getCurrentNode());
                AccountNode parentAccount = getAccountForTreeNode(treeNode);
                if (parentAccount instanceof NetworkAccountNode && ((NetworkAccountNode) parentAccount).hasMailSender()) {
                    compositionItem.run();
                    return true;
                }
        }
        return false;
    }

    private void scrollNextAccount(boolean direction) {
        int node = treeField.getCurrentNode();
        if (node == -1) {
            return;
        }

        while (treeField.getParent(node) > 0) {
            node = treeField.getParent(node);
        }
        if (node == -1) {
            return;
        }

        if (direction) {
            node = treeField.getNextSibling(node);
        } else {
            node = treeField.getPreviousSibling(node);
        }
        if (node == -1) {
            return;
        }

        treeField.setCurrentNode(node);
    }

    /* (non-Javadoc)
     * @see org.logicprobe.LogicMail.ui.AbstractScreenProvider#shortcutAction(org.logicprobe.LogicMail.ui.ScreenProvider.ShortcutItem)
     */
    public void shortcutAction(ShortcutItem item) {
        switch (item.getId()) {
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
        if (curNode == -1) {
            return;
        }

        // Make sure the selected node has children
        if (treeField.getFirstChild(curNode) == -1) {
            return;
        }

        // Toggle the expansion state of the current node
        treeField.setExpanded(curNode, !treeField.getExpanded(curNode));
    }

    private void treeField_FocusChanged(Field field, int eventType) {
        boolean enableCompose;
        boolean enableFolder;

        int curNode = treeField.getCurrentNode();
        if (curNode == -1) {
            enableFolder = false;
            enableCompose = false;
        } else {
            // Check whether we can enable folder expansion
            if (treeField.getFirstChild(curNode) == -1) {
                enableFolder = false;
            } else {
                enableFolder = true;
            }

            // Check whether we can enable composition
            MailHomeTreeNode treeNode = (MailHomeTreeNode) treeField.getCookie(curNode);
            if (treeNode.node instanceof NetworkAccountNode) {
                if (((NetworkAccountNode) treeNode.node).hasMailSender()) {
                    enableCompose = true;
                } else {
                    enableCompose = false;
                }
            } else if (treeNode.node instanceof MailboxNode) {
                AccountNode parentAccount = ((MailboxNode) treeNode.node).getParentAccount();
                if (parentAccount instanceof NetworkAccountNode && ((NetworkAccountNode) parentAccount).hasMailSender()) {
                    enableCompose = true;
                } else {
                    enableCompose = false;
                }
            } else {
                enableCompose = false;
            }
        }

        ((StandardScreen) screen).setShortcutEnabled(SHORTCUT_COMPOSE, enableCompose);
        ((StandardScreen) screen).setShortcutEnabled(SHORTCUT_FOLDER, enableFolder);
    }

    private void treeField_DrawTreeItem(
            TreeField treeField,
            Graphics graphics,
            int node, int y, int width, int indent) {
        MailHomeTreeNode treeNode = (MailHomeTreeNode) treeField.getCookie(node);
        int rowHeight = treeField.getRowHeight();
        int fontHeight = graphics.getFont().getHeight();

        Bitmap icon = getNodeIcon(treeNode.node);

        graphics.drawBitmap(
                indent + (rowHeight / 2 - icon.getWidth() / 2),
                y + (fontHeight / 2 - icon.getWidth() / 2),
                icon.getWidth(),
                icon.getHeight(),
                icon, 0, 0);

        Font origFont = graphics.getFont();
        StringBuffer buf = new StringBuffer();
        buf.append(treeNode.node.toString());

        if (treeNode.type == MailHomeTreeNode.TYPE_ACCOUNT) {
            graphics.setFont(origFont.derive(Font.BOLD));
        } else if (treeNode.type == MailHomeTreeNode.TYPE_MAILBOX) {
            MailboxNode mailboxNode = (MailboxNode) treeNode.node;

            if (!mailboxNode.isSelectable()) {
                graphics.setFont(origFont.derive(Font.ITALIC));
            } else {
                int unseenCount = mailboxNode.getUnseenMessageCount();
                if (unseenCount > 0) {
                    buf.append(" (");
                    buf.append(unseenCount);
                    buf.append(")");
                    graphics.setFont(origFont.derive(Font.BOLD));
                } else {
                    graphics.setFont(origFont.derive(Font.PLAIN));
                }
            }
        }
        graphics.drawText(buf.toString(), indent + rowHeight, y, Graphics.ELLIPSIS, width);
    }

    protected Bitmap getNodeIcon(Node node) {
        return NodeIcons.getIcon(node);
    }

    /**
     * Tree node data class
     */
    private static class MailHomeTreeNode implements TreeFieldNode {
        public static final int TYPE_ACCOUNT = 1;
        public static final int TYPE_MAILBOX = 2;

        public Node node;
        public int type;
        public MailHomeTreeNode[] children;

        public MailHomeTreeNode(Node node, int type) {
            this.node = node;
            this.type = type;
        }

        public boolean isNodeSelectable() {
            if (node instanceof MailboxNode) {
                MailboxNode mailboxNode = (MailboxNode) node;
                return mailboxNode.isSelectable();
            } else {
                return false;
            }
        }
    }

    /**
     * Recursively traverse a tree of TreeNodes and build a
     * linear vector of all the MailboxNodes contained within.
     *
     * @param result Result vector to add to.
     * @param nodeId Starting node.
     */
    private static void getMailboxNodes(Vector result, MailHomeTreeNode treeNode) {
        if (treeNode.node instanceof MailboxNode) {
            result.addElement(treeNode.node);
        }
        if (treeNode.children != null) {
            for (int i = 0; i < treeNode.children.length; i++) {
                getMailboxNodes(result, treeNode.children[i]);
            }
        }
    }

    private static AccountNode getAccountForTreeNode(MailHomeTreeNode treeNode) {
        AccountNode accountNode;
        if (treeNode.node instanceof AccountNode) {
            accountNode = (AccountNode) treeNode.node;
        } else if (treeNode.node instanceof MailboxNode) {
            MailboxNode mailboxNode = (MailboxNode) treeNode.node;
            accountNode = mailboxNode.getParentAccount();
        } else {
            accountNode = null;
        }
        return accountNode;
    }

    private void loadScreenMetadata() {
        DataStoreFactory.getMetadataStore().load();
        SerializableHashtable folderMetadata;
        folderMetadata =
                (SerializableHashtable) DataStoreFactory.getMetadataStore().getNamedObject(
                        this.getClass().toString());

        if (folderMetadata != null) {
            restoreScreenState(folderMetadata);
        } else {
            treeField.setCurrentNode(treeField.getFirstChild(0));
        }
    }

    private void restoreScreenState(Hashtable folderMetadata) {
        Object cookie;
        Object value;
        Vector actions = new Vector();
        String selectedNodeKey;
        int selectedNodeId = -1;

        // Get the currently selected node, if one was saved
        value = folderMetadata.get(Boolean.TRUE);
        if (value instanceof String) {
            selectedNodeKey = (String) value;
        } else {
            selectedNodeKey = null;
        }

        // Walk the folder tree and build a depth-first list of
        // nodes we need to set the expansion state for
        int curNode = treeField.nextNode(0, 0, true);
        while (curNode > 0) {
            cookie = treeField.getCookie(curNode);
            if (cookie instanceof MailHomeTreeNode) {
                String key = getTreeNodeKey((MailHomeTreeNode) cookie);

                // If the node has children, then check to see if we saved
                // its expansion state
                if (treeField.getFirstChild(curNode) != -1) {
                    value = folderMetadata.get(key);
                    if (value instanceof Boolean) {
                        actions.addElement(new Object[]{new Integer(curNode), value});
                    }
                }

                // If the selected node has not been found, check to see if it
                // is the element we are currently iterating over
                if (selectedNodeId == -1 && key.equals(selectedNodeKey)) {
                    selectedNodeId = curNode;
                }
            }

            curNode = treeField.nextNode(curNode, 0, true);
        }

        // Iterate backwards across the results from above, and set
        // the appropriate node expansion states.  This approach is
        // necessary so complex trees will be configured properly.
        Object[] action;
        for (int i = actions.size() - 1; i >= 0; i--) {
            action = (Object[]) actions.elementAt(i);
            treeField.setExpanded(((Integer) action[0]).intValue(), ((Boolean) action[1]).booleanValue());
        }

        // Set the selected node, if applicable
        if (selectedNodeId != -1) {
            treeField.setCurrentNode(selectedNodeId);
        } else {
            treeField.setCurrentNode(treeField.getFirstChild(0));
        }
    }

    void saveScreenMetadata() {
        SerializableHashtable folderMetadata = new SerializableHashtable();
        saveScreenState(folderMetadata);

        DataStoreFactory.getMetadataStore().putNamedObject(
                this.getClass().toString(), folderMetadata);
        DataStoreFactory.getMetadataStore().save();
    }

    private void saveScreenState(Hashtable folderMetadata) {
        // Save the expansion state
        int curNode = treeField.nextNode(0, 0, true);
        Object cookie;
        while (curNode > 0) {
            if (treeField.getFirstChild(curNode) != -1) {
                cookie = treeField.getCookie(curNode);
                if (cookie instanceof MailHomeTreeNode) {
                    String key = getTreeNodeKey((MailHomeTreeNode) cookie);
                    folderMetadata.put(
                            key,
                            new Boolean(treeField.getExpanded(curNode)));
                }
            }
            curNode = treeField.nextNode(curNode, 0, true);
        }

        // Save the currently selected node
        curNode = treeField.getCurrentNode();
        if (curNode != -1) {
            cookie = treeField.getCookie(curNode);
            if (cookie instanceof MailHomeTreeNode) {
                String key = getTreeNodeKey((MailHomeTreeNode) cookie);
                folderMetadata.put(Boolean.TRUE, key);
            }
        }
    }

    private static String getTreeNodeKey(MailHomeTreeNode treeNode) {
        Node node = treeNode.node;
        String key;
        if (node instanceof Serializable) {
            key = Long.toString(((Serializable) node).getUniqueId());
        } else {
            key = node.toString();
        }
        return key;
    }
}
