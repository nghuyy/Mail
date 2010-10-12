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
package org.logicprobe.LogicMail.model;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import net.rim.device.api.collection.util.BigVector;
import net.rim.device.api.util.Comparator;
import net.rim.device.api.util.SimpleSortingVector;

import org.logicprobe.LogicMail.mail.FolderEvent;
import org.logicprobe.LogicMail.mail.FolderMessagesEvent;
import org.logicprobe.LogicMail.mail.FolderTreeItem;
import org.logicprobe.LogicMail.mail.MessageEvent;
import org.logicprobe.LogicMail.mail.MessageToken;
import org.logicprobe.LogicMail.message.FolderMessage;
import org.logicprobe.LogicMail.message.Message;
import org.logicprobe.LogicMail.message.MessageEnvelope;
import org.logicprobe.LogicMail.message.MessageFlags;
import org.logicprobe.LogicMail.util.EventListenerList;
import org.logicprobe.LogicMail.util.Serializable;
import org.logicprobe.LogicMail.util.SerializationUtils;
import org.logicprobe.LogicMail.util.UniqueIdGenerator;

/**
 * Mailbox node for the mail data model.
 * This node contains both <tt>MailboxNode</tt> and
 * <tt>MessageNode</tt> instances as its children.
 */
public class MailboxNode implements Node, Serializable {
	private long uniqueId;
	private AccountNode parentAccount;
	private MailboxNode parentMailbox;
	private SimpleSortingVector mailboxes;
	private BigVector messages;
	private Hashtable messageMap;
	private Hashtable messageTokenMap;
	private Hashtable messageTokenUidSet;
	private EventListenerList listenerList = new EventListenerList();
	private int type;
	private FolderTreeItem folderTreeItem;
	private boolean hasAppend;
	private int unseenMessageCount;
    private final Vector pendingExpungeMessages = new Vector();
    
	public final static int TYPE_NORMAL = 0;
	public final static int TYPE_INBOX  = 1;
	public final static int TYPE_OUTBOX = 2;
	public final static int TYPE_DRAFTS = 3;
	public final static int TYPE_SENT   = 4;
	public final static int TYPE_TRASH  = 5;

	private static class MailboxNodeComparator implements Comparator {
		public int compare(Object o1, Object o2) {
			if(o1 instanceof MailboxNode && o2 instanceof MailboxNode) {
				MailboxNode mailbox1 = (MailboxNode)o1;
				int mailboxType1 = mailbox1.getType();
				MailboxNode mailbox2 = (MailboxNode)o2;
				int mailboxType2 = mailbox2.getType();
				int result = 0;
				
				if(mailboxType1 == MailboxNode.TYPE_INBOX) {
					result = -1;
				}
				else if(mailboxType2 == MailboxNode.TYPE_INBOX) {
					result = 1;
				}
				else if(mailboxType1 != MailboxNode.TYPE_NORMAL && mailboxType2 == MailboxNode.TYPE_NORMAL) {
					result = -1;
				}
				else if(mailboxType1 == MailboxNode.TYPE_NORMAL && mailboxType2 != MailboxNode.TYPE_NORMAL) {
					result = 1;
				}
				else {
					result = mailbox1.toString().compareTo(mailbox2.toString());
				}
				return result;
			}
			else {
				throw new ClassCastException("Cannot compare types");
			}
		}
	}

	/** Static comparator used to compare mailbox nodes for insertion ordering */
	private static MailboxNodeComparator comparator = new MailboxNodeComparator();
	
	/**
	 * Initializes a new instance of <tt>MailboxNode</tt>.
	 * 
	 * @param folderTreeItem The folder item this node wraps.
	 * @param hasAppend True if messages can be appended to this mailbox.
	 * @param type The type of mailbox this is representing.
	 */
	MailboxNode(FolderTreeItem folderTreeItem, boolean hasAppend, int type) {
		this.uniqueId = UniqueIdGenerator.getInstance().getUniqueId();
		this.mailboxes = new SimpleSortingVector();
		this.mailboxes.setSortComparator(MailboxNode.getComparator());
		this.mailboxes.setSort(true);
		this.messages = new BigVector();
		this.messageMap = new Hashtable();
		this.messageTokenMap = new Hashtable();
		this.messageTokenUidSet = new Hashtable();
		if(folderTreeItem != null) {
			this.setFolderTreeItem(new FolderTreeItem(folderTreeItem));
		}
		this.type = type;
		this.hasAppend = hasAppend;
	}
	
