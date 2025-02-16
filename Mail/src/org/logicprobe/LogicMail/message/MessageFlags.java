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

package org.logicprobe.LogicMail.message;

/**
 * This class contains flags that may be associated with a message.
 * These flags are normally part of a <tt>FolderMessage</tt>, however
 * they sometimes need to be represented independently.
 */
public final class MessageFlags {
    public static interface Flag {
        public static final int SEEN      = 1;
        public static final int ANSWERED  = 2;
        public static final int FLAGGED   = 4;
        public static final int DELETED   = 8;
        public static final int DRAFT     = 16;
        public static final int RECENT    = 32;
        public static final int JUNK      = 64;
        public static final int FORWARDED = 128;
    }
    
    private int flags;
    
	public MessageFlags() {
	}
    
	public MessageFlags(int flags) {
	    this.flags = flags;
	}
    
	public MessageFlags clone() {
	    return new MessageFlags(this.flags);
	}
	
	/**
	 * Gets the message flags as a bit-field value.
	 *
	 * @return the flags as described by {@link Flag}
	 */
	public int getFlags() {
        return flags;
    }
	
	/**
	 * Sets the message flags as a bit-field value.
	 *
	 * @param flags the new flags to set
	 */
	public MessageFlags setFlags(int flags) {
        this.flags = flags;
        return this;
    }
	
    /**
     * Find out whether this message has been previously viewed
     */
    public boolean isSeen() {
        return (flags & Flag.SEEN) != 0;
    }

    /**
     * Set the flag indicating whether this message has been previously viewed
     */
    public MessageFlags setSeen(boolean seen) {
        if(seen) {
            this.flags |= Flag.SEEN;
        }
        else {
            this.flags &= ~Flag.SEEN;
        }
        return this;
    }

    /**
     * Find out whether this message has been replied to
     */
    public boolean isAnswered() {
        return (flags & Flag.ANSWERED) != 0;
    }

    /**
     * Set the flag indicating whether this message has been replied to
     */
    public MessageFlags setAnswered(boolean answered) {
        if(answered) {
            this.flags |= Flag.ANSWERED;
        }
        else {
            this.flags &= ~Flag.ANSWERED;
        }
        return this;
    }

    /**
     * Find out whether this message has been flagged
     */
    public boolean isFlagged() {
        return (flags & Flag.FLAGGED) != 0;
    }

    /**
     * Set the flag indicating whether this message has been flagged
     */
    public MessageFlags setFlagged(boolean flagged) {
        if(flagged) {
            this.flags |= Flag.FLAGGED;
        }
        else {
            this.flags &= ~Flag.FLAGGED;
        }
        return this;
    }

    /**
     * Find out whether this message has been marked as deleted
     */
    public boolean isDeleted() {
        return (flags & Flag.DELETED) != 0;
    }

    /**
     * Set the flag indicating whether this message has been marked as deleted
     */
    public MessageFlags setDeleted(boolean deleted) {
        if(deleted) {
            this.flags |= Flag.DELETED;
        }
        else {
            this.flags &= ~Flag.DELETED;
        }
        return this;
    }

    /**
     * Find out whether this message is a draft
     */
    public boolean isDraft() {
        return (flags & Flag.DRAFT) != 0;
    }

    /**
     * Set the flag indicating whether this message is a draft
     */
    public MessageFlags setDraft(boolean draft) {
        if(draft) {
            this.flags |= Flag.DRAFT;
        }
        else {
            this.flags &= ~Flag.DRAFT;
        }
        return this;
    }

    /**
     * Find out whether this message has recently arrived
     */
    public boolean isRecent() {
        return (flags & Flag.RECENT) != 0;
    }

    /**
     * Set the flag indicating whether this message has recently arrived
     */
    public MessageFlags setRecent(boolean recent) {
        if(recent) {
            this.flags |= Flag.RECENT;
        }
        else {
            this.flags &= ~Flag.RECENT;
        }
        return this;
    }
    
    /**
     * Find out whether this message has been forwarded
     */
    public boolean isForwarded() {
        return (flags & Flag.FORWARDED) != 0;
    }
    
    /**
     * Set the flag indicating whether this message has been forwarded
     */
    public MessageFlags setForwarded(boolean forwarded) {
        if(forwarded) {
            this.flags |= Flag.FORWARDED;
        }
        else {
            this.flags &= ~Flag.FORWARDED;
        }
        return this;
    }
    
    /**
     * Find out whether this message has been flagged as junk
     */
    public boolean isJunk() {
        return (flags & Flag.JUNK) != 0;
    }

    /**
     * Set the flag indicating whether this message has been flagged as junk
     */
    public MessageFlags setJunk(boolean junk) {
        if(junk) {
            this.flags |= Flag.JUNK;
        }
        else {
            this.flags &= ~Flag.JUNK;
        }
        return this;
    }
}
