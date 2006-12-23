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

package org.logicprobe.LogicMail.mail;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import net.rim.device.api.util.Arrays;
import org.logicprobe.LogicMail.util.Serializable;

/**
 * Relevant information describing a folder tree.
 */
public class FolderTreeItem implements Serializable {
    private FolderTreeItem parent;
    private FolderTreeItem[] children;
    private String name;
    private String path;
    private String delim;
    private int msgCount;

    /**
     * Create a root folder tree item.
     * @param name The name of the folder
     * @param path The full path to this folder
     * @param delim The path deliminator
     */
    public FolderTreeItem(String name, String path, String delim) {
        this.parent = null;
        this.name = name;
        this.path = path;
        this.delim = delim;
        this.msgCount = -1;
        this.children = null;
    }
    
    /**
     * Create a folder tree item.
     * @param parent The parent of this item
     * @param name The name of the folder
     * @param path The full path to this folder
     * @param delim The path deliminator
     */
    public FolderTreeItem(FolderTreeItem parent, String name, String path, String delim) {
        this.parent = parent;
        this.name = name;
        this.path = path;
        this.delim = delim;
        this.msgCount = -1;
        this.children = null;
    }
    
    /**
     * Creates an uninitialized folder tree item.
     * Only for use during deserialization.
     */
    public FolderTreeItem() {
    }
    
    public void serialize(DataOutputStream output) throws IOException {
        serializeHelper(output, 0);
    }
    
    private void serializeHelper(DataOutputStream output, int depth) throws IOException {
        output.writeInt(depth); // track tree depth
        output.writeUTF(name);
        output.writeUTF(path);
        output.writeUTF(delim);
        output.writeInt(msgCount);
        if(children != null && children.length > 0)
            for(int i=0;i<children.length;i++)
                serializeHelper(output, depth+1);
    }

    public void deserialize(DataInputStream input) throws IOException {
        deserializeHelper(input, 0);
    }
    
    private void deserializeHelper(DataInputStream input, int curDepth) throws IOException {
        if(input.available() <= 0) return;
        
        int depth;
        depth = input.readInt();
        name = input.readUTF();
        path = input.readUTF();
        delim = input.readUTF();
        msgCount = input.readInt();
        if(depth > curDepth) {
            while(input.available() > 0) {
                FolderTreeItem item = new FolderTreeItem();
                item.deserializeHelper(input, depth);
                item.parent = this;
                addChild(item);
            }
        }
    }

    public FolderTreeItem[] children() {
        return children;
    }

    public FolderTreeItem getParent() {
        return parent;
    }

    public boolean hasChildren() {
        return (children != null && children.length > 0);
    }

    public void addChild(FolderTreeItem item) {
        if(children == null) {
            children = new FolderTreeItem[1];
            children[0] = item;
        }
        else {
            Arrays.add(children, item);
        }
    }

    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }

    public String getDelim() {
        return delim;
    }

    public int getMsgCount() {
        return msgCount;
    }

    public void setMsgCount(int msgCount) {
        this.msgCount = msgCount;
    }
}