	/**
	 * Initializes a new instance of <tt>MailboxNode</tt>.
	 * 
	 * @param folderTreeItem The folder item this node wraps.
	 * @param hasAppend True if messages can be appended to this mailbox.
	 */
	MailboxNode(FolderTreeItem folderTreeItem, boolean hasAppend) {
		this(folderTreeItem, hasAppend, TYPE_NORMAL);
	}
	
	/**
	 * Initializes a new instance of <tt>MailboxNode</tt>.
	 * 
	 * @param folderTreeItem The folder item this node wraps.
	 */
	MailboxNode(FolderTreeItem folderTreeItem) {
		this(folderTreeItem, false, TYPE_NORMAL);
	}

	/**
	 * Initializes a new instance of <tt>MailboxNode</tt>.
	 * 
	 * <p><i>Note:</i> This constructor is only exposed for
	 * serialization purposes, and should never be called from
	 * outside this package for any other reason.
	 */
	public MailboxNode() {
		this(null, false, TYPE_NORMAL);
	}
	
	public void accept(NodeVisitor visitor) {
		visitor.visit(this);
	}

	/**
	 * Gets the comparator used to compare mailbox nodes for insertion ordering.
	 * 
	 * @return the comparator
	 */
	public static Comparator getComparator() {
		return MailboxNode.comparator;
	}
	
	/**
	 * Sets the account which this mailbox is contained within.
	 * 
	 * @param parentAccount The account.
	 */
	void setParentAccount(AccountNode parentAccount) {
		this.parentAccount = parentAccount;
		
		// If this is the inbox of a network account which does not support
		// folders, then set the unique ID to match that of the account
		// configuration.  This is necessary because this node's original ID
		// will not be serialized, yet it still needs a way to be matched up
		// with a local cache folder.
		if(this.type == TYPE_INBOX && !this.parentAccount.hasFolders() && this.parentAccount instanceof NetworkAccountNode) {
		    this.uniqueId = ((NetworkAccountNode)this.parentAccount).getAccountConfig().getUniqueId();
		}
	}
	
	/**
	 * Gets the account which contains this mailbox.
	 * 
	 * @return The account.
	 */
	public AccountNode getParentAccount() {
		return this.parentAccount;
	}
	
	/**
	 * Sets the mailbox which contains this mailbox.
	 * 
	 * @param parentMailbox The mailbox, or null if top-level.
	 */
	void setParentMailbox(MailboxNode parentMailbox) {
		this.parentMailbox = parentMailbox;
	}
	
	/**
	 * Gets the mailbox which contains this mailbox.
	 * 
	 * @return The mailbox, or null if this is a top-level mailbox.
	 */
	public MailboxNode getParentMailbox() {
		return this.parentMailbox;
	}
	
	/**
	 * Sets the <tt>FolderTreeItem</tt> associated with this mailbox.
	 * This method exists to allow regenerating the folder tree
	 * without having to recreate the <tt>MailboxNode</tt> instances
	 * that maintain cache data for it.
	 * 
	 * <p><i>Note:</i> The actual <tt>FolderTreeItem</tt> used by this
	 * class is a deep copy of the one passed to this method.  It is
	 * implemented this way since we need a standalone object that
	 * is not part of a tree.  This is necessary to avoid excessive
	 * recursion during serialization.
	 * 
	 * @param folderTreeItem Folder tree item.
	 */
	void setFolderTreeItem(FolderTreeItem folderTreeItem) {
		this.folderTreeItem = new FolderTreeItem(folderTreeItem);
	}
	
	/**
	 * Gets the <tt>FolderTreeItem</tt> associated with this mailbox.
	 * This method exists to support regeneration of the folder tree,
	 * and the more specific public methods should be used for
	 * all other purposes.
	 * 
	 * @return Folder tree item.
	 */
	FolderTreeItem getFolderTreeItem() {
		return this.folderTreeItem;
	}
	
	/**
	 * Get the sub-level mailboxes contained under this mailbox.
	 * This method returns an array that is a shallow copy of the
	 * live mailbox list.  It is primarily intended for use
	 * during initialization.
	 *  
	 * @return Mailbox nodes.
	 */
	public MailboxNode[] getMailboxes() {
		MailboxNode[] result;
		synchronized(mailboxes) {
			int size = mailboxes.size();
			result = new MailboxNode[size];
			mailboxes.copyInto(result);
		}
		return result;
	}

	/**
	 * Get the messages contained within this mailbox.
	 * This method returns an array that is a shallow copy of the
	 * live message list.  It is primarily intended for use
	 * during initialization.
	 *  
	 * @return Mailbox nodes.
	 */
	public MessageNode[] getMessages() {
		MessageNode[] result;
		synchronized(messages) {
			int size = messages.size();
			result = new MessageNode[size];
			messages.copyInto(0, size, result, 0);
		}
		return result;
	}
	
	/**
	 * Appends a message node to this mailbox from an external source.
	 * This method will request that the underlying mail store
	 * add the provided message to its contents for this mailbox.
	 * 
	 * The actual messages contained within this node will not be
	 * updated until the mail store informs the object model of
	 * the new message.
	 * 
	 * @param message Message to append
	 */
	public void appendMessage(MessageNode message) {
	    parentAccount.getMailStoreServices().requestMessageAppend(
	            this.folderTreeItem, message);
	}

	/**
	 * Appends a message to this mailbox from an external source.
	 * This method will request that the underlying mail store
	 * add the provided message to its contents for this mailbox.
	 * 
	 * The actual messages contained within this node will not be
	 * updated until the mail store informs the object model of
	 * the new message.
	 * 
	 * @param envelope Envelope of the message to append
	 * @param message Message to append
	 * @param messageFlags Flags for the message
	 */
	public void appendMessage(MessageEnvelope envelope, Message message, MessageFlags messageFlags) {
	    parentAccount.getMailStoreServices().requestMessageAppend(
	            this.folderTreeItem, envelope, message, messageFlags);
	}
	
	/**
	 * Appends a raw message to this mailbox from an external source.
	 * This method will request that the underlying mail store
	 * add the provided message to its contents for this mailbox.
	 * 
	 * The actual messages contained within this node will not be
	 * updated until the mail store informs the object model of
	 * the new message.
	 * 
	 * @param rawMessage Message to append
	 * @param initialFlags Initial flags for the message
	 */
	public void appendRawMessage(String rawMessage, MessageFlags initialFlags) {
		parentAccount.getMailStoreServices().requestMessageAppend(
		        this.folderTreeItem, rawMessage, initialFlags);
	}

	/**
	 * Copies a message into this mailbox from another mailbox within the same account.
	 * This method will request the underlying mail store to copy the message
	 * on the server side.
     * <p>
     * If the mail store does not support this operation, then this method will
     * have no effect.
     * </p>
	 * 
	 * @param messageNode Message to copy into this mailbox
	 */
	public void copyMessageInto(MessageNode messageNode) {
		parentAccount.getMailStoreServices().requestMessageCopy(
		        this.folderTreeItem, messageNode);
	}

    /**
     * Moves a message into this mailbox from another mailbox within the same account.
     * This method will request the underlying mail store to copy the message
     * on the server side.  If and only if the copy is successful, the source message
     * will be marked as deleted.
     * <p>
     * If the mail store does not support this operation, then this method will
     * have no effect.
     * </p>
     * 
     * @param messageNode Message to copy into this mailbox
     */
	public void moveMessageInto(MessageNode messageNode) {
        parentAccount.getMailStoreServices().requestMessageMove(
                this.folderTreeItem, messageNode);
	}
	
	/**
	 * Adds a mailbox to this mailbox.
	 * 
	 * @param mailbox The mailbox to add.
	 */
	void addMailbox(MailboxNode mailbox) {
		synchronized(mailboxes) {
			if(!mailboxes.contains(mailbox)) {
				mailboxes.addElement(mailbox);
				mailbox.setParentMailbox(this);
			}
		}
	}
	
	/**
	 * Removes a mailbox from this mailbox.
	 * 
	 * @param mailbox The mailbox to remove.
	 */
	void removeMailbox(MailboxNode mailbox) {
		synchronized(mailboxes) {
			if(mailboxes.contains(mailbox)) {
				mailboxes.removeElement(mailbox);
				mailbox.setParentMailbox(null);
			}
		}
	}
	
	/**
	 * Removes all mailboxes from this mailbox.
	 */
	void clearMailboxes() {
		synchronized(mailboxes) {
			mailboxes.removeAllElements();
		}
	}
	
	/**
	 * Adds a message to this mailbox.
	 * 
	 * @param message The message to add.
	 */
	void addMessage(MessageNode message) {
		boolean messageAdded;
		synchronized(messages) {
			messageAdded = addMessageImpl(message);
		}
		if(messageAdded) {
			fireMailboxStatusChanged(MailboxNodeEvent.TYPE_NEW_MESSAGES, new MessageNode[] { message });
			updateUnseenMessages(true);
		}
	}
	
	/**
	 * Adds messages to this mailbox.
	 * 
	 * @param messages The messages to add.
	 */
	void addMessages(MessageNode[] messages) {
		Vector addedMessages = null;
		synchronized(messages) {
			for(int i=0; i<messages.length; i++) {
				if(addMessageImpl(messages[i])) {
					if(addedMessages == null) {
						addedMessages = new Vector();
					}
					addedMessages.addElement(messages[i]);
				}
			}
		}
		if(addedMessages != null) {
			MessageNode[] addedMessagesArray = new MessageNode[addedMessages.size()];
			addedMessages.copyInto(addedMessagesArray);
			fireMailboxStatusChanged(MailboxNodeEvent.TYPE_NEW_MESSAGES, addedMessagesArray);
			updateUnseenMessages(true);
		}
	}
	
	/**
	 * Adds a message to this mailbox using a sorted insertion
	 * based on message id.  Since this is intended to be used
	 * from within other visible methods, it should only be
	 * called from within a "synchronized(messages)" block.
	 *  
	 * @param message The message to add.
	 * @return True if the message was added, false otherwise
	 */
	private boolean addMessageImpl(MessageNode message) {
		if(!messageMap.containsKey(message)) {
			message.setParent(this);
			messages.insertElement(MessageNode.getComparator(), message);
			messageMap.put(message, message);
			messageTokenMap.put(message.getMessageToken(), message);
			messageTokenUidSet.put(message.getMessageToken().getMessageUid(), Boolean.TRUE);
			if(getParentAccount() instanceof NetworkAccountNode) {
			    message.setCachedContent(MailFileManager.getInstance().messageNodeExists(this, message.getMessageToken()));
			}
			return true;
		}
		else {
			return false;
		}
	}
	
	/**
	 * Removes a message from this mailbox.
	 * 
	 * @param message The message to remove
	 */
	void removeMessage(MessageNode message) {
		synchronized(messages) {
			if(messageMap.containsKey(message)) {
				messages.removeElement(MessageNode.getComparator(), message);
				message.setParent(null);
				messageMap.remove(message);
				messageTokenMap.remove(message.getMessageToken());
				messageTokenUidSet.remove(message.getMessageToken().getMessageUid());
			}
		}
        updateUnseenMessages(false);
		fireMailboxStatusChanged(MailboxNodeEvent.TYPE_DELETED_MESSAGES, new MessageNode[] { message });
	}

	/**
	 * Removes all messages from this mailbox.
	 */
	void clearMessages() {
		synchronized(messages) {
			// Clear message parent references
			int size = messages.size();
			for(int i=0; i<size; i++) {
				((MessageNode)messages.elementAt(i)).setParent(null);
			}
			// Clear out the collections
			messages.removeAll();
			messageMap.clear();
			messageTokenMap.clear();
			messageTokenUidSet.clear();
		}
        updateUnseenMessages(false);
		fireMailboxStatusChanged(MailboxNodeEvent.TYPE_STATUS, null);
	}
	
	/**
	 * Gets whether this mailbox contains a particular message.
	 * 
	 * @param messageNode The message to look for.
	 * @return True if it exists, false otherwise.
	 */
	boolean containsMessage(MessageNode messageNode) {
		synchronized(messages) {
			return messageMap.containsKey(messageNode);
		}
	}

	/**
	 * Gets whether this mailbox contains a particular message.
	 * 
	 * @param messageToken The message token to look for.
	 * @return True if it exists, false otherwise.
	 */
	boolean containsMessageByTag(MessageToken messageToken) {
		synchronized(messages) {
			return messageTokenMap.containsKey(messageToken);
		}
	}
	
	/**
	 * Gets a particular message.
	 * 
	 * @param messageToken The message token to look for.
	 * @return The message if it exists, null otherwise.
	 */
	MessageNode getMessageByToken(MessageToken messageToken) {
		synchronized(messages) {
			MessageNode message = (MessageNode)messageTokenMap.get(messageToken);
			return message;
		}
	}
	
	/**
	 * Gets the name of this mailbox.
	 * 
	 * @return The name.
	 */
	public String toString() {
		return this.folderTreeItem.getName();
	}

	/**
	 * Gets the path of this mailbox.
	 * <p>Once serialization and unique IDs are fully implemented, this should
	 * be removed.  It exposes too much implementation detail to the upper
	 * layers of the system.
	 * 
	 * @return The path.
	 */
	public String getPath() {
		return this.folderTreeItem.getPath();
	}
	
	/**
	 * Gets whether messages can be appended to this mailbox.
	 * 
	 * @return True if messages can be appended.
	 */
	public boolean hasAppend() {
		return this.hasAppend;
	}
	
	/**
	 * Gets whether messages can be copied into this mailbox
	 * from other mailboxes on the same account.
	 * 
	 * @return True if messages can be copied into this mailbox.
	 */
	public boolean hasCopy() {
		return parentAccount != null && parentAccount.getMailStoreServices().hasCopy();
	}
	
	/**
	 * Sets the type of this mailbox.
	 * 
	 * @param type The type.
	 */
	void setType(int type) {
		if(this.type != type) {
			this.type = type;
			if(this.getParentMailbox() != null) {
				this.getParentMailbox().mailboxes.reSort();
			}
		}
	}
	
	/**
	 * Gets the type of this mailbox.
	 * 
	 * @return The type.
	 */
	public int getType() {
		return this.type;
	}

	/**
	 * Gets whether this folder can be selected.
	 * If a folder cannot be selected, then it cannot
	 * contain any messages.  However, it can contain
	 * other folders.
	 * 
	 * @return True if this folder can be selected.
	 */
    public boolean isSelectable() {
        return this.folderTreeItem.isSelectable();
    }
    
    /**
     * Gets the message count for this folder.
     * This should match the length of the array returned
     * by getMessages(), and is provided for convenience.
     * 
     * @return Message count.
     */
    public int getMessageCount() {
    	int result = 0;
		synchronized(messages) {
			result = messages.size();
		}
        return result;
    }

    /**
     * Check if this mailbox has messages marked as deleted.
     * This is the recommended way to check for deleted messages,
     * since the UI may not always track such messages.
     * 
     * @return True if deleted messages exist
     */
    public boolean hasDeletedMessages() {
        boolean hasDeleted = false;
        synchronized(messages) {
            int size = messages.size();
            for(int i=0; i<size; i++) {
                int flags = ((MessageNode)messages.elementAt(i)).getFlags();
                if((flags & MessageNode.Flag.DELETED) != 0) {
                    hasDeleted = true;
                    break;
                }
            }
        }
        return hasDeleted;
    }
    
    /**
     * Tells the underlying mail store to expunge any deleted messages
     * from the mailbox, if possible.
     */
    public void expungeDeletedMessages() {
        synchronized(messages) {
            int size = messages.size();
            for(int i=0; i<size; i++) {
                MessageNode messageNode = (MessageNode)messages.elementAt(i);
                int flags = messageNode.getFlags();
                if((flags & MessageNode.Flag.DELETED) != 0) {
                    pendingExpungeMessages.addElement(messageNode);
                }
            }
        }
        parentAccount.getMailStoreServices().requestFolderExpunge(this.folderTreeItem);
    }

    /**
     * Update the unseen message count from the local messages collection.
     * 
     * @param fireEvent true to fire a status event if the count has changed
     */
    void updateUnseenMessages(boolean fireEvent) {
        boolean updated = false;
        synchronized(messages) {
            int newCount = 0;
            int size = messages.size();
            for(int i=0; i<size; i++) {
                int flags = ((MessageNode)messages.elementAt(i)).getFlags();
                if((flags & MessageNode.Flag.SEEN) == 0) {
                    newCount++;
                }
            }
            if(newCount != unseenMessageCount) {
                unseenMessageCount = newCount;
                updated = true;
            }
        }
        if(updated && fireEvent) {
            fireMailboxStatusChanged(MailboxNodeEvent.TYPE_STATUS, null);
        }
    }

    /**
     * Update the folder status numbers due to a direct server request.
     * This updates the numbers stored for the purposes of user notification,
     * and has no effect on the actual contents of the mailbox.
     *
     * @param e the event from the mail store
     */
    public void mailStoreFolderStatusChanged(FolderEvent e) {
        FolderTreeItem eventFolder = e.getFolder();
        synchronized(messages) {
            folderTreeItem.setMsgCount(eventFolder.getMsgCount());
            folderTreeItem.setUnseenCount(eventFolder.getUnseenCount());
            unseenMessageCount = folderTreeItem.getUnseenCount();
        }
        fireMailboxStatusChanged(MailboxNodeEvent.TYPE_STATUS, null);
    }

    /**
     * Called when the mail store notifies of folder messages becoming
     * available, as either complete headers or simple flag updates.
     *
     * @param e the event from the mail store
     */
    void mailStoreFolderMessagesAvailable(FolderMessagesEvent e) {
        FolderMessage[] folderMessages = e.getMessages();
        
        if (folderMessages != null) {
            if(e.isFlagsOnly()) {
                folderMessagesAvailableFlagsOnly(folderMessages, e.isServer());
            }
            else {
                folderMessagesAvailable(folderMessages, e.isServer());
            }
        }
        else {
            //folderMessagesAvailableComplete();
        }
    }

    private void folderMessagesAvailableFlagsOnly(FolderMessage[] folderMessages, boolean server) {
        // Only flags have been retrieved, so the existing messages need to
        // be checked and additional actions requested accordingly.
        for (int i = 0; i < folderMessages.length; i++) {
            MessageToken messageToken = folderMessages[i].getMessageToken();
            MessageFlags messageFlags = folderMessages[i].getFlags();
            
            synchronized(messages) {
                MessageNode messageNode = (MessageNode)messageTokenMap.get(messageToken);
                if(messageNode != null) {
                    // Message already exists in the mailbox, and just needs
                    // its flags updated.
                    messageNode.setFlags(MessageNode.convertMessageFlags(messageFlags));

                    // Update the token based on the token that came along
                    // with the flags.  This will update any volatile state
                    // information, such as POP message indices
                    messageNode.getMessageToken().updateToken(messageToken);

                    if(server && !messageNode.existsOnServer()) {
                        messageNode.setExistsOnServer(true);
                    }
                }
            }
        }
    }

    private void folderMessagesAvailable(FolderMessage[] folderMessages, boolean server) {
        // Determine what MessageNodes need to be created, and add them.
        Vector addedMessages = new Vector();
   
        for (int i = 0; i < folderMessages.length; i++) {
            MessageNode messageNode = new MessageNode(folderMessages[i]);
            messageNode.setExistsOnServer(server);
            addedMessages.addElement(messageNode);
        }
   
        MessageNode[] addedMessagesArray = new MessageNode[addedMessages.size()];
        addedMessages.copyInto(addedMessagesArray);
        addMessages(addedMessagesArray);
    }
    
    /**
     * Called when the mail store notifies of a completed expunge operation on
     * this folder, so anything we expected to be expunged can be cleaned out.
     * 
     * @param e the event from the mail store
     */
    void mailStoreFolderExpunged(FolderEvent e) {
        synchronized(messages) {
            Enumeration en = pendingExpungeMessages.elements();
            while(en.hasMoreElements()) {
                ((MessageNode)en.nextElement()).setExistsOnServer(false);
            }
            pendingExpungeMessages.removeAllElements();
        }
    }

    /**
     * Called when the mail store has new message data available.
     *
     * @param e the event from the mail store
     */
    void mailStoreMessageAvailable(MessageEvent e) {
        MessageNode messageNode = getMessageByToken(e.getMessageToken());
        if(messageNode != null) {
            messageNode.mailStoreMessageAvailable(e);
        }
    }
    
    /**
     * Called when the mail store notifies that message flags have changed.
     *
     * @param e the event from the mail store
     */
    void mailStoreMessageFlagsChanged(MessageEvent e) {
        MessageNode messageNode = getMessageByToken(e.getMessageToken());
        if(messageNode != null) {
            messageNode.mailStoreMessageFlagsChanged(e);
        }
    }
    
    /**
     * Gets the unseen message count for this folder.
     * 
     * @return Unseen message count.
     */
    public int getUnseenMessageCount() {
        return unseenMessageCount;
    }
	
    /**
     * Called to refresh the message list contained within this mailbox.
     * This currently replaces the existing message list with the N most
     * recent messages, as defined in the configuration options.
     * 
     * Eventually, this needs to replaced or supplemented with a more
     * sophisticated version that can selectively load additional
     * messages without replacing the existing ones. 
     */
    public void refreshMessages() {
        parentAccount.getMailStoreServices().requestFolderRefresh(this.folderTreeItem);
    }
    
	/**
     * Adds a <tt>MailboxNodeListener</tt> to the mailbox node.
     * 
     * @param l The <tt>MailboxNodeListener</tt> to be added.
     */
    public void addMailboxNodeListener(MailboxNodeListener l) {
        listenerList.add(MailboxNodeListener.class, l);
    }

    /**
     * Removes a <tt>MailboxNodeListener</tt> from the mailbox node.
     * 
     * @param l The <tt>MailboxNodeListener</tt> to be removed.
     */
    public void removeMailboxNodeListener(MailboxNodeListener l) {
        listenerList.remove(MailboxNodeListener.class, l);
    }
    
    /**
     * Returns an array of all <tt>MailboxNodeListener</tt>s
     * that have been added to this mailbox node.
     * 
     * @return All the <tt>MailboxNodeListener</tt>s that have been added,
     * or an empty array if no listeners have been added.
     */
    public MailboxNodeListener[] getMailboxNodeListeners() {
        return (MailboxNodeListener[])listenerList.getListeners(MailboxNodeListener.class);
    }
    
    /**
     * Notifies all registered <tt>MailboxNodeListener</tt>s that
     * the mailbox status has changed.
     * 
     * @param type The type of this event.
     * @param affectedMessages The affected messages. 
     */
    void fireMailboxStatusChanged(int type, MessageNode[] affectedMessages) {
        Object[] listeners = listenerList.getListeners(MailboxNodeListener.class);
        MailboxNodeEvent e = null;
        for(int i=0; i<listeners.length; i++) {
            if(e == null) {
                e = new MailboxNodeEvent(this, type, affectedMessages);
            }
            ((MailboxNodeListener)listeners[i]).mailboxStatusChanged(e);
        }
    }

	/* (non-Javadoc)
	 * @see org.logicprobe.LogicMail.util.Serializable#getUniqueId()
	 */
	public long getUniqueId() {
		return uniqueId;
	}
	
	/* (non-Javadoc)
	 * @see org.logicprobe.LogicMail.util.Serializable#serialize(java.io.DataOutput)
	 */
	public void serialize(DataOutput output) throws IOException {
		output.writeLong(uniqueId);
		output.writeBoolean(hasAppend);
		output.writeInt(type);
		folderTreeItem.serialize(output);
		synchronized(mailboxes) {
			int size = mailboxes.size();
			output.writeInt(size);
			for(int i=0; i<size; i++) {
				byte[] serializedBytes =
					SerializationUtils.serializeClass((MailboxNode)mailboxes.elementAt(i));
				output.writeInt(serializedBytes.length);
				output.write(serializedBytes);
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see org.logicprobe.LogicMail.util.Serializable#deserialize(java.io.DataInput)
	 */
	public void deserialize(DataInput input) throws IOException {
		uniqueId = input.readLong();
		hasAppend = input.readBoolean();
		type = input.readInt();
		folderTreeItem = new FolderTreeItem();
		folderTreeItem.deserialize(input);
		synchronized(mailboxes) {
			int size = input.readInt();
			for(int i=0; i<size; i++) {
				int length = input.readInt();
				if(length < 0 || length > 1000000) {
					// Quick check to deal with data incompatibility
					throw new IOException();
				}
				byte[] serializedBytes = new byte[length];
				input.readFully(serializedBytes);
				Object deserializedObject = SerializationUtils.deserializeClass(serializedBytes);
				if(deserializedObject instanceof MailboxNode) {
					MailboxNode child = (MailboxNode)deserializedObject;
					child.parentMailbox = this;
					mailboxes.addElement(child);
				}
			}
		}
	}
}
